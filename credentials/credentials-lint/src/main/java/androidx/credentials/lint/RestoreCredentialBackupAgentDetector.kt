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
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_BACKUP_DATA_INPUT
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_CREDENTIAL_MANAGER
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_GET_RESTORE_CREDENTIAL_OPTION
import androidx.credentials.lint.RestoreCredentialConstants.GET_CREDENTIAL_METHODS
import androidx.credentials.lint.RestoreCredentialConstants.RESTORE_CREDENTIALS_GUIDE_URL
import androidx.credentials.lint.RestoreCredentialConstants.anyInSubtreeOrSameFileHelpers
import androidx.credentials.lint.RestoreCredentialConstants.unwrapNamedAndParens
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiVariable
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCatchClause
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UNamedExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.USuperExpression
import org.jetbrains.uast.UThisExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Enforces `BackupAgent` lifecycle rules when retrieving Restore Credentials on a target device:
 * - [ASYNC_ISSUE] (`RestoreCredentialBackupAgentAsync`): flags asynchronous `getCredential` /
 *   `getCredentialAsync` execution inside `BackupAgent.onRestoreFinished()` that returns before IPC
 *   completion, causing the OS to kill the background `BackupAgent` process mid-restore.
 * - [WRONG_CALLBACK_ISSUE] (`RestoreCredentialWrongBackupCallback`): flags Restore Credentials
 *   retrieval inside legacy `BackupAgent.onRestore(...)`, which only runs for Key-Value backups and
 *   is bypassed by Android Auto Backup.
 */
class RestoreCredentialBackupAgentDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> = GET_CREDENTIAL_METHODS.toList()

    override fun getApplicableConstructorTypes(): List<String> =
        listOf(CLASS_GET_RESTORE_CREDENTIAL_OPTION)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val containingClass = method.containingClass ?: return
        if (!context.evaluator.inheritsFrom(containingClass, CLASS_CREDENTIAL_MANAGER, false))
            return

        val enclosingClass = node.getParentOfType<UClass>(true) ?: return
        if (!context.evaluator.inheritsFrom(enclosingClass.javaPsi, CLASS_BACKUP_AGENT, false))
            return

        val enclosingMethod = node.getParentOfType<UMethod>(true) ?: return

        // Rule 3: Calling getCredential / getCredentialAsync inside legacy
        // BackupAgent.onRestore(...)
        val onRestoreMethod =
            resolveEnclosingOnRestoreMethod(context, enclosingClass, enclosingMethod)
        if (onRestoreMethod != null) {
            reportWrongBackupCallback(
                context,
                node,
                enclosingClass,
                enclosingMethod,
                onRestoreMethod,
            )
            return
        }

        // Rule 2: Calling getCredentialAsync or async launch/async inside
        // BackupAgent.onRestoreFinished()
        val onRestoreFinishedContext =
            resolveOnRestoreFinishedContext(enclosingClass, enclosingMethod) ?: return
        checkAsyncInOnRestoreFinished(
            context,
            node,
            method,
            enclosingMethod,
            onRestoreFinishedContext,
        )
    }

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod,
    ) {
        val enclosingClass = node.getParentOfType<UClass>(true) ?: return
        if (!context.evaluator.inheritsFrom(enclosingClass.javaPsi, CLASS_BACKUP_AGENT, false))
            return
        val enclosingMethod = node.getParentOfType<UMethod>(true) ?: return

        val onRestoreMethod =
            resolveEnclosingOnRestoreMethod(context, enclosingClass, enclosingMethod) ?: return
        // Avoid duplicate diagnostics when GetRestoreCredentialOption is already inside a
        // getCredential call in the same method (which visitMethodCall flags directly).
        if (methodContainsGetCredentialCall(context, enclosingMethod)) return

        reportWrongBackupCallback(context, node, enclosingClass, enclosingMethod, onRestoreMethod)
    }

    private data class OnRestoreFinishedContext(
        val onRestoreFinishedMethod: UMethod,
        val helperCallSiteInOnRestoreFinished: UCallExpression?,
    )

    private fun checkAsyncInOnRestoreFinished(
        context: JavaContext,
        call: UCallExpression,
        method: PsiMethod,
        enclosingMethod: UMethod,
        restoreFinishedContext: OnRestoreFinishedContext,
    ) {
        val onRestoreFinished = restoreFinishedContext.onRestoreFinishedMethod
        val reportLocation =
            context.getCallLocation(call, includeReceiver = false, includeArguments = true)

        if (method.name == "getCredentialAsync") {
            if (
                containsBlockingSyncWait(context, onRestoreFinished) ||
                    containsBlockingSyncWait(context, enclosingMethod)
            ) {
                return
            }
            context.report(
                ASYNC_ISSUE,
                call,
                reportLocation,
                "`CredentialManager.getCredentialAsync` runs asynchronously and allows " +
                    "`BackupAgent.onRestoreFinished()` to return before credential retrieval finishes, " +
                    "causing Android to terminate the backup process; block with `runBlocking { " +
                    "credentialManager.getCredential(...) }` or await completion before returning",
            )
            return
        }

        // Suspend `getCredential`: inspect coroutine builders enclosing the call (both inside the
        // immediate method and, if inside a helper method, around the helper call site).
        val helperSite = restoreFinishedContext.helperCallSiteInOnRestoreFinished
        val asyncCallAtHelperSite = helperSite?.let {
            findUnjoinedAsyncBuilder(context, it, onRestoreFinished, false)
        }
        val isHelperInsideRunBlocking =
            helperSite != null &&
                asyncCallAtHelperSite == null &&
                isEnclosedByRunBlocking(helperSite, onRestoreFinished)
        val asyncCallInMethod =
            findUnjoinedAsyncBuilder(context, call, enclosingMethod, isHelperInsideRunBlocking)
        val offendingAsyncBuilder = asyncCallInMethod ?: asyncCallAtHelperSite ?: return

        val builderName = offendingAsyncBuilder.methodName ?: "launch"
        val fix = buildRunBlockingQuickFix(context, offendingAsyncBuilder)
        context.report(
            ASYNC_ISSUE,
            call,
            reportLocation,
            "`CredentialManager.getCredential` inside `$builderName` returns from " +
                "`BackupAgent.onRestoreFinished()` before the asynchronous restore IPC completes, " +
                "causing Android to kill the process; use `runBlocking` so `onRestoreFinished()` blocks " +
                "until restoration finishes",
            fix,
        )
    }

    private fun isEnclosedByRunBlocking(leaf: UElement, stopAtMethod: UMethod): Boolean {
        var current: UElement? = leaf
        while (current != null && current != stopAtMethod) {
            val lambda = current as? ULambdaExpression
            if (lambda != null) {
                val parentCall = lambda.getParentOfType<UCallExpression>(true)
                if (
                    parentCall != null &&
                        parentCall.methodName == "runBlocking" &&
                        parentCall.valueArguments.any {
                            ((it as? UNamedExpression)?.expression ?: it) == lambda
                        }
                ) {
                    return true
                }
            }
            current = current.uastParent
        }
        return false
    }

    /**
     * Walks up from [leaf] to [stopAtMethod] and returns an enclosing `launch` or `async` builder
     * call if that builder escapes `onRestoreFinished()` without being joined by an enclosing
     * `runBlocking` scope or explicit `.join()` / `.await()`.
     */
    private fun findUnjoinedAsyncBuilder(
        context: JavaContext,
        leaf: UElement,
        stopAtMethod: UMethod,
        isCallerBlockedByRunBlocking: Boolean,
    ): UCallExpression? {
        val enclosingBuilders = mutableListOf<UCallExpression>()
        var current: UElement? = leaf
        while (current != null && current != stopAtMethod) {
            val lambda = current as? ULambdaExpression
            if (lambda != null) {
                val parentCall = lambda.getParentOfType<UCallExpression>(true)
                if (
                    parentCall != null &&
                        parentCall.valueArguments.any {
                            ((it as? UNamedExpression)?.expression ?: it) == lambda
                        }
                ) {
                    enclosingBuilders.add(parentCall)
                }
            }
            current = current.uastParent
        }

        // Builders are ordered innermost -> outermost.
        val outermostBlockingIndex =
            if (isCallerBlockedByRunBlocking) {
                enclosingBuilders.size
            } else {
                enclosingBuilders.indexOfLast { it.methodName == "runBlocking" }
            }

        for ((index, builderCall) in enclosingBuilders.withIndex()) {
            val name = builderCall.methodName ?: continue
            if (name !in ASYNC_COROUTINE_BUILDERS) continue
            if (isBuilderExplicitlyAwaitedOrJoined(context, builderCall, stopAtMethod)) continue

            if (outermostBlockingIndex == -1 || index > outermostBlockingIndex) {
                // Not enclosed by runBlocking at all.
                return builderCall
            }

            // Even inside runBlocking { ... }, calling `CoroutineScope(Dispatchers.IO).launch {
            // ... }`, `GlobalScope.launch { ... }`, or `this@ScopedBackupAgent.launch { ... }`
            // (where BackupAgent implements CoroutineScope) launches on an external scope that
            // runBlocking does NOT join!
            val receiver = builderCall.receiver?.skipParenthesizedExprDown()
            if (receiver != null) {
                if (receiver !is UThisExpression || receiver.resolve() is PsiClass) {
                    return builderCall
                }
            }
        }
        return null
    }

    private fun isBuilderExplicitlyAwaitedOrJoined(
        context: JavaContext,
        builderCall: UCallExpression,
        enclosingMethod: UMethod,
    ): Boolean {
        val outerQualified =
            (builderCall.uastParent as? UQualifiedReferenceExpression)?.takeIf {
                it.selector == builderCall
            } ?: builderCall
        val chainedParent = outerQualified.uastParent as? UQualifiedReferenceExpression
        if (chainedParent != null && chainedParent.receiver == outerQualified) {
            val selectorCall =
                chainedParent.selector.skipParenthesizedExprDown() as? UCallExpression
            if (selectorCall?.methodName in JOIN_OR_AWAIT_METHODS) {
                return true
            }
        }
        val parentCall = outerQualified.uastParent as? UCallExpression
        if (
            parentCall != null &&
                parentCall.receiver == outerQualified &&
                parentCall.methodName in JOIN_OR_AWAIT_METHODS
        ) {
            return true
        }

        val assignedVar =
            (outerQualified.uastParent as? UVariable)?.javaPsi as? PsiVariable ?: return false
        return anyInSubtreeOrSameFileHelpers(context, enclosingMethod.uastBody, maxDepth = 0) {
            it is UCallExpression &&
                it.methodName in JOIN_OR_AWAIT_METHODS &&
                (it.receiver?.skipParenthesizedExprDown() as? UReferenceExpression)?.resolve() ==
                    assignedVar
        }
    }

    private fun buildRunBlockingQuickFix(
        context: JavaContext,
        asyncCall: UCallExpression,
    ): LintFix? {
        if (asyncCall.methodName != "launch") return null
        if (asyncCall.valueArgumentCount !in 1..2) return null

        val lambdaArg =
            asyncCall.valueArguments.lastOrNull()?.unwrapNamedAndParens() as? ULambdaExpression
                ?: return null
        if (containsLabeledLaunchReturn(context, lambdaArg)) return null

        val outerExpr: UElement =
            (asyncCall.uastParent as? UQualifiedReferenceExpression)?.takeIf {
                it.selector == asyncCall
            } ?: asyncCall
        if (outerExpr.uastParent !is UBlockExpression && outerExpr.uastParent !is UMethod) {
            return null
        }

        val fileContents = context.getContents() ?: return null
        val startOffset = outerExpr.sourcePsi?.textRange?.startOffset ?: return null
        val lambdaStartOffset = lambdaArg.sourcePsi?.textRange?.startOffset ?: return null
        if (
            startOffset < 0 ||
                lambdaStartOffset <= startOffset ||
                lambdaStartOffset > fileContents.length
        ) {
            return null
        }

        var headerEndOffset = lambdaStartOffset
        while (headerEndOffset > startOffset && fileContents[headerEndOffset - 1].isWhitespace()) {
            headerEndOffset--
        }
        val headerText = fileContents.substring(startOffset, headerEndOffset)
        if (headerText.isEmpty() || headerText.contains("?.") || headerText.endsWith("(")) {
            return null
        }

        val replacement =
            if (asyncCall.valueArgumentCount == 1) {
                "kotlinx.coroutines.runBlocking"
            } else {
                if (!headerText.endsWith(")")) return null
                val firstArg = asyncCall.valueArguments[0].unwrapNamedAndParens()
                val firstArgClass =
                    firstArg.getExpressionType()?.let { context.evaluator.getTypeClass(it) }
                if (
                    firstArgClass == null ||
                        !context.evaluator.inheritsFrom(
                            firstArgClass,
                            "kotlin.coroutines.CoroutineContext",
                            false,
                        )
                ) {
                    return null
                }
                val argRange = firstArg.sourcePsi?.textRange ?: return null
                if (argRange.startOffset < startOffset || argRange.endOffset > headerEndOffset) {
                    return null
                }
                val argText = fileContents.substring(argRange.startOffset, argRange.endOffset)
                "kotlinx.coroutines.runBlocking($argText)"
            }

        val headerLocation =
            Location.create(context.file, fileContents, startOffset, headerEndOffset)
        return LintFix.create()
            .name("Replace '$headerText' with 'runBlocking'")
            .replace()
            .range(headerLocation)
            .text(headerText)
            .with(replacement)
            .shortenNames()
            .reformat(true)
            .build()
    }

    private fun containsLabeledLaunchReturn(
        context: JavaContext,
        lambdaArg: ULambdaExpression,
    ): Boolean =
        anyInSubtreeOrSameFileHelpers(context, lambdaArg.body, maxDepth = 0) {
            it is UReturnExpression &&
                (it.label == "launch" || (it.label != null && it.jumpTarget == lambdaArg))
        }

    private fun reportWrongBackupCallback(
        context: JavaContext,
        call: UCallExpression,
        enclosingClass: UClass,
        enclosingMethod: UMethod,
        onRestoreMethod: UMethod,
    ) {
        val fix =
            if (enclosingMethod == onRestoreMethod) {
                buildMoveToOnRestoreFinishedFix(context, enclosingClass, onRestoreMethod)
            } else {
                null
            }
        val location =
            context.getCallLocation(call, includeReceiver = false, includeArguments = true)
        context.report(
            WRONG_CALLBACK_ISSUE,
            call,
            location,
            "`BackupAgent.onRestore()` only runs for legacy Key-Value backups and is bypassed by " +
                "Android Auto Backup (`android:fullBackupOnly=\"true\"`); retrieve Restore Credentials " +
                "in `BackupAgent.onRestoreFinished()` instead",
            fix,
        )
    }

    /**
     * Builds a composite quick-fix converting `onRestore(...)` to `onRestoreFinished()` when the
     * enclosing `BackupAgent` does not already override `onRestoreFinished()` and `onRestore`'s
     * parameters are unused outside an optional `super.onRestore(...)` call.
     */
    private fun buildMoveToOnRestoreFinishedFix(
        context: JavaContext,
        enclosingClass: UClass,
        onRestoreMethod: UMethod,
    ): LintFix? {
        if (
            enclosingClass.methods.any {
                it.name == "onRestoreFinished" && it.uastParameters.isEmpty()
            }
        ) {
            return null
        }
        val paramPsiSet: Set<PsiParameter> =
            onRestoreMethod.uastParameters.mapNotNull { it.javaPsi as? PsiParameter }.toSet()
        val body = onRestoreMethod.uastBody ?: return null

        var superOnRestoreExpr: UElement? = null
        var usesParamsForKeyValueData = false
        var hasIoExceptionDependency = false

        body.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    if (node.methodName == "onRestore" && node.receiver is USuperExpression) {
                        superOnRestoreExpr =
                            (node.uastParent as? UQualifiedReferenceExpression)?.takeIf {
                                it.selector == node
                            } ?: node
                    } else {
                        val thrownTypes = node.resolve()?.throwsList?.referencedTypes.orEmpty()
                        if (thrownTypes.any { it.canonicalText == "java.io.IOException" }) {
                            hasIoExceptionDependency = true
                        }
                    }
                    return false
                }

                override fun visitCatchClause(node: UCatchClause): Boolean {
                    if (
                        node.typeReferences.any { it.type.canonicalText == "java.io.IOException" }
                    ) {
                        hasIoExceptionDependency = true
                    }
                    return false
                }

                override fun visitSimpleNameReferenceExpression(
                    node: USimpleNameReferenceExpression
                ): Boolean {
                    val resolved = node.resolve()
                    if (resolved in paramPsiSet) {
                        val enclosingCall = node.getParentOfType<UCallExpression>(true)
                        val isSuperOnRestoreArg =
                            enclosingCall != null &&
                                enclosingCall.methodName == "onRestore" &&
                                enclosingCall.receiver is USuperExpression
                        if (!isSuperOnRestoreArg) {
                            usesParamsForKeyValueData = true
                        }
                    }
                    return false
                }
            }
        )

        if (usesParamsForKeyValueData || hasIoExceptionDependency) return null

        val fileContents = context.getContents() ?: return null
        val methodPsi = onRestoreMethod.sourcePsi ?: return null
        val bodyPsi = body.sourcePsi ?: return null
        val methodStart = methodPsi.textRange.startOffset
        val bodyStart = bodyPsi.textRange.startOffset
        val bodyEnd = bodyPsi.textRange.endOffset
        if (
            methodStart < 0 ||
                bodyStart <= methodStart ||
                bodyEnd <= bodyStart ||
                bodyEnd > fileContents.length
        ) {
            return null
        }

        val superImplementsOnRestore =
            onRestoreMethod.javaPsi.findSuperMethods().any {
                !it.hasModifierProperty(PsiModifier.ABSTRACT)
            }

        val headerSlice = fileContents.substring(methodStart, bodyStart)
        val kotlinFunMatch = KOTLIN_ON_RESTORE_HEADER_REGEX.find(headerSlice)
        val javaVoidMatch = JAVA_ON_RESTORE_HEADER_REGEX.find(headerSlice)

        // When extending direct `BackupAgent` (where `onRestore` is abstract), keep an empty
        // `onRestore(...) {}` implementation and move the body into a new `onRestoreFinished()`
        // method so the class continues to satisfy `BackupAgent`'s abstract contract.
        if (!superImplementsOnRestore) {
            if (kotlinFunMatch == null && javaVoidMatch == null) return null
            val lineStart =
                if (methodStart > 0) {
                    val prevNewline = fileContents.lastIndexOf('\n', methodStart - 1)
                    if (prevNewline == -1) 0 else prevNewline + 1
                } else {
                    0
                }
            val methodIndent =
                fileContents.substring(lineStart, methodStart).takeWhile {
                    it == ' ' || it == '\t'
                }
            val originalBodyText = fileContents.substring(bodyStart, bodyEnd)
            val superPsi = superOnRestoreExpr?.sourcePsi
            val movedBodyText =
                if (
                    superPsi != null &&
                        superPsi.textRange.startOffset >= bodyStart &&
                        superPsi.textRange.endOffset <= bodyEnd
                ) {
                    val relStart = superPsi.textRange.startOffset - bodyStart
                    val relEnd = superPsi.textRange.endOffset - bodyStart
                    originalBodyText.substring(0, relStart) +
                        "super.onRestoreFinished()" +
                        originalBodyText.substring(relEnd)
                } else {
                    originalBodyText
                }
            val newMethodHeader =
                if (kotlinFunMatch != null) {
                    "override fun onRestoreFinished()"
                } else {
                    "@Override\n${methodIndent}public void onRestoreFinished()"
                }
            val replacement = "{}\n\n$methodIndent$newMethodHeader $movedBodyText"
            return LintFix.create()
                .name("Move restore credential retrieval to onRestoreFinished()")
                .replace()
                .range(Location.create(context.file, fileContents, bodyStart, bodyEnd))
                .text(originalBodyText)
                .with(replacement)
                .reformat(true)
                .build()
        }

        val lastParamEnd =
            onRestoreMethod.uastParameters.lastOrNull()?.sourcePsi?.textRange?.endOffset
                ?: return null
        val closeParenOffset = fileContents.indexOf(')', lastParamEnd)
        if (closeParenOffset < lastParamEnd || closeParenOffset >= bodyStart) return null

        val signatureFix: LintFix =
            if (kotlinFunMatch != null) {
                val kotlinFunIdx = kotlinFunMatch.range.first
                val replaceStart = methodStart + kotlinFunIdx
                val replaceEnd = closeParenOffset + 1
                if (replaceEnd <= replaceStart) return null
                val oldSig = fileContents.substring(replaceStart, replaceEnd)
                LintFix.create()
                    .replace()
                    .range(Location.create(context.file, fileContents, replaceStart, replaceEnd))
                    .text(oldSig)
                    .with("fun onRestoreFinished()")
                    .reformat(true)
                    .build()
            } else if (javaVoidMatch != null) {
                val javaVoidIdx = javaVoidMatch.range.first
                var replaceEnd = bodyStart
                while (
                    replaceEnd > methodStart + javaVoidIdx &&
                        fileContents[replaceEnd - 1].isWhitespace()
                ) {
                    replaceEnd--
                }
                val replaceStart = methodStart + javaVoidIdx
                val oldSig = fileContents.substring(replaceStart, replaceEnd)
                LintFix.create()
                    .replace()
                    .range(Location.create(context.file, fileContents, replaceStart, replaceEnd))
                    .text(oldSig)
                    .with("void onRestoreFinished()")
                    .reformat(true)
                    .build()
            } else {
                return null
            }

        val superPsi = superOnRestoreExpr?.sourcePsi
        if (superPsi == null) {
            return LintFix.create()
                .name("Move restore credential retrieval to onRestoreFinished()")
                .composite(signatureFix)
        }

        val superStart = superPsi.textRange.startOffset
        val superEnd = superPsi.textRange.endOffset
        val oldSuperText = fileContents.substring(superStart, superEnd)
        val superFix =
            LintFix.create()
                .replace()
                .range(Location.create(context.file, fileContents, superStart, superEnd))
                .text(oldSuperText)
                .with("super.onRestoreFinished()")
                .reformat(true)
                .build()

        return LintFix.create()
            .name("Move restore credential retrieval to onRestoreFinished()")
            .composite(signatureFix, superFix)
    }

    private fun resolveOnRestoreFinishedContext(
        enclosingClass: UClass,
        enclosingMethod: UMethod,
    ): OnRestoreFinishedContext? {
        if (enclosingMethod.isOnRestoreFinished()) {
            return OnRestoreFinishedContext(enclosingMethod, null)
        }
        val onRestoreFinished =
            enclosingClass.methods.firstOrNull { it.isOnRestoreFinished() } ?: return null
        val targetPsi = enclosingMethod.javaPsi
        var helperCallSite: UCallExpression? = null
        onRestoreFinished.uastBody?.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    if (helperCallSite == null && node.resolve() == targetPsi) {
                        helperCallSite = node
                    }
                    return false
                }
            }
        )
        return helperCallSite?.let { OnRestoreFinishedContext(onRestoreFinished, it) }
    }

    private fun resolveEnclosingOnRestoreMethod(
        context: JavaContext,
        enclosingClass: UClass,
        enclosingMethod: UMethod,
    ): UMethod? {
        if (enclosingMethod.isLegacyOnRestore(context)) return enclosingMethod
        val onRestore =
            enclosingClass.methods.firstOrNull { it.isLegacyOnRestore(context) } ?: return null
        val targetPsi = enclosingMethod.javaPsi
        val called =
            anyInSubtreeOrSameFileHelpers(context, onRestore.uastBody, maxDepth = 0) {
                it is UCallExpression && it.resolve() == targetPsi
            }
        return if (called) onRestore else null
    }

    private fun UMethod.isOnRestoreFinished(): Boolean =
        name == "onRestoreFinished" && uastParameters.isEmpty()

    private fun UMethod.isLegacyOnRestore(context: JavaContext): Boolean {
        if (name != "onRestore" || uastParameters.size != 3) return false
        val firstParamType = uastParameters[0].type
        val firstParamClass = context.evaluator.getTypeClass(firstParamType) ?: return false
        return context.evaluator.inheritsFrom(firstParamClass, CLASS_BACKUP_DATA_INPUT, false)
    }

    private fun methodContainsGetCredentialCall(context: JavaContext, method: UMethod): Boolean =
        anyInSubtreeOrSameFileHelpers(context, method.uastBody, maxDepth = 0) { node ->
            val cls =
                (node as? UCallExpression)
                    ?.takeIf { it.methodName in GET_CREDENTIAL_METHODS }
                    ?.resolve()
                    ?.containingClass
            cls != null && context.evaluator.inheritsFrom(cls, CLASS_CREDENTIAL_MANAGER, false)
        }

    private fun containsBlockingSyncWait(context: JavaContext, method: UMethod): Boolean =
        anyInSubtreeOrSameFileHelpers(context, method.uastBody, maxDepth = 0) { node ->
            val call = node as? UCallExpression ?: return@anyInSubtreeOrSameFileHelpers false
            val owner =
                call.resolve()?.containingClass ?: return@anyInSubtreeOrSameFileHelpers false
            when (call.methodName) {
                "await",
                "get" ->
                    context.evaluator.inheritsFrom(
                        owner,
                        "java.util.concurrent.CountDownLatch",
                        false,
                    ) || context.evaluator.inheritsFrom(owner, "java.util.concurrent.Future", false)
                "acquire" ->
                    context.evaluator.inheritsFrom(owner, "java.util.concurrent.Semaphore", false)
                "join" -> context.evaluator.inheritsFrom(owner, "java.lang.Thread", false)
                else -> false
            }
        }

    companion object {
        private val ASYNC_COROUTINE_BUILDERS = setOf("launch", "async")
        private val JOIN_OR_AWAIT_METHODS = setOf("join", "await")
        private val KOTLIN_ON_RESTORE_HEADER_REGEX = Regex("""\bfun\s+onRestore\b""")
        private val JAVA_ON_RESTORE_HEADER_REGEX = Regex("""\bvoid\s+onRestore\b""")

        private val IMPLEMENTATION =
            Implementation(RestoreCredentialBackupAgentDetector::class.java, Scope.JAVA_FILE_SCOPE)

        val ASYNC_ISSUE: Issue =
            Issue.create(
                id = "RestoreCredentialBackupAgentAsync",
                briefDescription = "Asynchronous Restore Credential retrieval in `BackupAgent`",
                explanation =
                    """
                `BackupAgent.onRestoreFinished()` executes in a short-lived background backup process \
                that Android terminates as soon as `onRestoreFinished()` returns. Launching \
                `CredentialManager.getCredential()` inside an asynchronous `CoroutineScope.launch` / \
                `async` block or calling `getCredentialAsync()` without blocking causes the process to \
                be killed mid-IPC before the restored credential is retrieved.

                Wrap the `getCredential()` call in `runBlocking { ... }` inside `onRestoreFinished()` \
                so the method blocks until credential retrieval and session persistence complete.
            """,
                category = Category.CORRECTNESS,
                priority = 9,
                severity = Severity.ERROR,
                implementation = IMPLEMENTATION,
                moreInfo = RESTORE_CREDENTIALS_GUIDE_URL,
                androidSpecific = true,
            )

        val WRONG_CALLBACK_ISSUE: Issue =
            Issue.create(
                id = "RestoreCredentialWrongBackupCallback",
                briefDescription =
                    "Restore Credential retrieval in legacy `BackupAgent.onRestore()`",
                explanation =
                    """
                `BackupAgent.onRestore(BackupDataInput, int, ParcelFileDescriptor)` is only invoked for \
                legacy Key-Value backups and is completely bypassed when Android Auto Backup \
                (`android:fullBackupOnly="true"`) restores app data on a new device.

                Retrieve `RestoreCredential` inside `BackupAgent.onRestoreFinished()` instead, which \
                Android invokes after Auto Backup finishes restoring app files.
            """,
                category = Category.CORRECTNESS,
                priority = 8,
                severity = Severity.ERROR,
                implementation = IMPLEMENTATION,
                moreInfo = RESTORE_CREDENTIALS_GUIDE_URL,
                androidSpecific = true,
            )
    }
}
