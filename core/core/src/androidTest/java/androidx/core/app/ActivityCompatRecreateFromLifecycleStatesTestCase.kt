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
@file:Suppress("DEPRECATION")

package androidx.core.app

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.testutils.LifecycleOwnerUtils
import androidx.testutils.PollingCheck
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
public class ActivityCompatRecreateFromLifecycleStatesTestCase {
    @get:Rule
    public val activityTestRule: ActivityTestRule<ActivityCompatRecreateLifecycleTestActivity> =
        ActivityTestRule(ActivityCompatRecreateLifecycleTestActivity::class.java, false, false)

    @Test
    public fun testRecreateFromOnResume() {
        val calledRecreate = CountDownLatch(1)
        lateinit var firstActivity: Activity

        // Recreate the activity during the first post-resume callback.
        ActivityCompatRecreateLifecycleTestActivity.onResumeHandler = { activity ->
            firstActivity = activity
            ActivityCompatRecreateLifecycleTestActivity.onResumeHandler = null
            ActivityCompat.recreate(activity)
            calledRecreate.countDown()
        }

        activityTestRule.launchActivity(null)

        // Wait for the first activity's onResume() to get called. This should have already
        // happened before launchActivity returned.
        calledRecreate.await(5000, TimeUnit.MILLISECONDS)

        // Wait for the new activity to come up. In most (all?) cases, the result of the initial
        // call to launchActivity should be the new activity; however, we haven't verified this
        // across all SDK versions so we'll check the test rule's activity.
        PollingCheck.waitFor(5000) {
            activityTestRule.activity != firstActivity
        }

        // Wait until the new activity is resumed.
        val secondActivity = activityTestRule.activity
        LifecycleOwnerUtils.waitUntilState(
            secondActivity,
            Lifecycle.State.RESUMED
        )
    }

    @Test
    public fun testRecreateFromOnStart() {
        val calledRecreate = CountDownLatch(1)
        lateinit var firstActivity: Activity

        // Recreate the activity during the first post-start callback.
        ActivityCompatRecreateLifecycleTestActivity.onStartHandler = { activity ->
            firstActivity = activity
            ActivityCompatRecreateLifecycleTestActivity.onStartHandler = null
            ActivityCompat.recreate(activity)
            calledRecreate.countDown()
        }

        activityTestRule.launchActivity(null)

        // Wait for the first activity's onStart() to get called. This should have already
        // happened before launchActivity returned.
        calledRecreate.await(5000, TimeUnit.MILLISECONDS)

        // Wait for the new activity to come up. In most (all?) cases, the result of the initial
        // call to launchActivity should be the new activity; however, we haven't verified this
        // across all SDK versions so we'll check the test rule's activity.
        PollingCheck.waitFor(5000) {
            activityTestRule.activity != firstActivity
        }

        // Wait until the new activity is resumed.
        val secondActivity = activityTestRule.activity
        LifecycleOwnerUtils.waitUntilState(
            secondActivity,
            Lifecycle.State.RESUMED
        )
    }

    @Test
    public fun testRecreateFromOnStop() {
        val calledRecreate = CountDownLatch(1)
        lateinit var firstActivity: Activity

        // Recreate the activity during the first post-stop callback.
        ActivityCompatRecreateLifecycleTestActivity.onStopHandler = { activity ->
            ActivityCompatRecreateLifecycleTestActivity.onStopHandler = null
            ActivityCompat.recreate(activity)
            calledRecreate.countDown()
        }

        firstActivity = activityTestRule.launchActivity(null)
        firstActivity.finish()

        // Wait for the first activity's onStop() to get called.
        calledRecreate.await(5000, TimeUnit.MILLISECONDS)

        // Ensure the first activity is destroyed.
        LifecycleOwnerUtils.waitUntilState(
            firstActivity,
            Lifecycle.State.DESTROYED
        )

        // Make sure that we didn't start a new activity.
        assertEquals(firstActivity, activityTestRule.activity)
    }
}