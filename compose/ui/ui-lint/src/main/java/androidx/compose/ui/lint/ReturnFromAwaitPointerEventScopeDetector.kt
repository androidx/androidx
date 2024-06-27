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
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.skipParenthesizedExprUp

class ReturnFromAwaitPointerEventScopeDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> =
        listOf(Names.Ui.Pointer.AwaitPointerEventScope.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(Names.Ui.Pointer.PackageName)) return

        val methodParent = skipParenthesizedExprUp(node.uastParent)
        val isAssignedToVariable = methodParent is ULocalVariable

        val isReturnExpression = methodParent is UReturnExpression

        val invalidUseOfAwaitPointerEventScopeWithReturn =
            isReturnExpression && !validUseOfAwaitPointerEventScopeWithReturn(node)

        if (isAssignedToVariable || invalidUseOfAwaitPointerEventScopeWithReturn) {
            context.report(
                ExitAwaitPointerEventScope,
                node,
                context.getNameLocation(node),
                ErrorMessage
            )
        }
    }

    private fun validUseOfAwaitPointerEventScopeWithReturn(
        awaitPointerEventScopeNode: UCallExpression
    ): Boolean {
        // Traverse up the UAST tree
        var currentNode: UElement? = awaitPointerEventScopeNode.uastParent
        while (currentNode != null) {
            // Check if awaitPointerEventScope is within a PointerInputEventHandler or a
            // pointerInput method call (making it a valid use of return).
            if (
                currentNode is UCallExpression &&
                    (currentNode.methodName == POINTER_INPUT_HANDLER ||
                        currentNode.methodName == POINTER_INPUT_METHOD ||
                        currentNode.methodName == COROUTINE_METHOD)
            ) {
                return true
            }

            // For backward compatibility, checks if awaitPointerEventScopeNode is returned to a
            // "suspend PointerInputScope.() -> Unit" type variable (see test
            // awaitPointerEventScope_assignedFromContainingLambdaMethod_shouldNotWarn() ).
            if (currentNode is UVariable) {
                val variable = currentNode
                val lambda: UExpression? = variable.uastInitializer

                // Check if the initializer is a suspend lambda with the correct type
                if (lambda is ULambdaExpression) {
                    val ktLambdaExpression = lambda.sourcePsi
                    if (
                        ktLambdaExpression is KtLambdaExpression &&
                            isSuspendPointerInputLambda(ktLambdaExpression)
                    ) {
                        return true
                    }
                }
            }
            currentNode = currentNode.uastParent
        }
        return false
    }

    // Helper function for lambda type check
    private fun isSuspendPointerInputLambda(ktLambdaExpression: KtLambdaExpression): Boolean {
        return analyze(ktLambdaExpression) {
            val type = ktLambdaExpression.getExpectedType() as? KtFunctionalType ?: return false
            type.isSuspendFunctionType &&
                type.receiverType?.expandedClassSymbol?.classIdIfNonLocal?.asFqNameString() ==
                    POINTER_INPUT_SCOPE
        }
    }

    companion object {
        private const val POINTER_INPUT_SCOPE =
            "androidx.compose.ui.input.pointer.PointerInputScope"
        private const val POINTER_INPUT_HANDLER = "PointerInputEventHandler"
        private const val POINTER_INPUT_METHOD = "pointerInput"
        private const val COROUTINE_METHOD = "coroutineScope"

        const val IssueId: String = "ReturnFromAwaitPointerEventScope"
        const val ErrorMessage =
            "Returning from awaitPointerEventScope may cause some input " + "events to be dropped"
        val ExitAwaitPointerEventScope =
            Issue.create(
                IssueId,
                ErrorMessage,
                "Pointer Input events are queued inside awaitPointerEventScope. " +
                    "By using the return value of awaitPointerEventScope one might unexpectedly lose " +
                    "events. If another awaitPointerEventScope is restarted " +
                    "there is no guarantee that the events will persist between those calls. In this " +
                    "case you should keep all events inside the awaitPointerEventScope block",
                Category.CORRECTNESS,
                3,
                Severity.WARNING,
                Implementation(
                    ReturnFromAwaitPointerEventScopeDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}
