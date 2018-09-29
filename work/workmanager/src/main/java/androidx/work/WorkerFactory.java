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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * An interface for a factory object that creates {@link ListenableWorker} instances.
 */
public interface WorkerFactory {

    /**
     * Returns a new instance of the specified {@code workerClassName} given the arguments.  It
     * should be noted that the returned worker should be a newly-created instance and must not have
     * been previously returned or used by WorkManager.
     *
     * @param appContext The application context
     * @param workerClassName The class name of the worker to create
     * @param workerParameters Parameters for worker initialization
     * @return A new {@link ListenableWorker} instance of type {@code workerClassName}, or
     *         {@code null} if the worker could not be created
     */
    @Nullable
    ListenableWorker createWorker(
            @NonNull Context appContext,
            @NonNull String workerClassName,
            @NonNull WorkerParameters workerParameters);
}
