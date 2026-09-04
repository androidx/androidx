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
class TopLevelCompositionLocalDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = TopLevelCompositionLocalDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(TopLevelCompositionLocalDetector.ISSUE)

    private val CompositionLocalStub =
        kotlin(
                """
            package androidx.compose.runtime

            interface CompositionLocal<T>
            interface ProvidableCompositionLocal<T> : CompositionLocal<T>

            fun <T> compositionLocalOf(defaultFactory: () -> T): ProvidableCompositionLocal<T> = error("")
            """
            )
            .indented()

    @Test
    fun invalid_topLevelPublic() {
        lint()
            .files(
                CompositionLocalStub,
                kotlin(
                        "src/androidx/compose/material3/LocalFoo.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.compositionLocalOf

                    val LocalFoo = compositionLocalOf { "foo" }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/LocalFoo.kt:5: Error: CompositionLocals should not be defined as top-level public properties. Scope them inside an object or make them internal/private. [TopLevelCompositionLocal]
                val LocalFoo = compositionLocalOf { "foo" }
                    ~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Autofix for src/androidx/compose/material3/LocalFoo.kt line 5: Make internal:
                @@ -5 +5
                - val LocalFoo = compositionLocalOf { "foo" }
                + internal val LocalFoo = compositionLocalOf { "foo" }
                """
                    .trimIndent()
            )
    }

    @Test
    fun valid_topLevelPrivate() {
        lint()
            .files(
                CompositionLocalStub,
                kotlin(
                        "src/androidx/compose/material3/LocalFoo.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.compositionLocalOf

                    private val LocalFoo = compositionLocalOf { "foo" }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun valid_topLevelInternal() {
        lint()
            .files(
                CompositionLocalStub,
                kotlin(
                        "src/androidx/compose/material3/LocalFoo.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.compositionLocalOf

                    internal val LocalFoo = compositionLocalOf { "foo" }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun valid_scopedPublic() {
        lint()
            .files(
                CompositionLocalStub,
                kotlin(
                        "src/androidx/compose/material3/LocalFoo.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.compositionLocalOf

                    object FooDefaults {
                        val LocalFoo = compositionLocalOf { "foo" }
                    }
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun ignored_nonCompositionLocal() {
        lint()
            .files(
                CompositionLocalStub,
                kotlin(
                        "src/androidx/compose/material3/LocalFoo.kt",
                        """
                    package androidx.compose.material3

                    val Foo = "bar"
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }
}
