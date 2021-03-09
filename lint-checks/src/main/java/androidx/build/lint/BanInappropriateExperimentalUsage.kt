/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiCompiledElement
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.resolveToUElement

/**
 * Prevents usage of experimental annotations outside the groups in which they were defined.
 */
class BanInappropriateExperimentalUsage : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UAnnotation::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return AnnotationChecker(context)
    }

    private inner class AnnotationChecker(val context: JavaContext) : UElementHandler() {
        override fun visitAnnotation(node: UAnnotation) {
            val annotation = node.resolveToUElement()
            if (annotation is UAnnotated) {
                val annotations = context.evaluator.getAllAnnotations(annotation, false)
                val isOptIn = annotations.any { APPLICABLE_ANNOTATIONS.contains(it.qualifiedName) }
                if (isOptIn) {
                    verifyUsageOfElementIsWithinSameGroup(context, node, annotation, ISSUE)
                }
            }
        }
    }

    fun verifyUsageOfElementIsWithinSameGroup(
        context: JavaContext,
        usage: UElement,
        annotation: UElement,
        issue: Issue,
    ) {
        val evaluator = context.evaluator
        val usageCoordinates = evaluator.getLibrary(usage) ?: context.project.mavenCoordinate
        val annotationCoordinates = evaluator.getLibrary(annotation) ?: run {
            // Is the annotation defined in source code?
            if (usageCoordinates != null && annotation !is PsiCompiledElement) {
                annotation.sourcePsi?.let { sourcePsi ->
                    evaluator.getProject(sourcePsi)?.mavenCoordinate
                }
            } else {
                null
            }
        }
        val usageGroupId = usageCoordinates?.groupId
        val annotationGroupId = annotationCoordinates?.groupId
        if (annotationGroupId != usageGroupId && annotationGroupId != null) {
            context.report(
                issue, usage, context.getNameLocation(usage),
                "`Experimental` and `RequiresOptIn` APIs may only be used within the same-version" +
                    " group where they were defined."
            )
        }
    }

    companion object {
        private const val KOTLIN_EXPERIMENTAL_ANNOTATION = "kotlin.Experimental"
        private const val KOTLIN_REQUIRES_OPT_IN_ANNOTATION = "kotlin.RequiresOptIn"
        private const val JAVA_EXPERIMENTAL_ANNOTATION =
            "androidx.annotation.experimental.Experimental"
        private const val JAVA_REQUIRES_OPT_IN_ANNOTATION =
            "androidx.annotation.RequiresOptIn"

        private val APPLICABLE_ANNOTATIONS = listOf(
            JAVA_EXPERIMENTAL_ANNOTATION,
            KOTLIN_EXPERIMENTAL_ANNOTATION,
            JAVA_REQUIRES_OPT_IN_ANNOTATION,
            KOTLIN_REQUIRES_OPT_IN_ANNOTATION,
        )

        val ISSUE = Issue.create(
            id = "IllegalExperimentalApiUsage",
            briefDescription = "Using experimental API from separately versioned library",
            explanation = "Annotations meta-annotated with `@RequiresOptIn` or `@Experimental` " +
                "may only be referenced from within the same-version group in which they were " +
                "defined.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                BanInappropriateExperimentalUsage::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}