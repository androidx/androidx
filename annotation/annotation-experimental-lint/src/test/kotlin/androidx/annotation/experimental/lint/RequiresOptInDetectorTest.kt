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
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RequiresOptInDetectorTest {

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
            javaSample("sample.optin.AnnotatedJavaMembers"),
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.UseJavaExperimentalMembersFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseJavaExperimentalMembersFromJava.java:30: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return stableObject.field;
                            ~~~~~
src/sample/optin/UseJavaExperimentalMembersFromJava.java:38: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return stableObject.method();
                            ~~~~~~
src/sample/optin/UseJavaExperimentalMembersFromJava.java:45: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return AnnotatedJavaMembers.FIELD_STATIC;
                                    ~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalMembersFromJava.java:52: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return AnnotatedJavaMembers.methodStatic();
                                    ~~~~~~~~~~~~
4 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaExperimentalClassFromJava() {
        val input = arrayOf(
            javaSample("sample.optin.AnnotatedJavaClass"),
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.UseJavaExperimentalClassFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseJavaExperimentalClassFromJava.java:31: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
                                                ~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalClassFromJava.java:32: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return experimentalObject.field;
                                  ~~~~~
src/sample/optin/UseJavaExperimentalClassFromJava.java:39: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
                                                ~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalClassFromJava.java:40: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return experimentalObject.method();
                                  ~~~~~~
src/sample/optin/UseJavaExperimentalClassFromJava.java:47: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return AnnotatedJavaClass.FIELD_STATIC;
                                  ~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalClassFromJava.java:54: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return AnnotatedJavaClass.methodStatic();
                                  ~~~~~~~~~~~~
6 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaExperimentalMultipleMarkersFromJava() {
        val input = arrayOf(
            javaSample("sample.optin.AnnotatedJavaClass"),
            javaSample("sample.optin.AnnotatedJavaClass2"),
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.ExperimentalJavaAnnotation2"),
            javaSample("sample.optin.UseJavaExperimentalMultipleMarkersFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseJavaExperimentalMultipleMarkersFromJava.java:33: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation2 or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation2.class) [UnsafeOptInUsageError]
        AnnotatedJavaClass2 experimentalObject2 = new AnnotatedJavaClass2();
                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalMultipleMarkersFromJava.java:34: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation2 or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation2.class) [UnsafeOptInUsageError]
        return experimentalObject.method() + experimentalObject2.field;
                                                                 ~~~~~
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaExperimentalFromKt() {
        val input = arrayOf(
            javaSample("sample.optin.AnnotatedJavaClass"),
            javaSample("sample.optin.AnnotatedJavaClass2"),
            javaSample("sample.optin.AnnotatedJavaMembers"),
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.ExperimentalJavaAnnotation2"),
            ktSample("sample.optin.UseJavaExperimentalFromKt")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseJavaExperimentalFromKt.kt:28: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        val experimentalObject = AnnotatedJavaClass()
                                 ~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:29: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return experimentalObject.field
                                  ~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:36: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        val experimentalObject = AnnotatedJavaClass()
                                 ~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:37: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return experimentalObject.method()
                                  ~~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:44: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return AnnotatedJavaClass.FIELD_STATIC
                                  ~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:51: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return AnnotatedJavaClass.methodStatic()
                                  ~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:77: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return stableObject.field
                            ~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:85: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return stableObject.method()
                            ~~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:92: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return AnnotatedJavaMembers.FIELD_STATIC
                                    ~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:99: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return AnnotatedJavaMembers.methodStatic()
                                    ~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:108: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation2 or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation2.class) [UnsafeOptInUsageError]
        return experimentalObject.method() + AnnotatedJavaClass2.FIELD_STATIC
                                                                 ~~~~~~~~~~~~
11 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useKtExperimentalFromJava() {
        val input = arrayOf(
            ktSample("sample.optin.AnnotatedKotlinClass"),
            ktSample("sample.optin.AnnotatedKotlinClass2"),
            ktSample("sample.optin.AnnotatedKotlinMembers"),
            ktSample("sample.optin.ExperimentalKotlinAnnotation"),
            ktSample("sample.optin.ExperimentalKotlinAnnotation2"),
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.UseKtExperimentalFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseKtExperimentalFromJava.java:28: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.optin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedKotlinClass experimentalObject = new AnnotatedKotlinClass();
                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/UseKtExperimentalFromJava.java:29: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.optin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        return experimentalObject.method();
                                  ~~~~~~
src/sample/optin/UseKtExperimentalFromJava.java:56: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalKotlinAnnotation2 or @OptIn(markerClass = sample.optin.ExperimentalKotlinAnnotation2.class) [UnsafeOptInUsageError]
        return experimentalObject.method() + AnnotatedKotlinClass2.fieldStatic;
                                                                   ~~~~~~~~~~~
src/sample/optin/UseKtExperimentalFromJava.java:97: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.optin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedKotlinMembers.methodStatic();
                               ~~~~~~~~~~~~
src/sample/optin/UseKtExperimentalFromJava.java:98: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.optin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedKotlinMembers.Companion.methodStatic();
                                         ~~~~~~~~~~~~
src/sample/optin/UseKtExperimentalFromJava.java:107: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.optin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        new AnnotatedKotlinMembers().method();
                                     ~~~~~~
src/sample/optin/UseKtExperimentalFromJava.java:108: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        new AnnotatedKotlinMembers().methodWithJavaMarker();
                                     ~~~~~~~~~~~~~~~~~~~~
7 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaPackageFromJava() {
        val input = arrayOf(
            SAMPLE_FOO_PACKAGE_INFO,
            javaSample("sample.optin.foo.AnnotatedJavaPackage"),
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.UseJavaPackageFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseJavaPackageFromJava.java:33: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaPackage experimentalObject = new AnnotatedJavaPackage();
                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaPackageFromJava.java:34: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        experimentalObject.method();
                           ~~~~~~
src/sample/optin/UseJavaPackageFromJava.java:67: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        safePropagateMarker();
        ~~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaPackageFromKt() {
        val input = arrayOf(
            SAMPLE_FOO_PACKAGE_INFO,
            javaSample("sample.optin.foo.AnnotatedJavaPackage"),
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            ktSample("sample.optin.UseJavaPackageFromKt")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseJavaPackageFromKt.kt:30: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        val experimentalObject = AnnotatedJavaPackage()
                                 ~~~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaPackageFromKt.kt:31: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        experimentalObject.method()
                           ~~~~~~
src/sample/optin/UseJavaPackageFromKt.kt:64: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        callPackageExperimental()
        ~~~~~~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun regressionTestJava193110413() {
        val input = arrayOf(
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.RegressionTestJava193110413"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/RegressionTestJava193110413.java:92: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        foo.defaultExperimentalMethod(); // unsafe in Java but safe in Kotlin
            ~~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/RegressionTestJava193110413.java:93: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        foo.experimentalMethod(); // unsafe
            ~~~~~~~~~~~~~~~~~~
src/sample/optin/RegressionTestJava193110413.java:95: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        Bar bar = new Bar(); // unsafe
                  ~~~~~~~~~
src/sample/optin/RegressionTestJava193110413.java:96: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        bar.stableMethodLevelOptIn(); // unsafe due to experimental class scope
            ~~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/RegressionTestJava193110413.java:97: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        bar.experimentalMethod(); // unsafe
            ~~~~~~~~~~~~~~~~~~
5 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun regressionTestJava192562469() {
        val input = arrayOf(
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.RegressionTestJava192562469"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/RegressionTestJava192562469.java:34: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
    static class ConcreteExperimentalInterface implements ExperimentalInterface { // unsafe
                                                          ~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/RegressionTestJava192562469.java:36: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        public void experimentalMethod() {} // unsafe override
                    ~~~~~~~~~~~~~~~~~~
src/sample/optin/RegressionTestJava192562469.java:62: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        ExperimentalInterface anonymous = new ExperimentalInterface() { // unsafe
                                              ~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/RegressionTestJava192562469.java:64: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
            public void experimentalMethod() {} // unsafe override
                        ~~~~~~~~~~~~~~~~~~
src/sample/optin/RegressionTestJava192562469.java:67: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        ExperimentalInterface lambda = () -> {}; // unsafe
                                       ~~~~~~~~
5 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun regressionTestJava192562926() {
        val input = arrayOf(
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.RegressionTestJava192562926"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/RegressionTestJava192562926.java:39: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        public void experimentalMethod() {} // unsafe override
                    ~~~~~~~~~~~~~~~~~~
src/sample/optin/RegressionTestJava192562926.java:49: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
            public void experimentalMethod() {} // unsafe override
                        ~~~~~~~~~~~~~~~~~~
src/sample/optin/RegressionTestJava192562926.java:52: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
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
            package androidx.annotation;

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
            package androidx.annotation;

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
         * [TestFile] containing the package-level annotation for the sample.optin.foo package.
         *
         * This is a workaround for b/136184987 where package-level annotations cannot be loaded
         * from source code. This is generated from a single-class JAR using toBase64gzip(File).
         *
         * To re-generate this:
         * 1. ./gradlew :annotation:annotation-experimental-lint-integration-tests:assemble
         * 2. mkdir -p temp/sample/optin/foo/
         * 3. cp ../../out/androidx/annotation/annotation-experimental-lint-integration-tests/build/intermediates/javac/debug/classes/sample/optin/foo/package-info.class temp/sample/optin/foo/
         * 4. jar -c -f sample.optin.foo.package-info.jar -C temp . | openssl base64 < sample.optin.foo.package-info.jar | tr -d '\n' | pbcopy
         * 5. Paste below
         * 6. rm -rf temp sample.optin.foo.package-info.jar
         */
        val SAMPLE_FOO_PACKAGE_INFO: TestFile = base64gzip(
            "libs/sample.optin.foo.package-info.jar",
            "UEsDBBQACAgIABRYjVIAAAAAAAAAAAAAAAAJAAQATUVUQS1JTkYv/soAAAMAUEsHCAAAAAACAAAAAAAAAFBLAwQUAAgICAAUWI1SAAAAAAAAAAAAAAAAFAAAAE1FVEEtSU5GL01BTklGRVNULk1G803My0xLLS7RDUstKs7Mz7NSMNQz4OVyLkpNLElN0XWqBAoARfRMFDT8ixKTc1IVnPOLCvKLEkuAijV5uXi5AFBLBwiVBramQAAAAEIAAABQSwMECgAACAAAOVeNUgAAAAAAAAAAAAAAAAcAAABzYW1wbGUvUEsDBAoAAAgAADlXjVIAAAAAAAAAAAAAAAANAAAAc2FtcGxlL29wdGluL1BLAwQKAAAIAAA7V41SAAAAAAAAAAAAAAAAEQAAAHNhbXBsZS9vcHRpbi9mb28vUEsDBBQACAgIADtXjVIAAAAAAAAAAAAAAAAjAAAAc2FtcGxlL29wdGluL2Zvby9wYWNrYWdlLWluZm8uY2xhc3NVjcEOwUAYhGeLFicuLuIBHNiLm5MDCZFIeIJts222tv9u2m3j2Rw8gIcSiwPmMHOYbzL3x/UGYIFehChCl6F/MnWZyI3SkmFoRXIWmZwpSs08F41gGB9rcqqQW2pUpWItV0TGCacMVQzTfSUKqyU31ini64uVpYfJCb3z8y+7ZJj8oakx/PeOYfA65FpQxg9xLhM3AhgCfBSg9fY2Oj5D34TAE1BLBwjeUT3SpAAAANAAAABQSwECFAAUAAgICAAUWI1SAAAAAAIAAAAAAAAACQAEAAAAAAAAAAAAAAAAAAAATUVUQS1JTkYv/soAAFBLAQIUABQACAgIABRYjVKVBramQAAAAEIAAAAUAAAAAAAAAAAAAAAAAD0AAABNRVRBLUlORi9NQU5JRkVTVC5NRlBLAQIKAAoAAAgAADlXjVIAAAAAAAAAAAAAAAAHAAAAAAAAAAAAAAAAAL8AAABzYW1wbGUvUEsBAgoACgAACAAAOVeNUgAAAAAAAAAAAAAAAA0AAAAAAAAAAAAAAAAA5AAAAHNhbXBsZS9vcHRpbi9QSwECCgAKAAAIAAA7V41SAAAAAAAAAAAAAAAAEQAAAAAAAAAAAAAAAAAPAQAAc2FtcGxlL29wdGluL2Zvby9QSwECFAAUAAgICAA7V41S3lE90qQAAADQAAAAIwAAAAAAAAAAAAAAAAA+AQAAc2FtcGxlL29wdGluL2Zvby9wYWNrYWdlLWluZm8uY2xhc3NQSwUGAAAAAAYABgB9AQAAMwIAAAAA"
        )
    }
    /* ktlint-enable max-line-length */
}

/**
 * Loads a [TestFile] from Java source code included in the JAR resources.
 */
fun javaSample(className: String): TestFile {
    return java(
        RequiresOptInDetectorTest::class.java.getResource(
            "/java/${className.replace('.','/')}.java"
        )!!.readText()
    )
}

/**
 * Loads a [TestFile] from Kotlin source code included in the JAR resources.
 */
fun ktSample(className: String): TestFile {
    return kotlin(
        RequiresOptInDetectorTest::class.java.getResource(
            "/java/${className.replace('.','/')}.kt"
        )!!.readText()
    )
}