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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.ParticlesCompare
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.player.core.platform.AndroidPaintContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.util.fastForEach

/**
 * Particle rendering by *reusing the core implementation* — the same `ParticlesLoop.paint` /
 * `ParticlesCompare.paint` / `ParticlesCreate.paint` code the View player runs, so the particle
 * simulation (seeding, stepping, restart, pairwise interaction, per-particle child drawing) exists
 * in exactly one place.
 *
 * The bridge is [withCorePaintContext]: it wraps the Compose [DrawScope]'s native canvas in the
 * View player's [AndroidPaintContext] (inheriting the current canvas transform), replays the paint
 * state the Compose dispatcher has accumulated (see [ComposeLocalPaint.sourceBundles]), installs
 * the paint context + PAINT mode on the [RemoteContext] so the ops' `apply` routes to `paint`, and
 * restores everything after. Compose-driven reactivity is preserved by reading the frame-clock time
 * through the [GraphContext]: that registers this draw as an observer of the frame tick, so it
 * re-runs (and the core code re-steps the simulation) every frame while the player's frame loop is
 * alive (kept alive for documents containing particles — see `containsParticles` in RcPlayer).
 *
 * Particle state (the `float[count][dims]` array) lives on the core `ParticlesCreate` op and is
 * advanced in place by core; only the one-time seeding is tracked here (per document, on
 * [GraphContext]) because unlike the View player the embedded player never "paints" the setup ops.
 */
internal fun DrawScope.drawParticles(
    loop: ParticlesLoop,
    remoteContext: RemoteContext,
    paintState: ComposeLocalPaint,
    graph: GraphContext,
) {
    val source = loop.particlesSourceReflection ?: return
    withCorePaintContext(remoteContext, paintState, graph) { paintContext ->
        // Seed once per document via the core seeding path (ParticlesCreate.paint initializes
        // every particle from its initial-value equations).
        if (graph.particlesInitialized.add(System.identityHashCode(loop))) {
            source.updateVariables(remoteContext)
            source.paint(paintContext)
        }
        loop.updateVariables(remoteContext)
        loop.paint(paintContext)
    }
}

/** [ParticlesCompare] (particle interaction pass), reusing the core implementation. */
internal fun DrawScope.drawParticlesCompare(
    op: ParticlesCompare,
    remoteContext: RemoteContext,
    paintState: ComposeLocalPaint,
    graph: GraphContext,
) {
    withCorePaintContext(remoteContext, paintState, graph) { paintContext ->
        op.updateVariables(remoteContext)
        op.paint(paintContext)
    }
}

/**
 * Runs [block] with the View player's [AndroidPaintContext] installed on [remoteContext], bound to
 * this [DrawScope]'s native canvas, so core `paint()` implementations (and the child ops they
 * dispatch via `Operation.apply`) render into the Compose draw pass. See the file doc.
 */
private inline fun DrawScope.withCorePaintContext(
    remoteContext: RemoteContext,
    paintState: ComposeLocalPaint,
    graph: GraphContext,
    block: (PaintContext) -> Unit,
) {
    // Observe the frame clock so the draw re-runs each tick (continuous simulation).
    graph.getFloat(RemoteContext.ID_TIME_IN_SEC)
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
