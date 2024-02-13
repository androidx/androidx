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

import android.graphics.Outline as AndroidOutline
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DefaultDensity
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

@Suppress("NotCloseable")
actual class GraphicsLayer internal constructor(
    private val impl: GraphicsLayerImpl
) {
    private var density = DefaultDensity
    private var layoutDirection = LayoutDirection.Ltr
    private var drawBlock: DrawScope.() -> Unit = {}

    private var androidOutline: AndroidOutline? = null
    private var outlineDirty = true
    private var roundRectOutlineTopLeft: IntOffset = UnsetOffset
    private var roundRectOutlineSize: IntSize = UnsetSize
    private var roundRectCornerRadius: Float = 0f

    private var internalOutline: Outline? = null
    private var outlinePath: Path? = null
    private var usePathForClip = false

    init {
        impl.clip = false
    }

    /**
     * Determines if this [GraphicsLayer] has been released. Any attempts to use a [GraphicsLayer]
     * after it has been released is an error.
     */
    actual var isReleased: Boolean = false
        private set

    /**
     * [CompositingStrategy] determines whether or not the contents of this layer are rendered into
     * an offscreen buffer. This is useful in order to optimize alpha usages with
     * [CompositingStrategy.ModulateAlpha] which will skip the overhead of an offscreen buffer but can
     * generate different rendering results depending on whether or not the contents of the layer are
     * overlapping. Similarly leveraging [CompositingStrategy.Offscreen] is useful in situations where
     * creating an offscreen buffer is preferred usually in conjunction with [BlendMode] usage.
     */
    actual var compositingStrategy: CompositingStrategy
        get() = impl.compositingStrategy
        set(value) {
            if (impl.compositingStrategy != value) {
                impl.compositingStrategy = value
            }
        }

    /**
     * Offset in pixels where this [GraphicsLayer] will render within a provided canvas when
     * [drawLayer] is called. This is configured by calling [setPosition]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerTopLeftSample
     */
    actual var topLeft: IntOffset = IntOffset.Zero
        set(value) {
            if (field != value) {
                field = value
                setPosition(value, size)
            }
        }

    /**
     * Size in pixels of the [GraphicsLayer]. By default [GraphicsLayer] contents can draw outside
     * of the bounds specified by [topLeft] and [size], however, rasterization of this layer into
     * an offscreen buffer will be sized according to the specified size. This is configured
     * by calling [buildLayer]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerSizeSample
     */
    actual var size: IntSize = IntSize.Zero
        private set

    /**
     * Alpha of the content of the [GraphicsLayer] between 0f and 1f. Any value between 0f and 1f
     * will be translucent, where 0f will cause the layer to be completely invisible and 1f will be
     * entirely opaque.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerAlphaSample
     */
    actual var alpha: Float
        get() = impl.alpha
        set(value) {
            if (impl.alpha != value) {
                impl.alpha = value
            }
        }

    /**
     * BlendMode to use when drawing this layer to the destination in [drawLayer].
     * The default is [BlendMode.SrcOver].
     * Any value other than [BlendMode.SrcOver] will force this [GraphicsLayer] to use an offscreen
     * compositing layer for rendering and is equivalent to using [CompositingStrategy.Offscreen].
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerBlendModeSample
     */
    actual var blendMode: BlendMode
        get() = impl.blendMode
        set(value) {
            if (impl.blendMode != value) {
                impl.blendMode = value
            }
        }

    /**
     * ColorFilter applied when drawing this layer to the destination in [drawLayer].
     * Setting of this to any non-null will force this [GraphicsLayer] to use an offscreen
     * compositing layer for rendering and is equivalent to using [CompositingStrategy.Offscreen]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerColorFilterSample
     */
    actual var colorFilter: ColorFilter?
        get() = impl.colorFilter
        set(value) {
            if (impl.colorFilter != value) {
                impl.colorFilter = value
            }
        }

    /**
     * [Offset] in pixels used as the center for any rotation or scale transformation. If this value
     * is [Offset.Unspecified], then the center of the [GraphicsLayer] is used relative to [topLeft]
     * and [size]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerScaleAndPivotSample
     */
    actual var pivotOffset: Offset = Offset.Unspecified
        set(value) {
            if (field != value) {
                field = value
                impl.pivotOffset = value
            }
        }

    /**
     * The horizontal scale of the drawn area. Default value is `1`.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerScaleAndPivotSample
     */
    actual var scaleX: Float
        get() = impl.scaleX
        set(value) {
            if (impl.scaleX != value) {
                impl.scaleX = value
            }
        }

    /**
     * The vertical scale of the drawn area. Default value is `1`.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerScaleAndPivotSample
     */
    actual var scaleY: Float
        get() = impl.scaleY
        set(value) {
            if (impl.scaleY != value) {
                impl.scaleY = value
            }
        }

    /**
     * Horizontal pixel offset of the layer relative to its left bound. Default value is `0`.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerTranslateSample
     */
    actual var translationX: Float
        get() = impl.translationX
        set(value) {
            if (impl.translationX != value) {
                impl.translationX = value
            }
        }

    /**
     * Vertical pixel offset of the layer relative to its top bound. Default value is `0`
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerTranslateSample
     */
    actual var translationY: Float
        get() = impl.translationY
        set(value) {
            if (impl.translationY != value) {
                impl.translationY = value
            }
        }

    /**
     * Sets the elevation for the shadow in pixels. With the [shadowElevation] > 0f and
     * [Outline] set, a shadow is produced. Default value is `0` and the value must not be
     * negative. Configuring a non-zero [shadowElevation] enables clipping of [GraphicsLayer]
     * content.
     *
     * Note that if you provide a non-zero [shadowElevation] and if the passed [Outline] is concave
     * the shadow will not be drawn on Android versions less than 10.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerShadowSample
     */
    actual var shadowElevation: Float
        get() = impl.shadowElevation
        set(value) {
            if (impl.shadowElevation != value) {
                impl.shadowElevation = value
            }
        }

    /**
     * The rotation, in degrees, of the contents around the horizontal axis in degrees. Default
     * value is `0`.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRotationX
     */
    actual var rotationX: Float
        get() = impl.rotationX
        set(value) {
            if (impl.rotationX != value) {
                impl.rotationX = value
            }
        }

    /**
     * The rotation, in degrees, of the contents around the vertical axis in degrees. Default
     * value is `0`.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRotationYWithCameraDistance
     */
    actual var rotationY: Float
        get() = impl.rotationY
        set(value) {
            if (impl.rotationY != value) {
                impl.rotationY = value
            }
        }

    /**
     * The rotation, in degrees, of the contents around the Z axis in degrees. Default value is
     * `0`.
     */
    actual var rotationZ: Float
        get() = impl.rotationZ
        set(value) {
            if (impl.rotationZ != value) {
                impl.rotationZ = value
            }
        }

    /**
     * Sets the distance along the Z axis (orthogonal to the X/Y plane on which
     * layers are drawn) from the camera to this layer. The camera's distance
     * affects 3D transformations, for instance rotations around the X and Y
     * axis. If the rotationX or rotationY properties are changed and this view is
     * large (more than half the size of the screen), it is recommended to always
     * use a camera distance that's greater than the height (X axis rotation) or
     * the width (Y axis rotation) of this view.
     *
     * The distance of the camera from the drawing plane can have an affect on the
     * perspective distortion of the layer when it is rotated around the x or y axis.
     * For example, a large distance will result in a large viewing angle, and there
     * will not be much perspective distortion of the view as it rotates. A short
     * distance may cause much more perspective distortion upon rotation, and can
     * also result in some drawing artifacts if the rotated view ends up partially
     * behind the camera (which is why the recommendation is to use a distance at
     * least as far as the size of the view, if the view is to be rotated.)
     *
     * The distance is expressed in pixels and must always be positive.
     * Default value is [DefaultCameraDistance]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRotationYWithCameraDistance
     */
    actual var cameraDistance: Float
        get() = impl.cameraDistance
        set(value) {
            if (impl.cameraDistance != value) {
                impl.cameraDistance = value
            }
        }

    /**
     * Determines if the [GraphicsLayer] should be clipped to the rectangular bounds specified by
     * [topLeft] and [size] or to the Outline if one is provided. The default is false.
     * Note if elevation is provided then clipping will be enabled.
     */
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    actual var clip: Boolean
        get() = impl.clip
        set(value) {
            if (impl.clip != value) {
                impl.clip = value
            }
        }

    /**
     * Configure the [RenderEffect] to apply to this [GraphicsLayer].
     * This will apply a visual effect to the results of the [GraphicsLayer] before it is
     * drawn. For example if [BlurEffect] is provided, the contents will be drawn in a separate
     * layer, then this layer will be blurred when this [GraphicsLayer] is drawn.
     *
     * Note this parameter is only supported on Android 12
     * and above. Attempts to use this Modifier on older Android versions will be ignored.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRenderEffectSample
     */
    actual var renderEffect: RenderEffect?
        get() = impl.renderEffect
        set(value) {
            if (impl.renderEffect != value) {
                impl.renderEffect = value
            }
        }

    /**
     * Configures the [topLeft] and [size] of this [GraphicsLayer]. For covenience in use cases
     *
     * @param topLeft Offset of the [GraphicsLayer]. For convenience, this is set to [topLeft]
     * for use cases where only the [size] is desired to be changed.
     * @param size Size of the [GraphicsLayer]. For convenience, this is set to [size]
     * for use cases where only the [topLeft] is desired to be changed
     */
    private fun setPosition(topLeft: IntOffset, size: IntSize) {
        impl.setPosition(topLeft, size)
        this.outlineDirty = true
    }

    /**
     * Constructs the display list of drawing commands into this layer that will be rendered
     * when this [GraphicsLayer] is drawn elsewhere with [drawLayer].
     * @param density [Density] used to assist in conversions of density independent pixels to raw
     * pixels to draw.
     * @param layoutDirection [LayoutDirection] of the layout being drawn in.
     * @param size [Size] of the [GraphicsLayer]
     * @param block lambda that is called to issue drawing commands on this [DrawScope]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerTopLeftSample
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerBlendModeSample
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerTranslateSample
     */
    actual fun buildLayer(
        density: Density,
        layoutDirection: LayoutDirection,
        size: IntSize,
        block: DrawScope.() -> Unit
    ): GraphicsLayer {
        if (this.size != size) {
            setPosition(topLeft, size)
            this.size = size
        }
        this.density = density
        this.layoutDirection = layoutDirection
        this.drawBlock = block
        impl.isInvalidated = true

        impl.buildLayer(density, layoutDirection, drawBlock)

        return this
    }

    /**
     * Draw the contents of this [GraphicsLayer] into the specified [Canvas]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerDrawLayerIntoCanvas
     */
    actual fun draw(canvas: Canvas) {
        if (pivotOffset.isUnspecified) {
            impl.pivotOffset = Offset(size.width / 2f, size.height / 2f)
        }
        configureOutline()
        val useZ = shadowElevation > 0f
        if (useZ) {
            canvas.enableZ()
        }
        val clipPath = outlinePath
        val willClipPath = usePathForClip
        if (willClipPath && clipPath != null) {
            canvas.save()
            canvas.clipPath(clipPath)
        }
        impl.draw(canvas)
        if (willClipPath) {
            canvas.restore()
        }
        if (useZ) {
            canvas.disableZ()
        }
    }

    private fun configureOutline() {
        val shouldClip = clip || shadowElevation > 0f
        if (outlineDirty) {
            val tmpPath = outlinePath
            if (tmpPath != null) {
                val androidOutline = updatePathOutline(tmpPath).apply {
                    alpha = this@GraphicsLayer.alpha
                }
                impl.setOutline(androidOutline, shouldClip)
            } else {
                val roundRectOutline = obtainAndroidOutline().apply {
                    resolveOutlinePosition { outlineTopLeft, outlineSize ->
                        setRoundRect(
                            outlineTopLeft.x,
                            outlineTopLeft.y,
                            outlineTopLeft.x + outlineSize.width,
                            outlineTopLeft.y + outlineSize.height,
                            roundRectCornerRadius
                        )
                    }
                }.apply {
                    alpha = this@GraphicsLayer.alpha
                }
                impl.setOutline(roundRectOutline, shouldClip)
            }
            outlineDirty = false
        }
    }

    private inline fun <T> resolveOutlinePosition(block: (IntOffset, IntSize) -> T): T {
        val layerTopLeft = this.topLeft
        val layerSize = this.size
        val rRectTopLeft = roundRectOutlineTopLeft
        val rRectSize = roundRectOutlineSize
        val outlineTopLeft = if (rRectTopLeft == UnsetOffset) {
            layerTopLeft
        } else {
            rRectTopLeft
        }

        val outlineSize = if (rRectSize == UnsetSize) {
            layerSize
        } else {
            rRectSize
        }
        return block(outlineTopLeft, outlineSize)
    }

    // Suppress deprecation for usage of setConvexPath in favor of setPath on API levels that
    // previously only supported convex path outlines
    @Suppress("deprecation")
    private fun updatePathOutline(path: Path): AndroidOutline {
        val resultOutline = obtainAndroidOutline()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P || path.isConvex) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                OutlineVerificationHelper.setPath(resultOutline, path)
            } else {
                resultOutline.setConvexPath(path.asAndroidPath())
            }
        } else { // Concave outlines are not supported on older API levels
            androidOutline?.setEmpty()
            usePathForClip = true
            impl.isInvalidated = true
        }
        outlinePath = path
        return resultOutline
    }

    /**
     * Helper method to return the previously created [AndroidOutline] instance or creates and
     * caches it if it was not created previously.
     */
    private fun obtainAndroidOutline(): AndroidOutline =
        androidOutline ?: AndroidOutline().also { androidOutline = it }

    /**
     * Determines if this [GraphicsLayer] has been released. Any attempts to use a [GraphicsLayer]
     * after it has been released is an error.
     */
    internal fun release() {
        if (!isReleased) {
            impl.release()
            isReleased = true
        }
    }

    /**
     * Discards the displaylist of the GraphicsLayer. Used internally
     * for management of GraphicsLayer resources
     */
    internal fun discardDisplayList() {
        impl.discardDisplayList()
    }

    /**
     * The ID of the layer. This is used by tooling to match a layer to the associated
     * LayoutNode.
     */
    val layerId: Long
        get() = impl.layerId

    /**
     * The uniqueDrawingId of the owner view of this graphics layer. This is used by
     * tooling to match a layer to the associated owner View.
     */
    val ownerViewId: Long
        get() = impl.ownerId

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
                    val left = outlineTopLeft.x.toFloat()
                    val top = outlineTopLeft.y.toFloat()
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
        roundRectOutlineSize = UnsetSize
        roundRectOutlineTopLeft = UnsetOffset
        roundRectCornerRadius = 0f
        outlineDirty = true
    }

    /**
     * Specifies the given path to be configured as the outline for this [GraphicsLayer].
     * When [shadowElevation] is non-zero a shadow is produced using this [Outline].
     *
     * @param path Path to be used as the Outline for the [GraphicsLayer]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerOutlineSample
     */
    actual fun setPathOutline(path: Path) {
        resetOutlineParams()
        this.outlinePath = path
    }

    /**
     * Specifies a round rect as the outline.
     * By default, both [topLeft] and [size] are set to [UnsetOffset] and [UnsetSize] indicating
     * that the outline should match the bounds of the [GraphicsLayer].
     *
     * @param topLeft The top left of the rounded rect outline
     * @param size The size of the rounded rect outline
     * @param cornerRadius The corner radius of the rounded rect outline
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRoundRectOutline
     */
    actual fun setRoundRectOutline(topLeft: IntOffset, size: IntSize, cornerRadius: Float) {
        if (this.roundRectOutlineTopLeft != topLeft ||
            this.roundRectOutlineSize != size ||
            this.roundRectCornerRadius != cornerRadius
        ) {
            resetOutlineParams()
            this.roundRectOutlineTopLeft = topLeft
            this.roundRectOutlineSize = size
            this.roundRectCornerRadius = cornerRadius
        }
    }

    /**
     * Configures a rectangular outline for this [GraphicsLayer]. By default, both [topLeft] and
     * [size] are set to [UnsetOffset] and [UnsetSize] indicating that the outline should match the
     * bounds of the [GraphicsLayer]. When [shadowElevation] is non-zero a shadow is produced
     * using with an [Outline] created from the rect parameters provided. Additionally if
     * [clip] is true, the contents of this [GraphicsLayer] will be clipped to this geometry.
     *
     * @param topLeft The top left of the rounded rect outline
     * @param size The size of the rounded rect outline
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRectOutline
     */
    actual fun setRectOutline(
        topLeft: IntOffset,
        size: IntSize
    ) {
        setRoundRectOutline(topLeft, size, 0f)
    }

    /**
     * Sets the color of the ambient shadow that is drawn when [shadowElevation] > 0f.
     *
     * By default the shadow color is black. Generally, this color will be opaque so the intensity
     * of the shadow is consistent between different graphics layers with different colors.
     *
     * The opacity of the final ambient shadow is a function of the shadow caster height, the
     * alpha channel of the [ambientShadowColor] (typically opaque), and the
     * [android.R.attr.ambientShadowAlpha] theme attribute.
     *
     * Note that this parameter is only supported on Android 9 (Pie) and above. On older versions,
     * this property always returns [Color.Black] and setting new values is ignored.
     */
    actual var ambientShadowColor: Color = Color.Black
        set(value) {
            if (field != value) {
                impl.ambientShadowColor = value
                field = value
            }
        }

    /**
     * Sets the color of the spot shadow that is drawn when [shadowElevation] > 0f.
     *
     * By default the shadow color is black. Generally, this color will be opaque so the intensity
     * of the shadow is consistent between different graphics layers with different colors.
     *
     * The opacity of the final spot shadow is a function of the shadow caster height, the
     * alpha channel of the [spotShadowColor] (typically opaque), and the
     * [android.R.attr.spotShadowAlpha] theme attribute.
     *
     * Note that this parameter is only supported on Android 9 (Pie) and above. On older versions,
     * this property always returns [Color.Black] and setting new values is ignored.
     */
    actual var spotShadowColor: Color = Color.Black
        set(value) {
            if (field != value) {
                impl.spotShadowColor = value
                field = value
            }
        }

    actual companion object {
        actual val UnsetOffset = IntOffset(Int.MIN_VALUE, Int.MIN_VALUE)
        actual val UnsetSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)
    }
}

internal interface GraphicsLayerImpl {

    /**
     * The ID of the layer. This is used by tooling to match a layer to the associated
     * LayoutNode.
     */
    val layerId: Long

    /**
     * The uniqueDrawingId of the owner view of this graphics layer. This is used by
     * tooling to match a layer to the associated owner AndroidComposeView.
     */
    val ownerId: Long

    /**
     * @see GraphicsLayer.compositingStrategy
     */
    var compositingStrategy: CompositingStrategy

    /**
     * @see GraphicsLayer.pivotOffset
     */
    var pivotOffset: Offset

    /**
     * @see GraphicsLayer.alpha
     */
    var alpha: Float

    /**
     * @see GraphicsLayer.blendMode
     */
    var blendMode: BlendMode

    /**
     * @see GraphicsLayer.colorFilter
     */
    var colorFilter: ColorFilter?

    /**
     * @see GraphicsLayer.scaleX
     */
    var scaleX: Float

    /**
     * @see GraphicsLayer.scaleY
     */
    var scaleY: Float

    /**
     * @see GraphicsLayer.translationX
     */
    var translationX: Float

    /**
     * @see GraphicsLayer.translationY
     */
    var translationY: Float

    /**
     * @see GraphicsLayer.shadowElevation
     */
    var shadowElevation: Float

    /**
     * @see GraphicsLayer.ambientShadowColor
     */
    var ambientShadowColor: Color

    /**
     * @see GraphicsLayer.spotShadowColor
     */
    var spotShadowColor: Color

    /**
     * @see GraphicsLayer.rotationX
     */
    var rotationX: Float

    /**
     * @see GraphicsLayer.rotationY
     */
    var rotationY: Float

    /**
     * @see GraphicsLayer.rotationZ
     */
    var rotationZ: Float

    /**
     * @see GraphicsLayer.cameraDistance
     */
    var cameraDistance: Float

    /**
     * @see GraphicsLayer.clip
     */
    var clip: Boolean

    /**
     * @see GraphicsLayer.renderEffect
     */
    var renderEffect: RenderEffect?

    /**
     * Determine whether the GraphicsLayer implementation should invalidate itself
     */
    var isInvalidated: Boolean

    /**
     * @see GraphicsLayer.setPosition
     */
    fun setPosition(topLeft: IntOffset, size: IntSize)

    /**
     * @see GraphicsLayer.setPathOutline
     * @see GraphicsLayer.setRoundRectOutline
     */
    fun setOutline(outline: AndroidOutline, clip: Boolean)

    /**
     * Draw the GraphicsLayer into the provided canvas
     */
    fun draw(canvas: Canvas)

    /**
     * @see GraphicsLayer.buildLayer
     */
    fun buildLayer(
        density: Density,
        layoutDirection: LayoutDirection,
        block: DrawScope.() -> Unit
    )

    /**
     * @see GraphicsLayer.discardDisplayList
     */
    fun discardDisplayList()

    /**
     * @see GraphicsLayer.release
     */
    fun release()
}

@RequiresApi(Build.VERSION_CODES.R)
internal object OutlineVerificationHelper {

    @androidx.annotation.DoNotInline
    fun setPath(outline: AndroidOutline, path: Path) {
        outline.setPath(path.asAndroidPath())
    }
}
