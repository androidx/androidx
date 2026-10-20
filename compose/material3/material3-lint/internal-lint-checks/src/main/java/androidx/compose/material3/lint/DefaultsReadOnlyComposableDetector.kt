/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * [Detector] that checks that composable properties in `*Defaults` objects are annotated with
 * `@ReadOnlyComposable`.
 *
 * Note: This check applies specifically to property getters in `*Defaults` objects, not to
 * functions or methods (such as factory functions with configurable parameters).
 */
class DefaultsReadOnlyComposableDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        if (!context.isMaterial3Source()) return UElementHandler.NONE

        return object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                // Target only property getters. This detector checks properties, not
                // functions/methods.
                val sourcePsi = node.sourcePsi
                if (sourcePsi !is KtProperty && sourcePsi !is KtPropertyAccessor) return

                val containingClass = node.getContainingUClass() ?: return
                val className = containingClass.name ?: return
                if (!className.endsWith("Defaults")) return

                // Check if it is Composable
                if (!node.hasAnnotation("androidx.compose.runtime.Composable")) return

                // Check if it is already ReadOnlyComposable
                if (node.hasAnnotation("androidx.compose.runtime.ReadOnlyComposable")) return

                // If the getter body calls regular @Composable functions or accesses non-read-only
                // composable properties (which allocate groups or write into the slot table), it
                // cannot be annotated with @ReadOnlyComposable.
                if (node.callsNonReadOnlyComposable()) return

                val targetElement = (sourcePsi as? KtProperty)?.getter ?: sourcePsi
                val annotationSource =
                    if (targetElement is KtProperty) {
                        "@get:androidx.compose.runtime.ReadOnlyComposable"
                    } else {
                        "@androidx.compose.runtime.ReadOnlyComposable"
                    }

                val fix =
                    fix()
                        .name("Add @ReadOnlyComposable")
                        .annotate(
                            source = annotationSource,
                            context = context,
                            element = targetElement,
                            replace = false,
                        )
                        .autoFix()
                        .build()

                context.report(
                    ISSUE,
                    node as UElement,
                    context.getNameLocation(node),
                    "Composable properties in Defaults objects should be annotated with @ReadOnlyComposable",
                    fix,
                )
            }
        }
    }

    companion object {
        val ISSUE =
            Issue.create(
                id = "DefaultsMissingReadOnlyComposable",
                briefDescription = "Composable property in Defaults is missing @ReadOnlyComposable",
                explanation =
                    """
                    Composable properties in Defaults objects should be annotated with
                    `@ReadOnlyComposable` to avoid unnecessary recomposition overhead, as they
                    only read theme values/CompositionLocals and do not write to state or use `remember`.

                    Note: This detector only checks composable properties on Defaults objects,
                    not functions or methods (such as factory functions with configurable parameters).
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        DefaultsReadOnlyComposableDetector::class.java,
                        Scope.JAVA_FILE_SCOPE,
                    ),
            )
    }
}

private fun UMethod.callsNonReadOnlyComposable(): Boolean {
    var callsNonReadOnly = false
    uastBody?.accept(
        object : AbstractUastVisitor() {
            override fun visitElement(node: UElement): Boolean = callsNonReadOnly

            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (callsNonReadOnly) return true
                val resolved = node.resolve()
                if (
                    resolved != null &&
                        resolved.hasAnnotation("androidx.compose.runtime.Composable") &&
                        !resolved.hasAnnotation("androidx.compose.runtime.ReadOnlyComposable")
                ) {
                    callsNonReadOnly = true
                    return true
                }
                return super.visitCallExpression(node)
            }

            override fun visitSimpleNameReferenceExpression(
                node: USimpleNameReferenceExpression
            ): Boolean {
                if (callsNonReadOnly) return true
                val resolved = node.resolve()
                if (
                    resolved is PsiMethod &&
                        resolved.hasAnnotation("androidx.compose.runtime.Composable") &&
                        !resolved.hasAnnotation("androidx.compose.runtime.ReadOnlyComposable")
                ) {
                    callsNonReadOnly = true
                    return true
                }
                return super.visitSimpleNameReferenceExpression(node)
            }
        }
    )
    return callsNonReadOnly
}
