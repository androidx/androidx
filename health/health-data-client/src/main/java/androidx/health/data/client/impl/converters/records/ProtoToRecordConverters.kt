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
import androidx.health.data.client.records.TotalEnergyBurned
import androidx.health.data.client.records.Vo2Max
import androidx.health.data.client.records.WaistCircumference
import androidx.health.data.client.records.Weight
import androidx.health.data.client.records.WheelchairPushes
import androidx.health.platform.client.proto.DataProto
import java.lang.RuntimeException

/** Converts public API object into internal proto for ipc. */
fun toRecord(proto: DataProto.DataPoint): Record {
    return with(proto) {
        when (dataType.name) {
            "BasalMetabolicRate" ->
                BasalMetabolicRate(
                    bmr = getDouble("bmr"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BloodGlucose" ->
                BloodGlucose(
                    level = getDouble("level"),
                    specimenSource = getEnum("specimenSource"),
                    mealType = getEnum("mealType"),
                    relationToMeal = getEnum("relationToMeal"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BloodPressure" ->
                BloodPressure(
                    systolic = getDouble("systolic"),
                    diastolic = getDouble("diastolic"),
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
                    temperature = getDouble("temperature"),
                    measurementLocation = getEnum("measurementLocation"),
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
                    rpm = getDouble("rpm"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRate" ->
                HeartRate(
                    bpm = getLong("bpm"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
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
                    heartRateVariability = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilityRmssd" ->
                HeartRateVariabilityRmssd(
                    heartRateVariability = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilityS" ->
                HeartRateVariabilityS(
                    heartRateVariability = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySd2" ->
                HeartRateVariabilitySd2(
                    heartRateVariability = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySdann" ->
                HeartRateVariabilitySdann(
                    heartRateVariability = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySdnnIndex" ->
                HeartRateVariabilitySdnnIndex(
                    heartRateVariability = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySdnn" ->
                HeartRateVariabilitySdnn(
                    heartRateVariability = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilitySdsd" ->
                HeartRateVariabilitySdsd(
                    heartRateVariability = getDouble("heartRateVariability"),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilityTinn" ->
                HeartRateVariabilityTinn(
                    heartRateVariability = getDouble("heartRateVariability"),
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
                    bpm = getLong("bpm"),
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
                    speed = getDouble("speed"),
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
                    vo2 = getDouble("vo2"),
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
            "ActiveEnergyBurned" ->
                ActiveEnergyBurned(
                    energy = getDouble("energy"),
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
                    volume = getDouble("volume"),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "Nutrition" ->
                Nutrition(
                    biotin = getDouble("biotin"),
                    caffeine = getDouble("caffeine"),
                    calcium = getDouble("calcium"),
                    calories = getDouble("calories"),
                    caloriesFromFat = getDouble("caloriesFromFat"),
                    chloride = getDouble("chloride"),
                    cholesterol = getDouble("cholesterol"),
                    chromium = getDouble("chromium"),
                    copper = getDouble("copper"),
                    dietaryFiber = getDouble("dietaryFiber"),
                    folate = getDouble("folate"),
                    folicAcid = getDouble("folicAcid"),
                    iodine = getDouble("iodine"),
                    iron = getDouble("iron"),
                    magnesium = getDouble("magnesium"),
                    manganese = getDouble("manganese"),
                    molybdenum = getDouble("molybdenum"),
                    monounsaturatedFat = getDouble("monounsaturatedFat"),
                    niacin = getDouble("niacin"),
                    pantothenicAcid = getDouble("pantothenicAcid"),
                    phosphorus = getDouble("phosphorus"),
                    polyunsaturatedFat = getDouble("polyunsaturatedFat"),
                    potassium = getDouble("potassium"),
                    protein = getDouble("protein"),
                    riboflavin = getDouble("riboflavin"),
                    saturatedFat = getDouble("saturatedFat"),
                    selenium = getDouble("selenium"),
                    sodium = getDouble("sodium"),
                    sugar = getDouble("sugar"),
                    thiamin = getDouble("thiamin"),
                    totalCarbohydrate = getDouble("totalCarbohydrate"),
                    totalFat = getDouble("totalFat"),
                    transFat = getDouble("transFat"),
                    unsaturatedFat = getDouble("unsaturatedFat"),
                    vitaminA = getDouble("vitaminA"),
                    vitaminB12 = getDouble("vitaminB12"),
                    vitaminB6 = getDouble("vitaminB6"),
                    vitaminC = getDouble("vitaminC"),
                    vitaminD = getDouble("vitaminD"),
                    vitaminE = getDouble("vitaminE"),
                    vitaminK = getDouble("vitaminK"),
                    zinc = getDouble("zinc"),
                    mealType = getEnum("mealType"),
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
            "TotalEnergyBurned" ->
                TotalEnergyBurned(
                    energy = getDouble("energy"),
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
}
