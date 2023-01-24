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
import static androidx.camera.core.impl.ImageInputConfig.OPTION_INPUT_FORMAT;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.core.util.Preconditions.checkNotNull;

import static java.util.Objects.requireNonNull;

import android.graphics.Rect;
import android.os.Build;
import android.util.Size;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.processing.DefaultSurfaceProcessor;
import androidx.camera.core.processing.SurfaceEdge;
import androidx.camera.core.processing.SurfaceProcessorNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A {@link UseCase} that shares one PRIV stream to multiple children {@link UseCase}s.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class StreamSharing extends UseCase {

    @NonNull
    private static final StreamSharingConfig DEFAULT_CONFIG;

    @NonNull
    @SuppressWarnings("UnusedVariable")
    private final VirtualCamera mVirtualCamera;

    @Nullable
    private SurfaceProcessorNode mNode;
    @Nullable
    private SurfaceEdge mCameraEdge;

    static {
        MutableConfig mutableConfig = new StreamSharingBuilder().getMutableConfig();
        mutableConfig.insertOption(OPTION_INPUT_FORMAT,
                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE);
        DEFAULT_CONFIG = new StreamSharingConfig(OptionsBundle.from(mutableConfig));
    }

    /**
     * Constructs a {@link StreamSharing} with a parent {@link CameraInternal}, children
     * {@link UseCase}s, and a {@link UseCaseConfigFactory} for getting default {@link UseCase}
     * configurations.
     */
    public StreamSharing(@NonNull CameraInternal parentCamera,
            @NonNull Set<UseCase> children,
            @NonNull UseCaseConfigFactory useCaseConfigFactory) {
        this(new VirtualCamera(parentCamera, children, useCaseConfigFactory));
    }

    StreamSharing(@NonNull VirtualCamera virtualCamera) {
        super(DEFAULT_CONFIG);
        mVirtualCamera = virtualCamera;
    }

    @Nullable
    @Override
    public UseCaseConfig<?> getDefaultConfig(boolean applyDefaultConfig,
            @NonNull UseCaseConfigFactory factory) {
        // The shared stream optimizes for VideoCapture.
        Config captureConfig = factory.getConfig(
                UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE,
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY);

        if (applyDefaultConfig) {
            captureConfig = Config.mergeConfigs(captureConfig, DEFAULT_CONFIG.getConfig());
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
                streamSpec,
                getSensorToBufferTransformMatrix(),
                getHasCameraTransform(),
                requireNonNull(getCropRect(streamSpec.getResolution())),
                getRelativeRotation(camera),
                /*mirroring=*/false); // Mirroring will be decided by each child.
        mNode = new SurfaceProcessorNode(camera, DefaultSurfaceProcessor.Factory.newInstance());

        // Transform the input based on virtual camera configuration.
        Map<UseCase, SurfaceProcessorNode.OutConfig> outConfigMap =
                mVirtualCamera.getChildrenOutConfigs(mCameraEdge);
        SurfaceProcessorNode.Out out = mNode.transform(SurfaceProcessorNode.In.of(mCameraEdge,
                new ArrayList<>(outConfigMap.values())));

        // Pass the output edges to virtual camera to connect children.
        Map<UseCase, SurfaceEdge> outputEdges = new HashMap<>();
        for (Map.Entry<UseCase, SurfaceProcessorNode.OutConfig> entry : outConfigMap.entrySet()) {
            outputEdges.put(entry.getKey(), out.get(entry.getValue()));
        }
        mVirtualCamera.setChildrenEdges(outputEdges);

        // Send the camera edge Surface to the camera2.
        SessionConfig.Builder builder = SessionConfig.Builder.createFrom(config);
        builder.addSurface(mCameraEdge.getDeferrableSurface());
        builder.addRepeatingCameraCaptureCallback(mVirtualCamera.getParentMetadataCallback());
        addCameraErrorListener(builder, cameraId, config, streamSpec);
        return builder.build();
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
            }
        });
    }

    private void clearPipeline() {
        if (mCameraEdge != null) {
            mCameraEdge.close();
            mCameraEdge = null;
        }
        if (mNode != null) {
            mNode.release();
            mNode = null;
        }
    }

    @Nullable
    private Rect getCropRect(@NonNull Size surfaceResolution) {
        if (getViewPortCropRect() != null) {
            return getViewPortCropRect();
        }
        return new Rect(0, 0, surfaceResolution.getWidth(), surfaceResolution.getHeight());
    }

    @VisibleForTesting
    @Nullable
    SurfaceEdge getCameraEdge() {
        return mCameraEdge;
    }

    @VisibleForTesting
    @Nullable
    SurfaceProcessorNode getNode() {
        return mNode;
    }
}
