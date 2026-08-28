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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

@ExperimentalFoundationStyleApi
private abstract class StylePaddingValues(val resolver: StyleResolver) : PaddingValues {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as StylePaddingValues

        if (resolver != other.resolver) return false

        return true
    }

    override fun hashCode(): Int = resolver.hashCode()

    inline fun resolve(crossinline block: StyleResolverScope.() -> Dp) = resolver.resolve(block)
}

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * This modifier is to help enable transition between the old [Style] based API and the new
 * [CustomStyle] based API. This duplicates the behavior of the [contentPadding] properties of
 * [Style].
 */
@ExperimentalFoundationStyleApi
public fun Modifier.styleContentPadding(styleResolver: StyleResolver): Modifier {
    val paddingValues =
        object : StylePaddingValues(styleResolver) {
            override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = resolve {
                when (layoutDirection) {
                    LayoutDirection.Ltr -> contentPaddingStartProperty.value
                    else -> contentPaddingEndProperty.value
                }
            }

            override fun calculateTopPadding(): Dp = resolve { contentPaddingTopProperty.value }

            override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = resolve {
                when (layoutDirection) {
                    LayoutDirection.Ltr -> contentPaddingEndProperty.value
                    else -> contentPaddingStartProperty.value
                }
            }

            override fun calculateBottomPadding(): Dp = resolve {
                contentPaddingBottomProperty.value
            }
        }
    return this.padding(paddingValues)
}

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * This modifier is to help enable transition between the old [Style] based API and the new
 * [CustomStyle] based API. This duplicates the behavior of the [externalPadding] properties of
 * [Style].
 */
@ExperimentalFoundationStyleApi
public fun Modifier.styleExternalPadding(styleResolver: StyleResolver): Modifier {
    val paddingValues =
        object : StylePaddingValues(styleResolver) {
            override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = resolve {
                when (layoutDirection) {
                    LayoutDirection.Ltr -> externalPaddingStartProperty.value
                    else -> externalPaddingEndProperty.value
                }
            }

            override fun calculateTopPadding(): Dp = resolve { externalPaddingTopProperty.value }

            override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = resolve {
                when (layoutDirection) {
                    LayoutDirection.Ltr -> externalPaddingEndProperty.value
                    else -> externalPaddingStartProperty.value
                }
            }

            override fun calculateBottomPadding(): Dp = resolve {
                externalPaddingBottomProperty.value
            }
        }
    return this.padding(paddingValues)
}

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * This modifier is to help enable transition between the old [Style] based API and the new
 * [CustomStyle] based API. This duplicates the padding behavior [BorderScope.borderWidth] of
 * [Style].
 */
@ExperimentalFoundationStyleApi
public fun Modifier.styleBorderPadding(styleResolver: StyleResolver): Modifier {
    val paddingValues =
        object : StylePaddingValues(styleResolver) {
            override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = resolve {
                effectiveBorderWidth
            }

            override fun calculateTopPadding(): Dp = resolve { effectiveBorderWidth }

            override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = resolve {
                effectiveBorderWidth
            }

            override fun calculateBottomPadding(): Dp = resolve { effectiveBorderWidth }
        }
    return this.padding(paddingValues)
}

@ExperimentalFoundationStyleApi
private val StyleResolverScope.effectiveBorderWidth: Dp
    get() {
        val width = getOrNull(borderWidthProperty) ?: return 0.dp
        // Border uses ceil() where padding uses roundToPx(). This forces
        // the rounding to ceil()
        val widthPx = if (width == Dp.Hairline) 1f else ceil(width.toPx())
        return widthPx.toDp()
    }
