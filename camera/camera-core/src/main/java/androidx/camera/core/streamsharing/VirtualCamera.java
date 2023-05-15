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

import static androidx.camera.core.CameraEffect.IMAGE_CAPTURE;
import static androidx.camera.core.CameraEffect.PREVIEW;
import static androidx.camera.core.CameraEffect.VIDEO_CAPTURE;
import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_CUSTOM_ORDERED_RESOLUTIONS;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_SURFACE_OCCUPANCY_PRIORITY;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.camera.core.impl.utils.TransformUtils.getRotatedSize;
import static androidx.camera.core.impl.utils.TransformUtils.rectToSize;
import static androidx.camera.core.streamsharing.ResolutionUtils.getMergedResolutions;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.graphics.ImageFormat;
import android.os.Build;
import android.util.Size;

import androidx.annotation.IntRange;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.DeferrableSurface;
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
    // Whether a children is in the active state. See: UseCase.State.ACTIVE
    @NonNull
    final Map<UseCase, Boolean> mChildrenActiveState = new HashMap<>();
    // Config factory for getting children's config.
    @NonNull
    private final UseCaseConfigFactory mUseCaseConfigFactory;
    // The parent camera instance.
    @NonNull
    private final CameraInternal mParentCamera;
    // The callback that receives the parent camera's metadata.
    @NonNull
    private final CameraCaptureCallback mParentMetadataCallback = createCameraCaptureCallback();
    @NonNull
    private final VirtualCameraControl mVirtualCameraControl;

    /**
     * @param parentCamera         the parent {@link CameraInternal} instance. For example, the
     *                             real camera.
     * @param children             the children {@link UseCase}.
     * @param useCaseConfigFactory the factory for configuring children {@link UseCase}.
     */
    VirtualCamera(@NonNull CameraInternal parentCamera,
            @NonNull Set<UseCase> children,
            @NonNull UseCaseConfigFactory useCaseConfigFactory,
            @NonNull StreamSharing.Control streamSharingControl) {
        mParentCamera = parentCamera;
        mUseCaseConfigFactory = useCaseConfigFactory;
        mChildren = children;
        mVirtualCameraControl = new VirtualCameraControl(parentCamera.getCameraControlInternal(),
                streamSharingControl);
        // Set children state to inactive by default.
        for (UseCase child : children) {
            mChildrenActiveState.put(child, false);
        }
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

        // Merge Surface occupancy priority.
        mutableConfig.insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY,
                getHighestSurfacePriority(childrenConfigs));
    }

    void bindChildren() {
        for (UseCase useCase : mChildren) {
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
            int rotationDegrees = getChildRotationDegrees(useCase);
            outConfigs.put(useCase, OutConfig.of(
                    getChildTargetType(useCase),
                    getChildFormat(useCase),
                    cameraEdge.getCropRect(),
                    getRotatedSize(cameraEdge.getCropRect(), rotationDegrees),
                    rotationDegrees,
                    useCase.isMirroringRequired(this)));
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
    @MainThread
    @Override
    public void onUseCaseActive(@NonNull UseCase useCase) {
        checkMainThread();
        if (isUseCaseActive(useCase)) {
            return;
        }
        mChildrenActiveState.put(useCase, true);
        DeferrableSurface childSurface = getChildSurface(useCase);
        if (childSurface != null) {
            forceSetProvider(getUseCaseEdge(useCase), childSurface, useCase.getSessionConfig());
        }
    }

    @MainThread
    @Override
    public void onUseCaseInactive(@NonNull UseCase useCase) {
        checkMainThread();
        if (!isUseCaseActive(useCase)) {
            return;
        }
        mChildrenActiveState.put(useCase, false);
        getUseCaseEdge(useCase).disconnect();
    }

    @MainThread
    @Override
    public void onUseCaseUpdated(@NonNull UseCase useCase) {
        checkMainThread();
        if (!isUseCaseActive(useCase)) {
            // No-op if the child is inactive. It will connect when it becomes active.
            return;
        }
        SurfaceEdge edge = getUseCaseEdge(useCase);
        DeferrableSurface childSurface = getChildSurface(useCase);
        if (childSurface != null) {
            // If the child has a Surface, connect. VideoCapture uses this mechanism to
            // resume/start recording.
            forceSetProvider(edge, childSurface, useCase.getSessionConfig());
        } else {
            // If the child has no Surface, disconnect. VideoCapture uses this mechanism to
            // pause/stop recording.
            edge.disconnect();
        }
    }

    @MainThread
    @Override
    public void onUseCaseReset(@NonNull UseCase useCase) {
        checkMainThread();
        SurfaceEdge edge = getUseCaseEdge(useCase);
        edge.invalidate();
        if (!isUseCaseActive(useCase)) {
            // No-op if the child is inactive. It will connect when it becomes active.
            return;
        }
        DeferrableSurface childSurface = getChildSurface(useCase);
        if (childSurface != null) {
            forceSetProvider(edge, childSurface, useCase.getSessionConfig());
        }
    }

    // --- Forward parent camera properties and events ---

    @Override
    public boolean getHasTransform() {
        return false;
    }

    @NonNull
    @Override
    public CameraControlInternal getCameraControlInternal() {
        return mVirtualCameraControl;
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

    @IntRange(from = 0, to = 359)
    private int getChildRotationDegrees(@NonNull UseCase child) {
        if (child instanceof Preview) {
            // Rotate the buffer for Preview because SurfaceView cannot handle rotation.
            return mParentCamera.getCameraInfo().getSensorRotationDegrees(
                    ((Preview) child).getTargetRotation());
        }
        // By default, sharing node does not rotate
        return 0;
    }

    private static int getChildFormat(@NonNull UseCase useCase) {
        return useCase instanceof ImageCapture ? ImageFormat.JPEG
                : INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
    }

    @CameraEffect.Targets
    private static int getChildTargetType(@NonNull UseCase useCase) {
        if (useCase instanceof Preview) {
            return PREVIEW;
        } else if (useCase instanceof ImageCapture) {
            return IMAGE_CAPTURE;
        } else {
            return VIDEO_CAPTURE;
        }
    }

    private static int getHighestSurfacePriority(Set<UseCaseConfig<?>> childrenConfigs) {
        int highestPriority = 0;
        for (UseCaseConfig<?> childConfig : childrenConfigs) {
            highestPriority = Math.max(highestPriority,
                    childConfig.getSurfaceOccupancyPriority());
        }
        return highestPriority;
    }

    @NonNull
    private SurfaceEdge getUseCaseEdge(@NonNull UseCase useCase) {
        return requireNonNull(mChildrenEdges.get(useCase));
    }

    private boolean isUseCaseActive(@NonNull UseCase useCase) {
        return requireNonNull(mChildrenActiveState.get(useCase));
    }

    private void forceSetProvider(@NonNull SurfaceEdge edge,
            @NonNull DeferrableSurface childSurface,
            @NonNull SessionConfig childSessionConfig) {
        edge.invalidate();
        try {
            edge.setProvider(childSurface);
        } catch (DeferrableSurface.SurfaceClosedException e) {
            // The Surface is closed by the child. This will happen when e.g. the child is Preview
            // with SurfaceView implementation.
            // Invoke the error listener so it will recreate the pipeline.
            for (SessionConfig.ErrorListener listener : childSessionConfig.getErrorListeners()) {
                listener.onError(childSessionConfig,
                        SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET);
            }
        }
    }

    /**
     * Gets the {@link DeferrableSurface} associated with the child.
     */
    @VisibleForTesting
    @Nullable
    static DeferrableSurface getChildSurface(@NonNull UseCase child) {
        // Get repeating Surface for preview & video, regular Surface for image capture.
        List<DeferrableSurface> surfaces = child instanceof ImageCapture
                ? child.getSessionConfig().getSurfaces() :
                child.getSessionConfig().getRepeatingCaptureConfig().getSurfaces();
        checkState(surfaces.size() <= 1);
        if (surfaces.size() == 1) {
            return surfaces.get(0);
        }
        return null;
    }

    CameraCaptureCallback createCameraCaptureCallback() {
        return new CameraCaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureResult cameraCaptureResult) {
                super.onCaptureCompleted(cameraCaptureResult);
                for (UseCase child : mChildren) {
                    sendCameraCaptureResultToChild(cameraCaptureResult,
                            child.getSessionConfig());
                }
            }
        };
    }

    static void sendCameraCaptureResultToChild(
            @NonNull CameraCaptureResult cameraCaptureResult,
            @NonNull SessionConfig sessionConfig) {
        for (CameraCaptureCallback callback :
                sessionConfig.getRepeatingCameraCaptureCallbacks()) {
            callback.onCaptureCompleted(new VirtualCameraCaptureResult(
                    sessionConfig.getRepeatingCaptureConfig().getTagBundle(),
                    cameraCaptureResult));
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
