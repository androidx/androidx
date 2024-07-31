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
import androidx.room.solver.shortcut.binder.CoroutineDeleteOrUpdateMethodBinder
import androidx.room.solver.shortcut.binder.CoroutineInsertOrUpsertMethodBinder
import androidx.room.solver.shortcut.binder.DeleteOrUpdateMethodBinder
import androidx.room.solver.shortcut.binder.InsertOrUpsertMethodBinder
import androidx.room.solver.transaction.binder.CoroutineTransactionMethodBinder
import androidx.room.solver.transaction.binder.InstantTransactionMethodBinder
import androidx.room.solver.transaction.binder.TransactionMethodBinder
import androidx.room.solver.transaction.result.TransactionMethodAdapter
import androidx.room.vo.QueryParameter
import androidx.room.vo.ShortcutQueryParameter
import androidx.room.vo.TransactionMethod

/** Delegate class with common functionality for DAO method processors. */
abstract class MethodProcessorDelegate(
    val context: Context,
    val containing: XType,
    val executableElement: XMethodElement
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
                        query.bindSections.firstOrNull { it.varName == parameterElement.name }
                )
                .process()
        }
    }

    abstract fun findResultBinder(
        returnType: XType,
        query: ParsedQuery,
        extrasCreator: TypeAdapterExtras.() -> Unit
    ): QueryResultBinder

    abstract fun findPreparedResultBinder(
        returnType: XType,
        query: ParsedQuery
    ): PreparedQueryResultBinder

    abstract fun findInsertMethodBinder(
        returnType: XType,
        params: List<ShortcutQueryParameter>
    ): InsertOrUpsertMethodBinder

    abstract fun findDeleteOrUpdateMethodBinder(returnType: XType): DeleteOrUpdateMethodBinder

    abstract fun findUpsertMethodBinder(
        returnType: XType,
        params: List<ShortcutQueryParameter>
    ): InsertOrUpsertMethodBinder

    abstract fun findTransactionMethodBinder(
        callType: TransactionMethod.CallType
    ): TransactionMethodBinder

    companion object {
        fun createFor(
            context: Context,
            containing: XType,
            executableElement: XMethodElement
        ): MethodProcessorDelegate {
            val asMember = executableElement.asMemberOf(containing)
            return if (asMember.isSuspendFunction()) {
                SuspendMethodProcessorDelegate(context, containing, executableElement, asMember)
            } else {
                DefaultMethodProcessorDelegate(context, containing, executableElement, asMember)
            }
        }
    }
}

fun MethodProcessorDelegate.returnsDeferredType(): Boolean {
    val deferredTypes =
        DEFERRED_TYPES.mapNotNull { context.processingEnv.findType(it.canonicalName) }
    val returnType = extractReturnType()
    return deferredTypes.any { deferredType ->
        deferredType.rawType.isAssignableFrom(returnType.rawType)
    }
}

/** Default delegate for DAO methods. */
class DefaultMethodProcessorDelegate(
    context: Context,
    containing: XType,
    executableElement: XMethodElement,
    val executableType: XMethodType
) : MethodProcessorDelegate(context, containing, executableElement) {

    override fun extractReturnType(): XType {
        return executableType.returnType
    }

    override fun extractParams() = executableElement.parameters

    override fun findResultBinder(
        returnType: XType,
        query: ParsedQuery,
        extrasCreator: TypeAdapterExtras.() -> Unit
    ) = context.typeAdapterStore.findQueryResultBinder(returnType, query, extrasCreator)

    override fun findPreparedResultBinder(returnType: XType, query: ParsedQuery) =
        context.typeAdapterStore.findPreparedQueryResultBinder(returnType, query)

    override fun findInsertMethodBinder(returnType: XType, params: List<ShortcutQueryParameter>) =
        context.typeAdapterStore.findInsertMethodBinder(returnType, params)

    override fun findDeleteOrUpdateMethodBinder(returnType: XType) =
        context.typeAdapterStore.findDeleteOrUpdateMethodBinder(returnType)

    override fun findUpsertMethodBinder(returnType: XType, params: List<ShortcutQueryParameter>) =
        context.typeAdapterStore.findUpsertMethodBinder(returnType, params)

    override fun findTransactionMethodBinder(callType: TransactionMethod.CallType) =
        InstantTransactionMethodBinder(
            returnType = executableElement.returnType,
            adapter =
                TransactionMethodAdapter(
                    methodName = executableElement.name,
                    jvmMethodName = executableElement.jvmName,
                    callType = callType
                ),
        )
}

/** Delegate for DAO methods that are a suspend function. */
class SuspendMethodProcessorDelegate(
    context: Context,
    containing: XType,
    executableElement: XMethodElement,
    val executableType: XSuspendMethodType
) : MethodProcessorDelegate(context, containing, executableElement) {

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
        extrasCreator: TypeAdapterExtras.() -> Unit
    ) =
        CoroutineResultBinder(
            typeArg = returnType,
            adapter =
                context.typeAdapterStore.findQueryResultAdapter(returnType, query, extrasCreator),
            continuationParamName = continuationParam.name
        )

    override fun findPreparedResultBinder(returnType: XType, query: ParsedQuery) =
        CoroutinePreparedQueryResultBinder(
            adapter = context.typeAdapterStore.findPreparedQueryResultAdapter(returnType, query),
            continuationParamName = continuationParam.name
        )

    override fun findInsertMethodBinder(returnType: XType, params: List<ShortcutQueryParameter>) =
        CoroutineInsertOrUpsertMethodBinder(
            typeArg = returnType,
            adapter = context.typeAdapterStore.findInsertAdapter(returnType, params),
            continuationParamName = continuationParam.name
        )

    override fun findUpsertMethodBinder(returnType: XType, params: List<ShortcutQueryParameter>) =
        CoroutineInsertOrUpsertMethodBinder(
            typeArg = returnType,
            adapter = context.typeAdapterStore.findUpsertAdapter(returnType, params),
            continuationParamName = continuationParam.name
        )

    override fun findDeleteOrUpdateMethodBinder(returnType: XType) =
        CoroutineDeleteOrUpdateMethodBinder(
            typeArg = returnType,
            adapter = context.typeAdapterStore.findDeleteOrUpdateAdapter(returnType),
            continuationParamName = continuationParam.name
        )

    override fun findTransactionMethodBinder(callType: TransactionMethod.CallType) =
        CoroutineTransactionMethodBinder(
            returnType = executableElement.returnType,
            adapter =
                TransactionMethodAdapter(
                    methodName = executableElement.name,
                    jvmMethodName = executableElement.jvmName,
                    callType = callType
                ),
            continuationParamName = continuationParam.name
        )

    private fun XCodeBlock.Builder.addCoroutineExecuteStatement(
        callableImpl: XTypeSpec,
        dbProperty: XPropertySpec
    ) {
        when (context.codeLanguage) {
            CodeLanguage.JAVA ->
                addStatement(
                    "return %T.execute(%N, %L, %L, %N)",
                    COROUTINES_ROOM,
                    dbProperty,
                    "true", // inTransaction
                    callableImpl,
                    continuationParam.name
                )
            CodeLanguage.KOTLIN ->
                addStatement(
                    "return %T.execute(%N, %L, %L)",
                    COROUTINES_ROOM,
                    dbProperty,
                    "true", // inTransaction
                    callableImpl
                )
        }
    }
}
