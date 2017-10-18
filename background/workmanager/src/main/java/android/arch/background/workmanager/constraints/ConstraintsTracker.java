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
import android.arch.background.workmanager.constraints.controllers.BatteryChargingController;
import android.arch.background.workmanager.constraints.controllers.BatteryNotLowController;
import android.arch.background.workmanager.constraints.controllers.ConstraintController;
import android.arch.background.workmanager.constraints.controllers.StorageNotLowController;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to track the current status of various constraints.
 */

public class ConstraintsTracker implements ConstraintController.OnConstraintUpdatedListener {

    private LifecycleOwner mLifecycleOwner;

    private ConstraintController mBatteryChargingController;
    private ConstraintController mBatteryNotLowController;
    private ConstraintController mStorageNotLowController;

    private List<ConstraintController> mConstraintControllers = new ArrayList<>();

    public ConstraintsTracker(
            Context context,
            LifecycleOwner lifecycleOwner,
            WorkDatabase workDatabase) {
        Context appContext = context.getApplicationContext();
        mLifecycleOwner = lifecycleOwner;

        mConstraintControllers.add(
                new BatteryChargingController(
                        appContext,
                        workDatabase,
                        mLifecycleOwner,
                        this));

        mConstraintControllers.add(
                new BatteryNotLowController(
                        appContext,
                        workDatabase,
                        mLifecycleOwner,
                        this));

        mConstraintControllers.add(
                new StorageNotLowController(
                        appContext,
                        workDatabase,
                        mLifecycleOwner,
                        this));
    }

    /**
     * Shuts down this {@link ConstraintsTracker} and removes all internal observation.
     */
    public void shutdown() {
        for (ConstraintController constraintController : mConstraintControllers) {
            constraintController.shutdown();
        }
    }

    @Override
    public void onConstraintMet(List<String> workSpecIds) {
        for (String id : workSpecIds) {
            boolean workSpecIdConstrained = false;
            for (ConstraintController constraintController : mConstraintControllers) {
                if (constraintController.isWorkSpecConstrained(id)) {
                    workSpecIdConstrained = true;
                    break;
                }
            }

            if (!workSpecIdConstrained) {
                // TODO(sumir): signal this should be processed.
            }
        }
    }

    @Override
    public void onConstraintNotMet(List<String> workSpecIds) {
        for (String id : workSpecIds) {
            // TODO(sumir): signal this should be cancelled.
        }
    }
}
