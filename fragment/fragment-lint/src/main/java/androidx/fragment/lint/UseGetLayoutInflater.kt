/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.fragment.lint

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
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.getContainingUClass

/**
 * Lint check for detecting calls to [android.view.LayoutInflater.from]
 * while being invoked from DialogFragment
 */
class UseGetLayoutInflater : Detector(), SourceCodeScanner {

    companion object Issues {
        val ISSUE = Issue.create(
            id = "UseGetLayoutInflater",
            briefDescription = "Use getLayoutInflater() to get the LayoutInflater instead of " +
                "calling LayoutInflater.from(Context).",
            explanation = """Using LayoutInflater.from(Context) can return a LayoutInflater  \
                that does not have the correct theme.""",
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.WARNING,
            implementation = Implementation(
                UseGetLayoutInflater::class.java,
                Scope.JAVA_FILE_SCOPE
            ),
            androidSpecific = true
        )
    }

    override fun getApplicableMethodNames(): List<String>? {
        return listOf(UNWANTED_METHOD)
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val containingClass = method.containingClass ?: return
        val evaluator = context.evaluator
        if (evaluator.getQualifiedName(containingClass) == UNWANTED_CLASS &&
            evaluator.getParameterCount(method) == 1
        ) {
            if (!isKotlin(context.psiFile)) {
                startLintForJava(context, node)
            } else {
                startLintForKotlin(context, node)
            }
        }
    }

    private fun startLintForJava(context: JavaContext, node: UCallExpression) {
        if (node.getContainingUClass()?.superClassType?.name != DIALOG_FRAGMENT_CLASS) {
            return
        }
        val methodParameter = node.valueArguments[0].toString()

        context.report(
            issue = ISSUE,
            location = context.getLocation(node),
            message = "Use of LayoutInflater.from($methodParameter) detected. Consider using " +
                "${correctMethod(context)} instead",
            quickfixData = createFix(correctMethod(context), methodParameter)
        )
    }

    private fun startLintForKotlin(context: JavaContext, node: UCallExpression) {
        if (node.getContainingUClass()?.javaPsi?.text?.contains
            ("$DIALOG_FRAGMENT_CLASS()") == false
        ) {
            return
        }

        context.report(
            issue = ISSUE,
            location = context.getLocation(node),
            message = "Use of LayoutInflater.from(Context) detected. Consider using " +
                "${correctMethod(context)} instead",
            quickfixData = null
        )
    }

    private fun correctMethod(context: JavaContext): String {
        return if (isKotlin(context.psiFile)) {
            "layoutInflater"
        } else {
            "getLayoutInflater()"
        }
    }

    private fun createFix(correctMethod: String, parameter: String?): LintFix {
        return fix()
            .replace()
            .text("LayoutInflater.from($parameter)")
            .name("Replace with $correctMethod")
            .with(correctMethod)
            .autoFix()
            .build()
    }
}

private const val UNWANTED_CLASS = "android.view.LayoutInflater"
private const val UNWANTED_METHOD = "from"
private const val DIALOG_FRAGMENT_CLASS = "DialogFragment"
