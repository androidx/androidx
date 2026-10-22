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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlin.jvm.JvmInline

/** Defines a horizontal line to be drawn on Remote Compose text. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@JvmInline
public value class RemoteTextDecoration private constructor(private val mask: Int) {
    /** Creates a decoration that includes all the decorations in both this and [decoration]. */
    public operator fun plus(decoration: RemoteTextDecoration): RemoteTextDecoration =
        RemoteTextDecoration(mask or decoration.mask)

    /** Check whether this [RemoteTextDecoration] contains the given [other] decoration. */
    public operator fun contains(other: RemoteTextDecoration): Boolean =
        (mask or other.mask) == mask

    override fun toString(): String {
        if (mask == 0) {
            return "RemoteTextDecoration.None"
        }
        return when (mask and (Underline.mask or LineThrough.mask)) {
            Underline.mask -> "RemoteTextDecoration.Underline"
            LineThrough.mask -> "RemoteTextDecoration.LineThrough"
            Underline.mask or LineThrough.mask -> "RemoteTextDecoration[Underline, LineThrough]"
            else -> "RemoteTextDecoration[]"
        }
    }

    public companion object {
        /** Do not draw a decoration. */
        @Stable public val None: RemoteTextDecoration = RemoteTextDecoration(0x0)

        /** Draws a horizontal line below the text. */
        @Stable public val Underline: RemoteTextDecoration = RemoteTextDecoration(0x1)

        /** Draws a horizontal line over the text. */
        @Stable public val LineThrough: RemoteTextDecoration = RemoteTextDecoration(0x2)
    }
}
