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
import org.jetbrains.uast.UDeclaration

@Suppress("unused")
class BanHideAndSuppressTags : Detector(), Detector.UastScanner {
    private val tagToIssue = mapOf(
        "@hide" to HIDE_ISSUE,
        "@suppress" to SUPPRESS_ISSUE,
    )

    override fun getApplicableUastTypes() = listOf(UDeclaration::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        override fun visitDeclaration(node: UDeclaration) {
            tagToIssue.forEach { (tag, issue) ->
                if (node.comments.any { it.text.contains(tag) }) {
                    val incident = Incident(context)
                        .issue(issue)
                        .location(context.getNameLocation(node))
                        .message("$tag is not allowed in documentation")
                        .scope(node)
                    context.report(incident)
                }
            }
        }
    }

    companion object {
        val HIDE_ISSUE = Issue.create(
            id = "BanHideTag",
            briefDescription = "@hide is not allowed in Javadoc",
            explanation = "Use of the @hide annotation in Javadoc is no longer allowed." +
              " Please use @RestrictTo instead.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                BanHideAndSuppressTags::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
        val SUPPRESS_ISSUE = Issue.create(
            id = "BanSuppressTag",
            briefDescription = "@suppress is not allowed in KDoc",
            explanation = "Use of the @suppress annotation in KDoc is no longer allowed." +
                " Please use @RestrictTo instead.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                BanHideAndSuppressTags::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
