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
import static android.os.Build.VERSION_CODES.R;
import static android.os.Build.VERSION_CODES.S;
import static android.os.Build.VERSION_CODES.TIRAMISU;
import static android.os.ext.SdkExtensions.AD_SERVICES;
import static android.os.ext.SdkExtensions.getExtensionVersion;
import static com.android.os.ext.testing.CurrentVersion.ALLOWED_VERSIONS_CTS;
import static com.google.common.truth.Truth.assertThat;

import android.os.SystemProperties;
import android.os.ext.SdkExtensions;
import com.android.modules.utils.build.SdkLevel;
import junit.framework.TestCase;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SdkExtensionsTest extends TestCase {

    private static void assertCorrectVersion(int version) throws Exception {
        assertThat(ALLOWED_VERSIONS_CTS).contains(version);
    }

    private static void assertCorrectVersion(boolean expected, int version) throws Exception {
        if (expected) {
            assertCorrectVersion(version);
        } else {
            assertEquals(0, version);
        }
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
        for (int sdk = -1_000_000; sdk < VERSION_CODES.R; sdk++) {
            try {
                SdkExtensions.getExtensionVersion(sdk);
                fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) { }
        }
    }

    /** Verifies that getExtensionVersion only return existing versions */
    public void testValidValues() throws Exception {
        assertCorrectVersion(true, getExtensionVersion(R));
        assertCorrectVersion(SdkLevel.isAtLeastS(), getExtensionVersion(S));
        assertCorrectVersion(SdkLevel.isAtLeastT(), getExtensionVersion(TIRAMISU));
        assertCorrectVersion(SdkLevel.isAtLeastT(), getExtensionVersion(AD_SERVICES));

        Set<Integer> assignedCodes = Set.of(R, S, TIRAMISU, AD_SERVICES);
        for (int sdk = VERSION_CODES.R; sdk <= 1_000_000; sdk++) {
            if (assignedCodes.contains(sdk)) {
                continue;
            }
            // No extension SDKs yet.
            assertEquals(0, SdkExtensions.getExtensionVersion(sdk));
        }
    }

    /** Verifies that the public sysprops are set as expected */
    public void testSystemProperties() throws Exception {
        assertCorrectVersion(true, SystemProperties.get("build.version.extensions.r"));
        assertCorrectVersion(
            SdkLevel.isAtLeastS(), SystemProperties.get("build.version.extensions.s"));
        assertCorrectVersion(
            SdkLevel.isAtLeastT(), SystemProperties.get("build.version.extensions.t"));
    }

    public void testExtensionVersions() throws Exception {
        Map<Integer, Integer> versions = SdkExtensions.getAllExtensionVersions();
        Set<Integer> expectedKeys = new HashSet<>();
        assertCorrectVersion(versions.get(VERSION_CODES.R));
        expectedKeys.add(VERSION_CODES.R);

        if (SdkLevel.isAtLeastS()) {
            assertCorrectVersion(versions.get(VERSION_CODES.S));
            expectedKeys.add(VERSION_CODES.S);
        }
        if (SdkLevel.isAtLeastT()) {
            assertCorrectVersion(versions.get(VERSION_CODES.TIRAMISU));
            expectedKeys.add(VERSION_CODES.TIRAMISU);
            assertCorrectVersion(versions.get(AD_SERVICES));
            expectedKeys.add(AD_SERVICES);
        }
        assertThat(versions.keySet()).containsExactlyElementsIn(expectedKeys);
    }

}
