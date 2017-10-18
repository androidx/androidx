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

import android.content.Context;

/**
 * A singleton class that holds all the {@link ConstraintTracker}s and can track when to
 * register and unregister them.
 */

public class Trackers {

    private static Trackers sInstance;

    /**
     * Gets the singleton instance of {@link Trackers}.
     *
     * @param context The initializing context (we only use the application context)
     * @return The singleton instance of {@link Trackers}.
     */
    public static synchronized Trackers getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Trackers(context);
        }
        return sInstance;
    }

    private BatteryChargingTracker mBatteryChargingReceiver;
    private BatteryNotLowTracker mBatteryNotLowReceiver;
    private StorageNotLowTracker mStorageNotLowTracker;

    private Trackers(Context context) {
        Context appContext = context.getApplicationContext();
        mBatteryChargingReceiver = new BatteryChargingTracker(appContext);
        mBatteryNotLowReceiver = new BatteryNotLowTracker(appContext);
        mStorageNotLowTracker = new StorageNotLowTracker(appContext);
    }

    /**
     * Gets the receiver used to track the battery charging status.
     *
     * @return The receiver used to track battery charging status
     */
    public BatteryChargingTracker getBatteryChargingReceiver() {
        return mBatteryChargingReceiver;
    }

    /**
     * Gets the receiver used to track if the battery is okay or low.
     *
     * @return The receiver used to track if the battery is okay or low
     */
    public BatteryNotLowTracker getBatteryNotLowReceiver() {
        return mBatteryNotLowReceiver;
    }

    /**
     * Gets the receiver used to track if device storage is okay or low.
     *
     * @return The receiver used to track if device storage is okay or low.
     */
    public StorageNotLowTracker getStorageNotLowTracker() {
        return mStorageNotLowTracker;
    }
}
