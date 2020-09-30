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

package androidx.mediarouter.media;

import static androidx.mediarouter.media.MediaRouterActiveScanThrottlingHelper.MAX_ACTIVE_SCAN_DURATION_MS;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link MediaRouterActiveScanThrottlingHelper}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaRouterActiveScanThrottlingHelperTest {
    private static final long TIME_OUT_MS = 3000;
    private CountDownLatch mCountDownLatch;
    private Runnable mRunnable;

    @Before
    public void setUp() {
        resetCountDownLatch();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                mCountDownLatch.countDown();
            }
        };
    }

    @Test
    @LargeTest
    public void testActiveScan_noActiveScan() throws Exception {
        long currentTime = SystemClock.elapsedRealtime();
        MediaRouterActiveScanThrottlingHelper helper = new MediaRouterActiveScanThrottlingHelper(
                mRunnable);

        helper.reset();

        helper.requestActiveScan(false, currentTime);
        helper.requestActiveScan(false, currentTime);
        helper.requestActiveScan(false, currentTime);

        assertFalse(helper.finalizeActiveScanAndScheduleSuppressActiveScanRunnable());

        assertFalse(mCountDownLatch.await(
                MAX_ACTIVE_SCAN_DURATION_MS + TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @LargeTest
    public void testActiveScan_hasNonExpiredActiveScan() throws Exception {
        long currentTime = SystemClock.elapsedRealtime();
        MediaRouterActiveScanThrottlingHelper helper = new MediaRouterActiveScanThrottlingHelper(
                mRunnable);

        helper.reset();

        helper.requestActiveScan(true, currentTime);
        helper.requestActiveScan(false, currentTime);
        helper.requestActiveScan(false, currentTime);

        assertTrue(helper.finalizeActiveScanAndScheduleSuppressActiveScanRunnable());

        assertTrue(mCountDownLatch.await(
                MAX_ACTIVE_SCAN_DURATION_MS + TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @LargeTest
    public void testActiveScan_allActiveScanRequestsExpired() throws Exception {
        long currentTime = SystemClock.elapsedRealtime();
        MediaRouterActiveScanThrottlingHelper helper =
                new MediaRouterActiveScanThrottlingHelper(mRunnable);

        helper.reset();

        helper.requestActiveScan(true, currentTime - MAX_ACTIVE_SCAN_DURATION_MS);
        helper.requestActiveScan(true, currentTime - MAX_ACTIVE_SCAN_DURATION_MS);
        helper.requestActiveScan(true, currentTime - MAX_ACTIVE_SCAN_DURATION_MS);

        assertFalse(helper.finalizeActiveScanAndScheduleSuppressActiveScanRunnable());

        assertFalse(mCountDownLatch.await(
                MAX_ACTIVE_SCAN_DURATION_MS + TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }


    @Test
    @LargeTest
    public void testActiveScan_allActiveScanRequestsWithMixedTimestamps() throws Exception {
        long currentTime = SystemClock.elapsedRealtime();
        MediaRouterActiveScanThrottlingHelper helper =
                new MediaRouterActiveScanThrottlingHelper(mRunnable);

        helper.reset();

        helper.requestActiveScan(true, currentTime - 2000);
        helper.requestActiveScan(true, currentTime - 5000);
        helper.requestActiveScan(true, currentTime - 7000);

        assertTrue(helper.finalizeActiveScanAndScheduleSuppressActiveScanRunnable());

        // Active scan should not be suppressed before the last active scan is in effect.
        assertFalse(mCountDownLatch.await(MAX_ACTIVE_SCAN_DURATION_MS - 2000 - 1000,
                TimeUnit.MILLISECONDS));

        // Active scan should be suppressed after the last active scan times out.
        assertTrue(mCountDownLatch.await(1000 + TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @LargeTest
    public void testReset_shouldCleanupActiveScanAndSuppressingRunnable() throws Exception {
        long currentTime = SystemClock.elapsedRealtime();
        MediaRouterActiveScanThrottlingHelper helper =
                new MediaRouterActiveScanThrottlingHelper(mRunnable);

        helper.reset();

        helper.requestActiveScan(true, currentTime);
        helper.requestActiveScan(false, currentTime);
        helper.requestActiveScan(false, currentTime);

        assertTrue(helper.finalizeActiveScanAndScheduleSuppressActiveScanRunnable());

        helper.reset();

        assertFalse(mCountDownLatch.await(
                MAX_ACTIVE_SCAN_DURATION_MS + TIME_OUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(helper.finalizeActiveScanAndScheduleSuppressActiveScanRunnable());
    }

    private void resetCountDownLatch() {
        mCountDownLatch = new CountDownLatch(1);
    }
}
