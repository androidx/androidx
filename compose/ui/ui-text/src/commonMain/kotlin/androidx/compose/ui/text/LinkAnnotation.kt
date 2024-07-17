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
 *
 * @sample androidx.compose.ui.text.samples.AnnotatedStringWithLinkSample
 */
abstract class LinkAnnotation private constructor() : AnnotatedString.Annotation {
    /**
     * Interaction listener triggered when user interacts with this link.
     *
     * @sample androidx.compose.ui.text.samples.AnnotatedStringWithListenerSample
     */
    abstract val linkInteractionListener: LinkInteractionListener?
    /**
     * Style configuration for this link in different states.
     *
     * @sample androidx.compose.ui.text.samples.AnnotatedStringWithHoveredLinkStylingSample
     */
    abstract val styles: TextLinkStyles?

    /**
     * An annotation that contains a [url] string. When clicking on the text to which this
     * annotation is attached, the app will try to open the url using
     * [androidx.compose.ui.platform.UriHandler]. However, if [linkInteractionListener] is provided,
     * its [LinkInteractionListener.onClick] method will be called instead and so you need to then
     * handle opening url manually (for example by calling
     * [androidx.compose.ui.platform.UriHandler]).
     *
     * @see LinkAnnotation
     */
    class Url(
        val url: String,
        override val styles: TextLinkStyles? = null,
        override val linkInteractionListener: LinkInteractionListener? = null
    ) : LinkAnnotation() {

        /** Returns a copy of this [Url], optionally overriding some of the values. */
        @Suppress("ExecutorRegistration")
        fun copy(
            url: String = this.url,
            styles: TextLinkStyles? = this.styles,
            linkInteractionListener: LinkInteractionListener? = this.linkInteractionListener
        ) = Url(url, styles, linkInteractionListener)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Url) return false

            if (url != other.url) return false
            if (styles != other.styles) return false
            if (linkInteractionListener != other.linkInteractionListener) return false

            return true
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + (styles?.hashCode() ?: 0)
            result = 31 * result + (linkInteractionListener?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "LinkAnnotation.Url(url=$url)"
        }
    }

    /**
     * An annotation that contains a clickable marked with [tag]. When clicking on the text to which
     * this annotation is attached, the app will trigger a [linkInteractionListener] listener.
     *
     * @see LinkAnnotation
     */
    class Clickable(
        val tag: String,
        override val styles: TextLinkStyles? = null,
        // nullable for the save/restore purposes
        override val linkInteractionListener: LinkInteractionListener?
    ) : LinkAnnotation() {

        /** Returns a copy of this [Clickable], optionally overriding some of the values. */
        @Suppress("ExecutorRegistration")
        fun copy(
            tag: String = this.tag,
            styles: TextLinkStyles? = this.styles,
            linkInteractionListener: LinkInteractionListener? = this.linkInteractionListener
        ) = Clickable(tag, styles, linkInteractionListener)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Clickable) return false

            if (tag != other.tag) return false
            if (styles != other.styles) return false
            if (linkInteractionListener != other.linkInteractionListener) return false

            return true
        }

        override fun hashCode(): Int {
            var result = tag.hashCode()
            result = 31 * result + (styles?.hashCode() ?: 0)
            result = 31 * result + (linkInteractionListener?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "LinkAnnotation.Clickable(tag=$tag)"
        }
    }
}
