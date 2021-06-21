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

import static androidx.camera.extensions.internal.ImageCaptureConfigProvider.OPTION_IMAGE_CAPTURE_CONFIG_PROVIDER_MODE;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.util.Pair;
import android.util.Size;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.impl.CameraEventCallback;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.extensions.impl.AutoPreviewExtenderImpl;
import androidx.camera.extensions.impl.BeautyPreviewExtenderImpl;
import androidx.camera.extensions.impl.BokehPreviewExtenderImpl;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.HdrPreviewExtenderImpl;
import androidx.camera.extensions.impl.NightPreviewExtenderImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.camera.extensions.impl.PreviewImageProcessorImpl;
import androidx.core.util.Consumer;

import java.util.Collection;
import java.util.List;

/**
 * For providing extensions config for preview.
 */
public class PreviewConfigProvider implements ConfigProvider<PreviewConfig> {
    private static final String TAG = "PreviewConfigProvider";
    static final Config.Option<Integer> OPTION_PREVIEW_CONFIG_PROVIDER_MODE = Config.Option.create(
            "camerax.extensions.previewConfigProvider.mode", Integer.class);
    private PreviewExtenderImpl mImpl;
    private Context mContext;
    @ExtensionMode.Mode
    private int mEffectMode;

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    public PreviewConfigProvider(@ExtensionMode.Mode int mode,
            @NonNull CameraInfo cameraInfo, @NonNull Context context) {
        try {
            switch (mode) {
                case ExtensionMode.BOKEH:
                    mImpl = new BokehPreviewExtenderImpl();
                    break;
                case ExtensionMode.HDR:
                    mImpl = new HdrPreviewExtenderImpl();
                    break;
                case ExtensionMode.NIGHT:
                    mImpl = new NightPreviewExtenderImpl();
                    break;
                case ExtensionMode.BEAUTY:
                    mImpl = new BeautyPreviewExtenderImpl();
                    break;
                case ExtensionMode.AUTO:
                    mImpl = new AutoPreviewExtenderImpl();
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
    public PreviewConfig getConfig() {
        if (mImpl == null) {
            return new PreviewConfig(OptionsBundle.emptyBundle());
        }
        Preview.Builder builder = new Preview.Builder();

        updateBuilderConfig(builder, mEffectMode, mImpl, mContext);

        return builder.getUseCaseConfig();
    }

    /**
     * Update extension related configs to the builder.
     */
    private void updateBuilderConfig(@NonNull Preview.Builder builder,
            @ExtensionMode.Mode int effectMode, @NonNull PreviewExtenderImpl impl,
            @NonNull Context context) {
        PreviewEventAdapter previewEventAdapter;

        switch (impl.getProcessorType()) {
            case PROCESSOR_TYPE_REQUEST_UPDATE_ONLY:
                AdaptingRequestUpdateProcessor adaptingRequestUpdateProcessor =
                        new AdaptingRequestUpdateProcessor(impl);
                builder.setImageInfoProcessor(adaptingRequestUpdateProcessor);
                previewEventAdapter = new PreviewEventAdapter(impl, context,
                        adaptingRequestUpdateProcessor);
                break;
            case PROCESSOR_TYPE_IMAGE_PROCESSOR:
                AdaptingPreviewProcessor adaptingPreviewProcessor = new
                        AdaptingPreviewProcessor((PreviewImageProcessorImpl) impl.getProcessor());
                builder.setCaptureProcessor(adaptingPreviewProcessor);
                previewEventAdapter = new PreviewEventAdapter(impl, context,
                        adaptingPreviewProcessor);
                break;
            default:
                previewEventAdapter = new PreviewEventAdapter(impl, context, null);
        }

        new Camera2ImplConfig.Extender<>(builder).setCameraEventCallback(
                new CameraEventCallbacks(previewEventAdapter));
        builder.setUseCaseEventCallback(previewEventAdapter);

        try {
            Consumer<Collection<UseCase>> attachedUseCasesUpdateListener =
                    useCases -> checkImageCaptureEnabled(effectMode, useCases);
            builder.setAttachedUseCasesUpdateListener(attachedUseCasesUpdateListener);
        } catch (NoSuchMethodError e) {
            // setAttachedUseCasesUpdateListener function may not exist in the used core library.
            // Catches the NoSuchMethodError and make the extensions be able to be enabled but
            // only the ExtensionsErrorListener does not work.
            Logger.e(TAG, "Can't set attached use cases update listener.");
        }

        builder.getMutableConfig().insertOption(OPTION_PREVIEW_CONFIG_PROVIDER_MODE, effectMode);
        List<Pair<Integer, Size[]>> supportedResolutions = getSupportedResolutions(impl);
        if (supportedResolutions != null) {
            builder.setSupportedResolutions(supportedResolutions);
        }
    }

    /**
     * Get the resolutions.
     */
    @Nullable
    private List<Pair<Integer, Size[]>> getSupportedResolutions(@NonNull PreviewExtenderImpl impl) {
        if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_1) < 0) {
            return null;
        }

        try {
            return impl.getSupportedResolutions();
        } catch (NoSuchMethodError e) {
            Logger.e(TAG, "getSupportedResolution interface is not implemented in vendor library.");
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private void checkImageCaptureEnabled(@ExtensionMode.Mode int effectMode,
            Collection<UseCase> activeUseCases) {
        boolean isImageCaptureExtenderEnabled = false;
        boolean isMismatched = false;

        // In case all use cases are unbound when doing the check.
        if (activeUseCases == null || activeUseCases.isEmpty()) {
            return;
        }

        for (UseCase useCase : activeUseCases) {
            int imageCaptureExtenderMode = useCase.getCurrentConfig().retrieveOption(
                    OPTION_IMAGE_CAPTURE_CONFIG_PROVIDER_MODE,
                    ExtensionMode.NONE);

            if (effectMode == imageCaptureExtenderMode) {
                isImageCaptureExtenderEnabled = true;
            } else if (imageCaptureExtenderMode != ExtensionMode.NONE) {
                isMismatched = true;
            }
        }

        if (isMismatched) {
            ExtensionsManager.postExtensionsError(
                    androidx.camera.extensions.ExtensionsErrorListener
                            .ExtensionsErrorCode.MISMATCHED_EXTENSIONS_ENABLED);
        } else if (!isImageCaptureExtenderEnabled) {
            ExtensionsManager.postExtensionsError(
                    androidx.camera.extensions.ExtensionsErrorListener
                            .ExtensionsErrorCode.IMAGE_CAPTURE_EXTENSION_REQUIRED);
        }
    }

    /**
     * An implementation to adapt the OEM provided implementation to core.
     */
    private static class PreviewEventAdapter extends CameraEventCallback implements
            UseCase.EventCallback {
        @NonNull
        final PreviewExtenderImpl mImpl;
        @NonNull
        private final Context mContext;

        final CloseableProcessor mCloseableProcessor;

        // Once the adapter has set mActive to false a new instance needs to be created
        @GuardedBy("mLock")
        volatile boolean mActive = true;
        final Object mLock = new Object();
        @GuardedBy("mLock")
        private volatile int mEnabledSessionCount = 0;
        @GuardedBy("mLock")
        private volatile boolean mUnbind = false;

        PreviewEventAdapter(@NonNull PreviewExtenderImpl impl,
                @NonNull Context context, @Nullable CloseableProcessor closeableProcessor) {
            mImpl = impl;
            mContext = context;
            mCloseableProcessor = closeableProcessor;
        }

        @OptIn(markerClass = ExperimentalCamera2Interop.class)
        @Override
        public void onAttach(@NonNull CameraInfo cameraInfo) {
            synchronized (mLock) {
                if (mActive) {
                    String cameraId = Camera2CameraInfo.from(cameraInfo).getCameraId();
                    CameraCharacteristics cameraCharacteristics =
                            Camera2CameraInfo.extractCameraCharacteristics(cameraInfo);
                    mImpl.onInit(cameraId, cameraCharacteristics, mContext);
                }
            }
        }

        @Override
        public void onDetach() {
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
        @Nullable
        public CaptureConfig onPresetSession() {
            synchronized (mLock) {
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
            }

            return null;
        }

        @Override
        @Nullable
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
        @Nullable
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
        @Nullable
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
}
