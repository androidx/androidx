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

import static java.util.Objects.requireNonNull;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.hardware.common.PropertyManager;
import androidx.car.app.hardware.info.AutomotiveCarInfo;
import androidx.car.app.hardware.info.AutomotiveCarSensors;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.CarSensors;

/**
 * {@link CarHardwareManager} which uses Android Automotive OS APIs to access properties, sensors,
 * and actions.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public final class AutomotiveCarHardwareManager implements CarHardwareManager {

    private final AutomotiveCarInfo mCarInfo;
    private final AutomotiveCarSensors mCarSensors;

    public AutomotiveCarHardwareManager(@NonNull Context context) {
        requireNonNull(context);
        mCarInfo = new AutomotiveCarInfo(new PropertyManager(context));
        mCarSensors = new AutomotiveCarSensors();
    }

    @NonNull
    @Override
    public CarInfo getCarInfo() {
        return mCarInfo;
    }

    @NonNull
    @Override
    public CarSensors getCarSensors() {
        return mCarSensors;
    }
}
