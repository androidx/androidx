/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.processor

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XSuspendMethodType
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XVariableElement
import androidx.room.compiler.processing.isSuspendFunction
import androidx.room.ext.DEFERRED_TYPES
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.RoomCoroutinesTypeNames.COROUTINES_ROOM
import androidx.room.parser.ParsedQuery
import androidx.room.solver.TypeAdapterExtras
import androidx.room.solver.prepared.binder.CoroutinePreparedQueryResultBinder
import androidx.room.solver.prepared.binder.PreparedQueryResultBinder
import androidx.room.solver.query.result.CoroutineResultBinder
import androidx.room.solver.query.result.QueryResultBinder
import androidx.room.solver.shortcut.binder.CoroutineDeleteOrUpdateFunctionBinder
import androidx.room.solver.shortcut.binder.CoroutineInsertOrUpsertFunctionBinder
import androidx.room.solver.shortcut.binder.DeleteOrUpdateFunctionBinder
import androidx.room.solver.shortcut.binder.InsertOrUpsertFunctionBinder
import androidx.room.solver.transaction.binder.CoroutineTransactionFunctionBinder
import androidx.room.solver.transaction.binder.InstantTransactionFunctionBinder
import androidx.room.solver.transaction.binder.TransactionFunctionBinder
import androidx.room.solver.transaction.result.TransactionFunctionAdapter
import androidx.room.vo.QueryParameter
import androidx.room.vo.ShortcutQueryParameter
import androidx.room.vo.TransactionFunction

/** Delegate class with common functionality for DAO function processors. */
abstract class FunctionProcessorDelegate(
    val context: Context,
    val containing: XType,
    val executableElement: XMethodElement,
) {

    abstract fun extractReturnType(): XType

    abstract fun extractParams(): List<XExecutableParameterElement>

    fun extractQueryParams(query: ParsedQuery): List<QueryParameter> {
        return extractParams().map { parameterElement ->
            QueryParameterProcessor(
                    baseContext = context,
                    containing = containing,
                    element = parameterElement,
                    sqlName = parameterElement.name,
                    bindVarSection =
                        query.bindSections.firstOrNull { it.varName == parameterElement.name },
                )
                .process()
        }
    }

    abstract fun findResultBinder(
        returnType: XType,
        query: ParsedQuery,
        extrasCreator: TypeAdapterExtras.() -> Unit,
    ): QueryResultBinder

    abstract fun findPreparedResultBinder(
        returnType: XType,
        query: ParsedQuery,
    ): PreparedQueryResultBinder

    abstract fun findInsertFunctionBinder(
        returnType: XType,
        params: List<ShortcutQueryParameter>,
    ): InsertOrUpsertFunctionBinder

    abstract fun findDeleteOrUpdateFunctionBinder(returnType: XType): DeleteOrUpdateFunctionBinder

    abstract fun findUpsertFunctionBinder(
        returnType: XType,
        params: List<ShortcutQueryParameter>,
    ): InsertOrUpsertFunctionBinder

    abstract fun findTransactionFunctionBinder(
        callType: TransactionFunction.CallType
    ): TransactionFunctionBinder

    companion object {
        fun createFor(
            context: Context,
            containing: XType,
            executableElement: XMethodElement,
        ): FunctionProcessorDelegate {
            val asMember = executableElement.asMemberOf(containing)
            return if (asMember.isSuspendFunction()) {
                SuspendFunctionProcessorDelegate(context, containing, executableElement, asMember)
            } else {
                DefaultFunctionProcessorDelegate(context, containing, executableElement, asMember)
            }
        }
    }
}

fun FunctionProcessorDelegate.returnsDeferredType(): Boolean {
    val deferredTypes =
        DEFERRED_TYPES.mapNotNull { context.processingEnv.findType(it.canonicalName) }
    val returnType = extractReturnType()
    return deferredTypes.any { deferredType ->
        deferredType.rawType.isAssignableFrom(returnType.rawType)
    }
}

/** Default delegate for DAO functions. */
class DefaultFunctionProcessorDelegate(
    context: Context,
    containing: XType,
    executableElement: XMethodElement,
    val executableType: XMethodType,
) : FunctionProcessorDelegate(context, containing, executableElement) {

    override fun extractReturnType(): XType {
        return executableType.returnType
    }

    override fun extractParams() = executableElement.parameters

    override fun findResultBinder(
        returnType: XType,
        query: ParsedQuery,
        extrasCreator: TypeAdapterExtras.() -> Unit,
    ) = context.typeAdapterStore.findQueryResultBinder(returnType, query, extrasCreator)

    override fun findPreparedResultBinder(returnType: XType, query: ParsedQuery) =
        context.typeAdapterStore.findPreparedQueryResultBinder(returnType, query)

    override fun findInsertFunctionBinder(returnType: XType, params: List<ShortcutQueryParameter>) =
        context.typeAdapterStore.findInsertFunctionBinder(returnType, params)

    override fun findDeleteOrUpdateFunctionBinder(returnType: XType) =
        context.typeAdapterStore.findDeleteOrUpdateFunctionBinder(returnType)

    override fun findUpsertFunctionBinder(returnType: XType, params: List<ShortcutQueryParameter>) =
        context.typeAdapterStore.findUpsertFunctionBinder(returnType, params)

    override fun findTransactionFunctionBinder(callType: TransactionFunction.CallType) =
        InstantTransactionFunctionBinder(
            returnType = executableElement.returnType,
            adapter =
                TransactionFunctionAdapter(
                    functionName = executableElement.name,
                    jvmMethodName = executableElement.jvmName,
                    callType = callType,
                ),
        )
}

/** Delegate for DAO functions that are a suspend functions. */
class SuspendFunctionProcessorDelegate(
    context: Context,
    containing: XType,
    executableElement: XMethodElement,
    val executableType: XSuspendMethodType,
) : FunctionProcessorDelegate(context, containing, executableElement) {

    private val continuationParam: XVariableElement by lazy {
        val continuationType =
            context.processingEnv.requireType(KotlinTypeNames.CONTINUATION).rawType
        executableElement.parameters.last { it.type.rawType == continuationType }
    }

    override fun extractReturnType(): XType {
        return executableType.getSuspendFunctionReturnType()
    }

    override fun extractParams() =
        executableElement.parameters.filterNot { it == continuationParam }

    override fun findResultBinder(
        returnType: XType,
        query: ParsedQuery,
        extrasCreator: TypeAdapterExtras.() -> Unit,
    ) =
        CoroutineResultBinder(
            typeArg = returnType,
            adapter =
                context.typeAdapterStore.findQueryResultAdapter(returnType, query, extrasCreator),
            continuationParamName = continuationParam.name,
        )

    override fun findPreparedResultBinder(returnType: XType, query: ParsedQuery) =
        CoroutinePreparedQueryResultBinder(
            adapter = context.typeAdapterStore.findPreparedQueryResultAdapter(returnType, query),
            continuationParamName = continuationParam.name,
        )

    override fun findInsertFunctionBinder(returnType: XType, params: List<ShortcutQueryParameter>) =
        CoroutineInsertOrUpsertFunctionBinder(
            typeArg = returnType,
            adapter = context.typeAdapterStore.findInsertAdapter(returnType, params),
            continuationParamName = continuationParam.name,
        )

    override fun findUpsertFunctionBinder(returnType: XType, params: List<ShortcutQueryParameter>) =
        CoroutineInsertOrUpsertFunctionBinder(
            typeArg = returnType,
            adapter = context.typeAdapterStore.findUpsertAdapter(returnType, params),
            continuationParamName = continuationParam.name,
        )

    override fun findDeleteOrUpdateFunctionBinder(returnType: XType) =
        CoroutineDeleteOrUpdateFunctionBinder(
            typeArg = returnType,
            adapter = context.typeAdapterStore.findDeleteOrUpdateAdapter(returnType),
            continuationParamName = continuationParam.name,
        )

    override fun findTransactionFunctionBinder(callType: TransactionFunction.CallType) =
        CoroutineTransactionFunctionBinder(
            returnType = executableElement.returnType,
            adapter =
                TransactionFunctionAdapter(
                    functionName = executableElement.name,
                    jvmMethodName = executableElement.jvmName,
                    callType = callType,
                ),
            continuationParamName = continuationParam.name,
        )

    private fun XCodeBlock.Builder.addCoroutineExecuteStatement(
        callableImpl: XTypeSpec,
        dbProperty: XPropertySpec,
    ) {
        when (context.codeLanguage) {
            CodeLanguage.JAVA ->
                addStatement(
                    "return %T.execute(%N, %L, %L, %N)",
                    COROUTINES_ROOM,
                    dbProperty,
                    "true", // inTransaction
                    callableImpl,
                    continuationParam.name,
                )
            CodeLanguage.KOTLIN ->
                addStatement(
                    "return %T.execute(%N, %L, %L)",
                    COROUTINES_ROOM,
                    dbProperty,
                    "true", // inTransaction
                    callableImpl,
                )
        }
    }
}
