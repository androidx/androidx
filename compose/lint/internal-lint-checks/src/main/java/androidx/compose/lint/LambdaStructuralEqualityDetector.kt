/*
 * Copyright 2024 The Android Open Source Project
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
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UastBinaryOperator

/**
 * Lint [Detector] to ensure that lambda instances are compared referentially, instead of
 * structurally.
 *
 * This is needed as function references (::lambda) do not consider their capture scope in their
 * equals implementation. This means that structural equality can return true, even if the lambdas
 * are different references with a different capture scope. Instead, lambdas should be compared
 * referentially (=== or !==) to avoid this issue.
 */
class LambdaStructuralEqualityDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() =
        listOf(UBinaryExpression::class.java, UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitBinaryExpression(node: UBinaryExpression) {
                val op = node.operator
                if (op == UastBinaryOperator.EQUALS || op == UastBinaryOperator.NOT_EQUALS) {
                    val left = node.leftOperand.sourcePsi as? KtExpression ?: return
                    val right = node.rightOperand.sourcePsi as? KtExpression ?: return
                    if (left.isFunctionType() && right.isFunctionType()) {
                        val replacement = if (op == UastBinaryOperator.EQUALS) "===" else "!=="
                        context.report(
                            ISSUE,
                            node.operatorIdentifier,
                            context.getNameLocation(node.operatorIdentifier ?: node),
                            BriefDescription,
                            LintFix.create()
                                .replace()
                                .name("Change to $replacement")
                                .text(op.text)
                                .with(replacement)
                                .autoFix()
                                .build()
                        )
                    }
                }
            }

            override fun visitCallExpression(node: UCallExpression) {
                if (node.methodName == "equals") {
                    val left = node.receiver?.sourcePsi as? KtExpression ?: return
                    val right =
                        node.valueArguments.firstOrNull()?.sourcePsi as? KtExpression ?: return
                    if (left.isFunctionType() && right.isFunctionType()) {
                        context.report(ISSUE, node, context.getNameLocation(node), BriefDescription)
                    }
                }
            }
        }

    private fun KtExpression.isFunctionType(): Boolean =
        analyze(this) { expressionType?.isFunctionType == true }

    companion object {
        private const val BriefDescription =
            "Checking lambdas for structural equality, instead " +
                "of checking for referential equality"
        private const val Explanation =
            "Checking structural equality on lambdas can lead to issues, as function references " +
                "(::lambda) do not consider their capture scope in their equals implementation. " +
                "This means that structural equality can return true, even if the lambdas are " +
                "different references with a different capture scope. Instead, lambdas should be" +
                "compared referentially (=== or !==) to avoid this issue."

        val ISSUE =
            Issue.create(
                "LambdaStructuralEquality",
                BriefDescription,
                Explanation,
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(
                    LambdaStructuralEqualityDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}
