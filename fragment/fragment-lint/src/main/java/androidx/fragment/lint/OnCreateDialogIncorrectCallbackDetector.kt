/*
 * Copyright 2021 The Android Open Source Project
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

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.kotlin.KotlinUClass
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * When using a `DialogFragment`, the `setOnCancelListener` and `setOnDismissListener` callback
 * functions within the `onCreateDialog` function __must not be used__
 * because the `DialogFragment` owns these callbacks. Instead the respective `onCancel` and
 * `onDismiss` functions can be used to achieve the desired effect.
 */
class OnCreateDialogIncorrectCallbackDetector : Detector(), SourceCodeScanner {

    companion object Issues {
        val ISSUE = Issue.create(
            id = "DialogFragmentCallbacksDetector",
            briefDescription = "Use onCancel() and onDismiss() instead of calling " +
                "setOnCancelListener() and setOnDismissListener() from onCreateDialog()",
            explanation = """When using a `DialogFragment`, the `setOnCancelListener` and \
                `setOnDismissListener` callback functions within the `onCreateDialog` function \
                 __must not be used__ because the `DialogFragment` owns these callbacks. \
                 Instead the respective `onCancel` and `onDismiss` functions can be used to \
                 achieve the desired effect.""",
            category = Category.CORRECTNESS,
            severity = Severity.WARNING,
            implementation = Implementation(
                OnCreateDialogIncorrectCallbackDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            ),
            androidSpecific = true
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UClass::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return UastHandler(context)
    }

    private inner class UastHandler(val context: JavaContext) : UElementHandler() {
        override fun visitClass(node: UClass) {
            if (isKotlin(context.psiFile) &&
                (node as? KotlinUClass)?.ktClass?.getSuperNames()?.firstOrNull() !=
                DIALOG_FRAGMENT_CLASS
            ) {
                return
            }

            if (!isKotlin(context.psiFile) &&
                (node.uastSuperTypes.firstOrNull()?.type as? PsiClassReferenceType)
                    ?.className != DIALOG_FRAGMENT_CLASS
            ) {
                return
            }

            node.methods.forEach {
                if (it.name == ENTRY_METHOD) {
                    val visitor = UastMethodsVisitor(context, it.name)
                    it.uastBody?.accept(visitor)
                }
            }
        }
    }

    /**
     * A UAST Visitor that explores all method calls within a
     * [androidx.fragment.app.DialogFragment] callback to check for an incorrect method call.
     *
     * @param context The context of the lint request.
     * @param containingMethodName The name of the originating Fragment lifecycle method.
     */
    private class UastMethodsVisitor(
        private val context: JavaContext,
        private val containingMethodName: String
    ) : AbstractUastVisitor() {
        private val visitedMethods = mutableSetOf<UCallExpression>()

        override fun visitCallExpression(node: UCallExpression): Boolean {
            if (visitedMethods.contains(node)) {
                return super.visitCallExpression(node)
            }

            val methodName = node.methodIdentifier?.name ?: return super.visitCallExpression(node)

            when (methodName) {
                SET_ON_CANCEL_LISTENER -> {
                    report(
                        context = context,
                        node = node,
                        message = "Use onCancel() instead of calling setOnCancelListener() " +
                            "from onCreateDialog()"
                    )
                    visitedMethods.add(node)
                }
                SET_ON_DISMISS_LISTENER -> {
                    report(
                        context = context,
                        node = node,
                        message = "Use onDismiss() instead of calling setOnDismissListener() " +
                            "from onCreateDialog()"
                    )
                    visitedMethods.add(node)
                }
            }
            return super.visitCallExpression(node)
        }

        private fun report(context: JavaContext, node: UCallExpression, message: String) {
            context.report(
                issue = ISSUE,
                location = context.getLocation(node),
                message = message,
                quickfixData = null
            )
        }
    }
}

private const val ENTRY_METHOD = "onCreateDialog"
private const val DIALOG_FRAGMENT_CLASS = "DialogFragment"
private const val SET_ON_CANCEL_LISTENER = "setOnCancelListener"
private const val SET_ON_DISMISS_LISTENER = "setOnDismissListener"
