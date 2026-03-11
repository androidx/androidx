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

package androidx.compose.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiTypes
import com.intellij.psi.PsiWhiteSpace
import java.util.EnumSet
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField

/**
 * Detector that validates the setup of feature flags. Feature flag objects (objects ending in
 * "Flags") should have:
 * - Each entry as a Boolean var
 * - Annotated with @JvmField
 * - A TODO comment right above it linking to a bug
 */
class FeatureFlagDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitClass(node: UClass) {
                val name = node.name ?: return
                if (!name.endsWith("Flags")) return
                if (node.sourcePsi !is KtObjectDeclaration) return

                node.fields.forEach { field ->
                    val sourcePsi = field.sourcePsi
                    if (sourcePsi is KtProperty) {
                        validateField(context, field, sourcePsi)
                    }
                }
            }
        }

    private fun validateField(context: JavaContext, field: UField, sourcePsi: KtProperty) {
        // 1. Check if it's a Boolean
        val type = field.type
        val isBoolean =
            type == PsiTypes.booleanType() ||
                type.canonicalText == "java.lang.Boolean" ||
                type.canonicalText == "kotlin.Boolean"

        if (!isBoolean) {
            context.report(
                ISSUE,
                field,
                context.getNameLocation(field),
                "Feature flags must be of type Boolean",
            )
        }

        // 2. Check if it's a var (mutable)
        if (!sourcePsi.isVar) {
            context.report(
                ISSUE,
                field,
                context.getNameLocation(field),
                "Feature flags must be mutable (use 'var')",
            )
        }

        // 3. Check if annotated with @JvmField
        if (!field.annotations.any { it.qualifiedName == "kotlin.jvm.JvmField" }) {
            context.report(
                ISSUE,
                field,
                context.getNameLocation(field),
                "Feature flags must be annotated with @JvmField",
            )
        }

        // 4. Check for TODO comment
        if (!hasTodoComment(sourcePsi)) {
            context.report(
                ISSUE,
                field,
                context.getNameLocation(field),
                "Feature flags must have a TODO comment with a bug link immediately above them",
            )
        }
    }

    private fun hasTodoComment(property: KtProperty): Boolean {
        // Check preceding siblings
        var sibling = property.prevSibling
        while (sibling != null) {
            if (sibling is PsiComment) {
                if (isTodoWithBug(sibling.text)) return true
            }
            if (sibling !is PsiWhiteSpace && sibling !is PsiComment) {
                break
            }
            sibling = sibling.prevSibling
        }

        // Check children (sometimes the comment is part of the property range)
        var child = property.firstChild
        while (child != null) {
            if (child is PsiComment) {
                if (isTodoWithBug(child.text)) return true
            }
            if (
                child !is PsiWhiteSpace &&
                    child !is PsiComment &&
                    child !is org.jetbrains.kotlin.psi.KtAnnotationEntry &&
                    child !is org.jetbrains.kotlin.psi.KtModifierList
            ) {
                // If we hit something else, we might have passed the comments/annotations
            }
            if (child.text == "var" || child.text == "val") break
            child = child.nextSibling
        }

        return false
    }

    private fun isTodoWithBug(text: String): Boolean = text.contains(Regex("""TODO.*b/\d+"""))

    companion object {
        val ISSUE =
            Issue.create(
                id = "FeatureFlagSetup",
                briefDescription = "Feature flags should be properly configured",
                explanation =
                    """
                    Feature flags must be Boolean vars annotated with @JvmField.
                    Additionally, each feature flag must have a TODO comment linking to a bug for its cleanup.

                    See go/jetpack-compose/feature-flags for more information and the bug template.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(FeatureFlagDetector::class.java, EnumSet.of(Scope.JAVA_FILE)),
            )
    }
}
