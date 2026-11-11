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

import java.util.concurrent.Executor

/**
 * A fake [AudioEncoderFactory].
 *
 * @param encoderInfoProvider provides the encoder info reported for a MIME type.
 * @param onCreateEncoder creates the encoder.
 */
class FakeAudioEncoderFactory(
    private val encoderInfoProvider: (String) -> AudioEncoderInfo? = { FakeAudioEncoderInfo() },
    private val onCreateEncoder: (Executor, AudioEncoderConfig) -> Encoder,
) : AudioEncoderFactory {

    override fun createEncoder(executor: Executor, encoderConfig: AudioEncoderConfig): Encoder =
        onCreateEncoder(executor, encoderConfig)

    override fun getEncoderInfo(mimeType: String): AudioEncoderInfo? = encoderInfoProvider(mimeType)
}
