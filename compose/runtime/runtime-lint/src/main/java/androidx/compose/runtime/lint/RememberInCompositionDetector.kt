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

@file:Suppress("UnstableApiUsage")

package androidx.compose.runtime.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Names
import androidx.compose.lint.isNotRemembered
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.EnumSet
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccess
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.singleVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.util.isConstructorCall

/**
 * Detector to ensure that @RememberInComposition annotated constructors, functions, and property
 * getters are not called directly within composition.
 */
class RememberInCompositionDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UCallExpression::class.java, USimpleNameReferenceExpression::class.java)
    }

    // Can't use visitAnnotationUsage because of b/381898394
    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            /** Visit function calls / constructor invocations */
            override fun visitCallExpression(node: UCallExpression) {
                val rememberInComposition =
                    methodOrSuperMethodsHaveAnnotation(
                        context,
                        node,
                        Names.Runtime.Annotation.RememberInComposition,
                    )
                if (rememberInComposition && node.isNotRemembered()) {
                    report(node, context)
                }
            }

            /** Visit variable name references to see if we have a getter */
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                val rememberInComposition =
                    getterOrSuperDeclarationsHaveAnnotation(
                        node,
                        Names.Runtime.Annotation.RememberInComposition,
                    )
                if (rememberInComposition && node.isNotRemembered()) {
                    report(node, context)
                }
            }
        }
    }

    private fun report(node: UElement, context: JavaContext) {
        // Handle existing suppressions for the old lint checks that are now merged into this one -
        // we don't want to re-warn if they are already suppressed from before
        if (node is UCallExpression) {
            val method = node.resolve()
            if (context.driver.isSuppressed(context, UnrememberedAnimatable, node)) {
                if (node.isConstructorCall()) {
                    if (method?.containingClass?.name == Names.Animation.Core.Animatable.shortName)
                        return
                } else {
                    if (node.methodName == Names.Animation.Core.Animatable.shortName) return
                }
            }

            if (context.driver.isSuppressed(context, UnrememberedMutableInteractionSource, node)) {
                if (node.methodName == "MutableInteractionSource") return
            }
        }
        context.report(
            RememberInComposition,
            node,
            context.getNameLocation(node),
            "Calling a @RememberInComposition annotated declaration inside composition without using `remember`",
        )
    }

    companion object {
        val RememberInComposition =
            Issue.create(
                "RememberInComposition",
                "Calling a @RememberInComposition annotated declaration inside composition without using `remember`",
                "APIs annotated with @RememberInComposition must not be called inside composition. This can lead to correctness issues and / or performance issues. Instead, use `remember` to cache the value across compositions, or hoist this call outside of composition.",
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(
                    RememberInCompositionDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                ),
            )

        // Removed issues so we can still check suppressions against the old issue
        private val UnrememberedAnimatable =
            Issue.create(
                id = "UnrememberedAnimatable",
                briefDescription = "Removed issue, exists for suppression checking.",
                explanation = "Removed issue, exists for suppression checking.",
                category = Category.USABILITY,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(RememberInCompositionDetector::class.java, Scope.EMPTY),
            )

        private val UnrememberedMutableInteractionSource =
            Issue.create(
                id = "UnrememberedMutableInteractionSource",
                briefDescription = "Removed issue, exists for suppression checking.",
                explanation = "Removed issue, exists for suppression checking.",
                category = Category.USABILITY,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(RememberInCompositionDetector::class.java, Scope.EMPTY),
            )
    }
}

/**
 * @return true if [node] references a constructor or a method, and that constructor or method or
 *   any of its super declarations (if a method) are annotated with [annotationName]
 */
internal fun methodOrSuperMethodsHaveAnnotation(
    context: JavaContext,
    node: UCallExpression,
    annotationName: Name,
): Boolean {
    val resolved = node.resolve() ?: return false
    // If this is an overridden method, check if any of the super methods are annotated
    val methodsToCheck =
        if (context.evaluator.isOverride(resolved, true)) {
            resolved.findSuperMethods() + resolved
        } else {
            arrayOf(resolved)
        }
    return methodsToCheck.any { method ->
        method.annotations.any { it.hasQualifiedName(annotationName.javaFqn) }
    }
}

/**
 * @return true if [node] references a getter, and that getter or any of its super declarations are
 *   annotated with [annotationName]
 */
internal fun getterOrSuperDeclarationsHaveAnnotation(
    node: USimpleNameReferenceExpression,
    annotationName: Name,
): Boolean {
    val source = node.sourcePsi as? KtElement ?: return false
    // Need to use analysis APIs because of b/381898394
    analyze(source) {
        (source.resolveToCall()?.singleVariableAccessCall() as? KaSimpleVariableAccessCall)?.let {
            variableAccessCall ->
            val propertySymbol = variableAccessCall.symbol as? KaPropertySymbol ?: return false
            val getter =
                when (variableAccessCall.simpleAccess) {
                    is KaSimpleVariableAccess.Read -> propertySymbol.getter ?: return false
                    // We don't track property setters
                    is KaSimpleVariableAccess.Write -> return false
                }

            // Check if any super symbol is annotated as well
            // TODO: go back to just `getter.allOverriddenSymbols`
            //   if https://youtrack.jetbrains.com/issue/KT-77735 is fixed / available.
            val symbolsToCheck =
                propertySymbol.allOverriddenSymbols.mapNotNull {
                    (it as? KaPropertySymbol)?.getter
                } + getter
            symbolsToCheck.forEach { symbol ->
                if (
                    symbol.annotations.any {
                        it.classId?.asFqNameString() == annotationName.javaFqn
                    }
                ) {
                    return true
                }
            }
        }
    }
    return false
}
