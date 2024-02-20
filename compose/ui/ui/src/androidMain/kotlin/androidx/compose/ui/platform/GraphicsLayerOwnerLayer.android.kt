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

package androidx.compose.ui.platform

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.internal.throwIllegalStateException
import androidx.compose.ui.layout.GraphicLayerInfo
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize

internal class GraphicsLayerOwnerLayer(
    private val graphicsLayer: GraphicsLayer,
    private val ownerView: AndroidComposeView,
    drawBlock: (Canvas) -> Unit,
    invalidateParentLayer: () -> Unit
) : OwnedLayer, GraphicLayerInfo {
    private var drawBlock: ((Canvas) -> Unit)? = drawBlock
    private var invalidateParentLayer: (() -> Unit)? = invalidateParentLayer

    private var size: IntSize = IntSize.Zero
    private var isDestroyed = false
    private val matrixCache = Matrix()

    private var isDirty = true
        set(value) {
            if (value != field) {
                field = value
                ownerView.notifyLayerIsDirty(this, value)
            }
        }

    private var density = Density(1f)
    private var layoutDirection = LayoutDirection.Ltr
    private val scope = CanvasDrawScope()

    private var tmpTouchPointPath: Path? = null
    private var tmpOpPath: Path? = null

    override fun updateLayerProperties(
        scope: ReusableGraphicsLayerScope,
        layoutDirection: LayoutDirection,
        density: Density,
    ) {
        throwIllegalStateException(
            "Current apis doesn't allow for both GraphicsLayer and GraphicsLayerScope to be " +
                "used together"
        )
    }

    override fun isInLayer(position: Offset): Boolean {
        val x = position.x
        val y = position.y

        if (graphicsLayer.clip) {
            val outline = graphicsLayer.outline
            return isInOutline(outline, x, y, tmpTouchPointPath, tmpOpPath)
        }

        return true
    }

    override fun move(position: IntOffset) {
        graphicsLayer.topLeft = position
    }

    override fun resize(size: IntSize) {
        if (size != this.size) {
            this.size = size
            invalidate()
        }
    }

    override fun drawLayer(canvas: Canvas, parentLayer: GraphicsLayer?) {
        updateDisplayList()
        scope.draw(density, layoutDirection, canvas, size.toSize(), parentLayer) {
            drawLayer(graphicsLayer)
        }
    }

    override fun updateDisplayList() {
        if (isDirty) {
            graphicsLayer.buildLayer(density, layoutDirection, size) {
                drawIntoCanvas { canvas ->
                    drawBlock?.let { it(canvas) }
                }
            }
            isDirty = false
        }
    }

    override fun invalidate() {
        if (!isDirty && !isDestroyed) {
            ownerView.invalidate()
            isDirty = true
        }
    }

    override fun destroy() {
        drawBlock = null
        invalidateParentLayer = null
        isDestroyed = true
        isDirty = false
    }

    override fun mapOffset(point: Offset, inverse: Boolean): Offset {
        return getMatrix(inverse).map(point)
    }

    override fun mapBounds(rect: MutableRect, inverse: Boolean) {
        getMatrix(inverse).map(rect)
    }

    override fun reuseLayer(drawBlock: (Canvas) -> Unit, invalidateParentLayer: () -> Unit) {
        throwIllegalStateException("reuseLayer is not supported yet")
    }

    override fun transform(matrix: Matrix) {
        matrix.timesAssign(getMatrix(inverse = false))
    }

    override fun inverseTransform(matrix: Matrix) {
        matrix.timesAssign(getMatrix(inverse = true))
    }

    override val layerId: Long
        get() = graphicsLayer.layerId

    override val ownerViewId: Long
        get() = graphicsLayer.ownerViewId

    private fun getMatrix(inverse: Boolean = false): Matrix {
        updateMatrix()
        if (inverse) {
            matrixCache.invert()
        }
        return matrixCache
    }

    private fun updateMatrix() = with(graphicsLayer) {
        val pivot = if (pivotOffset.isUnspecified) size.center.toOffset() else pivotOffset

        matrixCache.reset()
        matrixCache *= Matrix().apply {
            translate(x = -pivot.x, y = -pivot.y)
        }
        matrixCache *= Matrix().apply {
            translate(translationX, translationY)
            rotateX(rotationX)
            rotateY(rotationY)
            rotateZ(rotationZ)
            scale(scaleX, scaleY)
        }
        matrixCache *= Matrix().apply {
            translate(x = pivot.x, y = pivot.y)
        }
    }
}
