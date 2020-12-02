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
import kotlin.Double.Companion.NaN
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Provides statistics such as mean, median, min, max, and percentiles, given a list of input
 * values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Stats(data: LongArray, public val name: String) {
    public val median: Long
    public val min: Long
    public val max: Long
    public val mean: Double = data.average()
    public val standardDeviation: Double

    init {
        val values = data.sorted()
        val size = values.size
        if (size < 1) {
            throw IllegalArgumentException("At least one result is necessary.")
        }

        min = values.first()
        max = values.last()
        median = getPercentile(values, 50)
        standardDeviation = if (size == 1) {
            NaN
        } else {
            val sum = values.map { (it - mean).pow(2) }.sum()
            Math.sqrt(sum / (size - 1).toDouble())
        }
    }

    internal fun getSummary(): String {
        return "Stats for $name: median $median, min $min, max $max, mean $mean, " +
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

        // format string for
        val bundleName = name.toOutputMetricName()

        status.putLong("${prefix}${bundleName}_min", min)
        status.putLong("${prefix}${bundleName}_median", median)
        status.putLong("${prefix}${bundleName}_stddev", standardDeviation.toLong())
    }

    override fun equals(other: Any?): Boolean {
        return (other is Stats && other.hashCode() == this.hashCode())
    }

    override fun hashCode(): Int {
        return min.hashCode() + max.hashCode() + median.hashCode() + standardDeviation.hashCode() +
            mean.hashCode()
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
