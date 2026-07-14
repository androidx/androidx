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

package androidx.glance.wear.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UNamedExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.toUElementOfType

/**
 * A [Detector] that enforces that all Wear Widgets specify a valid background within
 * [WearWidgetDocument] to maintain brand compliance and prevent rendering defects.
 */
class WearWidgetBackgroundDetector : Detector(), Detector.UastScanner {

    override fun getApplicableConstructorTypes(): List<String> = listOf(WEAR_WIDGET_DOCUMENT)

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod,
    ) {
        // Add null check here, although the background parameter is non-nullable in Kotlin, to
        // avoid crashing the Lint engine during live editing in the IDE when the user is actively
        // typing and hasn't yet provided the argument. The Kotlin compiler will naturally flag the
        // missing argument as an error.
        val backgroundArg = findBackgroundArgument(context, node) ?: return

        if (isEmptyBrushReference(backgroundArg)) {
            context.report(
                EMPTY_BACKGROUND_ISSUE,
                backgroundArg,
                context.getLocation(backgroundArg),
                "WearWidgetDocument background cannot be an empty WearWidgetBrush reference",
            )
            return
        }

        // TODO: b/529269573 Implement black and transparent background detection
    }

    private tailrec fun isEmptyBrushReference(expr: UExpression, depth: Int = 0): Boolean {
        // Prevent infinite recursion on cyclic variable references
        if (depth > MAX_INITIALIZER_TRACE_DEPTH) return false

        val actualExpr = (expr as? UNamedExpression)?.expression ?: expr
        val reference = (actualExpr as? UQualifiedReferenceExpression)?.selector ?: actualExpr

        if (reference !is UReferenceExpression) {
            return false
        }

        when (val resolved = reference.resolve()) {
            is PsiClass -> {
                // It resolves directly to a class/companion object.
                // Verify it matches the exact qualified name.
                val className = resolved.qualifiedName
                return className == WEAR_WIDGET_BRUSH || className == WEAR_WIDGET_BRUSH_COMPANION
            }
            is PsiVariable -> {
                // It resolves to a variable. Trace back to its initializer to see what was
                // assigned.
                val initializer = resolved.toUElementOfType<UVariable>()?.uastInitializer
                if (initializer != null) {
                    return isEmptyBrushReference(initializer, depth + 1)
                }
            }
        }

        return false
    }

    companion object {
        private const val WEAR_WIDGET_DOCUMENT = "androidx.glance.wear.WearWidgetDocument"
        private const val WEAR_WIDGET_BRUSH = "androidx.glance.wear.WearWidgetBrush"
        private const val WEAR_WIDGET_BRUSH_COMPANION =
            "androidx.glance.wear.WearWidgetBrush.Companion"

        /**
         * The maximum depth to trace variable initializers when checking for empty brushes. This
         * acts as a safeguard to prevent infinite recursion in the Lint engine if a developer
         * accidentally creates a cyclic variable reference. A depth of 5 is arbitrary but
         * sufficient to support realistic chained local variable assignments.
         */
        private const val MAX_INITIALIZER_TRACE_DEPTH = 5

        @JvmField
        val EMPTY_BACKGROUND_ISSUE: Issue =
            Issue.create(
                id = "WearWidgetEmptyBackground",
                briefDescription = "Wear Widgets should specify a valid background",
                explanation =
                    """
                    Wear Widgets should explicitly specify a valid background within WearWidgetDocument(...).
                    If an empty WearWidgetBrush is provided, a default background color will be applied.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.WARNING,
                implementation =
                    Implementation(WearWidgetBackgroundDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )

        // TODO: b/529269573 Define an INVALID_BACKGROUND with Error severity for black or
        // transparent background detected

        private fun findBackgroundArgument(
            context: JavaContext,
            node: UCallExpression,
        ): UExpression? {
            val method = node.resolve() ?: return null
            val mapping = context.evaluator.computeArgumentMapping(node, method)
            return mapping.entries.firstOrNull { it.value.name == "background" }?.key
        }
    }
}
