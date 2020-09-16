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
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.extensions.Extensions;
import androidx.camera.extensions.PreviewExtender;
import androidx.camera.extensions.impl.AutoPreviewExtenderImpl;
import androidx.camera.extensions.impl.BeautyPreviewExtenderImpl;
import androidx.camera.extensions.impl.BokehPreviewExtenderImpl;
import androidx.camera.extensions.impl.HdrPreviewExtenderImpl;
import androidx.camera.extensions.impl.NightPreviewExtenderImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.camera.extensions.impl.PreviewImageProcessorImpl;

import java.util.List;

/**
 * For providing extensions config for preview.
 */
public class PreviewConfigProvider implements ConfigProvider<PreviewConfig> {
    private PreviewExtenderImpl mImpl;
    private Context mContext;

    @UseExperimental(markerClass = ExperimentalCamera2Interop.class)
    public PreviewConfigProvider(@Extensions.ExtensionMode int mode,
            @NonNull CameraInfo cameraInfo, @NonNull Context context) {
        try {
            switch (mode) {
                case Extensions.EXTENSION_MODE_BOKEH:
                    mImpl = new BokehPreviewExtenderImpl();
                    break;
                case Extensions.EXTENSION_MODE_HDR:
                    mImpl = new HdrPreviewExtenderImpl();
                    break;
                case Extensions.EXTENSION_MODE_NIGHT:
                    mImpl = new NightPreviewExtenderImpl();
                    break;
                case Extensions.EXTENSION_MODE_BEAUTY:
                    mImpl = new BeautyPreviewExtenderImpl();
                    break;
                case Extensions.EXTENSION_MODE_AUTO:
                    mImpl = new AutoPreviewExtenderImpl();
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
    public PreviewConfig getConfig() {
        if (mImpl == null) {
            return new PreviewConfig(OptionsBundle.emptyBundle());
        }
        Preview.Builder builder = new Preview.Builder();

        PreviewExtender.PreviewExtenderAdapter previewExtenderAdapter;
        switch (mImpl.getProcessorType()) {
            case PROCESSOR_TYPE_REQUEST_UPDATE_ONLY:
                AdaptingRequestUpdateProcessor adaptingRequestUpdateProcessor =
                        new AdaptingRequestUpdateProcessor(mImpl);
                builder.setImageInfoProcessor(adaptingRequestUpdateProcessor);
                previewExtenderAdapter = new PreviewExtender.PreviewExtenderAdapter(mImpl,
                        mContext, adaptingRequestUpdateProcessor);
                break;
            case PROCESSOR_TYPE_IMAGE_PROCESSOR:
                AdaptingPreviewProcessor adaptingPreviewProcessor = new
                        AdaptingPreviewProcessor((PreviewImageProcessorImpl) mImpl.getProcessor());
                builder.setCaptureProcessor(adaptingPreviewProcessor);
                previewExtenderAdapter = new PreviewExtender.PreviewExtenderAdapter(mImpl,
                        mContext, adaptingPreviewProcessor);
                break;
            default:
                previewExtenderAdapter = new PreviewExtender.PreviewExtenderAdapter(mImpl,
                        mContext, null);
        }

        new Camera2ImplConfig.Extender<>(builder).setCameraEventCallback(
                new CameraEventCallbacks(previewExtenderAdapter));
        builder.setUseCaseEventCallback(previewExtenderAdapter);
        List<Pair<Integer, Size[]>> supportedResolutions =
                PreviewExtender.getSupportedResolutions(mImpl);
        if (supportedResolutions != null) {
            builder.setSupportedResolutions(supportedResolutions);
        }

        return builder.getUseCaseConfig();
    }
}
