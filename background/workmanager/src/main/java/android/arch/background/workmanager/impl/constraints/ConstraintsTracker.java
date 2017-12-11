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
package android.arch.background.workmanager.impl.constraints;

import android.arch.background.workmanager.impl.WorkDatabase;
import android.arch.background.workmanager.impl.constraints.controllers.BatteryChargingController;
import android.arch.background.workmanager.impl.constraints.controllers.BatteryNotLowController;
import android.arch.background.workmanager.impl.constraints.controllers.ConstraintController;
import android.arch.background.workmanager.impl.constraints.controllers.NetworkConnectedController;
import android.arch.background.workmanager.impl.constraints.controllers.NetworkMeteredController;
import android.arch.background.workmanager.impl.constraints.controllers.NetworkNotRoamingController;
import android.arch.background.workmanager.impl.constraints.controllers.NetworkUnmeteredController;
import android.arch.background.workmanager.impl.constraints.controllers.StorageNotLowController;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.impl.utils.LiveDataUtils;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to update the current status of various constraints.
 */

public class ConstraintsTracker implements LifecycleObserver, Observer<List<WorkSpec>>,
        ConstraintController.OnConstraintUpdatedCallback {

    private static final String TAG = "ConstraintsTracker";

    private final ConstraintsMetCallback mCallback;
    private final ConstraintController[] mConstraintControllers;

    public ConstraintsTracker(
            Context context,
            LifecycleOwner lifecycleOwner,
            WorkDatabase workDatabase,
            ConstraintsMetCallback callback) {
        Context appContext = context.getApplicationContext();
        mCallback = callback;
        mConstraintControllers = new ConstraintController[] {
                new BatteryChargingController(appContext, this),
                new BatteryNotLowController(appContext, this),
                new StorageNotLowController(appContext, this),
                new NetworkConnectedController(appContext, this),
                new NetworkUnmeteredController(appContext, this),
                new NetworkNotRoamingController(appContext, this),
                new NetworkMeteredController(appContext, this)
        };

        // TODO(janclarin): Move to ForegroundProcessor/SystemAlarmService.
        LiveData<List<WorkSpec>> workSpecLiveData = LiveDataUtils.dedupedLiveDataFor(
                workDatabase.workSpecDao().getConstraintsTrackerEligibleWorkSpecs());

        workSpecLiveData.observe(lifecycleOwner, this);
        lifecycleOwner.getLifecycle().addObserver(this);
    }

    @VisibleForTesting
    ConstraintsTracker(ConstraintsMetCallback callback, ConstraintController[] controllers) {
        mCallback = callback;
        mConstraintControllers = controllers;
    }

    /**
     * Replaces the list of tracked {@link WorkSpec}s to monitor if their constraints are met.
     *
     * @param workSpecs A list of {@link WorkSpec}s to monitor constraints for
     */
    public void replace(@NonNull List<WorkSpec> workSpecs) {
        for (ConstraintController controller : mConstraintControllers) {
            controller.replace(workSpecs);
        }
    }

    /**
     * Resets and clears all tracked {@link WorkSpec}s.
     */
    public void reset() {
        for (ConstraintController controller : mConstraintControllers) {
            controller.reset();
        }
    }

    @Override
    public void onChanged(@Nullable List<WorkSpec> workSpecs) {
        // TODO(janclarin): Remove when LiveData moved out of ConstraintsTracker.
        if (workSpecs != null) {
            replace(workSpecs);
        } else {
            reset();
        }
    }

    // TODO(janclarin): Remove when LiveData moved out.
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void onLifecycleStop() {
        reset();
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
