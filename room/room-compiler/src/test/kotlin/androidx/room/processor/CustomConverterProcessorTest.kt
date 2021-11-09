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

package androidx.room.processor

import androidx.room.TypeConverter
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.ext.typeName
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_EMPTY_CLASS
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_MUST_BE_PUBLIC
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_UNBOUND_GENERIC
import androidx.room.testing.context
import androidx.room.vo.CustomTypeConverter
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Date
import javax.lang.model.element.Modifier

@RunWith(JUnit4::class)
class CustomConverterProcessorTest {
    companion object {
        val CONVERTER = ClassName.get("foo.bar", "MyConverter")!!
        val CONVERTER_QNAME = CONVERTER.packageName() + "." + CONVERTER.simpleName()
        val CONTAINER = Source.java(
            "foo.bar.Container",
            """
                package foo.bar;
                import androidx.room.*;
                @TypeConverters(foo.bar.MyConverter.class)
                public class Container {}
                """
        )
    }

    @Test
    fun validCase() {
        singleClass(createConverter(TypeName.SHORT.box(), TypeName.CHAR.box())) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(TypeName.SHORT.box()))
            assertThat(converter?.toTypeName, `is`(TypeName.CHAR.box()))
        }
    }

    @Test
    fun primitiveFrom() {
        singleClass(createConverter(TypeName.SHORT, TypeName.CHAR.box())) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(TypeName.SHORT))
            assertThat(converter?.toTypeName, `is`(TypeName.CHAR.box()))
        }
    }

    @Test
    fun primitiveTo() {
        singleClass(createConverter(TypeName.INT.box(), TypeName.DOUBLE)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(TypeName.INT.box()))
            assertThat(converter?.toTypeName, `is`(TypeName.DOUBLE))
        }
    }

    @Test
    fun primitiveBoth() {
        singleClass(createConverter(TypeName.INT, TypeName.DOUBLE)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(TypeName.INT))
            assertThat(converter?.toTypeName, `is`(TypeName.DOUBLE))
        }
    }

    @Test
    fun nonNullButNotBoxed() {
        val string = String::class.typeName
        val date = Date::class.typeName
        singleClass(createConverter(string, date)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(string as TypeName))
            assertThat(converter?.toTypeName, `is`(date as TypeName))
        }
    }

    @Test
    fun parametrizedTypeUnbound() {
        val typeVarT = TypeVariableName.get("T")
        val list = ParameterizedTypeName.get(List::class.typeName, typeVarT)
        val typeVarK = TypeVariableName.get("K")
        val map = ParameterizedTypeName.get(Map::class.typeName, typeVarK, typeVarT)
        singleClass(createConverter(list, map, listOf(typeVarK, typeVarT))) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(TYPE_CONVERTER_UNBOUND_GENERIC)
            }
        }
    }

    @Test
    fun parametrizedTypeSpecific() {
        val string = String::class.typeName
        val date = Date::class.typeName
        val list = ParameterizedTypeName.get(List::class.typeName, string)
        val map = ParameterizedTypeName.get(Map::class.typeName, string, date)
        singleClass(createConverter(list, map)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(list as TypeName))
            assertThat(converter?.toTypeName, `is`(map as TypeName))
        }
    }

    @Test
    fun testNoConverters() {
        singleClass(
            Source.java(
                CONVERTER_QNAME,
                """
                package ${CONVERTER.packageName()};
                public class ${CONVERTER.simpleName()} {
                }
                """
            )
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(TYPE_CONVERTER_EMPTY_CLASS)
            }
        }
    }

    @Test
    fun checkNoArgConstructor() {
        singleClass(
            Source.java(
                CONVERTER_QNAME,
                """
                package ${CONVERTER.packageName()};
                import androidx.room.TypeConverter;

                public class ${CONVERTER.simpleName()} {
                    public ${CONVERTER.simpleName()}(int x) {}
                    @TypeConverter
                    public int x(short y) {return 0;}
                }
                """
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
                CONVERTER_QNAME,
                """
                package ${CONVERTER.packageName()};
                import androidx.room.TypeConverter;

                public class ${CONVERTER.simpleName()} {
                    public ${CONVERTER.simpleName()}(int x) {}
                    @TypeConverter
                    public static int x(short y) {return 0;}
                }
                """
            )
        ) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(TypeName.SHORT))
            assertThat(converter?.toTypeName, `is`(TypeName.INT))
            assertThat(converter?.isStatic, `is`(true))
        }
    }

    @Test
    fun checkPublic() {
        singleClass(
            Source.java(
                CONVERTER_QNAME,
                """
                package ${CONVERTER.packageName()};
                import androidx.room.TypeConverter;

                public class ${CONVERTER.simpleName()} {
                    @TypeConverter static int x(short y) {return 0;}
                    @TypeConverter private static int y(boolean y) {return 0;}
                }
                """
            )
        ) { converter, invocation ->
            assertThat(converter?.fromTypeName, `is`(TypeName.SHORT))
            assertThat(converter?.toTypeName, `is`(TypeName.INT))
            assertThat(converter?.isStatic, `is`(true))
            invocation.assertCompilationResult {
                hasErrorContaining(TYPE_CONVERTER_MUST_BE_PUBLIC)
                hasErrorCount(2)
            }
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Test
    fun parametrizedTypeBoundViaParent() {
        val typeVarT = TypeVariableName.get("T")
        val list = ParameterizedTypeName.get(List::class.typeName, typeVarT)
        val typeVarK = TypeVariableName.get("K")
        val map = ParameterizedTypeName.get(Map::class.typeName, typeVarK, typeVarT)

        val baseConverter = createConverter(list, map, listOf(typeVarT, typeVarK))
        val extendingQName = "foo.bar.Extending"
        val extendingClass = Source.java(
            extendingQName,
            "package foo.bar;\n" +
                TypeSpec.classBuilder(ClassName.bestGuess(extendingQName)).apply {
                    superclass(
                        ParameterizedTypeName.get(
                            CONVERTER, String::class.typeName,
                            Integer::class.typeName
                        )
                    )
                }.build().toString()
        )

        runProcessorTest(
            sources = listOf(baseConverter, extendingClass)
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement(extendingQName)
            val converter = CustomConverterProcessor(invocation.context, element)
                .process().firstOrNull()
            assertThat(
                converter?.fromTypeName,
                `is`(
                    ParameterizedTypeName.get(
                        List::class.typeName, String::class.typeName
                    ) as TypeName
                )
            )
            assertThat(
                converter?.toTypeName,
                `is`(
                    ParameterizedTypeName.get(
                        Map::class.typeName,
                        Integer::class.typeName, String::class.typeName
                    ) as TypeName
                )
            )
        }
    }

    @Test
    fun checkDuplicates() {
        singleClass(
            createConverter(TypeName.SHORT.box(), TypeName.CHAR.box(), duplicate = true)
        ) { converter, invocation ->
            assertThat(converter?.fromTypeName, `is`(TypeName.SHORT.box()))
            assertThat(converter?.toTypeName, `is`(TypeName.CHAR.box()))
            invocation.assertCompilationResult {
                hasErrorContaining("Multiple methods define the same conversion")
            }
        }
    }

    @Test
    fun checkDuplicates_nullability() {
        val source = Source.kotlin(
            "MyConverter.kt",
            """
        package ${CONVERTER.packageName()}
        import androidx.room.*
        class ${CONVERTER.simpleName()} {
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
            """.trimIndent()
        )
        singleClass(
            source
        ) { _, invocation ->
            invocation.assertCompilationResult {
                if (invocation.isKsp) {
                    // no error
                } else {
                    hasErrorContaining("Multiple methods define the same")
                }
            }
        }
    }

    @Test
    fun invalidConverterType() {
        val source = Source.java(
            "foo.bar.Container",
            """
                package foo.bar;
                import androidx.room.*;
                @TypeConverters(int.class)
                public class Container {}
                """
        )
        runProcessorTest(listOf(source)) { invocation ->
            val result = CustomConverterProcessor.findConverters(
                invocation.context,
                invocation.processingEnv.requireTypeElement("foo.bar.Container")
            )
            assertThat(result.converters).isEmpty()
            invocation.assertCompilationResult {
                if (invocation.isKsp) {
                    // for KSP it always has a type element but we rather assert the other error
                    // instead of not running the code path in ksp tests
                    hasErrorContaining(TYPE_CONVERTER_EMPTY_CLASS)
                } else {
                    hasErrorContaining(ProcessorErrors.typeConverterMustBeDeclared(TypeName.INT))
                }
            }
        }
    }

    private fun createConverter(
        from: TypeName,
        to: TypeName,
        typeVariables: List<TypeVariableName> = emptyList(),
        duplicate: Boolean = false
    ): Source {
        val code = TypeSpec.classBuilder(CONVERTER).apply {
            addTypeVariables(typeVariables)
            addModifiers(Modifier.PUBLIC)
            fun buildMethod(name: String) = MethodSpec.methodBuilder(name).apply {
                addAnnotation(TypeConverter::class.java)
                addModifiers(Modifier.PUBLIC)
                returns(to)
                addParameter(ParameterSpec.builder(from, "input").build())
                if (to.isPrimitive) {
                    addStatement("return 0")
                } else {
                    addStatement("return null")
                }
            }.build()
            addMethod(buildMethod("convertF"))
            if (duplicate) {
                addMethod(buildMethod("convertF2"))
            }
        }.build().toString()
        return Source.java(
            CONVERTER.toString(),
            "package ${CONVERTER.packageName()};\n$code"
        )
    }

    private fun singleClass(
        vararg sources: Source,
        handler: (CustomTypeConverter?, XTestInvocation) -> Unit
    ) {
        runProcessorTest(
            sources = sources.toList() + CONTAINER
        ) { invocation ->
            val processed = CustomConverterProcessor.findConverters(
                invocation.context,
                invocation.processingEnv.requireTypeElement("foo.bar.Container")
            )
            handler(processed.converters.firstOrNull()?.custom, invocation)
        }
    }
}
