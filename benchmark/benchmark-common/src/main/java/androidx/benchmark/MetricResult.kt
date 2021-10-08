/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.benchmark

import android.os.Bundle
import androidx.annotation.RestrictTo
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Results for a given metric from a benchmark, including each measurement made and general stats
 * for those measurements (min/median/max).
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MetricResult(
    val name: String,
    val data: List<Double>,
    val iterationData: List<List<Double>>? = null
) {
    val median: Double
    val medianIndex: Int
    val min: Double
    val minIndex: Int
    val max: Double
    val maxIndex: Int
    val standardDeviation: Double

    val p50: Double
    val p90: Double
    val p95: Double
    val p99: Double

    init {
        val values = data.sorted()
        val size = values.size
        require(size >= 1) { "At least one result is necessary." }

        val mean: Double = data.average()
        min = values.first()
        max = values.last()
        median = getPercentile(values, 50)

        p50 = getPercentile(values, 50)
        p90 = getPercentile(values, 90)
        p95 = getPercentile(values, 95)
        p99 = getPercentile(values, 99)

        minIndex = data.indexOfFirst { it == min }
        maxIndex = data.indexOfFirst { it == max }
        medianIndex = data.size / 2

        standardDeviation = if (data.size == 1) {
            0.0
        } else {
            val sum = values.map { (it - mean).pow(2) }.sum()
            sqrt(sum / (size - 1).toDouble())
        }
    }

    internal fun getSummary(): String {
        return "Metric ($name) results: median $median, min $min, max $max, " +
            "standardDeviation: $standardDeviation"
    }

    public fun putInBundle(status: Bundle, prefix: String) {
        // format string to be in instrumentation results format
        val bundleName = name.toOutputMetricName()

        status.putDouble("${prefix}${bundleName}_min", min)
        status.putDouble("${prefix}${bundleName}_median", median)
        status.putDouble("${prefix}${bundleName}_stddev", standardDeviation)
    }

    public fun putPercentilesInBundle(status: Bundle, prefix: String) {
        // format string to be in instrumentation results format
        val bundleName = name.toOutputMetricName()

        status.putDouble("${prefix}${bundleName}_p50", p50)
        status.putDouble("${prefix}${bundleName}_p90", p90)
        status.putDouble("${prefix}${bundleName}_p95", p95)
        status.putDouble("${prefix}${bundleName}_p99", p99)
    }

    // NOTE: Studio-generated, re-generate if members change
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MetricResult

        if (name != other.name) return false
        if (median != other.median) return false
        if (medianIndex != other.medianIndex) return false
        if (min != other.min) return false
        if (minIndex != other.minIndex) return false
        if (max != other.max) return false
        if (maxIndex != other.maxIndex) return false
        if (standardDeviation != other.standardDeviation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + median.hashCode()
        result = 31 * result + medianIndex
        result = 31 * result + min.hashCode()
        result = 31 * result + minIndex
        result = 31 * result + max.hashCode()
        result = 31 * result + maxIndex
        result = 31 * result + standardDeviation.hashCode()
        return result
    }

    companion object {
        internal fun lerp(a: Double, b: Double, ratio: Double): Double {
            return (a * (1 - ratio) + b * (ratio))
        }

        fun getPercentile(sortedData: List<Double>, percentile: Int): Double {
            val idealIndex = percentile.coerceIn(0, 100) / 100.0 * (sortedData.size - 1)
            val firstIndex = idealIndex.toInt()
            val secondIndex = firstIndex + 1

            val firstValue = sortedData[firstIndex]
            val secondValue = sortedData.getOrElse(secondIndex) { firstValue }
            return lerp(firstValue, secondValue, idealIndex - firstIndex)
        }
    }
}
