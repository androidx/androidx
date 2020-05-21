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
import androidx.ui.geometry.Size
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.DefaultAlpha
import androidx.ui.graphics.painter.Painter
import androidx.ui.graphics.drawscope.withTransform
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxSize
import androidx.ui.unit.ipx
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

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
 * @sample androidx.ui.core.samples.PainterModifierSample
 */
@Deprecated(
    "Use Modifier.paint",
    replaceWith = ReplaceWith(
        "Modifier.paint(this, sizeToIntrinsics, alignment, contentScale, alpha, colorFilter, rtl)",
        "androidx.ui.core.Modifier",
        "androidx.ui.core.paint"
    )
)
@Suppress("UNUSED_PARAMETER")
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
    return remember(this, sizeToIntrinsics, alignment, contentScale, alpha, colorFilter) {
        PainterModifier(this, sizeToIntrinsics, alignment, contentScale, alpha, colorFilter)
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
 */
fun Modifier.paint(
    painter: Painter,
    sizeToIntrinsics: Boolean = true,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Inside,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) = this + PainterModifier(
    painter = painter,
    sizeToIntrinsics = sizeToIntrinsics,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter
)

/**
 * [DrawModifier] used to draw the provided [Painter] followed by the contents
 * of the component itself
 */
private data class PainterModifier(
    val painter: Painter,
    val sizeToIntrinsics: Boolean,
    val alignment: Alignment = Alignment.Center,
    val contentScale: ContentScale = ContentScale.Inside,
    val alpha: Float = DefaultAlpha,
    val colorFilter: ColorFilter? = null
) : LayoutModifier, DrawModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(modifyConstraints(constraints))
        return layout(placeable.width, placeable.height) {
            placeable.place(IntPx.Zero, IntPx.Zero)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val constraints = Constraints(maxHeight = height)
        val layoutWidth =
            measurable.minIntrinsicWidth(modifyConstraints(constraints).maxHeight).value
        val painterIntrinsicWidth =
            painter.intrinsicSize.width.takeUnless {
                !sizeToIntrinsics || it == Float.POSITIVE_INFINITY
            }?.roundToInt() ?: layoutWidth
        return max(painterIntrinsicWidth, layoutWidth).ipx
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val constraints = Constraints(maxHeight = height)
        val layoutWidth =
            measurable.maxIntrinsicWidth(modifyConstraints(constraints).maxHeight).value
        val painterIntrinsicWidth =
            painter.intrinsicSize.width.takeUnless {
                !sizeToIntrinsics || it == Float.POSITIVE_INFINITY
            }?.roundToInt() ?: layoutWidth
        return max(painterIntrinsicWidth, layoutWidth).ipx
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val constraints = Constraints(maxWidth = width)
        val layoutHeight =
            measurable.minIntrinsicHeight(modifyConstraints(constraints).maxWidth).value
        val painterIntrinsicHeight =
            painter.intrinsicSize.height.takeUnless {
                !sizeToIntrinsics || it == Float.POSITIVE_INFINITY
            }?.roundToInt() ?: layoutHeight
        return max(painterIntrinsicHeight, layoutHeight).ipx
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx {
        val constraints = Constraints(maxWidth = width)
        val layoutHeight =
            measurable.maxIntrinsicHeight(modifyConstraints(constraints).maxWidth).value
        val painterIntrinsicHeight =
            painter.intrinsicSize.height.takeUnless {
                !sizeToIntrinsics || it == Float.POSITIVE_INFINITY
            }?.roundToInt() ?: layoutHeight
        return max(painterIntrinsicHeight, layoutHeight).ipx
    }

    private fun modifyConstraints(constraints: Constraints): Constraints {
        val intrinsicSize = painter.intrinsicSize
        val intrinsicWidth =
            intrinsicSize.width.takeUnless {
                !sizeToIntrinsics || it == Float.POSITIVE_INFINITY
            }?.roundToInt()?.ipx ?: constraints.minWidth
        val intrinsicHeight =
            intrinsicSize.height.takeUnless {
                !sizeToIntrinsics || it == Float.POSITIVE_INFINITY
            }?.roundToInt()?.ipx ?: constraints.minHeight

        val minWidth = intrinsicWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val minHeight = intrinsicHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        return if (minWidth == constraints.minWidth && minHeight == constraints.minHeight) {
            constraints
        } else {
            constraints.copy(minWidth = minWidth, minHeight = minHeight)
        }
    }

    override fun ContentDrawScope.draw() {
        val intrinsicSize = painter.intrinsicSize
        val srcWidth = if (intrinsicSize.width != Float.POSITIVE_INFINITY) {
            intrinsicSize.width
        } else {
            size.width
        }

        val srcHeight = if (intrinsicSize.height != Float.POSITIVE_INFINITY) {
            intrinsicSize.height
        } else {
            size.height
        }

        val srcSize = Size(srcWidth, srcHeight)
        val scale = contentScale.scale(srcSize, size)

        val alignedPosition = alignment.align(
            IntPxSize(
                IntPx(ceil(size.width - (srcWidth * scale)).toInt()),
                IntPx(ceil(size.height - (srcHeight * scale)).toInt())
            )
        )

        val dx = alignedPosition.x.value.toFloat()
        val dy = alignedPosition.y.value.toFloat()

        withTransform({
            translate(dx, dy)
            scale(scale, scale, 0.0f, 0.0f)
        }) {
            with(painter) {
                draw(size = srcSize, alpha = alpha, colorFilter = colorFilter)
            }
        }
    }
}