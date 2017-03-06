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
import com.android.support.room.ext.AndroidTypeNames
import com.android.support.room.ext.L
import com.android.support.room.ext.N
import com.android.support.room.ext.RoomTypeNames
import com.android.support.room.ext.S
import com.android.support.room.ext.SupportDbTypeNames
import com.android.support.room.ext.T
import com.android.support.room.ext.typeName
import com.android.support.room.solver.CodeGenScope
import com.android.support.room.vo.Database
import com.android.support.room.vo.Entity
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier.PUBLIC

/**
 * Create an open helper using SupportSQLiteOpenHelperFactory
 */
class SQLiteOpenHelperWriter(val database : Database) {
    companion object {
        // must match the runtime property Room#MASTER_TABLE_NAME
        val MASTER_TABLE_NAME = "room_master_table"
        val MASTER_TABLE_ID_COLUMN = "id"
        val MASTER_TABLE_IDENTITY_HASH_COLUMN = "identity_hash"
        val MASTER_TABLE_ID = 42
    }
    fun write(outVar : String, configuration : ParameterSpec, scope: CodeGenScope) {
        scope.builder().apply {
            val sqliteConfigVar = scope.getTmpVar("_sqliteConfig")
            val callbackVar = scope.getTmpVar("_openCallback")
            addStatement("final $T $L = $L",
                    SupportDbTypeNames.SQLITE_OPEN_HELPER_CALLBACK,
                    callbackVar, createOpenCallback())
            // build configuration
            addStatement(
                    """
                    final $T $L = $T.builder($N.context)
                    .name($N.name)
                    .version($N.version)
                    .callback($L)
                    .build()
                    """.trimIndent(),
                    SupportDbTypeNames.SQLITE_OPEN_HELPER_CONFIG, sqliteConfigVar,
                    SupportDbTypeNames.SQLITE_OPEN_HELPER_CONFIG,
                    configuration, configuration, configuration, callbackVar)
            addStatement("final $T $N = $N.sqliteOpenHelperFactory.create($L)",
                    SupportDbTypeNames.SQLITE_OPEN_HELPER, outVar,
                    configuration, sqliteConfigVar)
        }
    }

    private fun createOpenCallback() : TypeSpec {
        return TypeSpec.anonymousClassBuilder("").apply {
            superclass(SupportDbTypeNames.SQLITE_OPEN_HELPER_CALLBACK)
            addMethod(createOnCreate())
            addMethod(createOnUpgrade())
            addMethod(createOnDowngrade())
            addMethod(createOnOpen())
        }.build()
    }

    private fun createOnOpen(): MethodSpec {
        return MethodSpec.methodBuilder("onOpen").apply {
            addModifiers(PUBLIC)
            addParameter(SupportDbTypeNames.DB, "_db")
            addStatement("String identityHash = \"\"")
            addStatement("$T cursor = _db.rawQuery($S, $T.EMPTY_STRING_ARRAY)",
                    AndroidTypeNames.CURSOR, readIdentityHashQuery(),
                    RoomTypeNames.STRING_UTIL)
            beginControlFlow("try").apply {
                beginControlFlow("if (cursor.moveToFirst())").apply {
                    addStatement("identityHash = cursor.getString(0)")
                }
                endControlFlow()
            }
            nextControlFlow("finally").apply {
                addStatement("cursor.close()")
            }
            endControlFlow()
            beginControlFlow("if(!$S.equals(identityHash))", database.identityHash).apply {
                addStatement("throw new $T($S)", IllegalStateException::class.typeName(),
                        "Room cannot verify the data integrity. Looks like you've changed schema" +
                                " but forgot to update the version number. You can simply fix" +
                                " this by increasing the version number.")
            }
            endControlFlow()
            addStatement("mDatabase = _db")
            addStatement("internalInitInvalidationTracker(_db)")
        }.build()
    }

    private fun MethodSpec.Builder.writeCreateStatements() {
        addStatement("_db.execSQL($S)", createMasterTableQuery())
        addStatement("_db.execSQL($S)", setIdentityHashQuery())
        // this is already called in transaction so no need for a transaction
        database.entities.forEach {
            addStatement("_db.execSQL($S)", createQuery(it))
        }
        database.entities.forEach {
            it.createIndexQueries.forEach {
                addStatement("_db.execSQL($S)", it)
            }
        }
    }

    private fun setIdentityHashQuery(): String {
        return "INSERT OR REPLACE INTO $MASTER_TABLE_NAME VALUES($MASTER_TABLE_ID," +
                "\"${database.identityHash}\")"
    }

    private fun readIdentityHashQuery() : String {
        return "SELECT $MASTER_TABLE_IDENTITY_HASH_COLUMN FROM $MASTER_TABLE_NAME WHERE" +
                " $MASTER_TABLE_ID_COLUMN = $MASTER_TABLE_ID LIMIT 1"
    }

    private fun createMasterTableQuery() : String {
        return "CREATE TABLE IF NOT EXISTS `$MASTER_TABLE_NAME`(" +
                "$MASTER_TABLE_ID_COLUMN INTEGER PRIMARY KEY," +
                "$MASTER_TABLE_IDENTITY_HASH_COLUMN TEXT)"
    }

    private fun dropMasterTableQuery() : String {
        return "DROP TABLE IF EXISTS `$MASTER_TABLE_NAME`"
    }

    private fun createOnCreate() : MethodSpec {
        return MethodSpec.methodBuilder("onCreate").apply {
            addModifiers(PUBLIC)
            addParameter(SupportDbTypeNames.DB, "_db")
            writeCreateStatements()
        }.build()
    }

    private fun createOnUpgrade() : MethodSpec {
        return MethodSpec.methodBuilder("onUpgrade").apply {
            addModifiers(PUBLIC)
            addParameter(SupportDbTypeNames.DB, "_db")
            addParameter(TypeName.INT, "_oldVersion")
            addParameter(TypeName.INT, "_newVersion")
            database.entities.forEach {
                addStatement("_db.execSQL($S)", createDropTableQuery(it))
            }
            writeCreateStatements()
        }.build()
    }

    private fun createOnDowngrade() : MethodSpec {
        return MethodSpec.methodBuilder("onDowngrade").apply {
            addModifiers(PUBLIC)
            addParameter(SupportDbTypeNames.DB, "_db")
            addParameter(TypeName.INT, "_oldVersion")
            addParameter(TypeName.INT, "_newVersion")
            // TODO better handle this
            addStatement("onUpgrade(_db, _oldVersion, _newVersion)")
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
