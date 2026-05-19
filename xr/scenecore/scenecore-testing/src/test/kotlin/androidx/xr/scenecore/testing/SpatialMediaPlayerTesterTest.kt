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

import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.SoundFieldAttributes
import androidx.xr.scenecore.SpatialMediaPlayer
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
class SpatialMediaPlayerTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var session: Session

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        session = (result as SessionCreateSuccess).session
    }

    @Test
    fun equalsAndHashCode_behaveCorrectly() {
        val mediaPlayer = MediaPlayer()
        val tester1 = testRule.createTester(mediaPlayer)
        val tester2 = testRule.createTester(mediaPlayer)

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }

    @Test
    fun pointSourceParams_returnsCorrectParams() {
        val mediaPlayer = MediaPlayer()
        val tester = testRule.createTester(mediaPlayer)
        val params = PointSourceParams()
        val entity = Entity.create(session, "testEntity")

        assertThat(tester.pointSourceParams).isNull()

        SpatialMediaPlayer.setPointSourceParams(session, mediaPlayer, params, entity)

        assertThat(tester.pointSourceParams).isEqualTo(params)
    }

    @Test
    fun isPointSource_returnsCorrectValue() {
        val mediaPlayer = MediaPlayer()
        val tester = testRule.createTester(mediaPlayer)
        val params = PointSourceParams()
        val entity1 = Entity.create(session, "entity1")
        val entity2 = Entity.create(session, "entity2")

        assertThat(tester.isPointSource(entity1)).isFalse()

        SpatialMediaPlayer.setPointSourceParams(session, mediaPlayer, params, entity1)

        assertThat(tester.isPointSource(entity1)).isTrue()
        assertThat(tester.isPointSource(entity2)).isFalse()
    }

    @Test
    fun soundFieldAttributes_returnsCorrectAttributes() {
        val mediaPlayer = MediaPlayer()
        val tester = testRule.createTester(mediaPlayer)
        val attributes = SoundFieldAttributes(SpatializerConstants.AmbisonicsOrder.FIRST_ORDER)

        assertThat(tester.soundFieldAttributes).isNull()

        SpatialMediaPlayer.setSoundFieldAttributes(session, mediaPlayer, attributes)

        val retrievedAttributes = tester.soundFieldAttributes
        assertThat(retrievedAttributes).isNotNull()
        assertThat(retrievedAttributes?.order)
            .isEqualTo(SpatializerConstants.AmbisonicsOrder.FIRST_ORDER)
    }
}
