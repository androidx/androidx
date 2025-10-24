/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.camera.video

import android.util.Range
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.camera.core.AspectRatio
import java.util.Objects

/** Video specification that provides options to configure video encoding. */
@RestrictTo(Scope.LIBRARY)
public class VideoSpec
@JvmOverloads
public constructor(
    public val qualitySelector: QualitySelector = QUALITY_SELECTOR_AUTO,
    public val encodeFrameRate: Int = ENCODE_FRAME_RATE_AUTO,
    public val bitrate: Range<Int> = BITRATE_RANGE_AUTO,
    @get:AspectRatio.Ratio public val aspectRatio: Int = AspectRatio.RATIO_DEFAULT,
) {

    /** Returns a [Builder] instance with the same property values as this instance. */
    public fun toBuilder(): Builder {
        return Builder()
            .setQualitySelector(qualitySelector)
            .setEncodeFrameRate(encodeFrameRate)
            .setBitrate(bitrate)
            .setAspectRatio(aspectRatio)
    }

    public override fun toString(): String {
        return "VideoSpec{" +
            "qualitySelector=$qualitySelector, " +
            "encodeFrameRate=$encodeFrameRate, " +
            "bitrate=$bitrate, " +
            "aspectRatio=$aspectRatio" +
            "}"
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VideoSpec) return false
        return qualitySelector == other.qualitySelector &&
            encodeFrameRate == other.encodeFrameRate &&
            bitrate == other.bitrate &&
            aspectRatio == other.aspectRatio
    }

    public override fun hashCode(): Int {
        return Objects.hash(qualitySelector, encodeFrameRate, bitrate, aspectRatio)
    }

    /** The builder of the [VideoSpec]. */
    @RestrictTo(Scope.LIBRARY)
    public class Builder {
        private var qualitySelector: QualitySelector = QUALITY_SELECTOR_AUTO
        private var encodeFrameRate: Int = ENCODE_FRAME_RATE_AUTO
        private var bitrate: Range<Int> = BITRATE_RANGE_AUTO
        private var aspectRatio: Int = AspectRatio.RATIO_DEFAULT

        /**
         * Sets the [QualitySelector].
         *
         * If not set, defaults to [QUALITY_SELECTOR_AUTO].
         */
        public fun setQualitySelector(qualitySelector: QualitySelector): Builder = apply {
            this.qualitySelector = qualitySelector
        }

        /**
         * Sets the encode frame rate.
         *
         * If not set, defaults to [ENCODE_FRAME_RATE_AUTO].
         */
        public fun setEncodeFrameRate(frameRate: Int): Builder = apply {
            this.encodeFrameRate = frameRate
        }

        /**
         * Sets the bitrate.
         *
         * If not set, defaults to [BITRATE_RANGE_AUTO].
         */
        public fun setBitrate(bitrate: Range<Int>): Builder = apply { this.bitrate = bitrate }

        /**
         * Sets the aspect ratio.
         *
         * If not set, defaults to [AspectRatio.RATIO_DEFAULT].
         */
        public fun setAspectRatio(@AspectRatio.Ratio aspectRatio: Int): Builder = apply {
            this.aspectRatio = aspectRatio
        }

        /** Builds the VideoSpec instance. */
        public fun build(): VideoSpec {
            return VideoSpec(qualitySelector, encodeFrameRate, bitrate, aspectRatio)
        }
    }

    public companion object {
        /** Frame rate representing no preference for encode frame rate. */
        public const val ENCODE_FRAME_RATE_AUTO: Int = 0

        /** Bitrate range representing no preference for bitrate. */
        @JvmField public val BITRATE_RANGE_AUTO: Range<Int> = Range(0, Integer.MAX_VALUE)

        /** Quality selector representing no preference for quality. */
        @JvmField
        public val QUALITY_SELECTOR_AUTO: QualitySelector =
            QualitySelector.fromOrderedList(
                listOf(Quality.FHD, Quality.HD, Quality.SD),
                FallbackStrategy.higherQualityOrLowerThan(Quality.FHD),
            )

        /** Returns a build for this config. */
        @RestrictTo(Scope.LIBRARY) @JvmStatic public fun builder(): Builder = Builder()
    }
}
