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

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.SoundFieldAttributes
import androidx.xr.scenecore.SpatialSoundPool
import androidx.xr.scenecore.SpatializerConstants
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SpatialSoundPoolTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var session: Session
    private lateinit var soundPool: SoundPool

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        session = (result as SessionCreateSuccess).session

        val attributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        soundPool = SoundPool.Builder().setMaxStreams(10).setAudioAttributes(attributes).build()
    }

    @Test
    fun equalsAndHashCode_behaveCorrectly() {
        val tester1 = testRule.spatialSoundPoolTester
        val tester2 = testRule.spatialSoundPoolTester

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }

    @Test
    fun playAsPointSourceResult_getsAndSetsValue() {
        val tester = testRule.spatialSoundPoolTester
        val entity = Entity.create(session, "testEntity")
        val params = PointSourceParams()
        val soundId = 1

        tester.playAsPointSourceResult = 42
        assertThat(tester.playAsPointSourceResult).isEqualTo(42)

        val streamId = SpatialSoundPool.play(session, soundPool, soundId, params, entity)
        assertThat(streamId).isEqualTo(42)

        tester.playAsPointSourceResult = 0
        assertThat(tester.playAsPointSourceResult).isEqualTo(0)
        val failedStreamId = SpatialSoundPool.play(session, soundPool, soundId, params, entity)
        assertThat(failedStreamId).isEqualTo(0)
    }

    @Test
    fun playAsSoundFieldResult_getsAndSetsValue() {
        val tester = testRule.spatialSoundPoolTester
        val attributes = SoundFieldAttributes(SpatializerConstants.AmbisonicsOrder.FIRST_ORDER)
        val soundId = 2

        tester.playAsSoundFieldResult = 100
        assertThat(tester.playAsSoundFieldResult).isEqualTo(100)

        val streamId = SpatialSoundPool.play(session, soundPool, soundId, attributes)
        assertThat(streamId).isEqualTo(100)

        tester.playAsSoundFieldResult = 0
        assertThat(tester.playAsSoundFieldResult).isEqualTo(0)
        val failedStreamId = SpatialSoundPool.play(session, soundPool, soundId, attributes)
        assertThat(failedStreamId).isEqualTo(0)
    }

    @Test
    fun spatialSourceType_getsAndSetsValue() {
        val tester = testRule.spatialSoundPoolTester
        val streamId = 7

        tester.spatialSourceType = SpatializerConstants.SourceType.POINT_SOURCE
        assertThat(tester.spatialSourceType).isEqualTo(SpatializerConstants.SourceType.POINT_SOURCE)
        assertThat(SpatialSoundPool.getSpatialSourceType(session, soundPool, streamId))
            .isEqualTo(SpatializerConstants.SourceType.POINT_SOURCE)

        tester.spatialSourceType = SpatializerConstants.SourceType.SOUND_FIELD
        assertThat(tester.spatialSourceType).isEqualTo(SpatializerConstants.SourceType.SOUND_FIELD)
        assertThat(SpatialSoundPool.getSpatialSourceType(session, soundPool, streamId))
            .isEqualTo(SpatializerConstants.SourceType.SOUND_FIELD)
    }
}
