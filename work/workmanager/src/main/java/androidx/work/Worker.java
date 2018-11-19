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
 * A basic class that performs work on a background thread provided by {@link WorkManager}.
 * <p>
 * Worker classes are instantiated at runtime by WorkManager and the {@link #doWork()} method is
 * called on a pre-specified background thread (see {@link Configuration#getExecutor()}).  This
 * method is for <b>synchronous</b> processing of your work, meaning that once you return from that
 * method, the Worker is considered to be finished and will be destroyed.  If you need to do your
 * work asynchronously or call asynchronous APIs, you should use {@link ListenableWorker}.
 * <p>
 * In case the work is preempted for any reason, the same instance of Worker is not reused.  This
 * means that {@link #doWork()} is called exactly once per Worker instance.  A new Worker is created
 * if a unit of work needs to be rerun.
 */
public abstract class Worker extends ListenableWorker {

    // Package-private to avoid synthetic accessor.
    SettableFuture<Payload> mFuture;
    private @NonNull volatile Data mOutputData = Data.EMPTY;

    @Keep
    public Worker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Override this method to do your actual background processing.  This method is called on a
     * background thread - you are required to <b>synchronously</b> do your work and return the
     * {@link Result} from this method.  Once you return from this method, the Worker is considered
     * to have finished what its doing and will be destroyed.  If you need to do your work
     * asynchronously on a thread of your own choice, see {@link ListenableWorker}.
     *
     * @return The {@link ListenableWorker.Result} of the computation; note that dependent work will
     *         not execute if you return {@link ListenableWorker.Result#FAILURE}
     */
    @WorkerThread
    public abstract @NonNull Result doWork();

    @Override
    public final @NonNull ListenableFuture<Payload> startWork() {
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
     * <p>
     * In cases like where two or more {@link OneTimeWorkRequest}s share a dependent WorkRequest,
     * their Data will be merged together using an {@link InputMerger}.  The default InputMerger is
     * {@link OverwritingInputMerger}, unless otherwise specified using the
     * {@link OneTimeWorkRequest.Builder#setInputMerger(Class)} method.
     * <p>
     * The output Data is only valid if your worker returns a
     * {@link ListenableWorker.Result#SUCCESS} or a {@link ListenableWorker.Result#FAILURE}.
     *
     * @param outputData An {@link Data} object that will be merged into the input Data of any
     *                   OneTimeWorkRequest that is dependent on this one, or {@link Data#EMPTY} if
     *                   there is nothing to contribute
     */
    public final void setOutputData(@NonNull Data outputData) {
        mOutputData = outputData;
    }

    /**
     * @return the output {@link Data} set by the {@link Worker}.
     */
    public final @NonNull Data getOutputData() {
        return mOutputData;
    }
}
