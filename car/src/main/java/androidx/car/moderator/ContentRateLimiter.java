/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.car.moderator;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.util.Log;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

/**
 * A class that keeps track of a general number of permitted actions that happen over time and
 * determines if a subsequent interaction is allowed. The type of interaction is arbitrary and not
 * transparent to this class. Instead, it will refer to these actions as "permits," short for
 * "permitted action." It is up to a user of this class to determine the unit of permits.
 *
 * <p>This class allows for two quick acquires in succession to only consume one permit. This is
 * intended behavior to account for the fact that the user can be using many taps to scroll
 * quickly. This can fit within the window for which a user does not necessary have their eyes
 * off the road for a long period of time, and thus should not be penalized.
 *
 * <p>This class allows for the maximum number of permits that can be stored,the amount of permits
 * that are filled each second, as well as the delay before re-fill to be configured.
 */
public class ContentRateLimiter {
    private static final String TAG = "ContentRateLimiter";

    /** The maximum number of stored permits. */
    private double mMaxStoredPermits;

    /**
     * The interval between two unit requests at our stable rate. For example, a stable rate of
     * 5 permits per second has a stable interval of 200ms.
     */
    private long mStableIntervalMs;

    /**
     * The amount of time to wait between when a permit is acquired and when the model starts
     * refilling.
     */
    private long mFillDelayMs;

    /** Unlimited mode. Once enabled, any number of permits can be acquired and consumed. */
    private boolean mUnlimitedModeEnabled;

    /**
     * Used to do incremental calculations by {@link #getLastCalculatedPermitCount()}, cannot be
     * used directly.
     */
    private double mLastCalculatedPermitCount;

    /** Time in milliseconds when permits can resume incrementing. */
    private long mResumeIncrementingMs;

    /** Tracks if the model will allow a second permit to be requested in the fill delay. */
    private boolean mSecondaryFillDelayPermitAvailable = true;

    private final ElapsedTimeProvider mElapsedTimeProvider;

    /**
     * An interface for a provider of the current time that has passed since boot. This interface
     * is meant to abstract the {@link android.os.SystemClock} so that it can be mocked during
     * testing.
     */
    interface ElapsedTimeProvider {
        /** Returns milliseconds since boot, including time spent in sleep. */
        long getElapsedRealtime();
    }

    /**
     * Creates a {@code ContentRateLimiter} with the given parameters.
     *
     * @param acquiredPermitsPerSecond The amount of permits that are acquired each second.
     * @param maxStoredPermits The maximum number of permits that can be stored.
     * @param fillDelayMs The amount of time to wait between when a permit is acquired and when
     *                    the number of available permits start refilling.
     */
    public ContentRateLimiter(
            @FloatRange(from = 0, fromInclusive = false) double acquiredPermitsPerSecond,
            @FloatRange(from = 0) double maxStoredPermits,
            @IntRange(from = 0) long fillDelayMs) {
        this(acquiredPermitsPerSecond, maxStoredPermits, fillDelayMs,
                new SystemClockTimeProvider());
    }

    // A constructor that allows for the SystemClockTimeProvider to be provided. This is needed for
    // testing so that the unit test does not rely on the actual SystemClock.
    @VisibleForTesting
    ContentRateLimiter(
            @FloatRange(from = 0, fromInclusive = false) double acquiredPermitsPerSecond,
            @FloatRange(from = 0) double maxStoredPermits,
            @IntRange(from = 0) long fillDelayMs,
            ElapsedTimeProvider elapsedTimeProvider) {
        mElapsedTimeProvider = elapsedTimeProvider;
        mResumeIncrementingMs = mElapsedTimeProvider.getElapsedRealtime();
        setAcquiredPermitsRate(acquiredPermitsPerSecond);
        setMaxStoredPermits(maxStoredPermits);
        setPermitFillDelay(fillDelayMs);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format("permitsPerSecond: %f maxStoredPermits: %f, fillDelayMs %d",
                    acquiredPermitsPerSecond, maxStoredPermits, fillDelayMs));
        }
    }

    /**
     * Sets the amount of permits that are acquired each second. These permits are acquired when
     * the {@code ContentRateLimiter} is not being interacted with. That is, the
     * {@link #tryAcquire()} or {@link #tryAcquire(int)} methods have not been called.
     *
     * @param acquiredPermitsPerSecond The number of permits acquired each second. Must be greater
     *                                 than zero.
     */
    public void setAcquiredPermitsRate(
            @FloatRange(from = 0, fromInclusive = false) double acquiredPermitsPerSecond) {
        Preconditions.checkArgument(acquiredPermitsPerSecond > 0);
        mStableIntervalMs = (long) (SECONDS.toMillis(1L) / acquiredPermitsPerSecond);
    }

    /**
     * The maximum amount of permits that can be stored. Permits are accumulated when the
     * the {@code ContentRateLimiter} is not being interacted with. That is, the
     * {@link #tryAcquire()} or {@link #tryAcquire(int)} methods have not been called.
     *
     * @param maxStoredPermits The maximum number of stored permits. Must be greater than or equal
     *                         to zero.
     */
    public void setMaxStoredPermits(@FloatRange(from = 0) double maxStoredPermits) {
        Preconditions.checkArgument(maxStoredPermits >= 0);
        mMaxStoredPermits = maxStoredPermits;
        mLastCalculatedPermitCount = maxStoredPermits;
    }

    /**
     * Sets delay before permits begin accumulating. This is the delay after a {@link #tryAcquire()}
     * or {@link #tryAcquire(int)} has been called. After the given delay, permits will be
     * accumulated at the rate set by {@link #setAcquiredPermitsRate(double)}.
     *
     * @param fillDelayMs The delay in milliseconds before permits accumulate.
     */
    public void setPermitFillDelay(@IntRange(from = 0) long fillDelayMs) {
        Preconditions.checkArgument(fillDelayMs >= 0);
        mFillDelayMs = fillDelayMs;
    }

    /** Gets the current number of stored permits ready to be used. */
    @MainThread
    public double getAvailablePermits() {
        return getLastCalculatedPermitCount();
    }

    /**
     * Sets the current number of stored permits that are ready to be used. If this value exceeds
     * the maximum number of stored permits that is passed to the constructor, then the max value
     * is used instead.
     */
    @MainThread
    public void setAvailablePermits(double availablePermits) {
        setLastCalculatedPermitCount(availablePermits, mElapsedTimeProvider.getElapsedRealtime());
    }

    /** Gets the max number of permits allowed to be stored for future usage. */
    public double getMaxStoredPermits() {
        return mMaxStoredPermits;
    }

    /**
     * Checks if there are enough available permits for a single permit to be acquired.
     *
     * @return {@code true} if unlimited mode is enabled or enough permits are acquirable at the
     * time of this call; {@code false} if there isn't the number of permits requested available
     * currently.
     */
    @MainThread
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /**
     * Checks whether there are enough available permits to acquire.
     *
     * @return {@code true} if unlimited mode is enabled or enough permits are acquirable at the
     * time of this call; {@code false} if there isn't the number of permits requested available
     * currently.
     */
    @MainThread
    public boolean tryAcquire(int permits) {
        // Once unlimited mode is enabled, we can acquire any number of permits we want and don't
        // consume the stored permits.
        if (mUnlimitedModeEnabled) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Unlimited mode is enabled.");
            }
            return true;
        }
        double availablePermits = getLastCalculatedPermitCount();
        long nowMs = mElapsedTimeProvider.getElapsedRealtime();

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format("Requesting: %d, Stored: %f/%f", permits,
                    mLastCalculatedPermitCount, mMaxStoredPermits));
        }
        if (availablePermits <= permits) {
            // Once locked out, the user is prevented from acquiring any more permits until they
            // have waited long enough for a permit to refill. If the user attempts to acquire a
            // permit during this time, the countdown timer until a permit is refilled is reset.
            setLastCalculatedPermitCount(0, nowMs + mFillDelayMs);
            return false;
        } else if (nowMs < mResumeIncrementingMs && mSecondaryFillDelayPermitAvailable) {
            // If a second permit is requested between the time a first permit was requested and
            // the fill delay, allow the second permit to be acquired without decrementing the model
            // and set the point where permits can resume incrementing {@link #mFillDelayMs} in the
            // future.
            setLastCalculatedPermitCount(availablePermits, nowMs + mFillDelayMs);
            // Don't allow a third "free" permit to be acquired in the fill delay fringe.
            mSecondaryFillDelayPermitAvailable = false;

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Used up free secondary permit");
            }
            return true;
        } else {
            // Decrement the available permits, and set the point where permits can resume
            // incrementing {@link #mFillDelayMs} in the future.
            setLastCalculatedPermitCount(availablePermits - permits, nowMs + mFillDelayMs);

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, String.format("permits remaining %s, secondary permit available %s",
                        mLastCalculatedPermitCount,
                        mSecondaryFillDelayPermitAvailable));
            }

            mSecondaryFillDelayPermitAvailable = true;
            return true;
        }
    }

    /**
     * Sets unlimited mode. If enabled, there is no restriction on the number of permits that
     * can be acquired and any interaction does not consume stored permits.
     */
    public void setUnlimitedMode(boolean enabled) {
        mUnlimitedModeEnabled = enabled;
    }

    /**
     * Updates {@link #mLastCalculatedPermitCount} and {@link #mResumeIncrementingMs} based on the
     * current time.
     */
    private double getLastCalculatedPermitCount() {
        long nowMs = mElapsedTimeProvider.getElapsedRealtime();
        if (nowMs > mResumeIncrementingMs) {
            long deltaMs = nowMs - mResumeIncrementingMs;
            double newPermits = deltaMs / (double) mStableIntervalMs;
            setLastCalculatedPermitCount(mLastCalculatedPermitCount + newPermits, nowMs);
        }
        return mLastCalculatedPermitCount;
    }

    private void setLastCalculatedPermitCount(double newCount, long nextMs) {
        mLastCalculatedPermitCount = Math.min(mMaxStoredPermits, newCount);
        mResumeIncrementingMs = nextMs;
    }
}
