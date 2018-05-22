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

package androidx.work.test;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.Configuration;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Helps initialize {@link androidx.work.WorkManager} for testing.
 */
public final class WorkManagerTestInitHelper {
    /**
     * Initializes {@link androidx.work.WorkManager} with a {@link SynchronousExecutor}.
     *
     * @param context The application {@link Context}
     */
    public static void initializeTestWorkManager(@NonNull Context context) {
        setupSynchronousTaskExecutor();
        SynchronousExecutor synchronousExecutor = new SynchronousExecutor();
        Configuration configuration = new Configuration.Builder()
                .setExecutor(synchronousExecutor)
                .build();

        final TestScheduler scheduler = new TestScheduler();
        WorkManagerImpl workManager = new TestWorkManagerImpl(context, configuration) {
            @NonNull
            @Override
            public List<Scheduler> getSchedulers() {
                return Collections.singletonList((Scheduler) scheduler);
            }

            @Override
            public void setAllConstraintsMet(@NonNull UUID workSpecId) {
                scheduler.setAllConstraintsMet(workSpecId);
            }
        };
        workManager.getProcessor().addExecutionListener(scheduler);
        WorkManagerImpl.setDelegate(workManager);
    }

    /**
     * @return An instance of {@link TestDriver}. This exposes additional functionality
     * that are useful in the context of testing when using WorkManager.
     */
    public static TestDriver getTestDriver() {
        WorkManagerImpl workManager = WorkManagerImpl.getInstance();
        if (workManager == null) {
            return null;
        } else {
            return ((TestWorkManagerImpl) WorkManagerImpl.getInstance());
        }
    }

    private WorkManagerTestInitHelper() {
    }

    private static void setupSynchronousTaskExecutor() {
        WorkManagerTaskExecutor.getInstance()
                .setTaskExecutor(new TaskExecutor() {
                    @Override
                    public void postToMainThread(Runnable runnable) {
                        runnable.run();
                    }

                    @Override
                    public void executeOnBackgroundThread(Runnable runnable) {
                        runnable.run();
                    }
                });
    }
}
