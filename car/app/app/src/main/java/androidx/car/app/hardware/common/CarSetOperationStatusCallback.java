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

package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.climate.ClimateProfileRequest;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A callback for status being returned from the car hardware after completing the associated set
 * operation.
 */
@CarProtocol
@RequiresCarApi(5)
@ExperimentalCarApi
public interface CarSetOperationStatusCallback {
    /**
     * Defines the possible status code for the set operation.
     *
     */
    @IntDef({
            OPERATION_STATUS_SUCCESS,
            OPERATION_STATUS_FEATURE_UNIMPLEMENTED,
            OPERATION_STATUS_FEATURE_UNSUPPORTED,
            OPERATION_STATUS_FEATURE_TEMPORARILY_UNAVAILABLE,
            OPERATION_STATUS_FEATURE_SETTING_NOT_ALLOWED,
            OPERATION_STATUS_UNSUPPORTED_VALUE,
            OPERATION_STATUS_INSUFFICIENT_PERMISSION,
            OPERATION_STATUS_ILLEGAL_CAR_HARDWARE_STATE,
            OPERATION_STATUS_UPDATE_TIMEOUT,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    @interface StatusCode {
    }

    /** Set operation succeeded.*/
    int OPERATION_STATUS_SUCCESS = 0;

    /**
     * The feature is not implemented in this vehicle.
     *
     * <p>For example, if applications try to turn on the electric defroster in the car without
     * this feature, they will get this status code.
     */
    int OPERATION_STATUS_FEATURE_UNIMPLEMENTED = 1;

    /**
     * The feature is not supported in this zones.
     *
     * <p>For example, if applications try to turn on the electric defroster for the front window,
     * but the car only has the electric defroster for the rear window, they will get this status
     * code.
     */
    int OPERATION_STATUS_FEATURE_UNSUPPORTED = 2;

    /**
     * The feature is temporarily unavailable in the specified zone.
     *
     * <p>For example, if applications try to open the car door when the car is moving, they might
     * get this status code.
     */
    int OPERATION_STATUS_FEATURE_TEMPORARILY_UNAVAILABLE = 3;

    /** The feature is read-only feature in the vehicle. */
    int OPERATION_STATUS_FEATURE_SETTING_NOT_ALLOWED = 4;

    /**
     * The caller-provided value is not one of the supported values.
     *
     * <p>For example, if the car only support seat heating levels as 0, 1, 2. In this case, when
     * the application set seat heating level as 3, it will get this status code.
     */
    int OPERATION_STATUS_UNSUPPORTED_VALUE = 5;

    /**
     * The feature is writeable in the car, but the application does not have sufficient
     * Android-level permission to associated it.
     */
    int OPERATION_STATUS_INSUFFICIENT_PERMISSION = 6;

    /** The car returned an invalid value that cannot be interpreted.*/
    int OPERATION_STATUS_ILLEGAL_CAR_HARDWARE_STATE = 7;

    /** Failed to update to desired value in a zone within the allotted time. */
    int OPERATION_STATUS_UPDATE_TIMEOUT = 8;

    /**
     * Returns a human readable string value for the status code.
     *
     * @param statusCode                one of status codes in StatusCode
     * @throws IllegalArgumentException if status code is not in StatusCode
     */
    static @NonNull String toString(@StatusCode int statusCode) {
        switch (statusCode) {
            case OPERATION_STATUS_SUCCESS:
                return "OPERATION_STATUS_SUCCESS";
            case OPERATION_STATUS_FEATURE_UNIMPLEMENTED:
                return "OPERATION_STATUS_FEATURE_UNIMPLEMENTED";
            case OPERATION_STATUS_FEATURE_UNSUPPORTED:
                return "OPERATION_STATUS_FEATURE_UNSUPPORTED";
            case OPERATION_STATUS_FEATURE_TEMPORARILY_UNAVAILABLE:
                return "OPERATION_STATUS_FEATURE_TEMPORARILY_UNAVAILABLE";
            case OPERATION_STATUS_FEATURE_SETTING_NOT_ALLOWED:
                return "OPERATION_STATUS_FEATURE_SETTING_NOT_ALLOWED";
            case OPERATION_STATUS_UNSUPPORTED_VALUE:
                return "OPERATION_STATUS_UNSUPPORTED_VALUE";
            case OPERATION_STATUS_INSUFFICIENT_PERMISSION:
                return "OPERATION_STATUS_INSUFFICIENT_PERMISSION";
            case OPERATION_STATUS_ILLEGAL_CAR_HARDWARE_STATE:
                return "OPERATION_STATUS_ILLEGAL_CAR_HARDWARE_STATE";
            case OPERATION_STATUS_UPDATE_TIMEOUT:
                return "OPERATION_STATUS_UPDATE_TIMEOUT";
            default:
                throw new IllegalArgumentException("Invalid status code");
        }
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_HVAC_POWER} succeeded or failed.
     *
     * @param statusCode        one of status codes in StatusCode
     */
    default void onSetCarClimateStateHvacPower(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_HVAC_AC} succeeded or failed.
     *
     * @param statusCode        one of status codes in StatusCode
     */
    default void onSetCarClimateStateHvacAc(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_HVAC_MAX_AC} succeeded or failed.
     *
     * @param statusCode            one of status codes in StatusCode
     */
    default void onSetCarClimateStateHvacMaxAcMode(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_CABIN_TEMPERATURE} succeeded or failed.
     *
     * @param statusCode                one of status codes in StatusCode
     */
    default void onSetCarClimateStateCabinTemperature(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_FAN_SPEED} succeeded or failed.
     *
     * @param statusCode            one of status codes in StatusCode
     */
    default void onSetCarClimateStateFanSpeedLevel(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_FAN_DIRECTION} succeeded or failed.
     *
     * @param statusCode            one of status codes in StatusCode
     */
    default void onSetCarClimateStateFanDirection(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_SEAT_TEMPERATURE_LEVEL} succeeded or failed.
     *
     * @param statusCode                    one of status codes in StatusCode
     */
    default void onSetCarClimateStateSeatTemperatureLevel(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_SEAT_VENTILATION_LEVEL} succeeded or failed.
     *
     * @param statusCode                    one of status codes in StatusCode
     */
    default void onSetCarClimateStateSeatVentilationLevel(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_STEERING_WHEEL_HEAT} succeeded or failed.
     *
     * @param statusCode                one of status codes in StatusCode
     */
    default void onSetCarClimateStateSteeringWheelHeat(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_HVAC_RECIRCULATION} succeeded or failed.
     *
     * @param statusCode                one of status codes in StatusCode
     */
    default void onSetCarClimateStateHvacRecirculation(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_HVAC_AUTO_RECIRCULATION} succeeded or failed.
     *
     * @param statusCode                    one of status codes in StatusCode
     */
    default void onSetCarClimateStateHvacAutoRecirculation(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_HVAC_AUTO_MODE} succeeded or failed.
     *
     * @param statusCode            one of status codes in StatusCode
     */
    default void onSetCarClimateStateHvacAutoMode(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_HVAC_DUAL_MODE} succeeded or failed.
     *
     * @param statusCode            one of status codes in StatusCode
     */
    default void onSetCarClimateStateHvacDualMode(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_HVAC_DEFROSTER} succeeded or failed.
     *
     * @param statusCode        one of status codes in StatusCode
     */
    default void onSetCarClimateStateDefroster(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_HVAC_MAX_DEFROSTER} succeeded or failed.
     *
     * @param statusCode            one of status codes in StatusCode
     */
    default void onSetCarClimateStateMaxDefroster(@StatusCode int statusCode) {
    }

    /**
     * Notifies that set operation for
     * {@link ClimateProfileRequest#FEATURE_HVAC_ELECTRIC_DEFROSTER} succeeded or failed.
     *
     * @param statusCode                one of status codes in StatusCode
     */
    default void onSetCarClimateStateElectricDefroster(@StatusCode int statusCode) {
    }
}
