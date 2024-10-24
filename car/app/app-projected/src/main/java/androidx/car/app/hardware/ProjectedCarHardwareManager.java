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
package androidx.car.app.hardware;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;
import androidx.car.app.HostDispatcher;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.common.CarHardwareHostDispatcher;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.CarSensors;
import androidx.car.app.hardware.info.ProjectedCarInfo;
import androidx.car.app.hardware.info.ProjectedCarSensors;

import org.jspecify.annotations.NonNull;

/**
 * {@link CarHardwareManager} which access projected vehicle specific properties, sensors, and
 * actions via a host interface.
 *
 */
@RestrictTo(LIBRARY)
@CarProtocol
public class ProjectedCarHardwareManager implements CarHardwareManager {

    private final ProjectedCarInfo mVehicleInfo;
    private final ProjectedCarSensors mVehicleSensors;

    @Override
    public @NonNull CarInfo getCarInfo() {
        return mVehicleInfo;
    }

    @Override
    public @NonNull CarSensors getCarSensors() {
        return mVehicleSensors;
    }

    /**
     * Creates an instance of {@link CarHardwareManager}.
     */
    // TODO(b/216177515): Remove this annotation once EvStatus is ready.
    @OptIn(markerClass = ExperimentalCarApi.class)
    public ProjectedCarHardwareManager(@NonNull CarContext context,
            @NonNull HostDispatcher hostDispatcher) {
        CarHardwareHostDispatcher carHardwareHostDispatcher =
                new CarHardwareHostDispatcher(hostDispatcher);
        mVehicleInfo = new ProjectedCarInfo(carHardwareHostDispatcher);
        mVehicleSensors = new ProjectedCarSensors(carHardwareHostDispatcher);
    }
}

