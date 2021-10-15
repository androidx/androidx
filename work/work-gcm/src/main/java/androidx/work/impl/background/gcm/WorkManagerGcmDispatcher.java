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

package androidx.work.impl.background.gcm;


import android.content.Context;
import android.os.PowerManager;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.work.Logger;
import androidx.work.WorkInfo;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.Processor;
import androidx.work.impl.Schedulers;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.WakeLocks;
import androidx.work.impl.utils.WorkTimer;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.TaskParams;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Handles requests for executing {@link androidx.work.WorkRequest}s on behalf of
 * {@link WorkManagerGcmService}.
 */
public class WorkManagerGcmDispatcher {

    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("WrkMgrGcmDispatcher");

    private static final long AWAIT_TIME_IN_MINUTES = 10;
    private static final long AWAIT_TIME_IN_MILLISECONDS = AWAIT_TIME_IN_MINUTES * 60 * 1000;

    private final Context mContext;
    private final WorkTimer mWorkTimer;

    // Synthetic access
    WorkManagerImpl mWorkManagerImpl;


    public WorkManagerGcmDispatcher(@NonNull Context context, @NonNull WorkTimer workTimer) {
        mContext = context.getApplicationContext();
        mWorkTimer = workTimer;
        mWorkManagerImpl = WorkManagerImpl.getInstance(context);
    }


    /**
     * Handles {@link WorkManagerGcmService#onInitializeTasks()}.
     */
    @MainThread
    public void onInitializeTasks() {
        // Reschedule all eligible work, as all tasks have been cleared in GCMNetworkManager.
        // This typically happens after an upgrade.
        mWorkManagerImpl.getWorkTaskExecutor().executeOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                Logger.get().debug(TAG, "onInitializeTasks(): Rescheduling work");
                mWorkManagerImpl.rescheduleEligibleWork();
            }
        });
    }

    /**
     * Handles {@link WorkManagerGcmService#onRunTask(TaskParams)}.
     */
    public int onRunTask(@NonNull TaskParams taskParams) {
        // Tasks may be executed concurrently but every Task will be executed in a unique thread
        // per tag, which in our case is a workSpecId. Therefore its safe to block here with
        // a latch because there is 1 thread per workSpecId.

        Logger.get().debug(TAG, "Handling task " + taskParams);

        String workSpecId = taskParams.getTag();
        if (workSpecId == null || workSpecId.isEmpty()) {
            // Bad request. No WorkSpec id.
            Logger.get().debug(TAG, "Bad request. No workSpecId.");
            return GcmNetworkManager.RESULT_FAILURE;
        }

        WorkSpecExecutionListener listener = new WorkSpecExecutionListener(workSpecId);
        WorkSpecTimeLimitExceededListener timeLimitExceededListener =
                new WorkSpecTimeLimitExceededListener(mWorkManagerImpl);
        Processor processor = mWorkManagerImpl.getProcessor();
        processor.addExecutionListener(listener);
        String wakeLockTag = "WorkGcm-onRunTask (" + workSpecId + ")";
        PowerManager.WakeLock wakeLock = WakeLocks.newWakeLock(mContext, wakeLockTag);
        mWorkManagerImpl.startWork(workSpecId);
        mWorkTimer.startTimer(workSpecId, AWAIT_TIME_IN_MILLISECONDS, timeLimitExceededListener);

        try {
            wakeLock.acquire();
            listener.getLatch().await(AWAIT_TIME_IN_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException exception) {
            Logger.get().debug(TAG, "Rescheduling WorkSpec" + workSpecId);
            return reschedule(workSpecId);
        } finally {
            processor.removeExecutionListener(listener);
            mWorkTimer.stopTimer(workSpecId);
            wakeLock.release();
        }

        if (listener.needsReschedule()) {
            Logger.get().debug(TAG, "Rescheduling WorkSpec" + workSpecId);
            return reschedule(workSpecId);
        }

        WorkDatabase workDatabase = mWorkManagerImpl.getWorkDatabase();
        WorkSpec workSpec = workDatabase.workSpecDao().getWorkSpec(workSpecId);
        WorkInfo.State state = workSpec != null ? workSpec.state : null;

        if (state == null) {
            Logger.get().debug(TAG, "WorkSpec %s does not exist" + workSpecId);
            return GcmNetworkManager.RESULT_FAILURE;
        } else {
            switch (state) {
                case SUCCEEDED:
                case CANCELLED:
                    Logger.get().debug(TAG, "Returning RESULT_SUCCESS for WorkSpec " + workSpecId);
                    return GcmNetworkManager.RESULT_SUCCESS;
                case FAILED:
                    Logger.get().debug(TAG, "Returning RESULT_FAILURE for WorkSpec " +  workSpecId);
                    return GcmNetworkManager.RESULT_FAILURE;
                default:
                    Logger.get().debug(TAG, "Rescheduling eligible work.");
                    return reschedule(workSpecId);
            }
        }
    }

    /**
     * Cleans up resources when the {@link WorkManagerGcmDispatcher} is no longer in use.
     */
    public void onDestroy() {
        mWorkTimer.onDestroy();
    }

    private int reschedule(@NonNull final String workSpecId) {
        final WorkDatabase workDatabase = mWorkManagerImpl.getWorkDatabase();
        workDatabase.runInTransaction(new Runnable() {
            @Override
            public void run() {
                // Mark the workSpec as unscheduled. We are doing this explicitly here because
                // there are many cases where WorkerWrapper may not have had a chance to update this
                // flag. For e.g. this will happen if the Worker took longer than 10 minutes.
                workDatabase.workSpecDao()
                        .markWorkSpecScheduled(workSpecId, WorkSpec.SCHEDULE_NOT_REQUESTED_YET);
                // We reschedule on our own to apply our own backoff policy.
                Schedulers.schedule(
                        mWorkManagerImpl.getConfiguration(),
                        mWorkManagerImpl.getWorkDatabase(),
                        mWorkManagerImpl.getSchedulers());
            }
        });

        Logger.get().debug(TAG, "Returning RESULT_SUCCESS for WorkSpec " + workSpecId);
        return GcmNetworkManager.RESULT_SUCCESS;
    }

    static class WorkSpecTimeLimitExceededListener implements WorkTimer.TimeLimitExceededListener {
        private static final String TAG = Logger.tagWithPrefix("WrkTimeLimitExceededLstnr");

        private final WorkManagerImpl mWorkManager;

        WorkSpecTimeLimitExceededListener(@NonNull WorkManagerImpl workManager) {
            mWorkManager = workManager;
        }

        @Override
        public void onTimeLimitExceeded(@NonNull String workSpecId) {
            Logger.get().debug(TAG, "WorkSpec time limit exceeded " + workSpecId);
            mWorkManager.stopWork(workSpecId);
        }
    }

    static class WorkSpecExecutionListener implements ExecutionListener {
        private static final String TAG = Logger.tagWithPrefix("WorkSpecExecutionListener");
        private final String mWorkSpecId;
        private final CountDownLatch mLatch;
        private boolean mNeedsReschedule;

        WorkSpecExecutionListener(@NonNull String workSpecId) {
            mWorkSpecId = workSpecId;
            mLatch = new CountDownLatch(1);
            mNeedsReschedule = false;
        }

        boolean needsReschedule() {
            return mNeedsReschedule;
        }

        CountDownLatch getLatch() {
            return mLatch;
        }

        @Override
        public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {
            if (!mWorkSpecId.equals(workSpecId)) {
                Logger.get().warning(TAG,
                        "Notified for " + workSpecId + ", but was looking for " + mWorkSpecId);
            } else {
                mNeedsReschedule = needsReschedule;
                mLatch.countDown();
            }
        }
    }
}
