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

/* ktlint-disable max-line-length */
@RunWith(Parameterized::class)
class PrimitiveInCollectionDetectorTest(
    parameters: Parameters
) : LintDetectorTest() {

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
        lint().files(
            ContainsIntClass,
            SimpleValueClass.bytecode,
            kotlin(
                """
                        package androidx.compose.lint

                        fun foo(): Set<$type>? = null
                        fun bar(): Set<String>? = null // no warning for this
                        """
            )
        ).run().expect(
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
        lint().files(
            ContainsIntClass,
            SimpleValueClass.bytecode,
            kotlin(
                """
                        package androidx.compose.lint

                        fun foo(): HashSet<$type>? = null
                        fun bar(): HashSet<String>? = null // no warning for this
                        """
            )
        ).run().expect(
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
        lint().files(
            ContainsIntClass,
            SimpleValueClass.bytecode,
            kotlin(
                """
                        package androidx.compose.lint

                        fun foo(): List<$type>? = null
                        fun bar(): List<String>? = null // no warning for this
                        """
            )
        ).run().expect(
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
        lint().files(
            ContainsIntClass,
            SimpleValueClass.bytecode,
            kotlin(
                """
                        package androidx.compose.lint

                        fun foo(): ArrayList<$type>? = null
                        fun bar(): ArrayList<String>? = null // no warning for this
                        """
            )
        ).run().expect(
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
        lint().files(
            ContainsIntClass,
            SimpleValueClass.bytecode,
            kotlin(
                """
                        package androidx.compose.lint

                        fun foo(): Map<$type, Any>? = null
                        fun bar(): Map<String, Any>? = null // no warning for this
                        """
            )
        ).run().expect(
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
        lint().files(
            ContainsIntClass,
            SimpleValueClass.bytecode,
            kotlin(
                """
                        package androidx.compose.lint

                        fun foo(): HashMap<$type, Any>? = null
                        fun bar(): HashMap<String, Any>? = null // no warning for this
                        """
            )
        ).run().expect(
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
        lint().files(
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
        ).run().expect(
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
        lint().files(
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
        ).run().expect(
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
        lint().files(
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
        ).run().expect(
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
        lint().files(
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
        ).run().expect(
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
        lint().files(
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
        ).run().expect(
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
        lint().files(
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
        ).run().expect(
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
        lint().files(
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
        ).run().expect(
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
        lint().files(
            ContainsIntClass,
            SimpleValueClass.bytecode,
            kotlin(
                """
                        package androidx.compose.lint

                        data class Foo(val foo: Set<$type>)
                        """
            )
        ).run().expect(
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

    data class Parameters(
        val type: String,
        val value: String,
        val longType: String,
        val collectionType: String
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = listOf(
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
            Parameters("test.SimpleValueClass", "test.SimpleValueClass(0)", "SimpleValueClass", "Int")
        )

        val ContainsIntClass = kotlin(
            """
            package androidx.compose.lint

            @JvmInline value class ContainsInt(val value: Int)
            """.trimIndent()
        )

        val SimpleValueClass = kotlinAndBytecodeStub(
            filename = "SimpleValueClass.kt",
            filepath = "test",
            checksum = 0xc2548512,
            source = """
                package test

                @JvmInline
                value class SimpleValueClass(val value: Int)
            """.trimIndent(),
            """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAA/2NgYGBmYGBgBGIOBihQYtBiAABw+ypgGAAAAA==
            """,
            """
            test/SimpleValueClass.class:
            H4sIAAAAAAAA/31U31MbVRT+7s2vzWaBTaBAFtT+0DbhR5NirVUKUtDapaGt
            UKMUfVjCTlhINpjdZOob44v+BT74ouOLfeBBZxTQzjhI3/ybHMdzN5uEWTLO
            ZPbee+75zvnOd87N3//+8SeAm/ic4YJrOm5uzaruVcyiUWmYSxXDcWJgDOqO
            0TRyFcMu5x5t7pglN4YQg1Q2Xc+RIZTJ6gyRZuvEdAUxSHFwxBnC7rblMIwU
            esafZehza2tu3bLL0+KOiGT0bKGbsXVHfiNB22LDqmyZ9RgGGKJ3LNty5z0q
            RQVJpGSoGCRUMGPGYzkn4QKhjL09095imM6cz3iehJ9wVsEIRkX8NMN4L7Zn
            HceE47hwXPp/x1eF42uka1sPhqFMDyUUXMJl4XuFxDXq5byCPvTLpPZVEnPb
            cLaXalumL2aY6FFrkt0ouu2aZaHaBKVqeyuYQlbGJKYVZMSOI8eQML9oGBXH
            DzWc0QvBQZjNPmWQG/Zm7ZnnpeBNRAX6Jo1Dzd026wyp8yhSvhVadLtXUAU3
            MCPivNsqoSgjLLqplmq249YbJbdW92lJ7dwMmuhF7zETI3FHBLxL89lkUM6U
            lieuGV0XpfC9G+IzQ9UWdmtuxbJzO81qbrlZ1W06mMQ82b5YMV1jy3ANsvFq
            M0TviImPJD6gLLtkf2aJEyXgWxT495P9yzIf5TJXT/Zl+nFVkrkUoTVBa5TW
            flpD0unXC6Mn+zM8zxZTqajKNZ4PvTxmJ/unP0bDUliNLGuqRMb4jKTKWniU
            5dn9l9+GvNuEqiyrap+AkI15tn5CqOoA2dSOLammVpOt0HSWiI4WlqJq7PQb
            xlu5vuJhYpM+fR4dFAWQKFSW+sCr/nHF+LJcrzXE4xkMqn1916W2ibliGCiQ
            bA8b1U2z/sTYrJhiHmolo1I06pY4+8a+Ndco7a4Ye/5ZXqs16iXzniUO6dWG
            7VpVs2g5Ft3ete2aa7gWjQKNCafJEMRS4j+HdqLTEUTJ8gmdcqITtEYmfoV8
            QBuOT+kb9YyDWPcAngMStAPi4jH54LfJW9ylX0BdP8JQavgQmnaIV9TsIS4e
            4vWfvczdIGm84XFg4on6Qa76DCTB4BjXghipk5geno+50matHeP6QQAQ6SSZ
            6pQZSJIPYrpJ6F35mEdUnZhObfIv8O8QCR1MnoAf4q05bfx7cQy39HpK3xh4
            /B/0t0IOk5FgPg2xu0VSCQK38Y4fXPRFeMUFocljzHYZteBxn5HYeXCViyfq
            w+d9uDxxhLmJsd8g/9Kzd61YcieW7A0Bo5jzHTEv+tpwLagKb42MmsZ7WPC9
            r5Em4i7+AnxdO8JisF9xLHmgpPirC/arPWWsx2Sl8T4+CNSX0MZ+QCz8E8Kh
            rtgREnvhrFYJ3POlTuBDrz6ODc+9iM9o3aXdfVp1gi5vIKTjgY6CjhU8pC0e
            6XiMjzbAHKxibQNDDhQHTxzEvO+8g6yDiIOog9ue5Ra9KwczDqYcZBxc8ox9
            DvodfPwfuO9oETMIAAA=
            """
        )
    }
}
