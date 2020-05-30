/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.annotation.FloatRange
import androidx.compose.Stable
import androidx.ui.core.Modifier
import androidx.ui.core.semantics.semantics
import androidx.ui.semantics.AccessibilityRangeInfo
import androidx.ui.semantics.accessibilityValue
import androidx.ui.semantics.accessibilityValueRange
import kotlin.math.roundToInt

/**
 * Contains the [Semantics] required for a determinate progress indicator, that represents progress
 * ranging from 0.0 to 1.0.
 *
 * @sample androidx.ui.foundation.samples.DeterminateProgressSample
 *
 * @param progress The progress of this progress indicator, where 0.0 represents no progress and 1.0
 * represents full progress
 * @throws IllegalArgumentException when the progress is not within range
 */
@Stable
fun Modifier.determinateProgressIndicator(
    @FloatRange(from = 0.0, to = 1.0) progress: Float
): Modifier {
    if (progress !in 0f..1f) {
        throw IllegalArgumentException("Progress must be between 0.0 and 1.0")
    }

    // We only display 0% or 100% when it is exactly 0% or 100%.
    val percent = when (progress) {
        0f -> 0
        1f -> 100
        else -> (progress * 100).roundToInt().coerceIn(1, 99)
    }

    return semantics {
        accessibilityValue = Strings.TemplatePercent.format(percent)
        accessibilityValueRange = AccessibilityRangeInfo(progress, 0f..1f)
    }
}
