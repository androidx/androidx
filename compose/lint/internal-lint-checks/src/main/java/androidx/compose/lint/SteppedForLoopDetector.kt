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
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UForEachExpression
import org.jetbrains.uast.kotlin.isKotlin
import org.jetbrains.uast.skipParenthesizedExprDown

/**
 * Lint [Detector] to prevent allocating ranges and progression when using `step()` in a
 * for loops. For instance: `for (i in a..b step 2)` .
 * See https://youtrack.jetbrains.com/issue/KT-59115
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
                        isUntilRange(type.leftOperand.skipParenthesizedExprDown()) &&
                        type.operatorIdentifier?.name == "step" &&
                        isInteger(type.rightOperand.getExpressionType())
                    ) {
                        report(context, node, type, type.rightOperand.textRepresentation())
                    }
                }
            }
        }
    }

    private fun isIntegerProgression(type: PsiType?): Boolean {
        if (type == null) return false

        if (type is PsiClassType) {
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

    private fun UElement.textRepresentation() = sourcePsi?.text ?: asRenderString()

    // https://youtrack.jetbrains.com/issue/KT-59115
    private fun isUntilRange(expression: UExpression?) =
        expression is UBinaryExpression &&
            (expression.operatorIdentifier?.name == "..<" ||
                expression.operatorIdentifier?.name == "until")

    // TODO: Use PsiTypes.intType() and PsiTypes.longType() when they are available
    private fun isInteger(type: PsiType?) =
        type?.canonicalText == "int" || type?.canonicalText == "long"

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
            "A loop over an 'until' or '..<' primitive range (Int/Long/ULong/Char)" +
                " creates unnecessary allocations",
            "Using 'until' or '..<' to create an iteration range bypasses a compiler" +
                " optimization. Consider until '..' instead. " +
                "See https://youtrack.jetbrains.com/issue/KT-59115",
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
