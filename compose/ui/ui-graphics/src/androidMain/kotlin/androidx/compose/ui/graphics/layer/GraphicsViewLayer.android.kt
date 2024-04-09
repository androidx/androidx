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

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Picture
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.view.View
import android.view.View.LAYER_TYPE_HARDWARE
import android.view.View.LAYER_TYPE_NONE
import android.view.ViewOutlineProvider
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CanvasHolder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DefaultDensity
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.layer.GraphicsLayerImpl.Companion.DefaultDrawBlock
import androidx.compose.ui.graphics.layer.SurfaceUtils.isLockHardwareCanvasAvailable
import androidx.compose.ui.graphics.layer.view.DrawChildContainer
import androidx.compose.ui.graphics.layer.view.PlaceholderHardwareCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPorterDuffMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import java.lang.reflect.Method

internal class ViewLayer(
    val ownerView: View,
    val canvasHolder: CanvasHolder = CanvasHolder(),
    private val canvasDrawScope: CanvasDrawScope = CanvasDrawScope()
) : View(ownerView.context) {

    var isInvalidated = false

    init {
        outlineProvider = LayerOutlineProvider
    }

    /**
     * Configure the outline on the view, returning true if the outline was configured successfully
     * and false otherwise. This can fail on API level 21 if the reflective call to rebuildOutline
     * fails. In case of failure calls are expected to invalidate this view
     */
    fun setLayerOutline(outline: Outline): Boolean {
        layerOutline = outline
        return OutlineUtils.rebuildOutline(this)
    }

    private var layerOutline: Outline? = null

    internal var canUseCompositingLayer = true
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private var density: Density = DefaultDensity
    private var layoutDirection: LayoutDirection = LayoutDirection.Ltr
    private var drawBlock: DrawScope.() -> Unit = DefaultDrawBlock
    private var parentLayer: GraphicsLayer? = null

    fun setDrawParams(
        density: Density,
        layoutDirection: LayoutDirection,
        parentLayer: GraphicsLayer?,
        drawBlock: DrawScope.() -> Unit
    ) {
        this.density = density
        this.layoutDirection = layoutDirection
        this.drawBlock = drawBlock
        this.parentLayer = parentLayer
    }

    init {
        setWillNotDraw(false) // we WILL draw
        this.clipBounds = null
    }

    override fun invalidate() {
        if (!isInvalidated) {
            isInvalidated = true
            super.invalidate()
        }
    }

    override fun hasOverlappingRendering(): Boolean {
        return canUseCompositingLayer
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        canvasHolder.drawInto(canvas) {
            canvasDrawScope.draw(
                density,
                layoutDirection,
                this,
                Size(width.toFloat(), height.toFloat()),
                parentLayer,
                drawBlock
            )
        }
        isInvalidated = false
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    }

    override fun forceLayout() {
        // Don't do anything. These Views are treated as RenderNodes, so a forced layout
        // should not do anything. If we keep this, we get more redrawing than is necessary.
    }

    companion object {
        internal val LayerOutlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline) {
                if (view is ViewLayer) {
                    view.layerOutline?.let { layerOutline ->
                        outline.set(layerOutline)
                    }
                }
            }
        }
    }
}

internal class GraphicsViewLayer(
    private val layerContainer: DrawChildContainer,
    override val ownerId: Long,
    val canvasHolder: CanvasHolder = CanvasHolder(),
    canvasDrawScope: CanvasDrawScope = CanvasDrawScope()
) : GraphicsLayerImpl {

    private val viewLayer = ViewLayer(layerContainer, canvasHolder, canvasDrawScope)
    private val resources = layerContainer.resources
    private val clipRect = android.graphics.Rect()
    private var layerPaint: android.graphics.Paint? = null

    private val picture: Picture? = if (mayRenderInSoftware) {
            Picture()
        } else {
            null
        }
    private val pictureDrawScope: CanvasDrawScope? = if (mayRenderInSoftware) {
            CanvasDrawScope()
        } else {
            null
        }
    private val pictureCanvasHolder: CanvasHolder? = if (mayRenderInSoftware) {
            CanvasHolder()
        } else {
            null
        }

    init {
        layerContainer.addView(viewLayer)
        viewLayer.clipBounds = null
    }

    private var topLeft = IntOffset.Zero
    private var size = IntSize.Zero
    private var clipInvalidated = false
    override var isInvalidated: Boolean = true

    override val layerId: Long = View.generateViewId().toLong()

    override var blendMode: BlendMode = BlendMode.SrcOver
        set(value) {
            field = value
            obtainLayerPaint().apply { xfermode = PorterDuffXfermode(value.toPorterDuffMode()) }
            updateLayerProperties()
        }
    override var colorFilter: ColorFilter? = null
        set(value) {
            field = value
            obtainLayerPaint().apply { this.colorFilter = value?.asAndroidColorFilter() }
            updateLayerProperties()
        }
    override var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
        set(value) {
            field = value
            updateLayerProperties()
        }

    private fun applyCompositingLayer(compositingStrategy: CompositingStrategy) {
        viewLayer.canUseCompositingLayer = when (compositingStrategy) {
            CompositingStrategy.Offscreen -> {
                viewLayer.setLayerType(LAYER_TYPE_HARDWARE, layerPaint)
                true
            }
            CompositingStrategy.ModulateAlpha -> {
                viewLayer.setLayerType(LAYER_TYPE_NONE, layerPaint)
                false
            }
            else -> {
                viewLayer.setLayerType(LAYER_TYPE_NONE, layerPaint)
                true
            }
        }
    }

    private fun updateLayerProperties() {
        if (requiresCompositingLayer()) {
            applyCompositingLayer(CompositingStrategy.Offscreen)
        } else {
            applyCompositingLayer(compositingStrategy)
        }
    }

    private fun obtainLayerPaint(): android.graphics.Paint =
        layerPaint ?: android.graphics.Paint().also { layerPaint = it }

    private fun requiresCompositingLayer(): Boolean =
        compositingStrategy == CompositingStrategy.Offscreen ||
            requiresLayerPaint()

    private fun requiresLayerPaint(): Boolean =
        blendMode != BlendMode.SrcOver || colorFilter != null

    override var alpha: Float = 1f
        set(value) {
            field = value
            viewLayer.setAlpha(value)
        }

    override var pivotOffset: Offset = Offset.Zero
        set(value) {
            field = value
            viewLayer.pivotX = value.x
            viewLayer.pivotY = value.y
        }
    override var scaleX: Float = 1f
        set(value) {
            field = value
            viewLayer.scaleX = value
        }
    override var scaleY: Float = 1f
        set(value) {
            field = value
            viewLayer.scaleY = value
        }

    override var translationX: Float = 0f
        set(value) {
            field = value
            viewLayer.translationX = value
        }
    override var translationY: Float = 0f
        set(value) {
            field = value
            viewLayer.translationY = value
        }

    override var shadowElevation: Float = 0f
        set(value) {
            field = value
            viewLayer.elevation = value
        }
    override var ambientShadowColor: Color = Color.Black
        set(value) {
            field = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ViewLayerVerificationHelper28.setOutlineAmbientShadowColor(
                    viewLayer,
                    value.toArgb()
                )
            }
        }
    override var spotShadowColor: Color = Color.Black
        set(value) {
            field = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ViewLayerVerificationHelper28.setOutlineSpotShadowColor(viewLayer, value.toArgb())
            }
        }
    override var rotationX: Float = 0f
        set(value) {
            field = value
            viewLayer.rotationX = value
        }
    override var rotationY: Float = 0f
        set(value) {
            field = value
            viewLayer.rotationY = value
        }
    override var rotationZ: Float = 0f
        set(value) {
            field = value
            viewLayer.rotation = value
        }
    override var cameraDistance: Float
        get() {
            return viewLayer.getCameraDistance() / resources.displayMetrics.densityDpi
        }
        set(value) {
            viewLayer.setCameraDistance(value * resources.displayMetrics.densityDpi)
        }
    override var clip: Boolean = false
        set(value) {
            field = value
            clipInvalidated = true
        }
    override var renderEffect: RenderEffect? = null
        set(value) {
            field = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ViewLayerVerificationHelper31.setRenderEffect(viewLayer, value)
            }
        }

    override fun setPosition(topLeft: IntOffset, size: IntSize) {
        if (this.topLeft.x != topLeft.x) {
            viewLayer.offsetLeftAndRight(topLeft.x - this.topLeft.x)
        }

        if (this.topLeft.y != topLeft.y) {
            viewLayer.offsetTopAndBottom(topLeft.y - this.topLeft.y)
        }

        if (this.size != size) {
            if (clip) {
                clipInvalidated = true
            }
            viewLayer.layout(topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height)
        }
        this.topLeft = topLeft
        this.size = size
    }

    override fun setOutline(outline: Outline, clip: Boolean) {
        // b/18175261 On the initial Lollipop release invalidateOutline
        // would not invalidate shadows. As a workaround there is a reflective call to
        // invoke View#rebuildOutline directly. However, if the reflection fails
        // (setLayerOutline returns false), instead we need to invalidate the view and re-record
        // the drawing operations.
        val requiresRedraw = !viewLayer.setLayerOutline(outline)
        viewLayer.clipToOutline = clip
        if (requiresRedraw) {
            viewLayer.invalidate()
            recordDrawingOperations()
        }
    }

    override fun record(
        density: Density,
        layoutDirection: LayoutDirection,
        layer: GraphicsLayer,
        block: DrawScope.() -> Unit
    ) {
        viewLayer.setDrawParams(density, layoutDirection, layer, block)
        recordDrawingOperations()
        picture?.let { p ->
            val pictureCanvas = p.beginRecording(size.width, size.height)
            pictureCanvasHolder?.drawInto(pictureCanvas) {
                pictureDrawScope?.draw(
                    density,
                    layoutDirection,
                    this,
                    size.toSize(),
                    block
                )
            }
            p.endRecording()
        }
    }

    private fun recordDrawingOperations() {
        try {
            canvasHolder.drawInto(PlaceholderCanvas) {
                layerContainer.drawChild(this, viewLayer, viewLayer.drawingTime)
            }
        } catch (t: Throwable) {
            // We will run into class cast exceptions as View rendering attempts to
            // cast a canvas as a DisplayListCanvas. However, this cast happens after the call to
            // updateDisplayListIfDirty so just catch the error here and keep going
        }
    }

    override fun draw(canvas: androidx.compose.ui.graphics.Canvas) {
        updateClip()
        val androidCanvas = canvas.nativeCanvas
        if (androidCanvas.isHardwareAccelerated) {
            layerContainer.drawChild(canvas, viewLayer, viewLayer.drawingTime)
        } else {
            picture?.let { androidCanvas.drawPicture(it) }
        }
    }

    override fun calculateMatrix(): Matrix = viewLayer.matrix

    private fun updateClip() {
       if (clipInvalidated) {
           viewLayer.clipBounds = if (clip) {
               clipRect.apply {
                   left = 0
                   top = 0
                   right = viewLayer.width
                   bottom = viewLayer.height
               }
           } else {
               null
           }
       }
    }

    override fun discardDisplayList() {
        layerContainer.removeViewInLayout(viewLayer)
    }

    companion object {

        val mayRenderInSoftware = !isLockHardwareCanvasAvailable()

        val PlaceholderCanvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android M+ we just need a Canvas that returns true for isHardwareAccelerated
            // in order to get the draw calls to update the displaylist of the backing View
            object : Canvas() {
                override fun isHardwareAccelerated(): Boolean = true
            }
        } else {
            // On Android L, there is an instanceof check that verify that the Canvas is a
            // HardwareCanvas so return our subclass of the HardwareCanvas stub
            PlaceholderHardwareCanvas()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private object ViewLayerVerificationHelper31 {

    @androidx.annotation.DoNotInline
    fun setRenderEffect(view: View, target: RenderEffect?) {
        view.setRenderEffect(target?.asAndroidRenderEffect())
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private object ViewLayerVerificationHelper28 {

    @androidx.annotation.DoNotInline
    fun setOutlineAmbientShadowColor(view: View, target: Int) {
        view.outlineAmbientShadowColor = target
    }

    @androidx.annotation.DoNotInline
    fun setOutlineSpotShadowColor(view: View, target: Int) {
        view.outlineSpotShadowColor = target
    }
}

private object OutlineUtils {
    private var rebuildOutlineMethod: Method? = null
    private var hasRetrievedMethod = false

    /**
     * Returns true if the outline was rebuilt successfully, false otherwise.
     * This can only return false on API 21 if the reflective API call had failed
     */
    fun rebuildOutline(view: View): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            view.invalidateOutline()
            return true
        } else {
            // b/18175261 On the initial Lollipop release invalidateOutline
            // would not invalidate shadows so directly call rebuildOutline
            try {
                var method: Method?
                synchronized(this) {
                    if (!hasRetrievedMethod) {
                        hasRetrievedMethod = true

                        method = View::class.java.getDeclaredMethod("rebuildOutline")
                        method?.let {
                            it.isAccessible = true
                            rebuildOutlineMethod = it
                        }
                    } else {
                        method = rebuildOutlineMethod
                    }
                }
                method?.invoke(view)
                return method != null
            } catch (_: Throwable) {
                return false
            }
        }
    }
}
