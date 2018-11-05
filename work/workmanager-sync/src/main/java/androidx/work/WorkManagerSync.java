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

package androidx.work;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import androidx.work.impl.WorkManagerImpl;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Blocking methods for {@link androidx.work.WorkManager} operations. These methods are expected
 * to be called from a background thread.
 */
public final class WorkManagerSync {

    private final WorkManagerImpl mWorkManagerImpl;

    private static WorkManagerSync sInstance = null;
    private static final Object sLock = new Object();

    private WorkManagerSync(@NonNull WorkManagerImpl workManagerImpl) {
        this.mWorkManagerImpl = workManagerImpl;
    }

    /**
     * Retrieves the {@code default} singleton instance of {@link WorkManagerSync}.
     *
     * @return The singleton instance of {@link WorkManagerSync}; this may be {@code null} in
     *         unusual circumstances where you have disabled automatic initialization and have
     *         failed to manually call {@link WorkManager#initialize(Context, Configuration)}.
     * @throws IllegalStateException Thrown when WorkManager is not initialized properly. This is
     *         most likely because you disabled the automatic initialization but forgot to manually
     *         call {@link WorkManager#initialize(Context, Configuration)}.
     */
    public static @NonNull WorkManagerSync getInstance() {
        synchronized (sLock) {
            if (sInstance == null) {
                WorkManagerImpl workManager = WorkManagerImpl.getInstance();
                if (workManager == null) {
                    throw new IllegalStateException(
                            "WorkManager is not initialized properly. The most "
                                    + "likely cause is that you disabled WorkManagerInitializer "
                                    + "in your manifest but forgot to call WorkManager#initialize "
                                    + "in your Application#onCreate or a ContentProvider.");
                }
                sInstance = new WorkManagerSync(workManager);
            }
            return sInstance;
        }
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param workRequests One or more {@link WorkRequest}s to enqueue
     * @throws InterruptedException Thrown when the thread enqueueing the {@link WorkRequest}s is
     *                              interrupted.
     * @throws ExecutionException   Thrown when there is an error executing the request to enqueue
     *                              {@link WorkRequest}s.
     */
    @WorkerThread
    public void enqueue(@NonNull WorkRequest... workRequests)
            throws InterruptedException, ExecutionException {
        mWorkManagerImpl.enqueueInternal(Arrays.asList(workRequests)).getResult().get();
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param requests One or more {@link WorkRequest}s to enqueue
     * @throws InterruptedException Thrown when the thread enqueueing the {@link WorkRequest}s is
     *                              interrupted.
     * @throws ExecutionException   Thrown when there is an error executing the request to enqueue
     *                              {@link WorkRequest}s.
     */
    @WorkerThread
    public void enqueue(@NonNull List<? extends WorkRequest> requests)
            throws InterruptedException, ExecutionException {
        mWorkManagerImpl.enqueueInternal(requests).getResult().get();
    }

    /**
     * This method allows you to enqueue {@code work} requests to a uniquely-named
     * {@link WorkContinuation}, where only one continuation of a particular name can be active at
     * a time. For example, you may only want one sync operation to be active. If there is one
     * pending, you can choose to let it run or replace it with your new work.
     *
     * <p>
     * The {@code uniqueWorkName} uniquely identifies this {@link WorkContinuation}.
     * </p>
     *
     * @param uniqueWorkName     A unique name which for this operation
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}; see below for more information
     * @param work               {@link OneTimeWorkRequest}s to enqueue. {@code REPLACE} ensures
     *                           that if there is pending work labelled with {@code uniqueWorkName},
     *                           it will be cancelled and the new work will run. {@code KEEP} will
     *                           run the new OneTimeWorkRequests only if there is no pending work
     *                           labelled with {@code uniqueWorkName}. {@code APPEND} will append
     *                           the OneTimeWorkRequests as leaf nodes labelled with
     *                           {@code uniqueWorkName}.
     * @throws InterruptedException Thrown when the thread enqueueing the
     *                              {@link OneTimeWorkRequest}s is interrupted.
     * @throws ExecutionException   Thrown when there is an error executing the request to enqueue
     *                              {@link OneTimeWorkRequest}s.
     */
    @WorkerThread
    public void enqueueUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull OneTimeWorkRequest...work)
            throws InterruptedException, ExecutionException {
        mWorkManagerImpl.enqueueUniqueWorkInternal(
                uniqueWorkName, existingWorkPolicy, Arrays.asList(work)).getResult().get();
    }

    /**
     * This method allows you to enqueue {@code work} requests to a uniquely-named
     * {@link WorkContinuation}, where only one continuation of a particular name can be active at
     * a time. For example, you may only want one sync operation to be active. If there is one
     * pending, you can choose to let it run or replace it with your new work.
     *
     * <p>
     * The {@code uniqueWorkName} uniquely identifies this {@link WorkContinuation}.
     * </p>
     *
     * @param uniqueWorkName     A unique name which for this operation
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}; see below for more information
     * @param work               {@link OneTimeWorkRequest}s to enqueue. {@code REPLACE} ensures
     *                           that if there is pending work labelled with {@code uniqueWorkName},
     *                           it will be cancelled and the new work will run. {@code KEEP} will
     *                           run the new OneTimeWorkRequests only if there is no pending work
     *                           labelled with {@code uniqueWorkName}. {@code APPEND} will append
     *                           the OneTimeWorkRequests as leaf nodes labelled with
     *                           {@code uniqueWorkName}.
     * @throws InterruptedException Thrown when the thread enqueueing the
     *                              {@link OneTimeWorkRequest}s is interrupted.
     * @throws ExecutionException   Thrown when there is an error executing the request to enqueue
     *                              {@link OneTimeWorkRequest}s.
     */
    @WorkerThread
    public void enqueueUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work)
            throws InterruptedException, ExecutionException {
        mWorkManagerImpl.enqueueUniqueWorkInternal(uniqueWorkName, existingWorkPolicy, work)
                .getResult()
                .get();
    }

    /**
     * This method allows you to enqueue a uniquely-named {@link PeriodicWorkRequest}, where only
     * one PeriodicWorkRequest of a particular name can be active at a time.  For example, you may
     * only want one sync operation to be active.  If there is one pending, you can choose to let it
     * run or replace it with your new work.
     *
     * <p>
     * The {@code uniqueWorkName} uniquely identifies this PeriodicWorkRequest.
     * </p>
     *
     * @param uniqueWorkName             A unique name which for this operation
     * @param existingPeriodicWorkPolicy An {@link ExistingPeriodicWorkPolicy}
     * @param periodicWork               A {@link PeriodicWorkRequest} to enqueue. {@code REPLACE}
     *                                   ensures that if there is pending work labelled with
     *                                   {@code uniqueWorkName}, it will be cancelled and the new
     *                                   work will run. {@code KEEP} will run the new
     *                                   PeriodicWorkRequest only if there is no pending work
     *                                   labelled with {@code uniqueWorkName}.
     * @throws InterruptedException Thrown when the thread enqueueing the
     *                              {@link PeriodicWorkRequest} is interrupted.
     * @throws ExecutionException   Thrown when there is an error executing the request to enqueue
     *                              {@link PeriodicWorkRequest}.
     */
    @WorkerThread
    public void enqueueUniquePeriodicWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingPeriodicWorkPolicy existingPeriodicWorkPolicy,
            @NonNull PeriodicWorkRequest periodicWork)
            throws InterruptedException, ExecutionException {
        mWorkManagerImpl.enqueueUniquePeriodicWorkInternal(
                uniqueWorkName, existingPeriodicWorkPolicy, periodicWork)
                .getResult()
                .get();
    }

    /**
     * Cancels work with the given id if it isn't finished.  Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run.
     *
     * @param id The id of the work completed
     * @throws InterruptedException Thrown when the thread cancelling work by id is interrupted.
     * @throws ExecutionException   Thrown when there is an error executing the request to cancel
     *                              work by id.
     */
    @WorkerThread
    public void cancelWorkById(@NonNull UUID id)
            throws InterruptedException, ExecutionException {
        mWorkManagerImpl.cancelWorkByIdInternal(id).get();
    }

    /**
     * Cancels all unfinished work with the given tag.  Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run.
     *
     * @param tag The tag used to identify the work
     * @throws InterruptedException Thrown when the thread cancelling work by tag is interrupted.
     * @throws ExecutionException   Thrown when there is an error executing the request to cancel
     *                              work by tag.
     */
    @WorkerThread
    public void cancelAllWorkByTag(@NonNull String tag)
            throws InterruptedException, ExecutionException {
        mWorkManagerImpl.cancelAllWorkByTagInternal(tag).get();
    }

    /**
     * Cancels all unfinished work in the work chain with the given name.  Note that cancellation is
     * a best-effort policy and work that is already executing may continue to run.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     * @throws InterruptedException Thrown when the thread cancelling unique work is interrupted.
     * @throws ExecutionException   Thrown when there is an error executing the request to cancel
     *                              unique work.
     */
    @WorkerThread
    public void cancelUniqueWork(@NonNull String uniqueWorkName)
            throws InterruptedException, ExecutionException {
        mWorkManagerImpl.cancelUniqueWorkInternal(uniqueWorkName).get();
    }

    /**
     * Cancels all unfinished work.  <b>Use this method with extreme caution!</b>  By invoking it,
     * you will potentially affect other modules or libraries in your codebase.  It is strongly
     * recommended that you use one of the other cancellation methods at your disposal.
     * @throws InterruptedException Thrown when the thread cancelling all work is interrupted.
     * @throws ExecutionException   Thrown when there is an error executing the request to cancel
     *                              all work.
     */
    @WorkerThread
    public void cancelAllWork() throws InterruptedException, ExecutionException {
        mWorkManagerImpl.cancelAllWorkInternal().get();
    }

    /**
     * Prunes all eligible finished work from the internal database.  Eligible work must be finished
     * ({@link State#SUCCEEDED}, {@link State#FAILED}, or {@link State#CANCELLED}), with zero
     * unfinished dependents.
     * <p>
     * <b>Use this method with caution</b>; by invoking it, you (and any modules and libraries in
     * your codebase) will no longer be able to observe the {@link WorkStatus} of the pruned work.
     * You do not normally need to call this method - WorkManager takes care to auto-prune its work
     * after a sane period of time.  This method also ignores the
     * {@link OneTimeWorkRequest.Builder#keepResultsForAtLeast(long, TimeUnit)} policy.
     *
     * @throws InterruptedException Thrown when the thread pruning all work is interrupted.
     * @throws ExecutionException   Thrown when there is an error executing the request to prune all
     *                              work.
     */
    @WorkerThread
    public void pruneWork() throws InterruptedException, ExecutionException {
        mWorkManagerImpl.pruneWorkInternal().get();
    }
}
