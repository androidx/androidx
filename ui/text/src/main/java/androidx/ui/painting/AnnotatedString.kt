/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.painting

/**
 * The basic data structure of text with multiple styles.
 */
data class AnnotatedString(
    val text: String,
    val textStyles: List<Item<TextStyle>> = listOf()
) {
    /**
     * The information attached on the text such as a TextStyle.
     *
     * @param style The style being applied on this part of [AnnotatedString].
     * @param start The start of the range where [style] takes effect. It's inclusive.
     * @param end The end of the range where [style] takes effect. It's exclusive.
     */
    // TODO(haoyuchang): Check some other naming options.
    data class Item<T>(val style: T, val start: Int, val end: Int)
}