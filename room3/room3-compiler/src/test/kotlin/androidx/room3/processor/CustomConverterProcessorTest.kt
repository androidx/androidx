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

package androidx.room3.processor

import androidx.kruth.assertThat
import androidx.room3.RoomProcessor
import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.VisibilityModifier
import androidx.room3.compiler.codegen.XAnnotationSpec
import androidx.room3.compiler.codegen.XClassName
import androidx.room3.compiler.codegen.XFunSpec
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.codegen.XTypeSpec
import androidx.room3.compiler.codegen.compat.XConverters.applyToJavaPoet
import androidx.room3.compiler.codegen.compat.XConverters.toString
import androidx.room3.compiler.processing.XElement
import androidx.room3.compiler.processing.XFiler
import androidx.room3.compiler.processing.XProcessingEnv
import androidx.room3.compiler.processing.XProcessingEnvConfig
import androidx.room3.compiler.processing.XProcessingStep
import androidx.room3.compiler.processing.ksp.KspBasicAnnotationProcessor
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.XTestInvocation
import androidx.room3.compiler.processing.util.runKspProcessorTest
import androidx.room3.compiler.processing.util.runKspTest
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.CommonTypeNames.MUTABLE_LIST
import androidx.room3.ext.CommonTypeNames.STRING
import androidx.room3.ext.RoomAnnotationTypeNames
import androidx.room3.processor.ProcessorErrors.TYPE_CONVERTER_EMPTY_CLASS
import androidx.room3.processor.ProcessorErrors.TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR
import androidx.room3.processor.ProcessorErrors.TYPE_CONVERTER_MUST_BE_PUBLIC
import androidx.room3.processor.ProcessorErrors.TYPE_CONVERTER_UNBOUND_GENERIC
import androidx.room3.testing.context
import androidx.room3.vo.CustomTypeConverter
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.squareup.javapoet.TypeVariableName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CustomConverterProcessorTest {

    companion object {
        val CONVERTER = XClassName.get("foo.bar", "MyConverter")
        val CONVERTER_NAME = CONVERTER.canonicalName
        val CONTAINER =
            Source.java(
                "foo.bar.Container",
                """
                package foo.bar;
                import androidx.room3.*;
                @TypeConverters(foo.bar.MyConverter.class)
                public class Container {}
                """,
            )
    }

    @Test
    fun validCase() {
        singleClass(
            createConverter(
                XTypeName.BOXED_SHORT.copy(nullable = true),
                XTypeName.BOXED_CHAR.copy(nullable = true),
            )
        ) { converter, _ ->
            assertThat(converter?.fromTypeName)
                .isEqualTo(XTypeName.BOXED_SHORT.copy(nullable = true))
            assertThat(converter?.toTypeName).isEqualTo(XTypeName.BOXED_CHAR.copy(nullable = true))
        }
    }

    @Test
    fun primitiveFrom() {
        singleClass(
            createConverter(XTypeName.PRIMITIVE_SHORT, XTypeName.BOXED_CHAR.copy(nullable = true))
        ) { converter, _ ->
            assertThat(converter?.fromTypeName).isEqualTo(XTypeName.PRIMITIVE_SHORT)
            assertThat(converter?.toTypeName).isEqualTo(XTypeName.BOXED_CHAR.copy(nullable = true))
        }
    }

    @Test
    fun primitiveTo() {
        singleClass(
            createConverter(XTypeName.BOXED_INT.copy(nullable = true), XTypeName.PRIMITIVE_DOUBLE)
        ) { converter, _ ->
            assertThat(converter?.fromTypeName).isEqualTo(XTypeName.BOXED_INT.copy(nullable = true))
            assertThat(converter?.toTypeName).isEqualTo(XTypeName.PRIMITIVE_DOUBLE)
        }
    }

    @Test
    fun primitiveBoth() {
        singleClass(createConverter(XTypeName.PRIMITIVE_INT, XTypeName.PRIMITIVE_DOUBLE)) {
            converter,
            _ ->
            assertThat(converter?.fromTypeName).isEqualTo(XTypeName.PRIMITIVE_INT)
            assertThat(converter?.toTypeName).isEqualTo(XTypeName.PRIMITIVE_DOUBLE)
        }
    }

    @Test
    fun nonNullButNotBoxed() {
        val date = CommonTypeNames.DATE
        singleClass(createConverter(STRING, date)) { converter, _ ->
            assertThat(converter?.fromTypeName).isEqualTo(STRING)
            assertThat(converter?.toTypeName).isEqualTo(date)
        }
    }

    @Test
    fun parametrizedTypeUnbound() {
        val typeVarT = TypeVariableName.get("T")
        val list = CommonTypeNames.MUTABLE_LIST.parametrizedBy(XClassName.get("", "T"))
        val typeVarK = TypeVariableName.get("K")
        val map =
            CommonTypeNames.MUTABLE_MAP.parametrizedBy(
                XClassName.get("", "K"),
                XClassName.get("", "T"),
            )
        singleClass(createConverter(list, map, listOf(typeVarK, typeVarT))) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(TYPE_CONVERTER_UNBOUND_GENERIC)
            }
        }
    }

    @Test
    fun parametrizedTypeSpecific() {
        val date = CommonTypeNames.DATE
        val list = CommonTypeNames.MUTABLE_LIST.parametrizedBy(STRING.copy(nullable = true))
        val map =
            CommonTypeNames.MUTABLE_MAP.parametrizedBy(
                STRING.copy(nullable = true),
                date.copy(nullable = true),
            )
        singleClass(createConverter(list, map)) { converter, _ ->
            assertThat(converter?.fromTypeName).isEqualTo(list)
            assertThat(converter?.toTypeName).isEqualTo(map)
        }
    }

    @Test
    fun testNoConverters() {
        singleClass(
            Source.java(
                CONVERTER_NAME,
                """
                package ${CONVERTER.packageName};
                public class ${CONVERTER.simpleNames.first()} {
                }
                """,
            )
        ) { _, invocation ->
            invocation.assertCompilationResult { hasErrorContaining(TYPE_CONVERTER_EMPTY_CLASS) }
        }
    }

    @Test
    fun checkNoArgConstructor() {
        singleClass(
            Source.java(
                CONVERTER_NAME,
                """
                package ${CONVERTER.packageName};
                import androidx.room3.TypeConverter;

                public class ${CONVERTER.simpleNames.first()} {
                    public ${CONVERTER.simpleNames.first()}(int x) {}
                    @TypeConverter
                    public int x(short y) {return 0;}
                }
                """,
            )
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR)
            }
        }
    }

    @Test
    fun checkNoArgConstructor_withStatic() {
        singleClass(
            Source.java(
                CONVERTER_NAME,
                """
                package ${CONVERTER.packageName};
                import androidx.room3.TypeConverter;

                public class ${CONVERTER.simpleNames.first()} {
                    public ${CONVERTER.simpleNames.first()}(int x) {}
                    @TypeConverter
                    public static int x(short y) {return 0;}
                }
                """,
            )
        ) { converter, _ ->
            assertThat(converter?.fromTypeName).isEqualTo(XTypeName.PRIMITIVE_SHORT)
            assertThat(converter?.toTypeName).isEqualTo(XTypeName.PRIMITIVE_INT)
            assertThat(converter?.isStatic).isTrue()
        }
    }

    @Test
    fun checkPublic() {
        singleClass(
            Source.java(
                CONVERTER_NAME,
                """
                package ${CONVERTER.packageName};
                import androidx.room3.TypeConverter;

                public class ${CONVERTER.simpleNames.first()} {
                    @TypeConverter static int x(short y) {return 0;}
                    @TypeConverter private static int y(boolean y) {return 0;}
                }
                """,
            )
        ) { converter, invocation ->
            assertThat(converter?.fromTypeName).isEqualTo(XTypeName.PRIMITIVE_SHORT)
            assertThat(converter?.toTypeName).isEqualTo(XTypeName.PRIMITIVE_INT)
            assertThat(converter?.isStatic).isTrue()
            invocation.assertCompilationResult {
                hasErrorContaining(TYPE_CONVERTER_MUST_BE_PUBLIC)
                hasErrorCount(2)
            }
        }
    }

    @Test
    fun parametrizedTypeBoundViaParent() {
        val typeVarT = TypeVariableName.get("T")
        val list = CommonTypeNames.MUTABLE_LIST.parametrizedBy(XClassName.get("", "T"))
        val typeVarK = TypeVariableName.get("K")
        val map =
            CommonTypeNames.MUTABLE_MAP.parametrizedBy(
                XClassName.get("", "K"),
                XClassName.get("", "T"),
            )
        val baseConverter = createConverter(list, map, typeVariables = listOf(typeVarT, typeVarK))
        val extendingClassName = XClassName.get("foo.bar", "Extending")
        val extendingClass =
            Source.java(
                extendingClassName.canonicalName,
                "package foo.bar;\n" +
                    XTypeSpec.classBuilder(extendingClassName)
                        .apply { superclass(CONVERTER.parametrizedBy(STRING, XTypeName.BOXED_INT)) }
                        .build()
                        .toString(CodeLanguage.JAVA),
            )
        runKspTest(sources = listOf(baseConverter, extendingClass)) { invocation ->
            val element =
                invocation.processingEnv.requireTypeElement(extendingClassName.canonicalName)
            val converter =
                CustomConverterProcessor(invocation.context, element).process().firstOrNull()
            assertThat(converter?.fromTypeName)
                .isEqualTo(MUTABLE_LIST.parametrizedBy(STRING.copy(nullable = true)))
            assertThat(converter?.toTypeName)
                .isEqualTo(
                    CommonTypeNames.MUTABLE_MAP.parametrizedBy(
                        XTypeName.BOXED_INT.copy(nullable = true),
                        STRING.copy(nullable = true),
                    )
                )
        }
    }

    @Test
    fun checkDuplicates() {
        singleClass(
            createConverter(
                XTypeName.BOXED_SHORT.copy(nullable = true),
                XTypeName.BOXED_CHAR.copy(nullable = true),
                duplicate = true,
            )
        ) { converter, invocation ->
            assertThat(converter?.fromTypeName)
                .isEqualTo(XTypeName.BOXED_SHORT.copy(nullable = true))
            assertThat(converter?.toTypeName).isEqualTo(XTypeName.BOXED_CHAR.copy(nullable = true))
            invocation.assertCompilationResult {
                hasErrorContaining("Multiple functions define the same conversion")
            }
        }
    }

    @Test
    fun checkDuplicates_nullability() {
        val source =
            Source.kotlin(
                "MyConverter.kt",
                """
        package ${CONVERTER.packageName}
        import androidx.room3.*
        class ${CONVERTER.simpleNames.first()} {
            @TypeConverter
            fun nonNulls(input: Int): String {
                TODO()
            }
            @TypeConverter
            fun nullableInput(input: Int?): String {
                TODO()
            }
            @TypeConverter
            fun nullableOutput(input: Int): String? {
                TODO()
            }
        }
            """
                    .trimIndent(),
            )
        singleClass(source) { _, invocation ->
            invocation.assertCompilationResult {
                if (invocation.isKsp) {
                    // no error
                } else {
                    hasErrorContaining("Multiple functions define the same")
                }
            }
        }
    }

    @Test
    fun invalidConverterType() {
        val source =
            Source.java(
                "foo.bar.Container",
                """
                package foo.bar;
                import androidx.room3.*;
                @TypeConverters(int.class)
                public class Container {}
                """,
            )
        runKspTest(listOf(source)) { invocation ->
            val result =
                CustomConverterProcessor.findConverters(
                    invocation.context,
                    invocation.processingEnv.requireTypeElement("foo.bar.Container"),
                )
            assertThat(result.converters).isEmpty()
            invocation.assertCompilationResult {
                if (invocation.isKsp) {
                    // for KSP it always has a type element but we rather assert the other error
                    // instead of not running the code path in ksp tests
                    hasErrorContaining(TYPE_CONVERTER_EMPTY_CLASS)
                } else {
                    hasErrorContaining(ProcessorErrors.typeConverterMustBeDeclared("int"))
                }
            }
        }
    }

    @Test
    fun generatedConverter() {
        class GenerateTypeConverterStep(private val language: CodeLanguage) : XProcessingStep {
            private var generatedClass = false

            override fun annotations() = setOf("androidx.room3.Database")

            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>,
                isLastRound: Boolean,
            ): Set<XElement> {
                elementsByAnnotation["androidx.room3.Database"]?.singleOrNull()?.let {
                    databaseElement ->
                    if (!generatedClass) {
                        generatedClass = true
                        when (language) {
                            CodeLanguage.JAVA -> writeJava(env.filer, databaseElement)
                            CodeLanguage.KOTLIN -> writeKotlin(env.filer, databaseElement)
                        }
                    }
                }
                return super.process(env, elementsByAnnotation, isLastRound)
            }

            private fun writeJava(filer: XFiler, databaseElement: XElement) {
                filer
                    .writeSource(
                        packageName = "",
                        fileNameWithoutExtension = "GeneratedTypeConverter",
                        extension = "java",
                        originatingElements = listOf(databaseElement),
                    )
                    .bufferedWriter()
                    .use { output ->
                        output.write(
                            """
                            import androidx.room3.TypeConverter;

                            public class GeneratedTypeConverter {
                                @TypeConverter
                                public TestId toId(long id) {
                                    return new TestId();
                                }
                                
                                @TypeConverter
                                public long fromId(TestId id) {
                                    return 1;
                                }
                            }
                            """
                                .trimIndent()
                        )
                    }
            }

            private fun writeKotlin(filer: XFiler, databaseElement: XElement) {
                filer
                    .writeSource(
                        packageName = "",
                        fileNameWithoutExtension = "GeneratedTypeConverter",
                        extension = "kt",
                        originatingElements = listOf(databaseElement),
                    )
                    .bufferedWriter()
                    .use { output ->
                        output.write(
                            """
                            import androidx.room3.TypeConverter

                            class GeneratedTypeConverter {
                                @TypeConverter
                                fun toId(id: Long): TestId = TestId()
                                
                                @TypeConverter
                                fun fromId(id: TestId): Long = 1L
                            }
                            """
                                .trimIndent()
                        )
                    }
            }
        }

        val typeConverterKspProvider = SymbolProcessorProvider {
            object :
                KspBasicAnnotationProcessor(
                    symbolProcessorEnvironment = it,
                    config =
                        XProcessingEnvConfig.DEFAULT.copy(disableAnnotatedElementValidation = true),
                ) {
                override fun processingSteps() =
                    listOf(GenerateTypeConverterStep(CodeLanguage.KOTLIN))
            }
        }

        val databaseSrc =
            Source.kotlin(
                "MyDatabase.kt",
                """
                import androidx.room3.*

                class TestId

                @Entity
                data class TestEntity(@PrimaryKey val id: TestId)

                @Dao
                interface MyDao {
                    @Query("SELECT * FROM TestEntity")
                    fun getAll(): List<TestEntity>
                }

                @Database(entities = [TestEntity::class], version = 1, exportSchema = false)
                @TypeConverters(GeneratedTypeConverter::class)
                abstract class MyDatabase : RoomDatabase() {
                    abstract fun getDao(): MyDao
                }
                """
                    .trimIndent(),
            )
        runKspProcessorTest(
            sources = listOf(databaseSrc),
            kotlincArguments =
                listOf("-P", "plugin:org.jetbrains.kotlin.kapt3:correctErrorTypes=true"),
            symbolProcessorProviders = listOf(RoomProcessor.Provider(), typeConverterKspProvider),
        ) {
            it.hasErrorCount(0)
        }
    }

    private fun createConverter(
        from: XTypeName,
        to: XTypeName,
        typeVariables: List<TypeVariableName> = emptyList(),
        duplicate: Boolean = false,
    ): Source {
        val code =
            XTypeSpec.classBuilder(CONVERTER, isOpen = true)
                .apply {
                    setVisibility(VisibilityModifier.PUBLIC)
                    fun buildMethod(name: String) =
                        XFunSpec.builder(name, VisibilityModifier.PUBLIC)
                            .addAnnotation(
                                XAnnotationSpec.builder(RoomAnnotationTypeNames.TYPE_CONVERTER)
                                    .build()
                            )
                            .returns(to)
                            .addParameter("input", from)
                            .apply {
                                if (to.isPrimitive) {
                                    addStatement("return 0")
                                } else {
                                    addStatement("return null")
                                }
                            }
                            .build()
                    addFunction(buildMethod("convertF"))
                    if (duplicate) {
                        addFunction(buildMethod("convertF2"))
                    }
                }
                .applyToJavaPoet { addTypeVariables(typeVariables) }
                .build()
                .toString(CodeLanguage.JAVA)
        return Source.java(CONVERTER.canonicalName, "package ${CONVERTER.packageName};\n$code")
    }

    private fun singleClass(
        vararg sources: Source,
        handler: (CustomTypeConverter?, XTestInvocation) -> Unit,
    ) {
        runKspTest(sources = sources.toList() + CONTAINER) { invocation ->
            val processed =
                CustomConverterProcessor.findConverters(
                    invocation.context,
                    invocation.processingEnv.requireTypeElement("foo.bar.Container"),
                )
            handler(processed.converters.firstOrNull()?.custom, invocation)
        }
    }
}
