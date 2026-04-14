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

@file:JvmName("Throwables")

package androidx.tracing

/**
 * Records an exceptional event in a trace.
 *
 * @param category The [String] category. It's useful to categorize [TraceEvent]s, so that they can
 *   be filtered if necessary using the appropriate trace configuration.
 * @param name The name of the trace section.
 * @param throwable The exception thrown by a block of code.
 */
public fun Tracer.recordException(category: String, name: String, throwable: Throwable): Nothing {
    instant(category = category, name = name, metadataBlock = { addThrowable(throwable) })
    // Rethrow the exception so the caller knows something bad happened.
    throw throwable
}

/** Propagates a [Throwable] as event metadata when tracing. */
@Suppress("NOTHING_TO_INLINE")
internal inline fun EventMetadata.addThrowable(throwable: Throwable) {
    throwable.stackTrace.forEach { element ->
        addCallStackEntry(
            name = "${element.className}.${element.methodName}",
            sourceFile = element.fileName,
            lineNumber = element.lineNumber,
        )
    }
}
