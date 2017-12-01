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

import android.arch.background.workmanager.constraints.ConstraintListener;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A base for tracking constraints and notifying listeners of changes.
 *
 * @param <T> the constraint data type observed by this tracker
 */

public abstract class ConstraintTracker<T> {

    private static final String TAG = "ConstraintTracker";

    protected Context mAppContext;
    protected Set<ConstraintListener<T>> mListeners = new LinkedHashSet<>();

    ConstraintTracker(Context context) {
        mAppContext = context.getApplicationContext();
    }

    /**
     * Add the given listener for tracking.
     *
     * @param listener The target listener to start notifying
     */
    public void addListener(ConstraintListener<T> listener) {
        if (mListeners.add(listener)) {
            if (mListeners.size() == 1) {
                initState();
                startTracking();
            }
            notifyListener(listener);
        }
    }

    /**
     * Remove the given listener from tracking.
     *
     * @param listener The listener to stop notifying.
     */
    public void removeListener(ConstraintListener<T> listener) {
        if (mListeners.remove(listener) && mListeners.isEmpty()) {
            stopTracking();
        }
    }

    /**
     * Determines and stores the initial state of the constraint being tracked.
     */
    protected abstract void initState();

    /**
     * Notifies the listener about the current state.
     *
     * @param listener The listener to notify
     */
    protected abstract void notifyListener(@NonNull ConstraintListener<T> listener);

    /**
     * Start tracking for constraint state changes.
     */
    public abstract void startTracking();

    /**
     * Stop tracking for constraint state changes.
     */
    public abstract void stopTracking();
}
