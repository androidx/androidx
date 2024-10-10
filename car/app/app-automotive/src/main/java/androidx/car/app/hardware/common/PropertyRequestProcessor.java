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

import static android.car.VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL;
import static android.car.VehicleAreaType.VEHICLE_AREA_TYPE_SEAT;
import static android.car.VehiclePropertyIds.HVAC_FAN_DIRECTION;
import static android.car.VehiclePropertyIds.HVAC_FAN_DIRECTION_AVAILABLE;
import static android.car.VehiclePropertyIds.HVAC_TEMPERATURE_SET;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.hardware.common.CarValue.STATUS_SUCCESS;
import static androidx.car.app.hardware.common.CarZoneUtils.convertAreaIdToCarZones;

import android.car.Car;
import android.car.hardware.CarPropertyConfig;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.RestrictTo;
import androidx.car.app.utils.LogTags;

import com.google.common.collect.ImmutableList;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * A class for interacting with the {@link CarPropertyManager} for getting any vehicle property.
 *
 */
@RestrictTo(LIBRARY)
final class PropertyRequestProcessor {
    private final CarPropertyManager mCarPropertyManager;
    private PropertyEventCallback mPropertyEventCallback;

    static final float TEMPERATURE_CONFIG_DENOMINATION = 10f;

    /**
     *  Registers this listener to get results from
     *  {@link #fetchCarPropertyValues(List, OnGetPropertiesListener)}.
     */
    interface OnGetPropertiesListener {
        /**
         * Called when get all properties' value or errors.
         *
         * @param propertyValues    a list of {@link CarPropertyValue}, empty if there are no values
         * @param errors            a list of {@link CarInternalError}, empty if there are no errors
         */
        void onGetProperties(List<CarPropertyValue<?>> propertyValues,
                List<CarInternalError> errors);
    }

    interface OnGetCarPropertyProfilesListener {
        /**
         * Called when get all properties' supported car zones have value or errors.
         *
         * @param carPropertyProfiles  a list of {@link CarPropertyProfile}, empty if there are no
         *                           responses.
         */
        void onGetCarPropertyProfiles(List<CarPropertyProfile<?>> carPropertyProfiles);
    }

    /**
     * Registers this callback to receive property updates from cars.
     */
    abstract static class PropertyEventCallback implements
            CarPropertyManager.CarPropertyEventCallback {
        /**
         * Called when a property is updated.
         *
         * @param carPropertyValue property that has been updated
         */
        @Override
        public abstract void onChangeEvent(CarPropertyValue carPropertyValue);

        /**
         * Called when a property error detected in the car.
         *
         * @param carInternalError {@link CarInternalError} in the car
         */
        public abstract void onErrorEvent(CarInternalError carInternalError);

        /**
         * Create a {@link CarInternalError} with default status {@link CarValue#STATUS_UNKNOWN}.
         *
         * @param propertyId    in {@link android.car.VehiclePropertyIds}
         * @param areaId        in {@link CarPropertyValue#getAreaId()}
         */
        @Override
        public final void onErrorEvent(int propertyId, int areaId) {
            CarInternalError error = CarInternalError.create(propertyId, areaId,
                    CarValue.STATUS_UNKNOWN);
            onErrorEvent(error);
        }

        /**
         * Create a {@link CarInternalError} based on different status code from cars.
         *
         * @param propertyId    in {@link android.car.VehiclePropertyIds}
         * @param areaId        in {@link CarPropertyValue#getAreaId()}
         * @param statusCode    in {@link CarPropertyValue.PropertyStatus}
         */
        @Override
        public final void onErrorEvent(int propertyId, int areaId, int statusCode) {
            CarInternalError error = CarInternalError.create(propertyId, areaId,
                    PropertyUtils.mapToStatusCodeInCarValue(statusCode));
            onErrorEvent(error);
        }
    }

    /**
     * Gets {@link CarPropertyValue} and returns results by
     * {@link OnGetPropertiesListener#onGetProperties(List, List)}.
     *
     * @param requests  a list of {@Code PropertyIdAreaId}, consisting of property id and the
     *                  area id
     * @param listener  the listener that will be invoked with the results of the request
     */
    public void fetchCarPropertyValues(
            @NonNull List<PropertyIdAreaId> requests,
            @NonNull OnGetPropertiesListener listener) {
        List<CarPropertyValue<?>> values = new ArrayList<>();
        List<CarInternalError> errors = new ArrayList<>();
        for (PropertyIdAreaId request : requests) {
            try {
                CarPropertyConfig<?> propertyConfig = getPropertyConfig(request.getPropertyId());
                if (propertyConfig == null) {
                    errors.add(CarInternalError.create(request.getPropertyId(), request.getAreaId(),
                            CarValue.STATUS_UNIMPLEMENTED));
                } else {
                    Class<?> clazz = propertyConfig.getPropertyType();
                    CarPropertyValue<?> propertyValue = mCarPropertyManager.getProperty(clazz,
                            request.getPropertyId(), request.getAreaId());
                    values.add(propertyValue);
                }
            } catch (IllegalArgumentException e) {
                errors.add(CarInternalError.create(request.getPropertyId(), request.getAreaId(),
                        CarValue.STATUS_UNIMPLEMENTED));
            } catch (Exception e) {
                errors.add(CarInternalError.create(request.getPropertyId(), request.getAreaId(),
                        CarValue.STATUS_UNAVAILABLE));
            }
        }
        listener.onGetProperties(values, errors);
    }

    @SuppressWarnings("deprecation")
    public void fetchCarPropertyProfiles(List<Integer> propertyIds,
            @NonNull OnGetCarPropertyProfilesListener listener) {
        ImmutableList.Builder<CarInternalError> errors = new ImmutableList.Builder<>();
        List<CarPropertyProfile<?>> carPropertyProfile = new ArrayList<>();
        for (Integer propertyId : propertyIds) {
            try {
                CarPropertyConfig<?> propertyConfig = getPropertyConfig(propertyId);
                if (propertyConfig == null
                        || (propertyConfig.getAreaType() != VEHICLE_AREA_TYPE_GLOBAL
                        && propertyConfig.getAreaType() != VEHICLE_AREA_TYPE_SEAT)) {
                    errors.add(CarInternalError.create(propertyId, CarValue.STATUS_UNIMPLEMENTED));
                } else if (propertyId == HVAC_FAN_DIRECTION) {
                    CarPropertyConfig<?> fanDirectionPropertyConfig = getPropertyConfig(
                            HVAC_FAN_DIRECTION_AVAILABLE);
                    if (fanDirectionPropertyConfig == null) {
                        Log.e(LogTags.TAG_CAR_HARDWARE, "Failed to fetch fan direction"
                                + " config.");
                        errors.add(CarInternalError.create(HVAC_FAN_DIRECTION_AVAILABLE,
                                CarValue.STATUS_UNIMPLEMENTED));
                        continue;
                    }
                    if (fanDirectionPropertyConfig.getAreaType() != VEHICLE_AREA_TYPE_SEAT) {
                        Log.e(LogTags.TAG_CAR_HARDWARE,
                                "Invalid area type for fan direction.");
                        errors.add(CarInternalError.create(HVAC_FAN_DIRECTION_AVAILABLE,
                                CarValue.STATUS_UNIMPLEMENTED));
                        continue;
                    }
                    Map<Set<CarZone>, Set<Integer>> fanDirectionValues = new HashMap<>();
                    for (int areaId : fanDirectionPropertyConfig.getAreaIds()) {
                        CarPropertyValue<Integer[]> hvacFanDirectionAvailableValue =
                                mCarPropertyManager.getProperty(
                                        HVAC_FAN_DIRECTION_AVAILABLE, areaId);
                        Integer[] fanDirectionsAvailable =
                                (Integer[]) hvacFanDirectionAvailableValue.getValue();
                        fanDirectionValues.put(convertAreaIdToCarZones(CarZoneUtils.AreaType.SEAT,
                                areaId), Arrays.stream(fanDirectionsAvailable)
                                .collect(Collectors.toSet()));
                    }
                    carPropertyProfile.add(CarPropertyProfile.builder()
                            .setPropertyId(propertyId)
                            .setStatus(STATUS_SUCCESS)
                            .setHvacFanDirection(fanDirectionValues).build());
                } else {
                    int areaType = propertyConfig.getAreaType() == VEHICLE_AREA_TYPE_SEAT
                            ? CarZoneUtils.AreaType.SEAT : CarZoneUtils.AreaType.NONE;
                    Map<Set<CarZone>, Pair<Object, Object>> minMaxRange = new HashMap<>();
                    List<Set<CarZone>> carZones = new ArrayList<>();
                    for (int areaId : propertyConfig.getAreaIds()) {
                        if (propertyConfig.getMinValue(areaId) != null
                                && propertyConfig.getMaxValue(areaId) != null) {
                            minMaxRange.put(convertAreaIdToCarZones(areaType,
                                    areaId), new Pair<>(propertyConfig.getMinValue(areaId),
                                    propertyConfig.getMaxValue(areaId)));
                        }
                        carZones.add(convertAreaIdToCarZones(areaType, areaId));
                    }

                    if (propertyConfig.getConfigArray().size() != 0
                            && propertyId == HVAC_TEMPERATURE_SET) {
                        carPropertyProfile.add(CarPropertyProfile.builder()
                                .setPropertyId(propertyId)
                                .setCelsiusRange(new Pair<>(
                                        (propertyConfig.getConfigArray().get(0)
                                                / TEMPERATURE_CONFIG_DENOMINATION),
                                        (propertyConfig.getConfigArray().get(1)
                                                / TEMPERATURE_CONFIG_DENOMINATION)))
                                .setFahrenheitRange(new Pair<>(
                                        (propertyConfig.getConfigArray().get(3)
                                                / TEMPERATURE_CONFIG_DENOMINATION),
                                        (propertyConfig.getConfigArray().get(4)
                                                / TEMPERATURE_CONFIG_DENOMINATION)))
                                .setCelsiusIncrement(propertyConfig.getConfigArray().get(2)
                                        / TEMPERATURE_CONFIG_DENOMINATION)
                                .setFahrenheitIncrement(
                                        propertyConfig.getConfigArray().get(5)
                                                / TEMPERATURE_CONFIG_DENOMINATION)
                                .setStatus(STATUS_SUCCESS)
                                .build());
                    } else {
                        carPropertyProfile.add(CarPropertyProfile.builder()
                                .setPropertyId(propertyId)
                                .setCarZones(carZones)
                                .setStatus(STATUS_SUCCESS)
                                .setCarZoneSetsToMinMaxRange(minMaxRange).build());
                    }
                }
            } catch (IllegalArgumentException e) {
                errors.add(CarInternalError.create(propertyId, CarValue.STATUS_UNIMPLEMENTED));
            } catch (Exception e) {
                errors.add(CarInternalError.create(propertyId, CarValue.STATUS_UNAVAILABLE));
            }
        }
        for (CarInternalError error : errors.build()) {
            carPropertyProfile.add(CarPropertyProfile.builder()
                    .setPropertyId(error.getPropertyId())
                    .setStatus(error.getErrorCode())
                    .build());
        }
        listener.onGetCarPropertyProfiles(carPropertyProfile);
    }

    /**
     * Registers for the property updates at the input sampling rate.
     *
     * @param propertyId    property id in {@link android.car.VehiclePropertyIds}
     * @param sampleRate    float value in hertz
     * @throws IllegalArgumentException if a property is not implemented in the car
     */
    public void registerProperty(int propertyId, float sampleRate) {
        Log.i(LogTags.TAG_CAR_HARDWARE,
                "Attempting registration for the property: " + propertyId + " at sample rate: "
                        + sampleRate);
        if (getPropertyConfig(propertyId) == null) {
            throw new IllegalArgumentException("Property is not implemented in the car: "
                    + propertyId);
        }
        boolean registerCallback =
                mCarPropertyManager.registerCallback(mPropertyEventCallback,
                        propertyId,
                        sampleRate);
        Log.i(LogTags.TAG_CAR_HARDWARE,
                "Registration completed in CarPropertyManager with success status: "
                        + registerCallback);
    }

    /**
     * Unregisters from the property updates.
     *
     * @param propertyId    property id in {@link android.car.VehiclePropertyIds}
     * @throws IllegalArgumentException if a property is not implemented in the car
     */
    public void unregisterProperty(int propertyId) {
        if (getPropertyConfig(propertyId) == null) {
            throw new IllegalArgumentException("Property is not implemented in the car: "
                    + propertyId);
        }
        mCarPropertyManager.unregisterCallback(mPropertyEventCallback, propertyId);
    }

    PropertyRequestProcessor(Context context, PropertyEventCallback callback) {
        Car car = Car.createCar(context);
        mCarPropertyManager = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
        mPropertyEventCallback = callback;
    }

    @SuppressWarnings("rawtypes")
    @Nullable
    private CarPropertyConfig<?> getPropertyConfig(int propertyId) {
        ArraySet<Integer> propertySet = new ArraySet<>(1);
        propertySet.add(propertyId);
        List<CarPropertyConfig> configs = mCarPropertyManager.getPropertyList(propertySet);
        return configs.size() == 0 ? null : configs.get(0);
    }
}