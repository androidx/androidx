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
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.lang.jvm.types.JvmType
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.impl.source.PsiClassReferenceType
import java.util.EnumSet
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.kotlin.KotlinULambdaExpression

/**
 * Lambdas with primitives will box the primitives on every call. This lint rule will find
 * such lambdas in method parameters and variables. We don't lint for Boolean because there
 * are only 3 possible values. We do lint for `value class` that wrap primitives as well.
 * Reference types, like [String] are ignored.
 */
class PrimitiveInLambdaDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf<Class<out UElement>>(
        UMethod::class.java,
        UVariable::class.java
    )

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitMethod(node: UMethod) {
            if (!isKotlin(node) ||
                context.evaluator.isOverride(node) ||
                (context.evaluator.isData(node.containingClass) &&
                    (node.name.startsWith("copy") || node.name.startsWith("component")))
            ) {
                return
            }

            val isInline = context.evaluator.isInline(node)

            if (!isInline && node.returnType?.hasLambdaWithPrimitive() == true) {
                // The location doesn't appear to work with property types with getters rather than
                // full fields. Target the property name instead if we don't have a location.
                val target = if (context.getLocation(node.returnTypeReference).start == null) {
                    node
                } else {
                    node.returnTypeReference
                }
                report(
                    context,
                    node,
                    target,
                    "return type ${node.returnType?.presentableText} of '${node.name}'"
                )
            }
        }

        override fun visitVariable(node: UVariable) {
            if (!isKotlin(node) || node is UField) {
                return
            }

            if (node.type.hasLambdaWithPrimitive()) {
                val parent = node.uastParent
                if (parent is KotlinULambdaExpression) {
                    val sourcePsi = node.sourcePsi
                    if (sourcePsi == null ||
                        (sourcePsi as? KtParameter)?.isLambdaParameter == true
                    ) {
                        return // Don't notify for lambda parameters
                    }
                }
                val messageContext = if (parent is UMethod) {
                    val isInline = context.evaluator.isInline(parent)
                    val isParameterNoInline = context.evaluator.isNoInline(node)

                    // don't care about inline parameters or parameters in override methods or
                    // generated methods for a data class
                    if ((isInline && !isParameterNoInline) ||
                        context.evaluator.isOverride(parent) ||
                        (context.evaluator.isData(parent) && parent.name.startsWith("copy"))
                    ) {
                        return
                    }
                    val methodName = if (parent.isConstructor) {
                        "constructor ${parent.containingClass?.name}"
                    } else {
                        "method ${parent.name}"
                    }
                    "$methodName has parameter '${node.name}' " +
                        "with type ${node.type.presentableText}"
                } else {
                    "variable '${node.name}' with type ${node.type.presentableText}"
                }
                report(
                    context,
                    node,
                    node.typeReference,
                    messageContext
                )
            }
        }
    }

    private fun report(context: JavaContext, node: UElement, target: Any?, messageContext: String) {
        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(target),
            message = "Use a functional interface instead of lambda syntax for lambdas with " +
                "primitive values in $messageContext."
        )
    }

    companion object {
        private val PrimitiveInLambdaId = "PrimitiveInLambda"

        val ISSUE = Issue.create(
            id = PrimitiveInLambdaId,
            briefDescription = "A primitive (Short, Int, Long, Char, Float, Double) or " +
                "a value class wrapping a primitive was used as a parameter or return type of a " +
                "lambda, causing autoboxing",
            explanation = "Using a primitive type in a lambda will autobox the primitive value, " +
                "causing an allocation. To avoid the allocation, use a functional interface that " +
                "explicitly accepts the primitive.",
            category = Category.PERFORMANCE, priority = 3, severity = Severity.ERROR,
            implementation = Implementation(
                PrimitiveInLambdaDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE)
            )
        )
    }
}

private const val FunctionPrefix = "kotlin.jvm.functions.Function"
private val JvmInlineAnnotation = JvmInline::class.qualifiedName!!

// Set of all boxed types that we want to prevent. We don't have to worry
// about Boolean or because the boxed values are kept and reused (and there are only 2).
private val BoxedPrimitives = setOf(
    "java.lang.Byte",
    "java.lang.Character",
    "java.lang.Short",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Float",
    "java.lang.Double",
    "kotlin.UByte",
    "kotlin.UShort",
    "kotlin.UInt",
    "kotlin.ULong",
)

private fun JvmType.hasLambdaWithPrimitive(): Boolean {
    if (isLambda() && hasPrimitiveTypeArgs()) {
        return true
    }
    if (this is PsiClassReferenceType) {
        for (typeArg in typeArguments()) {
            if (typeArg.hasLambdaWithPrimitive()) {
                return true
            }
        }
    }
    return false
}

private fun JvmType.isLambda(): Boolean =
    (this is PsiClassReferenceType && reference.qualifiedName.startsWith(FunctionPrefix))

private fun JvmType.hasPrimitiveTypeArgs(): Boolean {
    if (this !is PsiClassReferenceType) {
        return false
    }

    for (typeArg in typeArguments()) {
        val isPrimitive = when (typeArg) {
            is PsiClassReferenceType -> typeArg.isBoxedPrimitive()
            is PsiWildcardType -> {
                val bound = if (typeArg.isBounded) {
                    typeArg.bound!!
                } else {
                    typeArg.superBound
                }
                when (bound) {
                    is PsiClassReferenceType -> bound.isBoxedPrimitive()
                    is PsiPrimitiveType -> bound.boxedTypeName in BoxedPrimitives
                    else -> false
                }
            }

            else -> false
        }
        if (isPrimitive) {
            return true
        }
    }
    return false
}

private fun PsiClassReferenceType.isBoxedPrimitive(): Boolean {
    val resolvedType = resolve() ?: return false
    if (resolvedType is KtUltraLightClass && hasJvmInline(resolvedType)) {
        val constructorParam =
            resolvedType.constructors.firstOrNull()?.parameters?.firstOrNull()
        if (constructorParam != null) {
            val type = constructorParam.type
            if (type is PsiPrimitiveType) {
                return true
            }
            if (type is PsiClassReferenceType) {
                return type.isBoxedPrimitive()
            }
        }
    }
    return resolvedType.qualifiedName in BoxedPrimitives
}

private fun hasJvmInline(type: KtUltraLightClass): Boolean {
    for (annotation in type.annotations) {
        if (annotation.qualifiedName == JvmInlineAnnotation) {
            return true
        }
    }
    return false
}
