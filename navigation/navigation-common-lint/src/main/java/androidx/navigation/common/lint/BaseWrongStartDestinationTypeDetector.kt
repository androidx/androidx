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
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
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

        /**
         * True if:
         * 1. reference to object (i.e. val myStart = TestStart(), startDest = myStart)
         * 2. object declaration (i.e. object MyStart, startDest = MyStart)
         * 3. class reference (i.e. class MyStart, startDest = MyStart)
         *
         *    We only want to catch case 3., so we need more filters to eliminate case 1 & 2.
         */
        val isSimpleRefExpression = startNode is USimpleNameReferenceExpression

        /** True if nested class i.e. OuterClass.InnerClass */
        val isQualifiedRefExpression = startNode is UQualifiedReferenceExpression

        if (!(isSimpleRefExpression || isQualifiedRefExpression)) return

        val sourcePsi = startNode.sourcePsi as? KtExpression ?: return
        val (isClassType, name) =
            analyze(sourcePsi) {
                val symbol =
                    when (sourcePsi) {
                        is KtDotQualifiedExpression -> {
                            val lastChild = sourcePsi.lastChild
                            if (lastChild is KtReferenceExpression) {
                                lastChild.mainReference.resolveToSymbol()
                            } else {
                                null
                            }
                        }
                        is KtReferenceExpression -> sourcePsi.mainReference.resolveToSymbol()
                        else -> null
                    }
                        as? KtClassOrObjectSymbol ?: return

                symbol.classKind.isClass to symbol.name
            }

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
