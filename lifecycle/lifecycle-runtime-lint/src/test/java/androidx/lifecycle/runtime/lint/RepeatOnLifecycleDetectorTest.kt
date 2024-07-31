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

package androidx.lifecycle.runtime.lint

import androidx.lifecycle.lint.RepeatOnLifecycleDetector
import androidx.lifecycle.runtime.lint.stubs.REPEAT_ON_LIFECYCLE_STUBS
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RepeatOnLifecycleDetectorTest(val config: TestConfig) {

    data class TestConfig(
        val isActivity: Boolean,
        val lifecycleMethod: String,
        val apiMethod: String,
        val methodBody: String,
        val helperMethodBody: String
    )

    companion object {
        private fun generateTestConfigs() =
            listOf(true, false).flatMap { activity ->
                listOf(
                        activity to (if (activity) "onCreate" else "onCreateView"),
                        activity to "onStart",
                        activity to "onResume"
                    )
                    .flatMap { (activity, lifecycleMethod) ->
                        listOf(
                            Triple(activity, lifecycleMethod, "repeatOnLifecycle"),
                            Triple(activity, lifecycleMethod, "lifecycle.repeatOnLifecycle"),
                        )
                    }
                    .flatMap { (activity, lifecycleMethod, apiMethod) ->
                        listOf(
                            // apiMethod is called directly from the lifecycleMethod
                            TestConfig(
                                activity,
                                lifecycleMethod,
                                apiMethod,
                                methodBody =
                                    """
                                GlobalScope.launch {
                                    $apiMethod(Lifecycle.State.STARTED) { }
                                }
                            """
                                        .trimIndent(),
                                helperMethodBody = ""
                            ),
                            // apiMethod is called from another function called from the
                            // lifecycleMethod
                            TestConfig(
                                activity,
                                lifecycleMethod,
                                apiMethod,
                                methodBody =
                                    """
                                GlobalScope.launch {
                                    helperMethod()
                                }
                            """
                                        .trimIndent(),
                                // The helper method body depends on the apiMethod we're testing
                                helperMethodBody =
                                    if (apiMethod == "lifecycle.repeatOnLifecycle") {
                                        "lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { }"
                                    } else {
                                        "repeatOnLifecycle(Lifecycle.State.STARTED) { }"
                                    }
                            )
                        )
                    }
            }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = generateTestConfigs()
    }

    @Test
    fun basicTest() {
        val fileToAdd =
            if (config.isActivity) {
                activityTemplate(config.lifecycleMethod, config.methodBody, config.helperMethodBody)
            } else {
                fragmentTemplate(config.lifecycleMethod, config.methodBody, config.helperMethodBody)
            }
        val testLintResult = check(fileToAdd)
        if (
            (config.isActivity && config.lifecycleMethod == "onCreate") ||
                (!config.isActivity) && config.lifecycleMethod == "onCreateView"
        ) {
            testLintResult.expectClean()
        } else {
            testLintResult.expect(error())
        }
    }

    private fun check(fileToAdd: String): TestLintResult {
        return TestLintTask.lint()
            .files(*REPEAT_ON_LIFECYCLE_STUBS, TestFiles.kt(fileToAdd))
            .issues(RepeatOnLifecycleDetector.ISSUE)
            .skipTestModes(TestMode.JVM_OVERLOADS)
            .run()
    }

    private fun error(): String {
        val className = if (config.isActivity) "MyActivity" else "MyFragment"
        val (error, curlyCharacters, indent, wrongLine) =
            if (config.methodBody.contains("helperMethod")) {
                Error(
                    error = "${config.helperMethodBody} // config.helperMethodBody",
                    curlyCharacters = config.helperMethodBody.length,
                    indent = 8,
                    wrongLine = "18"
                )
            } else {
                val error = "${config.apiMethod}(Lifecycle.State.STARTED) { }"
                Error(error = error, curlyCharacters = error.length, indent = 4, wrongLine = "13")
            }

        return """
            src/foo/$className.kt:$wrongLine: Error: Wrong usage of repeatOnLifecycle from $className.${config.lifecycleMethod}. [RepeatOnLifecycleWrongUsage]
            ${" ".repeat(indent)}$error
            ${" ".repeat(indent)}${"~".repeat(curlyCharacters)}
            1 errors, 0 warnings
        """
            .trimIndent()
        /* ERROR EXAMPLE:
           src/foo/MyActivity.kt:13: Error: Wrong usage of repeatOnLifecycle from MyActivity.onStart. [RepeatOnLifecycleWrongUsage]
               repeatOnLifecycle(Lifecycle.State.STARTED) { }
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
           1 errors, 0 warnings
        */

    }

    private fun fragmentTemplate(
        lifecycleMethod: String,
        methodBody: String,
        helperMethodBody: String
    ) = FRAGMENT_TEMPLATE.format(lifecycleMethod, methodBody, helperMethodBody)

    private fun activityTemplate(
        lifecycleMethod: String,
        methodBody: String,
        helperMethodBody: String
    ) = ACTIVITY_TEMPLATE.format(lifecycleMethod, methodBody, helperMethodBody)

    private val FRAGMENT_TEMPLATE =
        """
        package foo

        import androidx.lifecycle.Lifecycle
        import androidx.lifecycle.LifecycleOwner
        import androidx.lifecycle.repeatOnLifecycle
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.GlobalScope
        import androidx.fragment.app.Fragment

        class MyFragment : Fragment() {
            fun %s() { // config.lifecycleMethod
                %s // config.methodBody
            }

            suspend fun helperMethod() {
                %s // config.helperMethodBody
            }
        }
    """
            .trimIndent()

    private val ACTIVITY_TEMPLATE =
        """
        package foo

        import androidx.core.app.ComponentActivity
        import androidx.lifecycle.Lifecycle
        import androidx.lifecycle.LifecycleOwner
        import androidx.lifecycle.repeatOnLifecycle
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.GlobalScope

        class MyActivity : ComponentActivity() {
            fun %s() { // config.lifecycleMethod
                %s // config.methodBody
            }

            suspend fun helperMethod() {
                %s // config.helperMethodBody
            }
        }
    """
            .trimIndent()
}

private data class Error(
    val error: String,
    val curlyCharacters: Int,
    val indent: Int,
    val wrongLine: String
)
