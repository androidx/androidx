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
import androidx.compose.foundation.relocation.findBringIntoViewParent
import androidx.compose.foundation.relocation.scrollIntoView
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.requestFocus
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Configure component to be focusable via focus system or accessibility "focus" event.
 *
 * Add this modifier to the element to make it focusable within its bounds.
 *
 * @sample androidx.compose.foundation.samples.FocusableSample
 * @param enabled Controls the enabled state. When `false`, element won't participate in the focus
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 *   [FocusInteraction.Focus] when this element is being focused.
 */
@Stable
fun Modifier.focusable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) =
    this.then(
        if (enabled) {
            FocusableElement(interactionSource)
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
 * In the sample below, each column is a focus group, so pressing the tab key will move focus to all
 * the buttons in column 1 before visiting column 2.
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
@Stable
fun Modifier.focusGroup(): Modifier {
    return this.then(FocusGroupElement)
}

private object FocusGroupElement : ModifierNodeElement<FocusGroupNode>() {
    override fun create() = FocusGroupNode()

    override fun update(node: FocusGroupNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "focusGroup"
    }

    override fun hashCode() = "focusGroup".hashCode()

    override fun equals(other: Any?) = other === this
}

private class FocusGroupNode : DelegatingNode() {
    init {
        delegate(FocusTargetModifierNode(focusability = Focusability.Never))
    }
}

private class FocusableElement(private val interactionSource: MutableInteractionSource?) :
    ModifierNodeElement<FocusableNode>() {

    override fun create(): FocusableNode = FocusableNode(interactionSource)

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

internal class FocusableNode(
    private var interactionSource: MutableInteractionSource?,
    focusability: Focusability = Focusability.Always,
    private val onFocusChange: ((Boolean) -> Unit)? = null
) :
    DelegatingNode(),
    SemanticsModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode,
    TraversableNode {
    override val shouldAutoInvalidate: Boolean = false

    private companion object TraverseKey

    override val traverseKey: Any
        get() = TraverseKey

    private var focusedInteraction: FocusInteraction.Focus? = null
    private var pinnedHandle: PinnableContainer.PinnedHandle? = null
    private var globalLayoutCoordinates: LayoutCoordinates? = null

    private val focusTargetNode =
        delegate(
            FocusTargetModifierNode(
                focusability = focusability,
                onFocusChange = ::onFocusStateChange
            )
        )

    private var requestFocus: (() -> Boolean)? = null

    private val focusedBoundsObserver: FocusedBoundsObserverNode?
        get() =
            if (isAttached) {
                findNearestAncestor(FocusedBoundsObserverNode.TraverseKey)
                    as? FocusedBoundsObserverNode
            } else {
                null
            }

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

    fun update(interactionSource: MutableInteractionSource?) {
        if (this.interactionSource != interactionSource) {
            disposeInteractionSource()
            this.interactionSource = interactionSource
        }
    }

    private fun onFocusStateChange(previousState: FocusState, currentState: FocusState) {
        if (!isAttached) return
        val isFocused = currentState.isFocused
        val wasFocused = previousState.isFocused
        // Ignore cases where we are initialized as unfocused, or moving between different unfocused
        // states, such as Inactive -> ActiveParent.
        if (isFocused == wasFocused) return
        onFocusChange?.invoke(isFocused)
        if (isFocused) {
            val parent = findBringIntoViewParent()
            if (parent != null) {
                val layoutCoordinates = requireLayoutCoordinates()
                coroutineScope.launch {
                    if (isAttached) {
                        parent.scrollIntoView(layoutCoordinates)
                    }
                }
            }
            val pinnableContainer = retrievePinnableContainer()
            pinnedHandle = pinnableContainer?.pin()
            notifyObserverWhenAttached()
        } else {
            pinnedHandle?.release()
            pinnedHandle = null
            focusedBoundsObserver?.onFocusBoundsChanged(null)
        }
        invalidateSemantics()
        emitInteraction(isFocused)
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        focused = focusTargetNode.focusState.isFocused
        if (requestFocus == null) {
            requestFocus = { focusTargetNode.requestFocus() }
        }
        requestFocus(action = requestFocus)
    }

    override fun onReset() {
        pinnedHandle?.release()
        pinnedHandle = null
    }

    override fun onObservedReadsChanged() {
        val pinnableContainer = retrievePinnableContainer()
        if (focusTargetNode.focusState.isFocused) {
            pinnedHandle?.release()
            pinnedHandle = pinnableContainer?.pin()
        }
    }

    // TODO: b/276790428 move this to be lazily delegated when we are focused, we don't need to
    //  be notified of global position changes if we aren't focused.
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        globalLayoutCoordinates = coordinates
        if (!focusTargetNode.focusState.isFocused) return
        if (coordinates.isAttached) {
            notifyObserverWhenAttached()
        } else {
            focusedBoundsObserver?.onFocusBoundsChanged(null)
        }
    }

    private fun retrievePinnableContainer(): PinnableContainer? {
        var container: PinnableContainer? = null
        observeReads { container = currentValueOf(LocalPinnableContainer) }
        return container
    }

    private fun notifyObserverWhenAttached() {
        if (globalLayoutCoordinates != null && globalLayoutCoordinates!!.isAttached) {
            focusedBoundsObserver?.onFocusBoundsChanged(globalLayoutCoordinates)
        }
    }

    private fun emitInteraction(isFocused: Boolean) {
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
            // If this is being called from inside FocusTargetNode's onDetach(), we are still
            // attached, but the scope will be cancelled soon after - so the launch {} might not
            // even start before it is cancelled. We don't want to use CoroutineStart.UNDISPATCHED,
            // or always call tryEmit() as this will break other timing / cause some events to be
            // missed for other cases. Instead just make sure we call tryEmit if we cancel the
            // scope, before we finish emitting.
            val handler =
                coroutineScope.coroutineContext[Job]?.invokeOnCompletion { tryEmit(interaction) }
            coroutineScope.launch {
                emit(interaction)
                handler?.dispose()
            }
        } else {
            tryEmit(interaction)
        }
    }
}
