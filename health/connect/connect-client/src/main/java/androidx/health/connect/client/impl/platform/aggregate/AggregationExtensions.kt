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

@file:RequiresApi(api = 34)

package androidx.health.connect.client.impl.platform.aggregate

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresApi
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest

internal fun AggregateRequest.withFilteredMetrics(predicate: (AggregateMetric<*>) -> Boolean) =
    AggregateRequest(metrics.filter(predicate).toSet(), timeRangeFilter, dataOriginFilter)

internal fun AggregateGroupByPeriodRequest.withFilteredMetrics(
    predicate: (AggregateMetric<*>) -> Boolean
) =
    AggregateGroupByPeriodRequest(
        metrics.filter(predicate).toSet(),
        timeRangeFilter,
        timeRangeSlicer,
        dataOriginFilter
    )

internal fun AggregateGroupByDurationRequest.withFilteredMetrics(
    predicate: (AggregateMetric<*>) -> Boolean
) =
    AggregateGroupByDurationRequest(
        metrics.filter(predicate).toSet(),
        timeRangeFilter,
        timeRangeSlicer,
        dataOriginFilter
    )

// Only check against aggregate metrics added in sdk extension 10, to address b/326414908
// Metrics added later on will be present dependent on feature availability
internal fun AggregateMetric<*>.isPlatformSupportedMetric(): Boolean {
    return if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10) {
        true
    } else {
        this !in AGGREGATE_METRICS_ADDED_IN_SDK_EXT_10
    }
}

internal val AGGREGATE_METRICS_ADDED_IN_SDK_EXT_10: Set<AggregateMetric<*>> =
    setOf(
        BloodPressureRecord.DIASTOLIC_AVG,
        BloodPressureRecord.DIASTOLIC_MAX,
        BloodPressureRecord.DIASTOLIC_MIN,
        BloodPressureRecord.SYSTOLIC_AVG,
        BloodPressureRecord.SYSTOLIC_MAX,
        BloodPressureRecord.SYSTOLIC_MIN,
        CyclingPedalingCadenceRecord.RPM_AVG,
        CyclingPedalingCadenceRecord.RPM_MAX,
        CyclingPedalingCadenceRecord.RPM_MIN,
        NutritionRecord.TRANS_FAT_TOTAL,
        SpeedRecord.SPEED_AVG,
        SpeedRecord.SPEED_MAX,
        SpeedRecord.SPEED_MIN,
        StepsCadenceRecord.RATE_AVG,
        StepsCadenceRecord.RATE_MAX,
        StepsCadenceRecord.RATE_MIN
    )
