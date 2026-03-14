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

package androidx.xr.scenecore

import androidx.activity.ComponentActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.testing.FakePositionalAudioComponent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PositionalAudioComponentTest {

    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var session: Session

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @Test
    fun addComponent_addsRuntimePositionalAudioComponent() {
        val entity = Entity.create(session, "test")
        val params = PointSourceParams()
        val component = PositionalAudioComponent.create(session, params)

        assertThat(entity.addComponent(component)).isTrue()
        assertThat((entity as BaseEntity<*>).rtEntity?.getComponents()[0])
            .isInstanceOf(FakePositionalAudioComponent::class.java)
    }

    @Test
    fun addComponent_cannotBeAddedToTwoComponentsSimultaneously() {
        val firstEntity = Entity.create(session, "test")
        val secondEntity = Entity.create(session, "test")
        val params = PointSourceParams()
        val component = PositionalAudioComponent.create(session, params)

        assertThat(firstEntity.addComponent(component)).isTrue()
        assertThat((firstEntity as BaseEntity<*>).rtEntity?.getComponents()[0])
            .isInstanceOf(FakePositionalAudioComponent::class.java)

        assertThat(secondEntity.addComponent(component)).isFalse()
    }

    @Test
    fun addComponent_canBeAddedToSecondComponentAfterRemovingFirst() {
        val firstEntity = Entity.create(session, "test")
        val secondEntity = Entity.create(session, "test")
        val params = PointSourceParams()
        val component = PositionalAudioComponent.create(session, params)

        assertThat(firstEntity.addComponent(component)).isTrue()
        assertThat((firstEntity as BaseEntity<*>).rtEntity?.getComponents()[0])
            .isInstanceOf(FakePositionalAudioComponent::class.java)

        firstEntity.removeComponent(component)
        assertThat(firstEntity.rtEntity?.getComponents()).hasSize(0)

        assertThat(secondEntity.addComponent(component)).isTrue()
        assertThat((secondEntity as BaseEntity<*>).rtEntity?.getComponents()[0])
            .isInstanceOf(FakePositionalAudioComponent::class.java)
    }

    @Test
    fun setPointSourceParams_setsOnRuntime() {
        val params = PointSourceParams()
        val component = PositionalAudioComponent.create(session, params)

        val newParams = PointSourceParams()

        component.setPointSourceParams(newParams)

        val fakeComponent = component.rtComponent as FakePositionalAudioComponent
        assertThat(fakeComponent.params).isEqualTo(newParams.rtPointSourceParams)
    }

    @Test
    fun getAudioOutputProvider_returnsProvider() {
        val params = PointSourceParams()
        val component = PositionalAudioComponent.create(session, params)

        val provider = component.getAudioOutputProvider()

        val fakeComponent = component.rtComponent as FakePositionalAudioComponent
        assertThat(fakeComponent.getAudioOutputProviderCount).isEqualTo(1)
        assertThat(provider).isNotNull()
    }
}
