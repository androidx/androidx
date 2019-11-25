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

package androidx.fragment.testing.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GradleConfigurationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = GradleConfigurationDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(GradleConfigurationDetector.ISSUE)

    @Test
    fun expectPass() {
        lint().files(
            gradle("build.gradle", """
                dependencies {
                    debugImplementation("androidx.fragment:fragment-testing:1.2.0-beta02")
                }
            """).indented())
            .run()
            .expectClean()
    }

    @Test
    fun expectFail() {
        lint().files(
            gradle("build.gradle", """
                dependencies {
                    androidTestImplementation("androidx.fragment:fragment-testing:1.2.0-beta02")
                }
            """).indented())
            .run()
            .expect("""
                build.gradle:2: Error: Replace with debugImplementation. [FragmentGradleConfiguration]
                    androidTestImplementation("androidx.fragment:fragment-testing:1.2.0-beta02")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
            """.trimIndent())
            .checkFix(null, gradle("""
                dependencies {
                    debugImplementation("androidx.fragment:fragment-testing:1.2.0-beta02")
                }
            """).indented())
    }
}
