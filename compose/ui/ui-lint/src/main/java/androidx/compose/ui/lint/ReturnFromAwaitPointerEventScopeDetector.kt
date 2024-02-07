/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.lint

import androidx.compose.lint.Names
import androidx.compose.lint.isInPackageName
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isIncorrectImplicitReturnInLambda
import com.intellij.psi.LambdaUtil
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import java.util.EnumSet
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getParameterForArgument
import org.jetbrains.uast.skipParenthesizedExprUp

class ReturnFromAwaitPointerEventScopeDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> =
        listOf(Names.Ui.Pointer.AwaitPointerEventScope.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(Names.Ui.Pointer.PackageName)) return
        val methodParent = skipParenthesizedExprUp(node.uastParent)
        val isAssignedToVariable = methodParent is ULocalVariable
        val isReturnExpression = methodParent is UReturnExpression &&
            !methodParent.isIncorrectImplicitReturnInLambdaWorkaround()

        if (isAssignedToVariable || isReturnExpression) {
            context.report(
                ExitAwaitPointerEventScope,
                node,
                context.getNameLocation(node),
                ErrorMessage
            )
        }
    }

    companion object {
        const val IssueId: String = "ReturnFromAwaitPointerEventScope"
        const val ErrorMessage = "Returning from awaitPointerEventScope may cause some input " +
            "events to be dropped"
        val ExitAwaitPointerEventScope = Issue.create(
            IssueId,
            ErrorMessage,
            "Pointer Input events are queued inside awaitPointerEventScope. " +
                "By using the return value of awaitPointerEventScope one might unexpectedly lose " +
                "events. If another awaitPointerEventScope is restarted " +
                "there is no guarantee that the events will persist between those calls. In this " +
                "case you should keep all events inside the awaitPointerEventScope block",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                ReturnFromAwaitPointerEventScopeDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

// TODO: cleanup (perhaps after AGP 8.4.0-alpha09 b/323952048)
private fun UReturnExpression.isIncorrectImplicitReturnInLambdaWorkaround(): Boolean {
    if (isIncorrectImplicitReturnInLambda()) return true
    val block = uastParent as UBlockExpression
    // Check if this is the last expression of the lambda body
    if (block.expressions.lastOrNull() != this) return false
    // Make sure the lambda body belongs to a lambda
    if (block.uastParent !is ULambdaExpression) return false
    val lambda = block.uastParent as ULambdaExpression
    val lambdaReturnType =
        lambda
            .getReturnType()
            .let { returnType ->
                if (returnType is PsiWildcardType) returnType.bound else returnType
            }
            ?.canonicalText ?: return false
    // Already checked Unit, Nothing, void, Void
    if (lambdaReturnType != "java.lang.Object") return false
    val ktLambda = lambda.sourcePsi as? KtLambdaExpression
    return ktLambda?.let {
        analyze(it) {
            ktLambda.getExpectedType()?.isSuspendFunctionType
        }
    } ?: false
}

// utils "borrowed" from com.android.tools.lint.detector.api until we no longer need the workaround

private fun ULambdaExpression.getReturnType(): PsiType? {
    val lambdaType = getLambdaType()
    return LambdaUtil.getFunctionalInterfaceReturnType(lambdaType)
}

private fun ULambdaExpression.getLambdaType(): PsiType? =
    functionalInterfaceType
        ?: getExpressionType()
        ?: uastParent?.let {
            when (it) {
                is UVariable -> it.type
                is UCallExpression -> it.getParameterForArgument(this)?.type
                else -> null
            }
        }
