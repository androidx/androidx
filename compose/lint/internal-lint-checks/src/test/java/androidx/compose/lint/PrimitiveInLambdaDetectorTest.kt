/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/* ktlint-disable max-line-length */
@RunWith(Parameterized::class)
class PrimitiveInLambdaDetectorTest(
    typeAndValues: Triple<String, String, String>
) : LintDetectorTest() {

    private val type = typeAndValues.first
    private val value = typeAndValues.second
    private val longType = typeAndValues.third

    override fun getDetector(): Detector = PrimitiveInLambdaDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(PrimitiveInLambdaDetector.ISSUE)

    private val tild = "~".repeat(type.length)
    private val vtild = "~".repeat(value.length)

    @Test
    fun functionWithLambdaWithParamWithPrimitiveReturnType() {
        lint().files(
            ContainsIntClass,
            kotlin(
                """
                        package androidx.compose.lint

                        fun foo(block: () -> $type) {
                        }
                        fun bar(block: () -> String) { // no warning for this
                        }
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/test.kt:4: Error: Use a functional interface instead of lambda syntax for lambdas with primitive values in method foo has parameter 'block' with type Function0<$longType>. [PrimitiveInLambda]
                        fun foo(block: () -> $type) {
                                       ~~~~~~$tild
1 errors, 0 warnings
            """
        )
    }

    @Test
    fun functionWithLambdaWithParamWithPrimitiveParameter() {
        lint().files(
            ContainsIntClass,
            kotlin(
                """
                        package androidx.compose.lint

                        fun foo(block: ($type) -> Unit) {
                        }
                        fun bar(block: (String) -> Unit) { // no warning for this
                        }
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/test.kt:4: Error: Use a functional interface instead of lambda syntax for lambdas with primitive values in method foo has parameter 'block' with type Function1<? super $longType, Unit>. [PrimitiveInLambda]
                        fun foo(block: ($type) -> Unit) {
                                       ~$tild~~~~~~~~~
1 errors, 0 warnings
            """
        )
    }

    @Test
    fun inlineMethodsNoWarning() {
        lint().files(
            ContainsIntClass,
            kotlin(
                """
                        package androidx.compose.lint

                        inline fun foo(block: () -> $type) {
                        }
                        """
            )
        ).run().expectClean()
    }

    @Test
    fun inlineMethodWithNoInline() {
        lint().files(
            ContainsIntClass,
            kotlin(
                """
                        package androidx.compose.lint

                        inline fun foo(noinline block: () -> $type) {
                        }
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/test.kt:4: Error: Use a functional interface instead of lambda syntax for lambdas with primitive values in method foo has parameter 'block' with type Function0<$longType>. [PrimitiveInLambda]
                        inline fun foo(noinline block: () -> $type) {
                                                       ~~~~~~$tild
1 errors, 0 warnings
            """
        )
    }

    @Test
    fun functionWithLambdaReturnWithPrimitiveReturnType() {
        lint().files(
            ContainsIntClass,
            kotlin(
                """
                        package androidx.compose.lint

                        fun foo(): () -> $type = { $value }
                        fun bar(): () -> String = { "Hello" } // no warning for this
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/test.kt:4: Error: Use a functional interface instead of lambda syntax for lambdas with primitive values in return type Function0<$longType> of 'foo'. [PrimitiveInLambda]
                        fun foo(): () -> $type = { $value }
                                   ~~~~~~$tild
1 errors, 0 warnings
            """
        )
    }

    @Test
    fun functionWithLambdaReturnWithPrimitiveParameter() {
        lint().files(
            ContainsIntClass,
            kotlin(
                """
                        package androidx.compose.lint

                        fun foo(): ($type) -> Unit = { }
                        fun bar(): (String) -> Unit = { } // no warning for this
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/test.kt:4: Error: Use a functional interface instead of lambda syntax for lambdas with primitive values in return type Function1<$longType, Unit> of 'foo'. [PrimitiveInLambda]
                        fun foo(): ($type) -> Unit = { }
                                   ~$tild~~~~~~~~~
1 errors, 0 warnings
            """
        )
    }

    @Test
    fun propertyWithLambdaWithPrimitives() {
        // I can't figure out how to get the location for the return type of a property with
        // a getter. If the getLocation() starts working for the return type, then this should
        // be changed to highlighting the type instead of the parameter name.
        lint().files(
            ContainsIntClass,
            kotlin(
                """
                        package androidx.compose.lint

                        val foo: ($type) -> $type
                            get() = { it }
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/test.kt:4: Error: Use a functional interface instead of lambda syntax for lambdas with primitive values in return type Function1<$longType, $longType> of 'getFoo'. [PrimitiveInLambda]
                        val foo: ($type) -> $type
                            ~~~
1 errors, 0 warnings
            """
        )
    }

    @Test
    fun fieldWithLambdaWithPrimitives() {
        lint().files(
            ContainsIntClass,
            kotlin(
                """
                        package androidx.compose.lint

                        val foo: ($type) -> $type = { it }
                        class AnotherContainsInt(val value: Int) {
                            // no warning for this because it isn't a value class
                            val bar: (AnotherContainsInt) -> AnotherContainsInt = { it }
                        }
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/AnotherContainsInt.kt:4: Error: Use a functional interface instead of lambda syntax for lambdas with primitive values in return type Function1<$longType, $longType> of 'getFoo'. [PrimitiveInLambda]
                        val foo: ($type) -> $type = { it }
                                 ~$tild~~~~~$tild
1 errors, 0 warnings
            """
        )
    }

    @Test
    fun overrideNotWarning() {
        lint().files(
            ContainsIntClass,
            kotlin(
                """
                        package androidx.compose.lint

                        abstract class BaseClass {
                            abstract val foo: ($type) -> $type
                        }
                        class SubClass : BaseClass() {
                            // no warning for this because it is an override
                            override val foo: ($type) -> $type = { it }
                        }
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/BaseClass.kt:5: Error: Use a functional interface instead of lambda syntax for lambdas with primitive values in return type Function1<$longType, $longType> of 'getFoo'. [PrimitiveInLambda]
                            abstract val foo: ($type) -> $type
                                              ~$tild~~~~~$tild
1 errors, 0 warnings
            """
        )
    }

    @Test
    fun dataClass() {
        lint().files(
            ContainsIntClass,
            kotlin(
                """
                        package androidx.compose.lint

                        data class DataClass(val foo: () -> $type)
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/DataClass.kt:4: Error: Use a functional interface instead of lambda syntax for lambdas with primitive values in constructor DataClass has parameter 'foo' with type Function0<$longType>. [PrimitiveInLambda]
                        data class DataClass(val foo: () -> $type)
                                                      ~~~~~~$tild
src/androidx/compose/lint/DataClass.kt:4: Error: Use a functional interface instead of lambda syntax for lambdas with primitive values in return type Function0<$longType> of 'getFoo'. [PrimitiveInLambda]
                        data class DataClass(val foo: () -> $type)
                                                      ~~~~~~$tild
2 errors, 0 warnings
            """
        )
    }

    @Test
    fun variable() {
        lint().files(
            ContainsIntClass,
            kotlin(
                """
                        package androidx.compose.lint

                        fun foo() {
                            val bar: () -> $type = { $value }
                            val baz: () -> String = { "Hello" }
                            println(bar)
                        }
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/test.kt:5: Error: Use a functional interface instead of lambda syntax for lambdas with primitive values in variable 'bar' with type Function0<? extends $longType>. [PrimitiveInLambda]
                            val bar: () -> $type = { $value }
                                     ~~~~~~$tild
1 errors, 0 warnings
            """
        )
    }

    @Test
    fun collectionOfLambdaWithPrimitive() {
        lint().files(
            ContainsIntClass,
            kotlin(
                """
                        package androidx.compose.lint

                        fun foo() {
                            val list = mutableListOf(mutableListOf({ $value }))
                            list.forEach { println(it.first()()) }
                            list.forEach { l -> println(l.first()()) }
                        }
                        """
            )
        ).run().expect(
            """
src/androidx/compose/lint/test.kt:5: Error: Use a functional interface instead of lambda syntax for lambdas with primitive values in variable 'list' with type List<List<Function0<? extends $longType>>>. [PrimitiveInLambda]
                            val list = mutableListOf(mutableListOf({ $value }))
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~$vtild~~~~
1 errors, 0 warnings
            """
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = listOf(
            Triple("Char", "'a'", "Character"),
            Triple("Byte", "0.toByte()", "Byte"),
            Triple("UByte", "0u.toUByte()", "UByte"),
            Triple("Short", "0.toShort()", "Short"),
            Triple("UShort", "0u.toUShort()", "UShort"),
            Triple("Int", "1", "Integer"),
            Triple("UInt", "1u", "UInt"),
            Triple("Long", "1L", "Long"),
            Triple("ULong", "1uL", "ULong"),
            Triple("Float", "1f", "Float"),
            Triple("Double", "1.0", "Double"),
            Triple("ContainsInt", "ContainsInt(0)", "ContainsInt")
        )

        val ContainsIntClass = kotlin(
            """
            package androidx.compose.lint

            @JvmInline value class ContainsInt(val value: Int)
            """.trimIndent()
        )
    }
}
