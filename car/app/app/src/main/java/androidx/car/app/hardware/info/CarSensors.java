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
package androidx.car.app.hardware.info;

import androidx.annotation.NonNull;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.OnCarDataListener;

import java.util.concurrent.Executor;

/**
 * Manages access to androidx.car.app.hardware specific sensors such as compass, accelerometer,
 * and gyroscope.
 */
@RequiresCarApi(3)
public interface CarSensors {
    /**
     * Setup an ongoing listener to receive {@link Accelerometer} data from the car hardware.
     *
     * <p>If the listener was added previously then previous params are updated with the new params.
     *
     * @param params   the parameters for this request. Use {@link Accelerometer.Params#getDefault}
     *                 as a default
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener to use
     */
    void addAccelerometerListener(@NonNull Accelerometer.Params params,
            @NonNull Executor executor,
            @NonNull OnCarDataListener<Accelerometer> listener);

    /**
     * Remove an ongoing listener for {@link Accelerometer} information.
     *
     * <p>If the listener is not currently added, this call will be ignored.
     *
     * @param listener the listener to use
     */
    void removeAccelerometerListener(@NonNull OnCarDataListener<Accelerometer> listener);

    /**
     * Setup an ongoing listener to receive {@link Gyroscope} data from the car hardware.
     *
     * <p>If the listener was added previously then previous params are updated with the new params.
     *
     * @param params   the parameters for this request. Use {@link Gyroscope.Params#getDefault}
     *                 as a default
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener to use
     */
    void addGyroscopeListener(@NonNull Gyroscope.Params params,
            @NonNull Executor executor,
            @NonNull OnCarDataListener<Gyroscope> listener);

    /**
     * Remove an ongoing listener for {@link Gyroscope} information.
     *
     * <p>If the listener is not currently added, this call will be ignored.
     *
     * @param listener the listener to use
     */
    void removeGyroscopeListener(@NonNull OnCarDataListener<Gyroscope> listener);

    /**
     * Setup an ongoing listener to receive {@link Compass} data from the car hardware.
     *
     * <p>If the listener was added previously then previous params are updated with the new params.
     *
     * @param params   the parameters for this request. Use {@link Compass.Params#getDefault}
     *                 as a default
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener to use
     */
    void addCompassListener(@NonNull Compass.Params params,
            @NonNull Executor executor,
            @NonNull OnCarDataListener<Compass> listener);

    /**
     * Remove an ongoing listener for {@link Compass} information.
     *
     * <p>If the listener is not currently added, this call will be ignored.
     *
     * @param listener the listener to use
     */
    void removeCompassListener(@NonNull OnCarDataListener<Compass> listener);

    /**
     * Setup an ongoing listener to receive {@link CarHardwareLocation} data from the car hardware.
     *
     * <p>If the listener was added previously then previous params are updated with the new params.
     *
     * @param params   the parameters for this request. Use
     *                 {@link CarHardwareLocation.Params#getDefault}  as a default
     * @param executor the executor which will be used for invoking the listener
     * @param listener the listener to use
     */
    void addCarHardwareLocationListener(@NonNull CarHardwareLocation.Params params,
            @NonNull Executor executor,
            @NonNull OnCarDataListener<CarHardwareLocation> listener);

    /**
     * Remove an ongoing listener for {@link CarHardwareLocation} information.
     *
     * <p>If the listener is not currently added, this call will be ignored.
     *
     * @param listener the listener to use
     */
    void removeCarHardwareLocationListener(
            @NonNull OnCarDataListener<CarHardwareLocation> listener);
}
