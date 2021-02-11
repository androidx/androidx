/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.util

import org.junit.runners.Parameterized

class TestRunner(
    private val name: String,
    private val runner: (List<(XTestInvocation) -> Unit>) -> Unit
) {
    operator fun invoke(handlers: List<(XTestInvocation) -> Unit>) = runner(handlers)
    operator fun invoke(handler: (XTestInvocation) -> Unit) = runner(listOf(handler))
    override fun toString() = name
}

/**
 * Helper test runner class to run tests for each backend in isolation
 */
abstract class MultiBackendTest {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun runners(): List<TestRunner> = listOfNotNull(
            TestRunner("java") {
                runJavaProcessorTest(sources = emptyList(), handlers = it)
            },
            TestRunner("kapt") {
                runKaptTest(sources = emptyList(), handlers = it)
            },
            if (CompilationTestCapabilities.canTestWithKsp) {
                TestRunner("ksp") {
                    runKspTest(sources = emptyList(), handlers = it)
                }
            } else {
                null
            }
        )
    }
}
