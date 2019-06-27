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

package androidx.room.solver.shortcut.binderprovider

import androidx.room.ext.L
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.processor.Context
import androidx.room.solver.shortcut.binder.CallableDeleteOrUpdateMethodBinder.Companion.createDeleteOrUpdateBinder
import androidx.room.solver.shortcut.binder.DeleteOrUpdateMethodBinder
import com.squareup.javapoet.ClassName
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

/**
 * Provider for Rx Callable binders.
 */
sealed class RxCallableDeleteOrUpdateMethodBinderProvider(
    val context: Context,
    private val rxType: RxType
) : DeleteOrUpdateMethodBinderProvider {

    /**
     * [Single] and [Maybe] are generics but [Completable] is not so each implementation of this
     * class needs to define how to extract the type argument.
     */
    abstract fun extractTypeArg(declared: DeclaredType): TypeMirror

    override fun matches(declared: DeclaredType): Boolean =
            declared.typeArguments.size == 1 && matchesRxType(declared)

    private fun matchesRxType(declared: DeclaredType): Boolean {
        val erasure = context.processingEnv.typeUtils.erasure(declared)
        return erasure.typeName() == rxType.className
    }

    override fun provide(declared: DeclaredType): DeleteOrUpdateMethodBinder {
        val typeArg = extractTypeArg(declared)
        val adapter = context.typeAdapterStore.findDeleteOrUpdateAdapter(typeArg)
        return createDeleteOrUpdateBinder(typeArg, adapter) { callableImpl, _ ->
            addStatement("return $T.fromCallable($L)", rxType.className, callableImpl)
        }
    }

    /**
     * Supported types for delete and update
     */
    enum class RxType(val className: ClassName) {
        SINGLE(RxJava2TypeNames.SINGLE),
        MAYBE(RxJava2TypeNames.MAYBE),
        COMPLETABLE(RxJava2TypeNames.COMPLETABLE)
    }
}

class RxSingleDeleteOrUpdateMethodBinderProvider(context: Context) :
    RxCallableDeleteOrUpdateMethodBinderProvider(context, RxType.SINGLE) {

    override fun extractTypeArg(declared: DeclaredType): TypeMirror = declared.typeArguments.first()
}

class RxMaybeDeleteOrUpdateMethodBinderProvider(context: Context) :
    RxCallableDeleteOrUpdateMethodBinderProvider(context, RxType.MAYBE) {

    override fun extractTypeArg(declared: DeclaredType): TypeMirror = declared.typeArguments.first()
}

class RxCompletableDeleteOrUpdateMethodBinderProvider(context: Context) :
    RxCallableDeleteOrUpdateMethodBinderProvider(context, RxType.COMPLETABLE) {

    private val completableTypeMirror: TypeMirror? by lazy {
        context.processingEnv.elementUtils
                .getTypeElement(RxJava2TypeNames.COMPLETABLE.toString())?.asType()
    }

    /**
     * Since Completable is not a generic, the supported return type should be Void.
     * Like this, the generated Callable.call method will return Void.
     */
    override fun extractTypeArg(declared: DeclaredType): TypeMirror =
            context.COMMON_TYPES.VOID

    override fun matches(declared: DeclaredType): Boolean = isCompletable(declared)

    private fun isCompletable(declared: DeclaredType): Boolean {
        if (completableTypeMirror == null) {
            return false
        }
        val erasure = context.processingEnv.typeUtils.erasure(declared)
        return context.processingEnv.typeUtils.isAssignable(completableTypeMirror, erasure)
    }
}
