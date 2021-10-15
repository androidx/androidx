/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.work.impl.constraints.trackers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

/**
 * A {@link ConstraintTracker} with a {@link BroadcastReceiver} for monitoring constraint changes.
 *
 * @param <T> the constraint data type observed by this tracker
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class BroadcastReceiverConstraintTracker<T> extends ConstraintTracker<T> {
    private static final String TAG = Logger.tagWithPrefix("BrdcstRcvrCnstrntTrckr");

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                onBroadcastReceive(context, intent);
            }
        }
    };

    public BroadcastReceiverConstraintTracker(
            @NonNull Context context,
            @NonNull TaskExecutor taskExecutor) {
        super(context, taskExecutor);
    }

    /**
     * Called when the {@link BroadcastReceiver} is receiving an {@link Intent} broadcast and should
     * handle the received {@link Intent}.
     *
     * @param context The {@link Context} in which the receiver is running.
     * @param intent  The {@link Intent} being received.
     */
    public abstract void onBroadcastReceive(Context context, @NonNull Intent intent);

    /**
     * @return The {@link IntentFilter} associated with this tracker.
     */
    public abstract IntentFilter getIntentFilter();

    @Override
    public void startTracking() {
        Logger.get().debug(TAG, getClass().getSimpleName() + ": registering receiver");
        mAppContext.registerReceiver(mBroadcastReceiver, getIntentFilter());
    }

    @Override
    public void stopTracking() {
        Logger.get().debug(
                TAG,
                getClass().getSimpleName() + ": unregistering receiver");
        mAppContext.unregisterReceiver(mBroadcastReceiver);
    }
}
