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
import android.util.Pair;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.extensions.Extensions;
import androidx.camera.extensions.ImageCaptureExtender;
import androidx.camera.extensions.impl.AutoImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BeautyImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BokehImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.CaptureProcessorImpl;
import androidx.camera.extensions.impl.HdrImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.NightImageCaptureExtenderImpl;

import java.util.List;

/**
 * Provides extensions related configs for image capture
 */
public class ImageCaptureConfigProvider implements ConfigProvider<ImageCaptureConfig> {
    private ImageCaptureExtenderImpl mImpl;
    private Context mContext;

    @UseExperimental(markerClass = ExperimentalCamera2Interop.class)
    public ImageCaptureConfigProvider(@Extensions.ExtensionMode int mode,
            @NonNull CameraInfo cameraInfo, @NonNull Context context) {
        try {
            switch (mode) {
                case Extensions.EXTENSION_MODE_BOKEH:
                    mImpl = new BokehImageCaptureExtenderImpl();
                    break;
                case Extensions.EXTENSION_MODE_HDR:
                    mImpl = new HdrImageCaptureExtenderImpl();
                    break;
                case Extensions.EXTENSION_MODE_NIGHT:
                    mImpl = new NightImageCaptureExtenderImpl();
                    break;
                case Extensions.EXTENSION_MODE_BEAUTY:
                    mImpl = new BeautyImageCaptureExtenderImpl();
                    break;
                case Extensions.EXTENSION_MODE_AUTO:
                    mImpl = new AutoImageCaptureExtenderImpl();
                    break;
                case Extensions.EXTENSION_MODE_NONE:
                default:
                    return;
            }
        } catch (NoClassDefFoundError e) {
            throw new IllegalArgumentException("Extension mode does not exist: " + mode);
        }
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

        CaptureProcessorImpl captureProcessor = mImpl.getCaptureProcessor();
        if (captureProcessor != null) {
            builder.setCaptureProcessor(new AdaptingCaptureProcessor(captureProcessor));
        }

        if (mImpl.getMaxCaptureStage() > 0) {
            builder.setMaxCaptureStages(mImpl.getMaxCaptureStage());
        }

        ImageCaptureExtender.ImageCaptureAdapter
                imageCaptureAdapter = new ImageCaptureExtender.ImageCaptureAdapter(mImpl, mContext);
        new Camera2ImplConfig.Extender<>(builder).setCameraEventCallback(
                new CameraEventCallbacks(imageCaptureAdapter));
        builder.setUseCaseEventCallback(imageCaptureAdapter);
        builder.setCaptureBundle(imageCaptureAdapter);
        List<Pair<Integer, Size[]>> supportedResolutions =
                ImageCaptureExtender.getSupportedResolutions(mImpl);
        if (supportedResolutions != null) {
            builder.setSupportedResolutions(supportedResolutions);
        }

        return builder.getUseCaseConfig();
    }
}
