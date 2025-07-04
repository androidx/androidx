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

package androidx.compose.ui.node

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.LayoutNode.LayoutState
import androidx.compose.ui.unit.Constraints

/**
 * This class works as a layout delegate for [LayoutNode]. It delegates all the measure/layout
 * requests to its [measurePassDelegate] and [lookaheadPassDelegate] depending on whether the
 * request is specific to lookahead.
 */
internal class LayoutNodeLayoutDelegate(internal val layoutNode: LayoutNode) {
    val outerCoordinator: NodeCoordinator
        get() = layoutNode.nodes.outerCoordinator

    val lastConstraints: Constraints?
        get() = measurePassDelegate.lastConstraints

    val lastLookaheadConstraints: Constraints?
        get() = lookaheadPassDelegate?.lastConstraints

    internal val height: Int
        get() = measurePassDelegate.height

    internal val width: Int
        get() = measurePassDelegate.width

    /**
     * Marks that a LayoutNode will not participate in its parent's lookahead pass. More
     * specifically, when the parent does lookahead measurement and placement, the child that is
     * marked [detachedFromParentLookaheadPass] will be skipped. The lookahead measurement and
     * placement will instead happen during approach, as if the node was the lookahead root. This is
     * needed in SubcomposeLayout where some content is only composed in approach pass, therefore
     * they have already missed their parents' lookahead pass. They will instead do a make-up
     * lookahead measurement/placement in approach.
     *
     * This gets set to true via [MeasurePassDelegate.markDetachedFromParentLookaheadPass] and
     * automatically gets unset in `measure` when the measure call comes from parent with
     * layoutState being LookaheadMeasuring or LookaheadLayingOut.
     *
     * Note: This is different than [detachedFromParentLookaheadPlacement] in that the node is
     * detached from both parent's lookahead measurement and placement.
     */
    internal var detachedFromParentLookaheadPass: Boolean = false

    /**
     * This is a flag indicating the node is not going to be placed in the parent's lookahead, but
     * only placed during parent's approach. This is needed because in SubcomposeLayout, a custom
     * measure policy may consider placement unnecessary after measuring the content in lookahead.
     * However, the custom measure policy may require placement of the same content during approach,
     * due to different measured size in approach vs. lookahead. As such, we are marking the nodes
     * that were skipped by its parent's lookahead placement, so that we can do a make-up lookahead
     * placement later in approach.
     *
     * This gets set to `true` when the node is SubComposeLayout's direct children (i.e. folded
     * children of the SubComposeLayout's root) && not placed in lookahead but needs to be placed in
     * approach. [detachedFromParentLookaheadPlacement] gets reset to false when there is a
     * lookahead placement call coming from parent.
     *
     * Note: When [detachedFromParentLookaheadPlacement] is true, it is implied that the lookahead
     * measurement is still triggered by its parent's lookahead measure. In contrast,
     * [detachedFromParentLookaheadPass] indicates that both measurement and placement are detached
     * from parent's lookahead.
     */
    internal var detachedFromParentLookaheadPlacement: Boolean = false

    /**
     * The layout state the node is currently in.
     *
     * The mutation of [layoutState] is confined to [LayoutNodeLayoutDelegate], and is therefore
     * read-only outside this class. This makes the state machine easier to reason about.
     */
    internal var layoutState = LayoutState.Idle

    /**
     * Tracks whether another measure pass is needed for the LayoutNodeLayoutDelegate. Mutation to
     * [measurePending] is confined to LayoutNodeLayoutDelegate. It can only be set true from
     * outside of this class via [markMeasurePending]. It is cleared (i.e. set false) during the
     * measure pass (i.e. in [measurePassDelegate.performMeasure]).
     */
    internal val measurePending: Boolean
        get() = measurePassDelegate.measurePending

    /**
     * Tracks whether another layout pass is needed for the LayoutNodeLayoutDelegate. Mutation to
     * [layoutPending] is confined to this class. It can only be set true from outside of this class
     * via [markLayoutPending]. It is cleared (i.e. set false) during the layout pass (i.e. in
     * [MeasurePassDelegate.layoutChildren]).
     */
    internal val layoutPending: Boolean
        get() = measurePassDelegate.layoutPending

    /**
     * Tracks whether another lookahead measure pass is needed for the LayoutNodeLayoutDelegate.
     * Mutation to [lookaheadMeasurePending] is confined to LayoutNodeLayoutDelegate. It can only be
     * set true from outside of this class via [markLookaheadMeasurePending]. It is cleared (i.e.
     * set false) during the lookahead measure pass (i.e. in [performLookaheadMeasure]).
     */
    internal var lookaheadMeasurePending: Boolean = false

    /**
     * Tracks whether another lookahead layout pass is needed for the LayoutNodeLayoutDelegate.
     * Mutation to [lookaheadLayoutPending] is confined to this class. It can only be set true from
     * outside of this class via [markLookaheadLayoutPending]. It is cleared (i.e. set false) during
     * the layout pass (i.e. in [LookaheadPassDelegate.layoutChildren]).
     */
    internal var lookaheadLayoutPending: Boolean = false

    /**
     * Tracks whether another lookahead layout pass is needed for the LayoutNodeLayoutDelegate for
     * the purposes of calculating alignment lines. After calculating alignment lines, if the
     * [Placeable.PlacementScope.coordinates] have been accessed, there is no need to rerun layout
     * for further alignment lines checks, but [lookaheadLayoutPending] will indicate that the
     * normal placement still needs to be run.
     */
    internal var lookaheadLayoutPendingForAlignment = false

    /**
     * The counter on a parent node which is used by its children to understand the order in which
     * they were placed in the lookahead pass.
     */
    internal var nextChildLookaheadPlaceOrder: Int = 0

    /**
     * The counter on a parent node which is used by its children to understand the order in which
     * they were placed in the main pass.
     */
    internal var nextChildPlaceOrder: Int = 0

    /** Marks the layoutNode dirty for another layout pass. */
    internal fun markLayoutPending() {
        measurePassDelegate.markLayoutPending()
    }

    /** Marks the layoutNode dirty for another measure pass. */
    internal fun markMeasurePending() {
        measurePassDelegate.markMeasurePending()
    }

    /** Marks the layoutNode dirty for another lookahead layout pass. */
    internal fun markLookaheadLayoutPending() {
        lookaheadLayoutPending = true
        lookaheadLayoutPendingForAlignment = true
    }

    /** Marks the layoutNode dirty for another lookahead measure pass. */
    internal fun markLookaheadMeasurePending() {
        lookaheadMeasurePending = true
    }

    internal val alignmentLinesOwner: AlignmentLinesOwner
        get() = measurePassDelegate

    internal val lookaheadAlignmentLinesOwner: AlignmentLinesOwner?
        get() = lookaheadPassDelegate

    /**
     * This is used to track when the [Placeable.PlacementScope.coordinates] have been accessed
     * while placement is run. When the coordinates are accessed during an alignment line query, it
     * indicates that the placement is not final and must be run again so that the correct
     * positioning is done. If the coordinates are not accessed during an alignment lines query (and
     * it isn't just a [LookaheadCapablePlaceable.isShallowPlacing]), then the placement can be
     * considered final and doesn't have to be run again.
     *
     * Also, if coordinates are accessed during placement, then a change in parent coordinates
     * requires placement to be run again.
     */
    var coordinatesAccessedDuringPlacement = false
        set(value) {
            val oldValue = field
            if (oldValue != value) {
                field = value
                if (value && !coordinatesAccessedDuringModifierPlacement) {
                    // if first out of both flags changes to true increment
                    childrenAccessingCoordinatesDuringPlacement++
                } else if (!value && !coordinatesAccessedDuringModifierPlacement) {
                    // if both flags changes to false decrement
                    childrenAccessingCoordinatesDuringPlacement--
                }
            }
        }

    /**
     * Similar to [coordinatesAccessedDuringPlacement], but tracks the coordinates read happening
     * during the modifier layout blocks run.
     */
    var coordinatesAccessedDuringModifierPlacement = false
        set(value) {
            val oldValue = field
            if (oldValue != value) {
                field = value
                if (value && !coordinatesAccessedDuringPlacement) {
                    // if first out of both flags changes to true increment
                    childrenAccessingCoordinatesDuringPlacement++
                } else if (!value && !coordinatesAccessedDuringPlacement) {
                    // if both flags changes to false decrement
                    childrenAccessingCoordinatesDuringPlacement--
                }
            }
        }

    /**
     * The number of children with [coordinatesAccessedDuringPlacement] or have descendants with
     * [coordinatesAccessedDuringPlacement]. This also includes this, if
     * [coordinatesAccessedDuringPlacement] is `true`.
     */
    var childrenAccessingCoordinatesDuringPlacement = 0
        set(value) {
            val oldValue = field
            field = value
            if ((oldValue == 0) != (value == 0)) {
                // A child is either newly listening for coordinates or stopped listening
                val parentLayoutDelegate = layoutNode.parent?.layoutDelegate
                if (parentLayoutDelegate != null) {
                    if (value == 0) {
                        parentLayoutDelegate.childrenAccessingCoordinatesDuringPlacement--
                    } else {
                        parentLayoutDelegate.childrenAccessingCoordinatesDuringPlacement++
                    }
                }
            }
        }

    /** Equivalent flag of [coordinatesAccessedDuringPlacement] but for [lookaheadPassDelegate]. */
    var lookaheadCoordinatesAccessedDuringPlacement = false
        set(value) {
            val oldValue = field
            if (oldValue != value) {
                field = value
                if (value && !lookaheadCoordinatesAccessedDuringModifierPlacement) {
                    // if first out of both flags changes to true increment
                    childrenAccessingLookaheadCoordinatesDuringPlacement++
                } else if (!value && !lookaheadCoordinatesAccessedDuringModifierPlacement) {
                    // if both flags changes to false decrement
                    childrenAccessingLookaheadCoordinatesDuringPlacement--
                }
            }
        }

    /**
     * Equivalent flag of [coordinatesAccessedDuringModifierPlacement] but for
     * [lookaheadPassDelegate].
     */
    var lookaheadCoordinatesAccessedDuringModifierPlacement = false
        set(value) {
            val oldValue = field
            if (oldValue != value) {
                field = value
                if (value && !lookaheadCoordinatesAccessedDuringPlacement) {
                    // if first out of both flags changes to true increment
                    childrenAccessingLookaheadCoordinatesDuringPlacement++
                } else if (!value && !lookaheadCoordinatesAccessedDuringPlacement) {
                    // if both flags changes to false decrement
                    childrenAccessingLookaheadCoordinatesDuringPlacement--
                }
            }
        }

    /**
     * Equivalent flag of [childrenAccessingCoordinatesDuringPlacement] but for
     * [lookaheadPassDelegate].
     *
     * Naturally, this flag should only be affected by the lookahead coordinates access flags.
     */
    var childrenAccessingLookaheadCoordinatesDuringPlacement = 0
        set(value) {
            val oldValue = field
            field = value
            if ((oldValue == 0) != (value == 0)) {
                // A child is either newly listening for coordinates or stopped listening
                val parentLayoutDelegate = layoutNode.parent?.layoutDelegate
                if (parentLayoutDelegate != null) {
                    if (value == 0) {
                        parentLayoutDelegate.childrenAccessingLookaheadCoordinatesDuringPlacement--
                    } else {
                        parentLayoutDelegate.childrenAccessingLookaheadCoordinatesDuringPlacement++
                    }
                }
            }
        }

    /**
     * measurePassDelegate manages the measure/layout and alignmentLine related queries for the
     * actual measure/layout pass.
     */
    internal val measurePassDelegate = MeasurePassDelegate(this)

    /**
     * lookaheadPassDelegate manages the measure/layout and alignmentLine related queries for the
     * lookahead pass.
     */
    internal var lookaheadPassDelegate: LookaheadPassDelegate? = null
        private set

    fun onCoordinatesUsed() {
        val state = layoutNode.layoutState
        if (state == LayoutState.LayingOut || state == LayoutState.LookaheadLayingOut) {
            if (measurePassDelegate.layingOutChildren) {
                coordinatesAccessedDuringPlacement = true
            } else {
                coordinatesAccessedDuringModifierPlacement = true
            }
        }
        if (state == LayoutState.LookaheadLayingOut) {
            if (lookaheadPassDelegate?.layingOutChildren == true) {
                lookaheadCoordinatesAccessedDuringPlacement = true
            } else {
                lookaheadCoordinatesAccessedDuringModifierPlacement = true
            }
        }
    }

    internal fun performLookaheadMeasure(constraints: Constraints) {
        lookaheadPassDelegate?.performMeasure(constraints)
    }

    internal fun ensureLookaheadDelegateCreated() {
        if (lookaheadPassDelegate == null) {
            lookaheadPassDelegate = LookaheadPassDelegate(this)
        }
    }

    fun updateParentData() {
        if (measurePassDelegate.updateParentData()) {
            layoutNode.parent?.requestRemeasure()
        }
        if (lookaheadPassDelegate?.updateParentData() == true) {
            if (layoutNode.isOutMostLookaheadRoot) {
                layoutNode.parent?.requestRemeasure()
            } else {
                layoutNode.parent?.requestLookaheadRemeasure()
            }
        }
    }

    fun invalidateParentData() {
        measurePassDelegate.invalidateParentData()
        lookaheadPassDelegate?.invalidateParentData()
    }

    fun resetAlignmentLines() {
        measurePassDelegate.alignmentLines.reset()
        lookaheadPassDelegate?.alignmentLines?.reset()
    }

    fun markChildrenDirty() {
        measurePassDelegate.childDelegatesDirty = true
        lookaheadPassDelegate?.let { it.childDelegatesDirty = true }
    }

    fun onRemovedFromLookaheadScope() {
        lookaheadPassDelegate = null
        // Clear lookahead invalidations when a LayoutNode is moved out of LookaheadScope.
        lookaheadLayoutPending = false
        lookaheadMeasurePending = false
    }
}

/**
 * Returns if the we are at the lookahead root of the tree, by checking if the parent is has a
 * lookahead root.
 */
internal val LayoutNode.isOutMostLookaheadRoot: Boolean
    get() =
        lookaheadRoot != null &&
            (parent?.lookaheadRoot == null || layoutDelegate.detachedFromParentLookaheadPass)

internal inline fun <T : Measurable> LayoutNode.updateChildMeasurables(
    destination: MutableVector<T>,
    transform: (LayoutNode) -> T,
) {
    forEachChildIndexed { i, layoutNode ->
        if (destination.size <= i) {
            destination.add(transform(layoutNode))
        } else {
            destination[i] = transform(layoutNode)
        }
    }
    destination.removeRange(children.size, destination.size)
}

internal const val MeasuredTwiceErrorMessage: String =
    "measure() may not be called multiple times on the same Measurable. If you want to " +
        "get the content size of the Measurable before calculating the final constraints, " +
        "please use methods like minIntrinsicWidth()/maxIntrinsicWidth() and " +
        "minIntrinsicHeight()/maxIntrinsicHeight()"

/**
 * AlignmentLinesOwner defines APIs that are needed to respond to alignment line changes, and to
 * query alignment line related info.
 *
 * [LookaheadPassDelegate] and [MeasurePassDelegate] both implement this interface, and they
 * encapsulate the difference in alignment lines handling for lookahead pass vs. actual
 * measure/layout pass.
 */
internal interface AlignmentLinesOwner : Measurable {
    /** Whether the AlignmentLinesOwner has been placed. */
    val isPlaced: Boolean

    /** InnerNodeCoordinator of the LayoutNode that the AlignmentLinesOwner operates on. */
    val innerCoordinator: NodeCoordinator

    /**
     * Alignment lines for either lookahead pass or post-lookahead pass, depending on the
     * AlignmentLineOwner.
     */
    val alignmentLines: AlignmentLines

    /**
     * The implementation for laying out children. Different types of AlignmentLinesOwner will
     * layout children for either the lookahead pass, or the layout pass post-lookahead.
     */
    fun layoutChildren()

    /** Recalculate the alignment lines if dirty, and layout children as needed. */
    fun calculateAlignmentLines(): Map<AlignmentLine, Int>

    /**
     * Parent [AlignmentLinesOwner]. This will be the AlignmentLinesOwner for the same pass but for
     * the parent [LayoutNode].
     */
    val parentAlignmentLinesOwner: AlignmentLinesOwner?

    /**
     * This allows iterating all the AlignmentOwners for the same pass for each of the child
     * LayoutNodes
     */
    fun forEachChildAlignmentLinesOwner(block: (AlignmentLinesOwner) -> Unit)

    /**
     * Depending on which pass the [AlignmentLinesOwner] is created for, this could mean
     * requestLookaheadLayout() for the lookahead pass, or requestLayout() for post- lookahead pass.
     */
    fun requestLayout()

    /**
     * Depending on which pass the [AlignmentLinesOwner] is created for, this could mean
     * requestLookaheadMeasure() for the lookahead pass, or requestMeasure() for post- lookahead
     * pass.
     */
    fun requestMeasure()
}

/**
 * Interface for layout delegates, so that they can set the
 * [LookaheadCapablePlaceable.isPlacedUnderMotionFrameOfReference] to the proper placeable.
 */
internal interface MotionReferencePlacementDelegate {
    /**
     * Called when a layout is about to be placed.
     *
     * This updates the corresponding [LookaheadCapablePlaceable]'s
     * [LookaheadCapablePlaceable.isPlacedUnderMotionFrameOfReference] flag IF AND ONLY IF the
     * placement call comes from parent [LookaheadCapablePlaceable]. More specifically, for
     * [LookaheadCapablePlaceable] that are the head of the modifier chain (e.g. outerCoordinator),
     * the placement call doesn't always come from the parent, as the node can be independently
     * replaced. For these [LookaheadCapablePlaceable], we maintain the old
     * [isPlacedUnderMotionFrameOfReference] until the next placement call comes from the parent.
     * This reason is that only placement from parent runs the placement lambda where
     * [Placeable.PlacementScope.withMotionFrameOfReferencePlacement] is invoked. Also note, for
     * [LookaheadCapablePlaceable] that are not the head of the modifier chain, the placement call
     * always comes from the parent.
     *
     * The placeable should be tagged such that its corresponding coordinates reflect the flag in
     * [androidx.compose.ui.layout.LayoutCoordinates.introducesMotionFrameOfReference]. Note that
     * when it's placed on the current frame of reference, it means it doesn't introduce a new frame
     * of reference.
     */
    fun updatePlacedUnderMotionFrameOfReference(newMFR: Boolean)

    /**
     * Flag to indicate whether the [MotionReferencePlacementDelegate] is being placed under motion
     * frame of reference. This is also reflected in
     * [androidx.compose.ui.layout.LayoutCoordinates.introducesMotionFrameOfReference], which is
     * used for local position calculation between coordinates.
     */
    val isPlacedUnderMotionFrameOfReference: Boolean
}
