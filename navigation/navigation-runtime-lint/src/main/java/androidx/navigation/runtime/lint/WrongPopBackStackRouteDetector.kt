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
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParameterForArgument

class WrongPopBackStackRouteDetector : Detector(), SourceCodeScanner {

    companion object {
        val WrongPopBackStackRouteType =
            Issue.create(
                id = "WrongPopBackStackRouteType",
                briefDescription = "Data class routes should use PopBackStack with reified class.",
                explanation =
                    "If the route is a data class with arguments, attempting to call " +
                        "popBackStack with only the class will causes a serialization error and " +
                        "you may not have the proper argument to call it with an instance. You " +
                        "should instead used the provided popBackStack function that takes the " +
                        "reified class name.",
                category = Category.CORRECTNESS,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        WrongPopBackStackRouteDetector::class.java,
                        Scope.JAVA_FILE_SCOPE
                    )
            )
    }

    final override fun getApplicableMethodNames(): List<String> = listOf("popBackStack")

    final override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod
    ) {
        val startNode =
            node.valueArguments.find { node.getParameterForArgument(it)?.name == "route" } ?: return

        val (isClassType, _) =
            startNode.isClassReference(
                checkClass = false,
                checkInterface = false,
                checkCompanion = true
            )
        if (isClassType) {
            context.report(
                WrongPopBackStackRouteType,
                startNode,
                context.getNameLocation(startNode as UElement),
                """
                Use popBackStack with reified class instead.
                    """
                    .trimIndent(),
                LintFix.create()
                    .replace()
                    .range(context.getNameLocation(node.uastParent as UElement))
                    .name("Use popBackStack with reified class instead.")
                    .text(node.sourcePsi?.text)
                    .with(
                        node.sourcePsi
                            ?.text
                            ?.replace("(", "<")
                            ?.replace("route =", "")
                            ?.replace(",", ">(")
                            ?.filterNot { it.isWhitespace() } // remove all white space
                            ?.replace(",", ", ") // correct comma formatting
                    )
                    .autoFix()
                    .build()
            )
        }
    }
}
