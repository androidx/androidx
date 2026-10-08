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
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.scenecore.openxr.testing.FakeSceneCoreOpenXrNative
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialVisibility
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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

    private val directExecutor = Executor { it.run() }
    private val mockExecutor: ScheduledExecutorService = mock()
    private val mockSamplerFuture: ScheduledFuture<*> = mock()

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

    @After
    fun tearDown() {
        executor.shutdownNow()
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
    fun mainPanelEntity_isNotNullAndParentedToActivitySpace_beforeInitialize() {
        assertThat(runtime.mainPanelEntity).isNotNull()
        assertThat(runtime.mainPanelEntity.parent).isEqualTo(runtime.activitySpace)
        assertThat((runtime.mainPanelEntity as? OpenXrEntity)?.entityHandle)
            .isEqualTo(INVALID_HANDLE)
    }

    @Test
    fun spatialEnvironment_isNotNull() {
        assertThat(runtime.spatialEnvironment).isNotNull()
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
        fakeNative.simulateInitFailure = true

        val exception = assertThrows(IllegalStateException::class.java) { runtime.initialize() }
        assertThat(exception).hasMessageThat().isEqualTo("SceneCoreOpenXrNative.init failed.")
    }

    @Test
    fun initialize_whenCreateSpatialContainerFails_throwsIllegalStateException() {
        fakeNative.simulateCreateSpatialContainerFailure = true

        val exception = assertThrows(IllegalStateException::class.java) { runtime.initialize() }
        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("SceneCoreOpenXrNative.createSpatialContainer failed.")
    }

    @Test
    fun initialize_succeedsAndSetsRootEntity() {
        runtime.initialize()

        assertThat((runtime.activitySpace as? OpenXrEntity)?.entityHandle)
            .isEqualTo(fakeNative.fakeRootEntityHandle)
        assertThat(nodeRegistry.getEntityForNode(fakeNative.fakeRootEntityHandle))
            .isEqualTo(runtime.activitySpace)
        assertThat((runtime.mainPanelEntity as? OpenXrEntity)?.entityHandle)
            .isNotEqualTo(INVALID_HANDLE)
        assertThat(runtime.mainPanelEntity.parent).isEqualTo(runtime.activitySpace)
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
        runtime.initialize()
        runtime.initialize() // Should safely early return without throwing
        assertThat(runtime.isInitialized).isTrue()
    }

    @Test
    fun destroy_beforeInitialize_disposesEntities() {
        runtime.destroy()
        assertThat(runtime.isDestroyed).isTrue()
        assertThat((runtime.activitySpace as? OpenXrEntity)?.entityHandle).isEqualTo(INVALID_HANDLE)
        assertThat((runtime.mainPanelEntity as? OpenXrEntity)?.entityHandle)
            .isEqualTo(INVALID_HANDLE)
    }

    @Test
    fun destroy_afterInitialize_disposesMainPanelEntityAndActivitySpace() {
        runtime.initialize()
        val mainPanelHandle = (runtime.mainPanelEntity as OpenXrEntity).entityHandle
        assertThat(mainPanelHandle).isNotEqualTo(INVALID_HANDLE)

        runtime.destroy()

        assertThat(runtime.isDestroyed).isTrue()
        assertThat(fakeNative.destroyedEntities).contains(mainPanelHandle)
        assertThat((runtime.mainPanelEntity as? OpenXrEntity)?.entityHandle)
            .isEqualTo(INVALID_HANDLE)
        assertThat((runtime.activitySpace as? OpenXrEntity)?.entityHandle).isEqualTo(INVALID_HANDLE)
    }

    @Test
    fun getScenePoseFromPerceptionPose_transformsPoseIntoActivitySpace() {
        runtime.initialize()
        val rootPose = Pose(Vector3(1f, 2f, 3f), Quaternion.fromEulerAngles(Vector3(10f, 20f, 30f)))
        fakeNative.fakeRootSpacePoseInPlatformReferenceSpace = rootPose
        runtime.sampleRootSpacePose()
        val perceptionPose =
            Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(Vector3(0f, 90f, 0f)))

        val scenePose = runtime.getScenePoseFromPerceptionPose(perceptionPose)

        assertPose(scenePose.activitySpacePose, rootPose.inverse.compose(perceptionPose))
    }

    @Test
    fun getScenePoseFromPerceptionPose_whenRootSpacePoseUnavailable_returnsPerceptionPose() {
        runtime.initialize()
        val perceptionPose = Pose(Vector3(4f, 5f, 6f))

        val scenePose = runtime.getScenePoseFromPerceptionPose(perceptionPose)

        assertPose(scenePose.activitySpacePose, perceptionPose)
    }

    @Test
    fun initialize_withoutResume_doesNotScheduleSampling() {
        val runtime = createRuntimeWithMockExecutor()

        runtime.initialize()

        verify(mockExecutor, never()).scheduleWithFixedDelay(any(), any(), any(), any())
    }

    @Test
    fun resume_afterInitialize_schedulesSampling() {
        val runtime = createRuntimeWithMockExecutor()
        runtime.initialize()

        runtime.resume()

        verify(mockExecutor)
            .scheduleWithFixedDelay(
                any(),
                eq(0L),
                eq(ROOT_SPACE_POSE_SAMPLE_PERIOD_NANOS),
                eq(TimeUnit.NANOSECONDS),
            )
    }

    @Test
    fun resume_beforeInitialize_schedulesWhenRootSpaceIsBound() {
        val runtime = createRuntimeWithMockExecutor()

        runtime.resume()
        verify(mockExecutor, never()).scheduleWithFixedDelay(any(), any(), any(), any())

        runtime.initialize()
        verify(mockExecutor).scheduleWithFixedDelay(any(), any(), any(), any())
    }

    @Test
    fun resume_twice_schedulesOnce() {
        val runtime = createRuntimeWithMockExecutor()
        runtime.initialize()

        runtime.resume()
        runtime.resume()

        verify(mockExecutor).scheduleWithFixedDelay(any(), any(), any(), any())
    }

    @Test
    fun pause_cancelsSampling() {
        val runtime = createRuntimeWithMockExecutor()
        runtime.initialize()
        runtime.resume()

        runtime.pause()

        verify(mockSamplerFuture).cancel(false)
    }

    @Test
    fun resume_afterPause_reschedules() {
        val runtime = createRuntimeWithMockExecutor()
        runtime.initialize()
        runtime.resume()
        runtime.pause()

        runtime.resume()

        verify(mockExecutor, times(2)).scheduleWithFixedDelay(any(), any(), any(), any())
    }

    @Test
    fun resume_afterDestroy_doesNotSchedule() {
        val runtime = createRuntimeWithMockExecutor()
        runtime.initialize()
        runtime.destroy()

        runtime.resume()

        verify(mockExecutor, never()).scheduleWithFixedDelay(any(), any(), any(), any())
    }

    @Test
    fun rootSpacePoseSampling_notifiesOriginListenerWithoutAnyReads() {
        val runtime = createRuntimeWithMockExecutor()
        runtime.initialize()
        runtime.resume()
        val sampler = captureRootSpacePoseSampler()
        var originChangeCount = 0
        runtime.activitySpace.setOnOriginChangedListener({ originChangeCount++ }, directExecutor)
        val poseA = Pose(Vector3(1f, 2f, 3f))
        val poseB = Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(Vector3(0f, 90f, 0f)))

        fakeNative.fakeRootSpacePoseInPlatformReferenceSpace = poseA
        sampler.run()
        assertThat(originChangeCount).isEqualTo(1)

        sampler.run()
        assertThat(originChangeCount).isEqualTo(1)

        fakeNative.fakeRootSpacePoseInPlatformReferenceSpace = poseB
        sampler.run()
        assertThat(originChangeCount).isEqualTo(2)
        assertThat(runtime.activitySpace.poseInPlatformReferenceSpace).isEqualTo(poseB)
    }

    @Test
    fun scheduledSample_whenSampleThrows_reportsUncaughtExceptionAndRethrows() {
        val runtime = createRuntimeWithMockExecutor()
        runtime.initialize()
        runtime.resume()
        val sampler = captureRootSpacePoseSampler()
        val error = UnsatisfiedLinkError("test")
        fakeNative.rootSpacePoseQueryError = error

        val reported = recordUncaughtExceptions {
            assertThrows(UnsatisfiedLinkError::class.java) { sampler.run() }
        }

        assertThat(reported).hasSize(1)
        assertThat(reported[0]).isInstanceOf(IllegalStateException::class.java)
        assertThat(reported[0]).hasCauseThat().isSameInstanceAs(error)
    }

    @Test
    fun scheduledSample_whenRejectedDuringTeardown_rethrowsWithoutReporting() {
        val runtime = createRuntimeWithMockExecutor()
        runtime.initialize()
        runtime.resume()
        val sampler = captureRootSpacePoseSampler()
        fakeNative.rootSpacePoseQueryError = RejectedExecutionException()

        val reported = recordUncaughtExceptions {
            assertThrows(RejectedExecutionException::class.java) { sampler.run() }
        }

        assertThat(reported).isEmpty()
    }

    @Test
    fun sampleRootSpacePose_whenNativePoseUnavailable_keepsLastPoseAndDoesNotNotify() {
        val runtime = createRuntimeWithMockExecutor()
        runtime.initialize()
        val lastPose = Pose(Vector3(1f, 2f, 3f))
        fakeNative.fakeRootSpacePoseInPlatformReferenceSpace = lastPose
        runtime.sampleRootSpacePose()
        var originChangeCount = 0
        runtime.activitySpace.setOnOriginChangedListener({ originChangeCount++ }, directExecutor)
        fakeNative.fakeRootSpacePoseInPlatformReferenceSpace = null

        runtime.sampleRootSpacePose()

        assertThat(runtime.activitySpace.poseInPlatformReferenceSpace).isEqualTo(lastPose)
        assertThat(originChangeCount).isEqualTo(0)
    }

    @Test
    fun sampleRootSpacePose_afterDestroy_doesNotThrowOrNotify() {
        val runtime = createRuntimeWithMockExecutor()
        runtime.initialize()
        var originChangeCount = 0
        runtime.activitySpace.setOnOriginChangedListener({ originChangeCount++ }, directExecutor)
        fakeNative.fakeRootSpacePoseInPlatformReferenceSpace = Pose(Vector3(1f, 2f, 3f))
        runtime.destroy()

        runtime.sampleRootSpacePose()

        assertThat(originChangeCount).isEqualTo(0)
        assertThat(runtime.activitySpace.poseInPlatformReferenceSpace).isEqualTo(Pose.Identity)
    }

    @Test
    fun destroy_cancelsRootSpacePoseSampling() {
        val runtime = createRuntimeWithMockExecutor()
        runtime.initialize()
        runtime.resume()

        runtime.destroy()

        verify(mockSamplerFuture).cancel(false)
    }

    @Test
    fun initialize_withoutRootEntity_doesNotScheduleSampling() {
        fakeNative.fakeRootEntityHandle = INVALID_HANDLE
        val runtime = createRuntimeWithMockExecutor()

        runtime.initialize()
        runtime.resume()

        verify(mockExecutor, never()).scheduleWithFixedDelay(any(), any(), any(), any())
    }

    @Test
    fun spatialCapabilitiesChangedListener_addAndRemove_succeeds() {
        val listener = Consumer<SpatialCapabilities> {}
        runtime.addSpatialCapabilitiesChangedListener(executor, listener)
        runtime.removeSpatialCapabilitiesChangedListener(listener)
    }

    @Test
    fun spatialVisibilityChangedListener_setAndClear_succeeds() {
        val listener = Consumer<SpatialVisibility> {}
        runtime.setSpatialVisibilityChangedListener(executor, listener)
        runtime.clearSpatialVisibilityChangedListener()
    }

    /** Creates a runtime whose executor never runs tasks, so tests drive sampling directly. */
    private fun createRuntimeWithMockExecutor(): OpenXrSceneRuntime {
        doReturn(mockSamplerFuture)
            .whenever(mockExecutor)
            .scheduleWithFixedDelay(any(), any(), any(), any())
        return OpenXrSceneRuntime(
            activity = activity,
            unscaledGravityAlignedActivitySpace = true,
            nativeWrapper = fakeNative,
            sceneNodeRegistry = OpenXrSceneNodeRegistry(),
            scheduledExecutorService = mockExecutor,
        )
    }

    /** Returns the root space pose sampling task. Verifies that exactly one was scheduled. */
    private fun captureRootSpacePoseSampler(): Runnable {
        val sampler = argumentCaptor<Runnable>()
        verify(mockExecutor).scheduleWithFixedDelay(sampler.capture(), any(), any(), any())
        return sampler.firstValue
    }

    /**
     * Runs [block] with a handler on this thread that records uncaught exception reports instead of
     * passing them on, and returns the recorded exceptions.
     */
    private fun recordUncaughtExceptions(block: () -> Unit): List<Throwable> {
        val reported = mutableListOf<Throwable>()
        val thread = Thread.currentThread()
        val originalHandler = thread.uncaughtExceptionHandler
        thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e -> reported += e }
        try {
            block()
        } finally {
            thread.uncaughtExceptionHandler = originalHandler
        }
        return reported
    }
}
