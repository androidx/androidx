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
package androidx.fragment.lint

import androidx.fragment.lint.stubs.STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.util.Properties

@RunWith(JUnit4::class)
class FragmentLiveDataObserveDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = FragmentLiveDataObserverDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(FragmentLiveDataObserverDetector.ISSUE)

    private var sdkDir: File? = null

    @Before
    fun setup() {
        val stream = FragmentTagDetectorTest::class.java.classLoader.getResourceAsStream("sdk.prop")
        val properties = Properties()
        properties.load(stream)
        sdkDir = File(properties["sdk.dir"] as String)
    }

    @Test
    fun pass() {
        lint().files(
            kotlin("""
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.example.test.Foo

class TestFragment : Fragment {

    override fun onCreateView() {
        val liveData = MutableLiveData<String>()
        liveData.observe(getViewLifecycleOwner(), Observer<String> {})
    }

    override fun onViewCreated() {
        test()
        val foo = Foo()
        foo.observeData(this)
        foo.observe(this)
    }

    private fun test() {
        val liveData = MutableLiveData<String>()
        liveData.observe(getViewLifecycleOwner(), Observer<String> {})
        test()
    }
}
            """),
            kotlin("""
package com.example.test

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData

class Foo {
    fun observeData(fragment: Fragment) {
        val liveData = MutableLiveData<String>()
        liveData.observe(LifecycleOwner(), Observer<String> {})
        liveData.observe(fragment, Observer<String> {}, true)
    }

    fun observe(fragment: Fragment) {}
}
            """), *STUBS)
            .sdkHome(sdkDir!!)
            .run()
            .expectClean()
    }

    @Test
    fun observeInMethodFails() {
        lint().files(
            kotlin("""
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class TestFragment : Fragment {

    override fun onCreateView() {
        val liveData = MutableLiveData<String>()
        liveData.observe(this, Observer<String> {})
    }
}
            """), *STUBS)
            .sdkHome(sdkDir!!)
            .run()
            .expect("""
src/com/example/TestFragment.kt:11: Error: Use getViewLifecycleOwner() as the LifecycleOwner. [FragmentLiveDataObserve]
        liveData.observe(this, Observer<String> {})
                         ~~~~
1 errors, 0 warnings
            """)
            .checkFix(null, kotlin("""
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class TestFragment : Fragment {

    override fun onCreateView() {
        val liveData = MutableLiveData<String>()
        liveData.observe(getViewLifecycleOwner(), Observer<String> {})
    }
}
            """))
    }

    @Test
    fun helperMethodFails() {
        lint().files(
            kotlin("""
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val liveData = MutableLiveData<String>()
        liveData.observe(this, Observer<String> {})
    }
}
            """), *STUBS)
            .sdkHome(sdkDir!!)
            .run()
            .expect("""
src/com/example/TestFragment.kt:15: Error: Use getViewLifecycleOwner() as the LifecycleOwner. [FragmentLiveDataObserve]
        liveData.observe(this, Observer<String> {})
                         ~~~~
1 errors, 0 warnings
            """)
            .checkFix(null, kotlin("""
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val liveData = MutableLiveData<String>()
        liveData.observe(getViewLifecycleOwner(), Observer<String> {})
    }
}
            """))
    }

    @Test
    fun externalCallFails() {
        lint().files(
            kotlin("""
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.example.test.Foo

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val foo = Foo()
        foo.observeData(this)
    }
}
            """),
            kotlin("""
package com.example.test

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class Foo {
    fun observeData(fragment: Fragment) {
        val liveData = MutableLiveData<String>()
        liveData.observe(fragment, Observer<String> {})
    }
}
            """), *STUBS)
            .sdkHome(sdkDir!!)
            .run()
            .expect("""
src/com/example/test/Foo.kt:10: Error: Unsafe call to observe with Fragment instance from TestFragment.onCreateView. [FragmentLiveDataObserve]
        liveData.observe(fragment, Observer<String> {})
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
            """)
    }

    @Test
    fun externalHelperMethodFails() {
        lint().files(
            kotlin("""
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.example.test.Foo

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val foo = Foo()
        foo.observeData(this)
    }
}
            """),
            kotlin("""
package com.example.test

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class Foo {
    private lateinit val fragment: Fragment

    fun observeData(fragment: Fragment) {
        this.fragment = fragment
        observe()
    }

    private fun observe() {
        val liveData = MutableLiveData<String>()
        liveData.observe(fragment, Observer<String> {})
    }
}
            """), *STUBS)
            .sdkHome(sdkDir!!)
            .run()
            .expect("""
src/com/example/test/Foo.kt:17: Error: Unsafe call to observe with Fragment instance from TestFragment.onCreateView. [FragmentLiveDataObserve]
        liveData.observe(fragment, Observer<String> {})
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
            """)
    }
}
