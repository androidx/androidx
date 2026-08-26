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

package androidx.xr.scenecore.openxr

import android.app.Activity
import android.content.Context
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.openxr.testing.FakeSceneCoreOpenXrNative
import androidx.xr.scenecore.runtime.Component
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.impl.PerceptionSpaceScenePoseImpl
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class OpenXrEntityTest {

    private lateinit var activity: Activity
    private lateinit var fakeNative: FakeSceneCoreOpenXrNative
    private lateinit var nodeRegistry: OpenXrSceneNodeRegistry
    private lateinit var executor: ScheduledExecutorService
    private lateinit var activitySpace: OpenXrActivitySpace
    private lateinit var entity: TestOpenXrEntity

    private val testPose =
        Pose(Vector3(1f, 2f, 3f), Quaternion.fromEulerAngles(Vector3(10f, 20f, 30f)))

    private class TestOpenXrEntity(
        context: Context?,
        entityHandle: Long,
        nativeWrapper: SceneCoreOpenXrNative,
        sceneNodeRegistry: OpenXrSceneNodeRegistry,
        executor: ScheduledExecutorService,
    ) : OpenXrEntity(context, entityHandle, nativeWrapper, sceneNodeRegistry, executor)

    private class TestComponent : Component {
        var isAttached = false

        override fun onAttach(entity: Entity): Boolean {
            isAttached = true
            return true
        }

        override fun onDetach(entity: Entity) {
            isAttached = false
        }
    }

    private class TestComponent2 : Component {
        var isAttached = false

        override fun onAttach(entity: Entity): Boolean {
            isAttached = true
            return true
        }

        override fun onDetach(entity: Entity) {
            isAttached = false
        }
    }

    private fun assertPose(actual: Pose, expected: Pose) {
        assertThat(actual.translation.x).isWithin(1e-4f).of(expected.translation.x)
        assertThat(actual.translation.y).isWithin(1e-4f).of(expected.translation.y)
        assertThat(actual.translation.z).isWithin(1e-4f).of(expected.translation.z)
        assertThat(actual.rotation.x).isWithin(1e-4f).of(expected.rotation.x)
        assertThat(actual.rotation.y).isWithin(1e-4f).of(expected.rotation.y)
        assertThat(actual.rotation.z).isWithin(1e-4f).of(expected.rotation.z)
        assertThat(actual.rotation.w).isWithin(1e-4f).of(expected.rotation.w)
    }

    private fun assertVector3(actual: Vector3, expected: Vector3) {
        assertThat(actual.x).isWithin(1e-4f).of(expected.x)
        assertThat(actual.y).isWithin(1e-4f).of(expected.y)
        assertThat(actual.z).isWithin(1e-4f).of(expected.z)
    }

    @Before
    fun setUp() {
        activity = mock()
        fakeNative = FakeSceneCoreOpenXrNative()
        fakeNative.init(100L, 200L, 300L)
        fakeNative.createSpatialContainer()
        nodeRegistry = OpenXrSceneNodeRegistry()
        executor = Executors.newSingleThreadScheduledExecutor()

        activitySpace =
            OpenXrActivitySpace(
                activity,
                fakeNative.fakeRootEntityHandle,
                fakeNative,
                nodeRegistry,
                executor,
            )
        nodeRegistry.addSystemSpaceScenePose(activitySpace)

        val handle = fakeNative.createSceneEntity()
        entity = TestOpenXrEntity(activity, handle, fakeNative, nodeRegistry, executor)
        nodeRegistry.setEntityForNode(handle, entity)
        entity.parent = activitySpace
    }

    @Test
    fun entityHandle_returnsHandle() {
        assertThat(entity.entityHandle).isEqualTo(1001L)
    }

    @Test
    fun getPose_parentSpace_returnsParentPose() {
        entity.setPose(testPose, Space.PARENT)
        assertPose(entity.getPose(Space.PARENT), testPose)
    }

    @Test
    fun getPose_activitySpace_returnsActivitySpacePose() {
        entity.setPose(testPose, Space.ACTIVITY)
        assertPose(entity.getPose(Space.ACTIVITY), testPose)
    }

    @Test
    fun getPose_worldSpace_returnsWorldSpacePose() {
        val perceptionSpaceScenePose = PerceptionSpaceScenePoseImpl(activitySpace)
        nodeRegistry.addSystemSpaceScenePose(perceptionSpaceScenePose)

        entity.setPose(testPose, Space.REAL_WORLD)
        assertPose(entity.getPose(Space.REAL_WORLD), testPose)
    }

    @Test
    fun getPose_invalidSpace_throwsException() {
        assertThrows(IllegalArgumentException::class.java) { entity.getPose(999) }
    }

    @Test
    fun getParent_nullParent_returnsNull() {
        entity.parent = null
        assertThat(entity.parent).isNull()
    }

    @Test
    fun getPoseInParentSpace_nullParent_returnsIdentity() {
        entity.parent = null
        entity.setPose(Pose.Identity)
        assertThat(entity.getPose(Space.PARENT)).isEqualTo(Pose.Identity)
    }

    @Test
    fun getPoseInActivitySpace_nullParent_throwsException() {
        entity.parent = null
        assertThrows(IllegalStateException::class.java) { entity.getPose(Space.ACTIVITY) }
    }

    @Test
    fun getPoseInRealWorldSpace_nullParent_throwsException() {
        val perceptionSpaceScenePose = PerceptionSpaceScenePoseImpl(activitySpace)
        nodeRegistry.addSystemSpaceScenePose(perceptionSpaceScenePose)
        entity.parent = null
        assertThrows(IllegalStateException::class.java) { entity.getPose(Space.REAL_WORLD) }
    }

    @Test
    fun setPose_parentSpace_flushesToNative() {
        entity.setPose(testPose, Space.PARENT)
        assertPose(entity.getPose(Space.PARENT), testPose)

        val transform = fakeNative.entityTransforms[entity.entityHandle]
        assertThat(transform).isNotNull()
        assertPose(transform!!.first, testPose)
    }

    @Test
    fun setScale_setsScaleAndFlushesToNative() {
        val testScale = Vector3(2f, 3f, 4f)
        entity.setScale(testScale)

        assertVector3(entity.getScale(Space.PARENT), testScale)
        val transform = fakeNative.entityTransforms[entity.entityHandle]
        assertThat(transform).isNotNull()
        assertVector3(transform!!.second, testScale)
    }

    @Test
    fun getScale_parentSpace_returnsParentScale() {
        val scale = Vector3(1.0f, 2.0f, 3.0f)
        entity.setScale(scale, Space.PARENT)

        assertVector3(entity.getScale(Space.PARENT), scale)
    }

    @Test
    fun getScale_activitySpace_returnsActivitySpaceScale() {
        val scale = Vector3(1.0f, 2.0f, 3.0f)
        entity.setScale(scale, Space.PARENT)

        assertVector3(entity.getScale(Space.PARENT), scale)
        assertVector3(entity.getScale(Space.ACTIVITY), scale)
    }

    @Test
    fun getScale_worldSpace_returnsWorldSpaceScale() {
        val scale = Vector3(1.0f, 2.0f, 3.0f)
        entity.setScale(scale, Space.PARENT)

        assertVector3(entity.getScale(Space.REAL_WORLD), scale)
    }

    @Test
    fun getScale_invalidSpace_throwsException() {
        val scale = Vector3(1.0f, 2.0f, 3.0f)
        entity.setScale(scale, Space.PARENT)

        assertThrows(IllegalArgumentException::class.java) { entity.getScale(999) }
    }

    @Test
    fun setScaleActivitySpace_setsActivitySpaceScale() {
        val parentScale = Vector3(1.0f, 2.0f, 3.0f)
        entity.setScale(parentScale, Space.PARENT)
        val childHandle = fakeNative.createSceneEntity()
        val child = TestOpenXrEntity(activity, childHandle, fakeNative, nodeRegistry, executor)
        child.parent = entity

        val targetActivityScale = parentScale.scale(parentScale)
        child.setScale(targetActivityScale, Space.ACTIVITY)

        assertVector3(child.getScale(Space.ACTIVITY), targetActivityScale)
        assertVector3(child.getScale(Space.PARENT), parentScale)
        assertVector3(fakeNative.entityTransforms[childHandle]!!.second, parentScale)
    }

    @Test
    fun setScale_worldSpace_setsWorldSpaceScale() {
        val scale = Vector3(1.0f, 2.0f, 3.0f)
        entity.setScale(scale, Space.PARENT)
        val childHandle = fakeNative.createSceneEntity()
        val child = TestOpenXrEntity(activity, childHandle, fakeNative, nodeRegistry, executor)
        child.parent = entity
        child.setScale(scale.scale(scale), Space.REAL_WORLD)

        assertVector3(child.getScale(Space.REAL_WORLD), scale.scale(scale))
    }

    @Test
    fun setParent_setsParentAndUpdatesNative() {
        val childHandle = fakeNative.createSceneEntity()
        val parentHandle = fakeNative.createSceneEntity()
        val childEntity =
            TestOpenXrEntity(activity, childHandle, fakeNative, nodeRegistry, executor)
        val parentEntity =
            TestOpenXrEntity(activity, parentHandle, fakeNative, nodeRegistry, executor)

        childEntity.parent = parentEntity

        assertThat(childEntity.parent).isEqualTo(parentEntity)
        assertThat(fakeNative.entityParents[childHandle]).isEqualTo(parentHandle)
        assertThat(parentEntity.children).containsExactly(childEntity)
    }

    @Test
    fun setParent_selfParent_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) { entity.parent = entity }
    }

    @Test
    fun setParent_nonOpenXrEntityParent_throwsIllegalArgumentException() {
        val nonOpenXrEntity = mock<Entity>()

        assertThrows(IllegalArgumentException::class.java) { entity.parent = nonOpenXrEntity }
    }

    @Test
    fun dispose_cleansUpHierarchyAndNativeHandle() {
        val parentHandle = fakeNative.createSceneEntity()
        val parentEntity =
            TestOpenXrEntity(activity, parentHandle, fakeNative, nodeRegistry, executor)
        entity.parent = parentEntity

        val handle = entity.entityHandle
        entity.dispose()

        assertThat(entity.parent).isNull()
        assertThat(nodeRegistry.getEntityForNode(handle)).isNull()
        assertThat(fakeNative.destroyedEntities).contains(handle)
        assertThat(entity.entityHandle).isEqualTo(INVALID_HANDLE)
    }

    @Test
    fun dispose_invalidatesHandleAndRemovesFromRegistry() {
        val handle = entity.entityHandle
        assertThat(nodeRegistry.getEntityForNode(handle)).isEqualTo(entity)

        entity.dispose()

        assertThat(entity.entityHandle).isEqualTo(INVALID_HANDLE)
        assertThat(nodeRegistry.getEntityForNode(handle)).isNull()
        assertThat(fakeNative.destroyedEntities).contains(handle)
    }

    @Test
    fun dispose_detachesChildEntitiesAndCleansRegistry() {
        val childHandle = fakeNative.createSceneEntity()
        val child = TestOpenXrEntity(activity, childHandle, fakeNative, nodeRegistry, executor)
        entity.addChild(child)

        assertThat(nodeRegistry.getEntityForNode(childHandle)).isEqualTo(child)

        val parentHandle = entity.entityHandle
        entity.dispose()

        assertThat(child.parent).isNull()
        assertThat(child.entityHandle).isEqualTo(childHandle)
        assertThat(nodeRegistry.getEntityForNode(childHandle)).isEqualTo(child)
        assertThat(nodeRegistry.getEntityForNode(parentHandle)).isNull()
        assertThat(fakeNative.destroyedEntities).contains(parentHandle)
        assertThat(fakeNative.destroyedEntities).doesNotContain(childHandle)
    }

    @Test
    fun setAlpha_throwsNotImplementedError() {
        assertThrows(NotImplementedError::class.java) { entity.setAlpha(0.5f) }
    }

    @Test
    fun setHidden_throwsNotImplementedError() {
        assertThrows(NotImplementedError::class.java) { entity.setHidden(true) }
    }

    @Test
    fun addInputEventListener_throwsNotImplementedError() {
        assertThrows(NotImplementedError::class.java) { entity.addInputEventListener(executor) {} }
    }

    @Test
    fun removeInputEventListener_throwsNotImplementedError() {
        assertThrows(NotImplementedError::class.java) { entity.removeInputEventListener {} }
    }

    @Test
    fun hitTest_throwsNotImplementedError() = runTest {
        assertFailsWith<NotImplementedError> {
            entity.hitTest(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f), 0)
        }
    }

    @Test
    fun addComponent_addsComponent() {
        val testComponent = TestComponent()
        val added = entity.addComponent(testComponent)

        assertThat(added).isTrue()
        assertThat(entity.getComponents()).containsExactly(testComponent)
    }

    @Test
    fun removeComponent_removesComponent() {
        val testComponent = TestComponent()
        val testComponent2 = TestComponent2()
        entity.addComponent(testComponent)
        entity.addComponent(testComponent2)

        entity.removeComponent(testComponent)

        assertThat(entity.getComponents()).containsExactly(testComponent2)
    }

    @Test
    fun getComponentsOfType_getsOnlyComponentOfType() {
        val testComponent = TestComponent()
        val testComponent2 = TestComponent2()
        entity.addComponent(testComponent)
        entity.addComponent(testComponent2)

        assertThat(entity.getComponentsOfType(TestComponent::class.java))
            .containsExactly(testComponent)
        assertThat(entity.getComponentsOfType(TestComponent2::class.java))
            .containsExactly(testComponent2)
    }

    @Test
    fun addChild_and_addChildren_addsToHierarchy() {
        val child1 =
            TestOpenXrEntity(
                activity,
                fakeNative.createSceneEntity(),
                fakeNative,
                nodeRegistry,
                executor,
            )
        val child2 =
            TestOpenXrEntity(
                activity,
                fakeNative.createSceneEntity(),
                fakeNative,
                nodeRegistry,
                executor,
            )

        entity.addChild(child1)
        assertThat(entity.children).containsExactly(child1)

        val child3 =
            TestOpenXrEntity(
                activity,
                fakeNative.createSceneEntity(),
                fakeNative,
                nodeRegistry,
                executor,
            )
        entity.addChildren(listOf(child2, child3))
        assertThat(entity.children).containsExactly(child1, child2, child3)
    }
}
