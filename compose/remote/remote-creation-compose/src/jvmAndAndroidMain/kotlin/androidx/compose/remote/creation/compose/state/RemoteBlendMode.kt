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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlin.jvm.JvmInline

/** Blend mode algorithms used to determine how source and destination pixels are combined. */
@Immutable
@JvmInline
public value class RemoteBlendMode internal constructor(internal val value: Int) {
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
        /** Drop both the source and destination images, leaving nothing. */
        @Stable public val Clear: RemoteBlendMode = RemoteBlendMode(0)

        /** Drop the destination image, only paint the source image. */
        @Stable public val Src: RemoteBlendMode = RemoteBlendMode(1)

        /** Drop the source image, only paint the destination image. */
        @Stable public val Dst: RemoteBlendMode = RemoteBlendMode(2)

        /** Composite the source image over the destination image. */
        @Stable public val SrcOver: RemoteBlendMode = RemoteBlendMode(3)

        /** Composite the source image under the destination image. */
        @Stable public val DstOver: RemoteBlendMode = RemoteBlendMode(4)

        /** Show the source image, but only where it overlaps the destination image. */
        @Stable public val SrcIn: RemoteBlendMode = RemoteBlendMode(5)

        /** Show the destination image, but only where it overlaps the source image. */
        @Stable public val DstIn: RemoteBlendMode = RemoteBlendMode(6)

        /** Show the source image, but only where it does not overlap the destination image. */
        @Stable public val SrcOut: RemoteBlendMode = RemoteBlendMode(7)

        /** Show the destination image, but only where it does not overlap the source image. */
        @Stable public val DstOut: RemoteBlendMode = RemoteBlendMode(8)

        /**
         * Composite the source image over the destination image, only where it overlaps the
         * destination.
         */
        @Stable public val SrcAtop: RemoteBlendMode = RemoteBlendMode(9)

        /**
         * Composite the destination image over the source image, only where it overlaps the source.
         */
        @Stable public val DstAtop: RemoteBlendMode = RemoteBlendMode(10)

        /** Apply a bitwise XOR operator to the source and destination images. */
        @Stable public val Xor: RemoteBlendMode = RemoteBlendMode(11)

        /** Sum the components of the source and destination images. */
        @Stable public val Plus: RemoteBlendMode = RemoteBlendMode(12)

        /** Multiply the color components of the source and destination images. */
        @Stable public val Modulate: RemoteBlendMode = RemoteBlendMode(13)

        /**
         * Multiply the inverse components of the source and destination images, and invert the
         * result.
         */
        @Stable public val Screen: RemoteBlendMode = RemoteBlendMode(14)

        /**
         * Multiply the components of the source and destination images after adjusting them to
         * favor the destination.
         */
        @Stable public val Overlay: RemoteBlendMode = RemoteBlendMode(15)

        /**
         * Composite the source and destination image by choosing the lowest value from each color
         * channel.
         */
        @Stable public val Darken: RemoteBlendMode = RemoteBlendMode(16)

        /**
         * Composite the source and destination image by choosing the highest value from each color
         * channel.
         */
        @Stable public val Lighten: RemoteBlendMode = RemoteBlendMode(17)

        /** Divide the destination by the inverse of the source. */
        @Stable public val ColorDodge: RemoteBlendMode = RemoteBlendMode(18)

        /** Divide the inverse of the destination by the source, and invert the result. */
        @Stable public val ColorBurn: RemoteBlendMode = RemoteBlendMode(19)

        /**
         * Multiply the components of the source and destination images after adjusting them to
         * favor the source.
         */
        @Stable public val Hardlight: RemoteBlendMode = RemoteBlendMode(20)

        /** Use ColorDodge for source values below 0.5 and ColorBurn for source values above 0.5. */
        @Stable public val Softlight: RemoteBlendMode = RemoteBlendMode(21)

        /** Subtract the smaller value from the bigger value for each channel. */
        @Stable public val Difference: RemoteBlendMode = RemoteBlendMode(22)

        /** Subtract double the product of the two images from the sum of the two images. */
        @Stable public val Exclusion: RemoteBlendMode = RemoteBlendMode(23)

        /**
         * Multiply the components of the source and destination images, including the alpha
         * channel.
         */
        @Stable public val Multiply: RemoteBlendMode = RemoteBlendMode(24)

        /**
         * Take the hue of the source image, and the saturation and luminosity of the destination
         * image.
         */
        @Stable public val Hue: RemoteBlendMode = RemoteBlendMode(25)

        /**
         * Take the saturation of the source image, and the hue and luminosity of the destination
         * image.
         */
        @Stable public val Saturation: RemoteBlendMode = RemoteBlendMode(26)

        /**
         * Take the hue and saturation of the source image, and the luminosity of the destination
         * image.
         */
        @Stable public val Color: RemoteBlendMode = RemoteBlendMode(27)

        /**
         * Take the luminosity of the source image, and the hue and saturation of the destination
         * image.
         */
        @Stable public val Luminosity: RemoteBlendMode = RemoteBlendMode(28)
    }
}
