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

package androidx.car.moderator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

/**
 * Tests for the {@link ContentRateLimiter}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ContentRateLimiterTest {
    private static final double PERMITS_PER_SEC = 0.25d;
    private static final double MAX_STORED_PERMITS = 4d;
    private static final long DELAY_UNTIL_REFILL_SECONDS = 30;

    private ContentRateLimiter mContentRateLimiter;

    private final MockElapsedTimeProvider mElapsedTimeProvider =
            new MockElapsedTimeProvider();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mElapsedTimeProvider.setElapsedTime(SECONDS.toMillis(1000));
        mContentRateLimiter = new ContentRateLimiter(
                PERMITS_PER_SEC,
                MAX_STORED_PERMITS,
                SECONDS.toMillis(DELAY_UNTIL_REFILL_SECONDS),
                mElapsedTimeProvider);
    }

    @Test
    public void testTryAcquire_hasEnoughPermits() {
        assertTrue(mContentRateLimiter.tryAcquire());
    }

    @Test
    public void testTryAcquire_noAvailablePermitsWhileUnlimitedModeDisabled() {
        mContentRateLimiter.setAvailablePermits(0);
        assertFalse(mContentRateLimiter.tryAcquire());
    }

    @Test
    public void testTryAcquire_permitRefillDelayed() {
        mContentRateLimiter.tryAcquire();

        double availablePermits = mContentRateLimiter.getAvailablePermits();
        mElapsedTimeProvider.setElapsedTime(
                mElapsedTimeProvider.getElapsedRealtime() + SECONDS.toMillis(1));
        assertEquals(mContentRateLimiter.getAvailablePermits(), availablePermits, 0 /* delta */);

        mElapsedTimeProvider.setElapsedTime(mElapsedTimeProvider.getElapsedRealtime()
                + SECONDS.toMillis(DELAY_UNTIL_REFILL_SECONDS + 1));
        assertTrue(mContentRateLimiter.getAvailablePermits() > availablePermits);
    }

    @Test
    public void testTryAcquire_inFillDelayFringe_secondAcquireFree() {
        mContentRateLimiter.tryAcquire();
        double availablePermits = mContentRateLimiter.getAvailablePermits();
        mElapsedTimeProvider.setElapsedTime(mElapsedTimeProvider.getElapsedRealtime()
                + SECONDS.toMillis(DELAY_UNTIL_REFILL_SECONDS - 1));
        mContentRateLimiter.tryAcquire();
        assertEquals(availablePermits, mContentRateLimiter.getAvailablePermits(), 0 /* delta */);
    }

    @Test
    public void testTryAcquire_inFillDelayFringe_secondAcquireResetsFillDelay() {
        mContentRateLimiter.tryAcquire();
        double availablePermits = mContentRateLimiter.getAvailablePermits();
        mElapsedTimeProvider.setElapsedTime(
                mElapsedTimeProvider.getElapsedRealtime() + SECONDS.toMillis(1));
        mContentRateLimiter.tryAcquire();
        assertEquals(availablePermits, mContentRateLimiter.getAvailablePermits(), 0 /* delta */);

        long currentTimeMillis = mElapsedTimeProvider.getElapsedRealtime();

        // Advance to one second before the reset fringe should be over.
        mElapsedTimeProvider.setElapsedTime(
                currentTimeMillis + SECONDS.toMillis(DELAY_UNTIL_REFILL_SECONDS - 1));
        assertEquals(availablePermits, mContentRateLimiter.getAvailablePermits(), 0 /* delta */);

        // Advance to one second after the reset fringe should be over.
        mElapsedTimeProvider.setElapsedTime(
                currentTimeMillis + SECONDS.toMillis(DELAY_UNTIL_REFILL_SECONDS + 1));
        assertTrue(availablePermits < mContentRateLimiter.getAvailablePermits());
    }

    @Test
    public void testTryAcquire_inFillDelayFringe_thirdAcquireNotFree() {
        mContentRateLimiter.tryAcquire();

        double availablePermits = mContentRateLimiter.getAvailablePermits();
        mElapsedTimeProvider.setElapsedTime(
                mElapsedTimeProvider.getElapsedRealtime() + SECONDS.toMillis(1));
        mContentRateLimiter.tryAcquire();

        mElapsedTimeProvider.setElapsedTime(
                mElapsedTimeProvider.getElapsedRealtime() + SECONDS.toMillis(1));
        mContentRateLimiter.tryAcquire();
        assertTrue(availablePermits > mContentRateLimiter.getAvailablePermits());
    }

    @Test
    public void testTryAcquire_noAvailablePermitsThenUnlimitedModeEnabled() {
        mContentRateLimiter.setAvailablePermits(0);
        mContentRateLimiter.setUnlimitedMode(true);
        assertTrue(mContentRateLimiter.tryAcquire());
    }

    @Test
    public void testTryAcquire_noPermitsConsumedWhileUnlimitedModeEnabled() {
        mContentRateLimiter.setUnlimitedMode(true);
        assertTrue(mContentRateLimiter.tryAcquire());
        assertTrue(mContentRateLimiter.tryAcquire());
        assertTrue(mContentRateLimiter.tryAcquire());
        assertEquals(MAX_STORED_PERMITS, mContentRateLimiter.getAvailablePermits(), 0 /* delta */);
    }

    @Test
    public void testGetMaxStoredPermits() {
        assertEquals(MAX_STORED_PERMITS, mContentRateLimiter.getMaxStoredPermits(), 0 /* delta */);
    }

    @Test
    public void testSetAvailablePermits() {
        double permits = 2.0d;
        mContentRateLimiter.setAvailablePermits(permits);
        assertEquals(permits, mContentRateLimiter.getAvailablePermits(), 0 /* delta */);
    }

    @Test
    public void testSetAvailablePermits_setValueGreaterThanMax() {
        double greaterThanMax = MAX_STORED_PERMITS + 2;
        mContentRateLimiter.setAvailablePermits(greaterThanMax);
        assertEquals(MAX_STORED_PERMITS, mContentRateLimiter.getAvailablePermits(), 0 /* delta */);
    }

    /**
     * A mock {@link androidx.car.moderator.ContentRateLimiter.ElapsedTimeProvider} that allows
     * the current elapsed time to be set.
     */
    private class MockElapsedTimeProvider implements ContentRateLimiter.ElapsedTimeProvider {
        private long mElapsedTime;

        private void setElapsedTime(long elapsedTime) {
            mElapsedTime = elapsedTime;
        }

        @Override
        public long getElapsedRealtime() {
            return mElapsedTime;
        }
    }
}
