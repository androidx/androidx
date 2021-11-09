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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

/**
 * Tracks whether or not the device's battery level is low.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BatteryNotLowTracker extends BroadcastReceiverConstraintTracker<Boolean> {

    private static final String TAG = Logger.tagWithPrefix("BatteryNotLowTracker");

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/core/res/res/values/config.xml#986}
     */
    static final float BATTERY_LOW_THRESHOLD = 0.15f;

    /**
     * Create an instance of {@link BatteryNotLowTracker}.
     * @param context The application {@link Context}
     * @param taskExecutor The internal {@link TaskExecutor} being used by WorkManager.
     */
    public BatteryNotLowTracker(@NonNull Context context, @NonNull TaskExecutor taskExecutor) {
        super(context, taskExecutor);
    }

    /**
     * Based on BatteryService#shouldSendBatteryLowLocked(), but this ignores the previous plugged
     * state - cannot guarantee the last plugged state because this isn't always tracking.
     *
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/services/core/java/com/android/server/BatteryService.java#268}
     */
    @Override
    public Boolean getInitialState() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = mAppContext.registerReceiver(null, intentFilter);
        if (intent == null) {
            Logger.get().error(TAG, "getInitialState - null intent received");
            return null;
        }

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPercentage = level / (float) scale;

        // BATTERY_STATUS_UNKNOWN typically refers to devices without a battery.
        // So those kinds of devices must be allowed.
        return (status == BatteryManager.BATTERY_STATUS_UNKNOWN
                || batteryPercentage > BATTERY_LOW_THRESHOLD);
    }

    @Override
    public IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        return intentFilter;
    }

    @Override
    public void onBroadcastReceive(Context context, @NonNull Intent intent) {
        if (intent.getAction() == null) {
            return;
        }

        Logger.get().debug(TAG, "Received " + intent.getAction());

        switch (intent.getAction()) {
            case Intent.ACTION_BATTERY_OKAY:
                setState(true);
                break;

            case Intent.ACTION_BATTERY_LOW:
                setState(false);
                break;
        }
    }
}
