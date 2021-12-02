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

/**
 * Quirk required to disable extension for some devices.
 *
 * <p>An example is Pixel 5 which the availability check result of the basic extension interface
 * face should be false, but it actually return true. Therefore, a default VendorExtender will
 * be used to return false availability check result. See b/199408131.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExtensionDisabledQuirk implements Quirk {
    static boolean load() {
        return isPixel5();
    }

    /**
     * Checks whether extension should be disabled.
     */
    public boolean shouldDisableExtension(boolean isAdvancedExtenderSupported) {
        return !isAdvancedExtenderSupported && isPixel5();
    }

    private static boolean isPixel5() {
        return "google".equalsIgnoreCase(Build.BRAND) && "redfin".equalsIgnoreCase(Build.DEVICE);
    }
}
