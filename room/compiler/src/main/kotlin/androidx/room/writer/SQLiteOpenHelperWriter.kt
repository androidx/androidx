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

import androidx.annotation.VisibleForTesting
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.SupportDbTypeNames
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import androidx.room.vo.Database
import androidx.room.vo.Entity
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC

/**
 * Create an open helper using SupportSQLiteOpenHelperFactory
 */
class SQLiteOpenHelperWriter(val database: Database) {
    fun write(outVar: String, configuration: ParameterSpec, scope: CodeGenScope) {
        scope.builder().apply {
            val sqliteConfigVar = scope.getTmpVar("_sqliteConfig")
            val callbackVar = scope.getTmpVar("_openCallback")
            addStatement("final $T $L = new $T($N, $L, $S, $S)",
                    SupportDbTypeNames.SQLITE_OPEN_HELPER_CALLBACK,
                    callbackVar, RoomTypeNames.OPEN_HELPER, configuration,
                    createOpenCallback(scope), database.identityHash, database.legacyIdentityHash)
            // build configuration
            addStatement(
                    """
                    final $T $L = $T.builder($N.context)
                    .name($N.name)
                    .callback($L)
                    .build()
                    """.trimIndent(),
                    SupportDbTypeNames.SQLITE_OPEN_HELPER_CONFIG, sqliteConfigVar,
                    SupportDbTypeNames.SQLITE_OPEN_HELPER_CONFIG,
                    configuration, configuration, callbackVar)
            addStatement("final $T $N = $N.sqliteOpenHelperFactory.create($L)",
                    SupportDbTypeNames.SQLITE_OPEN_HELPER, outVar,
                    configuration, sqliteConfigVar)
        }
    }

    private fun createOpenCallback(scope: CodeGenScope): TypeSpec {
        return TypeSpec.anonymousClassBuilder(L, database.version).apply {
            superclass(RoomTypeNames.OPEN_HELPER_DELEGATE)
            addMethod(createCreateAllTables())
            addMethod(createDropAllTables())
            addMethod(createOnCreate(scope.fork()))
            addMethod(createOnOpen(scope.fork()))
            addMethod(createValidateMigration(scope.fork()))
        }.build()
    }

    private fun createValidateMigration(scope: CodeGenScope): MethodSpec {
        return MethodSpec.methodBuilder("validateMigration").apply {
            addModifiers(PROTECTED)
            addAnnotation(Override::class.java)
            val dbParam = ParameterSpec.builder(SupportDbTypeNames.DB, "_db").build()
            addParameter(dbParam)
            database.entities.forEach { entity ->
                val methodScope = scope.fork()
                TableInfoValidationWriter(entity).write(dbParam, methodScope)
                addCode(methodScope.builder().build())
            }
        }.build()
    }

    private fun createOnCreate(scope: CodeGenScope): MethodSpec {
        return MethodSpec.methodBuilder("onCreate").apply {
            addModifiers(PROTECTED)
            addAnnotation(Override::class.java)
            addParameter(SupportDbTypeNames.DB, "_db")
            invokeCallbacks(scope, "onCreate")
        }.build()
    }

    private fun createOnOpen(scope: CodeGenScope): MethodSpec {
        return MethodSpec.methodBuilder("onOpen").apply {
            addModifiers(PUBLIC)
            addAnnotation(Override::class.java)
            addParameter(SupportDbTypeNames.DB, "_db")
            addStatement("mDatabase = _db")
            if (database.enableForeignKeys) {
                addStatement("_db.execSQL($S)", "PRAGMA foreign_keys = ON")
            }
            addStatement("internalInitInvalidationTracker(_db)")
            invokeCallbacks(scope, "onOpen")
        }.build()
    }

    private fun createCreateAllTables(): MethodSpec {
        return MethodSpec.methodBuilder("createAllTables").apply {
            addModifiers(PUBLIC)
            addAnnotation(Override::class.java)
            addParameter(SupportDbTypeNames.DB, "_db")
            database.bundle.buildCreateQueries().forEach {
                addStatement("_db.execSQL($S)", it)
            }
        }.build()
    }

    private fun createDropAllTables(): MethodSpec {
        return MethodSpec.methodBuilder("dropAllTables").apply {
            addModifiers(PUBLIC)
            addAnnotation(Override::class.java)
            addParameter(SupportDbTypeNames.DB, "_db")
            database.entities.forEach {
                addStatement("_db.execSQL($S)", createDropTableQuery(it))
            }
        }.build()
    }

    private fun MethodSpec.Builder.invokeCallbacks(scope: CodeGenScope, methodName: String) {
        val iVar = scope.getTmpVar("_i")
        val sizeVar = scope.getTmpVar("_size")
        beginControlFlow("if (mCallbacks != null)").apply {
            beginControlFlow("for (int $N = 0, $N = mCallbacks.size(); $N < $N; $N++)",
                    iVar, sizeVar, iVar, sizeVar, iVar).apply {
                addStatement("mCallbacks.get($N).$N(_db)", iVar, methodName)
            }
            endControlFlow()
        }
        endControlFlow()
    }

    @VisibleForTesting
    fun createQuery(entity: Entity): String {
        return entity.createTableQuery
    }

    @VisibleForTesting
    fun createDropTableQuery(entity: Entity): String {
        return "DROP TABLE IF EXISTS `${entity.tableName}`"
    }
}
