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

import android.graphics.Bitmap
import androidx.annotation.RestrictTo
import androidx.collection.IntSet
import androidx.collection.intSetOf
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.PaintOperation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.AddMesh2D
import androidx.compose.remote.core.operations.DrawMesh2D
import androidx.compose.remote.core.operations.MatrixFromMesh2D
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.embedded.draw.withCorePaintContext
import androidx.compose.remote.player.compose.embedded.resources.resolveCanvasBitmap

/**
 * Draws 2D vertex meshes (`AddMesh2D`, `DrawMesh2D`, `MatrixFromMesh2D`) from the experimental
 * profile.
 *
 * Meshes have no Compose primitive, so their core paint runs through the paint context shared with
 * the View player, which caches mesh geometry across frames. `MatrixFromMesh2D` concats onto the
 * canvas, so the stream's save/restore scopes it like any other transform.
 */
@ExperimentalRemotePlayerApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object MeshFeature : RcPlayerFeature(Key("Mesh")) {
    override val opcodes: IntSet =
        intSetOf(Operations.ADD_MESH_2D, Operations.DRAW_MESH_2D, Operations.MATRIX_FROM_MESH_2D)

    override val profiles: Int
        get() = RcProfiles.PROFILE_EXPERIMENTAL

    override fun paint(op: Operation, scope: RcFeatureDrawScope): Boolean {
        when (op) {
            is AddMesh2D,
            is MatrixFromMesh2D -> scope.paintCore(op)
            is DrawMesh2D -> {
                // The core draw reads the texture straight from the store, so resolve it through
                // the host RcImageLoader (falling back to lazy embedded decode) and cache it first.
                val imageId = op.imageId
                if (imageId != DrawMesh2D.NO_IMAGE) {
                    val bitmap = resolveCanvasBitmap(scope.graph, scope.remoteContext, imageId)
                    val state = scope.remoteContext.mRemoteComposeState
                    if (bitmap != null && state.getFromId(imageId) !== bitmap) {
                        state.cacheData(imageId, bitmap)
                    } else if (bitmap == null && state.getFromId(imageId) !is Bitmap) {
                        return true
                    }
                }
                scope.paintCore(op)
            }
            else -> return false
        }
        return true
    }
}

/**
 * Runs [op]'s core paint through the paint context shared with the View player, so state it caches
 * (mesh geometry) survives across frames.
 */
@OptIn(ExperimentalRemotePlayerApi::class)
private fun RcFeatureDrawScope.paintCore(op: PaintOperation) {
    drawScope.withCorePaintContext(remoteContext, paintState, graph, observeFrameClock = false) {
        op.paint(it)
    }
}
