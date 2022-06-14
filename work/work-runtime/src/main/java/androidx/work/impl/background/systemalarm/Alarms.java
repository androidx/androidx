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

import static androidx.work.impl.model.SystemIdInfoKt.systemIdInfo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.SystemIdInfo;
import androidx.work.impl.model.SystemIdInfoDao;
import androidx.work.impl.model.WorkGenerationalId;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.IdGenerator;

/**
 * A common class for managing Alarms.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class Alarms {

    private static final String TAG = Logger.tagWithPrefix("Alarms");

    /**
     * Sets an exact alarm after cancelling any existing alarms for the given id.
     *
     * @param context         The application {@link Context}.
     * @param workManager     The instance of {@link WorkManagerImpl}.
     * @param id      The {@link WorkGenerationalId} identifier.
     * @param triggerAtMillis Determines when to trigger the Alarm.
     */
    public static void setAlarm(
            @NonNull Context context,
            @NonNull WorkManagerImpl workManager,
            @NonNull WorkGenerationalId id,
            long triggerAtMillis) {

        WorkDatabase workDatabase = workManager.getWorkDatabase();
        SystemIdInfoDao systemIdInfoDao = workDatabase.systemIdInfoDao();
        SystemIdInfo systemIdInfo = systemIdInfoDao.getSystemIdInfo(id);
        if (systemIdInfo != null) {
            cancelExactAlarm(context, id, systemIdInfo.systemId);
            setExactAlarm(context, id, systemIdInfo.systemId, triggerAtMillis);
        } else {
            IdGenerator idGenerator = new IdGenerator(workDatabase);
            int alarmId = idGenerator.nextAlarmManagerId();
            SystemIdInfo newSystemIdInfo = systemIdInfo(id, alarmId);
            systemIdInfoDao.insertSystemIdInfo(newSystemIdInfo);
            setExactAlarm(context, id, alarmId, triggerAtMillis);
        }
    }

    /**
     * Cancels an existing alarm and removes the {@link SystemIdInfo}.
     *
     * @param context     The application {@link Context}.
     * @param workManager The instance of {@link WorkManagerImpl}.
     * @param id  The {@link WorkSpec} identifier.
     */
    public static void cancelAlarm(
            @NonNull Context context,
            @NonNull WorkManagerImpl workManager,
            @NonNull WorkGenerationalId id) {
        WorkDatabase workDatabase = workManager.getWorkDatabase();
        SystemIdInfoDao systemIdInfoDao = workDatabase.systemIdInfoDao();
        SystemIdInfo systemIdInfo = systemIdInfoDao.getSystemIdInfo(id);
        if (systemIdInfo != null) {
            cancelExactAlarm(context, id, systemIdInfo.systemId);
            Logger.get().debug(TAG,
                    "Removing SystemIdInfo for workSpecId (" + id + ")");
            systemIdInfoDao.removeSystemIdInfo(id);
        }
    }

    private static void cancelExactAlarm(
            @NonNull Context context,
            @NonNull WorkGenerationalId id,
            int alarmId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent delayMet = CommandHandler.createDelayMetIntent(context, id);
        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getService(context, alarmId, delayMet, flags);
        if (pendingIntent != null && alarmManager != null) {
            Logger.get().debug(TAG,
                    "Cancelling existing alarm with (workSpecId, systemId) (" + id
                            + ", " + alarmId + ")");
            alarmManager.cancel(pendingIntent);
        }
    }

    private static void setExactAlarm(
            @NonNull Context context,
            @NonNull WorkGenerationalId id,
            int alarmId,
            long triggerAtMillis) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        Intent delayMet = CommandHandler.createDelayMetIntent(context, id);
        PendingIntent pendingIntent = PendingIntent.getService(context, alarmId, delayMet, flags);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= 19) {
                Api19Impl.setExact(alarmManager, RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.set(RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        }
    }

    private Alarms() {
    }

    @RequiresApi(19)
    static class Api19Impl {
        private Api19Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void setExact(AlarmManager alarmManager, int type, long triggerAtMillis,
                PendingIntent operation) {
            alarmManager.setExact(type, triggerAtMillis, operation);
        }
    }
}
