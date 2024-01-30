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

package androidx.lifecycle.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.UastLintUtils
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiVariable
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.PsiImmediateClassType
import org.jetbrains.kotlin.asJava.elements.KtLightTypeParameter
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.getUastParentOfType
import org.jetbrains.uast.isNullLiteral
import org.jetbrains.uast.kotlin.KotlinUField
import org.jetbrains.uast.kotlin.KotlinUSimpleReferenceExpression
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.toUElement

/**
 * Lint check for ensuring that [androidx.lifecycle.MutableLiveData] values are never null when
 * the type is defined as non-nullable in Kotlin.
 */
class NonNullableMutableLiveDataDetector : Detector(), UastScanner {

    companion object {
        val ISSUE = Issue.Companion.create(
            id = "NullSafeMutableLiveData",
            briefDescription = "LiveData value assignment nullability mismatch",
            explanation = """This check ensures that LiveData values are not null when explicitly \
                declared as non-nullable.

                Kotlin interoperability does not support enforcing explicit null-safety when using \
                generic Java type parameters. Since LiveData is a Java class its value can always \
                be null even when its type is explicitly declared as non-nullable. This can lead \
                to runtime exceptions from reading a null LiveData value that is assumed to be \
                non-nullable.""",
            category = Category.INTEROPERABILITY_KOTLIN,
            severity = Severity.FATAL,
            implementation = Implementation(
                NonNullableMutableLiveDataDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            ),
            androidSpecific = true
        )
    }

    val typesMap = HashMap<String, KtTypeReference>()

    val methods = listOf("setValue", "postValue")

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UCallExpression::class.java, UClass::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return object : UElementHandler() {
            override fun visitClass(node: UClass) {
                for (element in node.uastDeclarations) {
                    if (element is KotlinUField) {
                        getFieldTypeReference(element)?.let {
                            // map the variable name to the type reference of its expression.
                            typesMap.put(element.name, it)
                        }
                    }
                }
            }

            private fun getFieldTypeReference(element: KotlinUField): KtTypeReference? {
                // If field has type reference, we need to use type reference
                // Given the field `val liveDataField: MutableLiveData<Boolean> = MutableLiveData()`
                // reference: `MutableLiveData<Boolean>`
                // argument: `Boolean`
                val typeReference = element.sourcePsi
                    ?.children
                    ?.firstOrNull { it is KtTypeReference } as? KtTypeReference
                val typeArgument = typeReference?.typeElement?.typeArgumentsAsTypes?.singleOrNull()
                if (typeArgument != null) {
                    return typeArgument
                }

                // We need to extract type from the call expression
                // Given the field `val liveDataField = MutableLiveData<Boolean>()`
                // expression: `MutableLiveData<Boolean>()`
                // argument: `Boolean`
                val expression = element.sourcePsi
                    ?.children
                    ?.firstOrNull { it is KtCallExpression } as? KtCallExpression
                return expression?.typeArguments?.singleOrNull()?.typeReference
            }

            override fun visitCallExpression(node: UCallExpression) {
                if (!isKotlin(node.sourcePsi) || !methods.contains(node.methodName) ||
                    !context.evaluator.isMemberInSubClassOf(
                            node.resolve()!!, "androidx.lifecycle.LiveData", false
                        )
                ) return

                val receiverType = node.receiverType as? PsiClassType
                var liveDataType =
                    if (receiverType != null && receiverType.hasParameters()) {
                        val receiver =
                            (node.receiver as? KotlinUSimpleReferenceExpression)?.resolve()
                        val variable = (receiver as? PsiVariable)
                        val assignment = variable?.let {
                            UastLintUtils.findLastAssignment(it, node)
                        }
                        val constructorExpression = assignment?.sourcePsi as? KtCallExpression
                        constructorExpression?.typeArguments?.singleOrNull()?.typeReference
                    } else {
                        getTypeArg(receiverType)
                    }
                if (liveDataType == null) {
                    liveDataType = typesMap[getVariableName(node)] ?: return
                }
                checkNullability(liveDataType, context, node)
            }

            private fun getVariableName(node: UCallExpression): String? {
                // We need to get the variable this expression is being assigned to
                // Given the assignment `liveDataField.value = null`
                // node.sourcePsi : `value`
                // dot: `.`
                // variable: `liveDataField`
                val dot = generateSequence(node.sourcePsi?.prevSibling) {
                    it.prevSibling
                }.firstOrNull { it !is PsiWhiteSpace }
                val variable = generateSequence(generateSequence(dot?.prevSibling) {
                    it.prevSibling
                }.firstOrNull { it !is PsiWhiteSpace }) {
                    it.firstChild
                }.firstOrNull { it !is PsiWhiteSpace }
                return variable?.text
            }
        }
    }

    /**
     * Iterates [classType]'s hierarchy to find its [androidx.lifecycle.LiveData] value type.
     *
     * @param classType The [PsiClassType] to search
     * @return The LiveData type argument.
     */
    fun getTypeArg(classType: PsiClassType?): KtTypeReference? {
        if (classType == null) {
            return null
        }
        val cls = classType.resolve().getUastParentOfType<UClass>()
        val parentPsiType = cls?.superClassType as PsiClassType
        if (parentPsiType.hasParameters()) {
            val parentTypeReference = cls.uastSuperTypes[0]
            val superType = (parentTypeReference.sourcePsi as KtTypeReference).typeElement
            return superType!!.typeArgumentsAsTypes[0]
        }
        return getTypeArg(parentPsiType)
    }

    fun checkNullability(
        liveDataType: KtTypeReference,
        context: JavaContext,
        node: UCallExpression
    ) {
        // ignore generic types
        if (node.isGenericTypeDefinition()) return

        if (liveDataType.typeElement !is KtNullableType) {
            val fixes = mutableListOf<LintFix>()
            if (context.getLocation(liveDataType).file == context.file) {
                // Quick Fixes can only be applied to current file
                fixes.add(
                    fix().name("Change `LiveData` type to nullable")
                        .replace().with("?").range(context.getLocation(liveDataType)).end().build()
                )
            }
            val argument = node.valueArguments[0]
            if (argument.isNullLiteral()) {
                // Don't report null!! quick fix.
                checkNullability(
                    context,
                    argument,
                    "Cannot set non-nullable LiveData value to `null`",
                    fixes
                )
            } else if (argument.isNullable(context)) {
                fixes.add(
                    fix().name("Add non-null asserted (!!) call")
                        .replace().with("!!").range(context.getLocation(argument)).end().build()
                )
                checkNullability(context, argument, "Expected non-nullable value", fixes)
            }
        }
    }

    private fun UCallExpression.isGenericTypeDefinition(): Boolean {
        val classType = typeArguments.singleOrNull() as? PsiImmediateClassType
        val resolveGenerics = classType?.resolveGenerics()
        return resolveGenerics?.element is KtLightTypeParameter
    }

    /**
     * Reports a lint error at [element]'s location with message and quick fixes.
     *
     * @param context The lint detector context.
     * @param element The [UElement] to report this error at.
     * @param message The error message to report.
     * @param fixes The Lint Fixes to report.
     */
    private fun checkNullability(
        context: JavaContext,
        element: UElement,
        message: String,
        fixes: List<LintFix>
    ) {
        if (fixes.isEmpty()) {
            context.report(ISSUE, context.getLocation(element), message)
        } else {
            context.report(
                ISSUE, context.getLocation(element), message,
                fix().alternatives(*fixes.toTypedArray())
            )
        }
    }
}

/**
 * Checks if the [UElement] is nullable. Always returns `false` if the [UElement] is not a
 * [UReferenceExpression] or [UCallExpression].
 *
 * @return `true` if instance is nullable, `false` otherwise.
 */
internal fun UElement.isNullable(context: JavaContext): Boolean {
    if (this is UCallExpression) {
        val psiMethod = resolve() ?: return false
        val sourceMethod = psiMethod.toUElement()?.sourcePsi
        if (sourceMethod is KtCallableDeclaration) {
            // if we have source, check the suspend return type
            return sourceMethod.typeReference?.typeElement is KtNullableType
        }
        // Suspend functions have @Nullable Object return type in JVM
        val isSuspendMethod = !context.evaluator.isSuspend(psiMethod)
        return psiMethod.hasAnnotation(NULLABLE_ANNOTATION) && isSuspendMethod
    } else if (this is UReferenceExpression) {
        return (resolveToUElement() as? UAnnotated)?.findAnnotation(NULLABLE_ANNOTATION) != null
    }
    return false
}

const val NULLABLE_ANNOTATION = "org.jetbrains.annotations.Nullable"
