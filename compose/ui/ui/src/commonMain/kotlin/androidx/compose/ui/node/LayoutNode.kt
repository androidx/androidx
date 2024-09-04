/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.CompositionLocalMap
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.LayoutNodeSubcompositionsState
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.node.LayoutNode.LayoutState.Idle
import androidx.compose.ui.node.LayoutNode.LayoutState.LayingOut
import androidx.compose.ui.node.LayoutNode.LayoutState.LookaheadLayingOut
import androidx.compose.ui.node.LayoutNode.LayoutState.LookaheadMeasuring
import androidx.compose.ui.node.LayoutNode.LayoutState.Measuring
import androidx.compose.ui.node.Nodes.Draw
import androidx.compose.ui.node.Nodes.PointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.simpleIdentityToString
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.generateSemanticsId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.InteropViewFactoryHolder

/** Enable to log changes to the LayoutNode tree. This logging is quite chatty. */
private const val DebugChanges = false

private val DefaultDensity = Density(1f)

/** An element in the layout hierarchy, built with compose UI. */
@OptIn(InternalComposeUiApi::class)
internal class LayoutNode(
    // Virtual LayoutNode is the temporary concept allows us to a node which is not a real node,
    // but just a holder for its children - allows us to combine some children into something we
    // can subcompose in(LayoutNode) without being required to define it as a real layout - we
    // don't want to define the layout strategy for such nodes, instead the children of the
    // virtual nodes will be treated as the direct children of the virtual node parent.
    // This whole concept will be replaced with a proper subcomposition logic which allows to
    // subcompose multiple times into the same LayoutNode and define offsets.
    private val isVirtual: Boolean = false,
    // The unique semantics ID that is used by all semantics modifiers attached to this LayoutNode.
    // TODO(b/281907968): Implement this with a getter that returns the compositeKeyHash.
    override var semanticsId: Int = generateSemanticsId()
) :
    ComposeNodeLifecycleCallback,
    Remeasurement,
    OwnerScope,
    LayoutInfo,
    ComposeUiNode,
    InteroperableComposeUiNode,
    Owner.OnLayoutCompletedListener {

    internal var offsetFromRoot: IntOffset = IntOffset.Max
    internal var lastSize: IntSize = IntSize.Zero
    internal var outerToInnerOffset: IntOffset = IntOffset.Max
    internal var outerToInnerOffsetDirty: Boolean = true

    var forceUseOldLayers: Boolean = false

    override var compositeKeyHash: Int = 0

    internal var isVirtualLookaheadRoot: Boolean = false

    /**
     * This lookaheadRoot references the closest root to the LayoutNode, not the top-level lookahead
     * root.
     */
    internal var lookaheadRoot: LayoutNode? = null
        private set(newRoot) {
            if (newRoot != field) {
                field = newRoot
                if (newRoot != null) {
                    layoutDelegate.ensureLookaheadDelegateCreated()
                    forEachCoordinatorIncludingInner { it.ensureLookaheadDelegateCreated() }
                }
                invalidateMeasurements()
            }
        }

    val isPlacedInLookahead: Boolean?
        get() = lookaheadPassDelegate?.isPlaced

    private var virtualChildrenCount = 0

    // the list of nodes containing the virtual children as is
    private val _foldedChildren =
        MutableVectorWithMutationTracking(mutableVectorOf<LayoutNode>()) {
            layoutDelegate.markChildrenDirty()
        }
    internal val foldedChildren: List<LayoutNode>
        get() = _foldedChildren.asList()

    // the list of nodes where the virtual children are unfolded (their children are represented
    // as our direct children)
    private var _unfoldedChildren: MutableVector<LayoutNode>? = null

    private fun recreateUnfoldedChildrenIfDirty() {
        if (unfoldedVirtualChildrenListDirty) {
            unfoldedVirtualChildrenListDirty = false
            val unfoldedChildren =
                _unfoldedChildren ?: mutableVectorOf<LayoutNode>().also { _unfoldedChildren = it }
            unfoldedChildren.clear()
            _foldedChildren.forEach {
                if (it.isVirtual) {
                    unfoldedChildren.addAll(it._children)
                } else {
                    unfoldedChildren.add(it)
                }
            }
            layoutDelegate.markChildrenDirty()
        }
    }

    internal val childMeasurables: List<Measurable>
        get() = measurePassDelegate.childDelegates

    internal val childLookaheadMeasurables: List<Measurable>
        get() = lookaheadPassDelegate!!.childDelegates

    // when the list of our children is modified it will be set to true if we are a virtual node
    // or it will be set to true on a parent if the parent is a virtual node
    private var unfoldedVirtualChildrenListDirty = false

    private fun invalidateUnfoldedVirtualChildren() {
        if (virtualChildrenCount > 0) {
            unfoldedVirtualChildrenListDirty = true
        }
        if (isVirtual) {
            // Invalidate all virtual unfolded parent until we reach a non-virtual one
            this._foldedParent?.invalidateUnfoldedVirtualChildren()
        }
    }

    /**
     * This should **not** be mutated or even accessed directly from outside of [LayoutNode]. Use
     * [forEachChild]/[forEachChildIndexed] when there's a need to iterate through the vector.
     */
    internal val _children: MutableVector<LayoutNode>
        get() {
            updateChildrenIfDirty()
            return if (virtualChildrenCount == 0) {
                _foldedChildren.vector
            } else {
                _unfoldedChildren!!
            }
        }

    /** Update children if the list is not up to date. */
    internal fun updateChildrenIfDirty() {
        if (virtualChildrenCount > 0) {
            recreateUnfoldedChildrenIfDirty()
        }
    }

    inline fun forEachChild(block: (LayoutNode) -> Unit) = _children.forEach(block)

    inline fun forEachChildIndexed(block: (Int, LayoutNode) -> Unit) =
        _children.forEachIndexed(block)

    /** The children of this LayoutNode, controlled by [insertAt], [move], and [removeAt]. */
    internal val children: List<LayoutNode>
        get() = _children.asMutableList()

    /**
     * The parent node in the LayoutNode hierarchy. This is `null` when the [LayoutNode] is not
     * attached to a hierarchy or is the root of the hierarchy.
     */
    private var _foldedParent: LayoutNode? = null

    /*
     * The parent node in the LayoutNode hierarchy, skipping over virtual nodes.
     */
    internal val parent: LayoutNode?
        get() {
            var parent = _foldedParent
            while (parent?.isVirtual == true) {
                parent = parent._foldedParent
            }
            return parent
        }

    /** The view system [Owner]. This `null` until [attach] is called */
    internal var owner: Owner? = null
        private set

    /**
     * The [InteropViewFactoryHolder] associated with this node, which is used to instantiate and
     * manage platform View instances that are hosted in Compose.
     */
    internal var interopViewFactoryHolder: InteropViewFactoryHolder? = null

    @InternalComposeUiApi
    override fun getInteropView(): InteropView? = interopViewFactoryHolder?.getInteropView()

    /**
     * Returns true if this [LayoutNode] currently has an [LayoutNode.owner]. Semantically, this
     * means that the LayoutNode is currently a part of a component tree.
     */
    override val isAttached: Boolean
        get() = owner != null

    /**
     * The tree depth of the [LayoutNode]. This is valid only when it is attached to a hierarchy.
     */
    internal var depth: Int = 0

    /**
     * The layout state the node is currently in.
     *
     * The mutation of [layoutState] is confined to [LayoutNode], and is therefore read-only outside
     * LayoutNode. This makes the state machine easier to reason about.
     */
    internal val layoutState
        get() = layoutDelegate.layoutState

    /**
     * The lookahead pass delegate for the [LayoutNode]. This should only be used for measure and
     * layout related impl during *lookahead*. For the actual measure & layout, use
     * [measurePassDelegate].
     */
    internal val lookaheadPassDelegate
        get() = layoutDelegate.lookaheadPassDelegate

    /**
     * The measure pass delegate for the [LayoutNode]. This delegate is responsible for the actual
     * measure & layout, after lookahead if any.
     */
    internal val measurePassDelegate
        get() = layoutDelegate.measurePassDelegate

    /** [requestRemeasure] calls will be ignored while this flag is true. */
    private var ignoreRemeasureRequests = false

    /**
     * Inserts a child [LayoutNode] at a particular index. If this LayoutNode [owner] is not `null`
     * then [instance] will become [attach]ed also. [instance] must have a `null` [parent].
     */
    internal fun insertAt(index: Int, instance: LayoutNode) {
        checkPrecondition(instance._foldedParent == null) {
            "Cannot insert $instance because it already has a parent." +
                " This tree: " +
                debugTreeToString() +
                " Other tree: " +
                instance._foldedParent?.debugTreeToString()
        }
        checkPrecondition(instance.owner == null) {
            "Cannot insert $instance because it already has an owner." +
                " This tree: " +
                debugTreeToString() +
                " Other tree: " +
                instance.debugTreeToString()
        }

        if (DebugChanges) {
            println("$instance added to $this at index $index")
        }

        instance._foldedParent = this
        _foldedChildren.add(index, instance)
        onZSortedChildrenInvalidated()

        if (instance.isVirtual) {
            virtualChildrenCount++
        }
        invalidateUnfoldedVirtualChildren()

        val owner = this.owner
        if (owner != null) {
            instance.attach(owner)
        }

        if (instance.layoutDelegate.childrenAccessingCoordinatesDuringPlacement > 0) {
            layoutDelegate.childrenAccessingCoordinatesDuringPlacement++
        }
    }

    internal fun onZSortedChildrenInvalidated() {
        if (isVirtual) {
            parent?.onZSortedChildrenInvalidated()
        } else {
            zSortedChildrenInvalidated = true
        }
    }

    /** Removes one or more children, starting at [index]. */
    internal fun removeAt(index: Int, count: Int) {
        requirePrecondition(count >= 0) { "count ($count) must be greater than 0" }
        for (i in index + count - 1 downTo index) {
            // Call detach callbacks before removing from _foldedChildren, so the child is still
            // visible to parents traversing downwards, such as when clearing focus.
            onChildRemoved(_foldedChildren[i])
            val child = _foldedChildren.removeAt(i)
            if (DebugChanges) {
                println("$child removed from $this at index $i")
            }
        }
    }

    /** Removes all children. */
    internal fun removeAll() {
        for (i in _foldedChildren.size - 1 downTo 0) {
            onChildRemoved(_foldedChildren[i])
        }
        _foldedChildren.clear()

        if (DebugChanges) {
            println("Removed all children from $this")
        }
    }

    private fun onChildRemoved(child: LayoutNode) {
        if (child.layoutDelegate.childrenAccessingCoordinatesDuringPlacement > 0) {
            layoutDelegate.childrenAccessingCoordinatesDuringPlacement--
        }
        if (owner != null) {
            child.detach()
        }
        child._foldedParent = null
        child.outerCoordinator.wrappedBy = null

        if (child.isVirtual) {
            virtualChildrenCount--
            child._foldedChildren.forEach { it.outerCoordinator.wrappedBy = null }
        }
        invalidateUnfoldedVirtualChildren()
        onZSortedChildrenInvalidated()
    }

    /**
     * Moves [count] elements starting at index [from] to index [to]. The [to] index is related to
     * the position before the change, so, for example, to move an element at position 1 to after
     * the element at position 2, [from] should be `1` and [to] should be `3`. If the elements were
     * LayoutNodes A B C D E, calling `move(1, 3, 1)` would result in the LayoutNodes being
     * reordered to A C B D E.
     */
    internal fun move(from: Int, to: Int, count: Int) {
        if (from == to) {
            return // nothing to do
        }

        for (i in 0 until count) {
            // if "from" is after "to," the from index moves because we're inserting before it
            val fromIndex = if (from > to) from + i else from
            val toIndex = if (from > to) to + i else to + count - 2
            val child = _foldedChildren.removeAt(fromIndex)

            if (DebugChanges) {
                println("$child moved in $this from index $fromIndex to $toIndex")
            }

            _foldedChildren.add(toIndex, child)
        }
        onZSortedChildrenInvalidated()

        invalidateUnfoldedVirtualChildren()
        invalidateMeasurements()
    }

    private var _collapsedSemantics: SemanticsConfiguration? = null

    internal fun invalidateSemantics() {
        _collapsedSemantics = null
        // TODO(lmr): this ends up scheduling work that diffs the entire tree, but we should
        //  eventually move to marking just this node as invalidated since we are invalidating
        //  on a per-node level. This should preserve current behavior for now.
        requireOwner().onSemanticsChange()
    }

    internal val collapsedSemantics: SemanticsConfiguration?
        get() {
            // TODO: investigate if there's a better way to approach "half attached" state and
            // whether or not deactivated nodes should be considered removed or not.
            if (!isAttached || isDeactivated) return null

            if (!nodes.has(Nodes.Semantics) || _collapsedSemantics != null) {
                return _collapsedSemantics
            }

            var config = SemanticsConfiguration()
            requireOwner().snapshotObserver.observeSemanticsReads(this) {
                nodes.tailToHead(Nodes.Semantics) {
                    if (it.shouldClearDescendantSemantics) {
                        config = SemanticsConfiguration()
                        config.isClearingSemantics = true
                    }
                    if (it.shouldMergeDescendantSemantics) {
                        config.isMergingSemanticsOfDescendants = true
                    }
                    with(config) { with(it) { applySemantics() } }
                }
            }
            _collapsedSemantics = config
            return config
        }

    /**
     * Set the [Owner] of this LayoutNode. This LayoutNode must not already be attached. [owner]
     * must match its [parent].[owner].
     */
    internal fun attach(owner: Owner) {
        checkPrecondition(this.owner == null) {
            "Cannot attach $this as it already is attached.  Tree: " + debugTreeToString()
        }
        checkPrecondition(_foldedParent == null || _foldedParent?.owner == owner) {
            "Attaching to a different owner($owner) than the parent's owner(${parent?.owner})." +
                " This tree: " +
                debugTreeToString() +
                " Parent tree: " +
                _foldedParent?.debugTreeToString()
        }
        val parent = this.parent
        if (parent == null) {
            // it is a root node and attached root nodes are always placed (as there is no parent
            // to place them explicitly)
            measurePassDelegate.isPlaced = true
            lookaheadPassDelegate?.let { it.isPlaced = true }
        }

        // Use the inner coordinator of first non-virtual parent
        outerCoordinator.wrappedBy = parent?.innerCoordinator

        this.owner = owner
        this.depth = (parent?.depth ?: -1) + 1

        pendingModifier?.let { applyModifier(it) }
        pendingModifier = null

        if (nodes.has(Nodes.Semantics)) {
            invalidateSemantics()
        }
        owner.onAttach(this)

        // Update lookahead root when attached. For nested cases, we'll always use the
        // closest lookahead root
        if (isVirtualLookaheadRoot) {
            lookaheadRoot = this
        } else {
            // Favor lookahead root from parent than locally created scope, unless current node
            // is a virtual lookahead root
            lookaheadRoot = _foldedParent?.lookaheadRoot ?: lookaheadRoot
            if (lookaheadRoot == null && nodes.has(Nodes.ApproachMeasure)) {
                // This could happen when movableContent containing intermediateLayout is moved
                lookaheadRoot = this
            }
        }
        if (!isDeactivated) {
            nodes.markAsAttached()
        }
        _foldedChildren.forEach { child -> child.attach(owner) }
        if (!isDeactivated) {
            nodes.runAttachLifecycle()
        }

        invalidateMeasurements()
        parent?.invalidateMeasurements()

        forEachCoordinatorIncludingInner { it.onLayoutNodeAttach() }
        onAttach?.invoke(owner)

        layoutDelegate.updateParentData()
    }

    /**
     * Remove the LayoutNode from the [Owner]. The [owner] must not be `null` before this call and
     * its [parent]'s [owner] must be `null` before calling this. This will also [detach] all
     * children. After executing, the [owner] will be `null`.
     */
    internal fun detach() {
        val owner = owner
        checkPreconditionNotNull(owner) {
            "Cannot detach node that is already detached!  Tree: " + parent?.debugTreeToString()
        }
        val parent = this.parent
        if (parent != null) {
            parent.invalidateLayer()
            parent.invalidateMeasurements()
            measurePassDelegate.measuredByParent = UsageByParent.NotUsed
            lookaheadPassDelegate?.let { it.measuredByParent = UsageByParent.NotUsed }
        }
        layoutDelegate.resetAlignmentLines()
        onDetach?.invoke(owner)

        if (nodes.has(Nodes.Semantics)) {
            invalidateSemantics()
        }
        nodes.runDetachLifecycle()
        ignoreRemeasureRequests { _foldedChildren.forEach { child -> child.detach() } }
        nodes.markAsDetached()
        owner.onDetach(this)
        this.owner = null

        lookaheadRoot = null
        depth = 0
        measurePassDelegate.onNodeDetached()
        lookaheadPassDelegate?.onNodeDetached()
    }

    private val _zSortedChildren = mutableVectorOf<LayoutNode>()
    private var zSortedChildrenInvalidated = true

    /**
     * Returns the children list sorted by their [LayoutNode.zIndex] first (smaller first) and the
     * order they were placed via [Placeable.placeAt] by parent (smaller first). Please note that
     * this list contains not placed items as well, so you have to manually filter them.
     *
     * Note that the object is reused so you shouldn't save it for later.
     */
    @PublishedApi
    internal val zSortedChildren: MutableVector<LayoutNode>
        get() {
            if (zSortedChildrenInvalidated) {
                _zSortedChildren.clear()
                _zSortedChildren.addAll(_children)
                _zSortedChildren.sortWith(ZComparator)
                zSortedChildrenInvalidated = false
            }
            return _zSortedChildren
        }

    override val isValidOwnerScope: Boolean
        get() = isAttached

    override fun toString(): String {
        return "${simpleIdentityToString(this, null)} children: ${children.size} " +
            "measurePolicy: $measurePolicy"
    }

    internal val hasFixedInnerContentConstraints: Boolean
        get() {
            // it is the constraints we have after all the modifiers applied on this node,
            // the one to be passed into user provided [measurePolicy.measure]. if those
            // constraints are fixed this means the children size changes can't affect
            // this LayoutNode size.
            val innerContentConstraints = innerCoordinator.lastMeasurementConstraints
            return innerContentConstraints.hasFixedWidth && innerContentConstraints.hasFixedHeight
        }

    /** Call this method from the debugger to see a dump of the LayoutNode tree structure */
    @Suppress("unused")
    private fun debugTreeToString(depth: Int = 0): String {
        val tree = StringBuilder()
        for (i in 0 until depth) {
            tree.append("  ")
        }
        tree.append("|-")
        tree.append(toString())
        tree.append('\n')

        forEachChild { child -> tree.append(child.debugTreeToString(depth + 1)) }

        var treeString = tree.toString()
        if (depth == 0) {
            // Delete trailing newline
            treeString = treeString.substring(0, treeString.length - 1)
        }

        return treeString
    }

    internal abstract class NoIntrinsicsMeasurePolicy(private val error: String) : MeasurePolicy {
        override fun IntrinsicMeasureScope.minIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = error(error)

        override fun IntrinsicMeasureScope.minIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = error(error)

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = error(error)

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = error(error)
    }

    /** Blocks that define the measurement and intrinsic measurement of the layout. */
    override var measurePolicy: MeasurePolicy = ErrorMeasurePolicy
        set(value) {
            if (field != value) {
                field = value
                intrinsicsPolicy?.updateFrom(measurePolicy)
                invalidateMeasurements()
            }
        }

    /**
     * The intrinsic measurements of this layout, backed up by states to trigger correct
     * remeasurement for layouts using the intrinsics of this layout when the [measurePolicy] is
     * changing.
     */
    private var intrinsicsPolicy: IntrinsicsPolicy? = null

    private fun getOrCreateIntrinsicsPolicy(): IntrinsicsPolicy {
        return intrinsicsPolicy
            ?: IntrinsicsPolicy(this, measurePolicy).also { intrinsicsPolicy = it }
    }

    fun minLookaheadIntrinsicWidth(height: Int) =
        getOrCreateIntrinsicsPolicy().minLookaheadIntrinsicWidth(height)

    fun minLookaheadIntrinsicHeight(width: Int) =
        getOrCreateIntrinsicsPolicy().minLookaheadIntrinsicHeight(width)

    fun maxLookaheadIntrinsicWidth(height: Int) =
        getOrCreateIntrinsicsPolicy().maxLookaheadIntrinsicWidth(height)

    fun maxLookaheadIntrinsicHeight(width: Int) =
        getOrCreateIntrinsicsPolicy().maxLookaheadIntrinsicHeight(width)

    fun minIntrinsicWidth(height: Int) = getOrCreateIntrinsicsPolicy().minIntrinsicWidth(height)

    fun minIntrinsicHeight(width: Int) = getOrCreateIntrinsicsPolicy().minIntrinsicHeight(width)

    fun maxIntrinsicWidth(height: Int) = getOrCreateIntrinsicsPolicy().maxIntrinsicWidth(height)

    fun maxIntrinsicHeight(width: Int) = getOrCreateIntrinsicsPolicy().maxIntrinsicHeight(width)

    /** The screen density to be used by this layout. */
    override var density: Density = DefaultDensity
        set(value) {
            if (field != value) {
                field = value
                onDensityOrLayoutDirectionChanged()

                nodes.headToTail {
                    if (it.isKind(PointerInput)) {
                        (it as PointerInputModifierNode).onDensityChange()
                    } else if (it is CacheDrawModifierNode) {
                        // b/340662451 Replace both usages of DelegatableNode#onDensityChanged and
                        // DelegatableNode#onLayoutDirectionChanged when API changes can be
                        // made again
                        it.invalidateDrawCache()
                    }
                }
            }
        }

    /** The layout direction of the layout node. */
    override var layoutDirection: LayoutDirection = LayoutDirection.Ltr
        set(value) {
            if (field != value) {
                field = value
                onDensityOrLayoutDirectionChanged()

                nodes.headToTail(Draw) {
                    if (it is CacheDrawModifierNode) {
                        it.invalidateDrawCache()
                    }
                }
            }
        }

    override var viewConfiguration: ViewConfiguration = DummyViewConfiguration
        set(value) {
            if (field != value) {
                field = value

                nodes.headToTail(type = PointerInput) { it.onViewConfigurationChange() }
            }
        }

    override var compositionLocalMap = CompositionLocalMap.Empty
        set(value) {
            field = value
            density = value[LocalDensity]
            layoutDirection = value[LocalLayoutDirection]
            viewConfiguration = value[LocalViewConfiguration]
            nodes.headToTail(Nodes.CompositionLocalConsumer) { modifierNode ->
                val delegatedNode = modifierNode.node
                if (delegatedNode.isAttached) {
                    autoInvalidateUpdatedNode(delegatedNode)
                } else {
                    delegatedNode.updatedNodeAwaitingAttachForInvalidation = true
                }
            }
        }

    private fun onDensityOrLayoutDirectionChanged() {
        // TODO(b/242120396): it seems like we need to update some densities in the node
        // coordinators here
        // measure/layout modifiers on the node
        invalidateMeasurements()
        // draw modifiers on the node
        parent?.invalidateLayer()
        // and draw modifiers after graphics layers on the node
        invalidateLayers()
    }

    /** The measured width of this layout and all of its [modifier]s. Shortcut for `size.width`. */
    override val width: Int
        get() = layoutDelegate.width

    /**
     * The measured height of this layout and all of its [modifier]s. Shortcut for `size.height`.
     */
    override val height: Int
        get() = layoutDelegate.height

    internal val alignmentLinesRequired: Boolean
        get() =
            layoutDelegate.run {
                alignmentLinesOwner.alignmentLines.required ||
                    lookaheadAlignmentLinesOwner?.alignmentLines?.required == true
            }

    internal val mDrawScope: LayoutNodeDrawScope
        get() = requireOwner().sharedDrawScope

    /**
     * Whether or not this [LayoutNode] and all of its parents have been placed in the hierarchy.
     */
    override val isPlaced: Boolean
        get() = measurePassDelegate.isPlaced

    /**
     * Whether or not this [LayoutNode] was placed by its parent. The node can still be considered
     * not placed if some of the modifiers on it not placed the placeable.
     */
    val isPlacedByParent: Boolean
        get() = measurePassDelegate.isPlacedByParent

    /**
     * The order in which this node was placed by its parent during the previous `layoutChildren`.
     * Before the placement the order is set to [NotPlacedPlaceOrder] to all the children. Then
     * every placed node assigns this variable to [parent]s MeasurePassDelegate's
     * nextChildPlaceOrder and increments this counter. Not placed items will still have
     * [NotPlacedPlaceOrder] set.
     */
    internal val placeOrder: Int
        get() = measurePassDelegate.placeOrder

    /** Remembers how the node was measured by the parent. */
    internal val measuredByParent: UsageByParent
        get() = measurePassDelegate.measuredByParent

    /** Remembers how the node was measured by the parent in lookahead. */
    internal val measuredByParentInLookahead: UsageByParent
        get() = lookaheadPassDelegate?.measuredByParent ?: UsageByParent.NotUsed

    /** Remembers how the node was measured using intrinsics by an ancestor. */
    internal var intrinsicsUsageByParent: UsageByParent = UsageByParent.NotUsed

    /**
     * We must cache a previous value of [intrinsicsUsageByParent] because measurement is sometimes
     * skipped. When it is skipped, the subtree must be restored to this value.
     */
    private var previousIntrinsicsUsageByParent: UsageByParent = UsageByParent.NotUsed

    @Deprecated("Temporary API to support ConstraintLayout prototyping.")
    internal var canMultiMeasure: Boolean = false

    internal val nodes = NodeChain(this)
    internal val innerCoordinator: NodeCoordinator
        get() = nodes.innerCoordinator

    internal val layoutDelegate = LayoutNodeLayoutDelegate(this)
    internal val outerCoordinator: NodeCoordinator
        get() = nodes.outerCoordinator

    /**
     * zIndex defines the drawing order of the LayoutNode. Children with larger zIndex are drawn on
     * top of others (the original order is used for the nodes with the same zIndex). Default zIndex
     * is 0. We use sum of the values passed as zIndex to place() by the parent layout and all the
     * applied modifiers.
     */
    private val zIndex: Float
        get() = measurePassDelegate.zIndex

    /** The inner state associated with [androidx.compose.ui.layout.SubcomposeLayout]. */
    internal var subcompositionsState: LayoutNodeSubcompositionsState? = null

    /** The inner-most layer coordinator. Used for performance for NodeCoordinator.findLayer(). */
    private var _innerLayerCoordinator: NodeCoordinator? = null
    internal var innerLayerCoordinatorIsDirty = true
    internal val innerLayerCoordinator: NodeCoordinator?
        get() {
            if (innerLayerCoordinatorIsDirty) {
                var coordinator: NodeCoordinator? = innerCoordinator
                val final = outerCoordinator.wrappedBy
                _innerLayerCoordinator = null
                while (coordinator != final) {
                    if (coordinator?.layer != null) {
                        _innerLayerCoordinator = coordinator
                        break
                    }
                    coordinator = coordinator?.wrappedBy
                }
            }
            val layerCoordinator = _innerLayerCoordinator
            if (layerCoordinator != null) {
                checkPreconditionNotNull(layerCoordinator.layer) { "layer was not set" }
            }
            return layerCoordinator
        }

    /**
     * Invalidates the inner-most layer as part of this LayoutNode or from the containing
     * LayoutNode. This is added for performance so that NodeCoordinator.invalidateLayer() can be
     * faster.
     */
    internal fun invalidateLayer() {
        val innerLayerCoordinator = innerLayerCoordinator
        if (innerLayerCoordinator != null) {
            innerLayerCoordinator.invalidateLayer()
        } else {
            val parent = this.parent
            parent?.invalidateLayer()
        }
    }

    private var _modifier: Modifier = Modifier
    private var pendingModifier: Modifier? = null
    internal val applyingModifierOnAttach
        get() = pendingModifier != null

    /** The [Modifier] currently applied to this node. */
    override var modifier: Modifier
        get() = _modifier
        set(value) {
            requirePrecondition(!isVirtual || modifier === Modifier) {
                "Modifiers are not supported on virtual LayoutNodes"
            }
            requirePrecondition(!isDeactivated) { "modifier is updated when deactivated" }
            if (isAttached) {
                applyModifier(value)
            } else {
                pendingModifier = value
            }
        }

    private fun applyModifier(modifier: Modifier) {
        _modifier = modifier
        nodes.updateFrom(modifier)
        layoutDelegate.updateParentData()
        if (lookaheadRoot == null && nodes.has(Nodes.ApproachMeasure)) {
            lookaheadRoot = this
        }
    }

    private fun resetModifierState() {
        nodes.resetState()
    }

    internal fun invalidateParentData() {
        layoutDelegate.invalidateParentData()
    }

    /**
     * Coordinates of just the contents of the [LayoutNode], after being affected by all modifiers.
     */
    override val coordinates: LayoutCoordinates
        get() = innerCoordinator

    /** Callback to be executed whenever the [LayoutNode] is attached to a new [Owner]. */
    internal var onAttach: ((Owner) -> Unit)? = null

    /** Callback to be executed whenever the [LayoutNode] is detached from an [Owner]. */
    internal var onDetach: ((Owner) -> Unit)? = null

    /**
     * Flag used by [OnPositionedDispatcher] to identify LayoutNodes that have already had their
     * [OnGloballyPositionedModifier]'s dispatch called so that they aren't called multiple times.
     */
    internal var needsOnPositionedDispatch = false

    internal fun place(x: Int, y: Int) {
        if (intrinsicsUsageByParent == UsageByParent.NotUsed) {
            // This LayoutNode may have asked children for intrinsics. If so, we should
            // clear the intrinsics usage for everything that was requested previously.
            clearSubtreePlacementIntrinsicsUsage()
        }
        with(parent?.innerCoordinator?.placementScope ?: requireOwner().placementScope) {
            measurePassDelegate.placeRelative(x, y)
        }
    }

    /** Place this layout node again on the same position it was placed last time */
    internal fun replace() {
        if (intrinsicsUsageByParent == UsageByParent.NotUsed) {
            // This LayoutNode may have asked children for intrinsics. If so, we should
            // clear the intrinsics usage for everything that was requested previously.
            clearSubtreePlacementIntrinsicsUsage()
        }
        measurePassDelegate.replace()
    }

    internal fun lookaheadReplace() {
        if (intrinsicsUsageByParent == UsageByParent.NotUsed) {
            // This LayoutNode may have asked children for intrinsics. If so, we should
            // clear the intrinsics usage for everything that was requested previously.
            clearSubtreePlacementIntrinsicsUsage()
        }
        lookaheadPassDelegate!!.replace()
    }

    internal fun draw(canvas: Canvas, graphicsLayer: GraphicsLayer?) =
        outerCoordinator.draw(canvas, graphicsLayer)

    /**
     * Carries out a hit test on the [PointerInputModifier]s associated with this [LayoutNode] and
     * all [PointerInputModifier]s on all descendant [LayoutNode]s.
     *
     * If [pointerPosition] is within the bounds of any tested [PointerInputModifier]s, the
     * [PointerInputModifier] is added to [hitTestResult] and true is returned.
     *
     * @param pointerPosition The tested pointer position, which is relative to the LayoutNode.
     * @param hitTestResult The collection that the hit [PointerInputFilter]s will be added to if
     *   hit.
     */
    internal fun hitTest(
        pointerPosition: Offset,
        hitTestResult: HitTestResult,
        isTouchEvent: Boolean = false,
        isInLayer: Boolean = true
    ) {
        val positionInWrapped = outerCoordinator.fromParentPosition(pointerPosition)
        outerCoordinator.hitTest(
            NodeCoordinator.PointerInputSource,
            positionInWrapped,
            hitTestResult,
            isTouchEvent,
            isInLayer
        )
    }

    @Suppress("UNUSED_PARAMETER")
    internal fun hitTestSemantics(
        pointerPosition: Offset,
        hitSemanticsEntities: HitTestResult,
        isTouchEvent: Boolean = true,
        isInLayer: Boolean = true
    ) {
        val positionInWrapped = outerCoordinator.fromParentPosition(pointerPosition)
        outerCoordinator.hitTest(
            NodeCoordinator.SemanticsSource,
            positionInWrapped,
            hitSemanticsEntities,
            isTouchEvent = true,
            isInLayer = isInLayer
        )
    }

    internal fun rescheduleRemeasureOrRelayout(it: LayoutNode) {
        when (it.layoutState) {
            Idle -> {
                // this node was scheduled for remeasure or relayout while it was not
                // placed. such requests are ignored for non-placed nodes so we have to
                // re-schedule remeasure or relayout.
                if (it.lookaheadMeasurePending) {
                    it.requestLookaheadRemeasure(forceRequest = true)
                } else {
                    if (it.lookaheadLayoutPending) {
                        it.requestLookaheadRelayout(forceRequest = true)
                    }
                    if (it.measurePending) {
                        it.requestRemeasure(forceRequest = true)
                    } else if (it.layoutPending) {
                        it.requestRelayout(forceRequest = true)
                    }
                }
            }
            else -> throw IllegalStateException("Unexpected state ${it.layoutState}")
        }
    }

    /** Used to request a new measurement + layout pass from the owner. */
    internal fun requestRemeasure(
        forceRequest: Boolean = false,
        scheduleMeasureAndLayout: Boolean = true,
        invalidateIntrinsics: Boolean = true
    ) {
        if (!ignoreRemeasureRequests && !isVirtual) {
            val owner = owner ?: return
            owner.onRequestMeasure(
                layoutNode = this,
                forceRequest = forceRequest,
                scheduleMeasureAndLayout = scheduleMeasureAndLayout
            )
            if (invalidateIntrinsics) {
                measurePassDelegate.invalidateIntrinsicsParent(forceRequest)
            }
        }
    }

    /**
     * Used to request a new lookahead measurement, lookahead layout, and subsequently measure and
     * layout from the owner.
     */
    internal fun requestLookaheadRemeasure(
        forceRequest: Boolean = false,
        scheduleMeasureAndLayout: Boolean = true,
        invalidateIntrinsics: Boolean = true
    ) {
        checkPrecondition(lookaheadRoot != null) {
            "Lookahead measure cannot be requested on a node that is not a part of the" +
                "LookaheadScope"
        }
        val owner = owner ?: return
        if (!ignoreRemeasureRequests && !isVirtual) {
            owner.onRequestMeasure(
                layoutNode = this,
                affectsLookahead = true,
                forceRequest = forceRequest,
                scheduleMeasureAndLayout = scheduleMeasureAndLayout
            )
            if (invalidateIntrinsics) {
                lookaheadPassDelegate!!.invalidateIntrinsicsParent(forceRequest)
            }
        }
    }

    /**
     * This gets called when both lookahead measurement (if in a LookaheadScope) and actual
     * measurement need to be re-done. Such events include modifier change, attach/detach, etc.
     */
    internal fun invalidateMeasurements() {
        outerToInnerOffsetDirty = true
        if (lookaheadRoot != null) {
            requestLookaheadRemeasure()
        } else {
            requestRemeasure()
        }
    }

    internal fun invalidateOnPositioned() {
        // If we've already scheduled a measure, the positioned callbacks will get called anyway
        if (layoutPending || measurePending || needsOnPositionedDispatch) return
        requireOwner().requestOnPositionedCallback(this)
    }

    internal inline fun ignoreRemeasureRequests(block: () -> Unit) {
        ignoreRemeasureRequests = true
        block()
        ignoreRemeasureRequests = false
    }

    /** Used to request a new layout pass from the owner. */
    internal fun requestRelayout(forceRequest: Boolean = false) {
        outerToInnerOffsetDirty = true
        if (!isVirtual) {
            owner?.onRequestRelayout(this, forceRequest = forceRequest)
        }
    }

    internal fun requestLookaheadRelayout(forceRequest: Boolean = false) {
        if (!isVirtual) {
            owner?.onRequestRelayout(this, affectsLookahead = true, forceRequest)
        }
    }

    internal fun dispatchOnPositionedCallbacks() {
        if (layoutState != Idle || layoutPending || measurePending || isDeactivated) {
            return // it hasn't yet been properly positioned, so don't make a call
        }
        if (!isPlaced) {
            return // it hasn't been placed, so don't make a call
        }
        nodes.headToTail(Nodes.GlobalPositionAware) {
            it.onGloballyPositioned(it.requireCoordinator(Nodes.GlobalPositionAware))
        }
    }

    /**
     * This returns a new List of Modifiers and the coordinates and any extra information that may
     * be useful. This is used for tooling to retrieve layout modifier and layer information.
     */
    override fun getModifierInfo(): List<ModifierInfo> = nodes.getModifierInfo()

    /** Invalidates layers defined on this LayoutNode. */
    internal fun invalidateLayers() {
        forEachCoordinator { coordinator -> coordinator.layer?.invalidate() }
        innerCoordinator.layer?.invalidate()
    }

    internal fun lookaheadRemeasure(
        constraints: Constraints? = layoutDelegate.lastLookaheadConstraints
    ): Boolean {
        // Only lookahead remeasure when the constraints are valid and the node is in
        // a LookaheadScope (by checking whether the lookaheadScope is set)
        return if (constraints != null && lookaheadRoot != null) {
            lookaheadPassDelegate!!.remeasure(constraints)
        } else {
            false
        }
    }

    /** Return true if the measured size has been changed */
    internal fun remeasure(constraints: Constraints? = layoutDelegate.lastConstraints): Boolean {
        return if (constraints != null) {
            if (intrinsicsUsageByParent == UsageByParent.NotUsed) {
                // This LayoutNode may have asked children for intrinsics. If so, we should
                // clear the intrinsics usage for everything that was requested previously.
                clearSubtreeIntrinsicsUsage()
            }
            measurePassDelegate.remeasure(constraints)
        } else {
            false
        }
    }

    /**
     * Tracks whether another measure pass is needed for the LayoutNode. Mutation to
     * [measurePending] is confined to LayoutNodeLayoutDelegate. It can only be set true from
     * outside of LayoutNode via [markMeasurePending]. It is cleared (i.e. set false) during the
     * measure pass ( i.e. in [LayoutNodeLayoutDelegate.performMeasure]).
     */
    internal val measurePending: Boolean
        get() = layoutDelegate.measurePending

    /**
     * Tracks whether another layout pass is needed for the LayoutNode. Mutation to [layoutPending]
     * is confined to LayoutNode. It can only be set true from outside of LayoutNode via
     * [markLayoutPending]. It is cleared (i.e. set false) during the layout pass (i.e. in
     * layoutChildren).
     */
    internal val layoutPending: Boolean
        get() = layoutDelegate.layoutPending

    internal val lookaheadMeasurePending: Boolean
        get() = layoutDelegate.lookaheadMeasurePending

    internal val lookaheadLayoutPending: Boolean
        get() = layoutDelegate.lookaheadLayoutPending

    /** Marks the layoutNode dirty for another layout pass. */
    internal fun markLayoutPending() = layoutDelegate.markLayoutPending()

    /** Marks the layoutNode dirty for another measure pass. */
    internal fun markMeasurePending() = layoutDelegate.markMeasurePending()

    /** Marks the layoutNode dirty for another lookahead layout pass. */
    internal fun markLookaheadLayoutPending() = layoutDelegate.markLookaheadLayoutPending()

    fun invalidateSubtree(isRootOfInvalidation: Boolean = true) {
        if (isRootOfInvalidation) {
            parent?.invalidateLayer()
        }
        invalidateSemantics()
        requestRemeasure()
        nodes.headToTail(Nodes.Layout) { it.requireCoordinator(Nodes.Layout).layer?.invalidate() }
        // TODO: invalidate parent data
        _children.forEach { it.invalidateSubtree(false) }
    }

    /** Marks the layoutNode dirty for another lookahead measure pass. */
    internal fun markLookaheadMeasurePending() = layoutDelegate.markLookaheadMeasurePending()

    override fun forceRemeasure() {
        // we do not schedule measure and layout as we are going to call it manually right after
        if (lookaheadRoot != null) {
            requestLookaheadRemeasure(scheduleMeasureAndLayout = false)
        } else {
            requestRemeasure(scheduleMeasureAndLayout = false)
        }
        val lastConstraints = layoutDelegate.lastConstraints
        if (lastConstraints != null) {
            owner?.measureAndLayout(this, lastConstraints)
        } else {
            owner?.measureAndLayout()
        }
    }

    override fun onLayoutComplete() {
        innerCoordinator.visitNodes(Nodes.LayoutAware) { it.onPlaced(innerCoordinator) }
    }

    /** Calls [block] on all [LayoutModifierNodeCoordinator]s in the NodeCoordinator chain. */
    internal inline fun forEachCoordinator(block: (LayoutModifierNodeCoordinator) -> Unit) {
        var coordinator: NodeCoordinator? = outerCoordinator
        val inner = innerCoordinator
        while (coordinator !== inner) {
            block(coordinator as LayoutModifierNodeCoordinator)
            coordinator = coordinator.wrapped
        }
    }

    /** Calls [block] on all [NodeCoordinator]s in the NodeCoordinator chain. */
    internal inline fun forEachCoordinatorIncludingInner(block: (NodeCoordinator) -> Unit) {
        var delegate: NodeCoordinator? = outerCoordinator
        val final = innerCoordinator.wrapped
        while (delegate != final && delegate != null) {
            block(delegate)
            delegate = delegate.wrapped
        }
    }

    private fun shouldInvalidateParentLayer(): Boolean {
        if (nodes.has(Nodes.Draw) && !nodes.has(Nodes.Layout)) return true
        nodes.headToTail {
            if (it.isKind(Nodes.Layout)) {
                if (it.requireCoordinator(Nodes.Layout).layer != null) {
                    return false
                }
            }
            if (it.isKind(Nodes.Draw)) return true
        }
        return true
    }

    /**
     * Walks the subtree and clears all [intrinsicsUsageByParent] that this LayoutNode's measurement
     * used intrinsics on.
     *
     * The layout that asks for intrinsics of its children is the node to call this to request all
     * of its subtree to be cleared.
     *
     * We can't do clearing as part of measure() because the child's measure() call is normally done
     * after the intrinsics is requested and we don't want to clear the usage at that point.
     */
    internal fun clearSubtreeIntrinsicsUsage() {
        // save the usage in case we short-circuit the measure call
        previousIntrinsicsUsageByParent = intrinsicsUsageByParent
        intrinsicsUsageByParent = UsageByParent.NotUsed
        forEachChild {
            if (it.intrinsicsUsageByParent != UsageByParent.NotUsed) {
                it.clearSubtreeIntrinsicsUsage()
            }
        }
    }

    /**
     * Walks the subtree and clears all [intrinsicsUsageByParent] that this LayoutNode's layout
     * block used intrinsics on.
     *
     * The layout that asks for intrinsics of its children is the node to call this to request all
     * of its subtree to be cleared.
     *
     * We can't do clearing as part of measure() because the child's measure() call is normally done
     * after the intrinsics is requested and we don't want to clear the usage at that point.
     */
    private fun clearSubtreePlacementIntrinsicsUsage() {
        // save the usage in case we short-circuit the measure call
        previousIntrinsicsUsageByParent = intrinsicsUsageByParent
        intrinsicsUsageByParent = UsageByParent.NotUsed
        forEachChild {
            if (it.intrinsicsUsageByParent == UsageByParent.InLayoutBlock) {
                it.clearSubtreePlacementIntrinsicsUsage()
            }
        }
    }

    /**
     * For a subtree that skips measurement, this resets the [intrinsicsUsageByParent] to what it
     * was prior to [clearSubtreeIntrinsicsUsage].
     */
    internal fun resetSubtreeIntrinsicsUsage() {
        forEachChild {
            it.intrinsicsUsageByParent = it.previousIntrinsicsUsageByParent
            if (it.intrinsicsUsageByParent != UsageByParent.NotUsed) {
                it.resetSubtreeIntrinsicsUsage()
            }
        }
    }

    override val parentInfo: LayoutInfo?
        get() = parent

    override var isDeactivated = false
        private set

    override fun onReuse() {
        requirePrecondition(isAttached) { "onReuse is only expected on attached node" }
        interopViewFactoryHolder?.onReuse()
        subcompositionsState?.onReuse()
        if (isDeactivated) {
            isDeactivated = false
            invalidateSemantics()
            // we don't need to reset state as it was done when deactivated
        } else {
            resetModifierState()
        }
        // resetModifierState detaches all nodes, so we need to re-attach them upon reuse.
        semanticsId = generateSemanticsId()
        nodes.markAsAttached()
        nodes.runAttachLifecycle()
        rescheduleRemeasureOrRelayout(this)
    }

    override fun onDeactivate() {
        interopViewFactoryHolder?.onDeactivate()
        subcompositionsState?.onDeactivate()
        isDeactivated = true
        resetModifierState()
        // if the node is detached the semantics were already updated without this node.
        if (isAttached) {
            invalidateSemantics()
        }
        owner?.onLayoutNodeDeactivated(this)
    }

    override fun onRelease() {
        interopViewFactoryHolder?.onRelease()
        subcompositionsState?.onRelease()
        forEachCoordinatorIncludingInner { it.onRelease() }
    }

    internal companion object {
        private val ErrorMeasurePolicy: NoIntrinsicsMeasurePolicy =
            object :
                NoIntrinsicsMeasurePolicy(error = "Undefined intrinsics block and it is required") {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints
                ) = error("Undefined measure and it is required")
            }

        /** Constant used by [placeOrder]. */
        @Suppress("ConstPropertyName") internal const val NotPlacedPlaceOrder = Int.MAX_VALUE

        /** Pre-allocated constructor to be used with ComposeNode */
        internal val Constructor: () -> LayoutNode = { LayoutNode() }

        /**
         * All of these values are only used in tests. The real ViewConfiguration should be set in
         * Layout()
         */
        internal val DummyViewConfiguration =
            object : ViewConfiguration {
                override val longPressTimeoutMillis: Long
                    get() = 400L

                override val doubleTapTimeoutMillis: Long
                    get() = 300L

                override val doubleTapMinTimeMillis: Long
                    get() = 40L

                override val touchSlop: Float
                    get() = 16f

                override val minimumTouchTargetSize: DpSize
                    get() = DpSize.Zero
            }

        /** Comparator allowing to sort nodes by zIndex and placement order. */
        internal val ZComparator =
            Comparator<LayoutNode> { node1, node2 ->
                if (node1.zIndex == node2.zIndex) {
                    // if zIndex is the same we use the placement order
                    node1.placeOrder.compareTo(node2.placeOrder)
                } else {
                    node1.zIndex.compareTo(node2.zIndex)
                }
            }
    }

    /**
     * Describes the current state the [LayoutNode] is in. A [LayoutNode] is expected to be in
     * [LookaheadMeasuring] first, followed by [LookaheadLayingOut] if it is in a LookaheadScope.
     * After the lookahead is finished, [Measuring] and then [LayingOut] will happen as needed.
     */
    internal enum class LayoutState {
        /** Node is currently being measured. */
        Measuring,

        /** Node is being measured in lookahead. */
        LookaheadMeasuring,

        /** Node is currently being laid out. */
        LayingOut,

        /** Node is being laid out in lookahead. */
        LookaheadLayingOut,

        /**
         * Node is not currently measuring or laying out. It could be pending measure or pending
         * layout depending on the [measurePending] and [layoutPending] flags.
         */
        Idle,
    }

    internal enum class UsageByParent {
        InMeasureBlock,
        InLayoutBlock,
        NotUsed,
    }
}

/** Returns [LayoutNode.owner] or throws if it is null. */
internal fun LayoutNode.requireOwner(): Owner {
    val owner = owner
    checkPreconditionNotNull(owner) { "LayoutNode should be attached to an owner" }
    return owner
}

/**
 * Inserts a child [LayoutNode] at a last index. If this LayoutNode [LayoutNode.isAttached] then
 * [child] will become [LayoutNode.isAttached] also. [child] must have a `null` [LayoutNode.parent].
 */
internal fun LayoutNode.add(child: LayoutNode) {
    insertAt(children.size, child)
}
