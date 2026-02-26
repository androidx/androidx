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

@file:Suppress("NOTHING_TO_INLINE", "RemoveRedundantQualifierName")
@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import androidx.collection.MutableObjectList
import androidx.collection.mutableObjectListOf
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.internal.identityHashCode
import androidx.compose.foundation.text.modifiers.StylePhase
import androidx.compose.foundation.text.modifiers.TextStyleProviderNode
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalAccessorScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Outline.Rounded
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.shadow.DropShadowPainter
import androidx.compose.ui.graphics.shadow.InnerShadowPainter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.invalidateDrawForSubtree
import androidx.compose.ui.node.invalidateLayer
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.invalidateMeasurementForSubtree
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.node.updateLayerBlock
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Modifier that creates a region that is styled by the given [Style] object for the component this
 * Modifier is attached to.
 *
 * Apply [styleable] to creates a "styleable" component whose looks can be customized using the
 * provided style. This should be called by a component that wishes to make itself styleable via a
 * `style: Style = Style` parameter. If a component already takes a [Style] parameter, then that
 * component internally is applying the [styleable], and that [Style] parameter should be used
 * instead of applying [styleable] again.
 *
 * If [styleable] is added to a modifier chain that is after a another [styleable], then the second
 * region will wrap around the first. For example, if the two regions both supply padding then the
 * padding will the sum of both regions.
 *
 * @param styleState the state the style will use to decide which styles should be applied. If
 *   `null` is supplied, the style will only see the default state that will never be changed.
 * @param style the style to apply to the styleable region.
 * @see MutableStyleState
 * @see Style
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
fun Modifier.styleable(styleState: StyleState? = null, style: Style): Modifier =
    if (style === Style) this else this then StyleElement(styleState, style) then StyleInnerElement

/**
 * Modifier that creates a region that is styled by the given [Style] object for the component this
 * Modifier is attached to. Styles that are further "to the right", will have the properties they
 * set override set properties of Styles to the left of them.
 *
 * Apply [styleable] to creates a "styleable" component whose looks can be customized using the
 * provided `style` together with one or more default [Style] objects. This should be called by a
 * component that wishes to make itself styleable via a `style: Style = Style` parameter. If a
 * component already takes a [Style] parameter, then that component internally is applying the
 * [styleable], and that [Style] parameter should be used instead of applying [styleable] again,
 *
 * If [styleable] is added to a modifier chain that is after a another [styleable], then the second
 * region will wrap around the first. For example, if the two regions both supply padding then the
 * padding will the sum of both regions.
 *
 * @param styleState the state the style will use to decide which styles should be applied. If
 *   `null` is supplied, the style will only see the default state that will never be changed.
 * @param styles the styles to apply, in order, to the stylable region.
 * @see MutableStyleState
 * @see Style
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
fun Modifier.styleable(styleState: StyleState?, vararg styles: Style): Modifier =
    styleable(styleState, Style(*styles))

/**
 * Modifier that creates a region that is styled by the given [Style] object for the component this
 * Modifier is attached to. Styles that are further "to the right", will have the properties they
 * set override set properties of Styles to the left of them.
 *
 * Apply [styleable] to creates a "styleable" component whose looks can be customized using the
 * provided `style` together with one or more default [Style] objects. This should be called by a
 * component that wishes to make itself styleable via a `style: Style = Style` parameter. If a
 * component already takes a [Style] parameter, then that component internally is applying the
 * [styleable], and that [Style] parameter should be used instead of applying [styleable] again,
 *
 * If [styleable] is added to a modifier chain that is after a another [styleable], then the second
 * region will wrap around the first. For example, if the two regions both supply padding then the
 * padding will the sum of both regions.
 *
 * @param styleState the state the style will use to decide which styles should be applied. If
 *   `null` is supplied, the style will only see the default state that will never be changed.
 * @see MutableStyleState
 * @see Style
 * @see StyleScope
 */
@Deprecated(StyleableWithNoStyles, level = DeprecationLevel.ERROR)
@ExperimentalFoundationStyleApi
@Suppress(
    "DeprecatedCallableAddReplaceWith",
    "UNUSED_PARAMETER",
    "UnusedReceiverParameter",
    "ModifierFactoryUnreferencedReceiver",
)
fun Modifier.styleable(styleState: StyleState?): Modifier {
    error(StyleableWithNoStyles)
}

private const val StyleableWithNoStyles =
    "The styleable() modifier must provide one or more 'style' parameter values. Calling it " +
        "with no style parameter values has no effect."

internal data class StyleElement(val styleState: StyleState?, val style: Style) :
    ModifierNodeElement<StyleOuterNode>() {
    override fun create() = StyleOuterNode(styleState, style)

    override fun update(node: StyleOuterNode) {
        node.style = style
        node.state = styleState ?: MutableStyleState(null)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "style"
        properties["style"] = style
        properties["styleState"] = styleState
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is StyleElement && other.style == style && other.styleState == styleState

    override fun hashCode(): Int = style.hashCode()
}

internal const val OuterNodeKey: String = "StyleOuterNode"

internal object StyleInnerElement : ModifierNodeElement<StyleInnerNode>() {
    override fun create() = StyleInnerNode()

    override fun update(node: StyleInnerNode) {
        /* ... do nothing ... */
    }

    override fun InspectorInfo.inspectableProperties() {
        // do nothing. Metadata will be on the outer element
    }

    override fun equals(other: Any?) = this === other

    override fun hashCode(): Int = identityHashCode(this)
}

internal class StyleOuterNode(styleState: StyleState?, style: Style) :
    DelegatingNode(),
    LayoutModifierNode,
    DrawModifierNode,
    TraversableNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode,
    CompositionLocalAccessorScope,
    TextStyleProviderNode {

    // It is very important that we invalidate only the subsystems that we _need_ to, based on what
    // resolved styles have changed. We set this to false, and do all of the invalidations in
    // resolveStyleAndInvalidate
    override val shouldAutoInvalidate: Boolean
        get() = false

    internal var innerNodeField: StyleInnerNode? = null
    internal var innerNode: StyleInnerNode
        get() = innerNodeField ?: error("StyleOuterNode with no corresponding StyleInnerNode")
        set(value) {
            innerNodeField = value
        }

    // This is the combined Style lambda that we are applying to this node. This gets set when the
    // lambda has changed, so in the setter we want to re-resolve and invalidate.
    internal var style: Style = style
        set(value) {
            // NOTE: this value should never get set unless it was already determined that it was
            // not equal with the previous value, so there is no reason to check for that.
            field = value
            resolveStyleAndInvalidate()
        }

    // ResolvedStyle instances are expensive to allocate. We will always need at least one, so we go
    // ahead and allocate that here. This is a pointer to the "currently valid ResolvedStyle" of
    // this node, however we also store a ResolvedStyle in _bufferOrNull that we flip
    // between _resolved and _bufferOrNull every time we re-resolve, and we use the other as a
    // "previous copy" that we can use to compare against and figure out what has changed. Since
    // many style modifiers will be static and never change, we don't allocate a 2nd ResolvedStyle
    // until we need it.
    private var _resolved = ResolvedStyle()
    private var _bufferOrNull: ResolvedStyle? = null
    private val bufferNonNull: ResolvedStyle
        get() {
            if (_bufferOrNull == null) _bufferOrNull = ResolvedStyle()
            return _bufferOrNull!!
        }

    // We start out with null animations because most nodes won't ever be animated. We will lazily
    // create one if it ends up being needed.
    internal var animations: StyleAnimations? = null

    private var _state: StyleState = styleState ?: MutableStyleState(null)
    private var currentInteractionSource: InteractionSource? = null

    internal var state: StyleState
        get() = _state
        set(value) {
            if (_state != value) {
                _state = value
                resolveStyleAndInvalidate()
                innerNode.invalidateLayer()
            }
        }

    /**
     * This method will calculate an up-to-date value for ResolvedStyle, including the interpolated
     * styles from any animations which are ongoing. If there are no animations currently happening,
     * this method will just directly return the already-resolved style that was passed in to the
     * [base] parameter. This defaults to be the one calculated in resolveStyleAndInvalidate.
     *
     * If there are animations happening, this will return a ResolvedStyle which has the
     * interpolated styles of the animation applied to it, and the execution of this function will
     * end up reading an AnimatedValue in the process, which means if this function is used inside
     * of an observation scope, then it will automatically be subscribed to any animations that
     * affect the [flags] passed in.
     *
     * The [flags] passed in are a way to indicate specifically which style properties you care
     * about, and it is possible that the style returned does not have up to date properties for the
     * properties not covered by [flags]. This allows for animations which only affect (for example)
     * Draw to not end up causing something like Layout to be invalidated during the animation.
     */
    internal fun resolveAnimatedStyleFor(
        flags: Int,
        base: ResolvedStyle = _resolved,
    ): ResolvedStyle {
        val animations = animations
        @Suppress("VerboseNullabilityAndEmptiness")
        return if (animations != null && animations.isNotEmpty()) {
            animations.withAnimations(requireDensity(), base, this, flags)
        } else base
    }

    private fun currentLayerStyle() = resolveAnimatedStyleFor(LayerFlag)

    private fun currentLayoutStyle() = resolveAnimatedStyleFor(OuterLayoutFlag)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val resolved = currentLayoutStyle()
        // TODO: do an early exit if OuterLayoutFlags zero?
        val start = resolved.externalPaddingStart.addIfSpecified(resolved.left)
        val end = resolved.externalPaddingEnd.addIfSpecified(resolved.right)
        val top = resolved.externalPaddingTop.addIfSpecified(resolved.top)
        val bottom = resolved.externalPaddingBottom.addIfSpecified(resolved.bottom)

        val horizontal = (start + end).fastRoundToInt()
        val vertical = (top + bottom).fastRoundToInt()

        var minWidth = (constraints.minWidth - horizontal).fastCoerceAtLeast(0)
        var maxWidth = addMaxWithMinimum(constraints.maxWidth, horizontal)
        var minHeight = (constraints.minHeight - vertical).fastCoerceAtLeast(0)
        var maxHeight = addMaxWithMinimum(constraints.maxHeight, vertical)

        minWidth = resolved.minWidth.takeRoundedOrElse(minWidth)
        maxWidth = resolved.maxWidth.takeRoundedOrElse(maxWidth)
        minHeight = resolved.minHeight.takeRoundedOrElse(minHeight)
        maxHeight = resolved.maxHeight.takeRoundedOrElse(maxHeight)

        if (resolved.width.isSpecified) {
            val width = resolved.width.fastRoundToInt()
            minWidth = width
            maxWidth = width
        } else if (resolved.widthFraction.isSpecified && constraints.hasBoundedWidth) {
            val width =
                (maxWidth * resolved.widthFraction)
                    .fastRoundToInt()
                    .fastCoerceIn(minWidth, maxWidth)
            minWidth = width
            maxWidth = width
        } else if (resolved.left.isSpecified && resolved.right.isSpecified) {
            minWidth = maxWidth
        }

        if (resolved.height.isSpecified) {
            val height = resolved.height.fastRoundToInt()
            minHeight = height
            maxHeight = height
        } else if (resolved.heightFraction.isSpecified && constraints.hasBoundedHeight) {
            val height =
                (maxHeight * resolved.heightFraction)
                    .fastRoundToInt()
                    .fastCoerceIn(minHeight, maxHeight)
            minHeight = height
            maxHeight = height
        } else if (resolved.top.isSpecified && resolved.bottom.isSpecified) {
            minHeight = maxHeight
        }

        val placeable = measurable.measure(Constraints(minWidth, maxWidth, minHeight, maxHeight))
        return layout(placeable.width + horizontal, placeable.height + vertical) {
            val resolvedLayoutStyle = currentLayoutStyle()
            val x =
                if (resolvedLayoutStyle.shouldPlaceRelativeToRight()) {
                    constraints.maxWidth - placeable.width - end.fastRoundToInt()
                } else {
                    start.fastRoundToInt()
                }
            val y =
                if (resolvedLayoutStyle.shouldPlaceRelativeToBottom()) {
                    constraints.maxHeight - placeable.height - bottom.fastRoundToInt()
                } else {
                    top.fastRoundToInt()
                }
            // TODO: zIndex
            if (resolvedLayoutStyle.flags and LayerFlag != 0) {
                placeable.placeWithLayer(x, y, layerBlock = layerBlockNonNull)
            } else {
                placeable.place(x, y)
            }
        }
    }

    private fun ResolvedStyle.shouldPlaceRelativeToRight(): Boolean {
        return right.isSpecified && left.isUnspecified
    }

    private fun ResolvedStyle.shouldPlaceRelativeToBottom(): Boolean {
        return bottom.isSpecified && top.isUnspecified
    }

    internal var layerBlock: (GraphicsLayerScope.() -> Unit)? = null
    internal val layerBlockNonNull: GraphicsLayerScope.() -> Unit
        get() =
            layerBlock
                ?: run {
                    val result: GraphicsLayerScope.() -> Unit = { updateLayer() }
                    layerBlock = result
                    result
                }

    private fun GraphicsLayerScope.updateLayer() {
        val resolved = currentLayerStyle()
        alpha = resolved.alpha
        scaleX = resolved.scaleX
        scaleY = resolved.scaleY
        translationX = resolved.translationX
        translationY = resolved.translationY
        rotationX = resolved.rotationX
        rotationY = resolved.rotationY
        rotationZ = resolved.rotationZ
        transformOrigin = resolved.transformOrigin
        clip = resolved.clip
        shape = resolved.shape
    }

    // Outline caching
    private var lastSize: Size = Size.Unspecified
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastShape: Shape? = null
    private var lastOutline: Outline? = null

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

    // Stroke caching
    private var lastStrokeWidth: Float = Float.NaN
    private var lastStroke: Stroke? = null

    private fun getStroke(strokeWidth: Float): Stroke {
        if (lastStrokeWidth != strokeWidth) {
            lastStrokeWidth = strokeWidth
            lastStroke = Stroke(strokeWidth)
        }
        return lastStroke!!
    }

    override fun ContentDrawScope.draw() {
        val resolved = resolveAnimatedStyleFor(DrawFlag)

        val bgColor = resolved.backgroundColor
        val bgBrush = resolved.backgroundBrush
        val foregroundColor = resolved.foregroundColor
        val foregroundBrush = resolved.foregroundBrush
        val borderColor = resolved.borderColor
        val borderBrush = resolved.borderBrush
        val borderWidth = resolved.borderWidth
        val halfStrokeWidth = borderWidth / 2f
        val shape = resolved.shape
        val hasBorder = halfStrokeWidth > 0
        val hasBackground = bgColor.isSpecified || bgBrush != null
        val hasForeground = foregroundColor.isSpecified || foregroundBrush != null
        drawDropShadow(resolved)

        // draw background
        if (shape === RectangleShape) {
            drawForRectShape(
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
        } else {
            drawForShape(
                shape = shape,
                hasBackground = hasBackground,
                hasBorder = hasBorder,
                hasForeground = hasForeground,
                bgColor = bgColor,
                bgBrush = bgBrush,
                borderColor = borderColor,
                borderBrush = resolved.borderBrush,
                foregroundColor = foregroundColor,
                foregroundBrush = resolved.foregroundBrush,
                borderWidth = borderWidth,
            )
        }
        drawInnerShadow(resolved)
        // since we use shape as a cache key in multiple places, we set "lastShape" here at the
        // end of the full draw function body
        lastShape = shape
    }

    // Inner Shadow
    private var lastInnerShadow: Array<Shadow?>? = null
    private var cachedInnerShadowPainters: Array<InnerShadowPainter?>? = null

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

    fun reconcileInnerShadowCache(shadowOrArray: Any, shape: Shape) {
        val lastShadow = lastInnerShadow
        val cachedPainters = cachedInnerShadowPainters

        val size = if (shadowOrArray is Array<*>) shadowOrArray.size else 1

        if (lastShadow == null || lastShape != shape) {
            lastInnerShadow = Array(size) { null }
            cachedInnerShadowPainters = Array(size) { null }
        } else if (lastShadow.size != size) {
            lastInnerShadow = lastShadow.copyOf(size)
            cachedInnerShadowPainters = cachedPainters?.copyOf(size) ?: Array(size) { null }
        }
    }

    fun ContentDrawScope.drawInnerShadow(resolved: ResolvedStyle) {
        val shadowOrArray = resolved.innerShadow
        if (shadowOrArray == null) return
        val shape = resolved.shape

        reconcileInnerShadowCache(shadowOrArray, shape)

        if (shadowOrArray is Array<*>) {
            for (i in shadowOrArray.indices) {
                val shadow = shadowOrArray[i]
                if (shadow is Shadow) {
                    drawInnerShadow(i, shape, shadow)
                }
            }
        } else if (shadowOrArray is Shadow) {
            drawInnerShadow(0, shape, shadowOrArray)
        }
    }

    // Drop Shadow
    private var lastDropShadow: Array<Shadow?>? = null
    private var cachedDropShadowPainters: Array<DropShadowPainter?>? = null

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

    fun reconcileDropShadowCache(shadowOrArray: Any, shape: Shape) {
        val lastShadow = lastDropShadow
        val cachedPainters = cachedDropShadowPainters

        val size = if (shadowOrArray is Array<*>) shadowOrArray.size else 1

        if (lastShadow == null || lastShape != shape) {
            lastDropShadow = Array(size) { null }
            cachedDropShadowPainters = Array(size) { null }
        } else if (lastShadow.size != size) {
            lastDropShadow = lastShadow.copyOf(size)
            cachedDropShadowPainters = cachedPainters?.copyOf(size) ?: Array(size) { null }
        }
    }

    fun ContentDrawScope.drawDropShadow(resolved: ResolvedStyle) {
        val shadowOrArray = resolved.dropShadow ?: return
        val shape = resolved.shape

        reconcileDropShadowCache(shadowOrArray, shape)

        if (shadowOrArray is Array<*>) {
            for (i in shadowOrArray.indices) {
                val shadow = shadowOrArray[i]
                if (shadow is Shadow) {
                    drawDropShadow(i, shape, shadow)
                }
            }
        } else if (shadowOrArray is Shadow) {
            drawDropShadow(0, shape, shadowOrArray)
        }
    }

    fun ContentDrawScope.drawForRectShape(
        hasBackground: Boolean,
        hasBorder: Boolean,
        hasForeground: Boolean,
        bgColor: Color,
        bgBrush: Brush?,
        borderColor: Color,
        borderBrush: Brush?,
        foregroundColor: Color,
        foregroundBrush: Brush?,
        borderWidth: Float,
    ) {
        val needsBorder =
            hasBorder &&
                (hasBackground && bgColor != borderColor || borderBrush != null || !hasBackground)

        val halfStrokeWidth = borderWidth / 2f
        val topLeft = Offset.Zero + if (needsBorder) halfStrokeWidth else 0f
        val outerSize = size - if (needsBorder) borderWidth else 0f
        val innerTopLeft = if (needsBorder) topLeft + (halfStrokeWidth - eps) else topLeft
        val innerSize = if (needsBorder) outerSize - (borderWidth - 2 * eps) else outerSize

        // background
        if (hasBackground && bgBrush == null)
            drawRect(bgColor, topLeft = innerTopLeft, size = innerSize)
        if (hasBackground && bgBrush != null)
            drawRect(bgBrush, topLeft = innerTopLeft, size = innerSize)

        drawContent()

        // Foreground
        if (hasForeground && foregroundBrush == null)
            drawRect(foregroundColor, topLeft = topLeft, size = outerSize)
        if (hasForeground && foregroundBrush != null)
            drawRect(foregroundBrush, topLeft = topLeft, size = outerSize)

        // border
        if (needsBorder) {
            val stroke = getStroke(borderWidth)
            if (borderBrush == null) {
                drawRect(borderColor, topLeft = topLeft, size = outerSize, style = stroke)
            } else {
                drawRect(borderBrush, topLeft = topLeft, size = outerSize, style = stroke)
            }
        }
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
        borderWidth: Float,
    ) {
        val outlineSize = size - borderWidth
        val outline = getOutline(outlineSize, shape)

        when (outline) {
            is Outline.Rectangle ->
                drawForRectShape(
                    hasBackground,
                    hasBorder,
                    hasForeground,
                    bgColor,
                    bgBrush,
                    borderColor,
                    borderBrush,
                    foregroundColor,
                    foregroundBrush,
                    borderWidth,
                )
            is Outline.Rounded ->
                if (outline.roundRect.isSimple)
                    drawForSimpleRoundedShape(
                        outline,
                        outlineSize,
                        hasBackground,
                        hasBorder,
                        hasForeground,
                        bgColor,
                        bgBrush,
                        borderColor,
                        borderBrush,
                        foregroundColor,
                        foregroundBrush,
                        borderWidth,
                    )
                else
                    drawForRoundedShape(
                        outline,
                        hasBackground,
                        hasBorder,
                        hasForeground,
                        bgColor,
                        bgBrush,
                        borderColor,
                        borderBrush,
                        foregroundColor,
                        foregroundBrush,
                        borderWidth,
                    )
            is Outline.Generic ->
                drawForGenericShape(
                    outline,
                    outlineSize,
                    hasBackground,
                    hasBorder,
                    hasForeground,
                    bgColor,
                    bgBrush,
                    borderColor,
                    borderBrush,
                    foregroundColor,
                    foregroundBrush,
                    borderWidth,
                )
        }
    }

    fun ContentDrawScope.drawForSimpleRoundedShape(
        outline: Outline.Rounded,
        outlineSize: Size,
        hasBackground: Boolean,
        hasBorder: Boolean,
        hasForeground: Boolean,
        bgColor: Color,
        bgBrush: Brush?,
        borderColor: Color,
        borderBrush: Brush?,
        foregroundColor: Color,
        foregroundBrush: Brush?,
        borderWidth: Float,
    ) {
        val halfStrokeWidth = borderWidth / 2f
        val outerTopLeft = Offset.Zero + halfStrokeWidth
        val innerTopLeft = if (hasBorder) outerTopLeft + halfStrokeWidth - eps else outerTopLeft
        val innerSize = if (hasBorder) outlineSize - (borderWidth + 2 * eps) else outlineSize
        val cornerRadius = outline.roundRect.topLeftCornerRadius
        val innerRadius = cornerRadius - halfStrokeWidth

        // background
        if (hasBackground) {
            // if we have no border then we can just draw the outline.
            if (hasBorder) {
                // If we have a background and border, we want to make sure that the
                // background "fills" the shape left by the border, and that there are no
                // gaps. The easiest way to do this is to draw the background with the same
                // extents that the border uses
                if (bgBrush != null)
                    drawRoundRect(
                        brush = bgBrush,
                        topLeft = outerTopLeft,
                        size = outlineSize,
                        cornerRadius = cornerRadius,
                        style = Fill,
                    )
                else
                    drawRoundRect(
                        color = bgColor,
                        topLeft = outerTopLeft,
                        size = outlineSize,
                        cornerRadius = cornerRadius,
                        style = Fill,
                    )
            } else {
                drawOutline(outline, bgColor)
            }
        }

        drawContent()

        if (hasForeground && foregroundColor.isSpecified) {
            drawRoundRect(
                color = foregroundColor,
                topLeft = innerTopLeft,
                size = innerSize,
                cornerRadius = innerRadius,
                style = Fill,
            )
        }

        if (hasForeground && foregroundBrush != null) {
            drawRoundRect(
                brush = foregroundBrush,
                topLeft = innerTopLeft,
                size = innerSize,
                cornerRadius = innerRadius,
                style = Fill,
            )
        }

        if (hasBorder) {
            val stroke = getStroke(borderWidth)
            // TODO: there is a clipRect optimization. see Border.kt
            if (borderBrush != null)
                drawRoundRect(
                    brush = borderBrush,
                    topLeft = outerTopLeft,
                    size = outlineSize,
                    cornerRadius = cornerRadius,
                    style = stroke,
                )
            else
                drawRoundRect(
                    color = borderColor,
                    topLeft = outerTopLeft,
                    size = outlineSize,
                    cornerRadius = cornerRadius,
                    style = stroke,
                )
        }
    }

    fun ContentDrawScope.drawForRoundedShape(
        outline: Outline.Rounded,
        hasBackground: Boolean,
        hasBorder: Boolean,
        hasForeground: Boolean,
        bgColor: Color,
        bgBrush: Brush?,
        borderColor: Color,
        borderBrush: Brush?,
        foregroundColor: Color,
        foregroundBrush: Brush?,
        borderWidth: Float,
    ) {
        val halfStrokeWidth = borderWidth / 2f
        val offset = Offset.Zero + halfStrokeWidth
        val borderOutline =
            if (hasBorder)
            // TODO: consider caching
            outline.translate(offset)
            else outline
        val innerOutline =
            if (hasBorder)
            // TODO: consider caching
            outline.inset(halfStrokeWidth)
            else outline

        // background
        if (hasBackground) {
            // if we have no border then we can just draw the outline.
            if (hasBorder) {
                if (bgBrush != null)
                    drawOutline(outline = innerOutline, brush = bgBrush, style = Fill)
                else drawOutline(outline = innerOutline, color = bgColor, style = Fill)
            } else {
                drawOutline(outline, bgColor)
            }
        }

        drawContent()

        if (hasForeground && foregroundColor.isSpecified) {
            drawOutline(outline = innerOutline, color = foregroundColor, style = Fill)
        }
        if (hasForeground && foregroundBrush != null) {
            drawOutline(outline = innerOutline, brush = foregroundBrush, style = Fill)
        }

        // border
        if (hasBorder) {
            val stroke = getStroke(borderWidth)
            if (borderBrush != null)
                drawOutline(brush = borderBrush, outline = borderOutline, style = stroke)
            else drawOutline(color = borderColor, outline = borderOutline, style = stroke)
        }
    }

    fun ContentDrawScope.drawForGenericShape(
        outline: Outline.Generic,
        outlineSize: Size,
        hasBackground: Boolean,
        hasBorder: Boolean,
        hasForeground: Boolean,
        bgColor: Color,
        bgBrush: Brush?,
        borderColor: Color,
        borderBrush: Brush?,
        foregroundColor: Color,
        foregroundBrush: Brush?,
        borderWidth: Float,
    ) {
        // TODO: Implement generic shape support
    }

    override val traverseKey: Any
        get() = OuterNodeKey

    fun resolveStyleAndInvalidate(initial: Boolean = false) {
        val prev = if (initial) null else _resolved
        val next = if (initial) _resolved else bufferNonNull
        val density = requireDensity()

        // reset the style back to default/unset values
        next.clear()

        animations?.preResolve()

        // We declare a separate int var for the animation changes which need to be calculated
        // inside of observeReads. We do this because observeReads is not inline, which means that
        // animChanges will get compiled into a captured Ref.
        var animChanges = 0
        observeReads {
            next.resolve(style, this, density, false)

            _resolved = next
            _bufferOrNull = prev

            // if animations are scheduled, we need to make sure those changes are included
            animChanges = (animations?.postResolve(this, density, !initial) ?: 0)
        }

        // these are the flags of the types of properties that have updated, and will help us
        // selectively invalidate only the phases that NEED to be invalidated. We use diff() with
        // the previous resolved style to figure out which phases need to be invalidated
        val changes = animChanges or (prev?.diff(next) ?: next.flags)

        if (_state.interactionSource != currentInteractionSource) {
            updateInteractionSources()
        }

        // If this is the initial resolve, it means all of these things are going to get invalidated
        // anyway.
        if (initial) return

        if (changes and InnerLayoutFlag != 0) {
            innerNode.invalidateMeasurement()
        }
        if (changes and OuterLayoutFlag != 0) {
            invalidateMeasurement()
        }
        if (changes and DrawFlag != 0) {
            // TODO: invalidateDraw() doesn't seem to correct invalidate the drawing of THIS node,
            //  but it probably should. I think this is a bug. By calling invalidateLayer of the
            //  inner node, we sidestep the bug, but should probably investigate separately.
            innerNode.invalidateLayer()
        }
        if (changes and LayerFlag != 0) {
            updateLayerBlock(layerBlockNonNull)
        }
        if (changes and TextLayoutFlag != 0) {
            invalidateTextLayout()
        }
        if (changes and TextDrawFlag != 0) {
            invalidateTextDraw()
        }
    }

    override fun onObservedReadsChanged() {
        // if this method is getting called, it means that a state that was read inside of the
        // resolving of the Style was invalidated. We need to re-run the resolve so that we get the
        // most up to date ResolvedStyle, and invalidate whichever systems need to be invalidated.
        resolveStyleAndInvalidate()
    }

    var sourceJob: Job? = null

    fun updateInteractionSources() {
        sourceJob?.cancel()
        val source = _state.interactionSource
        currentInteractionSource = source
        if (source != null) {
            sourceJob = coroutineScope.launch { _state.processInteractions(source) }
        }
    }

    override val <T> CompositionLocal<T>.currentValue: T
        get() = this@StyleOuterNode.currentValueOf(this)

    /**
     * Text-related style properties are "inherited", which means that the ancestral chain of Style
     * modifiers that are above this one that contribute to Text styles should have those properties
     * merged together to form the base style used in Text.
     *
     * As a performance optimization, we cache the ResolvedStyle which is effectively the
     * "inherited" base style of everything above this node and including this node. This way, when
     * one of these inherited styles change, we only need to go up one level in order to recalculate
     * the new base style.
     *
     * It is possible that there are no Text nodes underneath this Style node, at which point this
     * cached ResolvedStyle would never be needed. Because of that, we only do this "on demand", and
     * we also don't allocate an expensive ResolvedStyle unless needed.
     *
     * Likewise, when we do allocate a ResolvedStyle, we don't release it whenever things get
     * invalidated. Instead, we just mark the node as invalidated, and whenever we recalculate the
     * style, we use the same ResolvedStyle that we had cached before.
     */
    override fun computeInheritedTextStyle(phase: StylePhase, fallback: TextStyle): TextStyle =
        resolveInheritedStyle(phase.toFlags())?.toTextStyle(fallback) ?: fallback

    internal var ancestorNodes: MutableObjectList<StyleOuterNode>? = null

    internal fun resolveInheritedStyle(flags: Int): ResolvedStyle? {
        var nodes = ancestorNodes

        if (_resolved.flags and InheritedFlags != 0 || animations?.isNotEmpty() == true) {
            (nodes
                    ?: mutableObjectListOf<StyleOuterNode>().also {
                        nodes = it
                        ancestorNodes = it
                    })
                .add(this)
        }

        // Collect all the ancestors that are animating or have inherited properties.
        // If we have any animations we need to compute the animations as a final pass.
        traverseAncestors(OuterNodeKey) { node ->
            if (node !is StyleOuterNode) return@traverseAncestors true
            if (
                node._resolved.flags and InheritedFlags != 0 ||
                    node.animations?.isNotEmpty() == true
            ) {
                (nodes
                        ?: mutableObjectListOf<StyleOuterNode>().also {
                            nodes = it
                            ancestorNodes = it
                        })
                    .add(node)
            }
            true
        }

        var startStyle: ResolvedStyle? = getCachedInheritedStyle()
        var startIndex = if (startStyle != null) -1 else -2
        var hasAnimations = animations?.isNotEmpty() ?: false
        var hasInheritedStyles = _resolved.flags and (TextLayoutFlag or TextDrawFlag)

        nodes?.forEachIndexed { index, node ->
            val cached = node.getCachedInheritedStyle()
            hasAnimations = hasAnimations || node.animations?.isNotEmpty() ?: false
            hasInheritedStyles =
                hasInheritedStyles or (node._resolved.flags and (TextLayoutFlag or TextDrawFlag))
            if (cached == null) {
                // can't use any cached styles we've found up until this point
                startStyle = null
                startIndex = -2
            } else if (startStyle == null) {
                startStyle = cached
                startIndex = index
            }
        }

        if (hasInheritedStyles == 0) return null

        // Fast out if this node's cached style has been resolved and no styles have any
        // pending animations.
        if (startStyle != null && startIndex < 0 && !hasAnimations) {
            return startStyle
        }

        // Update the caches for the non-animating values for inherited styles
        var ancestorStyle = startStyle
        startIndex = if (nodes != null && startIndex < -1) nodes.size - 1 else startIndex
        for (index in startIndex downTo -1) {
            val node = if (index < 0) this else (nodes ?: continue)[index]
            val cachedStyle = node.cachedInheritedStyle ?: ResolvedStyle()
            ancestorStyle?.copyInheritedStylesInto(cachedStyle)
            cachedStyle.applyInheritableStyles(node._resolved)
            ancestorStyle = cachedStyle
            node.saveInheritedStyles(cachedStyle)
        }

        if (hasAnimations) {
            val animatingStyle = ResolvedStyle()
            ancestorStyle?.copyInheritedStylesInto(animatingStyle)
            val size = nodes?.size ?: 0
            val density = requireDensity()
            for (index in size - 1 downTo -1) {
                val node = if (index < 0) this else (nodes ?: continue)[index]
                node.animations?.applyAnimationsTo(animatingStyle, density, node, flags)
            }
            ancestorStyle = animatingStyle
        }

        return ancestorStyle
    }

    private var cachedInheritedStyle: ResolvedStyle? = null
    // We track the invalid cache with a boolean to avoid having to reallocate it when the
    // cache is updated.
    private var inheritedStyleDirty = false

    internal fun getCachedInheritedStyle(): ResolvedStyle? =
        if (inheritedStyleDirty) null else cachedInheritedStyle

    /**
     * This takes the ResolvedStyle passed in and copies the inherited styles of it into the cached
     * style on this node. This means that the cached style will be a "snapshot" of the passed in
     * style, and the passed in style can continue to mutate elsewhere and it won't effect the
     * cached style on this node.
     */
    internal fun saveInheritedStyles(style: ResolvedStyle) {
        inheritedStyleDirty = false
        cachedInheritedStyle = style
    }

    private fun invalidateTextLayout() {
        inheritedStyleDirty = true
        // TODO: it might be better to use visitSubtree() and single out text nodes specifically,
        //  having Text modifier implement TraversableNode directly.
        invalidateMeasurementForSubtree()
    }

    private fun invalidateTextDraw() {
        inheritedStyleDirty = true
        invalidateDrawForSubtree()
    }
}

/**
 * The style modifier currently requires two modifier nodes in order to make all of the different
 * styles work properly. More specifically, two LayoutModifierNodes are required. The "outer"
 * modifier implements _almost_ everything, except for padding. In order for padding, drawing, etc.
 * to work properly, we need this inner modifier to add the "padding". If padding isn't used, then
 * this modifier is not really necessary, but it is difficult to toggle it on/off in this regard.
 *
 * We should think more about ways in which we could make it so this modifier isn't necessary while
 * still allowing for style to define padding.
 */
internal class StyleInnerNode() : Modifier.Node(), LayoutModifierNode {
    // This is only ever invalidated manually from StyleOuterModifier::resolveStyleAndInvalidate
    override val shouldAutoInvalidate: Boolean
        get() = false

    // A reference to the outer node that this modifier is a part of.
    var outerNode: StyleOuterNode? = null

    private fun currentLayoutStyle() = outerNode!!.resolveAnimatedStyleFor(InnerLayoutFlag)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val resolved = currentLayoutStyle()
        val start = resolved.contentPaddingStart + resolved.borderWidth
        val end = resolved.contentPaddingEnd + resolved.borderWidth
        val top = resolved.contentPaddingTop + resolved.borderWidth
        val bottom = resolved.contentPaddingBottom + resolved.borderWidth

        val horizontal = (start + end).fastRoundToInt()
        val vertical = (top + bottom).fastRoundToInt()

        val placeable = measurable.measure(constraints.offset(-horizontal, -vertical))

        return layout(
            constraints.constrainWidth(placeable.width + horizontal),
            constraints.constrainHeight(placeable.height + vertical),
        ) {
            placeable.place(start.fastRoundToInt(), top.fastRoundToInt())
        }
    }

    override fun onAttach() {
        // We attach top-down, so by the time we get to the inner node's "onAttach", the outer node
        // is already attached. finding ancestors is cheap and quick and we know that the Outer
        // node is guaranteed to be there.
        val outer = findNearestAncestor(OuterNodeKey) as StyleOuterNode
        val inner = this
        // Set up the pointers on both nodes so that they can communicate with one another
        outer.innerNode = inner
        inner.outerNode = outer
        // This is where we first resolve the Style applied to the outer node.
        outer.resolveStyleAndInvalidate(initial = true)
    }
}

private const val eps = 0.5f

private inline val Float.isSpecified: Boolean
    get() = !isNaN()
private inline val Float.isUnspecified: Boolean
    get() = isNaN()

private inline fun Float.addIfSpecified(abs: Float): Float = if (abs.isNaN()) this else (this + abs)

private inline fun Float.takeRoundedOrElse(fallback: Int): Int =
    if (isNaN()) fallback else fastRoundToInt()

private inline operator fun Size.plus(other: Float): Size = Size(width + other, height + other)

private inline operator fun Size.minus(other: Float): Size = this + -other

private inline operator fun Offset.plus(other: Float): Offset = Offset(x + other, y + other)

private inline operator fun Offset.minus(other: Float): Offset = Offset(x - other, y - other)

private inline fun addMaxWithMinimum(max: Int, value: Int): Int {
    return if (max == Constraints.Infinity) {
        max
    } else {
        (max + value).fastCoerceAtLeast(0)
    }
}

private fun RoundRect.insetBy(inset: Float): RoundRect {
    return RoundRect(
        left = left + inset + inset - 0.5f,
        top = top + inset + inset - 0.5f,
        right = right + 1f,
        bottom = bottom + 1f,
        topLeftCornerRadius = topLeftCornerRadius - inset,
        topRightCornerRadius = topRightCornerRadius - inset,
        bottomRightCornerRadius = bottomRightCornerRadius - inset,
        bottomLeftCornerRadius = bottomLeftCornerRadius - inset,
    )
}

private fun RoundRect.offsetBy(offset: Offset): RoundRect {
    return RoundRect(
        left = left + offset.x,
        top = top + offset.y,
        right = right + offset.x,
        bottom = bottom + offset.y,
        topLeftCornerRadius = topLeftCornerRadius,
        topRightCornerRadius = topRightCornerRadius,
        bottomRightCornerRadius = bottomRightCornerRadius,
        bottomLeftCornerRadius = bottomLeftCornerRadius,
    )
}

private fun Rounded.inset(inset: Float): Rounded {
    return Rounded(roundRect.insetBy(inset))
}

private fun Rounded.translate(offset: Offset): Rounded {
    return Rounded(roundRect.offsetBy(offset))
}

private operator fun CornerRadius.minus(value: Float): CornerRadius =
    CornerRadius(max(0f, x - value), max(0f, y - value))

private fun StylePhase.toFlags(): Int =
    when (this) {
        StylePhase.Layout -> TextLayoutFlag
        StylePhase.Draw -> TextDrawFlag
        else -> InheritedFlags
    }
