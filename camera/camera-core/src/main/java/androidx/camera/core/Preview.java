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

import static androidx.camera.core.CameraEffect.PREVIEW;
import static androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY;
import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
import static androidx.camera.core.impl.ImageInputConfig.OPTION_INPUT_FORMAT;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_APP_TARGET_ROTATION;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_CUSTOM_ORDERED_RESOLUTIONS;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_MIRROR_MODE;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_RESOLUTION_SELECTOR;
import static androidx.camera.core.impl.PreviewConfig.OPTION_BACKGROUND_EXECUTOR;
import static androidx.camera.core.impl.PreviewConfig.OPTION_CAPTURE_CONFIG_UNPACKER;
import static androidx.camera.core.impl.PreviewConfig.OPTION_DEFAULT_CAPTURE_CONFIG;
import static androidx.camera.core.impl.PreviewConfig.OPTION_DEFAULT_RESOLUTION;
import static androidx.camera.core.impl.PreviewConfig.OPTION_DEFAULT_SESSION_CONFIG;
import static androidx.camera.core.impl.PreviewConfig.OPTION_MAX_RESOLUTION;
import static androidx.camera.core.impl.PreviewConfig.OPTION_SESSION_CONFIG_UNPACKER;
import static androidx.camera.core.impl.PreviewConfig.OPTION_SUPPORTED_RESOLUTIONS;
import static androidx.camera.core.impl.PreviewConfig.OPTION_SURFACE_OCCUPANCY_PRIORITY;
import static androidx.camera.core.impl.PreviewConfig.OPTION_TARGET_ASPECT_RATIO;
import static androidx.camera.core.impl.PreviewConfig.OPTION_TARGET_CLASS;
import static androidx.camera.core.impl.PreviewConfig.OPTION_TARGET_NAME;
import static androidx.camera.core.impl.PreviewConfig.OPTION_TARGET_ROTATION;
import static androidx.camera.core.impl.PreviewConfig.OPTION_USE_CASE_EVENT_CALLBACK;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAMERA_SELECTOR;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_HIGH_RESOLUTION_DISABLED;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_ZSL_DISABLED;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.core.util.Preconditions.checkNotNull;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.util.Pair;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.IntRange;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.TargetConfig;
import androidx.camera.core.internal.ThreadConfig;
import androidx.camera.core.processing.Node;
import androidx.camera.core.processing.SurfaceEdge;
import androidx.camera.core.processing.SurfaceProcessorNode;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    // TODO(b/259308680): remove mSessionDeferrableSurface and rely on mCameraEdge to get the
    //  DeferrableSurface
    private DeferrableSurface mSessionDeferrableSurface;
    @Nullable
    private SurfaceEdge mCameraEdge;

    // TODO(b/259308680): remove mSessionDeferrableSurface and rely on appEdge to get the
    //  SurfaceRequest
    @VisibleForTesting
    @Nullable
    SurfaceRequest mCurrentSurfaceRequest;

    // The attached surface size. Same as getAttachedSurfaceResolution() but is available during
    // createPipeline().
    @Nullable
    private Size mSurfaceSize;

    @Nullable
    private SurfaceProcessorNode mNode;

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

    @MainThread
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    SessionConfig.Builder createPipeline(@NonNull String cameraId, @NonNull PreviewConfig config,
            @NonNull StreamSpec streamSpec) {
        // Build pipeline with node if processor is set. Eventually we will move all the code to
        // createPipelineWithNode.
        if (getEffect() != null) {
            return createPipelineWithNode(cameraId, config, streamSpec);
        }

        checkMainThread();
        SessionConfig.Builder sessionConfigBuilder = SessionConfig.Builder.createFrom(config,
                streamSpec.getResolution());

        // Close previous session's deferrable surface before creating new one
        clearPipeline();

        final SurfaceRequest surfaceRequest = new SurfaceRequest(
                streamSpec.getResolution(),
                getCamera(),
                streamSpec.getDynamicRange(),
                streamSpec.getExpectedFrameRateRange(),
                this::notifyReset);
        mCurrentSurfaceRequest = surfaceRequest;

        if (mSurfaceProvider != null) {
            // Only send surface request if the provider is set.
            sendSurfaceRequest();
        }

        mSessionDeferrableSurface = surfaceRequest.getDeferrableSurface();
        addCameraSurfaceAndErrorListener(sessionConfigBuilder, cameraId, config, streamSpec);
        sessionConfigBuilder.setExpectedFrameRateRange(streamSpec.getExpectedFrameRateRange());
        return sessionConfigBuilder;
    }

    /**
     * Creates the post-processing pipeline with the {@link Node} pattern.
     *
     * <p> After we migrate everything to {@link Node}, this will become the canonical way to
     * build pipeline .
     */
    @NonNull
    @MainThread
    private SessionConfig.Builder createPipelineWithNode(
            @NonNull String cameraId,
            @NonNull PreviewConfig config,
            @NonNull StreamSpec streamSpec) {
        // Check arguments
        checkMainThread();
        CameraEffect effect = requireNonNull(getEffect());
        CameraInternal camera = requireNonNull(getCamera());

        clearPipeline();

        // Create nodes and edges.
        mNode = new SurfaceProcessorNode(camera, effect.createSurfaceProcessorInternal());
        // Make sure the previously created camera edge is cleared before creating a new one.
        checkState(mCameraEdge == null);
        mCameraEdge = new SurfaceEdge(
                PREVIEW,
                INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                streamSpec,
                new Matrix(),
                camera.getHasTransform(),
                requireNonNull(getCropRect(streamSpec.getResolution())),
                getRelativeRotation(camera, isMirroringRequired(camera)),
                shouldMirror(camera));
        mCameraEdge.addOnInvalidatedListener(this::notifyReset);
        SurfaceProcessorNode.OutConfig outConfig = SurfaceProcessorNode.OutConfig.of(mCameraEdge);
        SurfaceProcessorNode.In nodeInput = SurfaceProcessorNode.In.of(mCameraEdge,
                singletonList(outConfig));
        SurfaceProcessorNode.Out nodeOutput = mNode.transform(nodeInput);
        SurfaceEdge appEdge = requireNonNull(nodeOutput.get(outConfig));
        appEdge.addOnInvalidatedListener(() -> onAppEdgeInvalidated(appEdge, camera));

        // Send the app Surface to the app.
        mSessionDeferrableSurface = mCameraEdge.getDeferrableSurface();
        mCurrentSurfaceRequest = appEdge.createSurfaceRequest(camera);
        if (mSurfaceProvider != null) {
            // Only send surface request if the provider is set.
            sendSurfaceRequest();
        }

        // Send the camera Surface to the camera2.
        SessionConfig.Builder sessionConfigBuilder = SessionConfig.Builder.createFrom(config,
                streamSpec.getResolution());
        addCameraSurfaceAndErrorListener(sessionConfigBuilder, cameraId, config, streamSpec);
        return sessionConfigBuilder;
    }

    @MainThread
    private void onAppEdgeInvalidated(@NonNull SurfaceEdge appEdge,
            @NonNull CameraInternal camera) {
        checkMainThread();
        if (camera == getCamera()) {
            mCurrentSurfaceRequest = appEdge.createSurfaceRequest(camera);
            sendSurfaceRequest();
        }
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @IntRange(from = 0, to = 359)
    protected int getRelativeRotation(@NonNull CameraInternal cameraInternal,
            boolean requireMirroring) {
        if (cameraInternal.getHasTransform()) {
            return super.getRelativeRotation(cameraInternal, requireMirroring);
        } else {
            // If there is a virtual parent camera, the buffer is already rotated because
            // SurfaceView cannot handle additional rotation.
            return 0;
        }
    }

    private boolean shouldMirror(@NonNull CameraInternal camera) {
        // Since PreviewView cannot mirror, we will always mirror preview stream during buffer
        // copy. If there has been a buffer copy, it means it's already mirrored. Otherwise,
        // mirror it for the front camera.
        return camera.getHasTransform() && isMirroringRequired(camera);
    }

    /**
     * Creates previously allocated {@link DeferrableSurface} include those allocated by nodes.
     */
    private void clearPipeline() {
        DeferrableSurface cameraSurface = mSessionDeferrableSurface;
        if (cameraSurface != null) {
            cameraSurface.close();
            mSessionDeferrableSurface = null;
        }
        SurfaceProcessorNode node = mNode;
        if (node != null) {
            node.release();
            mNode = null;
        }
        SurfaceEdge cameraEdge = mCameraEdge;
        if (cameraEdge != null) {
            cameraEdge.close();
            mCameraEdge = null;
        }
        mCurrentSurfaceRequest = null;
    }

    private void addCameraSurfaceAndErrorListener(
            @NonNull SessionConfig.Builder sessionConfigBuilder,
            @NonNull String cameraId,
            @NonNull PreviewConfig config,
            @NonNull StreamSpec streamSpec) {
        // TODO(b/245309800): Add the Surface if post-processing pipeline is used. Post-processing
        //  pipeline always provide a Surface.

        // Not to add deferrable surface if the surface provider is not set, as that means the
        // surface will never be provided. For simplicity, the same rule also applies to
        // SurfaceProcessorNode and CaptureProcessor cases, since no surface provider also means no
        // output target for these two cases.
        if (mSurfaceProvider != null) {
            sessionConfigBuilder.addSurface(mSessionDeferrableSurface);
        }

        sessionConfigBuilder.addErrorListener((sessionConfig, error) -> {
            // Ensure the attached camera has not changed before resetting.
            // TODO(b/143915543): Ensure this never gets called by a camera that is not attached
            //  to this use case so we don't need to do this check.
            if (isCurrentCamera(cameraId)) {
                // Only reset the pipeline when the bound camera is the same.
                SessionConfig.Builder sessionConfigBuilder1 = createPipeline(cameraId, config,
                        streamSpec);

                updateSessionConfig(sessionConfigBuilder1.build());
                notifyReset();
            }
        });
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
        if (cameraInternal != null && surfaceProvider != null && cropRect != null
                && surfaceRequest != null) {
            if (mNode == null) {
                surfaceRequest.updateTransformationInfo(SurfaceRequest.TransformationInfo.of(
                        cropRect,
                        getRelativeRotation(cameraInternal, isMirroringRequired(cameraInternal)),
                        getAppTargetRotation(),
                        cameraInternal.getHasTransform()));
            } else {
                mCameraEdge.setRotationDegrees(
                        getRelativeRotation(cameraInternal, isMirroringRequired(cameraInternal)));
            }
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
        checkMainThread();
        if (surfaceProvider == null) {
            // SurfaceProvider is removed. Inactivate the use case.
            mSurfaceProvider = null;
            notifyInactive();
        } else {
            mSurfaceProvider = surfaceProvider;
            mSurfaceProviderExecutor = executor;
            notifyActive();

            // It could be a previous request has already been sent, which means the caller wants
            // to replace the Surface. Or, it could be the pipeline has not started. Or the use
            // case may have been detached from the camera. Either way, try updating session
            // config and let createPipeline() sends a new SurfaceRequest.
            if (getAttachedSurfaceResolution() != null) {
                updateConfigAndOutput(getCameraId(), (PreviewConfig) getCurrentConfig(),
                        getAttachedStreamSpec());
                notifyReset();
            }
        }
    }

    private void sendSurfaceRequest() {
        final SurfaceProvider surfaceProvider = checkNotNull(mSurfaceProvider);
        final SurfaceRequest surfaceRequest = checkNotNull(mCurrentSurfaceRequest);

        mSurfaceProviderExecutor.execute(() -> surfaceProvider.onSurfaceRequested(surfaceRequest));
        sendTransformationInfoIfReady();
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
            @NonNull StreamSpec streamSpec) {
        updateSessionConfig(createPipeline(cameraId, config, streamSpec).build());
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
     * {@link androidx.camera.lifecycle.ProcessCameraProvider#bindToLifecycle(LifecycleOwner,
     * CameraSelector, UseCase...)} API, or null if the use case is not bound yet.
     */
    @Nullable
    @Override
    public ResolutionInfo getResolutionInfo() {
        return super.getResolutionInfo();
    }

    /**
     * Returns the resolution selector setting.
     *
     * <p>This setting is set when constructing an ImageCapture using
     * {@link Builder#setResolutionSelector(ResolutionSelector)}.
     */
    @Nullable
    public ResolutionSelector getResolutionSelector() {
        return ((ImageOutputConfig) getCurrentConfig()).getResolutionSelector(null);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + ":" + getName();
    }

    /**
     * {@inheritDoc}
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public UseCaseConfig<?> getDefaultConfig(boolean applyDefaultConfig,
            @NonNull UseCaseConfigFactory factory) {
        Config captureConfig = factory.getConfig(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY);

        if (applyDefaultConfig) {
            captureConfig = Config.mergeConfigs(captureConfig, DEFAULT_CONFIG.getConfig());
        }

        return captureConfig == null ? null :
                getUseCaseConfigBuilder(captureConfig).getUseCaseConfig();
    }

    /**
     * {@inheritDoc}
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    protected UseCaseConfig<?> onMergeConfig(@NonNull CameraInfoInternal cameraInfo,
            @NonNull UseCaseConfig.Builder<?, ?, ?> builder) {
        builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT,
                INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE);

        return builder.getUseCaseConfig();
    }

    /**
     * {@inheritDoc}
     *
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
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void onUnbind() {
        clearPipeline();
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    protected StreamSpec onSuggestedStreamSpecUpdated(@NonNull StreamSpec suggestedStreamSpec) {
        mSurfaceSize = suggestedStreamSpec.getResolution();
        updateConfigAndOutput(getCameraId(), (PreviewConfig) getCurrentConfig(),
                suggestedStreamSpec);
        return suggestedStreamSpec;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    @RestrictTo(Scope.LIBRARY)
    public void setViewPortCropRect(@NonNull Rect viewPortCropRect) {
        super.setViewPortCropRect(viewPortCropRect);
        sendTransformationInfoIfReady();
    }

    /**
     */
    @VisibleForTesting
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public SurfaceEdge getCameraEdge() {
        return requireNonNull(mCameraEdge);
    }

    /**
     * @inheritDoc
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public Set<Integer> getSupportedEffectTargets() {
        Set<Integer> targets = new HashSet<>();
        targets.add(PREVIEW);
        return targets;
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
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class Defaults implements ConfigProvider<PreviewConfig> {
        private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 2;
        private static final int DEFAULT_ASPECT_RATIO = AspectRatio.RATIO_4_3;
        private static final int DEFAULT_MIRROR_MODE = MIRROR_MODE_ON_FRONT_ONLY;

        private static final ResolutionSelector DEFAULT_RESOLUTION_SELECTOR =
                new ResolutionSelector.Builder().setAspectRatioStrategy(
                        AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).setResolutionStrategy(
                        ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY).build();

        private static final PreviewConfig DEFAULT_CONFIG;

        static {
            Builder builder = new Builder()
                    .setSurfaceOccupancyPriority(DEFAULT_SURFACE_OCCUPANCY_PRIORITY)
                    .setTargetAspectRatio(DEFAULT_ASPECT_RATIO)
                    .setResolutionSelector(DEFAULT_RESOLUTION_SELECTOR);
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
            mutableConfig.insertOption(OPTION_MIRROR_MODE, Defaults.DEFAULT_MIRROR_MODE);
        }

        /**
         * Generates a Builder from another Config object
         *
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
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Builder fromConfig(@NonNull PreviewConfig configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * {@inheritDoc}
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public MutableConfig getMutableConfig() {
            return mMutableConfig;
        }

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
            PreviewConfig previewConfig = getUseCaseConfig();
            ImageOutputConfig.validateConfig(previewConfig);
            return new Preview(previewConfig);
        }

        // Implementations of TargetConfig.Builder default methods

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
         * <p>If not set, or {@link AspectRatio#RATIO_DEFAULT} is supplied, resolutions with
         * aspect ratio 4:3 will be considered in higher priority.
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
         * @deprecated use {@link ResolutionSelector} with {@link AspectRatioStrategy} to specify
         * the preferred aspect ratio settings instead.
         */
        @NonNull
        @Override
        @Deprecated
        public Builder setTargetAspectRatio(@AspectRatio.Ratio int aspectRatio) {
            if (aspectRatio == AspectRatio.RATIO_DEFAULT) {
                aspectRatio = Defaults.DEFAULT_ASPECT_RATIO;
            }
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
            // This app specific target rotation will be sent to PreviewView (or other
            // SurfaceProvider) to transform the preview accordingly.
            getMutableConfig().insertOption(OPTION_APP_TARGET_ROTATION, rotation);
            return this;
        }

        /**
         * setMirrorMode is not supported on Preview.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setMirrorMode(@MirrorMode.Mirror int mirrorMode) {
            throw new UnsupportedOperationException("setMirrorMode is not supported.");
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
         * device's screen resolution, or to 1080p (1920x1080), whichever is smaller. Note that
         * due to compatibility reasons, CameraX may select a resolution that is larger than the
         * default screen resolution on certain devices.
         *
         * <p>When using the <code>camera-camera2</code> CameraX implementation, which resolution
         * will be finally selected will depend on the camera device's hardware level and the
         * bound use cases combination. For more details see the guaranteed supported
         * configurations tables in {@link android.hardware.camera2.CameraDevice}'s
         * <a href="https://developer.android.com/reference/android/hardware/camera2/CameraDevice#regular-capture">Regular capture</a> section.
         *
         * @param resolution The target resolution to choose from supported output sizes list.
         * @return The current Builder.
         * @deprecated use {@link ResolutionSelector} with {@link ResolutionStrategy} to specify
         * the preferred resolution settings instead.
         */
        @NonNull
        @Override
        @Deprecated
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
         */
        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setDefaultResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_DEFAULT_RESOLUTION, resolution);
            return this;
        }

        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setMaxResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_MAX_RESOLUTION, resolution);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSupportedResolutions(@NonNull List<Pair<Integer, Size[]>> resolutions) {
            getMutableConfig().insertOption(OPTION_SUPPORTED_RESOLUTIONS, resolutions);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setCustomOrderedResolutions(@NonNull List<Size> resolutions) {
            getMutableConfig().insertOption(OPTION_CUSTOM_ORDERED_RESOLUTIONS, resolutions);
            return this;
        }

        /**
         * Sets the resolution selector to select the preferred supported resolution.
         *
         * <p>When using the {@code camera-camera2} CameraX implementation, the selected
         * resolution will be limited by the {@code PREVIEW} size which is defined as the best
         * size match to the device's screen resolution, or to 1080p (1920x1080), whichever is
         * smaller. See the
         * <a href="https://developer.android.com/reference/android/hardware/camera2/CameraDevice#regular-capture">Regular capture</a>
         * section in {@link android.hardware.camera2.CameraDevice}'. {@link Preview} has a
         * default {@link ResolutionStrategy} with the {@code PREVIEW} bound size and
         * {@link ResolutionStrategy#FALLBACK_RULE_CLOSEST_LOWER} to achieve this. Applications
         * can override this default strategy with a different resolution strategy.
         *
         * <p>Note that due to compatibility reasons, CameraX may select a resolution that is
         * larger than the default screen resolution on certain devices.
         *
         * <p>The existing {@link #setTargetResolution(Size)} and
         * {@link #setTargetAspectRatio(int)} APIs are deprecated and are not compatible with
         * {@link #setResolutionSelector(ResolutionSelector)}. Calling either of these APIs
         * together with {@link #setResolutionSelector(ResolutionSelector)} will result in an
         * {@link IllegalArgumentException} being thrown when you attempt to build the
         * {@link Preview} instance.
         *
         * @return The current Builder.
         */
        @Override
        @NonNull
        public Builder setResolutionSelector(@NonNull ResolutionSelector resolutionSelector) {
            getMutableConfig().insertOption(OPTION_RESOLUTION_SELECTOR, resolutionSelector);
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
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setBackgroundExecutor(@NonNull Executor executor) {
            getMutableConfig().insertOption(OPTION_BACKGROUND_EXECUTOR, executor);
            return this;
        }

        // Implementations of UseCaseConfig.Builder default methods

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setDefaultSessionConfig(@NonNull SessionConfig sessionConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setDefaultCaptureConfig(@NonNull CaptureConfig captureConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_CAPTURE_CONFIG, captureConfig);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSessionOptionUnpacker(
                @NonNull SessionConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_SESSION_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setCaptureOptionUnpacker(
                @NonNull CaptureConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_CAPTURE_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setCameraSelector(@NonNull CameraSelector cameraSelector) {
            getMutableConfig().insertOption(OPTION_CAMERA_SELECTOR, cameraSelector);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setUseCaseEventCallback(
                @NonNull UseCase.EventCallback useCaseEventCallback) {
            getMutableConfig().insertOption(OPTION_USE_CASE_EVENT_CALLBACK, useCaseEventCallback);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setZslDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_ZSL_DISABLED, disabled);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setHighResolutionDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_HIGH_RESOLUTION_DISABLED, disabled);
            return this;
        }
    }
}
