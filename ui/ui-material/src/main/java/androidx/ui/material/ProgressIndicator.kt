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

package androidx.ui.material

import androidx.animation.CubicBezierEasing
import androidx.animation.FloatPropKey
import androidx.animation.Infinite
import androidx.animation.IntPropKey
import androidx.animation.LinearEasing
import androidx.animation.transitionDefinition
import androidx.annotation.FloatRange
import androidx.compose.Composable
import androidx.ui.animation.Transition
import androidx.ui.core.DensityAmbient
import androidx.ui.core.LayoutDirection
import androidx.ui.core.Modifier
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.Strings
import androidx.ui.foundation.determinateProgressIndicator
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Size
import androidx.ui.graphics.Color
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.drawscope.DrawScope
import androidx.ui.graphics.drawscope.Stroke
import androidx.ui.graphics.vectormath.degrees
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSize
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.accessibilityValue
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

/**
 * A determinate linear progress indicator that represents progress by drawing a horizontal line.
 *
 * @param progress The progress of this progress indicator, where 0.0 represents no progress and 1.0
 * represents full progress
 * @param color The color of the progress indicator.
 */
@Composable
fun LinearProgressIndicator(
    @FloatRange(from = 0.0, to = 1.0) progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary
) {
    val stroke = with(DensityAmbient.current) {
        Stroke(
            width = ProgressIndicatorConstants.DefaultStrokeWidth.toPx(),
            cap = StrokeCap.butt
        )
    }
    val backgroundColor = color.copy(alpha = BackgroundOpacity)
    Canvas(modifier.determinateProgressIndicator(progress)
        .preferredSize(LinearIndicatorWidth, LinearIndicatorHeight)
    ) {
        drawLinearIndicatorBackground(backgroundColor, stroke)
        drawLinearIndicator(0f, progress, color, stroke)
    }
}

/**
 * An indeterminate linear progress indicator that represents continual progress without a defined
 * start or end point.
 *
 * @param color The color of the progress indicator.
 */
@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary
) {
    // TODO(b/154875304) create IndeterminateProgressIndicator in foundation and move the
    //  semantics there
    Semantics(container = true, properties = { accessibilityValue = Strings.InProgress }) {
        Transition(
            definition = LinearIndeterminateTransition,
            initState = 0,
            toState = 1
        ) { state ->
            val firstLineHead = state[FirstLineHeadProp]
            val firstLineTail = state[FirstLineTailProp]
            val secondLineHead = state[SecondLineHeadProp]
            val secondLineTail = state[SecondLineTailProp]
            val backgroundColor = color.copy(alpha = BackgroundOpacity)
            val stroke = with(DensityAmbient.current) {
                Stroke(
                    width = ProgressIndicatorConstants.DefaultStrokeWidth.toPx(),
                    cap = StrokeCap.butt
                )
            }
            Canvas(modifier.preferredSize(LinearIndicatorWidth, LinearIndicatorHeight)) {
                drawLinearIndicatorBackground(backgroundColor, stroke)
                if (firstLineHead - firstLineTail > 0) {
                    drawLinearIndicator(
                        firstLineHead,
                        firstLineTail,
                        color,
                        stroke)
                }
                if ((secondLineHead - secondLineTail) > 0) {
                    drawLinearIndicator(
                        secondLineHead,
                        secondLineTail,
                        color,
                        stroke)
                }
            }
        }
    }
}

private fun DrawScope.drawLinearIndicator(
    startFraction: Float,
    endFraction: Float,
    color: Color,
    stroke: Stroke
) {
    val width = size.width
    val height = size.height
    // Start drawing from the vertical center of the stroke
    val yOffset = height / 2

    val isLtr = layoutDirection == LayoutDirection.Ltr
    val barStart = (if (isLtr) startFraction else 1f - endFraction) * width
    val barEnd = (if (isLtr) endFraction else 1f - startFraction) * width

    // Progress line
    drawLine(color, Offset(barStart, yOffset), Offset(barEnd, yOffset), stroke)
}

private fun DrawScope.drawLinearIndicatorBackground(
    color: Color,
    stroke: Stroke
) = drawLinearIndicator(0f, 1f, color, stroke)

/**
 * A determinate circular progress indicator that represents progress by drawing an arc ranging from
 * 0 to 360 degrees.
 *
 * @param progress The progress of this progress indicator, where 0.0 represents no progress and 1.0
 * represents full progress
 * @param color The color of the progress indicator.
 * @param strokeWidth The stroke width for the progress indicator.
 */
@Composable
fun CircularProgressIndicator(
    @FloatRange(from = 0.0, to = 1.0) progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    strokeWidth: Dp = ProgressIndicatorConstants.DefaultStrokeWidth
) {
    val stroke = with(DensityAmbient.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.butt)
    }
    Canvas(
        modifier
            .determinateProgressIndicator(progress)
            .padding(CircularIndicatorPadding)
            .preferredSize(CircularIndicatorDiameter)
    ) {
        // Start at 12 O'clock
        val startAngle = 270f
        val sweep = progress * 360f
        drawDeterminateCircularIndicator(startAngle, sweep, color, stroke)
    }
}

/**
 * An indeterminate circular progress indicator that represents continual progress without a defined
 * start or end point.
 *
 * @param color The color of the progress indicator.
 * @param strokeWidth The stroke width for the progress indicator.
 */
@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    strokeWidth: Dp = ProgressIndicatorConstants.DefaultStrokeWidth
) {
    // TODO(b/154875304) create IndeterminateProgressIndicator in foundation and move the
    //  semantics there
    Semantics(container = true, properties = { accessibilityValue = Strings.InProgress }) {
        val stroke = with(DensityAmbient.current) {
            Stroke(width = strokeWidth.toPx(), cap = StrokeCap.square)
        }
        Transition(
            definition = CircularIndeterminateTransition,
            initState = 0,
            toState = 1
        ) { state ->
            val currentRotation = state[IterationProp]
            val baseRotation = state[BaseRotationProp]

            val currentRotationAngleOffset = (currentRotation * RotationAngleOffset) % 360f

            var startAngle = state[TailRotationProp]
            val endAngle = state[HeadRotationProp]
            // How long a line to draw using the start angle as a reference point
            val sweep = abs(endAngle - startAngle)

            // Offset by the constant offset and the per rotation offset
            startAngle += StartAngleOffset + currentRotationAngleOffset
            startAngle += baseRotation

            Canvas(
                modifier.padding(CircularIndicatorPadding)
                    .preferredSize(CircularIndicatorDiameter)
            ) {
                drawIndeterminateCircularIndicator(startAngle, strokeWidth, sweep, color, stroke)
            }
        }
    }
}

private fun DrawScope.drawCircularIndicator(
    startAngle: Float,
    sweep: Float,
    color: Color,
    stroke: Stroke
) {
    // To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
    // To do this we need to remove half the stroke width from the total diameter for both sides.
    val diameterOffset = stroke.width / 2
    val arcDimen = size.width - 2 * diameterOffset
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(diameterOffset, diameterOffset),
        size = Size(arcDimen, arcDimen),
        style = stroke
    )
}

/**
 * Contains the default values used for [LinearProgressIndicator] and [CircularProgressIndicator].
 */
object ProgressIndicatorConstants {
    /**
     * Default stroke width for [CircularProgressIndicator], and default height for
     * [LinearProgressIndicator].
     *
     * This can be customized with the `strokeWidth` parameter on [CircularProgressIndicator],
     * and by passing a layout modifier setting the height for [LinearProgressIndicator].
     */
    val DefaultStrokeWidth = 4.dp
}

private fun DrawScope.drawDeterminateCircularIndicator(
    startAngle: Float,
    sweep: Float,
    color: Color,
    stroke: Stroke
) = drawCircularIndicator(startAngle, sweep, color, stroke)

private fun DrawScope.drawIndeterminateCircularIndicator(
    startAngle: Float,
    strokeWidth: Dp,
    sweep: Float,
    color: Color,
    stroke: Stroke
) {
    // Length of arc is angle * radius
    // Angle (radians) is length / radius
    // The length should be the same as the stroke width for calculating the min angle
    val squareStrokeCapOffset = degrees(strokeWidth / (CircularIndicatorDiameter / 2)) / 2

    // Adding a square stroke cap draws half the stroke width behind the start point, so we want to
    // move it forward by that amount so the arc visually appears in the correct place
    val adjustedStartAngle = startAngle + squareStrokeCapOffset

    // When the start and end angles are in the same place, we still want to draw a small sweep, so
    // the stroke caps get added on both ends and we draw the correct minimum length arc
    val adjustedSweep = max(sweep, 0.1f)

    drawCircularIndicator(adjustedStartAngle, adjustedSweep, color, stroke)
}

// LinearProgressIndicator Material specs
// TODO: there are currently 3 fixed widths in Android, should this be flexible? Material says
// the width should be 240dp here.
private val LinearIndicatorHeight = ProgressIndicatorConstants.DefaultStrokeWidth
private val LinearIndicatorWidth = 240.dp

// The opacity applied to the primary color to create the background color
private const val BackgroundOpacity = 0.24f

// CircularProgressIndicator Material specs
// Diameter of the indicator circle
private val CircularIndicatorDiameter = 40.dp
// We should reserve this amount on both sides of the indicator to allow space between components
private val CircularIndicatorPadding = 4.dp

// Indeterminate linear indicator transition specs
// Total duration for one cycle
private const val LinearAnimationDuration = 1800

// Fractional position of the 'head' and 'tail' of the two lines drawn. I.e if the head is 0.8 and
// the tail is 0.2, there is a line drawn from between 20% along to 80% along the total width
private val FirstLineHeadProp = FloatPropKey()
private val FirstLineTailProp = FloatPropKey()
private val SecondLineHeadProp = FloatPropKey()
private val SecondLineTailProp = FloatPropKey()

// Duration of the head and tail animations for both lines
private const val FirstLineHeadDuration = 750
private const val FirstLineTailDuration = 850
private const val SecondLineHeadDuration = 567
private const val SecondLineTailDuration = 533

// Delay before the start of the head and tail animations for both lines
private const val FirstLineHeadDelay = 0
private const val FirstLineTailDelay = 333
private const val SecondLineHeadDelay = 1000
private const val SecondLineTailDelay = 1267

private val FirstLineHeadEasing = CubicBezierEasing(0.2f, 0f, 0.8f, 1f)
private val FirstLineTailEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
private val SecondLineHeadEasing = CubicBezierEasing(0f, 0f, 0.65f, 1f)
private val SecondLineTailEasing = CubicBezierEasing(0.1f, 0f, 0.45f, 1f)

private val LinearIndeterminateTransition = transitionDefinition {
    state(0) {
        this[FirstLineHeadProp] = 0f
        this[FirstLineTailProp] = 0f
        this[SecondLineHeadProp] = 0f
        this[SecondLineTailProp] = 0f
    }

    state(1) {
        this[FirstLineHeadProp] = 1f
        this[FirstLineTailProp] = 1f
        this[SecondLineHeadProp] = 1f
        this[SecondLineTailProp] = 1f
    }

    transition(fromState = 0, toState = 1) {
        FirstLineHeadProp using repeatable {
            iterations = Infinite
            animation = keyframes {
                duration = LinearAnimationDuration
                0f at FirstLineHeadDelay with FirstLineHeadEasing
                1f at FirstLineHeadDuration + FirstLineHeadDelay
            }
        }
        FirstLineTailProp using repeatable {
            iterations = Infinite
            animation = keyframes {
                duration = LinearAnimationDuration
                0f at FirstLineTailDelay with FirstLineTailEasing
                1f at FirstLineTailDuration + FirstLineTailDelay
            }
        }
        SecondLineHeadProp using repeatable {
            iterations = Infinite
            animation = keyframes {
                duration = LinearAnimationDuration
                0f at SecondLineHeadDelay with SecondLineHeadEasing
                1f at SecondLineHeadDuration + SecondLineHeadDelay
            }
        }
        SecondLineTailProp using repeatable {
            iterations = Infinite
            animation = keyframes {
                duration = LinearAnimationDuration
                0f at SecondLineTailDelay with SecondLineTailEasing
                1f at SecondLineTailDuration + SecondLineTailDelay
            }
        }
    }
}

// Indeterminate circular indicator transition specs

// The animation comprises of 5 rotations around the circle forming a 5 pointed star.
// After the 5th rotation, we are back at the beginning of the circle.
private const val RotationsPerCycle = 5
// Each rotation is 1 and 1/3 seconds, but 1332ms divides more evenly
private const val RotationDuration = 1332

// When the rotation is at its beginning (0 or 360 degrees) we want it to be drawn at 12 o clock,
// which means 270 degrees when drawing.
private const val StartAngleOffset = -90f

// How far the base point moves around the circle
private const val BaseRotationAngle = 286f

// How far the head and tail should jump forward during one rotation past the base point
private const val JumpRotationAngle = 290f

// Each rotation we want to offset the start position by this much, so we continue where
// the previous rotation ended. This is the maximum angle covered during one rotation.
private const val RotationAngleOffset = (BaseRotationAngle + JumpRotationAngle) % 360f

// The head animates for the first half of a rotation, then is static for the second half
// The tail is static for the first half and then animates for the second half
private const val HeadAndTailAnimationDuration = (RotationDuration * 0.5).toInt()
private const val HeadAndTailDelayDuration = HeadAndTailAnimationDuration

// The current rotation around the circle, so we know where to start the rotation from
private val IterationProp = IntPropKey()
// How far forward (degrees) the base point should be from the start point
private val BaseRotationProp = FloatPropKey()
// How far forward (degrees) both the head and tail should be from the base point
private val HeadRotationProp = FloatPropKey()
private val TailRotationProp = FloatPropKey()

// The easing for the head and tail jump
private val CircularEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private val CircularIndeterminateTransition = transitionDefinition {
    state(0) {
        this[IterationProp] = 0
        this[BaseRotationProp] = 0f
        this[HeadRotationProp] = 0f
        this[TailRotationProp] = 0f
    }

    state(1) {
        this[IterationProp] = RotationsPerCycle
        this[BaseRotationProp] = BaseRotationAngle
        this[HeadRotationProp] = JumpRotationAngle
        this[TailRotationProp] = JumpRotationAngle
    }

    transition(fromState = 0, toState = 1) {
        IterationProp using repeatable {
            iterations = Infinite
            animation = tween {
                duration = RotationDuration * RotationsPerCycle
                easing = LinearEasing
            }
        }
        BaseRotationProp using repeatable {
            iterations = Infinite
            animation = tween {
                duration = RotationDuration
                easing = LinearEasing
            }
        }
        HeadRotationProp using repeatable {
            iterations = Infinite
            animation = keyframes {
                duration = HeadAndTailAnimationDuration + HeadAndTailDelayDuration
                0f at 0 with CircularEasing
                JumpRotationAngle at HeadAndTailAnimationDuration
            }
        }
        TailRotationProp using repeatable {
            iterations = Infinite
            animation = keyframes {
                duration = HeadAndTailAnimationDuration + HeadAndTailDelayDuration
                0f at HeadAndTailDelayDuration with CircularEasing
                JumpRotationAngle at duration
            }
        }
    }
}
