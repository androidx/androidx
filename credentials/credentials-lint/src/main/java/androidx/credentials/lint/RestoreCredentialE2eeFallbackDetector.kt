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

import androidx.credentials.lint.RestoreCredentialConstants.CLASS_CREATE_CREDENTIAL_EXCEPTION
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_CREDENTIAL_MANAGER
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_E2EE_UNAVAILABLE_EXCEPTION
import androidx.credentials.lint.RestoreCredentialConstants.CREATE_CREDENTIAL_METHODS
import androidx.credentials.lint.RestoreCredentialConstants.RESTORE_CREDENTIALS_GUIDE_URL
import androidx.credentials.lint.RestoreCredentialConstants.findArgument
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
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCatchClause
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UObjectLiteralExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UTryExpression
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Flags `CreateRestoreCredentialRequest` invocations with cloud backup enabled (`true` by default)
 * that do not catch `E2eeUnavailableException` and retry with `isCloudBackupEnabled = false` so
 * local USB/Wi-Fi Device-to-Device (D2D) transfers still succeed on devices without screen lock or
 * cloud E2EE (`RestoreCredentialMissingE2eeFallback`).
 */
class RestoreCredentialE2eeFallbackDetector : Detector(), SourceCodeScanner {

    override fun getApplicableConstructorTypes(): List<String> =
        listOf(CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST)

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod,
    ) {
        val cloudBackupArg = findArgument(context, node, constructor, "isCloudBackupEnabled", 1)
        if (cloudBackupArg != null) {
            val evaluated = ConstantEvaluator.evaluate(context, cloudBackupArg)
            // Explicitly false (`isCloudBackupEnabled = false`) or dynamic parameter variable is
            // exempt.
            if (evaluated != true) return
        }

        // Locate the `createCredential` / `createCredentialAsync` call(s) consuming this request.
        // Skip standalone request helpers/factories where no `createCredential` call exists in the
        // enclosing method, since `try`/`catch (E2eeUnavailableException)` belongs at the call
        // site.
        val consumingCalls = findConsumingCreateCredentialCalls(context, node)
        if (
            consumingCalls.isEmpty() ||
                consumingCalls.all { isProtectedByE2eeFallback(context, it) }
        ) {
            return
        }

        val singleInlineCall = consumingCalls.singleOrNull()?.takeIf { isDescendantOf(node, it) }
        val fix =
            if (singleInlineCall != null) {
                buildTryCatchFallbackQuickFix(context, singleInlineCall, node, constructor)
            } else {
                null
            }

        val reportTarget: UElement = singleInlineCall ?: node
        val reportLocation =
            if (singleInlineCall != null) {
                context.getCallLocation(
                    singleInlineCall,
                    includeReceiver = true,
                    includeArguments = true,
                )
            } else {
                context.getLocation(node)
            }

        context.report(
            ISSUE,
            reportTarget,
            reportLocation,
            "`CreateRestoreCredentialRequest` with cloud backup enabled throws " +
                "`E2eeUnavailableException` when the device lacks a screen lock or cloud E2EE; catch " +
                "`E2eeUnavailableException` and retry with `isCloudBackupEnabled = false` so local " +
                "Device-to-Device restore still works",
            fix,
        )
    }

    private fun findConsumingCreateCredentialCalls(
        context: JavaContext,
        ctorCall: UCallExpression,
    ): List<UCallExpression> {
        // Case 1: Inline argument inside createCredential / createCredentialAsync.
        var parent: UElement? = ctorCall.uastParent
        while (parent != null && parent !is UMethod) {
            if (parent is UCallExpression && isCreateCredentialCall(context, parent)) {
                return listOf(parent)
            }
            parent = parent.uastParent
        }

        // Case 2: Assigned to a local variable passed to createCredential / createCredentialAsync.
        val localVar = ctorCall.getParentOfType<UVariable>(true)
        val psiVar = localVar?.javaPsi as? PsiVariable ?: return emptyList()
        val enclosingMethod = ctorCall.getParentOfType<UMethod>(true) ?: return emptyList()

        val calls = mutableListOf<UCallExpression>()
        enclosingMethod.uastBody?.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    if (isCreateCredentialCall(context, node)) {
                        val usesVar =
                            node.valueArguments.any { arg ->
                                val unwrapped = arg.skipParenthesizedExprDown()
                                unwrapped is UReferenceExpression && unwrapped.resolve() == psiVar
                            }
                        if (usesVar) {
                            calls.add(node)
                        }
                    }
                    return false
                }
            }
        )
        return calls
    }

    private fun isCreateCredentialCall(context: JavaContext, call: UCallExpression): Boolean {
        if (call.methodName !in CREATE_CREDENTIAL_METHODS) return false
        val method = call.resolve() ?: return false
        val containingClass = method.containingClass ?: return false
        return context.evaluator.inheritsFrom(containingClass, CLASS_CREDENTIAL_MANAGER, false)
    }

    private fun isProtectedByE2eeFallback(
        context: JavaContext,
        createCall: UCallExpression,
    ): Boolean {
        if (isEnclosedInE2eeTryCatch(context, createCall)) return true
        if (createCall.methodName == "createCredentialAsync") {
            return asyncCallbackHandlesE2eeFallback(context, createCall)
        }
        return false
    }

    private fun isEnclosedInE2eeTryCatch(context: JavaContext, target: UElement): Boolean {
        var current: UElement? = target.uastParent
        while (current != null && current !is UMethod) {
            if (current is UTryExpression && isDescendantOf(target, current.tryClause)) {
                val hasValidCatch =
                    current.catchClauses.any { catchClause ->
                        catchHandlesE2eeException(context, catchClause) &&
                            containsLocalFallbackRetry(context, catchClause.body)
                    }
                if (hasValidCatch) return true
            }
            current = current.uastParent
        }
        return false
    }

    private fun catchHandlesE2eeException(
        context: JavaContext,
        catchClause: UCatchClause,
    ): Boolean {
        val caughtTypes = catchClause.typeReferences.map { it.type }
        for (psiType in caughtTypes) {
            val canonical = psiType.canonicalText
            if (canonical == CLASS_E2EE_UNAVAILABLE_EXCEPTION) return true
            val caughtClass = context.evaluator.getTypeClass(psiType) ?: continue
            if (
                context.evaluator.inheritsFrom(caughtClass, CLASS_E2EE_UNAVAILABLE_EXCEPTION, false)
            ) {
                return true
            }
            // Also allow catching CreateCredentialException / Exception / Throwable when the catch
            // block explicitly checks `e is E2eeUnavailableException` or `e instanceof
            // E2eeUnavailableException`.
            if (
                (canonical == CLASS_CREATE_CREDENTIAL_EXCEPTION ||
                    canonical == "java.lang.Exception" ||
                    canonical == "java.lang.Throwable") &&
                    elementReferencesE2eeException(catchClause.body)
            ) {
                return true
            }
        }
        return false
    }

    private fun asyncCallbackHandlesE2eeFallback(
        context: JavaContext,
        asyncCall: UCallExpression,
    ): Boolean {
        for (arg in asyncCall.valueArguments) {
            val unwrapped = arg.skipParenthesizedExprDown()
            if (unwrapped is UObjectLiteralExpression) {
                val onErrorMethod =
                    unwrapped.declaration.methods.firstOrNull { it.name == "onError" } ?: continue
                val body = onErrorMethod.uastBody ?: continue
                if (
                    elementReferencesE2eeException(body) &&
                        containsLocalFallbackRetry(context, body)
                ) {
                    return true
                }
            }
        }
        return false
    }

    private fun elementReferencesE2eeException(scope: UElement): Boolean {
        var found = false
        scope.accept(
            object : AbstractUastVisitor() {
                override fun visitBinaryExpressionWithType(
                    node: UBinaryExpressionWithType
                ): Boolean {
                    if (node.type.canonicalText == CLASS_E2EE_UNAVAILABLE_EXCEPTION) {
                        found = true
                    }
                    return false
                }

                override fun visitTypeReferenceExpression(node: UTypeReferenceExpression): Boolean {
                    if (node.type.canonicalText == CLASS_E2EE_UNAVAILABLE_EXCEPTION) {
                        found = true
                    }
                    return false
                }
            }
        )
        return found
    }

    private fun containsLocalFallbackRetry(context: JavaContext, scope: UElement): Boolean {
        var hasRetry = false
        scope.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    val resolved = node.resolve()
                    val owner = resolved?.containingClass
                    if (
                        resolved != null &&
                            resolved.isConstructor &&
                            owner != null &&
                            context.evaluator.inheritsFrom(
                                owner,
                                CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST,
                                false,
                            )
                    ) {
                        val cloudArg =
                            findArgument(context, node, resolved, "isCloudBackupEnabled", 1)
                        if (
                            cloudArg != null &&
                                ConstantEvaluator.evaluate(context, cloudArg) == false
                        ) {
                            hasRetry = true
                        }
                    }
                    return false
                }
            }
        )
        return hasRetry
    }

    private fun buildTryCatchFallbackQuickFix(
        context: JavaContext,
        createCall: UCallExpression,
        ctorCall: UCallExpression,
        constructor: PsiMethod,
    ): LintFix? {
        if (!context.file.name.endsWith(".kt")) return null
        if (createCall.methodName != "createCredential") return null

        val jsonArg: UExpression =
            findArgument(context, ctorCall, constructor, "requestJson", 0) ?: return null
        val outerCallExpr: UElement =
            (createCall.uastParent as? UQualifiedReferenceExpression)?.takeIf {
                it.selector == createCall
            } ?: createCall

        val fileContents = context.getContents() ?: return null
        val callRange = outerCallExpr.sourcePsi?.textRange ?: return null
        val ctorRange = ctorCall.sourcePsi?.textRange ?: return null
        val jsonRange = jsonArg.sourcePsi?.textRange ?: return null

        if (
            callRange.startOffset < 0 ||
                ctorRange.startOffset < callRange.startOffset ||
                ctorRange.endOffset > callRange.endOffset ||
                callRange.endOffset > fileContents.length
        ) {
            return null
        }

        val fullCallText = fileContents.substring(callRange.startOffset, callRange.endOffset)
        val ctorText = fileContents.substring(ctorRange.startOffset, ctorRange.endOffset)
        val parenIdx = ctorText.indexOf('(')
        if (parenIdx <= 0) return null
        val ctorCallee = ctorText.substring(0, parenIdx)
        val jsonExprText = fileContents.substring(jsonRange.startOffset, jsonRange.endOffset)

        val relStart = ctorRange.startOffset - callRange.startOffset
        val relEnd = ctorRange.endOffset - callRange.startOffset
        val fallbackCtorText = "$ctorCallee($jsonExprText, isCloudBackupEnabled = false)"
        val fallbackCallText =
            fullCallText.substring(0, relStart) + fallbackCtorText + fullCallText.substring(relEnd)

        val replacement =
            "try {\n" +
                "    $fullCallText\n" +
                "} catch (e: $CLASS_E2EE_UNAVAILABLE_EXCEPTION) {\n" +
                "    $fallbackCallText\n" +
                "}"

        val location =
            Location.create(context.file, fileContents, callRange.startOffset, callRange.endOffset)
        return LintFix.create()
            .name("Wrap in try/catch (E2eeUnavailableException) with local D2D fallback")
            .replace()
            .range(location)
            .text(fullCallText)
            .with(replacement)
            .shortenNames()
            .reformat(true)
            .build()
    }

    private fun isDescendantOf(child: UElement, ancestor: UElement): Boolean {
        var current: UElement? = child
        while (current != null) {
            if (current == ancestor) return true
            current = current.uastParent
        }
        return false
    }

    companion object {
        private val IMPLEMENTATION =
            Implementation(RestoreCredentialE2eeFallbackDetector::class.java, Scope.JAVA_FILE_SCOPE)

        val ISSUE: Issue =
            Issue.create(
                id = "RestoreCredentialMissingE2eeFallback",
                briefDescription = "Missing `E2eeUnavailableException` local D2D fallback",
                explanation =
                    """
                `CreateRestoreCredentialRequest` enables cloud backup by default \
                (`isCloudBackupEnabled = true`). On devices without a screen lock or end-to-end \
                encrypted cloud backup, `CredentialManager` throws `E2eeUnavailableException`.

                Catch `E2eeUnavailableException` and retry `CreateRestoreCredentialRequest` with \
                `isCloudBackupEnabled = false` so local USB/Wi-Fi Device-to-Device (D2D) session \
                restoration still succeeds.
            """,
                category = Category.CORRECTNESS,
                priority = 8,
                severity = Severity.WARNING,
                implementation = IMPLEMENTATION,
                moreInfo = RESTORE_CREDENTIALS_GUIDE_URL,
                androidSpecific = true,
            )
    }
}
