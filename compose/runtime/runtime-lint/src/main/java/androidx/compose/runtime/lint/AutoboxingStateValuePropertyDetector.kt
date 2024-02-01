/*
 * Copyright() 2023 The Android Open Source Project
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

package androidx.compose.runtime.lint

import com.android.tools.lint.detector.api.AnnotationInfo
import com.android.tools.lint.detector.api.AnnotationUsageInfo
import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils
import java.util.EnumSet
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression

class AutoboxingStateValuePropertyDetector : Detector(), SourceCodeScanner {

    private val UAnnotation.preferredPropertyName: String?
        get() = UastLintUtils.getAnnotationStringValue(this, "preferredPropertyName")

    private val UElement.identifier: String?
        get() = (this as? USimpleNameReferenceExpression)?.identifier

    private val UElement.resolvedName: String?
        get() = (this as? USimpleNameReferenceExpression)?.resolvedName

    override fun applicableAnnotations(): List<String> {
        return listOf("androidx.compose.runtime.snapshots.AutoboxingStateValueProperty")
    }

    override fun isApplicableAnnotationUsage(type: AnnotationUsageType): Boolean {
        return type == AnnotationUsageType.FIELD_REFERENCE
    }

    override fun visitAnnotationUsage(
        context: JavaContext,
        element: UElement,
        annotationInfo: AnnotationInfo,
        usageInfo: AnnotationUsageInfo
    ) {
        val resolvedPropertyName = element.identifier ?: "<unknown identifier>"
        val preferredPropertyName =
            annotationInfo.annotation.preferredPropertyName ?: "<unknown replacement>"

        val accessKind = when (element.resolvedName?.takeWhile { it.isLowerCase() }) {
            "get" -> "Reading"
            "set" -> "Assigning"
            else -> "Accessing"
        }

        context.report(
            AutoboxingStateValueProperty,
            element,
            context.getLocation(element),
            "$accessKind `$resolvedPropertyName` will cause an autoboxing operation. " +
                "Use `$preferredPropertyName` to avoid unnecessary allocations.",
            createPropertyReplacementQuickFix(
                resolvedPropertyName = resolvedPropertyName,
                preferredPropertyName = preferredPropertyName
            )
        )
    }

    private fun createPropertyReplacementQuickFix(
        resolvedPropertyName: String,
        preferredPropertyName: String
    ): LintFix {
        return fix().name("Replace with `$preferredPropertyName`")
            .replace()
            .text(resolvedPropertyName)
            .with(preferredPropertyName)
            .build()
    }

    companion object {

        val AutoboxingStateValueProperty = Issue.create(
            "AutoboxingStateValueProperty",
            "State access causes value to be autoboxed",
            "Avoid using the generic `value` property when using a specialized State type. " +
                "Reading or writing to the state's generic `value` property will result in an " +
                "unnecessary autoboxing operation. Prefer the specialized value property " +
                "(e.g. `intValue` for `MutableIntState`), or use property delegation to " +
                "avoid unnecessary allocations.",
            Category.PERFORMANCE, 3, Severity.WARNING,
            Implementation(
                AutoboxingStateValuePropertyDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
