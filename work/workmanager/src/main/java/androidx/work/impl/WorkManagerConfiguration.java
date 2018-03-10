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

package androidx.work.impl;

import static androidx.work.impl.utils.PackageManagerHelper.setComponentEnabled;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.work.R;
import androidx.work.WorkManager;
import androidx.work.impl.background.systemalarm.SystemAlarmScheduler;
import androidx.work.impl.background.systemalarm.SystemAlarmService;
import androidx.work.impl.background.systemjob.SystemJobScheduler;
import androidx.work.impl.background.systemjob.SystemJobService;
import androidx.work.impl.logger.Logger;

/**
 * Configuration for {@link WorkManager}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class WorkManagerConfiguration {
    private static final String TAG = "WorkManagerConfig";
    private static final String FIREBASE_JOB_SCHEDULER_CLASSNAME =
            "androidx.work.impl.background.firebase.FirebaseJobScheduler";

    @VisibleForTesting
    static final String FIREBASE_JOB_SERVICE_CLASSNAME =
            "androidx.work.impl.background.firebase.FirebaseJobService";

    private final WorkDatabase mWorkDatabase;
    private final Scheduler mBackgroundScheduler;
    private final ExecutorService mExecutorService;

    WorkManagerConfiguration(@NonNull Context context) {
        this(context,
                context.getResources().getBoolean(R.bool.workmanager_test_configuration),
                createExecutorService());
    }

    @VisibleForTesting
    WorkManagerConfiguration(@NonNull Context context,
            boolean useTestDatabase,
            @NonNull ExecutorService executorService) {
        mWorkDatabase = WorkDatabase.create(context, useTestDatabase);
        mBackgroundScheduler = createBestAvailableBackgroundScheduler(context);
        mExecutorService = executorService;
    }

    @NonNull
    WorkDatabase getWorkDatabase() {
        return mWorkDatabase;
    }

    @NonNull
    Scheduler getBackgroundScheduler() {
        return mBackgroundScheduler;
    }

    @NonNull
    ExecutorService getExecutorService() {
        return mExecutorService;
    }

    @NonNull
    private static Scheduler createBestAvailableBackgroundScheduler(@NonNull Context context) {
        Scheduler scheduler;
        boolean enableFirebaseJobService = false;
        boolean enableSystemAlarmService = false;

        if (Build.VERSION.SDK_INT >= WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            scheduler = new SystemJobScheduler(context);
            setComponentEnabled(context, SystemJobService.class, true);
            Logger.debug(TAG, "Created SystemJobScheduler and enabled SystemJobService");
        } else {
            try {
                scheduler = tryCreateFirebaseJobScheduler(context);
                enableFirebaseJobService = true;
                Logger.debug(TAG, "Created FirebaseJobScheduler");
            } catch (Exception e) {
                // Also catches the exception thrown if Play Services was not found on the device.
                scheduler = new SystemAlarmScheduler(context);
                enableSystemAlarmService = true;
                Logger.debug(TAG, "Created SystemAlarmScheduler");
            }
        }

        setComponentEnabled(context, FIREBASE_JOB_SERVICE_CLASSNAME, enableFirebaseJobService);
        setComponentEnabled(context, SystemAlarmService.class, enableSystemAlarmService);

        return scheduler;
    }

    @NonNull
    private static Scheduler tryCreateFirebaseJobScheduler(@NonNull Context context)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException,
            InvocationTargetException, NoSuchMethodException {
        Class<?> firebaseJobSchedulerClass = Class.forName(FIREBASE_JOB_SCHEDULER_CLASSNAME);
        return (Scheduler) firebaseJobSchedulerClass
                .getConstructor(Context.class)
                .newInstance(context);
    }

    @VisibleForTesting
    static ExecutorService createExecutorService() {
        return Executors.newFixedThreadPool(
                // This value is the same as the core pool size for AsyncTask#THREAD_POOL_EXECUTOR.
                Math.max(2, Math.min(Runtime.getRuntime().availableProcessors() - 1, 4)));
    }
}
