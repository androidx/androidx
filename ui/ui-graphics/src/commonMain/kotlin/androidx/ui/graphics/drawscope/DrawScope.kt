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

package androidx.ui.graphics.drawscope

import androidx.ui.util.annotation.FloatRange
import androidx.ui.core.LayoutDirection
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Size
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.ClipOp
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.NativePathEffect
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.graphics.Path
import androidx.ui.graphics.PointMode
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.StrokeJoin
import androidx.ui.graphics.setNativePathEffect
import androidx.ui.graphics.vectormath.degrees
import androidx.ui.unit.Density

/**
 * Simultaneously translate the [DrawScope] coordinate space by [left] and [top] as well as modify
 * the dimensions of the current painting area. This provides a callback to issue more
 * drawing instructions within the modified coordinate space. This method
 * modifies the width of the [DrawScope] to be equivalent to width - (left + right) as well as
 * height to height - (top + bottom)
 *
 * @param left number of pixels to inset the left drawing bound
 * @param top number of pixels to inset the top drawing bound
 * @param right number of pixels to inset the right drawing bound
 * @param bottom number of pixels to inset the bottom drawing bound
 * @param block lambda that is called to issue drawing commands within the inset coordinate space
 */
inline fun DrawScope.inset(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    block: DrawScope.() -> Unit
) = canvas?.apply {
        transform.inset(left, top, right, bottom)
        block()
        transform.inset(-left, -top, -right, -bottom)
    }

/**
 * Convenience method modifies the [DrawScope] bounds to inset both left and right bounds by
 * [dx] as well as the top and bottom by [dy]. If only [dx] is provided, the same inset is applied
 * to all 4 bounds
 *
 * @param dx number of pixels to inset both left and right bounds
 * @param dy Optional number of pixels to inset both top and bottom bounds, by default this also
 * insets the top and bottom by [dx] pixels
 * @param block lambda that is called to issue additional drawing commands within the modified
 * coordinate space
 */
inline fun DrawScope.inset(dx: Float = 0.0f, dy: Float = 0.0f, block: DrawScope.() -> Unit) =
    inset(dx, dy, dx, dy, block)

/**
 * Translate the coordinate space by the given delta in pixels in both the x and y coordinates
 * respectively
 *
 * @param left Pixels to translate the coordinate space in the x-axis
 * @param top Pixels to translate the coordinate space in the y-axis
 * @param block lambda that is called to issue drawing commands within the
 * translated coordinate space
 */
inline fun DrawScope.translate(
    left: Float = 0.0f,
    top: Float = 0.0f,
    block: DrawScope.() -> Unit
) = canvas?.apply {
        translate(left, top)
        block()
        translate(-left, -top)
    }

/**
 *  Add a rotation (in degrees clockwise) to the current transform at the given pivot point.
 *  The pivot coordinate remains unchanged by the rotation transformation. After the provided
 *  lambda is invoked, the rotation transformation is undone.
 *
 *  @param degrees to rotate clockwise
 *  @param pivotX The x-coordinate for the pivot point, defaults to the center of the
 *  coordinate space horizontally
 *  @param pivotY The y-coordinate for the pivot point, defaults to the center of the
 *  coordinate space vertically
 *  @param block lambda that is called to issue drawing commands within the rotated
 *  coordinate space
 */
inline fun DrawScope.rotate(
    degrees: Float,
    pivotX: Float = center.dx,
    pivotY: Float = center.dy,
    block: DrawScope.() -> Unit
) = withTransform({ rotate(degrees, pivotX, pivotY) }, block)

/**
 * Add a rotation (in radians clockwise) to the current transform at the given pivot point.
 * The pivot coordinate remains unchanged by the rotation transformation
 *
 * @param radians to rotate clockwise
 *  @param pivotX The x-coordinate for the pivot point, defaults to the center of the
 *  coordinate space horizontally
 *  @param pivotY The y-coordinate for the pivot point, defaults to the center of the
 *  coordinate space vertically
 * @param block lambda that is called to issue drawing commands within the rotated
 * coordinate space
 */
inline fun DrawScope.rotateRad(
    radians: Float,
    pivotX: Float = center.dx,
    pivotY: Float = center.dy,
    block: DrawScope.() -> Unit
) = withTransform({ rotate(degrees(radians), pivotX, pivotY) }, block)

/**
 * Add an axis-aligned scale to the current transform, scaling by the first
 * argument in the horizontal direction and the second in the vertical
 * direction at the given pivot coordinate. The pivot coordinate remains
 * unchanged by the scale transformation.
 *
 * If [scaleY] is unspecified, [scaleX] will be used for the scale in both
 * directions.
 *
 * @param scaleX The amount to scale in X
 * @param scaleY The amount to scale in Y
 * @param pivotX The x-coordinate for the pivot point, defaults to the center of the
 * coordinate space horizontally
 * @param pivotY The y-coordinate for the pivot point, defaults to the center of the
 * coordinate space vertically
 * @param block lambda used to issue drawing commands within the scaled coordinate space
 */
inline fun DrawScope.scale(
    scaleX: Float,
    scaleY: Float = scaleX,
    pivotX: Float = center.dx,
    pivotY: Float = center.dy,
    block: DrawScope.() -> Unit
) = withTransform({ scale(scaleX, scaleY, pivotX, pivotY) }, block)

/**
 * Reduces the clip region to the intersection of the current clip and the
 * given rectangle indicated by the given left, top, right and bottom bounds.
 *
 * Use [ClipOp.difference] to subtract the provided rectangle from the
 * current clip.
 *
 * @param left Left bound of the rectangle to clip
 * @param top Top bound of the rectangle to clip
 * @param right Right bound ofthe rectangle to clip
 * @param bottom Bottom bound of the rectangle to clip
 * @param clipOp Clipping operation to conduct on the given bounds, defaults to [ClipOp.intersect]
 * @param block Lambda callback with this CanvasScope as a receiver scope to issue drawing commands
 * within the provided clip
 */
inline fun DrawScope.clipRect(
    left: Float = 0.0f,
    top: Float = 0.0f,
    right: Float = size.width,
    bottom: Float = size.height,
    clipOp: ClipOp = ClipOp.intersect,
    block: DrawScope.() -> Unit
) = withTransform({ clipRect(left, top, right, bottom, clipOp) }, block)

/**
 * Reduces the clip region to the intersection of the current clip and the
 * given rounded rectangle.
 *
 * @param path Shape to clip drawing content within
 * @param clipOp Clipping operation to conduct on the given bounds, defaults to [ClipOp.intersect]
 * @param block Lambda callback with this CanvasScope as a receiver scope to issue drawing commands
 * within the provided clip
 */
inline fun DrawScope.clipPath(
    path: Path,
    clipOp: ClipOp = ClipOp.intersect,
    block: DrawScope.() -> Unit
) = withTransform({ clipPath(path, clipOp) }, block)

/**
 * Provides access to draw directly with the underlying [Canvas] along with the current
 * size of the [DrawScope]. This is helpful for situations
 * to re-use alternative drawing logic in combination with [DrawScope]
 *
 * @param block Lambda callback to issue drawing commands on the provided [Canvas] and given size
 */
inline fun DrawScope.drawCanvas(block: (Canvas, Size) -> Unit) =
    canvas?.let {
        block(it, size)
    }

/**
 * Perform 1 or more transformations and execute drawing commands with the specified transformations
 * applied. After this call is complete, the transformation before this call was made is restored
 *
 * @sample androidx.ui.graphics.samples.canvasScopeBatchedTransformSample
 *
 * @param transformBlock Callback invoked to issue transformations to be made before the drawing
 * operations are issued
 * @param drawBlock Callback invoked to issue drawing operations after the transformations are
 * applied
 */
inline fun DrawScope.withTransform(
    transformBlock: CanvasTransform.() -> Unit,
    drawBlock: DrawScope.() -> Unit
) = canvas?.apply {
        // Transformation can include inset calls which change the drawing area
        // so cache the previous size before the transformation is done
        // and reset it afterwards
        val previousSize = size
        save()
        transformBlock(transform)
        drawBlock()
        restore()
        setSize(previousSize)
    }

/**
 * Creates a scoped drawing environment with the provided [Canvas]. This provides a
 * declarative, stateless API to draw shapes and paths without requiring
 * consumers to maintain underlying [Canvas] state information.
 * The bounds for drawing within [DrawScope] are provided by the call to
 * [DrawScope.draw] and are always bound to the local translation. That is the left and
 * top coordinates are always the origin and the right and bottom coordinates are always the
 * specified width and height respectively. Drawing content is not clipped,
 * so it is possible to draw outside of the specified bounds.
 *
 * @sample androidx.ui.graphics.samples.canvasScopeSample
 */
@DrawScopeMarker
abstract class DrawScope : Density {

    @PublishedApi internal var canvas: Canvas? = null

    @PublishedApi internal val transform = object : CanvasTransform {

        override val size: Size
            get() = this@DrawScope.size

        override val center: Offset
            get() = this@DrawScope.center

        override fun inset(left: Float, top: Float, right: Float, bottom: Float) {
            this@DrawScope.canvas?.let {
                val updatedSize = size - Offset(left + right, top + bottom)
                require(updatedSize.width > 0 && updatedSize.height > 0) {
                    "Width and height must be greater than zero"
                }
                this@DrawScope.setSize(updatedSize)
                it.translate(left, top)
            }
        }

        override fun clipRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            clipOp: ClipOp
        ) {
            this@DrawScope.canvas?.clipRect(left, top, right, bottom, clipOp)
        }

        override fun clipPath(path: Path, clipOp: ClipOp) {
            this@DrawScope.canvas?.clipPath(path, clipOp)
        }

        override fun translate(left: Float, top: Float) {
            this@DrawScope.canvas?.translate(left, top)
        }

        override fun rotate(degrees: Float, pivotX: Float, pivotY: Float) {
            this@DrawScope.canvas?.apply {
                translate(pivotX, pivotY)
                rotate(degrees)
                translate(-pivotX, -pivotY)
            }
        }

        override fun scale(scaleX: Float, scaleY: Float, pivotX: Float, pivotY: Float) {
            this@DrawScope.canvas?.apply {
                translate(pivotX, pivotY)
                scale(scaleX, scaleY)
                translate(-pivotX, -pivotY)
            }
        }
    }

    /**
     * Internal [Paint] used only for drawing filled in shapes with a color or gradient
     * This is lazily allocated on the first drawing command that uses the [Fill] [DrawStyle]
     * and re-used across subsequent calls
     */
    private val fillPaint: Paint by lazy(LazyThreadSafetyMode.NONE) {
        Paint().apply { style = PaintingStyle.fill }
    }

    /**
     * Internal [Paint] used only for drawing stroked shapes with a color or gradient
     * This is lazily allocated on the first drawing command that uses the [Stroke] [DrawStyle]
     * and re-used across subsequent calls
     */
    private val strokePaint: Paint by lazy(LazyThreadSafetyMode.NONE) {
        Paint().apply { style = PaintingStyle.stroke }
    }

    /**
     * Center of the current bounds of the drawing environment
     */
    val center: Offset
        get() = Offset(size.width / 2, size.height / 2)

    /**
     * Provides the dimensions of the current drawing environment
     */
    var size: Size = Size.zero
        private set

    /**
     * The layout direction of the layout being drawn in.
     */
    abstract val layoutDirection: LayoutDirection

    /**
     * Draws a line between the given points using the given paint. The line is
     * stroked.
     *
     * @param brush: the color or fill to be applied to the line
     * @param p1: First point of the line to be drawn
     * @param p2: Second point of the line to be drawn
     * @param stroke: The stroke parameters to apply to the line
     * @param alpha: opacity to be applied to the [brush] from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param colorFilter: ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode: the blending algorithm to apply to the [brush]
     */
    fun drawLine(
        brush: Brush,
        p1: Offset,
        p2: Offset,
        stroke: Stroke,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawLine(
            p1,
            p2,
            configurePaint(brush, stroke, alpha, colorFilter, blendMode)
        )

    /**
     * Draws a line between the given points using the given paint. The line is
     * stroked.
     *
     * @param color: the color to be applied to the line
     * @param p1: First point of the line to be drawn
     * @param p2: Second point of the line to be drawn
     * @param stroke: The stroke parameters to apply to the line
     * @param alpha: opacity to be applied to the [color] from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param colorFilter: ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode: the blending algorithm to apply to the [color]
     */
    fun drawLine(
        color: Color,
        p1: Offset,
        p2: Offset,
        stroke: Stroke,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawLine(
            p1,
            p2,
            configurePaint(color, stroke, alpha, colorFilter, blendMode)
        )

    /**
     * Draws a rectangle with the given offset and size. If no offset from the top left is provided,
     * it is drawn starting from the origin of the current translation. If no size is provided,
     * the size of the current environment is used.
     *
     * @param brush: The color or fill to be applied to the rectangle
     * @param topLeft: Offset from the local origin of 0, 0 relative to the current translation
     * @param size: Dimensions of the rectangle to draw
     * @param alpha: Opacity to be applied to the [brush] from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style: Whether or not the rectangle is stroked or filled in
     * @param colorFilter: ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode: Blending algorithm to apply to destination
     */
    fun drawRect(
        brush: Brush,
        topLeft: Offset = Offset.zero,
        size: Size = this.size,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawRect(
            left = topLeft.dx,
            top = topLeft.dy,
            right = topLeft.dx + size.width,
            bottom = topLeft.dy + size.height,
            paint = configurePaint(brush, style, alpha, colorFilter, blendMode)
        )

    /**
     * Draws a rectangle with the given offset and size. If no offset from the top left is provided,
     * it is drawn starting from the origin of the current translation. If no size is provided,
     * the size of the current environment is used.
     *
     * @param color: The color to be applied to the rectangle
     * @param topLeft: Offset from the local origin of 0, 0 relative to the current translation
     * @param size: Dimensions of the rectangle to draw
     * @param alpha: Opacity to be applied to the [color] from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style: Whether or not the rectangle is stroked or filled in
     * @param colorFilter: ColorFilter to apply to the [color] source pixels
     * @param blendMode: Blending algorithm to apply to destination
     */
    fun drawRect(
        color: Color,
        topLeft: Offset = Offset.zero,
        size: Size = this.size,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawRect(
            left = topLeft.dx,
            top = topLeft.dy,
            right = topLeft.dx + size.width,
            bottom = topLeft.dy + size.height,
            paint = configurePaint(color, style, alpha, colorFilter, blendMode)
        )

    /**
     * Draws the given [ImageAsset] into the canvas with its top-left corner at the
     * given [Offset]. The image is composited into the canvas using the given [Paint].
     *
     * @param image The [ImageAsset] to draw
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param alpha Opacity to be applied to [image] from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Specifies whether the image is to be drawn filled in or as a rectangular stroke
     * @param colorFilter: ColorFilter to apply to the [image] when drawn into the destination
     * @param blendMode: Blending algorithm to apply to destination
     */
    fun drawImage(
        image: ImageAsset,
        topLeft: Offset = Offset.zero,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawImage(
            image,
            topLeft,
            configurePaint(null, style, alpha, colorFilter, blendMode)
        )

    /**
     * Draws the subset of the given image described by the `src` argument into
     * the canvas in the axis-aligned rectangle given by the `dst` argument.
     *
     * If no src rect is provided, the entire image is scaled into the corresponding destination
     * bounds
     *
     * @param image: The source image to draw
     * @param srcOffset: Optional offset representing the top left offset of the source image
     * to draw, this defaults to the origin of [image]
     * @param srcSize: Optional dimensions of the source image to draw relative to [srcOffset],
     * this defaults the width and height of [image]
     * @param dstOffset: Optional offset representing the top left offset of the destination
     * to draw the given image, this defaults to the origin of the current translation
     * tarting top left offset in the destination to draw the image
     * @param dstSize: Optional dimensions of the destination to draw, this defaults to the size
     * of the current drawing environment
     * @param alpha Opacity to be applied to [image] from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Specifies whether the image is to be drawn filled in or as a rectangular stroke
     * @param colorFilter: ColorFilter to apply to the [image] when drawn into the destination
     * @param blendMode: Blending algorithm to apply to destination
     */
    fun drawImage(
        image: ImageAsset,
        srcOffset: Offset = Offset.zero,
        srcSize: Size = Size(image.width.toFloat(), image.height.toFloat()),
        dstOffset: Offset = Offset.zero,
        dstSize: Size = this.size,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawImageRect(
            image,
            srcOffset,
            srcSize,
            dstOffset,
            dstSize,
            configurePaint(null, style, alpha, colorFilter, blendMode)
        )

    /**
     * Draws a rounded rectangle with the provided size, offset and radii for the x and y axis
     * respectively. This rectangle is drawn with the provided [Brush]
     * parameter and is filled or stroked based on the given [DrawStyle]
     *
     * @param brush The color or fill to be applied to the rounded rectangle
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param radiusX Corner radius of the rounded rectangle along the x-axis
     * @param radiusY Corner radius of the rounded rectangle along the y-axis
     * @param alpha Opacity to be applied to rounded rectangle from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Specifies whether the rounded rectangle is stroked or filled in
     * @param colorFilter: ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode: Blending algorithm to be applied to the brush
     */
    fun drawRoundRect(
        brush: Brush,
        topLeft: Offset = Offset.zero,
        size: Size = this.size,
        radiusX: Float = 0.0f,
        radiusY: Float = 0.0f,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawRoundRect(
            topLeft.dx,
            topLeft.dy,
            topLeft.dx + size.width,
            topLeft.dy + size.height,
            radiusX,
            radiusY,
            configurePaint(brush, style, alpha, colorFilter, blendMode)
        )

    /**
     * Draws a rounded rectangle with the given [Paint]. Whether the rectangle is
     * filled or stroked (or both) is controlled by [Paint.style].
     *
     * @param color The color to be applied to the rounded rectangle
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param radiusX Corner radius of the rounded rectangle along the x-axis
     * @param radiusY Corner radius of the rounded rectangle along the y-axis
     * @param alpha Opacity to be applied to rounded rectangle from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Specifies whether the rounded rectangle is stroked or filled in
     * @param colorFilter: ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode: Blending algorithm to be applied to the color
     */
    fun drawRoundRect(
        color: Color,
        topLeft: Offset = Offset.zero,
        size: Size = this.size,
        radiusX: Float = 0.0f,
        radiusY: Float = 0.0f,
        style: DrawStyle = Fill,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawRoundRect(
            topLeft.dx,
            topLeft.dy,
            topLeft.dx + size.width,
            topLeft.dy + size.height,
            radiusX,
            radiusY,
            configurePaint(color, style, alpha, colorFilter, blendMode)
        )

    /**
     * Draws a circle at the provided center coordinate and radius. If no center point is provided
     * the center of the bounds is used.
     *
     * @param brush: The color or fill to be applied to the circle
     * @param radius: The radius of the circle
     * @param center: The center coordinate where the circle is to be drawn
     * @param alpha: Opacity to be applied to the circle from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style: Whether or not the circle is stroked or filled in
     * @param colorFilter: ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode: Blending algorithm to be applied to the brush
     */
    fun drawCircle(
        brush: Brush,
        radius: Float = size.minDimension / 2.0f,
        center: Offset = this.center,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawCircle(
            center,
            radius,
            configurePaint(brush, style, alpha, colorFilter, blendMode)
        )

    /**
     * Draws a circle at the provided center coordinate and radius. If no center point is provided
     * the center of the bounds is used.
     *
     * @param color: The color or fill to be applied to the circle
     * @param radius: The radius of the circle
     * @param center: The center coordinate where the circle is to be drawn
     * @param alpha: Opacity to be applied to the circle from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style: Whether or not the circle is stroked or filled in
     * @param colorFilter: ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode: Blending algorithm to be applied to the brush
     */
    fun drawCircle(
        color: Color,
        radius: Float = size.minDimension / 2.0f,
        center: Offset = this.center,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawCircle(
            center,
            radius,
            configurePaint(color, style, alpha, colorFilter, blendMode)
        )

    /**
     * Draws an oval with the given offset and size. If no offset from the top left is provided,
     * it is drawn starting from the origin of the current translation. If no size is provided,
     * the size of the current environment is used.
     *
     * @param brush: Color or fill to be applied to the oval
     * @param topLeft: Offset from the local origin of 0, 0 relative to the current translation
     * @param size: Dimensions of the rectangle to draw
     * @param alpha: Opacity to be applied to the oval from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style: Whether or not the oval is stroked or filled in
     * @param colorFilter: ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode: Blending algorithm to be applied to the brush
     */
    fun drawOval(
        brush: Brush,
        topLeft: Offset = Offset.zero,
        size: Size = this.size,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawOval(
            left = topLeft.dx,
            top = topLeft.dy,
            right = topLeft.dx + size.width,
            bottom = topLeft.dy + size.height,
            paint = configurePaint(brush, style, alpha, colorFilter, blendMode)
        )

    /**
     * Draws an oval with the given offset and size. If no offset from the top left is provided,
     * it is drawn starting from the origin of the current translation. If no size is provided,
     * the size of the current environment is used.
     *
     * @param color: Color to be applied to the oval
     * @param topLeft: Offset from the local origin of 0, 0 relative to the current translation
     * @param size: Dimensions of the rectangle to draw
     * @param alpha: Opacity to be applied to the oval from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style: Whether or not the oval is stroked or filled in
     * @param colorFilter: ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode: Blending algorithm to be applied to the brush
     */
    fun drawOval(
        color: Color,
        topLeft: Offset = Offset.zero,
        size: Size = this.size,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawOval(
            left = topLeft.dx,
            top = topLeft.dy,
            right = topLeft.dx + size.width,
            bottom = topLeft.dy + size.height,
            paint = configurePaint(color, style, alpha, colorFilter, blendMode)
        )

    /**
     * Draw an arc scaled to fit inside the given rectangle. It starts from
     * startAngle degrees around the oval up to startAngle + sweepAngle
     * degrees around the oval, with zero degrees being the point on
     * the right hand side of the oval that crosses the horizontal line
     * that intersects the center of the rectangle and with positive
     * angles going clockwise around the oval. If useCenter is true, the arc is
     * closed back to the center, forming a circle sector. Otherwise, the arc is
     * not closed, forming a circle segment.
     *
     * @param brush: Color or fill to be applied to the arc
     * @param topLeft: Offset from the local origin of 0, 0 relative to the current translation
     * @param size: Dimensions of the arc to draw
     * @param startAngle: Starting angle in degrees. 0 represents 3 o'clock
     * @param sweepAngle: Size of the arc in degrees that is drawn at the position provided in
     * [startAngle]
     * @param useCenter: Flag indicating if the arc is to close the center of the bounds
     * @param alpha: Opacity to be applied to the arc from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style: Whether or not the arc is stroked or filled in
     * @param colorFilter: ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode: Blending algorithm to be applied to the arc when it is drawn
     */
    fun drawArc(
        brush: Brush,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset = Offset.zero,
        size: Size = this.size,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawArc(
            left = topLeft.dx,
            top = topLeft.dy,
            right = topLeft.dx + size.width,
            bottom = topLeft.dy + size.height,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = useCenter,
            paint = configurePaint(brush, style, alpha, colorFilter, blendMode)
        )

    /**
     * Draw an arc scaled to fit inside the given rectangle. It starts from
     * startAngle degrees around the oval up to startAngle + sweepAngle
     * degrees around the oval, with zero degrees being the point on
     * the right hand side of the oval that crosses the horizontal line
     * that intersects the center of the rectangle and with positive
     * angles going clockwise around the oval. If useCenter is true, the arc is
     * closed back to the center, forming a circle sector. Otherwise, the arc is
     * not closed, forming a circle segment.
     *
     * @param color: Color to be applied to the arc
     * @param topLeft: Offset from the local origin of 0, 0 relative to the current translation
     * @param size: Dimensions of the arc to draw
     * @param startAngle: Starting angle in degrees. 0 represents 3 o'clock
     * @param sweepAngle: Size of the arc in degrees that is drawn at the position provided in
     * [startAngle]
     * @param useCenter: Flag indicating if the arc is to close the center of the bounds
     * @param alpha: Opacity to be applied to the arc from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style: Whether or not the arc is stroked or filled in
     * @param colorFilter: ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode: Blending algorithm to be applied to the arc when it is drawn
     */
    fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset = Offset.zero,
        size: Size = this.size,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawArc(
        left = topLeft.dx,
        top = topLeft.dy,
        right = topLeft.dx + size.width,
        bottom = topLeft.dy + size.height,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = useCenter,
        paint = configurePaint(color, style, alpha, colorFilter, blendMode)
    )

    /**
     * Draws the given [Path] with the given [Color]. Whether this shape is
     * filled or stroked (or both) is controlled by [DrawStyle]. If the path is
     * filled, then subpaths within it are implicitly closed (see [Path.close]).
     *
     *
     * @param path: Path to draw
     * @param color: Color to be applied to the path
     * @param alpha: Opacity to be applied to the path from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style: Whether or not the path is stroked or filled in
     * @param colorFilter: ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode: Blending algorithm to be applied to the path when it is drawn
     */
    fun drawPath(
        path: Path,
        color: Color,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawPath(path, configurePaint(color, style, alpha, colorFilter, blendMode))

    /**
     * Draws the given [Path] with the given [Color]. Whether this shape is
     * filled or stroked (or both) is controlled by [DrawStyle]. If the path is
     * filled, then subpaths within it are implicitly closed (see [Path.close]).
     *
     * @param path: Path to draw
     * @param brush: Brush to be applied to the path
     * @param alpha: Opacity to be applied to the path from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style: Whether or not the path is stroked or filled in
     * @param colorFilter: ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode: Blending algorithm to be applied to the path when it is drawn
     */
    fun drawPath(
        path: Path,
        brush: Brush,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawPath(path, configurePaint(brush, style, alpha, colorFilter, blendMode))

    /**
     * Draws a sequence of points according to the given [PointMode].
     *
     * The `points` argument is interpreted as offsets from the origin.
     *
     * @param points: List of points to draw with the specified [PointMode]
     * @param pointMode: [PointMode] used to indicate how the points are to be drawn
     * @param color: Color to be applied to the points
     * @param alpha: Opacity to be applied to the path from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param stroke: The stroke parameters to apply to the points
     * @param colorFilter: ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode: Blending algorithm to be applied to the path when it is drawn
     */
    fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        color: Color,
        stroke: Stroke,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawPoints(
            pointMode,
            points,
            configurePaint(color, stroke, alpha, colorFilter,
            blendMode)
        )

    /**
     * Draws a sequence of points according to the given [PointMode].
     *
     * The `points` argument is interpreted as offsets from the origin.
     *
     * @param points: List of points to draw with the specified [PointMode]
     * @param pointMode: [PointMode] used to indicate how the points are to be drawn
     * @param brush: Brush to be applied to the points
     * @param alpha: Opacity to be applied to the path from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style: Whether or not the path is stroked or filled in
     * @param colorFilter: ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode: Blending algorithm to be applied to the path when it is drawn
     */
    fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        brush: Brush,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas?.drawPoints(
        pointMode,
        points,
        configurePaint(brush, style, alpha, colorFilter,
            blendMode)
    )

    /**
     * Draws into the provided [Canvas] with the commands specified in the lambda with this
     * [DrawScope] as a receiver
     *
     * @param canvas target canvas to render into
     * @param size bounds relative to the current canvas translation in which the [DrawScope]
     * should draw within
     * @param block lambda that is called to issue drawing commands on this [DrawScope]
     */
    fun draw(canvas: Canvas, size: Size, block: DrawScope.() -> Unit) {
        val previousSize = this.size
        // Remember the previous canvas in case we are temporarily re-directing our drawing
        // to a separate Layer/RenderNode only to draw that content back into the original Canvas
        // If there is no previous canvas that was being drawin into, this ends up reseting this
        // parameter back to null defensively
        val previousCanvas = this.canvas
        this.canvas = canvas
        setSize(size)
        canvas.save()
        this.block()
        canvas.restore()
        setSize(previousSize)
        this.canvas = previousCanvas
    }

    /**
     * Internal published APIs used to support inline scoped extension methods
     * on CanvasScope directly, without exposing the underlying stateful APIs
     * to conduct the transformations themselves as inline methods require
     * all methods called within them to be public
     */

    /**
     * Configures the current size of the drawing environment, this is configured as part of
     * the [draw] call
     */
    @PublishedApi
    internal fun setSize(size: Size) {
        this.size = size
    }

    /**
     * Selects the appropriate [Paint] object based on the style
     * and applies the underlying [DrawStyle] parameters
     */
    private fun selectPaint(drawStyle: DrawStyle): Paint =
        when (drawStyle) {
            Fill -> fillPaint
            is Stroke -> strokePaint.apply {
                with(drawStyle) {
                    strokeWidth = width
                    strokeCap = cap
                    strokeMiterLimit = miter
                    strokeJoin = join

                    // TODO b/154550525 add PathEffect to Paint if necessary
                    asFrameworkPaint().setNativePathEffect(pathEffect)
                }
            }
        }

    /**
     * Helper method to configure the corresponding [Brush] along with other properties
     * on the corresponding paint specified by [DrawStyle]
     */
    private fun configurePaint(
        brush: Brush?,
        style: DrawStyle,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ): Paint = selectPaint(style).apply {
        if (brush != null) {
            brush.applyTo(this, alpha)
        } else if (this.alpha != alpha) {
            this.alpha = alpha
        }
        if (this.colorFilter != colorFilter) this.colorFilter = colorFilter
        if (this.blendMode != blendMode) this.blendMode = blendMode
    }

    /**
     * Helper method to configure the corresponding [Color] along with other properties
     * on the corresponding paint specified by [DrawStyle]
     */
    private fun configurePaint(
        color: Color,
        style: DrawStyle,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ): Paint = selectPaint(style).apply {
        // Modulate the color alpha directly
        // instead of configuring a separate alpha parameter
        val targetColor = color.modulate(alpha)
        if (this.color != targetColor) this.color = targetColor
        if (this.shader != null) this.shader = null
        if (this.colorFilter != colorFilter) this.colorFilter = colorFilter
        if (this.blendMode != blendMode) this.blendMode = blendMode
    }

    /**
     * Returns a [Color] modulated with the given alpha value
     */
    private fun Color.modulate(alpha: Float): Color =
        if (alpha != 1.0f) {
            copy(alpha = this.alpha * alpha)
        } else {
            this
        }

    companion object {
        /**
         * Default alpha value used for each drawing operation
         * This represents a fully opaque drawing operation.
         *
         * Note color values that have their own alpha value
         * will draw with some transparency even if this value
         * is used
         */
        const val DefaultAlpha: Float = 1.0f

        /**
         * Default blending mode used for each drawing operation.
         * This ensures that content is drawn on top of the pixels
         * in the destination
         */
        val DefaultBlendMode: BlendMode = BlendMode.srcOver
    }
}

/**
 * Represents how the shapes should be drawn within a [DrawScope]
 */
sealed class DrawStyle

/**
 * Default [DrawStyle] indicating shapes should be drawn completely filled in with the
 * provided color or pattern
 */
object Fill : DrawStyle()

/**
 * [DrawStyle] that provides information for drawing content with a stroke
 */
data class Stroke(
    /**
     * Configure the width of the stroke in pixels
     */
    val width: Float = 0.0f,

    /**
     * Set the paint's stroke miter value. This is used to control the behavior of miter
     * joins when the joins angle si sharp. This value must be >= 0.
     */
    val miter: Float = 4.0f,

    /**
     * Return the paint's Cap, controlling how the start and end of stroked
     * lines and paths are treated. The default is [StrokeCap.butt]
     */
    val cap: StrokeCap = StrokeCap.butt,

    /**
     * Set's the treatment where lines and curve segments join on a stroked path.
     * The default is [StrokeJoin.miter]
     */
    val join: StrokeJoin = StrokeJoin.miter,

    /**
     * Effect to apply to the stroke, null indicates a solid stroke line is to be drawn
     */
    val pathEffect: NativePathEffect? = null
) : DrawStyle()