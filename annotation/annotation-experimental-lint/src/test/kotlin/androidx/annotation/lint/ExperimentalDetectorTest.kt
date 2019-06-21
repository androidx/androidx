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

package androidx.annotation.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExperimentalDetectorTest {

    private fun checkJava(vararg testFiles: TestFile): TestLintResult {
        return lint()
            .files(
                javaSample("androidx.annotation.Experimental"),
                javaSample("androidx.annotation.UseExperimental"),
                *testFiles
            )
            .allowMissingSdk(true)
            .issues(*ExperimentalDetector.ISSUES.toTypedArray())
            .run()
    }

    /**
     * Loads a [TestFile] from Java source code included in the JAR resources.
     */
    private fun javaSample(className: String): TestFile {
        return java(javaClass.getResource("/${className.replace('.','/')}.java").readText())
    }

    /**
     * Loads a [TestFile] from Kotlin source code included in the JAR resources.
     */
    private fun ktSample(className: String): TestFile {
        return kotlin(javaClass.getResource("/${className.replace('.','/')}.kt").readText())
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
src/sample/UseJavaExperimentalFromJava.java:24: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTime' or '@UseExperimental(sample.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        DateProvider dateProvider = new DateProvider();
                                    ~~~~~~~~~~~~~~~~~~
src/sample/UseJavaExperimentalFromJava.java:25: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTime' or '@UseExperimental(sample.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        return dateProvider.getDate();
                            ~~~~~~~
2 errors, 0 warnings
    """.trimIndent()
        /* ktlint-enable max-line-length */

        checkJava(*input).expect(expected)
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
src/sample/UseJavaExperimentalFromKt.kt:24: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTime' or '@UseExperimental(sample.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        val dateProvider = DateProvider()
                           ~~~~~~~~~~~~
src/sample/UseJavaExperimentalFromKt.kt:25: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTime' or '@UseExperimental(sample.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        return dateProvider.date
                            ~~~~
src/sample/UseJavaExperimentalFromKt.kt:36: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTime' or '@UseExperimental(sample.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        val dateProvider = DateProvider()
                           ~~~~~~~~~~~~
src/sample/UseJavaExperimentalFromKt.kt:37: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTime' or '@UseExperimental(sample.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        return dateProvider.date
                            ~~~~
4 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        checkJava(*input).expect(expected)
    }
}
