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

package androidx.compose.runtime.tooling

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.InternalComposer
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.coroutines.CoroutineContext

/**
 * Used to attach a compose stack trace to a throwable based on a location of compose node in
 * composition. This context is expected to be used by custom node implementations to attach
 * diagnostic compose stack traces to exceptions in passes that are not handled by the Compose
 * runtime (example: measure / layout / draw in Compose UI).
 *
 * Compose runtime automatically appends information about exceptions that happen in composition and
 * effects.
 */
public val LocalCompositionErrorContext: CompositionLocal<CompositionErrorContext?> =
    staticCompositionLocalOf {
        null
    }

/**
 * Provides a way to attach a compose stack trace to a throwable based on a location of compose node
 * in composition. This context is expected to be used by custom node implementations to attach
 * diagnostic compose stack traces to exceptions in passes that are not handled by the Compose
 * runtime (example: measure / layout / draw in Compose UI).
 *
 * Compose runtime automatically appends information about exceptions that happen in composition and
 * effects.
 */
public sealed interface CompositionErrorContext {
    /**
     * Attaches a Compose stack trace to a throwable as a suppressed [DiagnosticComposeException].
     * Has no effect if:
     * - Throwable already contains a suppressed [DiagnosticComposeException]
     * - [composeNode] is not found in composition
     * - composition contains no source information (e.g. in minified builds)
     *
     * @param composeNode closest node to where exception was originally thrown
     * @return true if the exception was attached, false otherwise
     * @receiver throwable to attach a compose stack trace to
     */
    public fun Throwable.attachComposeStackTrace(composeNode: Any): Boolean
}

internal class CompositionErrorContextImpl(private val composer: InternalComposer) :
    CompositionErrorContext, OperationErrorContext, CoroutineContext.Element {
    override fun Throwable.attachComposeStackTrace(composeNode: Any): Boolean =
        tryAttachComposeStackTrace {
            composer.stackTraceForValue(composeNode)
        }

    override fun buildStackTrace(currentOffset: Int?): List<ComposeStackTraceFrame> =
        composer.parentStackTrace()

    companion object Key : CoroutineContext.Key<CompositionErrorContextImpl> {
        override fun toString(): String = "CompositionErrorContext"
    }

    override val key: CoroutineContext.Key<*>
        get() = Key
}
