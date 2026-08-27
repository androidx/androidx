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

/**
 * A [CommonStyleScope] is the scope used by a [CommonStyle]. It provides the common implementation
 * that all styles scopes share. Is created by a [StyleResolver] when resolving styles and is used
 * to collect the properties set by a style.
 */
@ExperimentalFoundationStyleApi
public interface CommonStyleScope :
    CustomStyleScope,
    AnimateStyleScope,
    StyleStateScope,
    NestedStyleProviderStyleScope,
    NestedStyleConsumerStyleScope,
    StyleLayerScope

/**
 * The [CommonStyle] is the style all style types must be converted to be resolved by the
 * [StyleResolver].
 */
@ExperimentalFoundationStyleApi
public fun interface CommonStyle : CustomStyle<CommonStyleScope> {
    public companion object : CommonStyle {
        @Suppress("MissingJvmStatic") override fun CommonStyleScope.applyStyle() {}
    }
}

/**
 * Produce a [CommonStyle] by merging [style1] and [style2] where [style1] will apply before
 * [style2].
 *
 * @param style1 the first style to merge that will be applied first.
 * @param style2 the second style that will be applied second.
 * @return a merged style of [style1] and [style2]
 */
@ExperimentalFoundationStyleApi
public fun CommonStyle(style1: CommonStyle, style2: CommonStyle): CommonStyle = style1 then style2

/**
 * Produce a [CommonStyle] that merges all the style parameters together in the order they are
 * provided. Merging removes instances of the default empty [CommonStyle] as well as flattening the
 * merged styles. For example `Common(CommonStyle, CommonStyle, someStyle)`` will return `someStyle`
 * as the first two parameters are empty.
 *
 * @param style1 the first style to merge that will be applied first.
 * @param style2 the second style that will be applied second.
 * @param styles the rest of the parameter list that will be applied, in order, after [style2]
 * @return a merged style of all the parameters in parameter order.
 */
@ExperimentalFoundationStyleApi
public fun CommonStyle(
    style1: CommonStyle,
    style2: CommonStyle,
    vararg styles: CommonStyle,
): CommonStyle =
    if (style1 == CommonStyle)
        if (style2 == CommonStyle) unionStyles(*styles) else unionStyles(style2, *styles)
    else unionStyles(style1, style2, *styles)

@ExperimentalFoundationStyleApi
private fun unionStyles(vararg styles: CommonStyle): CommonStyle {
    val count = styles.fastSum {
        when (it) {
            CommonStyle -> 0
            is DualCommonStyle -> 2
            is CombinedCommonStyle -> it.styles.size
            else -> 1
        }
    }
    return when (count) {
        0 -> CommonStyle
        1 -> styles.fastFirst { it !== CommonStyle }
        else -> {
            val result = arrayOfNulls<CommonStyle>(count)
            var current = 0
            styles.fastForEach {
                when (it) {
                    CommonStyle -> {}
                    is DualCommonStyle -> {
                        result[current++] = it.style1
                        result[current++] = it.style2
                    }

                    is CombinedCommonStyle -> {
                        for (style in it.styles) {
                            result[current++] = style
                        }
                    }
                    else -> result[current++] = it
                }
            }
            @Suppress("UNCHECKED_CAST")
            result as Array<CommonStyle>
            if (result.size == 2) DualCommonStyle(result[0], result[1])
            else CombinedCommonStyle(*result)
        }
    }
}

private inline fun <T> Array<T>.fastSum(predicate: (T) -> Int): Int {
    var result = 0
    for (index in indices) {
        result += predicate(this[index])
    }
    return result
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

@ExperimentalFoundationStyleApi
internal class DualCommonStyle(val style1: CommonStyle, val style2: CommonStyle) : CommonStyle {
    override fun CommonStyleScope.applyStyle() {
        apply(style1)
        apply(style2)
    }
}

@ExperimentalFoundationStyleApi
internal class CombinedCommonStyle(vararg val styles: CommonStyle) : CommonStyle {
    override fun CommonStyleScope.applyStyle() {
        for (style in styles) apply(style)
    }
}

/**
 * Merge this style with another style where this style will be applied before the [other] style.
 *
 * @param other the right side of the` operator which will merge after [this].
 */
@ExperimentalFoundationStyleApi
public infix fun CommonStyle.then(other: CommonStyle): CommonStyle =
    when {
        this === CommonStyle -> other
        other === CommonStyle -> this
        this is DualCommonStyle ||
            other is DualCommonStyle ||
            this is CombinedCommonStyle ||
            other is CombinedCommonStyle -> unionStyles(this, other)
        else -> DualCommonStyle(this, other)
    }
