/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LazyDelegateDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = LazyDelegateDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(LazyDelegateDetector.ISSUE)

    @Test
    fun lazyPropertyWithoutMode_reportsError() {
        lint()
            .files(
                kotlin(
                    """
                    package androidx.compose.lint.test

                    val topLevelLazy by lazy { "test" }

                    class TestClass {
                        val memberLazy by lazy { 42 }
                        val qualifiedLazy by kotlin.lazy { "qualified" }

                        fun testMethod() {
                            val localLazy by lazy { "local" }
                        }
                    }
                    """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test/TestClass.kt:4: Error: Using by lazy has high overhead. Instead, either always execute the initialization or use a nullable or other invalid value by default to detect that the value hasn't been initialized. If lazy is intended, then the mode parameter should always be passed. [LazyDelegate]
                    val topLevelLazy by lazy { "test" }
                                     ~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/TestClass.kt:7: Error: Using by lazy has high overhead. Instead, either always execute the initialization or use a nullable or other invalid value by default to detect that the value hasn't been initialized. If lazy is intended, then the mode parameter should always be passed. [LazyDelegate]
                        val memberLazy by lazy { 42 }
                                       ~~~~~~~~~~~~~~
src/androidx/compose/lint/test/TestClass.kt:8: Error: Using by lazy has high overhead. Instead, either always execute the initialization or use a nullable or other invalid value by default to detect that the value hasn't been initialized. If lazy is intended, then the mode parameter should always be passed. [LazyDelegate]
                        val qualifiedLazy by kotlin.lazy { "qualified" }
                                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/lint/test/TestClass.kt:11: Error: Using by lazy has high overhead. Instead, either always execute the initialization or use a nullable or other invalid value by default to detect that the value hasn't been initialized. If lazy is intended, then the mode parameter should always be passed. [LazyDelegate]
                            val localLazy by lazy { "local" }
                                          ~~~~~~~~~~~~~~~~~~~
4 errors, 0 warnings
                """
            )
    }

    @Test
    fun lazyPropertyWithMode_doesNotReportError() {
        lint()
            .files(
                kotlin(
                    """
                    package androidx.compose.lint.test

                    import kotlin.LazyThreadSafetyMode

                    val parameterizedLazy by lazy(LazyThreadSafetyMode.NONE) { "none" }
                    val namedModeLazy by lazy(mode = LazyThreadSafetyMode.NONE) { "named" }

                    class CustomDelegate {
                        operator fun getValue(thisRef: Any?, property: Any?): String = "custom"
                    }

                    val customProperty by CustomDelegate()
                    val regularProperty = "regular"
                    """
                )
            )
            .run()
            .expectClean()
    }
}
