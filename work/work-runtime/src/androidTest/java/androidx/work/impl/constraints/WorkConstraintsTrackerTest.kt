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
import androidx.work.impl.constraints.trackers.ConstraintTracker
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.testutils.TestConstraintController
import androidx.work.impl.testutils.TestConstraintTracker
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class WorkConstraintsTrackerTest {

    private val capturingCallback = CapturingWorkConstraintsCallback()

    @Test
    fun testReplace() {
        val tracker = TestConstraintTracker(true)
        val workConstraintsTracker = WorkConstraintsTracker(capturingCallback, tracker)
        workConstraintsTracker.replace(TEST_WORKSPECS.subList(0, 2))
        val (unconstrained1, _) = capturingCallback.consumeCurrent()
        assertThat(unconstrained1).containsExactly(TEST_WORKSPEC_IDS[0], TEST_WORKSPEC_IDS[1])
        workConstraintsTracker.replace(TEST_WORKSPECS.subList(1, 3))
        val (unconstrained2, _) = capturingCallback.consumeCurrent()
        assertThat(unconstrained2).containsExactly(TEST_WORKSPEC_IDS[1], TEST_WORKSPEC_IDS[2])
    }

    @Test
    fun testReset() {
        val tracker = TestConstraintTracker(true)
        val workConstraintsTracker = WorkConstraintsTracker(capturingCallback, tracker)
        workConstraintsTracker.replace(TEST_WORKSPECS)
        assertThat(tracker.isTracking).isTrue()
        workConstraintsTracker.reset()
        assertThat(tracker.isTracking).isFalse()
    }

    @Test
    fun testOnConstraintMet_allConstraintsMet() {
        val tracker = TestConstraintTracker()
        val workConstraintsTracker = WorkConstraintsTracker(capturingCallback, tracker)
        workConstraintsTracker.replace(TEST_WORKSPECS)
        val (_, constrained) = capturingCallback.consumeCurrent()
        assertThat(constrained).isEqualTo(TEST_WORKSPEC_IDS)
        tracker.state = true
        val (unconstrained, _) = capturingCallback.consumeCurrent()
        assertThat(unconstrained).isEqualTo(TEST_WORKSPEC_IDS)
    }

    @Test
    fun testOnConstraintMet_allConstraintsMet_subList() {
        val tracker1 = TestConstraintTracker()
        val tracker2 = TestConstraintTracker()
        val controller1 = TestConstraintController(tracker1, TEST_WORKSPEC_IDS.subList(0, 2))
        val controller2 = TestConstraintController(tracker2, TEST_WORKSPEC_IDS.subList(2, 3))
        val workConstraintsTracker = WorkConstraintsTrackerImpl(
            capturingCallback,
            arrayOf(controller1, controller2)
        )
        workConstraintsTracker.replace(TEST_WORKSPECS)
        capturingCallback.consumeCurrent()
        tracker1.state = true
        val (unconstrained, _) = capturingCallback.consumeCurrent()
        assertThat(unconstrained).containsExactly(TEST_WORKSPEC_IDS[0], TEST_WORKSPEC_IDS[1])
    }

    @Test
    fun testOnConstraintMet_allConstraintsNotMet() {
        val tracker1 = TestConstraintTracker()
        val tracker2 = TestConstraintTracker()
        val workConstraintsTracker = WorkConstraintsTracker(capturingCallback, tracker1, tracker2)
        workConstraintsTracker.replace(TEST_WORKSPECS)
        capturingCallback.consumeCurrent()
        tracker1.state = true
        val (unconstrained, _) = capturingCallback.consumeCurrent()
        // only one constraint is resolved, so unconstrained is empty list
        assertThat(unconstrained).isEqualTo(emptyList<String>())
    }

    @Test
    fun testOnConstraintNotMet() {
        val tracker1 = TestConstraintTracker(true)
        val tracker2 = TestConstraintTracker(true)
        val workConstraintsTracker = WorkConstraintsTracker(capturingCallback, tracker1, tracker2)
        workConstraintsTracker.replace(TEST_WORKSPECS)
        val (unconstrained, _) = capturingCallback.consumeCurrent()
        assertThat(unconstrained).isEqualTo(TEST_WORKSPEC_IDS)
        tracker1.state = false
        val (_, constrained) = capturingCallback.consumeCurrent()
        assertThat(constrained).isEqualTo(TEST_WORKSPEC_IDS)
    }
}

private val TEST_WORKSPECS = listOf(
    WorkSpec("A", "Worker1"),
    WorkSpec("B", "Worker2"),
    WorkSpec("C", "Worker3"),
)
private val TEST_WORKSPEC_IDS = TEST_WORKSPECS.map { it.id }

private fun WorkConstraintsTracker(
    callback: WorkConstraintsCallback,
    vararg trackers: ConstraintTracker<Boolean>
): WorkConstraintsTrackerImpl {
    val controllers = trackers.map { TestConstraintController(it, TEST_WORKSPEC_IDS) }
    return WorkConstraintsTrackerImpl(callback, controllers.toTypedArray())
}

private class CapturingWorkConstraintsCallback(
    var unconstrainedWorkSpecIds: List<String>? = null,
    var constrainedWorkSpecIds: List<String>? = null,
) : WorkConstraintsCallback {
    override fun onAllConstraintsMet(workSpecs: List<WorkSpec>) {
        unconstrainedWorkSpecIds = workSpecs.map { it.id }
    }

    override fun onAllConstraintsNotMet(workSpecs: List<WorkSpec>) {
        constrainedWorkSpecIds = workSpecs.map { it.id }
    }

    fun consumeCurrent(): Pair<List<String>?, List<String>?> {
        val result = unconstrainedWorkSpecIds to constrainedWorkSpecIds
        unconstrainedWorkSpecIds = null
        constrainedWorkSpecIds = null
        return result
    }
}