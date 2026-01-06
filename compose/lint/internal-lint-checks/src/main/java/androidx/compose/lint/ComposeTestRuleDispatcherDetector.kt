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

package androidx.compose.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getParameterForArgument
import org.jetbrains.uast.skipParenthesizedExprDown

/**
 * A lint detector that enforces the use of `StandardTestDispatcher` when using `runComposeUiTest`,
 * `runAndroidComposeUiTest`, `createComposeRule`, `createAndroidComposeRule` or
 * `createEmptyComposeRule()`.
 *
 * This rule ensure that UI tests involving coroutines are predictable and reliable. It checks for
 * two primary scenarios:
 * 1. The test API is called without a coroutine dispatcher argument, which is required for
 *    controlling the execution of coroutines in tests.
 * 2. The test API is called with an `UnconfinedTestDispatcher`, which is discouraged as it can lead
 *    to non-deterministic behavior and hide potential race conditions.
 *
 * The detector provides quick fixes to automatically correct both of these issues by either adding
 * a `StandardTestDispatcher()` argument or replacing the incorrect dispatcher.
 */
class ComposeTestRuleDispatcherDetector : Detector(), SourceCodeScanner {

    /**
     * Specifies the names of the methods that this detector should inspect. The detector will only
     * be triggered for calls to these methods.
     */
    override fun getApplicableMethodNames(): List<String> =
        listOf(
            CreateComposeRule.shortName,
            CreateAndroidComposeRule.shortName,
            CreateEmptyComposeRule.shortName,
        )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val dispatcherArgument =
            node.valueArguments.find {
                val param = node.getParameterForArgument(it)
                if (
                    method.name == RunComposeUiTest.shortName ||
                        method.name == RunAndroidComposeUiTest.shortName
                ) {
                    param?.name == "effectContext"
                } else {
                    param?.type?.inheritsFrom(CoroutineContext) == true
                }
            }

        if (dispatcherArgument == null) {
            context.report(
                issue = ISSUE,
                location = context.getLocation(node),
                message =
                    "`TestDispatcher` is required. Add `effectContext = ${StandardTestDispatcher.shortName}()` to control coroutine execution.",
                quickfixData = buildMissingDispatcherQuickFix(node),
            )
            return
        }

        val dispatcherType = findDispatcherInExpression(dispatcherArgument)
        if (
            dispatcherType != StandardTestDispatcher.shortName &&
                dispatcherType != TestDispatcher.shortName
        ) {
            val argumentSource = dispatcherArgument.sourcePsi?.text ?: "the provided dispatcher"
            context.report(
                issue = ISSUE,
                location = context.getLocation(dispatcherArgument),
                message = "Use `StandardTestDispatcher()` instead of `$argumentSource`.",
                quickfixData =
                    buildWrongTestDispatcherQuickFix(context.getLocation(dispatcherArgument)),
            )
        }
    }

    /**
     * Recursively searches for a specific `TestDispatcher` type within a [UExpression].
     *
     * This function can identify dispatchers in various forms:
     * 1. **Direct constructor calls**: `StandardTestDispatcher()`
     * 2. **Variable references**: A variable initialized with a test dispatcher.
     * 3. **Function/property parameters**: An argument whose type is a test dispatcher.
     * 4. **Binary expressions**: Expressions like `UnconfinedTestDispatcher() + scheduler`.
     *
     * @param expression The expression to inspect.
     * @return "StandardTestDispatcher" or "UnconfinedTestDispatcher" or `null` if no known test
     *   dispatcher is found.
     */
    private fun findDispatcherInExpression(expression: UExpression): String? {
        var currentExpr = expression.skipParenthesizedExprDown()
        if (currentExpr is UQualifiedReferenceExpression) {
            currentExpr = currentExpr.selector
        }

        if (currentExpr is UBinaryExpression && currentExpr.operator.text == "+") {
            val left = findDispatcherInExpression(currentExpr.leftOperand)
            val right = findDispatcherInExpression(currentExpr.rightOperand)
            return listOf(left, right).find { it != null }
        }

        return when (currentExpr) {
            is UCallExpression -> {
                val name = currentExpr.resolve()?.name
                if (
                    name == StandardTestDispatcher.shortName ||
                        name == UnconfinedTestDispatcher.shortName
                ) {
                    name
                } else {
                    null
                }
            }
            is UResolvable -> {
                val resolved = currentExpr.resolve()
                if (resolved is UVariable) {
                    resolved.uastInitializer?.let { findDispatcherInExpression(it) }
                } else if (
                    // This case covers the scenario where we cannot identify the dispatcher.
                    // For instance, when 'resolved' is not a UVariable, but the expression
                    // type is a TestDispatcher.
                    expression.getExpressionType()?.canonicalText == TestDispatcher.javaFqn
                ) {
                    TestDispatcher.shortName
                } else {
                    null
                }
            }
            else -> null
        }
    }

    /**
     * Builds a [LintFix] for cases where the `TestDispatcher` argument is missing.
     *
     * This adds `StandardTestDispatcher()` to the method call's argument list, handling both cases
     * where other arguments are present and where there are none. It also correctly handles calls
     * that use trailing lambda syntax without parentheses.
     *
     * @param node The [UCallExpression] representing the method call.
     * @return A [LintFix] that adds the required dispatcher argument, or null if the source text
     *   cannot be processed.
     */
    private fun buildMissingDispatcherQuickFix(node: UCallExpression): LintFix? {
        val source = node.sourcePsi as? KtCallExpression ?: return null
        val originalText = source.text

        val arg = "effectContext = ${StandardTestDispatcher.javaFqn}()"

        // Call has parentheses, e.g., createComposeRule() or runComposeUiTest().
        if (source.valueArgumentList != null) {
            val closeParen = source.valueArgumentList?.rightParenthesis ?: return null
            val nodeStartOffset = source.textRange?.startOffset ?: return null
            val parenStartOffset = closeParen.textRange.startOffset
            val cutOffIndex = parenStartOffset - nodeStartOffset
            val textUntilParen = originalText.take(cutOffIndex)
            val textToInsert = if (node.valueArgumentCount > 0) ", $arg" else arg
            val textToReplace = originalText.take(cutOffIndex + closeParen.textLength)
            val replacementText = "$textUntilParen$textToInsert)"
            return fix()
                .name("Add effectContext = ${StandardTestDispatcher.shortName}()")
                .replace()
                .text(textToReplace)
                .with(replacementText)
                .shortenNames()
                .autoFix()
                .build()
        }

        // Trailing lambda with no parentheses, e.g., runComposeUiTest { ... }
        if (source.lambdaArguments.isNotEmpty()) {
            val methodName = source.calleeExpression?.text ?: return null
            val replacementText = "$methodName($arg) ${source.lambdaArguments.first().text}"
            return fix()
                .name("Add effectContext = ${StandardTestDispatcher.shortName}()")
                .replace()
                .text(originalText)
                .with(replacementText)
                .shortenNames()
                .autoFix()
                .build()
        }

        return null
    }

    /**
     * Builds a [LintFix] for cases where an incorrect dispatcher (e.g., `UnconfinedTestDispatcher`)
     * is used.
     *
     * This fix replaces only the incorrect dispatcher argument with a `StandardTestDispatcher()`
     * call, leaving other arguments untouched.
     *
     * @param location The source code location of the incorrect argument to be replaced.
     * @return A [LintFix] that performs the replacement.
     */
    private fun buildWrongTestDispatcherQuickFix(location: Location): LintFix {
        return fix()
            .name("Replace with ${StandardTestDispatcher.shortName}()")
            .replace()
            .range(location)
            .with(newText = "${StandardTestDispatcher.javaFqn}()")
            .shortenNames()
            .build()
    }

    companion object {
        val ISSUE =
            Issue.create(
                id = "ComposeTestRuleDispatcher",
                briefDescription = "Compose test rule must use StandardTestDispatcher",
                explanation =
                    "Running Compose UI tests requires a `TestDispatcher` to control " +
                        "coroutine execution. `UnconfinedTestDispatcher` is disallowed " +
                        "because it can hide race conditions. For reliable and " +
                        "predictable tests, please provide a `StandardTestDispatcher`.",
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        ComposeTestRuleDispatcherDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )
    }
}

private val KotlinCoroutinesPackage = Package("kotlin.coroutines")
private val CoroutinesTestPackage = Package("kotlinx.coroutines.test")
private val CoroutineContext = Name(KotlinCoroutinesPackage, "CoroutineContext")
private val TestDispatcher = Name(CoroutinesTestPackage, "TestDispatcher")
private val UnconfinedTestDispatcher = Name(CoroutinesTestPackage, "UnconfinedTestDispatcher")
private val StandardTestDispatcher = Name(CoroutinesTestPackage, "StandardTestDispatcher")
private val ComposeTestPackage = Package("androidx.compose.ui.test")
private val CreateComposeRule = Name(ComposeTestPackage, "createComposeRule")
private val CreateAndroidComposeRule = Name(ComposeTestPackage, "createAndroidComposeRule")
private val CreateEmptyComposeRule = Name(ComposeTestPackage, "createEmptyComposeRule")
private val RunComposeUiTest = Name(ComposeTestPackage, "runComposeUiTest")
private val RunAndroidComposeUiTest = Name(ComposeTestPackage, "runAndroidComposeUiTest")
