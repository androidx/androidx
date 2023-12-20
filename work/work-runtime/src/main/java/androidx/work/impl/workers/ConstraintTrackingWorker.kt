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
package androidx.work.impl.workers

import android.content.Context
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.Logger
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.constraints.ConstraintsState
import androidx.work.impl.constraints.ConstraintsState.ConstraintsNotMet
import androidx.work.impl.constraints.OnConstraintsStateChangedListener
import androidx.work.impl.constraints.WorkConstraintsTracker
import androidx.work.impl.constraints.listen
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.futures.SettableFuture
import com.google.common.util.concurrent.ListenableFuture

/**
 * Is an implementation of a [androidx.work.Worker] that can delegate to a different
 * [androidx.work.Worker] when the constraints are met.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConstraintTrackingWorker(
    appContext: Context,
    private val workerParameters: WorkerParameters
) : ListenableWorker(appContext, workerParameters), OnConstraintsStateChangedListener {

    private val lock = Any()

    // Marking this volatile as the delegated workers could switch threads.
    @Volatile
    private var areConstraintsUnmet: Boolean = false
    private val future = SettableFuture.create<Result>()

    /**
     * @return The [androidx.work.Worker] used for delegated work
     */
    @get:VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    var delegate: ListenableWorker? = null
        private set

    override fun startWork(): ListenableFuture<Result> {
        backgroundExecutor.execute { setupAndRunConstraintTrackingWork() }
        return future
    }

    private fun setupAndRunConstraintTrackingWork() {
        if (future.isCancelled) return

        val className = inputData.getString(ARGUMENT_CLASS_NAME)
        val logger = Logger.get()
        if (className.isNullOrEmpty()) {
            logger.error(TAG, "No worker to delegate to.")
            future.setFailed()
            return
        }
        delegate = workerFactory.createWorkerWithDefaultFallback(
            applicationContext, className, workerParameters
        )
        if (delegate == null) {
            logger.debug(TAG, "No worker to delegate to.")
            future.setFailed()
            return
        }

        val workManagerImpl = WorkManagerImpl.getInstance(applicationContext)
        // We need to know what the real constraints are for the delegate.
        val workSpec = workManagerImpl.workDatabase.workSpecDao().getWorkSpec(id.toString())
        if (workSpec == null) {
            future.setFailed()
            return
        }
        val workConstraintsTracker = WorkConstraintsTracker(workManagerImpl.trackers)

        // Start tracking
        val dispatcher = workManagerImpl.workTaskExecutor.taskCoroutineDispatcher
        val job = workConstraintsTracker.listen(workSpec, dispatcher, this)
        future.addListener({ job.cancel(null) }, SynchronousExecutor())
        if (workConstraintsTracker.areAllConstraintsMet(workSpec)) {
            logger.debug(TAG, "Constraints met for delegate $className")

            // Wrapping the call to mDelegate#doWork() in a try catch, because
            // changes in constraints can cause the worker to throw RuntimeExceptions, and
            // that should cause a retry.
            try {
                val innerFuture = delegate!!.startWork()
                innerFuture.addListener({
                    synchronized(lock) {
                        if (areConstraintsUnmet) {
                            future.setRetry()
                        } else {
                            future.setFuture(innerFuture)
                        }
                    }
                }, backgroundExecutor)
            } catch (exception: Throwable) {
                logger.debug(
                    TAG, "Delegated worker $className threw exception in startWork.", exception
                )
                synchronized(lock) {
                    if (areConstraintsUnmet) {
                        logger.debug(TAG, "Constraints were unmet, Retrying.")
                        future.setRetry()
                    } else {
                        future.setFailed()
                    }
                }
            }
        } else {
            logger.debug(
                TAG, "Constraints not met for delegate $className. Requesting retry."
            )
            future.setRetry()
        }
    }

    override fun onStopped() {
        super.onStopped()
        val delegateInner = delegate
        if (delegateInner != null && !delegateInner.isStopped) {
            // Stop is the method that sets the stopped and cancelled bits and invokes onStopped.
            val reason = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) stopReason else 0
            delegateInner.stop(reason)
        }
    }

    override fun onConstraintsStateChanged(workSpec: WorkSpec, state: ConstraintsState) {
        // If at any point, constraints are not met mark it so we can retry the work.
        Logger.get().debug(TAG, "Constraints changed for $workSpec")
        if (state is ConstraintsNotMet) {
            synchronized(lock) { areConstraintsUnmet = true }
        }
    }
}

private fun SettableFuture<Result>.setFailed() = set(Result.failure())
private fun SettableFuture<Result>.setRetry() = set(Result.retry())

private val TAG = Logger.tagWithPrefix("ConstraintTrkngWrkr")

/**
 * The `className` of the [androidx.work.Worker] to delegate to.
 */
internal const val ARGUMENT_CLASS_NAME =
    "androidx.work.impl.workers.ConstraintTrackingWorker.ARGUMENT_CLASS_NAME"
