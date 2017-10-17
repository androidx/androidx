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
package android.arch.background.workmanager.constraints;

import android.util.Log;

/**
 * An object specifying the current state of system constraints.
 */

public class ConstraintsState {

    private static final String TAG = "ConstraintsState";

    private Listener mListener;

    private boolean mPerformingInitialUpdates;
    private boolean mShouldNotifyAfterInitialUpdates;

    private Boolean mIsCharging;
    private Boolean mIsBatteryNotLow;

    public ConstraintsState(Listener listener) {
        mListener = listener;
    }

    /**
     * Sets the charging status of this {@link ConstraintsState}.
     *
     * @param charging {@code true} if the device is charging
     */
    public void setCharging(boolean charging) {
        if (mIsCharging != charging) {
            mIsCharging = charging;
            tryNotifyListener();
        }
    }

    public boolean isCharging() {
        return mIsCharging;
    }

    /**
     * Sets the battery status of this {@link ConstraintsState}.
     *
     * @param batteryNotLow {@code true} if the battery is not low
     */
    public void setBatteryNotLow(boolean batteryNotLow) {
        if (mIsBatteryNotLow != batteryNotLow) {
            mIsBatteryNotLow = batteryNotLow;
            tryNotifyListener();
        }
    }

    public boolean isBatteryNotLow() {
        return mIsBatteryNotLow;
    }

    /**
     * Signals this object should start performing batch updates.
     *
     * During batch updates, no listeners are notified until the end.
     */
    public void startPerformingBatchUpdates() {
        if (mPerformingInitialUpdates) {
            Log.d(TAG, "Already performing batch updates");
            return;
        }

        mPerformingInitialUpdates = true;
        mShouldNotifyAfterInitialUpdates = false;
    }

    /**
     * Signals this object should stop performing batch updates.
     *
     * If any changes were made during the update, listeners are notified at the end of this method.
     */
    public void stopPerformingBatchUpdates() {
        if (!mPerformingInitialUpdates) {
            Log.d(TAG, "Already not performing batch updates");
            return;
        }

        mPerformingInitialUpdates = false;
        if (mShouldNotifyAfterInitialUpdates) {
            tryNotifyListener();
        }
    }

    void tryNotifyListener() {
        if (mPerformingInitialUpdates) {
            mShouldNotifyAfterInitialUpdates = true;
        } else {
            mListener.onConstraintsUpdated(this);
        }
    }

    /**
     * A listener that is invoked as each constraint is updated.
     */
    public interface Listener {

        /**
         * @param constraintsState The {@link ConstraintsState} that was updated
         */
        void onConstraintsUpdated(ConstraintsState constraintsState);
    }
}
