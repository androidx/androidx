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

import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.SupportDbTypeNames
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import androidx.room.vo.DaoMethod
import androidx.room.vo.Database
import com.google.auto.common.MoreElements
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
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
class DatabaseWriter(val database: Database) : ClassWriter(database.implTypeName) {
    override fun createTypeSpecBuilder(): TypeSpec.Builder {
        val builder = TypeSpec.classBuilder(database.implTypeName)
        builder.apply {
            addModifiers(PUBLIC)
            superclass(database.typeName)
            addMethod(createCreateOpenHelper())
            addMethod(createCreateInvalidationTracker())
            addMethod(createClearAllTables())
        }
        addDaoImpls(builder)
        return builder
    }

    private fun createClearAllTables(): MethodSpec {
        val scope = CodeGenScope(this)
        return MethodSpec.methodBuilder("clearAllTables").apply {
            addStatement("super.assertNotMainThread()")
            val dbVar = scope.getTmpVar("_db")
            addStatement("final $T $L = super.getOpenHelper().getWritableDatabase()",
                    SupportDbTypeNames.DB, dbVar)
            val deferVar = scope.getTmpVar("_supportsDeferForeignKeys")
            if (database.enableForeignKeys) {
                addStatement("boolean $L = $L.VERSION.SDK_INT >= $L.VERSION_CODES.LOLLIPOP",
                        deferVar, AndroidTypeNames.BUILD, AndroidTypeNames.BUILD)
            }
            addAnnotation(Override::class.java)
            addModifiers(PUBLIC)
            returns(TypeName.VOID)
            beginControlFlow("try").apply {
                if (database.enableForeignKeys) {
                    beginControlFlow("if (!$L)", deferVar).apply {
                        addStatement("$L.execSQL($S)", dbVar, "PRAGMA foreign_keys = FALSE")
                    }
                    endControlFlow()
                }
                addStatement("super.beginTransaction()")
                if (database.enableForeignKeys) {
                    beginControlFlow("if ($L)", deferVar).apply {
                        addStatement("$L.execSQL($S)", dbVar, "PRAGMA defer_foreign_keys = TRUE")
                    }
                    endControlFlow()
                }
                database.entities.sortedWith(EntityDeleteComparator()).forEach {
                    addStatement("$L.execSQL($S)", dbVar, "DELETE FROM `${it.tableName}`")
                }
                addStatement("super.setTransactionSuccessful()")
            }
            nextControlFlow("finally").apply {
                addStatement("super.endTransaction()")
                if (database.enableForeignKeys) {
                    beginControlFlow("if (!$L)", deferVar).apply {
                        addStatement("$L.execSQL($S)", dbVar, "PRAGMA foreign_keys = TRUE")
                    }
                    endControlFlow()
                }
                addStatement("$L.query($S).close()", dbVar, "PRAGMA wal_checkpoint(FULL)")
                beginControlFlow("if (!$L.inTransaction())", dbVar).apply {
                    addStatement("$L.execSQL($S)", dbVar, "VACUUM")
                }
                endControlFlow()
            }
            endControlFlow()
        }.build()
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

    private fun addDaoImpls(builder: TypeSpec.Builder) {
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

    private fun createDaoGetter(field: FieldSpec, method: DaoMethod): MethodSpec {
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

    private fun createCreateOpenHelper(): MethodSpec {
        val scope = CodeGenScope(this)
        return MethodSpec.methodBuilder("createOpenHelper").apply {
            addModifiers(Modifier.PROTECTED)
            addAnnotation(Override::class.java)
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
