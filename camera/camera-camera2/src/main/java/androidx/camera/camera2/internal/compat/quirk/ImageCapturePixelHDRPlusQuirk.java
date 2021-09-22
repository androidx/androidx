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

package androidx.camera.camera2.internal.compat.quirk;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.Quirk;

/**
 * Quirk required to turn on/off HDR+ on Pixel devices by enabling/disabling zero-shutter-lag
 * (ZSL) mode on the capture request, depending on the image capture use case's capture mode, i.e.
 * prioritizing image capture latency over quality, or vice versa. This means that when the
 * capture mode is {@link ImageCapture#CAPTURE_MODE_MINIMIZE_LATENCY}, HDR+ is turned off by
 * disabling ZSL, and when it is {@link ImageCapture#CAPTURE_MODE_MAXIMIZE_QUALITY}, HDR+ is
 * turned on by enabling ZSL.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ImageCapturePixelHDRPlusQuirk implements Quirk {

    static boolean load() {
        return (isPixel2() || isPixel3()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    private static boolean isPixel2() {
        return "Google".equals(Build.MANUFACTURER) && "Pixel 2".equals(Build.MODEL);
    }

    private static boolean isPixel3() {
        return "Google".equals(Build.MANUFACTURER) && "Pixel 3".equals(Build.MODEL);
    }
}
