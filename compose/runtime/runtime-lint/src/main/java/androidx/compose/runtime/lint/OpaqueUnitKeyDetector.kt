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

package androidx.compose.runtime.lint

import androidx.compose.lint.Names
import androidx.compose.lint.isInPackageName
import androidx.compose.lint.isVoidOrUnit
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils.Companion.tryResolveUDeclaration
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import java.util.EnumSet
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UDeclarationsExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UExpressionList
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElement

/**
 * Detector to warn when [Unit] is being passed "opaquely" as an argument to any of the methods in
 * [getApplicableMethodNames]. An argument is defined as an opaque unit key if all the following
 * are true:
 *  - The argument is an expression of type `Unit`
 *  - The argument is being passed to a parameter of type `Any?`
 *  - The argument is not the `Unit` literal
 *  - The argument is not a trivial variable or property read expression
 */
class OpaqueUnitKeyDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> = listOf(
        Names.Runtime.Remember.shortName,
        Names.Runtime.RememberSaveable.shortName,
        Names.Runtime.DisposableEffect.shortName,
        Names.Runtime.LaunchedEffect.shortName,
        Names.Runtime.ProduceState.shortName,
        Names.Runtime.ReusableContent.shortName,
        Names.Runtime.Key.shortName,
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(Names.Runtime.PackageName)) return

        method.parameterList.parameters.forEach { parameter ->
            val arg = node.getArgumentForParameter(parameter.parameterIndex())
            if (parameter.isNullableAny()) {
                if (arg?.isOpaqueUnitExpression() == true) {
                    reportOpaqueUnitArgKey(
                        context = context,
                        method = method,
                        methodInvocation = node,
                        parameter = parameter,
                        argument = arg
                    )
                }
            } else if (parameter.isPotentiallyVarArgs() && arg is UExpressionList) {
                arg.expressions.forEach { varArg ->
                    if (varArg.isOpaqueUnitExpression()) {
                        reportOpaqueUnitArgKey(
                            context = context,
                            method = method,
                            methodInvocation = node,
                            parameter = parameter,
                            argument = varArg
                        )
                    }
                }
            }
        }
    }

    private fun reportOpaqueUnitArgKey(
        context: JavaContext,
        method: PsiMethod,
        methodInvocation: UCallExpression,
        parameter: PsiParameter,
        argument: UExpression
    ) {
        val rootExpression = methodInvocation.resolveRootExpression()
        val rootExpressionLocation = context.getLocation(rootExpression)

        context.report(
            OpaqueUnitKey,
            argument,
            context.getLocation(argument),
            "Implicitly passing `Unit` as argument to ${parameter.name}",
            fix()
                .name(
                    "Move expression outside of `${method.name}`'s arguments " +
                        "and pass `Unit` explicitly"
                )
                .composite(
                    if (rootExpression.isInPhysicalBlock()) {
                        // If we're in a block where we can add an expression without breaking any
                        // syntax rules, promote the argument's expression to a sibling.
                        fix()
                            .replace()
                            .range(rootExpressionLocation)
                            .beginning()
                            .with("${argument.asSourceString()}\n")
                            .reformat(true)
                            .build()
                    } else {
                        // If we're not in a block, then introduce one for cheap by wrapping the
                        // call with Kotlin's `run` function to a format that appears as follows:
                        //
                        // ```
                        // run {
                        //    theArgument()
                        //    theMethod(...)
                        // }
                        // ```
                        fix()
                            .composite(
                                fix()
                                    .replace()
                                    .range(rootExpressionLocation)
                                    .beginning()
                                    .with("kotlin.run {\n${argument.asSourceString()}\n")
                                    .reformat(true)
                                    .shortenNames()
                                    .build(),
                                fix()
                                    .replace()
                                    .range(rootExpressionLocation)
                                    .end()
                                    .with("\n}")
                                    .reformat(true)
                                    .build()
                            )
                    },

                    // Replace the old parameter with the Unit literal
                    fix()
                        .replace()
                        .range(context.getLocation(argument))
                        .with(FqUnitName)
                        .shortenNames()
                        .build(),
                )
        )
    }

    private fun UCallExpression.resolveRootExpression(): UExpression {
        var root: UExpression = this
        var parent: UExpression? = root.getParentExpression()
        while (parent != null && parent !is UBlockExpression) {
            if (!parent.isVirtual) { root = parent }
            parent = parent.getParentExpression()
        }
        return root
    }

    private fun UExpression.isInPhysicalBlock(): Boolean {
        var parent: UElement? = this
        while (parent != null) {
            if (parent is UBlockExpression) {
                return !parent.isVirtual
            }
            parent = parent.uastParent
        }
        return false
    }

    private val UElement.isVirtual get() = sourcePsi == null

    private fun UExpression.getParentExpression(): UExpression? {
        return when (val parent = uastParent) {
            is UVariable -> parent.uastParent as UDeclarationsExpression
            is UExpression -> parent
            else -> null
        }
    }

    private fun PsiParameter.isNullableAny(): Boolean {
        val element = toUElement() as UParameter
        return element.type.canonicalText == FqJavaObjectName &&
            element.getAnnotations().any { it.qualifiedName == FqKotlinNullableAnnotation }
    }

    private fun PsiParameter.isPotentiallyVarArgs(): Boolean {
        return type.canonicalText == "$FqJavaObjectName[]"
    }

    private fun UExpression.isOpaqueUnitExpression(): Boolean {
        return getExpressionType().isVoidOrUnit && !isUnitLiteral()
    }

    private fun UExpression.isUnitLiteral(): Boolean {
        val expr = skipParenthesizedExprDown() ?: this
        if (expr !is USimpleNameReferenceExpression) return false

        return (expr.tryResolveUDeclaration() as? UClass)?.qualifiedName == FqUnitName
    }

    companion object {
        private const val FqJavaObjectName = "java.lang.Object"
        private const val FqUnitName = "kotlin.Unit"
        private const val FqKotlinNullableAnnotation = "org.jetbrains.annotations.Nullable"

        val OpaqueUnitKey = Issue.create(
            "OpaqueUnitKey",
            "Passing an expression which always returns `Unit` as a key argument",
            "Certain Compose functions including `remember`, `LaunchedEffect`, and " +
                "`DisposableEffect` declare (and sometimes require) one or more key parameters. " +
                "When a key parameter changes, it is a signal that the previous invocation is " +
                "now invalid. In certain cases, it may be required to pass `Unit` as a key to " +
                "one of these functions, indicating that the invocation never becomes invalid. " +
                "Using `Unit` as a key should be done infrequently, and should always be done " +
                "explicitly by passing the `Unit` literal. This inspection checks for " +
                "invocations where `Unit` is being passed as a key argument in any form other " +
                "than the `Unit` literal. This is usually done by mistake, and can harm " +
                "readability. If a Unit expression is being passed as a key, it is always " +
                "equivalent to move the expression before the function invocation and pass the " +
                "`Unit` literal instead.",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                OpaqueUnitKeyDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}