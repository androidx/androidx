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

package androidx.ui.core

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.DefaultAlpha
import androidx.ui.graphics.painter.Painter
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.Px
import androidx.ui.unit.PxSize
import androidx.ui.unit.ceil
import androidx.ui.unit.max
import kotlin.math.ceil

/**
 * Create a [DrawModifier] from this [Painter]. This modifier is memoized and re-used across
 * subsequent compositions
 *
 * @param sizeToIntrinsics: Flag to indicate whether this PainterModifier should be involved with
 * appropriately sizing the component it is associated with. True if the intrinsic size should
 * influence the size of the component, false otherwise. A value of false here is equivalent to
 * the underlying Painter having no intrinsic size, that is [Painter.intrinsicSize] returns
 * [PxSize.UnspecifiedSize]
 *
 * @param alignment: Specifies the rule used to place the contents of the Painter within the
 * specified bounds, the default of [Alignment.Center] centers the content within the specified
 * rendering bounds
 *
 * @param contentScale: Specifies the rule used to scale the content of the Painter within the
 * specified bounds, the default of [ContentScale.Inside] scales the content to be as large as
 * possible within the specified bounds while still maintaining the aspect ratio of its intrinsic
 * size
 *
 * @param alpha: Specifies the opacity to render the contents of the underlying [Painter]
 *
 * @param colorFilter: Specifies an optional tint to apply to the contents of the [Painter] when
 * drawn in the specified area
 *
 * @param rtl: Flag to indicate contents of the [Painter] should render for right to left languages
 *
 * @sample androidx.ui.framework.samples.PainterModifierSample
 */
@Deprecated(
    "Use Modifier.paint",
    replaceWith = ReplaceWith(
        "Modifier.paint(this, sizeToIntrinsics, alignment, contentScale, alpha, colorFilter, rtl)",
        "androidx.ui.core.Modifier",
        "androidx.ui.core.paint"
    )
)
@Composable
fun Painter.asModifier(
    sizeToIntrinsics: Boolean = true,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Inside,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    rtl: Boolean = false
): DrawModifier {
    // TODO potentially create thread-safe PainterModifier pool to allow for re-use
    //  of PainterModifier instances and avoid gc overhead
    return remember(this, sizeToIntrinsics, alignment, contentScale, alpha, colorFilter, rtl) {
        PainterModifier(this, sizeToIntrinsics, alignment, contentScale, alpha, colorFilter, rtl)
    }
}

/**
 * Paint the content using [painter].
 *
 * @param sizeToIntrinsics `true` to size the element relative to [Painter.intrinsicSize]
 * @param alignment specifies alignment of the [painter] relative to content
 * @param contentScale strategy for scaling [painter] if its size does not match the content size
 * @param alpha opacity of [painter]
 * @param colorFilter optional [ColorFilter] to apply to [painter]
 * @param rtl layout direction to report to [painter] when drawing
 */
fun Modifier.paint(
    painter: Painter,
    sizeToIntrinsics: Boolean = true,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Inside,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    rtl: Boolean = false
) = this + PainterModifier(
    painter = painter,
    sizeToIntrinsics = sizeToIntrinsics,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    rtl = rtl
)

/**
 * [DrawModifier] used to draw the provided [Painter] followed by the contents
 * of the component itself
 */
@Suppress("Deprecation")
private data class PainterModifier(
    val painter: Painter,
    val sizeToIntrinsics: Boolean,
    val alignment: Alignment = Alignment.Center,
    val contentScale: ContentScale = ContentScale.Inside,
    val alpha: Float = DefaultAlpha,
    val colorFilter: ColorFilter? = null,
    val rtl: Boolean = false
) : LayoutModifier, DrawModifier {

    override fun Density.modifyConstraints(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): Constraints {
        val intrinsicSize = painter.intrinsicSize
        val intrinsicWidth =
            intrinsicSize.width.takeUnless {
                !sizeToIntrinsics || it == Px.Infinity
            }?.ceil() ?: constraints.minWidth
        val intrinsicHeight =
            intrinsicSize.height.takeUnless {
                !sizeToIntrinsics || it == Px.Infinity
            }?.ceil() ?: constraints.minHeight

        val minWidth = intrinsicWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val minHeight = intrinsicHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        return if (minWidth == constraints.minWidth && minHeight == constraints.minHeight) {
            constraints
        } else {
            constraints.copy(minWidth = minWidth, minHeight = minHeight)
        }
    }

    override fun Density.minIntrinsicWidthOf(
        measurable: Measurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val constraints = Constraints(maxHeight = height)
        val layoutWidth = measurable.minIntrinsicWidth(
            modifyConstraints(constraints, layoutDirection).maxHeight,
            layoutDirection
        )
        val painterIntrinsicWidth =
            painter.intrinsicSize.width.takeUnless {
                !sizeToIntrinsics || it == Px.Infinity
            }?.ceil() ?: layoutWidth
        return max(painterIntrinsicWidth, layoutWidth)
    }

    override fun Density.maxIntrinsicWidthOf(
        measurable: Measurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val constraints = Constraints(maxHeight = height)
        val layoutWidth = measurable.maxIntrinsicWidth(
            modifyConstraints(constraints, layoutDirection).maxHeight,
            layoutDirection
        )
        val painterIntrinsicWidth =
            painter.intrinsicSize.width.takeUnless {
                !sizeToIntrinsics || it == Px.Infinity
            }?.ceil() ?: layoutWidth
        return max(painterIntrinsicWidth, layoutWidth)
    }

    override fun Density.minIntrinsicHeightOf(
        measurable: Measurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val constraints = Constraints(maxWidth = width)
        val layoutHeight = measurable.minIntrinsicHeight(
            modifyConstraints(constraints, layoutDirection).maxWidth,
            layoutDirection
        )
        val painterIntrinsicHeight =
            painter.intrinsicSize.height.takeUnless {
                !sizeToIntrinsics || it == Px.Infinity
            }?.ceil() ?: layoutHeight
        return max(painterIntrinsicHeight, layoutHeight)
    }

    override fun Density.maxIntrinsicHeightOf(
        measurable: Measurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val constraints = Constraints(maxWidth = width)
        val layoutHeight = measurable.maxIntrinsicHeight(
            modifyConstraints(constraints, layoutDirection).maxWidth,
            layoutDirection
        )
        val painterIntrinsicHeight =
            painter.intrinsicSize.height.takeUnless {
                !sizeToIntrinsics || it == Px.Infinity
            }?.ceil() ?: layoutHeight
        return max(painterIntrinsicHeight, layoutHeight)
    }

    override fun ContentDrawScope.draw() {
        val intrinsicSize = painter.intrinsicSize
        val srcWidth = if (intrinsicSize.width.value != Float.POSITIVE_INFINITY) {
            intrinsicSize.width
        } else {
            size.width
        }

        val srcHeight = if (intrinsicSize.height.value != Float.POSITIVE_INFINITY) {
            intrinsicSize.height
        } else {
            size.height
        }

        val scale = contentScale.scale(PxSize(srcWidth, srcHeight), size)

        val alignedPosition = alignment.align(
            IntPxSize(
                IntPx(ceil(size.width.value - (srcWidth.value * scale)).toInt()),
                IntPx(ceil(size.height.value - (srcHeight.value * scale)).toInt())
            )
        )

        val dx = alignedPosition.x.value.toFloat()
        val dy = alignedPosition.y.value.toFloat()

        save()
        translate(dx, dy)
        scale(scale, scale)

        painter.draw(
            canvas = this,
            bounds = PxSize(srcWidth, srcHeight),
            alpha = alpha,
            colorFilter = colorFilter,
            rtl = rtl)

        restore()
    }
}