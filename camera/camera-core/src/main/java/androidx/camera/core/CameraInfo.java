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

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

/**
 * An interface for retrieving camera information.
 *
 * <p>Applications can retrieve an instance via {@link Camera#getCameraInfo()}.
 */
public interface CameraInfo {

    /**
     * Returns the sensor rotation in degrees, relative to the device's "natural" (default)
     * orientation.
     *
     * @return The sensor rotation in degrees, relative to device's "natural" (default) orientation.
     * @see
     * <a href="https://developer.android.com/guide/topics/sensors/sensors_overview#sensors-coords">
     *     Sensor Coordinate System</a>
     */
    int getSensorRotationDegrees();

    /**
     * Returns the sensor rotation, in degrees, relative to the given rotation value.
     *
     * <p>Valid values for the relative rotation are {@link Surface#ROTATION_0} (natural), {@link
     * Surface#ROTATION_90}, {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}.
     *
     * @param relativeRotation The rotation relative to which the output will be calculated.
     * @return The sensor rotation in degrees.
     */
    int getSensorRotationDegrees(@ImageOutputConfig.RotationValue int relativeRotation);

    /** Returns if flash unit is available or not. */
    boolean hasFlashUnit();

    /**
     * Returns a {@link LiveData} of current {@link TorchState}.
     *
     * <p>The torch can be turned on and off via {@link CameraControl#enableTorch(boolean)} which
     * will trigger the change event to the returned {@link LiveData}. Apps can either get
     * immediate value via {@link LiveData#getValue()} or observe it via
     * {@link LiveData#observe(LifecycleOwner, Observer)} to update torch UI accordingly.
     *
     * <p>If the camera doesn't have a flash unit (see {@link #hasFlashUnit()}), then the torch
     * state will be {@link TorchState#OFF}.
     *
     * @return a {@link LiveData} containing current torch state.
     */
    @NonNull
    LiveData<Integer> getTorchState();

    /**
     * Returns a {@link LiveData} of current zoom ratio.
     *
     * <p>Apps can either get immediate value via {@link LiveData#getValue()} (The value is never
     * null, it has default value in the beginning) or they can observe it via
     * {@link LiveData#observe(LifecycleOwner, Observer)} to update zoom UI accordingly.
     *
     * <p>Setting zoomRatio or linearZoom will both trigger the change event.
     *
     * @return a {@link LiveData} containing current zoom ratio.
     */
    @NonNull
    LiveData<Float> getZoomRatio();

    /**
     * Returns a {@link LiveData} of the maximum zoom ratio.
     *
     * <p>Apps can either get immediate value via {@link LiveData#getValue()} (The value is never
     * null, it has default value in the beginning) or they can observe it via
     * {@link LiveData#observe(LifecycleOwner, Observer)} to update zoom UI accordingly.
     *
     * <p>While the value is fixed most of the time, enabling extension could change the maximum
     * zoom ratio.
     *
     * @return a {@link LiveData} containing the maximum zoom ratio value.
     */
    @NonNull
    LiveData<Float> getMaxZoomRatio();

    /**
     * Returns a {@link LiveData} of the minimum zoom ratio.
     *
     * <p>Apps can either get immediate value via {@link LiveData#getValue()} (The value is never
     * null, it has default value in the beginning) or they can observe it via
     * {@link LiveData#observe(LifecycleOwner, Observer)} to update zoom UI accordingly.
     *
     * <p>While the value is fixed most of the time, enabling extension could change the minimum
     * zoom ratio value.
     *
     * @return a {@link LiveData} containing the minimum zoom ratio value.
     */
    @NonNull
    LiveData<Float> getMinZoomRatio();

    /**
     * Returns a {@link LiveData} of current linearZoom which is in range [0..1].
     * LinearZoom 0 represents the minimum zoom while linearZoom 1.0 represents the maximum zoom.
     *
     * <p>Apps can either get immediate value via {@link LiveData#getValue()} (The value is never
     * null, it has default value in the beginning) or they can observe it via
     * {@link LiveData#observe(LifecycleOwner, Observer)} to update zoom UI accordingly.
     * <p>Setting zoomRatio or linearZoom will both trigger the change event.
     *
     * @return a {@link LiveData} containing current linearZoom.
     */
    @NonNull
    LiveData<Float> getLinearZoom();
}
