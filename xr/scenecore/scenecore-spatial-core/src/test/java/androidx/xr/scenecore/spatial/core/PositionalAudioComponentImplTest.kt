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

package androidx.xr.scenecore.spatial.core

import android.app.Activity
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.media3.common.C.ENCODING_PCM_16BIT
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioTrackAudioOutput
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.testing.XrDeviceTestRule
import androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PositionalAudioComponentImplTest {
    private val activityController: ActivityController<Activity> =
        Robolectric.buildActivity(Activity::class.java)
    private val activity: Activity = activityController.create().start().get()
    private val fakeExecutor = FakeScheduledExecutorService()
    private val xrExtensions = SpatialCoreXrExtensionsHolderProvider.extensionsLegacy
    private lateinit var fakeRuntime: SpatialSceneRuntime

    @Rule @JvmField val xrDeviceTestRule = XrDeviceTestRule()

    @Before
    fun setUp() {
        xrDeviceTestRule.spatialApiVersion = 1
        fakeRuntime =
            SpatialSceneRuntime.create(activity, fakeExecutor, xrExtensions, SceneNodeRegistry())
    }

    @After
    fun tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        fakeRuntime.destroy()
    }

    private fun createTestEntity(): Entity {
        return fakeRuntime.createEntity(Pose(), "test", fakeRuntime.activitySpace)
    }

    private fun createMockAudioTrackExtensions(): AudioTrackExtensionsWrapper {
        return mock {
            on { setPointSourceParams(any<AudioTrack.Builder>(), any(), anyOrNull()) } doAnswer
                {
                    it.arguments[0] as AudioTrack.Builder
                }
        }
    }

    @Test
    fun getAudioOutputProvider_returnsProvider() {
        val mockAudioTrackExtensions = createMockAudioTrackExtensions()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, mockAudioTrackExtensions, params)

        val provider = component.getAudioOutputProvider()

        assertThat(provider).isNotNull()
    }

    @Test
    fun setPointSourceParams_setsParamsOnTrack_ifTrackExists() {
        val mockAudioTrackExtensions = createMockAudioTrackExtensions()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, mockAudioTrackExtensions, params)
        val config = TEST_OUTPUT_CONFIG

        val outputProvider = component.getAudioOutputProvider()
        val audioTrackOutput = outputProvider.getAudioOutput(config) as AudioTrackAudioOutput
        val audioTrack = audioTrackOutput.audioTrack

        val newParams = PointSourceParams()
        component.setPointSourceParams(newParams)

        verify(mockAudioTrackExtensions)
            .setPointSourceParams(eq(audioTrack), eq(newParams), isNull())
    }

    @Test
    fun onAttach_setsParamsOnTrack_ifTrackExists() {
        val mockAudioTrackExtensions = createMockAudioTrackExtensions()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, mockAudioTrackExtensions, params)
        val entity = createTestEntity()
        val config = TEST_OUTPUT_CONFIG

        val outputProvider = component.getAudioOutputProvider()
        val audioTrackOutput = outputProvider.getAudioOutput(config) as AudioTrackAudioOutput
        val audioTrack = audioTrackOutput.audioTrack

        assertThat(component.onAttach(entity)).isTrue()
        verify(mockAudioTrackExtensions)
            .setPointSourceParams(eq(audioTrack), eq(params), eq(entity))
    }

    @Test
    fun onAttach_setsParamsOnTrackBuilder() {
        val mockAudioTrackExtensions = createMockAudioTrackExtensions()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, mockAudioTrackExtensions, params)
        val entity = createTestEntity()
        val config = TEST_OUTPUT_CONFIG

        assertThat(component.onAttach(entity)).isTrue()

        val outputProvider = component.getAudioOutputProvider()
        outputProvider.getAudioOutput(config)

        verify(mockAudioTrackExtensions)
            .setPointSourceParams(any<AudioTrack.Builder>(), eq(params), eq(entity))
    }

    @Test
    fun onAttach_setsParamsOnTrack_andTrackBuilderOnNextTrackCreation() {
        val mockAudioTrackExtensions = createMockAudioTrackExtensions()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, mockAudioTrackExtensions, params)
        val entity = createTestEntity()
        val config = TEST_OUTPUT_CONFIG

        val outputProvider = component.getAudioOutputProvider()
        val audioTrackOutput = outputProvider.getAudioOutput(config) as AudioTrackAudioOutput
        val audioTrack = audioTrackOutput.audioTrack

        assertThat(component.onAttach(entity)).isTrue()
        verify(mockAudioTrackExtensions)
            .setPointSourceParams(eq(audioTrack), eq(params), eq(entity))

        outputProvider.getAudioOutput(config)

        verify(mockAudioTrackExtensions)
            .setPointSourceParams(any<AudioTrack.Builder>(), eq(params), eq(entity))
    }

    @Test
    fun onAttach_returnsFalse_ifNotAndroidXrEntity() {
        val mockAudioTrackExtensions = createMockAudioTrackExtensions()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, mockAudioTrackExtensions, params)
        val mockEntity = mock<Entity>()

        assertThat(component.onAttach(mockEntity)).isFalse()
    }

    @Test
    fun onDetach_clearsAttachedEntity() {
        val mockAudioTrackExtensions = createMockAudioTrackExtensions()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, mockAudioTrackExtensions, params)
        val entity = createTestEntity()

        val config = TEST_OUTPUT_CONFIG

        val outputProvider = component.getAudioOutputProvider()
        val audioTrackOutput = outputProvider.getAudioOutput(config) as AudioTrackAudioOutput
        val audioTrack = audioTrackOutput.audioTrack

        component.onAttach(entity)
        component.onDetach(entity)

        verify(mockAudioTrackExtensions).setPointSourceParams(eq(audioTrack), eq(params), isNull())
    }

    @Test
    fun onReattach_attachesToNewEntity() {
        val mockAudioTrackExtensions = createMockAudioTrackExtensions()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, mockAudioTrackExtensions, params)
        val entity1 = createTestEntity()
        val entity2 = createTestEntity()

        val config = TEST_OUTPUT_CONFIG

        val outputProvider = component.getAudioOutputProvider()
        val audioTrackOutput = outputProvider.getAudioOutput(config) as AudioTrackAudioOutput
        val audioTrack = audioTrackOutput.audioTrack

        component.onAttach(entity1)
        component.onDetach(entity1)
        component.onAttach(entity2)

        verify(mockAudioTrackExtensions)
            .setPointSourceParams(eq(audioTrack), eq(params), eq(entity2))
    }

    @Test
    fun setPointSourceParams_handlesReleasedTrack() {
        val mockAudioTrackExtensions = createMockAudioTrackExtensions()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, mockAudioTrackExtensions, params)
        val config = TEST_OUTPUT_CONFIG

        val outputProvider = component.getAudioOutputProvider()
        val audioTrackOutput = outputProvider.getAudioOutput(config) as AudioTrackAudioOutput
        val audioTrack = audioTrackOutput.audioTrack

        whenever(
                mockAudioTrackExtensions.setPointSourceParams(eq(audioTrack), eq(params), isNull())
            )
            .thenThrow(IllegalStateException("Simulated track released"))

        val newParams = PointSourceParams()
        component.setPointSourceParams(newParams)
    }

    @Test
    fun onAttach_handlesReleasedTrack() {
        val mockAudioTrackExtensions = createMockAudioTrackExtensions()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, mockAudioTrackExtensions, params)
        val entity = createTestEntity()
        val config = TEST_OUTPUT_CONFIG

        val outputProvider = component.getAudioOutputProvider()
        val audioTrackOutput = outputProvider.getAudioOutput(config) as AudioTrackAudioOutput
        val audioTrack = audioTrackOutput.audioTrack

        whenever(
                mockAudioTrackExtensions.setPointSourceParams(
                    eq(audioTrack),
                    eq(params),
                    eq(entity),
                )
            )
            .thenThrow(IllegalStateException("Simulated track released"))

        assertThat(component.onAttach(entity)).isTrue()
    }

    @Test
    fun onDetach_handlesReleasedTrack() {
        val mockAudioTrackExtensions = createMockAudioTrackExtensions()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, mockAudioTrackExtensions, params)
        val entity = createTestEntity()
        val config = TEST_OUTPUT_CONFIG

        val outputProvider = component.getAudioOutputProvider()
        val audioTrackOutput = outputProvider.getAudioOutput(config) as AudioTrackAudioOutput
        val audioTrack = audioTrackOutput.audioTrack

        component.onAttach(entity)

        whenever(
                mockAudioTrackExtensions.setPointSourceParams(eq(audioTrack), eq(params), isNull())
            )
            .thenThrow(IllegalStateException("Simulated track released"))

        component.onDetach(entity)
    }

    private companion object {
        private val TEST_OUTPUT_CONFIG: AudioOutputProvider.OutputConfig =
            AudioOutputProvider.OutputConfig.Builder()
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(ENCODING_PCM_16BIT)
                .setBufferSize(16)
                .setSampleRate(48000)
                .build()
    }
}
