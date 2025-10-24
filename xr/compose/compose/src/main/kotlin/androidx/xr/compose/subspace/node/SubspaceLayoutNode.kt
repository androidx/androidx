/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.subspace.node

import androidx.compose.runtime.CompositionLocalMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.subspace.layout.CoreEntityNode
import androidx.xr.compose.subspace.layout.LayoutSubspaceMeasureScope
import androidx.xr.compose.subspace.layout.OpaqueEntity
import androidx.xr.compose.subspace.layout.ParentLayoutParamsAdjustable
import androidx.xr.compose.subspace.layout.ParentLayoutParamsModifier
import androidx.xr.compose.subspace.layout.SubspaceLayoutCoordinates
import androidx.xr.compose.subspace.layout.SubspaceMeasurable
import androidx.xr.compose.subspace.layout.SubspaceMeasurePolicy
import androidx.xr.compose.subspace.layout.SubspaceMeasureResult
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.SubspacePlaceable
import androidx.xr.compose.subspace.layout.SubspaceRootMeasurePolicy
import androidx.xr.compose.subspace.layout.applyCoreEntityNodes
import androidx.xr.compose.subspace.layout.requireCoordinator
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.Entity
import java.util.concurrent.atomic.AtomicInteger

private var lastIdentifier = AtomicInteger(0)

internal fun generateSemanticsId() = lastIdentifier.incrementAndGet()

private val DefaultDensity = Density(1f)

/**
 * An element in the Subspace layout hierarchy (spatial scene graph), built with Compose UI for
 * subspace.
 *
 * This class is based on [androidx.compose.ui.node.LayoutNode].
 */
internal class SubspaceLayoutNode : ComposeSubspaceNode {
    /**
     * The layout state the node is currently in.
     *
     * [layoutState] may be read in other contexts to help determine if/when the node should be
     * measured/placed, but can only be set by the node itself.
     */
    internal var layoutState: LayoutState = LayoutState.Idle
        private set

    /**
     * The children of this [SubspaceLayoutNode], controlled by [insertAt], [move], and [removeAt].
     */
    internal val children: MutableList<SubspaceLayoutNode> = mutableListOf()

    /** The parent node in the [SubspaceLayoutNode] hierarchy. */
    internal var parent: SubspaceLayoutNode? = null

    /** Instance of [SubspaceMeasurableLayout] to aid with measure/layout phases. */
    val measurableLayout: SubspaceMeasurableLayout = SubspaceMeasurableLayout()

    /** The element system [SubspaceOwner]. This value is `null` until [attach] is called */
    internal var owner: SubspaceOwner? = null
        private set

    internal val isAttached: Boolean
        get() = owner != null

    internal val nodes: SubspaceModifierNodeChain = SubspaceModifierNodeChain(this)

    /**
     * The depth of this node in the [SubspaceLayoutNode] tree hierarchy. The root node has a depth
     * of 0. Its direct children have a depth of 1, and so on.
     *
     * This value is calculated by traversing up the [parent] chain and is used to sort invalidated
     * nodes, ensuring parents are processed before their children during measure and layout passes.
     */
    internal var depth: Int = 0

    override var measurePolicy: SubspaceMeasurePolicy = ErrorMeasurePolicy

    override var modifier: SubspaceModifier = SubspaceModifier
        set(value) {
            field = value
            nodes.updateFrom(value)
        }

    override var entity: OpaqueEntity? = null
        set(value) {
            check(field == null) { "overwriting non-null CoreEntity is not supported" }
            field = value
            if (value is CoreEntity) {
                value.layout = this
            }
        }

    internal var coreEntity: CoreEntity?
        get() = entity as? CoreEntity
        set(value) {
            entity = value
        }

    override var compositionLocalMap: CompositionLocalMap = CompositionLocalMap.Empty
        set(value) {
            field = value
            density = value[LocalDensity]
            layoutDirection = value[LocalLayoutDirection]
        }

    internal var density: Density = DefaultDensity
        private set

    internal var layoutDirection: LayoutDirection = LayoutDirection.Ltr
        private set

    private var ignoreMeasureRequests = false

    /**
     * This function sets up CoreEntity parent/child relationships that reflect the parent/child
     * relationships of the corresponding SubspaceLayoutNodes. This should be called any time the
     * `parent` or `coreEntity` fields are updated.
     */
    private fun syncCoreEntityHierarchy() {
        coreEntity?.parent = findCoreEntityParent(this)
    }

    /** Inserts a child [SubspaceLayoutNode] at the given [index]. */
    internal fun insertAt(index: Int, instance: SubspaceLayoutNode) {
        check(instance.parent == null) {
            "Cannot insert $instance because it already has a parent." +
                " This tree: " +
                debugTreeToString() +
                " Parent tree: " +
                parent?.debugTreeToString()
        }
        check(instance.owner == null) {
            "Cannot insert $instance because it already has an owner." +
                " This tree: " +
                debugTreeToString() +
                " Other tree: " +
                instance.debugTreeToString()
        }

        instance.parent = this
        children.add(index, instance)

        owner?.let { instance.attach(it) }
    }

    /**
     * Moves [count] elements starting at index [from] to index [to].
     *
     * The [to] index is related to the position before the change, so, for example, to move an
     * element at position 1 to after the element at position 2, [from] should be `1` and [to]
     * should be `3`. If the elements were [SubspaceLayoutNode] instances, A B C D E, calling
     * `move(1, 3, 1)` would result in the nodes being reordered to A C B D E.
     */
    internal fun move(from: Int, to: Int, count: Int) {
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

    /** Removes one or more children, starting at [index]. */
    internal fun removeAt(index: Int, count: Int) {
        require(count >= 0) { "count ($count) must be greater than 0." }

        for (i in index + count - 1 downTo index) {
            onChildRemoved(children[i])
        }

        children.removeAll(children.subList(index, index + count))
    }

    /** Removes all children nodes. */
    internal fun removeAll() {
        children.reversed().forEach { child -> onChildRemoved(child) }

        children.clear()
    }

    /** Called when the [child] node is removed from this [SubspaceLayoutNode] hierarchy. */
    private fun onChildRemoved(child: SubspaceLayoutNode) {
        owner?.let { child.detach() }
        child.parent = null
    }

    /**
     * Sets the [SubspaceOwner] of this node.
     *
     * This SubspaceLayoutNode must not already be attached and [subspaceOwner] must match the
     * [parent]'s [subspaceOwner].
     */
    internal fun attach(subspaceOwner: SubspaceOwner) {
        check(this.owner == null) {
            "Cannot attach $this as it already is attached. Tree: " + debugTreeToString()
        }
        check(parent == null || parent?.owner == subspaceOwner) {
            "Attaching to a different owner($subspaceOwner) than the parent's owner" +
                "(${parent?.owner})." +
                " This tree: " +
                debugTreeToString() +
                " Parent tree: " +
                parent?.debugTreeToString()
        }

        this.owner = subspaceOwner
        var calculatedDepth = 0
        var nodeParent = parent
        while (nodeParent != null) {
            calculatedDepth++
            nodeParent = nodeParent.parent
        }
        this.depth = calculatedDepth

        subspaceOwner.onAttach(this)
        syncCoreEntityHierarchy()

        nodes.markAsAttached()
        children.forEach { child -> child.attach(subspaceOwner) }
        nodes.runOnAttach()
    }

    /**
     * Detaches this node from the [owner].
     *
     * The [owner] must not be `null` when this method is called.
     *
     * This will also [detach] all children. After executing, the [owner] will be `null`.
     */
    internal fun detach() {
        val owner = owner

        checkNotNull(owner) {
            "Cannot detach node that is already detached!  Tree: " + parent?.debugTreeToString()
        }

        this.depth = 0
        nodes.runOnDetach()
        ignoreMeasureRequests { children.forEach { child -> child.detach() } }
        nodes.markAsDetached()
        coreEntity?.dispose()

        owner.onDetach(this)
        this.owner = null
    }

    private inline fun <T> ignoreMeasureRequests(block: () -> T): T {
        ignoreMeasureRequests = true
        val result = block()
        ignoreMeasureRequests = false
        return result
    }

    internal fun requestMeasure() {
        if (!ignoreMeasureRequests) {
            owner?.requestMeasure(this)
        }
    }

    internal fun requestLayout() {
        owner?.requestLayout(this)
    }

    override fun toString(): String {
        return measurableLayout.config.getOrElse(SemanticsProperties.TestTag) { super.toString() }
    }

    /** Call this method to see a dump of the SpatialLayoutNode tree structure. */
    @Suppress("unused")
    internal fun debugTreeToString(depth: Int = 0): String {
        val tree = StringBuilder()
        val depthString = "  ".repeat(depth)
        tree.append("$depthString|-${toString()}\n")

        var currentNode: SubspaceModifier.Node? = nodes.head
        while (currentNode != null && currentNode != nodes.tail) {
            tree.append("$depthString  *-$currentNode\n")
            currentNode = currentNode.child
        }

        children.forEach { child -> tree.append(child.debugTreeToString(depth + 1)) }

        var treeString = tree.toString()
        if (depth == 0) {
            // Delete trailing newline
            treeString = treeString.substring(0, treeString.length - 1)
        }

        return treeString
    }

    /** Call this method to see a dump of the Jetpack XR node hierarchy. */
    @Suppress("unused")
    internal fun debugEntityTreeToString(depth: Int = 0): String {
        val tree = StringBuilder()
        val depthString = "  ".repeat(depth)
        var nextDepth = depth
        if (entity != null) {
            tree.append("$depthString|-$coreEntity -> ${findCoreEntityParent(this)}\n")
            nextDepth++
        }

        children.forEach { child -> tree.append(child.debugEntityTreeToString(nextDepth)) }

        var treeString = tree.toString()
        if (depth == 0 && treeString.isNotEmpty()) {
            // Delete trailing newline
            treeString = treeString.substring(0, treeString.length - 1)
        }

        return treeString
    }

    private val outerCoordinator
        get() = nodes.getAll<SubspaceLayoutModifierNode>().firstOrNull()?.requireCoordinator()

    /**
     * Measures this layout node using the most recently provided constraints.
     *
     * Returns true if the measured size has changed.
     */
    internal fun remeasure(): Boolean =
        outerCoordinator?.remeasure() ?: measurableLayout.remeasure()

    /** Places this layout node using the most recently provided pose. */
    internal fun replace() = outerCoordinator?.replace() ?: measurableLayout.replace()

    /**
     * A [SubspaceMeasurable] and [SubspacePlaceable] object that is used to measure and lay out the
     * children of this node.
     *
     * See [androidx.compose.ui.node.NodeCoordinator]
     */
    inner class SubspaceMeasurableLayout :
        SubspaceMeasurable, SubspaceLayoutCoordinates, SubspaceSemanticsInfo, SubspacePlaceable() {

        private var lastConstraints: VolumeConstraints? = null
        private var subspaceMeasureResult: SubspaceMeasureResult? = null
        private var layoutPose: Pose? = null

        /** Unique ID used by semantics libraries. */
        override val semanticsId: Int = generateSemanticsId()

        /**
         * The tail node of [SubspaceModifierNodeChain].
         *
         * This node is used to mark the end of the modifier chain.
         */
        val tail: SubspaceModifier.Node = TailModifierNode()

        override fun measure(constraints: VolumeConstraints): SubspacePlaceable {
            layoutState = LayoutState.Measuring
            val placeable = nodes.measureChain(constraints, ::measureJustThis)
            layoutState = LayoutState.Idle
            return placeable
        }

        override fun adjustParams(params: ParentLayoutParamsAdjustable) {
            nodes.getAll<ParentLayoutParamsModifier>().forEach { it.adjustParams(params) }
        }

        private fun measureJustThis(constraints: VolumeConstraints): SubspacePlaceable {
            lastConstraints = constraints

            subspaceMeasureResult =
                with(measurePolicy) {
                    LayoutSubspaceMeasureScope(this@SubspaceLayoutNode)
                        .measure(
                            this@SubspaceLayoutNode.children.map { it.measurableLayout }.toList(),
                            constraints,
                        )
                }

            measuredWidth = subspaceMeasureResult!!.width
            measuredHeight = subspaceMeasureResult!!.height
            measuredDepth = subspaceMeasureResult!!.depth

            return this
        }

        /**
         * Measures this layout node using the most recently provided constraints.
         *
         * Returns true if the measured size has changed.
         */
        internal fun remeasure(): Boolean {
            return lastConstraints?.let {
                val oldSize = size
                measure(it)
                oldSize != size
            } ?: false
        }

        /**
         * Places the children of this node at the given pose.
         *
         * @param pose The pose to place the children at, with translation in pixels.
         */
        public override fun placeAt(pose: Pose) {
            layoutState = LayoutState.LayingOut

            layoutPose = pose

            coreEntity?.applyCoreEntityNodes(nodes.getAll<CoreEntityNode>())
            coreEntity?.updatePoseFromLayout()
            coreEntity?.size = IntVolumeSize(measuredWidth, measuredHeight, measuredDepth)

            subspaceMeasureResult?.placeChildren(
                object : SubspacePlacementScope() {
                    override val coordinates = this@SubspaceMeasurableLayout
                }
            )

            // Call coordinates-aware callbacks after the node and its children are placed.
            nodes.getAll<LayoutCoordinatesAwareModifierNode>().forEach {
                it.onLayoutCoordinates(this)
            }

            layoutState = LayoutState.Idle
        }

        /** Places this layout node using the most recently provided pose. */
        internal fun replace() {
            layoutPose?.let { placeAt(it) }
        }

        override val pose: Pose
            get() = layoutPose ?: Pose.Identity

        override val poseInParentEntity: Pose
            get() = coordinatesInParentEntity?.poseInParentEntity?.compose(pose) ?: pose

        /** The position of this node relative to the root of this Compose hierarchy, in pixels. */
        override val poseInRoot: Pose
            get() = parentCoordinates?.poseInRoot?.compose(pose) ?: pose

        /**
         * The coordinates of the immediate parent in the layout hierarchy.
         *
         * It includes application from any [SubspaceLayoutModifierNode] instances in the modifier
         * chain of this node. This property first checks for any layout modifiers on the current
         * node. If modifiers are present, it returns the coordinates of the outermost modifier. If
         * no modifiers are present, it falls back to returning the coordinates of the parent
         * layout.
         *
         * Returns `null` only for the root of the hierarchy.
         */
        override val parentCoordinates: SubspaceLayoutCoordinates?
            get() =
                nodes.getLast<SubspaceLayoutModifierNode>()?.requireCoordinator()
                    ?: parentLayoutCoordinates

        /**
         * The coordinates of the parent layout, skipping any modifiers on this node.
         *
         * Returns `null` only for the root of the hierarchy.
         */
        override val parentLayoutCoordinates: SubspaceLayoutCoordinates?
            get() = parent?.measurableLayout

        /**
         * The layout coordinates up to the nearest parent [CoreEntity], including mutations from
         * any [SubspaceLayoutModifierNode] instances applied to this node.
         *
         * This applies the layout changes of all [SubspaceLayoutModifierNode] instances in the
         * modifier chain.
         *
         * This property continues the coordinate search up the hierarchy, starting with any local
         * layout modifiers.
         *
         * It returns `null` only under a specific condition: when there are no layout modifiers on
         * the current node AND its immediate parent either is the root or has a `CoreEntity`.
         */
        private val coordinatesInParentEntity: SubspaceLayoutCoordinates?
            get() =
                nodes.getLast<SubspaceLayoutModifierNode>()?.requireCoordinator()
                    ?: parentCoordinatesInParentEntity

        /** Traverse up the parent hierarchy until we reach a node with an entity. */
        internal val parentCoordinatesInParentEntity: SubspaceLayoutCoordinates?
            get() = if (parent?.entity == null) parent?.measurableLayout else null

        override val semanticsChildren: MutableList<SubspaceSemanticsInfo>
            get() {
                val list: MutableList<SubspaceSemanticsInfo> = mutableListOf()
                fillOneLayerOfSemanticsWrappers(list)
                return list
            }

        override val semanticsParent: SubspaceSemanticsInfo?
            get() {
                var node: SubspaceLayoutNode? = parent
                while (node != null) {
                    if (node.hasSemantics) {
                        return node.measurableLayout
                    }
                    node = node.parent
                }
                return null
            }

        private fun SubspaceLayoutNode.fillOneLayerOfSemanticsWrappers(
            list: MutableList<SubspaceSemanticsInfo>
        ) {
            children.fastForEach { child ->
                if (child.hasSemantics) {
                    list.add(child.measurableLayout)
                } else {
                    child.fillOneLayerOfSemanticsWrappers(list)
                }
            }
        }

        private val SubspaceLayoutNode.hasSemantics: Boolean
            get() = nodes.getLast<SubspaceSemanticsModifierNode>() != null

        override val semanticsEntity: Entity?
            get() = coreEntity?.semanticsEntity

        override val size: IntVolumeSize
            get() {
                return coreEntity?.size
                    ?: IntVolumeSize(measuredWidth, measuredHeight, measuredDepth)
            }

        override fun toString(): String {
            return this@SubspaceLayoutNode.toString()
        }

        /**
         * The semantics configuration of this node.
         *
         * This includes all properties attached as modifiers to the current layout node.
         */
        override val config: SemanticsConfiguration
            get() {
                val config = SemanticsConfiguration()
                nodes.getAll<SubspaceSemanticsModifierNode>().forEach {
                    with(config) { with(it) { applySemantics() } }
                }
                return config
            }
    }

    /** Companion object for [SubspaceLayoutNode]. */
    companion object {
        private val ErrorMeasurePolicy: SubspaceMeasurePolicy = SubspaceMeasurePolicy { _, _ ->
            error("Undefined measure and it is required")
        }

        /**
         * A [SubspaceMeasurePolicy] that is used for the root node of the Subspace layout
         * hierarchy.
         *
         * Note: Root node itself has no size outside its children.
         */
        val RootMeasurePolicy: SubspaceMeasurePolicy = SubspaceRootMeasurePolicy()

        /** A constructor that creates a new [SubspaceLayoutNode]. */
        val Constructor: () -> SubspaceLayoutNode = { SubspaceLayoutNode() }

        /** Walk up the parent hierarchy to find the closest ancestor attached to a [CoreEntity]. */
        private fun findCoreEntityParent(node: SubspaceLayoutNode) =
            generateSequence(node.parent) { it.parent }.firstNotNullOfOrNull { it.coreEntity }
    }

    internal enum class LayoutState {
        /** Node is currently being measured. */
        Measuring,

        /** Node is currently being laid out. */
        LayingOut,

        /**
         * Node is not currently measuring or laying out. It could be pending measure or pending
         * layout.
         */
        Idle,
    }
}

internal class TailModifierNode : SubspaceModifier.Node() {
    override fun toString(): String {
        return "<tail>"
    }
}
