/*
 * Copyright 2025 The Android Open Source Project
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

import android.media.AudioTrack
import androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatializerConstants
import androidx.xr.scenecore.spatial.core.SpatialCoreXrExtensionsHolderProvider.Companion.extensionsLegacy
import com.android.extensions.xr.media.AudioTrackExtensions
import com.android.extensions.xr.media.ShadowAudioTrackExtensions
import com.android.extensions.xr.media.SpatializerExtensions
import com.android.extensions.xr.media.XrSpatialAudioExtensions
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class AudioTrackExtensionsWrapperImplTest {
    private val xrExtensions = extensionsLegacy
    private val spatialAudioExtensions: XrSpatialAudioExtensions =
        xrExtensions.xrSpatialAudioExtensions
    private val audioTrackExtensions: AudioTrackExtensions =
        spatialAudioExtensions.audioTrackExtensions
    private val sceneNodeRegistry = SceneNodeRegistry()
    private val audioTrackBuilder = mock<AudioTrack.Builder>()

    @Before
    fun setUp() {
        // Clear the sound fields before each test.
        // Because the audioTrackExtensions are fetched from the XrExtensions singleton it is
        // reused across tests.
        // TODO(b/401557718): Consider adding a reset method to the XrExtensions shadow.
        ShadowAudioTrackExtensions.extract(audioTrackExtensions).setSoundFieldAttributes(null)
    }

    @Test
    fun setPointSourceParams_callsExtensionsSetPointSourceParams() {
        val track = mock<AudioTrack>()
        val fakeNode = xrExtensions.createNode()
        val entity = mock<AndroidXrEntity>()
        whenever(entity.getNode()).thenReturn(fakeNode)
        val expectedRtParams = PointSourceParams()
        val wrapper: AudioTrackExtensionsWrapper =
            AudioTrackExtensionsWrapperImpl(audioTrackExtensions)
        wrapper.setPointSourceParams(track, expectedRtParams, entity)

        assertThat(audioTrackExtensions.getPointSourceParams(track).node).isEqualTo(fakeNode)
    }

    @Test
    fun setPointSourceParamsBuilder_callsExtensionsSetPointSourceParamsBuilder() {
        val track = mock<AudioTrack>()
        val fakeNode = xrExtensions.createNode()
        val entity = mock<AndroidXrEntity>()
        whenever(entity.getNode()).thenReturn(fakeNode)
        val expectedRtParams = PointSourceParams()
        val wrapper: AudioTrackExtensionsWrapper =
            AudioTrackExtensionsWrapperImpl(audioTrackExtensions)
        val actual = wrapper.setPointSourceParams(audioTrackBuilder, expectedRtParams, entity)

        assertThat(actual).isEqualTo(audioTrackBuilder)
        assertThat(audioTrackExtensions.getPointSourceParams(track).node).isEqualTo(fakeNode)
    }

    @Test
    fun setSoundFieldAttr_callsExtensionsSetSoundFieldAttr() {
        val expectedAmbisonicOrder = SpatializerExtensions.AMBISONICS_ORDER_THIRD_ORDER
        val expectedRtAttr = SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER)
        val wrapper: AudioTrackExtensionsWrapper =
            AudioTrackExtensionsWrapperImpl(audioTrackExtensions)
        val actual = wrapper.setSoundFieldAttributes(audioTrackBuilder, expectedRtAttr)

        assertThat(actual).isEqualTo(audioTrackBuilder)
        assertThat(audioTrackExtensions.getSoundFieldAttributes(mock<AudioTrack>()).ambisonicsOrder)
            .isEqualTo(expectedAmbisonicOrder)
    }

    @Test
    fun getPointSourceParams_callsExtensionsGetPointSourceParams() {
        val track = mock<AudioTrack>()

        val fakeNode = xrExtensions.createNode()
        val entity = mock<AndroidXrEntity>()
        whenever(entity.getNode()).thenReturn(fakeNode)
        sceneNodeRegistry.setEntityForNode(fakeNode, entity)
        val wrapper: AudioTrackExtensionsWrapper =
            AudioTrackExtensionsWrapperImpl(audioTrackExtensions)
        val actual = wrapper.getPointSourceParams(track)

        // TODO: Compare point source params once additional parameters are added.
        assertThat(actual).isNotNull()
    }

    @Test
    fun getPointSourceParams_returnsNullIfNotInExtensions() {
        val track = mock<AudioTrack>()
        val wrapper: AudioTrackExtensionsWrapper =
            AudioTrackExtensionsWrapperImpl(audioTrackExtensions)
        val actual = wrapper.getPointSourceParams(track)

        assertThat(actual).isNull()
    }

    @Test
    fun getSoundFieldAttributes_callsExtensionsGetSoundFieldAttributes() {
        val track = mock<AudioTrack>()

        audioTrackExtensions.setSoundFieldAttributes(
            AudioTrack.Builder(),
            com.android.extensions.xr.media.SoundFieldAttributes.Builder()
                .setAmbisonicsOrder(SpatializerExtensions.AMBISONICS_ORDER_THIRD_ORDER)
                .build(),
        )
        val expectedRtAttr = SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER)
        val wrapper: AudioTrackExtensionsWrapper =
            AudioTrackExtensionsWrapperImpl(audioTrackExtensions)
        val actual = wrapper.getSoundFieldAttributes(track)

        assertThat(actual!!.ambisonicsOrder).isEqualTo(expectedRtAttr.ambisonicsOrder)
    }

    @Test
    fun getSoundFieldAttributes_returnsNullIfNotInExtensions() {
        val track = mock<AudioTrack>()
        val wrapper: AudioTrackExtensionsWrapper =
            AudioTrackExtensionsWrapperImpl(audioTrackExtensions)
        val actual = wrapper.getSoundFieldAttributes(track)

        assertThat(actual).isNull()
    }

    @Test
    fun getSourceType_returnsFromExtensions() {
        val track = mock<AudioTrack>()
        val expected = SpatializerConstants.SOURCE_TYPE_SOUND_FIELD
        ShadowAudioTrackExtensions.extract(audioTrackExtensions).setSourceType(expected)
        val wrapper: AudioTrackExtensionsWrapper =
            AudioTrackExtensionsWrapperImpl(audioTrackExtensions)
        val actualSourceType = wrapper.getSpatialSourceType(track)

        assertThat(actualSourceType).isEqualTo(expected)
    }
}
