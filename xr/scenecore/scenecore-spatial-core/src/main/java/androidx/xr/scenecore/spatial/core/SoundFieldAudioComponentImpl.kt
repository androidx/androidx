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

package androidx.xr.scenecore.spatial.core

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import androidx.media3.exoplayer.audio.ForwardingAudioOutputProvider
import androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SoundFieldAudioComponent

internal class SoundFieldAudioComponentImpl(
    context: Context,
    private val audioTrackExtensions: AudioTrackExtensionsWrapper,
    private val soundFieldAttributes: SoundFieldAttributes,
) : SoundFieldAudioComponent {

    private val audioTrackAudioOutputProvider =
        AudioTrackAudioOutputProvider.Builder(context)
            .setAudioTrackBuilderModifier { audioTrackBuilder, config ->
                val audioFormat =
                    AudioFormat.Builder()
                        .setSampleRate(config.sampleRate)
                        .setChannelIndexMask(config.channelMask)
                        .setEncoding(config.encoding)
                        .build()

                audioTrackBuilder.setAudioFormat(audioFormat)
                audioTrackExtensions.setSoundFieldAttributes(
                    audioTrackBuilder,
                    soundFieldAttributes,
                )
            }
            .build()

    private val forwardingProvider =
        object : ForwardingAudioOutputProvider(audioTrackAudioOutputProvider) {
            // TODO: b/486263448 - Upgrade to exoplayer 1.10.0 once its released. The needed
            //   constants are stable in that version.
            @SuppressLint("IllegalExperimentalApiUsage") // This library is still in alpha.
            @OptIn(UnstableApi::class)
            override fun getOutputConfig(
                formatConfig: AudioOutputProvider.FormatConfig
            ): AudioOutputProvider.OutputConfig {
                // Use the right ambisonic channelMask to call getMinBufferSize.
                val channelMask =
                    getAmbisonicAudioTrackChannelConfig(formatConfig.format.channelCount)
                val minBufferSize =
                    AudioTrack.getMinBufferSize(
                        formatConfig.format.sampleRate,
                        channelMask,
                        formatConfig.format.pcmEncoding,
                    )

                // Workaround: Set the preferredBufferSize so that the provider doesn't call
                // getMinBufferSize.
                // The multiplier comes from DefaultAudioTrackBufferSizeProvider.
                val newFormatConfig =
                    formatConfig.buildUpon().setPreferredBufferSize(minBufferSize * 4).build()

                // Set the correct channel mask on the output config.
                return audioTrackAudioOutputProvider
                    .getOutputConfig(newFormatConfig)
                    .buildUpon()
                    .setChannelMask(channelMask)
                    .build()
            }
        }

    override fun getAudioOutputProvider(): AudioOutputProvider {
        return forwardingProvider
    }

    override fun onAttach(entity: Entity): Boolean {
        return true
    }

    override fun onDetach(entity: Entity) {
        return
    }

    private companion object {
        private const val FIRST_ORDER_INDEX_MASK = 0xF
        private const val SECOND_ORDER_INDEX_MASK = 0x1FF
        private const val THIRD_ORDER_INDEX_MASK = 0xFFFF

        /**
         * Note: Which bits are set in the channel mask for Ambisonics don't matter for rendering,
         * only the number of bits. But the platform's AudioTrack#getMinBufferSize() expects valid
         * channel masks with matched L/R pairs, so this returns a valid mask for that method.
         *
         * @return The channel configuration or [AudioFormat.CHANNEL_INVALID] if output is not
         *   possible.
         */
        private fun getAmbisonicAudioTrackChannelConfig(channelCount: Int): Int {
            return when (channelCount) {
                4 -> FIRST_ORDER_INDEX_MASK
                9 -> SECOND_ORDER_INDEX_MASK
                16 -> THIRD_ORDER_INDEX_MASK
                else -> AudioFormat.CHANNEL_INVALID
            }
        }
    }
}
