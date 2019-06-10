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
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class NestedInflatedFragmentTest {

    @get:Rule
    var activityRule = ActivityTestRule(FragmentTestActivity::class.java)

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

    /**
     * This mimics the behavior of FragmentStatePagerAdapter jumping between pages
     */
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

    open class ParentFragment : Fragment(R.layout.nested_inflated_fragment_parent)

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    class UserVisibleHintParentFragment : ParentFragment() {
        override fun setUserVisibleHint(isVisibleToUser: Boolean) {
            super.setUserVisibleHint(isVisibleToUser)
            if (host != null) {
                for (fragment in childFragmentManager.fragments) {
                    fragment.userVisibleHint = isVisibleToUser
                }
            }
        }

        override fun onAttachFragment(childFragment: Fragment) {
            super.onAttachFragment(childFragment)
            childFragment.userVisibleHint = userVisibleHint
        }
    }

    class InflatedChildFragment : Fragment(R.layout.nested_inflated_fragment_child)

    class SimpleFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = TextView(inflater.context).apply {
            text = "Simple fragment"
        }
    }
}
