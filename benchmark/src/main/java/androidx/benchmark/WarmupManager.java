/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.benchmark;

import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * Used to detect when a benchmark has warmed up, given time taken for each iteration.
 *
 * Uses emperically determined constants, primarily looking for the convergence of two
 * exponential moving averages.
 *
 * Tuned to do minimal amount of intrusive work in onNextIteration to avoid polluting the benchmark.
 */
class WarmupManager {
    static final long MIN_DURATION_NS = TimeUnit.MILLISECONDS.toNanos(250);
    static final long MAX_DURATION_NS = TimeUnit.SECONDS.toNanos(8);
    static final int MIN_ITERATIONS = 30;
    private static final int MIN_SIMILAR_ITERATIONS = 40;

    private static final float FAST_RATIO = 0.1f;
    private static final float SLOW_RATIO = 0.005f;
    private static final float THRESHOLD = 0.04f;

    private float mFastMovingAvg;
    private float mSlowMovingAvg;
    private float mSimilarIterationCount;

    private int mIteration = 0;

    private long mTotalDuration;

    /**
     * Pass the just-run iteration timing, and return whether the warmup has completed.
     * <p>
     * NOTE: it is critical to do a minimum amount of work and memory access in this method, to
     * avoid polluting the benchmark's memory access patterns. This is why we chose exponential
     * moving averages, and why we only log once at the end.
     *
     * @param durationNs Duration of the next iteration.
     * @return True if the warmup has completed, false otherwise.
     */
    public boolean onNextIteration(long durationNs) {
        mIteration++;
        mTotalDuration += durationNs;

        if (mIteration == 1) {
            mFastMovingAvg = durationNs;
            mSlowMovingAvg = durationNs;
            return false;
        }

        mFastMovingAvg = FAST_RATIO * durationNs + (1 - FAST_RATIO) * mFastMovingAvg;
        mSlowMovingAvg = SLOW_RATIO * durationNs + (1 - SLOW_RATIO) * mSlowMovingAvg;

        // If fast moving avg is close to slow, the benchmark is stabilizing
        float ratio = mFastMovingAvg / mSlowMovingAvg;
        if (ratio < 1 + THRESHOLD && ratio > 1 - THRESHOLD) {
            mSimilarIterationCount++;
        } else {
            mSimilarIterationCount = 0;
        }

        if (mIteration >= MIN_ITERATIONS && mTotalDuration >= MIN_DURATION_NS) {
            if (mSimilarIterationCount > MIN_SIMILAR_ITERATIONS
                    || mTotalDuration >= MAX_DURATION_NS) {
                // benchmark has stabilized, or we're out of time
                Log.d("WarmupManager", String.format(
                        "Complete: t=%.3f, iter=%d, fastAvg=%3.0f, slowAvg=%3.0f",
                        mTotalDuration / 1000000000.0,
                        mIteration,
                        mFastMovingAvg,
                        mSlowMovingAvg));
                return true;
            }
        }
        return false;
    }

    float getEstimatedIterationTime() {
        return mFastMovingAvg;
    }

    public int getIteration() {
        return mIteration;
    }

    public long getTotalDuration() {
        return mTotalDuration;
    }
}
