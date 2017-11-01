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

import static android.arch.background.workmanager.Work.STATUS_ENQUEUED;
import static android.arch.background.workmanager.Work.STATUS_FAILED;
import static android.arch.background.workmanager.Work.STATUS_RUNNING;
import static android.arch.background.workmanager.Work.STATUS_SUCCEEDED;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.arch.background.workmanager.model.DependencyDao;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.model.WorkSpecDao;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.lang.annotation.Retention;
import java.util.List;

/**
 * A runnable that looks up the {@link WorkSpec} from the database for a given id, instantiates
 * its Worker, and then calls it.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkerWrapper implements Runnable {
    @Retention(SOURCE)
    @IntDef({RESULT_NOT_ENQUEUED, RESULT_PERMANENT_ERROR, RESULT_FAILED, RESULT_INTERRUPTED,
            RESULT_SUCCEEDED, RESULT_RESCHEDULED})
    public @interface ExecutionResult {
    }

    public static final int RESULT_NOT_ENQUEUED = 0;
    public static final int RESULT_PERMANENT_ERROR = 1;
    public static final int RESULT_FAILED = 2;
    public static final int RESULT_INTERRUPTED = 3;
    public static final int RESULT_SUCCEEDED = 4;
    public static final int RESULT_RESCHEDULED = 5;

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
            notifyListener(RESULT_PERMANENT_ERROR);
            return;
        }

        if (workSpec.getStatus() != STATUS_ENQUEUED) {
            Log.d(TAG, "Status for " + mWorkSpecId + " is not enqueued; not doing any work");
            notifyListener(RESULT_NOT_ENQUEUED);
            return;
        }

        Worker worker = Worker.fromWorkSpec(mAppContext, workSpec);
        if (worker == null) {
            Log.e(TAG, "Could not create Worker " + workSpec.getWorkerClassName());
            workSpecDao.setWorkSpecStatus(mWorkSpecId, STATUS_FAILED);
            notifyListener(RESULT_PERMANENT_ERROR);
            return;
        }

        mWorkDatabase.beginTransaction();
        try {
            workSpecDao.setWorkSpecStatus(mWorkSpecId, STATUS_RUNNING);
            workSpecDao.incrementWorkSpecRunAttemptCount(mWorkSpecId);
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
        }

        try {
            checkForInterruption();
            worker.doWork();
            checkForInterruption();

            Log.d(TAG, "Work succeeded for " + mWorkSpecId);
            setSuccessAndUpdateDependencies(workSpec.isPeriodic());
            notifyListener(RESULT_SUCCEEDED);
        } catch (InterruptedException e) {
            // TODO(xbhatnag): Retry Policies
            Log.d(TAG, "Work interrupted for " + mWorkSpecId);
            workSpecDao.setWorkSpecStatus(mWorkSpecId, STATUS_ENQUEUED);
            notifyListener(RESULT_INTERRUPTED);
        } catch (Exception e) {
            Log.d(TAG, "Work failed for " + mWorkSpecId, e);
            workSpecDao.setWorkSpecStatus(mWorkSpecId, STATUS_FAILED);
            notifyListener(RESULT_FAILED);
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
        new Handler(Looper.getMainLooper()).post(new Runnable() {
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
                workSpecDao.setWorkSpecStatus(mWorkSpecId, STATUS_ENQUEUED);
                workSpecDao.resetWorkSpecRunAttemptCount(mWorkSpecId);
            } else {
                workSpecDao.setWorkSpecStatus(mWorkSpecId, STATUS_SUCCEEDED);
            }
            dependencyDao.deleteDependenciesWithPrerequisite(mWorkSpecId);
            List<String> unblockedWorkIds = workSpecDao.getUnblockedWorkIds();
            int unblockedWorkCount = unblockedWorkIds.size();
            if (unblockedWorkCount > 0) {
                Log.d(TAG, "Setting status to enqueued for " + unblockedWorkCount + " Works "
                        + "that were dependent on Work ID " + mWorkSpecId);
                workSpecDao.setWorkSpecStatus(unblockedWorkIds, Work.STATUS_ENQUEUED);
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
