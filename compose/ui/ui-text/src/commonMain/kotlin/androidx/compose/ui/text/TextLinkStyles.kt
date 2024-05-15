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

package androidx.compose.ui.text

import androidx.compose.runtime.Immutable

/**
 * Represents the styles of the links in the [AnnotatedString] in different states
 *
 * These style objects will be applied to every [LinkAnnotation] annotation present in the
 * [AnnotatedString], overriding any styling that might be already present in the [AnnotatedString]
 * at the [LinkAnnotation]'s position.
 *
 * If null is passed to the style argument, it means that a [LinkAnnotation] representing a link
 * will not get a specific link styling for this state. Instead it will be styled according to the
 * rest of the [AnnotatedString].
 *
 * The resulting style of the link is always a combination of all styles merged into one in
 * the order `style.merge(focusedStyle).merge(hoveredStyle).merge(pressedStyle)`.
 *
 * @param style style configuration for a link that is always applied
 * @param focusedStyle style configuration for a link applied on top of the [style] when the link
 * is focused
 * @param hoveredStyle style configuration for a link applied on top of the [style] when the link
 * is hovered
 * @param pressedStyle style configuration for a link applied on top of the [style] when the link
 * is pressed
 */
@Immutable
class TextLinkStyles(
    val style: SpanStyle? = null,
    val focusedStyle: SpanStyle? = null,
    val hoveredStyle: SpanStyle? = null,
    val pressedStyle: SpanStyle? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is TextLinkStyles) return false

        if (style != other.style) return false
        if (focusedStyle != other.focusedStyle) return false
        if (hoveredStyle != other.hoveredStyle) return false
        if (pressedStyle != other.pressedStyle) return false

        return true
    }

    override fun hashCode(): Int {
        var result = style?.hashCode() ?: 0
        result = 31 * result + (focusedStyle?.hashCode() ?: 0)
        result = 31 * result + (hoveredStyle?.hashCode() ?: 0)
        result = 31 * result + (pressedStyle?.hashCode() ?: 0)
        return result
    }
}
