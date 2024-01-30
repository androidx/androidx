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

import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentAnimatedContainerTest {

    @Suppress("DEPRECATION")
    val activityRule = androidx.test.rule.ActivityTestRule(FragmentTestActivity::class.java)

    // Detect leaks BEFORE and AFTER activity is destroyed
    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(DetectLeaksAfterTestSuccess())
        .around(activityRule)

    @Before
    fun setupContainer() {
        activityRule.setContentView(R.layout.animated_container)
    }

    // If container has a set Transition, cancel the Animation
    @Test
    fun cancelAnimationForContainerTransition() {
        activityRule.setContentView(R.layout.animated_container)
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val fragment1 = AnimatedContainerFragment()
        fm.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.animatedFragmentContainer, fragment1)
            .commit()
        activityRule.runOnUiThread { fm.executePendingTransactions() }

        val fragment2 = AnimatedContainerFragment()
        fm.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.animatedFragmentContainer, fragment2)
            .commit()

        assertWithMessage("Fragment should be destroyed after container animation completes")
            .that(fragment1.onDestroyLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(fragment1.onDetachLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }

    // If container has a set Transition, cancel the Transition
    @Test
    fun cancelTransitionForContainerTransition() {
        activityRule.setContentView(R.layout.animated_container)
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val fragment1 = AnimatedContainerFragment()
        fm.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.animatedFragmentContainer, fragment1)
            .commit()
        activityRule.runOnUiThread { fm.executePendingTransactions() }

        val fragment2 = AnimatedContainerFragment()
        fm.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.animatedFragmentContainer, fragment2)
            .commit()

        assertWithMessage("Fragment should be destroyed after container animation completes")
            .that(fragment1.onDestroyLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(fragment1.onDetachLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }

    class AnimatedContainerFragment : StrictViewFragment() {
        val onDestroyLatch = CountDownLatch(1)
        val onDetachLatch = CountDownLatch(1)

        override fun onDestroy() {
            super.onDestroy()
            onDestroyLatch.countDown()
        }

        override fun onDetach() {
            super.onDetach()
            onDetachLatch.countDown()
        }
    }
}
