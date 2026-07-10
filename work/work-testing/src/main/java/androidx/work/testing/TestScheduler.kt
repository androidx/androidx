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
import androidx.work.Clock
import androidx.work.RunnableScheduler
import androidx.work.WorkInfo
import androidx.work.Worker
import androidx.work.impl.Scheduler
import androidx.work.impl.StartStopToken
import androidx.work.impl.StartStopTokens
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkLauncher
import androidx.work.impl.background.greedy.DelayedWorkTracker
import androidx.work.impl.model.WorkGenerationalId
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.model.WorkSpecDao
import androidx.work.impl.model.generationalId
import androidx.work.testing.WorkManagerTestInitHelper.ExecutorsMode
import java.util.UUID

/**
 * A test scheduler that schedules unconstrained, non-timed workers. It intentionally does not
 * acquire any WakeLocks, instead trying to brute-force them as time allows before the process gets
 * killed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TestScheduler(
    private val workDatabase: WorkDatabase,
    private val launcher: WorkLauncher,
    private val clock: Clock,
    runnableScheduler: RunnableScheduler,
    private val executorsMode: ExecutorsMode,
) : Scheduler, TestDriver {
    @GuardedBy("lock") private val pendingWorkStates = mutableMapOf<String, InternalWorkState>()
    private val lock = Any()
    private val startStopTokens = StartStopTokens.create()
    private val delayedWorkTracker = DelayedWorkTracker(this, runnableScheduler, clock)

    override fun hasLimitedSchedulingSlots(): Boolean = true

    override fun schedule(vararg workSpecs: WorkSpec) {
        if (workSpecs.isEmpty()) {
            return
        }
        val toSchedule = mutableMapOf<WorkSpec, InternalWorkState>()
        synchronized(lock) {
            workSpecs.forEach {
                val oldState = pendingWorkStates[it.id] ?: InternalWorkState()
                val state = oldState.copy(isScheduled = true)
                pendingWorkStates[it.id] = state
                toSchedule[it] = state
            }
        }
        toSchedule.forEach { (spec, state) ->
            if (executorsMode != ExecutorsMode.USE_TIME_BASED_SCHEDULING) {
                if (spec.isBackedOff && spec.calculateNextRunTime() > clock.currentTimeMillis()) {
                    return@forEach
                }
            }
            maybeScheduleInternal(spec, state)
        }
    }

    // cancel called in two situations when:
    // 1. a worker was cancelled via workManager.cancelWorkById
    // 2. a worker finished (no matter successfully or not), see comment in
    // Schedulers.registerRescheduling
    override fun cancel(workSpecId: String) {
        val tokens: List<StartStopToken>
        synchronized(lock) { tokens = startStopTokens.remove(workSpecId) }
        tokens.forEach { launcher.stopWork(it) }
    }

    /**
     * Tells the [TestScheduler] to pretend that all constraints on the [Worker] with the given
     * `workSpecId` are met.
     *
     * @param workSpecId The [Worker]'s id
     * @throws IllegalArgumentException if `workSpecId` is not enqueued
     */
    override fun setAllConstraintsMet(workSpecId: UUID) {
        val id = workSpecId.toString()
        val spec = loadSpec(id)
        val state: InternalWorkState
        synchronized(lock) {
            val oldState = pendingWorkStates[id] ?: InternalWorkState(initialDelayMet = false)
            state = oldState.copy(constraintsMet = true)
            pendingWorkStates[id] = state
        }
        maybeScheduleInternal(spec, state)
    }

    /**
     * Tells the [TestScheduler] to pretend that the initial delay on the [Worker] with the given
     * `workSpecId` are met.
     *
     * @param workSpecId The [Worker]'s id
     * @throws IllegalArgumentException if `workSpecId` is not enqueued
     */
    override fun setInitialDelayMet(workSpecId: UUID) {
        check(executorsMode != ExecutorsMode.USE_TIME_BASED_SCHEDULING) {
            "Can't use setInitialDelayMet() when WorkManagerTestInitHelper is configured with" +
                "time-based scheduling"
        }

        val id = workSpecId.toString()
        val state: InternalWorkState
        val spec = loadSpec(id)
        synchronized(lock) {
            val oldState = pendingWorkStates[id] ?: InternalWorkState()
            state = oldState.copy(initialDelayMet = true)
            pendingWorkStates[id] = state
        }
        maybeScheduleInternal(spec, state)
    }

    /**
     * Tells the [TestScheduler] to pretend that the periodic delay on the [Worker] with the given
     * `workSpecId` are met.
     *
     * @param workSpecId The [Worker]'s id
     * @throws IllegalArgumentException if `workSpecId` is not enqueued
     */
    override fun setPeriodDelayMet(workSpecId: UUID) {
        check(executorsMode != ExecutorsMode.USE_TIME_BASED_SCHEDULING) {
            "Can't use setPeriodDelayMet() when WorkManagerTestInitHelper is configured with " +
                "time-based scheduling"
        }
        val id = workSpecId.toString()
        val spec = loadSpec(id)
        if (!spec.isPeriodic) throw IllegalArgumentException("Work with id $id isn't periodic!")

        val state: InternalWorkState
        synchronized(lock) {
            val oldState = pendingWorkStates[id] ?: InternalWorkState()
            state = oldState.copy(periodDelayMet = true)
            pendingWorkStates[id] = state
        }
        maybeScheduleInternal(spec, state)
    }

    /**
     * Tells [TestScheduler] to pretend that a running worker should be stopped with the provided
     * `StopReason`.
     *
     * @param workSpecId The [Worker]'s id
     * @param reason The `StopReason` that will be available to the worker
     * @throws IllegalStateException if `workSpecId` is not running
     * @throws IllegalArgumentException if `StopReason` is [WorkInfo.STOP_REASON_NOT_STOPPED]
     */
    override fun stopRunningWorkWithReason(workSpecId: UUID, reason: Int) {
        require(reason != WorkInfo.STOP_REASON_NOT_STOPPED) {
            "Work cannot be stopped with reason STOP_REASON_NOT_STOPPED"
        }

        val tokens: List<StartStopToken>
        synchronized(lock) { tokens = startStopTokens.remove(workSpecId.toString()) }
        if (tokens.isEmpty())
            throw IllegalStateException(
                "Work with id $workSpecId is not running and cannot be stopped."
            )

        tokens.forEach { launcher.stopWork(it, reason) }
    }

    private fun maybeScheduleInternal(spec: WorkSpec, state: InternalWorkState) {
        val generationalId = spec.generationalId()

        if (executorsMode == ExecutorsMode.USE_TIME_BASED_SCHEDULING) {
            // Only the clock unlocks scheduled work. setxDelayMet() throws.
            if (isRunnableClock(spec, state)) {
                launcher.startWork(generateStartStopToken(spec, generationalId))
            } else if (isSchedulable(spec, state)) {
                // No need for token, delayedWorkTracker calls back to schedule here.
                delayedWorkTracker.schedule(spec, spec.calculateNextRunTime())
            }
        } else {
            if (isRunnableInternalState(spec, state)) {
                workDatabase.rewindNextRunTimeToNow(spec.id, clock)
                launcher.startWork(generateStartStopToken(spec, generationalId))
            }
            // Clock is not considered, only InternalWorkSpec.
        }
    }

    private fun generateStartStopToken(
        spec: WorkSpec,
        generationalId: WorkGenerationalId,
    ): StartStopToken {
        val token =
            synchronized(lock) {
                delayedWorkTracker.unschedule(spec.id)
                pendingWorkStates.remove(generationalId.workSpecId)
                startStopTokens.tokenFor(generationalId)
            }
        return token
    }

    private fun loadSpec(id: String): WorkSpec {
        val workSpec =
            workDatabase.workSpecDao().getWorkSpec(id)
                ?: throw IllegalArgumentException("Work with id $id is not enqueued!")
        return workSpec
    }

    private fun isRunnableClock(spec: WorkSpec, state: InternalWorkState): Boolean {
        val scheduleTime = clock.currentTimeMillis() >= spec.calculateNextRunTime()
        val constraints = isConstraintsMet(spec, state)

        return state.isScheduled && constraints && scheduleTime
    }

    private fun isRunnableInternalState(spec: WorkSpec, state: InternalWorkState): Boolean {
        val constraints = isConstraintsMet(spec, state)

        val initialDelay =
            spec.initialDelay == 0L || state.initialDelayMet || !spec.isFirstPeriodicRun
        val periodic =
            // .isFirstPeriodicRun is false for overridden first periods.
            if (spec.isPeriodic) (state.periodDelayMet || spec.isFirstPeriodicRun) else true
        return state.isScheduled && constraints && periodic && initialDelay
    }

    private fun isSchedulable(spec: WorkSpec, state: InternalWorkState): Boolean {
        return state.isScheduled && isConstraintsMet(spec, state)
    }

    private fun isConstraintsMet(spec: WorkSpec, state: InternalWorkState): Boolean {
        return !spec.hasConstraints() || state.constraintsMet
    }
}

internal data class InternalWorkState(
    val constraintsMet: Boolean = false,
    val initialDelayMet: Boolean = false,
    val periodDelayMet: Boolean = false,
    /* means that TestScheduler received this workrequest in schedule(....) function */
    val isScheduled: Boolean = false,
)

private val WorkSpec.isNextScheduleOverridden
    get() = nextScheduleTimeOverride != Long.MAX_VALUE

private val WorkSpec.isFirstPeriodicRun
    get() = periodCount == 0 && runAttemptCount == 0 && !isNextScheduleOverridden

// Overrides are treated as continuing periods, not first runs.

private fun WorkDatabase.rewindNextRunTimeToNow(id: String, clock: Clock): WorkSpec {
    // We need to pass check that mWorkSpec.calculateNextRunTime() < now
    // so we reset "rewind" enqueue time or nextScheduleTimeOverride to pass the check
    // we don't reuse available internalWorkState.mWorkSpec, because it
    // is not update with period_count and last_enqueue_time
    val dao: WorkSpecDao = workSpecDao()
    val workSpec: WorkSpec =
        dao.getWorkSpec(id)
            ?: throw IllegalStateException("WorkSpec is already deleted from WM's db")
    val now = clock.currentTimeMillis()
    val timeOffset = workSpec.calculateNextRunTime() - now
    if (timeOffset > 0) {
        if (workSpec.isNextScheduleOverridden) {
            dao.setNextScheduleTimeOverride(id, now)
        } else {
            dao.setLastEnqueueTime(id, workSpec.lastEnqueueTime - timeOffset)
        }
    }
    return dao.getWorkSpec(id)
        ?: throw IllegalStateException("WorkSpec is already deleted from WM's db")
}
