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

package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.car.Car;
import android.car.VehiclePropertyIds;
import android.util.Pair;
import android.util.SparseArray;

import androidx.annotation.RestrictTo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility functions to work with {@link android.car.hardware.CarPropertyValue}
 *
 * @hide
 */
@RestrictTo(LIBRARY)
final class PropertyUtils {
    // System level permission in car-lib for read car' mileage.
    private static final String CAR_PERMISSION_MILEAGE = "android.car.permission.CAR_MILEAGE";

    // System level permission in car-lib for reading car tire pressures.
    private static final String CAR_PERMISSION_TIRES = "android.car.permission.CAR_TIRES";

    // System level permission in car-lib for setting range remaining value for cars.
    private static final String CAR_PERMISSION_ADJUST_RANGE_REMAINING =
            "android.car.permission.ADJUST_RANGE_REMAINING";

    // System level permission in car-lib for controlling car's energy ports.
    private static final String CAR_PERMISSION_CONTROL_CAR_ENERGY_PORTS =
            "android.car.permission.CONTROL_CAR_ENERGY_PORTS";

    // Index key is property id, value is the permission to read property.
    private static final SparseArray<String> PERMISSION_READ_PROPERTY = new SparseArray<String>() {
        {
            append(VehiclePropertyIds.INFO_VIN, Car.PERMISSION_IDENTIFICATION);
            append(VehiclePropertyIds.INFO_MAKE, Car.PERMISSION_CAR_INFO);
            append(VehiclePropertyIds.INFO_MODEL, Car.PERMISSION_CAR_INFO);
            append(VehiclePropertyIds.INFO_MODEL_YEAR, Car.PERMISSION_CAR_INFO);
            append(VehiclePropertyIds.INFO_FUEL_CAPACITY, Car.PERMISSION_CAR_INFO);
            append(VehiclePropertyIds.INFO_FUEL_TYPE, Car.PERMISSION_CAR_INFO);
            append(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY, Car.PERMISSION_CAR_INFO);
            append(VehiclePropertyIds.INFO_EV_CONNECTOR_TYPE, Car.PERMISSION_CAR_INFO);
            append(VehiclePropertyIds.INFO_DRIVER_SEAT, Car.PERMISSION_CAR_INFO);
            // CAR_MILEAGE is system permission
            append(VehiclePropertyIds.PERF_ODOMETER, CAR_PERMISSION_MILEAGE);
            append(VehiclePropertyIds.PERF_VEHICLE_SPEED, Car.PERMISSION_SPEED);
            append(VehiclePropertyIds.PERF_VEHICLE_SPEED_DISPLAY, Car.PERMISSION_SPEED);
            append(VehiclePropertyIds.WHEEL_TICK, Car.PERMISSION_SPEED);
            append(VehiclePropertyIds.FUEL_LEVEL, Car.PERMISSION_ENERGY);
            append(VehiclePropertyIds.FUEL_LEVEL_LOW, Car.PERMISSION_ENERGY);
            append(VehiclePropertyIds.EV_BATTERY_LEVEL, Car.PERMISSION_ENERGY);
            append(VehiclePropertyIds.FUEL_DOOR_OPEN, Car.PERMISSION_ENERGY_PORTS);
            append(VehiclePropertyIds.EV_CHARGE_PORT_OPEN, Car.PERMISSION_ENERGY_PORTS);
            append(VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED, Car.PERMISSION_ENERGY_PORTS);
            append(VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE,
                    Car.PERMISSION_ENERGY_PORTS);
            append(VehiclePropertyIds.RANGE_REMAINING, Car.PERMISSION_ENERGY);
            append(VehiclePropertyIds.TIRE_PRESSURE, CAR_PERMISSION_TIRES);
            append(VehiclePropertyIds.GEAR_SELECTION, Car.PERMISSION_POWERTRAIN);
            append(VehiclePropertyIds.CURRENT_GEAR, Car.PERMISSION_POWERTRAIN);
            append(VehiclePropertyIds.PARKING_BRAKE_ON, Car.PERMISSION_POWERTRAIN);
            append(VehiclePropertyIds.PARKING_BRAKE_AUTO_APPLY, Car.PERMISSION_POWERTRAIN);
        }
    };

    // Permissions for writing properties. They are system level permissions.
    private static final SparseArray<String> PERMISSION_WRITE_PROPERTY = new SparseArray<String>() {
        {
            append(VehiclePropertyIds.FUEL_DOOR_OPEN, CAR_PERMISSION_CONTROL_CAR_ENERGY_PORTS);
            append(VehiclePropertyIds.EV_CHARGE_PORT_OPEN, CAR_PERMISSION_CONTROL_CAR_ENERGY_PORTS);
            append(VehiclePropertyIds.RANGE_REMAINING, CAR_PERMISSION_ADJUST_RANGE_REMAINING);
        }
    };

    // VehicleArea:MASK in vehicle/2.0/types.hal
    private static final int VEHICLE_AREA_MASK = 0x0f000000;

    // VehicleArea:GLOBAL in vehicle/2.0/types.hal
    private static final int VEHICLE_AREA_GLOBAL = 0x01000000;

    /**
     * Returns a {@link Set<String>} that contains permissions for reading properties.
     *
     * @throws SecurityException if android application cannot access the property
     */
    static Set<String> getReadPermissions(List<GetPropertyRequest> requestList) {
        Set<String> permissions = new HashSet<>();
        for (GetPropertyRequest request : requestList) {
            String permissionString =
                    PERMISSION_READ_PROPERTY.get(request.getPropertyId(), null);
            if (permissionString == null) {
                throw new SecurityException(
                        "Application cannot get permission for reading property: "
                        + request.getPropertyId());
            }
            permissions.add(permissionString);
        }
        return permissions;
    }

    /**
     * Returns a {@link Set<String>} that contains permissions for setting properties.
     *
     * @throws SecurityException if android application cannot set value for property
     */
    static Set<String> getWritePermissions(List<Pair<Integer, Integer>> props) {
        Set<String> permissions = new HashSet<>();
        for (Pair<Integer, Integer> prop : props) {
            String permissionString = PERMISSION_WRITE_PROPERTY.get(prop.first, null);
            if (permissionString == null) {
                throw new SecurityException(
                        "Application cannot get permission for setting property: "
                                + prop.first);
            }
            permissions.add(permissionString);
        }
        return permissions;
    }

    /**
     * Returns {@code true} if the property is
     * {@link android.car.VehicleAreaType#VEHICLE_AREA_TYPE_GLOBAL} property.
     */
    static boolean isGlobalProperty(int propId) {
        return (propId & VEHICLE_AREA_MASK) == VEHICLE_AREA_GLOBAL;
    }

    private PropertyUtils() {
    }
}
