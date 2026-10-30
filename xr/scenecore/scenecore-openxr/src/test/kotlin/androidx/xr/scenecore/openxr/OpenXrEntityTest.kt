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
import androidx.xr.scenecore.openxr.testing.commitSceneChanges
import androidx.xr.scenecore.runtime.Component
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.impl.PerceptionSpaceScenePoseImpl
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import androidx.xr.scenecore.testing.MemoryUtils
import com.google.common.truth.Truth.assertThat
import java.lang.ref.WeakReference
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
    private lateinit var executor: FakeScheduledExecutorService
    private lateinit var sceneCommitter: OpenXrSceneCommitter
    private lateinit var activitySpace: OpenXrActivitySpace
    private lateinit var entity: TestOpenXrEntity

    private val testPose =
        Pose(Vector3(1f, 2f, 3f), Quaternion.fromEulerAngles(Vector3(10f, 20f, 30f)))

    private class TestOpenXrEntity(
        context: Context?,
        entityHandle: Long,
        nativeWrapper: SceneCoreOpenXrNative,
        sceneCommitter: OpenXrSceneCommitter,
        sceneNodeRegistry: OpenXrSceneNodeRegistry,
        executor: ScheduledExecutorService,
    ) :
        OpenXrEntity(
            context,
            entityHandle,
            nativeWrapper,
            sceneCommitter,
            sceneNodeRegistry,
            executor,
        )

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
        executor = FakeScheduledExecutorService()
        sceneCommitter = OpenXrSceneCommitter(fakeNative, executor)

        activitySpace =
            OpenXrActivitySpace(
                activity,
                fakeNative.fakeRootEntityHandle,
                fakeNative,
                sceneCommitter,
                nodeRegistry,
                executor,
            )
        nodeRegistry.addSystemSpaceScenePose(activitySpace)

        val handle = fakeNative.createSceneEntity()
        entity =
            TestOpenXrEntity(activity, handle, fakeNative, sceneCommitter, nodeRegistry, executor)
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
        assertPose(entity.transformPoseTo(Pose.Identity, perceptionSpaceScenePose), testPose)
    }

    @Test
    fun getPose_worldSpace_updatesWhenActivitySpacePlatformReferenceSpacePoseChanges() {
        val perceptionSpaceScenePose = PerceptionSpaceScenePoseImpl(activitySpace)
        nodeRegistry.addSystemSpaceScenePose(perceptionSpaceScenePose)

        entity.setPose(testPose, Space.PARENT)
        val newPlatformPose =
            Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(Vector3(0f, 45f, 0f)))
        activitySpace.setPlatformReferenceSpacePose(newPlatformPose)

        val expectedWorldPose = newPlatformPose.compose(testPose)
        assertPose(entity.getPose(Space.REAL_WORLD), expectedWorldPose)
        assertPose(
            entity.transformPoseTo(Pose.Identity, perceptionSpaceScenePose),
            expectedWorldPose,
        )
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
    fun setPose_updatesRightAwayAndCommitsShortlyAfter() {
        entity.setPose(testPose, Space.PARENT)

        assertPose(entity.getPose(Space.PARENT), testPose)
        assertThat(fakeNative.entityTransforms).doesNotContainKey(entity.entityHandle)
        executor.commitSceneChanges()
        assertPose(fakeNative.entityTransforms[entity.entityHandle]!!.first, testPose)
    }

    @Test
    fun setPose_whenNoTransactionIsFree_commitsOnceOneIs() {
        fakeNative.simulateTransactionUnavailable = true
        entity.setPose(testPose, Space.PARENT)
        executor.commitSceneChanges()
        assertThat(fakeNative.entityTransforms).doesNotContainKey(entity.entityHandle)

        fakeNative.simulateTransactionUnavailable = false
        executor.commitSceneChanges()

        assertPose(fakeNative.entityTransforms[entity.entityHandle]!!.first, testPose)
    }

    @Test
    fun setPose_waitingToCommit_doesNotKeepTheEntityReachable() {
        fun setPoseOfNewEntity(): WeakReference<TestOpenXrEntity> {
            val newEntity =
                TestOpenXrEntity(
                    activity,
                    fakeNative.createSceneEntity(),
                    fakeNative,
                    sceneCommitter,
                    nodeRegistry,
                    executor,
                )
            newEntity.setPose(testPose, Space.PARENT)
            return WeakReference(newEntity)
        }

        val entityRef = setPoseOfNewEntity()

        MemoryUtils.assertGarbageCollected(entityRef)
    }

    @Test
    fun setPoseAndScale_commitInOneTransaction() {
        val testScale = Vector3(2f, 3f, 4f)
        executor.commitSceneChanges()
        val committedBefore = fakeNative.submittedTransactions.size

        entity.setPose(testPose, Space.PARENT)
        entity.setScale(testScale, Space.PARENT)
        executor.commitSceneChanges()

        val transaction = fakeNative.submittedTransactions.drop(committedBefore).single()
        assertPose(transaction.transforms[entity.entityHandle]!!.first, testPose)
        assertVector3(transaction.transforms[entity.entityHandle]!!.second, testScale)
    }

    @Test
    fun setScale_setsScaleAndCommitsIt() {
        val testScale = Vector3(2f, 3f, 4f)
        entity.setScale(testScale)

        assertVector3(entity.getScale(Space.PARENT), testScale)
        executor.commitSceneChanges()
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
        val child =
            TestOpenXrEntity(
                activity,
                childHandle,
                fakeNative,
                sceneCommitter,
                nodeRegistry,
                executor,
            )
        child.parent = entity

        val targetActivityScale = parentScale.scale(parentScale)
        child.setScale(targetActivityScale, Space.ACTIVITY)

        assertVector3(child.getScale(Space.ACTIVITY), targetActivityScale)
        assertVector3(child.getScale(Space.PARENT), parentScale)
        executor.commitSceneChanges()
        assertVector3(fakeNative.entityTransforms[childHandle]!!.second, parentScale)
    }

    @Test
    fun setScale_worldSpace_setsWorldSpaceScale() {
        val scale = Vector3(1.0f, 2.0f, 3.0f)
        entity.setScale(scale, Space.PARENT)
        val childHandle = fakeNative.createSceneEntity()
        val child =
            TestOpenXrEntity(
                activity,
                childHandle,
                fakeNative,
                sceneCommitter,
                nodeRegistry,
                executor,
            )
        child.parent = entity
        child.setScale(scale.scale(scale), Space.REAL_WORLD)

        assertVector3(child.getScale(Space.REAL_WORLD), scale.scale(scale))
    }

    @Test
    fun setParent_setsParentAndUpdatesNative() {
        val childHandle = fakeNative.createSceneEntity()
        val parentHandle = fakeNative.createSceneEntity()
        val childEntity =
            TestOpenXrEntity(
                activity,
                childHandle,
                fakeNative,
                sceneCommitter,
                nodeRegistry,
                executor,
            )
        val parentEntity =
            TestOpenXrEntity(
                activity,
                parentHandle,
                fakeNative,
                sceneCommitter,
                nodeRegistry,
                executor,
            )

        childEntity.parent = parentEntity
        executor.commitSceneChanges()

        assertThat(childEntity.parent).isEqualTo(parentEntity)
        assertThat(fakeNative.entityParents[childHandle]).isEqualTo(parentHandle)
        assertThat(parentEntity.children).containsExactly(childEntity)
    }

    @Test
    fun setParent_nullOnAnEntityWithoutAParent_doesNotUnparentTheNativeEntity() {
        val handle = fakeNative.createSceneEntity()
        val orphan =
            TestOpenXrEntity(activity, handle, fakeNative, sceneCommitter, nodeRegistry, executor)

        orphan.parent = null
        executor.commitSceneChanges()

        // The OpenXR runtime would show an unparented entity under the activity window.
        assertThat(fakeNative.submittedTransactions.flatMap { it.parents.keys })
            .doesNotContain(handle)
    }

    @Test
    fun setParent_nullOnAnEntityWithAParent_unparentsTheNativeEntity() {
        executor.commitSceneChanges()

        entity.parent = null
        executor.commitSceneChanges()

        assertThat(fakeNative.submittedTransactions.last().parents)
            .containsExactly(entity.entityHandle, INVALID_HANDLE)
        assertThat(fakeNative.entityParents).doesNotContainKey(entity.entityHandle)
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
            TestOpenXrEntity(
                activity,
                parentHandle,
                fakeNative,
                sceneCommitter,
                nodeRegistry,
                executor,
            )
        entity.parent = parentEntity

        val handle = entity.entityHandle
        entity.dispose()
        executor.commitSceneChanges()

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
        executor.commitSceneChanges()

        assertThat(entity.entityHandle).isEqualTo(INVALID_HANDLE)
        assertThat(nodeRegistry.getEntityForNode(handle)).isNull()
        assertThat(fakeNative.destroyedEntities).contains(handle)
    }

    @Test
    fun dispose_detachesChildEntitiesAndCleansRegistry() {
        val childHandle = fakeNative.createSceneEntity()
        val child =
            TestOpenXrEntity(
                activity,
                childHandle,
                fakeNative,
                sceneCommitter,
                nodeRegistry,
                executor,
            )
        entity.addChild(child)

        assertThat(nodeRegistry.getEntityForNode(childHandle)).isEqualTo(child)

        val parentHandle = entity.entityHandle
        entity.dispose()
        executor.commitSceneChanges()

        assertThat(child.parent).isNull()
        assertThat(child.entityHandle).isEqualTo(childHandle)
        assertThat(nodeRegistry.getEntityForNode(childHandle)).isEqualTo(child)
        assertThat(nodeRegistry.getEntityForNode(parentHandle)).isNull()
        assertThat(fakeNative.destroyedEntities).contains(parentHandle)
        assertThat(fakeNative.destroyedEntities).doesNotContain(childHandle)
    }

    @Test
    fun dispose_unparentsTheChildrenBeforeTheNativeEntityIsDestroyed() {
        // Commits one entity at a time, so that the changes made before the destruction take more
        // than one commit.
        val sceneCommitter =
            OpenXrSceneCommitter(fakeNative, executor, maxEntitiesPerTransaction = 1)
        val parent =
            TestOpenXrEntity(
                activity,
                fakeNative.createSceneEntity(),
                fakeNative,
                sceneCommitter,
                nodeRegistry,
                executor,
            )
        val child =
            TestOpenXrEntity(
                activity,
                fakeNative.createSceneEntity(),
                fakeNative,
                sceneCommitter,
                nodeRegistry,
                executor,
            )
        val otherEntity =
            TestOpenXrEntity(
                activity,
                fakeNative.createSceneEntity(),
                fakeNative,
                sceneCommitter,
                nodeRegistry,
                executor,
            )
        child.parent = parent
        executor.commitSceneChanges()
        val parentHandle = parent.entityHandle
        // Waits to commit before the changes that dispose() makes.
        otherEntity.setPose(testPose, Space.PARENT)

        parent.dispose()
        executor.commitSceneChanges()

        // The runtime can remove an entity's children with it, so they're unparented first.
        assertThat(fakeNative.childrenWhenDestroyed[parentHandle]).isEmpty()
        assertThat(fakeNative.destroyedEntities).containsExactly(parentHandle)
        assertThat(fakeNative.entityParents).doesNotContainKey(child.entityHandle)
    }

    @Test
    fun dispose_ofAnEntityWaitingToCommit_unparentsTheChildrenFirst() {
        val child =
            TestOpenXrEntity(
                activity,
                fakeNative.createSceneEntity(),
                fakeNative,
                sceneCommitter,
                nodeRegistry,
                executor,
            )
        child.parent = entity
        executor.commitSceneChanges()
        val handle = entity.entityHandle
        // Still waits to commit when dispose() runs.
        entity.setPose(testPose, Space.PARENT)

        entity.dispose()
        executor.commitSceneChanges()

        assertThat(fakeNative.childrenWhenDestroyed[handle]).isEmpty()
        assertThat(fakeNative.destroyedEntities).contains(handle)
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
                sceneCommitter,
                nodeRegistry,
                executor,
            )
        val child2 =
            TestOpenXrEntity(
                activity,
                fakeNative.createSceneEntity(),
                fakeNative,
                sceneCommitter,
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
                sceneCommitter,
                nodeRegistry,
                executor,
            )
        entity.addChildren(listOf(child2, child3))
        assertThat(entity.children).containsExactly(child1, child2, child3)
    }
}
