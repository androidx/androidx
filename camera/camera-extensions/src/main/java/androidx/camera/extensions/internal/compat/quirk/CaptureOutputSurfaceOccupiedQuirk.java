/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 * Bug Id: b/347142571
 * Description:
 * On Xiaomi 13 or 13T devices, it doesn't close the previous ImageWriter that connects to the
 * output surface when camera is closed. Therefore when suspending the app and resuming, same
 * ImageCapture surface is passed to the onOutputSurface and cause it failed to connect the
 * surface to the another ImageWriter.
 * Device(s): Xiaomi devices.
 *
 */
public class CaptureOutputSurfaceOccupiedQuirk implements Quirk {
    static boolean load() {
        return Build.BRAND.equalsIgnoreCase("Xiaomi");
    }
}
