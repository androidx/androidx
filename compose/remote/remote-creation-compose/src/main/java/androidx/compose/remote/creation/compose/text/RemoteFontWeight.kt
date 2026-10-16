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

/** Represents the thickness of glyphs in a font for Remote Compose. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@JvmInline
public value class RemoteFontWeight(public val weight: Int) : Comparable<RemoteFontWeight> {
    override fun compareTo(other: RemoteFontWeight): Int = weight.compareTo(other.weight)

    override fun toString(): String = "RemoteFontWeight($weight)"

    public companion object {
        @Stable public val W100: RemoteFontWeight = RemoteFontWeight(100)
        @Stable public val W200: RemoteFontWeight = RemoteFontWeight(200)
        @Stable public val W300: RemoteFontWeight = RemoteFontWeight(300)
        @Stable public val W400: RemoteFontWeight = RemoteFontWeight(400)
        @Stable public val W500: RemoteFontWeight = RemoteFontWeight(500)
        @Stable public val W600: RemoteFontWeight = RemoteFontWeight(600)
        @Stable public val W700: RemoteFontWeight = RemoteFontWeight(700)
        @Stable public val W800: RemoteFontWeight = RemoteFontWeight(800)
        @Stable public val W900: RemoteFontWeight = RemoteFontWeight(900)

        @Stable public val Thin: RemoteFontWeight = W100
        @Stable public val ExtraLight: RemoteFontWeight = W200
        @Stable public val Light: RemoteFontWeight = W300
        @Stable public val Normal: RemoteFontWeight = W400
        @Stable public val Medium: RemoteFontWeight = W500
        @Stable public val SemiBold: RemoteFontWeight = W600
        @Stable public val Bold: RemoteFontWeight = W700
        @Stable public val ExtraBold: RemoteFontWeight = W800
        @Stable public val Black: RemoteFontWeight = W900
    }
}
