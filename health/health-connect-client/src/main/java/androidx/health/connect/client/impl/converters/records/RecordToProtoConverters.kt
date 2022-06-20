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
import androidx.health.connect.client.records.MenstruationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SeriesRecord
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
import androidx.health.platform.client.proto.DataProto

/** Converts public API object into internal proto for ipc. */
fun Record.toProto(): DataProto.DataPoint =
    when (this) {
        is BasalBodyTemperatureRecord ->
            instantaneousProto()
                .setDataType(protoDataType("BasalBodyTemperature"))
                .apply {
                    putValues("temperature", doubleVal(temperature.inCelsius))
                    measurementLocation?.let { putValues("measurementLocation", enumVal(it)) }
                }
                .build()
        is BasalMetabolicRateRecord ->
            instantaneousProto()
                .setDataType(protoDataType("BasalMetabolicRate"))
                .apply { putValues("bmr", doubleVal(basalMetabolicRate.inKilocaloriesPerDay)) }
                .build()
        is BloodGlucoseRecord ->
            instantaneousProto()
                .setDataType(protoDataType("BloodGlucose"))
                .apply {
                    putValues("level", doubleVal(levelMillimolesPerLiter))
                    specimenSource?.let { putValues("specimenSource", enumVal(it)) }
                    mealType?.let { putValues("mealType", enumVal(it)) }
                    relationToMeal?.let { putValues("relationToMeal", enumVal(it)) }
                }
                .build()
        is BloodPressureRecord ->
            instantaneousProto()
                .setDataType(protoDataType("BloodPressure"))
                .apply {
                    putValues("systolic", doubleVal(systolicMillimetersOfMercury))
                    putValues("diastolic", doubleVal(diastolicMillimetersOfMercury))
                    bodyPosition?.let { putValues("bodyPosition", enumVal(it)) }
                    measurementLocation?.let { putValues("measurementLocation", enumVal(it)) }
                }
                .build()
        is BodyFatRecord ->
            instantaneousProto()
                .setDataType(protoDataType("BodyFat"))
                .apply { putValues("percentage", doubleVal(percentage.toDouble())) }
                .build()
        is BodyTemperatureRecord ->
            instantaneousProto()
                .setDataType(protoDataType("BodyTemperature"))
                .apply {
                    putValues("temperature", doubleVal(temperature.inCelsius))
                    measurementLocation?.let { putValues("measurementLocation", enumVal(it)) }
                }
                .build()
        is BodyWaterMassRecord ->
            instantaneousProto()
                .setDataType(protoDataType("BodyWaterMass"))
                .apply { putValues("mass", doubleVal(massKg)) }
                .build()
        is BoneMassRecord ->
            instantaneousProto()
                .setDataType(protoDataType("BoneMass"))
                .apply { putValues("mass", doubleVal(massKg)) }
                .build()
        is CervicalMucusRecord ->
            instantaneousProto()
                .setDataType(protoDataType("CervicalMucus"))
                .apply {
                    appearance?.let { putValues("texture", enumVal(it)) }
                    sensation?.let { putValues("amount", enumVal(it)) }
                }
                .build()
        is CyclingPedalingCadenceRecord ->
            toProto(dataTypeName = "CyclingPedalingCadenceSeries") { sample ->
                DataProto.SeriesValue.newBuilder()
                    .putValues("rpm", doubleVal(sample.revolutionsPerMinute))
                    .setInstantTimeMillis(sample.time.toEpochMilli())
                    .build()
            }
        is HeartRateRecord ->
            toProto(dataTypeName = "HeartRateSeries") { sample ->
                DataProto.SeriesValue.newBuilder()
                    .putValues("bpm", longVal(sample.beatsPerMinute))
                    .setInstantTimeMillis(sample.time.toEpochMilli())
                    .build()
            }
        is HeightRecord ->
            instantaneousProto()
                .setDataType(protoDataType("Height"))
                .apply { putValues("height", doubleVal(height.inMeters)) }
                .build()
        is HipCircumferenceRecord ->
            instantaneousProto()
                .setDataType(protoDataType("HipCircumference"))
                .apply { putValues("circumference", doubleVal(circumference.inMeters)) }
                .build()
        is HeartRateVariabilityDifferentialIndexRecord ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilityDifferentialIndex"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariabilityMillis)) }
                .build()
        is HeartRateVariabilityRmssdRecord ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilityRmssd"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariabilityMillis)) }
                .build()
        is HeartRateVariabilitySRecord ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilityS"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariabilityMillis)) }
                .build()
        is HeartRateVariabilitySd2Record ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilitySd2"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariabilityMillis)) }
                .build()
        is HeartRateVariabilitySdannRecord ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilitySdann"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariabilityMillis)) }
                .build()
        is HeartRateVariabilitySdnnIndexRecord ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilitySdnnIndex"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariabilityMillis)) }
                .build()
        is HeartRateVariabilitySdnnRecord ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilitySdnn"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariabilityMillis)) }
                .build()
        is HeartRateVariabilitySdsdRecord ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilitySdsd"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariabilityMillis)) }
                .build()
        is HeartRateVariabilityTinnRecord ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilityTinn"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariabilityMillis)) }
                .build()
        is LeanBodyMassRecord ->
            instantaneousProto()
                .setDataType(protoDataType("LeanBodyMass"))
                .apply { putValues("mass", doubleVal(massKg)) }
                .build()
        is MenstruationRecord ->
            instantaneousProto()
                .setDataType(protoDataType("Menstruation"))
                .apply { flow?.let { putValues("flow", enumVal(it)) } }
                .build()
        is OvulationTestRecord ->
            instantaneousProto()
                .setDataType(protoDataType("OvulationTest"))
                .apply { putValues("result", enumVal(result)) }
                .build()
        is OxygenSaturationRecord ->
            instantaneousProto()
                .setDataType(protoDataType("OxygenSaturation"))
                .apply { putValues("percentage", doubleVal(percentage.toDouble())) }
                .build()
        is PowerRecord ->
            toProto(dataTypeName = "PowerSeries") { sample ->
                DataProto.SeriesValue.newBuilder()
                    .putValues("power", doubleVal(sample.power.inWatts))
                    .setInstantTimeMillis(sample.time.toEpochMilli())
                    .build()
            }
        is RespiratoryRateRecord ->
            instantaneousProto()
                .setDataType(protoDataType("RespiratoryRate"))
                .apply { putValues("rate", doubleVal(rate)) }
                .build()
        is RestingHeartRateRecord ->
            instantaneousProto()
                .setDataType(protoDataType("RestingHeartRate"))
                .apply { putValues("bpm", longVal(beatsPerMinute)) }
                .build()
        is SexualActivityRecord ->
            instantaneousProto()
                .setDataType(protoDataType("SexualActivity"))
                .apply { protectionUsed?.let { putValues("protectionUsed", enumVal(it)) } }
                .build()
        is SpeedRecord ->
            toProto(dataTypeName = "SpeedSeries") { sample ->
                DataProto.SeriesValue.newBuilder()
                    .putValues("speed", doubleVal(sample.metersPerSecond))
                    .setInstantTimeMillis(sample.time.toEpochMilli())
                    .build()
            }
        is StepsCadenceRecord ->
            toProto(dataTypeName = "StepsCadenceSeries") { sample ->
                DataProto.SeriesValue.newBuilder()
                    .putValues("rate", doubleVal(sample.rate))
                    .setInstantTimeMillis(sample.time.toEpochMilli())
                    .build()
            }
        is Vo2MaxRecord ->
            instantaneousProto()
                .setDataType(protoDataType("Vo2Max"))
                .apply {
                    putValues("vo2", doubleVal(vo2MillilitersPerMinuteKilogram))
                    measurementMethod?.let { putValues("measurementMethod", enumVal(it)) }
                }
                .build()
        is WaistCircumferenceRecord ->
            instantaneousProto()
                .setDataType(protoDataType("WaistCircumference"))
                .apply { putValues("circumference", doubleVal(circumference.inMeters)) }
                .build()
        is WeightRecord ->
            instantaneousProto()
                .setDataType(protoDataType("Weight"))
                .apply { putValues("weight", doubleVal(weightKg)) }
                .build()
        is ActiveCaloriesBurnedRecord ->
            intervalProto()
                .setDataType(protoDataType("ActiveCaloriesBurned"))
                .apply { putValues("energy", doubleVal(energy.inKilocalories)) }
                .build()
        is ExerciseEventRecord ->
            intervalProto()
                .setDataType(protoDataType("ActivityEvent"))
                .apply { putValues("eventType", enumVal(eventType)) }
                .build()
        is ExerciseLapRecord ->
            intervalProto()
                .setDataType(protoDataType("ActivityLap"))
                .apply {
                    if (length != null) {
                        putValues("length", doubleVal(length.inMeters))
                    }
                }
                .build()
        is ExerciseSessionRecord ->
            intervalProto()
                .setDataType(protoDataType("ActivitySession"))
                .apply {
                    putValues("activityType", enumVal(exerciseType))
                    title?.let { putValues("title", stringVal(it)) }
                    notes?.let { putValues("notes", stringVal(it)) }
                }
                .build()
        is DistanceRecord ->
            intervalProto()
                .setDataType(protoDataType("Distance"))
                .apply { putValues("distance", doubleVal(distance.inMeters)) }
                .build()
        is ElevationGainedRecord ->
            intervalProto()
                .setDataType(protoDataType("ElevationGained"))
                .apply { putValues("elevation", doubleVal(elevation.inMeters)) }
                .build()
        is FloorsClimbedRecord ->
            intervalProto()
                .setDataType(protoDataType("FloorsClimbed"))
                .apply { putValues("floors", doubleVal(floors)) }
                .build()
        is HydrationRecord ->
            intervalProto()
                .setDataType(protoDataType("Hydration"))
                .apply { putValues("volume", doubleVal(volumeLiters)) }
                .build()
        is NutritionRecord ->
            intervalProto()
                .setDataType(protoDataType("Nutrition"))
                .apply {
                    if (biotinGrams > 0) {
                        putValues("biotin", doubleVal(biotinGrams))
                    }
                    if (caffeineGrams > 0) {
                        putValues("caffeine", doubleVal(caffeineGrams))
                    }
                    if (calciumGrams > 0) {
                        putValues("calcium", doubleVal(calciumGrams))
                    }
                    if (energy != null) {
                        putValues("calories", doubleVal(energy.inKilocalories))
                    }
                    if (energyFromFat != null) {
                        putValues("caloriesFromFat", doubleVal(energyFromFat.inKilocalories))
                    }
                    if (chlorideGrams > 0) {
                        putValues("chloride", doubleVal(chlorideGrams))
                    }
                    if (cholesterolGrams > 0) {
                        putValues("cholesterol", doubleVal(cholesterolGrams))
                    }
                    if (chromiumGrams > 0) {
                        putValues("chromium", doubleVal(chromiumGrams))
                    }
                    if (copperGrams > 0) {
                        putValues("copper", doubleVal(copperGrams))
                    }
                    if (dietaryFiberGrams > 0) {
                        putValues("dietaryFiber", doubleVal(dietaryFiberGrams))
                    }
                    if (folateGrams > 0) {
                        putValues("folate", doubleVal(folateGrams))
                    }
                    if (folicAcidGrams > 0) {
                        putValues("folicAcid", doubleVal(folicAcidGrams))
                    }
                    if (iodineGrams > 0) {
                        putValues("iodine", doubleVal(iodineGrams))
                    }
                    if (ironGrams > 0) {
                        putValues("iron", doubleVal(ironGrams))
                    }
                    if (magnesiumGrams > 0) {
                        putValues("magnesium", doubleVal(magnesiumGrams))
                    }
                    if (manganeseGrams > 0) {
                        putValues("manganese", doubleVal(manganeseGrams))
                    }
                    if (molybdenumGrams > 0) {
                        putValues("molybdenum", doubleVal(molybdenumGrams))
                    }
                    if (monounsaturatedFatGrams > 0) {
                        putValues("monounsaturatedFat", doubleVal(monounsaturatedFatGrams))
                    }
                    if (niacinGrams > 0) {
                        putValues("niacin", doubleVal(niacinGrams))
                    }
                    if (pantothenicAcidGrams > 0) {
                        putValues("pantothenicAcid", doubleVal(pantothenicAcidGrams))
                    }
                    if (phosphorusGrams > 0) {
                        putValues("phosphorus", doubleVal(phosphorusGrams))
                    }
                    if (polyunsaturatedFatGrams > 0) {
                        putValues("polyunsaturatedFat", doubleVal(polyunsaturatedFatGrams))
                    }
                    if (potassiumGrams > 0) {
                        putValues("potassium", doubleVal(potassiumGrams))
                    }
                    if (proteinGrams > 0) {
                        putValues("protein", doubleVal(proteinGrams))
                    }
                    if (riboflavinGrams > 0) {
                        putValues("riboflavin", doubleVal(riboflavinGrams))
                    }
                    if (saturatedFatGrams > 0) {
                        putValues("saturatedFat", doubleVal(saturatedFatGrams))
                    }
                    if (seleniumGrams > 0) {
                        putValues("selenium", doubleVal(seleniumGrams))
                    }
                    if (sodiumGrams > 0) {
                        putValues("sodium", doubleVal(sodiumGrams))
                    }
                    if (sugarGrams > 0) {
                        putValues("sugar", doubleVal(sugarGrams))
                    }
                    if (thiaminGrams > 0) {
                        putValues("thiamin", doubleVal(thiaminGrams))
                    }
                    if (totalCarbohydrateGrams > 0) {
                        putValues("totalCarbohydrate", doubleVal(totalCarbohydrateGrams))
                    }
                    if (totalFatGrams > 0) {
                        putValues("totalFat", doubleVal(totalFatGrams))
                    }
                    if (transFatGrams > 0) {
                        putValues("transFat", doubleVal(transFatGrams))
                    }
                    if (unsaturatedFatGrams > 0) {
                        putValues("unsaturatedFat", doubleVal(unsaturatedFatGrams))
                    }
                    if (vitaminAGrams > 0) {
                        putValues("vitaminA", doubleVal(vitaminAGrams))
                    }
                    if (vitaminB12Grams > 0) {
                        putValues("vitaminB12", doubleVal(vitaminB12Grams))
                    }
                    if (vitaminB6Grams > 0) {
                        putValues("vitaminB6", doubleVal(vitaminB6Grams))
                    }
                    if (vitaminCGrams > 0) {
                        putValues("vitaminC", doubleVal(vitaminCGrams))
                    }
                    if (vitaminDGrams > 0) {
                        putValues("vitaminD", doubleVal(vitaminDGrams))
                    }
                    if (vitaminEGrams > 0) {
                        putValues("vitaminE", doubleVal(vitaminEGrams))
                    }
                    if (vitaminKGrams > 0) {
                        putValues("vitaminK", doubleVal(vitaminKGrams))
                    }
                    if (zincGrams > 0) {
                        putValues("zinc", doubleVal(zincGrams))
                    }
                    mealType?.let { putValues("mealType", enumVal(it)) }
                    name?.let { putValues("name", stringVal(it)) }
                }
                .build()
        is ExerciseRepetitionsRecord ->
            intervalProto()
                .setDataType(protoDataType("Repetitions"))
                .apply {
                    putValues("count", longVal(count))
                    putValues("type", enumVal(type))
                }
                .build()
        is SleepSessionRecord ->
            intervalProto()
                .setDataType(protoDataType("SleepSession"))
                .apply {
                    title?.let { putValues("title", stringVal(it)) }
                    notes?.let { putValues("notes", stringVal(it)) }
                }
                .build()
        is SleepStageRecord ->
            intervalProto()
                .setDataType(protoDataType("SleepStage"))
                .apply { putValues("stage", enumVal(stage)) }
                .build()
        is StepsRecord ->
            intervalProto()
                .setDataType(protoDataType("Steps"))
                .apply { putValues("count", longVal(count)) }
                .build()
        is SwimmingStrokesRecord ->
            intervalProto()
                .setDataType(protoDataType("SwimmingStrokes"))
                .apply {
                    if (count > 0) {
                        putValues("count", longVal(count))
                    }
                    putValues("type", enumVal(type))
                }
                .build()
        is TotalCaloriesBurnedRecord ->
            intervalProto()
                .setDataType(protoDataType("TotalCaloriesBurned"))
                .apply { putValues("energy", doubleVal(energy.inKilocalories)) }
                .build()
        is WheelchairPushesRecord ->
            intervalProto()
                .setDataType(protoDataType("WheelchairPushes"))
                .apply { putValues("count", longVal(count)) }
                .build()
        else -> throw RuntimeException("Unsupported yet!")
    }

private fun <T : Any> SeriesRecord<T>.toProto(
    dataTypeName: String,
    getSeriesValue: (sample: T) -> DataProto.SeriesValue,
): DataProto.DataPoint =
    intervalProto()
        .setDataType(protoDataType(dataTypeName = dataTypeName))
        .apply {
            for (sample in samples) {
                addSeriesValues(getSeriesValue(sample))
            }
        }
        .build()
