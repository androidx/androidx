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

import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.openxr.OpenXrSceneCommitter.Companion.COALESCING_DELAY_MS
import androidx.xr.scenecore.openxr.OpenXrSceneCommitter.Companion.INITIAL_RETRY_DELAY_MS
import androidx.xr.scenecore.openxr.OpenXrSceneCommitter.Companion.MAX_ENTITIES_PER_TRANSACTION
import androidx.xr.scenecore.openxr.OpenXrSceneCommitter.Companion.MAX_RETRY_DELAY_MS
import androidx.xr.scenecore.openxr.OpenXrSceneCommitter.Companion.MIN_COMMIT_INTERVAL_MS
import androidx.xr.scenecore.openxr.testing.FakeSceneCoreOpenXrNative
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** About one frame at 90 Hz. */
private const val FRAME_DURATION_MS = 11L

// For the java.time.Duration that FakeScheduledExecutorService takes to simulate time.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(JUnit4::class)
class OpenXrSceneCommitterTest {

    private val fakeNative = FakeSceneCoreOpenXrNative()
    private val executor = FakeScheduledExecutorService()
    private val committer = OpenXrSceneCommitter(fakeNative, executor)

    private val poseA = Pose(Vector3(1f, 2f, 3f))
    private val poseB = Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(Vector3(0f, 90f, 0f)))
    private val scaleA = Vector3(2f, 3f, 4f)

    private var entity = INVALID_HANDLE
    private var otherEntity = INVALID_HANDLE
    private var rootEntity = INVALID_HANDLE

    @Before
    fun setUp() {
        fakeNative.init(100L, 200L, 300L)
        fakeNative.createSpatialContainer()
        entity = fakeNative.createSceneEntity()
        otherEntity = fakeNative.createSceneEntity()
        rootEntity = fakeNative.fakeRootEntityHandle
    }

    /** Lets [durationMs] of simulated time pass, running the commits due by then. */
    private fun advanceTimeBy(durationMs: Long) {
        executor.simulateSleepExecutingAllTasks(Duration.ofMillis(durationMs))
    }

    /** Lets enough time pass for the committer to commit, retry or give up everything. */
    private fun commitAll() {
        advanceTimeBy(10_000L)
    }

    /**
     * Lets [durationMs] pass 1 ms at a time, running [eachMs] after each, and returns the times, in
     * ms from now, at which [count] went up.
     */
    private fun timesWhenIncreased(
        durationMs: Long,
        count: () -> Int,
        eachMs: (time: Long) -> Unit = {},
    ): List<Long> {
        val times = mutableListOf<Long>()
        for (time in 1L..durationMs) {
            val before = count()
            advanceTimeBy(1L)
            if (count() > before) times += time
            eachMs(time)
        }
        return times
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

    @Test
    fun constructor_invalidArguments_throwIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            OpenXrSceneCommitter(fakeNative, executor, coalescingDelayMs = -1L)
        }
        assertFailsWith<IllegalArgumentException> {
            OpenXrSceneCommitter(fakeNative, executor, initialRetryDelayMs = 0L)
        }
        assertFailsWith<IllegalArgumentException> {
            OpenXrSceneCommitter(
                fakeNative,
                executor,
                initialRetryDelayMs = 8L,
                maxRetryDelayMs = 4L,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            OpenXrSceneCommitter(fakeNative, executor, maxEntitiesPerTransaction = 0)
        }
    }

    @Test
    fun edit_commitsTheChangeAfterTheCoalescingDelay() {
        committer.edit { setTransform(entity, poseA, scaleA) }

        advanceTimeBy(COALESCING_DELAY_MS - 1)
        assertThat(fakeNative.transactionCreateAttempts).isEqualTo(0)

        advanceTimeBy(1L)
        assertThat(fakeNative.entityTransforms[entity]).isEqualTo(poseA to scaleA)
    }

    @Test
    fun edit_changesMadeWithinTheCoalescingDelay_commitInOneTransaction() {
        committer.edit { setTransform(entity, poseA, scaleA) }
        advanceTimeBy(COALESCING_DELAY_MS - 1)
        committer.edit { setParent(otherEntity, entity) }

        commitAll()

        val transaction = fakeNative.submittedTransactions.single()
        assertThat(transaction.transforms).containsExactly(entity, poseA to scaleA)
        assertThat(transaction.parents).containsExactly(otherEntity, entity)
    }

    @Test
    fun edit_componentSetSeveralTimes_commitsOnlyTheLatestValue() {
        committer.edit { setTransform(entity, poseA, scaleA) }
        committer.edit { setTransform(entity, poseB, Vector3.One) }

        commitAll()

        assertThat(fakeNative.submittedTransactions.single().transforms)
            .containsExactly(entity, poseB to Vector3.One)
    }

    @Test
    fun edit_parentAndTransformOfAnEntity_commitTogether() {
        committer.edit { setParent(entity, otherEntity) }
        committer.edit { setTransform(entity, poseA, scaleA) }

        commitAll()

        val transaction = fakeNative.submittedTransactions.single()
        assertThat(transaction.parents).containsExactly(entity, otherEntity)
        assertThat(transaction.transforms).containsExactly(entity, poseA to scaleA)
    }

    @Test
    fun edit_shortlyAfterACommit_waitsForTheMinimumCommitInterval() {
        committer.edit { setTransform(entity, poseA, scaleA) }
        advanceTimeBy(COALESCING_DELAY_MS)
        assertThat(fakeNative.submittedTransactions).hasSize(1)

        committer.edit { setTransform(entity, poseB, scaleA) }
        advanceTimeBy(MIN_COMMIT_INTERVAL_MS - 1)
        assertThat(fakeNative.submittedTransactions).hasSize(1)

        advanceTimeBy(1L)
        assertThat(fakeNative.submittedTransactions).hasSize(2)
        assertThat(fakeNative.entityTransforms[entity]).isEqualTo(poseB to scaleA)
    }

    @Test
    fun edit_afterTheCommitterWentIdle_commitsAfterTheCoalescingDelay() {
        committer.edit { setTransform(entity, poseA, scaleA) }
        commitAll()

        committer.edit { setTransform(entity, poseB, scaleA) }
        advanceTimeBy(COALESCING_DELAY_MS)

        assertThat(fakeNative.entityTransforms[entity]).isEqualTo(poseB to scaleA)
    }

    @Test
    fun edit_continuousChanges_commitOncePerMinimumCommitInterval() {
        val submitTimes =
            timesWhenIncreased(
                durationMs = 200L,
                count = { fakeNative.submittedTransactions.size },
            ) { time ->
                committer.edit {
                    setTransform(entity, Pose(Vector3(time.toFloat(), 0f, 0f)), scaleA)
                }
            }
        commitAll()

        // The first change is 1 ms in, and commits after the coalescing delay. The next commits are
        // the minimum interval apart, until the end.
        assertThat(submitTimes)
            .containsExactlyElementsIn(1L + COALESCING_DELAY_MS..200L step MIN_COMMIT_INTERVAL_MS)
            .inOrder()
        assertThat(fakeNative.entityTransforms[entity])
            .isEqualTo(Pose(Vector3(200f, 0f, 0f)) to scaleA)
    }

    @Test
    fun edit_withoutChanges_schedulesNothing() {
        committer.edit {}

        assertThat(executor.isEmpty()).isTrue()
    }

    @Test
    fun edit_sameValueAfterItCommitted_commitsAgain() {
        committer.edit { setTransform(entity, poseA, scaleA) }
        commitAll()

        committer.edit { setTransform(entity, poseA, scaleA) }
        commitAll()

        assertThat(fakeNative.submittedTransactions).hasSize(2)
    }

    @Test
    fun edit_invalidHandle_throwsAndStillCommitsTheEarlierChanges() {
        assertFailsWith<IllegalArgumentException> {
            committer.edit {
                setTransform(entity, poseA, scaleA)
                setTransform(INVALID_HANDLE, poseB, scaleA)
            }
        }

        commitAll()

        assertThat(fakeNative.entityTransforms).containsExactly(entity, poseA to scaleA)
    }

    @Test
    fun edit_duringACommit_commitsInTheNextCommit() {
        committer.edit { setTransform(entity, poseA, scaleA) }
        fakeNative.onSubmitSceneTransaction = {
            fakeNative.onSubmitSceneTransaction = null
            committer.edit { setTransform(entity, poseB, scaleA) }
        }

        advanceTimeBy(COALESCING_DELAY_MS)
        assertThat(fakeNative.submittedTransactions.single().transforms)
            .containsExactly(entity, poseA to scaleA)

        advanceTimeBy(MIN_COMMIT_INTERVAL_MS)
        assertThat(fakeNative.submittedTransactions).hasSize(2)
        assertThat(fakeNative.entityTransforms[entity]).isEqualTo(poseB to scaleA)
    }

    @Test
    fun commit_neverCancelsTheTransactionsItCreates() {
        fakeNative.simulateTransactionUnavailable = true
        committer.edit { setTransform(entity, poseA, scaleA) }
        advanceTimeBy(20L)
        fakeNative.simulateTransactionUnavailable = false
        committer.edit { setTransform(999L, poseA, scaleA) }

        commitAll()

        assertThat(fakeNative.cancelledTransactions).isEmpty()
        assertThat(fakeNative.openTransactions).isEmpty()
        assertThat(fakeNative.leakedTransactionSlots).isEqualTo(0)
    }

    @Test
    fun commit_whileNoTransactionIsFree_triesAgainWithExponentialBackoff() {
        val committer =
            OpenXrSceneCommitter(
                fakeNative,
                executor,
                coalescingDelayMs = 2L,
                initialRetryDelayMs = 4L,
                maxRetryDelayMs = 32L,
            )
        fakeNative.simulateTransactionUnavailable = true
        committer.edit { setTransform(entity, poseA, scaleA) }

        val attemptTimes =
            timesWhenIncreased(
                durationMs = 130L,
                count = { fakeNative.transactionCreateAttempts },
            )

        assertThat(attemptTimes).containsExactly(2L, 6L, 14L, 30L, 62L, 94L, 126L).inOrder()
        assertThat(fakeNative.submittedTransactions).isEmpty()
    }

    @Test
    fun commit_onceATransactionIsFree_commitsTheChanges() {
        fakeNative.simulateTransactionUnavailable = true
        committer.edit { setTransform(entity, poseA, scaleA) }
        advanceTimeBy(100L)
        assertThat(fakeNative.entityTransforms).isEmpty()

        fakeNative.simulateTransactionUnavailable = false
        commitAll()

        assertThat(fakeNative.entityTransforms[entity]).isEqualTo(poseA to scaleA)
    }

    @Test
    fun commit_afterACommitSucceeds_backsOffFromTheInitialDelayAgain() {
        val committer =
            OpenXrSceneCommitter(
                fakeNative,
                executor,
                coalescingDelayMs = 2L,
                minCommitIntervalMs = 10L,
                initialRetryDelayMs = 4L,
                maxRetryDelayMs = 32L,
            )
        fakeNative.simulateTransactionUnavailable = true
        committer.edit { setTransform(entity, poseA, scaleA) }
        // Tries at 2, 6, 14 and 30 ms, then at 62 ms, when a transaction is free.
        advanceTimeBy(30L)
        fakeNative.simulateTransactionUnavailable = false
        advanceTimeBy(32L)
        assertThat(fakeNative.submittedTransactions).hasSize(1)
        fakeNative.simulateTransactionUnavailable = true

        committer.edit { setTransform(entity, poseB, scaleA) }
        val attemptTimes =
            timesWhenIncreased(
                durationMs = 25L,
                count = { fakeNative.transactionCreateAttempts },
            )

        // 10 ms after the commit, then 4 and 8 ms after each failed try.
        assertThat(attemptTimes).containsExactly(10L, 14L, 22L).inOrder()
    }

    @Test
    fun commit_changesMadeWhileWaitingForATransaction_commitTogether() {
        fakeNative.simulateTransactionUnavailable = true
        committer.edit { setTransform(entity, poseA, scaleA) }
        advanceTimeBy(20L)
        committer.edit { setTransform(entity, poseB, scaleA) }
        committer.edit { setParent(otherEntity, entity) }
        fakeNative.simulateTransactionUnavailable = false

        commitAll()

        val transaction = fakeNative.submittedTransactions.single()
        assertThat(transaction.transforms).containsExactly(entity, poseB to scaleA)
        assertThat(transaction.parents).containsExactly(otherEntity, entity)
    }

    @Test
    fun commit_transactionNotSubmitted_commitsTheChangesAgainAfterBackingOff() {
        fakeNative.simulateSubmitFailure = true
        committer.edit { setTransform(entity, poseA, scaleA) }
        advanceTimeBy(COALESCING_DELAY_MS)
        assertThat(fakeNative.submittedTransactions).isEmpty()
        fakeNative.simulateSubmitFailure = false

        advanceTimeBy(INITIAL_RETRY_DELAY_MS)

        assertThat(fakeNative.submittedTransactions.single().transforms)
            .containsExactly(entity, poseA to scaleA)
    }

    @Test
    fun commit_transactionNotSubmitted_commitsItsChangesWithTheNewerOnes() {
        fakeNative.simulateSubmitFailure = true
        committer.edit { setTransform(entity, poseA, scaleA) }
        fakeNative.onSubmitSceneTransaction = {
            fakeNative.onSubmitSceneTransaction = null
            committer.edit {
                setTransform(otherEntity, poseA, scaleA)
                setParent(entity, otherEntity)
            }
        }
        advanceTimeBy(COALESCING_DELAY_MS)
        fakeNative.simulateSubmitFailure = false

        commitAll()

        val transaction = fakeNative.submittedTransactions.single()
        assertThat(transaction.transforms)
            .containsExactly(entity, poseA to scaleA, otherEntity, poseA to scaleA)
        assertThat(transaction.parents).containsExactly(entity, otherEntity)
    }

    @Test
    fun commit_transactionNotSubmitted_keepsTheNewerValueOfAComponent() {
        fakeNative.simulateSubmitFailure = true
        committer.edit { setTransform(entity, poseA, scaleA) }
        fakeNative.onSubmitSceneTransaction = {
            fakeNative.onSubmitSceneTransaction = null
            committer.edit { setTransform(entity, poseB, scaleA) }
        }
        advanceTimeBy(COALESCING_DELAY_MS)
        fakeNative.simulateSubmitFailure = false

        commitAll()

        assertThat(fakeNative.submittedTransactions.single().transforms)
            .containsExactly(entity, poseB to scaleA)
    }

    @Test
    fun commit_transactionNotSubmitted_commitsItsChangesBeforeNewerOnes() {
        val committer = OpenXrSceneCommitter(fakeNative, executor, maxEntitiesPerTransaction = 1)
        fakeNative.simulateSubmitFailure = true
        committer.edit { setTransform(entity, poseA, scaleA) }
        fakeNative.onSubmitSceneTransaction = {
            fakeNative.onSubmitSceneTransaction = null
            committer.edit { setTransform(otherEntity, poseB, scaleA) }
        }
        advanceTimeBy(COALESCING_DELAY_MS)
        fakeNative.simulateSubmitFailure = false

        commitAll()

        assertThat(fakeNative.submittedTransactions.map { it.transforms.keys.single() })
            .containsExactly(entity, otherEntity)
            .inOrder()
    }

    @Test
    fun commit_changeThatTheRuntimeRejects_isSubmittedAndNotRetried() {
        val unknownEntity = 999L
        committer.edit { setTransform(unknownEntity, poseA, scaleA) }

        commitAll()

        assertThat(fakeNative.submittedTransactions.single().transforms).isEmpty()
        assertThat(fakeNative.transactionCreateAttempts).isEqualTo(1)
    }

    @Test
    fun commit_rejectedChangeToOneEntity_commitsTheOtherEntities() {
        committer.edit {
            setTransform(999L, poseA, scaleA)
            setTransform(entity, poseB, scaleA)
        }

        commitAll()

        assertThat(fakeNative.entityTransforms).containsExactly(entity, poseB to scaleA)
    }

    @Test
    fun commit_rejectedParent_skipsTheTransformRelativeToIt() {
        committer.edit {
            setParent(entity, 999L)
            setTransform(entity, poseA, scaleA)
        }

        commitAll()

        val transaction = fakeNative.submittedTransactions.single()
        assertThat(transaction.parents).isEmpty()
        assertThat(transaction.transforms).isEmpty()
    }

    @Test
    fun edit_sameValueAfterTheRuntimeRejectedIt_triesAgain() {
        val laterEntity = 999L
        committer.edit { setParent(entity, laterEntity) }
        commitAll()
        fakeNative.createdEntities.add(laterEntity)

        committer.edit { setParent(entity, laterEntity) }
        commitAll()

        assertThat(fakeNative.entityParents[entity]).isEqualTo(laterEntity)
    }

    @Test
    fun commit_moreEntitiesThanATransactionHolds_splitsThemAcrossCommits() {
        val entities = List(MAX_ENTITIES_PER_TRANSACTION * 2 + 3) { fakeNative.createSceneEntity() }
        committer.edit { entities.forEach { setTransform(it, poseA, scaleA) } }

        val submitTimes =
            timesWhenIncreased(
                durationMs = 100L,
                count = { fakeNative.submittedTransactions.size },
            )

        assertThat(fakeNative.submittedTransactions.map { it.transforms.size })
            .containsExactly(MAX_ENTITIES_PER_TRANSACTION, MAX_ENTITIES_PER_TRANSACTION, 3)
            .inOrder()
        assertThat(submitTimes.zipWithNext { first, next -> next - first })
            .containsExactly(MIN_COMMIT_INTERVAL_MS, MIN_COMMIT_INTERVAL_MS)
        assertThat(fakeNative.entityTransforms.keys).containsExactlyElementsIn(entities)
    }

    @Test
    fun commit_entitiesThatWaitedLongest_commitFirst() {
        val committer = OpenXrSceneCommitter(fakeNative, executor, maxEntitiesPerTransaction = 1)
        committer.edit { setTransform(otherEntity, poseA, scaleA) }
        committer.edit { setTransform(entity, poseA, scaleA) }
        committer.edit { setTransform(otherEntity, poseB, scaleA) }

        commitAll()

        assertThat(fakeNative.submittedTransactions.map { it.transforms.keys.single() })
            .containsExactly(otherEntity, entity)
            .inOrder()
        assertThat(fakeNative.entityTransforms[otherEntity]).isEqualTo(poseB to scaleA)
    }

    @Test
    fun commit_entityThatKeepsChanging_doesNotStarveTheOthers() {
        val committer = OpenXrSceneCommitter(fakeNative, executor, maxEntitiesPerTransaction = 1)
        committer.edit { setTransform(entity, poseA, scaleA) }
        advanceTimeBy(1L)
        committer.edit { setTransform(otherEntity, poseB, scaleA) }

        for (time in 2L..30L) {
            committer.edit { setTransform(entity, Pose(Vector3(time.toFloat(), 0f, 0f)), scaleA) }
            advanceTimeBy(1L)
        }

        assertThat(fakeNative.submittedTransactions[1].transforms.keys).containsExactly(otherEntity)
    }

    @Test
    fun commit_withOneTransactionFreedPerFrame_commitsEveryFrameOnce() {
        fakeNative.transactionPoolSize = 1
        val entities = List(4) { fakeNative.createSceneEntity() }

        val commitsPerFrame =
            (1..60).map { frame ->
                val submittedBefore = fakeNative.submittedTransactions.size
                val pose = Pose(Vector3(frame.toFloat(), 0f, 0f))
                committer.edit { entities.forEach { setTransform(it, pose, scaleA) } }

                advanceTimeBy(FRAME_DURATION_MS)

                val commits = fakeNative.submittedTransactions.size - submittedBefore
                // The runtime applies the frame's transaction, which frees its slot.
                fakeNative.completeSubmittedTransactions()
                commits
            }

        assertThat(commitsPerFrame).isEqualTo(List(60) { 1 })
        val lastTransform = Pose(Vector3(60f, 0f, 0f)) to scaleA
        assertThat(fakeNative.entityTransforms)
            .containsAtLeastEntriesIn(entities.associateWith { lastTransform })
    }

    @Test
    fun commit_randomChangesWithASmallTransactionPool_convergeToTheLatestValues() {
        val random = Random(42)
        fakeNative.transactionPoolSize = 3
        val entities = List(12) { fakeNative.createSceneEntity() }
        val parents = entities + listOf(rootEntity, INVALID_HANDLE)
        val expectedTransforms = HashMap<Long, Pair<Pose, Vector3>>()
        val expectedParents = HashMap<Long, Long>()
        val submitTimes = mutableListOf<Long>()
        var time = 0L

        repeat(1_000) {
            val target = entities.random(random)
            when (random.nextInt(4)) {
                0,
                1,
                2 -> {
                    val pose = Pose(Vector3(random.nextFloat(), random.nextFloat(), 0f))
                    val scale = Vector3(random.nextFloat() + 0.5f, 1f, 1f)
                    committer.edit { setTransform(target, pose, scale) }
                    expectedTransforms[target] = pose to scale
                }
                else -> {
                    val parent = parents.filter { it != target }.random(random)
                    committer.edit { setParent(target, parent) }
                    if (parent == INVALID_HANDLE) {
                        expectedParents.remove(target)
                    } else {
                        expectedParents[target] = parent
                    }
                }
            }
            repeat(random.nextInt(4)) {
                val submittedBefore = fakeNative.submittedTransactions.size
                advanceTimeBy(1L)
                time++
                if (fakeNative.submittedTransactions.size > submittedBefore) submitTimes += time
            }
            // Now and then, the runtime applies the submitted transactions, which frees their
            // slots.
            if (random.nextInt(8) == 0) fakeNative.completeSubmittedTransactions()
        }
        repeat(50) {
            advanceTimeBy(MAX_RETRY_DELAY_MS)
            fakeNative.completeSubmittedTransactions()
        }

        assertThat(fakeNative.entityTransforms).containsExactlyEntriesIn(expectedTransforms)
        assertThat(fakeNative.entityParents).containsExactlyEntriesIn(expectedParents)
        assertThat(fakeNative.cancelledTransactions).isEmpty()
        assertThat(submitTimes.zipWithNext { first, next -> next - first }.min())
            .isAtLeast(MIN_COMMIT_INTERVAL_MS)
    }

    @Test
    fun destroyEntity_destroysTheNativeEntityAfterCommittingTheEarlierChanges() {
        committer.edit { setParent(otherEntity, entity) }
        commitAll()
        var destroyedEntitiesWhenSubmitted: List<Long>? = null
        fakeNative.onSubmitSceneTransaction = {
            destroyedEntitiesWhenSubmitted = fakeNative.destroyedEntities.toList()
        }

        // The entity still waits to commit a change of its own when its child moves elsewhere.
        committer.edit { setTransform(entity, poseA, scaleA) }
        committer.edit { setParent(otherEntity, rootEntity) }
        committer.edit { destroyEntity(entity) }
        commitAll()

        assertThat(destroyedEntitiesWhenSubmitted).isEmpty()
        assertThat(fakeNative.destroyedEntities).containsExactly(entity)
        assertThat(fakeNative.entityParents[otherEntity]).isEqualTo(rootEntity)
    }

    @Test
    fun destroyEntity_waitsForEarlierChangesThatDidNotFitInACommit() {
        val committer = OpenXrSceneCommitter(fakeNative, executor, maxEntitiesPerTransaction = 1)
        val thirdEntity = fakeNative.createSceneEntity()
        committer.edit { setParent(otherEntity, rootEntity) }
        committer.edit { setParent(thirdEntity, rootEntity) }
        committer.edit { destroyEntity(entity) }

        advanceTimeBy(COALESCING_DELAY_MS)
        assertThat(fakeNative.entityParents).containsExactly(otherEntity, rootEntity)
        assertThat(fakeNative.destroyedEntities).isEmpty()

        advanceTimeBy(MIN_COMMIT_INTERVAL_MS)
        assertThat(fakeNative.entityParents[thirdEntity]).isEqualTo(rootEntity)
        assertThat(fakeNative.destroyedEntities).containsExactly(entity)
    }

    @Test
    fun destroyEntity_ofAnEntityThatWasWaiting_waitsForTheChangesMadeBeforeIt() {
        val committer = OpenXrSceneCommitter(fakeNative, executor, maxEntitiesPerTransaction = 1)
        val thirdEntity = fakeNative.createSceneEntity()
        committer.edit { setTransform(entity, poseA, scaleA) }
        committer.edit { setTransform(otherEntity, poseA, scaleA) }
        committer.edit { setParent(thirdEntity, rootEntity) }
        committer.edit { destroyEntity(entity) }

        advanceTimeBy(COALESCING_DELAY_MS)
        assertThat(fakeNative.destroyedEntities).isEmpty()

        advanceTimeBy(MIN_COMMIT_INTERVAL_MS)
        assertThat(fakeNative.entityParents[thirdEntity]).isEqualTo(rootEntity)
        assertThat(fakeNative.destroyedEntities).containsExactly(entity)
    }

    @Test
    fun destroyEntity_dropsTheUncommittedChangesToIt() {
        committer.edit { setTransform(entity, poseA, scaleA) }
        committer.edit { destroyEntity(entity) }

        commitAll()

        assertThat(fakeNative.transactionCreateAttempts).isEqualTo(0)
        assertThat(fakeNative.destroyedEntities).containsExactly(entity)
    }

    @Test
    fun destroyEntity_ignoresLaterChangesToIt() {
        committer.edit { destroyEntity(entity) }
        committer.edit { setTransform(entity, poseA, scaleA) }

        commitAll()

        assertThat(fakeNative.transactionCreateAttempts).isEqualTo(0)
        assertThat(fakeNative.destroyedEntities).containsExactly(entity)
    }

    @Test
    fun destroyEntity_ignoresChangesMadeToItDuringTheCommit() {
        committer.edit {
            setTransform(otherEntity, poseA, scaleA)
            destroyEntity(entity)
        }
        fakeNative.onSubmitSceneTransaction = {
            fakeNative.onSubmitSceneTransaction = null
            committer.edit { setTransform(entity, poseB, scaleA) }
        }

        commitAll()

        assertThat(fakeNative.destroyedEntities).containsExactly(entity)
        assertThat(fakeNative.submittedTransactions).hasSize(1)
        assertThat(fakeNative.entityTransforms).containsExactly(otherEntity, poseA to scaleA)
    }

    @Test
    fun destroyEntity_withOneTransactionFreedPerFrameAndContinuousChanges_isNotStarved() {
        fakeNative.transactionPoolSize = 1
        committer.edit {
            setTransform(otherEntity, poseA, scaleA)
            destroyEntity(entity)
        }

        repeat(4) { frame ->
            val pose = Pose(Vector3(frame.toFloat(), 0f, 0f))
            committer.edit { setTransform(otherEntity, pose, scaleA) }
            advanceTimeBy(FRAME_DURATION_MS)
            fakeNative.completeSubmittedTransactions()
        }

        assertThat(fakeNative.destroyedEntities).containsExactly(entity)
        assertThat(fakeNative.entityTransforms[otherEntity])
            .isEqualTo(Pose(Vector3(3f, 0f, 0f)) to scaleA)
    }

    @Test
    fun destroyEntity_duringACommit_destroysItAfterTheCommit() {
        committer.edit { setTransform(entity, poseA, scaleA) }
        fakeNative.onSubmitSceneTransaction = {
            fakeNative.onSubmitSceneTransaction = null
            committer.edit {
                setTransform(entity, poseB, scaleA)
                destroyEntity(entity)
            }
        }

        commitAll()

        assertThat(fakeNative.submittedTransactions.single().transforms)
            .containsExactly(entity, poseA to scaleA)
        assertThat(fakeNative.destroyedEntities).containsExactly(entity)
    }

    @Test
    fun destroyEntity_whileNoTransactionIsFree_destroysItOnceOneIs() {
        fakeNative.simulateTransactionUnavailable = true
        committer.edit { destroyEntity(entity) }
        advanceTimeBy(100L)
        assertThat(fakeNative.destroyedEntities).isEmpty()

        fakeNative.simulateTransactionUnavailable = false
        commitAll()

        assertThat(fakeNative.destroyedEntities).containsExactly(entity)
    }

    @Test
    fun destroyEntity_manyEntitiesWithASmallTransactionPool_destroysThemAll() {
        fakeNative.transactionPoolSize = 3
        val entities = List(20) { fakeNative.createSceneEntity() }
        committer.edit { entities.forEach { destroyEntity(it) } }

        repeat(100) {
            advanceTimeBy(FRAME_DURATION_MS)
            fakeNative.completeSubmittedTransactions()
        }

        assertThat(fakeNative.destroyedEntities).containsExactlyElementsIn(entities)
    }

    @Test
    fun destroyEntity_thatKeepsFailing_keepsTryingUntilItSucceeds() {
        fakeNative.simulateDestroyFailure = true
        committer.edit { destroyEntity(entity) }
        // Many more tries than it takes for a transaction to be free.
        commitAll()
        assertThat(fakeNative.destroyedEntities).isEmpty()

        fakeNative.simulateDestroyFailure = false
        commitAll()

        assertThat(fakeNative.destroyedEntities).containsExactly(entity)
    }

    @Test
    fun destroyEntity_thatKeepsFailing_doesNotHoldBackTheOtherChanges() {
        fakeNative.simulateDestroyFailure = true
        committer.edit { destroyEntity(entity) }
        committer.edit { setTransform(otherEntity, poseA, scaleA) }

        commitAll()

        assertThat(fakeNative.destroyedEntities).isEmpty()
        assertThat(fakeNative.entityTransforms).containsExactly(otherEntity, poseA to scaleA)
    }

    @Test
    fun destroyEntity_thenANewEntityWithTheSameHandle_startsAfresh() {
        committer.edit { setTransform(entity, poseA, scaleA) }
        commitAll()
        committer.edit { destroyEntity(entity) }
        commitAll()
        // The runtime can reuse the handle of a destroyed entity.
        fakeNative.createdEntities.add(entity)

        committer.edit { setTransform(entity, poseA, scaleA) }
        commitAll()

        assertThat(fakeNative.submittedTransactions).hasSize(2)
        assertThat(fakeNative.entityTransforms[entity]).isEqualTo(poseA to scaleA)
    }

    @Test
    fun shutdown_dropsUncommittedChangesAndCancelsTheCommit() {
        committer.edit { setTransform(entity, poseA, scaleA) }

        committer.shutdown()
        commitAll()

        assertThat(fakeNative.transactionCreateAttempts).isEqualTo(0)
        assertThat(executor.isEmpty()).isTrue()
    }

    @Test
    fun shutdown_duringACommit_schedulesNoMoreCommits() {
        committer.edit { setTransform(entity, poseA, scaleA) }
        fakeNative.onSubmitSceneTransaction = {
            committer.edit { setTransform(entity, poseB, scaleA) }
            committer.shutdown()
        }

        commitAll()

        assertThat(fakeNative.submittedTransactions).hasSize(1)
        assertThat(executor.isEmpty()).isTrue()
    }

    @Test
    fun edit_afterShutdown_isIgnored() {
        committer.shutdown()

        committer.edit { setTransform(entity, poseA, scaleA) }
        commitAll()

        assertThat(fakeNative.transactionCreateAttempts).isEqualTo(0)
        assertThat(executor.isEmpty()).isTrue()
    }

    @Test
    fun commit_afterTheNativeInstanceIsDestroyed_makesNoNativeCallsAndStops() {
        committer.edit { setTransform(entity, poseA, scaleA) }
        // The fake throws if called after this.
        fakeNative.destroy()

        commitAll()
        committer.edit { setTransform(entity, poseB, scaleA) }

        assertThat(fakeNative.transactionCreateAttempts).isEqualTo(0)
        assertThat(executor.isEmpty()).isTrue()
    }

    @Test
    fun edit_afterTheExecutorShutDown_doesNotThrow() {
        executor.shutdown()

        committer.edit { setTransform(entity, poseA, scaleA) }
        committer.edit { setTransform(entity, poseB, scaleA) }

        assertThat(fakeNative.transactionCreateAttempts).isEqualTo(0)
    }

    @Test
    fun commit_whenTheExecutorShutsDownDuringIt_doesNotThrow() {
        committer.edit { setTransform(entity, poseA, scaleA) }
        fakeNative.onSubmitSceneTransaction = { executor.shutdown() }

        advanceTimeBy(COALESCING_DELAY_MS)
        committer.edit { setTransform(entity, poseB, scaleA) }

        assertThat(fakeNative.submittedTransactions).hasSize(1)
        assertThat(executor.isEmpty()).isTrue()
    }

    @Test
    fun commit_whenANativeCallThrows_reportsItAndStopsCommitting() {
        val error = IllegalStateException("test")
        committer.edit { setTransform(entity, poseA, scaleA) }
        fakeNative.onSubmitSceneTransaction = { throw error }

        val reported = recordUncaughtExceptions {
            val thrown =
                assertFailsWith<IllegalStateException> { advanceTimeBy(COALESCING_DELAY_MS) }
            assertThat(thrown).isSameInstanceAs(error)
        }
        fakeNative.onSubmitSceneTransaction = null
        committer.edit { setTransform(entity, poseB, scaleA) }
        commitAll()

        assertThat(reported.single()).hasCauseThat().isSameInstanceAs(error)
        assertThat(fakeNative.submittedTransactions).isEmpty()
        assertThat(executor.isEmpty()).isTrue()
    }

    @Test
    fun edit_fromManyThreads_commitsTheLatestValueOfEachEntity() {
        val realExecutor = Executors.newSingleThreadScheduledExecutor()
        try {
            val committer = OpenXrSceneCommitter(fakeNative, realExecutor)
            val entities = List(MAX_ENTITIES_PER_TRANSACTION * 2) { fakeNative.createSceneEntity() }
            val lastPose = Pose(Vector3(200f, 0f, 0f))

            val threads = entities.map { entity ->
                thread(isDaemon = true) {
                    for (i in 1..200) {
                        val pose = Pose(Vector3(i.toFloat(), 0f, 0f))
                        committer.edit { setTransform(entity, pose, scaleA) }
                    }
                }
            }
            threads.forEach { it.join(/* millis= */ 5_000L) }
            assertThat(threads.filter { it.isAlive }).isEmpty()

            // Reads the fake on the executor, where the commits change it, a little later each
            // time.
            var transforms: Map<Long, Pair<Pose, Vector3>>
            var reads = 0
            do {
                transforms =
                    realExecutor
                        .schedule(
                            Callable { fakeNative.entityTransforms.toMap() },
                            MIN_COMMIT_INTERVAL_MS,
                            TimeUnit.MILLISECONDS,
                        )
                        .get(1L, TimeUnit.SECONDS)
            } while (entities.any { transforms[it] != lastPose to scaleA } && ++reads < 1_000)
            assertThat(transforms)
                .containsAtLeastEntriesIn(entities.associateWith { lastPose to scaleA })
        } finally {
            realExecutor.shutdownNow()
        }
    }
}
