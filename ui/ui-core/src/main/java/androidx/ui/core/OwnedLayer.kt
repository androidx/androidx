/*
 * Copyright 2020 The Android Open Source Project
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

import android.graphics.Matrix
import androidx.ui.graphics.Canvas
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize

/**
 * A layer returned by [Owner.createLayer] to separate drawn content. An `OwnedLayer` has
 * the implementation to make [DrawLayerModifier]s work.
 */
interface OwnedLayer {
    /**
     * The ID of the layer. This is used by tooling to match a layer to the associated
     * LayoutNode.
     */
    val layerId: Long

    /**
     * Reads the [DrawLayerModifier] and dirties the layer so that it will be redrawn.
     */
    fun updateLayerProperties()

    /**
     * Changes the position of the layer contents.
     */
    fun move(position: IntPxPosition)

    /**
     * Changes the size of the layer's drawn area.
     */
    fun resize(size: IntPxSize)

    /**
     * Causes the layer to be drawn into [canvas]
     */
    fun drawLayer(canvas: Canvas)

    /**
     * Updates the drawing on the current canvas.
     */
    fun updateDisplayList()

    /**
     * Asks to the layer to redraw itself without forcing all of its parents to redraw.
     */
    fun invalidate()

    /**
     * Indicates that the layer is no longer needed.
     */
    fun destroy()

    /**
     * Returns a matrix that this layer will use to transform the contents.
     * The caller must not modify the returned Matrix.
     */
    fun getMatrix(): Matrix
}