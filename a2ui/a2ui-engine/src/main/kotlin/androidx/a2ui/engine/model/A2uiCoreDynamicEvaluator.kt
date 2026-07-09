/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.a2ui.engine.model

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiException

/**
 * A core layer utility that evaluates dynamic bindings and client functions. Because the
 * [androidx.a2ui.engine.platform.A2uiCoreDataModel] does not expose synchronous getters, this
 * evaluator relies on a framework-provided [A2uiCoreValueResolver] to retrieve state. This
 * guarantees that frameworks can accurately track state reads during evaluation for reactive
 * updates.
 *
 * @property catalog The catalog containing registered client-side components and functions.
 */
internal class A2uiCoreDynamicEvaluator(private val catalog: A2uiCoreCatalog) {
    /**
     * Evaluates a dynamic [payload] using [dataPath] for relative path resolution.
     *
     * This function supports evaluating complex, deeply-nested dynamic structures (including
     * [Map]s, [List]s, and primitive values). It scans for and resolves:
     * - **Path bindings**: Maps containing only a single `"path"` key, e.g., `{"path":
     *   "user/name"}`. These are resolved to actual values using [valueResolver].
     * - **Client function calls**: Maps containing a `"call"` key and optional `"args"` key, e.g.,
     *   `{"call": "add", "args": {"a": 1, "b": 2}}`. Arguments themselves can be nested dynamic
     *   payloads, which are fully resolved before the catalog function is executed.
     * - **Nested payloads**: Plain Maps and Lists, whose nested elements are recursively evaluated
     *   while maintaining their structural layout.
     *
     * @param dataPath the base data path used to resolve relative paths during evaluation
     * @param valueResolver a framework-provided class to read values from the data model
     * @param payload the dynamic payload to evaluate, which can be a [Map], [List], or a primitive
     *   literal
     * @return the fully evaluated and resolved payload
     */
    @Suppress("UNCHECKED_CAST")
    internal fun evaluate(
        dataPath: A2uiDataPath,
        valueResolver: A2uiCoreValueResolver,
        payload: Any?,
    ): Any? {
        if (payload !is Map<*, *> && payload !is List<*>) {
            return payload
        }

        val resultStack = mutableListOf<Any?>()
        val workStack = mutableListOf<Any?>()

        workStack.add(payload)

        while (workStack.isNotEmpty()) {
            when (val current = workStack.removeAt(workStack.lastIndex)) {
                is Map<*, *> ->
                    processMapNode(
                        current as Map<String, *>,
                        dataPath,
                        valueResolver,
                        workStack,
                        resultStack,
                    )
                is List<*> -> processListNode(current, workStack, resultStack)
                is Frame -> processFrameNode(current, resultStack)
                is MapFrame -> processMapFrameNode(current, resultStack)
                is ListFrame -> processListFrameNode(current, resultStack)
                else -> resultStack.add(current)
            }
        }

        return resultStack.first()
    }

    /**
     * Processes a map node which could be a path, a function call, or just a raw map.
     *
     * If it's a path node, it resolves the path using [valueResolver] and adds the result to
     * [resultStack]. If it's a call node, it schedules the function execution by pushing a [Frame]
     * and its arguments onto the [workStack]. Otherwise, it treats the map as a plain map and
     * schedules nested evaluation of its values.
     *
     * @param mapNode The map payload to process.
     * @param dataPath The base data path for relative path resolution.
     * @param valueResolver The framework-provided class to read values from the data model.
     * @param workStack The stack of pending nodes to process.
     * @param resultStack The stack of fully evaluated results.
     */
    private fun processMapNode(
        mapNode: Map<String, *>,
        dataPath: A2uiDataPath,
        valueResolver: A2uiCoreValueResolver,
        workStack: MutableList<Any?>,
        resultStack: MutableList<Any?>,
    ) {
        if (mapNode.isEmpty()) {
            resultStack.add(mapNode)
            return
        }

        if (tryProcessPathNode(mapNode, dataPath, valueResolver, resultStack)) {
            return
        }

        if (tryProcessCallNode(mapNode, workStack)) {
            return
        }

        val keys = mapNode.keys.toTypedArray()
        workStack.add(MapFrame(mapNode, keys))
        for (i in keys.indices.reversed()) {
            workStack.add(mapNode[keys[i]])
        }
    }

    /** Schedules evaluation of [listNode] elements onto [workStack] and [resultStack]. */
    private fun processListNode(
        listNode: List<*>,
        workStack: MutableList<Any?>,
        resultStack: MutableList<Any?>,
    ) {
        if (listNode.isEmpty()) {
            resultStack.add(listNode)
            return
        }

        workStack.add(ListFrame(listNode))
        for (i in listNode.indices.reversed()) {
            workStack.add(listNode[i])
        }
    }

    /** Resolves a [mapNode] to [resultStack] if it is a path node. Returns true if processed. */
    private fun tryProcessPathNode(
        mapNode: Map<*, *>,
        dataPath: A2uiDataPath,
        valueResolver: A2uiCoreValueResolver,
        resultStack: MutableList<Any?>,
    ): Boolean {
        val path = mapNode[KEY_PATH] as? String
        if (path != null && mapNode.size == 1) {
            val resolvedPath = dataPath / path
            resultStack.add(valueResolver.resolve(resolvedPath))
            return true
        }
        return false
    }

    /**
     * Schedules a [mapNode] function execution onto [workStack] if it is a call node. Returns true
     * if processed.
     */
    @Suppress("UNCHECKED_CAST")
    private fun tryProcessCallNode(mapNode: Map<String, *>, workStack: MutableList<Any?>): Boolean {
        val call = mapNode[KEY_CALL] as? String ?: return false

        for (key in mapNode.keys) {
            if (
                key != KEY_CALL &&
                    key != KEY_ARGS &&
                    key != KEY_CALLABLE_FROM &&
                    key != KEY_RETURN_TYPE
            ) {
                return false
            }
        }

        val argsMap = mapNode[KEY_ARGS] as? Map<String, *>
        if (argsMap == null && mapNode[KEY_ARGS] != null) {
            return false
        }

        if (argsMap.isNullOrEmpty()) {
            workStack.add(Frame(call, emptyArray()))
            return true
        }
        val keys = argsMap.keys.toTypedArray()
        workStack.add(Frame(call, keys))
        for (i in keys.indices.reversed()) {
            workStack.add(argsMap[keys[i]])
        }
        return true
    }

    /**
     * Executes function in [frame] using arguments from [resultStack] and pushes the result to the
     * [resultStack].
     */
    private fun processFrameNode(frame: Frame, resultStack: MutableList<Any?>) {
        val evaluatedArgs = LinkedHashMap<String, Any>(frame.keys.size)
        val startIndex = resultStack.size - frame.keys.size

        for (i in frame.keys.indices) {
            val argValue = resultStack[startIndex + i]
            if (argValue != null) {
                evaluatedArgs[frame.keys[i]] = argValue
            }
        }

        if (frame.keys.isNotEmpty()) {
            resultStack.subList(startIndex, resultStack.size).clear()
        }

        val func =
            catalog.getFunction(frame.callName)
                ?: throw A2uiException.A2uiRuntimeException(
                    "Function '${frame.callName}' not found in catalog"
                )

        resultStack.add(func.execute(evaluatedArgs))
    }

    /** Reconstructs evaluated map from [resultStack] using keys from [frame]. */
    private fun processMapFrameNode(frame: MapFrame, resultStack: MutableList<Any?>) {
        val startIndex = resultStack.size - frame.keysInStackOrder.size

        var changed = false
        for (i in frame.keysInStackOrder.indices) {
            val key = frame.keysInStackOrder[i]
            val originalValue = frame.original[key]
            val evaluatedValue = resultStack[startIndex + i]
            if (originalValue !== evaluatedValue) {
                changed = true
                break
            }
        }

        val result =
            if (changed) {
                val evaluatedMap = LinkedHashMap<String, Any?>(frame.keysInStackOrder.size)
                for (i in frame.keysInStackOrder.indices) {
                    evaluatedMap[frame.keysInStackOrder[i]] = resultStack[startIndex + i]
                }
                evaluatedMap
            } else {
                frame.original
            }

        if (frame.keysInStackOrder.isNotEmpty()) {
            resultStack.subList(startIndex, resultStack.size).clear()
        }

        resultStack.add(result)
    }

    /** Reconstructs evaluated list from [resultStack] using size from [frame]. */
    private fun processListFrameNode(frame: ListFrame, resultStack: MutableList<Any?>) {
        val size = frame.original.size
        val startIndex = resultStack.size - size

        var changed = false
        for (i in 0 until size) {
            val originalValue = frame.original[i]
            val evaluatedValue = resultStack[startIndex + i]
            if (originalValue !== evaluatedValue) {
                changed = true
                break
            }
        }

        val result =
            if (changed) {
                val evaluatedList = ArrayList<Any?>(size)
                for (i in startIndex until resultStack.size) {
                    evaluatedList.add(resultStack[i])
                }
                evaluatedList
            } else {
                frame.original
            }

        if (size > 0) {
            resultStack.subList(startIndex, resultStack.size).clear()
        }

        resultStack.add(result)
    }

    /**
     * Scheduled function call waiting for execution. Tracks the function name [callName] and
     * ordered argument parameter [keys].
     */
    private class Frame(val callName: String, val keys: Array<String>)

    /**
     * Scheduled plain map waiting for its values to be evaluated. Tracks the map
     * [keysInStackOrder].
     */
    private class MapFrame(val original: Map<String, *>, val keysInStackOrder: Array<String>)

    /** Scheduled list waiting for its elements to be evaluated. Tracks the original [List]. */
    private class ListFrame(val original: List<*>)

    private companion object {
        private const val KEY_PATH = "path"
        private const val KEY_CALL = "call"
        private const val KEY_ARGS = "args"
        private const val KEY_CALLABLE_FROM = "callableFrom"
        private const val KEY_RETURN_TYPE = "returnType"
    }
}
