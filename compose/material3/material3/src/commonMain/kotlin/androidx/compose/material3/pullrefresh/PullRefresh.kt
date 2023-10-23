/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.pullrefresh

import androidx.annotation.FloatRange
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularIndicatorDiameter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.pullrefresh.PullRefreshDefaults.Indicator
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// TODO: Provide samples once PullRefreshState implementation lands
/**
 * Material Design pull refresh indicator
 *
 * @param state the state of this [PullRefreshContainer]
 * @param modifier the [Modifier] to be applied to this container
 * @param indicator The indicator placed inside of the [PullRefreshContainer]. Has access to [state]
 * @param shape the [Shape] of this container
 * @param containerColor the color of this container
 * @param contentColor the color of the progress indicator
 */
@Composable
@ExperimentalMaterial3Api
@Suppress("ComposableLambdaParameterPosition")
internal fun PullRefreshContainer(
    state: PullRefreshState,
    modifier: Modifier = Modifier,
    indicator: @Composable (PullRefreshState) -> Unit = { pullRefreshState ->
        Indicator(state = pullRefreshState)
    },
    shape: Shape = PullRefreshDefaults.shape,
    containerColor: Color = PullRefreshDefaults.containerColor,
    contentColor: Color = contentColorFor(backgroundColor = containerColor),
) {
    // Surface is not used here, as we do not want its input-blocking behaviour, since the indicator
    // is typically displayed above other (possibly) interactive indicator.
    val showElevation = remember {
        derivedStateOf { state.refreshing || state.verticalOffset > 0.5f }
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .size(CircularIndicatorDiameter)
                .graphicsLayer {
                    translationY = state.verticalOffset - size.height
                }
                .shadow(
                    // Avoid shadow when indicator is hidden
                    elevation = if (showElevation.value) Elevation else 0.dp,
                    shape = shape,
                    clip = true
                )
                .background(color = containerColor, shape = shape)
        ) {
            indicator(state)
        }
    }
}

/**
 * Contains the default values for [PullRefreshContainer]
 */
@ExperimentalMaterial3Api
internal object PullRefreshDefaults {

    /** The default shape for [PullRefreshContainer] */
    val shape: Shape = CircleShape

    /** The default container color for [PullRefreshContainer] */
    val containerColor: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

    /**
     * The default indicator for [PullRefreshContainer].
     */
    @Composable
    fun Indicator(
        state: PullRefreshState,
        modifier: Modifier = Modifier,
        color: Color = LocalContentColor.current,
    ) {
        Crossfade(
            targetState = state.refreshing,
            animationSpec = tween(durationMillis = CrossfadeDurationMs)
        ) { refreshing ->
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (refreshing) {
                    CircularProgressIndicator(
                        strokeWidth = StrokeWidth,
                        color = color,
                        modifier = Modifier.size(SpinnerSize),
                    )
                } else {
                    CircularArrowProgressIndicator(
                        progress = { state.progress },
                        color = color,
                        strokeWidth = StrokeWidth,
                    )
                }
            }
        }
    }
}

/**
 * The state that is associated with a [PullRefreshContainer].
 * Each instance of [PullRefreshContainer] should have its own [PullRefreshState].
 */
@Stable
@ExperimentalMaterial3Api
internal interface PullRefreshState {
    /** The threshold above which, if a release occurs, a refresh will be called */
    val positionalThreshold: Dp

    /**
     * PullRefresh progress towards [positionalThreshold]. 0.0 indicates no progress, 1.0 indicates
     * complete progress, > 1.0 indicates overshoot beyond the provided threshold
     */
    @get:FloatRange(from = 0.0)
    val progress: Float

    /**
     * Indicates whether a refresh is occurring
     */
    val refreshing: Boolean

    /**
     * The vertical offset (in pixels) for the [PullRefreshContainer] to consume
     */
    @get:FloatRange(from = 0.0)
    val verticalOffset: Float
}

/** The default pull indicator for [PullRefreshContainer] */
@Composable
private fun CircularArrowProgressIndicator(
    progress: () -> Float,
    color: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val path = remember { Path().apply { fillType = PathFillType.EvenOdd } }
    // TODO: Consider refactoring this sub-component utilizing Modifier.Node
    val targetAlpha by remember {
        derivedStateOf { if (progress() >= 1f) MaxAlpha else MinAlpha }
    }
    val alphaState = animateFloatAsState(targetValue = targetAlpha, animationSpec = AlphaTween)
    Canvas(
        modifier
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo =
                    ProgressBarRangeInfo(progress(), 0f..1f, 0)
            }
            .size(SpinnerSize)
    ) {
        val values = ArrowValues(progress())
        val alpha = alphaState.value
        rotate(degrees = values.rotation) {
            val arcRadius = ArcRadius.toPx() + strokeWidth.toPx() / 2f
            val arcBounds = Rect(center = size.center, radius = arcRadius)
            drawCircularIndicator(color, alpha, values, arcBounds, strokeWidth)
            drawArrow(path, arcBounds, color, alpha, values, strokeWidth)
        }
    }
}

private fun DrawScope.drawCircularIndicator(
    color: Color,
    alpha: Float,
    values: ArrowValues,
    arcBounds: Rect,
    strokeWidth: Dp
) {
    drawArc(
        color = color,
        alpha = alpha,
        startAngle = values.startAngle,
        sweepAngle = values.endAngle - values.startAngle,
        useCenter = false,
        topLeft = arcBounds.topLeft,
        size = arcBounds.size,
        style = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Square
        )
    )
}

@Immutable
private class ArrowValues(
    val rotation: Float,
    val startAngle: Float,
    val endAngle: Float,
    val scale: Float
)

private fun ArrowValues(progress: Float): ArrowValues {
    // Discard first 40% of progress. Scale remaining progress to full range between 0 and 100%.
    val adjustedPercent = max(min(1f, progress) - 0.4f, 0f) * 5 / 3
    // How far beyond the threshold pull has gone, as a percentage of the threshold.
    val overshootPercent = abs(progress) - 1.0f
    // Limit the overshoot to 200%. Linear between 0 and 200.
    val linearTension = overshootPercent.coerceIn(0f, 2f)
    // Non-linear tension. Increases with linearTension, but at a decreasing rate.
    val tensionPercent = linearTension - linearTension.pow(2) / 4

    // Calculations based on SwipeRefreshLayout specification.
    val endTrim = adjustedPercent * MaxProgressArc
    val rotation = (-0.25f + 0.4f * adjustedPercent + tensionPercent) * 0.5f
    val startAngle = rotation * 360
    val endAngle = (rotation + endTrim) * 360
    val scale = min(1f, adjustedPercent)

    return ArrowValues(rotation, startAngle, endAngle, scale)
}

private fun DrawScope.drawArrow(
    arrow: Path,
    bounds: Rect,
    color: Color,
    alpha: Float,
    values: ArrowValues,
    strokeWidth: Dp,
) {
    arrow.reset()
    arrow.moveTo(0f, 0f) // Move to left corner
    arrow.lineTo(x = ArrowWidth.toPx() * values.scale, y = 0f) // Line to right corner

    // Line to tip of arrow
    arrow.lineTo(
        x = ArrowWidth.toPx() * values.scale / 2,
        y = ArrowHeight.toPx() * values.scale
    )

    val radius = min(bounds.width, bounds.height) / 2f
    val inset = ArrowWidth.toPx() * values.scale / 2f
    arrow.translate(
        Offset(
            x = radius + bounds.center.x - inset,
            y = bounds.center.y + strokeWidth.toPx() / 2f
        )
    )
    arrow.close()
    rotate(degrees = values.endAngle) {
        drawPath(path = arrow, color = color, alpha = alpha)
    }
}

private const val MaxProgressArc = 0.8f
private const val CrossfadeDurationMs = MotionTokens.DurationShort2.toInt()

/** The default stroke width for [Indicator] */
private val StrokeWidth = 2.5.dp

private val ArcRadius = 7.5.dp
private val SpinnerSize = 20.dp // (ArcRadius + PullRefreshIndicatorDefaults.StrokeWidth).times(2)
private val Elevation = 6.dp
private val ArrowWidth = 10.dp
private val ArrowHeight = 5.dp

// Values taken from SwipeRefreshLayout
private const val MinAlpha = 0.3f
private const val MaxAlpha = 1f
private val AlphaTween = tween<Float>(MotionTokens.DurationMedium2.toInt(), easing = LinearEasing)
