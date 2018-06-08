/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.vo

import androidx.room.OnConflictStrategy
import androidx.room.ext.typeName
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

data class InsertionMethod(val element: ExecutableElement, val name: String,
                           @OnConflictStrategy val onConflict: Int,
                           val entities: Map<String, Entity>, val returnType: TypeMirror,
                           val insertionType: Type?,
                           val parameters: List<ShortcutQueryParameter>) {
    fun insertMethodTypeFor(param: ShortcutQueryParameter): Type {
        return if (insertionType == Type.INSERT_VOID || insertionType == null) {
            Type.INSERT_VOID
        } else if (!param.isMultiple) {
            Type.INSERT_SINGLE_ID
        } else {
            insertionType
        }
    }

    enum class Type(
            // methodName matches EntityInsertionAdapter methods
            val methodName: String, val returnTypeName: TypeName) {
        INSERT_VOID("insert", TypeName.VOID), // return void
        INSERT_SINGLE_ID("insertAndReturnId", TypeName.LONG), // return long
        INSERT_ID_ARRAY("insertAndReturnIdsArray",
                ArrayTypeName.of(TypeName.LONG)), // return long[]
        INSERT_ID_ARRAY_BOX("insertAndReturnIdsArrayBox",
                ArrayTypeName.of(TypeName.LONG.box())), // return Long[]
        INSERT_ID_LIST("insertAndReturnIdsList", // return List<Long>
                ParameterizedTypeName.get(List::class.typeName(), TypeName.LONG.box()))
    }
}
