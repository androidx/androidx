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
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.util.isArrayInitializer

class BanRestrictToTestsScope : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UAnnotation::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return AnnotationChecker(context)
    }

    private inner class AnnotationChecker(val context: JavaContext) : UElementHandler() {
        override fun visitAnnotation(node: UAnnotation) {
            if (node.qualifiedName != "androidx.annotation.RestrictTo") return

            // Resolve the FQN for all arguments to value parameter.
            val scopes = node.findAttributeValue("value")?.let { value ->
                if (value.isArrayInitializer()) {
                    (value as? UCallExpression)?.valueArguments?.mapNotNull { arg ->
                        arg as? UReferenceExpression
                    } ?: emptyList()
                } else if (value is UReferenceExpression) {
                    listOfNotNull(value)
                } else {
                    emptyList()
                }
            }?.mapNotNull { expr ->
                expr.resolve()?.getFqName()
            } ?: emptyList()

            if (!scopes.contains("androidx.annotation.RestrictTo.Scope.TESTS")) return

            val incident = Incident(context)
                .issue(ISSUE)
                .location(context.getNameLocation(node))
                .message("Replace `@RestrictTo(TESTS)` with `@VisibleForTesting`")
                .scope(node)

            // If there's only one scope, suggest replacement.
            if (scopes.size == 1) {
                // Extract Kotlin use-site target, if available.
                val useSiteTarget = (node.sourcePsi as? KtAnnotationEntry)
                    ?.useSiteTarget
                    ?.getAnnotationUseSiteTarget()
                    ?.renderName
                    ?.let { "$it:" } ?: ""

                val fix = fix().name("Replace with `@${useSiteTarget}VisibleForTesting`")
                    .replace()
                    .with("@${useSiteTarget}androidx.annotation.VisibleForTesting")
                    .shortenNames()
                    .build()
                incident.fix(fix)
            }

            context.report(incident)
        }
    }

    companion object {
        val ISSUE = Issue.create(
            "UsesRestrictToTestsScope",
            "Uses @RestrictTo(TESTS) restriction scope",
            "Use of @RestrictTo(TESTS) restriction scope is not allowed, use " +
                "@VisibleForTesting instead.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(BanRestrictToTestsScope::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
