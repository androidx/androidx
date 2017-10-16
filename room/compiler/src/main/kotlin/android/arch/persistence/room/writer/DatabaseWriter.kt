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

package android.arch.persistence.room.writer

import android.arch.persistence.room.ext.L
import android.arch.persistence.room.ext.N
import android.arch.persistence.room.ext.RoomTypeNames
import android.arch.persistence.room.ext.SupportDbTypeNames
import android.arch.persistence.room.ext.T
import android.arch.persistence.room.solver.CodeGenScope
import android.arch.persistence.room.vo.DaoMethod
import android.arch.persistence.room.vo.Database
import com.google.auto.common.MoreElements
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import stripNonJava
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.VOLATILE

/**
 * Writes implementation of classes that were annotated with @Database.
 */
class DatabaseWriter(val database : Database) : ClassWriter(database.implTypeName) {
    override fun createTypeSpecBuilder(): TypeSpec.Builder {
        val builder = TypeSpec.classBuilder(database.implTypeName)
        builder.apply {
            addModifiers(PUBLIC)
            superclass(database.typeName)
            addMethod(createCreateOpenHelper())
            addMethod(createCreateInvalidationTracker())
        }
        addDaoImpls(builder)
        return builder
    }

    private fun createCreateInvalidationTracker(): MethodSpec {
        return MethodSpec.methodBuilder("createInvalidationTracker").apply {
            addAnnotation(Override::class.java)
            addModifiers(PROTECTED)
            returns(RoomTypeNames.INVALIDATION_TRACKER)
            val tableNames = database.entities.joinToString(",") {
                "\"${it.tableName}\""
            }
            addStatement("return new $T(this, $L)", RoomTypeNames.INVALIDATION_TRACKER, tableNames)
        }.build()
    }

    private fun  addDaoImpls(builder: TypeSpec.Builder) {
        val scope = CodeGenScope(this)
        builder.apply {
            database.daoMethods.forEach { method ->
                val name = method.dao.typeName.simpleName().decapitalize().stripNonJava()
                val fieldName = scope.getTmpVar("_$name")
                val field = FieldSpec.builder(method.dao.typeName, fieldName,
                        PRIVATE, VOLATILE).build()
                addField(field)
                addMethod(createDaoGetter(field, method))
            }
        }
    }

    private fun createDaoGetter(field: FieldSpec, method: DaoMethod) : MethodSpec {
        return MethodSpec.overriding(MoreElements.asExecutable(method.element)).apply {
            beginControlFlow("if ($N != null)", field).apply {
                addStatement("return $N", field)
            }
            nextControlFlow("else").apply {
                beginControlFlow("synchronized(this)").apply {
                    beginControlFlow("if($N == null)", field).apply {
                        addStatement("$N = new $T(this)", field, method.dao.implTypeName)
                    }
                    endControlFlow()
                    addStatement("return $N", field)
                }
                endControlFlow()
            }
            endControlFlow()
        }.build()
    }

    private fun createCreateOpenHelper() : MethodSpec {
        val scope = CodeGenScope(this)
        return MethodSpec.methodBuilder("createOpenHelper").apply {
            addModifiers(Modifier.PROTECTED)
            returns(SupportDbTypeNames.SQLITE_OPEN_HELPER)

            val configParam = ParameterSpec.builder(RoomTypeNames.ROOM_DB_CONFIG,
                    "configuration").build()
            addParameter(configParam)

            val openHelperVar = scope.getTmpVar("_helper")
            val openHelperCode = scope.fork()
            SQLiteOpenHelperWriter(database)
                    .write(openHelperVar, configParam, openHelperCode)
            addCode(openHelperCode.builder().build())
            addStatement("return $L", openHelperVar)
        }.build()
    }
}
