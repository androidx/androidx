/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.activity.contextaware.OnContextAvailableListener
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class FragmentContextAwareTest {

    @get:Rule val activityScenarioRule = ActivityScenarioRule(FragmentTestActivity::class.java)

    @Test
    fun testContextAvailableListener() {
        val fragment = Fragment()

        var callbackContext: Context? = null
        val listener = OnContextAvailableListener { context -> callbackContext = context }
        fragment.addOnContextAvailableListener(listener)

        activityScenarioRule.scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(R.id.content, fragment)
                .commitNow()

            assertThat(fragment.peekAvailableContext()).isSameInstanceAs(activity)
            assertThat(callbackContext).isSameInstanceAs(activity)

            activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()

            assertThat(fragment.peekAvailableContext()).isNull()
        }
    }

    @Test
    fun testContextAvailableListenerAddedAfterAttach() {
        val fragment = Fragment()

        activityScenarioRule.scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(R.id.content, fragment)
                .commitNow()

            assertThat(fragment.peekAvailableContext()).isSameInstanceAs(activity)

            var callbackContext: Context? = null
            val listener = OnContextAvailableListener { context -> callbackContext = context }

            fragment.addOnContextAvailableListener(listener)
            assertThat(callbackContext).isSameInstanceAs(activity)
        }
    }

    @Test
    fun testRemoveContextAvailableListener() {
        val fragment = Fragment()

        var callbackContext: Context? = null
        val listener = OnContextAvailableListener { context -> callbackContext = context }
        fragment.addOnContextAvailableListener(listener)
        fragment.removeOnContextAvailableListener(listener)

        activityScenarioRule.scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(R.id.content, fragment)
                .commitNow()

            assertThat(callbackContext).isNull()

            assertThat(fragment.peekAvailableContext()).isSameInstanceAs(activity)
        }
    }

    @Test
    fun testContextAvailableBeforeOnAttach() {
        val fragment = OrderTrackingFragment()
        activityScenarioRule.scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(R.id.content, fragment)
                .commitNow()

            assertThat(fragment.onContextAvailableCalled).isTrue()
            assertThat(fragment.onAttachCalled).isTrue()
            assertThat(fragment.onContextAvailableFirst).isTrue()
        }
    }

    @Test
    fun testContextAvailableDuringOnDetach() {
        val fragment = OrderTrackingFragment()
        activityScenarioRule.scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(R.id.content, fragment)
                .commitNow()

            assertThat(fragment.peekAvailableContext()).isSameInstanceAs(activity)

            activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()

            assertThat(fragment.contextAvailableBeforeSuperOnDetach).isTrue()
            assertThat(fragment.contextAvailableAfterSuperOnDetach).isTrue()
            assertThat(fragment.peekAvailableContext()).isNull()
        }
    }

    class OrderTrackingFragment : Fragment() {
        var onContextAvailableCalled = false
        var onAttachCalled = false
        var onContextAvailableFirst = false
        var contextAvailableBeforeSuperOnDetach = false
        var contextAvailableAfterSuperOnDetach = false

        init {
            addOnContextAvailableListener {
                onContextAvailableCalled = true
                if (!onAttachCalled) {
                    onContextAvailableFirst = true
                }
            }
        }

        override fun onAttach(context: Context) {
            onAttachCalled = true
            super.onAttach(context)
        }

        override fun onDetach() {
            contextAvailableBeforeSuperOnDetach = peekAvailableContext() != null
            super.onDetach()
            contextAvailableAfterSuperOnDetach = peekAvailableContext() != null
        }
    }
}
