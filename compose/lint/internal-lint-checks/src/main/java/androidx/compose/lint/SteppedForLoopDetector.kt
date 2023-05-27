/*
 * Copyright 2023 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.compose.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiType
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UForEachExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.isIntegralLiteral
import org.jetbrains.uast.kotlin.isKotlin
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.util.isMethodCall

/**
 * Lint [Detector] to prevent allocating ranges and progression when using `step()` in a
 * for loops. For instance: `for (i in a..b step 2)` .
 */
class SteppedForLoopDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(
        UForEachExpression::class.java
    )

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitForEachExpression(node: UForEachExpression) {
            if (!isKotlin(node.lang)) return

            when (val type = node.iteratedValue.skipParenthesizedExprDown()) {
                is UBinaryExpression -> {
                    // Check the expression is of the form a step b, where a is a Progression type
                    if (
                        isIntegerProgression(type.leftOperand.getExpressionType()) &&
                        !isLiteralProgression(type.leftOperand.skipParenthesizedExprDown()) &&
                        type.operatorIdentifier?.name == "step" &&
                        isInteger(type.rightOperand.getExpressionType())
                    ) {
                        report(context, node, type, type.rightOperand.asRenderString())
                    }
                }
                is UQualifiedReferenceExpression -> {
                    if (type.selector.isMethodCall()) {
                        val method = type.selector as UCallExpression
                        // Check we invoke step(x) on a Progression type
                        if (
                            isIntegerProgression(method.receiverType) &&
                            !isLiteralProgression(method.receiver?.skipParenthesizedExprDown()) &&
                            method.methodName == "step" &&
                            method.valueArgumentCount == 1 &&
                            isInteger(method.valueArguments[0].getExpressionType())
                        ) {
                            report(
                                context,
                                node,
                                method,
                                method.valueArguments[0].asRenderString()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isIntegerProgression(type: PsiType?): Boolean {
        if (type == null) return false

        if (type is PsiClassReferenceType) {
            val cls = type.resolve()
            return cls != null &&
                (
                    IntegerProgressionTypes.contains(cls.qualifiedName) ||
                    cls.superTypes.any {
                        IntegerProgressionTypes.contains(it.resolve()?.qualifiedName)
                    }
                )
        }

        return false
    }

    private fun isLiteralProgression(expression: UExpression?) =
            expression is UBinaryExpression &&
            expression.operator.text == ".." &&
            expression.leftOperand.skipParenthesizedExprDown().isIntegralLiteral() &&
            expression.rightOperand.skipParenthesizedExprDown().isIntegralLiteral()

    private fun isInteger(type: PsiType?) = type == PsiType.INT || type == PsiType.LONG

    private fun report(context: JavaContext, node: UElement, target: Any?, messageContext: String) {
        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(target),
            message = "stepping the integer range by $messageContext."
        )
    }

    companion object {
        val ISSUE = Issue.create(
            "SteppedForLoop",
            "A loop over a primitive range (Int/Long/ULong/Char) creates " +
                "unnecessary allocations",
            "Using the step function when iterating over a range of integer types " +
                "causes the allocation of a Range and of a Progression. To avoid the " +
                "allocations, consider using a while loop and manual loop counter.",
            Category.PERFORMANCE, 5, Severity.ERROR,
            Implementation(
                SteppedForLoopDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
        val IntegerProgressionTypes = listOf(
            "kotlin.ranges.IntProgression",
            "kotlin.ranges.LongProgression",
            "kotlin.ranges.CharProgression",
            "kotlin.ranges.UIntProgression",
            "kotlin.ranges.ULongProgression"
        )
    }
}
