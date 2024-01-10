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

package androidx.compose.foundation.content

import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * Type identifier for contents that are transferable between applications or processes. Although
 * mime type format was standardized, each platform is free to choose how they define media types.
 * Therefore, this class has an expect modifier for different platforms to specify how they define
 * certain common media types like Text, and Image.
 */
@ExperimentalFoundationApi
expect class MediaType(representation: String) {

    /**
     * How this [MediaType] is represented in a specific platform.
     */
    val representation: String

    @ExperimentalFoundationApi
    companion object {

        /**
         * Any type of text, html, stylized, or plain.
         */
        val Text: MediaType

        /**
         * Plain text that's only decoded from its raw representation, does not define or carry
         * any annotations.
         */
        val PlainText: MediaType

        /**
         * Text that represents an HTML content.
         */
        val HtmlText: MediaType

        /**
         * Any type of image like PNG, JPEG, or GIFs.
         */
        val Image: MediaType

        /**
         * Matches all content types.
         */
        val All: MediaType
    }
}
