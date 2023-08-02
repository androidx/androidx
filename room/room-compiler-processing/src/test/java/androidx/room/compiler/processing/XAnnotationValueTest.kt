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

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class XAnnotationValueTest(
    private val isPreCompiled: Boolean,
    private val sourceKind: SourceKind,
) {
    private fun runTest(
        javaSource: Source.JavaSource,
        kotlinSource: Source.KotlinSource,
        handler: (XTestInvocation) -> Unit
    ) {
        val sources = when (sourceKind) {
            SourceKind.JAVA -> listOf(javaSource)
            SourceKind.KOTLIN -> listOf(kotlinSource)
        }
        if (isPreCompiled) {
            val compiled = compileFiles(sources)
            val hasKotlinSources = sources.any {
                it is Source.KotlinSource
            }
            val kotlinSources = if (hasKotlinSources) {
                listOf(
                    Source.kotlin("placeholder.kt", "class PlaceholderKotlin")
                )
            } else {
                emptyList()
            }
            val newSources = kotlinSources + Source.java(
                "PlaceholderJava",
                "public class " +
                    "PlaceholderJava {}"
            )
            runProcessorTest(
                sources = newSources,
                handler = handler,
                classpath = compiled
            )
        } else {
            runProcessorTest(
                sources = sources,
                handler = handler
            )
        }
    }

    @Test
    fun testBooleanValue() {
        runTest(
            javaSource = Source.java(
                "test.MyClass",
                """
                package test;
                @interface MyAnnotation {
                    boolean booleanParam();
                    boolean[] booleanArrayParam();
                    boolean[] booleanVarArgsParam(); // There's no varargs in java so use array
                }
                @MyAnnotation(
                    booleanParam = true,
                    booleanArrayParam = {true, false, true},
                    booleanVarArgsParam = {false, true, false}
                )
                class MyClass {}
                """.trimIndent()
            ) as Source.JavaSource,
            kotlinSource = Source.kotlin(
                "test.MyClass.kt",
                """
                package test
                annotation class MyAnnotation(
                    val booleanParam: Boolean,
                    val booleanArrayParam: BooleanArray,
                    vararg val booleanVarArgsParam: Boolean,
                )
                @MyAnnotation(
                    booleanParam = true,
                    booleanArrayParam = [true, false, true],
                    booleanVarArgsParam = [false, true, false],
                )
                class MyClass
                """.trimIndent()
            ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Boolean) {
                assertThat(annotationValue.valueType.typeName).isEqualTo(TypeName.BOOLEAN)
                assertThat(annotationValue.hasBooleanValue()).isTrue()
                assertThat(annotationValue.asBoolean()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Boolean) {
                assertThat(annotationValue.valueType.typeName)
                    .isEqualTo(ArrayTypeName.of(TypeName.BOOLEAN))
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

            val annotation = invocation.processingEnv.requireTypeElement("test.MyClass")
                .getAllAnnotations()
                .single { it.qualifiedName == "test.MyAnnotation" }

            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo("""
                    @test.MyAnnotation(
                        booleanParam = true,
                        booleanArrayParam = {true, false, true},
                        booleanVarArgsParam = {false, true, false}
                    )
                    """.removeWhiteSpace())

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
            javaSource = Source.java(
                "test.MyClass",
                """
                package test;
                @interface MyAnnotation {
                    int intParam();
                    int[] intArrayParam();
                    int[] intVarArgsParam(); // There's no varargs in java so use array
                }
                @MyAnnotation(
                    intParam = 1,
                    intArrayParam = {3, 5, 7},
                    intVarArgsParam = {9, 11, 13}
                )
                class MyClass {}
                """.trimIndent()
            ) as Source.JavaSource,
            kotlinSource = Source.kotlin(
                "test.MyClass.kt",
                """
                package test
                annotation class MyAnnotation(
                    val intParam: Int,
                    val intArrayParam: IntArray,
                    vararg val intVarArgsParam: Int,
                )
                @MyAnnotation(
                    intParam = 1,
                    intArrayParam = [3, 5, 7],
                    intVarArgsParam = [9, 11, 13],
                )
                class MyClass
                """.trimIndent()
            ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Int) {
                assertThat(annotationValue.valueType.typeName).isEqualTo(TypeName.INT)
                assertThat(annotationValue.hasIntValue()).isTrue()
                assertThat(annotationValue.asInt()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Int) {
                assertThat(annotationValue.valueType.typeName)
                    .isEqualTo(ArrayTypeName.of(TypeName.INT))
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

            val annotation = invocation.processingEnv.requireTypeElement("test.MyClass")
                .getAllAnnotations()
                .single { it.qualifiedName == "test.MyAnnotation" }

            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo("""
                    @test.MyAnnotation(
                        intParam = 1,
                        intArrayParam = {3, 5, 7},
                        intVarArgsParam = {9, 11, 13}
                    )
                    """.removeWhiteSpace())

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
            javaSource = Source.java(
                "test.MyClass",
                """
                package test;
                @interface MyAnnotation {
                    short shortParam();
                    short[] shortArrayParam();
                    short[] shortVarArgsParam(); // There's no varargs in java so use array
                }
                @MyAnnotation(
                    shortParam = (short) 1,
                    shortArrayParam = {(short) 3, (short) 5, (short) 7},
                    shortVarArgsParam = {(short) 9, (short) 11, (short) 13}
                )
                class MyClass {}
                """.trimIndent()
            ) as Source.JavaSource,
            kotlinSource = Source.kotlin(
                "test.MyClass.kt",
                """
                package test
                annotation class MyAnnotation(
                    val shortParam: Short,
                    val shortArrayParam: ShortArray,
                    vararg val shortVarArgsParam: Short,
                )
                @MyAnnotation(
                    shortParam = 1,
                    shortArrayParam = [3, 5, 7],
                    shortVarArgsParam = [9, 11, 13],
                )
                class MyClass
                """.trimIndent()
            ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Short) {
                assertThat(annotationValue.valueType.typeName).isEqualTo(TypeName.SHORT)
                assertThat(annotationValue.hasShortValue()).isTrue()
                assertThat(annotationValue.asShort()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Short) {
                assertThat(annotationValue.valueType.typeName)
                    .isEqualTo(ArrayTypeName.of(TypeName.SHORT))
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

            val annotation = invocation.processingEnv.requireTypeElement("test.MyClass")
                .getAllAnnotations()
                .single { it.qualifiedName == "test.MyAnnotation" }

            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo("""
                    @test.MyAnnotation(
                        shortParam = 1,
                        shortArrayParam = {3, 5, 7},
                        shortVarArgsParam = {9, 11, 13}
                    )
                    """.removeWhiteSpace())

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
            javaSource = Source.java(
                "test.MyClass",
                """
                package test;
                @interface MyAnnotation {
                    long longParam();
                    long[] longArrayParam();
                    long[] longVarArgsParam(); // There's no varargs in java so use array
                }
                @MyAnnotation(
                    longParam = 1L,
                    longArrayParam = {3L, 5L, 7L},
                    longVarArgsParam = {9L, 11L, 13L}
                )
                class MyClass {}
                """.trimIndent()
            ) as Source.JavaSource,
            kotlinSource = Source.kotlin(
                "test.MyClass.kt",
                """
                package test
                annotation class MyAnnotation(
                    val longParam: Long,
                    val longArrayParam: LongArray,
                    vararg val longVarArgsParam: Long,
                )
                @MyAnnotation(
                    longParam = 1L,
                    longArrayParam = [3L, 5L, 7L],
                    longVarArgsParam = [9L, 11L, 13L],
                )
                class MyClass
                """.trimIndent()
            ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Long) {
                assertThat(annotationValue.valueType.typeName).isEqualTo(TypeName.LONG)
                assertThat(annotationValue.hasLongValue()).isTrue()
                assertThat(annotationValue.asLong()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Long) {
                assertThat(annotationValue.valueType.typeName)
                    .isEqualTo(ArrayTypeName.of(TypeName.LONG))
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

            val annotation = invocation.processingEnv.requireTypeElement("test.MyClass")
                .getAllAnnotations()
                .single { it.qualifiedName == "test.MyAnnotation" }

            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo("""
                    @test.MyAnnotation(
                        longParam = 1,
                        longArrayParam = {3, 5, 7},
                        longVarArgsParam = {9, 11, 13}
                    )
                    """.removeWhiteSpace())

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
            javaSource = Source.java(
                "test.MyClass",
                """
                package test;
                @interface MyAnnotation {
                    float floatParam();
                    float[] floatArrayParam();
                    float[] floatVarArgsParam(); // There's no varargs in java so use array
                }
                @MyAnnotation(
                    floatParam = 1.1F,
                    floatArrayParam = {3.1F, 5.1F, 7.1F},
                    floatVarArgsParam = {9.1F, 11.1F, 13.1F}
                )
                class MyClass {}
                """.trimIndent()
            ) as Source.JavaSource,
            kotlinSource = Source.kotlin(
                "test.MyClass.kt",
                """
                package test
                annotation class MyAnnotation(
                    val floatParam: Float,
                    val floatArrayParam: FloatArray,
                    vararg val floatVarArgsParam: Float,
                )
                @MyAnnotation(
                    floatParam = 1.1F,
                    floatArrayParam = [3.1F, 5.1F, 7.1F],
                    floatVarArgsParam = [9.1F, 11.1F, 13.1F],
                )
                class MyClass
                """.trimIndent()
            ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Float) {
                assertThat(annotationValue.valueType.typeName).isEqualTo(TypeName.FLOAT)
                assertThat(annotationValue.hasFloatValue()).isTrue()
                assertThat(annotationValue.asFloat()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Float) {
                assertThat(annotationValue.valueType.typeName)
                    .isEqualTo(ArrayTypeName.of(TypeName.FLOAT))
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

            val annotation = invocation.processingEnv.requireTypeElement("test.MyClass")
                .getAllAnnotations()
                .single { it.qualifiedName == "test.MyAnnotation" }

            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo("""
                    @test.MyAnnotation(
                        floatParam = 1.1f,
                        floatArrayParam = {3.1f, 5.1f, 7.1f},
                        floatVarArgsParam = {9.1f, 11.1f, 13.1f}
                    )
                    """.removeWhiteSpace())

            val floatParam = annotation.getAnnotationValue("floatParam")
            checkSingleValue(floatParam, 1.1F)

            val floatArrayParam = annotation.getAnnotationValue("floatArrayParam")
            checkListValues(floatArrayParam, 3.1F, 5.1F, 7.1F)

            val floatVarArgsParam = annotation.getAnnotationValue("floatVarArgsParam")
            checkListValues(floatVarArgsParam, 9.1F, 11.1F, 13.1F)
        }
    }

    @Test
    fun testDoubleValue() {
        runTest(
            javaSource = Source.java(
                "test.MyClass",
                """
                package test;
                @interface MyAnnotation {
                    double doubleParam();
                    double[] doubleArrayParam();
                    double[] doubleVarArgsParam(); // There's no varargs in java so use array
                }
                @MyAnnotation(
                    doubleParam = 1.1,
                    doubleArrayParam = {3.1, 5.1, 7.1},
                    doubleVarArgsParam = {9.1, 11.1, 13.1}
                )
                class MyClass {}
                """.trimIndent()
            ) as Source.JavaSource,
            kotlinSource = Source.kotlin(
                "test.MyClass.kt",
                """
                package test
                annotation class MyAnnotation(
                    val doubleParam: Double,
                    val doubleArrayParam: DoubleArray,
                    vararg val doubleVarArgsParam: Double,
                )
                @MyAnnotation(
                    doubleParam = 1.1,
                    doubleArrayParam = [3.1, 5.1, 7.1],
                    doubleVarArgsParam = [9.1, 11.1, 13.1],
                )
                class MyClass
                """.trimIndent()
            ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Double) {
                assertThat(annotationValue.valueType.typeName).isEqualTo(TypeName.DOUBLE)
                assertThat(annotationValue.hasDoubleValue()).isTrue()
                assertThat(annotationValue.asDouble()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Double) {
                assertThat(annotationValue.valueType.typeName)
                    .isEqualTo(ArrayTypeName.of(TypeName.DOUBLE))
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

            val annotation = invocation.processingEnv.requireTypeElement("test.MyClass")
                .getAllAnnotations()
                .single { it.qualifiedName == "test.MyAnnotation" }

            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo("""
                    @test.MyAnnotation(
                        doubleParam = 1.1,
                        doubleArrayParam = {3.1, 5.1, 7.1},
                        doubleVarArgsParam = {9.1, 11.1, 13.1}
                    )
                    """.removeWhiteSpace())

            val doubleParam = annotation.getAnnotationValue("doubleParam")
            checkSingleValue(doubleParam, 1.1)

            val doubleArrayParam = annotation.getAnnotationValue("doubleArrayParam")
            checkListValues(doubleArrayParam, 3.1, 5.1, 7.1)

            val doubleVarArgsParam = annotation.getAnnotationValue("doubleVarArgsParam")
            checkListValues(doubleVarArgsParam, 9.1, 11.1, 13.1)
        }
    }

    @Test
    fun testByteValue() {
        runTest(
            javaSource = Source.java(
                "test.MyClass",
                """
                package test;
                @interface MyAnnotation {
                    byte byteParam();
                    byte[] byteArrayParam();
                    byte[] byteVarArgsParam(); // There's no varargs in java so use array
                }
                @MyAnnotation(
                    byteParam = (byte) 1,
                    byteArrayParam = {(byte) 3, (byte) 5, (byte) 7},
                    byteVarArgsParam = {(byte) 9, (byte) 11, (byte) 13}
                )
                class MyClass {}
                """.trimIndent()
            ) as Source.JavaSource,
            kotlinSource = Source.kotlin(
                "test.MyClass.kt",
                """
                package test
                annotation class MyAnnotation(
                    val byteParam: Byte,
                    val byteArrayParam: ByteArray,
                    vararg val byteVarArgsParam: Byte,
                )
                @MyAnnotation(
                    byteParam = 1,
                    byteArrayParam = [3, 5, 7],
                    byteVarArgsParam = [9, 11, 13],
                )
                class MyClass
                """.trimIndent()
            ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Byte) {
                assertThat(annotationValue.valueType.typeName).isEqualTo(TypeName.BYTE)
                assertThat(annotationValue.hasByteValue()).isTrue()
                assertThat(annotationValue.asByte()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Byte) {
                assertThat(annotationValue.valueType.typeName)
                    .isEqualTo(ArrayTypeName.of(TypeName.BYTE))
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

            val annotation = invocation.processingEnv.requireTypeElement("test.MyClass")
                .getAllAnnotations()
                .single { it.qualifiedName == "test.MyAnnotation" }

            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo("""
                    @test.MyAnnotation(
                        byteParam = 1,
                        byteArrayParam = {3, 5, 7},
                        byteVarArgsParam = {9, 11, 13}
                    )
                    """.removeWhiteSpace())

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
            javaSource = Source.java(
                "test.MyClass",
                """
                package test;
                @interface MyAnnotation {
                    char charParam();
                    char[] charArrayParam();
                    char[] charVarArgsParam(); // There's no varargs in java so use array
                }
                @MyAnnotation(
                    charParam = '1',
                    charArrayParam = {'2', '3', '4'},
                    charVarArgsParam = {'5', '6', '7'}
                )
                class MyClass {}
                """.trimIndent()
            ) as Source.JavaSource,
            kotlinSource = Source.kotlin(
                "test.MyClass.kt",
                """
                package test
                annotation class MyAnnotation(
                    val charParam: Char,
                    val charArrayParam: CharArray,
                    vararg val charVarArgsParam: Char,
                )
                @MyAnnotation(
                    charParam = '1',
                    charArrayParam = ['2', '3', '4'],
                    charVarArgsParam = ['5', '6', '7'],
                )
                class MyClass
                """.trimIndent()
            ) as Source.KotlinSource
        ) { invocation ->
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: Char) {
                assertThat(annotationValue.valueType.typeName).isEqualTo(TypeName.CHAR)
                assertThat(annotationValue.hasCharValue()).isTrue()
                assertThat(annotationValue.asChar()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: Char) {
                assertThat(annotationValue.valueType.typeName)
                    .isEqualTo(ArrayTypeName.of(TypeName.CHAR))
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

            val annotation = invocation.processingEnv.requireTypeElement("test.MyClass")
                .getAllAnnotations()
                .single { it.qualifiedName == "test.MyAnnotation" }

            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo("""
                    @test.MyAnnotation(
                        charParam = '1',
                        charArrayParam = {'2', '3', '4'},
                        charVarArgsParam = {'5', '6', '7'}
                    )
                    """.removeWhiteSpace())

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
            javaSource = Source.java(
                "test.MyClass",
                """
                package test;
                @interface MyAnnotation {
                    String stringParam();
                    String[] stringArrayParam();
                    String[] stringVarArgsParam(); // There's no varargs in java so use array
                }
                @MyAnnotation(
                    stringParam = "1",
                    stringArrayParam = {"3", "5", "7"},
                    stringVarArgsParam = {"9", "11", "13"}
                )
                class MyClass {}
                """.trimIndent()
            ) as Source.JavaSource,
            kotlinSource = Source.kotlin(
                "test.MyClass.kt",
                """
                package test
                annotation class MyAnnotation(
                    val stringParam: String,
                    val stringArrayParam: Array<String>,
                    vararg val stringVarArgsParam: String,
                )
                @MyAnnotation(
                    stringParam = "1",
                    stringArrayParam = ["3", "5", "7"],
                    stringVarArgsParam = ["9", "11", "13"],
                )
                class MyClass
                """.trimIndent()
            ) as Source.KotlinSource
        ) { invocation ->
            val stringTypeName = TypeName.get(String::class.java)

            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: String) {
                assertThat(annotationValue.valueType.typeName).isEqualTo(stringTypeName)
                assertThat(annotationValue.hasStringValue()).isTrue()
                assertThat(annotationValue.asString()).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: String) {
                assertThat(annotationValue.valueType.typeName)
                    .isEqualTo(ArrayTypeName.of(stringTypeName))
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

            val annotation = invocation.processingEnv.requireTypeElement("test.MyClass")
                .getAllAnnotations()
                .single { it.qualifiedName == "test.MyAnnotation" }

            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo("""
                    @test.MyAnnotation(
                        stringParam = "1",
                        stringArrayParam = {"3", "5", "7"},
                        stringVarArgsParam = {"9", "11", "13"}
                    )
                    """.removeWhiteSpace())

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
            javaSource = Source.java(
                "test.MyClass",
                """
                package test;
                enum MyEnum {V1, V2, V3}
                @interface MyAnnotation {
                    MyEnum enumParam();
                    MyEnum[] enumArrayParam();
                    MyEnum[] enumVarArgsParam(); // There's no varargs in java so use array
                }
                @MyAnnotation(
                    enumParam = MyEnum.V1,
                    enumArrayParam = {MyEnum.V1, MyEnum.V2, MyEnum.V3},
                    enumVarArgsParam = {MyEnum.V3, MyEnum.V2, MyEnum.V1}
                )
                class MyClass {}
                """.trimIndent()
            ) as Source.JavaSource,
            kotlinSource = Source.kotlin(
                "test.MyClass.kt",
                """
                package test
                enum class MyEnum {V1, V2, V3}
                annotation class MyAnnotation(
                    val enumParam: MyEnum,
                    val enumArrayParam: Array<MyEnum>,
                    vararg val enumVarArgsParam: MyEnum,
                )
                @MyAnnotation(
                    enumParam = MyEnum.V1,
                    enumArrayParam = [MyEnum.V1, MyEnum.V2, MyEnum.V3],
                    enumVarArgsParam = [MyEnum.V3, MyEnum.V2, MyEnum.V1],
                )
                class MyClass
                """.trimIndent()
            ) as Source.KotlinSource
        ) { invocation ->
            val myEnumTypeName = ClassName.get("", "test.MyEnum")

            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: String) {
                assertThat(annotationValue.valueType.typeName).isEqualTo(myEnumTypeName)
                assertThat(annotationValue.hasEnumValue()).isTrue()
                assertThat(annotationValue.asEnum().name).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: String) {
                assertThat(annotationValue.valueType.typeName)
                    .isEqualTo(ArrayTypeName.of(myEnumTypeName))
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

            val annotation = invocation.processingEnv.requireTypeElement("test.MyClass")
                .getAllAnnotations()
                .single { it.qualifiedName == "test.MyAnnotation" }

            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo("""
                    @test.MyAnnotation(
                        enumParam = test.MyEnum.V1,
                        enumArrayParam = {test.MyEnum.V1, test.MyEnum.V2, test.MyEnum.V3},
                        enumVarArgsParam = {test.MyEnum.V3, test.MyEnum.V2, test.MyEnum.V1}
                    )
                    """.removeWhiteSpace())

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
            javaSource = Source.java(
                "test.MyClass",
                """
                package test;
                class C1 {}
                class C2 {}
                class C3 {}
                @interface MyAnnotation {
                    Class<?> typeParam();
                    Class<?>[] typeArrayParam();
                    Class<?>[] typeVarArgsParam(); // There's no varargs in java so use array
                }
                @MyAnnotation(
                    typeParam = C1.class,
                    typeArrayParam = {C1.class, C2.class, C3.class},
                    typeVarArgsParam = {C3.class, C2.class, C1.class}
                )
                class MyClass {}
                """.trimIndent()
            ) as Source.JavaSource,
            kotlinSource = Source.kotlin(
                "test.MyClass.kt",
                """
                package test
                class C1
                class C2
                class C3
                annotation class MyAnnotation(
                    val typeParam: kotlin.reflect.KClass<*>,
                    val typeArrayParam: Array<kotlin.reflect.KClass<*>>,
                    vararg val typeVarArgsParam: kotlin.reflect.KClass<*>,
                )
                @MyAnnotation(
                    typeParam = C1::class,
                    typeArrayParam = [C1::class, C2::class, C3::class],
                    typeVarArgsParam = [C3::class, C2::class, C1::class],
                )
                class MyClass
                """.trimIndent()
            ) as Source.KotlinSource
        ) { invocation ->
            val classTypeName = ParameterizedTypeName.get(
                ClassName.get(Class::class.java),
                WildcardTypeName.subtypeOf(TypeName.OBJECT)
            )
            val kClassTypeName = ParameterizedTypeName.get(
                ClassName.get(kotlin.reflect.KClass::class.java),
                WildcardTypeName.subtypeOf(TypeName.OBJECT)
            )
            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: String) {
                // TODO(bcorso): Consider making the value types match in this case.
                if (!invocation.isKsp || (sourceKind == SourceKind.JAVA && !isPreCompiled)) {
                    assertThat(annotationValue.valueType.typeName).isEqualTo(classTypeName)
                } else {
                    assertThat(annotationValue.valueType.typeName).isEqualTo(kClassTypeName)
                }
                assertThat(annotationValue.hasTypeValue()).isTrue()
                assertThat(annotationValue.asType().typeElement?.name).isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: String) {
                // TODO(bcorso): Consider making the value types match in this case.
                if (!invocation.isKsp || (sourceKind == SourceKind.JAVA && !isPreCompiled)) {
                    assertThat(annotationValue.valueType.typeName)
                        .isEqualTo(ArrayTypeName.of(classTypeName))
                } else {
                    assertThat(annotationValue.valueType.typeName)
                        .isEqualTo(ArrayTypeName.of(kClassTypeName))
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

            val annotation = invocation.processingEnv.requireTypeElement("test.MyClass")
                .getAllAnnotations()
                .single { it.qualifiedName == "test.MyAnnotation" }

            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo("""
                    @test.MyAnnotation(
                        typeParam = test.C1.class,
                        typeArrayParam = {test.C1.class, test.C2.class, test.C3.class},
                        typeVarArgsParam = {test.C3.class, test.C2.class, test.C1.class}
                    )
                    """.removeWhiteSpace())

            val typeParam = annotation.getAnnotationValue("typeParam")
            checkSingleValue(typeParam, "C1")

            val typeArrayParam = annotation.getAnnotationValue("typeArrayParam")
            checkListValues(typeArrayParam, "C1", "C2", "C3")

            val typeVarArgsParam = annotation.getAnnotationValue("typeVarArgsParam")
            checkListValues(typeVarArgsParam, "C3", "C2", "C1")
        }
    }

    @Test
    fun testAnnotationValue() {
        runTest(
            javaSource = Source.java(
                "test.MyClass",
                """
                package test;
                @interface A {
                    String value();
                }
                @interface MyAnnotation {
                    A annotationParam();
                    A[] annotationArrayParam();
                    A[] annotationVarArgsParam(); // There's no varargs in java so use array
                }
                @MyAnnotation(
                    annotationParam = @A("1"),
                    annotationArrayParam = {@A("3"), @A("5"), @A("7")},
                    annotationVarArgsParam = {@A("9"), @A("11"), @A("13")}
                )
                class MyClass {}
                """.trimIndent()
            ) as Source.JavaSource,
            kotlinSource = Source.kotlin(
                "test.MyClass.kt",
                """
                package test
                annotation class A(val value: String)
                annotation class MyAnnotation(
                    val annotationParam: A,
                    val annotationArrayParam: Array<A>,
                    vararg val annotationVarArgsParam: A,
                )
                @MyAnnotation(
                    annotationParam = A("1"),
                    annotationArrayParam = [A("3"), A("5"), A("7")],
                    annotationVarArgsParam = [A("9"), A("11"), A("13")],
                )
                class MyClass
                """.trimIndent()
            ) as Source.KotlinSource
        ) { invocation ->
            val aTypeName = ClassName.get("", "test.A")

            fun checkSingleValue(annotationValue: XAnnotationValue, expectedValue: String) {
                assertThat(annotationValue.valueType.typeName).isEqualTo(aTypeName)
                assertThat(annotationValue.hasAnnotationValue()).isTrue()
                assertThat(annotationValue.asAnnotation().getAsString("value"))
                    .isEqualTo(expectedValue)
            }

            fun checkListValues(annotationValue: XAnnotationValue, vararg expectedValues: String) {
                assertThat(annotationValue.valueType.typeName)
                    .isEqualTo(ArrayTypeName.of(aTypeName))
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

            val annotation = invocation.processingEnv.requireTypeElement("test.MyClass")
                .getAllAnnotations()
                .single { it.qualifiedName == "test.MyAnnotation" }

            // Compare the AnnotationSpec string ignoring whitespace
            assertThat(annotation.toAnnotationSpec().toString().removeWhiteSpace())
                .isEqualTo("""
                    @test.MyAnnotation(
                        annotationParam = @test.A("1"),
                        annotationArrayParam = {@test.A("3"), @test.A("5"), @test.A("7")},
                        annotationVarArgsParam = {@test.A("9"),@test.A("11"),@test.A("13")}
                    )
                    """.removeWhiteSpace())

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

    enum class SourceKind { JAVA, KOTLIN }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "isPreCompiled_{0}_sourceKind_{1}")
        fun params(): List<Array<Any>> {
            val isPreCompiledValues = arrayOf(false, true)
            val sourceKindValues = arrayOf(SourceKind.JAVA, SourceKind.KOTLIN)
            return isPreCompiledValues.flatMap { isPreCompiled ->
                sourceKindValues.map { sourceKind ->
                    arrayOf(isPreCompiled, sourceKind)
                }
            }
        }
    }
}
