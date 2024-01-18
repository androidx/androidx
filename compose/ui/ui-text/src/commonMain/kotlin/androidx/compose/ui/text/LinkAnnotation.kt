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
 * Disclaimer: This is a no-op at the moment. Continue using [UrlAnnotation] to make your links
 * visible for the accessibility services like Talkback
 */
abstract class LinkAnnotation private constructor() {
    /**
     * An annotation that contains a url string. When clicking on the text to which this annotation
     * is attached, the app will try to open the url using [androidx.compose.ui.platform.UriHandler].
     */
    class Url(val url: String) : LinkAnnotation() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Url) return false
            return url == other.url
        }

        override fun hashCode(): Int {
            return url.hashCode()
        }

        override fun toString(): String {
            return "LinkAnnotation.Url(url=$url)"
        }
    }

    /**
     * An annotation that contains a clickable marked with [tag]. When clicking on the text to
     * which this annotation is attached, the app will trigger a
     * [androidx.compose.foundation.TextLinkClickHandler.onClick] callback.
     */
    class Clickable(val tag: String) : LinkAnnotation() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Clickable) return false
            return tag == other.tag
        }

        override fun hashCode(): Int {
            return tag.hashCode()
        }

        override fun toString(): String {
            return "LinkAnnotation.Clickable(tag=$tag)"
        }
    }
}
