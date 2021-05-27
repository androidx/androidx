/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.fragment.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UPostfixExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.kotlin.KotlinUFunctionCallExpression
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.tryResolve
import java.util.Locale

/**
 * Androidx added new "require____()" versions of common "get___()" APIs, such as
 * getContext/getActivity/getArguments/etc. Rather than wrap these in something like
 * requireNotNull() or null-checking with `!!` in Kotlin, using these APIs will allow the
 * underlying component to try to tell you _why_ it was null, and thus yield a better error
 * message.
 */
@Suppress("UnstableApiUsage")
class UseRequireInsteadOfGet : Detector(), SourceCodeScanner {

    companion object {
        val ISSUE: Issue = Issue.create(
            "UseRequireInsteadOfGet",
            "Use the 'require_____()' API rather than 'get____()' API for more " +
                "descriptive error messages when it's null.",
            """
            AndroidX added new "require____()" versions of common "get___()" APIs, such as \
            getContext/getActivity/getArguments/etc. Rather than wrap these in something like \
            requireNotNull(), using these APIs will allow the underlying component to try \
            to tell you _why_ it was null, and thus yield a better error message.
            """,
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            Implementation(UseRequireInsteadOfGet::class.java, Scope.JAVA_FILE_SCOPE)
        )

        private const val FRAGMENT_FQCN = "androidx.fragment.app.Fragment"
        internal val REQUIRABLE_METHODS = setOf(
            "getArguments",
            "getContext",
            "getActivity",
            "getFragmentManager",
            "getHost",
            "getParentFragment",
            "getView"
        )
        // Convert 'getArguments' to 'arguments'
        internal val REQUIRABLE_REFERENCES = REQUIRABLE_METHODS.map {
            it.removePrefix("get").decapitalize(Locale.US)
        }
        internal val KNOWN_NULLCHECKS = setOf(
            "checkNotNull",
            "requireNonNull"
        )
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        super.visitMethodCall(context, node, method)
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UCallExpression::class.java, USimpleNameReferenceExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        val isKotlin = isKotlin(context.psiFile)
        return object : UElementHandler() {

            /** This covers Kotlin accessor syntax expressions like "fragment.arguments" */
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                val parent = node.uastParent
                if (parent is UQualifiedReferenceExpression) {
                    checkReferenceExpression(parent, node.identifier) {
                        parent.receiver.getExpressionType()
                            ?.let { context.evaluator.findClass(it.canonicalText) }
                    }
                } else {
                    // It's a member of the enclosing class
                    checkReferenceExpression(node, node.identifier) {
                        node.getContainingUClass()
                    }
                }
            }

            private fun checkReferenceExpression(
                node: UExpression,
                identifier: String,
                resolveEnclosingClass: () -> PsiClass?
            ) {
                if (identifier in REQUIRABLE_REFERENCES) {
                    // If this is a local variable do nothing
                    // We are doing this to avoid false positives on local variables that shadow
                    // Kotlin property accessors.  There is probably a better way to organize
                    // this Lint rule.
                    val element = node.tryResolve()
                    if (element != null && element.toUElement() is ULocalVariable) {
                        return
                    }
                    val enclosingClass = resolveEnclosingClass() ?: return
                    if (context.evaluator.extendsClass(enclosingClass, FRAGMENT_FQCN, false)) {
                        checkForIssue(node, identifier)
                    }
                }
            }

            /** This covers function/method calls like "fragment.getArguments()" */
            override fun visitCallExpression(node: UCallExpression) {
                val targetMethod = node.resolve() ?: return
                val containingClass = targetMethod.containingClass ?: return
                if (targetMethod.name in REQUIRABLE_METHODS &&
                    context.evaluator.extendsClass(containingClass, FRAGMENT_FQCN, false)
                ) {
                    checkForIssue(node, targetMethod.name, "${targetMethod.name}()")
                }
            }

            /** Called only when we know we're looking at an exempted method call type. */
            private fun checkForIssue(
                node: UExpression,
                targetMethodName: String,
                targetExpression: String = targetMethodName
            ) {
                // Note we go up potentially two parents - the first one may just be the qualified reference expression
                val nearestNonQualifiedReferenceParent =
                    node.nearestNonQualifiedReferenceParent ?: return
                if (isKotlin && nearestNonQualifiedReferenceParent.isNullCheckBlock()) {
                    // We're a double-bang expression (!!)
                    val parentSourceToReplace =
                        nearestNonQualifiedReferenceParent.asSourceString()
                    var correctMethod = correctMethod(
                        parentSourceToReplace,
                        "$targetExpression!!",
                        targetMethodName
                    )
                    if (correctMethod == parentSourceToReplace) {
                        correctMethod = parentSourceToReplace.replace(
                            "$targetExpression?",
                            "$targetExpression!!"
                        ).replaceFirstOccurrenceAfter("!!", "", "$targetExpression!!")
                    }
                    report(nearestNonQualifiedReferenceParent, parentSourceToReplace, correctMethod)
                } else if (nearestNonQualifiedReferenceParent is UCallExpression) {
                    // See if we're in a "requireNotNull(...)" or similar expression
                    val enclosingMethodCall =
                        nearestNonQualifiedReferenceParent.resolve() ?: return

                    if (enclosingMethodCall.name in KNOWN_NULLCHECKS) {
                        // Only match for single (specified) parameter. If existing code had a
                        // custom failure message, we don't want to overwrite it.
                        val singleParameterSpecified =
                            isSingleParameterSpecified(
                                enclosingMethodCall,
                                nearestNonQualifiedReferenceParent
                            )

                        if (singleParameterSpecified) {
                            // Grab the source of this argument as it's represented.
                            val source = nearestNonQualifiedReferenceParent.valueArguments[0]
                                .asSourceString()
                            val parentToReplace =
                                nearestNonQualifiedReferenceParent.fullyQualifiedNearestParent()
                                    .asSourceString()
                            val correctMethod =
                                correctMethod(source, targetExpression, targetMethodName)
                            report(
                                nearestNonQualifiedReferenceParent,
                                parentToReplace,
                                correctMethod
                            )
                        }
                    }
                }
            }

            private fun isSingleParameterSpecified(
                enclosingMethodCall: PsiMethod,
                nearestNonQualifiedRefParent: UCallExpression
            ) = enclosingMethodCall.parameterList.parametersCount == 1 ||
                (
                    isKotlin &&
                        nearestNonQualifiedRefParent is KotlinUFunctionCallExpression &&
                        nearestNonQualifiedRefParent.getArgumentForParameter(1) == null
                    )

            private fun correctMethod(
                source: String,
                targetExpression: String,
                targetMethodName: String
            ): String {
                return source.replace(
                    targetExpression,
                    "require${targetMethodName.removePrefix("get").capitalize(Locale.US)}()"
                )
            }

            // Replaces the first occurrence of a substring after given String
            private fun String.replaceFirstOccurrenceAfter(
                oldValue: String,
                newValue: String,
                prefix: String
            ): String = prefix + substringAfter(prefix).replaceFirst(oldValue, newValue)

            private fun report(node: UElement, targetExpression: String, correctMethod: String) {
                context.report(
                    ISSUE,
                    context.getLocation(node),
                    "Use $correctMethod instead of $targetExpression",
                    LintFix.create()
                        .replace()
                        .name("Replace with $correctMethod")
                        .text(targetExpression)
                        .with(correctMethod)
                        .autoFix()
                        .build()
                )
            }
        }
    }
}

/**
 * Copy of the currently experimental Kotlin stdlib version. Can be removed once the stdlib version
 * comes out of experimental.
 */
internal fun String.decapitalize(locale: Locale): String {
    return if (isNotEmpty() && !this[0].isLowerCase()) {
        substring(0, 1).lowercase(locale) + substring(1)
    } else {
        this
    }
}

/**
 * Copy of the currently experimental Kotlin stdlib version. Can be removed once the stdlib version
 * comes out of experimental.
 */
internal fun String.capitalize(locale: Locale): String {
    if (isNotEmpty()) {
        val firstChar = this[0]
        if (firstChar.isLowerCase()) {
            return buildString {
                val titleChar = firstChar.titlecaseChar()
                if (titleChar != firstChar.uppercaseChar()) {
                    append(titleChar)
                } else {
                    append(this@capitalize.substring(0, 1).uppercase(locale))
                }
                append(this@capitalize.substring(1))
            }
        }
    }
    return this
}

internal fun UElement.isNullCheckBlock(): Boolean {
    return this is UPostfixExpression && operator.text == "!!"
}
