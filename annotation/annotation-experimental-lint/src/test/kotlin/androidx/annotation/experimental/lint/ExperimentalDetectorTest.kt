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

        val expectedFix = """
Fix for src/sample/experimental/UseJavaExperimentalFromJava.java line 25: Add '@androidx.annotation.OptIn(markerClass = sample.experimental.ExperimentalDateTime.class)' annotation to 'getDateUnsafe':
@@ -24 +24
+     @androidx.annotation.OptIn(markerClass = ExperimentalDateTime.class)
Fix for src/sample/experimental/UseJavaExperimentalFromJava.java line 25: Add '@sample.experimental.ExperimentalDateTime' annotation to 'getDateUnsafe':
@@ -24 +24
+     @ExperimentalDateTime
Fix for src/sample/experimental/UseJavaExperimentalFromJava.java line 25: Add '@sample.experimental.ExperimentalDateTime' annotation to containing class 'UseJavaExperimentalFromJava':
@@ -19 +19
+ @ExperimentalDateTime
Fix for src/sample/experimental/UseJavaExperimentalFromJava.java line 26: Add '@androidx.annotation.OptIn(markerClass = sample.experimental.ExperimentalDateTime.class)' annotation to 'getDateUnsafe':
@@ -24 +24
+     @androidx.annotation.OptIn(markerClass = ExperimentalDateTime.class)
Fix for src/sample/experimental/UseJavaExperimentalFromJava.java line 26: Add '@sample.experimental.ExperimentalDateTime' annotation to 'getDateUnsafe':
@@ -24 +24
+     @ExperimentalDateTime
Fix for src/sample/experimental/UseJavaExperimentalFromJava.java line 26: Add '@sample.experimental.ExperimentalDateTime' annotation to containing class 'UseJavaExperimentalFromJava':
@@ -19 +19
+ @ExperimentalDateTime
Fix for src/sample/experimental/UseJavaExperimentalFromJava.java line 53: Add '@androidx.annotation.OptIn(markerClass = sample.experimental.ExperimentalLocation.class)' annotation to 'getDateExperimentalLocationUnsafe':
@@ -50 +50
+     @androidx.annotation.OptIn(markerClass = ExperimentalLocation.class)
Fix for src/sample/experimental/UseJavaExperimentalFromJava.java line 53: Add '@sample.experimental.ExperimentalLocation' annotation to 'getDateExperimentalLocationUnsafe':
@@ -50 +50
+     @ExperimentalLocation
Fix for src/sample/experimental/UseJavaExperimentalFromJava.java line 53: Add '@sample.experimental.ExperimentalLocation' annotation to containing class 'UseJavaExperimentalFromJava':
@@ -19 +19
+ @ExperimentalLocation
Fix for src/sample/experimental/UseJavaExperimentalFromJava.java line 54: Add '@androidx.annotation.OptIn(markerClass = sample.experimental.ExperimentalLocation.class)' annotation to 'getDateExperimentalLocationUnsafe':
@@ -50 +50
+     @androidx.annotation.OptIn(markerClass = ExperimentalLocation.class)
Fix for src/sample/experimental/UseJavaExperimentalFromJava.java line 54: Add '@sample.experimental.ExperimentalLocation' annotation to 'getDateExperimentalLocationUnsafe':
@@ -50 +50
+     @ExperimentalLocation
Fix for src/sample/experimental/UseJavaExperimentalFromJava.java line 54: Add '@sample.experimental.ExperimentalLocation' annotation to containing class 'UseJavaExperimentalFromJava':
@@ -19 +19
+ @ExperimentalLocation
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
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

        val expectedFix = """
Fix for src/sample/experimental/UseJavaExperimentalFromKt.kt line 29: Add '@androidx.annotation.OptIn(sample.experimental.ExperimentalDateTime::class)' annotation to 'getDateUnsafe':
@@ -25 +25
+     @androidx.annotation.OptIn(ExperimentalDateTime::class)
Fix for src/sample/experimental/UseJavaExperimentalFromKt.kt line 29: Add '@sample.experimental.ExperimentalDateTime' annotation to 'getDateUnsafe':
@@ -25 +25
+     @ExperimentalDateTime
Fix for src/sample/experimental/UseJavaExperimentalFromKt.kt line 29: Add '@sample.experimental.ExperimentalDateTime' annotation to containing class 'UseJavaExperimentalFromKt':
@@ -1 +1
+ @ExperimentalDateTime
Fix for src/sample/experimental/UseJavaExperimentalFromKt.kt line 30: Add '@androidx.annotation.OptIn(sample.experimental.ExperimentalDateTime::class)' annotation to 'getDateUnsafe':
@@ -25 +25
+     @androidx.annotation.OptIn(ExperimentalDateTime::class)
Fix for src/sample/experimental/UseJavaExperimentalFromKt.kt line 30: Add '@sample.experimental.ExperimentalDateTime' annotation to 'getDateUnsafe':
@@ -25 +25
+     @ExperimentalDateTime
Fix for src/sample/experimental/UseJavaExperimentalFromKt.kt line 30: Add '@sample.experimental.ExperimentalDateTime' annotation to containing class 'UseJavaExperimentalFromKt':
@@ -1 +1
+ @ExperimentalDateTime
Fix for src/sample/experimental/UseJavaExperimentalFromKt.kt line 57: Add '@androidx.annotation.OptIn(sample.experimental.ExperimentalLocation::class)' annotation to 'getDateExperimentalLocationUnsafe':
@@ -51 +51
+     @androidx.annotation.OptIn(ExperimentalLocation::class)
Fix for src/sample/experimental/UseJavaExperimentalFromKt.kt line 57: Add '@sample.experimental.ExperimentalLocation' annotation to 'getDateExperimentalLocationUnsafe':
@@ -51 +51
+     @ExperimentalLocation
Fix for src/sample/experimental/UseJavaExperimentalFromKt.kt line 57: Add '@sample.experimental.ExperimentalLocation' annotation to containing class 'UseJavaExperimentalFromKt':
@@ -1 +1
+ @ExperimentalLocation
Fix for src/sample/experimental/UseJavaExperimentalFromKt.kt line 58: Add '@androidx.annotation.OptIn(sample.experimental.ExperimentalLocation::class)' annotation to 'getDateExperimentalLocationUnsafe':
@@ -51 +51
+     @androidx.annotation.OptIn(ExperimentalLocation::class)
Fix for src/sample/experimental/UseJavaExperimentalFromKt.kt line 58: Add '@sample.experimental.ExperimentalLocation' annotation to 'getDateExperimentalLocationUnsafe':
@@ -51 +51
+     @ExperimentalLocation
Fix for src/sample/experimental/UseJavaExperimentalFromKt.kt line 58: Add '@sample.experimental.ExperimentalLocation' annotation to containing class 'UseJavaExperimentalFromKt':
@@ -1 +1
+ @ExperimentalLocation
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
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

        val expectedFix = """
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 25: Add '@androidx.annotation.OptIn(markerClass = sample.experimental.ExperimentalDateTimeKt.class)' annotation to 'getDateUnsafe':
@@ -24 +24
+     @androidx.annotation.OptIn(markerClass = ExperimentalDateTimeKt.class)
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 25: Add '@sample.experimental.ExperimentalDateTimeKt' annotation to 'getDateUnsafe':
@@ -24 +24
+     @ExperimentalDateTimeKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 25: Add '@sample.experimental.ExperimentalDateTimeKt' annotation to containing class 'UseKtExperimentalFromJava':
@@ -19 +19
+ @ExperimentalDateTimeKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 26: Add '@androidx.annotation.OptIn(markerClass = sample.experimental.ExperimentalDateTimeKt.class)' annotation to 'getDateUnsafe':
@@ -24 +24
+     @androidx.annotation.OptIn(markerClass = ExperimentalDateTimeKt.class)
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 26: Add '@sample.experimental.ExperimentalDateTimeKt' annotation to 'getDateUnsafe':
@@ -24 +24
+     @ExperimentalDateTimeKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 26: Add '@sample.experimental.ExperimentalDateTimeKt' annotation to containing class 'UseKtExperimentalFromJava':
@@ -19 +19
+ @ExperimentalDateTimeKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 54: Add '@androidx.annotation.OptIn(markerClass = sample.experimental.ExperimentalLocationKt.class)' annotation to 'getDateExperimentalLocationUnsafe':
@@ -51 +51
+     @androidx.annotation.OptIn(markerClass = ExperimentalLocationKt.class)
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 54: Add '@sample.experimental.ExperimentalLocationKt' annotation to 'getDateExperimentalLocationUnsafe':
@@ -51 +51
+     @ExperimentalLocationKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 54: Add '@sample.experimental.ExperimentalLocationKt' annotation to containing class 'UseKtExperimentalFromJava':
@@ -19 +19
+ @ExperimentalLocationKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 55: Add '@androidx.annotation.OptIn(markerClass = sample.experimental.ExperimentalLocationKt.class)' annotation to 'getDateExperimentalLocationUnsafe':
@@ -51 +51
+     @androidx.annotation.OptIn(markerClass = ExperimentalLocationKt.class)
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 55: Add '@sample.experimental.ExperimentalLocationKt' annotation to 'getDateExperimentalLocationUnsafe':
@@ -51 +51
+     @ExperimentalLocationKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 55: Add '@sample.experimental.ExperimentalLocationKt' annotation to containing class 'UseKtExperimentalFromJava':
@@ -19 +19
+ @ExperimentalLocationKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 88: Add '@androidx.annotation.OptIn(markerClass = sample.experimental.ExperimentalDateTimeKt.class)' annotation to 'regressionTestStaticUsage':
@@ -87 +87
+     @androidx.annotation.OptIn(markerClass = ExperimentalDateTimeKt.class)
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 88: Add '@sample.experimental.ExperimentalDateTimeKt' annotation to 'regressionTestStaticUsage':
@@ -87 +87
+     @ExperimentalDateTimeKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 88: Add '@sample.experimental.ExperimentalDateTimeKt' annotation to containing class 'UseKtExperimentalFromJava':
@@ -19 +19
+ @ExperimentalDateTimeKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 89: Add '@androidx.annotation.OptIn(markerClass = sample.experimental.ExperimentalDateTimeKt.class)' annotation to 'regressionTestStaticUsage':
@@ -87 +87
+     @androidx.annotation.OptIn(markerClass = ExperimentalDateTimeKt.class)
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 89: Add '@sample.experimental.ExperimentalDateTimeKt' annotation to 'regressionTestStaticUsage':
@@ -87 +87
+     @ExperimentalDateTimeKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 89: Add '@sample.experimental.ExperimentalDateTimeKt' annotation to containing class 'UseKtExperimentalFromJava':
@@ -19 +19
+ @ExperimentalDateTimeKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 96: Add '@androidx.annotation.OptIn(markerClass = sample.experimental.ExperimentalDateTimeKt.class)' annotation to 'regressionTestInlineUsage':
@@ -95 +95
+     @androidx.annotation.OptIn(markerClass = ExperimentalDateTimeKt.class)
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 96: Add '@sample.experimental.ExperimentalDateTimeKt' annotation to 'regressionTestInlineUsage':
@@ -95 +95
+     @ExperimentalDateTimeKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 96: Add '@sample.experimental.ExperimentalDateTimeKt' annotation to containing class 'UseKtExperimentalFromJava':
@@ -19 +19
+ @ExperimentalDateTimeKt
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 97: Add '@androidx.annotation.OptIn(markerClass = sample.experimental.ExperimentalDateTime.class)' annotation to 'regressionTestInlineUsage':
@@ -95 +95
+     @androidx.annotation.OptIn(markerClass = ExperimentalDateTime.class)
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 97: Add '@sample.experimental.ExperimentalDateTime' annotation to 'regressionTestInlineUsage':
@@ -95 +95
+     @ExperimentalDateTime
Fix for src/sample/experimental/UseKtExperimentalFromJava.java line 97: Add '@sample.experimental.ExperimentalDateTime' annotation to containing class 'UseKtExperimentalFromJava':
@@ -19 +19
+ @ExperimentalDateTime
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix).expect(expected)
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
src/sample/experimental/UseJavaPackageFromKt.kt:31: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.foo.ExperimentalPackage or @OptIn(markerClass = sample.experimental.foo.ExperimentalPackage.class) [UnsafeOptInUsageError]
        val bar = Bar()
                  ~~~
src/sample/experimental/UseJavaPackageFromKt.kt:32: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.foo.ExperimentalPackage or @OptIn(markerClass = sample.experimental.foo.ExperimentalPackage.class) [UnsafeOptInUsageError]
        bar.baz()
            ~~~
src/sample/experimental/UseJavaPackageFromKt.kt:55: Error: This declaration is opt-in and its usage should be marked with @sample.experimental.foo.ExperimentalPackage or @OptIn(markerClass = sample.experimental.foo.ExperimentalPackage.class) [UnsafeOptInUsageError]
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
            package androidx.annotation.experimental

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
            package androidx.annotation.experimental

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
                "H4sIAAAAAAAA/wvwZmYRYeDg4GDISPobwIAEOBlYGHxdQxx1Pf3c9P+dYmBg" +
                "ZgjwZucASTFBlQTg1CwCxHDNvo5+nm6uwSF6vm6ffc+c9vHW1bvI662rde7M" +
                "+c1BBleMHzwt0vPy1fH0vVi6ioWLwfWLj4jJn26hycVBonM+d3N96hbybugy" +
                "rdxZsRPsgqls25Y5AM13grqAi4EB6CphNBewA3FxYm5BTqo+bkUiCEWpFQWp" +
                "RZm5qXkliTlIOiTRdEjg0JGWn6+PCA50XVp4dBUkJmcnpqfqZual5esl5yQW" +
                "F5f67uVrcpB4/ZP51ZLu7tXa9x49e7Kgs7PTbf4DBfknH370HXCsMWOXP9Bu" +
                "tEhHpyxj8rbM+PfHhQ9IJcvv65944EnWaqVFe81UDE6HlgRzss5K3u0VupZF" +
                "bHrX3HMvDmzVK83IOJ0zt+hWkaaAjPfUp20qd4u1ZklZp6bkrL1T2lNsvVsw" +
                "4t/q8vmsy+7nZ4qofxJZJrLTUuGCc7fcL3u5hBsrrqvIfWAExcKVbVbHFwK9" +
                "dRscC4xMIgyoKQGWRkDJCBWgJCp0rciRK4KizRZHkgKZwMWAOxEgwH7kJIFb" +
                "E6q1T3AmEYQJ2BIJAogx4ksyCO+DTEEOVS0UU3zwmIKZhAK8WdlAutiAcBJQ" +
                "pys4OgCGehbu7QMAAA=="
        )
    }
    /* ktlint-enable max-line-length */
}
