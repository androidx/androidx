/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.engine.text

/**
 * Whether a [TextPosition] is visually upstream or downstream of its offset.
 *
 * For example, when a text position exists at a line break, a single offset has
 * two visual positions, one prior to the line break (at the end of the first
 * line) and one after the line break (at the start of the second line). A text
 * affinity disambiguates between those cases. (Something similar happens with
 * between runs of bidirectional text.)
 */
enum class TextAffinity {
    // TODO(Migration/siyamed): Afaik we do not currently have support for affinity

    /**
     * The position has affinity for the upstream side of the text position.
     *
     * For example, if the offset of the text position is a line break, the
     * position represents the end of the first line.
     */
    upstream,

    /**
     * The position has affinity for the downstream side of the text position.
     *
     * For example, if the offset of the text position is a line break, the
     * position represents the start of the second line.
     */
    downstream,
}