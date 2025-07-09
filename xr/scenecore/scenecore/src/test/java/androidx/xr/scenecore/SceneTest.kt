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

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ActivityPanelEntity as RtActivityPanelEntity
import androidx.xr.runtime.internal.ActivitySpace as RtActivitySpace
import androidx.xr.runtime.internal.AnchorEntity as RtAnchorEntity
import androidx.xr.runtime.internal.Entity as RtEntity
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.PanelEntity as RtPanelEntity
import androidx.xr.runtime.internal.PixelDimensions as RtPixelDimensions
import androidx.xr.runtime.internal.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.runtime.internal.SpatialVisibility as RtSpatialVisibility
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.FakeRuntimeFactory
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SceneTest {
    private val fakeRuntimeFactory = FakeRuntimeFactory()
    private val activityController = Robolectric.buildActivity(Activity::class.java)
    private val activity = activityController.create().start().get()
    private val mockPlatformAdapter = mock<JxrPlatformAdapter>()
    private val mockAnchorEntity = mock<RtAnchorEntity>()
    lateinit var session: Session

    @Before
    fun setUp() {
        whenever(mockPlatformAdapter.spatialEnvironment).thenReturn(mock())
        val mockActivitySpace = mock<RtActivitySpace>()
        whenever(mockPlatformAdapter.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockPlatformAdapter.headActivityPose).thenReturn(mock())
        whenever(mockPlatformAdapter.activitySpaceRootImpl).thenReturn(mockActivitySpace)
        whenever(mockPlatformAdapter.mainPanelEntity).thenReturn(mock())
        whenever(mockPlatformAdapter.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockAnchorEntity.state).thenReturn(RtAnchorEntity.State.UNANCHORED)
        whenever(mockPlatformAdapter.spatialCapabilities).thenReturn(RtSpatialCapabilities(0))
        session = Session(activity, fakeRuntimeFactory.createRuntime(activity), mockPlatformAdapter)
    }

    @Test
    fun getActivitySpace_returnsActivitySpace() {
        val activitySpace = session.scene.activitySpace

        assertThat(activitySpace).isNotNull()
    }

    @Test
    fun getActivitySpaceTwice_returnsSameSpace() {
        val activitySpace1 = session.scene.activitySpace
        val activitySpace2 = session.scene.activitySpace

        assertThat(activitySpace1).isEqualTo(activitySpace2)
    }

    @Test
    fun getActivitySpaceRoot_returnsActivitySpaceRoot() {
        val activitySpaceRoot = session.scene.activitySpaceRoot

        assertThat(activitySpaceRoot).isNotNull()
    }

    @Test
    fun getActivitySpaceRootTwice_returnsSameSpace() {
        val activitySpaceRoot1 = session.scene.activitySpaceRoot
        val activitySpaceRoot2 = session.scene.activitySpaceRoot

        assertThat(activitySpaceRoot1).isEqualTo(activitySpaceRoot2)
    }

    @Test
    fun getSpatialUser_returnsSpatialUser() {
        val spatialUser = session.scene.spatialUser

        assertThat(spatialUser).isNotNull()
    }

    @Test
    fun getSpatialUserTwice_returnsSameUser() {
        val spatialUser1 = session.scene.spatialUser
        val spatialUser2 = session.scene.spatialUser

        assertThat(spatialUser1).isEqualTo(spatialUser2)
    }

    @Test
    fun getPerceptionSpace_returnPerceptionSpace() {
        val perceptionSpace = session.scene.perceptionSpace

        assertThat(perceptionSpace).isNotNull()
    }

    @Test
    fun getMainPanelEntity_returnsPanelEntity() {
        @Suppress("UNUSED_VARIABLE") val unused = session.scene.mainPanelEntity
        @Suppress("UNUSED_VARIABLE") val unusedAgain = session.scene.mainPanelEntity

        verify(mockPlatformAdapter, times(1)).mainPanelEntity
    }

    @Test
    fun configureBundleForFullSpaceMode_Launch_callsThrough() {
        // Test that Session calls into the runtime.
        val bundle = Bundle().apply { putString("testkey", "testval") }
        whenever(mockPlatformAdapter.setFullSpaceMode(any())).thenReturn(bundle)
        @Suppress("UNUSED_VARIABLE")
        val unused = session.scene.configureBundleForFullSpaceModeLaunch(bundle)
        verify(mockPlatformAdapter).setFullSpaceMode(bundle)
    }

    @Test
    fun configureBundleForFullSpaceModeLaunchWithEnvironmentInherited_callsThrough() {
        // Test that Session calls into the runtime.
        val bundle = Bundle().apply { putString("testkey", "testval") }
        whenever(mockPlatformAdapter.setFullSpaceModeWithEnvironmentInherited(any()))
            .thenReturn(bundle)
        @Suppress("UNUSED_VARIABLE")
        val unused =
            session.scene.configureBundleForFullSpaceModeLaunchWithEnvironmentInherited(bundle)
        verify(mockPlatformAdapter).setFullSpaceModeWithEnvironmentInherited(bundle)
    }

    @Test
    fun setPreferredAspectRatio_callsThrough() {
        // Test that Session calls into the runtime.
        @Suppress("UNUSED_VARIABLE")
        val unused = session.scene.setPreferredAspectRatio(activity, 1.23f)
        verify(mockPlatformAdapter).setPreferredAspectRatio(activity, 1.23f)
    }

    @Test
    fun getPanelEntityType_returnsAllPanelEntities() {
        val mockPanelEntity1 = mock<RtPanelEntity>()
        val mockActivityPanelEntity = mock<RtActivityPanelEntity>()
        whenever(
                mockPlatformAdapter.createPanelEntity(
                    any<Context>(),
                    any<Pose>(),
                    any<View>(),
                    any<RtPixelDimensions>(),
                    any<String>(),
                    any<RtEntity>(),
                )
            )
            .thenReturn(mockPanelEntity1)
        whenever(mockPlatformAdapter.createActivityPanelEntity(any(), any(), any(), any(), any()))
            .thenReturn(mockActivityPanelEntity)
        val panelEntity =
            PanelEntity.create(session, TextView(activity), IntSize2d(720, 480), "test1")
        val activityPanelEntity = ActivityPanelEntity.create(session, IntSize2d(640, 480), "test2")

        assertThat(session.scene.getEntitiesOfType(PanelEntity::class.java))
            .containsAtLeast(panelEntity, activityPanelEntity)
    }

    @Test
    fun getEntitiesBaseType_returnsAllEntities() {
        val mockPanelEntity = mock<RtPanelEntity>()
        whenever(
                mockPlatformAdapter.createPanelEntity(
                    any<Context>(),
                    any<Pose>(),
                    any<View>(),
                    any<RtPixelDimensions>(),
                    any<String>(),
                    any<RtEntity>(),
                )
            )
            .thenReturn(mockPanelEntity)
        whenever(mockPlatformAdapter.createAnchorEntity(any(), any(), any(), any()))
            .thenReturn(mockAnchorEntity)
        val panelEntity =
            PanelEntity.create(session, TextView(activity), IntSize2d(720, 480), "test1")
        val anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ANY, PlaneSemanticType.ANY)

        assertThat(session.scene.getEntitiesOfType(Entity::class.java))
            .containsAtLeast(panelEntity, anchorEntity)
    }

    @Test
    fun addAndRemoveSpatialCapabilitiesChangedListener_callsRuntimeAddAndRemove() {
        val listener = Consumer<SpatialCapabilities> { _ -> }
        session.scene.addSpatialCapabilitiesChangedListener(listener = listener)
        verify(mockPlatformAdapter).addSpatialCapabilitiesChangedListener(any(), any())
        session.scene.removeSpatialCapabilitiesChangedListener(listener)
        verify(mockPlatformAdapter).removeSpatialCapabilitiesChangedListener(any())
    }

    @Test
    fun setSpatialVisibilityChangedListener_receivesRuntimeSpatialVisibilityChangedEvent() {
        var listenerCalledWithValue = SpatialVisibility.SPATIAL_VISIBILITY_UNKNOWN
        val captor = argumentCaptor<Consumer<RtSpatialVisibility>>()
        val listener = Consumer<Int> { visibility -> listenerCalledWithValue = visibility }

        // Test that it calls into the runtime and capture the runtime listener.
        val executor = directExecutor()
        session.scene.setSpatialVisibilityChangedListener(executor, listener)
        verify(mockPlatformAdapter)
            .setSpatialVisibilityChangedListener(eq(executor), captor.capture())

        // Simulate the runtime listener being called with any value.
        val rtListener = captor.firstValue
        rtListener.accept(RtSpatialVisibility(RtSpatialVisibility.WITHIN_FOV))
        assertThat(listenerCalledWithValue)
            .isNotEqualTo(SpatialVisibility.SPATIAL_VISIBILITY_UNKNOWN)
        assertThat(listenerCalledWithValue)
            .isEqualTo(SpatialVisibility.SPATIAL_VISIBILITY_WITHIN_FIELD_OF_VIEW)

        rtListener.accept(RtSpatialVisibility(RtSpatialVisibility.PARTIALLY_WITHIN_FOV))
        assertThat(listenerCalledWithValue)
            .isEqualTo(SpatialVisibility.SPATIAL_VISIBILITY_PARTIALLY_WITHIN_FIELD_OF_VIEW)

        rtListener.accept(RtSpatialVisibility(RtSpatialVisibility.OUTSIDE_FOV))
        assertThat(listenerCalledWithValue)
            .isEqualTo(SpatialVisibility.SPATIAL_VISIBILITY_OUTSIDE_FIELD_OF_VIEW)

        rtListener.accept(RtSpatialVisibility(RtSpatialVisibility.UNKNOWN))
        assertThat(listenerCalledWithValue).isEqualTo(SpatialVisibility.SPATIAL_VISIBILITY_UNKNOWN)
    }

    @Test
    fun setSpatialVisibilityChangedListener_withNoExecutor_callsRuntimeSetSpatialVisibilityChangedListenerWithMainThreadExecutor() {
        val listener = Consumer<Int> { _ -> }
        session.scene.setSpatialVisibilityChangedListener(listener)
        verify(mockPlatformAdapter)
            .setSpatialVisibilityChangedListener(eq(HandlerExecutor.mainThreadExecutor), any())
    }

    @Test
    fun clearSpatialVisibilityChangedListener_callsRuntimeClearSpatialVisibilityChangedListener() {
        session.scene.clearSpatialVisibilityChangedListener()
        verify(mockPlatformAdapter).clearSpatialVisibilityChangedListener()
    }

    @Test
    fun sceneInit_setsDefaultSpatialStateChangeHandler() {
        // Verify that default handler is always set.
        verify(mockPlatformAdapter).spatialModeChangeListener = any()
    }

    @Test
    fun setSpatialStateChangeHandler_callsRuntimeSetSpatialStateChangeHandler() {
        val mockSpatialModeChangeListener = mock<SpatialModeChangeListener>()
        session.scene.spatialModeChangeListener = mockSpatialModeChangeListener
        val captor = argumentCaptor<androidx.xr.runtime.internal.SpatialModeChangeListener>()
        verify(mockPlatformAdapter).spatialModeChangeListener = captor.capture()
        val rtSpatialStateChangeHandler = captor.firstValue
        val pose = Pose.Identity
        val scale = Vector3(2f, 2f, 2f)
        rtSpatialStateChangeHandler.onSpatialModeChanged(pose, scale)
        verify(mockSpatialModeChangeListener).onSpatialModeChanged(pose, scale.x)
    }

    @Test
    fun getSpatialCapabilities_invokesAdapterGetSpatialCapabilities() {
        val expectedCapabilities = SpatialCapabilities(1)
        whenever(mockPlatformAdapter.spatialCapabilities).thenReturn(RtSpatialCapabilities(1))
        val actualCapabilities = session.scene.spatialCapabilities
        verify(mockPlatformAdapter).spatialCapabilities
        assertThat(actualCapabilities).isEqualTo(expectedCapabilities)
    }

    @Test
    fun requestFullSpaceMode_callsThrough() {
        session.scene.requestFullSpaceMode()
        verify(mockPlatformAdapter).requestFullSpaceMode()
    }

    @Test
    fun requestHomeSpaceMode_callsThrough() {
        session.scene.requestHomeSpaceMode()
        verify(mockPlatformAdapter).requestHomeSpaceMode()
    }

    @Test
    fun panelClippingConfig_defaultValue_isTrue() {
        val defaultConfig = PanelClippingConfig(isDepthTestEnabled = true)
        assertThat(session.scene.panelClippingConfig).isEqualTo(defaultConfig)
        verify(mockPlatformAdapter, never()).enablePanelDepthTest(false)
    }

    @Test
    fun panelClippingConfig_setFalse_callsPlatformAdapterWithFalse() {
        val disabledConfig = PanelClippingConfig(isDepthTestEnabled = false)
        session.scene.panelClippingConfig = disabledConfig

        verify(mockPlatformAdapter).enablePanelDepthTest(false)
        assertThat(session.scene.panelClippingConfig).isEqualTo(disabledConfig)
        verify(mockPlatformAdapter, never()).enablePanelDepthTest(true)
    }

    @Test
    fun panelClippingConfig_setTrue_callsPlatformAdapterWithTrue() {
        // First, set to disabled to ensure the next call is a change.
        session.scene.panelClippingConfig = PanelClippingConfig(isDepthTestEnabled = false)

        val enabledConfig = PanelClippingConfig(isDepthTestEnabled = true)
        session.scene.panelClippingConfig = enabledConfig

        val inOrder = inOrder(mockPlatformAdapter)
        inOrder.verify(mockPlatformAdapter).enablePanelDepthTest(false)
        inOrder.verify(mockPlatformAdapter).enablePanelDepthTest(true)
        assertThat(session.scene.panelClippingConfig).isEqualTo(enabledConfig)
    }
}
