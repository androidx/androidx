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
import androidx.health.connect.client.records.ActiveEnergyBurnedRecord
import androidx.health.connect.client.records.ActivityEventRecord
import androidx.health.connect.client.records.ActivityLapRecord
import androidx.health.connect.client.records.ActivitySessionRecord
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
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityDifferentialIndexRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeartRateVariabilitySRecord
import androidx.health.connect.client.records.HeartRateVariabilitySd2Record
import androidx.health.connect.client.records.HeartRateVariabilitySdannRecord
import androidx.health.connect.client.records.HeartRateVariabilitySdnnRecord
import androidx.health.connect.client.records.HeartRateVariabilitySdnnIndexRecord
import androidx.health.connect.client.records.HeartRateVariabilitySdsdRecord
import androidx.health.connect.client.records.HeartRateVariabilityTinnRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HipCircumferenceRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RepetitionsRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.SwimmingStrokesRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalEnergyBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WaistCircumferenceRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import kotlin.reflect.KClass

private val ALL_RECORDS_TYPES =
    setOf(
        ActiveCaloriesBurnedRecord::class,
        ActiveEnergyBurnedRecord::class,
        ActivityEventRecord::class,
        ActivityLapRecord::class,
        ActivitySessionRecord::class,
        BasalBodyTemperatureRecord::class,
        BasalMetabolicRateRecord::class,
        BloodGlucoseRecord::class,
        BloodPressureRecord::class,
        BodyFatRecord::class,
        BodyTemperatureRecord::class,
        BodyWaterMassRecord::class,
        BoneMassRecord::class,
        CervicalMucusRecord::class,
        CyclingPedalingCadenceRecord::class,
        DistanceRecord::class,
        ElevationGainedRecord::class,
        FloorsClimbedRecord::class,
        HeartRateRecord::class,
        HeartRateVariabilityDifferentialIndexRecord::class,
        HeartRateVariabilityRmssdRecord::class,
        HeartRateVariabilitySRecord::class,
        HeartRateVariabilitySd2Record::class,
        HeartRateVariabilitySdannRecord::class,
        HeartRateVariabilitySdnnRecord::class,
        HeartRateVariabilitySdnnIndexRecord::class,
        HeartRateVariabilitySdsdRecord::class,
        HeartRateVariabilityTinnRecord::class,
        HeightRecord::class,
        HipCircumferenceRecord::class,
        HydrationRecord::class,
        LeanBodyMassRecord::class,
        MenstruationRecord::class,
        NutritionRecord::class,
        OvulationTestRecord::class,
        OxygenSaturationRecord::class,
        PowerRecord::class,
        RepetitionsRecord::class,
        RespiratoryRateRecord::class,
        RestingHeartRateRecord::class,
        SexualActivityRecord::class,
        SleepSessionRecord::class,
        SleepStageRecord::class,
        SpeedRecord::class,
        StepsRecord::class,
        StepsCadenceRecord::class,
        SwimmingStrokesRecord::class,
        TotalCaloriesBurnedRecord::class,
        TotalEnergyBurnedRecord::class,
        Vo2MaxRecord::class,
        WaistCircumferenceRecord::class,
        WheelchairPushesRecord::class,
        WeightRecord::class,
    )

val RECORDS_TYPE_NAME_MAP: Map<String, KClass<out Record>> =
    ALL_RECORDS_TYPES.associateBy { it.simpleName!! }
