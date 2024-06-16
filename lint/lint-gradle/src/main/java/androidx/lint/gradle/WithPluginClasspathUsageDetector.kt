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
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

class WithPluginClasspathUsageDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> = listOf("withPluginClasspath")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val evaluator = context.evaluator

        val message =
            "Avoid usage of GradleRunner#withPluginClasspath, which is broken. " +
                "Instead use something like https://github.com/autonomousapps/" +
                "dependency-analysis-gradle-plugin/tree/main/testkit#gradle-testkit-support-plugin"

        val incident =
            Incident(context)
                .issue(ISSUE)
                .location(context.getNameLocation(node))
                .message(message)
                .scope(node)

        if (evaluator.isMemberInClass(node.resolve(), "org.gradle.testkit.runner.GradleRunner")) {
            context.report(incident)
        }
    }

    companion object {
        val ISSUE: Issue =
            Issue.create(
                id = "WithPluginClasspathUsage",
                briefDescription = "Flags usage of GradleRunner#withPluginClasspath",
                explanation =
                    """
                This check flags usage of `GradleRunner#withPluginClasspath` in tests,
                as it might lead to potential issues or it is discouraged in certain contexts.
            """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        WithPluginClasspathUsageDetector::class.java,
                        Scope.JAVA_FILE_SCOPE
                    )
            )
    }
}
