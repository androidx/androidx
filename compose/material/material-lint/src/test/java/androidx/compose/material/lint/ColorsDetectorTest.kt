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

@file:Suppress("UnstableApiUsage")

package androidx.compose.material.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.kotlinAndBytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.useFirUast
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/**
 * Test for [ColorsDetector].
 *
 * Tests for when Colors.kt is available as source (during global / IDE analysis), and for when it
 * is available as bytecode (during partial / CLI analysis). Since we cannot resolve default values
 * when it is only available as bytecode, is it expected that we throw less errors in that mode.
 */
class ColorsDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ColorsDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(ColorsDetector.ConflictingOnColor)

    // Simplified Colors.kt stubs
    private val ColorsStub =
        kotlinAndBytecodeStub(
            filename = "Colors.kt",
            filepath = "androidx/compose/material",
            checksum = 0x73db7d2,
            """
            package androidx.compose.material

            import androidx.compose.ui.graphics.*

            class Colors(
                primary: Color,
                primaryVariant: Color,
                secondary: Color,
                secondaryVariant: Color,
                background: Color,
                surface: Color,
                error: Color,
                onPrimary: Color,
                onSecondary: Color,
                onBackground: Color,
                onSurface: Color,
                onError: Color,
                isLight: Boolean
            )

            fun lightColors(
                primary: Color = Color(0xFF6200EE),
                primaryVariant: Color = Color(0xFF3700B3),
                secondary: Color = Color(0xFF03DAC6),
                secondaryVariant: Color = Color(0xFF018786),
                background: Color = Color.White,
                surface: Color = Color.White,
                error: Color = Color(0xFFB00020),
                onPrimary: Color = Color.White,
                onSecondary: Color = Color.Black,
                onBackground: Color = Color.Black,
                onSurface: Color = Color.Black,
                onError: Color = Color.White
            ): Colors = Colors(
                primary,
                primaryVariant,
                secondary,
                secondaryVariant,
                background,
                surface,
                error,
                onPrimary,
                onSecondary,
                onBackground,
                onSurface,
                onError,
                true
            )

            fun darkColors(
                primary: Color = Color(0xFFBB86FC),
                primaryVariant: Color = Color(0xFF3700B3),
                secondary: Color = Color(0xFF03DAC6),
                secondaryVariant: Color = secondary,
                background: Color = Color(0xFF121212),
                surface: Color = Color(0xFF121212),
                error: Color = Color(0xFFCF6679),
                onPrimary: Color = Color.Black,
                onSecondary: Color = Color.Black,
                onBackground: Color = Color.White,
                onSurface: Color = Color.White,
                onError: Color = Color.Black
            ): Colors = Colors(
                primary,
                primaryVariant,
                secondary,
                secondaryVariant,
                background,
                surface,
                error,
                onPrimary,
                onSecondary,
                onBackground,
                onSurface,
                onError,
                false
            )
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuWSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHicM7PyS8q9i7hUueSxalMLy0/X4gtJLW4BKxQBkNhaaZeelFi
        QUZmcrEQO9hI7xIlBi0GAPW9qnSRAAAA
        """,
            """
        androidx/compose/material/Colors.class:
        H4sIAAAAAAAA/5WUT28TRxjGn1n/2Xhtx3aMNzbp8jdAkrbZkPYGQippKwWZ
        tmpQDuSAJuslmdg7E82sI3rLJ+CGxJlvkBNSD1XEBYkPhXh3vFmoeqiwrHl+
        M+87M+8zM/aHj3//A+BH/MBwjcuRVmL0IoxUcqxMHCY8jbXgk3BLTZQ2LhhD
        +4if8HDC5UH4+/5RHKUuSgzV+0KK9AGFVx598Xm6ustQWlndbaAC10MZcwzl
        9FAYhhvD/9vvHoN7rEXC9V8M7BHDfN7b5ZQlU4aaiSMlRzahXXAR9vZ5ND7Q
        aipHtJSZ6uc8ihkqsdZK02wl/7hYvq7kzue1Gko+/GIqJe5cTHaV/GU23RVm
        KA4OaR/2lOHBv40PxyqdCBkenSShkGRLkq2f4+d8Okm3lDSpnkap0o+5Hsf6
        3uyAqh4cfMPQXY4+ZzxLbArD+tctydC5mPA4TvmIp5zGnOSkRPfNsqaWNaDq
        xzT+QmS9DaLRXYZX56d9z+k7ntM+P/Xoa3muQf06dfvnp5vOBnuYLFTbzmVn
        o0RazrWSazVXN9e5XGu5ernWc23k2sx1PtcWafvdm6rT7mTFbbKs5OX/vp2p
        CA80Pz4UkZk9H3Jbm72j9THdUXlLjej6WkMh49+myX6sn/D9CY0sDFXEJ/bN
        UD8f9HbUVEfxryLrDP6cylQk8a4wgqI/SalSngo6ctylGytnx0jqZFdIxa1T
        74z0Mmll7S1qZzYcUpuFs3aD2peYzxLgoW4XqKCBpo1XKNJEyVKLqGKpTeRa
        6hDVLC0Q1S11iZqWLpG2LPWIOpZ8oq6lRaKepT7RoqUBlTpo0+8CS3n572lk
        iTRYu3rdr/iuX/PrftNv+R2/6/f8xd7gLYIz+3oyK6/thkFhJSisBIWVoLAS
        FFaCwkpQWAkKK0FhJSisBIWVoLASFFaCmRVLV3CV2KEbyg7/e2yScqrpGtVw
        fQ+lbdzYxs1tLOMWIW5v4w5W9sAMVrG2hwWDusG3Bg2D7wzmDVqG/mLQMVmo
        a3DJoGfgGywa9A0GNnnpExMAv0xQBQAA
        """,
            """
        androidx/compose/material/ColorsKt.class:
        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
        AgwAAA==
        """,
        )

    @Test
    fun constructorErrors_source() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val colors = Colors(
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.Red,
                    false
                )

                val colors2 = Colors(
                    primary = Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    background = Color.Blue,
                    Color.White,
                    Color.Green,
                    Color.White,
                    Color.Blue,
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onError = Color.Red,
                    isLight = false
                )

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors3 = Colors(
                    yellow200,
                    yellow400,
                    yellow200,
                    secondaryVariant = yellow200,
                    Color.White,
                    surface = Color.Blue,
                    Color.White,
                    Color.White,
                    yellow400,
                    Color.Blue,
                    onSurface = Color(0xFFFFBBCC),
                    yellow500,
                    false
                )
            """
                ),
                Stubs.Color,
                ColorsStub.kotlin,
            )
            .run()
            .expect(
                """
src/androidx/compose/material/foo/test.kt:15: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:16: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:17: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:18: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:19: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.Red,
                    ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:31: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:32: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.Blue,
                    ~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:34: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onSurface = Color.White,
                                ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:51: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:52: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    yellow400,
                    ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:53: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.Blue,
                    ~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:55: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    yellow500,
                    ~~~~~~~~~
12 errors, 0 warnings
            """
            )
    }

    @Test
    fun lightColorsErrors_source() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val colors = lightColors(
                    // Color.White is used by default for some colors, so onPrimary should conflict
                    primary = Color.White,
                    onPrimary = Color.Red,
                )

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors2 = lightColors(
                    primary = yellow200,
                    background = yellow200,
                    onPrimary = yellow400,
                    onBackground = Color.Green,
                )
            """
                ),
                Stubs.Color,
                ColorsStub.kotlin,
            )
            .skipTestModes(TestMode.JVM_OVERLOADS) // b/440099029
            .run()
            .expect(
                """
src/androidx/compose/material/foo/test.kt:10: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onPrimary = Color.Red,
                                ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:20: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onPrimary = yellow400,
                                ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:21: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onBackground = Color.Green,
                                   ~~~~~~~~~~~
3 errors, 0 warnings
            """
            )
    }

    @Test
    fun darkColorsErrors_source() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val colors = darkColors(
                    // Color(0xFF121212) is used by default for some colors, so onPrimary should
                    // conflict
                    primary = Color(0xFF121212),
                    onPrimary = Color.Red,
                )

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors2 = darkColors(
                    primary = yellow200,
                    background = yellow200,
                    onPrimary = yellow400,
                    onBackground = Color.Green,
                )
            """
                ),
                Stubs.Color,
                ColorsStub.kotlin,
            )
            .skipTestModes(TestMode.JVM_OVERLOADS) // b/440099029
            .run()
            .expect(
                """
src/androidx/compose/material/foo/test.kt:11: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onPrimary = Color.Red,
                                ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:21: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onPrimary = yellow400,
                                ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:22: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onBackground = Color.Green,
                                   ~~~~~~~~~~~
3 errors, 0 warnings
            """
            )
    }

    @Test
    fun trackVariableAssignment_source() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val testColor1 = Color.Black

                fun test() {
                    val colors = lightColors(
                        primary = Color.Green,
                        background = Color.Green,
                        onPrimary = testColor1,
                        onBackground = Color.Black,
                    )

                    val testColor2 = Color.Black

                    val colors2 = lightColors(
                        primary = Color.Green,
                        background = Color.Green,
                        onPrimary = testColor2,
                        onBackground = Color.Black,
                    )

                    var testColor3 = Color.Green
                    testColor3 = Color.Black

                    val colors2 = lightColors(
                        primary = Color.Green,
                        background = Color.Green,
                        onPrimary = testColor3,
                        onBackground = Color.Black,
                    )
                }
            """
                ),
                Stubs.Color,
                ColorsStub.kotlin,
            )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors_source() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val colors = lightColors()
                val colors2 = darkColors()
                val colors3 = Colors(
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    false
                )

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors4 = Colors(
                    yellow200,
                    yellow400,
                    Color.White,
                    secondaryVariant = yellow500,
                    Color.White,
                    surface = Color.Blue,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    onSurface = Color(0xFFFFBBCC),
                    Color.White,
                    false
                )

                val colors5 = lightColors(
                    yellow200,
                    yellow400,
                    Color.White,
                    surface = Color.Blue,
                    onSurface = Color(0xFFFFBBCC),
                )

                val colors6 = darkColors(
                    yellow200,
                    yellow400,
                    Color.White,
                    surface = Color.Blue,
                    onSurface = Color(0xFFFFBBCC),
                )

            """
                ),
                Stubs.Color,
                ColorsStub.kotlin,
            )
            .run()
            .expectClean()
    }

    @Test
    fun constructorErrors_compiled() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val colors = Colors(
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.Red,
                    false
                )

                val colors2 = Colors(
                    primary = Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    background = Color.Blue,
                    Color.White,
                    Color.Green,
                    Color.White,
                    Color.Blue,
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onError = Color.Red,
                    isLight = false
                )

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors3 = Colors(
                    yellow200,
                    yellow400,
                    yellow200,
                    secondaryVariant = yellow200,
                    Color.White,
                    surface = Color.Blue,
                    Color.White,
                    Color.White,
                    yellow400,
                    Color.Blue,
                    onSurface = Color(0xFFFFBBCC),
                    yellow500,
                    false
                )
            """
                ),
                Stubs.Color,
                ColorsStub.bytecode,
            )
            .run()
            .run {
                if (useFirUast()) {
                    expect(
                        """
src/androidx/compose/material/foo/test.kt:15: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:16: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:17: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:18: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:19: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.Red,
                    ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:31: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:32: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.Blue,
                    ~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:34: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onSurface = Color.White,
                                ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:51: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.White,
                    ~~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:52: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    yellow400,
                    ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:53: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    Color.Blue,
                    ~~~~~~~~~~
src/androidx/compose/material/foo/test.kt:55: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    yellow500,
                    ~~~~~~~~~
12 errors
                        """
                    )
                } else {
                    // In K1, the constructor call to Colors cannot be resolved when
                    // it is available as bytecode, so we don't see any errors.
                    expectClean()
                }
            }
    }

    @Test
    fun lightColorsErrors_compiled() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors = lightColors(
                    primary = yellow200,
                    background = yellow200,
                    onPrimary = yellow400,
                    onBackground = Color.Green,
                )
            """
                ),
                Stubs.Color,
                ColorsStub.bytecode,
            )
            .run()
            .expect(
                """
src/androidx/compose/material/foo/test.kt:14: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onPrimary = yellow400,
                                ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:15: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onBackground = Color.Green,
                                   ~~~~~~~~~~~
2 errors, 0 warnings
            """
            )
    }

    @Test
    fun darkColorsErrors_compiled() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors = darkColors(
                    primary = yellow200,
                    background = yellow200,
                    onPrimary = yellow400,
                    onBackground = Color.Green,
                )
            """
                ),
                Stubs.Color,
                ColorsStub.bytecode,
            )
            .run()
            .expect(
                """
src/androidx/compose/material/foo/test.kt:14: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onPrimary = yellow400,
                                ~~~~~~~~~
src/androidx/compose/material/foo/test.kt:15: Error: Conflicting 'on' color for a given background [ConflictingOnColor]
                    onBackground = Color.Green,
                                   ~~~~~~~~~~~
2 errors, 0 warnings
            """
            )
    }

    @Test
    fun trackVariableAssignment_compiled() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val testColor1 = Color.Black

                fun test() {
                    val colors = lightColors(
                        primary = Color.Green,
                        background = Color.Green,
                        onPrimary = testColor1,
                        onBackground = Color.Black,
                    )

                    val testColor2 = Color.Black

                    val colors2 = lightColors(
                        primary = Color.Green,
                        background = Color.Green,
                        onPrimary = testColor2,
                        onBackground = Color.Black,
                    )

                    var testColor3 = Color.Green
                    testColor3 = Color.Black

                    val colors2 = lightColors(
                        primary = Color.Green,
                        background = Color.Green,
                        onPrimary = testColor3,
                        onBackground = Color.Black,
                    )
                }
            """
                ),
                Stubs.Color,
                ColorsStub.bytecode,
            )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors_compiled() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.material.foo

                import androidx.compose.material.*
                import androidx.compose.ui.graphics.*

                val colors = lightColors()
                val colors2 = darkColors()
                val colors3 = Colors(
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    false
                )

                val yellow200 = Color(0xffffeb46)
                val yellow400 = Color(0xffffc000)
                val yellow500 = Color(0xffffde03)

                val colors4 = Colors(
                    yellow200,
                    yellow400,
                    Color.White,
                    secondaryVariant = yellow500,
                    Color.White,
                    surface = Color.Blue,
                    Color.White,
                    Color.White,
                    Color.White,
                    Color.White,
                    onSurface = Color(0xFFFFBBCC),
                    Color.White,
                    false
                )

                val colors5 = lightColors(
                    yellow200,
                    yellow400,
                    Color.White,
                    surface = Color.Blue,
                    onSurface = Color(0xFFFFBBCC),
                )

                val colors6 = darkColors(
                    yellow200,
                    yellow400,
                    Color.White,
                    surface = Color.Blue,
                    onSurface = Color(0xFFFFBBCC),
                )

            """
                ),
                Stubs.Color,
                ColorsStub.bytecode,
            )
            .run()
            .expectClean()
    }
}
