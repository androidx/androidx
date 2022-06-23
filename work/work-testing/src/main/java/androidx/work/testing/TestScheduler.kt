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
import androidx.work.impl.WorkDatabase
import androidx.work.impl.model.WorkGenerationalId
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.StartStopTokens
import androidx.work.impl.model.generationalId
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.model.WorkSpecDao
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
    private val mStartStopTokens = StartStopTokens()

    override fun hasLimitedSchedulingSlots() = true

    override fun schedule(vararg workSpecs: WorkSpec) {
        if (workSpecs.isEmpty()) {
            return
        }
        val toSchedule = mutableMapOf<WorkSpec, InternalWorkState>()
        synchronized(lock) {
            workSpecs.forEach {
                val state = pendingWorkStates.getOrPut(it.generationalId().workSpecId) {
                    InternalWorkState(it)
                }
                toSchedule[it] = state
            }
        }
        toSchedule.forEach { (spec, state) ->
            // this spec is attempted to run for the first time
            // so we have to rewind the time, because we have to override flex.
            if (spec.isPeriodic && state.periodDelayMet) {
                WorkManagerImpl.getInstance(context).rewindLastEnqueueTime(spec.id)
            }
            scheduleInternal(spec.generationalId(), state)
        }
    }

    override fun cancel(workSpecId: String) {
        // We don't need to keep track of cancelled workSpecs. This is because subsequent calls
        // to enqueue() will no-op because insertWorkSpec in WorkDatabase has a conflict
        // policy of @Ignore. So TestScheduler will _never_ be asked to schedule those
        // WorkSpecs.
        val tokens = mStartStopTokens.remove(workSpecId)
        tokens.forEach { WorkManagerImpl.getInstance(context).stopWork(it) }
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
        scheduleInternal(WorkGenerationalId(id, state.generation), state)
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
        WorkManagerImpl.getInstance(context).rewindLastEnqueueTime(id)
        scheduleInternal(WorkGenerationalId(id, state.generation), state)
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
        WorkManagerImpl.getInstance(context).rewindLastEnqueueTime(id)
        scheduleInternal(WorkGenerationalId(id, state.generation), state)
    }

    override fun onExecuted(id: WorkGenerationalId, needsReschedule: Boolean) {
        synchronized(lock) {
            val workSpecId = id.workSpecId
            val internalWorkState = pendingWorkStates[workSpecId] ?: return
            if (internalWorkState.isPeriodic) {
                pendingWorkStates[workSpecId] = internalWorkState.copy(
                    periodDelayMet = !internalWorkState.isPeriodic,
                    constraintsMet = !internalWorkState.hasConstraints,
                )
            } else {
                pendingWorkStates.remove(workSpecId)
                terminatedWorkIds.add(workSpecId)
            }
            mStartStopTokens.remove(workSpecId)
        }
    }

    private fun scheduleInternal(generationalId: WorkGenerationalId, state: InternalWorkState) {
        if (state.isRunnable) {
            val wm = WorkManagerImpl.getInstance(context)
            wm.startWork(mStartStopTokens.tokenFor(generationalId))
        }
    }
}

internal data class InternalWorkState(
    val generation: Int,
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
        generation = spec.generation,
        constraintsMet = !spec.hasConstraints(),
        initialDelayMet = spec.initialDelay == 0L,
        periodDelayMet = true,
        hasConstraints = spec.hasConstraints(),
        isPeriodic = spec.isPeriodic
    )

private fun WorkManagerImpl.rewindLastEnqueueTime(id: String) {
    // We need to pass check that mWorkSpec.calculateNextRunTime() < now
    // so we reset "rewind" enqueue time to pass the check
    // we don't reuse available internalWorkState.mWorkSpec, because it
    // is not update with period_count and last_enqueue_time
    // More proper solution would be to abstract away time instead of just using
    // System.currentTimeMillis() in WM
    val workDatabase: WorkDatabase = workDatabase
    val dao: WorkSpecDao = workDatabase.workSpecDao()
    val workSpec: WorkSpec = dao.getWorkSpec(id)
        ?: throw IllegalStateException("WorkSpec is already deleted from WM's db")
    val now = System.currentTimeMillis()
    val timeOffset = workSpec.calculateNextRunTime() - now
    if (timeOffset > 0) {
        dao.setLastEnqueuedTime(id, workSpec.lastEnqueueTime - timeOffset)
    }
}