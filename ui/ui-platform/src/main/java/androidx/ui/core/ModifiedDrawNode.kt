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

import androidx.ui.geometry.Offset
import androidx.ui.geometry.RRect
import androidx.ui.geometry.Rect
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.ClipOp
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.NativeCanvas
import androidx.ui.graphics.Paint
import androidx.ui.graphics.Path
import androidx.ui.graphics.Picture
import androidx.ui.graphics.PointMode
import androidx.ui.graphics.Vertices
import androidx.ui.graphics.vectormath.Matrix4
import androidx.ui.unit.PxSize
import androidx.ui.unit.toPxSize

internal class ModifiedDrawNode(
    wrapped: LayoutNodeWrapper,
    private val drawModifier: DrawModifier
) : DelegatingLayoutNodeWrapper(wrapped) {
    private val drawScope = DrawScopeImpl()
    private var canvas: Canvas? = null

    override fun performMeasure(constraints: Constraints): Placeable {
        val thisPlaceable = super.performMeasure(constraints)
        if (dirtySize) {
            // In case there is a layer between this modifier and the layout node.
            findLayer()?.invalidate()
        }
        return thisPlaceable
    }

    // This is not thread safe
    override fun draw(canvas: Canvas) {
        withPositionTranslation(canvas) {
            this.canvas = canvas
            with(drawScope) {
                with(drawModifier) { draw() }
            }
            this.canvas = null
        }
    }

    inner class DrawScopeImpl() : ContentDrawScope {
        override fun drawContent() {
            wrapped.draw(canvas!!)
        }

        override val density: Float
            get() = layoutNode.requireOwner().density.density

        override val fontScale: Float
            get() = layoutNode.requireOwner().density.fontScale

        override val size: PxSize
            get() = this@ModifiedDrawNode.measuredSize.toPxSize()

        override val nativeCanvas: NativeCanvas
            get() = canvas!!.nativeCanvas

        override fun save() = canvas!!.save()

        override fun restore() = canvas!!.restore()

        override fun saveLayer(bounds: Rect, paint: Paint) = canvas!!.saveLayer(bounds, paint)

        override fun translate(dx: Float, dy: Float) = canvas!!.translate(dx, dy)

        override fun scale(sx: Float, sy: Float) = canvas!!.scale(sx, sy)

        override fun rotate(degrees: Float) = canvas!!.rotate(degrees)

        override fun skew(sx: Float, sy: Float) = canvas!!.skew(sx, sy)

        override fun concat(matrix4: Matrix4) = canvas!!.concat(matrix4)

        override fun clipRect(rect: Rect, clipOp: ClipOp) = canvas!!.clipRect(rect, clipOp)

        override fun clipRRect(rrect: RRect) = canvas!!.clipRRect(rrect)

        override fun clipPath(path: Path) = canvas!!.clipPath(path)

        override fun drawLine(p1: Offset, p2: Offset, paint: Paint) =
            canvas!!.drawLine(p1, p2, paint)

        override fun drawRect(rect: Rect, paint: Paint) = canvas!!.drawRect(rect, paint)

        override fun drawRoundRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            radiusX: Float,
            radiusY: Float,
            paint: Paint
        ) = canvas!!.drawRoundRect(left, top, right, bottom, radiusX, radiusY, paint)

        override fun drawOval(rect: Rect, paint: Paint) = canvas!!.drawOval(rect, paint)

        override fun drawCircle(center: Offset, radius: Float, paint: Paint) =
            canvas!!.drawCircle(center, radius, paint)

        override fun drawArc(
            rect: Rect,
            startAngle: Float,
            sweepAngle: Float,
            useCenter: Boolean,
            paint: Paint
        ) = canvas!!.drawArc(rect, startAngle, sweepAngle, useCenter, paint)

        override fun drawPath(path: Path, paint: Paint) = canvas!!.drawPath(path, paint)

        override fun drawImage(image: ImageAsset, topLeftOffset: Offset, paint: Paint) =
            canvas!!.drawImage(image, topLeftOffset, paint)

        override fun drawImageRect(image: ImageAsset, src: Rect, dst: Rect, paint: Paint) =
            canvas!!.drawImageRect(image, src, dst, paint)

        override fun drawPicture(picture: Picture) = canvas!!.drawPicture(picture)

        override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) =
            canvas!!.drawPoints(pointMode, points, paint)

        override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) =
            canvas!!.drawRawPoints(pointMode, points, paint)

        override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) =
            canvas!!.drawVertices(vertices, blendMode, paint)

        override fun enableZ() = canvas!!.enableZ()

        override fun disableZ() = canvas!!.disableZ()
    }
}