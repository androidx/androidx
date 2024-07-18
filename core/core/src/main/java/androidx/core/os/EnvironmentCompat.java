/*
 * Copyright (C) 2011 The Android Open Source Project
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

package androidx.core.os;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;

/**
 * Helper for accessing features in {@link Environment}.
 */
public final class EnvironmentCompat {
    private static final String TAG = "EnvironmentCompat";

    /**
     * Unknown storage state, such as when a path isn't backed by known storage
     * media.
     *
     * @see #getStorageState(File)
     */
    public static final String MEDIA_UNKNOWN = "unknown";

    /**
     * Returns the current state of the storage device that provides the given
     * path.
     *
     * @return one of {@link #MEDIA_UNKNOWN}, {@link Environment#MEDIA_REMOVED},
     *         {@link Environment#MEDIA_UNMOUNTED},
     *         {@link Environment#MEDIA_CHECKING},
     *         {@link Environment#MEDIA_NOFS},
     *         {@link Environment#MEDIA_MOUNTED},
     *         {@link Environment#MEDIA_MOUNTED_READ_ONLY},
     *         {@link Environment#MEDIA_SHARED},
     *         {@link Environment#MEDIA_BAD_REMOVAL}, or
     *         {@link Environment#MEDIA_UNMOUNTABLE}.
     */
    @SuppressWarnings("deprecation")
    @NonNull
    public static String getStorageState(@NonNull File path) {
        if (Build.VERSION.SDK_INT >= 21) {
            return Api21Impl.getExternalStorageState(path);
        } else if (Build.VERSION.SDK_INT >= 19) {
            return Api19Impl.getStorageState(path);
        }

        try {
            final String canonicalPath = path.getCanonicalPath();
            @SuppressWarnings("deprecation")
            final String canonicalExternal = Environment.getExternalStorageDirectory()
                    .getCanonicalPath();

            if (canonicalPath.startsWith(canonicalExternal)) {
                return Environment.getExternalStorageState();
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to resolve canonical path: " + e);
        }

        return MEDIA_UNKNOWN;
    }

    private EnvironmentCompat() {
    }

    @RequiresApi(21)
    static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static String getExternalStorageState(File path) {
            return Environment.getExternalStorageState(path);
        }
    }

    @RequiresApi(19)
    static class Api19Impl {
        private Api19Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static String getStorageState(File path) {
            return Environment.getStorageState(path);
        }
    }
}
