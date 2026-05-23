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
    var lastPlayedStream: Stream? = null
    var lastPausedStream: Stream? = null
    var lastResumedStream: Stream? = null
    var lastStoppedStream: Stream? = null
    var lastSetVolumeStream: Stream? = null
    var lastSetVolumeVolume: Float? = null
    var lastSetLoopingStream: Stream? = null
    var lastSetLoopingIsLooping: Boolean? = null

    private var nextStreamId = 1

    private fun getStream(): Stream {
        val stream = Stream(nextStreamId)
        nextStreamId++
        return stream
    }

    private fun releaseStream(stream: Stream) {
        val streamId = stream.streamId
        _soundEffectMap.remove(streamId)
        _volumeMap.remove(streamId)
        _priorityMap.remove(streamId)
        _isLoopingMap.remove(streamId)
    }

    private val _soundEffectMap: MutableMap<Int, SoundEffect> = mutableMapOf()
    /** A map from [Stream] ID to the [SoundEffect] being played. */
    val soundEffectMap: Map<Int, SoundEffect>
        get() = _soundEffectMap

    private val _priorityMap: MutableMap<Int, Int> = mutableMapOf()
    /** A map from [Stream] ID to the priority of the sound effect. */
    val priorityMap: Map<Int, Int>
        get() = _priorityMap

    private val _volumeMap: MutableMap<Int, Float> = mutableMapOf()
    /** A map from [Stream] ID to the volume of the sound effect. */
    val volumeMap: Map<Int, Float>
        get() = _volumeMap

    private val _isLoopingMap: MutableMap<Int, Boolean> = mutableMapOf()
    /** A map from [Stream] ID to whether the sound effect is looping. */
    val isLoopingMap: Map<Int, Boolean>
        get() = _isLoopingMap

    override fun play(
        soundEffect: SoundEffect,
        pointSourceParams: PointSourceParams,
        entity: Entity?,
        volume: Float,
        priority: Int,
        isLooping: Boolean,
    ): Stream {
        val stream = getStream()
        val streamId = stream.streamId

        lastPlayedSoundEffect = soundEffect
        lastPlayedParams = pointSourceParams
        lastPlayedEntity = entity
        lastPlayedVolume = volume
        lastPlayedPriority = priority
        lastPlayedIsLooping = isLooping
        lastPlayedStream = stream
        _soundEffectMap[streamId] = soundEffect
        _volumeMap[streamId] = volume
        _priorityMap[streamId] = priority
        _isLoopingMap[streamId] = isLooping

        return stream
    }

    override fun pause(stream: Stream) {
        lastPausedStream = stream
    }

    override fun resume(stream: Stream) {
        lastResumedStream = stream
    }

    override fun stop(stream: Stream) {
        lastStoppedStream = stream
        releaseStream(stream)
    }

    override fun setVolume(stream: Stream, volume: Float) {
        lastSetVolumeStream = stream
        lastSetVolumeVolume = volume
        _volumeMap[stream.streamId] = volume
    }

    override fun setLooping(stream: Stream, isLooping: Boolean) {
        lastSetLoopingStream = stream
        lastSetLoopingIsLooping = isLooping
        _isLoopingMap[stream.streamId] = isLooping
    }
}
