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

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.PositionalAudioComponent
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
class PositionalAudioComponentTesterTest {
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
        val params = PointSourceParams()
        val component = PositionalAudioComponent.create(session, params)
        val tester1 = PositionalAudioComponentTester.create(component)
        val tester2 = PositionalAudioComponentTester.create(component)

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }

    @Test
    fun pointSourceParams_returnsCorrectValue() {
        val params1 = PointSourceParams()
        val params2 = PointSourceParams()
        val component = PositionalAudioComponent.create(session, params1)
        val tester = testRule.createTester<PositionalAudioComponentTester>(component)

        assertThat(tester.pointSourceParams).isEqualTo(params1)
        assertThat(tester.pointSourceParams).isNotEqualTo(params2)

        component.pointSourceParams = params2
        assertThat(tester.pointSourceParams).isEqualTo(params2)
    }

    @Test
    fun audioOutputProvider_delegatesToInternalFake() {
        val params = PointSourceParams()
        val component = PositionalAudioComponent.create(session, params)
        val tester = testRule.createTester<PositionalAudioComponentTester>(component)
        val fakeProvider = AudioTrackAudioOutputProvider.Builder(activity).build()

        tester.audioOutputProvider = fakeProvider

        assertThat(component.audioOutputProvider).isSameInstanceAs(fakeProvider)
        assertThat(tester.audioOutputProvider).isSameInstanceAs(fakeProvider)
    }
}
