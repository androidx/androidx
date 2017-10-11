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
package android.arch.background.workmanager.constraints;

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

    public BatteryReceiver(Context context, ConstraintsTracker constraintsTracker) {
        super(context, constraintsTracker);
    }

    @Override
    public void startTracking() {
        setupInitialState();

        IntentFilter intentFilter = new IntentFilter();
        if (Build.VERSION.SDK_INT >= 23) {
            intentFilter.addAction(BatteryManager.ACTION_CHARGING);
            intentFilter.addAction(BatteryManager.ACTION_DISCHARGING);
        } else {
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        }
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        mAppContext.registerReceiver(this, intentFilter);
    }

    @Override
    public void stopTracking() {
        mAppContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        switch (intent.getAction()) {
            case BatteryManager.ACTION_CHARGING:
                mConstraintsTracker.setIsCharging(true);
                break;

            case BatteryManager.ACTION_DISCHARGING:
                mConstraintsTracker.setIsCharging(false);
                break;

            case Intent.ACTION_BATTERY_CHANGED: {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean charging;
                if (Build.VERSION.SDK_INT >= 23) {
                    charging = (status == BatteryManager.BATTERY_STATUS_CHARGING
                            || status == BatteryManager.BATTERY_STATUS_FULL);
                } else {
                    int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    charging = (chargePlug != 0);
                }
                mConstraintsTracker.setIsCharging(charging);
                break;
            }

            case Intent.ACTION_BATTERY_OKAY:
                mConstraintsTracker.setIsBatteryNotLow(true);
                break;

            case Intent.ACTION_BATTERY_LOW:
                mConstraintsTracker.setIsBatteryNotLow(false);
                break;
        }
    }

    /**
     * Setup initial state.  Because we have multiple sticky actions on the final filter, we
     * do not know which Intent we will get back.  To get around this, we'll manually fire each
     * one to setup the initial state.
     */
    private void setupInitialState() {
        {
            // {@link ACTION_CHARGING} {@link ACTION_DISCHARGING} are not sticky broadcasts, so we
            // use {@link ACTION_BATTERY_CHANGED} on all APIs to get the initial state.
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            onReceive(mAppContext, mAppContext.registerReceiver(null, intentFilter));
        }

        {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
            intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
            onReceive(mAppContext, mAppContext.registerReceiver(null, intentFilter));
        }
    }
}
