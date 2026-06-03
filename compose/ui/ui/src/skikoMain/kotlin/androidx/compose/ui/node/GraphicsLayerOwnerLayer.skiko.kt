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

package androidx.compose.ui.node

import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Fields
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.graphics.SkiaGraphicsContext
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isIdentity
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.layer.setOutline
import androidx.compose.ui.graphics.prepareTransformationMatrix
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.platform.invertTo
import androidx.compose.ui.platform.isInOutline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A class representing a layer that owns and manages a `GraphicsLayer` for composable rendering.
 *
 * This layer is responsible for managing graphical properties, transformations, and rendering
 * tasks associated with a `GraphicsLayer`. It provides mechanisms for updating layer properties,
 * triggering invalidations, resizing, and mapping offsets or bounds using transformation matrices.
 *
 * @constructor
 * @param graphicsLayer The initial `GraphicsLayer` for this layer.
 * @param context The graphics context responsible for managing this layer, or `null` if externally
 *   managed. When context is not null, it means the object is created internally, and we need to
 *   release it during disposal.
 * @param layerManager The manager responsible for handling the ownership and lifecycle of this layer.
 * @param drawBlock The lambda function invoked to perform custom drawing on the canvas.
 * @param invalidateParentLayer Callback to invalidate the parent layer when necessary.
 */
internal class GraphicsLayerOwnerLayer(
    graphicsLayer: GraphicsLayer,
    private val context: GraphicsContext?,
    private val layerManager: OwnedLayerManager,
    drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
    invalidateParentLayer: () -> Unit,
) : OwnedLayer {
    internal var graphicsLayer: GraphicsLayer = graphicsLayer
        private set
    private var drawBlock: ((canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit)? = drawBlock
    private var invalidateParentLayer: (() -> Unit)? = invalidateParentLayer

    private var size: IntSize = IntSize(Int.MAX_VALUE, Int.MAX_VALUE)
    private var density = Density(1f)
    private var layoutDirection = LayoutDirection.Ltr
    private val scope = CanvasDrawScope()
    private var mutatedFields: Int = 0
    private var transformOrigin: TransformOrigin = TransformOrigin.Center
    private var outline: Outline? = null

    private val matrixCache = Matrix()
    private var inverseMatrixCache: Matrix? = null

    private var isDestroyed = false
    private var isDirty = true
        set(value) {
            if (value != field) {
                field = value
                layerManager.notifyLayerIsDirty(this, value)
            }
        }
    private var isMatrixDirty = false
    private var isInverseMatrixDirty = false
    private var isIdentity = true
    override var frameRate: Float = 0f
    override var isFrameRateFromParent = false

    override fun updateLayerProperties(scope: ReusableGraphicsLayerScope) {
        val maybeChangedFields = scope.mutatedFields or mutatedFields
        this.layoutDirection = scope.layoutDirection
        this.density = scope.graphicsDensity
        if (maybeChangedFields and Fields.TransformOrigin != 0) {
            this.transformOrigin = scope.transformOrigin
        }
        if (maybeChangedFields and Fields.ScaleX != 0) {
            graphicsLayer.scaleX = scope.scaleX
        }
        if (maybeChangedFields and Fields.ScaleY != 0) {
            graphicsLayer.scaleY = scope.scaleY
        }
        if (maybeChangedFields and Fields.Alpha != 0) {
            graphicsLayer.alpha = scope.alpha
        }
        if (maybeChangedFields and Fields.TranslationX != 0) {
            graphicsLayer.translationX = scope.translationX
        }
        if (maybeChangedFields and Fields.TranslationY != 0) {
            graphicsLayer.translationY = scope.translationY
        }
        if (maybeChangedFields and Fields.ShadowElevation != 0) {
            graphicsLayer.shadowElevation = scope.shadowElevation
        }
        if (maybeChangedFields and Fields.AmbientShadowColor != 0) {
            graphicsLayer.ambientShadowColor = scope.ambientShadowColor
        }
        if (maybeChangedFields and Fields.SpotShadowColor != 0) {
            graphicsLayer.spotShadowColor = scope.spotShadowColor
        }
        if (maybeChangedFields and Fields.RotationZ != 0) {
            graphicsLayer.rotationZ = scope.rotationZ
        }
        if (maybeChangedFields and Fields.RotationX != 0) {
            graphicsLayer.rotationX = scope.rotationX
        }
        if (maybeChangedFields and Fields.RotationY != 0) {
            graphicsLayer.rotationY = scope.rotationY
        }
        if (maybeChangedFields and Fields.CameraDistance != 0) {
            graphicsLayer.cameraDistance = scope.cameraDistance
        }
        if (maybeChangedFields and Fields.TransformOrigin != 0) {
            if (transformOrigin == TransformOrigin.Center) {
                graphicsLayer.pivotOffset = Offset.Unspecified
            } else {
                graphicsLayer.pivotOffset =
                    Offset(
                        transformOrigin.pivotFractionX * size.width,
                        transformOrigin.pivotFractionY * size.height
                    )
            }
        }
        if (maybeChangedFields and Fields.Clip != 0) {
            graphicsLayer.clip = scope.clip
        }
        if (maybeChangedFields and Fields.RenderEffect != 0) {
            graphicsLayer.renderEffect = scope.renderEffect
        }
        if (maybeChangedFields and Fields.ColorFilter != 0) {
            graphicsLayer.colorFilter = scope.colorFilter
        }
        if (maybeChangedFields and Fields.BlendMode != 0) {
            graphicsLayer.blendMode = scope.blendMode
        }
        if (maybeChangedFields and Fields.CompositingStrategy != 0) {
            graphicsLayer.compositingStrategy =
                when (scope.compositingStrategy) {
                    CompositingStrategy.Auto -> androidx.compose.ui.graphics.layer.CompositingStrategy.Auto
                    CompositingStrategy.Offscreen -> androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
                    CompositingStrategy.ModulateAlpha -> androidx.compose.ui.graphics.layer.CompositingStrategy.ModulateAlpha
                    else -> throw IllegalStateException("Not supported composition strategy")
                }
        }
        if (maybeChangedFields and Fields.MatrixAffectingFields != 0) {
            isMatrixDirty = true
            isInverseMatrixDirty = true
        }

        var outlineChanged = false

        if (outline != scope.outline) {
            outlineChanged = true
            outline = scope.outline
            updateOutline()
        }

        mutatedFields = scope.mutatedFields
        if (maybeChangedFields != 0 || outlineChanged) {
            triggerRepaint()
            layerManager.voteFrameRate(frameRate)
        }
    }

    /**
     * Triggers redrawing of Compose content during the next frame.
     */
    private fun triggerRepaint() {
        layerManager.invalidate()
    }

    private fun updateOutline() {
        val outline = outline ?: return
        graphicsLayer.setOutline(outline)
    }

    override fun isInLayer(position: Offset): Boolean {
        val x = position.x
        val y = position.y

        if (graphicsLayer.clip) {
            return isInOutline(graphicsLayer.outline, x, y)
        }

        return true
    }

    override fun move(position: IntOffset) {
        layerManager.voteFrameRate(FrameRateCategory.High.value)
        graphicsLayer.topLeft = position
        triggerRepaint()
    }

    override fun resize(size: IntSize) {
        if (size != this.size) {
            layerManager.voteFrameRate(FrameRateCategory.High.value)
            this.size = size
            invalidate()
        }
    }

    override fun drawLayer(canvas: Canvas, parentLayer: GraphicsLayer?) {
        updateDisplayList()
        scope.drawContext.also {
            it.canvas = canvas
            it.graphicsLayer = parentLayer
        }
        scope.drawLayer(graphicsLayer)
    }

    override fun updateDisplayList() {
        layerManager.voteFrameRate(frameRate)
        if (isDirty) {
            if (transformOrigin != TransformOrigin.Center && graphicsLayer.size != size) {
                graphicsLayer.pivotOffset =
                    Offset(
                        transformOrigin.pivotFractionX * size.width,
                        transformOrigin.pivotFractionY * size.height
                    )
            }
            graphicsLayer.record(density, layoutDirection, size, recordLambda)
            isDirty = false
        }
    }

    private val recordLambda: DrawScope.() -> Unit = {
        drawIntoCanvas { canvas ->
            this@GraphicsLayerOwnerLayer.drawBlock?.let { it(canvas, drawContext.graphicsLayer) }
        }
    }

    /**
     * Marks content as dirty and triggers redrawing.
     */
    override fun invalidate() {
        if (isDestroyed) return
        isDirty = true
        layerManager.invalidate()
    }

    override fun destroy() {
        frameRate = 0f
        isFrameRateFromParent = false
        drawBlock = null
        invalidateParentLayer = null
        isDestroyed = true
        isDirty = false
        if (context != null) {
            context.releaseGraphicsLayer(graphicsLayer)

            // Recycle only in case of non-null context (meaning only for not external layers).
            layerManager.recycle(this)
        }
    }

    override fun mapOffset(point: Offset, inverse: Boolean): Offset {
        val matrix =
            if (inverse) {
                getInverseMatrix() ?: return Offset.Infinite
            } else {
                getMatrix()
            }
        return if (isIdentity) {
            point
        } else {
            matrix.map(point)
        }
    }

    override fun mapBounds(rect: MutableRect, inverse: Boolean) {
        val matrix = if (inverse) getInverseMatrix() else getMatrix()
        if (!isIdentity) {
            if (matrix == null) {
                rect.set(0f, 0f, 0f, 0f)
            } else {
                matrix.map(rect)
            }
        }
    }

    override fun reuseLayer(
        drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
        invalidateParentLayer: () -> Unit
    ) {
        val context =
            checkPreconditionNotNull(context) {
                "currently reuse is only supported when we manage the layer lifecycle"
            }
        requirePrecondition(graphicsLayer.isReleased) {
            "layer should have been released before reuse"
        }

        // recreate a layer
        graphicsLayer = context.createGraphicsLayer()
        isDestroyed = false

        // apply new params
        this.drawBlock = drawBlock
        this.invalidateParentLayer = invalidateParentLayer

        // reset mutable variables to their initial values
        isMatrixDirty = false
        isInverseMatrixDirty = false
        isIdentity = true
        matrixCache.reset()
        inverseMatrixCache?.reset()
        transformOrigin = TransformOrigin.Center
        size = IntSize(Int.MAX_VALUE, Int.MAX_VALUE)
        outline = null
        mutatedFields = 0
    }

    override fun transform(matrix: Matrix) {
        matrix.timesAssign(getMatrix())
    }

    override val underlyingMatrix: Matrix
        get() = getMatrix()

    override fun inverseTransform(matrix: Matrix) {
        val inverse = getInverseMatrix()
        if (inverse != null) {
            matrix.timesAssign(inverse)
        }
    }

    private fun getMatrix(): Matrix {
        updateMatrix()
        return matrixCache
    }

    private fun getInverseMatrix(): Matrix? {
        val matrix = getMatrix()
        if (isIdentity) {
            return matrix
        }

        val inverseMatrix = inverseMatrixCache ?: Matrix().also { inverseMatrixCache = it }
        if (!isInverseMatrixDirty) {
            return if (inverseMatrix[0, 0].isNaN()) {
                null
            } else {
                inverseMatrix
            }
        }

        isInverseMatrixDirty = false
        return if (matrix.invertTo(inverseMatrix)) {
            inverseMatrix
        } else {
            inverseMatrix[0, 0] = Float.NaN
            null
        }
    }

    private fun updateMatrix() {
        if (!isMatrixDirty) return
        with(graphicsLayer) {
            val pivotX: Float
            val pivotY: Float
            if (pivotOffset.isUnspecified) {
                pivotX = this@GraphicsLayerOwnerLayer.size.width / 2f
                pivotY = this@GraphicsLayerOwnerLayer.size.height / 2f
            } else {
                pivotX = pivotOffset.x
                pivotY = pivotOffset.y
            }
            prepareTransformationMatrix(
                matrix = matrixCache,
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
        isMatrixDirty = false
        isIdentity = matrixCache.isIdentity()
    }
}

internal fun SkiaGraphicsContext.setLightingInfo(
    canvasOffset: Offset,
    density: Density,
    containerSize: IntSize
) = with(density) {
    // Adoption of android.view.ThreadedRenderer.setLightCenter
    val lightX = containerSize.width / 2f - canvasOffset.x
    val lightY = LIGHT_Y.toPx() - canvasOffset.y
    // To prevent shadow distortion on larger screens, scale the z position of the light source
    // relative to the smallest screen dimension.
    val zRatio = kotlin.math.min(containerSize.width, containerSize.height).toFloat() / 450.dp.toPx()
    val zWeightedAdjustment = (zRatio + 2) / 3f
    val lightZ = LIGHT_Z.toPx() * zWeightedAdjustment

    setLightingInfo(
        centerX = lightX,
        centerY = lightY,
        centerZ = lightZ,
        radius = LIGHT_RADIUS.toPx(),
        ambientShadowAlpha = AMBIENT_SHADOW_ALPHA,
        spotShadowAlpha = SPOT_SHADOW_ALPHA
    )
}

// Values from core/res/res/values/dimens.xml
private val LIGHT_Y = 0.dp
private val LIGHT_Z = 500.dp
private val LIGHT_RADIUS = 800.dp
private const val AMBIENT_SHADOW_ALPHA = 0.039f
private const val SPOT_SHADOW_ALPHA = 0.19f
