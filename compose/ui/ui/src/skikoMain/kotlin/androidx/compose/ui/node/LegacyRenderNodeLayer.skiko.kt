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

package androidx.compose.ui.node

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.DefaultCameraDistance
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.Fields
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.alphaMultiplier
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.graphics.skiaPaint
import androidx.compose.ui.graphics.prepareTransformationMatrix
import androidx.compose.ui.graphics.skiaImageFilter
import androidx.compose.ui.graphics.materializeSkiaPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.invertTo
import androidx.compose.ui.platform.isInOutline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toRect
import kotlin.math.max
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.Picture
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.Point3
import org.jetbrains.skia.RTreeFactory
import org.jetbrains.skia.ShadowUtils

/**
 * Maintained for compatibility purposes.
 *
 * @see androidx.compose.ui.SkikoComposeUiFlags.useLegacyRenderNodeLayers
 * @see androidx.compose.ui.node.GraphicsLayerOwnerLayer
 */
internal class LegacyRenderNodeLayer(
    private var density: Density,
    measureDrawBounds: Boolean,
    private val layerManager: OwnedLayerManager,
    private val requiresStateWorkaround: () -> Boolean,
    private val invalidateParentLayer: () -> Unit,
    private val drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
) : OwnedLayer {
    private var size = IntSize.Zero
    private var position = IntOffset.Zero
    private var outline: Outline? = null
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

    override var frameRate: Float = 0f
    override var isFrameRateFromParent = false

    // Composable state marker for tracking drawing invalidations.
    private val drawState = mutableStateOf(Unit, object : SnapshotMutationPolicy<Unit> {
        override fun equivalent(a: Unit, b: Unit): Boolean = false
        override fun merge(previous: Unit, current: Unit, applied: Unit) = current
    })

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
    private var outsetLeft: Int = 0
    private var outsetTop: Int = 0
    private var outsetRight: Int = 0
    private var outsetBottom: Int = 0

    override fun destroy() {
        picture?.close()
        pictureRecorder.close()
        isDestroyed = true
        layerManager.recycle(this)
    }

    override fun reuseLayer(
        drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
        invalidateParentLayer: () -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override fun resize(size: IntSize) {
        if (size != this.size) {
            layerManager.voteFrameRate(FrameRateCategory.High.value)
            this.size = size
            updateMatrix()
            invalidate()
        }
    }

    override fun move(position: IntOffset) {
        layerManager.voteFrameRate(FrameRateCategory.High.value)
        if (position != this.position) {
            this.position = position
            invalidateParentLayer()
            layerManager.invalidate()
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
        val outline = outline ?: return true

        return isInOutline(outline, x, y)
    }

    private var mutatedFields: Int = 0

    override fun updateLayerProperties(scope: ReusableGraphicsLayerScope) {
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
        this.density = scope.graphicsDensity
        this.renderEffect = scope.renderEffect
        this.ambientShadowColor = scope.ambientShadowColor
        this.spotShadowColor = scope.spotShadowColor
        this.compositingStrategy = scope.compositingStrategy
        this.outline = scope.outline
        if (maybeChangedFields and Fields.Outsets != 0) {
            with(density) {
                outsetLeft = scope.outsets.left.roundToPx()
                outsetTop = scope.outsets.top.roundToPx()
                outsetRight = scope.outsets.right.roundToPx()
                outsetBottom = scope.outsets.bottom.roundToPx()
            }
        }
        if (maybeChangedFields and Fields.MatrixAffectingFields != 0) {
            updateMatrix()
        }
        mutatedFields = scope.mutatedFields
        invalidate()
        layerManager.voteFrameRate(frameRate)
    }

    private fun updateMatrix() {
        val pivotX = transformOrigin.pivotFractionX * size.width
        val pivotY = transformOrigin.pivotFractionY * size.height
        prepareTransformationMatrix(
            matrix = matrix,
            pivotX = pivotX,
            pivotY = pivotY,
            translationX = translationX,
            translationY = translationY,
            rotationX = rotationX,
            rotationY = rotationY,
            rotationZ = rotationZ,
            scaleX = scaleX,
            scaleY = scaleY,
            cameraDistance = cameraDistance
        )
    }

    override fun invalidate() {
        if (!isDestroyed && picture != null) {
            picture?.close()
            picture = null
        }
        if (requiresStateWorkaround()) {
            drawState.value = Unit
        }
        invalidateParentLayer()
        layerManager.invalidate()
    }

    override fun drawLayer(canvas: Canvas, parentLayer: GraphicsLayer?) {
        if (requiresStateWorkaround() && parentLayer != null) {
            // Read the state because any changes to the state should trigger re-drawing of [GraphicsLayer].
            drawState.value
        }

        layerManager.voteFrameRate(frameRate)

        if (picture == null) {
            val measureDrawBounds = !clip || shadowElevation > 0 || hasOutsets()
            val bounds = if (hasOutsets()) {
                Rect(
                    -outsetLeft.toFloat(),
                    -outsetTop.toFloat(),
                    size.width + outsetRight.toFloat(),
                    size.height + outsetBottom.toFloat()
                )
            } else {
                size.toRect()
            }
            val pictureCanvas = pictureRecorder.beginRecording(
                left = if (measureDrawBounds) PICTURE_MIN_VALUE else bounds.left,
                top = if (measureDrawBounds) PICTURE_MIN_VALUE else bounds.top,
                right = if (measureDrawBounds) PICTURE_MAX_VALUE else bounds.right,
                bottom = if (measureDrawBounds) PICTURE_MAX_VALUE else bounds.bottom,
                bbh = if (measureDrawBounds) bbhFactory else null
            )
            performDrawLayer(pictureCanvas.asComposeCanvas(), bounds)
            picture = pictureRecorder.finishRecordingAsPicture()
        }

        canvas.save()
        canvas.concat(matrix)
        canvas.translate(position.x.toFloat(), position.y.toFloat())
        canvas.skiaCanvas.drawPicture(picture!!, null, null)
        canvas.restore()
    }

    override fun transform(matrix: Matrix) {
        matrix.timesAssign(this.matrix)
    }

    override val underlyingMatrix: Matrix
        get() = matrix

    override fun inverseTransform(matrix: Matrix) {
        matrix.timesAssign(inverseMatrix)
    }

    private fun performDrawLayer(canvas: Canvas, bounds: Rect) {
        if (alpha > 0) {
            if (shadowElevation > 0) {
                drawShadow(canvas)
            }

            val outline = outline
            val isClipping = if (clip && outline != null) {
                canvas.save()
                when (outline) {
                    is Outline.Rectangle -> canvas.clipRect(
                        outline.rect.left,
                        outline.rect.top,
                        outline.rect.right,
                        outline.rect.bottom
                    )
                    is Outline.Rounded -> {
                        val antiAlias = true
                        canvas.skiaCanvas.clipRRect(
                            outline.roundRect.left,
                            outline.roundRect.top,
                            outline.roundRect.right,
                            outline.roundRect.bottom,
                            floatArrayOf(
                                outline.roundRect.topLeftCornerRadius.x,
                                outline.roundRect.topLeftCornerRadius.y,
                                outline.roundRect.topRightCornerRadius.x,
                                outline.roundRect.topRightCornerRadius.y,
                                outline.roundRect.bottomRightCornerRadius.x,
                                outline.roundRect.bottomRightCornerRadius.y,
                                outline.roundRect.bottomLeftCornerRadius.x,
                                outline.roundRect.bottomLeftCornerRadius.y
                            ),

                            ClipMode.INTERSECT,
                            antiAlias
                        )
                    }
                    is Outline.Generic -> canvas.clipPath(outline.path)
                }
                true
            } else {
                false
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
                        alpha = this@LegacyRenderNodeLayer.alpha
                        skiaPaint.imageFilter = currentRenderEffect?.skiaImageFilter
                    }
                )
            } else {
                canvas.save()
            }
            @OptIn(InternalComposeApi::class)
            @Suppress("DEPRECATION_ERROR")
            canvas.alphaMultiplier = if (compositingStrategy == CompositingStrategy.ModulateAlpha) {
                alpha
            } else {
                1.0f
            }

            drawBlock(canvas, null)
            canvas.restore()
            if (isClipping) {
                canvas.restore()
            }
        }
    }

    override fun updateDisplayList() = Unit

    private fun hasOutsets() =
        outsetLeft > 0 || outsetTop > 0 || outsetRight > 0 || outsetBottom > 0

    @OptIn(InternalComposeUiApi::class)
    fun drawShadow(canvas: Canvas) = with(density) {
        val path = when (val outline = outline) {
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Generic -> outline.path
            else -> return
        }

        val zParams = Point3(0f, 0f, shadowElevation)
        val lightPos = Point3(0f, -300.dp.toPx(), 600.dp.toPx())
        val lightRad = 800.dp.toPx()

        val ambientAlpha = 0.039f * alpha
        val spotAlpha = 0.19f * alpha
        val ambientColor = ambientShadowColor.copy(alpha = ambientAlpha)
        val spotColor = spotShadowColor.copy(alpha = spotAlpha)

        ShadowUtils.drawShadow(
            canvas.skiaCanvas, path.materializeSkiaPath(), zParams, lightPos,
            lightRad,
            ambientColor.toArgb(),
            spotColor.toArgb(), alpha < 1f, false
        )
    }
}

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
