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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.util.Size;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.impl.CameraEventCallback;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.extensions.ExtensionsErrorListener.ExtensionsErrorCode;
import androidx.camera.extensions.ExtensionsManager.EffectMode;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.camera.extensions.impl.PreviewImageProcessorImpl;

import java.util.Collection;
import java.util.List;

/**
 * Class for using an OEM provided extension on preview.
 */
public abstract class PreviewExtender {
    private static final String TAG = "PreviewExtender";
    static final Config.Option<EffectMode> OPTION_PREVIEW_EXTENDER_MODE = Config.Option.create(
            "camerax.extensions.previewExtender.mode", EffectMode.class);

    private Preview.Builder mBuilder;
    private PreviewExtenderImpl mImpl;
    private EffectMode mEffectMode;
    private ExtensionCameraFilter mExtensionCameraFilter;

    void init(Preview.Builder builder, PreviewExtenderImpl implementation,
            EffectMode effectMode) {
        mBuilder = builder;
        mImpl = implementation;
        mEffectMode = effectMode;
        mExtensionCameraFilter = new ExtensionCameraFilter(mImpl);
    }

    /**
     * Indicates whether extension function can support with the given {@link CameraSelector}.
     *
     * @param cameraSelector The selector that determines a camera that will be checked for the
     *                       availability of extensions.
     * @return True if the specific extension function is supported for the camera device.
     */
    public boolean isExtensionAvailable(@NonNull CameraSelector cameraSelector) {
        return getCameraWithExtension(cameraSelector) != null;
    }

    /**
     * Returns the camera specified with the given camera selector and this extension, null if
     * there's no available can be found.
     */
    private String getCameraWithExtension(@NonNull CameraSelector cameraSelector) {
        CameraSelector.Builder extensionCameraSelectorBuilder =
                CameraSelector.Builder.fromSelector(cameraSelector);
        extensionCameraSelectorBuilder.appendFilter(mExtensionCameraFilter);

        return CameraUtil.getCameraIdUnchecked(extensionCameraSelectorBuilder.build());
    }

    /**
     * Enables the derived image capture extension feature. If the extension can't be
     * applied on any of the cameras specified with the given {@link CameraSelector}, it will be
     * no-ops.
     *
     * <p>Enabling extensions on {@link Preview} may limit the number of cameras which can be
     * selected when the {@link Preview} is used as a parameter to bindToLifecycle.
     * BindToLifecycle will throw an exception if no cameras are found that support the extension.
     *
     * <p>Preview extension has dependence on image capture extension. A
     * IMAGE_CAPTURE_EXTENSION_REQUIRED error will be thrown if corresponding image capture
     * extension is not enabled together.
     *
     * @param cameraSelector The selector used to determine the camera for which to enable
     *                       extensions.
     */
    public void enableExtension(@NonNull CameraSelector cameraSelector) {
        String cameraId = getCameraWithExtension(cameraSelector);
        if (cameraId == null) {
            // If there's no available camera id for the extender to function, just return here
            // and it will be no-ops.
            return;
        }

        // TODO: This will be move to a single place for enabling extensions. See b/135434036
        // Sets the extension camera id filter to the config.
        CameraSelector originalSelector = mBuilder.getUseCaseConfig().getCameraSelector(null);
        if (originalSelector == null) {
            mBuilder.setCameraSelector(
                    new CameraSelector.Builder().appendFilter(mExtensionCameraFilter).build());
        } else {
            mBuilder.setCameraSelector(CameraSelector.Builder.fromSelector(
                    originalSelector).appendFilter(mExtensionCameraFilter).build());
        }

        CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(cameraId);
        mImpl.init(cameraId, cameraCharacteristics);

        PreviewExtenderAdapter previewExtenderAdapter;
        switch (mImpl.getProcessorType()) {
            case PROCESSOR_TYPE_REQUEST_UPDATE_ONLY:
                AdaptingRequestUpdateProcessor adaptingRequestUpdateProcessor =
                        new AdaptingRequestUpdateProcessor(mImpl);
                mBuilder.setImageInfoProcessor(adaptingRequestUpdateProcessor);
                previewExtenderAdapter = new PreviewExtenderAdapter(mImpl, mEffectMode,
                        adaptingRequestUpdateProcessor);
                break;
            case PROCESSOR_TYPE_IMAGE_PROCESSOR:
                AdaptingPreviewProcessor adaptingPreviewProcessor = new
                        AdaptingPreviewProcessor((PreviewImageProcessorImpl) mImpl.getProcessor());
                mBuilder.setCaptureProcessor(adaptingPreviewProcessor);
                previewExtenderAdapter = new PreviewExtenderAdapter(mImpl, mEffectMode,
                        adaptingPreviewProcessor);
                break;
            default:
                previewExtenderAdapter = new PreviewExtenderAdapter(mImpl, mEffectMode, null);
        }

        new Camera2ImplConfig.Extender<>(mBuilder).setCameraEventCallback(
                new CameraEventCallbacks(previewExtenderAdapter));
        mBuilder.setUseCaseEventCallback(previewExtenderAdapter);
        mBuilder.getMutableConfig().insertOption(OPTION_PREVIEW_EXTENDER_MODE, mEffectMode);
        setSupportedResolutions();
    }

    private void setSupportedResolutions() {
        if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_1) < 0) {
            return;
        }

        List<Pair<Integer, Size[]>> supportedResolutions = null;

        try {
            supportedResolutions = mImpl.getSupportedResolutions();
        } catch (NoSuchMethodError e) {
            Log.e(TAG, "getSupportedResolution interface is not implemented in vendor library.");
        }

        if (supportedResolutions != null) {
            mBuilder.setSupportedResolutions(supportedResolutions);
        }
    }

    static void checkImageCaptureEnabled(EffectMode effectMode,
            Collection<UseCase> activeUseCases) {
        boolean isImageCaptureExtenderEnabled = false;
        boolean isMismatched = false;

        // In case all use cases are unbound when doing the check.
        if (activeUseCases == null || activeUseCases.isEmpty()) {
            return;
        }

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
    private static class PreviewExtenderAdapter extends CameraEventCallback implements
            UseCase.EventCallback {
        final EffectMode mEffectMode;

        final PreviewExtenderImpl mImpl;

        final CloseableProcessor mCloseableProcessor;

        // Once the adapter has set mActive to false a new instance needs to be created
        @GuardedBy("mLock")
        volatile boolean mActive = true;
        final Object mLock = new Object();
        @GuardedBy("mLock")
        private volatile int mEnabledSessionCount = 0;
        @GuardedBy("mLock")
        private volatile boolean mUnbind = false;

        PreviewExtenderAdapter(PreviewExtenderImpl impl, EffectMode effectMode,
                CloseableProcessor closeableProcessor) {
            mImpl = impl;
            mEffectMode = effectMode;
            mCloseableProcessor = closeableProcessor;
        }

        @Override
        public void onBind(@NonNull String cameraId) {
            synchronized (mLock) {
                if (mActive) {
                    CameraCharacteristics cameraCharacteristics =
                            CameraUtil.getCameraCharacteristics(cameraId);
                    mImpl.onInit(cameraId, cameraCharacteristics, CameraX.getContext());
                }
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
            synchronized (mLock) {
                if (mActive) {
                    if (mCloseableProcessor != null) {
                        mCloseableProcessor.close();
                    }
                    mImpl.onDeInit();
                    mActive = false;
                }
            }
        }

        @Override
        public CaptureConfig onPresetSession() {
            synchronized (mLock) {
                if (mActive) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                checkImageCaptureEnabled(mEffectMode, CameraX.getActiveUseCases());
                            } catch (IllegalStateException e) {
                                Log.e(TAG, "CameraX has been shutdown. Don't need to check for "
                                        + "active use cases.");
                            }
                        }
                    });
                }
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
                synchronized (mLock) {
                    if (mActive) {
                        CaptureStageImpl captureStageImpl = mImpl.onEnableSession();
                        if (captureStageImpl != null) {
                            return new AdaptingCaptureStage(captureStageImpl).getCaptureConfig();
                        }
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
                synchronized (mLock) {
                    if (mActive) {
                        CaptureStageImpl captureStageImpl = mImpl.onDisableSession();
                        if (captureStageImpl != null) {
                            return new AdaptingCaptureStage(captureStageImpl).getCaptureConfig();
                        }
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
            synchronized (mLock) {
                if (mActive) {
                    CaptureStageImpl captureStageImpl = mImpl.getCaptureStage();
                    if (captureStageImpl != null) {
                        return new AdaptingCaptureStage(captureStageImpl).getCaptureConfig();
                    }
                }
            }

            return null;
        }
    }

    /**
     * A processor that can be closed so that the underlying processing implementation is skipped,
     * if it has been closed.
     */
    interface CloseableProcessor {
        void close();
    }
}
