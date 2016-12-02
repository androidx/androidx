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

package com.android.support.room.solver.query.result

import com.android.support.room.ext.L
import com.android.support.room.ext.RoomTypeNames
import com.android.support.room.ext.T
import com.android.support.room.ext.typeName
import com.android.support.room.solver.CodeGenScope
import com.squareup.javapoet.ParameterizedTypeName
import javax.lang.model.type.TypeMirror

class EntityRowAdapter(type : TypeMirror) : RowAdapter(type) {
    override fun init(cursorVarName: String, scope : CodeGenScope) : RowConverter {
        val converterVar = scope.getTmpVar("_converter")
        scope.builder()
                .addStatement("final $T $L = $T.getConverter($T.class)",
                ParameterizedTypeName.get(RoomTypeNames.CURSOR_CONVERTER, out.typeName()),
                converterVar, RoomTypeNames.ROOM, out.typeName())
        return object : RowConverter {
            override fun convert(outVarName: String, cursorVarName: String) {
                scope.builder()
                        .addStatement("$L = $L.convert($L)", outVarName, converterVar,
                                cursorVarName)
            }

        }
    }
}
