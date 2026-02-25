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

/**
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
 * @sample androidx.compose.foundation.samples.SimpleStyleSample
 * @sample androidx.compose.foundation.samples.StyleAnimationSample
 * @see styleable
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
fun interface Style {
    fun StyleScope.applyStyle()

    companion object : Style {
        @Suppress("MissingJvmstatic") override fun StyleScope.applyStyle() {}
    }
}

/**
 * Merges this styles with another. The style to the right on the `then` will overwrite the
 * properties set by the style to the left.
 *
 * @param other the style to merge into the receiver.
 */
@ExperimentalFoundationStyleApi infix fun Style.then(other: Style): Style = Style(this, other)

/**
 * Combine multiple Style objects together. Styles whose argument positions are further "to the
 * right" will override styles to the left of them, on a per-property basis.
 */
@ExperimentalFoundationStyleApi
fun Style(style1: Style, style2: Style): Style =
    when {
        style1 === Style -> style2
        style2 === Style -> style1
        style1 is CombinedStyle && style2 is CombinedStyle -> Style(*style1.styles, *style2.styles)
        style1 is CombinedStyle -> Style(*style1.styles, style2)
        style2 is CombinedStyle -> Style(style1, *style2.styles)
        else -> CombinedStyle(style1, style2)
    }

/**
 * Combine multiple Style objects together. Styles whose argument positions are further "to the
 * right" will override styles to the left of them, on a per-property basis.
 */
@ExperimentalFoundationStyleApi
fun Style(style1: Style, style2: Style, style3: Style): Style =
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
 * Combine multiple Style objects together. Styles whose argument positions are further "to the
 * right" will override styles to the left of them, on a per-property basis.
 */
@ExperimentalFoundationStyleApi
fun Style(vararg styles: Style): Style =
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
