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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundEffect
import androidx.xr.scenecore.runtime.SoundEffectPoolComponent
import androidx.xr.scenecore.runtime.Stream

/** Test-only implementation of [SoundEffectPoolComponent]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSoundEffectPoolComponent : FakeComponent(), SoundEffectPoolComponent {
    public var lastPlayedSoundEffect: SoundEffect? = null
    public var lastPlayedParams: PointSourceParams? = null
    public var lastPlayedEntity: Entity? = null
    public var lastPlayedVolume: Float? = null
    public var lastPlayedPriority: Int? = null
    public var lastPlayedIsLooping: Boolean? = null
    public var lastPausedStream: Stream? = null
    public var lastResumedStream: Stream? = null
    public var lastStoppedStream: Stream? = null
    public var lastSetVolumeStream: Stream? = null
    public var lastSetVolumeVolume: Float? = null
    public var lastSetLoopingStream: Stream? = null
    public var lastSetLoopingIsLooping: Boolean? = null

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
