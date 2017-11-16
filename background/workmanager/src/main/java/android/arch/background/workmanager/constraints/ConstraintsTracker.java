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
import android.arch.background.workmanager.constraints.controllers.NetworkStateAnyController;
import android.arch.background.workmanager.constraints.controllers.NetworkStateMeteredController;
import android.arch.background.workmanager.constraints.controllers.NetworkStateNotRoamingController;
import android.arch.background.workmanager.constraints.controllers.NetworkStateUnmeteredController;
import android.arch.background.workmanager.constraints.controllers.StorageNotLowController;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to track the current status of various constraints.
 */

public class ConstraintsTracker implements ConstraintController.OnConstraintUpdatedCallback {

    private static final String TAG = "ConstraintsTracker";

    private final ConstraintsMetCallback mCallback;
    private final ConstraintController[] mConstraintControllers;

    public ConstraintsTracker(
            Context context,
            LifecycleOwner lifecycleOwner,
            WorkDatabase workDatabase,
            ConstraintsMetCallback callback,
            boolean allowPeriodic) {
        Context appContext = context.getApplicationContext();
        mCallback = callback;
        mConstraintControllers = new ConstraintController[]{
                new BatteryChargingController(
                        appContext, workDatabase, lifecycleOwner, this, allowPeriodic),
                new BatteryNotLowController(
                        appContext, workDatabase, lifecycleOwner, this, allowPeriodic),
                new StorageNotLowController(
                        appContext, workDatabase, lifecycleOwner, this, allowPeriodic),
                new NetworkStateAnyController(
                        appContext, workDatabase, lifecycleOwner, this, allowPeriodic),
                new NetworkStateUnmeteredController(
                        appContext, workDatabase, lifecycleOwner, this, allowPeriodic),
                new NetworkStateNotRoamingController(
                        appContext, workDatabase, lifecycleOwner, this, allowPeriodic),
                new NetworkStateMeteredController(
                        appContext, workDatabase, lifecycleOwner, this, allowPeriodic)
        };
    }

    @VisibleForTesting
    ConstraintsTracker(
            ConstraintsMetCallback callback,
            ConstraintController[] constraintControllers) {
        mCallback = callback;
        mConstraintControllers = constraintControllers;
    }

    private boolean areAllConstraintsMet(WorkSpec workSpec) {
        for (ConstraintController constraintController : mConstraintControllers) {
            if (constraintController.isWorkSpecConstrained(workSpec)) {
                Log.d(TAG, "Work " + workSpec + " constrained by "
                        + constraintController.getClass().getSimpleName());
                return false;
            }
        }
        return true;
    }

    @Override
    public void onConstraintMet(List<WorkSpec> workSpecs) {
        List<WorkSpec> unconstrainedWorkSpecs = new ArrayList<>();
        for (WorkSpec workSpec : workSpecs) {
            if (areAllConstraintsMet(workSpec)) {
                Log.d(TAG, "Constraints met for " + workSpec);
                unconstrainedWorkSpecs.add(workSpec);
            }
        }
        mCallback.onAllConstraintsMet(unconstrainedWorkSpecs);
    }

    @Override
    public void onConstraintNotMet(List<WorkSpec> workSpecs) {
        mCallback.onAllConstraintsNotMet(workSpecs);
    }
}
