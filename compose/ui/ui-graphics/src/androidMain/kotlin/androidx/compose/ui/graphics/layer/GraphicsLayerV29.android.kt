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

import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.RenderNode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.CanvasHolder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toAndroidBlendMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize

/** GraphicsLayer implementation for Android Q+ that uses the public RenderNode API */
@RequiresApi(Build.VERSION_CODES.Q)
internal class GraphicsLayerV29(
    override val ownerId: Long,
    private val canvasHolder: CanvasHolder = CanvasHolder(),
    private val canvasDrawScope: CanvasDrawScope = CanvasDrawScope(),
) : GraphicsLayerImpl {
    private val renderNode: RenderNode = RenderNode("graphicsLayer")

    private var size: Size = Size.Zero
    private var layerPaint: android.graphics.Paint? = null
    private var matrix: Matrix? = null
    private var outlineIsProvided = false

    init {
        renderNode.clipToBounds = false
        renderNode.applyCompositingStrategy(CompositingStrategy.Auto)
    }

    override var alpha: Float = 1.0f
        set(value) {
            field = value
            renderNode.alpha = value
        }

    override var blendMode: BlendMode = BlendMode.SrcOver
        set(value) {
            field = value
            obtainLayerPaint().apply { blendMode = value.toAndroidBlendMode() }
            updateLayerProperties()
        }

    override var colorFilter: ColorFilter? = null
        set(value) {
            field = value
            obtainLayerPaint().apply { colorFilter = value?.asAndroidColorFilter() }
            updateLayerProperties()
        }

    override var pivotOffset: Offset = Offset.Unspecified
        set(value) {
            field = value
            if (value.isUnspecified) {
                renderNode.resetPivot()
            } else {
                renderNode.pivotX = value.x
                renderNode.pivotY = value.y
            }
        }

    override var scaleX: Float = 1f
        set(value) {
            field = value
            renderNode.scaleX = value
        }

    override var scaleY: Float = 1f
        set(value) {
            field = value
            renderNode.scaleY = value
        }

    override var translationX: Float = 0f
        set(value) {
            field = value
            renderNode.translationX = value
        }

    override var translationY: Float = 0f
        set(value) {
            field = value
            renderNode.translationY = value
        }

    override var shadowElevation: Float = 0f
        set(value) {
            field = value
            renderNode.elevation = value
        }

    override var ambientShadowColor: Color = Color.Black
        set(value) {
            field = value
            renderNode.ambientShadowColor = value.toArgb()
        }

    override var spotShadowColor: Color = Color.Black
        set(value) {
            field = value
            renderNode.spotShadowColor = value.toArgb()
        }

    override var rotationX: Float = 0f
        set(value) {
            field = value
            renderNode.rotationX = value
        }

    override var rotationY: Float = 0f
        set(value) {
            field = value
            renderNode.rotationY = value
        }

    override var rotationZ: Float = 0f
        set(value) {
            field = value
            renderNode.rotationZ = value
        }

    override var cameraDistance: Float = DefaultCameraDistance
        set(value) {
            field = value
            renderNode.cameraDistance = value
        }

    override var clip: Boolean = false
        set(value) {
            field = value
            applyClip()
        }

    private var clipToBounds = false
    private var clipToOutline = false

    private fun applyClip() {
        val newClipToBounds = clip && !outlineIsProvided
        val newClipToOutline = clip && outlineIsProvided
        if (newClipToBounds != clipToBounds) {
            clipToBounds = newClipToBounds
            renderNode.clipToBounds = clipToBounds
        }
        if (newClipToOutline != clipToOutline) {
            clipToOutline = newClipToOutline
            renderNode.clipToOutline = newClipToOutline
        }
    }

    override var renderEffect: RenderEffect? = null
        set(value) {
            field = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RenderNodeVerificationHelper.setRenderEffect(renderNode, value)
            }
        }

    override var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
        set(value) {
            field = value
            updateLayerProperties()
        }

    private fun RenderNode.applyCompositingStrategy(compositingStrategy: CompositingStrategy) {
        when (compositingStrategy) {
            CompositingStrategy.Offscreen -> {
                setUseCompositingLayer(true, layerPaint)
                setHasOverlappingRendering(true)
            }
            CompositingStrategy.ModulateAlpha -> {
                setUseCompositingLayer(false, layerPaint)
                setHasOverlappingRendering(false)
            }
            else -> {
                setUseCompositingLayer(false, layerPaint)
                setHasOverlappingRendering(true)
            }
        }
    }

    private fun updateLayerProperties() {
        if (requiresCompositingLayer()) {
            renderNode.applyCompositingStrategy(CompositingStrategy.Offscreen)
        } else {
            renderNode.applyCompositingStrategy(compositingStrategy)
        }
    }

    override fun setPosition(x: Int, y: Int, size: IntSize) {
        renderNode.setPosition(x, y, x + size.width, y + size.height)
        this.size = size.toSize()
    }

    override fun setOutline(outline: Outline?, outlineSize: IntSize) {
        // outlineSize is not required for this GraphicsLayer implementation
        renderNode.setOutline(outline)
        outlineIsProvided = outline != null
        applyClip()
    }

    override var isInvalidated: Boolean = true

    override fun record(
        density: Density,
        layoutDirection: LayoutDirection,
        layer: GraphicsLayer,
        block: DrawScope.() -> Unit,
    ) {
        val recordingCanvas = renderNode.beginRecording()
        try {
            canvasHolder.drawInto(recordingCanvas) {
                canvasDrawScope.drawContext.also {
                    it.density = density
                    it.layoutDirection = layoutDirection
                    it.graphicsLayer = layer
                    it.size = size
                    it.canvas = this
                }
                canvasDrawScope.block()
            }
        } finally {
            renderNode.endRecording()
        }
        isInvalidated = false
    }

    override fun draw(canvas: Canvas) {
        canvas.nativeCanvas.drawRenderNode(renderNode)
    }

    override fun calculateMatrix(): Matrix {
        val m = matrix ?: Matrix().also { matrix = it }
        renderNode.getMatrix(m)
        return m
    }

    override val hasDisplayList: Boolean
        get() = renderNode.hasDisplayList()

    override fun discardDisplayList() {
        renderNode.discardDisplayList()
    }

    override val layerId: Long
        get() = renderNode.uniqueId

    private fun obtainLayerPaint(): android.graphics.Paint =
        layerPaint ?: android.graphics.Paint().also { layerPaint = it }

    private fun requiresCompositingLayer(): Boolean =
        compositingStrategy == CompositingStrategy.Offscreen ||
            requiresLayerPaint() ||
            renderEffect != null

    private fun requiresLayerPaint(): Boolean =
        blendMode != BlendMode.SrcOver || colorFilter != null
}

@RequiresApi(Build.VERSION_CODES.S)
internal object RenderNodeVerificationHelper {

    fun setRenderEffect(renderNode: RenderNode, target: RenderEffect?) {
        renderNode.setRenderEffect(target?.asAndroidRenderEffect())
    }
}
