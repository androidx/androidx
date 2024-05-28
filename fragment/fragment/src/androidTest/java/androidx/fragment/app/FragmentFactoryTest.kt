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
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentFactoryTest {

    @Suppress("DEPRECATION")
    val activityRule = androidx.test.rule.ActivityTestRule(EmptyFragmentTestActivity::class.java)

    // Detect leaks BEFORE and AFTER activity is destroyed
    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(activityRule)

    @Test
    @UiThreadTest
    fun testActivityFragmentManagerFactory() {
        val activity = activityRule.activity
        val fragmentManager = activity.supportFragmentManager
        val fragmentFactory = TestFragmentFactory()

        fragmentManager.fragmentFactory = fragmentFactory
        activity.setContentView(R.layout.activity_inflated_fragment)
        assertEquals(
            "FragmentFactory should be used for inflated Fragments",
            1,
            fragmentFactory.instantiateCount
        )
    }

    @Test
    @UiThreadTest
    fun testActivityFragmentManagerFactoryWithFragmentContainer() {
        val activity = activityRule.activity
        val fragmentManager = activity.supportFragmentManager
        val fragmentFactory = TestFragmentFactory()

        fragmentManager.fragmentFactory = fragmentFactory
        activity.setContentView(R.layout.inflated_fragment_container_view)
        assertEquals(
            "FragmentFactory should be used for inflated Fragments",
            1,
            fragmentFactory.instantiateCount
        )
    }

    @Test
    @UiThreadTest
    fun testActivityFragmentManagerFactoryWithChild() {
        val activity = activityRule.activity
        val fragmentManager = activity.supportFragmentManager
        val fragmentFactory = TestFragmentFactory()

        fragmentManager.fragmentFactory = fragmentFactory
        activity.setContentView(R.layout.activity_content)
        fragmentManager.beginTransaction().replace(R.id.content, ParentFragment()).commitNow()
        assertEquals(
            "FragmentFactory should be used for inflated child Fragments",
            1,
            fragmentFactory.instantiateCount
        )
    }

    @Test
    @UiThreadTest
    fun testActivityFragmentManagerFactoryWithChildWithFragmentContainerView() {
        val activity = activityRule.activity
        val fragmentManager = activity.supportFragmentManager
        val fragmentFactory = TestFragmentFactory()

        fragmentManager.fragmentFactory = fragmentFactory
        activity.setContentView(R.layout.activity_content)
        fragmentManager
            .beginTransaction()
            .replace(R.id.content, ParentFragmentContainerView())
            .commitNow()
        assertEquals(
            "FragmentFactory should be used for inflated child Fragments",
            1,
            fragmentFactory.instantiateCount
        )
    }

    @Test
    @UiThreadTest
    fun testChildFragmentManagerFactory() {
        val activity = activityRule.activity
        val fragmentManager = activity.supportFragmentManager
        val fragmentFactory = TestFragmentFactory()

        fragmentManager.fragmentFactory = fragmentFactory
        activity.setContentView(R.layout.activity_content)
        val childFragmentFactory = TestFragmentFactory()
        fragmentManager
            .beginTransaction()
            .replace(R.id.content, ParentFragment().apply { factory = childFragmentFactory })
            .commitNow()
        assertEquals(
            "FragmentFactory should not used for child Fragments when they " +
                "have their own FragmentFactory",
            0,
            fragmentFactory.instantiateCount
        )
        assertEquals(
            "Child FragmentFactory should be used for inflated child Fragments",
            1,
            childFragmentFactory.instantiateCount
        )
    }

    @Test
    @UiThreadTest
    fun testChildFragmentManagerFactoryWithFragmentContainerView() {
        val activity = activityRule.activity
        val fragmentManager = activity.supportFragmentManager
        val fragmentFactory = TestFragmentFactory()

        fragmentManager.fragmentFactory = fragmentFactory
        activity.setContentView(R.layout.activity_content)
        val childFragmentFactory = TestFragmentFactory()
        fragmentManager
            .beginTransaction()
            .replace(
                R.id.content,
                ParentFragmentContainerView().apply { factory = childFragmentFactory }
            )
            .commitNow()
        assertEquals(
            "FragmentFactory should not used for child Fragments when they " +
                "have their own FragmentFactory",
            0,
            fragmentFactory.instantiateCount
        )
        assertEquals(
            "Child FragmentFactory should be used for inflated child Fragments",
            1,
            childFragmentFactory.instantiateCount
        )
    }
}

class ParentFragment : Fragment(R.layout.nested_inflated_fragment_parent) {
    var factory: FragmentFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        factory?.let { factory -> childFragmentManager.fragmentFactory = factory }
    }
}

class ParentFragmentContainerView : Fragment(R.layout.nested_inflated_fragment_container_parent) {
    var factory: FragmentFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        factory?.let { factory -> childFragmentManager.fragmentFactory = factory }
    }
}

class TestFragmentFactory : FragmentFactory() {
    var instantiateCount = 0

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        instantiateCount++
        return super.instantiate(classLoader, className)
    }
}
