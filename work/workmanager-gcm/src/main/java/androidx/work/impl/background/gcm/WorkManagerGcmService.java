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


import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.work.Logger;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.Processor;
import androidx.work.impl.Schedulers;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The {@link GcmTaskService} responsible for handling requests for executing
 * {@link androidx.work.WorkRequest}s.
 */
public class WorkManagerGcmService extends GcmTaskService {

    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("WorkManagerGcmService");

    private static final long AWAIT_TIME_IN_MINUTES = 10;

    // Synthetic access
    WorkManagerImpl mWorkManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mWorkManager = WorkManagerImpl.getInstance(getApplicationContext());
    }

    @Override
    @MainThread
    public void onInitializeTasks() {
        // Reschedule all eligible work, as all tasks have been cleared in GCMNetworkManager.
        // This typically happens after an upgrade.
        mWorkManager.getWorkTaskExecutor().executeOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                Logger.get().debug(TAG, "onInitializeTasks(): Rescheduling work");
                mWorkManager.rescheduleEligibleWork();
            }
        });
    }

    @Override
    public int onRunTask(@NonNull TaskParams taskParams) {
        // Tasks may be executed concurrently but every Task will be executed in a unique thread
        // per tag, which in our case is a workSpecId. Therefore its safe to block here with
        // a latch because there is 1 thread per workSpecId.

        Logger.get().debug(TAG, String.format("Handling task %s", taskParams));

        String workSpecId = taskParams.getTag();
        if (workSpecId == null || workSpecId.isEmpty()) {
            // Bad request. No WorkSpec id.
            Logger.get().debug(TAG, "Bad request. No workSpecId.");
            return GcmNetworkManager.RESULT_FAILURE;
        }

        WorkSpecExecutionListener listener = new WorkSpecExecutionListener(workSpecId);
        Processor processor = mWorkManager.getProcessor();
        processor.addExecutionListener(listener);
        mWorkManager.startWork(workSpecId);

        try {
            listener.getLatch().await(AWAIT_TIME_IN_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException exception) {
            Logger.get().debug(TAG, String.format("Rescheduling WorkSpec %s", workSpecId));
            return reschedule(workSpecId);
        } finally {
            processor.removeExecutionListener(listener);
        }

        if (listener.needsReschedule()) {
            Logger.get().debug(TAG, String.format("Rescheduling WorkSpec %s", workSpecId));
            return reschedule(workSpecId);
        }

        WorkDatabase workDatabase = mWorkManager.getWorkDatabase();
        WorkSpec workSpec = workDatabase.workSpecDao().getWorkSpec(workSpecId);
        switch (workSpec.state) {
            case SUCCEEDED:
            case CANCELLED:
                Logger.get().debug(TAG,
                        String.format("Returning RESULT_SUCCESS for WorkSpec %s", workSpecId));
                return GcmNetworkManager.RESULT_SUCCESS;
            case FAILED:
                Logger.get().debug(TAG,
                        String.format("Returning RESULT_FAILURE for WorkSpec %s", workSpecId));
                return GcmNetworkManager.RESULT_FAILURE;
            default:
                Logger.get().debug(TAG, "Rescheduling eligible work.");
                return reschedule(workSpecId);
        }
    }

    private int reschedule(@NonNull String workSpecId) {
        WorkDatabase workDatabase = mWorkManager.getWorkDatabase();
        try {
            workDatabase.beginTransaction();
            // Mark the workSpec as unscheduled. We are doing this explicitly here because
            // there are many cases where WorkerWrapper may not have had a chance to update this
            // flag. For e.g. this will happen if the Worker took longer than 10 minutes.
            workDatabase.workSpecDao()
                    .markWorkSpecScheduled(workSpecId, WorkSpec.SCHEDULE_NOT_REQUESTED_YET);
            // We reschedule on our own to apply our own backoff policy.
            Schedulers.schedule(
                    mWorkManager.getConfiguration(),
                    mWorkManager.getWorkDatabase(),
                    mWorkManager.getSchedulers());
            workDatabase.setTransactionSuccessful();
        } finally {
            workDatabase.endTransaction();
        }

        Logger.get().debug(TAG,
                String.format("Returning RESULT_SUCCESS for WorkSpec %s", workSpecId));
        return GcmNetworkManager.RESULT_SUCCESS;
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
                        String.format("Notified for %s, but was looking for %s", workSpecId,
                                mWorkSpecId));
            } else {
                mNeedsReschedule = needsReschedule;
                mLatch.countDown();
            }
        }
    }
}
