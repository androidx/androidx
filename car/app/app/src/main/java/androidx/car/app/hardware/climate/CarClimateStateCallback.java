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
import androidx.car.app.hardware.common.CarValue;

import org.jspecify.annotations.NonNull;

/**
 * A callback for the car climate state returned from the car hardware, for example, Fan speed,
 * Temperature etc.
 */
@CarProtocol
@RequiresCarApi(5)
@ExperimentalCarApi
public interface CarClimateStateCallback {
    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_HVAC_POWER} is updated.
     *
     * @param hvacPowerState the updated state of hvacPowerState
     */
    default void onHvacPowerStateAvailable(@NonNull CarValue<Boolean> hvacPowerState) {
    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_HVAC_AC} is updated.
     *
     * @param hvacAcState   the updated state of hvacAcState
     */
    default void onHvacAcStateAvailable(@NonNull CarValue<Boolean> hvacAcState) {
    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_HVAC_MAX_AC} is updated.
     *
     * @param hvacMaxAcModeState    the updated state of hvacMaxAcModeState
     */
    default void onHvacMaxAcModeStateAvailable(@NonNull CarValue<Boolean> hvacMaxAcModeState) {
    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_CABIN_TEMPERATURE} is updated.
     *
     * @param hvacCabinTemperature  the updated state of hvacCabinTemperature
     */
    default void onCabinTemperatureStateAvailable(@NonNull CarValue<Float> hvacCabinTemperature) {
    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_FAN_SPEED} is updated.
     *
     * @param fanSpeedLevel the updated state of fanSpeedLevel
     */
    default void onFanSpeedLevelStateAvailable(@NonNull CarValue<Integer> fanSpeedLevel) {
    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_FAN_DIRECTION} is updated.
     *
     * @param fanDirection  the updated state of fanDirection
     */
    default void onFanDirectionStateAvailable(@NonNull CarValue<Integer> fanDirection) {
    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_SEAT_TEMPERATURE_LEVEL} is updated.
     *
     * @param seatTemperatureLevel  the updated state of seatTemperatureLevel
     */
    default void onSeatTemperatureLevelStateAvailable(
            @NonNull CarValue<Integer> seatTemperatureLevel) {
    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_SEAT_VENTILATION_LEVEL} is updated.
     *
     * @param seatVentilationLevel  the updated state of seatVentilationLevel
     */
    default void onSeatVentilationLevelStateAvailable(
            @NonNull CarValue<Integer> seatVentilationLevel) {
    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_STEERING_WHEEL_HEAT} is updated.
     *
     * @param steeringWheelHeatState    the updated state of steeringWheelHeatState
     */
    default void onSteeringWheelHeatStateAvailable(
            @NonNull CarValue<Boolean> steeringWheelHeatState) {
    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_HVAC_RECIRCULATION} is updated.
     *
     * @param hvacRecirculationState    the updated state of hvacRecirculationState
     */
    default void onHvacRecirculationStateAvailable(
            @NonNull CarValue<Boolean> hvacRecirculationState) {
    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_HVAC_AUTO_RECIRCULATION} is
     * updated.
     *
     * @param hvacAutoRecirculationState    the updated state of hvacAutoRecirculationState
     */
    default void onHvacAutoRecirculationStateAvailable(
            @NonNull CarValue<Boolean> hvacAutoRecirculationState) {
    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_HVAC_AUTO_MODE} is updated.
     *
     * @param hvacAutoModeState the updated state of hvacAutoModeState
     */
    default void onHvacAutoModeStateAvailable(@NonNull CarValue<Boolean> hvacAutoModeState) {
    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_HVAC_DUAL_MODE} is updated.
     *
     * @param hvacDualModeState the updated state of hvacDualModeState
     */
    default void onHvacDualModeStateAvailable(@NonNull CarValue<Boolean> hvacDualModeState) {
    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_HVAC_DEFROSTER} is updated.
     *
     * @param defrosterState the updated state of defrosterState
     */
    default void onDefrosterStateAvailable(@NonNull CarValue<Boolean> defrosterState) {

    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_HVAC_MAX_DEFROSTER} is updated.
     *
     * @param maxDefrosterState the updated state of maxDefrosterState
     */
    default void onMaxDefrosterStateAvailable(@NonNull CarValue<Boolean> maxDefrosterState) {

    }

    /**
     * Notifies that the climate state for
     * {@link ClimateProfileRequest#FEATURE_HVAC_ELECTRIC_DEFROSTER} is
     * updated.
     *
     * @param electricDefrosterState the updated state of electricDefrosterState
     */
    default void onElectricDefrosterStateAvailable(
            @NonNull CarValue<Boolean> electricDefrosterState) {

    }
}
