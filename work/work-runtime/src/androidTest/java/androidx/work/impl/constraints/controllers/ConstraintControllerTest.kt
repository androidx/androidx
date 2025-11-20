/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.work.impl.constraints.controllers

import android.app.job.JobParameters.STOP_REASON_CONSTRAINT_DEVICE_IDLE
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.impl.constraints.ConstraintsState.ConstraintsMet
import androidx.work.impl.constraints.ConstraintsState.ConstraintsNotMet
import androidx.work.impl.constraints.trackers.ConstraintTracker
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor
import androidx.work.testutils.launchTester
import androidx.work.worker.TestWorker
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConstraintControllerTest {
    private val tracker = FakeConstraintTracker()
    private val testIdleController = TestDeviceIdleConstraintController(tracker)

    @Test
    @SmallTest
    fun testTrackViaFlow() = runBlocking {
        tracker.setDeviceActive()
        // not actually used by TestDeviceIdleConstraintController,
        // only NetworkRequestContoller uses it.
        val constraints = Constraints.NONE
        val tester = launchTester(testIdleController.track(constraints))
        assertThat(tester.awaitNext()).isEqualTo(ConstraintsNotMet)
        assertThat(tracker.tracking).isTrue()
        tracker.setDeviceIdle()
        assertThat(tester.awaitNext()).isEqualTo(ConstraintsMet)
        tracker.setDeviceActive()
        assertThat(tester.awaitNext()).isEqualTo(ConstraintsNotMet)
    }

    @Test
    @SmallTest
    fun testIsConstrained() {
        val constrained =
            OneTimeWorkRequest.Builder(TestWorker::class.java)
                .setConstraints(Constraints(requiresDeviceIdle = true))
                .build()
                .workSpec
        val unconstrained = OneTimeWorkRequest.Builder(TestWorker::class.java).build().workSpec
        tracker.setDeviceActive()
        assertThat(testIdleController.isCurrentlyConstrained(unconstrained)).isFalse()
        assertThat(testIdleController.isCurrentlyConstrained(constrained)).isTrue()
        tracker.setDeviceIdle()
        assertThat(testIdleController.isCurrentlyConstrained(unconstrained)).isFalse()
        assertThat(testIdleController.isCurrentlyConstrained(constrained)).isFalse()
    }

    private class TestDeviceIdleConstraintController(tracker: ConstraintTracker<Boolean>) :
        BaseConstraintController<Boolean>(tracker) {
        override val reason = WorkInfo.STOP_REASON_CONSTRAINT_DEVICE_IDLE

        override fun hasConstraint(workSpec: WorkSpec): Boolean {
            return workSpec.constraints.requiresDeviceIdle()
        }

        override fun isConstrained(value: Boolean) = !value
    }

    private class FakeConstraintTracker :
        ConstraintTracker<Boolean>(
            ApplicationProvider.getApplicationContext(),
            InstantWorkTaskExecutor(),
        ) {
        var tracking = false
        var deviceIdle = false

        override fun startTracking() {
            tracking = true
        }

        override fun stopTracking() {
            tracking = false
        }

        fun setDeviceActive() {
            deviceIdle = false
            state = false
        }

        fun setDeviceIdle() {
            deviceIdle = true
            state = true
        }

        override fun readSystemState() = deviceIdle
    }
}

private val ConstraintsNotMet: ConstraintsNotMet =
    ConstraintsNotMet(STOP_REASON_CONSTRAINT_DEVICE_IDLE)
