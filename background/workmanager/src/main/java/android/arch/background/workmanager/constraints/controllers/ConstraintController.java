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
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * A controller for a particular constraint.
 *
 * @param <T> A specific type of {@link ConstraintListener} associated with this controller
 */

public class ConstraintController<T extends ConstraintListener> {

    private LiveData<List<String>> mConstraintLiveData;
    private ConstraintTracker<T> mTracker;
    private T mListener;
    private Observer<List<String>> mConstraintObserver;

    public ConstraintController(
            LiveData<List<String>> constraintLiveData,
            LifecycleOwner lifecycleOwner,
            ConstraintTracker<T> tracker,
            T listener) {

        mConstraintLiveData = constraintLiveData;
        mTracker = tracker;
        mListener = listener;
        mConstraintObserver = new Observer<List<String>>() {
            @Override
            public void onChanged(@Nullable List<String> matchingWorkSpecIds) {
                if (matchingWorkSpecIds != null && matchingWorkSpecIds.size() > 0) {
                    mTracker.addListener(mListener);
                } else {
                    mTracker.removeListener(mListener);
                }
            }
        };

        mConstraintLiveData.observe(lifecycleOwner, mConstraintObserver);
    }

    /**
     * Removes the {@link Observer} and stops tracking on the {@link ConstraintTracker}.
     */
    public void shutdown() {
        mConstraintLiveData.removeObserver(mConstraintObserver);
        mTracker.removeListener(mListener);
    }
}
