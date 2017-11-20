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

package android.arch.background.workmanager;

import android.arch.background.workmanager.background.systemalarm.SystemAlarmScheduler;
import android.arch.background.workmanager.background.systemalarm.SystemAlarmService;
import android.arch.background.workmanager.background.systemjob.SystemJobScheduler;
import android.arch.background.workmanager.background.systemjob.SystemJobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

class WorkManagerConfiguration {

    private static final String TAG = "WMConfiguration";
    private static final String FIREBASE_SCHEDULER_CLASSNAME =
            "android.arch.background.workmanager.background.firebase.FirebaseJobScheduler";

    private Scheduler mScheduler;

    WorkManagerConfiguration(Context context) {
        boolean usingSystemJob = (Build.VERSION.SDK_INT >= 23);
        boolean usingFirebase = !usingSystemJob && tryCreateFirebaseJobScheduler(context);
        boolean usingSystemAlarm = !usingSystemJob && !usingFirebase;

        if (usingSystemJob) {
            mScheduler = new SystemJobScheduler(context);
            Log.d(TAG, "Created SystemJobScheduler");
        } else if (usingFirebase) {
            // Scheduler already initialized as part of tryCreateFirebaseJobScheduler.
        } else if (usingSystemAlarm) {
            mScheduler = new SystemAlarmScheduler(context);
            Log.d(TAG, "Created SystemAlarmScheduler");
        }

        if (Build.VERSION.SDK_INT >= 23) {
            // SystemJobService isn't available on older platforms.
            setComponentEnabled(context, SystemJobService.class.getName(), usingSystemJob);
        }
        setComponentEnabled(context, FIREBASE_SCHEDULER_CLASSNAME, usingFirebase);
        setComponentEnabled(context, SystemAlarmService.class.getName(), usingSystemAlarm);
    }

    @NonNull Scheduler getBackgroundScheduler() {
        return mScheduler;
    }

    private boolean tryCreateFirebaseJobScheduler(@NonNull Context context) {
        try {
            Class<?> firebaseSchedulerClass = Class.forName(FIREBASE_SCHEDULER_CLASSNAME);
            mScheduler = (Scheduler) firebaseSchedulerClass
                    .getConstructor(Context.class)
                    .newInstance(context);
            Log.d(TAG, "Created FirebaseJobScheduler");
        } catch (Exception e) {
            // Catch all for class cast, invoke, no such method, security exceptions and more.
            // Also thrown if Play Services was not found on device.
            Log.e(TAG, "Could not instantiate FirebaseJobScheduler", e);
        }
        return (mScheduler != null);
    }

    private static void setComponentEnabled(Context context, String className, boolean enabled) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ComponentName componentName = new ComponentName(context, className);
            packageManager.setComponentEnabledSetting(componentName,
                    enabled
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            Log.d(TAG, className + " " + (enabled ? "enabled" : "disabled"));
        } catch (Exception e) {
            Log.d(TAG,
                    className + " could not be " + (enabled ? "enabled" : "disabled"),
                    e);
        }
    }

}
