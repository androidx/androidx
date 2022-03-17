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
package androidx.health.data.client.impl.converters.datatype

import androidx.health.data.client.records.ActiveEnergyBurned
import androidx.health.data.client.records.ActivityEvent
import androidx.health.data.client.records.ActivityLap
import androidx.health.data.client.records.ActivitySession
import androidx.health.data.client.records.BasalMetabolicRate
import androidx.health.data.client.records.BloodGlucose
import androidx.health.data.client.records.BloodPressure
import androidx.health.data.client.records.BodyFat
import androidx.health.data.client.records.BodyTemperature
import androidx.health.data.client.records.BoneMass
import androidx.health.data.client.records.CervicalMucus
import androidx.health.data.client.records.CervicalPosition
import androidx.health.data.client.records.CyclingPedalingCadence
import androidx.health.data.client.records.Distance
import androidx.health.data.client.records.ElevationGained
import androidx.health.data.client.records.FloorsClimbed
import androidx.health.data.client.records.HeartRate
import androidx.health.data.client.records.HeartRateVariabilityDifferentialIndex
import androidx.health.data.client.records.HeartRateVariabilityRmssd
import androidx.health.data.client.records.HeartRateVariabilityS
import androidx.health.data.client.records.HeartRateVariabilitySd2
import androidx.health.data.client.records.HeartRateVariabilitySdann
import androidx.health.data.client.records.HeartRateVariabilitySdnn
import androidx.health.data.client.records.HeartRateVariabilitySdnnIndex
import androidx.health.data.client.records.HeartRateVariabilitySdsd
import androidx.health.data.client.records.HeartRateVariabilityTinn
import androidx.health.data.client.records.Height
import androidx.health.data.client.records.HipCircumference
import androidx.health.data.client.records.Hydration
import androidx.health.data.client.records.LeanBodyMass
import androidx.health.data.client.records.Menstruation
import androidx.health.data.client.records.Nutrition
import androidx.health.data.client.records.OvulationTest
import androidx.health.data.client.records.OxygenSaturation
import androidx.health.data.client.records.Power
import androidx.health.data.client.records.Repetitions
import androidx.health.data.client.records.RespiratoryRate
import androidx.health.data.client.records.RestingHeartRate
import androidx.health.data.client.records.SexualActivity
import androidx.health.data.client.records.SleepSession
import androidx.health.data.client.records.SleepStage
import androidx.health.data.client.records.Speed
import androidx.health.data.client.records.Steps
import androidx.health.data.client.records.StepsCadence
import androidx.health.data.client.records.SwimmingStrokes
import androidx.health.data.client.records.TotalEnergyBurned
import androidx.health.data.client.records.Vo2Max
import androidx.health.data.client.records.WaistCircumference
import androidx.health.data.client.records.Weight
import androidx.health.data.client.records.WheelchairPushes

private val ALL_RECORDS_TYPES =
    setOf(
        ActiveEnergyBurned::class,
        ActivityEvent::class,
        ActivityLap::class,
        ActivitySession::class,
        BasalMetabolicRate::class,
        BloodGlucose::class,
        BloodPressure::class,
        BodyFat::class,
        BodyTemperature::class,
        BoneMass::class,
        CervicalMucus::class,
        CervicalPosition::class,
        CyclingPedalingCadence::class,
        Distance::class,
        ElevationGained::class,
        FloorsClimbed::class,
        HeartRate::class,
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
        Power::class,
        Repetitions::class,
        RespiratoryRate::class,
        RestingHeartRate::class,
        SexualActivity::class,
        SleepSession::class,
        SleepStage::class,
        Speed::class,
        Steps::class,
        StepsCadence::class,
        SwimmingStrokes::class,
        TotalEnergyBurned::class,
        Vo2Max::class,
        WaistCircumference::class,
        WheelchairPushes::class,
        Weight::class,
    )

val RECORDS_TYPE_NAME_MAP = ALL_RECORDS_TYPES.associateBy { it.simpleName!! }
