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

package androidx.room.solver.types

import androidx.room.ext.L
import androidx.room.solver.CodeGenScope
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.TypeKind.BOOLEAN
import javax.lang.model.type.TypeKind.INT

/**
 * int to boolean adapter.
 */
object PrimitiveBooleanToIntConverter {
    fun create(processingEnvironment: ProcessingEnvironment): List<TypeConverter> {
        val tBoolean = processingEnvironment.typeUtils.getPrimitiveType(BOOLEAN)
        val tInt = processingEnvironment.typeUtils.getPrimitiveType(INT)
        return listOf(
                object : TypeConverter(tBoolean, tInt) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().addStatement("$L = $L ? 1 : 0", outputVarName, inputVarName)
                    }
                },
                object : TypeConverter(tInt, tBoolean) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().addStatement("$L = $L != 0", outputVarName, inputVarName)
                    }
                })
    }
}
