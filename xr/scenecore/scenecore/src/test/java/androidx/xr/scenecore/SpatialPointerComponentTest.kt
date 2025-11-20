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

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.ActivitySpace as RtActivitySpace
import androidx.xr.scenecore.runtime.Entity as RtEntity
import androidx.xr.scenecore.runtime.PanelEntity as RtPanelEntity
import androidx.xr.scenecore.runtime.PixelDimensions as RtPixelDimensions
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.scenecore.testing.FakeSpatialPointerComponent
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SpatialPointerComponentTest {
    private val fakePerceptionRuntimeFactory = FakePerceptionRuntimeFactory()
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private val mockSceneRuntime = mock<SceneRuntime>()

    private val mockActivitySpace = mock<RtActivitySpace>()
    private lateinit var session: Session
    private val entityManager = EntityManager()
    private val mockGroupEntity = mock<RtEntity>()
    private val mockPanelEntity = mock<RtPanelEntity>()

    @Before
    fun setUp() {
        whenever(mockSceneRuntime.spatialEnvironment).thenReturn(mock())
        whenever(mockSceneRuntime.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockSceneRuntime.headActivityPose).thenReturn(mock())
        whenever(mockSceneRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockSceneRuntime.mainPanelEntity).thenReturn(mock())
        whenever(mockSceneRuntime.spatialCapabilities).thenReturn(RtSpatialCapabilities(0))
        whenever(mockSceneRuntime.createGroupEntity(any(), any(), any()))
            .thenReturn(mockGroupEntity)

        whenever(
                mockSceneRuntime.createPanelEntity(
                    any<Context>(),
                    any<Pose>(),
                    any<View>(),
                    any<RtPixelDimensions>(),
                    any<String>(),
                    any<RtEntity>(),
                )
            )
            .thenReturn(mockPanelEntity)

        session =
            Session(
                activity,
                runtimes =
                    listOf(fakePerceptionRuntimeFactory.createRuntime(activity), mockSceneRuntime),
            )
    }

    @Test
    fun addSpatialPointerComponent_addsRuntimeSpatialPointerComponent() {
        val entity = PanelEntity.create(session, TextView(activity), IntSize2d(720, 480), "test")
        assertThat(entity).isNotNull()
        whenever(mockSceneRuntime.createSpatialPointerComponent()).thenReturn(mock())
        whenever(mockPanelEntity.addComponent(any())).thenReturn(true)
        val pointerComponent = SpatialPointerComponent.create(session)

        assertThat(entity.addComponent(pointerComponent)).isTrue()
        verify(mockSceneRuntime).createSpatialPointerComponent()
        verify(mockPanelEntity).addComponent(any())
    }

    @Test
    fun addSpatialPointerComponent_failsForNonPanelEntity() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val pointerComponent = SpatialPointerComponent.create(session)

        assertThat(entity.addComponent(pointerComponent)).isFalse()
    }

    @Test
    fun getSpatialPointerIcon_returnsSetValue() {
        val entity = PanelEntity.create(session, TextView(activity), IntSize2d(720, 480), "test")
        val fakeRtSpatialPointerComponent = FakeSpatialPointerComponent()
        assertThat(entity).isNotNull()
        whenever(mockSceneRuntime.createSpatialPointerComponent())
            .thenReturn(fakeRtSpatialPointerComponent)
        whenever(mockPanelEntity.addComponent(any())).thenReturn(true)
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
        val fakeRtSpatialPointerComponent = FakeSpatialPointerComponent()
        assertThat(entity).isNotNull()
        whenever(mockSceneRuntime.createSpatialPointerComponent())
            .thenReturn(fakeRtSpatialPointerComponent)
        whenever(mockPanelEntity.addComponent(any())).thenReturn(true)
        val pointerComponent = SpatialPointerComponent.create(session)
        assertThat(entity.addComponent(pointerComponent)).isTrue()

        assertThat(pointerComponent.spatialPointerIcon).isEqualTo(SpatialPointerIcon.DEFAULT)
        pointerComponent.spatialPointerIcon = SpatialPointerIcon.NONE
        assertThat(pointerComponent.spatialPointerIcon).isEqualTo(SpatialPointerIcon.NONE)
        entity.removeComponent(pointerComponent)
        assertThat(pointerComponent.spatialPointerIcon).isEqualTo(SpatialPointerIcon.DEFAULT)
    }
}
