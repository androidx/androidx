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

package androidx.room.solver.shortcut.binder

import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomGuavaTypeNames
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.result.InsertMethodAdapter
import androidx.room.vo.ShortcutQueryParameter
import androidx.room.writer.DaoWriter
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.type.TypeMirror

/**
 * Binder that knows how to write insert methods that return ListenableFuture<T>.
 */
class GuavaListenableFutureInsertMethodBinder(
    private val typeArg: TypeMirror,
    adapter: InsertMethodAdapter?
) : InsertMethodBinder(adapter) {

    private val instantInsertMethodBinder = InstantInsertMethodBinder(adapter)

    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        insertionAdapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    ) {
        val adapterScope = scope.fork()
        val callableImpl = CallableTypeSpecBuilder(typeArg.typeName()) {
            // delegate body code generation to the instant method binder
            instantInsertMethodBinder.convertAndReturn(
                parameters = parameters,
                insertionAdapters = insertionAdapters,
                scope = adapterScope
            )
            addCode(adapterScope.generate())
        }.build()
        scope.builder().apply {
            addStatement(
                "return $T.createListenableFuture($N, $L)",
                RoomGuavaTypeNames.GUAVA_ROOM,
                DaoWriter.dbField,
                callableImpl)
        }
    }
}