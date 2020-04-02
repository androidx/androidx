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

import android.graphics.RectF
import androidx.ui.graphics.Canvas
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.PxPosition
import androidx.ui.unit.px

internal class LayerWrapper(
    wrapped: LayoutNodeWrapper,
    val drawLayerModifier: DrawLayerModifier
) : DelegatingLayoutNodeWrapper(wrapped) {
    private var _layer: OwnedLayer? = null
    private var layerDestroyed = false

    private val invalidateParentLayer: () -> Unit = {
        wrappedBy?.findLayer()?.invalidate()
    }

    val layer: OwnedLayer
        get() {
            return _layer ?: layoutNode.requireOwner().createLayer(
                drawLayerModifier,
                wrapped::draw,
                invalidateParentLayer
            ).also {
                _layer = it
                invalidateParentLayer()
            }
        }

    // TODO(mount): This cache isn't thread safe at all.
    private var positionCache: FloatArray? = null

    override fun measure(constraints: Constraints): Placeable {
        val placeable = super.measure(constraints)
        layer.resize(size)
        return placeable
    }

    override fun place(position: IntPxPosition) {
        super.place(position)
        layer.move(position)
    }

    override fun draw(canvas: Canvas) {
        layer.drawLayer(canvas)
    }

    override fun detach() {
        super.detach()
        _layer?.destroy()
    }

    override fun findLayer(): OwnedLayer? {
        return layer
    }

    override fun toParentPosition(position: PxPosition): PxPosition {
        val matrix = layer.getMatrix()
        if (!matrix.isIdentity) {
            val x = position.x.value
            val y = position.y.value
            val cache = positionCache
            val point = if (cache != null) {
                cache[0] = x
                cache[1] = y
                cache
            } else {
                floatArrayOf(x, y).also { positionCache = it }
            }
            matrix.mapPoints(point)
            return super.toParentPosition(PxPosition(point[0].px, point[1].px))
        } else {
            return super.toParentPosition(position)
        }
    }

    override fun rectInParent(bounds: RectF) {
        if ((drawLayerModifier.clipToBounds ||
                    (drawLayerModifier.clipToOutline && drawLayerModifier.outlineShape != null)) &&
            !bounds.intersect(0f, 0f, size.width.value.toFloat(), size.height.value.toFloat())
        ) {
            bounds.setEmpty()
        }
        val matrix = layer.getMatrix()
        matrix.mapRect(bounds)
        return super.rectInParent(bounds)
    }
}
