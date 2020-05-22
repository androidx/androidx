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

import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import androidx.ui.geometry.Size
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.CanvasHolder
import androidx.ui.graphics.Path
import androidx.ui.graphics.RectangleShape
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * View implementation of OwnedLayer.
 */
internal class ViewLayer(
    val ownerView: AndroidComposeView,
    val container: ViewLayerContainer,
    val drawLayerModifier: DrawLayerModifier,
    val drawBlock: (Canvas) -> Unit,
    val invalidateParentLayer: () -> Unit
) : View(ownerView.context), OwnedLayer {
    private val outlineResolver = OutlineResolver(ownerView.density)
    // Value of the layerModifier's clipToBounds property
    private var clipToBounds = false
    private var clipBoundsCache: android.graphics.Rect? = null
    private val manualClipPath: Path? get() =
        if (!clipToOutline) null else outlineResolver.clipPath
    private var isInvalidated = false
    private var drawnWithZ = false
    private val canvasHolder = CanvasHolder()

    /**
     * Local copy of the transform origin as DrawLayerModifier can be implemented
     * as a model object. Update this field within [updateLayerProperties] and use it
     * in [resize] or other methods
     */
    private var mTransformOrigin: TransformOrigin = TransformOrigin.Center

    init {
        setWillNotDraw(false) // we WILL draw
        id = generateViewId()
        container.addView(this)
    }

    override val layerId: Long
        get() = id.toLong()

    override fun updateLayerProperties() {
        this.mTransformOrigin = drawLayerModifier.transformOrigin
        this.scaleX = drawLayerModifier.scaleX
        this.scaleY = drawLayerModifier.scaleY
        this.alpha = drawLayerModifier.alpha
        this.translationX = drawLayerModifier.translationX
        this.translationY = drawLayerModifier.translationY
        this.elevation = drawLayerModifier.shadowElevation
        this.rotation = drawLayerModifier.rotationZ
        this.rotationX = drawLayerModifier.rotationX
        this.rotationY = drawLayerModifier.rotationY
        this.pivotX = mTransformOrigin.pivotFractionX * width
        this.pivotY = mTransformOrigin.pivotFractionY * height
        val shape = drawLayerModifier.shape
        val clip = drawLayerModifier.clip
        this.clipToBounds = clip && shape === RectangleShape
        resetClipBounds()
        val wasClippingManually = manualClipPath != null
        this.clipToOutline = clip && shape !== RectangleShape
        val shapeChanged = outlineResolver.update(
            shape,
            this.alpha,
            this.clipToOutline,
            this.elevation
        )
        updateOutlineResolver()
        val isClippingManually = manualClipPath != null
        if (wasClippingManually != isClippingManually || (isClippingManually && shapeChanged)) {
            invalidate() // have to redraw the content
        }
        if (!drawnWithZ && elevation > 0) {
            invalidateParentLayer()
        }
    }

    private fun updateOutlineResolver() {
        this.outlineProvider = if (outlineResolver.outline != null) {
            OutlineProvider
        } else {
            null
        }
    }

    private fun resetClipBounds() {
        this.clipBounds = if (clipToBounds) {
            if (clipBoundsCache == null) {
                clipBoundsCache = android.graphics.Rect(0, 0, width, height)
            } else {
                clipBoundsCache!!.set(0, 0, width, height)
            }
            clipBoundsCache
        } else {
            null
        }
    }

    override fun resize(size: IntPxSize) {
        val width = size.width.value
        val height = size.height.value
        if (width != this.width || height != this.height) {
            pivotX = mTransformOrigin.pivotFractionX * width
            pivotY = mTransformOrigin.pivotFractionY * height
            outlineResolver.update(Size(width.toFloat(), height.toFloat()))
            updateOutlineResolver()
            layout(left, top, left + width, top + height)
            resetClipBounds()
        }
    }

    override fun move(position: IntPxPosition) {
        val left = position.x.value

        if (left != this.left) {
            offsetLeftAndRight(left - this.left)
        }
        val top = position.y.value
        if (top != this.top) {
            offsetTopAndBottom(top - this.top)
        }
    }

    override fun drawLayer(canvas: Canvas) {
        drawnWithZ = elevation > 0f
        if (drawnWithZ) {
            canvas.enableZ()
        }
        container.drawChild(canvas, this, drawingTime)
        if (drawnWithZ) {
            canvas.disableZ()
        }
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        canvasHolder.drawInto(canvas) {
            val clipPath = manualClipPath
            if (clipPath != null) {
                save()
                clipPath(clipPath)
            }
            ownerView.observeLayerModelReads(this@ViewLayer) {
                drawBlock(this)
            }
            if (clipPath != null) {
                restore()
            }
            isInvalidated = false
        }
    }

    override fun invalidate() {
        if (!isInvalidated) {
            isInvalidated = true
            super.invalidate()
            ownerView.dirtyLayers += this
            ownerView.invalidate()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    }

    override fun destroy() {
        container.removeView(this)
        ownerView.dirtyLayers -= this
    }

    override fun updateDisplayList() {
        if (isInvalidated) {
            updateDisplayList(this)
            isInvalidated = false
        }
    }

    override fun forceLayout() {
        // Don't do anything. These Views are treated as RenderNodes, so a forced layout
        // should not do anything. If we keep this, we get more redrawing than is necessary.
    }

    companion object {
        val OutlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                view as ViewLayer
                outline.set(view.outlineResolver.outline!!)
            }
        }
        private var updateDisplayListIfDirtyMethod: Method? = null
        private var recreateDisplayList: Field? = null
        private var hasRetrievedMethod = false

        fun updateDisplayList(view: View) {
            if (!hasRetrievedMethod) {
                hasRetrievedMethod = true
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    updateDisplayListIfDirtyMethod =
                        View::class.java.getDeclaredMethod("updateDisplayListIfDirty")
                    recreateDisplayList =
                        View::class.java.getDeclaredField("mRecreateDisplayList")
                } else {
                    val getDeclaredMethod = Class::class.java.getDeclaredMethod(
                        "getDeclaredMethod",
                        String::class.java,
                        arrayOf<Class<*>>()::class.java
                    )
                    updateDisplayListIfDirtyMethod = getDeclaredMethod.invoke(
                        View::class.java,
                        "updateDisplayListIfDirty", emptyArray<Class<*>>()
                    ) as Method?
                    val getDeclaredField = Class::class.java.getDeclaredMethod(
                        "getDeclaredField",
                        String::class.java
                    )
                    recreateDisplayList = getDeclaredField.invoke(
                        View::class.java,
                        "mRecreateDisplayList"
                    ) as Field?
                }
                updateDisplayListIfDirtyMethod?.isAccessible = true
                recreateDisplayList?.isAccessible = true
            }
            recreateDisplayList?.setBoolean(view, true)
            updateDisplayListIfDirtyMethod?.invoke(view)
        }
    }
}
