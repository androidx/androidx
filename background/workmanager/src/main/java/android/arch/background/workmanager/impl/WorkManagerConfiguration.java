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

import static android.arch.background.workmanager.impl.utils.PackageManagerHelper.setComponentEnabled;

import android.arch.background.workmanager.impl.background.systemalarm.SystemAlarmScheduler;
import android.arch.background.workmanager.impl.background.systemalarm.SystemAlarmService;
import android.arch.background.workmanager.impl.background.systemjob.SystemJobScheduler;
import android.arch.background.workmanager.impl.background.systemjob.SystemJobService;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

class WorkManagerConfiguration {

    private static final String TAG = "WMConfiguration";
    private static final String FIREBASE_SCHEDULER_CLASSNAME =
            "android.arch.background.workmanager.impl.background.firebase.FirebaseJobScheduler";
    private static final String FIREBASE_SERVICE_CLASSNAME =
            "android.arch.background.workmanager.impl.background.firebase.FirebaseJobService";

    private final WorkDatabase mWorkDatabase;

    private Scheduler mBackgroundScheduler;

    WorkManagerConfiguration(Context context) {
        this(context, false);
    }

    @VisibleForTesting
    WorkManagerConfiguration(Context context, boolean useTestDatabase) {
        mWorkDatabase = WorkDatabase.create(context, useTestDatabase);

        boolean usingSystemJob = (Build.VERSION.SDK_INT >= 23);
        boolean usingFirebase = !usingSystemJob && tryCreateFirebaseJobScheduler(context);
        boolean usingSystemAlarm = !usingSystemJob && !usingFirebase;

        if (usingSystemJob) {
            mBackgroundScheduler = new SystemJobScheduler(context);
            Log.d(TAG, "Created SystemJobScheduler");
        } else if (usingFirebase) {
            // Scheduler already initialized as part of tryCreateFirebaseJobScheduler.
            Log.d(TAG, "Created FirebaseJobScheduler");
        } else if (usingSystemAlarm) {
            mBackgroundScheduler = new SystemAlarmScheduler(context);
            Log.d(TAG, "Created SystemAlarmScheduler");
        }

        if (Build.VERSION.SDK_INT >= 23) {
            // SystemJobService isn't available on older platforms.
            setComponentEnabled(context, SystemJobService.class, usingSystemJob);
        }
        setComponentEnabled(context, FIREBASE_SERVICE_CLASSNAME, usingFirebase);
        setComponentEnabled(context, SystemAlarmService.class, usingSystemAlarm);
    }

    @NonNull WorkDatabase getWorkDatabase() {
        return mWorkDatabase;
    }

    @NonNull Scheduler getBackgroundScheduler() {
        return mBackgroundScheduler;
    }

    private boolean tryCreateFirebaseJobScheduler(@NonNull Context context) {
        try {
            Class<?> firebaseSchedulerClass = Class.forName(FIREBASE_SCHEDULER_CLASSNAME);
            mBackgroundScheduler = (Scheduler) firebaseSchedulerClass
                    .getConstructor(Context.class)
                    .newInstance(context);
        } catch (Exception e) {
            // Catch all for class cast, invoke, no such method, security exceptions and more.
            // Also thrown if Play Services was not found on device.
            Log.e(TAG, "Could not instantiate FirebaseJobScheduler", e);
        }
        return (mBackgroundScheduler != null);
    }
}
