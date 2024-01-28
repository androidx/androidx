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

@file:Suppress("UnstableApiUsage")

package androidx.navigation.common.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression

/**
 * Lint for checking for empty construction of NavDeepLink in the Kotlin DSL,
 * i.e. navDeepLink { }
 */
class EmptyNavDeepLinkDetector : Detector(), SourceCodeScanner {
    companion object {
        val EmptyNavDeepLink = Issue.create(
            id = "EmptyNavDeepLink",
            briefDescription = "NavDeepLink must define an uri, action, and/or mimetype to be " +
                "valid.",
            explanation = "Attempting to create an empty NavDeepLink will result in an " +
                "IllegalStateException at runtime. You may set these arguments within the lambda " +
                "of the call to navDeepLink.",
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            implementation = Implementation(
                EmptyNavDeepLinkDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
    override fun getApplicableMethodNames(): List<String> = listOf("navDeepLink")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        // valueArgumentCount should be 1 when navDeepLink is called
        if (node.valueArgumentCount > 0) {
            val lam = node.valueArguments[0] as ULambdaExpression
            val body = lam.body as UBlockExpression
            if (body.expressions.isEmpty()) {
                context.report(
                    EmptyNavDeepLink,
                    node,
                    context.getNameLocation(node),
                    "Creation of empty NavDeepLink"
                )
            }
        }
    }
}
