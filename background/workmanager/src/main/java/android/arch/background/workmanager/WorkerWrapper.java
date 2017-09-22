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

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * A runnable that looks up the {@link WorkSpec} from the database for a given id, instantiates
 * its Worker, and then calls it.
 */
class WorkerWrapper implements Runnable {
    private static final String TAG = "WorkerWrapper";
    private Context mAppContext;
    private WorkDatabase mWorkDatabase;
    private String mWorkSpecId;
    private Listener mListener;

    /**
     * Listener that communicates with {@link WorkExecutionManager} and reports
     * the lifecycle of the Worker.
     */
    interface Listener {
        void onPermanentError(String workSpecId);
        void onNotEnqueued(String workSpecId);
        void onSuccess(String workSpecId);
    }

    WorkerWrapper(Context context, WorkDatabase database, String workSpecId,
                  @NonNull Listener listener) {
        mAppContext = context.getApplicationContext();
        mWorkDatabase = database;
        mWorkSpecId = workSpecId;
        mListener = listener;
    }

    @Override
    public void run() {
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        WorkSpec workSpec = workSpecDao.getWorkSpec(mWorkSpecId);
        if (workSpec == null) {
            Log.e(TAG, "Didn't find WorkSpec for id " + mWorkSpecId);
            mListener.onPermanentError(mWorkSpecId);
            return;
        }

        if (workSpec.mStatus != Work.STATUS_ENQUEUED) {
            Log.d(TAG, "Status for " + mWorkSpecId + " is not enqueued; not doing any work");
            mListener.onNotEnqueued(mWorkSpecId);
            return;
        }

        Worker worker = Worker.fromWorkSpec(mAppContext, workSpec);
        if (worker == null) {
            Log.e(TAG, "Could not create Worker " + workSpec.mWorkerClassName);
            workSpecDao.setWorkSpecStatus(mWorkSpecId, Work.STATUS_FAILED);
            mListener.onPermanentError(mWorkSpecId);
            return;
        }

        workSpecDao.setWorkSpecStatus(mWorkSpecId, STATUS_RUNNING);
        // TODO(xbhatnag): Add Running Status to Listener.

        try {
            checkForInterruption();
            worker.doWork();
            checkForInterruption();

            Log.d(TAG, "Work succeeded for " + mWorkSpecId);
            mListener.onSuccess(mWorkSpecId);
            workSpecDao.setWorkSpecStatus(mWorkSpecId, STATUS_SUCCEEDED);
        } catch (Exception e) {
            // TODO: Retry policies.
            if (e instanceof InterruptedException) {
                Log.d(TAG, "Work interrupted for " + mWorkSpecId);
                // TODO(xbhatnag): Add Rescheduled Status to Listener.
                workSpecDao.setWorkSpecStatus(mWorkSpecId, STATUS_ENQUEUED);
            } else {
                Log.d(TAG, "Work failed for " + mWorkSpecId, e);
                // TODO(xbhatnag): Add Failed Status to Listener.
                workSpecDao.setWorkSpecStatus(mWorkSpecId, STATUS_FAILED);
            }
        }
    }

    private void checkForInterruption() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }
}
