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
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
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
         *   [androidx.health.connect.client.PermissionController].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY) // To be deleted.
        @JvmStatic
        public fun createReadPermissionLegacy(recordType: KClass<out Record>): HealthPermission {
            return HealthPermission(recordType, AccessTypes.READ)
        }

        /**
         * Returns a permission defined in [HealthPermission] to read provided [recordType], such as
         * `Steps::class`.
         *
         * @return Permission to use with [androidx.health.connect.client.PermissionController].
         * @throws IllegalArgumentException if the given record type is invalid.
         */
        @JvmStatic
        public fun getReadPermission(recordType: KClass<out Record>): String {
            if (RECORD_TYPE_TO_PERMISSION[recordType] == null) {
                throw IllegalArgumentException(
                    "Given recordType is not valid : $recordType.simpleName"
                )
            }
            return READ_PERMISSION_PREFIX + RECORD_TYPE_TO_PERMISSION[recordType]
        }

        /**
         * Creates [HealthPermission] to write provided [recordType], such as `Steps::class`.
         *
         * @return Permission to use with [androidx.health.connect.client.PermissionController].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY) // To be deleted.
        @JvmStatic
        public fun createWritePermissionLegacy(recordType: KClass<out Record>): HealthPermission {
            return HealthPermission(recordType, AccessTypes.WRITE)
        }

        /**
         * Returns a permission defined in [HealthPermission] to read provided [recordType], such as
         * `Steps::class`.
         *
         * @return Permission object to use with
         *   [androidx.health.connect.client.PermissionController].
         * @throws IllegalArgumentException if the given record type is invalid.
         */
        @JvmStatic
        public fun getWritePermission(recordType: KClass<out Record>): String {
            if (RECORD_TYPE_TO_PERMISSION[recordType] == null) {
                throw IllegalArgumentException(
                    "Given recordType is not valid : $recordType.simpleName"
                )
            }
            return WRITE_PERMISSION_PREFIX + RECORD_TYPE_TO_PERMISSION.getOrDefault(recordType, "")
        }

        internal const val PERMISSION_PREFIX = "android.permission.health."

        // Read permissions for ACTIVITY.
        internal const val READ_ACTIVE_CALORIES_BURNED =
            PERMISSION_PREFIX + "READ_ACTIVE_CALORIES_BURNED"
        internal const val READ_DISTANCE = PERMISSION_PREFIX + "READ_DISTANCE"
        internal const val READ_ELEVATION_GAINED = PERMISSION_PREFIX + "READ_ELEVATION_GAINED"
        internal const val READ_EXERCISE = PERMISSION_PREFIX + "READ_EXERCISE"
        internal const val READ_FLOORS_CLIMBED = PERMISSION_PREFIX + "READ_FLOORS_CLIMBED"
        internal const val READ_STEPS = PERMISSION_PREFIX + "READ_STEPS"
        internal const val READ_TOTAL_CALORIES_BURNED =
            PERMISSION_PREFIX + "READ_TOTAL_CALORIES_BURNED"
        internal const val READ_VO2_MAX = PERMISSION_PREFIX + "READ_VO2_MAX"
        internal const val READ_WHEELCHAIR_PUSHES = PERMISSION_PREFIX + "READ_WHEELCHAIR_PUSHES"
        internal const val READ_POWER = PERMISSION_PREFIX + "READ_POWER"
        internal const val READ_SPEED = PERMISSION_PREFIX + "READ_SPEED"

        // Read permissions for BODY_MEASUREMENTS.
        internal const val READ_BASAL_METABOLIC_RATE =
            PERMISSION_PREFIX + "READ_BASAL_METABOLIC_RATE"
        internal const val READ_BODY_FAT = PERMISSION_PREFIX + "READ_BODY_FAT"
        internal const val READ_BODY_WATER_MASS = PERMISSION_PREFIX + "READ_BODY_WATER_MASS"
        internal const val READ_BONE_MASS = PERMISSION_PREFIX + "READ_BONE_MASS"
        internal const val READ_HEIGHT = PERMISSION_PREFIX + "READ_HEIGHT"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val READ_HIP_CIRCUMFERENCE = PERMISSION_PREFIX + "READ_HIP_CIRCUMFERENCE"
        internal const val READ_LEAN_BODY_MASS = PERMISSION_PREFIX + "READ_LEAN_BODY_MASS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val READ_WAIST_CIRCUMFERENCE = PERMISSION_PREFIX + "READ_WAIST_CIRCUMFERENCE"
        internal const val READ_WEIGHT = PERMISSION_PREFIX + "READ_WEIGHT"

        // Read permissions for CYCLE_TRACKING.
        internal const val READ_CERVICAL_MUCUS = PERMISSION_PREFIX + "READ_CERVICAL_MUCUS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val READ_INTERMENSTRUAL_BLEEDING =
            PERMISSION_PREFIX + "READ_INTERMENSTRUAL_BLEEDING"
        internal const val READ_MENSTRUATION = PERMISSION_PREFIX + "READ_MENSTRUATION"
        internal const val READ_OVULATION_TEST = PERMISSION_PREFIX + "READ_OVULATION_TEST"
        internal const val READ_SEXUAL_ACTIVITY = PERMISSION_PREFIX + "READ_SEXUAL_ACTIVITY"

        // Read permissions for NUTRITION.
        internal const val READ_HYDRATION = PERMISSION_PREFIX + "READ_HYDRATION"
        internal const val READ_NUTRITION = PERMISSION_PREFIX + "READ_NUTRITION"

        // Read permissions for SLEEP.
        internal const val READ_SLEEP = PERMISSION_PREFIX + "READ_SLEEP"

        // Read permissions for VITALS.
        internal const val READ_BASAL_BODY_TEMPERATURE =
            PERMISSION_PREFIX + "READ_BASAL_BODY_TEMPERATURE"
        internal const val READ_BLOOD_GLUCOSE = PERMISSION_PREFIX + "READ_BLOOD_GLUCOSE"
        internal const val READ_BLOOD_PRESSURE = PERMISSION_PREFIX + "READ_BLOOD_PRESSURE"
        internal const val READ_BODY_TEMPERATURE = PERMISSION_PREFIX + "READ_BODY_TEMPERATURE"
        internal const val READ_HEART_RATE = PERMISSION_PREFIX + "READ_HEART_RATE"
        internal const val READ_HEART_RATE_VARIABILITY =
            PERMISSION_PREFIX + "READ_HEART_RATE_VARIABILITY"
        internal const val READ_OXYGEN_SATURATION = PERMISSION_PREFIX + "READ_OXYGEN_SATURATION"
        internal const val READ_RESPIRATORY_RATE = PERMISSION_PREFIX + "READ_RESPIRATORY_RATE"
        internal const val READ_RESTING_HEART_RATE = PERMISSION_PREFIX + "READ_RESTING_HEART_RATE"

        // Write permissions for ACTIVITY.
        internal const val WRITE_ACTIVE_CALORIES_BURNED =
            PERMISSION_PREFIX + "WRITE_ACTIVE_CALORIES_BURNED"
        internal const val WRITE_DISTANCE = PERMISSION_PREFIX + "WRITE_DISTANCE"
        internal const val WRITE_ELEVATION_GAINED = PERMISSION_PREFIX + "WRITE_ELEVATION_GAINED"
        internal const val WRITE_EXERCISE = PERMISSION_PREFIX + "WRITE_EXERCISE"
        internal const val WRITE_EXERCISE_ROUTE = PERMISSION_PREFIX + "WRITE_EXERCISE_ROUTE"
        internal const val WRITE_FLOORS_CLIMBED = PERMISSION_PREFIX + "WRITE_FLOORS_CLIMBED"
        internal const val WRITE_STEPS = PERMISSION_PREFIX + "WRITE_STEPS"
        internal const val WRITE_TOTAL_CALORIES_BURNED =
            PERMISSION_PREFIX + "WRITE_TOTAL_CALORIES_BURNED"
        internal const val WRITE_VO2_MAX = PERMISSION_PREFIX + "WRITE_VO2_MAX"
        internal const val WRITE_WHEELCHAIR_PUSHES = PERMISSION_PREFIX + "WRITE_WHEELCHAIR_PUSHES"
        internal const val WRITE_POWER = PERMISSION_PREFIX + "WRITE_POWER"
        internal const val WRITE_SPEED = PERMISSION_PREFIX + "WRITE_SPEED"

        // Write permissions for BODY_MEASUREMENTS.
        internal const val WRITE_BASAL_METABOLIC_RATE =
            PERMISSION_PREFIX + "WRITE_BASAL_METABOLIC_RATE"
        internal const val WRITE_BODY_FAT = PERMISSION_PREFIX + "WRITE_BODY_FAT"
        internal const val WRITE_BODY_WATER_MASS = PERMISSION_PREFIX + "WRITE_BODY_WATER_MASS"
        internal const val WRITE_BONE_MASS = PERMISSION_PREFIX + "WRITE_BONE_MASS"
        internal const val WRITE_HEIGHT = PERMISSION_PREFIX + "WRITE_HEIGHT"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val WRITE_HIP_CIRCUMFERENCE = PERMISSION_PREFIX + "WRITE_HIP_CIRCUMFERENCE"
        internal const val WRITE_LEAN_BODY_MASS = PERMISSION_PREFIX + "WRITE_LEAN_BODY_MASS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val WRITE_WAIST_CIRCUMFERENCE =
            PERMISSION_PREFIX + "WRITE_WAIST_CIRCUMFERENCE"
        internal const val WRITE_WEIGHT = PERMISSION_PREFIX + "WRITE_WEIGHT"

        // Write permissions for CYCLE_TRACKING.
        internal const val WRITE_CERVICAL_MUCUS = PERMISSION_PREFIX + "WRITE_CERVICAL_MUCUS"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val WRITE_INTERMENSTRUAL_BLEEDING =
            PERMISSION_PREFIX + "WRITE_INTERMENSTRUAL_BLEEDING"
        internal const val WRITE_MENSTRUATION = PERMISSION_PREFIX + "WRITE_MENSTRUATION"
        internal const val WRITE_OVULATION_TEST = PERMISSION_PREFIX + "WRITE_OVULATION_TEST"
        internal const val WRITE_SEXUAL_ACTIVITY = PERMISSION_PREFIX + "WRITE_SEXUAL_ACTIVITY"

        // Write permissions for NUTRITION.
        internal const val WRITE_HYDRATION = PERMISSION_PREFIX + "WRITE_HYDRATION"
        internal const val WRITE_NUTRITION = PERMISSION_PREFIX + "WRITE_NUTRITION"

        // Write permissions for SLEEP.
        internal const val WRITE_SLEEP = PERMISSION_PREFIX + "WRITE_SLEEP"

        // Write permissions for VITALS.
        internal const val WRITE_BASAL_BODY_TEMPERATURE =
            PERMISSION_PREFIX + "WRITE_BASAL_BODY_TEMPERATURE"
        internal const val WRITE_BLOOD_GLUCOSE = PERMISSION_PREFIX + "WRITE_BLOOD_GLUCOSE"
        internal const val WRITE_BLOOD_PRESSURE = PERMISSION_PREFIX + "WRITE_BLOOD_PRESSURE"
        internal const val WRITE_BODY_TEMPERATURE = PERMISSION_PREFIX + "WRITE_BODY_TEMPERATURE"
        internal const val WRITE_HEART_RATE = PERMISSION_PREFIX + "WRITE_HEART_RATE"
        internal const val WRITE_HEART_RATE_VARIABILITY =
            PERMISSION_PREFIX + "WRITE_HEART_RATE_VARIABILITY"
        internal const val WRITE_OXYGEN_SATURATION = PERMISSION_PREFIX + "WRITE_OXYGEN_SATURATION"
        internal const val WRITE_RESPIRATORY_RATE = PERMISSION_PREFIX + "WRITE_RESPIRATORY_RATE"
        internal const val WRITE_RESTING_HEART_RATE = PERMISSION_PREFIX + "WRITE_RESTING_HEART_RATE"

        internal const val READ_PERMISSION_PREFIX = PERMISSION_PREFIX + "READ_"
        internal const val WRITE_PERMISSION_PREFIX = PERMISSION_PREFIX + "WRITE_"

        internal val RECORD_TYPE_TO_PERMISSION =
            mapOf<KClass<out Record>, String>(
                ActiveCaloriesBurnedRecord::class to
                    READ_ACTIVE_CALORIES_BURNED.substringAfter(READ_PERMISSION_PREFIX),
                BasalBodyTemperatureRecord::class to
                    READ_BASAL_BODY_TEMPERATURE.substringAfter(READ_PERMISSION_PREFIX),
                BasalMetabolicRateRecord::class to
                    READ_BASAL_METABOLIC_RATE.substringAfter(READ_PERMISSION_PREFIX),
                BloodGlucoseRecord::class to
                    READ_BLOOD_GLUCOSE.substringAfter(READ_PERMISSION_PREFIX),
                BloodPressureRecord::class to
                    READ_BLOOD_PRESSURE.substringAfter(READ_PERMISSION_PREFIX),
                BodyFatRecord::class to READ_BODY_FAT.substringAfter(READ_PERMISSION_PREFIX),
                BodyTemperatureRecord::class to
                    READ_BODY_TEMPERATURE.substringAfter(READ_PERMISSION_PREFIX),
                BodyWaterMassRecord::class to
                    READ_BODY_WATER_MASS.substringAfter(READ_PERMISSION_PREFIX),
                BoneMassRecord::class to READ_BONE_MASS.substringAfter(READ_PERMISSION_PREFIX),
                CervicalMucusRecord::class to
                    READ_CERVICAL_MUCUS.substringAfter(READ_PERMISSION_PREFIX),
                CyclingPedalingCadenceRecord::class to
                    READ_EXERCISE.substringAfter(READ_PERMISSION_PREFIX),
                DistanceRecord::class to READ_DISTANCE.substringAfter(READ_PERMISSION_PREFIX),
                ElevationGainedRecord::class to
                    READ_ELEVATION_GAINED.substringAfter(READ_PERMISSION_PREFIX),
                ExerciseSessionRecord::class to
                    READ_EXERCISE.substringAfter(READ_PERMISSION_PREFIX),
                FloorsClimbedRecord::class to
                    READ_FLOORS_CLIMBED.substringAfter(READ_PERMISSION_PREFIX),
                HeartRateRecord::class to READ_HEART_RATE.substringAfter(READ_PERMISSION_PREFIX),
                HeartRateVariabilityRmssdRecord::class to
                    READ_HEART_RATE_VARIABILITY.substringAfter(READ_PERMISSION_PREFIX),
                HeightRecord::class to READ_HEIGHT.substringAfter(READ_PERMISSION_PREFIX),
                HydrationRecord::class to READ_HYDRATION.substringAfter(READ_PERMISSION_PREFIX),
                IntermenstrualBleedingRecord::class to
                    READ_INTERMENSTRUAL_BLEEDING.substringAfter(READ_PERMISSION_PREFIX),
                LeanBodyMassRecord::class to
                    READ_LEAN_BODY_MASS.substringAfter(READ_PERMISSION_PREFIX),
                MenstruationFlowRecord::class to
                    READ_MENSTRUATION.substringAfter(READ_PERMISSION_PREFIX),
                MenstruationPeriodRecord::class to
                    READ_MENSTRUATION.substringAfter(READ_PERMISSION_PREFIX),
                NutritionRecord::class to READ_NUTRITION.substringAfter(READ_PERMISSION_PREFIX),
                OvulationTestRecord::class to
                    READ_OVULATION_TEST.substringAfter(READ_PERMISSION_PREFIX),
                OxygenSaturationRecord::class to
                    READ_OXYGEN_SATURATION.substringAfter(READ_PERMISSION_PREFIX),
                PowerRecord::class to READ_POWER.substringAfter(READ_PERMISSION_PREFIX),
                RespiratoryRateRecord::class to
                    READ_RESPIRATORY_RATE.substringAfter(READ_PERMISSION_PREFIX),
                RestingHeartRateRecord::class to
                    READ_RESTING_HEART_RATE.substringAfter(READ_PERMISSION_PREFIX),
                SexualActivityRecord::class to
                    READ_SEXUAL_ACTIVITY.substringAfter(READ_PERMISSION_PREFIX),
                SleepSessionRecord::class to READ_SLEEP.substringAfter(READ_PERMISSION_PREFIX),
                SleepStageRecord::class to READ_SLEEP.substringAfter(READ_PERMISSION_PREFIX),
                SpeedRecord::class to READ_SPEED.substringAfter(READ_PERMISSION_PREFIX),
                StepsCadenceRecord::class to READ_STEPS.substringAfter(READ_PERMISSION_PREFIX),
                StepsRecord::class to READ_STEPS.substringAfter(READ_PERMISSION_PREFIX),
                TotalCaloriesBurnedRecord::class to
                    READ_TOTAL_CALORIES_BURNED.substringAfter(READ_PERMISSION_PREFIX),
                Vo2MaxRecord::class to READ_VO2_MAX.substringAfter(READ_PERMISSION_PREFIX),
                WeightRecord::class to READ_WEIGHT.substringAfter(READ_PERMISSION_PREFIX),
                WheelchairPushesRecord::class to
                    READ_WHEELCHAIR_PUSHES.substringAfter(READ_PERMISSION_PREFIX),
            )
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
