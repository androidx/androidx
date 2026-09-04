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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField

/** [Detector] that flags top-level public CompositionLocals. */
class TopLevelCompositionLocalDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UField::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        if (context.isTestSource) return UElementHandler.NONE
        val packageName = context.uastFile?.packageName ?: return UElementHandler.NONE
        if (
            !packageName.startsWith("androidx.compose.material3") || packageName.contains("samples")
        )
            return UElementHandler.NONE

        return object : UElementHandler() {
            override fun visitField(node: UField) {
                val ktProperty = node.sourcePsi as? KtProperty ?: return
                if (!ktProperty.isTopLevel) return

                val name = node.name
                if (Allowlist.contains(name)) return

                // Check if it is a CompositionLocal
                val typeFqn = node.type.canonicalText
                if (!typeFqn.contains("CompositionLocal")) return

                // Check if public (no private/internal modifiers)
                val hasPrivate = ktProperty.hasModifier(KtTokens.PRIVATE_KEYWORD)
                val hasInternal = ktProperty.hasModifier(KtTokens.INTERNAL_KEYWORD)
                if (hasPrivate || hasInternal) return

                val valKeyword = ktProperty.valOrVarKeyword
                val fix =
                    fix()
                        .name("Make internal")
                        .replace()
                        .range(context.getLocation(valKeyword))
                        .beginning()
                        .with("internal ")
                        .autoFix()
                        .build()

                context.report(
                    ISSUE,
                    node as UElement,
                    context.getNameLocation(node),
                    "CompositionLocals should not be defined as top-level public properties. Scope them inside an object or make them internal/private.",
                    fix,
                )
            }
        }
    }

    companion object {
        private val Allowlist =
            setOf(
                "LocalTonalElevationEnabled",
                "LocalContentColor",
                "LocalMinimumInteractiveComponentEnforcement",
                "LocalMinimumInteractiveComponentSize",
                "LocalRippleThemeConfiguration",
                "LocalRippleConfiguration",
                "LocalAbsoluteTonalElevation",
                "LocalTextStyle",
            )

        val ISSUE =
            Issue.create(
                id = "TopLevelCompositionLocal",
                briefDescription = "Top-level public CompositionLocal",
                explanation =
                    """
                    CompositionLocals should not be defined as top-level public properties.
                    To prevent polluting the global namespace and to group related configurations,
                    scope public CompositionLocals inside an object (e.g. `SelectionConfiguration.LocalColors`?)
                    or make them internal/private if they are only used within the module.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        TopLevelCompositionLocalDetector::class.java,
                        Scope.JAVA_FILE_SCOPE,
                    ),
            )
    }
}
