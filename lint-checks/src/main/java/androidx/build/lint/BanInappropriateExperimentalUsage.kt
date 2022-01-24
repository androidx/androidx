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

import com.android.tools.lint.client.api.Configuration
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import java.io.File
import java.io.FileNotFoundException
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
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
        val atomicGroupList: List<String> by lazy {
            loadAtomicLibraryGroupData(context.configuration, ISSUE)
        }

        override fun visitAnnotation(node: UAnnotation) {
            if (DEBUG) {
                if (APPLICABLE_ANNOTATIONS.contains(node.qualifiedName) && node.sourcePsi != null) {
                    (node.uastParent as? UClass)?.let { annotation ->
                        println(
                            "${context.driver.mode}: declared ${annotation.qualifiedName} in " +
                                "${context.project}"
                        )
                    }
                }
            }

            // If we find an usage of an experimentally-declared annotation, check it.
            val annotation = node.resolveToUElement()
            if (annotation is UAnnotated) {
                val annotations = context.evaluator.getAllAnnotations(annotation, false)
                if (annotations.any { APPLICABLE_ANNOTATIONS.contains(it.qualifiedName) }) {
                    if (DEBUG) {
                        println(
                            "${context.driver.mode}: used ${node.qualifiedName} in " +
                                "${context.project}"
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

        private fun loadAtomicLibraryGroupData(
            configuration: Configuration,
            issue: Issue
        ): List<String> {
            val filename = configuration.getOption(issue, ATOMIC_LIBGROUP_FILE_PROPERTY, null)
                ?: throw RuntimeException(
                    "Property $ATOMIC_LIBGROUP_FILE_PROPERTY is not set in lint.xml.")

            val libGroupFile = if (filename.contains(OUT_DIR_PLACEHOLDER)) {
                val fileLocation = filename.replace(OUT_DIR_PLACEHOLDER, System.getenv("OUT_DIR"))
                val file = File(fileLocation)
                if (!file.exists()) {
                    throw FileNotFoundException("Couldn't find atomic library group file $filename")
                }
                file
            } else {
                configuration.getOptionAsFile(issue, ATOMIC_LIBGROUP_FILE_PROPERTY, null)
                    ?: throw FileNotFoundException(
                        "Couldn't find atomic library group file $filename")
            }

            val atomicLibraryGroups = libGroupFile.readLines()
            if (atomicLibraryGroups.isEmpty()) {
                throw RuntimeException("Atomic library group file should not be empty")
            }

            return atomicLibraryGroups
        }
    }

    @Suppress("UNUSED_PARAMETER") // TODO: write logic + tests in future CL that uses groupList
    fun verifyUsageOfElementIsWithinSameGroup(
        context: JavaContext,
        usage: UElement,
        annotation: UElement,
        issue: Issue,
        atomicGroupList: List<String>,
    ) {
        val evaluator = context.evaluator
        val usageCoordinates = evaluator.getLibrary(usage) ?: context.project.mavenCoordinate
        val usageGroupId = usageCoordinates?.groupId
        val annotationGroupId = evaluator.getLibrary(annotation)?.groupId
        if (annotationGroupId != usageGroupId && annotationGroupId != null) {
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
    }

    companion object {
        private const val DEBUG = false

        /**
         * This string must match the value defined in buildSrc/lint.xml
         *
         * This is needed as we need to define the location of the atomic library group file for
         * non-test usage.  For tests, we can directly use the value defined in test lint.xml files.
         */
        private const val OUT_DIR_PLACEHOLDER = "USE_SYSTEM_OUT_DIR"

        // This must match the setting in buildSrc/lint.xml
        private const val ATOMIC_LIBGROUP_FILE_PROPERTY = "atomicLibraryGroupFilename"

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