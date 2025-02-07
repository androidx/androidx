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
import androidx.room.compiler.codegen.KFunSpec
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.applyTo
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.applyTo
import androidx.room.compiler.codegen.XName
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XPropertySpec.Builder.Companion.applyTo
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.XTypeSpec.Builder.Companion.applyTo
import androidx.room.compiler.codegen.buildCodeBlock
import androidx.room.compiler.codegen.compat.XConverters.applyToJavaPoet
import androidx.room.compiler.codegen.compat.XConverters.applyToKotlinPoet
import androidx.room.compiler.processing.PropertySpecHelper
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.KotlinCollectionMemberNames
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.decapitalize
import androidx.room.ext.stripNonJava
import androidx.room.solver.CodeGenScope
import androidx.room.vo.DaoFunction
import androidx.room.vo.Database
import java.util.Locale
import javax.lang.model.element.Modifier

/** Writes implementation of classes that were annotated with @Database. */
class DatabaseWriter(
    val database: Database,
    writerContext: WriterContext,
) : TypeWriter(writerContext) {
    private val className = database.implTypeName
    override val packageName = className.packageName

    override fun createTypeSpecBuilder(): XTypeSpec.Builder {
        return XTypeSpec.classBuilder(className).apply {
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
        val typeConvertersVar = scope.getTmpVar("_typeConvertersMap")
        fun classOfAnyTypeName(language: CodeLanguage) =
            when (language) {
                CodeLanguage.JAVA -> CommonTypeNames.JAVA_CLASS
                CodeLanguage.KOTLIN -> CommonTypeNames.KOTLIN_CLASS
            }.parametrizedBy(XTypeName.ANY_WILDCARD)
        val body = buildCodeBlock { language ->
            val classOfAnyTypeName = classOfAnyTypeName(language)
            val typeConvertersTypeName =
                CommonTypeNames.MUTABLE_MAP.parametrizedBy(
                    classOfAnyTypeName,
                    CommonTypeNames.LIST.parametrizedBy(classOfAnyTypeName)
                )
            when (language) {
                CodeLanguage.JAVA ->
                    addLocalVariable(
                        name = typeConvertersVar,
                        typeName = typeConvertersTypeName,
                        assignExpr =
                            XCodeBlock.ofNewInstance(
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
            database.daoFunctions.forEach {
                addStatement(
                    "%L.put(%L, %T.%L())",
                    typeConvertersVar,
                    when (language) {
                        CodeLanguage.JAVA -> XCodeBlock.ofJavaClassLiteral(it.dao.typeName)
                        CodeLanguage.KOTLIN -> XCodeBlock.ofKotlinClassLiteral(it.dao.typeName)
                    },
                    it.dao.implTypeName,
                    DaoWriter.GET_LIST_OF_TYPE_CONVERTERS_FUNCTION
                )
            }
            addStatement("return %L", typeConvertersVar)
        }
        return XFunSpec.builder(
                name =
                    XName.of(
                        java = "getRequiredTypeConverters",
                        kotlin = "getRequiredTypeConverterClasses"
                    ),
                visibility = VisibilityModifier.PROTECTED,
                isOverride = true
            )
            .applyTo { language ->
                returns(
                    CommonTypeNames.MAP.parametrizedBy(
                        classOfAnyTypeName(language),
                        CommonTypeNames.LIST.parametrizedBy(classOfAnyTypeName(language))
                    )
                )
                addCode(body)
            }
            .build()
    }

    private fun createCreateAutoMigrationSpecsSet(): XFunSpec {
        val scope = CodeGenScope(this)
        fun classOfAutoMigrationSpecTypeName(language: CodeLanguage) =
            when (language) {
                CodeLanguage.JAVA -> CommonTypeNames.JAVA_CLASS
                CodeLanguage.KOTLIN -> CommonTypeNames.KOTLIN_CLASS
            }.parametrizedBy(XTypeName.getProducerExtendsName(RoomTypeNames.AUTO_MIGRATION_SPEC))
        val autoMigrationSpecsVar = scope.getTmpVar("_autoMigrationSpecsSet")
        val body = buildCodeBlock { language ->
            val classOfAutoMigrationSpecTypeName = classOfAutoMigrationSpecTypeName(language)
            val autoMigrationSpecsTypeName =
                CommonTypeNames.MUTABLE_SET.parametrizedBy(classOfAutoMigrationSpecTypeName)
            when (language) {
                CodeLanguage.JAVA ->
                    addLocalVariable(
                        name = autoMigrationSpecsVar,
                        typeName = autoMigrationSpecsTypeName,
                        assignExpr =
                            XCodeBlock.ofNewInstance(
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
                            CodeLanguage.JAVA -> XCodeBlock.ofJavaClassLiteral(specClassName)
                            CodeLanguage.KOTLIN -> XCodeBlock.ofKotlinClassLiteral(specClassName)
                        }
                    )
                }
            addStatement("return %L", autoMigrationSpecsVar)
        }
        return XFunSpec.builder(
                name =
                    XName.of(
                        java = "getRequiredAutoMigrationSpecs",
                        kotlin = "getRequiredAutoMigrationSpecClasses"
                    ),
                visibility = VisibilityModifier.PUBLIC,
                isOverride = true,
            )
            .applyTo { language ->
                returns(
                    CommonTypeNames.SET.parametrizedBy(classOfAutoMigrationSpecTypeName(language))
                )
                addCode(body)
            }
            .build()
    }

    private fun createClearAllTables(): XFunSpec {
        return XFunSpec.builder(
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
            XCodeBlock.builder()
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
                            buildCodeBlock { language ->
                                when (language) {
                                    CodeLanguage.JAVA ->
                                        add(
                                            "new %T(%L)",
                                            CommonTypeNames.HASH_MAP.parametrizedBy(
                                                *shadowTablesTypeParam
                                            ),
                                            shadowTableNames.size
                                        )
                                    CodeLanguage.KOTLIN ->
                                        add("%M()", KotlinCollectionMemberNames.MUTABLE_MAP_OF)
                                }
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
                            buildCodeBlock { language ->
                                when (language) {
                                    CodeLanguage.JAVA ->
                                        add(
                                            "new %T(%L)",
                                            CommonTypeNames.HASH_MAP.parametrizedBy(
                                                *viewTableTypeParam
                                            ),
                                            database.views.size
                                        )
                                    CodeLanguage.KOTLIN ->
                                        add("%M()", KotlinCollectionMemberNames.MUTABLE_MAP_OF)
                                }
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
                                buildCodeBlock { language ->
                                    when (language) {
                                        CodeLanguage.JAVA ->
                                            add(
                                                "new %T(%L)",
                                                CommonTypeNames.HASH_SET.parametrizedBy(
                                                    CommonTypeNames.STRING
                                                ),
                                                view.tables.size
                                            )
                                        CodeLanguage.KOTLIN ->
                                            add("%M()", KotlinCollectionMemberNames.MUTABLE_SET_OF)
                                    }
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
        database.daoFunctions.forEach { function ->
            val name =
                function.dao.typeName.simpleNames.first().decapitalize(Locale.US).stripNonJava()
            val privateDaoProperty =
                XPropertySpec.builder(
                        name = scope.getTmpVar("_$name"),
                        typeName =
                            when (scope.language) {
                                CodeLanguage.KOTLIN ->
                                    KotlinTypeNames.LAZY.parametrizedBy(function.dao.typeName)
                                CodeLanguage.JAVA -> function.dao.typeName
                            },
                        visibility = VisibilityModifier.PRIVATE,
                        isMutable = scope.language == CodeLanguage.JAVA
                    )
                    .applyTo { language ->
                        // For Kotlin we rely on kotlin.Lazy while for Java we'll memoize the dao
                        // impl in the getter.
                        if (language == CodeLanguage.KOTLIN) {
                            val lazyInit =
                                XCodeBlock.builder()
                                    .apply {
                                        beginControlFlow("lazy")
                                        addStatement(
                                            "%L",
                                            XCodeBlock.ofNewInstance(
                                                function.dao.implTypeName,
                                                "this"
                                            )
                                        )
                                        endControlFlow()
                                    }
                                    .build()
                            initializer(lazyInit)
                        }
                    }
                    // The volatile modifier is needed since in Java the memoization is generated.
                    .applyToJavaPoet { addModifiers(Modifier.VOLATILE) }
                    .build()
            builder.addProperty(privateDaoProperty)
            builder.applyTo { language ->
                if (language == CodeLanguage.KOTLIN && function.isProperty) {
                    applyToKotlinPoet {
                        addProperty(
                            PropertySpecHelper.overriding(function.element, database.type)
                                .getter(
                                    KFunSpec.getterBuilder()
                                        .addCode("return %L.value", privateDaoProperty.name)
                                        .build()
                                )
                                .build()
                        )
                    }
                } else {
                    addFunction(createDaoGetter(function, privateDaoProperty))
                }
            }
        }
    }

    private fun createDaoGetter(function: DaoFunction, daoProperty: XPropertySpec): XFunSpec {
        val body =
            XCodeBlock.builder().applyTo { language ->
                // For Java we implement the memoization logic in the Dao getter, meanwhile for
                // Kotlin we rely on kotlin.Lazy to the getter just delegates to it.
                when (language) {
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
                                        XCodeBlock.ofNewInstance(function.dao.implTypeName, "this")
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
        return XFunSpec.overridingBuilder(element = function.element, owner = database.element.type)
            .apply { addCode(body.build()) }
            .build()
    }

    private fun createOpenDelegate(): XFunSpec {
        val scope = CodeGenScope(this)
        val body =
            XCodeBlock.builder()
                .apply {
                    val openDelegateVar = scope.getTmpVar("_openDelegate")
                    val openDelegateCode = scope.fork()
                    OpenDelegateWriter(database).write(openDelegateVar, openDelegateCode)
                    add(openDelegateCode.generate())
                    addStatement("return %L", openDelegateVar)
                }
                .build()
        return XFunSpec.builder(
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
        val specsMapParamName = "autoMigrationSpecs"
        val listVar = scope.getTmpVar("_autoMigrations")
        val body = buildCodeBlock { language ->
            when (language) {
                CodeLanguage.JAVA ->
                    addLocalVariable(
                        name = listVar,
                        typeName =
                            CommonTypeNames.MUTABLE_LIST.parametrizedBy(RoomTypeNames.MIGRATION),
                        assignExpr =
                            XCodeBlock.ofNewInstance(
                                CommonTypeNames.ARRAY_LIST.parametrizedBy(RoomTypeNames.MIGRATION)
                            )
                    )
                CodeLanguage.KOTLIN ->
                    addLocalVal(
                        listVar,
                        CommonTypeNames.MUTABLE_LIST.parametrizedBy(RoomTypeNames.MIGRATION),
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
                        XCodeBlock.ofNewInstance(
                            implTypeName,
                            "%L.%L(%L)",
                            specsMapParamName,
                            XName.of(java = "get", kotlin = "getValue"),
                            buildCodeBlock { language ->
                                when (language) {
                                    CodeLanguage.JAVA ->
                                        add(XCodeBlock.ofJavaClassLiteral(specClassName))
                                    CodeLanguage.KOTLIN ->
                                        add(XCodeBlock.ofKotlinClassLiteral(specClassName))
                                }
                            }
                        )
                    } else {
                        XCodeBlock.ofNewInstance(implTypeName)
                    }
                addStatement("%L.add(%L)", listVar, newInstanceCode)
            }
            addStatement("return %L", listVar)
        }
        return XFunSpec.builder(
                name = XName.of(java = "getAutoMigrations", kotlin = "createAutoMigrations"),
                visibility = VisibilityModifier.PUBLIC,
                isOverride = true,
            )
            .applyTo { language ->
                val classOfAutoMigrationSpecTypeName =
                    when (language) {
                        CodeLanguage.JAVA -> CommonTypeNames.JAVA_CLASS
                        CodeLanguage.KOTLIN -> CommonTypeNames.KOTLIN_CLASS
                    }.parametrizedBy(
                        XTypeName.getProducerExtendsName(RoomTypeNames.AUTO_MIGRATION_SPEC)
                    )
                returns(CommonTypeNames.LIST.parametrizedBy(RoomTypeNames.MIGRATION))
                addParameter(
                    specsMapParamName,
                    CommonTypeNames.MAP.parametrizedBy(
                        classOfAutoMigrationSpecTypeName,
                        RoomTypeNames.AUTO_MIGRATION_SPEC,
                    )
                )
                addCode(body)
            }
            .build()
    }
}
