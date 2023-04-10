/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.extensions.internal.compat.quirk;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;
import androidx.camera.extensions.internal.ExtensionVersion;
import androidx.camera.extensions.internal.Version;


/**
 * <p>QuirkSummary
 * Bug Id: b/199408131, b/214130117, b/255956506
 * Description: Quirk required to disable extension for some devices. An example is that
 * Pixel 5's availability check result of the basic extension interface should
 * be false, but it actually returns true. Therefore, force disable Basic
 * Extender capability on the device. Another example is to ensure Motorola devices meet the
 * minimum quality requirements for camera extensions support. Common issues encountered with
 * Motorola extensions include: Bokeh not supported on some devices, SurfaceView not supported,
 * Image doesn't appear after taking a picture, Preview is pauses after resuming.
 * Device(s): Pixel 5, Motorola
 *
 * @see androidx.camera.extensions.internal.compat.workaround.ExtensionDisabledValidator
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExtensionDisabledQuirk implements Quirk {
    private final boolean mIsAdvancedInterface = isAdvancedExtenderSupported();

    static boolean load() {
        return isPixel5() || isMoto() || isAdvancedExtenderSupported();
    }

    /**
     * Checks whether extension should be disabled.
     */
    public boolean shouldDisableExtension() {
        if (isPixel5() && !mIsAdvancedInterface) {
            // 1. Disables Pixel 5's Basic Extender capability.
            return true;
        } else if (isMoto() && ExtensionVersion.isMaximumCompatibleVersion(Version.VERSION_1_1)) {
            // 2. Disables Motorola extensions capability for version 1.1 and older.
            return true;
        }

        return false;
    }

    private static boolean isPixel5() {
        return "google".equalsIgnoreCase(Build.BRAND) && "redfin".equalsIgnoreCase(Build.DEVICE);
    }

    private static boolean isMoto() {
        return "motorola".equalsIgnoreCase(Build.BRAND);
    }

    private static boolean isAdvancedExtenderSupported() {
        return ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_2)
                && ExtensionVersion.isAdvancedExtenderSupported();
    }
}
