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

import android.content.Context
import android.media.AudioTrack
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioTrackAudioOutput
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import androidx.media3.exoplayer.audio.ForwardingAudioOutputProvider
import androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.PositionalAudioComponent

internal class PositionalAudioComponentImpl(
    context: Context,
    private val audioTrackExtensions: AudioTrackExtensionsWrapper,
    private var params: PointSourceParams,
) : PositionalAudioComponent {

    private var audioTrack: AudioTrack? = null

    private var attachedEntity: Entity? = null

    internal val audioTrackAudioOutputProvider =
        AudioTrackAudioOutputProvider.Builder(context)
            .setAudioTrackBuilderModifier { audioTrackBuilder, _ ->
                audioTrackExtensions.setPointSourceParams(audioTrackBuilder, params, attachedEntity)
            }
            .build()

    internal val forwardingProvider =
        object : ForwardingAudioOutputProvider(audioTrackAudioOutputProvider) {
            override fun getAudioOutput(config: AudioOutputProvider.OutputConfig): AudioOutput {
                val audioTrackOutput =
                    audioTrackAudioOutputProvider.getAudioOutput(config) as AudioTrackAudioOutput
                audioTrack = audioTrackOutput.audioTrack
                return audioTrackOutput
            }
        }

    override fun getAudioOutputProvider(): AudioOutputProvider {
        return forwardingProvider
    }

    override fun setPointSourceParams(params: PointSourceParams) {
        this.params = params

        // If the track hasn't been created yet then the params will be set by the builder modifier.
        audioTrack?.let { audioTrackExtensions.setPointSourceParams(it, params, attachedEntity) }
    }

    override fun onAttach(entity: Entity): Boolean {
        if (entity !is AndroidXrEntity) {
            return false
        }
        attachedEntity = entity

        audioTrack?.let { audioTrackExtensions.setPointSourceParams(it, params, attachedEntity) }

        return true
    }

    override fun onDetach(entity: Entity) {
        if (entity is AndroidXrEntity && entity == attachedEntity) {
            audioTrack?.let { audioTrackExtensions.setPointSourceParams(it, params, null) }
        }
        attachedEntity = null
        return
    }
}
