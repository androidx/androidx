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

import android.media.MediaPlayer
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
class SpatialMediaPlayerTest {

    @get:Rule val scenecoreTestRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var activity: ComponentActivity
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var session: Session

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

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
    fun setWithPointSource_callsRuntimeMediaPlayerSetPointSource() {
        val mediaPlayer = MediaPlayer()
        val tester = scenecoreTestRule.createTester(mediaPlayer)

        val entity = Entity.create(session, "test")
        val pointSourceParams = PointSourceParams()

        SpatialMediaPlayer.setPointSourceParams(session, mediaPlayer, pointSourceParams, entity)

        assertThat(tester.isCurrentPointSource(entity)).isTrue()
        assertThat(tester.pointSourceParams).isEqualTo(pointSourceParams)
    }

    @Test
    fun setWithSoundField_callsRuntimeMediaPlayerSetSoundField() {
        val mediaPlayer = MediaPlayer()
        val tester = scenecoreTestRule.createTester(mediaPlayer)

        val soundFieldAttributes =
            SoundFieldAttributes(SpatializerConstants.AmbisonicsOrder.THIRD_ORDER)

        SpatialMediaPlayer.setSoundFieldAttributes(session, mediaPlayer, soundFieldAttributes)

        assertThat(tester.soundFieldAttributes?.order).isEqualTo(soundFieldAttributes.order)
    }
}
