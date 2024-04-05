/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.platform

import org.jetbrains.skia.Rect as SkRect
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.DefaultCameraDistance
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.Fields
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.alphaMultiplier
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.asSkiaPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toSkiaRRect
import androidx.compose.ui.graphics.toSkiaRect
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlin.math.max
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.Picture
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.Point3
import org.jetbrains.skia.RTreeFactory
import org.jetbrains.skia.ShadowUtils

internal class RenderNodeLayer(
    private var density: Density,
    measureDrawBounds: Boolean,
    private val invalidateParentLayer: () -> Unit,
    private val drawBlock: (Canvas) -> Unit,
    private val onDestroy: () -> Unit = {}
) : OwnedLayer {
    private var size = IntSize.Zero
    private var position = IntOffset.Zero
    private var outlineCache =
        OutlineCache(density, size, RectangleShape, LayoutDirection.Ltr)
    // Internal for testing
    internal val matrix = Matrix()
    private val inverseMatrix: Matrix
        get() = Matrix().apply {
            matrix.invertTo(this)
        }

    private val pictureRecorder = PictureRecorder()
    // Use factory for BBoxHierarchy to track real bounds of drawn content
    private val bbhFactory = if (measureDrawBounds) RTreeFactory() else null
    private var picture: Picture? = null
    private var isDestroyed = false

    private var transformOrigin: TransformOrigin = TransformOrigin.Center
    private var translationX: Float = 0f
    private var translationY: Float = 0f
    private var rotationX: Float = 0f
    private var rotationY: Float = 0f
    private var rotationZ: Float = 0f
    private var cameraDistance: Float = DefaultCameraDistance
    private var scaleX: Float = 1f
    private var scaleY: Float = 1f
    private var alpha: Float = 1f
    private var clip: Boolean = false
    private var renderEffect: RenderEffect? = null
    private var shadowElevation: Float = 0f
    private var ambientShadowColor: Color = DefaultShadowColor
    private var spotShadowColor: Color = DefaultShadowColor
    private var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto

    override fun destroy() {
        picture?.close()
        pictureRecorder.close()
        isDestroyed = true
        onDestroy()
    }

    override fun reuseLayer(drawBlock: (Canvas) -> Unit, invalidateParentLayer: () -> Unit) {
        // TODO: in destroy, call recycle, and reconfigure this layer to be ready to use here.
    }

    override fun resize(size: IntSize) {
        if (size != this.size) {
            this.size = size
            outlineCache.size = size
            updateMatrix()
            invalidate()
        }
    }

    override fun move(position: IntOffset) {
        if (position != this.position) {
            this.position = position
            invalidateParentLayer()
        }
    }

    override fun mapOffset(point: Offset, inverse: Boolean): Offset {
        return if (inverse) {
            inverseMatrix
        } else {
            matrix
        }.map(point)
    }

    override fun mapBounds(rect: MutableRect, inverse: Boolean) {
        if (inverse) {
            inverseMatrix
        } else {
            matrix
        }.map(rect)
    }

    override fun isInLayer(position: Offset): Boolean {
        if (!clip) {
            return true
        }

        val x = position.x
        val y = position.y
        if (outlineCache.shape === RectangleShape) {
            return 0f <= x && x < size.width && 0f <= y && y < size.height
        }

        return isInOutline(outlineCache.outline, x, y)
    }

    private var mutatedFields: Int = 0

    override fun updateLayerProperties(
        scope: ReusableGraphicsLayerScope,
        layoutDirection: LayoutDirection,
        density: Density,
    ) {
        val maybeChangedFields = scope.mutatedFields or mutatedFields
        this.transformOrigin = scope.transformOrigin
        this.translationX = scope.translationX
        this.translationY = scope.translationY
        this.rotationX = scope.rotationX
        this.rotationY = scope.rotationY
        this.rotationZ = scope.rotationZ
        this.cameraDistance = max(scope.cameraDistance, 0.001f)
        this.scaleX = scope.scaleX
        this.scaleY = scope.scaleY
        this.alpha = scope.alpha
        this.clip = scope.clip
        this.shadowElevation = scope.shadowElevation
        this.density = density
        this.renderEffect = scope.renderEffect
        this.ambientShadowColor = scope.ambientShadowColor
        this.spotShadowColor = scope.spotShadowColor
        this.compositingStrategy = scope.compositingStrategy
        outlineCache.shape = scope.shape
        outlineCache.layoutDirection = layoutDirection
        outlineCache.density = density
        if (maybeChangedFields and Fields.MatrixAffectingFields != 0) {
            updateMatrix()
        }
        invalidate()
        mutatedFields = scope.mutatedFields
    }

    private fun updateMatrix() {
        val pivotX = transformOrigin.pivotFractionX * size.width
        val pivotY = transformOrigin.pivotFractionY * size.height

        matrix.reset()
        matrix.translate(x = -pivotX, y = -pivotY)
        matrix *= Matrix().apply {
            rotateZ(rotationZ)
            rotateY(rotationY)
            rotateX(rotationX)
            scale(scaleX, scaleY)
        }
        // Perspective transform should be applied only in case of rotations to avoid
        // multiply application in hierarchies.
        // See Android's frameworks/base/libs/hwui/RenderProperties.cpp for reference
        if (!rotationX.isZero() || !rotationY.isZero()) {
            matrix *= Matrix().apply {
                // The camera location is passed in inches, set in pt
                val depth = cameraDistance * 72f
                this[2, 3] = -1f / depth
            }
        }
        matrix *= Matrix().apply {
            translate(x = pivotX + translationX, y = pivotY + translationY)
        }

        // Third column and row are irrelevant for 2D space.
        // Zeroing required to get correct inverse transformation matrix.
        matrix[2, 0] = 0f
        matrix[2, 1] = 0f
        matrix[2, 3] = 0f
        matrix[0, 2] = 0f
        matrix[1, 2] = 0f
        matrix[3, 2] = 0f
    }

    override fun invalidate() {
        if (!isDestroyed && picture != null) {
            picture?.close()
            picture = null
            invalidateParentLayer()
        }
    }

    override fun drawLayer(canvas: Canvas) {
        if (picture == null) {
            val bounds = size.toSize().toRect()
            val pictureCanvas = pictureRecorder.beginRecording(
                bounds = if (clip) bounds.toSkiaRect() else PICTURE_BOUNDS,
                bbh = if (clip) null else bbhFactory
            )
            performDrawLayer(pictureCanvas.asComposeCanvas(), bounds)
            picture = pictureRecorder.finishRecordingAsPicture()
        }

        canvas.save()
        canvas.concat(matrix)
        canvas.translate(position.x.toFloat(), position.y.toFloat())
        canvas.nativeCanvas.drawPicture(picture!!, null, null)
        canvas.restore()
    }

    override fun transform(matrix: Matrix) {
        matrix.timesAssign(this.matrix)
    }

    override fun inverseTransform(matrix: Matrix) {
        matrix.timesAssign(inverseMatrix)
    }

    private fun performDrawLayer(canvas: Canvas, bounds: Rect) {
        if (alpha > 0) {
            if (shadowElevation > 0) {
                drawShadow(canvas)
            }

            if (clip) {
                canvas.save()
                when (val outline = outlineCache.outline) {
                    is Outline.Rectangle -> canvas.clipRect(outline.rect)
                    is Outline.Rounded -> canvas.clipRoundRect(outline.roundRect)
                    is Outline.Generic -> canvas.clipPath(outline.path)
                }
            }

            val currentRenderEffect = renderEffect
            val requiresLayer =
                (alpha < 1 && compositingStrategy != CompositingStrategy.ModulateAlpha) ||
                    currentRenderEffect != null ||
                    compositingStrategy == CompositingStrategy.Offscreen
            if (requiresLayer) {
                canvas.saveLayer(
                    bounds,
                    Paint().apply {
                        alpha = this@RenderNodeLayer.alpha
                        asFrameworkPaint().imageFilter = currentRenderEffect?.asSkiaImageFilter()
                    }
                )
            } else {
                canvas.save()
            }
            canvas.alphaMultiplier = if (compositingStrategy == CompositingStrategy.ModulateAlpha) {
                alpha
            } else {
                1.0f
            }

            drawBlock(canvas)
            canvas.restore()
            if (clip) {
                canvas.restore()
            }
        }
    }

    private fun Canvas.clipRoundRect(rect: RoundRect, clipOp: ClipOp = ClipOp.Intersect) {
        val antiAlias = true
        nativeCanvas.clipRRect(rect.toSkiaRRect(), clipOp.toSkia(), antiAlias)
    }

    private fun ClipOp.toSkia() = when (this) {
        ClipOp.Difference -> ClipMode.DIFFERENCE
        ClipOp.Intersect -> ClipMode.INTERSECT
        else -> ClipMode.INTERSECT
    }

    override fun updateDisplayList() = Unit

    fun drawShadow(canvas: Canvas) = with(density) {
        val path = when (val outline = outlineCache.outline) {
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Generic -> outline.path
            else -> return
        }

        // TODO: perspective?
        val zParams = Point3(0f, 0f, shadowElevation)

        // TODO: configurable?
        val lightPos = Point3(0f, -300.dp.toPx(), 600.dp.toPx())
        val lightRad = 800.dp.toPx()

        val ambientAlpha = 0.039f * alpha
        val spotAlpha = 0.19f * alpha
        val ambientColor = ambientShadowColor.copy(alpha = ambientAlpha)
        val spotColor = spotShadowColor.copy(alpha = spotAlpha)

        ShadowUtils.drawShadow(
            canvas.nativeCanvas, path.asSkiaPath(), zParams, lightPos,
            lightRad,
            ambientColor.toArgb(),
            spotColor.toArgb(), alpha < 1f, false
        )
    }
}

// Copy from Android's frameworks/base/libs/hwui/utils/MathUtils.h
private const val NON_ZERO_EPSILON = 0.001f
private inline fun Float.isZero(): Boolean = abs(this) <= NON_ZERO_EPSILON

// The goal with selecting the size of the rectangle here is to avoid limiting the
// drawable area as much as possible.
// Due to https://partnerissuetracker.corp.google.com/issues/324465764 we have to
// leave room for scale between the values we specify here and Float.MAX_VALUE.
// The maximum possible scale that can be applied to the canvas will be
// Float.MAX_VALUE divided by the largest value below.
// 2^30 was chosen because it's big enough, leaves quite a lot of room between it
// and Float.MAX_VALUE, and also lets the width and height fit into int32 (just in
// case).
private const val PICTURE_MIN_VALUE = -(1 shl 30).toFloat()
private const val PICTURE_MAX_VALUE = ((1 shl 30)-1).toFloat()
private val PICTURE_BOUNDS = SkRect.makeLTRB(
    l = PICTURE_MIN_VALUE,
    t = PICTURE_MIN_VALUE,
    r = PICTURE_MAX_VALUE,
    b = PICTURE_MAX_VALUE
)
