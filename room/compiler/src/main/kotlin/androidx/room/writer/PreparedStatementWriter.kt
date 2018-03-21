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
package androidx.room.writer

import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

/**
 * Creates anonymous classes for RoomTypeNames#SHARED_SQLITE_STMT.
 */
class PreparedStatementWriter(val queryWriter: QueryWriter) {
    fun createAnonymous(classWriter: ClassWriter, dbParam: FieldSpec): TypeSpec {
        val scope = CodeGenScope(classWriter)
        @Suppress("RemoveSingleExpressionStringTemplate")
        return TypeSpec.anonymousClassBuilder("$N", dbParam).apply {
            superclass(RoomTypeNames.SHARED_SQLITE_STMT)
            addMethod(MethodSpec.methodBuilder("createQuery").apply {
                addAnnotation(Override::class.java)
                returns(ClassName.get("java.lang", "String"))
                addModifiers(Modifier.PUBLIC)
                val queryName = scope.getTmpVar("_query")
                val queryGenScope = scope.fork()
                queryWriter.prepareQuery(queryName, queryGenScope)
                addCode(queryGenScope.builder().build())
                addStatement("return $L", queryName)
            }.build())
        }.build()
    }
}
