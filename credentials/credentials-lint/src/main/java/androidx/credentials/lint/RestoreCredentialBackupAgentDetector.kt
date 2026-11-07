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
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.USuperExpression
import org.jetbrains.uast.UThisExpression
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
            if (containsBlockingSyncWait(onRestoreFinished)) return
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
        val asyncCallInMethod = findUnjoinedAsyncBuilder(call, enclosingMethod)
        val asyncCallAtHelperSite =
            restoreFinishedContext.helperCallSiteInOnRestoreFinished?.let {
                findUnjoinedAsyncBuilder(it, onRestoreFinished)
            }
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

    /**
     * Walks up from [leaf] to [stopAtMethod] and returns an enclosing `launch` or `async` builder
     * call if that builder escapes `onRestoreFinished()` without being joined by an enclosing
     * `runBlocking` scope.
     */
    private fun findUnjoinedAsyncBuilder(leaf: UElement, stopAtMethod: UMethod): UCallExpression? {
        val enclosingBuilders = mutableListOf<UCallExpression>()
        var current: UElement? = leaf
        while (current != null && current != stopAtMethod) {
            val lambda = current as? ULambdaExpression
            if (lambda != null) {
                val parentCall = lambda.getParentOfType<UCallExpression>(true)
                if (parentCall != null && lambda in parentCall.valueArguments) {
                    enclosingBuilders.add(parentCall)
                }
            }
            current = current.uastParent
        }

        // Builders are ordered innermost -> outermost.
        val outermostRunBlockingIndex = enclosingBuilders.indexOfLast {
            it.methodName == "runBlocking"
        }

        for ((index, builderCall) in enclosingBuilders.withIndex()) {
            val name = builderCall.methodName ?: continue
            if (name !in ASYNC_COROUTINE_BUILDERS) continue

            if (outermostRunBlockingIndex == -1 || index > outermostRunBlockingIndex) {
                // Not enclosed by runBlocking at all.
                return builderCall
            }

            // Even inside runBlocking { ... }, calling `CoroutineScope(Dispatchers.IO).launch {
            // ... }` or `GlobalScope.launch { ... }` launches on an external scope that
            // runBlocking does NOT join!
            val receiver = builderCall.receiver?.skipParenthesizedExprDown()
            if (receiver != null && receiver !is UThisExpression) {
                return builderCall
            }
        }
        return null
    }

    private fun buildRunBlockingQuickFix(
        context: JavaContext,
        asyncCall: UCallExpression,
    ): LintFix? {
        if (asyncCall.methodName != "launch") return null
        if (asyncCall.valueArgumentCount != 1) return null
        val lambdaArg = asyncCall.valueArguments.singleOrNull() as? ULambdaExpression ?: return null

        val outerExpr: UElement =
            (asyncCall.uastParent as? UQualifiedReferenceExpression)?.takeIf {
                it.selector == asyncCall
            } ?: asyncCall

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
        if (headerText.isEmpty()) return null

        val headerLocation =
            Location.create(context.file, fileContents, startOffset, headerEndOffset)
        return LintFix.create()
            .name("Replace '$headerText' with 'runBlocking'")
            .replace()
            .range(headerLocation)
            .text(headerText)
            .with("kotlinx.coroutines.runBlocking")
            .shortenNames()
            .reformat(true)
            .build()
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

        body.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    if (node.methodName == "onRestore" && node.receiver is USuperExpression) {
                        superOnRestoreExpr =
                            (node.uastParent as? UQualifiedReferenceExpression)?.takeIf {
                                it.selector == node
                            } ?: node
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

        if (usesParamsForKeyValueData) return null

        val fileContents = context.getContents() ?: return null
        val methodPsi = onRestoreMethod.sourcePsi ?: return null
        val bodyPsi = body.sourcePsi ?: return null
        val methodStart = methodPsi.textRange.startOffset
        val bodyStart = bodyPsi.textRange.startOffset
        if (methodStart < 0 || bodyStart <= methodStart || bodyStart > fileContents.length)
            return null

        val headerSlice = fileContents.substring(methodStart, bodyStart)
        val kotlinFunMatch = KOTLIN_ON_RESTORE_HEADER_REGEX.find(headerSlice)
        val javaVoidMatch = JAVA_ON_RESTORE_HEADER_REGEX.find(headerSlice)

        val signatureFix: LintFix =
            if (kotlinFunMatch != null) {
                val kotlinFunIdx = kotlinFunMatch.range.first
                val closeParenIdx = headerSlice.lastIndexOf(')')
                if (closeParenIdx <= kotlinFunIdx) return null
                val replaceStart = methodStart + kotlinFunIdx
                val replaceEnd = methodStart + closeParenIdx + 1
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
        var calledFromOnRestore = false
        onRestore.uastBody?.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    if (node.resolve() == targetPsi) {
                        calledFromOnRestore = true
                    }
                    return false
                }
            }
        )
        return if (calledFromOnRestore) onRestore else null
    }

    private fun UMethod.isOnRestoreFinished(): Boolean =
        name == "onRestoreFinished" && uastParameters.isEmpty()

    private fun UMethod.isLegacyOnRestore(context: JavaContext): Boolean {
        if (name != "onRestore" || uastParameters.size != 3) return false
        val firstParamType = uastParameters[0].type
        val firstParamClass = context.evaluator.getTypeClass(firstParamType) ?: return false
        return context.evaluator.inheritsFrom(firstParamClass, CLASS_BACKUP_DATA_INPUT, false)
    }

    private fun methodContainsGetCredentialCall(context: JavaContext, method: UMethod): Boolean {
        var found = false
        method.uastBody?.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    if (node.methodName in GET_CREDENTIAL_METHODS) {
                        val containingClass = node.resolve()?.containingClass
                        if (
                            containingClass != null &&
                                context.evaluator.inheritsFrom(
                                    containingClass,
                                    CLASS_CREDENTIAL_MANAGER,
                                    false,
                                )
                        ) {
                            found = true
                        }
                    }
                    return false
                }
            }
        )
        return found
    }

    private fun containsBlockingSyncWait(method: UMethod): Boolean {
        var hasSyncWait = false
        method.uastBody?.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    val methodName = node.methodName
                    if (methodName == "await" || methodName == "get") {
                        val ownerName = node.resolve()?.containingClass?.qualifiedName
                        if (
                            ownerName == "java.util.concurrent.CountDownLatch" ||
                                ownerName == "java.util.concurrent.Future" ||
                                ownerName == "java.util.concurrent.CompletableFuture"
                        ) {
                            hasSyncWait = true
                        }
                    }
                    return false
                }
            }
        )
        return hasSyncWait
    }

    companion object {
        private val ASYNC_COROUTINE_BUILDERS = setOf("launch", "async")
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
