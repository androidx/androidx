/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.text

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlin.jvm.JvmInline

/** Defines how text is aligned horizontally in Remote Compose. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@JvmInline
public value class RemoteTextAlign internal constructor(public val value: Int) {
    override fun toString(): String =
        when (this) {
            Left -> "RemoteTextAlign.Left"
            Right -> "RemoteTextAlign.Right"
            Center -> "RemoteTextAlign.Center"
            Justify -> "RemoteTextAlign.Justify"
            Start -> "RemoteTextAlign.Start"
            End -> "RemoteTextAlign.End"
            else -> "Invalid($value)"
        }

    public companion object {
        /** Align the text on the left edge of the container. */
        @Stable public val Left: RemoteTextAlign = RemoteTextAlign(TextLayout.TEXT_ALIGN_LEFT)

        /** Align the text on the right edge of the container. */
        @Stable public val Right: RemoteTextAlign = RemoteTextAlign(TextLayout.TEXT_ALIGN_RIGHT)

        /** Align the text in the center of the container. */
        @Stable public val Center: RemoteTextAlign = RemoteTextAlign(TextLayout.TEXT_ALIGN_CENTER)

        /** Stretch lines of text that end with a soft line break to fill the container width. */
        @Stable public val Justify: RemoteTextAlign = RemoteTextAlign(TextLayout.TEXT_ALIGN_JUSTIFY)

        /** Align the text on the leading edge of the container. */
        @Stable public val Start: RemoteTextAlign = RemoteTextAlign(TextLayout.TEXT_ALIGN_START)

        /** Align the text on the trailing edge of the container. */
        @Stable public val End: RemoteTextAlign = RemoteTextAlign(TextLayout.TEXT_ALIGN_END)
    }
}
