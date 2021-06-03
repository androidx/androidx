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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.hardware.ICarHardwareResultTypes;
import androidx.car.app.hardware.common.CarHardwareHostDispatcher;
import androidx.car.app.hardware.common.CarResultStubMap;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.OnCarDataListener;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Manages access to vehicle specific sensor communication with a car app host.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class ProjectedCarSensors implements CarSensors {

    private static final CarValue<List<Float>> UNIMPLEMENTED_FLOAT_LIST = new CarValue<>(null,
            0, CarValue.STATUS_UNIMPLEMENTED);
    private final CarResultStubMap<Accelerometer, Integer> mAccelerometerCarResultStubMap;
    private final CarResultStubMap<Gyroscope, Integer> mGyroscopeCarResultStubMap;
    private final CarResultStubMap<Compass, Integer> mCompassCarResultStubMap;
    private final CarResultStubMap<CarHardwareLocation, Integer>
            mCarHardwareLocationCarResultStubMap;

    final CarHardwareHostDispatcher mCarHardwareHostDispatcher;

    /**
     * Constructs a {@link CarSensors} implementation for android auto projected.
     *
     * @throws NullPointerException if {@code carHardwareHostDispatcher} is {@code null}
     */
    public ProjectedCarSensors(@NonNull CarHardwareHostDispatcher carHardwareHostDispatcher) {
        mCarHardwareHostDispatcher = requireNonNull(carHardwareHostDispatcher);
        mAccelerometerCarResultStubMap =
                new CarResultStubMap<>(ICarHardwareResultTypes.TYPE_SENSOR_ACCELEROMETER,
                        new Accelerometer(UNIMPLEMENTED_FLOAT_LIST), carHardwareHostDispatcher);
        mGyroscopeCarResultStubMap =
                new CarResultStubMap<>(ICarHardwareResultTypes.TYPE_SENSOR_GYROSCOPE,
                        new Gyroscope(UNIMPLEMENTED_FLOAT_LIST), carHardwareHostDispatcher);
        mCompassCarResultStubMap =
                new CarResultStubMap<>(ICarHardwareResultTypes.TYPE_SENSOR_COMPASS,
                        new Compass(UNIMPLEMENTED_FLOAT_LIST), carHardwareHostDispatcher);
        mCarHardwareLocationCarResultStubMap =
                new CarResultStubMap<>(ICarHardwareResultTypes.TYPE_SENSOR_CAR_LOCATION,
                        new CarHardwareLocation(
                                new CarValue<>(null, 0, CarValue.STATUS_UNIMPLEMENTED)),
                        carHardwareHostDispatcher);
    }

    @Override
    public void addAccelerometerListener(@UpdateRate int rate,
            @NonNull Executor executor, @NonNull OnCarDataListener<Accelerometer> listener) {
        mAccelerometerCarResultStubMap.addListener(rate, requireNonNull(executor),
                requireNonNull(listener));
    }

    @Override
    public void removeAccelerometerListener(@NonNull OnCarDataListener<Accelerometer> listener) {
        mAccelerometerCarResultStubMap.removeListener(requireNonNull(listener));
    }

    @Override
    public void addGyroscopeListener(@UpdateRate int rate, @NonNull Executor executor,
            @NonNull OnCarDataListener<Gyroscope> listener) {
        mGyroscopeCarResultStubMap.addListener(rate, requireNonNull(executor),
                requireNonNull(listener));
    }

    @Override
    public void removeGyroscopeListener(@NonNull OnCarDataListener<Gyroscope> listener) {
        mGyroscopeCarResultStubMap.removeListener(requireNonNull(listener));
    }

    @Override
    public void addCompassListener(@UpdateRate int rate, @NonNull Executor executor,
            @NonNull OnCarDataListener<Compass> listener) {
        mCompassCarResultStubMap.addListener(rate, requireNonNull(executor),
                requireNonNull(listener));
    }

    @Override
    public void removeCompassListener(@NonNull OnCarDataListener<Compass> listener) {
        mCompassCarResultStubMap.removeListener(requireNonNull(listener));
    }

    @Override
    public void addCarHardwareLocationListener(@UpdateRate int rate,
            @NonNull Executor executor, @NonNull OnCarDataListener<CarHardwareLocation> listener) {
        mCarHardwareLocationCarResultStubMap.addListener(rate, requireNonNull(executor),
                requireNonNull(listener));
    }

    @Override
    public void removeCarHardwareLocationListener(
            @NonNull OnCarDataListener<CarHardwareLocation> listener) {
        mCarHardwareLocationCarResultStubMap.removeListener(requireNonNull(listener));
    }
}
