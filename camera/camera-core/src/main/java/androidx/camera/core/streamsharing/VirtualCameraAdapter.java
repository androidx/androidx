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
import static androidx.camera.core.impl.ImageInputConfig.OPTION_INPUT_DYNAMIC_RANGE;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_CUSTOM_ORDERED_RESOLUTIONS;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_PREVIEW_STABILIZATION_MODE;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_SURFACE_OCCUPANCY_PRIORITY;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_TARGET_FRAME_RATE;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_VIDEO_STABILIZATION_MODE;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.camera.core.impl.utils.TransformUtils.getRotationDegrees;
import static androidx.camera.core.impl.utils.TransformUtils.isMirrored;
import static androidx.camera.core.impl.utils.TransformUtils.rotateSize;
import static androidx.camera.core.impl.utils.TransformUtils.within360;
import static androidx.camera.core.streamsharing.DynamicRangeUtils.resolveDynamicRange;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.IntRange;
import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.stabilization.StabilizationMode;
import androidx.camera.core.processing.SurfaceEdge;
import androidx.camera.core.processing.concurrent.DualOutConfig;
import androidx.camera.core.processing.util.OutConfig;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
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
class VirtualCameraAdapter implements UseCase.StateChangeCallback {

    private static final String TAG = "VirtualCameraAdapter";
    // Children UseCases associated with this virtual camera.
    final @NonNull Set<UseCase> mChildren;
    // Specs for children UseCase, calculated and set by StreamSharing.
    final @NonNull Map<UseCase, SurfaceEdge> mChildrenEdges = new HashMap<>();
    private final @NonNull Map<UseCase, VirtualCamera> mChildrenVirtualCameras = new HashMap<>();
    // Whether a children is in the active state. See: UseCase.State.ACTIVE
    final @NonNull Map<UseCase, Boolean> mChildrenActiveState = new HashMap<>();
    // Config factory for getting children's config.
    private final @NonNull UseCaseConfigFactory mUseCaseConfigFactory;
    // The parent camera instance.
    private final @NonNull CameraInternal mParentCamera;
    // The parent secondary camera instance in dual camera case.
    private final @Nullable CameraInternal mSecondaryParentCamera;
    // The callback that receives the parent camera's metadata.
    private final @NonNull CameraCaptureCallback mParentMetadataCallback =
            createCameraCaptureCallback();
    private final @NonNull Set<UseCaseConfig<?>> mChildrenConfigs;
    private final @NonNull Map<UseCase, UseCaseConfig<?>> mChildrenConfigsMap;
    private final @NonNull ResolutionsMerger mResolutionsMerger;
    private @Nullable ResolutionsMerger mSecondaryResolutionsMerger;

    /**
     * @param parentCamera         the parent {@link CameraInternal} instance. For example, the
     *                             real camera.
     * @param children             the children {@link UseCase}.
     * @param useCaseConfigFactory the factory for configuring children {@link UseCase}.
     */
    VirtualCameraAdapter(@NonNull CameraInternal parentCamera,
            @Nullable CameraInternal secondaryParentCamera,
            @NonNull Set<UseCase> children,
            @NonNull UseCaseConfigFactory useCaseConfigFactory,
            StreamSharing.@NonNull Control streamSharingControl) {
        mParentCamera = parentCamera;
        mSecondaryParentCamera = secondaryParentCamera;
        mUseCaseConfigFactory = useCaseConfigFactory;
        mChildren = children;
        // No need to create a new instance for secondary camera.
        mChildrenConfigsMap = toChildrenConfigsMap(parentCamera, children, useCaseConfigFactory);
        mChildrenConfigs = new HashSet<>(mChildrenConfigsMap.values());
        mResolutionsMerger = new ResolutionsMerger(parentCamera, mChildrenConfigs);
        if (mSecondaryParentCamera != null) {
            mSecondaryResolutionsMerger = new ResolutionsMerger(
                    mSecondaryParentCamera, mChildrenConfigs);
        }
        // Set children state to inactive by default.
        for (UseCase child : children) {
            mChildrenActiveState.put(child, false);
            // No need to create a new instance for secondary camera.
            mChildrenVirtualCameras.put(child, new VirtualCamera(
                    parentCamera,
                    this,
                    streamSharingControl));
        }
    }

    // --- API for StreamSharing ---
    void mergeChildrenConfigs(@NonNull MutableConfig mutableConfig) {
        // Merge resolution configs.
        List<Size> mergedResolutions = mResolutionsMerger.getMergedResolutions(mutableConfig);
        mutableConfig.insertOption(OPTION_CUSTOM_ORDERED_RESOLUTIONS, mergedResolutions);

        // Merge Surface occupancy priority.
        mutableConfig.insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY,
                getHighestSurfacePriority(mChildrenConfigs));

        // Merge dynamic range configs. Try to find a dynamic range that can match all child
        // requirements, or throw an exception if no matching dynamic range.
        //  TODO: This approach works for the current code base, where only VideoCapture can be
        //   configured (Preview follows the settings, ImageCapture is fixed as SDR). When
        //   dynamic range APIs opened on other use cases, we might want a more advanced approach
        //   that allows conflicts, e.g. converting HDR stream to SDR stream.
        DynamicRange dynamicRange = resolveDynamicRange(mChildrenConfigs);
        if (dynamicRange == null) {
            throw new IllegalArgumentException("Failed to merge child dynamic ranges, can not find"
                    + " a dynamic range that satisfies all children.");
        }
        mutableConfig.insertOption(OPTION_INPUT_DYNAMIC_RANGE, dynamicRange);

        mutableConfig.insertOption(OPTION_TARGET_FRAME_RATE,
                resolveTargetFrameRate(mChildrenConfigs));

        // Merge Preview stabilization and video stabilization configs.
        for (UseCase useCase : mChildren) {
            if (useCase.getCurrentConfig().getVideoStabilizationMode()
                    != StabilizationMode.UNSPECIFIED) {
                mutableConfig.insertOption(OPTION_VIDEO_STABILIZATION_MODE,
                        useCase.getCurrentConfig().getVideoStabilizationMode());
            }

            if (useCase.getCurrentConfig().getPreviewStabilizationMode()
                    != StabilizationMode.UNSPECIFIED) {
                mutableConfig.insertOption(OPTION_PREVIEW_STABILIZATION_MODE,
                        useCase.getCurrentConfig().getPreviewStabilizationMode());
            }
        }
    }

    void bindChildren() {
        for (UseCase useCase : mChildren) {
            useCase.bindToCamera(
                    requireNonNull(mChildrenVirtualCameras.get(useCase)),
                    null,
                    null,
                    useCase.getDefaultConfig(true, mUseCaseConfigFactory));
        }
    }

    void unbindChildren() {
        for (UseCase useCase : mChildren) {
            useCase.unbindFromCamera(requireNonNull(mChildrenVirtualCameras.get(useCase)));
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

    void notifyCameraControlReady() {
        for (UseCase useCase : mChildren) {
            useCase.onCameraControlReady();
        }
    }

    @NonNull Set<UseCase> getChildren() {
        return mChildren;
    }

    /**
     * Gets {@link OutConfig} for children {@link UseCase} based on the input edge.
     */
    @NonNull Map<UseCase, OutConfig> getChildrenOutConfigs(
            @NonNull SurfaceEdge sharingInputEdge,
            @ImageOutputConfig.RotationValue int parentTargetRotation, boolean isViewportSet) {
        Map<UseCase, OutConfig> outConfigs = new HashMap<>();
        for (UseCase useCase : mChildren) {
            OutConfig outConfig = calculateOutConfig(useCase, mResolutionsMerger,
                    mParentCamera, sharingInputEdge, parentTargetRotation, isViewportSet);
            outConfigs.put(useCase, outConfig);
        }
        return outConfigs;
    }

    /**
     * Gets original selected size for children {@link UseCase} based on the input edge.
     */
    @NonNull Map<UseCase, Size> getSelectedChildSizes(@NonNull SurfaceEdge sharingInputEdge,
            boolean isViewportSet) {
        Map<UseCase, Size> selectedChildSizes = new HashMap<>();
        for (UseCase useCase : mChildren) {
            PreferredChildSize preferredChildSize = mResolutionsMerger
                    .getPreferredChildSize(
                            requireNonNull(mChildrenConfigsMap.get(useCase)),
                            sharingInputEdge.getCropRect(),
                            getRotationDegrees(sharingInputEdge.getSensorToBufferTransform()),
                            isViewportSet);
            selectedChildSizes.put(useCase, preferredChildSize.getOriginalSelectedChildSize());
            Logger.d(TAG,
                    "Selected child size: " + preferredChildSize.getOriginalSelectedChildSize()
                            + ", useCase: " + useCase);
        }
        return selectedChildSizes;
    }

    @NonNull Map<UseCase, DualOutConfig> getChildrenOutConfigs(
            @NonNull SurfaceEdge primaryInputEdge,
            @NonNull SurfaceEdge secondaryInputEdge,
            @ImageOutputConfig.RotationValue int parentTargetRotation,
            boolean isViewportSet) {
        Map<UseCase, DualOutConfig> outConfigs = new HashMap<>();
        for (UseCase useCase : mChildren) {
            // primary
            OutConfig primaryOutConfig = calculateOutConfig(
                    useCase, mResolutionsMerger,
                    mParentCamera, primaryInputEdge,
                    parentTargetRotation, isViewportSet);
            // secondary
            OutConfig secondaryOutConfig = calculateOutConfig(
                    useCase, requireNonNull(mSecondaryResolutionsMerger),
                    requireNonNull(mSecondaryParentCamera),
                    secondaryInputEdge,
                    parentTargetRotation, isViewportSet);
            outConfigs.put(useCase, DualOutConfig.of(
                    primaryOutConfig, secondaryOutConfig));
        }
        return outConfigs;
    }

    private @NonNull OutConfig calculateOutConfig(
            @NonNull UseCase useCase,
            @NonNull ResolutionsMerger resolutionsMerger,
            @NonNull CameraInternal cameraInternal,
            @Nullable SurfaceEdge cameraInputEdge,
            @ImageOutputConfig.RotationValue int parentTargetRotation,
            boolean isViewportSet) {
        // TODO: we might be able to extract parent rotation degrees from the input edge's
        //  sensor-to-buffer matrix and the mirroring bit.
        int parentRotationDegrees = cameraInternal.getCameraInfo()
                .getSensorRotationDegrees(parentTargetRotation);
        boolean parentIsMirrored = isMirrored(
                cameraInputEdge.getSensorToBufferTransform());
        PreferredChildSize preferredChildSize = resolutionsMerger
                .getPreferredChildSize(
                        requireNonNull(mChildrenConfigsMap.get(useCase)),
                        cameraInputEdge.getCropRect(),
                        getRotationDegrees(cameraInputEdge.getSensorToBufferTransform()),
                        isViewportSet);
        Rect cropRectBeforeScaling = preferredChildSize.getCropRectBeforeScaling();
        Size childSizeToScale = preferredChildSize.getChildSizeToScale();

        // Only use primary camera info for output surface
        int childRotationDegrees = getChildRotationDegrees(useCase, mParentCamera);
        requireNonNull(mChildrenVirtualCameras.get(useCase))
                .setRotationDegrees(childRotationDegrees);
        int childParentDelta = within360(cameraInputEdge.getRotationDegrees()
                + childRotationDegrees - parentRotationDegrees);
        return OutConfig.of(
                getChildTargetType(useCase),
                getChildFormat(useCase),
                cropRectBeforeScaling,
                rotateSize(childSizeToScale, childParentDelta),
                childParentDelta,
                // Only mirror if the parent and the child disagrees.
                useCase.isMirroringRequired(cameraInternal)
                        ^ parentIsMirrored);
    }

    /**
     * Update children {@link SurfaceEdge} calculated by {@link StreamSharing}.
     */
    void setChildrenEdges(@NonNull Map<UseCase, SurfaceEdge> childrenEdges,
            @NonNull Map<UseCase, @NonNull Size> selectedChildSizes) {
        mChildrenEdges.clear();
        mChildrenEdges.putAll(childrenEdges);
        for (Map.Entry<UseCase, SurfaceEdge> entry : mChildrenEdges.entrySet()) {
            UseCase useCase = entry.getKey();
            SurfaceEdge surfaceEdge = entry.getValue();
            useCase.setViewPortCropRect(surfaceEdge.getCropRect());
            useCase.setSensorToBufferTransformMatrix(surfaceEdge.getSensorToBufferTransform());
            StreamSpec streamSpec = getChildStreamSpec(useCase, surfaceEdge.getStreamSpec(),
                    selectedChildSizes);
            useCase.updateSuggestedStreamSpec(streamSpec, null);
            useCase.notifyState();
        }
    }

    /**
     * Invokes {@link UseCase.StateChangeCallback#onUseCaseReset} for all children.
     */
    void resetChildren() {
        checkMainThread();
        for (UseCase useCase : mChildren) {
            onUseCaseReset(useCase);
        }
    }

    /**
     * Gets the callback for receiving parent camera's metadata.
     */
    @NonNull CameraCaptureCallback getParentMetadataCallback() {
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
        if (!isUseCaseActive(useCase)) {
            // No-op if the child is inactive. It will connect when it becomes active.
            return;
        }
        DeferrableSurface childSurface = getChildSurface(useCase);
        if (childSurface != null) {
            forceSetProvider(edge, childSurface, useCase.getSessionConfig());
        }
    }

    // --- private methods ---

    @IntRange(from = 0, to = 359)
    private int getChildRotationDegrees(@NonNull UseCase child,
            @NonNull CameraInternal cameraInternal) {
        int childTargetRotation = ((ImageOutputConfig) child.getCurrentConfig())
                .getTargetRotation(Surface.ROTATION_0);
        return cameraInternal.getCameraInfo().getSensorRotationDegrees(childTargetRotation);
    }

    @NonNull
    private static StreamSpec getChildStreamSpec(@NonNull UseCase useCase,
            @NonNull StreamSpec baseStreamSpec, @NonNull Map<UseCase, Size> selectedChildSizes) {
        StreamSpec.Builder builder = baseStreamSpec.toBuilder();
        Size selectedChildSize = selectedChildSizes.get(useCase);
        if (selectedChildSize != null) {
            builder.setOriginalConfiguredResolution(selectedChildSize);
        }
        return builder.build();
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

    private static @NonNull Map<UseCase, UseCaseConfig<?>> toChildrenConfigsMap(
            @NonNull CameraInternal parentCamera, @NonNull Set<UseCase> children,
            @NonNull UseCaseConfigFactory useCaseConfigFactory) {
        Map<UseCase, UseCaseConfig<?>> result = new HashMap<>();
        for (UseCase useCase : children) {
            UseCaseConfig<?> config = useCase.mergeConfigs(parentCamera.getCameraInfoInternal(),
                    null, useCase.getDefaultConfig(true, useCaseConfigFactory));
            result.put(useCase, config);
        }

        return result;
    }

    private static int getHighestSurfacePriority(Set<UseCaseConfig<?>> childrenConfigs) {
        int highestPriority = 0;
        for (UseCaseConfig<?> childConfig : childrenConfigs) {
            highestPriority = Math.max(highestPriority,
                    childConfig.getSurfaceOccupancyPriority(0));
        }
        return highestPriority;
    }

    private @NonNull SurfaceEdge getUseCaseEdge(@NonNull UseCase useCase) {
        return requireNonNull(mChildrenEdges.get(useCase));
    }

    private boolean isUseCaseActive(@NonNull UseCase useCase) {
        return requireNonNull(mChildrenActiveState.get(useCase));
    }

    private static void forceSetProvider(@NonNull SurfaceEdge edge,
            @NonNull DeferrableSurface childSurface,
            @NonNull SessionConfig childSessionConfig) {
        edge.invalidate();
        try {
            edge.setProvider(childSurface);
        } catch (DeferrableSurface.SurfaceClosedException e) {
            // The Surface is closed by the child. This will happen when e.g. the child is Preview
            // with SurfaceView implementation.
            // Invoke the error listener so it will recreate the pipeline.
            if (childSessionConfig.getErrorListener() != null) {
                childSessionConfig.getErrorListener().onError(childSessionConfig,
                        SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET);
            }
        }
    }

    /**
     * Gets the {@link DeferrableSurface} associated with the child.
     */
    @VisibleForTesting
    static @Nullable DeferrableSurface getChildSurface(@NonNull UseCase child) {
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
        // Use static class + WeakReference to avoid the reference being held in
        // CameraCaptureCallback. On some device, the cameraCaptureCallback could be held in
        // camera framework.
        return new VirtualCameraCaptureCallback(this);
    }

    static class VirtualCameraCaptureCallback extends CameraCaptureCallback {
        private final WeakReference<VirtualCameraAdapter> mVirtualCameraAdapterRef;
        VirtualCameraCaptureCallback(VirtualCameraAdapter virtualCameraAdapter) {
            mVirtualCameraAdapterRef = new WeakReference<>(virtualCameraAdapter);
        }
        @Override
        public void onCaptureCompleted(int captureConfigId,
                @NonNull CameraCaptureResult cameraCaptureResult) {
            VirtualCameraAdapter virtualCameraAdapter = mVirtualCameraAdapterRef.get();
            if (virtualCameraAdapter != null) {
                for (UseCase child : virtualCameraAdapter.mChildren) {
                    sendCameraCaptureResultToChild(cameraCaptureResult,
                            child.getSessionConfig(), captureConfigId);
                }
            }
        }
    }

    static void sendCameraCaptureResultToChild(
            @NonNull CameraCaptureResult cameraCaptureResult,
            @NonNull SessionConfig sessionConfig,
            int captureConfigId) {
        for (CameraCaptureCallback callback :
                sessionConfig.getRepeatingCameraCaptureCallbacks()) {
            callback.onCaptureCompleted(captureConfigId, new VirtualCameraCaptureResult(
                    sessionConfig.getRepeatingCaptureConfig().getTagBundle(),
                    cameraCaptureResult));
        }
    }

    /**
     * Resolves target frame rate from use case configs.
     *
     * <p>Tries to return a intersected frame rate range in priority. If it can't be found, return
     * the smallest range that includes both frame rate ranges.
     */
    private static @NonNull Range<Integer> resolveTargetFrameRate(
            @NonNull Set<UseCaseConfig<?>> useCaseConfigs) {
        Range<Integer> resolvedTargetFrameRate = StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED;

        for (UseCaseConfig<?> useCaseConfig : useCaseConfigs) {
            Range<Integer> targetFrameRate = requireNonNull(useCaseConfig.getTargetFrameRate(
                    resolvedTargetFrameRate));

            if (StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED.equals(resolvedTargetFrameRate)) {
                resolvedTargetFrameRate = targetFrameRate;
                continue;
            }

            try {
                resolvedTargetFrameRate = resolvedTargetFrameRate.intersect(targetFrameRate);
            } catch (IllegalArgumentException e) {
                Logger.d(TAG,
                        "No intersected frame rate can be found from the target frame rate "
                                + "settings of the UseCases! Resolved: " + resolvedTargetFrameRate
                                + " <<>> " + targetFrameRate);
                return resolvedTargetFrameRate.extend(targetFrameRate);
            }
        }

        return resolvedTargetFrameRate;
    }
}
