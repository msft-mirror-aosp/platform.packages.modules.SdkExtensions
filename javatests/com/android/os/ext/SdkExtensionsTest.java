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
import static com.android.os.ext.testing.CurrentVersion.CURRENT_TRAIN_VERSION;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import android.os.SystemProperties;
import android.os.ext.SdkExtensions;
import androidx.test.runner.AndroidJUnit4;
import com.android.modules.utils.build.SdkLevel;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SdkExtensionsTest {

    private enum Expectation {
        /** Expect an extension to be the current / latest defined version */
        CURRENT,
        /** Expect an extension to be missing / version 0 */
        MISSING,
    }

    private static final Expectation CURRENT = Expectation.CURRENT;
    private static final Expectation MISSING = Expectation.MISSING;

    private static void assertVersion(Expectation expectation, int version) {
        switch (expectation) {
            case CURRENT:
                assertEquals(CURRENT_TRAIN_VERSION, version);
                break;
            case MISSING:
                assertEquals(0, version);
                break;
        }
    }

    private static void assertVersion(Expectation expectation, String propValue) {
        if (expectation == Expectation.MISSING) {
            assertEquals("", propValue);
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

    /** Verify that getExtensionVersion only accepts valid extension SDKs */
    @Test
    public void testBadArgument() throws Exception {
        // R is the first SDK version with extensions.
        for (int sdk = -1_000_000; sdk < VERSION_CODES.R; sdk++) {
            final int finalSdk = sdk;
            assertThrows(IllegalArgumentException.class,
                    () -> SdkExtensions.getExtensionVersion(finalSdk));
        }
    }

    /** Verifies that getExtensionVersion returns zero value for non-existing extensions */
    @Test
    public void testZeroValues() throws Exception {
        Set<Integer> assignedCodes = Set.of(R, S, TIRAMISU);
        for (int sdk = VERSION_CODES.R; sdk <= 1_000_000; sdk++) {
            if (assignedCodes.contains(sdk)) {
                continue;
            }
            // No extension SDKs yet.
            int version = SdkExtensions.getExtensionVersion(sdk);
            assertEquals("Extension ID " + sdk + " has non-zero version", 0, version);
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
        }
        Set<Integer> actualKeys = SdkExtensions.getAllExtensionVersions().keySet();
        assertThat(actualKeys).containsExactlyElementsIn(expectedKeys);
    }

    @Test
    public void testExtensionR() {
        assertVersion(Expectation.CURRENT, R, "r");
    }

    @Test
    public void testExtensionS() {
        Expectation expectation = SdkLevel.isAtLeastS() ? CURRENT : MISSING;
        assertVersion(expectation, S, "s");
    }

    @Test
    public void testExtensionT() {
        Expectation expectation = SdkLevel.isAtLeastT() ? CURRENT : MISSING;
        assertVersion(expectation, TIRAMISU, "t");
    }

}
