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
package androidx.health.data.client.impl.converters.records

import androidx.health.data.client.records.ActiveCaloriesBurned
import androidx.health.data.client.records.ActivityEvent
import androidx.health.data.client.records.ActivityLap
import androidx.health.data.client.records.ActivitySession
import androidx.health.data.client.records.BasalBodyTemperature
import androidx.health.data.client.records.BasalMetabolicRate
import androidx.health.data.client.records.BloodGlucose
import androidx.health.data.client.records.BloodPressure
import androidx.health.data.client.records.BodyFat
import androidx.health.data.client.records.BodyTemperature
import androidx.health.data.client.records.BodyWaterMass
import androidx.health.data.client.records.BoneMass
import androidx.health.data.client.records.CervicalMucus
import androidx.health.data.client.records.CervicalPosition
import androidx.health.data.client.records.CyclingPedalingCadence
import androidx.health.data.client.records.Distance
import androidx.health.data.client.records.ElevationGained
import androidx.health.data.client.records.FloorsClimbed
import androidx.health.data.client.records.HeartRate
import androidx.health.data.client.records.HeartRateSeries
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
import androidx.health.data.client.records.Record
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
import androidx.health.data.client.records.TotalCaloriesBurned
import androidx.health.data.client.records.Vo2Max
import androidx.health.data.client.records.WaistCircumference
import androidx.health.data.client.records.Weight
import androidx.health.data.client.records.WheelchairPushes
import androidx.health.platform.client.proto.DataProto
import java.time.Instant

/** Converts public API object into internal proto for ipc. */
fun toRecord(proto: DataProto.DataPoint): Record =
    with(proto) {
        when (dataType.name) {
            "BasalBodyTemperature" ->
                BasalBodyTemperature(
                    temperatureDegreesCelsius = getDouble("temperature"),
                    measurementLocation = getEnum("measurementLocation"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BasalMetabolicRate" ->
                BasalMetabolicRate(
                    kcalPerDay = getDouble("bmr"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BloodGlucose" ->
                BloodGlucose(
                    levelMillimolesPerLiter = getDouble("level"),
                    specimenSource = getEnum("specimenSource"),
                    mealType = getEnum("mealType"),
                    relationToMeal = getEnum("relationToMeal"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BloodPressure" ->
                BloodPressure(
                    systolicMillimetersOfMercury = getDouble("systolic"),
                    diastolicMillimetersOfMercury = getDouble("diastolic"),
                    bodyPosition = getEnum("bodyPosition"),
                    measurementLocation = getEnum("measurementLocation"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BodyFat" ->
                BodyFat(
                    percentage = getDouble("percentage"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BodyTemperature" ->
                BodyTemperature(
                    temperatureDegreesCelsius = getDouble("temperature"),
                    measurementLocation = getEnum("measurementLocation"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BodyWaterMass" ->
                BodyWaterMass(
                    massKg = getDouble("mass"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BoneMass" ->
                BoneMass(
                    massKg = getDouble("mass"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "CervicalMucus" ->
                CervicalMucus(
                    texture = getEnum("texture"),
                    amount = getEnum("amount"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "CervicalPosition" ->
                CervicalPosition(
                    position = getEnum("position"),
                    dilation = getEnum("dilation"),
                    firmness = getEnum("firmness"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "CyclingPedalingCadence" ->
                CyclingPedalingCadence(
                    revolutionsPerMinute = getDouble("rpm"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateSeries" ->
                HeartRateSeries(
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
                Height(
                    heightMeters = getDouble("height"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HipCircumference" ->
                HipCircumference(
                    circumferenceMeters = getDouble("circumference"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilityDifferentialIndex" ->
                HeartRateVariabilityDifferentialIndex(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilityRmssd" ->
                HeartRateVariabilityRmssd(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilityS" ->
                HeartRateVariabilityS(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySd2" ->
                HeartRateVariabilitySd2(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySdann" ->
                HeartRateVariabilitySdann(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySdnnIndex" ->
                HeartRateVariabilitySdnnIndex(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySdnn" ->
                HeartRateVariabilitySdnn(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySdsd" ->
                HeartRateVariabilitySdsd(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilityTinn" ->
                HeartRateVariabilityTinn(
                    heartRateVariabilityMillis = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "LeanBodyMass" ->
                LeanBodyMass(
                    massKg = getDouble("mass"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "Menstruation" ->
                Menstruation(
                    flow = getEnum("flow"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "OvulationTest" ->
                OvulationTest(
                    result = getEnum("result") ?: "",
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "OxygenSaturation" ->
                OxygenSaturation(
                    percentage = getDouble("percentage"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "Power" ->
                Power(
                    power = getDouble("power"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "RespiratoryRate" ->
                RespiratoryRate(
                    rate = getDouble("rate"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "RestingHeartRate" ->
                RestingHeartRate(
                    beatsPerMinute = getLong("bpm"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "SexualActivity" ->
                SexualActivity(
                    protectionUsed = getEnum("protectionUsed"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "Speed" ->
                Speed(
                    speedMetersPerSecond = getDouble("speed"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "StepsCadence" ->
                StepsCadence(
                    rate = getDouble("rate"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "Vo2Max" ->
                Vo2Max(
                    vo2MillilitersPerMinuteKilogram = getDouble("vo2"),
                    measurementMethod = getEnum("measurementMethod"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "WaistCircumference" ->
                WaistCircumference(
                    circumferenceMeters = getDouble("circumference"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "Weight" ->
                Weight(
                    weightKg = getDouble("weight"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "ActiveCaloriesBurned" ->
                ActiveCaloriesBurned(
                    energyKcal = getDouble("energy"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "ActivityEvent" ->
                ActivityEvent(
                    eventType = getEnum("eventType") ?: "",
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "ActivityLap" ->
                ActivityLap(
                    lengthMeters = getDouble("length"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "ActivitySession" ->
                ActivitySession(
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
                Distance(
                    distanceMeters = getDouble("distance"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "ElevationGained" ->
                ElevationGained(
                    elevationMeters = getDouble("elevation"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "FloorsClimbed" ->
                FloorsClimbed(
                    floors = getDouble("floors"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "Hydration" ->
                Hydration(
                    volumeLiters = getDouble("volume"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "Nutrition" ->
                Nutrition(
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
                Repetitions(
                    count = getLong("count"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "SleepSession" ->
                SleepSession(
                    title = getString("title"),
                    notes = getString("notes"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "SleepStage" ->
                SleepStage(
                    stage = getEnum("stage") ?: "",
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "Steps" ->
                Steps(
                    count = getLong("count"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "SwimmingStrokes" ->
                SwimmingStrokes(
                    count = getLong("count"),
                    type = getEnum("type") ?: "",
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "TotalCaloriesBurned" ->
                TotalCaloriesBurned(
                    energyKcal = getDouble("energy"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "WheelchairPushes" ->
                WheelchairPushes(
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
