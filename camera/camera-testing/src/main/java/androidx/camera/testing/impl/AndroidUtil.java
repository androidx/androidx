/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl;

import static org.junit.Assume.assumeFalse;

import android.os.Build;

/**
 * Utility methods for testing related to Android OS.
 */
public final class AndroidUtil {

    private AndroidUtil() {
    }

    /**
     * Checks if the current device is emulator.
     */
    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("gphone")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Cuttlefish")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.PRODUCT.equals("google_sdk")
                || Build.HARDWARE.contains("ranchu");
    }

    /**
     * Checks if the current device is emulator with API 21.
     */
    public static boolean isEmulatorAndAPI21() {
        return Build.VERSION.SDK_INT == 21 && isEmulator();
    }

    /**
     * Checks if the current device is emulator with API 21.
     */
    public static boolean isEmulator(int apiLevel) {
        return Build.VERSION.SDK_INT == apiLevel && isEmulator();
    }

    /**
     * Skips the test if the current device is emulator that doesn't support video recording.
     */
    public static void skipVideoRecordingTestIfNotSupportedByEmulator() {
        // Skip test for b/168175357, b/233661493
        assumeFalse(
                "Cuttlefish has MediaCodec dequeInput/Output buffer fails issue. Unable to test.",
                Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29
        );
        // Skip test for b/268102904
        assumeFalse(
                "Emulator API 21 has empty supported qualities. Unable to test.",
                AndroidUtil.isEmulatorAndAPI21()
        );
        // Skip test for b/331618729
        assumeFalse(
                "Emulator API 28 crashes running this test.",
                Build.VERSION.SDK_INT == 28 && isEmulator()
        );
        // Skip test for b/331618729
        assumeFalse(
                "Emulator API 30 crashes running this test.",
                Build.VERSION.SDK_INT == 30 && isEmulator()
        );
    }
}
