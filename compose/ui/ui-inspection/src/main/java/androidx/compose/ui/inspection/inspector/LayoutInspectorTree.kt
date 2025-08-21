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

package androidx.compose.ui.inspection.inspector

import android.view.View
import androidx.collection.LongObjectMap
import androidx.collection.emptyLongObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableLongObjectMapOf
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionInstance
import androidx.compose.runtime.tooling.findCompositionInstance
import androidx.compose.ui.R
import androidx.compose.ui.inspection.util.AnchorMap
import androidx.compose.ui.inspection.util.isPrimitiveClass
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.tooling.data.ContextCache
import androidx.compose.ui.tooling.data.ParameterInformation
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.findParameters
import androidx.compose.ui.unit.Density
import java.util.ArrayDeque

/** Generator of a tree for the Layout Inspector. */
@OptIn(UiToolingDataApi::class)
class LayoutInspectorTree {
    private val builderData = SharedBuilderDataImpl()
    private val builder = CompositionBuilder(builderData)
    private val rootByComposition = mutableMapOf<CompositionInstance, CompositionData>()
    private val hierarchy = mutableMapOf<CompositionInstance, MutableList<CompositionInstance>>()
    private val resultByComposition = mutableMapOf<CompositionInstance, SubCompositionResult>()
    private val compositions = mutableSetOf<CompositionInstance>()

    /** If true, system nodes are excluded from the resulting tree */
    var hideSystemNodes: Boolean by builderData::hideSystemNodes

    /** If true, all parameters are parsed and included for each node */
    var includeAllParameters: Boolean by builderData::includeAllParameters

    /** Converts the [CompositionData] held by [views] into a list of root nodes per view id. */
    fun convert(views: List<View>): LongObjectMap<MutableList<InspectorNode>> {
        clear()
        if (views.isEmpty()) {
            return emptyLongObjectMap()
        }
        val defaultView = views.first()
        builderData.setDensity(defaultView)
        val defaultViewId = defaultView.uniqueDrawingId
        buildCompositionLookups(views)
        while (compositions.isNotEmpty()) {
            buildNodesFor(compositions.first())
        }
        val result = mutableLongObjectMapOf<MutableList<InspectorNode>>()
        resultByComposition.values.forEach {
            val viewId = it.ownerView?.uniqueDrawingId ?: defaultViewId
            result.getOrPut(viewId) { mutableListOf() }.addAll(it.nodes)
        }
        clear()
        return result
    }

    private fun buildCompositionLookups(views: List<View>) {
        for (view in views) {
            val roots = view.compositionRoots
            for (root in roots) {
                root.findCompositionInstance()?.let { composition ->
                    rootByComposition[composition] = root
                    buildCompositionHierarchy(composition)
                }
            }
            collectSemantics(view)
        }
    }

    private val View.compositionRoots: Set<CompositionData>
        get() {
            @Suppress("UNCHECKED_CAST")
            return getTag(R.id.inspection_slot_table_set) as? Set<CompositionData> ?: emptySet()
        }

    private fun buildCompositionHierarchy(root: CompositionInstance) {
        compositions.add(root)
        var composition = root
        var parent = composition.parent
        while (parent != null) {
            compositions.add(parent)
            val children = hierarchy.getOrPut(parent) { mutableListOf() }
            if (children.contains(composition)) {
                return
            }
            children.add(composition)
            composition = parent
            parent = composition.parent
        }
    }

    /** Build nodes for the specified [composition]. */
    private fun buildNodesFor(composition: CompositionInstance) {
        if (!compositions.contains(composition)) {
            // We have already built the nodes for this composition.
            return
        }
        // Mark this composition done:
        compositions.remove(composition)

        // We must build nodes for the child compositions first:
        val children = hierarchy[composition] ?: emptyList()
        children.forEach { buildNodesFor(it) }

        val root = rootByComposition[composition] ?: return
        val subCompositions = sort(children.mapNotNull { resultByComposition[it] })
        var result = builder.convert(composition, root, subCompositions)
        val singleSubComposition = children.singleOrNull()
        if (result.nodes.isEmpty() && result.ownerView == null && singleSubComposition != null) {
            // Special case:
            // Everything from this unowned composition was pushed to its single sub-composition.
            // Remove the result of the sub-composition and use that result for this composition.
            resultByComposition.remove(singleSubComposition)?.let {
                result = SubCompositionResult(composition, it.ownerView, it.nodes, result.listIndex)
            }
        }
        resultByComposition[composition] = result
    }

    fun findParameters(view: View, anchorId: Int): InspectorNode? {
        val identity = builderData.anchorMap[anchorId] ?: return null
        val roots = view.compositionRoots
        val node = MutableInspectorNode().apply { this.anchorId = anchorId }
        val group = roots.firstNotNullOfOrNull { it.find(identity) } ?: return null
        group.findParameters(builderData.contextCache).forEach {
            val castedValue = castValue(it)
            node.parameters.add(RawParameter(it.name, castedValue))
        }
        return node.build()
    }

    /**
     * Extract the merged semantics for this semantics owner such that they can be added to compose
     * nodes during the conversion of Group nodes.
     */
    private fun collectSemantics(view: View) {
        val root = view as? RootForTest ?: return
        val nodes = root.semanticsOwner.getAllSemanticsNodes(mergingEnabled = true)
        val unmergedNodes = root.semanticsOwner.getAllSemanticsNodes(mergingEnabled = false)
        val semanticsMap = builderData.semanticsMap
        val unmergedSemanticsMap = builderData.unmergedSemanticsMap
        nodes.forEach { node ->
            semanticsMap[node.id] = node.config.map { RawParameter(it.key.name, it.value) }
        }
        unmergedNodes.forEach { node ->
            unmergedSemanticsMap[node.id] = node.config.map { RawParameter(it.key.name, it.value) }
        }
    }

    /** Converts the [RawParameter]s of the [node] into displayable parameters. */
    fun convertParameters(
        rootId: Long,
        node: InspectorNode,
        kind: ParameterKind,
        maxRecursions: Int,
        maxInitialIterableSize: Int,
    ): List<NodeParameter> {
        val parameters = node.parametersByKind(kind)
        return parameters.mapIndexed { index, parameter ->
            builderData.parameterFactory.create(
                rootId,
                node.id,
                node.anchorId,
                parameter.name,
                parameter.value,
                kind,
                index,
                maxRecursions,
                maxInitialIterableSize,
            )
        }
    }

    /**
     * Converts a part of the [RawParameter] identified by [reference] into a displayable parameter.
     * If the parameter is some sort of a collection then [startIndex] and [maxElements] describes
     * the scope of the data returned.
     */
    fun expandParameter(
        rootId: Long,
        node: InspectorNode,
        reference: NodeParameterReference,
        startIndex: Int,
        maxElements: Int,
        maxRecursions: Int,
        maxInitialIterableSize: Int,
    ): NodeParameter? {
        val parameters = node.parametersByKind(reference.kind)
        if (reference.parameterIndex !in parameters.indices) {
            return null
        }
        val parameter = parameters[reference.parameterIndex]
        return builderData.parameterFactory.expand(
            rootId,
            node.id,
            node.anchorId,
            parameter.name,
            parameter.value,
            reference,
            startIndex,
            maxElements,
            maxRecursions,
            maxInitialIterableSize,
        )
    }

    private fun sort(compositions: List<SubCompositionResult>): List<SubCompositionResult> {
        val anyIndices = compositions.any { it.listIndex >= 0 }
        return if (anyIndices) compositions.sortedBy { it.listIndex } else compositions
    }

    private fun castValue(parameter: ParameterInformation): Any? {
        val value = parameter.value ?: return null
        if (parameter.inlineClass == null || !value.javaClass.isPrimitiveClass()) return value
        return builderData.inlineClassConverter.castParameterValue(parameter.inlineClass, value)
    }

    private fun clear() {
        builderData.clear()
        rootByComposition.clear()
        hierarchy.clear()
        resultByComposition.clear()
        compositions.clear()
    }

    private class SharedBuilderDataImpl : SharedBuilderData {
        override val cache = ArrayDeque<MutableInspectorNode>()
        override val contextCache = ContextCache()
        override val anchorMap = AnchorMap()
        override val semanticsMap = mutableIntObjectMapOf<List<RawParameter>>()
        override val unmergedSemanticsMap = mutableIntObjectMapOf<List<RawParameter>>()
        override val inlineClassConverter = InlineClassConverter()
        override val parameterFactory = ParameterFactory(inlineClassConverter)
        override var generatedId = -1L
        override var hideSystemNodes = true
        override var includeAllParameters = true

        fun setDensity(view: View) {
            parameterFactory.density = Density(view.context)
        }

        fun clear() {
            cache.clear()
            semanticsMap.clear()
            unmergedSemanticsMap.clear()
            inlineClassConverter.clear()
            generatedId = -1L
        }
    }
}
