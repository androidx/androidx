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
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Path
import androidx.ui.unit.Density
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.toPxSize
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * View implementation of OwnedLayer.
 */
internal class ViewLayer(
    val ownerView: AndroidComposeView,
    val drawLayerModifier: DrawLayerModifier,
    val drawBlock: (Canvas, Density) -> Unit
) : View(ownerView.context), OwnedLayer {
    private val outlineResolver = OutlineResolver(ownerView.density)
    // Value of the layerModifier's clipToBounds property
    private var clipToBounds = false
    private var clipBoundsCache: android.graphics.Rect? = null
    private val manualClipPath: Path? get() =
        if (!clipToOutline) null else outlineResolver.clipPath
    private var isInvalidated = false

    init {
        setWillNotDraw(false) // we WILL draw
        id = generateViewId()
        ownerView.addView(this)
    }

    override fun updateLayerProperties() {
        val props = drawLayerModifier.properties
        this.scaleX = props.scaleX
        this.scaleY = props.scaleY
        this.alpha = props.alpha
        this.elevation = props.elevation
        this.rotation = props.rotationZ
        this.rotationX = props.rotationX
        this.rotationY = props.rotationY
        this.clipToBounds = props.clipToBounds
        resetClipBounds()
        val wasClippingManually = manualClipPath != null
        this.clipToOutline = props.clipToOutline
        val shapeChanged = outlineResolver.update(props.outlineShape, this.alpha)
        updateOutlineResolver()
        val isClippingManually = manualClipPath != null
        if (wasClippingManually != isClippingManually || (isClippingManually && shapeChanged)) {
            invalidate() // have to redraw the content
        }
    }

    private fun updateOutlineResolver() {
        this.outlineProvider = if (outlineResolver.supportsNativeOutline) {
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
            outlineResolver.update(size.toPxSize())
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
        ownerView.drawChild(canvas, this, drawingTime)
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        val uiCanvas = Canvas(canvas)
        val clipPath = manualClipPath
        if (clipPath != null) {
            uiCanvas.save()
            uiCanvas.clipPath(clipPath)
        }
        uiCanvas.enableZ()
        ownerView.observeLayerModelReads(this) {
            drawBlock(uiCanvas, ownerView.density)
        }
        uiCanvas.disableZ()
        if (clipPath != null) {
            uiCanvas.restore()
        }
        isInvalidated = false
    }

    override fun invalidate() {
        if (!isInvalidated) {
            isInvalidated = true
            super.invalidate()
            ownerView.dirtyLayers += this
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    }

    override fun destroy() {
        ownerView.removeView(this)
        ownerView.dirtyLayers -= this
    }

    override fun updateDisplayList() {
        updateDisplayList(this)
        isInvalidated = false
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
