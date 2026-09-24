/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.camera.video.internal.utils

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.util.LruCache
import androidx.annotation.DoNotInline
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.camera.video.internal.encoder.EncoderConfig
import androidx.camera.video.internal.encoder.InvalidConfigException
import java.io.IOException

/** A codec utility class to deal with codec operations. */
public object CodecUtil {

    private const val MAX_CODEC_INFO_CACHE_COUNT = 10

    // A cache from mimeType to MediaCodecInfo.
    // This cache is created because MediaCodec.createEncoderByType() take relatively long time and
    // findCodecAndGetCodecInfo() is being called frequently in camera-video.
    @GuardedBy("codecInfoCache")
    private val codecInfoCache: LruCache<String, MediaCodecInfo> =
        LruCache(MAX_CODEC_INFO_CACHE_COUNT)

    private var allEncoderInfosCache: List<MediaCodecInfo>? = null
    private val allEncoderInfos: List<MediaCodecInfo>
        get() =
            allEncoderInfosCache
                ?: MediaCodecList(MediaCodecList.REGULAR_CODECS)
                    .codecInfos
                    .filter { it.isEncoder }
                    .also { allEncoderInfosCache = it }

    private var allEncoderMimeTypesCache: List<String>? = null
    private val allEncoderMimeTypes: List<String>
        get() =
            allEncoderMimeTypesCache
                ?: allEncoderInfos
                    .flatMap { info -> info.supportedTypes?.map { it.lowercase() } ?: emptyList() }
                    .distinct()
                    .also { allEncoderMimeTypesCache = it }

    private var allVideoEncoderMimeTypesCache: List<String>? = null
    private val allVideoEncoderMimeTypes: List<String>
        get() =
            allVideoEncoderMimeTypesCache
                ?: allEncoderMimeTypes
                    .filter { it.startsWith("video/") }
                    .also { allVideoEncoderMimeTypesCache = it }

    private var allAudioEncoderMimeTypesCache: List<String>? = null
    private val allAudioEncoderMimeTypes: List<String>
        get() =
            allAudioEncoderMimeTypesCache
                ?: allEncoderMimeTypes
                    .filter { it.startsWith("audio/") }
                    .also { allAudioEncoderMimeTypesCache = it }

    /**
     * Creates a codec instance suitable for the encoder config.
     *
     * @throws InvalidConfigException if it fails create the codec.
     */
    @Throws(InvalidConfigException::class)
    @JvmStatic
    public fun createCodec(encoderConfig: EncoderConfig): MediaCodec {
        return createCodec(encoderConfig.mimeType)
    }

    /**
     * Finds and creates a codec info instance suitable for the encoder config.
     *
     * @throws InvalidConfigException if it fails to find or create the codec info.
     */
    @Throws(InvalidConfigException::class)
    @JvmStatic
    public fun findCodecAndGetCodecInfo(mimeType: String): MediaCodecInfo {
        var codecInfo: MediaCodecInfo?

        synchronized(codecInfoCache) { codecInfo = codecInfoCache.get(mimeType) }

        if (codecInfo != null) {
            return codecInfo
        }

        var codec: MediaCodec? = null
        try {
            codec = createCodec(mimeType)
            codecInfo = codec.codecInfo

            synchronized(codecInfoCache) { codecInfoCache.put(mimeType, codecInfo) }
            return codecInfo
        } finally {
            codec?.release()
        }
    }

    @JvmStatic public fun getVideoEncoderMimeTypes(): List<String> = allVideoEncoderMimeTypes

    @JvmStatic public fun getAudioEncoderMimeTypes(): List<String> = allAudioEncoderMimeTypes

    /** Resets the cached codec information. */
    @VisibleForTesting
    public fun reset() {
        allEncoderInfosCache = null
        allEncoderMimeTypesCache = null
        allVideoEncoderMimeTypesCache = null
        allAudioEncoderMimeTypesCache = null
        synchronized(codecInfoCache) { codecInfoCache.evictAll() }
    }

    /**
     * Checks if a [MediaCodecInfo] is hardware-accelerated.
     *
     * On Android 10+ (API 29+), this checks [MediaCodecInfo.isHardwareAccelerated]. Below API 29,
     * this falls back to a heuristic that estimates whether the codec is software-only, adapted
     * from Media3's `androidx/media3/transformer/EncoderUtil.java`.
     *
     * @param codecInfo The codec to check.
     * @param mimeType The MIME type of the stream.
     * @return `true` if the codec is hardware-accelerated, `false` otherwise.
     */
    @JvmStatic
    public fun isHardwareAccelerated(codecInfo: MediaCodecInfo, mimeType: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Api29Impl.isHardwareAccelerated(codecInfo)
        }
        // codecInfo.isHardwareAccelerated() == !codecInfo.isSoftwareOnly() is not necessarily true.
        // However, we assume this to be true as an approximation.
        return !isSoftwareOnly(codecInfo, mimeType)
    }

    private fun isSoftwareOnly(codecInfo: MediaCodecInfo, mimeType: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Api29Impl.isSoftwareOnly(codecInfo)
        }

        if (mimeType.startsWith("audio/", ignoreCase = true)) {
            // Assume audio decoders are software only.
            return true
        }
        val codecName = codecInfo.name.lowercase()
        if (codecName.startsWith("arc.")) {
            // App Runtime for Chrome (ARC) codecs
            return false
        }

        // Estimate whether a codec is software-only, to emulate isSoftwareOnly on API < 29.
        return codecName.startsWith("omx.google.") ||
            codecName.startsWith("omx.ffmpeg.") ||
            (codecName.startsWith("omx.sec.") && codecName.contains(".sw.")) ||
            codecName == "omx.qcom.video.decoder.hevcswvdec" ||
            codecName.startsWith("c2.android.") ||
            codecName.startsWith("c2.google.") ||
            (!codecName.startsWith("omx.") && !codecName.startsWith("c2."))
    }

    @Throws(InvalidConfigException::class)
    private fun createCodec(mimeType: String): MediaCodec {
        return try {
            MediaCodec.createEncoderByType(mimeType)
        } catch (e: IOException) {
            throw InvalidConfigException(e)
        } catch (e: IllegalArgumentException) {
            throw InvalidConfigException(e)
        }
    }

    @RequiresApi(29)
    private object Api29Impl {
        @DoNotInline
        fun isHardwareAccelerated(codecInfo: MediaCodecInfo): Boolean =
            codecInfo.isHardwareAccelerated

        @DoNotInline
        fun isSoftwareOnly(codecInfo: MediaCodecInfo): Boolean = codecInfo.isSoftwareOnly
    }
}
