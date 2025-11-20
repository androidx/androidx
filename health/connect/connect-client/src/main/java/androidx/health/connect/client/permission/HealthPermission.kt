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
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ActivityIntensityRecord
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
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
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
public class HealthPermission internal constructor() {
    companion object {
        /**
         * Returns a permission defined in [HealthPermission] to read records of type [T], such as
         * `StepsRecord`.
         *
         * @return Permission to use with [androidx.health.connect.client.PermissionController].
         * @throws IllegalArgumentException if the given record type is invalid.
         */
        @JvmStatic
        inline fun <reified T : Record> getReadPermission(): String {
            return getReadPermission(T::class)
        }

        /**
         * Returns a permission defined in [HealthPermission] to read records of type [recordType],
         * such as `StepsRecord::class`.
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
         * Returns a permission defined in [HealthPermission] to write records of type [T], such as
         * `StepsRecord:`.
         *
         * @return Permission object to use with
         *   [androidx.health.connect.client.PermissionController].
         * @throws IllegalArgumentException if the given record type is invalid.
         */
        @JvmStatic
        inline fun <reified T : Record> getWritePermission(): String {
            return getWritePermission(T::class)
        }

        /**
         * Returns a permission defined in [HealthPermission] to write records of type [recordType],
         * such as `StepsRecord::class`.
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

        /**
         * A permission to write exercise routes.
         *
         * This permission must be granted to successfully insert a route as a field of the
         * corresponding [androidx.health.connect.client.records.ExerciseSessionRecord]. An attempt
         * to insert/update a session with a set route without the permission granted will result in
         * a failed call and the session insertion/update will be rejected.
         *
         * If the permission is not granted the previously written route will not be deleted if the
         * session gets updated with no route set.
         *
         * @sample androidx.health.connect.client.samples.InsertExerciseRoute
         */
        const val PERMISSION_WRITE_EXERCISE_ROUTE = PERMISSION_PREFIX + "WRITE_EXERCISE_ROUTE"

        /**
         * A permission to read exercise routes. The string value for this permission is
         * `android.permission.health.READ_EXERCISE_ROUTES`.
         *
         * This permission can't be granted via the standard permission request mechanism, and can
         * only be granted by a user in Settings, or via the dialog launched by
         * [androidx.health.connect.client.contracts.ExerciseRouteRequestContract].
         *
         * When this permission is granted, the app can read exercise routes without user
         * interaction, however reading apps must be in the foreground unless
         * `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND` is also granted.
         *
         * When this permission is revoked, exercise routes can only be obtained by launching the
         * [androidx.health.connect.client.contracts.ExerciseRouteRequestContract] intent.
         *
         * @sample androidx.health.connect.client.samples.ReadExerciseRoute
         */
        const val PERMISSION_READ_EXERCISE_ROUTES = PERMISSION_PREFIX + "READ_EXERCISE_ROUTES"

        /**
         * A permission to read data in background.
         *
         * An attempt to read data in background without this permission may result in an error.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND] as an argument.
         *
         * @sample androidx.health.connect.client.samples.RequestBackgroundReadPermission
         * @sample androidx.health.connect.client.samples.ReadRecordsInBackground
         */
        const val PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND =
            PERMISSION_PREFIX + "READ_HEALTH_DATA_IN_BACKGROUND"

        /**
         * A permission that allows to read the entire history of health data (of any type).
         *
         * Without this permission:
         * 1. Any attempt to read a single data point, via [HealthConnectClient.readRecord], older
         *    than 30 days from before the first HealthConnect permission was granted to the calling
         *    app, will result in an error.
         * 2. Any other read attempts will not return data points older than 30 days from before the
         *    first HealthConnect permission was granted to the calling app.
         *
         * This permission applies for the following api methods: [HealthConnectClient.readRecord],
         * [HealthConnectClient.readRecords], [HealthConnectClient.aggregate],
         * [HealthConnectClient.aggregateGroupByPeriod],
         * [HealthConnectClient.aggregateGroupByDuration] and [HealthConnectClient.getChanges].
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY] as an argument.
         *
         * @sample androidx.health.connect.client.samples.RequestHistoryReadPermission
         */
        const val PERMISSION_READ_HEALTH_DATA_HISTORY =
            PERMISSION_PREFIX + "READ_HEALTH_DATA_HISTORY"

        /**
         * Permission to write medical data records. This permission allows for the write of medical
         * data of any type. Note that read permissions are specified per-type.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. If not available,
         * this permission will not be granted if requested.
         *
         * @sample androidx.health.connect.client.samples.RequestMedicalPermissions
         */
        @ExperimentalPersonalHealthRecordApi
        const val PERMISSION_WRITE_MEDICAL_DATA = PERMISSION_PREFIX + "WRITE_MEDICAL_DATA"

        /**
         * Allows an application to read the user's data about allergies and intolerances.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. If not available,
         * this permission will not be granted if requested.
         *
         * @sample androidx.health.connect.client.samples.RequestMedicalPermissions
         */
        @ExperimentalPersonalHealthRecordApi
        const val PERMISSION_READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES =
            PERMISSION_PREFIX + "READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES"

        /**
         * Allows an application to read the user's data about medical conditions.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. If not available,
         * this permission will not be granted if requested.
         *
         * @sample androidx.health.connect.client.samples.RequestMedicalPermissions
         */
        @ExperimentalPersonalHealthRecordApi
        const val PERMISSION_READ_MEDICAL_DATA_CONDITIONS =
            PERMISSION_PREFIX + "READ_MEDICAL_DATA_CONDITIONS"

        /**
         * Allows an application to read the user's laboratory result data.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. If not available,
         * this permission will not be granted if requested.
         *
         * @sample androidx.health.connect.client.samples.RequestMedicalPermissions
         */
        @ExperimentalPersonalHealthRecordApi
        const val PERMISSION_READ_MEDICAL_DATA_LABORATORY_RESULTS =
            PERMISSION_PREFIX + "READ_MEDICAL_DATA_LABORATORY_RESULTS"

        /**
         * Allows an application to read the user's medication data.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. If not available,
         * this permission will not be granted if requested.
         *
         * @sample androidx.health.connect.client.samples.RequestMedicalPermissions
         */
        @ExperimentalPersonalHealthRecordApi
        const val PERMISSION_READ_MEDICAL_DATA_MEDICATIONS =
            PERMISSION_PREFIX + "READ_MEDICAL_DATA_MEDICATIONS"

        /**
         * Allows an application to read the user's personal details.
         *
         * This is demographic information such as name, date of birth, contact details like address
         * or telephone number and so on. For more examples see the FHIR Patient resource at
         * https://www.hl7.org/fhir/patient.html.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. If not available,
         * this permission will not be granted if requested.
         *
         * @sample androidx.health.connect.client.samples.RequestMedicalPermissions
         */
        @ExperimentalPersonalHealthRecordApi
        const val PERMISSION_READ_MEDICAL_DATA_PERSONAL_DETAILS =
            PERMISSION_PREFIX + "READ_MEDICAL_DATA_PERSONAL_DETAILS"

        /**
         * Allows an application to read the user's data about the practitioners who have interacted
         * with them in their medical record. This is the information about the clinicians (doctors,
         * nurses, etc) but also other practitioners (masseurs, physiotherapists, etc) who have been
         * involved with the patient.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. If not available,
         * this permission will not be granted if requested.
         *
         * @sample androidx.health.connect.client.samples.RequestMedicalPermissions
         */
        @ExperimentalPersonalHealthRecordApi
        const val PERMISSION_READ_MEDICAL_DATA_PRACTITIONER_DETAILS =
            PERMISSION_PREFIX + "READ_MEDICAL_DATA_PRACTITIONER_DETAILS"

        /**
         * Allows an application to read the user's pregnancy data.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. If not available,
         * this permission will not be granted if requested.
         *
         * @sample androidx.health.connect.client.samples.RequestMedicalPermissions
         */
        @ExperimentalPersonalHealthRecordApi
        const val PERMISSION_READ_MEDICAL_DATA_PREGNANCY =
            PERMISSION_PREFIX + "READ_MEDICAL_DATA_PREGNANCY"

        /**
         * Allows an application to read the user's data about medical procedures.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. If not available,
         * this permission will not be granted if requested.
         *
         * @sample androidx.health.connect.client.samples.RequestMedicalPermissions
         */
        @ExperimentalPersonalHealthRecordApi
        const val PERMISSION_READ_MEDICAL_DATA_PROCEDURES =
            PERMISSION_PREFIX + "READ_MEDICAL_DATA_PROCEDURES"

        /**
         * Allows an application to read the user's social history data.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. If not available,
         * this permission will not be granted if requested.
         *
         * @sample androidx.health.connect.client.samples.RequestMedicalPermissions
         */
        @ExperimentalPersonalHealthRecordApi
        const val PERMISSION_READ_MEDICAL_DATA_SOCIAL_HISTORY =
            PERMISSION_PREFIX + "READ_MEDICAL_DATA_SOCIAL_HISTORY"

        /**
         * Allows an application to read the user's data about immunizations and vaccinations.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. If not available,
         * this permission will not be granted if requested.
         *
         * @sample androidx.health.connect.client.samples.RequestMedicalPermissions
         */
        @ExperimentalPersonalHealthRecordApi
        const val PERMISSION_READ_MEDICAL_DATA_VACCINES =
            PERMISSION_PREFIX + "READ_MEDICAL_DATA_VACCINES"

        /**
         * Allows an application to read the user's information about their encounters with health
         * care practitioners, including things like location, time of appointment, and name of
         * organization the visit was with. Despite the name visit it covers remote encounters such
         * as telephone or videoconference appointments.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. If not available,
         * this permission will not be granted if requested.
         *
         * @sample androidx.health.connect.client.samples.RequestMedicalPermissions
         */
        @ExperimentalPersonalHealthRecordApi
        const val PERMISSION_READ_MEDICAL_DATA_VISITS =
            PERMISSION_PREFIX + "READ_MEDICAL_DATA_VISITS"

        /**
         * Allows an application to read the user's vital signs data.
         *
         * This feature is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. If not available,
         * this permission will not be granted if requested.
         *
         * @sample androidx.health.connect.client.samples.RequestMedicalPermissions
         */
        @ExperimentalPersonalHealthRecordApi
        const val PERMISSION_READ_MEDICAL_DATA_VITAL_SIGNS =
            PERMISSION_PREFIX + "READ_MEDICAL_DATA_VITAL_SIGNS"

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
        internal const val READ_PLANNED_EXERCISE = PERMISSION_PREFIX + "READ_PLANNED_EXERCISE"
        internal const val READ_POWER = PERMISSION_PREFIX + "READ_POWER"
        internal const val READ_SPEED = PERMISSION_PREFIX + "READ_SPEED"

        internal const val READ_ACTIVITY_INTENSITY = PERMISSION_PREFIX + "READ_ACTIVITY_INTENSITY"

        // Read permissions for BODY_MEASUREMENTS.
        internal const val READ_BASAL_METABOLIC_RATE =
            PERMISSION_PREFIX + "READ_BASAL_METABOLIC_RATE"
        internal const val READ_BODY_FAT = PERMISSION_PREFIX + "READ_BODY_FAT"
        internal const val READ_BODY_WATER_MASS = PERMISSION_PREFIX + "READ_BODY_WATER_MASS"
        internal const val READ_BONE_MASS = PERMISSION_PREFIX + "READ_BONE_MASS"
        internal const val READ_HEIGHT = PERMISSION_PREFIX + "READ_HEIGHT"

        internal const val READ_LEAN_BODY_MASS = PERMISSION_PREFIX + "READ_LEAN_BODY_MASS"

        internal const val READ_WEIGHT = PERMISSION_PREFIX + "READ_WEIGHT"

        // Read permissions for CYCLE_TRACKING.
        internal const val READ_CERVICAL_MUCUS = PERMISSION_PREFIX + "READ_CERVICAL_MUCUS"
        internal const val READ_INTERMENSTRUAL_BLEEDING =
            PERMISSION_PREFIX + "READ_INTERMENSTRUAL_BLEEDING"
        internal const val READ_MENSTRUATION = PERMISSION_PREFIX + "READ_MENSTRUATION"
        internal const val READ_OVULATION_TEST = PERMISSION_PREFIX + "READ_OVULATION_TEST"
        internal const val READ_SEXUAL_ACTIVITY = PERMISSION_PREFIX + "READ_SEXUAL_ACTIVITY"

        // Read permissions for WELLNESS
        internal const val READ_MINDFULNESS_SESSION = PERMISSION_PREFIX + "READ_MINDFULNESS"

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
        internal const val READ_SKIN_TEMPERATURE = PERMISSION_PREFIX + "READ_SKIN_TEMPERATURE"

        // Write permissions for ACTIVITY.
        internal const val WRITE_ACTIVE_CALORIES_BURNED =
            PERMISSION_PREFIX + "WRITE_ACTIVE_CALORIES_BURNED"
        internal const val WRITE_DISTANCE = PERMISSION_PREFIX + "WRITE_DISTANCE"
        internal const val WRITE_ELEVATION_GAINED = PERMISSION_PREFIX + "WRITE_ELEVATION_GAINED"
        internal const val WRITE_EXERCISE = PERMISSION_PREFIX + "WRITE_EXERCISE"
        internal const val WRITE_FLOORS_CLIMBED = PERMISSION_PREFIX + "WRITE_FLOORS_CLIMBED"
        internal const val WRITE_STEPS = PERMISSION_PREFIX + "WRITE_STEPS"
        internal const val WRITE_TOTAL_CALORIES_BURNED =
            PERMISSION_PREFIX + "WRITE_TOTAL_CALORIES_BURNED"
        internal const val WRITE_VO2_MAX = PERMISSION_PREFIX + "WRITE_VO2_MAX"
        internal const val WRITE_WHEELCHAIR_PUSHES = PERMISSION_PREFIX + "WRITE_WHEELCHAIR_PUSHES"
        internal const val WRITE_PLANNED_EXERCISE = PERMISSION_PREFIX + "WRITE_PLANNED_EXERCISE"
        internal const val WRITE_POWER = PERMISSION_PREFIX + "WRITE_POWER"
        internal const val WRITE_SPEED = PERMISSION_PREFIX + "WRITE_SPEED"
        internal const val WRITE_ACTIVITY_INTENSITY = PERMISSION_PREFIX + "WRITE_ACTIVITY_INTENSITY"

        // Write permissions for BODY_MEASUREMENTS.
        internal const val WRITE_BASAL_METABOLIC_RATE =
            PERMISSION_PREFIX + "WRITE_BASAL_METABOLIC_RATE"
        internal const val WRITE_BODY_FAT = PERMISSION_PREFIX + "WRITE_BODY_FAT"
        internal const val WRITE_BODY_WATER_MASS = PERMISSION_PREFIX + "WRITE_BODY_WATER_MASS"
        internal const val WRITE_BONE_MASS = PERMISSION_PREFIX + "WRITE_BONE_MASS"
        internal const val WRITE_HEIGHT = PERMISSION_PREFIX + "WRITE_HEIGHT"

        internal const val WRITE_LEAN_BODY_MASS = PERMISSION_PREFIX + "WRITE_LEAN_BODY_MASS"

        internal const val WRITE_WEIGHT = PERMISSION_PREFIX + "WRITE_WEIGHT"

        // Write permissions for CYCLE_TRACKING.
        internal const val WRITE_CERVICAL_MUCUS = PERMISSION_PREFIX + "WRITE_CERVICAL_MUCUS"
        internal const val WRITE_INTERMENSTRUAL_BLEEDING =
            PERMISSION_PREFIX + "WRITE_INTERMENSTRUAL_BLEEDING"
        internal const val WRITE_MENSTRUATION = PERMISSION_PREFIX + "WRITE_MENSTRUATION"
        internal const val WRITE_OVULATION_TEST = PERMISSION_PREFIX + "WRITE_OVULATION_TEST"
        internal const val WRITE_SEXUAL_ACTIVITY = PERMISSION_PREFIX + "WRITE_SEXUAL_ACTIVITY"

        // Write permissions for NUTRITION.
        internal const val WRITE_HYDRATION = PERMISSION_PREFIX + "WRITE_HYDRATION"
        internal const val WRITE_NUTRITION = PERMISSION_PREFIX + "WRITE_NUTRITION"

        // Write permissions for WELLNESS
        internal const val WRITE_MINDFULNESS_SESSION = PERMISSION_PREFIX + "WRITE_MINDFULNESS"

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
        internal const val WRITE_SKIN_TEMPERATURE = PERMISSION_PREFIX + "WRITE_SKIN_TEMPERATURE"

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
                MindfulnessSessionRecord::class to
                    READ_MINDFULNESS_SESSION.substringAfter(READ_PERMISSION_PREFIX),
                NutritionRecord::class to READ_NUTRITION.substringAfter(READ_PERMISSION_PREFIX),
                OvulationTestRecord::class to
                    READ_OVULATION_TEST.substringAfter(READ_PERMISSION_PREFIX),
                OxygenSaturationRecord::class to
                    READ_OXYGEN_SATURATION.substringAfter(READ_PERMISSION_PREFIX),
                PlannedExerciseSessionRecord::class to
                    READ_PLANNED_EXERCISE.substringAfter(READ_PERMISSION_PREFIX),
                PowerRecord::class to READ_POWER.substringAfter(READ_PERMISSION_PREFIX),
                RespiratoryRateRecord::class to
                    READ_RESPIRATORY_RATE.substringAfter(READ_PERMISSION_PREFIX),
                RestingHeartRateRecord::class to
                    READ_RESTING_HEART_RATE.substringAfter(READ_PERMISSION_PREFIX),
                SexualActivityRecord::class to
                    READ_SEXUAL_ACTIVITY.substringAfter(READ_PERMISSION_PREFIX),
                SleepSessionRecord::class to READ_SLEEP.substringAfter(READ_PERMISSION_PREFIX),
                SpeedRecord::class to READ_SPEED.substringAfter(READ_PERMISSION_PREFIX),
                SkinTemperatureRecord::class to
                    READ_SKIN_TEMPERATURE.substringAfter(READ_PERMISSION_PREFIX),
                StepsCadenceRecord::class to READ_STEPS.substringAfter(READ_PERMISSION_PREFIX),
                StepsRecord::class to READ_STEPS.substringAfter(READ_PERMISSION_PREFIX),
                TotalCaloriesBurnedRecord::class to
                    READ_TOTAL_CALORIES_BURNED.substringAfter(READ_PERMISSION_PREFIX),
                Vo2MaxRecord::class to READ_VO2_MAX.substringAfter(READ_PERMISSION_PREFIX),
                WeightRecord::class to READ_WEIGHT.substringAfter(READ_PERMISSION_PREFIX),
                WheelchairPushesRecord::class to
                    READ_WHEELCHAIR_PUSHES.substringAfter(READ_PERMISSION_PREFIX),
                ActivityIntensityRecord::class to
                    READ_ACTIVITY_INTENSITY.substringAfter(READ_PERMISSION_PREFIX),
            )

        /**
         * Exposes all PHR write and read permissions.
         *
         * @return A list of permissions as Strings
         */
        @OptIn(ExperimentalPersonalHealthRecordApi::class)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmField
        public val ALL_PERSONAL_HEALTH_RECORD_PERMISSIONS: List<String> = buildList {
            add(PERMISSION_WRITE_MEDICAL_DATA)
            add(PERMISSION_READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES)
            add(PERMISSION_READ_MEDICAL_DATA_CONDITIONS)
            add(PERMISSION_READ_MEDICAL_DATA_LABORATORY_RESULTS)
            add(PERMISSION_READ_MEDICAL_DATA_MEDICATIONS)
            add(PERMISSION_READ_MEDICAL_DATA_PERSONAL_DETAILS)
            add(PERMISSION_READ_MEDICAL_DATA_PRACTITIONER_DETAILS)
            add(PERMISSION_READ_MEDICAL_DATA_PREGNANCY)
            add(PERMISSION_READ_MEDICAL_DATA_PROCEDURES)
            add(PERMISSION_READ_MEDICAL_DATA_SOCIAL_HISTORY)
            add(PERMISSION_READ_MEDICAL_DATA_VACCINES)
            add(PERMISSION_READ_MEDICAL_DATA_VISITS)
            add(PERMISSION_READ_MEDICAL_DATA_VITAL_SIGNS)
        }

        /**
         * Exposes all write and read permissions.
         *
         * @return A list of permissions as Strings
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmField
        public val ALL_PERMISSIONS: List<String> = buildList {
            addAll(
                RECORD_TYPE_TO_PERMISSION.flatMap {
                    listOf(WRITE_PERMISSION_PREFIX + it.value, READ_PERMISSION_PREFIX + it.value)
                }
            )
            addAll(ALL_PERSONAL_HEALTH_RECORD_PERMISSIONS)
            add(PERMISSION_WRITE_EXERCISE_ROUTE)
            add(PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)
            add(PERMISSION_READ_HEALTH_DATA_HISTORY)
            add(PERMISSION_READ_EXERCISE_ROUTES)
        }
    }
}
