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

import android.media.AudioTrack
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SpatialAudioTrackTest {

    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(
                context = activity,
                coroutineContext = testDispatcher,
                lifecycleOwner = activity as LifecycleOwner,
            )

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun setWithPointSource_callsRuntimeAudioTrackSetPointSource() {
        val track = AudioTrack.Builder().build()

        val entity = Entity.create(session, "test")
        val pointSourceParams = PointSourceParams()
        val tester = testRule.spatialAudioTrackTester

        assertThat(SpatialAudioTrack.getSpatialSourceType(session, track))
            .isEqualTo(SpatializerConstants.SourceType.DEFAULT)
        assertThat(SpatialAudioTrack.getPointSourceParams(session, track)).isNull()
        assertThat(tester.isCurrentPointSource(track, entity)).isFalse()

        tester.setSpatialSourceType(track, SpatializerConstants.SourceType.POINT_SOURCE)
        SpatialAudioTrack.setPointSourceParams(session, track, pointSourceParams, entity)

        assertThat(SpatialAudioTrack.getSpatialSourceType(session, track))
            .isEqualTo(SpatializerConstants.SourceType.POINT_SOURCE)
        assertThat(SpatialAudioTrack.getPointSourceParams(session, track)).isNotNull()
        assertThat(tester.isCurrentPointSource(track, entity)).isTrue()
    }

    @Test
    fun setWithPointSource_callsRuntimeAudioTrackBuilderSetPointSource() {
        val builder = AudioTrack.Builder()

        val entity = Entity.create(session, "test")
        val pointSourceParams = PointSourceParams()
        val tester = testRule.spatialAudioTrackBuilderTester

        assertThat(tester.getPointSourceParams(builder)).isNull()
        assertThat(tester.isCurrentPointSource(builder, entity)).isFalse()

        SpatialAudioTrackBuilder.setPointSourceParams(session, builder, pointSourceParams, entity)

        // TODO: b/426001209 - Check params equality once additional params are implemented.
        assertThat(tester.getPointSourceParams(builder)).isNotNull()
        assertThat(tester.isCurrentPointSource(builder, entity)).isTrue()
    }

    @Test
    fun setWithSoundField_rethrowsIfExtensionThrows() {
        val track = AudioTrack.Builder().build()

        val entity = Entity.create(session, "test")
        val pointSourceParams = PointSourceParams()
        val tester = testRule.spatialAudioTrackTester

        assertThat(SpatialAudioTrack.getSpatialSourceType(session, track))
            .isEqualTo(SpatializerConstants.SourceType.DEFAULT)

        tester.setSpatialSourceType(track, SpatializerConstants.SourceType.SOUND_FIELD)

        assertThat(SpatialAudioTrack.getSpatialSourceType(session, track))
            .isEqualTo(SpatializerConstants.SourceType.SOUND_FIELD)

        kotlin.test.assertFailsWith<IllegalStateException> {
            SpatialAudioTrack.setPointSourceParams(session, track, pointSourceParams, entity)
        }
    }

    @Test
    fun setWithSoundField_callsRuntimeAudioTrackBuilderSetSoundField() {
        val builder = AudioTrack.Builder()
        val soundFieldAttributes =
            SoundFieldAttributes(SpatializerConstants.AmbisonicsOrder.FIRST_ORDER)
        val tester = testRule.spatialAudioTrackBuilderTester

        assertThat(tester.getSoundFieldAttributes(builder)).isNull()

        SpatialAudioTrackBuilder.setSoundFieldAttributes(session, builder, soundFieldAttributes)
        val result = tester.getSoundFieldAttributes(builder)

        assertThat(result).isNotNull()
        assertThat(result!!.order).isEqualTo(SpatializerConstants.AmbisonicsOrder.FIRST_ORDER)
    }

    @Test
    fun getSpatialSourceType_callsRuntimeAudioTrackGetSourceType() {
        val audioTrack = AudioTrack.Builder().build()
        val expectedSourceType = SpatializerConstants.SourceType.POINT_SOURCE
        val tester = testRule.spatialAudioTrackTester

        assertThat(SpatialAudioTrack.getSpatialSourceType(session, audioTrack))
            .isEqualTo(SpatializerConstants.SourceType.DEFAULT)

        tester.setSpatialSourceType(audioTrack, expectedSourceType)

        assertThat(SpatialAudioTrack.getSpatialSourceType(session, audioTrack))
            .isEqualTo(expectedSourceType)
    }

    @Test
    fun getPointSourceParams_returnsNullIfNotInRuntime() {
        val audioTrack = AudioTrack.Builder().build()

        val pointSourceParams = SpatialAudioTrack.getPointSourceParams(session, audioTrack)

        assertThat(pointSourceParams).isNull()
    }

    @Test
    fun getSoundFieldAttributes_callsRuntimeAudioTrackGetSoundFieldAttributes() {
        val audioTrack = AudioTrack.Builder().build()
        val expectedAmbisonicsOrder = SpatializerConstants.AmbisonicsOrder.THIRD_ORDER
        val soundField = SoundFieldAttributes(expectedAmbisonicsOrder)
        val tester = testRule.spatialAudioTrackTester

        assertThat(SpatialAudioTrack.getSoundFieldAttributes(session, audioTrack)).isNull()

        tester.setSoundFieldAttributes(audioTrack, soundField)
        val soundFieldAttributes = SpatialAudioTrack.getSoundFieldAttributes(session, audioTrack)

        assertThat(soundFieldAttributes?.order).isEqualTo(expectedAmbisonicsOrder)
    }
}
