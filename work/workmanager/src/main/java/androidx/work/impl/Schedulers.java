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

package androidx.work.impl;

import static androidx.work.impl.utils.PackageManagerHelper.setComponentEnabled;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.impl.background.systemalarm.SystemAlarmScheduler;
import androidx.work.impl.background.systemalarm.SystemAlarmService;
import androidx.work.impl.background.systemjob.SystemJobScheduler;
import androidx.work.impl.background.systemjob.SystemJobService;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Helper methods for {@link Scheduler}s.
 *
 * Helps schedule {@link androidx.work.impl.model.WorkSpec}s while enforcing
 * {@link Scheduler#MAX_SCHEDULER_LIMIT}s.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Schedulers {

    private static final String TAG = "Schedulers";
    private static final String FIREBASE_JOB_SCHEDULER_CLASSNAME =
            "androidx.work.impl.background.firebase.FirebaseJobScheduler";

    @VisibleForTesting
    static final String FIREBASE_JOB_SERVICE_CLASSNAME =
            "androidx.work.impl.background.firebase.FirebaseJobService";

    /**
     * Schedules {@link WorkSpec}s while honoring the {@link Scheduler#MAX_SCHEDULER_LIMIT}.
     *
     * @param workDatabase The {@link WorkDatabase}.
     * @param schedulers   The {@link List} of {@link Scheduler}s to delegate to.
     */
    public static void schedule(
            @NonNull Configuration configuration,
            @NonNull WorkDatabase workDatabase,
            List<Scheduler> schedulers) {
        if (schedulers == null || schedulers.size() == 0) {
            return;
        }

        WorkSpecDao workSpecDao = workDatabase.workSpecDao();
        List<WorkSpec> eligibleWorkSpecs;

        workDatabase.beginTransaction();
        try {
            eligibleWorkSpecs = workSpecDao.getEligibleWorkForScheduling(
                    configuration.getMaxSchedulerLimit());
            if (eligibleWorkSpecs != null && eligibleWorkSpecs.size() > 0) {
                long now = System.currentTimeMillis();

                // Mark all the WorkSpecs as scheduled.
                // Calls to Scheduler#schedule() could potentially result in more schedules
                // on a separate thread. Therefore, this needs to be done first.
                for (WorkSpec workSpec : eligibleWorkSpecs) {
                    workSpecDao.markWorkSpecScheduled(workSpec.id, now);
                }
            }
            workDatabase.setTransactionSuccessful();
        } finally {
            workDatabase.endTransaction();
        }

        if (eligibleWorkSpecs != null && eligibleWorkSpecs.size() > 0) {
            WorkSpec[] eligibleWorkSpecsArray = eligibleWorkSpecs.toArray(new WorkSpec[0]);
            // Delegate to the underlying scheduler.
            for (Scheduler scheduler : schedulers) {
                scheduler.schedule(eligibleWorkSpecsArray);
            }
        }
    }

    static @NonNull Scheduler createBestAvailableBackgroundScheduler(
            @NonNull Context context,
            @NonNull WorkManagerImpl workManager) {

        Scheduler scheduler;
        boolean enableFirebaseJobService = false;
        boolean enableSystemAlarmService = false;

        if (Build.VERSION.SDK_INT >= WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            scheduler = new SystemJobScheduler(context, workManager);
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

        try {
            Class firebaseJobServiceClass = Class.forName(FIREBASE_JOB_SERVICE_CLASSNAME);
            setComponentEnabled(context, firebaseJobServiceClass, enableFirebaseJobService);
        } catch (ClassNotFoundException e) {
            // Do nothing.
        }

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

    private Schedulers() {
    }
}
