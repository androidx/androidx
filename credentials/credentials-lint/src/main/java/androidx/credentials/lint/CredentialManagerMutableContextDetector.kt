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

package androidx.credentials.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.toUElementOfType

class CredentialManagerMutableContextDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> =
        listOf("getCredential", "getCredentialAsync", "createCredential", "createCredentialAsync")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        // Only target androidx.credentials.CredentialManager
        if (!context.evaluator.isMemberInClass(method, "androidx.credentials.CredentialManager"))
            return

        val args = node.valueArguments
        if (args.isEmpty()) return

        // The context is a named argument. We shouldn't rely on positional indices.
        val mapping = context.evaluator.computeArgumentMapping(node, method)
        val contextArgEntry = mapping.entries.find { it.value.name == "context" } ?: return
        val contextArg = contextArgEntry.key
        var type = contextArg.getExpressionType() ?: return

        var typeClass = context.evaluator.getTypeClass(type)

        // Enhance check to resolve local variables!
        if (contextArg is UReferenceExpression) {
            val resolvedElement = contextArg.resolve()
            if (resolvedElement is PsiVariable) {
                val uVariable = resolvedElement.toUElementOfType<UVariable>()
                val initializerType = uVariable?.uastInitializer?.getExpressionType()
                if (initializerType != null) {
                    typeClass = context.evaluator.getTypeClass(initializerType) ?: typeClass
                }
            }
        }

        if (typeClass != null) {
            // Check if the argument's type is or inherits from MutableContextWrapper
            val inheritsMutableContextWrapper =
                context.evaluator.inheritsFrom(
                    typeClass,
                    "android.content.MutableContextWrapper",
                    false,
                )

            if (!inheritsMutableContextWrapper) {
                // If it is any raw context or activity that is NOT a MutableContextWrapper, report
                // an error
                context.report(
                    ISSUE,
                    node,
                    context.getLocation(contextArg),
                    "Use a `MutableContextWrapper` instead of a raw `${typeClass.name}` for credential operations " +
                        "to properly handle activity configuration changes.",
                )
            }
        }
    }

    companion object {
        val ISSUE =
            Issue.create(
                id = "CredManMutableContext",
                briefDescription = "Missing MutableContextWrapper for CredentialManager",
                explanation =
                    """
                When calling CredentialManager methods such as `getCredential`, it is highly \
                recommended to pass a `MutableContextWrapper` instead of a raw `Context` or `Activity`. \
                This ensures that if the underlying Activity undergoes a configuration change (like screen rotation), \
                the CredentialManager can track and swap the base context correctly, preventing memory leaks and orphaned callbacks.
            """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.WARNING,
                implementation =
                    Implementation(
                        CredentialManagerMutableContextDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )
    }
}
