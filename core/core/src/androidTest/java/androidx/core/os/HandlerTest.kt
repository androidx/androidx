/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.os

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.os.TestLooperManager
import androidx.annotation.RequiresApi
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@SmallTest
@SdkSuppress(minSdkVersion = 26)
class HandlerTest {
    private val handlerThread = HandlerThread("handler-test")
    private lateinit var looper: Looper
    private lateinit var handler: Handler

    @Before
    fun before() {
        handlerThread.start()
        looper = handlerThread.looper
        handler = Handler(looper)
    }

    @After
    fun after() {
        handlerThread.quit()
    }

    @Test
    fun postDelayedLambdaMillis() {
        var called = 0
        handler.postDelayed(10) { called++ }

        handler.await(20, MILLISECONDS)
        assertEquals(1, called)
    }

    @Test
    fun postDelayedLambdaMillisRemoved() {
        looper.manage { manager ->
            val runnable = handler.postDelayed(1000) { throw AssertionError() }
            handler.removeCallbacks(runnable)

            assertFalse(manager.hasMessages(handler))
        }
    }

    @Test
    fun postAtTimeLambda() {
        var called = 0
        handler.postAtTime(SystemClock.uptimeMillis() + 10) { called++ }

        handler.await(20, MILLISECONDS)
        assertEquals(1, called)
    }

    @Test
    fun postAtTimeLambdaRemoved() {
        looper.manage { manager ->
            val runnable =
                handler.postAtTime(SystemClock.uptimeMillis() + 1000) { throw AssertionError() }
            handler.removeCallbacks(runnable)

            assertFalse(manager.hasMessages(handler))
        }
    }

    @Test
    fun postAtTimeLambdaWithTokenRuns() {
        val token = Any()
        var called = 0
        handler.postAtTime(SystemClock.uptimeMillis() + 10, token) { called++ }

        handler.await(20, MILLISECONDS)
        assertEquals(1, called)
    }

    @Test
    fun postAtTimeLambdaWithTokenCancelWithToken() {
        // This test uses the token to cancel the runnable as it's the only way we have to verify
        // that the Runnable was actually posted with the token.

        looper.manage { manager ->
            val token = Any()
            handler.postAtTime(SystemClock.uptimeMillis() + 1000, token) { throw AssertionError() }
            handler.removeCallbacksAndMessages(token)

            assertFalse(manager.hasMessages(handler))
        }
    }

    private fun Handler.await(amount: Long, unit: TimeUnit) {
        val latch = CountDownLatch(1)
        postDelayed(latch::countDown, unit.toMillis(amount))

        // Wait up to 1s longer than desired to account for time skew.
        val wait = unit.toMillis(amount) + SECONDS.toMillis(1)
        assertTrue(latch.await(wait, MILLISECONDS))
    }

    @RequiresApi(26)
    private inline fun Looper.manage(block: (TestLooperManager) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val looperManager = instrumentation.acquireLooperManager(this)
        try {
            block(looperManager)
        } finally {
            looperManager.release()
        }
    }

    @RequiresApi(26)
    private fun TestLooperManager.hasMessages(handler: Handler) = hasMessages(handler, null, null)
}
