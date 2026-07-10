/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.animation.lint

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
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.skipParenthesizedExprDown

/**
 * A Lint check that flags usages of `LookaheadAnimationVisualDebugging` and
 * `CustomizedLookaheadAnimationVisualDebugging`.
 *
 * `LookaheadAnimationVisualDebugging` and `CustomizedLookaheadAnimationVisualDebugging` are
 * debugging tools and should not be present in production code as they can have performance
 * implications and are not intended for release builds.
 */
class LookaheadAnimationVisualDebuggingDetector : Detector(), SourceCodeScanner {
    companion object {
        val DisallowLookaheadAnimationVisualDebug =
            Issue.create(
                id = "DisallowLookaheadAnimationVisualDebug",
                briefDescription =
                    "LookaheadAnimationVisualDebugging and CustomizedLookaheadAnimationVisualDebugging are disallowed in production code.",
                explanation =
                    """
                 Remove LookaheadAnimationVisualDebugging and CustomizedLookaheadAnimationVisualDebugging. They are debugging tools
                 for shared element and animated bounds animations that can introduce performance overhead.
                 They are not intended for use in release builds.
            """,
                category = Category.CORRECTNESS,
                priority = 10,
                severity = Severity.FATAL,
                implementation =
                    Implementation(
                        LookaheadAnimationVisualDebuggingDetector::class.java,
                        Scope.JAVA_FILE_SCOPE,
                    ),
            )
    }

    override fun getApplicableMethodNames(): List<String> =
        listOf("LookaheadAnimationVisualDebugging", "CustomizedLookaheadAnimationVisualDebugging")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (
            method.containingClass?.qualifiedName !=
                "androidx.compose.animation.LookaheadAnimationVisualDebugHelperKt"
        ) {
            return
        }

        val message =
            "LookaheadAnimationVisualDebugging and CustomizedLookaheadAnimationVisualDebugging are disallowed in production code."

        // Attempt to create a quick fix that removes the wrapper but keeps the content.
        val contentLambda =
            node.valueArguments.lastOrNull()?.skipParenthesizedExprDown() as? ULambdaExpression
        val lambdaBody = contentLambda?.body?.sourcePsi?.text

        val fix =
            if (lambdaBody != null) {
                // The lambda body from sourcePsi includes the outer braces, so we trim them.
                val content = lambdaBody.trim().removeSurrounding("{", "}")
                fix()
                    .replace()
                    .range(context.getLocation(node))
                    .with(content.trim())
                    .name(
                        "Remove LookaheadAnimationVisualDebugging and CustomizedLookaheadAnimationVisualDebugging wrapper"
                    )
                    .build()
            } else {
                null
            }

        context.report(
            DisallowLookaheadAnimationVisualDebug,
            node,
            context.getLocation(node),
            message,
            fix,
        )
    }
}
