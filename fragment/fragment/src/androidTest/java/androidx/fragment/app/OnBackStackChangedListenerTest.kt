/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.fragment.app.FragmentManager.OnBackStackChangedListener
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class OnBackStackChangedListenerTest {
    @get:Rule
    val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun testOnBackChangeStartedAdd() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }

            val fragment1 = StrictFragment()
            lateinit var innerFragment: Fragment
            var innerPop = false
            var count = 0
            fragmentManager.addOnBackStackChangedListener(object : OnBackStackChangedListener {
                override fun onBackStackChanged() { /* nothing */
                }

                override fun onBackStackChangeStarted(fragment: Fragment, pop: Boolean) {
                    innerFragment = fragment
                    innerPop = pop
                    count++
                }
            })

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.content, fragment1)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            assertThat(fragment1).isSameInstanceAs(innerFragment)
            assertThat(innerPop).isFalse()
            assertThat(count).isEqualTo(1)
        }
    }

    @Test
    fun testOnBackChangeStartedReplace() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }

            val fragment1 = StrictFragment()

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.content, fragment1)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            var innerPop = false
            var count = 0

            val incomingFragments = mutableListOf<Fragment>()

            fragmentManager.addOnBackStackChangedListener(object : OnBackStackChangedListener {
                override fun onBackStackChanged() { /* nothing */
                }

                override fun onBackStackChangeStarted(fragment: Fragment, pop: Boolean) {
                    incomingFragments.add(fragment)
                    innerPop = pop
                    count++
                }
            })

            val fragment2 = StrictFragment()

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragment2)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            assertThat(incomingFragments).containsExactlyElementsIn(listOf(fragment1, fragment2))
            assertThat(innerPop).isFalse()
            assertThat(count).isEqualTo(2)
        }
    }

    @Test
    fun testOnBackChangeStartedPop() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }

            val fragment1 = StrictFragment()

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.content, fragment1)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            val fragment2 = StrictFragment()

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragment2)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            var innerPop = false
            var count = 0

            val incomingFragments = mutableListOf<Fragment>()

            fragmentManager.addOnBackStackChangedListener(object : OnBackStackChangedListener {
                override fun onBackStackChanged() { /* nothing */
                }

                override fun onBackStackChangeStarted(fragment: Fragment, pop: Boolean) {
                    incomingFragments.add(fragment)
                    innerPop = pop
                    count++
                }
            })

            fragmentManager.popBackStack()
            executePendingTransactions()

            assertThat(incomingFragments).containsExactlyElementsIn(listOf(fragment1, fragment2))
            assertThat(innerPop).isTrue()
            assertThat(count).isEqualTo(2)

            incomingFragments.remove(fragment2)
        }
    }

    @Test
    fun testOnBackChangeCommittedAdd() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }

            val fragment1 = StrictFragment()
            lateinit var innerFragment: Fragment
            var innerPop = false
            var count = 0
            fragmentManager.addOnBackStackChangedListener(object : OnBackStackChangedListener {
                override fun onBackStackChanged() { /* nothing */
                }

                override fun onBackStackChangeCommitted(fragment: Fragment, pop: Boolean) {
                    innerFragment = fragment
                    innerPop = pop
                    count++
                }
            })

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.content, fragment1)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            assertThat(fragment1).isSameInstanceAs(innerFragment)
            assertThat(innerPop).isFalse()
            assertThat(count).isEqualTo(1)
        }
    }

    @Test
    fun testOnBackChangeCommittedReplace() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }

            val fragment1 = StrictFragment()

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.content, fragment1)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            var innerPop = false
            var count = 0

            val incomingFragments = mutableListOf<Fragment>()

            fragmentManager.addOnBackStackChangedListener(object : OnBackStackChangedListener {
                override fun onBackStackChanged() { /* nothing */
                }

                override fun onBackStackChangeCommitted(fragment: Fragment, pop: Boolean) {
                    incomingFragments.add(fragment)
                    innerPop = pop
                    count++
                }
            })

            val fragment2 = StrictFragment()

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragment2)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            assertThat(incomingFragments).containsExactlyElementsIn(listOf(fragment1, fragment2))
            assertThat(innerPop).isFalse()
            assertThat(count).isEqualTo(2)
        }
    }

    @Test
    fun testOnBackChangeCommittedPop() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }

            val fragment1 = StrictFragment()

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.content, fragment1)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            val fragment2 = StrictFragment()

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragment2)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            var innerPop = false
            var count = 0

            val incomingFragments = mutableListOf<Fragment>()

            fragmentManager.addOnBackStackChangedListener(object : OnBackStackChangedListener {
                override fun onBackStackChanged() { /* nothing */
                }

                override fun onBackStackChangeCommitted(fragment: Fragment, pop: Boolean) {
                    incomingFragments.add(fragment)
                    innerPop = pop
                    count++
                }
            })

            fragmentManager.popBackStack()
            executePendingTransactions()

            assertThat(incomingFragments).containsExactlyElementsIn(listOf(fragment1, fragment2))
            assertThat(innerPop).isTrue()
            assertThat(count).isEqualTo(2)

            incomingFragments.remove(fragment2)
        }
    }

    @Ignore("b/277763818")
    @Test
    fun testOnBackChangeCommittedReplacePop() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }

            val fragment1 = StrictFragment()

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.content, fragment1)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            var innerPop = false
            var count = 0

            val incomingFragments = mutableListOf<Fragment>()

            fragmentManager.addOnBackStackChangedListener(object : OnBackStackChangedListener {
                override fun onBackStackChanged() { /* nothing */
                }

                override fun onBackStackChangeCommitted(fragment: Fragment, pop: Boolean) {
                    incomingFragments.add(fragment)
                    innerPop = pop
                    count++
                }
            })

            val fragment2 = StrictFragment()

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content, fragment2)
                .addToBackStack(null)
                .commit()
            fragmentManager.popBackStack()
            executePendingTransactions()

            assertThat(incomingFragments).containsExactlyElementsIn(listOf(fragment1, fragment2))
            assertThat(innerPop).isTrue()
            assertThat(count).isEqualTo(2)
        }
    }

    @Test
    fun testOnBackChangeRemoveListenerAfterStarted() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }

            val fragment = StrictFragment()
            var startedCount = 0
            var committedCount = 0
            val listener = object : OnBackStackChangedListener {
                override fun onBackStackChanged() { /* nothing */ }

                override fun onBackStackChangeStarted(fragment: Fragment, pop: Boolean) {
                    startedCount++
                }

                override fun onBackStackChangeCommitted(fragment: Fragment, pop: Boolean) {
                    committedCount++
                }
            }
            fragmentManager.addOnBackStackChangedListener(listener)

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.content, fragment)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            assertThat(startedCount).isEqualTo(1)
            assertThat(committedCount).isEqualTo(1)
        }
    }

    @Test
    fun testOnBackChangeNoAddToBackstack() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragmentManager = withActivity { supportFragmentManager }

            val fragment = StrictFragment()
            var startedCount = 0
            var committedCount = 0
            val listener = object : OnBackStackChangedListener {
                override fun onBackStackChanged() { /* nothing */ }

                override fun onBackStackChangeStarted(fragment: Fragment, pop: Boolean) {
                    startedCount++
                }

                override fun onBackStackChangeCommitted(fragment: Fragment, pop: Boolean) {
                    committedCount++
                }
            }
            fragmentManager.addOnBackStackChangedListener(listener)

            withActivity {
                fragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.content, fragment)
                    .commitNow()
            }

            assertThat(startedCount).isEqualTo(0)
            assertThat(committedCount).isEqualTo(0)
        }
    }
}