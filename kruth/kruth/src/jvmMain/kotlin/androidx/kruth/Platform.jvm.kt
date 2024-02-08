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

package androidx.kruth

import com.google.common.base.Throwables.throwIfUnchecked
import com.google.common.collect.Sets
import java.lang.reflect.InvocationTargetException

/**
 * Clear the stacktrace of a [Throwable]. Unlike [cleanStackTrace], which only cleans "redundant"
 * information, this completely empties the entire stack trace.
 */
internal actual fun Throwable.clearStackTrace() {
    stackTrace = emptyArray()
}

/**
 *  Cleans the stack trace on the given [Throwable], replacing the original stack trace
 *  stored on the instance (see [Throwable.stackTrace]).
 *
 *  Removes Truth stack frames from the top and JUnit framework and reflective call frames from
 *  the bottom. Collapses the frames for various frameworks in the middle of the trace as well.
 */
internal actual fun Throwable.cleanStackTrace() {
    StackTraceCleaner(this).clean(Sets.newIdentityHashSet<Throwable>())
}

/**
 * Returns an array containing all of the exceptions that were suppressed to deliver the given
 * exception. If suppressed exceptions are not supported (pre-Java 1.7), an empty array will be
 * returned.
 */
internal fun getSuppressed(throwable: Throwable): Array<Throwable> {
    return try {
        val getSuppressed = throwable::class.java.getMethod("getSuppressed")

        val result = requireNonNull(getSuppressed.invoke(throwable))
        @Suppress("UNCHECKED_CAST")
        result as Array<Throwable>
    } catch (e: NoSuchMethodException) {
        emptyArray<Throwable>()
    } catch (e: IllegalAccessException) {
        // We're calling a public method on a public class.
        throw newLinkageError(e)
    } catch (e: InvocationTargetException) {
        // Intentionally run into NPE if e.cause is null as this is Truth's behavior.
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        throwIfUnchecked(e.cause)
        // getSuppressed has no `throws` clause.
        throw newLinkageError(e)
    }
}

private fun newLinkageError(cause: Throwable): LinkageError {
    val error = LinkageError(cause.toString())
    error.initCause(cause)
    return error
}
