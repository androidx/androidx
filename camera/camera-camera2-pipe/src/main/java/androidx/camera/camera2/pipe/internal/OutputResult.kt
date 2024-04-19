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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.camera.camera2.pipe.internal

import androidx.camera.camera2.pipe.OutputStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Inline value class that can be used in place of `Any` to represent either a valid output object
 * OR an [OutputStatus] indicating why the result is not available.
 */
@JvmInline
internal value class OutputResult<out T> private constructor(internal val result: Any?) {
    /**
     * Returns `true` if this instance represents a successful outcome.
     */
    val available: Boolean get() = !failure && result != null

    /**
     * Returns `true` if this instance represents a failed result.
     */
    val failure: Boolean get() = result is OutputStatus

    /**
     * Returns the value, if [from], else null.
     */
    @Suppress("UNCHECKED_CAST")
    inline val output: T?
        get() =
            when {
                available -> result as T
                else -> null
            }

    /**
     * If this OutputResult represents a failure, then return the [OutputStatus] associated with it,
     * otherwise report [OutputStatus.AVAILABLE] for successfully cases.
     */
    inline val status: OutputStatus
        get() =
            when {
                available -> OutputStatus.AVAILABLE
                result == null -> OutputStatus.UNAVAILABLE
                else -> result as OutputStatus
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    companion object {
        /**
         * Returns an instance that encapsulates the given [output] as successful value.
         */
        inline fun <T> from(output: T): OutputResult<T> =
            OutputResult(output as Any?)

        /**
         * Returns an instance that encapsulates the given OutputStatus as a failure.
         */
        inline fun <T> failure(failureReason: OutputStatus): OutputResult<T> =
            OutputResult(failureReason)

        /** Utility function to complete a CompletableDeferred with a successful [OutputResult]. */
        inline fun <T> CompletableDeferred<OutputResult<T>>.completeWithOutput(output: T): Boolean {
            return complete(from(output))
        }

        /** Utility function to complete a CompletableDeferred with a [OutputStatus] failure */
        inline fun <T> CompletableDeferred<OutputResult<T>>.completeWithFailure(
            status: OutputStatus
        ): Boolean {
            return complete(failure(status))
        }

        /**
         * For a [Deferred] object that contains an OutputResult, determine the status based on the
         * state of the [Deferred] or from the actual object if this status has been completed.
         */
        inline fun <T> Deferred<OutputResult<T>>.outputStatus(): OutputStatus {
            return if (!isCompleted) {
                // If the result is not completed, then this Output is in a PENDING state.
                OutputStatus.PENDING
            } else if (isCancelled) {
                // If the result was canceled for any reason, then this Output is, and will not, be
                // available.
                OutputStatus.UNAVAILABLE
            } else {
                // If we reach here, the result is A) completed, and B) not canceled. read the
                // status from the result and return it.
                getCompleted().status
            }
        }

        /**
         * Get the output from this [Deferred], if available, or null.
         */
        inline fun <T> Deferred<OutputResult<T>>.outputOrNull(): T? {
            if (isCompleted && !isCancelled) {
                return getCompleted().output
            }
            return null
        }
    }
}
