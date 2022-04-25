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

package androidx.fragment.lint

import androidx.fragment.lint.stubs.FRAGMENT_TRANSACTION_STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AttachAndDetachInSameTransactionDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = AttachAndDetachInSameTransactionDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(AttachAndDetachInSameTransactionDetector.DETACH_ATTACH_OPERATIONS_ISSUE)

    private fun check(vararg files: TestFile): TestLintResult {
        return lint().files(*files, *FRAGMENT_TRANSACTION_STUBS)
            .run()
    }

    @Test
    fun pass() {
        check(
            kotlin(
                """
package com.example

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.test.Foo

class Test {
    private fun testPassingToAnotherClass() {
        val ft = FragmentManager().beginTransaction()
        val foo = Foo().commitTransaction(ft)
    }

    private fun testTwoDifferentFragments() {
        val fragment1 = Fragment()
        val fragment2 = Fragment()
        val ft = FragmentManager().beginTransaction()
        ft.attach(fragment1)
        ft.detach(fragment2)
        ft.commit()
    }

    // If the variable is reassigned even though it is the same instance we won't warn
    private fun testReassign() {
        val fragment3 = Fragment()
        val fragment4 = fragment3
        val ft = FragmentManager().beginTransaction()
        ft.attach(fragment3)
        ft.detach(fragment4)
        ft.commit()
    }

    // If the fragment is passed through to another function we won't warn
    private fun testPassingToAnother() {
        val fragment = Fragment()
        val ft = FragmentManager().beginTransaction()
        ft.attach(fragment)
        helper(ft, fragment)
    }

    private fun helper(ft: FragmentTransaction, fragment: Fragment) {
        ft.detach(fragment)
        ft.commit()
    }
}
            """
            ),
            kotlin(
                """
package com.example.test

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

class Foo {
    fun commitTransaction(fragmentTransaction: FragmentTransaction, fragment: Fragment) {
        val fragment1 = Fragment()
        val fragment2 = Fragment()
        fragmentTransaction.attach(fragment1)
        fragmentTransaction.detach(fragment2)
        fragmentTransaction.commit()
    }
}
            """
            )
        )
            .expectClean()
    }

    @Test
    fun inMethodFails() {
        check(
            kotlin(
                """
package com.example

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

class Test {
    private fun testInternal() {
        val fragment = Fragment()
        val ft = FragmentManager().beginTransaction()
        ft.attach(fragment)
        ft.detach(fragment)
        ft.commit()
    }
}
            """
            )
        )
            .expect(
                """
src/com/example/Test.kt:11: Warning: Calling detach() and attach() in the same FragmentTransaction is a no-op, meaning it does not recreate the Fragment's view. If you would like the view to be recreated, separate these operations into separate transactions. [DetachAndAttachSameFragment]
        val ft = FragmentManager().beginTransaction()
                                   ~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
    }

    @Test
    fun helperMethodFails() {
        check(
            kotlin(
                """
package com.example

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

class Test {
    private fun test() {
        val fragment = Fragment()
        helper(fragment)
    }

    private fun helper(fragment: Fragment) {
        val ft = FragmentManager().beginTransaction()
        ft.detach(fragment)
        ft.attach(fragment)
        ft.commit()
    }
}
            """
            )
        )
            .expect(
                """
src/com/example/Test.kt:15: Warning: Calling detach() and attach() in the same FragmentTransaction is a no-op, meaning it does not recreate the Fragment's view. If you would like the view to be recreated, separate these operations into separate transactions. [DetachAndAttachSameFragment]
        val ft = FragmentManager().beginTransaction()
                                   ~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
    }
}
