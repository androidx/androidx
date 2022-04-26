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
package androidx.health.connect.client.impl.converters.datatype

import androidx.health.connect.client.records.ActiveCaloriesBurned
import androidx.health.connect.client.records.ActiveEnergyBurned
import androidx.health.connect.client.records.ActivityEvent
import androidx.health.connect.client.records.ActivityLap
import androidx.health.connect.client.records.ActivitySession
import androidx.health.connect.client.records.BasalBodyTemperature
import androidx.health.connect.client.records.BasalMetabolicRate
import androidx.health.connect.client.records.BloodGlucose
import androidx.health.connect.client.records.BloodPressure
import androidx.health.connect.client.records.BodyFat
import androidx.health.connect.client.records.BodyTemperature
import androidx.health.connect.client.records.BodyWaterMass
import androidx.health.connect.client.records.BoneMass
import androidx.health.connect.client.records.CervicalMucus
import androidx.health.connect.client.records.CervicalPosition
import androidx.health.connect.client.records.CyclingPedalingCadenceSeries
import androidx.health.connect.client.records.Distance
import androidx.health.connect.client.records.ElevationGained
import androidx.health.connect.client.records.FloorsClimbed
import androidx.health.connect.client.records.HeartRateSeries
import androidx.health.connect.client.records.HeartRateVariabilityDifferentialIndex
import androidx.health.connect.client.records.HeartRateVariabilityRmssd
import androidx.health.connect.client.records.HeartRateVariabilityS
import androidx.health.connect.client.records.HeartRateVariabilitySd2
import androidx.health.connect.client.records.HeartRateVariabilitySdann
import androidx.health.connect.client.records.HeartRateVariabilitySdnn
import androidx.health.connect.client.records.HeartRateVariabilitySdnnIndex
import androidx.health.connect.client.records.HeartRateVariabilitySdsd
import androidx.health.connect.client.records.HeartRateVariabilityTinn
import androidx.health.connect.client.records.Height
import androidx.health.connect.client.records.HipCircumference
import androidx.health.connect.client.records.Hydration
import androidx.health.connect.client.records.LeanBodyMass
import androidx.health.connect.client.records.Menstruation
import androidx.health.connect.client.records.Nutrition
import androidx.health.connect.client.records.OvulationTest
import androidx.health.connect.client.records.OxygenSaturation
import androidx.health.connect.client.records.PowerSeries
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.Repetitions
import androidx.health.connect.client.records.RespiratoryRate
import androidx.health.connect.client.records.RestingHeartRate
import androidx.health.connect.client.records.SexualActivity
import androidx.health.connect.client.records.SleepSession
import androidx.health.connect.client.records.SleepStage
import androidx.health.connect.client.records.SpeedSeries
import androidx.health.connect.client.records.Steps
import androidx.health.connect.client.records.StepsCadenceSeries
import androidx.health.connect.client.records.SwimmingStrokes
import androidx.health.connect.client.records.TotalCaloriesBurned
import androidx.health.connect.client.records.TotalEnergyBurned
import androidx.health.connect.client.records.Vo2Max
import androidx.health.connect.client.records.WaistCircumference
import androidx.health.connect.client.records.Weight
import androidx.health.connect.client.records.WheelchairPushes
import kotlin.reflect.KClass

private val ALL_RECORDS_TYPES =
    setOf(
        ActiveCaloriesBurned::class,
        ActiveEnergyBurned::class,
        ActivityEvent::class,
        ActivityLap::class,
        ActivitySession::class,
        BasalBodyTemperature::class,
        BasalMetabolicRate::class,
        BloodGlucose::class,
        BloodPressure::class,
        BodyFat::class,
        BodyTemperature::class,
        BodyWaterMass::class,
        BoneMass::class,
        CervicalMucus::class,
        CervicalPosition::class,
        CyclingPedalingCadenceSeries::class,
        Distance::class,
        ElevationGained::class,
        FloorsClimbed::class,
        HeartRateSeries::class,
        HeartRateVariabilityDifferentialIndex::class,
        HeartRateVariabilityRmssd::class,
        HeartRateVariabilityS::class,
        HeartRateVariabilitySd2::class,
        HeartRateVariabilitySdann::class,
        HeartRateVariabilitySdnn::class,
        HeartRateVariabilitySdnnIndex::class,
        HeartRateVariabilitySdsd::class,
        HeartRateVariabilityTinn::class,
        Height::class,
        HipCircumference::class,
        Hydration::class,
        LeanBodyMass::class,
        Menstruation::class,
        Nutrition::class,
        OvulationTest::class,
        OxygenSaturation::class,
        PowerSeries::class,
        Repetitions::class,
        RespiratoryRate::class,
        RestingHeartRate::class,
        SexualActivity::class,
        SleepSession::class,
        SleepStage::class,
        SpeedSeries::class,
        Steps::class,
        StepsCadenceSeries::class,
        SwimmingStrokes::class,
        TotalCaloriesBurned::class,
        TotalEnergyBurned::class,
        Vo2Max::class,
        WaistCircumference::class,
        WheelchairPushes::class,
        Weight::class,
    )

val RECORDS_TYPE_NAME_MAP: Map<String, KClass<out Record>> =
    ALL_RECORDS_TYPES.associateBy { it.simpleName!! }
