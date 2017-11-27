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
import static android.arch.background.workmanager.BaseWork.STATUS_CANCELLED;
import static android.arch.background.workmanager.BaseWork.STATUS_ENQUEUED;
import static android.arch.background.workmanager.BaseWork.STATUS_FAILED;
import static android.arch.background.workmanager.BaseWork.STATUS_RUNNING;
import static android.arch.background.workmanager.BaseWork.STATUS_SUCCEEDED;
import static android.arch.background.workmanager.Worker.WORKER_RESULT_FAILURE;
import static android.arch.background.workmanager.Worker.WORKER_RESULT_RETRY;
import static android.arch.background.workmanager.Worker.WORKER_RESULT_SUCCESS;

import android.arch.background.workmanager.model.Arguments;
import android.arch.background.workmanager.model.DependencyDao;
import android.arch.background.workmanager.model.WorkInput;
import android.arch.background.workmanager.model.WorkInputDao;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.model.WorkSpecDao;
import android.arch.background.workmanager.utils.taskexecutor.WorkManagerTaskExecutor;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.util.List;

/**
 * A runnable that looks up the {@link WorkSpec} from the database for a given id, instantiates
 * its Worker, and then calls it.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkerWrapper implements Runnable {

    private static final String TAG = "WorkerWrapper";
    private Context mAppContext;
    private String mWorkSpecId;
    private ExecutionListener mListener;
    private Scheduler mScheduler;
    private WorkSpec mWorkSpec;
    private Worker mWorker;

    private WorkDatabase mWorkDatabase;
    private WorkSpecDao mWorkSpecDao;
    private DependencyDao mDependencyDao;
    private WorkInputDao mWorkInputDao;

    private WorkerWrapper(Builder builder) {
        mAppContext = builder.mAppContext;
        mWorkSpecId = builder.mWorkSpecId;
        mListener = builder.mListener;
        mScheduler = builder.mScheduler;

        mWorkDatabase = builder.mWorkDatabase;
        mWorkSpecDao = mWorkDatabase.workSpecDao();
        mDependencyDao = mWorkDatabase.dependencyDao();
        mWorkInputDao = mWorkDatabase.workInputDao();
    }

    @WorkerThread
    @Override
    public void run() {
        mWorkSpec = mWorkSpecDao.getWorkSpec(mWorkSpecId);
        if (mWorkSpec == null) {
            Log.e(TAG, "Didn't find WorkSpec for id " + mWorkSpecId);
            notifyListener(false);
            return;
        }

        switch (mWorkSpec.getStatus()) {
            case STATUS_RUNNING: {
                Log.d(TAG, "Status for " + mWorkSpecId + " is RUNNING; "
                        + "not doing any work and rescheduling for later execution");
                notifyListener(true);
                return;
            }

            case STATUS_BLOCKED: {
                Log.e(TAG,
                        "Status for " + mWorkSpecId + " is BLOCKED - why is it trying to be "
                                + "executed?  This is a recoverable error, so not doing any work",
                        new Exception());
                notifyListener(false);
                return;
            }

            case STATUS_SUCCEEDED: {
                Log.d(TAG, "Status for " + mWorkSpecId + " is succeeded; not doing any work");
                notifyListener(false);
                return;
            }

            case STATUS_FAILED: {
                Log.d(TAG, "Status for " + mWorkSpecId + " is failed; not doing any work");
                notifyListener(false);
                return;
            }

            case STATUS_CANCELLED: {
                Log.d(TAG, "Status for " + mWorkSpecId + " is cancelled; not doing any work");
                notifyListener(false);
                return;
            }
        }

        List<Arguments> argumentsList = mWorkInputDao.getArguments(mWorkSpecId);
        // TODO(sumir): This isn't the final implementation.  Clean this up in the next CL.
        for (int i = 1; i < argumentsList.size(); ++i) {
            argumentsList.get(0).merge(argumentsList.get(i));
        }
        mWorker = Worker.fromWorkSpec(mAppContext, mWorkSpec, argumentsList.get(0));
        if (mWorker == null) {
            Log.e(TAG, "Could not create Worker " + mWorkSpec.getWorkerClassName());
            mWorkSpecDao.setStatus(STATUS_FAILED, mWorkSpecId);
            notifyListener(false);
            return;
        }

        mWorkDatabase.beginTransaction();
        try {
            mWorkSpecDao.setStatus(STATUS_RUNNING, mWorkSpecId);
            mWorkSpecDao.incrementWorkSpecRunAttemptCount(mWorkSpecId);
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
        }

        try {
            checkForInterruption();
            int result = mWorker.doWork();
            if (mWorkSpecDao.getWorkSpecStatus(mWorkSpecId) != STATUS_CANCELLED) {
                checkForInterruption();
                setStatusAndNotify(result);
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "Work interrupted for " + mWorkSpecId);
            mWorkSpecDao.setStatus(STATUS_ENQUEUED, mWorkSpecId);
            notifyListener(true);
        }
    }

    private void checkForInterruption() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    private void notifyListener(final boolean needsReschedule) {
        if (mListener == null) {
            return;
        }
        WorkManagerTaskExecutor.getInstance().postToMainThread(new Runnable() {
            @Override
            public void run() {
                mListener.onExecuted(mWorkSpecId, needsReschedule);
            }
        });
    }

    private void setStatusAndNotify(@Worker.WorkerResult int result) {
        switch (result) {
            case WORKER_RESULT_SUCCESS: {
                Log.d(TAG, "Worker result SUCCESS for " + mWorkSpecId);
                if (mWorkSpec.isPeriodic()) {
                    setEnqueuedAndResetRunAttemptCount();
                } else {
                    setSuccessAndUpdateDependencies();
                }
                notifyListener(false);
                break;
            }

            case WORKER_RESULT_RETRY: {
                Log.d(TAG, "Worker result RETRY for " + mWorkSpecId);
                mWorkSpecDao.setStatus(STATUS_ENQUEUED, mWorkSpecId);
                notifyListener(true);
                break;
            }

            case WORKER_RESULT_FAILURE:
            default: {
                Log.d(TAG, "Worker result FAILURE for " + mWorkSpecId);
                if (mWorkSpec.isPeriodic()) {
                    setEnqueuedAndResetRunAttemptCount();
                } else {
                    mWorkSpecDao.setStatus(STATUS_FAILED, mWorkSpecId);
                }
                notifyListener(false);
                break;
            }
        }
    }

    private void setEnqueuedAndResetRunAttemptCount() {
        mWorkDatabase.beginTransaction();
        try {
            mWorkSpecDao.setStatus(STATUS_ENQUEUED, mWorkSpecId);
            mWorkSpecDao.resetWorkSpecRunAttemptCount(mWorkSpecId);
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
        }
    }

    private void setSuccessAndUpdateDependencies() {
        mWorkDatabase.beginTransaction();
        try {
            mWorkSpecDao.setStatus(STATUS_SUCCEEDED, mWorkSpecId);
            mDependencyDao.deleteDependenciesWithPrerequisite(mWorkSpecId);
            String[] unblockedWorkIds = mWorkSpecDao.getUnblockedWorkIds();
            int unblockedWorkCount = unblockedWorkIds.length;
            if (unblockedWorkCount > 0) {
                Log.d(TAG, "Setting status to enqueued for " + unblockedWorkCount + " Works "
                        + "that were dependent on Work ID " + mWorkSpecId);
                mWorkSpecDao.setStatus(STATUS_ENQUEUED, unblockedWorkIds);

                // Update Arguments as necessary.
                Arguments outputArgs = mWorker.getOutput();
                if (outputArgs != null) {
                    for (String id : unblockedWorkIds) {
                        mWorkInputDao.insert(new WorkInput(id, outputArgs));
                    }
                }
            }
            mWorkDatabase.setTransactionSuccessful();

            if (mScheduler != null) {
                WorkSpec[] unblockedWorkSpecs = mWorkSpecDao.getWorkSpecs(unblockedWorkIds);
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
