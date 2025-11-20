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

package androidx.camera.camera2.pipe.compat

import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.ConcurrentCameraGraphs
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ConcurrentSessionSequencersTest {

    private val cameraGraphId1 = CameraGraphId.nextId()
    private val cameraGraphId2 = CameraGraphId.nextId()
    private val cameraGraphId3 = CameraGraphId.nextId()
    private val cameraGraphId4 = CameraGraphId.nextId()

    private val cameraId1 = CameraId("1")
    private val cameraId2 = CameraId("2")

    // Tests for ConcurrentSessionSequencers
    @Test
    fun createConcurrentCameraGraphs_withSingleCamera_fails() {
        assertThrows<IllegalStateException> {
            ConcurrentCameraGraphs(setOf(cameraGraphId1), setOf(cameraId1))
        }
    }

    @Test
    fun getSequencer_returnsSameSequencer_forConcurrentGraphs() {
        val sequencers = ConcurrentSessionSequencers()
        val concurrentGraphs =
            ConcurrentCameraGraphs(
                setOf(cameraGraphId1, cameraGraphId2),
                setOf(cameraId1, cameraId2),
            )

        val sequencer1 = sequencers.getSequencer(cameraGraphId1, concurrentGraphs)
        val sequencer2 = sequencers.getSequencer(cameraGraphId2, concurrentGraphs)

        assertThat(sequencer1).isSameInstanceAs(sequencer2)
    }

    @Test
    fun getSequencer_removesSequencer_afterAllGraphsRetrieveIt() {
        val sequencers = ConcurrentSessionSequencers()
        val concurrentGraphs =
            ConcurrentCameraGraphs(
                setOf(cameraGraphId1, cameraGraphId2),
                setOf(cameraId1, cameraId2),
            )

        val firstSequencer = sequencers.getSequencer(cameraGraphId1, concurrentGraphs)
        sequencers.getSequencer(cameraGraphId2, concurrentGraphs)

        // Now that both have retrieved it, the next call should create a new one.
        val secondSequencer = sequencers.getSequencer(cameraGraphId1, concurrentGraphs)

        assertThat(firstSequencer).isNotSameInstanceAs(secondSequencer)
    }

    @Test
    fun getSequencer_handlesMultipleConcurrentGroups() {
        val sequencers = ConcurrentSessionSequencers()
        val group12 =
            ConcurrentCameraGraphs(
                setOf(cameraGraphId1, cameraGraphId2),
                setOf(cameraId1, cameraId2),
            )
        val group34 =
            ConcurrentCameraGraphs(
                setOf(cameraGraphId3, cameraGraphId4),
                setOf(cameraId1, cameraId2),
            )

        val sequencer1 = sequencers.getSequencer(cameraGraphId1, group12)
        val sequencer3 = sequencers.getSequencer(cameraGraphId3, group34)

        assertThat(sequencer1).isNotSameInstanceAs(sequencer3)

        val sequencer2 = sequencers.getSequencer(cameraGraphId2, group12)
        val sequencer4 = sequencers.getSequencer(cameraGraphId4, group34)

        assertThat(sequencer1).isSameInstanceAs(sequencer2)
        assertThat(sequencer3).isSameInstanceAs(sequencer4)
    }

    // Tests for SessionSequencer
    @Test
    fun sessionSequencer_awaitSessionLock_locksMutex() = runTest {
        val concurrentSequencer = ConcurrentSessionSequencer()
        val sessionSequencer = SessionSequencer(concurrentSequencer)

        assertThat(concurrentSequencer.sharedMutex.isLocked).isFalse()

        val job = launch { sessionSequencer.awaitSessionLock() }
        // Advance to allow the job to execute and lock the mutex
        advanceTimeBy(1)

        assertThat(concurrentSequencer.sharedMutex.isLocked).isTrue()

        sessionSequencer.release()
        assertThat(concurrentSequencer.sharedMutex.isLocked).isFalse()
        job.cancel()
    }

    @Test
    fun sessionSequencer_multipleReleases_unlockOnlyOnce() = runTest {
        val concurrentSequencer = ConcurrentSessionSequencer()
        val sessionSequencer = SessionSequencer(concurrentSequencer)

        val job = launch { sessionSequencer.awaitSessionLock() }
        advanceTimeBy(1)
        assertThat(concurrentSequencer.sharedMutex.isLocked).isTrue()

        sessionSequencer.release()
        assertThat(concurrentSequencer.sharedMutex.isLocked).isFalse()

        // Second release should not cause any issues
        sessionSequencer.release()
        assertThat(concurrentSequencer.sharedMutex.isLocked).isFalse()

        // We should be able to acquire the lock again
        assertThat(concurrentSequencer.sharedMutex.tryLock()).isTrue()
        concurrentSequencer.sharedMutex.unlock()
        job.cancel()
    }

    @Test
    fun sessionSequencer_secondAwait_doesNotRelock() = runTest {
        val concurrentSequencer = ConcurrentSessionSequencer()
        val sessionSequencer = SessionSequencer(concurrentSequencer)

        // First await locks and sets state to CREATING
        val job1 = launch { sessionSequencer.awaitSessionLock() }
        advanceTimeBy(1)
        assertThat(concurrentSequencer.sharedMutex.isLocked).isTrue()

        // Second await will suspend waiting for the lock, then when it acquires it,
        // it will see the state is not PENDING and will immediately unlock.
        val job2 = launch { sessionSequencer.awaitSessionLock() }
        advanceTimeBy(1)

        // Release the first lock
        sessionSequencer.release()
        advanceTimeBy(1)

        // The mutex should be unlocked now, because job2 acquired and immediately released it.
        assertThat(concurrentSequencer.sharedMutex.isLocked).isFalse()

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun multipleSessionSequencers_areMutuallyExclusive() = runTest {
        val concurrentSequencer = ConcurrentSessionSequencer()
        val sessionSequencer1 = SessionSequencer(concurrentSequencer)
        val sessionSequencer2 = SessionSequencer(concurrentSequencer)
        val activeCounter = atomic(0)
        var job1Completed = false
        var job2Completed = false

        launch {
            sessionSequencer1.awaitSessionLock()
            assertThat(activeCounter.incrementAndGet()).isEqualTo(1)
            delay(100)
            assertThat(activeCounter.value).isEqualTo(1)
            activeCounter.decrementAndGet()
            sessionSequencer1.release()
            job1Completed = true
        }

        launch {
            sessionSequencer2.awaitSessionLock()
            assertThat(activeCounter.incrementAndGet()).isEqualTo(1)
            delay(100)
            assertThat(activeCounter.value).isEqualTo(1)
            activeCounter.decrementAndGet()
            sessionSequencer2.release()
            job2Completed = true
        }

        advanceTimeBy(300) // Enough time for both to complete sequentially

        assertThat(job1Completed).isTrue()
        assertThat(job2Completed).isTrue()
        assertThat(activeCounter.value).isEqualTo(0)
    }
}
