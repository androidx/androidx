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

package androidx.compose.ui.semantics

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.calculateOuterBoundsInRoot
import androidx.compose.ui.layout.calculateUnclippedBoundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.boundsInRoot
import androidx.compose.ui.node.effectiveBoundsInRoot
import androidx.compose.ui.node.requireCoordinator
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.useMinimumTouchTarget
import androidx.compose.ui.platform.ExperimentalComposeUiHostApi
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.IntSize
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal fun SemanticsNode(layoutNode: LayoutNode, mergingEnabled: Boolean) =
    SemanticsNode(
        layoutNode.nodes.head(Nodes.Semantics)!!.node,
        mergingEnabled,
        layoutNode,
        layoutNode.semanticsConfiguration ?: SemanticsConfiguration(),
    )

internal fun SemanticsNode(
    /*
     * This is expected to be the outermost semantics modifier on a layout node.
     */
    outerSemanticsNode: SemanticsModifierNode,
    /**
     * mergingEnabled specifies whether mergeDescendants config has any effect.
     *
     * If true, then mergeDescendants nodes will merge up all properties from child semantics nodes
     * and remove those children from "children", with the exception of nodes that themselves have
     * mergeDescendants. If false, then mergeDescendants has no effect.
     *
     * mergingEnabled is typically true or false consistently on every node of a SemanticsNode tree.
     */
    mergingEnabled: Boolean,
    /** The [LayoutNode] that this is associated with. */
    layoutNode: LayoutNode = outerSemanticsNode.requireLayoutNode(),
) =
    SemanticsNode(
        outerSemanticsNode.node,
        mergingEnabled,
        layoutNode,
        layoutNode.semanticsConfiguration ?: SemanticsConfiguration(),
    )

/**
 * A list of key/value pairs associated with a layout node or its subtree.
 *
 * Each SemanticsNode takes its id and initial key/value list from the outermost modifier on one
 * layout node. It also contains the "collapsed" configuration of any other semantics modifiers on
 * the same layout node, and if "mergeDescendants" is specified and enabled, also the "merged"
 * configuration of its subtree.
 */
public class SemanticsNode
internal constructor(
    internal val outerSemanticsNode: Modifier.Node,
    public val mergingEnabled: Boolean,
    internal val layoutNode: LayoutNode,
    unmergedConfig: SemanticsConfiguration,
) {
    internal val _unmergedConfig: SemanticsConfiguration = unmergedConfig

    /**
     * The unmerged semantics configuration directly declared on this node.
     *
     * Contains properties defined on this node without merging semantics from descendants.
     */
    @ExperimentalComposeUiHostApi
    public val unmergedConfig: SemanticsPropertyProvider
        get() = _unmergedConfig

    /**
     * The merged [SemanticsPropertyProvider] of this node and its descendants, even if this node
     * was constructed with [mergingEnabled] set to false.
     *
     * If this node does not merge descendant semantics, returns [unmergedConfig]. Otherwise each
     * call walks the merging subtree and allocates a new merged configuration, so the result
     * reflects the current tree. To read a single key use [getMergedOrNull]; to scan the merged
     * properties without building a merged configuration use [forEachMergedProperty] or
     * [anyMergedProperty].
     */
    @ExperimentalComposeUiHostApi
    public val mergedConfig: SemanticsPropertyProvider
        get() {
            if (!_unmergedConfig.isMergingSemanticsOfDescendants) {
                return _unmergedConfig
            }
            return if (mergingEnabled) {
                config
            } else {
                SemanticsNode(outerSemanticsNode, true, layoutNode, _unmergedConfig).config
            }
        }

    /**
     * Visits every property that contributes to [mergedConfig] without building the merged
     * configuration.
     *
     * Visits this node's own properties first, then, if [unmergedConfig] merges descendants and
     * doesn't clear them, the unmerged properties of each descendant that [mergedConfig] merges, in
     * merge order. A key can be visited more than once and values are not combined with
     * [SemanticsPropertyKey.merge]; use [getMergedOrNull] for the merged value of one key.
     *
     * @param action Called with each key and value.
     */
    @ExperimentalComposeUiHostApi
    public fun forEachMergedProperty(action: (key: SemanticsPropertyKey<*>, value: Any?) -> Unit) {
        val config = _unmergedConfig
        config.props.forEach(action)
        if (isFake || !config.isMergingSemanticsOfDescendants || config.isClearingSemantics) {
            return
        }
        layoutNode.forEachMergedDescendantProperty(action)
    }

    /**
     * Returns `true` if at least one property that contributes to [mergedConfig] matches
     * [predicate]. Visits properties in the same order as [forEachMergedProperty] and stops at the
     * first match, without building the merged configuration.
     *
     * @param predicate Called with each key and value.
     */
    @ExperimentalComposeUiHostApi
    public fun anyMergedProperty(
        predicate: (key: SemanticsPropertyKey<*>, value: Any?) -> Boolean
    ): Boolean {
        val config = _unmergedConfig
        if (config.props.any(predicate)) return true
        if (isFake || !config.isMergingSemanticsOfDescendants || config.isClearingSemantics) {
            return false
        }
        return layoutNode.anyMergedDescendantProperty(predicate)
    }

    /**
     * Returns the value of [key] in [mergedConfig] without building the merged configuration, or
     * `null` if neither this node nor any merged descendant sets it.
     *
     * Applies [SemanticsPropertyKey.merge] to the values in the same order as [mergedConfig].
     *
     * @param key Property key to retrieve.
     */
    @ExperimentalComposeUiHostApi
    @Suppress("UNCHECKED_CAST")
    public fun <T> getMergedOrNull(key: SemanticsPropertyKey<T>): T? {
        val config = _unmergedConfig
        val value = config.props[key] as T?
        if (isFake || !config.isMergingSemanticsOfDescendants || config.isClearingSemantics) {
            return value
        }
        return layoutNode.mergeDescendantValue(key, value)
    }

    // We emit fake nodes for several cases. One is to prevent the content description clobbering
    // issue. Another case is  temporary workaround to retrieve default role ordering for Button
    // and other selection controls.
    private var fakeNodeParent: SemanticsNode? = null
    internal val isFake: Boolean
        get() = fakeNodeParent != null

    /** The [LayoutInfo] that this is associated with. */
    public val layoutInfo: LayoutInfo
        get() = layoutNode

    /**
     * Calculates the bounding box in root coordinates of the semantics modifier node that provides
     * [SemanticsProperties.Shape] for this node, writing the result into [rect]. Bounds are not
     * clipped.
     *
     * If this node does not define a shape, [rect] is set to `(0, 0, 0, 0)` and this method returns
     * `false`. If the shape is defined but its modifier can't be matched or isn't attached, [rect]
     * receives the outer bounds of the layout ([LayoutInfo.calculateOuterBoundsInRoot]) and this
     * method returns `true`.
     *
     * @param rect The [MutableRect] into which the calculated bounds are written.
     * @return `true` if this node defines a [SemanticsProperties.Shape], `false` otherwise.
     */
    @ExperimentalComposeUiHostApi
    public fun calculateShapeBoundsInRoot(rect: MutableRect): Boolean {
        val shape = unmergedConfig.getOrNull(SemanticsProperties.Shape)
        if (shape == null) {
            rect.set(0f, 0f, 0f, 0f)
            return false
        }
        val shapeNodeMatcher =
            object : SemanticsPropertyReceiver {
                var hasMatchedShape: Boolean = false

                override fun <T> set(key: SemanticsPropertyKey<T>, value: T) {
                    if (key == SemanticsProperties.Shape && value === shape) {
                        hasMatchedShape = true
                    }
                }
            }
        val shapeSemanticsModifierNode =
            layoutNode.nodes.firstFromHead(Nodes.Semantics) {
                with(it) {
                    shapeNodeMatcher.run {
                        applySemantics()
                        return@firstFromHead hasMatchedShape
                    }
                }
            }
        if (shapeSemanticsModifierNode?.node?.isAttached != true) {
            layoutNode.calculateOuterBoundsInRoot(rect)
            return true
        }
        shapeSemanticsModifierNode.requireLayoutCoordinates().calculateUnclippedBoundsInRoot(rect)
        return true
    }

    /** The [root][RootForTest] this node is attached to. */
    public val root: RootForTest?
        get() = layoutNode.owner?.rootForTest

    /**
     * For newer AccessibilityNodeInfo-based integration test frameworks, it can be matched in the
     * extras with key "androidx.compose.ui.semantics.id"
     */
    public val id: Int = layoutNode.semanticsId

    // GEOMETRY

    /**
     * The rectangle of the touchable area.
     *
     * If this is a clickable region, this is the rectangle that accepts touch input. This can be
     * larger than [size] when the layout is less than [ViewConfiguration.minimumTouchTargetSize]
     */
    public val touchBoundsInRoot: Rect
        @OptIn(ExperimentalComposeUiHostApi::class)
        get() {
            if (isFake) return parent?.touchBoundsInRoot ?: Rect.Zero
            val semanticsModifierNode = findSemanticsModifierNodeToGetBounds()
            if (semanticsModifierNode == null) {
                // If no node is found that has isImportantForBounds == true, then we fallback to
                // the inner coordinator to get the bounds.
                return layoutNode.innerCoordinator.touchBoundsInRoot()
            }
            return semanticsModifierNode.node.effectiveBoundsInRoot(
                _unmergedConfig.useMinimumTouchTarget,
                clipBounds = true,
            )
        }

    /**
     * The touchable bounds of this node in root coordinates, clipped to any ancestor clipping
     * container whose size is at least the minimum touch target size. Used when subtracting this
     * node's area from unaccounted space so a child clipped inside a scrollable container cannot
     * occlude siblings outside that container.
     */
    internal val clippedTouchBoundsInRoot: Rect
        @OptIn(ExperimentalComposeUiHostApi::class)
        get() {
            val semanticsModifierNode = findSemanticsModifierNodeToGetBounds()
            if (semanticsModifierNode == null) {
                return layoutNode.innerCoordinator.touchBoundsInRoot(
                    clipToParentIfLargerThanMinTouchTarget = true
                )
            }
            return semanticsModifierNode.node.effectiveBoundsInRoot(
                _unmergedConfig.useMinimumTouchTarget,
                clipBounds = true,
                clipToParentIfLargerThanMinTouchTarget = true,
            )
        }

    internal val unclippedBoundsInRoot: Rect
        @OptIn(ExperimentalComposeUiHostApi::class)
        get() {
            val semanticsModifierNode = findSemanticsModifierNodeToGetBounds()
            if (semanticsModifierNode == null) {
                // If no node is found that has isImportantForBounds == true, then we fallback to
                // the inner coordinator to get the bounds.
                return layoutNode.innerCoordinator.boundsInRoot(false)
            }
            return semanticsModifierNode.node.effectiveBoundsInRoot(
                _unmergedConfig.useMinimumTouchTarget,
                clipBounds = false,
            )
        }

    /** The size of the bounding box for this node, with no clipping applied */
    public val size: IntSize
        get() = findCoordinatorToGetBounds()?.size ?: IntSize.Zero

    /**
     * The bounding box for this node relative to the root of this Compose hierarchy, with clipping
     * applied. To get the bounds with no clipping applied, use Rect([positionInRoot],
     * [size].toSize())
     */
    public val boundsInRoot: Rect
        get() = findCoordinatorToGetBounds()?.takeIf { it.isAttached }?.boundsInRoot() ?: Rect.Zero

    /**
     * The position of this node relative to the root of this Compose hierarchy, with no clipping
     * applied
     */
    public val positionInRoot: Offset
        get() =
            findCoordinatorToGetBounds()?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero

    /**
     * The bounding box for this node relative to the window, with clipping applied. To get the
     * bounds with no clipping applied, use PxBounds([positionInWindow], [size].toSize())
     */
    public val boundsInWindow: Rect
        get() =
            findCoordinatorToGetBounds()?.takeIf { it.isAttached }?.boundsInWindow() ?: Rect.Zero

    /** The position of this node relative to the window, with no clipping applied */
    public val positionInWindow: Offset
        get() =
            findCoordinatorToGetBounds()?.takeIf { it.isAttached }?.positionInWindow()
                ?: Offset.Zero

    /** The position of this node relative to the screen, with no clipping applied */
    public val positionOnScreen: Offset
        get() =
            findCoordinatorToGetBounds()?.takeIf { it.isAttached }?.positionOnScreen()
                ?: Offset.Zero

    /**
     * Calculates the bounding box of this node relative to its parent semantics node, with clipping
     * applied, writing the result into [rect].
     *
     * If this node is not attached, or has no parent semantics node with bounds, [rect] is set to
     * empty bounds at `(0, 0, 0, 0)`.
     *
     * @param rect The [MutableRect] into which the calculated bounds are written.
     */
    @ExperimentalComposeUiHostApi
    public fun calculateBoundsInParent(rect: MutableRect) {
        val coordinator = findCoordinatorToGetBounds()?.takeIf { it.isAttached }

        if (coordinator == null) {
            rect.set(0f, 0f, 0f, 0f)
            return
        }

        var currentLayoutNode: LayoutNode? =
            if (fakeNodeParent != null) {
                fakeNodeParent?.layoutNode
            } else {
                findParentLayoutNode(this.layoutNode)
            }

        while (currentLayoutNode != null) {
            val parentCoordinatorForBounds =
                currentLayoutNode.nodes
                    .firstFromHead(Nodes.Semantics) { it.isImportantForBounds }
                    ?.requireCoordinator(Nodes.Semantics)
                    ?.takeIf { it.isAttached }
            if (parentCoordinatorForBounds != null) {
                parentCoordinatorForBounds.localBoundingBoxOf(coordinator, rect)
                return
            }
            currentLayoutNode = findParentLayoutNode(currentLayoutNode)
        }
        rect.set(0f, 0f, 0f, 0f)
    }

    @OptIn(ExperimentalComposeUiHostApi::class)
    internal val boundsInParent: Rect
        get() {
            val rect = MutableRect(0f, 0f, 0f, 0f)
            calculateBoundsInParent(rect)
            return rect.toRect()
        }

    /** Whether this node is transparent. */
    internal val isTransparent: Boolean
        get() = findCoordinatorToGetBounds()?.isTransparent() ?: false

    /**
     * Returns the position of an [alignment line][AlignmentLine], or [AlignmentLine.Unspecified] if
     * the line is not provided.
     */
    public fun getAlignmentLinePosition(alignmentLine: AlignmentLine): Int {
        return findCoordinatorToGetBounds()?.get(alignmentLine) ?: AlignmentLine.Unspecified
    }

    /**
     * Returns the effective composite alpha (opacity) of this node, computed by resolving the
     * product of this node's layer alpha and all ancestor layer alphas.
     */
    public fun computeEffectiveAlpha(): Float {
        var alpha = 1f
        var coordinator: NodeCoordinator? = layoutNode.innerCoordinator
        while (coordinator != null) {
            // Checking whether coordinator.layer is non-null
            // handles both implicit and explicit layers.
            if (coordinator.layer != null) {
                alpha *= coordinator.alpha
                if (alpha == 0f) break
            }
            coordinator = coordinator.wrappedBy
        }
        return alpha
    }

    // CHILDREN

    /**
     * The list of semantics properties of this node.
     *
     * This includes all properties attached as modifiers to the current layout node. In addition,
     * if mergeDescendants and mergingEnabled are both true, then it also includes the semantics
     * properties of descendant nodes.
     */
    // TODO(b/184376083): This is too expensive for a val (full subtree recreation every call);
    //               optimize this when the merging algorithm is improved.
    public val config: SemanticsConfiguration
        @OptIn(ExperimentalComposeUiHostApi::class)
        get() {
            if (isMergingSemanticsOfDescendants) {
                val mergedConfig = _unmergedConfig.copy()
                mergeConfig(
                    // TODO(b/384549982): Pass in the unmerged children instead of an empty list.
                    mutableListOf(),
                    mergedConfig,
                )
                return mergedConfig
            } else {
                return _unmergedConfig
            }
        }

    @OptIn(ExperimentalComposeUiHostApi::class)
    private fun mergeConfig(
        unmergedChildren: MutableList<SemanticsNode>,
        mergedConfig: SemanticsConfiguration,
    ) {
        if (!_unmergedConfig.isClearingSemantics) {
            unmergedChildren.forEachUnmergedChild { child ->
                // Don't merge children that themselves merge all their descendants (because that
                // indicates they're independently screen-reader-focusable).
                if (!child.isMergingSemanticsOfDescendants) {
                    mergedConfig.mergeChild(child._unmergedConfig)
                    child.mergeConfig(unmergedChildren, mergedConfig)
                }
            }
        }
    }

    /**
     * Whether this node merges semantics from descendant nodes in this tree.
     *
     * `true` only when [mergingEnabled] is `true` and [unmergedConfig] declares
     * [SemanticsPropertyProvider.isMergingSemanticsOfDescendants]. In the unmerged tree
     * ([mergingEnabled] `false`) this is always `false`, while
     * `unmergedConfig.isMergingSemanticsOfDescendants` still reports the declared value.
     */
    @ExperimentalComposeUiHostApi
    public val isMergingSemanticsOfDescendants: Boolean
        get() = mergingEnabled && _unmergedConfig.isMergingSemanticsOfDescendants

    internal fun unmergedChildren(
        unmergedChildren: MutableList<SemanticsNode> = mutableListOf(),
        includeFakeNodes: Boolean = false,
        includeDeactivatedNodes: Boolean = false,
    ): List<SemanticsNode> {
        // TODO(lmr): we should be able to do this more efficiently using visitSubtree
        if (isFake) return unmergedChildren

        layoutNode.fillOneLayerOfSemanticsWrappers(unmergedChildren, includeDeactivatedNodes)

        if (includeFakeNodes) {
            emitFakeNodes(unmergedChildren)
        }

        return unmergedChildren
    }

    private fun LayoutNode.fillOneLayerOfSemanticsWrappers(
        list: MutableList<SemanticsNode>,
        includeDeactivatedNodes: Boolean,
    ) {
        // TODO(lmr): visitChildren would be great for this but we would lose the zSorted bit...
        //  i wonder if we can optimize this for the common case of no z-sortedness going on.
        zSortedChildren.forEach { child ->
            // TODO(b/290936195): In some conditions it appears that children here can be
            //  unattached. We just guard against that here as a "quick fix" but we need to
            //  understand why this is happening and followup with a proper fix.
            if (child.isAttached && (includeDeactivatedNodes || !child.isDeactivated)) {
                if (child.nodes.has(Nodes.Semantics)) {
                    list.add(SemanticsNode(child, mergingEnabled))
                } else {
                    child.fillOneLayerOfSemanticsWrappers(list, includeDeactivatedNodes)
                }
            }
        }
    }

    /**
     * Contains the children in inverse hit test order (i.e. paint order).
     *
     * Note that if mergingEnabled and mergeDescendants are both true, then there are no children
     * (except those that are themselves mergeDescendants).
     */
    // TODO(b/184376083): This is too expensive for a val (full subtree recreation every call);
    //               optimize this when the merging algorithm is improved.
    public val children: List<SemanticsNode>
        get() = getChildren()

    /**
     * Collects child semantics nodes for the accessibility hierarchy in inverse hit test order
     * (paint order), appending them to [outChildren].
     *
     * Unlike [children], if this node is marked with [clearAndSetSemantics], its children are
     * cleared and nothing is appended to [outChildren]. Synthetic accessibility nodes are included,
     * while inline text link test marker nodes ([SemanticsProperties.LinkTestMarker]) are excluded
     * because they are exposed via text spans.
     *
     * @param outChildren The mutable list to which the child semantics nodes are appended.
     */
    @ExperimentalComposeUiHostApi
    public fun collectAccessibilityChildren(outChildren: MutableList<SemanticsNode>) {
        forEachAccessibilityChild { child -> outChildren.add(child) }
    }

    internal val replacedChildren: List<SemanticsNode>
        get() = getChildren(includeReplacedSemantics = false, includeFakeNodes = true)

    /**
     * @param includeReplacedSemantics if true, the result will contain children of nodes marked as
     *   [clearAndSetSemantics]. For accessibility we always use false, but in testing and debugging
     *   we should be able to investigate both
     * @param includeFakeNodes if true, the tree will include fake nodes. For accessibility we set
     *   to true, but for testing purposes we don't want to expose the fake nodes and therefore set
     *   to false. When Talkback can properly handle unmerged tree, fake nodes will be removed and
     *   so will be this parameter.
     * @param includeDeactivatedNodes set to true if you want to collect the nodes which are
     *   deactivated. For example, the children of [androidx.compose.ui.layout.SubcomposeLayout]
     *   which are retained to be reused in future are considered deactivated.
     * @param outChildren The mutable list into which children are appended.
     */
    @OptIn(ExperimentalComposeUiHostApi::class)
    internal fun getChildren(
        includeReplacedSemantics: Boolean = !mergingEnabled,
        includeFakeNodes: Boolean = false,
        includeDeactivatedNodes: Boolean = false,
        outChildren: MutableList<SemanticsNode> = mutableListOf(),
    ): List<SemanticsNode> {
        if (!includeReplacedSemantics && _unmergedConfig.isClearingSemantics) {
            return outChildren
        }

        if (isMergingSemanticsOfDescendants) {
            // In most common merging scenarios like Buttons, this will return nothing.
            // In cases like a clickable Row itself containing a Button, this will
            // return the Button as a child.
            val unmergedChildren = mutableListOf<SemanticsNode>()
            return findOneLayerOfMergingSemanticsNodes(unmergedChildren, outChildren)
        }

        return unmergedChildren(outChildren, includeFakeNodes, includeDeactivatedNodes)
    }

    /** Whether this SemanticNode is the root of a tree or not */
    public val isRoot: Boolean
        get() = parent == null

    /** The parent of this node in the tree. */
    public val parent: SemanticsNode?
        get() {
            if (fakeNodeParent != null) return fakeNodeParent
            val node = findParentLayoutNode(this.layoutNode) ?: return null
            return SemanticsNode(node, mergingEnabled)
        }

    /**
     * Visits the semantics ancestors of this node, nearest first, without creating a
     * [SemanticsNode] per ancestor.
     *
     * An ancestor is any enclosing layout that has semantics, whether or not it merges. For a
     * synthetic accessibility node (see [collectAccessibilityChildren]) the node that owns it is
     * visited first, followed by that node's ancestors.
     *
     * @param block Called with each ancestor's [LayoutInfo] and unmerged configuration. Return
     *   `true` to stop at that ancestor.
     * @return the ancestor for which [block] returned `true`, or `null` if it never did.
     */
    @ExperimentalComposeUiHostApi
    public fun forEachSemanticsAncestor(
        block: (layoutInfo: LayoutInfo, config: SemanticsPropertyProvider) -> Boolean
    ): LayoutInfo? {
        var current = layoutNode
        val owner = fakeNodeParent
        if (owner != null) {
            if (block(owner.layoutNode, owner._unmergedConfig)) return owner.layoutNode
            current = owner.layoutNode
        }
        var parent = current.parent
        while (parent != null) {
            val config = parent.semanticsConfiguration
            if (config != null && block(parent, config)) return parent
            parent = parent.parent
        }
        return null
    }

    /**
     * Visits the same children as [collectAccessibilityChildren], in the same order, without
     * building a list.
     *
     * @param block Called with each child.
     */
    @ExperimentalComposeUiHostApi
    public fun forEachAccessibilityChild(block: (child: SemanticsNode) -> Unit) {
        val config = _unmergedConfig
        if (config.isClearingSemantics) return
        if (isMergingSemanticsOfDescendants) {
            layoutNode.forEachMergingLayerChild(block)
            return
        }
        if (isFake) return
        val isMerging = config.isMergingSemanticsOfDescendants
        if (isMerging && layoutNode.hasSemanticsChild()) {
            val contentDescription =
                config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
            if (contentDescription != null) {
                val fakeNode =
                    fakeSemanticsNode(null) { this.contentDescription = contentDescription }
                block(fakeNode)
            }
        }
        val count = layoutNode.visitUnmergedAccessibilityChildren(block)
        val nodeRole = role
        if (nodeRole != null && isMerging && count > 0) {
            block(fakeSemanticsNode(nodeRole) { this.role = nodeRole })
        }
    }

    /**
     * Visits the semantics children of this layout (skipping layouts without semantics) that are
     * attached and active, in [LayoutNode.zSortedChildren] order. Skips link test markers.
     *
     * @return the number of children found, including skipped link markers
     */
    private fun LayoutNode.visitUnmergedAccessibilityChildren(block: (SemanticsNode) -> Unit): Int {
        var count = 0
        zSortedChildren.forEach { child ->
            if (child.isAttached && !child.isDeactivated) {
                if (child.nodes.has(Nodes.Semantics)) {
                    count++
                    val isLinkMarker =
                        child.semanticsConfiguration?.contains(
                            SemanticsProperties.LinkTestMarker
                        ) == true
                    if (!isLinkMarker) block(SemanticsNode(child, mergingEnabled))
                } else {
                    count += child.visitUnmergedAccessibilityChildren(block)
                }
            }
        }
        return count
    }

    /**
     * Returns `true` if [forEachAccessibilityChild] visits at least one child. Stops at the first
     * child, without building a list or creating [SemanticsNode]s.
     */
    @ExperimentalComposeUiHostApi
    public fun hasAccessibilityChild(): Boolean {
        val config = _unmergedConfig
        if (config.isClearingSemantics) return false
        if (isMergingSemanticsOfDescendants) return layoutNode.hasMergingLayerChild()
        if (isFake) return false
        val isMerging = config.isMergingSemanticsOfDescendants
        if (
            isMerging &&
                layoutNode.hasSemanticsChild() &&
                config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() != null
        ) {
            return true
        }
        // A merging node with a role gets a synthetic role child when it has any semantics child,
        // including a link marker.
        return layoutNode.hasUnmergedAccessibilityChild(
            countLinkMarkers = isMerging && role != null
        )
    }

    /**
     * Returns `true` if [visitUnmergedAccessibilityChildren] finds a child that it visits, or any
     * child at all, including link markers, when [countLinkMarkers] is `true`.
     */
    private fun LayoutNode.hasUnmergedAccessibilityChild(countLinkMarkers: Boolean): Boolean {
        zSortedChildren.forEach { child ->
            if (child.isAttached && !child.isDeactivated) {
                if (child.nodes.has(Nodes.Semantics)) {
                    if (countLinkMarkers) return true
                    val isLinkMarker =
                        child.semanticsConfiguration?.contains(
                            SemanticsProperties.LinkTestMarker
                        ) == true
                    if (!isLinkMarker) return true
                } else if (child.hasUnmergedAccessibilityChild(countLinkMarkers)) {
                    return true
                }
            }
        }
        return false
    }

    /** Returns `true` if [forEachMergingLayerChild] visits at least one child. */
    private fun LayoutNode.hasMergingLayerChild(): Boolean {
        zSortedChildren.forEach { child ->
            if (child.isAttached && !child.isDeactivated) {
                val config =
                    if (child.nodes.has(Nodes.Semantics)) child.semanticsConfiguration else null
                if (config == null) {
                    if (child.hasMergingLayerChild()) return true
                } else if (config.isMergingSemanticsOfDescendants) {
                    if (!config.contains(SemanticsProperties.LinkTestMarker)) return true
                } else if (!config.isClearingSemantics) {
                    if (child.hasMergingLayerChild()) return true
                }
            }
        }
        return false
    }

    private fun LayoutNode.forEachMergingLayerChild(block: (SemanticsNode) -> Unit) {
        zSortedChildren.forEach { child ->
            if (child.isAttached && !child.isDeactivated) {
                val config =
                    if (child.nodes.has(Nodes.Semantics)) child.semanticsConfiguration else null
                if (config == null) {
                    child.forEachMergingLayerChild(block)
                } else if (config.isMergingSemanticsOfDescendants) {
                    if (!config.contains(SemanticsProperties.LinkTestMarker)) {
                        block(SemanticsNode(child, mergingEnabled))
                    }
                } else if (!config.isClearingSemantics) {
                    child.forEachMergingLayerChild(block)
                }
            }
        }
    }

    private fun LayoutNode.hasSemanticsChild(): Boolean {
        zSortedChildren.forEach { child ->
            if (child.isAttached && !child.isDeactivated) {
                if (child.nodes.has(Nodes.Semantics) || child.hasSemanticsChild()) return true
            }
        }
        return false
    }

    @OptIn(ExperimentalComposeUiHostApi::class)
    private fun findParentLayoutNode(layoutNode: LayoutNode): LayoutNode? {
        var node: LayoutNode? = null
        if (mergingEnabled) {
            node = layoutNode.findClosestParentNode {
                it.semanticsConfiguration?.isMergingSemanticsOfDescendants == true
            }
        }

        if (node == null) {
            node = layoutNode.findClosestParentNode { it.nodes.has(Nodes.Semantics) }
        }

        return node
    }

    @OptIn(ExperimentalComposeUiHostApi::class)
    private fun findOneLayerOfMergingSemanticsNodes(
        unmergedChildren: MutableList<SemanticsNode>,
        list: MutableList<SemanticsNode> = mutableListOf(),
    ): List<SemanticsNode> {
        unmergedChildren.forEachUnmergedChild { child ->
            if (child.isMergingSemanticsOfDescendants) {
                list.add(child)
            } else {
                if (!child._unmergedConfig.isClearingSemantics) {
                    child.findOneLayerOfMergingSemanticsNodes(unmergedChildren, list)
                }
            }
        }
        return list
    }

    @Suppress("BanInlineOptIn")
    @OptIn(ExperimentalContracts::class)
    private inline fun MutableList<SemanticsNode>.forEachUnmergedChild(
        block: (SemanticsNode) -> Unit
    ) {
        contract { callsInPlace(block) }

        // This allows block() to invoke unmergedChildren() recursively
        val start = size
        unmergedChildren(this)
        val end = size
        for (i in start until end) {
            block(this[i])
        }
    }

    /**
     * If the node is merging the descendants, we'll use the outermost semantics modifier that has
     * mergeDescendants == true to report the bounds, size and position of the node. For majority of
     * use cases it means that accessibility bounds will be equal to the clickable area. Otherwise
     * the outermost semantics will be used to report bounds, size and position. In both cases, only
     * nodes that have isImportantForBounds == true are taken into account.
     */
    internal fun findCoordinatorToGetBounds(): NodeCoordinator? {
        if (isFake) return parent?.findCoordinatorToGetBounds()
        return findSemanticsModifierNodeToGetBounds()?.requireCoordinator(Nodes.Semantics)
            ?: layoutNode.innerCoordinator
    }

    /**
     * Look for an outermost [SemanticsModifierNode] that has isImportantForBounds == true, while
     * prioritizing nodes with shouldMergeDescendantSemantics == true. If no such node found (i.e.,
     * there are no nodes with isImportantForBounds == true), this method returns null.
     */
    private fun findSemanticsModifierNodeToGetBounds(): SemanticsModifierNode? {
        var nodeForBounds: SemanticsModifierNode? = null
        if (_unmergedConfig.isMergingSemanticsOfDescendants) {
            layoutNode.nodes.headToTail(Nodes.Semantics) {
                if (it.isImportantForBounds) {
                    if (it.shouldMergeDescendantSemantics) return it
                    if (nodeForBounds == null) nodeForBounds = it
                }
            }
        } else {
            nodeForBounds =
                layoutNode.nodes.firstFromHead(Nodes.Semantics) { it.isImportantForBounds }
        }
        return nodeForBounds
    }

    // Fake nodes
    private fun emitFakeNodes(unmergedChildren: MutableList<SemanticsNode>) {
        val nodeRole = this.role
        if (
            nodeRole != null &&
                _unmergedConfig.isMergingSemanticsOfDescendants &&
                unmergedChildren.isNotEmpty()
        ) {
            val fakeNode = fakeSemanticsNode(nodeRole) { this.role = nodeRole }
            unmergedChildren.add(fakeNode)
        }

        // Fake node for contentDescription clobbering issue
        if (
            _unmergedConfig.contains(SemanticsProperties.ContentDescription) &&
                unmergedChildren.isNotEmpty() &&
                _unmergedConfig.isMergingSemanticsOfDescendants
        ) {
            val contentDescription =
                this._unmergedConfig
                    .getOrNull(SemanticsProperties.ContentDescription)
                    ?.firstOrNull()
            if (contentDescription != null) {
                val fakeNode =
                    fakeSemanticsNode(null) { this.contentDescription = contentDescription }
                unmergedChildren.add(0, fakeNode)
            }
        }
    }

    private fun fakeSemanticsNode(
        role: Role?,
        properties: SemanticsPropertyReceiver.() -> Unit,
    ): SemanticsNode {
        val configuration =
            SemanticsConfiguration().also {
                it.isMergingSemanticsOfDescendants = false
                it.isClearingSemantics = false
                it.properties()
            }
        val fakeNode =
            SemanticsNode(
                outerSemanticsNode =
                    object : SemanticsModifierNode, Modifier.Node() {
                        override fun SemanticsPropertyReceiver.applySemantics() {
                            properties()
                        }
                    },
                mergingEnabled = false,
                layoutNode =
                    LayoutNode(
                        isVirtual = true,
                        semanticsId =
                            if (role != null) roleFakeNodeId() else contentDescriptionFakeNodeId(),
                    ),
                unmergedConfig = configuration,
            )
        fakeNode.fakeNodeParent = this
        return fakeNode
    }

    internal fun copyWithMergingEnabled(): SemanticsNode {
        return SemanticsNode(outerSemanticsNode, true, layoutNode, _unmergedConfig)
    }
}

/**
 * Executes [selector] on every parent of this [LayoutNode] and returns the closest [LayoutNode] to
 * return `true` from [selector] or null if [selector] returns false for all ancestors.
 */
internal inline fun LayoutNode.findClosestParentNode(
    selector: (LayoutNode) -> Boolean
): LayoutNode? {
    var currentParent = this.parent
    while (currentParent != null) {
        if (selector(currentParent)) {
            return currentParent
        } else {
            currentParent = currentParent.parent
        }
    }

    return null
}

internal const val RoleFakeNodeIdOffset = 1_000_000_000
internal const val ContentDescriptionFakeNodeIdOffset = 2_000_000_000

private val SemanticsNode.role
    get() = this._unmergedConfig.getOrNull(SemanticsProperties.Role)

private fun SemanticsNode.contentDescriptionFakeNodeId() =
    this.id + ContentDescriptionFakeNodeIdOffset

private fun SemanticsNode.roleFakeNodeId() = this.id + RoleFakeNodeIdOffset

/**
 * Visits the unmerged properties of the descendants that [SemanticsNode.mergedConfig] merges into
 * this layout's configuration, in merge order. Mirrors [SemanticsNode.config]'s merge walk.
 */
private fun LayoutNode.forEachMergedDescendantProperty(
    action: (SemanticsPropertyKey<*>, Any?) -> Unit
) {
    zSortedChildren.forEach { child ->
        if (child.isAttached && !child.isDeactivated) {
            val config =
                if (child.nodes.has(Nodes.Semantics)) child.semanticsConfiguration else null
            if (config == null) {
                child.forEachMergedDescendantProperty(action)
            } else if (!config.isMergingSemanticsOfDescendants) {
                config.props.forEach(action)
                if (!config.isClearingSemantics) child.forEachMergedDescendantProperty(action)
            }
        }
    }
}

/** Like [forEachMergedDescendantProperty], but stops when [block] returns `true`. */
private fun LayoutNode.anyMergedDescendantProperty(
    block: (SemanticsPropertyKey<*>, Any?) -> Boolean
): Boolean {
    zSortedChildren.forEach { child ->
        if (child.isAttached && !child.isDeactivated) {
            val config =
                if (child.nodes.has(Nodes.Semantics)) child.semanticsConfiguration else null
            if (config == null) {
                if (child.anyMergedDescendantProperty(block)) return true
            } else if (!config.isMergingSemanticsOfDescendants) {
                if (config.props.any(block)) return true
                if (!config.isClearingSemantics && child.anyMergedDescendantProperty(block)) {
                    return true
                }
            }
        }
    }
    return false
}

/** Merges [key] across the descendants that [SemanticsNode.mergedConfig] merges, in merge order. */
@Suppress("UNCHECKED_CAST")
private fun <T> LayoutNode.mergeDescendantValue(key: SemanticsPropertyKey<T>, initial: T?): T? {
    var value = initial
    zSortedChildren.forEach { child ->
        if (child.isAttached && !child.isDeactivated) {
            val config =
                if (child.nodes.has(Nodes.Semantics)) child.semanticsConfiguration else null
            if (config == null) {
                value = child.mergeDescendantValue(key, value)
            } else if (!config.isMergingSemanticsOfDescendants) {
                val props = config.props
                if (props.containsKey(key)) {
                    val merged = key.merge(value, props[key] as T)
                    if (merged != null) value = merged
                }
                if (!config.isClearingSemantics) {
                    value = child.mergeDescendantValue(key, value)
                }
            }
        }
    }
    return value
}

/**
 * The unmerged semantics configuration of this layout, or `null` if it has no semantics modifiers.
 *
 * Reads existing state; doesn't allocate.
 */
@ExperimentalComposeUiHostApi
public val LayoutInfo.semanticsConfiguration: SemanticsPropertyProvider?
    get() = (this as? LayoutNode)?.semanticsConfiguration
