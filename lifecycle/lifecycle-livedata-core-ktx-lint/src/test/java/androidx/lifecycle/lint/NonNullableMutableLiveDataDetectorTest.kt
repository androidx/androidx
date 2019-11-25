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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NonNullableMutableLiveDataDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = NonNullableMutableLiveDataDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(NonNullableMutableLiveDataDetector.ISSUE)

    private fun check(vararg files: TestFile): TestLintResult {
        return lint().files(*files, *STUBS)
            .run()
    }

    @Test
    fun pass() {
        check(
            kotlin("""
                package com.example

                import androidx.lifecycle.MutableLiveData

                fun foo() {
                    val liveData = MutableLiveData<Boolean>()
                    val x = true
                    liveData.value = x
                    liveData.postValue(bar(5))
                    val myLiveData = MyLiveData()
                    liveData.value = x
                }

                fun bar(x: Int): Boolean {
                    return x > 0
                }
            """).indented(),
            kotlin("""
                package com.example

                import androidx.lifecycle.MutableLiveData

                class MyLiveData : MyLiveData2()
                open class MyLiveData2 : GenericLiveData<Boolean>()
                open class GenericLiveData<T> : MutableLiveData<T>()
            """).indented()
        ).expectClean()
    }

    @Test
    fun helperMethodFails() {
        check(
            kotlin("""
                package com.example

                import androidx.lifecycle.MutableLiveData

                fun foo() {
                    val liveData = MutableLiveData<Boolean>()
                    liveData.value = bar(5)
                }

                fun bar(x: Int): Boolean? {
                    if (x > 0) return true
                    return null
                }
            """).indented()
        ).expect("""
src/com/example/test.kt:7: Error: Expected non-nullable value [NullSafeMutableLiveData]
    liveData.value = bar(5)
                     ~~~~~~
1 errors, 0 warnings
        """)
    }

    @Test
    fun variableAssignmentFails() {
        check(
            kotlin("""
                package com.example

                import androidx.lifecycle.MutableLiveData

                fun foo() {
                    val liveData = MutableLiveData<Boolean>()
                    val bar: Boolean? = null
                    liveData.value = bar
                }
            """).indented()
        ).expect("""
src/com/example/test.kt:8: Error: Expected non-nullable value [NullSafeMutableLiveData]
    liveData.value = bar
                     ~~~
1 errors, 0 warnings
        """).expectFixDiffs("""
Fix for src/com/example/test.kt line 8: Change `LiveData` type to nullable:
@@ -6 +6
-     val liveData = MutableLiveData<Boolean>()
+     val liveData = MutableLiveData<Boolean?>()
Fix for src/com/example/test.kt line 8: Add non-null asserted (!!) call:
@@ -8 +8
-     liveData.value = bar
+     liveData.value = bar!!
        """)
    }

    @Test
    fun nullLiteralQuickFix() {
        check(
            kotlin("""
                package com.example

                import androidx.lifecycle.MutableLiveData

                fun foo() {
                    val liveData = MutableLiveData<Boolean>()
                    liveData.value = null
                }
            """).indented()
        ).expectFixDiffs("""
Fix for src/com/example/test.kt line 7: Change `LiveData` type to nullable:
@@ -6 +6
-     val liveData = MutableLiveData<Boolean>()
+     val liveData = MutableLiveData<Boolean?>()
        """)
    }

    @Test
    fun classHierarchyTest() {
        check(
            kotlin("""
                package com.example

                fun foo() {
                    val liveData = MyLiveData()
                    val bar: Boolean? = true
                    liveData.value = bar
                }
            """).indented(),
            kotlin("""
                package com.example

                import androidx.lifecycle.MutableLiveData

                class MyLiveData : MyLiveData2()
                open class MyLiveData2 : GenericLiveData<Boolean>()
                open class GenericLiveData<T> : MutableLiveData<T>()
            """).indented()
        ).expect("""
src/com/example/test.kt:6: Error: Expected non-nullable value [NullSafeMutableLiveData]
    liveData.value = bar
                     ~~~
1 errors, 0 warnings
        """).expectFixDiffs("""
Fix for src/com/example/test.kt line 6: Add non-null asserted (!!) call:
@@ -6 +6
-     liveData.value = bar
+     liveData.value = bar!!
        """)
    }
}
