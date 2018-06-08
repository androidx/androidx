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

package androidx.work.impl.background.systemalarm;

import static android.app.AlarmManager.RTC_WAKEUP;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;

import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.SystemIdInfo;
import androidx.work.impl.model.SystemIdInfoDao;
import androidx.work.impl.utils.IdGenerator;

/**
 * A common class for managing Alarms.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class Alarms {

    private static final String TAG = "Alarms";

    /**
     * Sets an exact alarm after cancelling any existing alarms for the given id.
     *
     * @param context         The application {@link Context}.
     * @param workManager     The instance of {@link WorkManagerImpl}.
     * @param workSpecId      The {@link androidx.work.impl.model.WorkSpec} identifier.
     * @param triggerAtMillis Determines when to trigger the Alarm.
     */
    public static void setAlarm(
            @NonNull Context context,
            @NonNull WorkManagerImpl workManager,
            @NonNull String workSpecId,
            long triggerAtMillis) {

        WorkDatabase workDatabase = workManager.getWorkDatabase();
        SystemIdInfoDao systemIdInfoDao = workDatabase.systemIdInfoDao();
        SystemIdInfo systemIdInfo = systemIdInfoDao.getSystemIdInfo(workSpecId);
        if (systemIdInfo != null) {
            cancelExactAlarm(context, workSpecId, systemIdInfo.systemId);
            setExactAlarm(context, workSpecId, systemIdInfo.systemId, triggerAtMillis);
        } else {
            IdGenerator idGenerator = new IdGenerator(context);
            int alarmId = idGenerator.nextAlarmManagerId();
            SystemIdInfo newSystemIdInfo = new SystemIdInfo(workSpecId, alarmId);
            systemIdInfoDao.insertSystemIdInfo(newSystemIdInfo);
            setExactAlarm(context, workSpecId, alarmId, triggerAtMillis);
        }
    }

    /**
     * Cancels an existing alarm and removes the {@link SystemIdInfo}.
     *
     * @param context     The application {@link Context}.
     * @param workManager The instance of {@link WorkManagerImpl}.
     * @param workSpecId  The {@link androidx.work.impl.model.WorkSpec} identifier.
     */
    public static void cancelAlarm(
            @NonNull Context context,
            @NonNull WorkManagerImpl workManager,
            @NonNull String workSpecId) {

        WorkDatabase workDatabase = workManager.getWorkDatabase();
        SystemIdInfoDao systemIdInfoDao = workDatabase.systemIdInfoDao();
        SystemIdInfo systemIdInfo = systemIdInfoDao.getSystemIdInfo(workSpecId);
        if (systemIdInfo != null) {
            cancelExactAlarm(context, workSpecId, systemIdInfo.systemId);
            Log.d(TAG, String.format("Removing SystemIdInfo for workSpecId (%s)", workSpecId));
            systemIdInfoDao.removeSystemIdInfo(workSpecId);
        }
    }

    private static void cancelExactAlarm(
            @NonNull Context context,
            @NonNull String workSpecId,
            int alarmId) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent delayMet = CommandHandler.createDelayMetIntent(context, workSpecId);
        PendingIntent pendingIntent = PendingIntent.getService(
                context, alarmId, delayMet, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null && alarmManager != null) {
            Log.d(TAG, String.format(
                    "Cancelling existing alarm with (workSpecId, systemId) (%s, %s)",
                    workSpecId,
                    alarmId));
            alarmManager.cancel(pendingIntent);
        }
    }

    private static void setExactAlarm(
            @NonNull Context context,
            @NonNull String workSpecId,
            int alarmId,
            long triggerAtMillis) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent delayMet = CommandHandler.createDelayMetIntent(context, workSpecId);
        PendingIntent pendingIntent = PendingIntent.getService(
                context, alarmId, delayMet, PendingIntent.FLAG_ONE_SHOT);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.set(RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        }
    }

    private Alarms() {
    }
}
