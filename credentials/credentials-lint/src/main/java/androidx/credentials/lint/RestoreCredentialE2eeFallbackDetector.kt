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
import androidx.credentials.lint.RestoreCredentialConstants.anyInSubtreeOrSameFileHelpers
import androidx.credentials.lint.RestoreCredentialConstants.asCallExpression
import androidx.credentials.lint.RestoreCredentialConstants.findArgument
import androidx.credentials.lint.RestoreCredentialConstants.isDescendantOf
import androidx.credentials.lint.RestoreCredentialConstants.unwrapNamedAndParens
import androidx.credentials.lint.RestoreCredentialConstants.unwrapVariableInitializer
import androidx.credentials.lint.RestoreCredentialConstants.unwrapVariableOrHelperReturn
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
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiVariable
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCatchClause
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UObjectLiteralExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.USwitchClauseExpressionWithBody
import org.jetbrains.uast.USwitchExpression
import org.jetbrains.uast.UTryExpression
import org.jetbrains.uast.UUnaryExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.UastBinaryExpressionWithTypeKind
import org.jetbrains.uast.UastPrefixOperator
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElementOfType
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

    override fun getApplicableMethodNames(): List<String> = CREATE_CREDENTIAL_METHODS.toList()

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod,
    ) {
        if (!isCloudBackupEnabledCtor(context, node, constructor)) return

        // Locate the `createCredential` / `createCredentialAsync` call(s) consuming this request.
        // Skip standalone request helpers/factories where no `createCredential` call exists in the
        // enclosing method, since `try`/`catch (E2eeUnavailableException)` belongs at the call
        // site (which is checked by `visitMethodCall`).
        val consumingCalls = findConsumingCreateCredentialCalls(context, node)
        if (
            consumingCalls.isEmpty() ||
                consumingCalls.all { isProtectedByE2eeFallback(context, it) } ||
                isParameterProtectedAtCallSites(context, node, constructor)
        ) {
            return
        }

        val singleInlineCall = consumingCalls.singleOrNull()?.takeIf { node.isDescendantOf(it) }
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

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!isCreateCredentialCall(context, node)) return
        val requestArg = findArgument(context, node, method, "request", 1) ?: return
        val unwrappedRequestArg = unwrapVariableInitializer(requestArg)
        val requestHelperCall = unwrappedRequestArg.asCallExpression()
        // Direct constructors in the same method are already handled by visitConstructor.
        if (requestHelperCall?.resolve()?.isConstructor == true) {
            return
        }
        val ctorCall =
            unwrapVariableOrHelperReturn(context, requestArg).asCallExpression() ?: return
        val resolvedCtor = ctorCall.resolve() ?: return
        val owner = resolvedCtor.containingClass ?: return
        if (
            !resolvedCtor.isConstructor ||
                !context.evaluator.inheritsFrom(
                    owner,
                    CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST,
                    false,
                ) ||
                !isCloudBackupEnabledCtor(context, ctorCall, resolvedCtor, requestHelperCall) ||
                isProtectedByE2eeFallback(context, node)
        ) {
            return
        }

        context.report(
            ISSUE,
            node,
            context.getCallLocation(node, includeReceiver = true, includeArguments = true),
            "`CreateRestoreCredentialRequest` with cloud backup enabled throws " +
                "`E2eeUnavailableException` when the device lacks a screen lock or cloud E2EE; catch " +
                "`E2eeUnavailableException` and retry with `isCloudBackupEnabled = false` so local " +
                "Device-to-Device restore still works",
        )
    }

    private fun isCloudBackupEnabledCtor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod,
        helperCallSite: UCallExpression? = null,
    ): Boolean {
        val cloudBackupArg = findArgument(context, node, constructor, "isCloudBackupEnabled", 1)
        if (cloudBackupArg != null) {
            val cloudParam =
                (cloudBackupArg.unwrapNamedAndParens() as? UReferenceExpression)?.resolve()
                    as? PsiParameter
            if (cloudParam != null && helperCallSite != null) {
                val helperMethod = helperCallSite.resolve()
                val ctorEnclosingMethod = node.getParentOfType<UMethod>(true)?.javaPsi
                if (
                    helperMethod != null &&
                        (ctorEnclosingMethod == null || helperMethod == ctorEnclosingMethod)
                ) {
                    val paramIndex =
                        helperMethod.parameterList.parameters.indexOfFirst {
                            it == cloudParam || it.name == cloudParam.name
                        }
                    if (paramIndex >= 0) {
                        val passedArg =
                            findArgument(
                                context,
                                helperCallSite,
                                helperMethod,
                                cloudParam.name,
                                paramIndex,
                            )
                        if (passedArg != null) {
                            return ConstantEvaluator.evaluate(context, passedArg) == true
                        }
                    }
                }
            }
            val evaluated = ConstantEvaluator.evaluate(context, cloudBackupArg)
            if (evaluated != true) return false
        }
        return true
    }

    private fun isParameterProtectedAtCallSites(
        context: JavaContext,
        ctorCall: UCallExpression,
        constructor: PsiMethod,
    ): Boolean {
        val cloudBackupArg =
            findArgument(context, ctorCall, constructor, "isCloudBackupEnabled", 1) ?: return false
        if (
            (cloudBackupArg.unwrapNamedAndParens() as? UReferenceExpression)?.resolve()
                !is PsiParameter
        ) {
            return false
        }
        val enclosingMethod = ctorCall.getParentOfType<UMethod>(true) ?: return false
        val targetPsi = enclosingMethod.javaPsi
        val uastFile = ctorCall.getParentOfType<UFile>(true) ?: context.uastFile ?: return false

        val nonLocalCallSites = mutableListOf<UCallExpression>()
        uastFile.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    if (node.resolve() == targetPsi && !isLocalFallbackCallOrCtor(context, node)) {
                        nonLocalCallSites.add(node)
                    }
                    return false
                }
            }
        )
        return nonLocalCallSites.isNotEmpty() &&
            nonLocalCallSites.all { isProtectedByE2eeFallback(context, it) }
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
                                (arg.unwrapNamedAndParens() as? UReferenceExpression)?.resolve() ==
                                    psiVar
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
        if (isEnclosedInRunCatchingWithFallback(context, createCall)) return true
        if (createCall.methodName == "createCredentialAsync") {
            return asyncCallbackHandlesE2eeFallback(context, createCall)
        }
        return false
    }

    private fun isEnclosedInE2eeTryCatch(context: JavaContext, target: UElement): Boolean {
        var current: UElement? = target.uastParent
        while (current != null && current !is UMethod) {
            if (current is UTryExpression && target.isDescendantOf(current.tryClause)) {
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

    private fun isEnclosedInRunCatchingWithFallback(
        context: JavaContext,
        target: UElement,
    ): Boolean {
        val enclosingMethod = target.getParentOfType<UMethod>(true) ?: return false
        val methodBody = enclosingMethod.uastBody ?: return false
        var current: UElement? = target.uastParent
        while (current != null && current != enclosingMethod) {
            if (current is UCallExpression && current.methodName in RUN_CATCHING_METHODS) {
                val handlerScopes = collectRunCatchingFailureHandlers(context, current, methodBody)
                if (handlerScopes.any { containsLocalFallbackRetry(context, it) }) {
                    return true
                }
            }
            current = current.uastParent
        }
        return false
    }

    private fun collectRunCatchingFailureHandlers(
        context: JavaContext,
        runCatchingCall: UCallExpression,
        methodBody: UElement,
    ): List<UElement> {
        val handlerScopes = mutableListOf<UElement>()
        var expr: UElement =
            (runCatchingCall.uastParent as? UQualifiedReferenceExpression)?.takeIf {
                it.selector == runCatchingCall
            } ?: runCatchingCall

        while (true) {
            val parent = expr.uastParent ?: break
            when {
                parent is UParenthesizedExpression -> expr = parent
                parent is UQualifiedReferenceExpression && parent.receiver == expr -> {
                    val selectorCall = parent.selector.asCallExpression()
                    if (selectorCall != null) {
                        addHandlerArguments(context, selectorCall, handlerScopes)
                    }
                    expr = parent
                }
                parent is UCallExpression && parent.receiver == expr -> {
                    addHandlerArguments(context, parent, handlerScopes)
                    expr =
                        (parent.uastParent as? UQualifiedReferenceExpression)?.takeIf {
                            it.selector == parent
                        } ?: parent
                }
                parent is UBinaryExpression &&
                    parent.operator.text == "?:" &&
                    expr.isDescendantOf(parent.leftOperand) -> {
                    handlerScopes.add(parent.rightOperand)
                    expr = parent
                }
                else -> break
            }
        }

        val resultVar = (expr.uastParent as? UVariable)?.javaPsi as? PsiVariable
        if (resultVar != null) {
            methodBody.accept(
                object : AbstractUastVisitor() {
                    override fun visitCallExpression(node: UCallExpression): Boolean {
                        if (unwrapRootReceiverRef(node.receiver)?.resolve() == resultVar) {
                            addHandlerArguments(context, node, handlerScopes)
                        }
                        return false
                    }

                    override fun visitIfExpression(node: UIfExpression): Boolean {
                        when (classifyResultFailureBranch(context, node.condition, resultVar)) {
                            ResultFailureBranch.THEN ->
                                node.thenExpression?.let { handlerScopes.add(it) }
                            ResultFailureBranch.ELSE ->
                                node.elseExpression?.let { handlerScopes.add(it) }
                            ResultFailureBranch.NONE -> {}
                        }
                        return false
                    }

                    override fun visitSwitchExpression(node: USwitchExpression): Boolean {
                        collectSwitchFailureHandlers(context, node, resultVar, handlerScopes)
                        return false
                    }

                    override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
                        if (
                            node.operator.text == "?:" &&
                                referencesVariable(node.leftOperand, resultVar)
                        ) {
                            handlerScopes.add(node.rightOperand)
                        }
                        return false
                    }
                }
            )
        }
        return handlerScopes
    }

    private fun addHandlerArguments(
        context: JavaContext,
        call: UCallExpression,
        out: MutableList<UElement>,
    ) {
        when (call.methodName) {
            in RUN_CATCHING_FAILURE_HANDLER_METHODS -> out.addAll(call.valueArguments)
            "fold" -> {
                val resolved = call.resolve()
                val failureArg =
                    if (resolved != null) {
                        findArgument(context, call, resolved, "onFailure", 1)
                    } else {
                        call.valueArguments.getOrNull(1)
                    }
                if (failureArg != null) {
                    out.add(failureArg)
                }
            }
        }
    }

    private fun unwrapRootReceiverRef(expr: UExpression?): UReferenceExpression? {
        var current = expr?.skipParenthesizedExprDown()
        while (current != null) {
            current =
                when (current) {
                    is UQualifiedReferenceExpression -> current.receiver.skipParenthesizedExprDown()
                    is UCallExpression -> current.receiver?.skipParenthesizedExprDown()
                    is UReferenceExpression -> return current
                    else -> return null
                }
        }
        return null
    }

    private enum class ResultFailureBranch {
        THEN,
        ELSE,
        NONE;

        fun invert(): ResultFailureBranch =
            when (this) {
                THEN -> ELSE
                ELSE -> THEN
                NONE -> NONE
            }
    }

    private fun collectSwitchFailureHandlers(
        context: JavaContext,
        switchExpr: USwitchExpression,
        resultVar: PsiVariable,
        out: MutableList<UElement>,
    ) {
        val clauses =
            switchExpr.body.expressions.filterIsInstance<USwitchClauseExpressionWithBody>()
        if (clauses.isEmpty()) return

        val subject = switchExpr.expression
        val subjectMember = subject?.let { matchResultPropertyOrCall(it, resultVar) }
        var sawSuccessGuardClause = false
        var sawExplicitFailureClause = false

        for (clause in clauses) {
            val caseValues = clause.caseValues
            if (caseValues.isEmpty()) {
                if (sawSuccessGuardClause && !sawExplicitFailureClause) {
                    out.add(clause.body)
                }
                continue
            }

            if (subject == null) {
                var isFailureCase = false
                var isSuccessCase = false
                for (caseExpr in caseValues) {
                    when (classifyResultFailureBranch(context, caseExpr, resultVar)) {
                        ResultFailureBranch.THEN -> isFailureCase = true
                        ResultFailureBranch.ELSE -> isSuccessCase = true
                        ResultFailureBranch.NONE -> {}
                    }
                }
                if (isFailureCase) {
                    sawExplicitFailureClause = true
                    out.add(clause.body)
                } else if (isSuccessCase) {
                    sawSuccessGuardClause = true
                }
            } else if (subjectMember != null) {
                var isFailureCase = false
                var isSuccessCase = false
                for (caseExpr in caseValues) {
                    val unwrappedCase = caseExpr.skipParenthesizedExprDown()
                    when (subjectMember) {
                        "isSuccess",
                        "isFailure" -> {
                            val boolVal =
                                ConstantEvaluator.evaluate(context, unwrappedCase) as? Boolean
                            if (boolVal != null) {
                                val failureWhen = subjectMember == "isFailure"
                                if (boolVal == failureWhen) isFailureCase = true
                                else isSuccessCase = true
                            }
                        }
                        "exceptionOrNull" -> {
                            if (unwrappedCase is ULiteralExpression && unwrappedCase.isNull) {
                                isSuccessCase = true
                            } else if (unwrappedCase is UBinaryExpressionWithType) {
                                if (
                                    unwrappedCase.operationKind ==
                                        UastBinaryExpressionWithTypeKind.InstanceCheck.INSTANCE ||
                                        unwrappedCase.operationKind.name == "is"
                                ) {
                                    isFailureCase = true
                                }
                            }
                        }
                        "getOrNull" -> {
                            if (unwrappedCase is ULiteralExpression && unwrappedCase.isNull) {
                                isFailureCase = true
                            }
                        }
                    }
                }
                if (isFailureCase) {
                    sawExplicitFailureClause = true
                    out.add(clause.body)
                } else if (isSuccessCase) {
                    sawSuccessGuardClause = true
                }
            }
        }
    }

    private fun classifyResultFailureBranch(
        context: JavaContext,
        condition: UExpression,
        resultVar: PsiVariable,
    ): ResultFailureBranch {
        val unwrapped = condition.skipParenthesizedExprDown()
        if (
            unwrapped is UUnaryExpression &&
                (unwrapped.operator == UastPrefixOperator.LOGICAL_NOT ||
                    unwrapped.operator.text == "!")
        ) {
            return classifyResultFailureBranch(context, unwrapped.operand, resultVar).invert()
        }
        when (matchResultPropertyOrCall(unwrapped, resultVar)) {
            "isFailure" -> return ResultFailureBranch.THEN
            "isSuccess" -> return ResultFailureBranch.ELSE
        }
        if (unwrapped is UBinaryExpressionWithType) {
            if (matchResultPropertyOrCall(unwrapped.operand, resultVar) == "exceptionOrNull") {
                return when {
                    unwrapped.operationKind ==
                        UastBinaryExpressionWithTypeKind.InstanceCheck.INSTANCE ||
                        unwrapped.operationKind.name == "is" -> ResultFailureBranch.THEN
                    unwrapped.operationKind.name == "!is" -> ResultFailureBranch.ELSE
                    else -> ResultFailureBranch.NONE
                }
            }
        }
        if (unwrapped is UBinaryExpression) {
            val opText = unwrapped.operator.text
            if (opText == "&&" || opText == "||") {
                val left = classifyResultFailureBranch(context, unwrapped.leftOperand, resultVar)
                val right = classifyResultFailureBranch(context, unwrapped.rightOperand, resultVar)
                return when {
                    left != ResultFailureBranch.NONE && right == ResultFailureBranch.NONE -> left
                    right != ResultFailureBranch.NONE && left == ResultFailureBranch.NONE -> right
                    left == right -> left
                    else -> ResultFailureBranch.NONE
                }
            }
            if (opText == "==" || opText == "!=" || opText == "===" || opText == "!==") {
                val isEquals = opText == "==" || opText == "==="
                classifyResultComparison(
                        context,
                        unwrapped.leftOperand,
                        unwrapped.rightOperand,
                        isEquals,
                        resultVar,
                    )
                    ?.let {
                        return it
                    }
                classifyResultComparison(
                        context,
                        unwrapped.rightOperand,
                        unwrapped.leftOperand,
                        isEquals,
                        resultVar,
                    )
                    ?.let {
                        return it
                    }
            }
        }
        return ResultFailureBranch.NONE
    }

    private fun classifyResultComparison(
        context: JavaContext,
        targetExpr: UExpression,
        otherExpr: UExpression,
        isEquals: Boolean,
        resultVar: PsiVariable,
    ): ResultFailureBranch? {
        val member = matchResultPropertyOrCall(targetExpr, resultVar) ?: return null
        val otherUnwrapped = otherExpr.skipParenthesizedExprDown()
        val isNullLiteral = otherUnwrapped is ULiteralExpression && otherUnwrapped.isNull
        return when (member) {
            "isFailure",
            "isSuccess" -> {
                val boolVal =
                    ConstantEvaluator.evaluate(context, otherUnwrapped) as? Boolean ?: return null
                val base =
                    if (member == "isFailure") ResultFailureBranch.THEN
                    else ResultFailureBranch.ELSE
                val samePolarity = (isEquals && boolVal) || (!isEquals && !boolVal)
                if (samePolarity) base else base.invert()
            }
            "exceptionOrNull" -> {
                if (!isNullLiteral) return null
                if (isEquals) ResultFailureBranch.ELSE else ResultFailureBranch.THEN
            }
            "getOrNull" -> {
                if (!isNullLiteral) return null
                if (isEquals) ResultFailureBranch.THEN else ResultFailureBranch.ELSE
            }
            else -> null
        }
    }

    private fun matchResultPropertyOrCall(expr: UExpression, resultVar: PsiVariable): String? {
        val unwrapped = unwrapVariableInitializer(expr)
        val receiver: UExpression
        val rawName: String
        when (unwrapped) {
            is UQualifiedReferenceExpression -> {
                receiver = unwrapped.receiver
                val selector = unwrapped.selector.skipParenthesizedExprDown()
                rawName =
                    when (selector) {
                        is USimpleNameReferenceExpression -> selector.identifier
                        is UCallExpression -> selector.methodName ?: return null
                        else -> return null
                    }
            }
            is UCallExpression -> {
                receiver = unwrapped.receiver ?: return null
                rawName = unwrapped.methodName ?: return null
            }
            else -> return null
        }
        if (
            (receiver.skipParenthesizedExprDown() as? UReferenceExpression)?.resolve() != resultVar
        ) {
            return null
        }
        return when (rawName) {
            "isSuccess",
            "getIsSuccess" -> "isSuccess"
            "isFailure",
            "getIsFailure" -> "isFailure"
            "exceptionOrNull",
            "getOrNull" -> rawName
            else -> null
        }
    }

    private fun referencesVariable(scope: UElement, targetVar: PsiVariable): Boolean {
        var found = false
        scope.accept(
            object : AbstractUastVisitor() {
                override fun visitElement(node: UElement): Boolean {
                    if (found) return true
                    if (node is UReferenceExpression && node.resolve() == targetVar) {
                        found = true
                        return true
                    }
                    return false
                }
            }
        )
        return found
    }

    private fun catchHandlesE2eeException(
        context: JavaContext,
        catchClause: UCatchClause,
    ): Boolean {
        val caughtTypes = catchClause.typeReferences.map { it.type }
        for (psiType in caughtTypes) {
            val canonical = psiType.canonicalText
            if (
                canonical == CLASS_E2EE_UNAVAILABLE_EXCEPTION ||
                    canonical == CLASS_CREATE_CREDENTIAL_EXCEPTION ||
                    canonical == "java.lang.Exception" ||
                    canonical == "kotlin.Exception" ||
                    canonical == "java.lang.Throwable" ||
                    canonical == "kotlin.Throwable"
            ) {
                return true
            }
            val caughtClass = context.evaluator.getTypeClass(psiType) ?: continue
            if (
                context.evaluator.inheritsFrom(
                    caughtClass,
                    CLASS_E2EE_UNAVAILABLE_EXCEPTION,
                    false,
                ) ||
                    context.evaluator.inheritsFrom(
                        caughtClass,
                        CLASS_CREATE_CREDENTIAL_EXCEPTION,
                        false,
                    )
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
            val unwrapped = unwrapVariableOrHelperReturn(context, arg)
            val callbackUClass: UClass? =
                if (unwrapped is UObjectLiteralExpression) {
                    unwrapped.declaration
                } else {
                    val psiClass =
                        RestoreCredentialConstants.resolveExpressionClass(context, unwrapped)
                            ?: RestoreCredentialConstants.resolveExpressionClass(context, arg)
                    psiClass?.toUElementOfType<UClass>()
                }
            val onErrorMethod =
                callbackUClass?.methods?.firstOrNull { it.name == "onError" } ?: continue
            val body = onErrorMethod.uastBody ?: continue
            if (containsLocalFallbackRetry(context, body)) {
                return true
            }
        }
        return false
    }

    private fun containsLocalFallbackRetry(context: JavaContext, scope: UElement): Boolean =
        anyInSubtreeOrSameFileHelpers(context, scope) {
            it is UCallExpression && isLocalFallbackCallOrCtor(context, it)
        }

    private fun isLocalFallbackCallOrCtor(context: JavaContext, node: UCallExpression): Boolean {
        if (isLocalOnlyRestoreRequestCtor(context, node)) return true

        val resolvedMethod = node.resolve() ?: return false
        if (resolvedMethod.isConstructor) return false
        val helperMethod = resolvedMethod.toUElementOfType<UMethod>() ?: return false
        if (helperMethod.sourcePsi?.containingFile != context.psiFile) return false

        val falseParams =
            resolvedMethod.parameterList.parameters
                .withIndex()
                .filter { (index, param) ->
                    val arg = findArgument(context, node, resolvedMethod, param.name, index)
                    val unwrapped = arg?.unwrapNamedAndParens() ?: return@filter false
                    (unwrapped as? UReferenceExpression)?.resolve() !is PsiParameter &&
                        ConstantEvaluator.evaluate(context, unwrapped) == false
                }
                .map { it.value }
                .toSet()
        if (falseParams.isEmpty()) return false

        return passesParamToCloudBackupDisabled(
            context,
            helperMethod,
            falseParams,
            visited = setOf(resolvedMethod),
        )
    }

    private fun passesParamToCloudBackupDisabled(
        context: JavaContext,
        helperMethod: UMethod,
        falseParams: Set<PsiParameter>,
        visited: Set<PsiMethod>,
    ): Boolean {
        val body = helperMethod.uastBody ?: return false
        var matched = false
        body.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    if (matched) return true
                    val resolved = node.resolve() ?: return false
                    val owner = resolved.containingClass
                    if (
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
                        val resolvedRef =
                            (cloudArg?.unwrapNamedAndParens() as? UReferenceExpression)?.resolve()
                                as? PsiParameter
                        if (resolvedRef != null && matchesAnyParam(resolvedRef, falseParams)) {
                            matched = true
                            return true
                        }
                        return false
                    }
                    if (!resolved.isConstructor && resolved !in visited && visited.size < 4) {
                        val nextMethod = resolved.toUElementOfType<UMethod>()
                        if (
                            nextMethod != null &&
                                nextMethod.sourcePsi?.containingFile == context.psiFile
                        ) {
                            val forwardedFalseParams =
                                resolved.parameterList.parameters
                                    .withIndex()
                                    .filter { (index, param) ->
                                        val arg =
                                            findArgument(context, node, resolved, param.name, index)
                                        val ref =
                                            (arg?.unwrapNamedAndParens() as? UReferenceExpression)
                                                ?.resolve() as? PsiParameter
                                        ref != null && matchesAnyParam(ref, falseParams)
                                    }
                                    .map { it.value }
                                    .toSet()
                            if (
                                forwardedFalseParams.isNotEmpty() &&
                                    passesParamToCloudBackupDisabled(
                                        context,
                                        nextMethod,
                                        forwardedFalseParams,
                                        visited + resolved,
                                    )
                            ) {
                                matched = true
                                return true
                            }
                        }
                    }
                    return false
                }
            }
        )
        return matched
    }

    private fun matchesAnyParam(candidate: PsiParameter, params: Set<PsiParameter>): Boolean =
        params.any {
            it == candidate ||
                (it.name == candidate.name && it.declarationScope == candidate.declarationScope)
        }

    private fun isLocalOnlyRestoreRequestCtor(
        context: JavaContext,
        node: UCallExpression,
    ): Boolean {
        val resolved = node.resolve() ?: return false
        val owner = resolved.containingClass ?: return false
        if (
            !resolved.isConstructor ||
                !context.evaluator.inheritsFrom(
                    owner,
                    CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST,
                    false,
                )
        ) {
            return false
        }
        val cloudArg =
            findArgument(context, node, resolved, "isCloudBackupEnabled", 1) ?: return false
        if ((cloudArg.unwrapNamedAndParens() as? UReferenceExpression)?.resolve() is PsiParameter) {
            return false
        }
        return ConstantEvaluator.evaluate(context, cloudArg) == false
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

    companion object {
        private val RUN_CATCHING_METHODS = setOf("runCatching", "mapCatching")
        private val RUN_CATCHING_FAILURE_HANDLER_METHODS =
            setOf("onFailure", "recover", "recoverCatching", "getOrElse")

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
