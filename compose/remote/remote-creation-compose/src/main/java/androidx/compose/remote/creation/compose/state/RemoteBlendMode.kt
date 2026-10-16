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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Immutable

/** Blend mode algorithms used to determine how source and destination pixels are combined. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@kotlin.jvm.JvmInline
public value class RemoteBlendMode internal constructor(public val value: Int) {
    override fun toString(): String =
        when (this) {
            Clear -> "Clear"
            Src -> "Src"
            Dst -> "Dst"
            SrcOver -> "SrcOver"
            DstOver -> "DstOver"
            SrcIn -> "SrcIn"
            DstIn -> "DstIn"
            SrcOut -> "SrcOut"
            DstOut -> "DstOut"
            SrcAtop -> "SrcAtop"
            DstAtop -> "DstAtop"
            Xor -> "Xor"
            Plus -> "Plus"
            Modulate -> "Modulate"
            Screen -> "Screen"
            Overlay -> "Overlay"
            Darken -> "Darken"
            Lighten -> "Lighten"
            ColorDodge -> "ColorDodge"
            ColorBurn -> "ColorBurn"
            Hardlight -> "Hardlight"
            Softlight -> "Softlight"
            Difference -> "Difference"
            Exclusion -> "Exclusion"
            Multiply -> "Multiply"
            Hue -> "Hue"
            Saturation -> "Saturation"
            Color -> "Color"
            Luminosity -> "Luminosity"
            else -> "Unknown"
        }

    public companion object {
        public val Clear: RemoteBlendMode = RemoteBlendMode(0)
        public val Src: RemoteBlendMode = RemoteBlendMode(1)
        public val Dst: RemoteBlendMode = RemoteBlendMode(2)
        public val SrcOver: RemoteBlendMode = RemoteBlendMode(3)
        public val DstOver: RemoteBlendMode = RemoteBlendMode(4)
        public val SrcIn: RemoteBlendMode = RemoteBlendMode(5)
        public val DstIn: RemoteBlendMode = RemoteBlendMode(6)
        public val SrcOut: RemoteBlendMode = RemoteBlendMode(7)
        public val DstOut: RemoteBlendMode = RemoteBlendMode(8)
        public val SrcAtop: RemoteBlendMode = RemoteBlendMode(9)
        public val DstAtop: RemoteBlendMode = RemoteBlendMode(10)
        public val Xor: RemoteBlendMode = RemoteBlendMode(11)
        public val Plus: RemoteBlendMode = RemoteBlendMode(12)
        public val Modulate: RemoteBlendMode = RemoteBlendMode(13)
        public val Screen: RemoteBlendMode = RemoteBlendMode(14)
        public val Overlay: RemoteBlendMode = RemoteBlendMode(15)
        public val Darken: RemoteBlendMode = RemoteBlendMode(16)
        public val Lighten: RemoteBlendMode = RemoteBlendMode(17)
        public val ColorDodge: RemoteBlendMode = RemoteBlendMode(18)
        public val ColorBurn: RemoteBlendMode = RemoteBlendMode(19)
        public val Hardlight: RemoteBlendMode = RemoteBlendMode(20)
        public val Softlight: RemoteBlendMode = RemoteBlendMode(21)
        public val Difference: RemoteBlendMode = RemoteBlendMode(22)
        public val Exclusion: RemoteBlendMode = RemoteBlendMode(23)
        public val Multiply: RemoteBlendMode = RemoteBlendMode(24)
        public val Hue: RemoteBlendMode = RemoteBlendMode(25)
        public val Saturation: RemoteBlendMode = RemoteBlendMode(26)
        public val Color: RemoteBlendMode = RemoteBlendMode(27)
        public val Luminosity: RemoteBlendMode = RemoteBlendMode(28)
    }
}
