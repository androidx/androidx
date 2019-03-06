/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.work.impl.utils.taskexecutor;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Default Task Executor for executing common tasks in WorkManager
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkManagerTaskExecutor implements TaskExecutor {

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    private final Executor mMainThreadExecutor = new Executor() {
        @Override
        public void execute(@NonNull Runnable command) {
            postToMainThread(command);
        }
    };

    // Avoiding synthetic accessor.
    volatile Thread mCurrentBackgroundExecutorThread;
    private final ThreadFactory mBackgroundThreadFactory = new ThreadFactory() {

        private int mThreadsCreated = 0;

        @Override
        public Thread newThread(@NonNull Runnable r) {
            // Delegate to the default factory, but keep track of the current thread being used.
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("WorkManager-WorkManagerTaskExecutor-thread-" + mThreadsCreated);
            mThreadsCreated++;
            mCurrentBackgroundExecutorThread = thread;
            return thread;
        }
    };

    private final ExecutorService mBackgroundExecutor =
            Executors.newSingleThreadExecutor(mBackgroundThreadFactory);

    @Override
    public void postToMainThread(Runnable r) {
        mMainThreadHandler.post(r);
    }

    @Override
    public Executor getMainThreadExecutor() {
        return mMainThreadExecutor;
    }

    @Override
    public void executeOnBackgroundThread(Runnable r) {
        mBackgroundExecutor.execute(r);
    }

    @Override
    public Executor getBackgroundExecutor() {
        return mBackgroundExecutor;
    }

    @NonNull
    @Override
    public Thread getBackgroundExecutorThread() {
        return mCurrentBackgroundExecutorThread;
    }
}
