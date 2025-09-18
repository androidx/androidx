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
import androidx.compose.ui.R
import androidx.compose.ui.inspection.util.AnchorMap
import androidx.compose.ui.inspection.util.isPrimitiveClass
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.tooling.data.ContextCache
import androidx.compose.ui.tooling.data.ParameterInformation
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.findParameters
import androidx.compose.ui.tooling.data.makeTree
import androidx.compose.ui.unit.Density
import java.util.ArrayDeque

/** Generator of a tree for the Layout Inspector. */
@OptIn(UiToolingDataApi::class)
class LayoutInspectorTree(anchorMap: AnchorMap) {
    private val builderData = SharedBuilderDataImpl(anchorMap)
    private val resultByComposition = mutableMapOf<CompositionInstance, SubCompositionResult>()
    private val builder = CompositionBuilder(builderData, resultByComposition)

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
        val data = mutableSetOf<CompositionData>()
        views.forEach { view ->
            data.addAll(view.compositionRoots)
            collectSemantics(view)
        }
        val defaultView = views.first()
        builderData.setDensity(defaultView)
        data.makeTree(
            builder::prepareResult,
            builder::convert,
            builder::createResult,
            builderData.contextCache,
        )

        val defaultViewId = defaultView.uniqueDrawingId
        val result = mutableLongObjectMapOf<MutableList<InspectorNode>>()
        resultByComposition.values.forEach {
            val viewId = it.ownerView?.uniqueDrawingId ?: defaultViewId
            result.getOrPut(viewId) { mutableListOf() }.addAll(it.nodes)
        }
        clear()
        return result
    }

    private val View.compositionRoots: Set<CompositionData>
        get() {
            @Suppress("UNCHECKED_CAST")
            return getTag(R.id.inspection_slot_table_set) as? Set<CompositionData> ?: emptySet()
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
        resultByComposition.clear()
    }

    private class SharedBuilderDataImpl(override val anchorMap: AnchorMap) : SharedBuilderData {
        override val cache = ArrayDeque<MutableInspectorNode>()
        override val contextCache = ContextCache()
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
