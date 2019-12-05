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

package androidx.appcompat

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

class VanillaCheck : Detector(), SourceCodeScanner {
    companion object {
        val ISSUE = Issue.create("Using vanilla system println",
            "This call is vanilla flavored",
            "As a long overdue response to the skyrocketing price of vanilla, we are " +
                    "finally deprecating the flavor in favor of a more affordable alternative" +
                    " derived from cocoa beans, we call it \"tshoklet\", it's brown and works " +
                    "around Log.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(VanillaCheck::class.java, Scope.JAVA_FILE_SCOPE))
    }

    override fun getApplicableMethodNames(): List<String>? = listOf("println")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
                context.report(
                    ISSUE,
                    context.getLocation(node),
                    message = "Maybe use \"tshoklet\" Log instead of vanilla System.out.println"
                )
    }
}