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
package androidx.xr.scenecore.spatial.core

import android.annotation.SuppressLint
import android.app.Activity
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.xr.runtime.NodeHolder
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Matrix4.Companion.fromTrs
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Quaternion.Companion.fromAxisAngle
import androidx.xr.runtime.math.Quaternion.Companion.fromEulerAngles
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.runtime.testing.math.assertRotation
import androidx.xr.runtime.testing.math.assertVector3
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.Component
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.InputEvent
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.PlaneSemantic
import androidx.xr.scenecore.runtime.PlaneType
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SpatialModeChangeListener
import androidx.xr.scenecore.runtime.SpatialVisibility
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeComponent
import androidx.xr.scenecore.testing.FakeGltfFeature.Companion.createWithMockFeature
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import androidx.xr.scenecore.testing.FakeSurfaceFeature
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.environment.EnvironmentVisibilityState
import com.android.extensions.xr.environment.PassthroughVisibilityState
import com.android.extensions.xr.environment.ShadowEnvironmentVisibilityState
import com.android.extensions.xr.environment.ShadowPassthroughVisibilityState
import com.android.extensions.xr.node.FakeCloseable
import com.android.extensions.xr.node.Mat4f
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.NodeRepository
import com.android.extensions.xr.node.ReformOptions
import com.android.extensions.xr.node.ShadowInputEvent
import com.android.extensions.xr.node.ShadowNode
import com.android.extensions.xr.node.ShadowNodeTransform
import com.android.extensions.xr.node.Vec3
import com.android.extensions.xr.space.PerceivedResolution
import com.android.extensions.xr.space.ShadowSpatialCapabilities
import com.android.extensions.xr.space.ShadowSpatialState
import com.android.extensions.xr.space.SpatialCapabilities
import com.android.extensions.xr.space.VisibilityState
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import java.util.function.Consumer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/** Tests for [SpatialSceneRuntimeFactory]. */
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SpatialSceneRuntimeTest {
    private val entityManager = EntityManager()
    private val nodeRepository = NodeRepository.getInstance()
    private val xrExtensions = requireNotNull(getXrExtensions())
    private val fakeExecutor = FakeScheduledExecutorService()
    private val mockGltfFeature = mock<GltfFeature>()
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val contentResolver = activity.contentResolver
    private val shadowContentResolver = Shadows.shadowOf(activity.contentResolver)
    private lateinit var testRuntime: SpatialSceneRuntime

    @Before
    fun setUp() {
        ShadowXrExtensions.extract(xrExtensions).setApiVersion(1)
        ShadowXrExtensions.extract(xrExtensions)
            .setOpenXrWorldSpaceType(OPEN_XR_REFERENCE_SPACE_TYPE)
        testRuntime =
            SpatialSceneRuntime.create(activity!!, fakeExecutor, xrExtensions, entityManager)
    }

    @After
    fun tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        testRuntime.destroy()
    }

    private fun createGltfEntity(): GltfEntityImpl {
        val nodeHolder: NodeHolder<*> =
            NodeHolder<Node>(xrExtensions.createNode(), Node::class.java)
        val fakeGltfFeature = createWithMockFeature(mockGltfFeature, nodeHolder)
        return GltfEntityImpl(
            activity!!,
            fakeGltfFeature,
            testRuntime.activitySpace,
            xrExtensions,
            entityManager,
            fakeExecutor,
        )
    }

    @Throws(Exception::class)
    fun createGltfEntity(pose: Pose): GltfEntity {
        val gltfEntity: GltfEntity = createGltfEntity()
        gltfEntity.setPose(pose)
        return gltfEntity
    }

    private fun createRuntime(): SpatialSceneRuntime {
        return SpatialSceneRuntime.create(activity!!, fakeExecutor, xrExtensions, entityManager)
    }

    @Test
    fun sceneRuntime_setUpSceneRootAndTaskLeashNodes() {
        val rootNode: Node = testRuntime.sceneRootNode
        val taskWindowLeashNode: Node = testRuntime.taskWindowLeashNode

        assertThat(nodeRepository.getName(rootNode))
            .isEqualTo("SpatialSceneAndActivitySpaceRootNode")
        assertThat(nodeRepository.getName(taskWindowLeashNode))
            .isEqualTo("MainPanelAndTaskWindowLeashNode")
        assertThat(nodeRepository.getParent(taskWindowLeashNode)).isEqualTo(rootNode)
    }

    @Test
    fun getEnvironment_returnsEnvironment() {
        val environment = testRuntime.spatialEnvironment
        assertThat(environment).isNotNull()
    }

    @Test
    fun getActivitySpace_returnsEntity() {
        val activitySpaceImpl = testRuntime.activitySpace

        assertThat(activitySpaceImpl).isNotNull()
        // Verify that there is an underlying extension node.
        assertThat(activitySpaceImpl.getNode()).isNotNull()
    }

    @Test
    fun onSpatialStateChanged_setsSpatialCapabilities() {
        val spatialState = ShadowSpatialState.create()
        ShadowSpatialState.extract(spatialState)
            .setSpatialCapabilities(
                ShadowSpatialCapabilities.create(SpatialCapabilities.SPATIAL_UI_CAPABLE)
            )
        testRuntime.onSpatialStateChanged(spatialState)

        val caps = testRuntime.spatialCapabilities
        assertThat(
                caps.hasCapability(
                    androidx.xr.scenecore.runtime.SpatialCapabilities.SPATIAL_CAPABILITY_UI
                )
            )
            .isTrue()
        assertThat(
                caps.hasCapability(
                    androidx.xr.scenecore.runtime.SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT
                )
            )
            .isFalse()
        assertThat(
                caps.hasCapability(
                    androidx.xr.scenecore.runtime.SpatialCapabilities
                        .SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL
                )
            )
            .isFalse()
        assertThat(
                caps.hasCapability(
                    androidx.xr.scenecore.runtime.SpatialCapabilities
                        .SPATIAL_CAPABILITY_APP_ENVIRONMENT
                )
            )
            .isFalse()
        assertThat(
                caps.hasCapability(
                    androidx.xr.scenecore.runtime.SpatialCapabilities
                        .SPATIAL_CAPABILITY_SPATIAL_AUDIO
                )
            )
            .isFalse()
        assertThat(
                caps.hasCapability(
                    androidx.xr.scenecore.runtime.SpatialCapabilities
                        .SPATIAL_CAPABILITY_EMBED_ACTIVITY
                )
            )
            .isFalse()
    }

    @Test
    fun onSpatialStateChanged_setsEnvironmentVisibility() {
        val environment = testRuntime.spatialEnvironment
        assertThat(environment.isPreferredSpatialEnvironmentActive).isFalse()

        var state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setEnvironmentVisibilityState(
                ShadowEnvironmentVisibilityState.create(EnvironmentVisibilityState.APP_VISIBLE)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        assertThat(environment.isPreferredSpatialEnvironmentActive).isTrue()

        state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setEnvironmentVisibilityState(
                ShadowEnvironmentVisibilityState.create(EnvironmentVisibilityState.INVISIBLE)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        assertThat(environment.isPreferredSpatialEnvironmentActive).isFalse()

        state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setEnvironmentVisibilityState(
                ShadowEnvironmentVisibilityState.create(EnvironmentVisibilityState.HOME_VISIBLE)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        assertThat(environment.isPreferredSpatialEnvironmentActive).isFalse()
    }

    @Test
    fun onSpatialStateChanged_callsEnvironmentListenerOnlyForChanges() {
        val environment = testRuntime.spatialEnvironment
        val listener = mock<Consumer<Boolean>>()
        environment.addOnSpatialEnvironmentChangedListener(MoreExecutors.directExecutor(), listener)

        assertThat(environment.isPreferredSpatialEnvironmentActive).isFalse()

        // The first spatial state should always fire the listener
        var state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setEnvironmentVisibilityState(
                ShadowEnvironmentVisibilityState.create(EnvironmentVisibilityState.APP_VISIBLE)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        verify(listener).accept(true)

        // The second spatial state should also fire the listener since it's a different state
        state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setEnvironmentVisibilityState(
                ShadowEnvironmentVisibilityState.create(EnvironmentVisibilityState.INVISIBLE)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        assertThat(environment.isPreferredSpatialEnvironmentActive).isFalse()
        verify(listener).accept(false)

        // The third spatial state should not fire the listener since it is the same as the last
        // state.
        state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setEnvironmentVisibilityState(
                ShadowEnvironmentVisibilityState.create(EnvironmentVisibilityState.INVISIBLE)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        assertThat(environment.isPreferredSpatialEnvironmentActive).isFalse()
        verify(listener, times(2)).accept(any()) // Verify the listener was not called a third time.
    }

    @Test
    fun onSpatialStateChanged_setsPassthroughOpacity() {
        val environment = testRuntime.spatialEnvironment
        assertThat(environment.currentPassthroughOpacity).isZero()

        var state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setPassthroughVisibilityState(
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.APP, 0.4f)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        assertThat(environment.currentPassthroughOpacity).isEqualTo(0.4f)

        state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setPassthroughVisibilityState(
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.HOME, 0.5f)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        assertThat(environment.currentPassthroughOpacity).isEqualTo(0.5f)

        state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setPassthroughVisibilityState(
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.SYSTEM, 0.9f)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        assertThat(environment.currentPassthroughOpacity).isEqualTo(0.9f)

        state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setPassthroughVisibilityState(
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.DISABLED, 0.0f)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        assertThat(environment.currentPassthroughOpacity).isZero()
    }

    @Test
    fun onSpatialStateChanged_callsPassthroughListenerOnlyForChanges() {
        val environment = testRuntime.spatialEnvironment
        val listener = mock<Consumer<Float>>()

        environment.addOnPassthroughOpacityChangedListener(MoreExecutors.directExecutor(), listener)

        assertThat(environment.currentPassthroughOpacity).isZero()

        // The first spatial state should always fire the listener
        var state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setPassthroughVisibilityState(
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.APP, 1.0f)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        verify(listener).accept(1.0f)

        // The second spatial state should also fire the listener even if only the opacity changes
        state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setPassthroughVisibilityState(
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.APP, 0.5f)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        assertThat(environment.currentPassthroughOpacity).isEqualTo(0.5f)

        // The third spatial state should also fire the listener even if only the visibility state
        // changes, but getCurrentPassthroughOpacity() returns the same value as the last state.
        state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setPassthroughVisibilityState(
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.HOME, 0.5f)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        assertThat(environment.currentPassthroughOpacity).isEqualTo(0.5f)
        verify(listener, times(2))
            .accept(0.5f) // Verify it was called a second time with this value.

        // The fourth spatial state should not fire the listener since it is the same as the last
        // state.
        state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setPassthroughVisibilityState(
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.HOME, 0.5f)
            )
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        assertThat(environment.currentPassthroughOpacity).isEqualTo(0.5f)
        verify(listener, times(3))
            .accept(any()) // Verify the listener was not called a fourth time.
    }

    @Test
    fun currentPassthroughOpacity_isSetDuringRuntimeCreation() {
        ShadowSpatialState.extract(xrExtensions.getSpatialState(activity))
            .setPassthroughVisibilityState(
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.APP, 0.5f)
            )

        val newEnvironment = testRuntime.spatialEnvironment
        assertThat(newEnvironment.currentPassthroughOpacity).isEqualTo(0.5f)
    }

    @Test
    fun onSpatialStateChanged_firesSpatialCapabilitiesChangedListener() {
        val listener1 = mock<Consumer<androidx.xr.scenecore.runtime.SpatialCapabilities>>()
        val listener2 = mock<Consumer<androidx.xr.scenecore.runtime.SpatialCapabilities>>()

        testRuntime.addSpatialCapabilitiesChangedListener(MoreExecutors.directExecutor(), listener1)
        testRuntime.addSpatialCapabilitiesChangedListener(MoreExecutors.directExecutor(), listener2)

        var state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setSpatialCapabilities(ShadowSpatialCapabilities.createAll())
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        verify(listener1).accept(any())
        verify(listener2).accept(any())

        state = ShadowSpatialState.create()
        ShadowSpatialState.extract(state)
            .setSpatialCapabilities(ShadowSpatialCapabilities.create(0))
        testRuntime.removeSpatialCapabilitiesChangedListener(listener1)
        ShadowXrExtensions.extract(xrExtensions).sendSpatialState(activity, state)
        verify(listener1)
            .accept(
                any<androidx.xr.scenecore.runtime.SpatialCapabilities>()
            ) // Verify the removed listener was called exactly once
        verify(listener2, times(2))
            .accept(
                any<androidx.xr.scenecore.runtime.SpatialCapabilities>()
            ) // Verify the active listener was called twice
    }

    private fun getNode(entity: Entity): Node {
        return (entity as AndroidXrEntity).getNode()
    }

    @Test
    fun createSubspaceNodeEntity_returnSubspaceNodeEntity() {
        val size = Dimensions(1.0f, 2.0f, 3.0f)

        val entity = testRuntime.createSubspaceNodeEntity(xrExtensions.createNode(), size)
        entity.size = size

        assertThat(entity).isNotNull()
        assertThat(entity.size).isEqualTo(size)
    }

    /**
     * Creates a generic panel entity instance for testing by creating a dummy view to insert into
     * the panel, and setting the activity space as parent.
     */
    private fun createPanelEntity(pose: Pose = Pose()): PanelEntity {
        val display = activity!!.getSystemService(DisplayManager::class.java).displays[0]
        val displayContext = activity.createDisplayContext(display!!)
        val view = View(displayContext)
        view.layoutParams = ViewGroup.LayoutParams(640, 480)
        return testRuntime.createPanelEntity(
            displayContext,
            pose,
            view,
            PixelDimensions(640, 480),
            "testPanel",
            testRuntime.activitySpace,
        )
    }

    private fun createEntity(pose: Pose = Pose()): Entity {
        return testRuntime.createEntity(pose, "test", testRuntime.activitySpace)
    }

    @Test
    @Throws(Exception::class)
    fun createEntity_returnsEntity() {
        assertThat(createEntity()).isNotNull()
    }

    @Test
    @Throws(Exception::class)
    fun entity_hasActivitySpaceRootImplAsParentByDefault() {
        val entity = createEntity()
        assertThat(entity.parent).isEqualTo(testRuntime.activitySpace)
    }

    @Test
    @Throws(Exception::class)
    fun entityAddChildren_addsChildren() {
        val childEntity1 = createEntity()
        val childEntity2 = createEntity()
        val parentEntity = createEntity()

        parentEntity.addChild(childEntity1)

        assertThat(parentEntity.children).containsExactly(childEntity1)

        parentEntity.addChildren(ImmutableList.of(childEntity2))

        assertThat(childEntity1.parent).isEqualTo(parentEntity)
        assertThat(childEntity2.parent).isEqualTo(parentEntity)
        assertThat(parentEntity.children).containsExactly(childEntity1, childEntity2)

        val childNode1 = getNode(childEntity1)
        assertThat(nodeRepository.getParent(childNode1)).isEqualTo(getNode(parentEntity))
        val childNode2 = getNode(childEntity2)
        assertThat(nodeRepository.getParent(childNode2)).isEqualTo(getNode(parentEntity))
    }

    @Test
    fun createLoggingEntity_returnsEntity() {
        val pose = Pose()
        val loggingEntity = testRuntime.createLoggingEntity(pose)
        val updatedPose = Pose(Vector3(1f, pose.translation.y, pose.translation.z), pose.rotation)
        loggingEntity.setPose(updatedPose)
    }

    @Test
    fun loggingEntitySetParent() {
        val pose = Pose()
        val childEntity = testRuntime.createLoggingEntity(pose)
        val parentEntity = testRuntime.createLoggingEntity(pose)

        childEntity.parent = parentEntity
        parentEntity.addChild(childEntity)

        assertThat(childEntity.parent).isEqualTo(parentEntity)
        assertThat(parentEntity.parent).isEqualTo(null)
        assertThat(childEntity.children).isEmpty()
        assertThat(parentEntity.children).containsExactly(childEntity)
    }

    @Test
    fun loggingEntityUpdateParent() {
        val pose = Pose()
        val childEntity = testRuntime.createLoggingEntity(pose)
        val parentEntity1 = testRuntime.createLoggingEntity(pose)
        val parentEntity2 = testRuntime.createLoggingEntity(pose)

        childEntity.parent = parentEntity1

        assertThat(childEntity.parent).isEqualTo(parentEntity1)
        assertThat(parentEntity1.children).containsExactly(childEntity)
        assertThat(parentEntity2.children).isEmpty()

        childEntity.parent = parentEntity2

        assertThat(childEntity.parent).isEqualTo(parentEntity2)
        assertThat(parentEntity2.children).containsExactly(childEntity)
        assertThat(parentEntity1.children).isEmpty()
    }

    @Test
    fun loggingEntity_getActivitySpacePose_returnsIdentityPose() {
        val identityPose = Pose()
        val loggingEntity = testRuntime.createLoggingEntity(identityPose)
        assertPose(loggingEntity.activitySpacePose, identityPose)
    }

    @Test
    fun loggingEntity_transformPoseTo_returnsIdentityPose() {
        val identityPose = Pose()
        val loggingEntity = testRuntime.createLoggingEntity(identityPose)
        assertPose(loggingEntity.transformPoseTo(identityPose, loggingEntity), identityPose)
    }

    @Test
    fun loggingEntityAddChildren() {
        val pose = Pose()
        val childEntity1 = testRuntime.createLoggingEntity(pose)
        val childEntity2 = testRuntime.createLoggingEntity(pose)
        val parentEntity = testRuntime.createLoggingEntity(pose)

        parentEntity.addChild(childEntity1)

        assertThat(parentEntity.children).containsExactly(childEntity1)

        parentEntity.addChildren(ImmutableList.of(childEntity2))

        assertThat(childEntity1.parent).isEqualTo(parentEntity)
        assertThat(childEntity2.parent).isEqualTo(parentEntity)
        assertThat(parentEntity.children).containsExactly(childEntity1, childEntity2)
    }

    @Test
    fun createAnchorEntity_returnsUnanchoredAnchorEntity() {
        val anchorEntity = testRuntime.createAnchorEntity()

        assertThat(anchorEntity).isNotNull()
        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.UNANCHORED)
    }

    @Test
    fun spatialStateChangeHandler_invokedWhenSpatialStateChangesToFSM() {
        val spatialState = ShadowSpatialState.create()
        val mockSpatialModeChangeListener = mock<SpatialModeChangeListener>()
        testRuntime.spatialModeChangeListener = mockSpatialModeChangeListener
        ShadowSpatialState.extract(spatialState)
            .setSpatialCapabilities(ShadowSpatialCapabilities.createAll())
        ShadowSpatialState.extract(spatialState)
            .setSceneParentTransform(Mat4f(Matrix4.Identity.data))
        testRuntime.onSpatialStateChanged(spatialState)

        verify(mockSpatialModeChangeListener).onSpatialModeChanged(any(), any())
    }

    private fun sendVisibilityState(
        shadowXrExtensions: ShadowXrExtensions,
        width: Int,
        height: Int,
    ) {
        sendVisibilityState(shadowXrExtensions, VisibilityState.FULLY_VISIBLE, width, height)
    }

    private fun sendVisibilityState(
        shadowXrExtensions: ShadowXrExtensions,
        visibilityState: Int,
        width: Int = 1,
        height: Int = 1,
    ) {
        shadowXrExtensions.sendVisibilityState(
            activity,
            VisibilityState(visibilityState, PerceivedResolution(width, height)),
        )
    }

    @Test
    fun setSpatialVisibilityChangedListener_callsExtensions() {
        val mockListener = mock<Consumer<SpatialVisibility>>()
        testRuntime.setSpatialVisibilityChangedListener(
            MoreExecutors.directExecutor(),
            mockListener,
        )
        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)

        // VISIBLE
        sendVisibilityState(shadowXrExtensions, VisibilityState.FULLY_VISIBLE)
        verify(mockListener).accept(SpatialVisibility(SpatialVisibility.WITHIN_FOV))

        // PARTIALLY_VISIBLE
        sendVisibilityState(shadowXrExtensions, VisibilityState.PARTIALLY_VISIBLE)
        verify(mockListener).accept(SpatialVisibility(SpatialVisibility.PARTIALLY_WITHIN_FOV))

        // OUTSIDE_OF_FOV
        sendVisibilityState(shadowXrExtensions, VisibilityState.NOT_VISIBLE)
        verify(mockListener).accept(SpatialVisibility(SpatialVisibility.OUTSIDE_FOV))

        // UNKNOWN
        sendVisibilityState(shadowXrExtensions, VisibilityState.UNKNOWN)
        verify(mockListener).accept(SpatialVisibility(SpatialVisibility.UNKNOWN))
    }

    @Test
    fun setSpatialVisibilityChangedListener_replacesExistingListenerOnSecondCall() {
        val mockListener1 = mock<Consumer<SpatialVisibility>>()
        val mockListener2 = mock<Consumer<SpatialVisibility>>()
        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)

        // Listener 1 is set and called once.
        testRuntime.setSpatialVisibilityChangedListener(
            MoreExecutors.directExecutor(),
            mockListener1,
        )
        sendVisibilityState(shadowXrExtensions, VisibilityState.FULLY_VISIBLE)
        verify(mockListener1).accept(SpatialVisibility(SpatialVisibility.WITHIN_FOV))
        verify(mockListener2, never()).accept(SpatialVisibility(SpatialVisibility.WITHIN_FOV))

        // Listener 2 is set and called once. Listener 1 is not called again.
        testRuntime.setSpatialVisibilityChangedListener(
            MoreExecutors.directExecutor(),
            mockListener2,
        )
        sendVisibilityState(shadowXrExtensions, VisibilityState.NOT_VISIBLE)
        verify(mockListener2).accept(SpatialVisibility(SpatialVisibility.OUTSIDE_FOV))
        verify(mockListener1, never()).accept(SpatialVisibility(SpatialVisibility.OUTSIDE_FOV))
    }

    @Test
    fun clearSpatialVisibilityChangedListener_stopsSpatialVisibilityCallbacks() {
        val mockListener = mock<Consumer<SpatialVisibility>>()
        testRuntime.setSpatialVisibilityChangedListener(
            MoreExecutors.directExecutor(),
            mockListener,
        )

        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isTrue()

        // Verify that the callback is called once when the visibility changes.
        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)
        sendVisibilityState(shadowXrExtensions, VisibilityState.FULLY_VISIBLE)
        verify(mockListener).accept(any<SpatialVisibility>())

        // Clear the listener and verify that the callback is not called a second time.
        testRuntime.clearSpatialVisibilityChangedListener()
        sendVisibilityState(shadowXrExtensions, VisibilityState.NOT_VISIBLE)
        sendVisibilityState(shadowXrExtensions, VisibilityState.PARTIALLY_VISIBLE)
        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isFalse()
        verify(mockListener).accept(any<SpatialVisibility>())
    }

    @Test
    fun clearSpatialVisibilityChangedListener_handlesThrowWhenCalledWithoutSettingListener() {
        // No assert needed, the test will fail if an unhandled exception is thrown.
        testRuntime.clearSpatialVisibilityChangedListener()
    }

    @Test
    fun destroy_closesSpatialVisibilityAndPerceivedResolutionSubscription() {
        val mockSpatialVisListener = mock<Consumer<SpatialVisibility>>()
        val mockPerceivedResListener = mock<Consumer<PixelDimensions>>()

        testRuntime.setSpatialVisibilityChangedListener(
            MoreExecutors.directExecutor(),
            mockSpatialVisListener,
        )
        testRuntime.addPerceivedResolutionChangedListener(
            MoreExecutors.directExecutor(),
            mockPerceivedResListener,
        )

        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isTrue()

        // Verify that the callback is called once when the visibility changes.
        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)
        sendVisibilityState(shadowXrExtensions, VisibilityState.FULLY_VISIBLE)

        verify(mockSpatialVisListener).accept(any<SpatialVisibility>())
        verify(mockPerceivedResListener).accept(any<PixelDimensions>())

        // Ensure destroy() clears the listener that the callbacks are not called a second time.
        testRuntime.destroy()

        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isFalse()

        sendVisibilityState(shadowXrExtensions, VisibilityState.NOT_VISIBLE)
        sendVisibilityState(shadowXrExtensions, VisibilityState.PARTIALLY_VISIBLE)

        verify(mockSpatialVisListener).accept(any<SpatialVisibility>())
        verify(mockPerceivedResListener).accept(any<PixelDimensions>())
    }

    @Test
    fun clearSpatialVisibilityChangedListener_doesNotStopPerceivedResolutionListener() {
        val mockSpatialListener = mock<Consumer<SpatialVisibility>>()
        val mockPerceivedResListener = mock<Consumer<PixelDimensions>>()

        testRuntime.setSpatialVisibilityChangedListener(
            MoreExecutors.directExecutor(),
            mockSpatialListener,
        )
        testRuntime.addPerceivedResolutionChangedListener(
            MoreExecutors.directExecutor(),
            mockPerceivedResListener,
        )

        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isTrue()

        testRuntime.clearSpatialVisibilityChangedListener()

        // Perceived resolution listener is still active, so callback should remain registered.
        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isTrue()

        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)
        sendVisibilityState(shadowXrExtensions, SpatialVisibility.WITHIN_FOV, 10, 20)

        verify(mockSpatialListener, never()).accept(any())

        verify(mockPerceivedResListener).accept(any())
    }

    @Test
    fun addPerceivedResolutionChangedListener_registersCombinedCallbackFirstTime() {
        val mockListener = mock<Consumer<PixelDimensions>>()
        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)

        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isFalse()

        testRuntime.addPerceivedResolutionChangedListener(
            MoreExecutors.directExecutor(),
            mockListener,
        )

        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isTrue()

        verify(mockListener, never()).accept(any<PixelDimensions>())

        sendVisibilityState(shadowXrExtensions, 10, 20)

        verify(mockListener).accept(PixelDimensions(10, 20))
    }

    @Test
    fun removePerceivedResolutionChangedListener_clearsCombinedCallbackIfLastListener() {
        val mockListener = mock<Consumer<PixelDimensions>>()
        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)

        testRuntime.addPerceivedResolutionChangedListener(
            MoreExecutors.directExecutor(),
            mockListener,
        )

        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isTrue()

        sendVisibilityState(shadowXrExtensions, 10, 20)

        verify(mockListener).accept(PixelDimensions(10, 20))

        testRuntime.removePerceivedResolutionChangedListener(mockListener)

        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isFalse()

        // It shouldn't be called a second time
        sendVisibilityState(shadowXrExtensions, 10, 20)

        verify(mockListener, times(1)).accept(PixelDimensions(10, 20))
    }

    @Test
    fun removePerceivedResolutionChangedListener_doesNotStopSpatialListener() {
        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)
        val mockSpatialListener = mock<Consumer<SpatialVisibility>>()
        val mockPerceivedResListener = mock<Consumer<PixelDimensions>>()

        testRuntime.setSpatialVisibilityChangedListener(
            MoreExecutors.directExecutor(),
            mockSpatialListener,
        )
        testRuntime.addPerceivedResolutionChangedListener(
            MoreExecutors.directExecutor(),
            mockPerceivedResListener,
        )

        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isTrue()

        testRuntime.removePerceivedResolutionChangedListener(mockPerceivedResListener)

        // Spatial listener still active, so callback should remain registered.
        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isTrue()

        sendVisibilityState(shadowXrExtensions, SpatialVisibility.WITHIN_FOV, 10, 20)

        verify(mockSpatialListener).accept(any())

        verify(mockPerceivedResListener, never()).accept(any())
    }

    @Test
    fun removePerceivedResolutionChangedListener_doesNotStopAnotherPerceivedResListener() {
        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)
        val mockListener1 = mock<Consumer<PixelDimensions>>()
        val mockListener2 = mock<Consumer<PixelDimensions>>()

        testRuntime.addPerceivedResolutionChangedListener(
            MoreExecutors.directExecutor(),
            mockListener1,
        )
        testRuntime.addPerceivedResolutionChangedListener(
            MoreExecutors.directExecutor(),
            mockListener2,
        )

        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isTrue()

        testRuntime.removePerceivedResolutionChangedListener(mockListener1)

        // mockListener2 still active, so callback should remain registered.
        assertThat(testRuntime.isExtensionVisibilityStateCallbackRegistered).isTrue()

        sendVisibilityState(shadowXrExtensions, 10, 20)

        verify(mockListener2).accept(any<PixelDimensions>())
        verify(mockListener1, never()).accept(any<PixelDimensions>())
    }

    @Test
    fun combinedCallback_dispatchesToBothListenersCorrectly() {
        val mockSpatialListener = mock<Consumer<SpatialVisibility>>()
        val mockPerceivedResListener = mock<Consumer<PixelDimensions>>()

        testRuntime.setSpatialVisibilityChangedListener(
            MoreExecutors.directExecutor(),
            mockSpatialListener,
        )
        testRuntime.addPerceivedResolutionChangedListener(
            MoreExecutors.directExecutor(),
            mockPerceivedResListener,
        )

        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)
        sendVisibilityState(shadowXrExtensions, SpatialVisibility.OUTSIDE_FOV, 30, 40)

        verify(mockSpatialListener).accept((SpatialVisibility(SpatialVisibility.OUTSIDE_FOV)))

        verify(mockPerceivedResListener).accept((PixelDimensions(30, 40)))
    }

    @Test
    fun requestHomeSpaceMode_callsExtensions() {
        testRuntime.requestHomeSpaceMode()

        assertThat<ShadowXrExtensions.SpaceMode>(
                ShadowXrExtensions.extract(xrExtensions).getSpaceMode(activity)
            )
            .isEqualTo(ShadowXrExtensions.SpaceMode.HOME_SPACE)
    }

    @Test
    fun requestFullSpaceMode_callsExtensions() {
        testRuntime.requestFullSpaceMode()

        assertThat<ShadowXrExtensions.SpaceMode>(
                ShadowXrExtensions.extract(xrExtensions).getSpaceMode(activity)
            )
            .isEqualTo(ShadowXrExtensions.SpaceMode.FULL_SPACE)
    }

    @Test
    fun setFullSpaceMode_callsExtensions() {
        var bundle = Bundle.EMPTY
        bundle = testRuntime.setFullSpaceMode(bundle)

        // TODO: b/440191514 - Change to assertThat(bundle).isNotEqualTo(Bundle.EMPTY);
        assertThat(bundle).isNotNull()
    }

    @Test
    fun setFullSpaceModeWithEnvironmentInherited_callsExtensions() {
        var bundle = Bundle.EMPTY
        bundle = testRuntime.setFullSpaceModeWithEnvironmentInherited(bundle)

        // TODO: b/440191514 - Change to assertThat(bundle).isNotEqualTo(Bundle.EMPTY);
        assertThat(bundle).isNotNull()
    }

    @Test
    fun enablePanelDepthTest_callsExtensions() {
        val rootNode: Node = testRuntime.sceneRootNode

        testRuntime.enablePanelDepthTest(true)

        assertThat(nodeRepository.isEnablePanelDepthTest(rootNode)).isTrue()

        testRuntime.enablePanelDepthTest(false)

        assertThat(nodeRepository.isEnablePanelDepthTest(rootNode)).isFalse()
    }

    @Test
    fun setPreferredAspectRatio_callsExtensions() {
        testRuntime.setPreferredAspectRatio(activity!!, 1.23f)
        assertThat(ShadowXrExtensions.extract(xrExtensions).getPreferredAspectRatio(activity))
            .isEqualTo(1.23f)
    }

    @Test
    fun sceneRuntime_getSoundPoolExtensionsWrapper() {
        val extensions = testRuntime.soundPoolExtensionsWrapper

        assertThat(extensions).isNotNull()
    }

    @Test
    fun sceneRuntime_getAudioTrackExtensionsWrapper() {
        val extensions = testRuntime.audioTrackExtensionsWrapper

        assertThat(extensions).isNotNull()
    }

    @Test
    fun sceneRuntime_getMediaPlayerExtensionsWrapper() {
        val extensions = testRuntime.mediaPlayerExtensionsWrapper

        assertThat(extensions).isNotNull()
    }

    @Test
    fun createAnchorPlacement_returnsAnchorPlacement() {
        val anchorPlacement =
            testRuntime.createAnchorPlacementForPlanes(
                ImmutableSet.of<@JvmSuppressWildcards PlaneType>(PlaneType.ANY),
                ImmutableSet.of<@JvmSuppressWildcards PlaneSemantic>(PlaneSemantic.ANY),
            )

        assertThat(anchorPlacement).isNotNull()
    }

    @Test
    fun createMovableComponent_returnsComponent() {
        val movableComponent =
            testRuntime.createMovableComponent(
                systemMovable = true,
                scaleInZ = true,
                userAnchorable = false,
            )

        assertThat(movableComponent).isNotNull()
    }

    @Test
    fun createResizableComponent_returnsComponent() {
        val resizableComponent =
            testRuntime.createResizableComponent(Dimensions(0f, 0f, 0f), Dimensions(5f, 5f, 5f))

        assertThat(resizableComponent).isNotNull()
    }

    @Test
    fun createPointerCaptureComponent_returnsComponent() {
        val pointerCaptureComponent =
            testRuntime.createPointerCaptureComponent(
                fakeExecutor,
                { _: Int -> },
                { _: InputEvent -> },
            )

        assertThat(pointerCaptureComponent).isNotNull()
    }

    @Test
    fun createSpatialPointerComponent_returnsComponent() {
        val pointerComponent = testRuntime.createSpatialPointerComponent()

        assertThat(pointerComponent).isNotNull()
    }

    @Test
    fun createSurfaceEntity_returnsSurfaceEntity() {
        val nodeHolder = NodeHolder<Node>(xrExtensions.createNode(), Node::class.java)
        val surfaceEntity =
            testRuntime.createSurfaceEntity(
                FakeSurfaceFeature(nodeHolder),
                Pose(),
                testRuntime.activitySpace,
            )

        assertThat(surfaceEntity).isNotNull()
        assertThat(surfaceEntity).isInstanceOf(SurfaceEntityImpl::class.java)
    }

    @Test
    fun createGltfEntity_returnsEntity() {
        assertThat(createGltfEntity()).isNotNull()
    }

    @Test
    fun gltfEntitySetParent() {
        val childEntity = createGltfEntity()
        val parentEntity = createGltfEntity()

        childEntity.parent = parentEntity

        assertThat(childEntity.parent).isEqualTo(parentEntity)
        assertThat(parentEntity.parent).isEqualTo(testRuntime.activitySpace)
        assertThat(childEntity.children).isEmpty()
        assertThat(parentEntity.children).containsExactly(childEntity)

        // Verify that there is an underlying extension node relationship.
        val childNode = childEntity.getNode()

        assertThat(nodeRepository.getParent(childNode)).isEqualTo(parentEntity.getNode())
    }

    @Test
    fun gltfEntityUpdateParent() {
        val childEntity = createGltfEntity()
        val parentEntity1 = createGltfEntity()
        val parentEntity2 = createGltfEntity()

        childEntity.parent = parentEntity1

        assertThat(childEntity.parent).isEqualTo(parentEntity1)
        assertThat(parentEntity1.children).containsExactly(childEntity)
        assertThat(parentEntity2.children).isEmpty()

        val childNode = childEntity.getNode()

        assertThat(nodeRepository.getParent(childNode)).isEqualTo(parentEntity1.getNode())

        childEntity.parent = parentEntity2

        assertThat(childEntity.parent).isEqualTo(parentEntity2)
        assertThat(parentEntity2.children).containsExactly(childEntity)
        assertThat(parentEntity1.children).isEmpty()
        assertThat(nodeRepository.getParent(childNode)).isEqualTo(parentEntity2.getNode())
    }

    @Test
    fun gltfEntityAddChildren() {
        val childEntity1 = createGltfEntity()
        val childEntity2 = createGltfEntity()
        val parentEntity = createGltfEntity()

        parentEntity.addChild(childEntity1)

        assertThat(parentEntity.children).containsExactly(childEntity1)

        parentEntity.addChildren(ImmutableList.of(childEntity2))

        assertThat(childEntity1.parent).isEqualTo(parentEntity)
        assertThat(childEntity2.parent).isEqualTo(parentEntity)
        assertThat(parentEntity.children).containsExactly(childEntity1, childEntity2)

        val childNode1 = childEntity1.getNode()

        assertThat(nodeRepository.getParent(childNode1)).isEqualTo(parentEntity.getNode())

        val childNode2 = childEntity2.getNode()

        assertThat(nodeRepository.getParent(childNode2)).isEqualTo(parentEntity.getNode())
    }

    @Test
    fun createPanelEntity_returnsEntity() {
        assertThat(createPanelEntity()).isNotNull()
    }

    @Test
    fun allPanelEntities_haveActivitySpaceRootImplAsParentByDefault() {
        val panelEntity = createPanelEntity()

        assertThat(panelEntity.parent).isEqualTo(testRuntime.activitySpace)
    }

    @Test
    fun panelEntitySetParent_setsParent() {
        val childEntity = createPanelEntity()
        val parentEntity = createPanelEntity()

        childEntity.parent = parentEntity

        assertThat(childEntity.parent).isEqualTo(parentEntity)
        assertThat(childEntity.children).isEmpty()
        assertThat(parentEntity.children).containsExactly(childEntity)

        // Verify that there is an underlying extension node relationship.
        val childNode = getNode(childEntity)

        assertThat(nodeRepository.getParent(childNode)).isEqualTo(getNode(parentEntity))
    }

    @Test
    fun panelEntityUpdateParent_updatesParent() {
        val childEntity = createPanelEntity()
        val parentEntity1 = createPanelEntity()
        val parentEntity2 = createPanelEntity()

        childEntity.parent = parentEntity1

        assertThat(childEntity.parent).isEqualTo(parentEntity1)
        assertThat(parentEntity1.children).containsExactly(childEntity)
        assertThat(parentEntity2.children).isEmpty()

        val childNode = getNode(childEntity)

        assertThat(nodeRepository.getParent(childNode)).isEqualTo(getNode(parentEntity1))

        childEntity.parent = parentEntity2

        assertThat(childEntity.parent).isEqualTo(parentEntity2)
        assertThat(parentEntity2.children).containsExactly(childEntity)
        assertThat(parentEntity1.children).isEmpty()
        assertThat(nodeRepository.getParent(childNode)).isEqualTo(getNode(parentEntity2))
    }

    @Test
    fun panelEntityAddChildren_addsChildren() {
        val childEntity1 = createPanelEntity()
        val childEntity2 = createPanelEntity()
        val parentEntity = createPanelEntity()

        parentEntity.addChild(childEntity1)

        assertThat(parentEntity.children).containsExactly(childEntity1)

        parentEntity.addChildren(ImmutableList.of(childEntity2))

        assertThat(childEntity1.parent).isEqualTo(parentEntity)
        assertThat(childEntity2.parent).isEqualTo(parentEntity)
        assertThat(parentEntity.children).containsExactly(childEntity1, childEntity2)

        val childNode1 = getNode(childEntity1)

        assertThat(nodeRepository.getParent(childNode1)).isEqualTo(getNode(parentEntity))

        val childNode2 = getNode(childEntity2)

        assertThat(nodeRepository.getParent(childNode2)).isEqualTo(getNode(parentEntity))
    }

    @Test
    fun getMainPanelEntity_returnsPanelEntity() {
        assertThat(testRuntime.mainPanelEntity).isNotNull()
    }

    @Test
    fun getMainPanelEntity_usesWindowLeashNode() {
        val mainPanel = testRuntime.mainPanelEntity

        assertThat((mainPanel as MainPanelEntityImpl).getNode())
            .isEqualTo(ShadowXrExtensions.extract(xrExtensions).getMainWindowNode(activity))
    }

    @Test
    fun getPose_returnsSetPose() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion(1f, 2f, 3f, 4f))
        val identityPose = Pose()
        val panelEntity = createPanelEntity()
        val gltfEntity: GltfEntity = createGltfEntity()
        val loggingEntity = testRuntime.createLoggingEntity(identityPose)
        val entity = createEntity()

        assertPose(panelEntity.getPose(), identityPose)
        assertPose(gltfEntity.getPose(), identityPose)
        assertPose(loggingEntity.getPose(), identityPose)
        assertPose(entity.getPose(), identityPose)

        panelEntity.setPose(pose)
        gltfEntity.setPose(pose)
        loggingEntity.setPose(pose)
        entity.setPose(pose)

        assertPose(panelEntity.getPose(), pose)
        assertPose(gltfEntity.getPose(), pose)
        assertPose(loggingEntity.getPose(), pose)
        assertPose(entity.getPose(), pose)
    }

    @Test
    @Throws(Exception::class)
    fun getPose_returnsFactoryMethodPose() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion(1f, 2f, 3f, 4f))
        val panelEntity = createPanelEntity(pose)
        val gltfEntity = createGltfEntity(pose)
        val loggingEntity = testRuntime.createLoggingEntity(pose)
        val entity = createEntity(pose)

        assertPose(panelEntity.getPose(), pose)
        assertPose(gltfEntity.getPose(), pose)
        assertPose(loggingEntity.getPose(), pose)
        assertPose(entity.getPose(), pose)
    }

    @Test
    fun getPoseInActivitySpace_withParentChainTranslation_returnsOffsetPositionFromRoot() {
        // Create a simple pose with only a small translation on all axes
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion.Identity)

        // Set the activity space as the root of this entity hierarchy.
        val parentEntity =
            testRuntime.createEntity(pose, "parent", testRuntime.activitySpace) as AndroidXrEntity
        val childEntity1 = testRuntime.createEntity(pose, "child1", parentEntity) as AndroidXrEntity
        val childEntity2 = testRuntime.createEntity(pose, "child2", childEntity1) as AndroidXrEntity

        assertVector3(parentEntity.poseInActivitySpace.translation, Vector3(1f, 2f, 3f))
        assertVector3(childEntity1.poseInActivitySpace.translation, Vector3(2f, 4f, 6f))
        assertVector3(childEntity2.poseInActivitySpace.translation, Vector3(3f, 6f, 9f))
    }

    @Test
    fun getPoseInActivitySpace_withParentChainRotation_returnsOffsetRotationFromRoot() {
        // Create a pose with a translation and one with 90-degree rotation around the y-axis.
        val parentTranslation = Vector3(1f, 2f, 3f)
        val translatedPose = Pose(parentTranslation, Quaternion.Identity)
        val quaternion = fromAxisAngle(Vector3(0f, 1f, 0f), 90f)
        val rotatedPose = Pose(Vector3(0f, 0f, 0f), quaternion)

        // The parent has a translation and no rotation.
        val parentEntity =
            testRuntime.createEntity(translatedPose, "parent", testRuntime.activitySpace)
                as AndroidXrEntity

        // Each child adds a rotation, but no translation.
        val childEntity1 =
            testRuntime.createEntity(rotatedPose, "child1", parentEntity) as AndroidXrEntity
        val childEntity2 =
            testRuntime.createEntity(rotatedPose, "child2", childEntity1) as AndroidXrEntity

        // There should be no translation offset from the root, only changes in rotation.
        assertPose(parentEntity.poseInActivitySpace, translatedPose)
        assertPose(childEntity1.poseInActivitySpace, Pose(parentTranslation, quaternion))
        assertPose(
            childEntity2.poseInActivitySpace,
            Pose(parentTranslation, fromAxisAngle(Vector3(0f, 1f, 0f), 180f)),
        )
    }

    @Test
    fun getPoseInActivitySpace_withParentChainPoseOffsets_returnsOffsetPoseFromRoot() {
        // Create a pose with a 1D translation and a 90-degree rotation around the z axis.
        val parentTranslation = Vector3(1f, 0f, 0f)
        val quaternion = fromAxisAngle(Vector3(0f, 0f, 1f), 90f)
        val pose = Pose(parentTranslation, quaternion)

        // Each entity adds a translation and a rotation.
        val parentEntity =
            testRuntime.createEntity(pose, "parent", testRuntime.activitySpace) as AndroidXrEntity
        val childEntity1 = testRuntime.createEntity(pose, "child1", parentEntity) as AndroidXrEntity
        val childEntity2 = testRuntime.createEntity(pose, "child2", childEntity1) as AndroidXrEntity

        // Local pose of ActivitySpace's direct child must be the same as child's ActivitySpace
        // pose.
        assertPose(parentEntity.poseInActivitySpace, parentEntity.getPose())

        // Each child should be positioned one unit away at 90 degrees from its parent's position.
        // Since our coordinate system is right-handed, a +ve rotation around the z axis is a
        // counter-clockwise rotation of the XY plane.
        // First child should be 1 unit in the ActivitySpace's positive y direction from its parent
        assertPose(
            childEntity1.poseInActivitySpace,
            Pose(Vector3(1f, 1f, 0f), fromAxisAngle(Vector3(0f, 0f, 1f), 180f)),
        )
        // Second child should be 1 unit in the ActivitySpace's negative x direction from its parent
        assertPose(
            childEntity2.poseInActivitySpace,
            Pose(Vector3(0f, 1f, 0f), fromAxisAngle(Vector3(0f, 0f, 1f), 270f)),
        )
    }

    @Test
    @Throws(Exception::class)
    fun getPoseInActivitySpace_withScaledActivitySpaceParent_returnsPose() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion(1f, 2f, 3f, 4f))

        // Set the parent as the activity space so these entities' activitySpacePose should match
        // their
        // local pose relative to their parent regardless of the activity space
        // scale/position/rotation.
        val panelEntity = createPanelEntity(pose) as PanelEntityImpl
        val gltfEntity = createGltfEntity(pose) as GltfEntityImpl
        val entity = createEntity(pose) as AndroidXrEntity
        val activitySpace: ActivitySpace = testRuntime.activitySpace
        (activitySpace as ActivitySpaceImpl).setOpenXrReferenceSpaceTransform(
            fromTrs(Vector3(5f, 6f, 7f), fromEulerAngles(22f, 33f, 44f), Vector3(2f, 2f, 2f))
        )
        panelEntity.parent = activitySpace
        gltfEntity.parent = activitySpace
        entity.parent = activitySpace

        assertPose(panelEntity.poseInActivitySpace, pose)
        assertPose(gltfEntity.poseInActivitySpace, pose)
        assertPose(entity.poseInActivitySpace, pose)
    }

    @Test
    @Throws(Exception::class)
    fun getPoseInActivitySpace_withScale_returnsPose() {
        val localPose = Pose(Vector3(1f, 2f, 1f), Quaternion.Identity)

        // Create a hierarchy of entities each translated from their parent by (1,2,1) in parent
        // space.
        val child1 = createGltfEntity(localPose) as GltfEntityImpl
        val child2 = createGltfEntity(localPose) as GltfEntityImpl
        val child3 = createGltfEntity(localPose) as GltfEntityImpl
        val activitySpace: ActivitySpace = testRuntime.activitySpace
        (activitySpace as ActivitySpaceImpl).setOpenXrReferenceSpaceTransform(
            fromTrs(Vector3(5f, 6f, 7f), fromEulerAngles(22f, 33f, 44f), Vector3(2f, 2f, 2f))
        )

        // Set a non-unit local scale to each child.
        child1.parent = activitySpace
        child1.setScale(Vector3(2f, 2f, 2f))

        child2.parent = child1
        child2.setScale(Vector3(3f, 2f, 3f))

        child3.parent = child2
        child3.setScale(Vector3(1f, 1f, 2f))

        // The position (in ActivitySpace) should be:
        // child's local position * parent's scale in AS + parent's position since there's no
        // rotation.

        // Assuming c1 = child1, c2 = child2, c3 = child3, AS = activitySpace.
        // c1.posInAS = c1.localPos * AS.scaleInAS + AS.posInAS = (1,2,1) * (1,1,1) + (0,0,0) =
        // (1,2,1)
        assertPose(child1.poseInActivitySpace, Pose(Vector3(1f, 2f, 1f), Quaternion.Identity))

        // c2.posInAS = c2.localPos * c1.scaleInAS + c1.posInAS = (1,2,1) * (2,2,2) + (1,2,1) =
        // (3,6,3)
        assertPose(child2.poseInActivitySpace, Pose(Vector3(3f, 6f, 3f), Quaternion.Identity))

        // c2.scaleInA = c2.localScale * c1.scaleInAS * AS.scale = (3,2,3) * (2,2,2) * (1,1,1) =
        // (6,4,6)
        // c3.posInAS = c3.localPos * c2.scaleInAS + c2.posInAS = (1,2,1) * (6,4,6) + (3,6,3) =
        // (9,14,9)
        assertPose(child3.poseInActivitySpace, Pose(Vector3(9f, 14f, 9f), Quaternion.Identity))
    }

    @Test
    fun getActivitySpacePose_withParentChainTranslation_returnsOffsetPositionFromRoot() {
        // Create a simple pose with only a small translation on all axes
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion.Identity)

        // Set the ActivitySpace as the root of this entity hierarchy.
        val parentEntity = testRuntime.createEntity(pose, "parent", testRuntime.activitySpace)
        val childEntity1 = testRuntime.createEntity(pose, "child1", parentEntity)
        val childEntity2 = testRuntime.createEntity(pose, "child2", childEntity1)

        // The translations should accumulate with each child, but there should be no rotation.
        assertVector3(parentEntity.activitySpacePose.translation, Vector3(1f, 2f, 3f))
        assertVector3(childEntity1.activitySpacePose.translation, Vector3(2f, 4f, 6f))
        assertVector3(childEntity2.activitySpacePose.translation, Vector3(3f, 6f, 9f))
        assertRotation(childEntity2.activitySpacePose.rotation, Quaternion.Identity)
    }

    @Test
    fun getActivitySpacePose_withParentChainRotation_returnsOffsetRotationFromRoot() {
        // Create a pose with a translation and one with 90-degree rotation around the y-axis.
        val parentTranslation = Vector3(1f, 0f, 0f)
        val translatedPose = Pose(parentTranslation, Quaternion.Identity)
        val quaternion = fromAxisAngle(Vector3(0f, 1f, 0f), 90f)
        val rotatedPose = Pose(Vector3(0f, 0f, 0f), quaternion)

        // The parent has a translation and no rotation and each child adds a rotation.
        val parentEntity =
            testRuntime.createEntity(translatedPose, "parent", testRuntime.activitySpace)
        val childEntity1 = testRuntime.createEntity(rotatedPose, "child1", parentEntity)
        val childEntity2 = testRuntime.createEntity(rotatedPose, "child2", childEntity1)

        // There should be no translation offset from the parent, but rotations should accumulate.
        assertPose(parentEntity.activitySpacePose, translatedPose)
        assertPose(childEntity1.activitySpacePose, Pose(parentTranslation, quaternion))
        assertPose(
            childEntity2.activitySpacePose,
            Pose(parentTranslation, fromAxisAngle(Vector3(0f, 1f, 0f), 180f)),
        )
    }

    @Test
    fun getActivitySpacePose_withParentChainPoseOffsets_returnsOffsetPoseFromRoot() {
        // Create a pose with a 1D translation and a 90-degree rotation around the z axis.
        val parentTranslation = Vector3(1f, 0f, 0f)
        val quaternion = fromAxisAngle(Vector3(0f, 0f, 1f), 90f)
        val pose = Pose(parentTranslation, quaternion)

        // Each entity adds a translation and a rotation.
        val parentEntity = testRuntime.createEntity(pose, "parent", testRuntime.activitySpace)
        val childEntity1 = testRuntime.createEntity(pose, "child1", parentEntity)
        val childEntity2 = testRuntime.createEntity(pose, "child2", childEntity1)

        // Local pose of ActivitySpace's direct child must be the same as child's ActivitySpace
        // pose.
        assertPose(parentEntity.activitySpacePose, parentEntity.getPose())

        // Each child should be positioned one unit away at 90 degrees from its parent's position.
        // Since our coordinate system is right-handed, a +ve rotation around the z axis is a
        // counter-clockwise rotation of the XY plane.
        // First child should be 1 unit in the ActivitySpace's positive y direction from its parent
        assertPose(
            childEntity1.activitySpacePose,
            Pose(Vector3(1f, 1f, 0f), fromAxisAngle(Vector3(0f, 0f, 1f), 180f)),
        )
        // Second child should be 1 unit in the ActivitySpace's negative x direction from its parent
        assertPose(
            childEntity2.activitySpacePose,
            Pose(Vector3(0f, 1f, 0f), fromAxisAngle(Vector3(0f, 0f, 1f), 270f)),
        )
    }

    @Test
    @Throws(Exception::class)
    fun getActivitySpacePose_withDefaultParent_returnsPose() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion(1f, 2f, 3f, 4f))

        // All these entities should have the ActivitySpaceRootImpl as their parent by default.
        val panelEntity = createPanelEntity(pose)
        val gltfEntity = createGltfEntity(pose)
        val entity = createEntity(pose)

        assertPose(panelEntity.activitySpacePose, pose)
        assertPose(gltfEntity.activitySpacePose, pose)
        assertPose(entity.activitySpacePose, pose)
    }

    @Test
    @Throws(Exception::class)
    fun getPoseInActivitySpace_withScale_returnsScaledPose() {
        val localPose = Pose(Vector3(1f, 2f, 1f), Quaternion.Identity)

        // Create a hierarchy of entities each translated from their parent by (1,1,1) in parent
        // space.
        val child1 = createGltfEntity(localPose) as GltfEntityImpl
        val child2 = createGltfEntity(localPose) as GltfEntityImpl
        val child3 = createGltfEntity(localPose) as GltfEntityImpl

        assertVector3(testRuntime.activitySpace.getScale(Space.ACTIVITY), Vector3(1f, 1f, 1f))

        // Set a non-unit local scale to each child.
        child1.parent = testRuntime.activitySpace
        child1.setScale(Vector3(2f, 2f, 2f))

        child2.parent = child1
        child2.setScale(Vector3(3f, 2f, 3f))

        child3.parent = child2
        child3.setScale(Vector3(1f, 1f, 2f))

        // See getPoseInActivitySpace_withScale_returnsScaledPose for more detailed comments.
        // The position should be (parent's scale * child's position) + parent's position
        // since there's no rotation.
        assertPose(child1.activitySpacePose, Pose(Vector3(1f, 2f, 1f), Quaternion.Identity))
        assertPose(child2.activitySpacePose, Pose(Vector3(3f, 6f, 3f), Quaternion.Identity))
        assertPose(child3.activitySpacePose, Pose(Vector3(9f, 14f, 9f), Quaternion.Identity))
    }

    @Test
    @Throws(Exception::class)
    fun transformPoseTo_sameDestAndSourceEntity_returnsUnchangedPose() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion(1f, 2f, 3f, 4f))
        val identity = Pose()

        val panelEntity = createPanelEntity(pose)
        val gltfEntity = createGltfEntity(pose)
        val entity = createEntity(pose)

        assertPose(panelEntity.transformPoseTo(pose, panelEntity), pose)
        assertPose(gltfEntity.transformPoseTo(pose, gltfEntity), pose)
        assertPose(entity.transformPoseTo(pose, entity), pose)
        assertPose(panelEntity.transformPoseTo(identity, panelEntity), identity)
        assertPose(gltfEntity.transformPoseTo(identity, gltfEntity), identity)
        assertPose(entity.transformPoseTo(identity, entity), identity)
    }

    @Test
    fun transformPoseTo_withOnlyTranslationOffset_returnsTranslationDifference() {
        val sourceEntity = createPanelEntity() as PanelEntityImpl
        val destinationEntity = createGltfEntity()
        sourceEntity.setPose(Pose(Vector3(1f, 2f, 3f), sourceEntity.getPose().rotation))
        destinationEntity.setPose(Pose(Vector3(4f, 5f, 6f), destinationEntity.getPose().rotation))
        val offsetFromSource = Pose(Vector3(7f, 8f, 9f), Quaternion.Identity)

        // The expected translation is destOffset = (sourceOrigin + sourceOffset) - destOrigin
        // since there's no rotation and the entities are in the same coordinate space.
        // So ((1,2,3) + (7,8,9)) - (4,5,6) = (4, 5, 6)
        val offsetInDestinationSpace =
            sourceEntity.transformPoseTo(offsetFromSource, destinationEntity)
        val expectedPose = Pose(Vector3(4f, 5f, 6f), Quaternion.Identity)

        assertPose(offsetInDestinationSpace, expectedPose)
    }

    @Test
    fun transformPoseTo_withOnlyRotationOffset_returnsRotationDifference() {
        val sourceEntity = createPanelEntity() as PanelEntityImpl
        val destinationEntity = createGltfEntity()

        sourceEntity.setPose(
            Pose(sourceEntity.getPose().translation, fromEulerAngles(Vector3(1f, 2f, 3f)))
        )
        destinationEntity.setPose(
            Pose(destinationEntity.getPose().translation, fromEulerAngles(Vector3(4f, 5f, 6f)))
        )

        val offsetFromSource = Pose(Vector3(), fromEulerAngles(Vector3(7f, 8f, 9f)))

        // The expected rotation is (source + sourceOffset) - destination since the source and
        // destination are in the same coordinate space: ((1,2,3) + (7,8,9)) - (4,5,6) = (4, 5, 6)
        val offsetInDestinationSpace =
            sourceEntity.transformPoseTo(offsetFromSource, destinationEntity)
        val expectedPose = Pose(Vector3(), fromEulerAngles(Vector3(4f, 5f, 6f)))

        assertPose(offsetInDestinationSpace, expectedPose)
    }

    @Test
    fun transformPoseTo_withDifferentTranslationAndRotation_returnsTransformedPose() {
        // Assume the source and destination entities are in the same coordinate space.
        val sourceVector = Vector3(1f, 2f, 3f)
        val sourceQuaternion = fromAxisAngle(Vector3(0f, 0f, 1f), -90f)
        val destinationVector = Vector3(10f, 20f, 30f)
        val destinationQuaternion = fromAxisAngle(Vector3(0f, 0f, 1f), 90f)
        val identity = Pose()

        val sourceEntity = createEntity(Pose(sourceVector, sourceQuaternion)) as AndroidXrEntity
        val destinationEntity =
            createEntity(Pose(destinationVector, destinationQuaternion)) as AndroidXrEntity

        // Transform an identity pose from the source to the destination space.
        val sourceToDestinationPose = sourceEntity.transformPoseTo(identity, destinationEntity)

        // The expected rotation is the difference between the quaternions -90 - 90 = -180.
        val expectedQuaternion = fromAxisAngle(Vector3(0f, 0f, 1f), -180f)
        assertRotation(sourceToDestinationPose.rotation, expectedQuaternion)

        // The expected translation is the difference between the source and destination vectors
        // rotated
        // by the inverse of the destination quaternion.
        val expectedVector =
            destinationQuaternion.inverse.times(sourceVector.minus(destinationVector))

        // So difference is (1,2,3) - (10,20,30) = (-9,-18,-27) then rotate CCW by -90 degrees
        // around
        // the z axis (i.e. swap x and y, set x positive and y negative since we're rotating from
        // 3rd
        // quadrant to the 2nd quadrant of XY plane) => (-18, 9, -27)
        assertVector3(expectedVector, Vector3(-18f, 9f, -27f))
        assertVector3(sourceToDestinationPose.translation, expectedVector)

        // Transform an offset pose from the source to the destination.
        val offsetPose = Pose(Vector3(1f, 0f, 0f), fromAxisAngle(Vector3(0f, 0f, 1f), 20f))
        val newSourceToDestinationPose = sourceEntity.transformPoseTo(offsetPose, destinationEntity)

        // The expected rotation is the difference between the quaternions (20-90) - 90 = -160.
        val newExpectedQuaternion = fromAxisAngle(Vector3(0f, 0f, 1f), -160f)
        assertRotation(newSourceToDestinationPose.rotation, newExpectedQuaternion)

        // The expected translation is expected to be the same as the previous one but with the
        // offset
        // vector added to it in the destination space.
        val offsetInActivitySpace = sourceQuaternion.times(offsetPose.translation)
        val offsetInDestinationSpace = destinationQuaternion.inverse.times(offsetInActivitySpace)
        val newExpectedVector = expectedVector.plus(offsetInDestinationSpace)

        // So (1, 0, 0) rotated by -90 degrees around the z axis is (0, 1, 0) in activity space then
        // add to the difference from source to destination vector (-9,-18,-27) to get (-9, -19,
        // -27)
        // and finally rotate by the inverse of the destination quaternion to get (-19, 9, -27).
        assertVector3(newExpectedVector, Vector3(-19f, 9f, -27f))
        assertVector3(newSourceToDestinationPose.translation, newExpectedVector)
    }

    @Test
    fun getAlpha_returnsSetAlpha() {
        val panelEntity = createPanelEntity()
        val gltfEntity: GltfEntity = createGltfEntity()
        val entity = createEntity()

        assertThat(panelEntity.getAlpha()).isEqualTo(1.0f)
        assertThat(gltfEntity.getAlpha()).isEqualTo(1.0f)
        assertThat(entity.getAlpha()).isEqualTo(1.0f)

        panelEntity.setAlpha(0.5f)
        gltfEntity.setAlpha(0.5f)
        entity.setAlpha(0.5f)

        assertThat(panelEntity.getAlpha()).isEqualTo(0.5f)
        assertThat(gltfEntity.getAlpha()).isEqualTo(0.5f)
        assertThat(entity.getAlpha()).isEqualTo(0.5f)
        assertThat(nodeRepository.map(NodeRepository.NodeMetadata::getAlpha))
            .containsAtLeast(0.5f, 0.5f, 0.5f)
    }

    @Test
    fun getActivitySpaceAlpha_returnsTotalAncestorAlpha() {
        val grandparent = createPanelEntity()
        val parent: GltfEntity = createGltfEntity()
        val entity = createEntity()

        assertThat(grandparent.getAlpha(Space.ACTIVITY)).isEqualTo(1.0f)
        assertThat(parent.getAlpha(Space.ACTIVITY)).isEqualTo(1.0f)
        assertThat(entity.getAlpha(Space.ACTIVITY)).isEqualTo(1.0f)

        grandparent.setAlpha(0.5f)
        parent.parent = grandparent
        parent.setAlpha(0.5f)
        entity.parent = parent
        entity.setAlpha(0.5f)

        assertThat(grandparent.getAlpha(Space.ACTIVITY)).isEqualTo(0.5f)
        assertThat(parent.getAlpha(Space.ACTIVITY)).isEqualTo(0.25f)
        assertThat(entity.getAlpha(Space.ACTIVITY)).isEqualTo(0.125f)
        assertThat(nodeRepository.map(NodeRepository.NodeMetadata::getAlpha))
            .containsAtLeast(0.5f, 0.5f, 0.5f)
    }

    @Test
    fun transformPoseTo_withScaleAndNoOffset_returnsPose() {
        val sourceEntity = createPanelEntity() as PanelEntityImpl
        val destinationEntity = createGltfEntity()
        sourceEntity.setPose(Pose(Vector3(0f, 0f, 1f), Quaternion.Identity))
        sourceEntity.setScale(Vector3(2f, 2f, 2f))
        destinationEntity.setPose(Pose(Vector3(1f, 0f, 0f), Quaternion.Identity))
        destinationEntity.setScale(Vector3(3f, 3f, 3f))

        assertPose(
            sourceEntity.transformPoseTo(Pose.Identity, destinationEntity),
            Pose(Vector3(-1 / 3f, 0f, 1 / 3f), Quaternion.Identity),
        )
    }

    @Test
    fun transformPoseTo_withScale_returnsPose() {
        val sourceEntity = createPanelEntity() as PanelEntityImpl
        val destinationEntity = createGltfEntity()
        sourceEntity.setPose(Pose(Vector3(0f, 0f, 1f), Quaternion.Identity))
        sourceEntity.setScale(Vector3(2f, 2f, 2f))
        destinationEntity.setPose(Pose(Vector3(1f, 0f, 0f), Quaternion.Identity))
        destinationEntity.setScale(Vector3(3f, 3f, 3f))

        val offsetFromSource = Pose(Vector3(0f, 0f, 1f), Quaternion.Identity)

        assertPose(
            sourceEntity.transformPoseTo(offsetFromSource, destinationEntity),
            Pose(Vector3(-1 / 3f, 0f, 1f), Quaternion.Identity),
        )
    }

    @Test
    fun transformPoseTo_withNonUniformScalesAndTranslations_returnsPose() {
        val sourceEntity = createPanelEntity() as PanelEntityImpl
        val destinationEntity = createGltfEntity()
        sourceEntity.setPose(Pose(Vector3(0f, 0f, 1f), Quaternion.Identity))
        sourceEntity.setScale(Vector3(0.5f, 2f, -3f))
        destinationEntity.setPose(Pose(Vector3(1f, 1f, 0f), Quaternion.Identity))
        destinationEntity.setScale(Vector3(4f, 5f, 6f))

        val offsetFromSource = Pose(Vector3(1f, 3f, 1f), Quaternion.Identity)

        // translation is:
        //  ((localOffsetFromSource * scale of source) + sourceTranslation - destinationTranslation)
        //    * (1/scale of  destination)
        //
        //  ((1, 3, 1) * (1/2, 2, -3) + (0, 0, 1) - (1, 1, 0)) * (1/4, 1/5, 1/6) =
        //              ((1/2, 6, -3) + (0, 0, 1) - (1, 1, 0)) * (1/4, 1/5, 1/6) =
        //                                       (-1/2, 5, -2) * (1/4, 1/5, 1/6) =
        //                                                     (-1/8, 1, -2/6) = (-1/8, 1, -1/3)
        assertPose(
            sourceEntity.transformPoseTo(offsetFromSource, destinationEntity),
            Pose(Vector3(-1 / 8f, 1f, -1 / 3f), Quaternion.Identity),
        )
    }

    @Test
    fun addComponent_callsOnAttach() {
        val panelEntity = createPanelEntity()
        val gltfEntity: GltfEntity = createGltfEntity()
        val loggingEntity = testRuntime.createLoggingEntity(Pose())
        val component = mock<Component>()
        whenever(component.onAttach(any<Entity>())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(panelEntity)

        assertThat(gltfEntity.addComponent(component)).isTrue()
        verify(component).onAttach(gltfEntity)

        assertThat(loggingEntity.addComponent(component)).isTrue()
        verify(component).onAttach(loggingEntity)
    }

    @Test
    fun addComponent_failsIfOnAttachFails() {
        val panelEntity = createPanelEntity()
        val gltfEntity: GltfEntity = createGltfEntity()
        val loggingEntity = testRuntime.createLoggingEntity(Pose())
        val component = mock<Component>()
        whenever(component.onAttach(any<Entity>())).thenReturn(false)

        assertThat(panelEntity.addComponent(component)).isFalse()
        verify(component).onAttach(panelEntity)

        assertThat(gltfEntity.addComponent(component)).isFalse()
        verify(component).onAttach(gltfEntity)

        assertThat(loggingEntity.addComponent(component)).isFalse()
        verify(component).onAttach(loggingEntity)
    }

    @Test
    fun removeComponent_callsOnDetach() {
        val panelEntity = createPanelEntity()
        val gltfEntity: GltfEntity = createGltfEntity()
        val loggingEntity = testRuntime.createLoggingEntity(Pose())
        val component = mock<Component>()
        whenever(component.onAttach(any<Entity>())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(panelEntity)

        panelEntity.removeComponent(component)

        verify(component).onDetach(panelEntity)

        assertThat(gltfEntity.addComponent(component)).isTrue()
        verify(component).onAttach(gltfEntity)

        gltfEntity.removeComponent(component)

        verify(component).onDetach(gltfEntity)

        assertThat(loggingEntity.addComponent(component)).isTrue()
        verify(component).onAttach(loggingEntity)

        loggingEntity.removeComponent(component)

        verify(component).onDetach(loggingEntity)
    }

    @Test
    fun addingSameComponentTypeAgain_addsComponent() {
        val panelEntity = createPanelEntity()
        val gltfEntity: GltfEntity = createGltfEntity()
        val loggingEntity = testRuntime.createLoggingEntity(Pose())
        val component1 = mock<Component>()
        val component2 = mock<Component>()
        whenever(component1.onAttach(any<Entity>())).thenReturn(true)
        whenever(component2.onAttach(any<Entity>())).thenReturn(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(panelEntity)
        verify(component2).onAttach(panelEntity)

        assertThat(gltfEntity.addComponent(component1)).isTrue()
        assertThat(gltfEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(gltfEntity)
        verify(component2).onAttach(gltfEntity)

        assertThat(loggingEntity.addComponent(component1)).isTrue()
        assertThat(loggingEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(loggingEntity)
        verify(component2).onAttach(loggingEntity)
    }

    @Test
    fun addingDifferentComponentType_addComponentSucceeds() {
        val panelEntity = createPanelEntity()
        val gltfEntity: GltfEntity = createGltfEntity()
        val loggingEntity = testRuntime.createLoggingEntity(Pose())
        val component1 = mock<Component>()
        val component2: Component = mock<FakeComponent>()
        whenever(component1.onAttach(any<Entity>())).thenReturn(true)
        whenever(component2.onAttach(any<Entity>())).thenReturn(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(panelEntity)
        verify(component2).onAttach(panelEntity)

        assertThat(gltfEntity.addComponent(component1)).isTrue()
        assertThat(gltfEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(gltfEntity)
        verify(component2).onAttach(gltfEntity)

        assertThat(loggingEntity.addComponent(component1)).isTrue()
        assertThat(loggingEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(loggingEntity)
        verify(component2).onAttach(loggingEntity)
    }

    @Test
    fun removeAll_callsOnDetachOnAll() {
        val panelEntity = createPanelEntity()
        val gltfEntity: GltfEntity = createGltfEntity()
        val loggingEntity = testRuntime.createLoggingEntity(Pose())
        val component1 = mock<Component>()
        val component2: Component = mock<FakeComponent>()
        whenever(component1.onAttach(any<Entity>())).thenReturn(true)
        whenever(component2.onAttach(any<Entity>())).thenReturn(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(panelEntity)
        verify(component2).onAttach(panelEntity)

        panelEntity.removeAllComponents()

        verify(component1).onDetach(panelEntity)
        verify(component2).onDetach(panelEntity)

        assertThat(gltfEntity.addComponent(component1)).isTrue()
        assertThat(gltfEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(gltfEntity)
        verify(component2).onAttach(gltfEntity)

        gltfEntity.removeAllComponents()

        verify(component1).onDetach(gltfEntity)
        verify(component2).onDetach(gltfEntity)

        assertThat(loggingEntity.addComponent(component1)).isTrue()
        assertThat(loggingEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(loggingEntity)
        verify(component2).onAttach(loggingEntity)

        loggingEntity.removeAllComponents()

        verify(component1).onDetach(loggingEntity)
        verify(component2).onDetach(loggingEntity)
    }

    @Test
    fun addSameComponentTwice_callsOnAttachTwice() {
        val panelEntity = createPanelEntity()
        val gltfEntity: GltfEntity = createGltfEntity()
        val loggingEntity = testRuntime.createLoggingEntity(Pose())
        val component = mock<Component>()
        whenever(component.onAttach(any<Entity>())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        assertThat(panelEntity.addComponent(component)).isTrue()

        verify(component, times(2)).onAttach(panelEntity)
        assertThat(gltfEntity.addComponent(component)).isTrue()
        assertThat(gltfEntity.addComponent(component)).isTrue()
        verify(component, times(2)).onAttach(gltfEntity)
        assertThat(loggingEntity.addComponent(component)).isTrue()
        assertThat(loggingEntity.addComponent(component)).isTrue()
        verify(component, times(2)).onAttach(loggingEntity)
    }

    @Test
    fun removeSameComponentTwice_callsOnDetachOnce() {
        val panelEntity = createPanelEntity()
        val gltfEntity: GltfEntity = createGltfEntity()
        val loggingEntity = testRuntime.createLoggingEntity(Pose())
        val component = mock<Component>()
        whenever(component.onAttach(any<Entity>())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(panelEntity)

        panelEntity.removeComponent(component)
        panelEntity.removeComponent(component)

        verify(component).onDetach(panelEntity)
        assertThat(gltfEntity.addComponent(component)).isTrue()
        verify(component).onAttach(gltfEntity)

        gltfEntity.removeComponent(component)
        gltfEntity.removeComponent(component)

        verify(component).onDetach(gltfEntity)
        assertThat(loggingEntity.addComponent(component)).isTrue()
        verify(component).onAttach(loggingEntity)

        loggingEntity.removeComponent(component)
        loggingEntity.removeComponent(component)

        verify(component).onDetach(loggingEntity)
    }

    @Test
    fun createInteractableComponent_returnsComponent() {
        val mockInputEventListener = mock<InputEventListener>()
        val interactableComponent =
            testRuntime.createInteractableComponent(
                MoreExecutors.directExecutor(),
                mockInputEventListener,
            )

        assertThat(interactableComponent).isNotNull()
    }

    private fun sendInputEvent(node: Node, inputEvent: com.android.extensions.xr.node.InputEvent) {
        val shadowNode = ShadowNode.extract(node)
        shadowNode.inputExecutor.execute { shadowNode.inputListener.accept(inputEvent) }
    }

    @Test
    fun addInputEventConsumerToEntity_setsUpNodeListener() {
        val mockInputEventListener = mock<InputEventListener>()
        val panelEntity = createPanelEntity()
        val executor = MoreExecutors.directExecutor()
        panelEntity.addInputEventListener(executor, mockInputEventListener)
        val shadowNode = ShadowNode.extract(getNode(panelEntity))

        assertThat(shadowNode.inputListener).isNotNull()
        assertThat(shadowNode.inputExecutor).isEqualTo(fakeExecutor)

        val inputEvent =
            ShadowInputEvent.create(
                /* origin= */ Vec3(0f, 0f, 0f),
                /* direction= */ Vec3(1f, 1f, 1f),
            )
        sendInputEvent(getNode(panelEntity), inputEvent)
        fakeExecutor.runAll()

        verify(mockInputEventListener).onInputEvent(any())
    }

    @Test
    fun inputEvent_hasHitInfo() {
        val mockInputEventListener = mock<InputEventListener>()
        val panelEntity = createPanelEntity()
        val executor = MoreExecutors.directExecutor()
        panelEntity.addInputEventListener(executor, mockInputEventListener)
        val node = getNode(panelEntity)
        val inputEvent =
            ShadowInputEvent.create(
                /* origin= */ Vec3(0f, 0f, 0f),
                /* direction= */ Vec3(1f, 1f, 1f),
                /* hitInfo= */ com.android.extensions.xr.node.InputEvent.HitInfo(
                    /* subspaceImpressNodeId= */ 0,
                    /* inputNode= */ node,
                    /* transform= */ Mat4f(FloatArray(16)),
                    /* hitPosition= */ Vec3(1f, 2f, 3f),
                ),
                /* secondaryHitInfo= */ null,
            )
        sendInputEvent(node, inputEvent)
        fakeExecutor.runAll()

        val inputEventCaptor = argumentCaptor<InputEvent>()

        verify(mockInputEventListener).onInputEvent(inputEventCaptor.capture())

        val capturedEvent = inputEventCaptor.lastValue

        assertThat(capturedEvent.hitInfoList).isNotEmpty()

        val hitInfo = capturedEvent.hitInfoList[0]

        assertThat(hitInfo.inputEntity).isEqualTo(panelEntity)
        assertThat(hitInfo.hitPosition).isEqualTo(Vector3(1f, 2f, 3f))
    }

    @Test
    fun passingNoExecutorWhenAddingConsumer_usesInternalExecutor() {
        val mockInputEventListener = mock<InputEventListener>()
        val panelEntity = createPanelEntity()
        panelEntity.addInputEventListener(null, mockInputEventListener)
        val shadowNode = ShadowNode.extract(getNode(panelEntity))

        assertThat(shadowNode.inputListener).isNotNull()
        assertThat(shadowNode.inputExecutor).isNotNull()
    }

    @Test
    fun addMultipleInputEventConsumerToEntity_setsUpInputCallbacksForAll() {
        val mockInputEventListener1 = mock<InputEventListener>()
        val mockInputEventListener2 = mock<InputEventListener>()
        val panelEntity = createPanelEntity()
        val executor = MoreExecutors.directExecutor()
        panelEntity.addInputEventListener(executor, mockInputEventListener1)
        panelEntity.addInputEventListener(executor, mockInputEventListener2)
        val inputEvent =
            ShadowInputEvent.create(
                /* origin= */ Vec3(0f, 0f, 0f),
                /* direction= */ Vec3(1f, 1f, 1f),
            )
        sendInputEvent(getNode(panelEntity), inputEvent)
        fakeExecutor.runAll()

        verify(mockInputEventListener1).onInputEvent(any())

        verify(mockInputEventListener2).onInputEvent(any())
    }

    @Test
    fun addMultipleInputEventConsumersToEntity_setsUpInputCallbacksOnGivenExecutors() {
        val mockInputEventListener1 = mock<InputEventListener>()
        val mockInputEventListener2 = mock<InputEventListener>()
        val panelEntity = createPanelEntity()
        val executor1 = FakeScheduledExecutorService()
        val executor2 = FakeScheduledExecutorService()
        panelEntity.addInputEventListener(executor1, mockInputEventListener1)
        panelEntity.addInputEventListener(executor2, mockInputEventListener2)
        val inputEvent =
            ShadowInputEvent.create(
                /* origin= */ Vec3(0f, 0f, 0f),
                /* direction= */ Vec3(1f, 1f, 1f),
            )

        sendInputEvent(getNode(panelEntity), inputEvent)
        fakeExecutor.runAll()

        assertThat(executor1.hasNext()).isTrue()
        assertThat(executor2.hasNext()).isTrue()

        executor1.runAll()
        executor2.runAll()

        verify(mockInputEventListener1).onInputEvent(any())

        verify(mockInputEventListener2).onInputEvent(any())
    }

    @Test
    fun removeInputEventConsumerToEntity_removesFromCallbacks() {
        val mockInputEventListener1 = mock<InputEventListener>()
        val mockInputEventListener2 = mock<InputEventListener>()
        val panelEntity = createPanelEntity()
        val executor = MoreExecutors.directExecutor()
        panelEntity.addInputEventListener(executor, mockInputEventListener1)
        panelEntity.addInputEventListener(executor, mockInputEventListener2)
        val inputEvent =
            ShadowInputEvent.create(
                /* origin= */ Vec3(0f, 0f, 0f),
                /* direction= */ Vec3(1f, 1f, 1f),
            )

        sendInputEvent(getNode(panelEntity), inputEvent)
        fakeExecutor.runAll()

        verify(mockInputEventListener1).onInputEvent(any())
        verify(mockInputEventListener2).onInputEvent(any())
        clearInvocations(mockInputEventListener1, mockInputEventListener2)

        panelEntity.removeInputEventListener(mockInputEventListener1)

        sendInputEvent(getNode(panelEntity), inputEvent)
        fakeExecutor.runAll()

        verify(mockInputEventListener1, never()).onInputEvent(any())
        verify(mockInputEventListener2).onInputEvent(any())
    }

    @Test
    fun removeAllInputEventConsumers_stopsInputListening() {
        val mockInputEventListener1 = mock<InputEventListener>()
        val mockInputEventListener2 = mock<InputEventListener>()
        val panelEntity = createPanelEntity()
        val executor = MoreExecutors.directExecutor()
        panelEntity.addInputEventListener(executor, mockInputEventListener1)
        panelEntity.addInputEventListener(executor, mockInputEventListener2)
        val shadowNode = ShadowNode.extract(getNode(panelEntity))
        val inputEvent =
            ShadowInputEvent.create(
                /* origin= */ Vec3(0f, 0f, 0f),
                /* direction= */ Vec3(1f, 1f, 1f),
            )

        sendInputEvent(getNode(panelEntity), inputEvent)
        fakeExecutor.runAll()

        verify(mockInputEventListener1).onInputEvent(any())

        verify(mockInputEventListener2).onInputEvent(any())

        panelEntity.removeInputEventListener(mockInputEventListener1)
        panelEntity.removeInputEventListener(mockInputEventListener2)

        assertThat((panelEntity as PanelEntityImpl).reformEventConsumerMap).isEmpty()
        assertThat(shadowNode.inputListener).isNull()
        assertThat(shadowNode.inputExecutor).isNull()
    }

    @Test
    fun dispose_stopsInputListening() {
        val mockInputEventListener1 = mock<InputEventListener>()
        val mockInputEventListener2 = mock<InputEventListener>()
        val panelEntity = createPanelEntity()
        val executor = MoreExecutors.directExecutor()
        panelEntity.addInputEventListener(executor, mockInputEventListener1)
        panelEntity.addInputEventListener(executor, mockInputEventListener2)
        val shadowNode = ShadowNode.extract(getNode(panelEntity))
        val inputEvent =
            ShadowInputEvent.create(
                /* origin= */ Vec3(0f, 0f, 0f),
                /* direction= */ Vec3(1f, 1f, 1f),
            )

        sendInputEvent(getNode(panelEntity), inputEvent)
        fakeExecutor.runAll()

        verify(mockInputEventListener1).onInputEvent(any())

        verify(mockInputEventListener2).onInputEvent(any())

        panelEntity.dispose()

        assertThat((panelEntity as PanelEntityImpl).reformEventConsumerMap).isEmpty()
        assertThat(shadowNode.inputListener).isNull()
        assertThat(shadowNode.inputExecutor).isNull()
    }

    @Test
    fun isHidden_returnsSetHidden() {
        val parentEntity = createPanelEntity()

        assertThat(parentEntity.isHidden(true)).isFalse()
        assertThat(parentEntity.isHidden(false)).isFalse()

        val childEntity1 = createPanelEntity()
        val childEntity2 = createPanelEntity()
        childEntity1.parent = parentEntity
        childEntity2.parent = childEntity1

        assertThat(childEntity1.isHidden(true)).isFalse()
        assertThat(childEntity1.isHidden(false)).isFalse()

        parentEntity.setHidden(true)

        assertThat(parentEntity.isHidden(true)).isTrue()
        assertThat(parentEntity.isHidden(false)).isTrue()
        assertThat(childEntity1.isHidden(true)).isTrue()
        assertThat(childEntity1.isHidden(false)).isFalse()
        assertThat(childEntity2.isHidden(true)).isTrue()
        assertThat(childEntity2.isHidden(false)).isFalse()

        parentEntity.setHidden(false)

        assertThat(parentEntity.isHidden(true)).isFalse()
        assertThat(parentEntity.isHidden(false)).isFalse()
        assertThat(childEntity1.isHidden(true)).isFalse()
        assertThat(childEntity1.isHidden(false)).isFalse()
        assertThat(childEntity2.isHidden(true)).isFalse()
        assertThat(childEntity2.isHidden(false)).isFalse()

        childEntity1.setHidden(true)

        assertThat(parentEntity.isHidden(true)).isFalse()
        assertThat(parentEntity.isHidden(false)).isFalse()
        assertThat(childEntity1.isHidden(true)).isTrue()
        assertThat(childEntity1.isHidden(false)).isTrue()
        assertThat(childEntity2.isHidden(true)).isTrue()
        assertThat(childEntity2.isHidden(false)).isFalse()
    }

    @Test
    fun setHidden_modifiesReforms() {
        val testEntity = createPanelEntity()

        assertThat(
                testEntity.addComponent(
                    testRuntime.createMovableComponent(
                        systemMovable = true,
                        scaleInZ = true,
                        userAnchorable = false,
                    )
                )
            )
            .isTrue()

        testEntity.setHidden(true)

        assertThat(nodeRepository.getReformOptions(getNode(testEntity))).isNull()

        testEntity.setHidden(false)

        assertThat(nodeRepository.getReformOptions(getNode(testEntity)).enabledReform)
            .isEqualTo(ReformOptions.ALLOW_MOVE)
    }

    @Test
    fun constructor_initializesBoundaryConsentCacheCorrectly() {
        testRuntime.destroy()
        Settings.System.putInt(contentResolver, TOGGLE_GUARDIAN, 1)
        Settings.Secure.putInt(contentResolver, GUARDIAN_CONSENT_GRANTED, 1)
        testRuntime = createRuntime()

        var result = testRuntime.isBoundaryConsentGranted

        assertThat(result).isTrue()

        testRuntime.destroy()
        Settings.System.putInt(contentResolver, TOGGLE_GUARDIAN, 0)
        Settings.Secure.putInt(contentResolver, GUARDIAN_CONSENT_GRANTED, 1)
        testRuntime = createRuntime()

        result = testRuntime.isBoundaryConsentGranted

        assertThat(result).isTrue()

        testRuntime.destroy()
        Settings.System.putInt(contentResolver, TOGGLE_GUARDIAN, 0)
        Settings.Secure.putInt(contentResolver, GUARDIAN_CONSENT_GRANTED, 0)
        testRuntime = createRuntime()

        result = testRuntime.isBoundaryConsentGranted

        assertThat(result).isTrue()

        testRuntime.destroy()
        Settings.System.putInt(contentResolver, TOGGLE_GUARDIAN, 1)
        Settings.Secure.putInt(contentResolver, GUARDIAN_CONSENT_GRANTED, 0)
        testRuntime = createRuntime()

        result = testRuntime.isBoundaryConsentGranted

        assertThat(result).isFalse()
    }

    @Test
    fun constructor_registerBoundaryConsentStateListener() {
        assertThat(
                shadowContentResolver!!.getContentObservers(
                    IS_BOUNDARY_ENABLED_IN_DEVELOPER_OPTIONS_URI
                )
            )
            .hasSize(1)
        assertThat(
                shadowContentResolver.getContentObservers(
                    IS_EXPLICITLY_BOUNDARY_CONSENT_GRANTED_URI
                )
            )
            .hasSize(1)
    }

    @Test
    fun isBoundaryConsentGranted_returnsCachedValue() {
        // Set initial state to GRANTED = false
        Settings.System.putInt(contentResolver, TOGGLE_GUARDIAN, 1)
        Settings.Secure.putInt(contentResolver, GUARDIAN_CONSENT_GRANTED, 0)
        // Recreate runtime to pick up the new initial state
        testRuntime.destroy()
        testRuntime = createRuntime()
        assertThat(testRuntime.isBoundaryConsentGranted).isFalse()

        // Directly change the underlying setting without notifying the observer
        // This simulates a situation where the cache should NOT be updated.
        Settings.Secure.putInt(contentResolver, GUARDIAN_CONSENT_GRANTED, 1)

        // The method should still return the old, cached value.
        assertThat(testRuntime.isBoundaryConsentGranted).isFalse()
    }

    @Test
    fun addOnBoundaryConsentChangedListener_contentResolverChange_notifiesListeners() {
        val listener1 = mock<Consumer<Boolean>>()
        val listener2 = mock<Consumer<Boolean>>()
        testRuntime.addOnBoundaryConsentChangedListener(MoreExecutors.directExecutor(), listener1)
        testRuntime.addOnBoundaryConsentChangedListener(MoreExecutors.directExecutor(), listener2)

        // Simulate initial state of system settings
        Settings.System.putInt(contentResolver, TOGGLE_GUARDIAN, 1)
        Settings.Secure.putInt(contentResolver, GUARDIAN_CONSENT_GRANTED, 0)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fakeExecutor.runAll()

        // Change setting
        Settings.Secure.putInt(activity!!.contentResolver, GUARDIAN_CONSENT_GRANTED, 1)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fakeExecutor.runAll()

        verify(listener1, times(1)).accept(true)
        verify(listener2, times(1)).accept(true)

        clearInvocations(listener1, listener2)
        // Change setting again
        Settings.Secure.putInt(activity.contentResolver, GUARDIAN_CONSENT_GRANTED, 0)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fakeExecutor.runAll()
        verify(listener1, times(1)).accept(false)
        verify(listener2, times(1)).accept(false)
    }

    @Test
    fun removeOnBoundaryConsentChangedListener_stopsReceivingEvents() {
        // Recreate the runtime to ensure a clean initial state.
        testRuntime.destroy()
        // Set an explicit initial state (GRANTED = false).
        Settings.System.putInt(contentResolver, TOGGLE_GUARDIAN, 1)
        Settings.Secure.putInt(contentResolver, GUARDIAN_CONSENT_GRANTED, 0)
        testRuntime = createRuntime() // Recreate the runtime to ensure a clean state for the test.

        val listener1 = mock<Consumer<Boolean>>()
        val listener2 = mock<Consumer<Boolean>>()
        testRuntime.addOnBoundaryConsentChangedListener(MoreExecutors.directExecutor(), listener1)
        testRuntime.addOnBoundaryConsentChangedListener(MoreExecutors.directExecutor(), listener2)

        testRuntime.removeOnBoundaryConsentChangedListener(listener1)

        // Trigger a state change (from false to true).
        Settings.Secure.putInt(contentResolver, GUARDIAN_CONSENT_GRANTED, 1)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fakeExecutor.runAll()

        verify(listener2).accept(true)
        verify(listener1, never()).accept(any<Boolean>())
    }

    @Test
    fun destroy_unregisterBoundaryConsentStateListener() {
        // A runtime is created in setUp(), which registers the observer.
        assertThat(
                shadowContentResolver!!.getContentObservers(
                    IS_BOUNDARY_ENABLED_IN_DEVELOPER_OPTIONS_URI
                )
            )
            .isNotEmpty()
        assertThat(
                shadowContentResolver.getContentObservers(
                    IS_EXPLICITLY_BOUNDARY_CONSENT_GRANTED_URI
                )
            )
            .isNotEmpty()

        testRuntime.destroy()

        assertThat(
                shadowContentResolver.getContentObservers(
                    IS_BOUNDARY_ENABLED_IN_DEVELOPER_OPTIONS_URI
                )
            )
            .isEmpty()
        assertThat(
                shadowContentResolver.getContentObservers(
                    IS_EXPLICITLY_BOUNDARY_CONSENT_GRANTED_URI
                )
            )
            .isEmpty()
    }

    @Test
    fun dispose_clearsReformOptions() {
        val entity = createEntity() as AndroidXrEntity
        val reformOptions = entity.getReformOptions()

        assertThat(reformOptions).isNotNull()

        reformOptions.enabledReform = ReformOptions.ALLOW_MOVE or ReformOptions.ALLOW_RESIZE
        entity.dispose()

        assertThat(nodeRepository.getReformOptions(entity.getNode())).isNull()
    }

    @Test
    fun dispose_clearsParents() {
        val entity = createEntity() as AndroidXrEntity
        entity.parent = testRuntime.activitySpace

        assertThat(entity.parent).isNotNull()

        entity.dispose()

        assertThat(entity.parent).isNull()
    }

    @Test
    fun destroy_clearsResources() {
        val entity = createEntity() as AndroidXrEntity
        assertThat(entity.getNode()).isNotNull()
        assertThat(nodeRepository.getParent(entity.getNode())).isNotNull()

        testRuntime.destroy()

        assertThat(nodeRepository.getParent(entity.getNode())).isNull()
        assertThat(ShadowXrExtensions.extract(xrExtensions).getSpatialStateCallback(activity))
            .isNull()
        assertThat(ShadowXrExtensions.extract(xrExtensions).getMainWindowNode(activity)).isNull()
        assertThat(ShadowXrExtensions.extract(xrExtensions).getTaskNode(activity)).isNull()
    }

    @Test
    fun destroy_disposeInvoked() {
        val entity = createEntity() as AndroidXrEntity
        assertThat(entity.getNode()).isNotNull()
        assertThat(nodeRepository.getParent(entity.getNode())).isNotNull()

        testRuntime.destroy()

        assertThat(nodeRepository.getParent(entity.getNode())).isNull()
        assertThat(ShadowXrExtensions.extract(xrExtensions).getSpatialStateCallback(activity))
            .isNull()
        assertThat(ShadowXrExtensions.extract(xrExtensions).getMainWindowNode(activity)).isNull()
        assertThat(ShadowXrExtensions.extract(xrExtensions).getTaskNode(activity)).isNull()
    }

    @Test
    fun setKeyEntity_setsKeyEntity() {
        val entity = createEntity()
        testRuntime.keyEntity = entity
        assertThat(testRuntime.keyEntity).isEqualTo(entity)
    }

    @Test
    fun setKeyEntity_apiV1_doesNotSubscribe() {
        testRuntime.destroy()
        ShadowXrExtensions.extract(xrExtensions).setApiVersion(1)
        testRuntime = createRuntime()

        val entity = createEntity()
        val node = ShadowNode.extract((entity as AndroidXrEntity).getNode())

        testRuntime.keyEntity = entity

        assertThat(node.transformListener).isNull()
        assertThat(testRuntime.keyEntityTransformCloseable).isNull()
    }

    @Test
    fun setKeyEntity_apiV2_subscribesToTransformAndUpdatesHint() {
        testRuntime.destroy()
        ShadowXrExtensions.extract(xrExtensions).setApiVersion(2)
        testRuntime = createRuntime()

        val entity = createEntity()
        val node = ShadowNode.extract((entity as AndroidXrEntity).getNode())

        testRuntime.keyEntity = entity

        assertThat(node.transformListener).isNotNull()
        assertThat(node.transformExecutor).isEqualTo(fakeExecutor)
        assertThat(testRuntime.keyEntityTransformCloseable).isNotNull()

        // Trigger callback
        node.transformExecutor.execute {
            node.transformListener.accept(ShadowNodeTransform.create(Mat4f(FloatArray(16))))
        }

        assertThat(fakeExecutor.hasNext()).isTrue()
        fakeExecutor.runAll()
        // Since we cannot verify the extension call directly, we just ensure it doesn't crash.
    }

    @Test
    @Throws(Exception::class)
    fun setKeyEntity_apiV2_null_unsubscribes() {
        testRuntime.destroy()
        ShadowXrExtensions.extract(xrExtensions).setApiVersion(2)
        testRuntime = createRuntime()

        val entity = createEntity()
        val node = ShadowNode.extract((entity as AndroidXrEntity).getNode())

        testRuntime.keyEntity = entity

        assertThat(testRuntime.keyEntityTransformCloseable).isNotNull()

        val keyEntityTransformCloseable = testRuntime.keyEntityTransformCloseable as FakeCloseable

        assertThat(node.transformListener).isNotNull()
        assertThat(node.transformExecutor).isEqualTo(fakeExecutor)
        assertThat(keyEntityTransformCloseable.isClosed).isFalse()

        testRuntime.keyEntity = null

        assertThat(testRuntime.keyEntity).isNull()
        assertThat(keyEntityTransformCloseable.isClosed).isTrue()
        assertThat(testRuntime.keyEntityTransformCloseable).isNull()
    }

    @Test
    fun setKeyEntity_apiV2_sameEntity_doesNotResubscribe() {
        testRuntime.destroy()
        ShadowXrExtensions.extract(xrExtensions).setApiVersion(2)
        testRuntime = createRuntime()

        val entity = createEntity()
        testRuntime.keyEntity = entity
        val closeable1 = testRuntime.keyEntityTransformCloseable

        testRuntime.keyEntity = entity
        val closeable2 = testRuntime.keyEntityTransformCloseable

        assertThat(closeable1).isSameInstanceAs(closeable2)
    }

    @Test
    @Throws(Exception::class)
    fun setKeyEntity_apiV2_differentEntity_resubscribes() {
        testRuntime.destroy()
        ShadowXrExtensions.extract(xrExtensions).setApiVersion(2)
        testRuntime = createRuntime()

        val entity1 = createEntity()
        val entity2 = createEntity()
        val node1 = ShadowNode.extract((entity1 as AndroidXrEntity).getNode())
        val node2 = ShadowNode.extract((entity2 as AndroidXrEntity).getNode())

        testRuntime.keyEntity = entity1
        val closeable1 = testRuntime.keyEntityTransformCloseable as FakeCloseable
        assertThat(closeable1.isClosed).isFalse()
        assertThat(node1.transformListener).isNotNull()

        testRuntime.keyEntity = entity2
        val closeable2 = testRuntime.keyEntityTransformCloseable as FakeCloseable

        assertThat(closeable1.isClosed).isTrue()
        assertThat(closeable2).isNotSameInstanceAs(closeable1)
        assertThat(closeable2.isClosed).isFalse()
        assertThat(node2.transformListener).isNotNull()
        assertThat(testRuntime.keyEntity).isEqualTo(entity2)
    }

    @Test
    fun setKeyEntity_nullWhenAlreadyNull_doesNothing() {
        testRuntime.keyEntity = null
        assertThat(testRuntime.keyEntity).isNull()
        assertThat(testRuntime.keyEntityTransformCloseable).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun destroy_apiV2_unsubscribesKeyEntity() {
        testRuntime.destroy()
        ShadowXrExtensions.extract(xrExtensions).setApiVersion(2)
        testRuntime = createRuntime()

        val entity = createEntity()
        testRuntime.keyEntity = entity
        val closeable = testRuntime.keyEntityTransformCloseable as FakeCloseable
        assertThat(closeable.isClosed).isFalse()

        testRuntime.destroy()

        assertThat(closeable.isClosed).isTrue()
        assertThat(testRuntime.keyEntityTransformCloseable).isNull()
    }

    companion object {
        private const val OPEN_XR_REFERENCE_SPACE_TYPE = 1
        private const val GUARDIAN_CONSENT_GRANTED = "guardian_consent_granted"
        private const val TOGGLE_GUARDIAN = "toggle_guardian"
        private val IS_EXPLICITLY_BOUNDARY_CONSENT_GRANTED_URI: Uri =
            Settings.Secure.getUriFor(GUARDIAN_CONSENT_GRANTED)
        private val IS_BOUNDARY_ENABLED_IN_DEVELOPER_OPTIONS_URI: Uri =
            Settings.System.getUriFor(TOGGLE_GUARDIAN)
    }
}
