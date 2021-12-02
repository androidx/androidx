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

import androidx.room.compiler.processing.ExperimentalProcessingApi
import org.junit.AssumptionViolatedException
import org.junit.runners.Parameterized

@OptIn(ExperimentalProcessingApi::class)
class TestRunner(
    private val name: String,
    private val runner: (
        sources: List<Source>,
        options: Map<String, String>,
        handlers: List<(XTestInvocation) -> Unit>
    ) -> Unit
) {
    operator fun invoke(handlers: List<(XTestInvocation) -> Unit>) =
        runner(emptyList(), emptyMap(), handlers)

    operator fun invoke(handler: (XTestInvocation) -> Unit) =
        runner(emptyList(), emptyMap(), listOf(handler))

    operator fun invoke(
        sources: List<Source>,
        options: Map<String, String> = emptyMap(),
        handler: (XTestInvocation) -> Unit
    ) = runner(sources, options, listOf(handler))

    override fun toString() = name
    fun assumeCanCompileKotlin() {
        if (name == "java") {
            throw AssumptionViolatedException("cannot compile kotlin sources")
        }
    }
}

/**
 * Helper test runner class to run tests for each backend in isolation
 */
abstract class MultiBackendTest {
    companion object {
        @OptIn(ExperimentalProcessingApi::class)
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun runners(): List<TestRunner> = listOfNotNull(
            TestRunner("java") { sources, options, handlers ->
                runJavaProcessorTest(sources = sources, options = options, handlers = handlers)
            },
            TestRunner("kapt") { sources, options, handlers ->
                runKaptTest(sources = sources, options = options, handlers = handlers)
            },
            if (CompilationTestCapabilities.canTestWithKsp) {
                TestRunner("ksp") { sources, options, handlers ->
                    runKspTest(sources = sources, options = options, handlers = handlers)
                }
            } else {
                null
            }
        )
    }
}
