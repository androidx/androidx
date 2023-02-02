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

import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.work.Worker
import androidx.work.impl.Scheduler
import androidx.work.impl.StartStopTokens
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.model.WorkGenerationalId
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.model.WorkSpecDao
import androidx.work.impl.model.generationalId
import java.util.UUID

/**
 * A test scheduler that schedules unconstrained, non-timed workers. It intentionally does
 * not acquire any WakeLocks, instead trying to brute-force them as time allows before the process
 * gets killed.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TestScheduler(private val workManagerImpl: WorkManagerImpl) : Scheduler {
    @GuardedBy("lock")
    private val pendingWorkStates = mutableMapOf<String, InternalWorkState>()
    private val lock = Any()
    private val startStopTokens = StartStopTokens()

    override fun hasLimitedSchedulingSlots() = true

    override fun schedule(vararg workSpecs: WorkSpec) {
        if (workSpecs.isEmpty()) {
            return
        }
        val toSchedule = mutableMapOf<WorkSpec, InternalWorkState>()
        synchronized(lock) {
            workSpecs.forEach {
                val state = pendingWorkStates.getOrPut(it.generationalId().workSpecId) {
                    InternalWorkState(it, true)
                }
                toSchedule[it] = state.copy(isScheduled = true)
            }
        }
        toSchedule.forEach { (originalSpec, state) ->
            // this spec is attempted to run for the first time
            // so we have to rewind the time, because we have to override flex.
            val spec = if (originalSpec.isPeriodic && state.periodDelayMet) {
                workManagerImpl.rewindLastEnqueueTime(originalSpec.id)
            } else originalSpec
            // don't even try to run a worker that WorkerWrapper won't execute anyway.
            // similar to logic in WorkerWrapper
            if ((spec.isPeriodic || spec.isBackedOff) &&
                (spec.calculateNextRunTime() > System.currentTimeMillis())) {
                return@forEach
            }
            scheduleInternal(spec.generationalId(), state)
        }
    }

    // cancel called in two situations when:
    // 1. a worker was cancelled via workManager.cancelWorkById
    // 2. a worker finished (no matter successfully or not), see comment in
    // Schedulers.registerRescheduling
    override fun cancel(workSpecId: String) {
        val tokens = startStopTokens.remove(workSpecId)
        tokens.forEach { workManagerImpl.stopWork(it) }
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
        val spec = loadSpec(id)
        val state: InternalWorkState
        synchronized(lock) {
            val oldState = pendingWorkStates[id] ?: InternalWorkState(spec, false)
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
        val spec = loadSpec(id)
        synchronized(lock) {
            val oldState = pendingWorkStates[id] ?: InternalWorkState(spec, false)
            state = oldState.copy(initialDelayMet = true)
            pendingWorkStates[id] = state
        }
        workManagerImpl.rewindLastEnqueueTime(id)
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
        val spec = loadSpec(id)
        if (!spec.isPeriodic) throw IllegalArgumentException("Work with id $id isn't periodic!")

        val state: InternalWorkState
        synchronized(lock) {
            val oldState = pendingWorkStates[id] ?: InternalWorkState(spec, false)
            state = oldState.copy(periodDelayMet = true)
            pendingWorkStates[id] = state
        }
        workManagerImpl.rewindLastEnqueueTime(id)
        scheduleInternal(WorkGenerationalId(id, state.generation), state)
    }

    private fun scheduleInternal(generationalId: WorkGenerationalId, state: InternalWorkState) {
        if (state.isRunnable) {
            val token = synchronized(lock) {
                pendingWorkStates.remove(generationalId.workSpecId)
                startStopTokens.tokenFor(generationalId)
            }
            workManagerImpl.startWork(token)
        }
    }

    private fun loadSpec(id: String): WorkSpec {
        val workSpec = workManagerImpl.workDatabase.workSpecDao().getWorkSpec(id)
            ?: throw IllegalArgumentException("Work with id $id is not enqueued!")
        return workSpec
    }
}

internal data class InternalWorkState(
    val generation: Int,
    val constraintsMet: Boolean,
    val initialDelayMet: Boolean,
    val periodDelayMet: Boolean,
    val hasConstraints: Boolean,
    val isPeriodic: Boolean,
    /* means that TestScheduler received this workrequest in schedule(....) function */
    val isScheduled: Boolean
)

internal val InternalWorkState.isRunnable: Boolean
    get() = constraintsMet && initialDelayMet && periodDelayMet && isScheduled

internal fun InternalWorkState(spec: WorkSpec, isScheduled: Boolean): InternalWorkState =
    InternalWorkState(
        generation = spec.generation,
        constraintsMet = !spec.hasConstraints(),
        initialDelayMet = spec.initialDelay == 0L,
        periodDelayMet = spec.periodCount == 0 && spec.runAttemptCount == 0,
        hasConstraints = spec.hasConstraints(),
        isPeriodic = spec.isPeriodic,
        isScheduled = isScheduled,
    )

private fun WorkManagerImpl.rewindLastEnqueueTime(id: String): WorkSpec {
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
    return dao.getWorkSpec(id)
        ?: throw IllegalStateException("WorkSpec is already deleted from WM's db")
}