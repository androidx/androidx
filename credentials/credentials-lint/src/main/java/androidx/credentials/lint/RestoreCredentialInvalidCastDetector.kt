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

import androidx.credentials.lint.RestoreCredentialConstants.CLASS_CREDENTIAL
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_CREDENTIAL_MANAGER
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_GET_CREDENTIAL_RESPONSE
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_GET_PUBLIC_KEY_CREDENTIAL_OPTION
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_GET_RESTORE_CREDENTIAL_OPTION
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_PUBLIC_KEY_CREDENTIAL
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_RESTORE_CREDENTIAL
import androidx.credentials.lint.RestoreCredentialConstants.RESTORE_CREDENTIALS_GUIDE_URL
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiField
import com.intellij.psi.PsiVariable
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UDeclarationsExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UObjectLiteralExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.USwitchClauseExpression
import org.jetbrains.uast.USwitchExpression
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Flags casts and type checks targeting `PublicKeyCredential` when handling a
 * `GetRestoreCredentialOption` response, which returns `RestoreCredential` at runtime.
 */
class RestoreCredentialInvalidCastDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(
            UBinaryExpressionWithType::class.java,
            USwitchClauseExpression::class.java,
            UBinaryExpression::class.java,
        )

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitBinaryExpressionWithType(node: UBinaryExpressionWithType) {
                val typeRef = node.typeReference ?: return
                val targetClass = context.evaluator.getTypeClass(typeRef.type) ?: return
                if (
                    !context.evaluator.inheritsFrom(targetClass, CLASS_PUBLIC_KEY_CREDENTIAL, false)
                ) {
                    return
                }
                val effectiveOperand =
                    if (isCredentialExpression(context, node.operand)) {
                        node.operand
                    } else {
                        node
                            .getParentOfType<USwitchClauseExpression>(true)
                            ?.getParentOfType<USwitchExpression>(true)
                            ?.let(::extractSwitchSubject)
                            ?.takeIf { isCredentialExpression(context, it) } ?: return
                    }
                if (!isFromRestoreOnlyFlow(context, effectiveOperand, node)) return

                reportInvalidTypeTarget(context, node, typeRef)
            }

            override fun visitSwitchClauseExpression(node: USwitchClauseExpression) {
                val switchExpr = node.getParentOfType<USwitchExpression>(true) ?: return
                val subject = extractSwitchSubject(switchExpr) ?: return
                if (!isCredentialExpression(context, subject)) return

                for (caseVal in node.caseValues) {
                    // UBinaryExpressionWithType inside `when` is already handled by
                    // visitBinaryExpressionWithType.
                    if (caseVal is UBinaryExpressionWithType) continue
                    if (caseVal is UTypeReferenceExpression) {
                        val targetClass = context.evaluator.getTypeClass(caseVal.type) ?: continue
                        if (
                            context.evaluator.inheritsFrom(
                                targetClass,
                                CLASS_PUBLIC_KEY_CREDENTIAL,
                                false,
                            ) && isFromRestoreOnlyFlow(context, subject, node)
                        ) {
                            reportInvalidTypeTarget(context, node, caseVal)
                        }
                    }
                }
            }

            override fun visitBinaryExpression(node: UBinaryExpression) {
                val opText = node.operator.text
                if (opText != "==" && opText != "!=") return

                val publicKeyTypeConst =
                    findPublicKeyTypeConstantRef(context, node.leftOperand, node.rightOperand)
                        ?: return
                val otherSide =
                    if (publicKeyTypeConst === node.leftOperand) node.rightOperand
                    else node.leftOperand
                val credentialReceiver = unwrapCredentialTypeReceiver(context, otherSide) ?: return
                if (!isFromRestoreOnlyFlow(context, credentialReceiver, node)) return

                val location = context.getLocation(publicKeyTypeConst)
                val fix =
                    fix()
                        .name("Replace with 'RestoreCredential.TYPE_RESTORE_CREDENTIAL'")
                        .replace()
                        .range(location)
                        .with("$CLASS_RESTORE_CREDENTIAL.TYPE_RESTORE_CREDENTIAL")
                        .shortenNames()
                        .reformat(true)
                        .build()

                context.report(
                    ISSUE,
                    node,
                    location,
                    "`GetRestoreCredentialOption` returns `RestoreCredential` (`TYPE_RESTORE_CREDENTIAL`), " +
                        "not `PublicKeyCredential` (`TYPE_PUBLIC_KEY_CREDENTIAL`).",
                    fix,
                )
            }
        }

    private fun reportInvalidTypeTarget(
        context: JavaContext,
        scopeNode: UElement,
        typeRef: UTypeReferenceExpression,
    ) {
        val location = context.getLocation(typeRef)
        val fix =
            fix()
                .name("Replace 'PublicKeyCredential' with 'RestoreCredential'")
                .replace()
                .range(location)
                .with(CLASS_RESTORE_CREDENTIAL)
                .shortenNames()
                .reformat(true)
                .build()

        context.report(
            ISSUE,
            scopeNode,
            location,
            "`GetRestoreCredentialOption` returns `RestoreCredential`, not `PublicKeyCredential` " +
                "(causes `ClassCastException` or failed type check at runtime). Use `RestoreCredential` instead.",
            fix,
        )
    }

    private fun extractSwitchSubject(switchExpr: USwitchExpression): UExpression? {
        val expr = switchExpr.expression ?: return null
        if (expr is UDeclarationsExpression) {
            val variable = expr.declarations.firstOrNull() as? UVariable
            return variable?.uastInitializer ?: expr
        }
        return expr
    }

    private fun isCredentialExpression(context: JavaContext, expr: UExpression): Boolean {
        val typeClass =
            RestoreCredentialConstants.resolveExpressionClass(context, expr) ?: return false
        return context.evaluator.inheritsFrom(typeClass, CLASS_CREDENTIAL, false)
    }

    private fun findPublicKeyTypeConstantRef(
        context: JavaContext,
        left: UExpression,
        right: UExpression,
    ): UExpression? {
        if (isPublicKeyTypeConstant(context, left)) return left
        if (isPublicKeyTypeConstant(context, right)) return right
        return null
    }

    private fun isPublicKeyTypeConstant(context: JavaContext, expr: UExpression): Boolean {
        val unwrapped = expr.skipParenthesizedExprDown()
        val ref = unwrapped as? UReferenceExpression ?: return false
        val resolved = ref.resolve() as? PsiField ?: return false
        return resolved.name == "TYPE_PUBLIC_KEY_CREDENTIAL" &&
            context.evaluator.isMemberInClass(resolved, CLASS_PUBLIC_KEY_CREDENTIAL)
    }

    private fun unwrapCredentialTypeReceiver(
        context: JavaContext,
        expr: UExpression,
    ): UExpression? {
        val unwrapped = RestoreCredentialConstants.unwrapVariableInitializer(expr)
        if (unwrapped is UQualifiedReferenceExpression) {
            val selectorText = unwrapped.selector.asSourceString()
            if (selectorText == "type" || selectorText.startsWith("getType(")) {
                if (isCredentialExpression(context, unwrapped.receiver)) {
                    return unwrapped.receiver
                }
            }
        }
        if (unwrapped is UCallExpression && unwrapped.methodName == "getType") {
            val receiver = unwrapped.receiver
            if (receiver != null && isCredentialExpression(context, receiver)) {
                return receiver
            }
        }
        return null
    }

    private enum class Provenance {
        RESTORE_ONLY,
        PASSKEY_PRESENT,
        UNKNOWN,
    }

    private fun isFromRestoreOnlyFlow(
        context: JavaContext,
        credentialExpr: UExpression,
        site: UElement,
    ): Boolean {
        // 1. Direct variable provenance from `response = credMan.getCredential(ctx, req)` ->
        // `response.credential`.
        when (traceDirectCredentialProvenance(context, credentialExpr)) {
            Provenance.RESTORE_ONLY -> return true
            Provenance.PASSKEY_PRESENT -> return false
            Provenance.UNKNOWN -> {}
        }

        // 2. If inside an anonymous callback (e.g., CredentialManagerCallback.onResult) passed to
        // getCredentialAsync(ctx, req, ...), inspect that call's request argument.
        val objectLiteral = site.getParentOfType<UObjectLiteralExpression>(true)
        if (objectLiteral != null) {
            val enclosingCall = objectLiteral.getParentOfType<UCallExpression>(true)
            val resolved = enclosingCall?.resolve()
            if (
                enclosingCall != null &&
                    resolved != null &&
                    resolved.name in RestoreCredentialConstants.GET_CREDENTIAL_METHODS &&
                    context.evaluator.isMemberInClass(resolved, CLASS_CREDENTIAL_MANAGER)
            ) {
                val requestArg =
                    RestoreCredentialConstants.findArgument(
                        context,
                        enclosingCall,
                        resolved,
                        "request",
                        1,
                    )
                if (requestArg != null) {
                    return classifyRequestOptions(context, requestArg) == Provenance.RESTORE_ONLY
                }
            }
        }

        // 3. Enclosing method scope: flag if the method uses GetRestoreCredentialOption without
        // GetPublicKeyCredentialOption, unless the method already explicitly handles
        // RestoreCredential (e.g., dispatching over a try/catch fallback response).
        // Do not fall back to enclosingClass, as ViewModels and shared helpers often accept
        // GetCredentialResponse parameters whose interactive requests were built in another class.
        val enclosingMethod = site.getParentOfType<UMethod>(true)
        if (enclosingMethod != null) {
            if (scopeExplicitlyHandlesRestoreCredential(context, enclosingMethod)) {
                return false
            }
            val methodProvenance = classifyScopeReferences(context, enclosingMethod)
            if (methodProvenance != Provenance.UNKNOWN) {
                return methodProvenance == Provenance.RESTORE_ONLY
            }
        }

        return false
    }

    private fun scopeExplicitlyHandlesRestoreCredential(
        context: JavaContext,
        scope: UElement,
    ): Boolean {
        var handlesRestore = false
        scope.accept(
            object : AbstractUastVisitor() {
                override fun visitBinaryExpressionWithType(
                    node: UBinaryExpressionWithType
                ): Boolean {
                    val targetClass =
                        node.typeReference?.type?.let { context.evaluator.getTypeClass(it) }
                    if (
                        targetClass != null &&
                            context.evaluator.inheritsFrom(
                                targetClass,
                                CLASS_RESTORE_CREDENTIAL,
                                false,
                            )
                    ) {
                        handlesRestore = true
                    }
                    return false
                }

                override fun visitSwitchClauseExpression(node: USwitchClauseExpression): Boolean {
                    for (caseVal in node.caseValues) {
                        if (caseVal is UTypeReferenceExpression) {
                            val targetClass = context.evaluator.getTypeClass(caseVal.type)
                            if (
                                targetClass != null &&
                                    context.evaluator.inheritsFrom(
                                        targetClass,
                                        CLASS_RESTORE_CREDENTIAL,
                                        false,
                                    )
                            ) {
                                handlesRestore = true
                            }
                        }
                    }
                    return false
                }

                override fun visitSimpleNameReferenceExpression(
                    node: USimpleNameReferenceExpression
                ): Boolean {
                    val field = node.resolve() as? PsiField ?: return false
                    if (
                        field.name == "TYPE_RESTORE_CREDENTIAL" &&
                            context.evaluator.isMemberInClass(field, CLASS_RESTORE_CREDENTIAL)
                    ) {
                        handlesRestore = true
                    }
                    return false
                }
            }
        )
        return handlesRestore
    }

    private fun traceDirectCredentialProvenance(
        context: JavaContext,
        credentialExpr: UExpression,
    ): Provenance {
        val unwrappedCred = RestoreCredentialConstants.unwrapVariableInitializer(credentialExpr)
        val responseExpr: UExpression =
            when (unwrappedCred) {
                is UQualifiedReferenceExpression -> {
                    val receiverType =
                        unwrappedCred.receiver.getExpressionType()?.let {
                            context.evaluator.getTypeClass(it)
                        }
                    if (
                        receiverType != null &&
                            context.evaluator.inheritsFrom(
                                receiverType,
                                CLASS_GET_CREDENTIAL_RESPONSE,
                                false,
                            )
                    ) {
                        unwrappedCred.receiver
                    } else {
                        return Provenance.UNKNOWN
                    }
                }
                is UCallExpression -> {
                    val receiver = unwrappedCred.receiver ?: return Provenance.UNKNOWN
                    val receiverType =
                        receiver.getExpressionType()?.let { context.evaluator.getTypeClass(it) }
                    if (
                        receiverType != null &&
                            context.evaluator.inheritsFrom(
                                receiverType,
                                CLASS_GET_CREDENTIAL_RESPONSE,
                                false,
                            )
                    ) {
                        receiver
                    } else {
                        return Provenance.UNKNOWN
                    }
                }
                else -> return Provenance.UNKNOWN
            }

        val unwrappedResp = RestoreCredentialConstants.unwrapVariableInitializer(responseExpr)
        val getCredCall =
            when (unwrappedResp) {
                is UCallExpression -> unwrappedResp
                is UQualifiedReferenceExpression ->
                    unwrappedResp.selector.skipParenthesizedExprDown() as? UCallExpression
                else -> null
            } ?: return Provenance.UNKNOWN

        val resolvedMethod = getCredCall.resolve() ?: return Provenance.UNKNOWN
        if (
            resolvedMethod.name !in RestoreCredentialConstants.GET_CREDENTIAL_METHODS ||
                !context.evaluator.isMemberInClass(resolvedMethod, CLASS_CREDENTIAL_MANAGER)
        ) {
            return Provenance.UNKNOWN
        }

        val requestArg =
            RestoreCredentialConstants.findArgument(
                context,
                getCredCall,
                resolvedMethod,
                "request",
                1,
            ) ?: return Provenance.UNKNOWN
        return classifyRequestOptions(context, requestArg)
    }

    private fun classifyRequestOptions(context: JavaContext, requestExpr: UExpression): Provenance {
        val options =
            RestoreCredentialConstants.extractCredentialOptionExpressions(context, requestExpr)
        if (options.isEmpty()) return Provenance.UNKNOWN
        var hasRestore = false
        var hasPasskey = false
        for (opt in options) {
            val cls = RestoreCredentialConstants.resolveExpressionClass(context, opt) ?: continue
            if (context.evaluator.inheritsFrom(cls, CLASS_GET_RESTORE_CREDENTIAL_OPTION, false)) {
                hasRestore = true
            }
            if (
                context.evaluator.inheritsFrom(cls, CLASS_GET_PUBLIC_KEY_CREDENTIAL_OPTION, false)
            ) {
                hasPasskey = true
            }
        }
        return when {
            hasPasskey -> Provenance.PASSKEY_PRESENT
            hasRestore -> Provenance.RESTORE_ONLY
            else -> Provenance.UNKNOWN
        }
    }

    private fun classifyScopeReferences(context: JavaContext, scope: UElement): Provenance {
        var hasRestoreOption = false
        var hasPasskeyOption = false
        scope.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    val typeClass =
                        node.getExpressionType()?.let { context.evaluator.getTypeClass(it) }
                    if (typeClass != null) {
                        if (
                            context.evaluator.inheritsFrom(
                                typeClass,
                                CLASS_GET_RESTORE_CREDENTIAL_OPTION,
                                false,
                            )
                        ) {
                            hasRestoreOption = true
                        }
                        if (
                            context.evaluator.inheritsFrom(
                                typeClass,
                                CLASS_GET_PUBLIC_KEY_CREDENTIAL_OPTION,
                                false,
                            )
                        ) {
                            hasPasskeyOption = true
                        }
                    }
                    return false
                }

                override fun visitSimpleNameReferenceExpression(
                    node: USimpleNameReferenceExpression
                ): Boolean {
                    val resolved = node.resolve() as? PsiVariable ?: return false
                    val varType = resolved.type
                    val typeClass = context.evaluator.getTypeClass(varType)
                    if (typeClass != null) {
                        if (
                            context.evaluator.inheritsFrom(
                                typeClass,
                                CLASS_GET_RESTORE_CREDENTIAL_OPTION,
                                false,
                            )
                        ) {
                            hasRestoreOption = true
                        }
                        if (
                            context.evaluator.inheritsFrom(
                                typeClass,
                                CLASS_GET_PUBLIC_KEY_CREDENTIAL_OPTION,
                                false,
                            )
                        ) {
                            hasPasskeyOption = true
                        }
                    }
                    val uVar = resolved.toUElementOfType<UVariable>()
                    val init = uVar?.uastInitializer
                    if (
                        init != null &&
                            classifyRequestOptions(context, init) == Provenance.RESTORE_ONLY
                    ) {
                        hasRestoreOption = true
                    } else if (
                        init != null &&
                            classifyRequestOptions(context, init) == Provenance.PASSKEY_PRESENT
                    ) {
                        hasPasskeyOption = true
                    }
                    return false
                }
            }
        )

        return when {
            hasPasskeyOption -> Provenance.PASSKEY_PRESENT
            hasRestoreOption -> Provenance.RESTORE_ONLY
            else -> Provenance.UNKNOWN
        }
    }

    companion object {
        val ISSUE: Issue =
            Issue.create(
                id = "RestoreCredentialInvalidCast",
                briefDescription =
                    "Invalid `PublicKeyCredential` cast for `GetRestoreCredentialOption`",
                explanation =
                    """
                `CredentialManager.getCredential()` with `GetRestoreCredentialOption` returns \
                `androidx.credentials.RestoreCredential` (`TYPE_RESTORE_CREDENTIAL`), even though its \
                `authenticationResponseJson` payload uses WebAuthn JSON formatting.

                Casting or checking `GetCredentialResponse.credential` as `PublicKeyCredential` throws \
                `ClassCastException` at runtime (or causes `as?` / `is` checks to silently evaluate to \
                `null` / `false`, dropping sign-in restoration). Cast to `RestoreCredential` instead.
            """,
                category = Category.CORRECTNESS,
                priority = 9,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        RestoreCredentialInvalidCastDetector::class.java,
                        Scope.JAVA_FILE_SCOPE,
                    ),
                moreInfo = RESTORE_CREDENTIALS_GUIDE_URL,
                androidSpecific = true,
            )
    }
}
