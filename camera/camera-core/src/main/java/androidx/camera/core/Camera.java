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
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraInternal;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.LinkedHashSet;

/**
 * The camera interface is used to control the flow of data to use cases, control the
 * camera via the {@link CameraControl}, and publish the state of the camera via {@link CameraInfo}.
 *
 * <p>{@linkplain androidx.camera.lifecycle.ProcessCameraProvider#bindToLifecycle(
 *androidx.lifecycle.LifecycleOwner, CameraSelector, UseCase...) An example} of how to obtain an
 * instance of this class can be found in the {@link androidx.camera.lifecycle} package.
 */
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
     * Returns all of the {@link CameraInternal} instances represented by this Camera.
     *
     * <p> A {@link Camera} is a logical camera which wraps one or more {@link CameraInternal}.
     * At any time, only one of the CameraInternal is actually being used, and it is up to the
     * implementation to determine which {@link CameraInternal} will be used. Certain
     * reconfigurations of the camera will cause the current CameraInternal camera to change.
     * However, it will be transparent to the {@link CameraControl} and {@link CameraInfo}
     * retrieved from {@link #getCameraControl()} and {@link #getCameraInfo()}.
     *
     * <p> The CameraInternal are returned in the order of preference. The
     * {@link CameraConfig} that is set via {@link #setExtendedConfig(CameraConfig)} can filter
     * out specific instances of the CameraInternal. The remaining CameraInternal that comes
     * first in this ordering will be used.
     *
     * <p> The set of CameraInternal should be static for the lifetime of the Camera.
     *
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    LinkedHashSet<CameraInternal> getCameraInternals();

    /**
     * Get the currently set extended config of the Camera.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    CameraConfig getExtendedConfig();

    /**
     * Set the extended config of the Camera.
     *
     * <p>This is used to apply additional configs that modifying the behavior of the camera and
     * any attached {@link UseCase}. For example, it may configure the {@link ImageCapture} to use a
     * {@link androidx.camera.core.impl.CaptureProcessor} in order to implement effects such as
     * HDR or bokeh.
     *
     * @param cameraConfig if null then it will reset the camera to an empty config.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    void setExtendedConfig(@Nullable CameraConfig cameraConfig);
}
