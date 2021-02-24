/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tests.apex.sdkextensions;

import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.cts.install.lib.host.InstallUtilsHost;

import com.android.tests.rollback.host.AbandonSessionsRule;
import com.android.tradefed.device.ITestDevice.ApexInfo;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;
import com.android.tradefed.util.CommandResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(DeviceJUnit4ClassRunner.class)
public class SdkExtensionsHostTest extends BaseHostJUnit4Test {

    private static final String APP_FILENAME = "sdkextensions_e2e_test_app.apk";
    private static final String APP_PACKAGE = "com.android.tests.apex.sdkextensions";
    private static final String APP_R12_FILENAME = "sdkextensions_e2e_test_app_req_r12.apk";
    private static final String APP_R12_PACKAGE = "com.android.tests.apex.sdkextensions.r12";
    private static final String APP_R45_FILENAME = "sdkextensions_e2e_test_app_req_r45.apk";
    private static final String APP_R45_PACKAGE = "com.android.tests.apex.sdkextensions.r45";
    private static final String MEDIA_FILENAME = "test_com.android.media.apex";
    private static final String SDKEXTENSIONS_FILENAME = "test_com.android.sdkext.apex";

    private static final Duration BOOT_COMPLETE_TIMEOUT = Duration.ofMinutes(2);

    private final InstallUtilsHost mInstallUtils = new InstallUtilsHost(this);

    @Rule
    public AbandonSessionsRule mHostTestRule = new AbandonSessionsRule(this);

    @Before
    public void setUp() throws Exception {
        assumeTrue("Updating APEX is not supported", mInstallUtils.isApexUpdateSupported());
    }

    @Before
    public void installTestApp() throws Exception {
        File testAppFile = mInstallUtils.getTestFile(APP_FILENAME);
        String installResult = getDevice().installPackage(testAppFile, true);
        assertNull(installResult);
    }

    @Before // Generally not needed, but local test devices are sometimes in a "bad" start state.
    @After
    public void cleanup() throws Exception {
        getDevice().uninstallPackage(APP_PACKAGE);
        uninstallApexes(SDKEXTENSIONS_FILENAME, MEDIA_FILENAME);
    }

    @Test
    public void testDefault() throws Exception {
        assertVersion0();
    }

    @Test
    public void upgradeOneApexWithBump()  throws Exception {
        // Version 12 requires test_com.android.sdkext.
        assertVersion0();
        mInstallUtils.installApexes(SDKEXTENSIONS_FILENAME);
        reboot();
        assertVersion12();
    }

    @Test
    public void upgradeOneApex() throws Exception {
        // Version 45 requires updated sdkext and media, so updating just media changes nothing.
        assertVersion0();
        mInstallUtils.installApexes(MEDIA_FILENAME);
        reboot();
        assertVersion0();
    }

    @Test
    public void upgradeTwoApexes() throws Exception {
        // Updating sdkext and media bumps the version to 45.
        assertVersion0();
        mInstallUtils.installApexes(MEDIA_FILENAME, SDKEXTENSIONS_FILENAME);
        reboot();
        assertVersion45();
    }

    private boolean canInstallApp(String filename, String packageName) throws Exception {
        File appFile = mInstallUtils.getTestFile(filename);
        String installResult = getDevice().installPackage(appFile, true);
        if (installResult == null) {
            assertNull(getDevice().uninstallPackage(packageName));
        }
        return installResult == null;
    }

    private int getExtensionVersionR() throws Exception {
        int appValue = Integer.parseInt(broadcast("GET_SDK_VERSION"));
        int syspropValue = getExtensionVersionRFromSysprop();
        assertEquals(appValue, syspropValue);
        return appValue;
    }

    private int getExtensionVersionRFromSysprop() throws Exception {
        CommandResult res = getDevice().executeShellV2Command("getprop build.version.extensions.r");
        assertEquals(0, (int) res.getExitCode());
        String syspropString = res.getStdout().replace("\n", "");
        return Integer.parseInt(syspropString);
    }

    private String broadcast(String action) throws Exception {
        String command = getBroadcastCommand(action);
        CommandResult res = getDevice().executeShellV2Command(command);
        assertEquals(0, (int) res.getExitCode());
        Matcher matcher = Pattern.compile("data=\"([^\"]+)\"").matcher(res.getStdout());
        assertTrue("Unexpected output from am broadcast: " + res.getStdout(), matcher.find());
        return matcher.group(1);
    }

    private void assertVersion0() throws Exception {
        assertEquals(0, getExtensionVersionR());
        assertEquals("true", broadcast("MAKE_CALLS_0"));
        File testAppFile = mInstallUtils.getTestFile(APP_FILENAME);
        String installResult = getDevice().installPackage(testAppFile, true);
        assertFalse(canInstallApp(APP_R12_FILENAME, APP_R12_PACKAGE));
        assertFalse(canInstallApp(APP_R45_FILENAME, APP_R45_PACKAGE));
    }

    private void assertVersion12() throws Exception {
        assertEquals(12, getExtensionVersionR());
        assertEquals("true", broadcast("MAKE_CALLS_45")); // sdkext 45 APIs are available in 12 too.
        assertTrue(canInstallApp(APP_R12_FILENAME, APP_R12_PACKAGE));
        assertFalse(canInstallApp(APP_R45_FILENAME, APP_R45_PACKAGE));
    }

    private void assertVersion45() throws Exception {
        assertEquals(45, getExtensionVersionR());
        assertEquals("true", broadcast("MAKE_CALLS_45"));
        assertTrue(canInstallApp(APP_R12_FILENAME, APP_R12_PACKAGE));
        assertTrue(canInstallApp(APP_R45_FILENAME, APP_R45_PACKAGE));
    }

    private static String getBroadcastCommand(String action) {
        String cmd = "am broadcast";
        cmd += " -a com.android.tests.apex.sdkextensions." + action;
        cmd += " -n com.android.tests.apex.sdkextensions/.Receiver";
        return cmd;
    }

    private boolean uninstallApexes(String... filenames) throws Exception {
        boolean reboot = false;
        for (String filename : filenames) {
            ApexInfo apex = mInstallUtils.getApexInfo(mInstallUtils.getTestFile(filename));
            String res = getDevice().uninstallPackage(apex.name);
            // res is null for successful uninstalls (non-null likely implesfactory version).
            reboot |= res == null;
        }
        if (reboot) {
            reboot();
            return true;
        }
        return false;
    }

    private void reboot() throws Exception {
        getDevice().reboot();
        boolean success = getDevice().waitForBootComplete(BOOT_COMPLETE_TIMEOUT.toMillis());
        assertWithMessage("Device didn't boot in %s", BOOT_COMPLETE_TIMEOUT).that(success).isTrue();
    }
}
