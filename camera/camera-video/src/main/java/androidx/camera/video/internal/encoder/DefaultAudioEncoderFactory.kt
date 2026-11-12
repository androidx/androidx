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

package androidx.camera.video.internal.encoder

import androidx.camera.core.Logger
import androidx.camera.video.internal.utils.CodecUtil.findCodecAndGetCodecInfo
import java.util.concurrent.Executor

/**
 * The default [AudioEncoderFactory], which creates [EncoderImpl] instances.
 *
 * [getEncoderInfo] reports the capabilities of the same codec that [createEncoder] creates for a
 * MIME type.
 */
public class DefaultAudioEncoderFactory private constructor() : AudioEncoderFactory {

    @Throws(InvalidConfigException::class)
    override fun createEncoder(executor: Executor, encoderConfig: AudioEncoderConfig): Encoder =
        EncoderImpl(executor, encoderConfig)

    override fun getEncoderInfo(mimeType: String): AudioEncoderInfo? =
        try {
            AudioEncoderInfoImpl(findCodecAndGetCodecInfo(mimeType), mimeType)
        } catch (e: InvalidConfigException) {
            Logger.w(TAG, "Unable to find an AudioEncoderInfo for $mimeType", e)
            null
        }

    public companion object {
        private const val TAG = "DefaultAudioEncoderFactory"

        /** The default audio encoder factory. */
        @JvmField public val DEFAULT: DefaultAudioEncoderFactory = DefaultAudioEncoderFactory()
    }
}
