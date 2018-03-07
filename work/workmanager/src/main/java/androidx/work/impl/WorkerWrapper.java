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

package androidx.work.impl;

import static androidx.work.State.CANCELLED;
import static androidx.work.State.ENQUEUED;
import static androidx.work.State.FAILED;
import static androidx.work.State.RUNNING;
import static androidx.work.State.SUCCEEDED;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import androidx.work.Arguments;
import androidx.work.InputMerger;
import androidx.work.State;
import androidx.work.Worker;
import androidx.work.impl.logger.Logger;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor;

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
    private List<Scheduler> mSchedulers;
    private WorkSpec mWorkSpec;
    Worker mWorker;

    private WorkDatabase mWorkDatabase;
    private WorkSpecDao mWorkSpecDao;
    private DependencyDao mDependencyDao;

    private WorkerWrapper(Builder builder) {
        mAppContext = builder.mAppContext;
        mWorkSpecId = builder.mWorkSpecId;
        mListener = builder.mListener;
        mSchedulers = builder.mSchedulers;

        mWorkDatabase = builder.mWorkDatabase;
        mWorkSpecDao = mWorkDatabase.workSpecDao();
        mDependencyDao = mWorkDatabase.dependencyDao();
    }

    @WorkerThread
    @Override
    public void run() {
        mWorkSpec = mWorkSpecDao.getWorkSpec(mWorkSpecId);
        if (mWorkSpec == null) {
            Logger.error(TAG, "Didn't find WorkSpec for id %s", mWorkSpecId);
            notifyListener(false, false);
            return;
        }

        if (mWorkSpec.getState() != ENQUEUED) {
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
                Logger.error(TAG, "Could not create Input Merger %s",
                        mWorkSpec.getInputMergerClassName());
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
            Logger.error(TAG, "Could for create Worker %s", mWorkSpec.getWorkerClassName());
            setFailedAndNotify();
            return;
        }

        setRunning();

        try {
            checkForInterruption();
            Worker.WorkerResult result = mWorker.doWork();
            if (mWorkSpecDao.getState(mWorkSpecId) != CANCELLED) {
                checkForInterruption();
                handleResult(result);
            }
        } catch (InterruptedException e) {
            Logger.debug(TAG, "Work interrupted for %s", mWorkSpecId);
            rescheduleAndNotify(false);
        }
    }

    private void notifyIncorrectStatus() {
        // incorrect status is treated as a false-y attempt at execution
        State status = mWorkSpec.getState();
        if (status == RUNNING) {
            Logger.debug(TAG, "Status for %s is RUNNING;"
                    + "not doing any work and rescheduling for later execution", mWorkSpecId);
            notifyListener(false, true);
        } else {
            Logger.error(TAG, "Status for %s is %s; not doing any work", mWorkSpecId, status);
            notifyListener(false, false);
        }
    }

    private void checkForInterruption() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    private void notifyListener(final boolean isSuccessful, final boolean needsReschedule) {
        if (mListener == null) {
            return;
        }
        WorkManagerTaskExecutor.getInstance().postToMainThread(new Runnable() {
            @Override
            public void run() {
                mListener.onExecuted(mWorkSpecId, isSuccessful, needsReschedule);
            }
        });
    }

    private void handleResult(Worker.WorkerResult result) {
        switch (result) {
            case SUCCESS: {
                Logger.debug(TAG, "Worker result SUCCESS for %s", mWorkSpecId);
                if (mWorkSpec.isPeriodic()) {
                    resetPeriodicAndNotify(true);
                } else {
                    setSucceededAndNotify();
                }
                break;
            }

            case RETRY: {
                Logger.debug(TAG, "Worker result RETRY for %s", mWorkSpecId);
                rescheduleAndNotify(false /* treating current attempt as a false*/);
                break;
            }

            case FAILURE:
            default: {
                Logger.debug(TAG, "Worker result FAILURE for %s", mWorkSpecId);
                if (mWorkSpec.isPeriodic()) {
                    resetPeriodicAndNotify(false);
                } else {
                    setFailedAndNotify();
                }
            }
        }
    }

    private void setRunning() {
        mWorkDatabase.beginTransaction();
        try {
            mWorkSpecDao.setState(RUNNING, mWorkSpecId);
            mWorkSpecDao.incrementWorkSpecRunAttemptCount(mWorkSpecId);
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
        }
    }

    private void setFailedAndNotify() {
        mWorkDatabase.beginTransaction();
        try {
            recursivelyFailWorkAndDependents(mWorkSpecId);

            // Try to set the output for the failed work but check if the worker exists; this could
            // be a permanent error where we couldn't find or create the worker class.
            if (mWorker != null) {
                // Update Arguments as necessary.
                Arguments outputArgs = mWorker.getOutput();
                if (outputArgs != null) {
                    mWorkSpecDao.setOutput(mWorkSpecId, outputArgs);
                }
            }

            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
            notifyListener(false, false);
        }
    }

    private void recursivelyFailWorkAndDependents(String workSpecId) {
        List<String> dependentIds = mDependencyDao.getDependentWorkIds(workSpecId);
        for (String id : dependentIds) {
            recursivelyFailWorkAndDependents(id);
        }

        // Don't fail already cancelled work.
        if (mWorkSpecDao.getState(workSpecId) != CANCELLED) {
            mWorkSpecDao.setState(FAILED, workSpecId);
        }
    }

    private void rescheduleAndNotify(boolean isSuccessful) {
        mWorkDatabase.beginTransaction();
        try {
            mWorkSpecDao.setState(ENQUEUED, mWorkSpecId);
            // TODO(xbhatnag): Period Start Time is confusing for non-periodic work. Rename.
            mWorkSpecDao.setPeriodStartTime(mWorkSpecId, System.currentTimeMillis());
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
            notifyListener(isSuccessful, true);
        }
    }

    private void resetPeriodicAndNotify(boolean isSuccessful) {
        mWorkDatabase.beginTransaction();
        try {
            long currentPeriodStartTime = mWorkSpec.getPeriodStartTime();
            long nextPeriodStartTime = currentPeriodStartTime + mWorkSpec.getIntervalDuration();
            mWorkSpecDao.setPeriodStartTime(mWorkSpecId, nextPeriodStartTime);
            mWorkSpecDao.setState(ENQUEUED, mWorkSpecId);
            mWorkSpecDao.resetWorkSpecRunAttemptCount(mWorkSpecId);
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
            notifyListener(isSuccessful, false);
        }
    }

    private void setSucceededAndNotify() {
        List<String> unblockedWorkIds = new ArrayList<>();

        mWorkDatabase.beginTransaction();
        try {
            mWorkSpecDao.setState(SUCCEEDED, mWorkSpecId);

            // Update Arguments as necessary.
            Arguments outputArgs = mWorker.getOutput();
            if (outputArgs != null) {
                mWorkSpecDao.setOutput(mWorkSpecId, outputArgs);
            }

            // Unblock Dependencies and set Period Start Time
            long currentTimeMillis = System.currentTimeMillis();
            List<String> dependentWorkIds = mDependencyDao.getDependentWorkIds(mWorkSpecId);
            for (String dependentWorkId : dependentWorkIds) {
                if (mDependencyDao.hasCompletedAllPrerequisites(dependentWorkId)) {
                    Logger.debug(TAG, "Setting status to enqueued for %s", dependentWorkId);
                    mWorkSpecDao.setState(ENQUEUED, dependentWorkId);
                    mWorkSpecDao.setPeriodStartTime(dependentWorkId, currentTimeMillis);
                    unblockedWorkIds.add(dependentWorkId);
                }
            }

            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
            notifyListener(true, false);
        }

        if (mSchedulers != null) {
            int unblockedWorkCount = unblockedWorkIds.size();
            if (unblockedWorkCount > 0) {
                Logger.debug(TAG,
                        "Setting status to enqueued for %s items that were dependent on %s",
                        unblockedWorkCount, mWorkSpecId);
            }

            WorkSpec[] unblockedWorkSpecs = mWorkSpecDao.getWorkSpecs(unblockedWorkIds);
            for (Scheduler scheduler : mSchedulers) {
                scheduler.schedule(unblockedWorkSpecs);
            }
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
        private List<Scheduler> mSchedulers;

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

        Builder withSchedulers(List<Scheduler> schedulers) {
            mSchedulers = schedulers;
            return this;
        }

        WorkerWrapper build() {
            return new WorkerWrapper(this);
        }
    }
}
