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

import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.model.LintModelMavenName
import com.intellij.psi.PsiCompiledElement
import java.io.File
import java.io.FileNotFoundException
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.kotlin.KotlinUVarargExpression
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.toUElement

/**
 * Prevents usage of experimental annotations outside the groups in which they were defined.
 */
class BanInappropriateExperimentalUsage : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UAnnotation::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return AnnotationChecker(context)
    }

    private inner class AnnotationChecker(val context: JavaContext) : UElementHandler() {
        val atomicGroupList: List<String> by lazy { loadAtomicLibraryGroupList() }

        override fun visitAnnotation(node: UAnnotation) {
            val signature = node.qualifiedName

            if (DEBUG) {
                if (APPLICABLE_ANNOTATIONS.contains(signature) && node.sourcePsi != null) {
                    (node.uastParent as? UClass)?.let { annotation ->
                        println(
                            "${context.driver.mode}: declared ${annotation.qualifiedName} in " +
                                "${context.project}"
                        )
                    }
                }
            }

            /**
             * If the annotation under evaluation is a form of @OptIn, extract and evaluate the
             * annotation(s) referenced by @OptIn - denoted by its markerClass.
             */
            if (signature != null && APPLICATION_OPT_IN_ANNOTATIONS.contains(signature)) {
                if (DEBUG) {
                    println("Found an @OptIn annotation. Attempting to find markerClass element(s)")
                }

                val markerClass: UExpression? = node.findAttributeValue("markerClass")
                if (markerClass != null) {
                    val markerClasses = getUElementsFromOptInMarkerClass(markerClass)

                    if (DEBUG && markerClasses.isNotEmpty()) {
                        println("Found ${markerClasses.size} markerClass(es): ")
                    }

                    markerClasses.forEach { uElement ->
                        if (DEBUG) {
                            println("Inspecting markerClass annotation " +
                                uElement.getQualifiedName())
                        }
                        inspectAnnotation(uElement, node)
                    }
                }

                /**
                 * @OptIn has no effect if its markerClass isn't provided.
                 * Similarly, if [getUElementsFromOptInMarkerClass] returns an empty list then
                 * there isn't anything more to inspect.
                 *
                 * In both of these cases we can stop processing here.
                 */
                return
            }

            inspectAnnotation(node.resolveToUElement(), node)
        }

        private fun getUElementsFromOptInMarkerClass(markerClass: UExpression): List<UElement> {
            val elements = ArrayList<UElement?>()

            when (markerClass) {
                is UClassLiteralExpression -> { // opting in to single annotation
                    elements.add(markerClass.toUElement())
                }
                is KotlinUVarargExpression -> { // opting in to multiple annotations
                    val expressions: List<UExpression> = markerClass.valueArguments
                    for (expression in expressions) {
                        val uElement = (expression as UClassLiteralExpression).toUElement()
                        elements.add(uElement)
                    }
                }
                else -> {
                    // do nothing
                }
            }

            return elements.filterNotNull()
        }

        private fun UClassLiteralExpression.toUElement(): UElement? {
            val psiType = this.type
            val psiClass = context.evaluator.getTypeClass(psiType)
            return psiClass.toUElement()
        }

        // If we find an usage of an experimentally-declared annotation, check it.
        private fun inspectAnnotation(annotation: UElement?, node: UAnnotation) {
            if (annotation is UAnnotated) {
                val annotations = context.evaluator.getAllAnnotations(annotation, false)
                if (annotations.any { APPLICABLE_ANNOTATIONS.contains(it.qualifiedName) }) {
                    if (DEBUG) {
                        println(
                            "${context.driver.mode}: used ${annotation.getQualifiedName()} in " +
                                context.project.mavenCoordinate.groupId
                        )
                    }
                    verifyUsageOfElementIsWithinSameGroup(
                        context,
                        node,
                        annotation,
                        ISSUE,
                        atomicGroupList
                    )
                }
            }
        }

        private fun loadAtomicLibraryGroupList(): List<String> {
            val fileStream = this::class.java.classLoader
                .getResourceAsStream(ATOMIC_LIBRARY_GROUPS_FILENAME)
                ?: throw FileNotFoundException(
                    "Couldn't find atomic library group file $ATOMIC_LIBRARY_GROUPS_FILENAME" +
                        " within lint-checks.jar")

            val atomicLibraryGroupsString = fileStream.bufferedReader().use { it.readText() }
            if (atomicLibraryGroupsString.isEmpty()) {
                throw RuntimeException("Atomic library group file should not be empty")
            }

            return atomicLibraryGroupsString.split("\n")
        }
    }

    fun verifyUsageOfElementIsWithinSameGroup(
        context: JavaContext,
        usage: UElement,
        annotation: UElement,
        issue: Issue,
        atomicGroupList: List<String>,
    ) {
        val evaluator = context.evaluator

        // The location where the annotation is used
        val usageCoordinates = evaluator.getLibrary(usage) ?: context.project.mavenCoordinate
        val usageGroupId = usageCoordinates?.groupId

        // The location where the annotation is declared
        // TODO (b/222554358): annotationGroup is (unexpectedly) null sometimes; fix this
        val annotationCoordinates = evaluator.getLibraryLocalMode(annotation) ?: return
        val annotationGroupId = annotationCoordinates.groupId

        val isUsedInSameGroup = usageCoordinates.groupId == annotationCoordinates.groupId
        val isUsedInSameArtifact = usageCoordinates.artifactId == annotationCoordinates.artifactId
        val isAtomic = atomicGroupList.contains(usageGroupId)

        val annotationQualifiedName = annotation.getQualifiedName()
        val isAnnotationAllowed = if (annotationQualifiedName != null) {
            isAnnotationAlwaysAllowed(annotationQualifiedName)
        } else {
            false
        }

        /**
         * Usage of experimental APIs is allowed in either of the following conditions:
         *
         * - Both the group ID and artifact ID in `usageCoordinates` and
         *   `annotationCoordinates` match
         * - The group IDs match, and that group ID is atomic
         * - The annotation being used is is an allowlist
         */
        if ((isUsedInSameGroup && isUsedInSameArtifact) ||
            (isUsedInSameGroup && isAtomic) ||
            isAnnotationAllowed) return

        // Log inappropriate experimental usage
        if (DEBUG) {
            println(
                "${context.driver.mode}: report usage of $annotationGroupId in $usageGroupId"
            )
        }
        Incident(context)
            .issue(issue)
            .at(usage)
            .message(
                "`Experimental` and `RequiresOptIn` APIs may only be used within the " +
                    "same-version group where they were defined."
            )
            .report()
    }

    /**
     * An implementation of [JavaEvaluator.getLibrary] that attempts to use the JAR path when we
     * can't find the project from the sourcePsi, even if the element isn't a compiled element.
     */
    private fun JavaEvaluator.getLibraryLocalMode(element: UElement): LintModelMavenName? {
        if (element !is PsiCompiledElement) {
            val coord = element.sourcePsi?.let { psi -> getProject(psi)?.mavenCoordinate }
            if (coord != null) {
                return coord
            }
        }
        val findJarPath = findJarPath(element)
        return if (findJarPath != null) {
            getLibrary(File(findJarPath))
        } else {
            null
        }
    }

    private fun UElement.getQualifiedName() = (this as UClass).qualifiedName

    companion object {
        private const val DEBUG = false

        /**
         * Even though Kotlin's [Experimental] annotation is deprecated in favor of [RequiresOptIn],
         * we still want to check for its use in Lint.
         */
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

        private val APPLICATION_OPT_IN_ANNOTATIONS = listOf(
            "androidx.annotation.OptIn",
            "kotlin.OptIn",
        )

        // This must match the definition in ExportAtomicLibraryGroupsToTextTask
        const val ATOMIC_LIBRARY_GROUPS_FILENAME = "atomic-library-groups.txt"

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

        /**
         * Checks to see if the given annotation is always allowed for use in @OptIn.
         * For now, allow Kotlin all stdlib experimental annotations.
         */
        internal fun isAnnotationAlwaysAllowed(annotation: String): Boolean {
            val allowedExperimentalAnnotations = listOf(
                Regex("kotlin\\..*"),
                Regex("kotlinx\\..*"),
            )
            return allowedExperimentalAnnotations.any {
                annotation.matches(it)
            }
        }
    }
}
