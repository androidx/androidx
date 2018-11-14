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

package androidx.work.testing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import androidx.work.Configuration;
import androidx.work.WorkManager;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import java.util.concurrent.Executor;

/**
 * A concrete implementation of {@link WorkManager} which can be used for testing.
 * This implementation makes it easy to swap Schedulers.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class TestWorkManagerImpl extends WorkManagerImpl implements TestDriver {
    TestWorkManagerImpl(
            @NonNull Context context,
            @NonNull Configuration configuration) {

        // Note: This implies that the call to ForceStopRunnable() actually does nothing.
        // This is okay when testing.
        super(
                context,
                configuration,
                new TaskExecutor() {

                    Executor mSynchronousExecutor = new SynchronousExecutor();

                    @Override
                    public void postToMainThread(Runnable runnable) {
                        runnable.run();
                    }

                    @Override
                    public Executor getMainThreadExecutor() {
                        return mSynchronousExecutor;
                    }

                    @Override
                    public void executeOnBackgroundThread(Runnable runnable) {
                        runnable.run();
                    }

                    @Override
                    public Executor getBackgroundExecutor() {
                        return mSynchronousExecutor;
                    }

                    @NonNull
                    @Override
                    public Thread getBackgroundExecutorThread() {
                        return Thread.currentThread();
                    }
                },
                true);
    }
}
