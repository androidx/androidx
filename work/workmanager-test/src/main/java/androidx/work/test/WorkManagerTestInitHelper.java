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

import java.util.Collections;
import java.util.List;

/**
 * Helps initialize {@link androidx.work.WorkManager} for testing.
 */
public class WorkManagerTestInitHelper {
    /**
     * Initializes {@link androidx.work.WorkManager} with a {@link SynchronousExecutor}
     * and a {@link NoOpScheduler}.
     *
     * @param context The application {@link Context}
     */
    public static void initializeWorkManager(@NonNull Context context) {
        SynchronousExecutor synchronousExecutor = new SynchronousExecutor();
        Configuration configuration = new Configuration.Builder()
                .withExecutor(synchronousExecutor)
                .build();

        Scheduler scheduler = new NoOpScheduler();
        initializeWorkManager(context, configuration, Collections.singletonList(scheduler));
    }

    /**
     * Initializes {@link androidx.work.WorkManager}.
     *
     * @param context       The application {@link Context}
     * @param configuration The {@link Configuration} configuration.
     * @param schedulers    List of Schedulers to use.
     */
    public static void initializeWorkManager(
            @NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull List<Scheduler> schedulers) {

        WorkManagerImpl workManager = new WorkManagerImpl(
                context,
                configuration,
                true,
                schedulers);

        WorkManagerImpl.setDelegate(workManager);
    }

    private WorkManagerTestInitHelper() {
    }
}
