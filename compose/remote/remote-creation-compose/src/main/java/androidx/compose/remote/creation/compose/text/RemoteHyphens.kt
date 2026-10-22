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
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlin.jvm.JvmInline

/** Configuration for automatic hyphenation in Remote Compose text layout. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@JvmInline
public value class RemoteHyphens internal constructor(internal val value: Int) {
    override fun toString(): String =
        when (this) {
            None -> "RemoteHyphens.None"
            Auto -> "RemoteHyphens.Auto"
            else -> "Invalid($value)"
        }

    public companion object {
        /** Do not automatically break words with hyphens. */
        @Stable public val None: RemoteHyphens = RemoteHyphens(CoreText.HYPHENATION_FREQUENCY_NONE)

        /** Automatically break words with hyphens where appropriate. */
        @Stable public val Auto: RemoteHyphens = RemoteHyphens(CoreText.HYPHENATION_FREQUENCY_FULL)
    }
}
