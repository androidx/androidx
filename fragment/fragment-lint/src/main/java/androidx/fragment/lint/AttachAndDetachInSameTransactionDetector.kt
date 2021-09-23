/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.fragment.lint

import com.android.tools.lint.checks.DataFlowAnalyzer
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.getMethodName
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.tryResolve

/**
 * Lint check for detecting calls to [androidx.fragment.app.FragmentTransaction.attach] and
 * [androidx.fragment.app.FragmentTransaction.detach] in the same
 * [androidx.fragment.app.FragmentTransaction] on the same [androidx.fragment.app.Fragment] instance
 */
class AttachAndDetachInSameTransactionDetector : Detector(), SourceCodeScanner {

    companion object Issues {
        val DETACH_ATTACH_OPERATIONS_ISSUE = Issue.create(
            id = "DetachAndAttachSameFragment",
            briefDescription = "Separate attach() and detach() into separate FragmentTransactions",
            explanation = """When doing a FragmentTransaction that includes both attach() \
                and detach() operations being committed on the same fragment instance, it is a \
                no-op. The reason for this is that the FragmentManager optimizes all operations \
                within a single transaction so the attach() and detach() cancel each other out \
                and neither is actually executed. To get the desired behavior, you should separate \
                the attach() and detach() calls into separate FragmentTransactions.""",
            category = Category.CORRECTNESS,
            severity = Severity.WARNING,
            implementation = Implementation(
                AttachAndDetachInSameTransactionDetector::class.java, Scope.JAVA_FILE_SCOPE
            ),
            androidSpecific = true
        )

        // Target method names
        private const val ATTACH = "attach"
        private const val BEGIN_TRANSACTION = "beginTransaction"
        private const val DETACH = "detach"

        private const val FRAGMENT_CLS = "androidx.fragment.app.Fragment"
        private const val FRAGMENT_MANAGER_CLS = "androidx.fragment.app.FragmentManager"
        private const val FRAGMENT_TRANSACTION_CLS = "androidx.fragment.app.FragmentTransaction"
    }

    override fun getApplicableMethodNames(): List<String> = listOf(BEGIN_TRANSACTION)

    override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod
    ) {
        when (method.name) {
            BEGIN_TRANSACTION -> checkTransactionCommits(context, node, method)
            else -> super.visitMethodCall(context, node, method)
        }
    }

    private fun checkTransactionCommits(
        context: JavaContext,
        node: UCallExpression,
        calledMethod: PsiMethod
    ) {
        if (isBeginTransaction(context, calledMethod)) {
            val method = node.getParentOfType(UMethod::class.java) ?: return
            var attachingFragment: UElement? = null
            var detachingFragment: UElement? = null
            val visitor = object : DataFlowAnalyzer(setOf(node), emptyList()) {
                override fun receiver(call: UCallExpression) {
                    if (isAttachFragmentMethodCall(context, call)) {
                        val arg = call.valueArguments.firstOrNull {
                            it.getExpressionType().extends(context, FRAGMENT_CLS, false)
                        }
                        attachingFragment = arg?.tryResolve()?.toUElement()
                    }
                    if (isDetachFragmentMethodCall(context, call)) {
                        val arg = call.valueArguments.firstOrNull {
                            it.getExpressionType().extends(context, FRAGMENT_CLS, false)
                        }
                        detachingFragment = arg?.tryResolve()?.toUElement()
                    }
                }
            }
            method.accept(visitor)

            if (attachingFragment != null && attachingFragment == detachingFragment) {
                val message = "Calling detach() and attach() in the same FragmentTransaction is " +
                    "a no-op, meaning it does not recreate the Fragment's view. If you would " +
                    "like the view to be recreated, separate these operations into separate " +
                    "transactions."
                context.report(
                    DETACH_ATTACH_OPERATIONS_ISSUE,
                    node,
                    context.getNameLocation(node),
                    message
                )
            }
        }
    }

    private fun isBeginTransaction(
        context: JavaContext,
        method: PsiMethod
    ): Boolean {
        return BEGIN_TRANSACTION == method.name &&
            context.evaluator.isMemberInSubClassOf(method, FRAGMENT_MANAGER_CLS)
    }

    internal fun isAttachFragmentMethodCall(
        context: JavaContext,
        call: UCallExpression
    ): Boolean {
        val methodName = getMethodName(call)
        return ATTACH == methodName &&
            isMethodOnFragmentClass(context, call, FRAGMENT_TRANSACTION_CLS, true)
    }

    internal fun isDetachFragmentMethodCall(
        context: JavaContext,
        call: UCallExpression
    ): Boolean {
        val methodName = getMethodName(call)
        return DETACH == methodName &&
            isMethodOnFragmentClass(context, call, FRAGMENT_TRANSACTION_CLS, true)
    }

    private fun isMethodOnFragmentClass(
        context: JavaContext,
        call: UCallExpression,
        fragmentClass: String,
        returnForUnresolved: Boolean
    ): Boolean {
        // If we *can't* resolve the method call, caller can decide
        // whether to consider the method called or not
        val method = call.resolve() ?: return returnForUnresolved
        return context.evaluator.isMemberInSubClassOf(method, fragmentClass)
    }
}
