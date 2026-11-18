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

package androidx.xr.glimmer.internal.progressindicator

import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

/**
 * Increases the semantics bounds vertically by [VerticalSemanticsBoundsPadding] in order to meet
 * the TalkBack box minimum size while preserving the visual appearance.
 */
internal val IncreaseVerticalSemanticsBounds: Modifier =
    Modifier.layout { measurable, constraints ->
            val paddingPx = VerticalSemanticsBoundsPadding.roundToPx()
            // We need to add vertical padding to the semantics bounds in order to meet
            // screenreader green box minimum size, but we also want to
            // preserve a visual appearance and layout size below that minimum
            // in order to maintain backwards compatibility. This custom
            // layout effectively implements "negative padding".
            val newConstraint = constraints.offset(0, paddingPx * 2)
            val placeable = measurable.measure(newConstraint)

            // But when actually placing the placeable, create the layout without additional
            // space. Place the placeable where it would've been without any extra padding.
            val height = placeable.height - paddingPx * 2
            val width = placeable.width
            layout(width, height) { placeable.place(0, -paddingPx) }
        }
        .semantics(mergeDescendants = true) {}
        .padding(vertical = VerticalSemanticsBoundsPadding)

private val VerticalSemanticsBoundsPadding: Dp
    get() = 10.dp

/** An animation spec for indeterminate linear progress indicator first line head position. */
internal val linearIndeterminateFirstLineHeadAnimationSpec
    get() =
        infiniteRepeatable(
            animation =
                keyframes {
                    durationMillis = LinearAnimationDuration
                    0f at FirstLineHeadDelay using LinearIndeterminateProgressEasing
                    1f at FirstLineHeadDuration + FirstLineHeadDelay
                }
        )

/** An animation spec for indeterminate linear progress indicator first line tail position. */
internal val linearIndeterminateFirstLineTailAnimationSpec
    get() =
        infiniteRepeatable(
            animation =
                keyframes {
                    durationMillis = LinearAnimationDuration
                    0f at FirstLineTailDelay using LinearIndeterminateProgressEasing
                    1f at FirstLineTailDuration + FirstLineTailDelay
                }
        )

/** An animation spec for indeterminate linear progress indicator second line head position. */
internal val linearIndeterminateSecondLineHeadAnimationSpec
    get() =
        infiniteRepeatable(
            animation =
                keyframes {
                    durationMillis = LinearAnimationDuration
                    0f at SecondLineHeadDelay using LinearIndeterminateProgressEasing
                    1f at SecondLineHeadDuration + SecondLineHeadDelay
                }
        )

/** An animation spec for indeterminate linear progress indicator second line tail position. */
internal val linearIndeterminateSecondLineTailAnimationSpec
    get() =
        infiniteRepeatable(
            animation =
                keyframes {
                    durationMillis = LinearAnimationDuration
                    0f at SecondLineTailDelay using LinearIndeterminateProgressEasing
                    1f at SecondLineTailDuration + SecondLineTailDelay
                }
        )

internal val LinearIndicatorWidth = 360.dp

internal val LinearStrokeCap = StrokeCap.Round

// Indeterminate linear indicator transition specs

// Total duration for one linear cycle
internal const val LinearAnimationDuration = 1750

// Duration of the head and tail animations for both lines
internal const val FirstLineHeadDuration = 1000
internal const val FirstLineTailDuration = 1000
internal const val SecondLineHeadDuration = 850
internal const val SecondLineTailDuration = 850

// Delay before the start of the head and tail animations for both lines
internal const val FirstLineHeadDelay = 0
internal const val FirstLineTailDelay = 250
internal const val SecondLineHeadDelay = 650
internal const val SecondLineTailDelay = 900

internal val LinearIndeterminateProgressEasing = MotionTokens.EasingEmphasizedAccelerateCubicBezier
