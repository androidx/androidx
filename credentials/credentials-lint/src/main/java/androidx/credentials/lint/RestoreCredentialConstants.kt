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

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UNamedExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor

internal object RestoreCredentialConstants {
    const val CLASS_CREDENTIAL_MANAGER = "androidx.credentials.CredentialManager"
    const val CLASS_CREDENTIAL = "androidx.credentials.Credential"
    const val CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST =
        "androidx.credentials.CreateRestoreCredentialRequest"
    const val CLASS_GET_RESTORE_CREDENTIAL_OPTION =
        "androidx.credentials.GetRestoreCredentialOption"
    const val CLASS_GET_PUBLIC_KEY_CREDENTIAL_OPTION =
        "androidx.credentials.GetPublicKeyCredentialOption"
    const val CLASS_GET_CREDENTIAL_REQUEST = "androidx.credentials.GetCredentialRequest"
    const val CLASS_GET_CREDENTIAL_REQUEST_BUILDER =
        "androidx.credentials.GetCredentialRequest.Builder"
    const val CLASS_GET_CREDENTIAL_RESPONSE = "androidx.credentials.GetCredentialResponse"
    const val CLASS_RESTORE_CREDENTIAL = "androidx.credentials.RestoreCredential"
    const val CLASS_PUBLIC_KEY_CREDENTIAL = "androidx.credentials.PublicKeyCredential"
    const val CLASS_CLEAR_CREDENTIAL_STATE_REQUEST =
        "androidx.credentials.ClearCredentialStateRequest"
    const val CLASS_E2EE_UNAVAILABLE_EXCEPTION =
        "androidx.credentials.exceptions.restorecredential.E2eeUnavailableException"
    const val CLASS_CREATE_CREDENTIAL_EXCEPTION =
        "androidx.credentials.exceptions.CreateCredentialException"
    const val TYPE_CLEAR_CREDENTIAL_STATE = "androidx.credentials.TYPE_CLEAR_CREDENTIAL_STATE"
    const val TYPE_CLEAR_RESTORE_CREDENTIAL = "androidx.credentials.TYPE_CLEAR_RESTORE_CREDENTIAL"
    const val CLASS_MUTABLE_CONTEXT_WRAPPER = "android.content.MutableContextWrapper"
    const val CLASS_BACKUP_AGENT = "android.app.backup.BackupAgent"
    const val CLASS_BACKUP_DATA_INPUT = "android.app.backup.BackupDataInput"

    const val ANDROID_URI = "http://schemas.android.com/apk/res/android"
    const val TAG_MANIFEST = "manifest"
    const val TAG_APPLICATION = "application"
    const val ATTR_ALLOW_BACKUP = "allowBackup"
    const val ATTR_BACKUP_AGENT = "backupAgent"
    const val ATTR_FULL_BACKUP_ONLY = "fullBackupOnly"

    const val RESTORE_CREDENTIALS_GUIDE_URL =
        "https://developer.android.com/identity/sign-in/restore-credentials"

    val GET_CREDENTIAL_METHODS = setOf("getCredential", "getCredentialAsync")
    val CREATE_CREDENTIAL_METHODS = setOf("createCredential", "createCredentialAsync")
    val CLEAR_CREDENTIAL_METHODS = setOf("clearCredentialState", "clearCredentialStateAsync")

    private val COLLECTION_FACTORY_METHODS =
        setOf(
            "listOf",
            "mutableListOf",
            "arrayListOf",
            "listOfNotNull",
            "setOf",
            "mutableSetOf",
            "singletonList",
            "asList",
            "of",
        )

    /**
     * Resolves the argument passed to [paramName] (falling back to [paramIndex] when compiled
     * bytecode omits parameter names).
     */
    fun findArgument(
        context: JavaContext,
        call: UCallExpression,
        method: PsiMethod,
        paramName: String,
        paramIndex: Int,
    ): UExpression? {
        val mapping = context.evaluator.computeArgumentMapping(call, method)
        if (mapping.isNotEmpty()) {
            for ((expr, param) in mapping) {
                if (param.name == paramName) {
                    return (expr as? UNamedExpression)?.expression ?: expr
                }
            }
            val hasNamedMatchInSignature =
                method.parameterList.parameters.any { it.name == paramName }
            if (!hasNamedMatchInSignature) {
                for ((expr, param) in mapping) {
                    if (method.parameterList.getParameterIndex(param) == paramIndex) {
                        return (expr as? UNamedExpression)?.expression ?: expr
                    }
                }
            }
            return null
        }
        for (arg in call.valueArguments) {
            if (arg is UNamedExpression && arg.name == paramName) {
                return arg.expression
            }
        }
        val positional = call.valueArguments.getOrNull(paramIndex) ?: return null
        if (positional is UNamedExpression && positional.name != paramName) {
            return null
        }
        return (positional as? UNamedExpression)?.expression ?: positional
    }

    /**
     * Resolves the most specific [PsiClass] for [expression], following local variable
     * initializers.
     */
    fun resolveExpressionClass(
        context: JavaContext,
        expression: UExpression,
        maxDepth: Int = 4,
    ): PsiClass? {
        val unwrapped = expression.skipParenthesizedExprDown()
        var typeClass = unwrapped.getExpressionType()?.let { context.evaluator.getTypeClass(it) }
        if (maxDepth <= 0) return typeClass

        if (unwrapped is UReferenceExpression) {
            val resolved = unwrapped.resolve()
            if (resolved is PsiVariable) {
                val initializer = resolved.toUElementOfType<UVariable>()?.uastInitializer
                if (initializer != null) {
                    typeClass =
                        resolveExpressionClass(context, initializer, maxDepth - 1) ?: typeClass
                }
            }
        }
        return typeClass
    }

    /**
     * Follows local variable references to their underlying initializer expression, if available.
     */
    fun unwrapVariableInitializer(expression: UExpression, maxDepth: Int = 4): UExpression {
        var current = expression.unwrapNamedAndParens()
        var depth = 0
        while (depth < maxDepth && current is UReferenceExpression) {
            val resolved = current.resolve() as? PsiVariable ?: break
            val initializer = resolved.toUElementOfType<UVariable>()?.uastInitializer ?: break
            current = initializer.unwrapNamedAndParens()
            depth++
        }
        return current
    }

    fun UExpression.unwrapNamedAndParens(): UExpression =
        ((this as? UNamedExpression)?.expression ?: this).skipParenthesizedExprDown()

    fun UElement.isDescendantOf(ancestor: UElement): Boolean {
        var current: UElement? = this
        while (current != null) {
            if (current == ancestor) return true
            current = current.uastParent
        }
        return false
    }

    /**
     * Returns true if [call] on `CredentialManager` is a headless Restore Credentials operation
     * (`CreateRestoreCredentialRequest` or a `GetCredentialRequest` containing only
     * `GetRestoreCredentialOption`).
     */
    fun isNonUiRestoreCredentialCall(
        context: JavaContext,
        call: UCallExpression,
        method: PsiMethod,
    ): Boolean {
        val methodName = method.name
        val requestArg = findArgument(context, call, method, "request", 1) ?: return false
        if (methodName in CREATE_CREDENTIAL_METHODS) {
            val requestClass = resolveExpressionClass(context, requestArg) ?: return false
            return context.evaluator.inheritsFrom(
                requestClass,
                CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST,
                false,
            )
        }
        if (methodName in GET_CREDENTIAL_METHODS) {
            val optionExprs = extractCredentialOptionExpressions(context, requestArg)
            if (optionExprs.isEmpty()) return false
            return optionExprs.all { optionExpr ->
                val optionClass = resolveExpressionClass(context, optionExpr)
                optionClass != null &&
                    context.evaluator.inheritsFrom(
                        optionClass,
                        CLASS_GET_RESTORE_CREDENTIAL_OPTION,
                        false,
                    )
            }
        }
        return false
    }

    /**
     * Extracts the individual `CredentialOption` expressions configured on a `GetCredentialRequest`
     * expression (supporting constructors, `GetCredentialRequest.createFrom`, `Builder` chains, and
     * local variables).
     */
    fun extractCredentialOptionExpressions(
        context: JavaContext,
        requestExpr: UExpression,
        maxDepth: Int = 4,
    ): List<UExpression> {
        if (maxDepth <= 0) return emptyList()
        val unwrapped = unwrapVariableInitializer(requestExpr, maxDepth)
        val call = unwrapped.asCallExpression() ?: return emptyList()
        val resolvedMethod = call.resolve()

        // Case 1: GetCredentialRequest(optionsList, ...) constructor or createFrom(optionsList,
        // ...)
        val constructedClass =
            resolvedMethod?.containingClass
                ?: call.getExpressionType()?.let { context.evaluator.getTypeClass(it) }
        if (
            constructedClass != null &&
                context.evaluator.inheritsFrom(
                    constructedClass,
                    CLASS_GET_CREDENTIAL_REQUEST,
                    false,
                ) &&
                call.methodName != "build"
        ) {
            val listArg =
                if (resolvedMethod != null) {
                    findArgument(context, call, resolvedMethod, "credentialOptions", 0)
                } else {
                    call.valueArguments.firstOrNull()
                } ?: return emptyList()
            return extractElementsFromListExpression(context, listArg, maxDepth - 1)
        }

        // Case 2: GetCredentialRequest.Builder(...).addCredentialOption(...).build()
        if (
            call.methodName == "build" &&
                constructedClass != null &&
                context.evaluator.inheritsFrom(
                    constructedClass,
                    CLASS_GET_CREDENTIAL_REQUEST_BUILDER,
                    false,
                )
        ) {
            return collectBuilderOptions(context, unwrapped, maxDepth - 1)
        }

        return emptyList()
    }

    private fun collectBuilderOptions(
        context: JavaContext,
        buildExpr: UExpression,
        maxDepth: Int,
    ): List<UExpression> {
        val options = mutableListOf<UExpression>()
        val directChainCalls = mutableListOf<UCallExpression>()
        val initializerChainCalls = mutableListOf<UCallExpression>()
        val visitedChainCallPsis = mutableSetOf<Any>()
        var current: UExpression? = buildExpr.skipParenthesizedExprDown()
        var rootReceiverVar: PsiVariable? = null
        var inInitializer = false

        while (current != null) {
            when (current) {
                is UQualifiedReferenceExpression -> {
                    current.sourcePsi?.let { visitedChainCallPsis.add(it) }
                    val selectorCall = current.selector.asCallExpression()
                    if (selectorCall != null) {
                        if (inInitializer) {
                            initializerChainCalls.add(selectorCall)
                        } else {
                            directChainCalls.add(selectorCall)
                        }
                        visitedChainCallPsis.add(selectorCall.sourcePsi ?: selectorCall)
                    }
                    current = current.receiver.skipParenthesizedExprDown()
                }
                is UCallExpression -> {
                    if (inInitializer) {
                        initializerChainCalls.add(current)
                    } else {
                        directChainCalls.add(current)
                    }
                    visitedChainCallPsis.add(current.sourcePsi ?: current)
                    current = current.receiver?.skipParenthesizedExprDown()
                }
                is UReferenceExpression -> {
                    val resolved = current.resolve() as? PsiVariable
                    if (resolved != null && !inInitializer) {
                        rootReceiverVar = resolved
                        inInitializer = true
                        val init = resolved.toUElementOfType<UVariable>()?.uastInitializer
                        current = init?.skipParenthesizedExprDown()
                    } else {
                        current = null
                    }
                }
                is UParenthesizedExpression ->
                    current = current.expression.skipParenthesizedExprDown()
                else -> current = null
            }
        }

        // First apply options configured in the builder variable's initializer (if any).
        for (call in initializerChainCalls.asReversed()) {
            collectFromBuilderCall(context, call, options, maxDepth)
        }

        // Next collect all method-local builder calls (both fluent calls on `buildExpr` and
        // standalone imperative calls on `rootReceiverVar` prior to `build()`) in source order so
        // `setCredentialOptions` and `addCredentialOption` execute chronologically without
        // double-counting fluent chain calls.
        val localCalls = mutableListOf<UCallExpression>()
        localCalls.addAll(directChainCalls.asReversed())
        if (rootReceiverVar != null) {
            val buildEndOffset = buildExpr.sourcePsi?.textRange?.endOffset
            val enclosingBody: UElement? =
                buildExpr.getParentOfType<UMethod>(true)?.uastBody
                    ?: buildExpr.getParentOfType<UBlockExpression>(true)
            enclosingBody?.accept(
                object : AbstractUastVisitor() {
                    override fun visitCallExpression(node: UCallExpression): Boolean {
                        val key = node.sourcePsi ?: node
                        if (key in visitedChainCallPsis) return false
                        val nodeEndOffset = node.sourcePsi?.textRange?.endOffset
                        if (
                            buildEndOffset != null &&
                                nodeEndOffset != null &&
                                nodeEndOffset > buildEndOffset
                        ) {
                            return false
                        }
                        val receiverRef = unwrapRootReceiverRef(node.receiver)
                        if (receiverRef?.resolve() == rootReceiverVar) {
                            visitedChainCallPsis.add(key)
                            localCalls.add(node)
                        }
                        return false
                    }
                }
            )
            localCalls.sortBy { it.sourcePsi?.textRange?.endOffset ?: Int.MAX_VALUE }
        }
        for (call in localCalls) {
            collectFromBuilderCall(context, call, options, maxDepth)
        }
        return options
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

    private fun collectFromBuilderCall(
        context: JavaContext,
        call: UCallExpression,
        out: MutableList<UExpression>,
        maxDepth: Int,
    ) {
        when (call.methodName) {
            "addCredentialOption" -> {
                call.valueArguments.firstOrNull()?.let { out.add(it) }
            }
            "setCredentialOptions" -> {
                out.clear()
                call.valueArguments.firstOrNull()?.let { listExpr ->
                    out.addAll(extractElementsFromListExpression(context, listExpr, maxDepth))
                }
            }
        }
    }

    fun extractElementsFromListExpression(
        context: JavaContext,
        listExpr: UExpression,
        maxDepth: Int = 4,
    ): List<UExpression> {
        if (maxDepth < 0) return emptyList()
        val initialRef = listExpr.skipParenthesizedExprDown()
        val rootVar: PsiVariable? = (initialRef as? UReferenceExpression)?.resolve() as? PsiVariable
        val unwrapped = unwrapVariableInitializer(listExpr, maxDepth.coerceAtLeast(1))
        val elements = mutableListOf<UExpression>()

        val call = unwrapped.asCallExpression()
        if (
            call != null &&
                (call.methodName in COLLECTION_FACTORY_METHODS || isListConstructor(context, call))
        ) {
            elements.addAll(call.valueArguments)
        }

        if (rootVar != null) {
            val enclosingBody: UElement? =
                listExpr.getParentOfType<UMethod>(true)?.uastBody
                    ?: listExpr.getParentOfType<UBlockExpression>(true)
            enclosingBody?.accept(
                object : AbstractUastVisitor() {
                    override fun visitCallExpression(node: UCallExpression): Boolean {
                        if (node.methodName == "add") {
                            val receiverRef =
                                node.receiver?.skipParenthesizedExprDown() as? UReferenceExpression
                            if (
                                receiverRef?.resolve() == rootVar && node.valueArguments.size == 1
                            ) {
                                elements.add(node.valueArguments[0])
                            }
                        }
                        return false
                    }
                }
            )
        }
        return elements
    }

    private fun isListConstructor(context: JavaContext, call: UCallExpression): Boolean {
        val typeClass =
            call.getExpressionType()?.let { context.evaluator.getTypeClass(it) } ?: return false
        return context.evaluator.inheritsFrom(typeClass, "java.util.Collection", false)
    }

    private fun UExpression.asCallExpression(): UCallExpression? {
        val expr = skipParenthesizedExprDown()
        return when (expr) {
            is UCallExpression -> expr
            is UQualifiedReferenceExpression ->
                expr.selector.skipParenthesizedExprDown() as? UCallExpression
            else -> null
        }
    }
}
