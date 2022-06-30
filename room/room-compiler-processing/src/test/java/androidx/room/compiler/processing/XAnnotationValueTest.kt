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
                "MyClass",
                """
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
                "MyClass.kt",
                """
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
            val annotation = invocation.processingEnv.requireTypeElement("MyClass")
                .getAllAnnotations()
                .single { it.name == "MyAnnotation" }

            val booleanParam = annotation.getAnnotationValue("booleanParam")
            assertThat(booleanParam.hasBooleanValue()).isTrue()
            assertThat(booleanParam.asBoolean()).isEqualTo(true)

            val booleanArrayParam = annotation.getAnnotationValue("booleanArrayParam")
            assertThat(booleanArrayParam.hasBooleanListValue()).isTrue()
            assertThat(booleanArrayParam.asBooleanList())
                .containsExactly(true, false, true)
                .inOrder()

            val booleanVarArgsParam = annotation.getAnnotationValue("booleanVarArgsParam")
            assertThat(booleanVarArgsParam.hasBooleanListValue()).isTrue()
            assertThat(booleanVarArgsParam.asBooleanList())
                .containsExactly(false, true, false)
                .inOrder()
        }
    }

    @Test
    fun testIntValue() {
        runTest(
            javaSource = Source.java(
                "MyClass",
                """
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
                "MyClass.kt",
                """
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
            val annotation = invocation.processingEnv.requireTypeElement("MyClass")
                .getAllAnnotations()
                .single { it.name == "MyAnnotation" }

            val intParam = annotation.getAnnotationValue("intParam")
            assertThat(intParam.hasIntValue()).isTrue()
            assertThat(intParam.asInt()).isEqualTo(1)

            val intArrayParam = annotation.getAnnotationValue("intArrayParam")
            assertThat(intArrayParam.hasIntListValue()).isTrue()
            assertThat(intArrayParam.asIntList())
                .containsExactly(3, 5, 7)
                .inOrder()

            val intVarArgsParam = annotation.getAnnotationValue("intVarArgsParam")
            assertThat(intVarArgsParam.hasIntListValue()).isTrue()
            assertThat(intVarArgsParam.asIntList())
                .containsExactly(9, 11, 13)
                .inOrder()
        }
    }

    @Test
    fun testShortValue() {
        runTest(
            javaSource = Source.java(
                "MyClass",
                """
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
                "MyClass.kt",
                """
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
            val annotation = invocation.processingEnv.requireTypeElement("MyClass")
                .getAllAnnotations()
                .single { it.name == "MyAnnotation" }

            val shortParam = annotation.getAnnotationValue("shortParam")
            assertThat(shortParam.hasShortValue()).isTrue()
            assertThat(shortParam.asShort()).isEqualTo(1)

            val shortArrayParam = annotation.getAnnotationValue("shortArrayParam")
            assertThat(shortArrayParam.hasShortListValue()).isTrue()
            assertThat(shortArrayParam.asShortList())
                .containsExactly(3.toShort(), 5.toShort(), 7.toShort())
                .inOrder()

            val shortVarArgsParam = annotation.getAnnotationValue("shortVarArgsParam")
            assertThat(shortVarArgsParam.hasShortListValue()).isTrue()
            assertThat(shortVarArgsParam.asShortList())
                .containsExactly(9.toShort(), 11.toShort(), 13.toShort())
                .inOrder()
        }
    }

    @Test
    fun testLongValue() {
        runTest(
            javaSource = Source.java(
                "MyClass",
                """
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
                "MyClass.kt",
                """
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
            val annotation = invocation.processingEnv.requireTypeElement("MyClass")
                .getAllAnnotations()
                .single { it.name == "MyAnnotation" }

            val longParam = annotation.getAnnotationValue("longParam")
            assertThat(longParam.hasLongValue()).isTrue()
            assertThat(longParam.asLong()).isEqualTo(1L)

            val longArrayParam = annotation.getAnnotationValue("longArrayParam")
            assertThat(longArrayParam.hasLongListValue()).isTrue()
            assertThat(longArrayParam.asLongList())
                .containsExactly(3L, 5L, 7L)
                .inOrder()

            val longVarArgsParam = annotation.getAnnotationValue("longVarArgsParam")
            assertThat(longVarArgsParam.hasLongListValue()).isTrue()
            assertThat(longVarArgsParam.asLongList())
                .containsExactly(9L, 11L, 13L)
                .inOrder()
        }
    }

    @Test
    fun testFloatValue() {
        runTest(
            javaSource = Source.java(
                "MyClass",
                """
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
                "MyClass.kt",
                """
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
            val annotation = invocation.processingEnv.requireTypeElement("MyClass")
                .getAllAnnotations()
                .single { it.name == "MyAnnotation" }

            val floatParam = annotation.getAnnotationValue("floatParam")
            assertThat(floatParam.hasFloatValue()).isTrue()
            assertThat(floatParam.asFloat()).isEqualTo(1.1F)

            val floatArrayParam = annotation.getAnnotationValue("floatArrayParam")
            assertThat(floatArrayParam.hasFloatListValue()).isTrue()
            assertThat(floatArrayParam.asFloatList())
                .containsExactly(3.1F, 5.1F, 7.1F)
                .inOrder()

            val floatVarArgsParam = annotation.getAnnotationValue("floatVarArgsParam")
            assertThat(floatVarArgsParam.hasFloatListValue()).isTrue()
            assertThat(floatVarArgsParam.asFloatList())
                .containsExactly(9.1F, 11.1F, 13.1F)
                .inOrder()
        }
    }

    @Test
    fun testDoubleValue() {
        runTest(
            javaSource = Source.java(
                "MyClass",
                """
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
                "MyClass.kt",
                """
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
            val annotation = invocation.processingEnv.requireTypeElement("MyClass")
                .getAllAnnotations()
                .single { it.name == "MyAnnotation" }

            val doubleParam = annotation.getAnnotationValue("doubleParam")
            assertThat(doubleParam.hasDoubleValue()).isTrue()
            assertThat(doubleParam.asDouble()).isEqualTo(1.1)

            val doubleArrayParam = annotation.getAnnotationValue("doubleArrayParam")
            assertThat(doubleArrayParam.hasDoubleListValue()).isTrue()
            assertThat(doubleArrayParam.asDoubleList())
                .containsExactly(3.1, 5.1, 7.1)
                .inOrder()

            val doubleVarArgsParam = annotation.getAnnotationValue("doubleVarArgsParam")
            assertThat(doubleVarArgsParam.hasDoubleListValue()).isTrue()
            assertThat(doubleVarArgsParam.asDoubleList())
                .containsExactly(9.1, 11.1, 13.1)
                .inOrder()
        }
    }

    @Test
    fun testByteValue() {
        runTest(
            javaSource = Source.java(
                "MyClass",
                """
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
                "MyClass.kt",
                """
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
            val annotation = invocation.processingEnv.requireTypeElement("MyClass")
                .getAllAnnotations()
                .single { it.name == "MyAnnotation" }

            val byteParam = annotation.getAnnotationValue("byteParam")
            assertThat(byteParam.hasByteValue()).isTrue()
            assertThat(byteParam.asByte()).isEqualTo(1.toByte())

            val byteArrayParam = annotation.getAnnotationValue("byteArrayParam")
            assertThat(byteArrayParam.hasByteListValue()).isTrue()
            assertThat(byteArrayParam.asByteList())
                .containsExactly(3.toByte(), 5.toByte(), 7.toByte())
                .inOrder()

            val byteVarArgsParam = annotation.getAnnotationValue("byteVarArgsParam")
            assertThat(byteVarArgsParam.hasByteListValue()).isTrue()
            assertThat(byteVarArgsParam.asByteList())
                .containsExactly(9.toByte(), 11.toByte(), 13.toByte())
                .inOrder()
        }
    }

    @Test
    fun testCharValue() {
        runTest(
            javaSource = Source.java(
                "MyClass",
                """
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
                "MyClass.kt",
                """
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
            val annotation = invocation.processingEnv.requireTypeElement("MyClass")
                .getAllAnnotations()
                .single { it.name == "MyAnnotation" }

            val charParam = annotation.getAnnotationValue("charParam")
            assertThat(charParam.hasCharValue()).isTrue()
            assertThat(charParam.asChar()).isEqualTo('1')

            val charArrayParam = annotation.getAnnotationValue("charArrayParam")
            assertThat(charArrayParam.hasCharListValue()).isTrue()
            assertThat(charArrayParam.asCharList())
                .containsExactly('2', '3', '4')
                .inOrder()

            val charVarArgsParam = annotation.getAnnotationValue("charVarArgsParam")
            assertThat(charVarArgsParam.hasCharListValue()).isTrue()
            assertThat(charVarArgsParam.asCharList())
                .containsExactly('5', '6', '7')
                .inOrder()
        }
    }

    @Test
    fun testStringValue() {
        runTest(
            javaSource = Source.java(
                "MyClass",
                """
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
                "MyClass.kt",
                """
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
            val annotation = invocation.processingEnv.requireTypeElement("MyClass")
                .getAllAnnotations()
                .single { it.name == "MyAnnotation" }

            val stringParam = annotation.getAnnotationValue("stringParam")
            assertThat(stringParam.hasStringValue()).isTrue()
            assertThat(stringParam.asString()).isEqualTo("1")

            val stringArrayParam = annotation.getAnnotationValue("stringArrayParam")
            assertThat(stringArrayParam.hasStringListValue()).isTrue()
            assertThat(stringArrayParam.asStringList())
                .containsExactly("3", "5", "7")
                .inOrder()

            val stringVarArgsParam = annotation.getAnnotationValue("stringVarArgsParam")
            assertThat(stringVarArgsParam.hasStringListValue()).isTrue()
            assertThat(stringVarArgsParam.asStringList())
                .containsExactly("9", "11", "13")
                .inOrder()
        }
    }

    @Test
    fun testEnumValue() {
        runTest(
            javaSource = Source.java(
                "MyClass",
                """
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
                "MyClass.kt",
                """
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
            val annotation = invocation.processingEnv.requireTypeElement("MyClass")
                .getAllAnnotations()
                .single { it.name == "MyAnnotation" }

            val enumParam = annotation.getAnnotationValue("enumParam")
            assertThat(enumParam.hasEnumValue()).isTrue()
            assertThat(enumParam.asEnum().name).isEqualTo("V1")

            val enumArrayParam = annotation.getAnnotationValue("enumArrayParam")
            assertThat(enumArrayParam.hasEnumListValue()).isTrue()
            assertThat(enumArrayParam.asEnumList().map { it.name })
                .containsExactly("V1", "V2", "V3")
                .inOrder()

            val enumVarArgsParam = annotation.getAnnotationValue("enumVarArgsParam")
            assertThat(enumVarArgsParam.hasEnumListValue()).isTrue()
            assertThat(enumVarArgsParam.asEnumList().map { it.name })
                .containsExactly("V3", "V2", "V1")
                .inOrder()
        }
    }

    @Test
    fun testTypeValue() {
        runTest(
            javaSource = Source.java(
                "MyClass",
                """
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
                "MyClass.kt",
                """
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
            val annotation = invocation.processingEnv.requireTypeElement("MyClass")
                .getAllAnnotations()
                .single { it.name == "MyAnnotation" }

            val typeParam = annotation.getAnnotationValue("typeParam")
            assertThat(typeParam.hasTypeValue()).isTrue()
            assertThat(typeParam.asType().typeElement?.name).isEqualTo("C1")

            val typeArrayParam = annotation.getAnnotationValue("typeArrayParam")
            assertThat(typeArrayParam.hasTypeListValue()).isTrue()
            assertThat(typeArrayParam.asTypeList().map { it.typeElement?.name })
                .containsExactly("C1", "C2", "C3")
                .inOrder()

            val typeVarArgsParam = annotation.getAnnotationValue("typeVarArgsParam")
            assertThat(typeVarArgsParam.hasTypeListValue()).isTrue()
            assertThat(typeVarArgsParam.asTypeList().map { it.typeElement?.name })
                .containsExactly("C3", "C2", "C1")
                .inOrder()
        }
    }

    @Test
    fun testAnnotationValue() {
        runTest(
            javaSource = Source.java(
                "MyClass",
                """
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
                "MyClass.kt",
                """
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
            val annotation = invocation.processingEnv.requireTypeElement("MyClass")
                .getAllAnnotations()
                .single { it.name == "MyAnnotation" }

            val annotationParam = annotation.getAnnotationValue("annotationParam")
            assertThat(annotationParam.hasAnnotationValue()).isTrue()
            assertThat(annotationParam.asAnnotation().getAsString("value")).isEqualTo("1")

            val annotationArrayParam = annotation.getAnnotationValue("annotationArrayParam")
            assertThat(annotationArrayParam.hasAnnotationListValue()).isTrue()
            assertThat(annotationArrayParam.asAnnotationList().map { it.getAsString("value") })
                .containsExactly("3", "5", "7")
                .inOrder()

            val annotationVarArgsParam = annotation.getAnnotationValue("annotationVarArgsParam")
            assertThat(annotationVarArgsParam.hasAnnotationListValue()).isTrue()
            assertThat(annotationVarArgsParam.asAnnotationList().map { it.getAsString("value") })
                .containsExactly("9", "11", "13")
                .inOrder()
        }
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
