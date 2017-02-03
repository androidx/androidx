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

package com.android.support.room.solver.types

import com.android.support.room.ext.L
import com.android.support.room.ext.T
import com.android.support.room.ext.typeName
import com.android.support.room.processor.Context
import com.android.support.room.solver.CodeGenScope
import javax.lang.model.type.TypeMirror

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
object BoxedPrimitiveToStringConverter {
    fun createBoxedPrimitives(context : Context): List<TypeConverter> {
        val elmUtils = context.processingEnv.elementUtils
        val stringType = context.COMMON_TYPES.STRING
        return listOf(
                Pair(java.lang.Integer::class, "parseInt"),
                Pair(java.lang.Long::class, "parseLong"),
                Pair(java.lang.Short::class, "parseShort"),
                Pair(java.lang.Byte::class, "parseByte"),
                Pair(java.lang.Float::class, "parseFloat"),
                Pair(java.lang.Double::class, "parseDouble")
        ).flatMap {
            create(
                    boxed = elmUtils.getTypeElement(it.first.java.canonicalName).asType(),
                    parseMethod = it.second,
                    stringType = stringType
            )
        } + createChar(
                boxedChar = elmUtils.getTypeElement("java.lang.Character").asType(),
                stringType = stringType)
    }

    private fun create(boxed: TypeMirror, parseMethod: String, stringType: TypeMirror)
            : List<TypeConverter> {
        return listOf(
                object : TypeConverter(boxed, stringType) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().addStatement("$L = $L == null ? null : $T.toString($L)",
                                outputVarName, inputVarName, boxed.typeName(), inputVarName)
                    }
                },
                object : TypeConverter(stringType, boxed) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().addStatement("$L = $L == null ? null : $T.$L($L)",
                                outputVarName, inputVarName, boxed.typeName(), parseMethod,
                                inputVarName)
                    }
                }
        )
    }

    private fun createChar(boxedChar: TypeMirror, stringType: TypeMirror)
            : List<TypeConverter> {
        return listOf(
                object : TypeConverter(boxedChar, stringType) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().addStatement("$L = $L == null ? null : $T.toString($L)",
                                outputVarName, inputVarName, boxedChar.typeName(), inputVarName)
                    }
                },
                object : TypeConverter(stringType, boxedChar) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().addStatement("$L = $L == null ? null : $L.charAt(0)",
                                outputVarName, inputVarName, inputVarName)
                    }
                }
        )
    }
}
