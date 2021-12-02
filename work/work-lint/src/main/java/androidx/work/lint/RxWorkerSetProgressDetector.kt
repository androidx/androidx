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

package androidx.work.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import java.util.EnumSet

class RxWorkerSetProgressDetector : Detector(), SourceCodeScanner {
    companion object {
        private const val SET_COMPLETABLE_PROGRESS = "setCompletableProgress"

        private const val DESCRIPTION =
            "`setProgress` is deprecated. Use `$SET_COMPLETABLE_PROGRESS` instead."

        val ISSUE = Issue.create(
            id = "UseRxSetProgress2",
            briefDescription = DESCRIPTION,
            explanation = """
                Use `$SET_COMPLETABLE_PROGRESS(...)` instead of `setProgress(...) in `RxWorker`.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.FATAL,
            implementation = Implementation(
                RxWorkerSetProgressDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE)
            )
        )
    }

    override fun getApplicableMethodNames(): List<String> = listOf("setProgress")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.isMemberInClass(method, "androidx.work.RxWorker")) {
            val lintFix = LintFix.create()
                .name("Use $SET_COMPLETABLE_PROGRESS instead")
                .replace()
                .text(method.name)
                .with(SET_COMPLETABLE_PROGRESS)
                .independent(true)
                .build()

            context.report(
                issue = ISSUE,
                location = context.getLocation(node),
                message = DESCRIPTION,
                quickfixData = lintFix
            )
        }
    }
}
