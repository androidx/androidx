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

import android.health.connect.datatypes.ActiveCaloriesBurnedRecord as PlatformActiveCaloriesBurnedRecord
import android.health.connect.datatypes.BasalBodyTemperatureRecord as PlatformBasalBodyTemperatureRecord
import android.health.connect.datatypes.BasalMetabolicRateRecord as PlatformBasalMetabolicRateRecord
import android.health.connect.datatypes.BloodGlucoseRecord as PlatformBloodGlucoseRecord
import android.health.connect.datatypes.BloodPressureRecord as PlatformBloodPressureRecord
import android.health.connect.datatypes.BodyFatRecord as PlatformBodyFatRecord
import android.health.connect.datatypes.BodyTemperatureRecord as PlatformBodyTemperatureRecord
import android.health.connect.datatypes.BodyWaterMassRecord as PlatformBodyWaterMassRecord
import android.health.connect.datatypes.BoneMassRecord as PlatformBoneMassRecord
import android.health.connect.datatypes.CervicalMucusRecord as PlatformCervicalMucusRecord
import android.health.connect.datatypes.CyclingPedalingCadenceRecord as PlatformCyclingPedalingCadenceRecord
import android.health.connect.datatypes.DistanceRecord as PlatformDistanceRecord
import android.health.connect.datatypes.ElevationGainedRecord as PlatformElevationGainedRecord
import android.health.connect.datatypes.ExerciseSessionRecord as PlatformExerciseSessionRecord
import android.health.connect.datatypes.FloorsClimbedRecord as PlatformFloorsClimbedRecord
import android.health.connect.datatypes.HeartRateRecord as PlatformHeartRateRecord
import android.health.connect.datatypes.HeartRateVariabilityRmssdRecord as PlatformHeartRateVariabilityRmssdRecord
import android.health.connect.datatypes.HeightRecord as PlatformHeightRecord
import android.health.connect.datatypes.HydrationRecord as PlatformHydrationRecord
import android.health.connect.datatypes.IntermenstrualBleedingRecord as PlatformIntermenstrualBleedingRecord
import android.health.connect.datatypes.IntervalRecord as PlatformIntervalRecord
import android.health.connect.datatypes.LeanBodyMassRecord as PlatformLeanBodyMassRecord
import android.health.connect.datatypes.MenstruationFlowRecord as PlatformMenstruationFlowRecord
import android.health.connect.datatypes.MenstruationPeriodRecord as PlatformMenstruationPeriodRecord
import android.health.connect.datatypes.NutritionRecord as PlatformNutritionRecord
import android.health.connect.datatypes.OvulationTestRecord as PlatformOvulationTestRecord
import android.health.connect.datatypes.OxygenSaturationRecord as PlatformOxygenSaturationRecord
import android.health.connect.datatypes.PowerRecord as PlatformPowerRecord
import android.health.connect.datatypes.Record as PlatformRecord
import android.health.connect.datatypes.RespiratoryRateRecord as PlatformRespiratoryRateRecord
import android.health.connect.datatypes.RestingHeartRateRecord as PlatformRestingHeartRateRecord
import android.health.connect.datatypes.SexualActivityRecord as PlatformSexualActivityRecord
import android.health.connect.datatypes.SleepSessionRecord as PlatformSleepSessionRecord
import android.health.connect.datatypes.SpeedRecord as PlatformSpeedRecord
import android.health.connect.datatypes.StepsCadenceRecord as PlatformStepsCadenceRecord
import android.health.connect.datatypes.StepsRecord as PlatformStepsRecord
import android.health.connect.datatypes.TotalCaloriesBurnedRecord as PlatformTotalCaloriesBurnedRecord
import android.health.connect.datatypes.Vo2MaxRecord as PlatformVo2MaxRecord
import android.health.connect.datatypes.WeightRecord as PlatformWeightRecord
import android.health.connect.datatypes.WheelchairPushesRecord as PlatformWheelchairPushesRecord
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.IntervalRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
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
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import kotlin.reflect.KClass

internal val SDK_TO_PLATFORM_RECORD_CLASS: Map<KClass<out Record>, Class<out PlatformRecord>> =
    mapOf(
        ActiveCaloriesBurnedRecord::class to PlatformActiveCaloriesBurnedRecord::class.java,
        BasalBodyTemperatureRecord::class to PlatformBasalBodyTemperatureRecord::class.java,
        BasalMetabolicRateRecord::class to PlatformBasalMetabolicRateRecord::class.java,
        BloodGlucoseRecord::class to PlatformBloodGlucoseRecord::class.java,
        BloodPressureRecord::class to PlatformBloodPressureRecord::class.java,
        BodyFatRecord::class to PlatformBodyFatRecord::class.java,
        BodyTemperatureRecord::class to PlatformBodyTemperatureRecord::class.java,
        BodyWaterMassRecord::class to PlatformBodyWaterMassRecord::class.java,
        BoneMassRecord::class to PlatformBoneMassRecord::class.java,
        CervicalMucusRecord::class to PlatformCervicalMucusRecord::class.java,
        CyclingPedalingCadenceRecord::class to PlatformCyclingPedalingCadenceRecord::class.java,
        DistanceRecord::class to PlatformDistanceRecord::class.java,
        ElevationGainedRecord::class to PlatformElevationGainedRecord::class.java,
        ExerciseSessionRecord::class to PlatformExerciseSessionRecord::class.java,
        FloorsClimbedRecord::class to PlatformFloorsClimbedRecord::class.java,
        HeartRateRecord::class to PlatformHeartRateRecord::class.java,
        HeartRateVariabilityRmssdRecord::class to
            PlatformHeartRateVariabilityRmssdRecord::class.java,
        HeightRecord::class to PlatformHeightRecord::class.java,
        HydrationRecord::class to PlatformHydrationRecord::class.java,
        IntermenstrualBleedingRecord::class to PlatformIntermenstrualBleedingRecord::class.java,
        IntervalRecord::class to PlatformIntervalRecord::class.java,
        LeanBodyMassRecord::class to PlatformLeanBodyMassRecord::class.java,
        MenstruationFlowRecord::class to PlatformMenstruationFlowRecord::class.java,
        MenstruationPeriodRecord::class to PlatformMenstruationPeriodRecord::class.java,
        NutritionRecord::class to PlatformNutritionRecord::class.java,
        OvulationTestRecord::class to PlatformOvulationTestRecord::class.java,
        OxygenSaturationRecord::class to PlatformOxygenSaturationRecord::class.java,
        PowerRecord::class to PlatformPowerRecord::class.java,
        RespiratoryRateRecord::class to PlatformRespiratoryRateRecord::class.java,
        RestingHeartRateRecord::class to PlatformRestingHeartRateRecord::class.java,
        SexualActivityRecord::class to PlatformSexualActivityRecord::class.java,
        SleepSessionRecord::class to PlatformSleepSessionRecord::class.java,
        SpeedRecord::class to PlatformSpeedRecord::class.java,
        StepsCadenceRecord::class to PlatformStepsCadenceRecord::class.java,
        StepsRecord::class to PlatformStepsRecord::class.java,
        TotalCaloriesBurnedRecord::class to PlatformTotalCaloriesBurnedRecord::class.java,
        Vo2MaxRecord::class to PlatformVo2MaxRecord::class.java,
        WeightRecord::class to PlatformWeightRecord::class.java,
        WheelchairPushesRecord::class to PlatformWheelchairPushesRecord::class.java,
    )
