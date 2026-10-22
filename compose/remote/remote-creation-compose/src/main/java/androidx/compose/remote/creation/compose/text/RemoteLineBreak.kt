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

/** Configuration for breaking lines in Remote Compose text layout. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@JvmInline
public value class RemoteLineBreak internal constructor(internal val value: Int) {
    override fun toString(): String =
        when (this) {
            Simple -> "RemoteLineBreak.Simple"
            Paragraph -> "RemoteLineBreak.Paragraph"
            Heading -> "RemoteLineBreak.Heading"
            else -> "Invalid($value)"
        }

    public companion object {
        /** Basic, fast line breaking suitable for input fields. */
        @Stable public val Simple: RemoteLineBreak = RemoteLineBreak(CoreText.BREAK_STRATEGY_SIMPLE)

        /** Slower, higher-quality line breaking suitable for body paragraphs. */
        @Stable
        public val Paragraph: RemoteLineBreak =
            RemoteLineBreak(CoreText.BREAK_STRATEGY_HIGH_QUALITY)

        /** Balanced line lengths suitable for short headings. */
        @Stable
        public val Heading: RemoteLineBreak = RemoteLineBreak(CoreText.BREAK_STRATEGY_BALANCED)
    }
}
