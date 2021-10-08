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
import androidx.work.impl.utils.SerialExecutor;

import java.util.concurrent.Executor;

/**
 * Default Task Executor for executing common tasks in WorkManager
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkManagerTaskExecutor implements TaskExecutor {

    private final SerialExecutor mBackgroundExecutor;

    public WorkManagerTaskExecutor(@NonNull Executor backgroundExecutor) {
        // Wrap it with a serial executor so we have ordering guarantees on commands
        // being executed.
        mBackgroundExecutor = new SerialExecutor(backgroundExecutor);
    }

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    private final Executor mMainThreadExecutor = new Executor() {
        @Override
        public void execute(@NonNull Runnable command) {
            postToMainThread(command);
        }
    };

    @Override
    public void postToMainThread(Runnable runnable) {
        mMainThreadHandler.post(runnable);
    }

    @Override
    public Executor getMainThreadExecutor() {
        return mMainThreadExecutor;
    }

    @Override
    public void executeOnBackgroundThread(Runnable runnable) {
        mBackgroundExecutor.execute(runnable);
    }

    @Override
    @NonNull
    public SerialExecutor getBackgroundExecutor() {
        return mBackgroundExecutor;
    }
}
