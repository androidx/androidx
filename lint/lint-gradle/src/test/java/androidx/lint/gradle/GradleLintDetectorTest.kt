/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lint.gradle

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

/** Base test setup for lint checks in this project, providing the defined Gradle [STUBS]. */
abstract class GradleLintDetectorTest(
    private val detector: Detector,
    private val issues: List<Issue>
) : LintDetectorTest() {
    override fun getDetector(): Detector = detector

    override fun getIssues(): List<Issue> = issues

    /** Convenience method for running a lint test over the input [files] and [STUBS]. */
    fun check(vararg files: TestFile): TestLintResult {
        return lint().files(*STUBS, *files).run()
    }
}
