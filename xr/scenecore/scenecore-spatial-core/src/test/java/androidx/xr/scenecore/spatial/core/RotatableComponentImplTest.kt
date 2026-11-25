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

package androidx.xr.scenecore.spatial.core

import android.app.Activity
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.GeometryAffordanceState
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.MeshFeature
import androidx.xr.scenecore.runtime.NodeHolder
import androidx.xr.scenecore.runtime.ReformAffordanceFlag
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.node.InputEvent
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.ShadowInputEvent
import com.android.extensions.xr.node.ShadowNode
import com.android.extensions.xr.node.Vec3
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit
import kotlin.math.abs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RotatableComponentImplTest {
    private val activityController: ActivityController<Activity> =
        Robolectric.buildActivity(Activity::class.java)
    private val activity: Activity = activityController.create().start().get()
    private val fakeExecutor = FakeScheduledExecutorService()
    private val xrExtensions = SpatialCoreXrExtensionsHolderProvider.extensionsLegacy
    private val sceneNodeRegistry = SceneNodeRegistry()
    private val mockGltfFeature: GltfFeature = mock<GltfFeature>()
    private val mockMeshFeature: MeshFeature = mock<MeshFeature>()
    private lateinit var activitySpaceImpl: ActivitySpaceImpl
    private lateinit var sceneRuntime: SpatialSceneRuntime

    @Before
    fun setUp() {
        TruthJUnit.assume().that(xrExtensions).isNotNull()
        activitySpaceImpl =
            ActivitySpaceImpl(
                xrExtensions.createNode(),
                activity,
                xrExtensions,
                sceneNodeRegistry,
                { xrExtensions.getSpatialState(activity) },
                fakeExecutor,
            )
        sceneRuntime =
            SpatialSceneRuntime.create(activity, fakeExecutor, xrExtensions, sceneNodeRegistry)
    }

    @After
    fun tearDown() {
        sceneRuntime.destroy()
    }

    private fun createGltfEntity(
        boundingBox: BoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)
    ): GltfEntityImpl {
        whenever(mockGltfFeature.getGltfModelBoundingBox()).thenReturn(boundingBox)
        whenever(mockGltfFeature.getNodeHolder())
            .thenReturn(NodeHolder<Node>(xrExtensions.createNode(), Node::class.java))
        return GltfEntityImpl(
            activity,
            mockGltfFeature,
            activitySpaceImpl,
            xrExtensions,
            sceneNodeRegistry,
            fakeExecutor,
        )
    }

    private fun createMeshEntity(
        boundingBox: BoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)
    ): MeshEntityImpl {
        whenever(mockMeshFeature.meshBoundingBox).thenReturn(boundingBox)
        whenever(mockMeshFeature.getNodeHolder())
            .thenReturn(NodeHolder<Node>(xrExtensions.createNode(), Node::class.java))
        return MeshEntityImpl(
            activity,
            mockMeshFeature,
            activitySpaceImpl,
            xrExtensions,
            sceneNodeRegistry,
            fakeExecutor,
        )
    }

    private fun createRotatableComponent(): RotatableComponentImpl =
        RotatableComponentImpl(fakeExecutor)

    private fun createInputEvent(action: Int): InputEvent =
        ShadowInputEvent.create(
            InputEvent.SOURCE_HANDS,
            InputEvent.POINTER_TYPE_RIGHT,
            /* timestamp= */ 0,
            Vec3(0f, 0f, 0f),
            Vec3(0f, 0f, -1f),
            InputEvent.DISPATCH_FLAG_NONE,
            action,
        )

    /**
     * Mirrors what the rendering layer does for every input event on a glTF model with an enabled
     * reform affordance: it publishes the affordance [state] and the [recommendedRotation] (the
     * entity's recommended rotation in the runtime's world space) on the entity and then dispatches
     * the input event to the entity's node.
     */
    private fun sendRotationInputEvent(
        entity: AndroidXrEntity,
        state: GeometryAffordanceState,
        recommendedRotation: Quaternion,
        action: Int = InputEvent.ACTION_MOVE,
    ) {
        when (entity) {
            is GltfEntityImpl -> {
                entity.affordanceState = state
                entity.recommendedAffordanceRotation = recommendedRotation
            }
            is MeshEntityImpl -> {
                entity.affordanceState = state
                entity.recommendedAffordanceRotation = recommendedRotation
            }
            else -> throw IllegalArgumentException("Unsupported entity: $entity")
        }
        val shadowNode = ShadowNode.extract(entity.node)
        shadowNode.inputExecutor.execute {
            shadowNode.inputListener.accept(createInputEvent(action))
        }
        fakeExecutor.runAll()
    }

    /** Asserts that two quaternions describe the same rotation (q and -q are equivalent). */
    private fun assertSameRotation(actual: Quaternion, expected: Quaternion) {
        val dot = abs(Quaternion.dot(actual, expected))
        assertThat(dot).isWithin(1e-4f).of(1f)
    }

    private fun assertVectorEquals(actual: Vector3, expected: Vector3) {
        assertThat(actual.x).isWithin(1e-4f).of(expected.x)
        assertThat(actual.y).isWithin(1e-4f).of(expected.y)
        assertThat(actual.z).isWithin(1e-4f).of(expected.z)
    }

    @Test
    fun createRotatableComponent_returnsRotatableComponentImpl() {
        val component = sceneRuntime.createRotatableComponent()

        assertThat(component).isInstanceOf(RotatableComponentImpl::class.java)
    }

    @Test
    fun addComponent_toGltfEntity_enablesColliderAndRotatableAffordance() {
        val entity = createGltfEntity()
        val component = createRotatableComponent()

        assertThat(entity.addComponent(component)).isTrue()

        verify(mockGltfFeature).setColliderEnabled(true)
        verify(mockGltfFeature)
            .setReformAffordanceEnabled(entity, true, ReformAffordanceFlag.ROTATABLE)
    }

    @Test
    fun addComponent_toNonGltfEntity_returnsFalse() {
        val entity = sceneRuntime.createEntity(Pose(), "test", sceneRuntime.activitySpace)
        val component = createRotatableComponent()

        assertThat(entity.addComponent(component)).isFalse()
    }

    @Test
    fun addComponent_twice_returnsFalse() {
        val entity = createGltfEntity()
        val otherEntity = createGltfEntity()
        val component = createRotatableComponent()

        assertThat(entity.addComponent(component)).isTrue()
        assertThat(otherEntity.addComponent(component)).isFalse()
    }

    @Test
    fun removeComponent_disablesRotatableAffordanceAndCollider() {
        val entity = createGltfEntity()
        val component = createRotatableComponent()
        assertThat(entity.addComponent(component)).isTrue()

        entity.removeComponent(component)

        verify(mockGltfFeature)
            .setReformAffordanceEnabled(entity, false, ReformAffordanceFlag.ROTATABLE)
        verify(mockGltfFeature).setColliderEnabled(false)
    }

    @Test
    fun removeComponent_keepsColliderWhileInteractableComponentIsAttached() {
        val entity = createGltfEntity()
        val component = createRotatableComponent()
        assertThat(entity.addComponent(component)).isTrue()
        assertThat(entity.addComponent(InteractableComponentImpl(fakeExecutor, mock()))).isTrue()

        entity.removeComponent(component)

        verify(mockGltfFeature, never()).setColliderEnabled(false)
    }

    @Test
    fun addComponent_secondComponentOnSameGltfEntity_returnsFalseWithoutSideEffects() {
        val entity = createGltfEntity()
        assertThat(entity.addComponent(createRotatableComponent())).isTrue()

        assertThat(entity.addComponent(createRotatableComponent())).isFalse()

        // Only the first component enabled the collider, the affordance and an input listener.
        verify(mockGltfFeature).setColliderEnabled(true)
        verify(mockGltfFeature)
            .setReformAffordanceEnabled(entity, true, ReformAffordanceFlag.ROTATABLE)
        assertThat(entity.inputEventListenerMap).hasSize(1)
    }

    @Test
    fun removeComponent_rejectedSecondComponent_keepsFirstComponentRotating() {
        val entity = createGltfEntity(BoundingBox.fromMinMax(Vector3.Zero, Vector3.Zero))
        assertThat(entity.addComponent(createRotatableComponent())).isTrue()
        val rejected = createRotatableComponent()
        assertThat(entity.addComponent(rejected)).isFalse()

        entity.removeComponent(rejected)

        verify(mockGltfFeature, never())
            .setReformAffordanceEnabled(entity, false, ReformAffordanceFlag.ROTATABLE)
        val yaw = Quaternion.fromAxisAngle(Vector3.Up, 90f)
        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            Quaternion.Identity,
            InputEvent.ACTION_DOWN,
        )
        sendRotationInputEvent(entity, GeometryAffordanceState.ROTATION, yaw)
        assertSameRotation(entity.getPose(Space.PARENT).rotation, yaw)
    }

    @Test
    fun addComponent_secondComponentOnSameMeshEntity_succeedsOnlyAfterFirstIsRemoved() {
        val entity = createMeshEntity()
        val first = createRotatableComponent()
        val second = createRotatableComponent()
        assertThat(entity.addComponent(first)).isTrue()

        assertThat(entity.addComponent(second)).isFalse()

        entity.removeComponent(first)
        assertThat(entity.addComponent(second)).isTrue()
    }

    @Test
    fun addComponent_withOtherComponentTypeAttached_succeeds() {
        val entity = createGltfEntity()
        assertThat(entity.addComponent(InteractableComponentImpl(fakeExecutor, mock()))).isTrue()

        assertThat(entity.addComponent(createRotatableComponent())).isTrue()
    }

    @Test
    fun rotationGesture_appliesYawDeltaToEntityPose() {
        val entity = createGltfEntity(BoundingBox.fromMinMax(Vector3.Zero, Vector3.Zero))
        entity.setPose(Pose(Vector3(1f, 2f, 3f), Quaternion.Identity), Space.PARENT)
        assertThat(entity.addComponent(createRotatableComponent())).isTrue()
        val worldRotationAtStart = Quaternion.Identity
        val yaw = Quaternion.fromAxisAngle(Vector3.Up, 90f)

        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            worldRotationAtStart,
            InputEvent.ACTION_DOWN,
        )
        sendRotationInputEvent(entity, GeometryAffordanceState.ROTATION, yaw * worldRotationAtStart)

        val pose = entity.getPose(Space.PARENT)
        assertSameRotation(pose.rotation, yaw)
        // The bounding box center coincides with the entity origin, so it does not move.
        assertVectorEquals(pose.translation, Vector3(1f, 2f, 3f))
    }

    @Test
    fun rotationGesture_pitchInWorldSpace_isAppliedInParentSpace() {
        // The entity's parent space (the activity space) is yawed by 90 degrees relative to the
        // runtime's world space, which is what the recommended rotation is expressed in. The entity
        // itself is not rotated in its parent space, so its world rotation equals that yaw.
        val worldFromParent = Quaternion.fromAxisAngle(Vector3.Up, 90f)
        val entity = createGltfEntity(BoundingBox.fromMinMax(Vector3.Zero, Vector3.Zero))
        entity.setPose(Pose(Vector3.Zero, Quaternion.Identity), Space.PARENT)
        assertThat(entity.addComponent(createRotatableComponent())).isTrue()
        // A pitch about the world's X axis, as produced by a vertical hand drag while looking down
        // the world's -Z axis.
        val worldPitch = Quaternion.fromAxisAngle(Vector3.Right, 30f)

        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            worldFromParent,
            InputEvent.ACTION_DOWN,
        )
        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            worldPitch * worldFromParent,
        )

        // The same physical rotation expressed in the parent space.
        val expected = worldFromParent.inverse * worldPitch * worldFromParent
        val actual = entity.getPose(Space.PARENT).rotation
        assertSameRotation(actual, expected)
        // Applying the world-space pitch directly in the parent space would rotate about the
        // wrong axis.
        assertThat(abs(Quaternion.dot(actual, worldPitch))).isLessThan(0.999f)
    }

    @Test
    fun rotationGesture_pivotsAroundScaledBoundingBoxCenter() {
        val boundingBox = BoundingBox.fromMinMax(Vector3(0f, 0f, 0f), Vector3(1f, 2f, 1f))
        val entity = createGltfEntity(boundingBox)
        val startTranslation = Vector3(-1f, -0.6f, -1.2f)
        entity.setPose(Pose(startTranslation, Quaternion.Identity), Space.PARENT)
        entity.setScale(Vector3(2f, 2f, 2f), Space.PARENT)
        assertThat(entity.addComponent(createRotatableComponent())).isTrue()
        val yaw = Quaternion.fromAxisAngle(Vector3.Up, 90f)

        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            Quaternion.Identity,
            InputEvent.ACTION_DOWN,
        )
        sendRotationInputEvent(entity, GeometryAffordanceState.ROTATION, yaw)

        val pose = entity.getPose(Space.PARENT)
        assertSameRotation(pose.rotation, yaw)
        // The pivot (bounding box center, in parent-space units) stays where it was.
        val pivotOffset = Vector3(0.5f, 1f, 0.5f) * 2f
        val pivotBefore = startTranslation + pivotOffset
        val pivotAfter = pose.translation + pose.rotation * pivotOffset
        assertVectorEquals(pivotAfter, pivotBefore)
    }

    @Test
    fun rotationGesture_endOfGesture_keepsOrientation() {
        val entity = createGltfEntity(BoundingBox.fromMinMax(Vector3.Zero, Vector3.Zero))
        assertThat(entity.addComponent(createRotatableComponent())).isTrue()
        val yaw = Quaternion.fromAxisAngle(Vector3.Up, 45f)
        val pitch = Quaternion.fromAxisAngle(Vector3.Right, 20f)

        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            Quaternion.Identity,
            InputEvent.ACTION_DOWN,
        )
        sendRotationInputEvent(entity, GeometryAffordanceState.ROTATION, pitch * yaw)
        // On release the runtime drops the pitch from its own target; the entity must keep the
        // orientation it had when released.
        sendRotationInputEvent(entity, GeometryAffordanceState.IDLE, yaw, InputEvent.ACTION_UP)
        // Subsequent idle events (e.g. hover) must not move the entity either.
        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.IDLE,
            Quaternion.Identity,
            InputEvent.ACTION_HOVER_MOVE,
        )

        assertSameRotation(entity.getPose(Space.PARENT).rotation, pitch * yaw)
    }

    @Test
    fun rotationGesture_secondGesture_composesWithCurrentPose() {
        val entity = createGltfEntity(BoundingBox.fromMinMax(Vector3.Zero, Vector3.Zero))
        assertThat(entity.addComponent(createRotatableComponent())).isTrue()
        val firstYaw = Quaternion.fromAxisAngle(Vector3.Up, 30f)
        val secondYaw = Quaternion.fromAxisAngle(Vector3.Up, 50f)

        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            Quaternion.Identity,
            InputEvent.ACTION_DOWN,
        )
        sendRotationInputEvent(entity, GeometryAffordanceState.ROTATION, firstYaw)
        sendRotationInputEvent(entity, GeometryAffordanceState.IDLE, firstYaw, InputEvent.ACTION_UP)
        // The runtime re-synchronizes its baseline with the entity's world rotation while idle.
        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            firstYaw,
            InputEvent.ACTION_DOWN,
        )
        sendRotationInputEvent(entity, GeometryAffordanceState.ROTATION, secondYaw * firstYaw)

        assertSameRotation(entity.getPose(Space.PARENT).rotation, secondYaw * firstYaw)
    }

    @Test
    fun addComponent_toMeshEntity_enablesColliderAndRotatableAffordance() {
        val entity = createMeshEntity()
        val component = createRotatableComponent()

        assertThat(entity.addComponent(component)).isTrue()

        verify(mockMeshFeature).setColliderEnabled(true)
        verify(mockMeshFeature)
            .setReformAffordanceEnabled(entity, true, ReformAffordanceFlag.ROTATABLE)

        entity.removeComponent(component)

        verify(mockMeshFeature)
            .setReformAffordanceEnabled(entity, false, ReformAffordanceFlag.ROTATABLE)
        verify(mockMeshFeature).setColliderEnabled(false)
    }

    @Test
    fun rotationGesture_onMeshEntity_pivotsAroundMeshBoundingBoxCenter() {
        val entity = createMeshEntity(BoundingBox.fromMinMax(Vector3.Zero, Vector3(1f, 2f, 1f)))
        val startTranslation = Vector3(0.5f, -0.5f, -2f)
        entity.setPose(Pose(startTranslation, Quaternion.Identity), Space.PARENT)
        assertThat(entity.addComponent(createRotatableComponent())).isTrue()
        val yaw = Quaternion.fromAxisAngle(Vector3.Up, 90f)

        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            Quaternion.Identity,
            InputEvent.ACTION_DOWN,
        )
        sendRotationInputEvent(entity, GeometryAffordanceState.ROTATION, yaw)

        val pose = entity.getPose(Space.PARENT)
        assertSameRotation(pose.rotation, yaw)
        val pivotOffset = Vector3(0.5f, 1f, 0.5f)
        assertVectorEquals(
            pose.translation + pose.rotation * pivotOffset,
            startTranslation + pivotOffset,
        )
    }

    @Test
    fun reset_restoresAttachOrientationAroundCurrentCenter() {
        val boundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3(1f, 2f, 1f))
        val entity = createGltfEntity(boundingBox)
        val attachRotation = Quaternion.fromAxisAngle(Vector3.Up, 30f)
        entity.setPose(Pose(Vector3(-1f, -0.6f, -1.2f), attachRotation), Space.PARENT)
        val component = createRotatableComponent()
        assertThat(entity.addComponent(component)).isTrue()
        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            Quaternion.Identity,
            InputEvent.ACTION_DOWN,
        )
        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            Quaternion.fromAxisAngle(Vector3.Right, 40f) *
                Quaternion.fromAxisAngle(Vector3.Up, 80f),
        )
        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.IDLE,
            Quaternion.Identity,
            InputEvent.ACTION_UP,
        )
        // The app (or a MovableComponent) moved the entity after the gesture.
        val movedPose = entity.getPose(Space.PARENT)
        val afterMove = Pose(movedPose.translation + Vector3(0.3f, 0f, 0.2f), movedPose.rotation)
        entity.setPose(afterMove, Space.PARENT)
        val pivotOffset = Vector3(0.5f, 1f, 0.5f)
        val pivotBeforeReset = afterMove.translation + afterMove.rotation * pivotOffset

        component.reset()
        fakeExecutor.runAll()

        val pose = entity.getPose(Space.PARENT)
        assertSameRotation(pose.rotation, attachRotation)
        assertVectorEquals(pose.translation + pose.rotation * pivotOffset, pivotBeforeReset)
    }

    @Test
    fun reset_afterOriginalParentIsDetached_restoresAttachOrientationInCurrentParent() {
        val entity = createGltfEntity()
        val originalParent =
            sceneRuntime.createEntity(
                Pose(Vector3.Zero, Quaternion.fromAxisAngle(Vector3.Up, 20f)),
                "originalParent",
                sceneRuntime.activitySpace,
            )
        val newParent =
            sceneRuntime.createEntity(
                Pose(Vector3.Zero, Quaternion.fromAxisAngle(Vector3.Up, -40f)),
                "newParent",
                sceneRuntime.activitySpace,
            )
        entity.parent = originalParent
        entity.setPose(Pose(Vector3.Zero, Quaternion.fromAxisAngle(Vector3.Up, 30f)), Space.PARENT)
        val component = createRotatableComponent()
        assertThat(entity.addComponent(component)).isTrue()
        // Move the entity to a differently rotated parent, then detach the original one so that
        // its pose can no longer be resolved.
        entity.parent = newParent
        entity.setPose(Pose.Identity, Space.PARENT)
        originalParent.parent = null

        component.reset()
        fakeExecutor.runAll()

        // Same orientation in the activity space as when attached: 20 + 30 = -40 + 90 degrees.
        assertSameRotation(
            entity.getPose(Space.PARENT).rotation,
            Quaternion.fromAxisAngle(Vector3.Up, 90f),
        )
    }

    @Test
    fun reset_duringRotationGesture_ignoresRotationUntilIdle() {
        val entity = createGltfEntity(BoundingBox.fromMinMax(Vector3.Zero, Vector3.Zero))
        val component = createRotatableComponent()
        assertThat(entity.addComponent(component)).isTrue()
        val pitch = Quaternion.fromAxisAngle(Vector3.Right, 90f)
        val yaw = Quaternion.fromAxisAngle(Vector3.Up, 45f)
        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            Quaternion.Identity,
            InputEvent.ACTION_DOWN,
        )
        sendRotationInputEvent(entity, GeometryAffordanceState.ROTATION, pitch)

        component.reset()
        fakeExecutor.runAll()
        // The runtime still reports rotations against its pre-reset baseline until it is idle.
        sendRotationInputEvent(entity, GeometryAffordanceState.ROTATION, yaw * pitch)

        assertSameRotation(entity.getPose(Space.PARENT).rotation, Quaternion.Identity)

        sendRotationInputEvent(entity, GeometryAffordanceState.IDLE, pitch, InputEvent.ACTION_UP)
        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            Quaternion.Identity,
            InputEvent.ACTION_DOWN,
        )
        sendRotationInputEvent(entity, GeometryAffordanceState.ROTATION, yaw)

        assertSameRotation(entity.getPose(Space.PARENT).rotation, yaw)
    }

    @Test
    fun reset_whenDetached_doesNothing() {
        val entity = createGltfEntity(BoundingBox.fromMinMax(Vector3.Zero, Vector3.Zero))
        val component = createRotatableComponent()
        assertThat(entity.addComponent(component)).isTrue()
        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            Quaternion.Identity,
            InputEvent.ACTION_DOWN,
        )
        val yaw = Quaternion.fromAxisAngle(Vector3.Up, 60f)
        sendRotationInputEvent(entity, GeometryAffordanceState.ROTATION, yaw)
        entity.removeComponent(component)

        component.reset()
        fakeExecutor.runAll()

        assertSameRotation(entity.getPose(Space.PARENT).rotation, yaw)
    }

    @Test
    fun rotationGesture_withoutRotationState_doesNotMoveEntity() {
        val entity = createGltfEntity(BoundingBox.fromMinMax(Vector3.Zero, Vector3.Zero))
        assertThat(entity.addComponent(createRotatableComponent())).isTrue()
        val yaw = Quaternion.fromAxisAngle(Vector3.Up, 90f)

        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.TRANSLATION,
            Quaternion.Identity,
            InputEvent.ACTION_DOWN,
        )
        sendRotationInputEvent(entity, GeometryAffordanceState.TRANSLATION, yaw)

        assertSameRotation(entity.getPose(Space.PARENT).rotation, Quaternion.Identity)
    }

    private fun assertDynamicPivotScalingMidRotation(
        entity: AndroidXrEntity,
        boundingBox: BoundingBox,
    ) {
        val startTranslation = Vector3(-1f, -0.6f, -1.2f)
        entity.setPose(Pose(startTranslation, Quaternion.Identity), Space.PARENT)
        entity.setScale(Vector3(1f, 1f, 1f), Space.PARENT)
        assertThat(entity.addComponent(createRotatableComponent())).isTrue()

        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            Quaternion.Identity,
            InputEvent.ACTION_DOWN,
        )
        // Concurrent/interleaved resize doubles the entity's parent-space scale mid-rotation.
        entity.setScale(Vector3(2f, 2f, 2f), Space.PARENT)

        val rotation =
            Quaternion.fromAxisAngle(Vector3.Right, 90f) * Quaternion.fromAxisAngle(Vector3.Up, 90f)
        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            rotation,
            InputEvent.ACTION_MOVE,
        )

        val pose = entity.getPose(Space.PARENT)
        assertSameRotation(pose.rotation, rotation)
        val currentScaledPivot = boundingBox.center * 2f
        assertVectorEquals(
            pose.translation + pose.rotation * currentScaledPivot,
            startTranslation + currentScaledPivot,
        )
        // Verify a stale start-of-gesture (1x) pivot scale was not used.
        val staleTranslation = startTranslation + boundingBox.center - rotation * boundingBox.center
        assertThat((pose.translation - staleTranslation).length).isGreaterThan(1f)
    }

    @Test
    fun rotationGesture_scaleChangesMidGesture_usesCurrentEntityScaleForPivot() {
        // Non-zero bounding-box base offset: center.y - halfExtents.y = 2f - 1f = 1f != 0.
        val boundingBox = BoundingBox.fromMinMax(Vector3(0f, 1f, 0f), Vector3(1f, 3f, 1f))
        assertDynamicPivotScalingMidRotation(createGltfEntity(boundingBox), boundingBox)
    }

    @Test
    fun rotationGesture_onMeshEntityScaleChangesMidGesture_usesCurrentEntityScaleForPivot() {
        val boundingBox = BoundingBox.fromMinMax(Vector3(0f, 1f, 0f), Vector3(1f, 3f, 1f))
        assertDynamicPivotScalingMidRotation(createMeshEntity(boundingBox), boundingBox)
    }

    private fun assertResetAfterReparentingRestoresOrientationInNewParent(
        entity: AndroidXrEntity,
        pivotOffset: Vector3,
    ) {
        val parentARotation = Quaternion.fromAxisAngle(Vector3.Up, 25f)
        val parentBRotation =
            Quaternion.fromAxisAngle(Vector3.Right, 40f) *
                Quaternion.fromAxisAngle(Vector3.Up, -50f)
        val parentA =
            sceneRuntime.createEntity(
                Pose(Vector3(1f, 0f, 0f), parentARotation),
                "parentA",
                sceneRuntime.activitySpace,
            )
        val parentB =
            sceneRuntime.createEntity(
                Pose(Vector3(-2f, 1f, -1f), parentBRotation),
                "parentB",
                sceneRuntime.activitySpace,
            )

        entity.parent = parentA
        val attachRotation = Quaternion.fromAxisAngle(Vector3.Up, 30f)
        entity.setPose(Pose(Vector3(-1f, -0.6f, -1.2f), attachRotation), Space.PARENT)
        val component = createRotatableComponent()
        assertThat(entity.addComponent(component)).isTrue()

        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            Quaternion.Identity,
            InputEvent.ACTION_DOWN,
        )
        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.ROTATION,
            Quaternion.fromAxisAngle(Vector3.Right, 45f) *
                Quaternion.fromAxisAngle(Vector3.Up, 70f),
        )
        sendRotationInputEvent(
            entity,
            GeometryAffordanceState.IDLE,
            Quaternion.Identity,
            InputEvent.ACTION_UP,
        )

        // Reparent the entity from parentA to parentB with a new local pose.
        entity.parent = parentB
        val poseInParentB =
            Pose(
                Vector3(0.4f, -0.2f, -1.5f),
                Quaternion.fromAxisAngle(Vector3.Forward, 35f),
            )
        entity.setPose(poseInParentB, Space.PARENT)
        val pivotBeforeReset = poseInParentB.translation + poseInParentB.rotation * pivotOffset

        component.reset()
        fakeExecutor.runAll()

        val expectedRotationInParentB =
            parentA.transformPoseTo(Pose(Vector3.Zero, attachRotation), parentB).rotation
        val resetPose = entity.getPose(Space.PARENT)
        assertSameRotation(resetPose.rotation, expectedRotationInParentB)
        assertThat(abs(Quaternion.dot(resetPose.rotation, attachRotation))).isLessThan(0.99f)
        assertSameRotation(
            entity.getPose(Space.ACTIVITY).rotation,
            parentARotation * attachRotation,
        )
        assertVectorEquals(
            resetPose.translation + resetPose.rotation * pivotOffset,
            pivotBeforeReset,
        )
    }

    @Test
    fun reset_afterReparenting_restoresOrientationInNewParentSpace() {
        val boundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3(1f, 2f, 1f))
        assertResetAfterReparentingRestoresOrientationInNewParent(
            createGltfEntity(boundingBox),
            boundingBox.center,
        )
    }

    @Test
    fun reset_onMeshEntityAfterReparenting_restoresOrientationInNewParentSpace() {
        val boundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3(1f, 2f, 1f))
        assertResetAfterReparentingRestoresOrientationInNewParent(
            createMeshEntity(boundingBox),
            boundingBox.center,
        )
    }
}
