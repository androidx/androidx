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

package androidx.lifecycle.lint

import androidx.lifecycle.lint.stubs.STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.util.Properties

@RunWith(JUnit4::class)
class NonNullableMutableLiveDataDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = NonNullableMutableLiveDataDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(NonNullableMutableLiveDataDetector.ISSUE)

    private lateinit var sdkDir: File

    @Before
    fun setup() {
        val stream = NonNullableMutableLiveDataDetector::class.java.classLoader
            .getResourceAsStream("sdk.prop")
        val properties = Properties()
        properties.load(stream)
        sdkDir = File(properties["sdk.dir"] as String)
    }

    private fun check(vararg files: TestFile): TestLintResult {
        return lint().files(*files, *STUBS)
            .sdkHome(sdkDir)
            .run()
    }

    @Test
    fun pass() {
        check(
            kotlin("""
                package com.example

                import androidx.lifecycle.MutableLiveData

                fun foo() {
                    val liveData = MutableLiveData<Boolean?>()
                }
            """).indented()
        ).expectClean()
    }

    @Test
    fun expectFail() {
        check(
            kotlin("""
                package com.example

                import androidx.lifecycle.MutableLiveData

                fun foo() {
                    val liveData = MutableLiveData<Boolean>()
                }
            """).indented()
        ).expect("""
src/com/example/test.kt:6: Warning: Use nullable type parameter. [NullSafeMutableLiveData]
    val liveData = MutableLiveData<Boolean>()
                                   ~~~~~~~
0 errors, 1 warnings
        """).checkFix(null, kotlin("""
                package com.example

                import androidx.lifecycle.MutableLiveData

                fun foo() {
                    val liveData = MutableLiveData<Boolean?>()
                }
        """).indented())
    }
}
