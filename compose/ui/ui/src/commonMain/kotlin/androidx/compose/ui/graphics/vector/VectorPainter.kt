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

package androidx.compose.ui.graphics.vector

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsResourceCache
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.packFloats

/** Default identifier for the root group if a Vector graphic */
public const val RootGroupName: String = "VectorRootGroup"

/**
 * Create a [VectorPainter] with the Vector defined by the provided sub-composition
 *
 * @param [defaultWidth] Intrinsic width of the Vector in [Dp]
 * @param [defaultHeight] Intrinsic height of the Vector in [Dp]
 * @param [viewportWidth] Width of the viewport space. The viewport is the virtual canvas where
 *   paths are drawn on. This parameter is optional. Not providing it will use the [defaultWidth]
 *   converted to pixels
 * @param [viewportHeight] Height of the viewport space. The viewport is the virtual canvas where
 *   paths are drawn on. This parameter is optional. Not providing it will use the [defaultHeight]
 *   converted to pixels
 * @param [name] optional identifier used to identify the root of this vector graphic
 * @param [tintColor] optional color used to tint the root group of this vector graphic
 * @param [tintBlendMode] BlendMode used in combination with [tintColor]
 * @param [content] Composable used to define the structure and contents of the vector graphic
 */
@Deprecated(
    "Replace rememberVectorPainter graphicsLayer that consumes the auto mirror flag",
    replaceWith =
        ReplaceWith(
            "rememberVectorPainter(defaultWidth, defaultHeight, viewportWidth, " +
                "viewportHeight, name, tintColor, tintBlendMode, false, content)",
            "androidx.compose.ui.graphics.vector",
        ),
)
@Composable
@ComposableOpenTarget(-1)
public fun rememberVectorPainter(
    defaultWidth: Dp,
    defaultHeight: Dp,
    viewportWidth: Float = Float.NaN,
    viewportHeight: Float = Float.NaN,
    name: String = RootGroupName,
    tintColor: Color = Color.Unspecified,
    tintBlendMode: BlendMode = BlendMode.SrcIn,
    content: @Composable @VectorComposable (viewportWidth: Float, viewportHeight: Float) -> Unit,
): VectorPainter =
    rememberVectorPainter(
        defaultWidth,
        defaultHeight,
        viewportWidth,
        viewportHeight,
        name,
        tintColor,
        tintBlendMode,
        false,
        content,
    )

/**
 * Create a [VectorPainter] with the Vector defined by the provided sub-composition.
 *
 * Inside [content] use the [Group] and [Path] composables to define the vector.
 *
 * **Note:** To animate the size of the vector, animate the graphics layer ( `Modifier.graphicsLayer
 * { scaleX = ...; scaleY = ... }`) instead of the layout bounds ( `Modifier.size(...)`). Animating
 * the layout bounds forces the vector to re-render and causes memory churn on every frame.
 *
 * @param [defaultWidth] Intrinsic width of the Vector in [Dp]
 * @param [defaultHeight] Intrinsic height of the Vector in [Dp]
 * @param [viewportWidth] Width of the viewport space. The viewport is the virtual canvas where
 *   paths are drawn on. This parameter is optional. Not providing it will use the [defaultWidth]
 *   converted to pixels
 * @param [viewportHeight] Height of the viewport space. The viewport is the virtual canvas where
 *   paths are drawn on. This parameter is optional. Not providing it will use the [defaultHeight]
 *   converted to pixels
 * @param [name] optional identifier used to identify the root of this vector graphic
 * @param [tintColor] optional color used to tint the root group of this vector graphic
 * @param [tintBlendMode] BlendMode used in combination with [tintColor]
 * @param [autoMirror] Determines if the contents of the Vector should be mirrored for right to left
 *   layouts.
 * @param [content] Composable used to define the structure and contents of the vector graphic
 */
@Composable
@ComposableOpenTarget(-1)
public fun rememberVectorPainter(
    defaultWidth: Dp,
    defaultHeight: Dp,
    viewportWidth: Float = Float.NaN,
    viewportHeight: Float = Float.NaN,
    name: String = RootGroupName,
    tintColor: Color = Color.Unspecified,
    tintBlendMode: BlendMode = BlendMode.SrcIn,
    autoMirror: Boolean = false,
    content: @Composable @VectorComposable (viewportWidth: Float, viewportHeight: Float) -> Unit,
): VectorPainter {
    val density = LocalDensity.current
    val defaultSize = density.obtainSizePx(defaultWidth, defaultHeight)
    val viewport = obtainViewportSize(defaultSize, viewportWidth, viewportHeight)
    val intrinsicColorFilter =
        remember(tintColor, tintBlendMode) { createColorFilter(tintColor, tintBlendMode) }
    return remember { VectorPainter() }
        .apply {
            configureVectorPainter(
                defaultSize = defaultSize,
                viewportSize = viewport,
                name = name,
                intrinsicColorFilter = intrinsicColorFilter,
                autoMirror = autoMirror,
            )
            val compositionContext = rememberCompositionContext()
            val composition =
                remember(viewportWidth, viewportHeight, content) {
                    val curComp = this.composition
                    val next =
                        if (curComp == null || curComp.isDisposed) {
                            Composition(VectorApplier(this.vector.root), compositionContext)
                        } else {
                            curComp
                        }
                    next.setContent { content(viewport.width, viewport.height) }
                    next
                }
            this.composition = composition
            DisposableEffect(this) { onDispose { composition.dispose() } }
        }
}

/**
 * Create a [VectorPainter] with the given [ImageVector]. This will create a sub-composition of the
 * vector hierarchy given the tree structure in [ImageVector].
 *
 * **Note:** To animate the size of the vector, animate the graphics layer (e.g.,
 * `Modifier.graphicsLayer { scaleX = ...; scaleY = ... }`) instead of the layout bounds (e.g.,
 * `Modifier.size(...)`). Animating the layout bounds forces the vector to re-render and causes
 * memory churn on every frame.
 *
 * @param [image] ImageVector used to create a vector graphic sub-composition
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
public fun rememberVectorPainter(image: ImageVector): VectorPainter {
    val density = LocalDensity.current
    val key = packFloats(image.genId.toFloat(), density.density)
    val graphicsResourceCache =
        if (ComposeUiFlags.isVectorDrawCacheSharingEnabled) {
            LocalGraphicsResourceCache.current
        } else {
            null
        }

    val drawCacheProvider =
        remember(key) {
            if (graphicsResourceCache != null) {
                DrawCacheProvider { size, config ->
                    val drawCacheKey =
                        VectorDrawCacheKey(
                            genId = image.genId,
                            densityBits = density.density.toBits(),
                            width = size.width,
                            height = size.height,
                            config = config,
                        )
                    graphicsResourceCache.acquire(drawCacheKey) { DrawCache() }
                }
            } else {
                OwnedDrawCacheProvider()
            }
        }
    return remember(key) {
        createVectorPainterFromImageVector(
            density,
            image,
            GroupComponent().apply { createGroupComponent(image.root) },
            drawCacheProvider = drawCacheProvider,
            trustsSharedContent = graphicsResourceCache != null,
        )
    }
}

internal class VectorDrawCacheKey(
    val genId: Int,
    val densityBits: Int,
    val width: Int,
    val height: Int,
    val config: ImageBitmapConfig,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VectorDrawCacheKey) return false
        return genId == other.genId &&
            densityBits == other.densityBits &&
            width == other.width &&
            height == other.height &&
            config == other.config
    }

    override fun hashCode(): Int {
        var result = genId
        result = 31 * result + densityBits
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + config.hashCode()
        return result
    }
}

/**
 * [Painter] implementation that abstracts the drawing of a Vector graphic. This can be represented
 * by either a [ImageVector] or a programmatic composition of a vector
 */
public class VectorPainter
internal constructor(
    root: GroupComponent = GroupComponent(),
    drawCacheProvider: DrawCacheProvider = OwnedDrawCacheProvider(),
) : Painter() {

    internal var size by mutableStateOf(Size.Zero)

    internal var autoMirror by mutableStateOf(false)

    /** configures the intrinsic tint that may be defined on a VectorPainter */
    internal var intrinsicColorFilter: ColorFilter?
        get() = vector.intrinsicColorFilter
        set(value) {
            vector.intrinsicColorFilter = value
        }

    internal var viewportSize: Size
        get() = vector.viewportSize
        set(value) {
            vector.viewportSize = value
        }

    internal var name: String
        get() = vector.name
        set(value) {
            vector.name = value
        }

    internal val vector =
        VectorComponent(root, drawCacheProvider).apply {
            invalidateCallback = {
                // Trigger redraw
                drawInvalidation = Unit
            }
        }

    internal val bitmapConfig: ImageBitmapConfig
        get() = vector.cacheBitmapConfig

    internal var composition: Composition? = null

    private var drawInvalidation by mutableStateOf(Unit, neverEqualPolicy())

    private var currentAlpha: Float = 1.0f
    private var currentColorFilter: ColorFilter? = null

    public override val intrinsicSize: Size
        get() = size

    protected override fun DrawScope.onDraw() {
        with(vector) {
            val filter = currentColorFilter ?: intrinsicColorFilter
            if (autoMirror && layoutDirection == LayoutDirection.Rtl) {
                mirror { draw(currentAlpha, filter) }
            } else {
                draw(currentAlpha, filter)
            }
        }
        // State read
        drawInvalidation
    }

    protected override fun applyAlpha(alpha: Float): Boolean {
        currentAlpha = alpha
        return true
    }

    protected override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        currentColorFilter = colorFilter
        return true
    }
}

private inline fun DrawScope.mirror(block: DrawScope.() -> Unit) {
    scale(-1f, 1f, block = block)
}

/**
 * Represents one of the properties for PathComponent or GroupComponent that can be overwritten when
 * it is composed and drawn with [RenderVectorGroup].
 */
public sealed class VectorProperty<T> {
    public object Rotation : VectorProperty<Float>()

    public object PivotX : VectorProperty<Float>()

    public object PivotY : VectorProperty<Float>()

    public object ScaleX : VectorProperty<Float>()

    public object ScaleY : VectorProperty<Float>()

    public object TranslateX : VectorProperty<Float>()

    public object TranslateY : VectorProperty<Float>()

    public object PathData : VectorProperty<List<PathNode>>()

    public object Fill : VectorProperty<Brush?>()

    public object FillAlpha : VectorProperty<Float>()

    public object Stroke : VectorProperty<Brush?>()

    public object StrokeLineWidth : VectorProperty<Float>()

    public object StrokeAlpha : VectorProperty<Float>()

    public object TrimPathStart : VectorProperty<Float>()

    public object TrimPathEnd : VectorProperty<Float>()

    public object TrimPathOffset : VectorProperty<Float>()
}

/**
 * Holds a set of values that overwrite the original property values of an [ImageVector]. This
 * allows you to dynamically change any of the property values provided as [VectorProperty]. This
 * can be passed to [RenderVectorGroup] to alter some property values when the [VectorGroup] is
 * rendered.
 */
@JvmDefaultWithCompatibility
public interface VectorConfig {
    public fun <T> getOrDefault(property: VectorProperty<T>, defaultValue: T): T {
        return defaultValue
    }
}

private fun Density.obtainSizePx(defaultWidth: Dp, defaultHeight: Dp) =
    Size(defaultWidth.toPx(), defaultHeight.toPx())

/**
 * Helper method to calculate the viewport size. If the viewport width/height are not specified this
 * falls back on the default size provided
 */
private fun obtainViewportSize(defaultSize: Size, viewportWidth: Float, viewportHeight: Float) =
    Size(
        if (viewportWidth.isNaN()) defaultSize.width else viewportWidth,
        if (viewportHeight.isNaN()) defaultSize.height else viewportHeight,
    )

/**
 * Helper method to conditionally create a ColorFilter to tint contents if [tintColor] is specified,
 * that is [Color.isSpecified] returns true
 */
private fun createColorFilter(tintColor: Color, tintBlendMode: BlendMode): ColorFilter? =
    if (tintColor.isSpecified) {
        ColorFilter.tint(tintColor, tintBlendMode)
    } else {
        null
    }

/** Helper method to configure the properties of a VectorPainter that maybe re-used */
internal fun VectorPainter.configureVectorPainter(
    defaultSize: Size,
    viewportSize: Size,
    name: String = RootGroupName,
    intrinsicColorFilter: ColorFilter?,
    autoMirror: Boolean = false,
): VectorPainter = apply {
    this.size = defaultSize
    this.autoMirror = autoMirror
    this.intrinsicColorFilter = intrinsicColorFilter
    this.viewportSize = viewportSize
    this.name = name
}

/** Helper method to create a VectorPainter instance from an ImageVector */
internal fun createVectorPainterFromImageVector(
    density: Density,
    imageVector: ImageVector,
    root: GroupComponent,
    drawCacheProvider: DrawCacheProvider,
    trustsSharedContent: Boolean = false,
): VectorPainter {
    val defaultSize = density.obtainSizePx(imageVector.defaultWidth, imageVector.defaultHeight)
    val viewport =
        obtainViewportSize(defaultSize, imageVector.viewportWidth, imageVector.viewportHeight)
    return VectorPainter(root, drawCacheProvider)
        .apply { vector.sharedContentEnabled = trustsSharedContent }
        .configureVectorPainter(
            defaultSize = defaultSize,
            viewportSize = viewport,
            name = imageVector.name,
            intrinsicColorFilter =
                createColorFilter(imageVector.tintColor, imageVector.tintBlendMode),
            autoMirror = imageVector.autoMirror,
        )
}

/**
 * statically create a GroupComponent from the VectorGroup representation provided from an
 * [ImageVector] instance
 */
internal fun GroupComponent.createGroupComponent(currentGroup: VectorGroup): GroupComponent {
    for (index in 0 until currentGroup.size) {
        val vectorNode = currentGroup[index]
        if (vectorNode is VectorPath) {
            val pathComponent =
                PathComponent().apply {
                    pathData = vectorNode.pathData
                    pathFillType = vectorNode.pathFillType
                    name = vectorNode.name
                    fill = vectorNode.fill
                    fillAlpha = vectorNode.fillAlpha
                    stroke = vectorNode.stroke
                    strokeAlpha = vectorNode.strokeAlpha
                    strokeLineWidth = vectorNode.strokeLineWidth
                    strokeLineCap = vectorNode.strokeLineCap
                    strokeLineJoin = vectorNode.strokeLineJoin
                    strokeLineMiter = vectorNode.strokeLineMiter
                    trimPathStart = vectorNode.trimPathStart
                    trimPathEnd = vectorNode.trimPathEnd
                    trimPathOffset = vectorNode.trimPathOffset
                }
            insertAt(index, pathComponent)
        } else if (vectorNode is VectorGroup) {
            val groupComponent =
                GroupComponent().apply {
                    name = vectorNode.name
                    rotation = vectorNode.rotation
                    scaleX = vectorNode.scaleX
                    scaleY = vectorNode.scaleY
                    translationX = vectorNode.translationX
                    translationY = vectorNode.translationY
                    pivotX = vectorNode.pivotX
                    pivotY = vectorNode.pivotY
                    clipPathData = vectorNode.clipPathData
                    createGroupComponent(vectorNode)
                }
            insertAt(index, groupComponent)
        }
    }
    return this
}

/**
 * Recursively creates the vector graphic composition by traversing the tree structure.
 *
 * @param group The vector group to render.
 * @param configs An optional map of [VectorConfig] to provide animation values. The keys are the
 *   node names. The values are [VectorConfig] for that node.
 */
@Composable
public fun RenderVectorGroup(
    group: VectorGroup,
    configs: Map<String, VectorConfig> = emptyMap(),
): Unit {
    for (vectorNode in group) {
        if (vectorNode is VectorPath) {
            val config = configs[vectorNode.name] ?: object : VectorConfig {}
            Path(
                pathData = config.getOrDefault(VectorProperty.PathData, vectorNode.pathData),
                pathFillType = vectorNode.pathFillType,
                name = vectorNode.name,
                fill = config.getOrDefault(VectorProperty.Fill, vectorNode.fill),
                fillAlpha = config.getOrDefault(VectorProperty.FillAlpha, vectorNode.fillAlpha),
                stroke = config.getOrDefault(VectorProperty.Stroke, vectorNode.stroke),
                strokeAlpha =
                    config.getOrDefault(VectorProperty.StrokeAlpha, vectorNode.strokeAlpha),
                strokeLineWidth =
                    config.getOrDefault(VectorProperty.StrokeLineWidth, vectorNode.strokeLineWidth),
                strokeLineCap = vectorNode.strokeLineCap,
                strokeLineJoin = vectorNode.strokeLineJoin,
                strokeLineMiter = vectorNode.strokeLineMiter,
                trimPathStart =
                    config.getOrDefault(VectorProperty.TrimPathStart, vectorNode.trimPathStart),
                trimPathEnd =
                    config.getOrDefault(VectorProperty.TrimPathEnd, vectorNode.trimPathEnd),
                trimPathOffset =
                    config.getOrDefault(VectorProperty.TrimPathOffset, vectorNode.trimPathOffset),
            )
        } else if (vectorNode is VectorGroup) {
            val config = configs[vectorNode.name] ?: object : VectorConfig {}
            Group(
                name = vectorNode.name,
                rotation = config.getOrDefault(VectorProperty.Rotation, vectorNode.rotation),
                scaleX = config.getOrDefault(VectorProperty.ScaleX, vectorNode.scaleX),
                scaleY = config.getOrDefault(VectorProperty.ScaleY, vectorNode.scaleY),
                translationX =
                    config.getOrDefault(VectorProperty.TranslateX, vectorNode.translationX),
                translationY =
                    config.getOrDefault(VectorProperty.TranslateY, vectorNode.translationY),
                pivotX = config.getOrDefault(VectorProperty.PivotX, vectorNode.pivotX),
                pivotY = config.getOrDefault(VectorProperty.PivotY, vectorNode.pivotY),
                clipPathData = config.getOrDefault(VectorProperty.PathData, vectorNode.clipPathData),
            ) {
                RenderVectorGroup(group = vectorNode, configs = configs)
            }
        }
    }
}
