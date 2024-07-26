/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Indication represents visual effects that occur when certain interactions happens. For
 * example: showing a ripple effect when a component is pressed, or a highlight when a component
 * is focused.
 *
 * To implement your own Indication, see [IndicationNodeFactory] - an optimized [Indication] that
 * allows for more efficient implementations than the deprecated [rememberUpdatedInstance].
 *
 * Indication is typically provided throughout the hierarchy through [LocalIndication] - you can
 * provide a custom Indication to [LocalIndication] to change the default [Indication] used for
 * components such as [clickable].
 */
@Stable
interface Indication {

    /**
     * [remember]s a new [IndicationInstance], and updates its state based on [Interaction]s
     * emitted via [interactionSource] . Typically this will be called by [indication],
     * so one [IndicationInstance] will be used for one component that draws [Indication], such
     * as a button.
     *
     * Implementations of this function should observe [Interaction]s using [interactionSource],
     * using them to launch animations / state changes inside [IndicationInstance] that will
     * then be reflected inside [IndicationInstance.drawIndication].
     *
     * @param interactionSource the [InteractionSource] representing the stream of
     * [Interaction]s the returned [IndicationInstance] should represent
     * @return an [IndicationInstance] that represents the stream of [Interaction]s emitted by
     * [interactionSource]
     */
    @Suppress("DEPRECATION_ERROR")
    @Deprecated(RememberUpdatedInstanceDeprecationMessage, level = DeprecationLevel.ERROR)
    @Composable
    fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance =
        NoIndicationInstance
}

/**
 * IndicationNodeFactory is an Indication that creates [Modifier.Node] instances to render visual
 * effects that occur when certain interactions happens. For example: showing a ripple effect
 * when a component is pressed, or a highlight when a component is focused.
 *
 * An instance of IndicationNodeFactory is responsible for creating individual nodes on demand for
 * each component that needs to render indication. IndicationNodeFactory instances should be very
 * simple - they just hold the relevant configuration properties needed to create the node instances
 * that are responsible for drawing visual effects.
 *
 * IndicationNodeFactory is conceptually similar to [ModifierNodeElement] - it is designed to be
 * able to be created outside of composition, and re-used in multiple places.
 *
 * Indication is typically provided throughout the hierarchy through [LocalIndication] - you can
 * provide a custom Indication to [LocalIndication] to change the default [Indication] used for
 * components such as [clickable].
 */
@Stable
interface IndicationNodeFactory : Indication {
    /**
     * Creates a node that will be applied to a specific component and render indication for the
     * provided [interactionSource]. This method will be re-invoked for a given layout node if a new
     * [interactionSource] is provided or if [hashCode] or [equals] change for this
     * IndicationNodeFactory over time, allowing a new node to be created using the new properties
     * in this IndicationNodeFactory. If you instead want to gracefully update the existing node
     * over time, consider replacing those properties with [androidx.compose.runtime.State]
     * properties, so when the value of the State changes, [equals] and [hashCode] remain the
     * same, and the same node instance can just query the updated state value.
     *
     * The returned [DelegatableNode] should implement [DrawModifierNode], or delegate to a node
     * that implements [DrawModifierNode], so that it can draw visual effects. Inside
     * [DrawModifierNode.draw], make sure to call [ContentDrawScope.drawContent] to render the
     * component in addition to any visual effects.
     *
     * @param interactionSource the [InteractionSource] representing the stream of
     * [Interaction]s the returned node should render visual effects for
     * @return a [DelegatableNode] that renders visual effects for the provided [interactionSource]
     * by also implementing / delegating to a [DrawModifierNode]
     */
    fun create(interactionSource: InteractionSource): DelegatableNode

    /**
     * Require hashCode() to be implemented. Using a data class is sufficient. Singletons and
     * instances with no properties may implement this function by returning an arbitrary constant.
     */
    override fun hashCode(): Int

    /**
     * Require equals() to be implemented. Using a data class is sufficient. Singletons may
     * implement this function with referential equality (`this === other`). Instances with no
     * properties may implement this function by checking the type of the other object.
     */
    override fun equals(other: Any?): Boolean
}

/**
 * IndicationInstance is a specific instance of an [Indication] that draws visual effects on
 * certain interactions, such as press or focus.
 *
 * IndicationInstances can be stateful or stateless, and are created by
 * [Indication.rememberUpdatedInstance] - they should be used in-place and not re-used between
 * different [indication] modifiers.
 */
@Deprecated(IndicationInstanceDeprecationMessage, level = DeprecationLevel.ERROR)
interface IndicationInstance {

    /**
     * Draws visual effects for the current interactions present on this component.
     *
     * Typically this function will read state within this instance that is mutated by
     * [Indication.rememberUpdatedInstance]. This allows [IndicationInstance] to just read state
     * and draw visual effects, and not actually change any state itself.
     *
     * This method MUST call [ContentDrawScope.drawContent] at some point in order to draw the
     * component itself underneath any indication. Typically this is called at the beginning, so
     * that indication can be drawn as an overlay on top.
     */
    fun ContentDrawScope.drawIndication()
}

/**
 * Draws visual effects for this component when interactions occur.
 *
 * @sample androidx.compose.foundation.samples.IndicationSample
 *
 * @param interactionSource [InteractionSource] that will be used by [indication] to draw
 * visual effects - this [InteractionSource] represents the stream of [Interaction]s for this
 * component.
 * @param indication [Indication] used to draw visual effects. If `null`, no visual effects will
 * be shown for this component.
 */
fun Modifier.indication(
    interactionSource: InteractionSource,
    indication: Indication?
): Modifier = indicationImpl(
    interactionSource,
    platformIndication(indication)
)

private fun Modifier.indicationImpl(
    interactionSource: InteractionSource,
    indication: Indication?
): Modifier {
    if (indication == null) return this
    // Fast path - ideally we should never break into the composed path below.
    if (indication is IndicationNodeFactory) {
        return this.then(IndicationModifierElement(interactionSource, indication))
    }
    // In the future we might want to remove this as a forcing function to migrate away from the
    // error-deprecated rememberUpdatedInstance
    return composed(
        factory = {
            @Suppress("DEPRECATION_ERROR")
            val instance = indication.rememberUpdatedInstance(interactionSource)
            remember(instance) {
                IndicationModifier(instance)
            }
        },
        inspectorInfo = debugInspectorInfo {
            name = "indication"
            properties["interactionSource"] = interactionSource
            properties["indication"] = indication
        }
    )
}

/**
 * CompositionLocal that provides an [Indication] through the hierarchy. This [Indication] will
 * be used by default to draw visual effects for interactions such as press and drag in components
 * such as [clickable].
 *
 * By default this will provide a debug indication, this should always be replaced.
 */
val LocalIndication = staticCompositionLocalOf<Indication> {
    DefaultDebugIndication
}

/**
 * Empty [IndicationInstance] for backwards compatibility - this is not expected to be used.
 */
@Suppress("DEPRECATION_ERROR")
private object NoIndicationInstance : IndicationInstance {
    override fun ContentDrawScope.drawIndication() {
        drawContent()
    }
}

/**
 * Simple default [Indication] that draws a rectangular overlay when pressed.
 */
private object DefaultDebugIndication : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        DefaultDebugIndicationInstance(interactionSource)

    override fun hashCode(): Int = -1

    override fun equals(other: Any?) = other === this

    private class DefaultDebugIndicationInstance(private val interactionSource: InteractionSource) :
        Modifier.Node(), DrawModifierNode {
        private var isPressed = false
        private var isHovered = false
        private var isFocused = false
        override fun onAttach() {
            coroutineScope.launch {
                var pressCount = 0
                var hoverCount = 0
                var focusCount = 0
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> pressCount++
                        is PressInteraction.Release -> pressCount--
                        is PressInteraction.Cancel -> pressCount--
                        is HoverInteraction.Enter -> hoverCount++
                        is HoverInteraction.Exit -> hoverCount--
                        is FocusInteraction.Focus -> focusCount++
                        is FocusInteraction.Unfocus -> focusCount--
                    }
                    val pressed = pressCount > 0
                    val hovered = hoverCount > 0
                    val focused = focusCount > 0
                    var invalidateNeeded = false
                    if (isPressed != pressed) {
                        isPressed = pressed
                        invalidateNeeded = true
                    }
                    if (isHovered != hovered) {
                        isHovered = hovered
                        invalidateNeeded = true
                    }
                    if (isFocused != focused) {
                        isFocused = focused
                        invalidateNeeded = true
                    }
                    if (invalidateNeeded) invalidateDraw()
                }
            }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            if (isPressed) {
                drawRect(color = Color.Black.copy(alpha = 0.3f), size = size)
            } else if (isHovered || isFocused) {
                drawRect(color = Color.Black.copy(alpha = 0.1f), size = size)
            }
        }
    }
}

/**
 * ModifierNodeElement to create [IndicationNodeFactory] instances. More complicated modifiers such
 * as [clickable] should manually delegate to the node returned by [IndicationNodeFactory]
 * internally.
 */
private class IndicationModifierElement(
    private val interactionSource: InteractionSource,
    private val indication: IndicationNodeFactory
) : ModifierNodeElement<IndicationModifierNode>() {
    override fun create(): IndicationModifierNode {
        return IndicationModifierNode(indication.create(interactionSource))
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "indication"
        properties["interactionSource"] = interactionSource
        properties["indication"] = indication
    }

    override fun update(node: IndicationModifierNode) {
        node.update(indication.create(interactionSource))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IndicationModifierElement) return false

        if (interactionSource != other.interactionSource) return false
        if (indication != other.indication) return false

        return true
    }

    override fun hashCode(): Int {
        var result = interactionSource.hashCode()
        result = 31 * result + indication.hashCode()
        return result
    }
}

/**
 * Wrapper [DelegatableNode] that allows us to replace the wrapped node fully when a new node is
 * provided.
 */
private class IndicationModifierNode(private var indicationNode: DelegatableNode) :
    DelegatingNode() {
    init {
        delegate(indicationNode)
    }

    fun update(indicationNode: DelegatableNode) {
        undelegate(this.indicationNode)
        this.indicationNode = indicationNode
        delegate(indicationNode)
    }
}

@Suppress("DEPRECATION_ERROR")
private class IndicationModifier(
    val indicationInstance: IndicationInstance
) : DrawModifier {

    override fun ContentDrawScope.draw() {
        with(indicationInstance) {
            drawIndication()
        }
    }
}

private const val RememberUpdatedInstanceDeprecationMessage = "rememberUpdatedInstance has been " +
    "deprecated - implementers should instead implement IndicationNodeFactory#create for " +
    "improved performance and efficiency. Callers should check if the Indication is an " +
    "IndicationNodeFactory, and call that API instead. For a migration guide and background " +
    "information, please visit developer.android.com"

private const val IndicationInstanceDeprecationMessage = "IndicationInstance has been deprecated " +
    "along with the rememberUpdatedInstance that returns it. Indication implementations should " +
    "instead use Modifier.Node APIs, and should be returned from " +
    "IndicationNodeFactory#create. For a migration guide and background information, " +
    "please visit developer.android.com"

/**
 * Some platforms add additional logic to the indication.
 * For example, desktop/web in Compose Multiplatform hide focus indication depending on [InputMode]
 */
internal expect fun platformIndication(indication: Indication?): Indication?
