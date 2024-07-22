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

package androidx.navigation.runtime.lint

import androidx.navigation.lint.common.isClassReference
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
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParameterForArgument

class WrongNavigateRouteDetector() : Detector(), SourceCodeScanner {

    companion object {
        val WrongNavigateRouteType =
            Issue.create(
                id = "WrongNavigateRouteType",
                briefDescription =
                    "Navigation route should be an object literal or a destination class instance " +
                        "with arguments.",
                explanation =
                    "If the destination class contains arguments, the route is " +
                        "expected to be class instance with the arguments filled in.",
                category = Category.CORRECTNESS,
                severity = Severity.ERROR,
                implementation =
                    Implementation(WrongNavigateRouteDetector::class.java, Scope.JAVA_FILE_SCOPE)
            )
    }

    final override fun getApplicableMethodNames(): List<String> = listOf("navigate")

    final override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod
    ) {
        val startNode =
            node.valueArguments.find { node.getParameterForArgument(it)?.name == "route" } ?: return

        val isClassLiteral = startNode is UClassLiteralExpression
        val (isClassType, _) = startNode.isClassReference()
        if (isClassType || isClassLiteral) {
            context.report(
                WrongNavigateRouteType,
                startNode,
                context.getNameLocation(startNode as UElement),
                """
                The route should be a destination class instance or destination object.
                    """
                    .trimIndent()
            )
        }
    }
}
