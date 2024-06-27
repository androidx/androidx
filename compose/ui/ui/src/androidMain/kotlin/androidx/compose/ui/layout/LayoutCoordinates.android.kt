/*
 * Copyright 2024 The Android Open Source Project
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

@file:JvmName("LayoutCoordinatesKt")
@file:JvmMultifileClass

package androidx.compose.ui.layout

import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.input.pointer.MatrixPositionCalculator
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.node.requireOwner

/**
 * Takes a [matrix] which transforms some coordinate system `C` to local coordinates, and updates
 * the matrix to transform from `C` to screen coordinates instead.
 */
@Suppress("DocumentExceptions")
fun LayoutCoordinates.transformToScreen(matrix: Matrix) {
    val rootCoordinates = findRootCoordinates()

    // transformFrom resets matrix, so apply it to temporary one.
    val tmpMatrix = Matrix()
    rootCoordinates.transformFrom(this, tmpMatrix)
    matrix *= tmpMatrix

    val owner = toCoordinatorOrNull()?.layoutNode?.requireOwner() as? MatrixPositionCalculator
    if (owner != null) {
        owner.localToScreen(matrix)
    } else {
        // Fallback: try to extract just position
        val screenPosition = rootCoordinates.positionOnScreen()
        if (screenPosition.isSpecified) {
            matrix.translate(screenPosition.x, screenPosition.y, 0f)
        }
    }
}

private fun LayoutCoordinates.toCoordinatorOrNull() =
    (this as? LookaheadLayoutCoordinates)?.coordinator ?: this as? NodeCoordinator
