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
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.view.DisplayListCanvas
import android.view.RenderNode
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CanvasHolder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPorterDuffMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(Build.VERSION_CODES.M)
internal class GraphicsLayerV23(
    ownerView: View,
    override val ownerId: Long,
    private val canvasHolder: CanvasHolder = CanvasHolder(),
    private val canvasDrawScope: CanvasDrawScope = CanvasDrawScope()
) : GraphicsLayerImpl {

    private val renderNode = RenderNode.create("Compose", ownerView)
    private var size: IntSize = IntSize.Zero
    private var layerPaint: android.graphics.Paint? = null
    private var matrix: android.graphics.Matrix? = null
    private var outlineIsProvided = false
    private var outlineSize = IntSize.Zero

    private fun obtainLayerPaint(): android.graphics.Paint =
        layerPaint ?: android.graphics.Paint().also { layerPaint = it }

    init {
        // only need to do this once
        if (needToValidateAccess.getAndSet(false)) {
            // This is only to force loading the DisplayListCanvas class and causing the
            // MRenderNode to fail with a NoClassDefFoundError during construction instead of
            // later.
            @Suppress("UNUSED_VARIABLE") val displayListCanvas: DisplayListCanvas? = null

            // Ensure that we can access properties of the RenderNode. We want to force an
            // exception here if there is a problem accessing any of these so that we can
            // fall back to the View implementation.
            renderNode.scaleX = renderNode.scaleX
            renderNode.scaleY = renderNode.scaleY
            renderNode.translationX = renderNode.translationX
            renderNode.translationY = renderNode.translationY
            renderNode.elevation = renderNode.elevation
            renderNode.rotation = renderNode.rotation
            renderNode.rotationX = renderNode.rotationX
            renderNode.rotationY = renderNode.rotationY
            renderNode.cameraDistance = renderNode.cameraDistance
            renderNode.pivotX = renderNode.pivotX
            renderNode.pivotY = renderNode.pivotY
            renderNode.clipToOutline = renderNode.clipToOutline
            renderNode.setClipToBounds(false)
            renderNode.alpha = renderNode.alpha
            renderNode.isValid // only read
            renderNode.setLeftTopRightBottom(0, 0, 0, 0)
            renderNode.offsetLeftAndRight(0)
            renderNode.offsetTopAndBottom(0)
            verifyShadowColorProperties(renderNode)
            discardDisplayListInternal()
            renderNode.setLayerType(View.LAYER_TYPE_NONE)
            renderNode.setHasOverlappingRendering(renderNode.hasOverlappingRendering())
        }
        if (testFailCreateRenderNode) {
            throw NoClassDefFoundError()
        }

        renderNode.setClipToBounds(false)
        applyCompositingStrategy(CompositingStrategy.Auto)
    }

    override var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
        set(value) {
            field = value
            updateLayerProperties()
        }

    private fun applyCompositingStrategy(compositingStrategy: CompositingStrategy) {
        renderNode.apply {
            when (compositingStrategy) {
                CompositingStrategy.Offscreen -> {
                    setLayerType(View.LAYER_TYPE_HARDWARE)
                    setLayerPaint(layerPaint)
                    setHasOverlappingRendering(true)
                }
                CompositingStrategy.ModulateAlpha -> {
                    setLayerType(View.LAYER_TYPE_NONE)
                    setLayerPaint(layerPaint)
                    setHasOverlappingRendering(false)
                }
                else -> { // CompositingStrategy.Auto
                    setLayerType(View.LAYER_TYPE_NONE)
                    setLayerPaint(layerPaint)
                    setHasOverlappingRendering(true)
                }
            }
        }
    }

    override var blendMode: BlendMode = BlendMode.SrcOver
        set(value) {
            if (field != value) {
                field = value
                obtainLayerPaint().apply { xfermode = PorterDuffXfermode(value.toPorterDuffMode()) }
                updateLayerProperties()
            }
        }

    private fun requiresCompositingLayer(): Boolean =
        compositingStrategy == CompositingStrategy.Offscreen ||
            blendMode != BlendMode.SrcOver ||
            colorFilter != null

    private fun updateLayerProperties() {
        if (requiresCompositingLayer()) {
            applyCompositingStrategy(CompositingStrategy.Offscreen)
        } else {
            applyCompositingStrategy(compositingStrategy)
        }
    }

    override var colorFilter: ColorFilter? = null
        set(value) {
            field = value
            if (value != null) {
                applyCompositingStrategy(CompositingStrategy.Offscreen)
                renderNode.setLayerPaint(
                    obtainLayerPaint().apply { colorFilter = value.asAndroidColorFilter() }
                )
            } else {
                updateLayerProperties()
            }
        }

    override var alpha: Float = 1.0f
        set(value) {
            field = value
            renderNode.setAlpha(value)
        }

    private var shouldManuallySetCenterPivot = false

    override var pivotOffset: Offset = Offset.Unspecified
        set(value) {
            field = value
            if (value.isUnspecified) {
                shouldManuallySetCenterPivot = true
                renderNode.pivotX = size.width / 2f
                renderNode.pivotY = size.height / 2f
            } else {
                shouldManuallySetCenterPivot = false
                renderNode.pivotX = value.x
                renderNode.pivotY = value.y
            }
        }

    override var scaleX: Float = 1f
        set(value) {
            field = value
            renderNode.setScaleX(value)
        }

    override var scaleY: Float = 1f
        set(value) {
            field = value
            renderNode.setScaleY(value)
        }

    override var translationX: Float = 0f
        set(value) {
            field = value
            renderNode.setTranslationX(value)
        }

    override var translationY: Float = 0f
        set(value) {
            field = value
            renderNode.setTranslationY(value)
        }

    override var shadowElevation: Float = 0f
        set(value) {
            field = value
            renderNode.setElevation(value)
        }

    override var ambientShadowColor: Color = Color.Black
        set(value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                field = value
                RenderNodeVerificationHelper28.setAmbientShadowColor(renderNode, value.toArgb())
            }
        }

    override var spotShadowColor: Color = Color.Black
        set(value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                field = value
                RenderNodeVerificationHelper28.setSpotShadowColor(renderNode, value.toArgb())
            }
        }

    override var rotationX: Float = 0f
        set(value) {
            field = value
            renderNode.setRotationX(value)
        }

    override var rotationY: Float = 0f
        set(value) {
            field = value
            renderNode.setRotationY(value)
        }

    override var rotationZ: Float = 0f
        set(value) {
            field = value
            renderNode.setRotation(value)
        }

    override var cameraDistance: Float = DefaultCameraDistance
        set(value) {
            // Camera distance was negated in older API levels. Maintain the same input parameters
            // and negate the given camera distance before it is applied and also negate it when
            // it is queried
            field = value
            renderNode.setCameraDistance(-value)
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
            renderNode.setClipToBounds(clipToBounds)
        }
        if (newClipToOutline != clipToOutline) {
            clipToOutline = newClipToOutline
            renderNode.setClipToOutline(newClipToOutline)
        }
    }

    // API level 23 does not support RenderEffect so keep the field around for consistency
    // however, it will not be applied to the rendered result. Consumers are encouraged
    // to use the RenderEffect.isSupported API before consuming a [RenderEffect] instance.
    // If RenderEffect is used on an unsupported API level, it should act as a no-op and not
    // crash the compose application
    override var renderEffect: RenderEffect? = null

    override fun setPosition(x: Int, y: Int, size: IntSize) {
        renderNode.setLeftTopRightBottom(x, y, x + size.width, y + size.height)
        if (this.size != size) {
            if (shouldManuallySetCenterPivot) {
                renderNode.pivotX = size.width / 2f
                renderNode.pivotY = size.height / 2f
            }
            this.size = size
        }
    }

    override fun setOutline(outline: Outline?, outlineSize: IntSize) {
        this.outlineSize = outlineSize
        renderNode.setOutline(outline)
        outlineIsProvided = outline != null
        applyClip()
    }

    override var isInvalidated: Boolean = true

    override val hasDisplayList: Boolean
        get() = renderNode.isValid

    override fun record(
        density: Density,
        layoutDirection: LayoutDirection,
        layer: GraphicsLayer,
        block: DrawScope.() -> Unit
    ) {
        val recordingCanvas =
            renderNode.start(
                maxOf(size.width, outlineSize.width),
                maxOf(size.height, outlineSize.height)
            )
        try {
            canvasHolder.drawInto(recordingCanvas) {
                canvasDrawScope.draw(density, layoutDirection, this, size.toSize(), layer, block)
            }
        } finally {
            renderNode.end(recordingCanvas)
        }
        isInvalidated = false
    }

    override fun draw(canvas: androidx.compose.ui.graphics.Canvas) {
        (canvas.nativeCanvas as DisplayListCanvas).drawRenderNode(renderNode)
    }

    override fun calculateMatrix(): Matrix {
        val m = matrix ?: android.graphics.Matrix().also { matrix = it }
        renderNode.getMatrix(m)
        return m
    }

    override fun discardDisplayList() {
        discardDisplayListInternal()
    }

    override val layerId: Long = 0

    private fun verifyShadowColorProperties(renderNode: RenderNode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            RenderNodeVerificationHelper28.setAmbientShadowColor(
                renderNode,
                RenderNodeVerificationHelper28.getAmbientShadowColor(renderNode)
            )
            RenderNodeVerificationHelper28.setSpotShadowColor(
                renderNode,
                RenderNodeVerificationHelper28.getSpotShadowColor(renderNode)
            )
        }
    }

    internal fun discardDisplayListInternal() {
        // See b/216660268. RenderNode#discardDisplayList was originally called
        // destroyDisplayListData on Android M and below. Make sure we gate on the corresponding
        // API level and call the original method name on these API levels, otherwise invoke
        // the current method name of discardDisplayList
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            RenderNodeVerificationHelper24.discardDisplayList(renderNode)
        } else {
            RenderNodeVerificationHelper23.destroyDisplayListData(renderNode)
        }
    }

    companion object {
        // Used by tests to force failing creating a RenderNode to simulate a device that
        // doesn't support RenderNodes before Q.
        internal var testFailCreateRenderNode = false

        // We need to validate that RenderNodes can be accessed before using the RenderNode
        // stub implementation, but we only need to validate it once. This flag indicates that
        // validation is still needed.
        private val needToValidateAccess = AtomicBoolean(true)
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private object RenderNodeVerificationHelper28 {

    fun getAmbientShadowColor(renderNode: RenderNode): Int {
        return renderNode.ambientShadowColor
    }

    fun setAmbientShadowColor(renderNode: RenderNode, target: Int) {
        renderNode.ambientShadowColor = target
    }

    fun getSpotShadowColor(renderNode: RenderNode): Int {
        return renderNode.spotShadowColor
    }

    fun setSpotShadowColor(renderNode: RenderNode, target: Int) {
        renderNode.spotShadowColor = target
    }
}

@RequiresApi(Build.VERSION_CODES.N)
private object RenderNodeVerificationHelper24 {

    fun discardDisplayList(renderNode: RenderNode) {
        renderNode.discardDisplayList()
    }
}

@RequiresApi(Build.VERSION_CODES.M)
private object RenderNodeVerificationHelper23 {

    fun destroyDisplayListData(renderNode: RenderNode) {
        renderNode.destroyDisplayListData()
    }
}
