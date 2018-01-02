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

package android.arch.background.workmanager.impl;

import static android.arch.background.workmanager.impl.utils.PackageManagerHelper
        .setComponentEnabled;

import android.arch.background.workmanager.WorkManager;
import android.arch.background.workmanager.impl.background.systemalarm.SystemAlarmScheduler;
import android.arch.background.workmanager.impl.background.systemalarm.SystemAlarmService;
import android.arch.background.workmanager.impl.background.systemjob.SystemJobScheduler;
import android.arch.background.workmanager.impl.background.systemjob.SystemJobService;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for {@link WorkManager}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class WorkManagerConfiguration {
    private static final String TAG = "WorkManagerConfig";
    private static final String FIREBASE_JOB_SCHEDULER_CLASSNAME =
            "android.arch.background.workmanager.impl.background.firebase.FirebaseJobScheduler";

    @VisibleForTesting
    static final String FIREBASE_JOB_SERVICE_CLASSNAME =
            "android.arch.background.workmanager.impl.background.firebase.FirebaseJobService";

    private final WorkDatabase mWorkDatabase;
    private final Scheduler mBackgroundScheduler;
    private final ExecutorService mForegroundExecutorService;
    private final ExecutorService mBackgroundExecutorService;
    private final LifecycleOwner mForegroundLifecycleOwner;

    WorkManagerConfiguration(@NonNull Context context) {
        this(context,
                false,
                createForegroundExecutorService(),
                createBackgroundExecutorService(),
                ProcessLifecycleOwner.get());
    }

    @VisibleForTesting
    WorkManagerConfiguration(@NonNull Context context,
            boolean useTestDatabase,
            @NonNull ExecutorService foregroundExecutorService,
            @NonNull ExecutorService backgroundExecutorService,
            @NonNull LifecycleOwner foregroundLifecycleOwner) {
        mWorkDatabase = WorkDatabase.create(context, useTestDatabase);
        mBackgroundScheduler = createBestAvailableBackgroundScheduler(context);
        mForegroundExecutorService = foregroundExecutorService;
        mBackgroundExecutorService = backgroundExecutorService;
        mForegroundLifecycleOwner = foregroundLifecycleOwner;
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
    ExecutorService getForegroundExecutorService() {
        return mForegroundExecutorService;
    }

    @NonNull
    ExecutorService getBackgroundExecutorService() {
        return mBackgroundExecutorService;
    }

    @NonNull
    LifecycleOwner getForegroundLifecycleOwner() {
        return mForegroundLifecycleOwner;
    }

    @NonNull
    private static Scheduler createBestAvailableBackgroundScheduler(@NonNull Context context) {
        Scheduler scheduler;
        boolean enableFirebaseJobService = false;
        boolean enableSystemAlarmService = false;

        if (Build.VERSION.SDK_INT >= 23) {
            scheduler = new SystemJobScheduler(context);
            setComponentEnabled(context, SystemJobService.class, true);
            Log.d(TAG, "Created SystemJobScheduler and enabled SystemJobService");
        } else {
            try {
                scheduler = tryCreateFirebaseJobScheduler(context);
                enableFirebaseJobService = true;
                Log.d(TAG, "Created FirebaseJobScheduler");
            } catch (Exception e) {
                // Also catches the exception thrown if Play Services was not found on the device.
                scheduler = new SystemAlarmScheduler(context);
                enableSystemAlarmService = true;
                Log.d(TAG, "Created SystemAlarmScheduler");
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
    static ExecutorService createForegroundExecutorService() {
        // TODO(sumir): Make this more intelligent.
        return Executors.newFixedThreadPool(4);
    }

    @VisibleForTesting
    static ExecutorService createBackgroundExecutorService() {
        // TODO(sumir): Make this more intelligent.
        return Executors.newSingleThreadExecutor();
    }
}
