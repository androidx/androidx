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

import android.graphics.Rect;
import android.util.Log;
import android.util.Size;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.core.UseCase;
import androidx.camera.core.ViewPort;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * A {@link CameraInternal} adapter which checks that the UseCases to make sure that the resolutions
 * and image formats can be supported.
 *
 * <p> The CameraUseCaseAdapter wraps a set of CameraInternals which it can dynamically switch
 * between based on different configurations that are required by the adapter. This is used by
 * extensions in order to select the correct CameraInternal instance which has the required
 * camera id.
 */
public final class CameraUseCaseAdapter {
    private final CameraInternal mCameraInternal;
    private final LinkedHashSet<CameraInternal> mCameraInternals;
    private final CameraDeviceSurfaceManager mCameraDeviceSurfaceManager;

    private static final String TAG = "CameraUseCaseAdapter";

    private final CameraId mId;

    @GuardedBy("mLock")
    private final List<UseCase> mUseCases = new ArrayList<>();

    @GuardedBy("mLock")
    @Nullable
    private ViewPort mViewPort;

    private final Object mLock = new Object();

    // This indicates whether or not the UseCases that have been added to this adapter has
    // actually been attached to the CameraInternal instance.
    @GuardedBy("mLock")
    private boolean mAttached = true;

    /**
     * Create a new {@link CameraUseCaseAdapter} instance.
     *
     * @param cameraInternal             the actual camera implementation that is current attached
     * @param cameras                    the set of cameras that are wrapped
     * @param cameraDeviceSurfaceManager A class that checks for whether a specific camera
     *                                   can support the set of Surface with set resolutions.
     */
    public CameraUseCaseAdapter(@NonNull CameraInternal cameraInternal,
            @NonNull LinkedHashSet<CameraInternal> cameras,
            @NonNull CameraDeviceSurfaceManager cameraDeviceSurfaceManager) {
        mCameraInternal = cameraInternal;
        mCameraInternals = new LinkedHashSet<>(cameras);
        mId = new CameraId(mCameraInternals);
        mCameraDeviceSurfaceManager = cameraDeviceSurfaceManager;
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
     * Check to see if the set of {@link UseCase} can be attached to the camera.
     *
     * <p> This does not take into account UseCases which are already attached to the camera.
     */
    public void checkAttachUseCases(@NonNull List<UseCase> useCases) throws CameraException {
        // Only do resolution calculation if UseCases were bound
        if (!UseCaseOccupancy.checkUseCaseLimitNotExceeded(useCases)) {
            throw new CameraException("Attempting to bind too many ImageCapture or "
                    + "VideoCapture instances");
        }

        // If the UseCases exceed the resolutions then it will throw an exception
        try {
            calculateSuggestedResolutions(useCases, Collections.emptyList());
        } catch (IllegalArgumentException e) {
            throw new CameraException(e.getMessage());
        }
    }

    /**
     * Add the specified collection of {@link UseCase} to the adapter.
     *
     * @throws CameraException Thrown if the combination of newly added UseCases and the
     *                         currently added UseCases exceed the capability of the camera.
     */
    @UseExperimental(markerClass = androidx.camera.core.ExperimentalUseCaseGroup.class)
    public void addUseCases(@NonNull Collection<UseCase> useCases) throws CameraException {
        synchronized (mLock) {
            List<UseCase> useCaseListAfterUpdate = new ArrayList<>(mUseCases);
            List<UseCase> newUseCases = new ArrayList<>();

            for (UseCase useCase : useCases) {
                if (mUseCases.contains(useCase)) {
                    Log.e(TAG, "Attempting to attach already attached UseCase");
                } else {
                    useCaseListAfterUpdate.add(useCase);
                    newUseCases.add(useCase);
                }
            }

            // Only do resolution calculation if UseCases were bound
            if (!UseCaseOccupancy.checkUseCaseLimitNotExceeded(useCaseListAfterUpdate)) {
                throw new CameraException("Attempting to bind too many ImageCapture or "
                        + "VideoCapture instances");
            }

            Map<UseCase, Size> suggestedResolutionsMap;
            try {
                suggestedResolutionsMap =
                        calculateSuggestedResolutions(newUseCases, mUseCases);
            } catch (IllegalArgumentException e) {
                throw new CameraException(e.getMessage());
            }

            if (mViewPort != null) {
                // Calculate crop rect if view port is provided.
                Map<UseCase, Rect> cropRectMap = ViewPorts.calculateViewPortRects(
                        mCameraInternal.getCameraControlInternal().getSensorRect(),
                        mViewPort.getAspectRatio(),
                        mCameraInternal.getCameraInfoInternal().getSensorRotationDegrees(
                                mViewPort.getRotation()),
                        mViewPort.getScaleType(),
                        mViewPort.getLayoutDirection(),
                        suggestedResolutionsMap);
                for (UseCase useCase : useCases) {
                    useCase.setViewPortCropRect(cropRectMap.get(useCase));
                }
            }

            // At this point the binding will succeed since all the calculations are done
            // Do all attaching related work
            for (UseCase useCase : newUseCases) {
                useCase.onAttach(mCameraInternal);
                useCase.updateSuggestedResolution(
                        Preconditions.checkNotNull(suggestedResolutionsMap.get(useCase)));
            }

            mUseCases.addAll(newUseCases);
            if (mAttached) {
                mCameraInternal.attachUseCases(newUseCases);
            }

            // Once all use cases are attached, they need to notify the CameraInternal of its state
            for (UseCase useCase : newUseCases) {
                useCase.notifyState();
            }
        }
    }

    /**
     * Remove the specified collection of {@link UseCase} from the adapter.
     */
    public void removeUseCases(@NonNull Collection<UseCase> useCases) {
        synchronized (mLock) {
            mCameraInternal.detachUseCases(useCases);

            for (UseCase useCase : useCases) {
                if (mUseCases.contains(useCase)) {
                    useCase.onDetach(mCameraInternal);
                    useCase.onDestroy();
                } else {
                    Log.e(TAG, "Attempting to detach non-attached UseCase: " + useCase);
                }
            }

            mUseCases.removeAll(useCases);
        }
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

    /**
     * Attach the UseCases to the {@link CameraInternal} camera so that the UseCases can receive
     * data if they are active.
     *
     * <p> This will start the underlying {@link CameraInternal} instance.
     */
    public void attachUseCases() {
        synchronized (mLock) {
            if (!mAttached) {
                mCameraInternal.attachUseCases(mUseCases);
                mAttached = true;
            }
        }
    }

    /**
     * Detach the UseCases from the {@link CameraInternal} so that the UseCases stop receiving data.
     *
     * <p> This will stop the underlying {@link CameraInternal} instance.
     */
    public void detachUseCases() {
        synchronized (mLock) {
            if (mAttached) {
                mCameraInternal.detachUseCases(new ArrayList<>(mUseCases));
                mAttached = false;
            }
        }
    }

    private Map<UseCase, Size> calculateSuggestedResolutions(@NonNull List<UseCase> newUseCases,
            @NonNull List<UseCase> currentUseCases) {
        List<SurfaceConfig> existingSurfaces = new ArrayList<>();
        String cameraId = mCameraInternal.getCameraInfoInternal().getCameraId();

        Map<UseCaseConfig<?>, UseCase> configToUseCaseMap = new HashMap<>();

        for (UseCase useCase : currentUseCases) {
            SurfaceConfig surfaceConfig =
                    mCameraDeviceSurfaceManager.transformSurfaceConfig(cameraId,
                            useCase.getImageFormat(),
                            useCase.getAttachedSurfaceResolution());
            existingSurfaces.add(surfaceConfig);
        }

        for (UseCase useCase : newUseCases) {
            UseCaseConfig.Builder<?, ?, ?> defaultBuilder = useCase.getDefaultBuilder(
                    mCameraInternal.getCameraInfoInternal());

            // Combine with default configuration.
            UseCaseConfig<?> combinedUseCaseConfig =
                    useCase.applyDefaults(useCase.getUseCaseConfig(),
                            defaultBuilder);
            configToUseCaseMap.put(combinedUseCaseConfig, useCase);
        }

        // Get suggested resolutions and update the use case session configuration
        Map<UseCaseConfig<?>, Size> useCaseConfigSizeMap = mCameraDeviceSurfaceManager
                .getSuggestedResolutions(cameraId, existingSurfaces,
                        new ArrayList<>(configToUseCaseMap.keySet()));

        Map<UseCase, Size> suggestedResolutions = new HashMap<>();
        for (Map.Entry<UseCaseConfig<?>, UseCase> entry : configToUseCaseMap.entrySet()) {
            suggestedResolutions.put(entry.getValue(), useCaseConfigSizeMap.get(entry.getKey()));
        }

        return suggestedResolutions;
    }

    @NonNull
    public CameraInfoInternal getCameraInfoInternal() {
        return mCameraInternal.getCameraInfoInternal();
    }

    @NonNull
    public CameraControlInternal getCameraControlInternal() {
        return mCameraInternal.getCameraControlInternal();
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
}
