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

package androidx.kruth

/**
 * Defines what to do when a check fails.
 *
 * This type does not appear directly in a fluent assertion chain, but you choose a
 * [FailureStrategy] by choosing which method to call at the beginning of the chain.
 *
 * For people extending Kruth
 *
 * Custom [FailureStrategy] implementations are unusual. If you think you need one,
 * consider these alternatives:
 *
 *   To test a custom subject, use [ExpectFailure].
 *   To create subjects for other objects related to your actual value (for chained assertions),
 *   use [Subject.check], which preserves the existing [FailureStrategy] and other context.
 *
 * When you really do need to create your own strategy, rather than expose your [FailureStrategy]
 * instance to users, expose a [StandardSubjectBuilder] instance using
 * [StandardSubjectBuilder.forCustomFailureStrategy].
 */
fun interface FailureStrategy {
    /**
     * Handles a failure. The parameter is an [AssertionError] or subclass thereof, and it
     * contains information about the failure, which may include:
     *
     *   message: [Throwable.message]
     *   cause: [Throwable.cause]
     *
     * We encourage implementations to record as much of this information as practical in the
     * exceptions they may throw or the other records they may make.
     */
    fun fail(failure: Error): Nothing
}
