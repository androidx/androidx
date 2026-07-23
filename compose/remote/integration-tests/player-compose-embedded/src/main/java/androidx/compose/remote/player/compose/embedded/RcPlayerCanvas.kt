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
import androidx.compose.remote.core.operations.layout.CanvasContent
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.rememberTextMeasurer

@Composable
internal fun RcPlayerCanvas(layout: CanvasLayout, modifier: Modifier) {
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
    val textMeasurer = rememberTextMeasurer()
    Box(modifier = modifier) {
        // WIDTH/HEIGHT ComponentValue feedback is published from the component dispatch's
        // onSizeChanged (RcPlayerComponent), which fires at layout time — before anything draws —
        // so expressions reading the canvas size are correct on the same frame. Publishing here in
        // the draw pass (as this used to) is redundant and a frame-lag hazard: a reader drawn
        // earlier in the pass would see the previous frame's value, and writing snapshot state
        // during draw re-invalidates the frame.
        Canvas(modifier = Modifier.fillMaxSize()) {
            executeOperations(operations, remoteContext, textMeasurer, graph = graph)
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
