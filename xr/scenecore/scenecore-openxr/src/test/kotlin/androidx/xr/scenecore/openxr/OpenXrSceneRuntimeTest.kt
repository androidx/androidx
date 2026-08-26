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
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.openxr.testing.FakeSceneCoreOpenXrNative
import androidx.xr.scenecore.runtime.Space
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class OpenXrSceneRuntimeTest {

    private lateinit var activity: Activity
    private lateinit var fakeNative: FakeSceneCoreOpenXrNative
    private lateinit var nodeRegistry: OpenXrSceneNodeRegistry
    private lateinit var executor: ScheduledExecutorService
    private lateinit var runtime: OpenXrSceneRuntime

    @Before
    fun setUp() {
        activity = mock()
        fakeNative = FakeSceneCoreOpenXrNative()
        nodeRegistry = OpenXrSceneNodeRegistry()
        executor = Executors.newSingleThreadScheduledExecutor()

        runtime =
            OpenXrSceneRuntime(
                activity = activity,
                unscaledGravityAlignedActivitySpace = true,
                nativeWrapper = fakeNative,
                sceneNodeRegistry = nodeRegistry,
                scheduledExecutorService = executor,
            )
    }

    @Test
    fun activitySpace_isNotNullAndInRegistry() {
        assertThat(runtime.activitySpace).isNotNull()
        assertThat(nodeRegistry.getAllSystemSpaceScenePoses()).contains(runtime.activitySpace)
    }

    @Test
    fun perceptionSpaceActivityPose_isNotNullAndInRegistry() {
        assertThat(runtime.perceptionSpaceActivityPose).isNotNull()
        assertThat(nodeRegistry.getAllSystemSpaceScenePoses())
            .contains(runtime.perceptionSpaceActivityPose)
    }

    @Test
    fun createEntity_createsEntityWithParentAndPose() {
        fakeNative.init(100L, 200L, 300L)
        fakeNative.createSpatialContainer()
        val rootHandle = fakeNative.getRootEntityHandle()
        (runtime.activitySpace as? OpenXrEntity)?.entityHandle = rootHandle
        nodeRegistry.setEntityForNode(rootHandle, runtime.activitySpace)

        val initialPose = Pose(Vector3(1f, 2f, 3f))
        val entity = runtime.createEntity(initialPose, "test-entity", runtime.activitySpace)

        assertThat(entity).isNotNull()
        assertThat(entity.parent).isEqualTo(runtime.activitySpace)
        assertThat(entity.getPose(Space.PARENT)).isEqualTo(initialPose)

        val openXrEntity = entity as OpenXrEntity
        assertThat(fakeNative.createdEntities).contains(openXrEntity.entityHandle)
        assertThat(fakeNative.entityParents[openXrEntity.entityHandle])
            .isEqualTo(fakeNative.fakeRootEntityHandle)
        assertThat(nodeRegistry.getEntityForNode(openXrEntity.entityHandle)).isEqualTo(entity)
    }

    @Test
    fun createEntity_withNullParent_createsUnparentedEntity() {
        fakeNative.init(100L, 200L, 300L)
        fakeNative.createSpatialContainer()
        val initialPose = Pose(Vector3(1f, 2f, 3f))
        val entity = runtime.createEntity(initialPose, "unparented-entity", null)

        assertThat(entity).isNotNull()
        assertThat(entity.parent).isNull()
        assertThat(entity.getPose(Space.PARENT)).isEqualTo(initialPose)

        val openXrEntity = entity as OpenXrEntity
        assertThat(fakeNative.createdEntities).contains(openXrEntity.entityHandle)
        assertThat(fakeNative.entityParents[openXrEntity.entityHandle]).isNull()
    }

    @Test
    fun createEntity_afterDestroy_throwsIllegalStateException() {
        runtime.destroy()

        assertThrows(IllegalStateException::class.java) {
            runtime.createEntity(Pose(), "entity", null)
        }
    }

    @Test
    fun initialize_whenInitFails_throwsIllegalStateException() {
        val exception = assertThrows(IllegalStateException::class.java) { runtime.initialize() }
        assertThat(exception).hasMessageThat().isEqualTo("SceneCoreOpenXrNative.init failed.")
    }

    @Test
    fun initialize_whenCreateSpatialContainerFails_throwsIllegalStateException() {
        fakeNative.allowInvalidSessionHandle = true
        fakeNative.simulateCreateSpatialContainerFailure = true

        val exception = assertThrows(IllegalStateException::class.java) { runtime.initialize() }
        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("SceneCoreOpenXrNative.createSpatialContainer failed.")
    }

    @Test
    fun initialize_succeedsAndSetsRootEntity() {
        fakeNative.allowInvalidSessionHandle = true

        runtime.initialize()

        assertThat((runtime.activitySpace as? OpenXrEntity)?.entityHandle)
            .isEqualTo(fakeNative.fakeRootEntityHandle)
        assertThat(nodeRegistry.getEntityForNode(fakeNative.fakeRootEntityHandle))
            .isEqualTo(runtime.activitySpace)
    }

    @Test
    fun destroy_destroysNativeAndMarksAsDestroyed() {
        runtime.destroy()

        assertThat(runtime.isDestroyed).isTrue()
        assertThat(fakeNative.isDestroyed.get()).isTrue()
        val exception = assertThrows(IllegalStateException::class.java) { runtime.initialize() }
        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("Cannot initialize OpenXrSceneRuntime after it has been destroyed.")
    }

    @Test
    fun destroy_disposesCreatedEntitiesAndClearsRegistry() {
        fakeNative.init(100L, 200L, 300L)
        fakeNative.createSpatialContainer()
        val entity1 = runtime.createEntity(Pose(), "entity1", null)
        val entity2 = runtime.createEntity(Pose(), "entity2", null)
        val handle1 = (entity1 as OpenXrEntity).entityHandle
        val handle2 = (entity2 as OpenXrEntity).entityHandle

        assertThat(nodeRegistry.getAllEntities()).containsExactly(entity1, entity2)

        runtime.destroy()

        assertThat(runtime.isDestroyed).isTrue()
        assertThat(fakeNative.destroyedEntities).contains(handle1)
        assertThat(fakeNative.destroyedEntities).contains(handle2)
        assertThat(nodeRegistry.getAllEntities()).isEmpty()
        assertThat((entity1 as OpenXrEntity).entityHandle).isEqualTo(INVALID_HANDLE)
        assertThat((entity2 as OpenXrEntity).entityHandle).isEqualTo(INVALID_HANDLE)
    }

    @Test
    fun initialize_calledMultipleTimes_isIdempotent() {
        fakeNative.allowInvalidSessionHandle = true
        runtime.initialize()
        runtime.initialize() // Should safely early return without throwing
        assertThat(runtime.isInitialized).isTrue()
    }

    @Test
    fun destroy_beforeInitialize_disposesActivitySpace() {
        runtime.destroy()
        assertThat(runtime.isDestroyed).isTrue()
        assertThat((runtime.activitySpace as? OpenXrEntity)?.entityHandle).isEqualTo(INVALID_HANDLE)
    }
}
