/*
 * Copyright 2025 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")
@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import androidx.collection.MutableIntList
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.CompositionLocal
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Interpolatable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.shadow.lerp
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.isSpecified
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.jvm.JvmField
import kotlin.math.ceil

/**
 * This class is currently the actual object that [Style] lambdas get executed with, so this is the
 * primary implementation of [StyleScope] that exists. This is currently implemented as a genuine
 * class with properties/fields for all of the possible values that can be set via [StyleScope].
 *
 * Since [StyleScope] is a large API, that currently means there are ~50 individual fields on this
 * object, which means it is a fairly expensive object to allocate, and takes up a decent amount of
 * memory. We might consider alternative implementations that would hold values in an array or
 * something which might allow for the memory impact of a ResolvedStyle to be less, but the runtime
 * impact might be worse. This is all implementation detail however, and could be experimented with
 * over time.
 */
internal class ResolvedStyle internal constructor() : StyleScope, InspectableValue {
    // TODO: there's currently ~50 individual properties on this class. We could use a single bit
    //  for every single property if flags became a Long, which might allow for some functions to
    //  be written more efficiently?

    /**
     * This field is effectively a bitset indicating which "categories" of properties have been set
     * on this object. There are a lot of different properties, and we can make certain optimal
     * decisions if we know that a certain category of properties have never been changed from their
     * default value. For instance, we have some properties which affect "draw", but not "layout",
     * so we can avoid invalidating layout in those cases.
     *
     * For the flag values, see constants defined like [DrawFlag].
     */
    private var compositeHash = 0
    private var currentIndex = 0
    private var indexStack: MutableIntList? = null
    @JvmField internal var flags: Int = 0
    private var _density: Float = 1f
    private var _fontScale: Float = 1f
    private var animating: Boolean = false
    private var node: StyleOuterNode? = null

    internal var contentPaddingStart: Float = 0f
    internal var contentPaddingEnd: Float = 0f
    internal var contentPaddingTop: Float = 0f
    internal var contentPaddingBottom: Float = 0f
    internal var externalPaddingStart: Float = 0f
    internal var externalPaddingEnd: Float = 0f
    internal var externalPaddingTop: Float = 0f
    internal var externalPaddingBottom: Float = 0f
    internal var borderWidth: Float = 0f
    internal var width: Float = Float.NaN
    internal var height: Float = Float.NaN
    internal var widthFraction: Float = Float.NaN
    internal var heightFraction: Float = Float.NaN
    internal var left: Float = Float.NaN
    internal var top: Float = Float.NaN
    internal var right: Float = Float.NaN
    internal var bottom: Float = Float.NaN
    internal var minHeight: Float = Float.NaN
    internal var maxHeight: Float = Float.NaN
    internal var minWidth: Float = Float.NaN
    internal var maxWidth: Float = Float.NaN

    // draw properties
    internal var borderColor: Color = Color.Black
    internal var borderBrush: Brush? = null
    internal var backgroundColor: Color = Color.Transparent // white?
    internal var backgroundBrush: Brush? = null
    internal var foregroundColor: Color = Color.Unspecified
    internal var foregroundBrush: Brush? = null
    internal var clip: Boolean = false
    internal var shape: Shape = RectangleShape

    // layer properties
    internal var alpha: Float = 1.0f
    internal var scaleX: Float = 1.0f
    internal var scaleY: Float = 1.0f
    internal var translationX: Float = 0f
    internal var translationY: Float = 0f
    internal var rotationX: Float = 0f
    internal var rotationY: Float = 0f
    internal var rotationZ: Float = 0f
    internal var transformOrigin: TransformOrigin = TransformOrigin.Center
    internal var cameraDistance: Float = 1.0f
    internal var zIndex: Float = 0f

    // text style, affects draw only
    internal var contentColor: Color = Color.Unspecified
    internal var contentBrush: Brush? = null

    // text style, affects text layout
    internal var fontFamily: FontFamily? = null
    internal var textIndent: TextIndent? = null
    internal var fontSize: TextUnit = TextUnit.Unspecified
    internal var lineHeight: TextUnit = TextUnit.Unspecified
    internal var letterSpacing: TextUnit = TextUnit.Unspecified
    internal var baselineShift: BaselineShift = BaselineShift.Unspecified
    internal var lineBreak: LineBreak = LineBreak.Unspecified
    // This is several text related "enum" style values which we are packing
    // into a single Int for efficiency.
    internal var textEnums: Int = 0

    /**
     * Given another ResolvedStyle instance, this function will return a bitmask of the property
     * "categories" which have differences between the two. This bitmask matches the bitmask of
     * [flags].
     *
     * The passed in [filterFlags] parameter is a bitmask of the same format which can be used to
     * limit which categories of properties this function will actually check for differences of.
     */
    internal fun diff(other: ResolvedStyle, filterFlags: Int = 0.inv()): Int {
        // if only one has one category of things set, then by definition it is part of the diff
        var change = flags xor other.flags
        // if both have a certain category set, we need to check each property individually
        val checkFor = flags and other.flags and filterFlags

        if (checkFor and InnerLayoutFlag != 0) {
            if (
                contentPaddingStart != other.contentPaddingStart ||
                    contentPaddingEnd != other.contentPaddingEnd ||
                    contentPaddingTop != other.contentPaddingTop ||
                    contentPaddingBottom != other.contentPaddingBottom ||
                    borderWidth != other.borderWidth
            ) {
                change = change or InnerLayoutFlag
            }
        }
        if (checkFor and OuterLayoutFlag != 0) {
            if (
                width != other.width ||
                    height != other.height ||
                    widthFraction != other.widthFraction ||
                    heightFraction != other.heightFraction ||
                    externalPaddingStart != other.externalPaddingStart ||
                    externalPaddingEnd != other.externalPaddingEnd ||
                    externalPaddingTop != other.externalPaddingTop ||
                    externalPaddingBottom != other.externalPaddingBottom ||
                    left.toRawBits() != other.left.toRawBits() ||
                    top.toRawBits() != other.top.toRawBits() ||
                    right.toRawBits() != other.right.toRawBits() ||
                    bottom.toRawBits() != other.bottom.toRawBits() ||
                    minWidth.toRawBits() != other.minWidth.toRawBits() ||
                    maxWidth.toRawBits() != other.maxWidth.toRawBits() ||
                    minHeight.toRawBits() != other.minHeight.toRawBits() ||
                    maxHeight.toRawBits() != other.maxHeight.toRawBits()
            ) {
                change = change or OuterLayoutFlag
            }
        }
        if (checkFor and DrawFlag != 0) {
            if (
                borderWidth != other.borderWidth ||
                    borderColor != other.borderColor ||
                    borderBrush != other.borderBrush ||
                    backgroundColor != other.backgroundColor ||
                    backgroundBrush != other.backgroundBrush ||
                    foregroundBrush != other.foregroundBrush ||
                    innerShadow != other.innerShadow ||
                    dropShadow != other.dropShadow ||
                    shape != other.shape
            ) {
                change = change or DrawFlag
            }
        }
        if (checkFor and LayerFlag != 0) {
            if (
                alpha != other.alpha ||
                    scaleX != other.scaleX ||
                    scaleY != other.scaleY ||
                    translationX != other.translationX ||
                    translationY != other.translationY ||
                    rotationX != other.rotationX ||
                    rotationY != other.rotationY ||
                    rotationZ != other.rotationZ ||
                    transformOrigin != other.transformOrigin ||
                    //            cameraDistance != other.cameraDistance ||
                    //            zIndex != other.zIndex ||
                    clip != other.clip
            ) {
                change = change or LayerFlag
            }
        }
        if (shape != other.shape) {
            change = change or LayerFlag or DrawFlag
        }
        if (checkFor and TextDrawFlag != 0) {
            if (contentColor != other.contentColor || contentBrush != other.contentBrush
            // TODO: we could include TextDecoration here but it's part of textEnums so we would
            //  have to break it out.
            ) {
                change = change or TextDrawFlag
            }
        }
        if (checkFor and TextLayoutFlag != 0) {
            if (
                fontFamily != other.fontFamily ||
                    textIndent != other.textIndent ||
                    fontSize != other.fontSize ||
                    lineHeight != other.lineHeight ||
                    letterSpacing != other.letterSpacing ||
                    baselineShift != other.baselineShift ||
                    lineBreak != other.lineBreak ||
                    textEnums != other.textEnums
            ) {
                change = change or TextLayoutFlag or TextDrawFlag
            }
        }
        return change
    }

    /** Creates a shallow copy of this ResolvedStyle instance. */
    internal fun copy(): ResolvedStyle = ResolvedStyle().also { copyInto(it) }

    internal fun copyInheritedStyles(): ResolvedStyle =
        ResolvedStyle().also { copyInheritedStylesInto(it) }

    /**
     * Copies all style properties which are "inherited" from this [ResolvedStyle] into the [target]
     * [ResolvedStyle]. These properties are effectively all of the text related properties.
     */
    internal fun copyInheritedStylesInto(target: ResolvedStyle) {
        target.contentColor = contentColor
        target.contentBrush = contentBrush
        target.fontFamily = fontFamily
        target.textIndent = textIndent
        target.fontSize = fontSize
        target.lineHeight = lineHeight
        target.letterSpacing = letterSpacing
        target.baselineShift = baselineShift
        target.lineBreak = lineBreak
        target.textEnums = textEnums
    }

    /** Copies all style properties from this [ResolvedStyle] into the [target] [ResolvedStyle]. */
    internal fun copyInto(target: ResolvedStyle) {
        target.flags = flags
        target.left = left
        target.top = top
        target.right = right
        target.bottom = bottom
        target.minHeight = minHeight
        target.maxHeight = maxHeight
        target.minWidth = minWidth
        target.maxWidth = maxWidth
        target.contentPaddingStart = contentPaddingStart
        target.contentPaddingEnd = contentPaddingEnd
        target.contentPaddingTop = contentPaddingTop
        target.contentPaddingBottom = contentPaddingBottom
        target.externalPaddingStart = externalPaddingStart
        target.externalPaddingEnd = externalPaddingEnd
        target.externalPaddingTop = externalPaddingTop
        target.externalPaddingBottom = externalPaddingBottom
        target.borderWidth = borderWidth
        target.shape = shape
        target.alpha = alpha
        target.scaleX = scaleX
        target.scaleY = scaleY
        target.translationX = translationX
        target.translationY = translationY
        target.rotationX = rotationX
        target.rotationY = rotationY
        target.rotationZ = rotationZ
        target.transformOrigin = transformOrigin
        target.zIndex = zIndex
        target.cameraDistance = cameraDistance
        target.borderColor = borderColor
        target.borderBrush = borderBrush
        target.backgroundColor = backgroundColor
        target.backgroundBrush = backgroundBrush
        target.foregroundBrush = foregroundBrush
        target.dropShadow = dropShadow
        target.innerShadow = innerShadow
        target.clip = clip
        target.width = width
        target.height = height
        target.widthFraction = widthFraction
        target.heightFraction = heightFraction
        copyInheritedStylesInto(target)
    }

    /** Resets all properties of this instance to their default values. */
    internal fun clear() {
        EmptyResolvedStyle.copyInto(this)
    }

    internal fun resolve(style: Style, node: StyleOuterNode, density: Density, animating: Boolean) {
        startResolve(node, density, animating)
        with(style) { applyStyle() }
        doneResolve()
    }

    internal fun resolveForTesting(
        style: Style,
        density: Density,
        animating: Boolean,
        state: StyleState? = null,
    ) {
        currentIndex = 0
        compositeHash = 0
        this.node = StyleOuterNode(state, style)
        this._density = density.density
        this.animating = animating
        with(style) { applyStyle() }
        doneResolve()
    }

    /**
     * Applies all inheritable styles from [source] into this [ResolvedStyle].
     *
     * Inheritable styles are effectively all of the text-related style properties.
     */
    internal fun applyInheritableStyles(source: ResolvedStyle) {
        val sourceTextFlags = source.flags and (TextDrawFlag or TextLayoutFlag)
        if (sourceTextFlags == 0) return
        flags = flags or sourceTextFlags
        contentColor = source.contentColor.takeOrElse(contentColor)
        contentBrush = source.contentBrush ?: contentBrush
        fontFamily = source.fontFamily ?: fontFamily
        textIndent = source.textIndent ?: textIndent
        fontSize = source.fontSize.takeOrElse(fontSize)
        lineHeight = source.lineHeight.takeOrElse(lineHeight)
        letterSpacing = source.letterSpacing.takeOrElse(letterSpacing)
        baselineShift = source.baselineShift.takeOrElse(baselineShift)
        lineBreak = source.lineBreak.takeOrElse(lineBreak)
        textEnums = applyTextEnum(textEnums, source.textEnums)
    }

    /**
     * Converts this [ResolvedStyle] to a [TextStyle], using the given [fallback] [TextStyle] to
     * provide any values which are not set.
     */
    internal fun toTextStyle(fallback: TextStyle): TextStyle {
        val default = EmptyResolvedStyle
        return TextStyle(
                color = contentColor.takeOrElse { fallback.color },
                fontSize = fontSize.takeOrElse(fallback.fontSize),
                fontWeight = if (isFontWeightSpecified) fontWeight else fallback.fontWeight,
                fontStyle = if (fontStyle != default.fontStyle) fontStyle else fallback.fontStyle,
                fontSynthesis =
                    if (fontSynthesis != default.fontSynthesis) fontSynthesis
                    else fallback.fontSynthesis,
                fontFamily = fontFamily ?: fallback.fontFamily,
                fontFeatureSettings = fallback.fontFeatureSettings,
                letterSpacing = letterSpacing.takeOrElse(fallback.letterSpacing),
                baselineShift =
                    if (baselineShift.isSpecified) baselineShift else fallback.baselineShift,
                textGeometricTransform = fallback.textGeometricTransform,
                localeList = fallback.localeList,
                background = fallback.background,
                textDecoration =
                    if (textDecoration != default.textDecoration) textDecoration
                    else fallback.textDecoration,
                shadow = fallback.shadow,
                drawStyle = fallback.drawStyle,
                textAlign = if (textAlign != default.textAlign) textAlign else fallback.textAlign,
                textDirection =
                    if (textDirection != default.textDirection) textDirection
                    else fallback.textDirection,
                lineHeight = lineHeight.takeOrElse(fallback.lineHeight),
                textIndent = textIndent ?: fallback.textIndent,
                platformStyle = fallback.platformStyle,
                lineHeightStyle = fallback.lineHeightStyle,
                lineBreak = lineBreak.takeOrElse(fallback.lineBreak),
                hyphens = if (hyphens != default.hyphens) hyphens else fallback.hyphens,
                textMotion = fallback.textMotion,
            )
            .let { if (contentBrush != null) it.copy(brush = contentBrush) else it }
    }

    private fun valueElements(): List<ValueElement> =
        mutableListOf<ValueElement>().apply {
            val default = EmptyResolvedStyle
            fun add(name: String, value: Any?) = add(ValueElement(name, value))
            if (default.contentPaddingStart != contentPaddingStart)
                add("contentPaddingStart", contentPaddingStart)
            if (default.contentPaddingEnd != contentPaddingEnd)
                add("contentPaddingEnd", contentPaddingEnd)
            if (default.contentPaddingTop != contentPaddingTop)
                add("contentPaddingTop", contentPaddingTop)
            if (default.contentPaddingBottom != contentPaddingBottom)
                add("contentPaddingBottom", contentPaddingBottom)
            if (default.externalPaddingStart != externalPaddingStart)
                add("externalPaddingStart", externalPaddingStart)
            if (default.externalPaddingEnd != externalPaddingEnd)
                add("externalPaddingEnd", externalPaddingEnd)
            if (default.externalPaddingTop != externalPaddingTop)
                add("externalPaddingTop", externalPaddingTop)
            if (default.externalPaddingBottom != externalPaddingBottom)
                add("externalPaddingBottom", externalPaddingBottom)
            if (default.borderWidth != borderWidth) add("borderWidth", borderWidth)
            if (default.width != width) add("width", width)
            if (default.height != height) add("height", height)
            if (default.widthFraction.toRawBits() != widthFraction.toRawBits())
                add("widthFraction", widthFraction)
            if (default.heightFraction.toRawBits() != heightFraction.toRawBits())
                add("heightFraction", heightFraction)
            if (default.alpha != alpha) add("alpha", alpha)
            if (default.scaleX != scaleX) add("scaleX", scaleX)
            if (default.scaleY != scaleY) add("scaleY", scaleY)
            if (default.translationX != translationX) add("translationX", translationX)
            if (default.translationY != translationY) add("translationY", translationY)
            if (default.rotationX != rotationX) add("rotationX", rotationX)
            if (default.rotationY != rotationY) add("rotationY", rotationY)
            if (default.rotationZ != rotationZ) add("rotationZ", rotationZ)
            if (default.transformOrigin != transformOrigin) add("transformOrigin", transformOrigin)
            if (default.zIndex != zIndex) add("zIndex", zIndex)
            if (default.cameraDistance != cameraDistance) add("cameraDistance", cameraDistance)
            if (default.borderColor != borderColor) add("borderColor", borderColor)
            if (default.borderBrush != borderBrush) add("borderBrush", borderBrush)
            if (default.backgroundColor != backgroundColor) add("backgroundColor", backgroundColor)
            if (default.backgroundBrush != backgroundBrush) add("backgroundBrush", backgroundBrush)
            if (default.foregroundBrush != foregroundBrush) add("foregroundBrush", foregroundBrush)
            if (default.clip != clip) add("clip", clip)
            if (default.shape != shape) add("shape", shape)
            if (default.contentColor.isSpecified) add("contentColor", contentColor)
            if (default.contentBrush != backgroundBrush) add("contentBrush", contentBrush)
            if (default.fontFamily != fontFamily) add("fontFamily", fontFamily)
            if (default.textIndent != textIndent) add("textIndent", textIndent)
            if (default.fontSize != fontSize) add("fontSize", fontSize)
            if (default.lineHeight != lineHeight) add("lineHeight", lineHeight)
            if (default.letterSpacing != letterSpacing) add("letterSpacing", letterSpacing)
            if (default.baselineShift != baselineShift) add("baselineShift", baselineShift)
            if (default.lineBreak != lineBreak) add("lineBreak", lineBreak)
            if (default.textAlign != textAlign) add("textAlign", textAlign)
            if (default.textDirection != textDirection) add("textDirection", textDirection)
            if (default.hyphens != hyphens) add("hyphens", hyphens)
            if (default.fontStyle != fontStyle) add("fontStyle", fontStyle)
            if (default.fontWeight != fontWeight) add("fontWeight", fontWeight)
            if (default.fontSynthesis != fontSynthesis) add("fontSynthesis", fontSynthesis)
            if (default.textDecoration != textDecoration) add("textDecoration", textDecoration)
        }

    override val inspectableElements: Sequence<ValueElement>
        @Suppress("ListIterator") // Only allocates in the inspector API
        get() = valueElements().asSequence()

    override val density: Float
        get() = _density

    override val fontScale: Float
        get() = _fontScale

    override val state: StyleState
        get() = node!!.state

    // contentPadding
    override fun contentPaddingStart(value: Dp) {
        flags = flags or InnerLayoutFlag
        contentPaddingStart = value.roundToPx().toFloat()
    }

    override fun contentPaddingEnd(value: Dp) {
        flags = flags or InnerLayoutFlag
        contentPaddingEnd = value.roundToPx().toFloat()
    }

    override fun contentPaddingTop(value: Dp) {
        flags = flags or InnerLayoutFlag
        contentPaddingTop = value.roundToPx().toFloat()
    }

    override fun contentPaddingBottom(value: Dp) {
        flags = flags or InnerLayoutFlag
        contentPaddingBottom = value.roundToPx().toFloat()
    }

    override fun contentPaddingHorizontal(value: Dp) {
        flags = flags or InnerLayoutFlag
        val value = value.roundToPx().toFloat()
        contentPaddingStart = value
        contentPaddingEnd = value
    }

    override fun contentPaddingVertical(value: Dp) {
        flags = flags or InnerLayoutFlag
        val value = value.roundToPx().toFloat()
        contentPaddingTop = value
        contentPaddingBottom = value
    }

    override fun contentPadding(value: Dp) {
        flags = flags or InnerLayoutFlag
        val value = value.roundToPx().toFloat()
        contentPaddingStart = value
        contentPaddingEnd = value
        contentPaddingTop = value
        contentPaddingBottom = value
    }

    override fun contentPadding(start: Dp, top: Dp, end: Dp, bottom: Dp) {
        flags = flags or InnerLayoutFlag
        contentPaddingTop = top.roundToPx().toFloat()
        contentPaddingEnd = end.roundToPx().toFloat()
        contentPaddingBottom = bottom.roundToPx().toFloat()
        contentPaddingStart = start.roundToPx().toFloat()
    }

    override fun contentPadding(horizontal: Dp, vertical: Dp) {
        flags = flags or InnerLayoutFlag
        val vertical = vertical.roundToPx().toFloat()
        contentPaddingTop = vertical
        contentPaddingBottom = vertical
        val horizontal = horizontal.roundToPx().toFloat()
        contentPaddingEnd = horizontal
        contentPaddingStart = horizontal
    }

    // externalPadding
    override fun externalPaddingStart(value: Dp) {
        flags = flags or OuterLayoutFlag
        externalPaddingStart = value.roundToPx().toFloat()
    }

    override fun externalPaddingEnd(value: Dp) {
        flags = flags or OuterLayoutFlag
        externalPaddingEnd = value.roundToPx().toFloat()
    }

    override fun externalPaddingTop(value: Dp) {
        flags = flags or OuterLayoutFlag
        externalPaddingTop = value.roundToPx().toFloat()
    }

    override fun externalPaddingBottom(value: Dp) {
        flags = flags or OuterLayoutFlag
        externalPaddingBottom = value.roundToPx().toFloat()
    }

    override fun externalPaddingHorizontal(value: Dp) {
        flags = flags or OuterLayoutFlag
        val value = value.roundToPx().toFloat()
        externalPaddingStart = value
        externalPaddingEnd = value
    }

    override fun externalPaddingVertical(value: Dp) {
        flags = flags or OuterLayoutFlag
        val value = value.roundToPx().toFloat()
        externalPaddingTop = value
        externalPaddingBottom = value
    }

    override fun externalPadding(value: Dp) {
        flags = flags or OuterLayoutFlag
        val value = value.roundToPx().toFloat()
        externalPaddingStart = value
        externalPaddingEnd = value
        externalPaddingTop = value
        externalPaddingBottom = value
    }

    override fun externalPadding(start: Dp, top: Dp, end: Dp, bottom: Dp) {
        flags = flags or OuterLayoutFlag
        externalPaddingTop = top.roundToPx().toFloat()
        externalPaddingEnd = end.roundToPx().toFloat()
        externalPaddingBottom = bottom.roundToPx().toFloat()
        externalPaddingStart = start.roundToPx().toFloat()
    }

    override fun externalPadding(horizontal: Dp, vertical: Dp) {
        flags = flags or OuterLayoutFlag
        val density = _density
        val vertical = vertical.roundToPx().toFloat()
        externalPaddingTop = vertical
        externalPaddingBottom = vertical
        val horizontal = horizontal.roundToPx().toFloat()
        externalPaddingEnd = horizontal
        externalPaddingStart = horizontal
    }

    // border
    override fun borderWidth(value: Dp) {
        flags = flags or DrawFlag or InnerLayoutFlag
        val width =
            when (value) {
                Dp.Unspecified -> 0.0f
                Dp.Hairline -> 1.0f
                else -> ceil(value.value * _density)
            }
        borderWidth = width
    }

    override fun borderColor(value: Color) {
        flags = flags or DrawFlag
        borderColor = value
        borderBrush = null
    }

    override fun borderBrush(value: Brush) {
        flags = flags or DrawFlag
        borderBrush = value
        borderColor = Color.Unspecified
    }

    override fun border(width: Dp, color: Color) {
        borderWidth(width)
        borderColor(color)
    }

    override fun border(width: Dp, brush: Brush) {
        borderWidth(width)
        borderBrush(brush)
    }

    // size
    override fun width(value: Dp) {
        flags = flags or OuterLayoutFlag
        width = value.value * _density
        widthFraction = Float.NaN
    }

    override fun height(value: Dp) {
        flags = flags or OuterLayoutFlag
        height = value.value * _density
        heightFraction = Float.NaN
    }

    override fun size(width: Dp, height: Dp) {
        flags = flags or OuterLayoutFlag
        this.width = width.value * _density
        this.widthFraction = Float.NaN
        this.height = height.value * _density
        this.heightFraction = Float.NaN
    }

    override fun size(value: Dp) {
        flags = flags or OuterLayoutFlag
        val size = value.value * _density
        this.width = size
        this.widthFraction = Float.NaN
        this.height = size
        this.heightFraction = Float.NaN
    }

    override fun size(value: DpSize) {
        flags = flags or OuterLayoutFlag
        this.width = value.width.value * _density
        this.widthFraction = Float.NaN
        this.height = value.height.value * _density
        this.heightFraction = Float.NaN
    }

    override fun width(fraction: Float) {
        flags = flags or OuterLayoutFlag
        widthFraction = fraction
        width = Float.NaN
    }

    override fun height(fraction: Float) {
        flags = flags or OuterLayoutFlag
        heightFraction = fraction
        height = Float.NaN
    }

    override fun left(value: Dp) {
        flags = flags or OuterLayoutFlag
        left = value.value * _density
    }

    override fun top(value: Dp) {
        flags = flags or OuterLayoutFlag
        top = value.value * _density
    }

    override fun right(value: Dp) {
        flags = flags or OuterLayoutFlag
        right = value.value * _density
    }

    override fun bottom(value: Dp) {
        flags = flags or OuterLayoutFlag
        bottom = value.value * _density
    }

    override fun minWidth(value: Dp) {
        flags = flags or OuterLayoutFlag
        minWidth = value.value * _density
    }

    override fun minHeight(value: Dp) {
        flags = flags or OuterLayoutFlag
        minHeight = value.value * _density
    }

    override fun minSize(size: DpSize) {
        minWidth(size.width)
        minHeight(size.height)
    }

    override fun minSize(width: Dp, height: Dp) {
        minWidth(width)
        minHeight(height)
    }

    override fun maxWidth(value: Dp) {
        flags = flags or OuterLayoutFlag
        maxWidth = value.value * _density
    }

    override fun maxHeight(value: Dp) {
        flags = flags or OuterLayoutFlag
        maxHeight = value.value * _density
    }

    override fun maxSize(size: DpSize) {
        maxWidth(size.width)
        maxHeight(size.height)
    }

    override fun maxSize(width: Dp, height: Dp) {
        maxWidth(width)
        maxHeight(height)
    }

    // layer properties
    override fun alpha(value: Float) {
        flags = flags or LayerFlag
        alpha = value
    }

    override fun scaleX(value: Float) {
        flags = flags or LayerFlag
        scaleX = value
    }

    override fun scaleY(value: Float) {
        flags = flags or LayerFlag
        scaleY = value
    }

    override fun scale(value: Float) {
        flags = flags or LayerFlag
        scaleX = value
        scaleY = value
    }

    // TODO: dp-based translation
    override fun translationX(value: Float) {
        flags = flags or LayerFlag
        translationX = value
    }

    override fun translationY(value: Float) {
        flags = flags or LayerFlag
        translationY = value
    }

    override fun translation(x: Float, y: Float) {
        flags = flags or LayerFlag
        translationX = x
        translationY = y
    }

    override fun translation(offset: Offset) {
        flags = flags or LayerFlag
        translationX = offset.x
        translationY = offset.y
    }

    override fun rotationX(value: Float) {
        flags = flags or LayerFlag
        rotationX = value
    }

    override fun rotationY(value: Float) {
        flags = flags or LayerFlag
        rotationY = value
    }

    override fun rotationZ(value: Float) {
        flags = flags or LayerFlag
        rotationZ = value
    }

    override fun transformOrigin(value: TransformOrigin) {
        flags = flags or LayerFlag
        transformOrigin = value
    }

    override fun clip(value: Boolean) {
        flags = flags or LayerFlag
        clip = value
    }

    override fun zIndex(value: Float) {
        zIndex = value
    }

    //    var cameraDistance: Float = Float.NaN
    //    var renderEffect: RenderEffect? = null
    // TODO: need a TransformOrigin.Unspecified
    //    var transformOrigin: TransformOrigin = Float.NaN

    // draw properties
    override fun background(color: Color) {
        flags = flags or DrawFlag
        backgroundColor = color
        backgroundBrush = null
    }

    override fun background(value: Brush) {
        flags = flags or DrawFlag
        backgroundBrush = value
        backgroundColor = Color.Unspecified
    }

    override fun foreground(value: Color) {
        flags = flags or DrawFlag
        foregroundColor = value
        foregroundBrush = null
    }

    override fun foreground(value: Brush) {
        flags = flags or DrawFlag
        foregroundBrush = value
        foregroundColor = Color.Unspecified
    }

    // TODO: consider borderRadius?
    override fun shape(value: Shape) {
        // TODO: setting this will cause the layer flag to get set and for a layer to be created,
        //  but we actually don't really care about the shape in the layer unless clip is true.
        //  This means we will be creating more layers than we need to be as it is currently
        //  written. Consider the right way to tweak this so that isn't the case. Perhaps it needs
        //  its own flag?
        flags = flags or DrawFlag or LayerFlag
        shape = value
    }

    // animation
    override fun animate(value: Style) = animate(DefaultSpringSpec, DefaultSpringSpec, value)

    override fun animate(spec: AnimationSpec<Float>, value: Style) = animate(spec, spec, value)

    override fun animate(
        toSpec: AnimationSpec<Float>,
        fromSpec: AnimationSpec<Float>,
        value: Style,
    ) {
        flags = flags or AnimatedFlag
        group(AnimateGroup) {
            if (animating) {
                apply(value)
            } else {
                val node = node!!
                val animations =
                    node.animations ?: StyleAnimations(node).also { node.animations = it }
                animations.record(currentCompositeHash, value, toSpec, fromSpec)
            }
        }
    }

    override val <T> CompositionLocal<T>.currentValue: T
        get() = node!!.currentValueOf(this)

    internal var dropShadow: Any? = null
    internal var innerShadow: Any? = null

    override fun dropShadow(value: Shadow) {
        dropShadow = value
        flags = flags or DrawFlag
    }

    override fun dropShadow(vararg value: Shadow) {
        dropShadow = value
        flags = flags or DrawFlag
    }

    override fun innerShadow(value: Shadow) {
        innerShadow = value
        flags = flags or DrawFlag
    }

    override fun innerShadow(vararg value: Shadow) {
        innerShadow = value
        flags = flags or DrawFlag
    }

    override fun textStyle(value: TextStyle) {
        // TODO: optimize further
        val span = value.toSpanStyle()
        if (span.color.isSpecified) contentColor(span.color)
        if (span.fontSize.isSpecified) fontSize(span.fontSize)
        if (span.letterSpacing.isSpecified) letterSpacing(span.letterSpacing)
        span.brush?.let { contentBrush(it) }
        span.fontStyle?.let { fontStyle(it) }
        span.baselineShift?.let { if (it.isSpecified) baselineShift(it) }
        span.fontWeight?.let { fontWeight(it) }
        span.textDecoration?.let { textDecoration(it) }
        span.fontSynthesis?.let { fontSynthesis(it) }

        val p = value.toParagraphStyle()
        p.textIndent?.let { textIndent(it) }
        if (p.lineHeight.isSpecified) lineHeight(p.lineHeight)
        if (p.lineBreak.isSpecified) lineBreak(p.lineBreak)
        if (p.hyphens.isSpecified) hyphens(p.hyphens)
        if (p.textDirection.isSpecified) textDirection(p.textDirection)
        if (p.textAlign.isSpecified) textAlign(p.textAlign)
    }

    internal val fontStyle: FontStyle
        get() =
            when (textEnums.getBits(FontStyleMask, FontStyleShift) and 0b1) {
                1 -> FontStyle.Italic
                else -> FontStyle.Normal
            }

    internal val textAlign: TextAlign
        get() = TextAlign.valueOf(textEnums.getBits(TextAlignMask, TextAlignShift))

    internal val textDirection: TextDirection
        get() = TextDirection.valueOf(textEnums.getBits(TextDirectionMask, TextDirectionShift))

    internal val hyphens: Hyphens
        get() = Hyphens.valueOf(textEnums.getBits(HyphensMask, HyphensShift))

    internal val fontWeight: FontWeight
        get() = FontWeight(textEnums.getBits(FontWeightMask, FontWeightShift))

    internal val isFontWeightSpecified: Boolean
        get() = textEnums.getBits(FontWeightMask, FontWeightShift) != 0

    internal val fontSynthesis: FontSynthesis
        get() =
            FontSynthesis.valueOf(
                textEnums.getBits(FontSynthesisMask, FontSynthesisShift) and 0b111
            )

    internal val textDecoration: TextDecoration
        get() =
            TextDecoration.valueOf(
                textEnums.getBits(TextDecorationMask, TextDecorationShift) and 0b11
            )

    override fun contentColor(value: Color) {
        flags = flags or TextDrawFlag
        contentColor = value
        contentBrush = null
    }

    override fun contentBrush(value: Brush) {
        flags = flags or TextDrawFlag
        contentBrush = value
        contentColor = Color.Unspecified
    }

    override fun textDecoration(value: TextDecoration) {
        flags = flags or TextDrawFlag
        val bits = 0b100 or value.mask
        textEnums = textEnums or (bits shl TextDecorationShift)
    }

    override fun fontFamily(value: FontFamily) {
        // TODO: should we deal with async differently?
        flags = flags or TextLayoutFlag
        fontFamily = value
    }

    override fun textIndent(value: TextIndent) {
        flags = flags or TextLayoutFlag
        textIndent = value
    }

    override fun fontSize(value: TextUnit) {
        flags = flags or TextLayoutFlag
        fontSize = value
    }

    override fun lineHeight(value: TextUnit) {
        flags = flags or TextLayoutFlag
        lineHeight = value
    }

    override fun letterSpacing(value: TextUnit) {
        flags = flags or TextLayoutFlag
        letterSpacing = value
    }

    override fun baselineShift(value: BaselineShift) {
        flags = flags or TextLayoutFlag
        baselineShift = value
    }

    override fun lineBreak(value: LineBreak) {
        flags = flags or TextLayoutFlag
        lineBreak = value
    }

    override fun fontStyle(value: FontStyle) {
        // TODO: do we need FontStyle.Unspecified?
        flags = flags or TextLayoutFlag
        textEnums = textEnums.setBits(FontStyleMask, FontStyleShift, 0b10 or value.value)
    }

    override fun textAlign(value: TextAlign) {
        flags = flags or TextLayoutFlag
        textEnums = textEnums.setBits(TextAlignMask, TextAlignShift, value.value)
    }

    override fun textDirection(value: TextDirection) {
        flags = flags or TextLayoutFlag
        textEnums = textEnums.setBits(TextDirectionMask, TextDirectionShift, value.value)
    }

    override fun hyphens(value: Hyphens) {
        flags = flags or TextLayoutFlag
        textEnums = textEnums.setBits(HyphensMask, HyphensShift, value.value)
    }

    override fun fontWeight(value: FontWeight) {
        flags = flags or TextLayoutFlag
        // this is between [0,1000], so fits into 10 bits. 0 is "unspecified"
        textEnums = textEnums.setBits(FontWeightMask, FontWeightShift, value.weight)
    }

    // TODO: should this just be part of font family definition?
    override fun fontSynthesis(value: FontSynthesis) {
        flags = flags or TextLayoutFlag
        // the and 0b111 at the end here is pretty important. It turns out that FontSynthesis.All
        // is 16 bits even though there are only 2 enums currently. We should probably update the
        // constant itself.
        val bits = 0b1000 or (value.value and 0b111)
        textEnums = textEnums or (bits shl FontSynthesisShift)
    }

    override fun <T> state(
        key: StyleStateKey<T>,
        value: Style,
        active: (key: StyleStateKey<T>, state: StyleState) -> Boolean,
    ) {
        group(key.hashCode(), active(key, state), value)
    }

    internal fun startResolve(node: StyleOuterNode, density: Density, animating: Boolean) {
        currentIndex = 0
        compositeHash = 0
        this.node = node
        this._density = density.density
        this.animating = animating
    }

    internal fun doneResolve() {
        node = null
        animating = false
        // don't reset density as it can be useful for toString() and debugging, and presents no
        // memory leak issue
    }

    private fun pushIndex(index: Int): MutableIntList =
        (indexStack
                ?: run {
                    val newStack = MutableIntList()
                    indexStack = newStack
                    newStack
                })
            .apply { push(index) }

    private inline fun group(key: Int, block: () -> Unit) {
        val index = currentIndex
        val effectiveKey = key xor index
        compositeHash = updateHashEnter(compositeHash, effectiveKey)
        val stack = pushIndex(index)
        currentIndex = 0
        block()
        currentIndex = stack.pop() + 1
        compositeHash = updateHashExit(compositeHash, effectiveKey)
    }

    private fun group(key: Int, active: Boolean, style: Style) {
        if (active) {
            group(key) { with(style) { applyStyle() } }
        } else {
            skippedGroup()
        }
    }

    private inline fun skippedGroup() {
        currentIndex++
    }

    private inline val currentCompositeHash
        get() = compositeHash xor currentIndex
}

internal fun lerpOuterLayout(a: ResolvedStyle, b: ResolvedStyle, t: Float, result: ResolvedStyle) {
    // TODO: optimize this function more
    with(result) {
        externalPaddingStart = lerpMaybeNan(a.externalPaddingStart, b.externalPaddingStart, t)
        externalPaddingEnd = lerpMaybeNan(a.externalPaddingEnd, b.externalPaddingEnd, t)
        externalPaddingTop = lerpMaybeNan(a.externalPaddingTop, b.externalPaddingTop, t)
        externalPaddingBottom = lerpMaybeNan(a.externalPaddingBottom, b.externalPaddingBottom, t)
        left = lerpMaybeNan(a.left, b.left, t)
        top = lerpMaybeNan(a.top, b.top, t)
        right = lerpMaybeNan(a.right, b.right, t)
        bottom = lerpMaybeNan(a.bottom, b.bottom, t)
        width = lerpMaybeNan(a.width, b.width, t)
        height = lerpMaybeNan(a.height, b.height, t)
        widthFraction = lerpMaybeNan(a.widthFraction, b.widthFraction, t)
        heightFraction = lerpMaybeNan(a.heightFraction, b.heightFraction, t)
        minWidth = lerpMaybeNan(a.minWidth, b.minWidth, t)
        maxWidth = lerpMaybeNan(a.maxWidth, b.maxWidth, t)
        minHeight = lerpMaybeNan(a.minHeight, b.minHeight, t)
        maxHeight = lerpMaybeNan(a.maxHeight, b.maxHeight, t)
    }
}

internal inline fun lerpMaybeNan(a: Float, b: Float, t: Float): Float {
    val aNan = a.isNaN()
    val bNan = b.isNaN()
    val next = (1 - t) * a + t * b
    return if (aNan) b else if (bNan) a else next
}

internal fun lerpInnerLayout(a: ResolvedStyle, b: ResolvedStyle, t: Float, result: ResolvedStyle) {
    // TODO: optimize this function more
    with(result) {
        contentPaddingStart = lerp(a.contentPaddingStart, b.contentPaddingStart, t)
        contentPaddingEnd = lerp(a.contentPaddingEnd, b.contentPaddingEnd, t)
        contentPaddingTop = lerp(a.contentPaddingTop, b.contentPaddingTop, t)
        contentPaddingBottom = lerp(a.contentPaddingBottom, b.contentPaddingBottom, t)
    }
}

internal fun lerpDraw(a: ResolvedStyle, b: ResolvedStyle, t: Float, result: ResolvedStyle) {
    // TODO: optimize this function more
    with(result) {
        borderWidth = lerp(a.borderWidth, b.borderWidth, t)
        borderColor = lerp(a.borderColor, b.borderColor, t)
        borderBrush = lerp(a.borderBrush, a.borderColor, b.borderBrush, b.borderColor, t)
        backgroundColor = lerp(a.backgroundColor, b.backgroundColor, t)
        backgroundBrush =
            lerp(a.backgroundBrush, a.backgroundColor, b.backgroundBrush, b.backgroundColor, t)
        foregroundBrush =
            lerp(a.foregroundBrush, Color.Unspecified, b.foregroundBrush, Color.Unspecified, t)
        innerShadow = lerpShadows(a.innerShadow, b.innerShadow, t)
        dropShadow = lerpShadows(a.dropShadow, b.dropShadow, t)
    }
}

internal fun lerpShadows(a: Any?, b: Any?, t: Float): Any? {
    if (a == null && b == null) {
        return null
    }

    val aIsArray = a is Array<*>
    val bIsArray = b is Array<*>

    if (!aIsArray && !bIsArray) {
        return lerp(a as? Shadow, b as? Shadow, t)
    }

    @Suppress("UNCHECKED_CAST")
    return lerpArrayShadows(
        if (aIsArray) a as Array<Shadow> else arrayOf(a as Shadow),
        if (bIsArray) b as Array<Shadow> else arrayOf(b as Shadow),
        t,
    )
}

internal fun lerpArrayShadows(a: Array<Shadow>, b: Array<Shadow>, t: Float): Array<Shadow?> {
    val maxSize = maxOf(a.size, b.size)
    val result = Array<Shadow?>(maxSize) { null }
    for (i in 0 until maxSize) {
        val left = a.getOrNull(i)
        val right = b.getOrNull(i)
        result[i] = lerp(left, right, t)
    }
    return result
}

internal fun lerpLayer(a: ResolvedStyle, b: ResolvedStyle, t: Float, result: ResolvedStyle) {
    // TODO: optimize this function more
    with(result) {
        alpha = lerp(a.alpha, b.alpha, t)
        scaleX = lerp(a.scaleX, b.scaleX, t)
        scaleY = lerp(a.scaleY, b.scaleY, t)
        translationX = lerp(a.translationX, b.translationX, t)
        translationY = lerp(a.translationY, b.translationY, t)
        rotationX = lerp(a.rotationX, b.rotationX, t)
        rotationY = lerp(a.rotationY, b.rotationY, t)
        rotationZ = lerp(a.rotationZ, b.rotationZ, t)
        transformOrigin =
            TransformOrigin(
                lerp(a.transformOrigin.pivotFractionX, b.transformOrigin.pivotFractionX, t),
                lerp(a.transformOrigin.pivotFractionY, b.transformOrigin.pivotFractionY, t),
            )
        zIndex = lerp(a.zIndex, b.zIndex, t)
        shape = lerp(a.shape, b.shape, t)
        clip = if (t < 0.5f) a.clip else b.clip
    }
}

internal fun lerpTextDraw(a: ResolvedStyle, b: ResolvedStyle, t: Float, result: ResolvedStyle) {
    // TODO: optimize this function more
    with(result) {
        contentColor = lerp(a.contentColor, b.contentColor, t)
        contentBrush = lerp(a.contentBrush, a.contentColor, b.contentBrush, b.contentColor, t)
    }
}

internal fun lerpTextLayout(a: ResolvedStyle, b: ResolvedStyle, t: Float, result: ResolvedStyle) {
    // TODO: optimize this function more
    with(result) {
        if (a.fontSize.isSpecified && b.fontSize.isSpecified) {
            fontSize = lerp(a.fontSize, b.fontSize, t)
        }
        if (a.lineHeight.isSpecified && b.lineHeight.isSpecified) {
            lineHeight = lerp(a.lineHeight, b.lineHeight, t)
        }
        if (a.letterSpacing.isSpecified && b.letterSpacing.isSpecified) {
            letterSpacing = lerp(a.letterSpacing, b.letterSpacing, t)
        }

        fontFamily = if (t < 0.5f) a.fontFamily else b.fontFamily
        textIndent = if (t < 0.5f) a.textIndent else b.textIndent
        baselineShift = if (t < 0.5f) a.baselineShift else b.baselineShift
        lineBreak = if (t < 0.5f) a.lineBreak else b.lineBreak
        textEnums = if (t < 0.5f) a.textEnums else b.textEnums

        val aWeight = a.textEnums.getBits(FontWeightMask, FontWeightShift)
        val bWeight = b.textEnums.getBits(FontWeightMask, FontWeightShift)
        if (aWeight > 0 && bWeight > 0) {
            val weight = lerp(aWeight, bWeight, t).floorToNearest100()
            textEnums = textEnums.setBits(FontWeightMask, FontWeightShift, weight)
        }
    }
}

internal fun lerp(a: ResolvedStyle, b: ResolvedStyle, t: Float, flags: Int, result: ResolvedStyle) {
    val resultFlags = a.flags or b.flags
    result.flags = resultFlags // Ensure flags are set on the result
    val flagsToRun = resultFlags and flags

    if (flagsToRun and OuterLayoutFlag != 0) lerpOuterLayout(a, b, t, result)
    if (flagsToRun and InnerLayoutFlag != 0) lerpInnerLayout(a, b, t, result)
    if (flagsToRun and DrawFlag != 0) lerpDraw(a, b, t, result)
    if (flagsToRun and LayerFlag != 0) lerpLayer(a, b, t, result)
    if (flagsToRun and TextDrawFlag != 0) lerpTextDraw(a, b, t, result)
    if (flagsToRun and TextLayoutFlag != 0) lerpTextLayout(a, b, t, result)
}

private inline fun Int.floorToNearest100(): Int {
    return (this / 100) * 100
}

private fun lerp(
    leftBrush: Brush?,
    leftColor: Color,
    rightBrush: Brush?,
    rightColor: Color,
    t: Float,
): Brush? {
    var a: Brush? = leftBrush
    var b: Brush? = rightBrush
    if (leftBrush == null && rightBrush == null) {
        return null
    } else if (leftBrush == null) {
        a = SolidColor(leftColor)
    } else if (rightBrush == null) {
        b = SolidColor(rightColor)
    }
    return Interpolatable.lerp(a, b, t) as? Brush
}

private fun lerp(a: Brush, b: Brush, t: Float): Brush? {
    return Interpolatable.lerp(a, b, t) as? Brush
}

private fun lerp(a: Shape, b: Shape, t: Float): Shape {
    return Interpolatable.lerp(a, b, t) as? Shape ?: RectangleShape
}

private val DefaultSpringSpec = spring<Float>()
private val EmptyResolvedStyle = ResolvedStyle()

internal inline fun LineBreak.takeOrElse(other: LineBreak): LineBreak {
    return if (isSpecified) this else other
}

internal inline fun BaselineShift.takeOrElse(other: BaselineShift): BaselineShift {
    return if (this == BaselineShift.Unspecified) this else other
}

internal inline fun TextUnit.takeOrElse(other: TextUnit): TextUnit {
    return if (isSpecified) this else other
}

internal inline fun Color.takeOrElse(other: Color): Color {
    return if (isSpecified) this else other
}

internal inline fun applyTextEnum(left: Int, right: Int): Int {
    // TODO: make this pipelined by or-ing all of the results instead of having each one being
    //  a dependency on the last
    var left = left
    left = setBitsIfNonZero(left, right, FontStyleMask)
    left = setBitsIfNonZero(left, right, TextAlignMask)
    left = setBitsIfNonZero(left, right, TextDirectionMask)
    left = setBitsIfNonZero(left, right, HyphensMask)
    left = setBitsIfNonZero(left, right, FontSynthesisMask)
    left = setBitsIfNonZero(left, right, FontWeightMask)
    return left
}

internal inline fun setBitsIfNonZero(left: Int, right: Int, mask: Int): Int {
    val rightBits = right and mask
    return (left and mask.inv()) or (if (rightBits != 0) rightBits else left)
}

internal inline fun Int.getBits(mask: Int, shift: Int): Int {
    return (this and mask) shr shift
}

internal inline fun Int.setBits(mask: Int, shift: Int, value: Int): Int {
    return (this and mask.inv()) or ((value shl shift) and mask)
}

private fun updateHashEnter(hash: Int, key: Int) = (hash.rotateLeft(3) xor key)

private fun updateHashExit(hash: Int, key: Int) = (hash xor key).rotateRight(3)

private inline fun MutableIntList.push(value: Int) = add(value)

private inline fun MutableIntList.pop(): Int = removeAt(size - 1)

internal val TextDefaultsResolvedStyle =
    ResolvedStyle().apply {
        fontSize(14.sp)
        letterSpacing(0.sp)
        contentColor(Color.Black)
        fontWeight(FontWeight.Normal)
        fontStyle(FontStyle.Normal)
        fontSynthesis(FontSynthesis.All)
        fontFamily(FontFamily.Default)
        baselineShift(BaselineShift.None)
        textDecoration(TextDecoration.None)
    }

// Packing (offset, length)
// ====
//  0,  2 bits: fontStyle
//  2,  3 bits: textAlign
//  5,  3 bits: textDirection
//  8,  2 bits: hyphens
// 10,  4 bits: fontSynthesis
// 14,  3 bits: textDecoration
// 17, 10 bits: fontWeight
private const val FontStyleShift: Int = 0
private const val TextAlignShift: Int = 2
private const val TextDirectionShift: Int = 4
private const val HyphensShift: Int = 8
private const val FontSynthesisShift: Int = 10
private const val TextDecorationShift: Int = 14
private const val FontWeightShift: Int = 17

private const val FontStyleMask: Int = 0b11 shl FontStyleShift
private const val TextAlignMask: Int = 0b111 shl TextAlignShift
private const val TextDirectionMask: Int = 0b111 shl TextDirectionShift
private const val HyphensMask: Int = 0b11 shl HyphensShift
private const val FontSynthesisMask: Int = 0b1111 shl FontSynthesisShift
private const val TextDecorationMask: Int = 0b111 shl TextDecorationShift
private const val FontWeightMask: Int = 0b11_1111_1111 shl FontWeightShift

internal const val InnerLayoutFlag: Int = 1 shl 0
internal const val DrawFlag: Int = 1 shl 1
internal const val LayerFlag: Int = 1 shl 2

internal const val OuterLayoutFlag: Int = 1 shl 3
internal const val AnimatedFlag: Int = 1 shl 4
internal const val TextLayoutFlag: Int = 1 shl 5
internal const val TextDrawFlag: Int = 1 shl 6

internal const val InheritedFlags: Int = TextLayoutFlag or TextDrawFlag

internal const val AnimateGroup: Int = 0x4e95b218 // Randomly generated positive integer

@Suppress("UNUSED") internal fun ResolvedStyle.flagsAsString() = resolvedStyleFlagsToString(flags)

internal fun resolvedStyleFlagsToString(flags: Int) = buildString {
    var first = true
    fun emit(value: String) {
        if (!first) append(", ")
        first = false
        append(value)
    }
    if (flags and InnerLayoutFlag != 0) emit("InnerLayoutFlag")
    if (flags and DrawFlag != 0) emit("DrawFlag")
    if (flags and LayerFlag != 0) emit("LayerFlag")
    if (flags and OuterLayoutFlag != 0) emit("OuterLayoutFlag")
    if (flags and AnimatedFlag != 0) emit("AnimatedFlag")
    if (flags and TextLayoutFlag != 0) emit("TextLayoutFlag")
    if (flags and TextDrawFlag != 0) emit("TextDrawFlag")
}
