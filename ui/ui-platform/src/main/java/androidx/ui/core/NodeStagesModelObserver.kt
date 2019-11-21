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

import android.os.Handler
import android.os.Looper
import android.util.ArrayMap
import androidx.compose.ObserverMap
import androidx.compose.WeakReference
import androidx.compose.frames.FrameCommitObserver
import androidx.compose.frames.FrameReadObserver
import androidx.compose.frames.observeAllReads
import androidx.compose.frames.registerCommitObserver

/**
 * Observes the frame reads during the node stages like drawing, measuring and layouting.
 *
 * The code you want to be observed for the model reads you have to wrap into [observeReads]
 * trailing lambda.
 *
 * Then before you start some new node stage you call [beforeStage], perform your logic where
 * you expect model reads related to this stage to be happening, and then close the stage by
 * calling [afterStage]. Or you can use [stage] if you can isolate your stage inside one lambda.
 *
 * To start/stop reacting on the model updates you need to call [enableModelUpdatesObserving].
 *
 * @param debugMode if true enables additional assertions which can be too expensive for
 * the release build.
 * @param onModelUpdated callback to be called every time the model update related to any stage
 * has been detected.
 */
internal class NodeStagesModelObserver(
    private val debugMode: Boolean = true,
    private val onModelUpdated: (Stage, ComponentNode) -> Unit
) {
    enum class Stage {
        Draw,
        Measure,
        Layout
    }

    /**
     * Map of [DrawNode]s and models used during the last [Stage.Draw] or
     * [LayoutNode]s and models used during the last [Stage.Measure].
     * We are putting these together to avoid having to create two maps.
     */
    private val drawMeasureModelMap = NodeModelMap<ComponentNode>()

    /**
     * Map of [LayoutNode]s and models used during the last calls of [Stage.Layout].
     */
    private val layoutModelMap = NodeModelMap<ComponentNode>()

    /**
     * Stores the values used for [beforeStage] calls.
     */
    private val currentNodes = mutableListOf<ComponentNode>()
    private var currentStages = mutableListOf<Stage>()

    /**
     * The value is true inside the [observeReads] block.
     */
    var isObserving = false
        private set

    /**
     * Observing the model reads can be temporary disabled.
     * For example if we are currently within the measure stage and we want some code block to
     * be skipped from the observing we disable if before calling the block, execute block and
     * then enable it again.
     */
    var modelReadEnabled: Boolean = true

    private val frameReadObserver: FrameReadObserver = { readValue ->
        check(currentNodes.isNotEmpty()) {
            "The model read happened when there were no active stages. Did you forget to call" +
                    "beforeStage() or stage()"
        }
        if (modelReadEnabled) {
            val node = currentNodes.last()
            val stage = currentStages.last()
            if (stage == Stage.Layout) {
                layoutModelMap.add(node, readValue)
            } else {
                drawMeasureModelMap.add(node, readValue)
            }
        }
    }

    private val commitObserver: FrameCommitObserver = { committed ->
        if (Looper.getMainLooper() == Looper.myLooper()) {
            onModelsCommitted(committed)
        } else {
            val list = ArrayList(committed)
            Handler(Looper.getMainLooper()).post { onModelsCommitted(list) }
        }
    }
    private var commitUnsubscribe: (() -> Unit)? = null

    private fun onModelsCommitted(models: Iterable<Any>) {
        drawMeasureModelMap[models].forEach {
            if (it is DrawNode) {
                onModelUpdated(Stage.Draw, it)
            } else {
                onModelUpdated(Stage.Measure, it)
            }
        }
        layoutModelMap[models].forEach { onModelUpdated(Stage.Layout, it) }
    }

    fun enableModelUpdatesObserving(enabled: Boolean) {
        require(enabled == (commitUnsubscribe == null)) {
            "Called twice with the same enabled value: $enabled"
        }
        if (enabled) {
            commitUnsubscribe = registerCommitObserver(commitObserver)
        } else {
            commitUnsubscribe?.invoke()
            commitUnsubscribe = null
        }
    }

    fun observeReads(block: () -> Unit) {
        checkNotNull(commitUnsubscribe)
        check(!isObserving)
        check(currentNodes.isEmpty())
        isObserving = true
        observeAllReads(frameReadObserver, block)
        isObserving = false
        check(currentNodes.isEmpty())
    }

    fun beforeStage(stage: Stage, node: ComponentNode) {
        check(modelReadEnabled) { "no stage starting is allowed while read is disabled" }
        check(isObserving) { "$stage should always be observed for the model reads" }
        if (debugMode) {
            require(stage == Stage.Draw && node is DrawNode || node is LayoutNode) {
                "$stage can't be started with this type of node - $node"
            }
        }
        currentNodes.add(node)
        currentStages.add(stage)
        when (stage) {
            Stage.Draw -> drawMeasureModelMap.clear(node)
            Stage.Measure -> drawMeasureModelMap.clear(node)
            Stage.Layout -> layoutModelMap.clear(node)
        }
    }

    fun afterStage(stage: Stage, node: ComponentNode) {
        check(modelReadEnabled) { "no stage finishing is allowed while read is disabled" }
        check(isObserving) { "$stage should always be observed for the model reads" }
        val currentNode = currentNodes.pop()
        require(node == currentNode) {
            "afterStage($stage) called with not the same node used for the " +
                    "corresponding beforeStage() call. $node instead of $currentNode"
        }
        val currentStage = currentStages.pop()
        require(currentStage == stage) {
            "afterStage($stage) called with not the same stage used for the " +
                    "corresponding beforeStage($currentStage) call."
        }
    }

    inline fun stage(stage: Stage, node: ComponentNode, crossinline block: () -> Unit) {
        beforeStage(stage, node)
        block()
        afterStage(stage, node)
    }

    fun onNodeDetached(node: ComponentNode) {
        when (node) {
            is LayoutNode -> {
                drawMeasureModelMap.clear(node)
                layoutModelMap.clear(node)
            }
            is DrawNode -> drawMeasureModelMap.clear(node)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun <T> MutableList<T>.pop(): T = removeAt(size - 1)
}

/**
 * Holder for two ObserverMaps both from model to nodes and from nodes to models.
 */
private class NodeModelMap<T : ComponentNode> {
    private val modelToNodes = ObserverMap<Any, T>()
    // This map assumes hashCode() of ComponentNode implementations are not changing
    // during the lifetime of the object.
    private val nodeToModels = ArrayMap<T, MutableList<WeakReference<Any>>>()

    fun add(node: T, readValue: Any) {
        modelToNodes.add(readValue, node)
        nodeToModels
            // don't use Set here as we don't control the hashCode() of models
            .getOrPut(node) { mutableListOf() }
            .add(WeakReference(readValue))
    }

    operator fun get(keys: Iterable<Any>): List<T> {
        return modelToNodes[keys]
    }

    fun clear(node: T) {
        nodeToModels.remove(node)?.forEach { weakRefModel ->
            weakRefModel.get()?.let {
                modelToNodes.remove(it, node)
            }
        }
    }
}
