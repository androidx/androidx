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

package androidx.biometric.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Utilities related to the system {@link PackageManager}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PackageUtils {
    // Prevent instantiation.
    private PackageUtils() {
    }

    /**
     * Checks if the current device supports fingerprint authentication.
     *
     * @param context The application or activity context.
     * @return Whether fingerprint is supported.
     */
    public static boolean hasSystemFeatureFingerprint(@Nullable Context context) {
        return context != null
                && context.getPackageManager() != null
                && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
    }

    /**
     * Checks if the current device supports fingerprint authentication.
     *
     * @param context The application or activity context.
     * @return Whether fingerprint is supported.
     */
    public static boolean hasSystemFeatureFace(@Nullable Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && context != null
                && context.getPackageManager() != null
                && Api29Impl.hasSystemFeatureFace(context.getPackageManager());
    }

    /**
     * Checks if the current device supports fingerprint authentication.
     *
     * @param context The application or activity context.
     * @return Whether fingerprint is supported.
     */
    public static boolean hasSystemFeatureIris(@Nullable Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && context != null
                && context.getPackageManager() != null
                && Api29Impl.hasSystemFeatureIris(context.getPackageManager());
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 6.0 (API 23).
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private static class Api29Impl {
        // Prevent instantiation.
        private Api29Impl() {
        }

        /**
         * Checks if the given package manager has support for the face system feature.
         *
         * @param packageManager The system package manager.
         * @return Whether face is supported.
         */
        static boolean hasSystemFeatureFace(@NonNull PackageManager packageManager) {
            return packageManager.hasSystemFeature(PackageManager.FEATURE_FACE);
        }

        /**
         * Checks if the given package manager has support for the iris system feature.
         *
         * @param packageManager The system package manager.
         * @return Whether iris is supported.
         */
        static boolean hasSystemFeatureIris(@NonNull PackageManager packageManager) {
            return packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS);
        }
    }
}
