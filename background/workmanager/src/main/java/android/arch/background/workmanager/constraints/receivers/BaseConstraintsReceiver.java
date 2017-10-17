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
package android.arch.background.workmanager.constraints.receivers;

import android.arch.background.workmanager.constraints.ConstraintsState;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * A base {@link BroadcastReceiver} for monitoring constraints changes.
 */

public abstract class BaseConstraintsReceiver extends BroadcastReceiver {

    protected Context mAppContext;
    protected List<ConstraintsState> mConstraintsStateList = new ArrayList<>();

    public BaseConstraintsReceiver(Context context) {
        mAppContext = context.getApplicationContext();
    }

    /**
     * Start tracking the constraints specified by this receiver.
     *
     * @param constraintsState The target {@link ConstraintsState} to update
     */
    public void startTracking(ConstraintsState constraintsState) {
        mConstraintsStateList.add(constraintsState);
        setUpInitialState(constraintsState);

        if (mConstraintsStateList.size() == 1) {
            registerReceiver();
        }
    }

    /**
     * Stop tracking the constraints specified by this receiver for the given
     * {@link ConstraintsState}.
     *
     * @param constraintsState The {@link ConstraintsState} to unregister
     */
    public void stopTracking(ConstraintsState constraintsState) {
        if (mConstraintsStateList.remove(constraintsState) && mConstraintsStateList.isEmpty()) {
            unregisterReceiver();
        }
    }

    /**
     * Set up the given {@link ConstraintsState} with the initial state.
     *
     * @param constraintsState The {@link ConstraintsState} to update
     */
    public abstract void setUpInitialState(ConstraintsState constraintsState);

    /**
     * @return The {@link IntentFilter} associated with this receiver.
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
