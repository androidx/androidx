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

package androidx.room.solver.prepared.binderprovider

import androidx.room.ext.L
import androidx.room.ext.RoomRxJava2TypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.prepared.binder.CallablePreparedQueryResultBinder.Companion.createPrepared
import androidx.room.solver.prepared.binder.PreparedQueryResultBinder
import com.squareup.javapoet.ClassName
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

sealed class RxPreparedQueryResultBinderProvider(
    val context: Context,
    val rxType: RxType
) : PreparedQueryResultBinderProvider {

    private val hasRxJava2Artifact by lazy {
        context.processingEnv.elementUtils
            .getTypeElement(RoomRxJava2TypeNames.RX_ROOM.toString()) != null
    }

    override fun matches(declared: DeclaredType): Boolean =
        declared.typeArguments.size == 1 && matchesRxType(declared)

    private fun matchesRxType(declared: DeclaredType): Boolean {
        val erasure = context.processingEnv.typeUtils.erasure(declared)
        return erasure.typeName() == rxType.className
    }

    override fun provide(declared: DeclaredType, query: ParsedQuery): PreparedQueryResultBinder {
        if (!hasRxJava2Artifact) {
            context.logger.e(ProcessorErrors.MISSING_ROOM_RXJAVA2_ARTIFACT)
        }
        val typeArg = extractTypeArg(declared)
        return createPrepared(
            returnType = typeArg,
            adapter = context.typeAdapterStore.findPreparedQueryResultAdapter(typeArg, query)
        ) { callableImpl, _ ->
            addStatement("return $T.fromCallable($L)", rxType.className, callableImpl)
        }
    }

    abstract fun extractTypeArg(declared: DeclaredType): TypeMirror

    enum class RxType(val className: ClassName) {
        SINGLE(RxJava2TypeNames.SINGLE),
        MAYBE(RxJava2TypeNames.MAYBE),
        COMPLETABLE(RxJava2TypeNames.COMPLETABLE)
    }
}

class RxSinglePreparedQueryResultBinderProvider(context: Context) :
    RxPreparedQueryResultBinderProvider(context, RxType.SINGLE) {
    override fun extractTypeArg(declared: DeclaredType): TypeMirror = declared.typeArguments.first()
}

class RxMaybePreparedQueryResultBinderProvider(context: Context) :
    RxPreparedQueryResultBinderProvider(context, RxType.MAYBE) {
    override fun extractTypeArg(declared: DeclaredType): TypeMirror = declared.typeArguments.first()
}

class RxCompletablePreparedQueryResultBinderProvider(context: Context) :
    RxPreparedQueryResultBinderProvider(context, RxType.COMPLETABLE) {

    private val completableType: TypeMirror? by lazy {
        context.processingEnv.elementUtils
            .getTypeElement(RxJava2TypeNames.COMPLETABLE.toString())?.asType()
    }

    override fun matches(declared: DeclaredType): Boolean {
        if (completableType == null) {
            return false
        }
        val erasure = context.processingEnv.typeUtils.erasure(declared)
        return context.processingEnv.typeUtils.isAssignable(completableType, erasure)
    }

    override fun extractTypeArg(declared: DeclaredType) = context.COMMON_TYPES.VOID
}