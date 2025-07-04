/*
 * Copyright 2024 The Android Open Source Project
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

import android.app.Activity
import android.media.AudioTrack
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ActivitySpace as RtActivitySpace
import androidx.xr.runtime.internal.AudioTrackExtensionsWrapper as RtAudioTrackExtensionsWrapper
import androidx.xr.runtime.internal.Entity as RtEntity
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.PointSourceParams as RtPointSourceParams
import androidx.xr.runtime.internal.SoundFieldAttributes as RtSoundFieldAttributes
import androidx.xr.runtime.internal.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.runtime.internal.SpatializerConstants as RtSpatializerConstants
import androidx.xr.runtime.testing.FakeRuntimeFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpatialAudioTrackTest {

    private val fakeRuntimeFactory = FakeRuntimeFactory()
    private var mockRuntime: JxrPlatformAdapter = mock()
    private var mockRtAudioTrackExtensions: RtAudioTrackExtensionsWrapper = mock()

    private val mockGroupEntity = mock<RtEntity>()
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val mockActivitySpace = mock<RtActivitySpace>()

    private lateinit var session: Session

    @Before
    fun setUp() {
        mockRuntime.stub {
            on { spatialEnvironment } doReturn mock()
            on { activitySpace } doReturn mockActivitySpace
            on { activitySpaceRootImpl } doReturn mockActivitySpace
            on { headActivityPose } doReturn mock()
            on { perceptionSpaceActivityPose } doReturn mock()
            on { mainPanelEntity } doReturn mock()
            on { createGroupEntity(any(), any(), any()) } doReturn mockGroupEntity
            on { spatialCapabilities } doReturn RtSpatialCapabilities(0)
        }

        mockRtAudioTrackExtensions = mock()
        whenever(mockRuntime.audioTrackExtensionsWrapper).thenReturn(mockRtAudioTrackExtensions)
        session = Session(activity, fakeRuntimeFactory.createRuntime(activity), mockRuntime)
    }

    @Test
    fun setWithPointSource_callsRuntimeAudioTrackSetPointSource() {
        val track = AudioTrack.Builder().build()

        val entity = GroupEntity.create(session, "test")
        val pointSourceParams = PointSourceParams(entity)

        SpatialAudioTrack.setPointSourceParams(session, track, pointSourceParams)

        verify(mockRtAudioTrackExtensions)
            .setPointSourceParams(
                eq(track),
                argWhere<RtPointSourceParams> { it.entity == mockGroupEntity },
            )
    }

    @Test
    fun setWithPointSource_rethrowsIfExtensionThrows() {
        val track = AudioTrack.Builder().build()

        val entity = GroupEntity.create(session, "test")
        val pointSourceParams = PointSourceParams(entity)

        whenever(
                mockRtAudioTrackExtensions.setPointSourceParams(
                    eq(track),
                    any<RtPointSourceParams>(),
                )
            )
            .thenThrow(IllegalStateException("test"))

        kotlin.test.assertFailsWith<IllegalStateException> {
            SpatialAudioTrack.setPointSourceParams(session, track, pointSourceParams)
        }
    }

    @Test
    fun setWithPointSource_callsRuntimeAudioTrackBuilderSetPointSource() {
        val builder = AudioTrack.Builder()

        val entity = GroupEntity.create(session, "test")
        val pointSourceParams = PointSourceParams(entity)

        whenever(
                mockRtAudioTrackExtensions.setPointSourceParams(
                    eq(builder),
                    any<RtPointSourceParams>(),
                )
            )
            .thenReturn(builder)

        val actualBuilder =
            SpatialAudioTrackBuilder.setPointSourceParams(session, builder, pointSourceParams)

        verify(mockRtAudioTrackExtensions)
            .setPointSourceParams(
                eq(builder),
                argWhere<RtPointSourceParams> { it.entity == mockGroupEntity },
            )
        assertThat(actualBuilder).isEqualTo(builder)
    }

    @Test
    fun setWithSoundField_callsRuntimeAudioTrackBuilderSetSoundField() {
        val builder = AudioTrack.Builder()
        val soundFieldAttributes =
            SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER)

        whenever(
                mockRtAudioTrackExtensions.setSoundFieldAttributes(
                    eq(builder),
                    any<RtSoundFieldAttributes>(),
                )
            )
            .thenReturn(builder)

        val actualBuilder =
            SpatialAudioTrackBuilder.setSoundFieldAttributes(session, builder, soundFieldAttributes)

        verify(mockRtAudioTrackExtensions)
            .setSoundFieldAttributes(
                eq(builder),
                argWhere<RtSoundFieldAttributes> {
                    it.ambisonicsOrder == SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER
                },
            )
        assertThat(actualBuilder).isEqualTo(builder)
    }

    @Test
    fun getSourceType_callsRuntimeAudioTrackGetSourceType() {
        val audioTrack = AudioTrack.Builder().build()
        val expectedSourceType = RtSpatializerConstants.SOURCE_TYPE_POINT_SOURCE

        whenever(mockRtAudioTrackExtensions.getSpatialSourceType(eq(audioTrack)))
            .thenReturn(expectedSourceType)

        val sourceType = SpatialAudioTrack.getSpatialSourceType(session, audioTrack)

        verify(mockRtAudioTrackExtensions).getSpatialSourceType(eq(audioTrack))
        assertThat(sourceType).isEqualTo(expectedSourceType)
    }

    @Test
    fun getPointSourceParams_callsRuntimeAudioTrackGetPointSourceParams() {
        val audioTrack = AudioTrack.Builder().build()
        val entity = GroupEntity.create(session, "test")

        val temp: BaseEntity<*> = entity as BaseEntity<*>
        val rtEntity = temp.rtEntity
        val rtPointSourceParams = RtPointSourceParams(rtEntity)

        whenever(mockRtAudioTrackExtensions.getPointSourceParams(eq(audioTrack)))
            .thenReturn(rtPointSourceParams)

        val pointSourceParams = SpatialAudioTrack.getPointSourceParams(session, audioTrack)

        verify(mockRtAudioTrackExtensions).getPointSourceParams(eq(audioTrack))
        assertThat((pointSourceParams!!.entity as BaseEntity<*>).rtEntity).isEqualTo(rtEntity)
    }

    @Test
    fun getPointSourceParams_returnsNullIfNotInRuntime() {
        val audioTrack = AudioTrack.Builder().build()

        whenever(mockRtAudioTrackExtensions.getPointSourceParams(eq(audioTrack))).thenReturn(null)

        val pointSourceParams = SpatialAudioTrack.getPointSourceParams(session, audioTrack)

        assertThat(pointSourceParams).isNull()
    }

    @Test
    fun getSoundFieldAttributes_callsRuntimeAudioTrackGetSoundFieldAttributes() {
        val audioTrack = AudioTrack.Builder().build()
        val expectedAmbisonicsOrder = SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER
        val rtSoundFieldAttributes = RtSoundFieldAttributes(expectedAmbisonicsOrder)

        whenever(mockRtAudioTrackExtensions.getSoundFieldAttributes(eq(audioTrack)))
            .thenReturn(rtSoundFieldAttributes)

        val soundFieldAttributes = SpatialAudioTrack.getSoundFieldAttributes(session, audioTrack)

        verify(mockRtAudioTrackExtensions).getSoundFieldAttributes(eq(audioTrack))
        assertThat(soundFieldAttributes?.order).isEqualTo(expectedAmbisonicsOrder)
    }

    @Test
    fun getSoundFieldAttributes_returnsNullIfNotInRuntime() {
        val audioTrack = AudioTrack.Builder().build()

        whenever(mockRtAudioTrackExtensions.getSoundFieldAttributes(eq(audioTrack)))
            .thenReturn(null)

        val soundFieldAttributes = SpatialAudioTrack.getSoundFieldAttributes(session, audioTrack)

        assertThat(soundFieldAttributes?.order).isNull()
    }
}
