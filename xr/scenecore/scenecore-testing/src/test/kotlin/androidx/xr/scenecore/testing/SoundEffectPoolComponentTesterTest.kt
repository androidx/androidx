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

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.SoundEffectPool
import androidx.xr.scenecore.SoundEffectPoolComponent
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
class SoundEffectPoolComponentTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()
    private val params = PointSourceParams()
    private val maxStreams = 4

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session
    private lateinit var soundEffectPool: SoundEffectPool
    private lateinit var component: SoundEffectPoolComponent
    private lateinit var tester: SoundEffectPoolComponentTester

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(
                context = activity,
                coroutineContext = testDispatcher,
                lifecycleOwner = activity as LifecycleOwner,
            )

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        soundEffectPool = SoundEffectPool.create(session, maxStreams)
        component = SoundEffectPoolComponent.create(session, soundEffectPool, params)
        tester = testRule.createTester<SoundEffectPoolComponentTester>(component)
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun equalsAndHashCode_behaveCorrectly() {
        val tester1 = SoundEffectPoolComponentTester.create(component)
        val tester2 = SoundEffectPoolComponentTester.create(component)

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }

    @Test
    fun play_withEntity_forwardsCorrectDataToRuntime() {
        val soundEffect = soundEffectPool.load(activity, 123)
        val entity = Entity.create(session, "test")

        assertThat(entity.addComponent(component)).isTrue()

        assertThat(tester.lastPlayedPointSourceParams).isNull()
        assertThat(tester.lastPlayedStream).isNull()

        val stream = component.play(soundEffect, volume = 0.5f, priority = 1, isLooping = true)

        assertThat(tester.lastPlayedPointSourceParams).isEqualTo(params)
        assertThat(tester.lastPlayedStream).isEqualTo(stream)
        assertThat(tester.getSoundEffect(stream)).isEqualTo(soundEffect)
        assertThat(tester.getVolume(stream)).isEqualTo(0.5f)
        assertThat(tester.getPriority(stream)).isEqualTo(1)
        assertThat(tester.isLooping(stream)).isTrue()
    }

    @Test
    fun playWithSameSoundEffect_multipleTimes_returnsDifferentStreams() {
        val soundEffect = soundEffectPool.load(activity, 123)
        val entity = Entity.create(session, "test")

        assertThat(entity.addComponent(component)).isTrue()

        val stream1 = component.play(soundEffect, volume = 0.5f, priority = 1, isLooping = true)
        val stream2 = component.play(soundEffect, volume = 0.8f, priority = 2, isLooping = false)

        assertThat(stream1).isNotEqualTo(stream2)
        assertThat(tester.lastPlayedStream).isEqualTo(stream2)

        assertThat(tester.getSoundEffect(stream1)).isEqualTo(soundEffect)
        assertThat(tester.getSoundEffect(stream2)).isEqualTo(soundEffect)

        assertThat(tester.getVolume(stream1)).isEqualTo(0.5f)
        assertThat(tester.getVolume(stream2)).isEqualTo(0.8f)

        assertThat(tester.getPriority(stream1)).isEqualTo(1)
        assertThat(tester.getPriority(stream2)).isEqualTo(2)

        assertThat(tester.isLooping(stream1)).isTrue()
        assertThat(tester.isLooping(stream2)).isFalse()
    }

    @Test
    fun pause_forwardsCorrectStreamIdToRuntime() {
        val soundEffect = soundEffectPool.load(activity, 123)
        val entity = Entity.create(session, "test")

        assertThat(entity.addComponent(component)).isTrue()

        val stream = component.play(soundEffect, volume = 0.5f, priority = 1, isLooping = true)

        // Null indicates no stream has been paused
        assertThat(tester.lastPausedStream).isNull()

        component.pause(stream)

        assertThat(tester.lastPausedStream).isEqualTo(stream)
    }

    @Test
    fun resume_forwardsCorrectStreamIdToRuntime() {
        val soundEffect = soundEffectPool.load(activity, 123)
        val entity = Entity.create(session, "test")

        assertThat(entity.addComponent(component)).isTrue()

        val stream = component.play(soundEffect, volume = 0.5f, priority = 1, isLooping = true)

        // Null indicates no stream has been resumed
        assertThat(tester.lastPausedStream).isNull()

        component.resume(stream)

        assertThat(tester.lastResumedStream).isEqualTo(stream)
    }

    @Test
    fun stop_forwardsCorrectStreamIdToRuntime_andClearsState() {
        val soundEffect = soundEffectPool.load(activity, 123)
        val entity = Entity.create(session, "test")

        assertThat(entity.addComponent(component)).isTrue()

        val stream = component.play(soundEffect, volume = 0.5f, priority = 1, isLooping = true)

        // Null indicates no stream has been stopped
        assertThat(tester.lastStoppedStream).isNull()
        assertThat(tester.getSoundEffect(stream)).isEqualTo(soundEffect)
        assertThat(tester.getVolume(stream)).isEqualTo(0.5f)
        assertThat(tester.getPriority(stream)).isEqualTo(1)
        assertThat(tester.isLooping(stream)).isTrue()

        component.stop(stream)

        assertThat(tester.lastStoppedStream).isEqualTo(stream)
        assertThrows(IllegalArgumentException::class.java) { tester.getSoundEffect(stream) }
        assertThrows(IllegalArgumentException::class.java) { tester.getVolume(stream) }
        assertThrows(IllegalArgumentException::class.java) { tester.getPriority(stream) }
        assertThrows(IllegalArgumentException::class.java) { tester.isLooping(stream) }
    }

    @Test
    fun setLooping_forwardsCorrectDataToRuntime() {
        val soundEffect = soundEffectPool.load(activity, 123)
        val entity = Entity.create(session, "test")

        assertThat(entity.addComponent(component)).isTrue()

        val stream = component.play(soundEffect, volume = 0.5f, priority = 1, isLooping = false)

        assertThat(tester.isLooping(stream)).isFalse()

        component.setLooping(stream, isLooping = true)

        assertThat(tester.isLooping(stream)).isTrue()
    }

    @Test
    fun setVolume_forwardsCorrectDataToRuntime() {
        val soundEffect = soundEffectPool.load(activity, 123)
        val entity = Entity.create(session, "test")

        assertThat(entity.addComponent(component)).isTrue()

        val stream = component.play(soundEffect, volume = 0.0f, priority = 1, isLooping = true)

        assertThat(tester.getVolume(stream)).isEqualTo(0.0f)

        component.setVolume(stream, volume = 0.8f)

        assertThat(tester.getVolume(stream)).isEqualTo(0.8f)
    }
}
