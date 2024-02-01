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
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.health.connect.client.impl.converters.datatype

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
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import kotlin.reflect.KClass

val RECORDS_TYPE_NAME_MAP: Map<String, KClass<out Record>> =
    mapOf(
        "ActiveCaloriesBurned" to ActiveCaloriesBurnedRecord::class,
        "ActivitySession" to ExerciseSessionRecord::class, // Keep legacy Activity name
        "BasalBodyTemperature" to BasalBodyTemperatureRecord::class,
        "BasalMetabolicRate" to BasalMetabolicRateRecord::class,
        "BloodGlucose" to BloodGlucoseRecord::class,
        "BloodPressure" to BloodPressureRecord::class,
        "BodyFat" to BodyFatRecord::class,
        "BodyTemperature" to BodyTemperatureRecord::class,
        "BodyWaterMass" to BodyWaterMassRecord::class,
        "BoneMass" to BoneMassRecord::class,
        "CervicalMucus" to CervicalMucusRecord::class,
        "CyclingPedalingCadenceSeries" to
            CyclingPedalingCadenceRecord::class, // Keep legacy Series suffix
        "Distance" to DistanceRecord::class,
        "ElevationGained" to ElevationGainedRecord::class,
        "FloorsClimbed" to FloorsClimbedRecord::class,
        "HeartRateSeries" to HeartRateRecord::class, // Keep legacy Series suffix
        "HeartRateVariabilityRmssd" to HeartRateVariabilityRmssdRecord::class,
        "Height" to HeightRecord::class,
        "Hydration" to HydrationRecord::class,
        "LeanBodyMass" to LeanBodyMassRecord::class,
        "Menstruation" to MenstruationFlowRecord::class,
        "MenstruationPeriod" to MenstruationPeriodRecord::class,
        "Nutrition" to NutritionRecord::class,
        "OvulationTest" to OvulationTestRecord::class,
        "OxygenSaturation" to OxygenSaturationRecord::class,
        "PowerSeries" to PowerRecord::class, // Keep legacy Series suffix
        "RespiratoryRate" to RespiratoryRateRecord::class,
        "RestingHeartRate" to RestingHeartRateRecord::class,
        "SexualActivity" to SexualActivityRecord::class,
        "SleepSession" to SleepSessionRecord::class,
        "SpeedSeries" to SpeedRecord::class, // Keep legacy Series suffix
        "IntermenstrualBleeding" to IntermenstrualBleedingRecord::class,
        "Steps" to StepsRecord::class,
        "StepsCadenceSeries" to StepsCadenceRecord::class, // Keep legacy Series suffix
        "TotalCaloriesBurned" to TotalCaloriesBurnedRecord::class,
        "Vo2Max" to Vo2MaxRecord::class,
        "WheelchairPushes" to WheelchairPushesRecord::class,
        "Weight" to WeightRecord::class,
    )

val RECORDS_CLASS_NAME_MAP: Map<KClass<out Record>, String> =
    RECORDS_TYPE_NAME_MAP.entries.associate { it.value to it.key }
