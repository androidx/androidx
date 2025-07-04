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

package androidx.build.lint

import com.android.SdkConstants.ATTR_VALUE
import com.android.tools.lint.checks.TypedefDetector
import com.android.tools.lint.detector.api.AnnotationInfo
import com.android.tools.lint.detector.api.AnnotationOrigin
import com.android.tools.lint.detector.api.AnnotationUsageInfo
import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import com.android.tools.lint.detector.api.getMethodName
import com.android.tools.lint.detector.api.isUnconditionalReturn
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralValue
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiStatement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtContainerNode
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.USwitchClauseExpression
import org.jetbrains.uast.UUnaryExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.UastFacade
import org.jetbrains.uast.UastPrefixOperator
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.util.isConstructorCall

/**
 * Enforced flag checking in the Android platform; see go/android-flagged-apis.
 *
 * **NOTE:** This file is a fork of the original sources in the Android lint code base, see
 * `lint-checks/src/main/java/com/android/tools/lint/checks/optional/FlaggedApiDetector.kt`
 */
class FlaggedApiDetector : Detector(), SourceCodeScanner {

    companion object Issues {
        private val IMPLEMENTATION =
            Implementation(FlaggedApiDetector::class.java, Scope.JAVA_FILE_SCOPE)

        private val ALLOWLIST_OPTION =
            StringOption(
                name = "allowlist",
                description =
                    "Comma-delimited list of libraries which are allowed to call flagged " +
                        "APIs, where `groupId:artifactId` represents a single module and `groupId` " +
                        "represents an entire group of modules. This may only be used by the AndroidX " +
                        "Infra team",
            )

        /** Accessing flagged api without check. */
        @JvmField
        val ISSUE =
            Issue.create(
                    id = "AndroidXFlaggedApi",
                    explanation =
                        """
          This lint check looks for accesses of APIs marked with `@FlaggedApi(X)` without \
          a guarding `if (Flags.X)` check or equivalent gating check. See go/android-flagged-apis.
          """,
                    briefDescription = "FlaggedApi access without check",
                    category = Category.CORRECTNESS,
                    priority = 6,
                    severity = Severity.ERROR,
                    androidSpecific = true,
                    implementation = IMPLEMENTATION,
                )
                .setOptions(listOf(ALLOWLIST_OPTION))

        // Message embedded in the autofix reminding the developer to implement a fallback.
        private const val TODO_FALLBACK_MESSAGE = "Implement fallback behavior"

        private const val COMPAT_FLAGS_CLASS = "androidx.core.flagging.Flags"
        private const val COMPAT_FLAGS_COMPANION_CLASS = "androidx.core.flagging.Flags.Companion"
        private const val CHECKS_ACONFIG_FLAG_ANNOTATION = "androidx.annotation.ChecksAconfigFlag"
        private const val REQUIRES_ACONFIG_FLAG_ANNOTATION =
            "androidx.annotation.RequiresAconfigFlag"
        private const val ATTR_FLAG = "flag"
        private const val FLAGGED_API_ANNOTATION = "android.annotation.FlaggedApi"
    }

    override fun applicableAnnotations(): List<String> {
        return listOf(FLAGGED_API_ANNOTATION, REQUIRES_ACONFIG_FLAG_ANNOTATION)
    }

    override fun isApplicableAnnotationUsage(type: AnnotationUsageType): Boolean {
        return when (type) {
            AnnotationUsageType.METHOD_CALL,
            AnnotationUsageType.METHOD_REFERENCE,
            AnnotationUsageType.FIELD_REFERENCE,
            AnnotationUsageType.CLASS_REFERENCE,
            AnnotationUsageType.ANNOTATION_REFERENCE,
            AnnotationUsageType.EXTENDS,
            AnnotationUsageType.DEFINITION -> true
            else -> false
        }
    }

    override fun inheritAnnotation(annotation: String): Boolean {
        return false
    }

    override fun visitAnnotationUsage(
        context: JavaContext,
        element: UElement,
        annotationInfo: AnnotationInfo,
        usageInfo: AnnotationUsageInfo,
    ) {
        val flagString = getFlaggedApiString(context, annotationInfo.annotation)
        if (flagString == null) {
            context.report(
                ISSUE,
                element,
                context.getLocation(element),
                "Failed to obtain flag string",
            )
            return
        }

        // Avoid checking usage of the `@FlaggedApi` or `@RequiresAconfigFlag` annotations
        // themselves.
        if (annotationInfo.origin == AnnotationOrigin.SELF) {
            if (
                annotationInfo.qualifiedName == FLAGGED_API_ANNOTATION ||
                    annotationInfo.qualifiedName == REQUIRES_ACONFIG_FLAG_ANNOTATION
            ) {
                return
            }
        } else if (isAlreadyAnnotated(context, element, flagString)) {
            return
        }

        // Avoid checking flagged deprecations. We don't allow adding APIs as deprecated, so we can
        // safely assume that the flag applies to the deprecated state rather than the API itself.
        if (isFlaggedDeprecation(usageInfo)) return

        // Only allowlisted libraries are allowed to call flagged APIs.
        if (!isUsageInAllowlistedLibrary(context, usageInfo.usage)) {
            context.report(
                ISSUE,
                element,
                context.getLocation(element),
                "Flagged APIs are subject to additional policies and may only be called by " +
                    "libraries that have been allowlisted by Jetpack Working Group",
            )
            return
        }

        // Only alpha libraries are allowed to call flagged APIs.
        if (!isUsageInAlphaLibrary(context, usageInfo.usage)) {
            context.report(
                ISSUE,
                element,
                context.getLocation(element),
                "Flagged APIs may only be called during alpha and must be removed before moving " +
                    "to beta",
            )
            return
        }

        // Is the usage checked? Great.
        if (isFlagChecked(context, element, flagString)) return

        val referenced = element.tryResolve()
        val description =
            when {
                referenced is PsiMethod -> "Method `${referenced.name}()`"
                element is UCallExpression ->
                    if (element.isConstructorCall()) {
                        val className = (element.classReference?.tryResolve() as? PsiClass)?.name
                        "Constructor for class `$className`"
                    } else {
                        "Method `${getMethodName(element)}()`"
                    }
                referenced is PsiField -> "Field `${referenced.name}`"
                referenced is PsiClass -> "Class `${referenced.name}`"
                element is UClassLiteralExpression ->
                    "Class `${element.expression?.sourcePsi?.text}`"
                referenced is PsiNamedElement -> "Reference `${referenced.name}`"
                else -> "This"
            }
        val message =
            "$description is a flagged API and must be inside a flag check for \"$flagString\""
        val quickfixData = autoFixWithFlagCheck(context, element, flagString)
        context.report(ISSUE, element, context.getLocation(element), message, quickfixData)
    }

    private val AnnotationUsageInfo.referencedElement: UElement?
        get() =
            referenced.toUElement()
                ?: if ((usage as? UCallExpression)?.isConstructorCall() == true) {
                    (usage as UCallExpression).classReference?.tryResolve().toUElement()
                } else {
                    null
                }

    private fun isUsageInAllowlistedLibrary(context: JavaContext, usage: UElement): Boolean =
        context.getAllowlistedCoordinates().let { allowlistedCoordinates ->
            (context.evaluator.getLibrary(usage) ?: context.project.mavenCoordinate)?.let {
                allowlistedCoordinates.contains(it.groupId) ||
                    allowlistedCoordinates.contains("${it.groupId}:${it.artifactId}")
            } ?: true // If we can't obtain the Maven coordinate, assume we're in a lint test.
        }

    private fun isUsageInAlphaLibrary(context: JavaContext, usage: UElement): Boolean =
        (context.evaluator.getLibrary(usage) ?: context.project.mavenCoordinate)?.version?.let {
            it.substringAfter('-').startsWith("alpha") || it == "unspecified" || it.isEmpty()
        } ?: true // If we can't obtain the Maven coordinate, assume we're in a lint test or app.

    private fun isFlaggedDeprecation(usageInfo: AnnotationUsageInfo): Boolean =
        (usageInfo.referencedElement as? UAnnotated)?.let {
            it.findAnnotation("java.lang.Deprecated") != null ||
                it.findAnnotation("kotlin.Deprecated") != null
        } == true

    private fun getFlaggedApiString(context: JavaContext, annotation: UAnnotation): String? =
        annotation.findAttributeValue(ATTR_VALUE)?.let { value ->
            ConstantEvaluator.evaluate(context, value)
        } as? String

    /**
     * Is the given [element] within a code block already annotated with the same flagged api as
     * [flagString].
     */
    private fun isAlreadyAnnotated(
        context: JavaContext,
        element: UElement?,
        flagString: String,
    ): Boolean {
        var current = element
        while (current != null) {
            if (current is UAnnotated) {
                //noinspection AndroidLintExternalAnnotations
                for (annotation in current.uAnnotations) {
                    if (!applicableAnnotations().contains(annotation.qualifiedName)) continue
                    val flag = getFlaggedApiString(context, annotation) ?: continue
                    if (flag == flagString) return true
                }
            }
            if (current is UAnnotation) {
                if (TypedefDetector.isTypeDef(current.qualifiedName)) {
                    return true
                }
            } else if (current is UFile) {
                // Also consult any package annotations
                val pkg = context.evaluator.getPackage(current.javaPsi ?: current.sourcePsi)
                if (pkg != null) {
                    for (psiAnnotation in pkg.annotations) {
                        val annotation =
                            UastFacade.convertElement(psiAnnotation, null) as? UAnnotation
                                ?: continue
                        if (!applicableAnnotations().contains(annotation.qualifiedName)) continue
                        val flag = getFlaggedApiString(context, annotation) ?: continue
                        if (flag == flagString) return true
                    }
                }

                break
            }
            current = current.uastParent
        }

        return false
    }

    /** Is the given [element] inside a flag check? */
    private fun isFlagChecked(
        context: JavaContext,
        element: UElement,
        flagString: String,
    ): Boolean {
        var curr = element.uastParent ?: return false

        var prev = element
        while (curr !is UFile) {
            if (curr is UIfExpression) {
                val condition = curr.condition
                if (prev !== condition) {
                    val fromThen = prev == curr.thenExpression
                    if (fromThen) {
                        if (isFlagExpression(context, condition, flagString)) {
                            return true
                        }
                    } else {
                        // Handle "if (!Flags.X) else <CALL>"
                        val op = condition.skipParenthesizedExprDown()
                        if (
                            op is UUnaryExpression &&
                                op.operator == UastPrefixOperator.LOGICAL_NOT &&
                                isFlagExpression(context, op.operand, flagString)
                        ) {
                            return true
                        } else if (
                            op is UPolyadicExpression &&
                                op.operator == UastBinaryOperator.LOGICAL_OR &&
                                (op.operands.any {
                                    val nested = it.skipParenthesizedExprDown()
                                    nested is UUnaryExpression &&
                                        nested.operator == UastPrefixOperator.LOGICAL_NOT &&
                                        isFlagExpression(context, nested.operand, flagString)
                                })
                        ) {
                            return true
                        }
                    }
                }
            } else if (curr is USwitchClauseExpression) {
                if (curr.caseValues.any { value -> isFlagExpression(context, value, flagString) }) {
                    return true
                }
            } else if (
                curr is UPolyadicExpression && curr.operator == UastBinaryOperator.LOGICAL_AND
            ) {
                for (operand in curr.operands) {
                    if (operand === curr) {
                        break
                    } else if (isFlagExpression(context, operand, flagString)) {
                        return true
                    }
                }
            } else if (curr is UMethod) {
                // See if there's an early return. We *only* handle a very simple canonical format
                // here;
                // must be first statement in method.
                val body = curr.uastBody
                if (body is UBlockExpression && body.expressions.size > 1) {
                    val first = body.expressions[0]
                    if (first is UIfExpression) {
                        val condition = first.condition.skipParenthesizedExprDown()
                        if (
                            condition is UUnaryExpression &&
                                condition.operator == UastPrefixOperator.LOGICAL_NOT &&
                                isFlagExpression(context, condition.operand, flagString)
                        ) {
                            // It's a flag check; make sure we just return
                            val then = first.thenExpression?.skipParenthesizedExprDown()
                            if (then != null && then.isUnconditionalReturn()) {
                                return true
                            }
                        }
                    }
                }
            }

            prev = curr
            curr = curr.uastParent ?: break
        }

        return false
    }

    /** Is the given [element] a flag expression (e.g. "Flags.set()") or equivalently annotated? */
    private fun isFlagExpression(
        context: JavaContext,
        element: UElement,
        flagString: String,
    ): Boolean {
        if (element is UUnaryExpression && element.operator == UastPrefixOperator.LOGICAL_NOT) {
            return !isFlagExpression(context, element.operand, flagString)
        } else if (element is UReferenceExpression || element is UCallExpression) {
            val resolved = element.tryResolve()
            if (resolved is PsiMethod) {
                if (
                    (resolved.toUElement() as UAnnotated)
                        .uAnnotations
                        .filter { it.qualifiedName == CHECKS_ACONFIG_FLAG_ANNOTATION }
                        .mapNotNull {
                            val attr =
                                it.findAttributeValue(ATTR_FLAG)
                                    ?: it.findAttributeValue(null)
                                    ?: return@mapNotNull null
                            attr.evaluateString()
                                ?: (attr.javaPsi as? PsiLiteralValue)?.value as? String
                        }
                        .contains(flagString)
                ) {
                    return true
                }
                // Is this a call to a flag check method on the AndroidX Flags compat class?
                if (
                    (resolved.containingClass?.qualifiedName == COMPAT_FLAGS_CLASS ||
                        resolved.containingClass?.qualifiedName == COMPAT_FLAGS_COMPANION_CLASS) &&
                        resolved.name.startsWith("get") &&
                        resolved.name.endsWith("FlagValue") &&
                        element is UQualifiedReferenceExpression
                ) {
                    val selector = element.selector
                    if (selector is UCallExpression && selector.valueArgumentCount >= 2) {
                        val flagPackage = flagString.substringBeforeLast('.')
                        val flagName = flagString.substringAfterLast('.')
                        val arg1 =
                            ConstantEvaluator.evaluate(
                                context,
                                selector.getArgumentForParameter(0)!!,
                            )
                        val arg2 =
                            ConstantEvaluator.evaluate(
                                context,
                                selector.getArgumentForParameter(1)!!,
                            )
                        if (arg1 == flagPackage && arg2 == flagName) return true
                    }
                }
            } else if (resolved is PsiField) {
                // Arguably we should look for final fields here, but on the other hand
                // there may be cases where it's initialized later, so it's a bit like
                // Kotlin's "lateinit". Treat them all as constant.
                val initializer = UastFacade.getInitializerBody(resolved)
                if (initializer != null) {
                    return isFlagExpression(context, initializer, flagString)
                }
            }
        } else if (element is UParenthesizedExpression) {
            return isFlagExpression(context, element.expression, flagString)
        } else if (element is UPolyadicExpression) {
            if (element.operator == UastBinaryOperator.LOGICAL_AND) {
                for (operand in element.operands) {
                    if (isFlagExpression(context, operand, flagString)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun autoFixWithFlagCheck(
        context: JavaContext,
        usage: UElement,
        flagString: String,
    ): LintFix? {
        val flagPackage = flagString.substringBeforeLast('.')
        val flagName = flagString.substringAfterLast('.')
        val presentation = usage.sourcePsi?.getPresentation() ?: return null
        val condition =
            "androidx.core.flagging.Flags.getBooleanFlagValue(\"$flagPackage\", \"$flagName\")"
        val oldText = presentation.text
        val todoText =
            when (usage.lang) {
                KotlinLanguage.INSTANCE -> "TODO(\"$TODO_FALLBACK_MESSAGE\")"
                JavaLanguage.INSTANCE ->
                    "throw new RuntimeException(\"TODO: $TODO_FALLBACK_MESSAGE\");"
                else -> return null
            }

        return fix()
            .replace()
            .name("Wrap with flag check")
            .range(context.getLocation(presentation))
            .reformat(true)
            .with("if ($condition) { $oldText } else { $todoText }")
            .build()
    }

    /** Returns an element that is suitable for wrapping with an `if` check. */
    private fun PsiElement.getPresentation(): PsiElement =
        when (language) {
            JavaLanguage.INSTANCE ->
                // For Java, take the first enclosing statement.
                findParent(withSelf = true) { it is PsiStatement } ?: this
            KotlinLanguage.INSTANCE ->
                // For Kotlin, expand the enclosing element until we reach something that is not a
                // valid target expression.
                findParent(withSelf = true) { it?.parent.isInvalidTargetKtExpression() } ?: this
            else -> this
        }

    /**
     * Returns the first parent element -- or the element itself when [withSelf] if `true` -- that
     * matches the [predicate].
     */
    private fun PsiElement.findParent(
        withSelf: Boolean,
        predicate: (PsiElement?) -> Boolean,
    ): PsiElement? {
        var current = if (withSelf) this else this.parent
        while (current != null && !predicate(current)) {
            current = current.parent
        }
        return current
    }

    /**
     * Returns whether a Kotlin element should not be wrapped with an `if` statement.
     *
     * This is adapted from a method in Android Lint's `AddTargetVersionCheckQuickFix` class.
     */
    private fun PsiElement?.isInvalidTargetKtExpression(): Boolean {
        return this is KtBlockExpression ||
            this is KtContainerNode ||
            this is KtWhenEntry ||
            this is KtFunction ||
            this is KtPropertyAccessor ||
            this is KtProperty ||
            this is KtReturnExpression ||
            this is KtDestructuringDeclaration ||
            this is KtClassInitializer
    }

    fun JavaContext.getAllowlistedCoordinates(): List<String> =
        ALLOWLIST_OPTION.getValue(this)?.split(',') ?: emptyList()
}
