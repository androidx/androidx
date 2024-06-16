/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.compose.ui.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Test for [ModifierDeclarationDetector]. */
@RunWith(JUnit4::class)
class ModifierDeclarationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ModifierDeclarationDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            ModifierDeclarationDetector.ModifierFactoryExtensionFunction,
            ModifierDeclarationDetector.ModifierFactoryReturnType,
            ModifierDeclarationDetector.ModifierFactoryUnreferencedReceiver
        )

    // Simplified Density.kt stubs
    private val DensityStub =
        bytecodeStub(
            filename = "Density.kt",
            filepath = "androidx/compose/ui/unit",
            checksum = 0x3ceb3f57,
            """
            package androidx.compose.ui.unit

            interface Density
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuEST8xLKcrPTKnQS87PLcgvTtUr
        zdRLy88X4nTLz3dJLEn0LlFi0GIAAHSOuCo+AAAA
        """,
            """
        androidx/compose/ui/unit/Density.class:
        H4sIAAAAAAAA/4VOTUvDQBB9s9GmjV+pWqg38Qe4benNkyBCoCIoeMlpm6yy
        bbor3U2pt/4uD9KzP0qcqHdn4M17M/DefH69fwAYo0c4V7ZcOlOuZeEWr85r
        WRtZWxPkjbbehLcYREhnaqVkpeyLvJ/OdBFiRITuZO5CZay800GVKqgrglis
        IvamBjoNgEBz3q9NowbMyiGht920E9EXiUiZPfe3m5EYUHMcES4m/z3FQeyb
        /KnLeWDx6OploW9NpQlnD7UNZqGfjDfTSl9b64IKxlnf4gzs4LcETn7wGKc8
        h2y5y93KEWWIM7QzdJAwxV6GfRzkII9DHOUQHqlH9xtDUhD7SQEAAA==
        """
        )

    // Simplified ParentDataModifier.kt / Measurable.kt merged stubs
    private val MeasurableAndParentDataModifierStub =
        bytecodeStub(
            filename = "Measurable.kt",
            filepath = "androidx/compose/ui/layout",
            checksum = 0x1c810a26,
            """
            package androidx.compose.ui.layout

            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Density

            interface ParentDataModifier : Modifier.Element {
                fun Density.modifyParentData(parentData: Any?): Any?
            }

            interface Measurable {
                val parentData: Any?
            }
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuEST8xLKcrPTKnQS87PLcgvTtUr
        zdRLy88X4nTLz3dJLEn0LlFi0GIAAHSOuCo+AAAA
        """,
            """
        androidx/compose/ui/layout/Measurable.class:
        H4sIAAAAAAAA/41QwU7bQBB9YxsnMaQ1kNIQrkXApQ6IE5wqISRLgSKQEFJO
        m2QbbeLsIu86glu+hQMfwQFFOfajqo5RJRBcepl58+bNvpn9/efpGcAhWoRt
        oQe5UYO7pG8mt8bKpFBJJu5N4ZIzKWyRi14mKyBCPBJTwT09TH72RrLvKvAJ
        9aF0FyKX2p0IJwiN3b3Oe+Ex4VvH5MNkJF0vF0rbRGhtnHDKMD4vsqx0Ydlq
        Z2xcpjR7OzHgB5nzJlOf16Uy1MoAAo2Zv1Nl1WY02Cfsz2crkdf0Ii+ezyKv
        6ld/NeezA69Nl43YawVNatPN4jFYPIRhK6j6cVAOHhB2Ov/1B7wJG0e3b26t
        v3a/jx1h67LQTk1kqqfKKmZ/vF7Jo1emyPvyVGWSsPlPev1BGPJaCPByJ6+4
        hJBtv3JV5gpnD82XuIFNzkfMVllV68JPEaVYTrGCOkN8SvEZcRdksYq1LpYs
        1i0aFl9sicO/4SGINQgCAAA=
        """,
            """
        androidx/compose/ui/layout/ParentDataModifier＄DefaultImpls.class:
        H4sIAAAAAAAA/6VSTU8TQRh+ZgvdthZKq6AI4gdVWhAWjCc5GVCzSVuNGC6e
        ptuhnXZ3hszONvivPBIPxrM/yvguNIiVoI2Hfb+fZ+f9+P7jy1cAz/GM4QVX
        HaNl58QLdHSsY+El0gv5J51Y7x03Qtl9bnlTd+SRFKa6L454Elo/Og5jF4xh
        rs+HnACq671t90VgXWQYpmxPKIao1piIfvfK8ots/fo0w2pDm67XF7ZtuFSx
        x5XSllupyW5p20rCkKqmNb3O5JBjWBloG0rl9YeRJ5UVRvHQ85U1hJYBdVhg
        mA96IhiM4PRmHgkqZFirNcZ7370UOUhJurv1wyKKmCngBmYZqtc1UH0Viogm
        4mKOYedfKsfWUWHoXD3xceD/DbqIadwq4CbmaZhV25Mxgzfhpmn2f9tmuTHa
        TlNY3iEwxZxomKHTZanIpwIMbJAadHXOiUytbYbl66hdrDK4o0kwFC8PkWFz
        oj5crDHMNAWPE8PbodgaEOPUnu4IhlJDKtFKorYwH9IcQ6WhAx4eciNTfxRc
        ep8oKyPhq6GMJYVe/rpaupjx7MUF/lZW9JUSZi/kcSzILRzoxATitUx/sDii
        OPyDHjtwMIXzMeZpq1lksE7eG4o7pEsblfwpSuvfUN44xcJnCjnYIFkgnYWL
        WYI9JX/hvBy3ceeMroQcFol0k+wsaZf0Fn0zzsg5lxl4JO+SLqOKx9k8HVUZ
        TwjqoHamt89+WaeXAiuESmuXPiLjY9nHPZ9i9308wEMfj34CPA2YO9cEAAA=
        """,
            """
        androidx/compose/ui/layout/ParentDataModifier.class:
        H4sIAAAAAAAA/5VS328SQRD+9oA7QNCrVoW2/moRfyR6SHyy6YMRTc9ANZr4
        wtMCS7Nwt0du90h54+/ywfDsH2Wcw5pWijU+7OzMd998Mzez3398/QbgJR4x
        PONqEEdycOL1o3ASaeEl0gv4LEqM95HHQpkWN7wTDeRQitgBY3BHfMqJo469
        D72R6BsHGYbaOqHfebW3gQhJy0GO8sMUnZ2pMxw+bq9LT5Q0XksoLc1sv71a
        dv/JRYj6aEfxsTcSphdzqbTHlYoMNzIi/ygJAt4LBNH2LqNFJmUSa6M9jkwg
        ldcRhg+oVcKscJqh6bHUFFIDBjYm/ESmUYO8wQuGg8V8s2hVrOVZzIuWm5ql
        nx9WFvOm1WDvK661la2wRuap1cg2827uV3RYT0WaDN7asfx1PdRdfW3G6h6I
        SF3v/nvoDMXJuT3tXCbuYJvBOa3A8Oq/Wq+1xJAngfHDSaAd3GUonUcYyh3B
        dRKn63s+JvntT4kyMhS+mkotCX19tkF6BatfqSIPhRHxH7SSr5SI3wRca0Fh
        8XOUxH3xTgaCoXoq8eWCvE3bQZYGaKfLz9JPI0/ILkU2oQW69+iULQqKy/eR
        QhnUyF4noo0d3LELuEf+fVTxkHALD1Cn+4DYV5BDqYuMj7KPqz6uwSUXGz5l
        3+iCaWziZhe2xi2N2xqORkWjqrH1E3XfjsnaAwAA
        """
        )

    @Test
    fun functionReturnsModifierElement() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                fun Modifier.fooModifier(): Modifier.Element {
                    return TestModifier
                }
            """
                ),
                Stubs.Modifier
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                fun Modifier.fooModifier(): Modifier.Element {
                             ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier(): Modifier.Element {
                             ~~~~~~~~~~~
1 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 8: Change return type to Modifier:
@@ -8 +8
-                 fun Modifier.fooModifier(): Modifier.Element {
+                 fun Modifier.fooModifier(): Modifier {
            """
            )
    }

    @Test
    fun getterReturnsModifierElement() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                val Modifier.fooModifier get(): Modifier.Element {
                    return TestModifier
                }

                val Modifier.fooModifier2: Modifier.Element get() {
                    return TestModifier
                }

                val Modifier.fooModifier3: Modifier.Element get() = TestModifier
            """
                ),
                Stubs.Modifier
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                val Modifier.fooModifier get(): Modifier.Element {
                             ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:12: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                val Modifier.fooModifier2: Modifier.Element get() {
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:16: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                val Modifier.fooModifier3: Modifier.Element get() = TestModifier
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                val Modifier.fooModifier get(): Modifier.Element {
                             ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:12: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                val Modifier.fooModifier2: Modifier.Element get() {
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:16: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                val Modifier.fooModifier3: Modifier.Element get() = TestModifier
                             ~~~~~~~~~~~~
3 errors, 3 warnings
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/ui/foo/TestModifier.kt line 8: Change return type to Modifier:
@@ -8 +8
-                 val Modifier.fooModifier get(): Modifier.Element {
+                 val Modifier.fooModifier get(): Modifier {
Autofix for src/androidx/compose/ui/foo/TestModifier.kt line 12: Change return type to Modifier:
@@ -12 +12
-                 val Modifier.fooModifier2: Modifier.Element get() {
+                 val Modifier.fooModifier2: Modifier get() {
Autofix for src/androidx/compose/ui/foo/TestModifier.kt line 16: Change return type to Modifier:
@@ -16 +16
-                 val Modifier.fooModifier3: Modifier.Element get() = TestModifier
+                 val Modifier.fooModifier3: Modifier get() = TestModifier
            """
            )
    }

    @Test
    fun functionImplicitlyReturnsModifierElement() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                fun Modifier.fooModifier() = TestModifier
            """
                ),
                Stubs.Modifier
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                fun Modifier.fooModifier() = TestModifier
                             ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier() = TestModifier
                             ~~~~~~~~~~~
1 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 8: Add explicit Modifier return type:
@@ -8 +8
-                 fun Modifier.fooModifier() = TestModifier
+                 fun Modifier.fooModifier(): Modifier = TestModifier
            """
            )
    }

    @Test
    fun getterImplicitlyReturnsModifierElement() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                val Modifier.fooModifier get() = TestModifier
            """
                ),
                Stubs.Modifier
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                val Modifier.fooModifier get() = TestModifier
                             ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                val Modifier.fooModifier get() = TestModifier
                             ~~~~~~~~~~~
1 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 8: Add explicit Modifier return type:
@@ -8 +8
-                 val Modifier.fooModifier get() = TestModifier
+                 val Modifier.fooModifier get(): Modifier = TestModifier
            """
            )
    }

    @Test
    fun returnsCustomModifierImplementation() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                fun Modifier.fooModifier(): TestModifier {
                    return TestModifier
                }
            """
                ),
                Stubs.Modifier
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                fun Modifier.fooModifier(): TestModifier {
                             ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier(): TestModifier {
                             ~~~~~~~~~~~
1 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 8: Change return type to Modifier:
@@ -8 +8
-                 fun Modifier.fooModifier(): TestModifier {
+                 fun Modifier.fooModifier(): Modifier {
            """
            )
    }

    @Test
    fun modifierVariables_noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                var modifier1: TestModifier? = null
                var modifier2: TestModifier = TestModifier
                lateinit var modifier3: TestModifier
                var modifier4 = TestModifier
                    set(value) { field = TestModifier }
                var modifier5 = TestModifier
                    get() = TestModifier
                    set(value) { field = TestModifier }

                class Foo(
                    var modifier1: TestModifier,
                ) {
                    var modifier2: TestModifier? = null
                    var modifier3: TestModifier = TestModifier
                    lateinit var modifier4: TestModifier
                    var modifier5 = TestModifier
                        set(value) { field = TestModifier }
                    var modifier6 = TestModifier
                        get() = TestModifier
                        set(value) { field = TestModifier }
                }
            """
                ),
                Stubs.Modifier
            )
            .run()
            .expectClean()
    }

    @Test
    fun modifierVals_noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                val modifier1: TestModifier? = null
                val modifier2: TestModifier = TestModifier

                class Foo(
                    val modifier1: TestModifier,
                ) {
                    val modifier2: TestModifier? = null
                    val modifier3: TestModifier = TestModifier
                    val modifier4: TestModifier? get() = null
                    val modifier5: TestModifier get() = TestModifier
                }

                interface Bar {
                    val modifier1: TestModifier?
                    val modifier2: TestModifier
                }

                object Baz : Bar {
                    override val modifier1: TestModifier? = null
                    override val modifier2: TestModifier = TestModifier
                    val modifier3: TestModifier = TestModifier
                    val modifier4: TestModifier? get() = null
                    val modifier5: TestModifier get() = TestModifier
                }

                val Qux = object : Bar {
                    override val modifier1: TestModifier? = null
                    override val modifier2: TestModifier = TestModifier
                    val modifier3: TestModifier = TestModifier
                    val modifier4: TestModifier? get() = null
                    val modifier5: TestModifier get() = TestModifier
                }
            """
                ),
                Stubs.Modifier
            )
            .run()
            .expectClean()
    }

    @Test
    fun noModifierReceiver() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                fun fooModifier(): Modifier {
                    return Modifier
                }

                val fooModifier get(): Modifier {
                    return Modifier
                }

                val fooModifier2: Modifier get() {
                    return Modifier
                }

                val fooModifier3: Modifier get() = Modifier
            """
                ),
                Stubs.Modifier
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/test.kt:6: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                fun fooModifier(): Modifier {
                    ~~~~~~~~~~~
src/androidx/compose/ui/foo/test.kt:10: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                val fooModifier get(): Modifier {
                    ~~~~~~~~~~~
src/androidx/compose/ui/foo/test.kt:14: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                val fooModifier2: Modifier get() {
                    ~~~~~~~~~~~~
src/androidx/compose/ui/foo/test.kt:18: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                val fooModifier3: Modifier get() = Modifier
                    ~~~~~~~~~~~~
0 errors, 4 warnings
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/ui/foo/test.kt line 6: Add Modifier receiver:
@@ -6 +6
-                 fun fooModifier(): Modifier {
+                 fun Modifier.fooModifier(): Modifier {
Autofix for src/androidx/compose/ui/foo/test.kt line 10: Add Modifier receiver:
@@ -10 +10
-                 val fooModifier get(): Modifier {
+                 val Modifier.fooModifier get(): Modifier {
Autofix for src/androidx/compose/ui/foo/test.kt line 14: Add Modifier receiver:
@@ -14 +14
-                 val fooModifier2: Modifier get() {
+                 val Modifier.fooModifier2: Modifier get() {
Autofix for src/androidx/compose/ui/foo/test.kt line 18: Add Modifier receiver:
@@ -18 +18
-                 val fooModifier3: Modifier get() = Modifier
+                 val Modifier.fooModifier3: Modifier get() = Modifier
            """
            )
    }

    @Test
    fun incorrectReceiver() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                fun TestModifier.fooModifier(): Modifier {
                    return this.then(TestModifier)
                }

                val TestModifier.fooModifier get(): Modifier {
                    return this.then(TestModifier)
                }

                val TestModifier.fooModifier2: Modifier get() {
                    return this.then(TestModifier)
                }

                val TestModifier.fooModifier3: Modifier get() = this.then(TestModifier)
            """
                ),
                Stubs.Modifier
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                fun TestModifier.fooModifier(): Modifier {
                                 ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:12: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                val TestModifier.fooModifier get(): Modifier {
                                 ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:16: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                val TestModifier.fooModifier2: Modifier get() {
                                 ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:20: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                val TestModifier.fooModifier3: Modifier get() = this.then(TestModifier)
                                 ~~~~~~~~~~~~
0 errors, 4 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 8: Change receiver to Modifier:
@@ -8 +8
-                 fun TestModifier.fooModifier(): Modifier {
+                 fun Modifier.fooModifier(): Modifier {
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 12: Change receiver to Modifier:
@@ -12 +12
-                 val TestModifier.fooModifier get(): Modifier {
+                 val Modifier.fooModifier get(): Modifier {
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 16: Change receiver to Modifier:
@@ -16 +16
-                 val TestModifier.fooModifier2: Modifier get() {
+                 val Modifier.fooModifier2: Modifier get() {
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 20: Change receiver to Modifier:
@@ -20 +20
-                 val TestModifier.fooModifier3: Modifier get() = this.then(TestModifier)
+                 val Modifier.fooModifier3: Modifier get() = this.then(TestModifier)
            """
            )
    }

    @Test
    fun unreferencedReceiver() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.*

                object TestModifier : Modifier.Element

                // Modifier factory without a receiver - since this has no receiver it should
                // trigger an error if this is returned inside another factory function
                fun testModifier(): Modifier = TestModifier

                interface FooInterface {
                    fun Modifier.fooModifier(): Modifier {
                        return TestModifier
                    }
                }

                fun Modifier.fooModifier(): Modifier {
                    return TestModifier
                }

                fun Modifier.fooModifier2(): Modifier {
                    return testModifier()
                }

                fun Modifier.fooModifier3(): Modifier = TestModifier

                fun Modifier.fooModifier4(): Modifier = testModifier()

                fun Modifier.fooModifier5(): Modifier {
                    return Modifier.then(TestModifier)
                }

                fun Modifier.fooModifier6(): Modifier {
                    return Modifier.fooModifier()
                }
            """
                ),
                Stubs.Modifier,
                Stubs.Composable
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:10: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                fun testModifier(): Modifier = TestModifier
                    ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:13: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                    fun Modifier.fooModifier(): Modifier {
                                 ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:18: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier(): Modifier {
                             ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:22: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier2(): Modifier {
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:26: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier3(): Modifier = TestModifier
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:28: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier4(): Modifier = testModifier()
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:30: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier5(): Modifier {
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:34: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier6(): Modifier {
                             ~~~~~~~~~~~~
7 errors, 1 warnings
            """
            )
    }

    @Test
    fun ignoresParentDataModifiers() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.layout.Measurable
                import androidx.compose.ui.layout.ParentDataModifier
                import androidx.compose.ui.unit.Density

                private val Measurable.boxChildData: FooData? get() = parentData as? FooData

                private class FooData(var boolean: Boolean) : ParentDataModifier {
                    override fun Density.modifyParentData(parentData: Any?) = this
                }
            """
                ),
                Stubs.Modifier,
                DensityStub,
                MeasurableAndParentDataModifierStub
            )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors_inlineAndValueClasses() {
        val inlineAndValueClassStub =
            bytecodeStub(
                filename = "InlineAndValueClassStub.kt",
                filepath = "androidx/compose/ui/foo",
                checksum = 0x402f4998,
                """
            package androidx.compose.ui.foo

            import androidx.compose.ui.Modifier

            inline class Inline(val value: Float)

            @JvmInline
            value class Value(val value: Float)

            private object TestModifier : Modifier.Element

            fun Modifier.inline(inline: Inline = Inline(1f)): Modifier {
                return this.then(TestModifier)
            }

            fun Modifier.value(value: Value = Value(1f)): Modifier {
                return this.then(TestModifier)
            }
        """,
                """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgsuEST8xLKcrPTKnQS87PLcgvTtUr
            zdRLy88XkvTMy8nMS3XMSwlLzClNdc5JLC4OLilN8i4RYgtJLS7xLlFi0GIA
            AA4XavBWAAAA
            """,
                """
            androidx/compose/ui/foo/Inline.class:
            H4sIAAAAAAAA/4VU3VMbVRT/3ZuvzbKEhQKF2FYaapvw0QBVq1KQjzY2GFqF
            GqX4tUlWWEh2Y3bD8Mib/gXO2EdffJDp6IwCY2ccxDf/Jsfx3JsNYQJTZ3bu
            vefcc87vnN85d//+9/c/ALyOAsM1wy7VHKu0my46larjmum6lf7KcdJZu2zZ
            ZgSMQd8ydox02bA30o8LW2bRiyDAoGyYXt4o102GQDKVYQjtNCSW0RCBEgVH
            lCHobVouw1Du5UDTDJ2es+rVLHtj3KpUywx9yUwq14Ju3JHd5XbdQt0ql8xa
            BF0M4XuWbXmzMqe8hm70qNBxiaI3cJIyyRkFfWRrVKumXWIYT57HOQ/tw0xr
            uIwBEXWQ4cpFOZ41fEUYXhGGiy83vCYMXyVamyww9CYvqF/DdSSE7TBxa9Q2
            JjR0IqYS2TepyE3D3Vx0SqZPYZDSyzJ0taJkyo5B/RshoKathjGkVIxiXLKW
            1ZAUMscEQ4f5dd0ou364/mQm1z4L06mnDGrdLji70kqjuQoL7zcYIqLVRo1m
            IpTMZETku3hLIL1NGsfbNGsMPecjUmcasGIGLgLUMIU7AuPdRol5FUHRY73o
            2K5Xqxc9p3aGAZpNpZkew7Bo2f8MoxiceQHwHo3zDoN2hoYJvxoqm1cnxTJF
            zOS2HY9c01s7lfTSTuV0qLubF8umZ5QMzyAdr+wE6P0xsUTFAkLZJv2uJSQC
            4CUK/NPx3ojKB7jK9eM9lT6uR1SuhGjvoD1Iu0J7gPYY7Vw5+WZu4Hhvik+w
            ha6esM7jfCJw8kM4qAT10FJcV0iOTim6Gg8OsAn28K/vGrcdurak6510GyMd
            k7ouXSddN+l6TnWX9N6V7tOoCmUUDyphPXLyLeMiZaKBCok3Sp+3S/LnsFg2
            XHfVqxdub3vUDTFvNI85snhUrxTM2hOjUDbFFDhFo5w3apaQfWXnqmcUt5eN
            qi+rq069VjQzlhAGV+q2Z1XMvOVadDtv245neBYNACapcUHJao/4EdFJ9DOE
            MGk+JSkt+KY9NPIr1Od04PiM1rBUhvG5dJAG6KATNUk8Md/5LlmLu8EX0NcO
            0dvTf4B44gBX9dQBhg5w42eJ3AoyiNdkDkw8XD/ITT8DRWRwhFvtPsopMD1F
            32e4mXXiCLf32xxCpyBjsrwLQCbbfVog9Jp8nxWqTszg1dE/wZ8hFNgfPQY/
            wJsPEjeO8M73QhPcl5R9QWsEPPoPurmM2k9K8vQzEadp3JM5zGDWjy9aI6yi
            IqfRI8y1kmq4R/2kxEm661y8Rd991ndXRw6xMDL8G9RfLmxfI5Z6GkuVc8Ao
            5iLu+7GGfHp44nkbMbwxNfogHiDjW98iWsRd9AX4WuIQD9tbFkVWOnWL/197
            y5qDxi4YrkEs4X3fYd6vL5YYJrafIRL8EcFAi+8QuDZ3lq4Ycj7bMSzLEjm+
            lObrMGi36fSI9sfk+sE6All8mMVKFqt4Qkd8lEUeH6+DufgEa+voc6G5eOoi
            ItdFF/ddhFyEXcxIzbSLKRd3XIxJMeki5eK6PHe6iP0HcneOKFoIAAA=
            """,
                """
            androidx/compose/ui/foo/InlineAndValueClassStubKt.class:
            H4sIAAAAAAAA/4VUXVPbRhQ9K38JY4iBBAIBJy1uAm6NbJd+uk1KnThVMG4n
            ZnjhIbOWBSyWJUZaefLUYfov+tj+gk6faB86HvrWH9Xpla0CJhg/6O7eu0d3
            z557d//598+/AGyixlDkdst1ROutZjidE8czNV9oB46j6bYlbHPLbu1xyzcr
            Fve8hvSb2zIBxpA+5l2uWdw+1L5vHpsGRSMM06L/U76wab/ZeeUwPFur3ZR/
            x2mJA2G65er67esMqzXHPdSOTdl0ubA9jdu2I7kUDs3rjqz7lkWoVFYeCS87
            2F6FypBpO5Ic7bjb0YQtTdfmFp1JupREGF4CSYZ7xpFptMMsP3CXd0wCMjxZ
            q10/XvlKpBEkOSyv76WQwlQSk5hmyI7Scdf05P/nSSDNoOr1xu5WvfKC4fGN
            p7/+VzmFWcxNYAZ3GZZv0yuBeYaoPDJthqdjpB+jfAr3sTiJBSwxLFxV941f
            al3UNzOufPHBTwysyjA/3B7ZlnnAfUsyvB7XJvq7BRnbOZnbOzuB96mNDeoj
            6fqGdNy86JxYJN9adb2aQhYfJLGKxynEEE9CwRrDVDe4CvliXmzzqsEwOdCl
            H1XxIcPilUCgUzEwITjWj1LTDSW51GBlFN3+/UugkEQxoJMb0PmYYaYW9viO
            KXmLS06HVjrdCF1tFpiJwICkbwcThRbfimBWoFmryNDpnd5N9k6Tyn1l8KmR
            /tg7XcqkySgFlqOvpKrKwFX+PmO90/Nf41E1ko4SKDaECbz4JSSRjp7/pCSS
            MfX8l0yBBZuWGB6NbPlBWegMD0dC+lIQYmnE47TRJiGjFadFOt+pEaLud5qm
            u8ubFkVma47BrT3uisAPgw9e+7YUHVO3u8ITFNq6fGDoVl9fvXglhmBTDcmN
            9g4/CZMmG47vGmZVBM5imGPvnfxUUAXRoEhkF4NGQwTPyXtJcYXGudzsxBnu
            5H7HvR4W/sADBb8FpcQLsnEqbRzTqNJ8fgDHMlb66eaQwUNafxniEjR+F7SE
            EjpIT+AR3iMv2K8U8phZjv74M9TUGZ48z62eYX2wm042AjZ5sS0wRXxzN/H9
            aAzf2SG++Qu+G+P5arfwLfX5bo7kO0PhV/3FCrZprFD0E1L8031EdHym43Md
            X+BLHWV8peNrPN0H8/AM3+xD9ZDxsOJhy0PMQ9zDhodvPeT+A2h8MqZHBwAA
            """,
                """
            androidx/compose/ui/foo/TestModifier.class:
            H4sIAAAAAAAA/6VUW0/UQBT+pgvbbi1y8cJN8cKKXJQC6osQEkRImiyrEbKJ
            4Wl2O+BA2yHtlPBI/Cn+AokPGE0M0Td/lPG0LETkIokPM3Mu33dybu3PX1++
            AXiKZwxlHvmxkv6O21DhlkqEm0p3TSl3RSR6SflyTYrYBGPo2ODb3A14tO6+
            qm+IhjZROId/xCsvBCIUESFbGYozMpJ6lqEwPFJzYMKy0YISQ4t+JxOGocpl
            UpnO8SJimB0+k3AMHLnYzTBYUfG6uyF0PeYySlweRUpzLRXJVaWraRAQavIy
            BZZfijWeBtoLt4LERAeDf3F2R8Tp/6rBQRu6bHTiGkOrorbEDAP/KtuaaQT5
            JGwYWfstr7q8MledX3DQC6dExj6Gzsqm0gRzl4TmPteciEa4XaC1YdlVyi4w
            sE2y78hMmyDJn6S2HuzattFj5Odg1/rx3ug52J0yJtgL0zK+fygaHUYGnTpv
            5Kd6xHDrIpyJYQazCWZw/pwFQ58XUR1iLvJrPEjFfMCTZFmn9fFNwva/SSMt
            Q+FF2zKR9YBgxztAizavfMHQXiF+NQ3rIl7hhGHoqqgGD2o8lpneNJb/jvWa
            xzwUWsQngjpeFIk4T0OQai+rNG6IRZmF6G2GqJ1KBhM0lxZqeJFObzYoeh9T
            1zO9nd4C+emTIm2cNDcbDb2to/uw90gwyHQIBq5QMOQBCECak0+yDVcpSEZ+
            TmgjQ4+Ofcb1j2eybx4imuxMukG2kylN0jFZU7HQfZxdd06mUF9hvN1Hzyf0
            7+WGAqbo7iJ3ESMYLZZotYsYo3Kf5Fk8on9W9t+ihaDMb6+i4GHAwx0Pd3GP
            RNz3MIjyKliCBxgifwInwcME1m+TOp2p9AQAAA==
            """,
                """
            androidx/compose/ui/foo/Value.class:
            H4sIAAAAAAAA/31U3VMbVRT/3ZuvzbKETQqUxGLbUNvw1QBVq1IqH21sMLQK
            FaX4tUlWWEh2Y3bD8Mib/gXO2Edf+iDT0RkFxs44iG/+TY7juTcbYALDzM69
            95x7zvmd8zvn7j///fEngDdRZOg37HLdscrb2ZJTrTmumW1Y2W8cJ7tsVBpm
            BIxB3zC2jGzFsNeyT4obZsmLIMCgrJmetGEIZAZzDKGtpsRyGiJQouCIMgS9
            dctluFq4EGeSodNzlry6Za+NWtVahaEnkxssnCA378jucrtutmFVymY9gi6G
            8D3Ltrz7MqVlDXEkVOi4xKBJmIxMcUpBD5katZpplxlGM2dhziL7KJMaLqNP
            BE0yXDkvxdOGrwnDK8Jw7mLD14XhVSK1RQJDd+ac8jVcR1rYDhCzRn1tTEMn
            YipRfZMYXDfc9TmnbPoMBim9PEPXSZRcxTGoe0ME1LLVMIJBFcMYlaTlNWSE
            zDHG0GF+2zAqrh+uN5MrtE/C5OAzBrVhF51taaXRUIWF91sMEdFoo04TEcrk
            ciLyXbwjkN4ljeOtm3WGxNmI1JkmrBiB8wA1TOCOwHi/WeKyiqBosV5ybNer
            N0qeUz/FAE2m0kqPIS1advEoirGZEfE/oFneotE5xcKYXwxVzWvjYpkgYgqb
            jlex7OzGVjU7v1XN2ySIkY63LhZMzygbnkE6Xt0K0NtjYomKBYSySfptS0gE
            wMsU+OfDnSGV93GV64c7Kn1cj6hcCdHeQXuQdoX2AO0x2rly9N103+HOBB9j
            s12JsM5TfCxw9FM4qAT10HxKV0iOTii6mgr2sTH26O8fmrcdujav6510GyMd
            k7ouXSddnHSJY90lvXsxfhxVoYxSQSWsR46+Z1ykTDRQIalm6TN2WVI5VzFc
            d8lrFG9vetQMMW40jgWyeNyoFs36U6NYMcUQOCWjsmzULSH7ys4lzyhtLhg1
            X1aXnEa9ZOYsISQXG7ZnVc1ly7Xodsa2Hc/wLOo/xqlxQclqQvyF6CT6GUKY
            NJ+TlBV80x4a+g3qSzpwfEFrWCqj+FI6SAN00Eno6IX5znfJWtwlX0Ff2Ud3
            oncPqfQe+vXBPVzbw41fJPJJkCTekDkw8W79IDf9DBSRwQFutfsox8D0En2f
            gVbW6QPc3m1zCB2DjMjyzgEZb/c5AaHH5PssUnViBvuH/wJ/jlBgd/gQfA9v
            P0zfOMB7PwpNcFdS9hWtEfDov4hzGbWXlOTpZyJOk7gnc5jCfT++aI2wioqc
            hg8wfZJU0z3qJyVO0l3n4i367vd9d3VoH7NDA79D/fXc9jVjqcexVDkHjG7n
            8MCPdc2nh6dfthHDm1OjJ/EQOd/6FtEi478CX0nv41F7y6LIS6e4+P21t6w1
            aOyc4UpiHh/6DjN+fbH0ALH9HJHgCwQDJ3yHwLXp03TFUPDZjmFBlsjxtTRf
            hUG7TafHtD8h149WEcjj4zwW81jCUzrikzyW8ekqmIvPsLKKHheai2cuInKd
            c/HARchF2MWU1Ey6mHBxx8WIFDMuBl1cl+dOF7H/AVoTuh9WCAAA
            """
            )

        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                fun Modifier.inlineModifier(): Modifier = inline()

                fun Modifier.valueModifier(): Modifier = value()
            """
                ),
                Stubs.Modifier,
                inlineAndValueClassStub
            )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                fun Modifier.fooModifier(): Modifier {
                    return this.then(TestModifier)
                }

                fun Modifier.fooModifier2(): Modifier {
                    return then(TestModifier)
                }

                fun Modifier.fooModifier3(): Modifier {
                    return fooModifier()
                }
            """
                ),
                Stubs.Modifier
            )
            .run()
            .expectClean()
    }

    @Test
    fun composedNoErrors() {
        // Regression test from b/328119668
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.composed

                fun Modifier.bar(): Modifier = composed {
                    object : Modifier {}
                }
            """
                ),
                Stubs.Modifier,
                UiStubs.composed,
            )
            .run()
            .expectClean()
    }

    // Test for b/341056462
    @Test
    fun nestedReceivers() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.composed

                object TestModifier : Modifier.Element

                fun Modifier.fooModifier(): Modifier {
                    return this.then(TestModifier)
                }

                fun Modifier.noError_implicitReceiver(): Modifier = composed {
                    fooModifier()
                }

                // fooModifier() uses the receiver from composed, but composed doesn't use the
                // receiver from Modifier.error() - so there is still an issue.
                fun Modifier.error_implicitReceiver(): Modifier = Modifier.composed {
                    fooModifier()
                }

                fun Modifier.noError_explicitReceiver(): Modifier = this.composed {
                    this.fooModifier()
                }

                // fooModifier() uses the receiver from composed, but composed doesn't use the
                // receiver from Modifier.error() - so there is still an issue.
                fun Modifier.error_explicitReceiver(): Modifier = Modifier.composed {
                    this.fooModifier()
                }
            """
                ),
                Stubs.Modifier,
                UiStubs.composed,
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:19: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.error_implicitReceiver(): Modifier = Modifier.composed {
                             ~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:29: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.error_explicitReceiver(): Modifier = Modifier.composed {
                             ~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
"""
            )
    }
}
