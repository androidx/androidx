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
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass

/**
 * [Detector] that checks that composable properties in `*Defaults` objects are annotated with
 * `@ReadOnlyComposable`.
 */
class ThemeGetterAnnotationDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        if (context.isTestSource) return UElementHandler.NONE
        val packageName = context.uastFile?.packageName ?: return UElementHandler.NONE
        if (
            !packageName.startsWith("androidx.compose.material3") || packageName.contains("samples")
        )
            return UElementHandler.NONE

        return object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                val containingClass = node.getContainingUClass() ?: return
                val className = containingClass.name ?: return
                if (!className.endsWith("Defaults")) return

                // Target only property getters
                val sourcePsi = node.sourcePsi
                if (sourcePsi !is KtProperty && sourcePsi !is KtPropertyAccessor) return

                // Check if it is Composable
                if (!node.hasAnnotation("androidx.compose.runtime.Composable")) return

                // Check if it is already ReadOnlyComposable
                if (node.hasAnnotation("androidx.compose.runtime.ReadOnlyComposable")) return

                val fix =
                    fix()
                        .name("Add @ReadOnlyComposable")
                        .annotate(
                            source = "@androidx.compose.runtime.ReadOnlyComposable",
                            context = context,
                            element = node.sourcePsi,
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
                id = "ThemeGetterMissingReadOnlyComposable",
                briefDescription = "Composable property in Defaults is missing @ReadOnlyComposable",
                explanation =
                    """
                    Composable properties in Defaults objects (which act as theme getters) should be
                    annotated with `@ReadOnlyComposable` to avoid unnecessary recomposition overhead,
                    as they only read theme values/CompositionLocals and do not write to state or use `remember`.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        ThemeGetterAnnotationDetector::class.java,
                        Scope.JAVA_FILE_SCOPE,
                    ),
            )
    }
}
