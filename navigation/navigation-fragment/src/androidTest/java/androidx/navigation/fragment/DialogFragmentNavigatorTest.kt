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

package androidx.navigation.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DialogFragmentNavigatorTest {

    companion object {
        private const val INITIAL_FRAGMENT = 1
    }

    @get:Rule
    var activityRule = ActivityTestRule(EmptyActivity::class.java)

    private lateinit var emptyActivity: EmptyActivity
    private lateinit var fragmentManager: FragmentManager

    @Before
    fun setup() {
        emptyActivity = activityRule.activity
        fragmentManager = emptyActivity.supportFragmentManager
    }

    @UiThreadTest
    @Test
    fun testNavigate() {
        lateinit var dialogFragment: DialogFragment
        fragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return super.instantiate(classLoader, className).also { fragment ->
                    if (fragment is DialogFragment) {
                        dialogFragment = fragment
                    }
                }
            }
        }
        val dialogNavigator = DialogFragmentNavigator(emptyActivity, fragmentManager)
        val destination = dialogNavigator.createDestination().apply {
            id = INITIAL_FRAGMENT
            className = EmptyDialogFragment::class.java.name
        }

        assertThat(dialogNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        assertWithMessage("Dialog should be shown")
            .that(dialogFragment.requireDialog().isShowing)
            .isTrue()
    }

    @UiThreadTest
    @Test
    fun testPop() {
        lateinit var dialogFragment: DialogFragment
        fragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return super.instantiate(classLoader, className).also { fragment ->
                    if (fragment is DialogFragment) {
                        dialogFragment = fragment
                    }
                }
            }
        }
        val dialogNavigator = DialogFragmentNavigator(emptyActivity, fragmentManager)
        val destination = dialogNavigator.createDestination().apply {
            id = INITIAL_FRAGMENT
            className = EmptyDialogFragment::class.java.name
        }

        assertThat(dialogNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        assertWithMessage("Dialog should be shown")
            .that(dialogFragment.requireDialog().isShowing)
            .isTrue()
        assertWithMessage("DialogNavigator should pop dialog off the back stack")
            .that(dialogNavigator.popBackStack())
            .isTrue()
        assertWithMessage("Pop should dismiss the DialogFragment")
            .that(dialogFragment.requireDialog().isShowing)
            .isFalse()
    }
}

class EmptyDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext()).create()
}
