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
import androidx.health.connect.client.records.BodyTemperatureMeasurementLocation
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.CervicalMucusRecord.Companion.APPEARANCE_INT_TO_STRING_MAP
import androidx.health.connect.client.records.CervicalMucusRecord.Companion.SENSATION_INT_TO_STRING_MAP
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
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
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
import androidx.health.connect.client.records.SleepStageRecord.Companion.STAGE_TYPE_INT_TO_STRING_MAP
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
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
                    enumValFromInt(
                        measurementLocation,
                        BodyTemperatureMeasurementLocation
                            .MEASUREMENT_LOCATION_INT_TO_STRING_MAP,
                    )
                        ?.let { putValues("measurementLocation", it) }
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
                    putValues("level", doubleVal(level.inMillimolesPerLiter))
                    enumValFromInt(
                        specimenSource,
                        BloodGlucoseRecord.SPECIMEN_SOURCE_INT_TO_STRING_MAP
                    )
                        ?.let { putValues("specimenSource", it) }
                    enumValFromInt(mealType, MealType.MEAL_TYPE_INT_TO_STRING_MAP)?.let {
                        putValues("mealType", it)
                    }
                    enumValFromInt(
                        relationToMeal,
                        BloodGlucoseRecord.RELATION_TO_MEAL_INT_TO_STRING_MAP,
                    )
                        ?.let { putValues("relationToMeal", it) }
                }
                .build()

        is BloodPressureRecord ->
            instantaneousProto()
                .setDataType(protoDataType("BloodPressure"))
                .apply {
                    putValues("systolic", doubleVal(systolic.inMillimetersOfMercury))
                    putValues("diastolic", doubleVal(diastolic.inMillimetersOfMercury))
                    enumValFromInt(
                        bodyPosition,
                        BloodPressureRecord.BODY_POSITION_INT_TO_STRING_MAP
                    )
                        ?.let { putValues("bodyPosition", it) }
                    enumValFromInt(
                        measurementLocation,
                        BloodPressureRecord.MEASUREMENT_LOCATION_INT_TO_STRING_MAP
                    )
                        ?.let { putValues("measurementLocation", it) }
                }
                .build()

        is BodyFatRecord ->
            instantaneousProto()
                .setDataType(protoDataType("BodyFat"))
                .apply { putValues("percentage", doubleVal(percentage.value)) }
                .build()

        is BodyTemperatureRecord ->
            instantaneousProto()
                .setDataType(protoDataType("BodyTemperature"))
                .apply {
                    putValues("temperature", doubleVal(temperature.inCelsius))
                    enumValFromInt(
                        measurementLocation,
                        BodyTemperatureMeasurementLocation
                            .MEASUREMENT_LOCATION_INT_TO_STRING_MAP,
                    )
                        ?.let { putValues("measurementLocation", it) }
                }
                .build()

        is BodyWaterMassRecord ->
            instantaneousProto()
                .setDataType(protoDataType("BodyWaterMass"))
                .apply { putValues("mass", doubleVal(mass.inKilograms)) }
                .build()

        is BoneMassRecord ->
            instantaneousProto()
                .setDataType(protoDataType("BoneMass"))
                .apply { putValues("mass", doubleVal(mass.inKilograms)) }
                .build()

        is CervicalMucusRecord ->
            instantaneousProto()
                .setDataType(protoDataType("CervicalMucus"))
                .apply {
                    enumValFromInt(appearance, APPEARANCE_INT_TO_STRING_MAP)?.let {
                        putValues("texture", it)
                    }
                    enumValFromInt(sensation, SENSATION_INT_TO_STRING_MAP)?.let {
                        putValues("amount", it)
                    }
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

        is HeartRateVariabilityRmssdRecord ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilityRmssd"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariabilityMillis)) }
                .build()

        is IntermenstrualBleedingRecord ->
            instantaneousProto().setDataType(protoDataType("IntermenstrualBleeding")).build()

        is LeanBodyMassRecord ->
            instantaneousProto()
                .setDataType(protoDataType("LeanBodyMass"))
                .apply { putValues("mass", doubleVal(mass.inKilograms)) }
                .build()

        is MenstruationFlowRecord ->
            instantaneousProto()
                .setDataType(protoDataType("Menstruation"))
                .apply {
                    enumValFromInt(flow, MenstruationFlowRecord.FLOW_TYPE_INT_TO_STRING_MAP)?.let {
                        putValues("flow", it)
                    }
                }
                .build()

        is MenstruationPeriodRecord ->
            intervalProto().setDataType(protoDataType("MenstruationPeriod")).build()

        is OvulationTestRecord ->
            instantaneousProto()
                .setDataType(protoDataType("OvulationTest"))
                .apply {
                    enumValFromInt(result, OvulationTestRecord.RESULT_INT_TO_STRING_MAP)?.let {
                        putValues("result", it)
                    }
                }
                .build()

        is OxygenSaturationRecord ->
            instantaneousProto()
                .setDataType(protoDataType("OxygenSaturation"))
                .apply { putValues("percentage", doubleVal(percentage.value)) }
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
                .apply {
                    enumValFromInt(
                        protectionUsed,
                        SexualActivityRecord.PROTECTION_USED_INT_TO_STRING_MAP
                    )
                        ?.let { putValues("protectionUsed", it) }
                }
                .build()

        is SpeedRecord ->
            toProto(dataTypeName = "SpeedSeries") { sample ->
                DataProto.SeriesValue.newBuilder()
                    .putValues("speed", doubleVal(sample.speed.inMetersPerSecond))
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
                    enumValFromInt(
                        measurementMethod,
                        Vo2MaxRecord.MEASUREMENT_METHOD_INT_TO_STRING_MAP
                    )
                        ?.let { putValues("measurementMethod", it) }
                }
                .build()

        is WeightRecord ->
            instantaneousProto()
                .setDataType(protoDataType("Weight"))
                .apply { putValues("weight", doubleVal(weight.inKilograms)) }
                .build()

        is ActiveCaloriesBurnedRecord ->
            intervalProto()
                .setDataType(protoDataType("ActiveCaloriesBurned"))
                .apply { putValues("energy", doubleVal(energy.inKilocalories)) }
                .build()

        is ExerciseSessionRecord ->
            intervalProto()
                .setDataType(protoDataType("ActivitySession"))
                .apply {
                    val exerciseType =
                        enumValFromInt(
                            exerciseType,
                            ExerciseSessionRecord.EXERCISE_TYPE_INT_TO_STRING_MAP
                        )
                            ?: enumVal("workout")
                    putValues("activityType", exerciseType)
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
                .apply { putValues("volume", doubleVal(volume.inLiters)) }
                .build()

        is NutritionRecord ->
            intervalProto()
                .setDataType(protoDataType("Nutrition"))
                .apply {
                    if (biotin != null) {
                        putValues("biotin", doubleVal(biotin.inGrams))
                    }
                    if (caffeine != null) {
                        putValues("caffeine", doubleVal(caffeine.inGrams))
                    }
                    if (calcium != null) {
                        putValues("calcium", doubleVal(calcium.inGrams))
                    }
                    if (energy != null) {
                        putValues("calories", doubleVal(energy.inKilocalories))
                    }
                    if (energyFromFat != null) {
                        putValues("caloriesFromFat", doubleVal(energyFromFat.inKilocalories))
                    }
                    if (chloride != null) {
                        putValues("chloride", doubleVal(chloride.inGrams))
                    }
                    if (cholesterol != null) {
                        putValues("cholesterol", doubleVal(cholesterol.inGrams))
                    }
                    if (chromium != null) {
                        putValues("chromium", doubleVal(chromium.inGrams))
                    }
                    if (copper != null) {
                        putValues("copper", doubleVal(copper.inGrams))
                    }
                    if (dietaryFiber != null) {
                        putValues("dietaryFiber", doubleVal(dietaryFiber.inGrams))
                    }
                    if (folate != null) {
                        putValues("folate", doubleVal(folate.inGrams))
                    }
                    if (folicAcid != null) {
                        putValues("folicAcid", doubleVal(folicAcid.inGrams))
                    }
                    if (iodine != null) {
                        putValues("iodine", doubleVal(iodine.inGrams))
                    }
                    if (iron != null) {
                        putValues("iron", doubleVal(iron.inGrams))
                    }
                    if (magnesium != null) {
                        putValues("magnesium", doubleVal(magnesium.inGrams))
                    }
                    if (manganese != null) {
                        putValues("manganese", doubleVal(manganese.inGrams))
                    }
                    if (molybdenum != null) {
                        putValues("molybdenum", doubleVal(molybdenum.inGrams))
                    }
                    if (monounsaturatedFat != null) {
                        putValues("monounsaturatedFat", doubleVal(monounsaturatedFat.inGrams))
                    }
                    if (niacin != null) {
                        putValues("niacin", doubleVal(niacin.inGrams))
                    }
                    if (pantothenicAcid != null) {
                        putValues("pantothenicAcid", doubleVal(pantothenicAcid.inGrams))
                    }
                    if (phosphorus != null) {
                        putValues("phosphorus", doubleVal(phosphorus.inGrams))
                    }
                    if (polyunsaturatedFat != null) {
                        putValues("polyunsaturatedFat", doubleVal(polyunsaturatedFat.inGrams))
                    }
                    if (potassium != null) {
                        putValues("potassium", doubleVal(potassium.inGrams))
                    }
                    if (protein != null) {
                        putValues("protein", doubleVal(protein.inGrams))
                    }
                    if (riboflavin != null) {
                        putValues("riboflavin", doubleVal(riboflavin.inGrams))
                    }
                    if (saturatedFat != null) {
                        putValues("saturatedFat", doubleVal(saturatedFat.inGrams))
                    }
                    if (selenium != null) {
                        putValues("selenium", doubleVal(selenium.inGrams))
                    }
                    if (sodium != null) {
                        putValues("sodium", doubleVal(sodium.inGrams))
                    }
                    if (sugar != null) {
                        putValues("sugar", doubleVal(sugar.inGrams))
                    }
                    if (thiamin != null) {
                        putValues("thiamin", doubleVal(thiamin.inGrams))
                    }
                    if (totalCarbohydrate != null) {
                        putValues("totalCarbohydrate", doubleVal(totalCarbohydrate.inGrams))
                    }
                    if (totalFat != null) {
                        putValues("totalFat", doubleVal(totalFat.inGrams))
                    }
                    if (transFat != null) {
                        putValues("transFat", doubleVal(transFat.inGrams))
                    }
                    if (unsaturatedFat != null) {
                        putValues("unsaturatedFat", doubleVal(unsaturatedFat.inGrams))
                    }
                    if (vitaminA != null) {
                        putValues("vitaminA", doubleVal(vitaminA.inGrams))
                    }
                    if (vitaminB12 != null) {
                        putValues("vitaminB12", doubleVal(vitaminB12.inGrams))
                    }
                    if (vitaminB6 != null) {
                        putValues("vitaminB6", doubleVal(vitaminB6.inGrams))
                    }
                    if (vitaminC != null) {
                        putValues("vitaminC", doubleVal(vitaminC.inGrams))
                    }
                    if (vitaminD != null) {
                        putValues("vitaminD", doubleVal(vitaminD.inGrams))
                    }
                    if (vitaminE != null) {
                        putValues("vitaminE", doubleVal(vitaminE.inGrams))
                    }
                    if (vitaminK != null) {
                        putValues("vitaminK", doubleVal(vitaminK.inGrams))
                    }
                    if (zinc != null) {
                        putValues("zinc", doubleVal(zinc.inGrams))
                    }
                    enumValFromInt(mealType, MealType.MEAL_TYPE_INT_TO_STRING_MAP)?.let {
                        putValues("mealType", it)
                    }
                    name?.let { putValues("name", stringVal(it)) }
                }
                .build()

        is SleepSessionRecord ->
            intervalProto()
                .setDataType(protoDataType("SleepSession"))
                .putSubTypeDataLists("stages",
                    DataProto.DataPoint.SubTypeDataList.newBuilder()
                        .addAllValues(stages.map { it.toProto() }).build()
                )
                .apply {
                    title?.let { putValues("title", stringVal(it)) }
                    notes?.let { putValues("notes", stringVal(it)) }
                }
                .build()

        is SleepStageRecord ->
            intervalProto()
                .setDataType(protoDataType("SleepStage"))
                .apply {
                    enumValFromInt(stage, STAGE_TYPE_INT_TO_STRING_MAP)?.let {
                        putValues("stage", it)
                    }
                }
                .build()

        is StepsRecord ->
            intervalProto()
                .setDataType(protoDataType("Steps"))
                .apply { putValues("count", longVal(count)) }
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
