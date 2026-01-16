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

/**
 * AudioEncoderInfoImpl provides audio encoder related information and capabilities.
 *
 * The implementation wraps and queries [MediaCodecInfo] relevant capability classes such as
 * [MediaCodecInfo.CodecCapabilities], [MediaCodecInfo.EncoderCapabilities] and
 * [MediaCodecInfo.AudioCapabilities].
 */
public class AudioEncoderInfoImpl
@Throws(InvalidConfigException::class)
internal constructor(codecInfo: MediaCodecInfo, mime: String) :
    EncoderInfoImpl(codecInfo, mime), AudioEncoderInfo {
    private val audioCapabilities: MediaCodecInfo.AudioCapabilities =
        codecCapabilities.audioCapabilities!!

    override val bitrateRange: Range<Int>
        get() = audioCapabilities.bitrateRange
}
