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

import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.annotation.RestrictTo
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
import androidx.wear.protolayout.types.LayoutString

/** Returns a [LayoutElementMatcher] which checks whether the element is clickable. */
public fun isClickable(): LayoutElementMatcher =
    LayoutElementMatcher("is clickable") { it.modifiers?.clickable != null }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element has the specific [Clickable]
 * attached.
 */
@JvmOverloads
public fun hasClickable(
    action: Action = loadAction(),
    id: String? = null,
    @Dimension(DP) minClickableWidth: Float = Float.NaN,
    @Dimension(DP) minClickableHeight: Float = Float.NaN
): LayoutElementMatcher =
    LayoutElementMatcher("has clickable($action, $id, $minClickableWidth, $minClickableHeight)") {
        val clk = it.modifiers?.clickable ?: return@LayoutElementMatcher false
        if (!minClickableWidth.isNaN() && clk.minimumClickableWidth.value != minClickableWidth) {
            return@LayoutElementMatcher false
        }
        if (!minClickableHeight.isNaN() && clk.minimumClickableHeight.value != minClickableHeight) {
            return@LayoutElementMatcher false
        }
        clk.onClick?.toActionProto() == action.toActionProto() && id?.let { clk.id == it } != false
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element's content description contains
 * the given value.
 *
 * @param value Value to match with content description.
 */
public fun hasContentDescription(value: String): LayoutElementMatcher =
    LayoutElementMatcher("Content description = '$value'") {
        it.contentDescription?.value?.equals(value) ?: false
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element's content description matches
 * the given pattern.
 *
 * @param pattern String pattern to match with content description.
 */
public fun hasContentDescription(pattern: Regex): LayoutElementMatcher =
    LayoutElementMatcher("Content description matches $pattern.") {
        pattern.matches(it.contentDescription?.value ?: "")
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element's metadata tag equals to the
 * given value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun hasTag(value: String): LayoutElementMatcher =
    LayoutElementMatcher("Tag = $value") { it.tag contentEquals value.toByteArray() }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element's metadata tag contains the
 * given value.
 */
public fun containsTag(value: String): LayoutElementMatcher =
    LayoutElementMatcher("Tag = $value") { it.tag?.contains(value.toByteArray()) ?: false }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element's text equals the given value.
 */
public fun hasText(value: LayoutString): LayoutElementMatcher =
    LayoutElementMatcher("Element text = '$value'") {
        it is Text &&
            // TODO: b/375448507 - Add dynamic data evaluation and compare the current string value
            it.text?.toProto()?.value == value.staticValue &&
            it.text?.dynamicValue?.toDynamicStringProto() ==
                value.dynamicValue?.toDynamicStringProto()
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element's text contains the given
 * value.
 *
 * Note that this only checks the static content of the element's text.
 */
@JvmOverloads
public fun hasText(
    value: String,
    subString: Boolean = false,
    ignoreCase: Boolean = false
): LayoutElementMatcher =
    if (subString) {
        LayoutElementMatcher("Element text contains '$value' (ignoreCase: $ignoreCase)") {
            it is Text && it.text?.value?.contains(value, ignoreCase) ?: false
        }
    } else {
        LayoutElementMatcher("Element text = '$value' (ignoreCase: $ignoreCase)") {
            it is Text && it.text?.value?.equals(value, ignoreCase) ?: false
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
    LayoutElementMatcher("Element has image with protolayoutResId = $protolayoutResId") {
        it is Image && it.resourceId?.value.equals(protolayoutResId)
    }

/**
 * Returns a [LayoutElementMatcher] which checks whether the element is drawn with the given color.
 */
public fun hasColor(@ColorInt argb: Int): LayoutElementMatcher =
    LayoutElementMatcher("Element has color $argb ") { it.color?.argb == argb }

/** Returns a [LayoutElementMatcher] which checks whether the element has the given width value. */
public fun hasWidth(width: ContainerDimension): LayoutElementMatcher =
    LayoutElementMatcher("Element has width = $width") {
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
        when (it) {
            is Text,
            is Arc,
            is Spannable -> false
            is Box -> it.width?.toContainerDimensionProto() == widthProto
            is Column -> it.width?.toContainerDimensionProto() == widthProto
            is Row -> it.width?.toContainerDimensionProto() == widthProto
            is Image -> it.width?.toContainerDimensionProtoOrNull() == widthProto
            is Spacer -> it.width?.toContainerDimensionProtoOrNull() == widthProto
            else -> false
        }
    }

/** Returns a [LayoutElementMatcher] which checks whether the element has the given width value. */
public fun hasWidth(width: ProportionalDimensionProp): LayoutElementMatcher =
    LayoutElementMatcher("Element has width = $width") {
        it is Image && it.width?.toImageDimensionProto() == width.toImageDimensionProto()
    }

/** Returns a [LayoutElementMatcher] which checks whether the element has the given height value. */
public fun hasHeight(height: ContainerDimension): LayoutElementMatcher =
    LayoutElementMatcher("Element has height = $height") {
        val heightProto = height.toContainerDimensionProto()
        when (it) {
            is Text,
            is Arc,
            is Spannable -> false
            is Box -> it.height?.toContainerDimensionProto() == heightProto
            is Column -> it.height?.toContainerDimensionProto() == heightProto
            is Row -> it.height?.toContainerDimensionProto() == heightProto
            is Image -> it.height?.toContainerDimensionProtoOrNull() == heightProto
            is Spacer -> it.height?.toContainerDimensionProtoOrNull() == heightProto
            else -> false
        }
    }

/** Returns a [LayoutElementMatcher] which checks whether the element has the given height value. */
public fun hasHeight(height: ProportionalDimensionProp): LayoutElementMatcher =
    LayoutElementMatcher("Element has height = $height") {
        it is Image && it.height?.toImageDimensionProto() == height.toImageDimensionProto()
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
        searchElement(it, matcher) != null
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
