/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.manager;

import static java.lang.Long.max;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.app.AlarmManager;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;

class UpdateScheduler implements AlarmManager.OnAlarmListener {
    private static final String TAG = "UpdateScheduler";

    @VisibleForTesting static final long MIN_INTER_UPDATE_INTERVAL_MILLIS = SECONDS.toMillis(20);
    private static final long NO_SCHEDULED_UPDATE = Long.MAX_VALUE;

    private final AlarmManager mAlarmManager;
    private final Clock mClock;
    private UpdateReceiver mUpdateReceiver;

    private boolean mUpdatesEnabled = false;
    private long mScheduledUpdateTimeMillis = NO_SCHEDULED_UPDATE;

    // Last time at which we updated the tile, measured by the device uptime. This needs to be
    // device uptime to prevent issues when time changes (e.g. time jumps caused by syncs with NTP
    // or similar).
    private long mLastUpdateRealtimeMillis = 0;

    UpdateScheduler(AlarmManager alarmManager, Clock clock) {
        this.mAlarmManager = alarmManager;
        this.mClock = clock;
    }

    /** Sets the receiver for update notifications. */
    @MainThread
    public void setUpdateReceiver(UpdateReceiver receiver) {
        this.mUpdateReceiver = receiver;
    }

    /**
     * Schedule an update at some point in the future. Note that this method will cancel any
     * previous scheduled updates. Note also that if the requested time is too close to the previous
     * update time (either a previously fired schedule update, or a previous call to updateNow), the
     * update may by delayed.
     *
     * @param scheduleTimeMillis The time to schedule an update at. Note, this is elapsed real time,
     *     **not** wall-clock time.
     */
    @MainThread
    public void scheduleUpdateAtTime(long scheduleTimeMillis) {
        scheduleUpdateInternal(
                max(
                        scheduleTimeMillis,
                        mLastUpdateRealtimeMillis + MIN_INTER_UPDATE_INTERVAL_MILLIS));
    }

    /**
     * Schedule an update now. This also cancels any previous scheduled updates. Note that if {@code
     * force} is false, the update may be delayed in order to respect the minimum inter-update
     * interval.
     *
     * <p>Note that the registered {@link UpdateReceiver} may be called directly from this method;
     * you should avoid triggering forced updates within the registered {@link UpdateReceiver}.
     *
     * @param force Whether to force the update (ignore minimum inter-update interval).
     */
    @MainThread
    public void updateNow(boolean force) {
        cancelScheduledUpdates();

        long nowMillis = mClock.getElapsedTimeMillis();

        // Can we update now, or should we schedule at some point in the future?
        if (nowMillis < (mLastUpdateRealtimeMillis + MIN_INTER_UPDATE_INTERVAL_MILLIS) && !force) {
            // Schedule update instead.
            scheduleUpdateInternal(mLastUpdateRealtimeMillis + MIN_INTER_UPDATE_INTERVAL_MILLIS);
        } else {
            if (mUpdatesEnabled) {
                fireUpdate();
            } else {
                // "Schedule" an update. This is just so enableUpdates will definitely trigger the
                // update when called.
                mScheduledUpdateTimeMillis = nowMillis;
            }
        }
    }

    /** Schedule an update *without* checking the inter-update frequency. */
    private void scheduleUpdateInternal(long scheduleTimeMillis) {
        cancelScheduledUpdates();

        mScheduledUpdateTimeMillis = scheduleTimeMillis;

        if (mUpdatesEnabled) {
            mAlarmManager.set(AlarmManager.ELAPSED_REALTIME, scheduleTimeMillis, TAG, this, null);
        }
    }

    @MainThread
    public void enableUpdates() {
        if (mUpdatesEnabled) {
            return;
        }

        if (mScheduledUpdateTimeMillis != Long.MAX_VALUE) {
            // If the schedule update is in the past, then fire now, otherwise schedule for the
            // given time.
            long now = mClock.getElapsedTimeMillis();

            if (now >= mScheduledUpdateTimeMillis) {
                onAlarm();
            } else {
                mAlarmManager.set(
                        AlarmManager.ELAPSED_REALTIME, mScheduledUpdateTimeMillis, TAG, this, null);
            }
        }

        mUpdatesEnabled = true;
    }

    @MainThread
    public void disableUpdates() {
        if (!mUpdatesEnabled) {
            return;
        }

        // Just deschedule the alarm. Don't touch any other flags.
        mAlarmManager.cancel(this);

        mUpdatesEnabled = false;
    }

    /** Cancel any scheduled updates. */
    @MainThread
    public void cancelScheduledUpdates() {
        mAlarmManager.cancel(this);

        mScheduledUpdateTimeMillis = NO_SCHEDULED_UPDATE;
    }

    private void fireUpdate() {
        mLastUpdateRealtimeMillis = mClock.getElapsedTimeMillis();

        UpdateReceiver receiver = mUpdateReceiver;

        // Reset state now, as acceptUpdate may re-schedule an alarm.
        mScheduledUpdateTimeMillis = Long.MAX_VALUE;

        if (receiver != null) {
            receiver.acceptUpdate();
        }
    }

    @Override
    public void onAlarm() {
        if (mScheduledUpdateTimeMillis == Long.MAX_VALUE) {
            Log.i(TAG, "Received update notification, but no update was scheduled");
            return;
        }

        fireUpdate();
    }

    /** Receiver for update notifications. */
    interface UpdateReceiver {
        /** Called by the {@link UpdateScheduler} when an update should occur. */
        void acceptUpdate();
    }

    interface Clock {
        long getElapsedTimeMillis();
    }
}
