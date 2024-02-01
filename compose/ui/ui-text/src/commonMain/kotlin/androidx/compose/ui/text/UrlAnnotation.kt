/*
 * Copyright 2022 The Android Open Source Project
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
 * An annotation that contains a url string. When clicking on the text to which this annotation is
 * attached, the app will try to open the url using [androidx.compose.ui.platform.UriHandler].
 */
@ExperimentalTextApi
class UrlAnnotation(val url: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UrlAnnotation) return false
        if (url != other.url) return false
        return true
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

    override fun toString(): String {
        return "UrlAnnotation(url=$url)"
    }
}
