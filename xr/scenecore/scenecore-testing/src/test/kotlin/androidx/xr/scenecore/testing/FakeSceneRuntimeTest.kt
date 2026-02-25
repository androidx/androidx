/*
 * Copyright 2025 The Android Open Source Project
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

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.InputEvent
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.PlaneSemantic
import androidx.xr.scenecore.runtime.PlaneType
import androidx.xr.scenecore.runtime.PointerCaptureComponent.StateListener
import androidx.xr.scenecore.runtime.SpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialVisibility
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FakeSceneRuntimeTest {
    private lateinit var fakeSceneRuntime: FakeSceneRuntime

    @Before
    fun setUp() {
        fakeSceneRuntime = FakeSceneRuntime()
    }

    @Test
    fun getState_whenCreated_returnsCreatedState() {
        assertThat(fakeSceneRuntime.state).isEqualTo(FakeSceneRuntime.State.CREATED)
    }

    @Test
    fun enablePanelDepthTest_setsValueCorrectly() {
        check(!fakeSceneRuntime.enabledPanelDepthTest)

        fakeSceneRuntime.enablePanelDepthTest(true)

        assertThat(fakeSceneRuntime.enabledPanelDepthTest).isTrue()
    }

    @Test
    fun addRemoveSpatialCapabilitiesChangedListener_spatialCapabilitiesChangedMapUpdated() {
        check(fakeSceneRuntime.spatialCapabilitiesChangedMap.isEmpty())

        val consumer = Consumer<SpatialCapabilities> {}
        fakeSceneRuntime.addSpatialCapabilitiesChangedListener(
            { command -> command.run() },
            consumer,
        )

        assertThat(fakeSceneRuntime.spatialCapabilitiesChangedMap).hasSize(1)

        fakeSceneRuntime.removeSpatialCapabilitiesChangedListener(consumer)

        assertThat(fakeSceneRuntime.spatialCapabilitiesChangedMap).isEmpty()
    }

    @Test
    fun setClearSpatialVisibilityChangedListener_spatialVisibilityChangedMapUpdated() {
        check(fakeSceneRuntime.spatialVisibilityChangedMap.isEmpty())

        val consumer = Consumer<SpatialVisibility> {}
        fakeSceneRuntime.setSpatialVisibilityChangedListener({ command -> command.run() }, consumer)

        assertThat(fakeSceneRuntime.spatialVisibilityChangedMap).hasSize(1)

        fakeSceneRuntime.clearSpatialVisibilityChangedListener()

        assertThat(fakeSceneRuntime.spatialVisibilityChangedMap).isEmpty()
    }

    @Test
    fun addRemovePerceivedResolutionChangedListener_perceivedResolutionChangedMapUpdated() {
        check(fakeSceneRuntime.perceivedResolutionChangedMap.isEmpty())

        val consumer = Consumer<PixelDimensions> {}
        fakeSceneRuntime.addPerceivedResolutionChangedListener(
            { command -> command.run() },
            consumer,
        )

        assertThat(fakeSceneRuntime.perceivedResolutionChangedMap).hasSize(1)

        fakeSceneRuntime.removePerceivedResolutionChangedListener(consumer)

        assertThat(fakeSceneRuntime.perceivedResolutionChangedMap).isEmpty()
    }

    @Test
    fun requestHomeSpaceMode_requestFullSpaceMode_spatialCapabilitiesIsUpdated() {
        assertThat(fakeSceneRuntime.spatialCapabilities.capabilities)
            .isEqualTo(FakeSceneRuntime.ALL_SPATIAL_CAPABILITIES)

        fakeSceneRuntime.requestHomeSpaceMode()

        assertThat(fakeSceneRuntime.spatialCapabilities.capabilities).isEqualTo(0)

        fakeSceneRuntime.requestFullSpaceMode()

        assertThat(fakeSceneRuntime.spatialCapabilities.capabilities)
            .isEqualTo(FakeSceneRuntime.ALL_SPATIAL_CAPABILITIES)
    }

    @Test
    fun createPanelEntity_withDimensionsInMeter_returnsInitialValue() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
        val pose = Pose.Identity
        val view = View(activity)
        val dimensions = Dimensions(2f, 1f, 0f)
        val name = "test_panel"
        val parent = FakeEntity()
        val panelEntity =
            fakeSceneRuntime.createPanelEntity(activity, pose, view, dimensions, name, parent)

        assertThat(panelEntity).isInstanceOf(FakePanelEntity::class.java)
        assertThat(panelEntity.getPose()).isEqualTo(pose)
        assertThat(panelEntity.size.width).isWithin(0.001f).of(dimensions.width)
        assertThat(panelEntity.size.height).isWithin(0.001f).of(dimensions.height)
        assertThat(panelEntity.size.depth).isWithin(0.001f).of(dimensions.depth)
        assertThat(panelEntity.parent).isEqualTo(parent)
        assertThat((panelEntity as FakeEntity).name).isEqualTo(name)
    }

    @Test
    fun createPanelEntity_withDimensionsInPixel_returnsInitialValue() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
        val pose = Pose.Identity
        val view = View(activity)
        val pixelDimensions = PixelDimensions(640, 480)
        val name = "test_panel"
        val parent = FakeEntity()
        val panelEntity =
            fakeSceneRuntime.createPanelEntity(activity, pose, view, pixelDimensions, name, parent)

        assertThat(panelEntity).isInstanceOf(FakePanelEntity::class.java)
        assertThat(panelEntity.getPose()).isEqualTo(pose)
        assertThat(panelEntity.sizeInPixels).isEqualTo(pixelDimensions)
        assertThat(panelEntity.parent).isEqualTo(parent)
        assertThat((panelEntity as FakeEntity).name).isEqualTo(name)
    }

    @Test
    fun createActivityPanelEntity_returnsInitialValue() {
        val pose = Pose.Identity
        val windowBoundsPx = PixelDimensions(640, 480)
        val name = "test_activity_panel"
        val hostActivity = Robolectric.buildActivity(Activity::class.java).create().start().get()
        val parent = FakeEntity()
        val activityPanelEntity =
            fakeSceneRuntime.createActivityPanelEntity(
                pose,
                windowBoundsPx,
                name,
                hostActivity,
                parent,
            )

        assertThat(activityPanelEntity).isInstanceOf(FakeActivityPanelEntity::class.java)
        assertThat(activityPanelEntity.getPose()).isEqualTo(pose)
        assertThat(activityPanelEntity.sizeInPixels).isEqualTo(windowBoundsPx)
        assertThat(activityPanelEntity.parent).isEqualTo(parent)
        assertThat((activityPanelEntity as FakeEntity).name).isEqualTo(name)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun createAnchorEntity_returnsInitialValue() {
        val anchorEntity = fakeSceneRuntime.createAnchorEntity()

        assertThat(anchorEntity).isInstanceOf(FakeAnchorEntity::class.java)
        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.UNANCHORED)
    }

    @Test
    fun createGroupEntity_returnsInitialValue() {
        val pose = Pose.Identity
        val name = "test_entity"
        val parent = FakeEntity()
        val groupEntity = fakeSceneRuntime.createGroupEntity(pose, name, parent)

        assertThat(groupEntity).isInstanceOf(FakeEntity::class.java)
        assertThat(groupEntity.getPose()).isEqualTo(pose)
        assertThat(groupEntity.parent).isEqualTo(parent)
        assertThat((groupEntity as FakeEntity).name).isEqualTo(name)
    }

    @Test
    fun createInteractableComponent_returnsInitialValue() {
        val listener = TestInputEventListener()

        assertThat(
                fakeSceneRuntime.createInteractableComponent({ command -> command.run() }, listener)
            )
            .isInstanceOf(FakeInteractableComponent::class.java)
    }

    @Test
    fun createMovableComponent_returnsInitialValue() {
        val movableComponent =
            fakeSceneRuntime.createMovableComponent(
                systemMovable = false,
                scaleInZ = false,
                userAnchorable = false,
            )
        assertThat(movableComponent).isInstanceOf(FakeMovableComponent::class.java)
        assertThat(movableComponent.systemMovable).isFalse()
        assertThat(movableComponent.scaleInZ).isFalse()
        assertThat(movableComponent.userAnchorable).isFalse()
    }

    @Test
    fun createAnchorPlacementForPlanes_returnsInitialValue() {
        val planeTypeFilter = setOf(PlaneType.HORIZONTAL)
        val planeSemanticFilter = setOf(PlaneSemantic.TABLE, PlaneSemantic.FLOOR)

        val anchorPlacement =
            fakeSceneRuntime.createAnchorPlacementForPlanes(planeTypeFilter, planeSemanticFilter)

        assertThat(anchorPlacement).isInstanceOf(FakeAnchorPlacement::class.java)
        assertThat(anchorPlacement.planeTypeFilter).isEqualTo(planeTypeFilter)
        assertThat(anchorPlacement.planeSemanticFilter).isEqualTo(planeSemanticFilter)
    }

    @Test
    fun createResizableComponent_returnsInitialValue() {
        val minimumSize = Dimensions(1.0f, 1.0f, 0.0f)
        val maximumSize = Dimensions(2.0f, 2.0f, 2.0f)
        val resizableComponent = fakeSceneRuntime.createResizableComponent(minimumSize, maximumSize)

        assertThat(resizableComponent).isInstanceOf(FakeResizableComponent::class.java)
        assertThat(resizableComponent.minimumSize).isEqualTo(minimumSize)
        assertThat(resizableComponent.maximumSize).isEqualTo(maximumSize)
    }

    @Test
    fun createPointerCaptureComponent_returnsInitialValue() {
        val executor = Executor { command -> command.run() }
        val stateListener = StateListener {}
        val inputListener = TestInputEventListener()
        val pointerCaptureComponent =
            fakeSceneRuntime.createPointerCaptureComponent(executor, stateListener, inputListener)

        assertThat(pointerCaptureComponent).isInstanceOf(FakePointerCaptureComponent::class.java)
        assertThat(pointerCaptureComponent.executor).isEqualTo(executor)
        assertThat(pointerCaptureComponent.stateListener).isEqualTo(stateListener)
    }

    @Test
    fun createSpatialPointerComponent_returnsInitialValue() {
        assertThat(fakeSceneRuntime.createSpatialPointerComponent())
            .isInstanceOf(FakeSpatialPointerComponent::class.java)
    }

    @Test
    fun setFullSpaceMode_returnsSameBundle() {
        val bundle = Bundle().apply { putString("testkey", "testval") }

        assertThat(fakeSceneRuntime.setFullSpaceMode(bundle)).isEqualTo(bundle)
    }

    @Test
    fun setFullSpaceModeWithEnvironmentInherited_returnsSameBundle() {
        val bundle = Bundle().apply { putString("testkey", "testval") }
        assertThat(fakeSceneRuntime.setFullSpaceModeWithEnvironmentInherited(bundle))
            .isEqualTo(bundle)
    }

    @Test
    fun setPreferredAspectRatio_setsLastActivityAndRatio() {
        check(fakeSceneRuntime.lastSetPreferredAspectRatioActivity == null)
        check(fakeSceneRuntime.lastSetPreferredAspectRatioRatio == -1f)

        val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
        val preferredRatio = 1.23f
        fakeSceneRuntime.setPreferredAspectRatio(activity, preferredRatio)

        assertThat(fakeSceneRuntime.lastSetPreferredAspectRatioActivity).isEqualTo(activity)
        assertThat(fakeSceneRuntime.lastSetPreferredAspectRatioRatio).isEqualTo(preferredRatio)
    }

    @Test
    fun onBoundaryConsentChanged_listenersAreCalledAndInternalStateUpdates() {
        var listenerCalledWith: Boolean? = null
        val listener = Consumer<Boolean> { granted -> listenerCalledWith = granted }
        val executor = Executor { command -> command.run() }

        assertThat(fakeSceneRuntime.isBoundaryConsentGranted).isFalse()

        fakeSceneRuntime.addOnBoundaryConsentChangedListener(executor, listener)

        assertThat(listenerCalledWith).isNull()

        // Change to true
        fakeSceneRuntime.onBoundaryConsentChanged(true)

        assertThat(fakeSceneRuntime.isBoundaryConsentGranted).isTrue()
        assertThat(listenerCalledWith).isTrue()

        // Change to false
        listenerCalledWith = null
        fakeSceneRuntime.onBoundaryConsentChanged(false)

        assertThat(fakeSceneRuntime.isBoundaryConsentGranted).isFalse()
        assertThat(listenerCalledWith).isFalse()
    }

    @Test
    fun addOnBoundaryConsentChangedListener_addsListenerAndExecutorToMap() {
        val listener = Consumer<Boolean> {}
        val executor = Executor { command -> command.run() }

        assertThat(fakeSceneRuntime.boundaryConsentChangedMap).isEmpty()

        fakeSceneRuntime.addOnBoundaryConsentChangedListener(executor, listener)

        assertThat(fakeSceneRuntime.boundaryConsentChangedMap).hasSize(1)
        assertThat(fakeSceneRuntime.boundaryConsentChangedMap).containsEntry(listener, executor)
    }

    @Test
    fun removeOnBoundaryConsentChangedListener_removesListenerFromMap() {
        val listener = Consumer<Boolean> {}
        val executor = Executor { command -> command.run() }

        fakeSceneRuntime.addOnBoundaryConsentChangedListener(executor, listener)

        assertThat(fakeSceneRuntime.boundaryConsentChangedMap).isNotEmpty() //

        fakeSceneRuntime.removeOnBoundaryConsentChangedListener(listener)

        assertThat(fakeSceneRuntime.boundaryConsentChangedMap).isEmpty()
    }

    private class TestInputEventListener : InputEventListener {
        override fun onInputEvent(event: InputEvent) {}
    }
}
