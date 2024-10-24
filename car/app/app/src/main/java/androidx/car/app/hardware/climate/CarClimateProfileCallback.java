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

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;

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
     * @param hvacPowerProfile an object of {@code HvacPowerProfile} class containing
     *                         information about the feature's supported zones
     */
    default void onHvacPowerProfileAvailable(@NonNull HvacPowerProfile hvacPowerProfile) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_AC} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * AC mode independently in multiple zones. Returns an empty list if the car can not support the
     * feature.
     *
     * @param hvacAcProfile an object of {@code HvacAcProfile} class containing information
     *                      about the feature's supported zones
     */
    default void onHvacAcProfileAvailable(@NonNull HvacAcProfile hvacAcProfile) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_MAX_AC} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * MAX AC mode independently in multiple zones. Returns an empty list if the car can not
     * support the feature.
     *
     * @param hvacMaxAcModeProfile an object of {@code HvacMaxAcModeProfile} class containing
     *                             information about the feature's supported zones
     */
    default void onHvacMaxAcModeProfileAvailable(
            @NonNull HvacMaxAcModeProfile hvacMaxAcModeProfile) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_CABIN_TEMPERATURE} is available.
     *
     * <p>Applications will get a single callback containing a list of different temperatures in
     * Celsius and Fahrenheit for different groups of zones. Returns empty lists if the car can
     * not support the feature.
     *s
     * @param cabinTemperatureProfile   an object of {@code CabinTemperatureProfile} class
     *                                  containing information about the feature's supported values
     */
    default void onCabinTemperatureProfileAvailable(
            @NonNull CabinTemperatureProfile cabinTemperatureProfile) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_FAN_SPEED} is available.
     *
     * <p>Applications will get a single callback containing a list of different fan speed levels
     * for different groups of zones. Returns empty lists if the car cannot support the feature.
     *
     * @param fanSpeedLevelProfile   an object of {@code FanSpeedLevelProfile} class
     *                               containing information about the feature's supported values
     */
    default void onFanSpeedLevelProfileAvailable(
            @NonNull FanSpeedLevelProfile fanSpeedLevelProfile) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_FAN_DIRECTION} is available.
     *
     * <p>Applications will get a single callback containing a list of different fan directions
     * for different groups of zones. Returns empty lists if the car cannot support the feature.
     *
     * @param fanDirectionProfile    an object of {@code FanDirectionProfile} class containing
     *                               information about the feature's supported values
     */
    default void onFanDirectionProfileAvailable(@NonNull FanDirectionProfile fanDirectionProfile) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_SEAT_TEMPERATURE_LEVEL} is available.
     *
     * <p>Applications will get a single callback containing a list of different seat temperature
     * levels for different groups of zones. Returns empty lists if the car cannot support the
     * feature.
     * <p>The return list can have negative and positive values. Negative values indicate
     * cooling level. Positive values indicates heating level.
     *
     * @param seatTemperatureProfile    an object of {@code SeatTemperatureProfile} class
     *                                  containing information about the feature's supported values
     */
    default void onSeatTemperatureLevelProfileAvailable(
            @NonNull SeatTemperatureProfile seatTemperatureProfile) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_SEAT_VENTILATION_LEVEL} is available.
     *
     * <p>Applications will get a single callback containing a list of different seat ventilation
     * levels for different groups of zones. Returns empty lists if the car cannot support the
     * feature.
     *
     * @param seatVentilationProfile   an object of {@code SeatVentilationProfile} class
     *                                 containing information about the feature's supported values
     */
    default void onSeatVentilationLevelProfileAvailable(
            @NonNull SeatVentilationProfile seatVentilationProfile) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_STEERING_WHEEL_HEAT} is available.
     *
     * @param steeringWheelHeatProfile an object of {@code SteeringWheelHeatProfile} class
     *                                 containing information about the feature's supported values
     */
    default void onSteeringWheelHeatProfileAvailable(
            @NonNull SteeringWheelHeatProfile steeringWheelHeatProfile) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_RECIRCULATION} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * recirculation independently in multiple zones. Returns an empty list if the car can not
     * support the feature.
     *
     * @param hvacRecirculationProfile an object of {@code HvacRecirculationProfile} class
     *                                 containing information about the feature's supported zones
     */
    default void onHvacRecirculationProfileAvailable(
            @NonNull HvacRecirculationProfile hvacRecirculationProfile) {
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
     * @param hvacAutoRecirculationProfile an object of {@code HvacAutoRecirculationProfile}
     *                                    class containing information about the feature's
     *                                     supported zones
     */
    default void onHvacAutoRecirculationProfileAvailable(
            @NonNull HvacAutoRecirculationProfile hvacAutoRecirculationProfile) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_AUTO_MODE} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * AUTO mode independently in multiple zones. Returns an empty list if the car can not
     * support the feature.
     *
     * @param hvacAutoModeProfile an object of {@code HvacAutoModeProfile} class containing
     *                            information about the feature's supported zones
     */
    default void onHvacAutoModeProfileAvailable(@NonNull HvacAutoModeProfile hvacAutoModeProfile) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_HVAC_DUAL_MODE} is available.
     *
     * <p>Applications will get multiple callbacks if the car climate system can control the HVAC
     * DUAL mode independently in multiple zones. Returns an empty list if the car can not
     * support the feature.
     *
     * @param hvacDualModeProfile an object of {@code HvacDualModeProfile} class containing
     *                            information about the feature's supported zones
     */
    default void onHvacDualModeProfileAvailable(@NonNull HvacDualModeProfile hvacDualModeProfile) {
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
     * @param defrosterProfile an object of {@code DefrosterProfile} class containing information
     *                        about the feature's supported zones
     */
    default void onDefrosterProfileAvailable(@NonNull DefrosterProfile defrosterProfile) {
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
     * @param maxDefrosterProfile an object of {@code MaxDefrosterProfile} class containing
     *                            information about the feature's supported zones
     */
    default void onMaxDefrosterProfileAvailable(@NonNull MaxDefrosterProfile maxDefrosterProfile) {
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
     * @param electricDefrosterProfile an object of {@code ElectricDefrosterProfile} class
     *                                 containing information about the feature's supported zones
     */
    default void onElectricDefrosterProfileAvailable(
            @NonNull ElectricDefrosterProfile electricDefrosterProfile) {
    }

    /**
     * Notifies that the profile information for
     * {@link ClimateProfileRequest#FEATURE_CAR_ZONE_MAPPING} is available.
     *
     * <p>Applications will get multiple callbacks. For each callback, it returns a {@code
     * CarZone seatsGroupedZone} and a {@code List<CarZone>} which contains all individual seats in
     * this{@code seatsGroupedZone}.
     *
     * @param carZoneMappingInfoProfile   an object of {@code CarZoneMappingInfoProfile} class
     *                                    containing information about the feature's supported zones
     */
    default void onCarZoneMappingInfoProfileAvailable(
            @NonNull CarZoneMappingInfoProfile carZoneMappingInfoProfile) {
    }
}