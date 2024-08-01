/*
 * Copyright (C) 2017 The Android Open Source Project
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

@file:Suppress("HasPlatformType")

package androidx.room.solver

import androidx.room.DatabaseProcessingStep
import androidx.room.TypeConverter
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.processing.util.CompilationResultSubject
import androidx.room.compiler.processing.util.Source
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomAnnotationTypeNames
import androidx.room.ext.RoomTypeNames.ROOM_DB
import androidx.room.processor.ProcessorErrors.CANNOT_BIND_QUERY_PARAMETER_INTO_STMT
import androidx.room.runProcessorTestWithK1
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CustomTypeConverterResolutionTest {
    fun XTypeSpec.toSource(): Source {
        return Source.java(this.className.canonicalName, "package foo.bar;\n" + toString())
    }

    companion object {
        val ENTITY = XClassName.get("foo.bar", "MyEntity")
        val DB = XClassName.get("foo.bar", "MyDb")
        val DAO = XClassName.get("foo.bar", "MyDao")

        val CUSTOM_TYPE = XClassName.get("foo.bar", "CustomType")
        val CUSTOM_TYPE_JFO =
            Source.java(
                CUSTOM_TYPE.canonicalName,
                """
                package ${CUSTOM_TYPE.packageName};
                public class ${CUSTOM_TYPE.simpleNames.first()} {
                    public int value;
                }
                """
            )
        val CUSTOM_TYPE_CONVERTER = XClassName.get("foo.bar", "MyConverter")
        val CUSTOM_TYPE_CONVERTER_JFO =
            Source.java(
                CUSTOM_TYPE_CONVERTER.canonicalName,
                """
                package ${CUSTOM_TYPE_CONVERTER.packageName};
                public class ${CUSTOM_TYPE_CONVERTER.simpleNames.first()} {
                    @${TypeConverter::class.java.canonicalName}
                    public static ${CUSTOM_TYPE.canonicalName} toCustom(int value) {
                        return null;
                    }
                    @${TypeConverter::class.java.canonicalName}
                    public static int fromCustom(${CUSTOM_TYPE.canonicalName} input) {
                        return 0;
                    }
                }
                """
            )
        val CUSTOM_TYPE_SET = CommonTypeNames.SET.parametrizedBy(CUSTOM_TYPE)
        val CUSTOM_TYPE_SET_CONVERTER = XClassName.get("foo.bar", "MySetConverter")
        val CUSTOM_TYPE_SET_CONVERTER_JFO =
            Source.java(
                CUSTOM_TYPE_SET_CONVERTER.canonicalName,
                """
                package ${CUSTOM_TYPE_SET_CONVERTER.packageName};
                import java.util.HashSet;
                import java.util.Set;
                public class ${CUSTOM_TYPE_SET_CONVERTER.simpleNames.first()} {
                    @${TypeConverter::class.java.canonicalName}
                    public static ${CUSTOM_TYPE_SET.toString(CodeLanguage.JAVA)} toCustom(int value) {
                        return null;
                    }
                    @${TypeConverter::class.java.canonicalName}
                    public static int fromCustom(${CUSTOM_TYPE_SET.toString(CodeLanguage.JAVA)} input) {
                        return 0;
                    }
                }
                """
            )
    }

    @Test
    fun useFromDatabase_forEntity() {
        val entity = createEntity(hasCustomField = true)
        val database = createDatabase(hasConverters = true, hasDao = true)
        val dao = createDao(hasQueryReturningEntity = true, hasQueryWithCustomParam = true)
        runTest(sources = listOf(entity.toSource(), dao.toSource(), database.toSource()))
    }

    @Test
    fun collection_forEntity() {
        val entity = createEntity(hasCustomField = true, useCollection = true)
        val database = createDatabase(hasConverters = true, hasDao = true, useCollection = true)
        val dao = createDao(hasQueryWithCustomParam = false, useCollection = true)
        runTest(sources = listOf(entity.toSource(), dao.toSource(), database.toSource()))
    }

    @Test
    fun collection_forDao() {
        val entity = createEntity(hasCustomField = true, useCollection = true)
        val database = createDatabase(hasConverters = true, hasDao = true, useCollection = true)
        val dao = createDao(hasQueryWithCustomParam = true, useCollection = true)
        runTest(sources = listOf(entity.toSource(), dao.toSource(), database.toSource()))
    }

    @Test
    fun useFromDatabase_forQueryParameter() {
        val entity = createEntity()
        val database = createDatabase(hasConverters = true, hasDao = true)
        val dao = createDao(hasQueryWithCustomParam = true)
        runTest(sources = listOf(entity.toSource(), dao.toSource(), database.toSource()))
    }

    @Test
    fun useFromDatabase_forReturnValue() {
        val entity = createEntity(hasCustomField = true)
        val database = createDatabase(hasConverters = true, hasDao = true)
        val dao = createDao(hasQueryReturningEntity = true)
        runTest(sources = listOf(entity.toSource(), dao.toSource(), database.toSource()))
    }

    @Test
    fun useFromDao_forQueryParameter() {
        val entity = createEntity()
        val database = createDatabase(hasDao = true)
        val dao =
            createDao(
                hasConverters = true,
                hasQueryReturningEntity = true,
                hasQueryWithCustomParam = true
            )
        runTest(sources = listOf(entity.toSource(), dao.toSource(), database.toSource()))
    }

    @Test
    fun useFromEntity_forReturnValue() {
        val entity = createEntity(hasCustomField = true, hasConverters = true)
        val database = createDatabase(hasDao = true)
        val dao = createDao(hasQueryReturningEntity = true)
        runTest(sources = listOf(entity.toSource(), dao.toSource(), database.toSource()))
    }

    @Test
    fun useFromEntityField_forReturnValue() {
        val entity = createEntity(hasCustomField = true, hasConverterOnField = true)
        val database = createDatabase(hasDao = true)
        val dao = createDao(hasQueryReturningEntity = true)
        runTest(sources = listOf(entity.toSource(), dao.toSource(), database.toSource()))
    }

    @Test
    fun useFromEntity_forQueryParameter() {
        val entity = createEntity(hasCustomField = true, hasConverters = true)
        val database = createDatabase(hasDao = true)
        val dao = createDao(hasQueryWithCustomParam = true)
        runTest(sources = listOf(entity.toSource(), dao.toSource(), database.toSource())) {
            it.hasErrorContaining(CANNOT_BIND_QUERY_PARAMETER_INTO_STMT)
        }
    }

    @Test
    fun useFromEntityField_forQueryParameter() {
        val entity = createEntity(hasCustomField = true, hasConverterOnField = true)
        val database = createDatabase(hasDao = true)
        val dao = createDao(hasQueryWithCustomParam = true)
        runTest(sources = listOf(entity.toSource(), dao.toSource(), database.toSource())) {
            it.hasErrorContaining(CANNOT_BIND_QUERY_PARAMETER_INTO_STMT)
        }
    }

    @Test
    fun useFromQueryMethod_forQueryParameter() {
        val entity = createEntity()
        val database = createDatabase(hasDao = true)
        val dao = createDao(hasQueryWithCustomParam = true, hasMethodConverters = true)
        runTest(sources = listOf(entity.toSource(), dao.toSource(), database.toSource()))
    }

    @Test
    fun useFromQueryParameter_forQueryParameter() {
        val entity = createEntity()
        val database = createDatabase(hasDao = true)
        val dao = createDao(hasQueryWithCustomParam = true, hasParameterConverters = true)
        runTest(sources = listOf(entity.toSource(), dao.toSource(), database.toSource()))
    }

    private fun runTest(
        sources: List<Source>,
        onCompilationResult: (CompilationResultSubject) -> Unit = { it.hasErrorCount(0) }
    ) {
        runProcessorTestWithK1(
            sources =
                sources +
                    CUSTOM_TYPE_JFO +
                    CUSTOM_TYPE_CONVERTER_JFO +
                    CUSTOM_TYPE_SET_CONVERTER_JFO,
            createProcessingSteps = { listOf(DatabaseProcessingStep()) },
            onCompilationResult = onCompilationResult
        )
    }

    private fun createEntity(
        hasCustomField: Boolean = false,
        hasConverters: Boolean = false,
        hasConverterOnField: Boolean = false,
        useCollection: Boolean = false
    ): XTypeSpec {
        if (hasConverterOnField && hasConverters) {
            throw IllegalArgumentException("cannot have both converters")
        }
        val type =
            if (useCollection) {
                CUSTOM_TYPE_SET
            } else {
                CUSTOM_TYPE
            }
        return XTypeSpec.classBuilder(CodeLanguage.JAVA, ENTITY)
            .apply {
                addAnnotation(
                    XAnnotationSpec.builder(CodeLanguage.JAVA, RoomAnnotationTypeNames.ENTITY)
                        .build()
                )
                setVisibility(VisibilityModifier.PUBLIC)
                if (hasCustomField) {
                    addProperty(
                        XPropertySpec.builder(
                                CodeLanguage.JAVA,
                                "myCustomField",
                                type,
                                VisibilityModifier.PUBLIC,
                                isMutable = true
                            )
                            .apply {
                                if (hasConverterOnField) {
                                    addAnnotation(createConvertersAnnotation())
                                }
                            }
                            .build()
                    )
                }
                if (hasConverters) {
                    addAnnotation(createConvertersAnnotation())
                }
                addProperty(
                    XPropertySpec.builder(
                            language = CodeLanguage.JAVA,
                            name = "id",
                            typeName = XTypeName.PRIMITIVE_INT,
                            visibility = VisibilityModifier.PUBLIC,
                            isMutable = true
                        )
                        .addAnnotation(
                            XAnnotationSpec.builder(
                                    CodeLanguage.JAVA,
                                    RoomAnnotationTypeNames.PRIMARY_KEY
                                )
                                .build()
                        )
                        .build()
                )
            }
            .build()
    }

    private fun createDatabase(
        hasConverters: Boolean = false,
        hasDao: Boolean = false,
        useCollection: Boolean = false
    ): XTypeSpec {
        return XTypeSpec.classBuilder(CodeLanguage.JAVA, DB, isOpen = true)
            .apply {
                addAbstractModifier()
                setVisibility(VisibilityModifier.PUBLIC)
                superclass(ROOM_DB)
                if (hasConverters) {
                    addAnnotation(createConvertersAnnotation(useCollection = useCollection))
                }
                addProperty(
                    XPropertySpec.builder(
                            language = CodeLanguage.JAVA,
                            name = "id",
                            typeName = XTypeName.PRIMITIVE_INT,
                            visibility = VisibilityModifier.PUBLIC,
                            isMutable = true
                        )
                        .addAnnotation(
                            XAnnotationSpec.builder(
                                    CodeLanguage.JAVA,
                                    RoomAnnotationTypeNames.PRIMARY_KEY
                                )
                                .build()
                        )
                        .build()
                )
                if (hasDao) {
                    addFunction(
                        XFunSpec.builder(
                                language = CodeLanguage.JAVA,
                                "getDao",
                                VisibilityModifier.PUBLIC
                            )
                            .apply {
                                addAbstractModifier()
                                returns(DAO)
                            }
                            .build()
                    )
                }
                addAnnotation(
                    XAnnotationSpec.builder(CodeLanguage.JAVA, RoomAnnotationTypeNames.DATABASE)
                        .apply {
                            addMember("entities", XCodeBlock.of(language, "{%T.class}", ENTITY))
                            addMember("version", XCodeBlock.of(language, "42"))
                            addMember("exportSchema", XCodeBlock.of(language, "false"))
                        }
                        .build()
                )
            }
            .build()
    }

    private fun createDao(
        hasConverters: Boolean = false,
        hasQueryReturningEntity: Boolean = false,
        hasQueryWithCustomParam: Boolean = false,
        hasMethodConverters: Boolean = false,
        hasParameterConverters: Boolean = false,
        useCollection: Boolean = false
    ): XTypeSpec {
        val annotationCount =
            listOf(hasMethodConverters, hasConverters, hasParameterConverters)
                .map { if (it) 1 else 0 }
                .sum()
        if (annotationCount > 1) {
            throw IllegalArgumentException("cannot set both of these")
        }
        if (hasParameterConverters && !hasQueryWithCustomParam) {
            throw IllegalArgumentException("inconsistent")
        }
        return XTypeSpec.classBuilder(CodeLanguage.JAVA, DAO, isOpen = true)
            .apply {
                addAbstractModifier()
                addAnnotation(
                    XAnnotationSpec.builder(CodeLanguage.JAVA, RoomAnnotationTypeNames.DAO).build()
                )
                setVisibility(VisibilityModifier.PUBLIC)
                if (hasConverters) {
                    addAnnotation(createConvertersAnnotation(useCollection = useCollection))
                }
                if (hasQueryReturningEntity) {
                    addFunction(
                        XFunSpec.builder(CodeLanguage.JAVA, "loadAll", VisibilityModifier.PUBLIC)
                            .apply {
                                addAbstractModifier()
                                addAnnotation(
                                    XAnnotationSpec.builder(
                                            CodeLanguage.JAVA,
                                            RoomAnnotationTypeNames.QUERY
                                        )
                                        .addMember(
                                            "value",
                                            XCodeBlock.of(
                                                CodeLanguage.JAVA,
                                                "%S",
                                                "SELECT * FROM ${ENTITY.simpleNames.first()} LIMIT 1"
                                            )
                                        )
                                        .build()
                                )
                                returns(ENTITY)
                            }
                            .build()
                    )
                }
                val customType =
                    if (useCollection) {
                        CUSTOM_TYPE_SET
                    } else {
                        CUSTOM_TYPE
                    }
                if (hasQueryWithCustomParam) {
                    addFunction(
                        XFunSpec.builder(
                                CodeLanguage.JAVA,
                                "queryWithCustom",
                                VisibilityModifier.PUBLIC
                            )
                            .apply {
                                addAbstractModifier()
                                addAnnotation(
                                    XAnnotationSpec.builder(
                                            CodeLanguage.JAVA,
                                            RoomAnnotationTypeNames.QUERY
                                        )
                                        .addMember(
                                            "value",
                                            XCodeBlock.of(
                                                CodeLanguage.JAVA,
                                                "%S",
                                                "SELECT COUNT(*) FROM ${ENTITY.simpleNames.first()} where" +
                                                    " id = :custom"
                                            )
                                        )
                                        .build()
                                )
                                if (hasMethodConverters) {
                                    addAnnotation(
                                        createConvertersAnnotation(useCollection = useCollection)
                                    )
                                }
                                addParameter(customType, "custom")
                                    .apply {
                                        if (hasParameterConverters) {
                                            addAnnotation(
                                                createConvertersAnnotation(
                                                    useCollection = useCollection
                                                )
                                            )
                                        }
                                    }
                                    .build()
                                returns(XTypeName.PRIMITIVE_INT)
                            }
                            .build()
                    )
                }
            }
            .build()
    }

    private fun createConvertersAnnotation(useCollection: Boolean = false): XAnnotationSpec {
        val converter =
            if (useCollection) {
                CUSTOM_TYPE_SET_CONVERTER
            } else {
                CUSTOM_TYPE_CONVERTER
            }
        return XAnnotationSpec.builder(CodeLanguage.JAVA, RoomAnnotationTypeNames.TYPE_CONVERTERS)
            .addMember("value", XCodeBlock.of(CodeLanguage.JAVA, "%T.class", converter))
            .build()
    }
}
