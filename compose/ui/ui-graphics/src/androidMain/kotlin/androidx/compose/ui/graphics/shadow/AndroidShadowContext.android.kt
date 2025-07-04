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

package androidx.compose.ui.graphics.shadow

import androidx.collection.MutableScatterMap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

private typealias DropShadowCache =
    MutableScatterMap<AndroidShadowContext.ShadowKey, DropShadowRenderer>

private typealias InnerShadowCache =
    MutableScatterMap<AndroidShadowContext.ShadowKey, InnerShadowRenderer>

/** Create a new [ShadowContext] */
fun ShadowContext(): ShadowContext = AndroidShadowContext()

private class AndroidShadowContext :
    PlatformShadowContext, DropShadowRendererProvider, InnerShadowRendererProvider {

    private var dropShadowCache: DropShadowCache? = null
    private var innerShadowCache: InnerShadowCache? = null
    private var shadowKey: ShadowKey? = null

    private fun obtainDropShadowCache(): DropShadowCache =
        dropShadowCache ?: DropShadowCache().also { dropShadowCache = it }

    private fun obtainInnerShadowCache(): InnerShadowCache =
        innerShadowCache ?: InnerShadowCache().also { innerShadowCache = it }

    private fun obtainShadowKey(): ShadowKey = shadowKey ?: ShadowKey().also { shadowKey = it }

    // Class to represent a lookup key for cached ShadowRenderer implementations
    // Note: Take care not to mutate a key which is used in the cache maps as the hash code depends
    // on its values.
    data class ShadowKey(
        var shape: Shape = RectangleShape,
        var size: Size = Size.Zero,
        var layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        var density: Float = 1f,
        var shadow: Shadow? = null,
    )

    override fun obtainDropShadowRenderer(
        shape: Shape,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
        shadow: Shadow,
    ): DropShadowRenderer {
        synchronized(this) {
            val key =
                obtainShadowKey().apply {
                    this.shape = shape
                    this.size = size
                    this.layoutDirection = layoutDirection
                    this.density = density.density
                    // The renderer does not use the offset
                    this.shadow = shadow.copyWithoutOffset()
                }
            var renderer = obtainDropShadowCache()[key]
            if (renderer == null) {
                val outline = shape.createOutline(size, layoutDirection, density)
                renderer = DropShadowRenderer(shadow, outline)
                obtainDropShadowCache()[key.copy()] = renderer
            }
            return renderer
        }
    }

    override fun obtainInnerShadowRenderer(
        shape: Shape,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
        shadow: Shadow,
    ): InnerShadowRenderer {
        synchronized(this) {
            val key =
                obtainShadowKey().apply {
                    this.shape = shape
                    this.size = size
                    this.layoutDirection = layoutDirection
                    this.density = density.density
                    this.shadow = shadow
                }
            var renderer = obtainInnerShadowCache()[key]
            if (renderer == null) {
                val outline = shape.createOutline(size, layoutDirection, density)
                renderer = InnerShadowRenderer(shadow, outline)
                obtainInnerShadowCache()[key.copy()] = renderer
            }
            return renderer
        }
    }

    override fun createDropShadowPainter(shape: Shape, shadow: Shadow): DropShadowPainter =
        DropShadowPainter(shape, shadow, this)

    override fun createInnerShadowPainter(shape: Shape, shadow: Shadow): InnerShadowPainter =
        InnerShadowPainter(shape, shadow, this)

    override fun clearCache() {
        synchronized(this) {
            dropShadowCache?.clear()
            innerShadowCache?.clear()
            shadowKey = null
        }
    }
}
