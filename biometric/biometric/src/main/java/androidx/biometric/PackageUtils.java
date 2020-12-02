/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.biometric;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Utilities related to the system {@link PackageManager}.
 */
class PackageUtils {
    // Prevent instantiation.
    private PackageUtils() {}

    /**
     * Checks if the current device supports fingerprint authentication.
     *
     * @param context The application or activity context.
     * @return Whether fingerprint is supported.
     */
    static boolean hasSystemFeatureFingerprint(@Nullable Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && context != null
                && context.getPackageManager() != null
                && Api23Impl.hasSystemFeatureFingerprint(context.getPackageManager());
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 6.0 (API 23).
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private static class Api23Impl {
        // Prevent instantiation.
        private Api23Impl() {}

        /**
         * Checks if the given package manager has support for the fingerprint system feature.
         *
         * @param packageManager The system package manager.
         * @return Whether fingerprint is supported.
         */
        static boolean hasSystemFeatureFingerprint(@NonNull PackageManager packageManager) {
            return packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
        }
    }
}
