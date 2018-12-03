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

import androidx.room.ext.KotlinMetadataElement
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.RoomCoroutinesTypeNames
import androidx.room.ext.getSuspendFunctionReturnType
import androidx.room.parser.ParsedQuery
import androidx.room.solver.query.result.CoroutineResultBinder
import androidx.room.solver.query.result.QueryResultBinder
import androidx.room.solver.shortcut.binder.CoroutineDeleteOrUpdateMethodBinder
import androidx.room.solver.shortcut.binder.CoroutineInsertMethodBinder
import androidx.room.solver.shortcut.binder.DeleteOrUpdateMethodBinder
import androidx.room.solver.shortcut.binder.InsertMethodBinder
import androidx.room.vo.QueryParameter
import androidx.room.vo.ShortcutQueryParameter
import com.google.auto.common.MoreTypes
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

/**
 *  Delegate class with common functionality for DAO method processors.
 */
abstract class MethodProcessorDelegate(
    val context: Context,
    val containing: DeclaredType,
    val executableElement: ExecutableElement,
    protected val classMetadata: KotlinMetadataElement?
) {

    abstract fun extractReturnType(): TypeMirror

    abstract fun extractParams(): List<VariableElement>

    fun extractQueryParams(): List<QueryParameter> {
        val kotlinParameterNames = classMetadata?.getParameterNames(executableElement)
        return extractParams().mapIndexed { index, variableElement ->
            QueryParameterProcessor(
                baseContext = context,
                containing = containing,
                element = variableElement,
                sqlName = kotlinParameterNames?.getOrNull(index)
            ).process()
        }
    }

    abstract fun findResultBinder(returnType: TypeMirror, query: ParsedQuery): QueryResultBinder

    abstract fun findInsertMethodBinder(
        returnType: TypeMirror,
        params: List<ShortcutQueryParameter>
    ): InsertMethodBinder

    abstract fun findDeleteOrUpdateMethodBinder(returnType: TypeMirror): DeleteOrUpdateMethodBinder

    companion object {
        fun createFor(
            context: Context,
            containing: DeclaredType,
            executableElement: ExecutableElement
        ): MethodProcessorDelegate {
            val kotlinMetadata = KotlinMetadataElement.createFor(context, containing.asElement())
            return if (kotlinMetadata?.isSuspendFunction(executableElement) == true) {
                val hasCoroutineArtifact = context.processingEnv.elementUtils
                    .getTypeElement(RoomCoroutinesTypeNames.COROUTINES_ROOM.toString()) != null
                if (!hasCoroutineArtifact) {
                    context.logger.e(ProcessorErrors.MISSING_ROOM_COROUTINE_ARTIFACT)
                }
                SuspendMethodProcessorDelegate(
                    context,
                    containing,
                    executableElement,
                    kotlinMetadata
                )
            } else {
                DefaultMethodProcessorDelegate(
                    context,
                    containing,
                    executableElement,
                    kotlinMetadata
                )
            }
        }
    }
}

/**
 * Default delegate for DAO methods.
 */
class DefaultMethodProcessorDelegate(
    context: Context,
    containing: DeclaredType,
    executableElement: ExecutableElement,
    classMetadata: KotlinMetadataElement?
) : MethodProcessorDelegate(context, containing, executableElement, classMetadata) {

    override fun extractReturnType(): TypeMirror {
        val asMember = context.processingEnv.typeUtils.asMemberOf(containing, executableElement)
        return MoreTypes.asExecutable(asMember).returnType
    }

    override fun extractParams() = executableElement.parameters

    override fun findResultBinder(returnType: TypeMirror, query: ParsedQuery) =
        context.typeAdapterStore.findQueryResultBinder(returnType, query)

    override fun findInsertMethodBinder(
        returnType: TypeMirror,
        params: List<ShortcutQueryParameter>
    ) = context.typeAdapterStore.findInsertMethodBinder(returnType, params)

    override fun findDeleteOrUpdateMethodBinder(returnType: TypeMirror) =
        context.typeAdapterStore.findDeleteOrUpdateMethodBinder(returnType)
}

/**
 * Delegate for DAO methods that are a suspend function.
 */
class SuspendMethodProcessorDelegate(
    context: Context,
    containing: DeclaredType,
    executableElement: ExecutableElement,
    kotlinMetadata: KotlinMetadataElement
) : MethodProcessorDelegate(context, containing, executableElement, kotlinMetadata) {

    private val continuationParam: VariableElement by lazy {
        val typesUtil = context.processingEnv.typeUtils
        val continuationType = typesUtil.erasure(
            context.processingEnv.elementUtils
                .getTypeElement(KotlinTypeNames.CONTINUATION.toString())
                .asType()
        )
        executableElement.parameters.last {
            typesUtil.isSameType(typesUtil.erasure(it.asType()), continuationType)
        }
    }

    override fun extractReturnType() = executableElement.getSuspendFunctionReturnType()

    override fun extractParams() =
        executableElement.parameters.filterNot { it == continuationParam }

    override fun findResultBinder(returnType: TypeMirror, query: ParsedQuery) =
        CoroutineResultBinder(
            typeArg = returnType,
            adapter = context.typeAdapterStore.findQueryResultAdapter(returnType, query),
            continuationParamName = continuationParam.simpleName.toString()
        )

    override fun findInsertMethodBinder(
        returnType: TypeMirror,
        params: List<ShortcutQueryParameter>
    ) = CoroutineInsertMethodBinder(
        typeArg = returnType,
        adapter = context.typeAdapterStore.findInsertAdapter(returnType, params),
        continuationParamName = continuationParam.simpleName.toString()
    )

    override fun findDeleteOrUpdateMethodBinder(returnType: TypeMirror) =
        CoroutineDeleteOrUpdateMethodBinder(
            typeArg = returnType,
            adapter = context.typeAdapterStore.findDeleteOrUpdateAdapter(returnType),
            continuationParamName = continuationParam.simpleName.toString()
        )
}