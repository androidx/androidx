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

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.impl.AutoImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.AutoPreviewExtenderImpl;
import androidx.camera.extensions.impl.BeautyImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BeautyPreviewExtenderImpl;
import androidx.camera.extensions.impl.BokehImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BokehPreviewExtenderImpl;
import androidx.camera.extensions.impl.HdrImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.HdrPreviewExtenderImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.NightImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.NightPreviewExtenderImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;

import java.util.Map;

/**
 * Basic vendor interface implementation
 */
public class BasicVendorExtender implements VendorExtender {
    private final PreviewExtenderImpl mPreviewExtenderImpl;
    private final ImageCaptureExtenderImpl mImageCaptureExtenderImpl;

    public BasicVendorExtender(@ExtensionMode.Mode int mode) {
        try {
            switch (mode) {
                case ExtensionMode.BOKEH:
                    mPreviewExtenderImpl = new BokehPreviewExtenderImpl();
                    mImageCaptureExtenderImpl = new BokehImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.HDR:
                    mPreviewExtenderImpl = new HdrPreviewExtenderImpl();
                    mImageCaptureExtenderImpl = new HdrImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.NIGHT:
                    mPreviewExtenderImpl = new NightPreviewExtenderImpl();
                    mImageCaptureExtenderImpl = new NightImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.BEAUTY:
                    mPreviewExtenderImpl = new BeautyPreviewExtenderImpl();
                    mImageCaptureExtenderImpl = new BeautyImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.AUTO:
                    mPreviewExtenderImpl = new AutoPreviewExtenderImpl();
                    mImageCaptureExtenderImpl = new AutoImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.NONE:
                default:
                    throw new IllegalArgumentException("Should not active ExtensionMode.NONE");
            }
        } catch (NoClassDefFoundError e) {
            throw new IllegalArgumentException("Extension mode does not exist: " + mode);
        }
    }

    /**
     * Return the {@link PreviewExtenderImpl} instance. This method will be removed once the
     * existing basic extender implementation is migrated to the unified vendor extender.
     */
    @NonNull
    public PreviewExtenderImpl getPreviewExtenderImpl() {
        return mPreviewExtenderImpl;
    }

    /**
     * Return the {@link ImageCaptureExtenderImpl} instance. This method will be removed once the
     * existing basic extender implementation is migrated to the unified vendor extender.
     */
    @NonNull
    public ImageCaptureExtenderImpl getImageCaptureExtenderImpl() {
        return mImageCaptureExtenderImpl;
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> characteristicsMap) {
        CameraCharacteristics cameraCharacteristics = characteristicsMap.get(cameraId);
        return mPreviewExtenderImpl.isExtensionAvailable(cameraId, cameraCharacteristics)
                && mImageCaptureExtenderImpl.isExtensionAvailable(cameraId, cameraCharacteristics);
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @Override
    public void init(@NonNull CameraInfo cameraInfo) {
        String cameraId = Camera2CameraInfo.from(cameraInfo).getCameraId();
        CameraCharacteristics cameraCharacteristics =
                Camera2CameraInfo.extractCameraCharacteristics(cameraInfo);
        mPreviewExtenderImpl.init(cameraId, cameraCharacteristics);
        mImageCaptureExtenderImpl.init(cameraId, cameraCharacteristics);
    }
}
