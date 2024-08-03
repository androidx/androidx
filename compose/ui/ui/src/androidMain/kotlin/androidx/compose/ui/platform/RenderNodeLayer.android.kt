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

package androidx.compose.ui.platform

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.CanvasHolder
import androidx.compose.ui.graphics.Fields
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.GraphicLayerInfo
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/** RenderNode implementation of OwnedLayer. */
@RequiresApi(Build.VERSION_CODES.M)
internal class RenderNodeLayer(
    val ownerView: AndroidComposeView,
    drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
    invalidateParentLayer: () -> Unit
) : OwnedLayer, GraphicLayerInfo {
    private var drawBlock: ((canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit)? = drawBlock
    private var invalidateParentLayer: (() -> Unit)? = invalidateParentLayer

    /** True when the RenderNodeLayer has been invalidated and not yet drawn. */
    private var isDirty = false
        set(value) {
            if (value != field) {
                field = value
                ownerView.notifyLayerIsDirty(this, value)
            }
        }

    private val outlineResolver = OutlineResolver()
    private var isDestroyed = false
    private var drawnWithZ = false

    /**
     * Optional paint used when the RenderNode is rendered on a software backed canvas and is
     * somewhat transparent (i.e. alpha less than 1.0f)
     */
    private var softwareLayerPaint: Paint? = null

    private val matrixCache = LayerMatrixCache(getMatrix)

    private val canvasHolder = CanvasHolder()

    /**
     * Local copy of the transform origin as GraphicsLayerModifier can be implemented as a model
     * object. Update this field within [updateLayerProperties] and use it in [resize] or other
     * methods
     */
    private var transformOrigin: TransformOrigin = TransformOrigin.Center

    private val renderNode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RenderNodeApi29(ownerView)
            } else {
                RenderNodeApi23(ownerView)
            }
            .apply {
                setHasOverlappingRendering(true)
                // in compose the default is to not clip.
                clipToBounds = false
            }

    override val layerId: Long
        get() = renderNode.uniqueId

    override val ownerViewId: Long
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                UniqueDrawingIdApi29.getUniqueDrawingId(ownerView)
            } else {
                -1
            }

    @RequiresApi(29)
    private object UniqueDrawingIdApi29 {
        @JvmStatic fun getUniqueDrawingId(view: View) = view.uniqueDrawingId
    }

    private var mutatedFields: Int = 0

    override fun updateLayerProperties(scope: ReusableGraphicsLayerScope) {
        val maybeChangedFields = scope.mutatedFields or mutatedFields
        if (maybeChangedFields and Fields.TransformOrigin != 0) {
            this.transformOrigin = scope.transformOrigin
        }
        val wasClippingManually = renderNode.clipToOutline && !outlineResolver.outlineClipSupported
        if (maybeChangedFields and Fields.ScaleX != 0) {
            renderNode.scaleX = scope.scaleX
        }
        if (maybeChangedFields and Fields.ScaleY != 0) {
            renderNode.scaleY = scope.scaleY
        }
        if (maybeChangedFields and Fields.Alpha != 0) {
            renderNode.alpha = scope.alpha
        }
        if (maybeChangedFields and Fields.TranslationX != 0) {
            renderNode.translationX = scope.translationX
        }
        if (maybeChangedFields and Fields.TranslationY != 0) {
            renderNode.translationY = scope.translationY
        }
        if (maybeChangedFields and Fields.ShadowElevation != 0) {
            renderNode.elevation = scope.shadowElevation
        }
        if (maybeChangedFields and Fields.AmbientShadowColor != 0) {
            renderNode.ambientShadowColor = scope.ambientShadowColor.toArgb()
        }
        if (maybeChangedFields and Fields.SpotShadowColor != 0) {
            renderNode.spotShadowColor = scope.spotShadowColor.toArgb()
        }
        if (maybeChangedFields and Fields.RotationZ != 0) {
            renderNode.rotationZ = scope.rotationZ
        }
        if (maybeChangedFields and Fields.RotationX != 0) {
            renderNode.rotationX = scope.rotationX
        }
        if (maybeChangedFields and Fields.RotationY != 0) {
            renderNode.rotationY = scope.rotationY
        }
        if (maybeChangedFields and Fields.CameraDistance != 0) {
            renderNode.cameraDistance = scope.cameraDistance
        }
        if (maybeChangedFields and Fields.TransformOrigin != 0) {
            renderNode.pivotX = transformOrigin.pivotFractionX * renderNode.width
            renderNode.pivotY = transformOrigin.pivotFractionY * renderNode.height
        }
        val clipToOutline = scope.clip && scope.shape !== RectangleShape
        if (maybeChangedFields and (Fields.Clip or Fields.Shape) != 0) {
            renderNode.clipToOutline = clipToOutline
            renderNode.clipToBounds = scope.clip && scope.shape === RectangleShape
        }
        if (maybeChangedFields and Fields.RenderEffect != 0) {
            renderNode.renderEffect = scope.renderEffect
        }
        if (maybeChangedFields and Fields.CompositingStrategy != 0) {
            renderNode.compositingStrategy = scope.compositingStrategy
        }
        val shapeChanged =
            outlineResolver.update(
                scope.outline,
                scope.alpha,
                clipToOutline,
                scope.shadowElevation,
                scope.size,
            )
        if (outlineResolver.cacheIsDirty) {
            renderNode.setOutline(outlineResolver.androidOutline)
        }
        val isClippingManually = clipToOutline && !outlineResolver.outlineClipSupported
        if (wasClippingManually != isClippingManually || (isClippingManually && shapeChanged)) {
            invalidate()
        } else {
            triggerRepaint()
        }
        if (!drawnWithZ && renderNode.elevation > 0f) {
            invalidateParentLayer?.invoke()
        }

        if (maybeChangedFields and Fields.MatrixAffectingFields != 0) {
            matrixCache.invalidate()
        }

        mutatedFields = scope.mutatedFields
    }

    override fun isInLayer(position: Offset): Boolean {
        val x = position.x
        val y = position.y
        if (renderNode.clipToBounds) {
            return 0f <= x && x < renderNode.width && 0f <= y && y < renderNode.height
        }

        if (renderNode.clipToOutline) {
            return outlineResolver.isInOutline(position)
        }

        return true
    }

    override fun resize(size: IntSize) {
        val width = size.width
        val height = size.height
        renderNode.pivotX = transformOrigin.pivotFractionX * width
        renderNode.pivotY = transformOrigin.pivotFractionY * height
        if (
            renderNode.setPosition(
                renderNode.left,
                renderNode.top,
                renderNode.left + width,
                renderNode.top + height
            )
        ) {
            renderNode.setOutline(outlineResolver.androidOutline)
            invalidate()
            matrixCache.invalidate()
        }
    }

    override fun move(position: IntOffset) {
        val oldLeft = renderNode.left
        val oldTop = renderNode.top
        val newLeft = position.x
        val newTop = position.y
        if (oldLeft != newLeft || oldTop != newTop) {
            if (oldLeft != newLeft) {
                renderNode.offsetLeftAndRight(newLeft - oldLeft)
            }
            if (oldTop != newTop) {
                renderNode.offsetTopAndBottom(newTop - oldTop)
            }
            triggerRepaint()
            matrixCache.invalidate()
        }
    }

    override fun invalidate() {
        if (!isDirty && !isDestroyed) {
            ownerView.invalidate()
            isDirty = true
        }
    }

    /**
     * This only triggers the system so that it knows that some kind of painting must happen without
     * actually causing the layer to be invalidated and have to re-record its drawing.
     */
    private fun triggerRepaint() {
        // onDescendantInvalidated is only supported on O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WrapperRenderNodeLayerHelperMethods.onDescendantInvalidated(ownerView)
        } else {
            ownerView.invalidate()
        }
    }

    override fun drawLayer(canvas: Canvas, parentLayer: GraphicsLayer?) {
        val androidCanvas = canvas.nativeCanvas
        if (androidCanvas.isHardwareAccelerated) {
            updateDisplayList()
            drawnWithZ = renderNode.elevation > 0f
            if (drawnWithZ) {
                canvas.enableZ()
            }
            renderNode.drawInto(androidCanvas)
            if (drawnWithZ) {
                canvas.disableZ()
            }
        } else {
            val left = renderNode.left.toFloat()
            val top = renderNode.top.toFloat()
            val right = renderNode.right.toFloat()
            val bottom = renderNode.bottom.toFloat()
            // If there is alpha applied, we must render into an offscreen buffer to
            // properly blend the contents of this layer against the background content
            if (renderNode.alpha < 1.0f) {
                val paint =
                    (softwareLayerPaint ?: Paint().also { softwareLayerPaint = it }).apply {
                        alpha = renderNode.alpha
                    }
                androidCanvas.saveLayer(left, top, right, bottom, paint.asFrameworkPaint())
            } else {
                canvas.save()
            }
            // If we are software rendered we must translate the canvas based on the offset provided
            // in the move call which operates directly on the RenderNode
            canvas.translate(left, top)
            canvas.concat(matrixCache.calculateMatrix(renderNode))
            clipRenderNode(canvas)
            drawBlock?.invoke(canvas, null)
            canvas.restore()
            isDirty = false
        }
    }

    /**
     * Manually clips the content of the RenderNodeLayer in the provided canvas. This is used only
     * in software rendered use cases
     */
    private fun clipRenderNode(canvas: Canvas) {
        if (renderNode.clipToOutline || renderNode.clipToBounds) {
            outlineResolver.clipToOutline(canvas)
        }
    }

    override fun updateDisplayList() {
        if (isDirty || !renderNode.hasDisplayList) {
            val clipPath =
                if (renderNode.clipToOutline && !outlineResolver.outlineClipSupported) {
                    outlineResolver.clipPath
                } else {
                    null
                }
            drawBlock?.let { drawBlock ->
                renderNode.record(canvasHolder, clipPath) { drawBlock(it, null) }
            }
            isDirty = false
        }
    }

    override fun destroy() {
        if (renderNode.hasDisplayList) {
            renderNode.discardDisplayList()
        }
        drawBlock = null
        invalidateParentLayer = null
        isDestroyed = true
        isDirty = false
        ownerView.requestClearInvalidObservations()
        ownerView.recycle(this)
    }

    override fun mapOffset(point: Offset, inverse: Boolean): Offset {
        return if (inverse) {
            matrixCache.calculateInverseMatrix(renderNode)?.map(point) ?: Offset.Infinite
        } else {
            matrixCache.calculateMatrix(renderNode).map(point)
        }
    }

    override fun mapBounds(rect: MutableRect, inverse: Boolean) {
        if (inverse) {
            val matrix = matrixCache.calculateInverseMatrix(renderNode)
            if (matrix == null) {
                rect.set(0f, 0f, 0f, 0f)
            } else {
                matrix.map(rect)
            }
        } else {
            matrixCache.calculateMatrix(renderNode).map(rect)
        }
    }

    override fun reuseLayer(
        drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
        invalidateParentLayer: () -> Unit
    ) {
        matrixCache.reset()
        isDirty = false
        isDestroyed = false
        drawnWithZ = false
        transformOrigin = TransformOrigin.Center
        this.drawBlock = drawBlock
        this.invalidateParentLayer = invalidateParentLayer
    }

    override fun transform(matrix: Matrix) {
        matrix.timesAssign(matrixCache.calculateMatrix(renderNode))
    }

    override fun inverseTransform(matrix: Matrix) {
        val inverse = matrixCache.calculateInverseMatrix(renderNode)
        if (inverse != null) {
            matrix.timesAssign(inverse)
        }
    }

    companion object {
        private val getMatrix: (DeviceRenderNode, android.graphics.Matrix) -> Unit = { rn, matrix ->
            rn.getMatrix(matrix)
        }
    }
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be AOT
 * compiled. It is expected that this class will soft-fail verification, but the classes which use
 * this method will pass.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal object WrapperRenderNodeLayerHelperMethods {
    fun onDescendantInvalidated(ownerView: AndroidComposeView) {
        ownerView.parent?.onDescendantInvalidated(ownerView, ownerView)
    }
}
