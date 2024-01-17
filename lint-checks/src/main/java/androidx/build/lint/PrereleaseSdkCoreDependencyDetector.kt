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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.model.LintModelAndroidLibrary
import com.android.tools.lint.model.LintModelLibrary
import org.jetbrains.uast.UCallExpression

class PrereleaseSdkCoreDependencyDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return CallChecker(context)
    }

    private inner class CallChecker(val context: JavaContext) : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            // Check that this is a prerelease SDK check
            val method = node.resolve() ?: return
            val containingClass = method.containingClass ?: return
            if (containingClass.qualifiedName != BUILD_COMPAT) return

            if (method.annotations.none { it.hasQualifiedName(PRERELEASE_SDK_CHECK) }) return

            // Check if the project is using a versioned dependency on core
            val dependencies = context.project.buildVariant.artifact.dependencies.getAll()
            if (dependencies.any { it.isInvalidCoreDependency() }) {
                val incident = Incident(context)
                    .issue(ISSUE)
                    .location(context.getLocation(node))
                    .message(
                        "Prelease SDK check ${method.name} cannot be called as this project has " +
                            "a versioned dependency on androidx.core:core"
                    )
                    .scope(node)
                context.report(incident)
            }
        }

        /**
         * Checks whether this library is a dependency on a specific version of androidx.core:core
         */
        private fun LintModelLibrary.isInvalidCoreDependency(): Boolean {
            val library = this as? LintModelAndroidLibrary ?: return false
            val coordinates = library.resolvedCoordinates
            return coordinates.artifactId == "core" &&
                coordinates.groupId == "androidx.core" &&
                coordinates.version != "unspecified"
        }
    }

    companion object {
        val ISSUE = Issue.create(
            "PrereleaseSdkCoreDependency",
            "Prerelease SDK checks can only be used by projects with a TOT dependency on " +
                "androidx.core:core",
            """
                The implementation of a prerelease SDK check will change when the SDK is finalized,
                so projects using these checks must have a tip-of-tree dependency on core to ensure
                the check stays up-to-date.

                This error means that the `androidx.core:core` dependency in this project's
                `build.gradle` file should be replaced with `implementation(project(":core:core"))`

                See go/androidx-api-guidelines#compat-sdk for more information.
            """,
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                PrereleaseSdkCoreDependencyDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        private const val BUILD_COMPAT = "androidx.core.os.BuildCompat"
        private const val PRERELEASE_SDK_CHECK = "$BUILD_COMPAT.PrereleaseSdkCheck"
    }
}
