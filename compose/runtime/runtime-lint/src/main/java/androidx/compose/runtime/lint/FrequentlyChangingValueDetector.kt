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

import androidx.compose.lint.Names
import androidx.compose.lint.isInvokedWithinComposable
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
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression

/**
 * Detector to ensure that @FrequentlyChangingValue annotated functions and property getters are not
 * called directly within composition.
 */
class FrequentlyChangingValueDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UCallExpression::class.java, USimpleNameReferenceExpression::class.java)
    }

    // Can't use visitAnnotationUsage because of b/381898394
    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            /** Visit function calls */
            override fun visitCallExpression(node: UCallExpression) {
                val frequentlyChangingValue =
                    methodOrSuperMethodsHaveAnnotation(
                        context,
                        node,
                        Names.Runtime.Annotation.FrequentlyChangingValue,
                    )
                if (frequentlyChangingValue && node.isInvokedWithinComposable()) {
                    report(node, context)
                }
            }

            /** Visit variable name references to see if we have a getter */
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                val frequentlyChangingValue =
                    getterOrSuperDeclarationsHaveAnnotation(
                        node,
                        Names.Runtime.Annotation.FrequentlyChangingValue,
                    )
                if (frequentlyChangingValue && node.isInvokedWithinComposable()) {
                    report(node, context)
                }
            }
        }
    }

    private fun report(node: UElement, context: JavaContext) {
        // Ignore existing suppressions from the previous check
        if (context.driver.isSuppressed(context, FrequentlyChangedStateReadInComposition, node)) {
            return
        }
        context.report(
            FrequentlyChangingValue,
            node,
            context.getNameLocation(node),
            "Reading a value annotated with @FrequentlyChangingValue inside composition",
        )
    }

    companion object {
        private const val Explanation =
            """
Reading a value annotated with @FrequentlyChangingValue inside composition can cause performance issues due to frequent recompositions. To avoid frequent recompositions, instead consider:

- Using derivedStateOf to filter state changes based on a provided calculation. For example, rather than recomposing on every scroll position change, only recomposing if the scroll position changes from 0 (at the top of the list) to greater than 0 (not at the top of the list), and vice versa.
- Using snapshotFlow to create a flow of changes from a provided state. This can then be collected inside a LaunchedEffect, and used to make changes without needing to recompose.
- If using Compose UI, read this value inside measure / layout / draw, depending on where it is needed. This will cause invalidation of the corresponding phase, instead of a recomposition. See developer.android.com for more information on Jetpack Compose phases.
"""

        val FrequentlyChangingValue =
            Issue.create(
                "FrequentlyChangingValue",
                "Reading a value annotated with @FrequentlyChangingValue inside composition",
                Explanation,
                Category.PERFORMANCE,
                5,
                Severity.WARNING,
                Implementation(
                    FrequentlyChangingValueDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                ),
            )

        // Removed issue so we can still check suppressions against the old issue
        private val FrequentlyChangedStateReadInComposition =
            Issue.create(
                id = "FrequentlyChangedStateReadInComposition",
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
