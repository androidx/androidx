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

import androidx.credentials.lint.RestoreCredentialConstants.CLASS_GET_CREDENTIAL_REQUEST
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_GET_CREDENTIAL_REQUEST_BUILDER
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_GET_RESTORE_CREDENTIAL_OPTION
import androidx.credentials.lint.RestoreCredentialConstants.RESTORE_CREDENTIALS_GUIDE_URL
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression

/**
 * Flags `GetCredentialRequest` constructions or `GetCredentialRequest.Builder` chains that combine
 * `GetRestoreCredentialOption` with any additional `CredentialOption`s.
 */
class RestoreCredentialOptionChainingDetector : Detector(), SourceCodeScanner {

    override fun getApplicableConstructorTypes(): List<String> =
        listOf(CLASS_GET_CREDENTIAL_REQUEST)

    override fun getApplicableMethodNames(): List<String> = listOf("createFrom", "build")

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod,
    ) {
        checkRequestExpression(context, node)
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val containingClass = method.containingClass ?: return
        val isRequestCreateFrom =
            method.name == "createFrom" &&
                (context.evaluator.inheritsFrom(
                    containingClass,
                    CLASS_GET_CREDENTIAL_REQUEST,
                    false,
                ) || containingClass.qualifiedName == "$CLASS_GET_CREDENTIAL_REQUEST.Companion")
        val isBuilderBuild =
            method.name == "build" &&
                context.evaluator.inheritsFrom(
                    containingClass,
                    CLASS_GET_CREDENTIAL_REQUEST_BUILDER,
                    false,
                )
        if (!isRequestCreateFrom && !isBuilderBuild) return

        checkRequestExpression(context, node)
    }

    private fun checkRequestExpression(context: JavaContext, requestCall: UCallExpression) {
        val optionExprs =
            RestoreCredentialConstants.extractCredentialOptionExpressions(context, requestCall)
        if (optionExprs.size <= 1) return

        val restoreOptionExpr: UExpression =
            optionExprs.firstOrNull { expr ->
                val optionClass = RestoreCredentialConstants.resolveExpressionClass(context, expr)
                optionClass != null &&
                    context.evaluator.inheritsFrom(
                        optionClass,
                        CLASS_GET_RESTORE_CREDENTIAL_OPTION,
                        false,
                    )
            } ?: return

        val anchor =
            if (restoreOptionExpr.sourcePsi?.containingFile == context.psiFile) {
                restoreOptionExpr
            } else {
                requestCall
            }

        context.report(
            ISSUE,
            requestCall,
            context.getLocation(anchor),
            "`GetRestoreCredentialOption` cannot be combined with other `CredentialOption` entries in a " +
                "single `GetCredentialRequest` (throws `IllegalArgumentException` at runtime). Request " +
                "`GetRestoreCredentialOption` alone first, then fall back to interactive sign-in options.",
        )
    }

    companion object {
        val ISSUE: Issue =
            Issue.create(
                id = "RestoreCredentialOptionChaining",
                briefDescription =
                    "`GetRestoreCredentialOption` combined with other credential options",
                explanation =
                    """
                `GetCredentialRequest` throws `IllegalArgumentException("Only a single \
                GetRestoreCredentialOption should be provided.")` at runtime whenever \
                `credentialOptions` contains `GetRestoreCredentialOption` alongside any other \
                `CredentialOption` (such as `GetPublicKeyCredentialOption`, `GetPasswordOption`, or \
                `GetSignInWithGoogleOption`).

                Perform silent restore retrieval first using a dedicated single-option \
                `GetCredentialRequest(listOf(GetRestoreCredentialOption(requestJson)))`, and only \
                invoke interactive sign-in options if no restore credential is returned.
            """,
                category = Category.CORRECTNESS,
                priority = 9,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        RestoreCredentialOptionChainingDetector::class.java,
                        Scope.JAVA_FILE_SCOPE,
                    ),
                moreInfo = RESTORE_CREDENTIALS_GUIDE_URL,
                androidSpecific = true,
            )
    }
}
