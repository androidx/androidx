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
import static androidx.camera.core.MirrorMode.MIRROR_MODE_UNSPECIFIED;
import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
import static androidx.camera.core.impl.ImageInputConfig.OPTION_INPUT_DYNAMIC_RANGE;
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
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_TYPE;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_HIGH_RESOLUTION_DISABLED;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_PREVIEW_STABILIZATION_MODE;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_TARGET_FRAME_RATE;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_ZSL_DISABLED;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.core.util.Preconditions.checkNotNull;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.os.Build;
import android.util.Pair;
import android.util.Range;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import androidx.camera.core.impl.ImageInputConfig;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.capability.PreviewCapabilitiesImpl;
import androidx.camera.core.impl.stabilization.StabilizationMode;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.TargetConfig;
import androidx.camera.core.internal.ThreadConfig;
import androidx.camera.core.processing.Node;
import androidx.camera.core.processing.SurfaceEdge;
import androidx.camera.core.processing.SurfaceProcessorNode;
import androidx.camera.core.processing.util.OutConfig;
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
public final class Preview extends UseCase {

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase lifetime constant] - Stays constant for the lifetime of the UseCase. Which means
    // they could be created in the constructor.
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Provides a static configuration with implementation-agnostic options.
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

    @SuppressWarnings("WeakerAccess") // Synthetic accessor
    SessionConfig.Builder mSessionConfigBuilder;

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

    @Nullable
    private SurfaceProcessorNode mNode;
    @Nullable
    private SessionConfig.CloseableErrorListener mCloseableErrorListener;

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

    /**
     * Creates the post-processing pipeline with the {@link Node} pattern.
     *
     * <p> After we migrate everything to {@link Node}, this will become the canonical way to
     * build pipeline .
     */
    @NonNull
    @MainThread
    private SessionConfig.Builder createPipeline(
            @NonNull PreviewConfig config,
            @NonNull StreamSpec streamSpec) {
        // Check arguments
        checkMainThread();

        CameraInternal camera = requireNonNull(getCamera());
        clearPipeline();

        // Make sure the previously created camera edge is cleared before creating a new one.
        checkState(mCameraEdge == null);
        mCameraEdge = new SurfaceEdge(
                PREVIEW,
                INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                streamSpec,
                getSensorToBufferTransformMatrix(),
                camera.getHasTransform(),
                requireNonNull(getCropRect(streamSpec.getResolution())),
                getRelativeRotation(camera, isMirroringRequired(camera)),
                getAppTargetRotation(),
                shouldMirror(camera));

        CameraEffect effect = getEffect();
        if (effect != null) {
            // Create nodes and edges.
            mNode = new SurfaceProcessorNode(camera, effect.createSurfaceProcessorInternal());
            mCameraEdge.addOnInvalidatedListener(this::notifyReset);
            OutConfig outConfig = OutConfig.of(mCameraEdge);
            SurfaceProcessorNode.In nodeInput = SurfaceProcessorNode.In.of(mCameraEdge,
                    singletonList(outConfig));
            SurfaceProcessorNode.Out nodeOutput = mNode.transform(nodeInput);
            SurfaceEdge appEdge = requireNonNull(nodeOutput.get(outConfig));
            appEdge.addOnInvalidatedListener(() -> onAppEdgeInvalidated(mCameraEdge, camera));
            mCurrentSurfaceRequest = appEdge.createSurfaceRequest(camera);
            mSessionDeferrableSurface = mCameraEdge.getDeferrableSurface();
        } else {
            mCameraEdge.addOnInvalidatedListener(this::notifyReset);
            mCurrentSurfaceRequest = mCameraEdge.createSurfaceRequest(camera);
            mSessionDeferrableSurface = mCurrentSurfaceRequest.getDeferrableSurface();
        }

        if (mSurfaceProvider != null) {
            // Only send surface request if the provider is set.
            sendSurfaceRequest();
        }

        // Send the camera Surface to the camera2.
        SessionConfig.Builder sessionConfigBuilder = SessionConfig.Builder.createFrom(config,
                streamSpec.getResolution());
        // Applies the AE fps range to the session config builder according to the stream spec and
        // quirk values.
        applyExpectedFrameRateRange(sessionConfigBuilder, streamSpec);
        sessionConfigBuilder.setPreviewStabilization(config.getPreviewStabilizationMode());
        if (streamSpec.getImplementationOptions() != null) {
            sessionConfigBuilder.addImplementationOptions(streamSpec.getImplementationOptions());
        }
        addCameraSurfaceAndErrorListener(sessionConfigBuilder, streamSpec);
        return sessionConfigBuilder;
    }

    @MainThread
    private void onAppEdgeInvalidated(@NonNull SurfaceEdge cameraEdge,
            @NonNull CameraInternal camera) {
        checkMainThread();
        if (camera == getCamera()) {
            cameraEdge.invalidate();
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
        // Closes the old error listener
        if (mCloseableErrorListener != null) {
            mCloseableErrorListener.close();
            mCloseableErrorListener = null;
        }

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
            @NonNull StreamSpec streamSpec) {
        // TODO(b/245309800): Add the Surface if post-processing pipeline is used. Post-processing
        //  pipeline always provide a Surface.

        // Not to add deferrable surface if the surface provider is not set, as that means the
        // surface will never be provided. For simplicity, the same rule also applies to
        // SurfaceProcessorNode and CaptureProcessor cases, since no surface provider also means no
        // output target for these two cases.
        if (mSurfaceProvider != null) {
            sessionConfigBuilder.addSurface(mSessionDeferrableSurface,
                    streamSpec.getDynamicRange(),
                    getPhysicalCameraId(),
                    getMirrorModeInternal());
        }

        if (mCloseableErrorListener != null) {
            mCloseableErrorListener.close();
        }
        mCloseableErrorListener = new SessionConfig.CloseableErrorListener(
                (sessionConfig, error) -> {
                    // Do nothing when the use case has been unbound.
                    if (getCamera() == null) {
                        return;
                    }

                    updateConfigAndOutput((PreviewConfig) getCurrentConfig(),
                            getAttachedStreamSpec());
                    notifyReset();
                });

        sessionConfigBuilder.setErrorListener(mCloseableErrorListener);
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
        SurfaceEdge cameraEdge = mCameraEdge;
        if (cameraInternal != null && cameraEdge != null) {
            cameraEdge.updateTransformation(
                    getRelativeRotation(cameraInternal, isMirroringRequired(cameraInternal)),
                    getAppTargetRotation());
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

            // It could be a previous request has already been sent, which means the caller wants
            // to replace the Surface. Or, it could be the pipeline has not started. Or the use
            // case may have been detached from the camera. Either way, try updating session
            // config and let createPipeline() sends a new SurfaceRequest.
            if (getAttachedSurfaceResolution() != null) {
                updateConfigAndOutput((PreviewConfig) getCurrentConfig(),
                        getAttachedStreamSpec());
                notifyReset();
            }
            notifyActive();
        }
    }

    /** Gets the {@link SurfaceProvider} associated with the preview. */
    @VisibleForTesting
    @RestrictTo(Scope.LIBRARY_GROUP)
    @UiThread
    @Nullable
    public SurfaceProvider getSurfaceProvider() {
        checkMainThread();
        return mSurfaceProvider;
    }

    private void sendSurfaceRequest() {
        // App receives TransformationInfo when 1) the listener is set or 2) the info is sent. We
        // should send the info before the listen is set so the app only receives once.
        sendTransformationInfoIfReady();

        // Send the SurfaceRequest.
        final SurfaceProvider surfaceProvider = checkNotNull(mSurfaceProvider);
        final SurfaceRequest surfaceRequest = checkNotNull(mCurrentSurfaceRequest);
        mSurfaceProviderExecutor.execute(() -> surfaceProvider.onSurfaceRequested(surfaceRequest));
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

    private void updateConfigAndOutput(@NonNull PreviewConfig config,
            @NonNull StreamSpec streamSpec) {
        mSessionConfigBuilder = createPipeline(config, streamSpec);
        updateSessionConfig(List.of(mSessionConfigBuilder.build()));
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
    public ResolutionInfo getResolutionInfo() {
        return getResolutionInfoInternal();
    }

    /**
     * Returns the resolution selector setting.
     *
     * <p>This setting is set when constructing a Preview using
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
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public UseCaseConfig<?> getDefaultConfig(boolean applyDefaultConfig,
            @NonNull UseCaseConfigFactory factory) {
        Config captureConfig = factory.getConfig(
                DEFAULT_CONFIG.getConfig().getCaptureType(),
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY);

        if (applyDefaultConfig) {
            captureConfig = Config.mergeConfigs(captureConfig, DEFAULT_CONFIG.getConfig());
        }

        return captureConfig == null ? null :
                getUseCaseConfigBuilder(captureConfig).getUseCaseConfig();
    }

    /**
     * {@inheritDoc}
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
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public UseCaseConfig.Builder<?, ?, ?> getUseCaseConfigBuilder(@NonNull Config config) {
        return Preview.Builder.fromConfig(config);
    }

    /**
     * {@inheritDoc}
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void onUnbind() {
        clearPipeline();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    protected StreamSpec onSuggestedStreamSpecUpdated(
            @NonNull StreamSpec primaryStreamSpec,
            @Nullable StreamSpec secondaryStreamSpec) {
        updateConfigAndOutput((PreviewConfig) getCurrentConfig(), primaryStreamSpec);
        return primaryStreamSpec;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected StreamSpec onSuggestedStreamSpecImplementationOptionsUpdated(@NonNull Config config) {
        mSessionConfigBuilder.addImplementationOptions(config);
        updateSessionConfig(List.of(mSessionConfigBuilder.build()));
        return getAttachedStreamSpec().toBuilder().setImplementationOptions(config).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RestrictTo(Scope.LIBRARY)
    public void setViewPortCropRect(@NonNull Rect viewPortCropRect) {
        super.setViewPortCropRect(viewPortCropRect);
        sendTransformationInfoIfReady();
    }

    /**
     *
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
     * Returns the target frame rate range, in frames per second, for the associated Preview use
     * case.
     * <p>The target frame rate can be set prior to constructing a Preview using
     * {@link Preview.Builder#setTargetFrameRate(Range)}.
     * If not set, the target frame rate defaults to the value of
     * {@link StreamSpec#FRAME_RATE_RANGE_UNSPECIFIED}.
     *
     * <p>This is just the frame rate range requested by the user, and may not necessarily be
     * equal to the range the camera is actually operating at.
     *
     * @return the target frame rate range of this Preview.
     */
    @NonNull
    public Range<Integer> getTargetFrameRate() {
        return getTargetFrameRateInternal();
    }

    /**
     * Returns the dynamic range.
     *
     * <p>The dynamic range is set by {@link Preview.Builder#setDynamicRange(DynamicRange)}.
     * If the dynamic range set is not a fully defined dynamic range, such as
     * {@link DynamicRange#HDR_UNSPECIFIED_10_BIT}, then it will be returned just as provided,
     * and will not be returned as a fully defined dynamic range. The fully defined dynamic
     * range, which is determined by resolving the combination of requested dynamic ranges from
     * other use cases according to the device capabilities, will be
     * communicated to the {@link Preview.SurfaceProvider} via
     * {@link SurfaceRequest#getDynamicRange()}}.
     *
     * <p>If the dynamic range was not provided to
     * {@link Preview.Builder#setDynamicRange(DynamicRange)}, this will return the default of
     * {@link DynamicRange#UNSPECIFIED}
     *
     * @return the dynamic range set for this {@code Preview} use case.
     * @see Preview.Builder#setDynamicRange(DynamicRange)
     */
    // Internal implementation note: this method should not be used to retrieve the dynamic range
    // that will be sent to the SurfaceProvider. That should always be retrieved from the StreamSpec
    // since that will be the final DynamicRange chosen by the camera based on other use case
    // combinations.
    @NonNull
    public DynamicRange getDynamicRange() {
        return getCurrentConfig().hasDynamicRange() ? getCurrentConfig().getDynamicRange() :
                Defaults.DEFAULT_DYNAMIC_RANGE;
    }

    /**
     * Returns {@link PreviewCapabilities} to query preview stream related device capability.
     *
     * @return {@link PreviewCapabilities}
     */
    @NonNull
    public static PreviewCapabilities getPreviewCapabilities(@NonNull CameraInfo cameraInfo) {
        return PreviewCapabilitiesImpl.from(cameraInfo);
    }

    /**
     * Returns whether video stabilization is enabled for preview stream.
     */
    public boolean isPreviewStabilizationEnabled() {
        return getCurrentConfig().getPreviewStabilizationMode() == StabilizationMode.ON;
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

        /**
         * Preview uses an UNSPECIFIED dynamic range by default. This means the dynamic range can be
         * inherited from other use cases during dynamic range resolution when the use case is
         * bound.
         */
        private static final DynamicRange DEFAULT_DYNAMIC_RANGE = DynamicRange.UNSPECIFIED;

        static {
            Builder builder = new Builder()
                    .setSurfaceOccupancyPriority(DEFAULT_SURFACE_OCCUPANCY_PRIORITY)
                    .setTargetAspectRatio(DEFAULT_ASPECT_RATIO)
                    .setResolutionSelector(DEFAULT_RESOLUTION_SELECTOR)
                    .setDynamicRange(DEFAULT_DYNAMIC_RANGE);
            DEFAULT_CONFIG = builder.getUseCaseConfig();
        }

        @NonNull
        @Override
        public PreviewConfig getConfig() {
            return DEFAULT_CONFIG;
        }
    }

    /** Builder for a {@link Preview}. */
    @SuppressWarnings({"ObjectToString", "HiddenSuperclass"})
    public static final class Builder
            implements UseCaseConfig.Builder<Preview, PreviewConfig, Builder>,
            ImageOutputConfig.Builder<Builder>,
            ImageInputConfig.Builder<Builder>,
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

            setCaptureType(UseCaseConfigFactory.CaptureType.PREVIEW);
            setTargetClass(Preview.class);

            if (mutableConfig.retrieveOption(
                    OPTION_MIRROR_MODE, MIRROR_MODE_UNSPECIFIED) == MIRROR_MODE_UNSPECIFIED) {
                mutableConfig.insertOption(OPTION_MIRROR_MODE, Defaults.DEFAULT_MIRROR_MODE);
            }
        }

        /**
         * Generates a Builder from another Config object
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
         * Sets the mirror mode.
         *
         * <p>Valid values include: {@link MirrorMode#MIRROR_MODE_OFF},
         * {@link MirrorMode#MIRROR_MODE_ON} and {@link MirrorMode#MIRROR_MODE_ON_FRONT_ONLY}.
         * If not set, it defaults to {@link MirrorMode#MIRROR_MODE_ON_FRONT_ONLY}.
         *
         * <p>For API 33 and above, it will change the mirroring behavior for Preview use case.
         * It is calling
         * {@link android.hardware.camera2.params.OutputConfiguration#setMirrorMode(int)}.
         *
         * <p> For API 32 and below, it will be no-op.
         *
         * @param mirrorMode The mirror mode of the intended target.
         * @return The current Builder.
         * @see android.hardware.camera2.params.OutputConfiguration#setMirrorMode(int)
         */
        @ExperimentalMirrorMode
        @NonNull
        @Override
        public Builder setMirrorMode(@MirrorMode.Mirror int mirrorMode) {
            if (Build.VERSION.SDK_INT >= 33) {
                getMutableConfig().insertOption(OPTION_MIRROR_MODE, mirrorMode);
            }
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
         * section in {@link android.hardware.camera2.CameraDevice}'. Applications can set any
         * {@link ResolutionStrategy} to override it.
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

        // Implementations of ImageInputConfig.Builder default methods

        /**
         * Sets the {@link DynamicRange}.
         *
         * <p>Dynamic range specifies how the range of colors, highlights and shadows captured by
         * the frame producer are represented on a display. Some dynamic ranges allow the preview
         * surface to make full use of the extended range of brightness of the display.
         *
         * <p>The supported dynamic ranges for preview depend on the capabilities of the
         * camera and the ability of the {@link Surface} provided by the
         * {@link Preview.SurfaceProvider} to consume the dynamic range. The supported dynamic
         * ranges of the camera can be queried using
         * {@link CameraInfo#querySupportedDynamicRanges(Set)}.
         *
         * <p>As an example, having written an OpenGL frame processing pipeline that can properly
         * handle input dynamic ranges {@link DynamicRange#SDR}, {@link DynamicRange#HLG_10_BIT} and
         * {@link DynamicRange#HDR10_10_BIT}, it's possible to filter those dynamic
         * ranges based on which dynamic ranges the camera can produce via the {@link Preview}
         * use case:
         * <pre>
         *   <code>
         *
         *        // Constant defining the dynamic ranges supported as input for
         *        // my OpenGL processing pipeline. These will either be outputted
         *        // in the same dynamic range as the input or will be tone-mapped
         *        // to another dynamic range by my pipeline.
         *        List&lt;DynamicRange&gt; MY_SUPPORTED_DYNAMIC_RANGES = Set.of(
         *                DynamicRange.SDR,
         *                DynamicRange.HLG_10_BIT,
         *                DynamicRange.HDR10_10_BIT);
         *        ...
         *
         *        // Query dynamic ranges supported by the camera from the
         *        // dynamic ranges supported by my processing pipeline.
         *        mSupportedHighDynamicRanges =
         *                mCameraInfo.querySupportedDynamicRanges(
         *                        mySupportedDynamicRanges);
         *
         *        // Update our UI picker for dynamic range.
         *        ...
         *
         *
         *        // Create the Preview use case from the dynamic range
         *        // selected by the UI picker.
         *        mPreview = new Preview.Builder()
         *                .setDynamicRange(mSelectedDynamicRange)
         *                .build();
         *   </code>
         * </pre>
         *
         * <p>If the dynamic range is not provided, the returned {@code Preview} use case will use
         * a default of {@link DynamicRange#UNSPECIFIED}. When a {@code Preview} is bound with
         * other use cases that specify a dynamic range, such as
         * {@link androidx.camera.video.VideoCapture}, and the preview dynamic range is {@code
         * UNSPECIFIED}, the resulting dynamic range of the preview will usually match the other
         * use case's dynamic range. If no other use cases are bound with the preview, an
         * {@code UNSPECIFIED} dynamic range will resolve to {@link DynamicRange#SDR}. When
         * using a {@code Preview} with another use case, it is recommended to leave the dynamic
         * range of the {@code Preview} as {@link DynamicRange#UNSPECIFIED}, so the camera can
         * choose the best supported dynamic range that satisfies the requirements of both use
         * cases.
         *
         * <p>If an unspecified dynamic range is used, the resolved fully-defined dynamic range of
         * frames sent from the camera will be communicated to the
         * {@link Preview.SurfaceProvider} via {@link SurfaceRequest#getDynamicRange()}, and the
         * provided {@link Surface} should be configured to use that dynamic range.
         *
         * <p>It is possible to choose a high dynamic range (HDR) with unspecified encoding by
         * providing {@link DynamicRange#HDR_UNSPECIFIED_10_BIT}.
         *
         * @return The current Builder.
         * @see DynamicRange
         * @see CameraInfo#querySupportedDynamicRanges(Set)
         */
        @NonNull
        @Override
        public Builder setDynamicRange(@NonNull DynamicRange dynamicRange) {
            getMutableConfig().insertOption(OPTION_INPUT_DYNAMIC_RANGE, dynamicRange);
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

        /**
         * Sets the target frame rate range in frames per second for the associated Preview use
         * case.
         *
         * <p>
         * Device will try to get as close as possible to the target frame rate. This may affect
         * the selected resolutions of the surfaces, resulting in better frame rates at the
         * potential reduction of resolution.
         *
         * <p>
         * Achieving target frame rate is dependent on device capabilities, as well as other
         * concurrently attached use cases and their target frame rates.
         * Because of this, the frame rate that is ultimately selected is not guaranteed to be a
         * perfect match to the requested target.
         *
         * @param targetFrameRate a desired frame rate range.
         * @return the current Builder.
         */
        @NonNull
        public Builder setTargetFrameRate(@NonNull Range<Integer> targetFrameRate) {
            getMutableConfig().insertOption(OPTION_TARGET_FRAME_RATE, targetFrameRate);
            return this;
        }

        /**
         * Enable preview stabilization. It will enable stabilization for both the preview and
         * video capture use cases.
         *
         * <p>Devices running Android 13 or higher can provide support for video stabilization.
         * This feature lets apps provide a what you see is what you get (WYSIWYG) experience
         * when comparing between the camera preview and the recording.
         *
         * <p>It is recommended to query the device capability via
         * {@link PreviewCapabilities#isStabilizationSupported()} before enabling this feature,
         * otherwise HAL error might be thrown.
         *
         * <p> If both preview stabilization and video stabilization are enabled or disabled, the
         * final result will be
         *
         * <p>
         * <table>
         * <tr> <th id="rb">Preview</th> <th id="rb">VideoCapture</th>   <th id="rb">Result</th>
         * </tr>
         * <tr> <td>ON</td> <td>ON</td> <td>Both Preview and VideoCapture will be stabilized,
         * VideoCapture quality might be worse than only VideoCapture stabilized</td>
         * </tr>
         * <tr> <td>ON</td> <td>OFF</td> <td>None of Preview and VideoCapture will be
         * stabilized</td>  </tr>
         * <tr> <td>ON</td> <td>NOT SPECIFIED</td> <td>Both Preview and VideoCapture will be
         * stabilized</td>  </tr>
         * <tr> <td>OFF</td> <td>ON</td> <td>None of Preview and VideoCapture will be
         * stabilized</td>  </tr>
         * <tr> <td>OFF</td> <td>OFF</td> <td>None of Preview and VideoCapture will be
         * stabilized</td>  </tr>
         * <tr> <td>OFF</td> <td>NOT SPECIFIED</td> <td>None of Preview and VideoCapture will be
         * stabilized</td>  </tr>
         * <tr> <td>NOT SPECIFIED</td> <td>ON</td> <td>Only VideoCapture will be stabilized,
         * Preview might be stabilized depending on devices</td>
         * </tr>
         * <tr> <td>NOT SPECIFIED</td> <td>OFF</td> <td>None of Preview and VideoCapture will be
         * stabilized</td>  </tr>
         * </table><br>
         *
         * @param enabled True if enable, otherwise false.
         * @return the current Builder.
         * @see PreviewCapabilities#isStabilizationSupported()
         */
        @NonNull
        public Builder setPreviewStabilizationEnabled(boolean enabled) {
            getMutableConfig().insertOption(OPTION_PREVIEW_STABILIZATION_MODE,
                    enabled ? StabilizationMode.ON : StabilizationMode.OFF);
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

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setCaptureType(@NonNull UseCaseConfigFactory.CaptureType captureType) {
            getMutableConfig().insertOption(OPTION_CAPTURE_TYPE, captureType);
            return this;
        }
    }
}
