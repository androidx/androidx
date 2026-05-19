/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.wear.health

import android.Manifest
import android.health.connect.HealthPermissions
import androidx.annotation.RequiresPermission
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.ri
import androidx.glance.wear.ExperimentalGlanceWearApi

/**
 * A representation of health data managed by Health Services.
 *
 * This provides access to various health metrics that can be used in Glance Wear Widgets, such as
 * heart rate and daily activity totals.
 */
@ExperimentalGlanceWearApi
public object DataTypes {

    // These constants are kept in sync with the widget renderer and the keys used by other system
    // components, including those responsible for rendering in low-power or ambient states.
    private const val HEART_RATE_BPM_METRIC_KEY = "metrics.heart_rate"
    private const val HEART_RATE_ACCURACY_METRIC_KEY = "metrics.heart_rate_accuracy"
    private const val DAILY_STEPS_METRIC_KEY = "metrics.day.steps"
    private const val DAILY_CALORIES_METRIC_KEY = "metrics.day.calories"
    private const val DAILY_DISTANCE_METERS_METRIC_KEY = "metrics.day.distance"
    private const val DAILY_FLOORS_METRIC_KEY = "metrics.day.flights_of_stairs"
    private const val DAILY_STEPS_AVAILABLE_METRIC_KEY = "metrics.day.steps_available"
    private const val DAILY_CALORIES_AVAILABLE_METRIC_KEY = "metrics.day.calories_available"
    private const val DAILY_DISTANCE_METERS_AVAILABLE_METRIC_KEY = "metrics.day.distance_available"
    private const val DAILY_FLOORS_AVAILABLE_METRIC_KEY = "metrics.day.flights_of_stairs_available"

    private const val UNSET_INT = -1
    private const val UNSET_FLOAT = -1f

    /** Heart rate accuracy is unknown. */
    @JvmField public val HEART_RATE_ACCURACY_UNKNOWN: RemoteInt = 0.ri

    /** Heart rate cannot be acquired because the sensor is not properly contacting skin. */
    @JvmField public val HEART_RATE_ACCURACY_NO_CONTACT: RemoteInt = 1.ri

    /** Heart rate data is currently too unreliable to be used. */
    @JvmField public val HEART_RATE_ACCURACY_UNRELIABLE: RemoteInt = 2.ri

    /** Heart rate data is available but the accuracy is low. */
    @JvmField public val HEART_RATE_ACCURACY_LOW: RemoteInt = 3.ri

    /** Heart rate data is available and the accuracy is medium. */
    @JvmField public val HEART_RATE_ACCURACY_MEDIUM: RemoteInt = 4.ri

    /** Heart rate data is available with high accuracy. */
    @JvmField public val HEART_RATE_ACCURACY_HIGH: RemoteInt = 5.ri

    /**
     * Current heart rate, in beats per minute.
     *
     * Required permissions are:
     * * API < 36: [Manifest.permission.BODY_SENSORS]
     * * API >= 36: [HealthPermissions.READ_HEART_RATE]
     */
    @get:RequiresPermission(
        anyOf = [Manifest.permission.BODY_SENSORS, HealthPermissions.READ_HEART_RATE]
    )
    public val heartRateBpm: RemoteFloat =
        RemoteFloat.createNamedRemoteFloat(
            name = HEART_RATE_BPM_METRIC_KEY,
            defaultValue = UNSET_FLOAT,
        )

    /**
     * Heart rate sensor accuracy data. The accuracy value is one of `HEART_RATE_ACCURACY_*`
     * constants.
     *
     * Required permissions are:
     * * API < 36: [Manifest.permission.BODY_SENSORS]
     * * API >= 36: [HealthPermissions.READ_HEART_RATE]
     */
    @get:RequiresPermission(
        anyOf = [Manifest.permission.BODY_SENSORS, HealthPermissions.READ_HEART_RATE]
    )
    public val heartRateAccuracy: RemoteInt =
        RemoteInt.createNamedRemoteInt(
            name = HEART_RATE_ACCURACY_METRIC_KEY,
            defaultValue = HEART_RATE_ACCURACY_UNKNOWN.constantValue,
        )

    /**
     * The total step count over a day.
     *
     * It resets when 00:00 is reached in the device's current timezone. This can result in the
     * daily period being greater or less than 24 hours if the timezone is changed.
     *
     * Check [isDailyStepsAvailable] to determine if this metric is available on the host.
     *
     * Required permission: [Manifest.permission.ACTIVITY_RECOGNITION]
     */
    @get:RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    public val dailySteps: RemoteInt =
        RemoteInt.createNamedRemoteInt(name = DAILY_STEPS_METRIC_KEY, defaultValue = UNSET_INT)

    /**
     * Returns `true` if [dailySteps] is available on the host, `false` otherwise.
     *
     * A health data type may be unavailable if the permission wasn't granted or if the host cannot
     * retrieve it.
     *
     * If `false`, [dailySteps] will return its default value.
     */
    public val isDailyStepsAvailable: RemoteBoolean =
        RemoteBoolean.createNamedRemoteBoolean(
            name = DAILY_STEPS_AVAILABLE_METRIC_KEY,
            defaultValue = false,
        )

    /**
     * The total number of calories burned over a day (including both BMR and active calories),
     * expressed in kilocalories (kcal).
     *
     * It resets when 00:00 is reached in the device's current timezone. This can result in the
     * daily period being greater or less than 24 hours if the timezone is changed.
     *
     * Check [isDailyCaloriesAvailable] to determine if this metric is available on the host.
     *
     * Required permission: [Manifest.permission.ACTIVITY_RECOGNITION]
     */
    @get:RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    public val dailyCalories: RemoteFloat =
        RemoteFloat.createNamedRemoteFloat(
            name = DAILY_CALORIES_METRIC_KEY,
            defaultValue = UNSET_FLOAT,
        )

    /**
     * Returns `true` if [dailyCalories] is available on the host, `false` otherwise.
     *
     * A health data type may be unavailable if the permission wasn't granted or if the host cannot
     * retrieve it.
     *
     * If `false`, [dailyCalories] will return its default value.
     */
    public val isDailyCaloriesAvailable: RemoteBoolean =
        RemoteBoolean.createNamedRemoteBoolean(
            name = DAILY_CALORIES_AVAILABLE_METRIC_KEY,
            defaultValue = false,
        )

    /**
     * The total distance traveled over a day, expressed in meters.
     *
     * It resets when 00:00 is reached in the device's current timezone. This can result in the
     * daily period being greater or less than 24 hours if the timezone is changed.
     *
     * Check [isDailyDistanceMetersAvailable] to determine if this metric is available on the host.
     *
     * Required permission: [Manifest.permission.ACTIVITY_RECOGNITION]
     */
    @get:RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    public val dailyDistanceMeters: RemoteFloat =
        RemoteFloat.createNamedRemoteFloat(
            name = DAILY_DISTANCE_METERS_METRIC_KEY,
            defaultValue = UNSET_FLOAT,
        )

    /**
     * Returns `true` if [dailyDistanceMeters] is available on the host, `false` otherwise.
     *
     * A health data type may be unavailable if the permission wasn't granted or if the host cannot
     * retrieve it.
     *
     * If `false`, [dailyDistanceMeters] will return its default value.
     */
    public val isDailyDistanceMetersAvailable: RemoteBoolean =
        RemoteBoolean.createNamedRemoteBoolean(
            name = DAILY_DISTANCE_METERS_AVAILABLE_METRIC_KEY,
            defaultValue = false,
        )

    /**
     * The total number of floors climbed over a day.
     *
     * It resets when 00:00 is reached in the device's current timezone. This can result in the
     * daily period being greater or less than 24 hours if the timezone is changed.
     *
     * Check [isDailyFloorsAvailable] to determine if this metric is available on the host.
     *
     * Required permission: [Manifest.permission.ACTIVITY_RECOGNITION]
     */
    @get:RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    public val dailyFloors: RemoteFloat =
        RemoteFloat.createNamedRemoteFloat(
            name = DAILY_FLOORS_METRIC_KEY,
            defaultValue = UNSET_FLOAT,
        )

    /**
     * Returns `true` if [dailyFloors] is available on the host, `false` otherwise.
     *
     * A health data type may be unavailable if the permission wasn't granted or if the host cannot
     * retrieve it.
     *
     * If `false`, [dailyFloors] will return its default value.
     */
    public val isDailyFloorsAvailable: RemoteBoolean =
        RemoteBoolean.createNamedRemoteBoolean(
            name = DAILY_FLOORS_AVAILABLE_METRIC_KEY,
            defaultValue = false,
        )
}
