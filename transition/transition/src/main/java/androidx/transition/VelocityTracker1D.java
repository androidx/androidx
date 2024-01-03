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

package androidx.transition;

import java.util.Arrays;

/**
 * Velocity Tracker, simplified from compose's VelocityTracker1D.
 */
class VelocityTracker1D {
    private static final int HISTORY_SIZE = 20;
    private static final int ASSUME_POINTER_MOVE_STOPPED_MILLIS = 40;
    private static final int HORIZON_MILLIS = 100;

    // Circular buffer; current sample at index.
    private long[] mTimeSamples = new long[HISTORY_SIZE];
    private float[] mDataSamples = new float[HISTORY_SIZE];
    private int mIndex = 0;

    VelocityTracker1D() {
        Arrays.fill(mTimeSamples, Long.MIN_VALUE);
    }

    /**
     * Adds a data point for velocity calculation at a given time, {@code timeMillis}. The data
     * point represents an absolute position.
     * <p>
     * Use the same units for the data points provided. For example, having some data points in `cm`
     * and some in `m` will result in incorrect velocity calculations, as this method (and the
     * tracker) has no knowledge of the units used.
     */
    public void addDataPoint(long timeMillis, float data) {
        mIndex = (mIndex + 1) % HISTORY_SIZE;
        mTimeSamples[mIndex] = timeMillis;
        mDataSamples[mIndex] = data;
    }

    public void resetTracking() {
        mIndex = 0;
        Arrays.fill(mTimeSamples, Long.MIN_VALUE);
        Arrays.fill(mDataSamples, 0f);
    }

    /**
     * Computes the estimated velocity at the time of the last provided data point. The units of
     * velocity will be `units/second`, where `units` is the units of the data points provided via
     * [addDataPoint].
     *
     * This can be expensive. Only call this when you need the velocity.
     */
    float calculateVelocity() {
        int sampleCount = 0;
        int index = mIndex;

        if (index == 0 && mTimeSamples[index] == Long.MIN_VALUE) {
            return 0f; // We haven't received any data
        }

        // The sample at index is our newest sample.  If it is null, we have no samples so return.
        long newestTime = mTimeSamples[index];

        long previousTime = newestTime;

        // Starting with the most recent sample, iterate backwards while
        // the samples represent continuous motion.
        do {
            long sampleTime = mTimeSamples[index];
            if (sampleTime == Long.MIN_VALUE) {
                break; // no point here
            }
            float age = newestTime - sampleTime;
            float delta = Math.abs(sampleTime - previousTime);
            previousTime = sampleTime;

            if (age > HORIZON_MILLIS || delta > ASSUME_POINTER_MOVE_STOPPED_MILLIS) {
                break;
            }

            index = (index == 0 ? HISTORY_SIZE : index) - 1;
            sampleCount++;
        } while (sampleCount < HISTORY_SIZE);

        if (sampleCount < 2) {
            return 0f; // Not enough data to have a velocity
        }

        if (sampleCount == 2) {
            // Simple diff in time
            int prevIndex = mIndex == 0 ? HISTORY_SIZE - 1 : mIndex - 1;
            float timeDiff = mTimeSamples[mIndex] - mTimeSamples[prevIndex];
            if (timeDiff == 0f) {
                return 0f;
            }
            float dataDiff = mDataSamples[mIndex] - mDataSamples[prevIndex];
            return dataDiff / timeDiff * 1000;
        }

        float work = 0f;
        int startIndex = (mIndex - sampleCount + HISTORY_SIZE + 1) % HISTORY_SIZE;
        int endIndex = (mIndex + 1 + HISTORY_SIZE) % HISTORY_SIZE;
        previousTime = mTimeSamples[startIndex];
        float previousData = mDataSamples[startIndex];
        for (int i = (startIndex + 1) % HISTORY_SIZE; i != endIndex; i = (i + 1) % HISTORY_SIZE) {
            long time = mTimeSamples[i];
            long timeDelta = time - previousTime;
            if (timeDelta == 0f) {
                continue;
            }
            float data = mDataSamples[i];
            float vPrev = kineticEnergyToVelocity(work);
            float dataPointsDelta = data - previousData;

            float vCurr = dataPointsDelta / timeDelta;
            work += (vCurr - vPrev) * Math.abs(vCurr);
            if (i == startIndex + 1) {
                work = (work * 0.5f);
            }
            previousTime = time;
            previousData = data;
        }
        return kineticEnergyToVelocity(work) * 1000;
    }

    private float kineticEnergyToVelocity(float kineticEnergy) {
        return (float) (Math.signum(kineticEnergy) * Math.sqrt(2 * Math.abs(kineticEnergy)));
    }
}
