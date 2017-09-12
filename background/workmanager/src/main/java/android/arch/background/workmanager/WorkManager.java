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

/**
 * WorkManager is a singleton class used to enqueue persisted work that is guaranteed to run after
 * its constraints are met.
 */
public final class WorkManager implements LifecycleObserver {

    private static final String TAG = "WorkManager";

    private static WorkManager sInstance;
    private static int sCurrentIdTODO = 0;  // TODO: Change this! This is temporary to get started.

    /**
     * Returns the singleton instance of WorkManager, creating it if necessary.
     *
     * @param context A Context for initialization (this method will use the application Context)
     * @return The singleton instance of WorkManager
     */
    public static WorkManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WorkManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private Context mContext;
    private WorkDatabase mWorkDatabase;
    private ExecutorService mEnqueueExecutor = Executors.newSingleThreadExecutor();

    private WorkManager(Context context) {
        this.mContext = context;
        mWorkDatabase = WorkDatabase.getInstance(context);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    /**
     * Called when the process lifecycle is considered started.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onLifecycleStart() {
    }

    /**
     * Called when the process lifecycle is considered stopped.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onLifecycleStop() {
    }

    /**
     * Enqueues an item for background processing.
     *
     * @param workItem {@link WorkItem} containing all {@link Blueprint}s
     * @return array of {@link Blueprint} ids enqueued
     */
    public int[] enqueue(WorkItem workItem) {
        int[] ids = new int[workItem.getBlueprints().size()];
        for (int i = 0; i < ids.length; i++) {
            int id = generateId();
            workItem.getBlueprints().get(i).mId = id;
            ids[i] = id;
        }
        mEnqueueExecutor.execute(new EnqueueRunnable(workItem));
        return ids;
    }

    private int generateId() {
        // TODO: Fix! Temporary solution for id assignment.
        return sCurrentIdTODO++;
    }

    /**
     * A Runnable to enqueue WorkItems in the database.
     */
    private class EnqueueRunnable implements Runnable {
        private WorkItem mWorkItem;

        EnqueueRunnable(WorkItem workItem) {
            mWorkItem = workItem;
        }

        @Override
        public void run() {
            // TODO: check for prerequisites.
            BlueprintDao blueprintDao = mWorkDatabase.blueprintDao();
            blueprintDao.insertBlueprints(mWorkItem.getBlueprints());

            // TODO: Schedule on in-process executor.
            Log.d(TAG, "Schedule in-process executor here");

            if (Build.VERSION.SDK_INT >= 21) {
                // TODO: Schedule on JobScheduler.
                Log.d(TAG, "Schedule JobScheduler here");
            }
        }
    }
}

