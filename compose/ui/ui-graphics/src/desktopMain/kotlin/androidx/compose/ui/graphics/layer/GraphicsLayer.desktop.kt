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

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.SkiaBackedCanvas
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.asSkiaColorFilter
import androidx.compose.ui.graphics.asSkiaPath
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toSkia
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.jetbrains.skia.Picture
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.Point3
import org.jetbrains.skia.Rect as SkiaRect

fun GraphicsContext(): GraphicsContext = DesktopGraphicsContext()

private class DesktopGraphicsContext : GraphicsContext {
    override fun createGraphicsLayer(): GraphicsLayer {
        return GraphicsLayer()
    }

    override fun releaseGraphicsLayer(layer: GraphicsLayer) {
        layer.release()
    }
}

actual class GraphicsLayer {

    private val pictureDrawScope = CanvasDrawScope()
    private val pictureRecorder = PictureRecorder()
    private var picture: Picture? = null

    private var matrixDirty = true

    private val matrix = Matrix()

    actual var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto

    private var internalOutline: Outline? = null
    private var outlineDirty = true
    private var roundRectOutlineTopLeft: IntOffset = UnsetOffset
    private var roundRectOutlineSize: IntSize = UnsetSize
    private var roundRectCornerRadius: Float = 0f
    private var outlinePath: Path? = null

    actual var topLeft: IntOffset = IntOffset.Zero
        set(value) {
            if (field != value) {
                field = value
                updateLayerConfiguration()
            }
        }

    actual var size: IntSize = IntSize.Zero
        private set

    actual var alpha: Float = 1f

    actual var scaleX: Float = 1f
        set(value) {
            invalidateMatrix()
            field = value
        }
    actual var scaleY: Float = 1f
        set(value) {
            invalidateMatrix()
            field = value
        }
    actual var translationX: Float = 0f
        set(value) {
            invalidateMatrix()
            field = value
        }
    actual var translationY: Float = 0f
        set(value) {
            invalidateMatrix()
            field = value
        }
    actual var shadowElevation: Float = 0f
    actual var rotationX: Float = 0f
        set(value) {
            invalidateMatrix()
            field = value
        }
    actual var rotationY: Float = 0f
        set(value) {
            invalidateMatrix()
            field = value
        }
    actual var rotationZ: Float = 0f
        set(value) {
            invalidateMatrix()
            field = value
        }

    actual var cameraDistance: Float = DefaultCameraDistance
        set(value) {
            invalidateMatrix()
            field = value
        }

    actual var renderEffect: RenderEffect? = null

    private var density: Density = Density(1f)

    private fun invalidateMatrix() {
        matrixDirty = true
    }

    private fun updateLayerConfiguration() {
        this.outlineDirty = true
        invalidateMatrix()
    }

    actual fun buildLayer(
        density: Density,
        layoutDirection: LayoutDirection,
        size: IntSize,
        block: DrawScope.() -> Unit
    ): GraphicsLayer {
        this.density = density
        this.size = size
        updateLayerConfiguration()
        val x = topLeft.x.toFloat()
        val y = topLeft.y.toFloat()
        val bounds = SkiaRect(
            x,
            y,
            x + size.width.toFloat(),
            y + size.height.toFloat()
        )
        val canvas = pictureRecorder.beginRecording(bounds)
        val skiaCanvas = canvas.asComposeCanvas() as SkiaBackedCanvas
        skiaCanvas.alphaMultiplier = if (compositingStrategy == CompositingStrategy.ModulateAlpha) {
            this@GraphicsLayer.alpha
        } else {
            1.0f
        }

        pictureDrawScope.draw(
            density,
            layoutDirection,
            skiaCanvas,
            size.toSize(),
            block
        )
        picture = pictureRecorder.finishRecordingAsPicture()
        return this
    }

    actual var clip: Boolean = false

    private inline fun createOutlineWithPosition(
        outlineTopLeft: IntOffset,
        outlineSize: IntSize,
        block: (IntOffset, IntSize) -> Outline
    ): Outline {
        val targetTopLeft = if (outlineTopLeft == UnsetOffset) {
            this.topLeft
        } else {
            outlineTopLeft
        }

        val targetSize = if (outlineSize == UnsetSize) {
            this.size
        } else {
            outlineSize
        }
        return block(targetTopLeft, targetSize)
    }

    private fun configureOutline(): Outline {
        var tmpOutline = internalOutline
        if (outlineDirty || tmpOutline == null) {
            val tmpPath = outlinePath
            tmpOutline = if (tmpPath != null) {
                Outline.Generic(tmpPath)
            } else {
                createOutlineWithPosition(
                    roundRectOutlineTopLeft,
                    roundRectOutlineSize
                ) { outlineTopLeft, outlineSize ->
                    if (roundRectCornerRadius > 0f) {
                        Outline.Rounded(
                            RoundRect(
                                outlineTopLeft.x.toFloat(),
                                outlineTopLeft.y.toFloat(),
                                outlineTopLeft.x.toFloat() + outlineSize.width,
                                outlineTopLeft.y.toFloat() + outlineSize.height,
                                CornerRadius(roundRectCornerRadius)
                            )
                        )
                    } else {
                        Outline.Rectangle(
                            Rect(
                                outlineTopLeft.x.toFloat(),
                                outlineTopLeft.y.toFloat(),
                                outlineTopLeft.x.toFloat() + outlineSize.width,
                                outlineTopLeft.y.toFloat() + outlineSize.height
                            )
                        )
                    }
                }
            }
            internalOutline = tmpOutline
            outlineDirty = false
        }
        return tmpOutline
    }

    internal actual fun draw(canvas: Canvas, parentLayer: GraphicsLayer?) {
        picture?.let {
            configureOutline()

            updateMatrix()
            canvas.save()
            canvas.concat(matrix)
            canvas.translate(topLeft.x.toFloat(), topLeft.y.toFloat())

            if (shadowElevation > 0) {
                drawShadow(canvas)
            }

            if (clip || shadowElevation > 0f) {
                canvas.save()

                when (val outline = internalOutline) {
                    is Outline.Rectangle ->
                        canvas.clipRect(outline.rect)
                    is Outline.Rounded ->
                        (canvas as SkiaBackedCanvas).clipRoundRect(outline.roundRect)
                    is Outline.Generic ->
                        canvas.clipPath(outline.path)
                    null -> {
                        canvas.clipRect(0f, 0f, size.width.toFloat(), size.height.toFloat())
                    }
                }
            }

            val useLayer = requiresLayer()
            if (useLayer) {
                canvas.saveLayer(
                    Rect(0f, 0f, size.width.toFloat(), size.height.toFloat()),
                    Paint().apply {
                        this.alpha = this@GraphicsLayer.alpha
                        this.asFrameworkPaint().apply {
                            this.imageFilter = this@GraphicsLayer.renderEffect?.asSkiaImageFilter()
                            this.colorFilter = this@GraphicsLayer.colorFilter?.asSkiaColorFilter()
                            this.blendMode = this@GraphicsLayer.blendMode.toSkia()
                        }
                    }
                )
            } else {
                canvas.save()
            }

            canvas.nativeCanvas.drawPicture(it, null, null)

            canvas.restore()

            if (clip) {
                canvas.restore()
            }

            canvas.restore()
        }
    }

    internal fun release() {
        discardDisplayList()
        isReleased = true
    }

    actual var pivotOffset: Offset = Offset.Unspecified
        set(value) {
            invalidateMatrix()
            field = value
        }

    /**
     * BlendMode to use when drawing this layer to the destination in [drawLayer].
     * The default is [BlendMode.SrcOver].
     * Any value other than [BlendMode.SrcOver] will force this [GraphicsLayer] to use an offscreen
     * compositing layer for rendering.
     */
    actual var blendMode: BlendMode = BlendMode.SrcOver

    /**
     * ColorFilter applied when drawing this layer to the destination in [drawLayer].
     * Setting of this to any non-null will force this [GraphicsLayer] to use an offscreen
     * compositing layer for rendering regardless of the value of [compositingStrategy]
     */
    actual var colorFilter: ColorFilter? = null

    private fun resetOutlineParams() {
        internalOutline = null
        outlinePath = null
        roundRectOutlineSize = UnsetSize
        roundRectOutlineTopLeft = UnsetOffset
        roundRectCornerRadius = 0f
        outlineDirty = true
    }

    actual fun setRoundRectOutline(
        topLeft: IntOffset,
        size: IntSize,
        cornerRadius: Float
    ) {
        resetOutlineParams()
        this.roundRectOutlineTopLeft = topLeft
        this.roundRectOutlineSize = size
        this.roundRectCornerRadius = cornerRadius
    }

    /**
     * Specifies the given path to be configured as the outline for this [GraphicsLayer].
     * When [shadowElevation] is non-zero a shadow is produced using this [Outline].
     *
     * @param path Path to be used as the Outline for the [GraphicsLayer]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerOutlineSample
     */
    actual fun setPathOutline(path: Path) {
        resetOutlineParams()
        this.outlinePath = path
    }

    /**
     * Returns the outline specified by either [setPathOutline] or [setRoundRectOutline].
     * By default this will return [Outline.Rectangle] with the size of the [GraphicsLayer]
     * specified by [buildLayer] or [IntSize.Zero] if [buildLayer] was not previously invoked.
     */
    actual val outline: Outline
        get() = configureOutline()

    /**
     * Configures a rectangular outline for this [GraphicsLayer]. By default, both [topLeft] and
     * [size] are set to [UnsetOffset] and [UnsetSize] indicating that the outline should match the
     * bounds of the [GraphicsLayer]. When [shadowElevation] is non-zero a shadow is produced
     * using with an [Outline] created from the rect parameters provided. Additionally if
     * [clip] is true, the contents of this [GraphicsLayer] will be clipped to this geometry.
     *
     * @param topLeft The top left of the rounded rect outline
     * @param size The size of the rounded rect outline
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRectOutline
     */
    actual fun setRectOutline(
        topLeft: IntOffset,
        size: IntSize
    ) {
        setRoundRectOutline(topLeft, size, 0f)
    }

    private fun updateMatrix() {
        if (matrixDirty) {
            val pivotX: Float
            val pivotY: Float
            if (pivotOffset.isUnspecified) {
                pivotX = size.width / 2f
                pivotY = size.height / 2f
            } else {
                pivotX = pivotOffset.x
                pivotY = pivotOffset.y
            }
            matrix.reset()
            matrix *= Matrix().apply {
                translate(x = -pivotX, y = -pivotY)
            }
            matrix *= Matrix().apply {
                translate(translationX, translationY)
                rotateX(rotationX)
                rotateY(rotationY)
                rotateZ(rotationZ)
                scale(scaleX, scaleY)
            }
            matrix *= Matrix().apply {
                translate(x = pivotX, y = pivotY)
            }
            matrixDirty = false
        }
    }

    actual var isReleased: Boolean = false
        private set

    private fun discardDisplayList() {
        picture?.close()
        pictureRecorder.close()
    }

    /**
     * Sets the color of the ambient shadow that is drawn when [shadowElevation] > 0f.
     *
     * By default the shadow color is black. Generally, this color will be opaque so the intensity
     * of the shadow is consistent between different graphics layers with different colors.
     *
     * The opacity of the final ambient shadow is a function of the shadow caster height, the
     * alpha channel of the [ambientShadowColor] (typically opaque), and the
     * [android.R.attr.ambientShadowAlpha] theme attribute.
     *
     * Note that this parameter is only supported on Android 9 (Pie) and above. On older versions,
     * this property always returns [Color.Black] and setting new values is ignored.
     */
    actual var ambientShadowColor: Color = Color.Black

    /**
     * Sets the color of the spot shadow that is drawn when [shadowElevation] > 0f.
     *
     * By default the shadow color is black. Generally, this color will be opaque so the intensity
     * of the shadow is consistent between different graphics layers with different colors.
     *
     * The opacity of the final spot shadow is a function of the shadow caster height, the
     * alpha channel of the [spotShadowColor] (typically opaque), and the
     * [android.R.attr.spotShadowAlpha] theme attribute.
     *
     * Note that this parameter is only supported on Android 9 (Pie) and above. On older versions,
     * this property always returns [Color.Black] and setting new values is ignored.
     */
    actual var spotShadowColor: Color = Color.Black

    private fun requiresLayer(): Boolean {
        val alphaNeedsLayer = alpha < 1f && compositingStrategy != CompositingStrategy.ModulateAlpha
        val hasColorFilter = colorFilter != null
        val hasBlendMode = blendMode != BlendMode.SrcOver
        val hasRenderEffect = renderEffect != null
        val offscreenBufferRequested = compositingStrategy == CompositingStrategy.Offscreen
        return alphaNeedsLayer || hasColorFilter || hasBlendMode || hasRenderEffect ||
            offscreenBufferRequested
    }

    private fun drawShadow(canvas: Canvas) = with(density) {
        val path = when (val tmpOutline = internalOutline) {
            is Outline.Rectangle -> Path().apply { addRect(tmpOutline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(tmpOutline.roundRect) }
            is Outline.Generic -> tmpOutline.path
            else -> return
        }

        val zParams = Point3(0f, 0f, shadowElevation)

        val lightPos = Point3(0f, -300.dp.toPx(), 600.dp.toPx())
        val lightRad = 800.dp.toPx()

        val ambientAlpha = 0.039f * alpha
        val spotAlpha = 0.19f * alpha
        val ambientColor = ambientShadowColor.copy(alpha = ambientAlpha)
        val spotColor = spotShadowColor.copy(alpha = spotAlpha)

        org.jetbrains.skia.ShadowUtils.drawShadow(
            canvas.nativeCanvas, path.asSkiaPath(), zParams, lightPos,
            lightRad,
            ambientColor.toArgb(),
            spotColor.toArgb(), alpha < 1f, false
        )
    }

    actual companion object {
        actual val UnsetOffset: IntOffset = IntOffset(Int.MIN_VALUE, Int.MIN_VALUE)
        actual val UnsetSize: IntSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)
    }
}
