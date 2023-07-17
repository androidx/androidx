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
import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
import static androidx.camera.core.impl.ImageInputConfig.OPTION_INPUT_FORMAT;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_MIRROR_MODE;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_TYPE;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.camera.core.impl.utils.TransformUtils.getRotatedSize;
import static androidx.core.util.Preconditions.checkNotNull;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.util.Size;

import androidx.annotation.IntRange;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Config;
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
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class StreamSharing extends UseCase {
    private static final String TAG = "StreamSharing";
    @NonNull
    private final StreamSharingConfig mDefaultConfig;

    @NonNull
    private final VirtualCamera mVirtualCamera;
    // Node that applies effect to the input.
    @Nullable
    private SurfaceProcessorNode mEffectNode;
    // Node that shares a single stream to multiple UseCases.
    @Nullable
    private SurfaceProcessorNode mSharingNode;
    // The input edge that connects to the camera.
    @Nullable
    private SurfaceEdge mCameraEdge;
    // The input edge of the sharing node.
    @Nullable
    private SurfaceEdge mSharingInputEdge;

    @SuppressWarnings("WeakerAccess") // Synthetic access
    SessionConfig.Builder mSessionConfigBuilder;

    private static StreamSharingConfig getDefaultConfig(Set<UseCase> children) {
        MutableConfig mutableConfig = new StreamSharingBuilder().getMutableConfig();
        mutableConfig.insertOption(OPTION_INPUT_FORMAT,
                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE);
        mutableConfig.insertOption(OPTION_CAPTURE_TYPE,
                UseCaseConfigFactory.CaptureType.STREAM_SHARING);
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
    public StreamSharing(@NonNull CameraInternal parentCamera,
            @NonNull Set<UseCase> children,
            @NonNull UseCaseConfigFactory useCaseConfigFactory) {
        super(getDefaultConfig(children));
        mDefaultConfig = getDefaultConfig(children);
        mVirtualCamera = new VirtualCamera(parentCamera, children, useCaseConfigFactory,
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
        mVirtualCamera.mergeChildrenConfigs(builder.getMutableConfig());
        return builder.getUseCaseConfig();
    }

    @NonNull
    @Override
    protected StreamSpec onSuggestedStreamSpecUpdated(@NonNull StreamSpec streamSpec) {
        updateSessionConfig(createPipelineAndUpdateChildrenSpecs(
                getCameraId(), getCurrentConfig(), streamSpec));
        notifyActive();
        return streamSpec;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected StreamSpec onSuggestedStreamSpecImplementationOptionsUpdated(@NonNull Config config) {
        mSessionConfigBuilder.addImplementationOptions(config);
        updateSessionConfig(mSessionConfigBuilder.build());
        return getAttachedStreamSpec().toBuilder().setImplementationOptions(config).build();
    }

    @Override
    public void onBind() {
        super.onBind();
        mVirtualCamera.bindChildren();
    }

    @Override
    public void onUnbind() {
        super.onUnbind();
        clearPipeline();
        mVirtualCamera.unbindChildren();
    }

    @Override
    public void onStateAttached() {
        super.onStateAttached();
        mVirtualCamera.notifyStateAttached();
    }

    @Override
    public void onStateDetached() {
        super.onStateDetached();
        mVirtualCamera.notifyStateDetached();
    }

    @NonNull
    public Set<UseCase> getChildren() {
        return mVirtualCamera.getChildren();
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
    private SessionConfig createPipelineAndUpdateChildrenSpecs(
            @NonNull String cameraId,
            @NonNull UseCaseConfig<?> config,
            @NonNull StreamSpec streamSpec) {
        checkMainThread();
        CameraInternal camera = checkNotNull(getCamera());
        // Create input edge and the node.
        mCameraEdge = new SurfaceEdge(
                /*targets=*/PREVIEW | VIDEO_CAPTURE,
                INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                streamSpec,
                getSensorToBufferTransformMatrix(),
                camera.getHasTransform(),
                requireNonNull(getCropRect(streamSpec.getResolution())),
                getRelativeRotation(camera), // Rotation can be overridden by children.
                // Once copied, the target rotation will no longer be useful.
                ImageOutputConfig.ROTATION_NOT_SPECIFIED,
                isMirroringRequired(camera)); // Mirroring can be overridden by children.
        mSharingInputEdge = getSharingInputEdge(mCameraEdge, camera);

        mSharingNode = new SurfaceProcessorNode(camera,
                DefaultSurfaceProcessor.Factory.newInstance(streamSpec.getDynamicRange()));

        // Transform the input based on virtual camera configuration.
        Map<UseCase, SurfaceProcessorNode.OutConfig> outConfigMap =
                mVirtualCamera.getChildrenOutConfigs(mSharingInputEdge);
        SurfaceProcessorNode.Out out = mSharingNode.transform(
                SurfaceProcessorNode.In.of(mSharingInputEdge,
                        new ArrayList<>(outConfigMap.values())));

        // Pass the output edges to virtual camera to connect children.
        Map<UseCase, SurfaceEdge> outputEdges = new HashMap<>();
        for (Map.Entry<UseCase, SurfaceProcessorNode.OutConfig> entry : outConfigMap.entrySet()) {
            outputEdges.put(entry.getKey(), out.get(entry.getValue()));
        }
        mVirtualCamera.setChildrenEdges(outputEdges);

        // Send the camera edge Surface to the camera2.
        SessionConfig.Builder builder = SessionConfig.Builder.createFrom(config,
                streamSpec.getResolution());
        builder.addSurface(mCameraEdge.getDeferrableSurface());
        builder.addRepeatingCameraCaptureCallback(mVirtualCamera.getParentMetadataCallback());
        if (streamSpec.getImplementationOptions() != null) {
            builder.addImplementationOptions(streamSpec.getImplementationOptions());
        }
        addCameraErrorListener(builder, cameraId, config, streamSpec);
        mSessionConfigBuilder = builder;
        return builder.build();
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
        // Transform the camera edge to get the input edge.
        mEffectNode = new SurfaceProcessorNode(camera,
                getEffect().createSurfaceProcessorInternal());
        // Effect does not apply rotation.
        int rotationAppliedByEffect = 0;
        SurfaceProcessorNode.OutConfig outConfig = SurfaceProcessorNode.OutConfig.of(
                cameraEdge.getTargets(),
                cameraEdge.getFormat(),
                cameraEdge.getCropRect(),
                getRotatedSize(cameraEdge.getCropRect(), rotationAppliedByEffect),
                rotationAppliedByEffect,
                /*mirroring=*/false); // Effects does not mirror.
        SurfaceProcessorNode.In in = SurfaceProcessorNode.In.of(cameraEdge,
                singletonList(outConfig));
        SurfaceProcessorNode.Out out = mEffectNode.transform(in);
        return requireNonNull(out.get(outConfig));
    }

    private void addCameraErrorListener(
            @NonNull SessionConfig.Builder sessionConfigBuilder,
            @NonNull String cameraId,
            @NonNull UseCaseConfig<?> config,
            @NonNull StreamSpec streamSpec) {
        sessionConfigBuilder.addErrorListener((sessionConfig, error) -> {
            // Clear both StreamSharing and the children.
            clearPipeline();
            if (isCurrentCamera(cameraId)) {
                // Only reset the pipeline when the bound camera is the same.
                updateSessionConfig(
                        createPipelineAndUpdateChildrenSpecs(cameraId, config, streamSpec));
                notifyReset();
                // Connect the latest {@link Surface} to newly created children edges. Currently
                // children UseCase does not have additional logic in SessionConfig error listener
                // so this is OK. If they do, we need to invoke the children's SessionConfig
                // error listeners instead.
                mVirtualCamera.resetChildren();
            }
        });
    }

    private void clearPipeline() {
        if (mCameraEdge != null) {
            mCameraEdge.close();
            mCameraEdge = null;
        }
        if (mSharingInputEdge != null) {
            mSharingInputEdge.close();
            mSharingInputEdge = null;
        }
        if (mSharingNode != null) {
            mSharingNode.release();
            mSharingNode = null;
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
    VirtualCamera getVirtualCamera() {
        return mVirtualCamera;
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
}
