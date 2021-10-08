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

import androidx.fragment.lint.stubs.ADD_MENU_PROVIDER_STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AddMenuProviderDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = UnsafeFragmentLifecycleObserverDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(UnsafeFragmentLifecycleObserverDetector.ADD_MENU_PROVIDER_ISSUE)

    private fun check(vararg files: TestFile): TestLintResult {
        return lint().files(*files, *ADD_MENU_PROVIDER_STUBS)
            .run()
    }

    @Test
    fun pass() {
        check(
            kotlin(
                """
package com.example

import androidx.fragment.app.Fragment
import com.example.test.Foo

class TestFragment : Fragment {

    override fun onCreateView() {
        requireActivity().addMenuProvider(this, getViewLifecycleOwner())
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.STARTED)
    }

    override fun onViewCreated() {
        test()
        val foo = Foo()
        foo.addMenuProvider(requireActivity(), this)
        foo.menuProvider(this)
    }

    private fun test() {
        requireActivity().addMenuProvider(this, getViewLifecycleOwner())
        test()
    }
}
            """
            ),
            kotlin(
                """
package com.example.test

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner

class Foo {
    fun addMenuProvider(activity: ComponentActivity, fragment: Fragment) {
        activity.addMenuProvider(fragment, LifecycleOwner())
        activity.addMenuProvider(fragment, LifecycleOwner(), Lifecycle.State.STARTED)
    }

    fun menuProvider(fragment: Fragment) {}
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

class TestFragment : Fragment {

    override fun onCreateView() {
        requireActivity().addMenuProvider(this, this)
    }
}
            """
            )
        )
            .expect(
                """
src/com/example/TestFragment.kt:9: Error: Use viewLifecycleOwner as the LifecycleOwner. [FragmentAddMenuProvider]
        requireActivity().addMenuProvider(this, this)
                                                ~~~~
1 errors, 0 warnings
            """
            )
            .checkFix(
                null,
                kotlin(
                    """
package com.example

import androidx.fragment.app.Fragment

class TestFragment : Fragment {

    override fun onCreateView() {
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }
}
            """
                )
            )
    }

    @Test
    fun helperMethodFails() {
        check(
            kotlin(
                """
package com.example

import androidx.fragment.app.Fragment

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        requireActivity().addMenuProvider(this, this)
    }
}
            """
            )
        )
            .expect(
                """
src/com/example/TestFragment.kt:13: Error: Use viewLifecycleOwner as the LifecycleOwner. [FragmentAddMenuProvider]
        requireActivity().addMenuProvider(this, this)
                                                ~~~~
1 errors, 0 warnings
            """
            )
            .checkFix(
                null,
                kotlin(
                    """
package com.example

import androidx.fragment.app.Fragment

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }
}
            """
                )
            )
    }

    @Test
    fun externalCallFails() {
        check(
            kotlin(
                """
package com.example

import androidx.fragment.app.Fragment
import com.example.test.Foo

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val foo = Foo()
        foo.addMenuProvider(requireActivity(), this)
    }
}
            """
            ),
            kotlin(
                """
package com.example.test

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment

class Foo {
    fun addMenuProvider(activity:ComponentActivity, fragment: Fragment) {
        activity.addMenuProvider(fragment, fragment)
        activity.addMenuProvider(fragment, fragment, Lifecycle.State.STARTED)
    }
}
            """
            )
        )
            .expect(
                """
src/com/example/test/Foo.kt:9: Error: Unsafe call to addMenuProvider with Fragment instance as LifecycleOwner from TestFragment.onCreateView. [FragmentAddMenuProvider]
        activity.addMenuProvider(fragment, fragment)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test/Foo.kt:10: Error: Unsafe call to addMenuProvider with Fragment instance as LifecycleOwner from TestFragment.onCreateView. [FragmentAddMenuProvider]
        activity.addMenuProvider(fragment, fragment, Lifecycle.State.STARTED)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
            """
            )
    }

    @Test
    fun externalHelperMethodFails() {
        check(
            kotlin(
                """
package com.example

import androidx.fragment.app.Fragment
import com.example.test.Foo

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val foo = Foo()
        foo.addMenuProvider(requireActivity(), this)
    }
}
            """
            ),
            kotlin(
                """
package com.example.test

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment

class Foo {
    private lateinit val fragment: Fragment

    fun addMenuProvider(activity:ComponentActivity, fragment: Fragment) {
        this.fragment = fragment
        menuProvider(activity)
    }

    private fun menuProvider(activity:ComponentActivity) {
        activity.addMenuProvider(fragment, fragment)
        activity.addMenuProvider(fragment, fragment, Lifecycle.State.STARTED)
    }
}
            """
            )
        )
            .expect(
                """
src/com/example/test/Foo.kt:16: Error: Unsafe call to addMenuProvider with Fragment instance as LifecycleOwner from TestFragment.onCreateView. [FragmentAddMenuProvider]
        activity.addMenuProvider(fragment, fragment)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test/Foo.kt:17: Error: Unsafe call to addMenuProvider with Fragment instance as LifecycleOwner from TestFragment.onCreateView. [FragmentAddMenuProvider]
        activity.addMenuProvider(fragment, fragment, Lifecycle.State.STARTED)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
            """
            )
    }
}
