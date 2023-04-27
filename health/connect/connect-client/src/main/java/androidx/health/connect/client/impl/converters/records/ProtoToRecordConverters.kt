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
import androidx.health.connect.client.records.CervicalMucusRecord.Companion.APPEARANCE_STRING_TO_INT_MAP
import androidx.health.connect.client.records.CervicalMucusRecord.Companion.SENSATION_STRING_TO_INT_MAP
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
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.celsius
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.kilocaloriesPerDay
import androidx.health.connect.client.units.kilograms
import androidx.health.connect.client.units.liters
import androidx.health.connect.client.units.meters
import androidx.health.connect.client.units.metersPerSecond
import androidx.health.connect.client.units.millimetersOfMercury
import androidx.health.connect.client.units.percent
import androidx.health.connect.client.units.watts
import androidx.health.platform.client.proto.DataProto
import java.time.Instant

/** Converts public API object into internal proto for ipc. */
fun toRecord(proto: DataProto.DataPoint): Record =
    with(proto) {
        when (dataType.name) {
            "BasalBodyTemperature" ->
                BasalBodyTemperatureRecord(
                    temperature = getDouble("temperature").celsius,
                    measurementLocation =
                        mapEnum(
                            "measurementLocation",
                            BodyTemperatureMeasurementLocation
                                .MEASUREMENT_LOCATION_STRING_TO_INT_MAP,
                            BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_UNKNOWN,
                        ),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BasalMetabolicRate" ->
                BasalMetabolicRateRecord(
                    basalMetabolicRate = getDouble("bmr").kilocaloriesPerDay,
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BloodGlucose" ->
                BloodGlucoseRecord(
                    level = BloodGlucose.millimolesPerLiter(getDouble("level")),
                    specimenSource =
                        mapEnum(
                            "specimenSource",
                            BloodGlucoseRecord.SPECIMEN_SOURCE_STRING_TO_INT_MAP,
                            BloodGlucoseRecord.SPECIMEN_SOURCE_UNKNOWN
                        ),
                    mealType =
                        mapEnum(
                            "mealType",
                            MealType.MEAL_TYPE_STRING_TO_INT_MAP,
                            MealType.MEAL_TYPE_UNKNOWN
                        ),
                    relationToMeal =
                        mapEnum(
                            "relationToMeal",
                            BloodGlucoseRecord.RELATION_TO_MEAL_STRING_TO_INT_MAP,
                            BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN
                        ),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BloodPressure" ->
                BloodPressureRecord(
                    systolic = getDouble("systolic").millimetersOfMercury,
                    diastolic = getDouble("diastolic").millimetersOfMercury,
                    bodyPosition =
                        mapEnum(
                            "bodyPosition",
                            BloodPressureRecord.BODY_POSITION_STRING_TO_INT_MAP,
                            BloodPressureRecord.BODY_POSITION_UNKNOWN
                        ),
                    measurementLocation =
                        mapEnum(
                            "measurementLocation",
                            BloodPressureRecord.MEASUREMENT_LOCATION_STRING_TO_INT_MAP,
                            BloodPressureRecord.MEASUREMENT_LOCATION_UNKNOWN
                        ),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BodyFat" ->
                BodyFatRecord(
                    percentage = getDouble("percentage").percent,
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BodyTemperature" ->
                BodyTemperatureRecord(
                    temperature = getDouble("temperature").celsius,
                    measurementLocation =
                        mapEnum(
                            "measurementLocation",
                            BodyTemperatureMeasurementLocation
                                .MEASUREMENT_LOCATION_STRING_TO_INT_MAP,
                            BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_UNKNOWN
                        ),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BodyWaterMass" ->
                BodyWaterMassRecord(
                    mass = getDouble("mass").kilograms,
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "BoneMass" ->
                BoneMassRecord(
                    mass = getDouble("mass").kilograms,
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "CervicalMucus" ->
                CervicalMucusRecord(
                    appearance =
                        mapEnum(
                            "texture",
                            APPEARANCE_STRING_TO_INT_MAP,
                            CervicalMucusRecord.APPEARANCE_UNKNOWN
                        ),
                    sensation =
                        mapEnum(
                            "amount",
                            SENSATION_STRING_TO_INT_MAP,
                            CervicalMucusRecord.SENSATION_UNKNOWN
                        ),
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
                            CyclingPedalingCadenceRecord.Sample(
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
                            HeartRateRecord.Sample(
                                time = Instant.ofEpochMilli(value.instantTimeMillis),
                                beatsPerMinute = value.getLong("bpm"),
                            )
                        },
                    metadata = metadata,
                )
            "Height" ->
                HeightRecord(
                    height = getDouble("height").meters,
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "HeartRateVariabilityRmssd" -> {
                // Ensure that the values being read from old APKs do not crash the client.
                val heartRateVariabilityMillis =
                    when {
                        getDouble("heartRateVariability") <
                            HeartRateVariabilityRmssdRecord.MIN_HRV_RMSSD ->
                            HeartRateVariabilityRmssdRecord.MIN_HRV_RMSSD
                        getDouble("heartRateVariability") >
                            HeartRateVariabilityRmssdRecord.MAX_HRV_RMSSD ->
                            HeartRateVariabilityRmssdRecord.MAX_HRV_RMSSD
                        else -> getDouble("heartRateVariability")
                    }

                HeartRateVariabilityRmssdRecord(
                    heartRateVariabilityMillis = heartRateVariabilityMillis,
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            }
            "LeanBodyMass" ->
                LeanBodyMassRecord(
                    mass = getDouble("mass").kilograms,
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "Menstruation" ->
                MenstruationFlowRecord(
                    flow =
                        mapEnum(
                            "flow",
                            MenstruationFlowRecord.FLOW_TYPE_STRING_TO_INT_MAP,
                            MenstruationFlowRecord.FLOW_UNKNOWN
                        ),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "MenstruationPeriod" ->
                MenstruationPeriodRecord(
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata,
                )
            "OvulationTest" ->
                OvulationTestRecord(
                    result =
                        mapEnum(
                            "result",
                            OvulationTestRecord.RESULT_STRING_TO_INT_MAP,
                            OvulationTestRecord.RESULT_INCONCLUSIVE
                        ),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "OxygenSaturation" ->
                OxygenSaturationRecord(
                    percentage = getDouble("percentage").percent,
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
                            PowerRecord.Sample(
                                time = Instant.ofEpochMilli(value.instantTimeMillis),
                                power = value.getDouble("power").watts,
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
                    protectionUsed =
                        mapEnum(
                            "protectionUsed",
                            SexualActivityRecord.PROTECTION_USED_STRING_TO_INT_MAP,
                            SexualActivityRecord.PROTECTION_USED_UNKNOWN
                        ),
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
                            SpeedRecord.Sample(
                                time = Instant.ofEpochMilli(value.instantTimeMillis),
                                speed = value.getDouble("speed").metersPerSecond,
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
                            StepsCadenceRecord.Sample(
                                time = Instant.ofEpochMilli(value.instantTimeMillis),
                                rate = value.getDouble("rate"),
                            )
                        },
                    metadata = metadata,
                )
            "Vo2Max" ->
                Vo2MaxRecord(
                    vo2MillilitersPerMinuteKilogram = getDouble("vo2"),
                    measurementMethod =
                        mapEnum(
                            "measurementMethod",
                            Vo2MaxRecord.MEASUREMENT_METHOD_STRING_TO_INT_MAP,
                            Vo2MaxRecord.MEASUREMENT_METHOD_OTHER
                        ),
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "Weight" ->
                WeightRecord(
                    weight = getDouble("weight").kilograms,
                    time = time,
                    zoneOffset = zoneOffset,
                    metadata = metadata
                )
            "ActiveCaloriesBurned" ->
                ActiveCaloriesBurnedRecord(
                    energy = getDouble("energy").kilocalories,
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "ActivitySession" ->
                ExerciseSessionRecord(
                    exerciseType =
                        mapEnum(
                            "activityType",
                            ExerciseSessionRecord.EXERCISE_TYPE_STRING_TO_INT_MAP,
                            ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
                        ),
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
                    distance = getDouble("distance").meters,
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "ElevationGained" ->
                ElevationGainedRecord(
                    elevation = getDouble("elevation").meters,
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
                    volume = getDouble("volume").liters,
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "Nutrition" ->
                NutritionRecord(
                    biotin = valuesMap["biotin"]?.doubleVal?.grams,
                    caffeine = valuesMap["caffeine"]?.doubleVal?.grams,
                    calcium = valuesMap["calcium"]?.doubleVal?.grams,
                    energy = valuesMap["calories"]?.doubleVal?.kilocalories,
                    energyFromFat = valuesMap["caloriesFromFat"]?.doubleVal?.kilocalories,
                    chloride = valuesMap["chloride"]?.doubleVal?.grams,
                    cholesterol = valuesMap["cholesterol"]?.doubleVal?.grams,
                    chromium = valuesMap["chromium"]?.doubleVal?.grams,
                    copper = valuesMap["copper"]?.doubleVal?.grams,
                    dietaryFiber = valuesMap["dietaryFiber"]?.doubleVal?.grams,
                    folate = valuesMap["folate"]?.doubleVal?.grams,
                    folicAcid = valuesMap["folicAcid"]?.doubleVal?.grams,
                    iodine = valuesMap["iodine"]?.doubleVal?.grams,
                    iron = valuesMap["iron"]?.doubleVal?.grams,
                    magnesium = valuesMap["magnesium"]?.doubleVal?.grams,
                    manganese = valuesMap["manganese"]?.doubleVal?.grams,
                    molybdenum = valuesMap["molybdenum"]?.doubleVal?.grams,
                    monounsaturatedFat = valuesMap["monounsaturatedFat"]?.doubleVal?.grams,
                    niacin = valuesMap["niacin"]?.doubleVal?.grams,
                    pantothenicAcid = valuesMap["pantothenicAcid"]?.doubleVal?.grams,
                    phosphorus = valuesMap["phosphorus"]?.doubleVal?.grams,
                    polyunsaturatedFat = valuesMap["polyunsaturatedFat"]?.doubleVal?.grams,
                    potassium = valuesMap["potassium"]?.doubleVal?.grams,
                    protein = valuesMap["protein"]?.doubleVal?.grams,
                    riboflavin = valuesMap["riboflavin"]?.doubleVal?.grams,
                    saturatedFat = valuesMap["saturatedFat"]?.doubleVal?.grams,
                    selenium = valuesMap["selenium"]?.doubleVal?.grams,
                    sodium = valuesMap["sodium"]?.doubleVal?.grams,
                    sugar = valuesMap["sugar"]?.doubleVal?.grams,
                    thiamin = valuesMap["thiamin"]?.doubleVal?.grams,
                    totalCarbohydrate = valuesMap["totalCarbohydrate"]?.doubleVal?.grams,
                    totalFat = valuesMap["totalFat"]?.doubleVal?.grams,
                    transFat = valuesMap["transFat"]?.doubleVal?.grams,
                    unsaturatedFat = valuesMap["unsaturatedFat"]?.doubleVal?.grams,
                    vitaminA = valuesMap["vitaminA"]?.doubleVal?.grams,
                    vitaminB12 = valuesMap["vitaminB12"]?.doubleVal?.grams,
                    vitaminB6 = valuesMap["vitaminB6"]?.doubleVal?.grams,
                    vitaminC = valuesMap["vitaminC"]?.doubleVal?.grams,
                    vitaminD = valuesMap["vitaminD"]?.doubleVal?.grams,
                    vitaminE = valuesMap["vitaminE"]?.doubleVal?.grams,
                    vitaminK = valuesMap["vitaminK"]?.doubleVal?.grams,
                    zinc = valuesMap["zinc"]?.doubleVal?.grams,
                    mealType =
                        mapEnum(
                            "mealType",
                            MealType.MEAL_TYPE_STRING_TO_INT_MAP,
                            MealType.MEAL_TYPE_UNKNOWN
                        ),
                    name = getString("name"),
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
                    stages = subTypeDataListsMap["stages"]?.toStageList() ?: listOf(),
                    metadata = metadata
                )
            "SleepStage" ->
                SleepStageRecord(
                    stage =
                        mapEnum(
                            "stage",
                            SleepStageRecord.STAGE_TYPE_STRING_TO_INT_MAP,
                            SleepStageRecord.STAGE_TYPE_UNKNOWN
                        ),
                    startTime = startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = endTime,
                    endZoneOffset = endZoneOffset,
                    metadata = metadata
                )
            "IntermenstrualBleeding" ->
                IntermenstrualBleedingRecord(
                    time = time,
                    zoneOffset = zoneOffset,
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
            "TotalCaloriesBurned" ->
                TotalCaloriesBurnedRecord(
                    energy = getDouble("energy").kilocalories,
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
