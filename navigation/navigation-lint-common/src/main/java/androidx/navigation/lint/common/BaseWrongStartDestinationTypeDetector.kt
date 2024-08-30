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

package androidx.navigation.lint.common

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
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParameterForArgument

abstract class BaseWrongStartDestinationTypeDetector(
    private val methodNames: List<String>,
    private val parameterNames: List<String>
) : Detector(), SourceCodeScanner {

    final override fun getApplicableMethodNames(): List<String> = methodNames

    final override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod
    ) {
        val startNode =
            node.valueArguments.find {
                parameterNames.contains(node.getParameterForArgument(it)?.name)
            } ?: return

        val (isClassType, name) = startNode.isClassReference()
        if (isClassType) {
            context.report(
                getIssue(),
                startNode,
                context.getNameLocation(startNode as UElement),
                """
                        StartDestination should not be a simple class name reference.
                        Did you mean to call its constructor $name(...)?
                        If the class $name does not contain arguments,
                        you can also pass in its KClass reference $name::class
                    """
                    .trimIndent()
            )
        }
    }

    abstract fun getIssue(): Issue
}

fun createWrongStartDestinationTypeIssue(
    detectorClass: Class<out BaseWrongStartDestinationTypeDetector>
) =
    Issue.create(
        id = "WrongStartDestinationType",
        briefDescription =
            "If the startDestination points to a Class with arguments, the " +
                "startDestination must be an instance of that class. If it points to a " +
                "Class without arguments, startDestination can be a KClass literal, such as" +
                " StartClass::class.",
        explanation =
            "If the startDestination contains arguments, the arguments must be " +
                "provided to navigation via a fully formed route (a class instance with arguments" +
                "filled in), or else it will be treated as a case of missing arguments.",
        category = Category.CORRECTNESS,
        severity = Severity.ERROR,
        implementation = Implementation(detectorClass, Scope.JAVA_FILE_SCOPE)
    )
