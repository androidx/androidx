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

package androidx.compose.ui.text.style

import androidx.compose.runtime.Stable

/** How overflowing text should be handled. */
@kotlin.jvm.JvmInline
value class TextOverflow internal constructor(internal val value: Int) {

    override fun toString(): String {
        return when (this) {
            Clip -> "Clip"
            Ellipsis -> "Ellipsis"
            MiddleEllipsis -> "MiddleEllipsis"
            Visible -> "Visible"
            StartEllipsis -> "StartEllipsis"
            else -> "Invalid"
        }
    }

    companion object {
        /**
         * Clip the overflowing text to fix its container.
         *
         * @sample androidx.compose.ui.text.samples.TextOverflowClipSample
         */
        @Stable val Clip = TextOverflow(1)

        /**
         * Use an ellipsis at the end of the string to indicate that the text has overflowed.
         *
         * For example, [This is a ...].
         *
         * @sample androidx.compose.ui.text.samples.TextOverflowEllipsisSample
         */
        @Stable val Ellipsis = TextOverflow(2)

        /**
         * Display all text, even if there is not enough space in the specified bounds. When
         * overflow is visible, text may be rendered outside the bounds of the composable displaying
         * the text. This ensures that all text is displayed to the user, and is typically the right
         * choice for most text display. It does mean that the text may visually occupy a region
         * larger than the bounds of it's composable. This can lead to situations where text
         * displays outside the bounds of the background and clickable on a Text composable with a
         * fixed height and width.
         *
         * @sample androidx.compose.ui.text.samples.TextOverflowVisibleFixedSizeSample
         *
         * To make the background and click region expand to match the size of the text, allow it to
         * expand vertically/horizontally using `Modifier.heightIn`/`Modifier.widthIn` or similar.
         *
         * @sample androidx.compose.ui.text.samples.TextOverflowVisibleMinHeightSample
         *
         * Note: text that expands past its bounds using `Visible` may be clipped by other modifiers
         * such as `Modifier.clipToBounds`.
         */
        @Stable val Visible = TextOverflow(3)

        /**
         * Use an ellipsis at the start of the string to indicate that the text has overflowed.
         *
         * For example, [... is a text].
         *
         * Note that not all platforms support the ellipsis at the start. For example, on Android
         * the start ellipsis is only available for a single line text (i.e. when either a soft wrap
         * is disabled or a maximum number of lines maxLines set to 1). In case of multiline text it
         * will fallback to [Clip].
         */
        @Stable val StartEllipsis = TextOverflow(4)

        /**
         * Use an ellipsis in the middle of the string to indicate that the text has overflowed.
         *
         * For example, [This ... text].
         *
         * Note that not all platforms support the ellipsis in the middle. For example, on Android
         * the middle ellipsis is only available for a single line text (i.e. when either a soft
         * wrap is disabled or a maximum number of lines maxLines set to 1). In case of multiline
         * text it will fallback to [Clip].
         */
        @Stable val MiddleEllipsis = TextOverflow(5)
    }
}
