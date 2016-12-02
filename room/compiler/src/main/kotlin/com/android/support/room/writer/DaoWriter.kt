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

package com.android.support.room.writer

import com.android.support.room.ext.AndroidTypeNames
import com.android.support.room.ext.L
import com.android.support.room.ext.N
import com.android.support.room.ext.RoomTypeNames
import com.android.support.room.ext.T
import com.android.support.room.solver.CodeGenScope
import com.android.support.room.vo.Dao
import com.android.support.room.vo.QueryMethod
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC

/**
 * Creates the implementation for a class annotated with Dao.
 */
class DaoWriter(val dao: Dao) : ClassWriter(ClassName.get(dao.type) as ClassName) {
    companion object {
        val dbField : FieldSpec = FieldSpec
                .builder(RoomTypeNames.ROOM_DB, "__db", PRIVATE, FINAL)
                .build()
    }

    override fun createTypeSpec(): TypeSpec {
        val builder = TypeSpec.classBuilder(dao.implClassName)
        builder.apply {
            addModifiers(PUBLIC)
            if (dao.element.kind == ElementKind.INTERFACE) {
                addSuperinterface(dao.typeName)
            } else {
                superclass(dao.typeName)
            }
            addField(dbField)
            val dbParam = ParameterSpec.builder(dbField.type, dbField.name).build()
            addMethod(
                    MethodSpec.constructorBuilder().apply {
                        addParameter(dbParam)
                        addModifiers(PUBLIC)
                        addStatement("this.$N = $N", dbField, dbParam)
                    }.build()
            )
        }
        dao.queryMethods.forEach { method ->
            val baseSpec = MethodSpec.overriding(method.element).build()
            val methodSpec = MethodSpec.methodBuilder(method.name).apply {
                addAnnotation(Override::class.java)
                addModifiers(baseSpec.modifiers)
                addParameters(baseSpec.parameters)
                varargs(baseSpec.varargs)
                returns(baseSpec.returnType)
                addCode(createQueryMethodBody(method))
            }.build()
            builder.addMethod(methodSpec)
        }
        return builder.build()
    }

    private fun createQueryMethodBody(method: QueryMethod) : CodeBlock {
        val queryWriter = QueryWriter(method)
        val scope = CodeGenScope()
        val sqlVar = scope.getTmpVar("_sql")
        val argsVar = scope.getTmpVar("_args")
        queryWriter.prepareReadQuery(sqlVar, argsVar, scope)
        scope.builder().apply {
            val cursorVar = scope.getTmpVar("_cursor")
            val outVar = scope.getTmpVar("_result")
            addStatement("final $T $L = $N.query($L, $L)", AndroidTypeNames.CURSOR, cursorVar,
                    dbField, sqlVar, argsVar)
            beginControlFlow("try")
                method.resultAdapter?.convert(outVar, cursorVar, scope)
                addStatement("return $L", outVar)
            nextControlFlow("finally")
                addStatement("$L.close()", cursorVar)
            endControlFlow()
        }
        return scope.builder().build()
    }
}
