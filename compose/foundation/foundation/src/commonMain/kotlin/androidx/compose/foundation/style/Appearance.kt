/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.style

import androidx.compose.foundation.border.BorderLogic
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.shadow.DropShadowPainter
import androidx.compose.ui.graphics.shadow.InnerShadowPainter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * A modifier that allows drawing a background, border, foreground, and shadows with a single
 * modifier. The values to use are in a callback that is invoked in the drawing phase and, if it
 * reads mutable state, will only invalidate drawing.
 *
 * If any mutable state, animations or style properties read in [block] change [block] to be called
 * again and update the appearance.
 *
 * NOTE: [block] can be invoked multiple times, which is why it's important for performance to
 * minimize work done inside of it. [block] may also be invoked before effects.
 *
 * @param block the block used to determine what this modifier draws.
 * @see AppearanceScope
 */
@ExperimentalFoundationStyleApi
public fun Modifier.appearance(block: AppearanceScope.() -> Unit): Modifier =
    this then AppearanceElement(block)

/** The scope of the [block][appearance] in the [appearance] modifier. */
@ExperimentalFoundationStyleApi
public class AppearanceScope {
    /**
     * Fills the background of the component. If a [shape] is set, the background will fill that
     * shape. The default shape is [RectangleShape].
     *
     * The default value is [Fill.None] which will not fill the background.
     *
     * @see Fill
     */
    public var background: Fill = Fill.None

    /**
     * Fills the foreground color of the component. This can be used to overlay a color on top of
     * the component's content. It is important that this brush be partially transparent (e.g. alpha
     * less than 1.0) or it will obscure the content. If a [shape] is set, the foreground will fill
     * that shape.
     *
     * The default value is [Fill.None] which will not fill the foreground.
     *
     * @see Fill
     */
    public var foreground: Fill = Fill.None

    /**
     * Sets the width of the border around the component. The border is drawn on top of the
     * background and the padded content. The border's width does not contribute to the component's
     * layout size (width/height); it is rendered within the component's bounds This method only
     * sets the width; color or brush must be set separately.
     *
     * Specifying a [Dp.Unspecified] value will remove the border.
     *
     * Specifying a [Dp.Hairline] or 0.dp value will create 1 pixel border regardless of density.
     *
     * The default value is [Dp.Unspecified].
     */
    public var borderWidth: Dp = Dp.Unspecified

    /**
     * Sets how the border is filled around the component. The border is drawn on top of the
     * background. This method only sets the fill; width must be by [borderWidth]. Both must be set
     * for a border to be drawn. The border's presence and appearance do not affect the component's
     * layout size.
     *
     * The Default value is [Fill.None] which doesn't draw a border.
     *
     * @see Fill
     * @see borderWidth
     */
    public var border: Fill = Fill.None

    /**
     * Applies a drop shadow effect directly to the component, often used for text or specific
     * graphics. This is distinct from `shadowElevation` which is specific to platform elevation
     * shadows. Te border and overall layout size are not affected by this shadow.
     *
     * Multiple shadows can be applied, using [Shadows.Compound], and will be drawn together.
     *
     * If [shape] is set, the shadow will be applied to the shape's bounds.
     *
     * The default value is [Shadows.None] which doesn't draw a shadow.
     *
     * @see Shadows
     * @see innerShadow
     * @see shape
     */
    public var dropShadow: Shadows = Shadows.None

    /**
     * Applies an inner shadow effect to the component. This shadow is drawn inside the bounds of
     * the component.The border and overall layout size are not affected by this shadow.
     *
     * Multiple shadows can be applied, using [Shadows.Compound], and will be drawn together.
     *
     * If [shape] is set, the shadow will be applied to the shape's bounds.
     *
     * The default value is [Shadows.None] which doesn't draw a shadow.
     *
     * @see Shadows
     */
    public var innerShadow: Shadows = Shadows.None

    /**
     * Defines the [Shape] to use for shape background rendering ([background]), foreground
     * rendering ([foreground]) and border rendering ([border]).
     *
     * If [shape] is not specified then a [RectangleShape] is used.
     *
     * @see Shape
     * @see background
     * @see border
     * @see foreground
     */
    public var shape: Shape = RectangleShape
}

@ExperimentalFoundationStyleApi
internal class AppearanceElement(val block: AppearanceScope.() -> Unit) :
    ModifierNodeElement<AppearanceNode>() {
    override fun create() = AppearanceNode(block)

    override fun update(node: AppearanceNode) {
        node.block = block
    }

    override fun hashCode(): Int = block.hashCode()

    override fun equals(other: Any?): Boolean = other is AppearanceNode && other.block === block

    override fun InspectorInfo.inspectableProperties() {
        name = "appearance"
        properties["block"] = block
    }
}

@ExperimentalFoundationStyleApi
internal class AppearanceNode(var block: AppearanceScope.() -> Unit) :
    Modifier.Node(), ObserverModifierNode, DrawModifierNode, SemanticsModifierNode {
    // Outline caching
    private var lastSize: Size = Size.Unspecified
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastShape: Shape? = null
    private var lastOutline: Outline? = null

    // Border
    private var borderLayer: GraphicsLayer? = null
    private var borderLayerProvider: (() -> GraphicsLayer)? = null
    private val borderLogic = BorderLogic()

    // Drop Shadow
    private var lastDropShadow: Array<Shadow?>? = null
    private var cachedDropShadowPainters: Array<DropShadowPainter?>? = null

    // Inner Shadow
    private var lastInnerShadow: Array<Shadow?>? = null
    private var cachedInnerShadowPainters: Array<InnerShadowPainter?>? = null

    override fun onObservedReadsChanged() {
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val scope = AppearanceScope()
        observeReads { scope.block() }
        var bgColor: Color = Color.Unspecified
        var bgBrush: Brush? = null

        when (val background = scope.background) {
            is Fill.Color -> bgColor = background.color
            is Fill.Brush -> bgBrush = background.brush
            else -> {}
        }

        var foregroundColor: Color = Color.Unspecified
        var foregroundBrush: Brush? = null
        when (val foreground = scope.foreground) {
            is Fill.Color -> foregroundColor = foreground.color
            is Fill.Brush -> foregroundBrush = foreground.brush
            else -> {}
        }
        var borderColor: Color = Color.Black
        var borderBrush: Brush? = null
        when (val borderFill = scope.border) {
            is Fill.Color -> borderColor = borderFill.color
            is Fill.Brush -> borderBrush = borderFill.brush
            else -> {}
        }

        val borderWidth = scope.borderWidth
        val halfStrokeWidth = borderWidth / 2f
        val shape = scope.shape
        val hasBorder = halfStrokeWidth.roundToPx() > 0
        val hasBackground = bgColor.isSpecified || bgBrush != null
        val hasForeground = foregroundColor.isSpecified || foregroundBrush != null

        if (scope.dropShadow != Shadows.None) {
            drawDropShadow(scope.dropShadow, shape)
        }
        drawForShape(
            shape = shape,
            hasBackground = hasBackground,
            hasBorder = hasBorder,
            hasForeground = hasForeground,
            bgColor = bgColor,
            bgBrush = bgBrush,
            borderColor = borderColor,
            borderBrush = borderBrush,
            foregroundColor = foregroundColor,
            foregroundBrush = foregroundBrush,
            borderWidth = borderWidth,
        )
        if (scope.innerShadow != Shadows.None) {
            drawInnerShadow(scope.innerShadow, shape)
        }

        // since we use shape as a cache key in multiple places, we set "lastShape" here at the
        // end of the full draw function body
        lastShape = shape
    }

    fun ContentDrawScope.drawForShape(
        shape: Shape,
        hasBackground: Boolean,
        hasBorder: Boolean,
        hasForeground: Boolean,
        bgColor: Color,
        bgBrush: Brush?,
        borderColor: Color,
        borderBrush: Brush?,
        foregroundColor: Color,
        foregroundBrush: Brush?,
        borderWidth: Dp,
    ) {
        val outline = getOutline(size, shape)

        // background
        if (hasBackground) {
            if (bgBrush != null) {
                drawOutline(outline, brush = bgBrush)
            } else {
                drawOutline(outline, color = bgColor)
            }
        }

        drawContent()

        // foreground
        if (hasForeground) {
            if (foregroundBrush != null) {
                drawOutline(outline, brush = foregroundBrush)
            } else {
                drawOutline(outline, color = foregroundColor)
            }
        }

        // border
        if (hasBorder) {
            val brush = borderBrush ?: SolidColor(borderColor)
            borderLogic.drawBorder(
                drawScope = this,
                width = borderWidth,
                brush = brush,
                borderLayerProvider
                    ?: {
                            borderLayer
                                ?: requireGraphicsContext().createGraphicsLayer().also {
                                    borderLayer = it
                                }
                        }
                        .also { borderLayerProvider = it },
                outline = outline,
            )
        }
    }

    private fun ContentDrawScope.drawDropShadow(shadows: Shadows, shape: Shape) {
        if (shadows is Shadows.None) return

        reconcileDropShadowCache(shadows, shape)

        when (shadows) {
            is Shadows.Compound -> {
                val shadowsArray = shadows.shadows
                for (i in shadowsArray.indices) {
                    val shadow = shadowsArray[i]
                    drawDropShadow(i, shape, shadow)
                }
            }
            is Shadows.Simple -> {
                drawDropShadow(0, shape, shadows.shadow)
            }
            is Shadows.None -> {}
        }
    }

    private fun ContentDrawScope.drawDropShadow(index: Int, shape: Shape, shadow: Shadow) {
        val lastShadow = lastDropShadow?.getOrNull(index)
        val lastPainter = cachedDropShadowPainters?.getOrNull(index)
        val painter =
            if (lastShadow == shadow && lastPainter != null) lastPainter
            else requireGraphicsContext().shadowContext.createDropShadowPainter(shape, shadow)

        lastDropShadow?.let { it[index] = shadow }
        cachedDropShadowPainters?.let { it[index] = painter }

        with(painter) { draw(size) }
    }

    fun reconcileDropShadowCache(shadows: Shadows, shape: Shape) {
        val lastShadow = lastDropShadow
        val cachedPainters = cachedDropShadowPainters

        val size = if (shadows is Shadows.Compound) shadows.shadows.size else 1

        if (lastShadow == null || lastShape != shape) {
            lastDropShadow = Array(size) { null }
            cachedDropShadowPainters = Array(size) { null }
        } else if (lastShadow.size != size) {
            lastDropShadow = lastShadow.copyOf(size)
            cachedDropShadowPainters = cachedPainters?.copyOf(size) ?: Array(size) { null }
        }
    }

    private fun ContentDrawScope.drawInnerShadow(index: Int, shape: Shape, shadow: Shadow) {
        val lastShadow = lastInnerShadow?.getOrNull(index)
        val lastPainter = cachedInnerShadowPainters?.getOrNull(index)
        val painter =
            if (lastShadow == shadow && lastPainter != null) lastPainter
            else requireGraphicsContext().shadowContext.createInnerShadowPainter(shape, shadow)

        lastInnerShadow?.let { it[index] = shadow }
        cachedInnerShadowPainters?.let { it[index] = painter }

        with(painter) { draw(size) }
    }

    fun reconcileInnerShadowCache(shadows: Shadows, shape: Shape) {
        val lastShadow = lastInnerShadow
        val cachedPainters = cachedInnerShadowPainters

        val size = if (shadows is Shadows.Compound) shadows.shadows.size else 1

        if (lastShadow == null || lastShape != shape) {
            lastInnerShadow = Array(size) { null }
            cachedInnerShadowPainters = Array(size) { null }
        } else if (lastShadow.size != size) {
            lastInnerShadow = lastShadow.copyOf(size)
            cachedInnerShadowPainters = cachedPainters?.copyOf(size) ?: Array(size) { null }
        }
    }

    fun ContentDrawScope.drawInnerShadow(shadows: Shadows, shape: Shape) {
        if (shadows is Shadows.None) return

        reconcileInnerShadowCache(shadows, shape)

        when (shadows) {
            is Shadows.Simple -> {
                drawInnerShadow(0, shape, shadows.shadow)
            }
            is Shadows.Compound -> {
                val shadowArray = shadows.shadows
                for (i in shadowArray.indices) {
                    val shadow = shadowArray[i]
                    drawInnerShadow(i, shape, shadow)
                }
            }
            is Shadows.None -> {}
        }
    }

    private fun ContentDrawScope.getOutline(size: Size, shape: Shape): Outline {
        val outline =
            if (lastSize == size && lastLayoutDirection == layoutDirection && lastShape == shape) {
                lastOutline!!
            } else {
                shape.createOutline(size, layoutDirection, this)
            }
        lastOutline = outline
        lastSize = size
        lastLayoutDirection = layoutDirection
        return outline
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        lastShape?.let { shape = it }
    }
}

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * This modifier is to help enable transition between the old [Style] based API and the new
 * [CustomStyle] based API. This duplicates the behavior of the properties from [BorderScope],
 * [BackgroundScope], [ForegroundScope], [ShadowScope] of [Style].
 *
 * This will be moved to be an example of how to use [appearance] to implement the style properties
 * similar to those provided in [Style].
 */
@ExperimentalFoundationStyleApi
public fun Modifier.styleAppearance(styleResolver: StyleResolver): Modifier =
    this then
        AppearanceElement {
            styleResolver.resolve {
                ifSet(shapeProperty) { shape = it }
                ifSet(backgroundProperty) { background = it }
                ifSet(foregroundProperty) { foreground = it }
                ifSet(borderFillProperty) { border = it }
                ifSet(borderWidthProperty) { borderWidth = it }
                ifSet(dropShadowProperty) { dropShadow = it }
                ifSet(innerShadowProperty) { innerShadow = it }
            }
        }
