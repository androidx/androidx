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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * A base {@link BroadcastReceiver} for monitoring constraints changes.
 *
 * @param <T> A specific type of {@link ConstraintListener} associated with this tracker
 */

public abstract class ConstraintTracker<T extends ConstraintListener> extends BroadcastReceiver {

    protected Context mAppContext;
    protected List<T> mListeners = new ArrayList<>();

    public ConstraintTracker(Context context) {
        mAppContext = context.getApplicationContext();
    }

    /**
     * Add the given listener for tracking.
     *
     * @param listener The target listener to register
     */
    public void addListener(T listener) {
        mListeners.add(listener);
        setUpInitialState(listener);

        if (mListeners.size() == 1) {
            registerReceiver();
        }
    }

    /**
     * Remove the given listener from tracking.
     *
     * @param listener The listener to unregister
     */
    public void removeListener(T listener) {
        if (mListeners.remove(listener) && mListeners.isEmpty()) {
            unregisterReceiver();
        }
    }

    /**
     * Set up the given listener with the initial state.
     *
     * @param listener The listener to update
     */
    public abstract void setUpInitialState(T listener);

    /**
     * @return The {@link IntentFilter} associated with this tracker.
     */
    public abstract IntentFilter getIntentFilter();

    /**
     * Registers this {@link BroadcastReceiver} with the application context.
     */
    public void registerReceiver() {
        mAppContext.registerReceiver(this, getIntentFilter());
    }

    /**
     * Unregisters this {@link BroadcastReceiver} with the application context.
     */
    public void unregisterReceiver() {
        mAppContext.unregisterReceiver(this);
    }
}
