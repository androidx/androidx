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

import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundEffect
import androidx.xr.scenecore.runtime.SoundEffectPoolComponent
import androidx.xr.scenecore.runtime.Stream

internal class SoundEffectPoolComponentImpl(soundEffectPool: SoundEffectPoolImpl) :
    SoundEffectPoolComponent {

    private val soundEffectPlayer = soundEffectPool.getSoundEffectPlayer()

    override fun onAttach(entity: Entity): Boolean {
        return entity is AndroidXrEntity
    }

    override fun onDetach(entity: Entity) {
        // No-op
    }

    override fun play(
        soundEffect: SoundEffect,
        pointSourceParams: PointSourceParams,
        entity: Entity?,
        volume: Float,
        priority: Int,
        isLooping: Boolean,
    ): Stream {

        val stream =
            soundEffectPlayer.play(
                soundEffect,
                pointSourceParams,
                entity,
                volume,
                priority,
                isLooping,
            )

        return stream
    }

    override fun pause(stream: Stream) {
        soundEffectPlayer.pause(stream)
    }

    override fun resume(stream: Stream) {
        soundEffectPlayer.resume(stream)
    }

    override fun stop(stream: Stream) {
        soundEffectPlayer.stop(stream)
    }

    override fun setVolume(stream: Stream, volume: Float) {
        soundEffectPlayer.setVolume(stream, volume)
    }

    override fun setLooping(stream: Stream, isLooping: Boolean) {
        soundEffectPlayer.setLooping(stream, isLooping)
    }
}
