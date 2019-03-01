/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

class WarningState {
    private static final String TAG = "Benchmark";

    static final String WARNING_PREFIX;
    private static String sWarningString;

    static {
        ApplicationInfo appInfo = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getApplicationInfo();
        String warningPrefix = "";
        String warningString = "";
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            warningPrefix += "DEBUGGABLE_";
            warningString += "\nWARNING: Debuggable Benchmark\n"
                    + "    Benchmark is running with debuggable=true, which drastically reduces\n"
                    + "    runtime performance in order to support debugging features. Run\n"
                    + "    benchmarks with debuggable=false. Debuggable affects execution speed\n"
                    + "    in ways that mean benchmark improvements might not carry over to a\n"
                    + "    real user's experience (or even regress release performance).\n";
        }
        if (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT)) {
            warningPrefix += "EMULATOR_";
            warningString += "\nWARNING: Running on Emulator\n"
                    + "    Benchmark is running on an emulator, which is not representative of\n"
                    + "    real user devices. Use a physical device to benchmark. Emulator\n"
                    + "    benchmark improvements might not carry over to a real user's\n"
                    + "    experience (or even regress real device performance).\n";
        }
        if (Build.FINGERPRINT.contains(":eng/")) {
            warningPrefix += "ENG-BUILD_";
            warningString += "\nWARNING: Running on Eng Build\n"
                    + "    Benchmark is running on device flashed with a '-eng' build. Eng builds\n"
                    + "    of the platform drastically reduce performance to enable testing\n"
                    + "    changes quickly. For this reason they should not be used for\n"
                    + "    benchmarking. Use a '-user' or '-userdebug' system image.\n";
        }
        WARNING_PREFIX = warningPrefix;

        if (!warningString.isEmpty()) {
            sWarningString = warningString;
            for (String line : sWarningString.split("\n")) {
                Log.w(TAG, line);
            }
            Log.w(TAG, "");
        }
    }

    @Nullable
    static String acquireWarningStringForLogging() {
        String ret = sWarningString;
        sWarningString = null;
        return ret;
    }
}
