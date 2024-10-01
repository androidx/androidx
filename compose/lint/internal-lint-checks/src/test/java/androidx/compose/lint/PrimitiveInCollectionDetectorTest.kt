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

import androidx.compose.lint.test.kotlinAndBytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PrimitiveInCollectionDetectorTest(parameters: Parameters) : LintDetectorTest() {

    private val type = parameters.type
    private val value = parameters.value
    private val longType = parameters.longType
    private val collectionType = parameters.collectionType

    override fun getDetector(): Detector = PrimitiveInCollectionDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(PrimitiveInCollectionDetector.ISSUE)

    private val tild = "~".repeat(type.length)
    private val vtild = "~".repeat(value.length)

    @Test
    fun functionWithSetReturnType() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo(): Set<$type>? = null
                        fun bar(): Set<String>? = null // no warning for this
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test.kt:4: Error: return type Set<$longType> of foo: replace with ${collectionType}Set [PrimitiveInCollection]
                        fun foo(): Set<$type>? = null
                                   ~~~~$tild~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun functionWithSetClassReturnType() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo(): HashSet<$type>? = null
                        fun bar(): HashSet<String>? = null // no warning for this
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test.kt:4: Error: return type HashSet<$longType> of foo: replace with ${collectionType}Set [PrimitiveInCollection]
                        fun foo(): HashSet<$type>? = null
                                   ~~~~~~~~$tild~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun functionWithListReturnType() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo(): List<$type>? = null
                        fun bar(): List<String>? = null // no warning for this
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test.kt:4: Error: return type List<$longType> of foo: replace with ${collectionType}List [PrimitiveInCollection]
                        fun foo(): List<$type>? = null
                                   ~~~~$tild~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun functionWithListClassReturnType() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo(): ArrayList<$type>? = null
                        fun bar(): ArrayList<String>? = null // no warning for this
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test.kt:4: Error: return type ArrayList<$longType> of foo: replace with ${collectionType}List [PrimitiveInCollection]
                        fun foo(): ArrayList<$type>? = null
                                   ~~~~~~~~~~$tild~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun functionWithMapReturnType() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo(): Map<$type, Any>? = null
                        fun bar(): Map<String, Any>? = null // no warning for this
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test.kt:4: Error: return type Map<$longType, Object> of foo: replace with ${collectionType}ObjectMap [PrimitiveInCollection]
                        fun foo(): Map<$type, Any>? = null
                                   ~~~~$tild~~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun functionWithMapClassReturnType() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo(): HashMap<$type, Any>? = null
                        fun bar(): HashMap<String, Any>? = null // no warning for this
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test.kt:4: Error: return type HashMap<$longType, Object> of foo: replace with ${collectionType}ObjectMap [PrimitiveInCollection]
                        fun foo(): HashMap<$type, Any>? = null
                                   ~~~~~~~~$tild~~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun variableSet() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo() {
                            val s = mutableSetOf($value)
                            println(s)
                        }
                        fun bar() {
                             // no warning for this
                            val s = mutableSetOf("Hello", "World")
                            println(s)
                        }
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test.kt:5: Error: variable s with type Set<$longType>: replace with ${collectionType}Set [PrimitiveInCollection]
                            val s = mutableSetOf($value)
                            ~~~~~~~~~~~~~~~~~~~~~$vtild~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun variableHashSet() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo() {
                            val s = HashSet<$type>()
                            println(s)
                        }
                        fun bar() {
                             // no warning for this
                            val s = HashSet<String>()
                            println(s)
                        }
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test.kt:5: Error: variable s with type HashSet<$longType>: replace with ${collectionType}Set [PrimitiveInCollection]
                            val s = HashSet<$type>()
                            ~~~~~~~~~~~~~~~~$tild~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun variableList() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo() {
                            val s = mutableListOf($value)
                            println(s)
                        }
                        fun bar() {
                             // no warning for this
                            val s = mutableListOf("Hello", "World")
                            println(s)
                        }
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test.kt:5: Error: variable s with type List<$longType>: replace with ${collectionType}List [PrimitiveInCollection]
                            val s = mutableListOf($value)
                            ~~~~~~~~~~~~~~~~~~~~~~$vtild~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun variableArrayList() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo() {
                            val s = ArrayList<$type>()
                            println(s)
                        }
                        fun bar() {
                             // no warning for this
                            val s = ArrayList<String>()
                            println(s)
                        }
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test.kt:5: Error: variable s with type ArrayList<$longType>: replace with ${collectionType}List [PrimitiveInCollection]
                            val s = ArrayList<$type>()
                            ~~~~~~~~~~~~~~~~~~$tild~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun variableHashMap() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo() {
                            val s = mutableMapOf("Hello" to $value)
                            println(s)
                        }
                        fun bar() {
                             // no warning for this
                            val s = mutableMapOf("Hello" to "World")
                            println(s)
                        }
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test.kt:5: Error: variable s with type Map<String, $longType>: replace with Object${collectionType}Map [PrimitiveInCollection]
                            val s = mutableMapOf("Hello" to $value)
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~$vtild~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun variableMap() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo() {
                            val s = HashMap<$type, String>()
                            println(s)
                        }
                        fun bar() {
                             // no warning for this
                            val s = HashMap<String, String>()
                            println(s)
                        }
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test.kt:5: Error: variable s with type HashMap<$longType, String>: replace with ${collectionType}ObjectMap [PrimitiveInCollection]
                            val s = HashMap<$type, String>()
                            ~~~~~~~~~~~~~~~~$tild~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun variablePrimitivePrimitiveMap() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo() {
                            val s = HashMap<$type, $type>()
                            println(s)
                        }
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/test.kt:5: Error: variable s with type HashMap<$longType, $longType>: replace with ${collectionType}${collectionType}Map [PrimitiveInCollection]
                            val s = HashMap<$type, $type>()
                            ~~~~~~~~~~~~~~~~$tild~~$tild~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun dataClassConstructor() {
        lint()
            .files(
                ContainsIntClass,
                SimpleValueClass.bytecode,
                kotlin(
                    """
                        package androidx.compose.lint

                        data class Foo(val foo: Set<$type>)
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/lint/Foo.kt:4: Error: constructor Foo has parameter foo with type Set<$longType>: replace with ${collectionType}Set [PrimitiveInCollection]
                        data class Foo(val foo: Set<$type>)
                                                ~~~~$tild~
src/androidx/compose/lint/Foo.kt:4: Error: field foo with type Set<$longType>: replace with ${collectionType}Set [PrimitiveInCollection]
                        data class Foo(val foo: Set<$type>)
                                                ~~~~$tild~
src/androidx/compose/lint/Foo.kt:4: Error: return type Set<$longType> of getFoo: replace with ${collectionType}Set [PrimitiveInCollection]
                        data class Foo(val foo: Set<$type>)
                                                ~~~~$tild~
3 errors, 0 warnings
            """
            )
    }

    @Test
    fun hiddenVariableInDeconstruction() {
        // Regression test from b/328122546
        lint()
            .files(
                kotlin(
                    """
                        package androidx.compose.lint

                        fun foo(value: Any) {
                            val list = value as List<Any>
                            val (first, second, third) = (list as List<$type>)
                            println(first)
                            println(second)
                            println(third)
                        }
                """
                )
            )
            .run()
            .expectClean()
    }

    data class Parameters(
        val type: String,
        val value: String,
        val longType: String,
        val collectionType: String
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() =
            listOf(
                Parameters("Char", "'a'", "Character", "Int"),
                Parameters("Byte", "0.toByte()", "Byte", "Int"),
                Parameters("UByte", "0u.toUByte()", "UByte", "Int"),
                Parameters("Short", "0.toShort()", "Short", "Int"),
                Parameters("UShort", "0u.toUShort()", "UShort", "Int"),
                Parameters("Int", "1", "Integer", "Int"),
                Parameters("UInt", "1u", "UInt", "Int"),
                Parameters("Long", "1L", "Long", "Long"),
                Parameters("ULong", "1uL", "ULong", "Long"),
                Parameters("Float", "1f", "Float", "Float"),
                Parameters("Double", "1.0", "Double", "Float"),
                Parameters("ContainsInt", "ContainsInt(0)", "ContainsInt", "Int"),
                Parameters(
                    "test.SimpleValueClass",
                    "test.SimpleValueClass(0)",
                    "SimpleValueClass",
                    "Int"
                )
            )

        val ContainsIntClass =
            kotlin(
                """
            package androidx.compose.lint

            @JvmInline value class ContainsInt(val value: Int) {
                 companion object {
                    val companionField = 0
                 }
            }
            """
                    .trimIndent()
            )

        val SimpleValueClass =
            kotlinAndBytecodeStub(
                filename = "SimpleValueClass.kt",
                filepath = "test",
                checksum = 0x8b98db3a,
                source =
                    """
                package test

                @JvmInline
                value class SimpleValueClass(val value: Int)
            """
                        .trimIndent(),
                """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgkucSTcxLKcrPTKnQS87PLcgvTtXL
            ycwrEWILSS0u8S5RYtBiAAC6aGYHOQAAAA==
            """,
                """
            test/SimpleValueClass.class:
            H4sIAAAAAAAA/31U31MbVRT+7s0m2Ww2sAktkAW1P7RN+NGkWGuVghS0djEU
            hRql+LKEHVhINpjdZPrIm/4FPviiow++8KAzCoydcRDf/Jscx3M3m4RZMs5k
            9t577vnO+c53zs3f//7+B4B7+ILhqme5XmHdrh1UrbJZbVpLVdN142AM2p7Z
            MgtV09kprG7tWRUvjgiDvGN5viNDJJc3GKKt9okZKuKQE+BIMEjeru0yjJT6
            xp9lSHn1da9hOzvT4o6I5Ix8qZexfUd+I2HbYtOubluNOAYZYg9tx/bmfSpl
            FWlkFGgYIlQ4Y85nOSfjKqHMgwPL2WaYzl3OeJlEkHBWxQhGRfwsw3g/thcd
            x4TjuHBc+n/HV4Xja6RrRw+GK7k+Sqi4jhvC9yaJazZ2iipSGFBI7Vsk5q7p
            7i7Vt61ATInoUWvSvSiG41k7QrUJStXxVjGFvIJJTKvIiR1HgSFpfdk0q24Q
            ajhnlMKDMJt/zqA0na36C99LxZuICfQ9Goe6t2s1GDKXUaR8O7Todr+gKu5i
            RsR5t11CWYEkuqlV6o7rNZoVr94IaMmd3Ay66EX/MRMj8VAEfETz2WJQL5RW
            JK45wxCl8IO74jND1Zb2617Vdgp7rVphuVUzHDpYxDzduVixPHPb9Eyy8Vor
            Qu+IiU9CfEBZ9sn+whYnSsC3KfCPZ4c3FD7KFa6dHSr045qscDlKa5LWGK0D
            tHL5/KuF0bPDGV5ki4OZmMZ1Xoyc/xCTZEmLLuuaTOfEjKwpujTKiuzJX9+0
            b5OauqxpKeFNNubbBgihaYNk07q2tJZZS3ejysREl+SYFj//mnFBlcqnAobC
            It7Z96gbYlwYBkukxtNmbctqPDO3qpZoc71iVstmwxbnwJha98zK/op5EJyV
            9XqzUbEe2+KQXWs6nl2zyrZr0+0jx6l7pmdTh6n7nBouWGTEXwntRAOjiJGl
            TKeCEJjW6MSvUI5ow/EZfWO+UcLnPsB3QJJ21BXxRgLw2+Qt7rIvoW2c4Epm
            +Bi6foxXtPwxrh3j9Z/9zL0gWbzhc2Di5QVBbgUMZMHgFLfDGLmbmN5TgLnZ
            Ya2f4s5RCBDtJpnqlhlKUgxjeknouQSYVapODJ0++Sf4t4hGjibPwI/x1pw+
            /p04Sm29NugbB0/8g4F2yGEyEiygIXb3SSpB4AHeCYKLvgivhCA0eYrZHqM2
            PBEwEjsfrnHx8gL4fABXJk4wNzH2G5Rf+vauHUvpxlL8IWAUc74r5rVAG66H
            VeHtkdGyeA8Lgfdt0kTcJV6Cb+gnWAz3K4ElH5QW/2DhfnWmjPWZrCzexweh
            +pL62PeISz9BivTEjpLYCxe1SuJxIHUSH/r1cTz33T/FJq27tHtCq0HQ5U1E
            DHxkoGRgBU9pi1UDH+OTTTAXa1jfxJAL1cUzF3H/O+8i7yLqIubigW+5T+/K
            xYyLKRc5F9d9Y8rFwH8DJohDBwgAAA==
            """
            )
    }
}
