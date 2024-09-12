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
@file:RequiresApi(api = 34)

package androidx.health.connect.client.impl.platform.records

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
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
import kotlin.reflect.KClass

@SuppressLint("NewApi") // Guarded by sdk extension
@RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 13)
internal val SDK_TO_PLATFORM_RECORD_CLASS_EXT_13:
    Map<KClass<out Record>, Class<out PlatformRecord>> =
    if (isAtLeastSdkExtension13()) {
        mapOf(SkinTemperatureRecord::class to PlatformSkinTemperatureRecord::class.java)
    } else {
        emptyMap()
    }

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
