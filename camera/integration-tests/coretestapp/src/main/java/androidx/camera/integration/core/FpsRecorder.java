/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.core;

import androidx.annotation.IntRange;

final class FpsRecorder {
    private static final double NANOS_IN_SECOND = 1_000_000_000;
    private final long[] mTimestamps;
    private int mIndex = 0;
    private int mNumSamples = 0; // Number of samples used in calculation.

    /**
     * Creates an fps recorder that creates a running average of {@code bufferLength+1} samples.
     */
    FpsRecorder(@IntRange(from = 1) int bufferLength) {
        if (bufferLength < 1) {
            throw new IllegalArgumentException("Invalid buffer length. Buffer must contain at "
                    + "least 1 sample");
        }
        mTimestamps = new long[bufferLength];
    }

    /**
     * Records the latest timestamp and returns the latest fps value or NaN if not enough samples
     * have been recorded.
     */
    double recordTimestamp(long timestampNs) {
        // Find the duration between the oldest and newest timestamp
        int nextIndex = (mIndex + 1) % mTimestamps.length;
        long duration = timestampNs - mTimestamps[mIndex];
        mTimestamps[mIndex] = timestampNs;
        mIndex = nextIndex;
        // The discarded sample is used in the calculation, so we use a maximum of bufferLength +
        // 1 samples.
        mNumSamples = Math.min(mNumSamples + 1, mTimestamps.length + 1);

        if (mNumSamples == mTimestamps.length + 1) {
            return (NANOS_IN_SECOND * mTimestamps.length) / duration;
        }

        // Return NaN if we don't have enough samples
        return Double.NaN;
    }

    /**
     * Ignores all previously recorded timestamps and calculates timestamps from new recorded
     * timestamps.
     *
     * <p>{@link #recordTimestamp(long)} will return NaN until enough samples have been
     * recorded.
     */
    void reset() {
        mNumSamples = 0;
        mIndex = 0;
    }
}
