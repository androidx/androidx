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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
package androidx.work.testing

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.work.Worker
import androidx.work.impl.ExecutionListener
import androidx.work.impl.Scheduler
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.model.WorkSpec
import java.util.UUID

/**
 * A test scheduler that schedules unconstrained, non-timed workers. It intentionally does
 * not acquire any WakeLocks, instead trying to brute-force them as time allows before the process
 * gets killed.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TestScheduler(private val context: Context) : Scheduler, ExecutionListener {
    @GuardedBy("lock")
    private val pendingWorkStates = mutableMapOf<String, InternalWorkState>()
    @GuardedBy("lock")
    private val terminatedWorkIds = mutableSetOf<String>()
    private val lock = Any()

    override fun hasLimitedSchedulingSlots() = true

    override fun schedule(vararg workSpecs: WorkSpec) {
        if (workSpecs.isEmpty()) {
            return
        }
        val toSchedule = mutableMapOf<String, InternalWorkState>()
        synchronized(lock) {
            workSpecs.forEach {
                val state = pendingWorkStates.getOrPut(it.id) { InternalWorkState(it) }
                toSchedule[it.id] = state
            }
        }
        toSchedule.forEach { (id, state) -> scheduleInternal(id, state) }
    }

    override fun cancel(workSpecId: String) {
        // We don't need to keep track of cancelled workSpecs. This is because subsequent calls
        // to enqueue() will no-op because insertWorkSpec in WorkDatabase has a conflict
        // policy of @Ignore. So TestScheduler will _never_ be asked to schedule those
        // WorkSpecs.
        WorkManagerImpl.getInstance(context).stopWork(workSpecId)
        synchronized(lock) {
            val internalWorkState = pendingWorkStates[workSpecId]
            if (internalWorkState != null && !internalWorkState.isPeriodic) {
                // Don't remove PeriodicWorkRequests from the list of pending work states.
                // This is because we keep track of mPeriodDelayMet for PeriodicWorkRequests.
                // `mPeriodDelayMet` is set to `false` when `onExecuted()` is called as a result of a
                // successful run or a cancellation. That way subsequent calls to schedule() no-op
                // until a developer explicitly calls setPeriodDelayMet().
                pendingWorkStates.remove(workSpecId)
            }
        }
    }

    /**
     * Tells the [TestScheduler] to pretend that all constraints on the [Worker] with
     * the given `workSpecId` are met.
     *
     * @param workSpecId The [Worker]'s id
     * @throws IllegalArgumentException if `workSpecId` is not enqueued
     */
    fun setAllConstraintsMet(workSpecId: UUID) {
        val id = workSpecId.toString()
        val state: InternalWorkState
        synchronized(lock) {
            if (id in terminatedWorkIds) return
            val oldState = pendingWorkStates[id]
                ?: throw IllegalArgumentException("Work with id $workSpecId is not enqueued!")
            state = oldState.copy(constraintsMet = true)
            pendingWorkStates[id] = state
        }
        scheduleInternal(id, state)
    }

    /**
     * Tells the [TestScheduler] to pretend that the initial delay on the [Worker] with
     * the given `workSpecId` are met.
     *
     * @param workSpecId The [Worker]'s id
     * @throws IllegalArgumentException if `workSpecId` is not enqueued
     */
    fun setInitialDelayMet(workSpecId: UUID) {
        val id = workSpecId.toString()
        val state: InternalWorkState
        synchronized(lock) {
            if (id in terminatedWorkIds) return
            val oldState = pendingWorkStates[id]
                ?: throw IllegalArgumentException("Work with id $workSpecId is not enqueued!")
            state = oldState.copy(initialDelayMet = true)
            pendingWorkStates[id] = state
        }
        scheduleInternal(id, state)
    }

    /**
     * Tells the [TestScheduler] to pretend that the periodic delay on the [Worker] with
     * the given `workSpecId` are met.
     *
     * @param workSpecId The [Worker]'s id
     * @throws IllegalArgumentException if `workSpecId` is not enqueued
     */
    fun setPeriodDelayMet(workSpecId: UUID) {
        val id = workSpecId.toString()
        val state: InternalWorkState
        synchronized(lock) {
            val oldState = pendingWorkStates[id]
                ?: throw IllegalArgumentException("Work with id $workSpecId is not enqueued!")
            state = oldState.copy(periodDelayMet = true)
            pendingWorkStates[id] = state
        }
        scheduleInternal(id, state)
    }

    override fun onExecuted(workSpecId: String, needsReschedule: Boolean) {
        val internalWorkState: InternalWorkState
        synchronized(lock) {
            internalWorkState = pendingWorkStates[workSpecId] ?: return
        }
        // first update db, only when update internal state
        // doing database update outside of the lock, because we don't want run any
        // callbacks holding lock, because `setPeriodStartTime` will trigger db observers.
        if (internalWorkState.isPeriodic) {
            // Reset the startTime to simulate the first run of PeriodicWork.
            // Otherwise WorkerWrapper de-dupes runs of PeriodicWork to 1 execution per interval
            val workManager = WorkManagerImpl.getInstance(context)
            val workDatabase = workManager.workDatabase
            workDatabase.workSpecDao().setPeriodStartTime(workSpecId, 0)
        }

        synchronized(lock) {
            if (internalWorkState.isPeriodic) {
                pendingWorkStates[workSpecId] = internalWorkState.copy(
                    periodDelayMet = !internalWorkState.isPeriodic,
                    constraintsMet = !internalWorkState.hasConstraints,
                )
            } else {
                pendingWorkStates.remove(workSpecId)
                terminatedWorkIds.add(workSpecId)
            }
        }
    }

    private fun scheduleInternal(workId: String, state: InternalWorkState) {
        if (state.isRunnable) WorkManagerImpl.getInstance(context).startWork(workId)
    }
}

internal data class InternalWorkState(
    val constraintsMet: Boolean,
    val initialDelayMet: Boolean,
    val periodDelayMet: Boolean,
    val hasConstraints: Boolean,
    val isPeriodic: Boolean,
)

internal val InternalWorkState.isRunnable: Boolean
    get() = constraintsMet && initialDelayMet && periodDelayMet

internal fun InternalWorkState(spec: WorkSpec): InternalWorkState =
    InternalWorkState(
        constraintsMet = !spec.hasConstraints(),
        initialDelayMet = spec.initialDelay == 0L,
        periodDelayMet = true,
        hasConstraints = spec.hasConstraints(),
        isPeriodic = spec.isPeriodic
    )