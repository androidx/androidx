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
import com.android.support.room.solver.CodeGenScope
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind.BYTE
import javax.lang.model.type.TypeKind.CHAR
import javax.lang.model.type.TypeKind.DOUBLE
import javax.lang.model.type.TypeKind.FLOAT
import javax.lang.model.type.TypeKind.INT
import javax.lang.model.type.TypeKind.LONG
import javax.lang.model.type.TypeKind.SHORT
import javax.lang.model.type.TypeMirror

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
open class PrimitiveToStringConverter(val boxed : TypeMirror,
                                      val parseMethod : String?,
                                      val primitiveType : PrimitiveType,
                                      val stringType : TypeMirror) :
        TypeConverter(primitiveType, stringType) {
    companion object {
        fun createPrimitives(processingEnv : ProcessingEnvironment) : List<TypeConverter> {
            val elmUtils = processingEnv.elementUtils
            val typeUtils = processingEnv.typeUtils
            val stringType = processingEnv.elementUtils.getTypeElement("java.lang.String").asType()

            return listOf(
                Triple(java.lang.Integer::class, "parseInt", INT),
                Triple(java.lang.Long::class, "parseLong", LONG),
                Triple(java.lang.Short::class, "parseShort", SHORT),
                Triple(java.lang.Byte::class, "parseByte", BYTE),
                Triple(java.lang.Float::class, "parseFloat", FLOAT),
                Triple(java.lang.Double::class, "parseDouble", DOUBLE)
            ).map {
                PrimitiveToStringConverter(
                        boxed = elmUtils.getTypeElement(it.first.java.canonicalName).asType(),
                        parseMethod = it.second,
                        primitiveType = typeUtils.getPrimitiveType(it.third),
                        stringType = stringType
                )
            } + object : PrimitiveToStringConverter(
                            boxed = elmUtils.getTypeElement("java.lang.Character").asType(),
                            parseMethod = null,
                            primitiveType = typeUtils.getPrimitiveType(CHAR),
                            stringType = stringType
                    ) {
                        override fun convertBackward(inputVarName: String, outputVarName: String,
                                                     scope: CodeGenScope) {
                            scope.builder().addStatement("$L = $L.charAt(0)", outputVarName,
                                    inputVarName)
                        }
                    }

        }
    }

    override fun convertForward(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder()
                .addStatement("$L = $T.toString($L)", outputVarName, boxed.typeName(), inputVarName)
    }

    override fun convertBackward(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder()
                .addStatement("$L = $T.$L($L)", outputVarName, boxed.typeName(), parseMethod,
                        inputVarName)
    }
}