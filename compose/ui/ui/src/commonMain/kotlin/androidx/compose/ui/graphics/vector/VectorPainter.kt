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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.packFloats

/** Default identifier for the root group if a Vector graphic */
const val RootGroupName = "VectorRootGroup"

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
            "androidx.compose.ui.graphics.vector"
        )
)
@Composable
@ComposableOpenTarget(-1)
fun rememberVectorPainter(
    defaultWidth: Dp,
    defaultHeight: Dp,
    viewportWidth: Float = Float.NaN,
    viewportHeight: Float = Float.NaN,
    name: String = RootGroupName,
    tintColor: Color = Color.Unspecified,
    tintBlendMode: BlendMode = BlendMode.SrcIn,
    content: @Composable @VectorComposable (viewportWidth: Float, viewportHeight: Float) -> Unit
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
        content
    )

/**
 * Create a [VectorPainter] with the Vector defined by the provided sub-composition.
 *
 * Inside [content] use the [Group] and [Path] composables to define the vector.
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
fun rememberVectorPainter(
    defaultWidth: Dp,
    defaultHeight: Dp,
    viewportWidth: Float = Float.NaN,
    viewportHeight: Float = Float.NaN,
    name: String = RootGroupName,
    tintColor: Color = Color.Unspecified,
    tintBlendMode: BlendMode = BlendMode.SrcIn,
    autoMirror: Boolean = false,
    content: @Composable @VectorComposable (viewportWidth: Float, viewportHeight: Float) -> Unit
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
                autoMirror = autoMirror
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
 * vector hierarchy given the tree structure in [ImageVector]
 *
 * @param [image] ImageVector used to create a vector graphic sub-composition
 */
@Composable
fun rememberVectorPainter(image: ImageVector): VectorPainter {
    val density = LocalDensity.current
    val key = packFloats(image.genId.toFloat(), density.density)
    return remember(key) {
        createVectorPainterFromImageVector(
            density,
            image,
            GroupComponent().apply { createGroupComponent(image.root) }
        )
    }
}

/**
 * [Painter] implementation that abstracts the drawing of a Vector graphic. This can be represented
 * by either a [ImageVector] or a programmatic composition of a vector
 */
class VectorPainter internal constructor(root: GroupComponent = GroupComponent()) : Painter() {

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
        VectorComponent(root).apply {
            invalidateCallback = {
                if (drawCount == invalidateCount) {
                    invalidateCount++
                }
            }
        }

    internal val bitmapConfig: ImageBitmapConfig
        get() = vector.cacheBitmapConfig

    internal var composition: Composition? = null

    // TODO replace with mutableStateOf(Unit, neverEqualPolicy()) after b/291647821 is addressed
    private var invalidateCount by mutableIntStateOf(0)

    private var currentAlpha: Float = 1.0f
    private var currentColorFilter: ColorFilter? = null

    override val intrinsicSize: Size
        get() = size

    private var drawCount = -1

    override fun DrawScope.onDraw() {
        with(vector) {
            val filter = currentColorFilter ?: intrinsicColorFilter
            if (autoMirror && layoutDirection == LayoutDirection.Rtl) {
                mirror { draw(currentAlpha, filter) }
            } else {
                draw(currentAlpha, filter)
            }
        }
        // This assignment is necessary to obtain invalidation callbacks as the state is
        // being read here which adds this callback to the snapshot observation
        drawCount = invalidateCount
    }

    override fun applyAlpha(alpha: Float): Boolean {
        currentAlpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
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
sealed class VectorProperty<T> {
    object Rotation : VectorProperty<Float>()

    object PivotX : VectorProperty<Float>()

    object PivotY : VectorProperty<Float>()

    object ScaleX : VectorProperty<Float>()

    object ScaleY : VectorProperty<Float>()

    object TranslateX : VectorProperty<Float>()

    object TranslateY : VectorProperty<Float>()

    object PathData : VectorProperty<List<PathNode>>()

    object Fill : VectorProperty<Brush?>()

    object FillAlpha : VectorProperty<Float>()

    object Stroke : VectorProperty<Brush?>()

    object StrokeLineWidth : VectorProperty<Float>()

    object StrokeAlpha : VectorProperty<Float>()

    object TrimPathStart : VectorProperty<Float>()

    object TrimPathEnd : VectorProperty<Float>()

    object TrimPathOffset : VectorProperty<Float>()
}

/**
 * Holds a set of values that overwrite the original property values of an [ImageVector]. This
 * allows you to dynamically change any of the property values provided as [VectorProperty]. This
 * can be passed to [RenderVectorGroup] to alter some property values when the [VectorGroup] is
 * rendered.
 */
@JvmDefaultWithCompatibility
interface VectorConfig {
    fun <T> getOrDefault(property: VectorProperty<T>, defaultValue: T): T {
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
        if (viewportHeight.isNaN()) defaultSize.height else viewportHeight
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
    root: GroupComponent
): VectorPainter {
    val defaultSize = density.obtainSizePx(imageVector.defaultWidth, imageVector.defaultHeight)
    val viewport =
        obtainViewportSize(defaultSize, imageVector.viewportWidth, imageVector.viewportHeight)
    return VectorPainter(root)
        .configureVectorPainter(
            defaultSize = defaultSize,
            viewportSize = viewport,
            name = imageVector.name,
            intrinsicColorFilter =
                createColorFilter(imageVector.tintColor, imageVector.tintBlendMode),
            autoMirror = imageVector.autoMirror
        )
}

/**
 * statically create a a GroupComponent from the VectorGroup representation provided from an
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
fun RenderVectorGroup(group: VectorGroup, configs: Map<String, VectorConfig> = emptyMap()) {
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
                    config.getOrDefault(VectorProperty.TrimPathOffset, vectorNode.trimPathOffset)
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
                clipPathData = config.getOrDefault(VectorProperty.PathData, vectorNode.clipPathData)
            ) {
                RenderVectorGroup(group = vectorNode, configs = configs)
            }
        }
    }
}
