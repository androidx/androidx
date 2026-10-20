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
class DefaultsReadOnlyComposableDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = DefaultsReadOnlyComposableDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(DefaultsReadOnlyComposableDetector.ISSUE)

    private val ComposableStub =
        kotlin(
                """
            package androidx.compose.runtime
            annotation class Composable
            annotation class ReadOnlyComposable
            """
            )
            .indented()

    @Test
    fun valid_annotated() {
        lint()
            .files(
                ComposableStub,
                kotlin(
                        "src/androidx/compose/material3/CardDefaults.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.ReadOnlyComposable

                    object CardDefaults {
                        val shape: String
                            @Composable @ReadOnlyComposable get() = "shape"
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun invalid_missing() {
        lint()
            .files(
                ComposableStub,
                kotlin(
                        "src/androidx/compose/material3/CardDefaults.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable

                    object CardDefaults {
                        val shape: String
                            @Composable get() = "shape"
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/CardDefaults.kt:6: Error: Composable properties in Defaults objects should be annotated with @ReadOnlyComposable [DefaultsMissingReadOnlyComposable]
                    val shape: String
                        ~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Autofix for src/androidx/compose/material3/CardDefaults.kt line 6: Add @ReadOnlyComposable:
                @@ -3,0 +4 @@
                +import androidx.compose.runtime.ReadOnlyComposable
                @@ -6,0 +8 @@
                +        @ReadOnlyComposable
                """
                    .trimIndent()
            )
    }

    @Test
    fun ignored_nonDefaults() {
        lint()
            .files(
                ComposableStub,
                kotlin(
                        "src/androidx/compose/material3/CardUtils.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable

                    object CardUtils {
                        val shape: String
                            @Composable get() = "shape"
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun ignored_nonComposable() {
        lint()
            .files(
                ComposableStub,
                kotlin(
                        "src/androidx/compose/material3/CardDefaults.kt",
                        """
                    package androidx.compose.material3

                    object CardDefaults {
                        val shape: String
                            get() = "shape"
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun ignored_function() {
        lint()
            .files(
                ComposableStub,
                kotlin(
                        "src/androidx/compose/material3/CardDefaults.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable

                    object CardDefaults {
                        @Composable
                        fun colors(): String = "colors"
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun invalid_wearComposeMaterial3_missing() {
        lint()
            .files(
                ComposableStub,
                kotlin(
                        "src/androidx/wear/compose/material3/CardDefaults.kt",
                        """
                    package androidx.wear.compose.material3

                    import androidx.compose.runtime.Composable

                    object CardDefaults {
                        val shape: String
                            @Composable get() = "shape"
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/wear/compose/material3/CardDefaults.kt:6: Error: Composable properties in Defaults objects should be annotated with @ReadOnlyComposable [DefaultsMissingReadOnlyComposable]
                    val shape: String
                        ~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun ignored_callsNonReadOnlyComposableFunction() {
        lint()
            .files(
                ComposableStub,
                kotlin(
                        "src/androidx/wear/compose/material3/TimePickerDefaults.kt",
                        """
                    package androidx.wear.compose.material3

                    import androidx.compose.runtime.Composable

                    @Composable
                    fun is24HourFormat(): Boolean = true

                    object TimePickerDefaults {
                        val timePickerType: String
                            @Composable
                            get() = if (is24HourFormat()) "24" else "12"
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun ignored_callsRemember() {
        lint()
            .files(
                ComposableStub,
                kotlin(
                        """
                    package androidx.compose.runtime
                    @Composable
                    fun <T> remember(calculation: () -> T): T = calculation()
                    """
                    )
                    .indented(),
                kotlin(
                        "src/androidx/compose/material3/CardDefaults.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.remember

                    object CardDefaults {
                        val shape: String
                            @Composable
                            get() = remember { "shape" }
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun ignored_callsNonReadOnlyComposableProperty() {
        lint()
            .files(
                ComposableStub,
                kotlin(
                        "src/androidx/compose/material3/Theme.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable

                    object Theme {
                        val regularProp: String
                            @Composable get() = "regular"
                    }
                    """,
                    )
                    .indented(),
                kotlin(
                        "src/androidx/compose/material3/CardDefaults.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable

                    object CardDefaults {
                        val shape: String
                            @Composable
                            get() = Theme.regularProp
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun invalid_callsReadOnlyComposable() {
        lint()
            .files(
                ComposableStub,
                kotlin(
                        "src/androidx/compose/material3/MaterialTheme.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.ReadOnlyComposable

                    object MaterialTheme {
                        val shape: String
                            @Composable @ReadOnlyComposable get() = "shape"
                    }
                    """,
                    )
                    .indented(),
                kotlin(
                        "src/androidx/compose/material3/CardDefaults.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable

                    object CardDefaults {
                        val shape: String
                            @Composable
                            get() = MaterialTheme.shape
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/CardDefaults.kt:6: Error: Composable properties in Defaults objects should be annotated with @ReadOnlyComposable [DefaultsMissingReadOnlyComposable]
                    val shape: String
                        ~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun invalid_missing_propertyTarget() {
        lint()
            .files(
                ComposableStub,
                kotlin(
                        "src/androidx/compose/material3/CardDefaults.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable

                    interface CardDefaults {
                        @get:Composable
                        val shape: String
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/CardDefaults.kt:7: Error: Composable properties in Defaults objects should be annotated with @ReadOnlyComposable [DefaultsMissingReadOnlyComposable]
                    val shape: String
                        ~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Autofix for src/androidx/compose/material3/CardDefaults.kt line 7: Add @ReadOnlyComposable:
                @@ -3,0 +4 @@
                +import androidx.compose.runtime.ReadOnlyComposable
                @@ -5,0 +7 @@
                +    @get:ReadOnlyComposable
                """
                    .trimIndent()
            )
    }
}
