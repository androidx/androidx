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

package androidx.compose.ui.graphics

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import org.jetbrains.skia.Canvas as SkCanvas
import org.jetbrains.skia.ClipMode as SkClipMode
import org.jetbrains.skia.CubicResampler
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.Matrix44
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Paint as SkPaint
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.impl.use

@Deprecated(
    message = "Use direct reference to org.jetbrains.skia.Canvas instead of typealias",
    replaceWith = ReplaceWith("Canvas", "org.jetbrains.skia.Canvas"),
    level = DeprecationLevel.ERROR,
)
actual typealias NativeCanvas = SkCanvas

internal actual fun ActualCanvas(image: ImageBitmap): Canvas {
    val skiaBitmap = image.asSkiaBitmap()
    require(!skiaBitmap.isImmutable) {
        "Cannot draw on immutable ImageBitmap"
    }
    return SkiaBackedCanvas(SkCanvas(skiaBitmap))
}

/**
 * Convert the [org.jetbrains.skia.Canvas] instance into a Compose-compatible Canvas
 */
fun SkCanvas.asComposeCanvas(): Canvas = SkiaBackedCanvas(this)

/**
 * Provides access to the underlying [org.jetbrains.skia.Canvas] instance.
 *
 * It throws an exception if accessed on unsupported types.
 */
val Canvas.skiaCanvas: SkCanvas
    get() {
        requirePrecondition(this is SkiaBackedCanvas) {
            "Extracting skia canvas reference is only supported from androidx.compose.ui.graphics.SkiaBackedCanvas instances but received ${this::class}"
        }
        return internalSkiaCanvas
    }

@Deprecated(
    message = "Naming alignment to avoid ambiguity: use [Canvas.skiaCanvas] extension instead",
    replaceWith = ReplaceWith("skiaCanvas", "androidx.compose.ui.graphics.skiaCanvas"),
)
val Canvas.nativeCanvas: SkCanvas
    get() = skiaCanvas

// This was added for internal usage from old render layers (another submodule),
// but wasn't properly marked as internal. Keep it as deprecated for some time to be safe.
@InternalComposeApi
@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This API is not supposed to be used outside of Compose UI package"
)
var Canvas.alphaMultiplier: Float
    get() = (this as SkiaBackedCanvas).alphaMultiplier
    set(value) { (this as SkiaBackedCanvas).alphaMultiplier = value }

internal class SkiaBackedCanvas(
    internal val internalSkiaCanvas: SkCanvas,
) : Canvas {
    internal var alphaMultiplier: Float = 1.0f

    private fun Paint.asSkiaPaintWithAppliedAlphaMultiplier(): SkPaint {
        require(this is SkiaBackedPaint)
        this.alphaMultiplier = this@SkiaBackedCanvas.alphaMultiplier
        return internalSkiaPaint
    }

    override fun save() {
        internalSkiaCanvas.save()
    }

    override fun restore() {
        internalSkiaCanvas.restore()
    }

    override fun saveLayer(bounds: Rect, paint: Paint) {
        internalSkiaCanvas.saveLayer(
            bounds.left,
            bounds.top,
            bounds.right,
            bounds.bottom,
            paint.asSkiaPaintWithAppliedAlphaMultiplier()
        )
    }

    override fun translate(dx: Float, dy: Float) {
        internalSkiaCanvas.translate(dx, dy)
    }

    override fun scale(sx: Float, sy: Float) {
        internalSkiaCanvas.scale(sx, sy)
    }

    override fun rotate(degrees: Float) {
        internalSkiaCanvas.rotate(degrees)
    }

    override fun skew(sx: Float, sy: Float) {
        internalSkiaCanvas.skew(sx, sy)
    }

    override fun concat(matrix: Matrix) {
        if (!matrix.isIdentity()) {
            internalSkiaCanvas.concat(matrix.toSkia())
        }
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
        val antiAlias = true
        internalSkiaCanvas.clipRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            mode = clipOp.toSkia(),
            antiAlias = antiAlias
        )
    }

    @OptIn(InternalComposeUiApi::class)
    override fun clipPath(path: Path, clipOp: ClipOp) {
        val antiAlias = true
        internalSkiaCanvas.clipPath(path.materializeSkiaPath(), clipOp.toSkia(), antiAlias)
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        internalSkiaCanvas.drawLine(
            x0 = p1.x,
            y0 = p1.y,
            x1 = p2.x,
            y1 = p2.y,
            paint = paint.asSkiaPaintWithAppliedAlphaMultiplier(),
        )
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        internalSkiaCanvas.drawRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            paint = paint.asSkiaPaintWithAppliedAlphaMultiplier(),
        )
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
        internalSkiaCanvas.drawRRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            radii = floatArrayOf(radiusX, radiusY),
            paint = paint.asSkiaPaintWithAppliedAlphaMultiplier(),
        )
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        internalSkiaCanvas.drawOval(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            paint = paint.asSkiaPaintWithAppliedAlphaMultiplier(),
        )
    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        internalSkiaCanvas.drawCircle(
            x = center.x,
            y = center.y,
            radius = radius,
            paint = paint.asSkiaPaintWithAppliedAlphaMultiplier(),
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
        internalSkiaCanvas.drawArc(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            includeCenter = useCenter,
            paint = paint.asSkiaPaintWithAppliedAlphaMultiplier(),
        )
    }

    @OptIn(InternalComposeUiApi::class)
    override fun drawPath(path: Path, paint: Paint) {
        internalSkiaCanvas.drawPath(
            path = path.materializeSkiaPath(),
            paint = paint.asSkiaPaintWithAppliedAlphaMultiplier(),
        )
    }

    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) {
        drawImageRect(
            image = image,
            srcLeft = 0f,
            srcTop = 0f,
            srcRight = image.width.toFloat(),
            srcBottom = image.height.toFloat(),
            dstLeft = topLeftOffset.x,
            dstTop = topLeftOffset.y,
            dstRight = topLeftOffset.x + image.width.toFloat(),
            dstBottom = topLeftOffset.y + image.height.toFloat(),
            paint = paint,
        )
    }

    override fun drawImageRect(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        paint: Paint
    ) {
        drawImageRect(
            image = image,
            srcLeft = srcOffset.x.toFloat(),
            srcTop = srcOffset.y.toFloat(),
            srcRight = srcOffset.x.toFloat() + srcSize.width.toFloat(),
            srcBottom = srcOffset.y.toFloat() + srcSize.height.toFloat(),
            dstLeft = dstOffset.x.toFloat(),
            dstTop = dstOffset.y.toFloat(),
            dstRight = dstOffset.x.toFloat() + dstSize.width.toFloat(),
            dstBottom = dstOffset.y.toFloat() + dstSize.height.toFloat(),
            paint = paint,
        )
    }

    // TODO(demin): probably this method should be in the common Canvas
    private fun drawImageRect(
        image: ImageBitmap,
        srcLeft: Float,
        srcTop: Float,
        srcRight: Float,
        srcBottom: Float,
        dstLeft: Float,
        dstTop: Float,
        dstRight: Float,
        dstBottom: Float,
        paint: Paint
    ) {
        val bitmap = image.asSkiaBitmap()

        Image.makeFromBitmap(bitmap).use { skiaImage ->
            internalSkiaCanvas.drawImageRect(
                image = skiaImage,
                srcLeft = srcLeft,
                srcTop = srcTop,
                srcRight = srcRight,
                srcBottom = srcBottom,
                dstLeft = dstLeft,
                dstTop = dstTop,
                dstRight = dstRight,
                dstBottom = dstBottom,
                samplingMode = paint.filterQuality.toSkia(),
                paint = paint.asSkiaPaintWithAppliedAlphaMultiplier(),
                strict = true,
            )
        }
    }

    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
        when (pointMode) {
            // Draw a line between each pair of points, each point has at most one line
            // If the number of points is odd, then the last point is ignored.
            PointMode.Lines -> drawLines(points, paint, 2)

            // Connect each adjacent point with a line
            PointMode.Polygon -> drawLines(points, paint, 1)

            // Draw a point at each provided coordinate
            PointMode.Points -> drawPoints(points, paint)
        }
    }

    override fun enableZ() = Unit

    override fun disableZ() = Unit

    private fun drawPoints(points: List<Offset>, paint: Paint) {
        val skiaPaint = paint.asSkiaPaintWithAppliedAlphaMultiplier()
        points.fastForEach { point ->
            internalSkiaCanvas.drawPoint(
                x = point.x,
                y = point.y,
                paint = skiaPaint,
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
            val skiaPaint = paint.asSkiaPaintWithAppliedAlphaMultiplier()
            var i = 0
            while (i < points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                internalSkiaCanvas.drawLine(p1.x, p1.y, p2.x, p2.y, skiaPaint)
                i += stepBy
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
            PointMode.Lines -> drawRawLines(points, paint, 2)
            PointMode.Polygon -> drawRawLines(points, paint, 1)
            PointMode.Points -> drawRawPoints(points, paint, 2)
        }
    }

    private fun drawRawPoints(points: FloatArray, paint: Paint, stepBy: Int) {
        if (points.size % 2 == 0) {
            val skiaPaint = paint.asSkiaPaintWithAppliedAlphaMultiplier()
            var i = 0
            while (i < points.size - 1) {
                val x = points[i]
                val y = points[i + 1]
                internalSkiaCanvas.drawPoint(x, y, skiaPaint)
                i += stepBy
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
            val skiaPaint = paint.asSkiaPaintWithAppliedAlphaMultiplier()
            var i = 0
            while (i < points.size - 3) {
                val x1 = points[i]
                val y1 = points[i + 1]
                val x2 = points[i + 2]
                val y2 = points[i + 3]
                internalSkiaCanvas.drawLine(x1, y1, x2, y2, skiaPaint)
                i += stepBy * 2
            }
        }
    }

    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {
        internalSkiaCanvas.drawVertices(
            vertexMode = vertices.vertexMode.toSkiaVertexMode(),
            positions = vertices.positions,
            colors = vertices.colors,
            texCoords = vertices.textureCoordinates,
            indices = vertices.indices,
            blendMode = blendMode.toSkia(),
            paint = paint.asSkiaPaintWithAppliedAlphaMultiplier(),
        )
    }

    private fun ClipOp.toSkia() = when (this) {
        ClipOp.Difference -> SkClipMode.DIFFERENCE
        ClipOp.Intersect -> SkClipMode.INTERSECT
        else -> SkClipMode.INTERSECT
    }

    private fun Matrix.toSkia() = Matrix44(
        this[0, 0],
        this[1, 0],
        this[2, 0],
        this[3, 0],

        this[0, 1],
        this[1, 1],
        this[2, 1],
        this[3, 1],

        this[0, 2],
        this[1, 2],
        this[2, 2],
        this[3, 2],

        this[0, 3],
        this[1, 3],
        this[2, 3],
        this[3, 3]
    )

    // These constants are chosen to correspond the old implementation of SkFilterQuality:
    // https://github.com/google/skia/blob/1f193df9b393d50da39570dab77a0bb5d28ec8ef/src/image/SkImage.cpp#L809
    // https://github.com/google/skia/blob/1f193df9b393d50da39570dab77a0bb5d28ec8ef/include/core/SkSamplingOptions.h#L86
    private fun FilterQuality.toSkia(): SamplingMode = when (this) {
        FilterQuality.Low -> FilterMipmap(FilterMode.LINEAR, MipmapMode.NONE)
        FilterQuality.Medium -> FilterMipmap(FilterMode.LINEAR, MipmapMode.NEAREST)
        FilterQuality.High -> CubicResampler(1 / 3.0f, 1 / 3.0f)
        else -> FilterMipmap(FilterMode.NEAREST, MipmapMode.NONE)
    }
}
