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

import androidx.credentials.lint.RestoreCredentialConstants.CLASS_BACKUP_AGENT
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_CREDENTIAL_MANAGER
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_MUTABLE_CONTEXT_WRAPPER
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UThisExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.skipParenthesizedExprDown

class CredentialManagerMutableContextDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> =
        listOf("getCredential", "getCredentialAsync", "createCredential", "createCredentialAsync")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        // Only target androidx.credentials.CredentialManager (and its implementations)
        val containingClass = method.containingClass ?: return
        if (!context.evaluator.inheritsFrom(containingClass, CLASS_CREDENTIAL_MANAGER, false)) {
            return
        }

        if (node.valueArguments.isEmpty()) return

        // Headless Restore Credential operations do not present an Activity-bound UI sheet.
        if (RestoreCredentialConstants.isNonUiRestoreCredentialCall(context, node, method)) {
            return
        }

        val contextArg =
            RestoreCredentialConstants.findArgument(context, node, method, "context", 0) ?: return
        val typeClass =
            RestoreCredentialConstants.resolveExpressionClass(context, contextArg) ?: return

        // Exempt MutableContextWrapper and headless BackupAgent contexts.
        if (
            context.evaluator.inheritsFrom(typeClass, CLASS_MUTABLE_CONTEXT_WRAPPER, false) ||
                context.evaluator.inheritsFrom(typeClass, CLASS_BACKUP_AGENT, false)
        ) {
            return
        }

        if (contextArg.skipParenthesizedExprDown() is UThisExpression) {
            val enclosingClass = node.getParentOfType<UClass>(true)?.javaPsi
            if (
                enclosingClass != null &&
                    context.evaluator.inheritsFrom(enclosingClass, CLASS_BACKUP_AGENT, false)
            ) {
                return
            }
        }

        context.report(
            ISSUE,
            node,
            context.getLocation(contextArg),
            "Use a `MutableContextWrapper` instead of a raw `${typeClass.name}` for credential operations " +
                "to properly handle activity configuration changes.",
        )
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
                the CredentialManager can track and swap the base context correctly, preventing memory leaks and orphaned callbacks. \
                Headless `BackupAgent` contexts and non-UI Restore Credential operations are exempt.
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
