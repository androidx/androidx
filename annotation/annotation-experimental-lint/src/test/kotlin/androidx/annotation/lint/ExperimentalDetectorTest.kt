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

    @Test
    fun useExperimentalClassUnchecked() {
        val input = arrayOf(
            javaSample("sample.ExperimentalDateTime"),
            javaSample("sample.UseExperimentalClassUnchecked")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/UseExperimentalClassUnchecked.java:29: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTime' or '@UseExperimental(sample.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        DateProvider provider = new DateProvider();
                                ~~~~~~~~~~~~~~~~~~
src/sample/UseExperimentalClassUnchecked.java:30: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTime' or '@UseExperimental(sample.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        return provider.getDate();
                        ~~~~~~~
2 errors, 0 warnings
    """.trimIndent()
        /* ktlint-enable max-line-length */

        checkJava(*input).expect(expected)
    }

    @Test
    fun useExperimentalClassChecked() {
        val input = arrayOf(
            javaSample("sample.ExperimentalDateTime"),
            javaSample("sample.UseExperimentalClassChecked")
        )

        val expected = "No warnings."

        checkJava(*input).expect(expected)
    }

    @Test
    fun useExperimentalMethodUnchecked() {
        val input = arrayOf(
            javaSample("sample.ExperimentalDateTime"),
            javaSample("sample.UseExperimentalMethodUnchecked")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/sample/UseExperimentalMethodUnchecked.java:35: Error: This declaration is experimental and its usage should be marked with
'@sample.ExperimentalDateTime' or '@UseExperimental(sample.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        System.out.println(getDate());
                           ~~~~~~~
1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        checkJava(*input).expect(expected)
    }

    @Test
    fun useExperimentalClassCheckedUses() {
        val input = arrayOf(
            javaSample("sample.ExperimentalDateTime"),
            javaSample("sample.UseExperimentalClassCheckedUses")
        )

        val expected = "No warnings."

        checkJava(*input).expect(expected)
    }
}
