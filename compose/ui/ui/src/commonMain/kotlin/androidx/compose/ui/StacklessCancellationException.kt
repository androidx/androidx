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

package androidx.compose.ui

import kotlinx.coroutines.CancellationException

/**
 * A [CancellationException] that doesn't try to capture a stacktrace and so is cheaper than
 * creating a [CancellationException] directly.
 */
internal class StacklessCancellationException(message: String? = null) :
    CancellationException(message) {

    // Copied from Kotlin-internal JobCancellationException.
    override fun fillInStackTrace(): Throwable {
        // Prevent Android <= 6.0 bug, #1866
        stackTrace = emptyArray()
        /*
         * We don't want to have a stacktrace on every cancellation/close, parent job reference is
         * enough. Stacktrace of JCE is not needed most of the time (e.g., it is not logged)
         * and hurts performance.
         */
        return this
    }
}