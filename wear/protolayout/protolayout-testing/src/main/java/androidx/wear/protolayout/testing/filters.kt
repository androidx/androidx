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
@file:JvmName("Filters")

package androidx.wear.protolayout.testing

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.wear.protolayout.ActionBuilders.Action
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension
import androidx.wear.protolayout.DimensionBuilders.DpProp
import androidx.wear.protolayout.DimensionBuilders.ExpandedDimensionProp
import androidx.wear.protolayout.DimensionBuilders.ImageDimension
import androidx.wear.protolayout.DimensionBuilders.ProportionalDimensionProp
import androidx.wear.protolayout.DimensionBuilders.SpacerDimension
import androidx.wear.protolayout.LayoutElementBuilders.Arc
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Spannable
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.modifiers.loadAction
import androidx.wear.protolayout.proto.DimensionProto

/** Returns a [LayoutElementMatcher] which checks whether the element is clickable. */
public fun isClickable(): LayoutElementMatcher =
    LayoutElementMatcher("is clickable") { element -> element.modifiers?.clickable != null }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element has the specific [Clickable]
 * attached.
 */
@JvmOverloads
public fun hasClickable(
    action: Action = loadAction(),
    id: String? = null,
    @Dimension(DP) minClickableWidth: Float = Float.NaN,
    @Dimension(DP) minClickableHeight: Float = Float.NaN,
): LayoutElementMatcher =
    LayoutElementMatcher("has clickable($action, $id, $minClickableWidth, $minClickableHeight)") {
        element ->
        val clk = element.modifiers?.clickable ?: return@LayoutElementMatcher false
        if (!minClickableWidth.isNaN() && clk.minimumClickableWidth.value != minClickableWidth) {
            return@LayoutElementMatcher false
        }
        if (!minClickableHeight.isNaN() && clk.minimumClickableHeight.value != minClickableHeight) {
            return@LayoutElementMatcher false
        }
        clk.onClick?.toActionProto() == action.toActionProto() &&
            id?.let { id -> clk.id == id } != false
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element's content description contains
 * the given value.
 *
 * @param value Value to match with content description.
 */
public fun hasContentDescription(value: String): LayoutElementMatcher =
    LayoutElementMatcher("Content description = '$value'") { element, context ->
        element.getContentDescription(context.dynamicData)?.equals(value) == true
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element's content description matches
 * the given pattern.
 *
 * @param pattern String pattern to match with content description.
 */
public fun hasContentDescription(pattern: Regex): LayoutElementMatcher =
    LayoutElementMatcher("Content description matches $pattern.") { element, context ->
        element.getContentDescription(context.dynamicData)?.let { pattern.matches(it) } ?: false
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element is marked as heading for
 * accessibility purpose.
 */
public fun isSemanticsHeading(): LayoutElementMatcher =
    LayoutElementMatcher("Element is semantics heading.") { element ->
        element.modifiers?.semantics?.isHeading ?: false
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element's metadata tag equals to the
 * given value.
 */
public fun hasTag(value: String): LayoutElementMatcher =
    LayoutElementMatcher("Tag = $value") { element ->
        element.tag contentEquals value.toByteArray()
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element's metadata tag contains the
 * given value.
 */
public fun containsTag(value: String): LayoutElementMatcher =
    LayoutElementMatcher("Tag = $value") { element ->
        element.tag?.contains(value.toByteArray()) == true
    }

/* Returns a [LayoutElementMatcher] which checks whether the element's text contains the given
 * value.
 *
 * @param value Value to match with the element's text.
 * @param subString Whether to use subString matching.
 * @param ignoreCase Whether case should be ignored.
 */
@JvmOverloads
public fun hasText(
    value: String,
    subString: Boolean = false,
    ignoreCase: Boolean = false,
): LayoutElementMatcher =
    if (subString) {
        LayoutElementMatcher("Element text contains '$value' (ignoreCase: $ignoreCase)") {
            element,
            context ->
            element.getText(context.dynamicData)?.contains(value, ignoreCase) == true
        }
    } else {
        LayoutElementMatcher("Element has text = '$value' (ignoreCase: $ignoreCase)") {
            element,
            context ->
            element.getText(context.dynamicData)?.equals(value, ignoreCase) == true
        }
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element displays an image with the
 * given protolayout resource id.
 *
 * @param protolayoutResId The resource id of the image, which should be a protolayout resource id
 *   instead of android resource id.
 */
public fun hasImage(protolayoutResId: String): LayoutElementMatcher =
    LayoutElementMatcher("Element has image with protolayoutResId = $protolayoutResId") { element ->
        element is Image && element.resourceId?.value.equals(protolayoutResId)
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element is drawn with the given color.
 */
public fun hasColor(@ColorInt argb: Int): LayoutElementMatcher =
    LayoutElementMatcher("Element has color $argb ") { element, context ->
        val dynamicValue: Color? = element.color?.dynamicValue?.evaluate(context.dynamicData)
        if (dynamicValue != null) {
            dynamicValue.toArgb() == argb
        } else {
            element.color?.argb == argb
        }
    }

/** Returns a [LayoutElementMatcher] which checks whether the element has the given width value. */
public fun hasWidth(width: ContainerDimension): LayoutElementMatcher =
    LayoutElementMatcher("Element has width = $width") { element ->
        val widthProto = width.toContainerDimensionProto()

        /*
          width & height for different type of LayoutElement:
            Text -> N/A, decided by text style, line_height etc
            Image -> ImageDimension(DpProp, ExpandedDimensionProp, ProportionalDimensionProp)
            Spacer -> SpacerDimension(DpProp, ExpandedDimensionProp)
            Box, Column, Row ->
                ContainerDimension(DpProp, ExpandedDimensionProp, WrappedDimensionProp)
            Arc -> N/A, fits inside its parent container
            Spannable -> N/A, decided by spannable element
        */
        when (element) {
            is Text,
            is Arc,
            is Spannable -> false
            is Box -> element.width?.toContainerDimensionProto() == widthProto
            is Column -> element.width?.toContainerDimensionProto() == widthProto
            is Row -> element.width?.toContainerDimensionProto() == widthProto
            is Image -> element.width?.toContainerDimensionProtoOrNull() == widthProto
            is Spacer -> element.width?.toContainerDimensionProtoOrNull() == widthProto
            else -> false
        }
    }

/** Returns a [LayoutElementMatcher] which checks whether the element has the given width value. */
public fun hasWidth(width: ProportionalDimensionProp): LayoutElementMatcher =
    LayoutElementMatcher("Element has width = $width") { element ->
        element is Image && element.width?.toImageDimensionProto() == width.toImageDimensionProto()
    }

/** Returns a [LayoutElementMatcher] which checks whether the element has the given height value. */
public fun hasHeight(height: ContainerDimension): LayoutElementMatcher =
    LayoutElementMatcher("Element has height = $height") { element ->
        val heightProto = height.toContainerDimensionProto()
        when (element) {
            is Text,
            is Arc,
            is Spannable -> false
            is Box -> element.height?.toContainerDimensionProto() == heightProto
            is Column -> element.height?.toContainerDimensionProto() == heightProto
            is Row -> element.height?.toContainerDimensionProto() == heightProto
            is Image -> element.height?.toContainerDimensionProtoOrNull() == heightProto
            is Spacer -> element.height?.toContainerDimensionProtoOrNull() == heightProto
            else -> false
        }
    }

/** Returns a [LayoutElementMatcher] which checks whether the element has the given height value. */
public fun hasHeight(height: ProportionalDimensionProp): LayoutElementMatcher =
    LayoutElementMatcher("Element has height = $height") { element ->
        element is Image &&
            element.height?.toImageDimensionProto() == height.toImageDimensionProto()
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element has a child matching the given
 * matcher.
 */
public fun hasChild(matcher: LayoutElementMatcher): LayoutElementMatcher =
    LayoutElementMatcher("Element has one of its child which is an ${matcher.description}") {
        element ->
        element.children.any { matcher.matches(it) }
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element has a descendant matches the
 * given matcher.
 */
public fun hasDescendant(matcher: LayoutElementMatcher): LayoutElementMatcher =
    LayoutElementMatcher("Element has one of its descendant which is an ${matcher.description}") {
        element ->
        searchElement(element, matcher) != null
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element has all its four corners with
 * the given radius.
 */
public fun hasAllCorners(radiusDp: Float): LayoutElementMatcher =
    hasTopLeftCorner(radiusDp, radiusDp) and
        hasTopRightCorner(radiusDp, radiusDp) and
        hasBottomLeftCorner(radiusDp, radiusDp) and
        hasBottomRightCorner(radiusDp, radiusDp)

/**
 * Returns a [LayoutElementMatcher] which checks whether the element has its top left corner with
 * the given radii.
 */
public fun hasTopLeftCorner(xRadiusDp: Float, yRadiusDp: Float): LayoutElementMatcher =
    LayoutElementMatcher(
        "Element has its top left corner with radius of {$xRadiusDp, $yRadiusDp}"
    ) { element ->
        val cornerRadius = element.modifiers?.background?.corner?.topLeftRadius
        cornerRadius?.let { it.x.value == xRadiusDp && it.y.value == yRadiusDp } == true
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element has its top right corner with
 * the given radii.
 */
public fun hasTopRightCorner(xRadiusDp: Float, yRadiusDp: Float): LayoutElementMatcher =
    LayoutElementMatcher(
        "Element has its top right corner with radius of {$xRadiusDp, $yRadiusDp}"
    ) { element ->
        val cornerRadius = element.modifiers?.background?.corner?.topRightRadius
        cornerRadius?.let { it.x.value == xRadiusDp && it.y.value == yRadiusDp } == true
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element has its bottom left corner with
 * the given radii.
 */
public fun hasBottomLeftCorner(xRadiusDp: Float, yRadiusDp: Float): LayoutElementMatcher =
    LayoutElementMatcher(
        "Element has its bottom left corner with radius of {$xRadiusDp, $yRadiusDp}"
    ) { element ->
        val cornerRadius = element.modifiers?.background?.corner?.bottomLeftRadius
        cornerRadius?.let { it.x.value == xRadiusDp && it.y.value == yRadiusDp } == true
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element has its bottom right corners
 * with the given radii.
 */
public fun hasBottomRightCorner(xRadiusDp: Float, yRadiusDp: Float): LayoutElementMatcher =
    LayoutElementMatcher(
        "Element has its bottom right corner with radius of {$xRadiusDp, $yRadiusDp}"
    ) { element ->
        val cornerRadius = element.modifiers?.background?.corner?.bottomRightRadius
        cornerRadius?.let { it.x.value == xRadiusDp && it.y.value == yRadiusDp } == true
    }

private operator fun ByteArray.contains(subset: ByteArray): Boolean {
    if (subset.size > this.size) return false
    for (i in 0..(this.size - subset.size)) {
        val slicedArray = slice(i until i + subset.size).toByteArray()
        if (subset contentEquals slicedArray) {
            return true
        }
    }
    return false
}

private fun ImageDimension.toContainerDimensionProtoOrNull(): DimensionProto.ContainerDimension? =
    when (this) {
        is DpProp -> this.toContainerDimensionProto()
        is ExpandedDimensionProp -> this.toContainerDimensionProto()
        is ProportionalDimensionProp -> null
        else -> throw AssertionError("Unknown ImageDimension Type")
    }

private fun SpacerDimension.toContainerDimensionProtoOrNull(): DimensionProto.ContainerDimension? =
    when (this) {
        is DpProp -> this.toContainerDimensionProto()
        is ExpandedDimensionProp -> this.toContainerDimensionProto()
        else -> throw AssertionError("Unknown SpacerDimension Type")
    }
