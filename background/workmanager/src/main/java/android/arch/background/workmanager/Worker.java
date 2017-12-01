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
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
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
    private @NonNull Arguments mArguments;
    private Arguments mOutput;

    protected final Context getAppContext() {
        return mAppContext;
    }

    protected final @NonNull Arguments getArguments() {
        return mArguments;
    }

    /**
     * Override this method to do your actual background processing.
     *
     * @return The result of the work, corresponding to a {@link WorkerResult} value.  If a
     * different value is returned, the result shall be defaulted to {@code WORKER_RESULT_FAILURE}.
     */
    @WorkerThread
    public abstract @WorkerResult int doWork();

    /**
     * Call this method to pass an {@link Arguments} object to {@link Work} that is dependent on
     * this one.  Note that if there are multiple {@link Worker}s that contribute to the target, the
     * Arguments will be merged together, so it is up to the developer to make sure that keys are
     * unique.  New values and types will clobber old values and types, and if there are multiple
     * parent Workers of a child Worker, the order of clobbering may not be deterministic.
     *
     * This method is invoked after {@link #doWork()} returns {@link #WORKER_RESULT_SUCCESS} and
     * there are chained jobs available.
     *
     * For example, if you had this structure:
     *
     * {@code WorkManager.getInstance().enqueue(WorkerA.class, WorkerB.class).then(WorkerC.class)}
     *
     * This method would be called for both WorkerA and WorkerB after their successful completion,
     * modifying the input Arguments for WorkerC.
     *
     * @param output An {@link Arguments} object that will be merged into the input Arguments of any
     *               Work that is dependent on this one, or {@code null} if there is nothing to
     *               contribute
     */
    public final void setOutput(Arguments output) {
        mOutput = output;
    }

    Arguments getOutput() {
        return mOutput;
    }

    private void internalInit(Context appContext, Arguments arguments) {
        mAppContext = appContext;
        mArguments = arguments;
    }

    /**
     * Determines if the {@link Worker} was interrupted and should stop executing.
     * The {@link Worker} can be interrupted for the following reasons:
     * 1. The {@link Work} or {@link PeriodicWork} was explicitly cancelled.
     *    {@link WorkManager#cancelAllWorkWithTag(String)}
     * 2. Constraints set in {@link Work} or {@link PeriodicWork} are no longer valid.
     * @return {@code true} if {@link Worker} is instructed to stop executing.
     */
    protected final boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    @SuppressWarnings("ClassNewInstance")
    static Worker fromWorkSpec(@NonNull Context context,
            @NonNull WorkSpec workSpec,
            @NonNull Arguments arguments) {
        Context appContext = context.getApplicationContext();
        String workerClassName = workSpec.getWorkerClassName();
        try {
            Class<?> clazz = Class.forName(workerClassName);
            Worker worker = (Worker) clazz.newInstance();
            worker.internalInit(appContext, arguments);
            return worker;
        } catch (Exception e) {
            Log.e(TAG, "Trouble instantiating " + workerClassName, e);
        }
        return null;
    }
}
