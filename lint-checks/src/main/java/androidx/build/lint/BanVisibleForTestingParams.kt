/*
 * Copyright (C) 2018 The Android Open Source Project
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

import com.android.tools.lint.checks.getFqName
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UReferenceExpression

class BanVisibleForTestingParams : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UAnnotation::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return AnnotationChecker(context)
    }

    private inner class AnnotationChecker(val context: JavaContext) : UElementHandler() {
        override fun visitAnnotation(node: UAnnotation) {
            if (node.qualifiedName != "androidx.annotation.VisibleForTesting") return

            // Using "declared" to resolve an unspecified value to null, rather than the default,
            // resolve the FQN for the `otherwise` attribute value and abort if it's unspecified.
            val otherwise = (node.findDeclaredAttributeValue("otherwise") as? UReferenceExpression)
                ?.resolve()
                ?.getFqName()
                ?: return // Unspecified, abort.

            val fixBuilder = when (otherwise) {
                "androidx.annotation.VisibleForTesting.Companion.PRIVATE",
                "androidx.annotation.VisibleForTesting.Companion.NONE" -> {
                    // Extract Kotlin use-site target, if available.
                    val useSiteTarget = (node.sourcePsi as? KtAnnotationEntry)
                        ?.useSiteTarget
                        ?.getAnnotationUseSiteTarget()
                        ?.renderName
                        ?.let { "$it:" } ?: ""

                    fix().name("Remove non-default `otherwise` value")
                        .replace()
                        .with("@${useSiteTarget}androidx.annotation.VisibleForTesting")
                }
                "androidx.annotation.VisibleForTesting.Companion.PACKAGE_PRIVATE",
                "androidx.annotation.VisibleForTesting.Companion.PROTECTED" -> {
                    fix().name("Remove @VisibleForTesting annotation")
                        .replace()
                        .with("")
                }
                else -> {
                    // This could happen if a new visibility is added in the future, in which case
                    // we'll warn about the non-default usage but we won't attempt a fix.
                    null
                }
            }

            val incident = Incident(context)
                .issue(ISSUE)
                .location(context.getNameLocation(node))
                .message("Found non-default `otherwise` value for @VisibleForTesting")
                .scope(node)

            fixBuilder?.let {
                incident.fix(it.shortenNames().build())
            }

            context.report(incident)
        }
    }

    companion object {
        val ISSUE = Issue.create(
            "UsesNonDefaultVisibleForTesting",
            "Uses non-default @VisibleForTesting visibility",
            "Use of non-default @VisibleForTesting visibility is not allowed, use the " +
                "default value instead.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(BanVisibleForTestingParams::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
