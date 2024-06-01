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

package androidx.compose.ui.text.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.getParameterForArgument
import org.jetbrains.uast.skipParenthesizedExprDown

class LocaleInvalidLanguageTagDetector : Detector(), SourceCodeScanner {
    override fun getApplicableConstructorTypes() =
        listOf("androidx.compose.ui.text.intl.LocaleList", "androidx.compose.ui.text.intl.Locale")

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod
    ) {
        val languageTag =
            node.valueArguments
                .find {
                    val name = node.getParameterForArgument(it)?.name
                    name == "languageTag" || name == "languageTags"
                }
                ?.skipParenthesizedExprDown()

        val localeValue = languageTag?.evaluate() as? String ?: return
        val localeInvalid = localeValue.contains('_')

        if (localeInvalid) {
            val fixedLocaleValue = localeValue.replace('_', '-')
            context.report(
                InvalidLanguageTagDelimiter,
                context.getLocation(languageTag),
                "A hyphen (-), not an underscore (_) delimiter should be used in a language tag",
                LintFix.create()
                    .replace()
                    .name("Change $localeValue to $fixedLocaleValue")
                    .text(localeValue)
                    .with(fixedLocaleValue)
                    .autoFix()
                    .build()
            )
        }
    }

    companion object {
        val InvalidLanguageTagDelimiter =
            Issue.create(
                id = "InvalidLanguageTagDelimiter",
                briefDescription = "Undercore (_) is an unsupported delimiter for subtags",
                explanation =
                    "A language tag must be compliant with IETF BCP47, specifically a " +
                        "sequence of subtags must be separated by hyphens (-) instead of underscores (_)",
                category = Category.CORRECTNESS,
                priority = 3,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        LocaleInvalidLanguageTagDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                    )
            )
    }
}
