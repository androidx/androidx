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

@file:Suppress("UnstableApiUsage")

package androidx.annotation.experimental.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.base64gzip
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Tests for usage of Kotlin opt-in annotations from Java sources.
 */
@RunWith(JUnit4::class)
class KotlinAnnotationsDetectorTest {

    private fun check(vararg testFiles: TestFile): TestLintResult {
        return lint()
            .files(
                ANDROIDX_REQUIRES_OPT_IN_KT,
                ANDROIDX_OPT_IN_KT,
                *testFiles
            )
            .issues(*ExperimentalDetector.ISSUES.toTypedArray())
            .testModes(TestMode.PARTIAL)
            .run()
    }

    @Test
    fun useJavaExperimentalMembersFromJava() {
        val input = arrayOf(
            javaSample("sample.kotlin.AnnotatedJavaMembers"),
            javaSample("sample.kotlin.ExperimentalJavaAnnotation"),
            javaSample("sample.kotlin.UseJavaExperimentalMembersFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/kotlin/UseJavaExperimentalMembersFromJava.java:30: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return stableObject.field;
                            ~~~~~
src/sample/kotlin/UseJavaExperimentalMembersFromJava.java:38: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return stableObject.method();
                            ~~~~~~
src/sample/kotlin/UseJavaExperimentalMembersFromJava.java:45: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return AnnotatedJavaMembers.FIELD_STATIC;
                                    ~~~~~~~~~~~~
src/sample/kotlin/UseJavaExperimentalMembersFromJava.java:52: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return AnnotatedJavaMembers.methodStatic();
                                    ~~~~~~~~~~~~
src/sample/kotlin/UseJavaExperimentalMembersFromJava.java:59: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        new AnnotatedJavaMembers().field = -1;
                                   ~~~~~
src/sample/kotlin/UseJavaExperimentalMembersFromJava.java:60: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        int value = new AnnotatedJavaMembers().field;
                                               ~~~~~
src/sample/kotlin/UseJavaExperimentalMembersFromJava.java:61: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        new AnnotatedJavaMembers().setFieldWithSetMarker(-1);
                                   ~~~~~~~~~~~~~~~~~~~~~
7 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaExperimentalClassFromJava() {
        val input = arrayOf(
            javaSample("sample.kotlin.AnnotatedJavaClass"),
            javaSample("sample.kotlin.ExperimentalJavaAnnotation"),
            javaSample("sample.kotlin.UseJavaExperimentalClassFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/kotlin/UseJavaExperimentalClassFromJava.java:31: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
        ~~~~~~~~~~~~~~~~~~
src/sample/kotlin/UseJavaExperimentalClassFromJava.java:31: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
                                                ~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/UseJavaExperimentalClassFromJava.java:32: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return experimentalObject.field;
                                  ~~~~~
src/sample/kotlin/UseJavaExperimentalClassFromJava.java:39: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
        ~~~~~~~~~~~~~~~~~~
src/sample/kotlin/UseJavaExperimentalClassFromJava.java:39: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
                                                ~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/UseJavaExperimentalClassFromJava.java:40: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return experimentalObject.method();
                                  ~~~~~~
src/sample/kotlin/UseJavaExperimentalClassFromJava.java:47: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return AnnotatedJavaClass.FIELD_STATIC;
                                  ~~~~~~~~~~~~
src/sample/kotlin/UseJavaExperimentalClassFromJava.java:54: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return AnnotatedJavaClass.methodStatic();
                                  ~~~~~~~~~~~~
8 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaExperimentalMultipleMarkersFromJava() {
        val input = arrayOf(
            javaSample("sample.kotlin.AnnotatedJavaClass"),
            javaSample("sample.kotlin.AnnotatedJavaClass2"),
            javaSample("sample.kotlin.ExperimentalJavaAnnotation"),
            javaSample("sample.kotlin.ExperimentalJavaAnnotation2"),
            javaSample("sample.kotlin.UseJavaExperimentalMultipleMarkersFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/kotlin/UseJavaExperimentalMultipleMarkersFromJava.java:33: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation2 or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation2.class) [UnsafeOptInUsageError]
        AnnotatedJavaClass2 experimentalObject2 = new AnnotatedJavaClass2();
        ~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/UseJavaExperimentalMultipleMarkersFromJava.java:33: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation2 or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation2.class) [UnsafeOptInUsageError]
        AnnotatedJavaClass2 experimentalObject2 = new AnnotatedJavaClass2();
                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/UseJavaExperimentalMultipleMarkersFromJava.java:34: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation2 or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation2.class) [UnsafeOptInUsageError]
        return experimentalObject.method() + experimentalObject2.field;
                                                                 ~~~~~
3 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useKtExperimentalFromJava() {
        val input = arrayOf(
            ktSample("sample.kotlin.AnnotatedKotlinClass"),
            ktSample("sample.kotlin.AnnotatedKotlinClass2"),
            ktSample("sample.kotlin.AnnotatedKotlinMembers"),
            ktSample("sample.kotlin.ExperimentalKotlinAnnotation"),
            ktSample("sample.kotlin.ExperimentalKotlinAnnotation2"),
            javaSample("sample.kotlin.ExperimentalJavaAnnotation"),
            javaSample("sample.kotlin.UseKtExperimentalFromJava")
        )

        // TODO(b/210881073): Access to annotated property `field` is still not detected.
        /* ktlint-disable max-line-length */
        val expected = """
src/sample/kotlin/UseKtExperimentalFromJava.java:28: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedKotlinClass experimentalObject = new AnnotatedKotlinClass();
        ~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/UseKtExperimentalFromJava.java:28: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedKotlinClass experimentalObject = new AnnotatedKotlinClass();
                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/UseKtExperimentalFromJava.java:29: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        return experimentalObject.method();
                                  ~~~~~~
src/sample/kotlin/UseKtExperimentalFromJava.java:56: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalKotlinAnnotation2 or @OptIn(markerClass = sample.kotlin.ExperimentalKotlinAnnotation2.class) [UnsafeOptInUsageError]
        return experimentalObject.method() + AnnotatedKotlinClass2.fieldStatic;
                                                                   ~~~~~~~~~~~
src/sample/kotlin/UseKtExperimentalFromJava.java:97: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedKotlinMembers.methodStatic();
                               ~~~~~~~~~~~~
src/sample/kotlin/UseKtExperimentalFromJava.java:98: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedKotlinMembers.Companion.methodStatic();
                                         ~~~~~~~~~~~~
src/sample/kotlin/UseKtExperimentalFromJava.java:107: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        new AnnotatedKotlinMembers().method();
                                     ~~~~~~
src/sample/kotlin/UseKtExperimentalFromJava.java:108: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        new AnnotatedKotlinMembers().methodWithJavaMarker();
                                     ~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/UseKtExperimentalFromJava.java:115: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        new AnnotatedKotlinMembers().setField(-1);
                                     ~~~~~~~~
src/sample/kotlin/UseKtExperimentalFromJava.java:116: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        int value = new AnnotatedKotlinMembers().getField();
                                                 ~~~~~~~~
src/sample/kotlin/UseKtExperimentalFromJava.java:117: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        new AnnotatedKotlinMembers().setFieldWithSetMarker(-1);
                                     ~~~~~~~~~~~~~~~~~~~~~
11 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaPackageFromJava() {
        val input = arrayOf(
            SAMPLE_FOO_PACKAGE_INFO,
            javaSample("sample.kotlin.foo.AnnotatedJavaPackage"),
            javaSample("sample.kotlin.ExperimentalJavaAnnotation"),
            javaSample("sample.kotlin.UseJavaPackageFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/kotlin/UseJavaPackageFromJava.java:32: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaPackage experimentalObject = new AnnotatedJavaPackage();
        ~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/UseJavaPackageFromJava.java:32: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaPackage experimentalObject = new AnnotatedJavaPackage();
                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/UseJavaPackageFromJava.java:33: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        experimentalObject.method();
                           ~~~~~~
src/sample/kotlin/UseJavaPackageFromJava.java:66: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        safePropagateMarker();
        ~~~~~~~~~~~~~~~~~~~
4 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    /**
     * Regression test for b/218798815 where the lint check yields false positives on usages within
     * an annotated package.
     */
    @Test
    fun regressionTestJava218798815() {
        val input = arrayOf(
            SAMPLE_FOO_PACKAGE_INFO,
            javaSample("sample.kotlin.foo.AnnotatedJavaPackage"),
            javaSample("sample.kotlin.ExperimentalJavaAnnotation"),
            javaSample("sample.kotlin.foo.RegressionTestJava218798815")
        )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun regressionTestJava193110413() {
        val input = arrayOf(
            javaSample("sample.kotlin.ExperimentalJavaAnnotation"),
            javaSample("sample.kotlin.RegressionTestJava193110413"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/kotlin/RegressionTestJava193110413.java:92: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        foo.defaultExperimentalMethod(); // unsafe in Java but safe in Kotlin
            ~~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/RegressionTestJava193110413.java:93: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        foo.experimentalMethod(); // unsafe
            ~~~~~~~~~~~~~~~~~~
src/sample/kotlin/RegressionTestJava193110413.java:95: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        Bar bar = new Bar(); // unsafe
        ~~~
src/sample/kotlin/RegressionTestJava193110413.java:95: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        Bar bar = new Bar(); // unsafe
                  ~~~~~~~~~
src/sample/kotlin/RegressionTestJava193110413.java:96: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        bar.stableMethodLevelOptIn(); // unsafe due to experimental class scope
            ~~~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/RegressionTestJava193110413.java:97: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        bar.experimentalMethod(); // unsafe
            ~~~~~~~~~~~~~~~~~~
6 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun regressionTestJava192562469() {
        val input = arrayOf(
            javaSample("sample.kotlin.ExperimentalJavaAnnotation"),
            javaSample("sample.kotlin.RegressionTestJava192562469"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/kotlin/RegressionTestJava192562469.java:34: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
    static class ConcreteExperimentalInterface implements ExperimentalInterface { // unsafe
                                                          ~~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/RegressionTestJava192562469.java:36: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        public void experimentalMethod() {} // unsafe override
                    ~~~~~~~~~~~~~~~~~~
src/sample/kotlin/RegressionTestJava192562469.java:62: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        ExperimentalInterface anonymous = new ExperimentalInterface() { // unsafe
        ~~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/RegressionTestJava192562469.java:62: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        ExperimentalInterface anonymous = new ExperimentalInterface() { // unsafe
                                              ~~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/RegressionTestJava192562469.java:64: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
            public void experimentalMethod() {} // unsafe override
                        ~~~~~~~~~~~~~~~~~~
src/sample/kotlin/RegressionTestJava192562469.java:67: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        ExperimentalInterface lambda = () -> {}; // unsafe
        ~~~~~~~~~~~~~~~~~~~~~
src/sample/kotlin/RegressionTestJava192562469.java:67: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        ExperimentalInterface lambda = () -> {}; // unsafe
                                       ~~~~~~~~
7 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun regressionTestJava192562926() {
        val input = arrayOf(
            javaSample("sample.kotlin.ExperimentalJavaAnnotation"),
            javaSample("sample.kotlin.RegressionTestJava192562926"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/kotlin/RegressionTestJava192562926.java:39: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        public void experimentalMethod() {} // unsafe override
                    ~~~~~~~~~~~~~~~~~~
src/sample/kotlin/RegressionTestJava192562926.java:49: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
            public void experimentalMethod() {} // unsafe override
                        ~~~~~~~~~~~~~~~~~~
src/sample/kotlin/RegressionTestJava192562926.java:52: Error: This declaration is opt-in and its usage should be marked with @sample.kotlin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.kotlin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        StableInterface lambda = () -> {}; // unsafe override
                                 ~~~~~~~~
3 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    /* ktlint-disable max-line-length */
    companion object {
        /**
         * [TestFile] containing RequiresOptIn.kt from the experimental annotation library.
         *
         * This is a workaround for IntelliJ failing to recognize source files if they are also
         * included as resources.
         */
        val ANDROIDX_REQUIRES_OPT_IN_KT: TestFile = kotlin(
            """
            package androidx.annotation

            import kotlin.annotation.Retention
            import kotlin.annotation.Target

            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.ANNOTATION_CLASS)
            annotation class RequiresOptIn(
                val level: Level = Level.ERROR
            ) {
                enum class Level {
                    WARNING,
                    ERROR
                }
            }
            """.trimIndent()
        )

        /**
         * [TestFile] containing OptIn.kt from the experimental annotation library.
         *
         * This is a workaround for IntelliJ failing to recognize source files if they are also
         * included as resources.
         */
        val ANDROIDX_OPT_IN_KT: TestFile = kotlin(
            """
            package androidx.annotation

            import kotlin.annotation.Retention
            import kotlin.annotation.Target
            import kotlin.reflect.KClass

            @Retention(AnnotationRetention.BINARY)
            @Target(
                AnnotationTarget.CLASS,
                AnnotationTarget.PROPERTY,
                AnnotationTarget.LOCAL_VARIABLE,
                AnnotationTarget.VALUE_PARAMETER,
                AnnotationTarget.CONSTRUCTOR,
                AnnotationTarget.FUNCTION,
                AnnotationTarget.PROPERTY_GETTER,
                AnnotationTarget.PROPERTY_SETTER,
                AnnotationTarget.FILE,
                AnnotationTarget.TYPEALIAS
            )
            annotation class OptIn(
                vararg val markerClass: KClass<out Annotation>
            )
            """.trimIndent()
        )

        /**
         * [TestFile] containing the package-level annotation for the sample.kotlin.foo package.
         *
         * This is a workaround for b/136184987 where package-level annotations cannot be loaded
         * from source code. This is generated from a single-class JAR using toBase64gzip(File).
         *
         * To re-generate this:
         * (if linux). alias pbcopy='xclip -selection clipboard'
         * 1. ./gradlew :annotation:annotation-experimental-lint-integration-tests:assemble
         * 2. mkdir -p temp/sample/kotlin/foo/
         * 3. cp ../../out/androidx/annotation/annotation-experimental-lint-integration-tests/build/intermediates/javac/debug/classes/sample/kotlin/foo/package-info.class temp/sample/kotlin/foo/
         * 4. jar -c -f sample.kotlin.foo.package-info.jar -C temp .; openssl base64 < sample.kotlin.foo.package-info.jar | tr -d '\n' | pbcopy
         * 5. Paste below
         * 6. rm -rf temp sample.kotlin.foo.package-info.jar
         */
        val SAMPLE_FOO_PACKAGE_INFO: TestFile = base64gzip(
            "libs/sample.kotlin.foo.package-info.jar",
            "H4sIAAAAAAAA/wvwZmYRYeDg4GCYlvw3hAEJcDKwMPi6hjjqevq56f87xcDA" +
                "zBDgzc4BkmKCKgnAqVkEiOGafR39PN1cg0P0fN0++5457eOtq3eR11tX69yZ" +
                "85uDDK4YP3hapOflq+Ppe7F0FQsXg+sXnl5RkzufP18uDhK1+Tzpq0nlzoqd" +
                "YMsn101ebw002gZqORcDA9BBE9AsZwfi4sTcgpxUfdyK+BCKsvNLcjLzkNRO" +
                "RlMrhKE2LT9fH+F7dPUqWNUXJCZnJ6an6mbmpeXrJeckFheH9tpyHXIQaUl/" +
                "7H/FS1r6IDPHrt65U1ublDiqmqZv4Jydc68tRdhmdiv7h4CkK05zk5bNyJ/x" +
                "+3EV8waX++3vF6sbWNxexXE1TFrV4JSmj2ZY8dmJsSohkw88Cdl4eeatwrXe" +
                "shJb07b1zdkWw3WGTzV1Z6DR1nMZ02Z7r+tqS3NPu/9m/wevhF/n3a6duu/c" +
                "+PB1kNSjCLlml9Y8Ho6KHyecX7/NLZn1xsxXvIIJFPDOfnrRy4C+ugQOeEYm" +
                "EQbUeIelCFCiQQUoSQhdK3J8iqBos8WRgEAmcDHgjncE2IWcCnBr4kPRdB9L" +
                "qkDoxZYuEICbEXsqQXgZpB85JFVQ9Ftj1Y+ZagK8WdlA6tmAsAGoxxgc+ACq" +
                "I6JIyQMAAA=="
        )
    }
    /* ktlint-enable max-line-length */
}
