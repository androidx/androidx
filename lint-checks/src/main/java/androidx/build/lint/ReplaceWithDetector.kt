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
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.impl.source.tree.TreeElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression

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

        var (expression, imports) = when (qualifiedName) {
            KOTLIN_DEPRECATED_ANNOTATION -> {
                val replaceWith = annotation.findAttributeValue("replaceWith")?.unwrap()
                    as? UCallExpression ?: return
                val expression = replaceWith.valueArguments.getOrNull(0)?.parseLiteral() ?: return
                val imports = replaceWith.valueArguments.getOrNull(1)?.parseVarargLiteral()
                    ?: emptyList()
                Pair(expression, imports)
            }
            JAVA_REPLACE_WITH_ANNOTATION -> {
                val expression = annotation.findAttributeValue("expression")?.let { expr ->
                    ConstantEvaluator.evaluate(context, expr)
                } as? String ?: return
                val imports = annotation.getAttributeValueVarargLiteral("imports")
                Pair(expression, imports)
            }
            else -> return
        }

        var location = context.getLocation(usage)
        val includeReceiver = Regex("^\\w+\\.\\w+.*\$").matches(expression)
        val includeArguments = Regex("^.*\\w+\\(.*\\)$").matches(expression)

        if (referenced is PsiMethod && usage is UCallExpression) {
            // Per Kotlin documentation for ReplaceWith: For function calls, the replacement
            // expression may contain argument names of the deprecated function, which will
            // be substituted with actual parameters used in the call being updated.
            val argsToParams = referenced.parameters.mapIndexed { index, param ->
                param.name to usage.getArgumentForParameter(index)?.asSourceString()
            }.toMap()

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

            location = when (val sourcePsi = usage.sourcePsi) {
                is PsiNewExpression -> {
                    // The expression should never specify "new", but if it specifies a
                    // receiver then we should replace the call to "new". For example, if
                    // we're replacing `new Clazz("arg")` with `ClazzCompat.create("arg")`.
                    context.getConstructorLocation(
                        usage, sourcePsi, includeReceiver, includeArguments
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

        reportLintFix(context, usage, location, expression, imports)
    }

    private fun reportLintFix(
        context: JavaContext,
        usage: UElement,
        location: Location,
        expression: String,
        imports: List<String>,
    ) {
        context.report(ISSUE, usage, location, "Replacement available",
            createLintFix(context, location, expression, imports))
    }

    private fun createLintFix(
        context: JavaContext,
        location: Location,
        expression: String,
        imports: List<String>
    ): LintFix {
        val name = "Replace with `$expression`"
        val lintFixBuilder = fix().composite().name(name)
        lintFixBuilder.add(
            fix()
                .replace()
                .range(location)
                .name(name)
                .with(expression)
                .build()
        )
        if (imports.isNotEmpty()) {
            lintFixBuilder.add(fix().import(context, add = imports).build())
        }
        return lintFixBuilder.build()
    }

    /**
     * Add imports.
     *
     * @return a string replace builder
     */
    fun LintFix.Builder.import(
        context: JavaContext,
        add: List<String>
    ): LintFix.ReplaceStringBuilder {
        val isKotlin = isKotlin(context.uastFile!!.lang)
        val lastImport = context.uastFile?.imports?.lastOrNull()
        val packageElem = when (val psiFile = context.psiFile) {
            is PsiJavaFile -> psiFile.packageStatement
            is KtFile -> psiFile.packageDirective?.psiOrParent
            else -> null
        }

        // Build the imports block. Leave any ordering or formatting up to the client.
        val prependImports = when {
            lastImport != null -> "\n"
            packageElem != null -> "\n\n"
            else -> ""
        }
        val appendImports = when {
            lastImport != null -> ""
            packageElem != null -> ""
            else -> "\n"
        }
        val formattedImports = add.joinToString("\n") { "import " + if (isKotlin) it else "$it;" }
        val importsText = prependImports + formattedImports + appendImports

        // Append after any existing imports, after the package declaration, or at the beginning of
        // the file if there are no imports and no package declaration.
        val appendLocation = (lastImport ?: packageElem) ?.let { context.getLocation(it) }
            ?: Location.create(context.file, context.getContents(), 0, 0)
        val replaceBuilder = replace().range(appendLocation).end().with(importsText)
        return replaceBuilder.autoFix()
    }

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
    call: UCallExpression,
    newExpression: PsiNewExpression,
    includeNew: Boolean,
    includeArguments: Boolean
): Location {
    if (includeArguments) {
        call.valueArguments.lastOrNull()?.let { lastArgument ->
            val argumentsEnd = lastArgument.sourcePsi?.endOffset
            val callEnds = newExpression.endOffset
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

/**
 * @return the value of the specified vararg attribute as a list of String literals, or an empty
 * list if not specified
 */
fun UAnnotation.getAttributeValueVarargLiteral(name: String): List<String> =
    findDeclaredAttributeValue(name)?.parseVarargLiteral() ?: emptyList()

fun UExpression.parseVarargLiteral(): List<String> =
    when (val expr = this.unwrap()) {
        is ULiteralExpression -> listOfNotNull(expr.parseLiteral())
        is UCallExpression -> expr.valueArguments.mapNotNull { it.parseLiteral() }
        else -> emptyList()
    }

fun UExpression.parseLiteral(): String? =
    when (val expr = this.unwrap()) {
        is ULiteralExpression -> expr.value.toString()
        else -> null
    }

fun UExpression.unwrap(): UExpression =
    when (this) {
        is UParenthesizedExpression -> expression.unwrap()
        is UPolyadicExpression -> operands.singleOrNull()?.unwrap() ?: this
        is UQualifiedReferenceExpression -> selector.unwrap()
        else -> this
    }
