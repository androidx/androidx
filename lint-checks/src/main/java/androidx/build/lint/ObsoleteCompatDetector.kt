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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifier
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.uast.UAnonymousClass
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.skipParenthesizedExprDown

class ObsoleteCompatDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        CompatMethodHandler(context)

    companion object {
        val ISSUE =
            Issue.create(
                "ObsoleteCompatMethod",
                "Obsolete compatibility method can be deprecated with replacement",
                "Compatibility methods that consist of a single call to the platform SDK " +
                    "should be deprecated and provide a suggestion to replace with a direct call.",
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(ObsoleteCompatDetector::class.java, Scope.JAVA_FILE_SCOPE)
            )
    }
}

private class CompatMethodHandler(val context: JavaContext) : UElementHandler() {

    override fun visitMethod(node: UMethod) {
        // If this method probably a compat method?
        if (!node.isMaybeJetpackUtilityMethod()) return

        // Is this method probably contained in a Jetpack utility class?
        if (node.getContainingUClass()?.isMaybeJetpackUtilityClass() != true) return

        // Does it already have @Deprecated and @ReplaceWith annotations?
        val hasDeprecated = node.hasAnnotation("java.lang.Deprecated")
        val hasReplaceWith = node.hasAnnotation("androidx.annotation.ReplaceWith")
        val hasDeprecatedDoc =
            node.comments.any { comment -> comment.text.contains("@deprecated ") }
        if (hasDeprecated && hasReplaceWith && hasDeprecatedDoc) return

        // Compat methods take the wrapped class as the first parameter.
        val firstParameter = node.javaPsi.parameterList.parameters.firstOrNull() ?: return

        // Ensure that we're dealing with a single-line method that operates on the wrapped class.
        val expression =
            (node.uastBody as? UBlockExpression)
                ?.expressions
                ?.singleOrNull()
                ?.unwrapReturnExpression()
                ?.skipParenthesizedExprDown() as? UQualifiedReferenceExpression
        val receiver = expression?.unwrapReceiver()
        if (firstParameter != receiver) return

        val lintFix = LintFix.create().composite().name("Replace obsolete compat method")

        if (!hasDeprecatedDoc) {
            val docLink =
                when (expression.selector) {
                    is UCallExpression -> {
                        val methodCall = expression.selector as UCallExpression
                        val className = (methodCall.receiverType as PsiClassReferenceType).className
                        val methodName = methodCall.methodName
                        val argTypes =
                            methodCall.typeArguments.map { psiType ->
                                (psiType as PsiClassReferenceType).className
                            }
                        "$className#$methodName(${argTypes.joinToString(", ")})"
                    }
                    is USimpleNameReferenceExpression -> {
                        val fieldName =
                            (expression.selector as USimpleNameReferenceExpression).resolvedName
                        val className =
                            (expression.receiver.getExpressionType() as PsiClassReferenceType)
                                .className
                        "$className#$fieldName"
                    }
                    else -> {
                        // We don't know how to handle this type of qualified reference.
                        return
                    }
                }
            val docText = "@deprecated Call {@link $docLink} directly."
            val javadocFix =
                buildInsertJavadocFix(context, node, docText)
                    .autoFix()
                    .shortenNames()
                    .reformat(true)
                    .build()
            lintFix.add(javadocFix)
        }

        if (!hasReplaceWith) {
            val replacement =
                expression.javaPsi!!.text!!.replace("\"", "\\\"").replace(Regex("\n\\s*"), "")
            lintFix.add(
                LintFix.create()
                    .name("Annotate with @ReplaceWith")
                    .annotate(
                        source = "androidx.annotation.ReplaceWith(expression = \"$replacement\")",
                        context = context,
                        element = node,
                        replace = false
                    )
                    .autoFix()
                    .build()
            )
        }

        if (!hasDeprecated) {
            lintFix.add(
                LintFix.create()
                    .name("Annotate with @Deprecated")
                    .annotate(
                        source = "java.lang.Deprecated",
                        context = context,
                        element = node,
                        replace = false
                    )
                    .autoFix()
                    .build()
            )
        }

        val incident =
            Incident(context)
                .issue(ObsoleteCompatDetector.ISSUE)
                .location(context.getNameLocation(node))
                .message("Obsolete compat method should provide replacement")
                .scope(node)
                .fix(lintFix.build())
        context.report(incident)
    }
}

fun buildInsertJavadocFix(
    context: JavaContext,
    node: UMethod,
    docText: String
): LintFix.ReplaceStringBuilder {
    val javadocNode = node.comments.lastOrNull { it.text.startsWith("/**") }
    val javadocFix = LintFix.create().name("Add @deprecated Javadoc annotation").replace()
    if (javadocNode != null) {
        // Append to the existing block comment before the close.
        val docEndOffset = javadocNode.text.lastIndexOf("*/")
        val insertAt = context.getRangeLocation(javadocNode, docEndOffset, 2)
        val replacement = applyIndentToInsertion(context, insertAt, "* $docText")
        javadocFix.range(insertAt).beginning().with(replacement)
    } else {
        // Insert a new comment before the declaration or any annotations.
        val insertAt = context.getLocation(node.annotations.firstOrNull() ?: node.modifierList)
        val replacement = applyIndentToInsertion(context, insertAt, "/** $docText */")
        javadocFix.range(insertAt).beginning().with(replacement)
    }
    return javadocFix
}

fun applyIndentToInsertion(context: JavaContext, insertAt: Location, replacement: String): String {
    val contents = context.getContents()!!
    val start = insertAt.start!!
    val startOffset = start.offset
    var lineBegin = startOffset
    while (lineBegin > 0) {
        val c = contents[lineBegin - 1]
        if (!Character.isWhitespace(c)) {
            break
        } else if (c == '\n' || lineBegin == 1) {
            if (startOffset > lineBegin) {
                val indent = contents.substring(lineBegin, startOffset)
                return replacement + "\n" + indent
            }
            break
        } else lineBegin--
    }
    return replacement
}

fun UExpression.unwrapReceiver(): PsiElement? =
    ((this as? UQualifiedReferenceExpression)?.receiver?.skipParenthesizedExprDown()
            as? UResolvable)
        ?.resolve()

fun UExpression.unwrapReturnExpression(): UExpression =
    (this as? UReturnExpression)?.returnExpression ?: this

@Suppress("UnstableApiUsage") // hasModifier, JvmModifier.PUBLIC
fun UMethod.isMaybeJetpackUtilityMethod(): Boolean {
    return isStatic && !isConstructor && hasModifier(JvmModifier.PUBLIC)
}

fun UClass.isMaybeJetpackUtilityClass(): Boolean {
    return !(isInterface ||
        isEnum ||
        hasModifierProperty(PsiModifier.ABSTRACT) ||
        this is UAnonymousClass ||
        // If this is a subclass, then don't flag it.
        supers.any { !it.qualifiedName.equals("java.lang.Object") } ||
        // Don't run for Kotlin, for now at least
        containingFile.fileType == KotlinFileType.INSTANCE)
}
