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

package androidx.camera.video.internal.compat.quirk

import android.annotation.SuppressLint
import android.media.MediaFormat.MIMETYPE_VIDEO_AV1
import android.os.Build
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: 576542786
 * - Description: Quirk denoting video encoder MIME types that are reported by
 *   [android.media.MediaCodecList] but cannot be used for camera recording on the device. On Pixel
 *   5 and Samsung SM-A042M, the only AV1 encoder is the software `c2.android.av1.encoder`, which
 *   rejects every camera Surface input buffer (`Unrecognized plane type` / `input is not YUV420`)
 *   and fails the recording with `MediaCodec` error `0x16` (`EINVAL`). Forcing surface processing
 *   makes the encoder accept the input, but the encoder cannot sustain real-time encoding on these
 *   devices (heavy frame drops), so AV1 is reported as unsupported instead.
 * - Device(s): Pixel 5, Samsung SM-A042M
 */
@SuppressLint("CameraXQuirksClassDetector")
public object VideoEncoderMimeTypeUnsupportedQuirk : Quirk {

    @JvmStatic public fun load(): Boolean = isPixel5 || isSamsungSmA042m

    /** Returns the video MIME types that must not be used for recording on this device. */
    @JvmStatic
    public fun getUnsupportedMimeTypes(): Set<String> =
        if (isPixel5 || isSamsungSmA042m) setOf(MIMETYPE_VIDEO_AV1) else emptySet()

    private val isPixel5: Boolean
        get() =
            "google".equals(Build.BRAND, ignoreCase = true) &&
                "redfin".equals(Build.DEVICE, ignoreCase = true)

    private val isSamsungSmA042m: Boolean
        get() =
            "samsung".equals(Build.BRAND, ignoreCase = true) &&
                "SM-A042M".equals(Build.MODEL, ignoreCase = true)
}
