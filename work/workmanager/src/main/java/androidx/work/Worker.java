/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * The basic object that performs work.  Worker classes are instantiated at runtime by
 * {@link WorkManager} and the {@link #doWork()} method is called on a background thread.  In case
 * the work is preempted for any reason, the same instance of Worker is not reused.  This means
 * that {@link #doWork()} is called exactly once per Worker instance.
 */
public abstract class Worker extends NonBlockingWorker {

    // Package-private to avoid synthetic accessor.
    SettableFuture<Payload> mFuture;
    private @NonNull volatile Data mOutputData = Data.EMPTY;

    @Keep
    public Worker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Override this method to do your actual background processing.
     */
    @WorkerThread
    public abstract @NonNull Result doWork();

    @Override
    public final @NonNull ListenableFuture<Payload> onStartWork() {
        mFuture = SettableFuture.create();
        getBackgroundExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Result result = doWork();
                mFuture.set(new Payload(result, getOutputData()));
            }
        });
        return mFuture;
    }

    /**
     * Call this method to pass a {@link Data} object as the output of this {@link Worker}.  This
     * result can be observed and passed to Workers that are dependent on this one.
     *
     * In cases like where two or more {@link OneTimeWorkRequest}s share a dependent WorkRequest,
     * their Data will be merged together using an {@link InputMerger}.  The default InputMerger is
     * {@link OverwritingInputMerger}, unless otherwise specified using the
     * {@link OneTimeWorkRequest.Builder#setInputMerger(Class)} method.
     * <p>
     * This method is invoked after {@code onStartWork} and returns
     * {@link androidx.work.NonBlockingWorker.Result#SUCCESS} or a
     * {@link androidx.work.NonBlockingWorker.Result#FAILURE}.
     * <p>
     * For example, if you had this structure:
     * <pre>
     * {@code WorkManager.getInstance(context)
     *             .beginWith(workRequestA, workRequestB)
     *             .then(workRequestC)
     *             .enqueue()}</pre>
     *
     * This method would be called for both {@code workRequestA} and {@code workRequestB} after
     * their completion, modifying the input Data for {@code workRequestC}.
     *
     * @param outputData An {@link Data} object that will be merged into the input Data of any
     *                   OneTimeWorkRequest that is dependent on this one, or {@link Data#EMPTY} if
     *                   there is nothing to contribute
     */
    public void setOutputData(@NonNull Data outputData) {
        mOutputData = outputData;
    }

    /**
     * @return the output {@link Data} set by the {@link Worker}.
     */
    public Data getOutputData() {
        return mOutputData;
    }
}
