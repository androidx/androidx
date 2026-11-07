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

import androidx.credentials.lint.RestoreCredentialConstants.CLASS_CLEAR_CREDENTIAL_STATE_REQUEST
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_CREDENTIAL_MANAGER
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_GET_RESTORE_CREDENTIAL_OPTION
import androidx.credentials.lint.RestoreCredentialConstants.CLEAR_CREDENTIAL_METHODS
import androidx.credentials.lint.RestoreCredentialConstants.RESTORE_CREDENTIALS_GUIDE_URL
import androidx.credentials.lint.RestoreCredentialConstants.TYPE_CLEAR_CREDENTIAL_STATE
import androidx.credentials.lint.RestoreCredentialConstants.TYPE_CLEAR_RESTORE_CREDENTIAL
import androidx.credentials.lint.RestoreCredentialConstants.findArgument
import androidx.credentials.lint.RestoreCredentialConstants.unwrapVariableInitializer
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.PartialResult
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.skipParenthesizedExprDown

/**
 * Flags apps that integrate Restore Credentials and call `CredentialManager.clearCredentialState`
 * with the default `ClearCredentialStateRequest()` (`TYPE_CLEAR_CREDENTIAL_STATE`) without also
 * clearing `TYPE_CLEAR_RESTORE_CREDENTIAL`, which leaves the restore key active after user sign-out
 * and causes post-logout "ghost auto-login" (`RestoreCredentialMissingLogoutClear`).
 */
class RestoreCredentialLogoutClearDetector : Detector(), SourceCodeScanner {

    override fun getApplicableConstructorTypes(): List<String> =
        listOf(
            CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST,
            CLASS_GET_RESTORE_CREDENTIAL_OPTION,
            CLASS_CLEAR_CREDENTIAL_STATE_REQUEST,
        )

    override fun getApplicableMethodNames(): List<String> = CLEAR_CREDENTIAL_METHODS.toList()

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod,
    ) {
        val ownerName = constructor.containingClass?.qualifiedName ?: return
        val map = context.getPartialResults(ISSUE).map()

        if (
            ownerName == CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST ||
                ownerName == CLASS_GET_RESTORE_CREDENTIAL_OPTION
        ) {
            map.put(KEY_USES_RESTORE_CREDENTIALS, true)
            return
        }

        if (ownerName == CLASS_CLEAR_CREDENTIAL_STATE_REQUEST) {
            val requestTypeArg =
                findArgument(context, node, constructor, "requestType", 0) ?: return
            if (isClearRestoreCredentialType(context, requestTypeArg)) {
                map.put(KEY_CLEARS_RESTORE_CREDENTIAL, true)
            }
        }
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val containingClass = method.containingClass ?: return
        if (!context.evaluator.inheritsFrom(containingClass, CLASS_CREDENTIAL_MANAGER, false))
            return
        if (context.driver.isSuppressed(context, ISSUE, node as UElement)) return

        val requestArg = findArgument(context, node, method, "request", 0) ?: return
        val unwrappedArg = unwrapVariableInitializer(requestArg)
        if (!isStandardClearRequestConstructor(context, unwrappedArg)) return

        val reportLocation =
            context.getCallLocation(node, includeReceiver = true, includeArguments = true)
        val fixSpec = computeAppendRestoreClearFixSpec(context, node)

        val map = context.getPartialResults(ISSUE).map()
        val index = map.getInt(KEY_INCIDENT_COUNT, 0) ?: 0
        map.put("$KEY_CALL_LOCATION_PREFIX$index", reportLocation)
        if (fixSpec != null) {
            map.put("$KEY_FIX_LOCATION_PREFIX$index", fixSpec.location)
            map.put("$KEY_FIX_TEXT_PREFIX$index", fixSpec.oldText)
            map.put("$KEY_FIX_REPLACEMENT_PREFIX$index", fixSpec.replacement)
        }
        map.put(KEY_INCIDENT_COUNT, index + 1)
    }

    override fun checkPartialResults(context: Context, partialResults: PartialResult) {
        if (context.project.isLibrary) return

        val usesRestoreCredentials =
            partialResults.maps().any { it.getBoolean(KEY_USES_RESTORE_CREDENTIALS) == true }
        if (!usesRestoreCredentials) return

        val clearsRestoreCredential =
            partialResults.maps().any { it.getBoolean(KEY_CLEARS_RESTORE_CREDENTIAL) == true }
        if (clearsRestoreCredential) return

        for (moduleMap in partialResults.maps()) {
            val count = moduleMap.getInt(KEY_INCIDENT_COUNT, 0) ?: 0
            for (i in 0 until count) {
                val callLocation = moduleMap.getLocation("$KEY_CALL_LOCATION_PREFIX$i") ?: continue
                val fixLocation = moduleMap.getLocation("$KEY_FIX_LOCATION_PREFIX$i")
                val fixText = moduleMap.getString("$KEY_FIX_TEXT_PREFIX$i", null)
                val fixReplacement = moduleMap.getString("$KEY_FIX_REPLACEMENT_PREFIX$i", null)

                val fix =
                    if (fixLocation != null && fixText != null && fixReplacement != null) {
                        LintFix.create()
                            .name(
                                "Also clear RestoreCredential state (TYPE_CLEAR_RESTORE_CREDENTIAL)"
                            )
                            .replace()
                            .range(fixLocation)
                            .text(fixText)
                            .with(fixReplacement)
                            .shortenNames()
                            .reformat(true)
                            .build()
                    } else {
                        null
                    }

                context.report(
                    Incident(
                        ISSUE,
                        callLocation,
                        "Default `ClearCredentialStateRequest()` does not delete the stored `RestoreCredential` " +
                            "key; also call `clearCredentialState(ClearCredentialStateRequest(" +
                            "TYPE_CLEAR_RESTORE_CREDENTIAL))` on sign-out to prevent post-logout auto-login",
                        fix,
                    )
                )
            }
        }
    }

    override fun afterCheckRootProject(context: Context) {
        if (context.isGlobalAnalysis()) {
            checkPartialResults(context, context.getPartialResults(ISSUE))
        }
    }

    private fun isClearRestoreCredentialType(context: JavaContext, expr: UExpression): Boolean {
        val constantValue = ConstantEvaluator.evaluate(context, expr) as? String
        if (constantValue == TYPE_CLEAR_RESTORE_CREDENTIAL) return true
        return expr.asSourceString().contains("TYPE_CLEAR_RESTORE_CREDENTIAL")
    }

    private fun isStandardClearRequestConstructor(
        context: JavaContext,
        expr: UExpression,
    ): Boolean {
        val call =
            when (val unwrapped = expr.skipParenthesizedExprDown()) {
                is UCallExpression -> unwrapped
                is UQualifiedReferenceExpression ->
                    unwrapped.selector.skipParenthesizedExprDown() as? UCallExpression
                else -> null
            } ?: return false

        val resolved = call.resolve() ?: return false
        if (!resolved.isConstructor) return false
        if (resolved.containingClass?.qualifiedName != CLASS_CLEAR_CREDENTIAL_STATE_REQUEST) {
            return false
        }

        val requestTypeArg = findArgument(context, call, resolved, "requestType", 0) ?: return true
        val constantValue = ConstantEvaluator.evaluate(context, requestTypeArg) as? String
        if (constantValue == TYPE_CLEAR_CREDENTIAL_STATE) return true
        return requestTypeArg.asSourceString().contains("TYPE_CLEAR_CREDENTIAL_STATE")
    }

    private data class FixSpec(
        val location: Location,
        val oldText: String,
        val replacement: String,
    )

    private fun computeAppendRestoreClearFixSpec(
        context: JavaContext,
        call: UCallExpression,
    ): FixSpec? {
        if (call.methodName != "clearCredentialState") return null
        if (!context.file.name.endsWith(".kt")) return null

        val outerStmt: UElement =
            (call.uastParent as? UQualifiedReferenceExpression)?.takeIf { it.selector == call }
                ?: call
        if (outerStmt.uastParent !is UBlockExpression) return null

        val fileContents = context.getContents() ?: return null
        val stmtRange = outerStmt.sourcePsi?.textRange ?: return null
        val selectorRange = call.sourcePsi?.textRange ?: return null
        if (
            stmtRange.startOffset < 0 ||
                selectorRange.startOffset < stmtRange.startOffset ||
                stmtRange.endOffset > fileContents.length
        ) {
            return null
        }

        val lineStart =
            if (stmtRange.startOffset > 0) {
                val prevNewline = fileContents.lastIndexOf('\n', stmtRange.startOffset - 1)
                if (prevNewline == -1) 0 else prevNewline + 1
            } else {
                0
            }
        val lineIndent =
            fileContents.substring(lineStart, stmtRange.startOffset).takeWhile {
                it == ' ' || it == '\t'
            }
        val stmtText = fileContents.substring(stmtRange.startOffset, stmtRange.endOffset)
        val receiverPrefix =
            fileContents.substring(stmtRange.startOffset, selectorRange.startOffset)
        val appendedCall =
            "${receiverPrefix}clearCredentialState(" +
                "$CLASS_CLEAR_CREDENTIAL_STATE_REQUEST(" +
                "$CLASS_CLEAR_CREDENTIAL_STATE_REQUEST.TYPE_CLEAR_RESTORE_CREDENTIAL))"

        val location =
            Location.create(context.file, fileContents, stmtRange.startOffset, stmtRange.endOffset)
        return FixSpec(
            location = location,
            oldText = stmtText,
            replacement = "$stmtText\n$lineIndent$appendedCall",
        )
    }

    companion object {
        private const val KEY_USES_RESTORE_CREDENTIALS = "USES_RESTORE_CREDENTIALS"
        private const val KEY_CLEARS_RESTORE_CREDENTIAL = "CLEARS_RESTORE_CREDENTIAL"
        private const val KEY_INCIDENT_COUNT = "INCIDENT_COUNT"
        private const val KEY_CALL_LOCATION_PREFIX = "CALL_LOCATION_"
        private const val KEY_FIX_LOCATION_PREFIX = "FIX_LOCATION_"
        private const val KEY_FIX_TEXT_PREFIX = "FIX_TEXT_"
        private const val KEY_FIX_REPLACEMENT_PREFIX = "FIX_REPLACEMENT_"

        private val IMPLEMENTATION =
            Implementation(
                RestoreCredentialLogoutClearDetector::class.java,
                EnumSet.of(Scope.ALL_JAVA_FILES),
                Scope.JAVA_FILE_SCOPE,
            )

        val ISSUE: Issue =
            Issue.create(
                id = "RestoreCredentialMissingLogoutClear",
                briefDescription = "Missing `TYPE_CLEAR_RESTORE_CREDENTIAL` cleanup on sign-out",
                explanation =
                    """
                Calling `CredentialManager.clearCredentialState(ClearCredentialStateRequest())` clears \
                cached passkey and password sign-in state (`TYPE_CLEAR_CREDENTIAL_STATE`), but leaves \
                any stored `RestoreCredential` active on the device and in backup. If the user transfers \
                or restores their device after signing out, the app will silently log them back in.

                Also invoke `clearCredentialState(ClearCredentialStateRequest(\
                ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL))` during sign-out to delete \
                the restore credential.
            """,
                category = Category.SECURITY,
                priority = 8,
                severity = Severity.WARNING,
                implementation = IMPLEMENTATION,
                moreInfo = RESTORE_CREDENTIALS_GUIDE_URL,
                androidSpecific = true,
            )
    }
}
