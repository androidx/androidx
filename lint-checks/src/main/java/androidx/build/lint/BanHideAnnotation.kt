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
class BanHideAnnotation : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UDeclaration::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        override fun visitDeclaration(node: UDeclaration) {
            if (node.comments.any { it.text.contains("@hide") }) {
                val incident = Incident(context)
                    .issue(ISSUE)
                    .location(context.getNameLocation(node))
                    .message("@hide is not allowed in Javadoc")
                    .scope(node)
                context.report(incident)
            }
        }
    }

    companion object {
        val ISSUE = Issue.create(
            id = "BanHideAnnotation",
            briefDescription = "@hide is not allowed in Javadoc",
            explanation = "Use of the @hide annotation in Javadoc is no longer allowed." +
              " Please use @RestrictTo instead.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(BanHideAnnotation::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
