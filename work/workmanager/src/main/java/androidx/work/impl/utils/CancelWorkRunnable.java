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

import static androidx.work.State.CANCELLED;
import static androidx.work.State.FAILED;
import static androidx.work.State.SUCCEEDED;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import androidx.work.State;
import androidx.work.impl.Processor;
import androidx.work.impl.Scheduler;
import androidx.work.impl.Schedulers;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.WorkSpecDao;

import java.util.List;
import java.util.UUID;

/**
 * A {@link Runnable} to cancel work.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class CancelWorkRunnable implements Runnable {

    void cancel(WorkManagerImpl workManagerImpl, String workSpecId) {
        recursivelyCancelWorkAndDependents(workManagerImpl.getWorkDatabase(), workSpecId);

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

    private void recursivelyCancelWorkAndDependents(WorkDatabase workDatabase, String workSpecId) {

        WorkSpecDao workSpecDao = workDatabase.workSpecDao();
        DependencyDao dependencyDao = workDatabase.dependencyDao();

        List<String> dependentIds = dependencyDao.getDependentWorkIds(workSpecId);
        for (String id : dependentIds) {
            recursivelyCancelWorkAndDependents(workDatabase, id);
        }

        State state = workSpecDao.getState(workSpecId);
        if (state != SUCCEEDED && state != FAILED) {
            workSpecDao.setState(CANCELLED, workSpecId);
        }
    }

    /**
     * Creates a {@link CancelWorkRunnable} that cancels work for a specific id.
     *
     * @param id The id to cancel
     * @param workManagerImpl The {@link WorkManagerImpl} to use
     * @return A {@link Runnable} that cancels work for a specific id
     */
    public static Runnable forId(
            @NonNull final UUID id,
            @NonNull final WorkManagerImpl workManagerImpl) {
        return new CancelWorkRunnable() {
            @WorkerThread
            @Override
            public void run() {
                cancel(workManagerImpl, id.toString());
                reschedulePendingWorkers(workManagerImpl);
            }
        };
    }

    /**
     * Creates a {@link CancelWorkRunnable} that cancels work for a specific tag.
     *
     * @param tag The tag to cancel
     * @param workManagerImpl The {@link WorkManagerImpl} to use
     * @return A {@link Runnable} that cancels work for a specific tag
     */
    public static Runnable forTag(
            @NonNull final String tag,
            @NonNull final WorkManagerImpl workManagerImpl) {
        return new CancelWorkRunnable() {
            @WorkerThread
            @Override
            public void run() {
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
     * @return A {@link Runnable} that cancels work labelled with a specific name
     */
    public static Runnable forName(
            @NonNull final String name,
            @NonNull final WorkManagerImpl workManagerImpl) {
        return new CancelWorkRunnable() {
            @WorkerThread
            @Override
            public void run() {
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
                reschedulePendingWorkers(workManagerImpl);
            }
        };
    }

    /**
     * Creates a {@link CancelWorkRunnable} that cancels all work.
     *
     * @param workManagerImpl The {@link WorkManagerImpl} to use
     * @return A {@link Runnable} that cancels all work
     */
    public static Runnable forAll(@NonNull final WorkManagerImpl workManagerImpl) {
        return new CancelWorkRunnable() {
            @Override
            public void run() {
                WorkDatabase workDatabase = workManagerImpl.getWorkDatabase();
                workDatabase.beginTransaction();
                try {
                    WorkSpecDao workSpecDao = workDatabase.workSpecDao();
                    List<String> workSpecIds = workSpecDao.getAllUnfinishedWork();
                    for (String workSpecId : workSpecIds) {
                        cancel(workManagerImpl, workSpecId);
                    }
                    workDatabase.setTransactionSuccessful();
                    // Update the preferences
                    new Preferences(workManagerImpl.getApplicationContext())
                            .setLastCancelAllTimeMillis(System.currentTimeMillis());
                } finally {
                    workDatabase.endTransaction();
                }
                // No need to call reschedule pending workers here as we just cancelled everything.
            }
        };
    }
}
