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

package androidx.tracing

/**
 * Wrap the specified [block] in calls to [Trace.beginSection] (with the supplied [label])
 * and [Trace.endSection].
 *
 * @param label A name of the code section to appear in the trace.
 * @param block A block of code which is being traced.
 */
inline fun <T> trace(label: String, crossinline block: () -> T): T {
    try {
        Trace.beginSection(label)
        return block()
    } finally {
        Trace.endSection()
    }
}

/**
 * Wrap the specified [block] in calls to [Trace.beginAsyncSection] (with the supplied [methodName]
 * and [cookie]) and [Trace.endAsyncSection].
 *
 * @param methodName The method name to appear in the trace.
 * @param cookie Unique identifier for distinguishing simultaneous events
 */
suspend inline fun <T> traceAsync(
    methodName: String,
    cookie: Int,
    crossinline block: suspend () -> T
): T {
    try {
        Trace.beginAsyncSection(methodName, cookie)
        return block()
    } finally {
        Trace.endAsyncSection(methodName, cookie)
    }
}
