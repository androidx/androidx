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

package androidx.compose.material3.ripple

import androidx.collection.mutableObjectListOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastAny
import kotlinx.coroutines.launch

/**
 * Creates a Ripple node using the values provided.
 *
 * A Ripple is a Material 3 node that expresses different [Interaction]s by drawing ripple
 * animations, and state layers, and other graphical effects.
 *
 * A Ripple responds to [PressInteraction.Press] by starting a new [RippleAnimation], and responds
 * to other [Interaction]s by showing a fixed state layer with varying alpha values depending on the
 * [Interaction], or an inset ring for [FocusInteraction], depending on the supplied
 * [RippleNodeConfiguration].
 *
 * This Ripple node is a low level building block for building IndicationNodeFactory implementations
 * that use a Ripple - higher level design system libraries such as material3 provide ripple
 * implementations using this node internally. In most cases you should use those factories
 * directly: this node exists for design system libraries to delegate their Ripple implementation
 * to, after querying any required theme values for customizing the Ripple.
 *
 * NOTE: when using this factory with [DelegatingNode.delegate], ensure that the node is created
 * once or [DelegatingNode.undelegate] is called in [Modifier.Node.onDetach]. Repeatedly delegating
 * to a new node returned by this method in [Modifier.Node.onAttach] without removing the old one
 * will result in multiple ripple nodes being attached to the node.
 *
 * @param interactionSource the [InteractionSource] used to determine the state of the ripple.
 * @param rippleNodeConfiguration the [RippleNodeConfiguration] that will be applied to the ripple
 *   depending on the state of the ripple. This lambda may be invoked repeatedly, so consider
 *   caching values of configuration when they haven't changed.
 */
public fun createRippleModifierNode(
    interactionSource: InteractionSource,
    rippleNodeConfiguration: () -> RippleNodeConfiguration,
): DelegatableNode {
    return RippleModifierNode(interactionSource, rippleNodeConfiguration)
}

/**
 * Node that handles both the ripple and the state layer, by delegating to specific nodes for each.
 */
internal class RippleModifierNode(
    interactionSource: InteractionSource,
    rippleNodeConfiguration: () -> RippleNodeConfiguration,
) : DelegatingNode() {
    init {
        delegate(createPlatformRippleNode(interactionSource, rippleNodeConfiguration))
    }
}

/** Creates the platform specific [RippleNode] implementation. */
internal expect fun createPlatformRippleNode(
    interactionSource: InteractionSource,
    rippleNodeConfiguration: () -> RippleNodeConfiguration,
): DelegatableNode

/**
 * Abstract [Modifier.Node] that provides common functionality used by ripple node implementations.
 * Implementing classes only need to handle showing the ripple effect when pressed, and not other
 * [Interaction]s.
 */
internal abstract class RippleNode(
    private val interactionSource: InteractionSource,
    /**
     * The producer of a [RippleNodeConfiguration]. Inside this node, use
     * [resolveRippleNodeConfiguration] instead to cache the value.
     */
    private val rippleNodeConfiguration: () -> RippleNodeConfiguration,
) : Modifier.Node(), ObserverModifierNode, DrawModifierNode, LayoutAwareModifierNode {
    final override val shouldAutoInvalidate: Boolean = false

    // The following are calculated inside updateTargetRadius(). These must be initialized before
    // adding a ripple.

    protected var targetRadius: Float = 0f
    // The size is needed for Android to update ripple bounds if the size changes
    protected var rippleSize: Size = Size.Zero
        private set

    // Track interactions that were emitted before we have been placed - we need to wait until we
    // have a valid size in order to set the radius and size correctly.
    private var hasValidSize = false
    private val pendingInteractions = mutableObjectListOf<PressInteraction>()

    private val animatedAlpha = Animatable(0f)

    private val animatedFocusRingInterpolation = Animatable(0f)

    private var focusedBorderLogic: BorderLogicLayerDelegate? = null

    private var _rippleNodeConfiguration: RippleNodeConfiguration? = null

    /**
     * Resolves and caches the ripple node configuration.
     *
     * @return the up-to-date [RippleNodeConfiguration].
     */
    protected fun resolveRippleNodeConfiguration(): RippleNodeConfiguration =
        getRippleNodeConfiguration(forceConfigurationRefresh = false)

    private fun getRippleNodeConfiguration(
        forceConfigurationRefresh: Boolean = false
    ): RippleNodeConfiguration {
        // Retrieve the currently set ripple node configuration (if any)
        val currentConfiguration = _rippleNodeConfiguration
        lateinit var resolvedConfiguration: RippleNodeConfiguration

        val refreshRippleNodeConfiguration =
            forceConfigurationRefresh || currentConfiguration == null
        val rippleNodeConfigurationChanged: Boolean

        if (refreshRippleNodeConfiguration) {
            // In practice, this branch will only be reached from `onObservedReadsChanged` or
            // `onAttach`. This avoids running the rippleNodeConfiguration() from an observable
            // scope like draw
            observeReads { resolvedConfiguration = rippleNodeConfiguration() }
            rippleNodeConfigurationChanged = resolvedConfiguration != currentConfiguration
        } else {
            resolvedConfiguration = currentConfiguration
            rippleNodeConfigurationChanged = false
        }
        _rippleNodeConfiguration = resolvedConfiguration

        if (rippleNodeConfigurationChanged) {
            updateTargetRadius(resolvedConfiguration)
            invalidateDraw()
        }

        return resolvedConfiguration
    }

    /**
     * Updates the target radius using the given [configuration].
     *
     * @return true if the target radius changed
     */
    private fun updateTargetRadius(configuration: RippleNodeConfiguration): Boolean {
        if (hasValidSize) {
            val newTargetRadius =
                with(requireDensity()) {
                    if (configuration.radius.isUnspecified) {
                        // Explicitly calculate the radius instead of using
                        // RippleDrawable.RADIUS_AUTO on Android since the latest spec does not
                        // match with the existing radius calculation in the framework.
                        getRippleEndRadius(configuration.isBounded, rippleSize)
                    } else {
                        configuration.radius.toPx()
                    }
                }
            val targetRadiusChanged = targetRadius != newTargetRadius
            if (targetRadiusChanged) {
                targetRadius = newTargetRadius
            }
            return targetRadiusChanged
        } else {
            return false
        }
    }

    override fun onObservedReadsChanged() {
        getRippleNodeConfiguration(forceConfigurationRefresh = true)
    }

    override fun onRemeasured(size: IntSize) {
        hasValidSize = true
        rippleSize = size.toSize()
        // Force a target radius refresh, to calculate a new target radius
        if (updateTargetRadius(resolveRippleNodeConfiguration())) {
            invalidateDraw()
        }
        // Flush any pending interactions that were waiting for measurement
        pendingInteractions.forEach { handlePressInteraction(it) }
        pendingInteractions.clear()
    }

    override fun onAttach() {
        // Resolve the ripple node configuration immediately, to observe reads from an otherwise
        // non-observable function
        resolveRippleNodeConfiguration()

        coroutineScope.launch {
            val interactions: MutableList<Interaction> = mutableListOf()
            var currentInteraction: Interaction? = null

            var isFocused = false

            interactionSource.interactions.collect { interaction ->
                val wasFocused = isFocused
                if (interaction is PressInteraction) {
                    if (hasValidSize) {
                        handlePressInteraction(interaction)
                    } else {
                        // Handle these later when we have a valid size
                        pendingInteractions += interaction
                    }
                }

                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }
                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }
                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                        isFocused = true
                    }
                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                        if (!interactions.fastAny { it is FocusInteraction.Focus }) {
                            isFocused = false
                        }
                    }
                    is DragInteraction.Start -> {
                        interactions.add(interaction)
                    }
                    is DragInteraction.Stop -> {
                        interactions.remove(interaction.start)
                    }
                    is DragInteraction.Cancel -> {
                        interactions.remove(interaction.start)
                    }
                    else -> return@collect
                }

                // The most recent interaction is the one we want to show
                val newInteraction = interactions.lastOrNull()

                val config = resolveRippleNodeConfiguration()
                if (currentInteraction != newInteraction) {
                    if (newInteraction != null) {
                        val targetAlpha =
                            when (newInteraction) {
                                is HoverInteraction.Enter -> {
                                    when (config.hoverConfiguration) {
                                        is RippleNodeConfiguration.HoverConfiguration.Opacity ->
                                            config.hoverConfiguration.alpha
                                        is RippleNodeConfiguration.HoverConfiguration.None -> 0f
                                        else -> 0f
                                    }
                                }
                                is FocusInteraction.Focus -> {
                                    when (config.focusConfiguration) {
                                        is RippleNodeConfiguration.FocusConfiguration.Opacity ->
                                            config.focusConfiguration.alpha
                                        is RippleNodeConfiguration.FocusConfiguration.InsetRing,
                                        is RippleNodeConfiguration.FocusConfiguration.None -> 0f
                                        else -> 0f
                                    }
                                }
                                is DragInteraction.Start -> {
                                    when (config.dragConfiguration) {
                                        is RippleNodeConfiguration.DragConfiguration.Opacity ->
                                            config.dragConfiguration.alpha
                                        is RippleNodeConfiguration.DragConfiguration.None -> 0f
                                        else -> 0f
                                    }
                                }
                                else -> 0f
                            }
                        val incomingAnimationSpec =
                            incomingStateLayerAnimationSpecFor(newInteraction)

                        launch { animatedAlpha.animateTo(targetAlpha, incomingAnimationSpec) }
                    } else {
                        val outgoingAnimationSpec =
                            outgoingStateLayerAnimationSpecFor(currentInteraction)

                        launch { animatedAlpha.animateTo(0f, outgoingAnimationSpec) }
                    }

                    if (interaction is FocusInteraction) {
                        when (config.focusConfiguration) {
                            is RippleNodeConfiguration.FocusConfiguration.InsetRing -> {
                                // Only launch the focus ring animation if the state changed
                                if (wasFocused != isFocused) {
                                    val targetValue = if (isFocused) 1f else 0f
                                    launch {
                                        animatedFocusRingInterpolation.animateTo(
                                            targetValue,
                                            if (isFocused) {
                                                config.focusConfiguration.focusingAnimationSpec
                                            } else {
                                                config.focusConfiguration.unfocusingAnimationSpec
                                            },
                                        )
                                    }
                                }
                            }
                            else -> {
                                launch { animatedFocusRingInterpolation.snapTo(0f) }
                            }
                        }
                    }

                    currentInteraction = newInteraction
                }
            }
        }
    }

    override fun onDetach() {
        focusedBorderLogic?.release()
        focusedBorderLogic = null
        _rippleNodeConfiguration = null
        hasValidSize = false
        rippleSize = Size.Zero
        targetRadius = 0f
        pendingInteractions.clear()
    }

    private fun handlePressInteraction(pressInteraction: PressInteraction) {
        when (pressInteraction) {
            is PressInteraction.Press -> addRipple(pressInteraction, rippleSize, targetRadius)
            is PressInteraction.Release -> removeRipple(pressInteraction.press)
            is PressInteraction.Cancel -> removeRipple(pressInteraction.press)
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        drawRipples()
        drawStateLayers()
    }

    private fun DrawScope.drawStateLayers() {
        val alpha = animatedAlpha.value

        if (alpha > 0f) {
            val config = resolveRippleNodeConfiguration()
            val modulatedColor = config.color().copy(alpha = alpha)

            if (config.isBounded) {
                clipRect { drawCircle(modulatedColor, targetRadius) }
            } else {
                drawCircle(modulatedColor, targetRadius)
            }
        }

        if (animatedFocusRingInterpolation.value > 0f) {
            focusedBorderLogic = focusedBorderLogic ?: BorderLogicLayerDelegate()
            val config = resolveRippleNodeConfiguration()
            val insetRing =
                config.focusConfiguration as? RippleNodeConfiguration.FocusConfiguration.InsetRing
                    ?: return

            val outline =
                insetRing.shape.createOutline(
                    size = size,
                    layoutDirection = layoutDirection,
                    density = this,
                )
            focusedBorderLogic!!.drawBorder(
                drawScope = this,
                width = { insetRing.innerStrokeWidth * animatedFocusRingInterpolation.value },
                inset = { insetRing.innerStrokeInset * animatedFocusRingInterpolation.value },
                brush = SolidColor(insetRing.innerStrokeColor()),
                outline = outline,
            )
            focusedBorderLogic!!.drawBorder(
                drawScope = this,
                width = { insetRing.outerStrokeWidth * animatedFocusRingInterpolation.value },
                inset = { insetRing.outerStrokeInset * animatedFocusRingInterpolation.value },
                brush = SolidColor(insetRing.outerStrokeColor()),
                outline = outline,
            )
        }
    }

    abstract fun DrawScope.drawRipples()

    abstract fun addRipple(interaction: PressInteraction.Press, size: Size, targetRadius: Float)

    abstract fun removeRipple(interaction: PressInteraction.Press)

    /** Border logic that correctly manages the [GraphicsLayer], should be released in [onDetach] */
    inner class BorderLogicLayerDelegate {
        val borderLogic = BorderLogic()
        var layer: GraphicsLayer? = null

        fun drawBorder(
            drawScope: DrawScope,
            width: () -> Dp,
            inset: () -> Dp,
            brush: Brush,
            outline: Outline,
        ) {
            return borderLogic.drawBorder(
                drawScope,
                width,
                inset,
                brush,
                { layer ?: obtainGraphicsLayer().also { layer = it } },
                outline,
            )
        }

        fun obtainGraphicsLayer(): GraphicsLayer = requireGraphicsContext().createGraphicsLayer()

        fun release() {
            layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        }
    }
}

/**
 * @return the [AnimationSpec] used when transitioning to [interaction], either from a previous
 *   state, or no state.
 */
private fun incomingStateLayerAnimationSpecFor(interaction: Interaction): AnimationSpec<Float> {
    return when (interaction) {
        is HoverInteraction.Enter -> DefaultTweenSpec
        is FocusInteraction.Focus -> TweenSpec(durationMillis = 45, easing = LinearEasing)
        is DragInteraction.Start -> TweenSpec(durationMillis = 45, easing = LinearEasing)
        else -> DefaultTweenSpec
    }
}

/** @return the [AnimationSpec] used when transitioning away from [interaction], to no state. */
private fun outgoingStateLayerAnimationSpecFor(interaction: Interaction?): AnimationSpec<Float> {
    return when (interaction) {
        is HoverInteraction.Enter -> DefaultTweenSpec
        is FocusInteraction.Focus -> DefaultTweenSpec
        is DragInteraction.Start -> TweenSpec(durationMillis = 150, easing = LinearEasing)
        else -> DefaultTweenSpec
    }
}

/** Default / fallback [AnimationSpec]. */
private val DefaultTweenSpec = TweenSpec<Float>(durationMillis = 15, easing = LinearEasing)
