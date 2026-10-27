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

import androidx.annotation.RestrictTo
import androidx.collection.IntSet
import androidx.collection.intSetOf
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.operations.ParticlesCompare
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.embedded.draw.drawParticles
import androidx.compose.remote.player.compose.embedded.draw.drawParticlesCompare
import androidx.compose.remote.player.compose.embedded.draw.drawParticlesCreate

/**
 * Runs particle systems: `ParticlesCreate` (seeds the particles), `ParticlesLoop` (steps and draws
 * them) and `ParticlesCompare` (pairwise interaction).
 *
 * These are in the baseline and AndroidX profiles, but only some documents use them, and they run
 * through the View player's core paint context, so an app that knows its documents don't use
 * particles can leave the feature out. The overloads of [RcPlayer] without an [RcPlayerFeatureSet]
 * always include it.
 */
@ExperimentalRemotePlayerApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object ParticlesFeature : RcPlayerFeature(Key("Particles")) {
    override val opcodes: IntSet =
        intSetOf(Operations.PARTICLE_DEFINE, Operations.PARTICLE_LOOP, Operations.PARTICLE_COMPARE)

    override fun paint(op: Operation, scope: RcFeatureDrawScope): Boolean {
        val graph = scope.graph
        when (op) {
            // Re-seeds on every paint, as in the View player; wrap it in an impulse to seed once.
            is ParticlesCreate -> drawParticlesCreate(op, scope.evalPaintContext(), graph)
            // Stepping and interaction need the graph for seed state and frame-clock observation;
            // without it they are skipped.
            is ParticlesLoop ->
                if (graph != null) {
                    scope.drawScope.drawParticles(op, scope.remoteContext, scope.paintState, graph)
                }
            is ParticlesCompare ->
                if (graph != null) {
                    scope.drawScope.drawParticlesCompare(
                        op,
                        scope.remoteContext,
                        scope.paintState,
                        graph,
                    )
                }
            else -> return false
        }
        return true
    }
}
