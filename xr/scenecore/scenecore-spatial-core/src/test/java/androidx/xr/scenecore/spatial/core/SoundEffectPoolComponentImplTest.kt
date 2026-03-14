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

import android.app.Activity
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.testing.FakeSpatialApiVersionProvider.Companion.testSpatialApiVersion
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundEffect
import androidx.xr.scenecore.runtime.SoundEffectPlayer
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper
import androidx.xr.scenecore.runtime.Stream
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SoundEffectPoolComponentImplTest {
    private val activityController: ActivityController<Activity> =
        Robolectric.buildActivity(Activity::class.java)
    private val activity: Activity = activityController.create().start().get()
    private val fakeExecutor = FakeScheduledExecutorService()
    private val xrExtensions = getXrExtensions()
    private lateinit var fakeRuntime: SpatialSceneRuntime

    @Before
    fun setUp() {
        testSpatialApiVersion = 1
        fakeRuntime =
            SpatialSceneRuntime.create(activity, fakeExecutor, xrExtensions!!, EntityManager())
    }

    @After
    fun tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        fakeRuntime.destroy()
        testSpatialApiVersion = null
    }

    private fun createTestEntity(): Entity {
        return fakeRuntime.createEntity(Pose(), "test", fakeRuntime.activitySpace)
    }

    @Test
    fun onAttach_returnsTrue_ifAndroidXrEntity() {
        val mockSoundPoolExtensions = mock<SoundPoolExtensionsWrapper>()
        val mockSoundEffectPlayer = mock<SoundEffectPlayer>()

        val soundEffectPool = SoundEffectPoolImpl(1, mockSoundPoolExtensions, mockSoundEffectPlayer)

        val component = SoundEffectPoolComponentImpl(soundEffectPool)
        val entity = createTestEntity()

        assertThat(component.onAttach(entity)).isTrue()
    }

    @Test
    fun onAttach_returnsFalse_ifNotAndroidXrEntity() {
        val mockSoundPoolExtensions = mock<SoundPoolExtensionsWrapper>()
        val mockSoundEffectPlayer = mock<SoundEffectPlayer>()

        val soundEffectPool =
            SoundEffectPoolImpl(maxStreams = 1, mockSoundPoolExtensions, mockSoundEffectPlayer)

        val params = PointSourceParams()
        val component = SoundEffectPoolComponentImpl(soundEffectPool)
        val mockEntity = mock<Entity>()

        assertThat(component.onAttach(mockEntity)).isFalse()
    }

    @Test
    fun play_callsExtensionsWrapper() {
        val mockSoundPoolExtensions = mock<SoundPoolExtensionsWrapper>()
        val mockSoundEffectPlayer = mock<SoundEffectPlayer>()

        val soundEffectPool = SoundEffectPoolImpl(1, mockSoundPoolExtensions, mockSoundEffectPlayer)

        val params = PointSourceParams()
        val component = SoundEffectPoolComponentImpl(soundEffectPool)
        val entity = createTestEntity()
        component.onAttach(entity)

        val soundEffect = SoundEffect(1)
        val volume = 0.5f
        val priority = 1
        val isLooping = false

        component.play(soundEffect, params, entity, volume, priority, isLooping)

        verify(mockSoundEffectPlayer)
            .play(
                eq(soundEffect),
                eq(params),
                eq(entity),
                eq(volume),
                eq(priority),
                eq(isLooping), // loopCount for isLooping = false
            )
    }

    @Test
    fun play_callsExtensionsWrapper_withLooping() {
        val mockSoundPoolExtensions = mock<SoundPoolExtensionsWrapper>()
        val mockSoundEffectPlayer = mock<SoundEffectPlayer>()

        val soundEffectPool = SoundEffectPoolImpl(1, mockSoundPoolExtensions, mockSoundEffectPlayer)

        val params = PointSourceParams()
        val component = SoundEffectPoolComponentImpl(soundEffectPool)
        val entity = createTestEntity()
        component.onAttach(entity)

        val soundEffect = SoundEffect(1)
        val volume = 0.5f
        val priority = 1
        val isLooping = true

        component.play(soundEffect, params, entity, volume, priority, isLooping)

        verify(mockSoundEffectPlayer)
            .play(eq(soundEffect), eq(params), eq(entity), eq(volume), eq(priority), eq(isLooping))
    }

    @Test
    fun pause_callsSoundPool() {
        val mockSoundPoolExtensions = mock<SoundPoolExtensionsWrapper>()
        val mockSoundEffectPlayer = mock<SoundEffectPlayer>()

        val soundEffectPool = SoundEffectPoolImpl(1, mockSoundPoolExtensions, mockSoundEffectPlayer)

        val component = SoundEffectPoolComponentImpl(soundEffectPool)
        val stream = Stream(10)

        component.pause(stream)

        verify(mockSoundEffectPlayer).pause(stream)
    }

    @Test
    fun resume_callsSoundPool() {
        val mockSoundPoolExtensions = mock<SoundPoolExtensionsWrapper>()
        val mockSoundEffectPlayer = mock<SoundEffectPlayer>()

        val soundEffectPool = SoundEffectPoolImpl(1, mockSoundPoolExtensions, mockSoundEffectPlayer)

        val component = SoundEffectPoolComponentImpl(soundEffectPool)
        val stream = Stream(10)

        component.resume(stream)

        verify(mockSoundEffectPlayer).resume(stream)
    }

    @Test
    fun stop_callsSoundPool() {
        val mockSoundPoolExtensions = mock<SoundPoolExtensionsWrapper>()
        val mockSoundEffectPlayer = mock<SoundEffectPlayer>()

        val soundEffectPool = SoundEffectPoolImpl(1, mockSoundPoolExtensions, mockSoundEffectPlayer)

        val component = SoundEffectPoolComponentImpl(soundEffectPool)
        val stream = Stream(10)

        component.stop(stream)

        verify(mockSoundEffectPlayer).stop(stream)
    }

    @Test
    fun setVolume_callsSoundPool() {
        val mockSoundPoolExtensions = mock<SoundPoolExtensionsWrapper>()
        val mockSoundEffectPlayer = mock<SoundEffectPlayer>()
        val soundEffectPool =
            SoundEffectPoolImpl(maxStreams = 1, mockSoundPoolExtensions, mockSoundEffectPlayer)

        val component = SoundEffectPoolComponentImpl(soundEffectPool)
        val stream = Stream(10)
        val volume = 0.8f

        component.setVolume(stream, volume)

        verify(mockSoundEffectPlayer).setVolume(stream, volume)
    }

    @Test
    fun setLooping_callsSoundPool() {
        val mockSoundPoolExtensions = mock<SoundPoolExtensionsWrapper>()
        val mockSoundEffectPlayer = mock<SoundEffectPlayer>()
        val soundEffectPool =
            SoundEffectPoolImpl(maxStreams = 1, mockSoundPoolExtensions, mockSoundEffectPlayer)

        val component = SoundEffectPoolComponentImpl(soundEffectPool)
        val stream = Stream(10)

        component.setLooping(stream, true)

        verify(mockSoundEffectPlayer).setLooping(stream, true)

        component.setLooping(stream, false)

        verify(mockSoundEffectPlayer).setLooping(stream, false)
    }
}
