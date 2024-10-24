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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.util.Objects.requireNonNull;

import android.content.Context;

import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;
import androidx.car.app.HostDispatcher;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.climate.AutomotiveCarClimate;
import androidx.car.app.hardware.climate.CarClimate;
import androidx.car.app.hardware.common.PropertyManager;
import androidx.car.app.hardware.info.AutomotiveCarInfo;
import androidx.car.app.hardware.info.AutomotiveCarSensors;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.CarSensors;

import org.jspecify.annotations.NonNull;

/**
 * {@link CarHardwareManager} which uses Android Automotive OS APIs to access properties, sensors,
 * and actions.
 */
@ExperimentalCarApi
@CarProtocol
public final class AutomotiveCarHardwareManager implements CarHardwareManager {

    private final AutomotiveCarInfo mCarInfo;
    private final AutomotiveCarSensors mCarSensors;
    private final AutomotiveCarClimate mCarClimate;

    public AutomotiveCarHardwareManager(@NonNull Context context) {
        Context appContext = requireNonNull(context.getApplicationContext());
        PropertyManager mPropertyManager = new PropertyManager(appContext);
        mCarInfo = new AutomotiveCarInfo(mPropertyManager);
        mCarSensors = new AutomotiveCarSensors();
        mCarClimate = new AutomotiveCarClimate(mPropertyManager);
    }

    @RestrictTo(LIBRARY_GROUP)
    public AutomotiveCarHardwareManager(@NonNull CarContext context,
            @NonNull HostDispatcher dispatcher) {
        this(context);
    }

    @Override
    public @NonNull CarInfo getCarInfo() {
        return mCarInfo;
    }

    @Override
    public @NonNull CarSensors getCarSensors() {
        return mCarSensors;
    }

    @Override
    public @NonNull CarClimate getCarClimate() {
        return mCarClimate;
    }
}
