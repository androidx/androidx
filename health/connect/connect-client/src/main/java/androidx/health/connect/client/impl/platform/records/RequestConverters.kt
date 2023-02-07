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
@file:RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

package androidx.health.connect.client.impl.platform.records

import android.health.connect.AggregateRecordsRequest
import android.health.connect.ChangeLogTokenRequest
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.TimeInstantRangeFilter
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord as PlatformActiveCaloriesBurnedRecord
import android.health.connect.datatypes.AggregationType
import android.health.connect.datatypes.BasalMetabolicRateRecord as PlatformBasalMetabolicRateRecord
import android.health.connect.datatypes.DistanceRecord as PlatformDistanceRecord
import android.health.connect.datatypes.ElevationGainedRecord as PlatformElevationGainedRecord
import android.health.connect.datatypes.FloorsClimbedRecord as PlatformFloorsClimbedRecord
import android.health.connect.datatypes.HeartRateRecord as PlatformHeartRateRecord
import android.health.connect.datatypes.HeightRecord as PlatformHeightRecord
import android.health.connect.datatypes.HydrationRecord as PlatformHydrationRecord
import android.health.connect.datatypes.NutritionRecord as PlatformNutritionRecord
import android.health.connect.datatypes.PowerRecord as PlatformPowerRecord
import android.health.connect.datatypes.Record as PlatformRecord
import android.health.connect.datatypes.RestingHeartRateRecord as PlatformRestingHeartRateRecord
import android.health.connect.datatypes.StepsRecord as PlatformStepsRecord
import android.health.connect.datatypes.TotalCaloriesBurnedRecord as PlatformTotalCaloriesBurnedRecord
import android.health.connect.datatypes.WeightRecord as PlatformWeightRecord
import android.health.connect.datatypes.WheelchairPushesRecord as PlatformWheelchairPushesRecord
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.impl.platform.time.TimeSource
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

// TODO(b/268327085): Is casting the map values necessary?
// TODO(b/268326895): Add PlatformRestingHeartRateCord.BPM_AV
@Suppress("UNCHECKED_CAST")
private val LONG_AGGREGATION_METRIC_TYPE_MAP: Map<AggregateMetric<Any>, AggregationType<Any>> =
    mapOf(
        HeartRateRecord.BPM_MIN to PlatformHeartRateRecord.BPM_MIN as AggregationType<Any>,
        HeartRateRecord.BPM_MAX to PlatformHeartRateRecord.BPM_MAX as AggregationType<Any>,
        HeartRateRecord.BPM_AVG to PlatformHeartRateRecord.BPM_AVG as AggregationType<Any>,
        HeartRateRecord.MEASUREMENTS_COUNT to
            PlatformHeartRateRecord.HEART_MEASUREMENTS_COUNT as AggregationType<Any>,
        RestingHeartRateRecord.BPM_MIN to
            PlatformRestingHeartRateRecord.BPM_MIN as AggregationType<Any>,
        RestingHeartRateRecord.BPM_MAX to
            PlatformRestingHeartRateRecord.BPM_MAX as AggregationType<Any>,
        StepsRecord.COUNT_TOTAL to PlatformStepsRecord.STEPS_COUNT_TOTAL as AggregationType<Any>,
        WheelchairPushesRecord.COUNT_TOTAL to
            PlatformWheelchairPushesRecord.WHEEL_CHAIR_PUSHES_COUNT_TOTAL as AggregationType<Any>,
    )

@Suppress("UNCHECKED_CAST")
private val DOUBLE_AGGREGATION_METRIC_TYPE_MAP: Map<AggregateMetric<Any>, AggregationType<Any>> =
    mapOf(
        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL to
            PlatformActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL as AggregationType<Any>,
        BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL to
            PlatformBasalMetabolicRateRecord.BASAL_CALORIES_TOTAL as AggregationType<Any>,
        DistanceRecord.DISTANCE_TOTAL to
            PlatformDistanceRecord.DISTANCE_TOTAL as AggregationType<Any>,
        ElevationGainedRecord.ELEVATION_GAINED_TOTAL to
            PlatformElevationGainedRecord.ELEVATION_GAINED_TOTAL as AggregationType<Any>,
        FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL to
            PlatformFloorsClimbedRecord.FLOORS_CLIMBED_TOTAL as AggregationType<Any>,
        HeightRecord.HEIGHT_AVG to PlatformHeightRecord.HEIGHT_AVG as AggregationType<Any>,
        HeightRecord.HEIGHT_MIN to PlatformHeightRecord.HEIGHT_MIN as AggregationType<Any>,
        HeightRecord.HEIGHT_MAX to PlatformHeightRecord.HEIGHT_MAX as AggregationType<Any>,
        HydrationRecord.VOLUME_TOTAL to
            PlatformHydrationRecord.VOLUME_TOTAL as AggregationType<Any>,
        NutritionRecord.BIOTIN_TOTAL to
            PlatformNutritionRecord.BIOTIN_TOTAL as AggregationType<Any>,
        NutritionRecord.CAFFEINE_TOTAL to
            PlatformNutritionRecord.CAFFEINE_TOTAL as AggregationType<Any>,
        NutritionRecord.CALCIUM_TOTAL to
            PlatformNutritionRecord.CALCIUM_TOTAL as AggregationType<Any>,
        NutritionRecord.CHLORIDE_TOTAL to
            PlatformNutritionRecord.CHLORIDE_TOTAL as AggregationType<Any>,
        NutritionRecord.CHOLESTEROL_TOTAL to
            PlatformNutritionRecord.CHOLESTEROL_TOTAL as AggregationType<Any>,
        NutritionRecord.CHROMIUM_TOTAL to
            PlatformNutritionRecord.CHROMIUM_TOTAL as AggregationType<Any>,
        NutritionRecord.COPPER_TOTAL to
            PlatformNutritionRecord.COPPER_TOTAL as AggregationType<Any>,
        NutritionRecord.DIETARY_FIBER_TOTAL to
            PlatformNutritionRecord.DIETARY_FIBER_TOTAL as AggregationType<Any>,
        NutritionRecord.ENERGY_TOTAL to
            PlatformNutritionRecord.ENERGY_TOTAL as AggregationType<Any>,
        NutritionRecord.ENERGY_FROM_FAT_TOTAL to
            PlatformNutritionRecord.ENERGY_FROM_FAT_TOTAL as AggregationType<Any>,
        NutritionRecord.FOLATE_TOTAL to
            PlatformNutritionRecord.FOLATE_TOTAL as AggregationType<Any>,
        NutritionRecord.FOLIC_ACID_TOTAL to
            PlatformNutritionRecord.FOLIC_ACID_TOTAL as AggregationType<Any>,
        NutritionRecord.IODINE_TOTAL to
            PlatformNutritionRecord.IODINE_TOTAL as AggregationType<Any>,
        NutritionRecord.IRON_TOTAL to PlatformNutritionRecord.IRON_TOTAL as AggregationType<Any>,
        NutritionRecord.MAGNESIUM_TOTAL to
            PlatformNutritionRecord.MAGNESIUM_TOTAL as AggregationType<Any>,
        NutritionRecord.MANGANESE_TOTAL to
            PlatformNutritionRecord.MANGANESE_TOTAL as AggregationType<Any>,
        NutritionRecord.MOLYBDENUM_TOTAL to
            PlatformNutritionRecord.MOLYBDENUM_TOTAL as AggregationType<Any>,
        NutritionRecord.MONOUNSATURATED_FAT_TOTAL to
            PlatformNutritionRecord.MONOUNSATURATED_FAT_TOTAL as AggregationType<Any>,
        NutritionRecord.NIACIN_TOTAL to
            PlatformNutritionRecord.NIACIN_TOTAL as AggregationType<Any>,
        NutritionRecord.PANTOTHENIC_ACID_TOTAL to
            PlatformNutritionRecord.PANTOTHENIC_ACID_TOTAL as AggregationType<Any>,
        NutritionRecord.PHOSPHORUS_TOTAL to
            PlatformNutritionRecord.PHOSPHORUS_TOTAL as AggregationType<Any>,
        NutritionRecord.POLYUNSATURATED_FAT_TOTAL to
            PlatformNutritionRecord.POLYUNSATURATED_FAT_TOTAL as AggregationType<Any>,
        NutritionRecord.POTASSIUM_TOTAL to
            PlatformNutritionRecord.POTASSIUM_TOTAL as AggregationType<Any>,
        NutritionRecord.PROTEIN_TOTAL to
            PlatformNutritionRecord.PROTEIN_TOTAL as AggregationType<Any>,
        NutritionRecord.RIBOFLAVIN_TOTAL to
            PlatformNutritionRecord.RIBOFLAVIN_TOTAL as AggregationType<Any>,
        NutritionRecord.SATURATED_FAT_TOTAL to
            PlatformNutritionRecord.SATURATED_FAT_TOTAL as AggregationType<Any>,
        NutritionRecord.SELENIUM_TOTAL to
            PlatformNutritionRecord.SELENIUM_TOTAL as AggregationType<Any>,
        NutritionRecord.SODIUM_TOTAL to
            PlatformNutritionRecord.SODIUM_TOTAL as AggregationType<Any>,
        NutritionRecord.SUGAR_TOTAL to PlatformNutritionRecord.SUGAR_TOTAL as AggregationType<Any>,
        NutritionRecord.THIAMIN_TOTAL to
            PlatformNutritionRecord.THIAMIN_TOTAL as AggregationType<Any>,
        NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL to
            PlatformNutritionRecord.TOTAL_CARBOHYDRATE_TOTAL as AggregationType<Any>,
        NutritionRecord.TOTAL_FAT_TOTAL to
            PlatformNutritionRecord.TOTAL_FAT_TOTAL as AggregationType<Any>,
        NutritionRecord.UNSATURATED_FAT_TOTAL to
            PlatformNutritionRecord.UNSATURATED_FAT_TOTAL as AggregationType<Any>,
        NutritionRecord.VITAMIN_A_TOTAL to
            PlatformNutritionRecord.VITAMIN_A_TOTAL as AggregationType<Any>,
        NutritionRecord.VITAMIN_B12_TOTAL to
            PlatformNutritionRecord.VITAMIN_B12_TOTAL as AggregationType<Any>,
        NutritionRecord.VITAMIN_B6_TOTAL to
            PlatformNutritionRecord.VITAMIN_B6_TOTAL as AggregationType<Any>,
        NutritionRecord.VITAMIN_C_TOTAL to
            PlatformNutritionRecord.VITAMIN_C_TOTAL as AggregationType<Any>,
        NutritionRecord.VITAMIN_D_TOTAL to
            PlatformNutritionRecord.VITAMIN_D_TOTAL as AggregationType<Any>,
        NutritionRecord.VITAMIN_E_TOTAL to
            PlatformNutritionRecord.VITAMIN_E_TOTAL as AggregationType<Any>,
        NutritionRecord.VITAMIN_K_TOTAL to
            PlatformNutritionRecord.VITAMIN_K_TOTAL as AggregationType<Any>,
        NutritionRecord.ZINC_TOTAL to PlatformNutritionRecord.ZINC_TOTAL as AggregationType<Any>,
        PowerRecord.POWER_AVG to PlatformPowerRecord.POWER_AVG as AggregationType<Any>,
        PowerRecord.POWER_MAX to PlatformPowerRecord.POWER_MAX as AggregationType<Any>,
        PowerRecord.POWER_MIN to PlatformPowerRecord.POWER_MIN as AggregationType<Any>,
        TotalCaloriesBurnedRecord.ENERGY_TOTAL to
            PlatformTotalCaloriesBurnedRecord.ENERGY_TOTAL as AggregationType<Any>,
        WeightRecord.WEIGHT_AVG to PlatformWeightRecord.WEIGHT_AVG as AggregationType<Any>,
        WeightRecord.WEIGHT_MIN to PlatformWeightRecord.WEIGHT_MIN as AggregationType<Any>,
        WeightRecord.WEIGHT_MAX to PlatformWeightRecord.WEIGHT_MAX as AggregationType<Any>,
    )

fun ReadRecordsRequest<out Record>.toPlatformRequest(
    timeSource: TimeSource
): ReadRecordsRequestUsingFilters<out PlatformRecord> {
    return ReadRecordsRequestUsingFilters.Builder(recordType.toPlatformRecordClass())
        .setTimeRangeFilter(timeRangeFilter.toPlatformTimeRangeFilter(timeSource))
        .apply { dataOriginFilter.forEach { addDataOrigins(it.toPlatformDataOrigin()) } }
        .build()
}

fun TimeRangeFilter.toPlatformTimeRangeFilter(timeSource: TimeSource): TimeInstantRangeFilter {
    // TODO(b/262571990): pass nullable Instant start/end
    // TODO(b/262571990): pass nullable LocalDateTime start/end
    return TimeInstantRangeFilter.Builder()
        .setStartTime(startTime ?: Instant.EPOCH)
        .setEndTime(endTime ?: timeSource.now)
        .build()
}

fun ChangesTokenRequest.toPlatformRequest(): ChangeLogTokenRequest {
    return ChangeLogTokenRequest.Builder()
        .apply {
            dataOriginFilters.forEach { addDataOriginFilter(it.toPlatformDataOrigin()) }
            recordTypes.forEach { addRecordType(it.toPlatformRecordClass()) }
        }
        .build()
}

fun AggregateRequest.toPlatformRequest(timeSource: TimeSource): AggregateRecordsRequest<Any> {
    return AggregateRecordsRequest.Builder<Any>(
            timeRangeFilter.toPlatformTimeRangeFilter(timeSource))
        .apply {
            dataOriginFilter.forEach { addDataOriginsFilter(it.toPlatformDataOrigin()) }
            metrics.forEach { addAggregationType(it.toAggregationType()) }
        }
        .build()
}

fun AggregateGroupByDurationRequest.toPlatformRequest(
    timeSource: TimeSource
): AggregateRecordsRequest<Any> {
    return AggregateRecordsRequest.Builder<Any>(
            timeRangeFilter.toPlatformTimeRangeFilter(timeSource))
        .apply {
            dataOriginFilter.forEach { addDataOriginsFilter(it.toPlatformDataOrigin()) }
            metrics.forEach { addAggregationType(it.toAggregationType()) }
        }
        .build()
}

fun AggregateGroupByPeriodRequest.toPlatformRequest(
    timeSource: TimeSource
): AggregateRecordsRequest<Any> {
    return AggregateRecordsRequest.Builder<Any>(
            timeRangeFilter.toPlatformTimeRangeFilter(timeSource))
        .apply {
            dataOriginFilter.forEach { addDataOriginsFilter(it.toPlatformDataOrigin()) }
            metrics.forEach { addAggregationType(it.toAggregationType()) }
        }
        .build()
}

fun AggregateMetric<Any>.toAggregationType(): AggregationType<Any> {
    return LONG_AGGREGATION_METRIC_TYPE_MAP[this]
        ?: DOUBLE_AGGREGATION_METRIC_TYPE_MAP[this]
            ?: throw IllegalArgumentException("Unsupported aggregation type $metricKey")
}

fun AggregateMetric<Any>.isLongAggregationType() =
    LONG_AGGREGATION_METRIC_TYPE_MAP.containsKey(this)

fun AggregateMetric<Any>.isDoubleAggregationType() =
    DOUBLE_AGGREGATION_METRIC_TYPE_MAP.containsKey(this)
