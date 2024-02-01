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

package androidx.compose.material3.pulltorefresh

import androidx.annotation.FloatRange
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.tokens.ElevationTokens
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// TODO: Link to Material design specs when available.
/**
 * Material Design pull-to-refresh indicator
 *
 * A pull-to-refresh container contains a progress indicator to indicate a users drag progress
 * towards triggering a refresh. On a refresh the progress indicator inside this container is
 * indeterminate.
 *
 * @sample androidx.compose.material3.samples.PullToRefreshSample
 *
 * A custom state implementation can be initialized like this
 * @sample androidx.compose.material3.samples.PullToRefreshSampleCustomState
 *
 * Scaling behavior can be implemented like this
 * @sample androidx.compose.material3.samples.PullToRefreshScalingSample
 *
 * @param state the state of this [PullToRefreshContainer]
 * @param modifier the [Modifier] to be applied to this container
 * @param indicator The indicator placed inside of the [PullToRefreshContainer]. Has access to
 * [state]
 * @param shape the [Shape] of this container
 * @param containerColor the color of this container
 * @param contentColor the color of the progress indicator
 */
@Composable
@ExperimentalMaterial3Api
@Suppress("ComposableLambdaParameterPosition")
fun PullToRefreshContainer(
    state: PullToRefreshState,
    modifier: Modifier = Modifier,
    indicator: @Composable (PullToRefreshState) -> Unit = { pullRefreshState ->
        Indicator(state = pullRefreshState)
    },
    shape: Shape = PullToRefreshDefaults.shape,
    containerColor: Color = PullToRefreshDefaults.containerColor,
    contentColor: Color = PullToRefreshDefaults.contentColor,
) {
    // Surface is not used here, as we do not want its input-blocking behaviour, since the indicator
    // is typically displayed above other (possibly) interactive indicator.
    val showElevation = remember {
        derivedStateOf { state.verticalOffset > 1f || state.isRefreshing }
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .size(SpinnerContainerSize)
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
 * Contains the default values for [PullToRefreshContainer]
 */
@ExperimentalMaterial3Api
object PullToRefreshDefaults {
    /** The default shape for [PullToRefreshContainer] */
    val shape: Shape = CircleShape

    /** The default container color for [PullToRefreshContainer] */
    val containerColor: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

    /** The default container color for [PullToRefreshContainer] */
    val contentColor: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

    /** The default refresh threshold for [rememberPullToRefreshState] */
    val PositionalThreshold = 80.dp

    /**
     * The default indicator for [PullToRefreshContainer].
     */
    @Composable
    fun Indicator(
        state: PullToRefreshState,
        modifier: Modifier = Modifier,
        color: Color = LocalContentColor.current,
    ) {
        Crossfade(
            targetState = state.isRefreshing,
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
                    )
                }
            }
        }
    }
}

/**
 * The state that is associated with a [PullToRefreshContainer].
 * Each instance of [PullToRefreshContainer] should have its own [PullToRefreshState].
 *
 * [PullToRefreshState] can be used with other progress indicators like so:
 * @sample androidx.compose.material3.samples.PullToRefreshLinearProgressIndicatorSample
 */
@Stable
@ExperimentalMaterial3Api
interface PullToRefreshState {
    /** The threshold (in pixels), above which if a release occurs, a refresh will be called */
    val positionalThreshold: Float

    /**
     * PullRefresh progress towards [positionalThreshold]. 0.0 indicates no progress, 1.0 indicates
     * complete progress, > 1.0 indicates overshoot beyond the provided threshold
     */
    @get:FloatRange(from = 0.0)
    val progress: Float

    /**
     * Indicates whether a refresh is occurring.
     */
    val isRefreshing: Boolean

    /**
     * Sets [isRefreshing] to true.
     */
    fun startRefresh()

    /**
     * Sets [isRefreshing] to false.
     */
    fun endRefresh()

    /**
     * The vertical offset (in pixels) for the [PullToRefreshContainer] to consume
     */
    @get:FloatRange(from = 0.0)
    val verticalOffset: Float

    /**
     * A [NestedScrollConnection] that should be attached to a [Modifier.nestedScroll] in order to
     * keep track of the scroll events.
     */
    var nestedScrollConnection: NestedScrollConnection
}

/**
 * Create and remember the default [PullToRefreshState].
 *
 * @param positionalThreshold The positional threshold when a refresh would be triggered
 * @param enabled a callback used to determine whether scroll events are to be handled by this
 * [PullToRefreshState]
 */
@Composable
@ExperimentalMaterial3Api
fun rememberPullToRefreshState(
    positionalThreshold: Dp = PullToRefreshDefaults.PositionalThreshold,
    enabled: () -> Boolean = { true },
): PullToRefreshState {
    val density = LocalDensity.current
    val positionalThresholdPx = with(density) { positionalThreshold.toPx() }
    return rememberSaveable(
        positionalThresholdPx, enabled,
        saver = PullToRefreshStateImpl.Saver(
            positionalThreshold = positionalThresholdPx,
            enabled = enabled,
        )
    ) {
        PullToRefreshStateImpl(
            initialRefreshing = false,
            positionalThreshold = positionalThresholdPx,
            enabled = enabled,
        )
    }
}

/**
 * Creates a [PullToRefreshState].
 *
 * Note that in most cases, you are advised to use [rememberPullToRefreshState] when in composition.
 *
 * @param positionalThresholdPx The positional threshold, in pixels, in which a refresh is triggered
 * @param initialRefreshing The initial refreshing value of [PullToRefreshState]
 * @param enabled a callback used to determine whether scroll events are to be handled by this
 * [PullToRefreshState]
 */
@ExperimentalMaterial3Api
fun PullToRefreshState(
    positionalThresholdPx: Float,
    initialRefreshing: Boolean = false,
    enabled: () -> Boolean = { true },
): PullToRefreshState = PullToRefreshStateImpl(
    initialRefreshing = initialRefreshing,
    positionalThreshold = positionalThresholdPx,
    enabled = enabled,
)

@ExperimentalMaterial3Api
internal class PullToRefreshStateImpl(
    initialRefreshing: Boolean,
    override val positionalThreshold: Float,
    enabled: () -> Boolean,
) : PullToRefreshState {

    override val progress get() = adjustedDistancePulled / positionalThreshold
    override val verticalOffset get() = _verticalOffset

    override val isRefreshing get() = _refreshing

    override fun startRefresh() {
        _refreshing = true
        _verticalOffset = positionalThreshold
    }

    override fun endRefresh() {
        _verticalOffset = 0f
        _refreshing = false
    }

    override var nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource,
        ): Offset = when {
            !enabled() -> Offset.Zero
            // Swiping up
            source == NestedScrollSource.Drag && available.y < 0 -> {
                consumeAvailableOffset(available)
            }
            else -> Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset = when {
            !enabled() -> Offset.Zero
            // Swiping down
            source == NestedScrollSource.Drag && available.y > 0 -> {
                consumeAvailableOffset(available)
            }
            else -> Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            return Velocity(0f, onRelease(available.y))
        }
    }

    /** Helper method for nested scroll connection */
    fun consumeAvailableOffset(available: Offset): Offset {
        val y = if (isRefreshing) 0f else {
            val newOffset = (distancePulled + available.y).coerceAtLeast(0f)
            val dragConsumed = newOffset - distancePulled
            distancePulled = newOffset
            _verticalOffset = calculateVerticalOffset()
            dragConsumed
        }
        return Offset(0f, y)
    }

    /** Helper method for nested scroll connection. Calls onRefresh callback when triggered */
    suspend fun onRelease(velocity: Float): Float {
        if (isRefreshing) return 0f // Already refreshing, do nothing
        // Trigger refresh
        if (adjustedDistancePulled > positionalThreshold) {
            startRefresh()
        } else {
            animateTo(0f)
        }

        val consumed = when {
            // We are flinging without having dragged the pull refresh (for example a fling inside
            // a list) - don't consume
            distancePulled == 0f -> 0f
            // If the velocity is negative, the fling is upwards, and we don't want to prevent the
            // the list from scrolling
            velocity < 0f -> 0f
            // We are showing the indicator, and the fling is downwards - consume everything
            else -> velocity
        }
        distancePulled = 0f
        return consumed
    }

    suspend fun animateTo(offset: Float) {
        animate(initialValue = _verticalOffset, targetValue = offset) { value, _ ->
            _verticalOffset = value
        }
    }

    /** Provides custom vertical offset behavior for [PullToRefreshContainer] */
    fun calculateVerticalOffset(): Float = when {
        // If drag hasn't gone past the threshold, the position is the adjustedDistancePulled.
        adjustedDistancePulled <= positionalThreshold -> adjustedDistancePulled
        else -> {
            // How far beyond the threshold pull has gone, as a percentage of the threshold.
            val overshootPercent = abs(progress) - 1.0f
            // Limit the overshoot to 200%. Linear between 0 and 200.
            val linearTension = overshootPercent.coerceIn(0f, 2f)
            // Non-linear tension. Increases with linearTension, but at a decreasing rate.
            val tensionPercent = linearTension - linearTension.pow(2) / 4
            // The additional offset beyond the threshold.
            val extraOffset = positionalThreshold * tensionPercent
            positionalThreshold + extraOffset
        }
    }

    companion object {
        /** The default [Saver] for [PullToRefreshStateImpl]. */
        fun Saver(
            positionalThreshold: Float,
            enabled: () -> Boolean,
        ) = Saver<PullToRefreshState, Boolean>(
            save = { it.isRefreshing },
            restore = { isRefreshing ->
                PullToRefreshStateImpl(isRefreshing, positionalThreshold, enabled)
            }
        )
    }

    internal var distancePulled by mutableFloatStateOf(0f)
    private val adjustedDistancePulled: Float get() = distancePulled * DragMultiplier
    private var _verticalOffset by mutableFloatStateOf(0f)
    private var _refreshing by mutableStateOf(initialRefreshing)
}

/** The default pull indicator for [PullToRefreshContainer] */
@Composable
private fun CircularArrowProgressIndicator(
    progress: () -> Float,
    color: Color,
) {
    val path = remember { Path().apply { fillType = PathFillType.EvenOdd } }
    // TODO: Consider refactoring this sub-component utilizing Modifier.Node
    val targetAlpha by remember {
        derivedStateOf { if (progress() >= 1f) MaxAlpha else MinAlpha }
    }
    val alphaState = animateFloatAsState(targetValue = targetAlpha, animationSpec = AlphaTween)
    Canvas(
        Modifier
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo =
                    ProgressBarRangeInfo(progress(), 0f..1f, 0)
            }
            .size(SpinnerSize)
    ) {
        val values = ArrowValues(progress())
        val alpha = alphaState.value
        rotate(degrees = values.rotation) {
            val arcRadius = ArcRadius.toPx() + StrokeWidth.toPx() / 2f
            val arcBounds = Rect(center = size.center, radius = arcRadius)
            drawCircularIndicator(color, alpha, values, arcBounds, StrokeWidth)
            drawArrow(path, arcBounds, color, alpha, values, StrokeWidth)
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
            cap = StrokeCap.Butt
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
    // Line to tip of arrow
    arrow.lineTo(
        x = ArrowWidth.toPx() * values.scale / 2,
        y = ArrowHeight.toPx() * values.scale
    )
    arrow.lineTo(x = ArrowWidth.toPx() * values.scale, y = 0f) // Line to right corner

    val radius = min(bounds.width, bounds.height) / 2f
    val inset = ArrowWidth.toPx() * values.scale / 2f
    arrow.translate(
        Offset(
            x = radius + bounds.center.x - inset,
            y = bounds.center.y - strokeWidth.toPx()
        )
    )
    rotate(degrees = values.endAngle - strokeWidth.toPx()) {
        drawPath(path = arrow, color = color, alpha = alpha, style = Stroke(strokeWidth.toPx()))
    }
}

private const val MaxProgressArc = 0.8f
private const val CrossfadeDurationMs = MotionTokens.DurationShort2.toInt()

/** The default stroke width for [Indicator] */
private val StrokeWidth = 2.5.dp
private val ArcRadius = 5.5.dp
internal val SpinnerSize = 16.dp // (ArcRadius + PullRefreshIndicatorDefaults.StrokeWidth).times(2)
internal val SpinnerContainerSize = 40.dp
private val Elevation = ElevationTokens.Level2
private val ArrowWidth = 10.dp
private val ArrowHeight = 5.dp

// Values taken from SwipeRefreshLayout
private const val MinAlpha = 0.3f
private const val MaxAlpha = 1f
private val AlphaTween = tween<Float>(MotionTokens.DurationMedium2.toInt(), easing = LinearEasing)

/**
 * The distance pulled is multiplied by this value to give us the adjusted distance pulled, which
 * is used in calculating the indicator position (when the adjusted distance pulled is less than
 * the refresh threshold, it is the indicator position, otherwise the indicator position is
 * derived from the progress).
 */
private const val DragMultiplier = 0.5f
