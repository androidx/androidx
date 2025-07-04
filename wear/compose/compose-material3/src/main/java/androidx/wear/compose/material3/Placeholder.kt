/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.material3.tokens.MotionTokens
import androidx.wear.compose.material3.tokens.ShapeTokens
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

/**
 * Modifier.placeholder draws a skeleton shape over a component, for situations when no provisional
 * content (such as cached data) is available. The placeholder skeleton can be displayed instead,
 * while the content is loading. The reveal of the content will be animated when it becomes
 * available (and hidden again if the content becomes unavailable), unless the ReducedMotion setting
 * is enabled, in which case those are instantaneous. NOTE: For animations to work, an [AppScaffold]
 * should be used.
 *
 * @sample androidx.wear.compose.material3.samples.ButtonWithIconAndLabelAndPlaceholders
 *
 * If there is some cached data for this field, it may be better to show that while loading, see
 *
 * @sample androidx.wear.compose.material3.samples.ButtonWithIconAndLabelCachedData
 *
 * Note that the component should still be sized close to the target, so the final reveal of the
 * content is less disruptive.
 *
 * @param placeholderState the state used to coordinate several placeholder effects.
 * @param shape the shape of the placeholder.
 * @param color the color to use in the placeholder.
 */
@Composable
public fun Modifier.placeholder(
    placeholderState: PlaceholderState,
    shape: Shape = PlaceholderDefaults.shape,
    color: Color = PlaceholderDefaults.color,
): Modifier {
    DisposableEffect(Unit) {
        placeholderState.register()
        onDispose { placeholderState.unregister() }
    }

    val reduceMotion = LocalReduceMotion.current
    return this.then(
            Modifier.drawWithContent {
                val outlineAlpha =
                    if (reduceMotion) 1f
                    else if (placeholderState.isVisible)
                        placeholderState.resetPlaceholderFadeInAlpha
                    else 1f - placeholderState.placeholderWipeOffAlpha

                if (
                    placeholderState.isVisible ||
                        !reduceMotion && placeholderState.isAnimationRunning
                ) {
                    if (shape === RectangleShape) {
                        // shortcut to avoid Outline calculation and allocation
                        drawRect(color, alpha = outlineAlpha)
                    } else {
                        val outline = shape.createOutline(size, layoutDirection, this)
                        drawOutline(outline, color, alpha = outlineAlpha)
                    }
                }
                drawContent()
            }
        )
        .then(
            if (reduceMotion) {
                Modifier
            } else {
                Modifier.graphicsLayer {
                    alpha =
                        if (placeholderState.isVisible)
                            placeholderState.resetPlaceholderFadeOutAlpha
                        else placeholderState.placeholderWipeOffAlpha
                }
            }
        )
}

/**
 * Modifier.placeholderShimmer draws a periodic shimmer over content, indicating to the user that
 * contents are loading or potentially out of date. The placeholder shimmer is a 45 degree gradient
 * from Top|Left of the screen to Bottom|Right. The shimmer is coordinated via the animation frame
 * clock which orchestrates the shimmer so that every component will shimmer as the gradient
 * progresses across the screen. NOTE: For animations to work, an [AppScaffold] should be used.
 *
 * Example of a [Button] with icon and a label that put placeholders over individual content slots
 * and then draws a placeholder shimmer over the result:
 *
 * @sample androidx.wear.compose.material3.samples.ButtonWithIconAndLabelAndPlaceholders
 *
 * Example of a simple text placeholder:
 *
 * @sample androidx.wear.compose.material3.samples.TextPlaceholder
 * @param placeholderState the current placeholder state that determine whether the placeholder
 *   shimmer should be shown.
 * @param shape the shape of the component.
 * @param color the color to use in the shimmer.
 */
@Composable
public fun Modifier.placeholderShimmer(
    placeholderState: PlaceholderState,
    shape: Shape = PlaceholderDefaults.shape,
    color: Color = PlaceholderDefaults.shimmerColor,
): Modifier =
    this.then(
        if (LocalReduceMotion.current) {
            Modifier
        } else {
            PlaceholderShimmerElement(
                placeholderState = placeholderState,
                color = color,
                shape = shape,
                inspectorInfo =
                    debugInspectorInfo {
                        name = "placeholderShimmer"
                        properties["placeholderState"] = placeholderState
                        properties["shape"] = shape
                        properties["color"] = color
                    },
            )
        }
    )

/**
 * Creates a [PlaceholderState] that is remembered across compositions.
 *
 * A [PlaceholderState] should be created for each component that has placeholder data, such as a
 * [Card] or a [Button]. The state is used to coordinate all of the different placeholder effects
 * and animations.
 *
 * Placeholder has a number of different effects designed to work together.
 * [Modifier.placeholderShimmer] does a shimmer animation over the whole component that includes the
 * placeholders. There should only be one placeholderShimmer for each component.
 * [Modifier.placeholder] draws a placeholder shape on top of content that is waiting to load. There
 * can be multiple placeholders in a component. For example, one for the title of an [AppCard],
 * another for the app name, and so on.
 *
 * Once all of the components content is loaded, [isVisible] is `false` the shimmer will stop and a
 * wipe off animation will remove the placeholders to reveal the content.
 *
 * @param isVisible the initial state of the placeholder.
 */
@Composable
public fun rememberPlaceholderState(isVisible: Boolean): PlaceholderState {
    return remember { PlaceholderState(isVisible) }
        .also {
            // Update the state when the Composition completes successfully.
            SideEffect { it.isVisible = isVisible }
        }
}

/**
 * Contains the default values used for providing placeholders.
 *
 * There are two distinct but coordinated aspects to placeholders in Compose for Wear OS. Firstly
 * [Modifier.placeholder] which is drawn instead of content that is not yet loaded. Secondly a
 * placeholder shimmer effect [Modifier.placeholderShimmer] effect which runs in an animation loop
 * while waiting for the data to load. Note that besides placeholders, cached content may also be
 * shown while waiting for the data to refresh.
 */
public object PlaceholderDefaults {
    /** Default [Shape] for [Modifier.placeholder] and [Modifier.placeholderShimmer]. */
    public val shape: Shape
        @Composable get() = ShapeTokens.CornerFull

    /** Default [Color] for [Modifier.placeholder]. */
    public val color: Color
        @Composable
        get() =
            MaterialTheme.colorScheme.onSurface
                .copy(alpha = 0.1f)
                .compositeOver(MaterialTheme.colorScheme.surfaceContainer)

    /** Default [Color] for [Modifier.placeholderShimmer]. */
    public val shimmerColor: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
}

/**
 * A state object that can be used to control placeholders. Placeholders are used when the content
 * that needs to be displayed in a component is not yet available, e.g. it is loading
 * asynchronously.
 *
 * A [PlaceholderState] should be created for each component that has placeholder data. The state is
 * used to coordinate all of the different placeholder effects and animations. The state should be
 * created and remembered (maybe using [rememberPlaceholderState]), and as needed the [isVisible]
 * property should be updated to show/hide the placeholder.
 *
 * Placeholder has a number of different effects designed to work together. [Modifier.placeholder]
 * draws a placeholder shape on top of content that is waiting to load. There can be multiple
 * placeholders in a component. [Modifier.placeholderShimmer] does a shimmer animation over the
 * whole component that includes the placeholders. There should only be one placeholderShimmer for
 * each component.
 *
 * NOTE: The order of modifiers is important. If you are adding both [Modifier.placeholder] and
 * [Modifier.placeholderShimmer] to the same composable then the shimmer must be before in the
 * modifier chain. Example of [Text] composable with both placeholderShimmer and placeholder
 * modifiers.
 *
 * @sample androidx.wear.compose.material3.samples.TextPlaceholder
 *
 * Once all of the components content is loaded the shimmer will stop and a wipe off animation will
 * remove the placeholders.
 *
 * @param isVisible whether the placeholder will be displayed. This should be modified later
 *   updating the state using [PlaceholderState.isVisible]
 */
@Stable
public class PlaceholderState(isVisible: Boolean) {

    /**
     * Whether the placeholder should be visible. Note that if there is an animation running, this
     * is the target state for the animation.
     */
    public var isVisible: Boolean
        get() = _isVisible.value
        set(newVisible) {
            // We don't want to both register for changes in _visible and make them.
            val currentVisible = Snapshot.withoutReadObservation { _isVisible.value }
            if (newVisible == currentVisible) return

            _isVisible.value = newVisible
            animationHelper.startAnimation(
                duration =
                    if (newVisible) PLACEHOLDER_RESET_ANIMATION_DURATION_MS
                    else PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS
            )
        }

    /**
     * Helper class to run animations in a lightweight way, i.e. instead of each animation needing
     * to call a suspend function, we use the [AnimationCoordinator], which keeps track of the
     * frameTime and animations are run by reading state derived from that.
     */
    private val animationHelper = PlaceholderAnimationHelper()

    /** Returns true while there is an animation in progress to show/hide the placeholders. */
    internal val isAnimationRunning: Boolean
        get() = animationHelper.isAnimationRunning()

    internal val frameTimeMillis: Long
        get() = animationHelper.frameTimeMillis()

    // Called by Modifiers using this state to coordinate animations
    internal fun register() = animationHelper.register()

    internal fun unregister() = animationHelper.unregister()

    private var _isVisible = mutableStateOf(isVisible)

    /**
     * The current value of the placeholder wipe off visual effect gradient progression alpha. The
     * progression is a 45 degree angle sweep across the whole screen running from outside of the
     * Top|Left of the screen to Bottom|Right used as the anchor for wipe-off gradient effects.
     *
     * The progression represents the x and y coordinates in pixels of the Top|Left part of the
     * gradient that flows across the screen. The progression will start at -maxScreenDimension (max
     * of height/width to create a 45 degree angle) and progress to the maximumScreenDimension.
     *
     * The time taken for this progression is 80ms.
     */
    internal val placeholderWipeOffAlpha: Float
        get() =
            wipeOffInterpolator.transform(
                (animationHelper.animationProgress() /
                        PLACEHOLDER_WIPE_OFF_PROGRESSION_ALPHA_RELATIVE)
                    .coerceAtMost(1f)
            )

    /**
     * The current value of the placeholder visual effect gradient progression alpha/opacity during
     * the fade-in part of reset placeholder animation. This allows the effect to be faded in during
     * 450ms.
     */
    internal val resetPlaceholderFadeInAlpha: Float
        get() =
            ((animationHelper.animationProgress() -
                    PLACEHOLDER_RESET_ANIMATION_OUTGOING_DURATION_RELATIVE) /
                    (1f - PLACEHOLDER_RESET_ANIMATION_OUTGOING_DURATION_RELATIVE))
                .let { absoluteProgression ->
                    if (absoluteProgression < 0f) {
                        0f
                    } else {
                        val alpha = lerp(0.1f, 1f, absoluteProgression)
                        resetFadeInInterpolator.transform(alpha)
                    }
                }

    /**
     * The current value of the placeholder visual effect gradient progression alpha/opacity during
     * the fade-out part of reset placeholder animation. This allows the effect to be faded out
     * during 450ms.
     */
    internal val resetPlaceholderFadeOutAlpha: Float
        get() = run {
            val absoluteProgression =
                animationHelper.animationProgress() /
                    PLACEHOLDER_RESET_ANIMATION_OUTGOING_DURATION_RELATIVE
            resetFadeOutInterpolator.transform(1f - absoluteProgression)
        }

    private val wipeOffInterpolator: Easing = CubicBezierEasing(0f, 0.2f, 1f, 0.6f)
    private val resetFadeInInterpolator: Easing = MotionTokens.EasingStandard
    private val resetFadeOutInterpolator: Easing = MotionTokens.EasingStandardAccelerate
}

private class PlaceholderShimmerElement(
    private val placeholderState: PlaceholderState,
    private val color: Color,
    private val shape: Shape,
    private val inspectorInfo: InspectorInfo.() -> Unit,
) : ModifierNodeElement<PlaceholderShimmerModifierNode>() {

    override fun create(): PlaceholderShimmerModifierNode {
        return PlaceholderShimmerModifierNode(placeholderState, color, shape)
    }

    override fun update(node: PlaceholderShimmerModifierNode) {
        node.updatePlaceholderState(placeholderState)
        node.color = color
        node.shape = shape
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as PlaceholderShimmerElement

        if (placeholderState != other.placeholderState) return false
        if (color != other.color) return false
        if (shape != other.shape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = placeholderState.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }
}

private class PlaceholderShimmerModifierNode(
    var placeholderState: PlaceholderState,
    var color: Color,
    var shape: Shape,
) :
    DrawModifierNode,
    Modifier.Node(),
    GlobalPositionAwareModifierNode,
    ObserverModifierNode,
    CompositionLocalConsumerModifierNode {
    private var offset = Offset.Zero
    private var maxScreenDimensionDp: Dp = 0f.dp

    // naive cache outline calculation if size is the same
    private var lastSize: Size = Size.Unspecified
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastOutline: Outline? = null
    private var lastShape: Shape? = null

    fun updatePlaceholderState(placeholderState: PlaceholderState) {
        onDetach()
        this.placeholderState = placeholderState
        onAttach()
    }

    override fun onAttach() {
        placeholderState.register()
    }

    override fun onDetach() {
        placeholderState.unregister()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        offset = coordinates.positionInRoot()
        val config = currentValueOf(LocalConfiguration)
        maxScreenDimensionDp = Dp(max(config.screenHeightDp, config.screenWidthDp).toFloat())
    }

    override fun onObservedReadsChanged() {
        // Reset cached properties
        lastOutline = null
        lastSize = Size.Unspecified
        lastLayoutDirection = null
        lastShape = null

        // Invalidate draw so we build the cache again - this is needed because observeReads within
        // the draw scope obscures the state reads from the draw scope's observer.
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val maxScreenDimensionPx =
            with(currentValueOf(LocalDensity)) { maxScreenDimensionDp.toPx() }
        drawContent()
        // Draw the shimmer on top of everything.
        generateBrush(maxScreenDimensionPx)?.let { brush ->
            if (shape === RectangleShape) {
                // shortcut to avoid Outline calculation and allocation
                drawRect(brush)
            } else {
                drawOutline(brush)
            }
        }
    }

    fun generateBrush(maxScreenDimension: Float): Brush? {
        return if (placeholderState.isVisible && !placeholderState.isAnimationRunning) {
            val halfGradientWidth = maxScreenDimension * 2f.pow(1.5f) / 2f
            val screenShimmerProgression =
                lerp(
                    -maxScreenDimension * 0.5f,
                    maxScreenDimension * 1.5f,
                    placeholderShimmerProgression,
                )
            val shimmerOffset = Offset(screenShimmerProgression, screenShimmerProgression) - offset
            val xOffset = Offset(halfGradientWidth, halfGradientWidth)
            Brush.linearGradient(
                start = shimmerOffset - xOffset,
                end = shimmerOffset + xOffset,
                colorStops =
                    listOf(
                            0.1f to color.copy(alpha = 0f),
                            0.65f to color.copy(alpha = placeholderShimmerAlpha),
                            0.9f to color.copy(alpha = 0f),
                        )
                        .toTypedArray(),
            )
        } else {
            null
        }
    }

    private fun ContentDrawScope.drawOutline(brush: Brush) {
        var outline: Outline? = null
        if (size == lastSize && layoutDirection == lastLayoutDirection && lastShape == shape) {
            outline = lastOutline!!
        } else {
            // Manually observe reads so we can directly invalidate the outline when it changes
            observeReads { outline = shape.createOutline(size, layoutDirection, this) }
        }
        drawOutline(outline!!, brush = brush)
        lastOutline = outline
        lastSize = size
        lastLayoutDirection = layoutDirection
        lastShape = shape
    }

    private fun shimmerAbsoluteProgression() =
        placeholderState.frameTimeMillis
            .mod(PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS)
            .coerceAtMost(PLACEHOLDER_SHIMMER_DURATION_MS)
            .toFloat() / PLACEHOLDER_SHIMMER_DURATION_MS

    /**
     * The current value of the placeholder visual effect gradient progression in Px. The
     * progression gives the x coordinate to be applied to the placeholder gradient as it moves
     * across the screen. Starting off screen to the left and progressing across the screen and
     * finishing off the screen to the after 800ms.
     */
    val placeholderShimmerProgression: Float
        get() = progressionInterpolator.transform(shimmerAbsoluteProgression())

    /**
     * The current value of the placeholder visual effect gradient progression alpha/opacity. The
     * progression gives the alpha to apply during the period of the placeholder effect. This allows
     * the effect to be faded in and then out during 800ms.
     */
    val placeholderShimmerAlpha: Float
        get() = 0.5f - abs(0.5f - shimmerAbsoluteProgression())

    private val progressionInterpolator: Easing = CubicBezierEasing(0.3f, 0f, 0.7f, 1f)
}

/**
 * Helper class to run animations using the [AnimationCoordinator], keeping track of the start and
 * end times for the current animation if there is one running.
 */
internal class PlaceholderAnimationHelper() {
    /**
     * Returns true if there is an animation in progress. It is a 'state' read, so clients will be
     * notified when this changes.
     */
    fun isAnimationRunning(): Boolean = maybeGetFrameMillis() != Long.MAX_VALUE

    /** Starts an animation for the given duration. */
    fun startAnimation(duration: Long) {
        AnimationCoordinator.frameMillis.longValue.let { frameMillis ->
            startOfTransitionAnimation = frameMillis
            endOfTransitionAnimation = frameMillis + duration
        }
    }

    /**
     * Returns a number between 0f and 1f representing the linear progress of this animation, or 1f
     * if there is no animation running. It is a 'state' read, so clients will be notified when this
     * changes.
     */
    fun animationProgress(): Float {
        val frameMillis = maybeGetFrameMillis()
        if (frameMillis == Long.MAX_VALUE) return 1f
        return ((frameMillis - startOfTransitionAnimation).toFloat() /
            (endOfTransitionAnimation - startOfTransitionAnimation))
    }

    /**
     * Returns the current frame time in milliseconds. This should be used with care, as clients
     * will be notified on each frame, mostly useful for infinite animations.
     */
    fun frameTimeMillis(): Long = AnimationCoordinator.frameMillis.longValue

    /** Register as a user. Animations will not run if there are no users. */
    fun register() {
        AnimationCoordinator.register()
        registeredUsers++
    }

    /** Unregister as a user. Animations will not run if there are no users. */
    fun unregister() {
        require(registeredUsers >= 1) {
            "Calls to unregister() can't be more than calls to register()"
        }
        AnimationCoordinator.unregister()
        registeredUsers--
    }

    /** How many Modifiers are using this, if this is 0, no animations will run. */
    private var registeredUsers = 0

    /**
     * The start time in milliseconds for the animation, or Long.MIN_VALUE if no animation is in
     * progress.
     */
    private var startOfTransitionAnimation by mutableLongStateOf(Long.MIN_VALUE)

    /**
     * The time in milliseconds for when the animation will end, or Long.MAX_VALUE if no animation
     * is in progress.
     */
    private var endOfTransitionAnimation = Long.MAX_VALUE

    // Returns the frame time if we are inside an animation, Long.MAX_VALUE otherwise.
    // If an animation just finished, update start/end times.
    private fun maybeGetFrameMillis(): Long {
        if (startOfTransitionAnimation == Long.MIN_VALUE || registeredUsers == 0)
            return Long.MAX_VALUE
        val frameMillis = frameTimeMillis()
        return if (frameMillis >= endOfTransitionAnimation) {
            // Stop listening to changes in the clock
            startOfTransitionAnimation = Long.MIN_VALUE
            endOfTransitionAnimation = Long.MAX_VALUE
            Long.MAX_VALUE
        } else {
            frameMillis
        }
    }
}

internal val PLACEHOLDER_SHIMMER_DURATION_MS
    get() = MotionTokens.DurationExtraLong2.toLong()
internal val PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS
    get() = MotionTokens.DurationMedium2.toLong()
internal val PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS
    get() = MotionTokens.DurationExtraLong4.toLong().times(2L)
internal val PLACEHOLDER_WIPE_OFF_PROGRESSION_ALPHA_DURATION_MS
    get() = 80L
internal val PLACEHOLDER_WIPE_OFF_PROGRESSION_ALPHA_RELATIVE =
    PLACEHOLDER_WIPE_OFF_PROGRESSION_ALPHA_DURATION_MS.toFloat() /
        PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS
internal val PLACEHOLDER_RESET_ANIMATION_INCOMING_DURATION_MS
    get() = MotionTokens.DurationMedium1.toLong()
internal val PLACEHOLDER_RESET_ANIMATION_OUTGOING_DURATION_MS
    get() = MotionTokens.DurationShort3.toLong()
internal val PLACEHOLDER_RESET_ANIMATION_DURATION_MS
    get() =
        PLACEHOLDER_RESET_ANIMATION_INCOMING_DURATION_MS +
            PLACEHOLDER_RESET_ANIMATION_OUTGOING_DURATION_MS
internal val PLACEHOLDER_RESET_ANIMATION_OUTGOING_DURATION_RELATIVE
    get() =
        PLACEHOLDER_RESET_ANIMATION_OUTGOING_DURATION_MS.toFloat() /
            PLACEHOLDER_RESET_ANIMATION_DURATION_MS
