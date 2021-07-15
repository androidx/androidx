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
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Provides statistics such as mean, median, min, max, and percentiles, given a list of input
 * values.
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Stats(data: LongArray, public val name: String) {
    public val median: Long
    public val medianIndex: Int
    public val min: Long
    public val minIndex: Int
    public val max: Long
    public val maxIndex: Int
    public val standardDeviation: Double

    init {
        val values = data.sorted()
        val size = values.size
        require(size >= 1) { "At least one result is necessary." }

        val mean: Double = data.average()
        min = values.first()
        max = values.last()
        median = getPercentile(values, 50)

        minIndex = data.indexOf(min)
        maxIndex = data.indexOf(max)
        medianIndex = data.size / 2

        standardDeviation = if (data.size == 1) {
            0.0
        } else {
            val sum = values.map { (it - mean).pow(2) }.sum()
            sqrt(sum / (size - 1).toDouble())
        }
    }

    internal fun getSummary(): String {
        return "Stats for $name: median $median, min $min, max $max, " +
            "standardDeviation: $standardDeviation"
    }

    public fun putInBundle(status: Bundle, prefix: String) {
        if (name == "timeNs") {
            // compatibility naming scheme.
            // should be removed, once we timeNs_min has been in dashboard for several weeks
            status.putLong("${prefix}min", min)
            status.putLong("${prefix}median", median)
            status.putLong("${prefix}standardDeviation", standardDeviation.toLong())
        }

        // format string to be in instrumentation results format
        val bundleName = name.toOutputMetricName()

        status.putLong("${prefix}${bundleName}_min", min)
        status.putLong("${prefix}${bundleName}_median", median)
        status.putLong("${prefix}${bundleName}_stddev", standardDeviation.toLong())
    }

    // NOTE: Studio-generated, re-generate if members change
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Stats

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

    internal companion object {
        internal fun lerp(a: Long, b: Long, ratio: Double): Long {
            return (a * (1 - ratio) + b * (ratio)).roundToLong()
        }

        internal fun getPercentile(data: List<Long>, percentile: Int): Long {
            val idealIndex = percentile.coerceIn(0, 100) / 100.0 * (data.size - 1)
            val firstIndex = idealIndex.toInt()
            val secondIndex = firstIndex + 1

            val firstValue = data[firstIndex]
            val secondValue = data.getOrElse(secondIndex) { firstValue }
            return lerp(firstValue, secondValue, idealIndex - firstIndex)
        }
    }
}
