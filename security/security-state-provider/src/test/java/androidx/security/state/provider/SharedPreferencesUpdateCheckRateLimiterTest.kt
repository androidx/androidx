/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.security.state.provider

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesUpdateCheckRateLimiterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefsName = "TestRateLimiter"

    private lateinit var rateLimiter: SharedPreferencesUpdateCheckRateLimiter

    @Before
    fun setUp() {
        // Clear prefs before each test
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit()

        // Initialize with a 1-hour throttle for testing
        rateLimiter =
            SharedPreferencesUpdateCheckRateLimiter(
                context = context,
                throttleIntervalMillis = TimeUnit.HOURS.toMillis(1),
                prefsFileName = prefsName,
            )
    }

    @Test
    fun shouldThrottle_returnsFalse_whenNeverRun() {
        // First run should always be allowed
        assertFalse("Should not throttle first attempt", rateLimiter.shouldThrottle())
    }

    @Test
    fun shouldThrottle_returnsTrue_immediatelyAfterAttempt() {
        // GIVEN an attempt is recorded
        rateLimiter.noteAttempt()

        // THEN immediate subsequent checks should be throttled
        assertTrue("Should throttle immediately after attempt", rateLimiter.shouldThrottle())
    }

    @Test
    fun shouldThrottle_returnsFalse_afterIntervalPasses() {
        // 1. Simulate an attempt made 2 hours ago
        // (We write directly to Prefs to simulate time passing without Thread.sleep)
        val twoHoursAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2)

        context
            .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putLong("last_attempt_time_millis", twoHoursAgo)
            .commit()

        // 2. Verify throttle is lifted
        assertFalse("Should allow request after interval", rateLimiter.shouldThrottle())
    }

    @Test
    fun shouldThrottle_returnsTrue_beforeIntervalPasses() {
        // 1. Simulate an attempt made 30 minutes ago
        val thirtyMinsAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)

        context
            .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putLong("last_attempt_time_millis", thirtyMinsAgo)
            .commit()

        // 2. Verify throttle is still active
        assertTrue("Should still throttle within interval", rateLimiter.shouldThrottle())
    }

    @Test
    fun shouldThrottle_returnsTrue_whenTimeMovesBackwards() {
        // 1. Simulate an attempt "in the future" (e.g. user set clock ahead, then back)
        val futureTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)

        context
            .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putLong("last_attempt_time_millis", futureTime)
            .commit()

        // 2. Verify we throttle.
        // This prevents an attacker from bypassing the limit by resetting the clock.
        assertTrue(
            "Should throttle if last attempt appears to be in the future",
            rateLimiter.shouldThrottle(),
        )
    }
}
