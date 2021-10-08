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

import static androidx.camera.core.impl.ImageInputConfig.OPTION_INPUT_FORMAT;
import static androidx.camera.core.impl.PreviewConfig.IMAGE_INFO_PROCESSOR;
import static androidx.camera.core.impl.PreviewConfig.OPTION_BACKGROUND_EXECUTOR;
import static androidx.camera.core.impl.PreviewConfig.OPTION_CAPTURE_CONFIG_UNPACKER;
import static androidx.camera.core.impl.PreviewConfig.OPTION_DEFAULT_CAPTURE_CONFIG;
import static androidx.camera.core.impl.PreviewConfig.OPTION_DEFAULT_RESOLUTION;
import static androidx.camera.core.impl.PreviewConfig.OPTION_DEFAULT_SESSION_CONFIG;
import static androidx.camera.core.impl.PreviewConfig.OPTION_MAX_RESOLUTION;
import static androidx.camera.core.impl.PreviewConfig.OPTION_PREVIEW_CAPTURE_PROCESSOR;
import static androidx.camera.core.impl.PreviewConfig.OPTION_SESSION_CONFIG_UNPACKER;
import static androidx.camera.core.impl.PreviewConfig.OPTION_SUPPORTED_RESOLUTIONS;
import static androidx.camera.core.impl.PreviewConfig.OPTION_SURFACE_OCCUPANCY_PRIORITY;
import static androidx.camera.core.impl.PreviewConfig.OPTION_TARGET_ASPECT_RATIO;
import static androidx.camera.core.impl.PreviewConfig.OPTION_TARGET_CLASS;
import static androidx.camera.core.impl.PreviewConfig.OPTION_TARGET_NAME;
import static androidx.camera.core.impl.PreviewConfig.OPTION_TARGET_RESOLUTION;
import static androidx.camera.core.impl.PreviewConfig.OPTION_TARGET_ROTATION;
import static androidx.camera.core.impl.PreviewConfig.OPTION_USE_CASE_EVENT_CALLBACK;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAMERA_SELECTOR;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.CaptureStage;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.ImageInfoProcessor;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraCaptureResultImageInfo;
import androidx.camera.core.internal.TargetConfig;
import androidx.camera.core.internal.ThreadConfig;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * A use case that provides a camera preview stream for displaying on-screen.
 *
 * <p>The preview stream is connected to the {@link Surface} provided via
 * {@link SurfaceProvider}. The application decides how the {@link Surface} is shown,
 * and is responsible for managing the {@link Surface} lifecycle after providing it.
 *
 * <p> To display the preview with the correct orientation, app needs to take different actions
 * based on the source of the Surface. If the {@link Surface} is backed by a {@link SurfaceView},
 * it will always be in the device's display orientation. If the {@link Surface} is backed by
 * {@link ImageReader}, {@link MediaCodec} or other objects, it's the application's
 * responsibility to calculate the rotation. If the {@link Surface} is backed by a
 * {@link SurfaceTexture}, {@link SurfaceTexture#getTransformMatrix(float[])} can be used to
 * transform the preview to natural orientation. The value is available after a frame is pushed
 * to the {@link SurfaceTexture} and its
 * {@link SurfaceTexture.OnFrameAvailableListener#onFrameAvailable(SurfaceTexture)} has been called.
 * {@link TextureView} handles this automatically and always puts the preview in the
 * natural orientation. To further transform the {@link TextureView} to display orientation,
 * the app needs to apply the current display rotation. Example:
 * <pre>
 *     <code>
 *         switch (getWindowManager().getDefaultDisplay().getRotation()) {
 *             case Surface.ROTATION_0:
 *                 displayRotation = 0;
 *                 break;
 *             case Surface.ROTATION_90:
 *                 displayRotation = 90;
 *                 break;
 *             case Surface.ROTATION_180:
 *                 displayRotation = 180;
 *                 break;
 *             case Surface.ROTATION_270:
 *                 displayRotation = 270;
 *                 break;
 *             default:
 *                 throw new UnsupportedOperationException(
 *                         "Unsupported display rotation: " + displayRotation);
 *         }
 *         matrix.postRotate(-displayRotation, centerX, centerY);
 *         textureView.setTransform(matrix);
 *     </code>
 * </pre>
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Preview extends UseCase {

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase lifetime constant] - Stays constant for the lifetime of the UseCase. Which means
    // they could be created in the constructor.
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Provides a static configuration with implementation-agnostic options.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final Defaults DEFAULT_CONFIG = new Defaults();
    private static final String TAG = "Preview";
    private static final Executor DEFAULT_SURFACE_PROVIDER_EXECUTOR =
            CameraXExecutors.mainThreadExecutor();

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase lifetime dynamic] - Dynamic variables which could change during anytime during
    // the UseCase lifetime.
    ////////////////////////////////////////////////////////////////////////////////////////////

    @Nullable
    private SurfaceProvider mSurfaceProvider;

    @NonNull
    private Executor mSurfaceProviderExecutor = DEFAULT_SURFACE_PROVIDER_EXECUTOR;

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase attached dynamic] - Can change but is only available when the UseCase is attached.
    ////////////////////////////////////////////////////////////////////////////////////////////

    private DeferrableSurface mSessionDeferrableSurface;

    @VisibleForTesting
    @Nullable
    SurfaceRequest mCurrentSurfaceRequest;
    // Flag indicates that there is a SurfaceRequest created by Preview but hasn't sent to the
    // caller.
    private boolean mHasUnsentSurfaceRequest = false;
    // The attached surface size. Same as getAttachedSurfaceResolution() but is available during
    // createPipeline().
    @Nullable
    private Size mSurfaceSize;

    /**
     * Creates a new preview use case from the given configuration.
     *
     * @param config for this use case instance
     */
    @SuppressWarnings("WeakerAccess")
    @MainThread
    Preview(@NonNull PreviewConfig config) {
        super(config);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    SessionConfig.Builder createPipeline(@NonNull String cameraId, @NonNull PreviewConfig config,
            @NonNull Size resolution) {
        Threads.checkMainThread();
        SessionConfig.Builder sessionConfigBuilder = SessionConfig.Builder.createFrom(config);
        final CaptureProcessor captureProcessor = config.getCaptureProcessor(null);

        // Close previous session's deferrable surface before creating new one
        if (mSessionDeferrableSurface != null) {
            mSessionDeferrableSurface.close();
        }

        final SurfaceRequest surfaceRequest = new SurfaceRequest(resolution, getCamera(),
                captureProcessor != null);
        mCurrentSurfaceRequest = surfaceRequest;

        if (sendSurfaceRequestIfReady()) {
            sendTransformationInfoIfReady();
        } else {
            mHasUnsentSurfaceRequest = true;
        }

        if (captureProcessor != null) {
            CaptureStage captureStage = new CaptureStage.DefaultCaptureStage();
            // TODO: To allow user to use an Executor for the processing.
            HandlerThread handlerThread = new HandlerThread(
                    CameraXThreads.TAG + "preview_processing");
            handlerThread.start();

            String tagBundleKey = Integer.toString(captureStage.hashCode());

            ProcessingSurface processingSurface = new ProcessingSurface(
                    resolution.getWidth(),
                    resolution.getHeight(),
                    config.getInputFormat(),
                    new Handler(handlerThread.getLooper()),
                    captureStage,
                    captureProcessor,
                    surfaceRequest.getDeferrableSurface(),
                    tagBundleKey);

            sessionConfigBuilder.addCameraCaptureCallback(
                    processingSurface.getCameraCaptureCallback());

            processingSurface.getTerminationFuture().addListener(handlerThread::quitSafely,
                    CameraXExecutors.directExecutor());

            mSessionDeferrableSurface = processingSurface;

            // Use CaptureStage object as the key for TagBundle
            sessionConfigBuilder.addTag(tagBundleKey, captureStage.getId());
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
            mSessionDeferrableSurface = surfaceRequest.getDeferrableSurface();
        }
        sessionConfigBuilder.addSurface(mSessionDeferrableSurface);
        sessionConfigBuilder.addErrorListener((sessionConfig, error) -> {
            // Ensure the attached camera has not changed before resetting.
            // TODO(b/143915543): Ensure this never gets called by a camera that is not attached
            //  to this use case so we don't need to do this check.
            if (isCurrentCamera(cameraId)) {
                // Only reset the pipeline when the bound camera is the same.
                SessionConfig.Builder sessionConfigBuilder1 = createPipeline(cameraId, config,
                        resolution);

                updateSessionConfig(sessionConfigBuilder1.build());
                notifyReset();
            }
        });

        return sessionConfigBuilder;
    }

    /**
     * Sets the target rotation.
     *
     * <p>This adjust the {@link Preview#getTargetRotation()}, which once applied will update the
     * output to match target rotation specified here.
     *
     * <p>While rotation can also be set via {@link Preview.Builder#setTargetRotation(int)}
     * , using {@link Preview#setTargetRotation(int)} allows the target rotation to be set
     * without rebinding new use cases. When this function is called, value set by
     * {@link Preview.Builder#setTargetResolution(Size)} will be updated automatically to
     * make sure the suitable resolution can be selected when the use case is bound.
     *
     * <p>If not set here or by configuration, the target rotation will default to the value of
     * {@link Display#getRotation()} of the default display at the time the use case is created. The
     * use case is fully created once it has been attached to a camera.
     *
     * @param targetRotation Target rotation of the output image, expressed as one of
     *                       {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     *                       {@link Surface#ROTATION_180}, or {@link Surface#ROTATION_270}.
     * @see Preview.Builder#setTargetRotation(int)
     */
    public void setTargetRotation(@ImageOutputConfig.RotationValue int targetRotation) {
        if (setTargetRotationInternal(targetRotation)) {
            sendTransformationInfoIfReady();
        }
    }

    private void sendTransformationInfoIfReady() {
        // TODO(b/159659392): only send transformation after CameraCaptureCallback
        //  .onCaptureCompleted is called.
        CameraInternal cameraInternal = getCamera();
        SurfaceProvider surfaceProvider = mSurfaceProvider;
        Rect cropRect = getCropRect(mSurfaceSize);
        SurfaceRequest surfaceRequest = mCurrentSurfaceRequest;
        if (cameraInternal != null && surfaceProvider != null && cropRect != null) {
            surfaceRequest.updateTransformationInfo(SurfaceRequest.TransformationInfo.of(cropRect,
                    getRelativeRotation(cameraInternal), getTargetRotation()));
        }
    }

    /**
     * Gets the crop rect for {@link Preview}.
     *
     * <p> Fall back to the full {@link Surface} rect if {@link ViewPort} crop rect is not
     * available. Returns null if no valid crop rect. This could happen if the {@link Preview} is
     * not attached to a camera.
     */
    @Nullable
    private Rect getCropRect(@Nullable Size surfaceResolution) {
        if (getViewPortCropRect() != null) {
            return getViewPortCropRect();
        } else if (surfaceResolution != null) {
            return new Rect(0, 0, surfaceResolution.getWidth(), surfaceResolution.getHeight());
        }
        return null;
    }

    /**
     * Sets a {@link SurfaceProvider} to provide a {@link Surface} for Preview.
     *
     * <p> Setting the provider will signal to the camera that the use case is ready to receive
     * data. If the provider is removed by calling this again with a {@code null} SurfaceProvider
     * then the camera will stop producing data for this Preview instance.
     *
     * @param executor        on which the surfaceProvider will be invoked.
     * @param surfaceProvider SurfaceProvider that provides a {@link Surface} for Preview. This
     *                        will replace the previous SurfaceProvider set either this method or
     *                        {@link #setSurfaceProvider(SurfaceProvider)}.
     */
    @UiThread
    public void setSurfaceProvider(@NonNull Executor executor,
            @Nullable SurfaceProvider surfaceProvider) {
        Threads.checkMainThread();
        if (surfaceProvider == null) {
            // SurfaceProvider is removed. Inactivate the use case.
            mSurfaceProvider = null;
            notifyInactive();
        } else {
            mSurfaceProvider = surfaceProvider;
            mSurfaceProviderExecutor = executor;
            notifyActive();

            if (mHasUnsentSurfaceRequest) {
                if (sendSurfaceRequestIfReady()) {
                    sendTransformationInfoIfReady();
                    mHasUnsentSurfaceRequest = false;
                }
            } else {
                // No pending SurfaceRequest. It could be a previous request has already been
                // sent, which means the caller wants to replace the Surface. Or, it could be the
                // pipeline has not started. Or the use case may have been detached from the camera.
                // Either way, try updating session config and let createPipeline() sends a
                // new SurfaceRequest.
                if (getAttachedSurfaceResolution() != null) {
                    updateConfigAndOutput(getCameraId(), (PreviewConfig) getCurrentConfig(),
                            getAttachedSurfaceResolution());
                    notifyReset();
                }
            }
        }
    }

    private boolean sendSurfaceRequestIfReady() {
        final SurfaceRequest surfaceRequest = mCurrentSurfaceRequest;
        final SurfaceProvider surfaceProvider = mSurfaceProvider;
        if (surfaceProvider != null && surfaceRequest != null) {
            mSurfaceProviderExecutor.execute(
                    () -> surfaceProvider.onSurfaceRequested(surfaceRequest));
            return true;
        }
        return false;
    }

    /**
     * Sets a {@link SurfaceProvider} to provide a {@link Surface} for Preview.
     *
     * <p> Setting the provider will signal to the camera that the use case is ready to receive
     * data. The provider will be triggered on main thread. If the provider is removed by calling
     * this again with a {@code null} SurfaceProvider then the camera will stop producing data for
     * this Preview instance.
     *
     * @param surfaceProvider SurfaceProvider that provides a {@link Surface} for Preview. This
     *                        will replace the previous SurfaceProvider set either this method or
     *                        {@link #setSurfaceProvider(Executor, SurfaceProvider)}.
     */
    @UiThread
    public void setSurfaceProvider(@Nullable SurfaceProvider surfaceProvider) {
        setSurfaceProvider(DEFAULT_SURFACE_PROVIDER_EXECUTOR, surfaceProvider);
    }

    private void updateConfigAndOutput(@NonNull String cameraId, @NonNull PreviewConfig config,
            @NonNull Size resolution) {
        updateSessionConfig(createPipeline(cameraId, config, resolution).build());
    }

    /**
     * Returns the rotation that the intended target resolution is expressed in.
     *
     * <p>
     * The rotation is set when constructing an {@link Preview} instance using
     * {@link Preview.Builder#setTargetRotation(int)}. If not set, the target rotation defaults to
     * the value of {@link Display#getRotation()} of the default display at the time the use case
     * is created. The use case is fully created once it has been attached to a camera.
     * </p>
     *
     * @return The rotation of the intended target.
     */
    @ImageOutputConfig.RotationValue
    public int getTargetRotation() {
        return getTargetRotationInternal();
    }

    /**
     * Gets selected resolution information of the {@link Preview}.
     *
     * <p>The returned {@link ResolutionInfo} will be expressed in the coordinates of the camera
     * sensor. It will be the same as the resolution inside a {@link SurfaceRequest} to request a
     * surface for {@link Preview}.
     *
     * <p>The resolution information might change if the use case is unbound and then rebound or
     * {@link #setTargetRotation(int)} is called to change the target rotation setting. The
     * application needs to call {@link #getResolutionInfo()} again to get the latest
     * {@link ResolutionInfo} for the changes.
     *
     * @return the resolution information if the use case has been bound by the
     * {@link androidx.camera.lifecycle.ProcessCameraProvider#bindToLifecycle(LifecycleOwner
     * , CameraSelector, UseCase...)} API, or null if the use case is not bound yet.
     */
    @Nullable
    @Override
    public ResolutionInfo getResolutionInfo() {
        return super.getResolutionInfo();
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
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public UseCaseConfig<?> getDefaultConfig(boolean applyDefaultConfig,
            @NonNull UseCaseConfigFactory factory) {
        Config captureConfig = factory.getConfig(UseCaseConfigFactory.CaptureType.PREVIEW);

        if (applyDefaultConfig) {
            captureConfig = Config.mergeConfigs(captureConfig, DEFAULT_CONFIG.getConfig());
        }

        return captureConfig == null ? null :
                getUseCaseConfigBuilder(captureConfig).getUseCaseConfig();
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    protected UseCaseConfig<?> onMergeConfig(@NonNull CameraInfoInternal cameraInfo,
            @NonNull UseCaseConfig.Builder<?, ?, ?> builder) {
        if (builder.getMutableConfig().retrieveOption(OPTION_PREVIEW_CAPTURE_PROCESSOR, null)
                != null) {
            builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT, ImageFormat.YUV_420_888);
        } else {
            builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT,
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE);
        }
        return builder.getUseCaseConfig();
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public UseCaseConfig.Builder<?, ?, ?> getUseCaseConfigBuilder(@NonNull Config config) {
        return Preview.Builder.fromConfig(config);
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void onDetached() {
        if (mSessionDeferrableSurface != null) {
            mSessionDeferrableSurface.close();
        }

        mCurrentSurfaceRequest = null;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    protected Size onSuggestedResolutionUpdated(@NonNull Size suggestedResolution) {
        mSurfaceSize = suggestedResolution;
        updateConfigAndOutput(getCameraId(), (PreviewConfig) getCurrentConfig(),
                mSurfaceSize);
        return suggestedResolution;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @RestrictTo(Scope.LIBRARY)
    public void setViewPortCropRect(@NonNull Rect viewPortCropRect) {
        super.setViewPortCropRect(viewPortCropRect);
        sendTransformationInfoIfReady();
    }

    /**
     * A interface implemented by the application to provide a {@link Surface} for {@link Preview}.
     *
     * <p> This interface is implemented by the application to provide a {@link Surface}. This
     * will be called by CameraX when it needs a Surface for Preview. It also signals when the
     * Surface is no longer in use by CameraX.
     *
     * @see Preview#setSurfaceProvider(Executor, SurfaceProvider)
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public interface SurfaceProvider {
        /**
         * Called when a new {@link Surface} has been requested by the camera.
         *
         * <p>This is called every time a new surface is required to keep the preview running.
         * The camera may repeatedly request surfaces throughout usage of a Preview use case, but
         * only a single request will be active at a time.
         *
         * <p>A request is considered active until it is
         *
         * {@linkplain SurfaceRequest#provideSurface(Surface, Executor, androidx.core.util.Consumer)
         * fulfilled}, {@linkplain SurfaceRequest#willNotProvideSurface() marked as 'will not
         * complete'}, or
         * {@linkplain SurfaceRequest#addRequestCancellationListener(Executor, Runnable) cancelled
         * by the camera}. After one of these conditions occurs, a request is considered completed.
         *
         * <p>Once a request is successfully completed, it is guaranteed that if a new request is
         * made, the {@link Surface} used to fulfill the previous request will be detached from the
         * camera and {@link SurfaceRequest#provideSurface(Surface, Executor, Consumer)} will be
         * invoked with a {@link androidx.camera.core.SurfaceRequest.Result} containing
         * {@link androidx.camera.core.SurfaceRequest.Result#RESULT_SURFACE_USED_SUCCESSFULLY}.
         *
         * Example:
         *
         * <pre>
         * class MyGlSurfaceProvider implements Preview.SurfaceProvider {
         *     // This executor must have also been used with Preview.setSurfaceProvider() to
         *     // ensure onSurfaceRequested() is called on our GL thread.
         *     Executor mGlExecutor;
         *
         *     {@literal @}Override
         *     public void onSurfaceRequested(@NonNull SurfaceRequest request) {
         *         // If our GL thread/context is shutting down. Signal we will not fulfill
         *         // the request.
         *         if (isShuttingDown()) {
         *             request.willNotProvideSurface();
         *             return;
         *         }
         *
         *         // Create the surface and attempt to provide it to the camera.
         *         Surface surface = resetGlInputSurface(request.getResolution());
         *
         *         // Provide the surface and wait for the result to clean up the surface.
         *         request.provideSurface(surface, mGlExecutor, (result) -> {
         *             // In all cases (even errors), we can clean up the state. As an
         *             // optimization, we could also optionally check for REQUEST_CANCELLED
         *             // since we may be able to reuse the surface on subsequent surface requests.
         *             closeGlInputSurface(surface);
         *         });
         *     }
         * }
         * </pre>
         *
         * @param request the request for a surface which contains the requirements of the
         *                surface and methods for completing the request.
         */
        void onSurfaceRequested(@NonNull SurfaceRequest request);
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
        private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 2;
        private static final int DEFAULT_ASPECT_RATIO = AspectRatio.RATIO_4_3;

        private static final PreviewConfig DEFAULT_CONFIG;

        static {
            Builder builder = new Builder().setSurfaceOccupancyPriority(
                    DEFAULT_SURFACE_OCCUPANCY_PRIORITY).setTargetAspectRatio(DEFAULT_ASPECT_RATIO);
            DEFAULT_CONFIG = builder.getUseCaseConfig();
        }

        @NonNull
        @Override
        public PreviewConfig getConfig() {
            return DEFAULT_CONFIG;
        }
    }

    /** Builder for a {@link Preview}. */
    @SuppressWarnings("ObjectToString")
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Builder
            implements UseCaseConfig.Builder<Preview, PreviewConfig, Builder>,
            ImageOutputConfig.Builder<Builder>,
            ThreadConfig.Builder<Builder> {

        private final MutableOptionsBundle mMutableConfig;

        /** Creates a new Builder object. */
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfig.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(Preview.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(Preview.class);
        }

        /**
         * Generates a Builder from another Config object
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        static Builder fromConfig(@NonNull Config configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * Generates a Builder from another Config object
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Builder fromConfig(@NonNull PreviewConfig configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * {@inheritDoc}
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public MutableConfig getMutableConfig() {
            return mMutableConfig;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public PreviewConfig getUseCaseConfig() {
            return new PreviewConfig(OptionsBundle.from(mMutableConfig));
        }

        /**
         * Builds an immutable {@link Preview} from the current state.
         *
         * @return A {@link Preview} populated with the current state.
         * @throws IllegalArgumentException if attempting to set both target aspect ratio and
         *                                  target resolution.
         */
        @NonNull
        @Override
        public Preview build() {
            // Error at runtime for using both setTargetResolution and setTargetAspectRatio on
            // the same config.
            if (getMutableConfig().retrieveOption(OPTION_TARGET_ASPECT_RATIO, null) != null
                    && getMutableConfig().retrieveOption(OPTION_TARGET_RESOLUTION, null) != null) {
                throw new IllegalArgumentException(
                        "Cannot use both setTargetResolution and setTargetAspectRatio on the same "
                                + "config.");
            }

            return new Preview(getUseCaseConfig());
        }

        // Implementations of TargetConfig.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setTargetClass(@NonNull Class<Preview> targetClass) {
            getMutableConfig().insertOption(OPTION_TARGET_CLASS, targetClass);

            // If no name is set yet, then generate a unique name
            if (null == getMutableConfig().retrieveOption(OPTION_TARGET_NAME, null)) {
                String targetName = targetClass.getCanonicalName() + "-" + UUID.randomUUID();
                setTargetName(targetName);
            }

            return this;
        }

        /**
         * Sets the name of the target object being configured, used only for debug logging.
         *
         * <p>The name should be a value that can uniquely identify an instance of the object being
         * configured.
         *
         * <p>If not set, the target name will default to an unique name automatically generated
         * with the class canonical name and random UUID.
         *
         * @param targetName A unique string identifier for the instance of the class being
         *                   configured.
         * @return the current Builder.
         */
        @Override
        @NonNull
        public Builder setTargetName(@NonNull String targetName) {
            getMutableConfig().insertOption(OPTION_TARGET_NAME, targetName);
            return this;
        }

        // Implementations of ImageOutputConfig.Builder default methods

        /**
         * Sets the aspect ratio of the intended target for images from this configuration.
         *
         * <p>The aspect ratio is the ratio of width to height in the sensor orientation.
         *
         * <p>It is not allowed to set both target aspect ratio and target resolution on the same
         * use case. Attempting so will throw an IllegalArgumentException when building the Config.
         *
         * <p>The target aspect ratio is used as a hint when determining the resulting output aspect
         * ratio which may differ from the request, possibly due to device constraints.
         * Application code should check the resulting output's resolution and the resulting aspect
         * ratio may not be exactly as requested.
         *
         * <p>For Preview, the value will be used to calculate the suggested resolution size in
         * {@link SurfaceRequest#getResolution()}.
         *
         * <p>If not set, resolutions with aspect ratio 4:3 will be considered in higher
         * priority.
         *
         * <p>For the following devices, the aspect ratio will be forced to
         * {@link AspectRatio#RATIO_16_9} regardless of the config. On these devices, the
         * camera HAL produces a preview with a 16:9 aspect ratio regardless of the aspect ratio
         * of the preview surface.
         * <ul>
         *     <li>SM-J710MN, Samsung Galaxy J7 (2016)
         *     <li>SM-T580, Samsung Galaxy Tab A J7 (2016)
         * </ul>
         *
         * @param aspectRatio The desired Preview {@link AspectRatio}
         * @return The current Builder.
         */
        @NonNull
        @Override
        public Builder setTargetAspectRatio(@AspectRatio.Ratio int aspectRatio) {
            getMutableConfig().insertOption(OPTION_TARGET_ASPECT_RATIO, aspectRatio);
            return this;
        }

        /**
         * Sets the rotation that the intended target resolution is expressed in.
         *
         * <p>This sets the rotation that is used when specifying a target resolution using
         * {@link #setTargetResolution(Size)}, which accepts a resolution at the target orientation.
         *
         * <p>rotation is one of four valid values: {@link Surface#ROTATION_0}, {@link
         * Surface#ROTATION_90}, {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}.
         * Rotation values are relative to the "natural" rotation, {@link Surface#ROTATION_0}.
         *
         * <p>For example a portrait natural device may specify a portrait image target resolution
         * as 480x640, and the same device rotated to and displaying in landscape (i.e. as
         * returned by {@link Display#getRotation()}) may set the target rotation to
         * {@link Surface#ROTATION_90} and resolution to 640x480.
         *
         * <p>If not set, the target rotation will default to the value of
         * {@link Display#getRotation()} of the default display at the time the use case is created.
         * The use case is fully created once it has been attached to a camera.
         *
         * <p> Note that {@link SurfaceView} does not support non-display rotation. If the target
         * rotation is different than the value of {@link Display#getRotation()},
         * {@link SurfaceView} should not be used to provide the {@link Surface} in
         * {@link SurfaceRequest#provideSurface(Surface, Executor, Consumer)}
         *
         * @param rotation The rotation of the intended target.
         * @return The current Builder.
         * @see #setTargetResolution(Size)
         */
        @NonNull
        @Override
        public Builder setTargetRotation(@ImageOutputConfig.RotationValue int rotation) {
            getMutableConfig().insertOption(OPTION_TARGET_ROTATION, rotation);
            return this;
        }

        /**
         * Sets the resolution of the intended target from this configuration.
         *
         * <p>The target resolution attempts to establish a minimum bound for the preview
         * resolution. The actual preview resolution will be the closest available resolution in
         * size that is not smaller than the target resolution, as determined by the Camera
         * implementation. However, if no resolution exists that is equal to or larger than the
         * target resolution, the nearest available resolution smaller than the target resolution
         * will be chosen.  Resolutions with the same aspect ratio of the provided {@link Size} will
         * be considered in higher priority before resolutions of different aspect ratios.
         *
         * <p>It is not allowed to set both target aspect ratio and target resolution on the same
         * use case. Attempting so will throw an IllegalArgumentException when building the Config.
         *
         * <p>The resolution {@link Size} should be expressed in the coordinate frame after
         * rotating the supported sizes by the target rotation. For example, a device with
         * portrait natural orientation in natural target rotation requesting a portrait image
         * may specify 480x640, and the same device, rotated 90 degrees and targeting landscape
         * orientation may specify 640x480.
         *
         * <p>The maximum available resolution that could be selected for a {@link Preview} is
         * limited to be under 1080p. The limitation of 1080p for {@link Preview} has considered
         * both performance and quality factors that users can obtain reasonable quality and smooth
         * output stream under 1080p.
         *
         * <p>If not set, the default selected resolution will be the best size match to the
         * device's screen resolution, or to 1080p (1920x1080), whichever is smaller.
         *
         * <p>When using the <code>camera-camera2</code> CameraX implementation, which resolution
         * will be finally selected will depend on the camera device's hardware level and the
         * bound use cases combination. For more details see the guaranteed supported
         * configurations tables in {@link android.hardware.camera2.CameraDevice}'s
         * <a href="https://developer.android.com/reference/android/hardware/camera2/CameraDevice#regular-capture">Regular capture</a> section.
         *
         * @param resolution The target resolution to choose from supported output sizes list.
         * @return The current Builder.
         */
        @NonNull
        @Override
        public Builder setTargetResolution(@NonNull Size resolution) {
            getMutableConfig()
                    .insertOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION, resolution);
            return this;
        }

        /**
         * Sets the default resolution of the intended target from this configuration.
         *
         * @param resolution The default resolution to choose from supported output sizes list.
         * @return The current Builder.
         * @hide
         */
        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setDefaultResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_DEFAULT_RESOLUTION, resolution);
            return this;
        }

        /** @hide */
        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setMaxResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_MAX_RESOLUTION, resolution);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSupportedResolutions(@NonNull List<Pair<Integer, Size[]>> resolutions) {
            getMutableConfig().insertOption(OPTION_SUPPORTED_RESOLUTIONS, resolutions);
            return this;
        }

        // Implementations of ThreadConfig.Builder default methods

        /**
         * Sets the default executor that will be used for background tasks.
         *
         * <p>If not set, the background executor will default to an automatically generated
         * {@link Executor}.
         *
         * @param executor The executor which will be used for background tasks.
         * @return the current Builder.
         * @hide Background executor not used in {@link Preview}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setBackgroundExecutor(@NonNull Executor executor) {
            getMutableConfig().insertOption(OPTION_BACKGROUND_EXECUTOR, executor);
            return this;
        }

        // Implementations of UseCaseConfig.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setDefaultSessionConfig(@NonNull SessionConfig sessionConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setDefaultCaptureConfig(@NonNull CaptureConfig captureConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_CAPTURE_CONFIG, captureConfig);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSessionOptionUnpacker(
                @NonNull SessionConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_SESSION_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setCaptureOptionUnpacker(
                @NonNull CaptureConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_CAPTURE_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setCameraSelector(@NonNull CameraSelector cameraSelector) {
            getMutableConfig().insertOption(OPTION_CAMERA_SELECTOR, cameraSelector);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setUseCaseEventCallback(
                @NonNull UseCase.EventCallback useCaseEventCallback) {
            getMutableConfig().insertOption(OPTION_USE_CASE_EVENT_CALLBACK, useCaseEventCallback);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setImageInfoProcessor(@NonNull ImageInfoProcessor processor) {
            getMutableConfig().insertOption(IMAGE_INFO_PROCESSOR, processor);
            return this;
        }

        /**
         * Sets the {@link CaptureProcessor}.
         *
         * @param captureProcessor The requested capture processor for extension.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setCaptureProcessor(@NonNull CaptureProcessor captureProcessor) {
            getMutableConfig().insertOption(OPTION_PREVIEW_CAPTURE_PROCESSOR, captureProcessor);
            return this;
        }
    }
}
