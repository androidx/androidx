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
src/sample/optin/UseJavaExperimentalMembersFromJava.java:59: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        new AnnotatedJavaMembers().field = -1;
                                   ~~~~~
src/sample/optin/UseJavaExperimentalMembersFromJava.java:60: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        int value = new AnnotatedJavaMembers().field;
                                               ~~~~~
src/sample/optin/UseJavaExperimentalMembersFromJava.java:61: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
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
            javaSample("sample.optin.AnnotatedJavaClass"),
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.UseJavaExperimentalClassFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseJavaExperimentalClassFromJava.java:31: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
        ~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalClassFromJava.java:31: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
                                                ~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalClassFromJava.java:32: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return experimentalObject.field;
                                  ~~~~~
src/sample/optin/UseJavaExperimentalClassFromJava.java:39: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaClass experimentalObject = new AnnotatedJavaClass();
        ~~~~~~~~~~~~~~~~~~
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
8 errors, 0 warnings
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
        ~~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalMultipleMarkersFromJava.java:33: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation2 or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation2.class) [UnsafeOptInUsageError]
        AnnotatedJavaClass2 experimentalObject2 = new AnnotatedJavaClass2();
                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalMultipleMarkersFromJava.java:34: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation2 or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation2.class) [UnsafeOptInUsageError]
        return experimentalObject.method() + experimentalObject2.field;
                                                                 ~~~~~
3 errors, 0 warnings
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
src/sample/optin/UseJavaExperimentalFromKt.kt:28: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        val experimentalObject = AnnotatedJavaClass()
                                 ~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:29: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        return experimentalObject.field
                                  ~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:36: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        val experimentalObject = AnnotatedJavaClass()
            ~~~~~~~~~~~~~~~~~~
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
src/sample/optin/UseJavaExperimentalFromKt.kt:144: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaMembers().field = -1
                               ~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:145: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        val value = AnnotatedJavaMembers().field
                                           ~~~~~
src/sample/optin/UseJavaExperimentalFromKt.kt:146: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaMembers().fieldWithSetMarker = -1
                               ~~~~~~~~~~~~~~~~~~
16 errors, 0 warnings
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

        // TODO(b/210881073): Access to annotated property `field` is still not detected.
        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseKtExperimentalFromJava.java:28: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.optin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedKotlinClass experimentalObject = new AnnotatedKotlinClass();
        ~~~~~~~~~~~~~~~~~~~~
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
src/sample/optin/UseKtExperimentalFromJava.java:115: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.optin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        new AnnotatedKotlinMembers().setField(-1);
                                     ~~~~~~~~
src/sample/optin/UseKtExperimentalFromJava.java:116: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.optin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
        int value = new AnnotatedKotlinMembers().getField();
                                                 ~~~~~~~~
src/sample/optin/UseKtExperimentalFromJava.java:117: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalKotlinAnnotation or @OptIn(markerClass = sample.optin.ExperimentalKotlinAnnotation.class) [UnsafeOptInUsageError]
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
            javaSample("sample.optin.foo.AnnotatedJavaPackage"),
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.UseJavaPackageFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/UseJavaPackageFromJava.java:33: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaPackage experimentalObject = new AnnotatedJavaPackage();
        ~~~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaPackageFromJava.java:33: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        AnnotatedJavaPackage experimentalObject = new AnnotatedJavaPackage();
                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaPackageFromJava.java:34: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        experimentalObject.method();
                           ~~~~~~
src/sample/optin/UseJavaPackageFromJava.java:67: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
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
            javaSample("sample.optin.foo.AnnotatedJavaPackage"),
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.foo.RegressionTestJava218798815")
        )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
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
src/sample/optin/UseJavaPackageFromKt.kt:29: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        val experimentalObject = AnnotatedJavaPackage()
            ~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaPackageFromKt.kt:29: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        val experimentalObject = AnnotatedJavaPackage()
                                 ~~~~~~~~~~~~~~~~~~~~
src/sample/optin/UseJavaPackageFromKt.kt:30: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        experimentalObject.method()
                           ~~~~~~
src/sample/optin/UseJavaPackageFromKt.kt:63: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        callPackageExperimental()
        ~~~~~~~~~~~~~~~~~~~~~~~
4 errors, 0 warnings
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
        ~~~
src/sample/optin/RegressionTestJava193110413.java:95: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        Bar bar = new Bar(); // unsafe
                  ~~~~~~~~~
src/sample/optin/RegressionTestJava193110413.java:96: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        bar.stableMethodLevelOptIn(); // unsafe due to experimental class scope
            ~~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/RegressionTestJava193110413.java:97: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
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
src/sample/optin/RegressionTestJava192562469.java:62: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        ExperimentalInterface anonymous = new ExperimentalInterface() { // unsafe
                                              ~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/RegressionTestJava192562469.java:64: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
            public void experimentalMethod() {} // unsafe override
                        ~~~~~~~~~~~~~~~~~~
src/sample/optin/RegressionTestJava192562469.java:67: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        ExperimentalInterface lambda = () -> {}; // unsafe
        ~~~~~~~~~~~~~~~~~~~~~
src/sample/optin/RegressionTestJava192562469.java:67: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
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

    /**
     * Regression test for b/219525415 where the @OptIn annotation did not target packages.
     */
    @Test
    fun regressionTestJava219525415() {
        val input = arrayOf(
            SAMPLE_FOO_PACKAGE_INFO,
            SAMPLE_BAR_PACKAGE_INFO,
            javaSample("sample.optin.AnnotatedJavaClass"),
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.bar.RegressionTestJava219525415"),
            javaSample("sample.optin.foo.AnnotatedJavaPackage")
        )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun regressionTestKotlin298322402() {
        val input = arrayOf(
            javaSample("sample.optin.ExperimentalJavaAnnotation"),
            javaSample("sample.optin.AnnotatedJavaMembers"),
            ktSample("sample.optin.RegressionTestKotlin298322402")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/optin/RegressionTestKotlin298322402.kt:22: Error: This declaration is opt-in and its usage should be marked with @sample.optin.ExperimentalJavaAnnotation or @OptIn(markerClass = sample.optin.ExperimentalJavaAnnotation.class) [UnsafeOptInUsageError]
        player.accessor
               ~~~~~~~~
1 errors, 0 warnings
        """.trimIndent()

        val expectedFix = """
Fix for src/sample/optin/RegressionTestKotlin298322402.kt line 22: Add '@androidx.annotation.OptIn(sample.optin.ExperimentalJavaAnnotation::class)' annotation to 'testMethod':
@@ -19 +19
+ import androidx.annotation.OptIn
@@ -21 +22
+     @OptIn(ExperimentalJavaAnnotation::class)
Fix for src/sample/optin/RegressionTestKotlin298322402.kt line 22: Add '@sample.optin.ExperimentalJavaAnnotation' annotation to 'testMethod':
@@ -21 +21
+     @ExperimentalJavaAnnotation
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix).expect(expected)
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
         * [TestFile] containing the package-level annotation for the sample.optin.foo package.
         *
         * This is a workaround for b/136184987 where package-level annotations cannot be loaded
         * from source code. This is generated from a single-class JAR using toBase64gzip(File).
         *
         * To re-generate this:
         * (if linux). alias pbcopy='xclip -selection clipboard'
         * 1. ./gradlew :annotation:annotation-experimental-lint-integration-tests:assemble
         * 2. mkdir -p temp/sample/optin/foo/
         * 3. cp ../../out/androidx/annotation/annotation-experimental-lint-integration-tests/build/intermediates/javac/debug/classes/sample/optin/foo/package-info.class temp/sample/optin/foo/
         * 4. jar -c -f sample.optin.foo.package-info.jar -C temp . | openssl base64 < sample.optin.foo.package-info.jar | tr -d '\n' | pbcopy
         * 5. Paste below
         * 6. rm -rf temp sample.optin.foo.package-info.jar
         */
        val SAMPLE_FOO_PACKAGE_INFO: TestFile = base64gzip(
            "libs/sample.optin.foo.package-info.jar",
            "H4sIAAAAAAAA/wvwZmYRYeDg4GAQiegNYkACnAwsDL6uIY66nn5u+v9OMTAw" +
                "MwR4s3OApJigSgJwahYBYrhmX0c/TzfX4BA9X7fPvmdO+3jr6l3k9dbVOnfm" +
                "/OYggyvGD54W6Xn56nj6XixdxcLF4PrFR8TkT7fQ5OIg0Tmfu7k+dQt5N3SZ" +
                "Vu6s2Al2wVS2bcscgOY7QV3AxcAAdJVlOKoL2IG4ODG3ICdVH7ciXoSi/IKS" +
                "zDwkpdZoSgXRlabl5+sjAgBduTI25QWJydmJ6am6mXlp+XrJOYnFxaG9B/kO" +
                "Oki0pHeLqevpPWKUudE9ezIz50SPiqbczbnbtv3Pu5X7+KaMTUO7UDfzM4Pi" +
                "GflG349/ZUtojGvRcJq+sN6odOaJ3kuTEjNci8RmztH0Omsj3psgIZ9dtGpC" +
                "dFbI0iTdcJdjnMt5Qnku16pyrVY1v6b56HX31KXtJzn3fv6svztlxp+FKw3/" +
                "fO9L/GD1JCrgGH+hnrA5kwRTjciCr9/MrOyc77ccEAaF+b1A20tLgF66AA5z" +
                "RiYRBtR4h6UIUKJBBShJCF0rclSKoGizxZGAQCZwMeCOcgTYj5wAcGviRdH0" +
                "BDNBILRiSxIIwM+INYEgPAzSjhyOyija7bBpx0wwAd6sbCDlbEBYC9RiDA55" +
                "AGF9KXfGAwAA"
        )

        /**
         * [TestFile] containing the package-level annotation for the sample.optin.bar package.
         *
         * See [SAMPLE_FOO_PACKAGE_INFO] for details on how to re-generate this data.
         */
        val SAMPLE_BAR_PACKAGE_INFO: TestFile = base64gzip(
            "libs/sample.optin.bar.package-info.jar",
            "H4sIAAAAAAAA/wvwZmYRYeDg4GBwyysMYUACnAwsDL6uIY66nn5u+v9OMTAw" +
                "MwR4s3OApJigSgJwahYBYrhmX0c/TzfX4BA9X7fPvmdO+3jr6l3k9dbVOnfm" +
                "/OYggyvGD54W6Xn56nj6XixdxcLF4PqFp1fE5M7nz5eLg0RtPk/6alK5s2In" +
                "2PLT/worrIFG20At52JgADpILxrVcnYgLk7MLchJ1cetiBehKL+gJDMPj1JB" +
                "dKVJiUX6CL+bovldGZvygsTk7MT0VN3MvLR8veScxOLiUD/vLCZHgdpcO2HR" +
                "pps7OC0dxDaa30wVPdLgKFAyM/SsT+qLjct3vcw8ujl1IvOTgKTVApObVjVV" +
                "za+y370+n+H8i/QXaS80v0zLk+WqM54m+s5GVHvVj5Mnlktf3aLY+vto1KLM" +
                "Ci3py7PufFrd0S3SO9lsofEkI4vgPNO/977eOb5yj8YXaS5tvmTv3Ed256Ky" +
                "9qS+rTFZpB59XlHSW+X3vLi5tZM/PZQ3Xb7gv1uwhMyhTO+gl5VxxYLtDU7s" +
                "y189eGShXzj1W67TuuB0+QDWvG+gmHlzKTkYmEYYWBhBMcPIJMKAmjBgSQaU" +
                "qlABShpD14oc4SIo2mxxpDCQCVwMuBMGAuxCTia4NfGiaLqPmWxwaxVE0crF" +
                "iDUZITwMSkjI4aiMot0Sm3bMZBXgzcoGUs4GhLVALSHgkAcAwvFVfOcDAAA="
        )
    }
    /* ktlint-enable max-line-length */
}
