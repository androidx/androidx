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

package android.arch.persistence.room.solver.types

import android.arch.persistence.room.ext.L
import android.arch.persistence.room.ext.T
import android.arch.persistence.room.ext.typeName
import android.arch.persistence.room.processor.Context
import android.arch.persistence.room.solver.CodeGenScope
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeKind.BYTE
import javax.lang.model.type.TypeKind.DOUBLE
import javax.lang.model.type.TypeKind.FLOAT
import javax.lang.model.type.TypeKind.INT
import javax.lang.model.type.TypeKind.LONG
import javax.lang.model.type.TypeKind.SHORT
import javax.lang.model.type.TypeMirror

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
object PrimitiveToStringConverter {
    fun createPrimitives(context: Context): List<TypeConverter> {
        val elmUtils = context.processingEnv.elementUtils
        val typeUtils = context.processingEnv.typeUtils
        val stringType = context.COMMON_TYPES.STRING

        return listOf(
                Triple(java.lang.Integer::class, "parseInt", INT),
                Triple(java.lang.Long::class, "parseLong", LONG),
                Triple(java.lang.Short::class, "parseShort", SHORT),
                Triple(java.lang.Byte::class, "parseByte", BYTE),
                Triple(java.lang.Float::class, "parseFloat", FLOAT),
                Triple(java.lang.Double::class, "parseDouble", DOUBLE)
        ).flatMap {
            create(
                    boxed = elmUtils.getTypeElement(it.first.java.canonicalName).asType(),
                    parseMethod = it.second,
                    primitiveType = typeUtils.getPrimitiveType(it.third),
                    stringType = stringType
            )
        } + createChar(stringType = stringType,
                boxed = elmUtils.getTypeElement("java.lang.Character").asType(),
                charType = typeUtils.getPrimitiveType(TypeKind.CHAR))
    }

    private fun createChar(stringType: TypeMirror, boxed: TypeMirror,
                           charType: PrimitiveType): List<TypeConverter> {
        return listOf(
                object : TypeConverter(charType, stringType) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().addStatement("$L = $T.toString($L)",
                                outputVarName, boxed.typeName(), inputVarName)
                    }
                },
                object : TypeConverter(stringType, charType) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().addStatement("$L = $L.charAt(0)",
                                outputVarName, inputVarName)
                    }
                })
    }

    private fun create(boxed: TypeMirror,
                       parseMethod: String?,
                       primitiveType: PrimitiveType,
                       stringType: TypeMirror): List<TypeConverter> {
        return listOf(
                object : TypeConverter(primitiveType, stringType) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().addStatement("$L = $T.toString($L)",
                                outputVarName, boxed.typeName(), inputVarName)
                    }
                },
                object : TypeConverter(stringType, primitiveType) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().addStatement("$L = $T.$L($L)",
                                outputVarName, boxed.typeName(), parseMethod, inputVarName)
                    }

                })
    }
}
