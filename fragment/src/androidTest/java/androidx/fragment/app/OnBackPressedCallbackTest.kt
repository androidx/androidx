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

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class OnBackPressedCallbackTest {

    @get:Rule
    var activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testBackPressWithFrameworkFragment() {
        val activity = activityRule.activity
        val fragmentManager = activity.fragmentManager
        val fragment = android.app.Fragment()

        fragmentManager.beginTransaction()
            .add(R.id.content, fragment)
            .addToBackStack(null)
            .commit()
        fragmentManager.executePendingTransactions()
        assertThat(fragmentManager.findFragmentById(R.id.content))
            .isSameAs(fragment)

        activity.onBackPressed()

        assertThat(fragmentManager.findFragmentById(R.id.content))
            .isNull()
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testBackPressWithFragmentOverFrameworkFragment() {
        val activity = activityRule.activity
        val fragmentManager = activity.fragmentManager
        val fragment = android.app.Fragment()

        fragmentManager.beginTransaction()
            .add(R.id.content, fragment)
            .addToBackStack(null)
            .commit()
        fragmentManager.executePendingTransactions()
        assertThat(fragmentManager.findFragmentById(R.id.content))
            .isSameAs(fragment)

        val supportFragmentManager = activity.supportFragmentManager
        val supportFragment = StrictFragment()

        supportFragmentManager.beginTransaction()
            .add(R.id.content, supportFragment)
            .addToBackStack(null)
            .commit()
        supportFragmentManager.executePendingTransactions()
        assertThat(supportFragmentManager.findFragmentById(R.id.content))
            .isSameAs(supportFragment)

        activity.onBackPressed()

        assertThat(supportFragmentManager.findFragmentById(R.id.content))
            .isNull()
        assertThat(fragmentManager.findFragmentById(R.id.content))
            .isSameAs(fragment)
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testBackPressWithCallbackOverFrameworkFragment() {
        val activity = activityRule.activity
        val fragmentManager = activity.fragmentManager
        val fragment = android.app.Fragment()

        fragmentManager.beginTransaction()
            .add(R.id.content, fragment)
            .addToBackStack(null)
            .commit()
        fragmentManager.executePendingTransactions()
        assertThat(fragmentManager.findFragmentById(R.id.content))
            .isSameAs(fragment)

        val callback = CountingOnBackPressedCallback()
        activity.onBackPressedDispatcher.addCallback(callback)

        activity.onBackPressed()

        assertThat(callback.count)
            .isEqualTo(1)
        assertThat(fragmentManager.findFragmentById(R.id.content))
            .isSameAs(fragment)
    }

    @UiThreadTest
    @Test
    fun testBackPressWithCallbackOverFragment() {
        val activity = activityRule.activity
        val fragmentManager = activity.supportFragmentManager
        val fragment = StrictFragment()
        fragmentManager.beginTransaction()
            .replace(R.id.content, fragment)
            .addToBackStack("back_stack")
            .commit()
        fragmentManager.executePendingTransactions()
        assertThat(fragmentManager.findFragmentById(R.id.content))
            .isSameAs(fragment)

        val callback = CountingOnBackPressedCallback()
        activity.onBackPressedDispatcher.addCallback(callback)

        activity.onBackPressed()

        assertWithMessage("OnBackPressedCallbacks should be called before FragmentManager")
            .that(callback.count)
            .isEqualTo(1)
        assertThat(fragmentManager.findFragmentById(R.id.content))
            .isSameAs(fragment)
    }
}

class CountingOnBackPressedCallback(val returnValue: Boolean = true) :
    OnBackPressedCallback {
    var count = 0

    override fun handleOnBackPressed(): Boolean {
        count++
        return returnValue
    }
}
