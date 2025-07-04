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
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.ext.RoomMemberNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SQLiteDriverMemberNames
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.vo.Database
import androidx.room.vo.DatabaseView
import androidx.room.vo.Entity
import androidx.room.vo.FtsEntity
import java.util.ArrayDeque

/**
 * The threshold amount of statements in a validateMigration() method before creating additional
 * secondary validate methods.
 */
const val VALIDATE_CHUNK_SIZE = 1000

/** Create an open helper using SupportSQLiteOpenHelperFactory */
class OpenDelegateWriter(val database: Database) {

    private val connectionParamName = "connection"

    fun write(outVar: String, scope: CodeGenScope) {
        scope.builder.apply {
            addLocalVal(outVar, RoomTypeNames.ROOM_OPEN_DELEGATE, "%L", createOpenDelegate(scope))
        }
    }

    private fun createOpenDelegate(scope: CodeGenScope): XTypeSpec {
        return XTypeSpec.anonymousClassBuilder(
                "%L, %S, %S",
                database.version,
                database.identityHash,
                database.legacyIdentityHash,
            )
            .apply {
                superclass(RoomTypeNames.ROOM_OPEN_DELEGATE)
                addFunction(createCreateAllTables())
                addFunction(createDropAllTables())
                addFunction(createOnCreate())
                addFunction(createOnOpen())
                addFunction(createOnPreMigrate())
                addFunction(createOnPostMigrate())
                createValidateMigration(scope.fork()).forEach { addFunction(it) }
            }
            .build()
    }

    private fun createValidateMigration(scope: CodeGenScope): List<XFunSpec> {
        val methodBuilders = mutableMapOf<String, XFunSpec.Builder>()
        val entities = ArrayDeque(database.entities)
        val views = ArrayDeque(database.views)
        while (!entities.isEmpty() || !views.isEmpty()) {
            val isPrimaryMethod = methodBuilders.isEmpty()
            val methodName =
                if (isPrimaryMethod) {
                    "onValidateSchema"
                } else {
                    "onValidateSchema${methodBuilders.size + 1}"
                }
            val validateMethod =
                XFunSpec.builder(
                        name = methodName,
                        visibility =
                            if (isPrimaryMethod) {
                                VisibilityModifier.PUBLIC
                            } else {
                                VisibilityModifier.PRIVATE
                            },
                        isOverride = isPrimaryMethod,
                    )
                    .apply {
                        returns(RoomTypeNames.ROOM_OPEN_DELEGATE_VALIDATION_RESULT)
                        addParameter(connectionParamName, SQLiteDriverTypeNames.CONNECTION)
                        var statementCount = 0
                        while (!entities.isEmpty() && statementCount < VALIDATE_CHUNK_SIZE) {
                            val methodScope = scope.fork()
                            val entity = entities.poll()
                            val validationWriter =
                                when (entity) {
                                    is FtsEntity -> FtsTableInfoValidationWriter(entity)
                                    else -> TableInfoValidationWriter(entity)
                                }
                            validationWriter.write(connectionParamName, methodScope)
                            addCode(methodScope.generate())
                            statementCount += validationWriter.statementCount()
                        }
                        while (!views.isEmpty() && statementCount < VALIDATE_CHUNK_SIZE) {
                            val methodScope = scope.fork()
                            val view = views.poll()
                            val validationWriter = ViewInfoValidationWriter(view)
                            validationWriter.write(connectionParamName, methodScope)
                            addCode(methodScope.generate())
                            statementCount += validationWriter.statementCount()
                        }
                        if (!isPrimaryMethod) {
                            addStatement(
                                "return %L",
                                XCodeBlock.ofNewInstance(
                                    RoomTypeNames.ROOM_OPEN_DELEGATE_VALIDATION_RESULT,
                                    "true, null",
                                ),
                            )
                        }
                    }
            methodBuilders.put(methodName, validateMethod)
        }

        // If there are secondary validate methods then add invocation statements to all of them
        // from the primary method.
        if (methodBuilders.size > 1) {
            val body =
                XCodeBlock.builder()
                    .apply {
                        val resultVar = scope.getTmpVar("_result")
                        addLocalVariable(
                            name = resultVar,
                            typeName = RoomTypeNames.ROOM_OPEN_DELEGATE_VALIDATION_RESULT,
                            isMutable = true,
                        )
                        methodBuilders.keys.drop(1).forEach { methodName ->
                            addStatement("%L = %L(%L)", resultVar, methodName, connectionParamName)
                            beginControlFlow("if (!%L.isValid)", resultVar).apply {
                                addStatement("return %L", resultVar)
                            }
                            endControlFlow()
                        }
                        addStatement(
                            "return %L",
                            XCodeBlock.ofNewInstance(
                                RoomTypeNames.ROOM_OPEN_DELEGATE_VALIDATION_RESULT,
                                "true, null",
                            ),
                        )
                    }
                    .build()
            methodBuilders.values.first().addCode(body)
        } else if (methodBuilders.size == 1) {
            methodBuilders.values
                .first()
                .addStatement(
                    "return %L",
                    XCodeBlock.ofNewInstance(
                        RoomTypeNames.ROOM_OPEN_DELEGATE_VALIDATION_RESULT,
                        "true, null",
                    ),
                )
        }
        return methodBuilders.values.map { it.build() }
    }

    private fun createOnCreate(): XFunSpec {
        return XFunSpec.builder(
                name = "onCreate",
                visibility = VisibilityModifier.PUBLIC,
                isOverride = true,
            )
            .apply { addParameter(connectionParamName, SQLiteDriverTypeNames.CONNECTION) }
            .build()
    }

    private fun createOnOpen(): XFunSpec {
        return XFunSpec.builder(
                name = "onOpen",
                visibility = VisibilityModifier.PUBLIC,
                isOverride = true,
            )
            .apply {
                addParameter(connectionParamName, SQLiteDriverTypeNames.CONNECTION)
                if (database.enableForeignKeys) {
                    addStatement(
                        "%L",
                        XCodeBlock.ofExtensionCall(
                            memberName = SQLiteDriverMemberNames.CONNECTION_EXEC_SQL,
                            receiverVarName = connectionParamName,
                            args = XCodeBlock.of("%S", "PRAGMA foreign_keys = ON"),
                        ),
                    )
                }
                addStatement("internalInitInvalidationTracker(%L)", connectionParamName)
            }
            .build()
    }

    private fun createCreateAllTables(): XFunSpec {
        return XFunSpec.builder(
                name = "createAllTables",
                visibility = VisibilityModifier.PUBLIC,
                isOverride = true,
            )
            .apply {
                addParameter(connectionParamName, SQLiteDriverTypeNames.CONNECTION)
                database.bundle.buildCreateQueries().forEach { createQuery ->
                    addStatement(
                        "%L",
                        XCodeBlock.ofExtensionCall(
                            memberName = SQLiteDriverMemberNames.CONNECTION_EXEC_SQL,
                            receiverVarName = connectionParamName,
                            args = XCodeBlock.of("%S", createQuery),
                        ),
                    )
                }
            }
            .build()
    }

    private fun createDropAllTables(): XFunSpec {
        return XFunSpec.builder(
                name = "dropAllTables",
                visibility = VisibilityModifier.PUBLIC,
                isOverride = true,
            )
            .apply {
                addParameter(connectionParamName, SQLiteDriverTypeNames.CONNECTION)
                database.entities.forEach {
                    addStatement(
                        "%L",
                        XCodeBlock.ofExtensionCall(
                            memberName = SQLiteDriverMemberNames.CONNECTION_EXEC_SQL,
                            receiverVarName = connectionParamName,
                            args = XCodeBlock.of("%S", createDropTableQuery(it)),
                        ),
                    )
                }
                database.views.forEach {
                    addStatement(
                        "%L",
                        XCodeBlock.ofExtensionCall(
                            memberName = SQLiteDriverMemberNames.CONNECTION_EXEC_SQL,
                            receiverVarName = connectionParamName,
                            args = XCodeBlock.of("%S", createDropViewQuery(it)),
                        ),
                    )
                }
            }
            .build()
    }

    private fun createOnPreMigrate(): XFunSpec {
        return XFunSpec.builder(
                name = "onPreMigrate",
                visibility = VisibilityModifier.PUBLIC,
                isOverride = true,
            )
            .apply {
                addParameter(connectionParamName, SQLiteDriverTypeNames.CONNECTION)
                addStatement(
                    "%M(%L)",
                    RoomMemberNames.DB_UTIL_DROP_FTS_SYNC_TRIGGERS,
                    connectionParamName,
                )
            }
            .build()
    }

    private fun createOnPostMigrate(): XFunSpec {
        return XFunSpec.builder(
                name = "onPostMigrate",
                visibility = VisibilityModifier.PUBLIC,
                isOverride = true,
            )
            .apply {
                addParameter(connectionParamName, SQLiteDriverTypeNames.CONNECTION)
                database.entities
                    .filterIsInstance(FtsEntity::class.java)
                    .filter { it.ftsOptions.contentEntity != null }
                    .flatMap { it.contentSyncTriggerCreateQueries }
                    .forEach { syncTriggerQuery ->
                        addStatement(
                            "%L",
                            XCodeBlock.ofExtensionCall(
                                memberName = SQLiteDriverMemberNames.CONNECTION_EXEC_SQL,
                                receiverVarName = connectionParamName,
                                args = XCodeBlock.of("%S", syncTriggerQuery),
                            ),
                        )
                    }
            }
            .build()
    }

    @VisibleForTesting
    fun createTableQuery(entity: Entity): String {
        return entity.createTableQuery
    }

    @VisibleForTesting
    fun createViewQuery(view: DatabaseView): String {
        return view.createViewQuery
    }

    @VisibleForTesting
    fun createDropTableQuery(entity: Entity): String {
        return "DROP TABLE IF EXISTS `${entity.tableName}`"
    }

    @VisibleForTesting
    fun createDropViewQuery(view: DatabaseView): String {
        return "DROP VIEW IF EXISTS `${view.viewName}`"
    }
}
