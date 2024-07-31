/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.internal;

import static androidx.camera.core.CameraEffect.IMAGE_CAPTURE;
import static androidx.camera.core.CameraEffect.PREVIEW;
import static androidx.camera.core.CameraEffect.VIDEO_CAPTURE;
import static androidx.camera.core.DynamicRange.BIT_DEPTH_10_BIT;
import static androidx.camera.core.DynamicRange.ENCODING_SDR;
import static androidx.camera.core.DynamicRange.ENCODING_UNSPECIFIED;
import static androidx.camera.core.ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_OUTPUT_FORMAT;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_TYPE;
import static androidx.camera.core.impl.utils.TransformUtils.rectToSize;
import static androidx.camera.core.processing.TargetUtils.getNumberOfTargets;
import static androidx.camera.core.streamsharing.StreamSharing.getCaptureTypes;
import static androidx.camera.core.streamsharing.StreamSharing.isStreamSharing;
import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CompositionSettings;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.ViewPort;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.impl.AttachedSurfaceInfo;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraConfigs;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CameraMode;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.Identifier;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.core.impl.RestrictedCameraControl;
import androidx.camera.core.impl.RestrictedCameraInfo;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType;
import androidx.camera.core.impl.stabilization.StabilizationMode;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.streamsharing.StreamSharing;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link CameraInternal} adapter which checks that the UseCases to make sure that the resolutions
 * and image formats can be supported.
 */
public final class CameraUseCaseAdapter implements Camera {
    @NonNull
    private final CameraInternal mCameraInternal;
    @Nullable
    private final CameraInternal mSecondaryCameraInternal;
    private final CameraDeviceSurfaceManager mCameraDeviceSurfaceManager;
    private final UseCaseConfigFactory mUseCaseConfigFactory;

    private static final String TAG = "CameraUseCaseAdapter";

    private final CameraId mId;

    // UseCases from the app. This does not include internal UseCases created by CameraX.
    @GuardedBy("mLock")
    private final List<UseCase> mAppUseCases = new ArrayList<>();
    // UseCases sent to the camera including internal UseCases created by CameraX.
    @GuardedBy("mLock")
    private final List<UseCase> mCameraUseCases = new ArrayList<>();

    @GuardedBy("mLock")
    private final CameraCoordinator mCameraCoordinator;

    @GuardedBy("mLock")
    @Nullable
    private ViewPort mViewPort;

    @GuardedBy("mLock")
    @NonNull
    private List<CameraEffect> mEffects = emptyList();

    // Additional configs to apply onto the UseCases when added to this Camera
    @GuardedBy("mLock")
    @NonNull
    private final CameraConfig mCameraConfig;

    private final Object mLock = new Object();

    // This indicates whether or not the UseCases that have been added to this adapter has
    // actually been attached to the CameraInternal instance.
    @GuardedBy("mLock")
    private boolean mAttached = true;

    // This holds the cached Interop config from CameraControlInternal.
    @GuardedBy("mLock")
    private Config mInteropConfig = null;

    // The placeholder UseCase created to meet combination criteria for Extensions. e.g. When
    // Extensions require both Preview and ImageCapture and app only provides one of them,
    // CameraX will create the other and track it with this variable.
    @GuardedBy("mLock")
    @Nullable
    private UseCase mPlaceholderForExtensions;
    // Current StreamSharing parent UseCase if exists.
    @GuardedBy("mLock")
    @Nullable
    private StreamSharing mStreamSharing;

    @NonNull
    private final RestrictedCameraControl mAdapterCameraControl;
    @NonNull
    private final RestrictedCameraInfo mAdapterCameraInfo;

    @Nullable
    private final RestrictedCameraInfo mAdapterSecondaryCameraInfo;

    @NonNull
    private final CompositionSettings mCompositionSettings;
    @NonNull
    private final CompositionSettings mSecondaryCompositionSettings;

    /**
     * Create a new {@link CameraUseCaseAdapter} instance.
     *
     * @param camera                     The camera that is wrapped.
     * @param cameraCoordinator          Camera coordinator that exposes concurrent camera mode.
     * @param cameraDeviceSurfaceManager A class that checks for whether a specific camera
     *                                   can support the set of Surface with set resolutions.
     * @param useCaseConfigFactory       UseCase config factory that exposes configuration for
     *                                   each UseCase.
     */
    public CameraUseCaseAdapter(@NonNull CameraInternal camera,
            @NonNull CameraCoordinator cameraCoordinator,
            @NonNull CameraDeviceSurfaceManager cameraDeviceSurfaceManager,
            @NonNull UseCaseConfigFactory useCaseConfigFactory) {
        this(camera,
                null,
                new RestrictedCameraInfo(camera.getCameraInfoInternal(),
                        CameraConfigs.defaultConfig()),
                null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                cameraCoordinator,
                cameraDeviceSurfaceManager,
                useCaseConfigFactory);
    }

    /**
     * Create a new {@link CameraUseCaseAdapter} instance.
     *
     * @param camera                     The camera that is wrapped.
     * @param secondaryCamera            The secondary camera that is wrapped.
     * @param restrictedCameraInfo       The {@link RestrictedCameraInfo} that contains the extra
     *                                   information to configure the {@link CameraInternal} when
     *                                   attaching the uses cases of this adapter to the camera.
     * @param secondaryRestrictedCameraInfo The {@link RestrictedCameraInfo} of secondary camera.
     * @param compositionSettings        The composition settings that will be used to configure the
     *                                   camera.
     * @param secondaryCompositionSettings  The composition settings that will be used to configure
     *                                      the secondary camera.
     * @param cameraCoordinator          Camera coordinator that exposes concurrent camera mode.
     * @param cameraDeviceSurfaceManager A class that checks for whether a specific camera
     *                                   can support the set of Surface with set resolutions.
     * @param useCaseConfigFactory       UseCase config factory that exposes configuration for
     *                                   each UseCase.
     */
    public CameraUseCaseAdapter(
            @NonNull CameraInternal camera,
            @Nullable CameraInternal secondaryCamera,
            @NonNull RestrictedCameraInfo restrictedCameraInfo,
            @Nullable RestrictedCameraInfo secondaryRestrictedCameraInfo,
            @NonNull CompositionSettings compositionSettings,
            @NonNull CompositionSettings secondaryCompositionSettings,
            @NonNull CameraCoordinator cameraCoordinator,
            @NonNull CameraDeviceSurfaceManager cameraDeviceSurfaceManager,
            @NonNull UseCaseConfigFactory useCaseConfigFactory) {
        mCameraInternal = camera;
        mSecondaryCameraInternal = secondaryCamera;
        mCompositionSettings = compositionSettings;
        mSecondaryCompositionSettings = secondaryCompositionSettings;
        mCameraCoordinator = cameraCoordinator;
        mCameraDeviceSurfaceManager = cameraDeviceSurfaceManager;
        mUseCaseConfigFactory = useCaseConfigFactory;
        mCameraConfig = restrictedCameraInfo.getCameraConfig();
        SessionProcessor sessionProcessor = mCameraConfig.getSessionProcessor(null);
        // TODO(b/279996499): bind the same restricted CameraControl and CameraInfo to use cases.
        mAdapterCameraControl = new RestrictedCameraControl(
                mCameraInternal.getCameraControlInternal(), sessionProcessor);
        mAdapterCameraInfo = restrictedCameraInfo;
        mAdapterSecondaryCameraInfo = secondaryRestrictedCameraInfo;
        mId = generateCameraId(restrictedCameraInfo, secondaryRestrictedCameraInfo);
    }

    /**
     * Generate a identifier for the {@link RestrictedCameraInfo}.
     */
    @NonNull
    public static CameraId generateCameraId(
            @NonNull RestrictedCameraInfo primaryCameraInfo,
            @Nullable RestrictedCameraInfo secondaryCameraInfo) {
        return CameraId.create(
                primaryCameraInfo.getCameraId()
                        + (secondaryCameraInfo == null ? "" : secondaryCameraInfo.getCameraId()),
                primaryCameraInfo.getCameraConfig().getCompatibilityId());
    }

    /**
     * Returns the identifier for this {@link CameraUseCaseAdapter}.
     */
    @NonNull
    public CameraId getCameraId() {
        return mId;
    }

    /**
     * Returns true if the {@link CameraUseCaseAdapter} is an equivalent camera.
     */
    public boolean isEquivalent(@NonNull CameraUseCaseAdapter cameraUseCaseAdapter) {
        return getCameraId().equals(cameraUseCaseAdapter.getCameraId());
    }

    /**
     * Set the viewport that will be used for the {@link UseCase} attached to the camera.
     */
    public void setViewPort(@Nullable ViewPort viewPort) {
        synchronized (mLock) {
            mViewPort = viewPort;
        }
    }

    /**
     * Set the effects that will be used for the {@link UseCase} attached to the camera.
     */
    public void setEffects(@Nullable List<CameraEffect> effects) {
        synchronized (mLock) {
            mEffects = effects;
        }
    }

    /**
     * Add the specified collection of {@link UseCase} to the adapter with dual camera support.
     *
     * @throws CameraException Thrown if the combination of newly added UseCases and the
     *                         currently added UseCases exceed the capability of the camera.
     */
    public void addUseCases(@NonNull Collection<UseCase> appUseCasesToAdd) throws CameraException {
        synchronized (mLock) {
            // Configure the CameraConfig when binding
            mCameraInternal.setExtendedConfig(mCameraConfig);
            if (mSecondaryCameraInternal != null) {
                mSecondaryCameraInternal.setExtendedConfig(mCameraConfig);
            }
            Set<UseCase> appUseCases = new LinkedHashSet<>(mAppUseCases);
            //TODO(b/266641900): must be LinkedHashSet otherwise ExistingActivityLifecycleTest
            // fails due to a camera-pipe integration bug.
            appUseCases.addAll(appUseCasesToAdd);
            try {
                updateUseCases(appUseCases,
                        mSecondaryCameraInternal != null, mSecondaryCameraInternal != null);
            } catch (IllegalArgumentException e) {
                throw new CameraException(e);
            }
        }
    }

    /**
     * Remove the specified collection of {@link UseCase} from the adapter.
     */
    public void removeUseCases(@NonNull Collection<UseCase> useCasesToRemove) {
        synchronized (mLock) {
            Set<UseCase> appUseCases = new LinkedHashSet<>(mAppUseCases);
            appUseCases.removeAll(useCasesToRemove);
            updateUseCases(appUseCases,
                    mSecondaryCameraInternal != null, mSecondaryCameraInternal != null);
        }
    }

    /**
     * Updates the states based the new app UseCases.
     */
    void updateUseCases(@NonNull Collection<UseCase> appUseCases) {
        updateUseCases(appUseCases, /*applyStreamSharing*/false, /*isDualCamera*/false);
    }

    /**
     * Updates the states based the new app UseCases.
     *
     * <p> This method calculates the new camera UseCases based on the input and the current state,
     * attach/detach the camera UseCases, and save the updated state in following member variables:
     * {@link #mCameraUseCases}, {@link #mAppUseCases} and {@link #mPlaceholderForExtensions}.
     *
     * @throws IllegalArgumentException if the UseCase combination is not supported. In that case,
     *                                  it will not update the internal states.
     */
    void updateUseCases(@NonNull Collection<UseCase> appUseCases,
            boolean applyStreamSharing,
            boolean isDualCamera) {
        synchronized (mLock) {
            checkUnsupportedFeatureCombinationAndThrow(appUseCases);

            // Force enable StreamSharing for Extensions to support VideoCapture. This means that
            // applyStreamSharing is set to true when the use case combination contains
            // VideoCapture and Extensions is enabled.
            if (!applyStreamSharing && hasExtension() && hasVideoCapture(appUseCases)) {
                updateUseCases(appUseCases, /*applyStreamSharing*/true, isDualCamera);
                return;
            }

            // Calculate camera UseCases and keep the result in local variables in case they don't
            // meet the stream combination rules.
            StreamSharing streamSharing = createOrReuseStreamSharing(appUseCases,
                    applyStreamSharing);
            UseCase placeholderForExtensions = calculatePlaceholderForExtensions(appUseCases,
                    streamSharing);
            Collection<UseCase> cameraUseCases =
                    calculateCameraUseCases(appUseCases, placeholderForExtensions, streamSharing);

            // Calculate the action items.
            List<UseCase> cameraUseCasesToAttach = new ArrayList<>(cameraUseCases);
            cameraUseCasesToAttach.removeAll(mCameraUseCases);
            List<UseCase> cameraUseCasesToKeep = new ArrayList<>(cameraUseCases);
            cameraUseCasesToKeep.retainAll(mCameraUseCases);
            List<UseCase> cameraUseCasesToDetach = new ArrayList<>(mCameraUseCases);
            cameraUseCasesToDetach.removeAll(cameraUseCases);

            // Calculate suggested resolutions. This step throws exception if the camera UseCases
            // fails the supported stream combination rules.
            Map<UseCase, ConfigPair> configs = getConfigs(cameraUseCasesToAttach,
                    mCameraConfig.getUseCaseConfigFactory(), mUseCaseConfigFactory);

            Map<UseCase, StreamSpec> primaryStreamSpecMap;
            Map<UseCase, StreamSpec> secondaryStreamSpecMap = Collections.emptyMap();
            try {
                primaryStreamSpecMap = calculateSuggestedStreamSpecs(
                        getCameraMode(),
                        mCameraInternal.getCameraInfoInternal(), cameraUseCasesToAttach,
                        cameraUseCasesToKeep, configs);
                if (mSecondaryCameraInternal != null) {
                    secondaryStreamSpecMap = calculateSuggestedStreamSpecs(
                            getCameraMode(),
                            requireNonNull(mSecondaryCameraInternal).getCameraInfoInternal(),
                            cameraUseCasesToAttach,
                            cameraUseCasesToKeep, configs);
                }
                // TODO(b/265704882): enable stream sharing for LEVEL_3 and high preview
                //  resolution. Throw exception here if (applyStreamSharing == false), both video
                //  and preview are used and preview resolution is lower than user configuration.
            } catch (IllegalArgumentException exception) {
                // TODO(b/270187871): instead of catch and retry, we can check UseCase
                //  combination directly with #isUseCasesCombinationSupported(). However
                //  calculateSuggestedStreamSpecs() is currently slow. We will do it after it's
                //  optimized
                // Only allow StreamSharing for non-concurrent mode.
                if (!applyStreamSharing && !hasExtension()
                        && mCameraCoordinator.getCameraOperatingMode()
                        != CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT) {
                    // Try again and see if StreamSharing resolves the issue.
                    updateUseCases(appUseCases, /*applyStreamSharing*/true, isDualCamera);
                    return;
                } else {
                    // If StreamSharing already on or not enabled, throw exception.
                    throw exception;
                }
            }

            // Update properties.
            updateViewPort(primaryStreamSpecMap, cameraUseCases);
            updateEffects(mEffects, cameraUseCases, appUseCases);

            // Detach unused UseCases.
            for (UseCase useCase : cameraUseCasesToDetach) {
                useCase.unbindFromCamera(mCameraInternal);
            }
            mCameraInternal.detachUseCases(cameraUseCasesToDetach);

            // Detach unused UseCases for secondary camera.
            if (mSecondaryCameraInternal != null) {
                for (UseCase useCase : cameraUseCasesToDetach) {
                    useCase.unbindFromCamera(requireNonNull(mSecondaryCameraInternal));
                }
                requireNonNull(mSecondaryCameraInternal)
                        .detachUseCases(cameraUseCasesToDetach);
            }

            // Update StreamSpec for UseCases to keep.
            if (cameraUseCasesToDetach.isEmpty()) {
                // Only do this if we are not removing UseCase, because updating SessionConfig
                // when removing UseCases may lead to flickering.
                for (UseCase useCase : cameraUseCasesToKeep) {
                    // Assume secondary camera will not have implementation options in dual camera.
                    if (primaryStreamSpecMap.containsKey(useCase)) {
                        StreamSpec newStreamSpec = primaryStreamSpecMap.get(useCase);
                        Config config = newStreamSpec.getImplementationOptions();
                        if (config != null && hasImplementationOptionChanged(newStreamSpec,
                                useCase.getSessionConfig())) {
                            useCase.updateSuggestedStreamSpecImplementationOptions(config);
                            if (mAttached) {
                                mCameraInternal.onUseCaseUpdated(useCase);
                                if (mSecondaryCameraInternal != null) {
                                    requireNonNull(mSecondaryCameraInternal)
                                            .onUseCaseUpdated(useCase);
                                }
                            }
                        }
                    }
                }
            }

            // Attach new UseCases.
            for (UseCase useCase : cameraUseCasesToAttach) {
                ConfigPair configPair = requireNonNull(configs.get(useCase));
                if (mSecondaryCameraInternal != null) {
                    useCase.bindToCamera(mCameraInternal,
                            requireNonNull(mSecondaryCameraInternal),
                            configPair.mExtendedConfig,
                            configPair.mCameraConfig);
                    useCase.updateSuggestedStreamSpec(
                            Preconditions.checkNotNull(primaryStreamSpecMap.get(useCase)),
                            secondaryStreamSpecMap.get(useCase));
                } else {
                    useCase.bindToCamera(mCameraInternal,
                            null,
                            configPair.mExtendedConfig,
                            configPair.mCameraConfig);
                    useCase.updateSuggestedStreamSpec(
                            Preconditions.checkNotNull(primaryStreamSpecMap.get(useCase)),
                            null);
                }
            }
            if (mAttached) {
                mCameraInternal.attachUseCases(cameraUseCasesToAttach);
                if (mSecondaryCameraInternal != null) {
                    requireNonNull(mSecondaryCameraInternal)
                            .attachUseCases(cameraUseCasesToAttach);
                }
            }

            // Once UseCases are detached/attached, notify the camera.
            for (UseCase useCase : cameraUseCasesToAttach) {
                useCase.notifyState();
            }

            // The changes are successful. Update the states of this class.
            mAppUseCases.clear();
            mAppUseCases.addAll(appUseCases);
            mCameraUseCases.clear();
            mCameraUseCases.addAll(cameraUseCases);
            mPlaceholderForExtensions = placeholderForExtensions;
            mStreamSharing = streamSharing;
        }
    }

    /**
     * Return true if the given StreamSpec has any option with a different value than that
     * of the given sessionConfig.
     */
    private static boolean hasImplementationOptionChanged(
            StreamSpec streamSpec,
            SessionConfig sessionConfig) {
        Config newStreamSpecOptions = streamSpec.getImplementationOptions();
        Config sessionConfigOptions = sessionConfig.getImplementationOptions();
        if (newStreamSpecOptions.listOptions().size()
                != sessionConfig.getImplementationOptions().listOptions().size()) {
            return true;
        }
        for (Config.Option<?> newOption : newStreamSpecOptions.listOptions()) {
            if (!sessionConfigOptions.containsOption(newOption)
                    || !Objects.equals(sessionConfigOptions.retrieveOption(newOption),
                    newStreamSpecOptions.retrieveOption(newOption))) {
                return true;
            }
        }
        return false;
    }

    private @CameraMode.Mode int getCameraMode() {
        synchronized (mLock) {
            if (mCameraCoordinator.getCameraOperatingMode()
                    == CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT) {
                return CameraMode.CONCURRENT_CAMERA;
            }
        }

        // TODO(b/271199876): return ULTRA_HIGH_RESOLUTION_CAMERA when it can be enabled via
        //  Camera2Interop

        return CameraMode.DEFAULT;
    }

    private boolean hasExtension() {
        synchronized (mLock) {
            return mCameraConfig.getSessionProcessor(null) != null;
        }
    }

    /**
     * Returns {@link UseCase}s qualified for {@link StreamSharing}.
     */
    @NonNull
    private Set<UseCase> getStreamSharingChildren(@NonNull Collection<UseCase> appUseCases,
            boolean forceSharingToPreviewAndVideo) {
        Set<UseCase> children = new HashSet<>();
        int sharingTargets = getSharingTargets(forceSharingToPreviewAndVideo);
        for (UseCase useCase : appUseCases) {
            checkArgument(!isStreamSharing(useCase), "Only support one level of sharing for now.");
            if (useCase.isEffectTargetsSupported(sharingTargets)) {
                children.add(useCase);
            }
        }
        return children;
    }

    @CameraEffect.Targets
    private int getSharingTargets(boolean forceSharingToPreviewAndVideo) {
        synchronized (mLock) {
            // Find the only effect that has more than one targets.
            CameraEffect sharingEffect = null;
            for (CameraEffect effect : mEffects) {
                if (getNumberOfTargets(effect.getTargets()) > 1) {
                    checkState(sharingEffect == null, "Can only have one sharing effect.");
                    sharingEffect = effect;
                }
            }
            int sharingTargets = sharingEffect == null ? 0 : sharingEffect.getTargets();

            // Share stream to preview and video capture if the device requires it.
            if (forceSharingToPreviewAndVideo) {
                sharingTargets |= PREVIEW | VIDEO_CAPTURE;
            }
            return sharingTargets;
        }
    }

    /**
     * Creates a new {@link StreamSharing} or returns the existing one.
     *
     * <p>Returns the existing {@link StreamSharing} if the children have not changed.
     * Otherwise, create a new {@link StreamSharing} and return.
     *
     * <p>Returns null when there is no need to share the stream, or the combination of children
     * UseCase are invalid(e.g. contains more than 1 UseCase per type).
     */
    @Nullable
    private StreamSharing createOrReuseStreamSharing(@NonNull Collection<UseCase> appUseCases,
            boolean forceSharingToPreviewAndVideo) {
        synchronized (mLock) {
            Set<UseCase> newChildren = getStreamSharingChildren(appUseCases,
                    forceSharingToPreviewAndVideo);
            if (newChildren.size() < 2) {
                // No need to share the stream for 1 or less children. Except the case that
                // StreamSharing is enabled for Extensions to support VideoCapture.
                if (!(hasExtension() && hasVideoCapture(newChildren))) {
                    return null;
                }
            }
            if (mStreamSharing != null && mStreamSharing.getChildren().equals(newChildren)) {
                // Returns the current instance if the new children equals the old.
                return requireNonNull(mStreamSharing);
            }

            if (!isStreamSharingChildrenCombinationValid(newChildren)) {
                return null;
            }

            return new StreamSharing(mCameraInternal,
                    mSecondaryCameraInternal,
                    mCompositionSettings,
                    mSecondaryCompositionSettings,
                    newChildren,
                    mUseCaseConfigFactory);
        }
    }

    /**
     * Returns true if the children are valid for {@link StreamSharing}.
     */
    static boolean isStreamSharingChildrenCombinationValid(
            @NonNull Collection<UseCase> children) {
        int[] validChildrenTypes = {PREVIEW, VIDEO_CAPTURE, IMAGE_CAPTURE};
        Set<Integer> childrenTypes = new HashSet<>();
        // Loop through all children and add supported types.
        for (UseCase child : children) {
            for (int type : validChildrenTypes) {
                if (child.isEffectTargetsSupported(type)) {
                    if (childrenTypes.contains(type)) {
                        // Return false if there are 2 use case supporting the same type.
                        return false;
                    }
                    childrenTypes.add(type);
                }
            }
        }
        return true;
    }

    /**
     * Returns {@link UseCase} that connects to the camera.
     */
    static Collection<UseCase> calculateCameraUseCases(@NonNull Collection<UseCase> appUseCases,
            @Nullable UseCase placeholderForExtensions,
            @Nullable StreamSharing streamSharing) {
        List<UseCase> useCases = new ArrayList<>(appUseCases);
        if (placeholderForExtensions != null) {
            useCases.add(placeholderForExtensions);
        }
        if (streamSharing != null) {
            useCases.add(streamSharing);
            useCases.removeAll(streamSharing.getChildren());
        }
        return useCases;
    }

    /**
     * Returns the UseCases currently associated with the adapter.
     *
     * <p> The UseCases may or may not be actually attached to the underlying
     * {@link CameraInternal} instance.
     */
    @NonNull
    public List<UseCase> getUseCases() {
        synchronized (mLock) {
            return new ArrayList<>(mAppUseCases);
        }
    }

    @VisibleForTesting
    @NonNull
    public Collection<UseCase> getCameraUseCases() {
        synchronized (mLock) {
            return new ArrayList<>(mCameraUseCases);
        }
    }

    /**
     * Attach the UseCases to the {@link CameraInternal} camera so that the UseCases can receive
     * data if they are active.
     *
     * <p> This will start the underlying {@link CameraInternal} instance.
     *
     * <p> This will restore the cached Interop config to the {@link CameraInternal}.
     */
    public void attachUseCases() {
        synchronized (mLock) {
            if (!mAttached) {
                // Ensure the current opening camera has the right camera config.
                if (!mCameraUseCases.isEmpty()) {
                    mCameraInternal.setExtendedConfig(mCameraConfig);
                    if (mSecondaryCameraInternal != null) {
                        mSecondaryCameraInternal.setExtendedConfig(mCameraConfig);
                    }
                }
                mCameraInternal.attachUseCases(mCameraUseCases);
                if (mSecondaryCameraInternal != null) {
                    mSecondaryCameraInternal.attachUseCases(mCameraUseCases);
                }
                restoreInteropConfig();

                // Notify to update the use case's active state because it may be cleared if the
                // use case was ever detached from a camera previously.
                for (UseCase useCase : mCameraUseCases) {
                    useCase.notifyState();
                }

                mAttached = true;
            }
        }
    }

    /**
     * When in active resuming mode, it will actively retry opening the camera periodically to
     * resume regardless of the camera availability if the camera is interrupted in
     * OPEN/OPENING/PENDING_OPEN state.
     *
     * When not in actively resuming mode, it will retry opening camera only when camera
     * becomes available.
     */
    public void setActiveResumingMode(boolean enabled) {
        mCameraInternal.setActiveResumingMode(enabled);
    }

    /**
     * Detach the UseCases from the {@link CameraInternal} so that the UseCases stop receiving data.
     *
     * <p> This will stop the underlying {@link CameraInternal} instance.
     *
     * <p> This will cache the Interop config from the {@link CameraInternal}.
     */
    public void detachUseCases() {
        synchronized (mLock) {
            if (mAttached) {
                mCameraInternal.detachUseCases(new ArrayList<>(mCameraUseCases));
                if (mSecondaryCameraInternal != null) {
                    mSecondaryCameraInternal.detachUseCases(new ArrayList<>(mCameraUseCases));
                }
                cacheInteropConfig();
                mAttached = false;
            }
        }
    }

    /**
     * Restores the cached InteropConfig to the camera.
     */
    private void restoreInteropConfig() {
        synchronized (mLock) {
            if (mInteropConfig != null) {
                mCameraInternal.getCameraControlInternal().addInteropConfig(mInteropConfig);
            }
        }
    }

    /**
     * Caches and clears the InteropConfig from the camera.
     */
    private void cacheInteropConfig() {
        synchronized (mLock) {
            CameraControlInternal cameraControlInternal =
                    mCameraInternal.getCameraControlInternal();
            mInteropConfig = cameraControlInternal.getInteropConfig();
            cameraControlInternal.clearInteropConfig();
        }
    }

    private Map<UseCase, StreamSpec> calculateSuggestedStreamSpecs(
            @CameraMode.Mode int cameraMode,
            @NonNull CameraInfoInternal cameraInfoInternal,
            @NonNull Collection<UseCase> newUseCases,
            @NonNull Collection<UseCase> currentUseCases,
            @NonNull Map<UseCase, ConfigPair> configPairMap) {
        List<AttachedSurfaceInfo> existingSurfaces = new ArrayList<>();
        String cameraId = cameraInfoInternal.getCameraId();
        Map<UseCase, StreamSpec> suggestedStreamSpecs = new HashMap<>();
        Map<AttachedSurfaceInfo, UseCase> surfaceInfoUseCaseMap = new HashMap<>();

        // Get resolution for current use cases.
        for (UseCase useCase : currentUseCases) {
            SurfaceConfig surfaceConfig =
                    mCameraDeviceSurfaceManager.transformSurfaceConfig(
                            cameraMode,
                            cameraId,
                            useCase.getImageFormat(),
                            useCase.getAttachedSurfaceResolution());
            AttachedSurfaceInfo attachedSurfaceInfo = AttachedSurfaceInfo.create(surfaceConfig,
                    useCase.getImageFormat(), useCase.getAttachedSurfaceResolution(),
                    Preconditions.checkNotNull(useCase.getAttachedStreamSpec()).getDynamicRange(),
                    getCaptureTypes(useCase),
                    useCase.getAttachedStreamSpec().getImplementationOptions(),
                    useCase.getCurrentConfig().getTargetFrameRate(null));
            existingSurfaces.add(attachedSurfaceInfo);
            surfaceInfoUseCaseMap.put(attachedSurfaceInfo, useCase);
            suggestedStreamSpecs.put(useCase, useCase.getAttachedStreamSpec());
        }

        // Calculate resolution for new use cases.
        if (!newUseCases.isEmpty()) {
            Map<UseCaseConfig<?>, UseCase> configToUseCaseMap = new HashMap<>();
            Map<UseCaseConfig<?>, List<Size>> configToSupportedSizesMap = new HashMap<>();
            Rect sensorRect;
            try {
                sensorRect = mCameraInternal.getCameraControlInternal().getSensorRect();
            } catch (NullPointerException e) {
                // TODO(b/274531208): Remove the unnecessary SENSOR_INFO_ACTIVE_ARRAY_SIZE NPE
                //  check related code only which is used for robolectric tests
                sensorRect = null;
            }
            SupportedOutputSizesSorter supportedOutputSizesSorter = new SupportedOutputSizesSorter(
                    cameraInfoInternal,
                    sensorRect != null ? rectToSize(sensorRect) : null);
            boolean isPreviewStabilizationOn = false;
            for (UseCase useCase : newUseCases) {
                ConfigPair configPair = configPairMap.get(useCase);
                // Combine with default configuration.
                UseCaseConfig<?> combinedUseCaseConfig =
                        useCase.mergeConfigs(cameraInfoInternal, configPair.mExtendedConfig,
                                configPair.mCameraConfig);
                configToUseCaseMap.put(combinedUseCaseConfig, useCase);
                configToSupportedSizesMap.put(combinedUseCaseConfig,
                        supportedOutputSizesSorter.getSortedSupportedOutputSizes(
                                combinedUseCaseConfig));

                if (useCase.getCurrentConfig() instanceof PreviewConfig) {
                    isPreviewStabilizationOn =
                            ((PreviewConfig) useCase.getCurrentConfig())
                                    .getPreviewStabilizationMode() == StabilizationMode.ON;
                }
            }

            // Get suggested stream specifications and update the use case session configuration
            Pair<Map<UseCaseConfig<?>, StreamSpec>, Map<AttachedSurfaceInfo, StreamSpec>>
                    streamSpecMaps =
                    mCameraDeviceSurfaceManager.getSuggestedStreamSpecs(
                            cameraMode,
                            cameraId, existingSurfaces,
                            configToSupportedSizesMap,
                            isPreviewStabilizationOn,
                            hasVideoCapture(newUseCases));

            for (Map.Entry<UseCaseConfig<?>, UseCase> entry : configToUseCaseMap.entrySet()) {
                suggestedStreamSpecs.put(entry.getValue(),
                        streamSpecMaps.first.get(entry.getKey()));
            }
            for (Map.Entry<AttachedSurfaceInfo, StreamSpec> entry :
                    streamSpecMaps.second.entrySet()) {
                if (surfaceInfoUseCaseMap.containsKey(entry.getKey())) {
                    suggestedStreamSpecs.put(surfaceInfoUseCaseMap.get(entry.getKey()),
                            entry.getValue());
                }
            }
        }
        return suggestedStreamSpecs;
    }

    @VisibleForTesting
    static void updateEffects(@NonNull List<CameraEffect> effects,
            @NonNull Collection<UseCase> cameraUseCases,
            @NonNull Collection<UseCase> appUseCases) {
        // Match camera UseCases first. Apply the effect early in the pipeline if possible.
        List<CameraEffect> unusedEffects = setEffectsOnUseCases(effects, cameraUseCases);

        // Match unused effects with app only UseCases.
        List<UseCase> appOnlyUseCases = new ArrayList<>(appUseCases);
        appOnlyUseCases.removeAll(cameraUseCases);
        unusedEffects = setEffectsOnUseCases(unusedEffects, appOnlyUseCases);

        if (unusedEffects.size() > 0) {
            Logger.w(TAG, "Unused effects: " + unusedEffects);
        }
    }

    /**
     * Sets effects on the given {@link UseCase} list and returns unused effects.
     */
    @NonNull
    private static List<CameraEffect> setEffectsOnUseCases(@NonNull List<CameraEffect> effects,
            @NonNull Collection<UseCase> useCases) {
        List<CameraEffect> unusedEffects = new ArrayList<>(effects);
        for (UseCase useCase : useCases) {
            useCase.setEffect(null);
            for (CameraEffect effect : effects) {
                if (useCase.isEffectTargetsSupported(effect.getTargets())) {
                    checkState(useCase.getEffect() == null,
                            useCase + " already has effect" + useCase.getEffect());
                    useCase.setEffect(effect);
                    unusedEffects.remove(effect);
                }
            }
        }
        return unusedEffects;
    }

    private void updateViewPort(@NonNull Map<UseCase, StreamSpec> suggestedStreamSpecMap,
            @NonNull Collection<UseCase> useCases) {
        synchronized (mLock) {
            if (mViewPort != null && !useCases.isEmpty()) {
                Integer lensFacing = mCameraInternal.getCameraInfoInternal().getLensFacing();
                boolean isFrontCamera;
                if (lensFacing == null) {
                    // TODO(b/122975195): If the lens facing is null, it's probably an external
                    //  camera. We treat it as like a front camera with unverified behaviors. Will
                    //  have to define this later.
                    Logger.w(TAG, "The lens facing is null, probably an external.");
                    isFrontCamera = true;
                } else {
                    isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT;
                }
                // Calculate crop rect if view port is provided.
                Map<UseCase, Rect> cropRectMap = ViewPorts.calculateViewPortRects(
                        mCameraInternal.getCameraControlInternal().getSensorRect(),
                        isFrontCamera,
                        mViewPort.getAspectRatio(),
                        mCameraInternal.getCameraInfoInternal().getSensorRotationDegrees(
                                mViewPort.getRotation()),
                        mViewPort.getScaleType(),
                        mViewPort.getLayoutDirection(),
                        suggestedStreamSpecMap);
                for (UseCase useCase : useCases) {
                    useCase.setViewPortCropRect(
                            Preconditions.checkNotNull(cropRectMap.get(useCase)));
                    useCase.setSensorToBufferTransformMatrix(
                            calculateSensorToBufferTransformMatrix(
                                    mCameraInternal.getCameraControlInternal().getSensorRect(),
                                    Preconditions.checkNotNull(
                                            suggestedStreamSpecMap.get(useCase)).getResolution()));
                }
            }
        }
    }

    @NonNull
    private static Matrix calculateSensorToBufferTransformMatrix(
            @NonNull Rect fullSensorRect,
            @NonNull Size useCaseSize) {
        checkArgument(
                fullSensorRect.width() > 0 && fullSensorRect.height() > 0,
                "Cannot compute viewport crop rects zero sized sensor rect.");
        RectF fullSensorRectF = new RectF(fullSensorRect);
        Matrix sensorToUseCaseTransformation = new Matrix();
        RectF srcRect = new RectF(0, 0, useCaseSize.getWidth(),
                useCaseSize.getHeight());
        sensorToUseCaseTransformation.setRectToRect(srcRect, fullSensorRectF,
                Matrix.ScaleToFit.CENTER);
        sensorToUseCaseTransformation.invert(sensorToUseCaseTransformation);
        return sensorToUseCaseTransformation;
    }

    // Pair of UseCase configs. One for the extended config applied on top of the use case and
    // the camera default which applied underneath the use case's config.
    private static class ConfigPair {
        ConfigPair(UseCaseConfig<?> extendedConfig, UseCaseConfig<?> cameraConfig) {
            mExtendedConfig = extendedConfig;
            mCameraConfig = cameraConfig;
        }

        UseCaseConfig<?> mExtendedConfig;
        UseCaseConfig<?> mCameraConfig;
    }

    /**
     * Gets a map of the configs for the use cases from the respective factories.
     */
    private static Map<UseCase, ConfigPair> getConfigs(@NonNull Collection<UseCase> useCases,
            @NonNull UseCaseConfigFactory extendedFactory,
            @NonNull UseCaseConfigFactory cameraFactory) {
        Map<UseCase, ConfigPair> configs = new HashMap<>();
        for (UseCase useCase : useCases) {
            UseCaseConfig<?> extendedConfig;
            if (isStreamSharing(useCase)) {
                extendedConfig = generateExtendedStreamSharingConfigFromPreview(extendedFactory,
                        (StreamSharing) useCase);
            } else {
                extendedConfig = useCase.getDefaultConfig(false, extendedFactory);
            }
            UseCaseConfig<?> cameraConfig = useCase.getDefaultConfig(true, cameraFactory);
            configs.put(useCase, new ConfigPair(extendedConfig, cameraConfig));
        }
        return configs;
    }

    private static UseCaseConfig<?> generateExtendedStreamSharingConfigFromPreview(
            @NonNull UseCaseConfigFactory extendedFactory, @NonNull StreamSharing streamSharing) {
        Preview preview = new Preview.Builder().build();
        Config previewConfig = preview.getDefaultConfig(false, extendedFactory);
        if (previewConfig == null) {
            return null;
        }

        // Remove OPTION_TARGET_CLASS, since its value would be "Preview".
        MutableOptionsBundle mutableConfig = MutableOptionsBundle.from(previewConfig);
        mutableConfig.removeOption(TargetConfig.OPTION_TARGET_CLASS);

        return streamSharing.getUseCaseConfigBuilder(mutableConfig).getUseCaseConfig();
    }

    /**
     * Checks for any unsupported feature combinations and throws an exception if found.
     *
     * @throws IllegalArgumentException if any feature combination is not supported.
     */
    private void checkUnsupportedFeatureCombinationAndThrow(@NonNull Collection<UseCase> useCases)
            throws IllegalArgumentException {
        // TODO(b/309900490): since there are other places (e.g. SupportedSurfaceCombination in
        //  camera2) that feature combination constraints are enforced, it would be nice if they
        //  followed a similar pattern for checking constraints.
        if (hasExtension() && hasNonSdrConfig(useCases)) {
            throw new IllegalArgumentException("Extensions are only supported for use with "
                    + "standard dynamic range.");
        }

        // TODO(b/322311893): throw exception to block feature combination of effect with Ultra
        //  HDR, until ImageProcessor and SurfaceProcessor can support JPEG/R format.
        synchronized (mLock) {
            if (!mEffects.isEmpty() && hasUltraHdrImageCapture(useCases)) {
                throw new IllegalArgumentException("Ultra HDR image capture does not support for "
                        + "use with CameraEffect.");
            }
        }
    }

    private static boolean hasNonSdrConfig(@NonNull Collection<UseCase> useCases) {
        for (UseCase useCase : useCases) {
            DynamicRange dynamicRange = useCase.getCurrentConfig().getDynamicRange();
            if (isNotSdr(dynamicRange)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNotSdr(@NonNull DynamicRange dynamicRange) {
        boolean is10Bit = dynamicRange.getBitDepth() == BIT_DEPTH_10_BIT;
        boolean isHdr = dynamicRange.getEncoding() != ENCODING_SDR
                && dynamicRange.getEncoding() != ENCODING_UNSPECIFIED;

        return is10Bit || isHdr;
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalImageCaptureOutputFormat.class)
    private static boolean hasUltraHdrImageCapture(@NonNull Collection<UseCase> useCases) {
        for (UseCase useCase : useCases) {
            if (!isImageCapture(useCase)) {
                continue;
            }

            UseCaseConfig<?> config = useCase.getCurrentConfig();
            if (config.containsOption(OPTION_OUTPUT_FORMAT) && checkNotNull(
                    config.retrieveOption(OPTION_OUTPUT_FORMAT)) == OUTPUT_FORMAT_JPEG_ULTRA_HDR) {
                return true;
            }

        }
        return false;
    }

    /**
     * An identifier for a {@link CameraUseCaseAdapter}.
     *
     * <p>This identifies the actual camera instances that are wrapped by the
     * CameraUseCaseAdapter and is used to determine if 2 different instances of
     * CameraUseCaseAdapter are actually equivalent.
     */
    @AutoValue
    public abstract static class CameraId {
        /** Creates a identifier for a {@link CameraUseCaseAdapter}. */
        @NonNull
        public static CameraId create(@NonNull String cameraIdString,
                @NonNull Identifier cameraConfigId) {
            return new AutoValue_CameraUseCaseAdapter_CameraId(cameraIdString, cameraConfigId);
        }

        /** Gets the camera ID string. */
        @NonNull
        public abstract String getCameraIdString();
        /** Gets the camera configuration. */
        @NonNull
        public abstract Identifier getCameraConfigId();
    }

    /**
     * An exception thrown when the {@link CameraUseCaseAdapter} errors in one of its operations.
     */
    public static final class CameraException extends Exception {
        public CameraException() {
            super();
        }

        public CameraException(@NonNull String message) {
            super(message);
        }

        public CameraException(@NonNull Throwable cause) {
            super(cause);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Camera interface
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @NonNull
    @Override
    public CameraControl getCameraControl() {
        return mAdapterCameraControl;
    }

    @NonNull
    @Override
    public CameraInfo getCameraInfo() {
        return mAdapterCameraInfo;
    }

    @Nullable
    public CameraInfo getSecondaryCameraInfo() {
        return mAdapterSecondaryCameraInfo;
    }

    @Override
    @NonNull
    public CameraConfig getExtendedConfig() {
        synchronized (mLock) {
            return mCameraConfig;
        }
    }

    @Override
    public boolean isUseCasesCombinationSupported(boolean withStreamSharing,
            @NonNull UseCase... useCases) {
        Collection<UseCase> useCasesToVerify = Arrays.asList(useCases);
        if (withStreamSharing) {
            StreamSharing streamSharing = createOrReuseStreamSharing(useCasesToVerify, true);
            useCasesToVerify = calculateCameraUseCases(useCasesToVerify, null, streamSharing);
        }
        synchronized (mLock) {
            // If the UseCases exceed the resolutions then it will throw an exception
            try {
                Map<UseCase, ConfigPair> configs = getConfigs(useCasesToVerify,
                        mCameraConfig.getUseCaseConfigFactory(), mUseCaseConfigFactory);
                calculateSuggestedStreamSpecs(
                        getCameraMode(),
                        mCameraInternal.getCameraInfoInternal(),
                        useCasesToVerify, emptyList(), configs);
            } catch (IllegalArgumentException e) {
                return false;
            }

            return true;
        }
    }

    /**
     * Calculate the internal created placeholder UseCase for Extensions.
     *
     * @param appUseCases UseCase provided by the app.
     */
    @Nullable
    private UseCase calculatePlaceholderForExtensions(@NonNull Collection<UseCase> appUseCases,
            @Nullable StreamSharing streamSharing) {
        synchronized (mLock) {
            // Replace children with StreamSharing before calculation.
            List<UseCase> useCasesToCheck = new ArrayList<>(appUseCases);
            if (streamSharing != null) {
                useCasesToCheck.add(streamSharing);
                useCasesToCheck.removeAll(streamSharing.getChildren());
            }

            // Perform calculation.
            UseCase placeholder = null;
            if (isCoexistingPreviewImageCaptureRequired()) {
                if (isExtraPreviewRequired(useCasesToCheck)) {
                    if (isPreview(mPlaceholderForExtensions)) {
                        placeholder = mPlaceholderForExtensions;
                    } else {
                        placeholder = createExtraPreview();
                    }
                } else if (isExtraImageCaptureRequired(useCasesToCheck)) {
                    if (isImageCapture(mPlaceholderForExtensions)) {
                        placeholder = mPlaceholderForExtensions;
                    } else {
                        placeholder = createExtraImageCapture();
                    }
                }
            }
            return placeholder;
        }
    }

    private boolean isCoexistingPreviewImageCaptureRequired() {
        synchronized (mLock) {
            return mCameraConfig.getUseCaseCombinationRequiredRule()
                    == CameraConfig.REQUIRED_RULE_COEXISTING_PREVIEW_AND_IMAGE_CAPTURE;
        }
    }

    /**
     * Returns true if the input use case list contains a {@link ImageCapture} but does not
     * contain a {@link Preview}.
     *
     * <p> Note that {@link StreamSharing} provides preview output surface to
     * {@link SessionProcessor} and is therefore considered a {@link Preview}.
     */
    private static boolean isExtraPreviewRequired(@NonNull Collection<UseCase> useCases) {
        boolean hasPreviewOrStreamSharing = false;
        boolean hasImageCapture = false;

        for (UseCase useCase : useCases) {
            if (isPreview(useCase) || isStreamSharing(useCase)) {
                hasPreviewOrStreamSharing = true;
            } else if (isImageCapture(useCase)) {
                hasImageCapture = true;
            }
        }

        return hasImageCapture && !hasPreviewOrStreamSharing;
    }

    /**
     * Returns true if the input use case list contains a {@link Preview} but does not contain an
     * {@link ImageCapture}.
     *
     * <p> Note that {@link StreamSharing} provides preview output surface to
     * {@link SessionProcessor} and is therefore considered a {@link Preview}.
     */
    private static boolean isExtraImageCaptureRequired(@NonNull Collection<UseCase> useCases) {
        boolean hasPreviewOrStreamSharing = false;
        boolean hasImageCapture = false;

        for (UseCase useCase : useCases) {
            if (isPreview(useCase) || isStreamSharing(useCase)) {
                hasPreviewOrStreamSharing = true;
            } else if (isImageCapture(useCase)) {
                hasImageCapture = true;
            }
        }

        return hasPreviewOrStreamSharing && !hasImageCapture;
    }

    private static boolean hasVideoCapture(@NonNull Collection<UseCase> useCases) {
        for (UseCase useCase : useCases) {
            if (isVideoCapture(useCase)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVideoCapture(@Nullable UseCase useCase) {
        if (useCase != null) {
            if (useCase.getCurrentConfig().containsOption(OPTION_CAPTURE_TYPE)) {
                return useCase.getCurrentConfig().getCaptureType() == CaptureType.VIDEO_CAPTURE;
            } else {
                Log.e(TAG, useCase + " UseCase does not have capture type.");
            }

        }
        return false;
    }

    private static boolean isPreview(@Nullable UseCase useCase) {
        return useCase instanceof Preview;
    }

    private static boolean isImageCapture(@Nullable UseCase useCase) {
        return useCase instanceof ImageCapture;
    }

    private Preview createExtraPreview() {
        Preview preview = new Preview.Builder().setTargetName("Preview-Extra").build();

        // Sets a SurfaceProvider to provide the needed surface and release it
        preview.setSurfaceProvider((surfaceRequest) -> {
            SurfaceTexture surfaceTexture = new SurfaceTexture(0);
            surfaceTexture.setDefaultBufferSize(surfaceRequest.getResolution().getWidth(),
                    surfaceRequest.getResolution().getHeight());
            surfaceTexture.detachFromGLContext();
            Surface surface = new Surface(surfaceTexture);
            surfaceRequest.provideSurface(surface,
                    CameraXExecutors.directExecutor(),
                    (surfaceResponse) -> {
                        surface.release();
                        surfaceTexture.release();
                    });
        });

        return preview;
    }

    private ImageCapture createExtraImageCapture() {
        return new ImageCapture.Builder().setTargetName("ImageCapture-Extra").build();
    }
}
