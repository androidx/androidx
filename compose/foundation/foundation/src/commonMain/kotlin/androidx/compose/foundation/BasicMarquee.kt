/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.FixedMotionDurationScale.scaleFactor
import androidx.compose.foundation.MarqueeAnimationMode.Companion.Immediately
import androidx.compose.foundation.MarqueeAnimationMode.Companion.WhileFocused
import androidx.compose.foundation.MarqueeDefaults.Iterations
import androidx.compose.foundation.MarqueeDefaults.RepeatDelayMillis
import androidx.compose.foundation.MarqueeDefaults.Spacing
import androidx.compose.foundation.MarqueeDefaults.Velocity
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Namespace for constants representing the default values for various [basicMarquee] parameters.
 */
object MarqueeDefaults {
    /** Default value for the `iterations` parameter to [basicMarquee]. */
    // From
    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/TextView.java;l=736;drc=6d97d6d7215fef247d1a90e05545cac3676f9212
    @Suppress("MayBeConstant") val Iterations: Int = 3

    /** Default value for the `repeatDelayMillis` parameter to [basicMarquee]. */
    // From
    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/TextView.java;l=13979;drc=6d97d6d7215fef247d1a90e05545cac3676f9212
    @Suppress("MayBeConstant") val RepeatDelayMillis: Int = 1_200

    /** Default value for the `spacing` parameter to [basicMarquee]. */
    // From
    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/TextView.java;l=14088;drc=6d97d6d7215fef247d1a90e05545cac3676f9212
    val Spacing: MarqueeSpacing = MarqueeSpacing.fractionOfContainer(1f / 3f)

    /** Default value for the `velocity` parameter to [basicMarquee]. */
    // From
    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/TextView.java;l=13980;drc=6d97d6d7215fef247d1a90e05545cac3676f9212
    val Velocity: Dp = 30.dp
}

/**
 * Applies an animated marquee effect to the modified content if it's too wide to fit in the
 * available space. This modifier has no effect if the content fits in the max constraints. The
 * content will be measured with unbounded width.
 *
 * When the animation is running, it will restart from the initial state any time:
 * - any of the parameters to this modifier change, or
 * - the content or container size change.
 *
 * The animation only affects the drawing of the content, not its position. The offset returned by
 * the [LayoutCoordinates] of anything inside the marquee is undefined relative to anything outside
 * the marquee, and may not match its drawn position on screen. This modifier also does not
 * currently support content that accepts position-based input such as pointer events.
 *
 * To only animate when the composable is focused, specify [animationMode] and make the composable
 * focusable. This modifier does not add any visual effects aside from scrolling, but you can add
 * your own by placing modifiers before this one.
 *
 * @sample androidx.compose.foundation.samples.BasicMarqueeSample
 * @sample androidx.compose.foundation.samples.BasicMarqueeWithFadedEdgesSample
 * @sample androidx.compose.foundation.samples.BasicFocusableMarqueeSample
 * @param iterations The number of times to repeat the animation. `Int.MAX_VALUE` will repeat
 *   forever, and 0 will disable animation.
 * @param animationMode Whether the marquee should start animating [Immediately] or only
 *   [WhileFocused]. In [WhileFocused] mode, the modified node or the content must be made
 *   [focusable]. Note that the [initialDelayMillis] is part of the animation, so this parameter
 *   determines when that initial delay starts counting down, not when the content starts to
 *   actually scroll.
 * @param repeatDelayMillis The duration to wait before starting each subsequent iteration, in
 *   millis.
 * @param initialDelayMillis The duration to wait before starting the first iteration of the
 *   animation, in millis. By default, there will be no initial delay if [animationMode] is
 *   [WhileFocused], otherwise the initial delay will be [repeatDelayMillis].
 * @param spacing A [MarqueeSpacing] that specifies how much space to leave at the end of the
 *   content before showing the beginning again.
 * @param velocity The speed of the animation in dps / second.
 */
@Stable
fun Modifier.basicMarquee(
    iterations: Int = Iterations,
    animationMode: MarqueeAnimationMode = Immediately,
    // TODO(aosp/2339066) Consider taking an AnimationSpec instead of specific configuration params.
    repeatDelayMillis: Int = RepeatDelayMillis,
    initialDelayMillis: Int = if (animationMode == Immediately) repeatDelayMillis else 0,
    spacing: MarqueeSpacing = Spacing,
    velocity: Dp = Velocity
): Modifier =
    this then
        MarqueeModifierElement(
            iterations = iterations,
            animationMode = animationMode,
            delayMillis = repeatDelayMillis,
            initialDelayMillis = initialDelayMillis,
            spacing = spacing,
            velocity = velocity,
        )

private data class MarqueeModifierElement(
    private val iterations: Int,
    private val animationMode: MarqueeAnimationMode,
    private val delayMillis: Int,
    private val initialDelayMillis: Int,
    private val spacing: MarqueeSpacing,
    private val velocity: Dp,
) : ModifierNodeElement<MarqueeModifierNode>() {
    override fun create(): MarqueeModifierNode =
        MarqueeModifierNode(
            iterations = iterations,
            animationMode = animationMode,
            delayMillis = delayMillis,
            initialDelayMillis = initialDelayMillis,
            spacing = spacing,
            velocity = velocity,
        )

    override fun update(node: MarqueeModifierNode) {
        node.update(
            iterations = iterations,
            animationMode = animationMode,
            delayMillis = delayMillis,
            initialDelayMillis = initialDelayMillis,
            spacing = spacing,
            velocity = velocity,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "basicMarquee"
        properties["iterations"] = iterations
        properties["animationMode"] = animationMode
        properties["delayMillis"] = delayMillis
        properties["initialDelayMillis"] = initialDelayMillis
        properties["spacing"] = spacing
        properties["velocity"] = velocity
    }
}

private class MarqueeModifierNode(
    private var iterations: Int,
    animationMode: MarqueeAnimationMode,
    private var delayMillis: Int,
    private var initialDelayMillis: Int,
    spacing: MarqueeSpacing,
    private var velocity: Dp,
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode, FocusEventModifierNode {

    private var contentWidth by mutableIntStateOf(0)
    private var containerWidth by mutableIntStateOf(0)
    private var hasFocus by mutableStateOf(false)
    private var animationJob: Job? = null
    private var marqueeLayer: GraphicsLayer? = null
    var spacing: MarqueeSpacing by mutableStateOf(spacing)
    var animationMode: MarqueeAnimationMode by mutableStateOf(animationMode)

    private val offset = Animatable(0f)
    private val direction
        get() =
            sign(velocity.value) *
                when (requireLayoutDirection()) {
                    LayoutDirection.Ltr -> 1
                    LayoutDirection.Rtl -> -1
                }

    private val spacingPx by derivedStateOf {
        with(spacing) { requireDensity().calculateSpacing(contentWidth, containerWidth) }
    }

    override fun onAttach() {
        val layer = marqueeLayer
        val graphicsContext = requireGraphicsContext()
        // Shouldn't happen as detach should be called in between in onAttach call but
        // just in case
        if (layer != null) {
            graphicsContext.releaseGraphicsLayer(layer)
        }

        marqueeLayer = graphicsContext.createGraphicsLayer()
        restartAnimation()
    }

    override fun onDetach() {
        animationJob?.cancel()
        animationJob = null

        val layer = marqueeLayer
        if (layer != null) {
            requireGraphicsContext().releaseGraphicsLayer(layer)
            marqueeLayer = null
        }
    }

    fun update(
        iterations: Int,
        animationMode: MarqueeAnimationMode,
        delayMillis: Int,
        initialDelayMillis: Int,
        spacing: MarqueeSpacing,
        velocity: Dp,
    ) {
        this.spacing = spacing
        this.animationMode = animationMode

        if (
            this.iterations != iterations ||
                this.delayMillis != delayMillis ||
                this.initialDelayMillis != initialDelayMillis ||
                this.velocity != velocity
        ) {
            this.iterations = iterations
            this.delayMillis = delayMillis
            this.initialDelayMillis = initialDelayMillis
            this.velocity = velocity
            restartAnimation()
        }
    }

    override fun onFocusEvent(focusState: FocusState) {
        hasFocus = focusState.hasFocus
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val childConstraints = constraints.copy(maxWidth = Constraints.Infinity)
        val placeable = measurable.measure(childConstraints)
        containerWidth = constraints.constrainWidth(placeable.width)
        contentWidth = placeable.width
        return layout(containerWidth, placeable.height) {
            // Placing the marquee content in a layer means we don't invalidate the parent draw
            // scope on every animation frame.
            placeable.placeWithLayer(x = (-offset.value * direction).roundToInt(), y = 0)
        }
    }

    // Override intrinsic calculations to avoid setting state (see b/278729564).

    /** Always returns zero since the marquee has no minimum width. */
    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = measurable.maxIntrinsicWidth(height)

    /** Ignores width since marquee contents are always measured with infinite width. */
    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = measurable.minIntrinsicHeight(Constraints.Infinity)

    /** Ignores width since marquee contents are always measured with infinite width. */
    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = measurable.maxIntrinsicHeight(Constraints.Infinity)

    override fun ContentDrawScope.draw() {
        val clipOffset = offset.value * direction
        val firstCopyVisible =
            when (direction) {
                1f -> offset.value < contentWidth
                else -> offset.value < containerWidth
            }
        val secondCopyVisible =
            when (direction) {
                1f -> offset.value > (contentWidth + spacingPx) - containerWidth
                else -> offset.value > spacingPx
            }
        val secondCopyOffset =
            when (direction) {
                1f -> contentWidth + spacingPx
                else -> -contentWidth - spacingPx
            }.toFloat()

        val drawHeight = size.height
        marqueeLayer?.let { layer ->
            layer.record(size = IntSize(contentWidth, drawHeight.roundToInt())) {
                this@draw.drawContent()
            }
        }
        clipRect(left = clipOffset, right = clipOffset + containerWidth) {
            val layer = marqueeLayer
            // Unless there are circumstances where the Modifier's draw call can be invoked without
            // an attach call, the else case here is optional. However we can be safe and make sure
            // that we definitely draw even when the layer could not be initialized for any reason.
            if (layer != null) {
                if (firstCopyVisible) {
                    drawLayer(layer)
                }
                if (secondCopyVisible) {
                    translate(left = secondCopyOffset) { drawLayer(layer) }
                }
            } else {
                if (firstCopyVisible) {
                    this@draw.drawContent()
                }
                if (secondCopyVisible) {
                    translate(left = secondCopyOffset) { this@draw.drawContent() }
                }
            }
        }
    }

    private fun restartAnimation() {
        val oldJob = animationJob
        oldJob?.cancel()
        if (isAttached) {
            animationJob =
                coroutineScope.launch {
                    // Wait for the cancellation to finish.
                    oldJob?.join()
                    runAnimation()
                }
        }
    }

    private suspend fun runAnimation() {
        if (iterations <= 0) {
            // No animation.
            return
        }

        // Marquee animations should not be affected by motion accessibility settings.
        // Wrap the entire flow instead of just the animation calls so kotlin doesn't have to create
        // an extra CoroutineContext every time the flow emits.
        withContext(FixedMotionDurationScale) {
            snapshotFlow {
                    // Don't animate if content fits. (Because coroutines, the int will get boxed
                    // anyway.)
                    if (contentWidth <= containerWidth) return@snapshotFlow null
                    if (animationMode == WhileFocused && !hasFocus) return@snapshotFlow null
                    (contentWidth + spacingPx).toFloat()
                }
                .collectLatest { contentWithSpacingWidth ->
                    // Don't animate when the content fits.
                    if (contentWithSpacingWidth == null) return@collectLatest

                    val spec =
                        createMarqueeAnimationSpec(
                            iterations,
                            contentWithSpacingWidth,
                            initialDelayMillis,
                            delayMillis,
                            velocity,
                            requireDensity()
                        )

                    offset.snapTo(0f)
                    try {
                        offset.animateTo(contentWithSpacingWidth, spec)
                    } finally {
                        // This needs to be in a finally so the offset is reset if the animation is
                        // cancelled when losing focus in WhileFocused mode.
                        offset.snapTo(0f)
                    }
                }
        }
    }
}

private fun createMarqueeAnimationSpec(
    iterations: Int,
    targetValue: Float,
    initialDelayMillis: Int,
    delayMillis: Int,
    velocity: Dp,
    density: Density
): AnimationSpec<Float> {
    val pxPerSec = with(density) { velocity.toPx() }
    val singleSpec =
        velocityBasedTween(
            velocity = pxPerSec.absoluteValue,
            targetValue = targetValue,
            delayMillis = delayMillis
        )
    // Need to cancel out the non-initial delay.
    val startOffset = StartOffset(-delayMillis + initialDelayMillis)
    return if (iterations == Int.MAX_VALUE) {
        infiniteRepeatable(singleSpec, initialStartOffset = startOffset)
    } else {
        repeatable(iterations, singleSpec, initialStartOffset = startOffset)
    }
}

/**
 * Calculates a float [TweenSpec] that moves at a constant [velocity] for an animation from 0 to
 * [targetValue].
 *
 * @param velocity Speed of animation in px / sec.
 */
private fun velocityBasedTween(
    velocity: Float,
    targetValue: Float,
    delayMillis: Int
): TweenSpec<Float> {
    val pxPerMilli = velocity / 1000f
    return tween(
        durationMillis = ceil(targetValue / pxPerMilli).toInt(),
        easing = LinearEasing,
        delayMillis = delayMillis
    )
}

/** Specifies when the [basicMarquee] animation runs. */
@JvmInline
value class MarqueeAnimationMode private constructor(private val value: Int) {

    override fun toString(): String =
        when (this) {
            Immediately -> "Immediately"
            WhileFocused -> "WhileFocused"
            else -> error("invalid value: $value")
        }

    companion object {
        /**
         * Starts animating immediately (accounting for any initial delay), irrespective of focus
         * state.
         */
        val Immediately = MarqueeAnimationMode(0)

        /**
         * Only animates while the marquee has focus or a node in the marquee's content has focus.
         */
        val WhileFocused = MarqueeAnimationMode(1)
    }
}

/** A [MarqueeSpacing] with a fixed size. */
fun MarqueeSpacing(spacing: Dp): MarqueeSpacing = MarqueeSpacing { _, _ -> spacing.roundToPx() }

/**
 * Defines a [calculateSpacing] method that determines the space after the end of [basicMarquee]
 * content before drawing the content again.
 */
@Stable
fun interface MarqueeSpacing {
    /**
     * Calculates the space after the end of [basicMarquee] content before drawing the content
     * again.
     *
     * This is a restartable method: any state used to calculate the result will cause the spacing
     * to be re-calculated when it changes.
     *
     * @param contentWidth The width of the content inside the marquee, in pixels. Will always be
     *   larger than [containerWidth].
     * @param containerWidth The width of the marquee itself, in pixels. Will always be smaller than
     *   [contentWidth].
     * @return The space in pixels between the end of the content and the beginning of the content
     *   when wrapping.
     */
    fun Density.calculateSpacing(contentWidth: Int, containerWidth: Int): Int

    companion object {
        /** A [MarqueeSpacing] that is a fraction of the container's width. */
        fun fractionOfContainer(fraction: Float): MarqueeSpacing = MarqueeSpacing { _, width ->
            (fraction * width).roundToInt()
        }
    }
}

/** A [MotionDurationScale] that always reports a [scaleFactor] of 1. */
private object FixedMotionDurationScale : MotionDurationScale {
    override val scaleFactor: Float
        get() = 1f
}
