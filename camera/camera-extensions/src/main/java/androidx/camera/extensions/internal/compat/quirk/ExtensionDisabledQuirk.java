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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;
import androidx.camera.extensions.ExtensionMode;


/**
 * <p>QuirkSummary
 *     Bug Id: b/199408131, b/214130117
 *     Description: Quirk required to disable extension for some devices. An example is that
 *                  Pixel 5's availability check result of the basic extension interface should
 *                  be false, but it actually returns true. Therefore, force disable Basic
 *                  Extender capability on the device. Another example is that Motorola razr 5G's
 *                  availability check results of both back and front camera are true, but it
 *                  will cause the black preview screen issue. Therefore, force disable the bokeh
 *                  mode on the device.
 *     Device(s): Pixel 5, Motorola razr 5G
 *     @see androidx.camera.extensions.internal.compat.workaround.ExtensionDisabledValidator
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExtensionDisabledQuirk implements Quirk {
    static boolean load() {
        return isPixel5() || isMotoRazr5G();
    }

    /**
     * Checks whether extension should be disabled.
     */
    public boolean shouldDisableExtension(@NonNull String cameraId,
            @ExtensionMode.Mode int extensionMode, boolean isAdvancedInterface) {
        if (isPixel5() && !isAdvancedInterface) {
            // 1. Disables Pixel 5's Basic Extender capability.
            return true;
        } else if (isMotoRazr5G() && ("0".equals(cameraId) || "1".equals(cameraId)) && (
                ExtensionMode.BOKEH == extensionMode)) {
            // 2. Disables Motorola Razr 5G's bokeh capability.
            return true;
        }

        return false;
    }

    private static boolean isPixel5() {
        return "google".equalsIgnoreCase(Build.BRAND) && "redfin".equalsIgnoreCase(Build.DEVICE);
    }

    private static boolean isMotoRazr5G() {
        return "motorola".equalsIgnoreCase(Build.BRAND) && "smith".equalsIgnoreCase(Build.DEVICE);
    }
}
