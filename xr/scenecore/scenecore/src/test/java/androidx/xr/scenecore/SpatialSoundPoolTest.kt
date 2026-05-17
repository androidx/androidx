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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore

import android.media.SoundPool
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Unit tests for the JXRCore SDK SpatialSoundPool Interface. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SpatialSoundPoolTest {

    @get:Rule val scenecoreTestRule = SceneCoreTestRule()

    private lateinit var activity: ComponentActivity
    private lateinit var session: Session

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
        val testDispatcher = StandardTestDispatcher()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @Test
    fun playWithPointSource_callsRuntimeSoundPoolPlayPointSource() {
        val expectedStreamId = 1234
        val soundPool = SoundPool.Builder().build()
        val entity = Entity.create(session, "test", parent = session.scene.activitySpace)
        val pointSourceAttributes = PointSourceParams()

        scenecoreTestRule.spatialSoundPoolTester.playAsPointSourceResult = expectedStreamId

        val actualStreamId =
            SpatialSoundPool.play(
                session,
                soundPool,
                TEST_SOUND_ID,
                pointSourceAttributes,
                entity,
                TEST_VOLUME,
                TEST_PRIORITY,
                TEST_LOOP,
                TEST_RATE,
            )

        assertThat(actualStreamId).isEqualTo(expectedStreamId)
    }

    @Test
    fun playWithSoundField_callsRuntimeSoundPoolPlaySoundField() {
        val expectedStreamId = 5678
        val soundPool = SoundPool.Builder().build()
        val soundFieldAttributes =
            SoundFieldAttributes(SpatializerConstants.AmbisonicsOrder.FIRST_ORDER)

        scenecoreTestRule.spatialSoundPoolTester.playAsSoundFieldResult = expectedStreamId

        val actualStreamId =
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

        assertThat(actualStreamId).isEqualTo(expectedStreamId)
    }

    @Test
    fun getSourceType_returnsRuntimeSoundPoolGetSourceType() {
        val expected = SpatializerConstants.SourceType.SOUND_FIELD
        val soundPool = SoundPool.Builder().build()

        scenecoreTestRule.spatialSoundPoolTester.spatialSourceType =
            SpatializerConstants.SourceType.SOUND_FIELD

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
