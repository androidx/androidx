/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.tooling.data

import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.runtime.tooling.CompositionInstance
import androidx.compose.runtime.tooling.findCompositionInstance

/**
 * Processes a set of [CompositionData] instances and constructs a list of trees of custom nodes.
 *
 * This function builds a hierarchy from the provided compositions and then uses the `mapTree` to
 * transform each root composition into a custom tree structure defined by the [createNode]. Results
 * from `mapTree` that are `null` will be excluded from the final list.
 *
 * The processing involves:
 * 1. Building a lookup map from [CompositionInstance] to [CompositionData].
 * 2. Constructing a parent-to-children hierarchy of [CompositionInstance]s.
 * 3. Recursively traversing the hierarchy to map each composition using the provided [createNode],
 *    potentially stitching children processed from sub-compositions.
 *
 * @param prepareResult Give the caller a chance to setup data structures for a composition.
 * @param createNode A function that takes a [CompositionGroup], its [SourceContext], a list of
 *   already processed children of type [T] from the current composition, and an optional list of
 *   children of type [R] from stitched sub-compositions. It returns a custom node of type [R] or
 *   `null`.
 * @param createResult Create a result [R] from the top node [T] for this composition and the
 *   specified child compositions.
 * @param cache An optional [ContextCache] to optimize [SourceContext] creation.
 * @return A list of root nodes of type [R] representing the successfully processed trees.
 * @receiver A set of [CompositionData] to be processed into trees.
 */
@UiToolingDataApi
@OptIn(UiToolingDataApi::class)
fun <T, R> Set<CompositionData>.makeTree(
    prepareResult: (CompositionInstance) -> Unit,
    createNode: (CompositionGroup, SourceContext, List<T>, List<R>) -> T?,
    createResult: (CompositionInstance, T?, List<CompositionInstance>) -> R?,
    cache: ContextCache = ContextCache(),
): List<R> = CompositionDataTree(this, prepareResult, createNode, createResult, cache).build()

/**
 * Processes a set of [CompositionData] instances and constructs a list of trees of custom nodes.
 *
 * This function builds a hierarchy from the provided compositions and then uses the `mapTree` to
 * transform each root composition into a custom tree structure defined by the [factory]. Results
 * from `mapTree` that are `null` will be excluded from the final list.
 *
 * The processing involves:
 * 1. Building a lookup map from [CompositionInstance] to [CompositionData].
 * 2. Constructing a parent-to-children hierarchy of [CompositionInstance]s.
 * 3. Recursively traversing the hierarchy to map each composition using the provided [factory],
 *    potentially stitching children processed from sub-compositions.
 *
 * @param factory A function that takes a [CompositionGroup], its [SourceContext], a list of already
 *   processed children of type [T] from the current composition, and an optional list of children
 *   of type [T] from stitched sub-compositions. It returns a custom node of type [T] or `null`.
 * @param cache An optional [ContextCache] to optimize [SourceContext] creation.
 * @return A list of root nodes of type [T] representing the successfully processed trees.
 * @receiver A set of [CompositionData] to be processed into trees.
 */
@UiToolingDataApi
@OptIn(UiToolingDataApi::class)
fun <T> Set<CompositionData>.makeTree(
    factory: (CompositionGroup, SourceContext, List<T>, List<T>) -> T?,
    cache: ContextCache = ContextCache(),
): List<T> = makeTree({}, factory, { _, out, _ -> out }, cache)

@OptIn(UiToolingDataApi::class)
private class CompositionDataTree<T, R>(
    private val compositions: Set<CompositionData>,
    private val prepareResult: (CompositionInstance) -> Unit,
    private val createNode: (CompositionGroup, SourceContext, List<T>, List<R>) -> T?,
    private val createResult: (CompositionInstance, T?, List<CompositionInstance>) -> R?,
    private val cache: ContextCache,
) {
    private val hierarchy = mutableMapOf<CompositionInstance, MutableList<CompositionInstance>>()
    private val processedNodes = mutableMapOf<CompositionInstance, R?>()
    private val rootCompositionInstances = mutableSetOf<CompositionInstance>()

    init {
        for (compositionData in compositions) {
            compositionData.findCompositionInstance()?.let { compositionInstance ->
                buildCompositionParentHierarchy(compositionInstance)
            }
        }
    }

    fun build(): List<R> {
        return rootCompositionInstances.mapNotNull { rootInstance -> mapTree(rootInstance) }
    }

    @OptIn(UiToolingDataApi::class)
    private fun mapTree(instance: CompositionInstance): R? {
        // Check if already processed to handle shared nodes and cycles
        if (processedNodes.containsKey(instance)) {
            return processedNodes[instance]
        }
        val compositionData = instance.data

        // Recursively process children and collect their results
        val children = hierarchy[instance] ?: emptyList()

        for (childInstance in children) {
            mapTree(childInstance)
        }

        val childrenToAdd = mutableMapOf<Any?, MutableList<R>>()
        children
            .filter { it in processedNodes }
            .groupByTo(
                childrenToAdd,
                { it.findContextGroup()!!.identity },
                { processedNodes[it]!! },
            )

        // Now, map the current tree, stitching the children's results.
        // The `mapTreeWithStitching` function is an assumed extension that handles the actual
        // mapping.
        prepareResult(instance)
        val node = compositionData.mapTreeWithStitching(createNode, cache, childrenToAdd)
        val result = createResult(instance, node, children)

        // Memoize the result
        processedNodes[instance] = result
        return result
    }

    /**
     * Traverses up the composition tree from a given [CompositionInstance] to build its parent
     * hierarchy and identify the ultimate root of its specific tree.
     *
     * Starting from the given [instance], this function iteratively moves to its parent:
     * - It adds the current [instance] to its parent's list of children in the [hierarchy] map.
     * - If this parent-child relationship is already recorded, it stops to avoid redundant
     *   processing.
     * - This continues until an instance with no parent is reached (i.e., `parentComposition` is
     *   `null`).
     * - The instance that has no parent is considered an ultimate root and is added to
     *   [rootCompositionInstances].
     *
     * @param instance The [CompositionInstance] from which to start building the parent hierarchy.
     */
    private fun buildCompositionParentHierarchy(instance: CompositionInstance) {
        var currentComposition = instance
        var parentComposition = currentComposition.parent
        while (parentComposition != null) {
            val children = hierarchy.getOrPut(parentComposition) { mutableListOf() }
            if (children.contains(currentComposition)) {
                // This path (parent-child link) has already been processed.
                return
            }
            children.add(currentComposition)
            currentComposition = parentComposition
            parentComposition = currentComposition.parent
        }
        // After the loop, currentComposition is the instance that has no parent.
        rootCompositionInstances.add(currentComposition)
    }
}
