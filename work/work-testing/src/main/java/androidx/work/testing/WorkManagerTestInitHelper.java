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

import static androidx.work.testing.TestWorkManagerImplKt.createTestWorkManagerImpl;
import static androidx.work.testing.WorkManagerTestInitHelper.ExecutorsMode.LEGACY_OVERRIDE_WITH_SYNCHRONOUS_EXECUTORS;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Configuration;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.utils.SerialExecutorImpl;
import androidx.work.impl.utils.taskexecutor.SerialExecutor;

import java.util.UUID;


/**
 * Helps initialize {@link androidx.work.WorkManager} for testing.
 */
public final class WorkManagerTestInitHelper {
    /**
     * Initializes a test {@link androidx.work.WorkManager} with a {@link SynchronousExecutor}.
     *
     * @param context The application {@link Context}
     */
    public static void initializeTestWorkManager(@NonNull Context context) {
        SynchronousExecutor synchronousExecutor = new SynchronousExecutor();
        Configuration configuration = new Configuration.Builder()
                .setExecutor(synchronousExecutor)
                .setTaskExecutor(synchronousExecutor)
                .build();
        initializeTestWorkManager(context, configuration);
    }

    /**
     * Initializes a test {@link androidx.work.WorkManager} with a user-specified
     * {@link androidx.work.Configuration}, but using
     * {@link SynchronousExecutor} instead of main thread.
     *
     * @param context       The application {@link Context}
     * @param configuration The {@link androidx.work.Configuration}
     */
    public static void initializeTestWorkManager(
            @NonNull Context context,
            @NonNull Configuration configuration) {
        initializeTestWorkManager(context, configuration,
                LEGACY_OVERRIDE_WITH_SYNCHRONOUS_EXECUTORS);
    }

    /**
     * Modes that control which executors are used in tests.
     */
    public enum ExecutorsMode {
        /**
         * Use executors as they are configured in passed {@link Configuration} and preserving
         * real main thread.
         */
        PRESERVE_EXECUTORS,

        /**
         * Preserve old behavior of {@link #initializeTestWorkManager(Context)} and
         * {@link #initializeTestWorkManager(Context, Configuration)}.
         *
         * <p> In this mode {@link SynchronousExecutor} is used instead of main thread. Similarly,
         * {@link SynchronousExecutor} is used as {@link Configuration#getTaskExecutor()}, unless
         * {@link Configuration#getTaskExecutor()} was explicitly set in {@code configuration}
         * passed in {@link #initializeTestWorkManager(Context, Configuration, ExecutorsMode)}
         */
        LEGACY_OVERRIDE_WITH_SYNCHRONOUS_EXECUTORS,

        /**
         * Like {@link #PRESERVE_EXECUTORS}, but uses the real Clock and RunnableScheduler in
         * the provided {@link Configuration} instead of the {@link TestDriver} setDelayMet()
         * methods to run scheduled work.
         *
         * <p> Work will be passed to RunnableScheduler with appropriate time-based delay, and the
         * RunnableScheduler must reschedule the work itself when the clock delay has passed.
         *
         * {@link TestDriver#setInitialDelayMet(UUID)} and
         * {@link TestDriver#setPeriodDelayMet(UUID)}
         * throw exceptions when this configuration is used.
         *
         * <p> This mode is intended for integrated fake clock / schedule test frameworks, eg.
         * {@link kotlinx.coroutines.test.StandardTestDispatcherImpl} with
         * {@link kotlinx.coroutines.test.TestCoroutineScheduler}
         */
        USE_TIME_BASED_SCHEDULING,
    }

    /**
     * Initializes a test {@link androidx.work.WorkManager} that can be controlled via {@link
     * TestDriver}.
     *
     * @param context       The application {@link Context}
     * @param configuration test configuration of WorkManager
     * @param executorsMode mode controlling executors used by WorkManager in tests. See
     *                      documentation of modes in {@link ExecutorsMode}
     */
    public static void initializeTestWorkManager(@NonNull Context context,
            @NonNull Configuration configuration, @NonNull ExecutorsMode executorsMode) {
        WorkManagerImpl workManager;
        switch (executorsMode) {
            case LEGACY_OVERRIDE_WITH_SYNCHRONOUS_EXECUTORS:
                SerialExecutor serialExecutor;
                if (configuration.isUsingDefaultTaskExecutor()) {
                    Configuration.Builder builder = new Configuration.Builder(configuration)
                            .setTaskExecutor(new SynchronousExecutor());
                    configuration = builder.build();
                    serialExecutor = new SynchronousSerialExecutor();
                } else {
                    serialExecutor = new SerialExecutorImpl(configuration.getTaskExecutor());
                }
                workManager = createTestWorkManagerImpl(
                        context, configuration, serialExecutor, executorsMode);
                break;
            case PRESERVE_EXECUTORS:
            case USE_TIME_BASED_SCHEDULING:
                workManager = createTestWorkManagerImpl(
                        context, configuration, executorsMode);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + executorsMode);
        }
        WorkManagerImpl.setDelegate(workManager);
    }

    /**
     * Initializes a test {@link androidx.work.WorkManager} that can be controlled via {@link
     * TestDriver}.
     *
     * @param context       The application {@link Context}
     * @param executorsMode mode controlling executors used by WorkManager in tests. See
     *                      documentation of modes in {@link ExecutorsMode}
     */
    public static void initializeTestWorkManager(@NonNull Context context,
            @NonNull ExecutorsMode executorsMode) {
        Configuration configuration;
        if (executorsMode == LEGACY_OVERRIDE_WITH_SYNCHRONOUS_EXECUTORS) {
            SynchronousExecutor synchronousExecutor = new SynchronousExecutor();
            configuration = new Configuration.Builder()
                    .setExecutor(synchronousExecutor)
                    .setTaskExecutor(synchronousExecutor)
                    .build();
        } else {
            configuration = new Configuration.Builder().build();
        }
        initializeTestWorkManager(context, configuration, executorsMode);
    }

    /**
     * @return An instance of {@link TestDriver}. This exposes additional functionality that is
     * useful in the context of testing when using WorkManager.
     * @deprecated Call {@link WorkManagerTestInitHelper#getTestDriver(Context)} instead.
     */
    @Deprecated
    public static @Nullable TestDriver getTestDriver() {
        WorkManagerImpl workManager = WorkManagerImpl.getInstance();
        if (workManager == null) {
            return null;
        } else {
            return TestWorkManagerImplKt.getTestDriver(workManager);
        }
    }

    /**
     * @return An instance of {@link TestDriver}. This exposes additional functionality that is
     * useful in the context of testing when using WorkManager.
     */
    public static @Nullable TestDriver getTestDriver(@NonNull Context context) {
        try {
            return TestWorkManagerImplKt.getTestDriver(WorkManagerImpl.getInstance(context));
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * Closes internal {@link androidx.work.WorkManager}'s database.
     * <p>
     * It could be helpful to avoid warnings by CloseGuard in testing infra. You need to be
     * make sure that {@code WorkManager} finished all operations and won't touch database
     * anymore. Meaning that both {@link Configuration#getTaskExecutor()} and
     * {@link Configuration#getExecutor()} are idle.
     */
    @SuppressWarnings("deprecation")
    public static void closeWorkDatabase() {
        WorkManagerImpl workManager = WorkManagerImpl.getInstance();
        if (workManager != null) {
            WorkDatabase workDatabase = workManager.getWorkDatabase();
            workDatabase.close();
        }
    }

    private WorkManagerTestInitHelper() {
    }
}
