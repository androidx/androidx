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

import static android.content.ContentValues.TAG;

import android.arch.background.workmanager.background.systemjob.SystemJobScheduler;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

class SchedulerHelper {
    private static final String FIREBASE_SCHEDULER_CLASSNAME =
            "android.arch.background.workmanager.background.firebase.FirebaseJobScheduler";

    private SchedulerHelper() {
    }

    @Nullable
    static Scheduler getBackgroundScheduler(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            Log.d(TAG, "Created SystemJobScheduler");
            return new SystemJobScheduler(context);
        }

        //TODO(sumir): AlarmManagerJobScheduler
        return tryCreateFirebaseJobScheduler(context);
    }

    @Nullable
    private static Scheduler tryCreateFirebaseJobScheduler(@NonNull Context context) {
        Scheduler scheduler = null;
        try {
            Class<?> firebaseSchedulerClass = Class.forName(FIREBASE_SCHEDULER_CLASSNAME);
            scheduler = (Scheduler) firebaseSchedulerClass
                    .getConstructor(Context.class)
                    .newInstance(context);
            Log.d(TAG, "Created FirebaseJobScheduler");
        } catch (Exception e) {
            // Catch all for class cast, invoke, no such method, security exceptions and more.
            // Also thrown if Play Services was not found on device.
            Log.e(TAG, "Could not instantiate FirebaseJobScheduler", e);
        }
        return scheduler;
    }
}
