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

package android.os.ext.cts;

import android.os.Build;
import android.os.SystemProperties;
import android.os.ext.SdkExtensions;
import com.android.modules.utils.build.SdkLevel;
import junit.framework.TestCase;
import java.util.Map;
import java.util.Set;

public class SdkExtensionsTest extends TestCase {

    /** The version R shipped with (0) */
    public static final int R_BASE_VERSION = 0;

    /** The version S shipped with (1) */
    public static final int S_BASE_VERSION = 1;

    private static void assertCorrectVersion(int version) throws Exception {
        int minVersion = R_BASE_VERSION;
        if (SdkLevel.isAtLeastS()) {
            minVersion = S_BASE_VERSION;
        }
        assertTrue(version >= minVersion);
    }
    private static void assertCorrectVersion(boolean expected, String propValue) throws Exception {
        if (expected) {
            int version = Integer.parseInt(propValue);
            assertCorrectVersion(version);
        } else {
            assertEquals("", propValue);
        }
    }

    /** Verify that getExtensionVersion only accepts valid extension SDKs */
    public void testBadArgument() throws Exception {
        // R is the first SDK version with extensions.
        for (int sdk = -1_000_000; sdk < Build.VERSION_CODES.R; sdk++) {
            try {
                SdkExtensions.getExtensionVersion(sdk);
                fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) { }
        }
    }

    /** Verifies that getExtensionVersion only return existing versions */
    public void testValidValues() throws Exception {
        assertCorrectVersion(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R));
        assertCorrectVersion(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S));

        int firstUnassigned = Build.VERSION_CODES.S + 1;
        for (int sdk = firstUnassigned; sdk <= 1_000_000; sdk++) {
            // No extension SDKs yet.
            assertEquals(0, SdkExtensions.getExtensionVersion(sdk));
        }
    }

    /** Verifies that the public sysprops are set as expected */
    public void testSystemProperties() throws Exception {
        assertCorrectVersion(true, SystemProperties.get("build.version.extensions.r"));
        assertCorrectVersion(
            SdkLevel.isAtLeastS(), SystemProperties.get("build.version.extensions.s"));
    }

    public void testExtensionVersions() throws Exception {
        Map<Integer, Integer> versions = SdkExtensions.getAllExtensionVersions();
        int expectedSize = 1;
        assertCorrectVersion(versions.get(Build.VERSION_CODES.R));

        if (SdkLevel.isAtLeastS()) {
            assertCorrectVersion(versions.get(Build.VERSION_CODES.S));
            expectedSize++;
        }
        assertEquals(expectedSize, versions.size());
    }

}
