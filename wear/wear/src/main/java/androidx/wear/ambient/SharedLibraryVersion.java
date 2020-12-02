/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.wear.ambient;

import android.os.Build;

import androidx.annotation.VisibleForTesting;

import com.google.android.wearable.WearableSharedLib;

/**
 * Internal class which can be used to determine the version of the wearable shared library that is
 * available on the current device.
 */
final class SharedLibraryVersion {

    private SharedLibraryVersion() {
    }

    /**
     * Returns the version of the wearable shared library available on the current device.
     * <p>
     * <p>Version 1 was introduced on 2016-09-26, so any previous shared library will return 0. In
     * those cases, it may be necessary to check {@code Build.VERSION.SDK_INT}.
     *
     * @throws IllegalStateException if the Wearable Shared Library is not present, which means that
     * the {@code <uses-library>} tag is missing.
     */
    public static int version() {
        verifySharedLibraryPresent();
        return VersionHolder.VERSION;
    }

    /**
     * Throws {@link IllegalStateException} if the Wearable Shared Library is not present and API
     * level is at least LMP MR1.
     * <p>
     * <p>This validates that the developer hasn't forgotten to include a {@code <uses-library>} tag
     * in their manifest. The method should be used in combination with API level checks for
     * features added before {@link #version() version} 1.
     */
    public static void verifySharedLibraryPresent() {
        if (!PresenceHolder.PRESENT) {
            throw new IllegalStateException("Could not find wearable shared library classes. "
                    + "Please add <uses-library android:name=\"com.google.android.wearable\" "
                    + "android:required=\"false\" /> to the application manifest");
        }
    }

    // Lazy initialization holder class (see Effective Java item 71)
    @VisibleForTesting
    static final class VersionHolder {
        static final int VERSION = getSharedLibVersion(Build.VERSION.SDK_INT);

        @VisibleForTesting
        static int getSharedLibVersion(int sdkInt) {
            if (sdkInt < Build.VERSION_CODES.N_MR1) {
                // WearableSharedLib was introduced in N MR1 (Wear FDP 4)
                return 0;
            }
            return WearableSharedLib.version();
        }

        private VersionHolder() {
        }
    }

    // Lazy initialization holder class (see Effective Java item 71)
    @VisibleForTesting
    static final class PresenceHolder {
        static final boolean PRESENT = isSharedLibPresent(Build.VERSION.SDK_INT);

        @VisibleForTesting
        static boolean isSharedLibPresent(int sdkInt) {
            try {
                // A class which has been available on the shared library from the first version.
                Class.forName("com.google.android.wearable.compat.WearableActivityController");
            } catch (ClassNotFoundException e) {
                return false;
            }
            return true;
        }

        private PresenceHolder() {
        }
    }
}
