/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.testapp.common

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

/**
 * Checks if the device has a decoder that supports the MV-HEVC format.
 *
 * This function iterates through the available media codecs on the device and checks for a decoder
 * that supports the MV-HEVC Mime Type.
 *
 * @return `true` if an MV-HEVC decoder is found, `false` otherwise.
 */
@OptIn(UnstableApi::class) // for MimeTypes.VIDEO_MV_HEVC and normalizeMimeType usage
fun isMvHevcSupported(): Boolean {
    val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
    for (codecInfo in mediaCodecList.codecInfos) {
        // We only care about decoders
        if (codecInfo.isEncoder) {
            continue
        }

        val types = codecInfo.supportedTypes
        for (type in types) {
            if (
                MimeTypes.normalizeMimeType(type).equals(MimeTypes.VIDEO_MV_HEVC, ignoreCase = true)
            ) {
                return true
            }
        }
    }
    // No decoder found that supports the MV-HEVC profile.
    return false
}

@OptIn(UnstableApi::class) // For MimeTypes like VP9
/**
 * Checks if the device supports Widevine DRM and has a secure decoder.
 *
 * This prevents crashes on devices that might report partial DRM support but lack the secure
 * rendering path required for SurfaceEntity.SurfaceProtection.PROTECTED.
 */
public fun isDrmSupported(): Boolean {
    // 1. Check if the Widevine scheme is supported by the device.
    if (!android.media.MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID)) {
        return false
    }

    // 2. Check if a secure decoder is available.
    // For example, the emulator might support the scheme (L3) but fail to create a protected
    // surface if no secure decoder is present.
    val mimeTypesToCheck =
        listOf(
            MimeTypes.VIDEO_H264,
            MimeTypes.VIDEO_H265,
            MimeTypes.VIDEO_VP9,
            MimeTypes.VIDEO_AV1,
            MimeTypes.VIDEO_MP4,
        )
    val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)

    for (mimeType in mimeTypesToCheck) {
        for (info in mediaCodecList.codecInfos) {
            if (info.isEncoder) continue
            try {
                val caps = info.getCapabilitiesForType(mimeType)
                if (
                    caps != null &&
                        caps.isFeatureSupported(
                            MediaCodecInfo.CodecCapabilities.FEATURE_SecurePlayback
                        )
                ) {
                    return true
                }
            } catch (e: IllegalArgumentException) {
                // MIME type not supported by this codec, continue searching.
            }
        }
    }
    return false
}
