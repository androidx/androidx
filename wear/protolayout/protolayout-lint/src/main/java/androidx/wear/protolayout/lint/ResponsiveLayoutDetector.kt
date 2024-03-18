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

package androidx.wear.protolayout.lint

import com.android.tools.lint.checks.DataFlowAnalyzer
import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.skipParenthesizedExprUp

private const val PRIMARY_LAYOUT_BUILDER =
    "androidx.wear.protolayout.material.layouts.PrimaryLayout.Builder"

private const val EDGE_CONTENT_LAYOUT_BUILDER =
    "androidx.wear.protolayout.material.layouts.EdgeContentLayout.Builder"

private const val RESPONSIVE_SETTER_NAME = "setResponsiveContentInsetEnabled"

// TODO(b/328785945): Improve edge cases like different scope for calls.
class ResponsiveLayoutDetector : Detector(), SourceCodeScanner {
    override fun getApplicableConstructorTypes(): List<String> =
        listOf(PRIMARY_LAYOUT_BUILDER, EDGE_CONTENT_LAYOUT_BUILDER)

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod
    ) {
        val containingClass = constructor.containingClass
        val evaluator = context.evaluator
        var issue: Issue? = null
        var message = ""
        var quickFix: LintFix? = null

        if (isPrimaryLayout(evaluator, containingClass)) {
            val potentialQuickFix = tryFindResponsiveSetter(context, node)
            if (potentialQuickFix != null) {
                issue = PRIMARY_LAYOUT_ISSUE
                message =
                    """
                        PrimaryLayout used, but responsiveness isn't set: Please call
                        `$RESPONSIVE_SETTER_NAME(true)` for the best results across
                        different screen sizes.
                    """
                        .trimIndent()
                quickFix = potentialQuickFix
            }
        }

        if (isEdgeContentLayout(evaluator, containingClass)) {
            val potentialQuickFix = tryFindResponsiveSetter(context, node)
            if (potentialQuickFix != null) {
                issue = EDGE_CONTENT_LAYOUT_ISSUE
                message =
                    """
                            EdgeContentLayout used, but responsiveness isn't set: Please call
                            `$RESPONSIVE_SETTER_NAME(true)` for the best results across
                            different screen sizes and update to the looks of layout.
                        """
                        .trimIndent()
                quickFix = potentialQuickFix
            }
        }

        if (issue != null) {
            context.report(
                issue,
                node,
                context.getCallLocation(node, includeReceiver = true, includeArguments = false),
                message,
                quickFix,
            )
        }
    }

    private fun isEdgeContentLayout(evaluator: JavaEvaluator, containingClass: PsiClass?) =
        evaluator.extendsClass(containingClass, EDGE_CONTENT_LAYOUT_BUILDER)

    private fun isPrimaryLayout(evaluator: JavaEvaluator, containingClass: PsiClass?) =
        evaluator.extendsClass(containingClass, PRIMARY_LAYOUT_BUILDER)

    /** Tries to find a {@link #RESPONSIVE_SETTER_NAME} and provides a quick fix if not found. */
    private fun tryFindResponsiveSetter(context: JavaContext, node: UCallExpression): LintFix? {
        var foundResponsive = false
        var foundFalseResponsive: UCallExpression? = null

        val visitor =
            object : DataFlowAnalyzer(setOf(node)) {
                override fun receiver(call: UCallExpression) {
                    if (call.methodName != RESPONSIVE_SETTER_NAME) {
                        return
                    }

                    if (call.valueArgumentCount != 1) {
                        return
                    }

                    val argValue = ConstantEvaluator.evaluate(context, call.valueArguments[0])

                    if (argValue is Boolean && argValue) {
                        // Found, everything is correct for now.
                        foundResponsive = true
                    } else {
                        if (foundResponsive) {
                            // We found a later call that called it with false, so the true
                            // version will be overridden, which should still report.
                            foundResponsive = false
                        }
                        // Since we found the wrong one, we need to provide a quick fix with
                        // replacement value.
                        foundFalseResponsive = call
                    }
                }
            }

        // Find a method/class/file (kt) where this expression is defined and visit all calls
        // to see if responsiveness is called.
        (node.getParentOfType(UCallExpression::class.java)
                ?: node.getParentOfType(UMethod::class.java)
                ?: node.getParentOfType(UClass::class.java)
                ?: skipParenthesizedExprUp(node.uastParent))
            ?.accept(visitor)

        if (foundResponsive && foundFalseResponsive == null) {
            return null
        }

        return if (foundFalseResponsive == null)
            fix()
                .replace()
                .name("Call $RESPONSIVE_SETTER_NAME(true) on layouts")
                .range(context.getLocation(node))
                .end()
                .with(".$RESPONSIVE_SETTER_NAME(true)")
                .build()
        else
            fix()
                .replace()
                .name("Call $RESPONSIVE_SETTER_NAME(true) on layouts")
                .range(
                    context.getCallLocation(
                        foundFalseResponsive!!,
                        includeReceiver = false,
                        includeArguments = true
                    )
                )
                .pattern("(.*)")
                .with("$RESPONSIVE_SETTER_NAME(true)")
                .reformat(true)
                .build()
    }

    companion object {
        @JvmField
        val PRIMARY_LAYOUT_ISSUE =
            Issue.create(
                id = "ProtoLayoutPrimaryLayoutResponsive",
                briefDescription =
                    "ProtoLayout Material PrimaryLayout should be used with responsive behaviour" +
                        "to ensure the best behaviour across different screen sizes and locales.",
                explanation =
                    """
            It is highly recommended to use the latest setResponsiveInsetEnabled(true) when you're
            using the ProtoLayout's PrimaryLayout.

            This is will take care of all inner padding to ensure that content of labels and bottom
            chip doesn't go off the screen (especially with different locales).
            """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.WARNING,
                androidSpecific = true,
                implementation =
                    Implementation(ResponsiveLayoutDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )

        @JvmField
        val EDGE_CONTENT_LAYOUT_ISSUE =
            Issue.create(
                id = "ProtoLayoutEdgeContentLayoutResponsive",
                briefDescription =
                    "ProtoLayout Material EdgeContentLayout should be used with responsive" +
                        "behaviour to ensure the best behaviour across different screen sizes and" +
                        "locales.",
                explanation =
                    """
            It is highly recommended to use the latest setResponsiveInsetEnabled(true) when you're
            using the ProtoLayout's EdgeContentLayout.

            This is will take care of all outer margins and inner padding to ensure that content of
            labels doesn't go off the screen (especially with different locales) and that primary
            label is placed in the consistent place.
            """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.WARNING,
                androidSpecific = true,
                implementation =
                    Implementation(ResponsiveLayoutDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )
    }
}
