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

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A class to manage the actual in-process (foreground) execution of work.
 */
class WorkExecutionManager implements WorkerWrapper.Listener {
    private Context mAppContext;
    private WorkDatabase mWorkDatabase;
    private ScheduledExecutorService mExecutor;

    private Map<String, Future<?>> mFutures = new HashMap<>();
    private final Object mLock = new Object();

    WorkExecutionManager(
            Context context,
            WorkDatabase workDatabase,
            ScheduledExecutorService executor) {
        mAppContext = context.getApplicationContext();
        mWorkDatabase = workDatabase;
        mExecutor = executor;
    }

    void enqueue(String id, long delayMs) {
        synchronized (mLock) {
            WorkerWrapper runnable = new WorkerWrapper(
                    mAppContext, mWorkDatabase, id, this);
            Future<?> future = mExecutor.schedule(runnable, delayMs, TimeUnit.MILLISECONDS);
            mFutures.put(id, future);
        }
    }

    boolean cancel(String id) {
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
            for (Future future : mFutures.values()) {
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

    @Override
    public void onExecuted(String workSpecId, @WorkerWrapper.ExecutionResult int result) {
        synchronized (mLock) {
            mFutures.remove(workSpecId);
        }
    }
}
