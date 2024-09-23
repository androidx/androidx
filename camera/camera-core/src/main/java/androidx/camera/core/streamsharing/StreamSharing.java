/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.streamsharing;

import static androidx.camera.core.CameraEffect.PREVIEW;
import static androidx.camera.core.CameraEffect.VIDEO_CAPTURE;
import static androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY;
import static androidx.camera.core.impl.CaptureConfig.TEMPLATE_TYPE_NONE;
import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
import static androidx.camera.core.impl.ImageInputConfig.OPTION_INPUT_FORMAT;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_MIRROR_MODE;
import static androidx.camera.core.impl.SessionConfig.getHigherPriorityTemplateType;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_TYPE;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.camera.core.impl.utils.TransformUtils.getRotatedSize;
import static androidx.camera.core.impl.utils.TransformUtils.sizeToRect;
import static androidx.core.util.Preconditions.checkNotNull;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import android.graphics.Rect;
import android.util.Log;
import android.util.Size;

import androidx.annotation.IntRange;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.CompositionSettings;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.MirrorMode;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.processing.DefaultSurfaceProcessor;
import androidx.camera.core.processing.SurfaceEdge;
import androidx.camera.core.processing.SurfaceProcessorNode;
import androidx.camera.core.processing.concurrent.DualOutConfig;
import androidx.camera.core.processing.concurrent.DualSurfaceProcessor;
import androidx.camera.core.processing.concurrent.DualSurfaceProcessorNode;
import androidx.camera.core.processing.util.OutConfig;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link UseCase} that shares one PRIV stream to multiple children {@link UseCase}s.
 */
public class StreamSharing extends UseCase {
    private static final String TAG = "StreamSharing";
    @NonNull
    private final StreamSharingConfig mDefaultConfig;

    @NonNull
    private final VirtualCameraAdapter mVirtualCameraAdapter;
    // The composition settings of primary camera in dual camera case.
    @NonNull
    private final CompositionSettings mCompositionSettings;
    // The composition settings of secondary camera in dual camera case.
    @NonNull
    private final CompositionSettings mSecondaryCompositionSettings;
    // Node that applies effect to the input.
    @Nullable
    private SurfaceProcessorNode mEffectNode;
    // Node that shares a single stream to multiple UseCases.
    @Nullable
    private SurfaceProcessorNode mSharingNode;
    // Node that shares dual streams to multiple UseCases.
    @Nullable
    private DualSurfaceProcessorNode mDualSharingNode;
    // The input edge that connects to the camera.
    @Nullable
    private SurfaceEdge mCameraEdge;
    // The input edge that connects to the secondary camera in dual camera case.
    @Nullable
    private SurfaceEdge mSecondaryCameraEdge;
    // The input edge of the sharing node.
    @Nullable
    private SurfaceEdge mSharingInputEdge;
    // The input edge of the secondary sharing node in dual camera case.
    @Nullable
    private SurfaceEdge mSecondarySharingInputEdge;

    @SuppressWarnings("WeakerAccess") // Synthetic access
    SessionConfig.Builder mSessionConfigBuilder;
    @SuppressWarnings("WeakerAccess") // Synthetic access
    SessionConfig.Builder mSecondarySessionConfigBuilder;

    @Nullable
    private SessionConfig.CloseableErrorListener mCloseableErrorListener;

    private static StreamSharingConfig getDefaultConfig(Set<UseCase> children) {
        MutableConfig mutableConfig = new StreamSharingBuilder().getMutableConfig();
        mutableConfig.insertOption(OPTION_INPUT_FORMAT,
                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE);
        List<UseCaseConfigFactory.CaptureType> captureTypes = new ArrayList<>();
        for (UseCase child : children) {
            if (child.getCurrentConfig().containsOption(OPTION_CAPTURE_TYPE)) {
                captureTypes.add(child.getCurrentConfig().getCaptureType());
            } else {
                Log.e(TAG, "A child does not have capture type.");
            }
        }
        mutableConfig.insertOption(StreamSharingConfig.OPTION_CAPTURE_TYPES, captureTypes);
        mutableConfig.insertOption(OPTION_MIRROR_MODE, MIRROR_MODE_ON_FRONT_ONLY);
        return new StreamSharingConfig(OptionsBundle.from(mutableConfig));
    }

    /**
     * Constructs a {@link StreamSharing} with a parent {@link CameraInternal}, children
     * {@link UseCase}s, and a {@link UseCaseConfigFactory} for getting default {@link UseCase}
     * configurations.
     */
    public StreamSharing(@NonNull CameraInternal camera,
            @Nullable CameraInternal secondaryCamera,
            @NonNull CompositionSettings compositionSettings,
            @NonNull CompositionSettings secondaryCompositionSettings,
            @NonNull Set<UseCase> children,
            @NonNull UseCaseConfigFactory useCaseConfigFactory) {
        super(getDefaultConfig(children));
        mDefaultConfig = getDefaultConfig(children);
        mCompositionSettings = compositionSettings;
        mSecondaryCompositionSettings = secondaryCompositionSettings;
        mVirtualCameraAdapter = new VirtualCameraAdapter(
                camera, secondaryCamera, children, useCaseConfigFactory,
                (jpegQuality, rotationDegrees) -> {
                    SurfaceProcessorNode sharingNode = mSharingNode;
                    if (sharingNode != null) {
                        return sharingNode.getSurfaceProcessor().snapshot(
                                jpegQuality, rotationDegrees);
                    } else {
                        return Futures.immediateFailedFuture(new Exception(
                                "Failed to take picture: pipeline is not ready."));
                    }
                });
    }

    @Nullable
    @Override
    public UseCaseConfig<?> getDefaultConfig(boolean applyDefaultConfig,
            @NonNull UseCaseConfigFactory factory) {
        // The shared stream optimizes for VideoCapture.
        Config captureConfig = factory.getConfig(
                mDefaultConfig.getCaptureType(),
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY);

        if (applyDefaultConfig) {
            captureConfig = Config.mergeConfigs(captureConfig, mDefaultConfig.getConfig());
        }
        return captureConfig == null ? null :
                getUseCaseConfigBuilder(captureConfig).getUseCaseConfig();
    }

    @NonNull
    @Override
    public UseCaseConfig.Builder<?, ?, ?> getUseCaseConfigBuilder(@NonNull Config config) {
        return new StreamSharingBuilder(MutableOptionsBundle.from(config));
    }

    @NonNull
    @Override
    protected UseCaseConfig<?> onMergeConfig(@NonNull CameraInfoInternal cameraInfo,
            @NonNull UseCaseConfig.Builder<?, ?, ?> builder) {
        mVirtualCameraAdapter.mergeChildrenConfigs(builder.getMutableConfig());
        return builder.getUseCaseConfig();
    }

    @NonNull
    @Override
    protected StreamSpec onSuggestedStreamSpecUpdated(
            @NonNull StreamSpec primaryStreamSpec,
            @Nullable StreamSpec secondaryStreamSpec) {
        updateSessionConfig(
                createPipelineAndUpdateChildrenSpecs(getCameraId(),
                        getSecondaryCameraId(),
                        getCurrentConfig(),
                        primaryStreamSpec, secondaryStreamSpec));
        notifyActive();
        return primaryStreamSpec;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected StreamSpec onSuggestedStreamSpecImplementationOptionsUpdated(@NonNull Config config) {
        mSessionConfigBuilder.addImplementationOptions(config);
        updateSessionConfig(List.of(mSessionConfigBuilder.build()));
        return getAttachedStreamSpec().toBuilder().setImplementationOptions(config).build();
    }

    @Override
    public void onBind() {
        super.onBind();
        mVirtualCameraAdapter.bindChildren();
    }

    @Override
    public void onUnbind() {
        super.onUnbind();
        clearPipeline();
        mVirtualCameraAdapter.unbindChildren();
    }

    @Override
    public void onStateAttached() {
        super.onStateAttached();
        mVirtualCameraAdapter.notifyStateAttached();
    }

    @Override
    public void onStateDetached() {
        super.onStateDetached();
        mVirtualCameraAdapter.notifyStateDetached();
    }

    @NonNull
    public Set<UseCase> getChildren() {
        return mVirtualCameraAdapter.getChildren();
    }

    /**
     * StreamSharing supports [PREVIEW, VIDEO_CAPTURE] or [PREVIEW, VIDEO_CAPTURE, IMAGE_CAPTURE].
     */
    @Override
    @NonNull
    public Set<Integer> getSupportedEffectTargets() {
        Set<Integer> targets = new HashSet<>();
        targets.add(PREVIEW | VIDEO_CAPTURE);
        return targets;
    }

    @NonNull
    @MainThread
    private List<SessionConfig> createPipelineAndUpdateChildrenSpecs(
            @NonNull String cameraId,
            @Nullable String secondaryCameraId,
            @NonNull UseCaseConfig<?> config,
            @NonNull StreamSpec primaryStreamSpec,
            @Nullable StreamSpec secondaryStreamSpec) {
        checkMainThread();

        if (secondaryStreamSpec == null) {
            // primary
            createPrimaryCamera(cameraId, secondaryCameraId,
                    config, primaryStreamSpec, null);

            // sharing node
            mSharingNode = getSharingNode(requireNonNull(getCamera()), primaryStreamSpec);

            boolean isViewportSet = getViewPortCropRect() != null;
            Map<UseCase, OutConfig> outConfigMap =
                    mVirtualCameraAdapter.getChildrenOutConfigs(mSharingInputEdge,
                            getTargetRotationInternal(), isViewportSet);
            SurfaceProcessorNode.Out out = mSharingNode.transform(
                    SurfaceProcessorNode.In.of(mSharingInputEdge,
                            new ArrayList<>(outConfigMap.values())));

            Map<UseCase, SurfaceEdge> outputEdges = new HashMap<>();
            for (Map.Entry<UseCase, OutConfig> entry : outConfigMap.entrySet()) {
                outputEdges.put(entry.getKey(), out.get(entry.getValue()));
            }

            mVirtualCameraAdapter.setChildrenEdges(outputEdges);

            return List.of(mSessionConfigBuilder.build());
        } else {
            // primary
            createPrimaryCamera(cameraId, secondaryCameraId,
                    config, primaryStreamSpec, secondaryStreamSpec);

            // secondary
            createSecondaryCamera(cameraId, secondaryCameraId,
                    config, primaryStreamSpec, secondaryStreamSpec);

            // sharing node
            mDualSharingNode = getDualSharingNode(
                    getCamera(),
                    getSecondaryCamera(),
                    primaryStreamSpec, // use primary stream spec
                    mCompositionSettings,
                    mSecondaryCompositionSettings);
            boolean isViewportSet = getViewPortCropRect() != null;
            Map<UseCase, DualOutConfig> outConfigMap =
                    mVirtualCameraAdapter.getChildrenOutConfigs(
                            mSharingInputEdge,
                            mSecondarySharingInputEdge,
                            getTargetRotationInternal(),
                            isViewportSet);
            DualSurfaceProcessorNode.Out out = mDualSharingNode.transform(
                    DualSurfaceProcessorNode.In.of(
                            mSharingInputEdge,
                            mSecondarySharingInputEdge,
                            new ArrayList<>(outConfigMap.values())));

            Map<UseCase, SurfaceEdge> outputEdges = new HashMap<>();
            for (Map.Entry<UseCase, DualOutConfig> entry : outConfigMap.entrySet()) {
                outputEdges.put(entry.getKey(), out.get(entry.getValue()));
            }
            mVirtualCameraAdapter.setChildrenEdges(outputEdges);

            return List.of(mSessionConfigBuilder.build(),
                    mSecondarySessionConfigBuilder.build());
        }
    }

    private void createPrimaryCamera(
            @NonNull String cameraId,
            @Nullable String secondaryCameraId,
            @NonNull UseCaseConfig<?> config,
            @NonNull StreamSpec primaryStreamSpec,
            @Nullable StreamSpec secondaryStreamSpec) {
        mCameraEdge = new SurfaceEdge(
                /*targets=*/PREVIEW | VIDEO_CAPTURE,
                INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                primaryStreamSpec,
                getSensorToBufferTransformMatrix(),
                requireNonNull(getCamera()).getHasTransform(),
                requireNonNull(getCropRect(primaryStreamSpec.getResolution())),
                getRelativeRotation(requireNonNull(getCamera())),
                ImageOutputConfig.ROTATION_NOT_SPECIFIED,
                isMirroringRequired(requireNonNull(getCamera())));
        mSharingInputEdge = getSharingInputEdge(mCameraEdge, requireNonNull(getCamera()));

        mSessionConfigBuilder = createSessionConfigBuilder(
                mCameraEdge, config, primaryStreamSpec);
        addCameraErrorListener(mSessionConfigBuilder,
                cameraId, secondaryCameraId, config,
                primaryStreamSpec, secondaryStreamSpec);
    }

    private void createSecondaryCamera(
            @NonNull String cameraId,
            @Nullable String secondaryCameraId,
            @NonNull UseCaseConfig<?> config,
            @NonNull StreamSpec primaryStreamSpec,
            @Nullable StreamSpec secondaryStreamSpec) {
        mSecondaryCameraEdge = new SurfaceEdge(
                /*targets=*/PREVIEW | VIDEO_CAPTURE,
                INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                secondaryStreamSpec,
                getSensorToBufferTransformMatrix(),
                requireNonNull(getSecondaryCamera()).getHasTransform(),
                requireNonNull(getCropRect(secondaryStreamSpec.getResolution())),
                getRelativeRotation(requireNonNull(getSecondaryCamera())),
                ImageOutputConfig.ROTATION_NOT_SPECIFIED,
                isMirroringRequired(requireNonNull(getSecondaryCamera())));
        mSecondarySharingInputEdge = getSharingInputEdge(mSecondaryCameraEdge,
                requireNonNull(getSecondaryCamera()));

        mSecondarySessionConfigBuilder = createSessionConfigBuilder(
                mSecondaryCameraEdge, config, secondaryStreamSpec);
        addCameraErrorListener(mSecondarySessionConfigBuilder,
                cameraId, secondaryCameraId, config,
                primaryStreamSpec, secondaryStreamSpec);
    }

    @NonNull
    private SessionConfig.Builder createSessionConfigBuilder(
            @NonNull SurfaceEdge surfaceEdge,
            @NonNull UseCaseConfig<?> config,
            @NonNull StreamSpec streamSpec) {
        // Send the camera edge Surface to the camera2.
        SessionConfig.Builder builder = SessionConfig.Builder.createFrom(config,
                streamSpec.getResolution());
        propagateChildrenTemplate(builder);
        propagateChildrenCamera2Interop(streamSpec.getResolution(), builder);

        DeferrableSurface deferrableSurface = surfaceEdge.getDeferrableSurface();
        builder.addSurface(deferrableSurface,
                streamSpec.getDynamicRange(),
                null,
                MirrorMode.MIRROR_MODE_UNSPECIFIED);
        builder.addRepeatingCameraCaptureCallback(
                mVirtualCameraAdapter.getParentMetadataCallback());
        if (streamSpec.getImplementationOptions() != null) {
            builder.addImplementationOptions(streamSpec.getImplementationOptions());
        }
        // Applies the AE fps range to the session config builder according to the stream spec and
        // quirk values.
        applyExpectedFrameRateRange(builder, streamSpec);
        return builder;
    }

    private void propagateChildrenTemplate(@NonNull SessionConfig.Builder builder) {
        int targetTemplate = TEMPLATE_TYPE_NONE;
        for (UseCase child : getChildren()) {
            targetTemplate = getHigherPriorityTemplateType(targetTemplate, getChildTemplate(child));
        }
        if (targetTemplate != TEMPLATE_TYPE_NONE) {
            builder.setTemplateType(targetTemplate);
        }
    }

    private static int getChildTemplate(@NonNull UseCase useCase) {
        return useCase.getCurrentConfig().getDefaultSessionConfig().getTemplateType();
    }


    /**
     * Propagates children Camera2interop settings.
     */
    private void propagateChildrenCamera2Interop(
            @NonNull Size resolution,
            @NonNull SessionConfig.Builder builder) {
        for (UseCase useCase : getChildren()) {
            SessionConfig childConfig =
                    SessionConfig.Builder.createFrom(useCase.getCurrentConfig(), resolution)
                            .build();
            builder.addAllRepeatingCameraCaptureCallbacks(
                    childConfig.getRepeatingCameraCaptureCallbacks());
            builder.addAllCameraCaptureCallbacks(childConfig.getSingleCameraCaptureCallbacks());
            builder.addAllSessionStateCallbacks(childConfig.getSessionStateCallbacks());
            builder.addAllDeviceStateCallbacks(childConfig.getDeviceStateCallbacks());
            builder.addImplementationOptions(childConfig.getImplementationOptions());
        }
    }

    /**
     * Creates the input {@link SurfaceEdge} for {@link #mSharingNode}.
     */
    @NonNull
    private SurfaceEdge getSharingInputEdge(@NonNull SurfaceEdge cameraEdge,
            @NonNull CameraInternal camera) {
        if (getEffect() == null) {
            // No effect. The input edge is the camera edge.
            return cameraEdge;
        }
        if (getEffect().getTransformation() == CameraEffect.TRANSFORMATION_PASSTHROUGH) {
            // This is a passthrough effect for testing.
            return cameraEdge;
        }
        if (getEffect().getOutputOption() == CameraEffect.OUTPUT_OPTION_ONE_FOR_EACH_TARGET) {
            // When OUTPUT_OPTION_ONE_FOR_EACH_TARGET is used, we will apply the effect at the
            // sharing stage.
            return cameraEdge;
        }
        // Transform the camera edge to get the input edge.
        mEffectNode = new SurfaceProcessorNode(camera,
                getEffect().createSurfaceProcessorInternal());
        int rotationAppliedByEffect = getRotationAppliedByEffect();
        Rect cropRectAppliedByEffect = getCropRectAppliedByEffect(cameraEdge);
        OutConfig outConfig = OutConfig.of(
                cameraEdge.getTargets(),
                cameraEdge.getFormat(),
                cropRectAppliedByEffect,
                getRotatedSize(cropRectAppliedByEffect, rotationAppliedByEffect),
                rotationAppliedByEffect,
                getMirroringAppliedByEffect(),
                /*shouldRespectInputCropRect=*/true);
        SurfaceProcessorNode.In in = SurfaceProcessorNode.In.of(cameraEdge,
                singletonList(outConfig));
        SurfaceProcessorNode.Out out = mEffectNode.transform(in);
        return requireNonNull(out.get(outConfig));
    }

    @NonNull
    private SurfaceProcessorNode getSharingNode(@NonNull CameraInternal camera,
            @NonNull StreamSpec streamSpec) {
        if (getEffect() != null
                && getEffect().getOutputOption()
                == CameraEffect.OUTPUT_OPTION_ONE_FOR_EACH_TARGET) {
            // The effect wants to handle the sharing itself. Use the effect's node for sharing.
            mEffectNode = new SurfaceProcessorNode(camera,
                    getEffect().createSurfaceProcessorInternal());
            return mEffectNode;
        } else {
            // Create an internal node for sharing.
            return new SurfaceProcessorNode(camera,
                    DefaultSurfaceProcessor.Factory.newInstance(streamSpec.getDynamicRange()));
        }
    }

    @NonNull
    private DualSurfaceProcessorNode getDualSharingNode(
            @NonNull CameraInternal primaryCamera,
            @NonNull CameraInternal secondaryCamera,
            @NonNull StreamSpec streamSpec,
            @NonNull CompositionSettings primaryCompositionSettings,
            @NonNull CompositionSettings secondaryCompositionSettings) {
        // TODO: handle EffectNode for dual camera case
        return new DualSurfaceProcessorNode(primaryCamera, secondaryCamera,
                DualSurfaceProcessor.Factory.newInstance(
                        streamSpec.getDynamicRange(),
                        primaryCompositionSettings,
                        secondaryCompositionSettings));
    }

    private int getRotationAppliedByEffect() {
        CameraEffect effect = checkNotNull(getEffect());
        if (effect.getTransformation() == CameraEffect.TRANSFORMATION_CAMERA_AND_SURFACE_ROTATION) {
            // Apply the rotation degrees if the effect is configured to do so.
            // TODO: handle this option in VideoCapture.
            return getRelativeRotation(checkNotNull(getCamera()));
        } else {
            // By default, the effect node does not apply any rotation.
            return 0;
        }
    }

    private boolean getMirroringAppliedByEffect() {
        CameraEffect effect = checkNotNull(getEffect());
        if (effect.getTransformation() == CameraEffect.TRANSFORMATION_CAMERA_AND_SURFACE_ROTATION) {
            // TODO: handle this option in VideoCapture.
            // For a Surface that connects to the front camera directly, the texture
            // transformation contains mirroring bit which will be applied by libraries using the
            // TRANSFORMATION_CAMERA_AND_SURFACE_ROTATION option.
            CameraInternal camera = checkNotNull(getCamera());
            return camera.isFrontFacing() && camera.getHasTransform();
        } else {
            // By default, the effect node does not apply any mirroring.
            return false;
        }
    }

    private Rect getCropRectAppliedByEffect(SurfaceEdge cameraEdge) {
        CameraEffect effect = checkNotNull(getEffect());
        if (effect.getTransformation() == CameraEffect.TRANSFORMATION_CAMERA_AND_SURFACE_ROTATION) {
            // TODO: handle this option in VideoCapture.
            // Do not apply the crop rect if the effect is configured to do so.
            Size parentSize = cameraEdge.getStreamSpec().getResolution();
            return sizeToRect(parentSize);
        } else {
            // By default, the effect node does not apply any crop rect.
            return cameraEdge.getCropRect();
        }
    }

    private void addCameraErrorListener(
            @NonNull SessionConfig.Builder sessionConfigBuilder,
            @NonNull String cameraId,
            @Nullable String secondaryCameraId,
            @NonNull UseCaseConfig<?> config,
            @NonNull StreamSpec primaryStreamSpec,
            @Nullable StreamSpec secondaryStreamSpec) {
        if (mCloseableErrorListener != null) {
            mCloseableErrorListener.close();
        }
        mCloseableErrorListener = new SessionConfig.CloseableErrorListener(
                (sessionConfig, error) -> {
                    // Do nothing when the use case has been unbound.
                    if (getCamera() == null) {
                        return;
                    }

                    // Clear both StreamSharing and the children.
                    clearPipeline();
                    updateSessionConfig(
                            createPipelineAndUpdateChildrenSpecs(cameraId, secondaryCameraId,
                                    config, primaryStreamSpec, secondaryStreamSpec));
                    notifyReset();
                    // Connect the latest {@link Surface} to newly created children edges.
                    // Currently children UseCase does not have additional logic in SessionConfig
                    // error listener so this is OK. If they do, we need to invoke the children's
                    // SessionConfig error listeners instead.
                    mVirtualCameraAdapter.resetChildren();
                });
        sessionConfigBuilder.setErrorListener(mCloseableErrorListener);
    }

    private void clearPipeline() {
        // Closes the old error listener
        if (mCloseableErrorListener != null) {
            mCloseableErrorListener.close();
            mCloseableErrorListener = null;
        }

        if (mCameraEdge != null) {
            mCameraEdge.close();
            mCameraEdge = null;
        }
        if (mSecondaryCameraEdge != null) {
            mSecondaryCameraEdge.close();
            mSecondaryCameraEdge = null;
        }
        if (mSharingInputEdge != null) {
            mSharingInputEdge.close();
            mSharingInputEdge = null;
        }
        if (mSecondarySharingInputEdge != null) {
            mSecondarySharingInputEdge.close();
            mSecondarySharingInputEdge = null;
        }
        if (mSharingNode != null) {
            mSharingNode.release();
            mSharingNode = null;
        }
        if (mDualSharingNode != null) {
            mDualSharingNode.release();
            mDualSharingNode = null;
        }
        if (mEffectNode != null) {
            mEffectNode.release();
            mEffectNode = null;
        }
    }

    @Nullable
    private Rect getCropRect(@NonNull Size surfaceResolution) {
        if (getViewPortCropRect() != null) {
            return getViewPortCropRect();
        }
        return new Rect(0, 0, surfaceResolution.getWidth(), surfaceResolution.getHeight());
    }

    /**
     * Interface for controlling the {@link StreamSharing}.
     */
    interface Control {

        /**
         * Takes a snapshot of the current stream and write it to the children with JPEG Surface.
         */
        @NonNull
        ListenableFuture<Void> jpegSnapshot(
                @IntRange(from = 0, to = 100) int jpegQuality,
                @IntRange(from = 0, to = 359) int rotationDegrees);
    }

    @VisibleForTesting
    @Nullable
    SurfaceEdge getCameraEdge() {
        return mCameraEdge;
    }

    @VisibleForTesting
    @Nullable
    SurfaceProcessorNode getSharingNode() {
        return mSharingNode;
    }

    @VisibleForTesting
    @NonNull
    VirtualCameraAdapter getVirtualCameraAdapter() {
        return mVirtualCameraAdapter;
    }

    /**
     * Gets the capture types of all the children use cases when use case is StreamSharing, or just
     * the capture type of the use case itself otherwise.
     */
    @NonNull
    public static List<UseCaseConfigFactory.CaptureType> getCaptureTypes(@NonNull UseCase useCase) {
        List<UseCaseConfigFactory.CaptureType> result = new ArrayList<>();
        if (isStreamSharing(useCase)) {
            for (UseCase child : ((StreamSharing) useCase).getChildren()) {
                result.add(child.getCurrentConfig().getCaptureType());
            }
        } else {
            result.add(useCase.getCurrentConfig().getCaptureType());
        }
        return result;
    }

    /**
     * Checks if the provided use case is a StreamSharing use case.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static boolean isStreamSharing(@Nullable UseCase useCase) {
        return useCase instanceof StreamSharing;
    }

    @VisibleForTesting
    @Nullable
    public SurfaceEdge getSharingInputEdge() {
        return mSharingInputEdge;
    }
}
