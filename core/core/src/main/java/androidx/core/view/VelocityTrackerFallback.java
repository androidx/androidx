/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.view;

import static androidx.core.view.MotionEventCompat.AXIS_SCROLL;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

/**
 * A fallback implementation of {@link android.view.VelocityTracker}. The methods its provide
 * mirror the platform's implementation.
 *
 * <p>It will be used to provide velocity tracking logic for certain axes that may not be
 * supported at different API levels, so that {@link VelocityTrackerCompat} can provide compat
 * service to its clients.
 *
 * <p>Currently, it supports AXIS_SCROLL with the default pointer ID.
 */
class VelocityTrackerFallback {
    private static final long RANGE_MS = 100L;
    private static final int HISTORY_SIZE = 20;
    /**
     * If there's no data beyond this period of time, we assume that the previous chain of motion
     * from the pointer has stopped, and we handle subsequent data points separately.
     */
    private static final long ASSUME_POINTER_STOPPED_MS = 40L;

    private final float[] mMovements = new float[HISTORY_SIZE];
    private final long[] mEventTimes = new long[HISTORY_SIZE];

    /** Cached value of the last computed velocity, for a O(1) get operation. */
    private float mLastComputedVelocity = 0f;

    /** Number of data points that are potential velocity calculation candidates. */
    private int mDataPointsBufferSize = 0;
    /**
     * The last index in the circular buffer where a data point was added. Irrelevant if {@code
     * dataPointsBufferSize} == 0.
     */
    private int mDataPointsBufferLastUsedIndex = 0;

    /** Adds a motion for velocity tracking. */
    void addMovement(@NonNull MotionEvent event) {
        long eventTime = event.getEventTime();
        if (mDataPointsBufferSize != 0
                && (eventTime - mEventTimes[mDataPointsBufferLastUsedIndex]
                > ASSUME_POINTER_STOPPED_MS)) {
            // There has been at least `ASSUME_POINTER_STOPPED_MS` since the last recorded event.
            // When this happens, consider that the pointer has stopped until this new event. Thus,
            // clear all past events.
            clear();
        }

        mDataPointsBufferLastUsedIndex = (mDataPointsBufferLastUsedIndex + 1) % HISTORY_SIZE;
        // We do not need to increase size if the size is already `HISTORY_SIZE`, since we always
        // will  have at most `HISTORY_SIZE` data points stored, due to the circular buffer.
        if (mDataPointsBufferSize != HISTORY_SIZE) {
            mDataPointsBufferSize += 1;
        }

        mMovements[mDataPointsBufferLastUsedIndex] = event.getAxisValue(AXIS_SCROLL);
        mEventTimes[mDataPointsBufferLastUsedIndex] = eventTime;
    }

    /** Same as {@link #computeCurrentVelocity} with {@link Float#MAX_VALUE} as the max velocity. */
    void computeCurrentVelocity(int units) {
        computeCurrentVelocity(units, Float.MAX_VALUE);
    }

    /** Computes the current velocity with the given unit and max velocity. */
    void computeCurrentVelocity(int units, float maxVelocity) {
        mLastComputedVelocity = getCurrentVelocity() * units;

        // Fix the velocity as per the max velocity
        // (i.e. clamp it between [-maxVelocity, maxVelocity])
        if (mLastComputedVelocity < -Math.abs(maxVelocity)) {
            mLastComputedVelocity = -Math.abs(maxVelocity);
        } else if (mLastComputedVelocity > Math.abs(maxVelocity)) {
            mLastComputedVelocity = Math.abs(maxVelocity);
        }
    }

    /** Returns the computed velocity for the given {@code axis}. */
    float getAxisVelocity(int axis) {
        if (axis != AXIS_SCROLL) {
            return 0;
        }
        return mLastComputedVelocity;
    }

    private void clear() {
        mDataPointsBufferSize = 0;
        mLastComputedVelocity = 0;
    }

    private float getCurrentVelocity() {
        // At least 2 data points needed to get Impulse velocity.
        if (mDataPointsBufferSize < 2) {
            return 0f;
        }

        // The first valid index that contains a data point that should be part of the velocity
        // calculation, as long as it's within `RANGE_MS` from the latest data point.
        int firstValidIndex =
                (mDataPointsBufferLastUsedIndex + HISTORY_SIZE - (mDataPointsBufferSize - 1))
                        % HISTORY_SIZE;
        long lastEventTime = mEventTimes[mDataPointsBufferLastUsedIndex];
        while (lastEventTime - mEventTimes[firstValidIndex] > RANGE_MS) {
            // Decrementing the size is equivalent to practically "removing" this data point.
            mDataPointsBufferSize--;
            // Increment the `firstValidIndex`, since we just found out that the current
            // `firstValidIndex` is not valid (not within `RANGE_MS`).
            firstValidIndex = (firstValidIndex + 1) % HISTORY_SIZE;
        }

        // At least 2 data points needed to get Impulse velocity.
        if (mDataPointsBufferSize < 2) {
            return 0;
        }

        if (mDataPointsBufferSize == 2) {
            int lastIndex = (firstValidIndex + 1) % HISTORY_SIZE;
            if (mEventTimes[firstValidIndex] == mEventTimes[lastIndex]) {
                return 0f;
            }
            return mMovements[lastIndex] / (mEventTimes[lastIndex] - mEventTimes[firstValidIndex]);
        }

        float work = 0;
        int numDataPointsProcessed = 0;
        // Loop from the `firstValidIndex`, to the "second to last" valid index. We need to go only
        // to the "second to last" element, since the body of the loop checks against the next data
        // point, so we cannot go all the way to the end.
        for (int i = 0; i < mDataPointsBufferSize - 1; i++) {
            int currentIndex = i + firstValidIndex;
            long eventTime = mEventTimes[currentIndex % HISTORY_SIZE];
            int nextIndex = (currentIndex + 1) % HISTORY_SIZE;

            // Duplicate timestamp. Skip this data point.
            if (mEventTimes[nextIndex] == eventTime) {
                continue;
            }

            numDataPointsProcessed++;
            float vPrev = kineticEnergyToVelocity(work);
            float delta = mMovements[nextIndex];
            float vCurr = delta / (mEventTimes[nextIndex] - eventTime);

            work += (vCurr - vPrev) * Math.abs(vCurr);

            // Note that we are intentionally checking against `numDataPointsProcessed`, instead of
            // just checking `i` against `firstValidIndex`. This is to cover cases where there are
            // multiple data points that have the same timestamp as the one at `firstValidIndex`.
            if (numDataPointsProcessed == 1) {
                work = work * 0.5f;
            }
        }

        return kineticEnergyToVelocity(work);
    }

    /** Based on the formula: Kinetic Energy = (0.5 * mass * velocity^2), with mass = 1. */
    private static float kineticEnergyToVelocity(float work) {
        return (work < 0 ? -1.0f : 1.0f) * (float) Math.sqrt(2f * Math.abs(work));
    }
}
