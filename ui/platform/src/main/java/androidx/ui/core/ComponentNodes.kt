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

import androidx.ui.core.semantics.SemanticsAction
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.engine.text.TextDirection
import androidx.ui.painting.Canvas
import androidx.compose.Emittable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Owner implements the connection to the underlying view system. On Android, this connects
 * to Android [android.view.View]s and all layout, draw, input, and accessibility is hooked
 * through them.
 */
interface Owner {
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
     * Returns a position of the owner in its window.
     */
    fun calculatePosition(): PxPosition

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
    fun visitChildren(block: (ComponentNode) -> Unit) {
        children.forEach(block)
    }

    /**
     * Execute [block] on all children of this ComponentNode in reverse order.
     */
    fun visitChildrenReverse(block: (ComponentNode) -> Unit) {
        for (i in children.size - 1 downTo 0) {
            block(children[i])
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
            child.parent = null
            if (attached) {
                child.detach()
            }
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
        visitChildren { child ->
            child.detach()
        }
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
     * The horizontal position relative to its containing LayoutNode
     */
    var layoutX: IntPx = 0.ipx

    /**
     * The vertical position relative to its containing LayoutNode
     */
    var layoutY: IntPx = 0.ipx

    /**
     * The horizontal position relative to its containing RepaintBoundary or root container
     */
    var containerX: IntPx = 0.ipx

    /**
     * The vertical position relative to its containing RepaintBoundary or root container
     */
    var containerY: IntPx = 0.ipx

    override val repaintBoundary: RepaintBoundaryNode? get() = this
}

/**
 * Backing node for handling pointer events.
 */
class PointerInputNode : ComponentNode() {
    var pointerInputHandler: PointerInputHandler = { event, _ -> event }
}

interface DrawNodeScope : DensityReceiver {
    fun drawChildren()
}

/**
 * Backing node for the Draw component.
 */
class DrawNode : ComponentNode() {
    var onPaint: DrawNodeScope.(canvas: Canvas, parentSize: PxSize) -> Unit = { _, _ -> }
        set(value) {
            field = value
            invalidate()
        }

    var needsPaint = true

    override fun attach(owner: Owner) {
        super.attach(owner)
        if (needsPaint) {
            owner.onInvalidate(this)
        }
    }

    fun invalidate() {
        if (!needsPaint) {
            needsPaint = true
            owner?.onInvalidate(this)
        }
    }
}

/**
 * ComplexMeasureBox component methods that must be called from here.
 */
interface MeasurableLayout {
    /**
     * Performs measurement. After completion, the owned [LayoutNode] should be
     * properly sized.
     */
    fun callMeasure(constraints: Constraints)

    /**
     * Places all children that are to be part of the layout.
     */
    fun callLayout()
}

/**
 * Backing node for Layout component.
 */
class LayoutNode : ComponentNode() {
    /**
     * The constraints used the last time [layout] was called.
     */
    var constraints: Constraints = Constraints.tightConstraints(IntPx.Zero, IntPx.Zero)

    var ref: Ref<LayoutNode>?
        get() = null
        set(value) {
            value?.value = this
        }

    // This is a ComplexLayout, but we don't have access to that class from here.
    var layout: MeasurableLayout? = null

    /**
     * The width of this layout
     */
    var width = IntPx.Zero
        private set

    /**
     * The height of this layout
     */
    var height = IntPx.Zero
        private set

    /**
     * The horizontal position within the parent of this layout
     */
    var x = IntPx.Zero
        private set

    /**
     * The vertical position within the parent of this layout
     */
    var y = IntPx.Zero
        private set

    /**
     * Whether or not this has been placed in the hierarchy.
     */
    var visible = true
        private set

    /**
     * `true` when the parent's size depends on this LayoutNode's size
     */
    var affectsParentSize: Boolean = false

    /**
     * `true` when called between [startMeasure] and [endMeasure]
     */
    internal var isInMeasure: Boolean = false

    /**
     * `true` when the layout has been dirtied by [requestRemeasure]. `false` after
     * the measurement has been complete ([resize] has been called).
     */
    var needsRemeasure = true
        internal set

    /**
     * `true` when the layout has been measured or dirtied because the layout
     * lambda accessed a model that has been dirtied.
     */
    var needsRelayout = true
        internal set

    override val parentLayoutNode: LayoutNode?
        get() = super.containingLayoutNode

    override val containingLayoutNode: LayoutNode?
        get() = this

    fun moveTo(x: IntPx, y: IntPx) {
        visible = true
        if (x != this.x || y != this.y) {
            this.x = x
            this.y = y
            owner?.onPositionChange(this)
        }
    }

    fun resize(width: IntPx, height: IntPx) {
        val parent = parentLayoutNode
        needsRemeasure = false // we must have just finished measurement
        if (parent != null && parent.isInMeasure) {
            affectsParentSize = true
        }
        if (width != this.width || height != this.height) {
            this.width = width
            this.height = height
            owner?.onSizeChange(this)
        }
    }

    /**
     * Must be called by the [MeasurableLayout] when the measurement starts.
     */
    fun startMeasure() {
        isInMeasure = true
        visitLayoutChildren { child ->
            child.affectsParentSize = false
            child.visible = false
        }
        owner?.onStartMeasure(this)
    }

    /**
     * Must be called by the [MeasurableLayout] when the measurement ends.
     */
    fun endMeasure() {
        owner?.onEndMeasure(this)
        isInMeasure = false
        needsRelayout = true
    }

    /**
     * Must be called by the [MeasurableLayout] when the layout starts
     */
    fun startLayout() {
        owner?.onStartLayout(this)
    }

    /**
     * Must be called by the [MeasurableLayout] when the layout ends
     */
    fun endLayout() {
        owner?.onEndLayout(this)
        needsRelayout = false
    }

    /**
     * Used by [ComplexLayoutState] to request a new measurement + layout pass from the owner.
     */
    fun requestRemeasure() = owner?.onRequestMeasure(this)
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

private class InvalidatingCallbackProperty<T>(private var value: T) :
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
        val hadValue = this.value != null
        this.value = value
        if ((value != null) != hadValue) {
            thisRef.markNeedsSemanticsUpdate()
        }
    }
}

class SemanticsComponentNode(
    /**
     * If [container] is true, this widget will introduce a new
     * node in the semantics tree. Otherwise, the semantics will be
     * merged with the semantics of any ancestors (if the ancestor allows that).
     *
     * Whether descendants of this widget can add their semantic information to the
     * [SemanticsNode] introduced by this configuration is controlled by
     * [explicitChildNodes].
     */
    container: Boolean = false,
    /**
     * Whether descendants of this widget are allowed to add semantic information
     * to the [SemanticsNode] annotated by this widget.
     *
     * When set to false descendants are allowed to annotate [SemanticNode]s of
     * their parent with the semantic information they want to contribute to the
     * semantic tree.
     * When set to true the only way for descendants to contribute semantic
     * information to the semantic tree is to introduce new explicit
     * [SemanticNode]s to the tree.
     *
     * If the semantics properties of this node include
     * [SemanticsProperties.scopesRoute] set to true, then [explicitChildNodes]
     * must be true also.
     *
     * This setting is often used in combination with [SemanticsConfiguration.isSemanticBoundary]
     * to create semantic boundaries that are either writable or not for children.
     */
    explicitChildNodes: Boolean = false,
    enabled: Boolean? = null,
    checked: Boolean? = null,
    selected: Boolean? = null,
    button: Boolean? = null,
    header: Boolean? = null,
    textField: Boolean? = null,
    focused: Boolean? = null,
    inMutuallyExclusiveGroup: Boolean? = null,
    obscured: Boolean? = null,
    scopesRoute: Boolean? = null,
    namesRoute: Boolean? = null,
    hidden: Boolean? = null,
    label: String? = null,
    value: String? = null,
    hint: String? = null,
    textDirection: TextDirection? = null,
    testTag: String? = null,
    actions: List<SemanticsAction<*>> = emptyList()

) : ComponentNode() {
    private var needsSemanticsUpdate = true
    private var cachedSemanticsConfiguration: SemanticsConfiguration? = null
    val semanticsConfiguration: SemanticsConfiguration
        get() {
            if (cachedSemanticsConfiguration == null) {
                cachedSemanticsConfiguration = generateNodeLocalSemanticsConfiguration()
            }
            return cachedSemanticsConfiguration!!
        }

    private fun generateNodeLocalSemanticsConfiguration(): SemanticsConfiguration? {
        return SemanticsConfiguration().also { config ->
            // TODO(ryanmentley): add more once we enable them in the API
            enabled?.let { enabled ->
                config.isEnabled = enabled
            }
            checked?.let { checked ->
                config.isChecked = checked
            }
            selected?.let { selected ->
                config.isSelected = selected
            }
            button?.let { button ->
                config.isButton = button
            }
            inMutuallyExclusiveGroup?.let { inMutuallyExclusiveGroup ->
                config.isInMutuallyExclusiveGroup = inMutuallyExclusiveGroup
            }
            hidden?.let { hidden ->
                config.isHidden = hidden
            }
            label?.let { label ->
                config.label = label
            }
            value?.let { value ->
                config.value = value
            }
            textDirection?.let { textDirection ->
                config.textDirection = textDirection
            }
            testTag?.let { testTag ->
                config.testTag = testTag
            }
            config.actions = actions
        }
    }

    var container: Boolean by InvalidatingProperty(container)

    var explicitChildNodes: Boolean by InvalidatingProperty(explicitChildNodes)

    /**
     * If non-null, sets the [SemanticsNode.hasCheckedState] semantic to true and
     * the [SemanticsNode.isChecked] semantic to the given value.
     */
    var checked: Boolean? by InvalidatingProperty(checked)

    /**
     * If non-null, sets the [SemanticsNode.hasEnabledState] semantic to true and
     * the [SemanticsNode.isEnabled] semantic to the given value.
     */
    var enabled: Boolean? by InvalidatingProperty(enabled)

    /**
     * If non-null, sets the [SemanticsNode.isSelected] semantic to the given
     * value.
     */
    var selected: Boolean? by InvalidatingProperty(selected)

    /** If non-null, sets the [SemanticsNode.isButton] semantic to the given value. */
    var button: Boolean? by InvalidatingProperty(button)

    /** If non-null, sets the [SemanticsNode.isHeader] semantic to the given value. */
    var header: Boolean? by InvalidatingProperty(header)

    /** If non-null, sets the [SemanticsNode.isTextField] semantic to the given value. */
    var textField: Boolean? by InvalidatingProperty(textField)

    /** If non-null, sets the [SemanticsNode.isFocused] semantic to the given value. */
    var focused: Boolean? by InvalidatingProperty(focused)

    /**
     * If non-null, sets the [SemanticsNode.isInMutuallyExclusiveGroup] semantic
     * to the given value.
     */
    var inMutuallyExclusiveGroup: Boolean? by InvalidatingProperty(inMutuallyExclusiveGroup)

    /**
     * If non-null, sets the [SemanticsNode.isObscured] semantic to the given
     * value.
     */
    var obscured: Boolean? by InvalidatingProperty(obscured)

    /** If non-null, sets the [SemanticsNode.scopesRoute] semantic to the give value. */
    var scopesRoute: Boolean? by InvalidatingProperty(scopesRoute)

    /** If non-null, sets the [SemanticsNode.namesRoute] semantic to the give value. */
    var namesRoute: Boolean? by InvalidatingProperty(namesRoute)

    /**
     * If non-null, sets the [SemanticsNode.isHidden] semantic to the given
     * value.
     */
    var hidden: Boolean? by InvalidatingProperty(hidden)

    /**
     * If non-null, sets the [SemanticsNode.label] semantic to the given value.
     *
     * The reading direction is given by [textDirection].
     */
    var label: String? by InvalidatingProperty(label)

    /**
     * If non-null, sets the [SemanticsNode.value] semantic to the given value.
     *
     * The reading direction is given by [textDirection].
     */
    var value: String? by InvalidatingProperty(value)

    /**
     * If non-null, sets the [SemanticsNode.hint] semantic to the given value.
     *
     * The reading direction is given by [textDirection].
     */
    var hint: String? by InvalidatingProperty(hint)

    /**
     * If non-null, sets the [SemanticsNode.textDirection] semantic to the given value.
     *
     * This must not be null if [label], [hint], or [value] are not null.
     */
    var textDirection: TextDirection? by InvalidatingProperty(textDirection)

    var testTag: String? by InvalidatingProperty(testTag)

    var actions: List<SemanticsAction<*>> by InvalidatingProperty(actions)

    internal fun markNeedsSemanticsUpdate() {
        cachedSemanticsConfiguration = null
        needsSemanticsUpdate = true
    }
}

/**
 * The key used in DataNode.
 * TODO(mount): Make this inline
 *
 * @param T Identifies the type used in the value
 * @property name A unique name identifying the type of the key.
 */
class DataNodeKey<T>(val name: String)

/**
 * A ComponentNode that stores a value in the emitted hierarchy
 *
 * @param T The type used for the value
 * @property key The key object used to identify the key
 * @property value The value of the data being stored in the hierarchy
 */
class DataNode<T>(val key: DataNodeKey<T>, var value: T) : ComponentNode()

/**
 * Returns [ComponentNode.owner] or throws if it is null.
 */
fun ComponentNode.requireOwner(): Owner = owner ?: ErrorMessages.NodeShouldBeAttached.state()

/**
 * The list of child Layouts. It can contain zero or more entries.
 */
fun LayoutNode.childrenLayouts(): List<Any> {
    val layouts = mutableListOf<Any>()
    visitLayoutChildren { child ->
        val layout = child.layout
        if (layout != null) {
            layouts.add(layout)
        }
    }
    return layouts
}

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
 * Converts a global position into a local position within this LayoutNode.
 */
fun LayoutNode.globalToLocal(global: PxPosition, withOwnerOffset: Boolean = true): PxPosition {
    var x: Px = global.x
    var y: Px = global.y
    var node: LayoutNode? = this
    while (node != null) {
        x -= node.x.toPx()
        y -= node.y.toPx()
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
 * Converts a local position within this LayoutNode into a global one.
 */
fun LayoutNode.localToGlobal(local: PxPosition, withOwnerOffset: Boolean = true): PxPosition {
    var x: Px = local.x
    var y: Px = local.y
    var node: LayoutNode? = this
    while (node != null) {
        x += node.x.toPx()
        y += node.y.toPx()
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
        if (node == null) {
            throw IllegalStateException(
                "Current layout is not an ancestor of the provided" +
                        "child layout"
            )
        }
        x += node.x.toPx()
        y += node.y.toPx()
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
fun LayoutNode.positionRelativeToRoot() = localToGlobal(PxPosition.Origin, false)

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
 * Returns `true` if this ComponentNode has no descendant [LayoutNode]s.
 */
fun ComponentNode.hasNoLayoutDescendants() = findLastLayoutChild { true } == null

/**
 * Finds the union of all bounding boxes of LayoutNode children, relative to the containing
 * [LayoutNode].
 *
 * @param node The starting of the ComponentNode hierarchy in which to look for bounding boxes.
 */
fun ComponentNode.calculateChildrenBoundingBox(): IntPxBounds {
    var left = IntPx.Infinity
    var top = IntPx.Infinity
    var right = Int.MIN_VALUE.ipx
    var bottom = Int.MIN_VALUE.ipx

    visitLayoutChildren { layoutNode ->
        left = min(left, layoutNode.x)
        top = min(top, layoutNode.y)
        right = max(right, layoutNode.width + layoutNode.x)
        bottom = max(bottom, layoutNode.height + layoutNode.y)
    }

    return IntPxBounds(left = left, top = top, right = right, bottom = bottom)
}