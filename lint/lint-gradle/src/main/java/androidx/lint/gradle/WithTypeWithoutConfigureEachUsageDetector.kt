/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lint.gradle

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

class WithTypeWithoutConfigureEachUsageDetector : Detector(), Detector.UastScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("withType")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val evaluator = context.evaluator
        val message = "Avoid passing a closure to withType, use withType().configureEach instead"
        val incident =
            Incident(context)
                .issue(ISSUE)
                .location(context.getNameLocation(node))
                .message(message)
                .scope(node)

        if (
            evaluator.isMemberInClass(node.resolve(), DOMAIN_OBJECT_COLLECTION) &&
                node.valueArgumentCount != 1
        ) {
            context.report(incident)
        }
    }

    companion object {
        private const val DOMAIN_OBJECT_COLLECTION = "org.gradle.api.DomainObjectCollection"

        val ISSUE =
            Issue.create(
                id = "WithTypeWithoutConfigureEach",
                briefDescription =
                    "Flags usage of withType with a closure instead of configureEach",
                explanation =
                    """
                Using withType with a closure directly eagerly creates task.
                Using configureEach defers the creation of tasks.
            """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        WithTypeWithoutConfigureEachUsageDetector::class.java,
                        Scope.JAVA_FILE_SCOPE
                    )
            )
    }
}
