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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.Fill
import androidx.compose.foundation.style.StylePropertyAccessorScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

/**
 * The base scope of all Material3 component styles that are based on
 * [androidx.compose.material3.Surface], which provides the general style properties that are
 * applicable to surface-based components like [background], [shape], etc.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
internal interface SurfaceStyleScope : BoxStyleScope {

    /** Sets the content padding for the start edge of the layout. */
    public fun contentPaddingStart(padding: Dp) =
        SurfaceStyleProperties.ContentPaddingStart.provide(padding)

    /** Sets the content padding for the end edge of the layout. */
    public fun contentPaddingEnd(padding: Dp) =
        SurfaceStyleProperties.ContentPaddingEnd.provide(padding)

    /** Sets the content padding for the top edge of the layout. */
    public fun contentPaddingTop(padding: Dp) =
        SurfaceStyleProperties.ContentPaddingTop.provide(padding)

    /** Sets the content padding for the bottom edge of the layout. */
    public fun contentPaddingBottom(padding: Dp) =
        SurfaceStyleProperties.ContentPaddingBottom.provide(padding)

    /** Sets the content padding for all four edges of the layout to the given [Dp]. */
    public fun contentPadding(padding: Dp) {
        contentPaddingStart(padding)
        contentPaddingEnd(padding)
        contentPaddingTop(padding)
        contentPaddingBottom(padding)
    }

    /** Sets the content padding for four edges to the individual given [Dp]s. */
    public fun contentPadding(start: Dp, top: Dp, end: Dp, bottom: Dp) {
        contentPaddingStart(start)
        contentPaddingTop(top)
        contentPaddingEnd(end)
        contentPaddingBottom(bottom)
    }

    /** Sets the content padding of the layout from the given [PaddingValues]. */
    public fun contentPadding(paddingValues: PaddingValues) {
        val direction = LocalLayoutDirection.currentValue
        contentPadding(
            start = paddingValues.calculateStartPadding(direction),
            top = paddingValues.calculateTopPadding(),
            end = paddingValues.calculateEndPadding(direction),
            bottom = paddingValues.calculateBottomPadding(),
        )
    }

    /** Sets the background to the given [Fill]. */
    public fun background(fill: Fill) = SurfaceStyleProperties.Background.provide(fill)

    /** Sets the background to the given [Color]. */
    public fun background(color: Color) = background(Fill(color))

    /** Sets the background to the given [Brush]. */
    public fun background(brush: Brush) = background(Fill(brush))

    /** Sets the preferred [Shape] */
    public fun shape(shape: Shape) = SurfaceStyleProperties.Shape.provide(shape)

    /**
     * Sets the preferred shape to a [RoundedCornerShape] with all its corner sizes equal to the
     * given [Dp].
     */
    public fun cornerSize(cornerSize: Dp) = shape(RoundedCornerShape(cornerSize))

    /**
     * Sets the preferred shape to a [RoundedCornerShape] with all its corner sizes equal to the
     * given percentage in [Int].
     */
    public fun cornerSize(percent: Int) = shape(RoundedCornerShape(percent))

    /** Sets the border width in [Dp]. */
    public fun borderWidth(width: Dp) = SurfaceStyleProperties.BorderWidth.provide(width)

    /** Sets the [Fill] used to draw the border. */
    public fun borderFill(fill: Fill) = SurfaceStyleProperties.BorderFill.provide(fill)

    /** Sets the [Color] used to draw the border. */
    public fun borderColor(color: Color) = borderFill(Fill(color))

    /** Sets the [Brush] used to draw the border. */
    public fun borderBrush(brush: Brush) = borderFill(Fill(brush))

    /** Sets the [BorderStroke] used to draw the border. */
    public fun borderStroke(stroke: BorderStroke) {
        borderWidth(stroke.width)
        borderBrush(stroke.brush)
    }

    /** Sets the [Stroke] used to draw the border. */
    public fun borderStroke(stroke: Stroke) = SurfaceStyleProperties.BorderStroke.provide(stroke)

    /** Sets the ripple to the given [Color]. */
    public fun rippleColor(color: Color) = SurfaceStyleProperties.RippleColor.provide(color)
}

/** Returns the resolved padding start in [Dp], or [Dp.Unspecified] if not set. */
@OptIn(ExperimentalFoundationStyleApi::class)
internal val StylePropertyAccessorScope.contentPaddingStart: Dp
    get() = SurfaceStyleProperties.ContentPaddingStart.value

/** Returns the resolved padding end in [Dp], or [Dp.Unspecified] if not set. */
@OptIn(ExperimentalFoundationStyleApi::class)
internal val StylePropertyAccessorScope.contentPaddingEnd: Dp
    get() = SurfaceStyleProperties.ContentPaddingEnd.value

/** Returns the resolved padding top in [Dp], or [Dp.Unspecified] if not set. */
@OptIn(ExperimentalFoundationStyleApi::class)
internal val StylePropertyAccessorScope.contentPaddingTop: Dp
    get() = SurfaceStyleProperties.ContentPaddingTop.value

/** Returns the resolved padding bottom in [Dp], or [Dp.Unspecified] if not set. */
@OptIn(ExperimentalFoundationStyleApi::class)
internal val StylePropertyAccessorScope.contentPaddingBottom: Dp
    get() = SurfaceStyleProperties.ContentPaddingBottom.value

/** Returns the resolved background [Fill], or [Fill.None] if not set. */
@OptIn(ExperimentalFoundationStyleApi::class)
internal val StylePropertyAccessorScope.background: Fill
    get() = SurfaceStyleProperties.Background.value

/** Returns the resolved [Shape], or [RectangleShape] if not set. */
@OptIn(ExperimentalFoundationStyleApi::class)
internal val StylePropertyAccessorScope.shape: Shape
    get() = SurfaceStyleProperties.Shape.value

/** Returns the resolved border width in [Dp], or [Dp.Unspecified] if not set. */
@OptIn(ExperimentalFoundationStyleApi::class)
internal val StylePropertyAccessorScope.borderWidth: Dp
    get() = SurfaceStyleProperties.BorderWidth.value

/** Returns the resolved border [Fill], or [Fill.None] if not set. */
@OptIn(ExperimentalFoundationStyleApi::class)
internal val StylePropertyAccessorScope.borderFill: Fill
    get() = SurfaceStyleProperties.BorderFill.value

/** Returns the resolved border [Stroke], or `null` if not set. */
@OptIn(ExperimentalFoundationStyleApi::class)
internal val StylePropertyAccessorScope.borderStroke: Stroke?
    get() = SurfaceStyleProperties.BorderStroke.value

/** Returns the resolved ripple [Color], or [Color.Unspecified] if not set. */
@OptIn(ExperimentalFoundationStyleApi::class)
internal val StylePropertyAccessorScope.rippleColor: Color
    get() = SurfaceStyleProperties.RippleColor.value

@OptIn(ExperimentalFoundationStyleApi::class)
private object SurfaceStyleProperties {
    val ContentPaddingStart = dpStylePropertyOf("ContentPaddingStart")
    val ContentPaddingEnd = dpStylePropertyOf("ContentPaddingEnd")
    val ContentPaddingTop = dpStylePropertyOf("ContentPaddingTop")
    val ContentPaddingBottom = dpStylePropertyOf("ContentPaddingBottom")
    val Background = fillStylePropertyOf("Background")
    val Shape = nonNullStylePropertyOf("Shape") { RectangleShape }
    val BorderWidth = dpStylePropertyOf("BorderWidth")
    val BorderFill = fillStylePropertyOf("BorderFill")
    val BorderStroke = nullableStylePropertyOf<Stroke>("BorderStroke")
    val RippleColor = colorStylePropertyOf("RippleColor")
}
