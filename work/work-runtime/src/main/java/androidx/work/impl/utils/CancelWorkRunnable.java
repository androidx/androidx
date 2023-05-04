/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.work.impl.utils;

import static androidx.work.WorkInfo.State.CANCELLED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.SUCCEEDED;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.work.Operation;
import androidx.work.WorkInfo;
import androidx.work.impl.OperationImpl;
import androidx.work.impl.Processor;
import androidx.work.impl.Scheduler;
import androidx.work.impl.Schedulers;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.WorkSpecDao;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * A {@link Runnable} to cancel work.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class CancelWorkRunnable implements Runnable {

    private final OperationImpl mOperation = new OperationImpl();

    /**
     * @return The {@link Operation} that encapsulates the state of the {@link CancelWorkRunnable}.
     */
    @NonNull
    public Operation getOperation() {
        return mOperation;
    }

    @Override
    public void run() {
        try {
            runInternal();
            mOperation.markState(Operation.SUCCESS);
        } catch (Throwable throwable) {
            mOperation.markState(new Operation.State.FAILURE(throwable));
        }
    }

    abstract void runInternal();

    void cancel(WorkManagerImpl workManagerImpl, String workSpecId) {
        iterativelyCancelWorkAndDependents(workManagerImpl.getWorkDatabase(), workSpecId);

        Processor processor = workManagerImpl.getProcessor();
        processor.stopAndCancelWork(workSpecId);

        for (Scheduler scheduler : workManagerImpl.getSchedulers()) {
            scheduler.cancel(workSpecId);
        }
    }

    void reschedulePendingWorkers(WorkManagerImpl workManagerImpl) {
        Schedulers.schedule(
                workManagerImpl.getConfiguration(),
                workManagerImpl.getWorkDatabase(),
                workManagerImpl.getSchedulers());
    }

    private void iterativelyCancelWorkAndDependents(WorkDatabase workDatabase, String workSpecId) {
        WorkSpecDao workSpecDao = workDatabase.workSpecDao();
        DependencyDao dependencyDao = workDatabase.dependencyDao();

        @SuppressWarnings("JdkObsolete") // TODO(b/141962522): Suppressed during upgrade to AGP 3.6.
        LinkedList<String> idsToProcess = new LinkedList<>();
        idsToProcess.add(workSpecId);
        while (!idsToProcess.isEmpty()) {
            String id = idsToProcess.remove();
            // Don't fail already cancelled work.
            WorkInfo.State state = workSpecDao.getState(id);
            if (state != SUCCEEDED && state != FAILED) {
                workSpecDao.setState(CANCELLED, id);
            }
            idsToProcess.addAll(dependencyDao.getDependentWorkIds(id));
        }
    }

    /**
     * Creates a {@link CancelWorkRunnable} that cancels work for a specific id.
     *
     * @param id The id to cancel
     * @param workManagerImpl The {@link WorkManagerImpl} to use
     * @return A {@link CancelWorkRunnable} that cancels work for a specific id
     */
    @NonNull
    public static CancelWorkRunnable forId(
            @NonNull final UUID id,
            @NonNull final WorkManagerImpl workManagerImpl) {
        return new CancelWorkRunnable() {
            @WorkerThread
            @Override
            void runInternal() {
                WorkDatabase workDatabase = workManagerImpl.getWorkDatabase();
                workDatabase.beginTransaction();
                try {
                    cancel(workManagerImpl, id.toString());
                    workDatabase.setTransactionSuccessful();
                } finally {
                    workDatabase.endTransaction();
                }
                reschedulePendingWorkers(workManagerImpl);
            }
        };
    }

    /**
     * Creates a {@link CancelWorkRunnable} that cancels work for a specific tag.
     *
     * @param tag The tag to cancel
     * @param workManagerImpl The {@link WorkManagerImpl} to use
     * @return A {@link CancelWorkRunnable} that cancels work for a specific tag
     */
    @NonNull
    public static CancelWorkRunnable forTag(
            @NonNull final String tag,
            @NonNull final WorkManagerImpl workManagerImpl) {
        return new CancelWorkRunnable() {
            @WorkerThread
            @Override
            void runInternal() {
                WorkDatabase workDatabase = workManagerImpl.getWorkDatabase();
                workDatabase.beginTransaction();
                try {
                    WorkSpecDao workSpecDao = workDatabase.workSpecDao();
                    List<String> workSpecIds = workSpecDao.getUnfinishedWorkWithTag(tag);
                    for (String workSpecId : workSpecIds) {
                        cancel(workManagerImpl, workSpecId);
                    }
                    workDatabase.setTransactionSuccessful();
                } finally {
                    workDatabase.endTransaction();
                }
                reschedulePendingWorkers(workManagerImpl);
            }
        };
    }

    /**
     * Creates a {@link CancelWorkRunnable} that cancels work labelled with a specific name.
     *
     * @param name The name to cancel
     * @param workManagerImpl The {@link WorkManagerImpl} to use
     * @param allowReschedule If {@code true}, reschedule pending workers at the end
     * @return A {@link CancelWorkRunnable} that cancels work labelled with a specific name
     */
    @NonNull
    public static CancelWorkRunnable forName(
            @NonNull final String name,
            @NonNull final WorkManagerImpl workManagerImpl,
            final boolean allowReschedule) {
        return new CancelWorkRunnable() {
            @WorkerThread
            @Override
            void runInternal() {
                WorkDatabase workDatabase = workManagerImpl.getWorkDatabase();
                workDatabase.beginTransaction();
                try {
                    WorkSpecDao workSpecDao = workDatabase.workSpecDao();
                    List<String> workSpecIds = workSpecDao.getUnfinishedWorkWithName(name);
                    for (String workSpecId : workSpecIds) {
                        cancel(workManagerImpl, workSpecId);
                    }
                    workDatabase.setTransactionSuccessful();
                } finally {
                    workDatabase.endTransaction();
                }

                if (allowReschedule) {
                    reschedulePendingWorkers(workManagerImpl);
                }
            }
        };
    }

    /**
     * Creates a {@link CancelWorkRunnable} that cancels all work.
     *
     * @param workManagerImpl The {@link WorkManagerImpl} to use
     * @return A {@link CancelWorkRunnable} that cancels all work
     */
    @NonNull
    public static CancelWorkRunnable forAll(@NonNull final WorkManagerImpl workManagerImpl) {
        return new CancelWorkRunnable() {
            @WorkerThread
            @Override
            void runInternal() {
                WorkDatabase workDatabase = workManagerImpl.getWorkDatabase();
                workDatabase.beginTransaction();
                try {
                    WorkSpecDao workSpecDao = workDatabase.workSpecDao();
                    List<String> workSpecIds = workSpecDao.getAllUnfinishedWork();
                    for (String workSpecId : workSpecIds) {
                        cancel(workManagerImpl, workSpecId);
                    }
                    // Update the last cancelled time in Preference.
                    new PreferenceUtils(workManagerImpl.getWorkDatabase())
                            .setLastCancelAllTimeMillis(
                                    workManagerImpl.getConfiguration().getClock()
                                            .currentTimeMillis());
                    workDatabase.setTransactionSuccessful();
                } finally {
                    workDatabase.endTransaction();
                }
                // No need to call reschedule pending workers here as we just cancelled everything.
            }
        };
    }
}
