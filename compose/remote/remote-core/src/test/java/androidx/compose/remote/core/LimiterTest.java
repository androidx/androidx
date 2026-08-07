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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LimiterTest {

    @Test
    public void testDefaultInitialization() {
        Limiter limiter = new Limiter();
        // Default maxFps = 60 -> minIntervalNs = ceilDiv(1_000_000_000, 60) = 16_666_667 ns
        assertEquals(16_666_667L, limiter.computeDelay(0));
        assertEquals(16_666_667L, limiter.computeDelay(10_000_000L));
        assertEquals(50_000_000L, limiter.computeDelay(50_000_000L));
    }

    @Test
    public void testSetMaxFps() {
        Limiter limiter = new Limiter();

        // 30 FPS -> ceilDiv(1_000_000_000, 30) = 33_333_334 ns
        limiter.setMaxFps(30);
        assertEquals(33_333_334L, limiter.computeDelay(0));
        assertEquals(33_333_334L, limiter.computeDelay(20_000_000L));
        assertEquals(40_000_000L, limiter.computeDelay(40_000_000L));

        // 120 FPS -> ceilDiv(1_000_000_000, 120) = 8_333_334 ns
        limiter.setMaxFps(120);
        assertEquals(8_333_334L, limiter.computeDelay(0));
        assertEquals(8_333_334L, limiter.computeDelay(5_000_000L));
        assertEquals(15_000_000L, limiter.computeDelay(15_000_000L));

        // Non-positive maxFps should be clamped to 1 FPS -> ceilDiv(1_000_000_000, 1) =
        // 1_000_000_000 ns
        limiter.setMaxFps(0);
        assertEquals(1_000_000_000L, limiter.computeDelay(500_000_000L));

        limiter.setMaxFps(-10);
        assertEquals(1_000_000_000L, limiter.computeDelay(500_000_000L));
    }

    @Test
    public void testSetMaxAvgFpsAndThrottling() {
        Limiter limiter = new Limiter();
        limiter.setMaxFps(100); // minIntervalNs = 10_000_000 ns
        limiter.setMaxAvgFps(5); // avgIntervalNs = 200_000_000 ns
        limiter.setWindow(1); // bucketNs = 15_625_000 ns, spanNs = 1_000_000_000 ns, frameLimit = 5

        // Record 5 draws at t=0, 10ms, 20ms, 30ms, 40ms (in ns)
        for (long t = 0; t <= 40_000_000L; t += 10_000_000L) {
            limiter.recordDrawStart(t);
        }

        // 5 frames drawn, reaching frameLimit (5)
        // Request delay smaller than avgIntervalNs (200_000_000 ns) -> clamped to 200_000_000 ns
        assertEquals(200_000_000L, limiter.computeDelay(10_000_000L, 40_000_000L));
        assertEquals(200_000_000L, limiter.computeDelay(50_000_000L, 40_000_000L));

        // Request delay larger than avgIntervalNs -> returned as is
        assertEquals(250_000_000L, limiter.computeDelay(250_000_000L, 40_000_000L));

        // Non-positive maxAvgFps should be clamped to 1 FPS -> avgIntervalNs = 1_000_000_000 ns
        limiter.setMaxAvgFps(0);
        assertEquals(1_000_000_000L, limiter.computeDelay(10_000_000L, 40_000_000L));
    }

    @Test
    public void testSetWindowResetsHistory() {
        Limiter limiter = new Limiter();
        limiter.setMaxFps(100); // minIntervalNs = 10_000_000 ns
        limiter.setMaxAvgFps(5); // avgIntervalNs = 200_000_000 ns
        limiter.setWindow(1); // frameLimit = 5

        // Fill history
        for (long t = 0; t <= 40_000_000L; t += 10_000_000L) {
            limiter.recordDrawStart(t);
        }
        assertEquals(200_000_000L, limiter.computeDelay(10_000_000L, 40_000_000L));

        // Changing window resets history
        limiter.setWindow(2);
        // Gate should be open again since frame count was reset to 0
        assertEquals(10_000_000L, limiter.computeDelay(10_000_000L, 40_000_000L));

        // Non-positive window should be clamped to 1 sec
        limiter.setWindow(0);
        assertEquals(10_000_000L, limiter.computeDelay(10_000_000L, 40_000_000L));
    }

    @Test
    public void testBucketAging() {
        Limiter limiter = new Limiter();
        limiter.setMaxFps(100); // minIntervalNs = 10_000_000 ns
        limiter.setMaxAvgFps(5); // avgIntervalNs = 200_000_000 ns
        limiter.setWindow(1); // bucketNs = 15_625_000 ns, spanNs = 1_000_000_000 ns, frameLimit = 5

        // Draw 5 frames in bucket 0 (t=0)
        for (int i = 0; i < 5; i++) {
            limiter.recordDrawStart(0);
        }
        assertEquals(200_000_000L, limiter.computeDelay(10_000_000L, 0));

        // Advance time past the span (1 sec = 1_000_000_000 ns)
        // Buckets at t=0 will age out
        assertEquals(10_000_000L, limiter.computeDelay(10_000_000L, 1_000_000_000L));
    }

    @Test
    public void testTouchBoost() {
        Limiter limiter = new Limiter();
        limiter.setMaxFps(100); // minIntervalNs = 10_000_000 ns
        limiter.setMaxAvgFps(5); // avgIntervalNs = 200_000_000 ns
        limiter.setWindow(1); // frameLimit = 5

        for (long t = 0; t <= 40_000_000L; t += 10_000_000L) {
            limiter.recordDrawStart(t);
        }
        assertEquals(200_000_000L, limiter.computeDelay(10_000_000L, 40_000_000L));

        // Touch boost clears history
        limiter.touchBoost();
        assertEquals(10_000_000L, limiter.computeDelay(10_000_000L, 40_000_000L));
    }

    @Test
    public void testReset() {
        Limiter limiter = new Limiter();
        limiter.setMaxFps(100); // minIntervalNs = 10_000_000 ns
        limiter.setMaxAvgFps(5); // avgIntervalNs = 200_000_000 ns
        limiter.setWindow(1); // frameLimit = 5

        for (long t = 0; t <= 40_000_000L; t += 10_000_000L) {
            limiter.recordDrawStart(t);
        }
        assertEquals(200_000_000L, limiter.computeDelay(10_000_000L, 40_000_000L));

        limiter.reset();

        // Delay should not be throttled by average FPS after reset
        assertEquals(10_000_000L, limiter.computeDelay(10_000_000L, 40_000_000L));
    }

    @Test
    public void testClockGoingBackwards() {
        Limiter limiter = new Limiter();
        limiter.setMaxFps(100);
        limiter.setMaxAvgFps(5);
        limiter.setWindow(1);

        limiter.recordDrawStart(1_000_000_000L);
        // Clock jumps backward to 500ms (500_000_000 ns)
        limiter.recordDrawStart(500_000_000L);

        // State is reset and now tracking from t=500ms, count = 1
        assertEquals(10_000_000L, limiter.computeDelay(10_000_000L, 500_000_000L));
    }

    @Test
    public void testIdleLongerThanWindow() {
        Limiter limiter = new Limiter();
        limiter.setMaxFps(100);
        limiter.setMaxAvgFps(5);
        limiter.setWindow(1);

        limiter.recordDrawStart(100_000_000L);
        // Idle for 100 seconds (100_000_000_000 ns)
        limiter.recordDrawStart(100_100_000_000L);

        // Ring is cleared on advance, count = 1
        assertEquals(10_000_000L, limiter.computeDelay(10_000_000L, 100_100_000_000L));
    }

    @Test
    public void testDelayWithoutExplicitTime() {
        Limiter limiter = new Limiter();
        limiter.setMaxFps(100);
        limiter.setMaxAvgFps(5);
        limiter.setWindow(1);

        limiter.recordDrawStart(50_000_000L);
        // delay(requestedDelay) should use mLastDrawTime (50_000_000 ns)
        assertEquals(10_000_000L, limiter.computeDelay(5_000_000L));
    }
}
