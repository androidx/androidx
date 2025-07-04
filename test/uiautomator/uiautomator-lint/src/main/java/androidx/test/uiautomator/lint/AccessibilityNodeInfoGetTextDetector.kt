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

import androidx.test.uiautomator.lint.AccessibilityNodeInfoGetTextDetector.Companion.ISSUE
import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import java.util.EnumSet
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

class AccessibilityNodeInfoGetTextDetector
internal constructor(

    /** For testing, in test files we cannot use package directive. */
    private val useAccessibilityNodeInfoFullQualifiedClassName: Boolean
) : Detector(), SourceCodeScanner {

    constructor() : this(useAccessibilityNodeInfoFullQualifiedClassName = false)

    companion object {
        val ISSUE =
            Issue.create(
                id = "DontUseAccessibilityNodeInfoGetText",
                briefDescription =
                    "Do not use AccessibilityNodeInfo#getText in a UiAutomatorTestScope.",
                explanation =
                    """
                        You should not used AccessibilityNodeInfo#getText in a UiAutomatorTestScope.
                        AccessibilityNodeInfo#getText returns a CharSequence rather than a String.
                        This can also be a SpannableString that when compared with a String always
                        returns false. Use AccessibilityNodeInfo#getTextAsString instead.
                    """,
                category = Category.CORRECTNESS,
                severity = Severity.WARNING,
                implementation =
                    Implementation(
                        AccessibilityNodeInfoGetTextDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE),
                        Scope.JAVA_FILE_SCOPE,
                    ),
            )
    }

    override fun getApplicableMethodNames(): List<String> =
        listOf("onView", "onViews", "onViewOrNull")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {

        // This is called only on methods named "onView", "onViews", "onViewOrNull"` because of
        // `getApplicableMethodNames`. Node that we don't check if parent is `uiAutomator` because
        // these functions can be called also on other classes and separately from `uiAutomator`.

        val lambdaArgument = node.valueArguments.lastOrNull() as? ULambdaExpression ?: return
        val lambdaBody = lambdaArgument.body as? UBlockExpression ?: return
        lambdaBody.accept(
            TextPropertyVisitor(
                context = context,
                useAccessibilityNodeInfoFullQualifiedClassName =
                    useAccessibilityNodeInfoFullQualifiedClassName,
            )
        )
    }
}

private class TextPropertyVisitor(
    private val context: JavaContext,
    private val useAccessibilityNodeInfoFullQualifiedClassName: Boolean,
) : AbstractUastVisitor() {

    companion object {
        private const val PROPERTY_NAME_TEXT = "text"
    }

    private fun PsiType?.isAccessibilityNodeInfo(): Boolean =
        if (useAccessibilityNodeInfoFullQualifiedClassName) {
            this?.canonicalText == "android.view.accessibility.AccessibilityNodeInfo"
        } else {
            this?.canonicalText?.substringAfterLast(delimiter = ".") == "AccessibilityNodeInfo"
        }

    override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression): Boolean {

        // These are simple references, i.e. direct references to `text` or a function
        // named `text` like text("something"). We want to skip functions named `text`.

        if (node.identifier == PROPERTY_NAME_TEXT) {

            // Check if it's used as a function call, example: text("something"). In this case the
            // parent is a call expression and the node is the method identifier.
            val parent = node.uastParent as? UCallExpression
            if (parent != null && parent.methodIdentifier == node) {
                return super.visitSimpleNameReferenceExpression(node)
            }

            // Check if it is an implicit property access (like this.text, without `this`).
            val receiverType = node.getImplicitReceiverType(context.evaluator)
            if (receiverType.isAccessibilityNodeInfo()) {
                reportIssue(node)
            }
        }
        return super.visitSimpleNameReferenceExpression(node)
    }

    override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression): Boolean {

        // These are qualified references, i.e. references with a qualifier, followed by a dot `.`.
        // For example `someObject.text`, `someObject.text()`.

        if (node.resolvedName == PROPERTY_NAME_TEXT) {

            // Check if it's used as a function call. If `text` is a function call, ignore.
            val parentCall = node.uastParent as? UCallExpression
            if (parentCall != null && parentCall.methodIdentifier == node.selector) {
                return super.visitQualifiedReferenceExpression(node)
            }

            // Check if it's a property access on an explicit object. Note that in this case
            // we need to determine the type of the receiver to determine whether we're looking at
            // AccessibilityNodeInfo#text vs SomeOtherObject#text. The latter is not an issue.
            if (node.receiver.getExpressionType().isAccessibilityNodeInfo()) {
                reportIssue(node.selector)
            }
        }
        return super.visitQualifiedReferenceExpression(node) // Continue traversal
    }

    private fun USimpleNameReferenceExpression.getImplicitReceiverType(
        evaluator: JavaEvaluator
    ): PsiType? {
        val resolvedElement: PsiElement? = resolve()
        if (resolvedElement is PsiMember) {
            val containingClass: PsiClass? = resolvedElement.containingClass
            if (containingClass != null) {
                return evaluator.getClassType(containingClass)
            }
        }
        return null
    }

    /** Reports the issue found. */
    private fun reportIssue(node: UElement) {
        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(node),
            message = ISSUE.getBriefDescription(format = TextFormat.TEXT),
        )
    }
}
