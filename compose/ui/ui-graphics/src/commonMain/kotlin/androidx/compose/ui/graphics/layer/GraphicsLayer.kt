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

package androidx.compose.ui.graphics.layer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * Draw the provided [GraphicsLayer] into the current [DrawScope]. The [GraphicsLayer] provided must
 * have [GraphicsLayer.record] invoked on it otherwise no visual output will be seen in the rendered
 * result.
 *
 * @sample androidx.compose.ui.graphics.samples.GraphicsLayerTopLeftSample
 * @sample androidx.compose.ui.graphics.samples.GraphicsLayerScaleAndPivotSample
 * @sample androidx.compose.ui.graphics.samples.GraphicsLayerColorFilterSample
 * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRenderEffectSample
 * @sample androidx.compose.ui.graphics.samples.GraphicsLayerAlphaSample
 * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRotationX
 * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRotationYWithCameraDistance
 */
fun DrawScope.drawLayer(graphicsLayer: GraphicsLayer) {
    drawIntoCanvas { canvas -> graphicsLayer.draw(canvas, drawContext.graphicsLayer) }
}

/** Default camera distance for all layers */
const val DefaultCameraDistance = 8.0f

/**
 * Drawing layer used to record drawing commands in a displaylist as well as additional properties
 * that affect the rendering of the display list. This provides an isolation boundary to divide a
 * complex scene into smaller pieces that can be updated individually of one another without
 * recreating the entire scene. Transformations made to a [GraphicsLayer] can be done without
 * re-recording the display list.
 *
 * Usage of a [GraphicsLayer] requires a minimum of 2 steps.
 * 1) The [GraphicsLayer] must be built, which involves specifying the position alongside a list of
 *    drawing commands using [GraphicsLayer.record]
 * 2) The [GraphicsLayer] is then drawn into another destination [Canvas] using
 *    [GraphicsLayer.draw].
 *
 * Additionally the contents of the displaylist can be transformed when it is drawn into a
 * desintation [Canvas] by specifying either [scaleX], [scaleY], [translationX], [translationY],
 * [rotationX], [rotationY], or [rotationZ].
 *
 * The rendered result of the displaylist can also be modified by configuring the
 * [GraphicsLayer.blendMode], [GraphicsLayer.colorFilter], [GraphicsLayer.alpha] or
 * [GraphicsLayer.renderEffect]
 */
expect class GraphicsLayer {

    /**
     * [CompositingStrategy] determines whether or not the contents of this layer are rendered into
     * an offscreen buffer. This is useful in order to optimize alpha usages with
     * [CompositingStrategy.ModulateAlpha] which will skip the overhead of an offscreen buffer but
     * can generate different rendering results depending on whether or not the contents of the
     * layer are overlapping. Similarly leveraging [CompositingStrategy.Offscreen] is useful in
     * situations where creating an offscreen buffer is preferred usually in conjunction with
     * [BlendMode] usage.
     */
    var compositingStrategy: CompositingStrategy

    /**
     * Offset in pixels where this [GraphicsLayer] will render within a provided canvas when
     * [drawLayer] is called.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerTopLeftSample
     */
    var topLeft: IntOffset

    /**
     * Size in pixels of the [GraphicsLayer]. By default [GraphicsLayer] contents can draw outside
     * of the bounds specified by [topLeft] and [size], however, rasterization of this layer into an
     * offscreen buffer will be sized according to the specified size. This is configured by calling
     * [record]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerSizeSample
     */
    var size: IntSize
        private set

    /**
     * [Offset] in pixels used as the center for any rotation or scale transformation. If this value
     * is [Offset.Unspecified], then the center of the [GraphicsLayer] is used relative to [topLeft]
     * and [size]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerScaleAndPivotSample
     */
    var pivotOffset: Offset

    /**
     * Alpha of the content of the [GraphicsLayer] between 0f and 1f. Any value between 0f and 1f
     * will be translucent, where 0f will cause the layer to be completely invisible and 1f will be
     * entirely opaque.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerAlphaSample
     */
    var alpha: Float

    /**
     * The horizontal scale of the drawn area. Default value is `1`.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerScaleAndPivotSample
     */
    var scaleX: Float

    /**
     * The vertical scale of the drawn area. Default value is `1`.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerScaleAndPivotSample
     */
    var scaleY: Float

    /**
     * Horizontal pixel offset of the layer relative to [topLeft].x. Default value is `0`.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerTranslateSample
     */
    var translationX: Float

    /**
     * Vertical pixel offset of the layer relative to [topLeft].y. Default value is `0`
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerTranslateSample
     */
    var translationY: Float

    /**
     * Sets the elevation for the shadow in pixels. With the [shadowElevation] > 0f and [Outline]
     * set, a shadow is produced. Default value is `0` and the value must not be negative.
     * Configuring a non-zero [shadowElevation] enables clipping of [GraphicsLayer] content.
     *
     * Note that if you provide a non-zero [shadowElevation] and if the passed [Outline] is concave
     * the shadow will not be drawn on Android versions less than 10.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerShadowSample
     */
    var shadowElevation: Float

    /**
     * Sets the color of the ambient shadow that is drawn when [shadowElevation] > 0f.
     *
     * By default the shadow color is black. Generally, this color will be opaque so the intensity
     * of the shadow is consistent between different graphics layers with different colors.
     *
     * The opacity of the final ambient shadow is a function of the shadow caster height, the alpha
     * channel of the [ambientShadowColor] (typically opaque), and the
     * [android.R.attr.ambientShadowAlpha] theme attribute.
     *
     * Note that this parameter is only supported on Android 9 (Pie) and above. On older versions,
     * this property always returns [Color.Black] and setting new values is ignored.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerShadowSample
     */
    var ambientShadowColor: Color

    /**
     * Sets the color of the spot shadow that is drawn when [shadowElevation] > 0f.
     *
     * By default the shadow color is black. Generally, this color will be opaque so the intensity
     * of the shadow is consistent between different graphics layers with different colors.
     *
     * The opacity of the final spot shadow is a function of the shadow caster height, the alpha
     * channel of the [spotShadowColor] (typically opaque), and the [android.R.attr.spotShadowAlpha]
     * theme attribute.
     *
     * Note that this parameter is only supported on Android 9 (Pie) and above. On older versions,
     * this property always returns [Color.Black] and setting new values is ignored.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerShadowSample
     */
    var spotShadowColor: Color

    /**
     * BlendMode to use when drawing this layer to the destination in [drawLayer]. The default is
     * [BlendMode.SrcOver]. Any value other than [BlendMode.SrcOver] will force this [GraphicsLayer]
     * to use an offscreen compositing layer for rendering and is equivalent to using
     * [CompositingStrategy.Offscreen].
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerBlendModeSample
     */
    var blendMode: BlendMode

    /**
     * ColorFilter applied when drawing this layer to the destination in [drawLayer]. Setting of
     * this to any non-null will force this [GraphicsLayer] to use an offscreen compositing layer
     * for rendering and is equivalent to using [CompositingStrategy.Offscreen]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerColorFilterSample
     */
    var colorFilter: ColorFilter?

    /**
     * Returns the outline specified by either [setPathOutline] or [setRoundRectOutline]. By default
     * this will return [Outline.Rectangle] with the size of the [GraphicsLayer] specified by
     * [record] or [IntSize.Zero] if [record] was not previously invoked.
     */
    val outline: Outline

    /**
     * Specifies the given path to be configured as the outline for this [GraphicsLayer]. When
     * [shadowElevation] is non-zero a shadow is produced with an [Outline] created from the
     * provided [path]. Additionally if [clip] is true, the contents of this [GraphicsLayer] will be
     * clipped to this geometry.
     *
     * @param path Path to be used as the Outline for the [GraphicsLayer]
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerOutlineSample
     */
    fun setPathOutline(path: Path)

    /**
     * Configures a rounded rect outline for this [GraphicsLayer]. By default, [topLeft] is set to
     * [Size.Zero] and [size] is set to [Size.Unspecified] indicating that the outline should match
     * the size of the [GraphicsLayer]. When [shadowElevation] is non-zero a shadow is produced
     * using an [Outline] created from the round rect parameters provided. Additionally if [clip] is
     * true, the contents of this [GraphicsLayer] will be clipped to this geometry.
     *
     * @param topLeft The top left of the rounded rect outline
     * @param size The size of the rounded rect outline
     * @param cornerRadius The corner radius of the rounded rect outline
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRoundRectOutline
     */
    fun setRoundRectOutline(
        topLeft: Offset = Offset.Zero,
        size: Size = Size.Unspecified,
        cornerRadius: Float = 0f,
    )

    /**
     * Configures a rectangular outline for this [GraphicsLayer]. By default, [topLeft] is set to
     * [Size.Zero] and [size] is set to [Size.Unspecified] indicating that the outline should match
     * the size of the [GraphicsLayer]. When [shadowElevation] is non-zero a shadow is produced
     * using an [Outline] created from the round rect parameters provided. Additionally if [clip] is
     * true, the contents of this [GraphicsLayer] will be clipped to this geometry.
     *
     * @param topLeft The top left of the rounded rect outline
     * @param size The size of the rounded rect outline
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRectOutline
     */
    fun setRectOutline(topLeft: Offset = Offset.Zero, size: Size = Size.Unspecified)

    /**
     * The rotation, in degrees, of the contents around the horizontal axis in degrees. Default
     * value is `0`.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRotationX
     */
    var rotationX: Float

    /**
     * The rotation, in degrees, of the contents around the vertical axis in degrees. Default value
     * is `0`.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRotationYWithCameraDistance
     */
    var rotationY: Float

    /**
     * The rotation, in degrees, of the contents around the Z axis in degrees. Default value is `0`.
     */
    var rotationZ: Float

    /**
     * Sets the distance along the Z axis (orthogonal to the X/Y plane on which layers are drawn)
     * from the camera to this layer. The camera's distance affects 3D transformations, for instance
     * rotations around the X and Y axis. If the rotationX or rotationY properties are changed and
     * this view is large (more than half the size of the screen), it is recommended to always use a
     * camera distance that's greater than the height (X axis rotation) or the width (Y axis
     * rotation) of this view.
     *
     * The distance of the camera from the drawing plane can have an affect on the perspective
     * distortion of the layer when it is rotated around the x or y axis. For example, a large
     * distance will result in a large viewing angle, and there will not be much perspective
     * distortion of the view as it rotates. A short distance may cause much more perspective
     * distortion upon rotation, and can also result in some drawing artifacts if the rotated view
     * ends up partially behind the camera (which is why the recommendation is to use a distance at
     * least as far as the size of the view, if the view is to be rotated.)
     *
     * The distance is expressed in pixels and must always be positive. Default value is
     * [DefaultCameraDistance]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRotationYWithCameraDistance
     */
    var cameraDistance: Float

    /**
     * Determines if the [GraphicsLayer] should be clipped to the rectangular bounds specified by
     * [topLeft] and [size]. The default is false, however, contents will always be clipped to their
     * bounds when the GraphicsLayer is promoted off an offscreen rendering buffer (i.e.
     * CompositingStrategy.Offscreen is used, a non-null ColorFilter, RenderEffect is applied or if
     * the BlendMode is not equivalent to BlendMode.SrcOver
     */
    @Suppress("GetterSetterNames") @get:Suppress("GetterSetterNames") var clip: Boolean

    /**
     * Configure the [RenderEffect] to apply to this [GraphicsLayer]. This will apply a visual
     * effect to the results of the [GraphicsLayer] before it is drawn. For example if [BlurEffect]
     * is provided, the contents will be drawn in a separate layer, then this layer will be blurred
     * when this [GraphicsLayer] is drawn.
     *
     * Note this parameter is only supported on Android 12 and above. Attempts to use this Modifier
     * on older Android versions will be ignored.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRenderEffectSample
     */
    var renderEffect: RenderEffect?

    /**
     * Determines if this [GraphicsLayer] has been released. Any attempts to use a [GraphicsLayer]
     * after it has been released is an error.
     */
    var isReleased: Boolean
        private set

    /**
     * Constructs the display list of drawing commands into this layer that will be rendered when
     * this [GraphicsLayer] is drawn elsewhere with [drawLayer].
     *
     * @param density [Density] used to assist in conversions of density independent pixels to raw
     *   pixels to draw.
     * @param layoutDirection [LayoutDirection] of the layout being drawn in.
     * @param size [Size] of the [GraphicsLayer]
     * @param block lambda that is called to issue drawing commands on this [DrawScope]
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerTopLeftSample
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerBlendModeSample
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerTranslateSample
     */
    fun record(
        density: Density,
        layoutDirection: LayoutDirection,
        size: IntSize,
        block: DrawScope.() -> Unit
    )

    /**
     * Create an [ImageBitmap] with the contents of this [GraphicsLayer] instance. Note that
     * [GraphicsLayer.record] must be invoked first to record drawing operations before invoking
     * this method.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerToImageBitmap
     */
    suspend fun toImageBitmap(): ImageBitmap

    /** Draw the contents of this [GraphicsLayer] into the specified [Canvas] */
    internal fun draw(canvas: Canvas, parentLayer: GraphicsLayer?)
}

/**
 * Configures an outline for this [GraphicsLayer] based on the provided [Outline] object.
 *
 * When [GraphicsLayer.shadowElevation] is non-zero a shadow is produced using a provided [Outline].
 * Additionally if [GraphicsLayer.clip] is true, the contents of this [GraphicsLayer] will be
 * clipped to this geometry.
 *
 * @param outline an [Outline] to apply for the layer.
 */
fun GraphicsLayer.setOutline(outline: Outline) {
    when (outline) {
        is Outline.Rectangle ->
            setRectOutline(
                Offset(outline.rect.left, outline.rect.top),
                Size(outline.rect.width, outline.rect.height)
            )
        is Outline.Generic -> setPathOutline(outline.path)
        is Outline.Rounded -> {
            // If the rounded rect has a path, then the corner radii are not the same across
            // each of the corners, so we set the outline as a Path.
            // If there is no path available, then the corner radii are identical so we can
            // use setRoundRectOutline directly.
            if (outline.roundRectPath != null) {
                setPathOutline(outline.roundRectPath)
            } else {
                val rr = outline.roundRect
                setRoundRectOutline(
                    Offset(rr.left, rr.top),
                    Size(rr.width, rr.height),
                    rr.bottomLeftCornerRadius.x
                )
            }
        }
    }
}
