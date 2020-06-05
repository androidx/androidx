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

    class TransitFragment : StrictViewFragment() {
        val transitValues = mutableSetOf<Int>()

        override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
            transitValues += transit
            return super.onCreateAnimation(transit, enter, nextAnim)
        }

        override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int): Animator? {
            transitValues += transit
            return super.onCreateAnimator(transit, enter, nextAnim)
        }
    }
}
