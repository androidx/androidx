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

import android.arch.background.workmanager.constraints.ConstraintsState;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * A {@link BroadcastReceiver} for battery okay or low broadcasts.
 */

public class BatteryNotLowTracker extends ConstraintTracker {

    private Boolean mIsBatteryNotLow;

    public BatteryNotLowTracker(Context context) {
        super(context);
    }

    @Override
    public void setUpInitialState(ConstraintsState state) {
        if (mIsBatteryNotLow == null) {
            Intent intent = mAppContext.registerReceiver(null, getIntentFilter());
            if (intent == null || intent.getAction() == null) {
                return;
            }

            switch (intent.getAction()) {
                case Intent.ACTION_BATTERY_OKAY:
                    mIsBatteryNotLow = true;
                    break;

                case Intent.ACTION_BATTERY_LOW:
                    mIsBatteryNotLow = false;
                    break;
            }
        }

        if (mIsBatteryNotLow != null) {
            state.setBatteryNotLow(mIsBatteryNotLow);
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
                setIsBatteryNotLowAndNotify(true);
                break;

            case Intent.ACTION_BATTERY_LOW:
                setIsBatteryNotLowAndNotify(false);
                break;
        }
    }

    private void setIsBatteryNotLowAndNotify(boolean isBatteryNotLow) {
        if (mIsBatteryNotLow != isBatteryNotLow) {
            mIsBatteryNotLow = isBatteryNotLow;
            for (ConstraintsState state : mConstraintsStateList) {
                state.setBatteryNotLow(mIsBatteryNotLow);
            }
        }
    }
}
