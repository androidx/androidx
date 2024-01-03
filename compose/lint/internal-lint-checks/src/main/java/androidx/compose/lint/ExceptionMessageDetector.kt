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

package androidx.compose.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.getParameterForArgument

class ExceptionMessageDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> =
        listOf(Check, CheckNotNull, Require, RequireNotNull).map { it.shortName }
    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {

        // We ignore other functions with the same name.
        if (!method.isInPackageName(KotlinPackage)) return

        val lazyMessage = node.valueArguments
            .find { node.getParameterForArgument(it)?.name == "lazyMessage" }
        if (lazyMessage == null) {
            context.report(
                ISSUE,
                node,
                context.getNameLocation(node),
                "Please specify a lazyMessage param for ${node.methodName}",
            )
        }
    }

    companion object {
        val KotlinPackage = Package("kotlin")
        val Check = Name(KotlinPackage, "check")
        val CheckNotNull = Name(KotlinPackage, "checkNotNull")
        val Require = Name(KotlinPackage, "require")
        val RequireNotNull = Name(KotlinPackage, "requireNotNull")
        val ISSUE = Issue.create(
            id = "ExceptionMessage",
            briefDescription = "Please provide a string for the lazyMessage parameter",
            explanation = """
                Calls to check(), checkNotNull(), require() and requireNotNull() should
                include a message string that can be used to debug issues experienced
                by users.

                When we read user-supplied logs, the line numbers are sometimes not
                sufficient to determine the cause of a bug. Inline functions can
                sometimes make it hard to determine which file threw an exception.
                Consider supplying a lazyMessage parameter to identify the check()
                or require() call.
            """,
            category = Category.CORRECTNESS,
            priority = 3,
            severity = Severity.ERROR,
            implementation = Implementation(
                ExceptionMessageDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE)
            )
        )
    }
}
