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

package androidx.xr.scenecore.runtime

import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceCleanerTest {

    @Test
    fun cleanerExecutesActionOnGarbageCollection() {
        val cleaner = ReferenceCleaner.getInstance()
        val executor = Executors.newSingleThreadExecutor()
        val latch = CountDownLatch(1)

        var obj: Any? = Any()

        // Use a weak reference to confirm when the object is actually collected
        // to help diagnose test flakiness if it occurs.
        val weakRef = WeakReference(obj)

        cleaner.register(obj!!, executor) { latch.countDown() }

        // Nullify the strong reference
        obj = null

        // Force GC to trigger the phantom reference queue
        var gcAttempts = 0
        while (weakRef.get() != null && gcAttempts < 10) {
            System.gc()
            System.runFinalization()

            // Wait up to 100ms for the latch to decrease before forcing another GC.
            // Using latch.await is safe from the `BanThreadSleep` lint check.
            if (latch.await(100, TimeUnit.MILLISECONDS)) {
                break
            }
            gcAttempts++
        }

        // Wait for the background thread and executor to finish processing the cleanup action
        val actionExecuted = latch.await(5, TimeUnit.SECONDS)

        assertTrue("Cleanup action was not executed by ReferenceCleaner.", actionExecuted)

        executor.shutdown()
    }

    @Test
    fun cleanerDoesNotExecuteActionWhenStrongReferenceIsHeld() {
        val cleaner = ReferenceCleaner.getInstance()
        val executor = Executors.newSingleThreadExecutor()
        val latch = CountDownLatch(1)

        val obj = Any()
        val weakRef = WeakReference(obj)

        cleaner.register(obj, executor) { latch.countDown() }

        // Strong reference 'obj' is still held here.
        // Force GC to attempt to collect the object.
        System.gc()
        System.runFinalization()

        // Wait a bit to ensure the cleaner had a chance to run if it were going to.
        val actionExecuted = latch.await(500, TimeUnit.MILLISECONDS)

        assertTrue("Weak reference should still be alive.", weakRef.get() != null)
        assertTrue("Cleanup action should not have been executed.", !actionExecuted)

        executor.shutdown()
    }

    @Test
    fun cleanerContinuesProcessingAfterExecutorRejectsTask() {
        val cleaner = ReferenceCleaner.getInstance()

        // Create an executor that immediately rejects tasks
        val rejectedExecutor = Executor { throw RejectedExecutionException("Rejected") }
        val validExecutor = Executors.newSingleThreadExecutor()
        val latch = CountDownLatch(1)

        var obj1: Any? = Any()
        var obj2: Any? = Any()
        val weakRef1 = WeakReference(obj1)
        val weakRef2 = WeakReference(obj2)

        // Register first object with the failing executor
        cleaner.register(obj1!!, rejectedExecutor) { /* This will never execute */ }

        // Register second object with a valid executor
        cleaner.register(obj2!!, validExecutor) { latch.countDown() }

        // Nullify strong references
        obj1 = null
        obj2 = null

        // Force GC to trigger the phantom reference queue
        var gcAttempts = 0
        while ((weakRef1.get() != null || weakRef2.get() != null) && gcAttempts < 10) {
            System.gc()
            System.runFinalization()
            if (latch.await(100, TimeUnit.MILLISECONDS)) break
            gcAttempts++
        }

        // Wait for the background thread and executor to finish processing
        val actionExecuted = latch.await(5, TimeUnit.SECONDS)

        assertTrue(
            "Cleaner thread should survive an executor exception and process subsequent references.",
            actionExecuted,
        )

        (validExecutor as java.util.concurrent.ExecutorService).shutdown()
    }

    @Test
    fun cleanerExecutesMultipleActionsForSameObject() {
        val cleaner = ReferenceCleaner.getInstance()
        val executor = Executors.newSingleThreadExecutor()
        val latch1 = CountDownLatch(1)
        val latch2 = CountDownLatch(1)

        var obj: Any? = Any()
        val weakRef = WeakReference(obj)

        cleaner.register(obj!!, executor) { latch1.countDown() }
        cleaner.register(obj, executor) { latch2.countDown() }

        obj = null

        var gcAttempts = 0
        while (weakRef.get() != null && gcAttempts < 10) {
            System.gc()
            System.runFinalization()
            if (
                latch1.await(100, TimeUnit.MILLISECONDS) && latch2.await(100, TimeUnit.MILLISECONDS)
            )
                break
            gcAttempts++
        }

        assertTrue("First action was not executed.", latch1.await(5, TimeUnit.SECONDS))
        assertTrue("Second action was not executed.", latch2.await(5, TimeUnit.SECONDS))

        executor.shutdown()
    }

    @Test
    fun cleanerExecutesActionsForMultipleObjects() {
        val cleaner = ReferenceCleaner.getInstance()
        val executor = Executors.newSingleThreadExecutor()
        val latch1 = CountDownLatch(1)
        val latch2 = CountDownLatch(1)

        var obj1: Any? = Any()
        var obj2: Any? = Any()
        val weakRef1 = WeakReference(obj1)
        val weakRef2 = WeakReference(obj2)

        cleaner.register(obj1!!, executor) { latch1.countDown() }
        cleaner.register(obj2!!, executor) { latch2.countDown() }

        obj1 = null
        obj2 = null

        var gcAttempts = 0
        while ((weakRef1.get() != null || weakRef2.get() != null) && gcAttempts < 10) {
            System.gc()
            System.runFinalization()
            if (
                latch1.await(100, TimeUnit.MILLISECONDS) && latch2.await(100, TimeUnit.MILLISECONDS)
            )
                break
            gcAttempts++
        }

        assertTrue("Action for obj1 was not executed.", latch1.await(5, TimeUnit.SECONDS))
        assertTrue("Action for obj2 was not executed.", latch2.await(5, TimeUnit.SECONDS))

        executor.shutdown()
    }

    @Test
    fun cleanerHandlesActionThrowingException() {
        val cleaner = ReferenceCleaner.getInstance()
        val executor = Executors.newSingleThreadExecutor()
        val latch = CountDownLatch(1)

        var obj1: Any? = Any()
        var obj2: Any? = Any()
        val weakRef1 = WeakReference(obj1)
        val weakRef2 = WeakReference(obj2)

        // First action throws an exception
        cleaner.register(obj1!!, executor) { throw RuntimeException("Action failed") }

        // Second action should still run
        cleaner.register(obj2!!, executor) { latch.countDown() }

        obj1 = null
        obj2 = null

        var gcAttempts = 0
        while ((weakRef1.get() != null || weakRef2.get() != null) && gcAttempts < 10) {
            System.gc()
            System.runFinalization()
            if (latch.await(100, TimeUnit.MILLISECONDS)) break
            gcAttempts++
        }

        assertTrue(
            "Subsequent action should execute even if previous action failed.",
            latch.await(5, TimeUnit.SECONDS),
        )

        executor.shutdown()
    }

    @Test
    fun cleanupActionExecutesAtMostOnce() {
        val counter = AtomicInteger(0)
        val cleanupAction = CleanupAction { counter.incrementAndGet() }

        cleanupAction.run()
        cleanupAction.run()
        cleanupAction.run()

        assertEquals("Cleanup action should have executed exactly once.", 1, counter.get())
    }
}
