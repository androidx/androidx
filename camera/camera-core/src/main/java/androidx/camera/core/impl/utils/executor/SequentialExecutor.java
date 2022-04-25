/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl.utils.executor;

import static androidx.camera.core.impl.utils.executor.SequentialExecutor.WorkerRunningState.IDLE;
import static androidx.camera.core.impl.utils.executor.SequentialExecutor.WorkerRunningState.QUEUED;
import static androidx.camera.core.impl.utils.executor.SequentialExecutor.WorkerRunningState.QUEUING;
import static androidx.camera.core.impl.utils.executor.SequentialExecutor.WorkerRunningState.RUNNING;

import androidx.annotation.GuardedBy;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.core.util.Preconditions;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Executor ensuring that all Runnables submitted are executed in order, using the provided
 * Executor, and sequentially such that no two will ever be running at the same time.
 *
 * <p>Tasks submitted to {@link #execute(Runnable)} are executed in FIFO order.
 *
 * <p>The execution of tasks is done by one thread as long as there are tasks left in the queue.
 * When a task is {@linkplain Thread#interrupt interrupted}, execution of subsequent tasks
 * continues. See {@link QueueWorker#workOnQueue} for details.
 *
 * <p>{@code RuntimeException}s thrown by tasks are simply logged and the executor keeps trucking.
 * If an {@code Error} is thrown, the error will propagate and execution will stop until it is
 * restarted by a call to {@link #execute}.
 *
 * <p>Copied and adapted from Guava.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class SequentialExecutor implements Executor {
    private static final String TAG = "SequentialExecutor";
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mQueue")
    final Deque<Runnable> mQueue = new ArrayDeque<>();
    /** Underlying executor that all submitted Runnable objects are run on. */
    private final Executor mExecutor;
    private final QueueWorker mWorker = new QueueWorker();
    /** see {@link WorkerRunningState} */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mQueue")
    WorkerRunningState mWorkerRunningState = IDLE;

    /**
     * This counter prevents an ABA issue where a thread may successfully schedule the worker, the
     * worker runs and exhausts the queue, another thread enqueues a task and fails to schedule the
     * worker, and then the first thread's call to delegate.execute() returns. Without this counter,
     * it would observe the QUEUING state and set it to QUEUED, and the worker would never be
     * scheduled again for future submissions.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mQueue")
    long mWorkerRunCount = 0;

    /** Use {@link CameraXExecutors#newSequentialExecutor} */
    SequentialExecutor(Executor executor) {
        mExecutor = Preconditions.checkNotNull(executor);
    }

    /**
     * Adds a task to the queue and makes sure a worker thread is running.
     *
     * <p>If this method throws, e.g. a {@code RejectedExecutionException} from the delegate
     * executor, execution of tasks will stop until a call to this method is made.
     */
    @Override
    public void execute(final Runnable task) {
        Preconditions.checkNotNull(task);
        final Runnable submittedTask;
        final long oldRunCount;
        synchronized (mQueue) {
            // If the worker is already running (or execute() on the delegate returned
            // successfully, and
            // the worker has yet to start) then we don't need to start the worker.
            if (mWorkerRunningState == RUNNING || mWorkerRunningState == QUEUED) {
                mQueue.add(task);
                return;
            }

            oldRunCount = mWorkerRunCount;

            // If the worker is not yet running, the delegate Executor might reject our attempt
            // to start it. To preserve FIFO order and failure atomicity of rejected execution when
            // the same Runnable is executed more than once, allocate a wrapper that we know is safe
            // to remove by object identity. A data structure that returned a removal handle from
            // add() would allow eliminating this allocation.
            submittedTask =
                    new Runnable() {
                        @Override
                        public void run() {
                            task.run();
                        }
                    };
            mQueue.add(submittedTask);
            mWorkerRunningState = QUEUING;
        }

        try {
            mExecutor.execute(mWorker);
        } catch (RuntimeException | Error t) {
            synchronized (mQueue) {
                boolean removed =
                        (mWorkerRunningState == IDLE || mWorkerRunningState == QUEUING)
                                && mQueue.removeLastOccurrence(submittedTask);
                // If the delegate is directExecutor(), the submitted runnable could have thrown
                // a REE. But that's handled by the log check that catches RuntimeExceptions in the
                // queue worker.
                if (!(t instanceof RejectedExecutionException) || removed) {
                    throw t;
                }
            }
            return;
        }

        /*
         * This is an unsynchronized read! After the read, the function returns immediately or
         * acquires the lock to check again. Since an IDLE state was observed inside the preceding
         * synchronized block, and reference field assignment is atomic, this may save reacquiring
         * the lock when another thread or the worker task has cleared the count and set the state.
         *
         * <p>When {@link #executor} is a directExecutor(), the value written to
         * {@code mWorkerRunningState} will be available synchronously, and behaviour will be
         * deterministic.
         */
        @SuppressWarnings("GuardedBy")
        boolean alreadyMarkedQueued = mWorkerRunningState != QUEUING;
        if (alreadyMarkedQueued) {
            return;
        }
        synchronized (mQueue) {
            if (mWorkerRunCount == oldRunCount && mWorkerRunningState == QUEUING) {
                mWorkerRunningState = QUEUED;
            }
        }
    }

    enum WorkerRunningState {
        /** Runnable is not running and not queued for execution */
        IDLE,
        /** Runnable is not running, but is being queued for execution */
        QUEUING,
        /** Runnable has been submitted but has not yet begun execution */
        QUEUED,
        /** Runnable is running */
        RUNNING,
    }

    /** Worker that runs tasks from {@link #mQueue} until it is empty. */
    final class QueueWorker implements Runnable {
        @Override
        public void run() {
            try {
                workOnQueue();
            } catch (Error e) {
                synchronized (mQueue) {
                    mWorkerRunningState = IDLE;
                }
                throw e;
                // The execution of a task has ended abnormally.
                // We could have tasks left in the queue, so should perhaps try to restart a worker,
                // but then the Error will get delayed if we are using a direct (same thread)
                // executor.
            }
        }

        /**
         * Continues executing tasks from {@link #mQueue} until it is empty.
         *
         * <p>The thread's interrupt bit is cleared before execution of each task.
         *
         * <p>If the Thread in use is interrupted before or during execution of the tasks in {@link
         * #mQueue}, the Executor will complete its tasks, and then restore the interruption.
         * This means that once the Thread returns to the Executor that this Executor composes, the
         * interruption will still be present. If the composed Executor is an ExecutorService, it
         * can respond to shutdown() by returning tasks queued on that Thread after {@link #mWorker}
         * drains the queue.
         */
        private void workOnQueue() {
            boolean interruptedDuringTask = false;
            boolean hasSetRunning = false;
            try {
                while (true) {
                    Runnable task;
                    synchronized (mQueue) {
                        // Choose whether this thread will run or not after acquiring the lock on
                        // the first iteration
                        if (!hasSetRunning) {
                            if (mWorkerRunningState == RUNNING) {
                                // Don't want to have two workers pulling from the queue.
                                return;
                            } else {
                                // Increment the run counter to avoid the ABA problem of a submitter
                                // marking the thread as QUEUED after it already ran and exhausted
                                // the queue before returning from execute().
                                mWorkerRunCount++;
                                mWorkerRunningState = RUNNING;
                                hasSetRunning = true;
                            }
                        }
                        task = mQueue.poll();
                        if (task == null) {
                            mWorkerRunningState = IDLE;
                            return;
                        }
                    }
                    // Remove the interrupt bit before each task. The interrupt is for the
                    // "current task" when it is sent, so subsequent tasks in the queue should not
                    // be caused to be interrupted by a previous one in the queue being interrupted.
                    interruptedDuringTask |= Thread.interrupted();
                    try {
                        task.run();
                    } catch (RuntimeException e) {
                        Logger.e(TAG, "Exception while executing runnable " + task, e);
                    }
                }
            } finally {
                // Ensure that if the thread was interrupted at all while processing the task
                // queue, it is returned to the delegate Executor interrupted so that it may handle
                // the interruption if it likes.
                if (interruptedDuringTask) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
