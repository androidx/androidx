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

package com.android.support.room.solver

import com.android.support.room.ext.L
import com.android.support.room.ext.N
import com.android.support.room.ext.T
import com.squareup.javapoet.ClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.TypeMirror

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class IntListConverter(first : TypeMirror, second : TypeMirror) : TypeConverter(first, second) {
    companion object {
        fun create(processingEnv : ProcessingEnvironment) : IntListConverter {
            val stringType = processingEnv.elementUtils
                    .getTypeElement(String::class.java.canonicalName)
                    .asType()
            val intType = processingEnv.elementUtils
                    .getTypeElement(Integer::class.java.canonicalName)
                    .asType()
            val listType = processingEnv.elementUtils
                    .getTypeElement(java.util.List::class.java.canonicalName)
            val listOfInts = processingEnv.typeUtils.getDeclaredType(listType, intType)
            return IntListConverter(stringType, listOfInts)
        }

        val STRING_UTIL: ClassName = ClassName.get("com.android.support.room.util", "StringUtil")
    }
    override fun convertForward(inputVarName: String, outputVarName: String,
                                scope: CodeGenScope) {
        scope.builder()
                .addStatement("$L = $T.splitToIntList($L)", outputVarName, STRING_UTIL,
                        inputVarName)
    }

    override fun convertBackward(inputVarName: String, outputVarName: String,
                                 scope: CodeGenScope) {
        scope.builder()
                .addStatement("$L = $T.joinIntoString($L)", outputVarName, STRING_UTIL,
                        inputVarName)
    }
}
