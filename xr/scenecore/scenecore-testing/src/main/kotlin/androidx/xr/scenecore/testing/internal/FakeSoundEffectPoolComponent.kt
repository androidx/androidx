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

package androidx.xr.scenecore.testing.internal

import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundEffect
import androidx.xr.scenecore.runtime.SoundEffectPoolComponent
import androidx.xr.scenecore.runtime.Stream

/** Test-only implementation of [SoundEffectPoolComponent]. */
internal class FakeSoundEffectPoolComponent : FakeComponent(), SoundEffectPoolComponent {
    var lastPlayedSoundEffect: SoundEffect? = null
    var lastPlayedParams: PointSourceParams? = null
    var lastPlayedEntity: Entity? = null
    var lastPlayedVolume: Float? = null
    var lastPlayedPriority: Int? = null
    var lastPlayedIsLooping: Boolean? = null
    var lastPausedStream: Stream? = null
    var lastResumedStream: Stream? = null
    var lastStoppedStream: Stream? = null
    var lastSetVolumeStream: Stream? = null
    var lastSetVolumeVolume: Float? = null
    var lastSetLoopingStream: Stream? = null
    var lastSetLoopingIsLooping: Boolean? = null

    override fun play(
        soundEffect: SoundEffect,
        pointSourceParams: PointSourceParams,
        entity: Entity?,
        volume: Float,
        priority: Int,
        isLooping: Boolean,
    ): Stream {
        lastPlayedSoundEffect = soundEffect
        lastPlayedParams = pointSourceParams
        lastPlayedEntity = entity
        lastPlayedVolume = volume
        lastPlayedPriority = priority
        lastPlayedIsLooping = isLooping
        return Stream(1)
    }

    override fun pause(stream: Stream) {
        lastPausedStream = stream
    }

    override fun resume(stream: Stream) {
        lastResumedStream = stream
    }

    override fun stop(stream: Stream) {
        lastStoppedStream = stream
    }

    override fun setVolume(stream: Stream, volume: Float) {
        lastSetVolumeStream = stream
        lastSetVolumeVolume = volume
    }

    override fun setLooping(stream: Stream, isLooping: Boolean) {
        lastSetLoopingStream = stream
        lastSetLoopingIsLooping = isLooping
    }
}
