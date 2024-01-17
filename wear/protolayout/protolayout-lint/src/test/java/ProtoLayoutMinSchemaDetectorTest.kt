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

@file:Suppress("UnstableApiUsage")

package androidx.wear.protolayout.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProtoLayoutMinSchemaDetectorTest : LintDetectorTest() {
    override fun getDetector() = ProtoLayoutMinSchemaDetector()

    override fun getIssues() = mutableListOf(ProtoLayoutMinSchemaDetector.ISSUE)

    private val requiresSchemaAnnotationStub =
        java(
            """
            package androidx.wear.protolayout.expression;

            @Retention(CLASS)
            @Target({TYPE, METHOD, CONSTRUCTOR, FIELD})
            public @interface RequiresSchemaVersion {
                int major();
                int minor();
            }
        """
                .trimIndent()
        )
    private val requiresApiAnnotationStub =
        kotlin(
            """
            package androidx.annotation
            @Retention(AnnotationRetention.BINARY)
            @Target(
                AnnotationTarget.ANNOTATION_CLASS,
                AnnotationTarget.CLASS,
                AnnotationTarget.FUNCTION,
                AnnotationTarget.PROPERTY_GETTER,
                AnnotationTarget.PROPERTY_SETTER,
                AnnotationTarget.CONSTRUCTOR,
                AnnotationTarget.FIELD,
                AnnotationTarget.FILE
            )
            public actual annotation class RequiresApi(
                val value: Int = 1,
                val api: Int = 1
            )
            """
                .trimIndent()
        )

    @Test
    fun `calling V1_0 API doesn't`() {
        lint()
            .files(
                requiresSchemaAnnotationStub,
                kotlin(
                        """
            package foo
            import androidx.wear.protolayout.expression.RequiresSchemaVersion

            @RequiresSchemaVersion(major=1, minor=0)
            class WithAnnotation {
              fun unAnnotatedMethod(){}
            }
                """
                    )
                    .indented(),
                kotlin(
                        """
            package foo
            import androidx.wear.protolayout.expression.RequiresSchemaVersion

            class Bar {
              @RequiresSchemaVersion(major=1, minor=0)
              fun bar() {}

              fun baz() { bar() }
            }
        """
                    )
                    .indented()
            )
            .issues(ProtoLayoutMinSchemaDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun `calling V1_2 API requires SDK version check`() {
        lint()
            .files(
                requiresSchemaAnnotationStub,
                kotlin(
                        """
            package foo
            import androidx.wear.protolayout.expression.RequiresSchemaVersion

            @RequiresSchemaVersion(major=1, minor=200)
            class WithAnnotation {
              fun unAnnotatedMethod(){}

              @RequiresSchemaVersion(major=1, minor=200)
              fun annotatedMethod(){}

              @RequiresSchemaVersion(major=1, minor=200)
              fun unreferencedMethod(){}

              companion object {
                @RequiresSchemaVersion(major=1, minor=200)
                const val ANNOTATED_CONST = 10
              }
            }
                """
                    )
                    .indented(),
                kotlin(
                        """
            package foo
            import androidx.wear.protolayout.expression.RequiresSchemaVersion

            class Bar {
              private val withAnnotation = WithAnnotation()
              private val fieldAssignment = withAnnotation.annotatedMethod()

              @RequiresSchemaVersion(major=1, minor=200)
              fun bar() {}

              fun baz() {
                bar()
                withAnnotation.unAnnotatedMethod()
                withAnnotation.annotatedMethod()
                //TODO: b/308552481 - This should fail
                val b = withAnnotation.ANNOTATED_CONST
              }
            }
        """
                    )
                    .indented()
            )
            .issues(ProtoLayoutMinSchemaDetector.ISSUE)
            .run()
            .expect(
                """
src/foo/Bar.kt:6: Error: This API is not guaranteed to be available on the device (requires schema 1.200). [ProtoLayoutMinSchema]
  private val fieldAssignment = withAnnotation.annotatedMethod()
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/Bar.kt:12: Error: This API is not guaranteed to be available on the device (requires schema 1.200). [ProtoLayoutMinSchema]
    bar()
    ~~~~~
src/foo/Bar.kt:14: Error: This API is not guaranteed to be available on the device (requires schema 1.200). [ProtoLayoutMinSchema]
    withAnnotation.annotatedMethod()
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
            """
                    .trimIndent()
            )

        lint()
            .files(
                requiresSchemaAnnotationStub,
                kotlin(
                        """
            package foo
            import androidx.wear.protolayout.expression.RequiresSchemaVersion

            @RequiresSchemaVersion(major=1, minor=200)
            class WithAnnotation {
              fun unAnnotatedMethod(){}

              @RequiresSchemaVersion(major=1, minor=200)
              fun annotatedMethod(){}

              @RequiresSchemaVersion(major=1, minor=200)
              fun unreferencedMethod(){}
            }
                """
                    )
                    .indented(),
                kotlin(
                        """
            package foo
            import androidx.wear.protolayout.expression.RequiresSchemaVersion
            import android.os.Build

            class Bar {
              private val withAnnotation = WithAnnotation()

              @RequiresSchemaVersion(major=1, minor=200)
              fun bar() {}

              fun baz() {
                if (Build.VERSION.SDK_INT >= 33) {
                  bar()
                  withAnnotation.unAnnotatedMethod()
                  withAnnotation.annotatedMethod()
                }
              }
            }
        """
                    )
                    .indented()
            )
            .issues(ProtoLayoutMinSchemaDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun `calling V1_2 API requires SDK version check (Java)`() {
        lint()
            .files(
                requiresSchemaAnnotationStub,
                java(
                        """
            package foo;
            import androidx.wear.protolayout.expression.RequiresSchemaVersion;

            class Bar {
              @RequiresSchemaVersion(major=1, minor=200)
              public static final int ANNOTATED_CONSTANT = 10;

              @RequiresSchemaVersion(major=1, minor=200)
              void bar() {}

              void baz() {
                bar();
                // TODO: b/308552481: This should fail
                int t = ANNOTATED_CONSTANT;
              }
            }
        """
                    )
                    .indented()
            )
            .issues(ProtoLayoutMinSchemaDetector.ISSUE)
            .run()
            .expect(
                """
src/foo/Bar.java:12: Error: This API is not guaranteed to be available on the device (requires schema 1.200). [ProtoLayoutMinSchema]
    bar();
    ~~~~~
1 errors, 0 warnings
            """
                    .trimIndent()
            )

        lint()
            .files(
                requiresSchemaAnnotationStub,
                java(
                        """
            package foo;
            import androidx.wear.protolayout.expression.RequiresSchemaVersion;
            import android.os.Build;

            class Bar {
              @RequiresSchemaVersion(major=1, minor=200)
              void bar() {}

              void baz() {
                if (Build.VERSION.SDK_INT >= 33) {
                  bar();
                }
              }
            }
        """
                    )
                    .indented()
            )
            .issues(ProtoLayoutMinSchemaDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun `annotated call-site doesn't requires SDK version check`() {
        lint()
            .files(
                requiresSchemaAnnotationStub,
                requiresApiAnnotationStub,
                kotlin(
                        """
            package foo
            import androidx.wear.protolayout.expression.RequiresSchemaVersion

            class BarK {
              @RequiresSchemaVersion(major=1, minor=200)
              private val fieldAssignment = bar()

              @RequiresSchemaVersion(major=1, minor=200)
              fun bar() = 1

              @RequiresSchemaVersion(major=1, minor=200)
              fun baz2() { bar() }

              @RequiresSchemaVersion(major=1, minor=300)
              fun baz3() { bar() }
            }
            """
                    )
                    .indented(),
                java(
                        """
            package foo;
            import androidx.wear.protolayout.expression.RequiresSchemaVersion;

            class BarJ {
              @RequiresSchemaVersion(major=1, minor=200)
              private static final int fieldAssignment = bar();

              @RequiresSchemaVersion(major=1, minor=200)
              public static int bar() { return 1;}

              @RequiresSchemaVersion(major=1, minor=200)
              void baz2() { bar(); }

              @RequiresSchemaVersion(major=1, minor=300)
              void baz3() { bar(); }
            }
        """
                    )
                    .indented(),
                kotlin(
                        """
            package foo
            import androidx.wear.protolayout.expression.RequiresSchemaVersion
            import androidx.annotation.RequiresApi

            @RequiresApi(33)
            class BazK {
              private val fieldAssignment = bar()

              @RequiresSchemaVersion(major=1, minor=200)
              fun bar() = 1

              @RequiresApi(30)
              fun baz2() { bar() }

              @RequiresApi(34)
              fun baz3() { BarJ.baz3() }
            }
            """
                    )
                    .indented()
            )
            .issues(ProtoLayoutMinSchemaDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun `project with proper minSdk doesn't requires SDK version check`() {
        lint()
            .files(
                manifest().minSdk(34),
                requiresSchemaAnnotationStub,
                kotlin(
                        """
            package foo
            import androidx.wear.protolayout.expression.RequiresSchemaVersion

            class BarK {
              @RequiresSchemaVersion(major=1, minor=200)
              private val fieldAssignment = bar()

              @RequiresSchemaVersion(major=1, minor=200)
              fun bar() = 1

              @RequiresSchemaVersion(major=1, minor=200)
              fun baz2() { bar() }

              @RequiresSchemaVersion(major=1, minor=300)
              fun baz3() { bar() }
            }
            """
                    )
                    .indented(),
                java(
                        """
            package foo;
            import androidx.wear.protolayout.expression.RequiresSchemaVersion;

            class BarJ {
              private static final int fieldAssignment = bar();

              fun bar() {}

              fun baz2() { bar(); }

              fun baz3() { bar(); }
            }
        """
                    )
                    .indented()
            )
            .issues(ProtoLayoutMinSchemaDetector.ISSUE)
            .run()
            .expectClean()
    }
}
