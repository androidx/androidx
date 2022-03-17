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
@SuppressWarnings("NewApi") // Safe to use with java8 desugar
fun Record.toProto(): DataProto.DataPoint {
    return when (this) {
        is BasalMetabolicRate ->
            instantaneousProto()
                .setDataType(protoDataType("BasalMetabolicRate"))
                .apply { putValues("bmr", doubleVal(bmr)) }
                .build()
        is BloodGlucose ->
            instantaneousProto()
                .setDataType(protoDataType("BloodGlucose"))
                .apply {
                    putValues("level", doubleVal(level))
                    specimenSource?.let { putValues("specimenSource", enumVal(it)) }
                    mealType?.let { putValues("mealType", enumVal(it)) }
                    relationToMeal?.let { putValues("relationToMeal", enumVal(it)) }
                }
                .build()
        is BloodPressure ->
            instantaneousProto()
                .setDataType(protoDataType("BloodPressure"))
                .apply {
                    putValues("systolic", doubleVal(systolic))
                    putValues("diastolic", doubleVal(diastolic))
                    bodyPosition?.let { putValues("bodyPosition", enumVal(it)) }
                    measurementLocation?.let { putValues("measurementLocation", enumVal(it)) }
                }
                .build()
        is BodyFat ->
            instantaneousProto()
                .setDataType(protoDataType("BodyFat"))
                .apply { putValues("percentage", doubleVal(percentage)) }
                .build()
        is BodyTemperature ->
            instantaneousProto()
                .setDataType(protoDataType("BodyTemperature"))
                .apply {
                    putValues("temperature", doubleVal(temperature))
                    measurementLocation?.let { putValues("measurementLocation", enumVal(it)) }
                }
                .build()
        is BoneMass ->
            instantaneousProto()
                .setDataType(protoDataType("BoneMass"))
                .apply { putValues("mass", doubleVal(massKg)) }
                .build()
        is CervicalMucus ->
            instantaneousProto()
                .setDataType(protoDataType("CervicalMucus"))
                .apply {
                    texture?.let { putValues("texture", enumVal(it)) }
                    amount?.let { putValues("amount", enumVal(it)) }
                }
                .build()
        is CervicalPosition ->
            instantaneousProto()
                .setDataType(protoDataType("CervicalPosition"))
                .apply {
                    position?.let { putValues("position", enumVal(it)) }
                    dilation?.let { putValues("dilation", enumVal(it)) }
                    firmness?.let { putValues("firmness", enumVal(it)) }
                }
                .build()
        is CyclingPedalingCadence ->
            instantaneousProto()
                .setDataType(protoDataType("CyclingPedalingCadence"))
                .apply { putValues("rpm", doubleVal(rpm)) }
                .build()
        is HeartRate ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRate"))
                .apply { putValues("bpm", longVal(bpm)) }
                .build()
        is Height ->
            instantaneousProto()
                .setDataType(protoDataType("Height"))
                .apply { putValues("height", doubleVal(heightMeters)) }
                .build()
        is HipCircumference ->
            instantaneousProto()
                .setDataType(protoDataType("HipCircumference"))
                .apply { putValues("circumference", doubleVal(circumferenceMeters)) }
                .build()
        is HeartRateVariabilityDifferentialIndex ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilityDifferentialIndex"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariability)) }
                .build()
        is HeartRateVariabilityRmssd ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilityRmssd"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariability)) }
                .build()
        is HeartRateVariabilityS ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilityS"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariability)) }
                .build()
        is HeartRateVariabilitySd2 ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilitySd2"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariability)) }
                .build()
        is HeartRateVariabilitySdann ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilitySdann"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariability)) }
                .build()
        is HeartRateVariabilitySdnnIndex ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilitySdnnIndex"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariability)) }
                .build()
        is HeartRateVariabilitySdnn ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilitySdnn"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariability)) }
                .build()
        is HeartRateVariabilitySdsd ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilitySdsd"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariability)) }
                .build()
        is HeartRateVariabilityTinn ->
            instantaneousProto()
                .setDataType(protoDataType("HeartRateVariabilityTinn"))
                .apply { putValues("heartRateVariability", doubleVal(heartRateVariability)) }
                .build()
        is LeanBodyMass ->
            instantaneousProto()
                .setDataType(protoDataType("LeanBodyMass"))
                .apply { putValues("mass", doubleVal(massKg)) }
                .build()
        is Menstruation ->
            instantaneousProto()
                .setDataType(protoDataType("Menstruation"))
                .apply { flow?.let { putValues("flow", enumVal(it)) } }
                .build()
        is OvulationTest ->
            instantaneousProto()
                .setDataType(protoDataType("OvulationTest"))
                .apply { putValues("result", enumVal(result)) }
                .build()
        is OxygenSaturation ->
            instantaneousProto()
                .setDataType(protoDataType("OxygenSaturation"))
                .apply { putValues("percentage", doubleVal(percentage)) }
                .build()
        is Power ->
            instantaneousProto()
                .setDataType(protoDataType("Power"))
                .apply { putValues("power", doubleVal(power)) }
                .build()
        is RespiratoryRate ->
            instantaneousProto()
                .setDataType(protoDataType("RespiratoryRate"))
                .apply { putValues("rate", doubleVal(rate)) }
                .build()
        is RestingHeartRate ->
            instantaneousProto()
                .setDataType(protoDataType("RestingHeartRate"))
                .apply { putValues("bpm", longVal(bpm)) }
                .build()
        is SexualActivity ->
            instantaneousProto()
                .setDataType(protoDataType("SexualActivity"))
                .apply { protectionUsed?.let { putValues("protectionUsed", enumVal(it)) } }
                .build()
        is Speed ->
            instantaneousProto()
                .setDataType(protoDataType("Speed"))
                .apply { putValues("speed", doubleVal(speed)) }
                .build()
        is StepsCadence ->
            instantaneousProto()
                .setDataType(protoDataType("StepsCadence"))
                .apply { putValues("rate", doubleVal(rate)) }
                .build()
        is Vo2Max ->
            instantaneousProto()
                .setDataType(protoDataType("Vo2Max"))
                .apply {
                    putValues("vo2", doubleVal(vo2))
                    measurementMethod?.let { putValues("measurementMethod", enumVal(it)) }
                }
                .build()
        is WaistCircumference ->
            instantaneousProto()
                .setDataType(protoDataType("WaistCircumference"))
                .apply { putValues("circumference", doubleVal(circumferenceMeters)) }
                .build()
        is Weight ->
            instantaneousProto()
                .setDataType(protoDataType("Weight"))
                .apply { putValues("weight", doubleVal(weightKg)) }
                .build()
        is ActiveEnergyBurned ->
            intervalProto()
                .setDataType(protoDataType("ActiveEnergyBurned"))
                .apply { putValues("energy", doubleVal(energy)) }
                .build()
        is ActivityEvent ->
            intervalProto()
                .setDataType(protoDataType("ActivityEvent"))
                .apply { putValues("eventType", enumVal(eventType)) }
                .build()
        is ActivityLap ->
            intervalProto()
                .setDataType(protoDataType("ActivityLap"))
                .apply {
                    if (lengthMeters > 0) {
                        putValues("length", doubleVal(lengthMeters))
                    }
                }
                .build()
        is ActivitySession ->
            intervalProto()
                .setDataType(protoDataType("ActivitySession"))
                .apply {
                    putValues("activityType", enumVal(activityType))
                    title?.let { putValues("title", stringVal(it)) }
                    notes?.let { putValues("notes", stringVal(it)) }
                }
                .build()
        is Distance ->
            intervalProto()
                .setDataType(protoDataType("Distance"))
                .apply { putValues("distance", doubleVal(distanceMeters)) }
                .build()
        is ElevationGained ->
            intervalProto()
                .setDataType(protoDataType("ElevationGained"))
                .apply { putValues("elevation", doubleVal(elevationMeters)) }
                .build()
        is FloorsClimbed ->
            intervalProto()
                .setDataType(protoDataType("FloorsClimbed"))
                .apply { putValues("floors", doubleVal(floors)) }
                .build()
        is Hydration ->
            intervalProto()
                .setDataType(protoDataType("Hydration"))
                .apply { putValues("volume", doubleVal(volume)) }
                .build()
        is Nutrition ->
            intervalProto()
                .setDataType(protoDataType("Nutrition"))
                .apply {
                    if (biotin > 0) {
                        putValues("biotin", doubleVal(biotin))
                    }
                    if (caffeine > 0) {
                        putValues("caffeine", doubleVal(caffeine))
                    }
                    if (calcium > 0) {
                        putValues("calcium", doubleVal(calcium))
                    }
                    if (calories > 0) {
                        putValues("calories", doubleVal(calories))
                    }
                    if (caloriesFromFat > 0) {
                        putValues("caloriesFromFat", doubleVal(caloriesFromFat))
                    }
                    if (chloride > 0) {
                        putValues("chloride", doubleVal(chloride))
                    }
                    if (cholesterol > 0) {
                        putValues("cholesterol", doubleVal(cholesterol))
                    }
                    if (chromium > 0) {
                        putValues("chromium", doubleVal(chromium))
                    }
                    if (copper > 0) {
                        putValues("copper", doubleVal(copper))
                    }
                    if (dietaryFiber > 0) {
                        putValues("dietaryFiber", doubleVal(dietaryFiber))
                    }
                    if (folate > 0) {
                        putValues("folate", doubleVal(folate))
                    }
                    if (folicAcid > 0) {
                        putValues("folicAcid", doubleVal(folicAcid))
                    }
                    if (iodine > 0) {
                        putValues("iodine", doubleVal(iodine))
                    }
                    if (iron > 0) {
                        putValues("iron", doubleVal(iron))
                    }
                    if (magnesium > 0) {
                        putValues("magnesium", doubleVal(magnesium))
                    }
                    if (manganese > 0) {
                        putValues("manganese", doubleVal(manganese))
                    }
                    if (molybdenum > 0) {
                        putValues("molybdenum", doubleVal(molybdenum))
                    }
                    if (monounsaturatedFat > 0) {
                        putValues("monounsaturatedFat", doubleVal(monounsaturatedFat))
                    }
                    if (niacin > 0) {
                        putValues("niacin", doubleVal(niacin))
                    }
                    if (pantothenicAcid > 0) {
                        putValues("pantothenicAcid", doubleVal(pantothenicAcid))
                    }
                    if (phosphorus > 0) {
                        putValues("phosphorus", doubleVal(phosphorus))
                    }
                    if (polyunsaturatedFat > 0) {
                        putValues("polyunsaturatedFat", doubleVal(polyunsaturatedFat))
                    }
                    if (potassium > 0) {
                        putValues("potassium", doubleVal(potassium))
                    }
                    if (protein > 0) {
                        putValues("protein", doubleVal(protein))
                    }
                    if (riboflavin > 0) {
                        putValues("riboflavin", doubleVal(riboflavin))
                    }
                    if (saturatedFat > 0) {
                        putValues("saturatedFat", doubleVal(saturatedFat))
                    }
                    if (selenium > 0) {
                        putValues("selenium", doubleVal(selenium))
                    }
                    if (sodium > 0) {
                        putValues("sodium", doubleVal(sodium))
                    }
                    if (sugar > 0) {
                        putValues("sugar", doubleVal(sugar))
                    }
                    if (thiamin > 0) {
                        putValues("thiamin", doubleVal(thiamin))
                    }
                    if (totalCarbohydrate > 0) {
                        putValues("totalCarbohydrate", doubleVal(totalCarbohydrate))
                    }
                    if (totalFat > 0) {
                        putValues("totalFat", doubleVal(totalFat))
                    }
                    if (transFat > 0) {
                        putValues("transFat", doubleVal(transFat))
                    }
                    if (unsaturatedFat > 0) {
                        putValues("unsaturatedFat", doubleVal(unsaturatedFat))
                    }
                    if (vitaminA > 0) {
                        putValues("vitaminA", doubleVal(vitaminA))
                    }
                    if (vitaminB12 > 0) {
                        putValues("vitaminB12", doubleVal(vitaminB12))
                    }
                    if (vitaminB6 > 0) {
                        putValues("vitaminB6", doubleVal(vitaminB6))
                    }
                    if (vitaminC > 0) {
                        putValues("vitaminC", doubleVal(vitaminC))
                    }
                    if (vitaminD > 0) {
                        putValues("vitaminD", doubleVal(vitaminD))
                    }
                    if (vitaminE > 0) {
                        putValues("vitaminE", doubleVal(vitaminE))
                    }
                    if (vitaminK > 0) {
                        putValues("vitaminK", doubleVal(vitaminK))
                    }
                    if (zinc > 0) {
                        putValues("zinc", doubleVal(zinc))
                    }
                    mealType?.let { putValues("mealType", enumVal(it)) }
                }
                .build()
        is Repetitions ->
            intervalProto()
                .setDataType(protoDataType("Repetitions"))
                .apply { putValues("count", longVal(count)) }
                .build()
        is SleepSession ->
            intervalProto()
                .setDataType(protoDataType("SleepSession"))
                .apply {
                    title?.let { putValues("title", stringVal(it)) }
                    notes?.let { putValues("notes", stringVal(it)) }
                }
                .build()
        is SleepStage ->
            intervalProto()
                .setDataType(protoDataType("SleepStage"))
                .apply { putValues("stage", enumVal(stage)) }
                .build()
        is Steps ->
            intervalProto()
                .setDataType(protoDataType("Steps"))
                .apply { putValues("count", longVal(count)) }
                .build()
        is SwimmingStrokes ->
            intervalProto()
                .setDataType(protoDataType("SwimmingStrokes"))
                .apply {
                    if (count > 0) {
                        putValues("count", longVal(count))
                    }
                    putValues("type", enumVal(type))
                }
                .build()
        is TotalEnergyBurned ->
            intervalProto()
                .setDataType(protoDataType("TotalEnergyBurned"))
                .apply { putValues("energy", doubleVal(energy)) }
                .build()
        is WheelchairPushes ->
            intervalProto()
                .setDataType(protoDataType("WheelchairPushes"))
                .apply { putValues("count", longVal(count)) }
                .build()
        else -> throw RuntimeException("Unsupported yet!")
    }
}
