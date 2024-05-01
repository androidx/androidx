/*
 * Copyright 2023 The Android Open Source Project
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

/**
 * An annotation that represents a clickable part of the text.
 */
abstract class LinkAnnotation private constructor() {
    /** Interaction listener triggered when user interacts with this link. */
    abstract val linkInteractionListener: LinkInteractionListener?
    /**
     * Style configuration for this link that is always applied
     */
    abstract val style: SpanStyle?
    /**
     * Style configuration for this link applied on top of the [style] when the link is focused.
     *
     * The resulting style of the link is always a combination of all styles merged into one in
     * the order `style.merge(focusedStyle).merge(hoveredStyle).merge(pressedStyle)`
     */
    abstract val focusedStyle: SpanStyle?
    /**
     * Style configuration for this link applied on top of the [style] when the link is hovered.
     *
     * The resulting style of the link is always a combination of all styles merged into one in
     * the order `style.merge(focusedStyle).merge(hoveredStyle).merge(pressedStyle)`
     */
    abstract val hoveredStyle: SpanStyle?
    /**
     * Style configuration for this link applied on top of the [style] when the link is pressed.
     *
     * The resulting style of the link is always a combination of all styles merged into one in
     * the order `style.merge(focusedStyle).merge(hoveredStyle).merge(pressedStyle)`
     */
    abstract val pressedStyle: SpanStyle?
    /**
     * Returns a new [LinkAnnotation] which styles are a combination of its original styles and
     * the given default styles.
     *
     * This link's style's null or inherit properties are replaced with the non-null properties of
     * the corresponding default style. Another way to think of it is that the "missing" properties
     * of the style are _filled_ by the properties of the corresponding default style.
     */
    abstract fun withDefaultsFrom(
        defaultStyle: SpanStyle?,
        defaultFocusedStyle: SpanStyle?,
        defaultHoveredStyle: SpanStyle?,
        defaultPressedStyle: SpanStyle?
    ): LinkAnnotation

    /**
     * An annotation that contains a [url] string. When clicking on the text to which this annotation
     * is attached, the app will try to open the url using [androidx.compose.ui.platform.UriHandler].
     * However, if [linkInteractionListener] is provided, its [LinkInteractionListener.onClick]
     * method will be called instead and so you need to then handle opening url manually (for
     * example by calling [androidx.compose.ui.platform.UriHandler]).
     */
    class Url(
        val url: String,
        override val style: SpanStyle? = null,
        override val focusedStyle: SpanStyle? = null,
        override val hoveredStyle: SpanStyle? = null,
        override val pressedStyle: SpanStyle? = null,
        override val linkInteractionListener: LinkInteractionListener? = null
    ) : LinkAnnotation() {

        override fun withDefaultsFrom(
            defaultStyle: SpanStyle?,
            defaultFocusedStyle: SpanStyle?,
            defaultHoveredStyle: SpanStyle?,
            defaultPressedStyle: SpanStyle?
        ) = Url(
            url = this.url,
            style = defaultStyle?.merge(style) ?: this.style,
            focusedStyle = defaultFocusedStyle?.merge(this.focusedStyle) ?: this.focusedStyle,
            hoveredStyle = defaultHoveredStyle?.merge(this.hoveredStyle) ?: this.hoveredStyle,
            pressedStyle = defaultPressedStyle?.merge(this.pressedStyle) ?: this.pressedStyle,
            linkInteractionListener = this.linkInteractionListener
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Url) return false

            if (url != other.url) return false
            if (style != other.style) return false
            if (focusedStyle != other.focusedStyle) return false
            if (hoveredStyle != other.hoveredStyle) return false
            if (pressedStyle != other.pressedStyle) return false
            if (linkInteractionListener != other.linkInteractionListener) return false

            return true
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + (style?.hashCode() ?: 0)
            result = 31 * result + (focusedStyle?.hashCode() ?: 0)
            result = 31 * result + (hoveredStyle?.hashCode() ?: 0)
            result = 31 * result + (pressedStyle?.hashCode() ?: 0)
            result = 31 * result + (linkInteractionListener?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "LinkAnnotation.Url(url=$url)"
        }
    }

    /**
     * An annotation that contains a clickable marked with [tag]. When clicking on the text to
     * which this annotation is attached, the app will trigger a [linkInteractionListener] listener.
     */
    class Clickable(
        val tag: String,
        // nullable for the save/restore purposes
        override val style: SpanStyle? = null,
        override val focusedStyle: SpanStyle? = null,
        override val hoveredStyle: SpanStyle? = null,
        override val pressedStyle: SpanStyle? = null,
        override val linkInteractionListener: LinkInteractionListener?
    ) : LinkAnnotation() {

        override fun withDefaultsFrom(
            defaultStyle: SpanStyle?,
            defaultFocusedStyle: SpanStyle?,
            defaultHoveredStyle: SpanStyle?,
            defaultPressedStyle: SpanStyle?
        ) = Clickable(
            tag = this.tag,
            style = defaultStyle?.merge(style) ?: this.style,
            focusedStyle = defaultFocusedStyle?.merge(this.focusedStyle) ?: this.focusedStyle,
            hoveredStyle = defaultHoveredStyle?.merge(this.hoveredStyle) ?: this.hoveredStyle,
            pressedStyle = defaultPressedStyle?.merge(this.pressedStyle) ?: this.pressedStyle,
            linkInteractionListener = this.linkInteractionListener
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Clickable) return false

            if (tag != other.tag) return false
            if (style != other.style) return false
            if (focusedStyle != other.focusedStyle) return false
            if (hoveredStyle != other.hoveredStyle) return false
            if (pressedStyle != other.pressedStyle) return false
            if (linkInteractionListener != other.linkInteractionListener) return false

            return true
        }

        override fun hashCode(): Int {
            var result = tag.hashCode()
            result = 31 * result + (style?.hashCode() ?: 0)
            result = 31 * result + (focusedStyle?.hashCode() ?: 0)
            result = 31 * result + (hoveredStyle?.hashCode() ?: 0)
            result = 31 * result + (pressedStyle?.hashCode() ?: 0)
            result = 31 * result + (linkInteractionListener?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "LinkAnnotation.Clickable(tag=$tag)"
        }
    }
}
