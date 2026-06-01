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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.EnumSet
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpressionList
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.ULabeledExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.USwitchExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.UYieldExpression

/**
 * This inspector checks usages of Kotlin coroutine's `runTest` method to ensure that the returned
 * `TestResult` is used.
 *
 * On most platforms, `TestResult` is aliased to `Unit` and `runTest` executes its test lambda
 * synchronously and blocks the test thread. On web targets, `TestResult` is backed by a promise
 * that does not immediately execute. Returning the `TestResult` as the result of the test method
 * causes the test runner to await the promise and start the test.
 *
 * By not returning the result of `runTest` to the test method (or manually awaiting the result),
 * the test block will never execute on web targets.
 *
 * Note that this inspection is disabled in AndroidX targets that do not use KMP since it's not
 * possible for a standard Kotlin module to define a test that's impacted by the behavior this
 * inspection checks for. This inspection doesn't analyze source sets, but may be manually
 * suppressed for tests that are not in a source set consumed by JS or WASM tests.
 */
class KotlinRunTestReturnResultDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                if (node.isRunTest()) {
                    val usageKind = node.resolveUsageKind()
                    if (usageKind == ResultUsageKind.NotUsed) {
                        context.report(
                            RUN_TEST_RESULT_UNUSED_ISSUE,
                            node,
                            context.getNameLocation(node),
                            "Result of runTest is ignored.",
                            LintFix.create()
                                .name("Insert `return`")
                                .replace()
                                .beginning()
                                .with("return ")
                                .build(),
                        )
                    }
                }
            }
        }

    private fun UCallExpression.isRunTest(): Boolean {
        val source = sourcePsi as? KtCallExpression ?: return false
        analyze(source) {
            val symbol = source.resolveToCall()?.singleFunctionCallOrNull()?.symbol
            return (symbol as? KaNamedFunctionSymbol)?.returnType?.isRunTest() ?: false
        }
    }

    private fun KaType.isRunTest(): Boolean {
        return abbreviationOrSelf.symbol?.classId?.asFqNameString() == TEST_RESULT_FQ_NAME
    }

    private tailrec fun UElement.resolveUsageKind(): ResultUsageKind {
        return when (val parent = uastParent) {
            // These UElement types are always considered used
            is @Suppress("UnstableApiUsage")
            UYieldExpression,
            is UBinaryExpression,
            is UVariable,
            is UExpressionList -> ResultUsageKind.Used
            is UReturnExpression -> ResultUsageKind.Returned
            // These UElement types are sometimes considered used
            is UQualifiedReferenceExpression -> {
                if (parent.receiver == this) {
                    ResultUsageKind.Used
                } else {
                    parent.resolveUsageKind()
                }
            }
            is UBlockExpression -> {
                if (parent.expressions.lastOrNull() == this) {
                    parent.resolveUsageKind()
                } else {
                    ResultUsageKind.NotUsed
                }
            }
            is UIfExpression -> {
                if (parent.condition == this) {
                    ResultUsageKind.Used
                } else {
                    ResultUsageKind.NotUsed
                }
            }
            // These UElement types don't have enough information to indicate usage.
            is ULambdaExpression,
            is USwitchExpression,
            is UParenthesizedExpression,
            is ULabeledExpression -> parent.resolveUsageKind()
            // All other UElement types are considered not used.
            else -> ResultUsageKind.NotUsed
        }
    }

    private enum class ResultUsageKind {
        Used,
        Returned,
        NotUsed,
    }

    companion object {
        val RUN_TEST_RESULT_UNUSED_ISSUE =
            Issue.create(
                id = "KotlinRunTestResultUnused",
                briefDescription = "Ignored runTest result causes test to no-op on web targets",
                explanation =
                    "Tests that utilize runTest must return the result of the call as the " +
                        "return value of the test. Not using the return result will cause the " +
                        "test block to never execute on web targets where the return type is " +
                        "backed by a promise that triggers execution of the test block.\n\n" +
                        "You may suppress this error if your test does not target JS or WASM.",
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        KotlinRunTestReturnResultDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )

        private const val TEST_RESULT_FQ_NAME = "kotlinx.coroutines.test.TestResult"
    }
}
