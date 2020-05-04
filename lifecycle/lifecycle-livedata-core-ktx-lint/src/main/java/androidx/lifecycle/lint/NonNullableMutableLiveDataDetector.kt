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

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.getUastParentOfType
import org.jetbrains.uast.isNullLiteral
import org.jetbrains.uast.kotlin.KotlinUSimpleReferenceExpression
import org.jetbrains.uast.resolveToUElement

/**
 * Lint check for ensuring that [androidx.lifecycle.MutableLiveData] values are never null when
 * the type is defined as non-nullable in Kotlin.
 */
class NonNullableMutableLiveDataDetector : Detector(), SourceCodeScanner {

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

    override fun getApplicableMethodNames(): List<String>? = listOf("setValue", "postValue")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!isKotlin(node.sourcePsi) || !context.evaluator.isMemberInSubClassOf(method,
                "androidx.lifecycle.LiveData", false)) return

        val receiverType = node.receiverType as PsiClassType
        val liveDataType = if (receiverType.hasParameters()) {
            val receiver = (node.receiver as? KotlinUSimpleReferenceExpression)?.resolve() ?: return
            val assignment = UastLintUtils.findLastAssignment(receiver as PsiVariable, node)
                ?: return
            val constructorExpression = assignment.sourcePsi as? KtCallExpression
            constructorExpression?.typeArguments?.singleOrNull()?.typeReference
        } else {
            getTypeArg(receiverType)
        } ?: return

        if (liveDataType.typeElement !is KtNullableType) {
            val fixes = mutableListOf<LintFix>()
            if (context.getLocation(liveDataType).file == context.file) {
                // Quick Fixes can only be applied to current file
                fixes.add(fix()
                    .name("Change `LiveData` type to nullable")
                    .replace()
                    .with("?")
                    .range(context.getLocation(liveDataType))
                    .end()
                    .build())
            }
            val argument = node.valueArguments[0]
            if (argument.isNullLiteral()) {
                // Don't report null!! quick fix.
                report(context, argument, "Cannot set non-nullable LiveData value to `null`",
                    fixes)
            } else if (argument.isNullable()) {
                fixes.add(fix()
                    .name("Add non-null asserted (!!) call")
                    .replace()
                    .with("!!")
                    .range(context.getLocation(argument))
                    .end()
                    .build())
                report(context, argument, "Expected non-nullable value", fixes)
            }
        }
    }

    /**
     * Iterates [classType]'s hierarchy to find its [androidx.lifecycle.LiveData] value type.
     *
     * @param classType The [PsiClassType] to search
     * @return The LiveData type argument.
     */
    private fun getTypeArg(classType: PsiClassType): KtTypeReference {
        val cls = classType.resolve().getUastParentOfType<UClass>()
        val parentPsiType = cls?.superClassType as PsiClassType
        if (parentPsiType.hasParameters()) {
            val parentTypeReference = cls.uastSuperTypes[0]
            val superType = (parentTypeReference.sourcePsi as KtTypeReference).typeElement
            return superType!!.typeArgumentsAsTypes[0]
        }
        return getTypeArg(parentPsiType)
    }

    /**
     * Reports a lint error at [element]'s location with message and quick fixes.
     *
     * @param context The lint detector context.
     * @param element The [UElement] to report this error at.
     * @param message The error message to report.
     * @param fixes The Lint Fixes to report.
     */
    private fun report(
        context: JavaContext,
        element: UElement,
        message: String,
        fixes: List<LintFix>
    ) {
        if (fixes.isEmpty()) {
            context.report(ISSUE, context.getLocation(element), message)
        } else {
            context.report(ISSUE, context.getLocation(element), message,
                fix().alternatives(*fixes.toTypedArray()))
        }
    }
}

/**
 * Checks if the [UElement] is nullable. Always returns `false` if the [UElement] is not a
 * [UReferenceExpression] or [UCallExpression].
 *
 * @return `true` if instance is nullable, `false` otherwise.
 */
internal fun UElement.isNullable(): Boolean {
    if (this is UCallExpression) {
        val psiMethod = resolve() ?: return false
        return psiMethod.hasAnnotation(NULLABLE_ANNOTATION)
    } else if (this is UReferenceExpression) {
        return (resolveToUElement() as UAnnotated).findAnnotation(NULLABLE_ANNOTATION) != null
    }
    return false
}

const val NULLABLE_ANNOTATION = "org.jetbrains.annotations.Nullable"
