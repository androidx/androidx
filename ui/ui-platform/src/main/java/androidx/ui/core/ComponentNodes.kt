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
package androidx.ui.core

import androidx.ui.core.focus.FocusModifierImpl
import androidx.ui.core.focus.ModifiedFocusNode
import androidx.ui.core.focus.findParentFocusNode
import androidx.ui.core.focus.ownerHasFocus
import androidx.ui.core.focus.requestFocusForOwner
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.core.semantics.SemanticsModifier
import androidx.ui.core.semantics.SemanticsWrapper
import androidx.ui.focus.FocusDetailedState
import androidx.ui.focus.FocusDetailedState.Active
import androidx.ui.focus.FocusDetailedState.ActiveParent
import androidx.ui.focus.FocusDetailedState.Captured
import androidx.ui.focus.FocusDetailedState.Disabled
import androidx.ui.focus.FocusDetailedState.Inactive
import androidx.ui.focus.FocusModifier
import androidx.ui.graphics.Canvas
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.round
import androidx.ui.util.fastForEach

/**
 * Enable to log changes to the ComponentNode tree.  This logging is quite chatty.
 */
private const val DebugChanges = false

/**
 * The base type for all nodes from the tree generated from a component hierarchy.
 *
 * Specific components are backed by a tree of nodes: Draw, Layout, GestureDetector.
 * All other components are not represented in the backing hierarchy.
 */
sealed class ComponentNode {

    internal val children = mutableListOf<ComponentNode>()

    /**
     * The parent node in the ComponentNode hierarchy. This is `null` when the `ComponentNode`
     * is attached (has an [owner]) and is the root of the tree or has not had [add] called for it.
     */
    var parent: ComponentNode? = null
        private set

    /**
     * The view system [Owner]. This `null` until [attach] is called
     */
    var owner: Owner? = null
        private set

    /**
     * The tree depth of the ComponentNode. This is valid only when [isAttached] is true.
     */
    var depth: Int = 0

    /**
     * An opaque value set by the [Owner]. It is `null` when [isAttached] is false, but
     * may also be `null` when [isAttached] is true, depending on the needs of the Owner.
     */
    var ownerData: Any? = null

    /**
     * Returns the number of children in this ComponentNode.
     */
    val count: Int
        get() = children.size

    /**
     * This is the LayoutNode ancestor that contains this LayoutNode. This will be `null` for the
     * root [LayoutNode].
     */
    open val parentLayoutNode: LayoutNode?
        get() = containingLayoutNode

    /**
     * Method to find the layout node. LayoutNode returns itself, but
     * all other ComponentNodes return the parent's `containingLayoutNode`.
     */
    internal open val containingLayoutNode: LayoutNode?
        get() = parent?.containingLayoutNode

    /**
     * Execute [block] on all children of this ComponentNode.
     */
    inline fun visitChildren(block: (ComponentNode) -> Unit) {
        for (i in 0 until count) {
            block(this[i])
        }
    }

    /**
     * Execute [block] on all children of this ComponentNode in reverse order.
     */
    inline fun visitChildrenReverse(block: (ComponentNode) -> Unit) {
        for (i in count - 1 downTo 0) {
            block(this[i])
        }
    }

    /**
     * Inserts a child [ComponentNode] at a particular index. If this ComponentNode [isAttached]
     * then [instance] will become [attach]ed also. [instance] must have a `null` [parent].
     */
    fun insertAt(index: Int, instance: ComponentNode) {
        ErrorMessages.ComponentNodeHasParent.validateState(instance.parent == null)
        ErrorMessages.OwnerAlreadyAttached.validateState(instance.owner == null)

        if (DebugChanges) {
            println("$instance added to $this at index $index")
        }

        instance.parent = this
        children.add(index, instance)

        val containingLayout = containingLayoutNode
        if (containingLayout != null) {
            if (instance is LayoutNode) {
                instance.layoutNodeWrapper.wrappedBy = containingLayout.innerLayoutNodeWrapper
            } else {
                visitLayoutChildren {
                    it.layoutNodeWrapper.wrappedBy = containingLayout.innerLayoutNodeWrapper
                }
            }
        }

        val owner = this.owner
        if (owner != null) {
            instance.attach(owner)
        }
    }

    /**
     * Removes one or more children, starting at [index].
     */
    fun removeAt(index: Int, count: Int) {
        ErrorMessages.CountOutOfRange.validateArg(count >= 0, count)
        val attached = owner != null
        for (i in index + count - 1 downTo index) {
            val child = children.removeAt(i)
            if (DebugChanges) {
                println("$child removed from $this at index $i")
            }

            if (attached) {
                child.detach()
            }
            child.parent = null
        }
    }

    /**
     * Moves [count] elements starting at index [from] to index [to]. The [to] index is related to
     * the position before the change, so, for example, to move an element at position 1 to after
     * the element at position 2, [from] should be `1` and [to] should be `3`. If the elements
     * were ComponentNodes A B C D E, calling `move(1, 3, 1)` would result in the ComponentNodes
     * being reordered to A C B D E.
     */
    fun move(from: Int, to: Int, count: Int) {
        if (from == to) {
            return // nothing to do
        }

        for (i in 0 until count) {
            // if "from" is after "to," the from index moves because we're inserting before it
            val fromIndex = if (from > to) from + i else from
            val toIndex = if (from > to) to + i else to + count - 2
            val child = children.removeAt(fromIndex)

            if (DebugChanges) {
                println("$child moved in $this from index $fromIndex to $toIndex")
            }

            children.add(toIndex, child)
        }

        containingLayoutNode?.let {
            it.layoutChildrenDirty = true
            it.requestRemeasure()
        }
    }

    /**
     * Returns the child ComponentNode at the given index. An exception will be thrown if there
     * is no child at the given index.
     */
    operator fun get(index: Int): ComponentNode = children[index]

    /**
     * Set the [Owner] of this ComponentNode. This ComponentNode must not already be attached.
     * [owner] must match its [parent].[owner].
     */
    open fun attach(owner: Owner) {
        ErrorMessages.OwnerAlreadyAttached.validateState(this.owner == null)
        val parent = parent
        ErrorMessages.ParentOwnerMustMatchChild.validateState(
            parent == null ||
                    parent.owner == owner
        )
        this.owner = owner
        this.depth = (parent?.depth ?: -1) + 1
        owner.onAttach(this)
        visitChildren { child ->
            child.attach(owner)
        }
    }

    /**
     * Remove the ComponentNode from the [Owner]. The [owner] must not be `null` before this call
     * and its [parent]'s [owner] must be `null` before calling this. This will also [detach] all
     * children. After executing, the [owner] will be `null`.
     */
    open fun detach() {
        val owner = owner ?: ErrorMessages.OwnerAlreadyDetached.state()
        owner.onDetach(this)
        this.owner = null
        depth = 0
        visitChildren { child ->
            child.detach()
        }
    }

    private val _zIndexSortedChildren = mutableListOf<ComponentNode>()

    /**
     * Returns the children list sorted by their [LayoutNode.zIndex].
     * Note that the object is reused so you shouldn't save it for later.
     */
    @PublishedApi
    internal val zIndexSortedChildren: List<ComponentNode>
        get() {
            _zIndexSortedChildren.clear()
            _zIndexSortedChildren.addAll(children)
            _zIndexSortedChildren.sortWith(ZIndexComparator)
            return _zIndexSortedChildren
        }

    override fun toString(): String {
        return "${simpleIdentityToString(this)} children: ${children.size}"
    }

    /**
     * Call this method from the debugger to see a dump of the ComponentNode tree structure
     */
    private fun debugTreeToString(depth: Int = 0): String {
        val tree = StringBuilder()
        for (i in 0 until depth) {
            tree.append("  ")
        }
        tree.append("|-")
        tree.append(toString())
        tree.append('\n')

        for (child in children) {
            tree.append(child.debugTreeToString(depth + 1))
        }

        if (depth == 0) {
            // Delete trailing newline
            tree.deleteCharAt(tree.length - 1)
        }
        return tree.toString()
    }
}

/**
 * Comparator allowing to sort nodes by zIndex
 */
private val ZIndexComparator = Comparator<ComponentNode> { node1, node2 ->
    val depth1 = if (node1 is LayoutNode) node1.zIndex else 0f
    val depth2 = if (node2 is LayoutNode) node2.zIndex else 0f
    if (depth1 > depth2) 1 else if (depth1 < depth2) -1 else 0
}

/**
 * Returns true if this [ComponentNode] currently has an [ComponentNode.owner].  Semantically,
 * this means that the ComponentNode is currently a part of a component tree.
 */
fun ComponentNode.isAttached() = owner != null

// TODO(b/143778512): Why are the properties vars?  Shouldn't they be vals defined in the
//  constructor such that they both must be provided?
/**
 * Backing node for handling pointer events.
 */
class PointerInputNode : ComponentNode() {

    /**
     * Invoked right after the [PointerInputNode] is hit by a pointer during hit testing.
     *
     * @See CustomEventDispatcher
     */
    var initHandler: ((CustomEventDispatcher) -> Unit)? = null

    /**
     * Invoked when pointers that previously hit this PointerInputNode have changed.
     */
    var pointerInputHandler: PointerInputHandler = { event, _, _ -> event }

    /**
     * Invoked when a [CustomEvent] is dispatched by a [PointerInputNode].
     *
     * Dispatch occurs over all passes of [PointerEventPass].
     *
     * The [CustomEvent] is the event being dispatched. The [PointerEventPass] is the pass that
     * dispatch is currently on.
     *
     * @see CustomEvent
     * @see PointerEventPass
     */
    var customEventHandler: ((CustomEvent, PointerEventPass) -> Unit)? = null

    /**
     * Invoked to notify the handler that no more calls to pointerInputHandler will be made, until
     * at least new pointers exist.  This can occur for a few reasons:
     * 1. Android dispatches ACTION_CANCEL to [AndroidComposeView.onTouchEvent].
     * 2. The PointerInputNode has been removed from the compose hierarchy.
     * 3. The PointerInputNode no longer has any descendant [LayoutNode]s and therefore does not
     * know what region of the screen it should virtually exist in.
     */
    var cancelHandler: () -> Unit = { }
}

/**
 * Backing node that implements focus.
 */
class FocusNode : ComponentNode() {
    /**
     * Implementation oddity around composition; used to capture a reference to this
     * [FocusNode] when composed. This is a reverse property that mutates its right-hand side.
     *
     * TODO: Once we finalize the API consider removing this and replace this with an
     *  interface that sets the value as a property on the object that needs it.
     */
    var ref: Ref<FocusNode>?
        get() = null
        set(value) {
            value?.value = this
        }

    /**
     * The recompose function of the Recompose component this [FocusNode] is hosted in.
     *
     * We need to trigger re-composition manually because we determine focus during composition, and
     * editing an @Model object during composition does not trigger a re-composition.
     *
     * TODO (b/144897112): Remove manual recomposition.
     */
    private lateinit var _recompose: () -> Unit
    var recompose: () -> Unit
        get() = _recompose
        set(value) {
            _recompose = value
        }

    /**
     * The focus state for the current component. When the component is in the [Active] state, it
     * receives key events and other actions. We use [FocusDetailedState]s internally and
     * developers have the option to build their components using [FocusDetailedState], or a
     * subset of states defined in [FocusState][androidx.ui.focus.FocusState].
     */
    var focusState: FocusDetailedState = Inactive
        internal set

    /**
     * The [LayoutCoordinates] of the [OnChildPositioned][androidx.ui.core.OnChildPositioned]
     * component that hosts the child components of this [FocusNode].
     */
    @Suppress("KDocUnresolvedReference")
    var layoutCoordinates: LayoutCoordinates? = null

    /**
     * The list of focusable children of this [FocusNode]. The [ComponentNode] base class defines
     * [children] of this node, but the [focusableChildren] set includes all the [FocusNode]s
     * that are directly reachable from this [FocusNode].
     */
    private val focusableChildren = mutableSetOf<FocusNode>()

    /**
     * The [FocusNode] from the set of [focusableChildren] that is currently [Active].
     */
    private var focusedChild: FocusNode? = null

    /**
     * Add this focusable child to the parent's focusable children list.
     */
    override fun attach(owner: Owner) {
        findParentFocusNode()?.focusableChildren?.add(this)
        super.attach(owner)
    }

    /**
     * Remove this focusable child from the parent's focusable children list.
     */
    override fun detach() {
        // TODO (b/144119129): If this node is focused, let the parent know that it needs to
        //  grant focus to another focus node.
        super.detach()
        findParentFocusNode()?.focusableChildren?.remove(this)
    }

    /**
     * Request focus for this node.
     *
     * @param propagateFocus Whether the focus should be propagated to the node's children.
     *
     * In Compose, the parent [FocusNode] controls focus for its focusable children.Calling this
     * function will send a focus request to this [FocusNode]'s parent [FocusNode].
     */
    fun requestFocus(propagateFocus: Boolean = true) {

        when (focusState) {
            Active, Captured, Disabled -> return
            ActiveParent -> {
                /** We don't need to do anything if [propagateFocus] is true,
                since this subtree already has focus.*/
                if (!propagateFocus && focusedChild?.clearFocus() ?: true) {
                    grantFocus(propagateFocus)
                }
            }
            Inactive -> {
                val focusParent = findParentFocusNode()
                if (focusParent == null) {
                    // TODO (b/144116848) : Find out if the view hosting this composable is in focus.
                    //  The top most focusable is [Active] only if the view hosting this composable is
                    //  in focus. For now, we are making the assumption that our activity has only one
                    //  view, and it is always in focus.
                    //  Also, if the host AndroidComposeView does not have focus, request focus.
                    //  Proceed to grant focus to this node only if the host view gains focus.
                    grantFocus(propagateFocus)
                    recompose()
                } else {
                    focusParent.requestFocusForChild(this, propagateFocus)
                }
            }
        }
    }

    /**
     * Deny requests to clear focus.
     *
     * This is used when a component wants to hold onto focus (eg. A phone number field with an
     * invalid number.
     *
     * @return true if the focus was successfully captured. False otherwise.
     */
    fun captureFocus(): Boolean {
        if (focusState == Active) {
            focusState = Captured
            return true
        } else {
            return false
        }
    }

    /**
     * When the node is in the [Captured] state, it rejects all requests to clear focus. Calling
     * [freeFocus] puts the node in the [Active] state, where it is no longer preventing other
     * nodes from requesting focus.
     *
     * @return true if the captured focus was released. If the node is not in the [Captured]
     * state. this function returns false to indicate that this operation was a no-op.
     */
    fun freeFocus(): Boolean {
        if (focusState == Captured) {
            focusState = Active
            return true
        } else {
            return false
        }
    }

    /**
     * This function grants focus to this node.
     *
     * @param propagateFocus Whether the focus should be propagated to the node's children.
     *
     * Note: This function is private, and should only be called by a parent [FocusNode] to grant
     * focus to one of its child [FocusNode]s.
     */
    private fun grantFocus(propagateFocus: Boolean) {

        // TODO (b/144126570) use ChildFocusablility.
        //  For now we assume children get focus before parent).

        // TODO (b/144126759): Design a system to decide which child get's focus.
        //  for now we grant focus to the first child.
        val focusedCandidate = focusableChildren.firstOrNull()

        if (focusedCandidate == null || !propagateFocus) {
            // No Focused Children, or we don't want to propagate focus to children.
            focusState = Active
        } else {
            focusState = ActiveParent
            focusedChild = focusedCandidate
            focusedCandidate.grantFocus(propagateFocus)
            focusedCandidate.recompose()
        }
    }

    /**
     * This function clears focus from this node.
     *
     * Note: This function is private, and should only be called by a parent [FocusNode] to clear
     * focus from one of its child [FocusNode]s.
     */
    private fun clearFocus(): Boolean {
        return when (focusState) {

            Active -> {
                focusState = Inactive
                true
            }
            /**
             * If the node is [ActiveParent], we need to clear focus from the [Active] descendant
             * first, before clearing focus of this node.
             */
            ActiveParent -> focusedChild?.clearFocus() ?: error("No Focused Child")
            /**
             * If the node is [Captured], deny requests to clear focus.
             */
            Captured -> false
            /**
             * Nothing to do if the node is not focused. Even though the node ends up in a
             * cleared state, we return false to indicate that we didn't change any state (This
             * return value is used to trigger a recomposition, so returning false will not
             * trigger any recomposition).
             */
            Inactive, Disabled -> false
        }
    }

    /**
     * Focusable children of this [FocusNode] can use this function to request focus.
     *
     * @param childNode: The node that is requesting focus.
     * @param propagateFocus Whether the focus should be propagated to the node's children.
     * @return true if focus was granted, false otherwise.
     */
    private fun requestFocusForChild(childNode: FocusNode, propagateFocus: Boolean): Boolean {

        // Only this node's children can ask for focus.
        if (!focusableChildren.contains(childNode)) {
            error("Non child node cannot request focus.")
        }

        return when (focusState) {
            /**
             * If this node is [Active], it can give focus to the requesting child.
             */
            Active -> {
                focusState = ActiveParent
                focusedChild = childNode
                childNode.grantFocus(propagateFocus)
                recompose()
                true
            }
            /**
             * If this node is [ActiveParent] ie, one of the parent's descendants is [Active],
             * remove focus from the currently focused child and grant it to the requesting child.
             */
            ActiveParent -> {
                val previouslyFocusedNode = focusedChild ?: error("no focusedChild found")
                if (previouslyFocusedNode.clearFocus()) {
                    focusedChild = childNode
                    childNode.grantFocus(propagateFocus)
                    previouslyFocusedNode.recompose()
                    childNode.recompose()
                    true
                } else {
                    // Currently focused component does not want to give up focus.
                    false
                }
            }
            /**
             * If this node is not [Active], we must gain focus first before granting it
             * to the requesting child.
             */
            Inactive -> {
                val focusParent = findParentFocusNode()
                if (focusParent == null) {
                    requestFocusForOwner()
                    // If the owner successfully gains focus, proceed otherwise return false.
                    if (ownerHasFocus()) {
                        focusState = Active
                        requestFocusForChild(childNode, propagateFocus)
                    } else {
                        false
                    }
                } else if (focusParent.requestFocusForChild(this, propagateFocus = false)) {
                    requestFocusForChild(childNode, propagateFocus)
                } else {
                    // Could not gain focus, so have no focus to give.
                    false
                }
            }
            /**
             * If this node is [Captured], decline requests from the children.
             */
            Captured -> false
            /**
             * Children of a [Disabled] parent should also be [Disabled].
             */
            Disabled -> error("non root FocusNode needs a focusable parent")
        }
    }
}

/**
 * Backing node for the Draw component.
 */
class DrawNode : ComponentNode() {
    var onPaintWithChildren: (ContentDrawScope.(canvas: Canvas, parentSize: PxSize) -> Unit)? = null
        set(value) {
            field = value
            invalidate()
        }

    var onPaint: (Density.(canvas: Canvas, parentSize: PxSize) -> Unit)? = null
        set(value) {
            field = value
            invalidate()
        }

    var needsPaint = false

    override fun attach(owner: Owner) {
        super.attach(owner)
        needsPaint = true
        owner.onInvalidate(this)
    }

    override fun detach() {
        invalidate()
        super.detach()
        needsPaint = false
    }

    fun invalidate() {
        if (!needsPaint) {
            needsPaint = true
            owner?.onInvalidate(this)
        }
    }
}

/**
 * Backing node for Layout component.
 *
 * Measuring a [LayoutNode] as a [Measurable] will measure the node's content as adjusted by
 * [modifier].
 */
class LayoutNode : ComponentNode(), Measurable {
    interface MeasureBlocks {
        /**
         * The function used to measure the child. It must call [MeasureScope.layout] before
         * completing.
         */
        fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ): MeasureScope.MeasureResult

        /**
         * The function used to calculate [IntrinsicMeasurable.minIntrinsicWidth].
         */
        fun minIntrinsicWidth(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx,
            layoutDirection: LayoutDirection
        ): IntPx

        /**
         * The lambda used to calculate [IntrinsicMeasurable.minIntrinsicHeight].
         */
        fun minIntrinsicHeight(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx,
            layoutDirection: LayoutDirection
        ): IntPx

        /**
         * The function used to calculate [IntrinsicMeasurable.maxIntrinsicWidth].
         */
        fun maxIntrinsicWidth(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx,
            layoutDirection: LayoutDirection
        ): IntPx

        /**
         * The lambda used to calculate [IntrinsicMeasurable.maxIntrinsicHeight].
         */
        fun maxIntrinsicHeight(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx,
            layoutDirection: LayoutDirection
        ): IntPx
    }

    abstract class NoIntrinsicsMeasureBlocks(private val error: String) : MeasureBlocks {
        override fun minIntrinsicWidth(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx,
            layoutDirection: LayoutDirection
        ) = error(error)

        override fun minIntrinsicHeight(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx,
            layoutDirection: LayoutDirection
        ) = error(error)

        override fun maxIntrinsicWidth(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx,
            layoutDirection: LayoutDirection
        ) = error(error)

        override fun maxIntrinsicHeight(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx,
            layoutDirection: LayoutDirection
        ) = error(error)
    }

    /**
     * Blocks that define the measurement and intrinsic measurement of the layout.
     */
    var measureBlocks: MeasureBlocks = ErrorMeasureBlocks
        set(value) {
            if (field != value) {
                field = value
                requestRemeasure()
            }
        }

    /**
     * The scope used to run the [MeasureBlocks.measure] [MeasureBlock].
     */
    val measureScope: MeasureScope = object : MeasureScope(), Density {
        private val ownerDensity: Density
            get() = owner?.density ?: Density(1f)
        override val density: Float
            get() = ownerDensity.density
        override val fontScale: Float
            get() = ownerDensity.fontScale
        override val layoutDirection: LayoutDirection get() = this@LayoutNode.layoutDirection
    }

    /**
     * The constraints used the last time [layout] was called.
     */
    var constraints: Constraints = Constraints.fixed(IntPx.Zero, IntPx.Zero)

    /**
     * The layout direction of the layout node.
     */
    var layoutDirection: LayoutDirection = LayoutDirection.Ltr

    /**
     * Implementation oddity around composition; used to capture a reference to this
     * [LayoutNode] when composed. This is a reverse property that mutates its right-hand side.
     */
    var ref: Ref<LayoutNode>?
        get() = null
        set(value) {
            value?.value = this
        }

    /**
     * The measured width of this layout and all of its [modifier]s. Shortcut for `size.width`.
     */
    val width: IntPx get() = layoutNodeWrapper.width

    /**
     * The measured height of this layout and all of its [modifier]s. Shortcut for `size.height`.
     */
    val height: IntPx get() = layoutNodeWrapper.height

    /**
     * The alignment lines of this layout, inherited + intrinsic
     */
    internal val alignmentLines: MutableMap<AlignmentLine, IntPx> = hashMapOf()

    /**
     * The alignment lines provided by this layout at the last measurement
     */
    internal val providedAlignmentLines: MutableMap<AlignmentLine, IntPx> = hashMapOf()

    /**
     * Whether or not this has been placed in the hierarchy.
     */
    var isPlaced = false
        internal set

    /**
     * `true` when the parent's size depends on this LayoutNode's size
     */
    var affectsParentSize: Boolean = true
        private set

    /**
     * `true` when inside [measure]
     */
    var isMeasuring: Boolean = false
        private set

    /**
     * `true` when inside [layout]
     */
    var isLayingOut: Boolean = false
        private set

    /**
     * `true` when the current node is positioned during the measure pass,
     * since it needs to compute alignment lines.
     */
    var positionedDuringMeasurePass: Boolean = false

    /**
     * `true` when the layout has been dirtied by [requestRemeasure]. `false` after
     * the measurement has been complete ([place] has been called).
     */
    var needsRemeasure = false
        internal set(value) {
            require(!isMeasuring)
            field = value
        }

    /**
     * `true` when the layout has been measured or dirtied because the layout
     * lambda accessed a model that has been dirtied.
     */
    var needsRelayout = false
        internal set(value) {
            require(!isMeasuring)
            require(!isLayingOut)
            field = value
        }

    /**
     * `true` when the parent reads our alignment lines
     */
    internal var alignmentLinesRead = false

    /**
     * `true` when the alignment lines have to be recomputed because the layout has
     * been remeasured
     */
    internal var dirtyAlignmentLines = true

    /**
     * `true` when an ancestor relies on our alignment lines
     */
    internal val alignmentLinesRequired
        get() = alignmentLinesQueryOwner != null && alignmentLinesQueryOwner!!.alignmentLinesRead

    /**
     * Used by the parent to identify if the child has been queried for alignment lines since
     * last measurement.
     */
    internal var alignmentLinesQueriedSinceLastLayout = false

    /**
     * The closest layout node above in the hierarchy which asked for alignment lines.
     */
    internal var alignmentLinesQueryOwner: LayoutNode? = null

    override val parentLayoutNode: LayoutNode?
        get() = super.containingLayoutNode

    override val containingLayoutNode: LayoutNode?
        get() = this

    /**
     * A local version of [Owner.measureIteration] to ensure that [MeasureBlocks.measure]
     * is not called multiple times within a measure pass.
     */
    internal var measureIteration = 0L
        private set

    @Deprecated("Temporary API to support ConstraintLayout prototyping.")
    var canMultiMeasure: Boolean = false

    /**
     * Identifies when [layoutChildren] needs to be recalculated or if it can use
     * the cached value.
     */
    internal var layoutChildrenDirty = true

    /**
     * The cached value of [layoutChildren]
     */
    private val _layoutChildren = mutableListOf<LayoutNode>()

    /**
     * All first level [LayoutNode] descendants. All LayoutNodes in the List
     * will have this as [parentLayoutNode].
     */
    val layoutChildren: List<LayoutNode>
        get() {
            if (layoutChildrenDirty) {
                _layoutChildren.clear()
                addLayoutChildren(this, _layoutChildren)
                layoutChildrenDirty = false
            }
            return _layoutChildren
        }

    /**
     * `true` when parentDataNode has to be rediscovered. This is when the
     * LayoutNode has been attached.
     */
    private var parentDataDirty = false

    override val parentData: Any?
        get() = layoutNodeWrapper.parentData

    /**
     * The parentData [DataNode] for this LayoutNode.
     */
    internal var parentDataNode: DataNode<*>? = null
        get() {
            if (parentDataDirty) {
                // walk up to find ParentData
                field = null
                var node = parent
                val parentLayoutNode = parentLayoutNode
                while (node != null && node !== parentLayoutNode) {
                    if (node is DataNode<*> && node.key === ParentDataKey) {
                        field = node
                        break
                    }
                    node = node.parent
                }
                parentDataDirty = false
            }
            return field
        }

    internal val innerLayoutNodeWrapper: LayoutNodeWrapper = InnerPlaceable(this)
    internal var layoutNodeWrapper = innerLayoutNodeWrapper

    /**
     * zIndex defines the drawing order of the LayoutNode. Children with larger zIndex are drawn
     * after others (the original order is used for the nodes with the same zIndex).
     * Default zIndex is 0. Current implementation is using the first(front) DrawLayerModifier's
     * elevation as a zIndex. We will have a separate zIndex modifier later instead to decouple
     * this features.
     */
    internal val zIndex: Float get() = outerZIndexModifier?.zIndex ?: 0f

    /**
     * The outermost ZIndexModifier in the modifier chain or `null` if there are no
     * ZIndexModifier in the modifier chain.
     */
    private var outerZIndexModifier: ZIndexModifier? = null

    /**
     * The [Modifier] currently applied to this node.
     */
    var modifier: Modifier = Modifier
        set(value) {
            if (value == field) return
            field = value

            // Rebuild layoutNodeWrapper
            val oldPlaceable = layoutNodeWrapper
            val addedCallback = hasNewPositioningCallback()
            onPositionedCallbacks.clear()
            onChildPositionedCallbacks.clear()
            outerZIndexModifier = null
            layoutNodeWrapper = modifier.foldOut(innerLayoutNodeWrapper) { mod, toWrap ->
                var wrapper = toWrap
                // The order in which the following blocks occur matters.  For example, the
                // DrawModifier block should be before the LayoutModifier block so that a Modifier
                // that implements both DrawModifier and LayoutModifier will have it's draw bounds
                // reflect the dimensions defined by the LayoutModifier.
                if (mod is OnPositionedModifier) {
                    onPositionedCallbacks += mod
                }
                if (mod is OnChildPositionedModifier) {
                    onChildPositionedCallbacks += mod
                }
                if (mod is DrawModifier) {
                    wrapper = ModifiedDrawNode(wrapper, mod)
                }
                if (mod is DrawLayerModifier) {
                    wrapper = LayerWrapper(wrapper, mod)
                }
                if (mod is FocusModifier) {
                    require(mod is FocusModifierImpl)
                    wrapper = ModifiedFocusNode(wrapper, mod).also { mod.focusNode = it }
                }
                if (mod is PointerInputModifier) {
                    wrapper = PointerInputDelegatingWrapper(wrapper, mod)
                }
                @Suppress("Deprecation")
                if (mod is LayoutModifier) {
                    wrapper = ModifiedLayoutNode(wrapper, mod)
                }
                if (mod is LayoutModifier2) {
                    wrapper = ModifiedLayoutNode2(wrapper, mod)
                }
                if (mod is ParentDataModifier) {
                    wrapper = ModifiedParentDataNode(wrapper, mod)
                }
                if (mod is SemanticsModifier) {
                    wrapper = SemanticsWrapper(wrapper, mod)
                }
                if (mod is ZIndexModifier) {
                    outerZIndexModifier = mod
                }
                wrapper
            }
            // Optimize the case where the layout itself is not modified. A common reason for
            // this is if no wrapping actually occurs above because no LayoutModifiers are
            // present in the modifier chain.
            if (oldPlaceable != layoutNodeWrapper) {
                oldPlaceable.detach()
                requestRemeasure()
            } else if (!needsRemeasure && !needsRelayout && addedCallback) {
                // We need to notify the callbacks of a change in position since there's
                // a new one.
                requestRemeasure()
            }
            val containing = parentLayoutNode
            if (containing != null) {
                layoutNodeWrapper.wrappedBy = containing.innerLayoutNodeWrapper
            }
            owner?.onInvalidate(this)
        }

    @Deprecated(
        "Temporary API to support our transition from single child composables to modifiers."
    )
    // TODO(popam): remove this
    var handlesParentData: Boolean = true

    /**
     * Coordinates of just the contents of the LayoutNode, after being affected by all modifiers.
     */
    // TODO(mount): remove this
    val coordinates: LayoutCoordinates
        get() = innerLayoutNodeWrapper

    override fun attach(owner: Owner) {
        super.attach(owner)
        requestRemeasure()
        parentDataDirty = true
        parentLayoutNode?.layoutChildrenDirty = true
        onAttach?.invoke(owner)
    }

    /**
     * Callback to be executed whenever the [LayoutNode] is attached to a new [Owner].
     */
    var onAttach: ((Owner) -> Unit)? = null

    override fun detach() {
        val owner = owner!!
        val parentLayoutNode = parentLayoutNode
        if (parentLayoutNode != null) {
            parentLayoutNode.onInvalidate()
            parentLayoutNode.layoutChildrenDirty = true
            parentLayoutNode.requestRemeasure()
        }
        parentDataDirty = true
        alignmentLinesQueryOwner = null
        onDetach?.invoke(owner)
        layoutNodeWrapper.detach()
        super.detach()
    }

    /**
     * Callback to be executed whenever the [LayoutNode] is detached from an [Owner].
     */
    var onDetach: ((Owner) -> Unit)? = null

    /**
     * List of all OnPositioned callbacks in the modifier chain.
     */
    private val onPositionedCallbacks = mutableListOf<OnPositionedModifier>()

    /**
     * List of all OnChildPositioned callbacks in the modifier chain.
     */
    private val onChildPositionedCallbacks = mutableListOf<OnChildPositionedModifier>()

    override fun measure(constraints: Constraints, layoutDirection: LayoutDirection): Placeable {
        val owner = requireOwner()
        val iteration = owner.measureIteration
        @Suppress("Deprecation")
        canMultiMeasure = canMultiMeasure ||
                (parentLayoutNode != null && parentLayoutNode!!.canMultiMeasure)
        @Suppress("Deprecation")
        check(measureIteration != iteration || canMultiMeasure) {
            "measure() may not be called multiple times on the same Measurable"
        }
        measureIteration = iteration
        val parent = parentLayoutNode
        // The more idiomatic, `if (parentLayoutNode?.isMeasuring == true)` causes boxing
        affectsParentSize = parent != null && parent.isMeasuring == true
        if (this.constraints == constraints && !needsRemeasure) {
            return layoutNodeWrapper // we're already measured to this size, don't do anything
        }

        needsRemeasure = false
        isMeasuring = true
        dirtyAlignmentLines = true
        this.constraints = constraints
        owner.observeMeasureModelReads(this) {
            layoutNodeWrapper.measure(constraints, layoutDirection)
        }
        isMeasuring = false
        needsRelayout = true
        return layoutNodeWrapper
    }

    override fun minIntrinsicWidth(height: IntPx): IntPx =
        layoutNodeWrapper.minIntrinsicWidth(height)

    override fun maxIntrinsicWidth(height: IntPx): IntPx =
        layoutNodeWrapper.maxIntrinsicWidth(height)

    override fun minIntrinsicHeight(width: IntPx): IntPx =
        layoutNodeWrapper.minIntrinsicHeight(width)

    override fun maxIntrinsicHeight(width: IntPx): IntPx =
        layoutNodeWrapper.maxIntrinsicHeight(width)

    fun place(x: IntPx, y: IntPx) {
        with(InnerPlacementScope) {
            this.parentLayoutDirection = layoutDirection
            val previousParentWidth = parentWidth
            this.parentWidth = layoutNodeWrapper.size.width
            layoutNodeWrapper.place(x, y)
            this.parentWidth = previousParentWidth
        }
    }

    fun draw(canvas: Canvas) = layoutNodeWrapper.draw(canvas)

    /**
     * Carries out a hit test on the [PointerInputModifier]s associated with this [LayoutNode] and
     * all [PointerInputModifier]s on all descendant [LayoutNode]s.
     *
     * If [pointerPositionRelativeToScreen] is within the bounds of any tested
     * [PointerInputModifier]s, the [PointerInputModifier] is added to [hitPointerInputFilters]
     * and true is returned.
     *
     * @param pointerPositionRelativeToScreen The tested pointer position, which is relative to
     * the device screen.
     * @param hitPointerInputFilters The collection that the hit [PointerInputFilter]s will be
     * added to if hit.
     *
     * @return True if any [PointerInputFilter]s were hit and thus added to
     * [hitPointerInputFilters].
     */
    fun hitTest(
        pointerPositionRelativeToScreen: PxPosition,
        hitPointerInputFilters: MutableList<PointerInputFilter>
    ): Boolean {
        return layoutNodeWrapper.hitTest(pointerPositionRelativeToScreen, hitPointerInputFilters)
    }

    /**
     * Returns the alignment line value for a given alignment line without affecting whether
     * the flag for whether the alignment line was read.
     */
    fun getAlignmentLine(line: AlignmentLine): IntPx? {
        val linePos = alignmentLines[line] ?: return null
        var pos = PxPosition(linePos, linePos)
        var wrapper = innerLayoutNodeWrapper
        while (wrapper != layoutNodeWrapper) {
            pos = wrapper.toParentPosition(pos)
            wrapper = wrapper.wrappedBy!!
        }
        pos = wrapper.toParentPosition(pos)
        return if (line is HorizontalAlignmentLine) pos.y.round() else pos.x.round()
    }

    /**
     * Return true if there is a new [OnPositionedModifier] or [OnChildPositionedModifier]
     * assigned to this Layout.
     */
    private fun hasNewPositioningCallback(): Boolean {
        return modifier.foldOut(false) { mod, hasNewCallback ->
            hasNewCallback || when (mod) {
                is OnPositionedModifier -> mod !in onPositionedCallbacks
                is OnChildPositionedModifier -> mod !in onChildPositionedCallbacks
                else -> false
            }
        }
    }

    fun layout() {
        if (needsRelayout) {
            needsRelayout = false
            isLayingOut = true
            val owner = requireOwner()
            owner.observeLayoutModelReads(this) {
                layoutChildren.forEach { child ->
                    child.isPlaced = false
                    if (alignmentLinesRequired && child.dirtyAlignmentLines) {
                        child.needsRelayout = true
                    }
                    if (!child.alignmentLinesRequired) {
                        child.alignmentLinesQueryOwner = alignmentLinesQueryOwner
                    }
                    child.alignmentLinesQueriedSinceLastLayout = false
                }
                positionedDuringMeasurePass = parentLayoutNode?.isMeasuring ?: false ||
                        parentLayoutNode?.positionedDuringMeasurePass ?: false
                innerLayoutNodeWrapper.measureResult.placeChildren(layoutDirection)
                layoutChildren.forEach { child ->
                    child.alignmentLinesRead = child.alignmentLinesQueriedSinceLastLayout
                }
            }

            if (alignmentLinesRequired && dirtyAlignmentLines) {
                alignmentLines.clear()
                layoutChildren.forEach { child ->
                    if (!child.isPlaced) return@forEach
                    child.alignmentLines.keys.forEach { childLine ->
                        val linePositionInContainer = child.getAlignmentLine(childLine)!!
                        // If the line was already provided by a previous child, merge the values.
                        alignmentLines[childLine] = if (childLine in alignmentLines) {
                            childLine.merge(
                                alignmentLines.getValue(childLine),
                                linePositionInContainer
                            )
                        } else {
                            linePositionInContainer
                        }
                    }
                }
                alignmentLines += providedAlignmentLines
                dirtyAlignmentLines = false
            }
            isLayingOut = false
        }
    }

    internal fun calculateAlignmentLines(): Map<AlignmentLine, IntPx> {
        alignmentLinesRead = true
        alignmentLinesQueryOwner = this
        alignmentLinesQueriedSinceLastLayout = true
        if (dirtyAlignmentLines) {
            needsRelayout = true
            layout()
        }
        return alignmentLines
    }

    internal fun handleMeasureResult(measureResult: MeasureScope.MeasureResult) {
        innerLayoutNodeWrapper.measureResult = measureResult
        this.providedAlignmentLines.clear()
        this.providedAlignmentLines += measureResult.alignmentLines
    }

    /**
     * Used to request a new measurement + layout pass from the owner.
     */
    fun requestRemeasure() {
        owner?.onRequestMeasure(this)
    }

    /**
     * Used to request a new draw pass from the owner.
     */
    fun onInvalidate() {
        owner?.onInvalidate(this)
    }

    /**
     * Execute your code within the [block] if you want some code to not be observed for the
     * model reads even if you are currently inside some observed scope like measuring.
     */
    fun ignoreModelReads(block: () -> Unit) {
        requireOwner().pauseModelReadObserveration(block)
    }

    internal fun dispatchOnPositionedCallbacks() {
        if (needsRelayout) {
            return // it hasn't been properly positioned, so don't make a call
        }
        if (!isPlaced) {
            return // it hasn't been placed, so don't make a call
        }
        // There are two types of callbacks:
        // a) when the Layout is positioned - `onPositioned`
        // b) when the child of the Layout is positioned - `onChildPositioned`
        onPositionedCallbacks.fastForEach { it.onPositioned(coordinates) }
        parentLayoutNode?.onChildPositionedCallbacks?.fastForEach {
            it.onChildPositioned(coordinates)
        }
        // iterate through the subtree
        layoutChildren.fastForEach { it.dispatchOnPositionedCallbacks() }
    }

    /**
     * This returns a new List of Modifiers and the coordinates and any extra information
     * that may be useful. This is used for tooling to retrieve layout modifier and layer
     * information.
     */
    fun getModifierInfo(): List<ModifierInfo> {
        val infoList = mutableListOf<ModifierInfo>()
        var wrapper = layoutNodeWrapper

        while (wrapper != innerLayoutNodeWrapper) {
            val info = if (wrapper is LayerWrapper) {
                ModifierInfo(wrapper.modifier, wrapper, wrapper.layer)
            } else {
                wrapper as DelegatingLayoutNodeWrapper<*>
                ModifierInfo(wrapper.modifier, wrapper)
            }
            infoList += info
            wrapper = wrapper.wrapped!!
        }
        return infoList
    }

    override fun toString(): String {
        return "${super.toString()} measureBlocks: $measureBlocks"
    }

    internal companion object {
        private val ErrorMeasureBlocks = object : NoIntrinsicsMeasureBlocks(
            error = "Undefined intrinsics block and it is required"
        ) {
            override fun measure(
                measureScope: MeasureScope,
                measurables: List<Measurable>,
                constraints: Constraints,
                layoutDirection: LayoutDirection
            ) = error("Undefined measure and it is required")
        }

        private fun addLayoutChildren(node: ComponentNode, list: MutableList<LayoutNode>) {
            node.visitChildren { child ->
                if (child is LayoutNode) {
                    list += child
                } else {
                    addLayoutChildren(child, list)
                }
            }
        }
    }
}

/**
 * Used by tooling to examine the modifiers on a [LayoutNode].
 */
class ModifierInfo(
    val modifier: Modifier,
    val coordinates: LayoutCoordinates,
    val extra: Any? = null
)

/**
 * The key used in DataNode.
 *
 * @param T Identifies the type used in the value
 * @property name A unique name identifying the type of the key.
 */
// TODO(mount): Make this inline
class DataNodeKey<T>(val name: String)

/**
 * A ComponentNode that stores a value in the emitted hierarchy
 *
 * @param T The type used for the value
 * @property key The key object used to identify the key
 * @property value The value of the data being stored in the hierarchy
 */
class DataNode<T>(val key: DataNodeKey<T>, var value: T) : ComponentNode() {
    override fun attach(owner: Owner) {
        super.attach(owner)
        parentLayoutNode?.requestRemeasure()
    }
}

/**
 * Returns [ComponentNode.owner] or throws if it is null.
 */
fun ComponentNode.requireOwner(): Owner = owner ?: ErrorMessages.NodeShouldBeAttached.state()

/**
 * Inserts a child [ComponentNode] at a last index. If this ComponentNode [isAttached]
 * then [child] will become [isAttached]ed also. [child] must have a `null` [ComponentNode.parent].
 */
fun ComponentNode.add(child: ComponentNode) {
    insertAt(count, child)
}

/**
 * Executes [block] on first level of [LayoutNode] descendants of this ComponentNode.
 */
fun ComponentNode.visitLayoutChildren(block: (LayoutNode) -> Unit) {
    visitChildren { child ->
        if (child is LayoutNode) {
            block(child)
        } else {
            child.visitLayoutChildren(block)
        }
    }
}

/**
 * Executes [block] on first level of [LayoutNode] descendants of this ComponentNode
 * and returns the last `LayoutNode` to return `true` from [block].
 */
fun ComponentNode.findLastLayoutChild(block: (LayoutNode) -> Boolean): LayoutNode? {
    for (i in count - 1 downTo 0) {
        val child = this[i]
        if (child is LayoutNode) {
            if (block(child)) {
                return child
            }
        } else {
            val layoutNode = child.findLastLayoutChild(block)
            if (layoutNode != null) {
                return layoutNode
            }
        }
    }
    return null
}

/**
 * Executes [selector] on every parent of this [ComponentNode] and returns the closest
 * [ComponentNode] to return `true` from [selector] or null if [selector] returns false
 * for all ancestors.
 */
fun ComponentNode.findClosestParentNode(selector: (ComponentNode) -> Boolean): ComponentNode? {
    // TODO(b/143866294): move this to the testing side after the hierarchy isn't flattened anymore
    var currentParent = parent
    while (currentParent != null) {
        if (selector(currentParent)) {
            return currentParent
        } else {
            currentParent = currentParent.parent
        }
    }

    return null
}

/**
 * Returns `true` if this ComponentNode has no descendant [LayoutNode]s.
 */
fun ComponentNode.hasNoLayoutDescendants() = findLastLayoutChild { true } == null

/**
 * DataNodeKey for ParentData
 */
val ParentDataKey = DataNodeKey<Any>("Compose:ParentData")
