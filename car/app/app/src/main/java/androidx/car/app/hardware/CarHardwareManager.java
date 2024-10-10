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

import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

import androidx.annotation.MainThread;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarAppMetadataHolderService;
import androidx.car.app.CarContext;
import androidx.car.app.HostDispatcher;
import androidx.car.app.HostException;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.climate.CarClimate;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.CarSensors;
import androidx.car.app.managers.Manager;
import androidx.car.app.versioning.CarAppApiLevels;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.Constructor;

/**
 * Manages access to car hardware specific properties and sensors.
 */
@RequiresCarApi(3)
@MainThread
public interface CarHardwareManager extends Manager {
    /**
     * Returns the {@link CarInfo} that can be used to query the car hardware information
     * such as make, model, etc.
     */
    default @NonNull CarInfo getCarInfo() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the {@link CarSensors} that can be used to access sensor information from the
     * car hardware.
     */
    default @NonNull CarSensors getCarSensors() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the {@link CarClimate} that can be used to query climate information from the
     * car hardware.
     */
    @ExperimentalCarApi
    @SuppressWarnings({"HiddenTypeParameter", "UnavailableSymbol"})
    default @NonNull CarClimate getCarClimate() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an instance of {@link CarHardwareManager} depending on linked library including an
     * optional {@link HostDispatcher}.
     *
     * @throws IllegalStateException if none of the supported classes are found or if a supported
     *                               class was found but the constructor was mismatched
     * @throws HostException         if the negotiated api level is less than
     *                               {@link CarAppApiLevels#LEVEL_3}
     */
    @RestrictTo(LIBRARY_GROUP)
    static @NonNull CarHardwareManager create(@NonNull CarContext context,
            @NonNull HostDispatcher hostDispatcher) {
        if (context.getCarAppApiLevel() < CarAppApiLevels.LEVEL_3) {
            throw new HostException("Create CarHardwareManager failed",
                    new IllegalArgumentException("Attempted to retrieve CarHardwareManager "
                            + "service, but the host is less than " + CarAppApiLevels.LEVEL_3));
        }

        try {
            ServiceInfo serviceInfo = CarAppMetadataHolderService.getServiceInfo(context);
            String managerClassName = null;
            if (serviceInfo.metaData != null) {
                managerClassName = serviceInfo.metaData.getString(
                        "androidx.car.app.CarAppMetadataHolderService.CAR_HARDWARE_MANAGER");
            }
            if (managerClassName == null) {
                throw new ClassNotFoundException("CarHardwareManager metadata could not be found");
            }

            Class<?> managerClass = Class.forName(managerClassName);
            Constructor<?> ctor = managerClass.getConstructor(CarContext.class,
                    HostDispatcher.class);
            return (CarHardwareManager) ctor.newInstance(context, hostDispatcher);
        } catch (PackageManager.NameNotFoundException | ReflectiveOperationException e) {
            throw new IllegalStateException("CarHardwareManager not configured. Did you forget "
                    + "to add a dependency on app-automotive or app-projected artifacts?");
        }
    }
}
