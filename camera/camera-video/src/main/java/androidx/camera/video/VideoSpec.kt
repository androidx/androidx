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

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.camera.core.AspectRatio
import androidx.camera.video.MediaConstants.MIME_TYPE_UNSPECIFIED
import java.util.Objects

/** Video specification that provides options to configure video encoding. */
@RestrictTo(Scope.LIBRARY)
public class VideoSpec
@JvmOverloads
public constructor(
    public val qualitySelector: QualitySelector = QUALITY_SELECTOR_UNSPECIFIED,
    public val encodeFrameRate: Int = ENCODE_FRAME_RATE_UNSPECIFIED,
    public val bitrate: Int = BITRATE_UNSPECIFIED,
    @get:AspectRatio.Ratio public val aspectRatio: Int = AspectRatio.RATIO_DEFAULT,
    public val mimeType: String = MIME_TYPE_UNSPECIFIED,
) {

    /** Returns a [Builder] instance with the same property values as this instance. */
    public fun toBuilder(): Builder {
        return Builder()
            .setQualitySelector(qualitySelector)
            .setEncodeFrameRate(encodeFrameRate)
            .setBitrate(bitrate)
            .setAspectRatio(aspectRatio)
            .setMimeType(mimeType)
    }

    public override fun toString(): String {
        return "VideoSpec{" +
            "qualitySelector=$qualitySelector, " +
            "encodeFrameRate=$encodeFrameRate, " +
            "bitrate=$bitrate, " +
            "aspectRatio=$aspectRatio, " +
            "mimeType=$mimeType" +
            "}"
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VideoSpec) return false
        return qualitySelector == other.qualitySelector &&
            encodeFrameRate == other.encodeFrameRate &&
            bitrate == other.bitrate &&
            aspectRatio == other.aspectRatio &&
            mimeType == other.mimeType
    }

    public override fun hashCode(): Int {
        return Objects.hash(qualitySelector, encodeFrameRate, bitrate, aspectRatio, mimeType)
    }

    /** The builder of the [VideoSpec]. */
    @RestrictTo(Scope.LIBRARY)
    public class Builder {
        private var qualitySelector: QualitySelector = QUALITY_SELECTOR_UNSPECIFIED
        private var encodeFrameRate: Int = ENCODE_FRAME_RATE_UNSPECIFIED
        private var bitrate: Int = BITRATE_UNSPECIFIED
        private var aspectRatio: Int = AspectRatio.RATIO_DEFAULT
        private var mimeType: String = MIME_TYPE_UNSPECIFIED

        /**
         * Sets the [QualitySelector].
         *
         * If not set, defaults to [QUALITY_SELECTOR_UNSPECIFIED].
         */
        public fun setQualitySelector(qualitySelector: QualitySelector): Builder = apply {
            this.qualitySelector = qualitySelector
        }

        /**
         * Sets the encode frame rate.
         *
         * If not set, defaults to [ENCODE_FRAME_RATE_UNSPECIFIED].
         */
        public fun setEncodeFrameRate(frameRate: Int): Builder = apply {
            this.encodeFrameRate = frameRate
        }

        /**
         * Sets the bitrate.
         *
         * If not set, defaults to [BITRATE_UNSPECIFIED].
         */
        public fun setBitrate(bitrate: Int): Builder = apply { this.bitrate = bitrate }

        /**
         * Sets the aspect ratio.
         *
         * If not set, defaults to [AspectRatio.RATIO_DEFAULT].
         */
        public fun setAspectRatio(@AspectRatio.Ratio aspectRatio: Int): Builder = apply {
            this.aspectRatio = aspectRatio
        }

        /**
         * Sets the MIME type.
         *
         * If not set, defaults to [MIME_TYPE_UNSPECIFIED].
         */
        public fun setMimeType(mimeType: String): Builder = apply { this.mimeType = mimeType }

        /** Builds the VideoSpec instance. */
        public fun build(): VideoSpec {
            return VideoSpec(qualitySelector, encodeFrameRate, bitrate, aspectRatio, mimeType)
        }
    }

    public companion object {
        /** Frame rate representing no preference for encode frame rate. */
        public const val ENCODE_FRAME_RATE_UNSPECIFIED: Int = 0

        /** No preference for bitrate. */
        public const val BITRATE_UNSPECIFIED: Int = 0

        /** Quality selector representing no preference for quality. */
        public val QUALITY_SELECTOR_UNSPECIFIED: QualitySelector = QualitySelector.NONE

        /** A [VideoSpec] representing the default video configuration. */
        public val DEFAULT: VideoSpec = builder().build()

        /** Returns a build for this config. */
        @RestrictTo(Scope.LIBRARY) @JvmStatic public fun builder(): Builder = Builder()
    }
}
