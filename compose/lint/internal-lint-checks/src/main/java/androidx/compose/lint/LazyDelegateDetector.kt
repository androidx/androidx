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
import java.util.EnumSet
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UVariable

/** Lint [Detector] to prevent using `by lazy` in Compose internal code. */
class LazyDelegateDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UVariable::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitVariable(node: UVariable) {
                val property = node.sourcePsi as? KtProperty ?: return
                val delegate = property.delegate ?: return
                val expression = delegate.expression ?: return

                if (isLazyWithoutMode(expression)) {
                    context.report(
                        ISSUE,
                        delegate,
                        context.getLocation(delegate),
                        "Using `by lazy` has high overhead. Instead, either always execute " +
                            "the initialization or use a nullable or other invalid value by " +
                            "default to detect that the value hasn't been initialized. " +
                            "If `lazy` is intended, then the `mode` parameter should always be passed.",
                    )
                }
            }

            private tailrec fun isLazyWithoutMode(expression: KtExpression?): Boolean {
                if (expression == null) return false
                val callExpression =
                    when (expression) {
                        is KtCallExpression -> expression
                        is KtDotQualifiedExpression ->
                            expression.selectorExpression as? KtCallExpression
                        is KtParenthesizedExpression ->
                            return isLazyWithoutMode(expression.expression)
                        else -> null
                    } ?: return false

                val calleeName =
                    (callExpression.calleeExpression as? KtSimpleNameExpression)
                        ?.getReferencedName()
                if (calleeName != "lazy") return false

                return !hasModeParameter(callExpression)
            }

            private fun hasModeParameter(callExpression: KtCallExpression): Boolean {
                val valueArguments = callExpression.valueArguments
                if (valueArguments.isEmpty()) return false

                if (valueArguments.any { it.getArgumentName()?.asName?.asString() == "mode" }) {
                    return true
                }

                if (valueArguments.size >= 2) {
                    val firstArgName = valueArguments[0].getArgumentName()?.asName?.asString()
                    if (firstArgName == null || firstArgName == "mode" || firstArgName == "lock") {
                        return true
                    }
                }

                return false
            }
        }

    companion object {
        private const val LazyDelegateId = "LazyDelegate"

        val ISSUE =
            Issue.create(
                id = LazyDelegateId,
                briefDescription = "Using `by lazy` has high overhead",
                explanation =
                    "Using `by lazy` has high allocation and synchronization overhead. " +
                        "Developers should instead either always execute the initialization " +
                        "or use a nullable or other invalid value by default to detect that " +
                        "the value hasn't been initialized. " +
                        "If `lazy` is intended, then the `mode` parameter should always be passed.",
                category = Category.PERFORMANCE,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(LazyDelegateDetector::class.java, EnumSet.of(Scope.JAVA_FILE)),
            )
    }
}
