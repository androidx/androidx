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
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

/**
 * A singleton class to hold an instance of each {@link ConstraintTracker}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

    /**
     * Sets an instance of the {@link Trackers} for testing.
     */
    @VisibleForTesting
    public static synchronized void setInstance(@NonNull Trackers trackers) {
        sInstance = trackers;
    }

    private BatteryChargingTracker mBatteryChargingTracker;
    private BatteryNotLowTracker mBatteryNotLowTracker;
    private NetworkStateTracker mNetworkStateTracker;
    private StorageNotLowTracker mStorageNotLowTracker;

    private Trackers(Context context) {
        Context appContext = context.getApplicationContext();
        mBatteryChargingTracker = new BatteryChargingTracker(appContext);
        mBatteryNotLowTracker = new BatteryNotLowTracker(appContext);
        mNetworkStateTracker = new NetworkStateTracker(appContext);
        mStorageNotLowTracker = new StorageNotLowTracker(appContext);
    }

    /**
     * Gets the tracker used to track the battery charging status.
     *
     * @return The tracker used to track battery charging status
     */
    public BatteryChargingTracker getBatteryChargingTracker() {
        return mBatteryChargingTracker;
    }

    /**
     * Gets the tracker used to track if the battery is okay or low.
     *
     * @return The tracker used to track if the battery is okay or low
     */
    public BatteryNotLowTracker getBatteryNotLowTracker() {
        return mBatteryNotLowTracker;
    }

    /**
     * Gets the tracker used to track network state changes.
     *
     * @return The tracker used to track state of the network
     */
    public NetworkStateTracker getNetworkStateTracker() {
        return mNetworkStateTracker;
    }

    /**
     * Gets the tracker used to track if device storage is okay or low.
     *
     * @return The tracker used to track if device storage is okay or low.
     */
    public StorageNotLowTracker getStorageNotLowTracker() {
        return mStorageNotLowTracker;
    }
}
