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

import android.support.annotation.VisibleForTesting
import com.android.support.room.ext.L
import com.android.support.room.ext.N
import com.android.support.room.ext.RoomTypeNames
import com.android.support.room.ext.S
import com.android.support.room.ext.SupportDbTypeNames
import com.android.support.room.ext.T
import com.android.support.room.solver.CodeGenScope
import com.android.support.room.vo.Database
import com.android.support.room.vo.Entity
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC

/**
 * Create an open helper using SupportSQLiteOpenHelperFactory
 */
class SQLiteOpenHelperWriter(val database : Database) {
    fun write(outVar : String, configuration : ParameterSpec, scope: CodeGenScope) {
        scope.builder().apply {
            val sqliteConfigVar = scope.getTmpVar("_sqliteConfig")
            val callbackVar = scope.getTmpVar("_openCallback")
            addStatement("final $T $L = new $T($N, $L, $S)",
                    SupportDbTypeNames.SQLITE_OPEN_HELPER_CALLBACK,
                    callbackVar, RoomTypeNames.OPEN_HELPER, configuration, createOpenCallback(),
                    database.identityHash)
            // build configuration
            addStatement(
                    """
                    final $T $L = $T.builder($N.context)
                    .name($N.name)
                    .version($L)
                    .callback($L)
                    .build()
                    """.trimIndent(),
                    SupportDbTypeNames.SQLITE_OPEN_HELPER_CONFIG, sqliteConfigVar,
                    SupportDbTypeNames.SQLITE_OPEN_HELPER_CONFIG,
                    configuration, configuration, database.version, callbackVar)
            addStatement("final $T $N = $N.sqliteOpenHelperFactory.create($L)",
                    SupportDbTypeNames.SQLITE_OPEN_HELPER, outVar,
                    configuration, sqliteConfigVar)
        }
    }

    private fun createOpenCallback() : TypeSpec {
        return TypeSpec.anonymousClassBuilder("").apply {
            superclass(RoomTypeNames.OPEN_HELPER_DELEGATE)
            addMethod(createCreateAllTables())
            addMethod(createDropAllTables())
            addMethod(createOnOpen())
            addMethod(createValidateMigration())
        }.build()
    }

    private fun createValidateMigration(): MethodSpec {
        return MethodSpec.methodBuilder("validateMigration").apply {
            addModifiers(PROTECTED)
            returns(TypeName.BOOLEAN)
            addParameter(SupportDbTypeNames.DB, "_db")
            addStatement("return true")
        }.build()
    }

    private fun createOnOpen(): MethodSpec {
        return MethodSpec.methodBuilder("onOpen").apply {
            addModifiers(PUBLIC)
            addParameter(SupportDbTypeNames.DB, "_db")
            addStatement("mDatabase = _db")
            addStatement("internalInitInvalidationTracker(_db)")
        }.build()
    }

    private fun createCreateAllTables() : MethodSpec {
        return MethodSpec.methodBuilder("createAllTables").apply {
            addModifiers(PUBLIC)
            addParameter(SupportDbTypeNames.DB, "_db")
            database.bundle.buildCreateQueries().forEach {
                addStatement("_db.execSQL($S)", it)
            }
        }.build()
    }

    private fun createDropAllTables() : MethodSpec {
        return MethodSpec.methodBuilder("dropAllTables").apply {
            addModifiers(PUBLIC)
            addParameter(SupportDbTypeNames.DB, "_db")
            database.entities.forEach {
                addStatement("_db.execSQL($S)", createDropTableQuery(it))
            }
        }.build()
    }

    @VisibleForTesting
    fun createQuery(entity : Entity) : String {
        return entity.createTableQuery
    }

    @VisibleForTesting
    fun createDropTableQuery(entity: Entity) : String {
        return "DROP TABLE IF EXISTS `${entity.tableName}`"
    }
}
