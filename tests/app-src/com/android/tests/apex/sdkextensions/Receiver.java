/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.ext.SdkExtensions;
import android.os.ext.test.Test;
import android.util.Log;

public class Receiver extends BroadcastReceiver {

    private static final String ACTION_GET_SDK_VERSION =
            "com.android.tests.apex.sdkextensions.GET_SDK_VERSION";
    private static final String ACTION_MAKE_CALLS_0 =
            "com.android.tests.apex.sdkextensions.MAKE_CALLS_0";
    private static final String ACTION_MAKE_CALLS_45 =
            "com.android.tests.apex.sdkextensions.MAKE_CALLS_45";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            switch (intent.getAction()) {
                case ACTION_GET_SDK_VERSION:
                    int sdkVersion = SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R);
                    setResultData(String.valueOf(sdkVersion));
                    break;
                case ACTION_MAKE_CALLS_0:
                    makeCallsVersion0();
                    setResultData("true");
                    break;
                case ACTION_MAKE_CALLS_45:
                    makeCallsVersion45();
                    setResultData("true");
                    break;
            }
        } catch (Throwable e) {
            Log.e("SdkExtensionsE2E", "Unexpected error/exception", e);
            setResultData("Unexpected error or exception in test app, see log for details");
        }
    }

    private static void makeCallsVersion0() {
        expectException(NoClassDefFoundError.class, "test class", () -> new Test() );
    }

    private static void makeCallsVersion45() {
        Test test = new Test();

        test.publicMethod();
        test.systemApiMethod();
        expectException(NoSuchMethodError.class, "module method", () -> test.moduleLibsApiMethod());
        expectException(NoSuchMethodError.class, "testapi method", () -> test.testApiMethod());
        expectException(NoSuchMethodError.class, "hidden method", () -> test.hiddenMethod());
    }

    private static void expectException(Class exceptionClass, String type, Runnable runnable) {
        boolean success = false;
        try {
            runnable.run();
            success = true;
        } catch (Throwable e) {
            if (!e.getClass().equals(exceptionClass)) {
                throw new IllegalStateException("Saw unexpected exception", e);
            }
        }
        if (success) {
            throw new IllegalStateException("Accessed " + type + ", but shouldn't be able to");
        }
    }
}
