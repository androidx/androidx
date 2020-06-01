/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.graphics

import android.graphics.Matrix
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Rect
import androidx.ui.geometry.Size
import androidx.ui.graphics.vectormath.Matrix4
import androidx.ui.graphics.vectormath.isIdentity
import androidx.ui.util.fastForEach

actual typealias NativeCanvas = android.graphics.Canvas

/**
 * Create a new Canvas instance that targets its drawing commands
 * to the provided [ImageAsset]
 */
internal actual fun ActualCanvas(image: ImageAsset): Canvas =
    AndroidCanvas().apply {
        internalCanvas = android.graphics.Canvas(image.asAndroidBitmap())
    }

fun Canvas(c: android.graphics.Canvas): Canvas =
    AndroidCanvas().apply { internalCanvas = c }

/**
 * Holder class that is used to issue scoped calls to a [Canvas] from the framework
 * equivalent canvas without having to allocate an object on each draw call
 */
class CanvasHolder {
    @PublishedApi internal val androidCanvas = AndroidCanvas()

    inline fun drawInto(targetCanvas: android.graphics.Canvas, block: Canvas.() -> Unit) {
        val previousCanvas = androidCanvas.internalCanvas
        androidCanvas.internalCanvas = targetCanvas
        androidCanvas.block()
        androidCanvas.internalCanvas = previousCanvas
    }
}

// Stub canvas instance used to keep the internal canvas parameter non-null during its
// scoped usage and prevent unnecessary byte code null checks from being generated
private val EmptyCanvas = android.graphics.Canvas()

@PublishedApi internal class AndroidCanvas() : Canvas {

    // Keep the internal canvas as a var prevent having to allocate an AndroidCanvas
    // instance on each draw call
    @PublishedApi internal var internalCanvas: NativeCanvas = EmptyCanvas

    private val srcRect: android.graphics.Rect by lazy(LazyThreadSafetyMode.NONE) {
            android.graphics.Rect()
        }

    private val dstRect: android.graphics.Rect by lazy(LazyThreadSafetyMode.NONE) {
            android.graphics.Rect()
        }

    /**
     * @see Canvas.save
     */
    override fun save() {
        internalCanvas.save()
    }

    /**
     * @see Canvas.restore
     */
    override fun restore() {
        internalCanvas.restore()
    }

    override val nativeCanvas: NativeCanvas
        get() = internalCanvas

    /**
     * @see Canvas.saveLayer
     */
    @SuppressWarnings("deprecation")
    override fun saveLayer(bounds: Rect, paint: Paint) {
        @Suppress("DEPRECATION")
        internalCanvas.saveLayer(
            bounds.left,
            bounds.top,
            bounds.right,
            bounds.bottom,
            paint.asFrameworkPaint(),
            android.graphics.Canvas.ALL_SAVE_FLAG
        )
    }

    /**
     * @see Canvas.translate
     */
    override fun translate(dx: Float, dy: Float) {
        internalCanvas.translate(dx, dy)
    }

    /**
     * @see Canvas.scale
     */
    override fun scale(sx: Float, sy: Float) {
        internalCanvas.scale(sx, sy)
    }

    /**
     * @see Canvas.rotate
     */
    override fun rotate(degrees: Float) {
        internalCanvas.rotate(degrees)
    }

    /**
     * @see Canvas.skew
     */
    override fun skew(sx: Float, sy: Float) {
        internalCanvas.skew(sx, sy)
    }

    /**
     * @throws IllegalStateException if an arbitrary transform is provided
     */
    override fun concat(matrix4: Matrix4) {
        if (!matrix4.isIdentity()) {
            val frameworkMatrix = Matrix()
            if (matrix4.get(2, 0) != 0f ||
                matrix4.get(2, 1) != 0f ||
                matrix4.get(2, 0) != 0f ||
                matrix4.get(2, 1) != 0f ||
                matrix4.get(2, 2) != 1f ||
                matrix4.get(2, 3) != 0f ||
                matrix4.get(3, 2) != 0f) {
                throw IllegalStateException("Android does not support arbitrary transforms")
            }
            val values = floatArrayOf(
                matrix4.get(0, 0),
                matrix4.get(1, 0),
                matrix4.get(3, 0),
                matrix4.get(0, 1),
                matrix4.get(1, 1),
                matrix4.get(3, 1),
                matrix4.get(0, 3),
                matrix4.get(1, 3),
                matrix4.get(3, 3)
            )
            frameworkMatrix.setValues(values)
            internalCanvas.concat(frameworkMatrix)
        }
    }

    @SuppressWarnings("deprecation")
    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
        @Suppress("DEPRECATION")
        internalCanvas.clipRect(left, top, right, bottom, clipOp.toRegionOp())
    }

    /**
     * @see Canvas.clipPath
     */
    override fun clipPath(path: Path, clipOp: ClipOp) {
        @Suppress("DEPRECATION")
        internalCanvas.clipPath(path.asAndroidPath(), clipOp.toRegionOp())
    }

    fun ClipOp.toRegionOp(): android.graphics.Region.Op =
        when (this) {
            ClipOp.difference -> android.graphics.Region.Op.DIFFERENCE
            else -> android.graphics.Region.Op.INTERSECT
        }

    /**
     * @see Canvas.drawLine
     */
    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        internalCanvas.drawLine(
            p1.dx,
            p1.dy,
            p2.dx,
            p2.dy,
            paint.asFrameworkPaint()
        )
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        internalCanvas.drawRect(left, top, right, bottom, paint.asFrameworkPaint())
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
        paint: Paint
    ) {
        internalCanvas.drawRoundRect(
            left,
            top,
            right,
            bottom,
            radiusX,
            radiusY,
            paint.asFrameworkPaint()
        )
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        internalCanvas.drawOval(left, top, right, bottom, paint.asFrameworkPaint())
    }

    /**
     * @see Canvas.drawCircle
     */
    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        internalCanvas.drawCircle(
            center.dx,
            center.dy,
            radius,
            paint.asFrameworkPaint()
        )
    }

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        internalCanvas.drawArc(
            left,
            top,
            right,
            bottom,
            startAngle,
            sweepAngle,
            useCenter,
            paint.asFrameworkPaint()
        )
    }

    /**
     * @see Canvas.drawPath
     */
    override fun drawPath(path: Path, paint: Paint) {
        internalCanvas.drawPath(path.asAndroidPath(), paint.asFrameworkPaint())
    }

    /**
     * @see Canvas.drawImage
     */
    override fun drawImage(image: ImageAsset, topLeftOffset: Offset, paint: Paint) {
        internalCanvas.drawBitmap(
            image.asAndroidBitmap(),
            topLeftOffset.dx,
            topLeftOffset.dy,
            paint.asFrameworkPaint()
        )
    }

    /**
     * @See Canvas.drawImageRect
     */
    override fun drawImageRect(
        image: ImageAsset,
        srcOffset: Offset,
        srcSize: Size,
        dstOffset: Offset,
        dstSize: Size,
        paint: Paint
    ) {
        // There is no framework API to draw a subset of a target bitmap
        // that consumes only primitives so lazily allocate a src and dst
        // rect to populate the dimensions and re-use across calls
        internalCanvas.drawBitmap(
            image.asAndroidBitmap(),
            srcRect.apply {
                left = srcOffset.dx.toInt()
                top = srcOffset.dy.toInt()
                right = (srcOffset.dx + srcSize.width).toInt()
                bottom = (srcOffset.dy + srcSize.height).toInt()
            },
            dstRect.apply {
                left = dstOffset.dx.toInt()
                top = dstOffset.dy.toInt()
                right = (dstOffset.dx + dstSize.width).toInt()
                bottom = (dstOffset.dy + dstSize.height).toInt()
            },
            paint.asFrameworkPaint()
        )
    }

    /**
     * @see Canvas.drawPoints
     */
    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
        when (pointMode) {
            // Draw a line between each pair of points, each point has at most one line
            // If the number of points is odd, then the last point is ignored.
            PointMode.lines -> drawLines(points, paint, 2)

            // Connect each adjacent point with a line
            PointMode.polygon -> drawLines(points, paint, 1)

            // Draw a point at each provided coordinate
            PointMode.points -> drawPoints(points, paint)
        }
    }

    override fun enableZ() {
        CanvasUtils.enableZ(internalCanvas, true)
    }

    override fun disableZ() {
        CanvasUtils.enableZ(internalCanvas, false)
    }

    private fun drawPoints(points: List<Offset>, paint: Paint) {
        points.fastForEach { point ->
            internalCanvas.drawPoint(
                point.dx,
                point.dy,
                paint.asFrameworkPaint()
            )
        }
    }

    /**
     * Draw lines connecting points based on the corresponding step.
     *
     * ex. 3 points with a step of 1 would draw 2 lines between the first and second points
     * and another between the second and third
     *
     * ex. 4 points with a step of 2 would draw 2 lines between the first and second and another
     * between the third and fourth. If there is an odd number of points, the last point is
     * ignored
     *
     * @see drawRawLines
     */
    private fun drawLines(points: List<Offset>, paint: Paint, stepBy: Int) {
        if (points.size >= 2) {
            for (i in 0 until points.size - 1 step stepBy) {
                val p1 = points[i]
                val p2 = points[i + 1]
                internalCanvas.drawLine(
                    p1.dx,
                    p1.dy,
                    p2.dx,
                    p2.dy,
                    paint.asFrameworkPaint()
                )
            }
        }
    }

    /**
     * @throws IllegalArgumentException if a non even number of points is provided
     */
    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) {
        if (points.size % 2 != 0) {
            throw IllegalArgumentException("points must have an even number of values")
        }
        when (pointMode) {
            PointMode.lines -> drawRawLines(points, paint, 2)
            PointMode.polygon -> drawRawLines(points, paint, 1)
            PointMode.points -> drawRawPoints(points, paint, 2)
        }
    }

    private fun drawRawPoints(points: FloatArray, paint: Paint, stepBy: Int) {
        if (points.size % 2 == 0) {
            for (i in 0 until points.size - 1 step stepBy) {
                val x = points[i]
                val y = points[i + 1]
                internalCanvas.drawPoint(x, y, paint.asFrameworkPaint())
            }
        }
    }

    /**
     * Draw lines connecting points based on the corresponding step. The points are interpreted
     * as x, y coordinate pairs in alternating index positions
     *
     * ex. 3 points with a step of 1 would draw 2 lines between the first and second points
     * and another between the second and third
     *
     * ex. 4 points with a step of 2 would draw 2 lines between the first and second and another
     * between the third and fourth. If there is an odd number of points, the last point is
     * ignored
     *
     * @see drawLines
     */
    private fun drawRawLines(points: FloatArray, paint: Paint, stepBy: Int) {
        // Float array is treated as alternative set of x and y coordinates
        // x1, y1, x2, y2, x3, y3, ... etc.
        if (points.size >= 4 && points.size % 2 == 0) {
            for (i in 0 until points.size - 3 step stepBy * 2) {
                val x1 = points[i]
                val y1 = points[i + 1]
                val x2 = points[i + 2]
                val y2 = points[i + 3]
                internalCanvas.drawLine(
                    x1,
                    y1,
                    x2,
                    y2,
                    paint.asFrameworkPaint()
                )
            }
        }
    }

    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {
        // TODO(njawad) align drawVertices blendMode parameter usage with framework
        // android.graphics.Canvas#drawVertices does not consume a blendmode argument
        internalCanvas.drawVertices(
            vertices.vertexMode.toNativeVertexMode(),
            vertices.positions.size,
            vertices.positions,
            0, // TODO(njawad) figure out proper vertOffset)
            vertices.textureCoordinates,
            0, // TODO(njawad) figure out proper texOffset)
            vertices.colors,
            0, // TODO(njawad) figure out proper colorOffset)
            vertices.indices,
            0, // TODO(njawad) figure out proper indexOffset)
            vertices.indices.size,
            paint.asFrameworkPaint()
        )
    }
}
