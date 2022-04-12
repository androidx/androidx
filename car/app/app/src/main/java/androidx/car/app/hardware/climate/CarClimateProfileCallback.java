/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.hardware.climate;

import androidx.annotation.NonNull;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarZone;

import java.util.List;

/**
 * A callback for the car climate profiles returned from the car hardware, for example, Fan speed,
 * Temperature etc.
 */
@CarProtocol
@RequiresCarApi(5)
@ExperimentalCarApi
public interface CarClimateProfileCallback {
    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_POWER} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * power independently in multiple zones. Returns an empty list if the car can not support the
     * feature.
     *
     * @param supportedCarZones a list of CarZones which are controlled together by the
     *                          car climate system
     */
    default void onHvacPowerProfileAvailable(@NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_AC} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * AC mode independently in multiple zones. Returns an empty list if the car can not support the
     * feature.
     *
     * @param supportedCarZones a list of CarZones which are controlled together by the
     *                          car climate system
     */
    default void onHvacAcProfileAvailable(@NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_MAX_AC} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * MAX AC mode independently in multiple zones. Returns an empty list if the car can not
     * support the feature.
     *
     * @param supportedCarZones a list of CarZones which are controlled together by the
     *                          car climate system
     */
    default void onHvacMaxAcModeProfileAvailable(@NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_CABIN_TEMPERATURE} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can set different
     * temperatures independently for different groups of zones. Returns empty lists if the car can
     * not support the feature. The temperature units can be Celsius or Fahrenheit.
     *
     * @param isTemperatureUnitInMetricSystem    a boolean value to determine unit type for
     *                                           temperatures where value false represents
     *                                           Fahrenheit and true represents Celsius(metric
     *                                           system)
     * @param supportedTemperatures              all supported temperatures in Celsius or Fahrenheit
     *                                           or both for the car zones
     * @param supportedCarZones                  a list of CarZones which are controlled
     *                                           together by the car climate system
     */
    default void onCabinTemperatureProfileAvailable(
            boolean isTemperatureUnitInMetricSystem,
            @NonNull List<Float> supportedTemperatures,
            @NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_FAN_SPEED} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can set different
     * fan speed levels for different groups of zones. Returns empty lists if the car can not
     * support the feature.
     *
     * @param supportedFanSpeedLevels   a list of {@code Integer}s indicates supported fan speed
     *                                  levels in the car
     * @param supportedCarZones         a list of CarZones which are controlled together
     *                                  by the car climate system
     */
    default void onFanSpeedLevelProfileAvailable(@NonNull List<Integer> supportedFanSpeedLevels,
            @NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_FAN_DIRECTION} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can set different
     * fan directions for different groups of zones. Returns empty lists if the car can not
     * support the feature.
     *
     * @param supportedFanDirections    a list of {@code Integer}s indicates supported fan
     *                                  directions in the car
     * @param supportedCarZones         a list of CarZones which are controlled together
     *                                  by the car climate system
     */
    default void onFanDirectionProfileAvailable(@NonNull List<Integer> supportedFanDirections,
            @NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_SEAT_TEMPERATURE_LEVEL} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can set different
     * seat temperature levels for different groups of zones. Returns empty lists if the car can
     * not support the feature.
     * <p>The return list can have negative and positive values. Negative values indicate
     * cooling level. Positive values indicates heating level.
     *
     * @param supportedSeatTemperatureLevels    a list of {@code Integer}s indicates supported
     *                                          seat teamperature levels in the car
     * @param supportedCarZones                 a list of CarZones which are controlled
     *                                          together by the car climate system
     */
    default void onSeatTemperatureLevelProfileAvailable(
            @NonNull List<Integer> supportedSeatTemperatureLevels,
            @NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_SEAT_VENTILATION_LEVEL} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can set different
     * seat ventilation levels for different groups of zones. Returns empty lists if the car can
     * not support the feature.
     *
     * @param supportedSeatVentilationLevels    a list of {@code Integer}s indicates supported
     *                                          seat ventilation levels in the car
     * @param supportedCarZones                 a list of CarZones which are controlled
     *                                          together by the car climate system
     */
    default void onSeatVentilationLevelProfileAvailable(
            @NonNull List<Integer> supportedSeatVentilationLevels,
            @NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_STEERING_WHEEL_HEAT} is available.
     *
     * @param supportedCarZones should only contains CarZone#CAR_ZONE_GLOBAL or an empty
     *                         list if the feature is not supported in the car
     */
    default void onSteeringWheelHeatProfileAvailable(@NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_RECIRCULATION} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * recirculation independently in multiple zones. Returns an empty list if the car can not
     * support the feature.
     *
     * @param supportedCarZones a list of CarZones which are controlled together by the
     *                          car climate system
     */
    default void onHvacRecirculationProfileAvailable(@NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_AUTO_RECIRCULATION} is
     * available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * AUTO recirculation independently in multiple zones. Returns an empty list if the car can not
     * support the feature.
     *
     * @param supportedCarZones a list of CarZones which are controlled together by the
     *                          car climate system
     */
    default void onHvacAutoRecirculationProfileAvailable(
            @NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_AUTO_MODE} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * AUTO mode independently in multiple zones. Returns an empty list if the car can not
     * support the feature.
     *
     * @param supportedCarZones a list of CarZones which are controlled together by the
     *                          car climate system
     */
    default void onHvacAutoModeProfileAvailable(@NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_DUAL_MODE} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * DUAL mode independently in multiple zones. Returns an empty list if the car can not
     * support the feature.
     *
     * @param supportedCarZones a list of CarZones which are controlled together by the
     *                          car climate system
     */
    default void onHvacDualModeProfileAvailable(@NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_DEFROSTER} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the
     * defroster independently in multiple zones. Returns an empty list if the car can not
     * support the feature.
     * <p>A CarZone with CarZone#CAR_ZONE_ROW_FIRST indicates the front window.
     * The rear window's zone will have row value as CarZone#CAR_ZONE_ROW_EXCLUDE_FIRST.
     *
     * @param supportedCarZones a list of CarZones which are controlled together by the
     *                          car climate system
     */
    default void onDefrosterProfileAvailable(@NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_MAX_DEFROSTER} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * max defroster independently in multiple zones. Returns an empty list if the car can not
     * support the feature.
     * <p>A CarZone with CarZone#CAR_ZONE_ROW_FIRST indicates the front window.
     * The rear window's zone will have row value as CarZone#CAR_ZONE_ROW_EXCLUDE_FIRST.
     *
     * @param supportedCarZones a list of CarZones which are controlled together by the
     *                          car climate system
     */
    default void onMaxDefrosterProfileAvailable(@NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_ELECTRIC_DEFROSTER} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * electric defroster independently in multiple zones. Returns an empty list if the car can not
     * support the feature.
     * <p>A CarZone with CarZone#CAR_ZONE_ROW_FIRST indicates the front window.
     * The rear window's zone will have row value as CarZone#CAR_ZONE_ROW_EXCLUDE_FIRST.
     *
     * @param supportedCarZones a list of CarZones which are controlled together by the
     *                          car climate system
     */
    default void onElectricDefrosterProfileAvailable(@NonNull List<CarZone> supportedCarZones) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_CAR_ZONE_MAPPING} is available.
     *
     * <p>Applications will get multiple callbacks. For each callback, it returns a {@code
     * CarZone seatsGroupedZone} and a {@code List<CarZone>} which contains all individual seats in
     * this{@code seatsGroupedZone}.
     *
     * @param seatGroupedZone   a group of multiple seats represented together as a CarZone.
     * @param supportedCarZones a list of CarZones which indicates individual seats in
     *                          the {@code seatsGroupedZone}
     */
    default void onCarZoneMappingInfoProfileAvailable(@NonNull CarZone seatGroupedZone,
            @NonNull List<CarZone> supportedCarZones) {
    }
}
