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

@file:Suppress("UnstableApiUsage", "SyntheticAccessor")

package androidx.annotation.experimental.lint

import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.getParentOfType
import java.util.Locale

class ExperimentalDetector : Detector(), SourceCodeScanner {

    override fun applicableAnnotations(): List<String> = listOf(
        JAVA_EXPERIMENTAL_ANNOTATION,
        KOTLIN_EXPERIMENTAL_ANNOTATION,
        JAVA_REQUIRES_OPT_IN_ANNOTATION,
        KOTLIN_REQUIRES_OPT_IN_ANNOTATION
    )

    override fun visitAnnotationUsage(
        context: JavaContext,
        usage: UElement,
        type: AnnotationUsageType,
        annotation: UAnnotation,
        qualifiedName: String,
        method: PsiMethod?,
        referenced: PsiElement?,
        annotations: List<UAnnotation>,
        allMemberAnnotations: List<UAnnotation>,
        allClassAnnotations: List<UAnnotation>,
        allPackageAnnotations: List<UAnnotation>
    ) {
        when (qualifiedName) {
            JAVA_EXPERIMENTAL_ANNOTATION, JAVA_REQUIRES_OPT_IN_ANNOTATION -> {
                // Only allow Java annotations, since the Kotlin compiler doesn't understand our
                // annotations and could get confused when it's trying to opt-in to some random
                // annotation that it doesn't understand.
                checkExperimentalUsage(
                    context, annotation, usage,
                    listOf(
                        JAVA_USE_EXPERIMENTAL_ANNOTATION,
                        JAVA_OPT_IN_ANNOTATION
                    )
                )
            }
            KOTLIN_EXPERIMENTAL_ANNOTATION, KOTLIN_REQUIRES_OPT_IN_ANNOTATION -> {
                // Don't check usages of Kotlin annotations from Kotlin sources, since the Kotlin
                // compiler handles that already. Allow either Java or Kotlin annotations, since
                // we can enforce both and it's possible that a Kotlin-sourced experimental library
                // is being used from Java without the Kotlin stdlib in the classpath.
                if (!isKotlin(usage.sourcePsi)) {
                    checkExperimentalUsage(
                        context, annotation, usage,
                        listOf(
                            KOTLIN_USE_EXPERIMENTAL_ANNOTATION,
                            KOTLIN_OPT_IN_ANNOTATION,
                            JAVA_USE_EXPERIMENTAL_ANNOTATION,
                            JAVA_OPT_IN_ANNOTATION
                        )
                    )
                }
            }
        }
    }

    /**
     * Check whether the given experimental API [annotation] can be referenced from [usage] call
     * site.
     *
     * @param context the lint scanning context
     * @param annotation the experimental opt-in annotation detected on the referenced element
     * @param usage the element whose usage should be checked
     * @param useAnnotationNames fully-qualified class name for experimental opt-in annotation
     */
    private fun checkExperimentalUsage(
        context: JavaContext,
        annotation: UAnnotation,
        usage: UElement,
        useAnnotationNames: List<String>
    ) {
        val useAnnotation = (annotation.uastParent as? UClass)?.qualifiedName ?: return
        if (!hasOrUsesAnnotation(context, usage, useAnnotation, useAnnotationNames)) {
            val level = annotation.extractAttribute(context, "level") ?: "ERROR"
            report(
                context, usage, useAnnotation,
                """
                    This declaration is opt-in and its usage should be marked with
                    '@$useAnnotation' or '@OptIn(markerClass = $useAnnotation.class)'
                """,
                level
            )
        }
    }

    /**
     * Check whether the specified [usage] is either within the scope of [annotationName] or an
     * explicit opt-in via a [useAnnotationNames] annotation.
     */
    private fun hasOrUsesAnnotation(
        context: JavaContext,
        usage: UElement,
        annotationName: String,
        useAnnotationNames: List<String>
    ): Boolean {
        var element: UAnnotated? = if (usage is UAnnotated) {
            usage
        } else {
            usage.getParentOfType(UAnnotated::class.java)
        }

        while (element != null) {
            val annotations = context.evaluator.getAllAnnotations(element, false)

            val matchName = annotations.any { annotationName == it.qualifiedName }
            if (matchName) {
                return true
            }

            val matchUse = annotations.any { annotation ->
                val qualifiedName = annotation.qualifiedName
                if (qualifiedName != null && useAnnotationNames.contains(qualifiedName)) {
                    // Kotlin uses the same attribute for single- and multiple-marker usages.
                    if (annotation.hasMatchingAttributeValueClass(
                            context, "markerClass", annotationName
                        )
                    ) {
                        return@any true
                    }
                }

                return@any false
            }
            if (matchUse) {
                return true
            }

            element = element.getParentOfType(UAnnotated::class.java)
        }
        return false
    }

    /**
     * Reports an issue and trims indentation on the [message].
     */
    private fun report(
        context: JavaContext,
        usage: UElement,
        annotation: String,
        message: String,
        level: String,
    ) {
        val issue = when (level) {
            "ERROR" -> ISSUE_ERROR
            "WARNING" -> ISSUE_WARNING
            else -> throw IllegalArgumentException(
                "Level was \"" + level + "\" but must be one " +
                    "of: ERROR, WARNING"
            )
        }

        try {
            if (context.configuration.getOption(issue, "opt-in")?.contains(annotation) != true) {
                context.report(issue, usage, context.getNameLocation(usage), message.trimIndent())
            }
        } catch (e: UnsupportedOperationException) {
            if ("Method not implemented" == e.message) {
                // Workaround for b/191286558 where lint attempts to read annotations from a
                // compiled UAST parent of `usage`. Swallow the exception and don't report anything.
            } else {
                throw e
            }
        }
    }

    companion object {
        private val IMPLEMENTATION = Implementation(
            ExperimentalDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        const val KOTLIN_EXPERIMENTAL_ANNOTATION = "kotlin.Experimental"
        const val KOTLIN_USE_EXPERIMENTAL_ANNOTATION = "kotlin.UseExperimental"

        const val KOTLIN_OPT_IN_ANNOTATION = "kotlin.OptIn"
        const val KOTLIN_REQUIRES_OPT_IN_ANNOTATION = "kotlin.RequiresOptIn"

        const val JAVA_EXPERIMENTAL_ANNOTATION =
            "androidx.annotation.experimental.Experimental"
        const val JAVA_USE_EXPERIMENTAL_ANNOTATION =
            "androidx.annotation.experimental.UseExperimental"

        const val JAVA_REQUIRES_OPT_IN_ANNOTATION =
            "androidx.annotation.RequiresOptIn"
        const val JAVA_OPT_IN_ANNOTATION =
            "androidx.annotation.OptIn"

        @Suppress("DefaultLocale")
        private fun issueForLevel(level: String, severity: Severity): Issue = Issue.create(
            id = "UnsafeOptInUsage${level.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }}",
            briefDescription = "Unsafe opt-in usage intended to be $level-level severity",
            explanation = """
                This API has been flagged as opt-in with $level-level severity.

                Any declaration annotated with this marker is considered part of an unstable or
                otherwise non-standard API surface and its call sites should accept the opt-in
                aspect of it either by using `@OptIn` or by being annotated with that marker
                themselves, effectively causing further propagation of the opt-in aspect.
            """,
            category = Category.CORRECTNESS,
            priority = 4,
            severity = severity,
            implementation = IMPLEMENTATION
        )

        val ISSUE_ERROR =
            issueForLevel(
                "error",
                Severity.ERROR
            )
        val ISSUE_WARNING =
            issueForLevel(
                "warning",
                Severity.WARNING
            )

        val ISSUES = listOf(
            ISSUE_ERROR,
            ISSUE_WARNING
        )
    }
}

private fun UAnnotation.hasMatchingAttributeValueClass(
    context: JavaContext,
    attributeName: String,
    className: String
): Boolean {
    val attributeValue = findDeclaredAttributeValue(attributeName)
    if (attributeValue.getFullyQualifiedName(context) == className) {
        return true
    }
    if (attributeValue is UCallExpression) {
        return attributeValue.valueArguments.any { attrValue ->
            attrValue.getFullyQualifiedName(context) == className
        }
    }
    return false
}

/**
 * Returns the fully-qualified class name for a given attribute value, if any.
 */
private fun UExpression?.getFullyQualifiedName(context: JavaContext): String? {
    val type = if (this is UClassLiteralExpression) this.type else this?.evaluate()
    return (type as? PsiClassType)?.let { context.evaluator.getQualifiedName(it) }
}
