/*
 * Copyright 2023 The Android Open Source Project
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
@file:RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

package androidx.health.connect.client.impl.platform.records

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

internal typealias PlatformInstantRecord = android.health.connect.datatypes.InstantRecord

internal typealias PlatformIntervalRecord = android.health.connect.datatypes.IntervalRecord

internal typealias PlatformRecord = android.health.connect.datatypes.Record

internal typealias PlatformActiveCaloriesBurnedRecord =
    android.health.connect.datatypes.ActiveCaloriesBurnedRecord

internal typealias PlatformActiveCaloriesBurnedRecordBuilder =
    android.health.connect.datatypes.ActiveCaloriesBurnedRecord.Builder

internal typealias PlatformBasalBodyTemperatureRecord =
    android.health.connect.datatypes.BasalBodyTemperatureRecord

internal typealias PlatformBasalBodyTemperatureRecordBuilder =
    android.health.connect.datatypes.BasalBodyTemperatureRecord.Builder

internal typealias PlatformBodyTemperatureMeasurementLocation =
    android.health.connect.datatypes.BodyTemperatureMeasurementLocation

internal typealias PlatformBasalMetabolicRateRecord =
    android.health.connect.datatypes.BasalMetabolicRateRecord

internal typealias PlatformBasalMetabolicRateRecordBuilder =
    android.health.connect.datatypes.BasalMetabolicRateRecord.Builder

internal typealias PlatformBloodGlucoseRecord = android.health.connect.datatypes.BloodGlucoseRecord

internal typealias PlatformBloodGlucoseRecordBuilder =
    android.health.connect.datatypes.BloodGlucoseRecord.Builder

internal typealias PlatformBloodGlucoseSpecimenSource =
    android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource

internal typealias PlatformBloodGlucoseRelationToMealType =
    android.health.connect.datatypes.BloodGlucoseRecord.RelationToMealType

internal typealias PlatformBloodPressureRecord =
    android.health.connect.datatypes.BloodPressureRecord

internal typealias PlatformBloodPressureRecordBuilder =
    android.health.connect.datatypes.BloodPressureRecord.Builder

internal typealias PlatformBloodGlucoseRelationToMeal =
    android.health.connect.datatypes.BloodGlucoseRecord.RelationToMealType

internal typealias PlatformBloodPressureBodyPosition =
    android.health.connect.datatypes.BloodPressureRecord.BodyPosition

internal typealias PlatformBloodPressureMeasurementLocation =
    android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation

internal typealias PlatformBodyFatRecord = android.health.connect.datatypes.BodyFatRecord

internal typealias PlatformBodyFatRecordBuilder =
    android.health.connect.datatypes.BodyFatRecord.Builder

internal typealias PlatformBodyTemperatureRecord =
    android.health.connect.datatypes.BodyTemperatureRecord

internal typealias PlatformBodyTemperatureRecordBuilder =
    android.health.connect.datatypes.BodyTemperatureRecord.Builder

internal typealias PlatformBodyWaterMassRecord =
    android.health.connect.datatypes.BodyWaterMassRecord

internal typealias PlatformBodyWaterMassRecordBuilder =
    android.health.connect.datatypes.BodyWaterMassRecord.Builder

internal typealias PlatformBoneMassRecord = android.health.connect.datatypes.BoneMassRecord

internal typealias PlatformBoneMassRecordBuilder =
    android.health.connect.datatypes.BoneMassRecord.Builder

internal typealias PlatformCervicalMucusRecord =
    android.health.connect.datatypes.CervicalMucusRecord

internal typealias PlatformCervicalMucusRecordBuilder =
    android.health.connect.datatypes.CervicalMucusRecord.Builder

internal typealias PlatformCervicalMucusAppearance =
    android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusAppearance

internal typealias PlatformCervicalMucusSensation =
    android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusSensation

internal typealias PlatformCyclingPedalingCadenceRecord =
    android.health.connect.datatypes.CyclingPedalingCadenceRecord

internal typealias PlatformCyclingPedalingCadenceRecordBuilder =
    android.health.connect.datatypes.CyclingPedalingCadenceRecord.Builder

internal typealias PlatformCyclingPedalingCadenceSample =
    android.health.connect.datatypes.CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample

internal typealias PlatformDistanceRecord = android.health.connect.datatypes.DistanceRecord

internal typealias PlatformDistanceRecordBuilder =
    android.health.connect.datatypes.DistanceRecord.Builder

internal typealias PlatformElevationGainedRecord =
    android.health.connect.datatypes.ElevationGainedRecord

internal typealias PlatformElevationGainedRecordBuilder =
    android.health.connect.datatypes.ElevationGainedRecord.Builder

internal typealias PlatformExerciseLap = android.health.connect.datatypes.ExerciseLap

internal typealias PlatformExerciseLapBuilder = android.health.connect.datatypes.ExerciseLap.Builder

internal typealias PlatformExerciseSegment = android.health.connect.datatypes.ExerciseSegment

internal typealias PlatformExerciseSegmentBuilder =
    android.health.connect.datatypes.ExerciseSegment.Builder

internal typealias PlatformExerciseSegmentType =
    android.health.connect.datatypes.ExerciseSegmentType

internal typealias PlatformExerciseSessionRecord =
    android.health.connect.datatypes.ExerciseSessionRecord

internal typealias PlatformExerciseSessionRecordBuilder =
    android.health.connect.datatypes.ExerciseSessionRecord.Builder

internal typealias PlatformExerciseSessionType =
    android.health.connect.datatypes.ExerciseSessionType

internal typealias PlatformExerciseRoute = android.health.connect.datatypes.ExerciseRoute

internal typealias PlatformExerciseRouteLocation =
    android.health.connect.datatypes.ExerciseRoute.Location

internal typealias PlatformExerciseRouteLocationBuilder =
    android.health.connect.datatypes.ExerciseRoute.Location.Builder

internal typealias PlatformFloorsClimbedRecord =
    android.health.connect.datatypes.FloorsClimbedRecord

internal typealias PlatformFloorsClimbedRecordBuilder =
    android.health.connect.datatypes.FloorsClimbedRecord.Builder

internal typealias PlatformHeartRateRecord = android.health.connect.datatypes.HeartRateRecord

internal typealias PlatformHeartRateRecordBuilder =
    android.health.connect.datatypes.HeartRateRecord.Builder

internal typealias PlatformHeartRateSample =
    android.health.connect.datatypes.HeartRateRecord.HeartRateSample

internal typealias PlatformHeartRateVariabilityRmssdRecord =
    android.health.connect.datatypes.HeartRateVariabilityRmssdRecord

internal typealias PlatformHeartRateVariabilityRmssdRecordBuilder =
    android.health.connect.datatypes.HeartRateVariabilityRmssdRecord.Builder

internal typealias PlatformHeightRecord = android.health.connect.datatypes.HeightRecord

internal typealias PlatformHeightRecordBuilder =
    android.health.connect.datatypes.HeightRecord.Builder

internal typealias PlatformHydrationRecord = android.health.connect.datatypes.HydrationRecord

internal typealias PlatformHydrationRecordBuilder =
    android.health.connect.datatypes.HydrationRecord.Builder

internal typealias PlatformIntermenstrualBleedingRecord =
    android.health.connect.datatypes.IntermenstrualBleedingRecord

internal typealias PlatformIntermenstrualBleedingRecordBuilder =
    android.health.connect.datatypes.IntermenstrualBleedingRecord.Builder

internal typealias PlatformLeanBodyMassRecord = android.health.connect.datatypes.LeanBodyMassRecord

internal typealias PlatformLeanBodyMassRecordBuilder =
    android.health.connect.datatypes.LeanBodyMassRecord.Builder

internal typealias PlatformMenstruationFlowRecord =
    android.health.connect.datatypes.MenstruationFlowRecord

internal typealias PlatformMenstruationFlowRecordBuilder =
    android.health.connect.datatypes.MenstruationFlowRecord.Builder

internal typealias PlatformMenstruationFlowType =
    android.health.connect.datatypes.MenstruationFlowRecord.MenstruationFlowType

internal typealias PlatformMealType = android.health.connect.datatypes.MealType

internal typealias PlatformMenstruationPeriodRecord =
    android.health.connect.datatypes.MenstruationPeriodRecord

internal typealias PlatformMenstruationPeriodRecordBuilder =
    android.health.connect.datatypes.MenstruationPeriodRecord.Builder

internal typealias PlatformNutritionRecord = android.health.connect.datatypes.NutritionRecord

internal typealias PlatformNutritionRecordBuilder =
    android.health.connect.datatypes.NutritionRecord.Builder

internal typealias PlatformOvulationTestRecord =
    android.health.connect.datatypes.OvulationTestRecord

internal typealias PlatformOvulationTestRecordBuilder =
    android.health.connect.datatypes.OvulationTestRecord.Builder

internal typealias PlatformOvulationTestResult =
    android.health.connect.datatypes.OvulationTestRecord.OvulationTestResult

internal typealias PlatformOxygenSaturationRecord =
    android.health.connect.datatypes.OxygenSaturationRecord

internal typealias PlatformOxygenSaturationRecordBuilder =
    android.health.connect.datatypes.OxygenSaturationRecord.Builder

internal typealias PlatformPowerRecord = android.health.connect.datatypes.PowerRecord

internal typealias PlatformPowerRecordBuilder = android.health.connect.datatypes.PowerRecord.Builder

internal typealias PlatformPowerRecordSample =
    android.health.connect.datatypes.PowerRecord.PowerRecordSample

internal typealias PlatformRespiratoryRateRecord =
    android.health.connect.datatypes.RespiratoryRateRecord

internal typealias PlatformRespiratoryRateRecordBuilder =
    android.health.connect.datatypes.RespiratoryRateRecord.Builder

internal typealias PlatformRestingHeartRateRecord =
    android.health.connect.datatypes.RestingHeartRateRecord

internal typealias PlatformRestingHeartRateRecordBuilder =
    android.health.connect.datatypes.RestingHeartRateRecord.Builder

internal typealias PlatformSexualActivityRecord =
    android.health.connect.datatypes.SexualActivityRecord

internal typealias PlatformSexualActivityRecordBuilder =
    android.health.connect.datatypes.SexualActivityRecord.Builder

internal typealias PlatformSexualActivityProtectionUsed =
    android.health.connect.datatypes.SexualActivityRecord.SexualActivityProtectionUsed

internal typealias PlatformSkinTemperatureRecord =
    android.health.connect.datatypes.SkinTemperatureRecord

internal typealias PlatformSkinTemperatureDelta =
    android.health.connect.datatypes.SkinTemperatureRecord.Delta

internal typealias PlatformSkinTemperatureRecordBuilder =
    android.health.connect.datatypes.SkinTemperatureRecord.Builder

internal typealias PlatformSleepSessionRecord = android.health.connect.datatypes.SleepSessionRecord

internal typealias PlatformSleepSessionRecordBuilder =
    android.health.connect.datatypes.SleepSessionRecord.Builder

internal typealias PlatformSleepSessionStage =
    android.health.connect.datatypes.SleepSessionRecord.Stage

internal typealias PlatformSleepStageType =
    android.health.connect.datatypes.SleepSessionRecord.StageType

internal typealias PlatformSpeedRecord = android.health.connect.datatypes.SpeedRecord

internal typealias PlatformSpeedRecordBuilder = android.health.connect.datatypes.SpeedRecord.Builder

internal typealias PlatformSpeedSample =
    android.health.connect.datatypes.SpeedRecord.SpeedRecordSample

internal typealias PlatformStepsCadenceRecord = android.health.connect.datatypes.StepsCadenceRecord

internal typealias PlatformStepsCadenceRecordBuilder =
    android.health.connect.datatypes.StepsCadenceRecord.Builder

internal typealias PlatformStepsCadenceSample =
    android.health.connect.datatypes.StepsCadenceRecord.StepsCadenceRecordSample

internal typealias PlatformStepsRecord = android.health.connect.datatypes.StepsRecord

internal typealias PlatformStepsRecordBuilder = android.health.connect.datatypes.StepsRecord.Builder

internal typealias PlatformTotalCaloriesBurnedRecord =
    android.health.connect.datatypes.TotalCaloriesBurnedRecord

internal typealias PlatformTotalCaloriesBurnedRecordBuilder =
    android.health.connect.datatypes.TotalCaloriesBurnedRecord.Builder

internal typealias PlatformVo2MaxRecord = android.health.connect.datatypes.Vo2MaxRecord

internal typealias PlatformVo2MaxRecordBuilder =
    android.health.connect.datatypes.Vo2MaxRecord.Builder

internal typealias PlatformVo2MaxMeasurementMethod =
    android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod

internal typealias PlatformWeightRecord = android.health.connect.datatypes.WeightRecord

internal typealias PlatformWeightRecordBuilder =
    android.health.connect.datatypes.WeightRecord.Builder

internal typealias PlatformWheelchairPushesRecord =
    android.health.connect.datatypes.WheelchairPushesRecord

internal typealias PlatformWheelchairPushesRecordBuilder =
    android.health.connect.datatypes.WheelchairPushesRecord.Builder

internal typealias PlatformDataOrigin = android.health.connect.datatypes.DataOrigin

internal typealias PlatformDataOriginBuilder = android.health.connect.datatypes.DataOrigin.Builder

internal typealias PlatformDevice = android.health.connect.datatypes.Device

internal typealias PlatformDeviceBuilder = android.health.connect.datatypes.Device.Builder

internal typealias PlatformMetadata = android.health.connect.datatypes.Metadata

internal typealias PlatformMetadataBuilder = android.health.connect.datatypes.Metadata.Builder

internal typealias PlatformBloodGlucose = android.health.connect.datatypes.units.BloodGlucose

internal typealias PlatformEnergy = android.health.connect.datatypes.units.Energy

internal typealias PlatformLength = android.health.connect.datatypes.units.Length

internal typealias PlatformMass = android.health.connect.datatypes.units.Mass

internal typealias PlatformPercentage = android.health.connect.datatypes.units.Percentage

internal typealias PlatformPower = android.health.connect.datatypes.units.Power

internal typealias PlatformPressure = android.health.connect.datatypes.units.Pressure

internal typealias PlatformTemperature = android.health.connect.datatypes.units.Temperature

internal typealias PlatformTemperatureDelta =
    android.health.connect.datatypes.units.TemperatureDelta

internal typealias PlatformVelocity = android.health.connect.datatypes.units.Velocity

internal typealias PlatformVolume = android.health.connect.datatypes.units.Volume
