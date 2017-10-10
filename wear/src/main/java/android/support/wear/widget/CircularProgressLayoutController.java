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

package android.support.wear.widget;

import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

/**
 * Controller for {@link CircularProgressLayout}.
 */
class CircularProgressLayoutController {

    private final CircularProgressLayout mLayout;
    @VisibleForTesting CountDownTimer mTimer;
    private boolean mIsIndeterminate;
    private boolean mIsTimerRunning;

    /**
     * Called when the timer is finished.
     */
    @Nullable
    private CircularProgressLayout.OnTimerFinishedListener mOnTimerFinishedListener;

    CircularProgressLayoutController(CircularProgressLayout layout) {
        mLayout = layout;
    }

    /**
     * Returns the registered {@link CircularProgressLayout.OnTimerFinishedListener}.
     */
    @Nullable
    public CircularProgressLayout.OnTimerFinishedListener getOnTimerFinishedListener() {
        return mOnTimerFinishedListener;
    }

    /**
     * Sets the {@link CircularProgressLayout.OnTimerFinishedListener} to be notified when timer
     * countdown is finished.
     */
    public void setOnTimerFinishedListener(
            @Nullable CircularProgressLayout.OnTimerFinishedListener listener) {
        mOnTimerFinishedListener = listener;
    }

    /** Returns true if the progress is shown as an indeterminate spinner. */
    boolean isIndeterminate() {
        return mIsIndeterminate;
    }

    /** Returns true if timer is running. */
    boolean isTimerRunning() {
        return mIsTimerRunning;
    }

    /** Sets if the progress should be shown as an indeterminate spinner. */
    void setIndeterminate(boolean indeterminate) {
        if (mIsIndeterminate == indeterminate) {
            return;
        }
        mIsIndeterminate = indeterminate;
        if (mIsIndeterminate) {
            if (mIsTimerRunning) {
                stopTimer();
            }
            mLayout.getProgressDrawable().start();
        } else {
            mLayout.getProgressDrawable().stop();
        }
    }

    void startTimer(long totalTime, long updateInterval) {
        reset();
        mIsTimerRunning = true;
        mTimer = new CircularProgressTimer(totalTime, updateInterval);
        mTimer.start();
    }

    void stopTimer() {
        if (mIsTimerRunning) {
            mTimer.cancel();
            mIsTimerRunning = false;
            mLayout.getProgressDrawable().setStartEndTrim(0f, 0f); // Reset the progress
        }
    }

    /**
     * Resets everything.
     */
    void reset() {
        setIndeterminate(false); // If showing indeterminate progress, stop it
        stopTimer(); // Stop the previous timer if there is one
        mLayout.getProgressDrawable().setStartEndTrim(0f, 0f); // Reset the progress
    }

    /**
     * Class to handle timing for {@link CircularProgressLayout}.
     */
    private class CircularProgressTimer extends CountDownTimer {

        private final long mTotalTime;

        CircularProgressTimer(long totalTime, long updateInterval) {
            super(totalTime, updateInterval);
            mTotalTime = totalTime;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mLayout.getProgressDrawable()
                    .setStartEndTrim(0f, 1f - (float) millisUntilFinished / (float) mTotalTime);
            mLayout.invalidate();
        }

        @Override
        public void onFinish() {
            mLayout.getProgressDrawable().setStartEndTrim(0f, 1f);
            if (mOnTimerFinishedListener != null) {
                mOnTimerFinishedListener.onTimerFinished(mLayout);
            }
            mIsTimerRunning = false;
        }
    }
}
