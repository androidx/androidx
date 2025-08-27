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

@file:OptIn(UiToolingDataApi::class)

package androidx.compose.ui.inspection.inspector

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableIntObjectMap
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.runtime.tooling.CompositionInstance
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.inspection.util.AnchorMap
import androidx.compose.ui.inspection.util.NO_ANCHOR_ID
import androidx.compose.ui.inspection.util.isPrimitiveClass
import androidx.compose.ui.layout.GraphicLayerInfo
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.view
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.InteroperableComposeUiNode
import androidx.compose.ui.tooling.data.ContextCache
import androidx.compose.ui.tooling.data.ParameterInformation
import androidx.compose.ui.tooling.data.SourceContext
import androidx.compose.ui.tooling.data.SourceLocation
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.toSize
import java.lang.reflect.ParameterizedType
import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The [InspectorNode.id] will be populated with:
 * - the layerId from a LayoutNode if this exists
 * - an id generated from an Anchor instance from the SlotTree if this exists
 * - a generated id if none of the above ids are available
 *
 * The interval -10000..-2 is reserved for the generated ids.
 */
@VisibleForTesting const val RESERVED_FOR_GENERATED_IDS = -10000L
private const val LAZY_ITEM = "LazyLayoutPinnableItem"

private val unwantedCalls =
    setOf(
        "CompositionLocalProvider",
        "Content",
        "Inspectable",
        "ProvideAndroidCompositionLocals",
        "ProvideCommonCompositionLocals",
        "ProvideCompositionLocals",
    )

private val knownCompositionHolders = setOf("LayoutSpatialElevation", "SpatialElevation")

/** Builder of [InspectorNode] trees from [root] compositions. */
@OptIn(UiToolingDataApi::class)
internal class CompositionBuilder(
    private val info: SharedBuilderData,
    private val resultByComposition: MutableMap<CompositionInstance, SubCompositionResult>,
) : SharedBuilderData by info {
    private var ownerView: View? = null
    private val capturingSubComposition = mutableMapOf<MutableInspectorNode, SubCompositionResult>()
    private var listIndex = -1

    private fun clear() {
        ownerView = null
        capturingSubComposition.clear()
        listIndex = -1
    }

    /**
     * This function is called from [CompositionData.mapTree] before any calls to [convert] for this
     * [composition].
     */
    fun prepareResult(composition: CompositionInstance) {
        ownerView = findOwnerView(composition.data)
        capturingSubComposition.clear()
        listIndex = -1
    }

    /** This function is called recursively from [CompositionData.mapTree] in Post-Order (LRN). */
    fun convert(
        group: CompositionGroup,
        context: SourceContext,
        children: List<MutableInspectorNode>,
        childrenFromSubCompositions: List<SubCompositionResult>,
    ): MutableInspectorNode {
        val parent = parse(group, context, children)
        addToParent(parent, children)
        addSubCompositions(parent, sort(childrenFromSubCompositions))
        return parent
    }

    /**
     * This function is called from [CompositionData.mapTree] after all calls to [convert] for this
     * [composition].
     */
    fun createResult(
        composition: CompositionInstance,
        resultNode: MutableInspectorNode?,
        childCompositions: List<CompositionInstance>,
    ): SubCompositionResult {
        val node = resultNode ?: newNode()
        updateSubCompositionsAtEnd(node)
        val singleSubComposition = childCompositions.singleOrNull()
        var result = SubCompositionResult(ownerView, node.children.toList(), listIndex)
        if (node.children.isEmpty() && ownerView == null && singleSubComposition != null) {
            // Special case:
            // Everything from this unowned composition was pushed to its single sub-composition.
            // Remove the result of the sub-composition and use that result for this composition.
            resultByComposition.remove(singleSubComposition)?.let {
                result = SubCompositionResult(it.ownerView, it.nodes, listIndex)
            }
        }
        resultByComposition[composition] = result
        clear()
        return result
    }

    /**
     * We do NOT know which [View] a composition belongs to. Perform an initial tree walk to find
     * the composition owner.
     */
    private fun findOwnerView(root: CompositionData): View? {
        root.compositionGroups.forEach { group ->
            (group.node as? LayoutInfo)?.view?.let {
                return it
            }
            findOwnerView(group)?.let {
                return it
            }
        }
        return null
    }

    /**
     * Adds the nodes in [input] to the children of [parentNode]. Nodes without a reference to a
     * wanted Composable are skipped. A single skipped render id will be added to [parentNode].
     */
    private fun addToParent(parentNode: MutableInspectorNode, input: List<MutableInspectorNode>) {
        // If we're adding an unwanted node from the `input` to the parent node and it has a
        // View ID, then assign it to the parent view so that we don't lose the context that we
        // found a View as a descendant of the parent node. Most likely, there were one or more
        // unwanted intermediate nodes between the node that actually owns the Android View
        // and the desired node that the View should be associated with in the inspector. If
        // there's more than one input node with a View ID, we skip this step since it's
        // unclear how these views would be related.
        input
            .singleOrNull { it.hostingAndroidView }
            ?.takeIf { node -> node.isUnwanted }
            ?.let { nodeHostingAndroidView -> parentNode.viewId = nodeHostingAndroidView.viewId }

        var id: Long? = null
        val isUnwantedDrawComposable = isUnwantedDrawComposable(parentNode)
        input.forEach { node ->
            if (node.isUnwanted) {
                parentNode.hasDrawModifier =
                    !isUnwantedDrawComposable &&
                        (parentNode.hasDrawModifier or node.hasDrawModifier)
                parentNode.hasChildDrawModifier =
                    !isUnwantedDrawComposable &&
                        (parentNode.hasChildDrawModifier or node.hasChildDrawModifier)
                parentNode.children.addAll(node.children)
                if (node.hasLayerId) {
                    // If multiple siblings with a render ids are dropped:
                    // Ignore them all. And delegate the drawing to a parent in the inspector.
                    id = if (id == null) node.id else UNDEFINED_ID
                }
            } else {
                if (
                    (node.hasDrawModifier || node.hasChildDrawModifier) &&
                        systemPackages.contains(node.packageHash)
                ) {
                    if (isUnwantedDrawComposable) {
                        updateTree(node) {
                            it.hasDrawModifier = false
                            it.hasChildDrawModifier = false
                        }
                    } else {
                        parentNode.hasChildDrawModifier = true
                        node.hasChildDrawModifier = false
                    }
                }
                node.id = if (node.hasAssignedId) node.id else --generatedId
                val withSemantics = node.packageHash !in systemPackages
                val resultNode = node.build(withSemantics)
                parentNode.children.add(resultNode)
                if (withSemantics) {
                    node.mergedSemantics.clear()
                    node.unmergedSemantics.clear()
                }
            }
            if (node.bounds != null && parentNode.box == node.box) {
                parentNode.bounds = node.bounds
            }
            parentNode.mergedSemantics.addAll(node.mergedSemantics)
            parentNode.unmergedSemantics.addAll(node.unmergedSemantics)
            release(node)
        }
        val nodeId = id
        parentNode.id = if (!parentNode.hasLayerId && nodeId != null) nodeId else parentNode.id
    }

    private fun addSubCompositions(
        parent: MutableInspectorNode,
        childrenFromSubCompositions: List<SubCompositionResult>,
    ) {
        childrenFromSubCompositions.forEach { subComposition ->
            if (subComposition.ownerView == ownerView || subComposition.ownerView == null) {
                // Steal all the nodes from the sub-compositions and add them to the parent.
                // Propagate the size to the parent node if the parent has no size.
                parent.children.addAll(subComposition.nodes)
                if (parent.box == emptyBox) {
                    subComposition.nodes.forEach { parent.box = parent.box.union(it.box) }
                    parent.boxSizeOverridden = true
                }
                subComposition.nodes = emptyList()
            } else {
                // If the sub-composition belongs to a different view prepare to copy the
                // parent node to the sub-composition. See: checkCapturingSubCompositions.
                capturingSubComposition[parent] = subComposition
            }
        }
    }

    private fun sort(compositions: List<SubCompositionResult>): List<SubCompositionResult> {
        val anyIndices = compositions.any { it.listIndex >= 0 }
        return if (anyIndices) compositions.sortedBy { it.listIndex } else compositions
    }

    @OptIn(InternalComposeUiApi::class)
    private fun parse(
        group: CompositionGroup,
        context: SourceContext,
        children: List<MutableInspectorNode>,
    ): MutableInspectorNode {
        val node = newNode()
        node.name = context.name ?: ""
        node.key = group.key as? Int ?: 0
        node.inlined = context.isInline
        node.box = context.bounds.emptyCheck()
        if (node.box == emptyBox && children.any { it.boxSizeOverridden }) {
            // If this node has no size and a child box size comes from a sub-composition propagate
            // the size to the parent node.
            children.forEach { node.box = node.box.union(it.box) }
            node.boxSizeOverridden = true
        }
        if (node.name == LAZY_ITEM) {
            listIndex = getListIndexOfLazyItem(context)
        }

        // If this node is associated with an android View, set the node's viewId to point to
        // the hosted view. We use the parent's uniqueDrawingId since the interopView returned here
        // will be the view itself, but we want to use the `AndroidViewHolder` that hosts the view
        // instead of the view directly.
        (group.node as? InteroperableComposeUiNode?)?.getInteropView()?.let { interopView ->
            (interopView.parent as? View)?.uniqueDrawingId?.let { viewId -> node.viewId = viewId }
        }

        checkCapturingSubCompositions(node, children)

        val layoutInfo = group.node as? LayoutInfo
        if (layoutInfo != null) {
            return parseLayoutInfo(layoutInfo, context, node)
        }

        // If any of the children has an unknown location, we need to:
        // - change the calculated size to the children with known location
        // - or mark this node as an unknown location and unwanted if the size
        //   originates from children with unknown locations.
        if (children.any { it.unknownLocation } && !node.box.isEmpty) {
            var box = emptyBox
            children.filter { !it.unknownLocation }.forEach { child -> box = box.union(child.box) }
            if (box.isEmpty) {
                node.unknownLocation = true
                node.markUnwanted()
            } else {
                node.box = box
            }
        }

        // Keep an empty node if we are capturing nodes into sub-compositions.
        // Mark it unwanted after copying the node to the sub-compositions.
        if (
            (node.box == emptyBox && !capturingSubComposition.contains(node)) ||
                unwantedName(node.name)
        ) {
            return node.markUnwanted()
        }
        parseCallLocation(context.location, node)
        if (isHiddenSystemNode(node)) {
            return node.markUnwanted()
        }
        node.anchorId = anchorMap[group.identity]
        node.id = syntheticId(node.anchorId)
        if (includeAllParameters) {
            addParameters(context, node)
        }
        return node
    }

    /**
     * LazyLayoutPinnableItem is used in reusable compositions and has the index as the 2nd
     * parameter.
     */
    private fun getListIndexOfLazyItem(context: SourceContext): Int {
        val parameters = context.parameters
        if (parameters.size < 2) return -1
        return (parameters[1].value as? Int) ?: -1
    }

    private fun IntRect.union(other: IntRect): IntRect {
        if (this == emptyBox) return other else if (other == emptyBox) return this

        return IntRect(
            left = min(left, other.left),
            top = min(top, other.top),
            bottom = max(bottom, other.bottom),
            right = max(right, other.right),
        )
    }

    /**
     * Check if any of the previously found sub compositions have parent nodes in this parent
     * composition that should be copied to the sub-compositions.
     *
     * An example is a dialog:
     * <pre>
     *     <code>
     *         AlertDialog(
     *             onDismissRequest = {},
     *             confirmButton = { Button({}) { Text("Confirm Button") } }
     *         )
     *     </code>
     * </pre>
     *
     * The Button & Text will be in a sub-composition and the AlertDialog will be in a parent
     * composition with an empty size. We would like to show the Button inside the AlertDialog in
     * the sub-composition. Otherwise it will look like a random Button in the component tree.
     *
     * The [capturingSubComposition] will be setup when we encounter a sub-composition that belongs
     * to a different compose View. This method will move nodes from the parent composition to the
     * sub-composition if the parent node has no size or the parent node is one of the
     * [knownCompositionHolders] that has a size but should be moved to the sub-composition.
     *
     * If a node has multiple sub-compositions among its children, stop capturing and keep the node
     * in the parent composition.
     */
    private fun checkCapturingSubCompositions(
        node: MutableInspectorNode,
        children: List<MutableInspectorNode>,
    ) {
        if (capturingSubComposition.isEmpty()) {
            return
        }
        val childrenWithSubCompositions = children.intersect(capturingSubComposition.keys)
        if (childrenWithSubCompositions.isEmpty()) {
            return
        }
        var box: IntRect = emptyBox
        var stopCapturing = false
        childrenWithSubCompositions.forEach { child ->
            val subComposition = capturingSubComposition.remove(child)!!
            val isKnownChildCompositionHolder = knownCompositionHolders.contains(child.name)
            if (!child.isUnwanted || isKnownChildCompositionHolder) {
                copyNodeToSubComposition(child, subComposition)
                if (child.box == emptyBox || isKnownChildCompositionHolder) {
                    child.markUnwanted()
                }
            }
            if (
                childrenWithSubCompositions.size == 1 &&
                    (node.box == emptyBox || knownCompositionHolders.contains(node.name))
            ) {
                // Prepare to copy the current node to the sub composition:
                capturingSubComposition[node] = subComposition
            } else {
                stopCapturing = true
                box = box.union(subComposition.ownerViewBox ?: emptyBox)
            }
        }
        if (stopCapturing && node.box == emptyBox) {
            // Propagate the box size to the parent of the sub-composition if necessary.
            node.box = box
            node.boxSizeOverridden = true
        }
    }

    private fun copyNodeToSubComposition(
        node: MutableInspectorNode,
        subComposition: SubCompositionResult,
    ) {
        val copy = newNode(node)
        copy.box = subComposition.ownerViewBox ?: emptyBox
        copy.children.addAll(subComposition.nodes)
        subComposition.nodes = listOf(copy.build())
    }

    private fun updateSubCompositionsAtEnd(node: MutableInspectorNode?) {
        val subComposition = node?.let { capturingSubComposition.remove(node) } ?: return
        if (!node.isUnwanted) {
            copyNodeToSubComposition(node, subComposition)
            if (node.box == emptyBox) {
                node.markUnwanted()
            }
        }
    }

    private fun parseLayoutInfo(
        layoutInfo: LayoutInfo,
        context: SourceContext,
        node: MutableInspectorNode,
    ): MutableInspectorNode {
        val box = context.bounds
        val size = box.size.toSize()
        val coordinates = layoutInfo.coordinates
        var bounds: QuadBounds? = null
        if (!layoutInfo.isAttached || !coordinates.isAttached || !layoutInfo.isPlaced) {
            // This could happen for extra items generated for reusable content like the
            // items in a LazyColumn. Mark these nodes unwanted i.e. filter them out.
            node.unknownLocation = true
            node.markUnwanted()
        } else {
            node.hasDrawModifier = layoutInfo.hasDrawModifier
            val topLeft = toIntOffset(coordinates.localToWindow(Offset.Zero))
            val topRight = toIntOffset(coordinates.localToWindow(Offset(size.width, 0f)))
            val bottomRight =
                toIntOffset(coordinates.localToWindow(Offset(size.width, size.height)))
            val bottomLeft = toIntOffset(coordinates.localToWindow(Offset(0f, size.height)))

            if (
                topLeft.x != box.left ||
                    topLeft.y != box.top ||
                    topRight.x != box.right ||
                    topRight.y != box.top ||
                    bottomRight.x != box.right ||
                    bottomRight.y != box.bottom ||
                    bottomLeft.x != box.left ||
                    bottomLeft.y != box.bottom
            ) {
                bounds =
                    QuadBounds(
                        topLeft.x,
                        topLeft.y,
                        topRight.x,
                        topRight.y,
                        bottomRight.x,
                        bottomRight.y,
                        bottomLeft.x,
                        bottomLeft.y,
                    )
            }
        }

        node.box = box.emptyCheck()
        node.bounds = bounds
        node.layoutNodes.add(layoutInfo)
        val modifierInfo = layoutInfo.getModifierInfo()

        val unmergedSemantics = unmergedSemanticsMap[layoutInfo.semanticsId]
        if (unmergedSemantics != null) {
            node.unmergedSemantics.addAll(unmergedSemantics)
        }

        val mergedSemantics = semanticsMap[layoutInfo.semanticsId]
        if (mergedSemantics != null) {
            node.mergedSemantics.addAll(mergedSemantics)
        }

        val layerInfo =
            modifierInfo.map { it.extra }.filterIsInstance<GraphicLayerInfo>().firstOrNull()
        node.id = layerInfo?.layerId ?: UNDEFINED_ID
        return node
    }

    /**
     * Return true if this LayoutInfo could be drawing.
     *
     * Note: Some drawing could have no effect like drawing a background with color #00000000. We
     * are not trying to detect this case at this point.
     *
     * Checking if any of the modifiers is an implementation of DrawModifier will not work since the
     * use of DrawModifier is deprecated. Instead check if an actual generic parameter implements
     * DrawModifierNode.
     *
     * Assume: the existence/absence of DrawModifierNode is an accurate determination of whether the
     * LayoutInfo is drawing.
     */
    private val LayoutInfo.hasDrawModifier: Boolean
        get() =
            getModifierInfo().any {
                var modifierClass: Class<*>? = it.modifier.javaClass
                var hasDrawModifierNode = false
                while (!hasDrawModifierNode && modifierClass != null) {
                    val genericClass = modifierClass.genericSuperclass
                    val types =
                        (genericClass as? ParameterizedType)?.actualTypeArguments ?: emptyArray()
                    hasDrawModifierNode =
                        types.filterIsInstance<Class<*>>().any {
                            DrawModifierNode::class.java.isAssignableFrom(it)
                        }
                    modifierClass = modifierClass.superclass
                }
                hasDrawModifierNode
            }

    private fun parseCallLocation(location: SourceLocation?, node: MutableInspectorNode) {
        val fileName = location?.sourceFile ?: return
        node.fileName = fileName
        node.packageHash = location.packageHash
        node.lineNumber = location.lineNumber
        node.offset = location.offset
    }

    private fun addParameters(context: SourceContext, node: MutableInspectorNode) {
        context.parameters.forEach {
            val castedValue = castValue(it)
            node.parameters.add(RawParameter(it.name, castedValue))
        }
    }

    private fun castValue(parameter: ParameterInformation): Any? {
        val value = parameter.value ?: return null
        if (parameter.inlineClass == null || !value.javaClass.isPrimitiveClass()) return value
        return inlineClassConverter.castParameterValue(parameter.inlineClass, value)
    }

    private fun isHiddenSystemNode(node: MutableInspectorNode): Boolean =
        hideSystemNodes && node.packageHash in systemPackages

    private fun unwantedName(name: String): Boolean =
        name.isEmpty() ||
            name.startsWith("remember") ||
            name.startsWith('<') && name.endsWith('>') ||
            name in unwantedCalls

    /**
     * Generate a synthetic id for the node. If we have an anchor id, use an encoded anchor id such
     * that the id will stay the same after an update.
     */
    private fun syntheticId(anchorId: Int): Long {
        if (anchorId == NO_ANCHOR_ID) {
            return UNDEFINED_ID
        }
        // The anchorId is an Int
        return anchorId.toLong() - Int.MAX_VALUE.toLong() + RESERVED_FOR_GENERATED_IDS
    }

    /**
     * Make an update to a InspectorNode tree. Only use this for smaller trees or implement a non
     * recursive implementation.
     */
    private fun updateTree(
        node: MutableInspectorNode,
        modification: (MutableInspectorNode) -> Unit,
    ) {
        val oldChildren = node.children.toList()
        node.children.clear()
        oldChildren.mapTo(node.children) { updateTree(it, modification) }
        modification(node)
    }

    private fun updateTree(
        node: InspectorNode,
        modification: (MutableInspectorNode) -> Unit,
    ): InspectorNode {
        val newNode = newNode()
        newNode.shallowCopy(node)
        node.children.mapTo(newNode.children) { updateTree(it, modification) }
        modification(newNode)
        val withSemantics = node.packageHash !in systemPackages
        return newNode.build(withSemantics)
    }

    private fun IntRect.emptyCheck(): IntRect =
        if (left >= right && top >= bottom) emptyBox else this

    private fun toIntOffset(offset: Offset): IntOffset =
        IntOffset(offset.x.roundToInt(), offset.y.roundToInt())

    private fun newNode(): MutableInspectorNode =
        if (cache.isNotEmpty()) cache.pop() else MutableInspectorNode()

    private fun newNode(copyFrom: MutableInspectorNode): MutableInspectorNode =
        newNode().shallowCopy(copyFrom)

    private fun release(node: MutableInspectorNode) {
        node.reset()
        cache.add(node)
    }
}

@OptIn(UiToolingDataApi::class)
internal interface SharedBuilderData {
    val cache: ArrayDeque<MutableInspectorNode>
    val contextCache: ContextCache
    val anchorMap: AnchorMap

    /** Map from semantics id to a list of merged semantics information */
    val semanticsMap: MutableIntObjectMap<List<RawParameter>>

    /** Map of semantics id to a list of unmerged semantics information */
    val unmergedSemanticsMap: MutableIntObjectMap<List<RawParameter>>

    val inlineClassConverter: InlineClassConverter
    val parameterFactory: ParameterFactory
    val hideSystemNodes: Boolean
    val includeAllParameters: Boolean
    var generatedId: Long
}

/** Sub-Compositions of the composition being parsed. */
internal class SubCompositionResult(
    /** The ownerView of this composition */
    val ownerView: View?,

    /** The parsed sub-composition, that may be replaced later */
    var nodes: List<InspectorNode>,

    /**
     * The index of this reusable sub-composition or -1 if this is not reusable content. Example: an
     * item in a LazyColumn.
     */
    val listIndex: Int,
) {
    /** The size of the owner view */
    val ownerViewBox = ownerView?.let { IntRect(0, 0, it.width, it.height) }
}
