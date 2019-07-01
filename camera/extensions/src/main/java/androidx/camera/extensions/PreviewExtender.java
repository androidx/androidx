/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.extensions;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.GuardedBy;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.Camera2CameraCaptureResultConverter;
import androidx.camera.camera2.impl.CameraEventCallback;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.CameraCaptureResult;
import androidx.camera.core.CameraCaptureResults;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.CaptureStage;
import androidx.camera.core.Config;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageInfoProcessor;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.camera.extensions.ExtensionsErrorListener.ExtensionsErrorCode;
import androidx.camera.extensions.ExtensionsManager.EffectMode;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.camera.extensions.impl.PreviewImageProcessorImpl;
import androidx.camera.extensions.impl.RequestUpdateProcessorImpl;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class for using an OEM provided extension on preview.
 */
public abstract class PreviewExtender {
    static final Config.Option<EffectMode> OPTION_PREVIEW_EXTENDER_MODE = Config.Option.create(
            "camerax.extensions.previewExtender.mode", EffectMode.class);

    private PreviewConfig.Builder mBuilder;
    PreviewExtenderImpl mImpl;
    private EffectMode mEffectMode;

    void init(PreviewConfig.Builder builder, PreviewExtenderImpl implementation,
            EffectMode effectMode) {
        mBuilder = builder;
        mImpl = implementation;
        mEffectMode = effectMode;
    }

    /**
     * Indicates whether extension function can support with
     * {@link PreviewConfig.Builder}
     *
     * @return True if the specific extension function is supported for the camera device.
     */
    public boolean isExtensionAvailable() {
        CameraX.LensFacing lensFacing = mBuilder.build().getLensFacing();
        String cameraId = CameraUtil.getCameraId(lensFacing);
        CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(cameraId);
        return mImpl.isExtensionAvailable(cameraId, cameraCharacteristics);
    }

    /**
     * Enables the derived preview extension feature.
     *
     * <p>Preview extension has dependence on image capture extension. A
     * IMAGE_CAPTURE_EXTENSION_REQUIRED error will be thrown if corresponding image capture
     * extension is not enabled together.
     */
    @SuppressWarnings("unchecked")
    public void enableExtension() {
        CameraX.LensFacing lensFacing = mBuilder.build().getLensFacing();
        String cameraId = CameraUtil.getCameraId(lensFacing);
        CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(cameraId);
        mImpl.init(cameraId, cameraCharacteristics);

        switch (mImpl.getProcessorType()) {
            case PROCESSOR_TYPE_REQUEST_UPDATE_ONLY:
                mBuilder.setImageInfoProcessor(new ImageInfoProcessor() {
                    @Override
                    public CaptureStage getCaptureStage() {
                        return new AdaptingCaptureStage(mImpl.getCaptureStage());
                    }

                    @Override
                    public boolean process(ImageInfo imageInfo) {
                        CameraCaptureResult result =
                                CameraCaptureResults.retrieveCameraCaptureResult(imageInfo);
                        if (result == null) {
                            return false;
                        }

                        CaptureResult captureResult =
                                Camera2CameraCaptureResultConverter.getCaptureResult(result);
                        if (captureResult == null) {
                            return false;
                        }

                        TotalCaptureResult totalCaptureResult = (TotalCaptureResult) captureResult;
                        if (totalCaptureResult == null) {
                            return false;
                        }

                        CaptureStageImpl captureStageImpl =
                                ((RequestUpdateProcessorImpl) mImpl.getProcessor()).process(
                                        totalCaptureResult);
                        return captureStageImpl != null;
                    }
                });
                break;
            case PROCESSOR_TYPE_IMAGE_PROCESSOR:
                mBuilder.setCaptureProcessor(new
                        AdaptingPreviewProcessor((PreviewImageProcessorImpl) mImpl.getProcessor()));
                break;
            default: // fall out
        }

        PreviewExtenderAdapter previewExtenderAdapter = new PreviewExtenderAdapter(mImpl,
                mEffectMode);
        new Camera2Config.Extender(mBuilder).setCameraEventCallback(
                new CameraEventCallbacks(previewExtenderAdapter));
        mBuilder.setUseCaseEventListener(previewExtenderAdapter);
        mBuilder.getMutableConfig().insertOption(OPTION_PREVIEW_EXTENDER_MODE, mEffectMode);
    }

    static void checkImageCaptureEnabled(EffectMode effectMode,
            Collection<UseCase> activeUseCases) {
        boolean isImageCaptureExtenderEnabled = false;
        boolean isMismatched = false;

        for (UseCase useCase : activeUseCases) {
            EffectMode imageCaptureExtenderMode = useCase.getUseCaseConfig().retrieveOption(
                    ImageCaptureExtender.OPTION_IMAGE_CAPTURE_EXTENDER_MODE, null);

            if (effectMode == imageCaptureExtenderMode) {
                isImageCaptureExtenderEnabled = true;
            } else if (imageCaptureExtenderMode != null) {
                isMismatched = true;
            }
        }

        if (isMismatched) {
            ExtensionsManager.postExtensionsError(
                    ExtensionsErrorCode.MISMATCHED_EXTENSIONS_ENABLED);
        } else if (!isImageCaptureExtenderEnabled) {
            ExtensionsManager.postExtensionsError(
                    ExtensionsErrorCode.IMAGE_CAPTURE_EXTENSION_REQUIRED);
        }
    }

    /**
     * An implementation to adapt the OEM provided implementation to core.
     */
    static class PreviewExtenderAdapter extends CameraEventCallback implements
            UseCase.EventListener {
        final EffectMode mEffectMode;
        private final PreviewExtenderImpl mImpl;
        private final AtomicBoolean mActive = new AtomicBoolean(true);
        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private volatile int mEnabledSessionCount = 0;
        @GuardedBy("mLock")
        private volatile boolean mUnbind = false;

        PreviewExtenderAdapter(PreviewExtenderImpl impl, EffectMode effectMode) {
            mImpl = impl;
            mEffectMode = effectMode;
        }

        @Override
        public void onBind(String cameraId) {
            if (mActive.get()) {
                CameraCharacteristics cameraCharacteristics =
                        CameraUtil.getCameraCharacteristics(cameraId);
                mImpl.onInit(cameraId, cameraCharacteristics, CameraX.getContext());
            }
        }

        @Override
        public void onUnbind() {
            synchronized (mLock) {
                mUnbind = true;
                if (mEnabledSessionCount == 0) {
                    callDeInit();
                }
            }
        }

        private void callDeInit() {
            if (mActive.get()) {
                mImpl.onDeInit();
                mActive.set(false);
            }
        }

        @Override
        public CaptureConfig onPresetSession() {
            if (mActive.get()) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    public void run() {
                        checkImageCaptureEnabled(mEffectMode, CameraX.getActiveUseCases());
                    }
                });
                CaptureStageImpl captureStageImpl = mImpl.onPresetSession();
                if (captureStageImpl != null) {
                    return new AdaptingCaptureStage(captureStageImpl).getCaptureConfig();
                }
            }

            return null;
        }

        @Override
        public CaptureConfig onEnableSession() {
            try {
                if (mActive.get()) {
                    CaptureStageImpl captureStageImpl = mImpl.onEnableSession();
                    if (captureStageImpl != null) {
                        return new AdaptingCaptureStage(captureStageImpl).getCaptureConfig();
                    }
                }

                return null;
            } finally {
                synchronized (mLock) {
                    mEnabledSessionCount++;
                }
            }
        }

        @Override
        public CaptureConfig onDisableSession() {
            try {
                if (mActive.get()) {
                    CaptureStageImpl captureStageImpl = mImpl.onDisableSession();
                    if (captureStageImpl != null) {
                        return new AdaptingCaptureStage(captureStageImpl).getCaptureConfig();
                    }
                }

                return null;
            } finally {
                synchronized (mLock) {
                    mEnabledSessionCount--;
                    if (mEnabledSessionCount == 0 && mUnbind) {
                        callDeInit();
                    }
                }
            }
        }

        @Override
        public CaptureConfig onRepeating() {
            if (mActive.get()) {
                CaptureStageImpl captureStageImpl = mImpl.getCaptureStage();
                if (captureStageImpl != null) {
                    return new AdaptingCaptureStage(captureStageImpl).getCaptureConfig();
                }
            }

            return null;
        }
    }

}
