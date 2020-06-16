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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link CameraInternal} adapter which checks that the UseCases to make sure that the resolutions
 * and image formats can be supported.
 */
public final class CameraUseCaseAdapter {
    private final CameraInternal mCameraInternal;
    private final CameraDeviceSurfaceManager mCameraDeviceSurfaceManager;

    private static final String TAG = "CameraUseCaseAdapter";

    @GuardedBy("mLock")
    private final List<UseCase> mAttachedUseCases = new ArrayList<>();

    @GuardedBy("mLock")
    @Nullable
    private ViewPort mViewPort;

    private final Object mLock = new Object();

    /**
     * Create a new {@link CameraUseCaseAdapter} instance.
     *
     * @param cameraInternal the actual camera implementation that is wrapped
     * @param cameraDeviceSurfaceManager A class that checks for whether a specific camera
     *                                   can support the set of Surface with set resolutions.
     */
    public CameraUseCaseAdapter(@NonNull CameraInternal cameraInternal,
            @NonNull CameraDeviceSurfaceManager cameraDeviceSurfaceManager) {
        mCameraInternal = cameraInternal;
        mCameraDeviceSurfaceManager = cameraDeviceSurfaceManager;
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
     *
     * @throws CameraException
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
     * Attach the specified collection of {@link UseCase} to the camera.
     *
     * @throws CameraException Thrown if the combination of newly attached UseCases and the
     * currently attached UseCases exceed the capability of the camera.
     */
    @UseExperimental(markerClass = androidx.camera.core.ExperimentalUseCaseGroup.class)
    public void attachUseCases(@NonNull Collection<UseCase> useCases) throws CameraException {
        synchronized (mLock) {
            List<UseCase> useCaseListAfterUpdate = new ArrayList<>(mAttachedUseCases);
            List<UseCase> newUseCases = new ArrayList<>();

            for (UseCase useCase : useCases) {
                if (mAttachedUseCases.contains(useCase)) {
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
                        calculateSuggestedResolutions(newUseCases, mAttachedUseCases);
            }  catch (IllegalArgumentException e) {
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
                useCase.updateSuggestedResolution(suggestedResolutionsMap.get(useCase));
            }

            mAttachedUseCases.addAll(newUseCases);
            mCameraInternal.attachUseCases(newUseCases);
        }
    }

    /**
     * Detached the specified collection of {@link UseCase} from the camera.
     */
    public void detachUseCases(@NonNull Collection<UseCase> useCases) {
        synchronized (mLock) {
            mCameraInternal.detachUseCases(useCases);

            for (UseCase useCase : useCases) {
                if (mAttachedUseCases.contains(useCase)) {
                    useCase.onDetach(mCameraInternal);
                    useCase.onDestroy();
                } else {
                    Log.e(TAG, "Attempting to detach non-attached UseCase: " + useCase);
                }
            }

            mAttachedUseCases.removeAll(useCases);
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

    /**
     * Get the {@link CameraInternal} instance that is wrapped by this {@link CameraUseCaseAdapter}.
     */
    @NonNull
    public CameraInternal getCameraInternal() {
        return mCameraInternal;
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
