/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.text

/**
 * The settings used to specify how an inline element is vertically aligned within a text line.
 * @see InlineElementMetrics
 */
enum class TextVerticalAlign {
    /** Align the bottom of the inline element with the baseline. */
    AboveBaseline,
    /** Align the top of the inline element with the top of the entire line. */
    Top,
    /** Align the bottom of the inline element with the bottom of the entire line. */
    Bottom,
    /** Align the center of the inline element with the center of the entire line. */
    Center,
    /**
     *  Align the top of the inline element with the top of the proceeding text.
     *  It is different from the [Top] when there are texts with different font size, font or other
     *  styles in the same line. This option will use the proceeding text's top instead of the
     *  whole line's top.
     */
    TextTop,
    /**
     * Align the bottom of the inline element with the bottom of the proceeding text.
     * It is different from the [TextBottom] when there are texts with different font size, font or
     * other styles in the same line. This option will use the proceeding text's bottom instead of
     * the whole line's bottom.
     */
    TextBottom,
    /**
     * Align the center of the inline element with the center of the proceeding text.
     * It is different from the [Center] when there are texts with different font size, font or
     * other styles in the same line. This option will use the proceeding text's center instead of
     * the whole line's center.
     */
    TextCenter,
}