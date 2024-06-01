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
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XPropertySpec.Builder.Companion.apply
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.XTypeSpec.Builder.Companion.addOriginatingElement
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.KotlinCollectionMemberNames
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.decapitalize
import androidx.room.ext.stripNonJava
import androidx.room.solver.CodeGenScope
import androidx.room.vo.DaoMethod
import androidx.room.vo.Database
import java.util.Locale
import javax.lang.model.element.Modifier

/** Writes implementation of classes that were annotated with @Database. */
class DatabaseWriter(
    val database: Database,
    writerContext: WriterContext,
) : TypeWriter(writerContext) {
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
            addFunction(createOpenDelegate())
            addFunction(createCreateInvalidationTracker())
            if (database.overrideClearAllTables) {
                addFunction(createClearAllTables())
            }
            addFunction(createCreateTypeConvertersMap())
            addFunction(createCreateAutoMigrationSpecsSet())
            addFunction(createGetAutoMigrations())
            addDaoImpls(this)
        }
    }

    private fun createCreateTypeConvertersMap(): XFunSpec {
        val scope = CodeGenScope(this)
        val classOfAnyTypeName =
            when (codeLanguage) {
                CodeLanguage.JAVA -> CommonTypeNames.JAVA_CLASS
                CodeLanguage.KOTLIN -> CommonTypeNames.KOTLIN_CLASS
            }.parametrizedBy(XTypeName.ANY_WILDCARD)
        val typeConvertersTypeName =
            CommonTypeNames.MUTABLE_MAP.parametrizedBy(
                classOfAnyTypeName,
                CommonTypeNames.LIST.parametrizedBy(classOfAnyTypeName)
            )
        val body =
            XCodeBlock.builder(codeLanguage)
                .apply {
                    val typeConvertersVar = scope.getTmpVar("_typeConvertersMap")
                    when (language) {
                        CodeLanguage.JAVA ->
                            addLocalVariable(
                                name = typeConvertersVar,
                                typeName = typeConvertersTypeName,
                                assignExpr =
                                    XCodeBlock.ofNewInstance(
                                        codeLanguage,
                                        CommonTypeNames.HASH_MAP.parametrizedBy(
                                            classOfAnyTypeName,
                                            CommonTypeNames.LIST.parametrizedBy(classOfAnyTypeName)
                                        )
                                    )
                            )
                        CodeLanguage.KOTLIN ->
                            addLocalVal(
                                typeConvertersVar,
                                typeConvertersTypeName,
                                "%M()",
                                KotlinCollectionMemberNames.MUTABLE_MAP_OF
                            )
                    }
                    database.daoMethods.forEach {
                        addStatement(
                            "%L.put(%L, %T.%L())",
                            typeConvertersVar,
                            when (language) {
                                CodeLanguage.JAVA ->
                                    XCodeBlock.ofJavaClassLiteral(language, it.dao.typeName)
                                CodeLanguage.KOTLIN ->
                                    XCodeBlock.ofKotlinClassLiteral(language, it.dao.typeName)
                            },
                            it.dao.implTypeName,
                            DaoWriter.GET_LIST_OF_TYPE_CONVERTERS_METHOD
                        )
                    }
                    addStatement("return %L", typeConvertersVar)
                }
                .build()
        return XFunSpec.builder(
                language = codeLanguage,
                name =
                    when (codeLanguage) {
                        CodeLanguage.JAVA -> "getRequiredTypeConverters"
                        CodeLanguage.KOTLIN -> "getRequiredTypeConverterClasses"
                    },
                visibility = VisibilityModifier.PROTECTED,
                isOverride = true
            )
            .apply {
                returns(
                    CommonTypeNames.MAP.parametrizedBy(
                        classOfAnyTypeName,
                        CommonTypeNames.LIST.parametrizedBy(classOfAnyTypeName)
                    )
                )
                addCode(body)
            }
            .build()
    }

    private fun createCreateAutoMigrationSpecsSet(): XFunSpec {
        val scope = CodeGenScope(this)
        val classOfAutoMigrationSpecTypeName =
            when (codeLanguage) {
                CodeLanguage.JAVA -> CommonTypeNames.JAVA_CLASS
                CodeLanguage.KOTLIN -> CommonTypeNames.KOTLIN_CLASS
            }.parametrizedBy(XTypeName.getProducerExtendsName(RoomTypeNames.AUTO_MIGRATION_SPEC))
        val autoMigrationSpecsTypeName =
            CommonTypeNames.MUTABLE_SET.parametrizedBy(classOfAutoMigrationSpecTypeName)
        val body =
            XCodeBlock.builder(codeLanguage)
                .apply {
                    val autoMigrationSpecsVar = scope.getTmpVar("_autoMigrationSpecsSet")
                    when (language) {
                        CodeLanguage.JAVA ->
                            addLocalVariable(
                                name = autoMigrationSpecsVar,
                                typeName = autoMigrationSpecsTypeName,
                                assignExpr =
                                    XCodeBlock.ofNewInstance(
                                        codeLanguage,
                                        CommonTypeNames.HASH_SET.parametrizedBy(
                                            classOfAutoMigrationSpecTypeName
                                        )
                                    )
                            )
                        CodeLanguage.KOTLIN ->
                            addLocalVal(
                                autoMigrationSpecsVar,
                                autoMigrationSpecsTypeName,
                                "%M()",
                                KotlinCollectionMemberNames.MUTABLE_SET_OF
                            )
                    }
                    database.autoMigrations
                        .filter { it.isSpecProvided }
                        .map { autoMigration ->
                            val specClassName = checkNotNull(autoMigration.specClassName)
                            addStatement(
                                "%L.add(%L)",
                                autoMigrationSpecsVar,
                                when (language) {
                                    CodeLanguage.JAVA ->
                                        XCodeBlock.ofJavaClassLiteral(language, specClassName)
                                    CodeLanguage.KOTLIN ->
                                        XCodeBlock.ofKotlinClassLiteral(language, specClassName)
                                }
                            )
                        }
                    addStatement("return %L", autoMigrationSpecsVar)
                }
                .build()
        return XFunSpec.builder(
                language = codeLanguage,
                name =
                    when (codeLanguage) {
                        CodeLanguage.JAVA -> "getRequiredAutoMigrationSpecs"
                        CodeLanguage.KOTLIN -> "getRequiredAutoMigrationSpecClasses"
                    },
                visibility = VisibilityModifier.PUBLIC,
                isOverride = true,
            )
            .apply {
                returns(CommonTypeNames.SET.parametrizedBy(classOfAutoMigrationSpecTypeName))
                addCode(body)
            }
            .build()
    }

    private fun createClearAllTables(): XFunSpec {
        return XFunSpec.builder(
                language = codeLanguage,
                name = "clearAllTables",
                visibility = VisibilityModifier.PUBLIC,
                isOverride = true
            )
            .apply {
                val tableNames =
                    database.entities.sortedWith(EntityDeleteComparator()).joinToString(", ") {
                        "\"${it.tableName}\""
                    }
                addStatement("super.performClear(%L, %L)", database.enableForeignKeys, tableNames)
            }
            .build()
    }

    private fun createCreateInvalidationTracker(): XFunSpec {
        val scope = CodeGenScope(this)
        val body =
            XCodeBlock.builder(codeLanguage)
                .apply {
                    val shadowTablesVar = "_shadowTablesMap"
                    val shadowTablesTypeParam =
                        arrayOf(CommonTypeNames.STRING, CommonTypeNames.STRING)
                    val shadowTablesTypeName =
                        CommonTypeNames.MUTABLE_MAP.parametrizedBy(*shadowTablesTypeParam)
                    val shadowTableNames =
                        database.entities
                            .filter { it.shadowTableName != null }
                            .map { it.tableName to it.shadowTableName }
                    addLocalVariable(
                        name = shadowTablesVar,
                        typeName = shadowTablesTypeName,
                        assignExpr =
                            when (language) {
                                CodeLanguage.JAVA ->
                                    XCodeBlock.ofNewInstance(
                                        codeLanguage,
                                        CommonTypeNames.HASH_MAP.parametrizedBy(
                                            *shadowTablesTypeParam
                                        ),
                                        "%L",
                                        shadowTableNames.size
                                    )
                                CodeLanguage.KOTLIN ->
                                    XCodeBlock.of(
                                        language,
                                        "%M()",
                                        KotlinCollectionMemberNames.MUTABLE_MAP_OF
                                    )
                            }
                    )
                    shadowTableNames.forEach { (tableName, shadowTableName) ->
                        addStatement("%L.put(%S, %S)", shadowTablesVar, tableName, shadowTableName)
                    }
                    val viewTablesVar = scope.getTmpVar("_viewTables")
                    val viewTableTypeParam =
                        arrayOf(
                            CommonTypeNames.STRING,
                            CommonTypeNames.SET.parametrizedBy(CommonTypeNames.STRING)
                        )
                    val viewTablesTypeName =
                        CommonTypeNames.MUTABLE_MAP.parametrizedBy(*viewTableTypeParam)
                    addLocalVariable(
                        name = viewTablesVar,
                        typeName = viewTablesTypeName,
                        assignExpr =
                            when (language) {
                                CodeLanguage.JAVA ->
                                    XCodeBlock.ofNewInstance(
                                        codeLanguage,
                                        CommonTypeNames.HASH_MAP.parametrizedBy(
                                            *viewTableTypeParam
                                        ),
                                        "%L",
                                        database.views.size
                                    )
                                CodeLanguage.KOTLIN ->
                                    XCodeBlock.of(
                                        language,
                                        "%M()",
                                        KotlinCollectionMemberNames.MUTABLE_MAP_OF
                                    )
                            }
                    )
                    val tablesType =
                        CommonTypeNames.MUTABLE_SET.parametrizedBy(CommonTypeNames.STRING)
                    for (view in database.views) {
                        val tablesVar = scope.getTmpVar("_tables")
                        addLocalVariable(
                            name = tablesVar,
                            typeName = tablesType,
                            assignExpr =
                                when (language) {
                                    CodeLanguage.JAVA ->
                                        XCodeBlock.ofNewInstance(
                                            codeLanguage,
                                            CommonTypeNames.HASH_SET.parametrizedBy(
                                                CommonTypeNames.STRING
                                            ),
                                            "%L",
                                            view.tables.size
                                        )
                                    CodeLanguage.KOTLIN ->
                                        XCodeBlock.of(
                                            language,
                                            "%M()",
                                            KotlinCollectionMemberNames.MUTABLE_SET_OF
                                        )
                                }
                        )
                        for (table in view.tables) {
                            addStatement("%L.add(%S)", tablesVar, table)
                        }
                        addStatement(
                            "%L.put(%S, %L)",
                            viewTablesVar,
                            view.viewName.lowercase(Locale.US),
                            tablesVar
                        )
                    }
                    val tableNames = database.entities.joinToString(", ") { "\"${it.tableName}\"" }
                    addStatement(
                        "return %L",
                        XCodeBlock.ofNewInstance(
                            codeLanguage,
                            RoomTypeNames.INVALIDATION_TRACKER,
                            "this, %L, %L, %L",
                            shadowTablesVar,
                            viewTablesVar,
                            tableNames
                        )
                    )
                }
                .build()
        return XFunSpec.builder(
                language = codeLanguage,
                name = "createInvalidationTracker",
                visibility = VisibilityModifier.PROTECTED,
                isOverride = true
            )
            .apply {
                returns(RoomTypeNames.INVALIDATION_TRACKER)
                addCode(body)
            }
            .build()
    }

    private fun addDaoImpls(builder: XTypeSpec.Builder) {
        val scope = CodeGenScope(this)
        database.daoMethods.forEach { method ->
            val name =
                method.dao.typeName.simpleNames.first().decapitalize(Locale.US).stripNonJava()
            val privateDaoProperty =
                XPropertySpec.builder(
                        language = codeLanguage,
                        name = scope.getTmpVar("_$name"),
                        typeName =
                            when (codeLanguage) {
                                CodeLanguage.KOTLIN ->
                                    KotlinTypeNames.LAZY.parametrizedBy(method.dao.typeName)
                                CodeLanguage.JAVA -> method.dao.typeName
                            },
                        visibility = VisibilityModifier.PRIVATE,
                        isMutable = codeLanguage == CodeLanguage.JAVA
                    )
                    .apply {
                        // For Kotlin we rely on kotlin.Lazy while for Java we'll memoize the dao
                        // impl in
                        // the getter.
                        if (language == CodeLanguage.KOTLIN) {
                            val lazyInit =
                                XCodeBlock.builder(language)
                                    .apply {
                                        beginControlFlow("lazy")
                                        addStatement(
                                            "%L",
                                            XCodeBlock.ofNewInstance(
                                                language,
                                                method.dao.implTypeName,
                                                "this"
                                            )
                                        )
                                        endControlFlow()
                                    }
                                    .build()
                            initializer(lazyInit)
                        }
                    }
                    .apply(
                        javaFieldBuilder = {
                            // The volatile modifier is needed since in Java the memoization is
                            // generated.
                            addModifiers(Modifier.VOLATILE)
                        },
                        kotlinPropertyBuilder = {}
                    )
                    .build()
            builder.addProperty(privateDaoProperty)
            if (codeLanguage == CodeLanguage.KOTLIN && method.isProperty) {
                builder.addProperty(createDaoProperty(method, privateDaoProperty))
            } else {
                builder.addFunction(createDaoGetter(method, privateDaoProperty))
            }
        }
    }

    private fun createDaoGetter(method: DaoMethod, daoProperty: XPropertySpec): XFunSpec {
        val body =
            XCodeBlock.builder(codeLanguage).apply {
                // For Java we implement the memoization logic in the Dao getter, meanwhile for
                // Kotlin
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
            )
            .apply { addCode(body.build()) }
            .build()
    }

    private fun createDaoProperty(method: DaoMethod, daoProperty: XPropertySpec): XPropertySpec {
        return XPropertySpec.overridingBuilder(
                language = codeLanguage,
                element = method.element,
                owner = database.type
            )
            .getter(XCodeBlock.of(codeLanguage, "return %L.value", daoProperty.name))
            .build()
    }

    private fun createOpenDelegate(): XFunSpec {
        val scope = CodeGenScope(this)
        val body =
            XCodeBlock.builder(codeLanguage)
                .apply {
                    val openDelegateVar = scope.getTmpVar("_openDelegate")
                    val openDelegateCode = scope.fork()
                    OpenDelegateWriter(database).write(openDelegateVar, openDelegateCode)
                    add(openDelegateCode.generate())
                    addStatement("return %L", openDelegateVar)
                }
                .build()
        return XFunSpec.builder(
                language = codeLanguage,
                name = "createOpenDelegate",
                visibility = VisibilityModifier.PROTECTED,
                isOverride = true,
            )
            .apply {
                returns(RoomTypeNames.ROOM_OPEN_DELEGATE)
                addCode(body)
            }
            .build()
    }

    private fun createGetAutoMigrations(): XFunSpec {
        val scope = CodeGenScope(this)
        val classOfAutoMigrationSpecTypeName =
            when (codeLanguage) {
                CodeLanguage.JAVA -> CommonTypeNames.JAVA_CLASS
                CodeLanguage.KOTLIN -> CommonTypeNames.KOTLIN_CLASS
            }.parametrizedBy(XTypeName.getProducerExtendsName(RoomTypeNames.AUTO_MIGRATION_SPEC))
        val specsMapParamName = "autoMigrationSpecs"
        val body =
            XCodeBlock.builder(codeLanguage)
                .apply {
                    val listVar = scope.getTmpVar("_autoMigrations")
                    when (language) {
                        CodeLanguage.JAVA ->
                            addLocalVariable(
                                name = listVar,
                                typeName =
                                    CommonTypeNames.MUTABLE_LIST.parametrizedBy(
                                        RoomTypeNames.MIGRATION
                                    ),
                                assignExpr =
                                    XCodeBlock.ofNewInstance(
                                        codeLanguage,
                                        CommonTypeNames.ARRAY_LIST.parametrizedBy(
                                            RoomTypeNames.MIGRATION
                                        )
                                    )
                            )
                        CodeLanguage.KOTLIN ->
                            addLocalVal(
                                listVar,
                                CommonTypeNames.MUTABLE_LIST.parametrizedBy(
                                    RoomTypeNames.MIGRATION
                                ),
                                "%M()",
                                KotlinCollectionMemberNames.MUTABLE_LIST_OF
                            )
                    }

                    database.autoMigrations.forEach { autoMigrationResult ->
                        val implTypeName = autoMigrationResult.getImplTypeName(database.typeName)
                        val newInstanceCode =
                            if (autoMigrationResult.isSpecProvided) {
                                val specClassName = checkNotNull(autoMigrationResult.specClassName)
                                // For Kotlin use getValue() as the Map's values are never null.
                                val getFunction =
                                    when (language) {
                                        CodeLanguage.JAVA -> "get"
                                        CodeLanguage.KOTLIN -> "getValue"
                                    }
                                XCodeBlock.ofNewInstance(
                                    language,
                                    implTypeName,
                                    "%L.%L(%L)",
                                    specsMapParamName,
                                    getFunction,
                                    when (codeLanguage) {
                                        CodeLanguage.JAVA ->
                                            XCodeBlock.ofJavaClassLiteral(language, specClassName)
                                        CodeLanguage.KOTLIN ->
                                            XCodeBlock.ofKotlinClassLiteral(language, specClassName)
                                    }
                                )
                            } else {
                                XCodeBlock.ofNewInstance(language, implTypeName)
                            }
                        addStatement("%L.add(%L)", listVar, newInstanceCode)
                    }
                    addStatement("return %L", listVar)
                }
                .build()
        return XFunSpec.builder(
                language = codeLanguage,
                name =
                    when (codeLanguage) {
                        CodeLanguage.JAVA -> "getAutoMigrations"
                        CodeLanguage.KOTLIN -> "createAutoMigrations"
                    },
                visibility = VisibilityModifier.PUBLIC,
                isOverride = true,
            )
            .apply {
                returns(CommonTypeNames.LIST.parametrizedBy(RoomTypeNames.MIGRATION))
                addParameter(
                    CommonTypeNames.MAP.parametrizedBy(
                        classOfAutoMigrationSpecTypeName,
                        RoomTypeNames.AUTO_MIGRATION_SPEC,
                    ),
                    specsMapParamName
                )
                addCode(body)
            }
            .build()
    }
}
