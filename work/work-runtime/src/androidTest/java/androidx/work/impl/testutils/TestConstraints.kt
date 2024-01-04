/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.work.impl.testutils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo.Companion.STOP_REASON_PREEMPT
import androidx.work.impl.constraints.ConstraintsState
import androidx.work.impl.constraints.controllers.ConstraintController
import androidx.work.impl.constraints.trackers.ConstraintTracker
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor

class TestConstraintTracker(
    initialState: Boolean = false,
    context: Context = ApplicationProvider.getApplicationContext(),
    taskExecutor: TaskExecutor = InstantWorkTaskExecutor(),
) : ConstraintTracker<Boolean>(context, taskExecutor) {
    var isTracking = false

    // some awkwardness because "this.state = ..." is overridden
    // with `readSystemState` when first listener is added.
    // so we have a separate state returns that we return from `readSystemState` too.
    var constraintState: Boolean = initialState
        set(value) {
            state = value
            field = value
        }

    override fun startTracking() {
        isTracking = true
    }

    override fun stopTracking() {
        isTracking = false
    }

    override fun readSystemState() = constraintState
}

class TestConstraintController(
    tracker: ConstraintTracker<Boolean>,
    private val constrainedIds: List<String>
) : ConstraintController<Boolean>(tracker) {
    // using obscure stop reason for test purposes
    override val reason = STOP_REASON_PREEMPT
    override fun hasConstraint(workSpec: WorkSpec) = workSpec.id in constrainedIds
    override fun isConstrained(value: Boolean) = !value
}

val ConstraintsNotMet = ConstraintsState.ConstraintsNotMet(STOP_REASON_PREEMPT)
