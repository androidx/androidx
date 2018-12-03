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
import androidx.room.ext.RoomCoroutinesTypeNames
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.result.DeleteOrUpdateMethodAdapter
import androidx.room.vo.ShortcutQueryParameter
import androidx.room.writer.DaoWriter
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.type.TypeMirror

/**
 * Binder that knows how to write delete and update methods that are suspend functions.
 */
class CoroutineDeleteOrUpdateMethodBinder(
    private val typeArg: TypeMirror,
    private val continuationParamName: String,
    adapter: DeleteOrUpdateMethodAdapter?

) : DeleteOrUpdateMethodBinder(adapter) {

    private val instantDeleteOrUpdateMethodBinder = InstantDeleteOrUpdateMethodBinder(adapter)

    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    ) {
        val adapterScope = scope.fork()
        val callableImpl = CallableTypeSpecBuilder(typeArg.typeName()) {
            instantDeleteOrUpdateMethodBinder.convertAndReturn(
                parameters = parameters,
                adapters = adapters,
                scope = adapterScope
            )
            addCode(adapterScope.generate())
        }.build()

        scope.builder().apply {
            addStatement(
                "return $T.execute($N, $L, $N)",
                RoomCoroutinesTypeNames.COROUTINES_ROOM,
                DaoWriter.dbField,
                callableImpl,
                continuationParamName)
        }
    }
}