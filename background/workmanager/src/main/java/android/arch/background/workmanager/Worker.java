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

import static android.arch.background.workmanager.Blueprint.STATUS_ENQUEUED;
import static android.arch.background.workmanager.Blueprint.STATUS_FAILED;
import static android.arch.background.workmanager.Blueprint.STATUS_RUNNING;
import static android.arch.background.workmanager.Blueprint.STATUS_SUCCEEDED;

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
    private Blueprint mBlueprint;

    public Worker(Context appContext, WorkDatabase workDatabase, Blueprint blueprint) {
        this.mAppContext = appContext;
        this.mWorkDatabase = workDatabase;
        this.mBlueprint = blueprint;
    }

    /**
     * Override this method to do your actual background processing.
     *
     * @return The result payload
     */
    public abstract T doWork();

    @Override
    public final T call() throws Exception {
        int id = mBlueprint.mId;
        Log.v(TAG, "Worker.call for " + id);
        BlueprintDao blueprintDao = mWorkDatabase.blueprintDao();
        mBlueprint.mStatus = STATUS_RUNNING;
        blueprintDao.setBlueprintStatus(id, STATUS_RUNNING);

        T result = null;

        try {
            checkForInterruption();
            result = doWork();
            checkForInterruption();

            Log.d(TAG, "Work succeeded for " + id);
            mBlueprint.mStatus = STATUS_SUCCEEDED;
            blueprintDao.setBlueprintStatus(id, STATUS_SUCCEEDED);
        } catch (Exception e) {
            // TODO: Retry policies.
            if (e instanceof InterruptedException) {
                Log.d(TAG, "Work interrupted for " + id);
                mBlueprint.mStatus = STATUS_ENQUEUED;
                blueprintDao.setBlueprintStatus(id, STATUS_ENQUEUED);
            } else {
                Log.d(TAG, "Work failed for " + id, e);
                mBlueprint.mStatus = STATUS_FAILED;
                blueprintDao.setBlueprintStatus(id, STATUS_FAILED);
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
