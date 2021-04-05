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

package androidx.fragment.app

import android.animation.Animator
import android.view.animation.Animation
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_CLOSE
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.waitForExecution
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [FragmentTransaction.setTransition].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentTransitTest {
    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = androidx.test.rule.ActivityTestRule(FragmentTestActivity::class.java)

    @Before
    fun setupContainer() {
        activityRule.setContentView(R.layout.simple_container)
    }

    @Test
    fun addWithTransit() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment = TransitFragment()
        fm.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        assertThat(fragment.transitValues)
            .containsExactly(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
    }

    @Test
    fun testFragmentAnimationWithActivityTransition() {
        val fragmentA = FragmentA()
        val fragmentB = FragmentB()
        val fm = activityRule.activity.supportFragmentManager

        // set TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN to navigate forward when fragmentA entering.
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragmentA)
            .setTransition(TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN)
            .commit()
        activityRule.executePendingTransactions(fm)

        assertThat(fragmentA.transitValues).containsExactly(TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN)
        assertThat(fragmentA.isEnterTransit).isTrue()

        // set TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN to navigate forward when fragmentB entering
        // and fragmentA exiting.
        fm.beginTransaction()
            .replace(R.id.fragmentContainer, fragmentB)
            .setTransition(TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN)
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions(fm)

        assertThat(fragmentA.transitValues).containsExactly(TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN)
        assertThat(fragmentA.isEnterTransit).isFalse()
        assertThat(fragmentB.transitValues).containsExactly(TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN)
        assertThat(fragmentB.isEnterTransit).isTrue()

        // Simulating back key with popBackStack, system will set
        // TRANSIT_FRAGMENT_MATCH_ACTIVITY_CLOSE to navigate backward when fragmentB exiting
        // and fragmentA entering.
        fm.popBackStack()
        activityRule.executePendingTransactions(fm)

        assertThat(fragmentB.transitValues).contains(TRANSIT_FRAGMENT_MATCH_ACTIVITY_CLOSE)
        assertThat(fragmentB.isEnterTransit).isFalse()
        assertThat(fragmentA.transitValues).contains(TRANSIT_FRAGMENT_MATCH_ACTIVITY_CLOSE)
        assertThat(fragmentA.isEnterTransit).isTrue()
    }

    public open class TransitFragment(
        @LayoutRes contentLayoutId: Int = R.layout.strict_view_fragment
    ) : StrictFragment(contentLayoutId) {
        val transitValues = mutableSetOf<Int>()
        var isEnterTransit: Boolean = false

        override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
            transitValues += transit
            isEnterTransit = enter
            return super.onCreateAnimation(transit, enter, nextAnim)
        }

        override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int): Animator? {
            transitValues += transit
            return super.onCreateAnimator(transit, enter, nextAnim)
        }
    }

    class FragmentA : TransitFragment(R.layout.fragment_a)

    class FragmentB : TransitFragment(R.layout.fragment_b)
}
