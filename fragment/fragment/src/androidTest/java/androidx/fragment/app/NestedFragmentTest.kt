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

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.app.test.FragmentTestActivity.ParentFragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
@LargeTest
class NestedFragmentTest {
    @Suppress("DEPRECATION")
    var activityRule = androidx.test.rule.ActivityTestRule(FragmentTestActivity::class.java)

    // Detect leaks BEFORE and AFTER activity is destroyed
    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(activityRule)

    private fun setupParentFragment(parentFragment: ParentFragment) {
        val fragmentManager = activityRule.activity.supportFragmentManager
        fragmentManager.beginTransaction().add(parentFragment, "parent").commit()
        val latch = CountDownLatch(1)
        activityRule.runOnUiThread {
            fragmentManager.executePendingTransactions()
            latch.countDown()
        }
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testUsingUpper16BitRequestCode() {
        val parentFragment = ParentFragment()
        setupParentFragment(parentFragment)

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activityResult = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())

        val activityMonitor =
            instrumentation.addMonitor(
                IntentFilter(Intent.ACTION_CALL),
                activityResult,
                true /* block */
            )

        // Verify that onActivityResult hasn't been called yet.
        assertThat(parentFragment.childFragment.onActivityResultCalled).isFalse()

        val latch = CountDownLatch(1)
        activityRule.runOnUiThread {
            parentFragment.childFragment.startActivityForResult(
                Intent(Intent.ACTION_CALL),
                16777216 /* requestCode */
            )
            latch.countDown()
        }
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()

        assertThat(instrumentation.checkMonitorHit(activityMonitor, 1)).isTrue()

        val childFragment = parentFragment.childFragment
        assertThat(childFragment.onActivityResultCalled).isTrue()
        assertThat(childFragment.onActivityResultRequestCode).isEqualTo(16777216)
        assertThat(childFragment.onActivityResultResultCode).isEqualTo(Activity.RESULT_OK)
    }

    @Test
    fun testNestedFragmentStartActivityForResult() {
        val parentFragment = ParentFragment()
        setupParentFragment(parentFragment)

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activityResult = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())

        val activityMonitor =
            instrumentation.addMonitor(
                IntentFilter(Intent.ACTION_CALL),
                activityResult,
                true /* block */
            )

        // Verify that onActivityResult hasn't been called yet.
        assertThat(parentFragment.childFragment.onActivityResultCalled).isFalse()

        val latch = CountDownLatch(1)
        activityRule.runOnUiThread {
            parentFragment.childFragment.startActivityForResult(
                Intent(Intent.ACTION_CALL),
                5 /* requestCode */
            )
            latch.countDown()
        }
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()

        assertThat(instrumentation.checkMonitorHit(activityMonitor, 1)).isTrue()

        val childFragment = parentFragment.childFragment
        assertThat(childFragment.onActivityResultCalled).isTrue()
        assertThat(childFragment.onActivityResultRequestCode).isEqualTo(5)
        assertThat(childFragment.onActivityResultResultCode).isEqualTo(Activity.RESULT_OK)
    }
}
