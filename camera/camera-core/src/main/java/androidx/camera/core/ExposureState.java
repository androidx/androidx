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

package androidx.camera.core;

import android.util.Range;
import android.util.Rational;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * An interface which contains the camera exposure related information.
 *
 * <p>Applications can retrieve an instance via {@link CameraInfo#getExposureState()}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ExposureState {

    /**
     * Get the current exposure compensation index.
     *
     * <p>The exposure value (EV) is the compensation index multiplied by the step value
     * which is given by {@link #getExposureCompensationStep()}. Increasing the compensation
     * index by using the {@link CameraControl#setExposureCompensationIndex} will increase
     * exposure making the capture result brighter, decreasing the value making it darker.
     * <p>For example, if the exposure value (EV) step size is 0.333, set the exposure compensation
     * index value '6' will mean an exposure compensation of +2 EV; -3 will mean an exposure
     * compensation of -1 EV.
     * <p>The exposure value resets to default when there is no {@link UseCase} associated with
     * the camera. For example, unbind all use cases from the camera or when the lifecycle
     * changed that all the use case stopping data from the camera.
     *
     * @return The current exposure compensation index. If {@link
     * #isExposureCompensationSupported()} is false, always return 0.
     * @see CameraControl#setExposureCompensationIndex
     */
    int getExposureCompensationIndex();

    /**
     * Get the maximum and minimum exposure compensation values for
     * {@link CameraControl#setExposureCompensationIndex}
     *
     * <p>The actual exposure value (EV) range that supported by the camera can be calculated by
     * multiplying the {@link #getExposureCompensationStep()} with the maximum and minimum values:
     * <p><code>Min.exposure compensation * {@link #getExposureCompensationStep()} &lt;= minimum
     * supported EV</code>
     * <p><code>Max.exposure compensation * {@link #getExposureCompensationStep()} &gt;= maximum
     * supported EV</code>
     *
     * @return the maximum and minimum exposure compensation values range. If {@link
     * #isExposureCompensationSupported()} is false, return Range [0,0].
     * @see android.hardware.camera2.CameraCharacteristics#CONTROL_AE_COMPENSATION_RANGE
     */
    @NonNull
    Range<Integer> getExposureCompensationRange();

    /**
     * Get the smallest step by which the exposure compensation can be changed.
     *
     * @return The exposure compensation step. If {@link
     * #isExposureCompensationSupported()} is false, return {@link Rational#ZERO}.
     * @see android.hardware.camera2.CameraCharacteristics#CONTROL_AE_COMPENSATION_STEP
     */
    @NonNull
    Rational getExposureCompensationStep();

    /**
     * Whether exposure compensation is supported for this camera.
     *
     * @return true if exposure compensation is supported for this camera.
     */
    boolean isExposureCompensationSupported();
}
