/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.arch.persistence.room.ext.N
import android.arch.persistence.room.ext.T
import android.arch.persistence.room.solver.CodeGenScope
import android.arch.persistence.room.vo.CustomTypeConverter
import android.arch.persistence.room.writer.ClassWriter
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import javax.lang.model.element.Modifier

/**
 * Wraps a type converter specified by the developer and forwards calls to it.
 */
class CustomTypeConverterWrapper(val custom: CustomTypeConverter)
    : TypeConverter(custom.from, custom.to) {

    override fun convert(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            if (custom.isStatic) {
                addStatement("$L = $T.$L($L)",
                        outputVarName, custom.typeName,
                        custom.methodName, inputVarName)
            } else {
                addStatement("$L = $N.$L($L)",
                        outputVarName, typeConverter(scope),
                        custom.methodName, inputVarName)
            }
        }
    }

    fun typeConverter(scope: CodeGenScope): FieldSpec {
        val baseName = (custom.typeName as ClassName).simpleName().decapitalize()
        return scope.writer.getOrCreateField(object : ClassWriter.SharedFieldSpec(
                baseName, custom.typeName) {
            override fun getUniqueKey(): String {
                return "converter_${custom.typeName}"
            }

            override fun prepare(writer: ClassWriter, builder: FieldSpec.Builder) {
                builder.addModifiers(Modifier.PRIVATE)
                builder.addModifiers(Modifier.FINAL)
                builder.initializer("new $T()", custom.typeName)
            }
        })
    }
}
