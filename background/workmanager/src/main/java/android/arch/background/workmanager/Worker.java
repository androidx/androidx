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

import static android.arch.background.workmanager.WorkItem.STATUS_ENQUEUED;
import static android.arch.background.workmanager.WorkItem.STATUS_FAILED;
import static android.arch.background.workmanager.WorkItem.STATUS_RUNNING;
import static android.arch.background.workmanager.WorkItem.STATUS_SUCCEEDED;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.Callable;

/**
 * The basic unit of work.
 *
 * @param <T> The payload type for this unit of work.
 */
public abstract class Worker<T> implements Callable<T> {

    private static final String TAG = "Worker";

    protected Context mAppContext;
    private WorkDatabase mWorkDatabase;
    private WorkItem mWorkItem;

    public Worker(Context appContext, WorkDatabase workDatabase, WorkItem workItem) {
        this.mAppContext = appContext;
        this.mWorkDatabase = workDatabase;
        this.mWorkItem = workItem;
    }

    /**
     * Override this method to do your actual background processing.
     * @return The result payload
     */
    public abstract T doWork();

    @Override
    public final T call() throws Exception {
        int id = mWorkItem.getId();
        Log.v(TAG, "Worker.call for " + id);
        WorkItemDao workItemDao = mWorkDatabase.workItemDao();
        mWorkItem.setStatus(STATUS_RUNNING);
        workItemDao.setWorkItemStatus(id, STATUS_RUNNING);

        T result = null;

        try {
            checkForInterruption();
            result = doWork();
            checkForInterruption();

            Log.d(TAG, "Work succeeded for " + id);
            mWorkItem.setStatus(STATUS_SUCCEEDED);
            workItemDao.setWorkItemStatus(id, STATUS_SUCCEEDED);
        } catch (Exception e) {
            // TODO: Retry policies.
            if (e instanceof InterruptedException) {
                Log.d(TAG, "Work interrupted for " + id);
                mWorkItem.setStatus(STATUS_ENQUEUED);
                workItemDao.setWorkItemStatus(id, STATUS_ENQUEUED);
            } else {
                Log.d(TAG, "Work failed for " + id, e);
                mWorkItem.setStatus(STATUS_FAILED);
                workItemDao.setWorkItemStatus(id, STATUS_FAILED);
            }
        }

        return result;
    }

    private void checkForInterruption() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }
}
