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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.draw

import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.remote.player.core.platform.AndroidPaintContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.util.fastForEach

/**
 * Runs [block] with the View player's [AndroidPaintContext] installed on [remoteContext], bound to
 * this [DrawScope]'s native canvas, so core `paint()` implementations (and the child ops they
 * dispatch via `Operation.apply`) render into the Compose draw pass.
 *
 * This is how the embedded player reuses View player code for features that have no Compose
 * primitive (particles, vertex meshes, path/mesh-derived matrices) rather than reimplementing them.
 * The paint context is created once per [RemoteContext] and reused, so state it caches across
 * frames (e.g. mesh geometry) persists. Transforms it applies (`canvas.concat`) land on the shared
 * native canvas, so subsequent Compose draws inherit them exactly as in the View player.
 *
 * @param observeFrameClock read the frame-clock time through [graph] so the enclosing draw re-runs
 *   every frame; needed for continuous simulations such as particles, not for static geometry.
 */
internal inline fun DrawScope.withCorePaintContext(
    remoteContext: RemoteContext,
    paintState: ComposeLocalPaint,
    graph: GraphContext?,
    observeFrameClock: Boolean = true,
    block: (PaintContext) -> Unit,
) {
    if (observeFrameClock) graph?.getFloat(RemoteContext.ID_ANIMATION_TIME)
    val canvas = drawContext.canvas.nativeCanvas
    val currentPaintContext = remoteContext.paintContext
    val paintContext =
        if (currentPaintContext is AndroidPaintContext) {
            currentPaintContext.reset()
            currentPaintContext.setCanvas(canvas)
            currentPaintContext
        } else {
            AndroidPaintContext(remoteContext, canvas).also { remoteContext.setPaintContext(it) }
        }
    // Seed the core paint from the paint ops the Compose dispatcher already consumed into
    // ComposeLocalPaint, so paint set *outside* this subtree (color, stroke, …) still applies.
    paintState.sourceBundles.fastForEach { bundle -> paintContext.applyPaint(bundle) }
    val previousMode = remoteContext.mode
    remoteContext.mode = RemoteContext.ContextMode.PAINT
    try {
        block(paintContext)
    } finally {
        remoteContext.mode = previousMode
    }
}
