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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.addLocalVal
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XPropertySpec.Companion.apply
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.XTypeSpec.Builder.Companion.addOriginatingElement
import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SupportDbTypeNames
import androidx.room.ext.decapitalize
import androidx.room.ext.stripNonJava
import androidx.room.solver.CodeGenScope
import androidx.room.vo.DaoMethod
import androidx.room.vo.Database
import java.util.Locale
import javax.lang.model.element.Modifier

/**
 * Writes implementation of classes that were annotated with @Database.
 */
class DatabaseWriter(
    val database: Database,
    codeLanguage: CodeLanguage
) : TypeWriter(codeLanguage) {
    override fun createTypeSpecBuilder(): XTypeSpec.Builder {
        return XTypeSpec.classBuilder(codeLanguage, database.implTypeName).apply {
            addOriginatingElement(database.element)
            superclass(database.typeName)
            setVisibility(
                if (database.element.isInternal()) {
                    VisibilityModifier.INTERNAL
                } else {
                    VisibilityModifier.PUBLIC
                }
            )
            addFunction(createCreateOpenHelper())
            addFunction(createCreateInvalidationTracker())
            addFunction(createClearAllTables())
            addFunction(createCreateTypeConvertersMap())
            addFunction(createCreateAutoMigrationSpecsSet())
            addFunction(getAutoMigrations())
            addDaoImpls(this)
        }
    }

    private fun createCreateTypeConvertersMap(): XFunSpec {
        val scope = CodeGenScope(this)
        val classOfAnyTypeName = CommonTypeNames.JAVA_CLASS.parametrizedBy(
            XTypeName.getProducerExtendsName(KotlinTypeNames.ANY)
        )
        val typeConvertersTypeName = CommonTypeNames.HASH_MAP.parametrizedBy(
            classOfAnyTypeName,
            CommonTypeNames.LIST.parametrizedBy(classOfAnyTypeName)
        )
        val body = XCodeBlock.builder(codeLanguage).apply {
            val typeConvertersVar = scope.getTmpVar("_typeConvertersMap")
            addLocalVariable(
                name = typeConvertersVar,
                typeName = typeConvertersTypeName,
                assignExpr = XCodeBlock.ofNewInstance(codeLanguage, typeConvertersTypeName)
            )
            database.daoMethods.forEach {
                addStatement(
                    "%L.put(%L, %T.%L())",
                    typeConvertersVar,
                    XCodeBlock.ofJavaClassLiteral(codeLanguage, it.dao.typeName),
                    it.dao.implTypeName,
                    DaoWriter.GET_LIST_OF_TYPE_CONVERTERS_METHOD
                )
            }
            addStatement("return %L", typeConvertersVar)
        }.build()
        return XFunSpec.builder(
            language = codeLanguage,
            name = "getRequiredTypeConverters",
            visibility = VisibilityModifier.PROTECTED,
            isOverride = true
        ).apply {
            returns(
                CommonTypeNames.MAP.parametrizedBy(
                    classOfAnyTypeName,
                    CommonTypeNames.LIST.parametrizedBy(classOfAnyTypeName)
                )
            )
            addCode(body)
        }.build()
    }

    private fun createCreateAutoMigrationSpecsSet(): XFunSpec {
        val scope = CodeGenScope(this)
        val classOfAutoMigrationSpecTypeName = CommonTypeNames.JAVA_CLASS.parametrizedBy(
            XTypeName.getProducerExtendsName(RoomTypeNames.AUTO_MIGRATION_SPEC)
        )
        val autoMigrationSpecsTypeName =
            CommonTypeNames.HASH_SET.parametrizedBy(classOfAutoMigrationSpecTypeName)
        val body = XCodeBlock.builder(codeLanguage).apply {
            val autoMigrationSpecsVar = scope.getTmpVar("_autoMigrationSpecsSet")
            addLocalVariable(
                name = autoMigrationSpecsVar,
                typeName = autoMigrationSpecsTypeName,
                assignExpr = XCodeBlock.ofNewInstance(codeLanguage, autoMigrationSpecsTypeName)
            )
            database.autoMigrations.filter { it.isSpecProvided }.map { autoMigration ->
                val specClassName = checkNotNull(autoMigration.specClassName)
                addStatement(
                    "%L.add(%L)",
                    autoMigrationSpecsVar,
                    XCodeBlock.ofJavaClassLiteral(codeLanguage, specClassName)
                )
            }
            addStatement("return %L", autoMigrationSpecsVar)
        }.build()
        return XFunSpec.builder(
            language = codeLanguage,
            name = "getRequiredAutoMigrationSpecs",
            visibility = VisibilityModifier.PUBLIC,
            isOverride = true,
        ).apply {
            returns(CommonTypeNames.SET.parametrizedBy(classOfAutoMigrationSpecTypeName))
            addCode(body)
        }.build()
    }

    private fun createClearAllTables(): XFunSpec {
        val scope = CodeGenScope(this)
        val body = XCodeBlock.builder(codeLanguage).apply {
            addStatement("super.assertNotMainThread()")
            val dbVar = scope.getTmpVar("_db")
            addLocalVal(
                dbVar,
                SupportDbTypeNames.DB,
                when (language) {
                    CodeLanguage.JAVA -> "super.getOpenHelper().getWritableDatabase()"
                    CodeLanguage.KOTLIN -> "super.openHelper.writableDatabase"
                }
            )
            val deferVar = scope.getTmpVar("_supportsDeferForeignKeys")
            if (database.enableForeignKeys) {
                addLocalVal(
                    deferVar,
                    XTypeName.PRIMITIVE_BOOLEAN,
                    "%L.VERSION.SDK_INT >= %L.VERSION_CODES.LOLLIPOP",
                    AndroidTypeNames.BUILD,
                    AndroidTypeNames.BUILD
                )
            }
            beginControlFlow("try").apply {
                if (database.enableForeignKeys) {
                    beginControlFlow("if (!%L)", deferVar).apply {
                        addStatement("%L.execSQL(%S)", dbVar, "PRAGMA foreign_keys = FALSE")
                    }
                    endControlFlow()
                }
                addStatement("super.beginTransaction()")
                if (database.enableForeignKeys) {
                    beginControlFlow("if (%L)", deferVar).apply {
                        addStatement("%L.execSQL(%S)", dbVar, "PRAGMA defer_foreign_keys = TRUE")
                    }
                    endControlFlow()
                }
                database.entities.sortedWith(EntityDeleteComparator()).forEach {
                    addStatement("%L.execSQL(%S)", dbVar, "DELETE FROM `${it.tableName}`")
                }
                addStatement("super.setTransactionSuccessful()")
            }
            nextControlFlow("finally").apply {
                addStatement("super.endTransaction()")
                if (database.enableForeignKeys) {
                    beginControlFlow("if (!%L)", deferVar).apply {
                        addStatement("%L.execSQL(%S)", dbVar, "PRAGMA foreign_keys = TRUE")
                    }
                    endControlFlow()
                }
                addStatement("%L.query(%S).close()", dbVar, "PRAGMA wal_checkpoint(FULL)")
                beginControlFlow("if (!%L.inTransaction())", dbVar).apply {
                    addStatement("%L.execSQL(%S)", dbVar, "VACUUM")
                }
                endControlFlow()
            }
            endControlFlow()
        }.build()
        return XFunSpec.builder(
            language = codeLanguage,
            name = "clearAllTables",
            visibility = VisibilityModifier.PUBLIC,
            isOverride = true
        ).apply {
            addCode(body)
        }.build()
    }

    private fun createCreateInvalidationTracker(): XFunSpec {
        val scope = CodeGenScope(this)
        val body = XCodeBlock.builder(codeLanguage).apply {
            val shadowTablesVar = "_shadowTablesMap"
            val shadowTablesTypeName = CommonTypeNames.HASH_MAP.parametrizedBy(
                CommonTypeNames.STRING, CommonTypeNames.STRING
            )
            val tableNames = database.entities.joinToString(",") {
                "\"${it.tableName}\""
            }
            val shadowTableNames = database.entities.filter {
                it.shadowTableName != null
            }.map {
                it.tableName to it.shadowTableName
            }
            addLocalVariable(
                name = shadowTablesVar,
                typeName = shadowTablesTypeName,
                assignExpr = XCodeBlock.ofNewInstance(
                    codeLanguage,
                    shadowTablesTypeName,
                    "%L",
                    shadowTableNames.size
                )
            )
            shadowTableNames.forEach { (tableName, shadowTableName) ->
                addStatement("%L.put(%S, %S)", shadowTablesVar, tableName, shadowTableName)
            }
            val viewTablesVar = scope.getTmpVar("_viewTables")
            val tablesType = CommonTypeNames.HASH_SET.parametrizedBy(CommonTypeNames.STRING)
            val viewTablesType = CommonTypeNames.HASH_MAP.parametrizedBy(
                CommonTypeNames.STRING,
                CommonTypeNames.SET.parametrizedBy(CommonTypeNames.STRING)
            )
            addLocalVariable(
                name = viewTablesVar,
                typeName = viewTablesType,
                assignExpr = XCodeBlock.ofNewInstance(
                    codeLanguage,
                    viewTablesType,
                    "%L", database.views.size
                )
            )
            for (view in database.views) {
                val tablesVar = scope.getTmpVar("_tables")
                addLocalVariable(
                    name = tablesVar,
                    typeName = tablesType,
                    assignExpr = XCodeBlock.ofNewInstance(
                        codeLanguage,
                        tablesType,
                        "%L", view.tables.size
                    )
                )
                for (table in view.tables) {
                    addStatement("%L.add(%S)", tablesVar, table)
                }
                addStatement(
                    "%L.put(%S, %L)",
                    viewTablesVar, view.viewName.lowercase(Locale.US), tablesVar
                )
            }
            addStatement(
                "return %L",
                XCodeBlock.ofNewInstance(
                    codeLanguage,
                    RoomTypeNames.INVALIDATION_TRACKER,
                    "this, %L, %L, %L",
                    shadowTablesVar, viewTablesVar, tableNames
                )
            )
        }.build()
        return XFunSpec.builder(
            language = codeLanguage,
            name = "createInvalidationTracker",
            visibility = VisibilityModifier.PROTECTED,
            isOverride = true
        ).apply {
            returns(RoomTypeNames.INVALIDATION_TRACKER)
            addCode(body)
        }.build()
    }

    private fun addDaoImpls(builder: XTypeSpec.Builder) {
        val scope = CodeGenScope(this)
        database.daoMethods.forEach { method ->
            val name = method.dao.typeName.simpleNames.first()
                .decapitalize(Locale.US)
                .stripNonJava()
            val privateDaoProperty = XPropertySpec.builder(
                language = codeLanguage,
                name = scope.getTmpVar("_$name"),
                typeName = if (codeLanguage == CodeLanguage.KOTLIN) {
                    KotlinTypeNames.LAZY.parametrizedBy(method.dao.typeName)
                } else {
                    method.dao.typeName
                },
                visibility = VisibilityModifier.PRIVATE,
                isMutable = codeLanguage == CodeLanguage.JAVA
            ).apply {
                // For Kotlin we rely on kotlin.Lazy while for Java we'll memoize the dao impl in
                // the getter.
                if (language == CodeLanguage.KOTLIN) {
                    val lazyInit = XCodeBlock.builder(language).apply {
                        beginControlFlow("lazy")
                        addStatement(
                            "%L",
                            XCodeBlock.ofNewInstance(language, method.dao.implTypeName, "this")
                        )
                        endControlFlow()
                    }.build()
                    initializer(lazyInit)
                }
            }.apply(
                javaFieldBuilder = {
                    // The volatile modifier is needed since in Java the memoization is generated.
                    addModifiers(Modifier.VOLATILE)
                },
                kotlinPropertyBuilder = { }
            ).build()
            builder.addProperty(privateDaoProperty)
            builder.addFunction(createDaoGetter(method, privateDaoProperty))
        }
    }

    private fun createDaoGetter(method: DaoMethod, daoProperty: XPropertySpec): XFunSpec {
        val body = XCodeBlock.builder(codeLanguage).apply {
            // For Java we implement the memoization logic in the Dao getter, meanwhile for Kotlin
            // we rely on kotlin.Lazy to the getter just delegates to it.
            when (codeLanguage) {
                CodeLanguage.JAVA -> {
                    beginControlFlow("if (%N != null)", daoProperty).apply {
                        addStatement("return %N", daoProperty)
                    }
                    nextControlFlow("else").apply {
                        beginControlFlow("synchronized(this)").apply {
                            beginControlFlow("if(%N == null)", daoProperty).apply {
                                addStatement(
                                    "%N = %L",
                                    daoProperty,
                                    XCodeBlock.ofNewInstance(
                                        language,
                                        method.dao.implTypeName,
                                        "this"
                                    )

                                )
                            }
                            endControlFlow()
                            addStatement("return %N", daoProperty)
                        }
                        endControlFlow()
                    }
                    endControlFlow()
                }
                CodeLanguage.KOTLIN -> {
                    addStatement("return %N.value", daoProperty)
                }
            }
        }
        return XFunSpec.overridingBuilder(
            language = codeLanguage,
            element = method.element,
            owner = database.element.type
        ).apply {
            addCode(body.build())
        }.build()
    }

    private fun createCreateOpenHelper(): XFunSpec {
        val scope = CodeGenScope(this)
        val configParamName = "config"
        val body = XCodeBlock.builder(codeLanguage).apply {
            val openHelperVar = scope.getTmpVar("_helper")
            val openHelperCode = scope.fork()
            SQLiteOpenHelperWriter(database)
                .write(openHelperVar, configParamName, openHelperCode)
            add(openHelperCode.generate())
            addStatement("return %L", openHelperVar)
        }.build()
        return XFunSpec.builder(
            language = codeLanguage,
            name = "createOpenHelper",
            visibility = VisibilityModifier.PROTECTED,
            isOverride = true,
        ).apply {
            returns(SupportDbTypeNames.SQLITE_OPEN_HELPER)
            addParameter(RoomTypeNames.ROOM_DB_CONFIG, configParamName)
            addCode(body)
        }.build()
    }

    private fun getAutoMigrations(): XFunSpec {
        val scope = CodeGenScope(this)
        val classOfAutoMigrationSpecTypeName = CommonTypeNames.JAVA_CLASS.parametrizedBy(
            XTypeName.getProducerExtendsName(RoomTypeNames.AUTO_MIGRATION_SPEC)
        )
        val autoMigrationsListTypeName =
            CommonTypeNames.ARRAY_LIST.parametrizedBy(RoomTypeNames.MIGRATION)
        val specsMapParamName = "autoMigrationSpecs"
        val body = XCodeBlock.builder(codeLanguage).apply {
            val listVar = scope.getTmpVar("_autoMigrations")
            addLocalVariable(
                name = listVar,
                typeName = CommonTypeNames.MUTABLE_LIST.parametrizedBy(RoomTypeNames.MIGRATION),
                assignExpr = XCodeBlock.ofNewInstance(codeLanguage, autoMigrationsListTypeName)
            )
            database.autoMigrations.forEach { autoMigrationResult ->
                val implTypeName =
                    autoMigrationResult.getImplTypeName(database.typeName)
                val newInstanceCode = if (autoMigrationResult.isSpecProvided) {
                    val specClassName = checkNotNull(autoMigrationResult.specClassName)
                    // For Kotlin use getValue() as the Map's values are never null.
                    val getFunction = when (language) {
                        CodeLanguage.JAVA -> "get"
                        CodeLanguage.KOTLIN -> "getValue"
                    }
                    XCodeBlock.ofNewInstance(
                        language,
                        implTypeName,
                        "%L.%L(%L)",
                        specsMapParamName,
                        getFunction,
                        XCodeBlock.ofJavaClassLiteral(language, specClassName)
                    )
                } else {
                    XCodeBlock.ofNewInstance(language, implTypeName)
                }
                addStatement("%L.add(%L)", listVar, newInstanceCode)
            }
            addStatement("return %L", listVar)
        }.build()
        return XFunSpec.builder(
            language = codeLanguage,
            name = "getAutoMigrations",
            visibility = VisibilityModifier.PUBLIC,
            isOverride = true,
        ).apply {
            returns(CommonTypeNames.LIST.parametrizedBy(RoomTypeNames.MIGRATION))
            addParameter(
                CommonTypeNames.MAP.parametrizedBy(
                    classOfAutoMigrationSpecTypeName,
                    RoomTypeNames.AUTO_MIGRATION_SPEC,
                ),
                specsMapParamName
            )
            addCode(body)
        }.build()
    }
}
