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

package androidx.health.connect.client.impl.converters.datatype

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
import androidx.health.connect.client.records.ExerciseEventRecord
import androidx.health.connect.client.records.ExerciseLapRecord
import androidx.health.connect.client.records.ExerciseRepetitionsRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityDifferentialIndexRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeartRateVariabilitySRecord
import androidx.health.connect.client.records.HeartRateVariabilitySd2Record
import androidx.health.connect.client.records.HeartRateVariabilitySdannRecord
import androidx.health.connect.client.records.HeartRateVariabilitySdnnIndexRecord
import androidx.health.connect.client.records.HeartRateVariabilitySdnnRecord
import androidx.health.connect.client.records.HeartRateVariabilitySdsdRecord
import androidx.health.connect.client.records.HeartRateVariabilityTinnRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HipCircumferenceRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
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
import androidx.health.connect.client.records.SwimmingStrokesRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WaistCircumferenceRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import kotlin.reflect.KClass

internal val TYPE_ID_TO_RECORD_TYPE_MAP: Map<Int, KClass<out Record>> =
    mapOf(
        3 to ExerciseEventRecord::class,
        55 to ExerciseLapRecord::class,
        4 to ExerciseSessionRecord::class,
        6 to DistanceRecord::class,
        7 to ElevationGainedRecord::class,
        8 to FloorsClimbedRecord::class,
        9 to HydrationRecord::class,
        10 to NutritionRecord::class,
        11 to SleepStageRecord::class,
        12 to SleepSessionRecord::class,
        13 to StepsRecord::class,
        14 to SwimmingStrokesRecord::class,
        16 to BasalMetabolicRateRecord::class,
        17 to BloodGlucoseRecord::class,
        18 to BloodPressureRecord::class,
        19 to BodyFatRecord::class,
        20 to BodyTemperatureRecord::class,
        21 to BoneMassRecord::class,
        22 to CervicalMucusRecord::class,
        28 to HeightRecord::class,
        29 to HipCircumferenceRecord::class,
        30 to HeartRateVariabilityDifferentialIndexRecord::class,
        31 to HeartRateVariabilityRmssdRecord::class,
        32 to HeartRateVariabilitySRecord::class,
        33 to HeartRateVariabilitySd2Record::class,
        34 to HeartRateVariabilitySdannRecord::class,
        35 to HeartRateVariabilitySdnnIndexRecord::class,
        36 to HeartRateVariabilitySdnnRecord::class,
        37 to HeartRateVariabilitySdsdRecord::class,
        38 to HeartRateVariabilityTinnRecord::class,
        39 to LeanBodyMassRecord::class,
        41 to MenstruationFlowRecord::class,
        42 to OvulationTestRecord::class,
        43 to OxygenSaturationRecord::class,
        46 to RespiratoryRateRecord::class,
        47 to RestingHeartRateRecord::class,
        48 to SexualActivityRecord::class,
        51 to Vo2MaxRecord::class,
        52 to WaistCircumferenceRecord::class,
        53 to WeightRecord::class,
        54 to ExerciseRepetitionsRecord::class,
        56 to HeartRateRecord::class,
        58 to CyclingPedalingCadenceRecord::class,
        60 to PowerRecord::class,
        61 to SpeedRecord::class,
        62 to StepsCadenceRecord::class,
        63 to WheelchairPushesRecord::class,
        64 to BodyWaterMassRecord::class,
        65 to BasalBodyTemperatureRecord::class,
        66 to TotalCaloriesBurnedRecord::class,
        67 to ActiveCaloriesBurnedRecord::class,
    )

internal fun getRecordType(id: Int): KClass<out Record> =
    requireNotNull(TYPE_ID_TO_RECORD_TYPE_MAP[id]) { "Unknown data type id: $id" }
