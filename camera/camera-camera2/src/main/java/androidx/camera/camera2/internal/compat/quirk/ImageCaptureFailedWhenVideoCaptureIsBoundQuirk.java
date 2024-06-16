/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.camera.core.internal.compat.quirk.SurfaceProcessingQuirk;

/**
 * <p>QuirkSummary
 *     Bug Id: b/239369953, b/331754902, b/338869048, b/339555742, b/336925549
 *     Description: When taking image with VideoCapture is bound, the capture result is returned
 *                  but the resulting image can not be obtained. On Pixel 4XL API29, taking image
 *                  with VideoCapture UHD is bound, camera HAL returns error. Pixel 4XL starts
 *                  from API29 and API30+ work fine.
 *                  On Moto E13, taking picture will time out after recording is started, even if
 *                  the recording is stopped.
 *                  On Samsung Tab A8, apps can't take pictures successfully when ImageCapture
 *                  selects 1920x1080 under Preview + VideoCapture + ImageCapture UseCase
 *                  combination.
 *     Device(s): BLU Studio X10, Itel w6004, Twist 2 Pro, and Vivo 1805, Pixel 4XL API29, Moto
 *                E13, Samsung Tab A8
 */
public class ImageCaptureFailedWhenVideoCaptureIsBoundQuirk implements CaptureIntentPreviewQuirk,
        SurfaceProcessingQuirk {

    static boolean load() {
        return isBluStudioX10() || isItelW6004() || isVivo1805() || isPositivoTwist2Pro()
                || isPixel4XLApi29() || isMotoE13() || isSamsungTabA8();
    }

    public static boolean isBluStudioX10() {
        return "blu".equalsIgnoreCase(Build.BRAND) && "studio x10".equalsIgnoreCase(Build.MODEL);
    }

    public static boolean isItelW6004() {
        return "itel".equalsIgnoreCase(Build.BRAND) && "itel w6004".equalsIgnoreCase(Build.MODEL);
    }

    public static boolean isVivo1805() {
        return "vivo".equalsIgnoreCase(Build.BRAND) && "vivo 1805".equalsIgnoreCase(Build.MODEL);
    }

    public static boolean isPositivoTwist2Pro() {
        return "positivo".equalsIgnoreCase(Build.BRAND) && "twist 2 pro".equalsIgnoreCase(
                Build.MODEL);
    }

    private static boolean isPixel4XLApi29() {
        return "pixel 4 xl".equalsIgnoreCase(Build.MODEL) && Build.VERSION.SDK_INT == 29;
    }

    public static boolean isMotoE13() {
        return "motorola".equalsIgnoreCase(Build.BRAND) && "moto e13".equalsIgnoreCase(
                Build.MODEL);
    }

    public static boolean isSamsungTabA8() {
        return "samsung".equalsIgnoreCase(Build.BRAND) && ("gta8".equalsIgnoreCase(Build.DEVICE)
                || "gta8wifi".equalsIgnoreCase(Build.DEVICE));
    }

    @Override
    public boolean workaroundByCaptureIntentPreview() {
        return isBluStudioX10() || isItelW6004() || isVivo1805() || isPositivoTwist2Pro();
    }

    @Override
    public boolean workaroundBySurfaceProcessing() {
        return isBluStudioX10() || isItelW6004() || isVivo1805() || isPositivoTwist2Pro()
                || isPixel4XLApi29() || isMotoE13() || isSamsungTabA8();
    }
}
