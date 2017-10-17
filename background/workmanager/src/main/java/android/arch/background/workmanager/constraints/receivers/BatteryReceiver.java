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
package android.arch.background.workmanager.constraints.receivers;

import android.arch.background.workmanager.constraints.ConstraintsState;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

/**
 * A {@link BroadcastReceiver} for battery level and power.
 */

public class BatteryReceiver extends BaseConstraintsReceiver {


    public BatteryReceiver(Context context) {
        super(context);
    }

    @Override
    public IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        if (Build.VERSION.SDK_INT >= 23) {
            intentFilter.addAction(BatteryManager.ACTION_CHARGING);
            intentFilter.addAction(BatteryManager.ACTION_DISCHARGING);
        } else {
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        }
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        return intentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        switch (intent.getAction()) {
            case BatteryManager.ACTION_CHARGING:
                for (ConstraintsState state : mConstraintsStateList) {
                    state.setCharging(true);
                }
                break;

            case BatteryManager.ACTION_DISCHARGING:
                for (ConstraintsState state : mConstraintsStateList) {
                    state.setCharging(false);
                }
                break;

            case Intent.ACTION_BATTERY_CHANGED: {
                boolean charging = isBatteryChangedIntentCharging(intent);
                for (ConstraintsState state : mConstraintsStateList) {
                    state.setCharging(charging);
                }
                break;
            }

            case Intent.ACTION_BATTERY_OKAY:
                for (ConstraintsState state : mConstraintsStateList) {
                    state.setBatteryNotLow(true);
                }
                break;

            case Intent.ACTION_BATTERY_LOW:
                for (ConstraintsState state : mConstraintsStateList) {
                    state.setBatteryNotLow(false);
                }
                break;
        }
    }

    @Override
    public void setUpInitialState(ConstraintsState state) {
        state.startPerformingBatchUpdates();

        {
            // {@link ACTION_CHARGING} {@link ACTION_DISCHARGING} are not sticky broadcasts, so we
            // use {@link ACTION_BATTERY_CHANGED} on all APIs to get the initial state.
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);

            Intent intent = mAppContext.registerReceiver(null, intentFilter);
            state.setCharging(isBatteryChangedIntentCharging(intent));
        }

        {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
            intentFilter.addAction(Intent.ACTION_BATTERY_LOW);

            Intent intent = mAppContext.registerReceiver(null, intentFilter);
            switch (intent.getAction()) {
                case Intent.ACTION_BATTERY_OKAY:
                    state.setBatteryNotLow(true);
                    break;

                case Intent.ACTION_BATTERY_LOW:
                    state.setBatteryNotLow(false);
                    break;
            }
        }

        state.stopPerformingBatchUpdates();
    }

    private boolean isBatteryChangedIntentCharging(Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging;
        if (Build.VERSION.SDK_INT >= 23) {
            charging = (status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL);
        } else {
            int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            charging = (chargePlug != 0);
        }
        return charging;
    }
}
