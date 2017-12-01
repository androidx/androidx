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
package android.arch.background.workmanager.constraints.trackers;

import android.arch.background.workmanager.constraints.ConstraintListener;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

/**
 * Tracks whether or not the device's battery level is low.
 */

class BatteryNotLowTracker extends BroadcastReceiverConstraintTracker<Boolean> {

    private static final String TAG = "BatteryNotLowTracker";

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/services/core/java/com/android/server/BatteryService.java#111}
     */
    static final int BATTERY_PLUGGED_NONE = 0;

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/core/res/res/values/config.xml#986}
     */
    static final float BATTERY_LOW_PERCENTAGE = 0.15f;

    @VisibleForTesting
    Boolean mIsBatteryNotLow;

    BatteryNotLowTracker(Context context) {
        super(context);
    }

    /**
     * Based on BatteryService#shouldSendBatteryLowLocked(), but this ignores the previous plugged
     * state - cannot guarantee the last plugged state because this isn't always tracking.
     *
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/services/core/java/com/android/server/BatteryService.java#268}
     */
    @Override
    public void initState() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = mAppContext.registerReceiver(null, intentFilter);

        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, BATTERY_PLUGGED_NONE);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPercentage = level / (float) scale;

        mIsBatteryNotLow = plugged != BATTERY_PLUGGED_NONE
                || status == BatteryManager.BATTERY_STATUS_UNKNOWN
                || batteryPercentage > BATTERY_LOW_PERCENTAGE;

        Log.d(TAG, "Setting initial mIsBatteryNotLow to " + mIsBatteryNotLow);
    }

    @Override
    protected void notifyListener(@NonNull ConstraintListener<Boolean> listener) {
        if (mIsBatteryNotLow != null) {
            listener.onConstraintChanged(mIsBatteryNotLow);
        }
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

        Log.d(TAG, "Received " + intent.getAction());

        switch (intent.getAction()) {
            case Intent.ACTION_BATTERY_OKAY:
                setIsBatteryNotLowAndNotify(true);
                break;

            case Intent.ACTION_BATTERY_LOW:
                setIsBatteryNotLowAndNotify(false);
                break;
        }
    }

    private void setIsBatteryNotLowAndNotify(boolean isBatteryNotLow) {
        if (mIsBatteryNotLow == null || mIsBatteryNotLow != isBatteryNotLow) {
            Log.d(TAG, "Setting mIsBatteryNotLow to " + isBatteryNotLow);
            mIsBatteryNotLow = isBatteryNotLow;
            for (ConstraintListener<Boolean> listener : mListeners) {
                notifyListener(listener);
            }
        }
    }
}
