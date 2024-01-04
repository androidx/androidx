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

package androidx.build.lint

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DeprecationMismatchDetectorTest : AbstractLintDetectorTest(
    useDetector = DeprecationMismatchDetector(),
    useIssues = listOf(DeprecationMismatchDetector.ISSUE),
) {
    @Test
    fun `Test correctly matched @deprecated and @Deprecated`() {
        val input = arrayOf(
            java(
                """
                    package java.androidx;

                    /**
                     * @deprecated Foo is deprecated
                     */
                    @Deprecated
                    public class Foo {
                        /**
                         * @deprecated foo is deprecated
                         */
                        @Deprecated
                        public void foo() {}

                        /**
                         * @deprecated FOO is deprecated
                         */
                        @Deprecated
                        public static int FOO = 0;

                        /**
                         * @deprecated InnerFoo is deprecated
                         */
                        @Deprecated
                        public interface InnerFoo {}
                    }
                """.trimIndent()
            )
        )

        check(*input).expectClean()
    }

    @Test
    fun `Test @deprecated missing @Deprecated`() {
        val input = arrayOf(
            java(
                """
                    package java.androidx;

                    /**
                     * @deprecated Foo is deprecated
                     */
                    public class Foo {
                        /**
                         * @deprecated foo is deprecated
                         */
                        public void foo() {}

                        /**
                         * @deprecated FOO is deprecated
                         */
                        public static int FOO = 0;

                        /**
                         * @deprecated InnerFoo is deprecated
                         */
                        public interface InnerFoo {}
                    }
                """.trimIndent()
            )
        )

        /* ktlint-disable max-line-length */
        val expected = """
            src/java/androidx/Foo.java:6: Error: Items with a @deprecated doc tag must be annotated with @Deprecated [DeprecationMismatch]
            public class Foo {
                         ~~~
            src/java/androidx/Foo.java:10: Error: Items with a @deprecated doc tag must be annotated with @Deprecated [DeprecationMismatch]
                public void foo() {}
                            ~~~
            src/java/androidx/Foo.java:15: Error: Items with a @deprecated doc tag must be annotated with @Deprecated [DeprecationMismatch]
                public static int FOO = 0;
                                  ~~~
            src/java/androidx/Foo.java:20: Error: Items with a @deprecated doc tag must be annotated with @Deprecated [DeprecationMismatch]
                public interface InnerFoo {}
                                 ~~~~~~~~
            4 errors, 0 warnings
        """.trimIndent()

        val expectedFixDiffs = """
            Autofix for src/java/androidx/Foo.java line 6: Annotate with @Deprecated:
            @@ -3 +3
            + @Deprecated
            Autofix for src/java/androidx/Foo.java line 10: Annotate with @Deprecated:
            @@ -7 +7
            +     @Deprecated
            Autofix for src/java/androidx/Foo.java line 15: Annotate with @Deprecated:
            @@ -12 +12
            +     @Deprecated
            Autofix for src/java/androidx/Foo.java line 20: Annotate with @Deprecated:
            @@ -17 +17
            +     @Deprecated
        """.trimIndent()
        /* ktlint-enable max-line-length */

       check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test @Deprecated missing @deprecated`() {
        val input = arrayOf(
            java(
                """
                    package java.androidx;

                    @Deprecated
                    public class Foo {
                        @Deprecated
                        public void foo() {}

                        @Deprecated
                        public static int FOO = 0;

                        @Deprecated
                        public interface InnerFoo {}
                    }
                """.trimIndent()
            )
        )

        /* ktlint-disable max-line-length */
        val expected = """
            src/java/androidx/Foo.java:4: Error: Items annotated with @Deprecated must have a @deprecated doc tag [DeprecationMismatch]
            public class Foo {
                         ~~~
            src/java/androidx/Foo.java:6: Error: Items annotated with @Deprecated must have a @deprecated doc tag [DeprecationMismatch]
                public void foo() {}
                            ~~~
            src/java/androidx/Foo.java:9: Error: Items annotated with @Deprecated must have a @deprecated doc tag [DeprecationMismatch]
                public static int FOO = 0;
                                  ~~~
            src/java/androidx/Foo.java:12: Error: Items annotated with @Deprecated must have a @deprecated doc tag [DeprecationMismatch]
                public interface InnerFoo {}
                                 ~~~~~~~~
            4 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Test @deprecated not required for private APIs`() {
        val input = arrayOf(
            java(
                """
                    package java.androidx;

                    @Deprecated
                    private class Foo {
                        @Deprecated
                        private void foo() {}

                        @Deprecated
                        private static int FOO = 0;

                        @Deprecated
                        private interface InnerFoo {}
                    }
                """.trimIndent()
            )
        )
        check(*input).expectClean()
    }

    @Test
    fun `Test @deprecated not required for proto-generated APIs`() {
        val input = arrayOf(
            java(
                """
                    // Generated by the protocol buffer compiler.  DO NOT EDIT!
                    package java.androidx.proto;

                    @Deprecated
                    public class Foo {
                        @Deprecated
                        public void foo() {}

                        @Deprecated
                        public static int FOO = 0;

                        @Deprecated
                        public interface InnerFoo {}
                    }
                """.trimIndent()
            )
        )
        check(*input).expectClean()
    }

    @Test
    fun `Test anonymous classes don't need @deprecated`() {
        val input = arrayOf(
            java(
                """
                    package java.androidx;

                    /**
                     * @deprecated Foo is deprecated
                     */
                    @Deprecated
                    public abstract class Foo<T> {
                        /**
                         * @deprecated foo is deprecated
                         */
                        @Deprecated
                        public void foo();
                    }
                """.trimIndent()
            ),
            java(
                """
                    package java.androidx;

                    public class Bar {
                        public static void bar() {
                            new Foo<String>() {
                                @Override
                                public void foo() {}
                            }.foo();
                        }
                    }
                """.trimIndent()
            )
        )

        check(*input).expectClean()
    }

    @Test
    fun `Test @RestrictTo APIs don't need @deprecated`() {
        val input = arrayOf(
            java(
                """
                    package java.androidx;

                    import androidx.annotation.RestrictTo;

                    @RestrictTo(RestrictTo.Scope.LIBRARY)
                    @Deprecated
                    public class Foo {
                        @Deprecated
                        private void foo() {}

                        @Deprecated
                        private static int FOO = 0;

                        @Deprecated
                        private interface InnerFoo {}
                    }
                """.trimIndent()
            ),
            Stubs.RestrictTo
        )
        check(*input).expectClean()
    }

    @Test
    fun `Test overriding methods don't need @deprecated`() {
        val input = arrayOf(
            java(
                """
                    package java.androidX;

                    public interface MyInterface {
                        /** @deprecated Use XYZ instead. */
                        @Deprecated
                        void inheritedNoComment();

                        /** @deprecated Use XYZ instead. */
                        @Deprecated
                        void inheritedWithComment();

                        /** @deprecated Use XYZ instead. */
                        @Deprecated
                        void inheritedWithInheritDoc();
                    }
                """,
            ),
            java(
                """
                    package test.pkg;

                    public class MyClass implements MyInterface {
                        @Deprecated
                        @Override
                        public void inheritedNoComment() {}

                        /** @deprecated Use XYZ instead. */
                        @Deprecated
                        @Override
                        public void inheritedWithComment() {}

                        /** {@inheritDoc} */
                        @Deprecated
                        @Override
                        public void inheritedWithInheritDoc() {}
                    }
                """
            )
        )
        check(*input).expectClean()
    }
}
