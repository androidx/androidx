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
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.addLocalVal
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.beginForEachControlFlow
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomMemberNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SupportDbTypeNames
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

/**
 * Create an open helper using SupportSQLiteOpenHelperFactory
 */
class SQLiteOpenHelperWriter(val database: Database) {

    private val dbParamName = "db"

    fun write(outVar: String, configParamName: String, scope: CodeGenScope) {
        scope.builder.apply {
            val sqliteConfigVar = scope.getTmpVar("_sqliteConfig")
            val callbackVar = scope.getTmpVar("_openCallback")
            addLocalVariable(
                name = callbackVar,
                typeName = SupportDbTypeNames.SQLITE_OPEN_HELPER_CALLBACK,
                assignExpr = XCodeBlock.ofNewInstance(
                    language,
                    RoomTypeNames.OPEN_HELPER,
                    "%L, %L, %S, %S",
                    configParamName,
                    createOpenCallback(scope),
                    database.identityHash,
                    database.legacyIdentityHash
                )
            )
            // build configuration
            addLocalVal(
                sqliteConfigVar,
                SupportDbTypeNames.SQLITE_OPEN_HELPER_CONFIG,
                "%T.builder(%L.context).name(%L.name).callback(%L).build()",
                SupportDbTypeNames.SQLITE_OPEN_HELPER_CONFIG,
                configParamName,
                configParamName,
                callbackVar
            )
            addLocalVal(
                outVar,
                SupportDbTypeNames.SQLITE_OPEN_HELPER,
                "%L.sqliteOpenHelperFactory.create(%L)",
                configParamName,
                sqliteConfigVar
            )
        }
    }

    private fun createOpenCallback(scope: CodeGenScope): XTypeSpec {
        return XTypeSpec.anonymousClassBuilder(
            scope.language, "%L", database.version
        ).apply {
            superclass(RoomTypeNames.OPEN_HELPER_DELEGATE)
            addFunction(createCreateAllTables(scope))
            addFunction(createDropAllTables(scope.fork()))
            addFunction(createOnCreate(scope.fork()))
            addFunction(createOnOpen(scope.fork()))
            addFunction(createOnPreMigrate(scope))
            addFunction(createOnPostMigrate(scope))
            createValidateMigration(scope.fork()).forEach {
                addFunction(it)
            }
        }.build()
    }

    private fun createValidateMigration(scope: CodeGenScope): List<XFunSpec> {
        val methodBuilders = mutableListOf<XFunSpec.Builder>()
        val entities = ArrayDeque(database.entities)
        val views = ArrayDeque(database.views)
        while (!entities.isEmpty() || !views.isEmpty()) {
            val isPrimaryMethod = methodBuilders.isEmpty()
            val methodName = if (isPrimaryMethod) {
                "onValidateSchema"
            } else {
                "onValidateSchema${methodBuilders.size + 1}"
            }
            val validateMethod = XFunSpec.builder(
                language = scope.language,
                name = methodName,
                visibility = if (isPrimaryMethod) {
                    VisibilityModifier.PUBLIC
                } else {
                    VisibilityModifier.PRIVATE
                },
                isOverride = isPrimaryMethod
            ).apply {
                returns(RoomTypeNames.OPEN_HELPER_VALIDATION_RESULT)
                addParameter(SupportDbTypeNames.DB, dbParamName)
                var statementCount = 0
                while (!entities.isEmpty() && statementCount < VALIDATE_CHUNK_SIZE) {
                    val methodScope = scope.fork()
                    val entity = entities.poll()
                    val validationWriter = when (entity) {
                        is FtsEntity -> FtsTableInfoValidationWriter(entity)
                        else -> TableInfoValidationWriter(entity)
                    }
                    validationWriter.write(dbParamName, methodScope)
                    addCode(methodScope.generate())
                    statementCount += validationWriter.statementCount()
                }
                while (!views.isEmpty() && statementCount < VALIDATE_CHUNK_SIZE) {
                    val methodScope = scope.fork()
                    val view = views.poll()
                    val validationWriter = ViewInfoValidationWriter(view)
                    validationWriter.write(dbParamName, methodScope)
                    addCode(methodScope.generate())
                    statementCount += validationWriter.statementCount()
                }
                if (!isPrimaryMethod) {
                    addStatement(
                        "return %L",
                        XCodeBlock.ofNewInstance(
                            scope.language,
                            RoomTypeNames.OPEN_HELPER_VALIDATION_RESULT,
                            "true, null"
                        )
                    )
                }
            }
            methodBuilders.add(validateMethod)
        }

        // If there are secondary validate methods then add invocation statements to all of them
        // from the primary method.
        if (methodBuilders.size > 1) {
            val body = XCodeBlock.builder(scope.language).apply {
                val resultVar = scope.getTmpVar("_result")
                addLocalVariable(
                    name = resultVar,
                    typeName = RoomTypeNames.OPEN_HELPER_VALIDATION_RESULT,
                    isMutable = true
                )
                methodBuilders.drop(1).forEach {
                    addStatement("%L = %L(%L)", resultVar, it.name, dbParamName)
                    beginControlFlow("if (!%L.isValid)", resultVar).apply {
                        addStatement("return %L", resultVar)
                    }
                    endControlFlow()
                }
                addStatement(
                    "return %L",
                    XCodeBlock.ofNewInstance(
                        scope.language,
                        RoomTypeNames.OPEN_HELPER_VALIDATION_RESULT,
                        "true, null"
                    )
                )
            }.build()
            methodBuilders.first().addCode(body)
        } else if (methodBuilders.size == 1) {
            methodBuilders.first().addStatement(
                "return %L",
                XCodeBlock.ofNewInstance(
                    scope.language,
                    RoomTypeNames.OPEN_HELPER_VALIDATION_RESULT,
                    "true, null"
                )
            )
        }
        return methodBuilders.map { it.build() }
    }

    private fun createOnCreate(scope: CodeGenScope): XFunSpec {
        return XFunSpec.builder(
            language = scope.language,
            name = "onCreate",
            visibility = VisibilityModifier.PUBLIC,
            isOverride = true
        ).apply {
            addParameter(SupportDbTypeNames.DB, dbParamName)
            addCode(createInvokeCallbacksCode(scope, "onCreate"))
        }.build()
    }

    private fun createOnOpen(scope: CodeGenScope): XFunSpec {
        return XFunSpec.builder(
            language = scope.language,
            name = "onOpen",
            visibility = VisibilityModifier.PUBLIC,
            isOverride = true
        ).apply {
            addParameter(SupportDbTypeNames.DB, dbParamName)
            addStatement("mDatabase = %L", dbParamName)
            if (database.enableForeignKeys) {
                addStatement("%L.execSQL(%S)", dbParamName, "PRAGMA foreign_keys = ON")
            }
            addStatement("internalInitInvalidationTracker(%L)", dbParamName)
            addCode(createInvokeCallbacksCode(scope, "onOpen"))
        }.build()
    }

    private fun createCreateAllTables(scope: CodeGenScope): XFunSpec {
        return XFunSpec.builder(
            language = scope.language,
            name = "createAllTables",
            visibility = VisibilityModifier.PUBLIC,
            isOverride = true
        ).apply {
            addParameter(SupportDbTypeNames.DB, dbParamName)
            database.bundle.buildCreateQueries().forEach {
                addStatement("%L.execSQL(%S)", dbParamName, it)
            }
        }.build()
    }

    private fun createDropAllTables(scope: CodeGenScope): XFunSpec {
        return XFunSpec.builder(
            language = scope.language,
            name = "dropAllTables",
            visibility = VisibilityModifier.PUBLIC,
            isOverride = true
        ).apply {
            addParameter(SupportDbTypeNames.DB, dbParamName)
            database.entities.forEach {
                addStatement("%L.execSQL(%S)", dbParamName, createDropTableQuery(it))
            }
            database.views.forEach {
                addStatement("%L.execSQL(%S)", dbParamName, createDropViewQuery(it))
            }
            addCode(createInvokeCallbacksCode(scope, "onDestructiveMigration"))
        }.build()
    }

    private fun createOnPreMigrate(scope: CodeGenScope): XFunSpec {
        return XFunSpec.builder(
            language = scope.language,
            name = "onPreMigrate",
            visibility = VisibilityModifier.PUBLIC,
            isOverride = true
        ).apply {
            addParameter(SupportDbTypeNames.DB, dbParamName)
            addStatement("%M(%L)", RoomMemberNames.DB_UTIL_DROP_FTS_SYNC_TRIGGERS, dbParamName)
        }.build()
    }

    private fun createOnPostMigrate(scope: CodeGenScope): XFunSpec {
        return XFunSpec.builder(
            language = scope.language,
            name = "onPostMigrate",
            visibility = VisibilityModifier.PUBLIC,
            isOverride = true
        ).apply {
            addParameter(SupportDbTypeNames.DB, dbParamName)
            database.entities.filterIsInstance(FtsEntity::class.java)
                .filter { it.ftsOptions.contentEntity != null }
                .flatMap { it.contentSyncTriggerCreateQueries }
                .forEach { syncTriggerQuery ->
                    addStatement("%L.execSQL(%S)", dbParamName, syncTriggerQuery)
                }
        }.build()
    }

    private fun createInvokeCallbacksCode(scope: CodeGenScope, methodName: String): XCodeBlock {
        val localCallbackListVarName = scope.getTmpVar("_callbacks")
        val callbackVarName = scope.getTmpVar("_callback")
        return XCodeBlock.builder(scope.language).apply {
            addLocalVal(
                localCallbackListVarName,
                CommonTypeNames.LIST.parametrizedBy(
                    // For Kotlin, the variance is redundant, but for Java, due to `mCallbacks`
                    // not having @JvmSuppressWildcards, we use a wildcard name.
                    if (language == CodeLanguage.KOTLIN) {
                        RoomTypeNames.ROOM_DB_CALLBACK
                    } else {
                        XTypeName.getProducerExtendsName(RoomTypeNames.ROOM_DB_CALLBACK)
                    }
                ).copy(nullable = true),
                "mCallbacks"
            )
            beginControlFlow("if (%L != null)", localCallbackListVarName).apply {
                beginForEachControlFlow(
                    itemVarName = callbackVarName,
                    typeName = RoomTypeNames.ROOM_DB_CALLBACK,
                    iteratorVarName = localCallbackListVarName
                ).apply {
                    addStatement("%L.%L(%L)", callbackVarName, methodName, dbParamName)
                }
                endControlFlow()
            }
            endControlFlow()
        }.build()
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
