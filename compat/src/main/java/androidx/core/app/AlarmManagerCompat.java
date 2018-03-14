/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.core.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;

import androidx.annotation.NonNull;

/**
 * Compatibility library for {@link AlarmManager} with fallbacks for older platforms.
 */
public final class AlarmManagerCompat {
    /**
     * Schedule an alarm that represents an alarm clock.
     *
     * The system may choose to display information about this alarm to the user.
     *
     * <p>
     * This method is like {@link #setExact}, but implies
     * {@link AlarmManager#RTC_WAKEUP}.
     *
     * @param alarmManager AlarmManager instance used to set the alarm
     * @param triggerTime time at which the underlying alarm is triggered in wall time
     *                    milliseconds since the epoch
     * @param showIntent an intent that can be used to show or edit details of
     *                    the alarm clock.
     * @param operation Action to perform when the alarm goes off;
     *        typically comes from {@link PendingIntent#getBroadcast
     *        IntentSender.getBroadcast()}.
     *
     * @see AlarmManager#set
     * @see AlarmManager#setRepeating
     * @see AlarmManager#setWindow
     * @see #setExact
     * @see AlarmManager#cancel
     * @see AlarmManager#getNextAlarmClock()
     * @see android.content.Context#sendBroadcast
     * @see android.content.Context#registerReceiver
     * @see android.content.Intent#filterEquals
     */
    public static void setAlarmClock(@NonNull AlarmManager alarmManager, long triggerTime,
            @NonNull PendingIntent showIntent, @NonNull PendingIntent operation) {
        if (Build.VERSION.SDK_INT >= 21) {
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerTime, showIntent),
                    operation);
        } else {
            AlarmManagerCompat.setExact(alarmManager, AlarmManager.RTC_WAKEUP, triggerTime,
                    operation);
        }
    }

    /**
     * Like {@link AlarmManager#set(int, long, PendingIntent)}, but this alarm will be allowed to
     * execute even when the system is in low-power idle modes.  This type of alarm must <b>only</b>
     * be used for situations where it is actually required that the alarm go off while in
     * idle -- a reasonable example would be for a calendar notification that should make a
     * sound so the user is aware of it.  When the alarm is dispatched, the app will also be
     * added to the system's temporary whitelist for approximately 10 seconds to allow that
     * application to acquire further wake locks in which to complete its work.</p>
     *
     * <p>These alarms can significantly impact the power use
     * of the device when idle (and thus cause significant battery blame to the app scheduling
     * them), so they should be used with care.  To reduce abuse, there are restrictions on how
     * frequently these alarms will go off for a particular application.
     * Under normal system operation, it will not dispatch these
     * alarms more than about every minute (at which point every such pending alarm is
     * dispatched); when in low-power idle modes this duration may be significantly longer,
     * such as 15 minutes.</p>
     *
     * <p>Unlike other alarms, the system is free to reschedule this type of alarm to happen
     * out of order with any other alarms, even those from the same app.  This will clearly happen
     * when the device is idle (since this alarm can go off while idle, when any other alarms
     * from the app will be held until later), but may also happen even when not idle.</p>
     *
     * <p>Regardless of the app's target SDK version, this call always allows batching of the
     * alarm.</p>
     *
     * @param alarmManager AlarmManager instance used to set the alarm
     * @param type One of {@link AlarmManager#ELAPSED_REALTIME},
     *        {@link AlarmManager#ELAPSED_REALTIME_WAKEUP},
     *        {@link AlarmManager#RTC}, or {@link AlarmManager#RTC_WAKEUP}.
     * @param triggerAtMillis time in milliseconds that the alarm should go
     * off, using the appropriate clock (depending on the alarm type).
     * @param operation Action to perform when the alarm goes off;
     * typically comes from {@link PendingIntent#getBroadcast
     * IntentSender.getBroadcast()}.
     *
     * @see AlarmManager#set(int, long, PendingIntent)
     * @see #setExactAndAllowWhileIdle
     * @see AlarmManager#cancel
     * @see android.content.Context#sendBroadcast
     * @see android.content.Context#registerReceiver
     * @see android.content.Intent#filterEquals
     * @see AlarmManager#ELAPSED_REALTIME
     * @see AlarmManager#ELAPSED_REALTIME_WAKEUP
     * @see AlarmManager#RTC
     * @see AlarmManager#RTC_WAKEUP
     */
    public static void setAndAllowWhileIdle(@NonNull AlarmManager alarmManager, int type,
            long triggerAtMillis, @NonNull PendingIntent operation) {
        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setAndAllowWhileIdle(type, triggerAtMillis, operation);
        } else {
            alarmManager.set(type, triggerAtMillis, operation);
        }
    }

    /**
     * Schedule an alarm to be delivered precisely at the stated time.
     *
     * <p>
     * This method is like {@link AlarmManager#set(int, long, PendingIntent)}, but does not permit
     * the OS to adjust the delivery time.  The alarm will be delivered as nearly as
     * possible to the requested trigger time.
     *
     * <p>
     * <b>Note:</b> only alarms for which there is a strong demand for exact-time
     * delivery (such as an alarm clock ringing at the requested time) should be
     * scheduled as exact.  Applications are strongly discouraged from using exact
     * alarms unnecessarily as they reduce the OS's ability to minimize battery use.
     *
     * @param alarmManager AlarmManager instance used to set the alarm
     * @param type One of {@link AlarmManager#ELAPSED_REALTIME},
     *        {@link AlarmManager#ELAPSED_REALTIME_WAKEUP},
     *        {@link AlarmManager#RTC}, or {@link AlarmManager#RTC_WAKEUP}.
     * @param triggerAtMillis time in milliseconds that the alarm should go
     *        off, using the appropriate clock (depending on the alarm type).
     * @param operation Action to perform when the alarm goes off;
     *        typically comes from {@link PendingIntent#getBroadcast
     *        IntentSender.getBroadcast()}.
     *
     * @see AlarmManager#set
     * @see AlarmManager#setRepeating
     * @see AlarmManager#setWindow
     * @see AlarmManager#cancel
     * @see android.content.Context#sendBroadcast
     * @see android.content.Context#registerReceiver
     * @see android.content.Intent#filterEquals
     * @see AlarmManager#ELAPSED_REALTIME
     * @see AlarmManager#ELAPSED_REALTIME_WAKEUP
     * @see AlarmManager#RTC
     * @see AlarmManager#RTC_WAKEUP
     */
    public static void setExact(@NonNull AlarmManager alarmManager, int type, long triggerAtMillis,
            @NonNull PendingIntent operation) {
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(type, triggerAtMillis, operation);
        } else {
            alarmManager.set(type, triggerAtMillis, operation);
        }
    }

    /**
     * Like {@link #setExact}, but this alarm will be allowed to execute
     * even when the system is in low-power idle modes.  If you don't need exact scheduling of
     * the alarm but still need to execute while idle, consider using
     * {@link #setAndAllowWhileIdle}.  This type of alarm must <b>only</b>
     * be used for situations where it is actually required that the alarm go off while in
     * idle -- a reasonable example would be for a calendar notification that should make a
     * sound so the user is aware of it.  When the alarm is dispatched, the app will also be
     * added to the system's temporary whitelist for approximately 10 seconds to allow that
     * application to acquire further wake locks in which to complete its work.</p>
     *
     * <p>These alarms can significantly impact the power use
     * of the device when idle (and thus cause significant battery blame to the app scheduling
     * them), so they should be used with care.  To reduce abuse, there are restrictions on how
     * frequently these alarms will go off for a particular application.
     * Under normal system operation, it will not dispatch these
     * alarms more than about every minute (at which point every such pending alarm is
     * dispatched); when in low-power idle modes this duration may be significantly longer,
     * such as 15 minutes.</p>
     *
     * <p>Unlike other alarms, the system is free to reschedule this type of alarm to happen
     * out of order with any other alarms, even those from the same app.  This will clearly happen
     * when the device is idle (since this alarm can go off while idle, when any other alarms
     * from the app will be held until later), but may also happen even when not idle.
     * Note that the OS will allow itself more flexibility for scheduling these alarms than
     * regular exact alarms, since the application has opted into this behavior.  When the
     * device is idle it may take even more liberties with scheduling in order to optimize
     * for battery life.</p>
     *
     * @param alarmManager AlarmManager instance used to set the alarm
     * @param type One of {@link AlarmManager#ELAPSED_REALTIME},
     *        {@link AlarmManager#ELAPSED_REALTIME_WAKEUP},
     *        {@link AlarmManager#RTC}, or {@link AlarmManager#RTC_WAKEUP}.
     * @param triggerAtMillis time in milliseconds that the alarm should go
     *        off, using the appropriate clock (depending on the alarm type).
     * @param operation Action to perform when the alarm goes off;
     *        typically comes from {@link PendingIntent#getBroadcast
     *        IntentSender.getBroadcast()}.
     *
     * @see AlarmManager#set
     * @see AlarmManager#setRepeating
     * @see AlarmManager#setWindow
     * @see AlarmManager#cancel
     * @see android.content.Context#sendBroadcast
     * @see android.content.Context#registerReceiver
     * @see android.content.Intent#filterEquals
     * @see AlarmManager#ELAPSED_REALTIME
     * @see AlarmManager#ELAPSED_REALTIME_WAKEUP
     * @see AlarmManager#RTC
     * @see AlarmManager#RTC_WAKEUP
     */
    public static void setExactAndAllowWhileIdle(@NonNull AlarmManager alarmManager, int type,
            long triggerAtMillis, @NonNull PendingIntent operation) {
        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(type, triggerAtMillis, operation);
        } else {
            AlarmManagerCompat.setExact(alarmManager, type, triggerAtMillis, operation);
        }
    }

    private AlarmManagerCompat() {
    }
}
