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

import android.arch.background.workmanager.WorkDatabase;
import android.arch.background.workmanager.constraints.controllers.ConstraintController;
import android.arch.background.workmanager.constraints.listeners.BatteryChargingListener;
import android.arch.background.workmanager.constraints.listeners.BatteryNotLowListener;
import android.arch.background.workmanager.constraints.listeners.StorageNotLowListener;
import android.arch.background.workmanager.constraints.trackers.Trackers;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;

/**
 * A class to track the current status of various constraints.
 */

public class ConstraintsTracker implements
        BatteryChargingListener,
        BatteryNotLowListener,
        StorageNotLowListener {

    private LifecycleOwner mLifecycleOwner;

    private ConstraintController mBatteryChargingController;
    private ConstraintController mBatteryNotLowController;
    private ConstraintController mStorageNotLowController;

    public ConstraintsTracker(
            Context context,
            LifecycleOwner lifecycleOwner,
            WorkDatabase workDatabase) {
        Context appContext = context.getApplicationContext();
        mLifecycleOwner = lifecycleOwner;

        Trackers trackers = Trackers.getInstance(appContext);

        mBatteryChargingController = new ConstraintController<>(
                workDatabase.workSpecDao().getEnqueuedWorkSpecIdsWithBatteryChargingConstraint(),
                mLifecycleOwner,
                trackers.getBatteryChargingReceiver(),
                this);

        mBatteryNotLowController = new ConstraintController<>(
                workDatabase.workSpecDao().getEnqueuedWorkSpecIdsWithBatteryNotLowConstraint(),
                mLifecycleOwner,
                trackers.getBatteryNotLowReceiver(),
                this);

        mStorageNotLowController = new ConstraintController<>(
                workDatabase.workSpecDao().getEnqueuedWorkSpecIdsWithStorageNotLowConstraint(),
                mLifecycleOwner,
                trackers.getStorageNotLowTracker(),
                this);
    }

    /**
     * Shuts down this {@link ConstraintsTracker} and removes all internal observation.
     */
    public void shutdown() {
        mBatteryChargingController.shutdown();
        mBatteryNotLowController.shutdown();
        mStorageNotLowController.shutdown();
    }

    @Override
    public void setBatteryNotLow(boolean isBatteryNotLow) {

    }

    @Override
    public void setStorageNotLow(boolean isStorageNotLow) {

    }

    @Override
    public void setBatteryCharging(boolean isBatteryCharging) {

    }
}
