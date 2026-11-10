/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.core.templates

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo

/**
 * Defines a single column in a template bar chart.
 *
 * ```kotlin
 * val weeklySteps = listOf(
 *     ChartBar(0.65f, "M"),
 *     ChartBar(1.0f, "T"),
 *     ChartBar(0.80f, "W", isEmphasized = true),
 * )
 * ```
 *
 * @property value normalized bar fill height in `[0.0f, 1.0f]`, where `1.0f` fills the chart's
 *   available bar height
 * @property label optional short axis label displayed beneath the bar (for example, `"M"`), or
 *   `null` when unlabeled
 * @property isEmphasized whether this bar uses the emphasized accent color; defaults to `false`, so
 *   a bar is only emphasized when the caller asks for it, regardless of its [value]
 * @throws IllegalArgumentException if [value] is outside `[0.0f, 1.0f]` or [label] is blank
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ChartBar(
    @param:FloatRange(from = 0.0, to = 1.0)
    @get:FloatRange(from = 0.0, to = 1.0)
    public val value: Float,
    public val label: String? = null,
    public val isEmphasized: Boolean = false,
) {
    init {
        require(value.isFinite() && value in 0f..1f) {
            "Bar value must be in [0, 1], was $value."
        }
        require(label == null || label.isNotBlank()) { "Bar label must not be blank." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChartBar) return false
        return value.compareTo(other.value) == 0 &&
            label == other.label &&
            isEmphasized == other.isEmphasized
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + isEmphasized.hashCode()
        return result
    }

    override fun toString(): String =
        "ChartBar(value=$value, label=$label, isEmphasized=$isEmphasized)"
}
