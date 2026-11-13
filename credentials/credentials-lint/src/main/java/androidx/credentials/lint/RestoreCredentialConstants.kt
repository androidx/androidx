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
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.ULoopExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UNamedExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.USwitchExpression
import org.jetbrains.uast.UTryExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.UastBinaryOperator
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
            "singleton",
            "asList",
            "of",
        )

    private val LIST_CONVERSION_METHODS = setOf("toList", "toMutableList", "toSet", "toHashSet")

    private val LIST_WRAPPER_METHODS = setOf("unmodifiableList", "synchronizedList", "checkedList")

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

    /**
     * Follows local variable initializers and same-file helper method return expressions up to
     * [maxDepth] hops.
     */
    fun unwrapVariableOrHelperReturn(
        context: JavaContext,
        expression: UExpression,
        maxDepth: Int = 4,
    ): UExpression {
        var current = unwrapVariableInitializer(expression, maxDepth)
        var depth = 0
        while (depth < maxDepth) {
            val call = current.asCallExpression() ?: break
            val resolved = call.resolve() ?: break
            if (resolved.isConstructor) break
            val helperMethod = resolved.toUElementOfType<UMethod>() ?: break
            if (helperMethod.sourcePsi?.containingFile != context.psiFile) break
            val body = helperMethod.uastBody ?: break
            val returned: UExpression =
                when (body) {
                    is UReturnExpression -> body.returnExpression
                    is UBlockExpression ->
                        (body.expressions.singleOrNull() as? UReturnExpression)?.returnExpression
                    else -> body
                } ?: break
            current = unwrapVariableInitializer(returned, maxDepth - depth - 1)
            depth++
        }
        return current
    }

    /**
     * Returns true if any [UElement] in [root] (or in local variable initializers / same-file
     * helper methods referenced within [root], up to [maxDepth] hops) satisfies [predicate].
     */
    fun anyInSubtreeOrSameFileHelpers(
        context: JavaContext,
        root: UElement?,
        maxDepth: Int = 2,
        predicate: (UElement) -> Boolean,
    ): Boolean {
        if (root == null || maxDepth < 0) return false
        var matched = false
        root.accept(
            object : AbstractUastVisitor() {
                override fun visitElement(node: UElement): Boolean {
                    if (matched) return true
                    if (predicate(node)) {
                        matched = true
                        return true
                    }
                    if (maxDepth > 0) {
                        if (node is USimpleNameReferenceExpression) {
                            val init =
                                (node.resolve() as? PsiVariable)
                                    ?.toUElementOfType<UVariable>()
                                    ?.uastInitializer
                            if (
                                init != null &&
                                    anyInSubtreeOrSameFileHelpers(
                                        context,
                                        init,
                                        maxDepth - 1,
                                        predicate,
                                    )
                            ) {
                                matched = true
                                return true
                            }
                        } else if (node is UCallExpression) {
                            val resolved = node.resolve()
                            if (resolved != null && !resolved.isConstructor) {
                                val helperMethod = resolved.toUElementOfType<UMethod>()
                                if (
                                    helperMethod != null &&
                                        helperMethod.sourcePsi?.containingFile == context.psiFile
                                ) {
                                    val body = helperMethod.uastBody
                                    if (
                                        body != null &&
                                            anyInSubtreeOrSameFileHelpers(
                                                context,
                                                body,
                                                maxDepth - 1,
                                                predicate,
                                            )
                                    ) {
                                        matched = true
                                        return true
                                    }
                                }
                            }
                        }
                    }
                    return false
                }
            }
        )
        return matched
    }

    /**
     * If [expr] accesses Kotlin property [propName] (`receiver.propName`) or Java getter
     * [getterName] (`receiver.getterName()`), returns the underlying `receiver` expression.
     */
    fun selectorMatchesPropertyOrGetter(
        expr: UExpression,
        propName: String,
        getterName: String,
    ): UExpression? {
        val unwrapped = expr.unwrapNamedAndParens()
        if (unwrapped is UQualifiedReferenceExpression) {
            val sel = unwrapped.selector.skipParenthesizedExprDown()
            if (
                (sel is USimpleNameReferenceExpression && sel.identifier == propName) ||
                    (sel is UCallExpression && sel.methodName == getterName)
            ) {
                return unwrapped.receiver
            }
        }
        if (unwrapped is UCallExpression && unwrapped.methodName == getterName) {
            return unwrapped.receiver
        }
        return null
    }

    fun UExpression.unwrapNamedAndParens(): UExpression =
        ((this as? UNamedExpression)?.expression ?: this).skipParenthesizedExprDown()

    fun UExpression.asCallExpression(): UCallExpression? {
        val expr = unwrapNamedAndParens()
        return when (expr) {
            is UCallExpression -> expr
            is UQualifiedReferenceExpression ->
                expr.selector.skipParenthesizedExprDown() as? UCallExpression
            else -> null
        }
    }

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
     * expression (supporting constructors, `GetCredentialRequest.createFrom`, `Builder` chains,
     * local variables, and same-file helper methods).
     */
    fun extractCredentialOptionExpressions(
        context: JavaContext,
        requestExpr: UExpression,
        maxDepth: Int = 4,
    ): List<UExpression> {
        if (maxDepth <= 0) return emptyList()
        val unwrapped = unwrapVariableOrHelperReturn(context, requestExpr, maxDepth)
        val call = unwrapped.asCallExpression() ?: return emptyList()
        val resolvedMethod = call.resolve()

        val constructedClass =
            resolvedMethod?.containingClass
                ?: call.getExpressionType()?.let { context.evaluator.getTypeClass(it) }
        val isRequestOrCompanion =
            constructedClass != null &&
                (context.evaluator.inheritsFrom(
                    constructedClass,
                    CLASS_GET_CREDENTIAL_REQUEST,
                    false,
                ) || constructedClass.qualifiedName == "$CLASS_GET_CREDENTIAL_REQUEST.Companion")
        if (isRequestOrCompanion && call.methodName != "build") {
            if (call.methodName == "createFrom" && resolvedMethod != null) {
                val firstParamClass =
                    resolvedMethod.parameterList.parameters.firstOrNull()?.type?.let {
                        context.evaluator.getTypeClass(it)
                    } ?: return emptyList()
                if (!context.evaluator.inheritsFrom(firstParamClass, "java.util.Collection", false))
                    return emptyList()
            }
            val listArg =
                if (resolvedMethod != null) {
                    findArgument(context, call, resolvedMethod, "credentialOptions", 0)
                } else {
                    call.valueArguments.firstOrNull()
                } ?: return emptyList()
            return extractElementsFromListExpression(context, listArg, maxDepth - 1)
        }

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
                        if (inInitializer) initializerChainCalls.add(selectorCall)
                        else directChainCalls.add(selectorCall)
                        visitedChainCallPsis.add(selectorCall.sourcePsi ?: selectorCall)
                    }
                    current = current.receiver.skipParenthesizedExprDown()
                }
                is UCallExpression -> {
                    if (inInitializer) initializerChainCalls.add(current)
                    else directChainCalls.add(current)
                    visitedChainCallPsis.add(current.sourcePsi ?: current)
                    current = current.receiver?.skipParenthesizedExprDown()
                }
                is UReferenceExpression -> {
                    val resolved = current.resolve() as? PsiVariable
                    if (resolved != null && !inInitializer) {
                        rootReceiverVar = resolved
                        inInitializer = true
                        current =
                            resolved
                                .toUElementOfType<UVariable>()
                                ?.uastInitializer
                                ?.skipParenthesizedExprDown()
                    } else {
                        current = null
                    }
                }
                is UParenthesizedExpression ->
                    current = current.expression.skipParenthesizedExprDown()
                else -> current = null
            }
        }

        for (call in initializerChainCalls.asReversed()) {
            collectFromBuilderCall(context, call, options, maxDepth)
        }

        val localCalls = directChainCalls.asReversed().toMutableList()
        var hasBranchedMutation = false
        if (rootReceiverVar != null) {
            val buildEndOffset = buildExpr.sourcePsi?.textRange?.endOffset
            val enclosingBody: UElement? =
                buildExpr.getParentOfType<UMethod>(true)?.uastBody
                    ?: buildExpr.getParentOfType<UBlockExpression>(true)
            enclosingBody?.accept(
                object : AbstractUastVisitor() {
                    override fun visitCallExpression(node: UCallExpression): Boolean {
                        val key = node.sourcePsi ?: node
                        val nodeEndOffset = node.sourcePsi?.textRange?.endOffset
                        if (
                            key !in visitedChainCallPsis &&
                                (buildEndOffset == null ||
                                    nodeEndOffset == null ||
                                    nodeEndOffset <= buildEndOffset) &&
                                unwrapRootReceiverRef(node.receiver)?.resolve() == rootReceiverVar
                        ) {
                            if (isInsideControlFlowBranch(node, enclosingBody)) {
                                hasBranchedMutation = true
                                return true
                            }
                            visitedChainCallPsis.add(key)
                            localCalls.add(node)
                        }
                        return false
                    }
                }
            )
            if (hasBranchedMutation) return emptyList()
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
            "addCredentialOption" -> call.valueArguments.firstOrNull()?.let { out.add(it) }
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
        val initialRef = listExpr.unwrapNamedAndParens()
        val rootVar = (initialRef as? UReferenceExpression)?.resolve() as? PsiVariable
        val unwrapped = unwrapVariableInitializer(initialRef, maxDepth.coerceAtLeast(1))
        val elements = mutableListOf<UExpression>()

        val call = unwrapped.asCallExpression()
        if (call != null) {
            when {
                call.methodName in COLLECTION_FACTORY_METHODS -> {
                    for (arg in call.valueArguments) {
                        elements.add(arg.unwrapNamedAndParens())
                    }
                }
                isListConstructor(context, call) -> {
                    for (arg in call.valueArguments) {
                        val argExpr = arg.unwrapNamedAndParens()
                        if (isCollectionExpression(context, argExpr)) {
                            elements.addAll(
                                extractElementsFromListExpression(context, argExpr, maxDepth - 1)
                            )
                        }
                    }
                }
                call.methodName in LIST_CONVERSION_METHODS && call.valueArguments.isEmpty() -> {
                    val receiver =
                        call.receiver ?: (unwrapped as? UQualifiedReferenceExpression)?.receiver
                    if (receiver != null) {
                        elements.addAll(
                            extractElementsFromListExpression(context, receiver, maxDepth - 1)
                        )
                    }
                }
                call.methodName in LIST_WRAPPER_METHODS && call.valueArguments.isNotEmpty() -> {
                    elements.addAll(
                        extractElementsFromListExpression(
                            context,
                            call.valueArguments[0].unwrapNamedAndParens(),
                            maxDepth - 1,
                        )
                    )
                }
                call.methodName == "buildList" -> {
                    val lambdaArg =
                        call.valueArguments.lastOrNull()?.unwrapNamedAndParens()
                            as? ULambdaExpression
                    applyListMutationsInBlock(
                        context,
                        lambdaArg?.body,
                        targetVar = null,
                        maxEndOffset = null,
                        elements = elements,
                        maxDepth = maxDepth,
                    )
                }
            }
        }

        if (rootVar != null) {
            val enclosingBody: UElement? =
                initialRef.getParentOfType<UMethod>(true)?.uastBody
                    ?: initialRef.getParentOfType<UBlockExpression>(true)
            applyListMutationsInBlock(
                context,
                enclosingBody,
                targetVar = rootVar,
                maxEndOffset = initialRef.sourcePsi?.textRange?.endOffset,
                elements = elements,
                maxDepth = maxDepth,
            )
        }
        return elements
    }

    private fun applyListMutationsInBlock(
        context: JavaContext,
        block: UElement?,
        targetVar: PsiVariable?,
        maxEndOffset: Int?,
        elements: MutableList<UExpression>,
        maxDepth: Int,
    ) {
        if (block == null) return
        val mutations = mutableListOf<Pair<Int, () -> Unit>>()
        var hasBranchedMutation = false
        block.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    if (hasBranchedMutation) return true
                    val nodeEndOffset = node.sourcePsi?.textRange?.endOffset
                    if (
                        maxEndOffset != null &&
                            nodeEndOffset != null &&
                            nodeEndOffset > maxEndOffset
                    ) {
                        return false
                    }
                    if (targetVar != null) {
                        val receiverRef =
                            node.receiver?.skipParenthesizedExprDown() as? UReferenceExpression
                        if (receiverRef?.resolve() != targetVar) return false
                    }
                    val orderKey = nodeEndOffset ?: Int.MAX_VALUE
                    when (node.methodName) {
                        "add" -> {
                            val elemArg =
                                when (node.valueArguments.size) {
                                    1 -> node.valueArguments[0]
                                    2 -> node.valueArguments[1]
                                    else -> null
                                }
                            if (elemArg != null) {
                                if (isInsideControlFlowBranch(node, block)) {
                                    hasBranchedMutation = true
                                    return true
                                }
                                mutations.add(
                                    orderKey to { elements.add(elemArg.unwrapNamedAndParens()) }
                                )
                            }
                        }
                        "addAll" -> {
                            val collArg = node.valueArguments.lastOrNull()
                            if (collArg != null) {
                                if (isInsideControlFlowBranch(node, block)) {
                                    hasBranchedMutation = true
                                    return true
                                }
                                mutations.add(
                                    orderKey to
                                        {
                                            elements.addAll(
                                                extractElementsFromListExpression(
                                                    context,
                                                    collArg,
                                                    maxDepth - 1,
                                                )
                                            )
                                        }
                                )
                            }
                        }
                        "clear" -> {
                            if (node.valueArguments.isEmpty()) {
                                if (isInsideControlFlowBranch(node, block)) {
                                    hasBranchedMutation = true
                                    return true
                                }
                                mutations.add(orderKey to { elements.clear() })
                            }
                        }
                    }
                    return false
                }

                override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
                    if (hasBranchedMutation) return true
                    if (
                        targetVar == null ||
                            (node.operator != UastBinaryOperator.PLUS_ASSIGN &&
                                node.operator.text != "+=")
                    ) {
                        return false
                    }
                    val nodeEndOffset = node.sourcePsi?.textRange?.endOffset
                    if (
                        maxEndOffset != null &&
                            nodeEndOffset != null &&
                            nodeEndOffset > maxEndOffset
                    ) {
                        return false
                    }
                    val leftRef =
                        node.leftOperand.skipParenthesizedExprDown() as? UReferenceExpression
                    if (leftRef?.resolve() != targetVar) return false
                    if (isInsideControlFlowBranch(node, block)) {
                        hasBranchedMutation = true
                        return true
                    }
                    val orderKey = nodeEndOffset ?: Int.MAX_VALUE
                    val rhs = node.rightOperand
                    mutations.add(
                        orderKey to
                            {
                                if (isCollectionExpression(context, rhs)) {
                                    elements.addAll(
                                        extractElementsFromListExpression(
                                            context,
                                            rhs,
                                            maxDepth - 1,
                                        )
                                    )
                                } else {
                                    elements.add(rhs)
                                }
                            }
                    )
                    return false
                }
            }
        )
        if (hasBranchedMutation) {
            elements.clear()
            return
        }
        mutations.sortBy { it.first }
        for ((_, applyMutation) in mutations) {
            applyMutation()
        }
    }

    private fun isInsideControlFlowBranch(node: UElement, stopAt: UElement): Boolean {
        var current: UElement? = node.uastParent
        while (current != null && current != stopAt) {
            if (
                current is UIfExpression ||
                    current is USwitchExpression ||
                    current is UTryExpression ||
                    current is ULoopExpression
            ) {
                return true
            }
            current = current.uastParent
        }
        return false
    }

    private fun isCollectionExpression(context: JavaContext, expr: UExpression): Boolean {
        val typeClass = resolveExpressionClass(context, expr) ?: return false
        return context.evaluator.inheritsFrom(typeClass, "java.lang.Iterable", false)
    }

    private fun isListConstructor(context: JavaContext, call: UCallExpression): Boolean {
        val resolved = call.resolve()
        if (resolved != null && !resolved.isConstructor) return false
        val typeClass =
            call.getExpressionType()?.let { context.evaluator.getTypeClass(it) } ?: return false
        return context.evaluator.inheritsFrom(typeClass, "java.util.Collection", false)
    }
}
