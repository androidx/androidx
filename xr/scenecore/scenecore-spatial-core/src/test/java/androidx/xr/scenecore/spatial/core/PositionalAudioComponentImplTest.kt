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
import android.media.AudioFormat
import androidx.media3.common.C.ENCODING_PCM_16BIT
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioTrackAudioOutput
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.testing.FakeSpatialApiVersionProvider.Companion.testSpatialApiVersion
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeAudioTrackExtensionsWrapper
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
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
    fun getAudioOutputProvider_returnsProvider() {
        val fakeAudioTrackExtensions = FakeAudioTrackExtensionsWrapper()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, fakeAudioTrackExtensions, params)

        val provider = component.getAudioOutputProvider()

        assertThat(provider).isNotNull()
    }

    @Test
    fun setPointSourceParams_setsParamsOnTrack_ifTrackExists() {
        val fakeAudioTrackExtensions = FakeAudioTrackExtensionsWrapper()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, fakeAudioTrackExtensions, params)
        val config = TEST_OUTPUT_CONFIG

        val outputProvider = component.getAudioOutputProvider()
        val audioTrackOutput = outputProvider.getAudioOutput(config) as AudioTrackAudioOutput
        val audioTrack = audioTrackOutput.audioTrack

        val newParams = PointSourceParams()
        component.setPointSourceParams(newParams)

        assertThat(fakeAudioTrackExtensions.pointSourceParamsMap[audioTrack]).isEqualTo(newParams)
        assertThat(fakeAudioTrackExtensions.entityMap[audioTrack]).isEqualTo(null)
    }

    @Test
    fun onAttach_setsParamsOnTrack_ifTrackExists() {
        val fakeAudioTrackExtensions = FakeAudioTrackExtensionsWrapper()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, fakeAudioTrackExtensions, params)
        val entity = createTestEntity()
        val config = TEST_OUTPUT_CONFIG

        val outputProvider = component.getAudioOutputProvider()
        val audioTrackOutput = outputProvider.getAudioOutput(config) as AudioTrackAudioOutput
        val audioTrack = audioTrackOutput.audioTrack

        assertThat(component.onAttach(entity)).isTrue()
        assertThat(fakeAudioTrackExtensions.pointSourceParamsMap[audioTrack]).isEqualTo(params)
        assertThat(fakeAudioTrackExtensions.entityMap[audioTrack]).isEqualTo(entity)
    }

    @Test
    fun onAttach_setsParamsOnTrackBuilder() {
        val fakeAudioTrackExtensions = FakeAudioTrackExtensionsWrapper()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, fakeAudioTrackExtensions, params)
        val entity = createTestEntity()
        val config = TEST_OUTPUT_CONFIG

        assertThat(component.onAttach(entity)).isTrue()

        val outputProvider = component.getAudioOutputProvider()
        outputProvider.getAudioOutput(config)

        assertThat(fakeAudioTrackExtensions.pointSourceParamsBuilderMap.values).contains(params)
        assertThat(fakeAudioTrackExtensions.entityBuilderMap.values).contains(entity)
    }

    @Test
    fun onAttach_setsParamsOnTrack_andTrackBuilderOnNextTrackCreation() {
        val fakeAudioTrackExtensions = FakeAudioTrackExtensionsWrapper()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, fakeAudioTrackExtensions, params)
        val entity = createTestEntity()
        val config = TEST_OUTPUT_CONFIG

        val outputProvider = component.getAudioOutputProvider()
        val audioTrackOutput = outputProvider.getAudioOutput(config) as AudioTrackAudioOutput
        val audioTrack = audioTrackOutput.audioTrack

        assertThat(component.onAttach(entity)).isTrue()
        assertThat(fakeAudioTrackExtensions.pointSourceParamsMap[audioTrack]).isEqualTo(params)
        assertThat(fakeAudioTrackExtensions.entityMap[audioTrack]).isEqualTo(entity)

        outputProvider.getAudioOutput(config)

        assertThat(fakeAudioTrackExtensions.pointSourceParamsBuilderMap.values).contains(params)
        assertThat(fakeAudioTrackExtensions.entityBuilderMap.values).contains(entity)
    }

    @Test
    fun onAttach_returnsFalse_ifNotAndroidXrEntity() {
        val fakeAudioTrackExtensions = FakeAudioTrackExtensionsWrapper()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, fakeAudioTrackExtensions, params)
        val mockEntity = mock<Entity>()

        assertThat(component.onAttach(mockEntity)).isFalse()
    }

    @Test
    fun onDetach_clearsAttachedEntity() {
        val fakeAudioTrackExtensions = FakeAudioTrackExtensionsWrapper()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, fakeAudioTrackExtensions, params)
        val entity = createTestEntity()

        val config = TEST_OUTPUT_CONFIG

        val outputProvider = component.getAudioOutputProvider()
        val audioTrackOutput = outputProvider.getAudioOutput(config) as AudioTrackAudioOutput
        val audioTrack = audioTrackOutput.audioTrack

        component.onAttach(entity)
        component.onDetach(entity)

        assertThat(fakeAudioTrackExtensions.pointSourceParamsMap[audioTrack]).isEqualTo(params)
        assertThat(fakeAudioTrackExtensions.entityMap[audioTrack]).isNull()
    }

    @Test
    fun onReattach_attachesToNewEntity() {
        val fakeAudioTrackExtensions = FakeAudioTrackExtensionsWrapper()
        val params = PointSourceParams()
        val component = PositionalAudioComponentImpl(activity, fakeAudioTrackExtensions, params)
        val entity1 = createTestEntity()
        val entity2 = createTestEntity()

        val config = TEST_OUTPUT_CONFIG

        val outputProvider = component.getAudioOutputProvider()
        val audioTrackOutput = outputProvider.getAudioOutput(config) as AudioTrackAudioOutput
        val audioTrack = audioTrackOutput.audioTrack

        component.onAttach(entity1)
        component.onDetach(entity1)
        component.onAttach(entity2)

        assertThat(fakeAudioTrackExtensions.pointSourceParamsMap[audioTrack]).isEqualTo(params)
        assertThat(fakeAudioTrackExtensions.entityMap[audioTrack]).isEqualTo(entity2)
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
