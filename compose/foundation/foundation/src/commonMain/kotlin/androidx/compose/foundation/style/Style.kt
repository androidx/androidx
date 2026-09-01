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

@file:Suppress("UNCHECKED_CAST")

package androidx.compose.foundation.style

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.shadow.Shadow
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
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * Style represents an opaque type which encodes a description of how to style a node in compose. It
 * is implemented semantically like a lambda which is executed on a [StyleScope].
 *
 * These Style objects allow for styles to be defined similarly to a chain of Modifiers, however
 * these Styles are applied by passing them into a [styleable] Modifier, or by passing them into an
 * appropriately defined Style parameter of a Composable function.
 *
 * The primary benefits of Style objects are:
 * 1. They define their own observation scope, meaning you can read [State] objects inside of them
 *    without risking recomposition. Properties that are changed as a result of the State changing
 *    will cause only the minimal invalidation possible (ie, changing `background` will only cause a
 *    redraw)
 * 2. CompositionLocals can be read inside of them. This allows for many theme-based values to be
 *    used in their definition without adding to the capture scope of the lambda.
 * 3. The [StyleScope] interface allows for state-based styling to be defined such as "pressed"
 *    states or "hover" states or "focus" states.
 * 4. Transition-based animations of style properties can be done automatically without defining any
 *    animated values by leveraging the animate API.
 *
 * This type will be removed before style becomes stable. It is not yet deprecated to facilitate
 * removing code that was written using the experimental 1.11 version of this API.
 *
 * @sample androidx.compose.foundation.samples.SimpleStyleSample
 * @sample androidx.compose.foundation.samples.StyleAnimationSample
 * @see styleable
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
public fun interface Style : CustomStyle<StyleScope> {
    public companion object : Style {
        @Suppress("MissingJvmStatic") public override fun StyleScope.applyStyle() {}
    }
}

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * Convert a [Style] into a [CommonStyle] for use by the [StyleResolver].
 *
 * This function will be removed before style becomes stable. It is not yet deprecated to facilitate
 * removing code that was written using the experimental 1.11 version of this API.
 */
@ExperimentalFoundationStyleApi
public fun Style.toCommonStyle(): CommonStyle = CommonStyle {
    val scope =
        object : CommonStyleScope by this, StyleScope {
            override fun contentPaddingStart(value: Dp) {
                contentPaddingStartProperty.provide(value)
            }

            override fun contentPaddingEnd(value: Dp) {
                contentPaddingEndProperty.provide(value)
            }

            override fun contentPaddingTop(value: Dp) {
                contentPaddingTopProperty.provide(value)
            }

            override fun contentPaddingBottom(value: Dp) {
                contentPaddingBottomProperty.provide(value)
            }

            override fun externalPaddingStart(value: Dp) {
                externalPaddingStartProperty.provide(value)
            }

            override fun externalPaddingEnd(value: Dp) {
                externalPaddingEndProperty.provide(value)
            }

            override fun externalPaddingTop(value: Dp) {
                externalPaddingTopProperty.provide(value)
            }

            override fun externalPaddingBottom(value: Dp) {
                externalPaddingBottomProperty.provide(value)
            }

            override fun width(value: Dp) {
                widthProperty.provide(Breadth(value))
            }

            override fun height(value: Dp) {
                heightProperty.provide(Breadth(value))
            }

            override fun width(fraction: Float) {
                widthProperty.provide(Breadth(fraction))
            }

            override fun height(fraction: Float) {
                heightProperty.provide(Breadth(fraction))
            }

            override fun left(value: Dp) {
                leftProperty.provide(value)
            }

            override fun top(value: Dp) {
                topProperty.provide(value)
            }

            override fun right(value: Dp) {
                rightProperty.provide(value)
            }

            override fun bottom(value: Dp) {
                bottomProperty.provide(value)
            }

            override fun minWidth(value: Dp) {
                minWidthProperty.provide(value)
            }

            override fun minHeight(value: Dp) {
                minHeightProperty.provide(value)
            }

            override fun maxWidth(value: Dp) {
                maxWidthProperty.provide(value)
            }

            override fun maxHeight(value: Dp) {
                maxHeightProperty.provide(value)
            }

            override fun alpha(value: Float) {
                alphaProperty.provide(value)
            }

            override fun scaleX(value: Float) {
                scaleXProperty.provide(value)
            }

            override fun scaleY(value: Float) {
                scaleYProperty.provide(value)
            }

            override fun translationX(value: Float) {
                translationXProperty.provide(value)
            }

            override fun translationY(value: Float) {
                translationYProperty.provide(value)
            }

            override fun rotationX(value: Float) {
                rotationXProperty.provide(value)
            }

            override fun rotationY(value: Float) {
                rotationYProperty.provide(value)
            }

            override fun rotationZ(value: Float) {
                rotationZProperty.provide(value)
            }

            override fun transformOriginX(value: Float) {
                transformOriginXProperty.provide(value)
            }

            override fun transformOriginY(value: Float) {
                translationYProperty.provide(value)
            }

            override fun colorFilter(value: ColorFilter?) {
                colorFilterProperty.provide(value)
            }

            override fun clip(value: Boolean) {
                clipProperty.provide(value)
            }

            override fun zIndex(value: Float) {
                zIndexProperty.provide(value)
            }

            override fun borderWidth(value: Dp) {
                borderWidthProperty.provide(value)
            }

            override fun borderColor(value: Color) {
                borderFillProperty.provide(Fill(value))
            }

            override fun borderBrush(value: Brush) {
                borderFillProperty.provide(Fill(value))
            }

            override fun background(color: Color) {
                backgroundProperty.provide(Fill(color))
            }

            override fun background(value: Brush) {
                backgroundProperty.provide(Fill(value))
            }

            override fun foreground(value: Color) {
                foregroundProperty.provide(Fill(value))
            }

            override fun foreground(value: Brush) {
                foregroundProperty.provide(Fill(value))
            }

            override fun shape(value: Shape) {
                shapeProperty.provide(value)
            }

            override fun dropShadow(value: Shadow) {
                dropShadowProperty.provide(Shadows(value))
            }

            override fun dropShadow(vararg value: Shadow) {
                dropShadowProperty.provide(Shadows(*value))
            }

            override fun innerShadow(value: Shadow) {
                innerShadowProperty.provide(Shadows(value))
            }

            override fun innerShadow(vararg value: Shadow) {
                innerShadowProperty.provide(Shadows(*value))
            }

            override fun textStyle(value: TextStyle) {
                val brush = value.brush
                if (brush != null) contentBrush(brush)
                else if (value.color.isSpecified) contentColor(value.color)
                value.textDecoration?.let { textDecoration(it) }
                value.fontFamily?.let { fontFamily(it) }
                value.textIndent?.let { textIndent(it) }
                if (value.fontSize.isSpecified) fontSize(value.fontSize)
                if (value.lineHeight.isSpecified) lineHeight(value.lineHeight)
                if (value.letterSpacing.isSpecified) letterSpacing(value.letterSpacing)
                value.baselineShift?.let { baselineShift(it) }
                value.fontWeight?.let { fontWeight(it) }
                value.fontStyle?.let { fontStyle(it) }
                if (value.textDirection.isSpecified) textDirection(value.textDirection)
                if (value.textAlign.isSpecified) textAlign(value.textAlign)
                if (value.lineBreak.isSpecified) lineBreak(value.lineBreak)
                if (value.hyphens.isSpecified) hyphens(value.hyphens)
                value.textMotion?.let { textMotion(it) }
                value.fontSynthesis?.let { fontSynthesis(it) }
            }

            override fun contentColor(value: Color) {
                contentFillLocal.provide(Fill(value))
            }

            override fun contentBrush(value: Brush) {
                contentFillLocal.provide(Fill(value))
            }

            override fun textDecoration(value: TextDecoration) {
                textDecorationLocal.provide(value)
            }

            override fun fontFamily(value: FontFamily) {
                fontFamilyLocal.provide(value)
            }

            override fun textIndent(value: TextIndent) {
                textIndentFirstLineLocal.provide(value.firstLine)
                textIndentRestLineLocal.provide(value.restLine)
            }

            override fun fontSize(value: TextUnit) {
                fontSizeLocal.provide(value)
            }

            override fun lineHeight(value: TextUnit) {
                lineHeightLocal.provide(value)
            }

            override fun letterSpacing(value: TextUnit) {
                letterSpacingLocal.provide(value)
            }

            override fun baselineShift(value: BaselineShift) {
                baselineShiftLocal.provide(value)
            }

            override fun fontWeight(value: FontWeight) {
                fontWeightLocal.provide(value)
            }

            override fun fontStyle(value: FontStyle) {
                fontStyleLocal.provide(value)
            }

            override fun textDirection(value: TextDirection) {
                textDirectionLocal.provide(value)
            }

            override fun textAlign(value: TextAlign) {
                textAlignLocal.provide(value)
            }

            override fun lineBreak(value: LineBreak) {
                lineBreakLocal.provide(value)
            }

            override fun hyphens(value: Hyphens) {
                hyphensLocal.provide(value)
            }

            override fun textMotion(value: TextMotion) {
                textMotionLocal.provide(value)
            }

            override fun fontSynthesis(value: FontSynthesis) {
                fontSynthesisLocal.provide(value)
            }
        }
    with(scope) { applyStyle() }
}

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * Merges this styles with another. The style to the right on the `then` will overwrite the
 * properties set by the style to the left.
 *
 * @param other the style to merge into the receiver.
 */
@ExperimentalFoundationStyleApi
public infix fun Style.then(other: Style): Style = Style(this, other)

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * Combine multiple Style objects together. Styles whose argument positions are further "to the
 * right" will override styles to the left of them, on a per-property basis.
 */
@ExperimentalFoundationStyleApi
public fun Style(style1: Style, style2: Style): Style =
    when {
        style1 === Style -> style2
        style2 === Style -> style1
        style1 is CombinedStyle && style2 is CombinedStyle -> Style(*style1.styles, *style2.styles)
        style1 is CombinedStyle -> Style(*style1.styles, style2)
        style2 is CombinedStyle -> Style(style1, *style2.styles)
        else -> CombinedStyle(style1, style2)
    }

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * Combine multiple Style objects together. Styles whose argument positions are further "to the
 * right" will override styles to the left of them, on a per-property basis.
 */
@ExperimentalFoundationStyleApi
public fun Style(style1: Style, style2: Style, style3: Style): Style =
    when {
        style1 === Style -> Style(style2, style3)
        style2 === Style -> Style(style1, style3)
        style3 === Style -> Style(style1, style2)
        style1 is CombinedStyle && style2 is CombinedStyle && style3 is CombinedStyle ->
            Style(*style1.styles, *style2.styles, *style3.styles)
        style1 is CombinedStyle && style2 is CombinedStyle ->
            Style(*style1.styles, *style2.styles, style3)
        style1 is CombinedStyle && style3 is CombinedStyle ->
            Style(*style1.styles, style2, *style3.styles)
        style2 is CombinedStyle && style3 is CombinedStyle ->
            Style(style1, *style2.styles, *style3.styles)
        style1 is CombinedStyle -> Style(*style1.styles, style2, style3)
        style2 is CombinedStyle -> Style(style1, *style2.styles, style3)
        style3 is CombinedStyle -> Style(style1, style2, *style3.styles)
        else -> Style(*arrayOf(style1, style2, style3))
    }

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * Combine multiple Style objects together. Styles whose argument positions are further "to the
 * right" will override styles to the left of them, on a per-property basis.
 */
@ExperimentalFoundationStyleApi
public fun Style(vararg styles: Style): Style =
    if (styles.fastAny { it === Style }) {
        val count = styles.fastCount { it !== Style }
        when (count) {
            0 -> Style
            1 -> styles.fastFirst { it !== Style }
            else -> {
                val result = arrayOfNulls<Style>(count)
                var current = 0
                styles.fastForEach {
                    if (it !== Style) {
                        result[current++] = it
                    }
                }
                CombinedStyle(*(result as Array<Style>))
            }
        }
    } else {
        CombinedStyle(*styles)
    }

private inline fun <T> Array<T>.fastAny(predicate: (T) -> Boolean): Boolean {
    for (index in indices) {
        if (predicate(this[index])) return true
    }
    return false
}

private inline fun <T> Array<T>.fastCount(predicate: (T) -> Boolean): Int {
    var count = 0
    for (index in indices) {
        if (predicate(this[index])) count++
    }
    return count
}

private inline fun <T> Array<T>.fastFirst(predicate: (T) -> Boolean): T {
    for (index in indices) {
        val value = this[index]
        if (predicate(value)) return value
    }
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

private inline fun <T> Array<T>.fastForEach(block: (T) -> Unit) {
    for (index in indices) {
        block(this[index])
    }
}

/**
 * An internal helper class that is used to combine two or more styles together. Calling
 * [applyStyle] will apply all the styles in the [styles] field in order.
 */
@ExperimentalFoundationStyleApi
internal class CombinedStyle(vararg val styles: Style) : Style {
    override fun StyleScope.applyStyle() {
        for (style in styles) {
            with(style) { applyStyle() }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CombinedStyle) return false

        return styles.contentEquals(other.styles)
    }

    override fun hashCode(): Int = styles.contentHashCode()
}
