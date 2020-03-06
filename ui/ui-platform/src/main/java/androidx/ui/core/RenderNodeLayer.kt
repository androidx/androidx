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

package androidx.ui.core

import android.annotation.TargetApi
import android.graphics.RenderNode
import androidx.ui.graphics.Canvas
import androidx.ui.unit.Density
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.toPxSize

/**
 * RenderNode implementation of OwnedLayer.
 */
@TargetApi(29)
internal class RenderNodeLayer(
    val ownerView: AndroidComposeView,
    val drawLayerModifier: DrawLayerModifier,
    val drawBlock: (Canvas, Density) -> Unit
) : OwnedLayer {
    /**
     * True when the RenderNodeLayer has been invalidated and not yet drawn.
     */
    private var isDirty = false
    private val outlineResolver = OutlineResolver(ownerView.density)
    private var isDestoyed = false

    private val renderNode = RenderNode(null).apply {
        setHasOverlappingRendering(true)
    }

    override fun updateLayerProperties() {
        val wasClippingManually = renderNode.clipToOutline && outlineResolver.clipPath != null
        val props = drawLayerModifier.properties
        renderNode.scaleX = props.scaleX
        renderNode.scaleY = props.scaleY
        renderNode.alpha = props.alpha
        renderNode.elevation = props.elevation
        renderNode.rotationZ = props.rotationZ
        renderNode.rotationX = props.rotationX
        renderNode.rotationY = props.rotationY
        renderNode.clipToOutline = props.clipToOutline && props.outlineShape != null
        renderNode.clipToBounds = props.clipToBounds
        val shapeChanged = outlineResolver.update(props.outlineShape, renderNode.alpha)
        renderNode.setOutline(outlineResolver.outline)
        val isClippingManually = renderNode.clipToOutline && outlineResolver.clipPath != null
        if (wasClippingManually != isClippingManually || (isClippingManually && shapeChanged)) {
            invalidate()
        }
    }

    override fun resize(size: IntPxSize) {
        val width = size.width.value
        val height = size.height.value
        if (renderNode.setPosition(
            renderNode.left,
            renderNode.top,
            renderNode.left + width,
            renderNode.top + height
        )) {
            outlineResolver.update(size.toPxSize())
            renderNode.setOutline(outlineResolver.outline)
            invalidate()
        }
    }

    override fun move(position: IntPxPosition) {
        renderNode.offsetLeftAndRight(position.x.value - renderNode.left)
        renderNode.offsetTopAndBottom(position.y.value - renderNode.top)
    }

    override fun invalidate() {
        if (!isDirty && !isDestoyed) {
            ownerView.invalidate()
            ownerView.dirtyLayers += this
            isDirty = true
        }
    }

    override fun drawLayer(canvas: Canvas) {
        val androidCanvas = canvas.nativeCanvas
        if (androidCanvas.isHardwareAccelerated) {
            updateDisplayList()
            androidCanvas.drawRenderNode(renderNode)
        } else {
            drawBlock(canvas, ownerView.density)
        }
        isDirty = false
    }

    fun updateDisplayList() {
        if (isDirty || !renderNode.hasDisplayList()) {
            isDirty = false
            val renderNodeCanvas = renderNode.beginRecording()
            val uiCanvas = Canvas(renderNodeCanvas)

            uiCanvas.enableZ()
            val clipPath = outlineResolver.clipPath
            if (renderNode.clipToOutline && clipPath != null) {
                uiCanvas.save()
                uiCanvas.clipPath(clipPath)
            }
            ownerView.observeLayerModelReads(this) {
                drawBlock(uiCanvas, ownerView.density)
            }
            if (clipPath != null) {
                uiCanvas.restore()
            }
            uiCanvas.disableZ()
            renderNode.endRecording()
        }
    }

    override fun destroy() {
        isDestoyed = true
        ownerView.dirtyLayers -= this
    }
}
