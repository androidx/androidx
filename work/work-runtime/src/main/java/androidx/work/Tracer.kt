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

package androidx.work

import androidx.annotation.RestrictTo

/** Sets up trace spans when a {@link WorkRequest} is setup for execution by [WorkManager]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Tracer {
    /** Checks whether or not tracing is currently enabled. */
    fun isEnabled(): Boolean

    /**
     * Writes a trace message, with the provided [label] to indicate that a given section of code
     * has begun.
     */
    fun beginSection(label: String)

    /** Writes a trace message to indicate that a given section of code has ended. */
    fun endSection()

    /**
     * Writes a trace span to indicate that a given section of code has begun.
     *
     * @see [androidx.tracing.Trace.beginAsyncSection]
     */
    fun beginAsyncSection(methodName: String, cookie: Int)

    /**
     * Writes a trace span to indicate that a given section of code has ended.
     *
     * @see [androidx.tracing.Trace.endAsyncSection]
     */
    fun endAsyncSection(methodName: String, cookie: Int)
}

/** A helper that can insert trace sections around a [block] of code. */
internal inline fun <T> Tracer.traced(label: String, block: () -> T): T {
    val enabled = isEnabled()
    try {
        if (enabled) {
            beginSection(label)
        }
        return block()
    } finally {
        if (enabled) {
            endSection()
        }
    }
}
