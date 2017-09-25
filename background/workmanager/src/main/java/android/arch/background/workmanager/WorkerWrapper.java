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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.arch.background.workmanager.model.WorkSpec;
import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.annotation.Retention;

/**
 * A runnable that looks up the {@link WorkSpec} from the database for a given id, instantiates
 * its Worker, and then calls it.
 */
class WorkerWrapper implements Runnable {
    @Retention(SOURCE)
    @IntDef({RESULT_NOT_ENQUEUED, RESULT_PERMANENT_ERROR, RESULT_FAILED, RESULT_INTERRUPTED,
            RESULT_SUCCEEDED})
    public @interface ExecutionResult {
    }

    public static final int RESULT_NOT_ENQUEUED = 0;
    public static final int RESULT_PERMANENT_ERROR = 1;
    public static final int RESULT_FAILED = 2;
    public static final int RESULT_INTERRUPTED = 3;
    public static final int RESULT_SUCCEEDED = 4;

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
        void onExecuted(String workSpecId, @ExecutionResult int result);
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
            mListener.onExecuted(mWorkSpecId, RESULT_PERMANENT_ERROR);
            return;
        }

        if (workSpec.getStatus() != Work.STATUS_ENQUEUED) {
            Log.d(TAG, "Status for " + mWorkSpecId + " is not enqueued; not doing any work");
            mListener.onExecuted(mWorkSpecId, RESULT_NOT_ENQUEUED);
            return;
        }

        Worker worker = Worker.fromWorkSpec(mAppContext, workSpec);
        if (worker == null) {
            Log.e(TAG, "Could not create Worker " + workSpec.getWorkerClassName());
            workSpecDao.setWorkSpecStatus(mWorkSpecId, Work.STATUS_FAILED);
            mListener.onExecuted(mWorkSpecId, RESULT_PERMANENT_ERROR);
            return;
        }

        workSpecDao.setWorkSpecStatus(mWorkSpecId, Work.STATUS_RUNNING);

        try {
            checkForInterruption();
            worker.doWork();
            checkForInterruption();

            Log.d(TAG, "Work succeeded for " + mWorkSpecId);
            mListener.onExecuted(mWorkSpecId, RESULT_SUCCEEDED);
            workSpecDao.setWorkSpecStatus(mWorkSpecId, Work.STATUS_SUCCEEDED);
        } catch (Exception e) {
            // TODO: Retry policies.
            if (e instanceof InterruptedException) {
                Log.d(TAG, "Work interrupted for " + mWorkSpecId);
                mListener.onExecuted(mWorkSpecId, RESULT_INTERRUPTED);
                workSpecDao.setWorkSpecStatus(mWorkSpecId, STATUS_ENQUEUED);
            } else {
                Log.d(TAG, "Work failed for " + mWorkSpecId, e);
                mListener.onExecuted(mWorkSpecId, RESULT_FAILED);
                workSpecDao.setWorkSpecStatus(mWorkSpecId, Work.STATUS_FAILED);
            }
        }
    }

    private void checkForInterruption() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }
}
