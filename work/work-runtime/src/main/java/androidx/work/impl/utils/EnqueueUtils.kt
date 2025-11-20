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

package androidx.work.impl.utils

import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.impl.Scheduler
import androidx.work.impl.WorkContinuationImpl
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.workers.ARGUMENT_CLASS_NAME
import androidx.work.impl.workers.ConstraintTrackingWorker
import kotlin.collections.removeLast as removeLastKt

internal fun checkContentUriTriggerWorkerLimits(
    workDatabase: WorkDatabase,
    configuration: Configuration,
    continuation: WorkContinuationImpl,
) {
    if (Build.VERSION.SDK_INT < WorkManagerImpl.CONTENT_URI_TRIGGER_API_LEVEL) return
    val continuations = mutableListOf(continuation)
    var newCount = 0
    while (continuations.isNotEmpty()) {
        val current = continuations.removeLastKt()
        newCount += current.work.count { it.workSpec.constraints.hasContentUriTriggers() }
        (current.parents as List<WorkContinuationImpl>?)?.let { continuations.addAll(it) }
    }
    if (newCount == 0) return
    val alreadyEnqueuedCount = workDatabase.workSpecDao().countNonFinishedContentUriTriggerWorkers()
    val limit = configuration.contentUriTriggerWorkersLimit
    if (alreadyEnqueuedCount + newCount > limit)
        throw IllegalArgumentException(
            "Too many workers with contentUriTriggers are enqueued:\n" +
                "contentUriTrigger workers limit: $limit;\n" +
                "already enqueued count: $alreadyEnqueuedCount;\n" +
                "current enqueue operation count: $newCount.\n" +
                "To address this issue you can: \n" +
                "1. enqueue less workers or batch some of workers " +
                "with content uri triggers together;\n" +
                "2. increase limit via Configuration.Builder.setContentUriTriggerWorkersLimit;\n" +
                "Please beware that workers with content uri triggers immediately occupy " +
                "slots in JobScheduler so no updates to content uris are missed."
        )
}

@VisibleForTesting
public fun tryDelegateRemoteListenableWorker(workSpec: WorkSpec): WorkSpec {
    // Check for the arguments in the input data over checking for the workerClassName
    // directly. This is because, the workerClassName might get overridden given it could get
    // replaced by a ConstraintTrackingWorker.
    val hasDelegateWorker = workSpec.input.hasKey<String>(ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME)
    val hasPackageName = workSpec.input.hasKey<String>(ARGUMENT_SERVICE_PACKAGE_NAME)
    val hasClassName = workSpec.input.hasKey<String>(ARGUMENT_SERVICE_CLASS_NAME)
    if (!hasDelegateWorker && hasPackageName && hasClassName) {
        val workerClassName = workSpec.workerClassName
        val newInputData =
            Data.Builder()
                .putAll(workSpec.input)
                .putString(ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME, workerClassName)
                .build()

        return workSpec.copy(
            input = newInputData,
            workerClassName = REMOTE_DELEGATING_LISTENABLE_WORKER_CLASS_NAME,
        )
    }
    return workSpec
}

internal fun tryDelegateConstrainedWorkSpec(workSpec: WorkSpec): WorkSpec {
    // requiresBatteryNotLow and requiresStorageNotLow require API 26 for JobScheduler.
    // Delegate to ConstraintTrackingWorker between API 23-25.
    val constraints = workSpec.constraints
    val workerClassName = workSpec.workerClassName
    // Check if the Worker is a ConstraintTrackingWorker already. Otherwise we could end up
    // wrapping a ConstraintTrackingWorker with another and build a taller stack.
    // This usually happens when a developer accidentally enqueues() a named WorkRequest
    // with an ExistingWorkPolicy.KEEP and subsequent inserts no-op (while the state of the
    // Worker is not ENQUEUED or RUNNING i.e. the Worker probably just got done & the app is
    // holding on to a reference of WorkSpec which got updated). We end up reusing the
    // WorkSpec, and get a ConstraintTrackingWorker (instead of the original Worker class).
    val isConstraintTrackingWorker = workerClassName == ConstraintTrackingWorker::class.java.name
    if (
        !isConstraintTrackingWorker &&
            (constraints.requiresBatteryNotLow() || constraints.requiresStorageNotLow())
    ) {
        val newInputData =
            Data.Builder()
                .putAll(workSpec.input)
                .putString(ARGUMENT_CLASS_NAME, workerClassName)
                .build()
        return workSpec.copy(
            input = newInputData,
            workerClassName = ConstraintTrackingWorker::class.java.name,
        )
    }
    return workSpec
}

internal fun wrapWorkSpecIfNeeded(schedulers: List<Scheduler>, workSpec: WorkSpec): WorkSpec {
    // Use RemoteListenableWorker delegate if necessary.
    val delegated = tryDelegateRemoteListenableWorker(workSpec)
    // Use a ConstraintTrackingWorker when necessary.
    return when {
        Build.VERSION.SDK_INT <= 25 -> tryDelegateConstrainedWorkSpec(delegated)
        else -> delegated
    }
}

// Redefine the keys

internal const val ARGUMENT_SERVICE_PACKAGE_NAME =
    "androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME"

internal const val ARGUMENT_SERVICE_CLASS_NAME =
    "androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_CLASS_NAME"

/**
 * Originally defined in `RemoteListenableDelegatingWorker.kt`. This method is being re-defined here
 * to avoid a circular dependency on the multiprocess library.
 *
 * The fully qualified name of the class that is used when running in the context of a remote
 * process. This constant is useful when migrating remote workers between processes.
 */
internal const val REMOTE_DELEGATING_LISTENABLE_WORKER_CLASS_NAME =
    "androidx.work.multiprocess.RemoteListenableDelegatingWorker"

// The RemoteListenableWorker class to delegate to.

internal const val ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME =
    "androidx.work.multiprocess.RemoteListenableDelegatingWorker.ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME"
