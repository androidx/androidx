/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.os

import android.app.UiAutomation
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ProfilingResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = 35)
class ProfilingTest {

    private val mUiAutomation: UiAutomation =
        InstrumentationRegistry.getInstrumentation().uiAutomation

    @Before
    fun setup() {
        // Override the rate limiter
        mUiAutomation.executeShellCommand(
            "device_config put profiling_testing rate_limiter.disabled true"
        )
    }

    /** Test requesting a java heap dump with all parameters set. */
    @Test
    fun testRequestJavaHeapDump() {
        val listener = ResultListener()

        requestProfiling(
            ApplicationProvider.getApplicationContext(),
            JavaHeapDumpRequestBuilder().setBufferSizeKb(1000).build(),
            Executors.newSingleThreadExecutor(),
            listener
        )

        waitForSignal(listener.mAcceptedSignal)

        assertNotNull(listener.mResult)
        assertEquals(ProfilingResult.ERROR_NONE, listener.mResult!!.errorCode)
    }

    /** Test requesting a heap profile with all parameters set. */
    @Test
    fun testRequestHeapProfile() {
        val listener = ResultListener()

        requestProfiling(
            ApplicationProvider.getApplicationContext(),
            HeapProfileRequestBuilder()
                .setBufferSizeKb(1000)
                .setDurationMs(5000)
                .setTrackJavaAllocations(false)
                .setSamplingIntervalBytes(100)
                .build(),
            Executors.newSingleThreadExecutor(),
            listener
        )

        waitForSignal(listener.mAcceptedSignal)

        assertNotNull(listener.mResult)
        assertEquals(ProfilingResult.ERROR_NONE, listener.mResult!!.errorCode)
    }

    /** Test requesting stack sampling with all parameters set. */
    @Test
    fun testRequestStackSampling() {
        val listener = ResultListener()

        requestProfiling(
            ApplicationProvider.getApplicationContext(),
            StackSamplingRequestBuilder()
                .setBufferSizeKb(1000)
                .setDurationMs(5000)
                .setSamplingFrequencyHz(100)
                .build(),
            Executors.newSingleThreadExecutor(),
            listener
        )

        waitForSignal(listener.mAcceptedSignal)

        assertNotNull(listener.mResult)
        assertEquals(ProfilingResult.ERROR_NONE, listener.mResult!!.errorCode)
    }

    /** Test requesting a system trace with all parameters set. */
    @Test
    fun testRequestSystemTrace() {
        val listener = ResultListener()

        requestProfiling(
            ApplicationProvider.getApplicationContext(),
            SystemTraceRequestBuilder()
                .setBufferSizeKb(1000)
                .setDurationMs(5000)
                .setBufferFillPolicy(BufferFillPolicy.DISCARD)
                .build(),
            Executors.newSingleThreadExecutor(),
            listener
        )

        waitForSignal(listener.mAcceptedSignal)

        assertNotNull(listener.mResult)
        assertEquals(ProfilingResult.ERROR_NONE, listener.mResult!!.errorCode)
    }

    /**
     * Test requesting cancellation. Duration is set longer than test timeout to ensure the
     * cancellation worked.
     */
    @Test
    fun testCancellation() {
        val cancellationSignal = CancellationSignal()
        val listener = ResultListener()

        requestProfiling(
            ApplicationProvider.getApplicationContext(),
            SystemTraceRequestBuilder()
                .setBufferSizeKb(1000)
                .setDurationMs(5 * 60 * 1000)
                .setBufferFillPolicy(BufferFillPolicy.RING_BUFFER)
                .setCancellationSignal(cancellationSignal)
                .build(),
            Executors.newSingleThreadExecutor(),
            listener
        )

        // Schedule cancellation to occur after some short wait
        Handler(Looper.getMainLooper())
            .postDelayed({ cancellationSignal.cancel() }, (5 * 1000).toLong())

        waitForSignal(listener.mAcceptedSignal)

        assertNotNull(listener.mResult)
        assertEquals(ProfilingResult.ERROR_NONE, listener.mResult!!.errorCode)
    }

    /**
     * Test that multiple global listeners are all triggered, and that the request executes
     * correctly even though no listener is added directly to the request.
     */
    @Test
    fun testGlobalListener() {
        val listener1 = ResultListener()
        val listener2 = ResultListener()

        registerForAllProfilingResults(
            ApplicationProvider.getApplicationContext(),
            Executors.newSingleThreadExecutor(),
            listener1
        )
        registerForAllProfilingResults(
            ApplicationProvider.getApplicationContext(),
            Executors.newSingleThreadExecutor(),
            listener2
        )

        requestProfiling(
            ApplicationProvider.getApplicationContext(),
            HeapProfileRequestBuilder()
                .setBufferSizeKb(1000)
                .setDurationMs(5000)
                .setTrackJavaAllocations(true)
                .setSamplingIntervalBytes(100)
                .build(),
            null,
            null
        )

        waitForSignal(listener1.mAcceptedSignal)
        waitForSignal(listener2.mAcceptedSignal)

        assertNotNull(listener1.mResult)
        assertEquals(ProfilingResult.ERROR_NONE, listener1.mResult!!.errorCode)
        assertNotNull(listener2.mResult)
        assertEquals(ProfilingResult.ERROR_NONE, listener2.mResult!!.errorCode)
    }

    /** Test that global listener flows are triggered with each result. */
    @Test
    fun testGlobalListenerFlow() {
        var acceptedSignal = CountDownLatch(1)

        val resultList = ArrayList<ProfilingResult>()

        val flow = registerForAllProfilingResults(ApplicationProvider.getApplicationContext())
        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).launch {
            flow.collect { result ->
                resultList.add(result)
                acceptedSignal.countDown()
            }
        }

        // Wait for the other thread to actually register its listener
        runBlocking { delay(1000) }

        requestProfiling(
            ApplicationProvider.getApplicationContext(),
            HeapProfileRequestBuilder()
                .setBufferSizeKb(1000)
                .setDurationMs(5000)
                .setTrackJavaAllocations(true)
                .setSamplingIntervalBytes(100)
                .build(),
            null,
            null
        )

        waitForSignal(acceptedSignal)

        // Reset the latch
        acceptedSignal = CountDownLatch(1)

        requestProfiling(
            ApplicationProvider.getApplicationContext(),
            StackSamplingRequestBuilder()
                .setBufferSizeKb(1000)
                .setDurationMs(5000)
                .setSamplingFrequencyHz(100)
                .build(),
            null,
            null
        )

        waitForSignal(acceptedSignal)

        assertEquals(2, resultList.size)
        assertEquals(ProfilingResult.ERROR_NONE, resultList[0].errorCode)
        assertEquals(ProfilingResult.ERROR_NONE, resultList[1].errorCode)
    }

    /**
     * Test unregistering a global listener by adding two listeners and then removing one. Ensure
     * that the removed listener isn't triggered. Ensure that the remaining listener is triggered.
     */
    @Test
    fun testUnregisterGlobalListener() {
        val listener1 = ResultListener()
        val listener2 = ResultListener()

        registerForAllProfilingResults(
            ApplicationProvider.getApplicationContext(),
            Executors.newSingleThreadExecutor(),
            listener1
        )
        registerForAllProfilingResults(
            ApplicationProvider.getApplicationContext(),
            Executors.newSingleThreadExecutor(),
            listener2
        )

        requestProfiling(
            ApplicationProvider.getApplicationContext(),
            HeapProfileRequestBuilder()
                .setBufferSizeKb(1000)
                .setDurationMs(5000)
                .setSamplingIntervalBytes(4096L)
                .build(),
            null,
            null
        )

        unregisterForAllProfilingResults(ApplicationProvider.getApplicationContext(), listener2)

        waitForSignal(listener1.mAcceptedSignal)

        assertNotNull(listener1.mResult)
        assertEquals(ProfilingResult.ERROR_NONE, listener1.mResult!!.errorCode)
        assertNull(listener2.mResult)
    }

    /**
     * Test unregistering a global listener flow by adding two listener flows and then cancelling
     * one. Ensure that the removed flow doesn't collect a result. Ensure that the remaining flow
     * collects a result.
     */
    @Test
    fun testUnregisterGlobalListenerFlow() {
        val acceptedSignal = CountDownLatch(1)

        var profilingResultRegistered: ProfilingResult? = null

        val flowRegistered =
            registerForAllProfilingResults(ApplicationProvider.getApplicationContext())
        val flowUnregistered =
            registerForAllProfilingResults(ApplicationProvider.getApplicationContext())

        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).launch {
            flowRegistered.collect { result ->
                profilingResultRegistered = result
                acceptedSignal.countDown()
            }
        }
        val scopeToUnregister =
            CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).launch {
                flowUnregistered.collect { _ ->
                    // This flow should have been unregistered and not collecting, fail the test.
                    fail("Unregistered flow collected a result")
                }
            }

        // Wait for the other thread to actually register its listener
        runBlocking { delay(1000) }

        requestProfiling(
            ApplicationProvider.getApplicationContext(),
            HeapProfileRequestBuilder()
                .setBufferSizeKb(1000)
                .setDurationMs(10 * 1000)
                .setTrackJavaAllocations(true)
                .setSamplingIntervalBytes(100)
                .build(),
            null,
            null
        )

        // Schedule cancellation to occur after some short wait
        Handler(Looper.getMainLooper()).postDelayed({ scopeToUnregister.cancel() }, 1000L)

        waitForSignal(acceptedSignal)

        // Wait an extra second to confirm the other callback flow wasn't triggered.
        runBlocking { delay(1000) }

        assertNotNull(profilingResultRegistered)
        assertEquals(ProfilingResult.ERROR_NONE, profilingResultRegistered!!.errorCode)
    }

    /**
     * Wait for completed countdown signal to be received for up to 1 minute. Fails the test if it
     * times out waiting for the signal.
     */
    private fun waitForSignal(acceptedSignal: CountDownLatch) {
        val succeeded = acceptedSignal.await(1, TimeUnit.MINUTES)
        if (!succeeded) {
            fail("Test timed out waiting for callback")
        }
    }

    private class ResultListener : Consumer<ProfilingResult> {
        val mAcceptedSignal: CountDownLatch = CountDownLatch(1)
        var mResult: ProfilingResult? = null

        override fun accept(profilingResult: ProfilingResult) {
            mResult = profilingResult
            mAcceptedSignal.countDown()
        }
    }
}
