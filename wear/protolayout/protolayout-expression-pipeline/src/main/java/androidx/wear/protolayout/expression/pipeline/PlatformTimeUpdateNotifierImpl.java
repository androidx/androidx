/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Controls notifying for time-related updates using Android's clock. Updates can also be
 * enabled/disabled.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public class PlatformTimeUpdateNotifierImpl implements PlatformTimeUpdateNotifier {
    private static final String TAG = "PlatformTimeUpdateNotifierImpl";
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
    @Nullable private Supplier<ListenableFuture<Void>> mRegisteredReceiver;
    private final Runnable mNotifyAndSchedule = this::notifyAndScheduleNextSecond;
    private long mLastScheduleTimeMillis = 0;
    private boolean mUpdatesEnabled = false;
    private final MainThreadExecutor mExecutor = new MainThreadExecutor(mUiHandler);

    @Override
    public void setReceiver(@NonNull Supplier<ListenableFuture<Void>> tick) {
        if (mRegisteredReceiver != null) {
            Log.w(TAG, "Clearing previously set receiver.");
            clearReceiver();
        }
        mRegisteredReceiver = tick;

        if (mUpdatesEnabled) {
            // Send first update and schedule next.
            mLastScheduleTimeMillis = SystemClock.uptimeMillis();
            scheduleNextSecond();
            runReceiver();
        }
    }

    @Override
    public void clearReceiver() {
        mRegisteredReceiver = null;

        // There are no more registered callbacks, stop the periodic call.
        if (this.mUpdatesEnabled) {
            mUiHandler.removeCallbacks(this.mNotifyAndSchedule, this);
        }
    }

    /** Sets whether this notifier can send updates on the given receiver. */
    public void setUpdatesEnabled(boolean updatesEnabled) {
        if (updatesEnabled == this.mUpdatesEnabled) {
            return;
        }

        this.mUpdatesEnabled = updatesEnabled;

        if (!updatesEnabled) {
            mUiHandler.removeCallbacks(this.mNotifyAndSchedule, this);
        } else if (mRegisteredReceiver != null) {
            mLastScheduleTimeMillis = SystemClock.uptimeMillis();
            scheduleNextSecond();
        }
    }


    @SuppressWarnings("ExecutorTaskName")
    private void notifyAndScheduleNextSecond() {
        if (!this.mUpdatesEnabled) {
            return;
        }

        if (mRegisteredReceiver != null) {
            runReceiver();
        }
        // Trigger updates.
        scheduleNextSecond();
    }

    /** Call {@link Callable#call()} on the registered receiver and handles exception. */
    private void runReceiver() {
        if (mRegisteredReceiver == null) {
            return;
        }

        ListenableFuture<Void> result;
        result = mRegisteredReceiver.get();
        result.addListener(() -> {
            try {
                result.get();
            } catch (ExecutionException | InterruptedException | CancellationException e) {
                Log.e(TAG, "There was an error while running receiver.", e);
            }
        }, mExecutor);

    }

    private void scheduleNextSecond() {
        // Set up for the next update.
        mLastScheduleTimeMillis += 1000;

        // Ensure that the new time is actually in the future. If a call from uiHandler gets
        // significantly delayed for any reason, then without this, we'll reschedule immediately
        // (potentially multiple times), compounding the situation further.
        if (mLastScheduleTimeMillis < SystemClock.uptimeMillis()) {
            // Skip the failed updates...
            long missedTime = SystemClock.uptimeMillis() - mLastScheduleTimeMillis;

            // Round up to the nearest second...
            missedTime = ((missedTime / 1000) + 1) * 1000;
            mLastScheduleTimeMillis += missedTime;
        }

        mUiHandler.postAtTime(this.mNotifyAndSchedule, this, mLastScheduleTimeMillis);
    }
}
