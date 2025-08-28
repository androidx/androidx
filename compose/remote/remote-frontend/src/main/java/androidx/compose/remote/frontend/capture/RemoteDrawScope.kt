/*
 * Copyright (C) 2023 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.capture

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.remote.frontend.capture.shaders.RemoteBrush
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.remote.frontend.state.RemoteString
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScopeMarker
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * This is used to intercept the DrawScope
 *
 * @param left number of pixels to inset the left drawing bound
 * @param top number of pixels to inset the top drawing bound
 * @param right number of pixels to inset the right drawing bound
 * @param bottom number of pixels to inset the bottom drawing bound
 * @param block lambda that is called to issue drawing commands within the inset coordinate space
 */
inline fun RemoteDrawScope.inset(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    block: RemoteDrawScope.() -> Unit,
) {
    drawContext.transform.inset(left, top, right, bottom)
    block()
    drawContext.transform.inset(-left, -top, -right, -bottom)
}

/**
 * Convenience method modifies the [RemoteDrawScope] bounds to inset both left, top, right and
 * bottom bounds by [inset]. After this method is invoked, the coordinate space is returned to the
 * state before this inset was applied.
 *
 * @param inset number of pixels to inset left, top, right, and bottom bounds.
 * @param block lambda that is called to issue additional drawing commands within the modified
 *   coordinate space
 */
inline fun RemoteDrawScope.inset(inset: Float, block: RemoteDrawScope.() -> Unit) {
    drawContext.transform.inset(inset, inset, inset, inset)
    block()
    drawContext.transform.inset(-inset, -inset, -inset, -inset)
}

/**
 * Convenience method modifies the [RemoteDrawScope] bounds to inset both left and right bounds by
 * [horizontal] as well as the top and bottom by [vertical]. After this method is invoked, the
 * coordinate space is returned to the state before this inset was applied.
 *
 * @param horizontal number of pixels to inset both left and right bounds. Zero by default
 * @param vertical Optional number of pixels to inset both top and bottom bounds. Zero by default
 * @param block lambda that is called to issue additional drawing commands within the modified
 *   coordinate space
 */
inline fun RemoteDrawScope.inset(
    horizontal: Float = 0.0f,
    vertical: Float = 0.0f,
    block: RemoteDrawScope.() -> Unit,
) = inset(horizontal, vertical, horizontal, vertical, block)

/**
 * Translate the coordinate space by the given delta in pixels in both the x and y coordinates
 * respectively
 *
 * @param left Pixels to translate the coordinate space in the x-axis
 * @param top Pixels to translate the coordinate space in the y-axis
 * @param block lambda that is called to issue drawing commands within the translated coordinate
 *   space
 */
inline fun RemoteDrawScope.translate(
    left: Float = 0.0f,
    top: Float = 0.0f,
    block: RemoteDrawScope.() -> Unit,
) = withTransform({ translate(left, top) }, block)

/**
 * Add a rotation (in degrees clockwise) to the current transform at the given pivot point. The
 * pivot coordinate remains unchanged by the rotation transformation. After the provided lambda is
 * invoked, the rotation transformation is undone.
 *
 * @param degrees to rotate clockwise
 * @param pivot The coordinate for the pivot point, defaults to the center of the coordinate space
 * @param block lambda that is called to issue drawing commands within the rotated coordinate space
 */
inline fun RemoteDrawScope.rotate(
    degrees: Float,
    pivot: Offset = center,
    block: RemoteDrawScope.() -> Unit,
) = withTransform({ rotate(degrees, pivot) }, block)

/**
 * Add a rotation (in radians clockwise) to the current transform at the given pivot point. The
 * pivot coordinate remains unchanged by the rotation transformation
 *
 * @param radians to rotate clockwise
 * @param pivot The coordinate for the pivot point, defaults to the center of the coordinate space
 * @param block lambda that is called to issue drawing commands within the rotated coordinate space
 */
inline fun RemoteDrawScope.rotateRad(
    radians: Float,
    pivot: Offset = center,
    block: RemoteDrawScope.() -> Unit,
) {
    //    withTransform({ rotate(degrees(radians), pivot) }, block)
}

/**
 * Add an axis-aligned scale to the current transform, scaling by the first argument in the
 * horizontal direction and the second in the vertical direction at the given pivot coordinate. The
 * pivot coordinate remains unchanged by the scale transformation. After this method is invoked, the
 * coordinate space is returned to the state before the scale was applied.
 *
 * @param scaleX The amount to scale in X
 * @param scaleY The amount to scale in Y
 * @param pivot The coordinate for the pivot point, defaults to the center of the coordinate space
 * @param block lambda used to issue drawing commands within the scaled coordinate space
 */
inline fun RemoteDrawScope.scale(
    scaleX: Float,
    scaleY: Float,
    pivot: Offset = center,
    block: RemoteDrawScope.() -> Unit,
) = withTransform({ scale(scaleX, scaleY, pivot) }, block)

/**
 * Add an axis-aligned scale to the current transform, scaling both the horizontal direction and the
 * vertical direction at the given pivot coordinate. The pivot coordinate remains unchanged by the
 * scale transformation. After this method is invoked, the coordinate space is returned to the state
 * before the scale was applied.
 *
 * @param scale The amount to scale uniformly in both directions
 * @param pivot The coordinate for the pivot point, defaults to the center of the coordinate space
 * @param block lambda used to issue drawing commands within the scaled coordinate space
 */
inline fun RemoteDrawScope.scale(
    scale: Float,
    pivot: Offset = center,
    block: RemoteDrawScope.() -> Unit,
) = withTransform({ scale(scale, scale, pivot) }, block)

/**
 * Reduces the clip region to the intersection of the current clip and the given rectangle indicated
 * by the given left, top, right and bottom bounds. This provides a callback to issue drawing
 * commands within the clipped region. After this method is invoked, this clip is no longer applied.
 *
 * Use [ClipOp.Difference] to subtract the provided rectangle from the current clip.
 *
 * @param left Left bound of the rectangle to clip
 * @param top Top bound of the rectangle to clip
 * @param right Right bound of the rectangle to clip
 * @param bottom Bottom bound of the rectangle to clip
 * @param clipOp Clipping operation to conduct on the given bounds, defaults to [ClipOp.Intersect]
 * @param block Lambda callback with this CanvasScope as a receiver scope to issue drawing commands
 *   within the provided clip
 */
inline fun RemoteDrawScope.clipRect(
    left: Float = 0.0f,
    top: Float = 0.0f,
    right: Float = size.width,
    bottom: Float = size.height,
    clipOp: ClipOp = ClipOp.Intersect,
    block: RemoteDrawScope.() -> Unit,
) = withTransform({ clipRect(left, top, right, bottom, clipOp) }, block)

/**
 * Reduces the clip region to the intersection of the current clip and the given path. This method
 * provides a callback to issue drawing commands within the region defined by the clipped path.
 * After this method is invoked, this clip is no longer applied.
 *
 * @param path Shape to clip drawing content within
 * @param clipOp Clipping operation to conduct on the given bounds, defaults to [ClipOp.Intersect]
 * @param block Lambda callback with this CanvasScope as a receiver scope to issue drawing commands
 *   within the provided clip
 */
inline fun RemoteDrawScope.clipPath(
    path: Path,
    clipOp: ClipOp = ClipOp.Intersect,
    block: RemoteDrawScope.() -> Unit,
) = withTransform({ clipPath(path, clipOp) }, block)

/**
 * Provides access to draw directly with the underlying [Canvas]. This is helpful for situations to
 * re-use alternative drawing logic in combination with [RemoteDrawScope]
 *
 * @param block Lambda callback to issue drawing commands on the provided [Canvas]
 */
inline fun RemoteDrawScope.drawIntoCanvas(block: (Canvas) -> Unit) = block(drawContext.canvas)

/**
 * Perform 1 or more transformations and execute drawing commands with the specified transformations
 * applied. After this call is complete, the transformation before this call was made is restored
 *
 * @param transformBlock Callback invoked to issue transformations to be made before the drawing
 *   operations are issued
 * @param drawBlock Callback invoked to issue drawing operations after the transformations are
 *   applied
 * @sample androidx.compose.ui.graphics.samples.MyDrawScopeBatchedTransformSample
 */
inline fun RemoteDrawScope.withTransform(
    transformBlock: DrawTransform.() -> Unit,
    drawBlock: RemoteDrawScope.() -> Unit,
) =
    with(drawContext) {
        // Transformation can include inset calls which change the drawing area
        // so cache the previous size before the transformation is done
        // and reset it afterwards
        val previousSize = size
        canvas.save()
        transformBlock(transform)
        drawBlock()
        canvas.restore()
        size = previousSize
    }

/**
 * Creates a scoped drawing environment with the provided [Canvas]. This provides a declarative,
 * stateless API to draw shapes and paths without requiring consumers to maintain underlying
 * [Canvas] state information. [RemoteDrawScope] implementations are also provided sizing
 * information and transformations are done relative to the local translation. That is left and top
 * coordinates are always the origin and the right and bottom coordinates are always the specified
 * width and height respectively. Drawing content is not clipped, so it is possible to draw outside
 * of the specified bounds.
 *
 * @sample androidx.compose.ui.graphics.samples.DrawScopeSample
 */
@DrawScopeMarker
// @JvmDefaultWithCompatibility
interface RemoteDrawScope : Density {

    /**
     * The current [DrawContext] that contains the dependencies needed to create the drawing
     * environment
     */
    val drawContext: DrawContext

    /** Center of the current bounds of the drawing environment */
    val center: Offset
        get() = drawContext.size.center

    /** Provides the dimensions of the current drawing environment */
    val size: Size
        get() = drawContext.size

    /** The layout direction of the layout being drawn in. */
    val layoutDirection: LayoutDirection

    /**
     * Draws a line between the given points using the given paint. The line is stroked.
     *
     * @param brush the color or fill to be applied to the line
     * @param start first point of the line to be drawn
     * @param end second point of the line to be drawn
     * @param strokeWidth stroke width to apply to the line
     * @param cap treatment applied to the ends of the line segment
     * @param pathEffect optional effect or pattern to apply to the line
     * @param alpha opacity to be applied to the [brush] from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode the blending algorithm to apply to the [brush]
     */
    fun drawLine(
        brush: Brush,
        start: Offset,
        end: Offset,
        strokeWidth: Float = Stroke.HairlineWidth,
        cap: StrokeCap = Stroke.DefaultCap,
        pathEffect: PathEffect? = null,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws a line between the given points using the given paint. The line is stroked.
     *
     * @param color the color to be applied to the line
     * @param start first point of the line to be drawn
     * @param end second point of the line to be drawn
     * @param strokeWidth The stroke width to apply to the line
     * @param cap treatment applied to the ends of the line segment
     * @param pathEffect optional effect or pattern to apply to the line
     * @param alpha opacity to be applied to the [color] from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode the blending algorithm to apply to the [color]
     */
    fun drawLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float = Stroke.HairlineWidth,
        cap: StrokeCap = Stroke.DefaultCap,
        pathEffect: PathEffect? = null,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws a rectangle with the given offset and size. If no offset from the top left is provided,
     * it is drawn starting from the origin of the current translation. If no size is provided, the
     * size of the current environment is used.
     *
     * @param brush The color or fill to be applied to the rectangle
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param alpha Opacity to be applied to the [brush] from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the rectangle is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to apply to destination
     */
    fun drawRect(
        brush: Brush,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws a rectangle with the given offset and size. If no offset from the top left is provided,
     * it is drawn starting from the origin of the current translation. If no size is provided, the
     * size of the current environment is used.
     *
     * @param color The color to be applied to the rectangle
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param alpha Opacity to be applied to the [color] from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the rectangle is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] source pixels
     * @param blendMode Blending algorithm to apply to destination
     */
    fun drawRect(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws the given [ImageBitmap] into the canvas with its top-left corner at the given [Offset].
     * The image is composited into the canvas using the given [Paint].
     *
     * @param image The [ImageBitmap] to draw
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param alpha Opacity to be applied to [image] from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Specifies whether the image is to be drawn filled in or as a rectangular stroke
     * @param colorFilter ColorFilter to apply to the [image] when drawn into the destination
     * @param blendMode Blending algorithm to apply to destination
     */
    fun drawImage(
        image: ImageBitmap,
        topLeft: Offset = Offset.Zero,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws the subset of the given image described by the `src` argument into the canvas in the
     * axis-aligned rectangle given by the `dst` argument.
     *
     * If no src rect is provided, the entire image is scaled into the corresponding destination
     * bounds
     *
     * @param image The source image to draw
     * @param srcOffset Optional offset representing the top left offset of the source image to
     *   draw, this defaults to the origin of [image]
     * @param srcSize Optional dimensions of the source image to draw relative to [srcOffset], this
     *   defaults the width and height of [image]
     * @param dstOffset Optional offset representing the top left offset of the destination to draw
     *   the given image, this defaults to the origin of the current translation tarting top left
     *   offset in the destination to draw the image
     * @param dstSize Optional dimensions of the destination to draw, this defaults to [srcSize]
     * @param alpha Opacity to be applied to [image] from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Specifies whether the image is to be drawn filled in or as a rectangular stroke
     * @param colorFilter ColorFilter to apply to the [image] when drawn into the destination
     * @param blendMode Blending algorithm to apply to destination
     */
    @Deprecated(
        "Prefer usage of drawImage that consumes an optional FilterQuality parameter",
        level = DeprecationLevel.HIDDEN,
        replaceWith =
            ReplaceWith(
                "drawImage(image, srcOffset, srcSize, dstOffset, dstSize, alpha, style, " +
                    "colorFilter, blendMode, FilterQuality.Low)",
                "androidx.compose.ui.graphics.MyDrawScope",
                "androidx.compose.ui.graphics.FilterQuality",
            ),
    ) // Binary API compatibility.
    fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset = IntOffset.Zero,
        srcSize: IntSize = IntSize(image.width, image.height),
        dstOffset: IntOffset = IntOffset.Zero,
        dstSize: IntSize = srcSize,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws the subset of the given image described by the `src` argument into the canvas in the
     * axis-aligned rectangle given by the `dst` argument.
     *
     * If no src rect is provided, the entire image is scaled into the corresponding destination
     * bounds
     *
     * @param image The source image to draw
     * @param srcOffset Optional offset representing the top left offset of the source image to
     *   draw, this defaults to the origin of [image]
     * @param srcSize Optional dimensions of the source image to draw relative to [srcOffset], this
     *   defaults the width and height of [image]
     * @param dstOffset Optional offset representing the top left offset of the destination to draw
     *   the given image, this defaults to the origin of the current translation tarting top left
     *   offset in the destination to draw the image
     * @param dstSize Optional dimensions of the destination to draw, this defaults to [srcSize]
     * @param alpha Opacity to be applied to [image] from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Specifies whether the image is to be drawn filled in or as a rectangular stroke
     * @param colorFilter ColorFilter to apply to the [image] when drawn into the destination
     * @param blendMode Blending algorithm to apply to destination
     * @param filterQuality Sampling algorithm applied to the [image] when it is scaled and drawn
     *   into the destination. The default is [FilterQuality.Low] which scales using a bilinear
     *   sampling algorithm
     */
    fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset = IntOffset.Zero,
        srcSize: IntSize = IntSize(image.width, image.height),
        dstOffset: IntOffset = IntOffset.Zero,
        dstSize: IntSize = srcSize,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
        filterQuality: FilterQuality = DefaultFilterQuality,
    )

    //  TODO understand why this was here before deleting
    //  {
    //    drawScope.drawImage(
    //      image = image,
    //      srcOffset = srcOffset,
    //      srcSize = srcSize,
    //      dstOffset = dstOffset,
    //      dstSize = dstSize,
    //      alpha = alpha,
    //      style = style,
    //      colorFilter = colorFilter,
    //      blendMode = blendMode,
    //    )
    //  }

    /**
     * Draws a rounded rectangle with the provided size, offset and radii for the x and y axis
     * respectively. This rectangle is drawn with the provided [Brush] parameter and is filled or
     * stroked based on the given [DrawStyle]
     *
     * @param brush The color or fill to be applied to the rounded rectangle
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param cornerRadius Corner radius of the rounded rectangle, negative radii values are clamped
     *   to 0
     * @param alpha Opacity to be applied to rounded rectangle from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Specifies whether the rounded rectangle is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the brush
     */
    fun drawRoundRect(
        brush: Brush,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        cornerRadius: CornerRadius = CornerRadius.Zero,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws a rounded rectangle with the given [Paint]. Whether the rectangle is filled or stroked
     * (or both) is controlled by [Paint.style].
     *
     * @param color The color to be applied to the rounded rectangle
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param cornerRadius Corner radius of the rounded rectangle, negative radii values are clamped
     *   to 0
     * @param alpha Opacity to be applied to rounded rectangle from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Specifies whether the rounded rectangle is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the color
     */
    fun drawRoundRect(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        cornerRadius: CornerRadius = CornerRadius.Zero,
        style: DrawStyle = Fill,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws a circle at the provided center coordinate and radius. If no center point is provided
     * the center of the bounds is used.
     *
     * @param brush The color or fill to be applied to the circle
     * @param radius The radius of the circle
     * @param center The center coordinate where the circle is to be drawn
     * @param alpha Opacity to be applied to the circle from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the circle is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the brush
     */
    fun drawCircle(
        brush: Brush,
        radius: Float = size.minDimension / 2.0f,
        center: Offset = this.center,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws a circle at the provided center coordinate and radius. If no center point is provided
     * the center of the bounds is used.
     *
     * @param color The color or fill to be applied to the circle
     * @param radius The radius of the circle
     * @param center The center coordinate where the circle is to be drawn
     * @param alpha Opacity to be applied to the circle from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the circle is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the brush
     */
    fun drawCircle(
        color: Color,
        radius: Float = size.minDimension / 2.0f,
        center: Offset = this.center,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws a circle at the provided center coordinate and radius. If no center point is provided
     * the center of the bounds is used.
     *
     * @param color The color or fill to be applied to the circle
     * @param radius The radius of the circle
     * @param center The center coordinate where the circle is to be drawn
     * @param alpha Opacity to be applied to the circle from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the circle is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the brush
     */
    fun drawCircle(
        color: Color,
        radius: Number = size.minDimension / 2.0f,
        center: Offset = this.center,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Number = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    ) {
        val iRadius: Float =
            if (radius is RemoteFloat) radius.internalAsFloat() else radius.toFloat()
        val iAlpha: Float = if (alpha is RemoteFloat) alpha.internalAsFloat() else alpha.toFloat()
        drawCircle(
            color = color,
            radius = iRadius,
            center = center,
            alpha = iAlpha,
            style = style,
            colorFilter = colorFilter,
            blendMode = blendMode,
        )
    }

    /**
     * Draws an oval with the given offset and size. If no offset from the top left is provided, it
     * is drawn starting from the origin of the current translation. If no size is provided, the
     * size of the current environment is used.
     *
     * @param brush Color or fill to be applied to the oval
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param alpha Opacity to be applied to the oval from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the oval is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the brush
     * @sample androidx.compose.ui.graphics.samples.MyDrawScopeOvalBrushSample
     */
    fun drawOval(
        brush: Brush,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws an oval with the given offset and size. If no offset from the top left is provided, it
     * is drawn starting from the origin of the current translation. If no size is provided, the
     * size of the current environment is used.
     *
     * @param color Color to be applied to the oval
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param alpha Opacity to be applied to the oval from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the oval is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the brush
     * @sample androidx.compose.ui.graphics.samples.MyDrawScopeOvalColorSample
     */
    fun drawOval(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draw an arc scaled to fit inside the given rectangle. It starts from startAngle degrees
     * around the oval up to startAngle + sweepAngle degrees around the oval, with zero degrees
     * being the point on the right hand side of the oval that crosses the horizontal line that
     * intersects the center of the rectangle and with positive angles going clockwise around the
     * oval. If useCenter is true, the arc is closed back to the center, forming a circle sector.
     * Otherwise, the arc is not closed, forming a circle segment.
     *
     * @param brush Color or fill to be applied to the arc
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the arc to draw
     * @param startAngle Starting angle in degrees. 0 represents 3 o'clock
     * @param sweepAngle Size of the arc in degrees that is drawn clockwise relative to [startAngle]
     * @param useCenter Flag indicating if the arc is to close the center of the bounds
     * @param alpha Opacity to be applied to the arc from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the arc is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the arc when it is drawn
     */
    fun drawArc(
        brush: Brush,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draw an arc scaled to fit inside the given rectangle. It starts from startAngle degrees
     * around the oval up to startAngle + sweepAngle degrees around the oval, with zero degrees
     * being the point on the right hand side of the oval that crosses the horizontal line that
     * intersects the center of the rectangle and with positive angles going clockwise around the
     * oval. If useCenter is true, the arc is closed back to the center, forming a circle sector.
     * Otherwise, the arc is not closed, forming a circle segment.
     *
     * @param color Color to be applied to the arc
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the arc to draw
     * @param startAngle Starting angle in degrees. 0 represents 3 o'clock
     * @param sweepAngle Size of the arc in degrees that is drawn clockwise relative to [startAngle]
     * @param useCenter Flag indicating if the arc is to close the center of the bounds
     * @param alpha Opacity to be applied to the arc from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the arc is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the arc when it is drawn
     */
    fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws the given [Path] with the given [Color]. Whether this shape is filled or stroked (or
     * both) is controlled by [DrawStyle]. If the path is filled, then subpaths within it are
     * implicitly closed (see [Path.close]).
     *
     * @param path Path to draw
     * @param color Color to be applied to the path
     * @param alpha Opacity to be applied to the path from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the path is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the path when it is drawn
     */
    fun drawPath(
        path: Path,
        color: Color,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws a path, interpolated using tween, between path1, & path2 [Path] with the given [Color].
     * Whether this shape is filled or stroked (or both) is controlled by [DrawStyle]. If the path
     * is filled, then subpaths within it are implicitly closed (see [Path.close]). path must
     * contain the same pattern and order of path commands (path.xxTo())
     *
     * @param path1 Path to draw
     * @param path2 Path to draw
     * @param tween defines interpolation (path2-path1) * tween + path1
     * @param start defines fraction to start def = 0f at Nan means start at beginning
     * @param stop defines fraction to stop default = 1f, Nan means start at ending
     * @param color Color to be applied to the path
     * @param alpha Opacity to be applied to the path from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the path is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the path when it is drawn
     */
    fun drawTweenPath(
        path1: Path,
        path2: Path,
        tween: Number,
        color: Color,
        start: Number = 0f,
        stop: Number = 1f,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws a path, interpolated using tween, between path1, & path2 [Path] with the given [Color].
     * Whether this shape is filled or stroked (or both) is controlled by [DrawStyle]. If the path
     * is filled, then subpaths within it are implicitly closed (see [Path.close]). path must
     * contain the same pattern and order of path commands (path.xxTo())
     *
     * @param path1 Path to draw
     * @param path2 Path to draw
     * @param tween defines interpolation (path2-path1) * tween + path1
     * @param start defines fraction to start def = 0f at Nan means start at beginning
     * @param stop defines fraction to stop default = 1f, Nan means start at ending
     * @param color Color to be applied to the path
     * @param alpha Opacity to be applied to the path from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the path is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the path when it is drawn
     */
    fun drawAnchoredText(
        text: CharSequence,
        brush: RemoteBrush,
        anchor: Offset = Offset.Zero,
        /*@FloatRange(from = -1.0, to = 1.0)*/
        panx: Number = 0f,
        /*@FloatRange(from = -1.0, to = 1.0)*/
        pany: Number = 0f,
        alpha: Number = 1f,
        //    textDecoration: TextDecoration? = null,
        drawStyle: DrawStyle = Fill,
        typeface: android.graphics.Typeface? = null,
        textSize: Number = 32f,
        //    blendMode: BlendMode = DrawScope.DefaultBlendMode
    )

    /**
     * Draws a path, interpolated using tween, between path1, & path2 [Path] with the given [Color].
     * Whether this shape is filled or stroked (or both) is controlled by [DrawStyle]. If the path
     * is filled, then subpaths within it are implicitly closed (see [Path.close]). path must
     * contain the same pattern and order of path commands (path.xxTo())
     *
     * @param path1 Path to draw
     * @param path2 Path to draw
     * @param tween defines interpolation (path2-path1) * tween + path1
     * @param start defines fraction to start def = 0f at Nan means start at beginning
     * @param stop defines fraction to stop default = 1f, Nan means start at ending
     * @param color Color to be applied to the path
     * @param alpha Opacity to be applied to the path from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the path is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the path when it is drawn
     */
    fun drawAnchoredText(
        text: RemoteString,
        brush: RemoteBrush,
        anchor: Offset = Offset.Zero,
        /*@FloatRange(from = -1.0, to = 1.0)*/
        panx: Number = 0f,
        /*@FloatRange(from = -1.0, to = 1.0)*/
        pany: Number = 0f,
        alpha: Number = 1f,
        //    textDecoration: TextDecoration? = null,
        drawStyle: DrawStyle = Fill,
        typeface: android.graphics.Typeface? = null,
        textSize: Number = 32f,
        //    blendMode: BlendMode = DrawScope.DefaultBlendMode
    )

    /**
     * Draws a path, interpolated using tween, between path1, & path2 [Path] with the given [Color].
     * Whether this shape is filled or stroked (or both) is controlled by [DrawStyle]. If the path
     * is filled, then subpaths within it are implicitly closed (see [Path.close]). path must
     * contain the same pattern and order of path commands (path.xxTo())
     *
     * @param path1 Path to draw
     * @param path2 Path to draw
     * @param tween defines interpolation (path2-path1) * tween + path1
     * @param start defines fraction to start def = 0f at Nan means start at beginning
     * @param stop defines fraction to stop default = 1f, Nan means start at ending
     * @param color Color to be applied to the path
     * @param alpha Opacity to be applied to the path from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the path is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the path when it is drawn
     */
    fun drawAnchoredText(
        text: CharSequence,
        color: Color = Color.Unspecified,
        anchor: Offset = Offset.Zero,
        /*@FloatRange(from = -1.0, to = 1.0)*/
        panx: Number = 0f,
        /*@FloatRange(from = -1.0, to = 1.0)*/
        pany: Number = 0f,
        alpha: Number = 1f,
        //    textDecoration: TextDecoration? = null,
        drawStyle: DrawStyle = Fill,
        typeface: android.graphics.Typeface? = null,
        textSize: Number = 32f,
        //    blendMode: BlendMode = DrawScope.DefaultBlendMode
    )

    fun drawAnchoredText(
        text: RemoteString,
        color: Color = Color.Unspecified,
        anchor: Offset = Offset.Zero,
        /*@FloatRange(from = -1.0, to = 1.0)*/
        panx: Number = 0f,
        /*@FloatRange(from = -1.0, to = 1.0)*/
        pany: Number = 0f,
        alpha: Number = 1f,
        //    textDecoration: TextDecoration? = null,
        drawStyle: DrawStyle = Fill,
        typeface: android.graphics.Typeface? = null,
        textSize: Number = 32f,
        //    blendMode: BlendMode = DrawScope.DefaultBlendMode
    )

    /**
     * Draws the given [Path] with the given [Color]. Whether this shape is filled or stroked (or
     * both) is controlled by [DrawStyle]. If the path is filled, then subpaths within it are
     * implicitly closed (see [Path.close]).
     *
     * @param path Path to draw
     * @param brush Brush to be applied to the path
     * @param alpha Opacity to be applied to the path from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param style Whether or not the path is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the path when it is drawn
     */
    fun drawPath(
        path: Path,
        brush: Brush,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws a sequence of points according to the given [PointMode].
     *
     * The `points` argument is interpreted as offsets from the origin.
     *
     * @param points List of points to draw with the specified [PointMode]
     * @param pointMode [PointMode] used to indicate how the points are to be drawn
     * @param color Color to be applied to the points
     * @param alpha Opacity to be applied to the path from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param strokeWidth The stroke width to apply to the line
     * @param cap Treatment applied to the ends of the line segment
     * @param pathEffect optional effect or pattern to apply to the point
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the path when it is drawn
     */
    fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        color: Color,
        strokeWidth: Float = Stroke.HairlineWidth,
        cap: StrokeCap = StrokeCap.Butt,
        pathEffect: PathEffect? = null,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draws a sequence of points according to the given [PointMode].
     *
     * The `points` argument is interpreted as offsets from the origin.
     *
     * @param points List of points to draw with the specified [PointMode]
     * @param pointMode [PointMode] used to indicate how the points are to be drawn
     * @param brush Brush to be applied to the points
     * @param strokeWidth The stroke width to apply to the line
     * @param cap Treatment applied to the ends of the line segment
     * @param pathEffect optional effect or pattern to apply to the points
     * @param alpha Opacity to be applied to the path from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively.
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the path when it is drawn
     */
    fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        brush: Brush,
        strokeWidth: Float = Stroke.HairlineWidth,
        cap: StrokeCap = StrokeCap.Butt,
        pathEffect: PathEffect? = null,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    )

    /**
     * Draw an existing text layout as produced by [TextMeasurer].
     *
     * This draw function cannot relayout when async font loading resolves. If using async fonts or
     * other dynamic text layout, you are responsible for invalidating layout on changes.
     *
     * @param textLayoutResult Text Layout to be drawn
     * @param color Text color to use
     * @param topLeft Offsets the text from top left point of the current coordinate system.
     * @param alpha opacity to be applied to the [color] from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively
     * @param shadow The shadow effect applied on the text.
     * @param textDecoration The decorations to paint on the text (e.g., an underline).
     * @param drawStyle Whether or not the text is stroked or filled in.
     * @param blendMode Blending algorithm to be applied to the text
     * @sample androidx.compose.ui.text.samples.DrawTextMeasureInLayoutSample
     * @sample androidx.compose.ui.text.samples.DrawTextDrawWithCacheSample
     */
    fun drawText(
        textLayoutResult: TextLayoutResult,
        color: Color = Color.Unspecified,
        topLeft: Offset = Offset.Zero,
        alpha: Float = Float.NaN,
        shadow: Shadow? = null,
        textDecoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode,
    )

    /**
     * Draw an existing text layout as produced by [TextMeasurer].
     *
     * This draw function cannot relayout when async font loading resolves. If using async fonts or
     * other dynamic text layout, you are responsible for invalidating layout on changes.
     *
     * @param textLayoutResult Text Layout to be drawn
     * @param brush The brush to use when drawing the text.
     * @param topLeft Offsets the text from top left point of the current coordinate system.
     * @param alpha Opacity to be applied to [brush] from 0.0f to 1.0f representing fully
     *   transparent to fully opaque respectively.
     * @param shadow The shadow effect applied on the text.
     * @param textDecoration The decorations to paint on the text (e.g., an underline).
     * @param drawStyle Whether or not the text is stroked or filled in.
     * @param blendMode Blending algorithm to be applied to the text
     */
    fun drawText(
        textLayoutResult: TextLayoutResult,
        brush: Brush,
        topLeft: Offset = Offset.Zero,
        alpha: Float = Float.NaN,
        shadow: Shadow? = null,
        textDecoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode,
    )

    /**
     * Draw text using a TextMeasurer.
     *
     * This draw function supports only one text style, and async font loading.
     *
     * TextMeasurer carries an internal cache to optimize text layout measurement for repeated calls
     * in draw phase. If layout affecting attributes like font size, font weight, overflow,
     * softWrap, etc. are changed in consecutive calls to this method, TextMeasurer and its internal
     * cache that holds layout results may not offer any benefits. Check out [TextMeasurer] and
     * drawText overloads that take [TextLayoutResult] to learn more about text layout and draw
     * phase optimizations.
     *
     * @param textMeasurer Measures and lays out the text
     * @param text Text to be drawn
     * @param topLeft Offsets the text from top left point of the current coordinate system.
     * @param style the [TextStyle] to be applied to the text
     * @param overflow How visual overflow should be handled.
     * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in
     *   the text will be positioned as if there was unlimited horizontal space. If [softWrap] is
     *   false, [overflow] and TextAlign may have unexpected effects.
     * @param maxLines An optional maximum number of lines for the text to span, wrapping if
     *   necessary. If the text exceeds the given number of lines, it will be truncated according to
     *   [overflow] and [softWrap]. If it is not null, then it must be greater than zero.
     * @param size how wide and tall the text should be. If left [Size.Unspecified] as its default
     *   value, text will be forced to fit inside the total drawing area from where it's placed. If
     *   size is specified, [Size.width] will define the width of the text. [Size.height] helps
     *   defining the number of lines that fit if [softWrap] is enabled and [overflow] is
     *   [TextOverflow.Ellipsis]. Otherwise, [Size.height] either defines where the text is clipped
     *   ([TextOverflow.Clip]) or becomes no-op.
     * @param blendMode Blending algorithm to be applied to the text
     * @sample androidx.compose.ui.text.samples.DrawTextSample
     * @sample androidx.compose.ui.text.samples.DrawTextStyledSample
     */
    fun drawText(
        textMeasurer: TextMeasurer,
        text: String,
        topLeft: Offset = Offset.Zero,
        style: TextStyle = TextStyle.Default,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        size: Size = Size.Unspecified,
        blendMode: BlendMode = DrawScope.DefaultBlendMode,
    )

    /**
     * Draw styled text using a TextMeasurer.
     *
     * This draw function supports multi-styling and async font loading.
     *
     * TextMeasurer carries an internal cache to optimize text layout measurement for repeated calls
     * in draw phase. If layout affecting attributes like font size, font weight, overflow,
     * softWrap, etc. are changed in consecutive calls to this method, TextMeasurer and its internal
     * cache that holds layout results may not offer any benefits. Check out [TextMeasurer] and
     * drawText overloads that take [TextLayoutResult] to learn more about text layout and draw
     * phase optimizations.
     *
     * @param textMeasurer Measures and lays out the text
     * @param text Text to be drawn
     * @param topLeft Offsets the text from top left point of the current coordinate system.
     * @param style the [TextStyle] to be applied to the text
     * @param overflow How visual overflow should be handled.
     * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in
     *   the text will be positioned as if there was unlimited horizontal space. If [softWrap] is
     *   false, [overflow] and TextAlign may have unexpected effects.
     * @param maxLines An optional maximum number of lines for the text to span, wrapping if
     *   necessary. If the text exceeds the given number of lines, it will be truncated according to
     *   [overflow] and [softWrap]. If it is not null, then it must be greater than zero.
     * @param placeholders a list of [Placeholder]s that specify ranges of text which will be
     *   skipped during layout and replaced with [Placeholder]. It's required that the range of each
     *   [Placeholder] doesn't cross paragraph boundary, otherwise [IllegalArgumentException] is
     *   thrown.
     * @param size how wide and tall the text should be. If left [Size.Unspecified] as its default
     *   value, text will be forced to fit inside the total drawing area from where it's placed. If
     *   size is specified, [Size.width] will define the width of the text. [Size.height] helps
     *   defining the number of lines that fit if [softWrap] is enabled and [overflow] is
     *   [TextOverflow.Ellipsis]. Otherwise, [Size.height] either defines where the text is clipped
     *   ([TextOverflow.Clip]) or becomes no-op.
     * @param blendMode Blending algorithm to be applied to the text
     * @sample androidx.compose.ui.text.samples.DrawTextAnnotatedStringSample
     */
    fun drawText(
        textMeasurer: TextMeasurer,
        text: AnnotatedString,
        topLeft: Offset = Offset.Zero,
        style: TextStyle = TextStyle.Default,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        placeholders: List<AnnotatedString.Range<Placeholder>> = emptyList(),
        size: Size = Size.Unspecified,
        blendMode: BlendMode = DrawScope.DefaultBlendMode,
    )

    /** Helper method to offset the provided size with the offset in box width and height */
    private fun Size.offsetSize(offset: Offset): Size =
        Size(this.width - offset.x, this.height - offset.y)

    companion object {

        /**
         * Default blending mode used for each drawing operation. This ensures that content is drawn
         * on top of the pixels in the destination
         */
        val DefaultBlendMode: BlendMode = BlendMode.SrcOver

        /**
         * Default FilterQuality used for determining the filtering algorithm to apply when scaling
         * [ImageBitmap] objects. Maps to the default behavior of bilinear filtering
         */
        val DefaultFilterQuality: FilterQuality = FilterQuality.Low
    }
}
