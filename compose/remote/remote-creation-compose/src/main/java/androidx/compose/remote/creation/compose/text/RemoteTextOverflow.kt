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

/** How visual text overflow should be handled in Remote Compose. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@JvmInline
public value class RemoteTextOverflow internal constructor(public val value: Int) {
    override fun toString(): String =
        when (this) {
            Clip -> "RemoteTextOverflow.Clip"
            Ellipsis -> "RemoteTextOverflow.Ellipsis"
            Visible -> "RemoteTextOverflow.Visible"
            StartEllipsis -> "RemoteTextOverflow.StartEllipsis"
            MiddleEllipsis -> "RemoteTextOverflow.MiddleEllipsis"
            else -> "Invalid($value)"
        }

    public companion object {
        /** Clip the overflowing text to fix its container. */
        @Stable public val Clip: RemoteTextOverflow = RemoteTextOverflow(TextLayout.OVERFLOW_CLIP)

        /** Use an ellipsis at the end to indicate that the text has overflowed. */
        @Stable
        public val Ellipsis: RemoteTextOverflow = RemoteTextOverflow(TextLayout.OVERFLOW_ELLIPSIS)

        /** Display all text, even if there is not enough space in the specified bounds. */
        @Stable
        public val Visible: RemoteTextOverflow = RemoteTextOverflow(TextLayout.OVERFLOW_VISIBLE)

        /** Use an ellipsis at the start to indicate that the text has overflowed. */
        @Stable
        public val StartEllipsis: RemoteTextOverflow =
            RemoteTextOverflow(TextLayout.OVERFLOW_START_ELLIPSIS)

        /** Use an ellipsis in the middle to indicate that the text has overflowed. */
        @Stable
        public val MiddleEllipsis: RemoteTextOverflow =
            RemoteTextOverflow(TextLayout.OVERFLOW_MIDDLE_ELLIPSIS)
    }
}
