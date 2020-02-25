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

package androidx.window;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.window.extensions.ExtensionInterface;

/**
 * Helper class that loads the correct {@link ExtensionInterfaceCompat}.
 */
final class ExtensionHelper {
    static final boolean DEBUG = false;
    private static final String TAG = "WindowExtensionHelper";

    private ExtensionHelper() {}

    /**
     * Get an instance of {@link ExtensionInterface} implemented by OEM if available on this device.
     */
    static ExtensionInterfaceCompat getExtensionImpl(Context context) {
        ExtensionInterfaceCompat impl = null;
        try {
            if (isExtensionVersionSupported(ExtensionCompat.getExtensionVersion())) {
                impl = new ExtensionCompat(context);
            } else if (isExtensionVersionSupported(SidecarCompat.getSidecarVersion())) {
                impl = new SidecarCompat(context);
            }
        } catch (Throwable t) {
            if (DEBUG) {
                Log.d(TAG, "Failed to load extension: " + t);
            }
            return null;
        }

        if (impl == null) {
            if (DEBUG) {
                Log.d(TAG, "No supported extension found");
            }
            return null;
        }

        if (!impl.validateExtensionInterface()) {
            if (DEBUG) {
                Log.d(TAG, "Loaded extension doesn't match the interface version");
            }
            return null;
        }

        return impl;
    }

    /**
     * Check if the Extension version provided on this device is supported by the current version
     * of the library.
     */
    private static boolean isExtensionVersionSupported(@Nullable Version extensionVersion) {
        return extensionVersion != null
                && Version.CURRENT.getMajor() >= extensionVersion.getMajor();
    }
}

