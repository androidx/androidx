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

@Suppress("UnstableApiUsage")
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
            .run()
    }

    @Test
    fun useJavaExperimentalFromJava() {
        val input = arrayOf(
            javaSample("sample.optin.DateProvider"),
            javaSample("sample.optin.ExperimentalDateTime"),
            javaSample("sample.optin.ExperimentalLocation"),
            javaSample("sample.optin.LocationProvider"),
            javaSample("sample.optin.UseJavaExperimentalFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseJavaExperimentalFromJava.java:27: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.ExperimentalDateTime' or '@OptIn(markerClass = sample.optin.ExperimentalDateTime.class)' [UnsafeOptInUsageError]
        DateProvider dateProvider = new DateProvider();
                                    ~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalFromJava.java:28: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.ExperimentalDateTime' or '@OptIn(markerClass = sample.optin.ExperimentalDateTime.class)' [UnsafeOptInUsageError]
        return dateProvider.getDate();
                            ~~~~~~~
src/sample/optin/UseJavaExperimentalFromJava.java:55: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.ExperimentalLocation' or '@OptIn(markerClass = sample.optin.ExperimentalLocation.class)' [UnsafeOptInUsageError]
        LocationProvider locationProvider = new LocationProvider();
                                            ~~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalFromJava.java:56: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.ExperimentalLocation' or '@OptIn(markerClass = sample.optin.ExperimentalLocation.class)' [UnsafeOptInUsageError]
        return dateProvider.getDate() + locationProvider.getLocation();
                                                         ~~~~~~~~~~~
4 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaExperimentalFromKt() {
        val input = arrayOf(
            javaSample("sample.optin.DateProvider"),
            javaSample("sample.optin.ExperimentalDateTime"),
            javaSample("sample.optin.ExperimentalLocation"),
            javaSample("sample.optin.LocationProvider"),
            ktSample("sample.optin.UseJavaExperimentalFromKt")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseJavaExperimentalFromKt.kt:27: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.ExperimentalDateTime' or '@OptIn(markerClass = sample.optin.ExperimentalDateTime.class)' [UnsafeOptInUsageError]
        val dateProvider = DateProvider()
                           ~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:28: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.ExperimentalDateTime' or '@OptIn(markerClass = sample.optin.ExperimentalDateTime.class)' [UnsafeOptInUsageError]
        return dateProvider.date
                            ~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:55: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.ExperimentalLocation' or '@OptIn(markerClass = sample.optin.ExperimentalLocation.class)' [UnsafeOptInUsageError]
        val locationProvider = LocationProvider()
                               ~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:56: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.ExperimentalLocation' or '@OptIn(markerClass = sample.optin.ExperimentalLocation.class)' [UnsafeOptInUsageError]
        return dateProvider.date + locationProvider.location
                                                    ~~~~~~~~
4 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useKtExperimentalFromJava() {
        val input = arrayOf(
            OPT_IN_KT,
            ktSample("sample.optin.DateProviderKt"),
            ktSample("sample.optin.TimeProviderKt"),
            ktSample("sample.optin.ExperimentalDateTimeKt"),
            ktSample("sample.optin.ExperimentalLocationKt"),
            ktSample("sample.optin.LocationProviderKt"),
            javaSample("sample.optin.ExperimentalDateTime"),
            javaSample("sample.optin.UseKtExperimentalFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
 src/sample/optin/UseKtExperimentalFromJava.java:27: Error: This declaration is opt-in and its usage should be marked with
 '@sample.optin.ExperimentalDateTimeKt' or '@OptIn(markerClass = sample.optin.ExperimentalDateTimeKt.class)' [UnsafeOptInUsageError]
         DateProviderKt dateProvider = new DateProviderKt();
                                       ~~~~~~~~~~~~~~~~~~~~
 src/sample/optin/UseKtExperimentalFromJava.java:28: Error: This declaration is opt-in and its usage should be marked with
 '@sample.optin.ExperimentalDateTimeKt' or '@OptIn(markerClass = sample.optin.ExperimentalDateTimeKt.class)' [UnsafeOptInUsageError]
         return dateProvider.getDate();
                             ~~~~~~~
 src/sample/optin/UseKtExperimentalFromJava.java:55: Error: This declaration is opt-in and its usage should be marked with
 '@sample.optin.ExperimentalLocationKt' or '@OptIn(markerClass = sample.optin.ExperimentalLocationKt.class)' [UnsafeOptInUsageError]
         LocationProviderKt locationProvider = new LocationProviderKt();
                                               ~~~~~~~~~~~~~~~~~~~~~~~~
 src/sample/optin/UseKtExperimentalFromJava.java:56: Error: This declaration is opt-in and its usage should be marked with
 '@sample.optin.ExperimentalLocationKt' or '@OptIn(markerClass = sample.optin.ExperimentalLocationKt.class)' [UnsafeOptInUsageError]
         return dateProvider.getDate() + locationProvider.getLocation();
                                                          ~~~~~~~~~~~
 src/sample/optin/UseKtExperimentalFromJava.java:89: Error: This declaration is opt-in and its usage should be marked with
 '@sample.optin.ExperimentalDateTimeKt' or '@OptIn(markerClass = sample.optin.ExperimentalDateTimeKt.class)' [UnsafeOptInUsageError]
         TimeProviderKt.getTimeStatically();
                        ~~~~~~~~~~~~~~~~~
 src/sample/optin/UseKtExperimentalFromJava.java:90: Error: This declaration is opt-in and its usage should be marked with
 '@sample.optin.ExperimentalDateTimeKt' or '@OptIn(markerClass = sample.optin.ExperimentalDateTimeKt.class)' [UnsafeOptInUsageError]
         TimeProviderKt.Companion.getTimeStatically();
                                  ~~~~~~~~~~~~~~~~~
 src/sample/optin/UseKtExperimentalFromJava.java:97: Error: This declaration is opt-in and its usage should be marked with
 '@sample.optin.ExperimentalDateTimeKt' or '@OptIn(markerClass = sample.optin.ExperimentalDateTimeKt.class)' [UnsafeOptInUsageError]
         new TimeProviderKt().getTime();
                              ~~~~~~~
 src/sample/optin/UseKtExperimentalFromJava.java:98: Error: This declaration is opt-in and its usage should be marked with
 '@sample.optin.ExperimentalDateTime' or '@OptIn(markerClass = sample.optin.ExperimentalDateTime.class)' [UnsafeOptInUsageError]
         new TimeProviderKt().getTimeJava();
                              ~~~~~~~~~~~
 8 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaPackageFromJava() {
        val input = arrayOf(
            SAMPLE_FOO_PACKAGE_INFO,
            javaSample("sample.optin.foo.Bar"),
            javaSample("sample.optin.foo.ExperimentalPackage"),
            javaSample("sample.optin.UseJavaPackageFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseJavaPackageFromJava.java:30: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.foo.ExperimentalPackage' or '@OptIn(markerClass = sample.optin.foo.ExperimentalPackage.class)' [UnsafeOptInUsageError]
        Bar bar = new Bar();
                  ~~~~~~~~~
src/sample/optin/UseJavaPackageFromJava.java:31: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.foo.ExperimentalPackage' or '@OptIn(markerClass = sample.optin.foo.ExperimentalPackage.class)' [UnsafeOptInUsageError]
        bar.baz();
            ~~~
src/sample/optin/UseJavaPackageFromJava.java:54: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.foo.ExperimentalPackage' or '@OptIn(markerClass = sample.optin.foo.ExperimentalPackage.class)' [UnsafeOptInUsageError]
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
            javaSample("sample.optin.foo.Bar"),
            javaSample("sample.optin.foo.ExperimentalPackage"),
            ktSample("sample.optin.UseJavaPackageFromKt")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseJavaPackageFromKt.kt:30: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.foo.ExperimentalPackage' or '@OptIn(markerClass = sample.optin.foo.ExperimentalPackage.class)' [UnsafeOptInUsageError]
        val bar = Bar()
                  ~~~
src/sample/optin/UseJavaPackageFromKt.kt:31: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.foo.ExperimentalPackage' or '@OptIn(markerClass = sample.optin.foo.ExperimentalPackage.class)' [UnsafeOptInUsageError]
        bar.baz()
            ~~~
src/sample/optin/UseJavaPackageFromKt.kt:54: Error: This declaration is opt-in and its usage should be marked with
'@sample.optin.foo.ExperimentalPackage' or '@OptIn(markerClass = sample.optin.foo.ExperimentalPackage.class)' [UnsafeOptInUsageError]
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
         * [TestFile] containing OptIn.kt from the Kotlin standard library.
         *
         * This is a workaround for the Kotlin standard library used by the Lint test harness not
         * including the Experimental annotation by default.
         */
        val OPT_IN_KT: TestFile = kotlin(
            """
            package kotlin

            import kotlin.annotation.AnnotationRetention.BINARY
            import kotlin.annotation.AnnotationRetention.SOURCE
            import kotlin.annotation.AnnotationTarget.*
            import kotlin.internal.RequireKotlin
            import kotlin.internal.RequireKotlinVersionKind
            import kotlin.reflect.KClass

            @Target(ANNOTATION_CLASS)
            @Retention(BINARY)
            @SinceKotlin("1.3")
            @RequireKotlin("1.3.70", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
            public annotation class RequiresOptIn(
                val message: String = "",
                val level: Level = Level.ERROR
            ) {
                public enum class Level {
                    WARNING,
                    ERROR,
                }
            }

            @Target(
                CLASS, PROPERTY, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, EXPRESSION, FILE, TYPEALIAS
            )
            @Retention(SOURCE)
            @SinceKotlin("1.3")
            @RequireKotlin("1.3.70", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
            public annotation class OptIn(
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
         * 5. rm -rf temp sample.optin.foo.package-info.jar
         * 6. Paste here
         */
        val SAMPLE_FOO_PACKAGE_INFO: TestFile = base64gzip(
            "libs/sample.optin.foo.package-info.jar",
            "UEsDBBQACAgIAER1/VAAAAAAAAAAAAAAAAAJAAQATUVUQS1JTkYv/soAAAMAUEsHCAAAAAACAAAAAAAAAFBLAwQUAAgICABEdf1QAAAAAAAAAAAAAAAAFAAAAE1FVEEtSU5GL01BTklGRVNULk1G803My0xLLS7RDUstKs7Mz7NSMNQz4OVyLkpNLElN0XWqBAoARfRMFDT8ixKTc1IVnPOLCvKLEkuAijV5uXi5AFBLBwiVBramQAAAAEIAAABQSwMECgAACAAAOHX9UAAAAAAAAAAAAAAAAAcAAABzYW1wbGUvUEsDBAoAAAgAADh1/VAAAAAAAAAAAAAAAAANAAAAc2FtcGxlL29wdGluL1BLAwQKAAAIAABAdf1QAAAAAAAAAAAAAAAAEQAAAHNhbXBsZS9vcHRpbi9mb28vUEsDBBQACAgIAEB1/VAAAAAAAAAAAAAAAAAjAAAAc2FtcGxlL29wdGluL2Zvby9wYWNrYWdlLWluZm8uY2xhc3NlTb0OwVAYPbdoMbFYxGrjLjaTgUQiITzB1+a2uXX73aa9bTybwQN4KFG6SJzhnOH8PV/3B4AVBgGCAH2B4cVWRaR22iiBcU7RlRK10BzbZUo1CUzPFTudqT3XutShURtm68hpy6XA/FBSlhslbe40y9haub3lqmgK7Mic2r21wOwv9/slMPq8SUOcyGOYqshNAAEPLTx0vtxFr1G/cXzgDVBLBwiEWN3yoQAAAM0AAABQSwECFAAUAAgICABEdf1QAAAAAAIAAAAAAAAACQAEAAAAAAAAAAAAAAAAAAAATUVUQS1JTkYv/soAAFBLAQIUABQACAgIAER1/VCVBramQAAAAEIAAAAUAAAAAAAAAAAAAAAAAD0AAABNRVRBLUlORi9NQU5JRkVTVC5NRlBLAQIKAAoAAAgAADh1/VAAAAAAAAAAAAAAAAAHAAAAAAAAAAAAAAAAAL8AAABzYW1wbGUvUEsBAgoACgAACAAAOHX9UAAAAAAAAAAAAAAAAA0AAAAAAAAAAAAAAAAA5AAAAHNhbXBsZS9vcHRpbi9QSwECCgAKAAAIAABAdf1QAAAAAAAAAAAAAAAAEQAAAAAAAAAAAAAAAAAPAQAAc2FtcGxlL29wdGluL2Zvby9QSwECFAAUAAgICABAdf1QhFjd8qEAAADNAAAAIwAAAAAAAAAAAAAAAAA+AQAAc2FtcGxlL29wdGluL2Zvby9wYWNrYWdlLWluZm8uY2xhc3NQSwUGAAAAAAYABgB9AQAAMAIAAAAA"
        )
    }
    /* ktlint-enable max-line-length */
}
