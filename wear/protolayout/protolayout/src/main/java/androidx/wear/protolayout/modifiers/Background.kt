/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.wear.protolayout.modifiers

import android.annotation.SuppressLint
import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.CornerRadius
import androidx.wear.protolayout.expression.RequiresSchemaVersion
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.cornerRadius
import androidx.wear.protolayout.types.dp

/** Sets the background color to [color]. */
fun LayoutModifier.backgroundColor(color: LayoutColor): LayoutModifier =
    this then BaseBackgroundElement(color)

/**
 * Sets the background color and clipping.
 *
 * @param color for the background
 * @param corner to use for clipping the background
 */
fun LayoutModifier.background(color: LayoutColor, corner: Corner? = null): LayoutModifier =
    if (corner == null) {
        this then BaseBackgroundElement(color)
    } else {
        this then BaseBackgroundElement(color).clip(corner)
    }

/** Clips the element to a rounded rectangle with four corners with [cornerRadius] radius. */
fun LayoutModifier.clip(@Dimension(DP) cornerRadius: Float): LayoutModifier =
    this then BaseCornerElement(cornerRadius)

/**
 * Clips the element to a rounded shape with [x] as the radius on the horizontal axis and [y] as the
 * radius on the vertical axis for the four corners.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
fun LayoutModifier.clip(@Dimension(DP) x: Float, @Dimension(DP) y: Float): LayoutModifier {
    val r = cornerRadius(x, y)
    return this then
        BaseCornerElement(
            topLeftRadius = r,
            topRightRadius = r,
            bottomLeftRadius = r,
            bottomRightRadius = r
        )
}

/** Clips the element to a rounded rectangle with corners specified in [corner]. */
fun LayoutModifier.clip(corner: Corner): LayoutModifier =
    this then
        BaseCornerElement(
            cornerRadiusDp = corner.radius?.value,
            topLeftRadius = corner.topLeftRadius,
            topRightRadius = corner.topRightRadius,
            bottomLeftRadius = corner.bottomLeftRadius,
            bottomRightRadius = corner.bottomRightRadius
        )

/**
 * Clips the top left corner of the element with [x] as the radius on the horizontal axis and [y] as
 * the radius on the vertical axis.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
fun LayoutModifier.clipTopLeft(
    @Dimension(DP) x: Float,
    @Dimension(DP) y: Float = x
): LayoutModifier = this then BaseCornerElement(topLeftRadius = cornerRadius(x, y))

/**
 * Clips the top right corner of the element with [x] as the radius on the horizontal axis and [y]
 * as the radius on the vertical axis.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
fun LayoutModifier.clipTopRight(
    @Dimension(DP) x: Float,
    @Dimension(DP) y: Float = x
): LayoutModifier = this then BaseCornerElement(topRightRadius = cornerRadius(x, y))

/**
 * Clips the bottom left corner of the element with [x] as the radius on the horizontal axis and [y]
 * as the radius on the vertical axis.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
fun LayoutModifier.clipBottomLeft(
    @Dimension(DP) x: Float,
    @Dimension(DP) y: Float = x
): LayoutModifier = this then BaseCornerElement(bottomLeftRadius = cornerRadius(x, y))

/**
 * Clips the bottom right corner of the element with [x] as the radius on the horizontal axis and
 * [y] as the radius on the vertical axis.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
fun LayoutModifier.clipBottomRight(
    @Dimension(DP) x: Float,
    @Dimension(DP) y: Float = x
): LayoutModifier = this then BaseCornerElement(bottomRightRadius = cornerRadius(x, y))

internal class BaseBackgroundElement(val color: LayoutColor) : LayoutModifier.Element {
    fun mergeTo(initial: Background.Builder?): Background.Builder =
        (initial ?: Background.Builder()).setColor(color.prop)
}

internal class BaseCornerElement(
    val cornerRadiusDp: Float? = null,
    @RequiresSchemaVersion(major = 1, minor = 400) val topLeftRadius: CornerRadius? = null,
    @RequiresSchemaVersion(major = 1, minor = 400) val topRightRadius: CornerRadius? = null,
    @RequiresSchemaVersion(major = 1, minor = 400) val bottomLeftRadius: CornerRadius? = null,
    @RequiresSchemaVersion(major = 1, minor = 400) val bottomRightRadius: CornerRadius? = null
) : LayoutModifier.Element {
    @SuppressLint("ProtoLayoutMinSchema")
    fun mergeTo(initial: Corner.Builder?): Corner.Builder =
        (initial ?: Corner.Builder()).apply {
            cornerRadiusDp?.let { setRadius(cornerRadiusDp.dp) }
            topLeftRadius?.let { setTopLeftRadius(cornerRadius(it.x.value, it.y.value)) }
            topRightRadius?.let { setTopRightRadius(cornerRadius(it.x.value, it.y.value)) }
            bottomLeftRadius?.let { setBottomLeftRadius(cornerRadius(it.x.value, it.y.value)) }
            bottomRightRadius?.let { setBottomRightRadius(cornerRadius(it.x.value, it.y.value)) }
        }
}
