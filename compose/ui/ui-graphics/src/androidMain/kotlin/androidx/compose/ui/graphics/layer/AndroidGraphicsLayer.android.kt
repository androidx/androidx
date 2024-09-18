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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DefaultDensity
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.LayerManager.Companion.isRobolectric
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastRoundToInt
import org.jetbrains.annotations.TestOnly

@Suppress("NotCloseable")
actual class GraphicsLayer
internal constructor(
    internal val impl: GraphicsLayerImpl,
    private val layerManager: LayerManager?
) {
    private var density = DefaultDensity
    private var layoutDirection = LayoutDirection.Ltr
    private var drawBlock: DrawScope.() -> Unit = {}

    private var androidOutline: AndroidOutline? = null
    private var outlineDirty = true
    private var roundRectOutlineTopLeft: Offset = Offset.Zero
    private var roundRectOutlineSize: Size = Size.Unspecified
    private var roundRectCornerRadius: Float = 0f

    private var internalOutline: Outline? = null
    private var outlinePath: Path? = null
    private var roundRectClipPath: Path? = null
    private var usePathForClip = false

    // Paint used only in Software rendering scenarios for API 21 when rendering to a Bitmap
    private var softwareLayerPaint: Paint? = null

    /** Tracks the amount of the parent layers currently drawing this layer as a child. */
    private var parentLayerUsages = 0

    /** Keeps track of the child layers we currently draw into this layer. */
    private val childDependenciesTracker = ChildLayerDependenciesTracker()

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
     * [CompositingStrategy.ModulateAlpha] which will skip the overhead of an offscreen buffer but
     * can generate different rendering results depending on whether or not the contents of the
     * layer are overlapping. Similarly leveraging [CompositingStrategy.Offscreen] is useful in
     * situations where creating an offscreen buffer is preferred usually in conjunction with
     * [BlendMode] usage.
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
     * [drawLayer] is called. This is configured by calling [record]
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
     * of the bounds specified by [topLeft] and [size], however, rasterization of this layer into an
     * offscreen buffer will be sized according to the specified size. This is configured by calling
     * [record]
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerSizeSample
     */
    actual var size: IntSize = IntSize.Zero
        private set(value) {
            if (field != value) {
                field = value
                setPosition(topLeft, value)
                if (roundRectOutlineSize.isUnspecified) {
                    outlineDirty = true
                    configureOutlineAndClip()
                }
            }
        }

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
     * BlendMode to use when drawing this layer to the destination in [drawLayer]. The default is
     * [BlendMode.SrcOver]. Any value other than [BlendMode.SrcOver] will force this [GraphicsLayer]
     * to use an offscreen compositing layer for rendering and is equivalent to using
     * [CompositingStrategy.Offscreen].
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
     * ColorFilter applied when drawing this layer to the destination in [drawLayer]. Setting of
     * this to any non-null will force this [GraphicsLayer] to use an offscreen compositing layer
     * for rendering and is equivalent to using [CompositingStrategy.Offscreen]
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
     * Sets the elevation for the shadow in pixels. With the [shadowElevation] > 0f and [Outline]
     * set, a shadow is produced. Default value is `0` and the value must not be negative.
     * Configuring a non-zero [shadowElevation] enables clipping of [GraphicsLayer] content.
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
                outlineDirty = true
                configureOutlineAndClip()
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
     * The rotation, in degrees, of the contents around the vertical axis in degrees. Default value
     * is `0`.
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
     * The rotation, in degrees, of the contents around the Z axis in degrees. Default value is `0`.
     */
    actual var rotationZ: Float
        get() = impl.rotationZ
        set(value) {
            if (impl.rotationZ != value) {
                impl.rotationZ = value
            }
        }

    /**
     * Sets the distance along the Z axis (orthogonal to the X/Y plane on which layers are drawn)
     * from the camera to this layer. The camera's distance affects 3D transformations, for instance
     * rotations around the X and Y axis. If the rotationX or rotationY properties are changed and
     * this view is large (more than half the size of the screen), it is recommended to always use a
     * camera distance that's greater than the height (X axis rotation) or the width (Y axis
     * rotation) of this view.
     *
     * The distance of the camera from the drawing plane can have an affect on the perspective
     * distortion of the layer when it is rotated around the x or y axis. For example, a large
     * distance will result in a large viewing angle, and there will not be much perspective
     * distortion of the view as it rotates. A short distance may cause much more perspective
     * distortion upon rotation, and can also result in some drawing artifacts if the rotated view
     * ends up partially behind the camera (which is why the recommendation is to use a distance at
     * least as far as the size of the view, if the view is to be rotated.)
     *
     * The distance is expressed in pixels and must always be positive. Default value is
     * [DefaultCameraDistance]
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
     * [topLeft] and [size] or to the Outline if one is provided. The default is false. Note if
     * elevation is provided then clipping will be enabled.
     */
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    actual var clip: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                outlineDirty = true
                configureOutlineAndClip()
            }
        }

    /**
     * Configure the [RenderEffect] to apply to this [GraphicsLayer]. This will apply a visual
     * effect to the results of the [GraphicsLayer] before it is drawn. For example if [BlurEffect]
     * is provided, the contents will be drawn in a separate layer, then this layer will be blurred
     * when this [GraphicsLayer] is drawn.
     *
     * Note this parameter is only supported on Android 12 and above. Attempts to use this Modifier
     * on older Android versions will be ignored.
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
     * @param topLeft Offset of the [GraphicsLayer]. For convenience, this is set to [topLeft] for
     *   use cases where only the [size] is desired to be changed.
     * @param size Size of the [GraphicsLayer]. For convenience, this is set to [size] for use cases
     *   where only the [topLeft] is desired to be changed
     */
    private fun setPosition(topLeft: IntOffset, size: IntSize) {
        impl.setPosition(topLeft.x, topLeft.y, size)
    }

    /**
     * Constructs the display list of drawing commands into this layer that will be rendered when
     * this [GraphicsLayer] is drawn elsewhere with [drawLayer].
     *
     * @param density [Density] used to assist in conversions of density independent pixels to raw
     *   pixels to draw.
     * @param layoutDirection [LayoutDirection] of the layout being drawn in.
     * @param size [Size] of the [GraphicsLayer]
     * @param block lambda that is called to issue drawing commands on this [DrawScope]
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerTopLeftSample
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerBlendModeSample
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerTranslateSample
     */
    actual fun record(
        density: Density,
        layoutDirection: LayoutDirection,
        size: IntSize,
        block: DrawScope.() -> Unit
    ) {
        this.size = size
        this.density = density
        this.layoutDirection = layoutDirection
        this.drawBlock = block
        impl.isInvalidated = true
        recordInternal()
    }

    private fun recordInternal() {
        childDependenciesTracker.withTracking(
            onDependencyRemoved = { it.onRemovedFromParentLayer() }
        ) {
            impl.record(density, layoutDirection, this, drawBlock)
        }
    }

    private fun addSubLayer(graphicsLayer: GraphicsLayer) {
        if (childDependenciesTracker.onDependencyAdded(graphicsLayer)) {
            graphicsLayer.onAddedToParentLayer()
        }
    }

    private fun transformCanvas(androidCanvas: android.graphics.Canvas) {
        val left = topLeft.x.toFloat()
        val top = topLeft.y.toFloat()
        val right = topLeft.x + size.width.toFloat()
        val bottom = topLeft.y + size.height.toFloat()
        // If there is alpha applied, we must render into an offscreen buffer to
        // properly blend the contents of this layer against the background content
        val layerAlpha = alpha
        val layerColorFilter = colorFilter
        val layerBlendMode = blendMode
        val useSaveLayer =
            layerAlpha < 1.0f ||
                layerBlendMode != BlendMode.SrcOver ||
                layerColorFilter != null ||
                compositingStrategy == CompositingStrategy.Offscreen
        if (useSaveLayer) {
            val paint =
                (softwareLayerPaint ?: Paint().also { softwareLayerPaint = it }).apply {
                    alpha = layerAlpha
                    blendMode = layerBlendMode
                    colorFilter = layerColorFilter
                }
            androidCanvas.saveLayer(left, top, right, bottom, paint.asFrameworkPaint())
        } else {
            androidCanvas.save()
        }
        // If we are software rendered we must translate the canvas based on the offset provided
        // in the move call which operates directly on the RenderNode
        androidCanvas.translate(left, top)
        androidCanvas.concat(impl.calculateMatrix())
    }

    internal fun drawForPersistence(canvas: Canvas) {
        if (canvas.nativeCanvas.isHardwareAccelerated) {
            recreateDisplayListIfNeeded()
            impl.draw(canvas)
        }
    }

    private fun recreateDisplayListIfNeeded() {
        // If the displaylist has been discarded from underneath us, attempt to recreate it.
        // This can happen if the application resumes from a background state after a trim memory
        // callback has been invoked with a level greater than or equal to hidden. During which
        // HWUI attempts to cull out resources that can be recreated quickly.
        // Because recording instructions invokes the draw lambda again, there can be the potential
        // for the objects referenced to be invalid for example in the case of a lazylist removal
        // animation for a Composable that has been disposed, but the GraphicsLayer is drawn
        // for a transient animation. However, when the application is backgrounded, animations are
        // stopped anyway so attempts to recreate the displaylist from the draw lambda should
        // be safe as the draw lambdas should still be valid. If not catch potential exceptions
        // and continue as UI state would be recreated on resume anyway.
        if (!impl.hasDisplayList) {
            try {
                recordInternal()
            } catch (_: Throwable) {
                // NO-OP
            }
        }
    }

    /** Draw the contents of this [GraphicsLayer] into the specified [Canvas] */
    internal actual fun draw(canvas: Canvas, parentLayer: GraphicsLayer?) {
        if (isReleased) {
            return
        }

        recreateDisplayListIfNeeded()

        configureOutlineAndClip()
        val useZ = shadowElevation > 0f
        if (useZ) {
            canvas.enableZ()
        }
        val androidCanvas = canvas.nativeCanvas
        val softwareRendered = !androidCanvas.isHardwareAccelerated
        if (softwareRendered) {
            androidCanvas.save()
            transformCanvas(androidCanvas)
        }

        val willClipPath = usePathForClip || (softwareRendered && clip)
        if (willClipPath) {
            canvas.save()
            when (val tmpOutline = outline) {
                is Outline.Rectangle -> {
                    canvas.clipRect(tmpOutline.bounds)
                }
                is Outline.Rounded -> {
                    val rRectPath =
                        roundRectClipPath?.apply { rewind() }
                            ?: Path().also { roundRectClipPath = it }
                    rRectPath.addRoundRect(tmpOutline.roundRect)
                    canvas.clipPath(rRectPath)
                }
                is Outline.Generic -> {
                    canvas.clipPath(tmpOutline.path)
                }
            }
        }

        parentLayer?.addSubLayer(this)

        impl.draw(canvas)
        if (willClipPath) {
            canvas.restore()
        }
        if (useZ) {
            canvas.disableZ()
        }
        if (softwareRendered) {
            androidCanvas.restore()
        }
    }

    private fun onAddedToParentLayer() {
        parentLayerUsages++
    }

    private fun onRemovedFromParentLayer() {
        parentLayerUsages--
        discardContentIfReleasedAndHaveNoParentLayerUsages()
    }

    private fun configureOutlineAndClip() {
        if (outlineDirty) {
            val outlineIsNeeded = clip || shadowElevation > 0f
            if (!outlineIsNeeded) {
                impl.clip = false
                impl.setOutline(null)
            } else {
                val tmpPath = outlinePath
                if (tmpPath != null) {
                    val androidOutline =
                        updatePathOutline(tmpPath)?.apply { alpha = this@GraphicsLayer.alpha }
                    impl.setOutline(androidOutline)
                    if (usePathForClip && clip) {
                        impl.clip = false
                    } else {
                        impl.clip = clip
                    }
                } else {
                    impl.clip = clip
                    val roundRectOutline =
                        obtainAndroidOutline()
                            .apply {
                                resolveOutlinePosition { outlineTopLeft, outlineSize ->
                                    setRoundRect(
                                        outlineTopLeft.x.fastRoundToInt(),
                                        outlineTopLeft.y.fastRoundToInt(),
                                        (outlineTopLeft.x + outlineSize.width).fastRoundToInt(),
                                        (outlineTopLeft.y + outlineSize.height).fastRoundToInt(),
                                        roundRectCornerRadius
                                    )
                                }
                            }
                            .apply { alpha = this@GraphicsLayer.alpha }
                    impl.setOutline(roundRectOutline)
                }
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

    // Suppress deprecation for usage of setConvexPath in favor of setPath on API levels that
    // previously only supported convex path outlines
    @Suppress("deprecation")
    private fun updatePathOutline(path: Path): AndroidOutline? {
        val resultOutline: AndroidOutline?
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P || path.isConvex) {
            resultOutline = obtainAndroidOutline()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                OutlineVerificationHelper.setPath(resultOutline, path)
            } else {
                resultOutline.setConvexPath(path.asAndroidPath())
            }
            usePathForClip = !resultOutline.canClip()
        } else { // Concave outlines are not supported on older API levels
            androidOutline?.setEmpty()
            resultOutline = null
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
            isReleased = true
            discardContentIfReleasedAndHaveNoParentLayerUsages()
        }
    }

    private fun discardContentIfReleasedAndHaveNoParentLayerUsages() {
        if (isReleased && parentLayerUsages == 0) {
            if (layerManager != null) {
                layerManager.release(this)
            } else {
                discardDisplayList()
            }
        }
    }

    /**
     * Discards the displaylist of the GraphicsLayer. Used internally for management of
     * GraphicsLayer resources
     */
    internal fun discardDisplayList() {
        // discarding means we don't draw children layer anymore and need to remove dependencies:
        childDependenciesTracker.removeDependencies { it.onRemovedFromParentLayer() }
        impl.discardDisplayList()
    }

    /**
     * When the system is sending trim memory request all the render nodes will discard their
     * display list. in this case we are not being notified about that and don't update
     * [childDependenciesTracker], as it is done when we call [discardDisplayList] manually
     */
    @TestOnly
    internal fun emulateTrimMemory() {
        impl.discardDisplayList()
    }

    /**
     * The ID of the layer. This is used by tooling to match a layer to the associated LayoutNode.
     */
    val layerId: Long
        get() = impl.layerId

    /**
     * The uniqueDrawingId of the owner view of this graphics layer. This is used by tooling to
     * match a layer to the associated owner View.
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
                    }
                    .also { internalOutline = it }
            }
        }

    private fun resetOutlineParams() {
        internalOutline = null
        outlinePath = null
        roundRectOutlineSize = Size.Unspecified
        roundRectOutlineTopLeft = Offset.Zero
        roundRectCornerRadius = 0f
        outlineDirty = true
        usePathForClip = false
    }

    /**
     * Specifies the given path to be configured as the outline for this [GraphicsLayer]. When
     * [shadowElevation] is non-zero a shadow is produced using this [Outline].
     *
     * @param path Path to be used as the Outline for the [GraphicsLayer]
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerOutlineSample
     */
    actual fun setPathOutline(path: Path) {
        resetOutlineParams()
        this.outlinePath = path
        configureOutlineAndClip()
    }

    /**
     * Configures a rounded rect outline for this [GraphicsLayer]. By default, [topLeft] is set to
     * [Size.Zero] and [size] is set to [Size.Unspecified] indicating that the outline should match
     * the size of the [GraphicsLayer]. When [shadowElevation] is non-zero a shadow is produced
     * using an [Outline] created from the round rect parameters provided. Additionally if [clip] is
     * true, the contents of this [GraphicsLayer] will be clipped to this geometry.
     *
     * @param topLeft The top left of the rounded rect outline
     * @param size The size of the rounded rect outline
     * @param cornerRadius The corner radius of the rounded rect outline
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRoundRectOutline
     */
    actual fun setRoundRectOutline(topLeft: Offset, size: Size, cornerRadius: Float) {
        if (
            this.roundRectOutlineTopLeft != topLeft ||
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

    /**
     * Configures a rectangular outline for this [GraphicsLayer]. By default, both [topLeft] and
     * [size] are set to [Offset.Unspecified] and [Size.Unspecified] indicating that the outline
     * should match the bounds of the [GraphicsLayer]. When [shadowElevation] is non-zero a shadow
     * is produced using with an [Outline] created from the rect parameters provided. Additionally
     * if [clip] is true, the contents of this [GraphicsLayer] will be clipped to this geometry.
     *
     * @param topLeft The top left of the rounded rect outline
     * @param size The size of the rounded rect outline
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerRectOutline
     */
    actual fun setRectOutline(topLeft: Offset, size: Size) {
        setRoundRectOutline(topLeft, size, 0f)
    }

    /**
     * Sets the color of the ambient shadow that is drawn when [shadowElevation] > 0f.
     *
     * By default the shadow color is black. Generally, this color will be opaque so the intensity
     * of the shadow is consistent between different graphics layers with different colors.
     *
     * The opacity of the final ambient shadow is a function of the shadow caster height, the alpha
     * channel of the [ambientShadowColor] (typically opaque), and the
     * [android.R.attr.ambientShadowAlpha] theme attribute.
     *
     * Note that this parameter is only supported on Android 9 (Pie) and above. On older versions,
     * this property always returns [Color.Black] and setting new values is ignored.
     */
    actual var ambientShadowColor: Color
        get() = impl.ambientShadowColor
        set(value) {
            if (value != impl.ambientShadowColor) {
                impl.ambientShadowColor = value
            }
        }

    /**
     * Sets the color of the spot shadow that is drawn when [shadowElevation] > 0f.
     *
     * By default the shadow color is black. Generally, this color will be opaque so the intensity
     * of the shadow is consistent between different graphics layers with different colors.
     *
     * The opacity of the final spot shadow is a function of the shadow caster height, the alpha
     * channel of the [spotShadowColor] (typically opaque), and the [android.R.attr.spotShadowAlpha]
     * theme attribute.
     *
     * Note that this parameter is only supported on Android 9 (Pie) and above. On older versions,
     * this property always returns [Color.Black] and setting new values is ignored.
     */
    actual var spotShadowColor: Color
        get() = impl.spotShadowColor
        set(value) {
            if (value != impl.spotShadowColor) {
                impl.spotShadowColor = value
            }
        }

    /**
     * Create an [ImageBitmap] with the contents of this [GraphicsLayer] instance. Note that
     * [GraphicsLayer.record] must be invoked first to record drawing operations before invoking
     * this method.
     *
     * @sample androidx.compose.ui.graphics.samples.GraphicsLayerToImageBitmap
     */
    actual suspend fun toImageBitmap(): ImageBitmap = SnapshotImpl.toBitmap(this).asImageBitmap()

    companion object {

        // See b/340578758, fallback to software rendering for Robolectric tests
        private val SnapshotImpl =
            if (isRobolectric) {
                LayerSnapshotV21
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                LayerSnapshotV28
            } else if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 &&
                    SurfaceUtils.isLockHardwareCanvasAvailable()
            ) {
                LayerSnapshotV22
            } else {
                LayerSnapshotV21
            }
    }
}

internal interface GraphicsLayerImpl {

    /**
     * The ID of the layer. This is used by tooling to match a layer to the associated LayoutNode.
     */
    val layerId: Long

    /**
     * The uniqueDrawingId of the owner view of this graphics layer. This is used by tooling to
     * match a layer to the associated owner AndroidComposeView.
     */
    val ownerId: Long

    /** @see GraphicsLayer.compositingStrategy */
    var compositingStrategy: CompositingStrategy

    /** @see GraphicsLayer.pivotOffset */
    var pivotOffset: Offset

    /** @see GraphicsLayer.alpha */
    var alpha: Float

    /** @see GraphicsLayer.blendMode */
    var blendMode: BlendMode

    /** @see GraphicsLayer.colorFilter */
    var colorFilter: ColorFilter?

    /** @see GraphicsLayer.scaleX */
    var scaleX: Float

    /** @see GraphicsLayer.scaleY */
    var scaleY: Float

    /** @see GraphicsLayer.translationX */
    var translationX: Float

    /** @see GraphicsLayer.translationY */
    var translationY: Float

    /** @see GraphicsLayer.shadowElevation */
    var shadowElevation: Float

    /** @see GraphicsLayer.ambientShadowColor */
    var ambientShadowColor: Color

    /** @see GraphicsLayer.spotShadowColor */
    var spotShadowColor: Color

    /** @see GraphicsLayer.rotationX */
    var rotationX: Float

    /** @see GraphicsLayer.rotationY */
    var rotationY: Float

    /** @see GraphicsLayer.rotationZ */
    var rotationZ: Float

    /** @see GraphicsLayer.cameraDistance */
    var cameraDistance: Float

    /** @see GraphicsLayer.clip */
    var clip: Boolean

    /** @see GraphicsLayer.renderEffect */
    var renderEffect: RenderEffect?

    /** Determine whether the GraphicsLayer implementation should invalidate itself */
    var isInvalidated: Boolean

    /** @see GraphicsLayer.setPosition */
    fun setPosition(x: Int, y: Int, size: IntSize)

    /**
     * @see GraphicsLayer.setPathOutline
     * @see GraphicsLayer.setRoundRectOutline
     */
    fun setOutline(outline: AndroidOutline?)

    /** Draw the GraphicsLayer into the provided canvas */
    fun draw(canvas: Canvas)

    /** @see GraphicsLayer.record */
    fun record(
        density: Density,
        layoutDirection: LayoutDirection,
        layer: GraphicsLayer,
        block: DrawScope.() -> Unit
    )

    val hasDisplayList: Boolean
        get() = true

    /** @see GraphicsLayer.discardDisplayList */
    fun discardDisplayList()

    /** Calculate the current transformation matrix for the layer implementation */
    fun calculateMatrix(): android.graphics.Matrix

    companion object {
        val DefaultDrawBlock: DrawScope.() -> Unit = { drawRect(Color.Transparent) }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
internal object OutlineVerificationHelper {

    fun setPath(outline: AndroidOutline, path: Path) {
        outline.setPath(path.asAndroidPath())
    }
}
