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

import android.media.AudioTrack
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.SoundFieldAttributes
import androidx.xr.scenecore.SpatialAudioTrackBuilder
import androidx.xr.scenecore.SpatializerConstants
import androidx.xr.scenecore.testing.internal.FakeSceneRuntime
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
class SpatialAudioTrackBuilderTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()
    private val builder = AudioTrack.Builder()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session
    private lateinit var underTest: SpatialAudioTrackBuilderTester

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
        underTest = testRule.spatialAudioTrackBuilderTester
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun getPointSourceParams_setPointSourceParams_setsSuccessfully() {
        val pointSourceParams = PointSourceParams()
        val entity = Entity.create(session, "test")

        assertThat(underTest.getPointSourceParams(builder)).isNull()

        SpatialAudioTrackBuilder.setPointSourceParams(session, builder, pointSourceParams, entity)

        assertThat(underTest.getPointSourceParams(builder)).isNotNull()
    }

    @Test
    fun getSoundFieldAttributes_setSoundFieldAttributes_returnsSoundFieldAttributesSetViaPublicApi() {
        val expectedSoundFieldAttributes =
            SoundFieldAttributes(SpatializerConstants.AmbisonicsOrder.THIRD_ORDER)

        SpatialAudioTrackBuilder.setSoundFieldAttributes(
            session,
            builder,
            expectedSoundFieldAttributes,
        )

        val actualAttributes = underTest.getSoundFieldAttributes(builder)
        assertThat(actualAttributes).isNotNull()
        assertThat(actualAttributes?.order).isEqualTo(expectedSoundFieldAttributes.order)
    }

    @Test
    fun isSoundPlayedOnEntity_setPointSourceParams_returnsTrueForConfiguredEntity() {
        val entity = Entity.create(session, "test")
        val otherEntity = Entity.create(session, "other_entity")
        val pointSourceParams = PointSourceParams()

        SpatialAudioTrackBuilder.setPointSourceParams(session, builder, pointSourceParams, entity)

        assertThat(underTest.isCurrentPointSource(builder, entity)).isTrue()
        assertThat(underTest.isCurrentPointSource(builder, otherEntity)).isFalse()
    }

    @Test
    fun equalsAndHashCode_behaveCorrectlyForSameBuilder() {
        val tester1 =
            SpatialAudioTrackBuilderTester(
                requireNotNull(FakeSceneRuntime.instance).audioTrackExtensionsWrapper
            )
        val tester2 =
            SpatialAudioTrackBuilderTester(
                requireNotNull(FakeSceneRuntime.instance).audioTrackExtensionsWrapper
            )

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }
}
