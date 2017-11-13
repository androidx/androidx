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
package android.arch.background.workmanager.constraints.controllers;

import android.arch.background.workmanager.constraints.listeners.ConstraintListener;
import android.arch.background.workmanager.constraints.trackers.ConstraintTracker;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;

/**
 * A controller for a particular constraint.
 *
 * @param <T> A specific type of {@link ConstraintListener} associated with this controller
 */

public abstract class ConstraintController<T extends ConstraintListener>
        implements LifecycleObserver {

    /**
     * A callback for when a constraint changes.
     */
    public interface OnConstraintUpdatedCallback {

        /**
         * Called when a constraint is met.
         *
         * @param workSpecIds The list of work ids that may have become eligible to run
         */
        void onConstraintMet(List<String> workSpecIds);

        /**
         * Called when a constraint is not met.
         *
         * @param workSpecIds The list of work ids that have become ineligible to run
         */
        void onConstraintNotMet(List<String> workSpecIds);
    }

    private static final String TAG = "ConstraintCtrlr";

    private LiveData<List<String>> mConstraintLiveData;
    private LifecycleOwner mLifecycleOwner;
    private ConstraintTracker<T> mTracker;
    private Observer<List<String>> mConstraintObserver;
    private OnConstraintUpdatedCallback mOnConstraintUpdatedCallback;
    private List<String> mMatchingWorkSpecIds;

    ConstraintController(
            LiveData<List<String>> constraintLiveData,
            LifecycleOwner lifecycleOwner,
            ConstraintTracker<T> tracker,
            OnConstraintUpdatedCallback onConstraintUpdatedCallback) {

        mConstraintLiveData = constraintLiveData;
        mLifecycleOwner = lifecycleOwner;
        mTracker = tracker;
        mOnConstraintUpdatedCallback = onConstraintUpdatedCallback;

        mConstraintObserver = new Observer<List<String>>() {
            @Override
            public void onChanged(@Nullable List<String> matchingWorkSpecIds) {
                Log.d(
                        TAG,
                        ConstraintController.this.getClass().getSimpleName() + ": "
                                + matchingWorkSpecIds);
                mMatchingWorkSpecIds = matchingWorkSpecIds;
                if (matchingWorkSpecIds != null && matchingWorkSpecIds.size() > 0) {
                    mTracker.addListener(getListener());
                    updateListener();
                } else {
                    mTracker.removeListener(getListener());
                }
            }
        };

        mLifecycleOwner.getLifecycle().addObserver(this);
    }

    /**
     * Registers the {@link Observer}.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onLifecycleStart() {
        mConstraintLiveData.observe(mLifecycleOwner, mConstraintObserver);
    }

    /**
     * Removes the {@link Observer} and stops tracking on the {@link ConstraintTracker}.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void shutdown() {
        mConstraintLiveData.removeObserver(mConstraintObserver);
        mTracker.removeListener(getListener());
    }

    /**
     * Determines if a particular {@link android.arch.background.workmanager.model.WorkSpec} is
     * considered constrained.  A constrained WorkSpec is one that is known to this controller and
     * the constraint for this controller is not met.  Note that if this controller has not yet
     * received a list of matching WorkSpec ids, *everything* is considered constrained.
     *
     * @param id The WorkSpec id
     * @return {@code true} if the WorkSpec is considered constrained
     */
    public boolean isWorkSpecConstrained(String id) {
        if (mMatchingWorkSpecIds == null) {
            Log.d(TAG, getClass().getSimpleName() + ": null matching workspecs");
            return true;
        }
        return isConstrained() && mMatchingWorkSpecIds.contains(id);
    }

    protected void updateListener() {
        if (mMatchingWorkSpecIds == null) {
            Log.d(TAG, getClass().getSimpleName() + ": updateListener - no workspecs!");
            return;
        }

        Log.d(TAG, getClass().getSimpleName() + ": updateListener");
        if (isConstrained()) {
            mOnConstraintUpdatedCallback.onConstraintNotMet(mMatchingWorkSpecIds);
        } else {
            mOnConstraintUpdatedCallback.onConstraintMet(mMatchingWorkSpecIds);
        }
    }

    abstract T getListener();

    abstract boolean isConstrained();
}
