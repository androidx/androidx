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

import androidx.annotation.RestrictTo
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.Collections
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages the lifecycle of objects by hooking into the JVM Garbage Collector using PhantomReference
 * objects.
 *
 * Provides equivalent functionality to `java.lang.ref.Cleaner` for backwards compatibility with API
 * levels < 33.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ReferenceCleaner {

    /**
     * Registers an object and a cleaning action to run when the object becomes phantom reachable.
     *
     * The [action] runnable must NOT hold a strong reference to the monitored [obj]. Capturing the
     * object inside the runnable will create a strong reference cycle, preventing the object from
     * ever being garbage collected. Use static classes or explicitly capture only the inner
     * resources needing cleanup.
     *
     * @param obj The object to monitor.
     * @param executor The executor to run the cleaning action on.
     * @param action The cleaning action to run.
     */
    public abstract fun register(obj: Any, executor: Executor, action: Runnable)

    public companion object {
        private val sInstance: ReferenceCleaner by lazy { ReferenceCleanerImpl() }

        /** Returns the singleton instance of [ReferenceCleaner]. */
        @JvmStatic public fun getInstance(): ReferenceCleaner = sInstance
    }
}

/** A [Runnable] that executes a given cleanup function at most once. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class CleanupAction(private val cleanupFunc: () -> Unit) : Runnable {
    private val isRun = AtomicBoolean(false)

    override fun run() {
        if (isRun.getAndSet(true)) {
            return
        }
        cleanupFunc()
    }
}

/** Private implementation of [ReferenceCleaner]. */
private class ReferenceCleanerImpl : ReferenceCleaner() {
    private val queue = ReferenceQueue<Any>()
    private val xrPhantomReferences =
        Collections.synchronizedSet(mutableSetOf<XrPhantomReference>())

    init {
        val thread = Thread({ processQueue() }, "reference_cleaner_thread")
        thread.isDaemon = true
        thread.start()
    }

    override fun register(obj: Any, executor: Executor, action: Runnable) {
        xrPhantomReferences.add(XrPhantomReference(obj, queue, executor, action))
    }

    private fun processQueue() {
        while (true) {
            try {
                val ref = queue.remove() as XrPhantomReference
                xrPhantomReferences.remove(ref)
                ref.cleanup()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
    }

    private class XrPhantomReference(
        referent: Any,
        queue: ReferenceQueue<in Any>,
        val executor: Executor,
        val action: Runnable,
    ) : PhantomReference<Any>(referent, queue) {
        fun cleanup() {
            try {
                executor.execute(action)
            } catch (_: Exception) {
                // Ignore executor exceptions (e.g., if the executor is shut down).
            }
        }
    }
}
