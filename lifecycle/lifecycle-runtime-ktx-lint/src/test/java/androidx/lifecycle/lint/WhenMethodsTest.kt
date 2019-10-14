/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle.lint

import androidx.lifecycle.lint.stubs.COROUTINES_STUB
import androidx.lifecycle.lint.stubs.LIFECYCLE_STUB
import androidx.lifecycle.lint.stubs.VIEW_STUB
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class WhenMethodsTest(val config: TestConfig) {

    data class TestConfig(
        val receiver: String,
        val methodName: String
    ) {
        override fun toString() = "$receiver.$methodName"
    }

    companion object {
        private fun generateTestConfigs() =
            listOf("whenCreated", "whenStarted", "whenResumed").flatMap {
                listOf(TestConfig("owner", it), TestConfig("owner.lifecycle", it))
            }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = generateTestConfigs()
    }

    private fun check(body: String): TestLintResult {
        return TestLintTask.lint()
            .files(VIEW_STUB, LIFECYCLE_STUB, COROUTINES_STUB, TestFiles.kt(template(body)))
            .allowMissingSdk(true)
            .issues(LifecycleWhenChecks.ISSUE)
            .run()
    }

    private val TEMPLATE = """
        package foo

        import androidx.lifecycle.Lifecycle
        import androidx.lifecycle.LifecycleOwner
        import androidx.lifecycle.whenCreated
        import androidx.lifecycle.whenStarted
        import androidx.lifecycle.whenResumed
        import android.view.FooView
        import kotlinx.coroutines.GlobalScope

        suspend fun foo(owner: LifecycleOwner, view: FooView) {
            %s
        }

        suspend fun suspendingFun() {
        }
    """.trimIndent()

    private fun template(body: String) = TEMPLATE.format(body)

    private fun error(methodName: String): String {
        val errorMessage = errorMessage(methodName).replace("`", "")
        return """
            src/foo/test.kt:16: Error: $errorMessage [UnsafeLifecycleWhenUsage]
                    view.foo()
                    ~~~~~~~~~~
            1 errors, 0 warnings
        """.trimIndent()
    }

    @Test
    fun basicTryCatch() {
        val input = """
            ${config.receiver}.${config.methodName} {
                try {
                    suspendingFun()
                } finally {
                    view.foo()
                }
            }
        """.trimIndent()
        check(input).expect(error(config.methodName))
    }
}