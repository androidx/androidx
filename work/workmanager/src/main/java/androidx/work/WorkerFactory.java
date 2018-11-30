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
import android.support.annotation.RestrictTo;

import java.lang.reflect.Constructor;

/**
 * A factory object that creates {@link ListenableWorker} instances.  The factory is invoked every
 * time a work runs.  You can override the default implementation of this factory by manually
 * initializing {@link WorkManager} (see {@link WorkManager#initialize(Context, Configuration)} and
 * specifying a new WorkerFactory in {@link Configuration.Builder#setWorkerFactory(WorkerFactory)}.
 */

public abstract class WorkerFactory {

    private static final String TAG = "WorkerFactory";

    /**
     * Override this method to implement your custom worker-creation logic.  Use
     * {@link Configuration.Builder#setWorkerFactory(WorkerFactory)} to use your custom class.
     * <p></p>
     * Returns a new instance of the specified {@code workerClassName} given the arguments.  The
     * returned worker should be a newly-created instance and must not have been previously returned
     * or used by WorkManager.
     *
     * @param appContext The application context
     * @param workerClassName The class name of the worker to create
     * @param workerParameters Parameters for worker initialization
     * @return A new {@link ListenableWorker} instance of type {@code workerClassName}, or
     *         {@code null} if the worker could not be created
     */
    public abstract @Nullable ListenableWorker createWorker(
            @NonNull Context appContext,
            @NonNull String workerClassName,
            @NonNull WorkerParameters workerParameters);

    /**
     * Returns a new instance of the specified {@code workerClassName} given the arguments.  If no
     * worker is found, default reflection-based code will be used to instantiate the worker with
     * the current ClassLoader.  The returned worker should be a newly-created instance and must not
     * have been previously returned or used by WorkManager.
     *
     * @param appContext The application context
     * @param workerClassName The class name of the worker to create
     * @param workerParameters Parameters for worker initialization
     * @return A new {@link ListenableWorker} instance of type {@code workerClassName}, or
     *         {@code null} if the worker could not be created
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final @Nullable ListenableWorker createWorkerWithDefaultFallback(
            @NonNull Context appContext,
            @NonNull String workerClassName,
            @NonNull WorkerParameters workerParameters) {

        ListenableWorker worker;
        worker = createWorker(appContext, workerClassName, workerParameters);
        if (worker != null) {
            return worker;
        }

        Class<? extends ListenableWorker> clazz;
        try {
            clazz = Class.forName(workerClassName).asSubclass(ListenableWorker.class);
        } catch (ClassNotFoundException e) {
            Logger.get().error(TAG, "Class not found: " + workerClassName);
            return null;
        }

        try {
            Constructor<? extends ListenableWorker> constructor =
                    clazz.getDeclaredConstructor(Context.class, WorkerParameters.class);
            worker = constructor.newInstance(
                    appContext,
                    workerParameters);
            return worker;
        } catch (Exception e) {
            Logger.get().error(TAG, "Could not instantiate " + workerClassName, e);
        }
        return null;
    }

    /**
     * @return A default {@link WorkerFactory} with no custom behavior
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static WorkerFactory getDefaultWorkerFactory() {
        return new WorkerFactory() {

            @Override
            public @Nullable ListenableWorker createWorker(
                    @NonNull Context appContext,
                    @NonNull String workerClassName,
                    @NonNull WorkerParameters workerParameters) {
                return null;
            }
        };
    }
}
