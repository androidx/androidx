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
package androidx.work.impl.constraints.controllers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import androidx.work.impl.constraints.ConstraintListener;
import androidx.work.impl.constraints.trackers.ConstraintTracker;
import androidx.work.impl.model.WorkSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * A controller for a particular constraint.
 *
 * @param <T> the constraint data type managed by this controller.
 */

public abstract class ConstraintController<T> implements ConstraintListener<T> {

    /**
     * A callback for when a constraint changes.
     */
    public interface OnConstraintUpdatedCallback {

        /**
         * Called when a constraint is met.
         *
         * @param workSpecIds A list of {@link WorkSpec} IDs that may have become eligible to run
         */
        void onConstraintMet(@NonNull List<String> workSpecIds);

        /**
         * Called when a constraint is not met.
         *
         * @param workSpecIds A list of {@link WorkSpec} IDs that have become ineligible to run
         */
        void onConstraintNotMet(@NonNull List<String> workSpecIds);
    }

    private final List<String> mMatchingWorkSpecIds = new ArrayList<>();

    private T mCurrentValue;
    private ConstraintTracker<T> mTracker;
    private OnConstraintUpdatedCallback mCallback;

    ConstraintController(ConstraintTracker<T> tracker, OnConstraintUpdatedCallback callback) {
        mTracker = tracker;
        mCallback = callback;
    }

    abstract boolean hasConstraint(@NonNull WorkSpec workSpec);

    abstract boolean isConstrained(@NonNull T currentValue);

    /**
     * Replaces the list of {@link WorkSpec}s to monitor constraints for.
     *
     * @param workSpecs A list of {@link WorkSpec}s to monitor constraints for
     */
    public void replace(@NonNull List<WorkSpec> workSpecs) {
        mMatchingWorkSpecIds.clear();

        for (WorkSpec workSpec : workSpecs) {
            if (hasConstraint(workSpec)) {
                mMatchingWorkSpecIds.add(workSpec.id);
            }
        }

        if (mMatchingWorkSpecIds.isEmpty()) {
            mTracker.removeListener(this);
        } else {
            mTracker.addListener(this);
        }
        updateCallback();
    }

    /**
     * Clears all tracked {@link WorkSpec}s.
     */
    public void reset() {
        if (!mMatchingWorkSpecIds.isEmpty()) {
            mMatchingWorkSpecIds.clear();
            mTracker.removeListener(this);
        }
    }

    /**
     * Determines if a particular {@link WorkSpec} is constrained. It is constrained if it is
     * tracked by this controller, and the controller constraint was set, but not satisfied.
     *
     * @param workSpecId The ID of the {@link WorkSpec} to check if it is constrained.
     * @return {@code true} if the {@link WorkSpec} is considered constrained
     */
    public boolean isWorkSpecConstrained(@NonNull String workSpecId) {
        return mCurrentValue != null && isConstrained(mCurrentValue)
                && mMatchingWorkSpecIds.contains(workSpecId);
    }

    private void updateCallback() {
        if (mMatchingWorkSpecIds.isEmpty()) {
            return;
        }

        if (mCurrentValue == null || isConstrained(mCurrentValue)) {
            mCallback.onConstraintNotMet(mMatchingWorkSpecIds);
        } else {
            mCallback.onConstraintMet(mMatchingWorkSpecIds);
        }
    }

    @Override
    public void onConstraintChanged(@Nullable T newValue) {
        mCurrentValue = newValue;
        updateCallback();
    }
}
