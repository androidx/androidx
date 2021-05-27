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

import static androidx.camera.extensions.internal.PreviewConfigProvider.OPTION_PREVIEW_CONFIG_PROVIDER_MODE;

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
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Logger;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CaptureBundle;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.CaptureStage;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.ExtensionsErrorListener;
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.extensions.impl.AutoImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BeautyImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BokehImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.CaptureProcessorImpl;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.HdrImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.NightImageCaptureExtenderImpl;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides extensions related configs for image capture
 */
public class ImageCaptureConfigProvider implements ConfigProvider<ImageCaptureConfig> {
    private static final String TAG = "ImageCaptureConfigProvider";
    static final Config.Option<Integer> OPTION_IMAGE_CAPTURE_CONFIG_PROVIDER_MODE =
            Config.Option.create("camerax.extensions.imageCaptureConfigProvider.mode",
                    Integer.class);

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

        updateBuilderConfig(builder, mEffectMode, mImpl, mContext);

        return builder.getUseCaseConfig();
    }

    /**
     * Update extension related configs to the builder.
     */
    private void updateBuilderConfig(@NonNull ImageCapture.Builder builder,
            @ExtensionMode.Mode int effectMode, @NonNull ImageCaptureExtenderImpl impl,
            @NonNull Context context) {
        CaptureProcessorImpl captureProcessor = impl.getCaptureProcessor();
        if (captureProcessor != null) {
            builder.setCaptureProcessor(new AdaptingCaptureProcessor(captureProcessor));
        }

        if (impl.getMaxCaptureStage() > 0) {
            builder.setMaxCaptureStages(impl.getMaxCaptureStage());
        }

        ImageCaptureEventAdapter imageCaptureEventAdapter = new ImageCaptureEventAdapter(impl,
                context);
        new Camera2ImplConfig.Extender<>(builder).setCameraEventCallback(
                new CameraEventCallbacks(imageCaptureEventAdapter));
        builder.setUseCaseEventCallback(imageCaptureEventAdapter);

        try {
            Consumer<Collection<UseCase>> attachedUseCasesUpdateListener =
                    useCases -> checkPreviewEnabled(effectMode, useCases);
            builder.setAttachedUseCasesUpdateListener(attachedUseCasesUpdateListener);
        } catch (NoSuchMethodError e) {
            // setAttachedUseCasesUpdateListener function may not exist in the used core library.
            // Catches the NoSuchMethodError and make the extensions be able to be enabled but
            // only the ExtensionsErrorListener does not work.
            Logger.e(TAG, "Can't set attached use cases update listener.");
        }

        builder.setCaptureBundle(imageCaptureEventAdapter);
        builder.getMutableConfig().insertOption(OPTION_IMAGE_CAPTURE_CONFIG_PROVIDER_MODE,
                effectMode);

        List<Pair<Integer, Size[]>> supportedResolutions = getSupportedResolutions(impl);
        if (supportedResolutions != null) {
            builder.setSupportedResolutions(supportedResolutions);
        }
    }

    /**
     * Get the supported resolutions.
     */
    @Nullable
    private List<Pair<Integer, Size[]>> getSupportedResolutions(
            @NonNull ImageCaptureExtenderImpl impl) {
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

    private void checkPreviewEnabled(@ExtensionMode.Mode int effectMode,
            Collection<UseCase> activeUseCases) {
        boolean isPreviewExtenderEnabled = false;
        boolean isMismatched = false;

        // In case all use cases are unbound when doing the check.
        if (activeUseCases == null || activeUseCases.isEmpty()) {
            return;
        }

        for (UseCase useCase : activeUseCases) {
            int previewExtenderMode = useCase.getCurrentConfig().retrieveOption(
                    OPTION_PREVIEW_CONFIG_PROVIDER_MODE, ExtensionMode.NONE);

            if (effectMode == previewExtenderMode) {
                isPreviewExtenderEnabled = true;
            } else if (previewExtenderMode != ExtensionMode.NONE) {
                isMismatched = true;
            }
        }

        if (isMismatched) {
            ExtensionsManager.postExtensionsError(
                    ExtensionsErrorListener.ExtensionsErrorCode.MISMATCHED_EXTENSIONS_ENABLED);
        } else if (!isPreviewExtenderEnabled) {
            ExtensionsManager.postExtensionsError(
                    ExtensionsErrorListener.ExtensionsErrorCode.PREVIEW_EXTENSION_REQUIRED);
        }
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
        private final AtomicBoolean mActive = new AtomicBoolean(true);
        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private volatile int mEnabledSessionCount = 0;
        @GuardedBy("mLock")
        private volatile boolean mUnbind = false;

        ImageCaptureEventAdapter(@NonNull ImageCaptureExtenderImpl impl,
                @NonNull Context context) {
            mImpl = impl;
            mContext = context;
        }

        @OptIn(markerClass = ExperimentalCamera2Interop.class)
        @Override
        public void onAttach(@NonNull CameraInfo cameraInfo) {
            if (mActive.get()) {
                String cameraId = Camera2CameraInfo.from(cameraInfo).getCameraId();
                CameraCharacteristics cameraCharacteristics =
                        Camera2CameraInfo.extractCameraCharacteristics(cameraInfo);
                mImpl.onInit(cameraId, cameraCharacteristics, mContext);
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
            if (mActive.get()) {
                mImpl.onDeInit();
                mActive.set(false);
            }
        }

        @Override
        @Nullable
        public CaptureConfig onPresetSession() {
            if (mActive.get()) {
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
        @Nullable
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
        @Nullable
        public List<CaptureStage> getCaptureStages() {
            if (mActive.get()) {
                List<CaptureStageImpl> captureStages = mImpl.getCaptureStages();
                if (captureStages != null && !captureStages.isEmpty()) {
                    ArrayList<CaptureStage> ret = new ArrayList<>();
                    for (CaptureStageImpl s : captureStages) {
                        ret.add(new AdaptingCaptureStage(s));
                    }
                    return ret;
                }
            }

            return null;
        }
    }
}
