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

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A class to manage the actual in-process (foreground) execution of work.
 */
class WorkExecutionManager {

    private static final String TAG = "WorkExecMgr";

    private Context mAppContext;
    private WorkDatabase mWorkDatabase;
    private ScheduledExecutorService mExecutor;

    private SparseArray<Future<?>> mFutures = new SparseArray<>();
    private final Object mLock = new Object();

    WorkExecutionManager(
            Context context,
            WorkDatabase workDatabase,
            ScheduledExecutorService executor) {
        mAppContext = context.getApplicationContext();
        mWorkDatabase = workDatabase;
        mExecutor = executor;
    }

    void enqueue(int id, long delayMs) {
        synchronized (mLock) {
            InternalRunnable runnable = new InternalRunnable(id);
            Future<?> future = mExecutor.schedule(runnable, delayMs, TimeUnit.MILLISECONDS);
            mFutures.put(id, future);
        }
    }

    boolean cancel(int id) {
        synchronized (mLock) {
            Future<?> future = mFutures.get(id);
            if (future != null) {
                boolean canceled = future.cancel(true);
                mFutures.remove(id);
                return canceled;
            }
        }
        return false;
    }

    void shutdown() {
        synchronized (mLock) {
            for (int i = 0, size = mFutures.size(); i < size; ++i) {
                Future<?> future = mFutures.valueAt(i);
                if (future != null) {
                    // TODO(sumir): Investigate if we should interrupt running tasks.
                    // Also look at mExecutor.shutdown() vs. mExecutor.shutdownNow()
                    future.cancel(true);
                }
            }
            mFutures.clear();
            mExecutor.shutdownNow();
            mExecutor = null;
        }
    }

    /**
     * A callable that looks up the {@link WorkItem} from the database for a given id, instantiates
     * its Worker, and then calls it.
     */
    private class InternalRunnable implements Runnable {

        int mId;

        InternalRunnable(int id) {
            mId = id;
        }

        @Override
        public void run() {
            WorkItemDao workItemDao = mWorkDatabase.workItemDao();
            WorkItem workItem = workItemDao.getWorkItem(mId);
            if (workItem != null) {
                int status = workItem.mStatus;
                if (status != STATUS_ENQUEUED) {
                    Log.d(TAG, "Status for " + mId + " is not enqueued; not doing any work");
                    return;
                }

                Worker worker = Worker.fromWorkItem(mAppContext, mWorkDatabase, workItem);
                if (worker == null) {
                    Log.e(TAG, "Could not create Worker " + workItem.mWorkerClassName);
                    workItemDao.setWorkItemStatus(mId, STATUS_FAILED);
                    return;
                }

                synchronized (mLock) {
                    if (mFutures.get(mId) == null) {
                        Log.d(
                                TAG,
                                "InternalRunnable for id " + mId
                                        + " was interrupted; not starting work");
                        return;
                    }
                }

                worker.call();
                synchronized (mLock) {
                    mFutures.remove(mId);
                }
            } else {
                Log.e(TAG, "Didn't find WorkItem for id " + mId);
                synchronized (mLock) {
                    mFutures.remove(mId);
                }
            }
        }
    }
}
