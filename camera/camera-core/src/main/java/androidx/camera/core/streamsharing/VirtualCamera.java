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
import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_CUSTOM_ORDERED_RESOLUTIONS;
import static androidx.camera.core.impl.utils.TransformUtils.rectToSize;
import static androidx.camera.core.streamsharing.ResolutionUtils.getMergedResolutions;

import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.processing.SurfaceEdge;
import androidx.camera.core.processing.SurfaceProcessorNode.OutConfig;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A virtual implementation of {@link CameraInternal}.
 *
 * <p> This class manages children {@link UseCase} and connects/disconnects them to the
 * parent {@link StreamSharing}. It also forwards parent camera properties/events to the children.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class VirtualCamera implements CameraInternal {
    private static final String UNSUPPORTED_MESSAGE = "Operation not supported by VirtualCamera.";
    // Children UseCases associated with this virtual camera.
    @NonNull
    final Set<UseCase> mChildren;
    // Specs for children UseCase, calculated and set by StreamSharing.
    @NonNull
    final Map<UseCase, SurfaceEdge> mChildrenEdges = new HashMap<>();
    // Config factory for getting children's config.
    @NonNull
    private final UseCaseConfigFactory mUseCaseConfigFactory;
    // The parent camera instance.
    @NonNull
    private final CameraInternal mParentCamera;
    // The callback that receives the parent camera's metadata.
    @NonNull
    private final CameraCaptureCallback mParentMetadataCallback = createCameraCaptureCallback();

    /**
     * @param parentCamera         the parent {@link CameraInternal} instance. For example, the
     *                             real camera.
     * @param children             the children {@link UseCase}.
     * @param useCaseConfigFactory the factory for configuring children {@link UseCase}.
     */
    VirtualCamera(@NonNull CameraInternal parentCamera,
            @NonNull Set<UseCase> children,
            @NonNull UseCaseConfigFactory useCaseConfigFactory) {
        mParentCamera = parentCamera;
        mUseCaseConfigFactory = useCaseConfigFactory;
        mChildren = children;
    }

    // --- API for StreamSharing ---
    void mergeChildrenConfigs(@NonNull MutableConfig mutableConfig) {
        Set<UseCaseConfig<?>> childrenConfigs = new HashSet<>();
        for (UseCase useCase : mChildren) {
            childrenConfigs.add(useCase.mergeConfigs(mParentCamera.getCameraInfoInternal(),
                    null,
                    useCase.getDefaultConfig(true, mUseCaseConfigFactory)));
        }
        // Merge resolution configs.
        List<Size> supportedResolutions =
                new ArrayList<>(mParentCamera.getCameraInfoInternal().getSupportedResolutions(
                        INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE));
        Size sensorSize = rectToSize(mParentCamera.getCameraControlInternal().getSensorRect());
        mutableConfig.insertOption(OPTION_CUSTOM_ORDERED_RESOLUTIONS,
                getMergedResolutions(supportedResolutions, sensorSize,
                        childrenConfigs));
    }

    void bindChildren() {
        for (UseCase useCase : mChildren) {
            useCase.setHasCameraTransform(false);
            useCase.bindToCamera(this, null,
                    useCase.getDefaultConfig(true, mUseCaseConfigFactory));
        }
    }

    void unbindChildren() {
        for (UseCase useCase : mChildren) {
            useCase.unbindFromCamera(this);
        }
    }

    void notifyStateAttached() {
        for (UseCase useCase : mChildren) {
            useCase.onStateAttached();
        }
    }

    void notifyStateDetached() {
        for (UseCase useCase : mChildren) {
            useCase.onStateDetached();
        }
    }

    @NonNull
    Set<UseCase> getChildren() {
        return mChildren;
    }

    /**
     * Gets {@link OutConfig} for children {@link UseCase} based on the input edge.
     */
    @NonNull
    Map<UseCase, OutConfig> getChildrenOutConfigs(@NonNull SurfaceEdge cameraEdge) {
        Map<UseCase, OutConfig> outConfigs = new HashMap<>();
        for (UseCase useCase : mChildren) {
            // TODO(b/264936115): This is a temporary solution where children use the parent
            //  stream without changing it. Later we will update it to allow
            //  cropping/down-sampling to better match children UseCase config.
            int target = useCase instanceof Preview ? PREVIEW : VIDEO_CAPTURE;
            boolean mirroring = useCase instanceof Preview;
            outConfigs.put(useCase, OutConfig.of(
                    target,
                    cameraEdge.getCropRect(),
                    rectToSize(cameraEdge.getCropRect()),
                    mirroring));
        }
        return outConfigs;
    }

    /**
     * Update children {@link SurfaceEdge} calculated by {@link StreamSharing}.
     */
    void setChildrenEdges(@NonNull Map<UseCase, SurfaceEdge> childrenEdges) {
        mChildrenEdges.clear();
        mChildrenEdges.putAll(childrenEdges);
        for (Map.Entry<UseCase, SurfaceEdge> entry : mChildrenEdges.entrySet()) {
            UseCase useCase = entry.getKey();
            SurfaceEdge surfaceEdge = entry.getValue();
            useCase.setViewPortCropRect(surfaceEdge.getCropRect());
            useCase.updateSuggestedStreamSpec(surfaceEdge.getStreamSpec());
            useCase.notifyState();
        }
    }

    /**
     * Gets the callback for receiving parent camera's metadata.
     */
    @NonNull
    CameraCaptureCallback getParentMetadataCallback() {
        return mParentMetadataCallback;
    }

    // --- Handle children state change ---
    // TODO(b/264936250): Handle children state changes.
    @Override
    public void onUseCaseActive(@NonNull UseCase useCase) {
    }

    @Override
    public void onUseCaseInactive(@NonNull UseCase useCase) {
    }

    @Override
    public void onUseCaseUpdated(@NonNull UseCase useCase) {
    }

    @Override
    public void onUseCaseReset(@NonNull UseCase useCase) {
    }

    // --- Forward parent camera properties and events ---
    @NonNull
    @Override
    public CameraControlInternal getCameraControlInternal() {
        return mParentCamera.getCameraControlInternal();
    }

    @NonNull
    @Override
    public CameraInfoInternal getCameraInfoInternal() {
        // TODO(b/265818567): replace this with a virtual camera info that returns a updated sensor
        //  rotation degrees based on buffer transformation applied in StreamSharing.
        return mParentCamera.getCameraInfoInternal();
    }

    @NonNull
    @Override
    public Observable<State> getCameraState() {
        return mParentCamera.getCameraState();
    }

    // --- private methods ---
    @NonNull
    CameraCaptureCallback createCameraCaptureCallback() {
        return new CameraCaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureResult cameraCaptureResult) {
                super.onCaptureCompleted(cameraCaptureResult);
                for (UseCase child : mChildren) {
                    sendCameraCaptureResultToChild(cameraCaptureResult, child.getSessionConfig());
                }
            }
        };
    }

    static void sendCameraCaptureResultToChild(
            @NonNull CameraCaptureResult cameraCaptureResult,
            @NonNull SessionConfig sessionConfig) {
        for (CameraCaptureCallback callback : sessionConfig.getRepeatingCameraCaptureCallbacks()) {
            callback.onCaptureCompleted(new VirtualCameraCaptureResult(cameraCaptureResult,
                    sessionConfig.getRepeatingCaptureConfig().getTagBundle()));
        }
    }

    // --- Unused overrides ---
    @Override
    public void open() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> release() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public void attachUseCases(@NonNull Collection<UseCase> useCases) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public void detachUseCases(@NonNull Collection<UseCase> useCases) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }
}
