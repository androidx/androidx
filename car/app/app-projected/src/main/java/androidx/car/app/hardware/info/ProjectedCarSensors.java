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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.hardware.common.CarHardwareHostDispatcher;
import androidx.car.app.hardware.common.OnCarDataListener;

import java.util.concurrent.Executor;

/**
 * Manages access to vehicle specific sensor communication with a car app host.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class ProjectedCarSensors implements CarSensors {

    public ProjectedCarSensors(@NonNull CarHardwareHostDispatcher hostDispatcher) {
        // TODO(b/188534318): Implement calls to host
    }

    @Override
    public void addAccelerometerListener(@UpdateRate int rate,
            @NonNull Executor executor, @NonNull OnCarDataListener<Accelerometer> listener) {
        // TODO(b/188534318): Implement calls to host
    }

    @Override
    public void removeAccelerometerListener(@NonNull OnCarDataListener<Accelerometer> listener) {
        // TODO(b/188534318): Implement calls to host
    }

    @Override
    public void addGyroscopeListener(@UpdateRate int rate, @NonNull Executor executor,
            @NonNull OnCarDataListener<Gyroscope> listener) {
        // TODO(b/188534318): Implement calls to host
    }

    @Override
    public void removeGyroscopeListener(@NonNull OnCarDataListener<Gyroscope> listener) {
        // TODO(b/188534318): Implement calls to host
    }

    @Override
    public void addCompassListener(@UpdateRate int rate, @NonNull Executor executor,
            @NonNull OnCarDataListener<Compass> listener) {
        // TODO(b/188534318): Implement calls to host
    }

    @Override
    public void removeCompassListener(@NonNull OnCarDataListener<Compass> listener) {
        // TODO(b/188534318): Implement calls to host
    }

    @Override
    public void addCarHardwareLocationListener(@UpdateRate int rate,
            @NonNull Executor executor, @NonNull OnCarDataListener<CarHardwareLocation> listener) {
        // TODO(b/188534318): Implement calls to host
    }

    @Override
    public void removeCarHardwareLocationListener(
            @NonNull OnCarDataListener<CarHardwareLocation> listener) {
        // TODO(b/188534318): Implement calls to host
    }
}
