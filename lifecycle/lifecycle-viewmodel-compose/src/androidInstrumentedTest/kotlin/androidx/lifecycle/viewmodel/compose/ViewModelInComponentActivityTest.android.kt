/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.compose

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
public class ViewModelInComponentActivityTest {
    @Suppress("DEPRECATION")
    @get:Rule
    public val activityTestRule: androidx.test.rule.ActivityTestRule<ComponentActivity> =
        androidx.test.rule.ActivityTestRule(ComponentActivity::class.java)
    private lateinit var activity: ComponentActivity

    @Before
    public fun setup() {
        activity = activityTestRule.activity
    }

    @Test
    public fun lifecycleOwnerIsAvailable() {
        val latch = CountDownLatch(1)
        var owner: LifecycleOwner? = null

        activityTestRule.runOnUiThread {
            activity.setContent {
                owner = LocalLifecycleOwner.current
                latch.countDown()
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(activity, owner)
    }

    @FlakyTest(bugId = 190609144)
    @Test
    public fun lifecycleOwnerIsAvailableWhenComposedIntoViewGroup() {
        val latch = CountDownLatch(1)
        var owner: LifecycleOwner? = null

        activityTestRule.runOnUiThread {
            val view = ComposeView(activity)
            activity.setContentView(view)
            view.setContent {
                owner = LocalLifecycleOwner.current
                latch.countDown()
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(activity, owner)
    }
}
