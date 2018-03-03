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

import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.logger.Logger;
import androidx.work.impl.model.AlarmInfo;
import androidx.work.impl.model.AlarmInfoDao;
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
        AlarmInfoDao alarmInfoDao = workDatabase.alarmInfoDao();
        AlarmInfo alarmInfo = alarmInfoDao.getAlarmInfo(workSpecId);
        if (alarmInfo != null) {
            cancelExactAlarm(context, workSpecId, alarmInfo.getAlarmId());
            setExactAlarm(context, workSpecId, alarmInfo.getAlarmId(), triggerAtMillis);
        } else {
            IdGenerator idGenerator = new IdGenerator(context);
            int alarmId = idGenerator.nextAlarmManagerId();
            AlarmInfo newAlarmInfo = new AlarmInfo(workSpecId, alarmId);
            alarmInfoDao.insertAlarmInfo(newAlarmInfo);
            setExactAlarm(context, workSpecId, alarmId, triggerAtMillis);
        }
    }

    /**
     * Cancels an existing alarm and removes the {@link AlarmInfo}.
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
        AlarmInfoDao alarmInfoDao = workDatabase.alarmInfoDao();
        AlarmInfo alarmInfo = alarmInfoDao.getAlarmInfo(workSpecId);
        if (alarmInfo != null) {
            cancelExactAlarm(context, workSpecId, alarmInfo.getAlarmId());
            Logger.debug(TAG, "Removing AlarmInfo for workSpecId (%s)", workSpecId);
            alarmInfoDao.removeAlarmInfo(workSpecId);
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
            Logger.debug(TAG, "Cancelling existing alarm with (workSpecId, alarmId) (%s, %s)",
                    workSpecId, alarmId);
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
}
