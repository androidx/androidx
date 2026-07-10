/*
 * Copyright 2025 The Android Open Source Project
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

import kotlin.Unit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestResult

@OptIn(DelicateCoroutinesApi::class)
actual fun wrapRunTest(test: suspend WrapRunTestScope.() -> Unit): TestResult {
    return GlobalScope.promise { test(WrapRunTestScopeImpl) }.unsafeCast<TestResult>()
}

private object WrapRunTestScopeImpl : WrapRunTestScope {
    override suspend fun TestResult.awaitCompletion() {
        suspendCancellableCoroutine { cont: CancellableContinuation<Unit> ->
            then(onFulfilled = { cont.resume(Unit) }, onRejected = { cont.resumeWithException(it) })
        }
    }
}
