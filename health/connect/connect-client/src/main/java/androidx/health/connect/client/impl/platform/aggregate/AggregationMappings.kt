/*
 * Copyright 2024 The Android Open Source Project
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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@file:RequiresApi(api = 34)

package androidx.health.connect.client.impl.platform.aggregate

import android.health.connect.datatypes.ActiveCaloriesBurnedRecord as PlatformActiveCaloriesBurnedRecord
import android.health.connect.datatypes.AggregationType as PlatformAggregateMetric
import android.health.connect.datatypes.BasalMetabolicRateRecord as PlatformBasalMetabolicRateRecord
import android.health.connect.datatypes.DistanceRecord as PlatformDistanceRecord
import android.health.connect.datatypes.ElevationGainedRecord as PlatformElevationGainedRecord
import android.health.connect.datatypes.FloorsClimbedRecord as PlatformFloorsClimbedRecord
import android.health.connect.datatypes.HeartRateRecord as PlatformHeartRateRecord
import android.health.connect.datatypes.HeightRecord as PlatformHeightRecord
import android.health.connect.datatypes.HydrationRecord as PlatformHydrationRecord
import android.health.connect.datatypes.NutritionRecord as PlatformNutritionRecord
import android.health.connect.datatypes.PowerRecord as PlatformPowerRecord
import android.health.connect.datatypes.StepsRecord as PlatformStepsRecord
import android.health.connect.datatypes.TotalCaloriesBurnedRecord as PlatformTotalCaloriesBurnedRecord
import android.health.connect.datatypes.WeightRecord as PlatformWeightRecord
import android.health.connect.datatypes.WheelchairPushesRecord as PlatformWheelchairPushesRecord
import android.health.connect.datatypes.units.Energy as PlatformEnergy
import android.health.connect.datatypes.units.Length as PlatformLength
import android.health.connect.datatypes.units.Mass as PlatformMass
import android.health.connect.datatypes.units.Power as PlatformPower
import android.health.connect.datatypes.units.Volume as PlatformVolume
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.impl.platform.records.PlatformBloodPressureRecord
import androidx.health.connect.client.impl.platform.records.PlatformCyclingPedalingCadenceRecord
import androidx.health.connect.client.impl.platform.records.PlatformExerciseSessionRecord
import androidx.health.connect.client.impl.platform.records.PlatformPressure
import androidx.health.connect.client.impl.platform.records.PlatformRestingHeartRateRecord
import androidx.health.connect.client.impl.platform.records.PlatformSleepSessionRecord
import androidx.health.connect.client.impl.platform.records.PlatformSpeedRecord
import androidx.health.connect.client.impl.platform.records.PlatformStepsCadenceRecord
import androidx.health.connect.client.impl.platform.records.PlatformVelocity
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Velocity
import androidx.health.connect.client.units.Volume
import java.time.Duration

private val DOUBLE_AGGREGATION_METRIC_TYPE_SDK_EXT_10_PAIRS =
    if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10) {
        arrayOf(
            CyclingPedalingCadenceRecord.RPM_AVG to PlatformCyclingPedalingCadenceRecord.RPM_AVG,
            CyclingPedalingCadenceRecord.RPM_MAX to PlatformCyclingPedalingCadenceRecord.RPM_MAX,
            CyclingPedalingCadenceRecord.RPM_MIN to PlatformCyclingPedalingCadenceRecord.RPM_MIN,
            StepsCadenceRecord.RATE_AVG to PlatformStepsCadenceRecord.STEPS_CADENCE_RATE_AVG,
            StepsCadenceRecord.RATE_MAX to PlatformStepsCadenceRecord.STEPS_CADENCE_RATE_MAX,
            StepsCadenceRecord.RATE_MIN to PlatformStepsCadenceRecord.STEPS_CADENCE_RATE_MIN
        )
    } else {
        emptyArray()
    }

internal val DOUBLE_AGGREGATION_METRIC_TYPE_MAP:
    Map<AggregateMetric<Double>, PlatformAggregateMetric<Double>> =
    mapOf(
        FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL to
            PlatformFloorsClimbedRecord.FLOORS_CLIMBED_TOTAL,
        *DOUBLE_AGGREGATION_METRIC_TYPE_SDK_EXT_10_PAIRS
    )

internal val DURATION_AGGREGATION_METRIC_TYPE_MAP:
    Map<AggregateMetric<Duration>, PlatformAggregateMetric<Long>> =
    mapOf(
        ExerciseSessionRecord.EXERCISE_DURATION_TOTAL to
            PlatformExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
        SleepSessionRecord.SLEEP_DURATION_TOTAL to PlatformSleepSessionRecord.SLEEP_DURATION_TOTAL
    )

internal val ENERGY_AGGREGATION_METRIC_TYPE_MAP:
    Map<AggregateMetric<Energy>, PlatformAggregateMetric<PlatformEnergy>> =
    mapOf(
        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL to
            PlatformActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
        BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL to
            PlatformBasalMetabolicRateRecord.BASAL_CALORIES_TOTAL,
        NutritionRecord.ENERGY_TOTAL to PlatformNutritionRecord.ENERGY_TOTAL,
        NutritionRecord.ENERGY_FROM_FAT_TOTAL to PlatformNutritionRecord.ENERGY_FROM_FAT_TOTAL,
        TotalCaloriesBurnedRecord.ENERGY_TOTAL to PlatformTotalCaloriesBurnedRecord.ENERGY_TOTAL,
    )

internal val LENGTH_AGGREGATION_METRIC_TYPE_MAP:
    Map<AggregateMetric<Length>, PlatformAggregateMetric<PlatformLength>> =
    mapOf(
        DistanceRecord.DISTANCE_TOTAL to PlatformDistanceRecord.DISTANCE_TOTAL,
        ElevationGainedRecord.ELEVATION_GAINED_TOTAL to
            PlatformElevationGainedRecord.ELEVATION_GAINED_TOTAL,
        HeightRecord.HEIGHT_AVG to PlatformHeightRecord.HEIGHT_AVG,
        HeightRecord.HEIGHT_MIN to PlatformHeightRecord.HEIGHT_MIN,
        HeightRecord.HEIGHT_MAX to PlatformHeightRecord.HEIGHT_MAX,
    )

internal val LONG_AGGREGATION_METRIC_TYPE_MAP:
    Map<AggregateMetric<Long>, PlatformAggregateMetric<Long>> =
    mapOf(
        HeartRateRecord.BPM_AVG to PlatformHeartRateRecord.BPM_AVG,
        HeartRateRecord.BPM_MIN to PlatformHeartRateRecord.BPM_MIN,
        HeartRateRecord.BPM_MAX to PlatformHeartRateRecord.BPM_MAX,
        HeartRateRecord.MEASUREMENTS_COUNT to PlatformHeartRateRecord.HEART_MEASUREMENTS_COUNT,
        RestingHeartRateRecord.BPM_AVG to PlatformRestingHeartRateRecord.BPM_AVG,
        RestingHeartRateRecord.BPM_MIN to PlatformRestingHeartRateRecord.BPM_MIN,
        RestingHeartRateRecord.BPM_MAX to PlatformRestingHeartRateRecord.BPM_MAX,
        StepsRecord.COUNT_TOTAL to PlatformStepsRecord.STEPS_COUNT_TOTAL,
        WheelchairPushesRecord.COUNT_TOTAL to
            PlatformWheelchairPushesRecord.WHEEL_CHAIR_PUSHES_COUNT_TOTAL,
    )

internal val GRAMS_AGGREGATION_METRIC_TYPE_MAP:
    Map<AggregateMetric<Mass>, PlatformAggregateMetric<PlatformMass>> =
    mapOf(
        NutritionRecord.BIOTIN_TOTAL to PlatformNutritionRecord.BIOTIN_TOTAL,
        NutritionRecord.CAFFEINE_TOTAL to PlatformNutritionRecord.CAFFEINE_TOTAL,
        NutritionRecord.CALCIUM_TOTAL to PlatformNutritionRecord.CALCIUM_TOTAL,
        NutritionRecord.CHLORIDE_TOTAL to PlatformNutritionRecord.CHLORIDE_TOTAL,
        NutritionRecord.CHOLESTEROL_TOTAL to PlatformNutritionRecord.CHOLESTEROL_TOTAL,
        NutritionRecord.CHROMIUM_TOTAL to PlatformNutritionRecord.CHROMIUM_TOTAL,
        NutritionRecord.COPPER_TOTAL to PlatformNutritionRecord.COPPER_TOTAL,
        NutritionRecord.DIETARY_FIBER_TOTAL to PlatformNutritionRecord.DIETARY_FIBER_TOTAL,
        NutritionRecord.FOLATE_TOTAL to PlatformNutritionRecord.FOLATE_TOTAL,
        NutritionRecord.FOLIC_ACID_TOTAL to PlatformNutritionRecord.FOLIC_ACID_TOTAL,
        NutritionRecord.IODINE_TOTAL to PlatformNutritionRecord.IODINE_TOTAL,
        NutritionRecord.IRON_TOTAL to PlatformNutritionRecord.IRON_TOTAL,
        NutritionRecord.MAGNESIUM_TOTAL to PlatformNutritionRecord.MAGNESIUM_TOTAL,
        NutritionRecord.MANGANESE_TOTAL to PlatformNutritionRecord.MANGANESE_TOTAL,
        NutritionRecord.MOLYBDENUM_TOTAL to PlatformNutritionRecord.MOLYBDENUM_TOTAL,
        NutritionRecord.MONOUNSATURATED_FAT_TOTAL to
            PlatformNutritionRecord.MONOUNSATURATED_FAT_TOTAL,
        NutritionRecord.NIACIN_TOTAL to PlatformNutritionRecord.NIACIN_TOTAL,
        NutritionRecord.PANTOTHENIC_ACID_TOTAL to PlatformNutritionRecord.PANTOTHENIC_ACID_TOTAL,
        NutritionRecord.PHOSPHORUS_TOTAL to PlatformNutritionRecord.PHOSPHORUS_TOTAL,
        NutritionRecord.POLYUNSATURATED_FAT_TOTAL to
            PlatformNutritionRecord.POLYUNSATURATED_FAT_TOTAL,
        NutritionRecord.POTASSIUM_TOTAL to PlatformNutritionRecord.POTASSIUM_TOTAL,
        NutritionRecord.PROTEIN_TOTAL to PlatformNutritionRecord.PROTEIN_TOTAL,
        NutritionRecord.RIBOFLAVIN_TOTAL to PlatformNutritionRecord.RIBOFLAVIN_TOTAL,
        NutritionRecord.SATURATED_FAT_TOTAL to PlatformNutritionRecord.SATURATED_FAT_TOTAL,
        NutritionRecord.SELENIUM_TOTAL to PlatformNutritionRecord.SELENIUM_TOTAL,
        NutritionRecord.SODIUM_TOTAL to PlatformNutritionRecord.SODIUM_TOTAL,
        NutritionRecord.SUGAR_TOTAL to PlatformNutritionRecord.SUGAR_TOTAL,
        NutritionRecord.THIAMIN_TOTAL to PlatformNutritionRecord.THIAMIN_TOTAL,
        NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL to
            PlatformNutritionRecord.TOTAL_CARBOHYDRATE_TOTAL,
        NutritionRecord.TOTAL_FAT_TOTAL to PlatformNutritionRecord.TOTAL_FAT_TOTAL,
        NutritionRecord.UNSATURATED_FAT_TOTAL to PlatformNutritionRecord.UNSATURATED_FAT_TOTAL,
        NutritionRecord.VITAMIN_A_TOTAL to PlatformNutritionRecord.VITAMIN_A_TOTAL,
        NutritionRecord.VITAMIN_B12_TOTAL to PlatformNutritionRecord.VITAMIN_B12_TOTAL,
        NutritionRecord.VITAMIN_B6_TOTAL to PlatformNutritionRecord.VITAMIN_B6_TOTAL,
        NutritionRecord.VITAMIN_C_TOTAL to PlatformNutritionRecord.VITAMIN_C_TOTAL,
        NutritionRecord.VITAMIN_D_TOTAL to PlatformNutritionRecord.VITAMIN_D_TOTAL,
        NutritionRecord.VITAMIN_E_TOTAL to PlatformNutritionRecord.VITAMIN_E_TOTAL,
        NutritionRecord.VITAMIN_K_TOTAL to PlatformNutritionRecord.VITAMIN_K_TOTAL,
        NutritionRecord.ZINC_TOTAL to PlatformNutritionRecord.ZINC_TOTAL,
        *if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10) {
            arrayOf(NutritionRecord.TRANS_FAT_TOTAL to PlatformNutritionRecord.TRANS_FAT_TOTAL)
        } else {
            emptyArray()
        }
    )

val KILOGRAMS_AGGREGATION_METRIC_TYPE_MAP:
    Map<AggregateMetric<Mass>, PlatformAggregateMetric<PlatformMass>> =
    mapOf(
        WeightRecord.WEIGHT_AVG to PlatformWeightRecord.WEIGHT_AVG,
        WeightRecord.WEIGHT_MIN to PlatformWeightRecord.WEIGHT_MIN,
        WeightRecord.WEIGHT_MAX to PlatformWeightRecord.WEIGHT_MAX,
    )

internal val POWER_AGGREGATION_METRIC_TYPE_MAP:
    Map<AggregateMetric<Power>, PlatformAggregateMetric<PlatformPower>> =
    mapOf(
        PowerRecord.POWER_AVG to PlatformPowerRecord.POWER_AVG,
        PowerRecord.POWER_MAX to PlatformPowerRecord.POWER_MAX,
        PowerRecord.POWER_MIN to PlatformPowerRecord.POWER_MIN,
    )

internal val PRESSURE_AGGREGATION_METRIC_TYPE_MAP:
    Map<AggregateMetric<Pressure>, PlatformAggregateMetric<PlatformPressure>> =
    if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10) {
            arrayOf(
                BloodPressureRecord.DIASTOLIC_AVG to PlatformBloodPressureRecord.DIASTOLIC_AVG,
                BloodPressureRecord.DIASTOLIC_MAX to PlatformBloodPressureRecord.DIASTOLIC_MAX,
                BloodPressureRecord.DIASTOLIC_MIN to PlatformBloodPressureRecord.DIASTOLIC_MIN,
                BloodPressureRecord.SYSTOLIC_AVG to PlatformBloodPressureRecord.SYSTOLIC_AVG,
                BloodPressureRecord.SYSTOLIC_MAX to PlatformBloodPressureRecord.SYSTOLIC_MAX,
                BloodPressureRecord.SYSTOLIC_MIN to PlatformBloodPressureRecord.SYSTOLIC_MIN
            )
        } else {
            emptyArray()
        }
        .toMap()

internal val VELOCITY_AGGREGATION_METRIC_TYPE_MAP:
    Map<AggregateMetric<Velocity>, PlatformAggregateMetric<PlatformVelocity>> =
    if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10) {
            arrayOf(
                SpeedRecord.SPEED_AVG to PlatformSpeedRecord.SPEED_AVG,
                SpeedRecord.SPEED_MAX to PlatformSpeedRecord.SPEED_MAX,
                SpeedRecord.SPEED_MIN to PlatformSpeedRecord.SPEED_MIN
            )
        } else {
            emptyArray()
        }
        .toMap()

internal val VOLUME_AGGREGATION_METRIC_TYPE_MAP:
    Map<AggregateMetric<Volume>, PlatformAggregateMetric<PlatformVolume>> =
    mapOf(
        HydrationRecord.VOLUME_TOTAL to PlatformHydrationRecord.VOLUME_TOTAL,
    )
