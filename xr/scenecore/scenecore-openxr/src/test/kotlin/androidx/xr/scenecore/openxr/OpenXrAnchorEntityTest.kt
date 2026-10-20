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
import androidx.xr.arcore.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.scenecore.openxr.OpenXrAnchorEntity.AnchorSample
import androidx.xr.scenecore.openxr.testing.FakeSceneCoreOpenXrNative
import androidx.xr.scenecore.openxr.testing.FakeSceneCoreOpenXrNative.Attachment
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.impl.PerceptionSpaceScenePoseImpl
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val OTHER_SPACE_HANDLE = 4001L

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class OpenXrAnchorEntityTest {

    private val activity: Activity = mock()
    private val fakeNative = FakeSceneCoreOpenXrNative()
    private val nodeRegistry = OpenXrSceneNodeRegistry()
    private val executor = FakeScheduledExecutorService()
    private val directExecutor = Executor { it.run() }
    // A new anchor reports PAUSED at the identity pose until it's first tracked.
    private val samples = MutableStateFlow(AnchorSample(TrackingState.PAUSED, Pose()))
    private val stateChanges = mutableListOf<Int>()
    private val anchorPose =
        Pose(Vector3(1f, 2f, 3f), Quaternion.fromEulerAngles(Vector3(10f, 20f, 30f)))
    private val rootPose =
        Pose(Vector3(-1f, 0.5f, 2f), Quaternion.fromEulerAngles(Vector3(0f, 45f, 0f)))

    private lateinit var activitySpace: OpenXrActivitySpace
    private lateinit var anchorEntity: OpenXrAnchorEntity

    @Before
    fun setUp() {
        fakeNative.init(100L, 200L, 300L)
        fakeNative.createSpatialContainer()
        activitySpace = createActivitySpace(fakeNative.fakeRootEntityHandle)
        nodeRegistry.addSystemSpaceScenePose(activitySpace)
        nodeRegistry.addSystemSpaceScenePose(PerceptionSpaceScenePoseImpl(activitySpace))
        anchorEntity = createAnchorEntity()
    }

    private fun createActivitySpace(rootHandle: Long) =
        OpenXrActivitySpace(activity, rootHandle, fakeNative, nodeRegistry, executor)

    private fun createAnchorEntity(
        entityHandle: Long = fakeNative.createSceneEntity(),
        activitySpace: OpenXrActivitySpace = this.activitySpace,
    ) =
        OpenXrAnchorEntity(
            activity,
            entityHandle,
            fakeNative,
            nodeRegistry,
            executor,
            activitySpace,
        )

    private fun createChildEntity(): OpenXrEntity =
        object :
            OpenXrEntity(
                activity,
                fakeNative.createSceneEntity(),
                fakeNative,
                nodeRegistry,
                executor,
            ) {}

    /** Records later state changes, without the current state that registering posts. */
    private fun recordStateChanges() {
        anchorEntity.setOnStateChangedListener { stateChanges.add(it) }
        executor.runAll()
        stateChanges.clear()
    }

    /**
     * Attaches to [FakeSceneCoreOpenXrNative.fakeSpaceHandle], then runs the collector, which reads
     * the first sample.
     */
    private fun attachAndCollect(): Boolean {
        val isAttached = anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)
        executor.runAll()
        return isAttached
    }

    private fun emit(trackingState: TrackingState, pose: Pose = anchorPose) {
        samples.value = AnchorSample(trackingState, pose)
        executor.runAll()
    }

    @Test
    fun state_beforeAttach_isUnanchored() {
        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.UNANCHORED)
    }

    @Test
    fun setOnStateChangedListener_postsCurrentStateOnExecutor() {
        anchorEntity.setOnStateChangedListener { stateChanges.add(it) }

        assertThat(stateChanges).isEmpty()
        executor.runAll()
        assertThat(stateChanges).containsExactly(AnchorEntity.State.UNANCHORED)
    }

    @Test
    fun setOnStateChangedListener_null_stopsNotifications() {
        recordStateChanges()

        anchorEntity.setOnStateChangedListener(null)
        anchorEntity.attach(INVALID_HANDLE, samples)
        executor.runAll()

        assertThat(stateChanges).isEmpty()
    }

    @Test
    fun setOnStateChangedListener_null_dropsReportsNotYetDelivered() {
        recordStateChanges()
        anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)

        anchorEntity.setOnStateChangedListener(null)
        executor.runAll()

        assertThat(stateChanges).isEmpty()
    }

    @Test
    fun setOnStateChangedListener_replacedBeforeReport_reportsOnlyToNewListener() {
        val firstListenerStates = mutableListOf<Int>()
        anchorEntity.setOnStateChangedListener { firstListenerStates.add(it) }
        anchorEntity.setOnStateChangedListener { stateChanges.add(it) }

        executor.runAll()

        assertThat(firstListenerStates).isEmpty()
        assertThat(stateChanges).containsExactly(AnchorEntity.State.UNANCHORED)
    }

    @Test
    fun setOnStateChangedListener_stateChangesBeforeReport_reportsOnlyNewState() {
        anchorEntity.setOnStateChangedListener { stateChanges.add(it) }
        anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)

        executor.runAll()

        assertThat(stateChanges).containsExactly(AnchorEntity.State.ANCHORED)
    }

    @Test
    fun setOnStateChangedListener_attachFailsBeforeReport_reportsOnlyError() {
        // Like AnchorSpace.create(session, anchor), which sets ERROR itself once setAnchor()
        // returns false. A late report of the earlier UNANCHORED state would undo that.
        anchorEntity.setOnStateChangedListener { stateChanges.add(it) }
        assertThat(anchorEntity.attach(null, samples)).isFalse()

        executor.runAll()

        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
    }

    @Test
    fun stateListener_isNotCalledWhileHoldingTheEntityLock() {
        var isStateReadableFromOtherThread: Boolean? = null
        anchorEntity.setOnStateChangedListener { newState ->
            if (newState != AnchorEntity.State.ANCHORED) return@setOnStateChangedListener
            // If the listener ran while holding the entity's lock, this read would block.
            val reader = thread { anchorEntity.state }
            reader.join(TimeUnit.SECONDS.toMillis(5))
            isStateReadableFromOtherThread = !reader.isAlive
        }
        executor.runAll()

        anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)
        executor.runAll()

        assertThat(isStateReadableFromOtherThread).isTrue()
    }

    @Test
    fun stateChanges_areReportedInOrder() {
        recordStateChanges()

        attachAndCollect()
        emit(TrackingState.TRACKING)
        emit(TrackingState.PAUSED)
        anchorEntity.dispose()
        executor.runAll()

        assertThat(stateChanges)
            .containsExactly(
                AnchorEntity.State.ANCHORED,
                AnchorEntity.State.UNANCHORED,
                AnchorEntity.State.ERROR,
            )
            .inOrder()
    }

    @Test
    fun stateChanges_supersededBeforeReport_reportOnlyLatestState() {
        recordStateChanges()

        anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)
        anchorEntity.dispose()
        executor.runAll()

        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
    }

    @Test
    fun attach_parentsToRootAndAttachesToSpaceInOneTransaction() {
        recordStateChanges()

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isTrue()
        executor.runAll()

        val handle = anchorEntity.entityHandle
        assertThat(fakeNative.committedTransactions).hasSize(1)
        assertThat(fakeNative.entityParents[handle]).isEqualTo(fakeNative.fakeRootEntityHandle)
        assertThat(fakeNative.entityAttachments[handle])
            .isEqualTo(Attachment(fakeNative.fakeSpaceHandle, ATTACHMENT_LOSS_BEHAVIOUR_LAST_KNOWN))
        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ANCHORED)
        assertThat(anchorEntity.parent).isNull()
    }

    @Test
    fun attach_withoutSpaceHandle_movesToErrorAndReturnsFalse() {
        val otherEntity = createAnchorEntity()
        recordStateChanges()

        assertThat(anchorEntity.attach(null, samples)).isFalse()
        assertThat(otherEntity.attach(INVALID_HANDLE, samples)).isFalse()
        executor.runAll()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
        assertThat(otherEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(fakeNative.committedTransactions).isEmpty()
        assertThat(fakeNative.entityAttachments).isEmpty()
    }

    @Test
    fun attach_withoutNativeEntity_movesToErrorAndReturnsFalse() {
        anchorEntity = createAnchorEntity(entityHandle = INVALID_HANDLE)

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isFalse()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(fakeNative.committedTransactions).isEmpty()
    }

    @Test
    fun attach_rootNotBound_movesToErrorAndReturnsFalse() {
        anchorEntity = createAnchorEntity(activitySpace = createActivitySpace(INVALID_HANDLE))

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isFalse()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(fakeNative.committedTransactions).isEmpty()
    }

    @Test
    fun attach_noTransactionAvailable_movesToErrorAndReturnsFalse() {
        fakeNative.simulateTransactionUnavailable = true

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isFalse()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(fakeNative.entityAttachments).isEmpty()
    }

    @Test
    fun attach_attachmentRejected_cancelsWholeTransactionAndMovesToError() {
        fakeNative.simulateAttachmentFailure = true

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isFalse()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(fakeNative.cancelledTransactions).hasSize(1)
        assertThat(fakeNative.committedTransactions).isEmpty()
        assertThat(fakeNative.entityParents).doesNotContainKey(anchorEntity.entityHandle)
        assertThat(fakeNative.entityAttachments).isEmpty()
    }

    @Test
    fun attach_withoutAttachmentNatives_cancelsTransactionAndMovesToError() {
        fakeNative.simulateMissingAttachmentNatives = true

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isFalse()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(fakeNative.cancelledTransactions).hasSize(1)
        assertThat(fakeNative.committedTransactions).isEmpty()
        assertThat(fakeNative.entityParents).doesNotContainKey(anchorEntity.entityHandle)
    }

    @Test
    fun attach_afterError_returnsFalseWithoutNativeChanges() {
        anchorEntity.attach(INVALID_HANDLE, samples)

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isFalse()

        assertThat(fakeNative.committedTransactions).isEmpty()
        assertThat(fakeNative.entityAttachments).isEmpty()
    }

    @Test
    fun attach_again_replacesAttachmentAndIgnoresEarlierAnchor() {
        attachAndCollect()
        emit(TrackingState.TRACKING)
        val otherSamples = MutableStateFlow(AnchorSample(TrackingState.PAUSED, Pose()))

        assertThat(anchorEntity.attach(OTHER_SPACE_HANDLE, otherSamples)).isTrue()
        executor.runAll()
        emit(TrackingState.TRACKING, Pose(Vector3(7f, 8f, 9f)))

        assertThat(fakeNative.entityAttachments[anchorEntity.entityHandle])
            .isEqualTo(Attachment(OTHER_SPACE_HANDLE, ATTACHMENT_LOSS_BEHAVIOUR_LAST_KNOWN))
        // The earlier anchor's pose no longer applies, and its later samples are ignored.
        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(Pose.Identity)
        assertThat(anchorEntity.activitySpacePose).isEqualTo(Pose())
        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
    }

    @Test
    fun attach_again_failing_removesEarlierAttachmentAndMovesToError() {
        attachAndCollect()
        emit(TrackingState.TRACKING)
        recordStateChanges()
        val handle = anchorEntity.entityHandle

        // For example, the new anchor has no XrSpace.
        assertThat(anchorEntity.attach(null, samples)).isFalse()
        executor.runAll()
        emit(TrackingState.TRACKING, Pose(Vector3(7f, 8f, 9f)))

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
        assertThat(fakeNative.entityAttachments).doesNotContainKey(handle)
        assertThat(fakeNative.entityParents).doesNotContainKey(handle)
        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(anchorPose)
    }

    @Test
    fun attach_afterDispose_returnsFalse() {
        anchorEntity.dispose()

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isFalse()
    }

    @Test
    fun trackingSample_updatesPoseAndNotifiesOriginListener() {
        var originChanges = 0
        anchorEntity.setOnOriginChangedListener({ originChanges++ }, directExecutor)
        attachAndCollect()

        emit(TrackingState.TRACKING)

        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(anchorPose)
        assertThat(originChanges).isEqualTo(1)
        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
    }

    @Test
    fun pausedSample_beforeFirstTracking_staysAnchoredWithIdentityPose() {
        var originChanges = 0
        anchorEntity.setOnOriginChangedListener({ originChanges++ }, directExecutor)
        recordStateChanges()

        attachAndCollect()
        emit(TrackingState.PAUSED)

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ANCHORED)
        assertThat(anchorEntity.activitySpacePose).isEqualTo(Pose())
        assertThat(originChanges).isEqualTo(0)
    }

    @Test
    fun pausedSample_afterTracking_movesToUnanchoredAndKeepsLastPose() {
        attachAndCollect()
        emit(TrackingState.TRACKING)
        recordStateChanges()

        emit(TrackingState.PAUSED, Pose(Vector3(7f, 8f, 9f)))

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.UNANCHORED)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.UNANCHORED)
        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(anchorPose)
        assertPose(anchorEntity.activitySpacePose, anchorPose)
    }

    @Test
    fun trackingSample_afterPaused_movesBackToAnchored() {
        attachAndCollect()
        emit(TrackingState.TRACKING)
        emit(TrackingState.PAUSED)
        recordStateChanges()

        emit(TrackingState.TRACKING)

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ANCHORED)
    }

    @Test
    fun stoppedSample_removesAttachmentAndUnparentsBeforeReportingError() {
        attachAndCollect()
        emit(TrackingState.TRACKING)
        val handle = anchorEntity.entityHandle
        var isAttachedWhenErrorReported: Boolean? = null
        anchorEntity.setOnStateChangedListener { newState ->
            if (newState == AnchorEntity.State.ERROR) {
                isAttachedWhenErrorReported = fakeNative.entityAttachments.containsKey(handle)
            }
        }
        executor.runAll()

        emit(TrackingState.STOPPED)

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(isAttachedWhenErrorReported).isFalse()
        assertThat(fakeNative.entityAttachments).doesNotContainKey(handle)
        assertThat(fakeNative.entityParents).doesNotContainKey(handle)
        assertThat(anchorEntity.activitySpacePose).isEqualTo(Pose())
    }

    @Test
    fun samples_afterStopped_areIgnored() {
        attachAndCollect()
        emit(TrackingState.TRACKING)
        emit(TrackingState.STOPPED)

        emit(TrackingState.TRACKING, Pose(Vector3(7f, 8f, 9f)))

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(anchorPose)
    }

    @Test
    fun stoppedSample_noTransactionAvailable_movesToErrorAndLeavesAttachmentUntilDispose() {
        attachAndCollect()
        val handle = anchorEntity.entityHandle
        fakeNative.simulateTransactionUnavailable = true

        emit(TrackingState.STOPPED)

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(fakeNative.entityAttachments).containsKey(handle)

        anchorEntity.dispose()

        assertThat(fakeNative.entityAttachments).doesNotContainKey(handle)
    }

    @Test
    fun activitySpacePose_isAnchorPoseRelativeToActivitySpace() {
        activitySpace.setPlatformReferenceSpacePose(rootPose)
        attachAndCollect()

        emit(TrackingState.TRACKING)

        val expected = rootPose.inverse.compose(anchorPose)
        assertPose(anchorEntity.activitySpacePose, expected)
        assertPose(anchorEntity.getPose(Space.ACTIVITY), expected)
    }

    @Test
    fun getPose_realWorld_returnsAnchorPose() {
        activitySpace.setPlatformReferenceSpacePose(rootPose)
        attachAndCollect()

        emit(TrackingState.TRACKING)

        assertPose(anchorEntity.getPose(Space.REAL_WORLD), anchorPose)
    }

    @Test
    fun scale_inActivityAndRealWorldSpace_isOne() {
        assertThat(anchorEntity.activitySpaceScale).isEqualTo(Vector3.One)
        assertThat(anchorEntity.getScale(Space.ACTIVITY)).isEqualTo(Vector3.One)
        assertThat(anchorEntity.getScale(Space.REAL_WORLD)).isEqualTo(Vector3.One)
    }

    @Test
    fun getPoseAndScale_inParentSpace_throwUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.getPose(Space.PARENT)
        }
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.getScale(Space.PARENT)
        }
    }

    @Test
    fun getPoseAndScale_invalidSpace_throwIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) { anchorEntity.getPose(999) }
        assertThrows(IllegalArgumentException::class.java) { anchorEntity.getScale(999) }
    }

    @Test
    fun setPoseAndScale_throwUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setPose(Pose(), Space.ACTIVITY)
        }
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setScale(Vector3.One, Space.ACTIVITY)
        }
    }

    @Test
    fun setParent_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.parent = activitySpace
        }
        assertThat(anchorEntity.parent).isNull()
    }

    @Test
    fun child_isParentedToAnchorEntityInNativeSceneGraph() {
        attachAndCollect()
        val child = createChildEntity()

        child.parent = anchorEntity

        assertThat(fakeNative.entityParents[child.entityHandle])
            .isEqualTo(anchorEntity.entityHandle)
        assertThat(fakeNative.entityParents[anchorEntity.entityHandle])
            .isEqualTo(fakeNative.fakeRootEntityHandle)
        assertThat(anchorEntity.children).containsExactly(child)
    }

    @Test
    fun child_activitySpacePose_followsAnchor() {
        val child = createChildEntity()
        child.parent = anchorEntity
        val localPose = Pose(Vector3(0f, 1f, 0f))
        child.setPose(localPose, Space.PARENT)
        attachAndCollect()

        emit(TrackingState.TRACKING)

        assertPose(child.getPose(Space.ACTIVITY), anchorPose.compose(localPose))
    }

    @Test
    fun dispose_removesAttachmentAndDestroysEntityBeforeReportingError() {
        attachAndCollect()
        val handle = anchorEntity.entityHandle
        var isAttachedWhenErrorReported: Boolean? = null
        anchorEntity.setOnStateChangedListener { newState ->
            if (newState == AnchorEntity.State.ERROR) {
                isAttachedWhenErrorReported = fakeNative.entityAttachments.containsKey(handle)
            }
        }
        executor.runAll()

        anchorEntity.dispose()
        assertThat(isAttachedWhenErrorReported).isNull()
        executor.runAll()

        assertThat(isAttachedWhenErrorReported).isFalse()
        assertThat(fakeNative.destroyedEntities).contains(handle)
        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(anchorEntity.entityHandle).isEqualTo(INVALID_HANDLE)
    }

    @Test
    fun dispose_ignoresLaterSamples() {
        attachAndCollect()

        anchorEntity.dispose()
        emit(TrackingState.TRACKING)

        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(Pose.Identity)
    }

    @Test
    fun dispose_twice_reportsErrorOnce() {
        recordStateChanges()

        anchorEntity.dispose()
        anchorEntity.dispose()
        executor.runAll()

        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
    }

    @Test
    fun dispose_beforeAttach_destroysEntityWithoutTransaction() {
        val handle = anchorEntity.entityHandle

        anchorEntity.dispose()

        assertThat(fakeNative.committedTransactions).isEmpty()
        assertThat(fakeNative.destroyedEntities).contains(handle)
    }

    @Test
    fun dispose_afterNativeDestroyed_movesToErrorWithoutThrowing() {
        attachAndCollect()
        fakeNative.destroy()

        anchorEntity.dispose()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
    }
}
