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
class JSpecifyNullnessMigrationTest :
    AbstractLintDetectorTest(
        useDetector = JSpecifyNullnessMigration(),
        useIssues = listOf(JSpecifyNullnessMigration.ISSUE),
        stubs = annotationStubs
    ) {
    @Test
    fun `Nullness annotation on array parameter`() {
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
            src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                public void foo(@NonNull String[] arr) {}
                                ~~~~~~~~
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

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on array method return`() {
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
            src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                @Nullable
                ~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Autofix for src/test/pkg/Foo.java line 4: Move annotation:
            @@ -4 +4
            -     @Nullable
            -     public String[] foo() { return null; }
            +     public String @Nullable [] foo() { return null; }
            """
                .trimIndent()

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on array method return and array parameter`() {
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
            src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                @Nullable
                ~~~~~~~~~
            src/test/pkg/Foo.java:5: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                public String[] foo(@Nullable String[] arr) { return null; }
                                    ~~~~~~~~~
            2 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Autofix for src/test/pkg/Foo.java line 4: Move annotation:
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

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on array field`() {
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
            src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                @Nullable public String[] foo;
                ~~~~~~~~~
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

        runNullnessTest(input, expected, expectedFixDiffs)
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
            src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                @Nullable public String[][] foo;
                ~~~~~~~~~
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

        runNullnessTest(input, expected, expectedFixDiffs)
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
            src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                public void foo(@NonNull String... arr) {}
                                ~~~~~~~~
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

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on array varargs`() {
        val input =
            java(
                """
                    package test.pkg;
                    import androidx.annotation.NonNull;
                    public class Foo {
                        public void foo(@NonNull String[]... args) {}
                    }
                """
                    .trimIndent()
            )

        val expected =
            """
                src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                    public void foo(@NonNull String[]... args) {}
                                    ~~~~~~~~
                1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
                Autofix for src/test/pkg/Foo.java line 4: Move annotation:
                @@ -4 +4
                -     public void foo(@NonNull String[]... args) {}
                +     public void foo(String @NonNull []... args) {}
            """
                .trimIndent()

        runNullnessTest(input, expected, expectedFixDiffs)
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
            src/test/pkg/Foo.java:7: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                @Nullable
                ~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Autofix for src/test/pkg/Foo.java line 7: Move annotation:
            @@ -7 +7
            -     @Nullable
            -     public String[] foo() { return null; }
            +     public String @Nullable [] foo() { return null; }
            """
                .trimIndent()

        runNullnessTest(input, expected, expectedFixDiffs)
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
            src/test/pkg/Foo.java:7: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                @Nullable
                ~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Autofix for src/test/pkg/Foo.java line 7: Move annotation:
            @@ -7 +7
            -     @Nullable
            -     public String[] foo() { return null; }
            +     public String @Nullable [] foo() { return null; }
            """
                .trimIndent()

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation removed from local variable declaration`() {
        val input =
            java(
                """
                    package test.pkg;
                    import androidx.annotation.Nullable;
                    public class Foo {
                        public String foo() {
                            @Nullable String str = null;
                            return str;
                        }
                    }
                """
                    .trimIndent()
            )
        val expected =
            """
            src/test/pkg/Foo.java:5: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                    @Nullable String str = null;
                    ~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()
        val expectedFixDiffs =
            """
            Autofix for src/test/pkg/Foo.java line 5: Delete:
            @@ -5 +5
            -         @Nullable String str = null;
            +         String str = null;
            """
                .trimIndent()

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation removed from void return type`() {
        val input =
            java(
                """
                    package test.pkg;
                    import androidx.annotation.Nullable;
                    public class Foo {
                        @Nullable
                        public void foo() {}
                    }
                """
                    .trimIndent()
            )

        val expected =
            """
                src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                    @Nullable
                    ~~~~~~~~~
                1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
                Autofix for src/test/pkg/Foo.java line 4: Delete:
                @@ -4 +4
                -     @Nullable
            """
                .trimIndent()

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation removed from primitive parameter type`() {
        val input =
            java(
                """
                    package test.pkg;
                    import androidx.annotation.Nullable;
                    public class Foo {
                        public void foo(@Nullable int i) {}
                    }
                """
                    .trimIndent()
            )

        val expected =
            """
                src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                    public void foo(@Nullable int i) {}
                                    ~~~~~~~~~
                1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
                Autofix for src/test/pkg/Foo.java line 4: Delete:
                @@ -4 +4
                -     public void foo(@Nullable int i) {}
                +     public void foo(int i) {}
            """
                .trimIndent()

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on class type parameter`() {
        val input =
            java(
                """
                package test.pkg;
                import androidx.annotation.NonNull;
                public class Foo {
                    public void foo(@NonNull Foo.InnerFoo arr) {}
                    public class InnerFoo {}
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                public void foo(@NonNull Foo.InnerFoo arr) {}
                                ~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
                Autofix for src/test/pkg/Foo.java line 4: Move annotation:
                @@ -4 +4
                -     public void foo(@NonNull Foo.InnerFoo arr) {}
                +     public void foo(Foo.@NonNull InnerFoo arr) {}
            """
                .trimIndent()

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on class type return`() {
        val input =
            java(
                """
                package test.pkg;
                import androidx.annotation.Nullable;
                public class Foo {
                    @Nullable
                    public String foo() { return null; }
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                @Nullable
                ~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
                Autofix for src/test/pkg/Foo.java line 4: Move annotation:
                @@ -4 +4
                -     @Nullable
                -     public String foo() { return null; }
                +     public @Nullable String foo() { return null; }
            """
                .trimIndent()

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on class type param`() {
        val input =
            java(
                """
                package test.pkg;
                import androidx.annotation.Nullable;
                public class Foo {
                    public String foo(@Nullable String foo) { return null; }
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                public String foo(@Nullable String foo) { return null; }
                                  ~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs = ""

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on class type param and return`() {
        val input =
            java(
                """
                package test.pkg;
                import androidx.annotation.NonNull;
                import androidx.annotation.Nullable;
                public class Foo {
                    @Nullable
                    public String foo(@NonNull String foo) { return null; }
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test/pkg/Foo.java:5: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                @Nullable
                ~~~~~~~~~
            src/test/pkg/Foo.java:6: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                public String foo(@NonNull String foo) { return null; }
                                  ~~~~~~~~
            2 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
                Autofix for src/test/pkg/Foo.java line 5: Move annotation:
                @@ -5 +5
                -     @Nullable
                -     public String foo(@NonNull String foo) { return null; }
                +     public @Nullable String foo(@NonNull String foo) { return null; }
            """
                .trimIndent()

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on type parameter return`() {
        val input =
            java(
                """
                    package test.pkg;
                    import androidx.annotation.Nullable;
                    public class Foo {
                        @Nullable
                        public <T> T foo() {
                            return null;
                        }
                    }
                """
                    .trimIndent()
            )

        val expected =
            """
                src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                    @Nullable
                    ~~~~~~~~~
                1 errors, 0 warnings
            """
                .trimIndent()
        val expectedFixDiffs =
            """
                Autofix for src/test/pkg/Foo.java line 4: Move annotation:
                @@ -4 +4
                -     @Nullable
                -     public <T> T foo() {
                +     public <T> @Nullable T foo() {
            """
                .trimIndent()

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on inner type where outer type contains name`() {
        val input =
            java(
                """
                    package test.pkg;
                    import androidx.annotation.Nullable;
                    public class RecyclerView {
                        public class Recycler {}
                        @Nullable
                        public RecyclerView.Recycler foo() {
                            return null;
                        }
                    }
                """
                    .trimIndent()
            )

        val expected =
            """
                src/test/pkg/RecyclerView.java:5: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                    @Nullable
                    ~~~~~~~~~
                1 errors, 0 warnings
            """
                .trimIndent()
        val expectedFixDiffs =
            """
                Autofix for src/test/pkg/RecyclerView.java line 5: Move annotation:
                @@ -5 +5
                -     @Nullable
                -     public RecyclerView.Recycler foo() {
                +     public RecyclerView.@Nullable Recycler foo() {
            """
                .trimIndent()

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on parameterized type`() {
        val input =
            java(
                """
                    package test.pkg;
                    import androidx.annotation.Nullable;
                    import java.util.List;
                    public class Foo {
                        @Nullable
                        public List<String> foo() {
                            return null;
                        }
                    }
                """
                    .trimIndent()
            )

        val expected =
            """
                src/test/pkg/Foo.java:5: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                    @Nullable
                    ~~~~~~~~~
                1 errors, 0 warnings
            """
                .trimIndent()
        val expectedFixDiffs =
            """
                Autofix for src/test/pkg/Foo.java line 5: Move annotation:
                @@ -5 +5
                -     @Nullable
                -     public List<String> foo() {
                +     public @Nullable List<String> foo() {
            """
                .trimIndent()

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    @Test
    fun `Nullness annotation on parameter with newline after type`() {
        val input =
            java(
                """
                    package test.pkg;
                    import androidx.annotation.NonNull;
                    public class Foo {
                        public void foo(@NonNull String
                            arg) {}
                    }
                """
                    .trimIndent()
            )

        val expected =
            """
                src/test/pkg/Foo.java:4: Error: Switch nullness annotation to JSpecify [JSpecifyNullness]
                    public void foo(@NonNull String
                                    ~~~~~~~~
                1 errors, 0 warnings
            """
                .trimIndent()
        val expectedFixDiffs = ""

        runNullnessTest(input, expected, expectedFixDiffs)
    }

    private fun runNullnessTest(input: TestFile, expected: String, expectedFixDiffs: String) {
        lint()
            .files(*stubs, input)
            // Skip WHITESPACE mode because an array suffix with whitespace in the middle "[ ]" will
            // break the pattern matching, but is extremely unlikely to happen in practice.
            // Skip FULLY_QUALIFIED mode because the type-use annotation positioning depends on
            // whether types are fully qualified, so fixes will be different.
            .skipTestModes(TestMode.WHITESPACE, TestMode.FULLY_QUALIFIED)
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
