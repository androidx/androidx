/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.camera.core.CameraIdentifier;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.InitializationException;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.internal.StreamSpecsCalculator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * The factory class that creates {@link CameraInternal} instances.
 */
public interface CameraFactory extends CameraPresenceMonitor {

    /**
     * Interface for deferring creation of a CameraFactory.
     */
    interface Provider {
        /**
         * Creates a new, initialized instance of a CameraFactory.
         *
         * @param context                       the android context
         * @param threadConfig                  the thread config to run the camera operations
         * @param availableCamerasLimiter       a CameraSelector used to specify which cameras will
         *                                      be loaded and available to CameraX.
         * @param cameraOpenRetryMaxTimeoutInMs the max timeout for camera open retry.
         * @param streamSpecsCalculator         the {@link StreamSpecsCalculator} instance to use.
         * @param cameraXConfig                 the {@link CameraXConfig} to configure the camera.
         * @return the factory instance
         * @throws InitializationException if it fails to create the factory.
         */
        @SuppressLint("LambdaLast")
        @NonNull
        CameraFactory newInstance(@NonNull Context context,
                @NonNull CameraThreadConfig threadConfig,
                @Nullable CameraSelector availableCamerasLimiter,
                long cameraOpenRetryMaxTimeoutInMs,
                @Nullable CameraXConfig cameraXConfig,
                @NonNull StreamSpecsCalculator streamSpecsCalculator)
                throws InitializationException;
    }

    /**
     * An interface for a CameraFactory that can be interrogated about the results of a
     * potential camera ID list update without committing the change.
     *
     * <p>This is used for validation purposes before applying a state change.
     */
    interface Interrogator {
        /**
         * Calculates the filtered list of available camera IDs that would result from applying
         * the given raw list, without changing the factory's internal state.
         *
         * @param cameraIds The complete, raw list of camera IDs from the hardware.
         * @return The final, filtered list of available camera IDs.
         */
        @NonNull
        List<String> getAvailableCameraIds(@NonNull List<String> cameraIds);
    }

    /**
     * Gets the camera with the associated id.
     *
     * @param cameraId the camera id to get camera with
     * @return the camera object with given camera id
     * @throws CameraUnavailableException if unable to access cameras, perhaps due
     *                                    to insufficient permissions.
     * @throws IllegalArgumentException   if the given camera id is not on the available
     *                                    camera id list.
     */
    @NonNull CameraInternal getCamera(@NonNull String cameraId) throws CameraUnavailableException;

    /**
     * Gets the ids of all available cameras.
     *
     * @return the list of available cameras
     */
    @NonNull Set<String> getAvailableCameraIds();

    /**
     * Gets the {@link CameraCoordinator}.
     *
     * @return the instance of {@link CameraCoordinator}.
     */
    @NonNull CameraCoordinator getCameraCoordinator();

    /**
     * Gets the camera manager instance that is used to access the camera API.
     *
     * <p>Notes that actual type of this camera manager depends on the implementation. While it
     * is CameraManagerCompat in camera2 implementation, it could be some other type in
     * other implementation.
     */
    @Nullable Object getCameraManager();

    /**
     * Gets the observable source for camera availability.
     *
     * <p>This method returns an {@link Observable} that provides the definitive, filtered list of
     * {@link CameraIdentifier}s for cameras that are currently available and ready to be used by
     * CameraX. The list has already been processed by any configured {@link CameraSelector}
     * limiters and compatibility filters.
     *
     * <p>An observer will receive an update whenever this list of available cameras changes. This
     * can occur due to physical events (e.g., a USB camera is connected or disconnected) or
     * changes in system state that affect camera access.
     *
     * <p>Upon adding an observer via {@link Observable#addObserver(Executor, Observable.Observer)},
     * it will be immediately notified with the current list of available cameras. The observer
     * should also handle the {@link Observable.Observer#onError(Throwable)} callback, which may be
     * triggered if the underlying camera system encounters a critical failure.
     *
     * @return A non-null {@link Observable} instance that emits lists of available cameras.
     * @see CameraPresenceProvider
     * @see Observable
     * @see CameraIdentifier
     */
    @NonNull
    Observable<List<CameraIdentifier>> getCameraPresenceSource();

    /**
     * Instructs the CameraFactory to shut down, releasing all its held resources like threads.
     */
    default void shutdown() {}
}
