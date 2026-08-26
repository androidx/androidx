/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StateHolderDesignDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = StateHolderDesignDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            StateHolderDesignDetector.STABILITY_ISSUE,
            StateHolderDesignDetector.CONSTRUCTOR_ISSUE,
        )

    private val RuntimeStubs =
        kotlin(
                """
            package androidx.compose.runtime
            annotation class Stable
            annotation class Immutable
            annotation class Composable
            """
            )
            .indented()

    @Test
    fun valid_stableWithConstructor() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                        "src/androidx/compose/material3/CardColors.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Stable

                    @Stable
                    class CardColors(val containerColor: Long)
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun valid_immutableWithCopy() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                        "src/androidx/compose/material3/CardColors.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Immutable

                    @Immutable
                    class CardColors private constructor(val containerColor: Long) {
                        fun copy(containerColor: Long = this.containerColor): CardColors = CardColors(containerColor)
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun invalid_missingStability() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                        "src/androidx/compose/material3/CardColors.kt",
                        """
                    package androidx.compose.material3

                    class CardColors(val containerColor: Long)
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/CardColors.kt:3: Error: State holder class 'CardColors' should be annotated with @Stable or @Immutable [StateHolderMissingStabilityAnnotation]
                class CardColors(val containerColor: Long)
                      ~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/androidx/compose/material3/CardColors.kt line 3: Annotate with @Stable:
                @@ -1,0 +2 @@
                +import androidx.compose.runtime.Stable
                @@ -2,0 +4 @@
                +@Stable
                Fix for src/androidx/compose/material3/CardColors.kt line 3: Annotate with @Immutable:
                @@ -1,0 +2 @@
                +import androidx.compose.runtime.Immutable
                @@ -2,0 +4 @@
                +@Immutable
                """
                    .trimIndent()
            )
    }

    @Test
    fun invalid_privateConstructorNoCopy() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                        "src/androidx/compose/material3/CardColors.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Stable

                    @Stable
                    class CardColors private constructor(val containerColor: Long)
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/CardColors.kt:6: Error: State holder class 'CardColors' must provide a public constructor, a public copy() function, or a public non-composable factory function to allow creation/modification outside of composables [StateHolderMissingConstructorOrCopy]
                class CardColors private constructor(val containerColor: Long)
                      ~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun ignored_internalClass() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                        "src/androidx/compose/material3/InternalColors.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Stable

                    @Stable
                    internal class InternalColors private constructor(val containerColor: Long)
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun ignored_nonStateHolder() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                        "src/androidx/compose/material3/MyClass.kt",
                        """
                    package androidx.compose.material3

                    class MyClass
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun ignored_differentPackage() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                        "src/foo/CardColors.kt",
                        """
                    package foo

                    // Invalid according to rule, but ignored because of package
                    class CardColors(val containerColor: Long)
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun valid_interfaceStateHolder() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                        "src/androidx/compose/material3/DatePickerState.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Stable

                    @Stable
                    interface DatePickerState {
                        val value: String
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun invalid_interfaceMissingStability() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                        "src/androidx/compose/material3/DatePickerState.kt",
                        """
                    package androidx.compose.material3

                    interface DatePickerState {
                        val value: String
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/DatePickerState.kt:3: Error: State holder class 'DatePickerState' should be annotated with @Stable or @Immutable [StateHolderMissingStabilityAnnotation]
                interface DatePickerState {
                          ~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun valid_classWithConstructorFunction() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                        "src/androidx/compose/material3/MyState.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Stable

                    @Stable
                    class MyState internal constructor(val value: String)

                    fun MyState(value: String): MyState = MyState(value)
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun invalid_classWithComposableConstructorFunction() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                        "src/androidx/compose/material3/MyState.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Stable
                    import androidx.compose.runtime.Composable

                    @Stable
                    class MyState internal constructor(val value: String)

                    @Composable
                    fun MyState(value: String): MyState = MyState(value)
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/MyState.kt:7: Error: State holder class 'MyState' must provide a public constructor, a public copy() function, or a public non-composable factory function to allow creation/modification outside of composables [StateHolderMissingConstructorOrCopy]
                class MyState internal constructor(val value: String)
                      ~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun valid_elevationClassNoConstructorNoCopy() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                        "src/androidx/compose/material3/ButtonElevation.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Stable

                    @Stable
                    class ButtonElevation internal constructor(val value: Int)
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun invalid_elevationClassMissingStability() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                        "src/androidx/compose/material3/ButtonElevation.kt",
                        """
                    package androidx.compose.material3

                    class ButtonElevation internal constructor(val value: Int)
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/ButtonElevation.kt:3: Error: State holder class 'ButtonElevation' should be annotated with @Stable or @Immutable [StateHolderMissingStabilityAnnotation]
                class ButtonElevation internal constructor(val value: Int)
                      ~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }
}
