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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.work.Constraints;
import androidx.work.Logger;
import androidx.work.impl.constraints.controllers.BatteryChargingController;
import androidx.work.impl.constraints.controllers.BatteryNotLowController;
import androidx.work.impl.constraints.controllers.ConstraintController;
import androidx.work.impl.constraints.controllers.NetworkConnectedController;
import androidx.work.impl.constraints.controllers.NetworkMeteredController;
import androidx.work.impl.constraints.controllers.NetworkNotRoamingController;
import androidx.work.impl.constraints.controllers.NetworkUnmeteredController;
import androidx.work.impl.constraints.controllers.StorageNotLowController;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks {@link WorkSpec}s and their {@link Constraints}, and notifies an optional
 * {@link WorkConstraintsCallback} when all of their constraints are met or not met.
 */

public class WorkConstraintsTracker implements ConstraintController.OnConstraintUpdatedCallback {

    private static final String TAG = Logger.tagWithPrefix("WorkConstraintsTracker");

    @Nullable private final WorkConstraintsCallback mCallback;
    private final ConstraintController<?>[] mConstraintControllers;

    // We need to keep hold a lock here for the cases where there is 1 WCT tracking a list of
    // WorkSpecs. Changes in constraints are notified on the main thread. Enqueues / Cancellations
    // occur on the task executor thread pool. So there is a chance of
    // ConcurrentModificationExceptions.
    private final Object mLock;

    /**
     * @param context      The application {@link Context}
     * @param taskExecutor The {@link TaskExecutor} being used by WorkManager.
     * @param callback     The callback is only necessary when you need
     *                     {@link WorkConstraintsTracker} to notify you about changes in
     *                     constraints for the list of {@link WorkSpec}'s that it is tracking.
     */
    public WorkConstraintsTracker(
            @NonNull Context context,
            @NonNull TaskExecutor taskExecutor,
            @Nullable WorkConstraintsCallback callback) {

        Context appContext = context.getApplicationContext();
        mCallback = callback;
        mConstraintControllers = new ConstraintController[] {
                new BatteryChargingController(appContext, taskExecutor),
                new BatteryNotLowController(appContext, taskExecutor),
                new StorageNotLowController(appContext, taskExecutor),
                new NetworkConnectedController(appContext, taskExecutor),
                new NetworkUnmeteredController(appContext, taskExecutor),
                new NetworkNotRoamingController(appContext, taskExecutor),
                new NetworkMeteredController(appContext, taskExecutor)
        };
        mLock = new Object();
    }

    @VisibleForTesting
    WorkConstraintsTracker(
            @Nullable WorkConstraintsCallback callback,
            ConstraintController<?>[] controllers) {

        mCallback = callback;
        mConstraintControllers = controllers;
        mLock = new Object();
    }

    /**
     * Replaces the list of tracked {@link WorkSpec}s to monitor if their constraints are met.
     *
     * @param workSpecs A list of {@link WorkSpec}s to monitor constraints for
     */
    @SuppressWarnings("unchecked")
    public void replace(@NonNull Iterable<WorkSpec> workSpecs) {
        synchronized (mLock) {
            for (ConstraintController<?> controller : mConstraintControllers) {
                controller.setCallback(null);
            }

            for (ConstraintController<?> controller : mConstraintControllers) {
                controller.replace(workSpecs);
            }

            for (ConstraintController<?> controller : mConstraintControllers) {
                controller.setCallback(this);
            }
        }
    }

    /**
     * Resets and clears all tracked {@link WorkSpec}s.
     */
    public void reset() {
        synchronized (mLock) {
            for (ConstraintController<?> controller : mConstraintControllers) {
                controller.reset();
            }
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
        synchronized (mLock) {
            for (ConstraintController<?> constraintController : mConstraintControllers) {
                if (constraintController.isWorkSpecConstrained(workSpecId)) {
                    Logger.get().debug(TAG, "Work " + workSpecId + "constrained by " + constraintController.getClass().getSimpleName());
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public void onConstraintMet(@NonNull List<String> workSpecIds) {
        synchronized (mLock) {
            List<String> unconstrainedWorkSpecIds = new ArrayList<>();
            for (String workSpecId : workSpecIds) {
                if (areAllConstraintsMet(workSpecId)) {
                    Logger.get().debug(TAG, "Constraints met for " + workSpecId);
                    unconstrainedWorkSpecIds.add(workSpecId);
                }
            }
            if (mCallback != null) {
                mCallback.onAllConstraintsMet(unconstrainedWorkSpecIds);
            }
        }
    }

    @Override
    public void onConstraintNotMet(@NonNull List<String> workSpecIds) {
        synchronized (mLock) {
            if (mCallback != null) {
                mCallback.onAllConstraintsNotMet(workSpecIds);
            }
        }
    }
}
