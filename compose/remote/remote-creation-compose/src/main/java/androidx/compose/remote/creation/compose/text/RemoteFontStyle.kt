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

/** Defines whether a font should be styled as normal or italic in Remote Compose. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@JvmInline
public value class RemoteFontStyle internal constructor(public val value: Int) {
    override fun toString(): String =
        when (this) {
            Normal -> "RemoteFontStyle.Normal"
            Italic -> "RemoteFontStyle.Italic"
            else -> "Invalid($value)"
        }

    public companion object {
        /** Use the upright glyphs. */
        @Stable public val Normal: RemoteFontStyle = RemoteFontStyle(0)

        /** Use glyphs designed for slanting. */
        @Stable public val Italic: RemoteFontStyle = RemoteFontStyle(1)
    }
}
