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

import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.runtime.SceneRuntime
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
class SpatialMediaPlayerTest {

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
    fun setWithPointSource_callsRuntimeMediaPlayerSetPointSource() {
        val mediaPlayer = MediaPlayer()

        val entity = Entity.create(session, "test")
        val pointSourceAttributes = PointSourceParams(entity)

        SpatialMediaPlayer.setPointSourceParams(session, mediaPlayer, pointSourceAttributes)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime
        val fakeMediaPlayerExtensionsWrapper = fakeSceneRuntime.mediaPlayerExtensionsWrapper

        assertThat(fakeMediaPlayerExtensionsWrapper.pointSourceParams[mediaPlayer]?.entity)
            .isEqualTo(pointSourceAttributes.rtPointSourceParams.entity)
    }

    @Test
    fun setWithSoundField_callsRuntimeMediaPlayerSetSoundField() {
        val mediaPlayer = MediaPlayer()

        val soundFieldAttributes =
            SoundFieldAttributes(SpatializerConstants.AmbisonicsOrder.THIRD_ORDER)

        SpatialMediaPlayer.setSoundFieldAttributes(session, mediaPlayer, soundFieldAttributes)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime
        val fakeMediaPlayerExtensionsWrapper = fakeSceneRuntime.mediaPlayerExtensionsWrapper

        assertThat(fakeMediaPlayerExtensionsWrapper.soundFieldAttributes[mediaPlayer])
            .isEqualTo(soundFieldAttributes.rtSoundFieldAttributes)
    }
}
