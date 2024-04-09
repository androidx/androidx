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

package androidx.annotation.experimental.lint

import androidx.annotation.experimental.lint.ExperimentalDetector.Companion.JAVA_EXPERIMENTAL_ANNOTATION
import androidx.annotation.experimental.lint.ExperimentalDetector.Companion.JAVA_REQUIRES_OPT_IN_ANNOTATION
import androidx.annotation.experimental.lint.ExperimentalDetector.Companion.KOTLIN_EXPERIMENTAL_ANNOTATION
import androidx.annotation.experimental.lint.ExperimentalDetector.Companion.KOTLIN_REQUIRES_OPT_IN_ANNOTATION
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiField
import org.jetbrains.kotlin.name.Name
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UResolvable

class AnnotationRetentionDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UAnnotation::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return AnnotationChecker(context)
    }

    private inner class AnnotationChecker(val context: JavaContext) : UElementHandler() {
        override fun visitAnnotation(node: UAnnotation) {
            val annotated = node.uastParent as? UAnnotated ?: return
            val isKotlin = isKotlin(annotated.lang)
            val qualifiedName = node.qualifiedName

            if (isKotlin && qualifiedName == JAVA_REQUIRES_OPT_IN_ANNOTATION) {
                reportKotlinUsage(annotated)
            }

            when (qualifiedName) {
                KOTLIN_EXPERIMENTAL_ANNOTATION, KOTLIN_REQUIRES_OPT_IN_ANNOTATION,
                JAVA_EXPERIMENTAL_ANNOTATION, JAVA_REQUIRES_OPT_IN_ANNOTATION -> {
                    validateAnnotationRetention(annotated)
                }
            }
        }

        /**
         * Validates that the [annotated] element has the correct retention, reporting an issue
         * if it does not.
         */
        private fun validateAnnotationRetention(annotated: UAnnotated) {
            val isKotlin = isKotlin(annotated.lang)
            val annotations = context.evaluator.getAllAnnotations(annotated, false)

            val annotationClass: String
            val defaultRetention: String
            val expectedRetention: String

            if (isKotlin) {
                // The retention must be explicitly Kotlin BINARY. While Java CLASS is technically
                // correct, we prefer that Kotlin code be annotated with Kotlin annotations.
                expectedRetention = "BINARY"
                defaultRetention = "RUNTIME"
                annotationClass = "kotlin.annotation.Retention"
            } else {
                // The retention can either be default (which is CLASS) or explicitly Java CLASS.
                expectedRetention = "CLASS"
                defaultRetention = "CLASS"
                annotationClass = "java.lang.annotation.Retention"
            }

            val actualRetention = annotations.find { annotation ->
                annotationClass == annotation.qualifiedName
            }?.extractAttribute(context, "value") ?: defaultRetention

            if (expectedRetention != actualRetention) {
                reportRetention(
                    annotated,
                    formatRetention(expectedRetention, defaultRetention),
                    formatRetention(actualRetention, defaultRetention),
                )
            }
        }

        /**
         * Formats [retention] for presentation in an error message by adding code font and
         * labeling it as the default value, if applicable.
         */
        private fun formatRetention(retention: String, defaultRetention: String): String {
            return if (defaultRetention == retention) "default (`$retention`)" else "`$retention`"
        }

        /**
         * Reports an issue with the [annotated] element where the [expected] retention does not
         * match the [actual] retention.
         */
        private fun reportRetention(annotated: UAnnotated, expected: String, actual: String) {
            context.report(
                ISSUE_RETENTION, annotated, context.getNameLocation(annotated),
                "Experimental annotation has $actual retention, should use $expected"
            )
        }

        /**
         * Reports an issue with the [annotated] element where the experimental meta-annotation
         * should be changed to `kotlin.RequiresOptIn`.
         */
        private fun reportKotlinUsage(annotated: UAnnotated) {
            context.report(
                ISSUE_KOTLIN_USAGE, annotated, context.getNameLocation(annotated),
                "Experimental annotation should use kotlin.RequiresOptIn"
            )
        }
    }

    companion object {
        val ISSUE_KOTLIN_USAGE = Issue.create(
            "WrongRequiresOptIn",
            "Experimental annotations defined in Kotlin must use kotlin.RequiresOptIn",
            """
            Experimental features defined in Kotlin source code must be annotated with the Kotlin
            `@RequiresOptIn` annotation. Using `androidx.annotation.RequiresOptIn` will prevent the
            Kotlin compiler from enforcing its opt-in policies.
            """,
            Category.CORRECTNESS, 4, Severity.ERROR,
            Implementation(AnnotationRetentionDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        val ISSUE_RETENTION = Issue.create(
            "ExperimentalAnnotationRetention",
            "Experimental annotation with incorrect retention",
            "Experimental annotations defined in Java source should use default " +
                "(`CLASS`) retention, while Kotlin-sourced annotations should use `BINARY` " +
                "retention.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(AnnotationRetentionDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        val ISSUES = listOf(
            ISSUE_RETENTION,
            ISSUE_KOTLIN_USAGE
        )
    }
}

/**
 * Attempts to extract the name of the constant used for an attribute value, returning
 * [fallbackValue] if the attribute could not be found or `null` if it couldn't understand the
 * value representation.
 */
@Suppress("SameParameterValue")
fun UAnnotation.extractAttribute(
    context: JavaContext,
    attrName: String,
    fallbackValue: String? = null,
): String? {
    val attrValue = findAttributeValue(attrName) ?: return fallbackValue
    val value = attrValue.let {
        ConstantEvaluator.evaluate(context, it)
    } ?: (attrValue as? UResolvable)?.resolve()
    return when (value) {
        is PsiField -> value.name
        is Pair<*, *> -> (value.second as? Name)?.identifier
        else -> null
    }
}
