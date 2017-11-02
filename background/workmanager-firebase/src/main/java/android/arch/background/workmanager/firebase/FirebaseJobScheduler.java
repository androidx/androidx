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

package android.arch.background.workmanager.firebase;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.arch.background.workmanager.Scheduler;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.utils.IdGenerator;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;

/**
 * A class that schedules work using {@link FirebaseJobDispatcher}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FirebaseJobScheduler implements Scheduler {
    private static final String TAG = "FirebaseJobScheduler";
    private FirebaseJobDispatcher mDispatcher;
    private FirebaseJobConverter mJobConverter;
    private IdGenerator mIdGenerator;
    private AlarmManager mAlarmManager;
    private Context mAppContext;

    public FirebaseJobScheduler(Context context) {
        // TODO(xbhatnag): Check for Play Services. Throw Exception if not found.
        mAppContext = context.getApplicationContext();
        mDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(mAppContext));
        mJobConverter = new FirebaseJobConverter(mDispatcher);
    }

    @Override
    public void schedule(WorkSpec... workSpecs) {
        for (WorkSpec workSpec : workSpecs) {
            if (workSpec.calculateDelay() > 0) {
                scheduleLater(workSpec);
            } else {
                scheduleNow(workSpec);
            }
        }
    }

    void scheduleNow(WorkSpec workSpec) {
        Job job = mJobConverter.convert(workSpec);
        Log.d(TAG, "Scheduling work now, ID: " + workSpec.getId());
        int result = mDispatcher.schedule(job);
        if (result != FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS) {
            Log.e(TAG, "Schedule failed. Result = " + result);
        }
    }

    private void scheduleLater(WorkSpec workSpec) {
        // TODO(xbhatnag): Exact or Inexact Initial Delay?
        if (mAlarmManager == null) {
            mAlarmManager = (AlarmManager) mAppContext.getSystemService(Context.ALARM_SERVICE);
        }
        if (mIdGenerator == null) {
            mIdGenerator = new IdGenerator(mAppContext);
        }
        Log.d(TAG, "Scheduling work later, ID: " + workSpec.getId());
        PendingIntent pendingIntent = createScheduleLaterPendingIntent(workSpec);

        // This sets an alarm to wake up the device at System Current Time + Calculated Delay.
        // The wakeup is inexact for API 19+ and exact for API 14 - 18. A wakeup is necessary
        // because the device could be sleeping when this alarm is fired.
        mAlarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + workSpec.calculateDelay(),
                pendingIntent);
    }

    private PendingIntent createScheduleLaterPendingIntent(WorkSpec workSpec) {
        Intent intent = new Intent(mAppContext, FirebaseDelayedJobAlarmReceiver.class);
        intent.putExtra(FirebaseDelayedJobAlarmReceiver.WORKSPEC_ID_KEY, workSpec.getId());
        int requestCode = mIdGenerator.nextFirebaseAlarmId();
        return PendingIntent.getBroadcast(mAppContext, requestCode, intent, 0);
    }
}
