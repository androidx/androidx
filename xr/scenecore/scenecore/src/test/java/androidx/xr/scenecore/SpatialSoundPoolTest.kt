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
import android.media.SoundPool
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ActivitySpace as RtActivitySpace
import androidx.xr.runtime.internal.Entity as RtEntity
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.PointSourceParams as RtPointSourceParams
import androidx.xr.runtime.internal.SoundPoolExtensionsWrapper as RtSoundPoolExtensionsWrapper
import androidx.xr.runtime.internal.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.runtime.internal.SpatializerConstants as RtSpatializerConstants
import androidx.xr.runtime.testing.FakeRuntimeFactory
import androidx.xr.scenecore.SpatializerConstants.Companion.AMBISONICS_ORDER_FIRST_ORDER
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

/** Unit tests for the JXRCore SDK SpatialSoundPool Interface. */
@RunWith(RobolectricTestRunner::class)
class SpatialSoundPoolTest {

    private val fakeRuntimeFactory = FakeRuntimeFactory()
    private var mockRuntime: JxrPlatformAdapter = mock()
    private var mockRtSoundPoolExtensions: RtSoundPoolExtensionsWrapper = mock()

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

        mockRtSoundPoolExtensions = mock()
        whenever(mockRuntime.soundPoolExtensionsWrapper).thenReturn(mockRtSoundPoolExtensions)
        session = Session(activity, fakeRuntimeFactory.createRuntime(activity), mockRuntime)
    }

    @Test
    fun playWithPointSource_callsRuntimeSoundPoolPlayPointSource() {
        val expectedStreamId = 1234

        val soundPool = SoundPool.Builder().build()
        val entity = GroupEntity.create(session, "test")
        val pointSourceAttributes = PointSourceParams(entity)
        whenever(
                mockRtSoundPoolExtensions.play(
                    eq(soundPool),
                    any(),
                    any<RtPointSourceParams>(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(expectedStreamId)

        val actualStreamId =
            SpatialSoundPool.play(
                session,
                soundPool,
                TEST_SOUND_ID,
                pointSourceAttributes,
                TEST_VOLUME,
                TEST_PRIORITY,
                TEST_LOOP,
                TEST_RATE,
            )
        verify(mockRtSoundPoolExtensions)
            .play(
                eq(soundPool),
                eq(TEST_SOUND_ID),
                argWhere<RtPointSourceParams> { it.entity == mockGroupEntity },
                eq(TEST_VOLUME),
                eq(TEST_PRIORITY),
                eq(TEST_LOOP),
                eq(TEST_RATE),
            )
        assertThat(actualStreamId).isEqualTo(expectedStreamId)
    }

    @Test
    fun playWithSoundField_callsRuntimeSoundPoolPlaySoundField() {
        val soundPool = SoundPool.Builder().build()
        val soundFieldAttributes = SoundFieldAttributes(AMBISONICS_ORDER_FIRST_ORDER)

        assertThat(
                SpatialSoundPool.play(
                    session,
                    soundPool,
                    TEST_SOUND_ID,
                    soundFieldAttributes,
                    TEST_VOLUME,
                    TEST_PRIORITY,
                    TEST_LOOP,
                    TEST_RATE,
                )
            )
            .isEqualTo(0)
    }

    @Test
    fun getSourceType_returnsRuntimeSoundPoolGetSourceType() {
        val expected = SpatializerConstants.SOURCE_TYPE_SOUND_FIELD
        val soundPool = SoundPool.Builder().build()

        whenever(mockRtSoundPoolExtensions.getSpatialSourceType(any(), any()))
            .thenReturn(RtSpatializerConstants.SOURCE_TYPE_SOUND_FIELD)

        assertThat(SpatialSoundPool.getSpatialSourceType(session, soundPool, TEST_STREAM_ID))
            .isEqualTo(expected)
    }

    companion object {
        const val TEST_SOUND_ID = 0
        const val TEST_VOLUME = 1F
        const val TEST_PRIORITY = 0
        const val TEST_LOOP = 0
        const val TEST_RATE = 1F
        const val TEST_STREAM_ID = 10
    }
}
