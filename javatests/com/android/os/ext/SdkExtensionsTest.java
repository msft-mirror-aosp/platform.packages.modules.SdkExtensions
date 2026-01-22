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

package com.android.os.ext;

import static android.os.Build.VERSION_CODES;
import static android.os.Build.VERSION_CODES.BAKLAVA;
import static android.os.Build.VERSION_CODES.R;
import static android.os.Build.VERSION_CODES.S;
import static android.os.Build.VERSION_CODES.TIRAMISU;
import static android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
import static android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM;
import static android.os.ext.SdkExtensions.AD_SERVICES;

import static com.android.os.ext.testing.CurrentVersion.CURRENT_TRAIN_VERSION;
import static com.android.os.ext.testing.CurrentVersion.R_BASE_VERSION;
import static com.android.os.ext.testing.CurrentVersion.S_BASE_VERSION;
import static com.android.os.ext.testing.CurrentVersion.T_BASE_VERSION;

import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertThrows;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.os.ext.SdkExtensions;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.modules.utils.build.SdkLevel;
import com.android.os.ext.testing.DeriveSdk;

import com.google.common.truth.StandardSubjectBuilder;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(AndroidJUnit4.class)
public class SdkExtensionsTest {

    private static final String TAG = "SdkExtensionsTest";

    // This runs the copy of the dump code that is bundled inside the test APK.
    private static final String DERIVE_SDK_DUMP = DeriveSdk.dump();

    private enum Expectation {
        /**
         * Expect an extension to be the current / latest defined version, or later (which may be
         * the case if the device under test comes from a more recent build that the tests come
         * from).
         */
        AT_LEAST_CURRENT,
        /** Expect an extension to be missing / version 0 */
        MISSING,
        /** Expect an extension to be at least the base extension version of the device */
        AT_LEAST_BASE,
    }

    private static final Expectation AT_LEAST_CURRENT = Expectation.AT_LEAST_CURRENT;
    private static final Expectation MISSING = Expectation.MISSING;
    private static final Expectation AT_LEAST_BASE = Expectation.AT_LEAST_BASE;

    private static StandardSubjectBuilder assertWithHeader() {
        // This runs the copy of the dump code that is bundled inside the test APK.
        return assertWithMessage(DERIVE_SDK_DUMP);
    }

    private static StandardSubjectBuilder assertWithHeader(String message) {
        return assertWithMessage(DERIVE_SDK_DUMP + "\n" + message);
    }

    private static void assertAtLeastBaseVersion(int version) {
        int minVersion = R_BASE_VERSION;
        if (SdkLevel.isAtLeastU()) {
            minVersion = CURRENT_TRAIN_VERSION;
        } else if (SdkLevel.isAtLeastT()) {
            minVersion = T_BASE_VERSION;
        } else if (SdkLevel.isAtLeastS()) {
            minVersion = S_BASE_VERSION;
        }
        assertWithHeader().that(version).isAtLeast(minVersion);
        assertWithHeader().that(version).isAtMost(CURRENT_TRAIN_VERSION);
    }

    private static void assertVersion(Expectation expectation, int version) {
        switch (expectation) {
            case AT_LEAST_CURRENT:
                assertWithHeader().that(version).isAtLeast(CURRENT_TRAIN_VERSION);
                break;
            case AT_LEAST_BASE:
                assertAtLeastBaseVersion(version);
                break;
            case MISSING:
                assertWithHeader().that(version).isEqualTo(0);
                break;
        }
    }

    private static void assertVersion(Expectation expectation, String propValue) {
        if (expectation == Expectation.MISSING) {
            assertWithHeader().that(propValue).isEqualTo("");
        } else {
            int version = Integer.parseInt(propValue);
            assertVersion(expectation, version);
        }
    }

    public static final void assertVersion(Expectation expectation, int extension, String propId) {
        String prop = "build.version.extensions." + propId;
        assertVersion(expectation, SystemProperties.get(prop));
        assertVersion(expectation, SdkExtensions.getExtensionVersion(extension));
        if (expectation != Expectation.MISSING) {
            int v = SdkExtensions.getAllExtensionVersions().get(extension);
            assertVersion(expectation, v);
        }
    }

    private static int readSdkExtensionsVersion() throws Exception {
        Pattern regex = Pattern.compile("SDK_EXTENSIONS:(\\d+)");
        Matcher matcher = regex.matcher(DERIVE_SDK_DUMP);
        if (!matcher.find()) {
            throw new IllegalStateException("failed to read SdkExtensions version");
        }
        return Integer.parseInt(matcher.group(1));
    }

    @BeforeClass
    public static void setupBeforeTest() throws Exception {
        int sdkExtensionsVersion = readSdkExtensionsVersion();
        assertWithMessage(
                        "\n"
                                + "\n"
                                + "* * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                                + "\n"
                                + "INVALID TEST CONFIGURATION, THE TESTS WILL NOT RUN\n"
                                + "\n"
                                + "The version of the SdkExtensions module installed on\n"
                                + "device is older than the SdkExtensionsTest. This is\n"
                                + "not a supported configuration, and the tests will not\n"
                                + "run.\n"
                                + "\n"
                                + "Verify that the tests do not come from a more recent\n"
                                + "train than what is installed on the device. If you are\n"
                                + "manually installing a new train on the device, remember\n"
                                + "to reboot your device as part of the installation.\n"
                                + "\n"
                                + "* * * * * * * * * * * * * * * * * * * * * * * * * *\n")
                .that(/* version of the SdkExtensions module on device */ sdkExtensionsVersion)
                .isAtLeast(/* version of the test */ CURRENT_TRAIN_VERSION);
    }

    /** Verify that getExtensionVersion only accepts valid extension SDKs */
    @Test
    public void testBadArgument() throws Exception {
        // R is the first SDK version with extensions. Ideally, we'd test all <R values,
        // but it would take too long, so take 10k samples.
        int step = (int) ((VERSION_CODES.R - (long) Integer.MIN_VALUE) / 10_000);
        for (int sdk = Integer.MIN_VALUE; sdk < VERSION_CODES.R; sdk += step) {
            final int finalSdk = sdk;
            assertThrows(
                    IllegalArgumentException.class,
                    () -> SdkExtensions.getExtensionVersion(finalSdk));
        }
    }

    /** Verifies that getExtensionVersion returns zero value for non-existing extensions */
    @Test
    public void testZeroValues() throws Exception {
        Set<Integer> assignedCodes =
                Set.of(R, S, TIRAMISU, UPSIDE_DOWN_CAKE, VANILLA_ICE_CREAM, BAKLAVA, AD_SERVICES);
        for (int sdk = VERSION_CODES.R; sdk <= 1_000_000; sdk++) {
            if (assignedCodes.contains(sdk)) {
                continue;
            }
            // No extension SDKs yet.
            int version = SdkExtensions.getExtensionVersion(sdk);
            assertWithHeader("Extension ID " + sdk + " has non-zero version")
                    .that(version)
                    .isEqualTo(0);
        }
    }

    @Test
    public void testGetAllExtensionVersionsKeys() throws Exception {
        Set<Integer> expectedKeys = new HashSet<>();
        expectedKeys.add(VERSION_CODES.R);
        if (SdkLevel.isAtLeastS()) {
            expectedKeys.add(VERSION_CODES.S);
        }
        if (SdkLevel.isAtLeastT()) {
            expectedKeys.add(VERSION_CODES.TIRAMISU);
            expectedKeys.add(AD_SERVICES);
        }
        if (SdkLevel.isAtLeastU()) {
            expectedKeys.add(UPSIDE_DOWN_CAKE);
        }
        if (SdkLevel.isAtLeastV()) {
            expectedKeys.add(VANILLA_ICE_CREAM);
        }
        if (SdkLevel.isAtLeastB()) {
            expectedKeys.add(BAKLAVA);
        }
        Set<Integer> actualKeys = SdkExtensions.getAllExtensionVersions().keySet();
        assertWithHeader().that(actualKeys).containsExactlyElementsIn(expectedKeys);
    }

    @Test
    public void testExtensionR() throws Exception {
        Expectation expectation = dessertExpectation(true);
        assertVersion(expectation, R, "r");
    }

    @Test
    public void testExtensionS() throws Exception {
        Expectation expectation = dessertExpectation(SdkLevel.isAtLeastS());
        assertVersion(expectation, S, "s");
    }

    @Test
    public void testExtensionT() throws Exception {
        Expectation expectation = dessertExpectation(SdkLevel.isAtLeastT());
        assertVersion(expectation, TIRAMISU, "t");
    }

    @Test
    public void testExtensionU() throws Exception {
        Expectation expectation = dessertExpectation(SdkLevel.isAtLeastU());
        assertVersion(expectation, UPSIDE_DOWN_CAKE, "u");
    }

    @Test
    public void testExtensionV() throws Exception {
        Expectation expectation = dessertExpectation(SdkLevel.isAtLeastV());
        assertVersion(expectation, VANILLA_ICE_CREAM, "v");
    }

    @Test
    public void testExtensionB() throws Exception {
        Expectation expectation = dessertExpectation(SdkLevel.isAtLeastB());
        assertVersion(expectation, BAKLAVA, "b");
    }

    @Test
    public void testExtensionAdServices() throws Exception {
        int adServicesVersion = SdkExtensions.getExtensionVersion(AD_SERVICES);
        if (adServicesVersion >= 20) {
            // AD_SERVICES was discontinued after version 20
            return;
        }
        // Go trains do not ship the latest versions of AdServices, though they should. Temporarily
        // accept AT_LEAST_BASE of AdServices until the Go train situation has been resolved, then
        // revert back to expecting MISSING (before T) or CURRENT (on T+).
        Expectation expectation = dessertExpectation(SdkLevel.isAtLeastT());
        assertVersion(expectation, AD_SERVICES, "ad_services");
    }

    private Expectation dessertExpectation(boolean expectedPresent) throws Exception {
        if (!expectedPresent) {
            return MISSING;
        }
        // Go trains don't include all modules, so even when all trains for a particular release
        // have been installed correctly on a Go device, we can't generally expect the extension
        // version to be the current train version.
        return SdkLevel.isAtLeastT() && isGoWithSideloadedModules()
                ? AT_LEAST_BASE
                : AT_LEAST_CURRENT;
    }

    private boolean isGoWithSideloadedModules() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean isGoDevice = context.getSystemService(ActivityManager.class).isLowRamDevice();
        if (!isGoDevice) {
            return false;
        }

        PackageManager packageManager = context.getPackageManager();
        boolean anyApexesSideloaded = false;
        for (ModuleInfo module : packageManager.getInstalledModules(0)) {
            boolean sideloaded = isSideloadedApex(packageManager, module.getPackageName());
            anyApexesSideloaded |= sideloaded;
        }
        return anyApexesSideloaded;
    }

    private static boolean isSideloadedApex(PackageManager packageManager, String packageName)
            throws Exception {
        int flags = PackageManager.MATCH_APEX;
        PackageInfo currentInfo = packageManager.getPackageInfo(packageName, flags);
        if (!currentInfo.isApex) {
            return false;
        }
        flags |= PackageManager.MATCH_FACTORY_ONLY;
        PackageInfo factoryInfo = packageManager.getPackageInfo(packageName, flags);
        return !factoryInfo.applicationInfo.sourceDir.equals(currentInfo.applicationInfo.sourceDir);
    }
}
