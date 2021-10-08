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

import androidx.fragment.lint.stubs.BACK_CALLBACK_STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BackPressedDispatcherCallbackDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = UnsafeFragmentLifecycleObserverDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(UnsafeFragmentLifecycleObserverDetector.BACK_PRESSED_ISSUE)

    private fun check(vararg files: TestFile): TestLintResult {
        return lint().files(*files, *BACK_CALLBACK_STUBS)
            .run()
    }

    @Test
    fun pass() {
        check(
            kotlin(
                """
package com.example

import androidx.fragment.app.Fragment
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedCallback
import com.example.test.Foo

class TestFragment : Fragment {

    override fun onCreateView() {
        val dispatcher = OnBackPressedDispatcher()
        dispatcher.addCallback(getViewLifecycleOwner(), OnBackPressedCallback {})
    }

    override fun onViewCreated() {
        test()
        val foo = Foo()
        foo.addCallback(this)
        foo.callback(this)
    }

    private fun test() {
        val dispatcher = OnBackPressedDispatcher()
        dispatcher.addCallback(getViewLifecycleOwner(), OnBackPressedCallback {})
        test()
    }
}
            """
            ),
            kotlin(
                """
package com.example.test

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedCallback

class Foo {
    fun addCallback(fragment: Fragment) {
        val dispatcher = OnBackPressedDispatcher()
        dispatcher.addCallback(LifecycleOwner(), OnBackPressedCallback {})
    }

    fun callback(fragment: Fragment) {}
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
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedCallback

class TestFragment : Fragment {

    override fun onCreateView() {
        val dispatcher = OnBackPressedDispatcher()
        dispatcher.addCallback(this, OnBackPressedCallback {})
    }
}
            """
            )
        )
            .expect(
                """
src/com/example/TestFragment.kt:12: Error: Use viewLifecycleOwner as the LifecycleOwner. [FragmentBackPressedCallback]
        dispatcher.addCallback(this, OnBackPressedCallback {})
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
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedCallback

class TestFragment : Fragment {

    override fun onCreateView() {
        val dispatcher = OnBackPressedDispatcher()
        dispatcher.addCallback(viewLifecycleOwner, OnBackPressedCallback {})
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
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedCallback

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val dispatcher = OnBackPressedDispatcher()
        dispatcher.addCallback(this, OnBackPressedCallback {})
    }
}
            """
            )
        )
            .expect(
                """
src/com/example/TestFragment.kt:16: Error: Use viewLifecycleOwner as the LifecycleOwner. [FragmentBackPressedCallback]
        dispatcher.addCallback(this, OnBackPressedCallback {})
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
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedCallback

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val dispatcher = OnBackPressedDispatcher()
        dispatcher.addCallback(viewLifecycleOwner, OnBackPressedCallback {})
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
        foo.addCallback(this)
    }
}
            """
            ),
            kotlin(
                """
package com.example.test

import androidx.fragment.app.Fragment
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedCallback

class Foo {
    fun addCallback(fragment: Fragment) {
        val dispatcher = OnBackPressedDispatcher()
        dispatcher.addCallback(fragment, OnBackPressedCallback {})
    }
}
            """
            )
        )
            .expect(
                """
src/com/example/test/Foo.kt:11: Error: Unsafe call to addCallback with Fragment instance as LifecycleOwner from TestFragment.onCreateView. [FragmentBackPressedCallback]
        dispatcher.addCallback(fragment, OnBackPressedCallback {})
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
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
        foo.addCallback(this)
    }
}
            """
            ),
            kotlin(
                """
package com.example.test

import androidx.fragment.app.Fragment
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedCallback

class Foo {
    private lateinit val fragment: Fragment

    fun addCallback(fragment: Fragment) {
        this.fragment = fragment
        callback()
    }

    private fun callback() {
        val dispatcher = OnBackPressedDispatcher()
        dispatcher.addCallback(fragment, OnBackPressedCallback {})
    }
}
            """
            )
        )
            .expect(
                """
src/com/example/test/Foo.kt:18: Error: Unsafe call to addCallback with Fragment instance as LifecycleOwner from TestFragment.onCreateView. [FragmentBackPressedCallback]
        dispatcher.addCallback(fragment, OnBackPressedCallback {})
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
    }
}
