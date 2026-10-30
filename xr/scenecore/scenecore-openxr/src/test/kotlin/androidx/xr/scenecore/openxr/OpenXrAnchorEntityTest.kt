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
import androidx.xr.scenecore.openxr.testing.commitSceneChanges
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.impl.PerceptionSpaceScenePoseImpl
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import androidx.xr.scenecore.testing.MemoryUtils
import com.google.common.truth.Truth.assertThat
import java.lang.ref.WeakReference
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
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
    // Runs the entity's tasks and the scene committer's commits, like the runtime's executor.
    private val executor = FakeScheduledExecutorService()
    private val sceneCommitter = OpenXrSceneCommitter(fakeNative, executor)
    private val directExecutor = Executor { it.run() }
    // A new anchor reports PAUSED at the identity pose until it's first tracked.
    private val samples = MutableStateFlow(AnchorSample(TrackingState.PAUSED, Pose()))
    private val stateChanges = mutableListOf<Int>()
    // Whether the runtime still had the attachment when the listener got ERROR, see
    // detachAnchorOnError().
    private var wasAttachedOnError: Boolean? = null
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
        OpenXrActivitySpace(
            activity,
            rootHandle,
            fakeNative,
            sceneCommitter,
            nodeRegistry,
            executor,
        )

    private fun createAnchorEntity(
        entityHandle: Long = fakeNative.createSceneEntity(),
        activitySpace: OpenXrActivitySpace = this.activitySpace,
    ) =
        OpenXrAnchorEntity(
            activity,
            entityHandle,
            fakeNative,
            sceneCommitter,
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
                sceneCommitter,
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
     * Records later state changes like [recordStateChanges], and destroys the anchor's space on
     * ERROR, as AnchorSpace does when it detaches its anchor. Records then in [wasAttachedOnError]
     * whether the runtime still had the attachment.
     */
    private fun detachAnchorOnError() {
        val handle = anchorEntity.entityHandle
        anchorEntity.setOnStateChangedListener { state ->
            stateChanges.add(state)
            if (state == AnchorEntity.State.ERROR) {
                wasAttachedOnError = handle in fakeNative.entityAttachments
                fakeNative.destroySpace(fakeNative.fakeSpaceHandle)
            }
        }
        executor.runAll()
        stateChanges.clear()
    }

    /**
     * Attaches to [FakeSceneCoreOpenXrNative.fakeSpaceHandle] and commits the attachment, then runs
     * the collector, which reads the first sample.
     */
    private fun attachAndCollect(): Boolean {
        val isAttached = anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)
        executor.commitSceneChanges()
        return isAttached
    }

    private fun emit(trackingState: TrackingState, pose: Pose = anchorPose) {
        samples.value = AnchorSample(trackingState, pose)
        executor.runAll()
    }

    /** Waits until [thread] blocks, for example on a lock, or ends. */
    private fun awaitBlockedOrTerminated(thread: Thread) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5L)
        while (thread.state != Thread.State.BLOCKED && thread.state != Thread.State.TERMINATED) {
            check(System.nanoTime() < deadline) { "Timed out waiting for $thread to block." }
            // Waits up to 1 ms for it to end.
            thread.join(/* millis= */ 1L)
        }
    }

    /** Waits until [condition] holds on [executor], where the scene committer changes the fake. */
    private fun awaitOnExecutor(executor: ScheduledExecutorService, condition: () -> Boolean) {
        repeat(500) {
            val isMet =
                executor
                    .schedule(Callable { condition() }, /* delay= */ 10L, TimeUnit.MILLISECONDS)
                    .get(/* timeout= */ 1L, TimeUnit.SECONDS)
            if (isMet) return
        }
        throw AssertionError("Timed out waiting for the condition.")
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
        anchorEntity.attach(null, samples)

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

        // Commits the attachment before the report of the earlier state runs. Only the fake
        // executor runs them in this order, since it runs a due commit before the tasks posted
        // earlier. setOnStateChangedListener_attachFailsBeforeReport_reportsOnlyError covers the
        // same guard in an order that a real executor can run.
        executor.commitSceneChanges()

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
    fun setOnStateChangedListener_whileTheRemovalWaits_reportsErrorOnceItCommits() {
        attachAndCollect()
        fakeNative.simulateTransactionUnavailable = true
        emit(TrackingState.STOPPED)

        anchorEntity.setOnStateChangedListener { stateChanges.add(it) }
        executor.runAll()
        assertThat(stateChanges).isEmpty()

        fakeNative.simulateTransactionUnavailable = false
        executor.commitSceneChanges()

        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
    }

    @Test
    fun setOnStateChangedListener_afterTheExecutorShutDown_doesNotThrow() {
        executor.shutdown()

        anchorEntity.setOnStateChangedListener { stateChanges.add(it) }

        assertThat(stateChanges).isEmpty()
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
        executor.commitSceneChanges()

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
        // Commits the attachment, which posts the report of ANCHORED.
        executor.simulateSleepExecutingAtMostOneTask()

        anchorEntity.dispose()
        executor.runAll()

        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
    }

    @Test
    fun attach_untilTheAttachmentCommits_keepsItsState() {
        recordStateChanges()

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isTrue()
        executor.runAll()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.UNANCHORED)
        assertThat(stateChanges).isEmpty()
        assertThat(fakeNative.submittedTransactions).isEmpty()
    }

    @Test
    fun attach_parentsToRootAndAttachesToSpaceInOneTransaction() {
        recordStateChanges()

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isTrue()
        executor.commitSceneChanges()

        val handle = anchorEntity.entityHandle
        val transaction = fakeNative.submittedTransactions.single()
        assertThat(transaction.parents).containsExactly(handle, fakeNative.fakeRootEntityHandle)
        assertThat(transaction.attachments)
            .containsExactly(
                handle,
                Attachment(fakeNative.fakeSpaceHandle, ATTACHMENT_LOSS_BEHAVIOUR_LAST_KNOWN),
            )
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
        executor.commitSceneChanges()

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
        executor.commitSceneChanges()
        assertThat(fakeNative.committedTransactions).isEmpty()
    }

    @Test
    fun attach_rootNotBound_movesToErrorAndReturnsFalse() {
        anchorEntity = createAnchorEntity(activitySpace = createActivitySpace(INVALID_HANDLE))

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isFalse()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        executor.commitSceneChanges()
        assertThat(fakeNative.committedTransactions).isEmpty()
    }

    @Test
    fun attach_noTransactionAvailable_anchorsOnceOneIsFree() {
        recordStateChanges()
        fakeNative.simulateTransactionUnavailable = true

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isTrue()
        executor.commitSceneChanges()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.UNANCHORED)
        assertThat(fakeNative.committedTransactions).isEmpty()

        fakeNative.simulateTransactionUnavailable = false
        executor.commitSceneChanges()
        emit(TrackingState.TRACKING)

        assertThat(fakeNative.entityAttachments[anchorEntity.entityHandle])
            .isEqualTo(Attachment(fakeNative.fakeSpaceHandle, ATTACHMENT_LOSS_BEHAVIOUR_LAST_KNOWN))
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ANCHORED)
        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(anchorPose)
    }

    @Test
    fun attach_again_noTransactionAvailable_followsEarlierAnchorUntilCommitted() {
        attachAndCollect()
        emit(TrackingState.TRACKING)
        val otherSamples = MutableStateFlow(AnchorSample(TrackingState.PAUSED, Pose()))
        val laterPose = Pose(Vector3(7f, 8f, 9f))
        fakeNative.simulateTransactionUnavailable = true

        assertThat(anchorEntity.attach(OTHER_SPACE_HANDLE, otherSamples)).isTrue()
        executor.commitSceneChanges()
        emit(TrackingState.TRACKING, laterPose)

        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(laterPose)
        assertThat(fakeNative.entityAttachments[anchorEntity.entityHandle]?.spaceHandle)
            .isEqualTo(fakeNative.fakeSpaceHandle)

        fakeNative.simulateTransactionUnavailable = false
        executor.commitSceneChanges()
        emit(TrackingState.TRACKING, anchorPose)

        assertThat(fakeNative.entityAttachments[anchorEntity.entityHandle]?.spaceHandle)
            .isEqualTo(OTHER_SPACE_HANDLE)
        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(Pose.Identity)
        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
    }

    @Test
    fun attach_againBeforeTheFirstCommits_attachesOnlyToTheLatestSpace() {
        recordStateChanges()
        val otherSamples = MutableStateFlow(AnchorSample(TrackingState.PAUSED, Pose()))

        anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)
        anchorEntity.attach(OTHER_SPACE_HANDLE, otherSamples)
        // The first anchor never committed, so this entity doesn't use it anymore.
        fakeNative.destroySpace(fakeNative.fakeSpaceHandle)
        executor.commitSceneChanges()

        assertThat(fakeNative.submittedTransactions.single().attachments)
            .containsExactly(
                anchorEntity.entityHandle,
                Attachment(OTHER_SPACE_HANDLE, ATTACHMENT_LOSS_BEHAVIOUR_LAST_KNOWN),
            )
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ANCHORED)
    }

    @Test
    fun attach_attachmentRejected_movesToErrorWithoutParenting() {
        recordStateChanges()
        fakeNative.simulateAttachmentFailure = true

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isTrue()
        executor.commitSceneChanges()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
        // Doesn't unparent the entity, which was never parented.
        assertThat(fakeNative.submittedTransactions).hasSize(1)
        assertThat(fakeNative.entityParents).doesNotContainKey(anchorEntity.entityHandle)
        assertThat(fakeNative.entityAttachments).isEmpty()
        assertThat(fakeNative.openTransactions).isEmpty()
    }

    @Test
    fun attach_withoutAttachmentNatives_movesToErrorWithoutParenting() {
        fakeNative.simulateMissingAttachmentNatives = true

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isTrue()
        executor.commitSceneChanges()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(fakeNative.entityParents).doesNotContainKey(anchorEntity.entityHandle)
        assertThat(fakeNative.openTransactions).isEmpty()
    }

    @Test
    fun attach_afterError_returnsFalseWithoutNativeChanges() {
        anchorEntity.attach(INVALID_HANDLE, samples)

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isFalse()
        executor.commitSceneChanges()

        assertThat(fakeNative.committedTransactions).isEmpty()
        assertThat(fakeNative.entityAttachments).isEmpty()
    }

    @Test
    fun attach_again_replacesAttachmentAndIgnoresEarlierAnchor() {
        attachAndCollect()
        emit(TrackingState.TRACKING)
        val otherSamples = MutableStateFlow(AnchorSample(TrackingState.PAUSED, Pose()))

        assertThat(anchorEntity.attach(OTHER_SPACE_HANDLE, otherSamples)).isTrue()
        executor.commitSceneChanges()
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
        executor.commitSceneChanges()
        emit(TrackingState.TRACKING, Pose(Vector3(7f, 8f, 9f)))

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
        assertThat(fakeNative.entityAttachments).doesNotContainKey(handle)
        assertThat(fakeNative.entityParents).doesNotContainKey(handle)
        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(anchorPose)
    }

    @Test
    fun attach_failingBeforeTheEarlierAttachmentCommits_reportsErrorAndNeverStagesIt() {
        detachAnchorOnError()
        val handle = anchorEntity.entityHandle
        anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)

        // For example, the new anchor has no XrSpace.
        assertThat(anchorEntity.attach(null, samples)).isFalse()
        // The runtime never had the attachment, so ERROR is reported right away, and the listener
        // destroys the space before the commit that the attachment waited for.
        executor.runAll()
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
        executor.commitSceneChanges()

        assertThat(fakeNative.submittedTransactions.flatMap { it.attachments.keys })
            .doesNotContain(handle)
        assertThat(fakeNative.submittedTransactions.flatMap { it.parents.keys })
            .doesNotContain(handle)
    }

    @Test
    fun attach_againWhileTheEarlierAttachmentWaitsToBeStaged_neverStagesIt() {
        val laterSpaceHandle = 4002L
        // The other entity's attachment waited longer, so the commit stages it first.
        val otherEntity = createAnchorEntity()
        otherEntity.attach(OTHER_SPACE_HANDLE, samples)
        anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)
        fakeNative.onSetTransactionSpaceAttachment = {
            fakeNative.onSetTransactionSpaceAttachment = null
            // The commit has the earlier attachment, which it hasn't staged yet.
            anchorEntity.attach(laterSpaceHandle, samples)
            // The earlier anchor never committed, so this entity doesn't use it anymore.
            fakeNative.destroySpace(fakeNative.fakeSpaceHandle)
        }

        executor.commitSceneChanges()

        val handle = anchorEntity.entityHandle
        val firstTransaction = fakeNative.submittedTransactions.first()
        assertThat(firstTransaction.attachments.keys).containsExactly(otherEntity.entityHandle)
        assertThat(firstTransaction.parents).doesNotContainKey(handle)
        assertThat(fakeNative.entityAttachments[handle])
            .isEqualTo(Attachment(laterSpaceHandle, ATTACHMENT_LOSS_BEHAVIOUR_LAST_KNOWN))
        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
    }

    @Test
    fun attach_afterTheCommitterShutDown_movesToErrorAndReturnsFalse() {
        recordStateChanges()
        sceneCommitter.shutdown()

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isFalse()
        executor.runAll()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
    }

    @Test
    fun attach_committerShutsDownBeforeTheAttachmentCommits_movesToError() {
        recordStateChanges()
        anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)

        sceneCommitter.shutdown()
        executor.runAll()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
    }

    @Test
    fun attach_again_rejected_removesTheEarlierAttachmentAndMovesToError() {
        attachAndCollect()
        recordStateChanges()
        val handle = anchorEntity.entityHandle
        // Fails to stage the new attachment, but can still remove the earlier one.
        fakeNative.simulateMissingAttachmentNatives = true

        assertThat(anchorEntity.attach(OTHER_SPACE_HANDLE, samples)).isTrue()
        executor.commitSceneChanges()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
        assertThat(fakeNative.entityAttachments).doesNotContainKey(handle)
        assertThat(fakeNative.entityParents).doesNotContainKey(handle)
    }

    @Test
    fun attach_againWhileTheFirstCommits_rejected_removesTheFirstAttachment() {
        val handle = anchorEntity.entityHandle
        val otherSamples = MutableStateFlow(AnchorSample(TrackingState.PAUSED, Pose()))
        fakeNative.onSubmitSceneTransaction = {
            fakeNative.onSubmitSceneTransaction = null
            // The first attachment is staged by now, but the runtime rejects the second one.
            fakeNative.simulateMissingAttachmentNatives = true
            anchorEntity.attach(OTHER_SPACE_HANDLE, otherSamples)
        }

        anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)
        executor.commitSceneChanges()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(fakeNative.entityAttachments).doesNotContainKey(handle)
        assertThat(fakeNative.entityParents).doesNotContainKey(handle)
    }

    @Test
    fun attach_failingWhileTheEarlierAttachmentCommits_reportsErrorOnceItIsRemoved() {
        detachAnchorOnError()
        val handle = anchorEntity.entityHandle
        fakeNative.onSubmitSceneTransaction = {
            fakeNative.onSubmitSceneTransaction = null
            // The earlier attachment is staged by now. For example, the new anchor has no XrSpace.
            anchorEntity.attach(null, samples)
        }

        anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)
        executor.commitSceneChanges()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
        assertThat(wasAttachedOnError).isFalse()
        assertThat(fakeNative.entityAttachments).doesNotContainKey(handle)
        assertThat(fakeNative.entityParents).doesNotContainKey(handle)
    }

    @Test
    fun attach_afterDispose_returnsFalse() {
        anchorEntity.dispose()

        assertThat(anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)).isFalse()
    }

    @Test
    fun attach_waitingToCommit_doesNotKeepTheEntityReachable() {
        fun attachNewEntity(): WeakReference<OpenXrAnchorEntity> {
            val entity = createAnchorEntity()
            entity.attach(fakeNative.fakeSpaceHandle, samples)
            return WeakReference(entity)
        }

        val entityRef = attachNewEntity()

        MemoryUtils.assertGarbageCollected(entityRef)
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
    fun stoppedSample_movesToErrorAndReportsItOnceTheAttachmentIsRemoved() {
        attachAndCollect()
        emit(TrackingState.TRACKING)
        detachAnchorOnError()
        val handle = anchorEntity.entityHandle

        emit(TrackingState.STOPPED)

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(anchorEntity.activitySpacePose).isEqualTo(Pose())
        assertThat(stateChanges).isEmpty()

        executor.commitSceneChanges()

        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
        assertThat(wasAttachedOnError).isFalse()
        assertThat(fakeNative.entityAttachments).doesNotContainKey(handle)
        assertThat(fakeNative.entityParents).doesNotContainKey(handle)
    }

    @Test
    fun samples_afterStopped_areIgnored() {
        attachAndCollect()
        emit(TrackingState.TRACKING)
        emit(TrackingState.STOPPED)
        executor.commitSceneChanges()

        emit(TrackingState.TRACKING, Pose(Vector3(7f, 8f, 9f)))

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(anchorPose)
    }

    @Test
    fun stoppedSample_noTransactionAvailable_reportsErrorOnceTheRemovalCommits() {
        attachAndCollect()
        emit(TrackingState.TRACKING)
        detachAnchorOnError()
        val handle = anchorEntity.entityHandle
        fakeNative.simulateTransactionUnavailable = true

        emit(TrackingState.STOPPED)
        executor.commitSceneChanges()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(stateChanges).isEmpty()
        assertThat(fakeNative.entityAttachments).containsKey(handle)

        fakeNative.simulateTransactionUnavailable = false
        executor.commitSceneChanges()

        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
        assertThat(wasAttachedOnError).isFalse()
        assertThat(fakeNative.destroyedSpaces).contains(fakeNative.fakeSpaceHandle)
    }

    @Test
    fun stoppedSample_removalRejected_reportsErrorAndLeavesTheEntityParented() {
        attachAndCollect()
        recordStateChanges()
        val handle = anchorEntity.entityHandle
        fakeNative.simulateAttachmentFailure = true

        emit(TrackingState.STOPPED)
        executor.commitSceneChanges()

        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
        // The runtime may still have the attachment, so the entity stays parented to the root.
        assertThat(fakeNative.entityParents[handle]).isEqualTo(fakeNative.fakeRootEntityHandle)
    }

    @Test
    fun stoppedSample_committerShutsDownBeforeTheRemovalCommits_reportsError() {
        attachAndCollect()
        recordStateChanges()
        fakeNative.simulateTransactionUnavailable = true
        emit(TrackingState.STOPPED)
        executor.commitSceneChanges()
        assertThat(stateChanges).isEmpty()

        sceneCommitter.shutdown()
        executor.runAll()

        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
    }

    @Test
    fun stoppedSample_nativeInstanceDestroyedBeforeTheRemovalCommits_reportsError() {
        attachAndCollect()
        recordStateChanges()
        fakeNative.simulateTransactionUnavailable = true
        emit(TrackingState.STOPPED)
        executor.commitSceneChanges()
        assertThat(stateChanges).isEmpty()

        // Releases every native entity, attachments included.
        fakeNative.destroy()
        executor.commitSceneChanges()

        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
    }

    @Test
    fun stoppedSample_afterTheCommitterShutDown_reportsErrorRightAway() {
        attachAndCollect()
        recordStateChanges()
        sceneCommitter.shutdown()

        emit(TrackingState.STOPPED)

        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
    }

    @Test
    fun earlierAnchorStopped_whileReplacementWaits_followsReplacementOnceItCommits() {
        attachAndCollect()
        emit(TrackingState.TRACKING)
        recordStateChanges()
        val handle = anchorEntity.entityHandle
        val otherSamples = MutableStateFlow(AnchorSample(TrackingState.PAUSED, Pose()))
        val otherPose = Pose(Vector3(7f, 8f, 9f))
        fakeNative.simulateTransactionUnavailable = true
        assertThat(anchorEntity.attach(OTHER_SPACE_HANDLE, otherSamples)).isTrue()
        executor.commitSceneChanges()

        emit(TrackingState.STOPPED)
        // This entity no longer follows the earlier anchor.
        emit(TrackingState.TRACKING, Pose(Vector3(4f, 5f, 6f)))

        assertThat(anchorEntity.state).isNotEqualTo(AnchorEntity.State.ERROR)
        assertThat(stateChanges).doesNotContain(AnchorEntity.State.ERROR)
        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(anchorPose)

        fakeNative.simulateTransactionUnavailable = false
        executor.commitSceneChanges()

        assertThat(fakeNative.entityAttachments[handle])
            .isEqualTo(Attachment(OTHER_SPACE_HANDLE, ATTACHMENT_LOSS_BEHAVIOUR_LAST_KNOWN))
        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
        // The earlier anchor's pose no longer applies.
        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(Pose.Identity)

        otherSamples.value = AnchorSample(TrackingState.TRACKING, otherPose)
        executor.runAll()

        assertThat(anchorEntity.poseInPlatformReferenceSpace).isEqualTo(otherPose)
        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
        assertThat(stateChanges).doesNotContain(AnchorEntity.State.ERROR)
    }

    @Test
    fun earlierAnchorStopped_whileReplacementWaits_replacementRejected_movesToErrorAndRemovesAttachment() {
        attachAndCollect()
        emit(TrackingState.TRACKING)
        detachAnchorOnError()
        val handle = anchorEntity.entityHandle
        val otherSamples = MutableStateFlow(AnchorSample(TrackingState.PAUSED, Pose()))
        fakeNative.simulateTransactionUnavailable = true
        assertThat(anchorEntity.attach(OTHER_SPACE_HANDLE, otherSamples)).isTrue()
        executor.commitSceneChanges()
        emit(TrackingState.STOPPED)
        assertThat(anchorEntity.state).isNotEqualTo(AnchorEntity.State.ERROR)
        // Fails to stage the new attachment, but can still remove the earlier one.
        fakeNative.simulateMissingAttachmentNatives = true
        fakeNative.simulateTransactionUnavailable = false

        executor.commitSceneChanges()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
        // The listener got ERROR only once the removal of the earlier attachment committed.
        assertThat(wasAttachedOnError).isFalse()
        assertThat(fakeNative.entityAttachments).doesNotContainKey(handle)
        assertThat(fakeNative.entityParents).doesNotContainKey(handle)
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
        executor.commitSceneChanges()

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
    fun dispose_reportsErrorThenDestroysEntity() {
        attachAndCollect()
        recordStateChanges()
        val handle = anchorEntity.entityHandle

        anchorEntity.dispose()
        executor.runAll()
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)

        executor.commitSceneChanges()

        // Destroying the native entity removes its attachment too, so nothing else is committed.
        assertThat(fakeNative.submittedTransactions).hasSize(1)
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
        executor.commitSceneChanges()

        assertThat(fakeNative.committedTransactions).isEmpty()
        assertThat(fakeNative.destroyedEntities).contains(handle)
    }

    @Test
    fun dispose_afterNativeDestroyed_movesToErrorWithoutThrowing() {
        attachAndCollect()
        fakeNative.destroy()

        anchorEntity.dispose()
        executor.commitSceneChanges()

        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
    }

    @Test
    fun dispose_whileWaitingForTheAttachmentToCommit_dropsIt() {
        val handle = anchorEntity.entityHandle
        fakeNative.simulateTransactionUnavailable = true
        anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)
        executor.commitSceneChanges()
        fakeNative.simulateTransactionUnavailable = false

        anchorEntity.dispose()
        fakeNative.destroySpace(fakeNative.fakeSpaceHandle)
        executor.commitSceneChanges()

        assertThat(fakeNative.committedTransactions).isEmpty()
        assertThat(fakeNative.destroyedEntities).contains(handle)
        assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
    }

    @Test
    fun dispose_whileTheRemovalWaits_reportsErrorRightAway() {
        attachAndCollect()
        recordStateChanges()
        val handle = anchorEntity.entityHandle
        fakeNative.simulateTransactionUnavailable = true
        emit(TrackingState.STOPPED)
        assertThat(stateChanges).isEmpty()

        anchorEntity.dispose()
        executor.runAll()
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)

        fakeNative.simulateTransactionUnavailable = false
        executor.commitSceneChanges()

        // Destroying the native entity removes the attachment instead.
        assertThat(fakeNative.destroyedEntities).contains(handle)
        assertThat(stateChanges).containsExactly(AnchorEntity.State.ERROR)
    }

    @Test
    fun dispose_whileTheCommitterStagesTheAttachment_waitsForItToFinish() {
        val realExecutor = Executors.newSingleThreadScheduledExecutor()
        try {
            val committer = OpenXrSceneCommitter(fakeNative, realExecutor)
            anchorEntity =
                OpenXrAnchorEntity(
                    activity,
                    fakeNative.createSceneEntity(),
                    fakeNative,
                    committer,
                    nodeRegistry,
                    realExecutor,
                    activitySpace,
                )
            val isStaging = CountDownLatch(1)
            val canFinishStaging = CountDownLatch(1)
            fakeNative.onSetTransactionSpaceAttachment = {
                isStaging.countDown()
                canFinishStaging.await(5L, TimeUnit.SECONDS)
            }
            anchorEntity.attach(fakeNative.fakeSpaceHandle, samples)
            assertThat(isStaging.await(5L, TimeUnit.SECONDS)).isTrue()

            val disposer = thread {
                anchorEntity.dispose()
                // The anchor can be detached once dispose() returns.
                fakeNative.destroySpace(fakeNative.fakeSpaceHandle)
            }
            // Lets dispose() destroy the space before the staging finishes, if it doesn't wait.
            awaitBlockedOrTerminated(disposer)
            val wasDisposeWaiting = disposer.isAlive
            canFinishStaging.countDown()
            disposer.join(/* millis= */ 5_000L)
            // Waits for the staging to read the space, and for the entity's destruction after it.
            awaitOnExecutor(realExecutor) {
                fakeNative.stagedDestroyedSpaces.isNotEmpty() ||
                    fakeNative.destroyedEntities.isNotEmpty()
            }

            assertThat(wasDisposeWaiting).isTrue()
            assertThat(fakeNative.stagedDestroyedSpaces).isEmpty()
            assertThat(disposer.isAlive).isFalse()
        } finally {
            realExecutor.shutdownNow()
        }
    }

    @Test
    fun dispose_noTransactionAvailable_destroysEntityOnceOneIs() {
        attachAndCollect()
        val handle = anchorEntity.entityHandle
        fakeNative.simulateTransactionUnavailable = true

        anchorEntity.dispose()
        // A few tries, each of which fails.
        repeat(3) { executor.simulateSleepExecutingAtMostOneTask() }
        assertThat(fakeNative.destroyedEntities).doesNotContain(handle)

        fakeNative.simulateTransactionUnavailable = false
        executor.commitSceneChanges()
        assertThat(fakeNative.destroyedEntities).contains(handle)
    }
}
