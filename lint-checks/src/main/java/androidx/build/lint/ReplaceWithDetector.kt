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

package androidx.build.lint

import com.android.tools.lint.detector.api.AnnotationInfo
import com.android.tools.lint.detector.api.AnnotationUsageInfo
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
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.tree.TreeElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.java.JavaConstructorUCallExpression

class ReplaceWithDetector : Detector(), SourceCodeScanner {

    override fun applicableAnnotations(): List<String> = listOf(
        JAVA_REPLACE_WITH_ANNOTATION,
        KOTLIN_DEPRECATED_ANNOTATION,
    )

    override fun visitAnnotationUsage(
        context: JavaContext,
        element: UElement,
        annotationInfo: AnnotationInfo,
        usageInfo: AnnotationUsageInfo
    ) {
        val qualifiedName = annotationInfo.qualifiedName
        val annotation = annotationInfo.annotation
        val referenced = usageInfo.referenced
        val usage = usageInfo.usage
        val type = usageInfo.type

        // Ignore callbacks for assignment on the original declaration of an annotated field.
        if (type == AnnotationUsageType.ASSIGNMENT_RHS && usage.uastParent == referenced) return

        when (qualifiedName) {
            JAVA_REPLACE_WITH_ANNOTATION -> {
                var location = context.getLocation(usage)
                var expression = annotation.findAttributeValue("expression") ?.let { expr ->
                    ConstantEvaluator.evaluate(context, expr)
                } as? String ?: return

                val includeReceiver = Regex("^\\w+\\.\\w+.*\$").matches(expression)
                val includeArguments = Regex("^.*\\w+\\(.*\\)$").matches(expression)

                if (referenced is PsiMethod && usage is UCallExpression) {
                    // Per Kotlin documentation for ReplaceWith: For function calls, the replacement
                    // expression may contain argument names of the deprecated function, which will
                    // be substituted with actual parameters used in the call being updated.
                    val argsToParams = referenced.parameters.mapIndexed { index, param ->
                        param.name to usage.getArgumentForParameter(index)?.sourcePsi?.text
                    }.associate { it }

                    // Tokenize the replacement expression using a regex, replacing as we go. This
                    // isn't the most efficient approach (e.g. trie) but it's easy to write.
                    val search = Regex("\\w+")
                    var index = 0
                    do {
                        val matchResult = search.find(expression, index) ?: break
                        val replacement = argsToParams[matchResult.value]
                        if (replacement != null) {
                            expression = expression.replaceRange(matchResult.range, replacement)
                            index += replacement.length
                        } else {
                            index += matchResult.value.length
                        }
                    } while (index < expression.length)

                    location = when (usage) {
                        is JavaConstructorUCallExpression -> {
                            // The expression should never specify "new", but if it specifies a
                            // receiver then we should replace the call to "new". For example, if
                            // we're replacing `new Clazz("arg")` with `ClazzCompat.create("arg")`.
                            context.getConstructorLocation(
                                usage, includeReceiver, includeArguments
                            )
                        }
                        else -> {
                            // The expression may optionally specify a receiver or arguments, in
                            // which case we should include the originals in the replacement range.
                            context.getCallLocation(usage, includeReceiver, includeArguments)
                        }
                    }
                } else if (referenced is PsiField && usage is USimpleNameReferenceExpression) {
                    // The expression may optionally specify a receiver, in which case we should
                    // include the original in the replacement range.
                    if (includeReceiver) {
                        // If this is a qualified reference and we're including the "receiver" then
                        // we should replace the fully-qualified expression.
                        (usage.uastParent as? UQualifiedReferenceExpression)?.let { reference ->
                            location = context.getLocation(reference)
                        }
                    }
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

/**
 * Modified version of [JavaContext.getRangeLocation] that uses the `classReference` instead of the
 * `receiver` to handle trimming the `new` keyword from the start of a Java constructor call.
 */
fun JavaContext.getConstructorLocation(
    call: JavaConstructorUCallExpression,
    includeNew: Boolean,
    includeArguments: Boolean
): Location {
    if (includeArguments) {
        call.valueArguments.lastOrNull()?.let { lastArgument ->
            val argumentsEnd = lastArgument.sourcePsi?.endOffset
            val callEnds = call.sourcePsi.endOffset
            if (argumentsEnd != null && argumentsEnd > callEnds) {
                // The call element has arguments that are outside of its own range.
                // This typically means users are making a function call using
                // assignment syntax, e.g. key = value instead of setKey(value);
                // here the call range is just "key" and the arguments range is "value".
                // Create a range which merges these two.
                val startElement = if (!includeNew) call.classReference ?: call else call
                // Work around UAST bug where the value argument list points directly to the
                // string content node instead of a node containing the opening and closing
                // tokens as well. We need to include the closing tags in the range as well!
                val next = (lastArgument.sourcePsi as? KtLiteralStringTemplateEntry)
                    ?.nextSibling as? TreeElement
                val delta =
                    if (next != null && next.elementType == KtTokens.CLOSING_QUOTE) {
                        next.textLength
                    } else {
                        0
                    }
                return getRangeLocation(startElement, 0, lastArgument, delta)
            }
        }
    }

    val classReference = call.classReference
    if (includeNew || classReference == null) {
        if (includeArguments) {
            // Method with arguments but no receiver is the default range for UCallExpressions
            // modulo the scenario with arguments outside the call, handled at the beginning
            // of this method
            return getLocation(call)
        }
        // Just the method name
        val methodIdentifier = call.methodIdentifier
        if (methodIdentifier != null) {
            return getLocation(methodIdentifier)
        }
    } else {
        if (!includeArguments) {
            val methodIdentifier = call.methodIdentifier
            if (methodIdentifier != null) {
                return getRangeLocation(classReference, 0, methodIdentifier, 0)
            }
        }

        // Use PsiElement variant of getRangeLocation because UElement variant returns wrong results
        // when the `from` argument starts after the `to` argument, as it does for the constructor
        // class reference.
        return getRangeLocation(classReference.javaPsi!!, 0, call.javaPsi!!, 0)
    }

    return getLocation(call)
}
