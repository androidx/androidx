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

/**
 * A {@link BroadcastReceiver} for battery okay or low broadcasts.
 */

public class BatteryNotLowReceiver extends BaseConstraintsReceiver {

    public BatteryNotLowReceiver(Context context) {
        super(context);
    }

    @Override
    public void setUpInitialState(ConstraintsState constraintsState) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);

        Intent intent = mAppContext.registerReceiver(null, intentFilter);
        switch (intent.getAction()) {
            case Intent.ACTION_BATTERY_OKAY:
                constraintsState.setBatteryNotLow(true);
                break;

            case Intent.ACTION_BATTERY_LOW:
                constraintsState.setBatteryNotLow(false);
                break;
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
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        switch (intent.getAction()) {
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
}
