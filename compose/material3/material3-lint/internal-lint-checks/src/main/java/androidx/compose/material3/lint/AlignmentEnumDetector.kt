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
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement

/** [Detector] that flags custom alignment enums. */
class AlignmentEnumDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        if (context.isTestSource) return UElementHandler.NONE
        val packageName = context.uastFile?.packageName ?: return UElementHandler.NONE
        if (
            !packageName.startsWith("androidx.compose.material3") || packageName.contains("samples")
        )
            return UElementHandler.NONE

        return object : UElementHandler() {
            override fun visitClass(node: UClass) {
                val ktClass = node.sourcePsi as? KtClassOrObject ?: return
                val isPublic =
                    !ktClass.hasModifier(KtTokens.PRIVATE_KEYWORD) &&
                        !ktClass.hasModifier(KtTokens.INTERNAL_KEYWORD) &&
                        !ktClass.hasModifier(KtTokens.PROTECTED_KEYWORD)
                if (!isPublic) return

                val name = node.name ?: return
                if (name.endsWith("Alignment")) {
                    context.report(
                        ISSUE,
                        node as UElement,
                        context.getNameLocation(node),
                        "Avoid custom alignment enums; use standard Layout/Alignment APIs instead",
                    )
                }
            }
        }
    }

    companion object {
        val ISSUE =
            Issue.create(
                id = "AvoidCustomAlignmentEnum",
                briefDescription = "Avoid custom alignment enums",
                explanation =
                    """
                    Custom alignment enums should be avoided. Instead, use standard Compose UI Alignment
                    and Layout APIs (like `androidx.compose.ui.Alignment`) to maintain consistency and
                    interoperability across components.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(AlignmentEnumDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )
    }
}
