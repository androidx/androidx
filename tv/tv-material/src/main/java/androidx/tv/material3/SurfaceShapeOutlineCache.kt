/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Caches the shape's outline across re-compositions
 */
internal class SurfaceShapeOutlineCache(
    private var shape: Shape,
    private var size: Size,
    private var layoutDirection: LayoutDirection,
    private var density: Density
) {
    private var outline: Outline? = null

    /**
     * If there are updates to the arguments, creates a new outline
     * based on the updated values, else, returns the cached value
     */
    fun updatedOutline(
        shape: Shape,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (outline == null || hasUpdates(shape, size, layoutDirection, density)) {
            syncUpdates(shape, size, layoutDirection, density)
            createNewOutline()
        }
        return outline!!
    }

    private fun createNewOutline() {
        outline = shape.createOutline(
            size = size,
            layoutDirection = layoutDirection,
            density = density
        )
    }

    private fun syncUpdates(
        shape: Shape,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ) {
        this.shape = shape
        this.size = size
        this.layoutDirection = layoutDirection
        this.density = density
    }

    private fun hasUpdates(
        shape: Shape,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Boolean {
        if (shape != this.shape) return true
        if (size != this.size) return true
        if (layoutDirection != this.layoutDirection) return true
        if (density != this.density) return true
        return false
    }
}
