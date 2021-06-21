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

package androidx.camera.extensions.internal;

import static androidx.camera.extensions.ExtensionMode.AUTO;
import static androidx.camera.extensions.ExtensionMode.BEAUTY;
import static androidx.camera.extensions.ExtensionMode.BOKEH;
import static androidx.camera.extensions.ExtensionMode.HDR;
import static androidx.camera.extensions.ExtensionMode.NIGHT;
import static androidx.camera.extensions.ExtensionMode.NONE;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.impl.AutoImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BeautyImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BokehImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.HdrImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.NightImageCaptureExtenderImpl;

/**
 * A Extensions util class to provide related util functions.
 */
public final class ExtensionsUtil {
    private ExtensionsUtil() { }

    /**
     * Returns a ImageCaptureExtenderImpl instance for specific camera and extensions mode.
     *
     * @param cameraId The target camera id.
     * @param cameraCharacteristics The camera characteristic of the target camera.
     * @param mode The target extensions mode.
     * @return a ImageCaptureExtenderImpl object corresponding to the extensions mode. Returns
     * null if the input extensions mode is not supported or the vendor library doesn't implement
     * the class.
     */
    @Nullable
    public static ImageCaptureExtenderImpl createImageCaptureExtenderImpl(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics,
            @ExtensionMode.Mode int mode) {

        try {
            ImageCaptureExtenderImpl impl;

            switch (mode) {
                case BOKEH:
                    impl = new BokehImageCaptureExtenderImpl();
                    break;
                case HDR:
                    impl = new HdrImageCaptureExtenderImpl();
                    break;
                case NIGHT:
                    impl = new NightImageCaptureExtenderImpl();
                    break;
                case BEAUTY:
                    impl = new BeautyImageCaptureExtenderImpl();
                    break;
                case AUTO:
                    impl = new AutoImageCaptureExtenderImpl();
                    break;
                case NONE:
                default:
                    return null;
            }

            impl.init(cameraId, cameraCharacteristics);

            return impl;
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }
}
