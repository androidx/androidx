/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.work.impl.constraints

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.impl.constraints.ConstraintsState.ConstraintsMet
import androidx.work.impl.constraints.trackers.ConstraintTracker
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.testutils.ConstraintsNotMet
import androidx.work.impl.testutils.TestConstraintController
import androidx.work.impl.testutils.TestConstraintTracker
import androidx.work.testutils.launchTester
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class WorkConstraintsTrackerTest {

    @Test
    fun testOnConstraintMet_allConstraintsMet() = runBlocking {
        val tracker = TestConstraintTracker()
        val workConstraintsTracker = WorkConstraintsTracker(tracker)
        val tester = launchTester(workConstraintsTracker.track(TEST_WORKSPECS[0]))
        assertThat(tester.awaitNext()).isEqualTo(ConstraintsNotMet)
        tracker.constraintState = true
        assertThat(tester.awaitNext()).isEqualTo(ConstraintsMet)
    }

    @Test
    fun testOnConstraintMet_allConstraintsMet_subList() = runBlocking {
        val tracker1 = TestConstraintTracker()
        val tracker2 = TestConstraintTracker()
        val controller1 = TestConstraintController(tracker1, TEST_WORKSPEC_IDS.subList(0, 2))
        val controller2 = TestConstraintController(tracker2, TEST_WORKSPEC_IDS.subList(1, 3))
        val workConstraintsTracker = WorkConstraintsTracker(listOf(controller1, controller2))
        val testerO = launchTester(workConstraintsTracker.track(TEST_WORKSPECS[0]))
        val tester1 = launchTester(workConstraintsTracker.track(TEST_WORKSPECS[1]))
        val tester2 = launchTester(workConstraintsTracker.track(TEST_WORKSPECS[2]))
        assertThat(testerO.awaitNext()).isEqualTo(ConstraintsNotMet)
        assertThat(tester1.awaitNext()).isEqualTo(ConstraintsNotMet)
        assertThat(tester2.awaitNext()).isEqualTo(ConstraintsNotMet)

        val deferred0 = async { testerO.awaitNext() }
        val deferred1 = async { tester1.awaitNext() }
        val deferred2 = async { tester2.awaitNext() }
        tracker1.constraintState = true
        assertThat(deferred0.await()).isEqualTo(ConstraintsMet)
        assertThat(deferred1.isCompleted).isFalse()
        assertThat(deferred2.isCompleted).isFalse()
        tracker2.constraintState = true
        assertThat(deferred1.await()).isEqualTo(ConstraintsMet)
        assertThat(deferred2.await()).isEqualTo(ConstraintsMet)
    }

    @Test
    fun testViaCallback() = runBlocking {
        val tracker = TestConstraintTracker()
        val workConstraintsTracker = WorkConstraintsTracker(tracker)
        val executor = Executors.newSingleThreadExecutor()
        val callback = Callback()
        val job = workConstraintsTracker.listen(TEST_WORKSPECS[0],
            executor.asCoroutineDispatcher(), callback)
        assertThat(callback.channel.receive()).isEqualTo(ConstraintsNotMet)
        tracker.constraintState = true
        assertThat(callback.channel.receive()).isEqualTo(ConstraintsMet)
        assertThat(tracker.isTracking).isTrue()
        job.cancelAndJoin()
        assertThat(tracker.isTracking).isFalse()
    }

    @Test
    fun testAreAllConstraintsMet() {
        val tracker1 = TestConstraintTracker()
        val tracker2 = TestConstraintTracker()
        val controller1 = TestConstraintController(tracker1, TEST_WORKSPEC_IDS.subList(0, 2))
        val controller2 = TestConstraintController(tracker2, TEST_WORKSPEC_IDS.subList(1, 3))
        val workConstraintsTracker = WorkConstraintsTracker(listOf(controller1, controller2))
        assertThat(workConstraintsTracker.areAllConstraintsMet(TEST_WORKSPECS[0])).isFalse()
        assertThat(workConstraintsTracker.areAllConstraintsMet(TEST_WORKSPECS[1])).isFalse()
        assertThat(workConstraintsTracker.areAllConstraintsMet(TEST_WORKSPECS[2])).isFalse()
        tracker1.constraintState = true
        assertThat(workConstraintsTracker.areAllConstraintsMet(TEST_WORKSPECS[0])).isTrue()
        assertThat(workConstraintsTracker.areAllConstraintsMet(TEST_WORKSPECS[1])).isFalse()
        assertThat(workConstraintsTracker.areAllConstraintsMet(TEST_WORKSPECS[2])).isFalse()
        tracker2.constraintState = true
        assertThat(workConstraintsTracker.areAllConstraintsMet(TEST_WORKSPECS[0])).isTrue()
        assertThat(workConstraintsTracker.areAllConstraintsMet(TEST_WORKSPECS[1])).isTrue()
        assertThat(workConstraintsTracker.areAllConstraintsMet(TEST_WORKSPECS[2])).isTrue()
        tracker1.constraintState = false
        assertThat(workConstraintsTracker.areAllConstraintsMet(TEST_WORKSPECS[0])).isFalse()
        assertThat(workConstraintsTracker.areAllConstraintsMet(TEST_WORKSPECS[1])).isFalse()
        assertThat(workConstraintsTracker.areAllConstraintsMet(TEST_WORKSPECS[2])).isTrue()
    }

    class Callback : OnConstraintsStateChangedListener {
        val channel = Channel<ConstraintsState>(10)
        override fun onConstraintsStateChanged(workSpec: WorkSpec, state: ConstraintsState) {
            channel.trySend(state)
        }
    }
}

private val TEST_WORKSPECS = listOf(
    WorkSpec("A", "Worker1"),
    WorkSpec("B", "Worker2"),
    WorkSpec("C", "Worker3"),
)
private val TEST_WORKSPEC_IDS = TEST_WORKSPECS.map { it.id }

internal fun WorkConstraintsTracker(
    vararg trackers: ConstraintTracker<Boolean>
): WorkConstraintsTracker {
    val controllers = trackers.map { TestConstraintController(it, TEST_WORKSPEC_IDS) }
    return WorkConstraintsTracker(controllers)
}
