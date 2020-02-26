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
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.DefaultAlpha
import androidx.ui.graphics.ScaleFit
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
 * @param scaleFit: Specifies the rule used to scale the content of the Painter within the
 * specified bounds, the default of [ScaleFit.Fit] scales the content to be as large as possible
 * within the specified bounds while still maintaining the aspect ratio of its intrinsic size
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
@Composable
fun Painter.toModifier(
    sizeToIntrinsics: Boolean = true,
    alignment: Alignment = Alignment.Center,
    scaleFit: ScaleFit = ScaleFit.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    rtl: Boolean = false
): DrawModifier {
    // TODO potentially create thread-safe PainterModifier pool to allow for re-use
    //  of PainterModifier instances and avoid gc overhead
    return remember(this, sizeToIntrinsics, alignment, scaleFit, alpha, colorFilter, rtl) {
        PainterModifier(this, sizeToIntrinsics, alignment, scaleFit, alpha, colorFilter, rtl)
    }
}

/**
 * [DrawModifier] used to draw the provided [Painter] followed by the contents
 * of the component itself
 */
private data class PainterModifier(
    val painter: Painter,
    var sizeToIntrinsics: Boolean,
    var alignment: Alignment = Alignment.Center,
    var scaleFit: ScaleFit = ScaleFit.Fit,
    var alpha: Float = DefaultAlpha,
    var colorFilter: ColorFilter? = null,
    var rtl: Boolean = false
) : LayoutModifier, DrawModifier {

    override fun ModifierScope.modifyConstraints(constraints: Constraints): Constraints {
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

    override fun ModifierScope.minIntrinsicWidthOf(measurable: Measurable, height: IntPx): IntPx {
        val constraints = Constraints(maxHeight = height)
        val layoutWidth = measurable.minIntrinsicWidth(modifyConstraints(constraints).maxHeight)
        val painterIntrinsicWidth =
            painter.intrinsicSize.width.takeUnless {
                !sizeToIntrinsics || it == Px.Infinity
            }?.ceil() ?: layoutWidth
        return max(painterIntrinsicWidth, layoutWidth)
    }

    override fun ModifierScope.maxIntrinsicWidthOf(measurable: Measurable, height: IntPx): IntPx {
        val constraints = Constraints(maxHeight = height)
        val layoutWidth = measurable.maxIntrinsicWidth(modifyConstraints(constraints).maxHeight)
        val painterIntrinsicWidth =
            painter.intrinsicSize.width.takeUnless {
                !sizeToIntrinsics || it == Px.Infinity
            }?.ceil() ?: layoutWidth
        return max(painterIntrinsicWidth, layoutWidth)
    }

    override fun ModifierScope.minIntrinsicHeightOf(measurable: Measurable, width: IntPx): IntPx {
        val constraints = Constraints(maxWidth = width)
        val layoutHeight = measurable.minIntrinsicHeight(modifyConstraints(constraints).maxWidth)
        val painterIntrinsicHeight =
            painter.intrinsicSize.height.takeUnless {
                !sizeToIntrinsics || it == Px.Infinity
            }?.ceil() ?: layoutHeight
        return max(painterIntrinsicHeight, layoutHeight)
    }

    override fun ModifierScope.maxIntrinsicHeightOf(measurable: Measurable, width: IntPx): IntPx {
        val constraints = Constraints(maxWidth = width)
        val layoutHeight = measurable.maxIntrinsicHeight(modifyConstraints(constraints).maxWidth)
        val painterIntrinsicHeight =
            painter.intrinsicSize.height.takeUnless {
                !sizeToIntrinsics || it == Px.Infinity
            }?.ceil() ?: layoutHeight
        return max(painterIntrinsicHeight, layoutHeight)
    }

    override fun draw(
        density: Density,
        drawContent: () -> Unit,
        canvas: Canvas,
        size: PxSize
    ) {
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

        val scale = scaleFit.scale(PxSize(srcWidth, srcHeight), size)

        val alignedPosition = alignment.align(
            IntPxSize(
                IntPx(ceil(size.width.value - (srcWidth.value * scale)).toInt()),
                IntPx(ceil(size.height.value - (srcHeight.value * scale)).toInt())
            )
        )

        val dx = alignedPosition.x.value.toFloat()
        val dy = alignedPosition.y.value.toFloat()

        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)

        painter.draw(
            canvas = canvas,
            bounds = PxSize(srcWidth, srcHeight),
            alpha = alpha,
            colorFilter = colorFilter,
            rtl = rtl)

        canvas.restore()
    }
}