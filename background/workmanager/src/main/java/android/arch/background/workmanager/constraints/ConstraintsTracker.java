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

import android.content.Context;

/**
 * A class to track the current status of various constraints.
 */

public class ConstraintsTracker {

    private Context mAppContext;
    private final ConstraintsState mConstraintsState = new ConstraintsState();

    private BatteryReceiver mBatteryReceiver;

    public ConstraintsTracker(Context context) {
        mAppContext = context.getApplicationContext();
        mBatteryReceiver = new BatteryReceiver(mAppContext, this);
    }

    /**
     * @param charging {@code true} if the device is charging
     */
    public void setIsCharging(boolean charging) {
        synchronized (mConstraintsState) {
            mConstraintsState.mIsCharging = charging;
        }
    }

    /**
     * @param batteryNotLow {@code true} if the device battery is not considered low
     */
    public void setIsBatteryNotLow(boolean batteryNotLow) {
        synchronized (mConstraintsState) {
            mConstraintsState.mIsBatteryNotLow = batteryNotLow;
        }
    }
}
