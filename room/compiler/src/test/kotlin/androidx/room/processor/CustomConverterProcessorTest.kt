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
import androidx.room.ext.typeName
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_EMPTY_CLASS
import androidx.room.processor.ProcessorErrors
        .TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_MUST_BE_PUBLIC
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_UNBOUND_GENERIC
import androidx.room.testing.TestInvocation
import androidx.room.vo.CustomTypeConverter
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
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
import simpleRun
import java.util.Date
import javax.lang.model.element.Modifier
import javax.tools.JavaFileObject

@RunWith(JUnit4::class)
class CustomConverterProcessorTest {
    companion object {
        val CONVERTER = ClassName.get("foo.bar", "MyConverter")!!
        val CONVERTER_QNAME = CONVERTER.packageName() + "." + CONVERTER.simpleName()
        val CONTAINER = JavaFileObjects.forSourceString("foo.bar.Container",
                """
                package foo.bar;
                import androidx.room.*;
                @TypeConverters(foo.bar.MyConverter.class)
                public class Container {}
                """)
    }

    @Test
    fun validCase() {
        singleClass(createConverter(TypeName.SHORT.box(), TypeName.CHAR.box())) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(TypeName.SHORT.box()))
            assertThat(converter?.toTypeName, `is`(TypeName.CHAR.box()))
        }.compilesWithoutError()
    }

    @Test
    fun primitiveFrom() {
        singleClass(createConverter(TypeName.SHORT, TypeName.CHAR.box())) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(TypeName.SHORT))
            assertThat(converter?.toTypeName, `is`(TypeName.CHAR.box()))
        }.compilesWithoutError()
    }

    @Test
    fun primitiveTo() {
        singleClass(createConverter(TypeName.INT.box(), TypeName.DOUBLE)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(TypeName.INT.box()))
            assertThat(converter?.toTypeName, `is`(TypeName.DOUBLE))
        }.compilesWithoutError()
    }

    @Test
    fun primitiveBoth() {
        singleClass(createConverter(TypeName.INT, TypeName.DOUBLE)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(TypeName.INT))
            assertThat(converter?.toTypeName, `is`(TypeName.DOUBLE))
        }.compilesWithoutError()
    }

    @Test
    fun nonNullButNotBoxed() {
        val string = String::class.typeName()
        val date = Date::class.typeName()
        singleClass(createConverter(string, date)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(string as TypeName))
            assertThat(converter?.toTypeName, `is`(date as TypeName))
        }
    }

    @Test
    fun parametrizedTypeUnbound() {
        val typeVarT = TypeVariableName.get("T")
        val list = ParameterizedTypeName.get(List::class.typeName(), typeVarT)
        val typeVarK = TypeVariableName.get("K")
        val map = ParameterizedTypeName.get(Map::class.typeName(), typeVarK, typeVarT)
        singleClass(createConverter(list, map, listOf(typeVarK, typeVarT))) {
            _, _ ->
        }.failsToCompile().withErrorContaining(TYPE_CONVERTER_UNBOUND_GENERIC)
    }

    @Test
    fun parametrizedTypeSpecific() {
        val string = String::class.typeName()
        val date = Date::class.typeName()
        val list = ParameterizedTypeName.get(List::class.typeName(), string)
        val map = ParameterizedTypeName.get(Map::class.typeName(), string, date)
        singleClass(createConverter(list, map)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(list as TypeName))
            assertThat(converter?.toTypeName, `is`(map as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun testNoConverters() {
        singleClass(JavaFileObjects.forSourceString(CONVERTER_QNAME,
                """
                package ${CONVERTER.packageName()};
                public class ${CONVERTER.simpleName()} {
                }
                """)) { _, _ ->
        }.failsToCompile().withErrorContaining(TYPE_CONVERTER_EMPTY_CLASS)
    }

    @Test
    fun checkNoArgConstructor() {
        singleClass(JavaFileObjects.forSourceString(CONVERTER_QNAME,
                """
                package ${CONVERTER.packageName()};
                import androidx.room.TypeConverter;

                public class ${CONVERTER.simpleName()} {
                    public ${CONVERTER.simpleName()}(int x) {}
                    @TypeConverter
                    public int x(short y) {return 0;}
                }
                """)) { _, _ ->
        }.failsToCompile().withErrorContaining(TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR)
    }

    @Test
    fun checkNoArgConstructor_withStatic() {
        singleClass(JavaFileObjects.forSourceString(CONVERTER_QNAME,
                """
                package ${CONVERTER.packageName()};
                import androidx.room.TypeConverter;

                public class ${CONVERTER.simpleName()} {
                    public ${CONVERTER.simpleName()}(int x) {}
                    @TypeConverter
                    public static int x(short y) {return 0;}
                }
                """)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(TypeName.SHORT))
            assertThat(converter?.toTypeName, `is`(TypeName.INT))
            assertThat(converter?.isStatic, `is`(true))
        }.compilesWithoutError()
    }

    @Test
    fun checkPublic() {
        singleClass(JavaFileObjects.forSourceString(CONVERTER_QNAME,
                """
                package ${CONVERTER.packageName()};
                import androidx.room.TypeConverter;

                public class ${CONVERTER.simpleName()} {
                    @TypeConverter static int x(short y) {return 0;}
                    @TypeConverter private static int y(boolean y) {return 0;}
                }
                """)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(TypeName.SHORT))
            assertThat(converter?.toTypeName, `is`(TypeName.INT))
            assertThat(converter?.isStatic, `is`(true))
        }.failsToCompile().withErrorContaining(TYPE_CONVERTER_MUST_BE_PUBLIC).and()
                .withErrorCount(2)
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Test
    fun parametrizedTypeBoundViaParent() {
        val typeVarT = TypeVariableName.get("T")
        val list = ParameterizedTypeName.get(List::class.typeName(), typeVarT)
        val typeVarK = TypeVariableName.get("K")
        val map = ParameterizedTypeName.get(Map::class.typeName(), typeVarK, typeVarT)

        val baseConverter = createConverter(list, map, listOf(typeVarT, typeVarK))
        val extendingQName = "foo.bar.Extending"
        val extendingClass = JavaFileObjects.forSourceString(extendingQName,
                "package foo.bar;\n" +
                        TypeSpec.classBuilder(ClassName.bestGuess(extendingQName)).apply {
                            superclass(
                                    ParameterizedTypeName.get(CONVERTER, String::class.typeName(),
                                    Integer::class.typeName()))
                        }.build().toString())

        simpleRun(baseConverter, extendingClass) { invocation ->
            val element = invocation.processingEnv.elementUtils.getTypeElement(extendingQName)
            val converter = CustomConverterProcessor(invocation.context, element)
                    .process().firstOrNull()
            assertThat(converter?.fromTypeName, `is`(ParameterizedTypeName.get(
                    List::class.typeName(), String::class.typeName()) as TypeName
            ))
            assertThat(converter?.toTypeName, `is`(ParameterizedTypeName.get(Map::class.typeName(),
                    Integer::class.typeName(), String::class.typeName()) as TypeName
            ))
        }.compilesWithoutError()
    }

    @Test
    fun checkDuplicates() {
        singleClass(
                createConverter(TypeName.SHORT.box(), TypeName.CHAR.box(), duplicate = true)
        ) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(TypeName.SHORT.box()))
            assertThat(converter?.toTypeName, `is`(TypeName.CHAR.box()))
        }.failsToCompile().withErrorContaining("Multiple methods define the same conversion")
    }

    private fun createConverter(
            from: TypeName,
            to: TypeName,
            typeVariables: List<TypeVariableName> = emptyList(),
            duplicate: Boolean = false
    ): JavaFileObject {
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
        return JavaFileObjects.forSourceString(CONVERTER.toString(),
                "package ${CONVERTER.packageName()};\n$code")
    }

    private fun singleClass(
            vararg jfo: JavaFileObject,
            handler: (CustomTypeConverter?, TestInvocation) -> Unit
    ): CompileTester {
        return simpleRun(*((jfo.toList() + CONTAINER).toTypedArray())) { invocation ->
            val processed = CustomConverterProcessor.findConverters(invocation.context,
                    invocation.processingEnv.elementUtils.getTypeElement("foo.bar.Container"))
            handler(processed.converters.firstOrNull()?.custom, invocation)
        }
    }
}
