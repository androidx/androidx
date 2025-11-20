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

package androidx.compose.runtime

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope

private const val DEFAULT_DISPATCH_TIMEOUT_MS = 60_000L

internal fun runTest(
    context: CoroutineContext = EmptyCoroutineContext,
    dispatchTimeoutMs: Long = DEFAULT_DISPATCH_TIMEOUT_MS,
    timeoutMs: Long? = null,
    expected: KClass<out Throwable>? = null,
    testBody: suspend TestScope.() -> Unit,
): TestResult =
    kotlinx.coroutines.test.runTest(context, timeout = dispatchTimeoutMs.milliseconds) {
        val testScope = this
        if (timeoutMs == null) {
            runTestImpl(expected) { testBody() }
        } else {
            testWithTimeout(timeoutMs) {
                testBody(testScope)
                runTestImpl(expected) { testBody(testScope) }
            }
        }
    }

internal expect suspend fun testWithTimeout(
    timeoutMs: Long,
    block: suspend CoroutineScope.() -> Unit,
)

private inline fun runTestImpl(expected: KClass<out Throwable>? = null, block: () -> Unit) {
    if (expected != null) {
        var exception: Throwable? = null
        try {
            block()
        } catch (e: Throwable) {
            exception = e
        }
        assertTrue(
            exception != null && expected.isInstance(exception),
            "Expected $expected to be thrown",
        )
    } else {
        block()
    }
}
