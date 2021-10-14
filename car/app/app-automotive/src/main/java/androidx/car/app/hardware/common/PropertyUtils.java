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
import static androidx.car.app.hardware.common.CarUnit.IMPERIAL_GALLON;
import static androidx.car.app.hardware.common.CarUnit.LITER;
import static androidx.car.app.hardware.common.CarUnit.MILLILITER;
import static androidx.car.app.hardware.common.CarUnit.US_GALLON;

import android.car.Car;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyValue;
import android.util.Pair;
import android.util.SparseArray;

import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.info.AutomotiveCarInfo;
import androidx.car.app.hardware.info.EnergyProfile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility functions to work with {@link android.car.hardware.CarPropertyValue}
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public final class PropertyUtils {
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
            append(AutomotiveCarInfo.TOLL_CARD_STATUS_ID, Car.PERMISSION_CAR_INFO);
            append(AutomotiveCarInfo.SPEED_DISPLAY_UNIT_ID, Car.PERMISSION_READ_DISPLAY_UNITS);
            append(VehiclePropertyIds.DISTANCE_DISPLAY_UNITS, Car.PERMISSION_READ_DISPLAY_UNITS);
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
    private static final Set<Integer> ON_CHANGE_PROPERTIES =
            new HashSet<>(Arrays.asList(VehiclePropertyIds.FUEL_LEVEL_LOW,
                    AutomotiveCarInfo.TOLL_CARD_STATUS_ID,
                    AutomotiveCarInfo.SPEED_DISPLAY_UNIT_ID,
                    VehiclePropertyIds.DISTANCE_DISPLAY_UNITS));

    // VehicleArea:MASK in vehicle/2.0/types.hal
    private static final int VEHICLE_AREA_MASK = 0x0f000000;

    // VehicleArea:GLOBAL in vehicle/2.0/types.hal
    private static final int VEHICLE_AREA_GLOBAL = 0x01000000;

    // VehicleUnit.METER_PER_SEC in car service
    private static final int VEHICLE_UNIT_METER_PER_SEC = 0x01;

    // VehicleUnit.MILES_PER_HOUR in car service
    private static final int VEHICLE_UNIT_MILES_PER_HOUR = 0x90;

    // VehicleUnit.KILOMETERS_PER_HOUR in car service
    private static final int VEHICLE_UNIT_KILOMETERS_PER_HOUR = 0x91;

    // VehicleUnit.MILLIMETER in car service
    private static final int VEHICLE_UNIT_MILLIMETER = 0x20;

    // VehicleUnit.METER in car service
    private static final int VEHICLE_UNIT_METER = 0x21;

    // VehicleUnit.KILOMETER in car service
    private static final int VEHICLE_UNIT_KILOMETER = 0x23;

    // VehicleUnit.MILE in car service
    private static final int VEHICLE_UNIT_MILE = 0x24;

    // VehicleUnit.MILLIMETER in car service
    private static final int VEHICLE_UNIT_MILLILITER = 0x40;

    // VehicleUnit.LITER in car service
    private static final int VEHICLE_UNIT_VOLUME_LITER = 0x41;

    // VehicleUnit.US_GALLON in car service
    private static final int VEHICLE_UNIT_VOLUME_US_GALLON = 0x42;

    // VehicleUnit.IMPERIAL_GALLON in car service
    private static final int VEHICLE_UNIT_VOLUME_IMPERIAL_GALLON = 0x43;

    /**
     * Maps speed units in car service to speed units in {@link CarUnit}.
     */
    public static @CarUnit.CarSpeedUnit int covertSpeedUnit(int vehicleUnit) {
        switch (vehicleUnit) {
            case VEHICLE_UNIT_METER_PER_SEC:
                return CarUnit.METERS_PER_SEC;
            case VEHICLE_UNIT_MILES_PER_HOUR:
                return CarUnit.MILES_PER_HOUR;
            case VEHICLE_UNIT_KILOMETERS_PER_HOUR:
                return CarUnit.KILOMETERS_PER_HOUR;
            default:
                throw new IllegalArgumentException("Invalid speed unit: " + vehicleUnit);
        }
    }

    /**
     * Maps distance units in car service to distance units in {@link CarUnit}.
     */
    public static @CarUnit.CarDistanceUnit int covertDistanceUnit(int vehicleUnit) {
        switch (vehicleUnit) {
            case VEHICLE_UNIT_METER:
                return CarUnit.METER;
            case VEHICLE_UNIT_MILE:
                return CarUnit.MILE;
            case VEHICLE_UNIT_MILLIMETER:
                return CarUnit.MILLIMETER;
            case VEHICLE_UNIT_KILOMETER:
                return CarUnit.KILOMETER;
            default:
                throw new IllegalArgumentException("Invalid display unit: " + vehicleUnit);
        }
    }

    /**
     * Maps volume units in car service to volume units in {@link CarUnit}.
     */
    @OptIn(markerClass = ExperimentalCarApi.class)
    public static @CarUnit.CarVolumeUnit int covertVolumeUnit(int vehicleUnit) {
        switch (vehicleUnit) {
            case VEHICLE_UNIT_MILLILITER:
                return MILLILITER;
            case VEHICLE_UNIT_VOLUME_LITER:
                return LITER;
            case VEHICLE_UNIT_VOLUME_US_GALLON:
                return US_GALLON;
            case VEHICLE_UNIT_VOLUME_IMPERIAL_GALLON:
                return IMPERIAL_GALLON;
            default:
                throw new IllegalArgumentException("Invalid volume unit: " + vehicleUnit);
        }
    }

    /**
     * Maps EV connector types in car service to types in {@link EnergyProfile}.
     */
    public static @EnergyProfile.EvConnectorType int covertEvConnectorType(
            int vehicleEvConnectorType) {
        switch (vehicleEvConnectorType) {
            case 1: // IEC_TYPE_1_AC
                return EnergyProfile.EVCONNECTOR_TYPE_J1772;
            case 2: // IEC_TYPE_2_AC
                return EnergyProfile.EVCONNECTOR_TYPE_MENNEKES;
            case 3: // IEC_TYPE_3_AC
                return EnergyProfile.EVCONNECTOR_TYPE_SCAME;
            case 4: // IEC_TYPE_4_DC
                return EnergyProfile.EVCONNECTOR_TYPE_CHADEMO;
            case 5: // IEC_TYPE_1_CCS_DC
                return EnergyProfile.EVCONNECTOR_TYPE_COMBO_1;
            case 6: // IEC_TYPE_2_CCS_DC
                return EnergyProfile.EVCONNECTOR_TYPE_COMBO_2;
            case 7: // TESLA_ROADSTER
                return EnergyProfile.EVCONNECTOR_TYPE_TESLA_ROADSTER;
            case 8: // TESLA_HPWC
                return EnergyProfile.EVCONNECTOR_TYPE_TESLA_HPWC;
            case 9: // TESLA_SUPERCHARGER
                return EnergyProfile.EVCONNECTOR_TYPE_TESLA_SUPERCHARGER;
            case 10: // GBT_AC
                return EnergyProfile.EVCONNECTOR_TYPE_GBT;
            case 11: // GBT_DC
                return EnergyProfile.EVCONNECTOR_TYPE_GBT_DC;
            case 101: // OTHER
                return EnergyProfile.EVCONNECTOR_TYPE_OTHER;
            default:
                return EnergyProfile.EVCONNECTOR_TYPE_UNKNOWN;
        }
    }

    /**
     * Returns a {@link Set<String>} that contains permissions for reading properties.
     *
     * @throws SecurityException if android application cannot access the property
     */
    static Set<String> getReadPermissionsByPropertyIds(List<Integer> requestList) {
        Set<String> permissions = new HashSet<>();
        for (int propertyId : requestList) {
            String permissionString =
                    PERMISSION_READ_PROPERTY.get(propertyId, null);
            if (permissionString == null) {
                throw new SecurityException(
                        "Application cannot get permission for reading property: " + propertyId);
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
    static boolean isGlobalProperty(int propertyId) {
        return (propertyId & VEHICLE_AREA_MASK) == VEHICLE_AREA_GLOBAL;
    }

    /**
     * Returns true if the property has change mode as
     * {@link android.car.hardware.CarPropertyConfig#VEHICLE_PROPERTY_CHANGE_MODE_ONCHANGE}.
     */
    static boolean isOnChangeProperty(int propertyId) {
        return ON_CHANGE_PROPERTIES.contains(propertyId);
    }

    /**
     * Maps from status in {@link CarPropertyValue.PropertyStatus} to status
     * in {@link CarValue.StatusCode}.
     */
    static @CarValue.StatusCode int mapToStatusCodeInCarValue(int carPropertyStatus) {
        switch (carPropertyStatus) {
            case CarPropertyValue.STATUS_AVAILABLE:
                return CarValue.STATUS_SUCCESS;
            case CarPropertyValue.STATUS_ERROR:
                // TODO(b/191932488): add status_error in CarValue.
                return CarValue.STATUS_UNKNOWN;
            case CarPropertyValue.STATUS_UNAVAILABLE:
                return CarValue.STATUS_UNAVAILABLE;
            default:
                throw new IllegalArgumentException("Invalid car property status: "
                        + carPropertyStatus);
        }
    }

    private PropertyUtils() {
    }
}
