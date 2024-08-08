/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.core;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * A {@link CameraProvider} provides basic access to a set of cameras such as querying for camera
 * existence or information.
 *
 * <p>A device might have multiple cameras. According to the applications' design, they might
 * need to search for a suitable camera which supports their functions. A {@link CameraProvider}
 * allows the applications to check whether any camera exists to fulfill the requirements or to
 * get {@link CameraInfo} instances of all cameras to retrieve the camera information.
 */
public interface CameraProvider {

    /**
     * Checks whether this provider supports at least one camera that meets the requirements from a
     * {@link CameraSelector}.
     *
     * <p>If this method returns {@code true}, then the camera selector can be used to bind
     * use cases and retrieve a {@link Camera} instance.
     *
     * @param cameraSelector the {@link CameraSelector} that filters available cameras.
     * @return true if the device has at least one available camera, otherwise false.
     * @throws CameraInfoUnavailableException if unable to access cameras, perhaps due to
     *                                        insufficient permissions.
     */
    boolean hasCamera(@NonNull CameraSelector cameraSelector) throws CameraInfoUnavailableException;

    /**
     * Returns {@link CameraInfo} instances of the available cameras.
     *
     * <p>While iterating through all the available {@link CameraInfo}, if one of them meets some
     * predefined requirements, a {@link CameraSelector} that uniquely identifies its camera
     * can be retrieved using {@link CameraInfo#getCameraSelector()}, which can then be used to bind
     * {@linkplain UseCase use cases} to that camera.
     *
     * @return A list of {@link CameraInfo} instances for the available cameras.
     */
    @NonNull
    List<CameraInfo> getAvailableCameraInfos();

    /**
     * Returns the {@link CameraInfo} instance of the camera resulted from the specified
     * {@link CameraSelector}.
     *
     * <p>The returned {@link CameraInfo} corresponds to the camera that will be bound when calling
     * {@code bindToLifecycle} with the specified {@link CameraSelector}.
     *
     * @param cameraSelector the {@link CameraSelector} to use for selecting the camera to receive
     * information about.
     * @return the corresponding {@link CameraInfo}.
     * @throws IllegalArgumentException if the given {@link CameraSelector} can't result in a
     * valid camera to provide the {@link CameraInfo}.
     */
    @ExperimentalCameraInfo
    @NonNull
    default CameraInfo getCameraInfo(@NonNull CameraSelector cameraSelector) {
        throw new UnsupportedOperationException("The camera provider is not implemented properly.");
    }
}
