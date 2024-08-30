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

package androidx.compose.runtime.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Names
import androidx.compose.lint.inheritsFrom
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
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMultiResolvable
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.getParameterForArgument
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * [Detector] that checks calls to produceState, to make sure that the producer lambda writes to
 * MutableState#value.
 *
 * We also check to see if the lambda calls an external function that accepts a parameter of type
 * ProduceStateScope / MutableState to avoid false positives in case there is a utility function
 * that writes to MutableState#value.
 */
class ProduceStateDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> =
        listOf(Names.Runtime.ProduceState.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (method.isInPackageName(Names.Runtime.PackageName)) {
            // The ProduceStateScope lambda
            val producer =
                node.valueArguments.find { node.getParameterForArgument(it)?.name == "producer" }
                    ?: return

            var referencesReceiver = false
            var callsSetValue = false

            producer.accept(
                object : AbstractUastVisitor() {
                    val mutableStatePsiClass =
                        context.evaluator.findClass(Names.Runtime.MutableState.javaFqn)

                    /**
                     * Visit function calls to see if the functions have a parameter of MutableState
                     * / ProduceStateScope. If they do, we cannot know for sure whether those
                     * functions internally call setValue, so we avoid reporting an error to avoid
                     * false positives.
                     */
                    override fun visitCallExpression(node: UCallExpression): Boolean {
                        val resolvedMethod = node.resolve() ?: return referencesReceiver
                        return resolvedMethod.parameterList.parameters.any { parameter ->
                            val type = parameter.type

                            // Is the parameter type ProduceStateScope or a subclass
                            if (type.inheritsFrom(ProduceStateScopeName)) {
                                referencesReceiver = true
                            }

                            // Is the parameter type MutableState
                            if (
                                mutableStatePsiClass != null &&
                                    context.evaluator.getTypeClass(type) == mutableStatePsiClass
                            ) {
                                referencesReceiver = true
                            }

                            referencesReceiver
                        }
                    }

                    /**
                     * Visit binary operator to see if
                     * 1) it is an assign operator;
                     * 2) its left operand refers to 'value'; and
                     * 3) it is resolved to a call to MutableState#setValue.
                     */
                    override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
                        // =, +=, etc.
                        if (node.operator !is UastBinaryOperator.AssignOperator) {
                            return callsSetValue
                        }
                        // this.value =, value +=, etc.
                        if (node.leftOperand.rightMostNameReference()?.identifier != "value") {
                            return callsSetValue
                        }
                        // NB: we can't use node.resolveOperator() during the migration,
                        // since K1 / K2 UAST behaviors mismatch.
                        // `multiResolve()` literally encompasses all possible resolution
                        // results for (compound, overridden) operators.
                        val resolvedMethods =
                            (node as? UMultiResolvable)?.multiResolve() ?: return callsSetValue
                        if (
                            resolvedMethods.any {
                                (it.element as? PsiMethod).isValueAccessorFromMutableState()
                            }
                        ) {
                            callsSetValue = true
                            return true
                        }
                        // TODO(b/34684249): revisit this fallback option after restoring
                        //  K2 UAST binary resolution to allow only setter, not getter
                        val resolvedOperand = node.leftOperand.tryResolve()
                        if ((resolvedOperand as? PsiMethod).isValueAccessorFromMutableState()) {
                            callsSetValue = true
                        }
                        return callsSetValue
                    }

                    private tailrec fun UExpression.rightMostNameReference():
                        USimpleNameReferenceExpression? {
                        return when (this) {
                            is USimpleNameReferenceExpression -> this
                            is UQualifiedReferenceExpression ->
                                this.selector.rightMostNameReference()
                            else -> null
                        }
                    }

                    private fun PsiMethod?.isValueAccessorFromMutableState(): Boolean =
                        this != null &&
                            (name == "setValue" || name == "getValue") &&
                            containingClass?.inheritsFrom(Names.Runtime.MutableState) == true
                }
            )

            if (!callsSetValue && !referencesReceiver) {
                context.report(
                    ProduceStateDoesNotAssignValue,
                    node,
                    context.getNameLocation(node),
                    "produceState calls should assign `value` inside the producer lambda"
                )
            }
        }
    }

    companion object {
        val ProduceStateDoesNotAssignValue = Issue.create(
            "ProduceStateDoesNotAssignValue",
            "produceState calls should assign `value` inside the producer lambda",
            "produceState returns an observable State using values assigned inside the producer " +
                "lambda. If the lambda never assigns (i.e `value = foo`), then the State will " +
                "never change. Make sure to assign a value when the source you are producing " +
                "values from changes / emits a new value. For sample usage see the produceState " +
                "documentation.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                ProduceStateDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

private val ProduceStateScopeName = Name(Names.Runtime.PackageName, "ProduceStateScope")
