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
package android.arch.background.workmanager.constraints.trackers;

import android.arch.background.workmanager.constraints.listeners.ConstraintListener;
import android.content.Context;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A base for tracking constraints and notifying listeners of changes.
 *
 * @param <T> A specific type of {@link ConstraintListener} associated with this tracker
 */

public abstract class ConstraintTracker<T extends ConstraintListener> {

    private static final String TAG = "ConstraintTracker";

    protected Context mAppContext;
    protected Set<T> mListeners = new LinkedHashSet<>();

    ConstraintTracker(Context context) {
        mAppContext = context.getApplicationContext();
    }

    /**
     * Add the given listener for tracking.
     *
     * @param listener The target listener to startTracking
     */
    public void addListener(T listener) {
        if (mListeners.add(listener)) {
            setUpInitialState(listener);

            if (mListeners.size() == 1) {
                startTracking();
            }
        }
    }

    /**
     * Remove the given listener from tracking.
     *
     * @param listener The listener to stopTracking
     */
    public void removeListener(T listener) {
        if (mListeners.remove(listener) && mListeners.isEmpty()) {
            stopTracking();
        }
    }

    /**
     * Set up the given listener with the initial state.
     *
     * @param listener The listener to update
     */
    public abstract void setUpInitialState(T listener);

    /**
     * Start tracking for constraint state changes.
     */
    public abstract void startTracking();

    /**
     * Stop tracking for constraint state changes.
     */
    public abstract void stopTracking();
}
