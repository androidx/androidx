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

/** Specifies how to handle overflowing text. */
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
         * Clips overflowing text to fit its container.
         *
         * @sample androidx.compose.ui.text.samples.TextOverflowClipSample
         */
        @Stable val Clip = TextOverflow(1)

        /**
         * Displays an ellipsis at the end of the line to indicate overflow.
         *
         * For example, "This is a ...".
         *
         * @sample androidx.compose.ui.text.samples.TextOverflowEllipsisSample
         */
        @Stable val Ellipsis = TextOverflow(2)

        /**
         * Displays all text, even if it exceeds the specified bounds.
         *
         * Text may render outside the composable bounds. To allow the container to expand with the
         * text, use modifiers like `Modifier.heightIn` or `Modifier.widthIn`.
         *
         * @sample androidx.compose.ui.text.samples.TextOverflowVisibleFixedSizeSample
         * @sample androidx.compose.ui.text.samples.TextOverflowVisibleMinHeightSample
         *
         * Note: Text expanding past its bounds may still be clipped by modifiers like
         * `Modifier.clipToBounds`.
         */
        @Stable val Visible = TextOverflow(3)

        /**
         * Displays an ellipsis at the start of the line.
         *
         * For example, "... is a text".
         *
         * Note: On Android, this falls back to [Clip] for multiline text (only supported for single
         * line or maxLines=1).
         */
        @Stable val StartEllipsis = TextOverflow(4)

        /**
         * Displays an ellipsis in the middle of the line.
         *
         * For example, "This ... text".
         *
         * Note: On Android, this falls back to [Clip] for multiline text (only supported for single
         * line or maxLines=1).
         */
        @Stable val MiddleEllipsis = TextOverflow(5)
    }
}
