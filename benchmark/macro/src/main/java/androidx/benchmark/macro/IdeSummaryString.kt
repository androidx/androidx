/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.benchmark.macro

import androidx.benchmark.Stats
import java.util.Collections
import kotlin.math.max

internal fun ideSummaryString(benchmarkName: String, statsList: List<Stats>): String {
    val maxLabelLength = Collections.max(statsList.map { it.name.length })

    // max string length of any printed min/median/max is the largest max value seen. used to pad.
    val maxValueLength = statsList
        .map { it.max }
        .reduce { acc, maxValue -> max(acc, maxValue) }
        .toString().length

    return "$benchmarkName\n" + statsList.joinToString("\n") {
        val displayName = it.name.padStart(maxLabelLength)
        val displayMin = it.min.toString().padStart(maxValueLength)
        val displayMedian = it.median.toString().padStart(maxValueLength)
        val displayMax = it.max.toString().padStart(maxValueLength)
        "  $displayName   min $displayMin,   median $displayMedian,   max $displayMax"
    } + "\n"
}