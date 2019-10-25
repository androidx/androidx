/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A use case that provides a camera preview stream for displaying on-screen.
 *
 * <p>The preview stream is connected to the {@link Surface} provided
 * via{@link PreviewSurfaceCallback}. The application decides how the {@link Surface} is shown,
 * and is responsible for managing the {@link Surface} lifecycle after providing it.
 */
public class Preview extends UseCase {
    /**
     * Provides a static configuration with implementation-agnostic options.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final Defaults DEFAULT_CONFIG = new Defaults();
    private static final String TAG = "Preview";
    @Nullable
    private HandlerThread mProcessingPreviewThread;
    @Nullable
    private Handler mProcessingPreviewHandler;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @Nullable
    PreviewSurfaceCallback mPreviewSurfaceCallback;
    @SuppressWarnings("WeakerAccess") /* Synthetic Accessor */
    @Nullable
    Executor mPreviewSurfaceCallbackExecutor;
    // Cached latest resolution for creating the pipeline as soon as it's ready.
    @Nullable
    private Size mLatestResolution;

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    SurfaceHolder mSurfaceHolder;

    /**
     * Creates a new preview use case from the given configuration.
     *
     * @param config for this use case instance
     */
    @MainThread
    public Preview(@NonNull PreviewConfig config) {
        super(config);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    SessionConfig.Builder createPipeline(PreviewConfig config, Size resolution) {
        Threads.checkMainThread();
        Preconditions.checkState(isPreviewSurfaceCallbackSet());
        SessionConfig.Builder sessionConfigBuilder = SessionConfig.Builder.createFrom(config);

        final CaptureProcessor captureProcessor = config.getCaptureProcessor(null);
        final CallbackDeferrableSurface callbackDeferrableSurface = new CallbackDeferrableSurface(
                resolution, mPreviewSurfaceCallbackExecutor,
                mPreviewSurfaceCallback);
        if (captureProcessor != null) {
            CaptureStage captureStage = new CaptureStage.DefaultCaptureStage();
            // TODO: To allow user to use an Executor for the processing.

            if (mProcessingPreviewHandler == null) {
                mProcessingPreviewThread = new HandlerThread("ProcessingSurfaceTexture");
                mProcessingPreviewThread.start();
                mProcessingPreviewHandler = new Handler(mProcessingPreviewThread.getLooper());
            }

            ProcessingSurface processingSurface =
                    new ProcessingSurface(
                            resolution.getWidth(),
                            resolution.getHeight(),
                            ImageFormat.YUV_420_888,
                            mProcessingPreviewHandler,
                            captureStage,
                            captureProcessor,
                            callbackDeferrableSurface);

            sessionConfigBuilder.addCameraCaptureCallback(
                    processingSurface.getCameraCaptureCallback());

            mSurfaceHolder = processingSurface;
            sessionConfigBuilder.addSurface(processingSurface);
            sessionConfigBuilder.setTag(captureStage.getId());
        } else {
            final ImageInfoProcessor processor = config.getImageInfoProcessor(null);

            if (processor != null) {
                sessionConfigBuilder.addCameraCaptureCallback(new CameraCaptureCallback() {
                    @Override
                    public void onCaptureCompleted(
                            @NonNull CameraCaptureResult cameraCaptureResult) {
                        super.onCaptureCompleted(cameraCaptureResult);
                        if (processor.process(
                                new CameraCaptureResultImageInfo(cameraCaptureResult))) {
                            notifyUpdated();
                        }
                    }
                });
            }
            mSurfaceHolder = callbackDeferrableSurface;
            sessionConfigBuilder.addSurface(callbackDeferrableSurface);
        }
        sessionConfigBuilder.addErrorListener(new SessionConfig.ErrorListener() {
            @Override
            public void onError(@NonNull SessionConfig sessionConfig,
                    @NonNull SessionConfig.SessionError error) {
                callbackDeferrableSurface.release();
                SessionConfig.Builder sessionConfigBuilder = createPipeline(config, resolution);
                String cameraId = getCameraIdUnchecked(config);
                attachToCamera(cameraId, sessionConfigBuilder.build());
                notifyReset();
            }
        });

        return sessionConfigBuilder;
    }

    /**
     * Gets {@link PreviewSurfaceCallback}
     *
     * <p> Setting the callback will signal to the camera that the use case is ready to receive
     * data.
     *
     * <p> To displaying preview with a {@link TextureView}, consider
     * using {@link PreviewUtil#createPreviewSurfaceCallback(PreviewUtil.SurfaceTextureCallback)} to
     * create the callback.
     *
     * @return the last set callback or {@code null} if no listener is set
     */
    @UiThread
    @Nullable
    public PreviewSurfaceCallback getPreviewSurfaceCallback() {
        Threads.checkMainThread();
        return mPreviewSurfaceCallback;
    }

    /**
     * Sets a {@link PreviewSurfaceCallback} to provide Surface for Preview.
     *
     * <p> Setting the callback will signal to the camera that the use case is ready to receive
     * data.
     *
     * <p> To displaying preview with a {@link TextureView}, consider
     * using {@link PreviewUtil#createPreviewSurfaceCallback(PreviewUtil.SurfaceTextureCallback)} to
     * create the callback.
     *
     * @param previewSurfaceCallback PreviewSurfaceCallback that provides a Preview.
     * @param callbackExecutor       on which the previewSurfaceCallback will be triggered.
     */
    @UiThread
    public void setPreviewSurfaceCallback(@NonNull Executor callbackExecutor,
            @Nullable PreviewSurfaceCallback previewSurfaceCallback) {
        Threads.checkMainThread();
        if (previewSurfaceCallback == null) {
            mPreviewSurfaceCallback = null;
            notifyInactive();
        } else {
            mPreviewSurfaceCallback = previewSurfaceCallback;
            mPreviewSurfaceCallbackExecutor = callbackExecutor;
            notifyActive();
            if (mLatestResolution != null) {
                updateConfigAndOutput((PreviewConfig) getUseCaseConfig(), mLatestResolution);
            }
        }
    }

    /**
     * Sets a {@link PreviewSurfaceCallback} to provide Surface for Preview.
     *
     * <p> Setting the callback will signal to the camera that the use case is ready to receive
     * data. The callback will be triggered on main thread.
     *
     * @param previewSurfaceCallback PreviewSurfaceCallback that provides a Preview.
     */
    @UiThread
    public void setPreviewSurfaceCallback(@Nullable PreviewSurfaceCallback previewSurfaceCallback) {
        setPreviewSurfaceCallback(CameraXExecutors.mainThreadExecutor(), previewSurfaceCallback);
    }

    /**
     * Checks if {@link PreviewSurfaceCallback} is set by the user.
     */
    @SuppressWarnings("WeakerAccess")
    boolean isPreviewSurfaceCallbackSet() {
        return mPreviewSurfaceCallback != null && mPreviewSurfaceCallbackExecutor != null;
    }

    private void updateConfigAndOutput(PreviewConfig config, Size resolution) {
        Preconditions.checkState(isPreviewSurfaceCallbackSet());
        String cameraId = getCameraIdUnchecked(config);
        attachToCamera(cameraId, createPipeline(config, resolution).build());
    }

    private CameraControlInternal getCurrentCameraControl() {
        PreviewConfig config = (PreviewConfig) getUseCaseConfig();
        String cameraId = getCameraIdUnchecked(config);
        return getCameraControl(cameraId);
    }

    /**
     * Sets torch on/off.
     *
     * When the torch is on, the torch will remain on during photo capture regardless of flash
     * setting.  When the torch is off, flash will function as set by {@link ImageCapture}.
     *
     * @param torch True if turn on torch, otherwise false
     */
    public void enableTorch(boolean torch) {
        getCurrentCameraControl().enableTorch(torch);
    }

    /** True if the torch is on */
    public boolean isTorchOn() {
        return getCurrentCameraControl().isTorchOn();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + ":" + getName();
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @Nullable
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected UseCaseConfig.Builder<?, ?, ?> getDefaultBuilder(LensFacing lensFacing) {
        PreviewConfig defaults = CameraX.getDefaultUseCaseConfig(PreviewConfig.class, lensFacing);
        if (defaults != null) {
            return PreviewConfig.Builder.fromConfig(defaults);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    protected UseCaseConfig<?> applyDefaults(
            @NonNull UseCaseConfig<?> userConfig,
            @Nullable UseCaseConfig.Builder<?, ?, ?> defaultConfigBuilder) {
        PreviewConfig previewConfig = (PreviewConfig) super.applyDefaults(userConfig,
                defaultConfigBuilder);

        CameraDeviceConfig deviceConfig = getBoundDeviceConfig();
        // Checks the device constraints and get the corrected aspect ratio.
        if (deviceConfig != null && CameraX.getSurfaceManager().requiresCorrectedAspectRatio(
                deviceConfig)) {
            ImageOutputConfig imageConfig = previewConfig;
            Rational resultRatio =
                    CameraX.getSurfaceManager().getCorrectedAspectRatio(deviceConfig,
                            imageConfig.getTargetRotation(Surface.ROTATION_0));
            if (resultRatio != null) {
                PreviewConfig.Builder configBuilder = PreviewConfig.Builder.fromConfig(
                        previewConfig);
                configBuilder.setTargetAspectRatioCustom(resultRatio);
                previewConfig = configBuilder.build();
            }
        }

        return previewConfig;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void clear() {
        notifyInactive();
        if (mSurfaceHolder != null) {
            mSurfaceHolder.release();
        }
        super.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    protected Map<String, Size> onSuggestedResolutionUpdated(
            @NonNull Map<String, Size> suggestedResolutionMap) {
        PreviewConfig config = (PreviewConfig) getUseCaseConfig();
        String cameraId = getCameraIdUnchecked(config);
        Size resolution = suggestedResolutionMap.get(cameraId);
        if (resolution == null) {
            throw new IllegalArgumentException(
                    "Suggested resolution map missing resolution for camera " + cameraId);
        }
        mLatestResolution = resolution;
        if (isPreviewSurfaceCallbackSet()) {
            updateConfigAndOutput(config, resolution);
        }
        return suggestedResolutionMap;
    }


    /**
     * A callback to access the Preview Surface.
     */
    public interface PreviewSurfaceCallback {

        /**
         * Creates preview output Surface with the given resolution and format.
         *
         * <p> This is called when Preview needs a valid {@link Surface}. e.g. when the Preview is
         * bound to lifecycle. If the {@link Surface} is backed by a {@link SurfaceTexture}, both
         * the {@link Surface} and the {@link ListenableFuture} need to be recreated each time this
         * is invoked. The implementer is also responsible to hold a reference to the
         * {@link SurfaceTexture} since the weak reference from {@link Surface} does not prevent
         * it to be garbage collected.
         *
         * <p> To display the preview with the correct orientation, if the {@link Surface} is
         * backed by a {@link SurfaceTexture}, {@link SurfaceTexture#getTransformMatrix(float[])}
         * can be used to transform the preview to natural orientation ({@link TextureView}
         * handles this automatically); if the {@link Surface} is backed by a {@link SurfaceView}
         * , it will always be in display orientation; for {@link Surface} backed by
         * {@link ImageReader}, {@link MediaCodec} or other objects, it's implementer's
         * responsibility to calculate the rotation.
         *
         * <p> It's most common to use it with a {@link SurfaceView} or a {@link TextureView}.
         * For {@link TextureView}, see {@link PreviewUtil} for creating {@link Surface} backed
         * by a {@link SurfaceTexture}. For {@link SurfaceView}, the creation is in the
         * hands of the {@link SurfaceView}. Use {@link CallbackToFutureAdapter} to wait for the
         * creation of the {@link Surface} in {@link android.view.SurfaceHolder.Callback
         * #surfaceChanged(android.view.SurfaceHolder, int, int, int)}. Example:
         *
         * <pre><code>
         * class SurfaceViewHandler implements SurfaceHolder.Callback, PreviewSurfaceCallback {
         *
         *     Size mResolution;
         *     CallbackToFutureAdapter.Completer<Surface> mCompleter;
         *
         *     &#64;Override
         *     public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
         *         if (mResolution != null && mCompleter != null && mResolution.getHeight()
         *                 == height && mResolution.getWidth() == width) {
         *             mCompleter.set(holder.getSurface());
         *         }
         *     }
         *
         *     &#64;Override
         *     public ListenableFuture<Surface> createSurfaceFuture(@NonNull Size resolution,
         *             int imageFormat) {
         *         mResolution = resolution;
         *         return CallbackToFutureAdapter.getFuture(completer -> {
         *             mCompleter = completer
         *         });
         *     }
         * }
         * </code></pre>
         *
         * @param resolution  the resolution required by CameraX.
         * @param imageFormat the {@link ImageFormat} required by CameraX.
         * @return A ListenableFuture that contains the implementer created Surface.
         */
        @NonNull
        ListenableFuture<Surface> createSurfaceFuture(@NonNull Size resolution, int imageFormat);

        /**
         * Called when the {@link Surface} is safe to be released.
         *
         * <p> This method is called when the {@link Surface} previously returned from
         * {@link #createSurfaceFuture(Size, int)} is no longer being used by the camera system, and
         * it's safe to be released during or after this is called. The implementer is
         * responsible to release the {@link Surface} when it's also no longer being used by the
         * app.
         *
         * @param surfaceFuture the {@link Surface} to be released.
         */
        void onSafeToRelease(@NonNull ListenableFuture<Surface> surfaceFuture);
    }

    /**
     * Provides a base static default configuration for the Preview
     *
     * <p>These values may be overridden by the implementation. They only provide a minimum set of
     * defaults that are implementation independent.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class Defaults implements ConfigProvider<PreviewConfig> {
        private static final Size DEFAULT_MAX_RESOLUTION =
                CameraX.getSurfaceManager().getPreviewSize();
        private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 2;

        private static final PreviewConfig DEFAULT_CONFIG;

        static {
            PreviewConfig.Builder builder =
                    new PreviewConfig.Builder()
                            .setMaxResolution(DEFAULT_MAX_RESOLUTION)
                            .setSurfaceOccupancyPriority(DEFAULT_SURFACE_OCCUPANCY_PRIORITY);
            DEFAULT_CONFIG = builder.build();
        }

        @Override
        public PreviewConfig getConfig(LensFacing lensFacing) {
            if (lensFacing != null) {
                PreviewConfig.Builder configBuilder = PreviewConfig.Builder.fromConfig(
                        DEFAULT_CONFIG);
                configBuilder.setLensFacing(lensFacing);
                return configBuilder.build();
            } else {
                return DEFAULT_CONFIG;
            }
        }
    }
}
