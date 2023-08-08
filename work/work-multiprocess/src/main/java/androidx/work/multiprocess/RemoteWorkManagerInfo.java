/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.work.multiprocess;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.Configuration;
import androidx.work.ForegroundUpdater;
import androidx.work.ProgressUpdater;
import androidx.work.WorkManager;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor;

/**
 * Can keep track of WorkManager configuration and schedulers without having to fully
 * initialize {@link androidx.work.WorkManager} in a remote process.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RemoteWorkManagerInfo {

    private static final Object sLock = new Object();
    private static volatile RemoteWorkManagerInfo sInstance;

    private final Configuration mConfiguration;
    private final TaskExecutor mTaskExecutor;
    private final ProgressUpdater mProgressUpdater;
    private final ForegroundUpdater mForegroundUpdater;

    /**
     * Returns an instance of {@link RemoteWorkManagerInfo}.
     *
     * @param context The application {@link Context}.
     * @return an instance of {@link RemoteWorkManagerInfo} which tracks {@link WorkManager}
     * configuration without having to initialize {@link WorkManager}.
     */
    @NonNull
    public static RemoteWorkManagerInfo getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new RemoteWorkManagerInfo(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * Clears the instance of {@link RemoteWorkManagerInfo}.
     */
    @VisibleForTesting
    public static void clearInstance() {
        synchronized (sLock) {
            sInstance = null;
        }
    }

    @SuppressWarnings("deprecation")
    private RemoteWorkManagerInfo(@NonNull Context context) {
        WorkManagerImpl instance = WorkManagerImpl.getInstance();
        if (instance != null) {
            // WorkManager has been initialized in this process.
            mConfiguration = instance.getConfiguration();
            mTaskExecutor = instance.getWorkTaskExecutor();
        } else {
            Context appContext = context.getApplicationContext();
            if (appContext instanceof Configuration.Provider) {
                Configuration.Provider provider = (Configuration.Provider) appContext;
                mConfiguration = provider.getWorkManagerConfiguration();
            } else {
                // Assume that the configuration to be used is the default configuration.
                mConfiguration = new Configuration.Builder()
                        .setDefaultProcessName(appContext.getPackageName())
                        .build();
            }
            mTaskExecutor = new WorkManagerTaskExecutor(mConfiguration.getTaskExecutor());
        }
        mProgressUpdater = new RemoteProgressUpdater();
        mForegroundUpdater = new RemoteForegroundUpdater();
    }

    /**
     * @return The {@link Configuration} instance which can be used without having to initialize
     * {@link WorkManager}.
     */
    @NonNull
    public Configuration getConfiguration() {
        return mConfiguration;
    }

    /**
     * @return The {@link TaskExecutor} instance that can be used without having to initialize
     * {@link WorkManager}.
     */
    @NonNull
    public TaskExecutor getTaskExecutor() {
        return mTaskExecutor;
    }

    /**
     * @return The {@link androidx.work.ProgressUpdater} instance that can be use without
     * having to initialize {@link WorkManager}.
     */
    @NonNull
    public ProgressUpdater getProgressUpdater() {
        return mProgressUpdater;
    }

    /**
     * @return The {@link androidx.work.ForegroundUpdater} instance that can be use without
     * having to initialize {@link WorkManager}.
     */
    @NonNull
    public ForegroundUpdater getForegroundUpdater() {
        return mForegroundUpdater;
    }
}
