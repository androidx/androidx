/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.connect.client.permission

import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.Record
import kotlin.reflect.KClass

/**
 * A Permission either to read or write data associated with a [Record] type.
 *
 * @see androidx.health.connect.client.PermissionController
 */
public class HealthPermission
internal constructor(
    /** type of [Record] the permission gives access for. */
    internal val recordType: KClass<out Record>,
    /** whether read or write access. */
    @property:AccessType internal val accessType: Int,
) {
    companion object {
        /**
         * Creates [HealthPermission] to read provided [recordType], such as `Steps::class`.
         *
         * @return Permission object to use with
         * [androidx.health.connect.client.PermissionController].
         */
        @JvmStatic
        public fun createReadPermission(recordType: KClass<out Record>): HealthPermission {
            return HealthPermission(recordType, AccessTypes.READ)
        }

        /**
         * Creates [HealthPermission] to write provided [recordType], such as `Steps::class`.
         *
         * @return Permission object to use with
         * [androidx.health.connect.client.PermissionController].
         */
        @JvmStatic
        public fun createWritePermission(recordType: KClass<out Record>): HealthPermission {
            return HealthPermission(recordType, AccessTypes.WRITE)
        }

        // Read permissions for ACTIVITY.
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_ACTIVE_CALORIES_BURNED =
            "android.permission.health.READ_ACTIVE_CALORIES_BURNED"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_DISTANCE = "android.permission.health.READ_DISTANCE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_ELEVATION_GAINED = "android.permission.health.READ_ELEVATION_GAINED"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_EXERCISE = "android.permission.health.READ_EXERCISE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_FLOORS_CLIMBED = "android.permission.health.READ_FLOORS_CLIMBED"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_STEPS = "android.permission.health.READ_STEPS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_TOTAL_CALORIES_BURNED =
            "android.permission.health.READ_TOTAL_CALORIES_BURNED"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_VO2_MAX = "android.permission.health.READ_VO2_MAX"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_WHEELCHAIR_PUSHES = "android.permission.health.READ_WHEELCHAIR_PUSHES"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_POWER = "android.permission.health.READ_POWER"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_SPEED = "android.permission.health.READ_SPEED"

        // Read permissions for BODY_MEASUREMENTS.
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_BASAL_METABOLIC_RATE = "android.permission.health.READ_BASAL_METABOLIC_RATE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_BODY_FAT = "android.permission.health.READ_BODY_FAT"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_BODY_WATER_MASS = "android.permission.health.READ_BODY_WATER_MASS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_BONE_MASS = "android.permission.health.READ_BONE_MASS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_HEIGHT = "android.permission.health.READ_HEIGHT"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_HIP_CIRCUMFERENCE = "android.permission.health.READ_HIP_CIRCUMFERENCE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_LEAN_BODY_MASS = "android.permission.health.READ_LEAN_BODY_MASS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_WAIST_CIRCUMFERENCE = "android.permission.health.READ_WAIST_CIRCUMFERENCE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_WEIGHT = "android.permission.health.READ_WEIGHT"

        // Read permissions for CYCLE_TRACKING.
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_CERVICAL_MUCUS = "android.permission.health.READ_CERVICAL_MUCUS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_MENSTRUATION = "android.permission.health.READ_MENSTRUATION"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_OVULATION_TEST = "android.permission.health.READ_OVULATION_TEST"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_SEXUAL_ACTIVITY = "android.permission.health.READ_SEXUAL_ACTIVITY"

        // Read permissions for NUTRITION.
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_HYDRATION = "android.permission.health.READ_HYDRATION"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_NUTRITION = "android.permission.health.READ_NUTRITION"

        // Read permissions for SLEEP.
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_SLEEP = "android.permission.health.READ_SLEEP"

        // Read permissions for VITALS.
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_BASAL_BODY_TEMPERATURE =
            "android.permission.health.READ_BASAL_BODY_TEMPERATURE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_BLOOD_GLUCOSE = "android.permission.health.READ_BLOOD_GLUCOSE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_BLOOD_PRESSURE = "android.permission.health.READ_BLOOD_PRESSURE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_BODY_TEMPERATURE = "android.permission.health.READ_BODY_TEMPERATURE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_HEART_RATE = "android.permission.health.READ_HEART_RATE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_HEART_RATE_VARIABILITY =
            "android.permission.health.READ_HEART_RATE_VARIABILITY"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_INTERMENSTRUAL_BLEEDING =
            "android.permission.health.READ_INTERMENSTRUAL_BLEEDING"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_OXYGEN_SATURATION = "android.permission.health.READ_OXYGEN_SATURATION"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_RESPIRATORY_RATE = "android.permission.health.READ_RESPIRATORY_RATE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val READ_RESTING_HEART_RATE = "android.permission.health.READ_RESTING_HEART_RATE"

        // Write permissions for ACTIVITY.
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_ACTIVE_CALORIES_BURNED =
            "android.permission.health.WRITE_ACTIVE_CALORIES_BURNED"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_DISTANCE = "android.permission.health.WRITE_DISTANCE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_ELEVATION_GAINED = "android.permission.health.WRITE_ELEVATION_GAINED"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_EXERCISE = "android.permission.health.WRITE_EXERCISE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_FLOORS_CLIMBED = "android.permission.health.WRITE_FLOORS_CLIMBED"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_STEPS = "android.permission.health.WRITE_STEPS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_TOTAL_CALORIES_BURNED =
            "android.permission.health.WRITE_TOTAL_CALORIES_BURNED"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_VO2_MAX = "android.permission.health.WRITE_VO2_MAX"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_WHEELCHAIR_PUSHES = "android.permission.health.WRITE_WHEELCHAIR_PUSHES"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_POWER = "android.permission.health.WRITE_POWER"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_SPEED = "android.permission.health.WRITE_SPEED"

        // Write permissions for BODY_MEASUREMENTS.
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_BASAL_METABOLIC_RATE =
            "android.permission.health.WRITE_BASAL_METABOLIC_RATE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_BODY_FAT = "android.permission.health.WRITE_BODY_FAT"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_BODY_WATER_MASS = "android.permission.health.WRITE_BODY_WATER_MASS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_BONE_MASS = "android.permission.health.WRITE_BONE_MASS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_HEIGHT = "android.permission.health.WRITE_HEIGHT"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_HIP_CIRCUMFERENCE = "android.permission.health.WRITE_HIP_CIRCUMFERENCE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_INTERMENSTRUAL_BLEEDING =
            "android.permission.health.WRITE_INTERMENSTRUAL_BLEEDING"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_LEAN_BODY_MASS = "android.permission.health.WRITE_LEAN_BODY_MASS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_WAIST_CIRCUMFERENCE = "android.permission.health.WRITE_WAIST_CIRCUMFERENCE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_WEIGHT = "android.permission.health.WRITE_WEIGHT"

        // Write permissions for CYCLE_TRACKING.
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_CERVICAL_MUCUS = "android.permission.health.WRITE_CERVICAL_MUCUS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_MENSTRUATION = "android.permission.health.WRITE_MENSTRUATION"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_OVULATION_TEST = "android.permission.health.WRITE_OVULATION_TEST"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_SEXUAL_ACTIVITY = "android.permission.health.WRITE_SEXUAL_ACTIVITY"

        // Write permissions for NUTRITION.
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_HYDRATION = "android.permission.health.WRITE_HYDRATION"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_NUTRITION = "android.permission.health.WRITE_NUTRITION"

        // Write permissions for SLEEP.
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_SLEEP = "android.permission.health.WRITE_SLEEP"

        // Write permissions for VITALS.
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_BASAL_BODY_TEMPERATURE =
            "android.permission.health.WRITE_BASAL_BODY_TEMPERATURE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_BLOOD_GLUCOSE = "android.permission.health.WRITE_BLOOD_GLUCOSE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_BLOOD_PRESSURE = "android.permission.health.WRITE_BLOOD_PRESSURE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_BODY_TEMPERATURE = "android.permission.health.WRITE_BODY_TEMPERATURE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_HEART_RATE = "android.permission.health.WRITE_HEART_RATE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_HEART_RATE_VARIABILITY =
            "android.permission.health.WRITE_HEART_RATE_VARIABILITY"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_OXYGEN_SATURATION = "android.permission.health.WRITE_OXYGEN_SATURATION"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_RESPIRATORY_RATE = "android.permission.health.WRITE_RESPIRATORY_RATE"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val WRITE_RESTING_HEART_RATE = "android.permission.health.WRITE_RESTING_HEART_RATE"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HealthPermission) return false

        if (recordType != other.recordType) return false
        if (accessType != other.accessType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = recordType.hashCode()
        result = 31 * result + accessType
        return result
    }
}
