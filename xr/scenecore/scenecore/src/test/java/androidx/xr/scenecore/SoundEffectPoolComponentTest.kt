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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore

import androidx.activity.ComponentActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.testing.SceneCoreTestRule
import androidx.xr.scenecore.testing.SoundEffectPoolComponentTester
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SoundEffectPoolComponentTest {

    @Rule @JvmField val testRule = SceneCoreTestRule()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()

        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun addComponent_addsRuntimeSoundEffectPoolComponent() {
        val entity = Entity.create(session, "test")
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)

        assertThat(entity.getComponents()).isEmpty()
        assertThat(entity.addComponent(component)).isTrue()
        assertThat(entity.getComponents()).containsExactly(component)
    }

    @Test
    fun addComponent_canBeAddedToSecondComponent() {
        val firstEntity = Entity.create(session, "test")
        val secondEntity = Entity.create(session, "test")
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)

        assertThat(firstEntity.getComponents()).isEmpty()
        assertThat(firstEntity.addComponent(component)).isTrue()
        assertThat(firstEntity.getComponents()).containsExactly(component)

        firstEntity.removeComponent(component)
        assertThat(firstEntity.getComponents()).isEmpty()

        assertThat(secondEntity.getComponents()).isEmpty()
        assertThat(secondEntity.addComponent(component)).isTrue()
        assertThat(secondEntity.getComponents()).containsExactly(component)
    }

    @Test
    fun play_withEntity_forwardsCorrectDataToRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        val tester = testRule.createTester<SoundEffectPoolComponentTester>(component)
        val soundEffect = soundEffectPool.load(activity, 123)
        val entity = Entity.create(session, "test")
        entity.addComponent(component)

        // Use an invalid or uninitialized stream id
        var stream = Stream(0)

        assertThat(tester.lastPlayedPointSourceParams).isNull()
        assertThat(tester.lastPlayedStream).isNull()
        // Throws an exception if the stream is inactive.
        assertThrows(IllegalArgumentException::class.java) { tester.getSoundEffect(stream) }
        assertThrows(IllegalArgumentException::class.java) { tester.getVolume(stream) }
        assertThrows(IllegalArgumentException::class.java) { tester.getPriority(stream) }
        assertThrows(IllegalArgumentException::class.java) { tester.isLooping(stream) }

        stream = component.play(soundEffect, 0.5f, 1, true)

        assertThat(tester.lastPlayedPointSourceParams).isEqualTo(params)
        assertThat(tester.lastPlayedStream).isEqualTo(stream)
        assertThat(tester.getSoundEffect(stream)).isEqualTo(soundEffect)
        assertThat(tester.getVolume(stream)).isEqualTo(0.5f)
        assertThat(tester.getPriority(stream)).isEqualTo(1)
        assertThat(tester.isLooping(stream)).isTrue()
    }

    @Test
    fun pause_forwardsCorrectStreamIdToRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        val tester = testRule.createTester<SoundEffectPoolComponentTester>(component)
        val stream = Stream(1)

        // Null indicates no stream has been paused
        assertThat(tester.lastPausedStream).isNull()

        component.pause(stream)

        assertThat(tester.lastPausedStream).isEqualTo(stream)
    }

    @Test
    fun resume_forwardsCorrectStreamIdToRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        val tester = testRule.createTester<SoundEffectPoolComponentTester>(component)
        val stream = Stream(2)

        // Null indicates no stream has been resumed
        assertThat(tester.lastResumedStream).isNull()

        component.resume(stream)

        assertThat(tester.lastResumedStream).isEqualTo(stream)
    }

    @Test
    fun stop_forwardsCorrectStreamIdToRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        val tester = testRule.createTester<SoundEffectPoolComponentTester>(component)
        val stream = Stream(3)

        // Null indicates no stream has been stopped
        assertThat(tester.lastStoppedStream).isNull()

        component.stop(stream)

        assertThat(tester.lastStoppedStream).isEqualTo(stream)
    }

    @Test
    fun setVolume_forwardsCorrectDataToRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        val tester = testRule.createTester<SoundEffectPoolComponentTester>(component)
        val stream = Stream(4)

        // Throws an exception if the stream is inactive.
        assertThrows(IllegalArgumentException::class.java) { tester.getVolume(stream) }

        component.setVolume(stream, 0.8f)

        assertThat(tester.getVolume(stream)).isEqualTo(0.8f)
    }

    @Test
    fun setLooping_forwardsCorrectDataToRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        val tester = testRule.createTester<SoundEffectPoolComponentTester>(component)
        val stream = Stream(5)

        // Throws an exception if the stream is inactive.
        assertThrows(IllegalArgumentException::class.java) { tester.isLooping(stream) }

        component.setLooping(stream, true)

        assertThat(tester.isLooping(stream)).isTrue()
    }

    @Test
    fun pointSourceParams_updatesParamsForFuturePlays() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val params = PointSourceParams()
        val component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        val tester = testRule.createTester<SoundEffectPoolComponentTester>(component)
        val newParams = PointSourceParams()

        assertThat(component.pointSourceParams).isEqualTo(params)

        component.pointSourceParams = newParams

        assertThat(component.pointSourceParams).isEqualTo(newParams)

        val soundEffect = soundEffectPool.load(activity, 123)
        val entity = Entity.create(session, "test")
        entity.addComponent(component)

        component.play(soundEffect, 0.5f, 1, true)

        assertThat(tester.lastPlayedPointSourceParams).isEqualTo(newParams)
    }
}
