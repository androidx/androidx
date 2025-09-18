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

package androidx.compose.ui.node

import androidx.collection.mutableObjectIntMapOf
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.classKeyForObject
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusPropertiesModifierNode
import androidx.compose.ui.focus.FocusTargetNode
import androidx.compose.ui.focus.invalidateFocusEvent
import androidx.compose.ui.focus.invalidateFocusProperties
import androidx.compose.ui.input.indirect.IndirectTouchInputModifierNode
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.SoftKeyboardInterceptionModifierNode
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.rotary.RotaryInputModifierNode
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.BeyondBoundsLayoutProviderModifierNode
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.layout.OnPlacedModifier
import androidx.compose.ui.layout.OnPlacedNode
import androidx.compose.ui.layout.OnRemeasuredModifier
import androidx.compose.ui.layout.OnSizeChangedNode
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.semantics.SemanticsModifier
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

@Suppress("NOTHING_TO_INLINE")
@JvmInline
internal value class NodeKind<T>(val mask: Int) {
    inline infix fun or(other: NodeKind<*>): Int = mask or other.mask

    inline infix fun or(other: Int): Int = mask or other
}

@Suppress("NOTHING_TO_INLINE")
internal inline infix fun Int.or(other: NodeKind<*>): Int = this or other.mask

@Suppress("NOTHING_TO_INLINE")
internal inline operator fun Int.contains(value: NodeKind<*>): Boolean = this and value.mask != 0

// For a given NodeCoordinator, the "OnRemeasured" or "OnPlaced" nodes that it is concerned with
// should include its own measureNode if the measureNode happens to implement "OnRemeasured" or
// "OnPlaced". If the measureNode implements any other node interfaces, such as draw, those
// should be visited by the coordinator below them.
internal val NodeKind<*>.includeSelfInTraversal: Boolean
    get() = (mask and Nodes.OnRemeasured.mask != 0) or (mask and Nodes.OnPlaced.mask != 0)

// Note that these don't inherit from Modifier.Node to allow for a single Modifier.Node
// instance to implement multiple Node interfaces

internal object Nodes {
    @JvmStatic
    inline val Any
        get() = NodeKind<Modifier.Node>(0b1 shl 0)

    @JvmStatic
    inline val Layout
        get() = NodeKind<LayoutModifierNode>(0b1 shl 1)

    @JvmStatic
    inline val Draw
        get() = NodeKind<DrawModifierNode>(0b1 shl 2)

    @JvmStatic
    inline val Semantics
        get() = NodeKind<SemanticsModifierNode>(0b1 shl 3)

    @JvmStatic
    inline val PointerInput
        get() = NodeKind<PointerInputModifierNode>(0b1 shl 4)

    @JvmStatic
    inline val Locals
        get() = NodeKind<ModifierLocalModifierNode>(0b1 shl 5)

    @JvmStatic
    inline val ParentData
        get() = NodeKind<ParentDataModifierNode>(0b1 shl 6)

    @JvmStatic
    inline val OnRemeasured
        get() = NodeKind<LayoutAwareModifierNode>(0b1 shl 7)

    @JvmStatic
    inline val GlobalPositionAware
        get() = NodeKind<GlobalPositionAwareModifierNode>(0b1 shl 8)

    @JvmStatic
    inline val ApproachMeasure
        get() = NodeKind<ApproachLayoutModifierNode>(0b1 shl 9)

    @JvmStatic
    inline val FocusTarget
        get() = NodeKind<FocusTargetNode>(0b1 shl 10)

    @JvmStatic
    inline val FocusProperties
        get() = NodeKind<FocusPropertiesModifierNode>(0b1 shl 11)

    @JvmStatic
    inline val FocusEvent
        get() = NodeKind<FocusEventModifierNode>(0b1 shl 12)

    @JvmStatic
    inline val KeyInput
        get() = NodeKind<KeyInputModifierNode>(0b1 shl 13)

    @JvmStatic
    inline val RotaryInput
        get() = NodeKind<RotaryInputModifierNode>(0b1 shl 14)

    @JvmStatic
    inline val CompositionLocalConsumer
        get() = NodeKind<CompositionLocalConsumerModifierNode>(0b1 shl 15)

    @JvmStatic
    inline val SoftKeyboardKeyInput
        get() = NodeKind<SoftKeyboardInterceptionModifierNode>(0b1 shl 17)

    @JvmStatic
    inline val Traversable
        get() = NodeKind<TraversableNode>(0b1 shl 18)

    @JvmStatic
    inline val BringIntoView
        get() = NodeKind<BringIntoViewModifierNode>(0b1 shl 19)

    @JvmStatic
    inline val Unplaced
        get() = NodeKind<OnUnplacedModifierNode>(0b1 shl 20)

    @JvmStatic
    @OptIn(ExperimentalIndirectTouchTypeApi::class)
    inline val IndirectTouchInput
        get() = NodeKind<IndirectTouchInputModifierNode>(0b1 shl 21)

    @JvmStatic
    inline val OnPlaced
        get() = NodeKind<LayoutAwareModifierNode>(0b1 shl 22)

    @JvmStatic
    inline val BeyondBoundsLayout
        get() = NodeKind<BeyondBoundsLayoutProviderModifierNode>(0b1 shl 23)
    // ...
}

internal fun calculateNodeKindSetFrom(element: Modifier.Element): Int {
    var mask = Nodes.Any.mask
    if (element is LayoutModifier) {
        mask = mask or Nodes.Layout
    }
    if (element is DrawModifier) {
        mask = mask or Nodes.Draw
    }
    if (element is SemanticsModifier) {
        mask = mask or Nodes.Semantics
    }
    if (element is PointerInputModifier) {
        mask = mask or Nodes.PointerInput
    }
    if (element is ModifierLocalConsumer || element is ModifierLocalProvider<*>) {
        mask = mask or Nodes.Locals
    }
    @Suppress("DEPRECATION")
    if (element is androidx.compose.ui.focus.FocusEventModifier) {
        mask = mask or Nodes.FocusEvent
    }
    @Suppress("DEPRECATION")
    if (element is androidx.compose.ui.focus.FocusOrderModifier) {
        mask = mask or Nodes.FocusProperties
    }
    if (element is OnGloballyPositionedModifier) {
        mask = mask or Nodes.GlobalPositionAware
    }
    if (element is ParentDataModifier) {
        mask = mask or Nodes.ParentData
    }
    if (element is OnPlacedModifier) {
        mask = mask or Nodes.OnPlaced
    }
    if (element is OnRemeasuredModifier) {
        mask = mask or Nodes.OnRemeasured
    }
    if (element is BringIntoViewModifierNode) {
        mask = mask or Nodes.BringIntoView
    }
    return mask
}

private val classToKindSetMap = mutableObjectIntMapOf<Any>()

internal fun calculateNodeKindSetFrom(node: Modifier.Node): Int {
    // This function does not take delegates into account, as a result, the kindSet will never
    // change, so if it is non-zero, it means we've already calculated it and we can just bail
    // early here.
    if (node.kindSet != 0) return node.kindSet
    return classToKindSetMap.getOrPut(classKeyForObject(node)) {
        var mask = Nodes.Any.mask
        if (node is LayoutModifierNode) {
            mask = mask or Nodes.Layout
        }
        if (node is DrawModifierNode) {
            mask = mask or Nodes.Draw
        }
        if (node is SemanticsModifierNode) {
            mask = mask or Nodes.Semantics
        }
        if (node is PointerInputModifierNode) {
            mask = mask or Nodes.PointerInput
        }
        if (node is ModifierLocalModifierNode) {
            mask = mask or Nodes.Locals
        }
        if (node is ParentDataModifierNode) {
            mask = mask or Nodes.ParentData
        }
        // OnSizeChangedNode and OnPlacedNode implement the combined LayoutAwareModifierNode,
        // But we know they only use one part of the interface, so we want to invalidate smarter.
        if (node is OnPlacedNode) {
            mask = mask or Nodes.OnPlaced
        } else if (node is OnSizeChangedNode) {
            mask = mask or Nodes.OnRemeasured
        } else if (node is LayoutAwareModifierNode) {
            mask = mask or Nodes.OnRemeasured
            mask = mask or Nodes.OnPlaced
        }
        if (node is GlobalPositionAwareModifierNode) {
            mask = mask or Nodes.GlobalPositionAware
        }
        if (node is ApproachLayoutModifierNode) {
            mask = mask or Nodes.ApproachMeasure
        }
        if (node is FocusTargetNode) {
            mask = mask or Nodes.FocusTarget
        }
        if (node is FocusPropertiesModifierNode) {
            mask = mask or Nodes.FocusProperties
        }
        if (node is FocusEventModifierNode) {
            mask = mask or Nodes.FocusEvent
        }
        if (node is KeyInputModifierNode) {
            mask = mask or Nodes.KeyInput
        }
        if (node is RotaryInputModifierNode) {
            mask = mask or Nodes.RotaryInput
        }
        if (node is CompositionLocalConsumerModifierNode) {
            mask = mask or Nodes.CompositionLocalConsumer
        }
        if (node is SoftKeyboardInterceptionModifierNode) {
            mask = mask or Nodes.SoftKeyboardKeyInput
        }
        if (node is TraversableNode) {
            mask = mask or Nodes.Traversable
        }
        if (node is BringIntoViewModifierNode) {
            mask = mask or Nodes.BringIntoView
        }
        if (node is OnUnplacedModifierNode) {
            mask = mask or Nodes.Unplaced
        }
        @OptIn(ExperimentalIndirectTouchTypeApi::class)
        if (node is IndirectTouchInputModifierNode) {
            mask = mask or Nodes.IndirectTouchInput
        }
        if (node is BeyondBoundsLayoutProviderModifierNode) {
            mask = mask or Nodes.BeyondBoundsLayout
        }
        // ...
        mask
    }
}

@Suppress("ConstPropertyName") private const val Updated = 0
@Suppress("ConstPropertyName") private const val Inserted = 1
@Suppress("ConstPropertyName") private const val Removed = 2

internal fun autoInvalidateRemovedNode(node: Modifier.Node) {
    checkPrecondition(node.isAttached) { "autoInvalidateRemovedNode called on unattached node" }
    autoInvalidateNodeIncludingDelegates(node, 0.inv(), Removed)
}

internal fun autoInvalidateInsertedNode(node: Modifier.Node) {
    checkPrecondition(node.isAttached) { "autoInvalidateInsertedNode called on unattached node" }
    autoInvalidateNodeIncludingDelegates(node, 0.inv(), Inserted)
}

internal fun autoInvalidateUpdatedNode(node: Modifier.Node) {
    checkPrecondition(node.isAttached) { "autoInvalidateUpdatedNode called on unattached node" }
    autoInvalidateNodeIncludingDelegates(node, 0.inv(), Updated)
}

internal fun autoInvalidateNodeIncludingDelegates(
    node: Modifier.Node,
    remainingSet: Int,
    phase: Int,
) {
    if (node is DelegatingNode) {
        autoInvalidateNodeSelf(node, node.selfKindSet and remainingSet, phase)
        val newRemaining = remainingSet and node.selfKindSet.inv()
        node.forEachImmediateDelegate {
            autoInvalidateNodeIncludingDelegates(it, newRemaining, phase)
        }
    } else {
        autoInvalidateNodeSelf(node, node.kindSet and remainingSet, phase)
    }
}

private fun autoInvalidateNodeSelf(node: Modifier.Node, selfKindSet: Int, phase: Int) {
    // TODO(lmr): Implementing it this way means that delegates of an autoInvalidate=false node will
    //  still get invalidated. Not sure if that's what we want or not.
    // Don't invalidate the node if it marks itself as autoInvalidate = false.
    if (phase == Updated && !node.shouldAutoInvalidate) return
    if (Nodes.Layout in selfKindSet && node is LayoutModifierNode) {
        node.invalidateMeasurement()
        if (phase == Removed) {
            val coordinator = node.requireCoordinator(Nodes.Layout)
            coordinator.onRelease()
        }
    }
    if (Nodes.OnRemeasured in selfKindSet) {
        // No need to invalidate measurement when removing a LayoutAwareModifierNode,
        // as these won't be invoked anyway
        if (phase != Removed) {
            node.requireLayoutNode().invalidateMeasurements()
        }
    }
    if (Nodes.OnPlaced in selfKindSet) {
        // No need to invalidate layout when removing a LayoutAwareModifierNode, as these won't be
        // invoked anyway
        if (phase != Removed) {
            node.requireLayoutNode().requestRelayout()
        }
    }
    if (Nodes.GlobalPositionAware in selfKindSet && node is GlobalPositionAwareModifierNode) {
        if (phase == Inserted) {
            node.requireLayoutNode().globallyPositionedObservers++
        } else if (phase == Removed) {
            node.requireLayoutNode().globallyPositionedObservers--
        }
        // No need to invalidate when removing a GlobalPositionAwareModifierNode, as these won't be
        // invoked anyway
        if (phase != Removed) {
            node.requireLayoutNode().invalidateOnPositioned()
        }
    }
    if (Nodes.Draw in selfKindSet && node is DrawModifierNode) {
        node.invalidateDraw()
    }
    if (Nodes.Semantics in selfKindSet && node is SemanticsModifierNode) {
        node.requireLayoutNode().isSemanticsInvalidated = true
    }
    if (Nodes.ParentData in selfKindSet && node is ParentDataModifierNode) {
        node.invalidateParentData()
    }
    if (
        Nodes.FocusProperties in selfKindSet &&
            node is FocusPropertiesModifierNode &&
            node.specifiesCanFocusProperty()
    ) {
        node.invalidateFocusProperties()
    }
    if (Nodes.FocusEvent in selfKindSet && node is FocusEventModifierNode) {
        node.invalidateFocusEvent()
    }
}

/**
 * This function checks if the FocusProperties node has set the canFocus [FocusProperties.canFocus]
 * property.
 *
 * We use a singleton CanFocusChecker to prevent extra allocations, and in doing so, we assume that
 * there won't be multiple concurrent calls of this function. This is not an issue since this is
 * called from the main thread, but if this changes in the future, replace the
 * [CanFocusChecker.reset] call with a new [FocusProperties] object for every invocation.
 */
private fun FocusPropertiesModifierNode.specifiesCanFocusProperty(): Boolean {
    CanFocusChecker.reset()
    applyFocusProperties(CanFocusChecker)
    return CanFocusChecker.isCanFocusSet()
}

private object CanFocusChecker : FocusProperties {
    private var canFocusValue: Boolean? = null
    override var canFocus: Boolean
        get() = checkPreconditionNotNull(canFocusValue) { "canFocus is read before it is written" }
        set(value) {
            canFocusValue = value
        }

    fun isCanFocusSet(): Boolean = canFocusValue != null

    fun reset() {
        canFocusValue = null
    }
}

internal fun calculateNodeKindSetFromIncludingDelegates(node: Modifier.Node): Int {
    return if (node is DelegatingNode) {
        var mask = node.selfKindSet
        node.forEachImmediateDelegate {
            mask = mask or calculateNodeKindSetFromIncludingDelegates(it)
        }
        mask
    } else {
        calculateNodeKindSetFrom(node)
    }
}
