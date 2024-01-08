/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.annotation.replacewith.lint

import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement

class ReplaceWithDetector : Detector(), SourceCodeScanner {

    override fun applicableAnnotations(): List<String> = listOf(
        JAVA_REPLACE_WITH_ANNOTATION,
        KOTLIN_DEPRECATED_ANNOTATION,
    )

    override fun visitAnnotationUsage(
        context: JavaContext,
        usage: UElement,
        type: AnnotationUsageType,
        annotation: UAnnotation,
        qualifiedName: String,
        method: PsiMethod?,
        referenced: PsiElement?,
        annotations: List<UAnnotation>,
        allMemberAnnotations: List<UAnnotation>,
        allClassAnnotations: List<UAnnotation>,
        allPackageAnnotations: List<UAnnotation>
    ) {
        when (qualifiedName) {
            JAVA_REPLACE_WITH_ANNOTATION -> {
                var location = context.getLocation(usage)
                var expression = annotation.findAttributeValue("expression") ?.let { expr ->
                    ConstantEvaluator.evaluate(context, expr)
                } as? String ?: return

                if (method != null && usage is UCallExpression) {
                    // Per Kotlin documentation for ReplaceWith: For function calls, the replacement
                    // expression may contain argument names of the deprecated function, which will
                    // be substituted with actual parameters used in the call being updated.
                    val argumentNamesToActualParams = method.parameters.mapIndexed { index, param ->
                        param.name to usage.getArgumentForParameter(index)?.sourcePsi?.text
                    }.associate { it }

                    // Tokenize the replacement expression using a regex, replacing as we go. This
                    // isn't the most efficient approach (e.g. trie) but it's easy to write.
                    val search = Regex("\\w+")
                    var index = 0
                    do {
                        val matchResult = search.find(expression, index) ?: break
                        val replacement = argumentNamesToActualParams[matchResult.value]
                        if (replacement != null) {
                            expression = expression.replaceRange(matchResult.range, replacement)
                            index += replacement.length
                        } else {
                            index += matchResult.value.length
                        }
                    } while (index < expression.length)

                    // The expression may optionally specify a receiver or arguments, in which case
                    // we should include the originals in the replacement range.
                    val includeReceiver = Regex("^\\w+\\.\\w+.*\$").matches(expression)
                    val includeArguments = Regex("^.*\\w+\\(.*\\)$").matches(expression)
                    location = context.getCallLocation(usage, includeReceiver, includeArguments)
                }

                reportLintFix(context, usage, location, expression)
            }
        }
    }

    private fun reportLintFix(
        context: JavaContext,
        usage: UElement,
        location: Location,
        expression: String,
    ) {
        context.report(ISSUE, usage, location, "Replacement available",
            createLintFix(location, expression))
    }

    private fun createLintFix(location: Location, expression: String): LintFix =
        fix().replace().range(location).name("Replace with `$expression`").with(expression).build()

    companion object {
        private val IMPLEMENTATION = Implementation(
            ReplaceWithDetector::class.java,
            Scope.JAVA_FILE_SCOPE,
        )

        const val KOTLIN_DEPRECATED_ANNOTATION = "kotlin.Deprecated"
        const val JAVA_REPLACE_WITH_ANNOTATION = "androidx.annotation.ReplaceWith"

        val ISSUE = Issue.create(
            id = "ReplaceWith",
            briefDescription = "Replacement available",
            explanation = "A recommended replacement is available for this API usage.",
            category = Category.CORRECTNESS,
            priority = 4,
            severity = Severity.INFORMATIONAL,
            implementation = IMPLEMENTATION,
        )
    }
}
