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

package androidx.test.uiautomator.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AccessibilityNodeInfoGetTextDetectorTest : LintDetectorTest() {

    private val methods = arrayOf("onView", "onViews", "onViewOrNull")

    override fun getDetector(): Detector =
        AccessibilityNodeInfoGetTextDetector(useAccessibilityNodeInfoFullQualifiedClassName = false)

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(AccessibilityNodeInfoGetTextDetector.ISSUE)

    @Test
    fun expectIssueOnProperty() =
        methods.forEach {
            kotlinCode(
                warnings = 1,
                code =
                    """
                @Test
                fun myTest() = uiAutomator {
                    $it { text == "Ok" }
                }
            """,
            )
        }

    @Test
    fun expectIssueOnProperty2() =
        methods.forEach {
            kotlinCode(
                warnings = 1,
                code =
                    """
                @Test
                fun myTest() = uiAutomator {
                    $it { "Ok" == text }
                }
            """,
            )
        }

    @Test
    fun expectIssueOnPropertyWithToString() =
        methods.forEach {
            kotlinCode(
                warnings = 1,
                code =
                    """
                @Test
                fun myTest() = uiAutomator {
                    $it { text.toString() == "Ok" }
                }
            """,
            )
        }

    @Test
    fun expectIssueOnPropertyInIfStatement() =
        methods.forEach {
            kotlinCode(
                warnings = 1,
                code =
                    """
                @Test
                fun myTest() = uiAutomator {
                    $it { text == "Ok" }
                }
            """,
            )
        }

    @Test
    fun expectIssueOnPropertyUsedInFunction() =
        methods.forEach {
            kotlinCode(
                warnings = 1,
                code =
                    """
                @Test
                fun myTest() = uiAutomator {
                    $it { someFunction(text) }
                }
            """,
            )
        }

    @Test
    fun expectIssueOnPropertyUsedInThis() =
        methods.forEach {
            kotlinCode(
                warnings = 1,
                code =
                    """
                @Test
                fun myTest() = uiAutomator {
                    $it { this.text == "Ok" }
                }
            """,
            )
        }

    @Test
    fun expectCleanOnAsStringProperty() =
        methods.forEach {
            kotlinCode(
                warnings = 0,
                code =
                    """
                @Test
                fun myTest() = uiAutomator {
                    $it { textAsString == "Ok" }
                }
            """,
            )
        }

    @Test
    fun expectCleanOnFunctionNamedText() =
        methods.forEach {
            kotlinCode(
                warnings = 0,
                code =
                    """
                @Test
                fun myTest() = uiAutomator {
                    $it { text("something") == "Ok" }
                }
            """,
            )
        }

    @Test
    fun expectIssueIfOtherObjectIsAccessibilityNodeInfo() =
        methods.forEach {
            kotlinCode(
                warnings = 1,
                code =
                    """
                @Test
                fun myTest() = uiAutomator {
                    $it {
                        val someOtherObject = this
                        someOtherObject.text == "Ok"
                    }
                }
            """,
            )
        }

    @Test
    fun expectCleanIfOtherObjectIsNotAccessibilityNodeInfo() =
        methods.forEach {
            kotlinCode(
                warnings = 0,
                code =
                    """
                @Test
                fun myTest() = uiAutomator {
                    $it { SomeObject().text == "Ok" }
                }
            """,
            )
        }

    @Test
    fun expectIssueWithInnerScopesWhenReferringToAccessibilityNodeInfo() =
        methods.forEach {
            kotlinCode(
                warnings = 1,
                code =
                    """
                @Test
                fun myTest() = uiAutomator {
                    $it {
                        val someOtherObject = this
                        blockFunction {
                            someOtherObject.text == "Ok"
                        }
                    }
                }
            """,
            )
        }

    @Test
    fun expectCleanWithInnerScopesWhenNotReferringToAccessibilityNodeInfo() =
        methods.forEach {
            kotlinCode(
                warnings = 0,
                code =
                    """
                @Test
                fun myTest() = uiAutomator {
                    $it {
                        val someOtherObject = this
                        blockFunction {
                            text == someOtherObject.textAsString
                        }
                    }
                }
            """,
            )
        }

    private fun kotlinCode(warnings: Int, @Language("kotlin") code: String) {
        lint().files(kotlin(code), STUBS).run().expectWarningCount(warnings)
    }
}

private val STUBS =
    kotlin(
        """
        fun onView(block: AccessibilityNodeInfo.() -> (Boolean)) { }
        fun onViews(block: AccessibilityNodeInfo.() -> (Boolean)) { }
        fun onViewOrNull(block: AccessibilityNodeInfo.() -> (Boolean)) { }

        fun someFunction(value: CharSequence): Boolean = true
        fun blockFunction(block: SomeObject.() -> (Boolean)) { }

        class SomeObject {
            val text = "text"
        }

        class AccessibilityNodeInfo {
            val text = "text"
            val textAsString = "textAsString"
        }
"""
    )
