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

import androidx.compose.Emittable
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.focus.FocusDetailedState.Active
import androidx.ui.focus.FocusDetailedState.Inactive
import androidx.ui.focus.FocusDetailedState.Disabled
import androidx.ui.focus.FocusDetailedState.Captured
import androidx.ui.focus.FocusDetailedState.ActiveParent
import androidx.ui.graphics.Canvas
import androidx.ui.engine.geometry.Shape
import androidx.ui.core.focus.findParentFocusNode
import androidx.ui.core.focus.ownerHasFocus
import androidx.ui.core.focus.requestFocusForOwner
import androidx.ui.focus.FocusDetailedState
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Owner implements the connection to the underlying view system. On Android, this connects
 * to Android [android.view.View]s and all layout, draw, input, and accessibility is hooked
 * through them.
 */
interface Owner {
    val density: Density

    /**
     * Called from a [DrawNode], this registers with the underlying view system that a
     * redraw of the given [drawNode] is required. It may cause other nodes to redraw, if
     * necessary.
     */
    fun onInvalidate(drawNode: DrawNode)

    /**
     * Called by [LayoutNode] to indicate the new size of [layoutNode].
     * The owner may need to track updated layouts.
     */
    fun onSizeChange(layoutNode: LayoutNode)

    /**
     * Called by [LayoutNode] to indicate the new position of [layoutNode].
     * The owner may need to track updated layouts.
     */
    fun onPositionChange(layoutNode: LayoutNode)

    /**
     * Called by [LayoutNode] to request the Owner a new measurement+layout.
     */
    fun onRequestMeasure(layoutNode: LayoutNode)

    /**
     * Called by [ComponentNode] when it is attached to the view system and now has an owner.
     * This is used by [Owner] to update [ComponentNode.ownerData] and track which nodes are
     * associated with it. It will only be called when [node] is not already attached to an
     * owner.
     */
    fun onAttach(node: ComponentNode)

    /**
     * Called by [ComponentNode] when it is detached from the view system, such as during
     * [ComponentNode.emitRemoveAt]. This will only be called for [node]s that are already
     * [ComponentNode.attach]ed.
     */
    fun onDetach(node: ComponentNode)

    /**
     * Called when measure starts.
     */
    fun onStartMeasure(layoutNode: LayoutNode)

    /**
     * Called when measure ends.
     */
    fun onEndMeasure(layoutNode: LayoutNode)

    /**
     * Called when layout (placement) starts.
     */
    fun onStartLayout(layoutNode: LayoutNode)

    /**
     * Called when layout (placement) ends.
     */
    fun onEndLayout(layoutNode: LayoutNode)

    /**
     * Returns the most global position of the owner that Compose can access (such as the device
     * screen).
     */
    fun calculatePosition(): IntPxPosition

    /**
     * Called when some params of [RepaintBoundaryNode] are updated.
     * This is not causing re-recording of the RepaintBoundary, but updates params
     * like outline, clipping, elevation or alpha.
     */
    fun onRepaintBoundaryParamsChange(repaintBoundaryNode: RepaintBoundaryNode)

    /**
     * Observing the model reads can be temporary disabled.
     * For example if we are currently within the measure stage and we want some code block to
     * be skipped from the observing we disable if before calling the block, execute block and
     * then enable it again.
     */
    fun enableModelReadObserving(enabled: Boolean)

    val measureIteration: Long
}

/**
 * The base type for all nodes from the tree generated from a component hierarchy.
 *
 * Specific components are backed by a tree of nodes: Draw, Layout, SemanticsComponentNode, GestureDetector.
 * All other components are not represented in the backing hierarchy.
 */
sealed class ComponentNode : Emittable {
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
     * Protected method to find the parent's layout node. LayoutNode returns itself, but
     * all other ComponentNodes return the parent's `containingLayoutNode`.
     */
    protected open val containingLayoutNode: LayoutNode?
        get() = parent?.containingLayoutNode

    /**
     * If this is a [RepaintBoundaryNode], `this` is returned, otherwise the nearest ancestor
     * `RepaintBoundaryNode` or `null` if there are no ancestor `RepaintBoundaryNode`s.
     */
    open val repaintBoundary: RepaintBoundaryNode? get() = parent?.repaintBoundary

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
    override fun emitInsertAt(index: Int, instance: Emittable) {
        if (instance !is ComponentNode) {
            ErrorMessages.OnlyComponents.state()
        }
        ErrorMessages.ComponentNodeHasParent.validateState(instance.parent == null)
        ErrorMessages.OwnerAlreadyAttached.validateState(instance.owner == null)
        instance.parent = this
        children.add(index, instance)

        val owner = this.owner
        if (owner != null) {
            instance.attach(owner)
        }
    }

    /**
     * Removes one or more children, starting at [index].
     */
    override fun emitRemoveAt(index: Int, count: Int) {
        ErrorMessages.CountOutOfRange.validateArg(count >= 0, count)
        val attached = owner != null
        for (i in index + count - 1 downTo index) {
            val child = children.removeAt(i)
            if (attached) {
                child.detach()
            }
            child.parent = null
        }
    }

    override fun emitMove(from: Int, to: Int, count: Int) {
        if (from == to) {
            return // nothing to do
        }
        for (i in 0 until count) {
            // if "from" is after "to," the from index moves because we're inserting before it
            val fromIndex = if (from > to) from + i else from
            val toIndex = if (from > to) to + i else to + count - 2
            val child = children.removeAt(fromIndex)
            children.add(toIndex, child)
        }
        containingLayoutNode?.layoutChildrenDirty = true
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
        visitChildren { child ->
            child.detach()
        }
        val owner = owner ?: ErrorMessages.OwnerAlreadyDetached.state()
        owner.onDetach(this)
        this.owner = null
        depth = 0
    }
}

/**
 * Returns true if this [ComponentNode] currently has an [ComponentNode.owner].  Semantically,
 * this means that the ComponentNode is currently a part of a component tree.
 */
fun ComponentNode.isAttached() = owner != null

class RepaintBoundaryNode(val name: String?) : ComponentNode() {

    /**
     * The shape used to calculate an outline of the RepaintBoundary.
     */
    var shape: Shape? = null
        set(value) {
            if (field != value) {
                field = value
                owner?.onRepaintBoundaryParamsChange(this)
            }
        }

    /**
     * If true RepaintBoundary will be clipped by the outline of it's [shape]
     */
    var clipToShape: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                owner?.onRepaintBoundaryParamsChange(this)
            }
        }

    /**
     * The z-coordinate at which to place this physical object.
     */
    var elevation: Dp = 0.dp
        set(value) {
            if (field != value) {
                field = value
                owner?.onRepaintBoundaryParamsChange(this)
            }
        }

    /**
     * The fraction of children's alpha value.
     */
    var opacity: Float = 1f
        set(value) {
            if (field != value) {
                require(value in 0f..1f) { "Opacity should be within [0, 1] range" }
                field = value
                owner?.onRepaintBoundaryParamsChange(this)
            }
        }

    override val repaintBoundary: RepaintBoundaryNode? get() = this
}

// TODO(b/143778512): Why are the properties vars?  Shouldn't they be vals defined in the
//  constructor such that they both must be provided?
/**
 * Backing node for handling pointer events.
 */
class PointerInputNode : ComponentNode() {
    /**
     * Invoked when pointers that previously hit this PointerInputNode have changed.
     */
    var pointerInputHandler: PointerInputHandler = { event, _, _ -> event }

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
        set(value) { _recompose = value }

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
    var onPaintWithChildren: (DrawReceiver.(canvas: Canvas, parentSize: PxSize) -> Unit)? = null
        set(value) {
            field = value
            invalidate()
        }

    var onPaint: (DensityScope.(canvas: Canvas, parentSize: PxSize) -> Unit)? = null
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

private val Unmeasured = IntPxSize(IntPx.Zero, IntPx.Zero)
private val Origin = IntPxPosition(IntPx.Zero, IntPx.Zero)

/**
 * Backing node for Layout component.
 *
 * Measuring a [LayoutNode] as a [Measurable] will measure the node's content as adjusted by
 * [modifier]. All layout state such as [modifiedSize] and [modifiedPosition] also reflect
 * the modified state of the node.
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
            constraints: Constraints
        ): MeasureScope.LayoutResult

        /**
         * The function used to calculate [IntrinsicMeasurable.minIntrinsicWidth].
         */
        fun minIntrinsicWidth(
            densityScope: DensityScope,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx
        ): IntPx

        /**
         * The lambda used to calculate [IntrinsicMeasurable.minIntrinsicHeight].
         */
        fun minIntrinsicHeight(
            densityScope: DensityScope,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx
        ): IntPx

        /**
         * The function used to calculate [IntrinsicMeasurable.maxIntrinsicWidth].
         */
        fun maxIntrinsicWidth(
            densityScope: DensityScope,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx
        ): IntPx

        /**
         * The lambda used to calculate [IntrinsicMeasurable.maxIntrinsicHeight].
         */
        fun maxIntrinsicHeight(
            densityScope: DensityScope,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx
        ): IntPx
    }

    abstract class NoIntristicsMeasureBlocks(private val error: String) : MeasureBlocks {
        override fun minIntrinsicWidth(
            densityScope: DensityScope,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx
        ) = error(error)

        override fun minIntrinsicHeight(
            densityScope: DensityScope,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx
        ) = error(error)

        override fun maxIntrinsicWidth(
            densityScope: DensityScope,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx
        ) = error(error)

        override fun maxIntrinsicHeight(
            densityScope: DensityScope,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx
        ) = error(error)
    }

    // TODO(popam): used for multi composable children. Consider removing if possible.
    abstract class InnerMeasureScope : MeasureScope() {
        abstract val layoutNode: LayoutNode
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
    val measureScope: MeasureScope = object : InnerMeasureScope() {
        override val density: Density
            get() = owner?.density ?: Density(1f)
        override val layoutNode: LayoutNode = this@LayoutNode
    }

    /**
     * The constraints used the last time [layout] was called.
     */
    var constraints: Constraints = Constraints.tightConstraints(IntPx.Zero, IntPx.Zero)

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
    val width: IntPx get() = modifiedSize.width

    /**
     * The measured height of this layout and all of its [modifier]s. Shortcut for `size.height`.
     */
    val height: IntPx get() = modifiedSize.height

    /**
     * The alignment lines of this layout, inherited + intrinsic
     */
    internal val alignmentLines: MutableMap<AlignmentLine, IntPx> = hashMapOf()

    /**
     * The alignment lines provided by this layout at the last measurement
     */
    internal val providedAlignmentLines: MutableMap<AlignmentLine, IntPx> = hashMapOf()

    /**
     * The measured size of this layout and all of its [modifier]s.
     */
    val modifiedSize: IntPxSize get() = layoutNodeWrapper.size

    /**
     * The horizontal position of this layout and all of its [modifier]s within its parent.
     */
    val x: IntPx get() = modifiedPosition.x

    /**
     * The vertical position of this layout and all of its [modifier]s within its parent.
     */
    val y: IntPx get() = modifiedPosition.y

    /**
     * The position of this layout and all of its [modifier] within its parent.
     */
    val modifiedPosition: IntPxPosition get() = layoutNodeWrapper.position

    /**
     * The position of the inner layout node content
     */
    val contentPosition: IntPxPosition get() = innerLayoutNodeWrapper.position

    /**
     * The size of the inner layout node content
     */
    val contentSize: IntPxSize get() = innerLayoutNodeWrapper.size

    /**
     * Whether or not this has been placed in the hierarchy.
     */
    var isPlaced = false
        internal set

    /**
     * `true` when the parent's size depends on this LayoutNode's size
     */
    var affectsParentSize: Boolean = true

    /**
     * `true` when inside [measure]
     */
    var isMeasuring: Boolean = false

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
        internal set

    /**
     * `true` when the layout has been measured or dirtied because the layout
     * lambda accessed a model that has been dirtied.
     */
    var needsRelayout = false
        internal set

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
     * The [MeasureScope.LayoutResult] obtained from the last measurement.
     * It should only be used for running the positioning block of the layout.
     */
    private var lastLayoutResult: MeasureScope.LayoutResult = measureScope.layout(0.ipx, 0.ipx) {}

    /**
     * A local version of [Owner.measureIteration] to ensure that [MeasureBlocks.measure]
     * is not called multiple times within a measure pass.
     */
    private var measureIteration = 0L

    /**
     * Identifies when [layoutChildren] needs to be recalculated or if it can use
     * the cached value.
     */
    internal var layoutChildrenDirty = false

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
    private var parentDataNode: DataNode<*>? = null
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

    private val innerLayoutNodeWrapper: LayoutNodeWrapper = InnerPlaceable()
    private var layoutNodeWrapper = innerLayoutNodeWrapper

    /**
     * The [Modifier] currently applied to this node.
     */
    var modifier: Modifier = Modifier.None
        set(value) {
            if (value == field) return
            field = value

            // Rebuild layoutNodeWrapper
            val oldPlaceable = layoutNodeWrapper
            layoutNodeWrapper = modifier.foldOut(innerLayoutNodeWrapper) { mod, toWrap ->
                if (mod is LayoutModifier) ModifiedLayoutNode(toWrap, mod) else toWrap
            }
            // Optimize the case where the layout itself is not modified. A common reason for
            // this is if no wrapping actually occurs above because no LayoutModifiers are
            // present in the modifier chain.
            if (oldPlaceable != layoutNodeWrapper) {
                requestRemeasure()
            }
        }

    /**
     * Measurable and Placeable type that has a position.
     */
    private abstract class LayoutNodeWrapper : Placeable(), Measurable {
        var position = Origin

        /**
         * Assigns a layout size to this [LayoutNodeWrapper] given the assigned innermost size
         * from the call to [MeasureScope.layout]. Assigns and returns [modifiedSize].
         */
        abstract fun layoutSize(innermostSize: IntPxSize): IntPxSize
    }

    private inner class InnerPlaceable : LayoutNodeWrapper(), DensityScope {
        override fun measure(constraints: Constraints): Placeable {
            val layoutResult = measureBlocks.measure(measureScope, layoutChildren, constraints)
            handleLayoutResult(layoutResult)
            return this
        }

        override val parentData: Any?
            get() = parentDataNode?.value

        override fun minIntrinsicWidth(height: IntPx): IntPx =
            measureBlocks.minIntrinsicWidth(measureScope, layoutChildren, height)

        override fun minIntrinsicHeight(width: IntPx): IntPx =
            measureBlocks.minIntrinsicHeight(measureScope, layoutChildren, width)

        override fun maxIntrinsicWidth(height: IntPx): IntPx =
            measureBlocks.maxIntrinsicWidth(measureScope, layoutChildren, height)

        override fun maxIntrinsicHeight(width: IntPx): IntPx =
            measureBlocks.maxIntrinsicHeight(measureScope, layoutChildren, width)

        override var size: IntPxSize = Unmeasured
            private set

        override fun performPlace(position: IntPxPosition) {
            isPlaced = true
            if (position != this.position) {
                this.position = position
                owner?.onPositionChange(this@LayoutNode)
            }
            placeChildren()
        }

        override val density: Density get() = measureScope.density

        override fun layoutSize(innermostSize: IntPxSize): IntPxSize {
            size = innermostSize
            return innermostSize
        }

        override fun get(line: AlignmentLine): IntPx? = calculateAlignmentLines()[line]
    }

    private inner class ModifiedLayoutNode(
        val wrapped: LayoutNodeWrapper,
        val layoutModifier: LayoutModifier
    ) : LayoutNodeWrapper() {

        /**
         * The [Placeable] returned by measuring [wrapped] in [measure].
         * Used to avoid creating more wrapper objects than necessary since [ModifiedLayoutNode]
         * also
         */
        private var measuredPlaceable: Placeable? = null

        /**
         * The [Constraints] used in the current measurement of this modified node wrapper.
         * See [withMeasuredConstraints]
         */
        private var measuredConstraints: Constraints? = null

        /**
         * Sets [measuredConstraints] for the duration of [block].
         */
        private inline fun <R> withMeasuredConstraints(
            constraints: Constraints,
            block: () -> R
        ): R = try {
            measuredConstraints = constraints
            block()
        } finally {
            measuredConstraints = null
        }

        /**
         * ParentData provided through the parentData node will override the data provided
         * through a modifier
         */
        override val parentData: Any?
            get() = with(layoutModifier) {
                parentDataNode?.value ?: measureScope.modifyParentData(wrapped.parentData)
            }

        override fun measure(constraints: Constraints): Placeable = with(layoutModifier) {
            val measureResult = withMeasuredConstraints(constraints) {
                wrapped.measure(measureScope.modifyConstraints(constraints))
            }
            measuredPlaceable = measureResult
            this@ModifiedLayoutNode
        }

        override fun minIntrinsicWidth(height: IntPx): IntPx = with(layoutModifier) {
            measureScope.minIntrinsicWidthOf(wrapped, height)
        }

        override fun maxIntrinsicWidth(height: IntPx): IntPx = with(layoutModifier) {
            measureScope.maxIntrinsicWidthOf(wrapped, height)
        }

        override fun minIntrinsicHeight(width: IntPx): IntPx = with(layoutModifier) {
            measureScope.minIntrinsicHeightOf(wrapped, width)
        }

        override fun maxIntrinsicHeight(width: IntPx): IntPx = with(layoutModifier) {
            measureScope.maxIntrinsicHeightOf(wrapped, width)
        }

        override var size: IntPxSize = Unmeasured
            private set

        override fun performPlace(position: IntPxPosition) {
            val placeable = measuredPlaceable ?: error("Placeable not measured")
            this.position = with(layoutModifier) {
                measureScope.modifyPosition(position, placeable.size, size)
            }
            placeable.place(this.position)
        }

        override fun get(line: AlignmentLine): IntPx? = with(layoutModifier) {
            measureScope.modifyAlignmentLine(line, wrapped[line])
        }

        override fun layoutSize(innermostSize: IntPxSize): IntPxSize = with(layoutModifier) {
            val constraints = measuredConstraints ?: error("must be called during measurement")
            measureScope.modifySize(constraints, wrapped.layoutSize(innermostSize)).also {
                size = it
            }
        }
    }

    internal val coordinates: LayoutCoordinates = LayoutNodeCoordinates(this)

    override fun attach(owner: Owner) {
        super.attach(owner)
        requestRemeasure()
        parentDataDirty = true
        parentLayoutNode?.layoutChildrenDirty = true
    }

    override fun detach() {
        parentLayoutNode?.layoutChildrenDirty = true
        parentLayoutNode?.requestRemeasure()
        parentDataDirty = true
        alignmentLinesQueryOwner = null
        super.detach()
    }

    override fun measure(constraints: Constraints): Placeable {
        val owner = requireOwner()
        val iteration = owner.measureIteration
        check(measureIteration != iteration) {
            "measure() may not be called multiple times on the same Measurable"
        }
        measureIteration = iteration
        if (this.constraints == constraints && !needsRemeasure) {
            val parent = parentLayoutNode
            if (parent != null && parent.isMeasuring) {
                affectsParentSize = true
            }
            return layoutNodeWrapper // we're already measured to this size, don't do anything
        }

        isMeasuring = true
        dirtyAlignmentLines = true
        layoutChildren.forEach { child ->
            child.affectsParentSize = false
        }
        owner.onStartMeasure(this)
        try {
            this.constraints = constraints

            layoutNodeWrapper.measure(constraints)
        } finally {
            owner.onEndMeasure(this)
            isMeasuring = false
        }
        needsRemeasure = false
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
        with(Placeable.PlacementScope) {
            layoutNodeWrapper.place(x, y)
        }
    }

    fun placeChildren() {
        if (needsRelayout) {
            val owner = requireOwner()
            owner.onStartLayout(this)
            layoutChildren.forEach { child ->
                child.isPlaced = false
                if (alignmentLinesRequired && child.dirtyAlignmentLines) child.needsRelayout = true
                if (!child.alignmentLinesRequired) {
                    child.alignmentLinesQueryOwner = alignmentLinesQueryOwner
                }
                child.alignmentLinesQueriedSinceLastLayout = false
            }
            positionedDuringMeasurePass = parentLayoutNode?.isMeasuring ?: false ||
                    parentLayoutNode?.positionedDuringMeasurePass ?: false
            lastLayoutResult.placeChildren(Placeable.PlacementScope)
            layoutChildren.forEach { child ->
                child.alignmentLinesRead = child.alignmentLinesQueriedSinceLastLayout
            }
            owner.onEndLayout(this)
            needsRelayout = false

            if (alignmentLinesRequired && dirtyAlignmentLines) {
                alignmentLines.clear()
                layoutChildren.forEach { child ->
                    if (!child.isPlaced) return@forEach
                    child.alignmentLines.entries.forEach { (childLine, linePosition) ->
                        val linePositionInContainer = linePosition +
                                if (childLine.horizontal) child.y else child.x
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
        }
    }

    internal fun calculateAlignmentLines(): Map<AlignmentLine, IntPx> {
        alignmentLinesRead = true
        alignmentLinesQueryOwner = this
        alignmentLinesQueriedSinceLastLayout = true
        if (dirtyAlignmentLines) {
            needsRelayout = true
            placeChildren()
        }
        return alignmentLines
    }

    internal fun handleLayoutResult(layoutResult: MeasureScope.LayoutResult) {
        val oldSize = modifiedSize
        val outerSize = layoutNodeWrapper.layoutSize(
            IntPxSize(layoutResult.width, layoutResult.height)
        )

        // The more idiomatic, `if (parentLayoutNode?.isMeasuring == true)` causes boxing
        val parent = parentLayoutNode
        @Suppress("SimplifyBooleanWithConstants")
        if (parent != null && parent.isMeasuring == true) {
            affectsParentSize = true
        }
        if (oldSize != outerSize) {
            // TODO this may also need to be updated when the modifier chain size changes
            owner?.onSizeChange(this@LayoutNode)
        }
        this.providedAlignmentLines.clear()
        this.providedAlignmentLines += layoutResult.alignmentLines
        this.lastLayoutResult = layoutResult
    }

    /**
     * Used by `ComplexLayoutState` to request a new measurement + layout pass from the owner.
     */
    fun requestRemeasure() {
        owner?.onRequestMeasure(this)
    }

    /**
     * Execute your code within the [block] if you want some code to not be observed for the
     * model reads even if you are currently inside some observed scope like measuring.
     */
    inline fun ignoreModelReads(crossinline block: () -> Unit) {
        val owner = requireOwner()
        owner.enableModelReadObserving(false)
        block()
        owner.enableModelReadObserving(true)
    }

    internal fun dispatchOnPositionedCallbacks() {
        // There are two types of callbacks:
        // a) when the Layout is positioned - `onPositioned`
        // b) when the child of the Layout is positioned - `onChildPositioned`
        walkOnPosition(this, this.coordinates)
        walkOnChildPositioned(this, this.coordinates)
    }

    internal companion object {
        @Suppress("UNCHECKED_CAST")
        private fun walkOnPosition(node: ComponentNode, coordinates: LayoutCoordinates) {
            node.visitChildren { child ->
                if (child !is LayoutNode) {
                    if (child is DataNode<*> && child.key === OnPositionedKey) {
                        val method = child.value as (LayoutCoordinates) -> Unit
                        method(coordinates)
                    }
                    walkOnPosition(child, coordinates)
                } else {
                    if (!child.needsRelayout) {
                        child.dispatchOnPositionedCallbacks()
                    }
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun walkOnChildPositioned(layoutNode: LayoutNode, coordinates: LayoutCoordinates) {
            var node = layoutNode.parent
            while (node != null && node !is LayoutNode) {
                if (node is DataNode<*> && node.key === OnChildPositionedKey) {
                    val method = node.value as (LayoutCoordinates) -> Unit
                    method(coordinates)
                }
                node = node.parent
            }
        }

        private val ErrorMeasureBlocks = object : NoIntristicsMeasureBlocks(
            error = "Undefined intrinsics block and it is required"
        ) {
            override fun measure(
                measureScope: MeasureScope,
                measurables: List<Measurable>,
                constraints: Constraints
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

private class InvalidatingProperty<T>(private var value: T) :
    ReadWriteProperty<SemanticsComponentNode, T> {
    override fun getValue(thisRef: SemanticsComponentNode, property: KProperty<*>): T {
        return value
    }

    override fun setValue(
        thisRef: SemanticsComponentNode,
        property: KProperty<*>,
        value: T
    ) {
        if (this.value == value) {
            return
        }
        this.value = value
        thisRef.markNeedsSemanticsUpdate()
    }
}

class SemanticsComponentNode(
    // TODO(ryanmentley): probably take away these default values
    semanticsConfiguration: SemanticsConfiguration = SemanticsConfiguration(),
    /**
     * If [container] is true, this composable will introduce a new
     * node in the semantics tree. Otherwise, the semantics will be
     * merged with the semantics of any ancestors (if the ancestor allows that).
     *
     * Whether descendants of this composable can add their semantic information to the
     * [SemanticsNode][androidx.ui.core.semantics.SemanticNode] introduced by this configuration is
     * controlled by [explicitChildNodes].
     */
    @Suppress("KDocUnresolvedReference")
    container: Boolean = false,
    /**
     * Whether descendants of this composable are allowed to add semantic information
     * to the [SemanticsNode][androidx.ui.core.semantics.SemanticNode] annotated by this composable.
     *
     * When set to false descendants are allowed to annotate [SemanticNodes][androidx.ui.core
     * .semantics.SemanticNode] of
     * their parent with the semantic information they want to contribute to the
     * semantic tree.
     * When set to true the only way for descendants to contribute semantic
     * information to the semantic tree is to introduce new explicit
     * [SemanticNodes][androidx.ui.core.semantics.SemanticNode] to the tree.
     *
     * If the semantics properties of this node include
     * [scopesRoute][androidx.ui.semantics.SemanticsProperties.scopesRoute] set to
     * true, then [explicitChildNodes] must be true also.
     *
     * This setting is often used in combination with [SemanticsConfiguration.isSemanticBoundary]
     * to create semantic boundaries that are either writable or not for children.
     */
    @Suppress("KDocUnresolvedReference")
    explicitChildNodes: Boolean = false
) : ComponentNode() {
    private var needsSemanticsUpdate = true

    var container: Boolean by InvalidatingProperty(container)

    var explicitChildNodes: Boolean by InvalidatingProperty(explicitChildNodes)

    // TODO(ryanmentley): this should be smarter and invalidate less
    var semanticsConfiguration: SemanticsConfiguration
            by InvalidatingProperty(semanticsConfiguration)

    internal fun markNeedsSemanticsUpdate() {
        needsSemanticsUpdate = true
    }
}

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
    emitInsertAt(count, child)
}

class Ref<T> {
    var value: T? = null
}

/**
 * Converts a [PxPosition] relative to a global context into a [PxPosition] that is relative
 * to this [LayoutNode].
 *
 * If [withOwnerOffset] is true (which is the default), the [global] parameter is interpreted as
 * being a position relative to the application window. Otherwise, the [global] parameter is
 * interpreted to be relative to the root of the compose context.
 */
fun LayoutNode.globalToLocal(global: PxPosition, withOwnerOffset: Boolean = true): PxPosition {
    var x: Px = global.x
    var y: Px = global.y
    var node: LayoutNode? = this
    while (node != null) {
        val pos = node.contentPosition
        x -= pos.x.toPx()
        y -= pos.y.toPx()
        node = node.parentLayoutNode
    }
    if (withOwnerOffset) {
        val ownerPosition = requireOwner().calculatePosition()
        x -= ownerPosition.x
        y -= ownerPosition.y
    }
    return PxPosition(x, y)
}

/**
 * Converts an [PxPosition] that is relative to this [LayoutNode] into one that is relative to
 * a more global context.
 *
 * If [withOwnerOffset] is true (which is the default), the return value will be relative to the
 * application window.  Otherwise, the location is relative to the root of the compose context.
 */
fun LayoutNode.localToGlobal(local: PxPosition, withOwnerOffset: Boolean = true): PxPosition {
    var x: Px = local.x
    var y: Px = local.y
    var node: LayoutNode? = this
    while (node != null) {
        val pos = node.contentPosition
        x += pos.x.toPx()
        y += pos.y.toPx()
        node = node.parentLayoutNode
    }
    if (withOwnerOffset) {
        val ownerPosition = requireOwner().calculatePosition()
        x += ownerPosition.x
        y += ownerPosition.y
    }
    return PxPosition(x, y)
}

/**
 * Converts a [IntPxPosition] relative to a global context into a [IntPxPosition] that is relative
 * to this [LayoutNode].
 *
 * If [withOwnerOffset] is true (which is the default), the [global] parameter is interpreted as
 * being a position relative to the application window. Otherwise, the [global] parameter is
 * interpreted to be relative to the root of the compose context.
 */
fun LayoutNode.globalToLocal(global: IntPxPosition, withOwnerOffset: Boolean = true):
        IntPxPosition {
    var x: IntPx = global.x
    var y: IntPx = global.y
    var node: LayoutNode? = this
    while (node != null) {
        val pos = node.contentPosition
        x -= pos.x
        y -= pos.y
        node = node.parentLayoutNode
    }
    if (withOwnerOffset) {
        val ownerPosition = requireOwner().calculatePosition()
        x -= ownerPosition.x
        y -= ownerPosition.y
    }
    return IntPxPosition(x, y)
}

/**
 * Converts an [IntPxPosition] that is relative to this [LayoutNode] into one that is relative to
 * a more global context.
 *
 * If [withOwnerOffset] is true (which is the default), the return value will be relative to the
 * app window.  Otherwise, the location is relative to the root of the compose context.
 */
fun LayoutNode.localToGlobal(local: IntPxPosition, withOwnerOffset: Boolean = true): IntPxPosition {
    var x: IntPx = local.x
    var y: IntPx = local.y
    var node: LayoutNode? = this
    while (node != null) {
        val pos = node.contentPosition
        x += pos.x
        y += pos.y
        node = node.parentLayoutNode
    }
    if (withOwnerOffset) {
        val ownerPosition = requireOwner().calculatePosition()
        x += ownerPosition.x
        y += ownerPosition.y
    }
    return IntPxPosition(x, y)
}

/**
 * Converts a child LayoutNode position into a local position within this LayoutNode.
 */
fun LayoutNode.childToLocal(child: LayoutNode, childLocal: PxPosition): PxPosition {
    if (child === this) {
        return childLocal
    }
    var x: Px = childLocal.x
    var y: Px = childLocal.y
    var node: LayoutNode? = child
    while (true) {
        checkNotNull(node) {
            "Current layout is not an ancestor of the provided child layout"
        }
        val pos = node.contentPosition
        x += pos.x.toPx()
        y += pos.y.toPx()
        node = node.parentLayoutNode
        if (node === this) {
            // found the node
            break
        }
    }
    return PxPosition(x, y)
}

/**
 * Calculates the position of this [LayoutNode] relative to the root of the ui tree.
 */
fun LayoutNode.positionRelativeToRoot() = localToGlobal(IntPxPosition.Origin, false)

/**
 * Calculates the position of this [LayoutNode] relative to the provided ancestor.
 */
fun LayoutNode.positionRelativeToAncestor(ancestor: LayoutNode) =
    ancestor.childToLocal(this, PxPosition.Origin)

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

/**
 * DataNodeKey for OnPositioned callback
 */
val OnPositionedKey = DataNodeKey<(LayoutCoordinates) -> Unit>("Compose:OnPositioned")

/**
 * DataNodeKey for OnChildPositioned callback
 */
val OnChildPositionedKey =
    DataNodeKey<(LayoutCoordinates) -> Unit>("Compose:OnChildPositioned")

/**
 * A LayoutCoordinates implementation based on LayoutNode.
 */
private class LayoutNodeCoordinates(
    private val layoutNode: LayoutNode
) : LayoutCoordinates {

    override val position get() = layoutNode.contentPosition.toPxPosition()

    override val size get() = layoutNode.contentSize.toPxSize()

    override val providedAlignmentLines get() = layoutNode.providedAlignmentLines

    override val parentCoordinates get() = layoutNode.parentLayoutNode?.coordinates

    override fun globalToLocal(global: PxPosition) = layoutNode.globalToLocal(global)

    override fun localToGlobal(local: PxPosition) = layoutNode.localToGlobal(local)

    override fun localToRoot(local: PxPosition) = layoutNode.localToGlobal(local, false)

    override fun childToLocal(child: LayoutCoordinates, childLocal: PxPosition): PxPosition {
        if (child !is LayoutNodeCoordinates) {
            throw IllegalArgumentException("Incorrect child provided.")
        }
        return layoutNode.childToLocal(child.layoutNode, childLocal)
    }
}
