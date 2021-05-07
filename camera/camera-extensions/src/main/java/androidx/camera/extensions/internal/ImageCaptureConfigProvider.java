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

package androidx.camera.extensions.internal;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.ImageCaptureExtender;
import androidx.camera.extensions.impl.AutoImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BeautyImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BokehImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.HdrImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.NightImageCaptureExtenderImpl;

/**
 * Provides extensions related configs for image capture
 */
public class ImageCaptureConfigProvider implements ConfigProvider<ImageCaptureConfig> {
    private ImageCaptureExtenderImpl mImpl;
    private Context mContext;
    @ExtensionMode.Mode
    private int mEffectMode;

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    public ImageCaptureConfigProvider(@ExtensionMode.Mode int mode,
            @NonNull CameraInfo cameraInfo, @NonNull Context context) {
        try {
            switch (mode) {
                case ExtensionMode.BOKEH:
                    mImpl = new BokehImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.HDR:
                    mImpl = new HdrImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.NIGHT:
                    mImpl = new NightImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.BEAUTY:
                    mImpl = new BeautyImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.AUTO:
                    mImpl = new AutoImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.NONE:
                default:
                    return;
            }
        } catch (NoClassDefFoundError e) {
            throw new IllegalArgumentException("Extension mode does not exist: " + mode);
        }
        mEffectMode = mode;
        mContext = context;

        String cameraId = Camera2CameraInfo.from(cameraInfo).getCameraId();
        CameraCharacteristics cameraCharacteristics =
                Camera2CameraInfo.extractCameraCharacteristics(cameraInfo);
        mImpl.init(cameraId, cameraCharacteristics);
    }
    @NonNull
    @Override
    public ImageCaptureConfig getConfig() {
        if (mImpl == null) {
            return new ImageCaptureConfig(OptionsBundle.emptyBundle());
        }

        ImageCapture.Builder builder = new ImageCapture.Builder();

        ImageCaptureExtender.updateBuilderConfig(builder, mEffectMode, mImpl, mContext);

        return builder.getUseCaseConfig();
    }
}
