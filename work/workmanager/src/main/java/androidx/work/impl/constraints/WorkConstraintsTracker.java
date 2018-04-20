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
package androidx.work.impl.constraints;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.impl.constraints.controllers.BatteryChargingController;
import androidx.work.impl.constraints.controllers.BatteryNotLowController;
import androidx.work.impl.constraints.controllers.ConstraintController;
import androidx.work.impl.constraints.controllers.NetworkConnectedController;
import androidx.work.impl.constraints.controllers.NetworkMeteredController;
import androidx.work.impl.constraints.controllers.NetworkNotRoamingController;
import androidx.work.impl.constraints.controllers.NetworkUnmeteredController;
import androidx.work.impl.constraints.controllers.StorageNotLowController;
import androidx.work.impl.model.WorkSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks {@link WorkSpec}s and their {@link Constraints}, and notifies an optional
 * {@link WorkConstraintsCallback} when all of their constraints are met or not met.
 */

public class WorkConstraintsTracker implements ConstraintController.OnConstraintUpdatedCallback {

    private static final String TAG = "WorkConstraintsTracker";

    @Nullable private final WorkConstraintsCallback mCallback;
    private final ConstraintController[] mConstraintControllers;

    /**
     * @param context  The application {@link Context}
     * @param callback The callback is only necessary when you need {@link WorkConstraintsTracker}
     *                 to notify you about changes in constraints for the list of  {@link
     *                 WorkSpec}'s that it is tracking.
     */
    public WorkConstraintsTracker(Context context, @Nullable WorkConstraintsCallback callback) {
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
    }

    @VisibleForTesting
    WorkConstraintsTracker(
            @Nullable WorkConstraintsCallback callback,
            ConstraintController[] controllers) {

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

    /**
     * Returns <code>true</code> if all the underlying constraints for a given WorkSpec are met.
     *
     * @param workSpecId The {@link WorkSpec} id
     * @return <code>true</code> if all the underlying constraints for a given {@link WorkSpec} are
     * met.
     */
    public boolean areAllConstraintsMet(@NonNull String workSpecId) {
        for (ConstraintController constraintController : mConstraintControllers) {
            if (constraintController.isWorkSpecConstrained(workSpecId)) {
                Log.d(TAG, String.format("Work %s constrained by %s", workSpecId,
                        constraintController.getClass().getSimpleName()));
                return false;
            }
        }
        return true;
    }

    @Override
    public void onConstraintMet(@NonNull List<String> workSpecIds) {
        List<String> unconstrainedWorkSpecIds = new ArrayList<>();
        for (String workSpecId : workSpecIds) {
            if (areAllConstraintsMet(workSpecId)) {
                Log.d(TAG, String.format("Constraints met for %s", workSpecId));
                unconstrainedWorkSpecIds.add(workSpecId);
            }
        }
        if (mCallback != null) {
            mCallback.onAllConstraintsMet(unconstrainedWorkSpecIds);
        }
    }

    @Override
    public void onConstraintNotMet(@NonNull List<String> workSpecIds) {
        if (mCallback != null) {
            mCallback.onAllConstraintsNotMet(workSpecIds);
        }
    }
}
