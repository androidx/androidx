/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

import org.junit.Test

class BanKeepAnnotationTest : LintDetectorTest() {
    override fun getDetector(): Detector = BanKeepAnnotation()

    override fun getIssues(): List<Issue> = listOf(
        BanKeepAnnotation.ISSUE
    )

    private fun check(code: String): TestLintResult {
        return lint().files(
            java(annotationSource),
            java(code)
        )
            .run()
    }

    private val annotationSource = """
        package androidx.annotation;

        public @interface Keep {
        }
    """

    @Test fun testAnnotatedUnreferencedClass() {
        val input = """
            package androidx.sample;

            import androidx.annotation.Keep;
            @Keep
            public class SampleClass {
            }
        """
        val expected = """
            src/androidx/sample/SampleClass.java:4: Error: Uses @Keep annotation [BanKeepAnnotation]
            @Keep
            ~~~~~
            1 errors, 0 warnings
        """
        check(input.trimIndent())
            .expect(expected.trimIndent())
    }
}
