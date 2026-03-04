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

import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.testing.FakeSpatialPointerComponent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SpatialPointerComponentTest {
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
    fun addSpatialPointerComponent_addsRuntimeSpatialPointerComponent() {
        val entity = PanelEntity.create(session, TextView(activity), IntSize2d(720, 480), "test")
        assertThat(entity).isNotNull()
        val pointerComponent = SpatialPointerComponent.create(session)

        assertThat(entity.addComponent(pointerComponent)).isTrue()
        assertThat(entity.rtEntity?.getComponents()).hasSize(1)
        assertThat(entity.rtEntity?.getComponents()[0])
            .isInstanceOf(FakeSpatialPointerComponent::class.java)
    }

    @Test
    fun addSpatialPointerComponent_failsForNonPanelEntity() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()
        val pointerComponent = SpatialPointerComponent.create(session)

        assertThat(entity.addComponent(pointerComponent)).isFalse()
    }

    @Test
    fun getSpatialPointerIcon_returnsSetValue() {
        val entity = PanelEntity.create(session, TextView(activity), IntSize2d(720, 480), "test")
        assertThat(entity).isNotNull()

        val pointerComponent = SpatialPointerComponent.create(session)
        assertThat(entity.addComponent(pointerComponent)).isTrue()

        pointerComponent.spatialPointerIcon = SpatialPointerIcon.CIRCLE
        assertThat(pointerComponent.spatialPointerIcon).isEqualTo(SpatialPointerIcon.CIRCLE)
        pointerComponent.spatialPointerIcon = SpatialPointerIcon.NONE
        assertThat(pointerComponent.spatialPointerIcon).isEqualTo(SpatialPointerIcon.NONE)
        pointerComponent.spatialPointerIcon = SpatialPointerIcon.DEFAULT
        assertThat(pointerComponent.spatialPointerIcon).isEqualTo(SpatialPointerIcon.DEFAULT)
    }

    @Test
    fun getSpatialPointerIcon_addAndRemoveComponentSetsDefaultIcon() {
        val entity = PanelEntity.create(session, TextView(activity), IntSize2d(720, 480), "test")
        assertThat(entity).isNotNull()

        val pointerComponent = SpatialPointerComponent.create(session)
        assertThat(entity.addComponent(pointerComponent)).isTrue()

        assertThat(pointerComponent.spatialPointerIcon).isEqualTo(SpatialPointerIcon.DEFAULT)
        pointerComponent.spatialPointerIcon = SpatialPointerIcon.NONE
        assertThat(pointerComponent.spatialPointerIcon).isEqualTo(SpatialPointerIcon.NONE)
        entity.removeComponent(pointerComponent)
        assertThat(pointerComponent.spatialPointerIcon).isEqualTo(SpatialPointerIcon.DEFAULT)
    }
}
