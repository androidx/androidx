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
import android.os.Build;
import android.util.Pair;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.impl.CameraEventCallback;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Logger;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CaptureBundle;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.CaptureStage;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.impl.CaptureProcessorImpl;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides extensions related configs for image capture
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ImageCaptureConfigProvider implements ConfigProvider<ImageCaptureConfig> {
    private static final String TAG = "ImageCaptureConfigProvider";
    static final Config.Option<Integer> OPTION_IMAGE_CAPTURE_CONFIG_PROVIDER_MODE =
            Config.Option.create("camerax.extensions.imageCaptureConfigProvider.mode",
                    Integer.class);

    private final VendorExtender mVendorExtender;
    private final Context mContext;
    @ExtensionMode.Mode
    private final int mEffectMode;

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    public ImageCaptureConfigProvider(
            @ExtensionMode.Mode int mode,
            @NonNull VendorExtender vendorExtender,
            @NonNull Context context) {
        mEffectMode = mode;
        mVendorExtender = vendorExtender;
        mContext = context;
    }

    @NonNull
    @Override
    public ImageCaptureConfig getConfig() {
        ImageCapture.Builder builder = new ImageCapture.Builder();
        updateBuilderConfig(builder, mEffectMode, mVendorExtender, mContext);

        return builder.getUseCaseConfig();
    }

    /**
     * Update extension related configs to the builder.
     */
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    void updateBuilderConfig(@NonNull ImageCapture.Builder builder,
            @ExtensionMode.Mode int effectMode, @NonNull VendorExtender vendorExtender,
            @NonNull Context context) {
        if (vendorExtender instanceof BasicVendorExtender) {
            ImageCaptureExtenderImpl imageCaptureExtenderImpl =
                    ((BasicVendorExtender) vendorExtender).getImageCaptureExtenderImpl();

            if (imageCaptureExtenderImpl != null) {
                CaptureProcessorImpl captureProcessor =
                        imageCaptureExtenderImpl.getCaptureProcessor();
                AdaptingCaptureProcessor adaptingCaptureProcessor = null;
                if (captureProcessor != null) {
                    adaptingCaptureProcessor = new AdaptingCaptureProcessor(captureProcessor);
                    builder.setCaptureProcessor(adaptingCaptureProcessor);
                }

                if (imageCaptureExtenderImpl.getMaxCaptureStage() > 0) {
                    builder.setMaxCaptureStages(
                            imageCaptureExtenderImpl.getMaxCaptureStage());
                }

                ImageCaptureEventAdapter imageCaptureEventAdapter =
                        new ImageCaptureEventAdapter(imageCaptureExtenderImpl,
                                context, adaptingCaptureProcessor);
                new Camera2ImplConfig.Extender<>(builder).setCameraEventCallback(
                        new CameraEventCallbacks(imageCaptureEventAdapter));
                builder.setUseCaseEventCallback(imageCaptureEventAdapter);

                builder.setCaptureBundle(imageCaptureEventAdapter);
            } else {
                Logger.e(TAG, "ImageCaptureExtenderImpl is null!");
            }
        }

        builder.getMutableConfig().insertOption(OPTION_IMAGE_CAPTURE_CONFIG_PROVIDER_MODE,
                effectMode);
        List<Pair<Integer, Size[]>> supportedResolutions =
                vendorExtender.getSupportedCaptureOutputResolutions();
        builder.setSupportedResolutions(supportedResolutions);
        builder.setHighResolutionDisabled(true);
    }


    /**
     * An implementation to adapt the OEM provided implementation to core.
     */
    private static class ImageCaptureEventAdapter extends CameraEventCallback implements
            UseCase.EventCallback,
            CaptureBundle {
        @NonNull
        private final ImageCaptureExtenderImpl mImpl;
        @NonNull
        private final Context mContext;
        @Nullable
        private VendorProcessor mVendorCaptureProcessor;
        @Nullable
        private volatile CameraInfo mCameraInfo;
        ImageCaptureEventAdapter(@NonNull ImageCaptureExtenderImpl impl,
                @NonNull Context context,
                @Nullable VendorProcessor vendorCaptureProcessor) {
            mImpl = impl;
            mContext = context;
            mVendorCaptureProcessor = vendorCaptureProcessor;
        }

        // Invoked from main thread
        @Override
        public void onAttach(@NonNull CameraInfo cameraInfo) {
            mCameraInfo = cameraInfo;
        }

        // Invoked from main thread
        @Override
        public void onDetach() {
        }

        // Invoked from camera thread
        @Override
        @Nullable
        @OptIn(markerClass = ExperimentalCamera2Interop.class)
        public CaptureConfig onInitSession() {
            Preconditions.checkNotNull(mCameraInfo,
                    "ImageCaptureConfigProvider was not attached.");
            String cameraId = Camera2CameraInfo.from(mCameraInfo).getCameraId();
            CameraCharacteristics cameraCharacteristics =
                    Camera2CameraInfo.extractCameraCharacteristics(mCameraInfo);
            Logger.d(TAG, "ImageCapture onInit");
            mImpl.onInit(cameraId, cameraCharacteristics, mContext);

            if (mVendorCaptureProcessor != null) {
                mVendorCaptureProcessor.onInit();
            }

            CaptureStageImpl captureStageImpl = mImpl.onPresetSession();
            if (captureStageImpl != null) {
                if (Build.VERSION.SDK_INT >= 28) {
                    return new AdaptingCaptureStage(captureStageImpl).getCaptureConfig();
                } else {
                    Logger.w(TAG, "The CaptureRequest parameters returned from "
                            + "onPresetSession() will be passed to the camera device as part "
                            + "of the capture session via "
                            + "SessionConfiguration#setSessionParameters(CaptureRequest) "
                            + "which only supported from API level 28!");
                }
            }
            return null;
        }

        // Invoked from camera thread
        @Override
        @Nullable
        public CaptureConfig onEnableSession() {
            Logger.d(TAG, "ImageCapture onEnableSession");
            CaptureStageImpl captureStageImpl = mImpl.onEnableSession();
            if (captureStageImpl != null) {
                return new AdaptingCaptureStage(captureStageImpl).getCaptureConfig();
            }

            return null;
        }

        // Invoked from camera thread
        @Override
        @Nullable
        public CaptureConfig onDisableSession() {
            Logger.d(TAG, "ImageCapture onDisableSession");
            CaptureStageImpl captureStageImpl = mImpl.onDisableSession();
            if (captureStageImpl != null) {
                return new AdaptingCaptureStage(captureStageImpl).getCaptureConfig();
            }

            return null;
        }

        // Invoked from main thread
        @Override
        @Nullable
        public List<CaptureStage> getCaptureStages() {
            List<CaptureStageImpl> captureStages = mImpl.getCaptureStages();
            if (captureStages != null && !captureStages.isEmpty()) {
                ArrayList<CaptureStage> ret = new ArrayList<>();
                for (CaptureStageImpl s : captureStages) {
                    ret.add(new AdaptingCaptureStage(s));
                }
                return ret;
            }

            return null;
        }

        // Invoked from camera thread
        @Override
        public void onDeInitSession() {
            if (mVendorCaptureProcessor != null) {
                mVendorCaptureProcessor.onDeInit();
            }
            mImpl.onDeInit();
        }
    }
}
