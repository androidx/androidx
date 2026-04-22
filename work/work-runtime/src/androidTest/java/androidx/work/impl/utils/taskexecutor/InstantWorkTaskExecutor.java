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

package androidx.work.impl.utils.taskexecutor;

import androidx.work.impl.utils.SerialExecutorImpl;
import androidx.work.impl.utils.SynchronousExecutor;

import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

/**
 * A TaskExecutor that runs instantly.
 */
public class InstantWorkTaskExecutor implements TaskExecutor {

    private Executor mSynchronousExecutor = new SynchronousExecutor();
    private SerialExecutorImpl mBackgroundExecutor = new SerialExecutorImpl(mSynchronousExecutor);
    private final CoroutineScope mCoroutineScope =
            CoroutineScopeKt.CoroutineScope(getTaskCoroutineDispatcher());

    @Override
    public @NonNull Executor getMainThreadExecutor() {
        return mSynchronousExecutor;
    }

    @Override
    public void executeOnTaskThread(@NonNull Runnable runnable) {
        runnable.run();
    }

    @Override
    public @NonNull SerialExecutor getSerialTaskExecutor() {
        return mBackgroundExecutor;
    }

    @Override
    public @NonNull CoroutineScope getCoroutineScope() {
        return mCoroutineScope;
    }
}
