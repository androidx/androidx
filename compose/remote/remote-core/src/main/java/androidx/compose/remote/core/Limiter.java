/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;

/**
 * Support limiting the frame rate of a repaint both instantaneous and averaged over a window.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Limiter {

    /** Bucket count. Power of two so the ring index is a mask, not a modulo. */
    private static final int BUCKETS = 64;
    private static final int MASK = BUCKETS - 1;

    private static final int DEFAULT_MAX_FPS = Limits.DEFAULT_MAX_FPS;
    private static final int DEFAULT_MAX_AVG_FPS = Limits.DEFAULT_MAX_AVG_FPS;
    private static final int DEFAULT_WINDOW_SEC = Limits.DEFAULT_WINDOW_SEC;

    private int mMaxFps = DEFAULT_MAX_FPS;
    private int mMaxAvgFps = DEFAULT_MAX_AVG_FPS;
    private int mWindowSec = DEFAULT_WINDOW_SEC;

    /** Derived: minimum gap between frames, ns. */
    private long mMinIntervalNs;
    /** Derived: the delay imposed once the gate closes, ns. */
    private long mAvgIntervalNs;
    /** Derived: width of one bucket, ns. */
    private long mBucketNs;
    /** Derived: the span the ring actually covers, ns (BUCKETS * mBucketNs). */
    private long mSpanNs;
    /** Derived: frame count at which the gate closes. */
    private int mFrameLimit;

    private final int[] mBuckets = new int[BUCKETS];
    /** Frames currently held in the ring. Kept in step with mBuckets. */
    private int mCount;
    /** Absolute index of the newest bucket, or MIN_VALUE before the first frame. */
    private long mHeadIndex = Long.MIN_VALUE;
    private long mLastDrawTime;
    private boolean mHasDrawn;

    public Limiter() {
        setup();
    }

    // ------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------

    /** Frames are never scheduled closer together than {@code 1/maxFps}. */
    public void setMaxFps(int maxFps) {
        if (maxFps < 1) {
            maxFps = 1;
        }
        if (maxFps != mMaxFps) {
            mMaxFps = maxFps;
            setup();
        }
    }

    /** Returns the instantaneous maximum frame rate. */
    public int getMaxFps() {
        return mMaxFps;
    }

    /** Ceiling on the frame rate averaged over the window. */
    public void setMaxAvgFps(int maxAvgFps) {
        if (maxAvgFps < 1) {
            maxAvgFps = 1;
        }
        if (maxAvgFps != mMaxAvgFps) {
            mMaxAvgFps = maxAvgFps;
            setup();
        }
    }

    /** Returns the ceiling on the frame rate averaged over the window. */
    public int getMaxAvgFps() {
        return mMaxAvgFps;
    }

    /** Length of the averaging window in seconds. Resets the window history. */
    public void setWindow(int seconds) {
        if (seconds < 1) {
            seconds = 1;
        }
        if (seconds != mWindowSec) {
            mWindowSec = seconds;
            setup();
            reset();
        }
    }

    /** Returns the length of the averaging window in seconds. */
    public int getWindow() {
        return mWindowSec;
    }

    /** Returns the derived minimum interval between frames in nanoseconds. */
    public long getMinIntervalNs() {
        return mMinIntervalNs;
    }

    /** Returns the derived average interval between frames in nanoseconds when throttled. */
    public long getAvgIntervalNs() {
        return mAvgIntervalNs;
    }

    private void setup() {
        mMinIntervalNs = ceilDiv(1_000_000_000L, mMaxFps);
        mAvgIntervalNs = ceilDiv(1_000_000_000L, mMaxAvgFps);
        mBucketNs = Math.max(1L, ((long) mWindowSec * 1_000_000_000L) / BUCKETS);
        mSpanNs = mBucketNs * BUCKETS;
        // avg >= maxAvgFps  <=>  count * 1000000000 >= maxAvgFps * span
        mFrameLimit = (int) (((long) mMaxAvgFps * mSpanNs + 999_999_999L) / 1_000_000_000L);
    }

    // ------------------------------------------------------------------
    // Per frame
    // ------------------------------------------------------------------

    /**
     * Record that a frame was drawn. Call once per repaint, before {@link #computeDelay}.
     *
     * @param currentTime monotonic clock, ns
     */
    public void recordDrawStart(long currentTime) {
        advance(currentTime);                   // leaves mHeadIndex == currentTime / mBucketNs
        mBuckets[(int) (mHeadIndex & MASK)]++;
        mCount++;
        mLastDrawTime = currentTime;
        mHasDrawn = true;
    }

    /**
     * Limit a requested repaint delay, evaluated as of the last {@link #recordDrawStart}.
     *
     * @param requestedDelayInNs what the animation asked for, ns
     * @return the delay to actually post, ns
     */
    public long computeDelay(long requestedDelayInNs) {
        return computeDelay(requestedDelayInNs, mLastDrawTime);
    }

    /**
     * Limit a requested repaint delay, evaluated as of {@code currentTime}.
     *
     * @param requestedDelayInNs what the animation asked for, ns
     * @param currentTime        monotonic clock, ns
     * @return the delay to actually post, ns
     */
    public long computeDelay(long requestedDelayInNs, long currentTime) {
        if (mHasDrawn) {
            advance(currentTime);
        }
        long delay = requestedDelayInNs;
        if (delay < mMinIntervalNs) {
            delay = mMinIntervalNs;             // instantaneous ceiling
        }
        if (mCount >= mFrameLimit && delay < mAvgIntervalNs) {
            delay = mAvgIntervalNs;             // gate closed: average is at the limit
        }
        return delay;
    }


    /**
     * Limit a requested repaint delay, evaluated as of {@code currentTime}.
     *
     * @param nextFrame   what the animation asked for, s
     * @param currentTime monotonic clock, ns
     * @return the delay to actually post, ns
     */
    public long computeDelay(float nextFrame, long currentTime) {
        long requestedDelayNs = (nextFrame == 1) ? 0L : (long) (nextFrame * 1_000_000L);
        if (mHasDrawn) {
            advance(currentTime);
        }
        long delay = requestedDelayNs;
        if (delay < mMinIntervalNs) {
            delay = mMinIntervalNs;             // instantaneous ceiling
        }
        if (mCount >= mFrameLimit && delay < mAvgIntervalNs) {
            delay = mAvgIntervalNs;             // gate closed: average is at the limit
        }
        return delay;
    }


    /**
     * Forget the window. The average reads zero, so the next request is honoured in
     * full and the engine runs at whatever it asks for, up to maxFps.
     *
     * <p>Call on touch down and on each move: while the user is dragging, the gate
     * stays open. The window refills from empty once they let go, which buys roughly
     * one full window of unthrottled frames after the interaction ends.
     */
    public void touchBoost() {
        java.util.Arrays.fill(mBuckets, 0);
        mCount = 0;
    }

    /** Drop all history and timing state. */
    public void reset() {
        java.util.Arrays.fill(mBuckets, 0);
        mCount = 0;
        mHeadIndex = Long.MIN_VALUE;
        mHasDrawn = false;
    }

    /**
     * Roll the ring forward to {@code time}, clearing the buckets that have aged out
     * and keeping {@link #mCount} in step. O(1) amortised; O(BUCKETS) worst case when
     * the engine has been idle, and that path only runs once per idle period.
     */
    private void advance(long time) {
        long index = time / mBucketNs;
        if (mHeadIndex == Long.MIN_VALUE) {
            mHeadIndex = index;
            return;
        }
        if (index == mHeadIndex) {
            return;
        }
        if (index < mHeadIndex) {               // clock went backwards
            reset();
            mHeadIndex = index;
            return;
        }
        if (index - mHeadIndex >= BUCKETS) {    // idle longer than the window
            java.util.Arrays.fill(mBuckets, 0);
            mCount = 0;
        } else {
            for (long i = mHeadIndex + 1; i <= index; i++) {
                int slot = (int) (i & MASK);
                mCount -= mBuckets[slot];
                mBuckets[slot] = 0;
            }
        }
        mHeadIndex = index;
    }

    private static long ceilDiv(long a, long b) {
        return (a + b - 1) / b;
    }
}
