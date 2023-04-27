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
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequesterNode
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusPropertiesModifierNode
import androidx.compose.ui.focus.FocusRequesterModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectableModifier
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.requestFocus
import kotlinx.coroutines.launch

/**
 * Configure component to be focusable via focus system or accessibility "focus" event.
 *
 * Add this modifier to the element to make it focusable within its bounds.
 *
 * @sample androidx.compose.foundation.samples.FocusableSample
 *
 * @param enabled Controls the enabled state. When `false`, element won't participate in the focus
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * [FocusInteraction.Focus] when this element is being focused.
 */
fun Modifier.focusable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) = this.then(
    if (enabled) {
        FocusableElement(
            interactionSource
        ).focusTarget()
    } else {
        Modifier
    }
)

/**
 * Creates a focus group or marks this component as a focus group. This means that when we move
 * focus using the keyboard or programmatically using
 * [FocusManager.moveFocus()][androidx.compose.ui.focus.FocusManager.moveFocus], the items within
 * the focus group will be given a higher priority before focus moves to items outside the focus
 * group.
 *
 * In the sample below, each column is a focus group, so pressing the tab key will move focus
 * to all the buttons in column 1 before visiting column 2.
 *
 * @sample androidx.compose.foundation.samples.FocusGroupSample
 *
 * Note: The focusable children of a focusable parent automatically form a focus group. This
 * modifier is to be used when you want to create a focus group where the parent is not focusable.
 * If you encounter a component that uses a [focusGroup] internally, you can make it focusable by
 * using a [focusable] modifier. In the second sample here, the
 * [LazyRow][androidx.compose.foundation.lazy.LazyRow] is a focus group that is not itself
 * focusable. But you can make it focusable by adding a [focusable] modifier.
 *
 * @sample androidx.compose.foundation.samples.FocusableFocusGroupSample
 */
@ExperimentalFoundationApi
fun Modifier.focusGroup(): Modifier {
    return this
        .then(focusGroupInspectorInfo)
        .focusProperties { canFocus = false }
        .focusTarget()
}

private val focusGroupInspectorInfo = InspectableModifier(
    debugInspectorInfo { name = "focusGroup" }
)

// TODO: b/202856230 - consider either making this / a similar API public, or add a parameter to
//  focusable to configure this behavior.
/**
 * [focusable] but only when not in touch mode - when [LocalInputModeManager] is
 * not [InputMode.Touch]
 */
internal fun Modifier.focusableInNonTouchMode(
    enabled: Boolean,
    interactionSource: MutableInteractionSource?
) = inspectable(inspectorInfo = {
    name = "focusableInNonTouchMode"
    properties["enabled"] = enabled
    properties["interactionSource"] = interactionSource
},
    factory = {
        Modifier
            .then(FocusableInNonTouchModeElement)
            .focusable(enabled, interactionSource)
    })

private val FocusableInNonTouchModeElement =
    object : ModifierNodeElement<FocusableInNonTouchMode>() {
        override fun create(): FocusableInNonTouchMode = FocusableInNonTouchMode()

        override fun update(node: FocusableInNonTouchMode) {}

        override fun hashCode(): Int = System.identityHashCode(this)

        override fun equals(other: Any?): Boolean = this === other

        override fun InspectorInfo.inspectableProperties() {
            name = "focusableInNonTouchMode"
        }
    }

private class FocusableInNonTouchMode : Modifier.Node(), CompositionLocalConsumerModifierNode,
    FocusPropertiesModifierNode {

    private val inputModeManager: InputModeManager
        get() = currentValueOf(LocalInputModeManager)

    override fun modifyFocusProperties(focusProperties: FocusProperties) {
        focusProperties.apply {
            canFocus = inputModeManager.inputMode != InputMode.Touch
        }
    }
}

private class FocusableElement(
    private val interactionSource: MutableInteractionSource?
) : ModifierNodeElement<FocusableNode>() {

    override fun create(): FocusableNode =
        FocusableNode(interactionSource)

    override fun update(node: FocusableNode) {
        node.update(interactionSource)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FocusableElement) return false

        if (interactionSource != other.interactionSource) return false
        return true
    }

    override fun hashCode(): Int {
        return interactionSource.hashCode()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "focusable"
        properties["enabled"] = true
        properties["interactionSource"] = interactionSource
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class FocusableNode(
    interactionSource: MutableInteractionSource?
) : DelegatingNode(), FocusEventModifierNode, LayoutAwareModifierNode, SemanticsModifierNode,
    GlobalPositionAwareModifierNode {

    private var focusState: FocusState? = null

    private val focusableSemanticsNode = delegate(FocusableSemanticsNode())
    // (lpf) could we remove this if interactionsource is null?
    private val focusableInteractionNode = delegate(FocusableInteractionNode(interactionSource))
    private val focusablePinnableContainer = delegate(FocusablePinnableContainerNode())
    private val focusedBoundsNode = delegate(FocusedBoundsNode())

    // Focusables have a few different cases where they need to make sure they stay visible:
    //
    // 1. Focusable node newly receives focus – always bring entire node into view. That's what this
    //    BringIntoViewRequester does.
    // 2. Scrollable parent resizes and the currently-focused item is now hidden – bring entire node
    //    into view if it was also in view before the resize. This handles the case of
    //    `softInputMode=ADJUST_RESIZE`. See b/216842427.
    // 3. Entire window is panned due to `softInputMode=ADJUST_PAN` – report the correct focused
    //    rect to the view system, and the view system itself will keep the focused area in view.
    //    See aosp/1964580.
    private val bringIntoViewRequester = BringIntoViewRequester()

    private val bringIntoViewRequesterNode = delegate(
        BringIntoViewRequesterNode(bringIntoViewRequester)
    )

    // TODO(levima) Remove this once delegation can propagate this events on its own
    override fun onPlaced(coordinates: LayoutCoordinates) =
        bringIntoViewRequesterNode.onPlaced(coordinates)

    fun update(interactionSource: MutableInteractionSource?) =
        focusableInteractionNode.update(interactionSource)

    // TODO(levima) Update this once delegation can propagate this events on its own
    override fun onFocusEvent(focusState: FocusState) {
        if (this.focusState != focusState) { // focus state changed
            val isFocused = focusState.isFocused
            if (isFocused) {
                coroutineScope.launch {
                    bringIntoViewRequester.bringIntoView()
                }
            }
            if (isAttached) invalidateSemantics()
            focusableInteractionNode.setFocus(isFocused)
            focusedBoundsNode.setFocus(isFocused)
            focusablePinnableContainer.setFocus(isFocused)
            focusableSemanticsNode.setFocus(isFocused)
            this.focusState = focusState
        }
    }

    // TODO(levima) Remove this once delegation can propagate this events on its own
    override val semanticsConfiguration: SemanticsConfiguration
        get() = focusableSemanticsNode.semanticsConfiguration

    // TODO(levima) Remove this once delegation can propagate this events on its own
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        focusedBoundsNode.onGloballyPositioned(coordinates)
    }
}

private class FocusableInteractionNode(
    private var interactionSource: MutableInteractionSource?
) : Modifier.Node() {
    private var focusedInteraction: FocusInteraction.Focus? = null

    /**
     * Interaction source events will be controlled entirely by changes in focus events. The
     * FocusEventNode will be the source of truth for this and will emit an event in case it
     * is detached.
     */
    fun setFocus(isFocused: Boolean) {
        interactionSource?.let { interactionSource ->
            if (isFocused) {
                focusedInteraction?.let { oldValue ->
                    val interaction = FocusInteraction.Unfocus(oldValue)
                    interactionSource.emitWithFallback(interaction)
                    focusedInteraction = null
                }
                val interaction = FocusInteraction.Focus()
                interactionSource.emitWithFallback(interaction)
                focusedInteraction = interaction
            } else {
                focusedInteraction?.let { oldValue ->
                    val interaction = FocusInteraction.Unfocus(oldValue)
                    interactionSource.emitWithFallback(interaction)
                    focusedInteraction = null
                }
            }
        }
    }

    fun update(interactionSource: MutableInteractionSource?) {
        if (this.interactionSource != interactionSource) {
            disposeInteractionSource()
            this.interactionSource = interactionSource
        }
    }

    private fun disposeInteractionSource() {
        interactionSource?.let { interactionSource ->
            focusedInteraction?.let { oldValue ->
                val interaction = FocusInteraction.Unfocus(oldValue)
                interactionSource.tryEmit(interaction)
            }
        }
        focusedInteraction = null
    }

    private fun MutableInteractionSource.emitWithFallback(interaction: Interaction) {
        if (isAttached) {
            coroutineScope.launch {
                emit(interaction)
            }
        } else {
            tryEmit(interaction)
        }
    }
}

private class FocusablePinnableContainerNode : Modifier.Node(),
    CompositionLocalConsumerModifierNode, ObserverModifierNode {
    private var pinnedHandle: PinnableContainer.PinnedHandle? = null
    private var isFocused: Boolean = false

    private fun retrievePinnableContainer(): PinnableContainer? {
        var container: PinnableContainer? = null
        observeReads {
            container = currentValueOf(LocalPinnableContainer)
        }
        return container
    }

    fun setFocus(focused: Boolean) {
        if (focused) {
            val pinnableContainer = retrievePinnableContainer()
            pinnedHandle = pinnableContainer?.pin()
        } else {
            pinnedHandle?.release()
            pinnedHandle = null
        }
        isFocused = focused
    }

    override fun onReset() {
        pinnedHandle?.release()
        pinnedHandle = null
    }

    override fun onObservedReadsChanged() {
        val pinnableContainer = retrievePinnableContainer()
        if (isFocused) {
            pinnedHandle?.release()
            pinnedHandle = pinnableContainer?.pin()
        }
    }
}

private class FocusableSemanticsNode : Modifier.Node(), SemanticsModifierNode,
    FocusRequesterModifierNode {
    private var semanticsConfigurationCache = SemanticsConfiguration()

    private var isFocused = false

    fun setFocus(focused: Boolean) {
        this.isFocused = focused
    }

    override val semanticsConfiguration: SemanticsConfiguration
        get() = semanticsConfigurationCache.apply {
            focused = isFocused
            requestFocus {
                this@FocusableSemanticsNode.requestFocus()
            }
        }
}