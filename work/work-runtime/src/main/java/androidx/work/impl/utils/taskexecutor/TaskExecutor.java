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

import androidx.annotation.RestrictTo;
import androidx.work.Configuration;

import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.ExecutorsKt;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

/**
 * Interface for executing common tasks in WorkManager.
 *
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface TaskExecutor {
    /**
     * @return The {@link Executor} for main thread task processing
     */
    @NonNull Executor getMainThreadExecutor();

    /**
     * @param runnable {@link Runnable} to execute on a thread pool used
     *                 for internal book-keeping.
     */
    default void executeOnTaskThread(@NonNull Runnable runnable) {
        getSerialTaskExecutor().execute(runnable);
    }

    /**
     * It wraps an executor passed in {@link Configuration#getTaskExecutor()}
     *
     * Executor must guarantee a serial execution.
     *
     * @return The {@link Executor} for internal book-keeping
     */
    @NonNull SerialExecutor getSerialTaskExecutor();

    /**
     * Get the coroutine scope used for all WorkManager tasks
     */
    @NonNull CoroutineScope getCoroutineScope();

    default @NonNull CoroutineDispatcher getTaskCoroutineDispatcher() {
        return ExecutorsKt.from(getSerialTaskExecutor());
    }

    /**
     * Dispatcher for event listener hooks e.g. {@link Configuration#getExecutionEventListener()}.
     * These events maintain an ordering guarantee with each other but not necessarily with any
     * other tasks on the task executor.
     *
     * @return The {@link CoroutineDispatcher} for running event listener hooks
     */
    default @NonNull CoroutineDispatcher getEventListenerDispatcher() {
        return getTaskCoroutineDispatcher();
    }
}
