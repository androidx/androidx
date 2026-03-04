/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.camera.video.internal.encoder

import android.media.MediaCodecInfo
import android.util.Range
import androidx.camera.core.Logger
import androidx.camera.video.internal.utils.CodecUtil.findCodecAndGetCodecInfo
import androidx.camera.video.internal.workaround.ProfileAwareVideoEncoderInfo

/**
 * VideoEncoderInfoImpl provides video encoder related information and capabilities.
 *
 * The implementation wraps and queries [MediaCodecInfo] relevant capability classes such as
 * [MediaCodecInfo.CodecCapabilities], [MediaCodecInfo.EncoderCapabilities] and
 * [MediaCodecInfo.VideoCapabilities].
 */
public class VideoEncoderInfoImpl
@Throws(InvalidConfigException::class)
internal constructor(codecInfo: MediaCodecInfo, mime: String) :
    EncoderInfoImpl(codecInfo, mime), VideoEncoderInfo {
    private val videoCapabilities: MediaCodecInfo.VideoCapabilities =
        codecCapabilities.videoCapabilities!!

    override fun canSwapWidthHeight(): Boolean {
        /*
         * The capability to swap width and height is saved in media_codecs.xml with key
         * "can-swap-width-height". But currently there is no API to query it. See
         * b/314694668#comment4.
         * By experimentation, most default codecs found by MediaCodec.createEncoderByType(), allow
         * swapping width and height.
         * SupportedQualitiesVerificationTest#qualityOptionCanRecordVideo_enableSurfaceProcessor
         * should verify it to an extent. We leave it returns true until we have a way to know the
         * capability. If we get a "false" case, we may have to add a quirk for now.
         */
        return true
    }

    override fun isSizeSupported(width: Int, height: Int): Boolean {
        return videoCapabilities.isSizeSupported(width, height)
    }

    override fun getSupportedWidths(): Range<Int> {
        return videoCapabilities.supportedWidths
    }

    override fun getSupportedHeights(): Range<Int> {
        return videoCapabilities.supportedHeights
    }

    override fun getSupportedWidthsFor(height: Int): Range<Int> {
        return try {
            videoCapabilities.getSupportedWidthsFor(height)
        } catch (t: Throwable) {
            throw toIllegalArgumentException(t)
        }
    }

    override fun getSupportedHeightsFor(width: Int): Range<Int> {
        return try {
            videoCapabilities.getSupportedHeightsFor(width)
        } catch (t: Throwable) {
            throw toIllegalArgumentException(t)
        }
    }

    override val widthAlignment: Int
        get() = videoCapabilities.widthAlignment

    override val heightAlignment: Int
        get() = videoCapabilities.heightAlignment

    override val supportedBitrateRange: Range<Int>
        get() = videoCapabilities.bitrateRange

    public companion object {
        private const val TAG = "VideoEncoderInfoImpl"

        /**
         * A default implementation of the VideoEncoderInfoImpl finder.
         *
         * The function will return `null` if it can't find a VideoEncoderInfoImpl.
         */
        @JvmField
        public val FINDER: VideoEncoderInfo.Finder =
            VideoEncoderInfo.Finder { mimeType: String ->
                try {
                    val videoEncoderInfo =
                        VideoEncoderInfoImpl(findCodecAndGetCodecInfo(mimeType), mimeType)
                    return@Finder ProfileAwareVideoEncoderInfo.from(videoEncoderInfo)
                } catch (e: InvalidConfigException) {
                    Logger.w(TAG, "Unable to find a VideoEncoderInfoImpl", e)
                    return@Finder null
                }
            }

        private fun toIllegalArgumentException(t: Throwable): IllegalArgumentException {
            return t as? IllegalArgumentException ?: IllegalArgumentException(t)
        }
    }
}
