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
import com.android.tools.lint.checks.infrastructure.TestFiles.xml
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
                ANDROIDX_EXPERIMENTAL_KT,
                ANDROIDX_USE_EXPERIMENTAL_KT,
                *testFiles
            )
            .issues(*ExperimentalDetector.ISSUES.toTypedArray())
            .run()
    }

    @Test
    fun useJavaExperimentalFromJava() {
        val input = arrayOf(
            javaSample("sample.experimental.DateProvider"),
            javaSample("sample.experimental.ExperimentalDateTime"),
            javaSample("sample.experimental.ExperimentalLocation"),
            javaSample("sample.experimental.LocationProvider"),
            javaSample("sample.experimental.UseJavaExperimentalFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/experimental/UseJavaExperimentalFromJava.java:25: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalDateTime or @OptIn(markerClass = sample.experimental.ExperimentalDateTime.class) [UnsafeOptInUsageError]
        DateProvider dateProvider = new DateProvider();
                                    ~~~~~~~~~~~~~~~~~~
src/sample/experimental/UseJavaExperimentalFromJava.java:26: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalDateTime or @OptIn(markerClass = sample.experimental.ExperimentalDateTime.class) [UnsafeOptInUsageError]
        return dateProvider.getDate();
                            ~~~~~~~
src/sample/experimental/UseJavaExperimentalFromJava.java:53: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalLocation or @OptIn(markerClass = sample.experimental.ExperimentalLocation.class) [UnsafeOptInUsageError]
        LocationProvider locationProvider = new LocationProvider();
                                            ~~~~~~~~~~~~~~~~~~~~~~
src/sample/experimental/UseJavaExperimentalFromJava.java:54: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalLocation or @OptIn(markerClass = sample.experimental.ExperimentalLocation.class) [UnsafeOptInUsageError]
        return dateProvider.getDate() + locationProvider.getLocation();
                                                         ~~~~~~~~~~~
4 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaExperimentalFromJavaWithOptions() {
        val lintConfig = xml(
            "lint.xml",
            """
<lint>
    <issue id="UnsafeOptInUsageError">
        <option name="opt-in" value="sample.experimental.ExperimentalDateTime" />
    </issue>
</lint>
            """.trimIndent()
        )
        val input = arrayOf(
            javaSample("sample.experimental.DateProvider"),
            javaSample("sample.experimental.ExperimentalDateTime"),
            javaSample("sample.experimental.ExperimentalLocation"),
            javaSample("sample.experimental.LocationProvider"),
            javaSample("sample.experimental.UseJavaExperimentalFromJava"),
            lintConfig
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/experimental/UseJavaExperimentalFromJava.java:53: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalLocation or @OptIn(markerClass = sample.experimental.ExperimentalLocation.class) [UnsafeOptInUsageError]
        LocationProvider locationProvider = new LocationProvider();
                                            ~~~~~~~~~~~~~~~~~~~~~~
src/sample/experimental/UseJavaExperimentalFromJava.java:54: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalLocation or @OptIn(markerClass = sample.experimental.ExperimentalLocation.class) [UnsafeOptInUsageError]
        return dateProvider.getDate() + locationProvider.getLocation();
                                                         ~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun useJavaExperimentalFromKt() {
        val input = arrayOf(
            javaSample("sample.experimental.DateProvider"),
            javaSample("sample.experimental.ExperimentalDateTime"),
            javaSample("sample.experimental.ExperimentalLocation"),
            javaSample("sample.experimental.LocationProvider"),
            ktSample("sample.experimental.UseJavaExperimentalFromKt")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/experimental/UseJavaExperimentalFromKt.kt:29: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalDateTime or @OptIn(markerClass = sample.experimental.ExperimentalDateTime.class) [UnsafeOptInUsageError]
        val dateProvider = DateProvider()
                           ~~~~~~~~~~~~
src/sample/experimental/UseJavaExperimentalFromKt.kt:30: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalDateTime or @OptIn(markerClass = sample.experimental.ExperimentalDateTime.class) [UnsafeOptInUsageError]
        return dateProvider.date
                            ~~~~
src/sample/experimental/UseJavaExperimentalFromKt.kt:57: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalLocation or @OptIn(markerClass = sample.experimental.ExperimentalLocation.class) [UnsafeOptInUsageError]
        val locationProvider = LocationProvider()
                               ~~~~~~~~~~~~~~~~
src/sample/experimental/UseJavaExperimentalFromKt.kt:58: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalLocation or @OptIn(markerClass = sample.experimental.ExperimentalLocation.class) [UnsafeOptInUsageError]
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
            ktSample("sample.experimental.DateProviderKt"),
            ktSample("sample.experimental.TimeProviderKt"),
            ktSample("sample.experimental.ExperimentalDateTimeKt"),
            ktSample("sample.experimental.ExperimentalLocationKt"),
            ktSample("sample.experimental.LocationProviderKt"),
            javaSample("sample.experimental.ExperimentalDateTime"),
            javaSample("sample.experimental.UseKtExperimentalFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/experimental/UseKtExperimentalFromJava.java:25: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalDateTimeKt or @OptIn(markerClass = sample.experimental.ExperimentalDateTimeKt.class) [UnsafeOptInUsageError]
        sample.experimental.DateProviderKt dateProvider = new sample.experimental.DateProviderKt();
                                                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/experimental/UseKtExperimentalFromJava.java:26: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalDateTimeKt or @OptIn(markerClass = sample.experimental.ExperimentalDateTimeKt.class) [UnsafeOptInUsageError]
        return dateProvider.getDate();
                            ~~~~~~~
src/sample/experimental/UseKtExperimentalFromJava.java:54: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalLocationKt or @OptIn(markerClass = sample.experimental.ExperimentalLocationKt.class) [UnsafeOptInUsageError]
        LocationProviderKt locationProvider = new LocationProviderKt();
                                              ~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/experimental/UseKtExperimentalFromJava.java:55: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalLocationKt or @OptIn(markerClass = sample.experimental.ExperimentalLocationKt.class) [UnsafeOptInUsageError]
        return dateProvider.getDate() + locationProvider.getLocation();
                                                         ~~~~~~~~~~~
src/sample/experimental/UseKtExperimentalFromJava.java:88: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalDateTimeKt or @OptIn(markerClass = sample.experimental.ExperimentalDateTimeKt.class) [UnsafeOptInUsageError]
        TimeProviderKt.getTimeStatically();
                       ~~~~~~~~~~~~~~~~~
src/sample/experimental/UseKtExperimentalFromJava.java:89: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalDateTimeKt or @OptIn(markerClass = sample.experimental.ExperimentalDateTimeKt.class) [UnsafeOptInUsageError]
        TimeProviderKt.Companion.getTimeStatically();
                                 ~~~~~~~~~~~~~~~~~
src/sample/experimental/UseKtExperimentalFromJava.java:96: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalDateTimeKt or @OptIn(markerClass = sample.experimental.ExperimentalDateTimeKt.class) [UnsafeOptInUsageError]
        new TimeProviderKt().getTime();
                             ~~~~~~~
src/sample/experimental/UseKtExperimentalFromJava.java:97: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.ExperimentalDateTime or @OptIn(markerClass = sample.experimental.ExperimentalDateTime.class) [UnsafeOptInUsageError]
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
            javaSample("sample.experimental.foo.Bar"),
            javaSample("sample.experimental.foo.ExperimentalPackage"),
            javaSample("sample.experimental.UseJavaPackageFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/experimental/UseJavaPackageFromJava.java:28: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.foo.ExperimentalPackage or @OptIn(markerClass = sample.experimental.foo.ExperimentalPackage.class) [UnsafeOptInUsageError]
        Bar bar = new Bar();
                  ~~~~~~~~~
src/sample/experimental/UseJavaPackageFromJava.java:29: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.foo.ExperimentalPackage or @OptIn(markerClass = sample.experimental.foo.ExperimentalPackage.class) [UnsafeOptInUsageError]
        bar.baz();
            ~~~
src/sample/experimental/UseJavaPackageFromJava.java:52: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.foo.ExperimentalPackage or @OptIn(markerClass = sample.experimental.foo.ExperimentalPackage.class) [UnsafeOptInUsageError]
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
            javaSample("sample.experimental.foo.Bar"),
            javaSample("sample.experimental.foo.ExperimentalPackage"),
            ktSample("sample.experimental.UseJavaPackageFromKt")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/experimental/UseJavaPackageFromKt.kt:32: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.foo.ExperimentalPackage or @OptIn(markerClass = sample.experimental.foo.ExperimentalPackage.class) [UnsafeOptInUsageError]
        val bar = Bar()
                  ~~~
src/sample/experimental/UseJavaPackageFromKt.kt:33: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.foo.ExperimentalPackage or @OptIn(markerClass = sample.experimental.foo.ExperimentalPackage.class) [UnsafeOptInUsageError]
        bar.baz()
            ~~~
src/sample/experimental/UseJavaPackageFromKt.kt:56: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.foo.ExperimentalPackage or @OptIn(markerClass = sample.experimental.foo.ExperimentalPackage.class) [UnsafeOptInUsageError]
        callPackageExperimental()
        ~~~~~~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    /* ktlint-disable max-line-length */
    companion object {
        /**
         * [TestFile] containing Experimental.kt from the experimental annotation library.
         *
         * This is a workaround for IntelliJ failing to recognize source files if they are also
         * included as resources.
         */
        val ANDROIDX_EXPERIMENTAL_KT: TestFile = kotlin(
            """
            package androidx.annotation.experimental;

            import kotlin.annotation.Retention
            import kotlin.annotation.Target

            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.ANNOTATION_CLASS)
            annotation class Experimental(
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
         * [TestFile] containing UseExperimental.kt from the experimental annotation library.
         *
         * This is a workaround for IntelliJ failing to recognize source files if they are also
         * included as resources.
         */
        val ANDROIDX_USE_EXPERIMENTAL_KT: TestFile = kotlin(
            """
            package androidx.annotation.experimental;

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
            annotation class UseExperimental(
                vararg val markerClass: KClass<out Annotation>
            )
            """.trimIndent()
        )

        /**
         * [TestFile] containing the package-level annotation for the sample.experimental.foo package.
         *
         * This is a workaround for b/136184987 where package-level annotations cannot be loaded
         * from source code. This is generated from a single-class JAR using toBase64gzip(File).
         *
         * To re-generate this:
         * 1. ./gradlew :annotation:annotation-experimental-lint-integration-tests:assemble
         * 2. mkdir -p temp/sample/experimental/foo/
         * 3. cp ../../out/androidx/annotation/annotation-experimental-lint-integration-tests/build/intermediates/javac/debug/classes/sample/experimental/foo/package-info.class temp/sample/experimental/foo/
         * 4. jar -c -f sample.experimental.foo.package-info.jar -C temp . | openssl base64 < sample.experimental.foo.package-info.jar | tr -d \n | pbcopy
         * 5. rm -rf temp sample.experimental.foo.package-info.jar
         * 6. Paste here
         */
        val SAMPLE_FOO_PACKAGE_INFO: TestFile = base64gzip(
            "libs/sample.experimental.foo.package-info.jar",
            "UEsDBBQACAgIAGhi/VAAAAAAAAAAAAAAAAAJAAQATUVUQS1JTkYv/soAAAMAUEsHCAAAAAACAAAAAAAAAFBLAwQUAAgICABoYv1QAAAAAAAAAAAAAAAAFAAAAE1FVEEtSU5GL01BTklGRVNULk1G803My0xLLS7RDUstKs7Mz7NSMNQz4OVyLkpNLElN0XWqBAoARfRMFDT8ixKTc1IVnPOLCvKLEkuAijV5uXi5AFBLBwiVBramQAAAAEIAAABQSwMECgAACAAAE2L9UAAAAAAAAAAAAAAAAAcAAABzYW1wbGUvUEsDBAoAAAgAABNi/VAAAAAAAAAAAAAAAAAUAAAAc2FtcGxlL2V4cGVyaW1lbnRhbC9QSwMECgAACAAAGWL9UAAAAAAAAAAAAAAAABgAAABzYW1wbGUvZXhwZXJpbWVudGFsL2Zvby9QSwMEFAAICAgAGWL9UAAAAAAAAAAAAAAAACoAAABzYW1wbGUvZXhwZXJpbWVudGFsL2Zvby9wYWNrYWdlLWluZm8uY2xhc3N1Tb0OgkAY6/kD6qSLi6sr3uLm5KCJiYlGn+AgH+Tw+I7AQXw2Bx/AhzKiLCx2aJO2aV/vxxPAGmMfvo+RwORqqyKivTYkMMtVdFMJBZpju0pVrQQWl4qdzujAtS51aGjLbJ1y2nIpEBxLleWGJN1zKpoaO2VkbK3cdYxzO7sRWP6rd58Fpt9vaRQn8hSmFLk5INBDix76Px5g2KjXJB7wAVBLBwjUtjrHoQAAANsAAABQSwECFAAUAAgICABoYv1QAAAAAAIAAAAAAAAACQAEAAAAAAAAAAAAAAAAAAAATUVUQS1JTkYv/soAAFBLAQIUABQACAgIAGhi/VCVBramQAAAAEIAAAAUAAAAAAAAAAAAAAAAAD0AAABNRVRBLUlORi9NQU5JRkVTVC5NRlBLAQIKAAoAAAgAABNi/VAAAAAAAAAAAAAAAAAHAAAAAAAAAAAAAAAAAL8AAABzYW1wbGUvUEsBAgoACgAACAAAE2L9UAAAAAAAAAAAAAAAABQAAAAAAAAAAAAAAAAA5AAAAHNhbXBsZS9leHBlcmltZW50YWwvUEsBAgoACgAACAAAGWL9UAAAAAAAAAAAAAAAABgAAAAAAAAAAAAAAAAAFgEAAHNhbXBsZS9leHBlcmltZW50YWwvZm9vL1BLAQIUABQACAgIABli/VDUtjrHoQAAANsAAAAqAAAAAAAAAAAAAAAAAEwBAABzYW1wbGUvZXhwZXJpbWVudGFsL2Zvby9wYWNrYWdlLWluZm8uY2xhc3NQSwUGAAAAAAYABgCSAQAARQIAAAAA"
        )
    }
    /* ktlint-enable max-line-length */
}
