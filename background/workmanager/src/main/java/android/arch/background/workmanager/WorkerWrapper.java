/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.background.workmanager;

import static android.arch.background.workmanager.BaseWork.STATUS_BLOCKED;
import static android.arch.background.workmanager.BaseWork.STATUS_ENQUEUED;
import static android.arch.background.workmanager.BaseWork.STATUS_FAILED;
import static android.arch.background.workmanager.BaseWork.STATUS_RUNNING;
import static android.arch.background.workmanager.BaseWork.STATUS_SUCCEEDED;
import static android.arch.background.workmanager.Worker.WORKER_RESULT_FAILURE;
import static android.arch.background.workmanager.Worker.WORKER_RESULT_RETRY;
import static android.arch.background.workmanager.Worker.WORKER_RESULT_SUCCESS;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.arch.background.workmanager.model.DependencyDao;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.model.WorkSpecDao;
import android.arch.background.workmanager.utils.taskexecutor.WorkManagerTaskExecutor;
import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.lang.annotation.Retention;

/**
 * A runnable that looks up the {@link WorkSpec} from the database for a given id, instantiates
 * its Worker, and then calls it.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkerWrapper implements Runnable {
    @Retention(SOURCE)
    @IntDef({EXECUTION_RESULT_PERMANENT_ERROR,
            EXECUTION_RESULT_RESCHEDULE,
            EXECUTION_RESULT_FAILURE,
            EXECUTION_RESULT_SUCCESS})
    public @interface ExecutionResult {
    }

    public static final int EXECUTION_RESULT_PERMANENT_ERROR = 0;
    public static final int EXECUTION_RESULT_RESCHEDULE = 1;
    public static final int EXECUTION_RESULT_FAILURE = 2;
    public static final int EXECUTION_RESULT_SUCCESS = 3;

    private static final String TAG = "WorkerWrapper";
    private Context mAppContext;
    private WorkDatabase mWorkDatabase;
    private String mWorkSpecId;
    private ExecutionListener mListener;
    private Scheduler mScheduler;

    private WorkerWrapper(Builder builder) {
        mAppContext = builder.mAppContext;
        mWorkDatabase = builder.mWorkDatabase;
        mWorkSpecId = builder.mWorkSpecId;
        mListener = builder.mListener;
        mScheduler = builder.mScheduler;
    }

    @Override
    public void run() {
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        WorkSpec workSpec = workSpecDao.getWorkSpec(mWorkSpecId);
        if (workSpec == null) {
            Log.e(TAG, "Didn't find WorkSpec for id " + mWorkSpecId);
            notifyListener(EXECUTION_RESULT_PERMANENT_ERROR);
            return;
        }

        switch (workSpec.getStatus()) {
            case STATUS_BLOCKED: {
                Log.e(TAG,
                        "Status for " + mWorkSpecId + " is BLOCKED - why is it trying to "
                                + "be executed?  This is a recoverable error, so not doing any "
                                + "work and rescheduling for later execution",
                        new Exception());
                notifyListener(EXECUTION_RESULT_RESCHEDULE);
                return;
            }

            case STATUS_RUNNING: {
                Log.d(TAG, "Status for " + mWorkSpecId + " is BLOCKED or RUNNING; "
                        + "not doing any work and rescheduling for later execution");
                notifyListener(EXECUTION_RESULT_RESCHEDULE);
                return;
            }

            case STATUS_SUCCEEDED: {
                Log.d(TAG, "Status for " + mWorkSpecId + " is succeeded; not doing any work");
                notifyListener(EXECUTION_RESULT_SUCCESS);
                return;
            }

            case STATUS_FAILED: {
                Log.d(TAG, "Status for " + mWorkSpecId + " is failed; not doing any work");
                notifyListener(EXECUTION_RESULT_FAILURE);
                return;
            }
        }

        Worker worker = Worker.fromWorkSpec(mAppContext, workSpec);
        if (worker == null) {
            Log.e(TAG, "Could not create Worker " + workSpec.getWorkerClassName());
            workSpecDao.setStatus(STATUS_FAILED, mWorkSpecId);
            notifyListener(EXECUTION_RESULT_PERMANENT_ERROR);
            return;
        }

        mWorkDatabase.beginTransaction();
        try {
            workSpecDao.setStatus(STATUS_RUNNING, mWorkSpecId);
            workSpecDao.incrementWorkSpecRunAttemptCount(mWorkSpecId);
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
        }

        try {
            checkForInterruption();
            int result = worker.doWork();
            checkForInterruption();

            switch (result) {
                case WORKER_RESULT_SUCCESS: {
                    Log.d(TAG, "Worker result SUCCESS for " + mWorkSpecId);
                    setSuccessAndUpdateDependencies(workSpec.isPeriodic());
                    notifyListener(EXECUTION_RESULT_SUCCESS);
                    break;
                }

                case WORKER_RESULT_RETRY: {
                    Log.d(TAG, "Worker result RETRY for " + mWorkSpecId);
                    workSpecDao.setStatus(STATUS_ENQUEUED, mWorkSpecId);
                    notifyListener(EXECUTION_RESULT_RESCHEDULE);
                    break;
                }

                case WORKER_RESULT_FAILURE:
                default: {
                    Log.d(TAG, "Worker result FAILURE for " + mWorkSpecId);
                    workSpecDao.setStatus(STATUS_FAILED, mWorkSpecId);
                    notifyListener(EXECUTION_RESULT_FAILURE);
                    break;
                }
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "Work interrupted for " + mWorkSpecId);
            workSpecDao.setStatus(STATUS_ENQUEUED, mWorkSpecId);
            notifyListener(EXECUTION_RESULT_RESCHEDULE);
        }
    }

    private void checkForInterruption() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    private void notifyListener(@ExecutionResult final int result) {
        if (mListener == null) {
            return;
        }
        WorkManagerTaskExecutor.getInstance().postToMainThread(new Runnable() {
            @Override
            public void run() {
                mListener.onExecuted(mWorkSpecId, result);
            }
        });
    }

    private void setSuccessAndUpdateDependencies(boolean isPeriodicWork) {
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        DependencyDao dependencyDao = mWorkDatabase.dependencyDao();

        mWorkDatabase.beginTransaction();
        try {
            if (isPeriodicWork) { // For periodic jobs a success means to be set back to enqueued.
                workSpecDao.setStatus(STATUS_ENQUEUED, mWorkSpecId);
                workSpecDao.resetWorkSpecRunAttemptCount(mWorkSpecId);
            } else {
                workSpecDao.setStatus(STATUS_SUCCEEDED, mWorkSpecId);
            }
            dependencyDao.deleteDependenciesWithPrerequisite(mWorkSpecId);
            String[] unblockedWorkIds = workSpecDao.getUnblockedWorkIds();
            int unblockedWorkCount = unblockedWorkIds.length;
            if (unblockedWorkCount > 0) {
                Log.d(TAG, "Setting status to enqueued for " + unblockedWorkCount + " Works "
                        + "that were dependent on Work ID " + mWorkSpecId);
                workSpecDao.setStatus(STATUS_ENQUEUED, unblockedWorkIds);
            }
            mWorkDatabase.setTransactionSuccessful();

            if (mScheduler != null) {
                WorkSpec[] unblockedWorkSpecs = workSpecDao.getWorkSpecs(unblockedWorkIds);
                mScheduler.schedule(unblockedWorkSpecs);
            }
        } finally {
            mWorkDatabase.endTransaction();
        }
    }

    /**
     * Builder class for {@link WorkerWrapper}
     */
    static class Builder {
        private Context mAppContext;
        private WorkDatabase mWorkDatabase;
        private String mWorkSpecId;
        private ExecutionListener mListener;
        private Scheduler mScheduler;

        Builder(@NonNull Context context,
                @NonNull WorkDatabase database,
                @NonNull String workSpecId) {
            mAppContext = context.getApplicationContext();
            mWorkDatabase = database;
            mWorkSpecId = workSpecId;
        }

        Builder withListener(ExecutionListener listener) {
            mListener = listener;
            return this;
        }

        Builder withScheduler(Scheduler scheduler) {
            mScheduler = scheduler;
            return this;
        }

        WorkerWrapper build() {
            return new WorkerWrapper(this);
        }
    }
}
