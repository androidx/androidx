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

@file:Suppress("UnstableApiUsage")

package androidx.work.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

class PeriodicEnqueueIssueDetector : Detector(), SourceCodeScanner {
    companion object {
        val ISSUE = Issue.create(
            id = "BadPeriodicWorkRequestEnqueue",
            briefDescription = "Use `enqueueUniquePeriodicWork()` instead of `enqueue()`",
            explanation = """
                When using `enqueue()` for `PeriodicWorkRequest`s, you might end up enqueuing
                duplicate requests unintentionally. You should be using
                `enqueueUniquePeriodicWork` with an `ExistingPeriodicWorkPolicy.KEEP` instead.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.WARNING,
            implementation = Implementation(
                PeriodicEnqueueIssueDetector::class.java, Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableMethodNames(): List<String>? = listOf("enqueue")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.isMemberInClass(method, "androidx.work.WorkManager")) {
            val periodic = node.valueArguments.filter { argument ->
                val type = argument.getExpressionType()?.canonicalText
                type == "androidx.work.PeriodicWorkRequest" ||
                    type == "java.util.List<? extends androidx.work.PeriodicWorkRequest>" ||
                    type == "java.util.List<? extends androidx.work.WorkRequest>"
            }
            if (periodic.isNotEmpty()) {
                context.report(
                    ISSUE,
                    context.getLocation(method),
                    message = "Use `enqueueUniquePeriodicWork()` instead of `enqueue()`"
                )
            }
        }
    }
}
