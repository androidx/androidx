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

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.focus.FocusEventModifier
import androidx.compose.ui.focus.FocusModifier
import androidx.compose.ui.focus.FocusOrderModifier
import androidx.compose.ui.focus.FocusRequesterModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.input.key.KeyInputModifier
import androidx.compose.ui.input.nestedscroll.NestedScrollDelegatingWrapper
import androidx.compose.ui.input.nestedscroll.NestedScrollModifier
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.layout.OnPlacedModifier
import androidx.compose.ui.layout.OnRemeasuredModifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.node.LayoutNode.LayoutState.LayingOut
import androidx.compose.ui.node.LayoutNode.LayoutState.Measuring
import androidx.compose.ui.node.LayoutNode.LayoutState.NeedsRelayout
import androidx.compose.ui.node.LayoutNode.LayoutState.NeedsRemeasure
import androidx.compose.ui.node.LayoutNode.LayoutState.Ready
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.nativeClass
import androidx.compose.ui.platform.simpleIdentityToString
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.SemanticsWrapper
import androidx.compose.ui.semantics.outerSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * Enable to log changes to the LayoutNode tree.  This logging is quite chatty.
 */
private const val DebugChanges = false

/**
 * An element in the layout hierarchy, built with compose UI.
 */
internal class LayoutNode(
    // Virtual LayoutNode is the temporary concept allows us to a node which is not a real node,
    // but just a holder for its children - allows us to combine some children into something we
    // can subcompose in(LayoutNode) without being required to define it as a real layout - we
    // don't want to define the layout strategy for such nodes, instead the children of the
    // virtual nodes will be treated as the direct children of the virtual node parent.
    // This whole concept will be replaced with a proper subcomposition logic which allows to
    // subcompose multiple times into the same LayoutNode and define offsets.
    private val isVirtual: Boolean = false
) : Measurable, Remeasurement, OwnerScope, LayoutInfo, ComposeUiNode {

    private var virtualChildrenCount = 0

    // the list of nodes containing the virtual children as is
    private val _foldedChildren = mutableVectorOf<LayoutNode>()
    internal val foldedChildren: List<LayoutNode> get() = _foldedChildren.asMutableList()

    // the list of nodes where the virtual children are unfolded (their children are represented
    // as our direct children)
    private var _unfoldedChildren: MutableVector<LayoutNode>? = null

    private fun recreateUnfoldedChildrenIfDirty() {
        if (unfoldedVirtualChildrenListDirty) {
            unfoldedVirtualChildrenListDirty = false
            val unfoldedChildren = _unfoldedChildren ?: mutableVectorOf<LayoutNode>().also {
                _unfoldedChildren = it
            }
            unfoldedChildren.clear()
            _foldedChildren.forEach {
                if (it.isVirtual) {
                    unfoldedChildren.addAll(it._children)
                } else {
                    unfoldedChildren.add(it)
                }
            }
        }
    }

    // when the list of our children is modified it will be set to true if we are a virtual node
    // or it will be set to true on a parent if the parent is a virtual node
    private var unfoldedVirtualChildrenListDirty = false
    private fun invalidateUnfoldedVirtualChildren() {
        if (virtualChildrenCount > 0) {
            unfoldedVirtualChildrenListDirty = true
        }
        if (isVirtual) {
            this.parent?.unfoldedVirtualChildrenListDirty = true
        }
    }

    @Suppress("PropertyName")
    internal val _children: MutableVector<LayoutNode>
        get() = if (virtualChildrenCount == 0) {
            _foldedChildren
        } else {
            recreateUnfoldedChildrenIfDirty()
            _unfoldedChildren!!
        }

    /**
     * The children of this LayoutNode, controlled by [insertAt], [move], and [removeAt].
     */
    internal val children: List<LayoutNode> get() = _children.asMutableList()

    /**
     * The parent node in the LayoutNode hierarchy. This is `null` when the [LayoutNode]
     * is not attached to a hierarchy or is the root of the hierarchy.
     */
    private var _foldedParent: LayoutNode? = null

    /*
     * The parent node in the LayoutNode hierarchy, skipping over virtual nodes.
     */
    internal val parent: LayoutNode?
        get() {
            return if (_foldedParent?.isVirtual == true) _foldedParent?.parent else _foldedParent
        }

    /**
     * The view system [Owner]. This `null` until [attach] is called
     */
    internal var owner: Owner? = null
        private set

    /**
     * Returns true if this [LayoutNode] currently has an [LayoutNode.owner].  Semantically,
     * this means that the LayoutNode is currently a part of a component tree.
     */
    override val isAttached: Boolean get() = owner != null

    /**
     * The tree depth of the [LayoutNode]. This is valid only when it is attached to a hierarchy.
     */
    internal var depth: Int = 0

    /**
     * The layout state the node is currently in.
     */
    internal var layoutState = Ready

    internal val wasMeasuredDuringThisIteration: Boolean
        get() = requireOwner().measureIteration == outerMeasurablePlaceable.measureIteration

    /**
     * A cache of modifiers to be used when setting and reusing previous modifiers.
     */
    private var wrapperCache = mutableVectorOf<DelegatingLayoutNodeWrapper<*>>()

    /**
     * [requestRemeasure] calls will be ignored while this flag is true.
     */
    private var ignoreRemeasureRequests = false

    /**
     * Inserts a child [LayoutNode] at a particular index. If this LayoutNode [owner] is not `null`
     * then [instance] will become [attach]ed also. [instance] must have a `null` [parent].
     */
    internal fun insertAt(index: Int, instance: LayoutNode) {
        check(instance._foldedParent == null) {
            "Cannot insert $instance because it already has a parent." +
                " This tree: " + debugTreeToString() +
                " Other tree: " + instance._foldedParent?.debugTreeToString()
        }
        check(instance.owner == null) {
            "Cannot insert $instance because it already has an owner." +
                " This tree: " + debugTreeToString() +
                " Other tree: " + instance.debugTreeToString()
        }

        if (DebugChanges) {
            println("$instance added to $this at index $index")
        }

        instance._foldedParent = this
        _foldedChildren.add(index, instance)
        onZSortedChildrenInvalidated()

        if (instance.isVirtual) {
            require(!isVirtual) { "Virtual LayoutNode can't be added into a virtual parent" }
            virtualChildrenCount++
        }
        invalidateUnfoldedVirtualChildren()

        instance.outerLayoutNodeWrapper.wrappedBy = innerLayoutNodeWrapper

        val owner = this.owner
        if (owner != null) {
            instance.attach(owner)
        }
    }

    private fun onZSortedChildrenInvalidated() {
        if (isVirtual) {
            parent?.onZSortedChildrenInvalidated()
        } else {
            zSortedChildrenInvalidated = true
        }
    }

    /**
     * Removes one or more children, starting at [index].
     */
    internal fun removeAt(index: Int, count: Int) {
        require(count >= 0) {
            "count ($count) must be greater than 0"
        }
        val attached = owner != null
        for (i in index + count - 1 downTo index) {
            val child = _foldedChildren.removeAt(i)
            onZSortedChildrenInvalidated()
            if (DebugChanges) {
                println("$child removed from $this at index $i")
            }

            if (attached) {
                child.detach()
            }
            child._foldedParent = null

            if (child.isVirtual) {
                virtualChildrenCount--
            }
            invalidateUnfoldedVirtualChildren()
        }
    }

    /**
     * Removes all children.
     */
    internal fun removeAll() {
        val attached = owner != null
        for (i in _foldedChildren.size - 1 downTo 0) {
            val child = _foldedChildren[i]
            if (attached) {
                child.detach()
            }
            child._foldedParent = null
        }
        _foldedChildren.clear()
        onZSortedChildrenInvalidated()

        virtualChildrenCount = 0
        invalidateUnfoldedVirtualChildren()
    }

    /**
     * Moves [count] elements starting at index [from] to index [to]. The [to] index is related to
     * the position before the change, so, for example, to move an element at position 1 to after
     * the element at position 2, [from] should be `1` and [to] should be `3`. If the elements
     * were LayoutNodes A B C D E, calling `move(1, 3, 1)` would result in the LayoutNodes
     * being reordered to A C B D E.
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
        requestRemeasure()
    }

    /**
     * Set the [Owner] of this LayoutNode. This LayoutNode must not already be attached.
     * [owner] must match its [parent].[owner].
     */
    internal fun attach(owner: Owner) {
        check(this.owner == null) {
            "Cannot attach $this as it already is attached.  Tree: " + debugTreeToString()
        }
        check(_foldedParent == null || _foldedParent?.owner == owner) {
            "Attaching to a different owner($owner) than the parent's owner(${parent?.owner})." +
                " This tree: " + debugTreeToString() +
                " Parent tree: " + _foldedParent?.debugTreeToString()
        }
        val parent = this.parent
        if (parent == null) {
            // it is a root node and attached root nodes are always placed (as there is no parent
            // to place them explicitly)
            isPlaced = true
        }

        this.owner = owner
        this.depth = (parent?.depth ?: -1) + 1
        if (outerSemantics != null) {
            owner.onSemanticsChange()
        }
        owner.onAttach(this)
        _foldedChildren.forEach { child ->
            child.attach(owner)
        }

        requestRemeasure()
        parent?.requestRemeasure()
        innerLayoutNodeWrapper.attach()
        forEachDelegate { it.attach() }
        onAttach?.invoke(owner)
    }

    /**
     * Remove the LayoutNode from the [Owner]. The [owner] must not be `null` before this call
     * and its [parent]'s [owner] must be `null` before calling this. This will also [detach] all
     * children. After executing, the [owner] will be `null`.
     */
    internal fun detach() {
        val owner = owner
        checkNotNull(owner) {
            "Cannot detach node that is already detached!  Tree: " + parent?.debugTreeToString()
        }
        val parent = this.parent
        if (parent != null) {
            parent.invalidateLayer()
            parent.requestRemeasure()
        }
        alignmentLines.reset()
        onDetach?.invoke(owner)
        forEachDelegate { it.detach() }
        innerLayoutNodeWrapper.detach()

        if (outerSemantics != null) {
            owner.onSemanticsChange()
        }
        owner.onDetach(this)
        this.owner = null
        depth = 0
        _foldedChildren.forEach { child ->
            child.detach()
        }
        placeOrder = NotPlacedPlaceOrder
        previousPlaceOrder = NotPlacedPlaceOrder
        isPlaced = false
    }

    private val _zSortedChildren = mutableVectorOf<LayoutNode>()
    private var zSortedChildrenInvalidated = true

    /**
     * Returns the children list sorted by their [LayoutNode.zIndex] first (smaller first) and the
     * order they were placed via [Placeable.placeAt] by parent (smaller first).
     * Please note that this list contains not placed items as well, so you have to manually
     * filter them.
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

    override val isValid: Boolean
        get() = isAttached

    override fun toString(): String {
        return "${simpleIdentityToString(this, null)} children: ${children.size} " +
            "measurePolicy: $measurePolicy"
    }

    /**
     * Call this method from the debugger to see a dump of the LayoutNode tree structure
     */
    @Suppress("unused")
    private fun debugTreeToString(depth: Int = 0): String {
        val tree = StringBuilder()
        for (i in 0 until depth) {
            tree.append("  ")
        }
        tree.append("|-")
        tree.append(toString())
        tree.append('\n')

        _children.forEach { child ->
            tree.append(child.debugTreeToString(depth + 1))
        }

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

    /**
     * Blocks that define the measurement and intrinsic measurement of the layout.
     */
    override var measurePolicy: MeasurePolicy = ErrorMeasurePolicy
        set(value) {
            if (field != value) {
                field = value
                intrinsicsPolicy.updateFrom(measurePolicy)
                requestRemeasure()
            }
        }

    /**
     * The intrinsic measurements of this layout, backed up by states to trigger
     * correct remeasurement for layouts using the intrinsics of this layout
     * when the [measurePolicy] is changing.
     */
    internal val intrinsicsPolicy = IntrinsicsPolicy(this)

    /**
     * The screen density to be used by this layout.
     */
    override var density: Density = Density(1f)
        set(value) {
            if (field != value) {
                field = value
                onDensityOrLayoutDirectionChanged()
            }
        }

    /**
     * The scope used to [measure][MeasurePolicy.measure] children.
     */
    internal val measureScope: MeasureScope = object : MeasureScope, Density {
        override val density: Float get() = this@LayoutNode.density.density
        override val fontScale: Float get() = this@LayoutNode.density.fontScale
        override val layoutDirection: LayoutDirection get() = this@LayoutNode.layoutDirection
    }

    /**
     * The layout direction of the layout node.
     */
    override var layoutDirection: LayoutDirection = LayoutDirection.Ltr
        set(value) {
            if (field != value) {
                field = value
                onDensityOrLayoutDirectionChanged()
            }
        }

    override var viewConfiguration: ViewConfiguration = DummyViewConfiguration

    private fun onDensityOrLayoutDirectionChanged() {
        // measure/layout modifiers on the node
        requestRemeasure()
        // draw modifiers on the node
        parent?.invalidateLayer()
        // and draw modifiers after graphics layers on the node
        invalidateLayers()
    }

    /**
     * The measured width of this layout and all of its [modifier]s. Shortcut for `size.width`.
     */
    override val width: Int get() = outerMeasurablePlaceable.width

    /**
     * The measured height of this layout and all of its [modifier]s. Shortcut for `size.height`.
     */
    override val height: Int get() = outerMeasurablePlaceable.height

    /**
     * State corresponding to the alignment lines of this layout, inherited + intrinsic.
     */
    internal val alignmentLines = LayoutNodeAlignmentLines(this)

    internal val mDrawScope: LayoutNodeDrawScope
        get() = requireOwner().sharedDrawScope

    /**
     * Whether or not this [LayoutNode] and all of its parents have been placed in the hierarchy.
     */
    override var isPlaced: Boolean = false
        private set

    /**
     * The order in which this node was placed by its parent during the previous [layoutChildren].
     * Before the placement the order is set to [NotPlacedPlaceOrder] to all the children. Then
     * every placed node assigns this variable to [parent]s [nextChildPlaceOrder] and increments
     * this counter. Not placed items will still have [NotPlacedPlaceOrder] set.
     */
    internal var placeOrder: Int = NotPlacedPlaceOrder
        private set

    /**
     * The value [placeOrder] had during the previous parent [layoutChildren]. Helps us to
     * understand if the order did change.
     */
    private var previousPlaceOrder: Int = NotPlacedPlaceOrder

    /**
     * The counter on a parent node which is used by its children to understand the order in which
     * they were placed.
     * @see placeOrder
     */
    private var nextChildPlaceOrder: Int = 0

    /**
     * Remembers how the node was measured by the parent.
     */
    internal var measuredByParent: UsageByParent = UsageByParent.NotUsed

    @Deprecated("Temporary API to support ConstraintLayout prototyping.")
    internal var canMultiMeasure: Boolean = false

    internal val innerLayoutNodeWrapper: LayoutNodeWrapper = InnerPlaceable(this)
    private val outerMeasurablePlaceable = OuterMeasurablePlaceable(this, innerLayoutNodeWrapper)
    internal val outerLayoutNodeWrapper: LayoutNodeWrapper
        get() = outerMeasurablePlaceable.outerWrapper

    /**
     * zIndex defines the drawing order of the LayoutNode. Children with larger zIndex are drawn
     * on top of others (the original order is used for the nodes with the same zIndex).
     * Default zIndex is 0. We use sum of the values passed as zIndex to place() by the
     * parent layout and all the applied modifiers.
     */
    private var zIndex: Float = 0f

    /**
     * The inner-most layer wrapper. Used for performance for LayoutNodeWrapper.findLayer().
     */
    private var _innerLayerWrapper: LayoutNodeWrapper? = null
    internal var innerLayerWrapperIsDirty = true
    private val innerLayerWrapper: LayoutNodeWrapper? get() {
        if (innerLayerWrapperIsDirty) {
            var delegate: LayoutNodeWrapper? = innerLayoutNodeWrapper
            val final = outerLayoutNodeWrapper.wrappedBy
            _innerLayerWrapper = null
            while (delegate != final) {
                if (delegate?.layer != null) {
                    _innerLayerWrapper = delegate
                    break
                }
                delegate = delegate?.wrappedBy
            }
        }
        val layerWrapper = _innerLayerWrapper
        if (layerWrapper != null) {
            requireNotNull(layerWrapper.layer)
        }
        return layerWrapper
    }

    /**
     * Invalidates the inner-most layer as part of this LayoutNode or from the containing
     * LayoutNode. This is added for performance so that LayoutNodeWrapper.invalidateLayer() can be
     * faster.
     */
    internal fun invalidateLayer() {
        val innerLayerWrapper = innerLayerWrapper
        if (innerLayerWrapper != null) {
            innerLayerWrapper.invalidateLayer()
        } else {
            val parent = this.parent
            parent?.invalidateLayer()
        }
    }

    /**
     * The [Modifier] currently applied to this node.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    override var modifier: Modifier = Modifier
        set(value) {
            if (value == field) return
            if (modifier != Modifier) {
                require(!isVirtual) { "Modifiers are not supported on virtual LayoutNodes" }
            }
            field = value

            val invalidateParentLayer = shouldInvalidateParentLayer()

            copyWrappersToCache()
            markReusedModifiers(value)

            // Rebuild LayoutNodeWrapper
            val oldOuterWrapper = outerMeasurablePlaceable.outerWrapper
            if (outerSemantics != null && isAttached) {
                owner!!.onSemanticsChange()
            }
            val addedCallback = hasNewPositioningCallback()
            onPositionedCallbacks?.clear()

            innerLayoutNodeWrapper.onInitialize()

            // Create a new chain of LayoutNodeWrappers, reusing existing ones from wrappers
            // when possible.
            val outerWrapper = modifier.foldOut(innerLayoutNodeWrapper) { mod, toWrap ->
                if (mod is RemeasurementModifier) {
                    mod.onRemeasurementAvailable(this)
                }

                if (mod is DrawModifier) {
                    val drawEntity = DrawEntity(toWrap, mod)
                    drawEntity.next = toWrap.drawEntityHead
                    toWrap.drawEntityHead = drawEntity
                    drawEntity.onInitialize()
                }

                // Re-use the layoutNodeWrapper if possible.
                reuseLayoutNodeWrapper(mod, toWrap)?.let {
                    return@foldOut it
                }

                // The order in which the following blocks occur matters. For example, the
                // DrawModifier block should be before the LayoutModifier block so that a
                // Modifier that implements both DrawModifier and LayoutModifier will have
                // it's draw bounds reflect the dimensions defined by the LayoutModifier.
                // Please ensure that ModifierLocalProvider is the first item here so that
                // other layoutNodeWrappers don't accidentally use values that they provided.
                // Also ensure that ModifierLocalConsumer is the next item here, so that it is
                // created after all the other LayoutNodeWrappers are created, (So that the
                // other layoutNodeWrappers are initialized by the time
                // onModifierLocalsUpdated() is called.
                var wrapper = toWrap
                if (mod is ModifierLocalProvider<*>) {
                    wrapper = ModifierLocalProviderNode(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is ModifierLocalConsumer) {
                    wrapper = ModifierLocalConsumerNode(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is FocusModifier) {
                    wrapper = ModifiedFocusNode(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is FocusEventModifier) {
                    wrapper = ModifiedFocusEventNode(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is FocusRequesterModifier) {
                    wrapper = ModifiedFocusRequesterNode(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is FocusOrderModifier) {
                    wrapper = ModifiedFocusOrderNode(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is KeyInputModifier) {
                    wrapper = ModifiedKeyInputNode(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is PointerInputModifier) {
                    wrapper = PointerInputDelegatingWrapper(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is NestedScrollModifier) {
                    wrapper = NestedScrollDelegatingWrapper(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is LayoutModifier) {
                    wrapper = ModifiedLayoutNode(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is ParentDataModifier) {
                    wrapper = ModifiedParentDataNode(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is SemanticsModifier) {
                    wrapper = SemanticsWrapper(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is OnRemeasuredModifier) {
                    wrapper = RemeasureModifierWrapper(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is OnPlacedModifier) {
                    wrapper = OnPlacedModifierWrapper(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                if (mod is OnGloballyPositionedModifier) {
                    wrapper = OnGloballyPositionedModifierWrapper(wrapper, mod)
                        .initialize()
                        .assignChained(toWrap)
                }
                wrapper
            }

            outerWrapper.wrappedBy = parent?.innerLayoutNodeWrapper
            outerMeasurablePlaceable.outerWrapper = outerWrapper

            if (isAttached) {
                // call detach() on all removed LayoutNodeWrappers
                wrapperCache.forEach {
                    it.detach()
                }

                // attach() all new LayoutNodeWrappers
                forEachDelegate {
                    if (!it.isAttached) {
                        it.attach()
                    }
                }
            }
            wrapperCache.clear()

            // call onModifierChanged() on all LayoutNodeWrappers
            forEachDelegate { it.onModifierChanged() }

            // Optimize the case where the layout itself is not modified. A common reason for
            // this is if no wrapping actually occurs above because no LayoutModifiers are
            // present in the modifier chain.
            if (oldOuterWrapper != innerLayoutNodeWrapper ||
                outerWrapper != innerLayoutNodeWrapper
            ) {
                requestRemeasure()
            } else if (layoutState == Ready && addedCallback) {
                // We need to notify the callbacks of a change in position since there's
                // a new one.
                requestRemeasure()
            }
            // If the parent data has changed, the parent needs remeasurement.
            val oldParentData = parentData
            outerMeasurablePlaceable.recalculateParentData()
            if (oldParentData != parentData) {
                parent?.requestRemeasure()
            }
            if (invalidateParentLayer || shouldInvalidateParentLayer()) {
                parent?.invalidateLayer()
            }
        }

    /**
     * Coordinates of just the contents of the [LayoutNode], after being affected by all modifiers.
     */
    override val coordinates: LayoutCoordinates
        get() = innerLayoutNodeWrapper

    /**
     * Callback to be executed whenever the [LayoutNode] is attached to a new [Owner].
     */
    internal var onAttach: ((Owner) -> Unit)? = null

    /**
     * Callback to be executed whenever the [LayoutNode] is detached from an [Owner].
     */
    internal var onDetach: ((Owner) -> Unit)? = null

    /**
     * List of all OnPositioned callbacks in the modifier chain.
     */
    private var onPositionedCallbacks: MutableVector<OnGloballyPositionedModifierWrapper>? = null

    internal fun getOrCreateOnPositionedCallbacks() = onPositionedCallbacks
        ?: mutableVectorOf<OnGloballyPositionedModifierWrapper>().also {
            onPositionedCallbacks = it
        }

    /**
     * Flag used by [OnPositionedDispatcher] to identify LayoutNodes that have already
     * had their [OnGloballyPositionedModifier]'s dispatch called so that they aren't called
     * multiple times.
     */
    internal var needsOnPositionedDispatch = false

    internal fun place(x: Int, y: Int) {
        Placeable.PlacementScope.executeWithRtlMirroringValues(
            outerMeasurablePlaceable.measuredWidth,
            layoutDirection
        ) {
            outerMeasurablePlaceable.placeRelative(x, y)
        }
    }

    /**
     * Place this layout node again on the same position it was placed last time
     */
    internal fun replace() {
        outerMeasurablePlaceable.replace()
    }

    internal fun draw(canvas: Canvas) = outerLayoutNodeWrapper.draw(canvas)

    /**
     * Carries out a hit test on the [PointerInputModifier]s associated with this [LayoutNode] and
     * all [PointerInputModifier]s on all descendant [LayoutNode]s.
     *
     * If [pointerPosition] is within the bounds of any tested
     * [PointerInputModifier]s, the [PointerInputModifier] is added to [hitTestResult]
     * and true is returned.
     *
     * @param pointerPosition The tested pointer position, which is relative to
     * the LayoutNode.
     * @param hitTestResult The collection that the hit [PointerInputFilter]s will be
     * added to if hit.
     */
    internal fun hitTest(
        pointerPosition: Offset,
        hitTestResult: HitTestResult<PointerInputFilter>,
        isTouchEvent: Boolean = false,
        isInLayer: Boolean = true
    ) {
        val positionInWrapped = outerLayoutNodeWrapper.fromParentPosition(pointerPosition)
        outerLayoutNodeWrapper.hitTest(
            positionInWrapped,
            hitTestResult,
            isTouchEvent,
            isInLayer
        )
    }

    @Suppress("UNUSED_PARAMETER")
    internal fun hitTestSemantics(
        pointerPosition: Offset,
        hitSemanticsWrappers: HitTestResult<SemanticsWrapper>,
        isTouchEvent: Boolean = true,
        isInLayer: Boolean = true
    ) {
        val positionInWrapped = outerLayoutNodeWrapper.fromParentPosition(pointerPosition)
        outerLayoutNodeWrapper.hitTestSemantics(
            positionInWrapped,
            hitSemanticsWrappers,
            isInLayer
        )
    }

    /**
     * Return true if there is a new [OnGloballyPositionedModifier] assigned to this Layout.
     */
    private fun hasNewPositioningCallback(): Boolean {
        val onPositionedCallbacks = onPositionedCallbacks
        return modifier.foldOut(false) { mod, hasNewCallback ->
            hasNewCallback || mod is OnGloballyPositionedModifier &&
                (onPositionedCallbacks?.firstOrNull { mod == it.modifier } == null)
        }
    }

    /**
     * Invoked when the parent placed the node. It will trigger the layout.
     */
    internal fun onNodePlaced() {
        val parent = parent

        var newZIndex = innerLayoutNodeWrapper.zIndex
        forEachDelegate {
            newZIndex += it.zIndex
        }
        if (newZIndex != zIndex) {
            zIndex = newZIndex
            parent?.onZSortedChildrenInvalidated()
            parent?.invalidateLayer()
        }

        if (!isPlaced) {
            // when the visibility of a child has been changed we need to invalidate
            // parents inner layer - the layer in which this child will be drawn
            parent?.invalidateLayer()
            markNodeAndSubtreeAsPlaced()
        }

        if (parent != null) {
            if (parent.layoutState == LayingOut) {
                // the parent is currently placing its children
                check(placeOrder == NotPlacedPlaceOrder) {
                    "Place was called on a node which was placed already"
                }
                placeOrder = parent.nextChildPlaceOrder
                parent.nextChildPlaceOrder++
            }
            // if parent is not laying out we were asked to be relaid out without affecting the
            // parent. this means our placeOrder didn't change since the last time parent placed us
        } else {
            // parent is null for the root node
            placeOrder = 0
        }

        layoutChildren()
    }

    internal fun layoutChildren() {
        alignmentLines.recalculateQueryOwner()

        if (layoutState == NeedsRelayout) {
            onBeforeLayoutChildren()
        }
        // as a result of the previous operation we can figure out a child has been resized
        // and we need to be remeasured, not relaid out
        if (layoutState == NeedsRelayout) {
            layoutState = LayingOut
            val owner = requireOwner()
            owner.snapshotObserver.observeLayoutSnapshotReads(this) {
                // reset the place order counter which will be used by the children
                nextChildPlaceOrder = 0
                _children.forEach { child ->
                    // and reset the place order for all the children before placing them
                    child.previousPlaceOrder = child.placeOrder
                    child.placeOrder = NotPlacedPlaceOrder
                    child.alignmentLines.usedDuringParentLayout = false
                }

                innerLayoutNodeWrapper.measureResult.placeChildren()
                _children.forEach { child ->
                    // we set `placeOrder` to NotPlacedPlaceOrder for all the children, then
                    // during the placeChildren() invocation the real order will be assigned for
                    // all the placed children.
                    if (child.previousPlaceOrder != child.placeOrder) {
                        onZSortedChildrenInvalidated()
                        invalidateLayer()
                        if (child.placeOrder == NotPlacedPlaceOrder) {
                            child.markSubtreeAsNotPlaced()
                        }
                    }
                    child.alignmentLines.previousUsedDuringParentLayout =
                        child.alignmentLines.usedDuringParentLayout
                }
            }

            layoutState = Ready
        }

        if (alignmentLines.usedDuringParentLayout) {
            alignmentLines.previousUsedDuringParentLayout = true
        }
        if (alignmentLines.dirty && alignmentLines.required) alignmentLines.recalculate()
    }

    private fun markNodeAndSubtreeAsPlaced() {
        isPlaced = true
        // invalidate all the nodes layers that were invalidated while the node was not placed
        forEachDelegateIncludingInner {
            if (it.lastLayerDrawingWasSkipped) {
                it.invalidateLayer()
            }
        }
        _children.forEach {
            // this child was placed during the previous parent's layoutChildren(). this means that
            // before the parent became not placed this child was placed. we need to restore that
            if (it.placeOrder != NotPlacedPlaceOrder) {
                it.markNodeAndSubtreeAsPlaced()
                rescheduleRemeasureOrRelayout(it)
            }
        }
    }

    private fun rescheduleRemeasureOrRelayout(it: LayoutNode) {
        when (val state = it.layoutState) {
            NeedsRemeasure, NeedsRelayout -> {
                // we need to reset the state before requesting as otherwise the request
                // would be ignored.
                it.layoutState = Ready
                // this node was scheduled for remeasure or relayout while it was not
                // placed. such requests are ignored for non-placed nodes so we have to
                // re-schedule remeasure or relayout.
                if (state == NeedsRemeasure) {
                    it.requestRemeasure()
                } else {
                    it.requestRelayout()
                }
            }
            Ready -> {
                // no extra work required and node is ready to be displayed
            }
            else -> throw IllegalStateException("Unexpected state ${it.layoutState}")
        }
    }

    private fun markSubtreeAsNotPlaced() {
        if (isPlaced) {
            isPlaced = false
            _children.forEach {
                it.markSubtreeAsNotPlaced()
            }
        }
    }

    /**
     * The callback to be executed before running layoutChildren.
     *
     * There are possible cases when we run layoutChildren() on the parent node, but some of its
     * children are not yet measured even if they are supposed to be measured in the measure
     * block of our parent.
     *
     * Example:
     * val child = Layout(...)
     * Layout(child) { measurable, constraints ->
     *    val placeable = measurable.first().measure(constraints)
     *    layout(placeable.width, placeable.height) {
     *       placeable.place(0, 0)
     *    }
     * }
     * And now some set of changes scheduled remeasure for child and relayout for parent.
     *
     * During the [MeasureAndLayoutDelegate.measureAndLayout] we will start with the parent as it
     * has lower depth. Inside the layout block we will call placeable.width which is currently
     * dirty as the child was scheduled to remeasure. This callback will ensure it never happens
     * and pre-remeasure everything required for this layoutChildren().
     */
    private fun onBeforeLayoutChildren() {
        _children.forEach {
            if (it.layoutState == NeedsRemeasure &&
                it.measuredByParent == UsageByParent.InMeasureBlock
            ) {
                if (it.remeasure()) {
                    requestRemeasure()
                }
            }
        }
    }

    internal fun onAlignmentsChanged() {
        if (alignmentLines.dirty) return
        alignmentLines.dirty = true

        val parent = parent ?: return
        if (alignmentLines.usedDuringParentMeasurement) {
            parent.requestRemeasure()
        } else if (alignmentLines.previousUsedDuringParentLayout) {
            parent.requestRelayout()
        }
        if (alignmentLines.usedByModifierMeasurement) {
            requestRemeasure()
        }
        if (alignmentLines.usedByModifierLayout) {
            parent.requestRelayout()
        }
        parent.onAlignmentsChanged()
    }

    internal fun calculateAlignmentLines(): Map<AlignmentLine, Int> {
        if (!outerMeasurablePlaceable.duringAlignmentLinesQuery) {
            alignmentLinesQueriedByModifier()
        }
        layoutChildren()
        return alignmentLines.getLastCalculation()
    }

    private fun alignmentLinesQueriedByModifier() {
        if (layoutState == Measuring) {
            alignmentLines.usedByModifierMeasurement = true
            // We quickly transition to NeedsRelayout as we need the alignment lines now.
            // Later we will see that we also laid out as part of measurement and will skip layout.
            if (alignmentLines.dirty) layoutState = NeedsRelayout
        } else {
            // Note this can also happen for onGloballyPositioned queries.
            alignmentLines.usedByModifierLayout = true
        }
    }

    internal fun handleMeasureResult(measureResult: MeasureResult) {
        innerLayoutNodeWrapper.measureResult = measureResult
    }

    /**
     * Used to request a new measurement + layout pass from the owner.
     */
    internal fun requestRemeasure() {
        val owner = owner ?: return
        if (!ignoreRemeasureRequests && !isVirtual) {
            owner.onRequestMeasure(this)
        }
    }

    internal inline fun ignoreRemeasureRequests(block: () -> Unit) {
        ignoreRemeasureRequests = true
        block()
        ignoreRemeasureRequests = false
    }

    /**
     * Used to request a new layout pass from the owner.
     */
    internal fun requestRelayout() {
        if (!isVirtual) {
            owner?.onRequestRelayout(this)
        }
    }

    /**
     * Execute your code within the [block] if you want some code to not be observed for the
     * model reads even if you are currently inside some observed scope like measuring.
     */
    internal fun withNoSnapshotReadObservation(block: () -> Unit) {
        requireOwner().snapshotObserver.withNoSnapshotReadObservation(block)
    }

    internal fun dispatchOnPositionedCallbacks() {
        if (layoutState != Ready) {
            return // it hasn't yet been properly positioned, so don't make a call
        }
        if (!isPlaced) {
            return // it hasn't been placed, so don't make a call
        }
        onPositionedCallbacks?.forEach {
            it.modifier.onGloballyPositioned(it)
        }
    }

    /**
     * This returns a new List of Modifiers and the coordinates and any extra information
     * that may be useful. This is used for tooling to retrieve layout modifier and layer
     * information.
     */
    override fun getModifierInfo(): List<ModifierInfo> {
        val infoList = mutableVectorOf<ModifierInfo>()
        forEachDelegate { wrapper ->
            wrapper as DelegatingLayoutNodeWrapper<*>
            val layer = wrapper.layer
            val info = ModifierInfo(wrapper.modifier, wrapper, layer)
            infoList += info
            var node = wrapper.drawEntityHead // head
            while (node != null) {
                infoList += ModifierInfo(node.modifier, wrapper, layer)
                node = node.next
            }
        }
        var innerNode = innerLayoutNodeWrapper.drawEntityHead
        while (innerNode != null) {
            infoList += ModifierInfo(
                innerNode.modifier,
                innerLayoutNodeWrapper,
                innerLayoutNodeWrapper.layer
            )
            innerNode = innerNode.next
        }
        return infoList.asMutableList()
    }

    /**
     * Invalidates layers defined on this LayoutNode.
     */
    internal fun invalidateLayers() {
        forEachDelegate { wrapper ->
            wrapper.layer?.invalidate()
        }
        innerLayoutNodeWrapper.layer?.invalidate()
    }

    /**
     * Reuses a [DelegatingLayoutNodeWrapper] from [wrapperCache] if one matches the class
     * type of [modifier]. This walks backward through the [wrapperCache] and
     * extracts all [DelegatingLayoutNodeWrapper]s that are
     * [chained][DelegatingLayoutNodeWrapper.isChained] together.
     * If none can be reused, `null` is returned.
     */
    private fun reuseLayoutNodeWrapper(
        modifier: Modifier.Element,
        wrapper: LayoutNodeWrapper
    ): DelegatingLayoutNodeWrapper<*>? {
        if (wrapperCache.isEmpty()) {
            return null
        }
        // Look for exact match
        var lastIndex = wrapperCache.indexOfLast {
            it.toBeReusedForSameModifier && it.modifier === modifier
        }

        if (lastIndex < 0) {
            // Look for class match
            lastIndex = wrapperCache.indexOfLast {
                !it.toBeReusedForSameModifier && it.modifier.nativeClass() == modifier.nativeClass()
            }
        }

        if (lastIndex < 0) {
            return null
        }

        val endWrapper = wrapperCache.removeAt(lastIndex--)
        endWrapper.wrapped = wrapper
        endWrapper.setModifierTo(modifier)
        endWrapper.initialize()

        var startWrapper = endWrapper
        while (startWrapper.isChained) {
            startWrapper = wrapperCache.removeAt(lastIndex--)
            startWrapper.setModifierTo(modifier)
            startWrapper.initialize()
        }
        return startWrapper
    }

    /**
     * Copies all [DelegatingLayoutNodeWrapper]s currently in use and returns them in a new
     * Array.
     */
    private fun copyWrappersToCache() {
        forEachDelegate {
            wrapperCache += it as DelegatingLayoutNodeWrapper<*>
            it.drawEntityHead = null
        }
        innerLayoutNodeWrapper.drawEntityHead = null
    }

    private fun markReusedModifiers(modifier: Modifier) {
        wrapperCache.forEach {
            it.toBeReusedForSameModifier = false
        }

        modifier.foldIn(Unit) { _, mod ->
            var wrapper = wrapperCache.lastOrNull {
                it.modifier === mod && !it.toBeReusedForSameModifier
            }
            // we want to walk up the chain up all LayoutNodeWrappers for the same modifier
            while (wrapper != null) {
                wrapper.toBeReusedForSameModifier = true
                wrapper = if (wrapper.isChained)
                    wrapper.wrappedBy as? DelegatingLayoutNodeWrapper<*>
                else
                    null
            }
        }
    }

    // Delegation from Measurable to measurableAndPlaceable
    override fun measure(constraints: Constraints) =
        outerMeasurablePlaceable.measure(constraints)

    /**
     * Return true if the measured size has been changed
     */
    internal fun remeasure(
        constraints: Constraints? = outerMeasurablePlaceable.lastConstraints
    ): Boolean {
        return if (constraints != null) {
            outerMeasurablePlaceable.remeasure(constraints)
        } else {
            false
        }
    }

    override val parentData: Any? get() = outerMeasurablePlaceable.parentData

    override fun minIntrinsicWidth(height: Int): Int =
        outerMeasurablePlaceable.minIntrinsicWidth(height)

    override fun maxIntrinsicWidth(height: Int): Int =
        outerMeasurablePlaceable.maxIntrinsicWidth(height)

    override fun minIntrinsicHeight(width: Int): Int =
        outerMeasurablePlaceable.minIntrinsicHeight(width)

    override fun maxIntrinsicHeight(width: Int): Int =
        outerMeasurablePlaceable.maxIntrinsicHeight(width)

    override fun forceRemeasure() {
        requestRemeasure()
        owner?.measureAndLayout()
    }

    /**
     * Calls [block] on all [DelegatingLayoutNodeWrapper]s in the LayoutNodeWrapper chain.
     */
    private inline fun forEachDelegate(block: (LayoutNodeWrapper) -> Unit) {
        var delegate = outerLayoutNodeWrapper
        val inner = innerLayoutNodeWrapper
        while (delegate != inner) {
            block(delegate)
            delegate = delegate.wrapped!!
        }
    }

    /**
     * Calls [block] on all [DelegatingLayoutNodeWrapper]s in the LayoutNodeWrapper chain.
     */
    private inline fun forEachDelegateIncludingInner(block: (LayoutNodeWrapper) -> Unit) {
        var delegate: LayoutNodeWrapper? = outerLayoutNodeWrapper
        val final = innerLayoutNodeWrapper.wrapped
        while (delegate != final && delegate != null) {
            block(delegate)
            delegate = delegate.wrapped
        }
    }

    private fun shouldInvalidateParentLayer(): Boolean {
        forEachDelegateIncludingInner {
            if (it.layer != null) {
                return false
            } else if (it.drawEntityHead != null) {
                return true
            }
        }
        return true
    }

    /**
     * Comparator allowing to sort nodes by zIndex and placement order.
     */
    private val ZComparator = Comparator<LayoutNode> { node1, node2 ->
        if (node1.zIndex == node2.zIndex) {
            // if zIndex is the same we use the placement order
            node1.placeOrder.compareTo(node2.placeOrder)
        } else {
            node1.zIndex.compareTo(node2.zIndex)
        }
    }

    override val parentInfo: LayoutInfo?
        get() = parent

    internal companion object {
        private val ErrorMeasurePolicy: NoIntrinsicsMeasurePolicy =
            object : NoIntrinsicsMeasurePolicy(
                error = "Undefined intrinsics block and it is required"
            ) {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints
                ) = error("Undefined measure and it is required")
            }

        /**
         * Constant used by [placeOrder].
         */
        internal const val NotPlacedPlaceOrder = Int.MAX_VALUE

        /**
         * Pre-allocated constructor to be used with ComposeNode
         */
        internal val Constructor: () -> LayoutNode = { LayoutNode() }

        /**
         * All of these values are only used in tests. The real ViewConfiguration should
         * be set in Layout()
         */
        internal val DummyViewConfiguration = object : ViewConfiguration {
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
    }

    /**
     * Describes the current state the [LayoutNode] is in.
     */
    internal enum class LayoutState {
        /**
         * Request remeasure was called on the node.
         */
        NeedsRemeasure,
        /**
         * Node is currently being measured.
         */
        Measuring,
        /**
         * Request relayout was called on the node or the node was just measured and is going to
         * layout soon (measure stage is always being followed by the layout stage).
         */
        NeedsRelayout,
        /**
         * Node is currently being laid out.
         */
        LayingOut,
        /**
         * Node is measured and laid out or not yet attached to the [Owner] (see [LayoutNode.owner]).
         */
        Ready
    }

    internal enum class UsageByParent {
        InMeasureBlock,
        InLayoutBlock,
        NotUsed,
    }
}

/**
 * Returns [LayoutNode.owner] or throws if it is null.
 */
internal fun LayoutNode.requireOwner(): Owner {
    val owner = owner
    checkNotNull(owner) {
        "LayoutNode should be attached to an owner"
    }
    return owner
}

/**
 * Inserts a child [LayoutNode] at a last index. If this LayoutNode [LayoutNode.isAttached]
 * then [child] will become [LayoutNode.isAttached] also. [child] must have a `null`
 * [LayoutNode.parent].
 */
internal fun LayoutNode.add(child: LayoutNode) {
    insertAt(children.size, child)
}

/**
 * Sets [DelegatingLayoutNodeWrapper#isChained] to `true` of the [wrapped][this.wrapped] when it
 * is part of a chain of LayoutNodes for the same modifier.
 *
 * @param originalWrapper The LayoutNodeWrapper that the modifier chain should be wrapping.
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun <T : DelegatingLayoutNodeWrapper<*>> T.assignChained(
    originalWrapper: LayoutNodeWrapper
): T {
    if (originalWrapper !== wrapped) {
        val wrapper = wrapped as DelegatingLayoutNodeWrapper<*>
        wrapper.isChained = true
    }
    return this
}

@Suppress("NOTHING_TO_INLINE")
private inline fun <T : DelegatingLayoutNodeWrapper<*>> T.initialize(): T {
    onInitialize()
    return this
}
