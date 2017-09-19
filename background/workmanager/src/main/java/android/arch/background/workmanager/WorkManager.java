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

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * WorkManager is a class used to enqueue persisted work that is guaranteed to run after its
 * constraints are met.
 */
public final class WorkManager implements LifecycleObserver {

    private static final String TAG = "WorkManager";

    private Context mContext;
    private String mName;
    private ScheduledExecutorService mForegroundExecutor;
    private ExecutorService mBackgroundExecutor;
    private WorkDatabase mWorkDatabase;
    private ExecutorService mEnqueueExecutor = Executors.newSingleThreadExecutor();
    private WorkExecutionManager mForegroundWorkExecutionMgr;

    private WorkManager(
            Context context,
            String name,
            ScheduledExecutorService foregroundExecutor,
            ExecutorService backgroundExecutor) {
        mContext = context.getApplicationContext();
        mName = name;
        mForegroundExecutor =
                (foregroundExecutor == null)
                        ? Executors.newScheduledThreadPool(4)   // TODO: Configure intelligently.
                        : foregroundExecutor;
        mBackgroundExecutor =
                (backgroundExecutor == null)
                        ? Executors.newSingleThreadExecutor()   // TODO: Configure intelligently.
                        : backgroundExecutor;
        mWorkDatabase = WorkDatabase.getInstance(mContext, mName);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    /**
     * Called when the process lifecycle is considered started.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onLifecycleStart() {
        mForegroundWorkExecutionMgr = new WorkExecutionManager(
                mContext,
                mWorkDatabase,
                mForegroundExecutor);
    }

    /**
     * Called when the process lifecycle is considered stopped.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onLifecycleStop() {
        mForegroundWorkExecutionMgr.shutdown();
        mForegroundWorkExecutionMgr = null;
    }

    /**
     * Enqueues an item for background processing.
     *
     * @param work the {@link Work} to enqueue
     */
    public void enqueue(Work work) {
        mEnqueueExecutor.execute(new EnqueueRunnable(work));
    }

    /**
     * A Runnable to enqueue WorkItems in the database.
     */
    private class EnqueueRunnable implements Runnable {
        private Work mWork;

        EnqueueRunnable(Work work) {
            mWork = work;
        }

        @Override
        public void run() {
            // TODO: check for prerequisites.
            mWorkDatabase.beginTransaction();
            try {
                mWorkDatabase.workSpecDao().insertWorkSpec(mWork.getWorkSpec());
                mWorkDatabase.workItemDao().insertWorkItems(mWork.getWorkItems());
                mWorkDatabase.dependencyDao().insertDependencies(mWork.generateDependencies());

                if (mForegroundWorkExecutionMgr != null) {
                    mForegroundWorkExecutionMgr.enqueue(
                        mWork.getWorkItems().get(0).mId,
                        0L /* TODO: delay */);
                    // TODO: Schedule dependent work.
                }

                if (Build.VERSION.SDK_INT >= 21) {
                    // TODO: Schedule on JobScheduler.
                    Log.d(TAG, "Schedule JobScheduler here");
                }

                mWorkDatabase.setTransactionSuccessful();
            } finally {
                mWorkDatabase.endTransaction();
            }
        }
    }

    /**
     * A Builder for {@link WorkManager}.
     */
    public static class Builder {

        private String mName;
        private ScheduledExecutorService mForegroundExecutor;
        private ExecutorService mBackgroundExecutor;

        public Builder(String name) {
            mName = name;
        }

        /**
         * @param foregroundExecutor The ExecutorService to run in-process during active lifecycles
         * @return The Builder
         */
        public Builder withForegroundExecutor(ScheduledExecutorService foregroundExecutor) {
            mForegroundExecutor = foregroundExecutor;
            return this;
        }

        /**
         * @param backgroundExecutor The ExecutorService to run via OS-defined background execution
         *                           such as {@link android.app.job.JobScheduler}
         * @return The Builder
         */
        public Builder withBackgroundExecutor(ExecutorService backgroundExecutor) {
            mBackgroundExecutor = backgroundExecutor;
            return this;
        }

        /**
         * Builds the {@link WorkManager}.
         *
         * @param context The context used for initialization (we will get the Application context)
         * @return The {@link WorkManager}
         */
        public WorkManager build(Context context) {
            return new WorkManager(context, mName, mForegroundExecutor, mBackgroundExecutor);
        }
    }
}

