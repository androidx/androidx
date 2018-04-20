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
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;

/**
 * Tracks whether or not the device's battery is charging.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BatteryChargingTracker extends BroadcastReceiverConstraintTracker<Boolean> {

    private static final String TAG = "BatteryChrgTracker";

    /**
     * Create an instance of {@link BatteryChargingTracker}.
     * @param context The application {@link Context}
     */
    public BatteryChargingTracker(Context context) {
        super(context);
    }

    @Override
    public Boolean getInitialState() {
        // {@link ACTION_CHARGING} and {@link ACTION_DISCHARGING} are not sticky broadcasts, so
        // we use {@link ACTION_BATTERY_CHANGED} on all APIs to get the initial state.
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = mAppContext.registerReceiver(null, intentFilter);
        if (intent == null) {
            Log.e(TAG, "getInitialState - null intent received");
            return null;
        }
        return isBatteryChangedIntentCharging(intent);
    }

    @Override
    public IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        if (Build.VERSION.SDK_INT >= 23) {
            intentFilter.addAction(BatteryManager.ACTION_CHARGING);
            intentFilter.addAction(BatteryManager.ACTION_DISCHARGING);
        } else {
            intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
            intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        }
        return intentFilter;
    }

    @Override
    public void onBroadcastReceive(Context context, @NonNull Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        Log.d(TAG, String.format("Received %s", action));
        switch (action) {
            case BatteryManager.ACTION_CHARGING:
                setState(true);
                break;

            case BatteryManager.ACTION_DISCHARGING:
                setState(false);
                break;

            case Intent.ACTION_POWER_CONNECTED:
                setState(true);
                break;

            case Intent.ACTION_POWER_DISCONNECTED:
                setState(false);
                break;
        }
    }

    private boolean isBatteryChangedIntentCharging(Intent intent) {
        boolean charging;
        if (Build.VERSION.SDK_INT >= 23) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            charging = (status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL);
        } else {
            int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            charging = (chargePlug != 0);
        }
        return charging;
    }
}
