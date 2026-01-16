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

package androidx.compose.runtime.platform

internal expect class SynchronizedObject

/**
 * Returns [ref] as a [SynchronizedObject] on platforms where [Any] is a valid [SynchronizedObject],
 * or a new [SynchronizedObject] instance if [ref] is null or this is not supported on the current
 * platform.
 */
internal expect inline fun makeSynchronizedObject(ref: Any? = null): SynchronizedObject

@PublishedApi
@Suppress("LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING") // b/446705238
internal expect inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R

/**
 * An implementation of the "monitor" synchronization construct.
 *
 * This class should only be used when [wait] and [notifyAll] are needed, because [synchronized]
 * blocks that synchronize on [SynchronizedObject]s are more performant than ones that synchronize
 * on [Monitor]s.
 *
 * Every method of this class is a no-op on Kotlin/JS and Kotlin/Wasm because they are
 * single-threaded.
 */
internal expect class Monitor {
    /**
     * Causes the current thread to wait until another thread invokes the [notifyAll] method on this
     * object.
     *
     * This method should only be called by a thread that is the owner of this monitor. A thread
     * becomes the owner of a monitor by executing the body of a [synchronized] statement that
     * synchronizes on the monitor. Calling this method relases the monitor. The monitor will be
     * re-acquired before this thread resumes execution.
     */
    fun wait()

    /**
     * Wakes up all threads that are waiting on this monitor.
     *
     * This method should only be called by a thread that is the owner of this monitor. A thread
     * becomes the owner of a monitor by executing the body of a [synchronized] statement that
     * synchronizes on the monitor.
     */
    fun notifyAll()
}

/**
 * Returns [ref] as a [Monitor] on platforms where [Any] is a valid [Monitor], or a new [Monitor]
 * instance if [ref] is null or [ref] cannot be cast to [Monitor] on the current platform.
 */
internal expect inline fun makeMonitor(ref: Any? = null): Monitor

/** Sequentially acquires [monitor], executes [block], and releases [monitor]. */
internal expect inline fun <R> synchronized(monitor: Monitor, block: () -> R): R
