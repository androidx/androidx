/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.work.impl.utils

import android.text.TextUtils
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.work.ExistingWorkPolicy
import androidx.work.Logger
import androidx.work.WorkInfo
import androidx.work.WorkRequest
import androidx.work.impl.Schedulers
import androidx.work.impl.WorkContinuationImpl
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.model.Dependency
import androidx.work.impl.model.WorkName
import androidx.work.impl.model.WorkSpec

/** Manages the enqueuing of a [WorkContinuationImpl]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object EnqueueRunnable {
    private val TAG = Logger.tagWithPrefix("EnqueueRunnable")

    /** Enqueues the given workContinuation. */
    @JvmStatic
    public fun enqueue(workContinuation: WorkContinuationImpl) {
        check(!workContinuation.hasCycles()) { "WorkContinuation has cycles ($workContinuation)" }
        val needsScheduling = addToDatabase(workContinuation)
        if (needsScheduling) {
            scheduleWorkInBackground(workContinuation)
        }
    }

    /**
     * Adds the [WorkSpec]'s to the datastore, parent first. Schedules work on the background
     * scheduler, if transaction is successful.
     */
    @VisibleForTesting
    @Suppress("deprecation")
    public fun addToDatabase(workContinuation: WorkContinuationImpl): Boolean {
        val workManagerImpl = workContinuation.workManagerImpl
        val workDatabase = workManagerImpl.workDatabase
        workDatabase.beginTransaction()
        try {
            checkContentUriTriggerWorkerLimits(
                workDatabase,
                workManagerImpl.configuration,
                workContinuation,
            )
            val needsScheduling = processContinuation(workContinuation)
            workDatabase.setTransactionSuccessful()
            return needsScheduling
        } finally {
            workDatabase.endTransaction()
        }
    }

    /** Schedules work on the background scheduler. */
    @VisibleForTesting
    public fun scheduleWorkInBackground(workContinuation: WorkContinuationImpl) {
        val workManager = workContinuation.workManagerImpl
        Schedulers.schedule(
            workManager.configuration,
            workManager.workDatabase,
            workManager.schedulers,
        )
    }

    private fun processContinuation(workContinuation: WorkContinuationImpl): Boolean {
        var needsScheduling = false
        val parents = workContinuation.parents
        if (parents != null) {
            for (parent in parents) {
                // When chaining off a completed continuation we need to pay
                // attention to parents that may have been marked as enqueued before.
                if (!parent.isEnqueued) {
                    needsScheduling = needsScheduling or processContinuation(parent)
                } else {
                    Logger.get()
                        .warning(
                            TAG,
                            "Already enqueued work ids (${TextUtils.join(", ", parent.ids)})",
                        )
                }
            }
        }
        needsScheduling = needsScheduling or enqueueContinuation(workContinuation)
        return needsScheduling
    }

    private fun enqueueContinuation(workContinuation: WorkContinuationImpl): Boolean {
        val prerequisiteIds = WorkContinuationImpl.prerequisitesFor(workContinuation)

        val needsScheduling =
            enqueueWorkWithPrerequisites(
                workContinuation.workManagerImpl,
                workContinuation.work,
                prerequisiteIds.toTypedArray<String>(),
                workContinuation.name,
                workContinuation.existingWorkPolicy,
            )

        workContinuation.markEnqueued()
        return needsScheduling
    }

    /**
     * Enqueues the [WorkSpec]'s while keeping track of the prerequisites.
     *
     * @return `true` If there is any scheduling to be done.
     */
    private fun enqueueWorkWithPrerequisites(
        workManagerImpl: WorkManagerImpl,
        workList: List<WorkRequest>,
        prerequisiteIds: Array<String>,
        name: String?,
        existingWorkPolicy: ExistingWorkPolicy,
    ): Boolean {
        var prerequisiteIds = prerequisiteIds
        var needsScheduling = false

        val currentTimeMillis = workManagerImpl.configuration.clock.currentTimeMillis()
        val workDatabase = workManagerImpl.workDatabase

        var hasPrerequisite = prerequisiteIds.isNotEmpty()
        var hasCompletedAllPrerequisites = true
        var hasFailedPrerequisites = false
        var hasCancelledPrerequisites = false

        if (hasPrerequisite) {
            // If there are prerequisites, make sure they actually exist before enqueuing
            // anything.  Prerequisites may not exist if we are using unique tags, because the
            // chain of work could have been wiped out already.
            for (id in prerequisiteIds) {
                val prerequisiteWorkSpec = workDatabase.workSpecDao().getWorkSpec(id)
                if (prerequisiteWorkSpec == null) {
                    Logger.get().error(TAG, "Prerequisite $id doesn't exist; not enqueuing")
                    return false
                }

                val prerequisiteState = prerequisiteWorkSpec.state
                hasCompletedAllPrerequisites =
                    hasCompletedAllPrerequisites and (prerequisiteState == WorkInfo.State.SUCCEEDED)
                if (prerequisiteState == WorkInfo.State.FAILED) {
                    hasFailedPrerequisites = true
                } else if (prerequisiteState == WorkInfo.State.CANCELLED) {
                    hasCancelledPrerequisites = true
                }
            }
        }

        val isNamed = !TextUtils.isEmpty(name)

        // We only apply existing work policies for unique tag sequences that are the beginning of
        // chains.
        val shouldApplyExistingWorkPolicy = isNamed && !hasPrerequisite
        if (shouldApplyExistingWorkPolicy) {
            // Get everything with the unique tag.
            val existingWorkSpecIdAndStates: List<WorkSpec.IdAndState> =
                workDatabase.workSpecDao().getWorkSpecIdAndStatesForName(name!!)

            if (!existingWorkSpecIdAndStates.isEmpty()) {
                // If appending, these are the new prerequisites.
                if (
                    existingWorkPolicy == ExistingWorkPolicy.APPEND ||
                        existingWorkPolicy == ExistingWorkPolicy.APPEND_OR_REPLACE
                ) {
                    val dependencyDao = workDatabase.dependencyDao()
                    var newPrerequisiteIds: MutableList<String> = ArrayList()
                    for (idAndState in existingWorkSpecIdAndStates) {
                        if (!dependencyDao.hasDependents(idAndState.id)) {
                            hasCompletedAllPrerequisites =
                                hasCompletedAllPrerequisites and
                                    (idAndState.state == WorkInfo.State.SUCCEEDED)
                            if (idAndState.state == WorkInfo.State.FAILED) {
                                hasFailedPrerequisites = true
                            } else if (idAndState.state == WorkInfo.State.CANCELLED) {
                                hasCancelledPrerequisites = true
                            }
                            newPrerequisiteIds.add(idAndState.id)
                        }
                    }
                    if (existingWorkPolicy == ExistingWorkPolicy.APPEND_OR_REPLACE) {
                        if (hasCancelledPrerequisites || hasFailedPrerequisites) {
                            // Delete all WorkSpecs with this name
                            val workSpecDao = workDatabase.workSpecDao()
                            val idAndStates: List<WorkSpec.IdAndState> =
                                workSpecDao.getWorkSpecIdAndStatesForName(name)
                            for (idAndState in idAndStates) {
                                workSpecDao.delete(idAndState.id)
                            }
                            // Treat this as a new chain of work.
                            newPrerequisiteIds = mutableListOf()
                            hasCancelledPrerequisites = false
                            hasFailedPrerequisites = false
                        }
                    }
                    prerequisiteIds = newPrerequisiteIds.toTypedArray<String>()
                    hasPrerequisite = (prerequisiteIds.size > 0)
                } else {
                    // If we're keeping existing work, make sure to do so only if something is
                    // enqueued or running.
                    if (existingWorkPolicy == ExistingWorkPolicy.KEEP) {
                        for (idAndState in existingWorkSpecIdAndStates) {
                            if (
                                idAndState.state == WorkInfo.State.ENQUEUED ||
                                    idAndState.state == WorkInfo.State.RUNNING
                            ) {
                                return false
                            }
                        }
                    }

                    // Cancel all of these workers.
                    // Don't allow rescheduling in CancelWorkRunnable because it will happen inside
                    // the current transaction.  We want it to happen separately to avoid race
                    // conditions (see ag/4502245, which tries to avoid work trying to run before
                    // it's actually been committed to the database).
                    forNameInline(name, workManagerImpl)
                    // Because we cancelled some work but didn't allow rescheduling inside
                    // CancelWorkRunnable, we need to make sure we do schedule work at the end of
                    // this runnable.
                    needsScheduling = true

                    // And delete all the database records.
                    val workSpecDao = workDatabase.workSpecDao()
                    for (idAndState in existingWorkSpecIdAndStates) {
                        workSpecDao.delete(idAndState.id)
                    }
                }
            }
        }

        for (work in workList) {
            val workSpec = work.workSpec

            if (hasPrerequisite && !hasCompletedAllPrerequisites) {
                if (hasFailedPrerequisites) {
                    workSpec.state = WorkInfo.State.FAILED
                } else if (hasCancelledPrerequisites) {
                    workSpec.state = WorkInfo.State.CANCELLED
                } else {
                    workSpec.state = WorkInfo.State.BLOCKED
                }
            } else {
                // Set scheduled times only for work without prerequisites.
                // Dependent work will set their scheduled times when they are
                // unblocked.
                workSpec.lastEnqueueTime = currentTimeMillis
            }

            // If we have one WorkSpec with an enqueued state, then we need to schedule.
            if (workSpec.state == WorkInfo.State.ENQUEUED) {
                needsScheduling = true
            }

            workDatabase
                .workSpecDao()
                .insertWorkSpec(wrapWorkSpecIfNeeded(workManagerImpl.schedulers, workSpec))

            if (hasPrerequisite) {
                for (prerequisiteId in prerequisiteIds) {
                    val dep = Dependency(work.stringId, prerequisiteId)
                    workDatabase.dependencyDao().insertDependency(dep)
                }
            }

            workDatabase.workTagDao().insertTags(work.stringId, work.tags)
            if (isNamed) {
                workDatabase.workNameDao().insert(WorkName(name!!, work.stringId))
            }
        }
        return needsScheduling
    }
}
