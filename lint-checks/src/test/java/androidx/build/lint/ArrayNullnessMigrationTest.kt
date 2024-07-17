/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ArrayNullnessMigrationTest :
    AbstractLintDetectorTest(
        useDetector = ArrayNullnessMigration(),
        useIssues = listOf(ArrayNullnessMigration.ISSUE),
        stubs = annotationStubs
    ) {
    @Test
    fun `Nullness annotation on parameter`() {
        val input =
            java(
                """
                package test.pkg;
                import androidx.annotation.NonNull;
                public class Foo {
                    public void foo(@NonNull String[] arr) {}
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test/pkg/Foo.java:4: Error: Nullness annotation on array will apply to element [ArrayMigration]
                public void foo(@NonNull String[] arr) {}
                                ~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Autofix for src/test/pkg/Foo.java line 4: Move annotation:
            @@ -4 +4
            -     public void foo(@NonNull String[] arr) {}
            +     public void foo(String @NonNull [] arr) {}
            """
                .trimIndent()

        runArrayNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on method return`() {
        val input =
            java(
                """
                package test.pkg;
                import androidx.annotation.Nullable;
                public class Foo {
                    @Nullable
                    public String[] foo() { return null; }
                }
                """
                    .trimIndent(),
            )

        val expected =
            """
            src/test/pkg/Foo.java:5: Error: Nullness annotation on array will apply to element [ArrayMigration]
                public String[] foo() { return null; }
                                ~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Autofix for src/test/pkg/Foo.java line 5: Move annotation:
            @@ -4 +4
            -     @Nullable
            -     public String[] foo() { return null; }
            +     public String @Nullable [] foo() { return null; }
            """
                .trimIndent()

        runArrayNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on method return and parameter`() {
        val input =
            java(
                """
                package test.pkg;
                import androidx.annotation.Nullable;
                public class Foo {
                    @Nullable
                    public String[] foo(@Nullable String[] arr) { return null; }
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test/pkg/Foo.java:5: Error: Nullness annotation on array will apply to element [ArrayMigration]
                public String[] foo(@Nullable String[] arr) { return null; }
                                ~~~
            src/test/pkg/Foo.java:5: Error: Nullness annotation on array will apply to element [ArrayMigration]
                public String[] foo(@Nullable String[] arr) { return null; }
                                    ~~~~~~~~~~~~~~~~~~~~~~
            2 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Autofix for src/test/pkg/Foo.java line 5: Move annotation:
            @@ -4 +4
            -     @Nullable
            -     public String[] foo(@Nullable String[] arr) { return null; }
            +     public String @Nullable [] foo(@Nullable String[] arr) { return null; }
            Autofix for src/test/pkg/Foo.java line 5: Move annotation:
            @@ -5 +5
            -     public String[] foo(@Nullable String[] arr) { return null; }
            +     public String[] foo(String @Nullable [] arr) { return null; }
            """
                .trimIndent()

        runArrayNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on field`() {
        val input =
            java(
                """
                package test.pkg;
                import androidx.annotation.Nullable;
                public class Foo {
                    @Nullable public String[] foo;
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test/pkg/Foo.java:4: Error: Nullness annotation on array will apply to element [ArrayMigration]
                @Nullable public String[] foo;
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Autofix for src/test/pkg/Foo.java line 4: Move annotation:
            @@ -4 +4
            -     @Nullable public String[] foo;
            +     public String @Nullable [] foo;
            """
                .trimIndent()

        runArrayNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on 2d array`() {
        val input =
            java(
                """
                package test.pkg;
                import androidx.annotation.Nullable;
                public class Foo {
                    @Nullable public String[][] foo;
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test/pkg/Foo.java:4: Error: Nullness annotation on array will apply to element [ArrayMigration]
                @Nullable public String[][] foo;
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Autofix for src/test/pkg/Foo.java line 4: Move annotation:
            @@ -4 +4
            -     @Nullable public String[][] foo;
            +     public String @Nullable [][] foo;
            """
                .trimIndent()

        runArrayNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on varargs`() {
        val input =
            java(
                """
                package test.pkg;
                import androidx.annotation.NonNull;
                public class Foo {
                    public void foo(@NonNull String... arr) {}
                }
                """
                    .trimIndent(),
            )

        val expected =
            """
            src/test/pkg/Foo.java:4: Error: Nullness annotation on array will apply to element [ArrayMigration]
                public void foo(@NonNull String... arr) {}
                                ~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Autofix for src/test/pkg/Foo.java line 4: Move annotation:
            @@ -4 +4
            -     public void foo(@NonNull String... arr) {}
            +     public void foo(String @NonNull ... arr) {}
            """
                .trimIndent()

        runArrayNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on method return with array in comments`() {
        val input =
            java(
                """
                package test.pkg;
                import androidx.annotation.Nullable;
                public class Foo {
                   /**
                    * @return A String[]
                    */
                    @Nullable
                    public String[] foo() { return null; }
                }
                """
                    .trimIndent(),
            )

        val expected =
            """
            src/test/pkg/Foo.java:8: Error: Nullness annotation on array will apply to element [ArrayMigration]
                public String[] foo() { return null; }
                                ~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Autofix for src/test/pkg/Foo.java line 8: Move annotation:
            @@ -7 +7
            -     @Nullable
            -     public String[] foo() { return null; }
            +     public String @Nullable [] foo() { return null; }
            """
                .trimIndent()

        runArrayNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on method return with annotation in comments`() {
        val input =
            java(
                """
                package test.pkg;
                import androidx.annotation.Nullable;
                public class Foo {
                   /**
                    * @return A @Nullable string array
                    */
                    @Nullable
                    public String[] foo() { return null; }
                }
                """
                    .trimIndent(),
            )

        val expected =
            """
            src/test/pkg/Foo.java:8: Error: Nullness annotation on array will apply to element [ArrayMigration]
                public String[] foo() { return null; }
                                ~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Autofix for src/test/pkg/Foo.java line 8: Move annotation:
            @@ -7 +7
            -     @Nullable
            -     public String[] foo() { return null; }
            +     public String @Nullable [] foo() { return null; }
            """
                .trimIndent()

        runArrayNullnessTest(input, expected, expectedFixDiffs)
    }

    private fun runArrayNullnessTest(input: TestFile, expected: String, expectedFixDiffs: String) {
        lint()
            .files(*stubs, input)
            .skipTestModes(TestMode.WHITESPACE)
            .run()
            .expect(expected)
            .expectFixDiffs(expectedFixDiffs)
    }

    companion object {
        val annotationStubs =
            arrayOf(
                kotlin(
                    """
                        package androidx.annotation
                        annotation class NonNull
                    """
                ),
                kotlin(
                    """
                        package androidx.annotation
                        annotation class Nullable
                    """
                )
            )
    }
}
