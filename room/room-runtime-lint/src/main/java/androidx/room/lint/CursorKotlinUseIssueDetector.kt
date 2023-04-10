/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.lint

import com.android.tools.lint.checks.VersionChecks.Companion.isPrecededByVersionCheckExit
import com.android.tools.lint.checks.VersionChecks.Companion.isWithinVersionCheckConditional
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.VersionChecks
import com.android.tools.lint.detector.api.minSdkLessThan
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

class CursorKotlinUseIssueDetector : Detector(), SourceCodeScanner {
    companion object {
        private const val DESCRIPTION = "Usage of `kotlin.io.use()` with Cursor requires API 16."
        val ISSUE = Issue.create(
            id = "CursorKotlinUse",
            briefDescription = DESCRIPTION,
            explanation = """
                The use of `kotlin.io.use()` with `android.database.Cursor` is not safe when min
                API level is less than 16 since Cursor does not implement Closeable.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.FATAL,
            implementation = Implementation(
                CursorKotlinUseIssueDetector::class.java, Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableMethodNames(): List<String> = listOf("use")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        // Skip if `use` is not Kotlin's
        if (!context.evaluator.isMemberInClass(method, "kotlin.io.CloseableKt")) {
            return
        }
        // Skip if the receiver is not Android's Cursor
        if (node.receiverType?.canonicalText != "android.database.Cursor") {
            return
        }
        // If the call is within an SDK_INT check, then its OK
        if (
            VersionChecks.isWithinVersionCheckConditional(context, node, 16) ||
            VersionChecks.isPrecededByVersionCheckExit(context, node, 16)
        ) {
            return
        }
        context.report(
            incident = Incident(ISSUE, DESCRIPTION, context.getLocation(node)),
            constraint = minSdkLessThan(16)
        )
    }
}