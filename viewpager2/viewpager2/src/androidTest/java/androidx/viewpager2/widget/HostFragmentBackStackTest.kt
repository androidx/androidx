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

package androidx.viewpager2.widget

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock.sleep
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.PollingCheck
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.test.R
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.swipe.FragmentAdapter
import androidx.viewpager2.widget.swipe.PageFragment
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Regression test for an issue when recreating a [ViewPager2]'s host [Fragment]'s [View] causes
 * issues with post-grace-period PageFragments' gc() .
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HostFragmentBackStackTest : BaseTest() {
    @Test
    fun test_sameFragment_multipleBackStackEntries() {
        @Suppress("DEPRECATION")
        FragmentManager.enableDebugLogging(true)
        val containerId = ViewCompat.generateViewId()
        setUpTest(ORIENTATION_HORIZONTAL).apply {
            val container: ViewGroup = FrameLayout(activity).apply {
                id = containerId
                layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
                setBackgroundColor(Color.WHITE)
            }
            runOnUiThreadSync {
                activity.setContentView(container)
            }

            val viewPagerFragment = ViewPagerFragment()
            val blankFragment = Fragment()

            fun setActiveFragment(f: Fragment, targetPage: Int? = null) {
                // set new active fragment
                runOnUiThreadSync {
                    activity.supportFragmentManager.beginTransaction().replace(container.id, f)
                        .addToBackStack(null).commit()
                }

                // wait for view hierarchy to be updated
                PollingCheck.waitFor(2000) {
                    val expectedCount: Int = when (f) {
                        viewPagerFragment -> 1
                        blankFragment -> 0
                        else -> throw IllegalArgumentException()
                    }
                    container.childCount == expectedCount
                }

                // wait for viewpager state to be restored if applicable
                if (targetPage != null) {
                    PollingCheck.waitFor(2000) { viewPager.currentItem == targetPage }
                    assertBasicState(targetPage)
                }
            }

            setActiveFragment(viewPagerFragment, 0) // ViewPagerFragment#1, page#0
            val targetPage = 1
            setCurrentPage(targetPage) // ViewPagerFragment#1, page#1

            // verify that f0 and f1 are both in the Fragment manager
            viewPagerFragment.childFragmentManager.fragments.filter { it is PageFragment }
                .map { it as Fragment }
                .sortedBy { it.tag }.toList().let { fragments: List<Fragment> ->
                    assertThat(fragments.size, equalTo(2))
                    assertThat(fragments[0].tag, equalTo("f0"))
                    assertThat(fragments[1].tag, equalTo("f1"))
                    assertThat(fragments[0].isAdded, equalTo(true))
                    assertThat(fragments[1].isResumed, equalTo(true))
                }

            setActiveFragment(blankFragment)
            setActiveFragment(viewPagerFragment, targetPage) // ViewPagerFragment#2, page#1
            setActiveFragment(blankFragment)
            setActiveFragment(viewPagerFragment, targetPage) // ViewPagerFragment#3, page#1

            // verify before and after the grace-period
            assertBasicState(targetPage)
            sleep(gracePeriodMs * 3 / 2) // 1.5x grace period
            assertBasicState(targetPage)

            // verify that gc still happened (f0 removed)
            viewPagerFragment.childFragmentManager.fragments.filter { it is PageFragment }
                .map { it as Fragment }
                .sortedBy { it.tag }.toList().let { fragments: List<Fragment> ->
                    assertThat(fragments.size, equalTo(1))
                    assertThat(fragments[0].tag, equalTo("f1"))
                    assertThat(fragments[0].isResumed, equalTo(true))
                }
        }
    }

    private fun Context.setCurrentPage(targetPage: Int) {
        viewPager.setCurrentItemSync(targetPage, false, 2000, SECONDS)
        assertBasicState(targetPage)
    }

    private val gracePeriodMs: Long
        get() {
            val result: Long = FragmentStateAdapter::class.java.declaredFields
                .first { it.name == "GRACE_WINDOW_TIME_MS" }
                .let {
                    it.isAccessible = true
                    it.get(null) as Long
                }
            assertThat(result, isBetweenInIn<Long>(500, 50_000))
            return result
        }

    class ViewPagerFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return ViewPager2(inflater.context).apply {
                id = R.id.view_pager
                layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
                adapter = FragmentAdapter(childFragmentManager, lifecycle, stringSequence(3))
            }
        }

        override fun onDestroyView() {
            /**
             * Too late {@link FragmentStateAdapter#saveState} already happened, but effective
             * at reproducing the issue (scenario from b/139095195), so keeping for now.
             * TODO: create a different bug reproduction scenario.
             */
            (view as ViewPager2).adapter = null

            super.onDestroyView()
        }
    }
}
