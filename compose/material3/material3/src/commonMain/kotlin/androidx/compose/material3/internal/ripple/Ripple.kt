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

package androidx.compose.material3.internal.ripple

import androidx.collection.mutableObjectListOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
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
 * A Ripple is a Material implementation of [Indication] that expresses different [Interaction]s by
 * drawing ripple animations and state layers.
 *
 * A Ripple responds to [PressInteraction.Press] by starting a new [RippleAnimation], and responds
 * to other [Interaction]s by showing a fixed state layer with varying alpha values depending on the
 * [Interaction].
 *
 * This Ripple node is a low level building block for building IndicationNodeFactory implementations
 * that use a Ripple - higher level design system libraries such as material and material3 provide
 * [Indication] implementations using this node internally. In most cases you should use those
 * factories directly: this node exists for design system libraries to delegate their Ripple
 * implementation to, after querying any required theme values for customizing the Ripple.
 *
 * NOTE: when using this factory with [DelegatingNode.delegate], ensure that the node is created
 * once or [DelegatingNode.undelegate] is called in [Modifier.Node.onDetach]. Repeatedly delegating
 * to a new node returned by this method in [Modifier.Node.onAttach] without removing the old one
 * will result in multiple ripple nodes being attached to the node.
 *
 * @param interactionSource the [InteractionSource] used to determine the state of the ripple.
 * @param bounded if true, ripples are clipped by the bounds of the target layout. Unbounded ripples
 *   always animate from the target layout center, bounded ripples animate from the touch position.
 * @param radius the radius for the ripple. If [Dp.Unspecified] is provided then the size will be
 *   calculated based on the target layout size.
 * @param color the color of the ripple. This color is usually the same color used by the text or
 *   iconography in the component. This color will then have [rippleNodeConfig] applied to calculate
 *   the final color used to draw the ripple.
 * @param rippleNodeConfig the [RippleNodeConfig] that will be applied to the [color] depending on
 *   the state of the ripple.
 */
internal fun createRippleModifierNode(
    interactionSource: InteractionSource,
    bounded: Boolean,
    radius: Dp,
    color: ColorProducer,
    rippleNodeConfig: () -> RippleNodeConfig,
): DelegatableNode {
    return RippleModifierNode(interactionSource, bounded, radius, color, rippleNodeConfig)
}

/**
 * Node that handles both the ripple and the state layer, by delegating to specific nodes for each.
 */
internal class RippleModifierNode(
    interactionSource: InteractionSource,
    bounded: Boolean,
    radius: Dp,
    color: ColorProducer,
    rippleNodeConfig: () -> RippleNodeConfig,
) : DelegatingNode() {
    init {
        delegate(
            createPlatformRippleNode(interactionSource, bounded, radius, color, rippleNodeConfig)
        )
    }
}

/** Creates the platform specific [RippleNode] implementation. */
internal expect fun createPlatformRippleNode(
    interactionSource: InteractionSource,
    bounded: Boolean,
    radius: Dp,
    color: ColorProducer,
    rippleNodeConfig: () -> RippleNodeConfig,
): DelegatableNode

/**
 * Abstract [Modifier.Node] that provides common functionality used by ripple node implementations.
 * Implementing classes only need to handle showing the ripple effect when pressed, and not other
 * [Interaction]s.
 */
internal abstract class RippleNode(
    private val interactionSource: InteractionSource,
    protected val bounded: Boolean,
    private val radius: Dp,
    private val color: ColorProducer,
    protected val rippleNodeConfig: () -> RippleNodeConfig,
) :
    Modifier.Node(),
    CompositionLocalConsumerModifierNode,
    DrawModifierNode,
    LayoutAwareModifierNode {
    final override val shouldAutoInvalidate: Boolean = false

    // The following are calculated inside onRemeasured(). These must be initialized before adding
    // a ripple.

    protected var targetRadius: Float = 0f
    // The size is needed for Android to update ripple bounds if the size changes
    protected var rippleSize: Size = Size.Zero
        private set

    val rippleColor: Color
        get() = color()

    // Track interactions that were emitted before we have been placed - we need to wait until we
    // have a valid size in order to set the radius and size correctly.
    private var hasValidSize = false
    private val pendingInteractions = mutableObjectListOf<PressInteraction>()

    private val animatedAlpha = Animatable(0f)

    private val interactions: MutableList<Interaction> = mutableListOf()
    private var currentInteraction: Interaction? = null

    private val animatedFocusRingInterpolation = Animatable(0f)

    private var isFocused by mutableStateOf(false)

    private var focusedBorderLogic: BorderLogicLayerDelegate? = null

    override fun onRemeasured(size: IntSize) {
        hasValidSize = true
        val density = requireDensity()
        rippleSize = size.toSize()
        targetRadius =
            with(density) {
                if (radius.isUnspecified) {
                    // Explicitly calculate the radius instead of using RippleDrawable.RADIUS_AUTO
                    // on
                    // Android since the latest spec does not match with the existing radius
                    // calculation
                    // in the framework.
                    getRippleEndRadius(bounded, rippleSize)
                } else {
                    radius.toPx()
                }
            }
        // Flush any pending interactions that were waiting for measurement
        pendingInteractions.forEach { handlePressInteraction(it) }
        pendingInteractions.clear()
    }

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction) {
                    if (hasValidSize) {
                        handlePressInteraction(interaction)
                    } else {
                        // Handle these later when we have a valid size
                        pendingInteractions += interaction
                    }
                }

                val wasFocused = isFocused

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

                val rippleNodeConfig = rippleNodeConfig()
                if (currentInteraction != newInteraction) {
                    if (newInteraction != null) {
                        val targetAlpha =
                            when (newInteraction) {
                                is HoverInteraction.Enter -> {
                                    when (rippleNodeConfig.hover) {
                                        is RippleNodeConfig.Hover.Opacity ->
                                            rippleNodeConfig.hover.alpha
                                        else -> 0f
                                    }
                                }
                                is FocusInteraction.Focus -> {
                                    when (rippleNodeConfig.focus) {
                                        is RippleNodeConfig.Focus.Opacity ->
                                            rippleNodeConfig.focus.alpha
                                        else -> 0f
                                    }
                                }
                                is DragInteraction.Start -> {
                                    when (rippleNodeConfig.drag) {
                                        is RippleNodeConfig.Drag.Opacity ->
                                            rippleNodeConfig.drag.alpha
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

                    when (rippleNodeConfig.focus) {
                        is RippleNodeConfig.Focus.InsetRing -> {
                            // Only launch the focus ring animation if the state changed
                            if (wasFocused != isFocused) {
                                val isFocusing = newInteraction is FocusInteraction.Focus
                                launch {
                                    animatedFocusRingInterpolation.animateTo(
                                        if (isFocusing) 1f else 0f,
                                        if (isFocusing) {
                                            rippleNodeConfig.focus.focusingAnimationSpec
                                        } else {
                                            rippleNodeConfig.focus.unfocusingAnimationSpec
                                        },
                                    )
                                }
                            }
                        }
                        else -> {
                            launch { animatedFocusRingInterpolation.snapTo(0f) }
                        }
                    }

                    currentInteraction = newInteraction
                }
            }
        }
    }

    override fun onDetach() {
        focusedBorderLogic?.release()
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
            val modulatedColor = color().copy(alpha = alpha)

            if (bounded) {
                clipRect { drawCircle(modulatedColor, targetRadius) }
            } else {
                drawCircle(modulatedColor, targetRadius)
            }
        }

        if (animatedFocusRingInterpolation.value > 0f) {
            focusedBorderLogic = focusedBorderLogic ?: BorderLogicLayerDelegate()
            val insetRing = rippleNodeConfig().focus as? RippleNodeConfig.Focus.InsetRing ?: return

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
