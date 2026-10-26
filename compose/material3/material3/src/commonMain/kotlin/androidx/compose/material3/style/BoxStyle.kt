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

package androidx.compose.material3.style

import androidx.annotation.FloatRange
import androidx.compose.foundation.style.Breadth
import androidx.compose.foundation.style.CommonStyleScope
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.StylePropertyAccessorScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

/**
 * The base scope of all Material3 component styles, which provides the general style properties
 * that are applicable to all components like [width] or [height].
 */
@OptIn(ExperimentalFoundationStyleApi::class)
internal interface BoxStyleScope : CommonStyleScope {
    /** Sets the preferred component width in [Dp]. */
    public fun width(size: Dp) = BoxStyleProperties.Width.provide(Breadth.Distance(size))

    /** Sets the preferred component height in [Dp]. */
    public fun height(size: Dp) = BoxStyleProperties.Height.provide(Breadth.Distance(size))

    /** Sets the preferred component width as a fraction of available width. */
    public fun width(@FloatRange(from = 0.0, to = 1.0) fraction: Float) =
        BoxStyleProperties.Width.provide(Breadth.Fraction(fraction))

    /** Sets the preferred component height as a fraction of available height. */
    public fun height(@FloatRange(from = 0.0, to = 1.0) fraction: Float) =
        BoxStyleProperties.Height.provide(Breadth.Fraction(fraction))

    /** Sets both the preferred component width and height to [size] in [Dp]. */
    public fun size(size: Dp) {
        width(size)
        height(size)
    }

    /** Sets the preferred component size in [DpSize]. */
    public fun size(size: DpSize) {
        width(size.width)
        height(size.height)
    }
}

/** Returns the resolved component width in [Breadth], or [Breadth.None] if not set. */
@OptIn(ExperimentalFoundationStyleApi::class)
internal val StylePropertyAccessorScope.width: Breadth
    get() = BoxStyleProperties.Width.value

/** Returns the resolved component height in [Breadth], or [Breadth.None] if not set. */
@OptIn(ExperimentalFoundationStyleApi::class)
internal val StylePropertyAccessorScope.height: Breadth
    get() = BoxStyleProperties.Height.value

@OptIn(ExperimentalFoundationStyleApi::class)
private object BoxStyleProperties {
    val Width = breadthStylePropertyOf("Width")
    val Height = breadthStylePropertyOf("Height")
}
