/*
 * Copyright 2022 The Android Open Source Project
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
@file:RequiresApi(api = 34)
@file:OptIn(ExperimentalFeatureAvailabilityApi::class)

package androidx.health.connect.client.impl.platform.records

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi
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
import androidx.health.connect.client.records.ExerciseCompletionGoal
import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExercisePerformanceTarget
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PlannedExerciseBlock
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseStep
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.isAtLeastSdkExtension13
import java.time.Duration
import kotlin.math.roundToInt
import kotlin.reflect.KClass

// TODO(b/270559291): Validate that all class fields are being converted.

internal fun KClass<out Record>.toPlatformRecordClass(): Class<out PlatformRecord> {
    return toPlatformRecordClassExt13()
        ?: SDK_TO_PLATFORM_RECORD_CLASS[this]
        ?: throw IllegalArgumentException("Unsupported record type $this")
}

@SuppressLint("NewApi") // Guarded by sdk extension check
private fun KClass<out Record>.toPlatformRecordClassExt13(): Class<out PlatformRecord>? {
    if (!isAtLeastSdkExtension13()) {
        return null
    }
    return SDK_TO_PLATFORM_RECORD_CLASS_EXT_13[this]
}

fun Record.toPlatformRecord(): PlatformRecord {
    return toPlatformRecordExt13()
        ?: when (this) {
            is ActiveCaloriesBurnedRecord -> toPlatformActiveCaloriesBurnedRecord()
            is BasalBodyTemperatureRecord -> toPlatformBasalBodyTemperatureRecord()
            is BasalMetabolicRateRecord -> toPlatformBasalMetabolicRateRecord()
            is BloodGlucoseRecord -> toPlatformBloodGlucoseRecord()
            is BloodPressureRecord -> toPlatformBloodPressureRecord()
            is BodyFatRecord -> toPlatformBodyFatRecord()
            is BodyTemperatureRecord -> toPlatformBodyTemperatureRecord()
            is BodyWaterMassRecord -> toPlatformBodyWaterMassRecord()
            is BoneMassRecord -> toPlatformBoneMassRecord()
            is CervicalMucusRecord -> toPlatformCervicalMucusRecord()
            is CyclingPedalingCadenceRecord -> toPlatformCyclingPedalingCadenceRecord()
            is DistanceRecord -> toPlatformDistanceRecord()
            is ElevationGainedRecord -> toPlatformElevationGainedRecord()
            is ExerciseSessionRecord -> toPlatformExerciseSessionRecord()
            is FloorsClimbedRecord -> toPlatformFloorsClimbedRecord()
            is HeartRateRecord -> toPlatformHeartRateRecord()
            is HeartRateVariabilityRmssdRecord -> toPlatformHeartRateVariabilityRmssdRecord()
            is HeightRecord -> toPlatformHeightRecord()
            is HydrationRecord -> toPlatformHydrationRecord()
            is IntermenstrualBleedingRecord -> toPlatformIntermenstrualBleedingRecord()
            is LeanBodyMassRecord -> toPlatformLeanBodyMassRecord()
            is MenstruationFlowRecord -> toPlatformMenstruationFlowRecord()
            is MenstruationPeriodRecord -> toPlatformMenstruationPeriodRecord()
            is NutritionRecord -> toPlatformNutritionRecord()
            is OvulationTestRecord -> toPlatformOvulationTestRecord()
            is OxygenSaturationRecord -> toPlatformOxygenSaturationRecord()
            is PowerRecord -> toPlatformPowerRecord()
            is RespiratoryRateRecord -> toPlatformRespiratoryRateRecord()
            is RestingHeartRateRecord -> toPlatformRestingHeartRateRecord()
            is SexualActivityRecord -> toPlatformSexualActivityRecord()
            is SleepSessionRecord -> toPlatformSleepSessionRecord()
            is SpeedRecord -> toPlatformSpeedRecord()
            is StepsCadenceRecord -> toPlatformStepsCadenceRecord()
            is StepsRecord -> toPlatformStepsRecord()
            is TotalCaloriesBurnedRecord -> toPlatformTotalCaloriesBurnedRecord()
            is Vo2MaxRecord -> toPlatformVo2MaxRecord()
            is WeightRecord -> toPlatformWeightRecord()
            is WheelchairPushesRecord -> toPlatformWheelchairPushesRecord()
            else -> throw IllegalArgumentException("Unsupported record $this")
        }
}

private fun Record.toPlatformRecordExt13(): PlatformRecord? {
    if (!isAtLeastSdkExtension13()) {
        return null
    }
    return when (this) {
        is PlannedExerciseSessionRecord -> toPlatformPlannedExerciseSessionRecord()
        is SkinTemperatureRecord -> toPlatformSkinTemperatureRecord()
        else -> null
    }
}

fun PlatformRecord.toSdkRecord(): Record {
    return toSdkRecordExt13()
        ?: when (this) {
            is PlatformActiveCaloriesBurnedRecord -> toSdkActiveCaloriesBurnedRecord()
            is PlatformBasalBodyTemperatureRecord -> toSdkBasalBodyTemperatureRecord()
            is PlatformBasalMetabolicRateRecord -> toSdkBasalMetabolicRateRecord()
            is PlatformBloodGlucoseRecord -> toSdkBloodGlucoseRecord()
            is PlatformBloodPressureRecord -> toSdkBloodPressureRecord()
            is PlatformBodyFatRecord -> toSdkBodyFatRecord()
            is PlatformBodyTemperatureRecord -> toSdkBodyTemperatureRecord()
            is PlatformBodyWaterMassRecord -> toSdkBodyWaterMassRecord()
            is PlatformBoneMassRecord -> toSdkBoneMassRecord()
            is PlatformCervicalMucusRecord -> toSdkCervicalMucusRecord()
            is PlatformCyclingPedalingCadenceRecord -> toSdkCyclingPedalingCadenceRecord()
            is PlatformDistanceRecord -> toSdkDistanceRecord()
            is PlatformElevationGainedRecord -> toSdkElevationGainedRecord()
            is PlatformExerciseSessionRecord -> toSdkExerciseSessionRecord()
            is PlatformFloorsClimbedRecord -> toSdkFloorsClimbedRecord()
            is PlatformHeartRateRecord -> toSdkHeartRateRecord()
            is PlatformHeartRateVariabilityRmssdRecord -> toSdkHeartRateVariabilityRmssdRecord()
            is PlatformHeightRecord -> toSdkHeightRecord()
            is PlatformHydrationRecord -> toSdkHydrationRecord()
            is PlatformIntermenstrualBleedingRecord -> toSdkIntermenstrualBleedingRecord()
            is PlatformLeanBodyMassRecord -> toSdkLeanBodyMassRecord()
            is PlatformMenstruationFlowRecord -> toSdkMenstruationFlowRecord()
            is PlatformMenstruationPeriodRecord -> toSdkMenstruationPeriodRecord()
            is PlatformNutritionRecord -> toSdkNutritionRecord()
            is PlatformOvulationTestRecord -> toSdkOvulationTestRecord()
            is PlatformOxygenSaturationRecord -> toSdkOxygenSaturationRecord()
            is PlatformPowerRecord -> toSdkPowerRecord()
            is PlatformRespiratoryRateRecord -> toSdkRespiratoryRateRecord()
            is PlatformRestingHeartRateRecord -> toSdkRestingHeartRateRecord()
            is PlatformSexualActivityRecord -> toSdkSexualActivityRecord()
            is PlatformSleepSessionRecord -> toSdkSleepSessionRecord()
            is PlatformSpeedRecord -> toSdkSpeedRecord()
            is PlatformStepsCadenceRecord -> toSdkStepsCadenceRecord()
            is PlatformStepsRecord -> toSdkStepsRecord()
            is PlatformTotalCaloriesBurnedRecord -> toSdkTotalCaloriesBurnedRecord()
            is PlatformVo2MaxRecord -> toSdkVo2MaxRecord()
            is PlatformWeightRecord -> toSdkWeightRecord()
            is PlatformWheelchairPushesRecord -> toWheelchairPushesRecord()
            else -> throw IllegalArgumentException("Unsupported record $this")
        }
}

@SuppressLint("NewApi") // Guarded by sdk extension check
private fun PlatformRecord.toSdkRecordExt13(): Record? {
    if (!isAtLeastSdkExtension13()) {
        return null
    }
    return when (this) {
        is PlatformPlannedExerciseSessionRecord -> toSdkPlannedExerciseSessionRecord()
        is PlatformSkinTemperatureRecord -> toSdkSkinTemperatureRecord()
        else -> null
    }
}

private fun PlatformActiveCaloriesBurnedRecord.toSdkActiveCaloriesBurnedRecord() =
    ActiveCaloriesBurnedRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        energy = energy.toSdkEnergy(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformBasalBodyTemperatureRecord.toSdkBasalBodyTemperatureRecord() =
    BasalBodyTemperatureRecord(
        time = time,
        zoneOffset = zoneOffset,
        temperature = temperature.toSdkTemperature(),
        measurementLocation = measurementLocation,
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformBasalMetabolicRateRecord.toSdkBasalMetabolicRateRecord() =
    BasalMetabolicRateRecord(
        time = time,
        zoneOffset = zoneOffset,
        basalMetabolicRate = basalMetabolicRate.toSdkPower(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformBloodGlucoseRecord.toSdkBloodGlucoseRecord() =
    BloodGlucoseRecord(
        time = time,
        zoneOffset = zoneOffset,
        level = level.toSdkBloodGlucose(),
        specimenSource = specimenSource.toSdkBloodGlucoseSpecimenSource(),
        mealType = mealType.toSdkMealType(),
        relationToMeal = relationToMeal.toSdkRelationToMeal(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformBloodPressureRecord.toSdkBloodPressureRecord() =
    BloodPressureRecord(
        time = time,
        zoneOffset = zoneOffset,
        systolic = systolic.toSdkPressure(),
        diastolic = diastolic.toSdkPressure(),
        bodyPosition = bodyPosition.toSdkBloodPressureBodyPosition(),
        measurementLocation = measurementLocation.toSdkBloodPressureMeasurementLocation(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformBodyFatRecord.toSdkBodyFatRecord() =
    BodyFatRecord(
        time = time,
        zoneOffset = zoneOffset,
        percentage = percentage.toSdkPercentage(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformBodyTemperatureRecord.toSdkBodyTemperatureRecord() =
    BodyTemperatureRecord(
        time = time,
        zoneOffset = zoneOffset,
        temperature = temperature.toSdkTemperature(),
        measurementLocation = measurementLocation.toSdkBodyTemperatureMeasurementLocation(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformBodyWaterMassRecord.toSdkBodyWaterMassRecord() =
    BodyWaterMassRecord(
        time = time,
        zoneOffset = zoneOffset,
        mass = bodyWaterMass.toSdkMass(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformBoneMassRecord.toSdkBoneMassRecord() =
    BoneMassRecord(
        time = time,
        zoneOffset = zoneOffset,
        mass = mass.toSdkMass(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformCervicalMucusRecord.toSdkCervicalMucusRecord() =
    CervicalMucusRecord(
        time = time,
        zoneOffset = zoneOffset,
        appearance = appearance.toSdkCervicalMucusAppearance(),
        sensation = sensation.toSdkCervicalMucusSensation(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformCyclingPedalingCadenceRecord.toSdkCyclingPedalingCadenceRecord() =
    CyclingPedalingCadenceRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        samples = samples.map { it.toSdkCyclingPedalingCadenceSample() }.sortedBy { it.time },
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformDistanceRecord.toSdkDistanceRecord() =
    DistanceRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        distance = distance.toSdkLength(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformElevationGainedRecord.toSdkElevationGainedRecord() =
    ElevationGainedRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        elevation = elevation.toSdkLength(),
        metadata = metadata.toSdkMetadata()
    )

@SuppressLint("NewApi") // Guarded by sdk extension check
private fun PlatformExerciseSessionRecord.toSdkExerciseSessionRecord() =
    ExerciseSessionRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        exerciseType = exerciseType.toSdkExerciseSessionType(),
        title = title?.toString(),
        notes = notes?.toString(),
        laps = laps.map { it.toSdkExerciseLap() }.sortedBy { it.startTime },
        segments = segments.map { it.toSdkExerciseSegment() }.sortedBy { it.startTime },
        metadata = metadata.toSdkMetadata(),
        exerciseRouteResult =
            route?.let { ExerciseRouteResult.Data(it.toSdkExerciseRoute()) }
                ?: if (hasRoute()) ExerciseRouteResult.ConsentRequired()
                else ExerciseRouteResult.NoData(),
        plannedExerciseSessionId =
            if (isAtLeastSdkExtension13()) {
                plannedExerciseSessionId
            } else {
                null
            }
    )

private fun PlatformFloorsClimbedRecord.toSdkFloorsClimbedRecord() =
    FloorsClimbedRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        floors = floors,
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformHeartRateRecord.toSdkHeartRateRecord() =
    HeartRateRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        samples = samples.map { it.toSdkHeartRateSample() }.sortedBy { it.time },
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformHeartRateVariabilityRmssdRecord.toSdkHeartRateVariabilityRmssdRecord() =
    HeartRateVariabilityRmssdRecord(
        time = time,
        zoneOffset = zoneOffset,
        heartRateVariabilityMillis = heartRateVariabilityMillis,
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformHeightRecord.toSdkHeightRecord() =
    HeightRecord(
        time = time,
        zoneOffset = zoneOffset,
        height = height.toSdkLength(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformHydrationRecord.toSdkHydrationRecord() =
    HydrationRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        volume = volume.toSdkVolume(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformIntermenstrualBleedingRecord.toSdkIntermenstrualBleedingRecord() =
    IntermenstrualBleedingRecord(
        time = time,
        zoneOffset = zoneOffset,
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformLeanBodyMassRecord.toSdkLeanBodyMassRecord() =
    LeanBodyMassRecord(
        time = time,
        zoneOffset = zoneOffset,
        mass = mass.toSdkMass(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformMenstruationFlowRecord.toSdkMenstruationFlowRecord() =
    MenstruationFlowRecord(
        time = time,
        zoneOffset = zoneOffset,
        flow = flow.toSdkMenstruationFlow(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformMenstruationPeriodRecord.toSdkMenstruationPeriodRecord() =
    MenstruationPeriodRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformNutritionRecord.toSdkNutritionRecord() =
    NutritionRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        name = mealName,
        mealType = mealType.toSdkMealType(),
        metadata = metadata.toSdkMetadata(),
        biotin = biotin?.toNonDefaultSdkMass(),
        caffeine = caffeine?.toNonDefaultSdkMass(),
        calcium = calcium?.toNonDefaultSdkMass(),
        energy = energy?.toNonDefaultSdkEnergy(),
        energyFromFat = energyFromFat?.toNonDefaultSdkEnergy(),
        chloride = chloride?.toNonDefaultSdkMass(),
        cholesterol = cholesterol?.toNonDefaultSdkMass(),
        chromium = chromium?.toNonDefaultSdkMass(),
        copper = copper?.toNonDefaultSdkMass(),
        dietaryFiber = dietaryFiber?.toNonDefaultSdkMass(),
        folate = folate?.toNonDefaultSdkMass(),
        folicAcid = folicAcid?.toNonDefaultSdkMass(),
        iodine = iodine?.toNonDefaultSdkMass(),
        iron = iron?.toNonDefaultSdkMass(),
        magnesium = magnesium?.toNonDefaultSdkMass(),
        manganese = manganese?.toNonDefaultSdkMass(),
        molybdenum = molybdenum?.toNonDefaultSdkMass(),
        monounsaturatedFat = monounsaturatedFat?.toNonDefaultSdkMass(),
        niacin = niacin?.toNonDefaultSdkMass(),
        pantothenicAcid = pantothenicAcid?.toNonDefaultSdkMass(),
        phosphorus = phosphorus?.toNonDefaultSdkMass(),
        polyunsaturatedFat = polyunsaturatedFat?.toNonDefaultSdkMass(),
        potassium = potassium?.toNonDefaultSdkMass(),
        protein = protein?.toNonDefaultSdkMass(),
        riboflavin = riboflavin?.toNonDefaultSdkMass(),
        saturatedFat = saturatedFat?.toNonDefaultSdkMass(),
        selenium = selenium?.toNonDefaultSdkMass(),
        sodium = sodium?.toNonDefaultSdkMass(),
        sugar = sugar?.toNonDefaultSdkMass(),
        thiamin = thiamin?.toNonDefaultSdkMass(),
        totalCarbohydrate = totalCarbohydrate?.toNonDefaultSdkMass(),
        totalFat = totalFat?.toNonDefaultSdkMass(),
        transFat = transFat?.toNonDefaultSdkMass(),
        unsaturatedFat = unsaturatedFat?.toNonDefaultSdkMass(),
        vitaminA = vitaminA?.toNonDefaultSdkMass(),
        vitaminB12 = vitaminB12?.toNonDefaultSdkMass(),
        vitaminB6 = vitaminB6?.toNonDefaultSdkMass(),
        vitaminC = vitaminC?.toNonDefaultSdkMass(),
        vitaminD = vitaminD?.toNonDefaultSdkMass(),
        vitaminE = vitaminE?.toNonDefaultSdkMass(),
        vitaminK = vitaminK?.toNonDefaultSdkMass(),
        zinc = zinc?.toNonDefaultSdkMass()
    )

private fun PlatformOvulationTestRecord.toSdkOvulationTestRecord() =
    OvulationTestRecord(
        time = time,
        zoneOffset = zoneOffset,
        result = result.toSdkOvulationTestResult(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformOxygenSaturationRecord.toSdkOxygenSaturationRecord() =
    OxygenSaturationRecord(
        time = time,
        zoneOffset = zoneOffset,
        percentage = percentage.toSdkPercentage(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformPowerRecord.toSdkPowerRecord() =
    PowerRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        samples = samples.map { it.toSdkPowerRecordSample() }.sortedBy { it.time },
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformRespiratoryRateRecord.toSdkRespiratoryRateRecord() =
    RespiratoryRateRecord(
        time = time,
        zoneOffset = zoneOffset,
        rate = rate,
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformRestingHeartRateRecord.toSdkRestingHeartRateRecord() =
    RestingHeartRateRecord(
        time = time,
        zoneOffset = zoneOffset,
        beatsPerMinute = beatsPerMinute,
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformSexualActivityRecord.toSdkSexualActivityRecord() =
    SexualActivityRecord(
        time = time,
        zoneOffset = zoneOffset,
        protectionUsed = protectionUsed.toSdkProtectionUsed(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformSleepSessionRecord.toSdkSleepSessionRecord() =
    SleepSessionRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        metadata = metadata.toSdkMetadata(),
        title = title?.toString(),
        notes = notes?.toString(),
        stages = stages.map { it.toSdkSleepSessionStage() }.sortedBy { it.startTime },
    )

@SuppressLint("NewApi") // Guarded by sdk extension check
@RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 13)
private fun PlatformSkinTemperatureRecord.toSdkSkinTemperatureRecord() =
    SkinTemperatureRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        metadata = metadata.toSdkMetadata(),
        measurementLocation = measurementLocation.toSdkSkinTemperatureMeasurementLocation(),
        deltas = deltas.map { it.toSdkSkinTemperatureDelta() },
        baseline = baseline?.toSdkTemperature()
    )

private fun PlatformSpeedRecord.toSdkSpeedRecord() =
    SpeedRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        samples = samples.map { it.toSdkSpeedSample() }.sortedBy { it.time },
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformStepsCadenceRecord.toSdkStepsCadenceRecord() =
    StepsCadenceRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        samples = samples.map { it.toSdkStepsCadenceSample() }.sortedBy { it.time },
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformStepsRecord.toSdkStepsRecord() =
    StepsRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        count = count,
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformTotalCaloriesBurnedRecord.toSdkTotalCaloriesBurnedRecord() =
    TotalCaloriesBurnedRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        energy = energy.toSdkEnergy(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformVo2MaxRecord.toSdkVo2MaxRecord() =
    Vo2MaxRecord(
        time = time,
        zoneOffset = zoneOffset,
        vo2MillilitersPerMinuteKilogram = vo2MillilitersPerMinuteKilogram,
        measurementMethod = measurementMethod.toSdkVo2MaxMeasurementMethod(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformWeightRecord.toSdkWeightRecord() =
    WeightRecord(
        time = time,
        zoneOffset = zoneOffset,
        weight = weight.toSdkMass(),
        metadata = metadata.toSdkMetadata()
    )

private fun PlatformWheelchairPushesRecord.toWheelchairPushesRecord() =
    WheelchairPushesRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        count = count,
        metadata = metadata.toSdkMetadata()
    )

private fun ActiveCaloriesBurnedRecord.toPlatformActiveCaloriesBurnedRecord() =
    PlatformActiveCaloriesBurnedRecordBuilder(
            metadata.toPlatformMetadata(),
            startTime,
            endTime,
            energy.toPlatformEnergy(),
        )
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

private fun BasalBodyTemperatureRecord.toPlatformBasalBodyTemperatureRecord() =
    PlatformBasalBodyTemperatureRecordBuilder(
            metadata.toPlatformMetadata(),
            time,
            measurementLocation.toPlatformBodyTemperatureMeasurementLocation(),
            temperature.toPlatformTemperature()
        )
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun BasalMetabolicRateRecord.toPlatformBasalMetabolicRateRecord() =
    PlatformBasalMetabolicRateRecordBuilder(
            metadata.toPlatformMetadata(),
            time,
            basalMetabolicRate.toPlatformPower()
        )
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun BloodGlucoseRecord.toPlatformBloodGlucoseRecord() =
    PlatformBloodGlucoseRecordBuilder(
            metadata.toPlatformMetadata(),
            time,
            specimenSource.toPlatformBloodGlucoseSpecimenSource(),
            level.toPlatformBloodGlucose(),
            relationToMeal.toPlatformBloodGlucoseRelationToMeal(),
            mealType.toPlatformMealType()
        )
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun BloodPressureRecord.toPlatformBloodPressureRecord() =
    PlatformBloodPressureRecordBuilder(
            metadata.toPlatformMetadata(),
            time,
            measurementLocation.toPlatformBloodPressureMeasurementLocation(),
            systolic.toPlatformPressure(),
            diastolic.toPlatformPressure(),
            bodyPosition.toPlatformBloodPressureBodyPosition()
        )
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun BodyFatRecord.toPlatformBodyFatRecord() =
    PlatformBodyFatRecordBuilder(
            metadata.toPlatformMetadata(),
            time,
            percentage.toPlatformPercentage()
        )
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun BodyTemperatureRecord.toPlatformBodyTemperatureRecord() =
    PlatformBodyTemperatureRecordBuilder(
            metadata.toPlatformMetadata(),
            time,
            measurementLocation.toPlatformBodyTemperatureMeasurementLocation(),
            temperature.toPlatformTemperature()
        )
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun BodyWaterMassRecord.toPlatformBodyWaterMassRecord() =
    PlatformBodyWaterMassRecordBuilder(metadata.toPlatformMetadata(), time, mass.toPlatformMass())
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun BoneMassRecord.toPlatformBoneMassRecord() =
    PlatformBoneMassRecordBuilder(metadata.toPlatformMetadata(), time, mass.toPlatformMass())
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun CervicalMucusRecord.toPlatformCervicalMucusRecord() =
    PlatformCervicalMucusRecordBuilder(
            metadata.toPlatformMetadata(),
            time,
            sensation.toPlatformCervicalMucusSensation(),
            appearance.toPlatformCervicalMucusAppearance(),
        )
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun CyclingPedalingCadenceRecord.toPlatformCyclingPedalingCadenceRecord() =
    PlatformCyclingPedalingCadenceRecordBuilder(
            metadata.toPlatformMetadata(),
            startTime,
            endTime,
            samples.map { it.toPlatformCyclingPedalingCadenceSample() }
        )
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

private fun CyclingPedalingCadenceRecord.Sample.toPlatformCyclingPedalingCadenceSample() =
    PlatformCyclingPedalingCadenceSample(revolutionsPerMinute, time)

private fun DistanceRecord.toPlatformDistanceRecord() =
    PlatformDistanceRecordBuilder(
            metadata.toPlatformMetadata(),
            startTime,
            endTime,
            distance.toPlatformLength()
        )
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

private fun ElevationGainedRecord.toPlatformElevationGainedRecord() =
    PlatformElevationGainedRecordBuilder(
            metadata.toPlatformMetadata(),
            startTime,
            endTime,
            elevation.toPlatformLength()
        )
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

@SuppressLint("NewApi") // Guarded by sdk extension check
private fun ExerciseSessionRecord.toPlatformExerciseSessionRecord() =
    PlatformExerciseSessionRecordBuilder(
            metadata.toPlatformMetadata(),
            startTime,
            endTime,
            exerciseType.toPlatformExerciseSessionType()
        )
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
            notes?.let { setNotes(it) }
            title?.let { setTitle(it) }
            setLaps(laps.map { it.toPlatformExerciseLap() })
            setSegments(segments.map { it.toPlatformExerciseSegment() })
            if (exerciseRouteResult is ExerciseRouteResult.Data) {
                setRoute(exerciseRouteResult.exerciseRoute.toPlatformExerciseRoute())
            }
            plannedExerciseSessionId?.let { setPlannedExerciseSessionId(it) }
        }
        .build()

private fun ExerciseLap.toPlatformExerciseLap() =
    PlatformExerciseLapBuilder(startTime, endTime)
        .apply { length?.let { setLength(it.toPlatformLength()) } }
        .build()

private fun ExerciseRoute.toPlatformExerciseRoute() =
    PlatformExerciseRoute(
        route.map { location ->
            PlatformExerciseRouteLocationBuilder(
                    location.time,
                    location.latitude,
                    location.longitude
                )
                .apply {
                    location.horizontalAccuracy?.let {
                        setHorizontalAccuracy(it.toPlatformLength())
                    }
                    location.verticalAccuracy?.let { setVerticalAccuracy(it.toPlatformLength()) }
                    location.altitude?.let { setAltitude(it.toPlatformLength()) }
                }
                .build()
        }
    )

private fun ExerciseSegment.toPlatformExerciseSegment() =
    PlatformExerciseSegmentBuilder(startTime, endTime, segmentType.toPlatformExerciseSegmentType())
        .setRepetitionsCount(repetitions)
        .build()

private fun FloorsClimbedRecord.toPlatformFloorsClimbedRecord() =
    PlatformFloorsClimbedRecordBuilder(metadata.toPlatformMetadata(), startTime, endTime, floors)
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

private fun HeartRateRecord.toPlatformHeartRateRecord() =
    PlatformHeartRateRecordBuilder(
            metadata.toPlatformMetadata(),
            startTime,
            endTime,
            samples.map { it.toPlatformHeartRateSample() }
        )
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

private fun HeartRateRecord.Sample.toPlatformHeartRateSample() =
    PlatformHeartRateSample(beatsPerMinute, time)

private fun HeartRateVariabilityRmssdRecord.toPlatformHeartRateVariabilityRmssdRecord() =
    PlatformHeartRateVariabilityRmssdRecordBuilder(
            metadata.toPlatformMetadata(),
            time,
            heartRateVariabilityMillis
        )
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun HeightRecord.toPlatformHeightRecord() =
    PlatformHeightRecordBuilder(metadata.toPlatformMetadata(), time, height.toPlatformLength())
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun HydrationRecord.toPlatformHydrationRecord() =
    PlatformHydrationRecordBuilder(
            metadata.toPlatformMetadata(),
            startTime,
            endTime,
            volume.toPlatformVolume()
        )
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

private fun IntermenstrualBleedingRecord.toPlatformIntermenstrualBleedingRecord() =
    PlatformIntermenstrualBleedingRecordBuilder(metadata.toPlatformMetadata(), time)
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun LeanBodyMassRecord.toPlatformLeanBodyMassRecord() =
    PlatformLeanBodyMassRecordBuilder(metadata.toPlatformMetadata(), time, mass.toPlatformMass())
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun MenstruationFlowRecord.toPlatformMenstruationFlowRecord() =
    PlatformMenstruationFlowRecordBuilder(
            metadata.toPlatformMetadata(),
            time,
            flow.toPlatformMenstruationFlow()
        )
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun MenstruationPeriodRecord.toPlatformMenstruationPeriodRecord() =
    PlatformMenstruationPeriodRecordBuilder(metadata.toPlatformMetadata(), startTime, endTime)
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

private fun NutritionRecord.toPlatformNutritionRecord() =
    PlatformNutritionRecordBuilder(metadata.toPlatformMetadata(), startTime, endTime)
        .setMealType(mealType.toPlatformMealType())
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
            biotin?.let { setBiotin(it.toPlatformMass()) }
            caffeine?.let { setCaffeine(it.toPlatformMass()) }
            calcium?.let { setCalcium(it.toPlatformMass()) }
            chloride?.let { setChloride(it.toPlatformMass()) }
            cholesterol?.let { setCholesterol(it.toPlatformMass()) }
            chromium?.let { setChromium(it.toPlatformMass()) }
            copper?.let { setCopper(it.toPlatformMass()) }
            dietaryFiber?.let { setDietaryFiber(it.toPlatformMass()) }
            energy?.let { setEnergy(it.toPlatformEnergy()) }
            energyFromFat?.let { setEnergyFromFat(it.toPlatformEnergy()) }
            folate?.let { setFolate(it.toPlatformMass()) }
            folicAcid?.let { setFolicAcid(it.toPlatformMass()) }
            iodine?.let { setIodine(it.toPlatformMass()) }
            iron?.let { setIron(it.toPlatformMass()) }
            magnesium?.let { setMagnesium(it.toPlatformMass()) }
            manganese?.let { setManganese(it.toPlatformMass()) }
            molybdenum?.let { setMolybdenum(it.toPlatformMass()) }
            monounsaturatedFat?.let { setMonounsaturatedFat(it.toPlatformMass()) }
            name?.let { setMealName(it) }
            niacin?.let { setNiacin(it.toPlatformMass()) }
            pantothenicAcid?.let { setPantothenicAcid(it.toPlatformMass()) }
            phosphorus?.let { setPhosphorus(it.toPlatformMass()) }
            polyunsaturatedFat?.let { setPolyunsaturatedFat(it.toPlatformMass()) }
            potassium?.let { setPotassium(it.toPlatformMass()) }
            protein?.let { setProtein(it.toPlatformMass()) }
            riboflavin?.let { setRiboflavin(it.toPlatformMass()) }
            saturatedFat?.let { setSaturatedFat(it.toPlatformMass()) }
            selenium?.let { setSelenium(it.toPlatformMass()) }
            sodium?.let { setSodium(it.toPlatformMass()) }
            sugar?.let { setSugar(it.toPlatformMass()) }
            thiamin?.let { setThiamin(it.toPlatformMass()) }
            totalCarbohydrate?.let { setTotalCarbohydrate(it.toPlatformMass()) }
            totalFat?.let { setTotalFat(it.toPlatformMass()) }
            transFat?.let { setTransFat(it.toPlatformMass()) }
            unsaturatedFat?.let { setUnsaturatedFat(it.toPlatformMass()) }
            vitaminA?.let { setVitaminA(it.toPlatformMass()) }
            vitaminB6?.let { setVitaminB6(it.toPlatformMass()) }
            vitaminB12?.let { setVitaminB12(it.toPlatformMass()) }
            vitaminC?.let { setVitaminC(it.toPlatformMass()) }
            vitaminD?.let { setVitaminD(it.toPlatformMass()) }
            vitaminE?.let { setVitaminE(it.toPlatformMass()) }
            vitaminK?.let { setVitaminK(it.toPlatformMass()) }
            zinc?.let { setZinc(it.toPlatformMass()) }
        }
        .build()

private fun OvulationTestRecord.toPlatformOvulationTestRecord() =
    PlatformOvulationTestRecordBuilder(
            metadata.toPlatformMetadata(),
            time,
            result.toPlatformOvulationTestResult()
        )
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun OxygenSaturationRecord.toPlatformOxygenSaturationRecord() =
    PlatformOxygenSaturationRecordBuilder(
            metadata.toPlatformMetadata(),
            time,
            percentage.toPlatformPercentage()
        )
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

@SuppressLint("NewApi") // Guarded by sdk extension check
@RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 13)
private fun PlannedExerciseSessionRecord.toPlatformPlannedExerciseSessionRecord() =
    if (hasExplicitTime) {
            PlatformPlannedExerciseSessionRecordBuilder(
                metadata.toPlatformMetadata(),
                exerciseType.toPlatformExerciseSessionType(),
                startTime,
                endTime
            )
        } else {
            PlatformPlannedExerciseSessionRecordBuilder(
                metadata.toPlatformMetadata(),
                exerciseType.toPlatformExerciseSessionType(),
                startTime.atZone(startZoneOffset).toLocalDate(),
                Duration.between(startTime, endTime)
            )
        }
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
            title?.let { setTitle(it) }
            notes?.let { setNotes(it) }
            setBlocks(blocks.map { it.toPlatformPlannedExerciseBlock() })
        }
        .build()

@SuppressLint("NewApi")
@RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 13)
private fun PlannedExerciseBlock.toPlatformPlannedExerciseBlock() =
    PlatformPlannedExerciseBlockBuilder(
            repetitions,
        )
        .apply { setSteps(steps.map { it.toPlatformPlannedExerciseStep() }) }
        .build()

@SuppressLint("NewApi")
private fun PlannedExerciseStep.toPlatformPlannedExerciseStep() =
    PlatformPlannedExerciseStepBuilder(
            exerciseType.toPlatformExerciseSegmentType(),
            exercisePhase.toPlatformExerciseCategory(),
            completionGoal.toPlatformExerciseCompletionGoal()
        )
        .apply {
            setPerformanceGoals(performanceTargets.map { it.toPlatformExercisePerformanceTarget() })
        }
        .build()

@SuppressLint("NewApi")
internal fun ExerciseCompletionGoal.toPlatformExerciseCompletionGoal() =
    when (this) {
        is ExerciseCompletionGoal.DistanceGoal -> PlatformDistanceGoal(distance.toPlatformLength())
        is ExerciseCompletionGoal.DistanceAndDurationGoal ->
            PlatformDistanceAndDurationGoal(distance.toPlatformLength(), duration)
        is ExerciseCompletionGoal.StepsGoal -> PlatformStepsGoal(steps)
        is ExerciseCompletionGoal.DurationGoal -> PlatformDurationGoal(duration)
        is ExerciseCompletionGoal.RepetitionsGoal -> PlatformRepetitionsGoal(repetitions)
        is ExerciseCompletionGoal.TotalCaloriesBurnedGoal ->
            PlatformTotalCaloriesBurnedGoal(totalCalories.toPlatformEnergy())
        is ExerciseCompletionGoal.ActiveCaloriesBurnedGoal ->
            PlatformActiveCaloriesBurnedGoal(activeCalories.toPlatformEnergy())
        is ExerciseCompletionGoal.UnknownGoal -> PlatformUnknownCompletionGoal.INSTANCE
        is ExerciseCompletionGoal.ManualCompletion -> PlatformManualCompletion.INSTANCE
        else -> throw IllegalArgumentException("Unsupported exercise completion goal $this")
    }

@SuppressLint("NewApi")
internal fun ExercisePerformanceTarget.toPlatformExercisePerformanceTarget() =
    when (this) {
        is ExercisePerformanceTarget.PowerTarget ->
            PlatformPowerTarget(minPower.toPlatformPower(), maxPower.toPlatformPower())
        is ExercisePerformanceTarget.SpeedTarget ->
            PlatformSpeedTarget(minSpeed.toPlatformVelocity(), maxSpeed.toPlatformVelocity())
        is ExercisePerformanceTarget.CadenceTarget -> PlatformCadenceTarget(minCadence, maxCadence)
        is ExercisePerformanceTarget.HeartRateTarget ->
            PlatformHeartRateTarget(minHeartRate.roundToInt(), maxHeartRate.roundToInt())
        is ExercisePerformanceTarget.WeightTarget -> PlatformWeightTarget(mass.toPlatformMass())
        is ExercisePerformanceTarget.RateOfPerceivedExertionTarget ->
            PlatformRateOfPerceivedExertionTarget(rpe)
        is ExercisePerformanceTarget.AmrapTarget -> PlatformAmrapTarget.INSTANCE
        is ExercisePerformanceTarget.UnknownTarget -> PlatformUnknownPerformanceTarget.INSTANCE
        else -> throw IllegalArgumentException("Unsupported exercise performance target $this")
    }

@SuppressLint("NewApi")
@RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 13)
internal fun PlatformPlannedExerciseSessionRecord.toSdkPlannedExerciseSessionRecord() =
    PlannedExerciseSessionRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        metadata = metadata.toSdkMetadata(),
        hasExplicitTime = hasExplicitTime(),
        exerciseType = exerciseType.toSdkExerciseSessionType(),
        completedExerciseSessionId = completedExerciseSessionId,
        blocks = blocks.map { it.toSdkPlannedExerciseBlock() },
        title = title?.toString(),
        notes = notes?.toString(),
    )

@SuppressLint("NewApi")
@RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 13)
private fun PlatformPlannedExerciseBlock.toSdkPlannedExerciseBlock() =
    PlannedExerciseBlock(
        repetitions = repetitions,
        description = description?.toString(),
        steps = steps.map { it.toSdkPlannedExerciseStep() },
    )

@SuppressLint("NewApi")
private fun PlatformPlannedExerciseStep.toSdkPlannedExerciseStep() =
    PlannedExerciseStep(
        exerciseType = exerciseType.toSdkExerciseSegmentType(),
        exercisePhase = exerciseCategory.toSdkExerciseCategory(),
        completionGoal = completionGoal.toSdkExerciseCompletionGoal(),
        performanceTargets = performanceGoals.map { it.toSdkExercisePerformanceTarget() }
    )

@SuppressLint("NewApi")
internal fun PlatformExerciseCompletionGoal.toSdkExerciseCompletionGoal() =
    when (this) {
        is PlatformDistanceGoal -> ExerciseCompletionGoal.DistanceGoal(distance.toSdkLength())
        is PlatformDistanceAndDurationGoal ->
            ExerciseCompletionGoal.DistanceAndDurationGoal(distance.toSdkLength(), duration)
        is PlatformStepsGoal -> ExerciseCompletionGoal.StepsGoal(steps)
        is PlatformDurationGoal -> ExerciseCompletionGoal.DurationGoal(duration)
        is PlatformRepetitionsGoal -> ExerciseCompletionGoal.RepetitionsGoal(repetitions)
        is PlatformTotalCaloriesBurnedGoal ->
            ExerciseCompletionGoal.TotalCaloriesBurnedGoal(totalCalories.toSdkEnergy())
        is PlatformActiveCaloriesBurnedGoal ->
            ExerciseCompletionGoal.ActiveCaloriesBurnedGoal(activeCalories.toSdkEnergy())
        is PlatformUnknownCompletionGoal -> ExerciseCompletionGoal.UnknownGoal
        is PlatformManualCompletion -> ExerciseCompletionGoal.ManualCompletion
        else -> throw IllegalArgumentException("Unsupported exercise completion goal $this")
    }

@SuppressLint("NewApi")
internal fun PlatformExercisePerformanceTarget.toSdkExercisePerformanceTarget() =
    when (this) {
        is PlatformPowerTarget ->
            ExercisePerformanceTarget.PowerTarget(minPower.toSdkPower(), maxPower.toSdkPower())
        is PlatformSpeedTarget ->
            ExercisePerformanceTarget.SpeedTarget(
                minSpeed.toSdkVelocity(),
                maxSpeed.toSdkVelocity()
            )
        is PlatformCadenceTarget -> ExercisePerformanceTarget.CadenceTarget(minRpm, maxRpm)
        is PlatformHeartRateTarget ->
            ExercisePerformanceTarget.HeartRateTarget(minBpm.toDouble(), maxBpm.toDouble())
        is PlatformWeightTarget -> ExercisePerformanceTarget.WeightTarget(mass.toSdkMass())
        is PlatformRateOfPerceivedExertionTarget ->
            ExercisePerformanceTarget.RateOfPerceivedExertionTarget(rpe)
        is PlatformAmrapTarget -> ExercisePerformanceTarget.AmrapTarget
        is PlatformUnknownPerformanceTarget -> ExercisePerformanceTarget.UnknownTarget
        else -> throw IllegalArgumentException("Unsupported exercise performance target $this")
    }

private fun PowerRecord.toPlatformPowerRecord() =
    PlatformPowerRecordBuilder(
            metadata.toPlatformMetadata(),
            startTime,
            endTime,
            samples.map { it.toPlatformPowerRecordSample() }
        )
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

private fun PowerRecord.Sample.toPlatformPowerRecordSample() =
    PlatformPowerRecordSample(power.toPlatformPower(), time)

private fun RespiratoryRateRecord.toPlatformRespiratoryRateRecord() =
    PlatformRespiratoryRateRecordBuilder(metadata.toPlatformMetadata(), time, rate)
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun RestingHeartRateRecord.toPlatformRestingHeartRateRecord() =
    PlatformRestingHeartRateRecordBuilder(metadata.toPlatformMetadata(), time, beatsPerMinute)
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun SexualActivityRecord.toPlatformSexualActivityRecord() =
    PlatformSexualActivityRecordBuilder(
            metadata.toPlatformMetadata(),
            time,
            protectionUsed.toPlatformSexualActivityProtectionUsed()
        )
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

@SuppressLint("NewApi") // Guarded by sdk extension check
@RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 13)
private fun SkinTemperatureRecord.toPlatformSkinTemperatureRecord() =
    PlatformSkinTemperatureRecordBuilder(metadata.toPlatformMetadata(), startTime, endTime)
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
            baseline?.let { setBaseline(it.toPlatformTemperature()) }
            setMeasurementLocation(
                measurementLocation.toPlatformSkinTemperatureMeasurementLocation()
            )
            setDeltas(deltas.map { it.toPlatformSkinTemperatureRecordDelta() })
        }
        .build()

@SuppressLint("NewApi") // Guarded by sdk extension check
@RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 13)
private fun SkinTemperatureRecord.Delta.toPlatformSkinTemperatureRecordDelta() =
    PlatformSkinTemperatureDelta(delta.toPlatformTemperatureDelta(), time)

private fun SleepSessionRecord.toPlatformSleepSessionRecord() =
    PlatformSleepSessionRecordBuilder(metadata.toPlatformMetadata(), startTime, endTime)
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
            notes?.let { setNotes(it) }
            title?.let { setTitle(it) }
            setStages(stages.map { it.toPlatformSleepSessionStage() })
        }
        .build()

private fun SleepSessionRecord.Stage.toPlatformSleepSessionStage() =
    PlatformSleepSessionStage(startTime, endTime, stage.toPlatformSleepStageType())

private fun SpeedRecord.toPlatformSpeedRecord() =
    PlatformSpeedRecordBuilder(
            metadata.toPlatformMetadata(),
            startTime,
            endTime,
            samples.map { it.toPlatformSpeedRecordSample() }
        )
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

private fun SpeedRecord.Sample.toPlatformSpeedRecordSample() =
    PlatformSpeedSample(speed.toPlatformVelocity(), time)

private fun StepsRecord.toPlatformStepsRecord() =
    PlatformStepsRecordBuilder(metadata.toPlatformMetadata(), startTime, endTime, count)
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

private fun StepsCadenceRecord.toPlatformStepsCadenceRecord() =
    PlatformStepsCadenceRecordBuilder(
            metadata.toPlatformMetadata(),
            startTime,
            endTime,
            samples.map { it.toPlatformStepsCadenceSample() }
        )
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

private fun StepsCadenceRecord.Sample.toPlatformStepsCadenceSample() =
    PlatformStepsCadenceSample(rate, time)

private fun TotalCaloriesBurnedRecord.toPlatformTotalCaloriesBurnedRecord() =
    PlatformTotalCaloriesBurnedRecordBuilder(
            metadata.toPlatformMetadata(),
            startTime,
            endTime,
            energy.toPlatformEnergy()
        )
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

private fun Vo2MaxRecord.toPlatformVo2MaxRecord() =
    PlatformVo2MaxRecordBuilder(
            metadata.toPlatformMetadata(),
            time,
            measurementMethod.toPlatformVo2MaxMeasurementMethod(),
            vo2MillilitersPerMinuteKilogram
        )
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun WeightRecord.toPlatformWeightRecord() =
    PlatformWeightRecordBuilder(metadata.toPlatformMetadata(), time, weight.toPlatformMass())
        .apply { zoneOffset?.let { setZoneOffset(it) } }
        .build()

private fun WheelchairPushesRecord.toPlatformWheelchairPushesRecord() =
    PlatformWheelchairPushesRecordBuilder(metadata.toPlatformMetadata(), startTime, endTime, count)
        .apply {
            startZoneOffset?.let { setStartZoneOffset(it) }
            endZoneOffset?.let { setEndZoneOffset(it) }
        }
        .build()

private fun PlatformCyclingPedalingCadenceSample.toSdkCyclingPedalingCadenceSample() =
    CyclingPedalingCadenceRecord.Sample(time, revolutionsPerMinute)

private fun PlatformHeartRateSample.toSdkHeartRateSample() =
    HeartRateRecord.Sample(time, beatsPerMinute)

private fun PlatformPowerRecordSample.toSdkPowerRecordSample() =
    PowerRecord.Sample(time, power.toSdkPower())

@SuppressLint("NewApi") // Guarded by sdk extension check
@RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 13)
private fun PlatformSkinTemperatureDelta.toSdkSkinTemperatureDelta() =
    SkinTemperatureRecord.Delta(time, delta.toSdkTemperatureDelta())

private fun PlatformSpeedSample.toSdkSpeedSample() = SpeedRecord.Sample(time, speed.toSdkVelocity())

private fun PlatformStepsCadenceSample.toSdkStepsCadenceSample() =
    StepsCadenceRecord.Sample(time, rate)

private fun PlatformSleepSessionStage.toSdkSleepSessionStage() =
    SleepSessionRecord.Stage(startTime, endTime, type.toSdkSleepStageType())

internal fun PlatformExerciseRoute.toSdkExerciseRoute() =
    ExerciseRoute(
        routeLocations.map { value ->
            ExerciseRoute.Location(
                time = value.time,
                latitude = value.latitude,
                longitude = value.longitude,
                horizontalAccuracy = value.horizontalAccuracy?.toSdkLength(),
                verticalAccuracy = value.verticalAccuracy?.toSdkLength(),
                altitude = value.altitude?.toSdkLength()
            )
        }
    )

internal fun PlatformExerciseLap.toSdkExerciseLap() =
    ExerciseLap(startTime, endTime, length?.toSdkLength())

internal fun PlatformExerciseSegment.toSdkExerciseSegment() =
    ExerciseSegment(startTime, endTime, segmentType.toSdkExerciseSegmentType(), repetitionsCount)
