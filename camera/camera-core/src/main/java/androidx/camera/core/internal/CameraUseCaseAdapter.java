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

import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.ViewPort;
import androidx.camera.core.impl.AttachedSurfaceInfo;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraConfigs;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.processing.SurfaceProcessorWithExecutor;
import androidx.camera.core.streamsharing.StreamSharing;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link CameraInternal} adapter which checks that the UseCases to make sure that the resolutions
 * and image formats can be supported.
 *
 * <p> The CameraUseCaseAdapter wraps a set of CameraInternals which it can dynamically switch
 * between based on different configurations that are required by the adapter. This is used by
 * extensions in order to select the correct CameraInternal instance which has the required
 * camera id.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CameraUseCaseAdapter implements Camera {
    @NonNull
    private final CameraInternal mCameraInternal;
    private final LinkedHashSet<CameraInternal> mCameraInternals;
    private final CameraDeviceSurfaceManager mCameraDeviceSurfaceManager;
    private final UseCaseConfigFactory mUseCaseConfigFactory;

    private static final String TAG = "CameraUseCaseAdapter";
    private boolean mStreamSharingEnabled = false;

    private final CameraId mId;

    // UseCases from the app. This does not include internal UseCases created by CameraX.
    @GuardedBy("mLock")
    private final List<UseCase> mUseCases = new ArrayList<>();
    // UseCases sent to the camera including internal UseCases created by CameraX.
    @GuardedBy("mLock")
    private final List<UseCase> mCameraUseCases = new ArrayList<>();

    @GuardedBy("mLock")
    @Nullable
    private ViewPort mViewPort;

    @GuardedBy("mLock")
    @NonNull
    private List<CameraEffect> mEffects = emptyList();

    // Additional configs to apply onto the UseCases when added to this Camera
    @GuardedBy("mLock")
    @NonNull
    private CameraConfig mCameraConfig = CameraConfigs.emptyConfig();

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

    /**
     * Create a new {@link CameraUseCaseAdapter} instance.
     *
     * @param cameras                    the set of cameras that are wrapped, with them in order
     *                                   of preference. The actual camera used will be dependent
     *                                   on configs set by
     *                                   {@link #setExtendedConfig(CameraConfig)} which can
     *                                   filter out specific camera instances
     * @param cameraDeviceSurfaceManager A class that checks for whether a specific camera
     *                                   can support the set of Surface with set resolutions.
     */
    public CameraUseCaseAdapter(@NonNull LinkedHashSet<CameraInternal> cameras,
            @NonNull CameraDeviceSurfaceManager cameraDeviceSurfaceManager,
            @NonNull UseCaseConfigFactory useCaseConfigFactory) {
        mCameraInternal = cameras.iterator().next();
        mCameraInternals = new LinkedHashSet<>(cameras);
        mId = new CameraId(mCameraInternals);
        mCameraDeviceSurfaceManager = cameraDeviceSurfaceManager;
        mUseCaseConfigFactory = useCaseConfigFactory;
    }

    /**
     * Generate a identifier for the set of {@link CameraInternal}.
     */
    @NonNull
    public static CameraId generateCameraId(@NonNull LinkedHashSet<CameraInternal> cameras) {
        return new CameraId(cameras);
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
        return mId.equals(cameraUseCaseAdapter.getCameraId());
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
     * Add the specified collection of {@link UseCase} to the adapter.
     *
     * @throws CameraException Thrown if the combination of newly added UseCases and the
     *                         currently added UseCases exceed the capability of the camera.
     */
    public void addUseCases(@NonNull Collection<UseCase> appUseCasesToAdd) throws CameraException {
        synchronized (mLock) {
            List<UseCase> appUseCasesAfter = new ArrayList<>(mUseCases);
            // Removing new from existing to avoid duplicate UseCases.
            appUseCasesAfter.removeAll(appUseCasesToAdd);
            appUseCasesAfter.addAll(appUseCasesToAdd);
            try {
                updateUseCases(appUseCasesAfter);
            } catch (IllegalArgumentException e) {
                throw new CameraException(e.getMessage());
            }
        }
    }

    /**
     * Remove the specified collection of {@link UseCase} from the adapter.
     */
    public void removeUseCases(@NonNull Collection<UseCase> useCasesToRemove) {
        synchronized (mLock) {
            List<UseCase> appUseCasesAfter = new ArrayList<>(mUseCases);
            appUseCasesAfter.removeAll(useCasesToRemove);
            updateUseCases(appUseCasesAfter);
        }
    }

    /**
     * Updates the states based the new app UseCases.
     */
    void updateUseCases(@NonNull Collection<UseCase> appUseCases) {
        // TODO(b/265820449): set applyStreamSharing to true if Effects requires it..
        updateUseCases(appUseCases, /*applyStreamSharing*/false);
    }

    /**
     * Updates the states based the new app UseCases.
     *
     * <p> This method calculates the new camera UseCases based on the input and the current state,
     * attach/detach the camera UseCases, and save the updated state in following member variables:
     * {@link #mCameraUseCases}, {@link #mUseCases} and {@link #mPlaceholderForExtensions}.
     *
     * @throws IllegalArgumentException if the UseCase combination is not supported. In that case,
     *                                  it will not update the internal states.
     */
    void updateUseCases(@NonNull Collection<UseCase> appUseCases, boolean applyStreamSharing) {
        synchronized (mLock) {
            // Calculate camera UseCases and keep the result in local variables in case they don't
            // meet the stream combination rules.
            UseCase placeholderForExtensions = calculatePlaceholderForExtensions(appUseCases);
            StreamSharing streamSharing = applyStreamSharing
                    ? createOrReuseStreamSharing(appUseCases) : null;
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

            Map<UseCase, StreamSpec> suggestedStreamSpecMap;
            try {
                suggestedStreamSpecMap = calculateSuggestedStreamSpecs(
                        mCameraInternal.getCameraInfoInternal(), cameraUseCasesToAttach,
                        cameraUseCasesToKeep, configs);
                // TODO(b/265704882): enable stream sharing for LEVEL_3 and high preview
                //  resolution. Throw exception here if (applyStreamSharing == false), both video
                //  and preview are used and preview resolution is lower than user configuration.
            } catch (IllegalArgumentException exception) {
                if (!applyStreamSharing && mStreamSharingEnabled && hasNoExtension()) {
                    // Try again and see if StreamSharing resolves the issue.
                    updateUseCases(appUseCases, /*applyStreamSharing*/true);
                    return;
                } else {
                    // If StreamSharing already on or not enabled, throw exception.
                    throw exception;
                }
            }

            // Update properties.
            updateViewPort(suggestedStreamSpecMap, cameraUseCases);
            updateEffects(mEffects, appUseCases);

            // Detach unused UseCases.
            for (UseCase useCase : cameraUseCasesToDetach) {
                useCase.unbindFromCamera(mCameraInternal);
            }
            mCameraInternal.detachUseCases(cameraUseCasesToDetach);

            // Attach new UseCases.
            for (UseCase useCase : cameraUseCasesToAttach) {
                ConfigPair configPair = requireNonNull(configs.get(useCase));
                useCase.setHasCameraTransform(true);
                useCase.bindToCamera(mCameraInternal, configPair.mExtendedConfig,
                        configPair.mCameraConfig);
                useCase.updateSuggestedStreamSpec(
                        Preconditions.checkNotNull(suggestedStreamSpecMap.get(useCase)));
            }
            if (mAttached) {
                mCameraInternal.attachUseCases(cameraUseCasesToAttach);
            }

            // Once UseCases are detached/attached, notify the camera.
            for (UseCase useCase : cameraUseCasesToAttach) {
                useCase.notifyState();
            }

            // The changes are successful. Update the states of this class.
            mUseCases.clear();
            mUseCases.addAll(appUseCases);
            mCameraUseCases.clear();
            mCameraUseCases.addAll(cameraUseCases);
            mPlaceholderForExtensions = placeholderForExtensions;
            mStreamSharing = streamSharing;
        }
    }

    private boolean hasNoExtension() {
        synchronized (mLock) {
            return mCameraConfig == CameraConfigs.emptyConfig();
        }
    }

    /**
     * Returns {@link UseCase}s qualified for {@link StreamSharing}.
     */
    @NonNull
    private Set<UseCase> getStreamSharingChildren(@NonNull Collection<UseCase> appUseCases) {
        Set<UseCase> useCases = new HashSet<>();
        for (UseCase useCase : appUseCases) {
            checkArgument(!isStreamSharing(useCase), "Only support one level of sharing for now.");
            if (isPrivateInputFormat(useCase)) {
                // Add UseCase if the input format is PRIVATE(Preview and VideoCapture).
                useCases.add(useCase);
            }
        }
        return useCases;
    }

    /**
     * Creates a new {@link StreamSharing} or returns the existing one.
     *
     * <p> Returns the existing {@link StreamSharing} if the children have not changed.
     * Otherwise, create a new {@link StreamSharing} and return.
     *
     * <p> Currently, only {@link UseCase} with {@link ImageFormat#PRIVATE} can be
     * {@link StreamSharing} children({@link Preview} and VideoCapture).
     */
    @Nullable
    private StreamSharing createOrReuseStreamSharing(@NonNull Collection<UseCase> appUseCases) {
        synchronized (mLock) {
            Set<UseCase> newChildren = getStreamSharingChildren(appUseCases);
            if (newChildren.size() < 2) {
                // No need to share the stream for 1 or less children.
                return null;
            }
            if (mStreamSharing != null && mStreamSharing.getChildren().equals(newChildren)) {
                // Returns the current instance if the new children equals the old.
                return requireNonNull(mStreamSharing);
            }
            return new StreamSharing(mCameraInternal, newChildren, mUseCaseConfigFactory);
        }
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
            return new ArrayList<>(mUseCases);
        }
    }

    @VisibleForTesting
    @NonNull
    Collection<UseCase> getCameraUseCases() {
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
                mCameraInternal.attachUseCases(mUseCases);
                restoreInteropConfig();

                // Notify to update the use case's active state because it may be cleared if the
                // use case was ever detached from a camera previously.
                for (UseCase useCase : mUseCases) {
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
                mCameraInternal.detachUseCases(new ArrayList<>(mUseCases));
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
            @NonNull CameraInfoInternal cameraInfoInternal,
            @NonNull Collection<UseCase> newUseCases,
            @NonNull Collection<UseCase> currentUseCases,
            @NonNull Map<UseCase, ConfigPair> configPairMap) {
        List<AttachedSurfaceInfo> existingSurfaces = new ArrayList<>();
        String cameraId = cameraInfoInternal.getCameraId();
        Map<UseCase, StreamSpec> suggestedStreamSpecs = new HashMap<>();

        // Get resolution for current use cases.
        for (UseCase useCase : currentUseCases) {
            SurfaceConfig surfaceConfig =
                    mCameraDeviceSurfaceManager.transformSurfaceConfig(cameraId,
                            useCase.getImageFormat(),
                            useCase.getAttachedSurfaceResolution());
            existingSurfaces.add(AttachedSurfaceInfo.create(surfaceConfig,
                    useCase.getImageFormat(), useCase.getAttachedSurfaceResolution(),
                    useCase.getCurrentConfig().getTargetFramerate(null)));
            suggestedStreamSpecs.put(useCase, useCase.getAttachedStreamSpec());
        }

        // Calculate resolution for new use cases.
        if (!newUseCases.isEmpty()) {
            Map<UseCaseConfig<?>, UseCase> configToUseCaseMap = new HashMap<>();
            for (UseCase useCase : newUseCases) {
                ConfigPair configPair = configPairMap.get(useCase);
                // Combine with default configuration.
                UseCaseConfig<?> combinedUseCaseConfig =
                        useCase.mergeConfigs(cameraInfoInternal, configPair.mExtendedConfig,
                                configPair.mCameraConfig);
                configToUseCaseMap.put(combinedUseCaseConfig, useCase);
            }

            // Get suggested stream specifications and update the use case session configuration
            Map<UseCaseConfig<?>, StreamSpec> useCaseConfigStreamSpecMap =
                    mCameraDeviceSurfaceManager.getSuggestedStreamSpecs(cameraId, existingSurfaces,
                            new ArrayList<>(configToUseCaseMap.keySet()));

            for (Map.Entry<UseCaseConfig<?>, UseCase> entry : configToUseCaseMap.entrySet()) {
                suggestedStreamSpecs.put(entry.getValue(),
                        useCaseConfigStreamSpecMap.get(entry.getKey()));
            }
        }
        return suggestedStreamSpecs;
    }

    @VisibleForTesting
    static void updateEffects(@NonNull List<CameraEffect> effects,
            @NonNull Collection<UseCase> useCases) {
        Map<Integer, CameraEffect> effectsByTargets = new HashMap<>();
        for (CameraEffect effect : effects) {
            effectsByTargets.put(effect.getTargets(), effect);
        }

        // Set effects on the UseCases. This also removes existing effects if necessary.
        for (UseCase useCase : useCases) {
            if (useCase instanceof Preview) {
                Preview preview = ((Preview) useCase);
                CameraEffect effect = effectsByTargets.get(CameraEffect.PREVIEW);
                if (effect == null) {
                    preview.setProcessor(null);
                    continue;
                }
                preview.setProcessor(new SurfaceProcessorWithExecutor(
                        requireNonNull(effect.getSurfaceProcessor()),
                        effect.getExecutor()));
            } else if (useCase instanceof ImageCapture) {
                ImageCapture imageCapture = ((ImageCapture) useCase);
                imageCapture.setEffect(effectsByTargets.get(CameraEffect.IMAGE_CAPTURE));
            }
        }
    }

    private void updateViewPort(@NonNull Map<UseCase, StreamSpec> suggestedStreamSpecMap,
            @NonNull Collection<UseCase> useCases) {
        synchronized (mLock) {
            if (mViewPort != null) {
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

    // Get a map of the configs for the use cases from the respective factories
    private Map<UseCase, ConfigPair> getConfigs(Collection<UseCase> useCases,
            UseCaseConfigFactory extendedFactory, UseCaseConfigFactory cameraFactory) {
        Map<UseCase, ConfigPair> configs = new HashMap<>();
        for (UseCase useCase : useCases) {
            configs.put(useCase, new ConfigPair(useCase.getDefaultConfig(false, extendedFactory),
                    useCase.getDefaultConfig(true, cameraFactory)));
        }
        return configs;
    }

    /**
     * An identifier for a {@link CameraUseCaseAdapter}.
     *
     * <p>This identifies the actual camera instances that are wrapped by the
     * CameraUseCaseAdapter and is used to determine if 2 different instances of
     * CameraUseCaseAdapter are actually equivalent.
     */
    public static final class CameraId {
        private final List<String> mIds;

        CameraId(LinkedHashSet<CameraInternal> cameraInternals) {
            mIds = new ArrayList<>();
            for (CameraInternal cameraInternal : cameraInternals) {
                mIds.add(cameraInternal.getCameraInfoInternal().getCameraId());
            }
        }

        @Override
        public boolean equals(Object cameraId) {
            if (cameraId instanceof CameraId) {
                return mIds.equals(((CameraId) cameraId).mIds);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 53 * mIds.hashCode();
        }
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
        return mCameraInternal.getCameraControlInternal();
    }

    @NonNull
    @Override
    public CameraInfo getCameraInfo() {
        return mCameraInternal.getCameraInfoInternal();
    }

    @NonNull
    @Override
    public LinkedHashSet<CameraInternal> getCameraInternals() {
        return mCameraInternals;
    }

    @NonNull
    @Override
    public CameraConfig getExtendedConfig() {
        synchronized (mLock) {
            return mCameraConfig;
        }
    }

    @Override
    public void setExtendedConfig(@Nullable CameraConfig cameraConfig) {
        synchronized (mLock) {
            if (cameraConfig == null) {
                cameraConfig = CameraConfigs.emptyConfig();
            }

            if (!mUseCases.isEmpty() && !mCameraConfig.getCompatibilityId().equals(
                    cameraConfig.getCompatibilityId())) {
                throw new IllegalStateException(
                        "Need to unbind all use cases before binding with extension enabled");
            }

            mCameraConfig = cameraConfig;

            //Configure the CameraInternal as well so that it can get SessionProcessor.
            mCameraInternal.setExtendedConfig(mCameraConfig);
        }
    }

    @Override
    public boolean isUseCasesCombinationSupported(@NonNull UseCase... useCases) {
        synchronized (mLock) {
            // If the UseCases exceed the resolutions then it will throw an exception
            try {
                Map<UseCase, ConfigPair> configs = getConfigs(Arrays.asList(useCases),
                        mCameraConfig.getUseCaseConfigFactory(), mUseCaseConfigFactory);
                calculateSuggestedStreamSpecs(mCameraInternal.getCameraInfoInternal(),
                        Arrays.asList(useCases), emptyList(), configs);
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
    UseCase calculatePlaceholderForExtensions(@NonNull Collection<UseCase> appUseCases) {
        synchronized (mLock) {
            UseCase placeholder = null;
            if (isCoexistingPreviewImageCaptureRequired()) {
                if (isExtraPreviewRequired(appUseCases)) {
                    if (isPreview(mPlaceholderForExtensions)) {
                        placeholder = mPlaceholderForExtensions;
                    } else {
                        placeholder = createExtraPreview();
                    }
                } else if (isExtraImageCaptureRequired(appUseCases)) {
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
     * contain an {@link Preview}.
     */
    private boolean isExtraPreviewRequired(@NonNull Collection<UseCase> useCases) {
        boolean hasPreview = false;
        boolean hasImageCapture = false;

        for (UseCase useCase : useCases) {
            if (isPreview(useCase)) {
                hasPreview = true;
            } else if (isImageCapture(useCase)) {
                hasImageCapture = true;
            }
        }

        return hasImageCapture && !hasPreview;
    }

    /**
     * Returns true if the input use case list contains a {@link Preview} but does not contain an
     * {@link ImageCapture}.
     */
    private boolean isExtraImageCaptureRequired(@NonNull Collection<UseCase> useCases) {
        boolean hasPreview = false;
        boolean hasImageCapture = false;

        for (UseCase useCase : useCases) {
            if (isPreview(useCase)) {
                hasPreview = true;
            } else if (isImageCapture(useCase)) {
                hasImageCapture = true;
            }
        }

        return hasPreview && !hasImageCapture;
    }

    private static boolean isStreamSharing(@Nullable UseCase useCase) {
        return useCase instanceof StreamSharing;
    }

    private static boolean isPreview(@Nullable UseCase useCase) {
        return useCase instanceof Preview;
    }

    private static boolean isImageCapture(@Nullable UseCase useCase) {
        return useCase instanceof ImageCapture;
    }

    private boolean isPrivateInputFormat(@NonNull UseCase useCase) {
        UseCaseConfig<?> mergedConfig = useCase.mergeConfigs(
                mCameraInternal.getCameraInfoInternal(), null,
                useCase.getDefaultConfig(true, mUseCaseConfigFactory));
        return mergedConfig.getInputFormat() == INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
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

    @VisibleForTesting
    void setStreamSharingEnabled(boolean streamSharingEnabled) {
        mStreamSharingEnabled = streamSharingEnabled;
    }
}
