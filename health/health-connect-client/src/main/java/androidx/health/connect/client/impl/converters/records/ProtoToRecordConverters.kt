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

package androidx.health.connect.client.impl.converters.records

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
import androidx.health.connect.client.records.CyclingPedalingCadence
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRate
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
import androidx.health.connect.client.records.Power
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RepetitionsRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.records.Speed
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadence
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.SwimmingStrokesRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalEnergyBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WaistCircumferenceRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.platform.client.proto.DataProto
import java.time.Instant

/** Converts public API object into internal proto for ipc. */
fun toRecord(proto: DataProto.DataPoint): Record =
    with(proto) {
        when (dataType.name) {
            "BasalBodyTemperature" ->
                BasalBodyTemperatureRecord(
                    temperatureDegreesCelsius = getDouble("temperature"),
                    measurementLocation = getEnum("measurementLocation"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BasalMetabolicRate" ->
                BasalMetabolicRateRecord(
                    kcalPerDay = getDouble("bmr"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BloodGlucose" ->
                BloodGlucoseRecord(
                    levelMillimolesPerLiter = getDouble("level"),
                    specimenSource = getEnum("specimenSource"),
                    mealType = getEnum("mealType"),
                    relationToMeal = getEnum("relationToMeal"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BloodPressure" ->
                BloodPressureRecord(
                    systolicMillimetersOfMercury = getDouble("systolic"),
                    diastolicMillimetersOfMercury = getDouble("diastolic"),
                    bodyPosition = getEnum("bodyPosition"),
                    measurementLocation = getEnum("measurementLocation"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BodyFat" ->
                BodyFatRecord(
                    percentage = getDouble("percentage").toInt(),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BodyTemperature" ->
                BodyTemperatureRecord(
                    temperatureDegreesCelsius = getDouble("temperature"),
                    measurementLocation = getEnum("measurementLocation"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BodyWaterMass" ->
                BodyWaterMassRecord(
                    massKg = getDouble("mass"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BoneMass" ->
                BoneMassRecord(
                    massKg = getDouble("mass"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "CervicalMucus" ->
                CervicalMucusRecord(
                    appearance = getEnum("texture"),
                    sensation = getEnum("amount"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "CyclingPedalingCadenceSeries" ->
                CyclingPedalingCadenceRecord(
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    samples =
                        seriesValuesList.map { value ->
                            CyclingPedalingCadence(
                                time = Instant.ofEpochMilli(value.instantTimeMillis),
                                revolutionsPerMinute = value.getDouble("rpm"),
                            )
                        },
                    metadata = metadata,
                )
            "HeartRateSeries" ->
                HeartRateRecord(
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    samples =
                        seriesValuesList.map { value ->
                            HeartRate(
                                time = Instant.ofEpochMilli(value.instantTimeMillis),
                                beatsPerMinute = value.getLong("bpm"),
                            )
                        },
                    metadata = metadata,
                )
            "Height" ->
                HeightRecord(
                    heightMeters = getDouble("height"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HipCircumference" ->
                HipCircumferenceRecord(
                    circumferenceMeters = getDouble("circumference"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilityDifferentialIndex" ->
                HeartRateVariabilityDifferentialIndexRecord(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilityRmssd" ->
                HeartRateVariabilityRmssdRecord(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilityS" ->
                HeartRateVariabilitySRecord(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySd2" ->
                HeartRateVariabilitySd2Record(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySdann" ->
                HeartRateVariabilitySdannRecord(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySdnnIndex" ->
                HeartRateVariabilitySdnnIndexRecord(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySdnn" ->
                HeartRateVariabilitySdnnRecord(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySdsd" ->
                HeartRateVariabilitySdsdRecord(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilityTinn" ->
                HeartRateVariabilityTinnRecord(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "LeanBodyMass" ->
                LeanBodyMassRecord(
                    massKg = getDouble("mass"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "Menstruation" ->
                MenstruationRecord(
                    flow = getEnum("flow"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "OvulationTest" ->
                OvulationTestRecord(
                    result = getEnum("result") ?: "",
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "OxygenSaturation" ->
                OxygenSaturationRecord(
                    percentage = getDouble("percentage").toInt(),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "PowerSeries" ->
                PowerRecord(
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    samples =
                        seriesValuesList.map { value ->
                            Power(
                                time = Instant.ofEpochMilli(value.instantTimeMillis),
                                watts = value.getDouble("power"),
                            )
                        },
                    metadata = metadata,
                )
            "RespiratoryRate" ->
                RespiratoryRateRecord(
                    rate = getDouble("rate"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "RestingHeartRate" ->
                RestingHeartRateRecord(
                    beatsPerMinute = getLong("bpm"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "SexualActivity" ->
                SexualActivityRecord(
                    protectionUsed = getEnum("protectionUsed"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "SpeedSeries" ->
                SpeedRecord(
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    samples =
                        seriesValuesList.map { value ->
                            Speed(
                                time = Instant.ofEpochMilli(value.instantTimeMillis),
                                metersPerSecond = value.getDouble("speed"),
                            )
                        },
                    metadata = metadata,
                )
            "StepsCadenceSeries" ->
                StepsCadenceRecord(
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    samples =
                        seriesValuesList.map { value ->
                            StepsCadence(
                                time = Instant.ofEpochMilli(value.instantTimeMillis),
                                rate = value.getDouble("rate"),
                            )
                        },
                    metadata = metadata,
                )
            "Vo2Max" ->
                Vo2MaxRecord(
                    vo2MillilitersPerMinuteKilogram = getDouble("vo2"),
                    measurementMethod = getEnum("measurementMethod"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "WaistCircumference" ->
                WaistCircumferenceRecord(
                    circumferenceMeters = getDouble("circumference"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "Weight" ->
                WeightRecord(
                    weightKg = getDouble("weight"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "ActiveCaloriesBurned" ->
                ActiveCaloriesBurnedRecord(
                    energyKcal = getDouble("energy"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "ActiveEnergyBurned" ->
                ActiveEnergyBurnedRecord(
                    energyKcal = getDouble("energy"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "ActivityEvent" ->
                ActivityEventRecord(
                    eventType = getEnum("eventType") ?: "",
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "ActivityLap" ->
                ActivityLapRecord(
                    lengthMeters = getDouble("length"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "ActivitySession" ->
                ActivitySessionRecord(
                    activityType = getEnum("activityType") ?: "",
                    title = getString("title"),
                    notes = getString("notes"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "Distance" ->
                DistanceRecord(
                    distanceMeters = getDouble("distance"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "ElevationGained" ->
                ElevationGainedRecord(
                    elevationMeters = getDouble("elevation"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "FloorsClimbed" ->
                FloorsClimbedRecord(
                    floors = getDouble("floors"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "Hydration" ->
                HydrationRecord(
                    volumeLiters = getDouble("volume"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "Nutrition" ->
                NutritionRecord(
                    biotinGrams = getDouble("biotin"),
                    caffeineGrams = getDouble("caffeine"),
                    calciumGrams = getDouble("calcium"),
                    kcal = getDouble("calories"),
                    kcalFromFat = getDouble("caloriesFromFat"),
                    chlorideGrams = getDouble("chloride"),
                    cholesterolGrams = getDouble("cholesterol"),
                    chromiumGrams = getDouble("chromium"),
                    copperGrams = getDouble("copper"),
                    dietaryFiberGrams = getDouble("dietaryFiber"),
                    folateGrams = getDouble("folate"),
                    folicAcidGrams = getDouble("folicAcid"),
                    iodineGrams = getDouble("iodine"),
                    ironGrams = getDouble("iron"),
                    magnesiumGrams = getDouble("magnesium"),
                    manganeseGrams = getDouble("manganese"),
                    molybdenumGrams = getDouble("molybdenum"),
                    monounsaturatedFatGrams = getDouble("monounsaturatedFat"),
                    niacinGrams = getDouble("niacin"),
                    pantothenicAcidGrams = getDouble("pantothenicAcid"),
                    phosphorusGrams = getDouble("phosphorus"),
                    polyunsaturatedFatGrams = getDouble("polyunsaturatedFat"),
                    potassiumGrams = getDouble("potassium"),
                    proteinGrams = getDouble("protein"),
                    riboflavinGrams = getDouble("riboflavin"),
                    saturatedFatGrams = getDouble("saturatedFat"),
                    seleniumGrams = getDouble("selenium"),
                    sodiumGrams = getDouble("sodium"),
                    sugarGrams = getDouble("sugar"),
                    thiaminGrams = getDouble("thiamin"),
                    totalCarbohydrateGrams = getDouble("totalCarbohydrate"),
                    totalFatGrams = getDouble("totalFat"),
                    transFatGrams = getDouble("transFat"),
                    unsaturatedFatGrams = getDouble("unsaturatedFat"),
                    vitaminAGrams = getDouble("vitaminA"),
                    vitaminB12Grams = getDouble("vitaminB12"),
                    vitaminB6Grams = getDouble("vitaminB6"),
                    vitaminCGrams = getDouble("vitaminC"),
                    vitaminDGrams = getDouble("vitaminD"),
                    vitaminEGrams = getDouble("vitaminE"),
                    vitaminKGrams = getDouble("vitaminK"),
                    zincGrams = getDouble("zinc"),
                    mealType = getEnum("mealType"),
                    name = getString("name"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "Repetitions" ->
                RepetitionsRecord(
                    count = getLong("count"),
                    type = getEnum("type") ?: "",
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "SleepSession" ->
                SleepSessionRecord(
                    title = getString("title"),
                    notes = getString("notes"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "SleepStage" ->
                SleepStageRecord(
                    stage = getEnum("stage") ?: "",
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "Steps" ->
                StepsRecord(
                    count = getLong("count"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "SwimmingStrokes" ->
                SwimmingStrokesRecord(
                    count = getLong("count"),
                    type = getEnum("type") ?: "",
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "TotalCaloriesBurned" ->
                TotalCaloriesBurnedRecord(
                    energyKcal = getDouble("energy"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "TotalEnergyBurned" ->
                TotalEnergyBurnedRecord(
                    energyKcal = getDouble("energy"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "WheelchairPushes" ->
                WheelchairPushesRecord(
                    count = getLong("count"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            else -> throw RuntimeException("Unknown data type ${dataType.name}")
        }
    }
