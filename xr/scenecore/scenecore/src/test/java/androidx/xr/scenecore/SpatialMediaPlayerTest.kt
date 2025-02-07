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
import android.media.MediaPlayer
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
class SpatialMediaPlayerTest {

    private var mockRuntime: JxrPlatformAdapter = mock()
    private var mockRtMediaPlayerExtensions: JxrPlatformAdapter.MediaPlayerExtensionsWrapper =
        mock()

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

        mockRtMediaPlayerExtensions = mock()
        whenever(mockRuntime.mediaPlayerExtensionsWrapper).thenReturn(mockRtMediaPlayerExtensions)
        session = Session.create(activity, mockRuntime)
    }

    @Test
    fun setWithPointSource_callsRuntimeMediaPlayerSetPointSource() {
        val mediaPlayer = MediaPlayer()

        val entity = ContentlessEntity.create(session, "test")
        val pointSourceAttributes = PointSourceAttributes(entity)

        SpatialMediaPlayer.setPointSourceAttributes(session, mediaPlayer, pointSourceAttributes)

        verify(mockRtMediaPlayerExtensions)
            .setPointSourceAttributes(
                eq(mediaPlayer),
                argWhere<JxrPlatformAdapter.PointSourceAttributes> {
                    it.entity == mockContentlessEntity
                },
            )
    }

    @Test
    fun setWithSoundField_callsRuntimeMediaPlayerSetSoundField() {
        val mediaPlayer = MediaPlayer()

        val soundFieldAttributes =
            SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER)

        SpatialMediaPlayer.setSoundFieldAttributes(session, mediaPlayer, soundFieldAttributes)

        verify(mockRtMediaPlayerExtensions)
            .setSoundFieldAttributes(
                eq(mediaPlayer),
                argWhere<JxrPlatformAdapter.SoundFieldAttributes> {
                    it.ambisonicsOrder == SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER
                },
            )
    }
}
