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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.layout.CanvasContent
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale

@Composable
internal fun RcPlayerCanvas(layout: CanvasLayout, modifier: Modifier) {
    val componentValueMap = LocalComponentValueMap.current
    val componentValueStateMap = LocalComponentValueStateMap.current

    // A CanvasLayout's draw instructions are not in its own op list — they live in the (single)
    // CanvasContent component nested in its subtree (CanvasLayout -> ... -> CanvasContent ->
    // draws).
    // Executing layout.mList directly only runs the modifiers/wrapper, so the draws never render
    // and
    // the canvas is blank. Find the CanvasContent and execute its draw list.
    val operations =
        remember(layout) { (findCanvasContent(layout.mList)?.mList ?: layout.mList).toList() }

    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ops = componentValueMap[layout.getId()]
            ops?.forEach { op ->
                if (op.type == ComponentValue.WIDTH) {
                    componentValueStateMap[op.valueId]?.value = this.size.width
                } else if (op.type == ComponentValue.HEIGHT) {
                    componentValueStateMap[op.valueId]?.value = this.size.height
                }
            }
            val density = remoteContext.density
            scale(density, density, pivot = Offset.Zero) {
                executeOperations(operations, remoteContext, graph = graph)
            }
        }
    }
}

/**
 * Depth-first search for the (single) [CanvasContent] holding a canvas layout's draw instructions.
 */
private fun findCanvasContent(ops: List<Operation>): CanvasContent? {
    for (op in ops) {
        if (op is CanvasContent) return op
        if (op is Component) {
            findCanvasContent(op.mList)?.let {
                return it
            }
        }
    }
    return null
}
