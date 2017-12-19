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

package android.arch.background.workmanager.impl;

import static android.arch.background.workmanager.BaseWork.STATUS_CANCELLED;
import static android.arch.background.workmanager.BaseWork.STATUS_ENQUEUED;
import static android.arch.background.workmanager.BaseWork.STATUS_FAILED;
import static android.arch.background.workmanager.BaseWork.STATUS_RUNNING;
import static android.arch.background.workmanager.BaseWork.STATUS_SUCCEEDED;
import static android.arch.background.workmanager.Worker.WORKER_RESULT_FAILURE;
import static android.arch.background.workmanager.Worker.WORKER_RESULT_RETRY;
import static android.arch.background.workmanager.Worker.WORKER_RESULT_SUCCESS;

import android.arch.background.workmanager.Arguments;
import android.arch.background.workmanager.InputMerger;
import android.arch.background.workmanager.Worker;
import android.arch.background.workmanager.impl.model.DependencyDao;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.impl.model.WorkSpecDao;
import android.arch.background.workmanager.impl.utils.taskexecutor.WorkManagerTaskExecutor;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
    Worker mWorker;

    private WorkDatabase mWorkDatabase;
    private WorkSpecDao mWorkSpecDao;
    private DependencyDao mDependencyDao;

    private WorkerWrapper(Builder builder) {
        mAppContext = builder.mAppContext;
        mWorkSpecId = builder.mWorkSpecId;
        mListener = builder.mListener;
        mScheduler = builder.mScheduler;

        mWorkDatabase = builder.mWorkDatabase;
        mWorkSpecDao = mWorkDatabase.workSpecDao();
        mDependencyDao = mWorkDatabase.dependencyDao();
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

        if (mWorkSpec.getStatus() != STATUS_ENQUEUED) {
            notifyIncorrectStatus();
            return;
        }

        Arguments arguments;
        if (mWorkSpec.isPeriodic()) {
            arguments = mWorkSpec.getArguments();
        } else {
            InputMerger inputMerger = InputMerger.fromClassName(
                    mWorkSpec.getInputMergerClassName());
            if (inputMerger == null) {
                Log.e(TAG, "Could not create Input Merger " + mWorkSpec.getInputMergerClassName());
                setFailedAndNotify();
                return;
            }
            List<Arguments> inputs = new ArrayList<>();
            inputs.add(mWorkSpec.getArguments());
            inputs.addAll(mWorkSpecDao.getInputsFromPrerequisites(mWorkSpecId));
            arguments = inputMerger.merge(inputs);
        }


        mWorker = workerFromWorkSpec(mAppContext, mWorkSpec, arguments);
        if (mWorker == null) {
            Log.e(TAG, "Could not create Worker " + mWorkSpec.getWorkerClassName());
            setFailedAndNotify();
            return;
        }

        setRunning();

        try {
            checkForInterruption();
            int result = mWorker.doWork();
            if (mWorkSpecDao.getWorkSpecStatus(mWorkSpecId) != STATUS_CANCELLED) {
                checkForInterruption();
                handleResult(result);
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "Work interrupted for " + mWorkSpecId);
            rescheduleAndNotify();
        }
    }

    private void notifyIncorrectStatus() {
        int status = mWorkSpec.getStatus();
        if (status == STATUS_RUNNING) {
            Log.d(TAG, "Status for " + mWorkSpecId + " is RUNNING; "
                    + "not doing any work and rescheduling for later execution");
            notifyListener(true);
        } else {
            Log.e(TAG, "Status for " + mWorkSpecId + " is " + status + "; not doing any work");
            notifyListener(false);
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

    private void handleResult(@Worker.WorkerResult int result) {
        switch (result) {
            case WORKER_RESULT_SUCCESS: {
                Log.d(TAG, "Worker result SUCCESS for " + mWorkSpecId);
                if (mWorkSpec.isPeriodic()) {
                    resetPeriodicAndNotify();
                } else {
                    setSucceededAndNotify();
                }
                break;
            }

            case WORKER_RESULT_RETRY: {
                Log.d(TAG, "Worker result RETRY for " + mWorkSpecId);
                rescheduleAndNotify();
                break;
            }

            case WORKER_RESULT_FAILURE:
            default: {
                Log.d(TAG, "Worker result FAILURE for " + mWorkSpecId);
                if (mWorkSpec.isPeriodic()) {
                    resetPeriodicAndNotify();
                } else {
                    setFailedAndNotify();
                }
            }
        }
    }

    private void setRunning() {
        mWorkDatabase.beginTransaction();
        try {
            mWorkSpecDao.setStatus(STATUS_RUNNING, mWorkSpecId);
            mWorkSpecDao.incrementWorkSpecRunAttemptCount(mWorkSpecId);
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
        }
    }

    private void setFailedAndNotify() {
        mWorkSpecDao.setStatus(STATUS_FAILED, mWorkSpecId);
        notifyListener(false);
    }

    private void rescheduleAndNotify() {
        mWorkDatabase.beginTransaction();
        try {
            mWorkSpecDao.setStatus(STATUS_ENQUEUED, mWorkSpecId);
            // TODO(xbhatnag): Period Start Time is confusing for non-periodic work. Rename.
            mWorkSpecDao.setPeriodStartTime(mWorkSpecId, System.currentTimeMillis());
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
            notifyListener(true);
        }
    }

    private void resetPeriodicAndNotify() {
        mWorkDatabase.beginTransaction();
        try {
            long currentPeriodStartTime = mWorkSpec.getPeriodStartTime();
            long nextPeriodStartTime = currentPeriodStartTime + mWorkSpec.getIntervalDuration();
            mWorkSpecDao.setPeriodStartTime(mWorkSpecId, nextPeriodStartTime);
            mWorkSpecDao.setStatus(STATUS_ENQUEUED, mWorkSpecId);
            mWorkSpecDao.resetWorkSpecRunAttemptCount(mWorkSpecId);
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
            notifyListener(false);
        }
    }

    private void setSucceededAndNotify() {
        mWorkDatabase.beginTransaction();
        try {
            mWorkSpecDao.setStatus(STATUS_SUCCEEDED, mWorkSpecId);

            // Update Arguments as necessary.
            Arguments outputArgs = mWorker.getOutput();
            if (outputArgs != null) {
                mWorkSpecDao.setOutput(mWorkSpecId, outputArgs);
            }

            // Unblock Dependencies and set Period Start Time
            long currentTimeMillis = System.currentTimeMillis();
            List<String> dependentWorkIds = mDependencyDao.getDependentWorkIds(mWorkSpecId);
            List<String> unblockedWorkIds = new ArrayList<>();
            for (String dependentWorkId : dependentWorkIds) {
                if (mDependencyDao.hasCompletedAllPrerequisites(dependentWorkId)) {
                    Log.d(TAG, "Setting status to enqueued for " + dependentWorkId);
                    mWorkSpecDao.setStatus(STATUS_ENQUEUED, dependentWorkId);
                    mWorkSpecDao.setPeriodStartTime(dependentWorkId, currentTimeMillis);
                    unblockedWorkIds.add(dependentWorkId);
                }
            }

            int unblockedWorkCount = unblockedWorkIds.size();
            if (unblockedWorkCount > 0) {
                Log.d(TAG, "Setting status to enqueued for " + unblockedWorkCount + " Works "
                        + "that were dependent on Work ID " + mWorkSpecId);
            }
            mWorkDatabase.setTransactionSuccessful();

            if (mScheduler != null) {
                WorkSpec[] unblockedWorkSpecs = mWorkSpecDao.getWorkSpecs(unblockedWorkIds);
                mScheduler.schedule(unblockedWorkSpecs);
            }
        } finally {
            mWorkDatabase.endTransaction();
            notifyListener(false);
        }
    }

    @SuppressWarnings("ClassNewInstance")
    static Worker workerFromWorkSpec(@NonNull Context context,
            @NonNull WorkSpec workSpec,
            @NonNull Arguments arguments) {
        Context appContext = context.getApplicationContext();
        String workerClassName = workSpec.getWorkerClassName();
        try {
            Class<?> clazz = Class.forName(workerClassName);
            Worker worker = (Worker) clazz.newInstance();
            Method internalInitMethod = Worker.class.getDeclaredMethod(
                    "internalInit", Context.class, String.class, Arguments.class);
            internalInitMethod.setAccessible(true);
            internalInitMethod.invoke(worker, appContext, workSpec.getId(), arguments);
            return worker;
        } catch (Exception e) {
            Log.e(TAG, "Trouble instantiating " + workerClassName, e);
        }
        return null;
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
