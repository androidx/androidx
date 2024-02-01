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

package androidx.camera.core;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.CameraConfig;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * The camera interface is used to control the flow of data to use cases, control the
 * camera via the {@link CameraControl}, and publish the state of the camera via {@link CameraInfo}.
 *
 * <p>{@linkplain androidx.camera.lifecycle.ProcessCameraProvider#bindToLifecycle(
 *androidx.lifecycle.LifecycleOwner, CameraSelector, UseCase...) An example} of how to obtain an
 * instance of this class can be found in the {@link androidx.camera.lifecycle} package.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface Camera {

    /**
     * Returns the {@link CameraControl} for the {@link Camera}.
     *
     * <p>The {@link CameraControl} provides various asynchronous operations like zoom, focus and
     * metering. {@link CameraControl} is ready to start operations immediately after use cases
     * are bound to the {@link Camera}. When all {@link UseCase}s are unbound, or when camera is
     * closing or closed because lifecycle onStop happens, the {@link CameraControl} will reject
     * all operations.
     *
     * <p>Each method of {@link CameraControl} returns a {@link ListenableFuture} which apps can
     * use to check the asynchronous result. If the operation is not allowed in current state,
     * the returned {@link ListenableFuture} will fail immediately with
     * {@link CameraControl.OperationCanceledException}.
     */
    @NonNull
    CameraControl getCameraControl();

    /**
     * Returns information about this camera.
     *
     * <p>The returned information can be used to query static camera
     * characteristics or observe the runtime state of the camera.
     *
     * @return the {@link CameraInfo}.
     */
    @NonNull
    CameraInfo getCameraInfo();

    /**
     * Get the currently set extended config of the Camera.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    CameraConfig getExtendedConfig();

    /**
     * Checks whether the use cases combination is supported.
     *
     * @param useCases to be checked whether can be supported.
     * @return whether the use cases combination is supported by the camera.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    default boolean isUseCasesCombinationSupported(@NonNull UseCase... useCases) {
        return isUseCasesCombinationSupported(true, useCases);
    }

    /**
     * Checks whether the use cases combination is supported by camera framework.
     *
     * <p>This method verify whether the given use cases can be supported solely by the surface
     * configurations they require. It doesn't consider the optimization done by CameraX such as
     * {@link androidx.camera.core.streamsharing.StreamSharing}.
     *
     * @param useCases to be checked whether can be supported.
     * @return whether the use cases combination is supported by the camera.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    default boolean isUseCasesCombinationSupportedByFramework(@NonNull UseCase... useCases) {
        return isUseCasesCombinationSupported(false, useCases);
    }

    /**
     * Checks whether the use cases combination is supported.
     *
     * @param withStreamSharing {@code true} if
     * {@link androidx.camera.core.streamsharing.StreamSharing} feature is considered, otherwise
     * {@code false}.
     * @param useCases to be checked whether can be supported.
     * @return whether the use cases combination is supported by the camera.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    default boolean isUseCasesCombinationSupported(boolean withStreamSharing,
            @NonNull UseCase... useCases) {
        return true;
    }
}
