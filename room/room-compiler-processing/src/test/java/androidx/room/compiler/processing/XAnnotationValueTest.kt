/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing

import androidx.kruth.assertThat
import androidx.room.compiler.codegen.JArrayTypeName
import androidx.room.compiler.processing.util.KOTLINC_LANGUAGE_1_9_ARGS
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.asJClassName
import androidx.room.compiler.processing.util.asKClassName
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.runProcessorTest
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BOOLEAN_ARRAY
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.CHAR_ARRAY
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.DOUBLE_ARRAY
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FLOAT_ARRAY
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LONG_ARRAY
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.SHORT_ARRAY
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.JParameterizedTypeName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.JWildcardTypeName
import com.squareup.kotlinpoet.javapoet.KClassName
import com.squareup.kotlinpoet.javapoet.KWildcardTypeName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class XAnnotationValueTest(
    private val isPreCompiled: Boolean,
    private val sourceKind: SourceKind,
    private val isTypeAnnotation: Boolean,
) {
    private fun runTest(
        javaSource: Source.JavaSource,
        kotlinSource: Source.KotlinSource,
        kotlincArgs: List<String> = emptyList(),
        handler: (XTestInvocation) -> Unit
    ) {
        val sources =
            when (sourceKind) {
                SourceKind.JAVA -> listOf(javaSource)
                SourceKind.KOTLIN -> listOf(kotlinSource)
            }
        if (isPreCompiled) {
            val compiled = compileFiles(sources, kotlincArguments = kotlincArgs)
            val hasKotlinSources = sources.any { it is Source.KotlinSource }
            val kotlinSources =
                if (hasKotlinSources) {
                    listOf(Source.kotlin("placeholder.kt", "class PlaceholderKotlin"))
                } else {
                    emptyList()
                }
            val newSources =
                kotlinSources +
                    Source.java("PlaceholderJava", "public class " + "PlaceholderJava {}")
            runProcessorTest(
                sources = newSources,
                handler = handler,
                classpath = compiled,
                kotlincArguments = kotlincArgs
            )
        } else {
            runProcessorTest(sources = sources, handler = handler, kotlincArguments = kotlincArgs)
        }
    }

    @Test
    fun testBooleanValue() {
        runTest(
            javaSource =
                Source.java(
                    "test.MyClass",
                    """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @Target({ElementType.TYPE, ElementType.TYPE_USE})
                @interface MyAnnotation {
                    boolean booleanParam();
                    boolean[] booleanArrayParam();
                    boolean[] booleanVarArgsParam(); // There's no varargs in java so use array
                }
                interface MyInterface {}
                @MyAnnotation(
                    booleanParam = true,
                    booleanArrayParam = {true, false, true},
                    booleanVarArgsParam = {false, true, false}
                )
                class MyClass implements
                @MyAnnotation(
                    booleanParam = true,
                    booleanArrayParam = {true, false, true},
                    booleanVarArgsParam = {false, true, false}
                )
                MyInterface {}
                """
                        .trimIndent()
                ) as Source.JavaSource,
            kotlinSource =
                Source.kotlin(
                    "test.MyClass.kt",
                    """
                package test
                @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
                annotation class MyAnnotation(
                    val booleanParam: Boolean,
                    val booleanArrayParam: BooleanArray,
                    vararg val booleanVarArgsParam: Boolean,
                )
                interface MyInterface
                @MyAnnotation(
                    booleanParam = true,
                    booleanArrayParam = [true, false, true],
                    booleanVarArgsParam = [false, true, false],
                )
                class MyClass :
                @MyAnnotation(
                    booleanParam = true,
                    booleanArrayParam = [true, false, true],
                    booleanVarArgsParam = [false, true, false],
                )
                MyInterface
                """
                        .trimIndent()
                ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Boolean) {
                assertThat(annotationValue.valueType.asTypeName().java).isEqualTo(JTypeName.BOOLEAN)
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(BOOLEAN)
                }
                assertThat(annotationValue.hasBooleanValue()).isTrue()
                assertThat(annotationValue.asBoolean()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Boolean) {
                assertThat(annotationValue.valueType.asTypeName().java)
                    .isEqualTo(JArrayTypeName.of(JTypeName.BOOLEAN))
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin)
                        .isEqualTo(BOOLEAN_ARRAY)
                }
                assertThat(annotationValue.hasBooleanListValue()).isTrue()
                // Check the list of values
                assertThat(annotationValue.asBooleanList())
                    .containsExactly(*expectedValues.toTypedArray())
                    .inOrder()
                // Check each annotation value in the list
                annotationValue.asAnnotationValueList().forEachIndexed { i, value ->
                    checkSingleValue(value, expectedValues[i])
                }
            }
            val annotation = getAnnotation(invocation)
            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo(
                    """
                    @test.MyAnnotation(
                        booleanParam = true,
                        booleanArrayParam = {true, false, true},
                        booleanVarArgsParam = {false, true, false}
                    )
                    """
                        .removeWhiteSpace()
                )

            val booleanParam = annotation.getAnnotationValue("booleanParam")
            checkSingleValue(booleanParam, true)

            val booleanArrayParam = annotation.getAnnotationValue("booleanArrayParam")
            checkListValues(booleanArrayParam, true, false, true)

            val booleanVarArgsParam = annotation.getAnnotationValue("booleanVarArgsParam")
            checkListValues(booleanVarArgsParam, false, true, false)
        }
    }

    @Test
    fun testIntValue() {
        runTest(
            javaSource =
                Source.java(
                    "test.MyClass",
                    """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @Target({ElementType.TYPE, ElementType.TYPE_USE})
                @interface MyAnnotation {
                    int intParam();
                    int[] intArrayParam();
                    int[] intVarArgsParam(); // There's no varargs in java so use array
                }
                interface MyInterface {}
                @MyAnnotation(
                    intParam = (short) 1,
                    intArrayParam = {(byte) 3, (short) 5, 7},
                    intVarArgsParam = {(byte) 9, (short) 11, 13}
                )
                class MyClass implements
                @MyAnnotation(
                    intParam = (short) 1,
                    intArrayParam = {(byte) 3, (short) 5, 7},
                    intVarArgsParam = {(byte) 9, (short) 11, 13}
                )
                MyInterface {}
                """
                        .trimIndent()
                ) as Source.JavaSource,
            kotlinSource =
                Source.kotlin(
                    "test.MyClass.kt",
                    """
                package test
                @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
                annotation class MyAnnotation(
                    val intParam: Int,
                    val intArrayParam: IntArray,
                    vararg val intVarArgsParam: Int,
                )
                interface MyInterface
                @MyAnnotation(
                    intParam = 1,
                    intArrayParam = [3, 5, 7],
                    intVarArgsParam = [9, 11, 13],
                )
                class MyClass :
                @MyAnnotation(
                    intParam = 1,
                    intArrayParam = [3, 5, 7],
                    intVarArgsParam = [9, 11, 13],
                )
                MyInterface
                """
                        .trimIndent()
                ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Int) {
                assertThat(annotationValue.valueType.asTypeName().java).isEqualTo(JTypeName.INT)
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(INT)
                }
                assertThat(annotationValue.hasIntValue()).isTrue()
                assertThat(annotationValue.asInt()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Int) {
                assertThat(annotationValue.valueType.asTypeName().java)
                    .isEqualTo(JArrayTypeName.of(JTypeName.INT))
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(INT_ARRAY)
                }
                assertThat(annotationValue.hasIntListValue()).isTrue()
                // Check the list of values
                assertThat(annotationValue.asIntList())
                    .containsExactly(*expectedValues.toTypedArray())
                    .inOrder()
                // Check each annotation value in the list
                annotationValue.asAnnotationValueList().forEachIndexed { i, value ->
                    checkSingleValue(value, expectedValues[i])
                }
            }

            val annotation = getAnnotation(invocation)
            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo(
                    """
                    @test.MyAnnotation(
                        intParam = 1,
                        intArrayParam = {3, 5, 7},
                        intVarArgsParam = {9, 11, 13}
                    )
                    """
                        .removeWhiteSpace()
                )

            val intParam = annotation.getAnnotationValue("intParam")
            checkSingleValue(intParam, 1)

            val intArrayParam = annotation.getAnnotationValue("intArrayParam")
            checkListValues(intArrayParam, 3, 5, 7)

            val intVarArgsParam = annotation.getAnnotationValue("intVarArgsParam")
            checkListValues(intVarArgsParam, 9, 11, 13)
        }
    }

    @Test
    fun testShortValue() {
        runTest(
            javaSource =
                Source.java(
                    "test.MyClass",
                    """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @Target({ElementType.TYPE, ElementType.TYPE_USE})
                @interface MyAnnotation {
                    short shortParam();
                    short[] shortArrayParam();
                    short[] shortVarArgsParam(); // There's no varargs in java so use array
                }
                interface MyInterface {}
                @MyAnnotation(
                    shortParam = (byte) 1,
                    shortArrayParam = {(byte) 3, (short) 5, 7},
                    shortVarArgsParam = {(byte) 9, (short) 11, 13}
                )
                class MyClass implements
                @MyAnnotation(
                    shortParam = (byte) 1,
                    shortArrayParam = {(byte) 3, (short) 5, 7},
                    shortVarArgsParam = {(byte) 9, (short) 11, 13}
                )
                MyInterface {}
                """
                        .trimIndent()
                ) as Source.JavaSource,
            kotlinSource =
                Source.kotlin(
                    "test.MyClass.kt",
                    """
                package test
                @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
                annotation class MyAnnotation(
                    val shortParam: Short,
                    val shortArrayParam: ShortArray,
                    vararg val shortVarArgsParam: Short,
                )
                interface MyInterface
                @MyAnnotation(
                    shortParam = 1,
                    shortArrayParam = [3, 5, 7],
                    shortVarArgsParam = [9, 11, 13],
                )
                class MyClass :
                @MyAnnotation(
                    shortParam = 1,
                    shortArrayParam = [3, 5, 7],
                    shortVarArgsParam = [9, 11, 13],
                )
                MyInterface
                """
                        .trimIndent()
                ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Short) {
                assertThat(annotationValue.valueType.asTypeName().java).isEqualTo(JTypeName.SHORT)
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(SHORT)
                }
                assertThat(annotationValue.hasShortValue()).isTrue()
                assertThat(annotationValue.asShort()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Short) {
                assertThat(annotationValue.valueType.asTypeName().java)
                    .isEqualTo(JArrayTypeName.of(JTypeName.SHORT))
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(SHORT_ARRAY)
                }
                assertThat(annotationValue.hasShortListValue()).isTrue()
                // Check the list of values
                assertThat(annotationValue.asShortList())
                    .containsExactly(*expectedValues.toTypedArray())
                    .inOrder()
                // Check each annotation value in the list
                annotationValue.asAnnotationValueList().forEachIndexed { i, value ->
                    checkSingleValue(value, expectedValues[i])
                }
            }

            val annotation = getAnnotation(invocation)
            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo(
                    """
                    @test.MyAnnotation(
                        shortParam = 1,
                        shortArrayParam = {3, 5, 7},
                        shortVarArgsParam = {9, 11, 13}
                    )
                    """
                        .removeWhiteSpace()
                )

            val shortParam = annotation.getAnnotationValue("shortParam")
            checkSingleValue(shortParam, 1)

            val shortArrayParam = annotation.getAnnotationValue("shortArrayParam")
            checkListValues(shortArrayParam, 3, 5, 7)

            val shortVarArgsParam = annotation.getAnnotationValue("shortVarArgsParam")
            checkListValues(shortVarArgsParam, 9, 11, 13)
        }
    }

    @Test
    fun testLongValue() {
        runTest(
            javaSource =
                Source.java(
                    "test.MyClass",
                    """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @Target({ElementType.TYPE, ElementType.TYPE_USE})
                @interface MyAnnotation {
                    long longParam();
                    long[] longArrayParam();
                    long[] longVarArgsParam(); // There's no varargs in java so use array
                }
                interface MyInterface {}
                @MyAnnotation(
                    longParam = (byte) 1,
                    longArrayParam = {(short) 3, (int) 5, 7L},
                    longVarArgsParam = {(short) 9, (int) 11, 13L}
                )
                class MyClass implements
                @MyAnnotation(
                    longParam = (byte) 1,
                    longArrayParam = {(short) 3, (int) 5, 7L},
                    longVarArgsParam = {(short) 9, (int) 11, 13L}
                )
                MyInterface {}
                """
                        .trimIndent()
                ) as Source.JavaSource,
            kotlinSource =
                Source.kotlin(
                    "test.MyClass.kt",
                    """
                package test
                @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
                annotation class MyAnnotation(
                    val longParam: Long,
                    val longArrayParam: LongArray,
                    vararg val longVarArgsParam: Long,
                )
                interface MyInterface
                @MyAnnotation(
                    longParam = 1L,
                    longArrayParam = [3L, 5L, 7L],
                    longVarArgsParam = [9L, 11L, 13L],
                )
                class MyClass :
                @MyAnnotation(
                    longParam = 1L,
                    longArrayParam = [3L, 5L, 7L],
                    longVarArgsParam = [9L, 11L, 13L],
                )
                MyInterface
                """
                        .trimIndent()
                ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Long) {
                assertThat(annotationValue.valueType.asTypeName().java).isEqualTo(JTypeName.LONG)
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(LONG)
                }
                assertThat(annotationValue.hasLongValue()).isTrue()
                assertThat(annotationValue.asLong()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Long) {
                assertThat(annotationValue.valueType.asTypeName().java)
                    .isEqualTo(JArrayTypeName.of(JTypeName.LONG))
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(LONG_ARRAY)
                }
                assertThat(annotationValue.hasLongListValue()).isTrue()
                // Check the list of values
                assertThat(annotationValue.asLongList())
                    .containsExactly(*expectedValues.toTypedArray())
                    .inOrder()
                // Check each annotation value in the list
                annotationValue.asAnnotationValueList().forEachIndexed { i, value ->
                    checkSingleValue(value, expectedValues[i])
                }
            }

            val annotation = getAnnotation(invocation)
            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo(
                    """
                    @test.MyAnnotation(
                        longParam = 1,
                        longArrayParam = {3, 5, 7},
                        longVarArgsParam = {9, 11, 13}
                    )
                    """
                        .removeWhiteSpace()
                )

            val longParam = annotation.getAnnotationValue("longParam")
            checkSingleValue(longParam, 1L)

            val longArrayParam = annotation.getAnnotationValue("longArrayParam")
            checkListValues(longArrayParam, 3L, 5L, 7L)

            val longVarArgsParam = annotation.getAnnotationValue("longVarArgsParam")
            checkListValues(longVarArgsParam, 9L, 11L, 13L)
        }
    }

    @Test
    fun testFloatValue() {
        runTest(
            javaSource =
                Source.java(
                    "test.MyClass",
                    """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @Target({ElementType.TYPE, ElementType.TYPE_USE})
                @interface MyAnnotation {
                    float floatParam();
                    float[] floatArrayParam();
                    float[] floatVarArgsParam(); // There's no varargs in java so use array
                }
                interface MyInterface {}
                @MyAnnotation(
                    floatParam = (byte) 1,
                    floatArrayParam = {(short) 3, 5.1F, 7.1F},
                    floatVarArgsParam = {9, 11.1F, 13.1F}
                )
                class MyClass implements
                @MyAnnotation(
                    floatParam = (byte) 1,
                    floatArrayParam = {(short) 3, 5.1F, 7.1F},
                    floatVarArgsParam = {9, 11.1F, 13.1F}
                )
                MyInterface {}
                """
                        .trimIndent()
                ) as Source.JavaSource,
            kotlinSource =
                Source.kotlin(
                    "test.MyClass.kt",
                    """
                package test
                @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
                annotation class MyAnnotation(
                    val floatParam: Float,
                    val floatArrayParam: FloatArray,
                    vararg val floatVarArgsParam: Float,
                )
                interface MyInterface
                @MyAnnotation(
                    floatParam = 1F,
                    floatArrayParam = [3F, 5.1F, 7.1F],
                    floatVarArgsParam = [9F, 11.1F, 13.1F],
                )
                class MyClass :
                @MyAnnotation(
                    floatParam = 1F,
                    floatArrayParam = [3F, 5.1F, 7.1F],
                    floatVarArgsParam = [9F, 11.1F, 13.1F],
                )
                MyInterface
                """
                        .trimIndent()
                ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Float) {
                assertThat(annotationValue.valueType.asTypeName().java).isEqualTo(JTypeName.FLOAT)
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(FLOAT)
                }
                assertThat(annotationValue.hasFloatValue()).isTrue()
                assertThat(annotationValue.asFloat()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Float) {
                assertThat(annotationValue.valueType.asTypeName().java)
                    .isEqualTo(JArrayTypeName.of(JTypeName.FLOAT))
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(FLOAT_ARRAY)
                }
                assertThat(annotationValue.hasFloatListValue()).isTrue()
                // Check the list of values
                assertThat(annotationValue.asFloatList())
                    .containsExactly(*expectedValues.toTypedArray())
                    .inOrder()
                // Check each annotation value in the list
                annotationValue.asAnnotationValueList().forEachIndexed { i, value ->
                    checkSingleValue(value, expectedValues[i])
                }
            }

            val annotation = getAnnotation(invocation)
            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo(
                    """
                    @test.MyAnnotation(
                        floatParam = 1.0f,
                        floatArrayParam = {3.0f, 5.1f, 7.1f},
                        floatVarArgsParam = {9.0f, 11.1f, 13.1f}
                    )
                    """
                        .removeWhiteSpace()
                )

            val floatParam = annotation.getAnnotationValue("floatParam")
            checkSingleValue(floatParam, 1.0F)

            val floatArrayParam = annotation.getAnnotationValue("floatArrayParam")
            checkListValues(floatArrayParam, 3.0F, 5.1F, 7.1F)

            val floatVarArgsParam = annotation.getAnnotationValue("floatVarArgsParam")
            checkListValues(floatVarArgsParam, 9.0F, 11.1F, 13.1F)
        }
    }

    @Test
    fun testDoubleValue() {
        runTest(
            javaSource =
                Source.java(
                    "test.MyClass",
                    """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @Target({ElementType.TYPE, ElementType.TYPE_USE})
                @interface MyAnnotation {
                    double doubleParam();
                    double[] doubleArrayParam();
                    double[] doubleVarArgsParam(); // There's no varargs in java so use array
                }
                interface MyInterface {}
                @MyAnnotation(
                    doubleParam = (byte) 1,
                    doubleArrayParam = {(short) 3, 5.1F, 7.1},
                    doubleVarArgsParam = {9, 11.1F, 13.1}
                )
                class MyClass implements
                @MyAnnotation(
                    doubleParam = (byte) 1,
                    doubleArrayParam = {(short) 3, 5.1F, 7.1},
                    doubleVarArgsParam = {9, 11.1F, 13.1}
                )
                MyInterface {}
                """
                        .trimIndent()
                ) as Source.JavaSource,
            kotlinSource =
                Source.kotlin(
                    "test.MyClass.kt",
                    """
                package test
                @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
                annotation class MyAnnotation(
                    val doubleParam: Double,
                    val doubleArrayParam: DoubleArray,
                    vararg val doubleVarArgsParam: Double,
                )
                interface MyInterface
                @MyAnnotation(
                    doubleParam = 1.0,
                    doubleArrayParam = [3.0, 5.1, 7.1],
                    doubleVarArgsParam = [9.0, 11.1, 13.1],
                )
                class MyClass :
                @MyAnnotation(
                    doubleParam = 1.0,
                    doubleArrayParam = [3.0, 5.1, 7.1],
                    doubleVarArgsParam = [9.0, 11.1, 13.1],
                )
                MyInterface
                """
                        .trimIndent()
                ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Double) {
                assertThat(annotationValue.valueType.asTypeName().java).isEqualTo(JTypeName.DOUBLE)
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(DOUBLE)
                }
                assertThat(annotationValue.hasDoubleValue()).isTrue()
                assertThat(annotationValue.asDouble()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Double) {
                assertThat(annotationValue.valueType.asTypeName().java)
                    .isEqualTo(JArrayTypeName.of(JTypeName.DOUBLE))
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin)
                        .isEqualTo(DOUBLE_ARRAY)
                }
                assertThat(annotationValue.hasDoubleListValue()).isTrue()
                // Check the list of values
                assertThat(annotationValue.asDoubleList())
                    .containsExactly(*expectedValues.toTypedArray())
                    .inOrder()
                // Check each annotation value in the list
                annotationValue.asAnnotationValueList().forEachIndexed { i, value ->
                    checkSingleValue(value, expectedValues[i])
                }
            }

            val annotation = getAnnotation(invocation)
            annotation.getAnnotationValue("doubleParam").value
            annotation.getAnnotationValue("doubleArrayParam").value
            annotation.getAnnotationValue("doubleVarArgsParam").value

            // The java source allows an interesting corner case where you can use a float,
            // e.g. 5.1F, in place of a double and the value returned is converted to a double.
            // Note that the kotlin source doesn't even allow this case so we've separated them
            // into two separate checks below.
            if (sourceKind == SourceKind.JAVA) {
                // Compare the AnnotationSpec string ignoring whitespace
                assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                    .isEqualTo(
                        """
                        @test.MyAnnotation(
                            doubleParam = 1.0,
                            doubleArrayParam = {3.0, 5.099999904632568, 7.1},
                            doubleVarArgsParam = {9.0, 11.100000381469727, 13.1}
                        )
                        """
                            .removeWhiteSpace()
                    )

                val doubleParam = annotation.getAnnotationValue("doubleParam")
                checkSingleValue(doubleParam, 1.0)

                val doubleArrayParam = annotation.getAnnotationValue("doubleArrayParam")
                checkListValues(doubleArrayParam, 3.0, 5.099999904632568, 7.1)

                val doubleVarArgsParam = annotation.getAnnotationValue("doubleVarArgsParam")
                checkListValues(doubleVarArgsParam, 9.0, 11.100000381469727, 13.1)
            } else {
                // Compare the AnnotationSpec string ignoring whitespace
                assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                    .isEqualTo(
                        """
                        @test.MyAnnotation(
                            doubleParam = 1.0,
                            doubleArrayParam = {3.0, 5.1, 7.1},
                            doubleVarArgsParam = {9.0, 11.1, 13.1}
                        )
                        """
                            .removeWhiteSpace()
                    )

                val doubleParam = annotation.getAnnotationValue("doubleParam")
                checkSingleValue(doubleParam, 1.0)

                val doubleArrayParam = annotation.getAnnotationValue("doubleArrayParam")
                checkListValues(doubleArrayParam, 3.0, 5.1, 7.1)

                val doubleVarArgsParam = annotation.getAnnotationValue("doubleVarArgsParam")
                checkListValues(doubleVarArgsParam, 9.0, 11.1, 13.1)
            }
        }
    }

    @Test
    fun testByteValue() {
        runTest(
            javaSource =
                Source.java(
                    "test.MyClass",
                    """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @Target({ElementType.TYPE, ElementType.TYPE_USE})
                @interface MyAnnotation {
                    byte byteParam();
                    byte[] byteArrayParam();
                    byte[] byteVarArgsParam(); // There's no varargs in java so use array
                }
                interface MyInterface {}
                @MyAnnotation(
                    byteParam = (byte) 1,
                    byteArrayParam = {(byte) 3, (byte) 5, (byte) 7},
                    byteVarArgsParam = {(byte) 9, (byte) 11, (byte) 13}
                )
                class MyClass implements
                @MyAnnotation(
                    byteParam = (byte) 1,
                    byteArrayParam = {(byte) 3, (byte) 5, (byte) 7},
                    byteVarArgsParam = {(byte) 9, (byte) 11, (byte) 13}
                )
                MyInterface {}
                """
                        .trimIndent()
                ) as Source.JavaSource,
            kotlinSource =
                Source.kotlin(
                    "test.MyClass.kt",
                    """
                package test
                @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
                annotation class MyAnnotation(
                    val byteParam: Byte,
                    val byteArrayParam: ByteArray,
                    vararg val byteVarArgsParam: Byte,
                )
                interface MyInterface
                @MyAnnotation(
                    byteParam = 1,
                    byteArrayParam = [3, 5, 7],
                    byteVarArgsParam = [9, 11, 13],
                )
                class MyClass :
                @MyAnnotation(
                    byteParam = 1,
                    byteArrayParam = [3, 5, 7],
                    byteVarArgsParam = [9, 11, 13],
                )
                MyInterface
                """
                        .trimIndent()
                ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Byte) {
                assertThat(annotationValue.valueType.asTypeName().java).isEqualTo(JTypeName.BYTE)
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(BYTE)
                }
                assertThat(annotationValue.hasByteValue()).isTrue()
                assertThat(annotationValue.asByte()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Byte) {
                assertThat(annotationValue.valueType.asTypeName().java)
                    .isEqualTo(JArrayTypeName.of(JTypeName.BYTE))
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(BYTE_ARRAY)
                }
                assertThat(annotationValue.hasByteListValue()).isTrue()
                // Check the list of values
                assertThat(annotationValue.asByteList())
                    .containsExactly(*expectedValues.toTypedArray())
                    .inOrder()
                // Check each annotation value in the list
                annotationValue.asAnnotationValueList().forEachIndexed { i, value ->
                    checkSingleValue(value, expectedValues[i])
                }
            }

            val annotation = getAnnotation(invocation)
            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo(
                    """
                    @test.MyAnnotation(
                        byteParam = 1,
                        byteArrayParam = {3, 5, 7},
                        byteVarArgsParam = {9, 11, 13}
                    )
                    """
                        .removeWhiteSpace()
                )

            val byteParam = annotation.getAnnotationValue("byteParam")
            checkSingleValue(byteParam, 1)

            val byteArrayParam = annotation.getAnnotationValue("byteArrayParam")
            checkListValues(byteArrayParam, 3, 5, 7)

            val byteVarArgsParam = annotation.getAnnotationValue("byteVarArgsParam")
            checkListValues(byteVarArgsParam, 9, 11, 13)
        }
    }

    @Test
    fun testCharValue() {
        runTest(
            javaSource =
                Source.java(
                    "test.MyClass",
                    """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @Target({ElementType.TYPE, ElementType.TYPE_USE})
                @interface MyAnnotation {
                    char charParam();
                    char[] charArrayParam();
                    char[] charVarArgsParam(); // There's no varargs in java so use array
                }
                interface MyInterface {}
                @MyAnnotation(
                    charParam = '1',
                    charArrayParam = {'2', '3', '4'},
                    charVarArgsParam = {'5', '6', '7'}
                )
                class MyClass implements
                @MyAnnotation(
                    charParam = '1',
                    charArrayParam = {'2', '3', '4'},
                    charVarArgsParam = {'5', '6', '7'}
                )
                MyInterface {}
                """
                        .trimIndent()
                ) as Source.JavaSource,
            kotlinSource =
                Source.kotlin(
                    "test.MyClass.kt",
                    """
                package test
                @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
                annotation class MyAnnotation(
                    val charParam: Char,
                    val charArrayParam: CharArray,
                    vararg val charVarArgsParam: Char,
                )
                interface MyInterface
                @MyAnnotation(
                    charParam = '1',
                    charArrayParam = ['2', '3', '4'],
                    charVarArgsParam = ['5', '6', '7'],
                )
                class MyClass :
                @MyAnnotation(
                    charParam = '1',
                    charArrayParam = ['2', '3', '4'],
                    charVarArgsParam = ['5', '6', '7'],
                )
                MyInterface
                """
                        .trimIndent()
                ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Char) {
                assertThat(annotationValue.valueType.asTypeName().java).isEqualTo(JTypeName.CHAR)
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(CHAR)
                }
                assertThat(annotationValue.hasCharValue()).isTrue()
                assertThat(annotationValue.asChar()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Char) {
                assertThat(annotationValue.valueType.asTypeName().java)
                    .isEqualTo(JArrayTypeName.of(JTypeName.CHAR))
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(CHAR_ARRAY)
                }
                assertThat(annotationValue.hasCharListValue()).isTrue()
                // Check the list of values
                assertThat(annotationValue.asCharList())
                    .containsExactly(*expectedValues.toTypedArray())
                    .inOrder()
                // Check each annotation value in the list
                annotationValue.asAnnotationValueList().forEachIndexed { i, value ->
                    checkSingleValue(value, expectedValues[i])
                }
            }

            val annotation = getAnnotation(invocation)
            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo(
                    """
                    @test.MyAnnotation(
                        charParam = '1',
                        charArrayParam = {'2', '3', '4'},
                        charVarArgsParam = {'5', '6', '7'}
                    )
                    """
                        .removeWhiteSpace()
                )

            val charParam = annotation.getAnnotationValue("charParam")
            checkSingleValue(charParam, '1')

            val charArrayParam = annotation.getAnnotationValue("charArrayParam")
            checkListValues(charArrayParam, '2', '3', '4')

            val charVarArgsParam = annotation.getAnnotationValue("charVarArgsParam")
            checkListValues(charVarArgsParam, '5', '6', '7')
        }
    }

    @Test
    fun testStringValue() {
        runTest(
            javaSource =
                Source.java(
                    "test.MyClass",
                    """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @Target({ElementType.TYPE, ElementType.TYPE_USE})
                @interface MyAnnotation {
                    String stringParam();
                    String stringParam2() default "2";
                    String[] stringArrayParam();
                    String[] stringVarArgsParam(); // There's no varargs in java so use array
                }
                interface MyInterface {}
                @MyAnnotation(
                    stringParam = "1",
                    stringArrayParam = {"3", "5", "7"},
                    stringVarArgsParam = {"9", "11", "13"}
                )
                class MyClass implements
                @MyAnnotation(
                    stringParam = "1",
                    stringArrayParam = {"3", "5", "7"},
                    stringVarArgsParam = {"9", "11", "13"}
                )
                MyInterface {}
                """
                        .trimIndent()
                ) as Source.JavaSource,
            kotlinSource =
                Source.kotlin(
                    "test.MyClass.kt",
                    """
                package test
                @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
                annotation class MyAnnotation(
                    val stringParam: String,
                    val stringParam2: String = "2",
                    val stringArrayParam: Array<String>,
                    vararg val stringVarArgsParam: String,
                )
                interface MyInterface
                @MyAnnotation(
                    stringParam = "1",
                    stringArrayParam = ["3", "5", "7"],
                    stringVarArgsParam = ["9", "11", "13"],
                )
                class MyClass :
                @MyAnnotation(
                    stringParam = "1",
                    stringArrayParam = ["3", "5", "7"],
                    stringVarArgsParam = ["9", "11", "13"],
                )
                MyInterface
                """
                        .trimIndent()
                ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: String) {
                assertThat(annotationValue.valueType.asTypeName().java)
                    .isEqualTo(String::class.asJClassName())
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin)
                        .isEqualTo(String::class.asKClassName())
                }
                assertThat(annotationValue.hasStringValue()).isTrue()
                assertThat(annotationValue.asString()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: String) {
                assertThat(annotationValue.valueType.asTypeName().java)
                    .isEqualTo(JArrayTypeName.of(String::class.asJClassName()))
                if (invocation.isKsp) {
                    if (
                        sourceKind == SourceKind.KOTLIN && annotationValue.name.contains("VarArgs")
                    ) {
                        // Kotlin vararg are producers
                        assertThat(annotationValue.valueType.asTypeName().kotlin)
                            .isEqualTo(
                                ARRAY.parameterizedBy(
                                    KWildcardTypeName.producerOf(String::class.asKClassName())
                                )
                            )
                    } else {
                        assertThat(annotationValue.valueType.asTypeName().kotlin)
                            .isEqualTo(ARRAY.parameterizedBy(String::class.asKClassName()))
                    }
                }
                assertThat(annotationValue.hasStringListValue()).isTrue()
                // Check the list of values
                assertThat(annotationValue.asStringList())
                    .containsExactly(*expectedValues)
                    .inOrder()
                // Check each annotation value in the list
                annotationValue.asAnnotationValueList().forEachIndexed { i, value ->
                    checkSingleValue(value, expectedValues[i])
                }
            }

            val annotation = getAnnotation(invocation)
            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo(
                    """
                  @test.MyAnnotation(
                      stringParam = "1",
                      stringParam2 = "2",
                      stringArrayParam = {"3", "5", "7"},
                      stringVarArgsParam = {"9", "11", "13"}
                  )
                  """
                        .removeWhiteSpace()
                )
            val stringParam2 = annotation.getAnnotationValue("stringParam2")
            checkSingleValue(stringParam2, "2")

            val stringParam = annotation.getAnnotationValue("stringParam")
            checkSingleValue(stringParam, "1")

            val stringArrayParam = annotation.getAnnotationValue("stringArrayParam")
            checkListValues(stringArrayParam, "3", "5", "7")

            val stringVarArgsParam = annotation.getAnnotationValue("stringVarArgsParam")
            checkListValues(stringVarArgsParam, "9", "11", "13")
        }
    }

    @Test
    fun testEnumValue() {
        runTest(
            javaSource =
                Source.java(
                    "test.MyClass",
                    """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                enum MyEnum {V1, V2, V3}
                @Target({ElementType.TYPE, ElementType.TYPE_USE})
                @interface MyAnnotation {
                    MyEnum enumParam();
                    MyEnum[] enumArrayParam();
                    MyEnum[] enumVarArgsParam(); // There's no varargs in java so use array
                }
                interface MyInterface {}
                @MyAnnotation(
                    enumParam = MyEnum.V1,
                    enumArrayParam = {MyEnum.V1, MyEnum.V2, MyEnum.V3},
                    enumVarArgsParam = {MyEnum.V3, MyEnum.V2, MyEnum.V1}
                )
                class MyClass implements
                @MyAnnotation(
                    enumParam = MyEnum.V1,
                    enumArrayParam = {MyEnum.V1, MyEnum.V2, MyEnum.V3},
                    enumVarArgsParam = {MyEnum.V3, MyEnum.V2, MyEnum.V1}
                )
                MyInterface {}
                """
                        .trimIndent()
                ) as Source.JavaSource,
            kotlinSource =
                Source.kotlin(
                    "test.MyClass.kt",
                    """
                package test
                enum class MyEnum {V1, V2, V3}
                @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
                annotation class MyAnnotation(
                    val enumParam: MyEnum,
                    val enumArrayParam: Array<MyEnum>,
                    vararg val enumVarArgsParam: MyEnum,
                )
                interface MyInterface
                @MyAnnotation(
                    enumParam = MyEnum.V1,
                    enumArrayParam = [MyEnum.V1, MyEnum.V2, MyEnum.V3],
                    enumVarArgsParam = [MyEnum.V3, MyEnum.V2, MyEnum.V1],
                )
                class MyClass :
                @MyAnnotation(
                    enumParam = MyEnum.V1,
                    enumArrayParam = [MyEnum.V1, MyEnum.V2, MyEnum.V3],
                    enumVarArgsParam = [MyEnum.V3, MyEnum.V2, MyEnum.V1],
                )
                MyInterface
                """
                        .trimIndent()
                ) as Source.KotlinSource
        ) { invocation ->
            val myEnumJTypeName = JClassName.get("test", "MyEnum")
            val myEnumKTypeName = KClassName("test", "MyEnum")

            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: String) {
                assertThat(annotationValue.valueType.asTypeName().java).isEqualTo(myEnumJTypeName)
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin)
                        .isEqualTo(myEnumKTypeName)
                }
                assertThat(annotationValue.hasEnumValue()).isTrue()
                assertThat(annotationValue.asEnum().name).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: String) {
                assertThat(annotationValue.valueType.asTypeName().java)
                    .isEqualTo(JArrayTypeName.of(myEnumJTypeName))
                if (invocation.isKsp) {
                    if (
                        sourceKind == SourceKind.KOTLIN && annotationValue.name.contains("VarArgs")
                    ) {
                        // Kotlin vararg are producers
                        assertThat(annotationValue.valueType.asTypeName().kotlin)
                            .isEqualTo(
                                ARRAY.parameterizedBy(KWildcardTypeName.producerOf(myEnumKTypeName))
                            )
                    } else {
                        assertThat(annotationValue.valueType.asTypeName().kotlin)
                            .isEqualTo(ARRAY.parameterizedBy(myEnumKTypeName))
                    }
                }
                assertThat(annotationValue.hasEnumListValue()).isTrue()
                // Check the list of values
                assertThat(annotationValue.asEnumList().map { it.name })
                    .containsExactly(*expectedValues)
                    .inOrder()
                // Check each annotation value in the list
                annotationValue.asAnnotationValueList().forEachIndexed { i, value ->
                    checkSingleValue(value, expectedValues[i])
                }
            }

            val annotation = getAnnotation(invocation)
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo(
                    """
                    @test.MyAnnotation(
                        enumParam = test.MyEnum.V1,
                        enumArrayParam = {test.MyEnum.V1, test.MyEnum.V2, test.MyEnum.V3},
                        enumVarArgsParam = {test.MyEnum.V3, test.MyEnum.V2, test.MyEnum.V1}
                    )
                    """
                        .removeWhiteSpace()
                )

            val enumParam = annotation.getAnnotationValue("enumParam")
            checkSingleValue(enumParam, "V1")

            val enumArrayParam = annotation.getAnnotationValue("enumArrayParam")
            checkListValues(enumArrayParam, "V1", "V2", "V3")

            val enumVarArgsParam = annotation.getAnnotationValue("enumVarArgsParam")
            checkListValues(enumVarArgsParam, "V3", "V2", "V1")
        }
    }

    @Test
    fun testTypeValue() {
        runTest(
            javaSource =
                Source.java(
                    "test.MyClass",
                    """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                class C1 {}
                class C2 {}
                class C3 {}
                @Target({ElementType.TYPE, ElementType.TYPE_USE})
                @interface MyAnnotation {
                    Class<?> typeParam();
                    Class<?>[] typeArrayParam();
                    Class<?>[] typeVarArgsParam(); // There's no varargs in java so use array
                }
                interface MyInterface {}
                @MyAnnotation(
                    typeParam = C1.class,
                    typeArrayParam = {C1.class, C2.class, C3.class},
                    typeVarArgsParam = {C3.class, C2.class, C1.class}
                )
                class MyClass implements
                @MyAnnotation(
                    typeParam = C1.class,
                    typeArrayParam = {C1.class, C2.class, C3.class},
                    typeVarArgsParam = {C3.class, C2.class, C1.class}
                )
                MyInterface {}
                """
                        .trimIndent()
                ) as Source.JavaSource,
            kotlinSource =
                Source.kotlin(
                    "test.MyClass.kt",
                    """
                package test
                class C1
                class C2
                class C3
                @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
                annotation class MyAnnotation(
                    val typeParam: kotlin.reflect.KClass<*>,
                    val typeArrayParam: Array<kotlin.reflect.KClass<*>>,
                    vararg val typeVarArgsParam: kotlin.reflect.KClass<*>,
                )
                interface MyInterface
                @MyAnnotation(
                    typeParam = C1::class,
                    typeArrayParam = [C1::class, C2::class, C3::class],
                    typeVarArgsParam = [C3::class, C2::class, C1::class],
                )
                class MyClass :
                @MyAnnotation(
                    typeParam = C1::class,
                    typeArrayParam = [C1::class, C2::class, C3::class],
                    typeVarArgsParam = [C3::class, C2::class, C1::class],
                )
                MyInterface
                """
                        .trimIndent()
                ) as Source.KotlinSource
        ) { invocation ->
            val classJTypeName =
                JParameterizedTypeName.get(
                    JClassName.get(Class::class.java),
                    JWildcardTypeName.subtypeOf(JTypeName.OBJECT)
                )
            val kClassJTypeName =
                JParameterizedTypeName.get(
                    JClassName.get(kotlin.reflect.KClass::class.java),
                    JWildcardTypeName.subtypeOf(JTypeName.OBJECT)
                )
            val kClassKTypeName = kotlin.reflect.KClass::class.asKClassName().parameterizedBy(STAR)
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: String) {
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().java)
                        .isEqualTo(kClassJTypeName)
                    assertThat(annotationValue.valueType.asTypeName().kotlin)
                        .isEqualTo(kClassKTypeName)
                } else {
                    assertThat(annotationValue.valueType.asTypeName().java)
                        .isEqualTo(classJTypeName)
                }
                assertThat(annotationValue.hasTypeValue()).isTrue()
                assertThat(annotationValue.asType().typeElement?.name).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: String) {
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().java)
                        .isEqualTo(JArrayTypeName.of(kClassJTypeName))
                    if (
                        sourceKind == SourceKind.KOTLIN && annotationValue.name.contains("VarArgs")
                    ) {
                        // Kotlin vararg are producers
                        assertThat(annotationValue.valueType.asTypeName().kotlin)
                            .isEqualTo(
                                ARRAY.parameterizedBy(KWildcardTypeName.producerOf(kClassKTypeName))
                            )
                    } else {
                        assertThat(annotationValue.valueType.asTypeName().kotlin)
                            .isEqualTo(ARRAY.parameterizedBy(kClassKTypeName))
                    }
                } else {
                    assertThat(annotationValue.valueType.asTypeName().java)
                        .isEqualTo(JArrayTypeName.of(classJTypeName))
                }
                assertThat(annotationValue.hasTypeListValue()).isTrue()
                // Check the list of values
                assertThat(annotationValue.asTypeList().map { it.typeElement?.name })
                    .containsExactly(*expectedValues)
                    .inOrder()
                // Check each annotation value in the list
                annotationValue.asAnnotationValueList().forEachIndexed { i, value ->
                    checkSingleValue(value, expectedValues[i])
                }
            }

            val annotation = getAnnotation(invocation)
            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo(
                    """
                    @test.MyAnnotation(
                        typeParam = test.C1.class,
                        typeArrayParam = {test.C1.class, test.C2.class, test.C3.class},
                        typeVarArgsParam = {test.C3.class, test.C2.class, test.C1.class}
                    )
                    """
                        .removeWhiteSpace()
                )

            val typeParam = annotation.getAnnotationValue("typeParam")
            checkSingleValue(typeParam, "C1")

            val typeArrayParam = annotation.getAnnotationValue("typeArrayParam")
            checkListValues(typeArrayParam, "C1", "C2", "C3")

            val typeVarArgsParam = annotation.getAnnotationValue("typeVarArgsParam")
            checkListValues(typeVarArgsParam, "C3", "C2", "C1")
        }
    }

    @Test
    fun testDefaultValues() {
        runTest(
            javaSource =
                Source.java(
                    "test.MyClass",
                    """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @Target({ElementType.TYPE, ElementType.TYPE_USE})
                @interface MyAnnotation {
                    String stringParam() default "1";
                    String stringParam2() default "1";
                    String[] stringArrayParam() default {"3", "5", "7"};
                }
                interface MyInterface {}
                @MyAnnotation(stringParam = "2") class MyClass implements
                        @MyAnnotation(stringParam = "2") MyInterface {}
                """
                        .trimIndent()
                ) as Source.JavaSource,
            kotlinSource =
                Source.kotlin(
                    "test.MyClass.kt",
                    """
                package test
                @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
                annotation class MyAnnotation(
                    val stringParam: String = "1",
                    val stringParam2: String = "1",
                    val stringArrayParam: Array<String> = ["3", "5", "7"]
                )
                interface MyInterface
                @MyAnnotation(stringParam = "2") class MyClass :
                        @MyAnnotation(stringParam = "2") MyInterface
                """
                        .trimIndent()
                ) as Source.KotlinSource,
            kotlincArgs = KOTLINC_LANGUAGE_1_9_ARGS
        ) { invocation ->
            val annotation = getAnnotation(invocation)
            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo(
                    """
                    @test.MyAnnotation(
                        stringParam="2",
                        stringParam2="1",
                        stringArrayParam={"3","5","7"}
                    )
                    """
                        .removeWhiteSpace()
                )

            assertThat(
                    annotation
                        .toAnnotationSpec(includeDefaultValues = false)
                        .toString()
                        .removeWhiteSpace()
                )
                .isEqualTo(
                    """
                        @test.MyAnnotation(stringParam="2")
                        """
                        .removeWhiteSpace()
                )
            assertThat(annotation.getAnnotationValue("stringParam").value).isEqualTo("2")
            assertThat(annotation.getAnnotationValue("stringParam2").value).isEqualTo("1")
            assertThat(
                    annotation
                        .getAnnotationValue("stringArrayParam")
                        .asAnnotationValueList()
                        .firstOrNull()
                        ?.value
                )
                .isEqualTo("3")
        }
    }

    @Test
    fun testAnnotationValue() {
        runTest(
            javaSource =
                Source.java(
                    "test.MyClass",
                    """
                package test;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @interface A {
                    String value();
                }
                @Target({ElementType.TYPE, ElementType.TYPE_USE})
                @interface MyAnnotation {
                    A annotationParam();
                    A[] annotationArrayParam();
                    A[] annotationVarArgsParam(); // There's no varargs in java so use array
                }
                interface MyInterface {}
                @MyAnnotation(
                    annotationParam = @A("1"),
                    annotationArrayParam = {@A("3"), @A("5"), @A("7")},
                    annotationVarArgsParam = {@A("9"), @A("11"), @A("13")}
                )
                class MyClass implements
                @MyAnnotation(
                    annotationParam = @A("1"),
                    annotationArrayParam = {@A("3"), @A("5"), @A("7")},
                    annotationVarArgsParam = {@A("9"), @A("11"), @A("13")}
                )
                MyInterface {}
                """
                        .trimIndent()
                ) as Source.JavaSource,
            kotlinSource =
                Source.kotlin(
                    "test.MyClass.kt",
                    """
                package test
                annotation class A(val value: String)
                @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
                annotation class MyAnnotation(
                    val annotationParam: A,
                    val annotationArrayParam: Array<A>,
                    vararg val annotationVarArgsParam: A,
                )
                interface MyInterface
                @MyAnnotation(
                    annotationParam = A("1"),
                    annotationArrayParam = [A("3"), A("5"), A("7")],
                    annotationVarArgsParam = [A("9"), A("11"), A("13")],
                )
                class MyClass :
                @MyAnnotation(
                    annotationParam = A("1"),
                    annotationArrayParam = [A("3"), A("5"), A("7")],
                    annotationVarArgsParam = [A("9"), A("11"), A("13")],
                )
                MyInterface
                """
                        .trimIndent()
                ) as Source.KotlinSource
        ) { invocation ->
            val aJTypeName = JClassName.get("test", "A")
            val aKTypeName = KClassName("test", "A")

            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: String) {
                assertThat(annotationValue.valueType.asTypeName().java).isEqualTo(aJTypeName)
                if (invocation.isKsp) {
                    assertThat(annotationValue.valueType.asTypeName().kotlin).isEqualTo(aKTypeName)
                }
                assertThat(annotationValue.hasAnnotationValue()).isTrue()
                assertThat(annotationValue.asAnnotation().getAsString("value"))
                    .isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: String) {
                assertThat(annotationValue.valueType.asTypeName().java)
                    .isEqualTo(JArrayTypeName.of(aJTypeName))
                if (invocation.isKsp) {
                    if (
                        sourceKind == SourceKind.KOTLIN && annotationValue.name.contains("VarArgs")
                    ) {
                        // Kotlin vararg are producers
                        assertThat(annotationValue.valueType.asTypeName().kotlin)
                            .isEqualTo(
                                ARRAY.parameterizedBy(KWildcardTypeName.producerOf(aKTypeName))
                            )
                    } else {
                        assertThat(annotationValue.valueType.asTypeName().kotlin)
                            .isEqualTo(ARRAY.parameterizedBy(aKTypeName))
                    }
                }
                assertThat(annotationValue.hasAnnotationListValue()).isTrue()
                // Check the list of values
                assertThat(annotationValue.asAnnotationList().map { it.getAsString("value") })
                    .containsExactly(*expectedValues)
                    .inOrder()
                // Check each annotation value in the list
                annotationValue.asAnnotationValueList().forEachIndexed { i, value ->
                    checkSingleValue(value, expectedValues[i])
                }
            }

            val annotation = getAnnotation(invocation)
            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo(
                    """
                    @test.MyAnnotation(
                        annotationParam = @test.A("1"),
                        annotationArrayParam = {@test.A("3"), @test.A("5"), @test.A("7")},
                        annotationVarArgsParam = {@test.A("9"),@test.A("11"),@test.A("13")}
                    )
                    """
                        .removeWhiteSpace()
                )

            val annotationParam = annotation.getAnnotationValue("annotationParam")
            checkSingleValue(annotationParam, "1")

            val annotationArrayParam = annotation.getAnnotationValue("annotationArrayParam")
            checkListValues(annotationArrayParam, "3", "5", "7")

            val annotationVarArgsParam = annotation.getAnnotationValue("annotationVarArgsParam")
            checkListValues(annotationVarArgsParam, "9", "11", "13")
        }
    }

    private fun String.removeWhiteSpace(): String {
        return this.replace("\\s+".toRegex(), "")
    }

    private fun getAnnotation(invocation: XTestInvocation): XAnnotation {
        val typeElement = invocation.processingEnv.requireTypeElement("test.MyClass")
        return if (isTypeAnnotation) {
            typeElement.superInterfaces.first().getAllAnnotations().single {
                it.qualifiedName == "test.MyAnnotation"
            }
        } else {
            typeElement.getAllAnnotations().single { it.qualifiedName == "test.MyAnnotation" }
        }
    }

    enum class SourceKind {
        JAVA,
        KOTLIN
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "isPreCompiled_{0}_sourceKind_{1}_isTypeAnnotation_{2}")
        fun params(): List<Array<Any>> {
            val isPreCompiledValues = arrayOf(false, true)
            val sourceKindValues = arrayOf(SourceKind.JAVA, SourceKind.KOTLIN)
            val isTypeAnnotation = arrayOf(false, true)
            return isPreCompiledValues.flatMap { isPreCompiled ->
                sourceKindValues.flatMap { sourceKind ->
                    isTypeAnnotation.mapNotNull { isTypeAnnotation ->
                        // We can't see type annotations from precompiled Java classes. Skipping it
                        // for now: https://github.com/google/ksp/issues/1296
                        if (isPreCompiled && sourceKind == SourceKind.JAVA && isTypeAnnotation) {
                            null
                        } else {
                            arrayOf(isPreCompiled, sourceKind, isTypeAnnotation)
                        }
                    }
                }
            }
        }
    }
}
