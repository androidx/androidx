/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FragmentManagerTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun addRemoveFragmentOnAttachListener() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val fragmentBefore = StrictFragment()
            val fragmentDuring = StrictFragment()
            val fragmentAfter = StrictFragment()

            val attachedFragments = mutableListOf<Fragment>()
            val listener = FragmentOnAttachListener { _, fragment ->
                attachedFragments.add(fragment)
            }

            fm.beginTransaction().add(fragmentBefore, "before").commit()
            executePendingTransactions()

            fm.addFragmentOnAttachListener(listener)

            fm.beginTransaction().add(fragmentDuring, "during").commit()
            executePendingTransactions()

            fm.removeFragmentOnAttachListener(listener)

            fm.beginTransaction().add(fragmentAfter, "after").commit()
            executePendingTransactions()

            assertThat(attachedFragments).containsExactly(fragmentDuring)
        }
    }

    @Test
    fun removeReentrantFragmentOnAttachListener() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val fragmentDuring = StrictFragment()
            val fragmentAfter = StrictFragment()

            val attachedFragments = mutableListOf<Fragment>()
            fm.addFragmentOnAttachListener(
                object : FragmentOnAttachListener {
                    override fun onAttachFragment(
                        fragmentManager: FragmentManager,
                        fragment: Fragment
                    ) {
                        attachedFragments.add(fragment)
                        fragmentManager.removeFragmentOnAttachListener(this)
                    }
                }
            )

            fm.beginTransaction().add(fragmentDuring, "during").commit()
            executePendingTransactions()

            fm.beginTransaction().add(fragmentAfter, "after").commit()
            executePendingTransactions()

            assertThat(attachedFragments).containsExactly(fragmentDuring)
        }
    }

    @Test
    fun findFragmentChildFragment() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val parentFragment = StrictViewFragment(R.layout.scene1)
            val childFragment = StrictViewFragment(R.layout.fragment_a)

            fm.beginTransaction()
                .add(R.id.fragmentContainer, parentFragment)
                .setReorderingAllowed(false)
                .commit()
            executePendingTransactions()
            parentFragment.childFragmentManager
                .beginTransaction()
                .add(R.id.squareContainer, childFragment)
                .setReorderingAllowed(false)
                .commit()
            executePendingTransactions(parentFragment.childFragmentManager)

            val parentRootView = parentFragment.requireView()
            val childRootView = childFragment.requireView()
            assertThat(FragmentManager.findFragment<Fragment>(parentRootView))
                .isEqualTo(parentFragment)
            assertThat(FragmentManager.findFragment<Fragment>(childRootView))
                .isEqualTo(childFragment)

            fm.beginTransaction().remove(parentFragment).commit()
            executePendingTransactions()

            // Check that even after removal, findFragment still returns the right Fragment
            assertThat(FragmentManager.findFragment<Fragment>(parentRootView))
                .isEqualTo(parentFragment)
            assertThat(FragmentManager.findFragment<Fragment>(childRootView))
                .isEqualTo(childFragment)
        }
    }

    @Test
    fun findFragmentManagerChildFragment() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }
            val parentFragment = StrictViewFragment(R.layout.scene1)
            val childFragment = StrictViewFragment(R.layout.fragment_a)

            fm.beginTransaction()
                .add(R.id.fragmentContainer, parentFragment)
                .setReorderingAllowed(false)
                .commit()
            executePendingTransactions()
            parentFragment.childFragmentManager
                .beginTransaction()
                .add(R.id.squareContainer, childFragment)
                .setReorderingAllowed(false)
                .commit()
            executePendingTransactions(parentFragment.childFragmentManager)

            val parentChildFragmentManager = parentFragment.childFragmentManager
            val parentRootView = parentFragment.requireView()
            val childChildFragmentManager = childFragment.childFragmentManager
            val childRootView = childFragment.requireView()
            assertThat(FragmentManager.findFragmentManager(parentRootView))
                .isEqualTo(parentChildFragmentManager)
            assertThat(FragmentManager.findFragmentManager(childRootView))
                .isEqualTo(childChildFragmentManager)

            fm.beginTransaction().remove(parentFragment).commit()
            executePendingTransactions()

            try {
                FragmentManager.findFragmentManager(parentRootView)
                fail("findFragmentManager on the removed parentRootView should throw")
            } catch (expected: IllegalStateException) {
                assertThat(expected)
                    .hasMessageThat()
                    .isEqualTo(
                        "The Fragment $parentFragment that owns View $parentRootView" +
                            " has already been destroyed. Nested fragments should always use " +
                            "the child FragmentManager."
                    )
            }
            try {
                FragmentManager.findFragmentManager(childRootView)
                fail("findFragmentManager on the removed childRootView should throw")
            } catch (expected: IllegalStateException) {
                assertThat(expected)
                    .hasMessageThat()
                    .isEqualTo(
                        "The Fragment $childFragment that owns View $childRootView" +
                            " has already been destroyed. Nested fragments should always use " +
                            "the child FragmentManager."
                    )
            }
        }
    }

    @Test
    fun addRemoveReorderingAllowedWithoutExecutePendingTransactions() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val fragment1 = StrictFragment()

            val originalWho = fragment1.mWho

            withActivity {
                fm.beginTransaction()
                    .add(fragment1, "fragment1")
                    .setReorderingAllowed(true)
                    .addToBackStack("stack1")
                    .commit()
                fm.popBackStack()
            }
            executePendingTransactions()

            assertThat(fragment1.mWho).isNotEqualTo(originalWho)
            assertThat(fragment1.mFragmentManager).isNull()
            assertThat(fm.findFragmentByWho(originalWho)).isNull()
            assertThat(fm.findFragmentByWho(fragment1.mWho)).isNull()
        }
    }

    @Test
    fun reAddRemovedBeforeAttached() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val fragment1 = StrictFragment()

            val originalWho = fragment1.mWho

            withActivity {
                fm.beginTransaction()
                    .add(fragment1, "fragment1")
                    .setReorderingAllowed(true)
                    .addToBackStack("stack1")
                    .commit()
                fm.popBackStack()
            }
            executePendingTransactions()

            assertThat(fragment1.mWho).isNotEqualTo(originalWho)
            assertThat(fragment1.mFragmentManager).isNull()
            assertThat(fm.findFragmentByWho(originalWho)).isNull()
            assertThat(fm.findFragmentByWho(fragment1.mWho)).isNull()

            val afterRemovalWho = fragment1.mWho

            fm.beginTransaction().add(fragment1, "fragment1").setReorderingAllowed(true).commit()
            executePendingTransactions()

            assertThat(fragment1.mWho).isEqualTo(afterRemovalWho)
        }
    }

    @Test
    fun popBackStackImmediate() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val fragment1 = StrictFragment()

            fm.beginTransaction().add(fragment1, "fragment1").addToBackStack("stack1").commit()
            executePendingTransactions()

            var popped = false

            withActivity {
                popped = fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
            assertThat(popped).isTrue()
        }
    }

    @Test
    fun navigatePopNavigateRestore() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val fragment2 = StrictViewFragment()

            fm.beginTransaction()
                .setCustomAnimations(
                    androidx.fragment.R.animator.fragment_fade_enter,
                    androidx.fragment.R.animator.fragment_fade_exit,
                    androidx.fragment.R.animator.fragment_fade_enter,
                    androidx.fragment.R.animator.fragment_fade_exit
                )
                .replace(R.id.content, fragment2, "fragment2")
                .setReorderingAllowed(true)
                .addToBackStack("stack2")
                .commit()
            executePendingTransactions()

            val fragment3 = StrictViewFragment()

            fm.saveBackStack("stack2")
            fm.beginTransaction()
                .setCustomAnimations(
                    androidx.fragment.R.animator.fragment_fade_enter,
                    androidx.fragment.R.animator.fragment_fade_exit,
                    androidx.fragment.R.animator.fragment_fade_enter,
                    androidx.fragment.R.animator.fragment_fade_exit
                )
                .replace(R.id.content, fragment3, "fragment3")
                .setReorderingAllowed(true)
                .addToBackStack("stack3")
                .commit()

            fm.saveBackStack("stack3")
            fm.restoreBackStack("stack2")
            executePendingTransactions()

            recreate()

            val restoredFragmentManager = withActivity { supportFragmentManager }

            assertThat(restoredFragmentManager.findFragmentByTag("fragment2")).isNotNull()
        }
    }

    @Test
    fun replacePopReplaceWithRestored() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val fragment2 = StrictViewFragment()

            fm.beginTransaction()
                .setCustomAnimations(
                    androidx.fragment.R.animator.fragment_fade_enter,
                    androidx.fragment.R.animator.fragment_fade_exit,
                    androidx.fragment.R.animator.fragment_fade_enter,
                    androidx.fragment.R.animator.fragment_fade_exit
                )
                .replace(R.id.content, fragment2, "fragment2")
                .setReorderingAllowed(true)
                .addToBackStack("stack2")
                .commit()
            executePendingTransactions()

            val fragment3 = StrictViewFragment()

            fm.popBackStack("stack2", 0)
            fm.beginTransaction()
                .setCustomAnimations(
                    androidx.fragment.R.animator.fragment_fade_enter,
                    androidx.fragment.R.animator.fragment_fade_exit,
                    androidx.fragment.R.animator.fragment_fade_enter,
                    androidx.fragment.R.animator.fragment_fade_exit
                )
                .replace(R.id.content, fragment3, "fragment3")
                .setReorderingAllowed(true)
                .addToBackStack("stack3")
                .commit()

            fm.popBackStack("stack3", 0)

            val restoreFragment2 = StrictViewFragment()
            withActivity {
                restoreFragment2.setInitialSavedState(fm.saveFragmentInstanceState(fragment2))
            }
            fm.beginTransaction()
                .setCustomAnimations(
                    androidx.fragment.R.animator.fragment_fade_enter,
                    androidx.fragment.R.animator.fragment_fade_exit,
                    androidx.fragment.R.animator.fragment_fade_enter,
                    androidx.fragment.R.animator.fragment_fade_exit
                )
                .replace(R.id.content, restoreFragment2, "fragment2")
                .setReorderingAllowed(true)
                .addToBackStack("stack2")
                .commit()
            executePendingTransactions()

            recreate()

            val restoredFragmentManager = withActivity { supportFragmentManager }

            assertThat(restoredFragmentManager.findFragmentByTag("fragment2")).isNotNull()
        }
    }
}
