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

package android.os.ext;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Build.VERSION_CODES;
import android.os.SystemProperties;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Methods for interacting with the extension SDK.
 *
 * This class provides information about the extension SDK version present
 * on this device. Use the {@link #getExtensionVersion(int) getExtension} to
 * query for the extension version for the given SDK version.

 * @hide
 */
@SystemApi
public class SdkExtensions {

    private static final int R_EXTENSION_INT;
    private static final int S_EXTENSION_INT;
    static {
        // Note: when adding more extension versions, the logic that records current
        // extension versions when saving a rollback must also be updated.
        // At the time of writing this is in RollbackManagerServiceImpl#getExtensionVersions()
        R_EXTENSION_INT = SystemProperties.getInt("build.version.extensions.r", 0);
        S_EXTENSION_INT = SystemProperties.getInt("build.version.extensions.s", 0);
    }

    /**
     * Values suitable as parameters for {@link #getExtensionVersion(int)}.
     * @hide
     */
    @IntDef(value = { VERSION_CODES.R, VERSION_CODES.CUR_DEVELOPMENT })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Extension {}

    /** The current set of extensions. */
    @NonNull
    @Extension
    public static final int[] EXTENSIONS = { VERSION_CODES.R, VERSION_CODES.CUR_DEVELOPMENT };

    private SdkExtensions() { }

    /**
     * Return the version of the specified extensions.
     *
     * @param extension the extension to get the version of.
     * @throws IllegalArgumentException if extension is not a valid extension
     */
    public static int getExtensionVersion(@Extension int extension) {
        if (extension < VERSION_CODES.R) {
            throw new IllegalArgumentException("not a valid extension: " + extension);
        }

        if (extension == VERSION_CODES.R) {
            return R_EXTENSION_INT;
        }
        if (extension == VERSION_CODES.CUR_DEVELOPMENT) {
            return S_EXTENSION_INT;
        }
        return 0;
    }

}
