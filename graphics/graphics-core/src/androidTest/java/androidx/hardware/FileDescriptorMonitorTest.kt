/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.hardware

import androidx.graphics.utils.HandlerThreadExecutor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class FileDescriptorMonitorTest {

    @Test
    fun testStartMonitoringInvokesCallback() {
        val monitor = FileDescriptorMonitor(scheduleMillis = 10)
        val cleanupLatch = CountDownLatch(1)
        monitor.addCleanupCallback { cleanupLatch.countDown() }
        monitor.startMonitoring()
        try {
            assertTrue(cleanupLatch.await(3000, TimeUnit.MILLISECONDS))
        } finally {
            monitor.stopMonitoring()
        }
    }

    @Test
    fun testIsMonitoring() {
        val monitor = FileDescriptorMonitor()
        try {
            assertFalse(monitor.isMonitoring)
            monitor.startMonitoring()
            assertTrue(monitor.isMonitoring)
        } finally {
            monitor.stopMonitoring()
        }
    }

    @Test
    fun testStopMonitoringTearsDownExecutorAfterCancel() {
        val testHandlerThreadExecutor = HandlerThreadExecutor("test")
        val monitor = FileDescriptorMonitor(testHandlerThreadExecutor, manageExecutor = true)
        monitor.startMonitoring()
        monitor.stopMonitoring(true)
        assertFalse(testHandlerThreadExecutor.isRunning)
    }

    @Test
    fun testStopMonitoringTearsDownExecutorAfterPendingRequest() {
        val testHandlerThreadExecutor = HandlerThreadExecutor("test")
        val monitor =
            FileDescriptorMonitor(
                testHandlerThreadExecutor,
                scheduleMillis = 200,
                manageExecutor = true
            )
        val latch = CountDownLatch(1)
        monitor.addCleanupCallback { latch.countDown() }
        monitor.startMonitoring()
        monitor.stopMonitoring(false)
        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS))
        assertFalse(testHandlerThreadExecutor.isRunning)
    }

    @Test
    fun testStopMonitoringDoesNotTearDownExecutor() {
        val testHandlerThreadExecutor = HandlerThreadExecutor("test")
        val monitor = FileDescriptorMonitor(testHandlerThreadExecutor)
        monitor.startMonitoring()
        monitor.stopMonitoring()
        assertTrue(testHandlerThreadExecutor.isRunning)
        testHandlerThreadExecutor.quit()
    }

    @Test
    fun testCancelPendingDoesNotInvokeCleanupTask() {
        val monitor = FileDescriptorMonitor(scheduleMillis = 500)
        val countDownLatch = CountDownLatch(1)
        monitor.addCleanupCallback { countDownLatch.countDown() }
        monitor.startMonitoring()
        monitor.stopMonitoring(true)
        assertFalse(countDownLatch.await(1000, TimeUnit.MILLISECONDS))
    }

    @Test
    fun testCancelPendingInvokesInFlightCleanupTask() {
        val monitor = FileDescriptorMonitor(scheduleMillis = 500)
        val countDownLatch = CountDownLatch(1)
        monitor.addCleanupCallback { countDownLatch.countDown() }
        monitor.startMonitoring()
        monitor.stopMonitoring(false)
        assertTrue(countDownLatch.await(1000, TimeUnit.MILLISECONDS))
    }
}
