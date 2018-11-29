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
 * A class that performs work synchronously on a background thread provided by {@link WorkManager}.
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
    SettableFuture<Result> mFuture;

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
     * @return The {@link Result} of the computation; note that dependent work will
     *         not execute if you use {@link Result#failure()} or {@link Result#failure(Data)}
     */
    @WorkerThread
    public abstract @NonNull Result doWork();

    @Override
    public final @NonNull ListenableFuture<Result> startWork() {
        mFuture = SettableFuture.create();
        getBackgroundExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Result result = doWork();
                mFuture.set(result);
            }
        });
        return mFuture;
    }
}
