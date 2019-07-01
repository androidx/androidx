/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.annotation.experimental.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.base64gzip
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExperimentalDetectorTest {

    private fun check(vararg testFiles: TestFile): TestLintResult {
        return lint()
            .files(
                EXPERIMENTAL_JAVA,
                USE_EXPERIMENTAL_JAVA,
                *testFiles
            )
            .allowMissingSdk(true)
            .issues(*ExperimentalDetector.ISSUES.toTypedArray())
            .run()
    }

    @Test
    fun useJavaExperimentalFromJava() {
        val input = arrayOf(
            javaSample("sample.DateProvider"),
            javaSample("sample.ExperimentalDateTime"),
            javaSample("sample.UseJavaExperimentalFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/UseJavaExperimentalFromJava.java:27: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTime' or '@UseExperimental(sample.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        DateProvider dateProvider = new DateProvider();
                                    ~~~~~~~~~~~~~~~~~~
src/sample/UseJavaExperimentalFromJava.java:28: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTime' or '@UseExperimental(sample.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        return dateProvider.getDate();
                            ~~~~~~~
2 errors, 0 warnings
    """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaExperimentalFromKt() {
        val input = arrayOf(
            javaSample("sample.DateProvider"),
            javaSample("sample.ExperimentalDateTime"),
            ktSample("sample.UseJavaExperimentalFromKt")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/UseJavaExperimentalFromKt.kt:27: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTime' or '@UseExperimental(sample.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        val dateProvider = DateProvider()
                           ~~~~~~~~~~~~
src/sample/UseJavaExperimentalFromKt.kt:28: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTime' or '@UseExperimental(sample.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        return dateProvider.date
                            ~~~~
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useKtExperimentalFromJava() {
        val input = arrayOf(
            EXPERIMENTAL_KT,
            ktSample("sample.DateProviderKt"),
            ktSample("sample.ExperimentalDateTimeKt"),
            javaSample("sample.UseKtExperimentalFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/UseKtExperimentalFromJava.java:27: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTimeKt' or '@UseExperimental(sample.ExperimentalDateTimeKt.class)' [UnsafeExperimentalUsageError]
        DateProviderKt dateProvider = new DateProviderKt();
                                      ~~~~~~~~~~~~~~~~~~~~
src/sample/UseKtExperimentalFromJava.java:28: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTimeKt' or '@UseExperimental(sample.ExperimentalDateTimeKt.class)' [UnsafeExperimentalUsageError]
        return dateProvider.getDate();
                            ~~~~~~~
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaPackageFromJava() {
        val input = arrayOf(
            SAMPLE_FOO_PACKAGE_INFO,
            javaSample("sample.foo.Bar"),
            javaSample("sample.foo.ExperimentalPackage"),
            javaSample("sample.UseJavaPackageFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/UseJavaPackageFromJava.java:30: Error: This declaration is experimental and its usage should be marked with
'@sample.foo.ExperimentalPackage' or '@UseExperimental(sample.foo.ExperimentalPackage.class)' [UnsafeExperimentalUsageError]
        Bar bar = new Bar();
                  ~~~~~~~~~
src/sample/UseJavaPackageFromJava.java:31: Error: This declaration is experimental and its usage should be marked with
'@sample.foo.ExperimentalPackage' or '@UseExperimental(sample.foo.ExperimentalPackage.class)' [UnsafeExperimentalUsageError]
        bar.baz();
            ~~~
src/sample/UseJavaPackageFromJava.java:54: Error: This declaration is experimental and its usage should be marked with
'@sample.foo.ExperimentalPackage' or '@UseExperimental(sample.foo.ExperimentalPackage.class)' [UnsafeExperimentalUsageError]
        callPackageExperimental();
        ~~~~~~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
    """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaPackageFromKt() {
        val input = arrayOf(
            SAMPLE_FOO_PACKAGE_INFO,
            javaSample("sample.foo.Bar"),
            javaSample("sample.foo.ExperimentalPackage"),
            ktSample("sample.UseJavaPackageFromKt")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/UseJavaPackageFromKt.kt:30: Error: This declaration is experimental and its usage should be marked with
'@sample.foo.ExperimentalPackage' or '@UseExperimental(sample.foo.ExperimentalPackage.class)' [UnsafeExperimentalUsageError]
        val bar = Bar()
                  ~~~
src/sample/UseJavaPackageFromKt.kt:31: Error: This declaration is experimental and its usage should be marked with
'@sample.foo.ExperimentalPackage' or '@UseExperimental(sample.foo.ExperimentalPackage.class)' [UnsafeExperimentalUsageError]
        bar.baz()
            ~~~
src/sample/UseJavaPackageFromKt.kt:54: Error: This declaration is experimental and its usage should be marked with
'@sample.foo.ExperimentalPackage' or '@UseExperimental(sample.foo.ExperimentalPackage.class)' [UnsafeExperimentalUsageError]
        callPackageExperimental()
        ~~~~~~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
    """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    /**
     * Loads a [TestFile] from Java source code included in the JAR resources.
     */
    private fun javaSample(className: String): TestFile {
        return java(javaClass.getResource("/java/${className.replace('.','/')}.java").readText())
    }

    /**
     * Loads a [TestFile] from Kotlin source code included in the JAR resources.
     */
    private fun ktSample(className: String): TestFile {
        return kotlin(javaClass.getResource("/java/${className.replace('.','/')}.kt").readText())
    }

    /* ktlint-disable max-line-length */
    companion object {
        /**
         * [TestFile] containing Experimental.java from the experimental annotation library.
         *
         * This is a workaround for IntelliJ failing to recognize source files if they are also
         * included as resources.
         */
        val EXPERIMENTAL_JAVA: TestFile = java("""
            package androidx.annotation.experimental;

            import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
            import static java.lang.annotation.RetentionPolicy.CLASS;

            import java.lang.annotation.Retention;
            import java.lang.annotation.Target;

            @Retention(CLASS)
            @Target({ANNOTATION_TYPE})
            public @interface Experimental {
                enum Level {
                    WARNING,
                    ERROR,
                }

                Level level() default Level.ERROR;
            }
        """.trimIndent())

        /**
         * [TestFile] containing UseExperimental.java from the experimental annotation library.
         *
         * This is a workaround for IntelliJ failing to recognize source files if they are also
         * included as resources.
         */
        val USE_EXPERIMENTAL_JAVA: TestFile = java("""
            package androidx.annotation.experimental;

            import static java.lang.annotation.ElementType.CONSTRUCTOR;
            import static java.lang.annotation.ElementType.FIELD;
            import static java.lang.annotation.ElementType.METHOD;
            import static java.lang.annotation.ElementType.PACKAGE;
            import static java.lang.annotation.ElementType.TYPE;
            import static java.lang.annotation.RetentionPolicy.CLASS;

            import java.lang.annotation.Retention;
            import java.lang.annotation.Target;

            @Retention(CLASS)
            @Target({TYPE, METHOD, CONSTRUCTOR, FIELD, PACKAGE})
            public @interface UseExperimental {
                Class<?> markerClass();
            }
        """.trimIndent())

        /**
         * [TestFile] containing Experimental.kt from the Kotlin standard library.
         *
         * This is a workaround for the Kotlin standard library used by the Lint test harness not
         * including the Experimental annotation by default.
         */
        val EXPERIMENTAL_KT: TestFile = kotlin("""
            package kotlin

            import kotlin.annotation.AnnotationRetention.BINARY
            import kotlin.annotation.AnnotationRetention.SOURCE
            import kotlin.annotation.AnnotationTarget.*
            import kotlin.internal.RequireKotlin
            import kotlin.internal.RequireKotlinVersionKind
            import kotlin.reflect.KClass

            @Target(ANNOTATION_CLASS)
            @Retention(BINARY)
            @SinceKotlin("1.2")
            @RequireKotlin("1.2.50", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
            @Suppress("ANNOTATION_CLASS_MEMBER")
            public annotation class Experimental(val level: Level = Level.ERROR) {
                public enum class Level {
                    WARNING,
                    ERROR,
                }
            }

            @Target(
                CLASS, PROPERTY, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, EXPRESSION, FILE, TYPEALIAS
            )
            @Retention(SOURCE)
            @SinceKotlin("1.2")
            @RequireKotlin("1.2.50", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
            public annotation class UseExperimental(
                vararg val markerClass: KClass<out Annotation>
            )

            @Target(CLASS, PROPERTY, CONSTRUCTOR, FUNCTION, TYPEALIAS)
            @Retention(BINARY)
            internal annotation class WasExperimental(
                vararg val markerClass: KClass<out Annotation>
            )
        """.trimIndent())

        /**
         * [TestFile] containing the package-level annotation for the sample.foo package.
         *
         * This is a workaround for b/136184987 where package-level annotations cannot be loaded
         * from source code. This is generated from a single-class JAR using toBase64gzip(File).
         */
        val SAMPLE_FOO_PACKAGE_INFO: TestFile = base64gzip("libs/sample.foo.package-info.jar", "" +
                "H4sIAAAAAAAAAAvwZmYRYWDg4GBgYFBkYGguSJ4HZB0EYlkGQYbixNyCnFT9" +
                "tPx8/YLE5OzE9FTdzLy0fL3knMTi4tAQXgZ2BiTg22vI1+Qg4pIe6Lh2y8VD" +
                "x7hfmJWFic2aMSPjCwv3zwlHn+o3tlaYcfM/WNijwRt8RuSz0ed/NvEMZhKs" +
                "HEw8rMfMo0UFZbcoNmauW7TbK2Op5bbXIVe9EgrX3njZ0xfzOqi9rezMNc3l" +
                "utOZTgev3HRnQs/aqb/d/VybNwU/u6SXy/pMdtFufYOfth6LLSY+N1h64iwD" +
                "I/9amy//7h1f4Lk/s+YBdwCKT3+yX33NA2QJgNwN9Kmva4ijrqefm35iXl5+" +
                "SWJJZn6ebmpFQWpRZm5qXklijm5OZl4J0PMlqelFENmS1OKS4vii1JzUxOJU" +
                "vez8EqCK+Nz8lNKcVIyQSU5ISEgD4pakOoYAb0YmEQZcYc7JgAoIxAArxB50" +
                "M1F9h2zmK5AIdf0KdwMrG4hmAsIbQLqIEcQDAGCtt2pgAgAA")
    }
    /* ktlint-enable max-line-length */
}
