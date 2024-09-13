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

import com.android.tools.lint.checks.infrastructure.ProjectDescription
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AutoValueNullnessOverrideTest :
    AbstractLintDetectorTest(
        useDetector = AutoValueNullnessOverride(),
        useIssues = listOf(AutoValueNullnessOverride.ISSUE),
        stubs = arrayOf(autovalueStub, jspecifyNonNullStub, jspecifyNullableStub)
    ) {
    @Test
    fun `No superclass`() {
        val input =
            java(
                """
                    package test.pkg;
                    import com.google.auto.value.AutoValue;
                    import org.jspecify.annotations.Nullable;
                    @AutoValue
                    public abstract class Foo {
                        public abstract @Nullable String getString();
                    }
                """
                    .trimIndent()
            )
        check(input).expectClean()
    }

    @Test
    fun `Superclass in same library`() {
        val input =
            arrayOf(
                java(
                    """
                        package test.pkg;
                        import com.google.auto.value.AutoValue;
                        @AutoValue
                        public abstract class Foo extends ParentClass {
                        }
                    """
                        .trimIndent()
                ),
                java(
                    """
                        package test.pkg;
                        import org.jspecify.annotations.Nullable;
                        public abstract class ParentClass {
                            public abstract @Nullable String getString();
                        }
                    """
                        .trimIndent()
                )
            )
        check(*input).expectClean()
    }

    @Test
    fun `Superclass in different library`() {
        // Files needs to be set up in project structure for the lint to understand they are from
        // different libraries
        val jspecify =
            project()
                .files(jspecifyNonNullStub, jspecifyNullableStub)
                .type(ProjectDescription.Type.LIBRARY)

        val autovalue = project().files(autovalueStub).type(ProjectDescription.Type.LIBRARY)

        val parentLibrary =
            project()
                .files(
                    java(
                        """
                            package androidx.example;
                            import org.jspecify.annotations.NonNull;
                            import org.jspecify.annotations.Nullable;
                            public abstract class SuperClass {
                                public abstract @Nullable String getNullableStringNotOverridden();
                                public abstract @NonNull String getNonNullStringNotOverridden();
                                public abstract String getUnannotatedStringNotOverridden();

                                public abstract @Nullable String getNullableStringOverridden();

                                public abstract @Nullable String getNullableStringOverrideNotAbstract();
                            }
                        """
                            .trimIndent(),
                    ),
                    gradle(
                        """
                            apply plugin: 'com.android.library'
                            group=androidx.example
                        """
                    )
                )
                .dependsOn(jspecify)
                .type(ProjectDescription.Type.LIBRARY)

        val sourceProject =
            project()
                .files(
                    java(
                        """
                            package test.pkg;
                            import com.google.auto.value.AutoValue;
                            import androidx.example.SuperClass;
                            @AutoValue
                            public abstract class Foo extends SuperClass {
                                @Override
                                public abstract @Nullable String getNullableStringOverridden();

                                @Override
                                public abstract @Nullable String getNullableStringOverrideNotAbstract() {
                                    return null;
                                }
                            }
                        """
                            .trimIndent(),
                    ),
                    gradle(
                        """
                            apply plugin: 'com.android.library'
                            group=test.pkg
                        """
                    )
                )
                .dependsOn(jspecify)
                .dependsOn(autovalue)
                .dependsOn(parentLibrary)

        val expected =
            """
                src/main/java/test/pkg/Foo.java:5: Error: Methods need @Nullable overrides for AutoValue: getNullableStringNotOverridden() [AutoValueNullnessOverride]
                public abstract class Foo extends SuperClass {
                                      ~~~
                1 errors, 0 warnings
            """
                .trimIndent()
        val expectedFixDiffs =
            """
                Fix for src/main/java/test/pkg/Foo.java line 5: Replace with ...:
                @@ -6 +6
                + @Override
                + public abstract @Nullable String getNullableStringNotOverridden();
            """
                .trimIndent()

        lint().projects(sourceProject).run().expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    companion object {
        private val autovalueStub =
            kotlin(
                """
                    package com.google.auto.value
                    annotation class AutoValue
                """
                    .trimIndent()
            )
        private val jspecifyNullableStub =
            kotlin(
                """
                    package org.jspecify.annotations
                    @Target(AnnotationTarget.TYPE)
                    annotation class Nullable
                """
                    .trimIndent()
            )
        private val jspecifyNonNullStub =
            kotlin(
                """
                    package org.jspecify.annotations
                    @Target(AnnotationTarget.TYPE)
                    annotation class NonNull
                """
                    .trimIndent()
            )
    }
}
