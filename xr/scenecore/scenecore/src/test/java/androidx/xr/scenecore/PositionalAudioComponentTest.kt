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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.testing.PositionalAudioComponentTester
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PositionalAudioComponentTest {

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
    fun addComponent_addsRuntimePositionalAudioComponent() {
        val entity = Entity.create(session, "test")
        val params = PointSourceParams()
        val component = PositionalAudioComponent.create(session, params)

        assertThat(entity.addComponent(component)).isTrue()
    }

    @Test
    fun addComponent_cannotBeAddedToTwoEntitiesSimultaneously() {
        val firstEntity = Entity.create(session, "test")
        val secondEntity = Entity.create(session, "test")
        val params = PointSourceParams()
        val component = PositionalAudioComponent.create(session, params)

        assertThat(firstEntity.addComponent(component)).isTrue()

        assertThat(secondEntity.addComponent(component)).isFalse()
    }

    @Test
    fun addComponent_canBeAddedToSecondEntityAfterRemovingFirst() {
        val firstEntity = Entity.create(session, "test")
        val secondEntity = Entity.create(session, "test")
        val params = PointSourceParams()
        val component = PositionalAudioComponent.create(session, params)

        assertThat(firstEntity.addComponent(component)).isTrue()
        assertThat(firstEntity.getComponents()).containsExactly(component)

        firstEntity.removeComponent(component)
        assertThat(firstEntity.getComponents()).doesNotContain(component)

        assertThat(secondEntity.addComponent(component)).isTrue()
    }

    @Test
    fun pointSourceParams_updatesParamsForFuturePlays() {
        val params = PointSourceParams()
        val component = PositionalAudioComponent.create(session, params)
        val tester = scenecoreTestRule.createTester<PositionalAudioComponentTester>(component)

        assertThat(component.pointSourceParams).isEqualTo(params)

        val newParams = PointSourceParams()
        component.pointSourceParams = newParams

        assertThat(component.pointSourceParams).isEqualTo(newParams)

        assertThat(tester.pointSourceParams).isEqualTo(newParams)
    }

    @Test
    fun audioOutputProvider_returnsProvider() {
        val params = PointSourceParams()
        val component = PositionalAudioComponent.create(session, params)
        val tester = scenecoreTestRule.createTester<PositionalAudioComponentTester>(component)
        val audioOutputProvider: AudioOutputProvider =
            AudioTrackAudioOutputProvider.Builder(session.context).build()

        tester.audioOutputProvider = audioOutputProvider

        assertThat(component.audioOutputProvider).isEqualTo(audioOutputProvider)
        assertThat(tester.audioOutputProvider).isEqualTo(audioOutputProvider)
    }
}
