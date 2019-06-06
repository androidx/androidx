/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.fragment.app

import android.os.Bundle
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.fragment.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentFactoryTest {

    @get:Rule
    var activityRule = ActivityTestRule(EmptyFragmentTestActivity::class.java)

    private lateinit var activity: EmptyFragmentTestActivity
    private lateinit var fragmentManager: FragmentManager
    private lateinit var fragmentFactory: TestFragmentFactory

    @Before
    fun setup() {
        activity = activityRule.activity
        fragmentManager = activity.supportFragmentManager
        fragmentFactory = TestFragmentFactory()
    }

    @Test
    @UiThreadTest
    fun testActivityFragmentManagerFactory() {
        fragmentManager.fragmentFactory = fragmentFactory
        activity.setContentView(R.layout.activity_inflated_fragment)
        assertEquals("FragmentFactory should be used for inflated Fragments",
                1, fragmentFactory.instantiateCount)
    }

    @Test
    @UiThreadTest
    fun testActivityFragmentManagerFactoryWithChild() {
        fragmentManager.fragmentFactory = fragmentFactory
        activity.setContentView(R.layout.activity_content)
        fragmentManager.beginTransaction()
                .replace(R.id.content, ParentFragment())
                .commitNow()
        assertEquals("FragmentFactory should be used for inflated child Fragments",
                1, fragmentFactory.instantiateCount)
    }

    @Test
    @UiThreadTest
    fun testChildFragmentManagerFactory() {
        fragmentManager.fragmentFactory = fragmentFactory
        activity.setContentView(R.layout.activity_content)
        val childFragmentFactory = TestFragmentFactory()
        fragmentManager.beginTransaction()
                .replace(R.id.content, ParentFragment().apply {
                    factory = childFragmentFactory
                })
                .commitNow()
        assertEquals("FragmentFactory should not used for child Fragments when they " +
                "have their own FragmentFactory", 0, fragmentFactory.instantiateCount)
        assertEquals("Child FragmentFactory should be used for inflated child Fragments",
                1, childFragmentFactory.instantiateCount)
    }
}

class ParentFragment : Fragment(R.layout.nested_inflated_fragment_parent) {
    var factory: FragmentFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        factory?.let { factory ->
            childFragmentManager.fragmentFactory = factory
        }
    }
}

class TestFragmentFactory : FragmentFactory() {
    var instantiateCount = 0

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        instantiateCount++
        return super.instantiate(classLoader, className)
    }
}
