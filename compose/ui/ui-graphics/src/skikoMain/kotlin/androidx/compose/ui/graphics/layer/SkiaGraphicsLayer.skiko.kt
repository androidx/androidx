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

import androidx.annotation.IntRange
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.SkiaBackedCanvas
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.asSkiaColorFilter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.graphics.skiaImageFilter
import androidx.compose.ui.graphics.materializeSkiaPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toSkia
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import org.jetbrains.skia.Paint as SkPaint
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect as SkRect
import org.jetbrains.skiko.node.RenderNode

actual class GraphicsLayer internal constructor(
    renderNode: RenderNode,
) {
    private var renderNode: RenderNode? = renderNode
    private val pictureDrawScope = CanvasDrawScope()

    private var outlineDirty = true
    private var roundRectOutlineTopLeft: Offset = Offset.Zero
    private var roundRectOutlineSize: Size = Size.Unspecified
    private var roundRectCornerRadius: Float = 0f

    private var internalOutline: Outline? = null
    private var outlinePath: Path? = null

    private var parentLayerUsages = 0
    private val childDependenciesTracker = ChildLayerDependenciesTracker()

    actual var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
        set(value) {
            if (field != value) {
                field = value
                updateLayerProperties()
            }
        }

    actual var topLeft: IntOffset = IntOffset.Zero
        set(value) {
            if (field != value) {
                field = value
                renderNode?.bounds = SkRect.makeXYWH(
                    value.x.toFloat(),
                    value.y.toFloat(),
                    size.width.toFloat(),
                    size.height.toFloat()
                )
            }
        }

    actual var size: IntSize = IntSize.Zero
        private set(value) {
            if (field != value) {
                field = value
                renderNode?.bounds = SkRect.makeXYWH(
                    topLeft.x.toFloat(),
                    topLeft.y.toFloat(),
                    value.width.toFloat(),
                    value.height.toFloat()
                )
                if (roundRectOutlineSize.isUnspecified) {
                    outlineDirty = true
                    configureOutlineAndClip()
                }
            }
        }

    actual var pivotOffset: Offset = Offset.Unspecified
        set(value) {
            if (field != value) {
                field = value
                renderNode?.pivot = Point(value.x, value.y)
            }
        }

    actual var alpha: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                renderNode?.alpha = value
                updateLayerProperties()
            }
        }

    actual var scaleX: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                renderNode?.scaleX = value
            }
        }

    actual var scaleY: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                renderNode?.scaleY = value
            }
        }

    actual var translationX: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                renderNode?.translationX = value
            }
        }
    actual var translationY: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                renderNode?.translationY = value
            }
        }

    actual var shadowElevation: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                renderNode?.shadowElevation = value
                outlineDirty = true
                configureOutlineAndClip()
            }
        }

    actual var ambientShadowColor: Color = Color.Black
        set(value) {
            if (field != value) {
                field = value
                renderNode?.ambientShadowColor = value.toArgb()
            }
        }

    actual var spotShadowColor: Color = Color.Black
        set(value) {
            if (field != value) {
                field = value
                renderNode?.spotShadowColor = value.toArgb()
            }
        }

    actual var blendMode: BlendMode = BlendMode.SrcOver
        set(value) {
            if (field != value) {
                field = value
                updateLayerProperties()
            }
        }

    actual var colorFilter: ColorFilter? = null
        set(value) {
            if (field != value) {
                field = value
                updateLayerProperties()
            }
        }

    actual val outline: Outline
        get() {
            val tmpOutline = internalOutline
            val tmpPath = outlinePath
            return if (tmpOutline != null) {
                tmpOutline
            } else if (tmpPath != null) {
                Outline.Generic(tmpPath).also { internalOutline = it }
            } else {
                resolveOutlinePosition { outlineTopLeft, outlineSize ->
                    val left = outlineTopLeft.x
                    val top = outlineTopLeft.y
                    val right = left + outlineSize.width
                    val bottom = top + outlineSize.height
                    val cornerRadius = this.roundRectCornerRadius
                    if (cornerRadius > 0f) {
                        Outline.Rounded(
                            RoundRect(left, top, right, bottom, CornerRadius(cornerRadius))
                        )
                    } else {
                        Outline.Rectangle(Rect(left, top, right, bottom))
                    }
                }.also { internalOutline = it }
            }
        }

    private fun resetOutlineParams() {
        internalOutline = null
        outlinePath = null
        roundRectOutlineSize = Size.Unspecified
        roundRectOutlineTopLeft = Offset.Zero
        roundRectCornerRadius = 0f
        outlineDirty = true
    }

    actual fun setPathOutline(path: Path) {
        resetOutlineParams()
        this.outlinePath = path
        configureOutlineAndClip()
    }

    actual fun setRoundRectOutline(topLeft: Offset, size: Size, cornerRadius: Float) {
        if (this.roundRectOutlineTopLeft != topLeft ||
            this.roundRectOutlineSize != size ||
            this.roundRectCornerRadius != cornerRadius ||
            this.outlinePath != null
        ) {
            resetOutlineParams()
            this.roundRectOutlineTopLeft = topLeft
            this.roundRectOutlineSize = size
            this.roundRectCornerRadius = cornerRadius
            configureOutlineAndClip()
        }
    }

    actual fun setRectOutline(topLeft: Offset, size: Size) {
        setRoundRectOutline(topLeft, size, 0f)
    }

    actual var rotationX: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                renderNode?.rotationX = value
            }
        }

    actual var rotationY: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                renderNode?.rotationY = value
            }
        }

    actual var rotationZ: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                renderNode?.rotationZ = value
            }
        }

    actual var cameraDistance: Float = DefaultCameraDistance
        set(value) {
            if (field != value) {
                field = value
                renderNode?.cameraDistance = value
            }
        }

    actual var clip: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                outlineDirty = true
                configureOutlineAndClip()
            }
        }

    actual var renderEffect: RenderEffect? = null
        set(value) {
            if (field != value) {
                field = value
                updateLayerProperties()
            }
        }

    actual var isReleased: Boolean = false
        private set

    actual fun record(
        density: Density,
        layoutDirection: LayoutDirection,
        size: IntSize,
        block: DrawScope.() -> Unit
    ) {
        this.size = size
        recordWithTracking { canvas ->
            canvas.alphaMultiplier = if (compositingStrategy == CompositingStrategy.ModulateAlpha) {
                this@GraphicsLayer.alpha
            } else {
                1.0f
            }
            pictureDrawScope.draw(
                density = density,
                layoutDirection = layoutDirection,
                canvas = canvas,
                size = size.toSize(),
                graphicsLayer = this,
                block = block
            )
        }
    }

    private fun recordWithTracking(block: (SkiaBackedCanvas) -> Unit) {
        val renderNode = renderNode ?: return
        val recordingCanvas = renderNode.beginRecording()
        try {
            val composeCanvas = recordingCanvas.asComposeCanvas() as SkiaBackedCanvas
            childDependenciesTracker.withTracking(
                onDependencyRemoved = { it.onRemovedFromParentLayer() },
            ) { block(composeCanvas) }
        } finally {
            renderNode.endRecording()
        }
    }

    private fun addSubLayer(graphicsLayer: GraphicsLayer) {
        if (childDependenciesTracker.onDependencyAdded(graphicsLayer)) {
            graphicsLayer.onAddedToParentLayer()
        }
    }

    internal actual fun draw(canvas: Canvas, parentLayer: GraphicsLayer?) {
        if (isReleased) return
        configureOutlineAndClip()
        parentLayer?.addSubLayer(this)
        renderNode?.drawInto(canvas.skiaCanvas)
    }

    private fun onAddedToParentLayer() {
        parentLayerUsages++
    }

    private fun onRemovedFromParentLayer() {
        parentLayerUsages--
        discardContentIfReleasedAndHaveNoParentLayerUsages()
    }

    @OptIn(InternalComposeUiApi::class)
    private fun configureOutlineAndClip() {
        if (!outlineDirty) return
        val renderNode = renderNode ?: return
        val outlineIsNeeded = clip || shadowElevation > 0f
        if (!outlineIsNeeded) {
            renderNode.clip = false
            renderNode.setClipPath(null)
        } else {
            renderNode.clip = clip
            when (val tmpOutline = outline) {
                is Outline.Rectangle -> renderNode.setClipRect(
                    tmpOutline.rect.left,
                    tmpOutline.rect.top,
                    tmpOutline.rect.right,
                    tmpOutline.rect.bottom,
                    antiAlias = true
                )
                is Outline.Rounded -> renderNode.setClipRRect(
                    tmpOutline.roundRect.left,
                    tmpOutline.roundRect.top,
                    tmpOutline.roundRect.right,
                    tmpOutline.roundRect.bottom,
                    floatArrayOf(
                        tmpOutline.roundRect.topLeftCornerRadius.x,
                        tmpOutline.roundRect.topLeftCornerRadius.y,
                        tmpOutline.roundRect.topRightCornerRadius.x,
                        tmpOutline.roundRect.topRightCornerRadius.y,
                        tmpOutline.roundRect.bottomRightCornerRadius.x,
                        tmpOutline.roundRect.bottomRightCornerRadius.y,
                        tmpOutline.roundRect.bottomLeftCornerRadius.x,
                        tmpOutline.roundRect.bottomLeftCornerRadius.y
                    ),
                    antiAlias = true
                )
                is Outline.Generic -> renderNode.setClipPath(tmpOutline.path.materializeSkiaPath(), antiAlias = true)
            }
        }
        outlineDirty = false
    }

    private inline fun <T> resolveOutlinePosition(block: (Offset, Size) -> T): T {
        val layerSize = this.size.toSize()
        val rRectTopLeft = roundRectOutlineTopLeft
        val rRectSize = roundRectOutlineSize

        val outlineSize =
            if (rRectSize.isUnspecified) {
                layerSize
            } else {
                rRectSize
            }
        return block(rRectTopLeft, outlineSize)
    }

    internal fun release() {
        if (!isReleased) {
            isReleased = true
            discardContentIfReleasedAndHaveNoParentLayerUsages()
        }
    }

    private fun discardContentIfReleasedAndHaveNoParentLayerUsages() {
        if (isReleased && parentLayerUsages == 0) {
            // discarding means we don't draw children layer anymore and need to remove dependencies:
            childDependenciesTracker.removeDependencies { it.onRemovedFromParentLayer() }

            renderNode?.close()
            renderNode = null
        }
    }

    actual suspend fun toImageBitmap(): ImageBitmap =
        ImageBitmap(size.width, size.height).apply { draw(Canvas(this), null) }

    private fun updateLayerProperties() {
        renderNode?.layerPaint = if (requiresLayer()) {
            SkPaint().also {
                it.setAlphaf(alpha)
                it.imageFilter = renderEffect?.skiaImageFilter
                it.colorFilter = colorFilter?.asSkiaColorFilter()
                it.blendMode = blendMode.toSkia()
            }
        } else {
            null
        }
    }

    private fun requiresLayer(): Boolean {
        val alphaNeedsLayer = alpha < 1f && compositingStrategy != CompositingStrategy.ModulateAlpha
        val hasColorFilter = colorFilter != null
        val hasBlendMode = blendMode != BlendMode.SrcOver
        val hasRenderEffect = renderEffect != null
        val offscreenBufferRequested = compositingStrategy == CompositingStrategy.Offscreen
        return alphaNeedsLayer || hasColorFilter || hasBlendMode || hasRenderEffect ||
            offscreenBufferRequested
    }

    actual fun setOutsets(
        @IntRange(from = 0) left: Int,
        @IntRange(from = 0) top: Int,
        @IntRange(from = 0) right: Int,
        @IntRange(from = 0) bottom: Int
    ) {
        // TODO: https://youtrack.jetbrains.com/issue/CMP-10054/Implement-GraphicsLayer.setOutsets-method
    }
}
