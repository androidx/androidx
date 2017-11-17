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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.arch.background.workmanager.model.Arguments;
import android.arch.background.workmanager.model.WorkSpec;
import android.content.Context;
import android.support.annotation.IntDef;
import android.util.Log;

import java.lang.annotation.Retention;

/**
 * The basic unit of work.
 */
public abstract class Worker {

    @Retention(SOURCE)
    @IntDef({WORKER_RESULT_SUCCESS, WORKER_RESULT_FAILURE, WORKER_RESULT_RETRY})
    public @interface WorkerResult {
    }

    public static final int WORKER_RESULT_SUCCESS = 0;
    public static final int WORKER_RESULT_FAILURE = 1;
    public static final int WORKER_RESULT_RETRY = 2;

    private static final String TAG = "Worker";

    private Context mAppContext;
    private Arguments mArguments;

    protected final Context getAppContext() {
        return mAppContext;
    }

    protected final Arguments getArguments() {
        return mArguments;
    }

    /**
     * Override this method to do your actual background processing.
     *
     * @return The result of the work, corresponding to a {@link WorkerResult} value.  If a
     * different value is returned, the result shall be defaulted to {@code WORKER_RESULT_FAILURE}.
     */
    public abstract @WorkerResult int doWork();

    private void internalInit(Context appContext, Arguments arguments) {
        mAppContext = appContext;
        mArguments = arguments;
    }

    /**
     * Determines if the {@link Worker} was interrupted and should stop executing.
     * The {@link Worker} can be interrupted for the following reasons:
     * 1. The {@link Work} or {@link PeriodicWork} was explicitly cancelled.
     *    {@link WorkManager#cancelAllWorkWithTag(String)}
     *    {@link WorkManager#cancelAllWorkWithTagPrefix(String)}
     * 2. Constraints set in {@link Work} or {@link PeriodicWork} are no longer valid.
     * @return {@code true} if {@link Worker} is instructed to stop executing.
     */
    protected final boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    @SuppressWarnings("ClassNewInstance")
    static Worker fromWorkSpec(Context context, WorkSpec workSpec) {
        Context appContext = context.getApplicationContext();
        String workerClassName = workSpec.getWorkerClassName();
        Arguments arguments = workSpec.getArguments();
        try {
            Class<?> clazz = Class.forName(workerClassName);
            if (Worker.class.isAssignableFrom(clazz)) {
                Worker worker = (Worker) clazz.newInstance();
                worker.internalInit(appContext, arguments);
                return worker;
            } else {
                Log.e(TAG, "" + workerClassName + " is not of type Worker");
            }
        } catch (Exception e) {
            Log.e(TAG, "Trouble instantiating " + workerClassName, e);
        }
        return null;
    }
}
