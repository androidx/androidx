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

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
@MediumTest
class NestedInflatedFragmentTest {

    var activityRule = androidx.test.rule.ActivityTestRule(FragmentTestActivity::class.java)

    // Detect leaks BEFORE and AFTER activity is destroyed
    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(activityRule)

    @Test
    @UiThreadTest
    fun inflatedChildFragment() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val parentFragment = ParentFragment()
        fm.beginTransaction().add(android.R.id.content, parentFragment).commitNow()

        fm.beginTransaction()
            .replace(android.R.id.content, SimpleFragment())
            .addToBackStack(null)
            .commit()
        fm.executePendingTransactions()

        fm.popBackStackImmediate()
    }

    @Test
    @UiThreadTest
    fun inflatedChildFragmentWithFragmentContainerView() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val parentFragment = ParentFragmentContainerView()
        fm.beginTransaction().add(android.R.id.content, parentFragment).commitNow()

        fm.beginTransaction()
            .replace(android.R.id.content, SimpleFragment())
            .addToBackStack(null)
            .commit()
        fm.executePendingTransactions()

        fm.popBackStackImmediate()
    }

    @Test
    @UiThreadTest
    fun inflatedChildFragmentHasAttributesOnInflate() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val parentFragment = ParentFragment()
        fm.beginTransaction().add(android.R.id.content, parentFragment).commitNow()

        val child =
            parentFragment.childFragmentManager.findFragmentById(R.id.child_fragment)
                as InflatedChildFragment

        assertThat(child.name)
            .isEqualTo(
                "androidx.fragment.app" + ".NestedInflatedFragmentTest\$InflatedChildFragment"
            )
    }

    @Test
    @UiThreadTest
    fun inflatedChildFragmentHasAttributesOnInflateWithFragmentContainerView() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val parentFragment = ParentFragmentContainerView()
        fm.beginTransaction().add(android.R.id.content, parentFragment).commitNow()

        val child =
            parentFragment.childFragmentManager.findFragmentById(R.id.child_fragment)
                as InflatedChildFragment

        assertThat(child.name)
            .isEqualTo(
                "androidx.fragment.app" + ".NestedInflatedFragmentTest\$InflatedChildFragment"
            )
    }

    /** This mimics the behavior of FragmentStatePagerAdapter jumping between pages */
    @Test
    @UiThreadTest
    fun nestedSetUserVisibleHint() {
        val fm = activityRule.activity.supportFragmentManager

        // Add a UserVisibleHintParentFragment
        var fragment = UserVisibleHintParentFragment()
        fm.beginTransaction().add(android.R.id.content, fragment).commit()
        fm.executePendingTransactions()

        fragment.userVisibleHint = false

        val state = fm.saveFragmentInstanceState(fragment)
        fm.beginTransaction().remove(fragment).commit()
        fm.executePendingTransactions()

        fragment = UserVisibleHintParentFragment()
        fragment.setInitialSavedState(state)
        fragment.userVisibleHint = true

        fm.beginTransaction().add(android.R.id.content, fragment).commit()
        fm.executePendingTransactions()
    }

    @Test
    @UiThreadTest
    fun nestedSetUserVisibleHintWithFragmentContainerView() {
        val fm = activityRule.activity.supportFragmentManager

        // Add a UserVisibleHintParentFragment
        var fragment = UserVisibleHintParentFragmentContainerView()
        fm.beginTransaction().add(android.R.id.content, fragment).commit()
        fm.executePendingTransactions()

        fragment.userVisibleHint = false

        val state = fm.saveFragmentInstanceState(fragment)
        fm.beginTransaction().remove(fragment).commit()
        fm.executePendingTransactions()

        fragment = UserVisibleHintParentFragmentContainerView()
        fragment.setInitialSavedState(state)
        fragment.userVisibleHint = true

        fm.beginTransaction().add(android.R.id.content, fragment).commit()
        fm.executePendingTransactions()
    }

    open class ParentFragment : Fragment(R.layout.nested_inflated_fragment_parent)

    open class ParentFragmentContainerView :
        Fragment(R.layout.nested_inflated_fragment_container_parent)

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    class UserVisibleHintParentFragment : ParentFragment(), FragmentOnAttachListener {
        override fun onAttach(context: Context) {
            super.onAttach(context)
            childFragmentManager.addFragmentOnAttachListener(this)
        }

        @Deprecated("Deprecated in Fragment")
        override fun setUserVisibleHint(isVisibleToUser: Boolean) {
            super.setUserVisibleHint(isVisibleToUser)
            if (host != null) {
                for (fragment in childFragmentManager.fragments) {
                    fragment.userVisibleHint = isVisibleToUser
                }
            }
        }

        override fun onAttachFragment(fragmentManager: FragmentManager, childFragment: Fragment) {
            childFragment.userVisibleHint = userVisibleHint
        }
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    class UserVisibleHintParentFragmentContainerView :
        ParentFragmentContainerView(), FragmentOnAttachListener {
        override fun onAttach(context: Context) {
            super.onAttach(context)
            childFragmentManager.addFragmentOnAttachListener(this)
        }

        @Deprecated("Deprecated in Fragment")
        override fun setUserVisibleHint(isVisibleToUser: Boolean) {
            super.setUserVisibleHint(isVisibleToUser)
            if (host != null) {
                for (fragment in childFragmentManager.fragments) {
                    fragment.userVisibleHint = isVisibleToUser
                }
            }
        }

        override fun onAttachFragment(fragmentManager: FragmentManager, childFragment: Fragment) {
            childFragment.userVisibleHint = userVisibleHint
        }
    }

    class InflatedChildFragment : Fragment(R.layout.nested_inflated_fragment_child) {
        var name: String? = null

        override fun onInflate(context: Context, attrs: AttributeSet, savedInstanceState: Bundle?) {
            super.onInflate(context, attrs, savedInstanceState)
            val a = context.obtainStyledAttributes(attrs, androidx.fragment.R.styleable.Fragment)
            name = a.getString(androidx.fragment.R.styleable.Fragment_android_name)
            a.recycle()
        }
    }

    class SimpleFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ) = TextView(inflater.context).apply { text = "Simple fragment" }
    }
}
