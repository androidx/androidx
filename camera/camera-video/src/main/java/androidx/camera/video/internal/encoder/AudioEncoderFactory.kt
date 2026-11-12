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
 * Creates audio [Encoder]s and reports their capabilities.
 *
 * Callers query the capabilities before creating an encoder, e.g. to resolve the audio settings and
 * the [AudioEncoderConfig]. Reporting them from the factory that creates the encoders keeps them
 * consistent with the created encoders.
 */
public interface AudioEncoderFactory {

    /**
     * Creates an audio [Encoder].
     *
     * @param executor the executor suitable for background tasks.
     * @param encoderConfig the audio encoder config.
     * @throws InvalidConfigException if the encoder can't be created with the config.
     */
    @Throws(InvalidConfigException::class)
    public fun createEncoder(executor: Executor, encoderConfig: AudioEncoderConfig): Encoder

    /**
     * Returns the [AudioEncoderInfo] of the encoder that [createEncoder] creates for [mimeType].
     *
     * @return the encoder info, or `null` if this factory can't encode [mimeType].
     */
    public fun getEncoderInfo(mimeType: String): AudioEncoderInfo?
}
