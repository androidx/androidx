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
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.runtime.PointSourceParams as RtPointSourceParams
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SoundFieldAttributes as RtSoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatializerConstants as RtSpatializerConstants
import androidx.xr.scenecore.testing.FakeSceneRuntime
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SpatialAudioTrackTest {

    private lateinit var sceneRuntime: SceneRuntime
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()

    private lateinit var session: Session

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        sceneRuntime = session.sceneRuntime
    }

    @Test
    fun setWithPointSource_callsRuntimeAudioTrackSetPointSource() {
        val track = AudioTrack.Builder().build()

        val entity = Entity.create(session, "test")
        val pointSourceParams = PointSourceParams(entity)

        SpatialAudioTrack.setPointSourceParams(session, track, pointSourceParams)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime
        val rtAudioTrackExtensionsWrapper = fakeSceneRuntime.audioTrackExtensionsWrapper
        val storedRtParams = rtAudioTrackExtensionsWrapper.pointSourceParamsMap[track]

        assertThat(storedRtParams).isNotNull()
        assertThat(storedRtParams?.entity).isEqualTo(pointSourceParams.rtPointSourceParams.entity)
        assertThat(storedRtParams?.entity).isEqualTo((entity as BaseEntity<*>).rtEntity)
    }

    @Test
    fun setWithPointSource_rethrowsIfExtensionThrows() {
        val track = AudioTrack.Builder().build()

        val entity = Entity.create(session, "test")
        val pointSourceParams = PointSourceParams(entity)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime
        fakeSceneRuntime.audioTrackExtensionsWrapper.fakeExtensionException =
            IllegalStateException("Simulated runtime failure")

        kotlin.test.assertFailsWith<IllegalStateException> {
            SpatialAudioTrack.setPointSourceParams(session, track, pointSourceParams)
        }
    }

    @Test
    fun setWithPointSource_callsRuntimeAudioTrackBuilderSetPointSource() {
        val builder = AudioTrack.Builder()

        val entity = Entity.create(session, "test")
        val pointSourceParams = PointSourceParams(entity)

        SpatialAudioTrackBuilder.setPointSourceParams(session, builder, pointSourceParams)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime
        val rtAudioTrackExtensionsWrapper = fakeSceneRuntime.audioTrackExtensionsWrapper
        val storedRtParams = rtAudioTrackExtensionsWrapper.pointSourceParamsBuilderMap[builder]

        assertThat(storedRtParams).isNotNull()
        assertThat(storedRtParams!!.entity).isEqualTo(pointSourceParams.rtPointSourceParams.entity)
    }

    @Test
    fun setWithSoundField_callsRuntimeAudioTrackBuilderSetSoundField() {
        val builder = AudioTrack.Builder()
        val soundFieldAttributes =
            SoundFieldAttributes(SpatializerConstants.AmbisonicsOrder.FIRST_ORDER)

        SpatialAudioTrackBuilder.setSoundFieldAttributes(session, builder, soundFieldAttributes)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime
        val rtAudioTrackExtensionsWrapper = fakeSceneRuntime.audioTrackExtensionsWrapper

        assertThat(rtAudioTrackExtensionsWrapper.soundFieldAttributesBuilderMap[builder])
            .isEqualTo(soundFieldAttributes.rtSoundFieldAttributes)
    }

    @Test
    fun getSourceType_callsRuntimeAudioTrackGetSourceType() {
        val audioTrack = AudioTrack.Builder().build()
        val expectedSourceType = RtSpatializerConstants.SOURCE_TYPE_POINT_SOURCE
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime
        fakeSceneRuntime.audioTrackExtensionsWrapper.spatialSourceTypeMap[audioTrack] =
            expectedSourceType
        val sourceType = SpatialAudioTrack.getSpatialSourceType(session, audioTrack)

        assertThat(sourceType.sourceTypeToRt()).isEqualTo(expectedSourceType)
    }

    @Test
    fun getPointSourceParams_callsRuntimeAudioTrackGetPointSourceParams() {
        val audioTrack = AudioTrack.Builder().build()
        val entity = Entity.create(session, "test")

        val temp: BaseEntity<*> = entity as BaseEntity<*>
        val rtEntity = temp.rtEntity!!
        val rtPointSourceParams = RtPointSourceParams(rtEntity)

        sceneRuntime.audioTrackExtensionsWrapper.setPointSourceParams(
            audioTrack,
            rtPointSourceParams,
        )
        val pointSourceParams = SpatialAudioTrack.getPointSourceParams(session, audioTrack)

        assertThat((pointSourceParams!!.entity as BaseEntity<*>).rtEntity).isEqualTo(rtEntity)
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
        val rtSoundFieldAttributes =
            RtSoundFieldAttributes(expectedAmbisonicsOrder.sourceTypeToRt())
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime
        fakeSceneRuntime.audioTrackExtensionsWrapper.setSoundFieldAttributes(
            audioTrack,
            rtSoundFieldAttributes,
        )
        val soundFieldAttributes = SpatialAudioTrack.getSoundFieldAttributes(session, audioTrack)

        assertThat(soundFieldAttributes?.order).isEqualTo(expectedAmbisonicsOrder)
    }

    @Test
    fun getSoundFieldAttributes_returnsNullIfNotInRuntime() {
        val audioTrack = AudioTrack.Builder().build()

        val soundFieldAttributes = SpatialAudioTrack.getSoundFieldAttributes(session, audioTrack)

        assertThat(soundFieldAttributes?.order).isNull()
    }
}
