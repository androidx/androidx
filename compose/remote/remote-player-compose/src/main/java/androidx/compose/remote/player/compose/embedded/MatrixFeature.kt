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

import android.graphics.Matrix as AndroidMatrix
import android.graphics.PathMeasure as AndroidPathMeasure
import androidx.annotation.RestrictTo
import androidx.collection.IntSet
import androidx.collection.intSetOf
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.MatrixFromPath
import androidx.compose.remote.core.operations.matrix.MatrixExpression
import androidx.compose.remote.core.operations.matrix.MatrixVectorMath
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.utils.getPath
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas

/**
 * Executes matrix operations: `MatrixExpression` (4x4 matrices built from an RPN expression),
 * `MatrixVectorMath` (matrix * vector, e.g. projecting 3D vertices) and `MatrixFromPath` (a canvas
 * matrix from a position along a path).
 *
 * These are in the baseline and AndroidX profiles, but only documents with 3D content or
 * path-following transforms use them, so an app that knows its documents don't can leave the
 * feature out. The overloads of [RcPlayer] without an [RcPlayerFeatureSet] always include it.
 */
@ExperimentalRemotePlayerApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object MatrixFeature : RcPlayerFeature(Key("Matrix")) {
    override val opcodes: IntSet =
        intSetOf(
            Operations.MATRIX_EXPRESSION,
            Operations.MATRIX_VECTOR_MATH,
            Operations.MATRIX_FROM_PATH,
        )

    override fun paint(op: Operation, scope: RcFeatureDrawScope): Boolean {
        val remoteContext = scope.remoteContext
        when (op) {
            is MatrixExpression -> {
                if (remoteContext.getObject(op.id) !== op) remoteContext.putObject(op.id, op)
                // Re-evaluate for the current variables (updateVariables ran in the draw loop).
                // apply() recomputes the op's own values in place - the op is the MatrixAccess
                // the store holds - then writes a change timestamp; routing it through the graph
                // drops that write and the nanoTime loadFloat notification, so a per-frame
                // timestamp can't dirty snapshot state from inside draw.
                op.apply(scope.graph ?: remoteContext)
            }
            is MatrixVectorMath -> {
                // matrix * vector, e.g. the 3D projection step. The matrix is re-evaluated first so
                // it reflects variables changed earlier in this stream. Outputs go to the real
                // store so later ops in this stream (path vertices, draws) read them, like
                // FloatExpression.
                val read = scope.graph ?: remoteContext
                val matrix = read.getObject(op.mMatrixId)
                if (matrix is MatrixExpression) {
                    matrix.updateVariables(read)
                    matrix.apply(read)
                }
                op.apply(remoteContext)
            }
            is MatrixFromPath -> {
                val paintContext = scope.evalPaintContext()
                paintContext.onMatrixFromPath = { pathId, fraction, _, flags ->
                    scope.drawScope.concatMatrixFromPath(remoteContext, pathId, fraction, flags)
                }
                try {
                    op.paint(paintContext)
                } finally {
                    paintContext.onMatrixFromPath = null
                }
            }
            else -> return false
        }
        return true
    }
}

/**
 * Concatenates the position/tangent matrix at [fraction] of path [pathId] onto the canvas. Mirrors
 * the View player's `AndroidPaintContext.matrixFromPath`, but resolves the path through the
 * embedded player's (Compose) path cache rather than the View player's.
 */
private fun DrawScope.concatMatrixFromPath(
    remoteContext: RemoteContext,
    pathId: Int,
    fraction: Float,
    flags: Int,
) {
    val path = remoteContext.mRemoteComposeState.getPath(pathId, 0f, 1f)
    if (path.isEmpty) return
    val measure = AndroidPathMeasure(path.asAndroidPath(), false)
    val length = measure.length
    if (length <= 0f) return
    val matrix = AndroidMatrix()
    if (measure.getMatrix((length * fraction) % length, matrix, flags)) {
        drawContext.canvas.nativeCanvas.concat(matrix)
    }
}
