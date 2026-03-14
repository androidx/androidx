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

package androidx.xr.scenecore

import androidx.activity.ComponentActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.testing.FakeSoundEffectPoolComponent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SoundEffectPoolComponentTest {

    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var session: Session

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @Test
    fun addComponent_addsRuntimeSoundEffectPoolComponent() {
        val entity = Entity.create(session, "test")
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)

        assertThat(entity.addComponent(component)).isTrue()
        assertThat((entity as BaseEntity<*>).rtEntity?.getComponents()?.get(0))
            .isInstanceOf(FakeSoundEffectPoolComponent::class.java)
    }

    @Test
    fun addComponent_canBeAddedToSecondComponent() {
        val firstEntity = Entity.create(session, "test")
        val secondEntity = Entity.create(session, "test")
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)

        assertThat(firstEntity.addComponent(component)).isTrue()
        assertThat((firstEntity as BaseEntity<*>).rtEntity?.getComponents()?.get(0))
            .isInstanceOf(FakeSoundEffectPoolComponent::class.java)

        firstEntity.removeComponent(component)
        assertThat(firstEntity.rtEntity?.getComponents()).hasSize(0)

        assertThat(secondEntity.addComponent(component)).isTrue()
        assertThat((secondEntity as BaseEntity<*>).rtEntity?.getComponents()?.get(0))
            .isInstanceOf(FakeSoundEffectPoolComponent::class.java)
    }

    @Test
    fun play_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        val soundEffect = soundEffectPool.load(activity, 123)
        val entity = Entity.create(session, "test")
        entity.addComponent(component)

        component.play(soundEffect, 0.5f, 1, true)

        val fakeComponent = component.rtComponent as FakeSoundEffectPoolComponent

        assertThat(fakeComponent.lastPlayedSoundEffect?.id).isEqualTo(soundEffect.id)
        assertThat(fakeComponent.lastPlayedParams).isEqualTo(params.rtPointSourceParams)
        assertThat(fakeComponent.lastPlayedEntity).isEqualTo((entity as BaseEntity<*>).rtEntity)
        assertThat(fakeComponent.lastPlayedVolume).isEqualTo(0.5f)
        assertThat(fakeComponent.lastPlayedPriority).isEqualTo(1)
        assertThat(fakeComponent.lastPlayedIsLooping).isTrue()
    }

    @Test
    fun pause_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        val stream = Stream(1)

        component.pause(stream)

        val fakeComponent = component.rtComponent as FakeSoundEffectPoolComponent
        assertThat(fakeComponent.lastPausedStream?.streamId).isEqualTo(stream.streamId)
    }

    @Test
    fun resume_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        val stream = Stream(1)

        component.resume(stream)

        val fakeComponent = component.rtComponent as FakeSoundEffectPoolComponent
        assertThat(fakeComponent.lastResumedStream?.streamId).isEqualTo(stream.streamId)
    }

    @Test
    fun stop_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        val stream = Stream(1)

        component.stop(stream)

        val fakeComponent = component.rtComponent as FakeSoundEffectPoolComponent
        assertThat(fakeComponent.lastStoppedStream?.streamId).isEqualTo(stream.streamId)
    }

    @Test
    fun setVolume_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        val stream = Stream(1)

        component.setVolume(stream, 0.8f)

        val fakeComponent = component.rtComponent as FakeSoundEffectPoolComponent
        assertThat(fakeComponent.lastSetVolumeStream?.streamId).isEqualTo(stream.streamId)
        assertThat(fakeComponent.lastSetVolumeVolume).isEqualTo(0.8f)
    }

    @Test
    fun setLooping_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        val stream = Stream(1)

        component.setLooping(stream, true)

        val fakeComponent = component.rtComponent as FakeSoundEffectPoolComponent
        assertThat(fakeComponent.lastSetLoopingStream?.streamId).isEqualTo(stream.streamId)
        assertThat(fakeComponent.lastSetLoopingIsLooping).isTrue()
    }
}
