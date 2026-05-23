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

import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundEffect
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeSoundEffectPoolComponentTest {
    private lateinit var underTest: FakeSoundEffectPoolComponent

    @Before
    fun setUp() {
        underTest = FakeSoundEffectPoolComponent()

        assertThat(underTest.lastPlayedSoundEffect).isNull()
        assertThat(underTest.lastPlayedParams).isNull()
        assertThat(underTest.lastPlayedEntity).isNull()
        assertThat(underTest.lastPlayedVolume).isNull()
        assertThat(underTest.lastPlayedPriority).isNull()
        assertThat(underTest.lastPlayedIsLooping).isNull()
    }

    @Test
    fun play_setsParametersCorrectly() {
        val soundEffect = SoundEffect(123)
        val pointSourceParams = PointSourceParams()
        val entity = FakeEntity()
        val volume = 0.5f
        val priority = 1
        val isLooping = true

        val stream =
            underTest.play(soundEffect, pointSourceParams, entity, volume, priority, isLooping)
        val streamId = stream.streamId

        assertThat(underTest.lastPlayedSoundEffect).isSameInstanceAs(soundEffect)
        assertThat(underTest.lastPlayedParams).isSameInstanceAs(pointSourceParams)
        assertThat(underTest.lastPlayedEntity).isSameInstanceAs(entity)
        assertThat(underTest.lastPlayedVolume).isEqualTo(volume)
        assertThat(underTest.lastPlayedPriority).isEqualTo(priority)
        assertThat(underTest.lastPlayedIsLooping).isEqualTo(isLooping)
        assertThat(underTest.soundEffectMap[streamId]).isSameInstanceAs(soundEffect)
        assertThat(underTest.volumeMap[streamId]).isEqualTo(volume)
        assertThat(underTest.priorityMap[streamId]).isEqualTo(priority)
        assertThat(underTest.isLoopingMap[streamId]).isEqualTo(isLooping)
    }

    @Test
    fun playWithSameSoundEffect_multipleTimes_returnsDifferentStreams() {
        val soundEffect = SoundEffect(123)
        val pointSourceParams = PointSourceParams()
        val entity = FakeEntity()
        val volume = 0.5f
        val priority = 1
        val isLooping = true

        val stream1 =
            underTest.play(soundEffect, pointSourceParams, entity, volume, priority, isLooping)
        val stream2 =
            underTest.play(soundEffect, pointSourceParams, entity, volume, priority, isLooping)

        assertThat(stream1).isNotSameInstanceAs(stream2)
        assertThat(stream1.streamId).isNotEqualTo(stream2.streamId)

        val streamId1 = stream1.streamId
        val streamId2 = stream2.streamId

        assertThat(underTest.soundEffectMap[streamId1]).isSameInstanceAs(soundEffect)
        assertThat(underTest.soundEffectMap[streamId2]).isSameInstanceAs(soundEffect)
    }

    @Test
    fun pause_getsLastPausedStreamCorrectly() {
        val soundEffect = SoundEffect(123)
        val pointSourceParams = PointSourceParams()
        val entity = FakeEntity()
        val volume = 0.5f
        val priority = 1
        val isLooping = true

        val stream =
            underTest.play(soundEffect, pointSourceParams, entity, volume, priority, isLooping)

        assertThat(underTest.lastPausedStream).isNull()

        underTest.pause(stream)

        assertThat(underTest.lastPausedStream).isSameInstanceAs(stream)
    }

    @Test
    fun resume_getsLastResumedStreamCorrectly() {
        val soundEffect = SoundEffect(123)
        val pointSourceParams = PointSourceParams()
        val entity = FakeEntity()
        val volume = 0.5f
        val priority = 1
        val isLooping = true

        val stream =
            underTest.play(soundEffect, pointSourceParams, entity, volume, priority, isLooping)

        assertThat(underTest.lastResumedStream).isNull()

        underTest.resume(stream)

        assertThat(underTest.lastResumedStream).isSameInstanceAs(stream)
    }

    @Test
    fun stop_getsLastStoppedStreamAndClearsState() {
        val soundEffect = SoundEffect(123)
        val pointSourceParams = PointSourceParams()
        val entity = FakeEntity()
        val volume = 0.5f
        val priority = 1
        val isLooping = true

        val stream =
            underTest.play(soundEffect, pointSourceParams, entity, volume, priority, isLooping)
        val streamId = stream.streamId

        assertThat(underTest.lastStoppedStream).isNull()
        assertThat(underTest.soundEffectMap[streamId]).isSameInstanceAs(soundEffect)
        assertThat(underTest.volumeMap[streamId]).isEqualTo(volume)
        assertThat(underTest.priorityMap[streamId]).isEqualTo(priority)
        assertThat(underTest.isLoopingMap[streamId]).isEqualTo(isLooping)

        underTest.stop(stream)

        assertThat(underTest.lastStoppedStream).isSameInstanceAs(stream)
        assertThat(underTest.soundEffectMap).doesNotContainKey(streamId)
        assertThat(underTest.volumeMap).doesNotContainKey(streamId)
        assertThat(underTest.priorityMap).doesNotContainKey(streamId)
        assertThat(underTest.isLoopingMap).doesNotContainKey(streamId)
    }

    @Test
    fun setVolume_setsParametersCorrectly() {
        val soundEffect = SoundEffect(123)
        val pointSourceParams = PointSourceParams()
        val entity = FakeEntity()
        val volume = 0.5f
        val priority = 1
        val isLooping = true

        val stream =
            underTest.play(soundEffect, pointSourceParams, entity, volume, priority, isLooping)
        val streamId = stream.streamId

        assertThat(underTest.lastSetVolumeStream).isNull()
        assertThat(underTest.lastSetVolumeVolume).isNull()
        assertThat(underTest.volumeMap[streamId]).isEqualTo(volume)

        val expectedVolume = 1.0f
        underTest.setVolume(stream, expectedVolume)

        assertThat(underTest.lastSetVolumeStream).isSameInstanceAs(stream)
        assertThat(underTest.lastSetVolumeVolume).isEqualTo(expectedVolume)
        assertThat(underTest.volumeMap[streamId]).isEqualTo(expectedVolume)
    }

    @Test
    fun setLooping_setsParametersCorrectly() {
        val soundEffect = SoundEffect(123)
        val pointSourceParams = PointSourceParams()
        val entity = FakeEntity()
        val volume = 0.5f
        val priority = 1
        val isLooping = false

        val stream =
            underTest.play(soundEffect, pointSourceParams, entity, volume, priority, isLooping)
        val streamId = stream.streamId

        assertThat(underTest.lastSetLoopingStream).isNull()
        assertThat(underTest.lastSetLoopingIsLooping).isNull()
        assertThat(underTest.isLoopingMap[streamId]).isEqualTo(isLooping)

        underTest.setLooping(stream, true)

        assertThat(underTest.lastSetLoopingStream).isSameInstanceAs(stream)
        assertThat(underTest.lastSetLoopingIsLooping).isTrue()
        assertThat(underTest.isLoopingMap[streamId]).isTrue()
    }
}
