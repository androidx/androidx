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

    private var mockRuntime: JxrPlatformAdapter = mock()
    private var mockRtAudioTrackExtensions: JxrPlatformAdapter.AudioTrackExtensionsWrapper = mock()

    private val mockContentlessEntity = mock<JxrPlatformAdapter.Entity>()
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()

    private lateinit var session: Session

    @Before
    fun setUp() {
        mockRuntime.stub {
            on { spatialEnvironment } doReturn mock()
            on { activitySpace } doReturn mock()
            on { activitySpaceRootImpl } doReturn mock()
            on { headActivityPose } doReturn mock()
            on { perceptionSpaceActivityPose } doReturn mock()
            on { mainPanelEntity } doReturn mock()
            on { createEntity(any(), any(), any()) } doReturn mockContentlessEntity
        }

        mockRtAudioTrackExtensions = mock()
        whenever(mockRuntime.audioTrackExtensionsWrapper).thenReturn(mockRtAudioTrackExtensions)
        session = Session.create(activity, mockRuntime)
    }

    @Test
    fun setWithPointSource_callsRuntimeAudioTrackBuilderSetPointSource() {
        val builder = AudioTrack.Builder()

        val entity = ContentlessEntity.create(session, "test")
        val pointSourceAttributes = PointSourceAttributes(entity)

        whenever(
                mockRtAudioTrackExtensions.setPointSourceAttributes(
                    eq(builder),
                    any<JxrPlatformAdapter.PointSourceAttributes>(),
                )
            )
            .thenReturn(builder)

        val actualBuilder =
            SpatialAudioTrackBuilder.setPointSourceAttributes(
                session,
                builder,
                pointSourceAttributes
            )

        verify(mockRtAudioTrackExtensions)
            .setPointSourceAttributes(
                eq(builder),
                argWhere<JxrPlatformAdapter.PointSourceAttributes> {
                    it.entity == mockContentlessEntity
                },
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
                    any<JxrPlatformAdapter.SoundFieldAttributes>(),
                )
            )
            .thenReturn(builder)

        val actualBuilder =
            SpatialAudioTrackBuilder.setSoundFieldAttributes(session, builder, soundFieldAttributes)

        verify(mockRtAudioTrackExtensions)
            .setSoundFieldAttributes(
                eq(builder),
                argWhere<JxrPlatformAdapter.SoundFieldAttributes> {
                    it.ambisonicsOrder == SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER
                },
            )
        assertThat(actualBuilder).isEqualTo(builder)
    }

    @Test
    fun getSourceType_callsRuntimeAudioTrackGetSourceType() {
        val audioTrack = AudioTrack.Builder().build()
        val expectedSourceType = JxrPlatformAdapter.SpatializerConstants.SOURCE_TYPE_POINT_SOURCE

        whenever(mockRtAudioTrackExtensions.getSpatialSourceType(eq(audioTrack)))
            .thenReturn(expectedSourceType)

        val sourceType = SpatialAudioTrack.getSpatialSourceType(session, audioTrack)

        verify(mockRtAudioTrackExtensions).getSpatialSourceType(eq(audioTrack))
        assertThat(sourceType).isEqualTo(expectedSourceType)
    }

    @Test
    fun getPointSourceAttributes_callsRuntimeAudioTrackGetPointSourceAttributes() {
        val audioTrack = AudioTrack.Builder().build()
        val entity = ContentlessEntity.create(session, "test")

        val temp: BaseEntity<*> = entity as BaseEntity<*>
        val rtEntity = temp.rtEntity
        val rtPointSourceAttributes = JxrPlatformAdapter.PointSourceAttributes(rtEntity)

        whenever(mockRtAudioTrackExtensions.getPointSourceAttributes(eq(audioTrack)))
            .thenReturn(rtPointSourceAttributes)

        val pointSourceAttributes = SpatialAudioTrack.getPointSourceAttributes(session, audioTrack)

        verify(mockRtAudioTrackExtensions).getPointSourceAttributes(eq(audioTrack))
        assertThat((pointSourceAttributes!!.entity as BaseEntity<*>).rtEntity).isEqualTo(rtEntity)
    }

    @Test
    fun getPointSourceAttributes_returnsNullIfNotInRuntime() {
        val audioTrack = AudioTrack.Builder().build()

        whenever(mockRtAudioTrackExtensions.getPointSourceAttributes(eq(audioTrack)))
            .thenReturn(null)

        val pointSourceAttributes = SpatialAudioTrack.getPointSourceAttributes(session, audioTrack)

        assertThat(pointSourceAttributes).isNull()
    }

    @Test
    fun getSoundFieldAttributes_callsRuntimeAudioTrackGetPointSourceAttributes() {
        val audioTrack = AudioTrack.Builder().build()
        val expectedAmbisonicsOrder = SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER
        val rtSoundFieldAttributes =
            JxrPlatformAdapter.SoundFieldAttributes(expectedAmbisonicsOrder)

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
