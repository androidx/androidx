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
import static androidx.camera.core.ImageCapture.OUTPUT_FORMAT_RAW;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_OUTPUT_FORMAT;
import static androidx.camera.core.impl.SessionConfig.SESSION_TYPE_HIGH_SPEED;
import static androidx.camera.core.impl.SessionConfig.SESSION_TYPE_REGULAR;
import static androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_TYPE;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_SESSION_TYPE;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_TARGET_FRAME_RATE;
import static androidx.camera.core.processing.TargetUtils.getNumberOfTargets;
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
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CompositionSettings;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ExperimentalSessionConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.ViewPort;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.featurecombination.Feature;
import androidx.camera.core.featurecombination.impl.ResolvedFeatureCombination;
import androidx.camera.core.impl.AdapterCameraInfo;
import androidx.camera.core.impl.AdapterCameraInternal;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraConfigs;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CameraMode;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.Identifier;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.compat.workaround.StreamSharingForceEnabler;
import androidx.camera.core.streamsharing.StreamSharing;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
    private final @NonNull AdapterCameraInternal mCameraInternal;
    private final @Nullable AdapterCameraInternal mSecondaryCameraInternal;
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
    private @Nullable ViewPort mViewPort;

    @GuardedBy("mLock")
    private @NonNull List<CameraEffect> mEffects = emptyList();

    @GuardedBy("mLock")
    private int mSessionType = SESSION_TYPE_REGULAR;

    @GuardedBy("mLock")
    private @NonNull Range<Integer> mFrameRate = FRAME_RATE_RANGE_UNSPECIFIED;

    // Additional configs to apply onto the UseCases when added to this Camera
    @GuardedBy("mLock")
    private final @NonNull CameraConfig mCameraConfig;

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
    private @Nullable UseCase mPlaceholderForExtensions;
    // Current StreamSharing parent UseCase if exists.
    @GuardedBy("mLock")
    private @Nullable StreamSharing mStreamSharing;

    private final @NonNull CompositionSettings mCompositionSettings;
    private final @NonNull CompositionSettings mSecondaryCompositionSettings;
    private final StreamSharingForceEnabler mStreamSharingForceEnabler =
            new StreamSharingForceEnabler();
    private final StreamSpecsCalculator mStreamSpecsCalculator;

    /**
     * Creates a new {@link CameraUseCaseAdapter} instance.
     *
     * @param camera               The camera that is wrapped.
     * @param cameraCoordinator    Camera coordinator that exposes concurrent camera mode.
     * @param useCaseConfigFactory UseCase config factory that exposes configuration for
     *                             each UseCase.
     */
    @VisibleForTesting
    public CameraUseCaseAdapter(@NonNull CameraInternal camera,
            @NonNull CameraCoordinator cameraCoordinator,
            @NonNull StreamSpecsCalculator streamSpecsCalculator,
            @NonNull UseCaseConfigFactory useCaseConfigFactory) {
        this(camera,
                null,
                new AdapterCameraInfo(camera.getCameraInfoInternal(),
                        CameraConfigs.defaultConfig()),
                null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                cameraCoordinator,
                streamSpecsCalculator,
                useCaseConfigFactory);
    }

    /**
     * Creates a new {@link CameraUseCaseAdapter} instance.
     *
     * @param camera                     The camera that is wrapped.
     * @param secondaryCamera            The secondary camera that is wrapped.
     * @param adapterCameraInfo       The {@link AdapterCameraInfo} that contains the extra
     *                                   information to configure the {@link CameraInternal} when
     *                                   attaching the uses cases of this adapter to the camera.
     * @param secondaryAdapterCameraInfo The {@link AdapterCameraInfo} of secondary camera.
     * @param compositionSettings        The composition settings that will be used to configure the
     *                                   camera.
     * @param secondaryCompositionSettings  The composition settings that will be used to configure
     *                                      the secondary camera.
     * @param cameraCoordinator          Camera coordinator that exposes concurrent camera mode.
     * @param streamSpecsCalculator      A class that calculates the suggested stream specs for
     *                                   a given combination of use cases and other settings.
     * @param useCaseConfigFactory       UseCase config factory that exposes configuration for
     *                                   each UseCase.
     */
    public CameraUseCaseAdapter(
            @NonNull CameraInternal camera,
            @Nullable CameraInternal secondaryCamera,
            @NonNull AdapterCameraInfo adapterCameraInfo,
            @Nullable AdapterCameraInfo secondaryAdapterCameraInfo,
            @NonNull CompositionSettings compositionSettings,
            @NonNull CompositionSettings secondaryCompositionSettings,
            @NonNull CameraCoordinator cameraCoordinator,
            @NonNull StreamSpecsCalculator streamSpecsCalculator,
            @NonNull UseCaseConfigFactory useCaseConfigFactory) {
        mCameraConfig = adapterCameraInfo.getCameraConfig();
        mCameraInternal = new AdapterCameraInternal(camera, adapterCameraInfo);
        if (secondaryCamera != null && secondaryAdapterCameraInfo != null) {
            mSecondaryCameraInternal = new AdapterCameraInternal(secondaryCamera,
                    secondaryAdapterCameraInfo);
        } else {
            mSecondaryCameraInternal = null;
        }
        mCompositionSettings = compositionSettings;
        mSecondaryCompositionSettings = secondaryCompositionSettings;
        mCameraCoordinator = cameraCoordinator;
        mUseCaseConfigFactory = useCaseConfigFactory;
        mId = generateCameraId(adapterCameraInfo, secondaryAdapterCameraInfo);
        mStreamSpecsCalculator = streamSpecsCalculator;
    }

    /**
     * Generate a identifier for the {@link AdapterCameraInfo}.
     */
    public static @NonNull CameraId generateCameraId(
            @NonNull AdapterCameraInfo primaryCameraInfo,
            @Nullable AdapterCameraInfo secondaryCameraInfo) {
        return CameraId.create(
                primaryCameraInfo.getCameraId()
                        + (secondaryCameraInfo == null ? "" : secondaryCameraInfo.getCameraId()),
                primaryCameraInfo.getCameraConfig().getCompatibilityId());
    }

    /**
     * Returns the identifier for this {@link CameraUseCaseAdapter}.
     */
    public @NonNull CameraId getCameraId() {
        return mId;
    }

    /**
     * Returns true if the {@link CameraUseCaseAdapter} is an equivalent camera.
     */
    public boolean isEquivalent(@NonNull CameraUseCaseAdapter cameraUseCaseAdapter) {
        return getCameraId().equals(cameraUseCaseAdapter.getCameraId());
    }

    /**
     * Sets the viewport that will be used for the {@link UseCase} attached to the camera.
     */
    public void setViewPort(@Nullable ViewPort viewPort) {
        synchronized (mLock) {
            mViewPort = viewPort;
        }
    }

    /**
     * Sets the effects that will be used for the {@link UseCase} attached to the camera.
     */
    public void setEffects(@NonNull List<CameraEffect> effects) {
        synchronized (mLock) {
            mEffects = effects;
        }
    }

    /**
     * Gets the {@link ViewPort} that is attached to the camera.
     */
    @Nullable
    public ViewPort getViewPort() {
        synchronized (mLock) {
            return mViewPort;
        }
    }

    /**
     * Gets the {@link CameraEffect}s that are attached to the camera.
     */
    @NonNull
    public List<CameraEffect> getEffects() {
        synchronized (mLock) {
            return mEffects;
        }
    }

    /**
     * Sets the session type.
     */
    public void setSessionType(int sessionType) {
        synchronized (mLock) {
            mSessionType = sessionType;
        }
    }


    /**
     * Gets the session type.
     */
    public int getSessionType() {
        synchronized (mLock) {
            return mSessionType;
        }
    }

    /**
     * Sets the frame rate that will be used for the {@link UseCase} attached to the camera.
     */
    public void setFrameRate(@NonNull Range<Integer> frameRate) {
        synchronized (mLock) {
            mFrameRate = frameRate;
        }
    }


    /**
     * Gets the frame rate.
     */
    @NonNull
    public Range<Integer> getFrameRate() {
        synchronized (mLock) {
            return mFrameRate;
        }
    }

    /**
     * Add the specified collection of {@link UseCase} to the adapter with dual camera support.
     *
     * @throws CameraException Thrown if the combination of newly added UseCases and the
     *                         currently added UseCases exceed the capability of the camera.
     */
    public void addUseCases(@NonNull Collection<UseCase> appUseCasesToAdd) throws CameraException {
        addUseCases(appUseCasesToAdd, /* featureCombination = */ null);
    }

    /**
     * Add the specified collection of {@link UseCase} to the adapter with dual camera support and
     * a feature combination.
     *
     * <p> If a non-null feature combination is set, the features are set to all the use cases, not
     * only the new use cases. This ensures that all use case configs are updated with the correct
     * feature combination-related values and there's no inconsistency among the use cases. On the
     * other hand, a null feature combination represents that the Feature Combination API is not
     * being used and all use cases should be updated accordingly just-in-case they were not unbound
     * or cleaned up properly before. See {@link UseCase#setFeatureCombination(Set)} for details.
     * Note that the new SessionConfig design prevents mixing use cases with and without feature
     * combination, but it's still being handled here to some extent just-in-case.
     *
     * <p> For example, if there were some use cases already added before without a non-null feature
     * combination, they also should be using feature combination configs if the new use cases are
     * being added with a feature combination. Conversely, if the new use cases are being added
     * with a null feature combination, all use cases are reset to use configs without using
     * feature combination.
     *
     * @param appUseCasesToAdd The use cases an application wants to add.
     * @param featureCombination A {@link ResolvedFeatureCombination} to use for all the use cases
     *                           after adding these use cases. A null value represents that the
     *                           feature combination API is not being used.
     * @throws CameraException Thrown if the combination of newly added UseCases and the
     *                         currently added UseCases exceed the capability of the camera, or
     *                         if the frame rate is not supported by the camera.
     */
    @OptIn(markerClass = ExperimentalSessionConfig.class)
    public void addUseCases(@NonNull Collection<UseCase> appUseCasesToAdd,
            @Nullable ResolvedFeatureCombination featureCombination) throws CameraException {
        Logger.d(TAG, "addUseCases: appUseCasesToAdd = " + appUseCasesToAdd
                + ", featureCombination = " + featureCombination);

        synchronized (mLock) {
            // TODO(b/421838573): Check supported frame rate in the binding flow.
            checkFrameRateOrThrow();

            applyCameraConfig();
            Set<UseCase> appUseCases = new LinkedHashSet<>(mAppUseCases);
            appUseCases.addAll(appUseCasesToAdd);

            Map<UseCase, Set<Feature>> previousFeatureComboMap =
                    applyFeatureCombination(appUseCases, featureCombination);

            try {
                applyCalculatedUseCaseChanges(calculateAndValidateUseCases(
                        appUseCases,
                        mSecondaryCameraInternal != null,
                        false
                ));
            } catch (IllegalArgumentException e) {
                restoreFeatureCombination(previousFeatureComboMap);
                throw new CameraException(e);
            }
        }
    }

    /**
     * Simulates to add the specified collection of {@link UseCase} to the adapter with an
     * optional feature combination, performing a dry-run to validate the configuration without
     * applying any changes to the camera's active state.
     *
     * <p>This method performs all necessary calculations and validations to determine if the
     * combination of newly added UseCases and the currently active UseCases is supported by the
     * camera. If the combination is not supported, a [CameraException] will be thrown.
     *
     * @param appUseCasesToAdd The use cases an application wants to add.
     * @param featureCombination A {@link ResolvedFeatureCombination} to use for all the use cases
     *                           after adding these use cases. A null value represents that the
     *                           feature combination API is not being used.
     * @param findMaxSupportedFrameRate whether to find the maximum supported frame rates. If true,
     *                                  the {@link StreamSpecQueryResult#maxSupportedFrameRate} in
     *                                  returned {@link CalculatedUseCaseInfo} will contain the
     *                                  maximum supported frame rate. However, the calculation of
     *                                  {@link StreamSpecQueryResult} will ignore the target
     *                                  frame rate settings in SessionConfig and UseCases. So the
     *                                  flag shouldn't be set for normal flow of adding UseCases.
     * @return the CalculatedUseCaseInfo
     * @throws CameraException Thrown if the combination of newly added UseCases and the
     *                         currently added UseCases exceed the capability of the camera.
     */
    @OptIn(markerClass = ExperimentalSessionConfig.class)
    @NonNull
    public CalculatedUseCaseInfo simulateAddUseCases(
            @NonNull Collection<UseCase> appUseCasesToAdd,
            @Nullable ResolvedFeatureCombination featureCombination,
            boolean findMaxSupportedFrameRate)
            throws CameraException {
        Logger.d(TAG, "simulateAddUseCases: appUseCasesToAdd = " + appUseCasesToAdd
                + ", featureCombination = " + featureCombination);

        synchronized (mLock) {
            // TODO(b/421838573): Check supported frame rate in the binding flow.
            if (!findMaxSupportedFrameRate) {
                checkFrameRateOrThrow();
            }

            applyCameraConfig();
            Set<UseCase> appUseCases = new LinkedHashSet<>(mAppUseCases);
            appUseCases.addAll(appUseCasesToAdd);

            Map<UseCase, Set<Feature>> previousFeatureComboMap =
                    applyFeatureCombination(appUseCases, featureCombination);

            try {
                return calculateAndValidateUseCases(
                        appUseCases,
                        mSecondaryCameraInternal != null,
                        findMaxSupportedFrameRate
                );
            } catch (IllegalArgumentException e) {
                throw new CameraException(e);
            } finally {
                restoreFeatureCombination(previousFeatureComboMap);
            }
        }
    }

    /**
     * Remove the specified collection of {@link UseCase} from the adapter.
     */
    public void removeUseCases(@NonNull Collection<UseCase> useCasesToRemove) {
        synchronized (mLock) {
            clearFeatureCombination(useCasesToRemove);

            Set<UseCase> appUseCases = new LinkedHashSet<>(mAppUseCases);
            appUseCases.removeAll(useCasesToRemove);
            applyCalculatedUseCaseChanges(calculateAndValidateUseCases(
                    appUseCases,
                    mSecondaryCameraInternal != null,
                    /*findMaxSupportedFrameRate=*/false
            ));
        }
    }

    /** Remove all  {@link UseCase} from the adapter. */
    public void removeAllUseCases() {
        synchronized (mLock) {
            removeUseCases(mAppUseCases);
        }
    }

    @GuardedBy("mLock")
    private CalculatedUseCaseInfo calculateAndValidateUseCases(
            @NonNull Collection<UseCase> appUseCases,
            boolean applyStreamSharing,
            boolean findMaxSupportedFrameRate)
            throws IllegalArgumentException {
        checkUnsupportedFeatureCombinationAndThrow(appUseCases);

        // Force enable StreamSharing for Extensions to support VideoCapture. This means that
        // applyStreamSharing is set to true when the use case combination contains
        // VideoCapture and Extensions is enabled.
        if (!applyStreamSharing && shouldForceEnableStreamSharing(appUseCases)) {
            return calculateAndValidateUseCases(appUseCases, /*applyStreamSharing*/true,
                    findMaxSupportedFrameRate);
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
                mCameraConfig.getUseCaseConfigFactory(), mUseCaseConfigFactory,
                mSessionType, mFrameRate);
        StreamSpecQueryResult primaryStreamSpecResult;
        StreamSpecQueryResult secondaryStreamSpecResult = null;

        boolean isFeatureComboInvocation = isFeatureComboInvocation(cameraUseCasesToAttach,
                cameraUseCasesToKeep);

        try {
            primaryStreamSpecResult = mStreamSpecsCalculator.calculateSuggestedStreamSpecs(
                    getCameraMode(),
                    mCameraInternal.getCameraInfoInternal(),
                    /* newUseCases = */ cameraUseCasesToAttach,
                    /* attachedUseCases = */ cameraUseCasesToKeep,
                    mCameraConfig,
                    mSessionType,
                    mFrameRate,
                    isFeatureComboInvocation,
                    findMaxSupportedFrameRate);
            if (mSecondaryCameraInternal != null) {
                secondaryStreamSpecResult = mStreamSpecsCalculator.calculateSuggestedStreamSpecs(
                        getCameraMode(),
                        requireNonNull(mSecondaryCameraInternal).getCameraInfoInternal(),
                        /* newUseCases = */ cameraUseCasesToAttach,
                        /* attachedUseCases = */ cameraUseCasesToKeep,
                        mCameraConfig,
                        mSessionType,
                        mFrameRate,
                        isFeatureComboInvocation,
                        findMaxSupportedFrameRate);
            }
            // TODO(b/265704882): enable stream sharing for LEVEL_3 and high preview
            //  resolution. Throw exception here if (applyStreamSharing == false), both video
            //  and preview are used and preview resolution is lower than user configuration.
        } catch (IllegalArgumentException exception) {
            if (isFeatureComboInvocation) {
                // TODO: b/402297808 - Add StreamSharing support properly for feature combination.
                throw exception;
            }

            // TODO(b/270187871): instead of catch and retry, we can check UseCase
            //  combination directly with #isUseCasesCombinationSupported(). However
            //  calculateSuggestedStreamSpecs() is currently slow. We will do it after it's
            //  optimized
            if (!applyStreamSharing && isStreamSharingAllowed()) {
                // Try again and see if StreamSharing resolves the issue.
                return calculateAndValidateUseCases(appUseCases, /*applyStreamSharing*/true,
                        findMaxSupportedFrameRate);
            } else {
                // If StreamSharing already on or not enabled, throw exception.
                throw exception;
            }
        }

        return new CalculatedUseCaseInfo(
                appUseCases,
                cameraUseCases,
                cameraUseCasesToAttach,
                cameraUseCasesToKeep,
                cameraUseCasesToDetach,
                streamSharing,
                placeholderForExtensions,
                configs,
                primaryStreamSpecResult,
                secondaryStreamSpecResult
        );
    }

    @GuardedBy("mLock")
    private void applyCalculatedUseCaseChanges(@NonNull CalculatedUseCaseInfo info) {
        // Update properties.
        updateViewPortAndSensorToBufferMatrix(
                info.getPrimaryStreamSpecResult().getStreamSpecs(),
                info.getCameraUseCases());
        updateEffects(mEffects, info.getCameraUseCases(), info.getAppUseCases());

        // Detach unused UseCases.
        for (UseCase useCase : info.getCameraUseCasesToDetach()) {
            useCase.unbindFromCamera(mCameraInternal);
        }
        mCameraInternal.detachUseCases(info.getCameraUseCasesToDetach());

        // Detach unused UseCases for secondary camera.
        if (mSecondaryCameraInternal != null) {
            for (UseCase useCase : info.getCameraUseCasesToDetach()) {
                useCase.unbindFromCamera(requireNonNull(mSecondaryCameraInternal));
            }
            requireNonNull(mSecondaryCameraInternal)
                    .detachUseCases(info.getCameraUseCasesToDetach());
        }

        // Update StreamSpec for UseCases to keep.
        if (info.getCameraUseCasesToDetach().isEmpty()) {
            // Only do this if we are not removing UseCase, because updating SessionConfig
            // when removing UseCases may lead to flickering.
            for (UseCase useCase : info.getCameraUseCasesToKeep()) {
                // Assume secondary camera will not have implementation options in dual camera.
                Map<UseCase, StreamSpec> primaryStreamSpecMap =
                        info.getPrimaryStreamSpecResult().getStreamSpecs();
                if (primaryStreamSpecMap.containsKey(useCase)) {
                    StreamSpec newStreamSpec = primaryStreamSpecMap.get(useCase);
                    Config config = requireNonNull(newStreamSpec).getImplementationOptions();
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
        for (UseCase useCase : info.getCameraUseCasesToAttach()) {
            ConfigPair configPair = requireNonNull(info.getUseCaseConfigs().get(useCase));
            if (mSecondaryCameraInternal != null) {
                useCase.bindToCamera(mCameraInternal,
                        requireNonNull(mSecondaryCameraInternal),
                        configPair.mExtendedConfig,
                        configPair.mCameraConfig);
                useCase.updateSuggestedStreamSpec(Preconditions.checkNotNull(
                                info.getPrimaryStreamSpecResult().getStreamSpecs().get(useCase)),
                        Preconditions.checkNotNull(
                                info.getSecondaryStreamSpecResult()).getStreamSpecs().get(useCase));
            } else {
                useCase.bindToCamera(mCameraInternal,
                        null,
                        configPair.mExtendedConfig,
                        configPair.mCameraConfig);
                useCase.updateSuggestedStreamSpec(Preconditions.checkNotNull(
                                info.getPrimaryStreamSpecResult().getStreamSpecs().get(useCase)),
                        null);
            }
        }
        if (mAttached) {
            mCameraInternal.attachUseCases(info.getCameraUseCasesToAttach());
            if (mSecondaryCameraInternal != null) {
                requireNonNull(mSecondaryCameraInternal)
                        .attachUseCases(info.getCameraUseCasesToAttach());
            }
        }

        // Once UseCases are detached/attached, notify the camera.
        for (UseCase useCase : info.getCameraUseCasesToAttach()) {
            useCase.notifyState();
        }

        // The changes are successful. Update the states of this class.
        mAppUseCases.clear();
        mAppUseCases.addAll(info.getAppUseCases());
        mCameraUseCases.clear();
        mCameraUseCases.addAll(info.getCameraUseCases());
        mPlaceholderForExtensions = info.getPlaceholderForExtensions();
        mStreamSharing = info.getStreamSharing();
    }

    private void checkFrameRateOrThrow() throws CameraException {
        if (FRAME_RATE_RANGE_UNSPECIFIED.equals(mFrameRate)) {
            return;
        }

        CameraInfoInternal cameraInfo = mCameraInternal.getCameraInfoInternal();
        Set<Range<Integer>> supportedFrameRates =
                mSessionType == SESSION_TYPE_HIGH_SPEED
                        ? cameraInfo.getSupportedHighSpeedFrameRateRanges()
                        : cameraInfo.getSupportedFrameRateRanges();

        if (!supportedFrameRates.contains(mFrameRate)) {
            throw new CameraException("Frame rate is not supported: " + mFrameRate + " by "
                    + supportedFrameRates);
        }
    }

    private void applyCameraConfig() {
        mCameraInternal.setExtendedConfig(mCameraConfig);
        if (mSecondaryCameraInternal != null) {
            mSecondaryCameraInternal.setExtendedConfig(mCameraConfig);
        }
    }

    @OptIn(markerClass = ExperimentalSessionConfig.class)
    @NonNull
    private static Map<UseCase, Set<Feature>> applyFeatureCombination(
            @NonNull Collection<UseCase> useCases,
            @Nullable ResolvedFeatureCombination featureCombination) {
        Map<UseCase, Set<Feature>> previousFeatureComboMap = new HashMap<>();
        for (UseCase useCase : useCases) {
            previousFeatureComboMap.put(useCase, useCase.getFeatureCombination());
            useCase.setFeatureCombination(
                    featureCombination != null ? featureCombination.getFeatures() : null);
        }
        return previousFeatureComboMap;
    }

    @OptIn(markerClass = ExperimentalSessionConfig.class)
    private static void restoreFeatureCombination(
            Map<UseCase, @Nullable Set<@NonNull Feature>> previousFeatureComboMap) {
        for (Map.Entry<UseCase, Set<Feature>> entry : previousFeatureComboMap.entrySet()) {
            entry.getKey().setFeatureCombination(entry.getValue());
        }
    }

    private static void clearFeatureCombination(@NonNull Collection<UseCase> useCases) {
        for (UseCase useCase : useCases) {
            useCase.setFeatureCombination(null);
        }
    }

    @SafeVarargs
    private static boolean isFeatureComboInvocation(
            @NonNull List<@NonNull UseCase>... useCaseLists) {
        boolean isFeatureComboInvocation = false;

        for (List<UseCase> useCases : useCaseLists) {
            for (UseCase useCase : useCases) {
                if (useCase.getFeatureCombination() != null) {
                    isFeatureComboInvocation = true;
                    break;
                }
            }

            if (isFeatureComboInvocation) {
                break;
            }
        }

        return isFeatureComboInvocation;
    }

    /**
     * Checks if StreamSharing is allowed under the current configuration.
     *
     * <p>StreamSharing is only allowed when the following conditions are met:
     * <ul>
     * <li>When extension is not enabled.</li>
     * <li>When concurrent camera is not enabled.</li>
     * <li>When high-speed session is not enabled.</li>
     * </ul>
     */
    private boolean isStreamSharingAllowed() {
        return !hasExtension()
                && mCameraCoordinator.getCameraOperatingMode()
                != CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT
                && mSessionType != SESSION_TYPE_HIGH_SPEED;
    }

    private boolean shouldForceEnableStreamSharing(@NonNull Collection<UseCase> appUseCases) {
        if (hasExtension() && hasVideoCapture(appUseCases)) {
            return true;
        }

        return mStreamSharingForceEnabler.shouldForceEnableStreamSharing(
                mCameraInternal.getCameraInfoInternal().getCameraId(), appUseCases);
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
        if (requireNonNull(newStreamSpecOptions).listOptions().size()
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
    private @NonNull Set<UseCase> getStreamSharingChildren(@NonNull Collection<UseCase> appUseCases,
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
    private @Nullable StreamSharing createOrReuseStreamSharing(
            @NonNull Collection<UseCase> appUseCases, boolean forceSharingToPreviewAndVideo) {
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
    public @NonNull List<UseCase> getUseCases() {
        synchronized (mLock) {
            return new ArrayList<>(mAppUseCases);
        }
    }

    @VisibleForTesting
    public @NonNull Collection<UseCase> getCameraUseCases() {
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
     * <p>When not in actively resuming mode, it will retry opening camera only when camera
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

        if (!unusedEffects.isEmpty()) {
            Logger.w(TAG, "Unused effects: " + unusedEffects);
        }
    }

    /**
     * Sets effects on the given {@link UseCase} list and returns unused effects.
     */
    private static @NonNull List<CameraEffect> setEffectsOnUseCases(
            @NonNull List<CameraEffect> effects, @NonNull Collection<UseCase> useCases) {
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

    private void updateViewPortAndSensorToBufferMatrix(
            @NonNull Map<UseCase, StreamSpec> suggestedStreamSpecMap,
            @NonNull Collection<UseCase> useCases) {
        synchronized (mLock) {
            if (mViewPort != null && !useCases.isEmpty()) {
                int lensFacing = mCameraInternal.getCameraInfoInternal().getLensFacing();
                boolean isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT;
                // Calculate crop rect if view port is provided.
                Map<UseCase, Rect> cropRectMap = ViewPorts.calculateViewPortRects(
                        mCameraInternal.getCameraInfoInternal().getSensorRect(),
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
                }
            }

            // Regardless of having ViewPort, SensorToBufferTransformMatrix must be set correctly
            // in order for get the correct meteringPoint coordinates.
            for (UseCase useCase : useCases) {
                useCase.setSensorToBufferTransformMatrix(
                        calculateSensorToBufferTransformMatrix(
                                mCameraInternal.getCameraInfoInternal().getSensorRect(),
                                Preconditions.checkNotNull(
                                        suggestedStreamSpecMap.get(useCase)).getResolution()));
            }
        }
    }

    private static @NonNull Matrix calculateSensorToBufferTransformMatrix(
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
    public static class ConfigPair {
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
    static Map<UseCase, ConfigPair> getConfigs(@NonNull Collection<UseCase> useCases,
            @NonNull UseCaseConfigFactory extendedFactory,
            @NonNull UseCaseConfigFactory cameraFactory,
            int sessionType,
            @NonNull Range<Integer> targetFrameRate) {
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
            cameraConfig = attachUseCaseSharedConfigs(useCase, cameraConfig, sessionType,
                    targetFrameRate);
            configs.put(useCase, new ConfigPair(extendedConfig, cameraConfig));
        }
        return configs;
    }

    @NonNull
    private static UseCaseConfig<?> attachUseCaseSharedConfigs(
            @NonNull UseCase useCase,
            @Nullable UseCaseConfig<?> useCaseConfig,
            int sessionType,
            @NonNull Range<Integer> targetFrameRate) {
        MutableOptionsBundle mutableConfig = useCaseConfig != null
                ? MutableOptionsBundle.from(useCaseConfig) : MutableOptionsBundle.create();

        mutableConfig.insertOption(OPTION_SESSION_TYPE, sessionType);
        // In case useCase already contains target frame rate, only set target frame rate when it
        // is not the default value, i.e. FRAME_RATE_RANGE_UNSPECIFIED.
        if (!FRAME_RATE_RANGE_UNSPECIFIED.equals(targetFrameRate)) {
            if (mutableConfig.containsOption(OPTION_TARGET_FRAME_RATE)) {
                Logger.w(TAG, "Override UseCase's target frame rate: "
                        + mutableConfig.removeOption(OPTION_TARGET_FRAME_RATE)
                        + " by " + targetFrameRate);
            }
            mutableConfig.insertOption(OPTION_TARGET_FRAME_RATE, targetFrameRate);
        }

        return useCase.getUseCaseConfigBuilder(mutableConfig).getUseCaseConfig();
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
        if (hasExtension()) {
            if (hasNonSdrConfig(useCases)) {
                throw new IllegalArgumentException("Extensions are only supported for use with "
                        + "standard dynamic range.");
            }

            if (hasRawImageCapture(useCases)) {
                throw new IllegalArgumentException("Extensions are not supported for use with "
                        + "Raw image capture.");
            }
        }

        // TODO(b/322311893): throw exception to block feature combination of effect with Ultra
        //  HDR, until ImageProcessor and SurfaceProcessor can support JPEG/R format.
        synchronized (mLock) {
            if (!mEffects.isEmpty() && (hasUltraHdrImageCapture(useCases)
                    || hasRawImageCapture(useCases))) {
                throw new IllegalArgumentException("Ultra HDR image and Raw capture does not "
                        + "support for use with CameraEffect.");
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

    private static boolean hasRawImageCapture(@NonNull Collection<UseCase> useCases) {
        for (UseCase useCase : useCases) {
            if (!isImageCapture(useCase)) {
                continue;
            }

            UseCaseConfig<?> config = useCase.getCurrentConfig();
            if (config.containsOption(OPTION_OUTPUT_FORMAT)
                    && (checkNotNull(config.retrieveOption(OPTION_OUTPUT_FORMAT))
                    == OUTPUT_FORMAT_RAW)) {
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
        public static @NonNull CameraId create(@NonNull String cameraIdString,
                @NonNull Identifier cameraConfigId) {
            return new AutoValue_CameraUseCaseAdapter_CameraId(cameraIdString, cameraConfigId);
        }

        /** Gets the camera ID string. */
        public abstract @NonNull String getCameraIdString();
        /** Gets the camera configuration. */
        public abstract @NonNull Identifier getCameraConfigId();
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
    @Override
    public @NonNull CameraControl getCameraControl() {
        return mCameraInternal.getCameraControl();
    }

    @Override
    public @NonNull CameraInfo getCameraInfo() {
        return mCameraInternal.getCameraInfo();
    }

    public @Nullable CameraInfo getSecondaryCameraInfo() {
        return mSecondaryCameraInternal != null ? mSecondaryCameraInternal.getCameraInfo() : null;
    }

    @Override
    public @NonNull CameraConfig getExtendedConfig() {
        synchronized (mLock) {
            return mCameraConfig;
        }
    }

    @Override
    public boolean isUseCasesCombinationSupported(boolean withStreamSharing,
            UseCase @NonNull ... useCases) {
        Collection<UseCase> useCasesToVerify = Arrays.asList(useCases);
        if (withStreamSharing) {
            try {
                StreamSharing streamSharing = createOrReuseStreamSharing(useCasesToVerify, true);
                useCasesToVerify = calculateCameraUseCases(useCasesToVerify, null, streamSharing);
            } catch (IllegalArgumentException e) {
                Logger.d(TAG, "Unable to apply StreamSharing", e);
                return false;
            }
        }
        return mCameraInternal.getCameraInfoInternal().isUseCaseCombinationSupported(
                new ArrayList<>(useCasesToVerify), getCameraMode(), false, mCameraConfig);
    }

    /**
     * Calculate the internal created placeholder UseCase for Extensions.
     *
     * @param appUseCases UseCase provided by the app.
     */
    private @Nullable UseCase calculatePlaceholderForExtensions(
            @NonNull Collection<UseCase> appUseCases, @Nullable StreamSharing streamSharing) {
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

    static boolean hasVideoCapture(@NonNull Collection<UseCase> useCases) {
        for (UseCase useCase : useCases) {
            if (isVideoCapture(useCase)) {
                return true;
            }
        }
        return false;
    }

    /** Checks if a [UseCase] is a video capture use case instance. */
    public static boolean isVideoCapture(@Nullable UseCase useCase) {
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
