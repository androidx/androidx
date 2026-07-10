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

import kotlinx.coroutines.test.TestResult

// It's required that a test method returns the TestResult right away:
// https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/run-test.html
// wrapRunTest is useful when a @Test can't return TestResult right away due to its structure or
// other reasons.
// It's a responsibility of the test body to call TestResult.awaitCompletion explicitly to make sure
// that a TestResult is not ignored.
// Example:
// @Test fun testSomething() = wrapRunTest { compositionTest { }.awaitCompletion() }
expect fun wrapRunTest(test: suspend WrapRunTestScope.() -> Unit): TestResult

interface WrapRunTestScope {
    suspend fun TestResult.awaitCompletion()
}
