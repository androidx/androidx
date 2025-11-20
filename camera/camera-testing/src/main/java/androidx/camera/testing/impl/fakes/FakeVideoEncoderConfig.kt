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

package androidx.camera.testing.impl.fakes

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Size
import androidx.camera.core.impl.Timebase
import androidx.camera.video.internal.encoder.VideoEncoderConfig

private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
private const val BIT_RATE = 10 * 1024 * 1024 // 10M
private const val COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
private const val FRAME_RATE = 30
private const val I_FRAME_INTERVAL = 1
private val RESOLUTION = Size(640, 480)

public fun createFakeVideoEncoderConfig(): VideoEncoderConfig =
    VideoEncoderConfig.builder()
        .setInputTimebase(Timebase.UPTIME)
        .setBitrate(BIT_RATE)
        .setColorFormat(COLOR_FORMAT)
        .setCaptureFrameRate(FRAME_RATE)
        .setEncodeFrameRate(FRAME_RATE)
        .setIFrameInterval(I_FRAME_INTERVAL)
        .setMimeType(MIME_TYPE)
        .setResolution(RESOLUTION)
        .build()
