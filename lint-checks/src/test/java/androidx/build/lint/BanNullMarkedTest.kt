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

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanNullMarkedTest :
    AbstractLintDetectorTest(
        useDetector = BanNullMarked(),
        useIssues = listOf(BanNullMarked.ISSUE),
        stubs = arrayOf(nullMarkedStub)
    ) {
    @Test
    fun `Usage of NullMarked in a package-info file`() {
        val input =
            java(
                """
                    @NullMarked
                    package test.pkg;

                    import org.jspecify.annotations.NullMarked;
                """
                    .trimIndent()
            )

        val expected =
            """
                src/test/pkg/package-info.java:1: Error: Should not use @NullMarked annotation [BanNullMarked]
                @NullMarked
                ~~~~~~~~~~~
                1 errors, 0 warnings
            """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Usage of NullMarked on a class`() {
        val input =
            java(
                """
                    package test.pkg;

                    import org.jspecify.annotations.NullMarked;

                    @NullMarked
                    public class Foo {}
                """
                    .trimIndent()
            )

        val expected =
            """
                src/test/pkg/Foo.java:5: Error: Should not use @NullMarked annotation [BanNullMarked]
                @NullMarked
                ~~~~~~~~~~~
                1 errors, 0 warnings
            """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Usage of NullMarked on a method`() {
        val input =
            java(
                """
                    package test.pkg;

                    import org.jspecify.annotations.NullMarked;

                    public class Foo {
                        @NullMarked
                        public void foo() {}
                    }
                """
                    .trimIndent()
            )

        val expected =
            """
                src/test/pkg/Foo.java:6: Error: Should not use @NullMarked annotation [BanNullMarked]
                    @NullMarked
                    ~~~~~~~~~~~
                1 errors, 0 warnings
            """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Usage of NullMarked on a constructor`() {
        val input =
            java(
                """
                    package test.pkg;

                    import org.jspecify.annotations.NullMarked;

                    public class Foo {
                        @NullMarked
                        public Foo() {}
                    }
                """
                    .trimIndent()
            )

        val expected =
            """
                src/test/pkg/Foo.java:6: Error: Should not use @NullMarked annotation [BanNullMarked]
                    @NullMarked
                    ~~~~~~~~~~~
                1 errors, 0 warnings
            """
                .trimIndent()

        check(input).expect(expected)
    }

    companion object {
        private val nullMarkedStub =
            java(
                """
                    package org.jspecify.annotations;

                    import java.lang.annotation.ElementType;
                    import java.lang.annotation.Target;

                    @Target({ElementType.MODULE, ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
                    public @interface NullMarked {}
                """
                    .trimIndent()
            )
    }
}
